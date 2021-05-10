package com.app.voxmed.main;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.app.voxmed.R;
import com.app.voxmed.adapters.PatientListAdapter;
import com.app.voxmed.base.BaseActivity;
import com.app.voxmed.commons.Commons;
import com.app.voxmed.commons.ReqConst;
import com.app.voxmed.models.User;
import com.iamhabib.easy_preference.EasyPreference;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.wang.avi.AVLoadingIndicatorView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

public class PatientListActivity extends BaseActivity {

    ImageView searchButton, cancelButton, newPatientButton;
    LinearLayout searchBar;
    EditText ui_edtsearch;
    LinearLayout titleBox;
    ListView list;
    ArrayList<User> patients = new ArrayList<>();
    PatientListAdapter adapter = new PatientListAdapter(this);

    AVLoadingIndicatorView progressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_list);

        Commons.patientListActivity = this;

        progressBar = (AVLoadingIndicatorView) findViewById(R.id.loading_bar);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                onBackPressed();
            }
        });

        titleBox = (LinearLayout) findViewById(R.id.titleLayout);

        searchBar = (LinearLayout)findViewById(R.id.search_bar);
        searchButton = (ImageView)findViewById(R.id.searchButton);
        cancelButton = (ImageView)findViewById(R.id.cancelButton);

        ui_edtsearch = (EditText)findViewById(R.id.edt_search);
        ui_edtsearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String text = ui_edtsearch.getText().toString().toLowerCase(Locale.getDefault());
                adapter.filter(text);
            }
        });

        list = (ListView) findViewById(R.id.list);
        adapter.setDatas(patients);
        list.setAdapter(adapter);

        newPatientButton = (ImageView)findViewById(R.id.newPatientButton);
        newPatientButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Commons.patient = null;
                Intent intent = new Intent(getApplicationContext(), AddPatientActivity.class);
                startActivity(intent);
            }
        });

        setupUI(findViewById(R.id.activity), this);

    }

    @Override
    public void onResume() {
        super.onResume();
        getPatients();
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.top_in, R.anim.bottom_out);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Commons.patientListActivity = null;
    }

    public void search(View view) {
        cancelButton.setVisibility(View.VISIBLE);
        searchButton.setVisibility(View.GONE);
        searchBar.setVisibility(View.VISIBLE);
        titleBox.setVisibility(View.GONE);
    }

    public void cancelSearch(View view) {
        cancelButton.setVisibility(View.GONE);
        searchButton.setVisibility(View.VISIBLE);
        searchBar.setVisibility(View.GONE);
        titleBox.setVisibility(View.VISIBLE);
        ui_edtsearch.setText("");
    }

    private void getPatients(){
        progressBar.setVisibility(View.VISIBLE);
        AndroidNetworking.post(ReqConst.SERVER_URL + "getallusers")
                .setPriority(Priority.HIGH)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // do anything with response
                        Log.d("RESPONSE!!!", response.toString());
                        progressBar.setVisibility(View.GONE);
                        try {
                            String result = response.getString("result_code");
                            if(result.equals("0")){
                                patients.clear();
                                JSONArray dataArr = response.getJSONArray("data");
                                for(int i=0; i<dataArr.length(); i++) {
                                    JSONObject object = (JSONObject) dataArr.get(i);
                                    User user = new User();
                                    user.set_idx(object.getInt("id"));
                                    user.set_name(object.getString("name"));
                                    user.set_email(object.getString("email"));
                                    user.set_password(object.getString("password"));
                                    user.set_picture_url(object.getString("picture_url"));
                                    user.set_registered_time(object.getString("registered_time"));
                                    user.set_role(object.getString("role"));
                                    user.set_age(object.getString("age"));
                                    user.set_patientID(object.getString("patientID"));
                                    user.set_status(object.getString("status"));
                                    if(user.get_role().equals("patient"))patients.add(user);
                                }
                                if(patients.isEmpty()){
                                    ((TextView)findViewById(R.id.no_result)).setVisibility(View.VISIBLE);
                                }else {
                                    ((TextView)findViewById(R.id.no_result)).setVisibility(View.GONE);
                                }
                                adapter.setDatas(patients);
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
                        progressBar.setVisibility(View.GONE);
                    }
                });

    }
}






































