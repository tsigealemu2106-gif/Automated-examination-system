package com.autoexam;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * Logo component for the Automated Examination System
 */
public class AppLogo extends Group {

    public AppLogo(double size) {
        createLogo(size);
    }

    private void createLogo(double size) {
        // Scale factor for the logo
        double scale = size / 100.0;

        // Create the main circular background
        Circle background = new Circle(size * 0.4);
        background.setFill(Color.web("#2563eb"));
        background.setStroke(Color.web("#1d4ed8"));
        background.setStrokeWidth(3 * scale);

        // Create the document/paper shape (representing exam/test)
        Rectangle document = new Rectangle(size * 0.25, size * 0.35);
        document.setFill(Color.WHITE);
        document.setStroke(Color.web("#e2e8f0"));
        document.setStrokeWidth(2 * scale);
        document.setArcWidth(8 * scale);
        document.setArcHeight(8 * scale);
        document.setX(-size * 0.125);
        document.setY(-size * 0.175);

        // Create lines on the document (representing text/questions)
        for (int i = 0; i < 4; i++) {
            Line line = new Line(
                -size * 0.1,
                -size * 0.12 + i * size * 0.06,
                size * 0.08,
                -size * 0.12 + i * size * 0.06
            );
            line.setStroke(Color.web("#94a3b8"));
            line.setStrokeWidth(2 * scale);
            this.getChildren().add(line);
        }

        // Create a checkmark (representing completion/grading)
        Path checkmark = new Path();
        checkmark.getElements().add(new MoveTo(-size * 0.08, size * 0.02));
        checkmark.getElements().add(new LineTo(-size * 0.03, size * 0.07));
        checkmark.getElements().add(new LineTo(size * 0.08, -size * 0.03));
        checkmark.setStroke(Color.web("#10b981"));
        checkmark.setStrokeWidth(4 * scale);
        checkmark.setFill(Color.TRANSPARENT);

        // Create the text "AUTO" above the icon
        Text autoText = new Text("AUTO");
        autoText.setFont(Font.font("Segoe UI", FontWeight.BOLD, size * 0.15));
        autoText.setFill(Color.web("#1e293b"));
        autoText.setX(-autoText.getLayoutBounds().getWidth() / 2);
        autoText.setY(-size * 0.25);

        // Create the text "EXAM" below the icon
        Text examText = new Text("EXAM");
        examText.setFont(Font.font("Segoe UI", FontWeight.BOLD, size * 0.15));
        examText.setFill(Color.web("#1e293b"));
        examText.setX(-examText.getLayoutBounds().getWidth() / 2);
        examText.setY(size * 0.35);

        // Add a subtle shadow effect to the background
        Circle shadow = new Circle(size * 0.4);
        shadow.setFill(Color.web("#000000", 0.1));
        shadow.setTranslateX(2 * scale);
        shadow.setTranslateY(2 * scale);

        // Add all elements to the group
        this.getChildren().addAll(shadow, background, document, checkmark, autoText, examText);
    }

    /**
     * Create a compact version of the logo (icon only, no text)
     */
    public static Group createIcon(double size) {
        Group icon = new Group();
        double scale = size / 100.0;

        // Create the main circular background
        Circle background = new Circle(size * 0.4);
        background.setFill(Color.web("#2563eb"));
        background.setStroke(Color.web("#1d4ed8"));
        background.setStrokeWidth(3 * scale);

        // Create the document/paper shape
        Rectangle document = new Rectangle(size * 0.25, size * 0.35);
        document.setFill(Color.WHITE);
        document.setStroke(Color.web("#e2e8f0"));
        document.setStrokeWidth(2 * scale);
        document.setArcWidth(8 * scale);
        document.setArcHeight(8 * scale);
        document.setX(-size * 0.125);
        document.setY(-size * 0.175);

        // Create lines on the document
        for (int i = 0; i < 3; i++) {
            Line line = new Line(
                -size * 0.1,
                -size * 0.12 + i * size * 0.08,
                size * 0.06,
                -size * 0.12 + i * size * 0.08
            );
            line.setStroke(Color.web("#94a3b8"));
            line.setStrokeWidth(2 * scale);
            icon.getChildren().add(line);
        }

        // Create a checkmark
        Path checkmark = new Path();
        checkmark.getElements().add(new MoveTo(-size * 0.08, size * 0.02));
        checkmark.getElements().add(new LineTo(-size * 0.03, size * 0.07));
        checkmark.getElements().add(new LineTo(size * 0.08, -size * 0.03));
        checkmark.setStroke(Color.web("#10b981"));
        checkmark.setStrokeWidth(4 * scale);
        checkmark.setFill(Color.TRANSPARENT);

        // Add shadow
        Circle shadow = new Circle(size * 0.4);
        shadow.setFill(Color.web("#000000", 0.1));
        shadow.setTranslateX(2 * scale);
        shadow.setTranslateY(2 * scale);

        icon.getChildren().addAll(shadow, background, document, checkmark);
        return icon;
    }
}