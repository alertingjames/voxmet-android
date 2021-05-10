package com.app.voxmed.main;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.res.ResourcesCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.app.voxmed.R;
import com.app.voxmed.base.BaseActivity;
import com.app.voxmed.classes.AudioTrackPlayer;
import com.app.voxmed.classes.MessageDialogFragment;
import com.app.voxmed.classes.SpeechService;
import com.app.voxmed.classes.VoiceRecorder;
import com.app.voxmed.commons.Commons;
import com.app.voxmed.commons.ReqConst;
import com.app.voxmed.models.Field;
import com.app.voxmed.models.User;
import com.makeramen.roundedimageview.RoundedImageView;
import com.squareup.picasso.Picasso;
import com.wang.avi.AVLoadingIndicatorView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import au.com.bytecode.opencsv.CSVWriter;

public class EditReportActivity extends BaseActivity implements MessageDialogFragment.Listener {

    private static final String FRAGMENT_MESSAGE_DIALOG = "message_dialog";

    private static final String STATE_RESULTS = "results";

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;

    private SpeechService mSpeechService;

    private VoiceRecorder mVoiceRecorder;
    private final VoiceRecorder.Callback mVoiceCallback = new VoiceRecorder.Callback() {

        @Override
        public void onVoiceStart() {
            showStatus(true);
            if (mSpeechService != null) {
                mSpeechService.startRecognizing(mVoiceRecorder.getSampleRate());
            }
        }

        @Override
        public void onVoice(byte[] data, int size) {
            if (mSpeechService != null) {
                mSpeechService.recognize(data, size);
            }
        }

        @Override
        public void onVoiceEnd() {
            showStatus(false);
            if (mSpeechService != null) {
                mSpeechService.finishRecognizing();
            }
        }

    };

    // Resource caches
    private int mColorHearing;
    private int mColorNotHearing;

    // View references
    private TextView mStatus;
    private TextView mText;

    private EditText resultBox;
    private LinearLayout container;
    private TextView addButton;
    private ImageButton submitButton, shareButton;
    private FrameLayout itemResultBox;
    private CheckBox correctedBox;
    ImageView statusBox;

    AudioTrackPlayer audioTrackPlayer = null;
    File audioFile = null;

    private RoundedImageView pictureBox;

    AVLoadingIndicatorView progressBar;
    LinearLayout loadingLayout;

    private String downloadAudioPath;
    private String urlDownloadLink = "";

    ArrayList<Field> fields = new ArrayList<>();

    private Button play, stop, record;

