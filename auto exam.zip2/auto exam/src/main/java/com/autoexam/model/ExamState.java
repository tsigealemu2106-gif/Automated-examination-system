package com.autoexam.model;

import java.util.Map;
import java.util.Set;

public class ExamState {
    private String username;
    private int remainingSeconds;
    private int currentIndex;
    private Map<String, String> answers;
    private Set<String> markedForReview;

    public ExamState() {}

    public ExamState(String username, int remainingSeconds, int currentIndex, Map<String, String> answers, Set<String> markedForReview) {
        this.username = username;
        this.remainingSeconds = remainingSeconds;
        this.currentIndex = currentIndex;
        this.answers = answers;
        this.markedForReview = markedForReview;
    }

    public String getUsername() { return username; }
    public int getRemainingSeconds() { return remainingSeconds; }
    public int getCurrentIndex() { return currentIndex; }
    public Map<String, String> getAnswers() { return answers; }
    public Set<String> getMarkedForReview() { return markedForReview; }
}