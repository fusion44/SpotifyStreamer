package net.malwasandres.spotifystreamer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    private static final String MAIN_ACTIVITY_FRAGMENT_KEY = "key_main_activity_fragment";
    private MenuItem mMenuItem;

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case PlaybackService.ACTION_SERVICE_CREATED:
                    mMenuItem.setVisible(true);
                    break;
                case PlaybackService.ACTION_SERVICE_DESTROYED:
                    mMenuItem.setVisible(false);
                    break;
                default:
                    break;
            }
        }
    };
    private boolean mUseTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUseTwoPane = findViewById(R.id.two_pane_track_list_container) != null;

        MainActivityFragment mainFragment = (MainActivityFragment) getSupportFragmentManager()
                .findFragmentByTag(MAIN_ACTIVITY_FRAGMENT_KEY);

        if (mainFragment == null) {
            mainFragment = new MainActivityFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.search_container, mainFragment, MAIN_ACTIVITY_FRAGMENT_KEY)
                    .commit();
        }

        mainFragment.setUseTwoPaneLayout(mUseTwoPane);
    }

    @Override protected void onStart() {
        IntentFilter filter = new IntentFilter();
        if (mUseTwoPane) filter.addAction(PlaybackService.ACTION_SERVICE_CREATED);
        filter.addAction(PlaybackService.ACTION_SERVICE_DESTROYED);
        registerReceiver(receiver, filter);
        super.onStart();
    }

    @Override protected void onPause() {
        unregisterReceiver(receiver);
        super.onStop();
    }

    @Override protected void onResume() {
        if (mMenuItem != null && !mUseTwoPane && PlaybackService.isRunning()) {
            mMenuItem.setVisible(true);
        }
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mMenuItem = menu.findItem(R.id.action_open_playback);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_preferences) {
            Intent i = new Intent(this, StreamerPreferenceActivity.class);
            startActivity(i);
        } else if (id == R.id.action_open_playback) {
            if (mUseTwoPane) {
                PlaybackActivityFragment frag = PlaybackActivityFragment.newInstance(new Bundle());
                frag.setUseTwoPaneLayout(true);
                frag.show(getSupportFragmentManager(), getString(R.string.key_fragment_playback));
            } else {
                Intent i = new Intent(this, PlaybackActivity.class);
                startActivity(i);
            }
        }

        return super.onOptionsItemSelected(item);
    }
}
