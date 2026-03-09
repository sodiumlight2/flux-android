package org.nikanikoo.flux.ui.activities;

import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.nikanikoo.flux.R;
import org.nikanikoo.flux.data.models.Post;
import org.nikanikoo.flux.ui.fragments.comments.CommentsFragment;
import org.nikanikoo.flux.utils.LocaleManager;
import org.nikanikoo.flux.utils.ThemeManager;

public class CommentsActivity extends AppCompatActivity {

    public static final String EXTRA_POST = "post";

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
        setContentView(R.layout.activity_comments);
        
        ThemeManager.applySystemBarsAppearance(this);
        
        Post post = (Post) getIntent().getSerializableExtra(EXTRA_POST);
        
        setupToolbar();
        
        if (savedInstanceState == null && post != null) {
            CommentsFragment commentsFragment = CommentsFragment.newInstance(post);
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.comments_container, commentsFragment)
                .commit();
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(getString(R.string.comments_title));
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
