package edu.niu.android.smartstudyplanner.ui.music;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import edu.niu.android.smartstudyplanner.R;
import edu.niu.android.smartstudyplanner.databinding.FragmentMusicBinding;
import edu.niu.android.smartstudyplanner.ui.MainActivity;

public class MusicFragment extends Fragment {

    private FragmentMusicBinding binding;

    // Reference to the bound MusicService obtained from MainActivity.
    private MusicService musicService;

    // Index of the currently selected track, or -1 if none.
    private int currentTrackIndex = -1;

    // Mirrors the service's playback state for quick UI checks.
    private boolean isPlaying = false;

    // Drives the once per second tick of the session timer label.
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private int secondsElapsed = 0;
    private Runnable timerRunnable;

    // Display name for each of the 16 tracks.
    private final String[] trackNames = {
            "Sleepy Rain", "Fireplace Holiday", "White Noise",
            "Ocean Wave Loops", "Ocean Wave", "Nature",
            "Winds Flute", "Nature Music", "Ambient Electronica",
            "Autumn Leaves", "Coffee Shop", "Morning Coffee",
            "Breezy Escape", "Dream of Peace", "Chill", "Coffee Shop Music"
    };

    // Emoji shown next to each track name, matched by index.
    private final String[] trackEmojis = {
            "🌧️", "🔥", "🌫️", "🌊", "🌊",
            "🌿", "🎵", "🌲", "🎧", "🍂",
            "☕", "☕", "🌬️", "✨", "😌", "☕"
    };

    // R.raw resource id for each track, matched by index.
    private final int[] trackResIds = {
            R.raw.sleepy_rain, R.raw.fireplace_holiday, R.raw.white_noise,
            R.raw.ocean_wave_loops, R.raw.ocean_wave, R.raw.nature,
            R.raw.winds_flute, R.raw.nature_music, R.raw.ambient_electronica,
            R.raw.autumn_leaves, R.raw.coffee_shop, R.raw.morning_coffee_shop,
            R.raw.breezy_escape, R.raw.dream_of_peace, R.raw.chill,
            R.raw.coffee_shop_music
    };

    // Background tint applied to each track's button when selected.
    private final int[] trackColors = {
            0xFF1565C0, 0xFFBF360C, 0xFF455A64, 0xFF0277BD, 0xFF01579B,
            0xFF2E7D32, 0xFF00695C, 0xFF1B5E20, 0xFF4A148C, 0xFFE65100,
            0xFF4E342E, 0xFF3E2723, 0xFF006064, 0xFF311B92, 0xFF1A237E, 0xFF4E342E
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMusicBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // MainActivity holds the service binding, so we just borrow it.
        if (getActivity() instanceof MainActivity) {
            musicService = ((MainActivity) getActivity()).getMusicService();
        }

        setupTrackButtons();
        setupPlayPauseButton();
        setupVolumeControl();
        setupTimer();

        // If music is already playing when the user opens this tab, sync
        // the UI to match the service's state.
        if (musicService != null) {
            currentTrackIndex = musicService.getCurrentTrackIndex();
            isPlaying = musicService.isPlaying();
            if (currentTrackIndex != -1) {
                highlightSelectedButton();
                binding.textNowPlaying.setText(
                        trackEmojis[currentTrackIndex] + "  "
                                + trackNames[currentTrackIndex]);
            }
        }

        updatePlayPauseButton();
        if (currentTrackIndex == -1) {
            binding.textNowPlaying.setText("Select a sound to begin");
        }
        binding.textSessionTime.setText("00:00");
        binding.textVolumeLabel.setText("Volume: 70%");
    }

    /**
     * Wires up all 16 track buttons. Each button shows the emoji and
     * track name, and tapping it selects that track.
     */
    private void setupTrackButtons() {
        View[] buttons = {
                binding.btnTrack0,  binding.btnTrack1,  binding.btnTrack2,
                binding.btnTrack3,  binding.btnTrack4,  binding.btnTrack5,
                binding.btnTrack6,  binding.btnTrack7,  binding.btnTrack8,
                binding.btnTrack9,  binding.btnTrack10, binding.btnTrack11,
                binding.btnTrack12, binding.btnTrack13, binding.btnTrack14,
                binding.btnTrack15
        };

        for (int i = 0; i < buttons.length; i++) {
            ((android.widget.TextView) buttons[i]).setText(
                    trackEmojis[i] + "\n" + trackNames[i]);
            final int index = i;
            buttons[i].setOnClickListener(v -> selectTrack(index));
        }
    }

