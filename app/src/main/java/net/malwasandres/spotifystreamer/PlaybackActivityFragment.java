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
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
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


/**
 * A placeholder fragment containing a simple view.
 */
public class PlaybackActivityFragment extends Fragment implements SeekBar.OnSeekBarChangeListener {
    private static final String LOG_TAG = PlaybackActivityFragment.class.getSimpleName();

    static final int MSG_CURRENT_TRACK = 1;
    static final int MSG_PLAYBACK_START = 2;
    static final int MSG_PLAYBACK_STOP = 3;
    static final int MSG_PLAYBACK_POSITION = 4;
    static final int MSG_TRACK_LENGTH = 5;

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

    @InjectView(R.id.seekBar)
    SeekBar mPositionSeekbar;

    private ArrayList<TrackModel> mTracks;
    private TrackModel mCurrentTrack;


    /**
     * Messenger for communicating with service.
     */
    Messenger mServiceMessenger = null;
    /**
     * Flag indicating whether we have called bind on the service.
     */
    boolean mIsBound;

    /**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CURRENT_TRACK:
                    msg.getData().setClassLoader(TrackModel.class.getClassLoader());
                    TrackModel t = msg.getData().getParcelable(
                            getString(R.string.key_spotify_playback_track_single));
                    mCurrentTrack = t;
                    setupUi();
                    break;
                case MSG_PLAYBACK_START:
                    mPlayButton.setImageResource(R.drawable.ic_av_pause);
                    break;
                case MSG_PLAYBACK_STOP:
                    mPlayButton.setImageResource(R.drawable.ic_av_play_arrow);
                    break;
                case MSG_PLAYBACK_POSITION:
                    int pos = msg.getData().getInt(getString(R.string.key_playback_position)) + 1000;
                    mPositionSeekbar.setProgress(pos / 1000);
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

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
    }

    @Override
    public void onResume() {
        super.onResume();
        doBindService();
    }

    @Override
    public void onPause() {
        super.onPause();
        doUnbindService();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupUi();
    }

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        Intent i = new Intent(getActivity(), PlaybackService.class);
        i.putExtra(getString(R.string.key_track_list), mTracks);
        i.putExtra(getString(R.string.key_spotify_playback_track_single), mCurrentTrack);
        getActivity().bindService(i, mConnection, Context.BIND_AUTO_CREATE);
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

            // Detach our existing connection.
            getActivity().unbindService(mConnection);
            mIsBound = false;
        }
    }

    private void setupUi() {
        mArtistNameTextView.setText(mCurrentTrack.artistName);
        mAlbumNameTextView.setText(mCurrentTrack.albumName);
        if (mCurrentTrack.imageUrl != null && !mCurrentTrack.imageUrl.equals("")) {
            Picasso.with(getActivity())
                    .load(mCurrentTrack.imageUrl)
                    .into(mAlbumImageView);
        }
        mEndTextView.setText(String.valueOf(mCurrentTrack.length / 1000));
        mPlayButton.setImageResource(R.drawable.ic_av_pause);
        mPositionSeekbar.setMax(mCurrentTrack.length / 1000);
        mPositionSeekbar.setProgress(0);
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

        return v;
    }
}
