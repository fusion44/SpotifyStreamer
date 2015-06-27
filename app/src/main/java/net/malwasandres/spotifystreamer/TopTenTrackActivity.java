package net.malwasandres.spotifystreamer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;


public class TopTenTrackActivity extends AppCompatActivity {
    private static final String TOP_TEN_TRACK_FRAGMENT_KEY = "TOP_TEN_TRACK_FRAGMENT_KEY";

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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_top_ten_track, menu);
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