    String str = "";

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            mSpeechService = SpeechService.from(binder);
            mSpeechService.addListener(mSpeechServiceListener);
            mStatus.setVisibility(View.VISIBLE);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mSpeechService = null;
        }

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_report);

        Commons.doctorReportEditF = true;

        play = (Button) findViewById(R.id.play);
        stop = (Button) findViewById(R.id.stop);
        record = (Button) findViewById(R.id.record);

        resultBox = (EditText)findViewById(R.id.resultBox);
        resultBox.setText(Html.fromHtml(Commons.report.get_body()));
        str = Commons.report.get_body();

        statusBox = (ImageView)findViewById(R.id.ic_status);

        pictureBox = (RoundedImageView)findViewById(R.id.pictureBox);

        itemResultBox = (FrameLayout) findViewById(R.id.item_result);
        correctedBox = (CheckBox)findViewById(R.id.chk_corrected);

        progressBar = (AVLoadingIndicatorView) findViewById(R.id.loading_bar);
        loadingLayout = (LinearLayout) findViewById(R.id.loadingLayout);

        container = (LinearLayout) findViewById(R.id.container);
        addButton = (TextView) findViewById(R.id.btn_add);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), TemplateSettingActivity.class);
                startActivity(intent);
            }
        });

        final Resources resources = getResources();
        final Resources.Theme theme = getTheme();
        mColorHearing = ResourcesCompat.getColor(resources, R.color.status_hearing, theme);
        mColorNotHearing = ResourcesCompat.getColor(resources, R.color.status_not_hearing, theme);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        mStatus = (TextView) findViewById(R.id.status);
        mText = (TextView) findViewById(R.id.text);

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
                        makeSceneTransitionAnimation(EditReportActivity.this, v, getString(R.string.transition));
                startActivity(intent, options.toBundle());
            }
        });

        if(Commons.report.get_status().length() > 0){
            statusBox.setVisibility(View.VISIBLE);
            correctedBox.setChecked(true);
        }else {
            statusBox.setVisibility(View.GONE);
            correctedBox.setChecked(false);
        }

        statusBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToast(getString(R.string.reviewed_and_corrected));
            }
        });

        submitButton = (ImageButton) findViewById(R.id.submitButton);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkProgressing())return;
                if(fields.size() > 0 && !stop.isEnabled()) {
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

                }else if(mVoiceRecorder != null)showToast(getString(R.string.please_stop_recording));
                else if(audioTrackPlayer != null)showToast(getString(R.string.please_stop_playing));
                else showToast(getString(R.string.please_record_report));
            }
        });

        shareButton = (ImageButton) findViewById(R.id.shareButton);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareReport("Voxmed Report", processString());
            }
        });

        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkProgressing())return;
                // Prepare Cloud Speech API
                bindService(new Intent(getApplicationContext(), SpeechService.class), mServiceConnection, BIND_AUTO_CREATE);

                // Start listening to voices
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED) {
                    startVoiceRecorder();
                } else if (ActivityCompat.shouldShowRequestPermissionRationale(EditReportActivity.this,
                        Manifest.permission.RECORD_AUDIO)) {
                    showPermissionMessageDialog();
                } else {
                    ActivityCompat.requestPermissions(EditReportActivity.this, new String[]{Manifest.permission.RECORD_AUDIO},
                            REQUEST_RECORD_AUDIO_PERMISSION);
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

    ArrayList<View> views = new ArrayList<>();

    private void addNewField(){
        View view = getLayoutInflater().inflate(R.layout.layout_temp_field, container, false);
        container.addView(view);
        Field field = new Field();
        field.setView(view);
        fields.add(field);
        views.add(view);
        final ImageView cancelButton = (ImageView)view.findViewById(R.id.btn_cancel);
        final TextView titleBox =(TextView) view.findViewById(R.id.titleBox);
        final EditText contentBox = (EditText) view.findViewById(R.id.contentBox);
        titleBox.setHint(getString(R.string.enter_keyword) + "(" + String.valueOf(fields.size() - 1) + ")");
        titleBox.setFocusable(true);
        titleBox.setFocusableInTouchMode(true);
        titleBox.requestFocus();
        contentBox.setHint(getString(R.string.enter_content) + "(" + String.valueOf(fields.size() - 1) + ")");

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

    private void shareReport(String subject, String body) {
        Intent txtIntent = new Intent(android.content.Intent.ACTION_SEND);
        txtIntent .setType("text/plain");
        txtIntent .putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
        txtIntent .putExtra(android.content.Intent.EXTRA_TEXT, Html.fromHtml(body));
        startActivity(Intent.createChooser(txtIntent ,"Share with..."));
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

        getAllKeywords();
        getPatient();
        resume();
    }

    private void resume(){

        setButtonStatus(record, true);
        setButtonStatus(stop, false);
        setButtonStatus(play,true);

    }

    @Override
    protected void onStart() {
        super.onStart();

        setButtonStatus(record, true);
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
        stopVoiceRecorder();
        // Stop Cloud Speech API
        if(mSpeechService != null){
            mSpeechService.removeListener(mSpeechServiceListener);
            unbindService(mServiceConnection);
            mSpeechService = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (permissions.length == 1 && grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                startVoiceRecorder();
            } else {
                showPermissionMessageDialog();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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

    private void startVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
        }
        mVoiceRecorder = new VoiceRecorder(mVoiceCallback);
        mVoiceRecorder.start();

        showToast(getString(R.string.recording_started));

        setButtonStatus(record, false);
        setButtonStatus(stop, true);
        setButtonStatus(play, false);

        itemResultBox.setVisibility(View.VISIBLE);
    }

    private void stopVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
            mVoiceRecorder = null;
            showToast(getString(R.string.recording_stopped));
            itemResultBox.setVisibility(View.GONE);
        }else if(audioTrackPlayer != null){
            audioTrackPlayer.stop();
            audioTrackPlayer = null;
            showToast(getString(R.string.playing_stopped));
        }

        resume();
    }

    private void showPermissionMessageDialog() {
        MessageDialogFragment
                .newInstance(getString(R.string.permission_message))
                .show(getSupportFragmentManager(), FRAGMENT_MESSAGE_DIALOG);
    }

    private void showStatus(final boolean hearingVoice) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStatus.setTextColor(hearingVoice ? mColorHearing : mColorNotHearing);
            }
        });
    }

    @Override
    public void onMessageDialogDismissed() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                REQUEST_RECORD_AUDIO_PERMISSION);
    }

    private final SpeechService.Listener mSpeechServiceListener =
            new SpeechService.Listener() {
                @Override
                public void onSpeechRecognized(final String text, final boolean isFinal) {
                    if (isFinal) {
                        mVoiceRecorder.dismiss();
                    }
                    if (mText != null && !TextUtils.isEmpty(text)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (isFinal) {
                                    mText.setText(null);

                                    String result = text;
                                    if(text.toLowerCase().equals(getString(R.string.new_line))) result = "\n";
                                    else if(text.toLowerCase().equals(getString(R.string.point))) result = ". ";
                                    else if(text.toLowerCase().equals(getString(R.string.comma))) result = ", ";
                                    else if(text.toLowerCase().equals(getString(R.string.space))) result = " ";
//                                    else if(text.toLowerCase().equals(getString(R.string.next_))) result = " ";

                                    int i = 0;

//                                    for(Field field:fields){
//                                        i++;
//                                        if(text.toLowerCase().equals(field.getTitle().toLowerCase())){
//                                            String insertStr = (resultBox.getSelectionStart() == 0? "":"<br><br>") + "<b>" + capitalize(result) + ":</b><br>";
//                                            resultBox.getText().insert(resultBox.getSelectionStart(), Html.fromHtml(insertStr));
//                                            break;
//                                        }else {
//                                            if(i == fields.size()){
//                                                resultBox.getText().insert(resultBox.getSelectionStart(), result);
//                                            }
//                                        }
//                                    }

                                    for (View view : views){

                                        i++;

                                        TextView titleBox = (TextView)view.findViewById(R.id.titleBox);
                                        EditText contentBox = (EditText)view.findViewById(R.id.contentBox);

                                        if(result.toLowerCase().equals(titleBox.getText().toString().toLowerCase())){
                                            contentBox.setFocusable(true);
                                            contentBox.requestFocus();
                                            contentBox.setSelection(contentBox.getText().length());
                                            break;
                                        }else {
                                            if(i == views.size()){
                                                for (View view1 : views){
                                                    EditText contentBox1 = (EditText)view1.findViewById(R.id.contentBox);
                                                    if(contentBox1.isFocused()){
                                                        contentBox1.getText().insert(contentBox1.getSelectionStart(), (contentBox1.getText().toString().endsWith(".")? " " + capitalize(result):result));
                                                    }
                                                }
                                            }
                                        }
                                    }

                                } else {
                                    mText.setText(text);
                                }
                            }
                        });
                    }
                }
            };

    private void playAudio(){
        if(audioFile != null && audioFile.exists()){
            showToast(getString(R.string.audio_playing));
            audioTrackPlayer = new AudioTrackPlayer();
            audioTrackPlayer.prepare(audioFile.getAbsolutePath());
            audioTrackPlayer.play();
            setButtonStatus(play, false);
            setButtonStatus(stop, true);
            setButtonStatus(record, false);
        }
    }

    private String capitalize(String string){

        String[] strArray = string.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String s : strArray) {
            String cap = s.substring(0, 1).toUpperCase() + s.substring(1);
            builder.append(cap);
        }
        return builder.toString();
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

                                    View view = getLayoutInflater().inflate(R.layout.layout_temp_field, container, false);
                                    container.addView(view);
                                    field.setView(view);
                                    fields.add(field);
                                    views.add(view);
                                    final ImageView cancelButton = (ImageView)view.findViewById(R.id.btn_cancel);
                                    final TextView titleBox =(TextView) view.findViewById(R.id.titleBox);
                                    final EditText contentBox = (EditText) view.findViewById(R.id.contentBox);
                                    titleBox.setText(field.getTitle());
                                    titleBox.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            openKeywordMenu(keywordList, titleBox, EditReportActivity.this);
                                        }
                                    });
                                    contentBox.setText(field.getContent());

                                    cancelButton.setVisibility(View.GONE);
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

    ArrayList<String> keywordList = new ArrayList<>();

    private void getAllKeywords(){
        AndroidNetworking.post(ReqConst.SERVER_URL + "getAllKeywords")
                .addBodyParameter("member_id", String.valueOf(Commons.thisUser.get_idx()))
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
                                keywordList.clear();
                                JSONArray dataArr = response.getJSONArray("data");
                                for(int i=0; i<dataArr.length(); i++) {
                                    JSONObject object = (JSONObject) dataArr.get(i);
                                    keywordList.add(object.getString("keyword"));
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
                    }
                });

    }

    public void exportReport(View view){
        ExportDatabaseCSVTask task = new ExportDatabaseCSVTask();
        task.execute();
    }

    private class ExportDatabaseCSVTask extends AsyncTask<String ,String, String>{
        private final ProgressDialog dialog = new ProgressDialog(EditReportActivity.this);
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
                rowData.add(Commons.report.get_patientID());

                String [] data = rowData.toArray(new String[rowData.size()]);
                csvWrite.writeNext(data);

                rowData = new ArrayList<>();

                rowData.add(getString(R.string.patient_name));
                rowData.add(patient.get_name());

                data = rowData.toArray(new String[rowData.size()]);
                csvWrite.writeNext(data);

                if (fields.size()>0) {
                    for (int i = 0; i < fields.size(); i++) {

                        Field field = fields.get(i);

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
                Toast.makeText(EditReportActivity.this, getString(R.string.export_success), Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(EditReportActivity.this, getString(R.string.export_failed), Toast.LENGTH_SHORT).show();
            }
        }
    }

    User patient = new User();

    private void getPatient(){
        AndroidNetworking.post(ReqConst.SERVER_URL + "getpatient")
                .addBodyParameter("patientID", Commons.report.get_patientID())
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
                                JSONObject object = (JSONObject) response.getJSONObject("data");
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
                                patient = user;
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

}




















































