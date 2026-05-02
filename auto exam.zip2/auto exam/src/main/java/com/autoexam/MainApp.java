package com.autoexam;

import com.autoexam.model.ExamResult;
import com.autoexam.model.Question;
import com.autoexam.model.User;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class MainApp extends Application {
    private boolean isPracticeMode = false;
    private Stage primaryStage;
    private StorageService storageService;
    private User currentUser;
    private ObservableList<Question> questionData;
    private ObservableList<ExamResult> resultData;
    
    // Global Settings Manager
    private Preferences prefs = Preferences.userNodeForPackage(MainApp.class);
    
    // Exam State Variables
    private IntegerProperty remainingSeconds;
    private Timeline examTimer;
    private List<Question> examQuestions;
    private Map<String, String> examAnswers;
    private int currentQuestionIndex;
    private Set<String> markedForReview;
    // --- NEW: Layout Managers ---
    private BorderPane mainLayout;
    private VBox sidebar;
    
    // UI Components
    private Label timerLabel;
    private Label examStatusLabel;
    private Label questionTextArea;
    private RadioButton optionA, optionB, optionC, optionD;
    private ToggleGroup optionGroup;
    private Label progressLabel;
    private VBox examCard;
    private Label resultSummary;
    private Label averageScoreLabel, totalAttemptsLabel, totalQuestionsLabel;
    private TilePane navigationGrid;
    private CheckBox markReviewBox;
    private HBox examLayout;
    private VBox modeSelection;
    private BarChart<String, Number> topicChart;
    private int cheatStrikes = 0; // Tracks tab-switching!

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        storageService = new StorageService();
        storageService.initialize();
        questionData = FXCollections.observableArrayList(storageService.loadQuestions());
        resultData = FXCollections.observableArrayList(storageService.loadExamResults());

        primaryStage.setTitle("Automated Examination System");
        primaryStage.setScene(createLoginScene());
        // Test the new JDBC SQL Connection!
        DatabaseHelper.initializeDatabase();
        // DatabaseHelper.testInsertUser("sql_test_student", "fakeHash123", "STUDENT");
        primaryStage.show();
    }

   private Scene createLoginScene() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(16);
        grid.setVgap(20);
        grid.setPadding(new Insets(40));
        grid.getStyleClass().add("login-grid");

        AppLogo loginLogo = new AppLogo(80); 
        VBox logoContainer = new VBox(loginLogo);
        logoContainer.setAlignment(Pos.CENTER);
        logoContainer.setPadding(new Insets(0, 0, 20, 0));
        grid.add(logoContainer, 0, 0, 2, 1);

        Text title = new Text("🎓 Automated Examination System");
        title.getStyleClass().add("title-text");
        grid.add(title, 0, 1, 2, 1);

        Label subtitle = new Label("Secure • Reliable • Modern");
        subtitle.getStyleClass().add("subtitle-text");
        subtitle.setStyle("-fx-text-alignment: center;");
        grid.add(subtitle, 0, 2, 2, 1);

        // --- NEW: Bold, uniform styling for all labels ---
        String labelStyle = "-fx-font-weight: bold; -fx-text-fill: #475569; -fx-font-size: 14px;";

        Label userLabel = new Label("👤 Username:");
        userLabel.setStyle(labelStyle);
        grid.add(userLabel, 0, 3);

        TextField userText = new TextField();
        userText.setPromptText("Enter your username");
        userText.getStyleClass().add("text-field");
        grid.add(userText, 1, 3);

        Label pwLabel = new Label("🔒 Password:");
        pwLabel.setStyle(labelStyle);
        grid.add(pwLabel, 0, 4);

        PasswordField pwBox = new PasswordField();
        pwBox.setPromptText("Enter your password");

        TextField visiblePwBox = new TextField();
        visiblePwBox.setPromptText("Enter your password");
        visiblePwBox.setVisible(false);
        visiblePwBox.setManaged(false);

        pwBox.textProperty().bindBidirectional(visiblePwBox.textProperty());
        StackPane pwStack = new StackPane(pwBox, visiblePwBox);
        grid.add(pwStack, 1, 4);

        CheckBox showPwBox = new CheckBox("👁️ Show Password");
        showPwBox.getStyleClass().add("subtitle-text");
        showPwBox.setStyle("-fx-cursor: hand;");
        grid.add(showPwBox, 1, 5);

        showPwBox.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
            pwBox.setVisible(!isNowSelected);
            pwBox.setManaged(!isNowSelected);
            visiblePwBox.setVisible(isNowSelected);
            visiblePwBox.setManaged(isNowSelected);
        });

        Hyperlink forgotPwLink = new Hyperlink("🔑 Forgot Password?");
        forgotPwLink.getStyleClass().add("subtitle-text");
        forgotPwLink.setOnAction(e -> primaryStage.setScene(createForgotPasswordScene()));

        Hyperlink contactLink = new Hyperlink("📞 Contact Admin");
        contactLink.getStyleClass().add("subtitle-text");
        contactLink.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("📞 Contact Support");
            alert.setHeaderText("System Administrator Contact");
            alert.setContentText("If you are locked out or experiencing system issues, please reach out:\n\n📧 Email: alexnega8100@gmail.com\n📱 Phone: +251991005780");
            alert.showAndWait();
        });

        HBox helpLinks = new HBox(20, forgotPwLink, contactLink);
        helpLinks.setAlignment(Pos.CENTER_LEFT);
        grid.add(helpLinks, 1, 6);

        Label loginStatus = new Label();
        loginStatus.getStyleClass().add("status-label");
        loginStatus.setStyle("-fx-background-color: rgba(239, 68, 68, 0.1); -fx-text-fill: #dc2626; -fx-padding: 8px 16px; -fx-background-radius: 8px;");
        loginStatus.setVisible(false);
        grid.add(loginStatus, 0, 8, 2, 1);

        Button loginButton = new Button("🚀 Login");
        loginButton.getStyleClass().add("primary-button");
        loginButton.setDefaultButton(true);
        loginButton.setPrefWidth(120);

        Button registerButton = new Button("📝 Register");
        registerButton.getStyleClass().add("secondary-button");
        registerButton.setPrefWidth(120);

        HBox buttonRow = new HBox(16, loginButton, registerButton);
        buttonRow.setAlignment(Pos.CENTER);
        grid.add(buttonRow, 0, 7, 2, 1);

        loginButton.setOnAction(event -> {
            String username = userText.getText().trim();
            String password = pwBox.getText();
            loginStatus.setVisible(false);

            if (username.isEmpty() || password.isEmpty()) {
                loginStatus.setText("⚠️ Please enter both username and password.");
                loginStatus.setStyle("-fx-background-color: rgba(245, 158, 11, 0.1); -fx-text-fill: #d97706;");
                loginStatus.setVisible(true);
                return;
            }

            loginButton.setText("⏳ Logging in...");
            loginButton.setDisable(true);

            Timeline loading = new Timeline(new KeyFrame(Duration.seconds(0.5), e -> {
                User user = storageService.authenticate(username, password);
                loginButton.setText("🚀 Login");
                loginButton.setDisable(false);

                if (user != null) {
                    currentUser = user;
                    loginStatus.setText("✅ Login successful! Welcome back!");
                    loginStatus.setStyle("-fx-background-color: rgba(34, 197, 94, 0.1); -fx-text-fill: #16a34a;");
                    loginStatus.setVisible(true);

                    Timeline successDelay = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
                        primaryStage.setScene(createMainScene());
                    }));
                    successDelay.play();
                } else {
                    loginStatus.setText("❌ Login failed. Please check your credentials.");
                    loginStatus.setStyle("-fx-background-color: rgba(239, 68, 68, 0.1); -fx-text-fill: #dc2626;");
                    loginStatus.setVisible(true);
                }
            }));
            loading.play();
        });

        registerButton.setOnAction(event -> primaryStage.setScene(createRegistrationScene()));

        userText.setOnAction(e -> {
            if (pwBox.isVisible()) pwBox.requestFocus();
            else visiblePwBox.requestFocus();
        });

        pwBox.setOnAction(e -> loginButton.fire());
        visiblePwBox.setOnAction(e -> loginButton.fire());

        VBox wrap = new VBox(grid);
        wrap.setAlignment(Pos.CENTER);
        wrap.setPadding(new Insets(20));
        Scene scene = new Scene(wrap, 1000, 700);

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.8), wrap);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        scene.getStylesheets().add(getClass().getResource("/application.css").toExternalForm());
        return scene;
    }

    private Scene createForgotPasswordScene() {
        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(400);
        box.setPadding(new Insets(30));
        box.getStyleClass().add("login-grid"); 

        Label title = new Label("Password Recovery");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        TextField userField = new TextField();
        userField.setPromptText("Enter your Username");
        
        Button findUserBtn = new Button("Find Account");
        findUserBtn.getStyleClass().add("primary-button");

        Label questionLabel = new Label("Security Question will appear here.");
        questionLabel.setWrapText(true);
        questionLabel.setVisible(false);

        TextField answerField = new TextField();
        answerField.setPromptText("Enter Answer");
        answerField.setVisible(false);

        PasswordField newPwField = new PasswordField();
        newPwField.setPromptText("Enter New Password");
        newPwField.setVisible(false);

        Button resetBtn = new Button("Reset Password");
        resetBtn.getStyleClass().add("danger-button");
        resetBtn.setVisible(false);

        Button backBtn = new Button("Back to Login");
        backBtn.setOnAction(e -> primaryStage.setScene(createLoginScene()));

        Label status = new Label();
        status.setStyle("-fx-text-fill: red;");

        findUserBtn.setOnAction(e -> {
            String q = storageService.getSecurityQuestion(userField.getText().trim());
            if (q != null && !q.equals("N/A")) {
                questionLabel.setText("Question: " + q);
                questionLabel.setVisible(true); answerField.setVisible(true); newPwField.setVisible(true); resetBtn.setVisible(true);
                userField.setDisable(true); findUserBtn.setDisable(true); status.setText("");
            } else {
                status.setText("User not found or no security question set.");
            }
        });

        resetBtn.setOnAction(e -> {
            if (storageService.verifySecurityAnswer(userField.getText().trim(), answerField.getText())) {
                storageService.changePassword(userField.getText().trim(), newPwField.getText());
                primaryStage.setScene(createLoginScene());
            } else {
                status.setText("Incorrect Answer!");
            }
        });

        box.getChildren().addAll(title, userField, findUserBtn, questionLabel, answerField, newPwField, resetBtn, status, backBtn);
        
        VBox wrap = new VBox(box); wrap.setAlignment(Pos.CENTER); wrap.setPadding(new Insets(20));
        Scene scene = new Scene(wrap, 900, 640);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/application.css")).toExternalForm());
        return scene;
    }

 private Scene createRegistrationScene() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10); grid.setVgap(12);
        grid.setPadding(new Insets(40));
        grid.getStyleClass().add("login-grid");

        Text title = new Text("Register New Student");
        title.getStyleClass().add("title-text");
        grid.add(title, 0, 0, 2, 1);

        // --- NEW: Universal Label Styling ---
        String labelStyle = "-fx-font-weight: bold; -fx-text-fill: #475569; -fx-font-size: 14px;";

        // Personal Information Fields
        Label nameLbl = new Label("Full Name:"); nameLbl.setStyle(labelStyle);
        grid.add(nameLbl, 0, 1);
        TextField nameText = new TextField();
        nameText.setPromptText("First and Last Name");
        grid.add(nameText, 1, 1);

        Label emailLbl = new Label("Email:"); emailLbl.setStyle(labelStyle);
        grid.add(emailLbl, 0, 2);
        TextField emailText = new TextField();
        emailText.setPromptText("student@domain.edu");
        grid.add(emailText, 1, 2);

        Label sexLbl = new Label("Sex:"); sexLbl.setStyle(labelStyle);
        grid.add(sexLbl, 0, 3);
        ComboBox<String> genderBox = new ComboBox<>(FXCollections.observableArrayList("Male", "Female", "Other"));
        genderBox.setPromptText("Select Sex");
        genderBox.setMaxWidth(Double.MAX_VALUE);
        grid.add(genderBox, 1, 3);

        Separator sep1 = new Separator();
        grid.add(sep1, 0, 4, 2, 1);

        // Account Credentials
        Label userLbl = new Label("Username:"); userLbl.setStyle(labelStyle);
        grid.add(userLbl, 0, 5);
        TextField userText = new TextField();
        userText.setPromptText("Choose a username");
        grid.add(userText, 1, 5);

        Label pwLbl = new Label("Password:"); pwLbl.setStyle(labelStyle);
        grid.add(pwLbl, 0, 6);
        PasswordField pwBox = new PasswordField();
        pwBox.setPromptText("Choose a password");
        TextField visiblePwBox = new TextField();
        visiblePwBox.setPromptText("Choose a password");
        visiblePwBox.setVisible(false); visiblePwBox.setManaged(false);
        pwBox.textProperty().bindBidirectional(visiblePwBox.textProperty());
        StackPane pwStack = new StackPane(pwBox, visiblePwBox);
        grid.add(pwStack, 1, 6);

        Label confirmLbl = new Label("Confirm:"); confirmLbl.setStyle(labelStyle);
        grid.add(confirmLbl, 0, 7);
        PasswordField confirmBox = new PasswordField();
        confirmBox.setPromptText("Repeat password");
        TextField visibleConfirmBox = new TextField();
        visibleConfirmBox.setPromptText("Repeat password");
        visibleConfirmBox.setVisible(false); visibleConfirmBox.setManaged(false);
        confirmBox.textProperty().bindBidirectional(visibleConfirmBox.textProperty());
        StackPane confirmStack = new StackPane(confirmBox, visibleConfirmBox);
        grid.add(confirmStack, 1, 7);

        Label secQLbl = new Label("Security Q:"); secQLbl.setStyle(labelStyle);
        grid.add(secQLbl, 0, 8);
        ComboBox<String> questionBox = new ComboBox<>(FXCollections.observableArrayList(
            "What city were you born in?",
            "What is your mother's maiden name?",
            "What was the name of your first pet?"
        ));
        questionBox.setPromptText("Select a question...");
        questionBox.setMaxWidth(Double.MAX_VALUE);
        grid.add(questionBox, 1, 8);

        Label answerLbl = new Label("Answer:"); answerLbl.setStyle(labelStyle);
        grid.add(answerLbl, 0, 9);
        TextField answerBox = new TextField();
        grid.add(answerBox, 1, 9);

        CheckBox showPwBox = new CheckBox("Show Passwords");
        showPwBox.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
        grid.add(showPwBox, 1, 10);

        showPwBox.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
            pwBox.setVisible(!isNowSelected); pwBox.setManaged(!isNowSelected);
            visiblePwBox.setVisible(isNowSelected); visiblePwBox.setManaged(isNowSelected);
            confirmBox.setVisible(!isNowSelected); confirmBox.setManaged(!isNowSelected);
            visibleConfirmBox.setVisible(isNowSelected); visibleConfirmBox.setManaged(isNowSelected);
        });

        Label statusLabel = new Label();
        statusLabel.getStyleClass().add("status-label");
        grid.add(statusLabel, 0, 12, 2, 1);

        Button registerButton = new Button("Register");
        registerButton.getStyleClass().add("primary-button");
        Button backButton = new Button("Back to Login");
        backButton.getStyleClass().add("secondary-button");
        
        HBox buttonRow = new HBox(12, registerButton, backButton);
        grid.add(buttonRow, 0, 11, 2, 1);
        GridPane.setHalignment(buttonRow, javafx.geometry.HPos.RIGHT);

        registerButton.setOnAction(event -> {
            String fullName = nameText.getText().trim();
            String email = emailText.getText().trim();
            String gender = genderBox.getValue();
            String username = userText.getText().trim();
            String password = pwBox.getText();
            String confirm = confirmBox.getText();
            String securityAnswer = answerBox.getText().trim();

            if (fullName.isEmpty() || email.isEmpty() || gender == null || username.isEmpty() || password.isEmpty() || questionBox.getValue() == null || securityAnswer.isEmpty()) {
                statusLabel.setText("Please fill out every field."); 
                return;
            }

            if (username.length() < 3) {
                statusLabel.setText("Username must be at least 3 characters."); return;
            }
            if (!password.equals(confirm)) {
                statusLabel.setText("Passwords do not match."); return;
            }
            
            if (storageService.addUser(username, password, questionBox.getValue(), securityAnswer, fullName, email, gender)) {
                primaryStage.setScene(createLoginScene());
            } else {
                statusLabel.setText("Username is already taken."); 
            }
        });
        backButton.setOnAction(event -> primaryStage.setScene(createLoginScene()));

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        
        VBox wrap = new VBox(scroll); wrap.setAlignment(Pos.CENTER); wrap.setPadding(new Insets(20));
        Scene scene = new Scene(wrap, 900, 750); 
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/application.css")).toExternalForm());
        return scene;
    }

  private Scene createMainScene() {
        mainLayout = new BorderPane();
        
        // Build the new sidebar
        sidebar = createSidebar();
        
        // Assemble the modern layout
        mainLayout.setTop(createToolbar());
        mainLayout.setLeft(sidebar);
        mainLayout.setCenter(createDashboardPane()); // Default screen is the Dashboard
        mainLayout.setBottom(createFooter());

        Scene scene = new Scene(mainLayout, 1200, 820);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/application.css")).toExternalForm());
        return scene;
    }
   // --- FEATURE 5: Application Footer ---
