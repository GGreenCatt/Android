package com.example.de2;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class DanhMucAnh extends AppCompatActivity {
    private static final String TAG = "DanhMucAnh";
    public static final int EDIT_ALBUM_REQUEST_CODE = 101;
    // (Tùy chọn) Thêm request code nếu ThemDanhMucActivity trả về kết quả
    // public static final int ADD_ALBUM_REQUEST_CODE = 102;

    RecyclerView recyclerView;
    AlbumAdapter albumAdapter;
    List<Album> albumList;
    List<Album> originalAlbumList;
    DatabaseHelper databaseHelper;
    FloatingActionButton fab_them_album;
    MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_danh_muc_anh);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        anhxa();
        setSupportActionBar(toolbar);

        fab_them_album.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent1 = new Intent(DanhMucAnh.this, ThemDanhMuc.class);
                // Nếu ThemDanhMuc trả kết quả, dùng startActivityForResult hoặc ActivityResultLauncher
                // ví dụ: startActivityForResult(intent1, ADD_ALBUM_REQUEST_CODE);
                startActivity(intent1);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        albumList = new ArrayList<>();
        originalAlbumList = new ArrayList<>();
        databaseHelper = new DatabaseHelper(this);

        albumAdapter = new AlbumAdapter(this, albumList);
        recyclerView.setAdapter(albumAdapter);

        Log.d(TAG, "onCreate: Initializing.");
        // loadAlbums() sẽ được gọi trong onResume
    }

    private void loadAlbums() {
        Log.d(TAG, "loadAlbums: Starting to load albums from database.");
        List<Album> tempAlbumList = new ArrayList<>();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, name, topic, image FROM albums ORDER BY name ASC", null);

        if (cursor != null && cursor.moveToFirst()) {
            Log.d(TAG, "loadAlbums: Found " + cursor.getCount() + " albums in 'albums' table.");
            do {
                int idIndex = cursor.getColumnIndex("id");
                int nameIndex = cursor.getColumnIndex("name");
                int topicIndex = cursor.getColumnIndex("topic");
                int imageIndex = cursor.getColumnIndex("image");

                int albumId = (idIndex != -1) ? cursor.getInt(idIndex) : -1;
                String name = (nameIndex != -1) ? cursor.getString(nameIndex) : "N/A";
                String topic = (topicIndex != -1) ? cursor.getString(topicIndex) : "";
                byte[] image = (imageIndex != -1) ? cursor.getBlob(imageIndex) : null;

                if (albumId == -1) continue;

                int totalImages = 0;
                String queryCount = "SELECT COUNT(*) FROM photos WHERE album_id = ?";
                Cursor imageCursor = db.rawQuery(queryCount, new String[]{String.valueOf(albumId)});
                if (imageCursor != null && imageCursor.moveToFirst()) {
                    totalImages = imageCursor.getInt(0);
                }
                if (imageCursor != null) {
                    imageCursor.close();
                }

                Album album = new Album(albumId, name, topic, image);
                album.setTotalImages(totalImages);
                tempAlbumList.add(album);

            } while (cursor.moveToNext());
        } else {
            Log.d(TAG, "loadAlbums: No albums found in 'albums' table or cursor is null.");
        }
        if (cursor != null) {
            cursor.close();
        }

        albumList.clear();
        albumList.addAll(tempAlbumList);
        originalAlbumList.clear();
        originalAlbumList.addAll(tempAlbumList);

        if (albumAdapter != null) {
            Log.d(TAG, "loadAlbums: Notifying adapter dataset changed.");
            albumAdapter.notifyDataSetChanged();
        }
        Log.d(TAG, "loadAlbums: Finished loading. Total albums in list: " + albumList.size());
        if (albumList.isEmpty() && !isFinishing()){
            Toast.makeText(this, "Chưa có album nào. Hãy thêm mới!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.danh_muc_anh_toolbar_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search_albums);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "Search submitted: " + query);
                filterAlbumList(query);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "Search text changed: " + newText);
                filterAlbumList(newText);
                return true;
            }
        });

        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                Log.d(TAG, "Search closed, restoring original list.");
                // Khôi phục lại danh sách đầy đủ khi SearchView đóng
                albumList.clear();
                albumList.addAll(originalAlbumList);
                if (albumAdapter != null) {
                    albumAdapter.notifyDataSetChanged();
                }
                return true;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_search_albums) {
            // Đã được xử lý bởi SearchView
            return true;
        } else if (itemId == R.id.action_sort_albums) {
            showSortOptionsDialog();
            return true;
        } else if (itemId == R.id.action_view_favorite_photos) { // XỬ LÝ ITEM MENU "ẢNH YÊU THÍCH"
            Log.d(TAG, "Mở màn hình Ảnh Yêu Thích.");
            Intent intent = new Intent(this, FavoritePhotosActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void filterAlbumList(String query) {
        List<Album> filteredList = new ArrayList<>();
        if (query.isEmpty()) {
            filteredList.addAll(originalAlbumList);
        } else {
            String filterPattern = query.toLowerCase(Locale.getDefault()).trim();
            for (Album album : originalAlbumList) {
                if (album.getName().toLowerCase(Locale.getDefault()).contains(filterPattern) ||
                        (album.getTopic() != null && album.getTopic().toLowerCase(Locale.getDefault()).contains(filterPattern))) {
                    filteredList.add(album);
                }
            }
        }
        albumList.clear();
        albumList.addAll(filteredList);
        if (albumAdapter != null) {
            albumAdapter.notifyDataSetChanged();
        }
    }

    private void showSortOptionsDialog() {
        final CharSequence[] items = {"Tên (A-Z)", "Tên (Z-A)", "Số lượng ảnh (Tăng dần)", "Số lượng ảnh (Giảm dần)"};
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Sắp xếp Album theo");
        builder.setItems(items, (dialog, which) -> {
            switch (which) {
                case 0:
                    sortAlbumList(Comparator.comparing(a -> a.getName().toLowerCase(Locale.getDefault())));
                    Toast.makeText(DanhMucAnh.this, "Đã sắp xếp theo tên (A-Z)", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    sortAlbumList(Comparator.comparing((Album a) -> a.getName().toLowerCase(Locale.getDefault())).reversed());
                    Toast.makeText(DanhMucAnh.this, "Đã sắp xếp theo tên (Z-A)", Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    sortAlbumList(Comparator.comparingInt(Album::getTotalImages));
                    Toast.makeText(DanhMucAnh.this, "Đã sắp xếp theo số lượng ảnh (Tăng dần)", Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    sortAlbumList(Comparator.comparingInt(Album::getTotalImages).reversed());
                    Toast.makeText(DanhMucAnh.this, "Đã sắp xếp theo số lượng ảnh (Giảm dần)", Toast.LENGTH_SHORT).show();
                    break;
            }
        });
        builder.show();
    }

    private void sortAlbumList(Comparator<Album> comparator) {
        // Khi SearchView đang hoạt động (người dùng đang lọc), chúng ta nên sắp xếp danh sách đã lọc.
        // Nếu không, sắp xếp danh sách gốc và cập nhật lại danh sách hiển thị.
        // Hiện tại, code đang sắp xếp `albumList` (danh sách hiển thị).
        // Nếu người dùng đang lọc, `albumList` sẽ là danh sách đã lọc.
        // Nếu người dùng không lọc, `albumList` sẽ giống `originalAlbumList`.
        Collections.sort(albumList, comparator);
        if (albumAdapter != null) {
            albumAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Reloading albums.");
        loadAlbums();
    }

    private void anhxa() {
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recycler_view_albums);
        fab_them_album = findViewById(R.id.fab_them_album);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_ALBUM_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "Album đã được cập nhật, tải lại danh sách từ onActivityResult.");
                Toast.makeText(this, "Album đã được cập nhật!", Toast.LENGTH_SHORT).show();
                loadAlbums();
            } else {
                Log.d(TAG, "Việc sửa album bị hủy hoặc có lỗi, resultCode: " + resultCode);
            }
        }
        // (Tùy chọn) Xử lý kết quả từ ThemDanhMucActivity nếu bạn dùng startActivityForResult
        // if (requestCode == ADD_ALBUM_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
        //    Log.d(TAG, "Album mới đã được thêm, tải lại danh sách.");
        //    Toast.makeText(this, "Đã thêm album mới!", Toast.LENGTH_SHORT).show();
        //    loadAlbums();
        // }
    }
}