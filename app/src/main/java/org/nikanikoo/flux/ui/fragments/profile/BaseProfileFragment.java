package org.nikanikoo.flux.ui.fragments.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.nikanikoo.flux.Constants;
import org.nikanikoo.flux.R;
import org.nikanikoo.flux.data.managers.LikesManager;
import org.nikanikoo.flux.data.managers.PostsManager;
import org.nikanikoo.flux.data.models.Post;
import org.nikanikoo.flux.ui.adapters.posts.PostAdapter;
import org.nikanikoo.flux.ui.custom.EndlessScrollListener;
import org.nikanikoo.flux.ui.custom.PaginationHelper;
import org.nikanikoo.flux.ui.fragments.BaseFragment;
import org.nikanikoo.flux.utils.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Базовый класс для фрагментов профилей (пользователь и группа).
 * Содержит общую логику для работы с постами, пагинацией и RecyclerView.
 */
public abstract class BaseProfileFragment extends BaseFragment implements PostAdapter.OnPostClickListener {
    
    private static final String TAG = "BaseProfileFragment";
    
    // Views
    protected RecyclerView recyclerPosts;
    protected SwipeRefreshLayout swipeRefresh;
    protected View progressLoading;
    
    // Adapters and Managers
    protected PostAdapter postAdapter;
    protected PostsManager postsManager;
    protected LikesManager likesManager;
    
    // Pagination
    protected LinearLayoutManager layoutManager;
    protected EndlessScrollListener scrollListener;
    protected PaginationHelper paginationHelper;
    
    // Data
    protected List<Post> posts = new ArrayList<>();
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(getLayoutResourceId(), container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        postsManager = PostsManager.getInstance(requireContext());
        likesManager = LikesManager.getInstance(requireContext());

        // Инициализируем paginationHelper всегда
        paginationHelper = new PaginationHelper(Constants.Api.POSTS_PER_PAGE);

        initViews(view);
        int contentId = swipeRefresh != null ? swipeRefresh.getId() : (recyclerPosts != null ? recyclerPosts.getId() : android.R.id.content);
        setupErrorView(view, contentId);
        setRetryCallback(() -> {
            paginationHelper.reset();
            posts.clear();
            loadPosts(true);
        });

        setupSwipeRefresh();
        setupRecyclerView();

        loadData();
    }
    
    /**
     * Возвращает ID layout ресурса
     */
    protected abstract int getLayoutResourceId();
    
    /**
     * Инициализация views
     */
    protected void initViews(View view) {
        // Пытаемся найти RecyclerView по разным ID
        recyclerPosts = view.findViewById(R.id.recycler_posts);
        if (recyclerPosts == null) {
            recyclerPosts = view.findViewById(R.id.recycler_profile_posts);
        }
        if (recyclerPosts == null) {
            recyclerPosts = view.findViewById(R.id.recycler_group_posts);
        }
        
        // Пытаемся найти SwipeRefreshLayout по разным ID
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        if (swipeRefresh == null) {
            swipeRefresh = view.findViewById(R.id.swipe_refresh_profile);
        }
        if (swipeRefresh == null) {
            swipeRefresh = view.findViewById(R.id.swipe_refresh_group);
        }
        
        progressLoading = view.findViewById(R.id.progress_loading);
    }
    
