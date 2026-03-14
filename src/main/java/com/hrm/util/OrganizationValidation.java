package com.hrm.util;

import java.util.regex.Pattern;

/**
 * Shared validation rules for organization modules (PhongBan/ChucVu).
 * Methods return null when input is valid, otherwise an error message.
 */
public final class OrganizationValidation {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{2,20}$");

    private OrganizationValidation() {
    }

    public static String validateMaPhongBan(String maPhongBan) {
        if (maPhongBan == null || maPhongBan.isBlank()) {
            return "Ma phong ban khong duoc de trong";
        }
        String value = maPhongBan.trim();
        return CODE_PATTERN.matcher(value).matches()
                ? null
                : "Ma phong ban chi duoc gom chu, so, '_' hoac '-', do dai 2-20 ky tu";
    }

    public static String validateTenPhongBan(String tenPhongBan) {
        if (tenPhongBan == null || tenPhongBan.isBlank()) {
            return "Ten phong ban khong duoc de trong";
        }
        String value = tenPhongBan.trim();
        if (value.length() < 2 || value.length() > 100) {
            return "Ten phong ban phai tu 2 den 100 ky tu";
        }
        return null;
    }

    public static String validateMaChucVu(String maChucVu) {
        if (maChucVu == null || maChucVu.isBlank()) {
            return "Ma chuc vu khong duoc de trong";
        }
        String value = maChucVu.trim();
        return CODE_PATTERN.matcher(value).matches()
                ? null
                : "Ma chuc vu chi duoc gom chu, so, '_' hoac '-', do dai 2-20 ky tu";
    }

    public static String validateTenChucVu(String tenChucVu) {
        if (tenChucVu == null || tenChucVu.isBlank()) {
            return "Ten chuc vu khong duoc de trong";
        }
        String value = tenChucVu.trim();
        if (value.length() < 2 || value.length() > 100) {
            return "Ten chuc vu phai tu 2 den 100 ky tu";
        }
        return null;
    }

    public static String validateHeSoLuong(double heSoLuong) {
        if (Double.isNaN(heSoLuong) || Double.isInfinite(heSoLuong)) {
            return "He so luong khong hop le";
        }
        if (heSoLuong <= 0) {
            return "He so luong phai lon hon 0";
        }
        if (heSoLuong > 100) {
            return "He so luong vuot qua gioi han cho phep";
        }
        return null;
    }

    public static String validateCapBac(int capBac) {
        if (capBac <= 0) {
            return "Cap bac phai lon hon 0";
        }
        if (capBac > 20) {
            return "Cap bac vuot qua gioi han cho phep";
        }
        return null;
    }

    public static String validatePhuCap(double phuCap) {
        if (Double.isNaN(phuCap) || Double.isInfinite(phuCap)) {
            return "Phu cap khong hop le";
        }
        if (phuCap < 0) {
            return "Phu cap khong duoc am";
        }
        if (phuCap > 1_000_000_000d) {
            return "Phu cap vuot qua gioi han cho phep";
        }
        return null;
    }
}