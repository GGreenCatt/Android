package com.example.de2;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class MainActivity extends AppCompatActivity {

    // Thông tin đăng nhập (mã hóa cứng)
    private static final String HARDCODED_USERNAME = "admin";
    private static final String HARDCODED_PASSWORD = "admin";

    private TextInputEditText edtUsername, edtPassword;
    private TextInputLayout tilUsername, tilPassword;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main); // Sử dụng layout activity_main.xml đã cập nhật
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        anhxa();

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = edtUsername.getText().toString().trim();
                String password = edtPassword.getText().toString().trim();

                // Xóa lỗi cũ (nếu có)
                tilUsername.setError(null);
                tilPassword.setError(null);

                boolean isValid = true;
                if (TextUtils.isEmpty(username)) {
                    tilUsername.setError("Vui lòng nhập tên đăng nhập");
                    isValid = false;
                }
                if (TextUtils.isEmpty(password)) {
                    tilPassword.setError("Vui lòng nhập mật khẩu");
                    isValid = false;
                }

                if (!isValid) {
                    return;
                }

                // Kiểm tra thông tin đăng nhập
                if (username.equals(HARDCODED_USERNAME) && password.equals(HARDCODED_PASSWORD)) {
                    // Đăng nhập thành công
                    Toast.makeText(MainActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, DanhMucAnh.class);
                    startActivity(intent);
                    finish(); // Đóng MainActivity để người dùng không quay lại màn hình đăng nhập bằng nút back
                } else {
                    // Đăng nhập thất bại
                    Toast.makeText(MainActivity.this, "Tên đăng nhập hoặc mật khẩu không đúng.", Toast.LENGTH_LONG).show();
                    tilPassword.setError(" "); // Đặt lỗi rỗng để làm nổi bật cả 2 trường nếu sai
                    tilUsername.setError(" "); // hoặc chỉ đặt lỗi cho một trường cụ thể
                }
            }
        });
    }

    private void anhxa() {
        edtUsername = findViewById(R.id.edt_username);
        edtPassword = findViewById(R.id.edt_password);
        tilUsername = findViewById(R.id.til_username); // Thêm TextInputLayout
        tilPassword = findViewById(R.id.til_password); // Thêm TextInputLayout
        btnLogin = findViewById(R.id.btn_login); // Đổi ID từ btn_start thành btn_login
    }
}