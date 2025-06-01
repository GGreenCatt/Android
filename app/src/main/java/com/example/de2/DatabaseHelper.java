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

    private static final String DATABASE_NAME = "AlbumDBv3.db";
    // Increment version to trigger onUpgrade
    private static final int DATABASE_VERSION = 8;

    // Table and column names (ensure these are all public as used elsewhere)
    public static final String TABLE_ALBUMS = "albums";
    public static final String COLUMN_ALBUM_ID_PK = "id";
    public static final String COLUMN_ALBUM_NAME = "name";
    public static final String COLUMN_ALBUM_TOPIC = "topic";
    public static final String COLUMN_ALBUM_IMAGE = "image";
    public static final String COLUMN_ALBUM_IS_HIDDEN = "is_hidden";

    public static final String TABLE_PHOTOS = "photos";
    public static final String COLUMN_PHOTO_ID_PK = "id";
    public static final String COLUMN_PHOTO_ALBUM_ID_FK = "album_id";
    public static final String COLUMN_PHOTO_NAME = "name";
    public static final String COLUMN_PHOTO_IMAGE_DATA = "image";
    public static final String COLUMN_PHOTO_IS_FAVORITE = "is_favorite";

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
        Log.i(TAG, "onCreate: Creating database tables for version " + DATABASE_VERSION);
        String createTableAlbums = "CREATE TABLE " + TABLE_ALBUMS + " (" +
                COLUMN_ALBUM_ID_PK + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_ALBUM_NAME + " TEXT, " +
                COLUMN_ALBUM_TOPIC + " TEXT, " +
                COLUMN_ALBUM_IMAGE + " BLOB, " +
                COLUMN_ALBUM_IS_HIDDEN + " INTEGER DEFAULT 0)";
        db.execSQL(createTableAlbums);
        Log.d(TAG, "onCreate: Table " + TABLE_ALBUMS + " created.");

        String createTablePhotos = "CREATE TABLE " + TABLE_PHOTOS + " (" +
                COLUMN_PHOTO_ID_PK + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_PHOTO_ALBUM_ID_FK + " INTEGER, " +
                COLUMN_PHOTO_NAME + " TEXT, " +
                COLUMN_PHOTO_IMAGE_DATA + " BLOB, " +
                COLUMN_PHOTO_IS_FAVORITE + " INTEGER DEFAULT 0, " +
                "FOREIGN KEY (" + COLUMN_PHOTO_ALBUM_ID_FK + ") REFERENCES " + TABLE_ALBUMS + "(" + COLUMN_ALBUM_ID_PK + ") ON DELETE CASCADE)";
        db.execSQL(createTablePhotos);
        Log.d(TAG, "onCreate: Table " + TABLE_PHOTOS + " created.");

        String createTableComments = "CREATE TABLE " + TABLE_COMMENTS + " (" +
                COLUMN_COMMENT_ID_PK + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_COMMENT_PHOTO_ID_FK + " INTEGER," +
                COLUMN_COMMENT_TEXT + " TEXT NOT NULL," +
                COLUMN_COMMENT_AUTHOR + " TEXT," +
                COLUMN_COMMENT_TIMESTAMP + " INTEGER, " +
                "FOREIGN KEY(" + COLUMN_COMMENT_PHOTO_ID_FK + ") REFERENCES " + TABLE_PHOTOS + "(" + COLUMN_PHOTO_ID_PK + ") ON DELETE CASCADE)";
        db.execSQL(createTableComments);
        Log.d(TAG, "onCreate: Table " + TABLE_COMMENTS + " created.");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

        // It's good practice to make upgrade steps idempotent.
        // The columnExists helper can be used, or simply try-catch ALTER TABLE.

        if (oldVersion < 5) {
            Log.d(TAG, "Applying upgrades for DB version < 5");
            if (!columnExists(db, TABLE_PHOTOS, COLUMN_PHOTO_IS_FAVORITE)) {
                try {
                    db.execSQL("ALTER TABLE " + TABLE_PHOTOS + " ADD COLUMN " + COLUMN_PHOTO_IS_FAVORITE + " INTEGER DEFAULT 0;");
                    Log.i(TAG, "Upgraded " + TABLE_PHOTOS + ": Added " + COLUMN_PHOTO_IS_FAVORITE);
                } catch (Exception e) {
                    Log.e(TAG, "Error adding " + COLUMN_PHOTO_IS_FAVORITE + " to " + TABLE_PHOTOS, e);
                }
            }
        }

        if (oldVersion < 6) {
            Log.d(TAG, "Applying upgrades for DB version < 6");
            if (!columnExists(db, TABLE_ALBUMS, COLUMN_ALBUM_IS_HIDDEN)) {
                try {
                    db.execSQL("ALTER TABLE " + TABLE_ALBUMS + " ADD COLUMN " + COLUMN_ALBUM_IS_HIDDEN + " INTEGER DEFAULT 0;");
                    Log.i(TAG, "Upgraded " + TABLE_ALBUMS + ": Added " + COLUMN_ALBUM_IS_HIDDEN);
                } catch (Exception e) {
                    Log.e(TAG, "Error adding " + COLUMN_ALBUM_IS_HIDDEN + " to " + TABLE_ALBUMS, e);
                }
            }
            // This is the critical column from your error log
            if (!columnExists(db, TABLE_PHOTOS, COLUMN_PHOTO_NAME)) {
                try {
                    db.execSQL("ALTER TABLE " + TABLE_PHOTOS + " ADD COLUMN " + COLUMN_PHOTO_NAME + " TEXT;");
                    Log.i(TAG, "Upgraded " + TABLE_PHOTOS + ": Added " + COLUMN_PHOTO_NAME);
                } catch (Exception e) {
                    Log.e(TAG, "Error adding " + COLUMN_PHOTO_NAME + " to " + TABLE_PHOTOS, e);
                }
            }
        }

        // Add more "if (oldVersion < X)" blocks here for future schema changes
        // Example for version 7, though we are using it here to ensure version 6 changes apply
        if (oldVersion < 7) {
            Log.d(TAG, "Applying upgrades for DB version < 7 (includes safeguards for earlier versions)");
            // Safeguard: Re-check critical columns if upgrading from a version that should have had them
            if (!columnExists(db, TABLE_PHOTOS, COLUMN_PHOTO_NAME)) {
                Log.w(TAG, "Safeguard: Column " + COLUMN_PHOTO_NAME + " was missing. Adding it now.");
                try {
                    db.execSQL("ALTER TABLE " + TABLE_PHOTOS + " ADD COLUMN " + COLUMN_PHOTO_NAME + " TEXT;");
                    Log.i(TAG, "Safeguard Upgraded " + TABLE_PHOTOS + ": Added " + COLUMN_PHOTO_NAME);
                } catch (Exception e) {
                    Log.e(TAG, "Error in safeguard adding " + COLUMN_PHOTO_NAME + " to " + TABLE_PHOTOS, e);
                }
            }
            if (!columnExists(db, TABLE_PHOTOS, COLUMN_PHOTO_IS_FAVORITE)) {
                try {
                    db.execSQL("ALTER TABLE " + TABLE_PHOTOS + " ADD COLUMN " + COLUMN_PHOTO_IS_FAVORITE + " INTEGER DEFAULT 0;");
                    Log.i(TAG, "Safeguard Upgraded " + TABLE_PHOTOS + ": Added " + COLUMN_PHOTO_IS_FAVORITE);
                } catch (Exception e) {
                    Log.e(TAG, "Error in safeguard adding " + COLUMN_PHOTO_IS_FAVORITE + " to " + TABLE_PHOTOS, e);
                }
            }
            if (!columnExists(db, TABLE_ALBUMS, COLUMN_ALBUM_IS_HIDDEN)) {
                try {
                    db.execSQL("ALTER TABLE " + TABLE_ALBUMS + " ADD COLUMN " + COLUMN_ALBUM_IS_HIDDEN + " INTEGER DEFAULT 0;");
                    Log.i(TAG, "Safeguard Upgraded " + TABLE_ALBUMS + ": Added " + COLUMN_ALBUM_IS_HIDDEN);
                } catch (Exception e) {
                    Log.e(TAG, "Error in safeguard adding " + COLUMN_ALBUM_IS_HIDDEN + " to " + TABLE_ALBUMS, e);
                }
            }
        }
        // Note: If onUpgrade becomes very complex or if an ALTER TABLE fails catastrophically,
        // you might consider a fallback like dropping and recreating tables, but this loses data.
        // For this specific error, ensuring the ALTER TABLE for COLUMN_PHOTO_NAME executes is key.
    }

    /**
     * Helper method to check if a column exists in a table.
     * @param db The SQLiteDatabase.
     * @param tableName The name of the table.
     * @param columnName The name of the column.
     * @return true if the column exists, false otherwise.
     */
    private boolean columnExists(SQLiteDatabase db, String tableName, String columnName) {
        Cursor cursor = null;
        try {
            // Query with LIMIT 0 to check schema without fetching data
            cursor = db.query(tableName, null, null, null, null, null, null, "0");
            // cursor = db.rawQuery("SELECT * FROM " + tableName + " LIMIT 0", null); // Alternative way
            if (cursor != null) {
                return cursor.getColumnIndex(columnName) != -1;
            }
        } catch (Exception e) {
            // This can happen if the table itself doesn't exist, which is fine in some onUpgrade scenarios
            Log.d(TAG, "columnExists: Error checking column " + columnName + " in " + tableName + " (may be expected if table is new): " + e.getMessage());
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return false;
    }

    private void fallbackToDropAndCreate(SQLiteDatabase db) { // Use with extreme caution
        Log.e(TAG, "fallbackToDropAndCreate: Dropping all tables and recreating database. ALL DATA WILL BE LOST.");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COMMENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PHOTOS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ALBUMS);
        onCreate(db);
    }

    // ... (rest of your DatabaseHelper methods: insertAlbum, insertImage, getPhotoById, etc.)
    // Ensure these methods are consistent with the column names defined above.
    // For example, getPhotoById and any method reading from 'photos' should expect 'COLUMN_PHOTO_NAME'.

    public boolean insertAlbum(String name, String topic, byte[] image, boolean isHidden) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ALBUM_NAME, name);
        values.put(COLUMN_ALBUM_TOPIC, topic);
        values.put(COLUMN_ALBUM_IMAGE, image);
        values.put(COLUMN_ALBUM_IS_HIDDEN, isHidden ? 1 : 0);
        long result = db.insert(TABLE_ALBUMS, null, values);
        return result != -1;
    }

    public boolean insertAlbum(String name, String topic, byte[] image) {
        return insertAlbum(name, topic, image, false);
    }


    public boolean insertImage(int albumId, String photoName, byte[] imageBytes, boolean isFavorite) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PHOTO_ALBUM_ID_FK, albumId);
        values.put(COLUMN_PHOTO_NAME, photoName);
        values.put(COLUMN_PHOTO_IMAGE_DATA, imageBytes);
        values.put(COLUMN_PHOTO_IS_FAVORITE, isFavorite ? 1 : 0);
        long result = db.insert(TABLE_PHOTOS, null, values);
        Log.d(TAG, "insertImage result: " + result + " for albumId: " + albumId);
        return result != -1;
    }

    public boolean insertImage(int albumId, String photoName, byte[] imageBytes) {
        return insertImage(albumId, photoName, imageBytes, false);
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
        } catch (Exception e) { Log.e(TAG, "Lỗi khi lấy bình luận cho photoId: " + photoId, e); }
        finally { if (cursor != null) { cursor.close(); } }
        return comments;
    }

    public boolean deleteAlbum(int albumId) {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean success = false;
        try {
            db.beginTransaction();
            int rowsAffected = db.delete(TABLE_ALBUMS, COLUMN_ALBUM_ID_PK + " = ?", new String[]{String.valueOf(albumId)});
            if (rowsAffected > 0) {
                db.setTransactionSuccessful();
                success = true;
                Log.d(TAG, "Đã xóa thành công album ID: " + albumId + " và các dữ liệu liên quan.");
            } else {
                Log.w(TAG, "Không tìm thấy album ID: " + albumId + " để xóa.");
            }
        } catch (Exception e) { Log.e(TAG, "Lỗi khi xóa album ID: " + albumId, e); }
        finally { db.endTransaction(); }
        return success;
    }

    public boolean deletePhoto(int photoId) {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean success = false;
        Log.d(TAG, "Attempting to delete photo with ID: " + photoId);
        try {
            db.beginTransaction();
            int rowsAffected = db.delete(TABLE_PHOTOS, COLUMN_PHOTO_ID_PK + " = ?", new String[]{String.valueOf(photoId)});
            if (rowsAffected > 0) {
                Log.d(TAG, "Successfully deleted photo ID: " + photoId + ". Rows affected: " + rowsAffected);
                db.setTransactionSuccessful();
                success = true;
            } else {
                Log.w(TAG, "No photo found with ID: " + photoId + " to delete.");
            }
        } catch (Exception e) { Log.e(TAG, "Error deleting photo ID: " + photoId, e); }
        finally { db.endTransaction(); }
        return success;
    }


    public boolean updateAlbum(int albumId, String newName, String newTopic, byte[] newImage) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        boolean hasChanges = false;

        if (newName != null) {
            values.put(COLUMN_ALBUM_NAME, newName);
            hasChanges = true;
        }
        if (newTopic != null) {
            values.put(COLUMN_ALBUM_TOPIC, newTopic);
            hasChanges = true;
        }
        if (newImage != null && newImage.length > 0) {
            values.put(COLUMN_ALBUM_IMAGE, newImage);
            hasChanges = true;
        }

        if (!hasChanges) {
            Log.d(TAG, "Không có thông tin cập nhật cho album ID: " + albumId);
            return false;
        }

        int rowsAffected = 0;
        try {
            rowsAffected = db.update(TABLE_ALBUMS, values, COLUMN_ALBUM_ID_PK + " = ?", new String[]{String.valueOf(albumId)});
            Log.d(TAG, "Cập nhật album ID: " + albumId + ". Số dòng bị ảnh hưởng: " + rowsAffected);
        } catch (Exception e) {
            Log.e(TAG, "Lỗi cập nhật album ID: " + albumId, e);
            return false;
        }
        return rowsAffected > 0;
    }

    public boolean updatePhotoName(int photoId, String newName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PHOTO_NAME, newName);

        int rowsAffected = 0;
        try {
            rowsAffected = db.update(TABLE_PHOTOS, values, COLUMN_PHOTO_ID_PK + " = ?", new String[]{String.valueOf(photoId)});
            Log.d(TAG, "Cập nhật tên ảnh ID: " + photoId + ". Tên mới: " + newName + ". Số dòng bị ảnh hưởng: " + rowsAffected);
        } catch (Exception e) {
            Log.e(TAG, "Lỗi cập nhật tên ảnh ID: " + photoId, e);
            return false;
        }
        return rowsAffected > 0;
    }


    public byte[] getAlbumCoverImage(int albumId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null; byte[] imageBytes = null;
        try {
            cursor = db.query(TABLE_ALBUMS, new String[]{COLUMN_ALBUM_IMAGE}, COLUMN_ALBUM_ID_PK + " = ?", new String[]{String.valueOf(albumId)}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int imageCol = cursor.getColumnIndexOrThrow(COLUMN_ALBUM_IMAGE);
                if (!cursor.isNull(imageCol)) { imageBytes = cursor.getBlob(imageCol); }
            }
        } catch (Exception e) { Log.e(TAG, "Lỗi lấy ảnh bìa album ID: " + albumId, e); }
        finally { if (cursor != null) { cursor.close(); } }
        return imageBytes;
    }

    public Photo getPhotoById(int photoId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        Photo photo = null;
        try {
            cursor = db.query(TABLE_PHOTOS, null, COLUMN_PHOTO_ID_PK + " = ?", new String[]{String.valueOf(photoId)}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idCol = cursor.getColumnIndexOrThrow(COLUMN_PHOTO_ID_PK);
                int albumIdCol = cursor.getColumnIndexOrThrow(COLUMN_PHOTO_ALBUM_ID_FK);
                int nameCol = cursor.getColumnIndexOrThrow(COLUMN_PHOTO_NAME);
                int imageCol = cursor.getColumnIndexOrThrow(COLUMN_PHOTO_IMAGE_DATA);
                int isFavoriteCol = cursor.getColumnIndexOrThrow(COLUMN_PHOTO_IS_FAVORITE);

                String name = cursor.getString(nameCol);
                if (name == null) { // Handle case where name might be null for older photos
                    name = "Ảnh ID " + cursor.getInt(idCol);
                }

                photo = new Photo(
                        cursor.getInt(idCol),
                        cursor.getInt(albumIdCol),
                        name,
                        cursor.getBlob(imageCol),
                        cursor.getInt(isFavoriteCol) == 1
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi lấy ảnh theo ID: " + photoId, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return photo;
    }


    public boolean setPhotoFavoriteStatus(int photoId, boolean isFavorite) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PHOTO_IS_FAVORITE, isFavorite ? 1 : 0);
        int rowsAffected = 0;
        try { rowsAffected = db.update(TABLE_PHOTOS, values, COLUMN_PHOTO_ID_PK + " = ?", new String[]{String.valueOf(photoId)}); }
        catch (Exception e) { Log.e(TAG, "Lỗi cập nhật TT yêu thích ảnh ID: " + photoId, e); return false; }
        return rowsAffected > 0;
    }

    public List<Photo> getFavoritePhotos() {
        List<Photo> favoritePhotos = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            String query = "SELECT * FROM " + TABLE_PHOTOS +
                    " WHERE " + COLUMN_PHOTO_IS_FAVORITE + " = 1 " +
                    " ORDER BY " + COLUMN_PHOTO_ID_PK + " DESC";
            cursor = db.rawQuery(query, null);

            if (cursor != null && cursor.moveToFirst()) {
                int idCol = cursor.getColumnIndexOrThrow(COLUMN_PHOTO_ID_PK);
                int albumIdCol = cursor.getColumnIndexOrThrow(COLUMN_PHOTO_ALBUM_ID_FK);
                int nameCol = cursor.getColumnIndexOrThrow(COLUMN_PHOTO_NAME);
                int imageCol = cursor.getColumnIndexOrThrow(COLUMN_PHOTO_IMAGE_DATA);
                int isFavoriteCol = cursor.getColumnIndexOrThrow(COLUMN_PHOTO_IS_FAVORITE);
                do {
                    String name = cursor.getString(nameCol);
                    if (name == null) { name = "Ảnh ID " + cursor.getInt(idCol); }

                    favoritePhotos.add(new Photo(
                            cursor.getInt(idCol),
                            cursor.getInt(albumIdCol),
                            name,
                            cursor.getBlob(imageCol),
                            cursor.getInt(isFavoriteCol) == 1
                    ));
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
            cursor = db.query(TABLE_PHOTOS, new String[]{COLUMN_PHOTO_IS_FAVORITE}, COLUMN_PHOTO_ID_PK + " = ?",
                    new String[]{String.valueOf(photoId)}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int favoriteCol = cursor.getColumnIndexOrThrow(COLUMN_PHOTO_IS_FAVORITE);
                favorite = cursor.getInt(favoriteCol) == 1;
            }
        } catch (Exception e) { Log.e(TAG, "Lỗi kiểm tra ảnh yêu thích ID: " + photoId, e); }
        finally { if (cursor != null) { cursor.close(); } }
        return favorite;
    }

    public boolean setAlbumHiddenStatus(int albumId, boolean isHidden) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ALBUM_IS_HIDDEN, isHidden ? 1 : 0);
        Log.d(TAG, "Setting hidden status for albumId " + albumId + " to " + isHidden);
        int rowsAffected = 0;
        try { rowsAffected = db.update(TABLE_ALBUMS, values, COLUMN_ALBUM_ID_PK + " = ?", new String[]{String.valueOf(albumId)}); }
        catch (Exception e) { Log.e(TAG, "Lỗi cập nhật TT ẩn album ID: " + albumId, e); return false; }
        return rowsAffected > 0;
    }

    public List<Album> getVisibleAlbums() {
        List<Album> albumListResult = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_ALBUM_ID_PK + ", " + COLUMN_ALBUM_NAME + ", " + COLUMN_ALBUM_TOPIC + ", " + COLUMN_ALBUM_IMAGE + ", " + COLUMN_ALBUM_IS_HIDDEN +
                " FROM " + TABLE_ALBUMS +
                " WHERE " + COLUMN_ALBUM_IS_HIDDEN + " = 0 " +
                " ORDER BY " + COLUMN_ALBUM_NAME + " ASC";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                int idCol = cursor.getColumnIndexOrThrow(COLUMN_ALBUM_ID_PK);
                int nameCol = cursor.getColumnIndexOrThrow(COLUMN_ALBUM_NAME);
                int topicCol = cursor.getColumnIndexOrThrow(COLUMN_ALBUM_TOPIC);
                int imageCol = cursor.getColumnIndexOrThrow(COLUMN_ALBUM_IMAGE);
                int isHiddenCol = cursor.getColumnIndexOrThrow(COLUMN_ALBUM_IS_HIDDEN);

                int id = cursor.getInt(idCol);
                String name = cursor.getString(nameCol);
                String topic = cursor.getString(topicCol);
                byte[] image = cursor.getBlob(imageCol);
                boolean isHidden = cursor.getInt(isHiddenCol) == 1;

                int totalImages = getTotalImages(id);
                Album album = new Album(id, name, topic, image, isHidden);
                album.setTotalImages(totalImages);
                albumListResult.add(album);
            } while (cursor.moveToNext());
        }
        if (cursor != null) {
            cursor.close();
        }
        Log.d(TAG, "getVisibleAlbums: Loaded " + albumListResult.size() + " visible albums.");
        return albumListResult;
    }

    public List<Album> getHiddenAlbums() {
        List<Album> albumListResult = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_ALBUM_ID_PK + ", " + COLUMN_ALBUM_NAME + ", " + COLUMN_ALBUM_TOPIC + ", " + COLUMN_ALBUM_IMAGE + ", " + COLUMN_ALBUM_IS_HIDDEN +
                " FROM " + TABLE_ALBUMS +
                " WHERE " + COLUMN_ALBUM_IS_HIDDEN + " = 1 " +
                " ORDER BY " + COLUMN_ALBUM_NAME + " ASC";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                int idCol = cursor.getColumnIndexOrThrow(COLUMN_ALBUM_ID_PK);
                int nameCol = cursor.getColumnIndexOrThrow(COLUMN_ALBUM_NAME);
                int topicCol = cursor.getColumnIndexOrThrow(COLUMN_ALBUM_TOPIC);
                int imageCol = cursor.getColumnIndexOrThrow(COLUMN_ALBUM_IMAGE);
                int isHiddenCol = cursor.getColumnIndexOrThrow(COLUMN_ALBUM_IS_HIDDEN);

                int id = cursor.getInt(idCol);
                String name = cursor.getString(nameCol);
                String topic = cursor.getString(topicCol);
                byte[] image = cursor.getBlob(imageCol);
                boolean isHidden = cursor.getInt(isHiddenCol) == 1;

                int totalImages = getTotalImages(id);
                Album album = new Album(id, name, topic, image, isHidden);
                album.setTotalImages(totalImages);
                albumListResult.add(album);
            } while (cursor.moveToNext());
        }
        if (cursor != null) {
            cursor.close();
        }
        Log.d(TAG, "getHiddenAlbums: Loaded " + albumListResult.size() + " hidden albums.");
        return albumListResult;
    }
}