package com.StudentManagementProject ;
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
 * Updated StudentManagement UI with fields:
 * id, name, gender, DOB, age, email, phone, address, father_name, course, semester
 *
 * Does NOT change Database.java — maps fields into existing Database methods.
 */
public class StudentManagement {

    private JFrame frame;
    private JTextField idField, nameField, dobField, ageField, emailField, phoneField, fatherField, searchField;
    private JTextArea addressArea;
    private JComboBox<String> genderCombo, courseCombo, semesterCombo;
    private JTable table;
    private DefaultTableModel model;

    // Date pattern used for DOB parsing/display. Keep consistent with how you store DOB in DB.
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
                Database.dbInit(); // keep Database.java untouched
                StudentManagement window = new StudentManagement();
                window.frame.setVisible(true);
                window.loadAllStudents();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Startup failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    public StudentManagement() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Student Management — Modern UI (fields: id,name,gender,DOB,age,email,phone,address,father_name,course,semester)");
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

        // Row helper
        JLabel lbl;
        gbc.gridx = 0; gbc.gridy = y; gbc.weightx = 0.0;
        lbl = new JLabel("Student ID:");
        p.add(lbl, gbc);
        idField = new JTextField();
        gbc.gridx = 1; gbc.gridy = y++; gbc.weightx = 1.0;
        p.add(idField, gbc);

        gbc.gridx = 0; gbc.gridy = y; gbc.weightx = 0.0;
        p.add(new JLabel("Name:"), gbc);
        nameField = new JTextField();
        gbc.gridx = 1; gbc.gridy = y++; gbc.weightx = 1.0;
        p.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = y;
        p.add(new JLabel("Father's Name:"), gbc);
        fatherField = new JTextField();
        gbc.gridx = 1; gbc.gridy = y++; 
        p.add(fatherField, gbc);

        gbc.gridx = 0; gbc.gridy = y;
        p.add(new JLabel("Gender:"), gbc);
        genderCombo = new JComboBox<>(new String[] {"Male","Female","Other"});
        gbc.gridx = 1; gbc.gridy = y++;
        p.add(genderCombo, gbc);

        gbc.gridx = 0; gbc.gridy = y;
        p.add(new JLabel("Date of Birth (YYYY-MM-DD):"), gbc);
        dobField = new JTextField();
        dobField.setToolTipText("Format: yyyy-MM-dd (e.g. 2003-05-21)");
        gbc.gridx = 1; gbc.gridy = y++;
        p.add(dobField, gbc);

        gbc.gridx = 0; gbc.gridy = y;
        p.add(new JLabel("Age:"), gbc);
        ageField = new JTextField();
        ageField.setEditable(false);
        gbc.gridx = 1; gbc.gridy = y++;
        p.add(ageField, gbc);

