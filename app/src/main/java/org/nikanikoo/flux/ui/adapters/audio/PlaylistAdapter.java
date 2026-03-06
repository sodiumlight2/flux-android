package org.nikanikoo.flux.ui.adapters.audio;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.nikanikoo.flux.R;
import org.nikanikoo.flux.data.models.Audio;

import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {

    private List<Audio> playlist;
    private int currentPosition;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public PlaylistAdapter(List<Audio> playlist, int currentPosition, OnItemClickListener listener) {
        this.playlist = playlist;
        this.currentPosition = currentPosition;
        this.listener = listener;
    }

    public void updatePlaylist(List<Audio> newPlaylist, int currentPosition) {
        this.playlist = newPlaylist;
        this.currentPosition = currentPosition;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_playlist_track, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Audio audio = playlist.get(position);
        holder.bind(audio, position == currentPosition);
    }

    @Override
    public int getItemCount() {
        return playlist.size();
    }

    public void updateCurrentPosition(int position) {
        int oldPosition = currentPosition;
        currentPosition = position;
        notifyItemChanged(oldPosition);
        notifyItemChanged(currentPosition);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView artist;
        TextView title;
        TextView duration;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            artist = itemView.findViewById(R.id.playlist_track_artist);
            title = itemView.findViewById(R.id.playlist_track_title);
            duration = itemView.findViewById(R.id.playlist_track_duration);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(getAdapterPosition());
                }
            });
        }

        void bind(Audio audio, boolean isCurrent) {
            artist.setText(audio.getArtist());
            title.setText(audio.getTitle());
            duration.setText(audio.getFormattedDuration());

            itemView.setSelected(isCurrent);
        }
    }
}
