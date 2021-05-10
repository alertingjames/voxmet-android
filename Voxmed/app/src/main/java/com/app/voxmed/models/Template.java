package com.app.voxmed.models;

public class Template {
    int id = 0;
    int user_id = 0;
    String name = "";
    int items_count = 0;

    public Template(){}

    public void setId(int id) {
        this.id = id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setItems_count(int items_count) {
        this.items_count = items_count;
    }

    public int getId() {
        return id;
    }

    public int getUser_id() {
        return user_id;
    }

    public String getName() {
        return name;
    }

    public int getItems_count() {
        return items_count;
    }
}
