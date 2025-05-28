package com.example.de2;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordManager {
    private static final String PREFS_NAME = "HiddenAlbumPrefs";
    private static final String KEY_PASSWORD_HASH = "passwordHash";
    private static final String ALGORITHM = "SHA-256";

    private SharedPreferences sharedPreferences;

    public PasswordManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Hàm mã hóa mật khẩu (đơn giản, không có salt - cân nhắc bảo mật cho ứng dụng thực tế)
    private String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashedBytes = digest.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e("PasswordManager", "Lỗi thuật toán mã hóa: " + ALGORITHM, e);
            return null; // Hoặc throw một exception tùy chỉnh
        }
    }

    public boolean isPasswordSet() {
        return sharedPreferences.getString(KEY_PASSWORD_HASH, null) != null;
    }

    public boolean setPassword(String newPassword) {
        if (newPassword == null || newPassword.length() < 4) { // Ví dụ: yêu cầu mật khẩu tối thiểu 4 ký tự
            Log.w("PasswordManager", "Mật khẩu quá ngắn hoặc null.");
            return false;
        }
        String hashedPassword = hashPassword(newPassword);
        if (hashedPassword != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_PASSWORD_HASH, hashedPassword);
            editor.apply();
            Log.d("PasswordManager", "Mật khẩu đã được đặt/thay đổi.");
            return true;
        }
        return false;
    }

    public boolean checkPassword(String inputPassword) {
        if (!isPasswordSet() || inputPassword == null) {
            return false; // Chưa có mật khẩu hoặc input rỗng
        }
        String storedHash = sharedPreferences.getString(KEY_PASSWORD_HASH, "");
        String inputHash = hashPassword(inputPassword);
        boolean match = storedHash.equals(inputHash);
        Log.d("PasswordManager", "Kiểm tra mật khẩu: " + (match ? "Khớp" : "Không khớp"));
        return match;
    }

    public boolean removePassword() {
        if (isPasswordSet()) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(KEY_PASSWORD_HASH);
            editor.apply();
            Log.d("PasswordManager", "Mật khẩu đã được xóa.");
            return true;
        }
        return false;
    }
}