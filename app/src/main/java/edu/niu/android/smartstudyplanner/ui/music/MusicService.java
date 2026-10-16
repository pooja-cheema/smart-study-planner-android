package edu.niu.android.smartstudyplanner.ui.music;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import edu.niu.android.smartstudyplanner.R;
import edu.niu.android.smartstudyplanner.ui.MainActivity;

public class MusicService extends Service {

    private static final String CHANNEL_ID = "music_channel";
    private static final int NOTIF_ID = 1001;

    // Binder handed back from onBind so clients can call service methods directly.
    private final IBinder binder = new MusicBinder();

    /**
     * Binder returned to clients that bind to this service. Exposes the
     * service instance so the fragment and activity can call methods on it.
     */
    public class MusicBinder extends Binder {
        public MusicService getService() { return MusicService.this; }
    }

    private MediaPlayer mediaPlayer;

    // Index of the track currently loaded into the player, or -1 if none.
    private int currentTrackIndex = -1;
    private String currentTrackName = "";

    // Mirrors mediaPlayer's playing state for cheap reads.
    private boolean isPlaying = false;

    // Last set volume, kept here so it survives recreating the player.
    private float volume = 0.7f;

    /**
     * Callback fired whenever playback state changes. MusicFragment and
     * MainActivity register one of these so the main UI and the bottom
     * mini player stay in sync with the service.
     */
    public interface MusicStateListener {
        void onStateChanged(boolean playing, int trackIndex, String trackName);
    }
    private MusicStateListener stateListener;

    /**
     * Registers a listener for playback state changes. Only one listener
     * is held at a time; the latest registration wins.
     */
    public void setStateListener(MusicStateListener listener) {
        this.stateListener = listener;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // START_STICKY so Android restarts the service if it gets killed
        // while the user wants music to keep playing.
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    // Public API called by MusicFragment

    /**
     * Releases any current player, builds a new one for the given track
     * resource, starts looped playback, and posts the foreground
     * notification so the service can keep running in the background.
     */
    public void playTrack(int index, int resId, String trackName) {
        currentTrackIndex = index;
        currentTrackName = trackName;
        releasePlayer();

        try {
            mediaPlayer = MediaPlayer.create(this, resId);
            if (mediaPlayer == null) return;

            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build());
            mediaPlayer.setLooping(true);
            mediaPlayer.setVolume(volume, volume);
            mediaPlayer.start();
            isPlaying = true;

            showNotification(trackName);
            notifyListener(trackName);

        } catch (Exception e) {
            isPlaying = false;
        }
    }

    /**
     * Pauses playback if a track is currently playing.
     */
    public void pause() {
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer.pause();
            isPlaying = false;
            notifyListener(getCurrentTrackName());
            updateNotification(getCurrentTrackName());
        }
    }

    /**
     * Resumes playback if a track is loaded but paused.
     */
    public void resume() {
        if (mediaPlayer != null && !isPlaying) {
            mediaPlayer.start();
            isPlaying = true;
            notifyListener(getCurrentTrackName());
            updateNotification(getCurrentTrackName());
        }
    }

    /**
     * Flips between playing and paused based on the current state.
     */
    public void togglePlayPause() {
        if (isPlaying) pause(); else resume();
    }

    /**
     * Sets player volume on a 0.0 to 1.0 scale and stores it so the
     * value survives swapping to a new track.
     */
    public void setVolume(float vol) {
        volume = vol;
        if (mediaPlayer != null) mediaPlayer.setVolume(vol, vol);
    }

    public boolean isPlaying()        { return isPlaying; }
    public int getCurrentTrackIndex() { return currentTrackIndex; }

    /**
     * Placeholder track name used in pause/resume notifications. The real
     * name is supplied via playTrack and used directly in showNotification.
     */
    public String getCurrentTrackName() {
        return currentTrackName;
    }

    // Notification

    /**
     * Creates the music notification channel. Required on Android 8+;
     * a no op on older versions.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Study Music",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Background study music playback");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    /**
     * Promotes the service to the foreground with the playback notification.
     * Called when a new track starts so Android keeps the service alive.
     */
    private void showNotification(String trackName) {
        startForeground(NOTIF_ID, buildNotification(trackName));
    }

    /**
     * Refreshes the visible notification, e.g. after pause/resume so the
     * play/pause glyph in the notification matches the player.
     */
    private void updateNotification(String trackName) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(NOTIF_ID, buildNotification(trackName));
    }

    /**
     * Builds the playback notification. Tapping it opens MainActivity.
     * The notification is ongoing while playing (so it can't be swiped
     * away) and dismissable when paused.
     */
    private Notification buildNotification(String trackName) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Study Music")
                .setContentText(isPlaying ? "▶ " + trackName : "⏸ " + trackName)
                .setSmallIcon(R.drawable.ic_schedule)
                .setContentIntent(pi)
                .setOngoing(isPlaying)
                .setSilent(true)
                .build();
    }

    /**
     * Pushes the latest playback state to the registered listener if any.
     */
    private void notifyListener(String trackName) {
        if (stateListener != null) {
            stateListener.onStateChanged(isPlaying, currentTrackIndex, trackName);
        }
    }

    // Cleanup

    /**
     * Stops and releases the current MediaPlayer, freeing audio resources.
     * Called before swapping tracks and on service shutdown.
     */
    private void releasePlayer() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        isPlaying = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }
}