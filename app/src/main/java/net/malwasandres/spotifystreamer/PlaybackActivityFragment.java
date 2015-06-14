package net.malwasandres.spotifystreamer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.os.Bundle;
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


/**
 * A placeholder fragment containing a simple view.
 */
public class PlaybackActivityFragment extends Fragment {
    private static final String LOG_TAG = PlaybackActivityFragment.class.getSimpleName();

    PlaybackService mPlaybackService;
    Intent mPlaybackIntent;
    private Boolean mPlaybackServiceBound = false;

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

    }

    @InjectView(R.id.playButton)
    ImageButton mPlayButton;

    @InjectView(R.id.playbackStartTextView)
    TextView mStartTextView;

    @InjectView(R.id.playbackEndTextView)
    TextView mEndTextView;

    @InjectView(R.id.playbackBandNameTextView)
    TextView mArtistNameTextView;

    @InjectView(R.id.playbackAlbumNameTextView)
    TextView mAlbumNameTextView;

    @InjectView(R.id.playbackAlbumImageView)
    ImageView mAlbumImageView;

    private ArrayList<TrackModel> mTracks;
    private TrackModel mCurrentTrack;

    public PlaybackActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = getActivity().getIntent();
        mTracks = i.getParcelableArrayListExtra(getActivity().getString(R.string.key_track_list));
        int trackId = getActivity().getIntent().getIntExtra(
                getString(R.string.key_spotify_playback_track_single), -1);

        if (trackId == -1) mCurrentTrack = mTracks.get(0);
        else mCurrentTrack = mTracks.get(trackId);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mPlaybackIntent == null) {
            mPlaybackIntent = new Intent(getActivity(), PlaybackService.class);
            getActivity().bindService(mPlaybackIntent, playbackConnection, Context.BIND_AUTO_CREATE);
            getActivity().startService(mPlaybackIntent);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupUi();
    }

    private ServiceConnection playbackConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlaybackService.TrackBinder binder = (PlaybackService.TrackBinder) service;
            mPlaybackService = binder.getService();
            mPlaybackServiceBound = true;
            mPlaybackService.setTrackList(mTracks);
            mPlaybackService.playTrack(mCurrentTrack);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mPlaybackServiceBound = false;
        }
    };

    private void setupUi() {
        mArtistNameTextView.setText(mCurrentTrack.artistName);
        mAlbumNameTextView.setText(mCurrentTrack.albumName);
        if (mCurrentTrack.imageUrl != null && !mCurrentTrack.imageUrl.equals("")) {
            Picasso.with(getActivity())
                    .load(mCurrentTrack.imageUrl)
                    .into(mAlbumImageView);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_playback, container, false);
        ButterKnife.inject(this, v);
        return v;
    }
}
