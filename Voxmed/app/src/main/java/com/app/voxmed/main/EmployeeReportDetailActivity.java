package com.app.voxmed.main;

import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityOptionsCompat;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
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
import com.makeramen.roundedimageview.RoundedImageView;
import com.squareup.picasso.Picasso;
import com.wang.avi.AVLoadingIndicatorView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class EmployeeReportDetailActivity extends BaseActivity {

    private Button submitButton;
    private CheckBox correctedBox;
    ImageView statusBox;

    private EditText resultBox;
    private LinearLayout container;

    AudioTrackPlayer audioTrackPlayer = null;
    File audioFile = null;

    private RoundedImageView pictureBox;

    AVLoadingIndicatorView progressBar;
    LinearLayout loadingLayout;

    private String downloadAudioPath;
    private String urlDownloadLink = "";

    private Button play, stop;

    String str = "";

    ArrayList<Field> fields = new ArrayList<>();
    ArrayList<View> views = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_report_detail);

        play = (Button) findViewById(R.id.play);
        stop = (Button) findViewById(R.id.stop);

        resultBox = (EditText)findViewById(R.id.resultBox);
        resultBox.setText(Html.fromHtml(Commons.report.get_body()));
        str = Commons.report.get_body();

        container = (LinearLayout) findViewById(R.id.container);
        statusBox = (ImageView)findViewById(R.id.ic_status);

        pictureBox = (RoundedImageView)findViewById(R.id.pictureBox);

        correctedBox = (CheckBox)findViewById(R.id.chk_corrected);

        progressBar = (AVLoadingIndicatorView) findViewById(R.id.loading_bar);
        loadingLayout = (LinearLayout) findViewById(R.id.loadingLayout);

        final Resources resources = getResources();
        final Resources.Theme theme = getTheme();

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        if(Commons.report.get_picture_url().length() > 0)
            Picasso.with(this)
                    .load(Commons.report.get_picture_url())
                    .error(R.mipmap.appicon)
                    .placeholder(R.mipmap.appicon)
                    .into(pictureBox);
        else {
            pictureBox.setVisibility(View.GONE);
        }

        pictureBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkProgressing())return;
                Intent intent = new Intent(getApplicationContext(), PictureListActivity.class);
                ActivityOptionsCompat options = ActivityOptionsCompat.
                        makeSceneTransitionAnimation(EmployeeReportDetailActivity.this, v, getString(R.string.transition));
                startActivity(intent, options.toBundle());
            }
        });

        if(Commons.report.get_status().length() > 0){
            statusBox.setVisibility(View.VISIBLE);
            correctedBox.setChecked(true);
        }else {
            statusBox.setVisibility(View.INVISIBLE);
            correctedBox.setChecked(false);
        }

        statusBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToast(getString(R.string.reviewed_and_corrected));
            }
        });

        submitButton = (Button) findViewById(R.id.submitButton);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkProgressing())return;
                if(fields.size() > 0 && !stop.isEnabled() && resultBox.getText().toString().trim().length() > 0) {
                    try {
                        String jsonStr = createTemplateJsonString();
                        if(jsonStr.length() > 0){
                            submitReport(jsonStr);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
//                    str = processString();
//                    submitReport(str);

                }else if(audioTrackPlayer != null)showToast(getString(R.string.please_stop_playing));
                else if(resultBox.getText().toString().trim().length() == 0){
                    showToast(getString(R.string.complete_report));
                }
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkProgressing())return;
                stop();
            }
        });

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkProgressing())return;
                playAudio();
            }
        });

        loadFile();
        getReportFields();

    }

    private String processString(){
        String[] splitStr = resultBox.getText().toString().trim().split("\\s+");
        String sss = "";
        for(String string:splitStr){
            Log.d("String", string);
            if(string.endsWith(":")){
                int i = 0;
                for(Field field:fields){
                    i++;
                    if(string.toLowerCase().replace(":","").equals(field.getTitle().toLowerCase())){
                        string = "<br><br><b>" + string + "</b><br>";
                        sss = sss + string;
                        break;
                    }else {
                        if(i == fields.size()){
                            sss = sss + (sss.length() == 0?"":" ") + string;
                        }
                    }
                }
            }else {
                sss = sss + (sss.length() == 0?"":" ") + string;
            }
        }
        return sss;
    }

    private boolean checkProgressing(){
        if(progressBar.getVisibility() == View.VISIBLE)
            return true;
        return false;
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
        stop();
    }

    @Override
    public void onResume() {
        super.onResume();
        resume();
//        getKeywords();
    }

    private void resume(){
        setButtonStatus(stop, false);
        setButtonStatus(play,true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        setButtonStatus(stop, false);
        setButtonStatus(play,true);
    }

    @Override
    protected void onStop() {
        // Stop listening to voice
        stop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Commons.doctorReportEditF = false;
        stop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stop();
    }

    private void stop(){
        if(audioTrackPlayer != null){
            audioTrackPlayer.stop();
            audioTrackPlayer = null;
            showToast(getString(R.string.playing_stopped));
        }
        resume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
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

    private void submitReport(String json_str){
        progressBar.setVisibility(View.VISIBLE);
        AndroidNetworking.post(ReqConst.SERVER_URL + "updatereport")
                .addBodyParameter("report_id", String.valueOf(Commons.report.get_idx()))
                .addBodyParameter("report", fields.get(fields.size() - 1).getContent())
                .addBodyParameter("status", (correctedBox.isChecked()? "corrected": ""))
                .addBodyParameter("temp_json_str", json_str)
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
                                showToast(getString(R.string.resubmitted));
                                finish();
                            }else if(result.equals("1")){
                                showToast(getString(R.string.unexisting_report));
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

                                    View view = getLayoutInflater().inflate(R.layout.layout_temp_field2, container, false);
                                    container.addView(view);
                                    field.setView(view);
                                    fields.add(field);
                                    views.add(view);
                                    final ImageView cancelButton = (ImageView)view.findViewById(R.id.btn_cancel);
                                    final TextView titleBox =(TextView) view.findViewById(R.id.titleBox);
                                    final EditText contentBox = (EditText) view.findViewById(R.id.contentBox);
                                    titleBox.setHint(getString(R.string.enter_keyword) + "(" + String.valueOf(fields.size() - 1) + ")");
                                    titleBox.setText(field.getTitle());
                                    contentBox.setHint(getString(R.string.enter_content) + "(" + String.valueOf(fields.size() - 1) + ")");
                                    contentBox.setText(field.getContent());

                                    cancelButton.setVisibility(View.GONE);

                                    cancelButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            int index = container.indexOfChild(view);
                                            container.removeViewAt(index);
                                            fields.remove(index);
                                            views.remove(index);
                                            for(View view1:views){
                                                ((TextView)view1.findViewById(R.id.titleBox)).setHint(getString(R.string.enter_keyword) + "(" + String.valueOf(views.indexOf(view1)) + ")");
                                                ((EditText)view1.findViewById(R.id.contentBox)).setHint(getString(R.string.enter_content) + "(" + String.valueOf(views.indexOf(view1)) + ")");
                                            }
                                        }
                                    });
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

    String tempStr = "";

    public String createTemplateJsonString()throws JSONException {

        tempStr = "";
        JSONObject jsonObj = null;
        JSONArray jsonArr = new JSONArray();
        if (fields.size()>0){
            for(int i=0; i<fields.size(); i++){

                Field field = fields.get(i);

                String title = ((TextView)field.getView().findViewById(R.id.titleBox)).getText().toString().trim();
                String content = ((EditText)field.getView().findViewById(R.id.contentBox)).getText().toString().trim();

                if(title.length() == 0 || content.length() == 0){
                    showToast(getString(R.string.fillinemptyfields));
                    return "";
                }

                field.setTitle(title);
                field.setContent(content);

                jsonObj=new JSONObject();

                try {
                    jsonObj.put("id", String.valueOf(field.getId()));
                    jsonObj.put("title",field.getTitle());
                    jsonObj.put("content",field.getContent());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                jsonArr.put(jsonObj);
            }
            JSONObject obj = new JSONObject();
            obj.put("fields", jsonArr);
            tempStr = obj.toString();
        }

        return tempStr;

    }

    private void getKeywords(){

        progressBar.setVisibility(View.VISIBLE);
        AndroidNetworking.post(ReqConst.SERVER_URL + "getKeywords")
                .addBodyParameter("member_id", String.valueOf(Commons.report.get_member_id()))
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
                                fields.clear();
                                views.clear();
                                container.removeAllViews();
                                JSONArray dataArr = response.getJSONArray("data");
                                for(int i=0; i<dataArr.length(); i++) {
                                    JSONObject object = (JSONObject) dataArr.get(i);
                                    Field field = new Field();
                                    field.setId(fields.size());
                                    field.setTitle(object.getString("keyword"));
                                    field.setContent("");

//                                    View view = getLayoutInflater().inflate(R.layout.layout_temp_field, container, false);
//                                    container.addView(view);
//                                    field.setView(view);

                                    fields.add(field);

//                                    views.add(view);
//                                    final ImageView cancelButton = (ImageView)view.findViewById(R.id.btn_cancel);
//                                    final TextView titleBox =( TextView) view.findViewById(R.id.titleBox);
//                                    final EditText contentBox = (EditText) view.findViewById(R.id.contentBox);
//                                    if(i == 0) {
//                                        contentBox.setFocusable(true);
//                                        contentBox.requestFocus();
//                                    }
//                                    titleBox.setText(field.getTitle());
//                                    cancelButton.setVisibility(View.GONE);
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
                        progressBar.setVisibility(View.GONE);
                    }
                });

    }

}




















































