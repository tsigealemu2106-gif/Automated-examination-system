package com.autoexam;

import com.autoexam.model.ExamResult;
import com.autoexam.model.ExamState;
import com.autoexam.model.Question;
import com.autoexam.model.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class StorageService {
    private static final Path DATA_FOLDER = Paths.get(System.getProperty("user.home"), ".autoexam");
    private static final Path STATE_FILE = DATA_FOLDER.resolve("active_states.json");
    private static final Path USER_FILE = DATA_FOLDER.resolve("users.json");
    private static final Path QUESTION_FILE = DATA_FOLDER.resolve("questions.json");
    private static final Path RESULT_FILE = DATA_FOLDER.resolve("results.json");

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public void initialize() {
        try {
            if (Files.notExists(DATA_FOLDER)) Files.createDirectories(DATA_FOLDER);
            if (Files.notExists(USER_FILE)) {
                User defaultUser = createUser("admin", "password", "ADMIN", "N/A", "N/A", "System Administrator", "admin@autoexam.edu", "N/A");
                saveUsers(List.of(defaultUser));
            }
            if (Files.notExists(QUESTION_FILE)) saveQuestions(getDefaultQuestionBank());
            if (Files.notExists(RESULT_FILE)) saveExamResults(new ArrayList<>());
            if (Files.notExists(STATE_FILE)) writeJson(STATE_FILE, new ArrayList<ExamState>());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public User authenticate(String username, String password) {
        return loadUsers().stream()
                .filter(user -> user.getUsername().equals(username) && PasswordUtils.verifyPassword(password, user.getPasswordHash(), user.getSalt()))
                .findFirst()
                .orElse(null);
    }

    public boolean usernameExists(String username) {
        return loadUsers().stream()
                .anyMatch(user -> user.getUsername().equalsIgnoreCase(username));
    }

    public boolean addUser(String username, String password, String question, String answer, String fullName, String email, String gender) {
        if (usernameExists(username)) return false;
        List<User> users = loadUsers();
        users.add(createUser(username, password, "STUDENT", question, answer, fullName, email, gender));
        saveUsers(users);
        return true;
    }

    public List<User> loadUsers() {
        return readJsonList(USER_FILE, new TypeToken<List<User>>() {}.getType());
    }

    public void saveUsers(List<User> users) {
        writeJson(USER_FILE, users);
    }

    public List<Question> loadQuestions() {
        return readJsonList(QUESTION_FILE, new TypeToken<List<Question>>() {}.getType());
    }

    public void saveQuestions(List<Question> questions) {
        writeJson(QUESTION_FILE, questions);
    }

    public List<ExamResult> loadExamResults() {
        return readJsonList(RESULT_FILE, new TypeToken<List<ExamResult>>() {}.getType());
    }

    public void saveExamResults(List<ExamResult> results) {
        writeJson(RESULT_FILE, results);
    }

    public Path exportResultsToPdf(List<ExamResult> results) {
        Path pdfPath = DATA_FOLDER.resolve("exam-results.pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            PDPageContentStream content = new PDPageContentStream(document, page);
            try {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 18);
                content.newLineAtOffset(40, 740);
                content.showText("Automated Examination Results");
                content.endText();

                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(40, 710);
                content.showText("Generated on " + java.time.LocalDateTime.now());
                content.endText();

                float y = 680;
                for (ExamResult result : results) {
                    if (y < 60) {
                        content.close();
                        page = new PDPage(PDRectangle.LETTER);
                        document.addPage(page);
                        content = new PDPageContentStream(document, page);
                        y = 740;
                    }
                    content.beginText();
                    content.setFont(PDType1Font.HELVETICA, 11);
                    content.newLineAtOffset(40, y);
                    content.showText(String.format("%s | %s | Score: %d/%d | Duration: %s",
                            result.getUsername(), result.getDateTime(), result.getScore(), result.getTotal(), result.getDurationLabel()));
                    content.endText();
                    y -= 20;
                }
            } finally {
                content.close();
            }
            document.save(pdfPath.toFile());
            return pdfPath;
        } catch (IOException e) {
            return null;
        }
    }

    public Path exportResultsToExcel(List<ExamResult> results) {
        Path excelPath = DATA_FOLDER.resolve("exam-results.xlsx");
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Exam Results");
            Row header = sheet.createRow(0);
            String[] columns = {"Username", "Date", "Score", "Total", "Duration"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
            }
            int rowIndex = 1;
            for (ExamResult result : results) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(result.getUsername());
                row.createCell(1).setCellValue(result.getDateTime());
                row.createCell(2).setCellValue(result.getScore());
                row.createCell(3).setCellValue(result.getTotal());
                row.createCell(4).setCellValue(result.getDurationLabel());
            }
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }
            try (var output = Files.newOutputStream(excelPath)) {
                workbook.write(output);
            }
            return excelPath;
        } catch (IOException e) {
            return null;
        }
    }

    private <T> List<T> readJsonList(Path path, Type typeOfT) {
        try {
            if (Files.notExists(path)) return new ArrayList<>();
            String content = Files.readString(path, StandardCharsets.UTF_8);
            return gson.fromJson(content, typeOfT);
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private void writeJson(Path path, Object data) {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private User createUser(String username, String password, String role, String question, String answer, String fullName, String email, String gender) {
        byte[] salt = PasswordUtils.generateSalt();
        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        String hash = PasswordUtils.hashPassword(password, salt);
        return new User(username, hash, saltBase64, role, question, answer, fullName, email, gender);
    }

    public String getSecurityQuestion(String username) {
        User u = loadUsers().stream().filter(user -> user.getUsername().equalsIgnoreCase(username)).findFirst().orElse(null);
        return (u != null) ? u.getSecurityQuestion() : null;
    }

    public boolean verifySecurityAnswer(String username, String answer) {
        User u = loadUsers().stream().filter(user -> user.getUsername().equalsIgnoreCase(username)).findFirst().orElse(null);
        return u != null && u.getSecurityAnswer() != null && u.getSecurityAnswer().equalsIgnoreCase(answer.trim());
    }

    public void adminResetPassword(String username) {
        changePassword(username, "password123");
    }

    public boolean changePassword(String username, String newPassword) {
        List<User> users = loadUsers();
        for (User u : users) {
            if (u.getUsername().equals(username)) {
                byte[] newSalt = PasswordUtils.generateSalt();
                String newSaltBase64 = Base64.getEncoder().encodeToString(newSalt);
                String newHash = PasswordUtils.hashPassword(newPassword, newSalt);
                u.setPasswordHash(newHash);
                u.setSalt(newSaltBase64);
                saveUsers(users);
                return true;
            }
        }
        return false;
    }

    private List<Question> getDefaultQuestionBank() {
        List<Question> questions = new ArrayList<>();
        questions.add(new Question("1", "What is the capital of France?", "Paris", "Berlin", "Rome", "Madrid", "A", "Geography", "Official"));
        questions.add(new Question("2", "Which object-oriented principle describes hiding details?", "Inheritance", "Encapsulation", "Polymorphism", "Abstraction", "B", "Programming", "Official"));
        questions.add(new Question("3", "What is the file extension for Java classes?", ".java", ".class", ".jar", ".xml", "B", "Programming", "Practice"));
        questions.add(new Question("4", "What is metadata used for?", "Storing data", "Describing data", "Encrypting files", "Sorting arrays", "B", "General IT", "Practice"));
        questions.add(new Question("5", "Which format is best for spreadsheets?", "PDF", "TXT", "XLSX", "JPEG", "C", "Office", "Practice"));
        return questions;
    }

    public boolean deleteUser(String username) {
        if ("admin".equalsIgnoreCase(username)) {
            return false;
        }
        List<User> users = loadUsers();
        boolean userRemoved = users.removeIf(u -> u.getUsername().equalsIgnoreCase(username));
        if (userRemoved) {
            saveUsers(users);
            List<ExamResult> results = loadExamResults();
            results.removeIf(r -> r.getUsername().equalsIgnoreCase(username));
            saveExamResults(results);
        }
        return userRemoved;
    }

    // --- NEW: Update User Profile Info ---
    public boolean updateProfile(String username, String newFullName, String newEmail, String newGender) {
        List<User> users = loadUsers();
        for (User u : users) {
            if (u.getUsername().equals(username)) {
                u.setFullName(newFullName);
                u.setEmail(newEmail);
                u.setGender(newGender);
                saveUsers(users); 
                return true;
            }
        }
        return false;
    }

    // --- Bulk Excel Import ---
    public int importQuestionsFromExcel(Path excelPath) {
        int importedCount = 0;
        try (Workbook workbook = new XSSFWorkbook(Files.newInputStream(excelPath))) {
            Sheet sheet = workbook.getSheetAt(0);
            List<Question> existing = loadQuestions();
            
            // Start at row 1 to skip the header row!
            for (int i = 1; i <= sheet.getLastRowNum(); i++) { 
                Row row = sheet.getRow(i);
                if (row == null || row.getCell(0) == null) continue;
                
                String topic = row.getCell(0).getStringCellValue();
                String type = row.getCell(1).getStringCellValue();
                String text = row.getCell(2).getStringCellValue();
                String optA = row.getCell(3).getStringCellValue();
                String optB = row.getCell(4).getStringCellValue();
                String optC = row.getCell(5).getStringCellValue();
                String optD = row.getCell(6).getStringCellValue();
                String ans = row.getCell(7).getStringCellValue();
                
                String id = String.valueOf(System.currentTimeMillis() + i).substring(8);
                existing.add(new Question(id, text, optA, optB, optC, optD, ans, topic, type));
                importedCount++;
            }
            saveQuestions(existing);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return importedCount;
    }

    // --- FEATURE: Auto-Save / Resume Exam Logic ---
    public ExamState getActiveExamState(String username) {
        List<ExamState> states = readJsonList(STATE_FILE, new TypeToken<List<ExamState>>() {}.getType());
        return states.stream().filter(s -> s.getUsername().equals(username)).findFirst().orElse(null);
    }

    public void saveActiveExamState(ExamState newState) {
        List<ExamState> states = readJsonList(STATE_FILE, new TypeToken<List<ExamState>>() {}.getType());
        states.removeIf(s -> s.getUsername().equals(newState.getUsername())); // Remove old snapshot
        states.add(newState); // Save new snapshot
        writeJson(STATE_FILE, states);
    }

    public void clearActiveExamState(String username) {
        List<ExamState> states = readJsonList(STATE_FILE, new TypeToken<List<ExamState>>() {}.getType());
        states.removeIf(s -> s.getUsername().equals(username));
        writeJson(STATE_FILE, states);
    }
}