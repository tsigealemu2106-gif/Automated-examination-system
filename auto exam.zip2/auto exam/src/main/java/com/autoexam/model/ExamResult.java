package com.autoexam.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.util.Map;

public class ExamResult {
    private String username;
    private String dateTime;
    private int score;
    private int total;
    private int durationSeconds;
    private Map<String, String> userAnswers; // NEW: Stores what they actually clicked!

    public ExamResult() {}

    public ExamResult(String username, String dateTime, int score, int total, int durationSeconds, Map<String, String> userAnswers) {
        this.username = username;
        this.dateTime = dateTime;
        this.score = score;
        this.total = total;
        this.durationSeconds = durationSeconds;
        this.userAnswers = userAnswers;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDateTime() { return dateTime; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
    public Map<String, String> getUserAnswers() { return userAnswers; }
    public void setUserAnswers(Map<String, String> userAnswers) { this.userAnswers = userAnswers; }

    public String getDurationLabel() {
        int minutes = durationSeconds / 60;
        int seconds = durationSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public StringProperty stringProperty(String field) {
        return switch (field) {
            case "username" -> new SimpleStringProperty(getUsername());
            case "dateTime" -> new SimpleStringProperty(getDateTime());
            case "score" -> new SimpleStringProperty(String.valueOf(getScore()));
            case "total" -> new SimpleStringProperty(String.valueOf(getTotal()));
            case "durationLabel" -> new SimpleStringProperty(getDurationLabel());
            default -> new SimpleStringProperty("");
        };
    }
}