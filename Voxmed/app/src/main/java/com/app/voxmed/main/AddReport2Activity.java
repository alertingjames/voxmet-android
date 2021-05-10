package com.app.voxmed.main;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Html;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.androidnetworking.interfaces.UploadProgressListener;
import com.app.voxmed.R;
import com.app.voxmed.adapters.PictureListAdapter;
import com.app.voxmed.base.BaseActivity;
import com.app.voxmed.commons.Commons;
import com.app.voxmed.commons.ReqConst;
import com.app.voxmed.models.Field;
import com.app.voxmed.models.Picture;
import com.app.voxmed.models.User;
import com.googlecode.mp4parser.authoring.Edit;
import com.iamhabib.easy_preference.EasyPreference;
import com.rd.PageIndicatorView;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.wang.avi.AVLoadingIndicatorView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import au.com.bytecode.opencsv.CSVWriter;
import de.hdodenhof.circleimageview.CircleImageView;

public class AddReport2Activity extends BaseActivity {

    ImageView pictureBox;
    ImageButton cameraButton, cancelButton, patientIDListButton;
    Button submitButton, shareButton;
    EditText patientIDBox;
    File imageFile = null;
    LinearLayout loadingLayout;
    ArrayList<File> fileList = new ArrayList<>();

    ViewPager pager;
    PageIndicatorView pageIndicatorView;
    ArrayList<Picture> pictures = new ArrayList<>();
    PictureListAdapter adapter = new PictureListAdapter(this);

