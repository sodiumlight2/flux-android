package org.nikanikoo.flux.ui.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import org.nikanikoo.flux.Constants;
import org.nikanikoo.flux.ui.fragments.media.PhotoViewerPageFragment;
import org.nikanikoo.flux.data.models.Post;
import org.nikanikoo.flux.data.managers.PostsManager;
import org.nikanikoo.flux.R;
import org.nikanikoo.flux.ui.custom.SwipeToCloseHelper;
import org.nikanikoo.flux.utils.Logger;
import java.util.ArrayList;
import java.util.List;

public class PhotoViewerActivity extends AppCompatActivity {
    
    private static final String TAG = "PhotoViewerActivity";
    private static final String EXTRA_IMAGE_URLS = "image_urls";
    private static final String EXTRA_CURRENT_POSITION = "current_position";
    private static final String EXTRA_POST = "post";
    private static final String EXTRA_AUTHOR_NAME = "author_name";
    
    private static final int UI_HIDE_DELAY = 3000;
    private static final float DISMISS_THRESHOLD = 0.3f;
    
    private ViewPager2 viewPager;
    private View rootContainer;
    private View topPanel;
    private View bottomPanel;
    private TextView titleText;
    private ImageView btnBack;
    private ImageView btnLike;
    private ImageView btnComments;
    private ImageView btnShare;
    
    private List<String> imageUrls;
    private int currentPosition;
    private Post post;
    private String authorName;
    
    private boolean isUIVisible = true;
    private Handler uiHandler;
    private Runnable hideUIRunnable;
    
    private GestureDetector gestureDetector;
    private SwipeToCloseHelper swipeHelper;
    private boolean isDragging = false;
    private float initialY = 0f;
    private float currentTranslationY = 0f;
    
