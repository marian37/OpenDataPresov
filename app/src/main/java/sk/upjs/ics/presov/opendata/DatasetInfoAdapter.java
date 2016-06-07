package sk.upjs.ics.presov.opendata;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import sk.upjs.ics.presov.opendata.model.DatasetInfo;

import static sk.upjs.ics.presov.opendata.Defaults.NO_ARGUMENTS;
import static sk.upjs.ics.presov.opendata.provider.DatasetContentProvider.ColumnsContentUri;
import static sk.upjs.ics.presov.opendata.provider.DatasetContentProvider.ID;
import static sk.upjs.ics.presov.opendata.provider.DatasetContentProvider.REMOVE_TABLE_COLUMNS_METHOD;
import static sk.upjs.ics.presov.opendata.provider.DatasetContentProvider.REMOVE_TABLE_ROWS_METHOD;
import static sk.upjs.ics.presov.opendata.provider.DatasetContentProvider.RowsContentUri;

public class DatasetInfoAdapter extends ArrayAdapter<DatasetInfo> {

    public DatasetInfoAdapter(Context context, List<DatasetInfo> datasetInfoList) {
        super(context, 0, datasetInfoList);
    }

    @Override
    public DatasetInfo getItem(int position) {
        return super.getItem(position);
    }

    @Override
    public View getView(int position, View convertView, final ViewGroup parent) {
        View rowView = LayoutInflater.from(getContext()).inflate(R.layout.dataset_info_layout, parent, false);

        TextView nameTextView = (TextView) rowView.findViewById(R.id.datasetNameTextView);
        TextView dateTextView = (TextView) rowView.findViewById(R.id.datasetDateTextView);
        Button downloadBtn = (Button) rowView.findViewById(R.id.datasetDownloadBtn);
        final ImageButton removeImageBtn = (ImageButton) rowView.findViewById(R.id.datasetRemoveImageBtn);
        ProgressBar datasetProgressBar = (ProgressBar) rowView.findViewById(R.id.datasetProgressBar);

        downloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadBtnOnClick(v);
            }
        });

        removeImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeImageBtnOnClick(v);
            }
        });

        DatasetInfo datasetInfo = getItem(position);

        nameTextView.setText(datasetInfo.getId() + ". " + datasetInfo.getName());
        downloadBtn.setFocusable(false);
        removeImageBtn.setFocusable(false);
        datasetProgressBar.setVisibility(View.INVISIBLE);
        if (datasetInfo.getDate() != null) {
            if (datasetInfo.getDate().equals(new SimpleDateFormat("dd.MM.yyyy").format(new Date()))) {
                downloadBtn.setEnabled(false);
            } else {
                downloadBtn.setEnabled(true);
            }
            dateTextView.setText(getContext().getString(R.string.download_date, datasetInfo.getDate()));
            downloadBtn.setText(R.string.download_btn_update);
            removeImageBtn.setVisibility(View.VISIBLE);
        } else {
            dateTextView.setText(R.string.empty_string);
            downloadBtn.setEnabled(true);
            downloadBtn.setText(R.string.download_btn_download);
            removeImageBtn.setVisibility(View.INVISIBLE);
        }

        return rowView;
    }

    private void downloadBtnOnClick(View v) {
        if (R.id.datasetDownloadBtn == v.getId()) {
            RelativeLayout parentLayout = (RelativeLayout) v.getParent();
            ListView listView = (ListView) parentLayout.getParent();
            int position = listView.getPositionForView(parentLayout);

            DownloadDatasetAsyncTask asyncTask = new DownloadDatasetAsyncTask(getContext(), parentLayout);
            asyncTask.execute(getItem(position));

            v.setEnabled(false);
            ProgressBar progressBar = (ProgressBar) parentLayout.findViewById(R.id.datasetProgressBar);
            progressBar.setVisibility(View.VISIBLE);
            ImageButton removeImageBtn = (ImageButton) parentLayout.findViewById(R.id.datasetRemoveImageBtn);
            removeImageBtn.setVisibility(View.INVISIBLE);
            TextView dateTextView = (TextView) parentLayout.findViewById(R.id.datasetDateTextView);
            dateTextView.setVisibility(View.INVISIBLE);
        }
    }

    private void removeImageBtnOnClick(final View v) {
        if (R.id.datasetRemoveImageBtn == v.getId()) {
            final RelativeLayout parentLayout = (RelativeLayout) v.getParent();
            ListView listView = (ListView) parentLayout.getParent();
            int position = listView.getPositionForView(parentLayout);

            AsyncTask<DatasetInfo, Void, Boolean> removeDataAsyncTask = new AsyncTask<DatasetInfo, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(DatasetInfo... params) {
                    DatasetInfo datasetInfo = params[0];
                    long id = datasetInfo.getId();
                    ContentResolver contentResolver = getContext().getContentResolver();

                    Bundle bundle = new Bundle();
                    bundle.putLong(ID, id);
                    contentResolver.call(ColumnsContentUri(id), REMOVE_TABLE_COLUMNS_METHOD, NO_ARGUMENTS, bundle);
                    contentResolver.call(RowsContentUri(id), REMOVE_TABLE_ROWS_METHOD, NO_ARGUMENTS, bundle);

                    SharedPreferences preferences = getContext().getSharedPreferences(Defaults.SHARED_PREFERENCES_DATASET_INFO, Context.MODE_PRIVATE);
                    if (!preferences.edit().remove(DatasetInfoDao.datePreferenceKey(id)).commit()) {
                        Log.w(getClass().getName(), "Nepodarilo sa vymazať dátum stiahnutia datasetu.");
                        return false;
                    }
                    datasetInfo.setDate(Defaults.NO_DATE);

                    return true;
                }

                @Override
                protected void onPostExecute(Boolean success) {
                    v.setEnabled(true);
                    ProgressBar progressBar = (ProgressBar) parentLayout.findViewById(R.id.datasetProgressBar);
                    progressBar.setVisibility(View.INVISIBLE);
                    if (success) {
                        Activity activity = (Activity) getContext();
                        NavigationView navigationView = (NavigationView) activity.findViewById(R.id.nav_view);
                        new Navigation().addMenuItems(getContext(), navigationView);

                        v.setVisibility(View.INVISIBLE);
                        TextView dateTextView = (TextView) parentLayout.findViewById(R.id.datasetDateTextView);
                        dateTextView.setVisibility(View.INVISIBLE);
                        Button downloadBtn = (Button) parentLayout.findViewById(R.id.datasetDownloadBtn);
                        downloadBtn.setEnabled(true);
                        downloadBtn.setText(R.string.download_btn_download);
                    } else {
                        v.setVisibility(View.VISIBLE);
                        Button downloadBtn = (Button) parentLayout.findViewById(R.id.datasetDownloadBtn);
                        downloadBtn.setEnabled(true);
                        downloadBtn.setText(R.string.download_btn_update);
                        Toast.makeText(v.getContext(), "Nepodarilo sa odstrániť dataset.", Toast.LENGTH_SHORT).show();
                    }
                }
            };

            removeDataAsyncTask.execute(getItem(position));

            v.setEnabled(false);
            Button downloadBtn = (Button) parentLayout.findViewById(R.id.datasetDownloadBtn);
            downloadBtn.setEnabled(false);
            ProgressBar progressBar = (ProgressBar) parentLayout.findViewById(R.id.datasetProgressBar);
            progressBar.setVisibility(View.VISIBLE);
            TextView dateTextView = (TextView) parentLayout.findViewById(R.id.datasetDateTextView);
            dateTextView.setVisibility(View.INVISIBLE);
        }
    }
}
