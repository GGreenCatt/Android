package com.example.de2;

import android.app.Activity;
// import android.content.DialogInterface; // Không cần thiết nữa nếu AlertDialog Builder đúng
import android.content.Intent;
import android.os.Bundle;
// import android.text.InputType; // Không cần trực tiếp ở đây nữa nếu dialog layout đúng
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
// import android.widget.EditText; // TextInputEditText được dùng trong dialog
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
// Xóa import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
// import com.google.android.material.textfield.TextInputLayout; // Không cần trực tiếp ở đây nữa nếu dialog layout đúng

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class DanhMucAnh extends AppCompatActivity implements AlbumAdapter.OnAlbumActionListener {
    private static final String TAG = "DanhMucAnh";
    public static final int EDIT_ALBUM_REQUEST_CODE = 101;
    public static final int ADD_ALBUM_REQUEST_CODE = 102;

    RecyclerView recyclerView;
    AlbumAdapter albumAdapter;
    List<Album> albumList;
    List<Album> originalAlbumList;
    DatabaseHelper databaseHelper;
    FloatingActionButton fab_them_album;
    MaterialToolbar toolbar;
    // Xóa khai báo SwipeRefreshLayout swipeRefreshLayout;

    private PasswordManager passwordManager;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

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

        passwordManager = new PasswordManager(this);
        databaseHelper = new DatabaseHelper(this);

        fab_them_album.setOnClickListener(v -> {
            Intent intent1 = new Intent(DanhMucAnh.this, ThemDanhMuc.class);
            startActivityForResult(intent1, ADD_ALBUM_REQUEST_CODE);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        albumList = new ArrayList<>();
        originalAlbumList = new ArrayList<>();
        albumAdapter = new AlbumAdapter(this, albumList);
        recyclerView.setAdapter(albumAdapter);

        // Xóa phần thiết lập SwipeRefreshLayout listener
        // swipeRefreshLayout.setOnRefreshListener(() -> { ... });

        Log.d(TAG, "onCreate: Initializing.");
    }

    @Override
    public void onAlbumDataChanged() {
        Log.d(TAG, "onAlbumDataChanged: Dữ liệu album đã thay đổi, đang tải lại danh sách.");
        loadAlbums();
    }

    private void loadAlbums() {
        Log.d(TAG, "loadAlbums: Starting to load visible albums from database.");
        executorService.execute(() -> {
            final List<Album> tempAlbumList = databaseHelper.getVisibleAlbums();
            runOnUiThread(() -> {
                albumList.clear();
                albumList.addAll(tempAlbumList);
                originalAlbumList.clear();
                originalAlbumList.addAll(tempAlbumList);

                if (albumAdapter != null) {
                    albumAdapter.notifyDataSetChanged();
                }
                Log.d(TAG, "loadAlbums: Finished loading. Total visible albums in list: " + albumList.size());
                if (albumList.isEmpty() && !isFinishing()) {
                    Toast.makeText(this, "Chưa có album nào. Hãy thêm mới!", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void promptForHiddenSectionAccess() {
        if (!passwordManager.isPasswordSet()) {
            showCreatePasswordDialog();
        } else {
            showEnterPasswordDialog();
        }
    }

    private void showCreatePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tạo mật khẩu cho Mục ẩn");

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_create_password, (ViewGroup) findViewById(android.R.id.content), false);
        final TextInputEditText inputPassword = viewInflated.findViewById(R.id.edt_create_password);
        final TextInputEditText inputConfirmPassword = viewInflated.findViewById(R.id.edt_confirm_password);
        final TextInputLayout tilCreatePassword = viewInflated.findViewById(R.id.til_create_password); // Lấy TextInputLayout
        final TextInputLayout tilConfirmPassword = viewInflated.findViewById(R.id.til_confirm_password); // Lấy TextInputLayout


        builder.setView(viewInflated);
        builder.setPositiveButton("Tạo", null); // Sẽ override
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String password = inputPassword.getText().toString();
                String confirmPassword = inputConfirmPassword.getText().toString();
                boolean valid = true;

                if (password.isEmpty() || password.length() < 4) {
                    tilCreatePassword.setError("Mật khẩu phải có ít nhất 4 ký tự.");
                    valid = false;
                } else {
                    tilCreatePassword.setError(null);
                }

                if (!password.equals(confirmPassword)) {
                    tilConfirmPassword.setError("Mật khẩu xác nhận không khớp.");
                    valid = false;
                } else {
                    tilConfirmPassword.setError(null);
                }

                if (valid) {
                    if (passwordManager.setPassword(password)) {
                        Toast.makeText(DanhMucAnh.this, "Đã tạo mật khẩu cho Mục ẩn!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        navigateToHiddenAlbums();
                    } else {
                        Toast.makeText(DanhMucAnh.this, "Lỗi tạo mật khẩu.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
        dialog.show();
    }

    private void showEnterPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nhập mật khẩu Mục ẩn");

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_enter_password, (ViewGroup) findViewById(android.R.id.content), false);
        final TextInputEditText inputPassword = viewInflated.findViewById(R.id.edt_enter_password);
        final TextInputLayout tilEnterPassword = viewInflated.findViewById(R.id.til_enter_password); // Lấy TextInputLayout

        builder.setView(viewInflated);
        builder.setPositiveButton("OK", null); // Sẽ override
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String password = inputPassword.getText().toString();
                if (passwordManager.checkPassword(password)) {
                    Toast.makeText(DanhMucAnh.this, "Mật khẩu đúng!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    navigateToHiddenAlbums();
                } else {
                    tilEnterPassword.setError("Mật khẩu không đúng!");
                }
            });
        });
        dialog.show();
    }

    private void navigateToHiddenAlbums() {
        Log.d(TAG, "Điều hướng đến HiddenAlbumsActivity.");
        Intent intent = new Intent(this, HiddenAlbumsActivity.class);
        startActivity(intent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.danh_muc_anh_toolbar_menu, menu); // Đảm bảo menu này có action_access_hidden_section

        MenuItem searchItem = menu.findItem(R.id.action_search_albums);
        SearchView searchView = (SearchView) searchItem.getActionView();
        // ... (code SearchView listener giữ nguyên)
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
            return true;
        } else if (itemId == R.id.action_sort_albums) {
            showSortOptionsDialog();
            return true;
        } else if (itemId == R.id.action_view_favorite_photos) {
            Log.d(TAG, "Mở màn hình Ảnh Yêu Thích.");
            Intent intent = new Intent(this, FavoritePhotosActivity.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.action_access_hidden_section) { // XỬ LÝ CLICK NÚT Ổ KHÓA
            Log.d(TAG, "Toolbar icon 'Mục ẩn' clicked.");
            promptForHiddenSectionAccess(); // Gọi hàm xử lý dialog mật khẩu
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // filterAlbumList, showSortOptionsDialog, sortAlbumList giữ nguyên
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
        Collections.sort(albumList, comparator);
        if (albumAdapter != null) {
            albumAdapter.notifyDataSetChanged();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Reloading visible albums.");
        loadAlbums();
    }

    private void anhxa() {
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recycler_view_albums);
        fab_them_album = findViewById(R.id.fab_them_album);
        // Xóa dòng ánh xạ swipeRefreshLayout
        // swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout_albums);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_ALBUM_REQUEST_CODE || requestCode == ADD_ALBUM_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "Album đã được thay đổi (thêm/sửa), tải lại danh sách.");
                Toast.makeText(this, "Dữ liệu album đã được cập nhật!", Toast.LENGTH_SHORT).show();
                loadAlbums();
            } else {
                Log.d(TAG, "Việc thay đổi album bị hủy hoặc có lỗi, resultCode: " + resultCode);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        // Nếu AlbumAdapter có hàm shutdownExecutor, bạn có thể gọi ở đây
        // if (albumAdapter != null) {
        //     albumAdapter.shutdownExecutor();
        // }
    }
}