package org.nikanikoo.flux.ui.fragments.news;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.nikanikoo.flux.ui.fragments.BaseFragment;
import org.nikanikoo.flux.ui.fragments.comments.CommentsFragment;
import org.nikanikoo.flux.ui.custom.EndlessScrollListener;
import org.nikanikoo.flux.ui.custom.FeedPaginationHelper;
import org.nikanikoo.flux.ui.fragments.profile.GroupProfileFragment;
import org.nikanikoo.flux.Constants;
import org.nikanikoo.flux.data.managers.api.OpenVKApi;
import org.nikanikoo.flux.data.managers.LikesManager;
import org.nikanikoo.flux.data.models.Post;
import org.nikanikoo.flux.ui.adapters.posts.PostAdapter;
import org.nikanikoo.flux.data.managers.PostsManager;
import org.nikanikoo.flux.ui.fragments.profile.ProfileFragment;
import org.nikanikoo.flux.data.managers.ProfileManager;
import org.nikanikoo.flux.R;
import org.nikanikoo.flux.data.models.UserProfile;
import org.nikanikoo.flux.ui.activities.CreatePostActivity;
import org.nikanikoo.flux.ui.activities.MainActivity;
import org.nikanikoo.flux.utils.Logger;

import java.util.ArrayList;
import java.util.List;


public class NewsFragment extends BaseFragment implements PostAdapter.OnPostClickListener {
    private static final String TAG = "NewsFragment";

    private RecyclerView recyclerPosts;
    private PostAdapter postAdapter;
    private PostsManager postsManager;
    private ProfileManager profileManager;
    private List<Post> posts;
    private SwipeRefreshLayout swipeRefresh;
    private ImageView arrow;
    private FloatingActionButton fabCreatePost;
    private ProgressBar progressLoading;
    private LinearLayoutManager layoutManager;
    private EndlessScrollListener scrollListener;
    private FeedPaginationHelper paginationHelper;
    
