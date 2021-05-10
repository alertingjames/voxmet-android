package com.app.voxmed.models;

public class Keyword {
    int id = 0;
    int userId = 0;
    String keyword = "";

    public Keyword(){}

    public void setId(int id) {
        this.id = id;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public String getKeyword() {
        return keyword;
    }
}
