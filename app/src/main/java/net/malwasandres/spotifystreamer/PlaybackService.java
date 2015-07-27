package net.malwasandres.spotifystreamer;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
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
public class PlaybackService extends IntentService implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnInfoListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {
    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_NEXT_TRACK = 3;
    static final int MSG_PREVIOUS_TRACK = 4;
    static final int MSG_TOGGLE_PLAY = 5;
    static final int MSG_SET_TRACKS = 6;
    static final int MSG_SET_PLAYBACK_POS = 7;

    public static final String ACTION_PLAY = "net.malwasandres.spotifystreamer.action_play";
    public static final String ACTION_PAUSE = "net.malwasandres.spotifystreamer.action_pause";
    public static final String ACTION_NEXT = "net.malwasandres.spotifystreamer.action_next";
    public static final String ACTION_PREVIOUS = "net.malwasandres.spotifystreamer.action_previous";
    public static final String ACTION_STOP = "net.malwasandres.spotifystreamer.action_stop";
    public static final String ACTION_START_PLAYBACK_ACTIVITY = "net.malwasandres.spotifystreamer.action_start_activity";

    private static final String LOG_TAG = PlaybackActivityFragment.class.getSimpleName();
    private static final int NOTIFICATION_ID = 1234;
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    private final ScheduledExecutorService mScheduler =
            Executors.newScheduledThreadPool(1);
    ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    private int mStartPlaybackFrom = -1;

    /*
     * Playback position updater
     */
    private ScheduledFuture<?> mScheduledFuture;
    private MediaPlayer mMediaPlayer;
    private ArrayList<TrackModel> mTracks;
    private TrackModel mCurrentTrack;
    private Bitmap mCurrentTrackBitmap = null;

    private final Handler mThreadHandler = new Handler();

    public PlaybackService() {
        super("net.malwasandres.spotifystreamer.PlaybackService");
    }

    public PlaybackService(String name) {
        super("name");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
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

        if (mStartPlaybackFrom != -1) mMediaPlayer.seekTo(mStartPlaybackFrom);
        mMediaPlayer.start();
        mCurrentTrack.length = mMediaPlayer.getDuration();

        Message m = Message.obtain(null, PlaybackActivityFragment.MSG_CURRENT_TRACK);
        Bundle b = new Bundle();
        b.putParcelable(getString(R.string.key_spotify_playback_track_single), mCurrentTrack);
        m.setData(b);
        try {
            mClients.get(0).send(m);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        buildNotification();
    }

    @Override
    public void onDestroy() {
        cleanup();
        super.onDestroy();
    }

    private void cleanup() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        clearNotification();
    }

    private void startScheduler() {
        stopScheduler();
        mScheduledFuture = mScheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {
                Message msg = Message.obtain(null, PlaybackActivityFragment.MSG_PLAYBACK_POSITION);
                Bundle b = new Bundle();
                b.putInt(getString(R.string.key_playback_position), mMediaPlayer.getCurrentPosition());
                msg.setData(b);
                try {
                    mClients.get(0).send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
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
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        switch (action) {
            case ACTION_PAUSE:
            case ACTION_PLAY:
                mThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onTogglePlay();
                    }
                });
                break;
            case ACTION_NEXT:
                mThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onNextTrack();
                    }
                });
                break;
            case ACTION_PREVIOUS:
                mThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onPreviousTrack();
                    }
                });
                break;
            case ACTION_START_PLAYBACK_ACTIVITY:
                Intent playbackIntent = new Intent(this, PlaybackActivity.class);
                playbackIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Bundle playbackBundle = new Bundle();
                playbackBundle.putInt(getString(R.string.key_spotify_playback_track_single),
                        mTracks.indexOf(mCurrentTrack));
                playbackBundle.putParcelableArrayList(getString(R.string.key_track_list), mTracks);
                playbackIntent.putExtras(playbackBundle);

                Intent topTenIntent = new Intent(this, TopTenTrackActivity.class);
                topTenIntent.putExtra(
                        getString(R.string.key_spotify_artist_id), mCurrentTrack.artistId);

                Intent mainIntent = new Intent(this, MainActivity.class);
                mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                try {
                    TaskStackBuilder builder = TaskStackBuilder.create(this);
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
                cleanup();
                stopSelf();
                break;
        }
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        stopScheduler();
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

            try {
                mClients.get(0).send(Message.obtain(null, PlaybackActivityFragment.MSG_PLAYBACK_STOP));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            mMediaPlayer.start();
            startScheduler();

            try {
                mClients.get(0).send(Message.obtain(null, PlaybackActivityFragment.MSG_PLAYBACK_START));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        buildNotification();
    }

    private void buildNotification() {
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
        notificationManager.cancel(NOTIFICATION_ID);
    }

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case MSG_NEXT_TRACK:
                    onNextTrack();
                    break;
                case MSG_PREVIOUS_TRACK:
                    onPreviousTrack();
                    break;
                case MSG_TOGGLE_PLAY:
                    onTogglePlay();
                    break;
                case MSG_SET_TRACKS:
                    Bundle data = msg.getData();
                    if (mTracks != null) mTracks.clear();
                    msg.getData().setClassLoader(TrackModel.class.getClassLoader());
                    mTracks = data.getParcelableArrayList(getString(R.string.key_track_list));
                    TrackModel t = data.getParcelable(
                            getString(R.string.key_spotify_playback_track_single));
                    playTrack(t);
                    mStartPlaybackFrom = -1;
                    break;
                case MSG_SET_PLAYBACK_POS:
                    int pos = msg.getData().getInt(getString(R.string.key_playback_position));
                    if (mMediaPlayer.isPlaying()) {
                        mMediaPlayer.seekTo(pos * 1000);
                    } else {
                        if (mCurrentTrack != null) startPlay(mCurrentTrack);
                        mStartPlaybackFrom = pos * 1000;
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
