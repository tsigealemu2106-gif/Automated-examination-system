package com.autoexam.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class User {
    private String username;
    private String passwordHash;
    private String salt;
    private String role; 
    private String securityQuestion; 
    private String securityAnswer;   
    
    // --- NEW FIELDS ---
    private String fullName;
    private String email;
    private String gender;

    public User() {}

    public User(String username, String passwordHash, String salt, String role, String securityQuestion, String securityAnswer, String fullName, String email, String gender) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.role = role;
        this.securityQuestion = securityQuestion;
        this.securityAnswer = securityAnswer;
        this.fullName = fullName;
        this.email = email;
        this.gender = gender;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getSecurityQuestion() { return securityQuestion; }
    public void setSecurityQuestion(String securityQuestion) { this.securityQuestion = securityQuestion; }
    public String getSecurityAnswer() { return securityAnswer; }
    public void setSecurityAnswer(String securityAnswer) { this.securityAnswer = securityAnswer; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public StringProperty usernameProperty() {
        return new SimpleStringProperty(username);
    }
}