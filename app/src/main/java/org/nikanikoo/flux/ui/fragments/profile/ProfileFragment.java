package org.nikanikoo.flux.ui.fragments.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;

import org.nikanikoo.flux.R;
import org.nikanikoo.flux.data.managers.PostsManager;
import org.nikanikoo.flux.data.managers.ProfileManager;
import org.nikanikoo.flux.data.models.Post;
import org.nikanikoo.flux.data.models.UserProfile;
import org.nikanikoo.flux.ui.activities.CreatePostActivity;
import org.nikanikoo.flux.ui.activities.MainActivity;
import org.nikanikoo.flux.ui.activities.PhotoViewerActivity;
import org.nikanikoo.flux.ui.dialogs.RepostDialog;
import org.nikanikoo.flux.ui.fragments.comments.CommentsFragment;
import org.nikanikoo.flux.ui.fragments.friends.UserFriendsFragment;
import org.nikanikoo.flux.utils.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Фрагмент профиля пользователя.
 * Использует ProfilePresenter для бизнес-логики и ProfileInfoController для управления UI.
 */
public class ProfileFragment extends BaseProfileFragment implements ProfileContract.View {
    private static final String TAG = "ProfileFragment";
    
    // Presenter и Controller
    private ProfilePresenter presenter;
    private ProfileInfoController infoController;
    
    // Views
    private ProgressBar profileMainProgress;
    private View profileContent;
    private CardView profileMainCard;
    private CardView profileDetailsCard;
    private ImageView expandArrow;
    private MaterialButton btnCreatePostProfile;
    private CardView friendsCard;
    
    // State
    private boolean isDetailsExpanded = false;
    
    // Arguments
    private static final String ARG_USER_NAME = "user_name";
    private static final String ARG_USER_STATUS = "user_status";
    private static final String ARG_USER_ID = "user_id";
    
    private String userName;
    private String userStatus;
    private int userId = -1;

