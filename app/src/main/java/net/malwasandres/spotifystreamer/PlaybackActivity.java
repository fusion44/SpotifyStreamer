package net.malwasandres.spotifystreamer;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;


public class PlaybackActivity extends AppCompatActivity {

    private static final String KEY_PLAYBACK_FRAGMENT = "KEY_PLAYBACK_FRAGMENT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        PlaybackActivityFragment mPlaybackActivity = (PlaybackActivityFragment) getSupportFragmentManager()
                .findFragmentByTag(KEY_PLAYBACK_FRAGMENT);

        if (mPlaybackActivity == null) {
            mPlaybackActivity = new PlaybackActivityFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.playback_container, mPlaybackActivity, KEY_PLAYBACK_FRAGMENT)
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_playback, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_preferences) {
            return true;
        } else if (id == android.R.id.home) {
            super.onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
