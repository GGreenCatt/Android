package com.example.de2;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory; // Đã có trong ThemAnh.java, ở đây có thể không cần nếu không xử lý bitmap trực tiếp
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HienThiAlbum extends AppCompatActivity implements PhotoAdapter.OnPhotoActionListener {
    private static final String TAG = "HienThiAlbum";
    private int albumId;
    private String albumName;
    private DatabaseHelper databaseHelper;
    private RecyclerView recyclerView;
    private PhotoAdapter photoAdapter;
    private List<Photo> photoList;

    private MaterialToolbar toolbar;
    private MaterialButton btnAddPhoto, btnDeleteAlbum;

    // Launcher để mở ThemAnh Activity
    private ActivityResultLauncher<Intent> addPhotoLauncher;



    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hien_thi_album);

        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recycler_view_photos);
        btnAddPhoto = findViewById(R.id.btnAddPhoto);
        btnDeleteAlbum = findViewById(R.id.btnDeleteAlbum);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        databaseHelper = new DatabaseHelper(this);
        albumId = getIntent().getIntExtra("albumId", -1);
        albumName = getIntent().getStringExtra("ALBUM_NAME");

        if (albumName != null) {
            toolbar.setTitle(albumName);
        } else {
            toolbar.setTitle("Ảnh trong Album");
        }

        if (albumId == -1) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID Album.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "albumId không hợp lệ.");
            finish();
            return;
        }

        photoList = new ArrayList<>();
        photoAdapter = new PhotoAdapter(this, photoList);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setAdapter(photoAdapter);

        // Đăng ký addPhotoLauncher
        addPhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // Ảnh đã được thêm thành công trong ThemAnh Activity
                        Log.d(TAG, "Thêm ảnh thành công, tải lại danh sách ảnh.");
                        loadPhotos(); // Tải lại danh sách để hiển thị ảnh mới
                        setResult(Activity.RESULT_OK); // Báo cho DanhMucAnh biết có thay đổi (tổng số ảnh)
                    }
                }
        );


        if (btnAddPhoto != null) { // Chỉ setup nút thêm ảnh
            setupAddPhotoButton();
        }
        if (btnDeleteAlbum != null) { // Chỉ setup nút xóa album
            setupDeleteAlbumButton();
        }

    }



    private void setupAddPhotoButton() {
        btnAddPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(HienThiAlbum.this, ThemAnh.class);
            intent.putExtra(ThemAnh.EXTRA_ALBUM_ID, albumId); // Truyền albumId sang ThemAnh
            addPhotoLauncher.launch(intent);
        });
    }

    private void setupDeleteAlbumButton() {
        btnDeleteAlbum.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Xác nhận xóa Album")
                    .setMessage("Bạn có chắc chắn muốn xóa album '" + albumName + "' và tất cả ảnh, bình luận bên trong không?")
                    .setPositiveButton("Xóa", (dialog, which) -> deleteAlbumAndRelatedData())
                    .setNegativeButton("Hủy", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        });
    }


    private void deleteAlbumAndRelatedData() {
        Log.d(TAG, "Chuẩn bị xóa album ID: " + albumId);
        executorService.execute(() -> {
            final boolean success = databaseHelper.deleteAlbum(albumId);
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(HienThiAlbum.this, "Album '" + albumName + "' đã được xóa.", Toast.LENGTH_SHORT).show();
                    setResult(Activity.RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(HienThiAlbum.this, "Lỗi khi xóa album.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }


    private void loadPhotos() {
        Log.d(TAG, "Đang tải ảnh cho albumId: " + albumId);
        executorService.execute(() -> {
            // Câu lệnh SQL không thay đổi, vẫn lấy các cột như trước
            final String query = "SELECT " + DatabaseHelper.COLUMN_PHOTO_ID_PK + ", " +
                    DatabaseHelper.COLUMN_PHOTO_NAME + ", " +
                    DatabaseHelper.COLUMN_PHOTO_IMAGE_DATA + ", " +
                    DatabaseHelper.COLUMN_PHOTO_IS_FAVORITE +
                    " FROM " + DatabaseHelper.TABLE_PHOTOS +
                    " WHERE " + DatabaseHelper.COLUMN_PHOTO_ALBUM_ID_FK + " = ? ORDER BY " + DatabaseHelper.COLUMN_PHOTO_ID_PK + " DESC";

            Cursor cursor = databaseHelper.getReadableDatabase().rawQuery(query, new String[]{String.valueOf(albumId)});
            final List<Photo> tempPhotoList = new ArrayList<>();

            if (cursor != null) {
                try {
                    int idIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PHOTO_ID_PK);
                    int nameIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PHOTO_NAME);
                    int imageIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PHOTO_IMAGE_DATA);
                    int isFavoriteIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PHOTO_IS_FAVORITE);

                    while (cursor.moveToNext()) {
                        int photoIdVal = cursor.getInt(idIndex);
                        String photoNameStr = cursor.getString(nameIndex);
                        byte[] image = cursor.getBlob(imageIndex);
                        boolean isFavorite = cursor.getInt(isFavoriteIndex) == 1;
                        if (photoNameStr == null) { // Xử lý tên null từ DB (cho ảnh cũ)
                            photoNameStr = "Ảnh ID " + photoIdVal;
                        }
                        Photo photo = new Photo(photoIdVal, albumId, photoNameStr, image, isFavorite);
                        tempPhotoList.add(photo);
                    }
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Lỗi tên cột không tồn tại khi tải ảnh: " + e.getMessage());
                } finally {
                    cursor.close();
                }
            } else {
                Log.e(TAG, "Cursor rỗng khi tải ảnh cho albumId: " + albumId);
            }

            runOnUiThread(() -> {
                photoList.clear();
                photoList.addAll(tempPhotoList);
                photoAdapter.notifyDataSetChanged();
                Log.d(TAG, "Đã tải " + photoList.size() + " ảnh cho album: " + albumName);
                if (photoList.isEmpty() && !isFinishing()) {
                    Toast.makeText(HienThiAlbum.this, "Album này chưa có ảnh nào.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // Bỏ phương thức getImageBytesFromUri nếu không dùng trực tiếp ở đây nữa

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Tải lại danh sách ảnh.");
        loadPhotos();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onPhotoDeleted() {
        Log.d(TAG, "HienThiAlbum: onPhotoDeleted callback. Tải lại danh sách ảnh.");
        setResult(Activity.RESULT_OK);
        loadPhotos();
    }

    @Override
    public void onPhotoFavoriteChanged(int photoId, boolean isFavorite) {
        Log.d(TAG, "HienThiAlbum: onPhotoFavoriteChanged callback. PhotoID: " + photoId + ", IsFavorite: " + isFavorite);
        loadPhotos(); // Tải lại để cập nhật icon
        setResult(Activity.RESULT_OK);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        if (photoAdapter != null) {
            photoAdapter.shutdownExecutor();
        }
    }
}