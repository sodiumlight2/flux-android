package org.nikanikoo.flux.ui.adapters.friends;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.squareup.picasso.Picasso;

import org.nikanikoo.flux.data.models.FriendRequest;
import org.nikanikoo.flux.R;

import java.util.List;

public class FriendRequestsAdapter extends RecyclerView.Adapter<FriendRequestsAdapter.ViewHolder> {

    private List<FriendRequest> friendRequests;
    private Context context;
    private OnRequestActionListener listener;

    public interface OnRequestActionListener {
        void onAcceptRequest(FriendRequest request);
        void onDeclineRequest(FriendRequest request);
        void onRequestClick(FriendRequest request);
    }

    public FriendRequestsAdapter(Context context, List<FriendRequest> friendRequests) {
        this.context = context;
        this.friendRequests = friendRequests;
    }

    public void setOnRequestActionListener(OnRequestActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FriendRequest request = friendRequests.get(position);
        
        holder.friendName.setText(request.getName());
        
        // Загружаем аватар
        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isEmpty()) {
            Picasso.get()
                    .load(request.getAvatarUrl())
                    .placeholder(R.drawable.camera_200)
                    .error(R.drawable.camera_200)
                    .into(holder.friendAvatar);
        } else {
            holder.friendAvatar.setImageResource(R.drawable.camera_200);
        }
        
        // Обработчики кнопок
        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAcceptRequest(request);
            }
        });

        holder.btnDecline.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeclineRequest(request);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRequestClick(request);
            }
        });
    }

    @Override
    public int getItemCount() {
        return friendRequests.size();
    }

    public void updateRequests(List<FriendRequest> newRequests) {
        friendRequests.clear();
        friendRequests.addAll(newRequests);
        notifyDataSetChanged();
    }

    public void removeRequest(FriendRequest request) {
        int position = friendRequests.indexOf(request);
        if (position != -1) {
            friendRequests.remove(position);
            notifyItemRemoved(position);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView friendAvatar;
        TextView friendName;
        TextView friendStatus;
        MaterialButton btnAccept;
        MaterialButton btnDecline;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            friendAvatar = itemView.findViewById(R.id.friend_avatar);
            friendName = itemView.findViewById(R.id.friend_name);
            friendStatus = itemView.findViewById(R.id.friend_status);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnDecline = itemView.findViewById(R.id.btn_decline);
        }
    }
}