    public static ProfileFragment newInstance(String userName, String userStatus) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_NAME, userName);
        args.putString(ARG_USER_STATUS, userStatus);
        fragment.setArguments(args);
        return fragment;
    }

    public static ProfileFragment newInstanceWithId(int userId, String userName) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_USER_ID, userId);
        args.putString(ARG_USER_NAME, userName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userName = getArguments().getString(ARG_USER_NAME);
            userStatus = getArguments().getString(ARG_USER_STATUS);
            userId = getArguments().getInt(ARG_USER_ID, -1);
        }
        
        // Инициализация Presenter
        ProfileManager profileManager = ProfileManager.getInstance(requireContext());
        presenter = new ProfilePresenter(profileManager);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.fragment_profile;
    }

    @Override
    protected void initViews(View view) {
        super.initViews(view);
        
        // Инициализация Controller
        infoController = new ProfileInfoController(view);
        
        // Инициализация Views
        profileMainProgress = view.findViewById(R.id.profile_main_progress);
        profileContent = view.findViewById(R.id.profile_content);
        profileMainCard = view.findViewById(R.id.profile_main_card);
        profileDetailsCard = view.findViewById(R.id.profile_details_card);
        expandArrow = view.findViewById(R.id.expand_arrow);
        btnCreatePostProfile = view.findViewById(R.id.btn_create_post_profile);
        friendsCard = view.findViewById(R.id.friends_card);
        
        // Настройка обработчиков кликов
        setupClickListeners();
    }
    
    private void setupClickListeners() {
        // Кнопка создания поста
        if (btnCreatePostProfile != null) {
            btnCreatePostProfile.setOnClickListener(v -> presenter.onCreatePostClick());
        }
        
        // Раскрытие деталей
        if (profileMainCard != null) {
            profileMainCard.setOnClickListener(v -> toggleDetailsCard());
        }
        
        // Клик по аватару
        ImageView avatarView = infoController.getAvatarView();
        if (avatarView != null) {
            avatarView.setOnClickListener(v -> presenter.onAvatarClick());
        }
        
        // Клик по карточке друзей
        if (friendsCard != null) {
            friendsCard.setOnClickListener(v -> presenter.onFriendsClick());
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        presenter.attachView(this);
        setToolbarTitleSafe(getString(R.string.profile_title));
        presenter.loadProfile();
    }

    private void setToolbarTitleSafe(String title) {
        if (getActivity() == null) {
            return;
        }
        
        try {
            java.lang.reflect.Method method = getActivity().getClass().getMethod("setToolbarTitle", String.class);
            method.invoke(getActivity(), title);
        } catch (Exception e) {
            if (getActivity() instanceof androidx.appcompat.app.AppCompatActivity) {
                androidx.appcompat.app.AppCompatActivity appCompatActivity = 
                    (androidx.appcompat.app.AppCompatActivity) getActivity();
                if (appCompatActivity.getSupportActionBar() != null) {
                    appCompatActivity.getSupportActionBar().setTitle(title);
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        presenter.detachView();
    }

    @Override
    protected void loadData() {
        // Загрузка профиля выполняется через Presenter
        // Этот метод вызывается из BaseProfileFragment
        if (presenter != null) {
            presenter.loadProfile();
        }
    }

    @Override
    protected void loadPosts(boolean isRefresh) {
        UserProfile profile = presenter.getCurrentProfile();
        Logger.d(TAG, "loadPosts: isRefresh=" + isRefresh + ", profile=" + (profile != null ? profile.getId() : "null"));
        
        if (profile == null) {
            Logger.w(TAG, "loadPosts: profile is null, skipping");
            return;
        }
        
        Logger.d(TAG, "loadPosts: canLoadMore=" + paginationHelper.canLoadMore() + ", isLoading=" + paginationHelper.isLoading());
        
        if (!paginationHelper.canLoadMore() && !isRefresh) {
            Logger.w(TAG, "loadPosts: cannot load more and not refresh, skipping");
            return;
        }
        
        if (!isRefresh) {
            paginationHelper.startLoading();
        }
        
        int targetUserId = isForeignProfile() ? userId : profile.getId();
        
        if (isRefresh) {
            paginationHelper.reset();
        }
        
        int offset = paginationHelper.getCurrentOffset();
        Logger.d(TAG, "loadPosts: targetUserId=" + targetUserId + ", offset=" + offset);
        
        postsManager.loadUserPosts(targetUserId, offset, new PostsManager.PostsCallback() {
            @Override
            public void onSuccess(List<Post> loadedPosts) {
                Logger.d(TAG, "loadPosts: onSuccess, loadedPosts=" + loadedPosts.size());
                onPostsLoaded(loadedPosts, isRefresh);
            }

            @Override
            public void onError(String error) {
                Logger.e(TAG, "loadPosts: onError=" + error);
                onPostsError(error, isRefresh);
            }
        });
    }

    // ==================== ProfileContract.View Implementation ====================

    @Override
    public void showProfileLoading() {
        if (profileMainProgress != null) {
            profileMainProgress.setVisibility(View.VISIBLE);
        }
        if (profileContent != null) {
            profileContent.setVisibility(View.GONE);
        }
    }

    @Override
    public void hideProfileLoading() {
        if (profileMainProgress != null) {
            profileMainProgress.setVisibility(View.GONE);
        }
        if (profileContent != null) {
            profileContent.setVisibility(View.VISIBLE);
        }
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(false);
        }
    }

    @Override
    public void displayProfile(UserProfile profile) {
        // Обновление заголовка
        setToolbarTitleSafe(profile.getFullName());

        // Обновление информации через Controller
        infoController.updateProfileInfo(profile);

        // Загружаем посты после загрузки профиля
        loadPosts(true);
    }

    @Override
    public void showProfileError(String error) {
        showErrorAuto(error);
    }

    @Override
    public void showMessage(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void navigateToFriends(int userId, String userName) {
        if (getActivity() == null) {
            return;
        }
        
        UserFriendsFragment friendsFragment = UserFriendsFragment.newInstance(userId, userName);
        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, friendsFragment)
                .addToBackStack("friends_" + userId)
                .commit();
    }

    @Override
    public void openProfilePhoto(String photoUrl, String userName) {
        if (photoUrl == null || photoUrl.isEmpty()) {
            return;
        }
        
        List<String> avatarUrls = new ArrayList<>();
        avatarUrls.add(photoUrl);
        
        PhotoViewerActivity.start(getContext(), avatarUrls, 0, userName);
    }

    @Override
    public void openCreatePost(int ownerId) {
        Intent intent = new Intent(getActivity(), CreatePostActivity.class);
        if (ownerId > 0) {
            intent.putExtra("owner_id", ownerId);
        }
        startActivity(intent);
    }

    @Override
    public void setCreatePostButtonVisible(boolean visible) {
        if (btnCreatePostProfile != null) {
            btnCreatePostProfile.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public int getCurrentUserId() {
        return userId;
    }

    @Override
    public boolean isForeignProfile() {
        return userId > 0;
    }

    // ==================== Helper Methods ====================

    private void toggleDetailsCard() {
        if (profileDetailsCard == null || expandArrow == null) {
            return;
        }
        
        if (isDetailsExpanded) {
            profileDetailsCard.setVisibility(View.GONE);
            expandArrow.setImageResource(R.drawable.ic_expand_more);
            isDetailsExpanded = false;
        } else {
            profileDetailsCard.setVisibility(View.VISIBLE);
            expandArrow.setImageResource(R.drawable.ic_expand_less);
            isDetailsExpanded = true;
        }
    }

    // ==================== Post Click Listeners ====================

    @Override
    public void onAuthorClick(int authorId, String authorName, boolean isGroup) {
        Logger.d(TAG, "onAuthorClick: authorId=" + authorId + ", userId=" + userId + ", isGroup=" + isGroup);
        UserProfile currentProfile = presenter.getCurrentProfile();
        
        Logger.d(TAG, "currentProfile=" + (currentProfile != null ? currentProfile.getId() : "null"));
        Logger.d(TAG, "activity=" + (getActivity() != null ? getActivity().getClass().getSimpleName() : "null"));
        
        if (authorId != userId && (currentProfile == null || authorId != currentProfile.getId())) {
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                if (isGroup) {
                    int groupId = authorId < 0 ? -authorId : authorId;
                    Logger.d(TAG, "Navigating to group " + groupId);
                    GroupProfileFragment groupProfileFragment = GroupProfileFragment.newInstance(groupId, authorName);
                    mainActivity.getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, groupProfileFragment)
                            .addToBackStack("group_" + groupId)
                            .commit();
                    Logger.d(TAG, "Transaction committed, backStackCount=" + mainActivity.getSupportFragmentManager().getBackStackEntryCount());
                } else {
                    Logger.d(TAG, "Navigating to profile " + authorId);
                    ProfileFragment profileFragment = ProfileFragment.newInstanceWithId(authorId, authorName);
                    mainActivity.getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, profileFragment)
                            .addToBackStack("profile_" + authorId)
                            .commit();
                    Logger.d(TAG, "Transaction committed, backStackCount=" + mainActivity.getSupportFragmentManager().getBackStackEntryCount());
                }
            } else {
                Logger.w(TAG, "Activity is not MainActivity!");
            }
        } else {
            Logger.d(TAG, "Skipping navigation - same profile");
        }
    }

    @Override
    public void onCommentClick(Post post) {
        CommentsFragment commentsFragment = CommentsFragment.newInstance(post);
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, commentsFragment)
                    .addToBackStack("comments_" + post.getPostId())
                    .commit();
        }
    }

    @Override
    public void onShareClick(Post post) {
        RepostDialog.show(requireContext(), post, (repostedPost, comment) -> {
            Logger.d(TAG, "Repost with comment: " + comment);
        });
    }

    @Override
    public void onPostLongClick(Post post, View view) {
    }
}
