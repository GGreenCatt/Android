package com.example.de2;

// Album.java
public class Album {
    private int id;
    private String name;
    private String topic;
    private byte[] image;
    private int totalImages;
    private boolean isHidden; // THÊM TRƯỜNG NÀY để lưu trạng thái ẩn

    // Constructor cơ bản
    public Album(int id, String name, String topic, byte[] image) {
        this.id = id;
        this.name = name;
        this.topic = topic;
        this.image = image;
        this.isHidden = false; // Mặc định khi tạo mới một đối tượng Album (không từ DB)
        // hoặc khi chưa có thông tin ẩn, album sẽ không bị ẩn.
    }

    // (Tùy chọn) Constructor đầy đủ hơn, bao gồm cả isHidden (hữu ích khi tạo từ database)
    public Album(int id, String name, String topic, byte[] image, boolean isHidden) {
        this.id = id;
        this.name = name;
        this.topic = topic;
        this.image = image;
        this.isHidden = isHidden; // Gán trạng thái ẩn được truyền vào
    }

    // Getter và Setter
    public int getId() {
        return id;
    }

    public void setId(int id) { // Thêm setter cho id nếu cần
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) { // Thêm setter cho name nếu cần
        this.name = name;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) { // Thêm setter cho topic nếu cần
        this.topic = topic;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public int getTotalImages() {
        return totalImages;
    }

    public void setTotalImages(int totalImages) {
        this.totalImages = totalImages;
    }

    // --- GETTER VÀ SETTER CHO isHidden ---
    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(boolean hidden) {
        isHidden = hidden;
    }
    // --- KẾT THÚC GETTER VÀ SETTER CHO isHidden ---
}