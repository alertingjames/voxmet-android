package com.app.voxmed.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
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
import com.app.voxmed.main.AddPatientActivity;
import com.app.voxmed.models.User;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.Calendar;

import de.hdodenhof.circleimageview.CircleImageView;

public class PatientListAdapter extends BaseAdapter {

    private Context _context;
    private ArrayList<User> _datas = new ArrayList<>();
    private ArrayList<User> _alldatas = new ArrayList<>();

    public PatientListAdapter(Context context){

        super();
        this._context = context;
    }

    public void setDatas(ArrayList<User> datas) {
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
            convertView = inflater.inflate(R.layout.item_patient_list, parent, false);

            holder.pictureBox = (CircleImageView) convertView.findViewById(R.id.pictureBox);
            holder.nameBox = (TextView) convertView.findViewById(R.id.nameBox);
            holder.patientIDBox = (TextView) convertView.findViewById(R.id.patientIDBox);
            holder.progressBar = (ProgressBar) convertView.findViewById(R.id.progressBar);

            convertView.setTag(holder);
        } else {
            holder = (CustomHolder) convertView.getTag();
        }

        final User entity = (User) _datas.get(position);

        holder.nameBox.setText(entity.get_name());
        holder.patientIDBox.setText(entity.get_patientID());
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

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Commons.patientListActivity != null){
                    Commons.patient = entity;
                    Commons.editText.setText(entity.get_patientID());
                    Commons.patientListActivity.onBackPressed();
                }
                else{
                    Commons.patient = entity;
                    Intent intent = new Intent(_context, AddPatientActivity.class);
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
            for (User user : _alldatas) {
                String value = user.get_name().toLowerCase();
                if (value.contains(charText)) {
                    _datas.add(user);
                } else {
                    value = user.get_patientID().toLowerCase();
                    if (value.contains(charText)) {
                        _datas.add(user);
                    } else {
                        String timeStamp = user.get_registered_time();
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
                            _datas.add(user);
                        }else {
                            value = user.get_status().toLowerCase();
                            if (value.contains(charText)) {
                                _datas.add(user);
                            }
                        }
                    }
                }
            }
            notifyDataSetChanged();
        }
    }

    class CustomHolder {
        CircleImageView pictureBox;
        TextView nameBox;
        TextView patientIDBox;
        ProgressBar progressBar;
    }
}





