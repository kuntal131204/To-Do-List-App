package com.example.todolist;

public class Task {
    public String title;
    public boolean isCompleted;
    public String date;
    public long timeMillis;
    public String soundUri;

    public Task(String title, boolean isCompleted, String date, long timeMillis, String soundUri) {
        this.title = title;
        this.isCompleted = isCompleted;
        this.date = date;
        this.timeMillis = timeMillis;
        this.soundUri = soundUri;
    }
}
