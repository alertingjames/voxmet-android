package com.app.voxmed.main;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.app.voxmed.R;
import com.app.voxmed.base.BaseActivity;
import com.app.voxmed.classes.AudioTrackPlayer;
import com.app.voxmed.commons.Commons;
import com.app.voxmed.commons.ReqConst;
import com.app.voxmed.models.Field;
import com.app.voxmed.models.User;
import com.iamhabib.easy_preference.EasyPreference;
import com.makeramen.roundedimageview.RoundedImageView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class PatientReportDetailActivity extends BaseActivity {

    RoundedImageView pictureBox;
    ImageView statusBox;
    TextView datetimeBox;
    Button play, stop;
    AudioTrackPlayer audioTrackPlayer = null;
    File audioFile = null;

    EditText resultBox;
    private LinearLayout container;
    ArrayList<Field> fields = new ArrayList<>();
    ArrayList<View> views = new ArrayList<>();

    LinearLayout loadingLayout;
    private String downloadAudioPath;
    private String urlDownloadLink = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_report_detail);

        loadingLayout = (LinearLayout) findViewById(R.id.loadingLayout);

        pictureBox = (RoundedImageView)findViewById(R.id.pictureBox);

        resultBox = (EditText)findViewById(R.id.resultBox);
        resultBox.setText(Html.fromHtml(Commons.report.get_body()));

        container = (LinearLayout) findViewById(R.id.container);
        statusBox = (ImageView)findViewById(R.id.ic_status);
        datetimeBox = (TextView)findViewById(R.id.datetimeBox);
        play = (Button)findViewById(R.id.play);
        stop = (Button)findViewById(R.id.stop);

        String[] monthes={"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};

        Calendar c = Calendar.getInstance();
        //Set time in milliseconds
        c.setTimeInMillis(Long.parseLong(Commons.report.get_date_time()));
        int mYear = c.get(Calendar.YEAR);
        int mMonth = c.get(Calendar.MONTH);
        int mDay = c.get(Calendar.DAY_OF_MONTH);
        int mHour = c.get(Calendar.HOUR_OF_DAY);
        int mMin = c.get(Calendar.MINUTE);
        if(mDay<10)
            datetimeBox.setText(monthes[mMonth] + " 0" + mDay + ", " + mYear + " " + mHour + ":" + mMin);
        else
            datetimeBox.setText(monthes[mMonth] + " " + mDay + ", " + mYear + " " + mHour + ":" + mMin);

        if(Commons.report.get_picture_url().length() > 0)
            Picasso.with(this)
                    .load(Commons.report.get_picture_url())
                    .error(R.mipmap.appicon)
                    .placeholder(R.mipmap.appicon)
                    .into(pictureBox);
        else {
            getDoctor(String.valueOf(Commons.report.get_member_id()));
        }

        if(Commons.report.get_status().length() > 0){
            statusBox.setVisibility(View.VISIBLE);
        }else {
            statusBox.setVisibility(View.GONE);
        }

        statusBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToast(getString(R.string.reviewed_and_corrected));
            }
        });

        pictureBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Commons.report.get_picture_url().length() > 0) {
                    Intent intent = new Intent(getApplicationContext(), PictureListActivity.class);
                    ActivityOptionsCompat options = ActivityOptionsCompat.
                            makeSceneTransitionAnimation(PatientReportDetailActivity.this, v, getString(R.string.transition));
                    startActivity(intent, options.toBundle());
                }
            }
        });

        play.setOnClickListener(new View.OnClickListener() {
            @Override

            public void onClick(View v) {
                playAudio();
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopAudio();
            }
        });

        loadFile();
        getReportFields();

    }

    private void loadFile(){
        downloadAudioPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        File audioVoice = new File(downloadAudioPath + File.separator + "voxmed");
        if(!audioVoice.exists()){
            audioVoice.mkdir();
        }

        if (!audioVoice.exists() && !audioVoice.mkdir()) {

        }

        urlDownloadLink = Commons.report.get_audio_url();
        String filename = extractFilename();
        downloadAudioPath = downloadAudioPath + File.separator + "voxmed" + File.separator + filename;
        DownloadFile downloadAudioFile = new DownloadFile();
        downloadAudioFile.execute(urlDownloadLink, downloadAudioPath);
    }

    public void back(View view){
        onBackPressed();
    }

    private void playAudio(){
        if(audioFile != null && audioFile.exists()){
            showToast(getString(R.string.audio_playing));
            audioTrackPlayer = new AudioTrackPlayer();
            audioTrackPlayer.prepare(audioFile.getAbsolutePath());
            audioTrackPlayer.play();
            setButtonStatus(play, false);
            setButtonStatus(stop, true);
        }
    }

    private void stopAudio(){
        if(audioTrackPlayer != null){
            audioTrackPlayer.stop();
            audioTrackPlayer = null;
            setButtonStatus(play, true);
            setButtonStatus(stop, false);
            showToast(getString(R.string.playing_stopped));
        }
    }

    private void setButtonStatus(Button button, boolean status){
        button.setEnabled(status);
        if(status){
            button.setTextColor(getColor(R.color.white));
            button.setTypeface(Typeface.DEFAULT_BOLD);
        }else {
            button.setTextColor(getColor(R.color.button_unabled));
            button.setTypeface(Typeface.DEFAULT);
        }
    }

    private void getDoctor(String memberId){

        AndroidNetworking.post(ReqConst.SERVER_URL + "getmember")
                .addBodyParameter("member_id", memberId)
                .setPriority(Priority.HIGH)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // do anything with response
                        Log.d("RESPONSE!!!", response.toString());
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

                                Picasso.with(getApplicationContext())
                                        .load(user.get_picture_url())
                                        .into(pictureBox);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(ANError error) {
                        // handle error
                        Log.d("ERROR!!!", error.getErrorBody());
                    }
                });

    }

    private class DownloadFile extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... url) {
            int count;
            try {
                URL urls = new URL(url[0]);
                URLConnection connection = urls.openConnection();
                connection.connect();
                // this will be useful so that you can show a tipical 0-100% progress bar
                int lenghtOfFile = connection.getContentLength();

                InputStream input = new BufferedInputStream(urls.openStream());
                OutputStream output = new FileOutputStream(url[1]);

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    publishProgress((int) (total * 100 / lenghtOfFile));
                    output.write(data, 0, count);
                }
                output.flush();
                output.close();
                input.close();
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loadingLayout.setVisibility(ProgressBar.VISIBLE);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            loadingLayout.setVisibility(ProgressBar.GONE);
            audioFile = new File(downloadAudioPath);
        }
    }

    private String extractFilename(){
        if(urlDownloadLink.equals("")){
            return "";
        }
        String newFilename = "";
        if(urlDownloadLink.contains("/")){
            int dotPosition = urlDownloadLink.lastIndexOf("/");
            newFilename = urlDownloadLink.substring(dotPosition + 1, urlDownloadLink.length());
        }
        else{
            newFilename = urlDownloadLink;
        }
        return newFilename;
    }

    private void getReportFields(){

//        progressBar.setVisibility(View.VISIBLE);

        AndroidNetworking.post(ReqConst.SERVER_URL + "getReportFields")
                .addBodyParameter("report_id", String.valueOf(Commons.report.get_idx()))
                .setPriority(Priority.HIGH)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // do anything with response
                        Log.d("RESPONSE!!!", response.toString());
//                        loadingLayout.setVisibility(View.GONE);
                        try {
                            String result = response.getString("result_code");
                            if(result.equals("0")){
                                fields.clear();
                                views.clear();
                                container.removeAllViews();
                                JSONArray dataArr = response.getJSONArray("data");
                                for(int i=0; i<dataArr.length(); i++) {
                                    JSONObject object = (JSONObject) dataArr.get(i);
                                    Field field = new Field();
                                    field.setId(object.getInt("id"));
                                    field.setTitle(object.getString("title"));
                                    field.setContent(object.getString("content"));

                                    View view = getLayoutInflater().inflate(R.layout.layout_temp_view_field, container, false);
                                    container.addView(view);
                                    field.setView(view);
                                    fields.add(field);
                                    views.add(view);
                                    final TextView titleBox =( TextView) view.findViewById(R.id.titleBox);
                                    titleBox.setPaintFlags(titleBox.getPaintFlags()| Paint.UNDERLINE_TEXT_FLAG);
                                    final TextView contentBox = (TextView) view.findViewById(R.id.contentBox);
                                    titleBox.setHint(getString(R.string.enter_keyword) + "(" + String.valueOf(fields.size() - 1) + ")");
                                    titleBox.setText(field.getTitle());
                                    contentBox.setHint(getString(R.string.enter_content) + "(" + String.valueOf(fields.size() - 1) + ")");
                                    contentBox.setText(field.getContent());
                                }

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(ANError error) {
                        // handle error
                        Log.d("ERROR!!!", error.getErrorBody());
//                        loadingLayout.setVisibility(View.GONE);
                    }
                });

    }

}

































