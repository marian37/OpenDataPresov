package sk.upjs.ics.presov.opendata;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import java.util.List;

import sk.upjs.ics.presov.opendata.activity.DatasetActivity;
import sk.upjs.ics.presov.opendata.activity.IntroActivity;
import sk.upjs.ics.presov.opendata.activity.ManagerActivity;
import sk.upjs.ics.presov.opendata.model.DatasetInfo;

public class Navigation {

    public void prepareNavigationBar(AppCompatActivity activity) {
        Toolbar toolbar = (Toolbar) activity.findViewById(R.id.toolbar);
        activity.setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) activity.findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                activity, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
    }

    public void onNavigationItemSelected(Activity activity, MenuItem item) {
        int id = item.getItemId();

        Intent intent = null;

        switch (id) {
            case R.id.nav_home:
                intent = new Intent(activity, IntroActivity.class);
                break;
            case R.id.nav_manager:
                intent = new Intent(activity, ManagerActivity.class);
                break;

            default:
                intent = new Intent(activity, DatasetActivity.class);
                intent.putExtra(Defaults.DATASET_ID, id - 1);
        }

        DrawerLayout drawer = (DrawerLayout) activity.findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        activity.startActivity(intent);
    }

    public void addMenuItems(Context context, NavigationView navigationView) {
        Menu menu = navigationView.getMenu();
        menu.removeGroup(R.id.drawer_menu_group);
        List<DatasetInfo> datasetInfoList = new DatasetInfoDao(context).downloaded();
        for (DatasetInfo info : datasetInfoList) {
            menu.add(R.id.drawer_menu_group, (int) info.getId(), Menu.NONE, info.getName());
        }
    }
}
