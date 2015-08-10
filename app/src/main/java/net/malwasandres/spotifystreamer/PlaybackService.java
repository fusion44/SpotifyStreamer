package net.malwasandres.spotifystreamer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


// http://code.tutsplus.com/tutorials/create-a-music-player-on-android-song-playback--mobile-22778
public class PlaybackService extends Service implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnInfoListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener,
        AudioManager.OnAudioFocusChangeListener {

    public static final String ACTION_PLAY = "net.malwasandres.spotifystreamer.action_play";
    public static final String ACTION_PAUSE = "net.malwasandres.spotifystreamer.action_pause";
    public static final String ACTION_NEXT = "net.malwasandres.spotifystreamer.action_next";
    public static final String ACTION_PREVIOUS = "net.malwasandres.spotifystreamer.action_previous";
    public static final String ACTION_STOP = "net.malwasandres.spotifystreamer.action_stop";
    public static final String ACTION_START_PLAYBACK_ACTIVITY = "net.malwasandres.spotifystreamer.action_start_activity";
    public static final String ACTION_SET_TRACK_LIST = "net.malwasandres.spotifystreamer.action_set_track_list";
    public static final String ACTION_SET_PLAYBACK_POSITION_IN = "net.malwasandres.spotifystreamer.action_playback_position_in";
    public static final String ACTION_GET_CURRENT_TRACK = "net.malwasandres.spotifystreamer.action_get_current_track";
    public static final String ACTION_SHOW_NOTIFICATION = "net.malwasandres.spotifystreamer.action_show_notification";
    public static final String ACTION_HIDE_NOTIFICATION = "net.malwasandres.spotifystreamer.action_hide_notification";

    // actions going out to PlaybackActivity
    public static final String ACTION_PLAYBACK_POSITION_OUT = "net.malwasandres.spotifystreamer.action_playback_position_out";
    public static final String ACTION_SET_CURRENT_TRACK = "net.malwasandres.spotifystreamer.action_current_track";
    public static final String ACTION_PLAYBACK_STOPPED = "net.malwasandres.spotifystreamer.action_playback_stopped";
    public static final String ACTION_PLAYBACK_STARTED = "net.malwasandres.spotifystreamer.action_playback_started";

    public static final String ACTION_SERVICE_CREATED = "net.malwasandres.spotifystreamer.action_service_created";
    public static final String ACTION_SERVICE_DESTROYED = "net.malwasandres.spotifystreamer.action_service_destroyed";

    private static final String LOG_TAG = PlaybackService.class.getSimpleName();
    private static final int NOTIFICATION_ID = 1234;

    private final ScheduledExecutorService mScheduler =
            Executors.newScheduledThreadPool(1);
    private int mStartPlaybackFrom = -1;

    private static boolean IS_RUNNING;
    private MediaSessionCompat mMediaSessionCompat;

    public static boolean isRunning() {
        return IS_RUNNING;
    }

    /*
     * Playback position updater
     */
    private ScheduledFuture<?> mScheduledFuture;
    private MediaPlayer mMediaPlayer;
    private ArrayList<TrackModel> mTracks;
    private TrackModel mCurrentTrack;
    private Bitmap mCurrentTrackBitmap = null;

    @Override
    public void onCreate() {
        super.onCreate();
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        if (result != AudioManager.AUDIOFOCUS_GAIN) {
            // TODO: notify playback fragment of the failure
            Log.e(LOG_TAG, "Failed to gain audio focus");
            stopSelf();
        } else {
            Log.d(LOG_TAG, "Audio focus gained");
        }

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);

        mMediaSessionCompat = new MediaSessionCompat(getApplicationContext(), "MediaSession", null, null);
        mMediaSessionCompat.setActive(true);

        PlaybackStateCompat playbackStateCompat = new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PAUSED, 0, 1.0f)
                .build();
        mMediaSessionCompat.setPlaybackState(playbackStateCompat);
        mMediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        sendBroadcast(new Intent(ACTION_SERVICE_CREATED));

        IS_RUNNING = true;
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            return super.onStartCommand(intent, flags, startId);
        }

        String action = intent.getAction();
        Bundle data = intent.getExtras();

        if (action.equals(ACTION_SET_TRACK_LIST) && data != null) {
            if (mTracks != null) mTracks.clear();
            data.setClassLoader(TrackModel.class.getClassLoader());
            mTracks = data.getParcelableArrayList(getString(R.string.key_track_list));

            TrackModel t;
            int trackId = data.getInt(getString(R.string.key_spotify_playback_track_single), -1);
            if (trackId == -1) t = mTracks.get(0);
            else t = mTracks.get(trackId);

            playTrack(t);
            mStartPlaybackFrom = -1;
        } else {
            switch (action) {
                case ACTION_PAUSE:
                case ACTION_PLAY:
                    onTogglePlay();
                    break;
                case ACTION_NEXT:
                    onNextTrack();
                    break;
                case ACTION_PREVIOUS:
                    onPreviousTrack();
                    break;
                case ACTION_START_PLAYBACK_ACTIVITY:
                    Intent playbackIntent = new Intent(getApplicationContext(), PlaybackActivity.class);
                    playbackIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    Bundle playbackBundle = new Bundle();
                    playbackBundle.putInt(getString(R.string.key_spotify_playback_track_single),
                            mTracks.indexOf(mCurrentTrack));
                    playbackBundle.putParcelableArrayList(getString(R.string.key_track_list), mTracks);
                    playbackIntent.putExtras(playbackBundle);

                    Intent topTenIntent = new Intent(getApplicationContext(), TopTenTrackActivity.class);
                    topTenIntent.putExtra(
                            getString(R.string.key_spotify_artist_id), mCurrentTrack.artistId);

                    Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
                    mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    try {
                        TaskStackBuilder builder = TaskStackBuilder.create(getApplicationContext());
                        builder.addParentStack(MainActivity.class);
                        builder.addNextIntent(mainIntent);
                        builder.addParentStack(TopTenTrackActivity.class);
                        builder.addNextIntent(topTenIntent);
                        builder.addParentStack(PlaybackActivity.class);
                        builder.addNextIntent(playbackIntent);
                        builder.startActivities();
                    } catch (Exception e) {
                        Log.e(LOG_TAG, e.toString());
                    }
                    break;
                case ACTION_STOP:
                    stopSelf();
                    break;
                case ACTION_SET_PLAYBACK_POSITION_IN:
                    int pos = intent.getIntExtra(getString(R.string.key_playback_position), 0);
                    seekTo(pos * 1000);
                    break;
                case ACTION_GET_CURRENT_TRACK:
                    Intent i = new Intent(ACTION_SET_CURRENT_TRACK);
                    i.putExtra(getString(R.string.key_spotify_playback_track_single), mCurrentTrack);
                    i.putExtra(getString(R.string.key_track_list), mTracks);
                    sendBroadcast(i);
                    break;
                case ACTION_SHOW_NOTIFICATION:
                case ACTION_HIDE_NOTIFICATION:
                    buildNotification();
                    break;
                default:
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void seekTo(int i) {
        mMediaPlayer.seekTo(i);
    }

    public void playTrack(int listPosition) {
        playTrack(mTracks.get(listPosition));
    }

    public void playTrack(TrackModel track) {
        mCurrentTrack = track;

        Picasso.with(this)
                .load(mCurrentTrack.imageUrl)
                .into(new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        mCurrentTrackBitmap = bitmap;
                        buildNotification();
                    }

                    @Override
                    public void onBitmapFailed(Drawable errorDrawable) {

                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {

                    }
                });

        startPlay(mCurrentTrack);
    }

    private void startPlay(TrackModel track) {
        try {
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(track.previewUrl);
            // uncomment this to play the national anthem of Norway :-)
            // for testing the player with a clip longer than 30 seconds
            //mMediaPlayer.setDataSource("http://www.noiseaddicts.com/samples_1w72b820/4239.mp3");
            mMediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        startScheduler();

        if (mStartPlaybackFrom != -1) seekTo(mStartPlaybackFrom);
        mMediaPlayer.start();
        mCurrentTrack.length = mMediaPlayer.getDuration();

        Intent i = new Intent(ACTION_SET_CURRENT_TRACK);
        i.putExtra(getString(R.string.key_spotify_playback_track_single), mCurrentTrack);
        sendBroadcast(i);

        buildNotification();
    }

    @Override
    public void onDestroy() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        stopScheduler();
        clearNotification();
        IS_RUNNING = false;
        sendBroadcast(new Intent(ACTION_SERVICE_DESTROYED));

        super.onDestroy();
    }

    private void startScheduler() {
        stopScheduler();
        mScheduledFuture = mScheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {
                Intent i = new Intent(ACTION_PLAYBACK_POSITION_OUT);
                i.putExtra(getString(R.string.key_playback_position), mMediaPlayer.getCurrentPosition());
                sendBroadcast(i);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    @Override public IBinder onBind(Intent intent) {
        return null;
    }

    private void stopScheduler() {
        if (mScheduledFuture != null && !mScheduledFuture.isCancelled()) {
            mScheduledFuture.cancel(true);
        }
    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
        return false;
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        Log.e(LOG_TAG, "An error occurred while loading media");
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        stopSelf();
    }

    public void onPreviousTrack() {
        final int index = mTracks.indexOf(mCurrentTrack);
        mStartPlaybackFrom = -1;
        if (index == 0) playTrack(mTracks.size() - 1);
        else playTrack(index - 1);
    }

    public void onNextTrack() {
        final int index = mTracks.indexOf(mCurrentTrack);
        mStartPlaybackFrom = -1;
        if (index == mTracks.size() - 1) playTrack(0);
        else playTrack(index + 1);
    }

    public void onTogglePlay() {
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            stopScheduler();
            sendBroadcast(new Intent(ACTION_PLAYBACK_STOPPED));
        } else {
            mMediaPlayer.start();
            startScheduler();
            sendBroadcast(new Intent(ACTION_PLAYBACK_STARTED));
        }

        buildNotification();
    }

    private void buildNotification() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean showNotification = prefs.getBoolean(getString(R.string.key_show_notification), true);

        if (android.os.Build.VERSION.SDK_INT > 20) {
            MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder()
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, mCurrentTrack.albumName)
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, mCurrentTrack.albumName)
                    .putString(MediaMetadata.METADATA_KEY_TITLE, mCurrentTrack.name)
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, mCurrentTrack.length)
                    .putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, 1);
            if (showNotification) {
                builder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, mCurrentTrackBitmap);
            }

            mMediaSessionCompat.setMetadata(builder.build());
        }

        Intent upIntent = new Intent(this, PlaybackActivity.class);

        PendingIntent pendingIntent =
                TaskStackBuilder.create(this)
                        .addNextIntentWithParentStack(upIntent)
                        .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setLargeIcon(mCurrentTrackBitmap)
                .setSmallIcon(R.drawable.ic_av_pause)
                .setContentTitle(mCurrentTrack.name)
                .setContentText(mCurrentTrack.albumName)
                .setContentInfo(mCurrentTrack.artistName)
                .setContentIntent(pendingIntent);

        // react to swipe to dismiss event ( stops service )
        Intent deleteIntent = new Intent(getApplicationContext(), PlaybackService.class);
        deleteIntent.setAction(ACTION_STOP);
        PendingIntent deletePendingIntent = PendingIntent.getService(getApplicationContext(), 1, deleteIntent, 0);
        builder.setDeleteIntent(deletePendingIntent);

        builder.addAction(newAction(android.R.drawable.ic_media_previous, "", ACTION_PREVIOUS));
        if (mMediaPlayer.isPlaying()) {
            builder.addAction(newAction(android.R.drawable.ic_media_pause, "", ACTION_PAUSE));
        } else {
            builder.addAction(newAction(android.R.drawable.ic_media_play, "", ACTION_PLAY));
        }
        builder.addAction(newAction(android.R.drawable.ic_media_next, "", ACTION_NEXT));

        if (!showNotification) {
            if (android.os.Build.VERSION.SDK_INT > 20) {
                builder.setPriority(NotificationCompat.PRIORITY_MIN);
                builder.setVisibility(NotificationCompat.VISIBILITY_SECRET);
            } else {
                builder.setPriority(NotificationCompat.PRIORITY_MIN);
            }
        }

        Notification notification = builder.build();
        startForeground(NOTIFICATION_ID, notification);
    }

    private NotificationCompat.Action newAction(int icon, String title, String intentAction) {
        Intent intent = new Intent(getApplicationContext(), PlaybackService.class);
        intent.setAction(intentAction);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
        return new NotificationCompat.Action.Builder(icon, title, pendingIntent).build();
    }

    private void clearNotification() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        stopForeground(true);
        //notificationManager.cancel(NOTIFICATION_ID);
    }

    @Override public void onAudioFocusChange(int i) {
        // TODO: react to focus events
    }
}
