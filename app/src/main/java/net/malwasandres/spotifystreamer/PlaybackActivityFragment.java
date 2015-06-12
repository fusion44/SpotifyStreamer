package net.malwasandres.spotifystreamer;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


/**
 * A placeholder fragment containing a simple view.
 */
public class PlaybackActivityFragment extends Fragment {
    private static final String LOG_TAG = PlaybackActivityFragment.class.getSimpleName();
    private SpotifyService mSpotify;
    private MediaPlayer mMediaPlayer;
    private ArrayList<TrackModel> mTracks;

    @OnClick(R.id.skipPreviousButton)
    public void onSkipPreviousClick() {
        Toast.makeText(getActivity(), "Not implemented yet!", Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.skipNextButton)
    public void onSkipNextClick() {
        Toast.makeText(getActivity(), "Not implemented yet!", Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.playButton)
    public void onPlayButton() {
        if(mMediaPlayer.isPlaying()) {
            mPlayButton.setImageResource(R.drawable.ic_av_play_arrow);
            mMediaPlayer.pause();
        } else {
            mPlayButton.setImageResource(R.drawable.ic_av_pause);
            mMediaPlayer.start();
        }
    }

    @InjectView(R.id.playButton)
    ImageButton mPlayButton;

    @InjectView(R.id.playbackStartTextView)
    TextView mStartTextView;

    @InjectView(R.id.playbackEndTextView)
    TextView mEndTextView;

    @InjectView(R.id.playbackBandNameTextView)
    TextView mBandNameTextView;

    @InjectView(R.id.playbackAlbumNameTextView)
    TextView mAlbumNameTextView;

    @InjectView(R.id.playbackAlbumImageView)
    ImageView mAlbumImageView;

    public PlaybackActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSpotify = new SpotifyApi().getService();
        String trackId = getActivity().getIntent().getStringExtra(
                getString(R.string.key_spotify_playback_track_single));
        mSpotify.getTrack(trackId, new Callback<Track>() {
            @Override
            public void success(final Track track, Response response) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadFinished(track);
                    }
                });
            }

            @Override
            public void failure(final RetrofitError error) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onSpotifyError(error.getMessage());
                    }
                });
            }
        });

        mTracks = getActivity().getIntent().getParcelableArrayListExtra(
                getString(R.string.key_spotify_playback_list));
    }

    private void onSpotifyError(String message) {
        Log.e(LOG_TAG, message);
    }

    @Override
    public void onStop() {
        if(mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        super.onStop();
    }

    private void loadFinished(Track track) {
        mBandNameTextView.setText(track.artists.get(0).name);
        mAlbumNameTextView.setText(track.album.name);
        if(track.album.images.size() > 0) {
            Picasso.with(getActivity())
                    .load(track.album.images.get(0).url)
                    .into(mAlbumImageView);
        }

        mMediaPlayer = MediaPlayer.create(getActivity(), Uri.parse(track.preview_url));
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.start();

        int duration = mMediaPlayer.getDuration();
        mEndTextView.setText(String.valueOf(duration / 1000));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_playback, container, false);
        ButterKnife.inject(this, v);
        return v;
    }
}
