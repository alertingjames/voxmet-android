package com.app.voxmed.adapters;

import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.app.voxmed.R;
import com.app.voxmed.commons.Commons;
import com.app.voxmed.main.EditReportActivity;
import com.app.voxmed.main.EmployeeReportDetailActivity;
import com.app.voxmed.main.PatientReportDetailActivity;
import com.app.voxmed.models.Report;
import com.app.voxmed.models.User;
import com.makeramen.roundedimageview.RoundedImageView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;

import de.hdodenhof.circleimageview.CircleImageView;

public class ReportListAdapter extends BaseAdapter {

    private Context _context;
    private ArrayList<Report> _datas = new ArrayList<>();
    private ArrayList<Report> _alldatas = new ArrayList<>();

    public ReportListAdapter(Context context){

        super();
        this._context = context;
    }

    public void setDatas(ArrayList<Report> datas) {
        _alldatas = datas;
        _datas.clear();
        _datas.addAll(_alldatas);
    }

    @Override
    public int getCount(){
        return _datas.size();
    }

    @Override
    public Object getItem(int position){
        return _datas.get(position);
    }

    @Override
    public long getItemId(int position){
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent){

        CustomHolder holder;

        if (convertView == null) {
            holder = new CustomHolder();

            LayoutInflater inflater = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            assert inflater != null;
            convertView = inflater.inflate(R.layout.item_report_list, parent, false);

            holder.pictureBox = (RoundedImageView) convertView.findViewById(R.id.pictureBox);
            holder.audioBox = (ImageView) convertView.findViewById(R.id.ic_audio);
            holder.statusBox = (ImageView) convertView.findViewById(R.id.ic_status);
            holder.reportBox = (TextView) convertView.findViewById(R.id.reportBox);
            holder.datetimeBox = (TextView) convertView.findViewById(R.id.datetimeBox);
            holder.patientIDBox = (TextView) convertView.findViewById(R.id.patientIDBox);
            holder.progressBar = (ProgressBar) convertView.findViewById(R.id.progressBar);

            convertView.setTag(holder);
        } else {
            holder = (CustomHolder) convertView.getTag();
        }

        final Report entity = (Report) _datas.get(position);

        holder.reportBox.setText(Html.fromHtml(entity.get_body()));
        holder.patientIDBox.setText(entity.get_patientID());
        if(entity.get_picture_url().length() > 0){
            Picasso.with(_context)
                    .load(entity.get_picture_url())
                    .error(R.mipmap.appicon)
                    .placeholder(R.mipmap.appicon)
                    .into(holder.pictureBox, new Callback() {
                        @Override
                        public void onSuccess() {
                            holder.progressBar.setVisibility(View.INVISIBLE);
                        }
                        @Override
                        public void onError() {
                            holder.progressBar.setVisibility(View.INVISIBLE);
                        }
                    });
            holder.audioBox.setVisibility(View.VISIBLE);
        }else {
            Picasso.with(_context)
                    .load(R.drawable.audio)
                    .error(R.mipmap.appicon)
                    .placeholder(R.mipmap.appicon)
                    .into(holder.pictureBox, new Callback() {
                        @Override
                        public void onSuccess() {
                            holder.progressBar.setVisibility(View.INVISIBLE);
                        }
                        @Override
                        public void onError() {
                            holder.progressBar.setVisibility(View.INVISIBLE);
                        }
                    });
            holder.audioBox.setVisibility(View.GONE);
        }

        String[] monthes={"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};

        Calendar c = Calendar.getInstance();
        //Set time in milliseconds
        c.setTimeInMillis(Long.parseLong(entity.get_date_time()));
        int mYear = c.get(Calendar.YEAR);
        int mMonth = c.get(Calendar.MONTH);
        int mDay = c.get(Calendar.DAY_OF_MONTH);
        int mHour = c.get(Calendar.HOUR_OF_DAY);
        int mMin = c.get(Calendar.MINUTE);
        if(mDay<10)
            holder.datetimeBox.setText(monthes[mMonth] + " 0" + mDay + ", " + mYear + " " + mHour + ":" + mMin);
        else
            holder.datetimeBox.setText(monthes[mMonth] + " " + mDay + ", " + mYear + " " + mHour + ":" + mMin);

        if(entity.get_status().length() > 0){
            holder.statusBox.setVisibility(View.VISIBLE);
        }else {
            holder.statusBox.setVisibility(View.GONE);
        }

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Commons.report = entity;
                if(Commons.thisUser.get_role().equals("doctor")){
                    Intent intent = new Intent(_context, EditReportActivity.class);
                    _context.startActivity(intent);
                }else if(Commons.thisUser.get_role().equals("patient")){
                    Intent intent = new Intent(_context, PatientReportDetailActivity.class);
                    _context.startActivity(intent);
                }else if(Commons.thisUser.get_role().equals("employee")){
                    Intent intent = new Intent(_context, EmployeeReportDetailActivity.class);
                    _context.startActivity(intent);
                }
            }
        });

        return convertView;
    }

    public void filter(String charText) {

        charText = charText.toLowerCase();
        _datas.clear();

        if (charText.length() == 0) {
            _datas.addAll(_alldatas);
        } else {
            for (Report report : _alldatas) {
                String value = report.get_body().toLowerCase();
                if (value.contains(charText)) {
                    _datas.add(report);
                } else {
                    value = report.get_patientID().toLowerCase();
                    if (value.contains(charText)) {
                        _datas.add(report);
                    } else {
                        String timeStamp = report.get_date_time();
                        String[] monthes = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

                        Calendar c = Calendar.getInstance();
                        //Set time in milliseconds
                        c.setTimeInMillis(Long.parseLong(timeStamp));
                        int mYear = c.get(Calendar.YEAR);
                        int mMonth = c.get(Calendar.MONTH);
                        int mDay = c.get(Calendar.DAY_OF_MONTH);
                        int mHour = c.get(Calendar.HOUR_OF_DAY);
                        int mMin = c.get(Calendar.MINUTE);
                        if (mDay < 10)
                            value = monthes[mMonth] + " 0" + mDay + ", " + mYear + " " + mHour + ":" + mMin;
                        else
                            value = monthes[mMonth] + " " + mDay + ", " + mYear + " " + mHour + ":" + mMin;
                        Log.d("DATETIME===>", value);
                        if (value.toLowerCase().contains(charText)) {
                            _datas.add(report);
                        }else {
                            value = report.get_status().toLowerCase();
                            if (value.contains(charText)) {
                                _datas.add(report);
                            }
                        }
                    }
                }
            }
            notifyDataSetChanged();
        }
    }

    class CustomHolder {
        RoundedImageView pictureBox;
        ImageView audioBox, statusBox;
        TextView reportBox, datetimeBox;
        TextView patientIDBox;
        ProgressBar progressBar;
    }
}





