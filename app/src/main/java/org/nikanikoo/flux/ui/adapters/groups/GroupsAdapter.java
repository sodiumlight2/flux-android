package org.nikanikoo.flux.ui.adapters.groups;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.nikanikoo.flux.Constants;
import org.nikanikoo.flux.data.models.Group;
import org.nikanikoo.flux.R;
import org.nikanikoo.flux.utils.Logger;

import java.util.List;

public class GroupsAdapter extends RecyclerView.Adapter<GroupsAdapter.GroupViewHolder> {
    private static final String TAG = "GroupsAdapter";
    private List<Group> groups;
    private Context context;
    private OnGroupClickListener listener;

    public interface OnGroupClickListener {
        void onGroupClick(Group group);
    }

    public GroupsAdapter(Context context, List<Group> groups) {
        this.context = context;
        this.groups = groups;
    }

    public void setOnGroupClickListener(OnGroupClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        Group group = groups.get(position);
        
        // Название группы
        holder.groupName.setText(group.getName());
        
        // Количество участников
        holder.groupMembersCount.setText(group.getMembersCountText());
        
        // Индикатор верификации
        Logger.d(TAG, "Group " + group.getName() + " verified: " + group.isVerified());
        holder.verifiedIndicator.setVisibility(group.isVerified() ? View.VISIBLE : View.GONE);
        
        // Аватар группы
        if (group.getPhoto50() != null && !group.getPhoto50().isEmpty()) {
            Picasso.get()
                    .load(group.getPhoto50())
                    .placeholder(R.drawable.camera_200)
                    .error(R.drawable.camera_200)
                    .resize(Constants.UI.THUMBNAIL_SIZE, Constants.UI.THUMBNAIL_SIZE)
                    .centerCrop()
                    .into(holder.groupAvatar);
        } else {
            holder.groupAvatar.setImageResource(R.drawable.camera_200);
        }
        
        // Обработчики кликов
        View.OnClickListener groupClickListener = v -> {
            if (listener != null) {
                listener.onGroupClick(group);
            }
        };
        
        holder.itemView.setOnClickListener(groupClickListener);
        holder.groupAvatar.setOnClickListener(groupClickListener);
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    public void updateGroups(List<Group> newGroups) {
        this.groups.clear();
        this.groups.addAll(newGroups);
        notifyDataSetChanged();
    }

    public void addGroups(List<Group> newGroups) {
        int startPosition = this.groups.size();
        this.groups.addAll(newGroups);
        notifyItemRangeInserted(startPosition, newGroups.size());
    }

    public void clearGroups() {
        this.groups.clear();
        notifyDataSetChanged();
    }

    // Метод для освобождения ресурсов
    public void onDestroy() {
        if (groups != null) {
            groups.clear();
        }
        listener = null;
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        ImageView groupAvatar;
        TextView groupName;
        TextView groupMembersCount;
        ImageView verifiedIndicator;

        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            groupAvatar = itemView.findViewById(R.id.group_avatar);
            groupName = itemView.findViewById(R.id.group_name);
            groupMembersCount = itemView.findViewById(R.id.group_members_count);
            verifiedIndicator = itemView.findViewById(R.id.verified_indicator);
        }
    }
}