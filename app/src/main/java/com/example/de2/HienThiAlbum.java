package com.example.de2;

import android.app.Activity;
import android.content.Intent;
// Các import của bạn đã khá đầy đủ
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
import androidx.annotation.Nullable; // Thêm cho onActivityResult nếu bạn dùng cách cũ cho ViewPhotoActivity
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

// THÊM implements PhotoAdapter.OnPhotoActionListener
public class HienThiAlbum extends AppCompatActivity implements PhotoAdapter.OnPhotoActionListener {
    private static final String TAG = "HienThiAlbum";
    private int albumId;
    private String albumName;
    private DatabaseHelper databaseHelper;
    private RecyclerView recyclerView;
    private PhotoAdapter photoAdapter; // PhotoAdapter bây giờ sẽ có thể gọi lại các phương thức trong interface
    private List<Photo> photoList;

    private MaterialToolbar toolbar;
    private MaterialButton btnAddPhoto, btnDeleteAlbum;

    private ActivityResultLauncher<Intent> pickImageLauncher;
    // (Tùy chọn) Nếu ViewPhotoActivity trả kết quả về đây
    private ActivityResultLauncher<Intent> viewPhotoLauncher;
    public static final int VIEW_PHOTO_REQUEST_CODE = 301; // Ví dụ request code

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hien_thi_album);

        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recycler_view_photos);
        btnAddPhoto = findViewById(R.id.btnAddPhoto); // Đảm bảo các ID này có trong layout của bạn
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
        // Truyền "this" (HienThiAlbum context) vào PhotoAdapter, vì HienThiAlbum đã implement OnPhotoActionListener
        photoAdapter = new PhotoAdapter(this, photoList);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setAdapter(photoAdapter);

        // loadPhotos() sẽ được gọi trong onResume()
        // Nếu các nút này không có trong layout activity_hien_thi_album.xml thì bạn có thể xóa
        if (btnAddPhoto != null && btnDeleteAlbum != null) {
            setupButtons();
        }
        setupImagePickerLauncher();
        setupViewPhotoLauncher(); // Đăng ký launcher cho ViewPhotoActivity
    }

    private void loadPhotos() {
        Log.d(TAG, "Đang tải ảnh cho albumId: " + albumId);
        executorService.execute(() -> {
            final List<Photo> tempPhotoList = new ArrayList<>();
            // SQLiteDatabase db = databaseHelper.getReadableDatabase(); // Không cần thiết nếu dbHelper.getReadableDatabase() đã được gọi bên trong hàm của nó
            final String query = "SELECT id, image, " + DatabaseHelper.COLUMN_PHOTO_IS_FAVORITE +
                    " FROM photos WHERE album_id = ? ORDER BY id DESC";
            Cursor cursor = databaseHelper.getReadableDatabase().rawQuery(query, new String[]{String.valueOf(albumId)});

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

            runOnUiThread(() -> {
                photoList.clear();
                photoList.addAll(tempPhotoList);
                photoAdapter.notifyDataSetChanged();
                Log.d(TAG, "Đã tải " + photoList.size() + " ảnh cho album: " + albumName);
                if (photoList.isEmpty() && !isFinishing()) {
                    Toast.makeText(HienThiAlbum.this, "Album này chưa có ảnh nào. Hãy thêm ảnh!", Toast.LENGTH_SHORT).show();
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
                    setResult(Activity.RESULT_OK, resultIntent); // Báo cho DanhMucAnh cập nhật
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
                                            setResult(Activity.RESULT_OK); // Báo cho DanhMucAnh biết có thay đổi (tổng số ảnh)
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

    // Đăng ký launcher để nhận kết quả từ ViewPhotoActivity (nếu trạng thái yêu thích thay đổi)
    private void setupViewPhotoLauncher() {
        viewPhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // Có thể có data trả về từ ViewPhotoActivity nếu bạn muốn xử lý cụ thể
                        // Ví dụ: result.getData().getIntExtra("PHOTO_ID_UPDATED", -1);
                        Log.d(TAG, "ViewPhotoActivity trả về RESULT_OK, có thể trạng thái yêu thích đã thay đổi. Tải lại ảnh.");
                        loadPhotos(); // Tải lại để cập nhật trạng thái yêu thích (nếu có thay đổi)
                    }
                }
        );
    }
    // Trong PhotoAdapter, khi mở ViewPhotoActivity, bạn sẽ dùng viewPhotoLauncher.launch(intent);
    // Và ViewPhotoActivity cần setResult(Activity.RESULT_OK) khi trạng thái yêu thích thay đổi.


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
            // Nếu có thay đổi dữ liệu (ví dụ xóa ảnh) thì kết quả đã được đặt là RESULT_OK
            // Nếu không, người dùng chỉ nhấn back, không cần làm gì thêm trước khi finish.
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // --- IMPLEMENT PHƯƠNG THỨC CỦA OnPhotoActionListener ---
    @Override
    public void onPhotoDeleted() {
        Log.d(TAG, "HienThiAlbum: onPhotoDeleted callback. Tải lại danh sách ảnh.");
        setResult(Activity.RESULT_OK); // Báo cho DanhMucAnh có thay đổi để cập nhật tổng số ảnh
        loadPhotos(); // Tải lại danh sách ảnh của album hiện tại
    }

    @Override
    public void onPhotoFavoriteChanged(int photoId, boolean isFavorite) {
        Log.d(TAG, "HienThiAlbum: onPhotoFavoriteChanged callback. PhotoID: " + photoId + ", IsFavorite: " + isFavorite);
        // Trạng thái yêu thích của một ảnh trong danh sách đã thay đổi.
        // Cập nhật lại item đó trong RecyclerView hoặc tải lại toàn bộ danh sách.
        // Cách đơn giản là tải lại:
        // loadPhotos(); // Hoặc tìm item và cập nhật trạng thái isFavorite của nó rồi notifyItemChanged

        // Quan trọng: Báo cho các Activity khác (ví dụ: FavoritePhotosActivity, ViewPhotoActivity nếu nó cũng lắng nghe)
        // rằng có thể có thay đổi về trạng thái yêu thích.
        // Cách đơn giản nhất là để các Activity đó tự cập nhật trong onResume của chúng.
        // Hoặc nếu bạn mở ViewPhotoActivity từ đây bằng launcher, nó sẽ tự cập nhật khi quay lại.
        setResult(Activity.RESULT_OK); // Báo hiệu có thay đổi chung
    }
    // --- KẾT THÚC IMPLEMENT ---

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        // Gọi shutdownExecutor của adapter nếu bạn đã thêm phương thức đó
        if (photoAdapter != null) {
            photoAdapter.shutdownExecutor();
        }
    }
}