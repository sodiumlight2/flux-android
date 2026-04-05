package org.nikanikoo.flux.ui.fragments.media;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.nikanikoo.flux.R;
import org.nikanikoo.flux.data.managers.AudioManager;
import org.nikanikoo.flux.data.managers.ProfileManager;
import org.nikanikoo.flux.data.models.Audio;
import org.nikanikoo.flux.data.models.UserProfile;
import org.nikanikoo.flux.ui.adapters.audio.AudioAdapter;
import org.nikanikoo.flux.ui.custom.EndlessScrollListener;
import org.nikanikoo.flux.ui.custom.PaginationHelper;
import org.nikanikoo.flux.ui.fragments.BaseFragment;

import java.util.ArrayList;
import java.util.List;

public class MusicListFragment extends BaseFragment implements AudioAdapter.OnAudioClickListener {

    private static final String TAG = "MusicListFragment";
    private static final int AUDIOS_PER_PAGE = 20;

    private RecyclerView recyclerAudios;
    private AudioAdapter audioAdapter;
    private AudioManager audioManager;
    private ProfileManager profileManager;
    private List<Audio> audios;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressLoading;
    private LinearLayoutManager layoutManager;
    private EndlessScrollListener scrollListener;
    private PaginationHelper paginationHelper;
    private boolean isSearchMode = false;
    private String currentSearchQuery = "";
    private int currentUserId = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_music_list, container, false);
        
        setHasOptionsMenu(true);
        
        audioManager = AudioManager.getInstance(requireContext());
        profileManager = ProfileManager.getInstance(requireContext());
        audios = new ArrayList<>();
        
        initViews(view);
        setupRecyclerView();
        setupSwipeRefresh();
        setupEndlessScroll();
        setupErrorView(view, R.id.swipe_refresh);
        setRetryCallback(() -> {
            if (isSearchMode) {
                searchAudios(currentSearchQuery, true);
            } else {
                loadAudios(true);
            }
        });
        loadUserProfile();

        return view;
    }

    private void initViews(View view) {
        recyclerAudios = view.findViewById(R.id.recycler_audios);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        progressLoading = view.findViewById(R.id.progress_loading);
    }

    private void setupRecyclerView() {
        layoutManager = new LinearLayoutManager(requireContext());
        recyclerAudios.setLayoutManager(layoutManager);
        audioAdapter = new AudioAdapter(audios, this);
        recyclerAudios.setAdapter(audioAdapter);
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(() -> {
            scrollListener.resetState();
            if (isSearchMode) {
                searchAudios(currentSearchQuery, true);
            } else {
                loadAudios(true);
            }
        });
    }

    private void setupEndlessScroll() {
        paginationHelper = new PaginationHelper(AUDIOS_PER_PAGE);
        scrollListener = new EndlessScrollListener(layoutManager, paginationHelper) {
            @Override
            public void onLoadMore(int offset, int totalItemsCount, RecyclerView view) {
                if (isSearchMode) {
                    searchAudios(currentSearchQuery, false);
                } else {
                    loadAudios(false);
                }
            }
        };
        recyclerAudios.addOnScrollListener(scrollListener);
    }

    private void loadUserProfile() {
        profileManager.loadProfile(false, new ProfileManager.ProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                currentUserId = profile.getId();
                loadAudios(true);
            }

            @Override
            public void onError(String error) {
                showErrorAuto(error);
            }
        });
    }

    private void loadAudios(boolean refresh) {
        if (!paginationHelper.canLoadMore() && !refresh) {
            return;
        }
        
        if (refresh) {
            progressLoading.setVisibility(View.VISIBLE);
        }

        int offset = refresh ? 0 : paginationHelper.getCurrentOffset();

        audioManager.getAudio(currentUserId, offset, AUDIOS_PER_PAGE, new AudioManager.AudioCallback() {
            @Override
            public void onSuccess(List<Audio> newAudios, int totalCount) {
                paginationHelper.onDataLoaded(newAudios.size());
                progressLoading.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                hideError();

                if (refresh) {
                    audios.clear();
                }
                audios.addAll(newAudios);
                audioAdapter.notifyDataSetChanged();

                if (audios.isEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.audio_no_tracks), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                paginationHelper.stopLoading();
                progressLoading.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                showErrorAuto(error);
            }
        });
    }

    private void searchAudios(String query, boolean refresh) {
        if (query.trim().isEmpty()) {
            isSearchMode = false;
            scrollListener.resetState();
            loadAudios(true);
            return;
        }

        if (!paginationHelper.canLoadMore() && !refresh) {
            return;
        }
        
        isSearchMode = true;
        currentSearchQuery = query;
        
        if (refresh) {
            progressLoading.setVisibility(View.VISIBLE);
        }

        int offset = refresh ? 0 : paginationHelper.getCurrentOffset();

        audioManager.searchAudio(query, offset, AUDIOS_PER_PAGE, new AudioManager.AudioCallback() {
            @Override
            public void onSuccess(List<Audio> newAudios, int totalCount) {
                paginationHelper.onDataLoaded(newAudios.size());
                progressLoading.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                hideError();

                if (refresh) {
                    audios.clear();
                }
                audios.addAll(newAudios);
                audioAdapter.notifyDataSetChanged();

                if (audios.isEmpty() && refresh) {
                    Toast.makeText(requireContext(), getString(R.string.audio_nothing_found), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                paginationHelper.stopLoading();
                progressLoading.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                showErrorAuto(error);
            }
        });
    }

    @Override
    public void onPlayClick(Audio audio, int position) {
        // Check if this audio is already playing
        boolean isCurrentlyPlaying = false; // TODO: Check service state
        
        if (isCurrentlyPlaying) {
            // Open player activity
            Intent intent = new Intent(requireContext(), org.nikanikoo.flux.ui.activities.AudioPlayerActivity.class);
            startActivity(intent);
        } else {
            // Start playing from this position
            startAudioPlayer(audios, position);
        }
    }

    @Override
    public void onAddClick(Audio audio, int position) {
        if (audio.isAdded()) {
            // Удаляем из коллекции
            audioManager.deleteAudio(audio.getId(), audio.getOwnerId(), new AudioManager.AudioActionCallback() {
                @Override
                public void onSuccess() {
                    audio.setAdded(false);
                    audioAdapter.updateAudio(position, audio);
                    Toast.makeText(requireContext(), getString(R.string.audio_removed), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(requireContext(), getString(R.string.audio_remove_error) + error, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Добавляем в коллекцию
            audioManager.addAudio(audio.getId(), audio.getOwnerId(), new AudioManager.AudioActionCallback() {
                @Override
                public void onSuccess() {
                    audio.setAdded(true);
                    audioAdapter.updateAudio(position, audio);
                    Toast.makeText(requireContext(), getString(R.string.audio_added), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(requireContext(), getString(R.string.audio_add_error) + error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void startAudioPlayer(List<Audio> playlist, int startPosition) {
        Intent serviceIntent = new Intent(requireContext(), org.nikanikoo.flux.services.AudioPlayerService.class);
        requireContext().startService(serviceIntent);

        org.nikanikoo.flux.ui.views.AudioPlayerHelper.setPlaylist(requireContext(), playlist, startPosition);
    }

    @Override
    public void onMoreClick(Audio audio, int position) {
        // TODO: Show more options dialog
        Toast.makeText(requireContext(), getString(R.string.audio_dob), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_audio, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) {
            SearchView searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {
                searchView.setQueryHint(getString(R.string.audio_search));
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        scrollListener.resetState();
                        searchAudios(query, true);
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        if (newText.isEmpty() && isSearchMode) {
                            isSearchMode = false;
                            scrollListener.resetState();
                            loadAudios(true);
                        }
                        return true;
                    }
                });
            }
        }
    }
}