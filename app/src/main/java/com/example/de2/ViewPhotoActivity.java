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
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu; // Thêm import cho Menu
import android.view.MenuInflater; // Thêm import cho MenuInflater
import android.view.MenuItem;
import android.widget.EditText; // Sẽ là TextInputEditText nếu dùng TextInputLayout
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull; // Thêm import
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager; // Giữ lại nếu dùng cho comments
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
// import com.google.android.material.button.MaterialButton; // Không thấy sử dụng trong code bạn gửi gần nhất

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ViewPhotoActivity extends AppCompatActivity {

    private static final String TAG = "ViewPhotoActivity";

    private ImageView imageView;
    private EditText edtComment;
    private ImageButton btnSendComment;
    private RecyclerView commentsRecyclerView;
    private MaterialToolbar toolbar;

    private List<Comment> commentsList;
    private CommentAdapter commentAdapter;
    private DatabaseHelper dbHelper;
    private int photoId = -1;
    private boolean isCurrentPhotoFavorite = false; // Biến lưu trạng thái yêu thích hiện tại

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // Giả sử bạn có một đối tượng Photo đầy đủ được truyền hoặc tải
    // private Photo currentPhotoObject; // Sẽ tốt hơn nếu có đối tượng Photo đầy đủ

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_photo); // Đảm bảo layout này đã có app:menu cho toolbar

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        imageView = findViewById(R.id.imageView);
        edtComment = findViewById(R.id.edtComment);
        btnSendComment = findViewById(R.id.btnSendComment);
        commentsRecyclerView = findViewById(R.id.recycler_view_comments);

        dbHelper = new DatabaseHelper(this);

        Intent intent = getIntent();
        photoId = intent.getIntExtra("PHOTO_ID", -1);
        byte[] imageBytes = intent.getByteArrayExtra("PHOTO_IMAGE_DATA");

        if (photoId == -1) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID ảnh.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "PHOTO_ID không hợp lệ hoặc không được truyền qua Intent.");
            finish();
            return;
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Ảnh ID: " + photoId);
        }

        if (imageBytes != null) {
            Glide.with(this)
                    .load(imageBytes)
                    .placeholder(R.drawable.ic_placeholder_album)
                    .error(R.drawable.ic_placeholder_album)
                    .into(imageView);
        } else {
            Log.w(TAG, "Không có dữ liệu ảnh (imageBytes is null) cho photoId: " + photoId + ". Đang thử tải từ DB.");
            // Nếu không truyền imageBytes, thử tải ảnh từ DB dựa trên photoId (cần hàm trong DB helper)
            // loadPhotoDataFromDb(photoId); // Bạn cần tạo hàm này nếu muốn
            imageView.setImageResource(R.drawable.ic_placeholder_album); // Tạm thời
        }

        // Tải trạng thái yêu thích ban đầu
        loadFavoriteStatus();

        commentsList = new ArrayList<>();
        commentAdapter = new CommentAdapter(this, commentsList);
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentsRecyclerView.setAdapter(commentAdapter);

        loadCommentsFromDb();

        btnSendComment.setOnClickListener(v -> {
            String commentText = edtComment.getText().toString().trim();
            if (!TextUtils.isEmpty(commentText)) {
                addNewComment(photoId, commentText, "Khách");
                edtComment.setText("");
            } else {
                Toast.makeText(ViewPhotoActivity.this, "Vui lòng nhập bình luận.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadFavoriteStatus() {
        if (photoId != -1) {
            executorService.execute(() -> {
                // Giả sử DatabaseHelper có phương thức isPhotoFavorite(photoId)
                // Hoặc bạn tải toàn bộ đối tượng Photo và lấy isFavorite từ đó
                final boolean favorite = dbHelper.isPhotoFavorite(photoId); // BẠN CẦN TẠO HÀM NÀY TRONG DBHELPER
                runOnUiThread(() -> {
                    isCurrentPhotoFavorite = favorite;
                    invalidateOptionsMenu(); // Gọi để vẽ lại menu với icon đúng
                    Log.d(TAG, "Trạng thái yêu thích ban đầu cho photoId " + photoId + ": " + isCurrentPhotoFavorite);
                });
            });
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.view_photo_toolbar_menu, menu); // Inflate menu của bạn
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem favoriteItem = menu.findItem(R.id.action_toggle_favorite);
        if (favoriteItem != null) {
            if (isCurrentPhotoFavorite) {
                favoriteItem.setIcon(R.drawable.ic_favorite_filled_24); // Icon trái tim đầy
                // favoriteItem.getIcon().setTint(ContextCompat.getColor(this, R.color.your_favorite_color)); // Tùy chọn màu
            } else {
                favoriteItem.setIcon(R.drawable.ic_favorite_border_24); // Icon trái tim rỗng
                // favoriteItem.getIcon().setTint(ContextCompat.getColor(this, R.color.your_default_icon_color)); // Tùy chọn màu
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (itemId == R.id.action_toggle_favorite) {
            toggleFavoriteStatus();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleFavoriteStatus() {
        if (photoId == -1) return;

        final boolean newFavoriteState = !isCurrentPhotoFavorite;
        executorService.execute(() -> {
            final boolean success = dbHelper.setPhotoFavoriteStatus(photoId, newFavoriteState);
            runOnUiThread(() -> {
                if (success) {
                    isCurrentPhotoFavorite = newFavoriteState;
                    invalidateOptionsMenu(); // Yêu cầu vẽ lại menu để cập nhật icon
                    String message = isCurrentPhotoFavorite ? "Đã thêm vào yêu thích" : "Đã xóa khỏi yêu thích";
                    Toast.makeText(ViewPhotoActivity.this, message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Đã cập nhật trạng thái yêu thích cho photoId " + photoId + " thành " + isCurrentPhotoFavorite);

                    // (Tùy chọn) Gửi kết quả về cho Activity trước đó nếu cần thiết
                    // Intent resultIntent = new Intent();
                    // resultIntent.putExtra("PHOTO_ID_UPDATED", photoId);
                    // resultIntent.putExtra("NEW_FAVORITE_STATUS", isCurrentPhotoFavorite);
                    // setResult(Activity.RESULT_OK, resultIntent);

                } else {
                    Toast.makeText(ViewPhotoActivity.this, "Lỗi cập nhật yêu thích.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Lỗi khi cập nhật trạng thái yêu thích cho photoId " + photoId);
                }
            });
        });
    }


    private void loadCommentsFromDb() {
        Log.d(TAG, "Đang tải bình luận cho photoId: " + photoId);
        if (photoId == -1) return;
        executorService.execute(() -> {
            final List<Comment> loadedComments = dbHelper.getCommentsForPhoto(photoId);
            runOnUiThread(() -> {
                commentsList.clear();
                commentsList.addAll(loadedComments);
                commentAdapter.notifyDataSetChanged();
                if (!commentsList.isEmpty()) {
                    commentsRecyclerView.scrollToPosition(commentsList.size() - 1);
                }
                Log.d(TAG, "Đã tải " + commentsList.size() + " bình luận từ DB.");
            });
        });
    }

    private void addNewComment(int currentPhotoId, String text, String author) {
        Log.d(TAG, "Đang thêm bình luận cho photoId: " + currentPhotoId);
        if (currentPhotoId == -1) return;
        executorService.execute(() -> {
            final boolean success = dbHelper.addComment(currentPhotoId, text, author);
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(ViewPhotoActivity.this, "Đã thêm bình luận!", Toast.LENGTH_SHORT).show();
                    loadCommentsFromDb();
                } else {
                    Toast.makeText(ViewPhotoActivity.this, "Lỗi khi thêm bình luận.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Không thể thêm bình luận vào DB cho photoId: " + currentPhotoId);
                }
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Tải lại dữ liệu.");
        // Tải lại trạng thái yêu thích và bình luận khi quay lại màn hình
        // để đảm bảo dữ liệu luôn mới (ví dụ nếu trạng thái yêu thích được thay đổi từ nơi khác)
        loadFavoriteStatus();
        loadCommentsFromDb();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}