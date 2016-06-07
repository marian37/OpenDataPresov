package sk.upjs.ics.presov.opendata;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sk.upjs.ics.presov.opendata.model.Dataset;
import sk.upjs.ics.presov.opendata.model.DatasetInfo;
import sk.upjs.ics.presov.opendata.model.Row;
import sk.upjs.ics.presov.opendata.provider.DatasetContentProvider;
import sk.upjs.ics.presov.opendata.provider.Provider.Columns;

import static sk.upjs.ics.presov.opendata.Defaults.NO_ARGUMENTS;
import static sk.upjs.ics.presov.opendata.Defaults.SHARED_PREFERENCES_DATASET_INFO;
import static sk.upjs.ics.presov.opendata.Defaults.XML_PARSER_INPUT_ENCODING;
import static sk.upjs.ics.presov.opendata.provider.DatasetContentProvider.CREATE_TABLE_COLUMNS_METHOD;
import static sk.upjs.ics.presov.opendata.provider.DatasetContentProvider.CREATE_TABLE_ROWS_METHOD;

public class DownloadDatasetAsyncTask extends AsyncTask<DatasetInfo, Void, Boolean> {

    private static final String NAMESPACE = null;

    private static final String ROW = "row";

    private static final String COLUMN = "column";

    private static final String DS = "ds";

    private static final String NAME = "name";

    private static final String COL_ID = "col_id";

    private final WeakReference<RelativeLayout> relativeLayoutWeakReference;

    private final WeakReference<Context> contextWeakReference;

    private final Context appContext;

    private DatasetInfo datasetInfo;

    public DownloadDatasetAsyncTask(Context context, RelativeLayout relativeLayout) {
        relativeLayoutWeakReference = new WeakReference<>(relativeLayout);
        contextWeakReference = new WeakReference<>(context);
        appContext = context.getApplicationContext();
    }

    @Override
    protected Boolean doInBackground(DatasetInfo... params) {
        datasetInfo = params[0];
        HttpURLConnection connection;
        try {
            long id = datasetInfo.getId();

            URL url = new URL(datasetInfo.getUrlString());
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            InputStream is = connection.getInputStream();

            Dataset dataset = parse(is, datasetInfo);

            if (!insertIntoContentProvider(dataset)) {
                return false;
            }

            String date = new SimpleDateFormat("dd.MM.yyyy").format(new Date());

            if (contextWeakReference != null) {
                final Context context = contextWeakReference.get();
                if (context != null) {
                    SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_DATASET_INFO, Context.MODE_PRIVATE);
                    if (!sharedPreferences.edit().putString(DatasetInfoDao.datePreferenceKey(id), date).commit()) {
                        Log.w(getClass().getName(), "Nepodarilo sa uložiť dátum stiahnutia datasetu.");
                        return false;
                    }
                    SharedPreferences columnPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                    if (!columnPreferences.edit()
                            .putString(appContext.getString(R.string.dataset_column_first, id), Defaults.COLUMN_PREFERENCE_DEFAULT_VALUE)
                            .putString(appContext.getString(R.string.dataset_column_second, id), Defaults.COLUMN_PREFERENCE_DEFAULT_VALUE)
                            .commit()) {
                        Log.w(getClass().getName(), "Nepodarilo sa uložiť defaultné stĺpce pre zobrazenie datasetu.");
                        return false;
                    }
                }
            }

            datasetInfo.setDate(date);

            return true;
        } catch (XmlPullParserException | IOException e) {
            Log.e(getClass().getName(), e.getMessage(), e);
        }

