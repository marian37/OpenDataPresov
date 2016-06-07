package sk.upjs.ics.presov.opendata.activity;

import android.app.LoaderManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import sk.upjs.ics.presov.opendata.DatasetInfoDao;
import sk.upjs.ics.presov.opendata.Defaults;
import sk.upjs.ics.presov.opendata.R;
import sk.upjs.ics.presov.opendata.model.DatasetInfo;
import sk.upjs.ics.presov.opendata.provider.DatasetContentProvider;
import sk.upjs.ics.presov.opendata.provider.Provider;

import static sk.upjs.ics.presov.opendata.Defaults.COLUMN_PREFERENCE_DEFAULT_VALUE;
import static sk.upjs.ics.presov.opendata.Defaults.COLUMN_PREFIX;
import static sk.upjs.ics.presov.opendata.Defaults.DATASET_ID;
import static sk.upjs.ics.presov.opendata.Defaults.DEFAULT_COLUMN_VALUE;

public class DatasetActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int DATASET_LOADER_ID = 0;

    public static final String ROW_ID = "rowid";

    private static final String QUERY = "query";

    private SimpleCursorAdapter adapter;

    private long datasetInfoId;

    private String query;

    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dataset);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final int datasetId = getIntent().getExtras().getInt(DATASET_ID);
        final DatasetInfo datasetInfo = new DatasetInfoDao(this).getDatasetInfo(datasetId);

        if (datasetInfo != null) {
            setTitle(datasetInfo.getName());
        }

        datasetInfoId = datasetInfo.getId();

        if (savedInstanceState != null) {
            datasetInfoId = savedInstanceState.getLong(DATASET_ID);
            query = savedInstanceState.getString(QUERY);
        }

        ListView datasetListView = (ListView) findViewById(R.id.datasetListView);

        int[] columnIds = getColumnIndexPreferences();

        datasetListView.setAdapter(initializeAdapter(columnIds));
        datasetListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = (Cursor) parent.getItemAtPosition(position);
                long rowId = cursor.getLong(cursor.getColumnIndex(ROW_ID));

                Intent intent = new Intent(DatasetActivity.this, DetailActivity.class);
                intent.putExtra(DATASET_ID, datasetInfo.getId());
                intent.putExtra(ROW_ID, rowId);

                startActivity(intent);
            }
        });

        handleIntent(getIntent());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(QUERY, searchView.getQuery().toString());
        outState.putLong(DATASET_ID, datasetInfoId);
    }

    private int[] getColumnIndexPreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(DatasetActivity.this);
        String valueFirst = preferences.getString(getString(R.string.dataset_column_first, datasetInfoId), COLUMN_PREFERENCE_DEFAULT_VALUE);
        String valueSecond = preferences.getString(getString(R.string.dataset_column_second, datasetInfoId), COLUMN_PREFERENCE_DEFAULT_VALUE);
        int first = DEFAULT_COLUMN_VALUE;
        int second = DEFAULT_COLUMN_VALUE;
        if (valueFirst.startsWith(COLUMN_PREFIX)) {
            first = Integer.parseInt(valueFirst.substring(COLUMN_PREFIX.length()));
        }
        if (valueSecond.startsWith(COLUMN_PREFIX)) {
            second = Integer.parseInt(valueSecond.substring(COLUMN_PREFIX.length()));
        }
        return new int[]{first, second};
    }

    @Override
    protected void onResume() {
        super.onResume();
        final int[] columnIds = getColumnIndexPreferences();
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (view.getId() == android.R.id.text1) {
                    TextView textView = (TextView) view;
                    textView.setText(cursor.getString(cursor.getColumnIndex(Provider.Rows.columnName(columnIds[0]))));
                    return true;
                }
                if (view.getId() == android.R.id.text2) {
                    TextView textView = (TextView) view;
                    textView.setText(cursor.getString(cursor.getColumnIndex(Provider.Rows.columnName(columnIds[1]))));
                    return true;
                }
                return false;
            }
        });
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_dataset, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        MenuItemCompat.setOnActionExpandListener(searchMenuItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                Bundle bundle = new Bundle();
                bundle.putLong(DATASET_ID, datasetInfoId);
                getLoaderManager().restartLoader(DATASET_LOADER_ID, bundle, DatasetActivity.this);
                return true;
            }
        });

        if (this.query != null && !this.query.isEmpty()) {
            searchMenuItem.expandActionView();
            searchView.setQuery(this.query, true);
            searchView.clearFocus();
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.action_preferences:
                Intent intent = new Intent(this, SettingsActivity.class);
                intent.putExtra(DATASET_ID, datasetInfoId);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private ListAdapter initializeAdapter(int[] columnIds) {
        String[] from = {Provider.Rows.columnName(columnIds[0]), Provider.Rows.columnName(columnIds[1])};
        int[] to = {android.R.id.text1, android.R.id.text2};
        this.adapter = new SimpleCursorAdapter(this, android.R.layout.two_line_list_item, Defaults.NO_CURSOR, from, to, Defaults.NO_FLAGS);

        return this.adapter;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        long datasetId = args.getLong(DATASET_ID);
        String query = args.getString(QUERY, Defaults.NO_SELECTION);
        CursorLoader loader = new CursorLoader(this);
        loader.setProjection(Defaults.ALL_COLUMNS_WITH_ROWID);
        loader.setUri(DatasetContentProvider.RowsContentUri(datasetId));
        if (query != Defaults.NO_SELECTION) {
            String selection = Provider.Rows.rowsTableName(datasetId) + " MATCH " + DatabaseUtils.sqlEscapeString(query + "*");
            loader.setSelection(selection);
        }
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        TextView datasetFoundTextView = (TextView) findViewById(R.id.datasetFoundTextView);
        datasetFoundTextView.setText(getString(R.string.dataset_found_text, cursor.getCount()));
        this.adapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        this.adapter.swapCursor(Defaults.NO_CURSOR);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        Bundle bundle = new Bundle();
        bundle.putLong(DATASET_ID, datasetInfoId);
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            bundle.putString(QUERY, query);
        }
        getLoaderManager().restartLoader(DATASET_LOADER_ID, bundle, this);
    }
}
