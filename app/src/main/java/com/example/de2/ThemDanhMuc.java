package com.example.de2;

import android.app.Activity; // Thêm import cho Activity.RESULT_OK và RESULT_CANCELED
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder; // Dùng ImageDecoder cho API 28+
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils; // Thêm để kiểm tra chuỗi rỗng
import android.util.Log;
import android.view.MenuItem; // Thêm cho nút back trên toolbar
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher; // Sử dụng ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts; // Sử dụng ActivityResultLauncher
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // Sử dụng Material Toolbar nếu có
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide; // Thêm Glide để hiển thị ảnh
import com.google.android.material.appbar.MaterialToolbar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ThemDanhMuc extends AppCompatActivity {
    private static final String TAG = "ThemDanhMuc";

    EditText edt_tenab, edt_chude; // Bỏ edt_maab nếu không dùng
    ImageButton btn_chonanh;
    Button btn_them, btn_huy;
    ImageView v_hienanhchon;
    DatabaseHelper dbHelper;
    MaterialToolbar toolbar; // Khai báo Toolbar

    private byte[] selectedImageBytes = null; // Lưu trữ byte array của ảnh đã chọn

    // ActivityResultLauncher mới để chọn ảnh
    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_them_danh_muc); // Đảm bảo layout này đúng

        // Xử lý EdgeToEdge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new DatabaseHelper(this);
        anhxa();

        // Thiết lập Toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Thêm Album Mới");
        }


        // Đăng ký ActivityResultLauncher
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            // Hiển thị ảnh đã chọn bằng Glide
                            Glide.with(this)
                                    .load(imageUri)
                                    .placeholder(R.drawable.ic_placeholder_album) // Placeholder
                                    .error(R.drawable.ic_placeholder_album)       // Ảnh lỗi
                                    .into(v_hienanhchon);
                            // Chuyển URI thành byte[] và lưu lại
                            try {
                                selectedImageBytes = uriToByteArray(imageUri);
                            } catch (IOException e) {
                                Log.e(TAG, "Lỗi chuyển đổi URI thành byte array: ", e);
                                Toast.makeText(this, "Lỗi xử lý ảnh.", Toast.LENGTH_SHORT).show();
                                selectedImageBytes = null; // Đặt lại nếu có lỗi
                            }
                        }
                    }
                });

        btn_chonanh.setOnClickListener(view -> openImageChooser());

        btn_them.setOnClickListener(view -> {
            String name = edt_tenab.getText().toString().trim();
            String topic = edt_chude.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                edt_tenab.setError("Tên album không được để trống");
                Toast.makeText(this, "Tên album không được để trống", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedImageBytes == null) {
                Toast.makeText(this, "Vui lòng chọn ảnh bìa cho album", Toast.LENGTH_SHORT).show();
                return;
            }

            // Thêm album với isHidden = false (mặc định)
            boolean success = dbHelper.insertAlbum(name, topic, selectedImageBytes, false);
            if (success) {
                Toast.makeText(this, "Thêm album thành công!", Toast.LENGTH_SHORT).show();
                setResult(Activity.RESULT_OK); // Đặt kết quả thành công
                finish(); // Đóng Activity và quay lại DanhMucAnh
            } else {
                Toast.makeText(this, "Thêm album thất bại!", Toast.LENGTH_SHORT).show();
            }
        });

        btn_huy.setOnClickListener(view -> {
            Toast.makeText(this, "Đã hủy thêm album", Toast.LENGTH_SHORT).show();
            setResult(Activity.RESULT_CANCELED); // Đặt kết quả là hủy
            finish(); // Đóng Activity hiện tại và quay lại trang trước (DanhMucAnh)
        });
    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // intent.setType("image/*"); // ACTION_PICK đã ngầm định type là image/*
        pickImageLauncher.launch(intent); // Sử dụng launcher mới
    }

    // Không cần onActivityResult nữa khi dùng ActivityResultLauncher


    private byte[] uriToByteArray(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            throw new IOException("Không thể mở InputStream từ URI");
        }
        Bitmap bitmap;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri));
        } else {
            // Cách cũ hơn cho API < 28
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        // Nén ảnh để giảm kích thước, bạn có thể điều chỉnh chất lượng
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream); // Nén thành JPEG chất lượng 85
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        inputStream.close();
        byteArrayOutputStream.close();
        return byteArray;
    }


    private void anhxa() {
        // Bỏ edt_maab nếu không có trong layout hoặc không dùng
        // edt_maab = findViewById(R.id.edt_maab);
        toolbar = findViewById(R.id.toolbar_them_danh_muc); // Giả sử ID toolbar là toolbar_them_danh_muc
        edt_tenab = findViewById(R.id.edt_tenab);
        edt_chude = findViewById(R.id.edt_chude);
        btn_chonanh = findViewById(R.id.btn_chonanh);
        btn_them = findViewById(R.id.btn_themab);
        btn_huy = findViewById(R.id.btn_huy);
        v_hienanhchon = findViewById(R.id.v_hienanhchon);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Xử lý nút back trên toolbar
        if (item.getItemId() == android.R.id.home) {
            setResult(Activity.RESULT_CANCELED); // Đặt kết quả là hủy nếu người dùng nhấn back
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}