    String jsonStr = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_report2);

        jsonStr = getIntent().getStringExtra("jsonStr");

        loadingLayout = (LinearLayout) findViewById(R.id.loadingLayout);

        pager = findViewById(R.id.viewPager);
        pageIndicatorView = findViewById(R.id.pageIndicatorView);

        pictureBox = (ImageView)findViewById(R.id.picture);
        patientIDBox = (EditText)findViewById(R.id.patientIDBox);

        cameraButton = (ImageButton)findViewById(R.id.btn_camera);
        cancelButton = (ImageButton)findViewById(R.id.btn_cancel);
        patientIDListButton = (ImageButton)findViewById(R.id.btn_patient_list);
        submitButton = (Button)findViewById(R.id.btn_submit);
        shareButton = (Button)findViewById(R.id.btn_share);

        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(AddReport2Activity.this);
            }
        });

        patientIDListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Commons.editText = patientIDBox;
                Intent intent = new Intent(getApplicationContext(), PatientListActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.bottom_in, R.anim.top_out);
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(patientIDBox.getText().length() == 0){
                    showToast(getString(R.string.enter_patient_ID));
                    return;
                }

                fileList.clear();
                fileList.add(Commons.report.get_audio_file());

                if(pictures.size() > 0){
                    for(Picture picture:pictures){
                        fileList.add(picture.getFile());
                    }
                }

                submitReport();

            }
        });

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAlertDialogForSharing(AddReport2Activity.this, new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        showInputEmailAlertDialog();
                        return null;
                    }
                }, new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        String message = "";
                        for(Field field: Commons.fields){
                            message = message + (message.length() > 0?"\n":"") + field.getTitle() + ": " + field.getContent();
                        }
                        message = message + "\n\n" + Commons.thisUser.get_name();
                        Log.d("MESSAGE!!!", message);
                        shareReport(getString(R.string.app_name) + " " + getString(R.string.report_detail), message);
                        return null;
                    }
                });
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delCurrentItem();
            }
        });


        setupUI(findViewById(R.id.addreport_activity), this);
    }

    private void showInputEmailAlertDialog(){
        AlertDialog.Builder alert = new AlertDialog.Builder(AddReport2Activity.this);
        alert.setTitle(getString(R.string.enter_email));
// Set an EditText view to get user input
        final EditText input = new EditText(AddReport2Activity.this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setGravity(Gravity.CENTER);
        alert.setView(input);
        alert.setCancelable(false);
        alert.setPositiveButton(getString(R.string.submit), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();
                if(value.length() == 0){
                    showToast(getString(R.string.enter_email));
                    return;
                }
                if(!isValidEmail(value)){
                    showToast(getString(R.string.enter_valid_email));
                    return;
                }
                submitReportToEmail(value);
            }
        });

        alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }

    private void submitReportToEmail(String email){
        loadingLayout.setVisibility(View.VISIBLE);
        AndroidNetworking.upload(ReqConst.SERVER_URL + "mailReport")
                .addMultipartParameter("to_email", email)
                .addMultipartParameter("from_email", Commons.thisUser.get_email())
                .addMultipartParameter("temp_json_str", jsonStr)
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
                        loadingLayout.setVisibility(View.GONE);
                        try {
                            String result = response.getString("result_code");
                            if(result.equals("0")){
                                showToast(getString(R.string.submitted));
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
                        loadingLayout.setVisibility(View.GONE);
                        showToast(error.getErrorDetail());
                    }
                });
    }

    public void back(View view){
        onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
//                try {
//                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
//                    pictureBox.setImageBitmap(bitmap);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }

                File imageFile = new File(resultUri.getPath());
                Picture picture = new Picture();
                picture.setFile(imageFile);
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                    picture.setBitmap(bitmap);
                    picture.setIdx(pictures.size());
                    pictures.add(picture);
                    adapter.setDatas(pictures);
                    adapter.notifyDataSetChanged();
                    pager.setAdapter(adapter);
                    pager.setCurrentItem(pictures.size());
                    if(pictureBox.getVisibility() == View.VISIBLE)pictureBox.setVisibility(View.GONE);
                    if(cancelButton.getVisibility() == View.GONE)cancelButton.setVisibility(View.VISIBLE);
                } catch (IOException e) {
                    e.printStackTrace();
                }


            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    public void delCurrentItem(){
        if(pictures.size() > 0){
            pictures.remove(pager.getCurrentItem());
            if(pictures.size() == 0) {
                if(pictureBox.getVisibility() == View.GONE)pictureBox.setVisibility(View.VISIBLE);
                if(cancelButton.getVisibility() == View.VISIBLE)cancelButton.setVisibility(View.GONE);
            }
            adapter.setDatas(pictures);
            adapter.notifyDataSetChanged();
            pager.setAdapter(adapter);
        }
    }

    private void shareReport(String subject, String body) {
        Intent txtIntent = new Intent(android.content.Intent.ACTION_SEND);
        txtIntent .setType("text/plain");
        txtIntent .putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
        txtIntent .putExtra(Intent.EXTRA_TEXT, body);
        startActivity(Intent.createChooser(txtIntent ,"Share with..."));
    }

    private void submitReport(){

        loadingLayout.setVisibility(View.VISIBLE);
        AndroidNetworking.upload(ReqConst.SERVER_URL + "postreport")
                .addMultipartFileList("files", fileList)
                .addMultipartParameter("member_id", String.valueOf(Commons.thisUser.get_idx()))
                .addMultipartParameter("report", Commons.report.get_body())
                .addMultipartParameter("patientID", patientIDBox.getText().toString())
                .addMultipartParameter("status", (Commons.report.is_corrected()? "corrected": ""))
                .addMultipartParameter("temp_json_str", jsonStr)
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
                        loadingLayout.setVisibility(View.GONE);
                        try {
                            String result = response.getString("result_code");
                            if(result.equals("0")){
                                showToast(getString(R.string.submitted));
                            }else if(result.equals("1")){
                                showToast(getString(R.string.unexisting_patient));
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
                        loadingLayout.setVisibility(View.GONE);
                        showToast(error.getErrorDetail());
                    }
                });

    }

    public void exportReport(View view){
        ExportDatabaseCSVTask task = new ExportDatabaseCSVTask();
        task.execute();
    }

    private class ExportDatabaseCSVTask extends AsyncTask<String ,String, String> {
        private final ProgressDialog dialog = new ProgressDialog(AddReport2Activity.this);
        @Override
        protected void onPreExecute() {
            this.dialog.setMessage("Exporting database...");
            this.dialog.show();
        }

        protected String doInBackground(final String... args){
            File exportDir = new File(Environment.getExternalStorageDirectory(), "");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            File file = new File(exportDir, "VoxmedReport.csv");
            try {

                file.createNewFile();
                CSVWriter csvWrite = new CSVWriter(new FileWriter(file));

                ArrayList<String> rowData = new ArrayList<>();

                rowData.add(getString(R.string.patient_ID));
                rowData.add(Commons.patient.get_patientID());

                String [] data = rowData.toArray(new String[rowData.size()]);
                csvWrite.writeNext(data);

                rowData = new ArrayList<>();

                rowData.add(getString(R.string.patient_name));
                rowData.add(Commons.patient.get_name());

                data = rowData.toArray(new String[rowData.size()]);
                csvWrite.writeNext(data);

                if (Commons.fields.size()>0) {
                    for (int i = 0; i < Commons.fields.size(); i++) {

                        Field field = Commons.fields.get(i);

                        String title = ((TextView) field.getView().findViewById(R.id.titleBox)).getText().toString().trim();
                        String content = ((EditText) field.getView().findViewById(R.id.contentBox)).getText().toString().trim();

                        if (title.length() == 0 || content.length() == 0) {
                            showToast(getString(R.string.fillinemptyfields));
                            return "";
                        }

                        rowData = new ArrayList<>();

                        rowData.add(title);
                        rowData.add(content);

                        data = rowData.toArray(new String[rowData.size()]);
                        csvWrite.writeNext(data);
                    }
                }

                csvWrite.close();
                return "";
            }
            catch (IOException e){
                Log.e("EditReportActivity", e.getMessage(), e);
                return "";
            }
        }

        @SuppressLint("NewApi")
        @Override
        protected void onPostExecute(final String success) {

            if (this.dialog.isShowing()){
                this.dialog.dismiss();
            }
            if (success.isEmpty()){
                Toast.makeText(AddReport2Activity.this, getString(R.string.export_success), Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(AddReport2Activity.this, getString(R.string.export_failed), Toast.LENGTH_SHORT).show();
            }
        }
    }

}







































