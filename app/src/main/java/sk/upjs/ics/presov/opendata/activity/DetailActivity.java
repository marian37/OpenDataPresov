package sk.upjs.ics.presov.opendata.activity;

import android.content.AsyncQueryHandler;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spanned;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.LinkedHashMap;
import java.util.Map;

import sk.upjs.ics.presov.opendata.Defaults;
import sk.upjs.ics.presov.opendata.R;
import sk.upjs.ics.presov.opendata.provider.DatasetContentProvider;

import static sk.upjs.ics.presov.opendata.Defaults.ALL_COLUMNS;
import static sk.upjs.ics.presov.opendata.Defaults.NO_COOKIE;
import static sk.upjs.ics.presov.opendata.Defaults.NO_GROUP_BY;
import static sk.upjs.ics.presov.opendata.Defaults.NO_SELECTION;
import static sk.upjs.ics.presov.opendata.Defaults.NO_SELECTION_ARGS;
import static sk.upjs.ics.presov.opendata.provider.Provider.Columns.KEY;
import static sk.upjs.ics.presov.opendata.provider.Provider.Columns.VALUE;

public class DetailActivity extends AppCompatActivity {

    private static final int QUERY_COLUMNS_TOKEN = 0;

    private static final int QUERY_ROW_TOKEN = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Bundle extras = getIntent().getExtras();
        final long datasetId = extras.getLong(Defaults.DATASET_ID);
        final long rowId = extras.getLong(DatasetActivity.ROW_ID);

        final TextView detailTextView = (TextView) findViewById(R.id.detailTextView);

        AsyncQueryHandler columnHandler = new AsyncQueryHandler(getContentResolver()) {
            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                final Map<String, String> columns = new LinkedHashMap<>();
                while (cursor.moveToNext()) {
                    columns.put(cursor.getString(cursor.getColumnIndex(KEY)), cursor.getString(cursor.getColumnIndex(VALUE)));
                }
                cursor.close();
                AsyncQueryHandler rowHandler = new AsyncQueryHandler(getContentResolver()) {
                    @Override
                    protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                        cursor.moveToNext();
                        StringBuilder html = new StringBuilder();
                        for (Map.Entry<String, String> columnsEntry : columns.entrySet()) {
                            html.append("<b>")
                                    .append(columnsEntry.getValue())
                                    .append("</b>: ")
                                    .append(cursor.getString(cursor.getColumnIndex(columnsEntry.getKey())))
                                    .append("<br />");
                        }
                        Spanned text = Html.fromHtml(html.toString());
                        detailTextView.setText(text);
                    }
                };
                String selection = "rowid =" + rowId;
                rowHandler.startQuery(QUERY_ROW_TOKEN, NO_COOKIE, DatasetContentProvider.RowsContentUri(datasetId), ALL_COLUMNS, selection, NO_SELECTION_ARGS, NO_GROUP_BY);
            }
        };

        columnHandler.startQuery(QUERY_COLUMNS_TOKEN, NO_COOKIE, DatasetContentProvider.ColumnsContentUri(datasetId), ALL_COLUMNS, NO_SELECTION, NO_SELECTION_ARGS, NO_GROUP_BY);
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
