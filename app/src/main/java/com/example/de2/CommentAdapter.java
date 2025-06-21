package com.example.de2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat; // Import để định dạng ngày giờ (tùy chọn)
import java.util.Date;           // Import để làm việc với ngày giờ (tùy chọn)
import java.util.List;
import java.util.Locale;         // Import để định dạng ngày giờ theo Locale (tùy chọn)


public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private Context context;
    private List<Comment> commentList; // Sửa: Bỏ dấu chấm phẩy thừa và đảm bảo kiểu là List<Comment>
    private SimpleDateFormat dateFormat; // Tùy chọn: để định dạng ngày giờ


    public CommentAdapter(Context context, List<Comment> commentList) {
        this.context = context;
        this.commentList = commentList;

    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Đảm bảo R.layout.item_comment_simple là layout cho mỗi item bình luận
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment_simple, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        // SỬA Ở ĐÂY: Lấy đối tượng Comment từ danh sách
        Comment comment = commentList.get(position);

        // Hiển thị thông tin từ đối tượng Comment.
        // Ví dụ: hiển thị tên tác giả và nội dung bình luận.
        // Bạn cần đảm bảo class Comment có các phương thức getAuthor() và getText().
        String author = comment.getAuthor() != null ? comment.getAuthor() : "Ẩn danh";
        String text = comment.getText() != null ? comment.getText() : "";

        holder.tvCommentText.setText(author + ": " + text);


    }

    @Override
    public int getItemCount() {
        return commentList != null ? commentList.size() : 0;
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvCommentText;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            // Đảm bảo ID này khớp với ID trong file item_comment_simple.xml
            tvCommentText = itemView.findViewById(R.id.tvCommentText);

        }
    }
}