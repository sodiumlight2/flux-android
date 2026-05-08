package org.nikanikoo.flux.ui.fragments.profile.edit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import org.nikanikoo.flux.R;

public class ProfileEditMainFragment extends Fragment {

    private MaterialButton saveButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_edit_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupClickListeners();
    }

    private void initViews(View view) {
        saveButton = view.findViewById(R.id.save_button);
    }

    private void setupClickListeners() {
        saveButton.setOnClickListener(v -> {
            // TODO: сохранить изменения профиля
            Toast.makeText(requireContext(), "Изменения сохранены", Toast.LENGTH_SHORT).show();
        });
    }
}