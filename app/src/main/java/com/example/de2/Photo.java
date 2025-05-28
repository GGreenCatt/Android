package com.example.de2;

public class Photo {
    private int id;
    private int albumId;
    private String name; // Tên của ảnh (có thể là mô tả ngắn hoặc tên file)
    private byte[] image; // Dữ liệu byte của ảnh
    private boolean isFavorite; // THÊM TRƯỜNG NÀY để lưu trạng thái yêu thích

    // Constructor cập nhật (có thể có nhiều constructor tùy nhu cầu)
    public Photo(int id, int albumId, String name, byte[] image) {
        this.id = id;
        this.albumId = albumId;
        this.name = name;
        this.image = image;
        this.isFavorite = false; // Mặc định khi tạo mới một đối tượng Photo (không từ DB)
        // Khi đọc từ DB, giá trị này sẽ được set dựa trên cột is_favorite
    }

    // Constructor có thể bao gồm isFavorite (tùy chọn, hữu ích khi tạo object từ DB)
    public Photo(int id, int albumId, String name, byte[] image, boolean isFavorite) {
        this.id = id;
        this.albumId = albumId;
        this.name = name;
        this.image = image;
        this.isFavorite = isFavorite;
    }


    // Getters và Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAlbumId() {
        return albumId;
    }

    public void setAlbumId(int albumId) {
        this.albumId = albumId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    // --- GETTER VÀ SETTER CHO isFavorite ---
    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }
    // --- KẾT THÚC GETTER VÀ SETTER CHO isFavorite ---
}