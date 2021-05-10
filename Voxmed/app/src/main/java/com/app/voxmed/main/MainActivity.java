package com.app.voxmed.main;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.StrictMode;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.app.voxmed.R;
import com.app.voxmed.adapters.ReportListAdapter;
import com.app.voxmed.base.BaseActivity;
import com.app.voxmed.commons.Commons;
import com.app.voxmed.commons.ReqConst;
import com.app.voxmed.models.Report;
import com.google.android.material.navigation.NavigationView;
import com.iamhabib.easy_preference.EasyPreference;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    NavigationView navigationView;
    DrawerLayout drawer;
    ImageView searchButton, cancelButton;
    public LinearLayout searchBar;
    EditText ui_edtsearch;
    LinearLayout titleBox;
    LinearLayout loadingLayout;
    TextView loadingcap;
    ListView list;
    ArrayList<Report> reports = new ArrayList<>();
    ReportListAdapter adapter = new ReportListAdapter(this);
    boolean initF = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyApp::MyWakelockTag");
        wakeLock.acquire();
        wakeLock.release();

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        loadingLayout = (LinearLayout) findViewById(R.id.loadingLayout);
        loadingcap = (TextView)findViewById(R.id.loadingcap);

        titleBox = (LinearLayout) findViewById(R.id.titleLayout);
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);

        Picasso.with(getApplicationContext())
                .load(Commons.thisUser.get_picture_url())
                .error(R.mipmap.appicon)
                .placeholder(R.mipmap.appicon)
                .into((CircleImageView)navigationView.getHeaderView(0).findViewById(R.id.avatar));
        ((TextView)navigationView.getHeaderView(0).findViewById(R.id.name)).setText(Commons.thisUser.get_name());

        if(Commons.thisUser.get_role().equals("patient") || Commons.thisUser.get_role().equals("employee")){
            navigationView.getMenu().getItem(0).setVisible(false);
            navigationView.getMenu().getItem(1).setVisible(false);
            navigationView.getMenu().getItem(2).setVisible(false);
            navigationView.getMenu().getItem(3).setVisible(false);
        }

        searchBar = (LinearLayout)findViewById(R.id.search_bar);
        searchButton = (ImageView)findViewById(R.id.searchButton);
        cancelButton = (ImageView)findViewById(R.id.cancelButton);

        ui_edtsearch = (EditText)findViewById(R.id.edt_search);
        ui_edtsearch.setFocusable(true);
        ui_edtsearch.requestFocus();

        ui_edtsearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String text = ui_edtsearch.getText().toString().trim().toLowerCase(Locale.getDefault());
                adapter.filter(text);

            }
        });

        list = (ListView) findViewById(R.id.list);

        setupUI(findViewById(R.id.activity), this);

    }

    public void search(View view){
        cancelButton.setVisibility(View.VISIBLE);
        searchButton.setVisibility(View.GONE);
        searchBar.setVisibility(View.VISIBLE);
        titleBox.setVisibility(View.GONE);
    }

    public void cancelSearch(View view){
        cancelButton.setVisibility(View.GONE);
        searchButton.setVisibility(View.VISIBLE);
        searchBar.setVisibility(View.GONE);
        titleBox.setVisibility(View.VISIBLE);
        ui_edtsearch.setText("");
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        displaySelectedScreen(menuItem.getItemId());
        return false;
    }

    private void displaySelectedScreen(int itemId) {
        switch (itemId) {
            case R.id.addreport:
                Intent intent = new Intent(getApplicationContext(), AddNewReportActivity.class);
                startActivity(intent);
                break;
            case R.id.addpatient:
                Commons.patient = null;
                intent = new Intent(getApplicationContext(), AddPatientActivity.class);
                startActivity(intent);
                break;
            case R.id.patients:
                intent = new Intent(getApplicationContext(), PatientsActivity.class);
                startActivity(intent);
                break;
            case R.id.keywords:
                intent = new Intent(getApplicationContext(), GeneralKeywordsActivity.class);
                startActivity(intent);
                break;
            case R.id.profile:
                intent = new Intent(getApplicationContext(), ProfileActivity.class);
                startActivity(intent);
                break;
            case R.id.setting:
                intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.logout:
                logout();
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
    }

    private void logout(){
        EasyPreference.with(getApplicationContext()).clearAll().save();
        Commons.thisUser = null;
        showToast(getString(R.string.logged_out));
        Intent intent = new Intent(getApplicationContext(), SplashActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(0,0);
    }

    @Override
    public void onResume() {
        super.onResume();
        Picasso.with(getApplicationContext())
                .load(Commons.thisUser.get_picture_url())
                .error(R.mipmap.appicon)
                .placeholder(R.mipmap.appicon)
                .into((CircleImageView)navigationView.getHeaderView(0).findViewById(R.id.avatar));
        getReports();
    }

    private void showProgressBar(){
        loadingLayout.setVisibility(View.VISIBLE);
        if (initF) {
            loadingcap.setText(getString(R.string.loading));
            initF = false;
        }
        else loadingcap.setText(getString(R.string.refreshing));
    }

    private void getReports(){
        showProgressBar();
        AndroidNetworking.post(ReqConst.SERVER_URL + "getreports")
                .setPriority(Priority.HIGH)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // do anything with response
                        Log.d("RESPONSE!!!", response.toString());
                        loadingLayout.setVisibility(View.GONE);
                        try {
                            String result = response.getString("result_code");
                            if(result.equals("0")){
                                reports.clear();
                                JSONArray dataArr = response.getJSONArray("data");
                                for(int i=0; i<dataArr.length(); i++) {
                                    JSONObject object = (JSONObject) dataArr.get(i);
                                    Report report = new Report();
                                    report.set_idx(object.getInt("id"));
                                    report.set_member_id(object.getInt("member_id"));
                                    report.set_body(object.getString("body"));
                                    report.set_patientID(object.getString("patientID"));
                                    report.set_picture_url(object.getString("picture_url"));
                                    report.set_audio_url(object.getString("audio_url"));
                                    report.set_date_time(object.getString("date_time"));
                                    report.set_status(object.getString("status"));

                                    if(Commons.thisUser.get_role().equals("doctor")){
                                        if(report.get_member_id() == Commons.thisUser.get_idx())
                                            reports.add(report);
                                    }else if(Commons.thisUser.get_role().equals("patient")){
                                        if(report.get_patientID().equals(Commons.thisUser.get_patientID()))
                                            reports.add(report);
                                    }else if(Commons.thisUser.get_role().equals("employee")){
                                        reports.add(report);
                                    }
                                }
                                if(reports.isEmpty()){
                                    ((TextView)findViewById(R.id.no_result)).setVisibility(View.VISIBLE);
                                }else {
                                    ((TextView)findViewById(R.id.no_result)).setVisibility(View.GONE);
                                }
                                adapter.setDatas(reports);
                                list.setAdapter(adapter);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(ANError error) {
                        // handle error
                        Log.d("ERROR!!!", error.getErrorBody());
                        loadingLayout.setVisibility(View.GONE);
                    }
                });

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}



































