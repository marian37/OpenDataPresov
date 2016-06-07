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
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.List;

import sk.upjs.ics.presov.opendata.model.DatasetInfo;
import sk.upjs.ics.presov.opendata.DatasetInfoDao;
import sk.upjs.ics.presov.opendata.Defaults;
import sk.upjs.ics.presov.opendata.Navigation;
import sk.upjs.ics.presov.opendata.R;

public class IntroActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, AdapterView.OnItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        Navigation navigation = new Navigation();
        navigation.prepareNavigationBar(this);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigation.addMenuItems(this, navigationView);

        List<DatasetInfo> datasetInfoList = new DatasetInfoDao(this).downloaded();

        Spinner spinner = (Spinner) findViewById(R.id.introSpinner);
        spinner.setOnItemSelectedListener(this);
        ArrayAdapter<DatasetInfo> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, datasetInfoList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter.insert(new DatasetInfo(Defaults.SPINNER_DEFAULT_DATASET_INFO_ID, getString(R.string.spinner_default_value), Defaults.NO_STRING_URL), 0);
        spinner.setAdapter(adapter);
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

    public void introStartBtnOnClick(View view) {
        Intent intent = new Intent(this, ManagerActivity.class);
        startActivity(intent);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        DatasetInfo info = (DatasetInfo) parent.getItemAtPosition(position);
        int datasetInfoId = (int) info.getId();
        if (datasetInfoId != Defaults.SPINNER_DEFAULT_DATASET_INFO_ID) {
            Intent intent = new Intent(this, DatasetActivity.class);
            intent.putExtra(Defaults.DATASET_ID, datasetInfoId - 1);

            startActivity(intent);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing
    }
}
