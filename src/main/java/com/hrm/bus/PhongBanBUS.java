package com.hrm.bus;

import com.hrm.dao.BoNhiemDAO;
import com.hrm.dao.PhongBanDAO;
import com.hrm.model.DataScope;
import com.hrm.model.PhongBan;
import com.hrm.model.TaiKhoan;
import com.hrm.util.HRMConstants;
import com.hrm.util.OrganizationValidation;
import com.hrm.util.PermissionCodes;
import com.hrm.util.SessionContext;

import java.util.List;

public class PhongBanBUS {

    private final PhongBanDAO phongBanDAO = new PhongBanDAO();
    private final BoNhiemDAO boNhiemDAO = BoNhiemDAO.getInstance();

    public List<PhongBan> getAllDepartments() {
        return phongBanDAO.findAll();
    }

    public List<PhongBan> getActiveDepartments() {
        return phongBanDAO.findActive();
    }

    public PhongBan getById(String ma) {
        return phongBanDAO.findById(ma);
    }

    public void addDepartment(String ma, String ten, String maCha) {
        addDepartment(ma, ten, maCha, "");
    }

    public void addDepartment(String ma, String ten, String maCha, String moTa) {
        validateManagePermission();
        ma  = ma  == null ? "" : ma.trim();
        ten = ten == null ? "" : ten.trim();
        maCha = maCha == null ? "" : maCha.trim();
        moTa = moTa == null ? "" : moTa.trim();

        String loiMa = OrganizationValidation.validateMaPhongBan(ma);
        if (loiMa != null) throw new IllegalArgumentException(loiMa);

        String loiTen = OrganizationValidation.validateTenPhongBan(ten);
        if (loiTen != null) throw new IllegalArgumentException(loiTen);

        if (phongBanDAO.existsById(ma))
            throw new IllegalArgumentException("Ma phong ban '" + ma + "' da ton tai.");

        if (!isEmpty(maCha)) {
            PhongBan cha = phongBanDAO.findById(maCha);
            if (cha == null)
                throw new IllegalArgumentException("Phong ban cha khong ton tai.");
            if (!dangHoatDong(cha.getTrangThai()))
                throw new IllegalArgumentException(
                        "Phong ban cha '" + cha.getTenPhongBan() + "' da ngung hoat dong, khong the them phong ban con.");
        }

        phongBanDAO.save(new PhongBan(ma, ten, isEmpty(maCha) ? null : maCha, moTa, "hoatDong"));
    }

    public void updateDepartment(String ma, String tenMoi, String chaId, String moTa) {
        validateManagePermission();
        tenMoi = tenMoi == null ? "" : tenMoi.trim();
        chaId  = chaId  == null ? "" : chaId.trim();
        moTa   = moTa   == null ? "" : moTa.trim();

        PhongBan pb = phongBanDAO.findById(ma);
        if (pb == null) throw new IllegalArgumentException("Khong tim thay phong ban.");

        String loiTen = OrganizationValidation.validateTenPhongBan(tenMoi);
        if (loiTen != null) throw new IllegalArgumentException(loiTen);

        if (!isEmpty(chaId)) {
            if (chaId.equals(ma))
                throw new IllegalArgumentException("Phong ban khong the la cha cua chinh no.");
            PhongBan cha = phongBanDAO.findById(chaId);
            if (cha == null)
                throw new IllegalArgumentException("Phong ban cha khong ton tai.");
            if (!dangHoatDong(cha.getTrangThai()))
                throw new IllegalArgumentException(
                        "Phong ban cha '" + cha.getTenPhongBan() + "' da ngung hoat dong.");
            if (laConChau(ma, chaId))
                throw new IllegalArgumentException("Khong the chon phong ban con/chau lam cha.");
        }

        pb.setTenPhongBan(tenMoi);
        pb.setPhongBanChaId(isEmpty(chaId) ? null : chaId);
        pb.setMoTa(moTa);
        phongBanDAO.update(pb);
    }

    public void deactivateDepartment(String ma) {
        validateManagePermission();
        PhongBan pb = phongBanDAO.findById(ma);
        if (pb == null) throw new IllegalArgumentException("Khong tim thay phong ban.");

        if (!dangHoatDong(pb.getTrangThai()))
            throw new IllegalArgumentException("Phong ban nay da ngung hoat dong roi.");

        checkKhongConConHoatDong(ma);

        if (boNhiemDAO.hasActiveBoNhiemInDepartment(ma))
            throw new IllegalArgumentException("Phong ban con bo nhiem dang hieu luc. Ket thuc bo nhiem truoc.");

        pb.setTrangThai("ngung_hoat_dong");
        phongBanDAO.update(pb);
    }

    public void activateDepartment(String ma) {
        validateManagePermission();
        PhongBan pb = phongBanDAO.findById(ma);
        if (pb == null) throw new IllegalArgumentException("Khong tim thay phong ban.");

        if (dangHoatDong(pb.getTrangThai()))
            throw new IllegalArgumentException("Phong ban nay dang hoat dong roi.");

        String maCha = pb.getPhongBanChaId();
        if (!isEmpty(maCha)) {
            PhongBan cha = phongBanDAO.findById(maCha);
            if (cha != null && !dangHoatDong(cha.getTrangThai()))
                throw new IllegalArgumentException(
                        "Phong ban cha '" + cha.getTenPhongBan() + "' dang ngung. Kich hoat cha truoc.");
        }

        pb.setTrangThai("hoatDong");
        phongBanDAO.update(pb);
    }

    // Dem quy check toan bo cay con/chau/chat
    private void checkKhongConConHoatDong(String ma) {
        for (PhongBan con : phongBanDAO.findChildren(ma)) {
            if (dangHoatDong(con.getTrangThai()))
                throw new IllegalArgumentException(
                        "Phong ban con '" + con.getTenPhongBan() + "' van dang hoat dong. Ngung phong ban con truoc.");
            checkKhongConConHoatDong(con.getId());
        }
    }

    // Kiem tra quyen quan ly phong ban qua role/quyen load tu DB (khong hardcode ArrayList)
    private void validateManagePermission() {
        TaiKhoan currentUser = SessionContext.getInstance().getCurrentUser();
        if (currentUser == null)
            throw new IllegalArgumentException("Phien dang nhap khong hop le.");
        if (HRMConstants.USERNAME_ADMIN.equalsIgnoreCase(currentUser.getTenDangNhap())
                || currentUser.coVaiTro(HRMConstants.ROLE_ADMIN))
            return;
        if (!currentUser.coQuyen(PermissionCodes.DEPARTMENT_MANAGE))
            throw new IllegalArgumentException("Ban khong co quyen quan ly phong ban.");
        if (XacThucBUS.getInstance().getScopeForAction(PermissionCodes.DEPARTMENT_MANAGE) != DataScope.ALL)
            throw new IllegalArgumentException("Quyen quan ly phong ban yeu cau pham vi ALL.");
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private boolean dangHoatDong(String trangThai) {
        return "hoatDong".equals(trangThai);
    }

    // De quy leo cay tranh chon con/chau lam cha gay vong lap
    private boolean laConChau(String maCha, String maCon) {
        if (maCon == null) return false;
        if (maCon.equals(maCha)) return true;
        PhongBan con = phongBanDAO.findById(maCon);
        if (con == null) return false;
        return laConChau(maCha, con.getPhongBanChaId());
    }
}