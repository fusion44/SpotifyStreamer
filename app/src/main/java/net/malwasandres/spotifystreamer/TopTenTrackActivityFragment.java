package net.malwasandres.spotifystreamer;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


/**
 * A placeholder fragment containing a simple view.
 */
public class TopTenTrackActivityFragment extends Fragment implements TrackAdapter.TrackClickListener {
    private static final String LOG_TAG = TopTenTrackActivityFragment.class.getSimpleName();

    @InjectView(R.id.trackList)
    RecyclerView mTrackList;
    private TrackAdapter mAdapter;

    public TopTenTrackActivityFragment() {
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putParcelableArrayList(
                getActivity().getString(R.string.key_top_ten_tracks), mAdapter.getTracks());
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            ArrayList<TrackModel> m = savedInstanceState.getParcelableArrayList(
                    getActivity().getString(R.string.key_top_ten_tracks));

            if (m != null) mAdapter.replaceTracks(m);
            setArtistTitle();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SpotifyService spotify = new SpotifyApi().getService();
        String artistId = getActivity().getIntent().getStringExtra(
                getActivity().getString(R.string.key_spotify_artist_id));

        if (artistId != null) {
            Map<String, Object> countryCode = new HashMap<>(1);
            countryCode.put("country", Locale.getDefault().getCountry());

            setRetainInstance(true);

            spotify.getArtistTopTrack(artistId, countryCode, new Callback<Tracks>() {
                @Override
                public void success(final Tracks tracks, Response response) {
                    final ArrayList<TrackModel> trackModels = new ArrayList<>(tracks.tracks.size());
                    for (Track track : tracks.tracks) {
                        TrackModel tm = new TrackModel(track);
                        trackModels.add(tm);
                    }

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadFinished(trackModels);
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
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_top_ten_track, container, false);
        ButterKnife.inject(this, v);
        mTrackList.setLayoutManager(
                new LinearLayoutManager(this.getActivity(), LinearLayoutManager.VERTICAL, false));

        mAdapter = new TrackAdapter(new ArrayList<TrackModel>());
        mAdapter.setOnTrackClickListener(this);
        mTrackList.setAdapter(mAdapter);

        return v;

    }

    private void onSpotifyError(String message) {
        Log.i(LOG_TAG, message);
    }

    private void loadFinished(ArrayList<TrackModel> tracks) {
        mAdapter.replaceTracks(tracks);
        setArtistTitle();
    }

    private void setArtistTitle() {
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(mAdapter.getTracks().get(0).artistName);
        }
    }

    @Override
    public void onTrackClick(TrackModel track) {
        Intent i = new Intent(getActivity(), PlaybackActivity.class);
        i.putExtra(getString(R.string.key_spotify_playback_track_single), track.id);
        i.putExtra(getString(R.string.key_spotify_playback_list), mAdapter.getTracks());
        startActivity(i);
    }
}
