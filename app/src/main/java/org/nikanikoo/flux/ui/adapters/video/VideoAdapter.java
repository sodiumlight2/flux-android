package org.nikanikoo.flux.ui.adapters.video;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.nikanikoo.flux.R;
import org.nikanikoo.flux.data.models.Video;

import java.util.Locale;

import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private List<Video> videos;
    private OnVideoClickListener listener;

    public interface OnVideoClickListener {
        void onVideoClick(Video video, int position);
    }

    public VideoAdapter(List<Video> videos, OnVideoClickListener listener) {
        this.videos = videos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_video, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        Video video = videos.get(position);
        holder.bind(video, position);
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView title;
        TextView duration;
        ImageView playIcon;

        VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.video_thumbnail);
            title = itemView.findViewById(R.id.video_title);
            duration = itemView.findViewById(R.id.video_duration);
            playIcon = itemView.findViewById(R.id.video_play_icon);
        }

        void bind(Video video, int position) {
            title.setText(video.getTitle());
            duration.setText(video.getFormattedDuration());

            // Load thumbnail
            String imageUrl = video.getImage();
            if (imageUrl == null || imageUrl.isEmpty()) {
                imageUrl = video.getFirstFrame();
            }
            
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Picasso.get()
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_video_placeholder)
                        .error(R.drawable.ic_video_placeholder)
                        .into(thumbnail);
            } else {
                thumbnail.setImageResource(R.drawable.ic_video_placeholder);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onVideoClick(video, position);
                }
            });
        }
    }
}
