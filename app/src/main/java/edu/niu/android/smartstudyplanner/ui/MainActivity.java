/**************************************************************
 * Purpose: Single Activity host for the entire app. Owns the *
 * NavHostFragment that swaps fragment destinations, the      *
 * bottom navigation bar that picks them, the toolbar styling,*
 * and the bound MusicService that drives the top mini        *
 * player. Also handles runtime permission prompts for        *
 * notifications and exact alarms on first launch.            *
 **************************************************************/

package edu.niu.android.smartstudyplanner.ui;

import android.app.AlarmManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import edu.niu.android.smartstudyplanner.R;
import edu.niu.android.smartstudyplanner.databinding.ActivityMainBinding;
import edu.niu.android.smartstudyplanner.ui.music.MusicService;
import edu.niu.android.smartstudyplanner.ui.planner.NavigationRequest;

public class MainActivity extends AppCompatActivity
        implements MusicService.MusicStateListener {

    private ActivityMainBinding binding;
    private NavController navController;

    // Shared dark navy used across the toolbar, status bar, and action bar.
    private static final int DEEP_NAVY = 0xFF0A1245;

    // Music service binding state used to drive the top mini player.
    private MusicService musicService;
    private boolean musicBound = false;
    private String currentTrackName = "";

    /**
     * Service connection that grabs the MusicService instance once bound,
     * registers this activity as the playback state listener, and seeds
     * the mini player with whatever is already playing.
     */
    private final ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            musicService.setStateListener(MainActivity.this);
            musicBound = true;

            // Seed the mini player so it reflects current playback as soon
            // as the activity attaches to the service.
            currentTrackName = musicService.getCurrentTrackName();
            updateMiniPlayer(musicService.isPlaying(),
                    musicService.getCurrentTrackIndex(), currentTrackName);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Android 13+ needs the user to explicitly grant POST_NOTIFICATIONS
        // before any notification can be shown.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }

        // Android 12+ requires opting into exact alarms via system settings;
        // bounce the user there if we don't have the permission yet.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
                startActivity(new Intent(
                        android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
            }
        }

        // Match the status bar to the toolbar so the top of the screen reads as one block.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(DEEP_NAVY);
        }

        // Toolbar styling: dark navy with white text and a white back arrow.
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setBackgroundColor(DEEP_NAVY);
        binding.toolbar.setTitleTextColor(0xFFFFFFFF);

        // Tint the back arrow whenever the toolbar lays out, since the
        // navigation icon can be re-set by the Navigation component when
        // the destination changes.
        binding.toolbar.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (binding.toolbar.getNavigationIcon() != null) {
                            binding.toolbar.getNavigationIcon().setTint(0xFFFFFFFF);
                        }
                    }
                });

        // If the layout includes an AppBarLayout wrapper, paint it too so
        // the elevation shadow doesn't ruin the navy block effect.
        if (binding.getRoot().findViewById(R.id.app_bar_layout) != null) {
            binding.getRoot().findViewById(R.id.app_bar_layout)
                    .setBackgroundColor(DEEP_NAVY);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(DEEP_NAVY));
        }

        // Hook the NavController up to the NavHostFragment.
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        // Treat the five primary tabs as top level so back doesn't pop the activity.
        AppBarConfiguration appBarConfig = new AppBarConfiguration.Builder(
                R.id.navigation_planner,
                R.id.navigation_courses,
                R.id.navigation_tasks,
                R.id.navigation_timer,
                R.id.navigation_stats
        ).build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfig);


        // Custom bottom-bar selection. Tapping Planner pops back to the
        // Planner root instead of stacking another instance, which keeps
        // the back stack clean.
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_planner) {
                navController.popBackStack(R.id.navigation_planner, false);
                return true;
            } else if (id == R.id.navigation_courses) {
                navController.navigate(R.id.navigation_courses);
                return true;
            } else if (id == R.id.navigation_tasks) {
                navController.navigate(R.id.navigation_tasks);
                return true;
            } else if (id == R.id.navigation_timer) {
                navController.navigate(R.id.navigation_timer);
                return true;
            } else if (id == R.id.navigation_stats) {
                navController.navigate(R.id.navigation_stats);
                return true;
            }
            return false;
        });

        // React to navigation. The five primary tabs hide the action bar
        // because they each render their own custom header; deeper screens
        // show it so the user has a back arrow.
        navController.addOnDestinationChangedListener(
                (controller, destination, arguments) -> {

                    if (getSupportActionBar() != null) {
                        if (destination.getId() == R.id.navigation_planner
                                || destination.getId() == R.id.navigation_tasks
                                || destination.getId() == R.id.navigation_courses
                                || destination.getId() == R.id.navigation_timer
                                || destination.getId() == R.id.navigation_stats) {
                            getSupportActionBar().hide();
                        } else {
                            getSupportActionBar().show();
                            getSupportActionBar().setTitle(destination.getLabel());
                            getSupportActionBar().setBackgroundDrawable(
                                    new ColorDrawable(DEEP_NAVY));
                        }
                    }

                    // Repaint the toolbar and re-tint the back arrow on
                    // every destination change so the navy stays consistent.
                    binding.toolbar.setBackgroundColor(DEEP_NAVY);
                    binding.toolbar.post(() -> {
                        if (binding.toolbar.getNavigationIcon() != null) {
                            binding.toolbar.getNavigationIcon().setTint(0xFFFFFFFF);
                        }
                    });

                    // Update the pill highlight on the bottom nav after the
                    // bar has had a chance to lay out the new selection.
                    binding.bottomNavigation.post(() ->
                            applyNavPillHighlight(destination.getId()));
                });

        // Mini player play/pause button drives the bound service.
        binding.miniPlayerToggle.setOnClickListener(v -> {
            if (musicBound && musicService != null) {
                musicService.togglePlayPause();
            }
        });

        // Tapping the mini player itself jumps to the Music screen.
        binding.miniPlayer.setOnClickListener(v -> {
            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() != R.id.navigation_music) {
                navController.navigate(R.id.navigation_music);
            }
        });

        // Start the music service (so it survives even if everything unbinds)
        // and bind to it so we can call methods on it directly.
        Intent serviceIntent = new Intent(this, MusicService.class);
        startService(serviceIntent);
        bindService(serviceIntent, musicConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Called by MusicService when its playback state changes. Caches the
     * track name and pushes the update to the mini player on the UI thread.
     */
    @Override
    public void onStateChanged(boolean playing, int trackIndex, String trackName) {
        if (trackName != null && !trackName.isEmpty()) {
            currentTrackName = trackName;
        }
        runOnUiThread(() -> updateMiniPlayer(playing, trackIndex, trackName));
    }

    /**
     * Refreshes the mini player. Hides it when no track is loaded; otherwise
     * shows the track name and a play or pause glyph.
     */
    private void updateMiniPlayer(boolean playing, int trackIndex, String trackName) {
        if (trackIndex == -1) {
            binding.miniPlayer.setVisibility(View.GONE);
            return;
        }
        binding.miniPlayer.setVisibility(View.VISIBLE);
        if (trackName != null && !trackName.isEmpty()) {
            binding.miniPlayerTrack.setText(trackName);
        }
        binding.miniPlayerToggle.setText(playing ? "⏸" : "▶");
    }

    /**
     * Lets MusicFragment borrow the same service binding instead of
     * creating its own. Returns null if we haven't bound yet.
     */
    public MusicService getMusicService() {
        return musicBound ? musicService : null;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Pick up any deferred tab switch requested by a satellite Activity
        // (e.g. DueTodayActivity). Reset the holder so the request only
        // fires once.
        if (NavigationRequest.navigateTo != -1 && navController != null) {
            int dest = NavigationRequest.navigateTo;
            NavigationRequest.navigateTo = -1;
            binding.bottomNavigation.setSelectedItemId(dest);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release the music service binding so we don't leak it.
        if (musicBound) {
            unbindService(musicConnection);
            musicBound = false;
        }
    }

    /**
     * Adds the rounded pill background behind the active bottom-nav item
     * and clears it from all the others. Called after every destination
     * change, posted to the bar so the views are laid out first.
     */
    private void applyNavPillHighlight(int activeDestinationId) {
        int[] navIds = {
                R.id.navigation_planner,
                R.id.navigation_courses,
                R.id.navigation_tasks,
                R.id.navigation_timer,
                R.id.navigation_stats
        };
        for (int navId : navIds) {
            View itemView = binding.bottomNavigation.findViewById(navId);
            if (itemView == null) continue;
            if (navId == activeDestinationId) {
                itemView.setBackgroundResource(R.drawable.nav_pill_bg);
            } else {
                itemView.setBackground(null);
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}