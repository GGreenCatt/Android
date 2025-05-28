package com.example.de2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";

    private static final String DATABASE_NAME = "AlbumDB.db";
    private static final int DATABASE_VERSION = 5; // Giữ nguyên version 5

    // Bảng albums
    private static final String TABLE_ALBUMS = "albums";
    // Các cột: id, name, topic, image

    // Bảng photos
    private static final String TABLE_PHOTOS = "photos";
    private static final String COLUMN_PHOTO_ALBUM_ID_FK = "album_id";
    public static final String COLUMN_PHOTO_IS_FAVORITE = "is_favorite";

    // Bảng comments
    public static final String TABLE_COMMENTS = "comments";
    public static final String COLUMN_COMMENT_ID_PK = "comment_id";
    public static final String COLUMN_COMMENT_PHOTO_ID_FK = "photo_id";
    public static final String COLUMN_COMMENT_TEXT = "comment_text";
    public static final String COLUMN_COMMENT_AUTHOR = "comment_author";
    public static final String COLUMN_COMMENT_TIMESTAMP = "comment_timestamp";


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "onCreate: Creating database tables (version " + DATABASE_VERSION + ")");
        String createTableAlbums = "CREATE TABLE IF NOT EXISTS " + TABLE_ALBUMS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, " +
                "topic TEXT, " +
                "image BLOB)";
        db.execSQL(createTableAlbums);
        Log.d(TAG, "onCreate: Table " + TABLE_ALBUMS + " created.");

        String createTablePhotos = "CREATE TABLE IF NOT EXISTS " + TABLE_PHOTOS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_PHOTO_ALBUM_ID_FK + " INTEGER, " +
                "image BLOB, " +
                COLUMN_PHOTO_IS_FAVORITE + " INTEGER DEFAULT 0, " +
                "FOREIGN KEY (" + COLUMN_PHOTO_ALBUM_ID_FK + ") REFERENCES " + TABLE_ALBUMS + "(id) ON DELETE CASCADE)";
        db.execSQL(createTablePhotos);
        Log.d(TAG, "onCreate: Table " + TABLE_PHOTOS + " created with is_favorite column.");

        String createTableComments = "CREATE TABLE IF NOT EXISTS " + TABLE_COMMENTS + " (" +
                COLUMN_COMMENT_ID_PK + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_COMMENT_PHOTO_ID_FK + " INTEGER," +
                COLUMN_COMMENT_TEXT + " TEXT NOT NULL," +
                COLUMN_COMMENT_AUTHOR + " TEXT," +
                COLUMN_COMMENT_TIMESTAMP + " INTEGER, " +
                "FOREIGN KEY(" + COLUMN_COMMENT_PHOTO_ID_FK + ") REFERENCES " + TABLE_PHOTOS + "(id) ON DELETE CASCADE)";
        db.execSQL(createTableComments);
        Log.d(TAG, "onCreate: Table " + TABLE_COMMENTS + " created.");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        // Với version 5, nếu bạn nâng cấp từ version < 5 và muốn giữ dữ liệu,
        // bạn cần ALTER TABLE photos ADD COLUMN is_favorite.
        // Hiện tại, logic này sẽ xóa và tạo lại.
        if (oldVersion < 5 && newVersion == 5) {
            // Đây là nơi bạn có thể thêm lệnh ALTER TABLE nếu muốn giữ dữ liệu cũ
            // Ví dụ:
            // try {
            //    if (oldVersion < 4) { // Nếu chưa có bảng comments
            //        // Tạo bảng comments (logic từ onCreate)
            //        String createTableComments = "CREATE TABLE IF NOT EXISTS " + TABLE_COMMENTS + " ( ... )";
            //        db.execSQL(createTableComments);
            //    }
            //    db.execSQL("ALTER TABLE " + TABLE_PHOTOS + " ADD COLUMN " + COLUMN_PHOTO_IS_FAVORITE + " INTEGER DEFAULT 0;");
            //    Log.i(TAG, "Upgraded " + TABLE_PHOTOS + " table to version " + newVersion + " by adding " + COLUMN_PHOTO_IS_FAVORITE + " column.");
            // } catch (Exception e) {
            //    Log.e(TAG, "Error upgrading database, falling back to drop/create", e);
            //    fallbackToDropAndCreate(db); // Nếu lỗi thì xóa và tạo lại
            // }
            // Nếu bạn chọn cách đơn giản là xóa và tạo lại cho mọi nâng cấp version:
            fallbackToDropAndCreate(db);
        } else {
            // Xử lý các kịch bản nâng cấp khác nếu có
            fallbackToDropAndCreate(db);
        }
    }

    private void fallbackToDropAndCreate(SQLiteDatabase db) {
        Log.w(TAG, "fallbackToDropAndCreate: Dropping all tables and recreating database.");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COMMENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PHOTOS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ALBUMS);
        onCreate(db);
    }

    public boolean insertAlbum(String name, String topic, byte[] image) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("topic", topic);
        values.put("image", image);
        long result = db.insert(TABLE_ALBUMS, null, values);
        return result != -1;
    }

    public boolean insertImage(int albumId, byte[] image, boolean isFavorite) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PHOTO_ALBUM_ID_FK, albumId);
        values.put("image", image);
        values.put(COLUMN_PHOTO_IS_FAVORITE, isFavorite ? 1 : 0);
        long result = db.insert(TABLE_PHOTOS, null, values);
        return result != -1;
    }

    public boolean insertImage(int albumId, byte[] image) {
        return insertImage(albumId, image, false);
    }

    public int getTotalImages(int albumId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + TABLE_PHOTOS + " WHERE " + COLUMN_PHOTO_ALBUM_ID_FK + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(albumId)});
        int totalImages = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                totalImages = cursor.getInt(0);
            }
            cursor.close();
        }
        return totalImages;
    }

    public boolean addComment(int photoId, String commentText, String author) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_COMMENT_PHOTO_ID_FK, photoId);
        values.put(COLUMN_COMMENT_TEXT, commentText);
        values.put(COLUMN_COMMENT_AUTHOR, author);
        values.put(COLUMN_COMMENT_TIMESTAMP, System.currentTimeMillis());
        long result = db.insert(TABLE_COMMENTS, null, values);
        Log.d(TAG, "addComment result: " + result + " for photoId: " + photoId);
        return result != -1;
    }

    public List<Comment> getCommentsForPhoto(int photoId) {
        List<Comment> comments = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        Log.d(TAG, "Getting comments for photoId: " + photoId);
        try {
            cursor = db.query(
                    TABLE_COMMENTS, null, COLUMN_COMMENT_PHOTO_ID_FK + " = ?",
                    new String[]{String.valueOf(photoId)}, null, null, COLUMN_COMMENT_TIMESTAMP + " ASC"
            );
            if (cursor != null && cursor.moveToFirst()) {
                int idCol = cursor.getColumnIndexOrThrow(COLUMN_COMMENT_ID_PK);
                int textCol = cursor.getColumnIndexOrThrow(COLUMN_COMMENT_TEXT);
                int authorCol = cursor.getColumnIndexOrThrow(COLUMN_COMMENT_AUTHOR);
                int timestampCol = cursor.getColumnIndexOrThrow(COLUMN_COMMENT_TIMESTAMP);
                do {
                    int id = cursor.getInt(idCol);
                    String text = cursor.getString(textCol);
                    String author = cursor.getString(authorCol);
                    long timestamp = cursor.getLong(timestampCol);
                    comments.add(new Comment(id, photoId, text, author, timestamp));
                } while (cursor.moveToNext());
                Log.d(TAG, "Found " + comments.size() + " comments for photoId: " + photoId);
            } else {
                Log.d(TAG, "No comments found or cursor is null for photoId: " + photoId);
            }
        } catch (Exception e) { Log.e(TAG, "Lỗi khi lấy bình luận: ", e); }
        finally { if (cursor != null) { cursor.close(); } }
        return comments;
    }

    public boolean deleteAlbum(int albumId) {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean success = false;
        Log.d(TAG, "Attempting to delete album with ID: " + albumId);
        try {
            db.beginTransaction();
            int rowsAffected = db.delete(TABLE_ALBUMS, "id = ?", new String[]{String.valueOf(albumId)});
            if (rowsAffected > 0) {
                db.setTransactionSuccessful(); success = true;
                Log.d(TAG, "Successfully deleted album ID: " + albumId);
            } else { Log.w(TAG, "No album found with ID: " + albumId + " to delete.");}
        } catch (Exception e) { Log.e(TAG, "Error deleting album ID: " + albumId, e); }
        finally { db.endTransaction(); }
        return success;
    }

    public boolean updateAlbum(int albumId, String newName, String newTopic, byte[] newImage) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        boolean hasChanges = false;
        if (newName != null) { values.put("name", newName); hasChanges = true; }
        if (newTopic != null) { values.put("topic", newTopic); hasChanges = true; }
        if (newImage != null && newImage.length > 0) { values.put("image", newImage); hasChanges = true; }
        if (!hasChanges) { Log.d(TAG, "Không có thông tin cập nhật cho album ID: " + albumId); return false; }
        Log.d(TAG, "Đang cập nhật album ID: " + albumId + " với: " + values.toString());
        int rowsAffected = 0;
        try { rowsAffected = db.update(TABLE_ALBUMS, values, "id = ?", new String[]{String.valueOf(albumId)}); }
        catch (Exception e) { Log.e(TAG, "Lỗi khi cập nhật album ID: " + albumId, e); return false; }
        if (rowsAffected > 0) { Log.d(TAG, "Cập nhật thành công album ID: " + albumId); return true; }
        else { Log.w(TAG, "Không có album được cập nhật cho ID: " + albumId); return false; }
    }

    public byte[] getAlbumCoverImage(int albumId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null; byte[] imageBytes = null;
        try {
            cursor = db.query(TABLE_ALBUMS, new String[]{"image"}, "id = ?", new String[]{String.valueOf(albumId)}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int imageCol = cursor.getColumnIndexOrThrow("image");
                if (!cursor.isNull(imageCol)) { imageBytes = cursor.getBlob(imageCol); }
            }
        } catch (Exception e) { Log.e(TAG, "Lỗi lấy ảnh bìa cho album ID: " + albumId, e); }
        finally { if (cursor != null) { cursor.close(); } }
        return imageBytes;
    }

    // ===== PHƯƠNG THỨC CHO TÍNH NĂNG "YÊU THÍCH" =====

    public boolean setPhotoFavoriteStatus(int photoId, boolean isFavorite) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PHOTO_IS_FAVORITE, isFavorite ? 1 : 0);
        Log.d(TAG, "Setting favorite status for photoId " + photoId + " to " + isFavorite);
        int rowsAffected = 0;
        try {
            rowsAffected = db.update(TABLE_PHOTOS, values, "id = ?", new String[]{String.valueOf(photoId)});
        } catch (Exception e) { Log.e(TAG, "Lỗi khi cập nhật trạng thái yêu thích cho ảnh ID: " + photoId, e); return false; }
        if (rowsAffected > 0) { Log.d(TAG, "Cập nhật trạng thái yêu thích thành công cho ảnh ID: " + photoId); return true; }
        else { Log.w(TAG, "Không cập nhật được TT yêu thích cho ảnh ID: " + photoId + ". Ảnh không tồn tại hoặc TT không đổi."); return false; }
    }

    public List<Photo> getFavoritePhotos() {
        List<Photo> favoritePhotos = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        Log.d(TAG, "Đang lấy danh sách ảnh yêu thích...");
        try {
            cursor = db.query(TABLE_PHOTOS, null, COLUMN_PHOTO_IS_FAVORITE + " = ?",
                    new String[]{"1"}, null, null, "id DESC");
            if (cursor != null && cursor.moveToFirst()) {
                int idCol = cursor.getColumnIndexOrThrow("id");
                int albumIdCol = cursor.getColumnIndexOrThrow(COLUMN_PHOTO_ALBUM_ID_FK);
                int imageCol = cursor.getColumnIndexOrThrow("image");
                int isFavoriteCol = cursor.getColumnIndexOrThrow(COLUMN_PHOTO_IS_FAVORITE);
                do {
                    int id = cursor.getInt(idCol);
                    int albumId = cursor.getInt(albumIdCol);
                    byte[] image = cursor.getBlob(imageCol);
                    boolean isFavorite = cursor.getInt(isFavoriteCol) == 1;
                    Photo photo = new Photo(id, albumId, "Ảnh ID " + id, image, isFavorite); // Dùng constructor có isFavorite
                    favoritePhotos.add(photo);
                } while (cursor.moveToNext());
                Log.d(TAG, "Tìm thấy " + favoritePhotos.size() + " ảnh yêu thích.");
            } else { Log.d(TAG, "Không tìm thấy ảnh yêu thích nào hoặc cursor rỗng."); }
        } catch (Exception e) { Log.e(TAG, "Lỗi khi lấy danh sách ảnh yêu thích: ", e); }
        finally { if (cursor != null) { cursor.close(); } }
        return favoritePhotos;
    }

    /**
     * Kiểm tra xem một ảnh cụ thể có được đánh dấu là yêu thích không.
     * @param photoId ID của ảnh cần kiểm tra.
     * @return true nếu ảnh được yêu thích, false nếu không hoặc không tìm thấy ảnh.
     */
    public boolean isPhotoFavorite(int photoId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        boolean favorite = false;
        Log.d(TAG, "Kiểm tra trạng thái yêu thích cho photoId: " + photoId);
        try {
            cursor = db.query(TABLE_PHOTOS,
                    new String[]{COLUMN_PHOTO_IS_FAVORITE},
                    "id = ?",
                    new String[]{String.valueOf(photoId)},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int favoriteCol = cursor.getColumnIndexOrThrow(COLUMN_PHOTO_IS_FAVORITE);
                favorite = cursor.getInt(favoriteCol) == 1;
                Log.d(TAG, "Trạng thái yêu thích cho photoId " + photoId + " là: " + favorite);
            } else {
                Log.d(TAG, "Không tìm thấy ảnh hoặc cursor rỗng cho photoId: " + photoId + " khi kiểm tra yêu thích.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi kiểm tra isPhotoFavorite cho photoId: " + photoId, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return favorite;
    }
}