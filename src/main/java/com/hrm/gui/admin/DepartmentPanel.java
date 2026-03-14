package com.hrm.gui.admin;

import com.hrm.model.PhongBan;
import com.hrm.bus.PhongBanBUS;
import com.hrm.util.SessionContext;
import com.hrm.gui.components.PurpleButton;
import com.hrm.util.UIColors;
import com.hrm.util.UIHelper;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;
import java.util.Objects;

public class DepartmentPanel extends JPanel {

    private PhongBanBUS service = new PhongBanBUS();
    private JTable table;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField txtSearch;
    private JComboBox<String> cboFilter;

    private PurpleButton btnThem;
    private PurpleButton btnSua;

    public DepartmentPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ── PANEL TRÊN
        JPanel topPanel = new JPanel(new BorderLayout());

        JLabel title = new JLabel("QUẢN LÝ PHÒNG BAN");
        title.setFont(new Font("Arial", Font.BOLD, 16));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        topPanel.add(title, BorderLayout.NORTH);

        // Gợi ý tìm kiếm
        JLabel lblHint = new JLabel("Tìm theo: Mã / Tên phòng ban / Trạng thái");
        lblHint.setFont(new Font("Arial", Font.ITALIC, 11));
        topPanel.add(lblHint, BorderLayout.SOUTH);

        // Panel chứa search + filter
        JPanel searchFilterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JLabel lblSearch = new JLabel("Tìm kiếm:");
        txtSearch = new JTextField(20);
        txtSearch.setToolTipText("Nhập mã hoặc tên phòng ban để tìm kiếm");

        JLabel lblFilter = new JLabel("    Trạng thái:");
        cboFilter = new JComboBox<>(new String[] {
                "\u0054\u1ea5\u0074\u0020\u0063\u1ea3",
                "\u0048\u006f\u1ea1\u0074\u0020\u0111\u1ed9\u006e\u0067",
                "\u004e\u0067\u1eeb\u006e\u0067\u0020\u0068\u006f\u1ea1\u0074\u0020\u0111\u1ed9\u006e\u0067"
        });

