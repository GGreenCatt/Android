package com.example.de2;

import android.app.Activity;
import android.content.Intent;
// ... các import khác
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ViewPhotoActivity extends AppCompatActivity {

    private static final String TAG = "ViewPhotoActivity";

    private ImageView imageView;
    private EditText edtComment; // Sẽ là TextInputEditText nếu dùng TextInputLayout trong XML
    private ImageButton btnSendComment;
    private RecyclerView commentsRecyclerView;
    private MaterialToolbar toolbar;

    private List<Comment> commentsList;
    private CommentAdapter commentAdapter;
    private DatabaseHelper dbHelper;
    private int photoId = -1;
    private Photo currentPhotoObject; // Để lưu trữ thông tin ảnh, bao gồm cả tên
    private boolean isCurrentPhotoFavorite = false;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // Launcher để mở EditPhotoActivity
    private ActivityResultLauncher<Intent> editPhotoLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_photo);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // Tiêu đề sẽ được đặt sau khi tải chi tiết ảnh
        }

        imageView = findViewById(R.id.imageView);
        edtComment = findViewById(R.id.edtComment);
        btnSendComment = findViewById(R.id.btnSendComment);
        commentsRecyclerView = findViewById(R.id.recycler_view_comments);

        dbHelper = new DatabaseHelper(this);

        Intent intent = getIntent();
        photoId = intent.getIntExtra("PHOTO_ID", -1);


        if (photoId == -1) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID ảnh.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "PHOTO_ID không hợp lệ hoặc không được truyền qua Intent.");
            finish();
            return;
        }

        // Đăng ký editPhotoLauncher
        editPhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        int updatedPhotoId = data.getIntExtra("UPDATED_PHOTO_ID", -1);
                        String updatedPhotoName = data.getStringExtra("UPDATED_PHOTO_NAME");

                        if (updatedPhotoId == photoId && updatedPhotoName != null) {
                            Log.d(TAG, "Tên ảnh đã được cập nhật: " + updatedPhotoName);
                            if (currentPhotoObject != null) {
                                currentPhotoObject.setName(updatedPhotoName);
                            }
                            if (getSupportActionBar() != null) {
                                getSupportActionBar().setTitle(updatedPhotoName); // Cập nhật tiêu đề toolbar
                            }
                            Toast.makeText(this, "Thông tin ảnh đã được cập nhật.", Toast.LENGTH_SHORT).show();
                            // Báo cho HienThiAlbum là có thay đổi
                            setResult(Activity.RESULT_OK);
                        }
                    }
                });

        loadPhotoDetailsAndComments(); // Tải cả chi tiết ảnh và bình luận

        commentsList = new ArrayList<>();
        commentAdapter = new CommentAdapter(this, commentsList);
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentsRecyclerView.setAdapter(commentAdapter);


        btnSendComment.setOnClickListener(v -> {
            String commentText = edtComment.getText().toString().trim();
            if (!TextUtils.isEmpty(commentText)) {
                addNewComment(photoId, commentText, "Khách"); // Giả sử tác giả là "Khách"
                edtComment.setText("");
            } else {
                Toast.makeText(ViewPhotoActivity.this, "Vui lòng nhập bình luận.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPhotoDetailsAndComments() {
        if (photoId != -1) {
            executorService.execute(() -> {
                currentPhotoObject = dbHelper.getPhotoById(photoId);
                final List<Comment> loadedComments = dbHelper.getCommentsForPhoto(photoId);

                runOnUiThread(() -> {
                    if (currentPhotoObject != null) {
                        isCurrentPhotoFavorite = currentPhotoObject.isFavorite();
                        if (getSupportActionBar() != null) {
                            getSupportActionBar().setTitle(currentPhotoObject.getName()); // Đặt tiêu đề
                        }
                        if (currentPhotoObject.getImage() != null && currentPhotoObject.getImage().length > 0) {
                            Glide.with(ViewPhotoActivity.this)
                                    .load(currentPhotoObject.getImage())
                                    .placeholder(R.drawable.ic_placeholder_album)
                                    .error(R.drawable.ic_placeholder_album)
                                    .into(imageView);
                        } else {
                            imageView.setImageResource(R.drawable.ic_placeholder_album);
                        }
                        invalidateOptionsMenu(); // Cập nhật icon yêu thích
                        Log.d(TAG, "Đã tải chi tiết ảnh: " + currentPhotoObject.getName());
                    } else {
                        Toast.makeText(this, "Không thể tải chi tiết ảnh.", Toast.LENGTH_SHORT).show();
                        finish(); // Hoặc xử lý lỗi khác
                        return;
                    }

                    commentsList.clear();
                    commentsList.addAll(loadedComments);
                    commentAdapter.notifyDataSetChanged();
                    if (!commentsList.isEmpty()) {
                        commentsRecyclerView.smoothScrollToPosition(commentsList.size() - 1);
                    }
                    Log.d(TAG, "Đã tải " + commentsList.size() + " bình luận.");
                });
            });
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.view_photo_toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem favoriteItem = menu.findItem(R.id.action_toggle_favorite);
        if (favoriteItem != null) {
            if (isCurrentPhotoFavorite) {
                favoriteItem.setIcon(R.drawable.ic_favorite_filled_24);
            } else {
                favoriteItem.setIcon(R.drawable.ic_favorite_border_24);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            // setResult(Activity.RESULT_OK); // Đảm bảo HienThiAlbum cập nhật nếu có thay đổi (ví dụ yêu thích)
            finish();
            return true;
        } else if (itemId == R.id.action_toggle_favorite) {
            toggleFavoriteStatus();
            return true;
        } else if (itemId == R.id.action_edit_photo) {
            if (currentPhotoObject != null) {
                Intent editIntent = new Intent(this, EditPhotoActivity.class);
                editIntent.putExtra("PHOTO_ID_TO_EDIT", currentPhotoObject.getId());
                editPhotoLauncher.launch(editIntent);
            } else {
                Toast.makeText(this, "Không thể sửa, chi tiết ảnh chưa được tải.", Toast.LENGTH_SHORT).show();
            }
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
                    if (currentPhotoObject != null) {
                        currentPhotoObject.setFavorite(newFavoriteState);
                    }
                    invalidateOptionsMenu();
                    String message = isCurrentPhotoFavorite ? "Đã thêm vào yêu thích" : "Đã xóa khỏi yêu thích";
                    Toast.makeText(ViewPhotoActivity.this, message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Đã cập nhật trạng thái yêu thích cho photoId " + photoId + " thành " + isCurrentPhotoFavorite);
                    setResult(Activity.RESULT_OK); // Báo cho HienThiAlbum cập nhật
                } else {
                    Toast.makeText(ViewPhotoActivity.this, "Lỗi cập nhật yêu thích.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }


    private void loadCommentsFromDb() { // Sẽ được gọi bên trong loadPhotoDetailsAndComments
    }

    private void addNewComment(int currentPhotoId, String text, String author) {
        Log.d(TAG, "Đang thêm bình luận cho photoId: " + currentPhotoId);
        if (currentPhotoId == -1) return;
        executorService.execute(() -> {
            final boolean success = dbHelper.addComment(currentPhotoId, text, author);
            final List<Comment> updatedComments = success ? dbHelper.getCommentsForPhoto(currentPhotoId) : commentsList;
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(ViewPhotoActivity.this, "Đã thêm bình luận!", Toast.LENGTH_SHORT).show();
                    commentsList.clear();
                    commentsList.addAll(updatedComments);
                    commentAdapter.notifyDataSetChanged();
                    if (!commentsList.isEmpty()) {
                        commentsRecyclerView.smoothScrollToPosition(commentsList.size() - 1);
                    }
                } else {
                    Toast.makeText(ViewPhotoActivity.this, "Lỗi khi thêm bình luận.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Tải lại chi tiết ảnh và bình luận (nếu cần).");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}