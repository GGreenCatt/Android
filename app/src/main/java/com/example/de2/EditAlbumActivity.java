package com.example.de2;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

public class EditAlbumActivity extends AppCompatActivity {

    private static final String TAG = "EditAlbumActivity";

    private MaterialToolbar toolbar;
    private ImageView ivEditAlbumCover;
    private Button btnChangeAlbumCover;
    private TextInputLayout tilEditAlbumName, tilEditAlbumTopic;
    private TextInputEditText edtEditAlbumName, edtEditAlbumTopic;
    private Button btnSaveAlbumChanges;

    private DatabaseHelper dbHelper;
    private int currentAlbumId = -1;
    private byte[] newAlbumImageBytes = null; // Lưu ảnh bìa mới được chọn (nếu có)

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // ActivityResultLauncher để chọn ảnh
    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_album);

        toolbar = findViewById(R.id.toolbar_edit_album);
        ivEditAlbumCover = findViewById(R.id.iv_edit_album_cover);
        btnChangeAlbumCover = findViewById(R.id.btn_change_album_cover);
        tilEditAlbumName = findViewById(R.id.til_edit_album_name);
        edtEditAlbumName = findViewById(R.id.edt_edit_album_name);
        tilEditAlbumTopic = findViewById(R.id.til_edit_album_topic);
        edtEditAlbumTopic = findViewById(R.id.edt_edit_album_topic);
        btnSaveAlbumChanges = findViewById(R.id.btn_save_album_changes);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        dbHelper = new DatabaseHelper(this);

        // Nhận dữ liệu album từ Intent
        Intent intent = getIntent();
        currentAlbumId = intent.getIntExtra("ALBUM_ID", -1);
        String currentAlbumName = intent.getStringExtra("ALBUM_NAME");
        String currentAlbumTopic = intent.getStringExtra("ALBUM_TOPIC");

        if (currentAlbumId == -1) {
            Toast.makeText(this, "Lỗi: Không có thông tin album để sửa.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Không nhận được ALBUM_ID hợp lệ từ Intent.");
            finish();
            return;
        }

        // Hiển thị thông tin album hiện tại
        edtEditAlbumName.setText(currentAlbumName);
        edtEditAlbumTopic.setText(currentAlbumTopic);
        toolbar.setTitle("Sửa: " + currentAlbumName);

        // Tải và hiển thị ảnh bìa hiện tại
        loadCurrentAlbumCover(currentAlbumId);

        // Xử lý chọn ảnh mới
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            // Hiển thị ảnh mới được chọn lên ImageView
                            Glide.with(this)
                                    .load(imageUri)
                                    .placeholder(R.drawable.ic_placeholder_album)
                                    .error(R.drawable.ic_placeholder_album)
                                    .into(ivEditAlbumCover);
                            // Chuyển URI thành byte[] để lưu trữ
                            // Việc này nên được thực hiện khi nhấn "Lưu" để tránh xử lý ảnh không cần thiết
                            // nhưng để đơn giản, có thể thực hiện ở đây hoặc lưu URI lại
                            // newAlbumImageBytes sẽ được set khi nhấn "Lưu" từ URI này
                            // Để an toàn, ta sẽ xử lý chuyển đổi byte[] khi nhấn lưu.
                            // Chỉ lưu URI lại.
                            convertUriToBytesAndStore(imageUri);
                        }
                    }
                });

        btnChangeAlbumCover.setOnClickListener(v -> {
            Intent pickImageIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageIntent.setType("image/*");
            pickImageLauncher.launch(pickImageIntent);
        });

        btnSaveAlbumChanges.setOnClickListener(v -> saveChanges());
    }

    private void loadCurrentAlbumCover(int albumIdToLoad) {
        Log.d(TAG, "Đang tải ảnh bìa cho album ID: " + albumIdToLoad);
        executorService.execute(() -> {
            // Giả sử bạn đã có phương thức getAlbumCoverImage trong DatabaseHelper
            final byte[] coverImage = dbHelper.getAlbumCoverImage(albumIdToLoad);
            runOnUiThread(() -> {
                if (coverImage != null && coverImage.length > 0) {
                    // newAlbumImageBytes sẽ lưu ảnh mới nếu người dùng chọn,
                    // nếu không nó sẽ là null, và khi lưu ta sẽ không cập nhật ảnh.
                    // Ảnh bìa hiện tại chỉ để hiển thị.
                    Glide.with(EditAlbumActivity.this)
                            .load(coverImage)
                            .placeholder(R.drawable.ic_placeholder_album)
                            .error(R.drawable.ic_placeholder_album)
                            .into(ivEditAlbumCover);
                    Log.d(TAG, "Đã tải ảnh bìa hiện tại.");
                } else {
                    ivEditAlbumCover.setImageResource(R.drawable.ic_placeholder_album);
                    Log.w(TAG, "Không tìm thấy ảnh bìa cho album ID: " + albumIdToLoad);
                }
            });
        });
    }

    // Phương thức này được gọi khi ảnh mới được chọn từ gallery
    private void convertUriToBytesAndStore(Uri imageUri) {
        executorService.execute(() -> {
            try (InputStream inputStream = getContentResolver().openInputStream(imageUri);
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                if (inputStream == null) {
                    newAlbumImageBytes = null; // Đặt lại nếu có lỗi
                    return;
                }
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                if (bitmap == null) {
                    newAlbumImageBytes = null; // Đặt lại nếu có lỗi
                    return;
                }
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos); // Nén ảnh
                newAlbumImageBytes = baos.toByteArray(); // Lưu byte array của ảnh MỚI
                Log.d(TAG, "Ảnh mới đã được chọn và chuyển thành byte array.");
            } catch (IOException e) {
                Log.e(TAG, "Lỗi khi xử lý ảnh mới từ URI: ", e);
                newAlbumImageBytes = null; // Đặt lại nếu có lỗi
                runOnUiThread(() -> Toast.makeText(EditAlbumActivity.this, "Lỗi xử lý ảnh được chọn.", Toast.LENGTH_SHORT).show());
            }
        });
    }


    private void saveChanges() {
        String newName = edtEditAlbumName.getText().toString().trim();
        String newTopic = edtEditAlbumTopic.getText().toString().trim();

        if (TextUtils.isEmpty(newName)) {
            tilEditAlbumName.setError("Tên album không được để trống");
            return;
        }
        tilEditAlbumName.setError(null); // Xóa lỗi nếu có

        Log.d(TAG, "Chuẩn bị lưu thay đổi cho album ID: " + currentAlbumId);
        Log.d(TAG, "Tên mới: " + newName + ", Chủ đề mới: " + newTopic + ", Ảnh mới có được chọn không: " + (newAlbumImageBytes != null));

        // Gọi phương thức updateAlbum từ DatabaseHelper trên luồng nền
        executorService.execute(() -> {
            // newAlbumImageBytes sẽ là null nếu người dùng không chọn ảnh mới,
            // khi đó phương thức updateAlbum trong DatabaseHelper sẽ không cập nhật ảnh.
            final boolean success = dbHelper.updateAlbum(currentAlbumId, newName, newTopic, newAlbumImageBytes);

            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(EditAlbumActivity.this, "Đã cập nhật album!", Toast.LENGTH_SHORT).show();
                    Intent resultIntent = new Intent();
                    setResult(Activity.RESULT_OK, resultIntent); // Đặt kết quả trả về cho DanhMucAnh
                    finish(); // Đóng Activity sửa
                } else {
                    Toast.makeText(EditAlbumActivity.this, "Lỗi khi cập nhật album hoặc không có gì thay đổi.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Xử lý khi nhấn nút back trên toolbar
            // Có thể hiển thị dialog xác nhận nếu có thay đổi chưa lưu
            finish(); // Đơn giản là đóng activity
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