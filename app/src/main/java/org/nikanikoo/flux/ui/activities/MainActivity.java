package org.nikanikoo.flux.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.navigation.NavigationView;

import org.nikanikoo.flux.R;
import org.nikanikoo.flux.data.managers.NotificationsManager;
import org.nikanikoo.flux.data.managers.ProfileManager;
import org.nikanikoo.flux.data.managers.api.OpenVKApi;
import org.nikanikoo.flux.data.models.Post;
import org.nikanikoo.flux.data.models.UserProfile;
import org.nikanikoo.flux.security.AccountManager;
import org.nikanikoo.flux.services.LongPollManager;
import org.nikanikoo.flux.services.LongPollService;
import org.nikanikoo.flux.services.MessageNotificationManager;
import org.nikanikoo.flux.ui.custom.CustomDrawerLayout;
import org.nikanikoo.flux.ui.custom.NotificationBadgeListener;
import org.nikanikoo.flux.ui.fragments.messages.ChatFragment;
import org.nikanikoo.flux.ui.fragments.messages.MessagesListFragment;
import org.nikanikoo.flux.ui.fragments.news.NewsFragment;
import org.nikanikoo.flux.utils.Logger;
import org.nikanikoo.flux.utils.LocaleManager;
import org.nikanikoo.flux.utils.ThemeManager;
import org.nikanikoo.flux.utils.ValidationUtils;

/**
 * Главная Activity приложения.
 * Использует NavigationController для управления навигацией и MiniPlayerController для плеера.
 */
public class MainActivity extends AppCompatActivity implements NotificationBadgeListener {

    private static final String TAG = "MainActivity";

    // Controllers
    private NavigationController navigationController;
    private MiniPlayerController miniPlayerController;

    // Managers
    private ProfileManager profileManager;
    private NotificationsManager notificationsManager;
    private LongPollManager longPollManager;
    private AccountManager accountManager;
    private LocaleManager localeManager;

    private final OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            Logger.d(TAG, "onBackPressed handled by callback, backStackCount=" +
                getSupportFragmentManager().getBackStackEntryCount());

