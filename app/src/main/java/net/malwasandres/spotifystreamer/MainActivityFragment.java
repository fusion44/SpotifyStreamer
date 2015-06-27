package net.malwasandres.spotifystreamer;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

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
public class MainActivityFragment extends Fragment implements ArtistListAdapter.ClickListener, TextWatcher {
    private static final String TOP_TEN_TRACK_FRAGMENT_KEY = "TOP_TEN_TRACK_FRAGMENT_KEY";
    public static String LOG_TAG = MainActivityFragment.class.getSimpleName();
    private static int MAX_SEARCH_RESULTS = 10;
    @InjectView(R.id.searchBoxDeco)
    TextInputLayout mSearchBoxDeco;
    @InjectView(R.id.searchBox)
    EditText mSearchBox;
    @InjectView(R.id.artistSearchResultList)
    RecyclerView mSearchResultList;
    private SpotifyService mSpotify;
    private ArtistListAdapter mAdapter;
    private boolean mUseTwoPaneLayout;
    TopTenTrackActivityFragment mTopTenFragment;

    public MainActivityFragment() {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(
                getActivity().getString(R.string.key_artist_search_result), mAdapter.getModels());
    }

    private void querySpotify(String query) {
        // TODO: do not flood Spotifys servers with useless requests while user is typing.
        // Next version of retrofit will have a method for canceling pending requests
        // https://github.com/square/retrofit/issues/297

        mSpotify.searchArtists(query, new Callback<ArtistsPager>() {
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

    private void loadFinished(ArrayList<ArtistModel> items) {
        mAdapter.clear();
        if (items.size() == 0) {
            setErrorState(true);
        } else {
            mAdapter.replaceModels(items);
            setErrorState(false);
        }
    }

    private void setErrorState(boolean error) {
        if (error) {
            mSearchBoxDeco.setError(getActivity().getString(R.string.no_artist_found));
            mAdapter.clear();
        } else {
            mSearchBoxDeco.setErrorEnabled(false);
        }
    }

    private void onSpotifyError(String message) {
        Log.e(LOG_TAG, message);
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

        mAdapter = new ArtistListAdapter();
        mAdapter.setOnItemClickListener(this);
        mSearchResultList.setAdapter(mAdapter);

        if (savedInstanceState != null) {
            ArrayList<ArtistModel> m = savedInstanceState.getParcelableArrayList(
                    getActivity().getString(R.string.key_artist_search_result));

            if (m != null) mAdapter.replaceModels(m);
        }
        return v;
    }

    @Override
    public void onPause() {
        super.onPause();
        mSearchBox.removeTextChangedListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSearchBox.addTextChangedListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ButterKnife.reset(this);
    }

    public void setUseTwoPaneLayout(boolean useTwoPaneLayout) {
        this.mUseTwoPaneLayout = useTwoPaneLayout;
    }

    @Override
    public void onItemClick(BaseViewModel model) {
        ArtistModel artist = (ArtistModel) model;
        Bundle b = new Bundle();
        b.putString(getActivity().getString(R.string.key_spotify_artist_id), artist.id);

        if (mUseTwoPaneLayout) {
            android.support.v4.app.FragmentManager fm = getActivity().getSupportFragmentManager();
            mTopTenFragment = new TopTenTrackActivityFragment();
            mTopTenFragment.setUseTwoPaneLayout(mUseTwoPaneLayout);
            mTopTenFragment.setArguments(b);

            fm.beginTransaction()
                    .replace(R.id.two_pane_track_list_container, mTopTenFragment, TOP_TEN_TRACK_FRAGMENT_KEY)
                    .commitAllowingStateLoss(); // not sure about this one.
        } else {
            Intent i = new Intent(getActivity(), TopTenTrackActivity.class);
            i.putExtras(b);
            startActivity(i);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence query, int i, int i1, int i2) {
        if (query.length() > 2) {
            querySpotify(query.toString());
        } else {
            setErrorState(true);
        }

    }

    @Override
    public void afterTextChanged(Editable editable) {

    }
}
