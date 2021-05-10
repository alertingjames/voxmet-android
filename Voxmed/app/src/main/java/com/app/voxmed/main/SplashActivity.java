package com.app.voxmed.main;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.app.voxmed.R;
import com.app.voxmed.base.BaseActivity;
import com.app.voxmed.commons.Commons;
import com.app.voxmed.commons.ReqConst;
import com.app.voxmed.models.User;
import com.iamhabib.easy_preference.EasyPreference;
import com.wang.avi.AVLoadingIndicatorView;

import org.json.JSONException;
import org.json.JSONObject;

public class SplashActivity extends BaseActivity {

    AVLoadingIndicatorView progressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        progressBar = (AVLoadingIndicatorView)findViewById(R.id.loading_bar);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                String email = EasyPreference.with(getApplicationContext()).getString("email", "");
                String role = EasyPreference.with(getApplicationContext()).getString("role", "");
                if(email.length() > 0 && role.length() > 0){
                    login(email, "", role);
                }else {
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        }, 1500);

    }

    private void login(String email, String password, String role){

        progressBar.setVisibility(View.VISIBLE);
        AndroidNetworking.post(ReqConst.SERVER_URL + "login")
                .addBodyParameter("email", email)
                .addBodyParameter("password", password)
                .addBodyParameter("role", role)
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
                                JSONObject object = response.getJSONObject("data");
                                User user = new User();
                                user.set_idx(object.getInt("id"));
                                user.set_name(object.getString("name"));
                                user.set_email(object.getString("email"));
                                user.set_password(object.getString("password"));
                                user.set_picture_url(object.getString("picture_url"));
                                user.set_registered_time(object.getString("registered_time"));
                                user.set_role(object.getString("role"));
                                user.set_patientID(object.getString("patientID"));
                                user.set_status(object.getString("status"));

                                Commons.thisUser = user;

                                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                startActivity(intent);
                                finish();

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
                        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });

    }
}






