    /**
     * Настройка SwipeRefreshLayout
     */
    protected void setupSwipeRefresh() {
        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(() -> {
                paginationHelper.reset();
                posts.clear();
                loadPosts(true);
            });
        }
    }
    
    /**
     * Настройка RecyclerView и пагинации
     */
    protected void setupRecyclerView() {
        if (recyclerPosts == null) {
            Logger.w(TAG, "RecyclerView not found in layout, skipping setup");
            return;
        }
        
        Logger.d(TAG, "setupRecyclerView: setting up adapter and scroll listener");

        layoutManager = new LinearLayoutManager(getContext());
        recyclerPosts.setLayoutManager(layoutManager);

        // RecyclerView optimizations
        recyclerPosts.setHasFixedSize(true);
        recyclerPosts.setItemViewCacheSize(20);

        // Enable item animator for smooth animations
        recyclerPosts.setItemAnimator(null);
        
        postAdapter = new PostAdapter(getContext(), posts);
        postAdapter.setOnPostClickListener(this);
        recyclerPosts.setAdapter(postAdapter);
        
        scrollListener = new EndlessScrollListener(layoutManager, paginationHelper) {
            @Override
            public void onLoadMore(int offset, int totalItemsCount, RecyclerView view) {
                Logger.d(TAG, "EndlessScrollListener.onLoadMore: offset=" + offset + ", canLoadMore=" + paginationHelper.canLoadMore());
                if (paginationHelper.canLoadMore()) {
                    loadPosts(false);
                }
            }
        };
        
        recyclerPosts.addOnScrollListener(scrollListener);
        
        Logger.d(TAG, "setupRecyclerView: complete, postAdapter=" + (postAdapter != null ? "created" : "null"));
    }
    
    /**
     * Загрузка данных профиля
     */
    protected abstract void loadData();
    
    /**
     * Загрузка постов
     */
    protected abstract void loadPosts(boolean isRefresh);
    
    /**
     * Показать индикатор загрузки
     */
    protected void showLoading() {
        if (progressLoading != null) {
            progressLoading.setVisibility(View.VISIBLE);
        }
        if (recyclerPosts != null) {
            recyclerPosts.setVisibility(View.GONE);
        }
    }
    
    /**
     * Скрыть индикатор загрузки
     */
    protected void hideLoading() {
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(false);
        }
        if (progressLoading != null) {
            progressLoading.setVisibility(View.GONE);
        }
        if (recyclerPosts != null) {
            recyclerPosts.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Обработка успешной загрузки постов
     */
    protected void onPostsLoaded(List<Post> loadedPosts, boolean isRefresh) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            hideLoading();
            hideError();

            if (postAdapter == null) {
                Logger.e(TAG, "postAdapter is null, cannot update posts");
                return;
            }
            
            int postsBeforeAdd = postAdapter.getPostsCount();
            
            if (isRefresh) {
                postAdapter.setPosts(loadedPosts);
                posts.clear();
                posts.addAll(loadedPosts);
            } else {
                postAdapter.addPosts(loadedPosts);
                posts.addAll(loadedPosts);
            }
            
            int postsAfterAdd = postAdapter.getPostsCount();
            int actuallyAdded = postsAfterAdd - postsBeforeAdd;
            
            Logger.d(TAG, "onPostsLoaded: isRefresh=" + isRefresh + ", loaded=" + loadedPosts.size() + 
                    ", before=" + postsBeforeAdd + ", after=" + postsAfterAdd + ", added=" + actuallyAdded);
            
            paginationHelper.onDataLoaded(actuallyAdded);
            
            if (loadedPosts.isEmpty() && postAdapter.getPostsCount() == 0) {
                showEmptyState();
            }
        });
    }
    
    /**
     * Обработка ошибки загрузки постов
     */
    protected void onPostsError(String error, boolean isRefresh) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            Logger.e(TAG, "Error loading posts: " + error);
            paginationHelper.stopLoading();
            hideLoading();

            showErrorAuto(error);
        });
    }
    
    /**
     * Показать состояние "Нет данных"
     */
    protected void showEmptyState() {
        // Переопределяется в наследниках при необходимости
    }
    
    @Override
    public void onLikeClick(Post post) {
        if (post == null) return;
        
        final boolean originalLikedState = post.isLiked();
        final int originalLikeCount = post.getLikeCount();
        final boolean newLikedState = !originalLikedState;
        final int newLikeCount = originalLikedState ? originalLikeCount - 1 : originalLikeCount + 1;
        
        post.setLiked(newLikedState);
        post.setLikeCount(newLikeCount);
        
        for (int i = 0; i < posts.size(); i++) {
            if (posts.get(i).getPostId() == post.getPostId() && 
                posts.get(i).getOwnerId() == post.getOwnerId()) {
                postAdapter.notifyItemChanged(i);
                break;
            }
        }
        
        likesManager.toggleLike("post", post.getOwnerId(), post.getPostId(), originalLikedState, 
            new LikesManager.LikeCallback() {
                @Override
                public void onSuccess(int newLikesCount) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            post.setLikeCount(newLikesCount);
                            post.setLiked(!originalLikedState);
                            for (int i = 0; i < posts.size(); i++) {
                                if (posts.get(i).getPostId() == post.getPostId() && 
                                    posts.get(i).getOwnerId() == post.getOwnerId()) {
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
                            post.setLiked(originalLikedState);
                            post.setLikeCount(originalLikeCount);
                            for (int i = 0; i < posts.size(); i++) {
                                if (posts.get(i).getPostId() == post.getPostId() && 
                                    posts.get(i).getOwnerId() == post.getOwnerId()) {
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
}