    // Тип новостей: true = подписки, false = все новости
    private boolean isSubscriptionMode = true;
    private String[] newsTypes;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_news, container, false);

        newsTypes = new String[] { getString(R.string.toolbar_subsfeed), getString(R.string.toolbar_globalfeed) };
        postsManager = PostsManager.getInstance(requireContext());
        profileManager = ProfileManager.getInstance(requireContext());
        posts = new ArrayList<>();

        // Инициализация ErrorViewHandler
        setupErrorView(view, R.id.swipe_refresh_news);
        setRetryCallback(() -> {
            scrollListener.resetState();
            loadPosts(true);
        });

        initViews(view);
        setupRecyclerView();
        setupSwipeRefresh();
        setupEndlessScroll();
        setupToolbarTitle();
        loadPosts(true);

        return view;
    }
    
    private void setupToolbarTitle() {
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            String currentTitle = isSubscriptionMode ? newsTypes[0] : newsTypes[1];
            mainActivity.setToolbarTitleClickable(currentTitle, v -> showNewsTypeDialog());
        }
    }
    
    private void showNewsTypeDialog() {
        if (getContext() == null) return;
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
        builder.setTitle(getString(R.string.news_select_type));
        
        int currentSelection = isSubscriptionMode ? 0 : 1;
        
        builder.setSingleChoiceItems(newsTypes, currentSelection, (dialog, which) -> {
            boolean newMode = (which == 0);
            if (newMode != isSubscriptionMode) {
                isSubscriptionMode = newMode;
                setupToolbarTitle();
                scrollListener.resetState();
                loadPosts(true);
            }
            dialog.dismiss();
        });
        
        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.show();
    }

    private void initViews(View view) {
        recyclerPosts = view.findViewById(R.id.recycler_posts);
        swipeRefresh = view.findViewById(R.id.swipe_refresh_news);
        fabCreatePost = view.findViewById(R.id.fab_create_post);
        progressLoading = view.findViewById(R.id.progress_loading);
        arrow = view.findViewById(R.id.news_toolbar_arrow);
        
        // Обработчик клика на FAB
        if (fabCreatePost != null) {
            fabCreatePost.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), CreatePostActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupRecyclerView() {
        layoutManager = new LinearLayoutManager(getContext());
        recyclerPosts.setLayoutManager(layoutManager);
        postAdapter = new PostAdapter(getContext(), posts);
        postAdapter.setOnPostClickListener(this);
        recyclerPosts.setAdapter(postAdapter);
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(() -> {
            Logger.d(TAG, " Refreshing posts...");
            scrollListener.resetState();
            loadPosts(true);
        });
    }

    private void setupEndlessScroll() {
        paginationHelper = new FeedPaginationHelper(Constants.Api.POSTS_PER_PAGE);
        scrollListener = new EndlessScrollListener(layoutManager, paginationHelper) {
            @Override
            public void onLoadMore(int offset, int totalItemsCount, RecyclerView view) {
                Logger.d(TAG, " Loading more posts, next_from: " + paginationHelper.getNextFromCursor());
                loadPosts(false);
            }
        };
        recyclerPosts.addOnScrollListener(scrollListener);
    }

    private void loadPosts(boolean isRefresh) {
        Logger.d(TAG, " loadPosts called, isRefresh=" + isRefresh +
            ", canLoadMore=" + paginationHelper.canLoadMore() +
            ", nextFrom=" + paginationHelper.getNextFromCursor());

        if (!paginationHelper.canLoadMore() && !isRefresh) {
            Logger.d(TAG, " Cannot load more, skipping request");
            return;
        }

        String nextFrom = isRefresh ? null : paginationHelper.getNextFromCursor();

        // Показываем прогрессбар при первой загрузке или обновлении
        if (isRefresh && posts.isEmpty()) {
            progressLoading.setVisibility(View.VISIBLE);
            recyclerPosts.setVisibility(View.GONE);
        }

        // Показываем индикатор загрузки только при загрузке новых постов
        if (!isRefresh && postAdapter.getPostsCount() > 0) {
            postAdapter.showLoading();
        }

        Logger.d(TAG, " Loading posts with next_from: " + nextFrom + ", isRefresh: " + isRefresh + 
            ", mode: " + (isSubscriptionMode ? "subscriptions" : "global"));

        // Вызываем startLoading ПОСЛЕ проверки canLoadMore
        paginationHelper.startLoading();

        // Выбираем метод загрузки в зависимости от режима
        if (isSubscriptionMode) {
            postsManager.loadSubscriptionNewsFeed(nextFrom, new PostsManager.FeedPostsCallback() {
                @Override
                public void onSuccess(List<Post> loadedPosts, String nextFrom) {
                    Logger.d(TAG, " Successfully loaded " + loadedPosts.size() + " subscription posts, next_from: " + nextFrom);
                    handlePostsLoaded(loadedPosts, nextFrom, isRefresh);
                }

                @Override
                public void onError(String error) {
                    Logger.d(TAG, " Error loading subscription newsfeed: " + error);
                    handlePostsError(error, isRefresh);
                }
            });
        } else {
            postsManager.loadGlobalNewsFeed(nextFrom, new PostsManager.FeedPostsCallback() {
                @Override
                public void onSuccess(List<Post> loadedPosts, String nextFrom) {
                    Logger.d(TAG, " Successfully loaded " + loadedPosts.size() + " global posts, next_from: " + nextFrom);
                    handlePostsLoaded(loadedPosts, nextFrom, isRefresh);
                }

                @Override
                public void onError(String error) {
                    Logger.d(TAG, " Error loading global newsfeed: " + error);
                    handlePostsError(error, isRefresh);
                }
            });
        }
    }
    
    private void handlePostsLoaded(List<Post> loadedPosts, String nextFrom, boolean isRefresh) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                swipeRefresh.setRefreshing(false);
                hideError();

                // Скрываем прогрессбар и показываем список
                progressLoading.setVisibility(View.GONE);
                recyclerPosts.setVisibility(View.VISIBLE);

                int postsBeforeAdd = postAdapter.getPostsCount();

                if (isRefresh) {
                    postAdapter.setPosts(loadedPosts);
                    posts.clear();
                    posts.addAll(loadedPosts);
                } else {
                    postAdapter.addPosts(loadedPosts);
                    posts.addAll(loadedPosts);
                }

                // Вычисляем сколько постов реально добавилось (после фильтрации дубликатов)
                int postsAfterAdd = postAdapter.getPostsCount();
                int actuallyAdded = postsAfterAdd - postsBeforeAdd;

                Logger.d(TAG, " Loaded " + loadedPosts.size() + " posts from API, " +
                    actuallyAdded + " actually added after duplicate filtering");

                // Уведомляем PaginationHelper о РЕАЛЬНОМ количестве добавленных постов
                paginationHelper.onDataLoaded(actuallyAdded, nextFrom);

                if (loadedPosts.isEmpty() && postAdapter.getPostsCount() == 0) {
                    Logger.d(TAG, " No posts received, trying alternative method...");
                    loadPublicPosts(isRefresh);
                }
            });
        }
    }
    
    private void handlePostsError(String error, boolean isRefresh) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                paginationHelper.stopLoading();
                swipeRefresh.setRefreshing(false);
                postAdapter.hideLoading();

                progressLoading.setVisibility(View.GONE);

                if (posts.isEmpty()) {
                    showErrorAuto(error);
                    // Пробуем альтернативный метод только если нет постов
                    loadPublicPosts(isRefresh);
                } else {
                    showErrorAuto(error);
                }
            });
        }
    }

    private void loadPublicPosts(boolean isRefresh) {
        Logger.d(TAG, " Trying public posts...");
        
        postsManager.loadPublicPosts(new PostsManager.PostsCallback() {
            @Override
            public void onSuccess(List<Post> loadedPosts) {
                Logger.d(TAG, " Successfully loaded " + loadedPosts.size() + " public posts");
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (isRefresh) {
                            postAdapter.setPosts(loadedPosts);
                        } else {
                            postAdapter.addPosts(loadedPosts);
                        }
                        
                        if (loadedPosts.isEmpty() && posts.isEmpty()) {
                            Logger.d(TAG, " Still no posts, loading test data...");
                            loadTestPosts();
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                Logger.d(TAG, " Error loading public posts: " + error);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (posts.isEmpty()) {
                            Toast.makeText(getContext(), getString(R.string.news_public_loading_error) + error, Toast.LENGTH_SHORT).show();
                            loadTestPosts();
                        }
                    });
                }
            }
        });
    }

    private void loadTestPosts() {
        // Тестовые посты удалены - используем только реальные данные из API
        List<Post> testPosts = new ArrayList<>();
        postAdapter.setPosts(testPosts);
    }

    private void checkLikeStatusForPosts(List<Post> postsToCheck) {
        for (int i = 0; i < postsToCheck.size(); i++) {
            Post post = postsToCheck.get(i);
            final int position = i;
            
            // Проверяем статус лайка только если есть необходимые данные
            if (post.getPostId() != 0 && post.getOwnerId() != 0) {
                postsManager.checkLikeStatus(post, new LikesManager.LikeStatusCallback() {
                    @Override
                    public void onSuccess(boolean isLiked) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (position < posts.size()) {
                                    posts.get(position).setLiked(isLiked);
                                    postAdapter.notifyItemChanged(position);
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Logger.e(TAG, "Error checking like status for post " + post.getPostId() + ": " + error);
                        // Не показываем ошибку пользователю, просто логируем
                    }
                });
            }
        }
    }

    // Реализация интерфейса OnPostClickListener
    @Override
    public void onAuthorClick(int authorId, String authorName, boolean isGroup) {
        if (getActivity() != null) {
            if (isGroup) {
                int groupId = authorId < 0 ? -authorId : authorId;
                GroupProfileFragment groupProfileFragment = GroupProfileFragment.newInstance(groupId, authorName);
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, groupProfileFragment)
                        .addToBackStack("group_" + groupId)
                        .commit();
            } else {
                ProfileFragment profileFragment = ProfileFragment.newInstanceWithId(authorId, authorName);
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, profileFragment)
                        .addToBackStack("profile_" + authorId)
                        .commit();
            }
        }
    }

    @Override
    public void onLikeClick(Post post) {
        // Проверяем, есть ли необходимые данные для лайка
        if (post.getPostId() == 0 || post.getOwnerId() == 0) {
            Toast.makeText(getContext(), getString(R.string.post_like_error), Toast.LENGTH_SHORT).show();
            return;
        }

        // Сохраняем текущее состояние для возможного отката
        final boolean originalLikedState = post.isLiked();
        final int originalLikeCount = post.getLikeCount();
        
        // Сразу обновляем UI (optimistic update)
        final boolean newLikedState = !originalLikedState;
        final int newLikeCount = originalLikedState ? originalLikeCount - 1 : originalLikeCount + 1;
        
        post.setLiked(newLikedState);
        post.setLikeCount(newLikeCount);
        
        // Находим позицию поста и обновляем UI
        for (int i = 0; i < posts.size(); i++) {
            if (posts.get(i).getPostId() == post.getPostId() && 
                posts.get(i).getOwnerId() == post.getOwnerId()) {
                postAdapter.notifyItemChanged(i);
                break;
            }
        }

        // Отправляем запрос на сервер
        postsManager.toggleLikeOptimistic(post, originalLikedState, new PostsManager.LikeToggleCallback() {
            @Override
            public void onSuccess(int serverLikesCount, boolean serverIsLiked) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Обновляем с данными от сервера (на случай расхождений)
                        for (int i = 0; i < posts.size(); i++) {
                            if (posts.get(i).getPostId() == post.getPostId() && 
                                posts.get(i).getOwnerId() == post.getOwnerId()) {
                                posts.get(i).setLikeCount(serverLikesCount);
                                posts.get(i).setLiked(serverIsLiked);
                                postAdapter.notifyItemChanged(i);
                                break;
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Откатываем изменения при ошибке
                        post.setLiked(originalLikedState);
                        post.setLikeCount(originalLikeCount);
                        
                        for (int i = 0; i < posts.size(); i++) {
                            if (posts.get(i).getPostId() == post.getPostId() && 
                                posts.get(i).getOwnerId() == post.getOwnerId()) {
                                posts.get(i).setLiked(originalLikedState);
                                posts.get(i).setLikeCount(originalLikeCount);
                                postAdapter.notifyItemChanged(i);
                                break;
                            }
                        }
                        
                        Toast.makeText(getContext(), getString(R.string.error_loading) + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    @Override
    public void onCommentClick(Post post) {
        // Переход к комментариям поста
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
        Toast.makeText(getContext(), post.getAuthorName(), Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onPostLongClick(Post post, View view) {
        // Показываем контекстное меню для поста
        showPostContextMenu(post, view);
    }
    
    private void showPostContextMenu(Post post, View view) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(requireContext(), view);
        
        // Получаем текущий профиль пользователя для проверки прав
        UserProfile currentProfile = profileManager.getCachedProfileSync();
        boolean isOwnPost = currentProfile != null && post.getAuthorId() == currentProfile.getId();
        boolean isOnOwnWall = currentProfile != null && post.getOwnerId() == currentProfile.getId();
        
        if (isOwnPost) {
            // Свой пост - можно редактировать, потом удалить
            popup.getMenu().add(0, 1, 0, getString(R.string.edit));
            popup.getMenu().add(0, 3, 0, getString(R.string.pin));
            popup.getMenu().add(0, 4, 0, getString(R.string.copy_link));
            popup.getMenu().add(0, 5, 0, getString(R.string.delete));
        } else if (isOnOwnWall) {
            // Пост на своей стене - можно закрепить, потом удалить
            popup.getMenu().add(0, 3, 0, getString(R.string.pin));
            popup.getMenu().add(0, 4, 0, getString(R.string.copy_link));
            popup.getMenu().add(0, 5, 0, getString(R.string.delete));
        } else {
            // Общие действия
            popup.getMenu().add(0, 4, 0, getString(R.string.copy_link));
        }
        
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: // Редактировать
                    editPost(post);
                    return true;
                case 5: // Удалить
                    deletePost(post);
                    return true;
                case 3: // Закрепить
                    pinPost(post);
                    return true;
                case 4: // Скопировать ссылку
                    copyPostLink(post);
                    return true;
                default:
                    return false;
            }
        });
        
        popup.show();
    }
    
    private void editPost(Post post) {
        Toast.makeText(getContext(), getString(R.string.post_edit_not_supported), Toast.LENGTH_SHORT).show();
    }
    
    private void deletePost(Post post) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.post_delete_confirm))
                .setMessage(getString(R.string.post_delete_message))
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                    // Здесь будет вызов API для удаления поста
                    Toast.makeText(getContext(), getString(R.string.post_delete_not_supported), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }
    
    private void pinPost(Post post) {
        Toast.makeText(getContext(), getString(R.string.post_pin_not_supported), Toast.LENGTH_SHORT).show();
    }
    
    private void copyPostLink(Post post) {
        OpenVKApi api = OpenVKApi.getInstance(requireContext());
        String baseUrl = api.getBaseUrl();
        // Убираем /method из URL если он есть
        if (baseUrl.endsWith("/method")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 7);
        }
        String postUrl = baseUrl + "/wall" + post.getOwnerId() + "_" + post.getPostId();
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) 
                requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Post URL", postUrl);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(getContext(), getString(R.string.copied_link), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getActivity() != null) {
            arrow = getActivity().findViewById(R.id.news_toolbar_arrow);
            if (arrow != null) {
                arrow.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (getActivity() != null) {
            arrow = getActivity().findViewById(R.id.news_toolbar_arrow);
            if (arrow != null) {
                arrow.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Очищаем слушатель скролла
        if (recyclerPosts != null && scrollListener != null) {
            recyclerPosts.removeOnScrollListener(scrollListener);
        }
    }
}