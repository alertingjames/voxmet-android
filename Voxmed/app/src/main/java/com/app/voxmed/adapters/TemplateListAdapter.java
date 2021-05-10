package com.app.voxmed.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.voxmed.R;
import com.app.voxmed.commons.Commons;
import com.app.voxmed.main.TemplateSettingActivity;
import com.app.voxmed.models.Template;

import java.util.ArrayList;

public class TemplateListAdapter extends BaseAdapter {

    private Context _context;
    private ArrayList<Template> _datas = new ArrayList<>();
    private ArrayList<Template> _alldatas = new ArrayList<>();

    public TemplateListAdapter(Context context){

        super();
        this._context = context;
    }

    public void setDatas(ArrayList<Template> datas) {
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
            convertView = inflater.inflate(R.layout.item_template, parent, false);

            holder.nameBox = (TextView) convertView.findViewById(R.id.name);
            holder.countBox = (TextView) convertView.findViewById(R.id.count);
            holder.selectButton = (TextView) convertView.findViewById(R.id.btn_select);
            holder.detailButton = (TextView) convertView.findViewById(R.id.btn_detail);
            holder.deleteButton = (ImageView) convertView.findViewById(R.id.btn_delete);

            convertView.setTag(holder);
        } else {
            holder = (CustomHolder) convertView.getTag();
        }

        final Template entity = (Template) _datas.get(position);

        holder.nameBox.setText(entity.getName());
        holder.countBox.setText(String.valueOf(entity.getItems_count()));

        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Commons.templateListActivity != null){
                    Commons.templateListActivity.deleteTemplate(entity);
                }
            }
        });

        holder.selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Commons.templateListActivity != null){
                    Commons.template = entity;
                    Commons.templateListActivity.finish();
                }
            }
        });

        holder.detailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Commons.template = entity;
                Intent intent = new Intent(_context, TemplateSettingActivity.class);
                _context.startActivity(intent);
            }
        });

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

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
            for (Template template : _alldatas) {
                String value = template.getName().toLowerCase();
                if (value.contains(charText)) {
                    _datas.add(template);
                }
            }
            notifyDataSetChanged();
        }
    }

    class CustomHolder {
        TextView nameBox;
        TextView countBox;
        TextView selectButton, detailButton;
        ImageView deleteButton;
    }
}





