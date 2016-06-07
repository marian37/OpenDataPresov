package sk.upjs.ics.presov.opendata.activity;

import android.content.AsyncQueryHandler;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;

import java.util.LinkedHashMap;
import java.util.Map;

import sk.upjs.ics.presov.opendata.Defaults;
import sk.upjs.ics.presov.opendata.R;

import static sk.upjs.ics.presov.opendata.Defaults.ALL_COLUMNS_WITH_ROWID;
import static sk.upjs.ics.presov.opendata.Defaults.COLUMN_PREFERENCE_DEFAULT_VALUE;
import static sk.upjs.ics.presov.opendata.Defaults.NO_COOKIE;
import static sk.upjs.ics.presov.opendata.Defaults.NO_GROUP_BY;
import static sk.upjs.ics.presov.opendata.Defaults.NO_SELECTION;
import static sk.upjs.ics.presov.opendata.Defaults.NO_SELECTION_ARGS;
import static sk.upjs.ics.presov.opendata.provider.DatasetContentProvider.ColumnsContentUri;
import static sk.upjs.ics.presov.opendata.provider.Provider.Columns.KEY;
import static sk.upjs.ics.presov.opendata.provider.Provider.Columns.VALUE;

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int COLUMNS_TOKEN = 0;

    private static final String COLUMNS_MAP = "columns_map";

    private long datasetId;

    private Map<String, String> columns;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        datasetId = getArguments().getLong(Defaults.DATASET_ID);

        addPreferencesFromResource(R.xml.settings);
        PreferenceScreen preferenceScreen = getPreferenceScreen();

        preferenceScreen.removeAll();

        final ListPreference preferenceFirst = new ListPreference(getActivity());
        preferenceFirst.setKey(getString(R.string.dataset_column_first, datasetId));
        preferenceFirst.setTitle(R.string.preference_main_column_title);
        preferenceFirst.setEntries(new CharSequence[]{""});
        preferenceFirst.setEntryValues(new CharSequence[]{""});

        preferenceScreen.addPreference(preferenceFirst);

        final ListPreference preferenceSecond = new ListPreference(getActivity());
        preferenceSecond.setKey(getString(R.string.dataset_column_second, datasetId));
        preferenceSecond.setTitle(R.string.preference_secondary_column_title);
        preferenceSecond.setEntries(new CharSequence[]{""});
        preferenceSecond.setEntryValues(new CharSequence[]{""});

        preferenceScreen.addPreference(preferenceSecond);

        if (savedInstanceState != null) {
            columns = (Map<String, String>) savedInstanceState.getSerializable(COLUMNS_MAP);
            setPreferenceEntries(columns, preferenceFirst, preferenceSecond);
            initializeSummaries();
        } else {
            AsyncQueryHandler handler = new AsyncQueryHandler(getActivity().getContentResolver()) {
                @Override
                protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                    columns = new LinkedHashMap<>();
                    while (cursor.moveToNext()) {
                        columns.put(cursor.getString(cursor.getColumnIndex(KEY)), cursor.getString(cursor.getColumnIndex(VALUE)));
                    }
                    setPreferenceEntries(columns, preferenceFirst, preferenceSecond);

                    initializeSummaries();
                }
            };
            handler.startQuery(COLUMNS_TOKEN, NO_COOKIE, ColumnsContentUri(datasetId), ALL_COLUMNS_WITH_ROWID, NO_SELECTION, NO_SELECTION_ARGS, NO_GROUP_BY);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        LinkedHashMap<String, String> serializableColumns = (LinkedHashMap<String, String>) columns;
        outState.putSerializable(COLUMNS_MAP, serializableColumns);
    }

    private void initializeSummaries() {
        SharedPreferences sharedPreferences = getPreferenceManager().getDefaultSharedPreferences(getActivity());
        for (String key : sharedPreferences.getAll().keySet()) {
            if (getString(R.string.dataset_column_first, datasetId).equals(key)
                    || getString(R.string.dataset_column_second, datasetId).equals(key)) {
                onSharedPreferenceChanged(sharedPreferences, key);
            }
        }
    }

    private void setPreferenceEntries(Map<String, String> columns, ListPreference preferenceFirst, ListPreference preferenceSecond) {
        int size = columns.size();
        CharSequence[] columnEntries = columns.values().toArray(new CharSequence[size]);
        CharSequence[] columnEntryValues = columns.keySet().toArray(new CharSequence[size]);
        preferenceFirst.setEntries(columnEntries);
        preferenceFirst.setEntryValues(columnEntryValues);
        preferenceSecond.setEntries(columnEntries);
        preferenceSecond.setEntryValues(columnEntryValues);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (columns != null) {
            findPreference(key).setSummary(columns.get(sharedPreferences.getString(key, COLUMN_PREFERENCE_DEFAULT_VALUE)));
        } else {
            Log.d(getClass().getName(), "Nepodarilo sa vypísať aktuálne nastavené hodnoty.");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }
}