        searchFilterPanel.add(lblSearch);
        searchFilterPanel.add(txtSearch);
        searchFilterPanel.add(lblFilter);
        searchFilterPanel.add(cboFilter);
        topPanel.add(searchFilterPanel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // ── BẢNG
        tableModel = new DefaultTableModel(
                new Object[] { "Mã PB", "Tên phòng ban", "Phòng ban cha", "Trạng thái" }, 0) {
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        table.getColumnModel().getColumn(0).setPreferredWidth(70);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(160);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);

        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // ── THANH NÚT
        btnThem = new PurpleButton("+ Thêm");
        btnSua = new PurpleButton("Sửa",
                UIColors.SUCCESS_GREEN, UIColors.SUCCESS_GREEN.darker(), UIColors.SUCCESS_GREEN.darker());

        btnSua.setEnabled(false);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnPanel.add(btnThem);
        btnPanel.add(btnSua);
        add(btnPanel, BorderLayout.SOUTH);

        // ── PHÂN QUYỀN
        setupPermissions();

        // ── SỰ KIỆN
        btnThem.addActionListener(e -> showAddDialog());
        btnSua.addActionListener(e -> showEditDialog());

        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) { applyFilter(); }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                btnSua.setEnabled(table.getSelectedRow() != -1);
            }
        });

        // Double-click mở dialog chi tiết
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showDetailDialog();
                }
            }
        });

        refreshTable();
        cboFilter.addActionListener(e -> applyFilter());
    }

    // ── PHÂN QUYỀN
    private void setupPermissions() {
        boolean canManage = SessionContext.getInstance().coQuyen("DEPARTMENT_MANAGE");
        btnThem.setVisible(canManage);
        btnSua.setVisible(canManage);
    }

    private boolean isRefreshing = false;

    // ── LỌC DỮ LIỆU
    private void applyFilter() {
        if (isRefreshing) return; // bo qua neu dang refresh
        String searchText = txtSearch.getText().toLowerCase().trim();
        int statusFilterIndex = cboFilter.getSelectedIndex();

        RowFilter<DefaultTableModel, Object> rf = new RowFilter<DefaultTableModel, Object>() {
            public boolean include(Entry<? extends DefaultTableModel, ? extends Object> entry) {
                // Lọc theo mã (cột 0) hoặc tên (cột 1)
                String ma = entry.getStringValue(0).toLowerCase();
                String tenPhongBan = entry.getStringValue(1).toLowerCase();
                boolean matchSearch = searchText.isEmpty() || ma.contains(searchText) || tenPhongBan.contains(searchText);

                // Lọc theo trạng thái (cột 3)
                String trangThai = normalizeTrangThai(entry.getStringValue(3));
                boolean matchStatus = true;

                if (statusFilterIndex == 1) {
                    matchStatus = "hoatdong".equals(trangThai);
                } else if (statusFilterIndex == 2) {
                    matchStatus = "ngunghoatdong".equals(trangThai) || "ngung".equals(trangThai);
                }
                return matchSearch && matchStatus;
            }
        };
        sorter.setRowFilter(rf);
    }

    // ── LÀM MỚI BẢNG
    private void refreshTable() {
        isRefreshing = true;
        tableModel.setRowCount(0);
        for (PhongBan d : service.getAllDepartments()) {
            String tenCha = "— (goc)";
            if (d.getPhongBanChaId() != null) {
                PhongBan cha = service.getById(d.getPhongBanChaId());
                if (cha != null) {
                    tenCha = cha.getTenPhongBan();
                }
            }

            tableModel.addRow(new Object[] {
                    d.getId(),
                    d.getTenPhongBan(),
                    tenCha,
                    toTrangThaiDisplay(d.getTrangThai())
            });
        }
        txtSearch.setText("");
        cboFilter.setSelectedIndex(0);
        sorter.setRowFilter(null); // xoa filter cu
        isRefreshing = false;
    }

    // ── FORM THÊM
    private void showAddDialog() {
        JTextField txtMa = new JTextField();
        JTextField txtTen = new JTextField();
        JTextField txtMoTa = new JTextField();

        List<PhongBan> dsActive = service.getActiveDepartments();
        JComboBox<String> comboCha = buildParentCombo(dsActive, null);

        Object[] fields = {
                "Ma phong ban (*):", txtMa,
                "Ten phong ban (*):", txtTen,
                "Mo ta:", txtMoTa,
                "Phong ban cha:", comboCha
        };

        int ok = JOptionPane.showConfirmDialog(this, fields, "Them phong ban moi", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;

        String maCha = getSelectedMa(comboCha, dsActive);
        try {
            service.addDepartment(txtMa.getText().trim(), txtTen.getText().trim(), maCha, txtMoTa.getText().trim());
            refreshTable();
            JOptionPane.showMessageDialog(this, "Them phong ban thanh cong!");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Loi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── DIALOG CHI TIẾT / SỬA (double-click)

    private void showEditDialog() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Vui lòng chọn một dòng để sửa."); return; }
        int modelRow = table.convertRowIndexToModel(row);
        String ma = (String) tableModel.getValueAt(modelRow, 0);
        PhongBan dept = service.getById(ma);
        if (dept == null) return;

        boolean canEdit = SessionContext.getInstance().coQuyen("DEPARTMENT_MANAGE");

        Frame frame = (Frame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(frame, "Chi tiết phòng ban - " + dept.getTenPhongBan(), true);

        JTextField txtMa = new JTextField(dept.getId());
        txtMa.setEnabled(false);
        JTextField txtTen = new JTextField(dept.getTenPhongBan());
        txtTen.setEditable(canEdit);
        JTextField txtMoTa = new JTextField(dept.getMoTa());
        txtMoTa.setEditable(canEdit);

        List<PhongBan> dsActive = service.getActiveDepartments();
        dsActive.removeIf(d -> d.getId().equals(ma));
        JComboBox<String> comboCha = buildParentCombo(dsActive, dept.getPhongBanChaId());
        comboCha.setEnabled(canEdit);
        JComboBox<String> cboTrangThai = new JComboBox<>(new String[]{
                "\u0048\u006f\u1ea1\u0074\u0020\u0111\u1ed9\u006e\u0067",
                "\u004e\u0067\u1eeb\u006e\u0067\u0020\u0068\u006f\u1ea1\u0074\u0020\u0111\u1ed9\u006e\u0067"
        });
        cboTrangThai.setSelectedItem(toTrangThaiDisplay(dept.getTrangThai()));
        cboTrangThai.setEnabled(canEdit);

        // Form
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;

        JComponent[] fields = {txtMa, txtTen, txtMoTa, comboCha, cboTrangThai};
        for (int i = 0; i < fields.length; i++) {
            gbc.gridx = 0; gbc.gridy = i; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
            JLabel lbl = new JLabel(
                    i == 0 ? "Mã phòng ban:" :
                    i == 1 ? "Tên phòng ban (*):" :
                    i == 2 ? "Mô tả:" :
                    i == 3 ? "Phòng ban cha:" : "Trạng thái:");
            lbl.setFont(com.hrm.util.UIFonts.TEXT_MEDIUM);
            form.add(lbl, gbc);
            gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
            fields[i].setPreferredSize(new Dimension(220, 28));
            form.add(fields[i], gbc);
        }

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton btnHuy = UIHelper.createDefaultButton("Hủy");
        btnHuy.addActionListener(e -> dialog.dispose());
        btnPanel.add(btnHuy);

        if (canEdit) {
            JButton btnLuu = UIHelper.createSuccessButton("Lưu");
            btnLuu.addActionListener(e -> {
                String maCha = getSelectedMa(comboCha, dsActive);
                try {
                    String tenMoi = txtTen.getText().trim();
                    String moTaMoi = txtMoTa.getText().trim();
                    if (!Objects.equals(tenMoi, dept.getTenPhongBan())
                            || !Objects.equals(maCha, dept.getPhongBanChaId())
                            || !Objects.equals(moTaMoi, dept.getMoTa())) {
                        service.updateDepartment(ma, tenMoi, maCha, moTaMoi);
                    }
                    String rawTrangThaiMoi = toTrangThaiRaw((String) cboTrangThai.getSelectedItem());
                    if (!normalizeTrangThai(rawTrangThaiMoi).equals(normalizeTrangThai(dept.getTrangThai()))) {
                        if ("hoatDong".equals(rawTrangThaiMoi)) {
                            service.activateDepartment(ma);
                        } else {
                            service.deactivateDepartment(ma);
                        }
                    }
                    refreshTable();
                    dialog.dispose();
                    JOptionPane.showMessageDialog(DepartmentPanel.this, "Cập nhật phòng ban thành công!");
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Loi", JOptionPane.ERROR_MESSAGE);
                }
            });
            btnPanel.add(btnLuu);
        }

        JPanel main = new JPanel(new BorderLayout());
        main.add(form, BorderLayout.CENTER);
        main.add(btnPanel, BorderLayout.SOUTH);
        dialog.setContentPane(main);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(400, 230));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showDetailDialog() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        String ma = (String) tableModel.getValueAt(table.convertRowIndexToModel(row), 0);
        PhongBan dept = service.getById(ma);
        if (dept == null) return;

        String tenCha = "(Gốc)";
        if (dept.getPhongBanChaId() != null && !dept.getPhongBanChaId().trim().isEmpty()) {
            PhongBan cha = service.getById(dept.getPhongBanChaId());
            tenCha = (cha != null) ? cha.getTenPhongBan() + " (" + cha.getId() + ")" : dept.getPhongBanChaId();
        }

        JOptionPane.showMessageDialog(this,
                "Mã:            " + dept.getId() + "\n"
                + "Tên:           " + dept.getTenPhongBan() + "\n"
                + "Phòng ban cha: " + tenCha + "\n"
                + "Trạng thái:    " + toTrangThaiDisplay(dept.getTrangThai()),
                "Chi tiết phòng ban", JOptionPane.INFORMATION_MESSAGE);
    }

    private JComboBox<String> buildParentCombo(List<PhongBan> dsActive, String maChaHienTai) {
        String[] items = new String[dsActive.size() + 1];
        items[0] = "-- Khong co (phong ban goc) --";

        int selectedIndex = 0;

        for (int i = 0; i < dsActive.size(); i++) {
            items[i + 1] = dsActive.get(i).toString();

            if (dsActive.get(i).getId().equals(maChaHienTai)) {
                selectedIndex = i + 1;
            }
        }

        JComboBox<String> combo = new JComboBox<>(items);
        combo.setSelectedIndex(selectedIndex);

        return combo;
    }

    private String getSelectedMa(JComboBox<String> combo, List<PhongBan> dsActive) {
        if (combo.getSelectedIndex() == 0) {
            return null;
        }
        int idx = combo.getSelectedIndex() - 1;
        return dsActive.get(idx).getId();
    }

    private String toTrangThaiDisplay(String raw) {
        String normalized = normalizeTrangThai(raw);
        if ("hoatdong".equals(normalized)) return "\u0048\u006f\u1ea1\u0074\u0020\u0111\u1ed9\u006e\u0067";
        if ("ngunghoatdong".equals(normalized) || "ngung".equals(normalized)) {
            return "\u004e\u0067\u1eeb\u006e\u0067\u0020\u0068\u006f\u1ea1\u0074\u0020\u0111\u1ed9\u006e\u0067";
        }
        return raw == null ? "" : raw;
    }

    private String toTrangThaiRaw(String display) {
        String normalized = normalizeTrangThai(display);
        if ("hoatdong".equals(normalized)) return "hoatDong";
        if ("ngunghoatdong".equals(normalized) || "ngung".equals(normalized)) return "ngung_hoat_dong";
        return display == null ? "" : display;
    }

    private String normalizeTrangThai(String value) {
        if (value == null) return "";
        String v = value.toLowerCase().trim();
        v = v.replace("áº¡", "a").replace("ạ", "a")
             .replace("á»™", "o").replace("ộ", "o")
             .replace("á»«", "u").replace("ừ", "u")
             .replace("á»", "o").replace("ờ", "o")
             .replace("Ä‘", "d").replace("đ", "d");
        v = v.replace("_", "").replace(" ", "").replace("-", "");
        return v;
    }
}
