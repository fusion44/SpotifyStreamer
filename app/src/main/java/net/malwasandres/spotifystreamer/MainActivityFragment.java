package net.malwasandres.spotifystreamer;

import android.content.Intent;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnTextChanged;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment implements ArtistsAdapter.ArtistClickListener {
    public static String LOG_TAG = MainActivityFragment.class.getSimpleName();
    private static final long API_CALL_RATE_LIMIT = 750;
    private static int MAX_SEARCH_RESULTS = 10;

    private SpotifyService mSpotify;
    private long mLastApiCall = 0;

    @InjectView(R.id.searchBoxDeco)
    TextInputLayout mSearchBoxDeco;
    @InjectView(R.id.artistSearchResultList)
    RecyclerView mSearchResultList;
    private ArtistsAdapter mAdapter;


    @OnTextChanged(R.id.searchBox)
    public void onTextChanged(CharSequence query) {
        long timeSinceLastApiCall = System.currentTimeMillis() - mLastApiCall;
        Log.i(LOG_TAG, String.valueOf(timeSinceLastApiCall));

        if (query.length() > 2) {
            mLastApiCall = System.currentTimeMillis();

            mSpotify.searchArtists(query.toString(), new Callback<ArtistsPager>() {
                @Override
                public void success(final ArtistsPager artistsPager, Response response) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (artistsPager.artists.items.size() > MAX_SEARCH_RESULTS) {
                                loadFinished(artistsPager.artists.items.subList(0, MAX_SEARCH_RESULTS));
                            } else {
                                loadFinished(artistsPager.artists.items);
                            }
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

    private void loadFinished(List<Artist> items) {
        mAdapter.clear();
        if(items.size() == 0) {
            mSearchBoxDeco.setError(getActivity().getString(R.string.no_artist_found));
        } else {
            mAdapter.replaceArtists(items);
            mSearchBoxDeco.setErrorEnabled(false);
        }
    }

    private void onSpotifyError(String message) {
        Log.e(LOG_TAG, message);
    }

    public MainActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSpotify = new SpotifyApi().getService();
        mLastApiCall = System.currentTimeMillis();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.inject(this, v);
        mSearchResultList.setLayoutManager(
                new LinearLayoutManager(this.getActivity(), LinearLayoutManager.VERTICAL, false));

        mAdapter = new ArtistsAdapter(new ArrayList<Artist>());
        mAdapter.setOnArtistClickListener(this);
        mSearchResultList.setAdapter(mAdapter);
        return v;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ButterKnife.reset(this);
    }

    @Override
    public void onArtistClick(Artist artist) {
        Intent i = new Intent(getActivity(), TopTenTrackActivity.class);
        i.putExtra(getActivity().getString(R.string.key_spotify_artist_id), artist.id);
        startActivity(i);
    }
}
