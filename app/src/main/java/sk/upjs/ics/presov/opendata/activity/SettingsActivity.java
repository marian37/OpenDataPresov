package sk.upjs.ics.presov.opendata.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import sk.upjs.ics.presov.opendata.Defaults;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        long datasetId = getIntent().getExtras().getLong(Defaults.DATASET_ID);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, newInstanceSettingsFragment(datasetId))
                .commit();
    }

    private static SettingsFragment newInstanceSettingsFragment(long id) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putLong(Defaults.DATASET_ID, id);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
