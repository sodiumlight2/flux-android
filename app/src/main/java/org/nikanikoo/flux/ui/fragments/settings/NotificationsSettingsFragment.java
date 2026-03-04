package org.nikanikoo.flux.ui.fragments.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

import org.nikanikoo.flux.R;

public class NotificationsSettingsFragment extends Fragment {

    private SwitchMaterial switchNotificationsMessages;
    private SwitchMaterial switchNotificationsLikes;
    private SwitchMaterial switchNotificationsComments;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications_settings, container, false);
        
        initViews(view);
        setupSwitches();
        
        return view;
    }
    
    private void initViews(View view) {
        switchNotificationsMessages = view.findViewById(R.id.switch_notifications_messages);
        switchNotificationsLikes = view.findViewById(R.id.switch_notifications_likes);
        switchNotificationsComments = view.findViewById(R.id.switch_notifications_comments);
    }
    
    private void setupSwitches() {
        switchNotificationsMessages.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(requireContext(), getString(R.string.notification_enabled) + (isChecked ? getString(R.string.notification_on) : getString(R.string.notification_off)), Toast.LENGTH_SHORT).show();
        });
        
        switchNotificationsLikes.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(requireContext(), getString(R.string.notification_likes_enabled) + (isChecked ? getString(R.string.notification_on) : getString(R.string.notification_off)), Toast.LENGTH_SHORT).show();
        });
        
        switchNotificationsComments.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(requireContext(), getString(R.string.notification_comments_enabled) + (isChecked ? getString(R.string.notification_on) : getString(R.string.notification_off)), Toast.LENGTH_SHORT).show();
        });
    }
}
