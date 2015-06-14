package net.malwasandres.spotifystreamer;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

// TODO: Add all countries Spotify supports
public class StreamerPreferenceActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new StreamerPreferenceFragment()).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            super.onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class StreamerPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.streamer_prefs);
        }
    }
}