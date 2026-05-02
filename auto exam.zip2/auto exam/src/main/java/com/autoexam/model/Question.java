package com.autoexam.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Question {
    private String id;
    private String text;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String answer;
    private String topic;
    private String examType; // NEW: "Official" or "Practice"

    public Question() {}

    public Question(String id, String text, String optionA, String optionB, String optionC, String optionD, String answer, String topic, String examType) {
        this.id = id;
        this.text = text;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.answer = answer;
        this.topic = topic;
        this.examType = examType;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getOptionA() { return optionA; }
    public void setOptionA(String optionA) { this.optionA = optionA; }
    public String getOptionB() { return optionB; }
    public void setOptionB(String optionB) { this.optionB = optionB; }
    public String getOptionC() { return optionC; }
    public void setOptionC(String optionC) { this.optionC = optionC; }
    public String getOptionD() { return optionD; }
    public void setOptionD(String optionD) { this.optionD = optionD; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getExamType() { return examType; }
    public void setExamType(String examType) { this.examType = examType; }

    public StringProperty stringProperty(String field) {
        return switch (field) {
            case "id" -> new SimpleStringProperty(getId());
            case "text" -> new SimpleStringProperty(getText());
            case "answer" -> new SimpleStringProperty(getAnswer());
            case "topic" -> new SimpleStringProperty(getTopic());
            case "examType" -> new SimpleStringProperty(getExamType()); // NEW
            default -> new SimpleStringProperty("");
        };
    }
}