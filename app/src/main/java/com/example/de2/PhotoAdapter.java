package com.example.de2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {
    private static final String TAG = "PhotoAdapter";
    private Context context;
    private List<Photo> photoList;
    private DatabaseHelper dbHelper;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public interface OnPhotoActionListener {
        void onPhotoDeleted();
        void onPhotoFavoriteChanged(int photoId, boolean isFavorite);
    }
    private OnPhotoActionListener actionListener;

    public PhotoAdapter(Context context, List<Photo> photoList) {
        this.context = context;
        this.photoList = photoList; // Đây là tham chiếu đến danh sách trong Activity
        this.dbHelper = new DatabaseHelper(context);
        if (context instanceof OnPhotoActionListener) { // FavoritePhotosActivity sẽ được gán làm listener
            this.actionListener = (OnPhotoActionListener) context;
        } else {
            Log.w(TAG, context.toString() + " nên implement OnPhotoActionListener để xử lý thay đổi trạng thái yêu thích.");
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

        if (holder.btnToggleFavoriteInList != null) {
            holder.btnToggleFavoriteInList.setVisibility(View.VISIBLE);
            if (currentPhoto.isFavorite()) {
                holder.btnToggleFavoriteInList.setImageResource(R.drawable.ic_favorite_filled_24);
            } else {
                holder.btnToggleFavoriteInList.setImageResource(R.drawable.ic_favorite_border_24);
            }

            holder.btnToggleFavoriteInList.setOnClickListener(v -> {
                toggleFavoriteStatus(currentPhoto, position);
            });
        }


        holder.ivPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(context, ViewPhotoActivity.class);
            intent.putExtra("PHOTO_ID", currentPhoto.getId());

            if (context instanceof Activity) {

                ((Activity) context).startActivity(intent);

            } else {
                context.startActivity(intent);
            }
        });

        if (holder.btnDeleteSinglePhoto != null) {
            holder.btnDeleteSinglePhoto.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Xác nhận xóa ảnh")
                        .setMessage("Bạn có chắc chắn muốn xóa ảnh này không?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            deletePhoto(currentPhoto.getId(), holder.getAdapterPosition()); // Sử dụng getAdapterPosition()
                        })
                        .setNegativeButton("Hủy", null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            });
        }
    }

    private void toggleFavoriteStatus(Photo photo, int position) {
        if (position == RecyclerView.NO_POSITION) { // Kiểm tra vị trí hợp lệ
            Log.w(TAG, "toggleFavoriteStatus: Invalid position received.");
            return;
        }
        final boolean newFavoriteState = !photo.isFavorite();
        executorService.execute(() -> {
            boolean success = dbHelper.setPhotoFavoriteStatus(photo.getId(), newFavoriteState);
            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(() -> {
                    if (success) {
                        // Cập nhật trạng thái của đối tượng Photo trong danh sách của adapter
                        // (quan trọng nếu danh sách này được chia sẻ và sửa đổi bởi Activity)
                        if (position < photoList.size() && photoList.get(position).getId() == photo.getId()) {
                            photoList.get(position).setFavorite(newFavoriteState);
                        }

                        Toast.makeText(context, newFavoriteState ? "Đã thêm vào yêu thích" : "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();

                        if (actionListener != null) {
                            actionListener.onPhotoFavoriteChanged(photo.getId(), newFavoriteState);
                        } else {

                            notifyItemChanged(position); // Để cập nhật icon nếu listener không làm gì khác
                        }
                    } else {
                        Toast.makeText(context, "Lỗi cập nhật yêu thích.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }


    private void deletePhoto(int photoId, int position) {
        if (position == RecyclerView.NO_POSITION) {
            Log.w(TAG, "deletePhoto: Invalid position.");
            // Có thể thử tìm lại photoId trong list nếu position không hợp lệ nhưng không khuyến khích
            return;
        }
        Log.d(TAG, "Chuẩn bị xóa ảnh ID: " + photoId + " tại vị trí: " + position);
        executorService.execute(() -> {
            final boolean deleted = dbHelper.deletePhoto(photoId);
            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(() -> {
                    if (deleted) {
                        // Kiểm tra lại vị trí trước khi xóa khỏi danh sách của adapter
                        if (position < photoList.size() && photoList.get(position).getId() == photoId) {
                            photoList.remove(position);
                            notifyItemRemoved(position);
                            notifyItemRangeChanged(position, photoList.size() - position);
                            Toast.makeText(context, "Đã xóa ảnh.", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Đã xóa ảnh ID: " + photoId);
                            if (actionListener != null) {
                                actionListener.onPhotoDeleted(); // Thông báo cho Activity
                            }
                        } else {
                            Log.w(TAG, "deletePhoto: Photo ID mismatch or position out of bounds after DB delete. Position: " + position + ", List size: " + photoList.size());
                            // Nếu có sự không khớp, yêu cầu Activity tải lại toàn bộ cho chắc chắn
                            if (actionListener != null) {
                                actionListener.onPhotoDeleted(); // Vẫn thông báo để Activity có thể refresh
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
        ImageButton btnDeleteSinglePhoto;
        ImageButton btnToggleFavoriteInList;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.iv_photo);
            btnDeleteSinglePhoto = itemView.findViewById(R.id.btn_delete_single_photo);
            btnToggleFavoriteInList = itemView.findViewById(R.id.btn_toggle_favorite_in_list);
        }
    }

    public void shutdownExecutor() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}