package com.example.de2;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.ByteArrayOutputStream;

public class ThemDanhMuc extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    EditText edt_maab, edt_tenab, edt_chude;
    ImageButton btn_chonanh;
    Button btn_them, btn_huy;
    ImageView v_hienanhchon;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_them_danh_muc);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        anhxa();
        btn_chonanh.setOnClickListener(view -> openImageChooser());

        btn_them.setOnClickListener(view -> {
            String name = edt_tenab.getText().toString();
            String topic = edt_chude.getText().toString();
            byte[] image = imageViewToByte(v_hienanhchon);

            DatabaseHelper dbHelper = new DatabaseHelper(this);
            boolean success = dbHelper.insertAlbum(name, topic, image);
            if (success) {
                Toast.makeText(this, "Thêm album thành công!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, DanhMucAnh.class);
                startActivity(intent);
                finish(); // Kết thúc Activity hiện tại để không trở lại
            } else {
                Toast.makeText(this, "Thêm album thất bại!", Toast.LENGTH_SHORT).show();
            }
        });

    }
    private void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Chọn ảnh"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            v_hienanhchon.setImageURI(imageUri);
        }
    }
    // Hàm để chuyển Bitmap thành byte array
    private byte[] imageViewToByte(ImageView imageView) {
        Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

// Gọi hàm lưu vào database khi nhấn nút 'Thêm'

    private void anhxa() {
        edt_maab= findViewById(R.id.edt_maab);
        edt_tenab= findViewById(R.id.edt_tenab);
        edt_chude= findViewById(R.id.edt_chude);
        btn_chonanh= findViewById(R.id.btn_chonanh);
        btn_them= findViewById(R.id.btn_themab);
        btn_huy= findViewById(R.id.btn_huy);
        v_hienanhchon= findViewById(R.id.v_hienanhchon);
    }
}