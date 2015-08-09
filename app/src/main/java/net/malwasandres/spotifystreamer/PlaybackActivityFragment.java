package net.malwasandres.spotifystreamer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.Optional;


/**
 * A placeholder fragment containing a simple view.
 */
public class PlaybackActivityFragment extends DialogFragment implements SeekBar.OnSeekBarChangeListener {
    private static final String LOG_TAG = PlaybackActivityFragment.class.getSimpleName();


    @InjectView(R.id.playButton)
    ImageButton mPlayButton;
    @InjectView(R.id.currentPositionTextView)
    TextView mCurrentPositionTextView;
    @InjectView(R.id.trackLengthTextView)
    TextView mTrackLengthTextView;
    @InjectView(R.id.playbackBandNameTextView)
    TextView mArtistNameTextView;
    @InjectView(R.id.playbackAlbumNameTextView)
    TextView mAlbumNameTextView;
    @InjectView(R.id.playbackTrackNameTextView)
    TextView mTrackNameTextView;
    @InjectView(R.id.playbackAlbumImageView)
    ImageView mAlbumImageView;
    @InjectView(R.id.seekBar)
    SeekBar mPositionSeekbar;
    @InjectView(R.id.shareTrackButton)
    @Optional // only available in two pane view
            ImageButton mShareTrackButton;


