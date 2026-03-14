package com.hrm.bus;

import com.hrm.dao.ChucVuDAO;
import com.hrm.dao.LichSuLuongDAO;
import com.hrm.model.ChucVu;
import com.hrm.model.LichSuHeSoLuong;
import com.hrm.util.OrganizationValidation;
import com.hrm.util.SessionContext;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ChucVuBUS {

    private final ChucVuDAO chucVuDAO = new ChucVuDAO();
    private final LichSuLuongDAO lichSuDAO = LichSuLuongDAO.getInstance();

    public List<ChucVu> getAllPositions() {
        return chucVuDAO.findAll();
    }

    public List<ChucVu> getActivePositions() {
        return chucVuDAO.findActive();
    }

    // capBac >= 3 moi duoc tuyen dung thong thuong
    public List<ChucVu> getRecruitablePositions() {
        return getActivePositions().stream()
                .filter(cv -> cv.getCapBac() >= 3)
                .collect(Collectors.toList());
    }

    public ChucVu getById(String ma) {
        return chucVuDAO.findById(ma);
    }

    public List<LichSuHeSoLuong> getHistoryByMaChucVu(String ma) {
        return lichSuDAO.findByMaChucVu(ma);
    }

    public void addPosition(String ma, String ten, int capBac,
                            double heSo, double phuCap, String moTa) {
        ma  = ma  == null ? "" : ma.trim();
        ten = ten == null ? "" : ten.trim();

        String loiMa = OrganizationValidation.validateMaChucVu(ma);
        if (loiMa != null) throw new IllegalArgumentException(loiMa);

        String loiTen = OrganizationValidation.validateTenChucVu(ten);
        if (loiTen != null) throw new IllegalArgumentException(loiTen);

        String loiCapBac = OrganizationValidation.validateCapBac(capBac);
        if (loiCapBac != null) throw new IllegalArgumentException(loiCapBac);

        if (chucVuDAO.existsById(ma))
            throw new IllegalArgumentException("Ma chuc vu '" + ma + "' da ton tai.");

        String loiHeSo = OrganizationValidation.validateHeSoLuong(heSo);
        if (loiHeSo != null) throw new IllegalArgumentException(loiHeSo);

        String loiPhuCap = OrganizationValidation.validatePhuCap(phuCap);
        if (loiPhuCap != null) throw new IllegalArgumentException(loiPhuCap);

        chucVuDAO.save(new ChucVu(ma, ten, capBac, heSo, phuCap, moTa, "hoatDong"));
    }

    // Tu dong ghi lich su neu he so hoac phu cap thay doi
    public void updatePosition(String ma, String tenMoi, int capBacMoi,
                               double heSoMoi, double phuCapMoi, String moTaMoi) {
        tenMoi = tenMoi == null ? "" : tenMoi.trim();

        ChucVu cv = chucVuDAO.findById(ma);
        if (cv == null)
            throw new IllegalArgumentException("Khong tim thay chuc vu.");

        String loiTen = OrganizationValidation.validateTenChucVu(tenMoi);
        if (loiTen != null) throw new IllegalArgumentException(loiTen);

        String loiCapBac = OrganizationValidation.validateCapBac(capBacMoi);
        if (loiCapBac != null) throw new IllegalArgumentException(loiCapBac);

        String loiHeSo = OrganizationValidation.validateHeSoLuong(heSoMoi);
        if (loiHeSo != null) throw new IllegalArgumentException(loiHeSo);

        String loiPhuCap = OrganizationValidation.validatePhuCap(phuCapMoi);
        if (loiPhuCap != null) throw new IllegalArgumentException(loiPhuCap);

        if (Double.compare(cv.getHeSoLuong(), heSoMoi) != 0
                || Double.compare(cv.getPhuCapChucVu(), phuCapMoi) != 0) {
            String ngay = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            lichSuDAO.save(new LichSuHeSoLuong(
                    lichSuDAO.generateId(), ma,
                    cv.getHeSoLuong(), heSoMoi,
                    cv.getPhuCapChucVu(), phuCapMoi,
                    ngay, getCurrentUserName()));
        }

        cv.setTenChucVu(tenMoi);
        cv.setCapBac(capBacMoi);
        cv.setHeSoLuong(heSoMoi);
        cv.setPhuCapChucVu(phuCapMoi);
        cv.setMoTa(moTaMoi);
        chucVuDAO.update(cv);
    }

    public void deactivatePosition(String ma) {
        ChucVu cv = chucVuDAO.findById(ma);
        if (cv == null) throw new IllegalArgumentException("Khong tim thay chuc vu.");

        if ("ngung_hoat_dong".equals(cv.getTrangThai()))
            throw new IllegalArgumentException("Chuc vu nay da ngung hoat dong roi.");

        cv.setTrangThai("ngung_hoat_dong");
        chucVuDAO.update(cv);
    }

    public void activatePosition(String ma) {
        ChucVu cv = chucVuDAO.findById(ma);
        if (cv == null) throw new IllegalArgumentException("Khong tim thay chuc vu.");

        if ("hoatDong".equals(cv.getTrangThai()))
            throw new IllegalArgumentException("Chuc vu nay dang hoat dong roi.");

        cv.setTrangThai("hoatDong");
        chucVuDAO.update(cv);
    }

    private String getCurrentUserName() {
        SessionContext s = SessionContext.getInstance();
        if (s.isLoggedIn() && s.getCurrentUser() != null) {
            String hoTen = s.getCurrentUser().getHoTen();
            if (hoTen != null && !hoTen.isEmpty()) return hoTen;
            return s.getCurrentUser().getTenDangNhap();
        }
        return "Admin";
    }
}