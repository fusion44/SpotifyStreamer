package net.malwasandres.spotifystreamer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;


public class TopTenTrackActivity extends AppCompatActivity {
    private static final String TOP_TEN_TRACK_FRAGMENT_KEY = "TOP_TEN_TRACK_FRAGMENT_KEY";
    private MenuItem mMenuItem;

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case PlaybackService.ACTION_SERVICE_DESTROYED:
                    mMenuItem.setVisible(false);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top_ten_track);

        TopTenTrackActivityFragment topTenFragment = (TopTenTrackActivityFragment) getSupportFragmentManager()
                .findFragmentByTag(TOP_TEN_TRACK_FRAGMENT_KEY);

        if (topTenFragment == null) {
            topTenFragment = new TopTenTrackActivityFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.top_ten_container, topTenFragment, TOP_TEN_TRACK_FRAGMENT_KEY)
                    .commit();
        }


    }

    @Override protected void onStart() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(PlaybackService.ACTION_SERVICE_DESTROYED);
        registerReceiver(receiver, filter);
        super.onStart();
    }

    @Override protected void onPause() {
        unregisterReceiver(receiver);
        super.onStop();
    }

    @Override protected void onResume() {
        if (mMenuItem != null) mMenuItem.setVisible(true);
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_top_ten_track, menu);
        mMenuItem = menu.findItem(R.id.action_open_playback);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_open_playback) {
            Intent i = new Intent(this, PlaybackActivity.class);
            startActivity(i);
        } else if (id == android.R.id.home) {
            super.onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
