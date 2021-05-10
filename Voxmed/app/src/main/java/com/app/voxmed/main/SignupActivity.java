package com.app.voxmed.main;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import com.googlecode.mp4parser.authoring.Edit;
import com.iamhabib.easy_preference.EasyPreference;
import com.makeramen.roundedimageview.RoundedImageView;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.wang.avi.AVLoadingIndicatorView;

import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import de.hdodenhof.circleimageview.CircleImageView;

public class SignupActivity extends BaseActivity {

    FrameLayout pictureFrame;
    EditText nameBox, emailBox, passwordBox;
    TextView signinButton;
    ImageButton showButton, nextButton;
    RadioButton doctor, employee, patient;
    AVLoadingIndicatorView progressBar;
    boolean pwShow = false;
    File imageFile = null;
    ArrayList<File> files = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        progressBar = (AVLoadingIndicatorView) findViewById(R.id.loading_bar);

        pictureFrame = (FrameLayout)findViewById(R.id.pictureFrame);
        nameBox = (EditText)findViewById(R.id.nameBox);
        emailBox = (EditText)findViewById(R.id.emailBox);
        passwordBox = (EditText)findViewById(R.id.passwordBox);

        signinButton = (TextView)findViewById(R.id.signinButton);

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
                registerMember();
            }
        });

        signinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        pictureFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(SignupActivity.this);
            }
        });

        setupUI(findViewById(R.id.activity), this);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                //From here you can load the image however you need to, I recommend using the Glide library
                imageFile = new File(resultUri.getPath());
                files.clear();
                files.add(imageFile);
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                    ((CircleImageView)findViewById(R.id.picture)).setImageBitmap(bitmap);
                    ((ImageView)findViewById(R.id.cameraButton)).setVisibility(View.GONE);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    private void registerMember(){

        if(nameBox.getText().length() == 0){
            showToast(getString(R.string.enter_name));
            return;
        }

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

        registerMember(files, nameBox.getText().toString().trim(), emailBox.getText().toString().trim(), passwordBox.getText().toString().trim(), role);

    }

    public void registerMember(ArrayList<File> files, String name, String email, String password, String role) {
        progressBar.setVisibility(View.VISIBLE);
        AndroidNetworking.upload(ReqConst.SERVER_URL + "registermember")
                .addMultipartFileList("files", files)
                .addMultipartParameter("name", name)
                .addMultipartParameter("email", email)
                .addMultipartParameter("password", password)
                .addMultipartParameter("age", "")
                .addMultipartParameter("role", role)
                .addMultipartParameter("patientID", (role.equals("patient")? RandomStringUtils.randomAlphanumeric(10): ""))
                .setPriority(Priority.HIGH)
                .build()
                .setUploadProgressListener(new UploadProgressListener() {
                    @Override
                    public void onProgress(long bytesUploaded, long totalBytes) {
                        // do anything with progress
                        Log.d("UPLOADED!!!", String.valueOf(bytesUploaded) + "/" + String.valueOf(totalBytes));
                    }
                })
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
                                showToast(getString(R.string.signup_success));

                                EasyPreference.with(getApplicationContext()).addString("email", Commons.thisUser.get_email()).save();
                                EasyPreference.with(getApplicationContext()).addString("role", Commons.thisUser.get_role()).save();
                                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                startActivity(intent);
                                finish();

                            }else if(result.equals("1")){
                                showToast(getString(R.string.existing_email));
                            }else if(result.equals("2")){
                                showToast(getString(R.string.existing_user));
                                finish();
                            }else {
                                showToast(getString(R.string.server_issue));
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

}


