private Node createFooter() {
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(16, 30, 16, 30)); // UPDATE: More padding
        
        // UPDATE: Solid dark footer, clear distinction, bigger font
        footer.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155; -fx-border-width: 3 0 0 0;");

        Label leftText = new Label("© 2026 Developed by Alex, Haile, Tsigie - Dept. of Information Technology");
        leftText.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 14px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label rightText = new Label("System Status: Online  |  Version 1.0");
        rightText.setStyle("-fx-text-fill: #10b981; -fx-font-size: 14px; -fx-font-weight: bold;"); // Green text for "Online"

        footer.getChildren().addAll(leftText, spacer, rightText);
        return footer;
    }

 private Node createToolbar() {
        HBox toolbar = new HBox();
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(12, 30, 12, 30));
        toolbar.setSpacing(18);
        toolbar.setStyle("-fx-background-color: #0f172a; -fx-border-width: 0 0 3 0; -fx-border-color: #3b82f6; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 4);");

        // --- NEW: The Hamburger Menu Toggle Button (Left Corner) ---
        Button toggleMenuBtn = new Button("≡");
        toggleMenuBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold; -fx-cursor: hand;");
        toggleMenuBtn.setOnAction(e -> {
            // This cleanly toggles the sidebar open and closed!
            boolean isVisible = sidebar.isVisible();
            sidebar.setVisible(!isVisible);
            sidebar.setManaged(!isVisible);
        });

        AppLogo logo = new AppLogo(40);
        logo.setStyle("-fx-background-color: white; -fx-background-radius: 8px;");

        Label welcome = new Label("Welcome, " + currentUser.getUsername());
        welcome.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button logout = new Button("Logout");
        logout.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 6px; -fx-cursor: hand; -fx-font-weight: bold;");
        logout.setOnAction(event -> primaryStage.setScene(createLoginScene()));

        toolbar.getChildren().addAll(toggleMenuBtn, logo, welcome, spacer, logout);
        return toolbar;
    }

  // --- NEW: The Sidebar Navigation Menu ---
    private VBox createSidebar() {
        VBox menu = new VBox(10);
        menu.setPadding(new Insets(20, 15, 20, 15));
        menu.setPrefWidth(220);
        menu.setStyle("-fx-background-color: #1e293b; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 0);");

        Label menuHeader = new Label("MAIN MENU");
        menuHeader.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 0 0 10 5;");
        menu.getChildren().add(menuHeader);

        // Core buttons for everyone
        menu.getChildren().add(createNavButton("📊 Dashboard", () -> mainLayout.setCenter(createDashboardPane())));
        
        if ("STUDENT".equals(currentUser.getRole())) {
            menu.getChildren().add(createNavButton("📝 Take Exam", () -> mainLayout.setCenter(createExamPane())));
        }
        
        menu.getChildren().add(createNavButton("👤 Profile & Security", () -> mainLayout.setCenter(createProfilePane())));

        // Admin-only buttons
        if ("ADMIN".equals(currentUser.getRole())) {
            Separator sep = new Separator();
            sep.setPadding(new Insets(15, 0, 15, 0));
            menu.getChildren().add(sep);

            Label adminHeader = new Label("ADMIN TOOLS");
            adminHeader.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 0 0 10 5;");
            menu.getChildren().add(adminHeader);

            menu.getChildren().add(createNavButton("📚 Question Bank", () -> mainLayout.setCenter(createQuestionManagementPane())));
            menu.getChildren().add(createNavButton("📈 Reports & Export", () -> mainLayout.setCenter(createReportsPane())));
            menu.getChildren().add(createNavButton("🏆 Student Rankings", () -> mainLayout.setCenter(createLeaderboardPane())));
        }

        return menu;
    }
    // Helper method to create beautifully styled sidebar buttons
    private Button createNavButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        // Using your existing sidebar CSS classes!
        btn.getStyleClass().add("sidebar-button"); 
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private Node createDashboardPane() {
        VBox dashboard = new VBox(24);
        dashboard.setPadding(new Insets(32));

        // Enhanced header with icon
        Label header = new Label();
        header.getStyleClass().add("section-title");

        ObservableList<ExamResult> dashboardResults;
        boolean isAdmin = "ADMIN".equals(currentUser.getRole());

        if (isAdmin) {
            dashboardResults = resultData;
            header.setText("📊 Admin Overview Dashboard");
        } else {
            List<ExamResult> myResults = resultData.stream()
                    .filter(r -> r.getUsername().equals(currentUser.getUsername()))
                    .collect(Collectors.toList());
            dashboardResults = FXCollections.observableArrayList(myResults);
            header.setText("📈 My Performance & Analytics");
        }

        // Welcome message
        Label welcomeMsg = new Label("Welcome back! Here's your learning progress overview.");
        welcomeMsg.getStyleClass().add("info-text");
        welcomeMsg.setStyle("-fx-font-size: 16px; -fx-text-fill: #64748b;");

        // Enhanced metrics cards with icons
        HBox metrics = new HBox(20);
        metrics.setAlignment(Pos.CENTER_LEFT);

        totalQuestionsLabel = new Label(String.valueOf(questionData.size()));
        totalAttemptsLabel = new Label(String.valueOf(dashboardResults.size()));
        averageScoreLabel = new Label(computeAverageScore(dashboardResults));
        Label lastScoreLabel = new Label(getLastScore(dashboardResults));

        metrics.getChildren().addAll(
                createEnhancedMetricCard("📚 Total Questions", totalQuestionsLabel, "#6366f1"),
                createEnhancedMetricCard("🎯 Exam Attempts", totalAttemptsLabel, "#8b5cf6"),
                createEnhancedMetricCard("📊 Average Score", averageScoreLabel, "#06b6d4"),
                createEnhancedMetricCard("🏆 Last Score", lastScoreLabel, "#10b981")
        );

        // Enhanced chart section
        VBox chartSection = new VBox(12);
        chartSection.setPadding(new Insets(20));
        chartSection.setStyle("-fx-background-color: rgba(255, 255, 255, 0.8); -fx-background-radius: 16px; -fx-border-color: rgba(241, 245, 249, 0.8); -fx-border-width: 1px; -fx-border-radius: 16px;");

        topicChart = createTopicChart();
        if (!isAdmin && !dashboardResults.isEmpty()) {
            topicChart.setTitle("📈 My Topic Mastery (%)");
            updateStudentMasteryChart(dashboardResults);
        } else {
            topicChart.setTitle("📊 Questions by Topic");
            updateTopicChart(topicChart);
        }

        Label chartDesc = new Label("Visual representation of your performance across different topics");
        chartDesc.getStyleClass().add("subtitle-text");
        chartDesc.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");

        chartSection.getChildren().addAll(topicChart, chartDesc);

        // Enhanced history section
        Label historyLabel = new Label("📋 Exam History");
        historyLabel.getStyleClass().add("subtitle-text");
        historyLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 600;");

        TableView<ExamResult> resultTable = new TableView<>(dashboardResults);
        resultTable.getColumns().addAll(List.of(
                createColumn("👤 Username", "username", 150),
                createColumn("📅 Date", "dateTime", 200),
                createColumn("🎯 Score", "score", 90),
                createColumn("📏 Total", "total", 90)
        ));

        if (!isAdmin) {
            TableColumn<ExamResult, Void> reviewCol = new TableColumn<>("🔍 Review");
            reviewCol.setCellFactory(param -> new javafx.scene.control.TableCell<>() {
                private final Button btn = new Button("📖 Review");
                {
                    btn.getStyleClass().add("primary-button");
                    btn.setStyle("-fx-font-size: 12px; -fx-padding: 6px 12px;");
                    btn.setOnAction(event -> showExamReviewDialog(getTableView().getItems().get(getIndex())));
                }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : btn);
                }
            });
            resultTable.getColumns().add(reviewCol);
        }

        resultTable.setMinHeight(300);
        resultTable.setStyle("-fx-background-color: rgba(255, 255, 255, 0.9); -fx-border-color: rgba(241, 245, 249, 0.8); -fx-border-radius: 12px;");

        VBox.setVgrow(resultTable, Priority.ALWAYS);

        // Quick actions for students
       // Quick actions for students
        if (!isAdmin) {
            HBox quickActions = new HBox(16);
            quickActions.setAlignment(Pos.CENTER_LEFT);
            quickActions.setPadding(new Insets(20, 0, 0, 0));

            Button startPracticeBtn = new Button("🎯 Start Practice");
            startPracticeBtn.getStyleClass().add("secondary-button");
            startPracticeBtn.setOnAction(e -> {
                // UPDATE: Swaps the center screen to the Exam Pane
                mainLayout.setCenter(createExamPane());
            });

            Button viewProfileBtn = new Button("👤 View Profile");
            viewProfileBtn.getStyleClass().add("secondary-button");
            viewProfileBtn.setOnAction(e -> {
                // UPDATE: Swaps the center screen to the Profile Pane
                mainLayout.setCenter(createProfilePane());
            });

            quickActions.getChildren().addAll(startPracticeBtn, viewProfileBtn);

            dashboard.getChildren().addAll(header, welcomeMsg, metrics, chartSection, historyLabel, resultTable, quickActions);
        } else {
            
            dashboard.getChildren().addAll(header, welcomeMsg, metrics, chartSection, historyLabel, resultTable);
        }

        ScrollPane scrollPane = new ScrollPane(dashboard);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        return scrollPane;
    }

    private VBox createMetricCard(String title, Label valueLabel) {
        VBox card = new VBox(6);
        card.getStyleClass().add("metric-card");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("metric-title");
        valueLabel.getStyleClass().add("metric-value");
        card.getChildren().addAll(titleLabel, valueLabel);

        // --- NEW: Smooth Hover Motion Animation ---
        // (Using fully qualified names so you don't even have to change your imports at the top!)
        javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(150), card);

        card.setOnMouseEntered(e -> {
            st.stop();
            st.setToX(1.06); // Grows 6% wider
            st.setToY(1.06); // Grows 6% taller
            st.play();
            // Adds a slightly deeper shadow when hovered
            card.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 15, 0, 0, 5); -fx-cursor: hand;");
        });

        card.setOnMouseExited(e -> {
            st.stop();
            st.setToX(1.0);  // Shrinks back to 100% normal size
            st.setToY(1.0);
            st.play();
            // Removes the extra shadow
            card.setStyle("");
        });
        // -----------------------------------------

        return card;
    }

    private VBox createEnhancedMetricCard(String title, Label valueLabel, String accentColor) {
        VBox card = new VBox(8);
        card.getStyleClass().add("metric-card");
        card.setStyle("-fx-border-color: " + accentColor + "33; -fx-background-color: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(255, 255, 255, 0.9) 100%);");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("metric-title");
        titleLabel.setStyle("-fx-text-fill: #64748b; -fx-font-weight: 600;");

        valueLabel.getStyleClass().add("metric-value");
        valueLabel.setStyle("-fx-text-fill: " + accentColor + ";");

        // Enhanced hover animation
        ScaleTransition st = new ScaleTransition(Duration.millis(200), card);

        card.setOnMouseEntered(e -> {
            st.stop();
            st.setToX(1.05);
            st.setToY(1.05);
            st.play();
            card.setStyle("-fx-effect: dropshadow(three-pass-box, " + accentColor + "40, 20, 0, 0, 8); -fx-cursor: hand; -fx-border-color: " + accentColor + "66;");
        });

        card.setOnMouseExited(e -> {
            st.stop();
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
            card.setStyle("-fx-border-color: " + accentColor + "33; -fx-background-color: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(255, 255, 255, 0.9) 100%);");
        });

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    private String computeAverageScore(ObservableList<ExamResult> resultsToProcess) {
        if (resultsToProcess.isEmpty()) return "N/A";
        double avg = resultsToProcess.stream().mapToDouble(ExamResult::getScore).average().orElse(0);
        return String.format("%.1f", avg);
    }

    private String getLastScore(ObservableList<ExamResult> resultsToProcess) {
        return resultsToProcess.stream()
                .max(Comparator.comparing(ExamResult::getDateTime))
                .map(result -> result.getScore() + " / " + result.getTotal())
                .orElse("No exam yet");
    }

    private BarChart<String, Number> createTopicChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Questions by Topic");
        xAxis.setLabel("Topic");
        yAxis.setLabel("Count");
        chart.setLegendVisible(false);
        updateTopicChart(chart);
        chart.setPrefHeight(350);
        return chart;
    }

    private void updateTopicChart(BarChart<String, Number> chart) {
        Map<String, Long> counts = questionData.stream()
                .collect(Collectors.groupingBy(Question::getTopic, Collectors.counting()));
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        counts.forEach((topic, count) -> series.getData().add(new XYChart.Data<>(topic, count)));
        chart.getData().clear();
        chart.getData().add(series);
    }

    private void updateStudentMasteryChart(ObservableList<ExamResult> myResults) {
        topicChart.getData().clear();
        Map<String, int[]> topicStats = new HashMap<>(); 
        
        for (ExamResult result : myResults) {
            if (result.getUserAnswers() == null) continue;
            for (Question q : questionData) {
                if (result.getUserAnswers().containsKey(q.getId())) {
                    topicStats.putIfAbsent(q.getTopic(), new int[]{0, 0});
                    topicStats.get(q.getTopic())[1]++;
                    if (q.getAnswer().equals(result.getUserAnswers().get(q.getId()))) {
                        topicStats.get(q.getTopic())[0]++;
                    }
                }
            }
        }
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        topicStats.forEach((topic, stats) -> {
            double percentage = stats[1] > 0 ? ((double) stats[0] / stats[1]) * 100 : 0;
            series.getData().add(new XYChart.Data<>(topic, percentage));
        });
        topicChart.getData().add(series);
    }

    private void updateDashboardMetrics() {
        totalQuestionsLabel.setText(String.valueOf(questionData.size()));
        ObservableList<ExamResult> dashboardResults;
        if ("ADMIN".equals(currentUser.getRole())) {
            dashboardResults = resultData;
        } else {
            List<ExamResult> myResults = resultData.stream()
                    .filter(r -> r.getUsername().equals(currentUser.getUsername()))
                    .collect(Collectors.toList());
            dashboardResults = FXCollections.observableArrayList(myResults);
        }
        totalAttemptsLabel.setText(String.valueOf(dashboardResults.size()));
        averageScoreLabel.setText(computeAverageScore(dashboardResults));
        if (topicChart != null) updateTopicChart(topicChart);
    }

    private void showExamReviewDialog(ExamResult result) {
        Stage reviewStage = new Stage();
        reviewStage.setTitle("Exam Review - " + result.getDateTime());
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: white;");
        
        Label header = new Label(String.format("Score: %d / %d", result.getScore(), result.getTotal()));
        header.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #4f46e5;");
        content.getChildren().add(header);

        if (result.getUserAnswers() != null) {
            for (Question q : questionData) {
                if (result.getUserAnswers().containsKey(q.getId())) {
                    String userAnswer = result.getUserAnswers().get(q.getId());
                    boolean isCorrect = userAnswer.equals(q.getAnswer());
                    
                    VBox qBox = new VBox(5);
                    qBox.setStyle("-fx-padding: 10px; -fx-border-color: #e2e8f0; -fx-border-radius: 8px; -fx-background-color: " + (isCorrect ? "#f0fdf4;" : "#fef2f2;"));
                    
                    Label text = new Label(q.getText());
                    text.setWrapText(true);
                    text.setStyle("-fx-font-weight: bold;");
                    
                    Label userLbl = new Label("Your Answer: " + userAnswer);
                    userLbl.setStyle(isCorrect ? "-fx-text-fill: #16a34a;" : "-fx-text-fill: #dc2626;");
                    
                    Label correctLbl = new Label("Correct Answer: " + q.getAnswer());
                    correctLbl.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
                    
                    qBox.getChildren().addAll(text, userLbl);
                    if (!isCorrect) qBox.getChildren().add(correctLbl);
                    content.getChildren().add(qBox);
                }
            }
        } else {
            content.getChildren().add(new Label("No review data available for this older exam."));
        }

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        Scene scene = new Scene(scroll, 600, 500);
        reviewStage.setScene(scene);
        reviewStage.show();
    }

  private Node createQuestionManagementPane() {
        VBox pane = new VBox(20);
        pane.setPadding(new Insets(20));

        Label header = new Label("Question Bank Management");
        header.getStyleClass().add("section-title");

        // --- TIMER SETTINGS ---
        HBox timerBox = new HBox(10);
        timerBox.setAlignment(Pos.CENTER_LEFT);
        timerBox.setPadding(new Insets(12));
        timerBox.setStyle("-fx-border-color: #cbd5e1; -fx-border-radius: 8px; -fx-background-color: #f8fafc;");
        
        Label timerLabel = new Label("Global Exam Duration (Seconds):");
        timerLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");
        TextField timerField = new TextField(String.valueOf(prefs.getInt("exam_duration", 180)));
        timerField.setPrefWidth(80);
        Button saveTimerBtn = new Button("Save Timer");
        saveTimerBtn.getStyleClass().add("primary-button");
        Label timerStatus = new Label();
        saveTimerBtn.setOnAction(e -> {
            try {
                int newDur = Integer.parseInt(timerField.getText().trim());
                if(newDur < 30) throw new Exception("Too short");
                prefs.putInt("exam_duration", newDur);
                timerStatus.setText("Settings Saved!");
                timerStatus.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;"); 
            } catch(Exception ex) {
                timerStatus.setText("Invalid format.");
                timerStatus.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;"); 
            }
        });
        timerBox.getChildren().addAll(timerLabel, timerField, saveTimerBtn, timerStatus);

        // --- DATA TABLE ---
        TableView<Question> table = new TableView<>(questionData);
        table.getColumns().addAll(List.of(
                createQuestionColumn("ID", "id", 50),
                createQuestionColumn("Topic", "topic", 120),
                createQuestionColumn("Type", "examType", 100),
                createQuestionColumn("Question", "text", 300),
                createQuestionColumn("Answer", "answer", 80)
        ));
        table.setPrefHeight(250);

        // --- FEATURE 2: Excel Import Button ---
        Button importBtn = new Button("📥 Bulk Import via Excel");
        importBtn.getStyleClass().add("secondary-button");
        importBtn.setOnAction(e -> {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
            java.io.File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                int count = storageService.importQuestionsFromExcel(file.toPath());
                questionData.setAll(storageService.loadQuestions()); // Refresh the table!
                updateDashboardMetrics();
                new Alert(Alert.AlertType.INFORMATION, "Successfully imported " + count + " questions!").show();
            }
        });
        pane.getChildren().add(importBtn); // Add it to the pane!
        // --------------------------------------

        // --- EXPLICITLY LABELED FORM ---
        VBox formContainer = new VBox(15);
        formContainer.setPadding(new Insets(20));
        formContainer.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e2e8f0; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");

        Label formHeader = new Label("Add / Edit Question");
        formHeader.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        GridPane formGrid = new GridPane();
        formGrid.setHgap(15); 
        formGrid.setVgap(15);

        // Styling for all form labels to make them stand out
        String labelStyle = "-fx-font-weight: bold; -fx-text-fill: #475569;";

        // Row 1
        Label topicLbl = new Label("Topic:"); topicLbl.setStyle(labelStyle);
        TextField topicField = new TextField(); 
        
        Label typeLbl = new Label("Exam Type:"); typeLbl.setStyle(labelStyle);
        ComboBox<String> typeBox = new ComboBox<>(FXCollections.observableArrayList("Official", "Practice"));
        
        formGrid.add(topicLbl, 0, 0); formGrid.add(topicField, 1, 0);
        formGrid.add(typeLbl, 2, 0); formGrid.add(typeBox, 3, 0);

        // Row 2
        Label qTextLbl = new Label("Question Text:"); qTextLbl.setStyle(labelStyle);
        TextField textField = new TextField(); 
        textField.setPrefWidth(400);
        
        formGrid.add(qTextLbl, 0, 1); formGrid.add(textField, 1, 1, 3, 1);

        // Row 3
        Label optALbl = new Label("Option A:"); optALbl.setStyle(labelStyle);
        TextField optA = new TextField(); 
        
        Label optBLbl = new Label("Option B:"); optBLbl.setStyle(labelStyle);
        TextField optB = new TextField(); 
        
        formGrid.add(optALbl, 0, 2); formGrid.add(optA, 1, 2);
        formGrid.add(optBLbl, 2, 2); formGrid.add(optB, 3, 2);

        // Row 4
        Label optCLbl = new Label("Option C:"); optCLbl.setStyle(labelStyle);
        TextField optC = new TextField(); 
        
        Label optDLbl = new Label("Option D:"); optDLbl.setStyle(labelStyle);
        TextField optD = new TextField(); 
        
        formGrid.add(optCLbl, 0, 3); formGrid.add(optC, 1, 3);
        formGrid.add(optDLbl, 2, 3); formGrid.add(optD, 3, 3);

        // Row 5
        Label ansLbl = new Label("Correct Answer:"); ansLbl.setStyle(labelStyle);
        ComboBox<String> ansBox = new ComboBox<>(FXCollections.observableArrayList("A", "B", "C", "D"));
        
        formGrid.add(ansLbl, 0, 4); formGrid.add(ansBox, 1, 4);

        // --- BUTTONS ---
        Button addButton = new Button("Add New");
        addButton.getStyleClass().add("primary-button");
        
        Button updateButton = new Button("Update Selected");
        updateButton.getStyleClass().add("secondary-button");
        updateButton.setDisable(true); 

        Button clearButton = new Button("Clear Form");
        clearButton.setStyle("-fx-background-color: transparent; -fx-border-color: #cbd5e1; -fx-border-radius: 8px; -fx-cursor: hand;");

        Button deleteButton = new Button("Delete Selected");
        deleteButton.getStyleClass().add("danger-button");

        HBox formButtons = new HBox(12, addButton, updateButton, clearButton, new Region(), deleteButton);
        HBox.setHgrow(formButtons.getChildren().get(3), Priority.ALWAYS); 

        formContainer.getChildren().addAll(formHeader, formGrid, formButtons);

        // --- LOGIC ---
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                topicField.setText(newSelection.getTopic());
                typeBox.setValue(newSelection.getExamType());
                textField.setText(newSelection.getText());
                optA.setText(newSelection.getOptionA());
                optB.setText(newSelection.getOptionB());
                optC.setText(newSelection.getOptionC());
                optD.setText(newSelection.getOptionD());
                ansBox.setValue(newSelection.getAnswer());
                updateButton.setDisable(false); 
                addButton.setDisable(true);     
            }
        });

        Runnable clearForm = () -> {
            topicField.clear(); typeBox.setValue(null); textField.clear();
            optA.clear(); optB.clear(); optC.clear(); optD.clear(); ansBox.setValue(null);
            table.getSelectionModel().clearSelection();
            updateButton.setDisable(true);
            addButton.setDisable(false);
        };
        clearButton.setOnAction(e -> clearForm.run());

        addButton.setOnAction(e -> {
            if (textField.getText().isEmpty() || typeBox.getValue() == null || ansBox.getValue() == null) return;
            String newId = String.valueOf(System.currentTimeMillis()).substring(8);
            Question q = new Question(newId, textField.getText(), optA.getText(), optB.getText(), optC.getText(), optD.getText(), ansBox.getValue(), topicField.getText(), typeBox.getValue());
            questionData.add(q);
            storageService.saveQuestions(new ArrayList<>(questionData));
            updateDashboardMetrics();
            clearForm.run();
        });

        updateButton.setOnAction(e -> {
            Question selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selected.setTopic(topicField.getText());
                selected.setExamType(typeBox.getValue());
                selected.setText(textField.getText());
                selected.setOptionA(optA.getText());
                selected.setOptionB(optB.getText());
                selected.setOptionC(optC.getText());
                selected.setOptionD(optD.getText());
                selected.setAnswer(ansBox.getValue());
                table.refresh(); 
                storageService.saveQuestions(new ArrayList<>(questionData)); 
                clearForm.run();
            }
        });

        deleteButton.setOnAction(e -> {
            Question selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                questionData.remove(selected);
                storageService.saveQuestions(new ArrayList<>(questionData));
                updateDashboardMetrics();
                clearForm.run();
            }
        });

        pane.getChildren().addAll(header, timerBox, table, formContainer);
        
        ScrollPane scrollPane = new ScrollPane(pane);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scrollPane;
    }

    private Node createExamPane() {
        VBox root = new VBox(24);
        root.setPadding(new Insets(32));

        // Enhanced header with icon
        Label header = new Label("📝 Automated Examination");
        header.getStyleClass().add("section-title");

        // Welcome message
        Label welcomeMsg = new Label("Choose your exam mode and begin your assessment journey.");
        welcomeMsg.getStyleClass().add("info-text");
        welcomeMsg.setStyle("-fx-font-size: 16px; -fx-text-fill: #64748b;");

        // Enhanced exam mode selection
         modeSelection = new VBox(16);
        modeSelection.setPadding(new Insets(24));
        modeSelection.setStyle("-fx-background-color: rgba(255, 255, 255, 0.8); -fx-background-radius: 16px; -fx-border-color: rgba(241, 245, 249, 0.8); -fx-border-width: 1px; -fx-border-radius: 16px;");

        Label modeLabel = new Label("🎯 Select Exam Mode");
        modeLabel.getStyleClass().add("subtitle-text");
        modeLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 600;");

        // Official exam card
        VBox officialCard = new VBox(12);
        officialCard.setPadding(new Insets(20));
        officialCard.setStyle("-fx-background-color: linear-gradient(135deg, rgba(239, 68, 68, 0.1) 0%, rgba(220, 38, 38, 0.05) 100%); -fx-border-color: rgba(239, 68, 68, 0.3); -fx-border-width: 2px; -fx-border-radius: 12px; -fx-background-radius: 12px;");

        Label officialTitle = new Label("🏆 Official Exam");
        officialTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #dc2626;");

        Label officialDesc = new Label("Timed assessment with final scoring. Your performance will be recorded.");
        officialDesc.setWrapText(true);
        officialDesc.getStyleClass().add("info-text");

        Button startOfficialBtn = new Button("🚀 Start Official Exam");
        startOfficialBtn.getStyleClass().add("danger-button");
        startOfficialBtn.setPrefWidth(200);
      startOfficialBtn.setOnAction(event -> {
            // --- NEW: Check for an interrupted exam ---
            com.autoexam.model.ExamState savedState = storageService.getActiveExamState(currentUser.getUsername());
            
            if (savedState != null) {
                // They have a saved exam! Ask if they want to resume.
                Alert resumeAlert = new Alert(Alert.AlertType.CONFIRMATION);
                resumeAlert.setTitle("Incomplete Exam Detected");
                resumeAlert.setHeaderText("⚠️ You have an exam in progress!");
                resumeAlert.setContentText("The system detected that your device previously lost connection or shut down during an official exam.\n\nDo you want to resume exactly where you left off?");
                
                ButtonType resumeBtn = new ButtonType("Resume Exam", ButtonBar.ButtonData.OK_DONE);
                ButtonType restartBtn = new ButtonType("Delete & Start Fresh", ButtonBar.ButtonData.CANCEL_CLOSE);
                resumeAlert.getButtonTypes().setAll(resumeBtn, restartBtn);
                
                resumeAlert.showAndWait().ifPresent(res -> {
                    if (res == resumeBtn) {
                        isPracticeMode = false;
                        beginExam(); // Setup the exam
                        
                        // INJECT THE SAVED DATA!
                        examAnswers = new HashMap<>(savedState.getAnswers());
                        markedForReview = new java.util.HashSet<>(savedState.getMarkedForReview());
                        currentQuestionIndex = savedState.getCurrentIndex();
                        remainingSeconds.set(savedState.getRemainingSeconds());
                        
                        updateExamQuestion(); // Refresh the screen with their old answers
                        buildNavigationGrid(); // Refresh the side grid
                    } else if (res == restartBtn) {
                        storageService.clearActiveExamState(currentUser.getUsername());
                        isPracticeMode = false;
                        beginExam();
                    }
                });
                return; // Stop here, don't show the regular warning yet.
            }
            
            // --- Original Pre-Exam Anti-Cheat Warning Dialog ---
            Alert warning = new Alert(Alert.AlertType.CONFIRMATION);
            warning.setTitle("Strict Proctor Agreement");
            warning.setHeaderText("⚠️ ANTI-CHEAT SYSTEM ACTIVE");
            warning.setContentText("This Official Exam is strictly monitored by the system.\n\n"
                    + "• The exam will launch in Fullscreen Mode.\n"
                    + "• Leaving the exam window (Alt+Tab, clicking other apps) will trigger a Cheating Strike.\n"
                    + "• You are allowed ONE warning. Your SECOND strike will result in automatic submission.\n\n"
                    + "Do you understand the rules and wish to begin?");
            
            ButtonType acceptBtn = new ButtonType("I Understand & Accept", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            warning.getButtonTypes().setAll(acceptBtn, cancelBtn);

            warning.showAndWait().ifPresent(response -> {
                if (response == acceptBtn) {
                    isPracticeMode = false;
                    beginExam();
                }
            });
        });

        officialCard.getChildren().addAll(officialTitle, officialDesc, startOfficialBtn);

        // Practice mode card
        VBox practiceCard = new VBox(12);
        practiceCard.setPadding(new Insets(20));
        practiceCard.setStyle("-fx-background-color: linear-gradient(135deg, rgba(99, 102, 241, 0.1) 0%, rgba(139, 92, 246, 0.05) 100%); -fx-border-color: rgba(99, 102, 241, 0.3); -fx-border-width: 2px; -fx-border-radius: 12px; -fx-background-radius: 12px;");

        Label practiceTitle = new Label("🎓 Practice Mode");
        practiceTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #6366f1;");

        Label practiceDesc = new Label("Learn and improve with instant feedback. No time pressure, no scoring.");
        practiceDesc.setWrapText(true);
        practiceDesc.getStyleClass().add("info-text");

        Button startPracticeBtn = new Button("🎯 Start Practice Mode");
        startPracticeBtn.getStyleClass().add("primary-button");
        startPracticeBtn.setPrefWidth(200);
        startPracticeBtn.setOnAction(event -> {
            isPracticeMode = true;
            beginExam();
        });

        practiceCard.getChildren().addAll(practiceTitle, practiceDesc, startPracticeBtn);

        HBox modeCards = new HBox(20, officialCard, practiceCard);
        modeCards.setAlignment(Pos.CENTER);

        modeSelection.getChildren().addAll(modeLabel, modeCards);

        // Enhanced timer and status
        timerLabel = new Label("⏰ Time remaining: 00:00");
        timerLabel.getStyleClass().add("timer-label");
        timerLabel.setVisible(false);

        examStatusLabel = new Label();
        examStatusLabel.getStyleClass().add("status-label");
        examStatusLabel.setVisible(false);

        // Enhanced exam layout
        examLayout = new HBox(24);
        examLayout.setVisible(false);

        // Main exam card with better styling
        examCard = new VBox(16);
        examCard.setPadding(new Insets(24));
        examCard.setPrefWidth(720);
        examCard.getStyleClass().add("exam-card");

        // Question header with progress
        progressLabel = new Label("Question 1 of 10");
        progressLabel.getStyleClass().add("subtitle-text");
        progressLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 600; -fx-text-fill: #6366f1;");

       // Enhanced question area
        questionTextArea = new Label(); // Changed to a Label for perfect auto-sizing!
        questionTextArea.setWrapText(true);
        questionTextArea.setMaxWidth(Double.MAX_VALUE);
        questionTextArea.setPadding(new Insets(15)); // Gives it breathing room inside
        questionTextArea.getStyleClass().add("question-area");
        
        // Beautiful modern styling with a light background
        questionTextArea.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-line-spacing: 6px; -fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 8px; -fx-background-radius: 8px;");
        
        // This tells JavaFX to NEVER stretch this box taller than the text inside it!
        VBox.setVgrow(questionTextArea, Priority.NEVER);

        // Options section
        Label optionsHeader = new Label("📋 Select your answer:");
        optionsHeader.getStyleClass().add("subtitle-text");
        optionsHeader.setStyle("-fx-font-weight: 600;");

        optionGroup = new ToggleGroup();
        optionA = createEnhancedOptionButton(optionGroup, "A");
        optionB = createEnhancedOptionButton(optionGroup, "B");
        optionC = createEnhancedOptionButton(optionGroup, "C");
        optionD = createEnhancedOptionButton(optionGroup, "D");

        VBox optionsBox = new VBox(12, optionsHeader, optionA, optionB, optionC, optionD);

        // Enhanced feedback for practice mode
        optionGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (isPracticeMode && newVal != null) {
                saveAnswer();
                Question q = examQuestions.get(currentQuestionIndex);
                String selected = examAnswers.get(q.getId());
                if (selected != null && selected.equals(q.getAnswer())) {
                    examStatusLabel.setText("✅ Correct! Well done!");
                    examStatusLabel.setStyle("-fx-background-color: rgba(34, 197, 94, 0.1); -fx-text-fill: #16a34a; -fx-font-weight: bold;");
                    examStatusLabel.setVisible(true);
                } else {
                    examStatusLabel.setText("❌ Incorrect. The correct answer is: " + q.getAnswer());
                    examStatusLabel.setStyle("-fx-background-color: rgba(239, 68, 68, 0.1); -fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    examStatusLabel.setVisible(true);
                }
            }
        });

        // Enhanced controls
        markReviewBox = new CheckBox("⭐ Mark for Review");
        markReviewBox.getStyleClass().add("subtitle-text");
        markReviewBox.setOnAction(e -> {
            Question currentQ = examQuestions.get(currentQuestionIndex);
            if (markReviewBox.isSelected()) {
                markedForReview.add(currentQ.getId());
            } else {
                markedForReview.remove(currentQ.getId());
            }
            updateGridColors();
        });

        Button prevButton = new Button("⬅️ Previous");
        prevButton.getStyleClass().add("secondary-button");
        prevButton.setOnAction(event -> moveQuestion(-1));

        Button nextButton = new Button("Next ➡️");
        nextButton.getStyleClass().add("secondary-button");
        nextButton.setOnAction(event -> moveQuestion(1));

        Button finishButton = new Button("🎯 Submit Exam");
        finishButton.getStyleClass().add("danger-button");
        finishButton.setOnAction(event -> completeExam());

        HBox navigation = new HBox(16, prevButton, markReviewBox, nextButton, finishButton);
        navigation.setAlignment(Pos.CENTER_LEFT);

        resultSummary = new Label();
        resultSummary.getStyleClass().add("result-summary");
        resultSummary.setWrapText(true);
        resultSummary.setVisible(false);

        examCard.getChildren().addAll(timerLabel, progressLabel, questionTextArea, optionsBox, navigation, resultSummary);

        // Enhanced question navigator
        VBox gridContainer = new VBox(12);
        gridContainer.setPrefWidth(280);
        gridContainer.setPadding(new Insets(20));
        gridContainer.setStyle("-fx-background-color: rgba(255, 255, 255, 0.9); -fx-background-radius: 16px; -fx-border-color: rgba(241, 245, 249, 0.8); -fx-border-width: 1px; -fx-border-radius: 16px;");

        Label gridLabel = new Label("🧭 Question Navigator");
        gridLabel.getStyleClass().add("subtitle-text");
        gridLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 600;");

        Label gridDesc = new Label("Click on question numbers to navigate");
        gridDesc.getStyleClass().add("info-text");

        navigationGrid = new TilePane();
        navigationGrid.setHgap(10);
        navigationGrid.setVgap(10);
        navigationGrid.setPrefColumns(5);
        navigationGrid.setStyle("-fx-padding: 16px; -fx-background-color: rgba(248, 250, 252, 0.8); -fx-background-radius: 12px;");

        gridContainer.getChildren().addAll(gridLabel, gridDesc, navigationGrid);
        examLayout.getChildren().addAll(examCard, gridContainer);

        root.getChildren().addAll(header, welcomeMsg, modeSelection, examLayout, examStatusLabel);

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true); 
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        return scrollPane;
    }

    private RadioButton createOptionButton(ToggleGroup group, String label) {
        RadioButton button = new RadioButton(label);
        button.setToggleGroup(group);
        button.setWrapText(true);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("option-button");
        return button;
    }

    private RadioButton createEnhancedOptionButton(ToggleGroup group, String label) {
        RadioButton button = new RadioButton("  " + label);
        button.setToggleGroup(group);
        button.setWrapText(true);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("option-button");
        button.setStyle("-fx-padding: 16px 20px; -fx-background-color: rgba(255, 255, 255, 0.8); -fx-border-color: rgba(226, 232, 240, 0.8); -fx-border-width: 2px; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-font-size: 15px; -fx-font-weight: 500; -fx-cursor: hand;");

        // Enhanced hover and selection effects
        button.setOnMouseEntered(e -> {
            if (!button.isSelected()) {
                button.setStyle("-fx-padding: 16px 20px; -fx-background-color: rgba(99, 102, 241, 0.05); -fx-border-color: rgba(99, 102, 241, 0.4); -fx-border-width: 2px; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-font-size: 15px; -fx-font-weight: 500; -fx-cursor: hand;");
            }
        });

        button.setOnMouseExited(e -> {
            if (!button.isSelected()) {
                button.setStyle("-fx-padding: 16px 20px; -fx-background-color: rgba(255, 255, 255, 0.8); -fx-border-color: rgba(226, 232, 240, 0.8); -fx-border-width: 2px; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-font-size: 15px; -fx-font-weight: 500; -fx-cursor: hand;");
            }
        });

        button.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                button.setStyle("-fx-padding: 16px 20px; -fx-background-color: linear-gradient(135deg, rgba(99, 102, 241, 0.1) 0%, rgba(139, 92, 246, 0.1) 100%); -fx-border-color: #6366f1; -fx-border-width: 2px; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #6366f1; -fx-cursor: hand;");
            } else {
                button.setStyle("-fx-padding: 16px 20px; -fx-background-color: rgba(255, 255, 255, 0.8); -fx-border-color: rgba(226, 232, 240, 0.8); -fx-border-width: 2px; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-font-size: 15px; -fx-font-weight: 500; -fx-text-fill: inherit; -fx-cursor: hand;");
            }
        });

        return button;
    }

    private void beginExam() {
        if (questionData.isEmpty()) {
            examStatusLabel.setText("No questions available yet. Add questions in the Question Bank first.");
            return;
        }
        examStatusLabel.setText(isPracticeMode ? "Practice Mode Active. No timer, no fullscreen." : "");
        examStatusLabel.setStyle(""); 
        timerLabel.getStyleClass().remove("timer-warning");
        
       // --- THE FIX: Filter AND Shuffle the questions! ---
        examQuestions = questionData.stream()
                .filter(q -> isPracticeMode ? "Practice".equals(q.getExamType()) : "Official".equals(q.getExamType()))
                .map(this::shuffleQuestion) // This applies our new algorithm to every question!
                .collect(Collectors.toList());
                
        if (examQuestions.isEmpty()) {
            examStatusLabel.setText("No questions available for this exam type.");
            examStatusLabel.setStyle("-fx-text-fill: red;");
            return;
        }
        Collections.shuffle(examQuestions);
        examAnswers = new HashMap<>();
        currentQuestionIndex = 0;
        markedForReview = new HashSet<>();
        
        int examDuration = prefs.getInt("exam_duration", 180);
        remainingSeconds = new SimpleIntegerProperty(examDuration);
        
        timerLabel.setText(isPracticeMode ? "Practice Mode" : formatTime(remainingSeconds.get()));
        if (!isPracticeMode) {
            cheatStrikes = 0; // Reset strikes
            startTimer();
            primaryStage.setFullScreenExitHint("ANTI-CHEAT ACTIVE: Exiting fullscreen will flag your exam!");
            primaryStage.setFullScreen(true);
            
            // --- FEATURE 1: The Strict Proctor Focus Tracker ---
            primaryStage.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                // If they lose focus, the exam is active, and they are not practicing:
                if (!isNowFocused && examLayout.isVisible() && !examCard.isDisabled() && !isPracticeMode) {
                    cheatStrikes++;
                    if (cheatStrikes >= 1) {
                        examTimer.stop();
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Exam auto-submitted due to multiple cheating infractions (Window Focus Lost).");
                        alert.setHeaderText("CHEATING DETECTED");
                        alert.showAndWait();
                        completeExam(); // Auto-submit!
                    } else {
                        examStatusLabel.setText("⚠️ WARNING: Do not leave the exam window! Strike " + cheatStrikes + " of 1.");
                        examStatusLabel.setStyle("-fx-background-color: rgba(239, 68, 68, 0.1); -fx-text-fill: #dc2626; -fx-font-weight: bold;");
                        examStatusLabel.setVisible(true);
                    }
                }
            });
        }
        
      
        examLayout.setVisible(true); 
        examLayout.setManaged(true);
        
        modeSelection.setVisible(false);
        modeSelection.setManaged(false);
        // -------------------------------------------------------------------
        
        examCard.setVisible(true);
        resultSummary.setVisible(false);
        examCard.setDisable(false); 
        examCard.setVisible(true);
        resultSummary.setVisible(false);
        examCard.setDisable(false);
        buildNavigationGrid();
        updateExamQuestion();

        if (!isPracticeMode) {
            startTimer();
            primaryStage.setFullScreenExitHint("ANTI-CHEAT ACTIVE: Exiting fullscreen will flag your exam!");
            primaryStage.setFullScreen(true);
            primaryStage.fullScreenProperty().addListener((obs, wasFullScreen, isNowFullScreen) -> {
                if (!isNowFullScreen && examLayout.isVisible() && !examCard.isDisabled()) {
                    examStatusLabel.setText("ANTI-CHEAT WARNING: You exited fullscreen mode!");
                    examStatusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-font-size: 18px;");
                }
            });
        }
    }

    private void startTimer() {
        if (examTimer != null) {
            examTimer.stop();
        }
        examTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            int seconds = remainingSeconds.get() - 1;
            remainingSeconds.set(seconds);
            
            if (seconds <= 30 && seconds > 0) {
                if (!timerLabel.getStyleClass().contains("timer-warning")) {
                    timerLabel.getStyleClass().add("timer-warning");
                }
            }
            if (seconds <= 0) {
                examTimer.stop();
                completeExam();
            }
        }));
        examTimer.setCycleCount(Timeline.INDEFINITE);
        examTimer.play();
    }

    private void updateExamQuestion() {
        if (currentQuestionIndex < 0) currentQuestionIndex = 0;
        else if (currentQuestionIndex >= examQuestions.size()) currentQuestionIndex = examQuestions.size() - 1;
        
        Question question = examQuestions.get(currentQuestionIndex);
        questionTextArea.setText(question.getText());
        optionA.setText("A. " + question.getOptionA());
        optionB.setText("B. " + question.getOptionB());
        optionC.setText("C. " + question.getOptionC());
        optionD.setText("D. " + question.getOptionD());
        optionGroup.selectToggle(null);

        String selectedOption = examAnswers.get(question.getId());
        if (selectedOption != null) {
            switch (selectedOption) {
                case "A": optionGroup.selectToggle(optionA); break;
                case "B": optionGroup.selectToggle(optionB); break;
                case "C": optionGroup.selectToggle(optionC); break;
                case "D": optionGroup.selectToggle(optionD); break;
            }
        }
        progressLabel.setText(String.format("Question %d of %d", currentQuestionIndex + 1, examQuestions.size()));
        markReviewBox.setSelected(markedForReview.contains(question.getId()));
        updateGridColors();
    }

    private void moveQuestion(int delta) {
        saveAnswer();
        currentQuestionIndex += delta;
        if (currentQuestionIndex < 0) currentQuestionIndex = 0;
        if (currentQuestionIndex >= examQuestions.size()) currentQuestionIndex = examQuestions.size() - 1;
        updateExamQuestion();
    }

   private void saveAnswer() {
        Toggle selected = optionGroup.getSelectedToggle();
        if (selected != null) {
            Question question = examQuestions.get(currentQuestionIndex);
            if (selected == optionA) examAnswers.put(question.getId(), "A");
            else if (selected == optionB) examAnswers.put(question.getId(), "B");
            else if (selected == optionC) examAnswers.put(question.getId(), "C");
            else if (selected == optionD) examAnswers.put(question.getId(), "D");
        }
        
        // --- AUTO-SAVE THE EXAM SNAPSHOT ---
        if (!isPracticeMode && remainingSeconds != null) {
            com.autoexam.model.ExamState snapshot = new com.autoexam.model.ExamState(
                currentUser.getUsername(), 
                remainingSeconds.get(), 
                currentQuestionIndex, 
                examAnswers, 
                markedForReview
            );
            storageService.saveActiveExamState(snapshot);
        }
    }

