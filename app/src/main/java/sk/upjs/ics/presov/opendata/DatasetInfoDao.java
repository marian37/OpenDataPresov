package sk.upjs.ics.presov.opendata;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import sk.upjs.ics.presov.opendata.model.DatasetInfo;

public class DatasetInfoDao {

    private static final String DELIMITER = ";";

    private List<DatasetInfo> datasetInfoList = new ArrayList<>();

    private Context context;

    public DatasetInfoDao(Context context) {
        this.context = context;
        this.datasetInfoList = loadDatasetInfo();
    }

    public List<DatasetInfo> list() {
        return new ArrayList<>(this.datasetInfoList);
    }

    public List<DatasetInfo> downloaded() {
        List<DatasetInfo> result = new ArrayList<>();

        for (DatasetInfo info : this.datasetInfoList) {
            if (info.getDate() != null) {
                result.add(info);
            }
        }

        return result;
    }

    public DatasetInfo getDatasetInfo(int id) {
        return datasetInfoList.get(id);
    }

    private List<DatasetInfo> loadDatasetInfo() {
        List<DatasetInfo> datasetInfoList = new ArrayList<>();

        InputStream is = context.getResources().openRawResource(R.raw.dataset_info);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line = null;
        String[] tokens = null;

        SharedPreferences preferences = context.getSharedPreferences(Defaults.SHARED_PREFERENCES_DATASET_INFO, Context.MODE_PRIVATE);

        try {
            while ((line = br.readLine()) != null) {
                tokens = line.split(DELIMITER);
                long id = Long.parseLong(tokens[0]);
                String name = tokens[1];
                String urlString = tokens[2];
                DatasetInfo datasetInfo = new DatasetInfo(id, name, urlString);
                datasetInfo.setDate(preferences.getString(datePreferenceKey(id), Defaults.DATE_PREFERENCE_DEFAULT_VALUE));
                datasetInfoList.add(datasetInfo);
            }
        } catch (IOException e) {
            Log.e(getClass().getName(), e.getMessage(), e);
        }

        return datasetInfoList;
    }

    public static String datePreferenceKey(long id) {
        return "dataset" + id + "date";
    }
}
