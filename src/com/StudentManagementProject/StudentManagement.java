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
 * StudentManagement.java (Modernized Version)
 * -------------------------------------------
 * A clean and modernized Student Management UI built with Java Swing.
 * This program connects to a SQLite database (via Database.java) to perform
 * CRUD operations (Create, Read, Update, Delete) for student data.
 *
 * Fields managed:
 *  id, name, gender, DOB, age, email, phone, address, father_name, course, semester
 *
 * NOTE: This class does NOT modify Database.java — it uses existing methods.
 */
public class StudentManagement {

    // --- Core Components ---
    private JFrame frame; // main window
    private JTextField idField, nameField, dobField, ageField, emailField, phoneField, fatherField, searchField;
    private JTextArea addressArea;
    private JComboBox<String> genderCombo, courseCombo, semesterCombo;
    private JTable table;
    private DefaultTableModel model;

    // --- Date Format ---
    private static final DateTimeFormatter DOB_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // --- Entry Point ---
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                // Apply Nimbus Look & Feel for a modern UI appearance
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception ignored) {}

            try {
                // Initialize database connection and launch UI
                Database.dbInit();
                StudentManagement window = new StudentManagement();
                window.frame.setVisible(true);
                window.loadAllStudents();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Startup failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    // --- Constructor ---
    public StudentManagement() {
        initialize();
    }

    // --- Initialize the UI ---
    private void initialize() {
        frame = new JFrame("Student Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 640);
        frame.setLocationRelativeTo(null);

        // SplitPane divides form (left) and table (right)
        JSplitPane split = new JSplitPane();
        split.setDividerLocation(420);
        frame.getContentPane().add(split, BorderLayout.CENTER);

        split.setLeftComponent(buildFormPanel());
        split.setRightComponent(buildTablePanel());
    }

    /**
     * Build the LEFT section — student input form.
     */
    private JPanel buildFormPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int y = 0; // row tracker

        // --- ID Field ---
        p.add(new JLabel("Student ID:"), gbcPos(gbc, 0, y));
        idField = new JTextField();
        p.add(idField, gbcPos(gbc, 1, y++));

        // --- Name ---
        p.add(new JLabel("Name:"), gbcPos(gbc, 0, y));
        nameField = new JTextField();
        p.add(nameField, gbcPos(gbc, 1, y++));

        // --- Father's Name ---
        p.add(new JLabel("Father's Name:"), gbcPos(gbc, 0, y));
        fatherField = new JTextField();
        p.add(fatherField, gbcPos(gbc, 1, y++));

        // --- Gender ---
        p.add(new JLabel("Gender:"), gbcPos(gbc, 0, y));
        genderCombo = new JComboBox<>(new String[]{"Male", "Female", "Other"});
        p.add(genderCombo, gbcPos(gbc, 1, y++));

        // --- DOB ---
        p.add(new JLabel("Date of Birth (YYYY-MM-DD):"), gbcPos(gbc, 0, y));
        dobField = new JTextField();
        dobField.setToolTipText("Example: 2003-05-21");
        p.add(dobField, gbcPos(gbc, 1, y++));

        // --- Age (auto-calculated) ---
        p.add(new JLabel("Age:"), gbcPos(gbc, 0, y));
        ageField = new JTextField();
        ageField.setEditable(false);
        p.add(ageField, gbcPos(gbc, 1, y++));

        // Automatically compute age when DOB changes
        dobField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) { computeAndSetAge(); }
        });
        dobField.getDocument().addDocumentListener(new SimpleDocListener(this::computeAndSetAge));

        // --- Email ---
        p.add(new JLabel("Email:"), gbcPos(gbc, 0, y));
        emailField = new JTextField();
        p.add(emailField, gbcPos(gbc, 1, y++));

        // --- Phone ---
        p.add(new JLabel("Phone:"), gbcPos(gbc, 0, y));
        phoneField = new JTextField();
        p.add(phoneField, gbcPos(gbc, 1, y++));

        // --- Course ---
        p.add(new JLabel("Course:"), gbcPos(gbc, 0, y));
        courseCombo = new JComboBox<>(new String[]{"B.Tech - CSE", "B.Tech - ECE", "B.Sc", "MCA", "M.Tech"});
        courseCombo.setEditable(true);
        p.add(courseCombo, gbcPos(gbc, 1, y++));

        // --- Semester ---
        p.add(new JLabel("Semester:"), gbcPos(gbc, 0, y));
        semesterCombo = new JComboBox<>(new String[]{"1", "2", "3", "4", "5", "6", "7", "8"});
        p.add(semesterCombo, gbcPos(gbc, 1, y++));

        // --- Address ---
        p.add(new JLabel("Address:"), gbcPos(gbc, 0, y));
        addressArea = new JTextArea(5, 20);
        addressArea.setLineWrap(true);
        addressArea.setWrapStyleWord(true);
        p.add(new JScrollPane(addressArea), gbcPos(gbc, 1, y++));

        // --- Action Buttons ---
        JPanel btnRow = new JPanel();
        JButton insertBtn = styledButton("Insert"), updateBtn = styledButton("Update"), deleteBtn = styledButton("Delete"), clearBtn = styledButton("Clear");
        insertBtn.addActionListener(e -> onInsert());
        updateBtn.addActionListener(e -> onUpdate());
        deleteBtn.addActionListener(e -> onDeleteSelected());
        clearBtn.addActionListener(e -> clearForm());
        btnRow.add(insertBtn); btnRow.add(updateBtn); btnRow.add(deleteBtn); btnRow.add(clearBtn);
        p.add(btnRow, gbcPos(gbc, 0, y, 2));

        return p;
    }

    /**
     * Build the RIGHT section — student table and search bar.
     */
    private JPanel buildTablePanel() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Search toolbar
        JPanel toolbar = new JPanel();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
        toolbar.add(new JLabel(" Search: "));
        searchField = new JTextField();
        toolbar.add(searchField);
        toolbar.add(Box.createRigidArea(new Dimension(8, 0)));
        JButton showAll = styledButton("Show All");
        toolbar.add(showAll);
        p.add(toolbar, BorderLayout.NORTH);

        // Table model and setup
        model = new DefaultTableModel(new String[]{"ID", "Name", "Father", "DOB", "Gender", "Phone", "Course", "Email", "Address"}, 0);
        table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        p.add(new JScrollPane(table), BorderLayout.CENTER);

        // Populate form when a table row is selected
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) populateFormFromTable();
        });

        // Search listener (live filtering)
        searchField.getDocument().addDocumentListener(new SimpleDocListener(this::searchStudents));
        showAll.addActionListener(e -> loadAllStudents());

        return p;
    }

    // --- Helper: GridBag Positioning ---
    private GridBagConstraints gbcPos(GridBagConstraints gbc, int x, int y) {
        gbc.gridx = x; gbc.gridy = y; gbc.gridwidth = 1; gbc.weightx = (x == 1 ? 1.0 : 0.0); gbc.fill = GridBagConstraints.HORIZONTAL;
        return gbc;
    }
    private GridBagConstraints gbcPos(GridBagConstraints gbc, int x, int y, int width) {
        gbc.gridx = x; gbc.gridy = y; gbc.gridwidth = width; gbc.weightx = 1.0; gbc.anchor = GridBagConstraints.CENTER;
        return gbc;
    }

    // --- Button Styling ---
    private JButton styledButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setMargin(new Insets(6, 12, 6, 12));
        return b;
    }

    // --- Core Logic Methods ---

    /** Compute and set age based on DOB */
    private void computeAndSetAge() {
        String dobText = dobField.getText().trim();
        if (dobText.isEmpty()) { ageField.setText(""); return; }
        try {
            LocalDate dob = LocalDate.parse(dobText, DOB_FMT);
            int age = Period.between(dob, LocalDate.now()).getYears();
            ageField.setText(String.valueOf(age));
        } catch (DateTimeParseException ex) {
            ageField.setText(""); // clear if invalid format
        }
    }

    /** Populate form fields when a row is selected in the table */
    private void populateFormFromTable() {
        int r = table.getSelectedRow();
        if (r < 0) return;
        idField.setText(model.getValueAt(r, 0).toString());
        nameField.setText(model.getValueAt(r, 1).toString());
        fatherField.setText(model.getValueAt(r, 2).toString());
        dobField.setText(model.getValueAt(r, 3).toString());
        computeAndSetAge();
        genderCombo.setSelectedItem(model.getValueAt(r, 4));
        phoneField.setText(model.getValueAt(r, 5).toString());
        courseCombo.setSelectedItem(model.getValueAt(r, 6));
        emailField.setText(model.getValueAt(r, 7).toString());
        addressArea.setText(model.getValueAt(r, 8).toString());
    }

    /** Insert a new student record */
    private void onInsert() {
        try {
            Database.insertStudent(idField.getText(), nameField.getText(), fatherField.getText(), dobField.getText(),
                    (String) genderCombo.getSelectedItem(), phoneField.getText(),
                    courseCombo.getSelectedItem() + " - " + semesterCombo.getSelectedItem(),
                    emailField.getText(), addressArea.getText());
            loadAllStudents();
            showInfo("Student added successfully!");
            clearForm();
        } catch (Exception ex) { showError("Insert failed: " + ex.getMessage()); }
    }

    /** Update existing student record */
    private void onUpdate() {
        try {
            Database.updateStudent(idField.getText(), nameField.getText(), fatherField.getText(), phoneField.getText(),
                    dobField.getText(), (String) genderCombo.getSelectedItem(), emailField.getText(),
                    courseCombo.getSelectedItem() + " - " + semesterCombo.getSelectedItem(), addressArea.getText());
            loadAllStudents();
            showInfo("Student updated successfully!");
        } catch (Exception ex) { showError("Update failed: " + ex.getMessage()); }
    }

    /** Delete selected student record */
    private void onDeleteSelected() {
        int sel = table.getSelectedRow();
        if (sel == -1) { showError("Please select a row to delete."); return; }
        String id = model.getValueAt(sel, 0).toString();
        int confirm = JOptionPane.showConfirmDialog(frame, "Delete student with ID: " + id + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try { Database.deleteStudent(id); loadAllStudents(); showInfo("Deleted successfully!"); }
            catch (Exception ex) { showError("Delete failed: " + ex.getMessage()); }
        }
    }

    /** Load all students from DB into table */
    private void loadAllStudents() {
        try { Database.fetchAllData(model); }
        catch (Exception ex) { showError("Failed to load data: " + ex.getMessage()); }
    }

    /** Search students based on query */
    private void searchStudents() {
        String q = searchField.getText().trim();
        if (q.isEmpty()) return;
        try { Database.searchStudents(model, q); }
        catch (Exception ex) { showError("Search failed: " + ex.getMessage()); }
    }

    /** Clear all form fields */
    private void clearForm() {
        idField.setText(""); nameField.setText(""); fatherField.setText(""); dobField.setText(""); ageField.setText("");
        emailField.setText(""); phoneField.setText(""); addressArea.setText("");
        genderCombo.setSelectedIndex(0); courseCombo.setSelectedIndex(0); semesterCombo.setSelectedIndex(0);
        table.clearSelection();
    }

    // --- Utility Dialogs ---
    private void showError(String msg) { JOptionPane.showMessageDialog(frame, msg, "Error", JOptionPane.ERROR_MESSAGE); }
    private void showInfo(String msg) { JOptionPane.showMessageDialog(frame, msg, "Info", JOptionPane.INFORMATION_MESSAGE); }

    // --- Inner Helper Class: SimpleDocListener for concise document event handling ---
    private static class SimpleDocListener implements DocumentListener {
        private final Runnable action;
        public SimpleDocListener(Runnable a) { this.action = a; }
        public void insertUpdate(DocumentEvent e) { action.run(); }
        public void removeUpdate(DocumentEvent e) { action.run(); }
        public void changedUpdate(DocumentEvent e) { action.run(); }
    }
}