    private boolean mUseTwoPaneLayout = false;
    private ArrayList<TrackModel> mTracks;
    private TrackModel mCurrentTrack;
    private ShareActionProvider mShareActionProvider;

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            switch (action) {
                case PlaybackService.ACTION_SET_CURRENT_TRACK:
                    intent.getExtras().setClassLoader(TrackModel.class.getClassLoader());
                    mCurrentTrack = intent.getExtras().getParcelable(
                            getString(R.string.key_spotify_playback_track_single));
                    if (!mUseTwoPaneLayout) doShareTrack(createShareIntent());
                    setupUi();
                    break;
                case PlaybackService.ACTION_PLAYBACK_POSITION_OUT:
                    int seconds = (intent.getExtras().getInt(
                            getString(R.string.key_playback_position)) + 500) / 1000;
                    mPositionSeekbar.setProgress(seconds);
                    mCurrentPositionTextView.setText(getTimeString(seconds));
                    break;
                case PlaybackService.ACTION_PLAYBACK_STOPPED:
                    mPlayButton.setImageResource(R.drawable.ic_av_play_arrow);
                    break;
                case PlaybackService.ACTION_PLAYBACK_STARTED:
                    mPlayButton.setImageResource(R.drawable.ic_av_pause);
                    break;
                case PlaybackService.ACTION_GET_CURRENT_TRACK:
                    mTracks = intent.getParcelableArrayListExtra(getActivity().getString(R.string.key_track_list));
                    mCurrentTrack = intent.getParcelableExtra(getString(R.string.key_spotify_playback_track_single));
                    setupUi();
                default:
                    break;
            }
        }
    };

    public PlaybackActivityFragment() {
    }

    @OnClick(R.id.skipPreviousButton)
    public void onSkipPreviousClick() {
        sendActionToService(PlaybackService.ACTION_PREVIOUS);
    }

    @OnClick(R.id.skipNextButton)
    public void onSkipNextClick() {
        sendActionToService(PlaybackService.ACTION_NEXT);
    }

    @OnClick(R.id.playButton)
    public void onPlayButton() {
        sendActionToService(PlaybackService.ACTION_PLAY);
    }

    @OnClick(R.id.shareTrackButton)
    @Optional
    public void onShareTrackButton() {
        Intent i = createShareIntent();
        getActivity().startActivity(i);
    }

    public void setUseTwoPaneLayout(boolean useTwoPane) {
        mUseTwoPaneLayout = useTwoPane;
    }

    @Override
    public void onDestroyView() {
        // see here: http://stackoverflow.com/questions/14657490/how-to-properly-retain-a-dialogfragment-through-rotation
        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        super.onDestroyView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int trackId;
        Bundle b;

        if (mUseTwoPaneLayout) b = getArguments();
        else b = getActivity().getIntent().getExtras();

        if (!PlaybackService.isRunning()) {
            mTracks = b.getParcelableArrayList(getActivity().getString(R.string.key_track_list));
            trackId = b.getInt(getString(R.string.key_spotify_playback_track_single), -1);
            if (trackId == -1) mCurrentTrack = mTracks.get(0);
            else mCurrentTrack = mTracks.get(trackId);

            Intent i = new Intent(getActivity().getApplicationContext(), PlaybackService.class);
            i.setAction(PlaybackService.ACTION_SET_TRACK_LIST);
            i.putExtras(b);
            getActivity().getApplicationContext().startService(i);
        } else {
            sendActionToService(PlaybackService.ACTION_GET_CURRENT_TRACK);
        }

        setRetainInstance(true);

        // use the share action provider in action bar only in phone mode
        // in tablet mode its embedded in the playback layout
        if (!mUseTwoPaneLayout) setHasOptionsMenu(true);
    }

    public static PlaybackActivityFragment newInstance(Bundle args) {
        PlaybackActivityFragment frag = new PlaybackActivityFragment();
        frag.setArguments(args);
        return frag;
    }

    private void sendActionToService(String action) {
        if (!PlaybackService.isRunning()) {
            Bundle b = new Bundle();
            b.putParcelableArrayList(getActivity().getString(R.string.key_track_list), mTracks);
            b.putInt(getString(R.string.key_spotify_playback_track_single), mTracks.indexOf(mCurrentTrack));
            Intent i = new Intent(getActivity().getApplicationContext(), PlaybackService.class);
            i.setAction(PlaybackService.ACTION_SET_TRACK_LIST);
            i.putExtras(b);
            getActivity().getApplicationContext().startService(i);
        }

        Intent i = new Intent(getActivity().getApplicationContext(), PlaybackService.class);
        i.setAction(action);
        getActivity().startService(i);
    }

    private void setupUi() {
        mArtistNameTextView.setText(mCurrentTrack.artistName);
        mAlbumNameTextView.setText(mCurrentTrack.albumName);
        mTrackNameTextView.setText(mCurrentTrack.name);
        if (mCurrentTrack.imageUrl != null && !mCurrentTrack.imageUrl.equals("")) {
            Picasso.with(getActivity())
                    .load(mCurrentTrack.imageUrl)
                    .into(mAlbumImageView);
        }

        String timeString = getTimeString(mCurrentTrack.length / 1000);
        mTrackLengthTextView.setText(timeString);
        mPlayButton.setImageResource(R.drawable.ic_av_pause);
        mPositionSeekbar.setMax(mCurrentTrack.length / 1000);
        mPositionSeekbar.setProgress(0);
    }

    private String getTimeString(int seconds) {
        int minutes = seconds / 60;
        seconds = seconds - minutes * 60;
        if (seconds < 10) return minutes + ":0" + seconds;
        else return minutes + ":" + seconds;
    }

    @Override public void onStart() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(PlaybackService.ACTION_SET_CURRENT_TRACK);
        filter.addAction(PlaybackService.ACTION_PLAYBACK_POSITION_OUT);
        filter.addAction(PlaybackService.ACTION_PLAYBACK_STOPPED);
        filter.addAction(PlaybackService.ACTION_PLAYBACK_STARTED);
        getActivity().registerReceiver(receiver, filter);

        super.onStart();
    }

    @Override public void onPause() {
        try {
            getActivity().unregisterReceiver(receiver);
        } catch (Exception e) {
            Log.i(LOG_TAG, e.getMessage());
        }

        super.onPause();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            Intent i = new Intent(getActivity().getApplicationContext(), PlaybackService.class);
            i.setAction(PlaybackService.ACTION_SET_PLAYBACK_POSITION_IN);
            i.putExtra(getString(R.string.key_playback_position), progress);
            getActivity().startService(i);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_playback, container, false);
        ButterKnife.inject(this, v);
        mPositionSeekbar.setOnSeekBarChangeListener(this);
        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        getActivity().getMenuInflater().inflate(R.menu.menu_playback, menu);
        MenuItem item = menu.findItem(R.id.action_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
        super.onCreateOptionsMenu(menu, inflater);
    }

    private Intent createShareIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String subject = String.format(getResources().getString(R.string.share_track_text),
                mCurrentTrack.name, mCurrentTrack.artistName);
        shareIntent.putExtra(Intent.EXTRA_TEXT, subject + mCurrentTrack.previewUrl);
        return shareIntent;
    }

    public void doShareTrack(Intent shareIntent) {
        if (mShareActionProvider != null) mShareActionProvider.setShareIntent(shareIntent);
    }
}
