package com.StudentManagementProject;

import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableModel;

/**
 * StudentManagement.java
 * Admin UI for Student Management Project — updated to call RequestsDialog via reflection
 *
 * Package: com.StudentManagementProject
 */
public class StudentManagement {

    JFrame frame;
    private JTextField idField, nameField, dobField, ageField, emailField, phoneField, fatherField, searchField;
    private JTextArea addressArea;
    private JComboBox<String> genderCombo, courseCombo, semesterCombo;
    private JTable table;
    private DefaultTableModel model;
    private Database.AuthResult auth; // logged-in user
    private static final DateTimeFormatter DOB_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                // Nimbus L&F for modern look
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception ignored) {}

            try {
                // Initialize DB (creates tables, migrations, default admin)
                Database.dbInit();

                // Show login dialog
                Database.AuthResult ar = LoginDialog.showLogin(null);
                if (ar == null || !ar.ok) {
                    System.out.println("Login cancelled or failed. Exiting.");
                    System.exit(0);
                }

                // If admin -> open admin UI, else open student view
                if ("admin".equalsIgnoreCase(ar.role)) {
                    StudentManagement win = new StudentManagement(ar);
                    win.frame.setVisible(true);
                    win.loadAllStudents();
                } else {
                    // open StudentView for student
                    StudentView sv = new StudentView(ar);
                    sv.setVisible(true);
                }

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Startup failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }

    public StudentManagement(Database.AuthResult auth) {
        this.auth = auth;
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Student Management — Admin: " + (auth == null ? "unknown" : auth.username));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 640);
        frame.setLocationRelativeTo(null);

        JSplitPane split = new JSplitPane();
        split.setDividerLocation(420);
        frame.getContentPane().add(split, BorderLayout.CENTER);

        split.setLeftComponent(buildFormPanel());
        split.setRightComponent(buildTablePanel());
    }

    private JPanel buildFormPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,6,6,6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int y = 0;

        // Student ID
        gbc.gridx = 0; gbc.gridy = y; gbc.weightx = 0.0;
        p.add(new JLabel("Student ID:"), gbc);
        idField = new JTextField();
        gbc.gridx = 1; gbc.gridy = y++; gbc.weightx = 1.0;
        p.add(idField, gbc);

        // Name
        gbc.gridx = 0; gbc.gridy = y; gbc.weightx = 0.0;
        p.add(new JLabel("Name:"), gbc);
        nameField = new JTextField();
        gbc.gridx = 1; gbc.gridy = y++; gbc.weightx = 1.0;
        p.add(nameField, gbc);

        // Father's Name
        gbc.gridx = 0; gbc.gridy = y;
        p.add(new JLabel("Father's Name:"), gbc);
        fatherField = new JTextField();
        gbc.gridx = 1; gbc.gridy = y++;
        p.add(fatherField, gbc);

        // Gender
        gbc.gridx = 0; gbc.gridy = y;
        p.add(new JLabel("Gender:"), gbc);
        genderCombo = new JComboBox<>(new String[] {"Male","Female","Other"});
        gbc.gridx = 1; gbc.gridy = y++;
        p.add(genderCombo, gbc);

        // DOB
        gbc.gridx = 0; gbc.gridy = y;
        p.add(new JLabel("Date of Birth (YYYY-MM-DD):"), gbc);
        dobField = new JTextField();
        dobField.setToolTipText("Format: yyyy-MM-dd (e.g. 2003-05-21)");
        gbc.gridx = 1; gbc.gridy = y++;
        p.add(dobField, gbc);

        // Age (computed)
        gbc.gridx = 0; gbc.gridy = y;
        p.add(new JLabel("Age:"), gbc);
        ageField = new JTextField();
        ageField.setEditable(false);
        gbc.gridx = 1; gbc.gridy = y++;
        p.add(ageField, gbc);

        // compute age when dob changes
        dobField.getDocument().addDocumentListener(new SimpleDocListener(this::computeAndSetAge));

        // Email
        gbc.gridx = 0; gbc.gridy = y;
        p.add(new JLabel("Email:"), gbc);
        emailField = new JTextField();
        gbc.gridx = 1; gbc.gridy = y++;
        p.add(emailField, gbc);

        // Phone
        gbc.gridx = 0; gbc.gridy = y;
        p.add(new JLabel("Phone:"), gbc);
        phoneField = new JTextField();
        gbc.gridx = 1; gbc.gridy = y++;
        p.add(phoneField, gbc);

        // Course
        gbc.gridx = 0; gbc.gridy = y;
        p.add(new JLabel("Course:"), gbc);
        courseCombo = new JComboBox<>(new String[] {"B.Tech - CSE","B.Tech - ECE","B.Sc","MCA","M.Tech"});
        courseCombo.setEditable(true);
        gbc.gridx = 1; gbc.gridy = y++;
        p.add(courseCombo, gbc);

        // Semester
        gbc.gridx = 0; gbc.gridy = y;
        p.add(new JLabel("Semester:"), gbc);
        semesterCombo = new JComboBox<>(new String[] {"Sem 1","Sem 2","Sem 3","Sem 4","Sem 5","Sem 6","Sem 7","Sem 8"});
        gbc.gridx = 1; gbc.gridy = y++;
        p.add(semesterCombo, gbc);

        // Address
        gbc.gridx = 0; gbc.gridy = y; gbc.anchor = GridBagConstraints.NORTHWEST;
        p.add(new JLabel("Address:"), gbc);
        addressArea = new JTextArea(5, 20);
        addressArea.setLineWrap(true);
        addressArea.setWrapStyleWord(true);
        gbc.gridx = 1; gbc.gridy = y++; gbc.fill = GridBagConstraints.BOTH;
        p.add(new JScrollPane(addressArea), gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.WEST;

        // Buttons row
        JPanel btnRow = new JPanel();
        JButton insertBtn = styledButton("Insert");
        JButton updateBtn = styledButton("Update");
        JButton deleteBtn = styledButton("Delete (table)");
        JButton clearBtn = styledButton("Clear");
        JButton changePwdBtn = styledButton("Change Password");
        JButton manageReqBtn = styledButton("Manage Requests"); // NEW

        insertBtn.addActionListener(e -> onInsert());
        updateBtn.addActionListener(e -> onUpdate());
        deleteBtn.addActionListener(e -> onDeleteSelected());
        clearBtn.addActionListener(e -> clearForm());
        changePwdBtn.addActionListener(e -> {
            // open change password dialog as admin
            if (auth != null) ChangePasswordDialog.showDialog(frame, auth.username, true);
            else JOptionPane.showMessageDialog(frame, "Admin context not available.", "Error", JOptionPane.ERROR_MESSAGE);
        });

        // Use reflection to call RequestsDialog.showForAdmin(...) so this file compiles even if RequestsDialog.java is missing.
        manageReqBtn.addActionListener(e -> {
            if (auth == null) {
                JOptionPane.showMessageDialog(frame, "Admin authentication missing.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                Class<?> cls = Class.forName("com.StudentManagementProject.RequestsDialog");
                java.lang.reflect.Method m = cls.getMethod("showForAdmin", javax.swing.JFrame.class, String.class);
                m.invoke(null, frame, auth.username);
            } catch (ClassNotFoundException cnf) {
                JOptionPane.showMessageDialog(frame, "RequestsDialog class not found.\nPlease add RequestsDialog.java to the project to use Manage Requests.", "Not found", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Failed to open Manage Requests: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        btnRow.add(insertBtn);
        btnRow.add(updateBtn);
        btnRow.add(deleteBtn);
        btnRow.add(clearBtn);
        btnRow.add(changePwdBtn);
        btnRow.add(manageReqBtn); // add to UI

        gbc.gridx = 0; gbc.gridy = y; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        p.add(btnRow, gbc);

        return p;
    }

    private JPanel buildTablePanel() {
        JPanel p = new JPanel(new BorderLayout(8,8));
        p.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));

        JPanel toolbar = new JPanel();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
        toolbar.add(new JLabel(" Search: "));
        searchField = new JTextField();
        toolbar.add(searchField);
        toolbar.add(Box.createRigidArea(new Dimension(8,0)));
        JButton showAll = styledButton("Show All");
        toolbar.add(showAll);
        p.add(toolbar, BorderLayout.NORTH);

        model = new DefaultTableModel();
        model.addColumn("Id");
        model.addColumn("Name");
        model.addColumn("Father");
        model.addColumn("DOB");
        model.addColumn("Gender");
        model.addColumn("Phone");
        model.addColumn("Course/Sem");
        model.addColumn("E-mail");
        model.addColumn("Address");
        model.addColumn("Age");
        model.addColumn("Course");
        model.addColumn("Semester");

        table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        p.add(new JScrollPane(table), BorderLayout.CENTER);

        // Selection -> populate form
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                populateFormFromTable();
            }
        });

        // Search live
        searchField.getDocument().addDocumentListener(new SimpleDocListener(() -> {
            String q = searchField.getText().trim();
            try {
                if (q.isEmpty()) {
                    // do nothing, user can press show all
                } else {
                    Database.searchStudents(model, q);
                }
            } catch (Exception ex) {
                showError("Search failed: " + ex.getMessage());
            }
        }));

        showAll.addActionListener(e -> loadAllStudents());

        return p;
    }

    private JButton styledButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setMargin(new Insets(6, 12, 6, 12));
        return b;
    }

    private void populateFormFromTable() {
        int r = table.getSelectedRow();
        if (r < 0) return;
        idField.setText(stringAt(r,0));
        nameField.setText(stringAt(r,1));
        fatherField.setText(stringAt(r,2));
        dobField.setText(stringAt(r,3));
        computeAndSetAge();
        genderCombo.setSelectedItem(stringAt(r,4).isEmpty() ? "Male" : stringAt(r,4));
        phoneField.setText(stringAt(r,5));
        String cs = stringAt(r,6);
        if (!cs.isEmpty()) courseCombo.setSelectedItem(cs);
        emailField.setText(stringAt(r,7));
        addressArea.setText(stringAt(r,8));
        // set course/semester if detailed columns exist
        String course = "";
        String sem = "";
        try { course = stringAt(r,10); sem = stringAt(r,11); } catch (Exception ignored){}
        if (!course.isEmpty()) courseCombo.setSelectedItem(course);
        if (!sem.isEmpty()) semesterCombo.setSelectedItem(sem);
    }

    private String stringAt(int row, int col) {
        if (row < 0 || col < 0) return "";
        Object v = null;
        try { v = model.getValueAt(row, col); } catch (Exception e) { return ""; }
        return v == null ? "" : v.toString();
    }

    private void computeAndSetAge() {
        String dobText = dobField.getText().trim();
        if (dobText.isEmpty()) { ageField.setText(""); return; }
        try {
            LocalDate dob = LocalDate.parse(dobText, DOB_FMT);
            LocalDate now = LocalDate.now();
            if (dob.isAfter(now)) { ageField.setText(""); return; }
            int age = Period.between(dob, now).getYears();
            ageField.setText(String.valueOf(age));
        } catch (DateTimeParseException ex) {
            ageField.setText("");
        }
    }

    /** Insert a new student record and auto-create a student user (username/password = student id) */
    private void onInsert() {
        try {
            String sid = idField.getText().trim();
            if (sid.isEmpty()) { showError("Student ID is required."); return; }
            String name = nameField.getText().trim();
            String father = fatherField.getText().trim();
            String dob = dobField.getText().trim();
            String gender = (String) genderCombo.getSelectedItem();
            String phone = phoneField.getText().trim();
            String courseSem = courseCombo.getSelectedItem() + " - " + semesterCombo.getSelectedItem();
            String email = emailField.getText().trim();
            String address = addressArea.getText().trim();

            Database.insertStudent(sid, name, father, dob, gender, phone, courseSem, email, address);

            // Try to auto-create user with username = student id and password = student id
            try {
                if (sid != null && !sid.isEmpty()) {
                    if (!Database.userExists(sid)) {
                        Database.createUser(sid, sid, "student", sid);
                        loadAllStudents();
                        showInfo("Student added and user created.\nUsername & password: " + sid);
                    } else {
                        loadAllStudents();
                        showInfo("Student added. User already exists for ID: " + sid);
                    }
                } else {
                    loadAllStudents();
                    showInfo("Student added.");
                }
            } catch (Exception userEx) {
                // Don't fail the insert if user creation fails; just warn
                loadAllStudents();
                showInfo("Student added. (But auto-create user failed: " + userEx.getMessage() + ")");
            }

            clearForm();
        } catch (Exception ex) {
            showError("Insert failed: " + ex.getMessage());
        }
    }

    private void onUpdate() {
        try {
            String id = idField.getText().trim();
            if (id.isEmpty()) { showError("Student ID required for update."); return; }
            Database.updateStudent(id, nameField.getText().trim(), fatherField.getText().trim(),
                    phoneField.getText().trim(), dobField.getText().trim(), (String)genderCombo.getSelectedItem(),
                    emailField.getText().trim(), courseCombo.getSelectedItem() + " - " + semesterCombo.getSelectedItem(),
                    addressArea.getText().trim());
            loadAllStudents();
            showInfo("Student updated successfully!");
        } catch (Exception ex) {
            showError("Update failed: " + ex.getMessage());
        }
    }

    private void onDeleteSelected() {
        int sel = table.getSelectedRow();
        if (sel == -1) { showError("Select a table row to delete."); return; }
        String id = stringAt(sel, 0);
        int confirm = JOptionPane.showConfirmDialog(frame, "Delete student with ID: " + id + " ?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Database.deleteStudent(id);
                loadAllStudents();
                showInfo("Student deleted.");
            } catch (Exception ex) {
                showError("Delete failed: " + ex.getMessage());
            }
        }
    }

    void loadAllStudents() {
        try {
            Database.fetchAllData(model);
        } catch (Exception ex) {
            showError("Failed loading students: " + ex.getMessage());
        }
    }

    private void clearForm() {
        idField.setText("");
        nameField.setText("");
        fatherField.setText("");
        dobField.setText("");
        ageField.setText("");
        emailField.setText("");
        phoneField.setText("");
        addressArea.setText("");
        genderCombo.setSelectedIndex(0);
        courseCombo.setSelectedIndex(0);
        semesterCombo.setSelectedIndex(0);
        table.clearSelection();
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showInfo(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    // Simple document listener helper
    private static class SimpleDocListener implements DocumentListener {
        private final Runnable action;
        public SimpleDocListener(Runnable action) { this.action = action; }
        public void insertUpdate(DocumentEvent e) { action.run(); }
        public void removeUpdate(DocumentEvent e) { action.run(); }
        public void changedUpdate(DocumentEvent e) { action.run(); }
    }
}
