package net.malwasandres.spotifystreamer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    private static final String MAIN_ACTIVITY_FRAGMENT_KEY = "key_main_activity_fragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        boolean twoPane = findViewById(R.id.two_pane_track_list_container) != null;

        MainActivityFragment mainFragment = (MainActivityFragment) getSupportFragmentManager()
                .findFragmentByTag(MAIN_ACTIVITY_FRAGMENT_KEY);

        if (mainFragment == null) {
            mainFragment = new MainActivityFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.search_container, mainFragment, MAIN_ACTIVITY_FRAGMENT_KEY)
                    .commit();
        }

        mainFragment.setUseTwoPaneLayout(twoPane);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
        }

        return super.onOptionsItemSelected(item);
    }
}
