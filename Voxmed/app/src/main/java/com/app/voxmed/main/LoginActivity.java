package com.app.voxmed.main;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.androidnetworking.interfaces.UploadProgressListener;
import com.app.voxmed.R;
import com.app.voxmed.base.BaseActivity;
import com.app.voxmed.commons.Commons;
import com.app.voxmed.commons.ReqConst;
import com.app.voxmed.models.User;
import com.iamhabib.easy_preference.EasyPreference;
import com.makeramen.roundedimageview.RoundedImageView;
import com.wang.avi.AVLoadingIndicatorView;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends BaseActivity {

    private static final String[] PERMISSIONS = {
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.INSTALL_PACKAGES,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.VIBRATE,
            android.Manifest.permission.SET_TIME,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.WAKE_LOCK,
    };

    EditText emailBox, passwordBox;
    TextView signupButton, forgotPasswordButton;
    ImageButton showButton, nextButton;
    RadioButton doctor, employee, patient;
    AVLoadingIndicatorView progressBar;
    boolean pwShow = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        checkAllPermission();

        progressBar = (AVLoadingIndicatorView) findViewById(R.id.loading_bar);

        emailBox = (EditText)findViewById(R.id.emailBox);
        passwordBox = (EditText)findViewById(R.id.passwordBox);

        signupButton = (TextView)findViewById(R.id.signupButton);
        forgotPasswordButton = (TextView)findViewById(R.id.forgotPasswordButton);

        showButton = (ImageButton)findViewById(R.id.showButton);
        nextButton = (ImageButton)findViewById(R.id.nextButton);

        doctor = (RadioButton)findViewById(R.id.doctor);
        employee = (RadioButton)findViewById(R.id.employee);
        patient = (RadioButton)findViewById(R.id.patient);

        showButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!pwShow){
                    pwShow = true;
                    showButton.setImageResource(R.drawable.eyelock);
                    if(passwordBox.getText().length() > 0){
                        passwordBox.setInputType(InputType.TYPE_CLASS_TEXT);
                        Typeface font = Typeface.createFromAsset(getApplicationContext().getAssets(), "Quicksand-Regular.otf");
                        passwordBox.setTypeface(font);
                    }
                }else {
                    pwShow = false;
                    showButton.setImageResource(R.drawable.eyeunlock);
                    if(passwordBox.getText().length() > 0){
                        passwordBox.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    }
                }
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });

        forgotPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ForgotPasswordActivity.class);
                startActivity(intent);
            }
        });

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
                startActivity(intent);
            }
        });

        setupUI(findViewById(R.id.activity), this);

    }

    private void login(){

        if(emailBox.getText().length() == 0){
            showToast(getString(R.string.enter_email));
            return;
        }

        if(!isValidEmail(emailBox.getText().toString().trim())){
            showToast(getString(R.string.enter_valid_email));
            return;
        }

        if(passwordBox.getText().length() == 0){
            showToast(getString(R.string.enter_password));
            return;
        }

        if(!doctor.isChecked() && !employee.isChecked() && !patient.isChecked()){
            showToast(getString(R.string.select_role));
            return;
        }

        String role = "";
        if(doctor.isChecked())role = "doctor";
        if(employee.isChecked())role = "employee";
        if(patient.isChecked())role = "patient";

        login(emailBox.getText().toString().trim(), passwordBox.getText().toString().trim(), role);

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
                                showToast(getString(R.string.login_success));

                                EasyPreference.with(getApplicationContext()).addString("email", Commons.thisUser.get_email()).save();
                                EasyPreference.with(getApplicationContext()).addString("role", Commons.thisUser.get_role()).save();

                                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                startActivity(intent);
                                finish();

                            }else if(result.equals("1")){
                                Commons.thisUser = null;
                                showToast(getString(R.string.unregistered_user));
                                Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
                                startActivity(intent);
                            }else if(result.equals("2")){
                                Commons.thisUser = null;
                                showToast(getString(R.string.incorrect_password));
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
                        showToast(error.getErrorDetail());
                    }
                });

    }

    public void checkAllPermission() {

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        if (hasPermissions(this, PERMISSIONS)){

        }else {
            ActivityCompat.requestPermissions(this, PERMISSIONS, 101);
        }
    }
    public static boolean hasPermissions(Context context, String... permissions) {

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {

            for (String permission : permissions) {

                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

}

































