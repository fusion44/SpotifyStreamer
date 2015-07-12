package net.malwasandres.spotifystreamer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
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
    static final int MSG_CURRENT_TRACK = 1;
    static final int MSG_PLAYBACK_START = 2;
    static final int MSG_PLAYBACK_STOP = 3;
    static final int MSG_PLAYBACK_POSITION = 4;
    static final int MSG_TRACK_LENGTH = 5;
    private static final String LOG_TAG = PlaybackActivityFragment.class.getSimpleName();
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());
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

    /**
     * Messenger for communicating with service.
     */
    Messenger mServiceMessenger = null;
    /**
     * Flag indicating whether we have called bind on the service.
     */
    boolean mIsBound;
    private boolean mUseTwoPaneLayout = false;
    private ArrayList<TrackModel> mTracks;
    private TrackModel mCurrentTrack;
    private ShareActionProvider mShareActionProvider;
    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            mServiceMessenger = new Messenger(service);

            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                Message msg = Message.obtain(null, PlaybackService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mServiceMessenger.send(msg);

                Bundle b = new Bundle();
                b.putParcelable(
                        getString(R.string.key_spotify_playback_track_single), mCurrentTrack);
                b.putParcelableArrayList(getString(R.string.key_track_list), mTracks);
                msg = Message.obtain(null, PlaybackService.MSG_SET_TRACKS);
                msg.setData(b);
                mServiceMessenger.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mServiceMessenger = null;
        }
    };
    private Context mContext;

    public PlaybackActivityFragment() {
    }

    @OnClick(R.id.skipPreviousButton)
    public void onSkipPreviousClick() {
        Message msg = Message.obtain(null, PlaybackService.MSG_PREVIOUS_TRACK);
        msg.replyTo = mMessenger;
        try {
            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @OnClick(R.id.skipNextButton)
    public void onSkipNextClick() {
        Message msg = Message.obtain(null, PlaybackService.MSG_NEXT_TRACK);
        msg.replyTo = mMessenger;
        try {
            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @OnClick(R.id.playButton)
    public void onPlayButton() {
        Message msg = Message.obtain(null, PlaybackService.MSG_TOGGLE_PLAY);
        msg.replyTo = mMessenger;
        try {
            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        /*if (true) mPlayButton.setImageResource(R.drawable.ic_av_pause);
        else */
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
        mContext = getActivity().getApplicationContext();
        int trackId;
        Bundle b;

        if (mUseTwoPaneLayout) b = getArguments();
        else b = getActivity().getIntent().getExtras();

        mTracks = b.getParcelableArrayList(getActivity().getString(R.string.key_track_list));
        trackId = b.getInt(getString(R.string.key_spotify_playback_track_single), -1);
        if (trackId == -1) mCurrentTrack = mTracks.get(0);
        else mCurrentTrack = mTracks.get(trackId);
        setRetainInstance(true);

        // use the share action provider in action bar only in phone mode
        // in tablet mode its embedded in the playback layout
        if (!mUseTwoPaneLayout) setHasOptionsMenu(true);

        doBindService();
    }

    public static PlaybackActivityFragment newInstance(Bundle args) {
        PlaybackActivityFragment frag = new PlaybackActivityFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onStart() {
        if (mServiceMessenger != null) {
            try {
                Message msg = Message.obtain(null,
                        PlaybackService.MSG_HIDE_NOTIFICATION);
                msg.replyTo = mMessenger;
                mServiceMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        super.onStart();
    }

    @Override
    public void onPause() {
        if (mServiceMessenger != null) {
            try {
                Message msg = Message.obtain(null,
                        PlaybackService.MSG_SHOW_NOTIFICATION);
                msg.replyTo = mMessenger;
                mServiceMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        Intent i = new Intent(mContext, PlaybackService.class);
        i.putExtra(getString(R.string.key_track_list), mTracks);
        i.putExtra(getString(R.string.key_spotify_playback_track_single), mCurrentTrack);
        mContext.startService(i);
        mContext.bindService(new Intent(mContext, PlaybackService.class),
                mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (mServiceMessenger != null) {
                try {
                    Message msg = Message.obtain(null,
                            PlaybackService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mServiceMessenger.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }

            mContext.unbindService(mConnection);
            mContext.stopService(new Intent(mContext, PlaybackService.class));
            mIsBound = false;
        }
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

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            final Message msg = Message.obtain(null, PlaybackService.MSG_SET_PLAYBACK_POS);
            Bundle b = new Bundle();
            b.putInt(getString(R.string.key_playback_position), progress);
            msg.setData(b);
            try {
                mServiceMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
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
        setupUi();
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

    /**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CURRENT_TRACK:
                    msg.getData().setClassLoader(TrackModel.class.getClassLoader());
                    mCurrentTrack = msg.getData().getParcelable(
                            getString(R.string.key_spotify_playback_track_single));
                    if (!mUseTwoPaneLayout) doShareTrack(createShareIntent());
                    setupUi();
                    break;
                case MSG_PLAYBACK_START:
                    mPlayButton.setImageResource(R.drawable.ic_av_pause);
                    break;
                case MSG_PLAYBACK_STOP:
                    mPlayButton.setImageResource(R.drawable.ic_av_play_arrow);
                    break;
                case MSG_PLAYBACK_POSITION:
                    int seconds = (msg.getData().getInt(getString(R.string.key_playback_position)) + 500) / 1000;
                    mPositionSeekbar.setProgress(seconds);
                    mCurrentPositionTextView.setText(getTimeString(seconds));
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
