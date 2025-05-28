package com.example.de2;

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

public class FavoritePhotosActivity extends AppCompatActivity {

    private static final String TAG = "FavoritePhotosActivity";

    private MaterialToolbar toolbar;
    private RecyclerView recyclerViewFavoritePhotos;
    private PhotoAdapter photoAdapter; // Sử dụng lại PhotoAdapter
    private List<Photo> favoritePhotoList;
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
        photoAdapter = new PhotoAdapter(this, favoritePhotoList); // PhotoAdapter cần có khả năng xử lý click để mở ViewPhotoActivity

        // Hiển thị ảnh dạng lưới, ví dụ 3 cột
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
                photoAdapter.notifyDataSetChanged();

                if (favoritePhotoList.isEmpty()) {
                    tvNoFavoritePhotos.setVisibility(View.VISIBLE);
                    recyclerViewFavoritePhotos.setVisibility(View.GONE);
                    Log.d(TAG, "Không có ảnh yêu thích nào.");
                } else {
                    tvNoFavoritePhotos.setVisibility(View.GONE);
                    recyclerViewFavoritePhotos.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Đã tải " + favoritePhotoList.size() + " ảnh yêu thích.");
                }
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Tải lại danh sách mỗi khi Activity này được hiển thị
        // để cập nhật nếu có thay đổi từ màn hình ViewPhotoActivity
        loadFavoritePhotos();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Hoặc onBackPressed();
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