        // compute age when dob changes (on focus lost and on typing)
        dobField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) { computeAndSetAge(); }
        });
        dobField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { computeAndSetAge(); }
            public void removeUpdate(DocumentEvent e) { computeAndSetAge(); }
            public void changedUpdate(DocumentEvent e) { computeAndSetAge(); }
        });

        gbc.gridx = 0; gbc.gridy = y;
        p.add(new JLabel("Email:"), gbc);
        emailField = new JTextField();
        gbc.gridx = 1; gbc.gridy = y++;
        p.add(emailField, gbc);

        gbc.gridx = 0; gbc.gridy = y;
        p.add(new JLabel("Phone:"), gbc);
        phoneField = new JTextField();
        gbc.gridx = 1; gbc.gridy = y++;
        p.add(phoneField, gbc);

        gbc.gridx = 0; gbc.gridy = y;
        p.add(new JLabel("Course:"), gbc);
        // example course combo; you can change / add entries as needed
        courseCombo = new JComboBox<>(new String[] {"B.Tech - CSE","B.Tech - ECE","B.Sc","MCA","M.Tech"});
        courseCombo.setEditable(true);
        gbc.gridx = 1; gbc.gridy = y++;
        p.add(courseCombo, gbc);

        gbc.gridx = 0; gbc.gridy = y;
        p.add(new JLabel("Semester:"), gbc);
        semesterCombo = new JComboBox<>(new String[] {"1","2","3","4","5","6","7","8"});
        gbc.gridx = 1; gbc.gridy = y++;
        p.add(semesterCombo, gbc);

        gbc.gridx = 0; gbc.gridy = y; gbc.anchor = GridBagConstraints.NORTHWEST;
        p.add(new JLabel("Address:"), gbc);
        addressArea = new JTextArea(5, 20);
        addressArea.setLineWrap(true);
        addressArea.setWrapStyleWord(true);
        gbc.gridx = 1; gbc.gridy = y++; gbc.fill = GridBagConstraints.BOTH;
        p.add(new JScrollPane(addressArea), gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.WEST;

        // Buttons
        JPanel btnRow = new JPanel();
        JButton insertBtn = styledButton("Insert");
        JButton updateBtn = styledButton("Update");
        JButton deleteBtn = styledButton("Delete Selected");
        JButton clearBtn = styledButton("Clear");

        insertBtn.addActionListener(e -> onInsert());
        updateBtn.addActionListener(e -> onUpdate());
        deleteBtn.addActionListener(e -> onDeleteSelected());
        clearBtn.addActionListener(e -> clearForm());

        btnRow.add(insertBtn);
        btnRow.add(updateBtn);
        btnRow.add(deleteBtn);
        btnRow.add(clearBtn);

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
        // Do NOT hardcode columns that must match Database.fetchAllData.
        // But to give a readable default, set typical column headers (Database.fetchAllData
        // should overwrite/populate rows into this model).
        model.addColumn("Id");
        model.addColumn("Name");
        model.addColumn("Father");
        model.addColumn("DOB");
        model.addColumn("Gender");
        model.addColumn("Phone");
        model.addColumn("Course");
        model.addColumn("E-mail");
        model.addColumn("Address");

        table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        p.add(new JScrollPane(table), BorderLayout.CENTER);

        // Selection -> populate form
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                int r = table.getSelectedRow();
                idField.setText(stringAt(r,0));
                nameField.setText(stringAt(r,1));
                fatherField.setText(stringAt(r,2));
                dobField.setText(stringAt(r,3));
                // compute age from dob
                computeAndSetAge();
                genderCombo.setSelectedItem(stringAt(r,4).isEmpty() ? "Male" : stringAt(r,4));
                phoneField.setText(stringAt(r,5));
                String cs = stringAt(r,6);
                if (!cs.isEmpty()) {
                    // try split course/semester if saved as "Course - Sem X"
                    courseCombo.setSelectedItem(cs);
                }
                emailField.setText(stringAt(r,7));
                addressArea.setText(stringAt(r,8));
            }
        });

        // Search live
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { doSearch(); }
            public void removeUpdate(DocumentEvent e) { doSearch(); }
            public void changedUpdate(DocumentEvent e) { doSearch(); }
            private void doSearch() {
                String q = searchField.getText().trim();
                if (q.isEmpty()) {
                    // do nothing — user can press Show All
                } else {
                    try {
                        Database.searchStudents(model, q);
                    } catch (Exception ex) {
                        showError("Search failed: " + ex.getMessage());
                    }
                }
            }
        });

        showAll.addActionListener(e -> loadAllStudents());

        return p;
    }

    private JButton styledButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setMargin(new Insets(6, 12, 6, 12));
        return b;
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
            // Keep age blank if parsing fails — user can still insert; DB will validate if needed
            ageField.setText("");
        }
    }

    private void onInsert() {
        String id = idField.getText().trim();
        if (id.isEmpty()) { showError("Student ID required."); return; }
        try {
            String name = nameField.getText().trim();
            String father = fatherField.getText().trim();
            String dob = dobField.getText().trim();
            String gender = (String) genderCombo.getSelectedItem();
            String phone = phoneField.getText().trim();
            String course = courseCombo.getSelectedItem() == null ? "" : courseCombo.getSelectedItem().toString();
            String semester = semesterCombo.getSelectedItem() == null ? "" : semesterCombo.getSelectedItem().toString();
            String courseSem = course + " - " + semester;
            String email = emailField.getText().trim();
            String address = addressArea.getText().trim();

            // Map fields to Database.insertStudent(...) signature used by the original tutorial
            // Database.insertStudent(id,name,fatherName,dob,gender,contact,section,email,address)
            Database.insertStudent(id, name, father, dob, gender, phone, courseSem, email, address);

            loadAllStudents();
            showInfo("Inserted student.");
            clearForm();
        } catch (Exception ex) {
            showError("Insert failed: " + ex.getMessage());
        }
    }

    private void onUpdate() {
        String id = idField.getText().trim();
        if (id.isEmpty()) { showError("Student ID required for update."); return; }
        try {
            String name = nameField.getText().trim();
            String father = fatherField.getText().trim();
            String dob = dobField.getText().trim();
            String gender = (String) genderCombo.getSelectedItem();
            String phone = phoneField.getText().trim();
            String course = courseCombo.getSelectedItem() == null ? "" : courseCombo.getSelectedItem().toString();
            String semester = semesterCombo.getSelectedItem() == null ? "" : semesterCombo.getSelectedItem().toString();
            String courseSem = course + " - " + semester;
            String email = emailField.getText().trim();
            String address = addressArea.getText().trim();

            // NOTE: This ordering matches the Database.updateStudent(...) call expected earlier:
            // updateStudent(String id,String name,String fatherName,String contact,
            //               String dob,String gender,String email, String section,String address)
            Database.updateStudent(id, name, father, phone, dob, gender, email, courseSem, address);

            loadAllStudents();
            showInfo("Student updated.");
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

    private void loadAllStudents() {
        try {
            // Let Database.fetchAllData populate the model (it expects a DefaultTableModel)
            // If Database.fetchAllData assumes particular columns, it will fill them appropriately.
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
}
