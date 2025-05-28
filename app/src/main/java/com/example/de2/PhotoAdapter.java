package com.example.de2;

import android.content.Context;
import android.content.Intent; // Đảm bảo import Intent đúng
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log; // Thêm Log để kiểm tra
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast; // Thêm Toast để thông báo nếu ID không hợp lệ

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // Nên dùng Glide để hiển thị ảnh thumbnail

import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {
    private Context context;
    private List<Photo> photoList;

    public PhotoAdapter(Context context, List<Photo> photoList) {
        this.context = context;
        this.photoList = photoList;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.photo, parent, false); // Đảm bảo R.layout.photo tồn tại và có ImageView R.id.iv_photo
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        Photo currentPhoto = photoList.get(position); // Lấy đối tượng Photo hiện tại

        byte[] imageBytes = currentPhoto.getImage();
        if (imageBytes != null && imageBytes.length > 0) {
            // Sử dụng Glide để hiển thị ảnh thumbnail (hiệu quả hơn)
            Glide.with(context)
                    .load(imageBytes)
                    .placeholder(R.drawable.ic_placeholder_album) // Tạo drawable này
                    .error(R.drawable.ic_placeholder_album)
                    .centerCrop() // Hoặc fitCenter() tùy theo thiết kế item
                    .into(holder.ivPhoto);
        } else {
            holder.ivPhoto.setImageResource(R.drawable.ic_placeholder_album);
        }


        holder.itemView.setOnClickListener(v -> { // Nên setOnClickListener cho itemView thay vì chỉ ivPhoto
            Intent intent = new Intent(context, ViewPhotoActivity.class);

            int photoIdToSend = currentPhoto.getId(); // LẤY ID TỪ ĐỐI TƯỢNG PHOTO
            byte[] imageDataToSend = currentPhoto.getImage(); // LẤY DỮ LIỆU ẢNH

            // THÊM LOG ĐỂ KIỂM TRA GIÁ TRỊ TRƯỚC KHI GỬI
            Log.d("PhotoAdapter", "Chuẩn bị gửi Intent đến ViewPhotoActivity.");
            Log.d("PhotoAdapter", "PHOTO_ID gửi đi: " + photoIdToSend);
            Log.d("PhotoAdapter", "Kích thước PHOTO_IMAGE_DATA gửi đi: " + (imageDataToSend != null ? imageDataToSend.length : "null"));

            if (photoIdToSend <= 0) { // Kiểm tra ID không hợp lệ (ID thường bắt đầu từ 1)
                Log.e("PhotoAdapter", "PHOTO_ID không hợp lệ trước khi gửi: " + photoIdToSend);
                Toast.makeText(context, "Lỗi: ID ảnh không hợp lệ để xem chi tiết.", Toast.LENGTH_SHORT).show();
                return; // Không khởi chạy Activity nếu ID không hợp lệ
            }

            // SỬA KEY CHO ĐÚNG VÀ THÊM PHOTO_ID
            intent.putExtra("PHOTO_ID", photoIdToSend);
            intent.putExtra("PHOTO_IMAGE_DATA", imageDataToSend); // Key phải là "PHOTO_IMAGE_DATA"

            context.startActivity(intent);
        });
    }


    @Override
    public int getItemCount() {
        return photoList != null ? photoList.size() : 0; // Thêm kiểm tra null
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.iv_photo); // Đảm bảo ID này đúng trong R.layout.photo
        }
    }
}