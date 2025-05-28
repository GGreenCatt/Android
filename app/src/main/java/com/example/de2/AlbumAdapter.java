package com.example.de2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder> {
    private static final String TAG = "AlbumAdapter";
    private Context context;
    private List<Album> albumList;
    private DatabaseHelper dbHelper;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public interface OnAlbumActionListener {
        void onAlbumDataChanged();
        // (Tùy chọn) Thêm các phương thức khác nếu cần, ví dụ:
        // void onRequestPasswordForHiding(Album albumToHide, int position);
    }
    private OnAlbumActionListener actionListener;

    public AlbumAdapter(Context context, List<Album> albumList) {
        this.context = context;
        this.albumList = albumList;
        this.dbHelper = new DatabaseHelper(context);
        if (context instanceof OnAlbumActionListener) {
            this.actionListener = (OnAlbumActionListener) context;
        } else {
            // Throw exception hoặc Log lỗi nếu Activity không implement interface
            // Vì các thao tác quan trọng như làm mới danh sách sẽ không hoạt động
            throw new RuntimeException(context.toString()
                    + " must implement OnAlbumActionListener");
        }
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Đảm bảo R.layout.item_album là layout đúng và có btn_album_options
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
                    .centerCrop() // Thêm centerCrop để ảnh trông đẹp hơn trong ImageView cố định
                    .into(holder.ivImage);
        } else {
            Glide.with(context)
                    .load(R.drawable.ic_placeholder_album)
                    .into(holder.ivImage);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, HienThiAlbum.class);
            intent.putExtra("ALBUM_NAME", album.getName());
            intent.putExtra("albumId", album.getId());
            Log.d(TAG, "Mở HienThiAlbum cho albumId: " + album.getId() + ", isHidden: " + album.isHidden());
            context.startActivity(intent);
        });

        if (holder.btnAlbumOptions != null) {
            holder.btnAlbumOptions.setOnClickListener(view -> {
                showAlbumOptionsMenu(view, album, position);
            });
        } else {
            // Lỗi này không nên xảy ra nếu R.layout.item_album đã đúng
            Log.e(TAG, "Lỗi nghiêm trọng: btnAlbumOptions is null. Kiểm tra R.layout.item_album và ID btn_album_options.");
        }
    }

    private void showAlbumOptionsMenu(View anchorView, final Album album, final int position) {
        PopupMenu popup = new PopupMenu(context, anchorView);
        popup.getMenuInflater().inflate(R.menu.album_item_options_menu, popup.getMenu());

        MenuItem toggleHiddenItem = popup.getMenu().findItem(R.id.menu_album_toggle_hidden);
        // Logic xác định xem context là DanhMucAnh hay HiddenAlbumsActivity
        // để quyết định tiêu đề "Ẩn" hay "Hiện"
        if (context instanceof HiddenAlbumsActivity) { // Nếu đang ở màn hình Album Ẩn
            toggleHiddenItem.setTitle("Hiện Album");
        } else { // Mặc định là màn hình DanhMucAnh (album đang hiện)
            toggleHiddenItem.setTitle("Ẩn Album");
        }
        // Nếu bạn muốn tiêu đề thay đổi dựa trên trạng thái album.isHidden() thì nên làm như sau:
        // if (album.isHidden()) {
        //     toggleHiddenItem.setTitle("Hiện Album");
        // } else {
        //     toggleHiddenItem.setTitle("Ẩn Album");
        // }


        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_album_edit) {
                Log.d(TAG, "Chọn sửa album: " + album.getName() + ", ID: " + album.getId());
                Intent editIntent = new Intent(context, EditAlbumActivity.class);
                editIntent.putExtra("ALBUM_ID", album.getId());
                editIntent.putExtra("ALBUM_NAME", album.getName());
                editIntent.putExtra("ALBUM_TOPIC", album.getTopic());

                if (context instanceof Activity) {
                    int requestCode = (context instanceof DanhMucAnh) ?
                            DanhMucAnh.EDIT_ALBUM_REQUEST_CODE :
                            HiddenAlbumsActivity.EDIT_HIDDEN_ALBUM_REQUEST_CODE; // Đảm bảo HiddenAlbumsActivity có hằng số này
                    ((Activity) context).startActivityForResult(editIntent, requestCode);
                } else {
                    context.startActivity(editIntent);
                }
                return true;

            } else if (itemId == R.id.menu_album_toggle_hidden) {
                // TODO: Cân nhắc việc kiểm tra mật khẩu cho mục ẩn ở đây.
                // Nếu chưa có mật khẩu, Adapter nên thông báo cho Activity để yêu cầu người dùng tạo.
                // Ví dụ: if (passwordManager.isPasswordSet()) { toggle... } else { actionListener.onRequestPasswordSetup(); }
                toggleAlbumHiddenStatus(album, position);
                return true;

            } else if (itemId == R.id.menu_album_delete) {
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

    private void toggleAlbumHiddenStatus(Album album, int position) {
        final boolean newHiddenState = !album.isHidden();
        Log.d(TAG, "Thay đổi trạng thái ẩn cho albumId " + album.getId() + " thành " + newHiddenState);

        executorService.execute(() -> {
            boolean success = dbHelper.setAlbumHiddenStatus(album.getId(), newHiddenState);
            // Chuyển về UI thread để cập nhật giao diện và thông báo
            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(() -> {
                    if (success) {
                        // Xóa item khỏi danh sách hiện tại của adapter và thông báo
                        // vì nó sẽ không còn thuộc danh sách này nữa (chuyển sang ẩn hoặc hiện)
                        if (position >= 0 && position < albumList.size()) {
                            albumList.remove(position);
                            notifyItemRemoved(position);
                            notifyItemRangeChanged(position, albumList.size()); // Quan trọng để cập nhật các vị trí còn lại
                        }

                        String message = newHiddenState ? "Đã ẩn album." : "Album đã được hiển thị.";
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();

                        // Thông báo cho Activity để tải lại danh sách (quan trọng)
                        if (actionListener != null) {
                            actionListener.onAlbumDataChanged();
                        } else {
                            Log.e(TAG, "actionListener is null in toggleAlbumHiddenStatus. Cannot refresh Activity list.");
                        }

                    } else {
                        Toast.makeText(context, "Lỗi khi thay đổi trạng thái ẩn của album.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }


    private void deleteAlbum(int albumId, int position) {
        Log.d(TAG, "Chuẩn bị xóa album ID: " + albumId + " tại vị trí: " + position);
        executorService.execute(() -> {
            final boolean deleted = dbHelper.deleteAlbum(albumId);
            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(() -> {
                    if (deleted) {
                        if (position >= 0 && position < albumList.size()) {
                            albumList.remove(position);
                            notifyItemRemoved(position);
                            notifyItemRangeChanged(position, albumList.size() - position); // Cập nhật các vị trí còn lại
                            Toast.makeText(context, "Đã xóa album.", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Đã xóa album ID: " + albumId + " tại vị trí: " + position);

                            if (actionListener != null) {
                                actionListener.onAlbumDataChanged(); // Thông báo để Activity có thể cập nhật UI khác (ví dụ: thông báo không có album)
                            }
                        } else {
                            Log.e(TAG, "Vị trí không hợp lệ để xóa: " + position + ", kích thước list: " + albumList.size());
                            if (actionListener != null) { // Nếu vị trí sai, vẫn nên load lại toàn bộ
                                actionListener.onAlbumDataChanged();
                            }
                        }
                    } else {
                        Toast.makeText(context, "Lỗi khi xóa album.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Lỗi khi xóa album ID: " + albumId + " từ database.");
                    }
                });
            }
        });
    }


    @Override
    public int getItemCount() {
        return albumList != null ? albumList.size() : 0;
    }

    public static class AlbumViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvTopic, tvTotalImages;
        ImageView ivImage;
        ImageButton btnAlbumOptions;

        public AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_TenDanhMuc);
            tvTopic = itemView.findViewById(R.id.tv_ChuDe);
            tvTotalImages = itemView.findViewById(R.id.tv_TongSoAnh);
            ivImage = itemView.findViewById(R.id.iv_anhdanhmuc);
            btnAlbumOptions = itemView.findViewById(R.id.btn_album_options); // ID từ item_album.xml
        }
    }

    // Phương thức này vẫn giữ nguyên, nhưng Activity sẽ chịu trách nhiệm
    // cung cấp danh sách đúng (chỉ visible albums hoặc chỉ hidden albums)
    public void updateAlbumList(List<Album> newAlbumList) {
        this.albumList.clear();
        this.albumList.addAll(newAlbumList);
        notifyDataSetChanged();
    }

    // Giải phóng ExecutorService khi Adapter không còn được sử dụng
    // (Ví dụ, có thể gọi từ onDestroy của Activity nếu Adapter chỉ dùng ở đó)
    public void shutdownExecutor() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            Log.d(TAG, "ExecutorService in AlbumAdapter has been shut down.");
        }
    }
}