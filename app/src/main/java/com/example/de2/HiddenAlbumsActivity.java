package com.example.de2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
// import androidx.swiperefreshlayout.widget.SwipeRefreshLayout; // Có thể không cần SwipeRefresh ở đây

import com.google.android.material.appbar.MaterialToolbar;
// import com.google.android.material.floatingactionbutton.FloatingActionButton; // Nếu có FAB

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HiddenAlbumsActivity extends AppCompatActivity implements AlbumAdapter.OnAlbumActionListener {
    private static final String TAG = "HiddenAlbumsActivity";
    // Request code nếu bạn mở EditAlbumActivity từ đây và muốn nhận kết quả
    public static final int EDIT_HIDDEN_ALBUM_REQUEST_CODE = 201;


    private MaterialToolbar toolbar;
    private RecyclerView recyclerViewHiddenAlbums;
    private AlbumAdapter albumAdapter; // Sử dụng lại AlbumAdapter
    private List<Album> hiddenAlbumList;
    private DatabaseHelper dbHelper;
    private TextView tvNoHiddenAlbums;
    // private FloatingActionButton fabCreateNewHiddenAlbum; // Nếu có FAB

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hidden_albums);

        toolbar = findViewById(R.id.toolbar_hidden_albums);
        recyclerViewHiddenAlbums = findViewById(R.id.recycler_view_hidden_albums);
        tvNoHiddenAlbums = findViewById(R.id.tv_no_hidden_albums);
        // fabCreateNewHiddenAlbum = findViewById(R.id.fab_create_new_hidden_album); // Nếu có

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Hiển thị nút back
            getSupportActionBar().setTitle("Album Ẩn");
        }

        dbHelper = new DatabaseHelper(this);
        hiddenAlbumList = new ArrayList<>();
        // Quan trọng: Truyền context của Activity này vào AlbumAdapter
        albumAdapter = new AlbumAdapter(this, hiddenAlbumList);

        recyclerViewHiddenAlbums.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewHiddenAlbums.setAdapter(albumAdapter);



    }
    @Override
    public void onAlbumDataChanged() {
        Log.d(TAG, "onAlbumDataChanged (HiddenAlbumsActivity): Dữ liệu album ẩn đã thay đổi, đang tải lại danh sách.");
        // Gọi phương thức tải lại danh sách album ẩn của bạn
        loadHiddenAlbums();
    }
    private void loadHiddenAlbums() {
        Log.d(TAG, "Đang tải danh sách album ẩn...");
        executorService.execute(() -> {
            final List<Album> loadedHiddenAlbums = dbHelper.getHiddenAlbums(); // Gọi phương thức lấy album ẩn
            runOnUiThread(() -> {
                hiddenAlbumList.clear();
                hiddenAlbumList.addAll(loadedHiddenAlbums);
                albumAdapter.notifyDataSetChanged();

                if (hiddenAlbumList.isEmpty()) {
                    tvNoHiddenAlbums.setVisibility(View.VISIBLE);
                    recyclerViewHiddenAlbums.setVisibility(View.GONE);
                    Log.d(TAG, "Không có album ẩn nào.");
                } else {
                    tvNoHiddenAlbums.setVisibility(View.GONE);
                    recyclerViewHiddenAlbums.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Đã tải " + hiddenAlbumList.size() + " album ẩn.");
                }
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Tải lại danh sách mỗi khi Activity này được hiển thị
        // để cập nhật nếu có thay đổi (ví dụ: một album được bỏ ẩn từ nơi khác)
        Log.d(TAG, "onResume: Tải lại danh sách album ẩn.");
        loadHiddenAlbums();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Xử lý nút back trên toolbar
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Xử lý kết quả trả về nếu bạn mở EditAlbumActivity hoặc ThemDanhMuc từ đây
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Ví dụ: nếu sửa album ẩn hoặc thêm album ẩn mới thành công
        if ((requestCode == EDIT_HIDDEN_ALBUM_REQUEST_CODE /* || requestCode == ADD_NEW_HIDDEN_ALBUM_REQUEST_CODE */)
                && resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "Một album ẩn đã được thay đổi, tải lại danh sách.");
            loadHiddenAlbums();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}