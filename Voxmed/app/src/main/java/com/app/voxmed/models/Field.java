package com.app.voxmed.models;

import android.view.View;

public class Field {
    int id = 0;
    int report_id = 0;
    String title = "";
    String content = "";
    View view = null;

    public Field(){}

    public void setView(View view) {
        this.view = view;
    }

    public View getView() {
        return view;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setReport_id(int report_id) {
        this.report_id = report_id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getId() {
        return id;
    }

    public int getReport_id() {
        return report_id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }
}
