package com.example.de2;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface; // Thêm import cho AlertDialog
import android.content.Intent;
import android.util.Log; // Thêm Log để debug
import android.view.LayoutInflater;
import android.view.MenuItem; // Thêm import
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton; // Thêm import
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast; // Thêm import

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog; // Thêm import cho AlertDialog
import androidx.appcompat.widget.PopupMenu; // Thêm import
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder> {
    private static final String TAG = "AlbumAdapter"; // Tag cho Logcat
    private Context context;
    private List<Album> albumList;
    private DatabaseHelper dbHelper; // Thêm DatabaseHelper để có thể xóa

    public AlbumAdapter(Context context, List<Album> albumList) {
        this.context = context;
        this.albumList = albumList;
        this.dbHelper = new DatabaseHelper(context); // Khởi tạo DatabaseHelper
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // QUAN TRỌNG: Sử dụng file layout item đã được cập nhật với nút options
        // Ví dụ: R.layout.item_album (thay vì R.layout.danhmuclist nếu nó chưa có nút options)
        View view = LayoutInflater.from(context).inflate(R.layout.danhmuclist, parent, false);
        return new AlbumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        Album album = albumList.get(position);

        holder.tvName.setText(album.getName());
        holder.tvTopic.setText(album.getTopic());
        holder.tvTotalImages.setText(album.getTotalImages() + " ảnh");

        if (album.getImage() != null && album.getImage().length > 0) {
            Glide.with(context)
                    .load(album.getImage())
                    .placeholder(R.drawable.ic_placeholder_album)
                    .error(R.drawable.ic_placeholder_album)
                    .into(holder.ivImage);
        } else {
            Glide.with(context)
                    .load(R.drawable.ic_placeholder_album)
                    .into(holder.ivImage);
        }

        // Sự kiện click cho toàn bộ item (mở chi tiết album)
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, HienThiAlbum.class); // Activity hiển thị danh sách ảnh
            intent.putExtra("ALBUM_NAME", album.getName());
            intent.putExtra("albumId", album.getId());
            Log.d(TAG, "Mở HienThiAlbum cho albumId: " + album.getId());
            context.startActivity(intent);
        });

        // Sự kiện click cho nút tùy chọn (ba dấu chấm)
        holder.btnAlbumOptions.setOnClickListener(view -> {
            showAlbumOptionsMenu(view, album, position);
        });
    }

    private void showAlbumOptionsMenu(View anchorView, final Album album, final int position) {
        PopupMenu popup = new PopupMenu(context, anchorView);
        popup.getMenuInflater().inflate(R.menu.album_item_options_menu, popup.getMenu()); // Đảm bảo menu này có item R.id.menu_album_edit

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_album_edit) {
                // --- ĐÂY LÀ PHẦN QUAN TRỌNG ĐỂ MỞ TRANG CẬP NHẬT ---
                Log.d(TAG, "Người dùng chọn sửa album: " + album.getName() + ", ID: " + album.getId());

                Intent editIntent = new Intent(context, EditAlbumActivity.class);
                // Truyền dữ liệu của album cần sửa sang EditAlbumActivity
                editIntent.putExtra("ALBUM_ID", album.getId());
                editIntent.putExtra("ALBUM_NAME", album.getName());
                editIntent.putExtra("ALBUM_TOPIC", album.getTopic());
                // editIntent.putExtra("ALBUM_IMAGE", album.getImage()); // Cân nhắc việc truyền byte array qua Intent

                // Để nhận kết quả trả về từ EditAlbumActivity (ví dụ, để làm mới danh sách trong DanhMucAnh)
                // bạn cần gọi startActivityForResult từ Activity chứa RecyclerView này.
                if (context instanceof Activity) {
                    // DanhMucAnh.EDIT_ALBUM_REQUEST_CODE là một hằng số int bạn định nghĩa trong DanhMucAnh.java
                    // Ví dụ: public static final int EDIT_ALBUM_REQUEST_CODE = 101;
                    ((Activity) context).startActivityForResult(editIntent, DanhMucAnh.EDIT_ALBUM_REQUEST_CODE);
                } else {
                    // Trường hợp hiếm gặp khi context không phải là Activity
                    Log.e(TAG, "Context không phải là Activity, không thể gọi startActivityForResult. Sử dụng startActivity thông thường.");
                    context.startActivity(editIntent); // Sẽ không nhận được kết quả trả về
                }
                // --- KẾT THÚC PHẦN QUAN TRỌNG ---
                return true;

            } else if (itemId == R.id.menu_album_delete) {
                // ... (code xử lý xóa album của bạn) ...
                new AlertDialog.Builder(context)
                        .setTitle("Xác nhận xóa Album")
                        .setMessage("Bạn có chắc chắn muốn xóa album '" + album.getName() + "' không? Tất cả ảnh và bình luận liên quan cũng sẽ bị xóa.")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            deleteAlbum(album.getId(), position);
                        })
                        .setNegativeButton("Hủy", null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void deleteAlbum(int albumId, int position) {
        // TODO: Implement logic xóa album trong DatabaseHelper
        // Ví dụ: dbHelper.deleteAlbumAndAssociatedData(albumId);
        // Sau đó cập nhật RecyclerView
        // Bước 1: Xóa khỏi database (ví dụ)
        boolean deleted = dbHelper.deleteAlbum(albumId); // Giả sử bạn có hàm deleteAlbum(id) trong DatabaseHelper
        // Hàm này cũng nên xóa các ảnh và bình luận liên quan (ON DELETE CASCADE)

        if (deleted) {
            // Bước 2: Xóa khỏi danh sách trong adapter và thông báo
            if (position >= 0 && position < albumList.size()) {
                albumList.remove(position);
                notifyItemRemoved(position);
                // Cập nhật lại vị trí của các item còn lại (quan trọng nếu bạn không dùng notifyDataSetChanged())
                notifyItemRangeChanged(position, albumList.size() - position);
                Toast.makeText(context, "Đã xóa album.", Toast.LENGTH_SHORT).show();
                Log.d(TAG,"Đã xóa album ID: " + albumId + " tại vị trí: " + position);
            } else {
                Log.e(TAG, "Vị trí không hợp lệ để xóa: " + position + ", kích thước list: " + albumList.size());
                // Có thể tải lại toàn bộ danh sách nếu có lỗi vị trí
                // ((DanhMucAnh) context).loadAlbums(); // Nếu bạn có hàm loadAlbums trong DanhMucAnh
            }
        } else {
            Toast.makeText(context, "Lỗi khi xóa album.", Toast.LENGTH_SHORT).show();
            Log.e(TAG,"Lỗi khi xóa album ID: " + albumId + " từ database.");
        }
    }


    @Override
    public int getItemCount() {
        return albumList != null ? albumList.size() : 0;
    }

    public static class AlbumViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvTopic, tvTotalImages;
        ImageView ivImage;
        ImageButton btnAlbumOptions; // Thêm ImageButton cho nút tùy chọn

        public AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            // Đảm bảo các ID này khớp với file item_album.xml của bạn
            tvName = itemView.findViewById(R.id.tv_TenDanhMuc);
            tvTopic = itemView.findViewById(R.id.tv_ChuDe);
            tvTotalImages = itemView.findViewById(R.id.tv_TongSoAnh);
            ivImage = itemView.findViewById(R.id.iv_anhdanhmuc);
            btnAlbumOptions = itemView.findViewById(R.id.btn_album_options); // Ánh xạ nút tùy chọn
        }
    }

    // (Tùy chọn) Thêm phương thức để cập nhật danh sách nếu cần từ Activity
    public void updateAlbumList(List<Album> newAlbumList) {
        this.albumList.clear();
        this.albumList.addAll(newAlbumList);
        notifyDataSetChanged();
    }
}