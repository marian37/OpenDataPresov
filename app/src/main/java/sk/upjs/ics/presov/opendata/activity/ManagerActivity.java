package sk.upjs.ics.presov.opendata.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.List;

import sk.upjs.ics.presov.opendata.model.DatasetInfo;
import sk.upjs.ics.presov.opendata.DatasetInfoAdapter;
import sk.upjs.ics.presov.opendata.DatasetInfoDao;
import sk.upjs.ics.presov.opendata.Navigation;
import sk.upjs.ics.presov.opendata.R;

import static sk.upjs.ics.presov.opendata.Defaults.DATASET_ID;

public class ManagerActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String DATASET_INFO_DAO = "datasetInfoDao";

    private DatasetInfoDao datasetInfoDao = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager);

        setTitle(getString(R.string.manager_activity_title));

        Navigation navigation = new Navigation();
        navigation.prepareNavigationBar(this);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigation.addMenuItems(this, navigationView);

        datasetInfoDao = new DatasetInfoDao(this);

        List<DatasetInfo> datasetInfoList = datasetInfoDao.list();

        ListView datasetInfoListView = (ListView) findViewById(R.id.managerListView);
        datasetInfoListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (datasetInfoDao.getDatasetInfo(position).getDate() != null) {
                    Intent intent = new Intent(ManagerActivity.this, DatasetActivity.class);
                    intent.putExtra(DATASET_ID, position);

                    startActivity(intent);
                } else {
                    Toast.makeText(ManagerActivity.this, R.string.dataset_not_downloaded, Toast.LENGTH_SHORT).show();
                }
            }
        });

        DatasetInfoAdapter datasetInfoAdapter = new DatasetInfoAdapter(this, datasetInfoList);

        datasetInfoListView.setAdapter(datasetInfoAdapter);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        new Navigation().onNavigationItemSelected(this, item);
        return true;
    }
}
