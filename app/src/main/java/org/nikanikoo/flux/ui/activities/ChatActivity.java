package org.nikanikoo.flux.ui.activities;

import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.nikanikoo.flux.R;
import org.nikanikoo.flux.ui.fragments.messages.ChatFragment;
import org.nikanikoo.flux.utils.LocaleManager;
import org.nikanikoo.flux.utils.ThemeManager;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_PEER_ID = "peer_id";
    public static final String EXTRA_PEER_NAME = "peer_name";
    public static final String EXTRA_FROM_ID = "from_id";

    @Override
    protected void attachBaseContext(Context newBase) {
        LocaleManager localeManager = LocaleManager.getInstance(newBase);
        Context context = localeManager.updateContext(newBase);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager themeManager = ThemeManager.getInstance(this);
        themeManager.applySavedTheme();
        themeManager.applyThemeToActivity(this);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        
        ThemeManager.applySystemBarsAppearance(this);
        
        int peerId = getIntent().getIntExtra(EXTRA_PEER_ID, 0);
        String peerName = getIntent().getStringExtra(EXTRA_PEER_NAME);
        int fromId = getIntent().getIntExtra(EXTRA_FROM_ID, peerId);
        
        setupToolbar(peerName);
        
        if (savedInstanceState == null) {
            ChatFragment chatFragment = ChatFragment.newInstance(peerId, peerName, fromId);
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.chat_container, chatFragment)
                .commit();
        }
    }

    private void setupToolbar(String title) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(title != null ? title : getString(R.string.chat_title));
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
