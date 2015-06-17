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
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    private MediaPlayer mMediaplayer;
    private ArrayList<TrackModel> mTracks;
    private TrackModel mCurrentTrack;
    private int mCurrentTrackPosition;
    private int mTrackDuration;

    public PlaybackService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        mMediaplayer = new MediaPlayer();
        mMediaplayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaplayer.setOnPreparedListener(this);
        mMediaplayer.setOnCompletionListener(this);
        mMediaplayer.setOnErrorListener(this);
    }

    public void playTrack(TrackModel track) {
        mCurrentTrack = track;
        startPlay(mCurrentTrack);
    }

    public void playTrack(int listPosition) {
        mCurrentTrack = mTracks.get(listPosition);
        startPlay(mCurrentTrack);

        Message m = Message.obtain(null, PlaybackActivityFragment.MSG_CURRENT_TRACK);
        Bundle b = new Bundle();
        b.putParcelable(getString(R.string.key_spotify_playback_track_single), mCurrentTrack);
        m.setData(b);
        try {
            mMessenger.send(m);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void startPlay(TrackModel track) {
        try {
            mMediaplayer.reset();
            mMediaplayer.setDataSource(track.previewUrl);
            mMediaplayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mMediaplayer.start();
        mTrackDuration = mMediaplayer.getDuration();
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
        if (mMediaplayer != null) {
            mMediaplayer.stop();
            mMediaplayer.release();
            mMediaplayer = null;
        }
        return false;
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {

    }

    public void onPreviousTrack() {
        final int index = mTracks.indexOf(mCurrentTrack);
        if (index == 0) playTrack(mTracks.size() - 1);
        else playTrack(index - 1);
    }

    public void onNextTrack() {
        final int index = mTracks.indexOf(mCurrentTrack);
        if (index == mTracks.size() - 1) playTrack(0);
        else playTrack(index + 1);
    }

    public boolean onTogglePlay() {
        if (mMediaplayer.isPlaying()) {
            mMediaplayer.pause();
            return false; // not playing -> Fragment must set button drawable accordingly
        } else {
            mMediaplayer.start();
            return true; // playing something
        }
    }
}