private void completeExam() {
        if (examTimer != null) examTimer.stop();
        
        // 1. Record the final answer (This will accidentally trigger the auto-save)
        saveAnswer(); 
        
        // 2. --- THE FIX: Clear the state AFTER saveAnswer() runs! ---
        storageService.clearActiveExamState(currentUser.getUsername());
        // ------------------------------------------------------------
        
        long correct = examQuestions.stream()
                .filter(question -> Objects.equals(examAnswers.get(question.getId()), question.getAnswer()))
                .count();
        int total = examQuestions.size();
        
        int examDuration = prefs.getInt("exam_duration", 180);
        int elapsed = isPracticeMode ? 0 : (examDuration - remainingSeconds.get());
        
        // Save the results to the database
        ExamResult examResult = new ExamResult(currentUser.getUsername(), java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), (int) correct, total, elapsed, new HashMap<>(examAnswers));
        resultData.add(examResult);
        storageService.saveExamResults(new ArrayList<>(resultData));
        
        updateDashboardMetrics();
        
        // Clean up the UI
        primaryStage.setFullScreen(false); 
        examLayout.setVisible(false);
        examLayout.setManaged(false);
        modeSelection.setVisible(true);
        modeSelection.setManaged(true);
        
        // Show the final score beautifully in the main status label
        examStatusLabel.setText(String.format("🎉 Exam Submitted! You scored %d out of %d.", correct, total));
        examStatusLabel.setStyle("-fx-background-color: rgba(34, 197, 94, 0.1); -fx-text-fill: #16a34a; -fx-font-weight: bold; -fx-font-size: 18px; -fx-padding: 15px; -fx-background-radius: 10px;");
        examStatusLabel.setVisible(true);
    }
    private Node createReportsPane() {
        VBox root = new VBox(18);
        root.setPadding(new Insets(20));

        Label header = new Label("Reports and Export");
        header.getStyleClass().add("section-title");

        Label instructions = new Label("Generate summary documentation for scored exams. Export to PDF or Excel with one click.");
        instructions.setWrapText(true);
        instructions.getStyleClass().add("info-text");

        Button exportPdf = new Button("Export Results to PDF");
        exportPdf.getStyleClass().add("primary-button");
        exportPdf.setOnAction(event -> {
            Path path = storageService.exportResultsToPdf(new ArrayList<>(resultData));
            showFileStatus(path, "PDF Export completed");
        });

        Button exportExcel = new Button("Export Results to Excel");
        exportExcel.getStyleClass().add("primary-button");
        exportExcel.setOnAction(event -> {
            Path path = storageService.exportResultsToExcel(new ArrayList<>(resultData));
            showFileStatus(path, "Excel export completed");
        });

        TableView<ExamResult> tableView = new TableView<>(resultData);
        tableView.getColumns().addAll(List.of(
                createColumn("Username", "username", 160),
                createColumn("Date", "dateTime", 220),
                createColumn("Score", "score", 100),
                createColumn("Total", "total", 100),
                createColumn("Duration", "durationLabel", 140)
        ));
        tableView.setPrefHeight(360);

        HBox controls = new HBox(12, exportPdf, exportExcel);
        controls.setAlignment(Pos.CENTER_LEFT);

        root.getChildren().addAll(header, instructions, controls, tableView);
        return root;
    }

    private void showFileStatus(Path path, String message) {
        if (path != null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Export Complete");
            alert.setHeaderText(message);
            alert.setContentText("File saved to: " + path.toAbsolutePath());
            alert.showAndWait();
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(path.toFile());
                }
            } catch (IOException ignored) {}
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Export failed. Please try again.");
            alert.showAndWait();
        }
    }

    private String formatTime(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("Time remaining: %02d:%02d", mins, secs);
    }

    private TableColumn<ExamResult, String> createColumn(String title, String property, int minWidth) {
        TableColumn<ExamResult, String> column = new TableColumn<>(title);
        column.setMinWidth(minWidth);
        column.setCellValueFactory(cellData -> cellData.getValue().stringProperty(property));
        return column;
    }

    private TableColumn<Question, String> createQuestionColumn(String title, String property, int minWidth) {
        TableColumn<Question, String> column = new TableColumn<>(title);
        column.setMinWidth(minWidth);
        column.setCellValueFactory(cellData -> cellData.getValue().stringProperty(property));
        return column;
    }

    private void buildNavigationGrid() {
        navigationGrid.getChildren().clear();
        for (int i = 0; i < examQuestions.size(); i++) {
            Button btn = new Button(String.valueOf(i + 1));
            btn.setPrefSize(40, 40);
            
            final int targetIndex = i;
            btn.setOnAction(e -> {
                saveAnswer(); 
                currentQuestionIndex = targetIndex;
                updateExamQuestion();
            });
            navigationGrid.getChildren().add(btn);
        }
        updateGridColors();
    }

    private void updateGridColors() {
        if (navigationGrid == null || navigationGrid.getChildren().isEmpty()) return;
        
        for (int i = 0; i < examQuestions.size(); i++) {
            Button btn = (Button) navigationGrid.getChildren().get(i);
            Question q = examQuestions.get(i);
            
            btn.setStyle(""); 
            
            if (currentQuestionIndex == i) {
                btn.setStyle("-fx-border-color: #2b3a67; -fx-border-width: 2px; -fx-background-color: #e8eef7;"); 
            } else if (markedForReview.contains(q.getId())) {
                btn.setStyle("-fx-background-color: #fca311; -fx-text-fill: white;"); 
            } else if (examAnswers.containsKey(q.getId())) {
                btn.setStyle("-fx-background-color: #2d6a4f; -fx-text-fill: white;"); 
            } else {
                btn.setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: black;"); 
            }
        }
    }

   private Node createProfilePane() {
        VBox pane = new VBox(25);
        pane.setPadding(new Insets(30));
        pane.setAlignment(Pos.TOP_LEFT);

        Label header = new Label("Profile & Security Settings");
        header.getStyleClass().add("section-title");

        // ==========================================
        // CARD 1: PERSONAL INFORMATION
        // ==========================================
        VBox infoCard = new VBox(15);
        infoCard.getStyleClass().add("exam-card");
        infoCard.setPadding(new Insets(25));
        infoCard.setMaxWidth(600);

        Label infoLabel = new Label("Personal Information");
        infoLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label usernameDisplay = new Label("Username: " + currentUser.getUsername() + "  |  Role: " + currentUser.getRole());
        usernameDisplay.getStyleClass().add("subtitle-text");

        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(15); 
        infoGrid.setVgap(15);

        // Load the current data into the text fields
        TextField nameField = new TextField(currentUser.getFullName());
        nameField.setPrefWidth(250);
        
        TextField emailField = new TextField(currentUser.getEmail());
        emailField.setPrefWidth(250);
        
        ComboBox<String> genderBox = new ComboBox<>(FXCollections.observableArrayList("Male", "Female", "Other"));
        genderBox.setValue(currentUser.getGender());
        genderBox.setPrefWidth(150);

        String labelStyle = "-fx-font-weight: bold; -fx-text-fill: #475569;";
        
        Label nameLbl = new Label("Full Name:"); nameLbl.setStyle(labelStyle);
        Label emailLbl = new Label("Email Address:"); emailLbl.setStyle(labelStyle);
        Label genderLbl = new Label("Sex:"); genderLbl.setStyle(labelStyle);

        infoGrid.add(nameLbl, 0, 0); infoGrid.add(nameField, 1, 0);
        infoGrid.add(emailLbl, 0, 1); infoGrid.add(emailField, 1, 1);
        infoGrid.add(genderLbl, 0, 2); infoGrid.add(genderBox, 1, 2);

        Label infoStatus = new Label();
        infoStatus.getStyleClass().add("status-label");

        Button updateInfoBtn = new Button("Save Profile Details");
        updateInfoBtn.getStyleClass().add("secondary-button");
        
        updateInfoBtn.setOnAction(e -> {
            if (nameField.getText().isEmpty() || emailField.getText().isEmpty() || genderBox.getValue() == null) {
                infoStatus.setText("Fields cannot be empty.");
                infoStatus.setStyle("-fx-text-fill: #ef4444;");
                return;
            }
            if (storageService.updateProfile(currentUser.getUsername(), nameField.getText(), emailField.getText(), genderBox.getValue())) {
                // Update the active memory session so the changes reflect immediately
                currentUser.setFullName(nameField.getText());
                currentUser.setEmail(emailField.getText());
                currentUser.setGender(genderBox.getValue());
                
                infoStatus.setText("Profile updated successfully!");
                infoStatus.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
            } else {
                infoStatus.setText("Database error updating profile.");
                infoStatus.setStyle("-fx-text-fill: #ef4444;");
            }
        });

        infoCard.getChildren().addAll(infoLabel, usernameDisplay, new Separator(), infoGrid, updateInfoBtn, infoStatus);

        // ==========================================
        // CARD 2: SECURITY & PASSWORD
        // ==========================================
        VBox securityCard = new VBox(15);
        securityCard.getStyleClass().add("exam-card");
        securityCard.setPadding(new Insets(25));
        securityCard.setMaxWidth(600);

        Label pwLabel = new Label("Change Password");
        pwLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        PasswordField newPw = new PasswordField();
        newPw.setPromptText("Enter new password");
        newPw.setMaxWidth(350);
        
        PasswordField confirmPw = new PasswordField();
        confirmPw.setPromptText("Confirm new password");
        confirmPw.setMaxWidth(350);

        Label pwStatus = new Label();
        pwStatus.getStyleClass().add("status-label");

        Button updatePwBtn = new Button("Update Password");
        updatePwBtn.getStyleClass().add("danger-button"); 
        
        updatePwBtn.setOnAction(e -> {
            if (newPw.getText().isEmpty() || confirmPw.getText().isEmpty()) {
                pwStatus.setText("Fields cannot be empty.");
                pwStatus.setStyle("-fx-text-fill: #ef4444;");
                return;
            }
            if (!newPw.getText().equals(confirmPw.getText())) {
                pwStatus.setText("Passwords do not match!");
                pwStatus.setStyle("-fx-text-fill: #ef4444;");
                return;
            }
            if (storageService.changePassword(currentUser.getUsername(), newPw.getText())) {
                pwStatus.setText("Password updated successfully!");
                pwStatus.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                newPw.clear(); confirmPw.clear();
            } else {
                pwStatus.setText("Database error updating password.");
                pwStatus.setStyle("-fx-text-fill: #ef4444;");
            }
        });

        securityCard.getChildren().addAll(pwLabel, new Separator(), newPw, confirmPw, updatePwBtn, pwStatus);

        // Assemble the final screen
        pane.getChildren().addAll(header, infoCard, securityCard);
        
        // Wrap in a ScrollPane for smaller laptop screens
        ScrollPane scrollPane = new ScrollPane(pane);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        
        return scrollPane;
    }

  // ==========================================
    // ADMIN LEADERBOARD/RANKINGS
    // ==========================================
    private Node createLeaderboardPane() {
        VBox pane = new VBox(18);
        pane.setPadding(new Insets(20));

        Label header = new Label("Global Student Rankings");
        header.getStyleClass().add("section-title");

        Label subtitle = new Label("Ranks all users based on their historical percentage accuracy across all exam attempts.");
        subtitle.getStyleClass().add("subtitle-text");

        Map<String, Double> studentPercentages = new HashMap<>();
        Map<String, Integer> studentAttempts = new HashMap<>();

        for (ExamResult r : resultData) {
            // --- THE FIX: Completely ignore the admin's test scores ---
            if ("admin".equalsIgnoreCase(r.getUsername())) {
                continue; 
            }
            // ----------------------------------------------------------

            double percent = r.getTotal() > 0 ? ((double) r.getScore() / r.getTotal()) * 100 : 0;
            studentAttempts.put(r.getUsername(), studentAttempts.getOrDefault(r.getUsername(), 0) + 1);
            studentPercentages.put(r.getUsername(), studentPercentages.getOrDefault(r.getUsername(), 0.0) + percent);
        }

        class Ranking {
            String name; int attempts; String avg;
            Ranking(String n, int a, String av) { this.name = n; this.attempts = a; this.avg = av; }
        }

        ObservableList<Ranking> rankings = FXCollections.observableArrayList();
        for (String user : studentAttempts.keySet()) {
            double avgPercent = studentPercentages.get(user) / studentAttempts.get(user);
            rankings.add(new Ranking(user, studentAttempts.get(user), String.format("%.1f%%", avgPercent)));
        }

        rankings.sort((r1, r2) -> {
            double val1 = Double.parseDouble(r1.avg.replace("%", ""));
            double val2 = Double.parseDouble(r2.avg.replace("%", ""));
            return Double.compare(val2, val1);
        });

        TableView<Ranking> table = new TableView<>(rankings);
        
        TableColumn<Ranking, String> rankCol = new TableColumn<>("Rank");
        rankCol.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : String.valueOf(getIndex() + 1));
                setStyle("-fx-font-weight: bold; -fx-alignment: CENTER;");
            }
        });
        rankCol.setPrefWidth(70);

        TableColumn<Ranking, String> nameCol = new TableColumn<>("Student Username");
        nameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().name));
        nameCol.setPrefWidth(250);

        TableColumn<Ranking, Number> attemptsCol = new TableColumn<>("Exams Taken");
        attemptsCol.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().attempts));
        attemptsCol.setPrefWidth(120);

        TableColumn<Ranking, String> avgCol = new TableColumn<>("Average Accuracy");
        avgCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().avg));
        avgCol.setPrefWidth(150);

        // Admin Action Column (View, Reset, Delete)
        TableColumn<Ranking, Void> actionCol = new TableColumn<>("Admin Actions");
        actionCol.setCellFactory(param -> new javafx.scene.control.TableCell<>() {
            private final Button viewBtn = new Button("View");
            private final Button resetBtn = new Button("Reset PW");
            private final Button deleteBtn = new Button("Delete");
            private final HBox btnBox = new HBox(8, viewBtn, resetBtn, deleteBtn); 

            {
                viewBtn.getStyleClass().add("primary-button");
                viewBtn.setStyle("-fx-font-size: 12px; -fx-padding: 6px 10px;");
                
                resetBtn.getStyleClass().add("secondary-button");
                resetBtn.setStyle("-fx-font-size: 12px; -fx-padding: 6px 10px;");
                
                deleteBtn.getStyleClass().add("danger-button");
                deleteBtn.setStyle("-fx-font-size: 12px; -fx-padding: 6px 10px;");

                viewBtn.setOnAction(event -> {
                    String uName = getTableView().getItems().get(getIndex()).name;
                    showUserProfileDialog(uName);
                });

                resetBtn.setOnAction(event -> {
                    String uName = getTableView().getItems().get(getIndex()).name;
                    storageService.adminResetPassword(uName);
                    Alert a = new Alert(Alert.AlertType.INFORMATION, "Password for '" + uName + "' forced to: password123");
                    a.setHeaderText("Success");
                    a.show();
                });

                deleteBtn.setOnAction(event -> {
                    Ranking item = getTableView().getItems().get(getIndex());
                    String uName = item.name;

                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirm Deletion");
                    confirm.setHeaderText("Delete User: " + uName);
                    confirm.setContentText("Are you sure? This will permanently delete the user and scrub all their exam history from the system.");

                    confirm.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            if (storageService.deleteUser(uName)) {
                                getTableView().getItems().remove(item);
                                resultData.removeIf(r -> r.getUsername().equals(uName));
                                updateDashboardMetrics(); 
                            } else {
                                new Alert(Alert.AlertType.ERROR, "Cannot delete the master admin account!").show();
                            }
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnBox);
            }
        });
        
        actionCol.setPrefWidth(240);

        table.getColumns().addAll(rankCol, nameCol, attemptsCol, avgCol, actionCol);
        table.setMinHeight(300);
        VBox.setVgrow(table, Priority.ALWAYS);

        pane.getChildren().addAll(header, subtitle, table);
        
        ScrollPane scrollPane = new ScrollPane(pane);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scrollPane;
    }
    // --- NEW: Admin Pop-up to View Student Profiles ---
    private void showUserProfileDialog(String username) {
        // Fetch the user's full data from the database
        User student = storageService.loadUsers().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst().orElse(null);

        if (student == null) return;

        Stage profileStage = new Stage();
        profileStage.setTitle("Student Profile - " + username);

        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: #f8fafc;");

        Label header = new Label("Student Profile Card");
        header.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        // Create a beautiful modern card to hold the data
        VBox card = new VBox(15);
        card.setStyle("-fx-background-color: #ffffff; -fx-padding: 25px; -fx-background-radius: 12px; -fx-border-radius: 12px; -fx-border-color: #e2e8f0; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");

        String labelStyle = "-fx-font-weight: bold; -fx-text-fill: #64748b; -fx-font-size: 13px; -fx-text-transform: uppercase;";
        String valueStyle = "-fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-font-size: 16px;";

        // Build the layout grid
        GridPane grid = new GridPane();
        grid.setVgap(15); grid.setHgap(30);

        Label nameLbl = new Label("FULL NAME"); nameLbl.setStyle(labelStyle);
        Label nameVal = new Label(student.getFullName()); nameVal.setStyle(valueStyle);
        
        Label userLbl = new Label("USERNAME"); userLbl.setStyle(labelStyle);
        Label userVal = new Label(student.getUsername()); userVal.setStyle(valueStyle);

        Label emailLbl = new Label("EMAIL ADDRESS"); emailLbl.setStyle(labelStyle);
        Label emailVal = new Label(student.getEmail()); emailVal.setStyle(valueStyle);

        Label sexLbl = new Label("SEX / GENDER"); sexLbl.setStyle(labelStyle);
        Label sexVal = new Label(student.getGender()); sexVal.setStyle(valueStyle);

        Label roleLbl = new Label("SYSTEM ROLE"); roleLbl.setStyle(labelStyle);
        Label roleVal = new Label(student.getRole()); roleVal.setStyle("-fx-font-weight: bold; -fx-text-fill: #10b981; -fx-font-size: 16px;");

        grid.add(nameLbl, 0, 0); grid.add(nameVal, 0, 1);
        grid.add(userLbl, 1, 0); grid.add(userVal, 1, 1);
        grid.add(emailLbl, 0, 2); grid.add(emailVal, 0, 3);
        grid.add(sexLbl, 1, 2); grid.add(sexVal, 1, 3);
        grid.add(roleLbl, 0, 4); grid.add(roleVal, 0, 5);

        card.getChildren().add(grid);

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #cbd5e1; -fx-text-fill: #0f172a; -fx-font-weight: bold; -fx-padding: 8px 20px; -fx-background-radius: 8px; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> profileStage.close());

        content.getChildren().addAll(header, card, closeBtn);

        Scene scene = new Scene(content, 500, 450);
        profileStage.setScene(scene);
        profileStage.show();
    }
    // Smart Option Shuffler ---
    private Question shuffleQuestion(Question original) {
        // 1. Clone the question so we don't mess up the database
        Question q = new Question(original.getId(), original.getText(), original.getOptionA(), original.getOptionB(), original.getOptionC(), original.getOptionD(), original.getAnswer(), original.getTopic(), original.getExamType());
        
        // 2. Figure out the actual TEXT of the correct answer
        String correctText = switch(original.getAnswer()) {
            case "A" -> original.getOptionA();
            case "B" -> original.getOptionB();
            case "C" -> original.getOptionC();
            case "D" -> original.getOptionD();
            default -> original.getOptionA();
        };

        // 3. Put options in a list and shuffle them
        List<String> options = new ArrayList<>(Arrays.asList(original.getOptionA(), original.getOptionB(), original.getOptionC(), original.getOptionD()));
        Collections.shuffle(options);

        // 4. Re-assign the shuffled options
        q.setOptionA(options.get(0));
        q.setOptionB(options.get(1));
        q.setOptionC(options.get(2));
        q.setOptionD(options.get(3));

        // 5. Find where the correct text ended up and update the Answer Key!
        if (options.get(0).equals(correctText)) q.setAnswer("A");
        else if (options.get(1).equals(correctText)) q.setAnswer("B");
        else if (options.get(2).equals(correctText)) q.setAnswer("C");
        else if (options.get(3).equals(correctText)) q.setAnswer("D");

        return q;
    }
}