package com.example.de2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditPhotoActivity extends AppCompatActivity {

    private static final String TAG = "EditPhotoActivity";

    private MaterialToolbar toolbar;
    private ImageView ivEditPhotoPreview;
    private TextInputLayout tilEditPhotoName;
    private TextInputEditText edtEditPhotoName;
    private Button btnSavePhotoChanges;

    private DatabaseHelper dbHelper;
    private int currentPhotoId = -1;
    private Photo currentPhoto; // Để lưu trữ thông tin ảnh hiện tại

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_photo);

        toolbar = findViewById(R.id.toolbar_edit_photo);
        ivEditPhotoPreview = findViewById(R.id.iv_edit_photo_preview);
        tilEditPhotoName = findViewById(R.id.til_edit_photo_name);
        edtEditPhotoName = findViewById(R.id.edt_edit_photo_name);
        btnSavePhotoChanges = findViewById(R.id.btn_save_photo_changes);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        dbHelper = new DatabaseHelper(this);

        Intent intent = getIntent();
        currentPhotoId = intent.getIntExtra("PHOTO_ID_TO_EDIT", -1);

        if (currentPhotoId == -1) {
            Toast.makeText(this, "Lỗi: Không có thông tin ảnh để sửa.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Không nhận được PHOTO_ID_TO_EDIT hợp lệ từ Intent.");
            finish();
            return;
        }

        loadPhotoDetails(currentPhotoId);

        btnSavePhotoChanges.setOnClickListener(v -> saveChanges());
    }

    private void loadPhotoDetails(int photoIdToLoad) {
        Log.d(TAG, "Đang tải chi tiết ảnh ID: " + photoIdToLoad);
        executorService.execute(() -> {
            currentPhoto = dbHelper.getPhotoById(photoIdToLoad); // Cần tạo getPhotoById trong DatabaseHelper
            runOnUiThread(() -> {
                if (currentPhoto != null) {
                    edtEditPhotoName.setText(currentPhoto.getName());
                    if (currentPhoto.getImage() != null && currentPhoto.getImage().length > 0) {
                        Glide.with(EditPhotoActivity.this)
                                .load(currentPhoto.getImage())
                                .placeholder(R.drawable.ic_placeholder_album)
                                .error(R.drawable.ic_placeholder_album)
                                .into(ivEditPhotoPreview);
                    } else {
                        ivEditPhotoPreview.setImageResource(R.drawable.ic_placeholder_album);
                    }
                    toolbar.setTitle("Sửa: " + currentPhoto.getName());
                } else {
                    Toast.makeText(EditPhotoActivity.this, "Không thể tải chi tiết ảnh.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Không tìm thấy ảnh với ID: " + photoIdToLoad);
                    finish();
                }
            });
        });
    }

    private void saveChanges() {
        String newName = edtEditPhotoName.getText().toString().trim();

        if (TextUtils.isEmpty(newName)) {
            tilEditPhotoName.setError("Tên ảnh không được để trống");
            return;
        }
        tilEditPhotoName.setError(null);

        if (currentPhotoId == -1) {
            Toast.makeText(this, "Lỗi: ID ảnh không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Chuẩn bị lưu thay đổi cho ảnh ID: " + currentPhotoId + ", Tên mới: " + newName);

        executorService.execute(() -> {
            final boolean success = dbHelper.updatePhotoName(currentPhotoId, newName);
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(EditPhotoActivity.this, "Đã cập nhật tên ảnh!", Toast.LENGTH_SHORT).show();
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("UPDATED_PHOTO_ID", currentPhotoId);
                    resultIntent.putExtra("UPDATED_PHOTO_NAME", newName);
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                } else {
                    Toast.makeText(EditPhotoActivity.this, "Lỗi khi cập nhật tên ảnh.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
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