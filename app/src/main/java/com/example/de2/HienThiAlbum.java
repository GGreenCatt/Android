package com.example.de2;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

public class HienThiAlbum extends AppCompatActivity {
    private static final String TAG = "HienThiAlbum";
    private int albumId;
    private String albumName;
    private DatabaseHelper databaseHelper;
    private RecyclerView recyclerView;
    private PhotoAdapter photoAdapter;
    private List<Photo> photoList;

    private MaterialToolbar toolbar;
    private MaterialButton btnAddPhoto, btnDeleteAlbum; // Giữ lại nếu layout của bạn có các nút này

    private ActivityResultLauncher<Intent> pickImageLauncher;
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
            Log.e(TAG, "albumId không hợp lệ hoặc không được truyền qua Intent.");
            finish();
            return;
        }

        photoList = new ArrayList<>();
        photoAdapter = new PhotoAdapter(this, photoList);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setAdapter(photoAdapter);

        // loadPhotos() sẽ được gọi trong onResume, nên có thể bỏ qua ở đây để tránh gọi thừa ban đầu
        // nếu bạn muốn, hoặc giữ lại nếu muốn tải ngay khi tạo.
        // loadPhotos();

        if (btnAddPhoto != null && btnDeleteAlbum != null) {
            setupButtons();
        }
        setupImagePickerLauncher();
    }

    private void loadPhotos() {
        Log.d(TAG, "Đang tải ảnh cho albumId: " + albumId);
        // KHÔNG gọi photoList.clear() ở đây nữa

        executorService.execute(() -> {
            final List<Photo> tempPhotoList = new ArrayList<>();
            SQLiteDatabase db = databaseHelper.getReadableDatabase(); // Nên lấy instance ở đây
            final String query = "SELECT id, image, " + DatabaseHelper.COLUMN_PHOTO_IS_FAVORITE +
                    " FROM photos WHERE album_id = ? ORDER BY id DESC";
            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(albumId)});

            if (cursor != null) {
                try {
                    int idIndex = cursor.getColumnIndexOrThrow("id");
                    int imageIndex = cursor.getColumnIndexOrThrow("image");
                    int isFavoriteIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PHOTO_IS_FAVORITE);

                    while (cursor.moveToNext()) {
                        int photoId = cursor.getInt(idIndex);
                        byte[] image = cursor.getBlob(imageIndex);
                        boolean isFavorite = cursor.getInt(isFavoriteIndex) == 1;
                        Photo photo = new Photo(photoId, albumId, "Ảnh " + photoId, image, isFavorite);
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
            // Không đóng db ở đây nếu databaseHelper quản lý kết nối

            runOnUiThread(() -> {
                photoList.clear(); // ✅ DI CHUYỂN clear() VÀO ĐÂY
                photoList.addAll(tempPhotoList);
                photoAdapter.notifyDataSetChanged();
                Log.d(TAG, "Đã tải " + photoList.size() + " ảnh cho album: " + albumName);
                if (photoList.isEmpty() && !isFinishing()) {
                    Toast.makeText(HienThiAlbum.this, "Album này chưa có ảnh nào.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }


    private void setupButtons() {
        btnAddPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });

        btnDeleteAlbum.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Xác nhận xóa Album")
                    .setMessage("Bạn có chắc chắn muốn xóa album '" + albumName + "' và tất cả ảnh, bình luận bên trong không?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        deleteAlbumAndRelatedData();
                    })
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
                    Intent resultIntent = new Intent();
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                } else {
                    Toast.makeText(HienThiAlbum.this, "Lỗi khi xóa album.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Không thể xóa album ID: " + albumId + " từ database.");
                }
            });
        });
    }


    private void setupImagePickerLauncher() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            executorService.execute(() -> {
                                byte[] imageBytes = getImageBytesFromUri(imageUri);
                                if (imageBytes != null) {
                                    boolean success = databaseHelper.insertImage(albumId, imageBytes, false);
                                    runOnUiThread(() -> {
                                        if (success) {
                                            Toast.makeText(HienThiAlbum.this, "Ảnh đã được thêm!", Toast.LENGTH_SHORT).show();
                                            loadPhotos(); // Tải lại danh sách để hiển thị ảnh mới
                                        } else {
                                            Toast.makeText(HienThiAlbum.this, "Lỗi khi thêm ảnh.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } else {
                                    runOnUiThread(() -> Toast.makeText(HienThiAlbum.this, "Không thể xử lý ảnh.", Toast.LENGTH_SHORT).show());
                                }
                            });
                        }
                    }
                });
    }

    private byte[] getImageBytesFromUri(Uri imageUri) {
        try (InputStream inputStream = getContentResolver().openInputStream(imageUri);
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            if (inputStream == null) return null;
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) return null;
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
            Log.d(TAG, "Ảnh đã được nén, kích thước: " + byteArrayOutputStream.size() / 1024 + "KB");
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            Log.e(TAG, "Lỗi khi chuyển đổi URI ảnh thành byte array: ", e);
            return null;
        }
    }

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
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}