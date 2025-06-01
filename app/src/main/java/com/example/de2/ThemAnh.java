package com.example.de2;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory; // Để nén ảnh
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThemAnh extends AppCompatActivity {

    private static final String TAG = "ThemAnhActivity";
    public static final String EXTRA_ALBUM_ID = "EXTRA_ALBUM_ID";

    private MaterialToolbar toolbar;
    private ImageView ivPreviewSelectedPhoto;
    private Button btnSelectPhoto;
    private TextInputLayout tilPhotoName;
    private TextInputEditText edtPhotoName;
    private Button btnAddThisPhoto, btnCancelAddPhoto;

    private DatabaseHelper dbHelper;
    private int albumId = -1;
    private Uri selectedImageUri = null;
    private byte[] selectedImageBytes = null;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_them_anh);

        toolbar = findViewById(R.id.toolbar_them_anh);
        ivPreviewSelectedPhoto = findViewById(R.id.iv_preview_selected_photo);
        btnSelectPhoto = findViewById(R.id.btn_select_photo);
        tilPhotoName = findViewById(R.id.til_photo_name);
        edtPhotoName = findViewById(R.id.edt_photo_name);
        btnAddThisPhoto = findViewById(R.id.btn_add_this_photo);
        btnCancelAddPhoto = findViewById(R.id.btn_cancel_add_photo);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Thêm Ảnh Mới");
        }

        dbHelper = new DatabaseHelper(this);

        albumId = getIntent().getIntExtra(EXTRA_ALBUM_ID, -1);
        if (albumId == -1) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID Album.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Album ID không hợp lệ.");
            finish();
            return;
        }

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            Glide.with(this)
                                    .load(selectedImageUri)
                                    .placeholder(R.drawable.ic_placeholder_album)
                                    .into(ivPreviewSelectedPhoto);
                            // Chuyển đổi URI sang byte array khi người dùng chọn ảnh
                            convertUriToBytes(selectedImageUri);
                        }
                    }
                });

        btnSelectPhoto.setOnClickListener(v -> openImageChooser());
        btnAddThisPhoto.setOnClickListener(v -> savePhoto());
        btnCancelAddPhoto.setOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });
    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void convertUriToBytes(Uri uri) {
        executorService.execute(() -> {
            try (InputStream inputStream = getContentResolver().openInputStream(uri);
                 ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                if (inputStream == null) {
                    selectedImageBytes = null;
                    return;
                }
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                if (bitmap == null) {
                    selectedImageBytes = null;
                    return;
                }
                // Nén ảnh để giảm kích thước, bạn có thể điều chỉnh chất lượng (ví dụ 85)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream);
                selectedImageBytes = byteArrayOutputStream.toByteArray();
                Log.d(TAG, "Ảnh đã được chọn và chuyển đổi, kích thước: " + selectedImageBytes.length / 1024 + "KB");
            } catch (IOException e) {
                Log.e(TAG, "Lỗi khi chuyển đổi URI ảnh thành byte array: ", e);
                selectedImageBytes = null;
                runOnUiThread(() -> Toast.makeText(ThemAnh.this, "Lỗi xử lý ảnh.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void savePhoto() {
        String photoName = edtPhotoName.getText().toString().trim();

        if (selectedImageBytes == null) {
            Toast.makeText(this, "Vui lòng chọn một ảnh.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(photoName)) {
            tilPhotoName.setError("Tên ảnh không được để trống");
            return;
        }
        tilPhotoName.setError(null); // Xóa lỗi nếu có

        Log.d(TAG, "Chuẩn bị thêm ảnh: " + photoName + " vào album ID: " + albumId);

        executorService.execute(() -> {
            // Tham số cuối (isFavorite) là false vì ảnh mới thêm mặc định không phải yêu thích
            final boolean success = dbHelper.insertImage(albumId, photoName, selectedImageBytes, false);
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(ThemAnh.this, "Đã thêm ảnh '" + photoName + "'!", Toast.LENGTH_SHORT).show();
                    Intent resultIntent = new Intent();
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                } else {
                    Toast.makeText(ThemAnh.this, "Lỗi khi thêm ảnh vào cơ sở dữ liệu.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(Activity.RESULT_CANCELED);
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