    private final OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            animateExit();
        }
    };
    
    public static void start(Context context, List<String> imageUrls, int position, Post post, String authorName) {
        Intent intent = new Intent(context, PhotoViewerActivity.class);
        intent.putStringArrayListExtra(EXTRA_IMAGE_URLS, new ArrayList<>(imageUrls));
        intent.putExtra(EXTRA_CURRENT_POSITION, position);
        intent.putExtra(EXTRA_POST, post);
        intent.putExtra(EXTRA_AUTHOR_NAME, authorName);
        context.startActivity(intent);
    }
    
    public static void start(Context context, List<String> imageUrls, int position, String title) {
        start(context, imageUrls, position, null, title);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupTransparentWindow();
        
        setContentView(R.layout.activity_photo_viewer);
        
        extractIntentData();
        initViews();
        setupGestureDetector();
        setupViewPager();
        setupUI();

        animateEnter();
        
        getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
        
        Logger.d(TAG, "PhotoViewerActivity created with " + (imageUrls != null ? imageUrls.size() : 0) + " images");
    }

    private void setupTransparentWindow() {
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        );

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }
    
    private void extractIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            imageUrls = intent.getStringArrayListExtra(EXTRA_IMAGE_URLS);
            currentPosition = intent.getIntExtra(EXTRA_CURRENT_POSITION, 0);
            post = (Post) intent.getSerializableExtra(EXTRA_POST);
            authorName = intent.getStringExtra(EXTRA_AUTHOR_NAME);
        }
    }
    
    private void initViews() {
        rootContainer = findViewById(R.id.rootContainer);
        viewPager = findViewById(R.id.viewPager);
        topPanel = findViewById(R.id.topPanel);
        bottomPanel = findViewById(R.id.bottomPanel);
        titleText = findViewById(R.id.titleText);
        btnBack = findViewById(R.id.btnBack);
        btnLike = findViewById(R.id.btnLike);
        btnComments = findViewById(R.id.btnComments);
        btnShare = findViewById(R.id.btnShare);
        
        uiHandler = new Handler(Looper.getMainLooper());
        hideUIRunnable = this::hideUI;
    }
    
    private void setupGestureDetector() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                toggleUI();
                return true;
            }
        });

        swipeHelper = new SwipeToCloseHelper(rootContainer, new SwipeToCloseHelper.OnSwipeListener() {
            @Override
            public void onSwipeStart() {
                cancelUIHide();
                viewPager.setUserInputEnabled(false);
            }
            
            @Override
            public void onSwipeProgress(float progress, float translationY) {
                currentTranslationY = translationY;
                rootContainer.setTranslationY(currentTranslationY);

                float alpha = 1f - progress * 0.7f;
                alpha = Math.max(0.3f, Math.min(1f, alpha));
                rootContainer.setAlpha(alpha);
            }
            
            @Override
            public void onSwipeEnd(boolean shouldClose) {
                viewPager.setUserInputEnabled(true);
                
                if (shouldClose) {
                    animateExit();
                } else {
                    animateReturn();
                }
            }
        });

        rootContainer.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return swipeHelper.onTouchEvent(event, rootContainer);
        });
    }

    public void toggleUI() {
        if (isUIVisible) {
            hideUI();
        } else {
            showUI();
        }
    }

    public void setSwipeToCloseEnabled(boolean enabled) {
        if (swipeHelper != null) {
            swipeHelper.setCanStartSwipe(enabled);
        }
    }
    
    private void handleDragEnd() {
        float dismissDistance = getWindow().getDecorView().getHeight() * DISMISS_THRESHOLD;
        
        if (Math.abs(currentTranslationY) > dismissDistance) {
            animateExit();
        } else {
            animateReturn();
        }
    }
    
    private void animateReturn() {
        ValueAnimator animator = ValueAnimator.ofFloat(currentTranslationY, 0f);
        animator.setDuration(Constants.UI.ANIMATION_DURATION_SHORT);
        animator.addUpdateListener(animation -> {
            float value = (Float) animation.getAnimatedValue();
            rootContainer.setTranslationY(value);
            
            float alpha = 1f - Math.abs(value) / (getWindow().getDecorView().getHeight() * 0.5f);
            alpha = Math.max(0.3f, Math.min(1f, alpha));
            rootContainer.setAlpha(alpha);
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                currentTranslationY = 0f;
                scheduleUIHide();
            }
        });
        animator.start();
    }
    
    private void setupViewPager() {
        PhotoPagerAdapter adapter = new PhotoPagerAdapter(this, imageUrls);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(currentPosition, false);
        
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentPosition = position;
                updateTitle();
                showUI();
            }
        });
        
        updateTitle();
    }
    
    private void setupUI() {
        titleText.setText(authorName != null ? authorName : getString(R.string.photo_viewer));
        
        btnBack.setOnClickListener(v -> animateExit());
        
        btnLike.setOnClickListener(v -> {
            if (post != null) {
                toggleLike();
            }
            showUI();
        });
        
        btnComments.setOnClickListener(v -> {
            if (post != null) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("open_comments", true);
                intent.putExtra("post", post);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });
        
        btnShare.setOnClickListener(v -> {
            Toast.makeText(this, getString(R.string.share), Toast.LENGTH_SHORT).show();
            showUI();
        });
        
        updateLikeButton();
        scheduleUIHide();

        if (swipeHelper != null) {
            swipeHelper.setCanStartSwipe(true);
        }
    }
    
    private void updateTitle() {
        if (imageUrls != null && imageUrls.size() > 1) {
            String title = (authorName != null ? authorName : getString(R.string.photo_viewer)) +
                          " (" + (currentPosition + 1) + "/" + imageUrls.size() + ")";
            titleText.setText(title);
        }
    }
    
    private void hideUI() {
        if (!isUIVisible) return;
        
        isUIVisible = false;
        
        topPanel.animate()
                .translationY(-topPanel.getHeight())
                .setDuration(Constants.UI.ANIMATION_DURATION_MEDIUM)
                .start();
        
        bottomPanel.animate()
                .translationY(bottomPanel.getHeight())
                .setDuration(Constants.UI.ANIMATION_DURATION_MEDIUM)
                .start();
        
        cancelUIHide();
        Logger.d(TAG, "UI hidden");
    }
    
    private void showUI() {
        if (isUIVisible) return;
        
        isUIVisible = true;
        
        topPanel.animate()
                .translationY(0)
                .setDuration(Constants.UI.ANIMATION_DURATION_MEDIUM)
                .start();
        
        bottomPanel.animate()
                .translationY(0)
                .setDuration(Constants.UI.ANIMATION_DURATION_MEDIUM)
                .start();
        
        scheduleUIHide();
        Logger.d(TAG, "UI shown");
    }
    
    private void scheduleUIHide() {
        cancelUIHide();
        if (uiHandler != null && hideUIRunnable != null) {
            uiHandler.postDelayed(hideUIRunnable, UI_HIDE_DELAY);
        }
    }
    
    private void cancelUIHide() {
        if (uiHandler != null && hideUIRunnable != null) {
            uiHandler.removeCallbacks(hideUIRunnable);
        }
    }
    
    private void animateEnter() {
        rootContainer.setAlpha(0f);
        rootContainer.setScaleX(0.9f);
        rootContainer.setScaleY(0.9f);
        
        rootContainer.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(Constants.UI.ANIMATION_DURATION_SHORT)
                .start();
    }
    
    private void animateExit() {
        cancelUIHide();
        
        rootContainer.animate()
                .alpha(0f)
                .scaleX(0.9f)
                .scaleY(0.9f)
                .translationY(currentTranslationY > 0 ? Constants.UI.ANIMATION_DURATION_MEDIUM : -Constants.UI.ANIMATION_DURATION_MEDIUM)
                .setDuration(Constants.UI.ANIMATION_DURATION_SHORT)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        finish();
                        overridePendingTransition(0, 0);
                    }
                })
                .start();
    }
    
    private void toggleLike() {
        if (post == null) return;
        
        final boolean originalLikedState = post.isLiked();
        final int originalLikeCount = post.getLikeCount();
        
        final boolean newLikedState = !originalLikedState;
        final int newLikeCount = originalLikedState ? originalLikeCount - 1 : originalLikeCount + 1;
        
        post.setLiked(newLikedState);
        post.setLikeCount(newLikeCount);
        updateLikeButton();
        
        PostsManager postsManager = PostsManager.getInstance(this);
        postsManager.toggleLikeOptimistic(post, originalLikedState, new PostsManager.LikeToggleCallback() {
            @Override
            public void onSuccess(int serverLikesCount, boolean serverIsLiked) {
                runOnUiThread(() -> {
                    post.setLikeCount(serverLikesCount);
                    post.setLiked(serverIsLiked);
                    updateLikeButton();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    post.setLiked(originalLikedState);
                    post.setLikeCount(originalLikeCount);
                    updateLikeButton();
                    Toast.makeText(PhotoViewerActivity.this, getString(R.string.error_loading) + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void updateLikeButton() {
        if (post != null) {
            btnLike.setImageResource(post.isLiked() ? R.drawable.ic_like_filled : R.drawable.ic_like);

            int color = post.isLiked() ? 
                androidx.core.content.ContextCompat.getColor(this, R.color.like_color) : 
                androidx.core.content.ContextCompat.getColor(this, R.color.icon_color);
            btnLike.setColorFilter(color);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelUIHide();
        if (uiHandler != null) {
            uiHandler.removeCallbacksAndMessages(null);
        }
        Logger.d(TAG, "PhotoViewerActivity destroyed");
    }

    private static class PhotoPagerAdapter extends FragmentStateAdapter {
        private final List<String> imageUrls;
        
        public PhotoPagerAdapter(@NonNull AppCompatActivity activity, List<String> imageUrls) {
            super(activity);
            this.imageUrls = imageUrls;
        }
        
        @NonNull
        @Override
        public PhotoViewerPageFragment createFragment(int position) {
            return PhotoViewerPageFragment.newInstance(imageUrls.get(position));
        }
        
        @Override
        public int getItemCount() {
            return imageUrls != null ? imageUrls.size() : 0;
        }
    }
}