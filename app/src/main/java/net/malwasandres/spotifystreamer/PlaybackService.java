package net.malwasandres.spotifystreamer;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;

import java.io.IOException;
import java.util.ArrayList;


// http://code.tutsplus.com/tutorials/create-a-music-player-on-android-song-playback--mobile-22778
public class PlaybackService extends Service implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnInfoListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {
    private static final String LOG_TAG = PlaybackActivityFragment.class.getSimpleName();

    private final IBinder mBinder = new TrackBinder();
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

    public void setTrackList(ArrayList<TrackModel> list) {
        mTracks = list;
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
        return mBinder;
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

    public void onPreviousTrackClick() {
        final int index = mTracks.indexOf(mCurrentTrack);
        if (index == 0) playTrack(mTracks.size() - 1);
        else playTrack(index - 1);
    }

    public void onNextTrackClick() {
        final int index = mTracks.indexOf(mCurrentTrack);
        if (index == mTracks.size() - 1) playTrack(0);
        else playTrack(index + 1);
    }

    public boolean onPlayClick() {
        if (mMediaplayer.isPlaying()) {
            mMediaplayer.pause();
            return false; // not playing -> Fragment must set button drawable accordingly
        } else {
            mMediaplayer.start();
            return true; // playing something
        }
    }

    public class TrackBinder extends Binder {
        PlaybackService getService() {
            return PlaybackService.this;
        }
    }
}
