package org.nikanikoo.flux.ui.fragments.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.nikanikoo.flux.R;
import org.nikanikoo.flux.utils.BuildInfo;

public class AboutAppFragment extends Fragment {

    private View itemGithub;
    private TextView versionText;
    private TextView commitText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about_app, container, false);

        initViews(view);
        setupClickListeners();
        updateVersionInfo();

        return view;
    }

    private void initViews(View view) {
        itemGithub = view.findViewById(R.id.item_github);
        versionText = view.findViewById(R.id.version_text);
        commitText = view.findViewById(R.id.commit_text);
    }

    private void updateVersionInfo() {
        if (getActivity() != null) {
            String version = BuildInfo.getAppVersion(getActivity());
            String commitHash = BuildInfo.getCommitHash();
            
            if (versionText != null) {
                versionText.setText(getString(R.string.about_app_version) + version);
            }
            
            if (commitText != null) {
                commitText.setText(getString(R.string.about_app_commit) + commitHash);
            }
        }
    }

    private void setupClickListeners() {
        itemGithub.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://github.com/nikanikoo/flux-android"));
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(requireContext(), getString(R.string.link_error), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
