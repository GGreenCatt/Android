package com.example.de2;

public class Comment {
    private int id;
    private int photoId;
    private String text;
    private String author;
    private long timestamp;

    public Comment(int id, int photoId, String text, String author, long timestamp) {
        this.id = id;
        this.photoId = photoId;
        this.text = text;
        this.author = author;
        this.timestamp = timestamp;
    }

    // Getters (và Setters nếu cần)
    public int getId() {
        return id;
    }

    public int getPhotoId() {
        return photoId;
    }

    public String getText() {
        return text;
    }

    public String getAuthor() {
        return author;
    }

    public long getTimestamp() {
        return timestamp;
    }
}