        return false;
    }

    private boolean insertIntoContentProvider(Dataset dataset) {
        long id = dataset.getDatasetInfo().getId();

        Uri columnsUri = DatasetContentProvider.ColumnsContentUri(id);
        Uri rowsUri = DatasetContentProvider.RowsContentUri(id);
        ContentResolver contentResolver = appContext.getContentResolver();

        Bundle columnsBundle = new Bundle();
        columnsBundle.putLong(DatasetContentProvider.ID, id);

        contentResolver.call(columnsUri, CREATE_TABLE_COLUMNS_METHOD, NO_ARGUMENTS, columnsBundle);

        Bundle rowsBundle = new Bundle();
        rowsBundle.putLong(DatasetContentProvider.ID, id);
        Set<String> columnsNamesSet = dataset.getColumns().keySet();
        String[] columnNames = columnsNamesSet.toArray(new String[columnsNamesSet.size()]);
        rowsBundle.putStringArray(DatasetContentProvider.COLUMNS, columnNames);

        contentResolver.call(rowsUri, CREATE_TABLE_ROWS_METHOD, NO_ARGUMENTS, rowsBundle);

        List<ContentValues> columnsContentValues = new ArrayList<>(dataset.getColumns().size());
        for (Map.Entry<String, String> columnEntry : dataset.getColumns().entrySet()) {
            ContentValues values = new ContentValues();
            values.put(Columns.KEY, columnEntry.getKey());
            values.put(Columns.VALUE, columnEntry.getValue());
            columnsContentValues.add(values);
        }
        int insertedColumns = contentResolver.bulkInsert(columnsUri, columnsContentValues.toArray(new ContentValues[columnsContentValues.size()]));
        if (insertedColumns != dataset.getColumns().size()) {
            Log.e(getClass().getName(), "Nepodarilo sa vložiť správny počet riadkov do databázy na URI: " + columnsUri);
            return false;
        }

        List<ContentValues> rowsContentValues = new ArrayList<>(dataset.getRows().size());
        for (Row row : dataset.getRows()) {
            ContentValues values = new ContentValues();
            for (String key : columnsNamesSet) {
                values.put(key, row.get(key));
            }
            rowsContentValues.add(values);
        }
        int insertedRows = contentResolver.bulkInsert(rowsUri, rowsContentValues.toArray(new ContentValues[rowsContentValues.size()]));
        if (insertedRows != dataset.getRows().size()) {
            Log.e(getClass().getName(), "Nepodarilo sa vložiť správny počet riadkov do databázy na URI: " + rowsUri);
            return false;
        }

        return true;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (success) {
            if (relativeLayoutWeakReference != null) {
                final RelativeLayout layout = relativeLayoutWeakReference.get();

                if (layout != null) {
                    ProgressBar progressBar = (ProgressBar) layout.findViewById(R.id.datasetProgressBar);
                    progressBar.setVisibility(View.INVISIBLE);

                    TextView dateTextView = (TextView) layout.findViewById(R.id.datasetDateTextView);
                    String date = datasetInfo.getDate();
                    dateTextView.setText(appContext.getString(R.string.download_date, date));
                    dateTextView.setVisibility(View.VISIBLE);

                    Button downloadBtn = (Button) layout.findViewById(R.id.datasetDownloadBtn);
                    downloadBtn.setText(R.string.download_btn_update);
                    downloadBtn.setEnabled(false);

                    ImageButton removeImageBtn = (ImageButton) layout.findViewById(R.id.datasetRemoveImageBtn);
                    removeImageBtn.setVisibility(View.VISIBLE);
                }
            }
            if (contextWeakReference != null) {
                try {
                    Context context = contextWeakReference.get();
                    Activity activity = (Activity) context;
                    NavigationView navigationView = (NavigationView) activity.findViewById(R.id.nav_view);
                    new Navigation().addMenuItems(context, navigationView);
                } catch (NullPointerException e) {
                    Log.d(getClass().getName(), e.getMessage(), e);
                    // Do nothing more
                }
            }
        } else {
            if (relativeLayoutWeakReference != null) {
                final RelativeLayout layout = relativeLayoutWeakReference.get();
                if (layout != null) {
                    ProgressBar progressBar = (ProgressBar) layout.findViewById(R.id.datasetProgressBar);
                    progressBar.setVisibility(View.INVISIBLE);

                    Button downloadBtn = (Button) layout.findViewById(R.id.datasetDownloadBtn);
                    downloadBtn.setEnabled(true);

                    if (datasetInfo.getDate() != null) {
                        downloadBtn.setText(R.string.download_btn_update);

                        TextView dateTextView = (TextView) layout.findViewById(R.id.datasetDateTextView);
                        dateTextView.setText(appContext.getString(R.string.download_date, datasetInfo.getDate()));
                        dateTextView.setVisibility(View.VISIBLE);

                        ImageButton removeImageBtn = (ImageButton) layout.findViewById(R.id.datasetRemoveImageBtn);
                        removeImageBtn.setEnabled(true);
                        removeImageBtn.setVisibility(View.VISIBLE);
                    }

                    Toast.makeText(appContext, appContext.getString(R.string.notification_download_failure, datasetInfo.getName()), Toast.LENGTH_SHORT).show();
                }
            }
        }

        triggerNotification(success, datasetInfo);
    }

    private void triggerNotification(Boolean success, DatasetInfo datasetInfo) {
        int NOTIFICATION_ID = (int) datasetInfo.getId();

        String contentText;

        if (success) {
            contentText = appContext.getString(R.string.notification_download_successful, datasetInfo.getName());
        } else {
            contentText = appContext.getString(R.string.notification_download_failure, datasetInfo.getName());
        }

        Notification notification = new Notification.Builder(appContext)
                .setContentTitle(appContext.getString(R.string.app_name))
                .setContentText(contentText)
                .setContentIntent(getEmptyNotificationContentIntent())
                .setTicker(appContext.getString(R.string.app_name))
                .setAutoCancel(true)
                .setSmallIcon(android.R.drawable.ic_menu_save)
                .getNotification();

        NotificationManager notificationManager
                = (NotificationManager) appContext.getSystemService(
                Context.NOTIFICATION_SERVICE);
        notificationManager.notify(appContext.getString(R.string.app_name), NOTIFICATION_ID, notification);
    }

    public PendingIntent getEmptyNotificationContentIntent() {
        int REQUEST_CODE = 0;
        int NO_FLAGS = 0;

        return PendingIntent.getActivity(appContext, REQUEST_CODE, new Intent(), NO_FLAGS);
    }

    /**
     * https://developer.android.com/training/basics/network-ops/xml.html
     */
    private Dataset parse(InputStream is, DatasetInfo datasetInfo) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(is, XML_PARSER_INPUT_ENCODING);
            parser.nextTag();
            return readFeed(parser, datasetInfo);
        } finally {
            is.close();
        }
    }

    private Dataset readFeed(XmlPullParser parser, DatasetInfo datasetInfo) throws XmlPullParserException, IOException {
        List<Row> rows = new ArrayList<>();
        Map<String, String> columns = new LinkedHashMap<>();

        parser.require(XmlPullParser.START_TAG, NAMESPACE, DS);
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();

            if (name.equals(COLUMN)) {
                Map.Entry<String, String> column = readColumn(parser);
                columns.put(column.getKey(), column.getValue());
            }
            if (name.equals(ROW)) {
                rows.add(readRow(parser, columns));
            }
        }

        return new Dataset(datasetInfo, columns, rows);
    }

    private Map.Entry<String, String> readColumn(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, NAMESPACE, COLUMN);
        String tag = parser.getName();

        String col_id = null;
        String name = null;

        if (tag.equals(COLUMN)) {
            name = parser.getAttributeValue(NAMESPACE, NAME);
            col_id = parser.getAttributeValue(NAMESPACE, COL_ID);
        }

        parser.nextTag();

        parser.require(XmlPullParser.END_TAG, NAMESPACE, COLUMN);
        return new AbstractMap.SimpleEntry<>(col_id, name);
    }

    private Row readRow(XmlPullParser parser, Map<String, String> columns) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, NAMESPACE, ROW);
        String tag = parser.getName();

        Map<String, String> map = new LinkedHashMap<>();

        if (tag.equals(ROW)) {
            for (String column : columns.keySet()) {
                String value = parser.getAttributeValue(NAMESPACE, column);
                map.put(column, value);
            }
        }

        parser.nextTag();

        parser.require(XmlPullParser.END_TAG, NAMESPACE, ROW);
        return new Row(map);
    }
}
