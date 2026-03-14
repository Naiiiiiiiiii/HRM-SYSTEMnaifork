package com.hrm.model;

public class PhongBan {
    private String id;
    private String tenPhongBan;
    private String phongBanChaId;
    private String moTa;
    private String trangThai;

    public PhongBan() {}

    public PhongBan(String id, String tenPhongBan, String phongBanChaId, String moTa, String trangThai) {
        this.id = id;
        this.tenPhongBan = tenPhongBan;
        this.phongBanChaId = phongBanChaId;
        this.moTa = moTa;
        this.trangThai = trangThai;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenPhongBan() { return tenPhongBan; }
    public void setTenPhongBan(String tenPhongBan) { this.tenPhongBan = tenPhongBan; }
    public String getPhongBanChaId() { return phongBanChaId; }
    public void setPhongBanChaId(String phongBanChaId) { this.phongBanChaId = phongBanChaId; }
    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }

    @Override
    public String toString() { return tenPhongBan + " (" + id + ")"; }
}
