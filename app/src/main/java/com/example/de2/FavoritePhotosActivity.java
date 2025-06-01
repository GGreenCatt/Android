package com.example.de2;

import android.app.Activity; // Thêm nếu chưa có
import android.content.Intent; // Thêm nếu chưa có
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Implement PhotoAdapter.OnPhotoActionListener
public class FavoritePhotosActivity extends AppCompatActivity implements PhotoAdapter.OnPhotoActionListener {

    private static final String TAG = "FavoritePhotosActivity";

    private MaterialToolbar toolbar;
    private RecyclerView recyclerViewFavoritePhotos;
    private PhotoAdapter photoAdapter;
    private List<Photo> favoritePhotoList; // Danh sách ảnh yêu thích hiển thị
    private DatabaseHelper dbHelper;
    private TextView tvNoFavoritePhotos;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_photos);

        toolbar = findViewById(R.id.toolbar_favorite_photos);
        recyclerViewFavoritePhotos = findViewById(R.id.recycler_view_favorite_photos);
        tvNoFavoritePhotos = findViewById(R.id.tv_no_favorite_photos);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Ảnh Yêu Thích");
        }

        dbHelper = new DatabaseHelper(this);
        favoritePhotoList = new ArrayList<>();
        // PhotoAdapter sẽ nhận 'this' (FavoritePhotosActivity) làm OnPhotoActionListener
        photoAdapter = new PhotoAdapter(this, favoritePhotoList);

        recyclerViewFavoritePhotos.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerViewFavoritePhotos.setAdapter(photoAdapter);

        // loadFavoritePhotos(); // Sẽ được gọi trong onResume
    }

    private void loadFavoritePhotos() {
        Log.d(TAG, "Đang tải danh sách ảnh yêu thích...");
        executorService.execute(() -> {
            final List<Photo> loadedFavorites = dbHelper.getFavoritePhotos();
            runOnUiThread(() -> {
                favoritePhotoList.clear();
                favoritePhotoList.addAll(loadedFavorites);
                photoAdapter.notifyDataSetChanged(); // Thông báo cho adapter về toàn bộ dữ liệu mới
                checkIfListEmpty(); // Kiểm tra và cập nhật thông báo nếu danh sách rỗng
                Log.d(TAG, "Đã tải " + favoritePhotoList.size() + " ảnh yêu thích.");
            });
        });
    }

    private void checkIfListEmpty() {
        if (favoritePhotoList.isEmpty()) {
            tvNoFavoritePhotos.setVisibility(View.VISIBLE);
            recyclerViewFavoritePhotos.setVisibility(View.GONE);
        } else {
            tvNoFavoritePhotos.setVisibility(View.GONE);
            recyclerViewFavoritePhotos.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Tải lại danh sách ảnh yêu thích.");
        loadFavoritePhotos(); // Luôn tải lại khi Activity resume để đảm bảo tính nhất quán
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
        if (photoAdapter != null) {
            photoAdapter.shutdownExecutor(); // Đảm bảo giải phóng executor của adapter
        }
    }

    // --- Implementation of PhotoAdapter.OnPhotoActionListener ---

    @Override
    public void onPhotoDeleted() {
        // Nếu một ảnh bị xóa (không chỉ bỏ thích) và nó đang nằm trong danh sách yêu thích
        // thì loadFavoritePhotos() trong onResume sẽ xử lý việc này.
        // Hoặc có thể gọi loadFavoritePhotos() ở đây nếu cần phản ứng ngay lập tức.
        Log.d(TAG, "onPhotoDeleted callback received. Reloading favorites.");
        loadFavoritePhotos();
    }

    @Override
    public void onPhotoFavoriteChanged(int photoId, boolean isFavorite) {
        Log.d(TAG, "onPhotoFavoriteChanged callback: photoId=" + photoId + ", isFavorite=" + isFavorite);
        if (!isFavorite) { // Nếu ảnh bị BỎ THÍCH
            boolean itemRemoved = false;
            for (int i = 0; i < favoritePhotoList.size(); i++) {
                if (favoritePhotoList.get(i).getId() == photoId) {
                    favoritePhotoList.remove(i); // Xóa khỏi danh sách dữ liệu
                    photoAdapter.notifyItemRemoved(i); // Thông báo cho adapter xóa item tại vị trí đó
                    // Thông báo cho adapter về sự thay đổi phạm vi của các item còn lại
                    photoAdapter.notifyItemRangeChanged(i, favoritePhotoList.size() - i);
                    itemRemoved = true;
                    Log.d(TAG, "Ảnh ID " + photoId + " đã được xóa khỏi danh sách yêu thích.");
                    break;
                }
            }
            if (itemRemoved) {
                checkIfListEmpty(); // Cập nhật hiển thị "Không có ảnh yêu thích" nếu cần
            }
        } else {
            // Nếu ảnh được ĐÁNH DẤU YÊU THÍCH (trường hợp này ít xảy ra từ FavoritePhotosActivity,
            // nhưng để đảm bảo, có thể tải lại danh sách)
            // Hoặc nếu bạn muốn thêm nó vào danh sách hiện tại mà không tải lại hoàn toàn.
            // loadFavoritePhotos();
            Log.d(TAG, "Ảnh ID " + photoId + " được đánh dấu yêu thích. Danh sách sẽ cập nhật trong onResume nếu cần.");
        }
    }
}