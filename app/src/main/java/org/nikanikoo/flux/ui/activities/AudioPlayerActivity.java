package org.nikanikoo.flux.ui.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.nikanikoo.flux.R;
import org.nikanikoo.flux.data.models.Audio;
import org.nikanikoo.flux.services.AudioPlayerService;
import org.nikanikoo.flux.ui.adapters.audio.PlaylistAdapter;
import org.nikanikoo.flux.utils.AlbumArtFetcher;
import org.nikanikoo.flux.utils.Logger;
import org.nikanikoo.flux.utils.LocaleManager;
import org.nikanikoo.flux.utils.ThemeManager;

import java.util.List;
import java.util.Locale;

public class AudioPlayerActivity extends AppCompatActivity implements AudioPlayerService.PlayerCallback {

    private static final String TAG = "AudioPlayerActivity";

    private AudioPlayerService playerService;
    private boolean serviceBound = false;

    private TextView trackTitle;
    private TextView trackArtist;
    private ImageView albumArt;
    private ImageView albumArtEmpty;
    private TextView currentTime;
    private TextView totalTime;
    private SeekBar seekBar;
    private ImageButton btnPlayPause;
    private ImageButton btnPrevious;
    private ImageButton btnNext;

    private DrawerLayout drawerLayout;
    private RecyclerView playlistRecycler;
    private TextView playlistCount;
    private PlaylistAdapter playlistAdapter;

    private AlbumArtFetcher albumArtFetcher;
    private boolean isUserSeeking = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AudioPlayerService.AudioBinder binder = (AudioPlayerService.AudioBinder) service;
            playerService = binder.getService();
            serviceBound = true;
            playerService.registerCallback(AudioPlayerActivity.this);
            updateUI();
            Logger.d(TAG, "Service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            Logger.d(TAG, "Service disconnected");
        }
    };

    @Override
    protected void attachBaseContext(Context newBase) {
        LocaleManager localeManager = LocaleManager.getInstance(newBase);
        Context context = localeManager.updateContext(newBase);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);
        
        ThemeManager.applySystemBarsAppearance(this);
        
        initViews();
        setupToolbar();
        setupControls();
        bindService();
    }

    private void initViews() {
        trackTitle = findViewById(R.id.track_title);
        trackArtist = findViewById(R.id.track_artist);
        albumArt = findViewById(R.id.album_art);
        albumArtEmpty = findViewById(R.id.album_art_empty);
        currentTime = findViewById(R.id.current_time);
        totalTime = findViewById(R.id.total_time);
        seekBar = findViewById(R.id.seek_bar);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnPrevious = findViewById(R.id.btn_previous);
        btnNext = findViewById(R.id.btn_next);
        drawerLayout = findViewById(R.id.drawer_layout);
        playlistRecycler = findViewById(R.id.playlist_recycler);
        playlistCount = findViewById(R.id.playlist_count);
        
        albumArtFetcher = new AlbumArtFetcher(this);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupControls() {
        btnPlayPause.setOnClickListener(v -> {
            if (serviceBound) {
                if (playerService.isPlaying()) {
                    playerService.pause();
                } else {
                    playerService.play();
                }
            }
        });

        btnPrevious.setOnClickListener(v -> {
            if (serviceBound) {
                playerService.previous();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (serviceBound) {
                playerService.next();
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (serviceBound) {
                    playerService.seekTo(seekBar.getProgress());
                }
                isUserSeeking = false;
            }
        });

        setupPlaylist();
    }

    private void setupPlaylist() {
        playlistRecycler.setLayoutManager(new LinearLayoutManager(this));
        playlistAdapter = new PlaylistAdapter(
                new java.util.ArrayList<>(),
                -1,
                position -> {
                    if (serviceBound) {
                        playerService.seekToTrack(position);
                        drawerLayout.closeDrawer(GravityCompat.END);
                    }
                }
        );
        playlistRecycler.setAdapter(playlistAdapter);
    }

    private void bindService() {
        Intent intent = new Intent(this, AudioPlayerService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_audio_player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_playlist) {
            if (drawerLayout != null && !drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.openDrawer(GravityCompat.END);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateUI() {
        if (!serviceBound) return;

        Audio currentAudio = playerService.getCurrentAudio();
        if (currentAudio != null) {
            trackTitle.setText(currentAudio.getTitle());
            trackArtist.setText(currentAudio.getArtist());
            loadAlbumArt(currentAudio.getArtist(), currentAudio.getTitle());
        }

        updatePlaylist();
        updatePlayPauseButton();
    }

    private void loadAlbumArt(String artist, String title) {
        albumArt.setVisibility(android.view.View.GONE);
        albumArtEmpty.setVisibility(android.view.View.VISIBLE);
        
        if (artist == null || title == null || artist.isEmpty() || title.isEmpty()) {
            return;
        }

        albumArtFetcher.loadAlbumArt(artist, title, albumArt, R.drawable.ic_music, new AlbumArtFetcher.AlbumArtCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                albumArt.setVisibility(android.view.View.VISIBLE);
                albumArtEmpty.setVisibility(android.view.View.GONE);
            }

            @Override
            public void onError(String error) {
                Logger.d(TAG, "Failed to load album art: " + error);
            }
        });
    }

    private void updatePlaylist() {
        if (serviceBound && playlistAdapter != null) {
            List<Audio> playlist = playerService.getPlaylist();
            int currentPosition = playerService.getCurrentTrackPosition();
            playlistAdapter.updatePlaylist(playlist, currentPosition);
            playlistCount.setText(playlist.size() > 0 ? String.valueOf(playlist.size()) : "");
        }
    }

    private void updatePlayPauseButton() {
        if (serviceBound && playerService.isPlaying()) {
            btnPlayPause.setImageResource(R.drawable.ic_pause);
        } else {
            btnPlayPause.setImageResource(R.drawable.ic_play);
        }
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        runOnUiThread(this::updatePlayPauseButton);
    }

    @Override
    public void onTrackChanged(Audio audio, int position) {
        runOnUiThread(() -> {
            trackTitle.setText(audio.getTitle());
            trackArtist.setText(audio.getArtist());
            loadAlbumArt(audio.getArtist(), audio.getTitle());
            updatePlaylist();
        });
    }

    @Override
    public void onProgressUpdate(int currentPosition, int duration) {
        runOnUiThread(() -> {
            if (!isUserSeeking) {
                seekBar.setMax(duration);
                seekBar.setProgress(currentPosition);
                currentTime.setText(formatTime(currentPosition));
                totalTime.setText(formatTime(duration));
            }
        });
    }

    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            Logger.e(TAG, "Playback error: " + error);
        });
    }

    private String formatTime(int milliseconds) {
        int seconds = milliseconds / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            playerService.unregisterCallback(this);
            unbindService(serviceConnection);
            serviceBound = false;
        }
        if (albumArtFetcher != null) {
            albumArtFetcher.shutdown();
        }
    }
}
