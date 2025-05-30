package com.example.de2;

import android.app.Activity; // Thêm import
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton; // Thêm import
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog; // Thêm import
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;
import java.util.concurrent.ExecutorService; // Thêm import
import java.util.concurrent.Executors;   // Thêm import

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {
    private static final String TAG = "PhotoAdapter";
    private Context context;
    private List<Photo> photoList;
    private DatabaseHelper dbHelper; // Thêm DatabaseHelper
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(); // Thêm ExecutorService

    // Interface để callback về HienThiAlbum Activity
    public interface OnPhotoActionListener {
        void onPhotoDeleted(); // Được gọi sau khi một ảnh bị xóa
        void onPhotoFavoriteChanged(int photoId, boolean isFavorite); // Được gọi khi trạng thái yêu thích thay đổi
    }
    private OnPhotoActionListener actionListener;

    public PhotoAdapter(Context context, List<Photo> photoList) {
        this.context = context;
        this.photoList = photoList;
        this.dbHelper = new DatabaseHelper(context); // Khởi tạo dbHelper
        if (context instanceof OnPhotoActionListener) {
            this.actionListener = (OnPhotoActionListener) context;
        } else {
            // Ghi log cảnh báo nếu Activity không implement listener và bạn dựa vào nó
            Log.w(TAG, context.toString() + " should implement OnPhotoActionListener for certain actions.");
        }
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        Photo currentPhoto = photoList.get(position);

        byte[] imageBytes = currentPhoto.getImage();
        if (imageBytes != null && imageBytes.length > 0) {
            Glide.with(context)
                    .load(imageBytes)
                    .placeholder(R.drawable.ic_placeholder_album)
                    .error(R.drawable.ic_placeholder_album)
                    .centerCrop()
                    .into(holder.ivPhoto);
        } else {
            holder.ivPhoto.setImageResource(R.drawable.ic_placeholder_album);
        }

        // Cập nhật icon yêu thích (nếu có nút yêu thích trên item)
        if (holder.btnToggleFavoriteInList != null) {
            holder.btnToggleFavoriteInList.setVisibility(View.VISIBLE); // Cho hiện nút
            if (currentPhoto.isFavorite()) {
                holder.btnToggleFavoriteInList.setImageResource(R.drawable.ic_favorite_filled_24);
                // holder.btnToggleFavoriteInList.setColorFilter(ContextCompat.getColor(context, R.color.your_favorite_color)); // Đặt màu nếu muốn
            } else {
                holder.btnToggleFavoriteInList.setImageResource(R.drawable.ic_favorite_border_24);
                // holder.btnToggleFavoriteInList.clearColorFilter(); // Xóa màu nếu có
            }

            holder.btnToggleFavoriteInList.setOnClickListener(v -> {
                toggleFavoriteStatus(currentPhoto, position);
            });
        }


        // Sự kiện click vào ảnh để xem chi tiết
        holder.ivPhoto.setOnClickListener(v -> { // Hoặc holder.itemView.setOnClickListener
            Intent intent = new Intent(context, ViewPhotoActivity.class);
            // ... (putExtra PHOTO_ID và PHOTO_IMAGE_DATA như cũ) ...
            intent.putExtra("PHOTO_ID", currentPhoto.getId());
            intent.putExtra("PHOTO_IMAGE_DATA", currentPhoto.getImage());
            // (Tùy chọn) Bạn có thể dùng ActivityResultLauncher nếu ViewPhotoActivity trả về kết quả
            // Ví dụ: nếu trạng thái yêu thích thay đổi trong ViewPhotoActivity, nó cần báo lại
            if (context instanceof Activity) {
                // ((Activity) context).startActivityForResult(intent, REQUEST_CODE_VIEW_PHOTO); // Tạo REQUEST_CODE
                context.startActivity(intent); // Đơn giản hơn, HienThiAlbum sẽ tự refresh trong onResume
            } else {
                context.startActivity(intent);
            }
        });

        // Sự kiện click cho nút xóa ảnh
        if (holder.btnDeleteSinglePhoto != null) {
            holder.btnDeleteSinglePhoto.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Xác nhận xóa ảnh")
                        .setMessage("Bạn có chắc chắn muốn xóa ảnh này không?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            deletePhoto(currentPhoto.getId(), position);
                        })
                        .setNegativeButton("Hủy", null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            });
        }
    }

    private void toggleFavoriteStatus(Photo photo, int position) {
        final boolean newFavoriteState = !photo.isFavorite();
        executorService.execute(() -> {
            boolean success = dbHelper.setPhotoFavoriteStatus(photo.getId(), newFavoriteState);
            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(() -> {
                    if (success) {
                        photo.setFavorite(newFavoriteState); // Cập nhật trạng thái trong đối tượng
                        notifyItemChanged(position); // Chỉ cập nhật item này
                        String message = newFavoriteState ? "Đã thêm vào yêu thích" : "Đã xóa khỏi yêu thích";
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                        if (actionListener != null) {
                            actionListener.onPhotoFavoriteChanged(photo.getId(), newFavoriteState);
                        }
                    } else {
                        Toast.makeText(context, "Lỗi cập nhật yêu thích.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }


    private void deletePhoto(int photoId, int position) {
        Log.d(TAG, "Chuẩn bị xóa ảnh ID: " + photoId + " tại vị trí: " + position);
        executorService.execute(() -> {
            final boolean deleted = dbHelper.deletePhoto(photoId); // Gọi hàm mới trong DatabaseHelper
            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(() -> {
                    if (deleted) {
                        if (position >= 0 && position < photoList.size()) {
                            photoList.remove(position);
                            notifyItemRemoved(position);
                            notifyItemRangeChanged(position, photoList.size() - position);
                            Toast.makeText(context, "Đã xóa ảnh.", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Đã xóa ảnh ID: " + photoId);
                            if (actionListener != null) {
                                actionListener.onPhotoDeleted(); // Thông báo cho Activity
                            }
                        }
                    } else {
                        Toast.makeText(context, "Lỗi khi xóa ảnh.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Lỗi khi xóa ảnh ID: " + photoId + " từ database.");
                    }
                });
            }
        });
    }

    @Override
    public int getItemCount() {
        return photoList != null ? photoList.size() : 0;
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        ImageButton btnDeleteSinglePhoto; // Nút xóa
        ImageButton btnToggleFavoriteInList; // Nút yêu thích (tùy chọn)

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.iv_photo);
            btnDeleteSinglePhoto = itemView.findViewById(R.id.btn_delete_single_photo); // Ánh xạ nút xóa
            btnToggleFavoriteInList = itemView.findViewById(R.id.btn_toggle_favorite_in_list); // Ánh xạ nút yêu thích
        }
    }

    public void shutdownExecutor() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}