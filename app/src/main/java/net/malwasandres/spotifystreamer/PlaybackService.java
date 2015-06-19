package net.malwasandres.spotifystreamer;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

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
        MediaPlayer.OnCompletionListener {
    private static final String LOG_TAG = PlaybackActivityFragment.class.getSimpleName();


    ArrayList<Messenger> mClients = new ArrayList<Messenger>();

    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_NEXT_TRACK = 3;
    static final int MSG_PREVIOUS_TRACK = 4;
    static final int MSG_TOGGLE_PLAY = 5;
    static final int MSG_SET_TRACKS = 6;
    static final int MSG_SET_PLAYBACK_POS = 7;
    private int mStartPlaybackFrom = -1;

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
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    /*
     * Playback position updater
     */

    private final ScheduledExecutorService mScheduler =
            Executors.newScheduledThreadPool(1);

    private ScheduledFuture<?> mScheduledFuture;

    private MediaPlayer mMediaPlayer;
    private ArrayList<TrackModel> mTracks;
    private TrackModel mCurrentTrack;

    public PlaybackService() {

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

    public void playTrack(TrackModel track) {
        mCurrentTrack = track;
        startPlay(mCurrentTrack);
    }

    public void playTrack(int listPosition) {
        mCurrentTrack = mTracks.get(listPosition);
        startPlay(mCurrentTrack);
    }

    private void startPlay(TrackModel track) {
        try {
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(track.previewUrl);
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
    public boolean onUnbind(Intent intent) {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        return false;
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
    }
}