    /**
     * Handles a track button tap. Tapping the already playing track is
     * treated as play/pause; tapping a different track switches to it
     * and restarts the session timer.
     */
    private void selectTrack(int index) {
        if (currentTrackIndex == index) {
            togglePlayPause();
            return;
        }
        currentTrackIndex = index;
        highlightSelectedButton();
        binding.textNowPlaying.setText(
                trackEmojis[index] + "  " + trackNames[index]);

        if (musicService != null) {
            musicService.playTrack(index, trackResIds[index], trackNames[index]);
            isPlaying = true;
            resetTimer();
            startTimer();
        }
        updatePlayPauseButton();
    }

    /**
     * Asks the service to flip its play/pause state and syncs the
     * session timer and play button accordingly.
     */
    private void togglePlayPause() {
        if (musicService == null) return;
        musicService.togglePlayPause();
        isPlaying = musicService.isPlaying();
        if (isPlaying) startTimer(); else stopTimer();
        updatePlayPauseButton();
    }

    /**
     * Wires up the bottom play/pause button. If no track is picked yet,
     * shows a hint instead of trying to start playback.
     */
    private void setupPlayPauseButton() {
        binding.btnPlayPause.setOnClickListener(v -> {
            if (currentTrackIndex == -1) {
                binding.textNowPlaying.setText("← Select a sound first");
                return;
            }
            togglePlayPause();
        });
    }

    /**
     * Wires up the volume slider. Default is 70%; changes are pushed to
     * the service immediately so the volume change is heard live.
     */
    private void setupVolumeControl() {
        binding.seekBarVolume.setProgress(70);
        binding.seekBarVolume.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar,
                                                  int progress, boolean fromUser) {
                        binding.textVolumeLabel.setText("Volume: " + progress + "%");
                        if (musicService != null) {
                            musicService.setVolume(progress / 100f);
                        }
                    }
                    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                });
    }

    /**
     * Sets up the per second session timer Runnable. The Runnable is
     * stored in a field so start/stop/reset can post and cancel it.
     */
    private void setupTimer() {
        timerRunnable = new Runnable() {
            @Override public void run() {
                secondsElapsed++;
                int minutes = secondsElapsed / 60;
                int seconds = secondsElapsed % 60;
                if (binding != null) {
                    binding.textSessionTime.setText(
                            String.format("%02d:%02d", minutes, seconds));
                }
                timerHandler.postDelayed(this, 1000);
            }
        };
    }

    private void startTimer() { timerHandler.postDelayed(timerRunnable, 1000); }
    private void stopTimer()  { timerHandler.removeCallbacks(timerRunnable); }

    /**
     * Stops the timer and zeroes the displayed session time. Called
     * when the user switches to a different track.
     */
    private void resetTimer() {
        stopTimer();
        secondsElapsed = 0;
        if (binding != null) binding.textSessionTime.setText("00:00");
    }

    /**
     * Updates the play/pause glyph and the state label to match the
     * current playback state.
     */
    private void updatePlayPauseButton() {
        if (isPlaying) {
            binding.btnPlayPause.setText("⏸");
            binding.textPlayState.setText("Now Playing");
        } else {
            binding.btnPlayPause.setText("▶");
            binding.textPlayState.setText(
                    currentTrackIndex == -1 ? "Not Playing" : "Paused");
        }
    }

    /**
     * Tints the selected track button with its theme color and resets
     * the others to the default dark background.
     */
    private void highlightSelectedButton() {
        View[] buttons = {
                binding.btnTrack0,  binding.btnTrack1,  binding.btnTrack2,
                binding.btnTrack3,  binding.btnTrack4,  binding.btnTrack5,
                binding.btnTrack6,  binding.btnTrack7,  binding.btnTrack8,
                binding.btnTrack9,  binding.btnTrack10, binding.btnTrack11,
                binding.btnTrack12, binding.btnTrack13, binding.btnTrack14,
                binding.btnTrack15
        };
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setBackgroundColor(
                    i == currentTrackIndex ? trackColors[i] : 0xFF2C2C3E);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopTimer();
        binding = null;
    }
}