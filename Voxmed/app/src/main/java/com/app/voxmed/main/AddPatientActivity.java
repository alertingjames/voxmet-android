package com.app.voxmed.main;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import com.bumptech.glide.Glide;
import com.iamhabib.easy_preference.EasyPreference;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.wang.avi.AVLoadingIndicatorView;

import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class AddPatientActivity extends BaseActivity {

    FrameLayout pictureFrame;
    CircleImageView pictureBox;
    EditText nameBox, emailBox, ageBox;
    Button submitButton;
    AVLoadingIndicatorView progressBar;
    File imageFile = null;
    ArrayList<File> files = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_patient);

        progressBar = (AVLoadingIndicatorView) findViewById(R.id.loading_bar);

        TextView titleBox = (TextView)findViewById(R.id.title);

        pictureFrame = (FrameLayout)findViewById(R.id.pictureFrame);
        pictureBox = (CircleImageView) findViewById(R.id.pictureBox);
        nameBox = (EditText)findViewById(R.id.nameBox);
        emailBox = (EditText)findViewById(R.id.emailBox);
        ageBox = (EditText)findViewById(R.id.ageBox);

        submitButton = (Button) findViewById(R.id.btn_submit);

        pictureFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(AddPatientActivity.this);
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerMember();
            }
        });

        setupUI(findViewById(R.id.activity), this);

        if(Commons.patient != null){
            titleBox.setText(getString(R.string.editpatient));
            Glide.with(getApplicationContext()).load(Commons.patient.get_picture_url()).into(pictureBox);
            nameBox.setText(Commons.patient.get_name());
            emailBox.setText(Commons.patient.get_email());
            emailBox.setEnabled(false);
            ageBox.setText(Commons.patient.get_age());
        }

    }

    public void back(View view){
        onBackPressed();
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
                    pictureBox.setImageBitmap(bitmap);
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

        if(ageBox.getText().length() == 0){
            showToast(getString(R.string.enter_age));
            return;
        }

        if(Commons.patient ==  null)registerMember(files, nameBox.getText().toString().trim(), emailBox.getText().toString().trim(), ageBox.getText().toString(), "patient");
        else updateMember(files, nameBox.getText().toString().trim(), ageBox.getText().toString());
    }

    public void registerMember(ArrayList<File> files, String name, String email, String age, String role) {
        progressBar.setVisibility(View.VISIBLE);
        AndroidNetworking.upload(ReqConst.SERVER_URL + "registermember")
                .addMultipartFileList("files", files)
                .addMultipartParameter("name", name)
                .addMultipartParameter("email", email)
                .addMultipartParameter("age", age)
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
                                showToast(getString(R.string.newpatientadded));
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

    public void updateMember(ArrayList<File> files, String name, String age) {
        progressBar.setVisibility(View.VISIBLE);
        AndroidNetworking.upload(ReqConst.SERVER_URL + "updatemember")
                .addMultipartFileList("files", files)
                .addMultipartParameter("member_id", String.valueOf(Commons.patient.get_idx()))
                .addMultipartParameter("name", name)
                .addMultipartParameter("age", age)
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
                                showToast(getString(R.string.patient_updated));
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































