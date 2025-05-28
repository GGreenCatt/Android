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
    private static final int DATABASE_VERSION = 6; // Giữ nguyên version 6

    // Bảng albums
    private static final String TABLE_ALBUMS = "albums";
    public static final String COLUMN_ALBUM_IS_HIDDEN = "is_hidden";

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
                "image BLOB, " +
                COLUMN_ALBUM_IS_HIDDEN + " INTEGER DEFAULT 0)";
        db.execSQL(createTableAlbums);
        Log.d(TAG, "onCreate: Table " + TABLE_ALBUMS + " created with is_hidden column.");

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
        if (oldVersion < 6 && newVersion >= 6) { // >=6 để xử lý nếu có version 7,8 sau này
            try {
                // Chỉ thêm cột nếu nó chưa tồn tại (kiểm tra cẩn thận nếu có nhiều version)
                // Cách đơn giản nhất là thử thêm, nếu lỗi thì có thể cột đã tồn tại
                // Hoặc, nếu đây là bước nâng cấp cụ thể từ version X (chưa có cột) sang Y (có cột)
                Cursor c = db.rawQuery("PRAGMA table_info(" + TABLE_ALBUMS + ")", null);
                boolean columnExists = false;
                if (c != null) {
                    while (c.moveToNext()) {
                        int nameColIdx = c.getColumnIndex("name");
                        if (nameColIdx != -1 && COLUMN_ALBUM_IS_HIDDEN.equalsIgnoreCase(c.getString(nameColIdx))) {
                            columnExists = true;
                            break;
                        }
                    }
                    c.close();
                }

                if (!columnExists) {
                    db.execSQL("ALTER TABLE " + TABLE_ALBUMS + " ADD COLUMN " + COLUMN_ALBUM_IS_HIDDEN + " INTEGER DEFAULT 0;");
                    Log.i(TAG, "Upgraded " + TABLE_ALBUMS + " table: Added " + COLUMN_ALBUM_IS_HIDDEN + " column.");
                } else {
                    Log.i(TAG, COLUMN_ALBUM_IS_HIDDEN + " column already exists in " + TABLE_ALBUMS + " table.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error adding " + COLUMN_ALBUM_IS_HIDDEN + " column to " + TABLE_ALBUMS + " during upgrade. Falling back to drop/create.", e);
                fallbackToDropAndCreate(db); // Nếu ALTER TABLE lỗi, fallback về xóa và tạo lại
                return;
            }
        } else {
            // Nếu không có logic nâng cấp cụ thể cho các phiên bản này,
            // hoặc nếu oldVersion >= 6 (schema đã có is_hidden trong albums rồi)
            // thì không làm gì ở đây, hoặc nếu cần cho các version sau thì thêm logic
            // Trong trường hợp này, nếu không có thay đổi nào khác, không cần gọi fallback
            // fallbackToDropAndCreate(db); // Chỉ gọi nếu thực sự cần thiết cho các version khác
        }
        // Nếu có các thay đổi schema khác cho các version cũ hơn (ví dụ version 4 lên 5 thêm is_favorite vào photos)
        // thì bạn cần xử lý chúng tuần tự ở đây.
        // Ví dụ:
        // if (oldVersion < 5) { ... logic cho is_favorite ... }
    }

    private void fallbackToDropAndCreate(SQLiteDatabase db) {
        Log.w(TAG, "fallbackToDropAndCreate: Dropping all tables and recreating database.");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COMMENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PHOTOS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ALBUMS);
        onCreate(db);
    }


    public boolean insertAlbum(String name, String topic, byte[] image, boolean isHidden) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("topic", topic);
        values.put("image", image);
        values.put(COLUMN_ALBUM_IS_HIDDEN, isHidden ? 1 : 0);
        long result = db.insert(TABLE_ALBUMS, null, values);
        return result != -1;
    }

    public boolean insertAlbum(String name, String topic, byte[] image) {
        return insertAlbum(name, topic, image, false);
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
            if (cursor.moveToFirst()) { totalImages = cursor.getInt(0); }
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
        try {
            cursor = db.query(TABLE_COMMENTS, null, COLUMN_COMMENT_PHOTO_ID_FK + " = ?",
                    new String[]{String.valueOf(photoId)}, null, null, COLUMN_COMMENT_TIMESTAMP + " ASC");
            if (cursor != null && cursor.moveToFirst()) {
                int idCol = cursor.getColumnIndexOrThrow(COLUMN_COMMENT_ID_PK);
                int textCol = cursor.getColumnIndexOrThrow(COLUMN_COMMENT_TEXT);
                int authorCol = cursor.getColumnIndexOrThrow(COLUMN_COMMENT_AUTHOR);
                int timestampCol = cursor.getColumnIndexOrThrow(COLUMN_COMMENT_TIMESTAMP);
                do {
                    comments.add(new Comment(cursor.getInt(idCol), photoId, cursor.getString(textCol), cursor.getString(authorCol), cursor.getLong(timestampCol)));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) { Log.e(TAG, "Lỗi khi lấy bình luận: " + photoId, e); }
        finally { if (cursor != null) { cursor.close(); } }
        return comments;
    }

    public boolean deleteAlbum(int albumId) {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean success = false;
        try {
            db.beginTransaction();
            int rowsAffected = db.delete(TABLE_ALBUMS, "id = ?", new String[]{String.valueOf(albumId)});
            if (rowsAffected > 0) { db.setTransactionSuccessful(); success = true; }
        } catch (Exception e) { Log.e(TAG, "Lỗi xóa album: " + albumId, e); }
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
        if (!hasChanges) return false;
        int rowsAffected = 0;
        try { rowsAffected = db.update(TABLE_ALBUMS, values, "id = ?", new String[]{String.valueOf(albumId)}); }
        catch (Exception e) { Log.e(TAG, "Lỗi cập nhật album: " + albumId, e); return false; }
        return rowsAffected > 0;
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
        } catch (Exception e) { Log.e(TAG, "Lỗi lấy ảnh bìa album: " + albumId, e); }
        finally { if (cursor != null) { cursor.close(); } }
        return imageBytes;
    }

    public boolean setPhotoFavoriteStatus(int photoId, boolean isFavorite) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PHOTO_IS_FAVORITE, isFavorite ? 1 : 0);
        int rowsAffected = 0;
        try { rowsAffected = db.update(TABLE_PHOTOS, values, "id = ?", new String[]{String.valueOf(photoId)}); }
        catch (Exception e) { Log.e(TAG, "Lỗi cập nhật TT yêu thích ảnh: " + photoId, e); return false; }
        return rowsAffected > 0;
    }

    public List<Photo> getFavoritePhotos() {
        List<Photo> favoritePhotos = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_PHOTOS, null, COLUMN_PHOTO_IS_FAVORITE + " = ?",
                    new String[]{"1"}, null, null, "id DESC");
            if (cursor != null && cursor.moveToFirst()) {
                int idCol = cursor.getColumnIndexOrThrow("id");
                int albumIdCol = cursor.getColumnIndexOrThrow(COLUMN_PHOTO_ALBUM_ID_FK);
                int imageCol = cursor.getColumnIndexOrThrow("image");
                int isFavoriteCol = cursor.getColumnIndexOrThrow(COLUMN_PHOTO_IS_FAVORITE);
                do {
                    favoritePhotos.add(new Photo(cursor.getInt(idCol), cursor.getInt(albumIdCol), "Ảnh ID " + cursor.getInt(idCol), cursor.getBlob(imageCol), cursor.getInt(isFavoriteCol) == 1));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) { Log.e(TAG, "Lỗi lấy ảnh yêu thích", e); }
        finally { if (cursor != null) { cursor.close(); } }
        return favoritePhotos;
    }

    public boolean isPhotoFavorite(int photoId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null; boolean favorite = false;
        try {
            cursor = db.query(TABLE_PHOTOS, new String[]{COLUMN_PHOTO_IS_FAVORITE}, "id = ?",
                    new String[]{String.valueOf(photoId)}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int favoriteCol = cursor.getColumnIndexOrThrow(COLUMN_PHOTO_IS_FAVORITE);
                favorite = cursor.getInt(favoriteCol) == 1;
            }
        } catch (Exception e) { Log.e(TAG, "Lỗi kiểm tra ảnh yêu thích: " + photoId, e); }
        finally { if (cursor != null) { cursor.close(); } }
        return favorite;
    }

    public boolean setAlbumHiddenStatus(int albumId, boolean isHidden) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ALBUM_IS_HIDDEN, isHidden ? 1 : 0);
        Log.d(TAG, "Setting hidden status for albumId " + albumId + " to " + isHidden);
        int rowsAffected = 0;
        try { rowsAffected = db.update(TABLE_ALBUMS, values, "id = ?", new String[]{String.valueOf(albumId)}); }
        catch (Exception e) { Log.e(TAG, "Lỗi khi cập nhật trạng thái ẩn cho album ID: " + albumId, e); return false; }
        return rowsAffected > 0;
    }

    public List<Album> getVisibleAlbums() {
        List<Album> albumList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT id, name, topic, image, " + COLUMN_ALBUM_IS_HIDDEN +
                " FROM " + TABLE_ALBUMS +
                " WHERE " + COLUMN_ALBUM_IS_HIDDEN + " = 0 " +
                " ORDER BY name ASC";
        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int idCol = cursor.getColumnIndexOrThrow("id");
                int nameCol = cursor.getColumnIndexOrThrow("name");
                int topicCol = cursor.getColumnIndexOrThrow("topic");
                int imageCol = cursor.getColumnIndexOrThrow("image");
                int isHiddenCol = cursor.getColumnIndexOrThrow(COLUMN_ALBUM_IS_HIDDEN); // Lấy index

                int id = cursor.getInt(idCol);
                String name = cursor.getString(nameCol);
                String topic = cursor.getString(topicCol);
                byte[] image = cursor.getBlob(imageCol);
                boolean isHidden = cursor.getInt(isHiddenCol) == 1; // Đọc giá trị isHidden

                int totalImages = getTotalImages(id);
                // Sử dụng constructor của Album đã có isHidden
                Album album = new Album(id, name, topic, image, isHidden);
                album.setTotalImages(totalImages);
                albumList.add(album);
            } while (cursor.moveToNext());
        }
        if (cursor != null) { cursor.close(); }
        Log.d(TAG, "getVisibleAlbums: Loaded " + albumList.size() + " visible albums.");
        return albumList;
    }

    public List<Album> getHiddenAlbums() {
        List<Album> albumList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT id, name, topic, image, " + COLUMN_ALBUM_IS_HIDDEN +
                " FROM " + TABLE_ALBUMS +
                " WHERE " + COLUMN_ALBUM_IS_HIDDEN + " = 1 " +
                " ORDER BY name ASC";
        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int idCol = cursor.getColumnIndexOrThrow("id");
                int nameCol = cursor.getColumnIndexOrThrow("name");
                int topicCol = cursor.getColumnIndexOrThrow("topic");
                int imageCol = cursor.getColumnIndexOrThrow("image");
                int isHiddenCol = cursor.getColumnIndexOrThrow(COLUMN_ALBUM_IS_HIDDEN); // Lấy index

                int id = cursor.getInt(idCol);
                String name = cursor.getString(nameCol);
                String topic = cursor.getString(topicCol);
                byte[] image = cursor.getBlob(imageCol);
                boolean isHidden = cursor.getInt(isHiddenCol) == 1; // Đọc giá trị isHidden (sẽ là true)

                int totalImages = getTotalImages(id);
                // Sử dụng constructor của Album đã có isHidden
                Album album = new Album(id, name, topic, image, isHidden);
                album.setTotalImages(totalImages);
                albumList.add(album);
            } while (cursor.moveToNext());
        }
        if (cursor != null) { cursor.close(); }
        Log.d(TAG, "getHiddenAlbums: Loaded " + albumList.size() + " hidden albums.");
        return albumList;
    }
}