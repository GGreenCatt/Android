package com.example.de2;

// Album.java
public class Album {
    private int id;
    private String name;
    private String topic;
    private byte[] image; // Đổi kiểu dữ liệu thành byte[]
    private int totalImages;

    public Album(int id, String name, String topic, byte[] image) {
        this.id = id;
        this.name = name;
        this.topic = topic;
        this.image = image;
    }

    // Getter và Setter
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTopic() {
        return topic;
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
}