            if (navigationController != null && navigationController.handleBackPress()) {
                Logger.d(TAG, "Drawer was open, closed it");
                return;
            }

            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                Logger.d(TAG, "Popping back stack");
                getSupportFragmentManager().popBackStack();
            } else {
                Logger.d(TAG, "Finishing activity");
                finish();
            }
        }
    };

    @Override
    protected void attachBaseContext(Context newBase) {
        localeManager = LocaleManager.getInstance(newBase);
        Context context = localeManager.updateContext(newBase);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        
        if (!checkAuthentication()) {
            return;
        }
        
        setContentView(R.layout.activity_main);
        
        initializeManagers();
        setupControllers(); // Setup controllers BEFORE toolbar (navigationController needed)
        setupToolbar();
        setupLongPoll();
        loadUserProfile();
        
        handleNotificationIntent(getIntent());
        
        if (savedInstanceState == null) {
            setupInitialFragment();
        }
        
        getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
        
        ThemeManager.applySystemBarsAppearance(this);
        
        Logger.d(TAG, "onCreate completed");
    }
    
    /**
     * Применение темы до создания View
     */
    private void applyTheme() {
        ThemeManager themeManager = ThemeManager.getInstance(this);
        themeManager.applySavedTheme();
        themeManager.applyThemeToActivity(this);
        
        if (themeManager.getThemeStyle() == ThemeManager.STYLE_MATERIAL_YOU && 
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            DynamicColors.applyToActivityIfAvailable(this);
        }
    }
    
    /**
     * Проверка аутентификации пользователя
     */
    private boolean checkAuthentication() {
        OpenVKApi.resetInstance();
        OpenVKApi api = OpenVKApi.getInstance(this);
        
        if (api.getToken() == null) {
            Logger.d(TAG, "Токен отсутствует, переход в авторизацию");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return false;
        }
        
        return true;
    }
    
    /**
     * Инициализация менеджеров
     */
    private void initializeManagers() {
        profileManager = ProfileManager.getInstance(this);
        notificationsManager = NotificationsManager.getInstance(this);
        longPollManager = LongPollManager.getInstance(this);
        accountManager = AccountManager.getInstance(this);
    }
    
    /**
     * Настройка Toolbar
     */
    private void setupToolbar() {
        // Toolbar уже настроен в NavigationController
    }
    
    /**
     * Настройка контроллеров
     */
    private void setupControllers() {
        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Navigation Controller
        CustomDrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.drawer_view);
        navigationController = new NavigationController(this, drawerLayout, navigationView, toolbar);
        
        // Mini Player Controller
        miniPlayerController = new MiniPlayerController(this);
        miniPlayerController.initViews(findViewById(R.id.main_mini_player));
        miniPlayerController.setOnPlayerStateChangeListener(
                new MiniPlayerController.OnPlayerStateChangeListener() {
            @Override
            public void onPlayerConnected() {
                Logger.d(TAG, "Player connected");
            }
            
            @Override
            public void onPlayerDisconnected() {
                Logger.d(TAG, "Player disconnected");
            }
            
            @Override
            public void onTrackChanged(org.nikanikoo.flux.data.models.Audio audio) {
                Logger.d(TAG, "Track changed: " + audio.getFullTitle());
            }
        });
        
        // Setup accounts in navigation
        setupAccountSwitcher();
    }
    
    /**
     * Настройка переключателя аккаунтов
     */
    private void setupAccountSwitcher() {
        // Account switching logic is now in NavigationController
    }
    
    /**
     * Начальный фрагмент
     */
    private void setupInitialFragment() {
        String openFragment = getIntent().getStringExtra("open_fragment");
        
        if ("appearance_settings".equals(openFragment)) {
            navigationController.navigateToFragment(
                    new org.nikanikoo.flux.ui.fragments.settings.AppearanceSettingsFragment(),
                    "appearance_settings");
            navigationController.setCurrentFragmentId(R.id.drawer_settings);
        } else if ("settings".equals(openFragment)) {
            navigationController.navigateToFragment(
                    new org.nikanikoo.flux.ui.fragments.settings.SettingsFragment(),
                    "settings");
            navigationController.setCurrentFragmentId(R.id.drawer_settings);
        } else {
            navigationController.navigateToFragment(new NewsFragment(), "news");
            navigationController.setCurrentFragmentId(R.id.drawer_news);
        }
        
        // Add listener for back stack changes
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            int backStackCount = getSupportFragmentManager().getBackStackEntryCount();
            Logger.d(TAG, "BackStack changed, count=" + backStackCount);
            
            // Логируем содержимое back stack
            for (int i = 0; i < backStackCount; i++) {
                Logger.d(TAG, "  BackStack[" + i + "]: " + getSupportFragmentManager().getBackStackEntryAt(i).getName());
            }
            
            navigationController.updateDrawerToggleForBackStack(backStackCount);
        });
    }
    
    /**
     * Настройка LongPoll
     */
    private void setupLongPoll() {
        longPollManager.addMessageEventListener(new LongPollManager.OnMessageEventListener() {
            @Override
            public void onNewMessage(int messageId, int peerId, long timestamp, 
                                     String text, int fromId, boolean isOut) {
                if (!isOut) {
                    MessageNotificationManager.getInstance(MainActivity.this)
                            .showMessageNotification(messageId, fromId, peerId, text, timestamp);
                }
                updateMessagesListIfVisible();
                onNotificationBadgeUpdate();
            }

            @Override
            public void onMessageRead(int peerId, int localId) {
                updateChatIfVisible(peerId);
                onNotificationBadgeUpdate();
            }

            @Override
            public void onMessageEdit(int messageId, int peerId, String newText) {
                updateChatIfVisible(peerId);
            }
        });
        
        longPollManager.setOnlineEventListener((userId, isOnline) -> {
            updateUserOnlineStatus(userId, isOnline);
        });

        LongPollService.start(this);
    }
    
    /**
     * Загрузка профиля пользователя для Drawer
     */
    private void loadUserProfile() {
        android.util.Log.d(TAG, "Loading user profile for drawer...");
        
        // Получаем текущий аккаунт из AccountManager
        AccountManager accountManager = AccountManager.getInstance(this);
        AccountManager.Account currentAccount = accountManager.getCurrentAccount();
        
        if (currentAccount != null) {
            android.util.Log.d(TAG, "Current account: " + currentAccount.fullName + " (id: " + currentAccount.userId + ")");
        } else {
            android.util.Log.w(TAG, "No current account found!");
        }
        
        profileManager.loadProfile(false, new ProfileManager.ProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                runOnUiThread(() -> {
                    android.util.Log.d(TAG, "Profile loaded: " + profile.getFullName() + " (id: " + profile.getId() + ")");
                    if (navigationController != null) {
                        navigationController.updateUserInfo(profile);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Logger.e(TAG, "Error loading profile: " + error);
            }
        });
    }
    
    /**
     * Обработка Intent от уведомлений
     */
    private void handleNotificationIntent(Intent intent) {
        if (intent == null) return;
        
        if (intent.getBooleanExtra("open_chat", false)) {
            int peerId = intent.getIntExtra("peer_id", 0);
            String peerName = intent.getStringExtra("peer_name");
            int fromId = intent.getIntExtra("from_id", peerId);
            
            if (ValidationUtils.isValidUserId(peerId) && peerName != null) {
                Intent chatIntent = new Intent(this, ChatActivity.class);
                chatIntent.putExtra(ChatActivity.EXTRA_PEER_ID, peerId);
                chatIntent.putExtra(ChatActivity.EXTRA_PEER_NAME, peerName);
                chatIntent.putExtra(ChatActivity.EXTRA_FROM_ID, fromId);
                startActivity(chatIntent);
                
                MessageNotificationManager.getInstance(this).cancelNotification(fromId);
            }
        } else if (intent.getBooleanExtra("open_comments", false)) {
            Post post = (Post) intent.getSerializableExtra("post");
            if (post != null) {
                Intent commentsIntent = new Intent(this, CommentsActivity.class);
                commentsIntent.putExtra(CommentsActivity.EXTRA_POST, post);
                startActivity(commentsIntent);
            }
        }
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleNotificationIntent(intent);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (longPollManager != null) {
            longPollManager.start();
        }
        updateAllBadges();
        
        if (miniPlayerController != null) {
            miniPlayerController.bindService();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            cleanupResources();
        }
    }
    
    private void cleanupResources() {
        if (longPollManager != null) {
            longPollManager.clearAllListeners();
        }
        if (miniPlayerController != null) {
            miniPlayerController.unbindService();
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Logger.d(TAG, "onOptionsItemSelected: itemId=" + item.getItemId() + ", homeId=" + android.R.id.home);
        
        // Если это home button и есть back stack - сначала обрабатываем навигацию
        if (item.getItemId() == android.R.id.home) {
            int backStackCount = getSupportFragmentManager().getBackStackEntryCount();
            Logger.d(TAG, "Home button pressed, backStackCount=" + backStackCount);
            
            if (backStackCount > 0) {
                Logger.d(TAG, "Popping back stack");
                getSupportFragmentManager().popBackStack();
                return true;
            }
        }
        
        // Затем проверяем drawer toggle (только если нет back stack)
        if (navigationController != null && navigationController.onOptionsItemSelected(item)) {
            Logger.d(TAG, "Handled by drawer toggle");
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Установить заголовок Toolbar
     */
    public void setToolbarTitle(String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }
    
    public void setToolbarTitleClickable(String title, View.OnClickListener clickListener) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                toolbar.setOnClickListener(clickListener);
            }
        }
    }
    
    /**
     * Показать диалог выбора темы
     */
    public void showThemeDialog() {
        ThemeManager themeManager = ThemeManager.getInstance(this);
        int currentTheme = themeManager.getThemeMode();
        
        String[] themes = {getString(R.string.appearance_theme_light), getString(R.string.appearance_theme_dark), getString(R.string.appearance_theme_system)};
        
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.appearance_select_theme))
                .setSingleChoiceItems(themes, currentTheme, (dialog, which) -> {
                    themeManager.setThemeMode(which);
                    dialog.dismiss();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }
    
    /**
     * Обновить все бейджи уведомлений
     */
    public void updateAllBadges() {
        OpenVKApi api = OpenVKApi.getInstance(this);
        api.getCounters(new OpenVKApi.CountersCallback() {
            @Override
            public void onSuccess(int messages, int notifications, int friends) {
                runOnUiThread(() -> {
                    NavigationView navigationView = findViewById(R.id.drawer_view);
                    if (navigationView != null) {
                        updateBadge(navigationView, R.id.drawer_notification, notifications, getString(R.string.notifications_title));
                        updateBadge(navigationView, R.id.drawer_messages, messages, getString(R.string.messages_title));
                        updateBadge(navigationView, R.id.drawer_friends, friends, getString(R.string.friends_title));
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                Logger.e(TAG, "Error updating counters: " + error);
            }
        });
    }
    
    private void updateBadge(NavigationView navigationView, int itemId, int count, String defaultTitle) {
        MenuItem item = navigationView.getMenu().findItem(itemId);
        if (item != null) {
            item.setTitle(count > 0 ? defaultTitle + " (" + count + ")" : defaultTitle);
        }
    }
    
    @Override
    public void onNotificationBadgeUpdate() {
        updateAllBadges();
    }
    
    // ==================== Helper Methods ====================
    
    private void updateMessagesListIfVisible() {
        androidx.fragment.app.Fragment currentFragment = 
                getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof MessagesListFragment) {
            ((MessagesListFragment) currentFragment).refreshConversations();
        }
    }
    
    private void updateChatIfVisible(int peerId) {
        androidx.fragment.app.Fragment currentFragment = 
                getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof ChatFragment) {
            ((ChatFragment) currentFragment).refreshMessagesIfSamePeer(peerId);
        }
    }
    
    private void updateUserOnlineStatus(int userId, boolean isOnline) {
        androidx.fragment.app.Fragment currentFragment = 
                getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof MessagesListFragment) {
            ((MessagesListFragment) currentFragment).updateUserOnlineStatus(userId, isOnline);
        } else if (currentFragment instanceof org.nikanikoo.flux.ui.fragments.friends.FriendsListFragment) {
            ((org.nikanikoo.flux.ui.fragments.friends.FriendsListFragment) currentFragment)
                    .updateUserOnlineStatus(userId, isOnline);
        }
    }
}
