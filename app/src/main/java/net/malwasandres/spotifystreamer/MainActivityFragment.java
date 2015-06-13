package net.malwasandres.spotifystreamer;

import android.content.Intent;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment implements ArtistsAdapter.ArtistClickListener {
    public static String LOG_TAG = MainActivityFragment.class.getSimpleName();
    private static int MAX_SEARCH_RESULTS = 10;

    private SpotifyService mSpotify;

    @InjectView(R.id.searchBoxDeco)
    TextInputLayout mSearchBoxDeco;
    @InjectView(R.id.artistSearchResultList)
    RecyclerView mSearchResultList;
    private ArtistsAdapter mAdapter;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(
                getActivity().getString(R.string.key_artist_search_result), mAdapter.getArtists());
    }

    @Override
    public void onResume() {
        super.onResume();

        mSearchBoxDeco.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence query, int i, int i1, int i2) {
                // TODO: do not flood Spotifys servers with useless requests while user is typing.
                // Next version of retrofit will have a method for canceling pending requests
                // https://github.com/square/retrofit/issues/297
                if (query.length() > 2) {
                    mSpotify.searchArtists(query.toString(), new Callback<ArtistsPager>() {
                        @Override
                        public void success(final ArtistsPager artistsPager, Response response) {
                            final ArrayList<ArtistModel> artistList =
                                    new ArrayList<>(artistsPager.artists.items.size());

                            for (int i = 0; i < artistsPager.artists.items.size(); i++) {
                                artistList.add(new ArtistModel(artistsPager.artists.items.get(i)));

                                if (i > MAX_SEARCH_RESULTS) break;
                            }

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    loadFinished(artistList);
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
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private void loadFinished(ArrayList<ArtistModel> items) {
        mAdapter.clear();
        if (items.size() == 0) {
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.inject(this, v);
        mSearchResultList.setLayoutManager(
                new LinearLayoutManager(this.getActivity(), LinearLayoutManager.VERTICAL, false));

        mAdapter = new ArtistsAdapter(new ArrayList<ArtistModel>());
        mAdapter.setOnArtistClickListener(this);
        mSearchResultList.setAdapter(mAdapter);

        if (savedInstanceState != null) {
            ArrayList<ArtistModel> m = savedInstanceState.getParcelableArrayList(
                    getActivity().getString(R.string.key_artist_search_result));

            if (m != null) mAdapter.replaceArtists(m);
        }
        return v;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ButterKnife.reset(this);
    }

    @Override
    public void onArtistClick(ArtistModel artist) {
        Intent i = new Intent(getActivity(), TopTenTrackActivity.class);
        i.putExtra(getActivity().getString(R.string.key_spotify_artist_id), artist.id);
        startActivity(i);
    }
}
