package com.StudentManagementProject;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;

/**
 * StudentView.java
 * Read-only UI for student users with Request Edit + My Requests features.
 *
 * Usage: new StudentView(auth).setVisible(true);
 */
public class StudentView extends JFrame {

    private final Database.AuthResult auth;
    private final String studentId; // id to load (from auth.studentId or auth.username)

    // UI components (read-only)
    private JTextField idField, nameField, fatherField, genderField, dobField, ageField, emailField, phoneField, courseField, semesterField;
    private JTextArea addressArea;

    public StudentView(Database.AuthResult auth) {
        super("Student Portal — " + (auth == null ? "Unknown" : auth.username));
        this.auth = auth;
        this.studentId = (auth != null && auth.studentId != null && !auth.studentId.trim().isEmpty())
                ? auth.studentId
                : (auth == null ? "" : auth.username);

        initUI();
        loadStudent();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(820, 520);
        setLocationRelativeTo(null);
    }

    private void initUI() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,6,6,6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int y = 0;

        idField = makeReadonlyField(); addRow(p, gbc, y++, "Student ID:", idField);
        nameField = makeReadonlyField(); addRow(p, gbc, y++, "Name:", nameField);
        fatherField = makeReadonlyField(); addRow(p, gbc, y++, "Father's Name:", fatherField);
        genderField = makeReadonlyField(); addRow(p, gbc, y++, "Gender:", genderField);
        dobField = makeReadonlyField(); addRow(p, gbc, y++, "DOB (yyyy-MM-dd):", dobField);
        ageField = makeReadonlyField(); addRow(p, gbc, y++, "Age:", ageField);
        emailField = makeReadonlyField(); addRow(p, gbc, y++, "E-mail:", emailField);
        phoneField = makeReadonlyField(); addRow(p, gbc, y++, "Phone:", phoneField);
        courseField = makeReadonlyField(); addRow(p, gbc, y++, "Course:", courseField);
        semesterField = makeReadonlyField(); addRow(p, gbc, y++, "Semester:", semesterField);

        // Address (multi-line)
        JLabel addrLbl = new JLabel("Address:");
        gbc.gridx = 0; gbc.gridy = y; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        p.add(addrLbl, gbc);

        addressArea = new JTextArea(5, 36);
        addressArea.setEditable(false);
        addressArea.setLineWrap(true);
        addressArea.setWrapStyleWord(true);
        JScrollPane addrScroll = new JScrollPane(addressArea);
        gbc.gridx = 1; gbc.gridy = y++; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.BOTH;
        p.add(addrScroll, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Buttons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshBtn = new JButton("Refresh");
        JButton changePwdBtn = new JButton("Change Password");
        JButton requestEditBtn = new JButton("Request Edit");
        JButton myRequestsBtn = new JButton("My Requests"); // NEW
        JButton logoutBtn = new JButton("Logout");

        btns.add(refreshBtn);
        btns.add(changePwdBtn);
        btns.add(requestEditBtn);
        btns.add(myRequestsBtn);
        btns.add(logoutBtn);

        gbc.gridx = 0; gbc.gridy = y; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.EAST;
        p.add(btns, gbc);

        setContentPane(p);

        // actions
        refreshBtn.addActionListener(e -> loadStudent());
        changePwdBtn.addActionListener(e -> {
            if (auth == null) {
                JOptionPane.showMessageDialog(this, "Authentication context missing.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            ChangePasswordDialog.showDialog(this, auth.username, false);
        });
        requestEditBtn.addActionListener(e -> showRequestDialog());
        myRequestsBtn.addActionListener(e -> showMyRequestsDialog());
        logoutBtn.addActionListener(e -> doLogout());

        // Esc -> logout
        KeyStroke esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().registerKeyboardAction(e -> doLogout(), esc, JComponent.WHEN_IN_FOCUSED_WINDOW);

        // initial focus
        SwingUtilities.invokeLater(() -> {
            if (!idField.getText().isEmpty()) nameField.requestFocusInWindow();
            else idField.requestFocusInWindow();
        });
    }

    private JTextField makeReadonlyField() {
        JTextField t = new JTextField();
        t.setEditable(false);
        return t;
    }

    private void addRow(JPanel p, GridBagConstraints gbc, int row, String label, JComponent comp) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        p.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        p.add(comp, gbc);
    }

    /** Loads the student's record from DB and populates UI fields */
    private void loadStudent() {
        if (studentId == null || studentId.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No student id available for this account.", "Error", JOptionPane.ERROR_MESSAGE);
            clearFields();
            return;
        }

        try {
            DefaultTableModel tmp = new DefaultTableModel();
            Database.searchStudents(tmp, studentId);
            if (tmp.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "No record found for student id: " + studentId, "Not found", JOptionPane.INFORMATION_MESSAGE);
                clearFields();
                return;
            }

            // take first matching row
            Object id = tmp.getValueAt(0, 0);
            Object name = tmp.getValueAt(0, 1);
            Object father = tmp.getValueAt(0, 2);
            Object dob = tmp.getValueAt(0, 3);
            Object gender = tmp.getValueAt(0, 4);
            Object phone = tmp.getValueAt(0, 5);
            Object courseSem = tmp.getValueAt(0, 6);
            Object email = tmp.getValueAt(0, 7);
            Object address = tmp.getValueAt(0, 8);
            Object age = tmp.getValueAt(0, 9);
            Object course = tmp.getValueAt(0, 10);
            Object semester = tmp.getValueAt(0, 11);

            idField.setText(id == null ? "" : id.toString());
            nameField.setText(name == null ? "" : name.toString());
            fatherField.setText(father == null ? "" : father.toString());
            dobField.setText(dob == null ? "" : dob.toString());
            genderField.setText(gender == null ? "" : gender.toString());
            phoneField.setText(phone == null ? "" : phone.toString());
            emailField.setText(email == null ? "" : email.toString());
            addressArea.setText(address == null ? "" : address.toString());
            ageField.setText(age == null ? "" : age.toString());

            // prefer explicit course/semester columns, fallback to Course/Sem column
            if (course != null && !course.toString().trim().isEmpty()) {
                courseField.setText(course.toString());
            } else if (courseSem != null) {
                courseField.setText(splitCourse(courseSem.toString())[0]);
            } else {
                courseField.setText("");
            }

            if (semester != null && !semester.toString().trim().isEmpty()) {
                semesterField.setText(semester.toString());
            } else if (courseSem != null) {
                semesterField.setText(splitCourse(courseSem.toString())[1]);
            } else {
                semesterField.setText("");
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to load your record:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private String[] splitCourse(String cs) {
        if (cs == null) return new String[] {"",""};
        if (cs.contains(" - ")) {
            String[] parts = cs.split(" - ", 2);
            return new String[] { parts[0].trim(), parts[1].trim() };
        } else {
            return new String[] { cs.trim(), "" };
        }
    }

    private void clearFields() {
        idField.setText(""); nameField.setText(""); fatherField.setText(""); dobField.setText(""); ageField.setText("");
        genderField.setText(""); phoneField.setText(""); courseField.setText(""); semesterField.setText(""); emailField.setText(""); addressArea.setText("");
    }

    /** Logout handler: close this window and show login again (then open appropriate UI) */
    private void doLogout() {
        dispose();
        Database.AuthResult ar = LoginDialog.showLogin(null);
        if (ar == null || !ar.ok) {
            System.exit(0);
            return;
        }
        if ("admin".equalsIgnoreCase(ar.role)) {
            SwingUtilities.invokeLater(() -> {
                try {
                    StudentManagement admin = new StudentManagement(ar);
                    admin.frame.setVisible(true);
                    admin.loadAllStudents();
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Failed to open admin UI: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        } else {
            SwingUtilities.invokeLater(() -> {
                StudentView sv = new StudentView(ar);
                sv.setVisible(true);
            });
        }
    }

    // -------------------- Request Edit Flow --------------------

    /** Show the Request Edit dialog (student chooses field, new value, message) */
    private void showRequestDialog() {
        if (auth == null) {
            JOptionPane.showMessageDialog(this, "Authentication missing.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String sid = studentId;
        if (sid == null || sid.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Student ID not available for your account.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String[] fields = new String[] {"Name","Father","Gender","DOB","Age","Email","Phone","Address","Course","Semester"};
        JComboBox<String> fieldCombo = new JComboBox<>(fields);
        JTextField newValueField = new JTextField(30);
        JTextArea msgArea = new JTextArea(5, 30);
        msgArea.setLineWrap(true);
        msgArea.setWrapStyleWord(true);

        JPanel panel = new JPanel(new BorderLayout(6,6));
        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,6,6,6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0; top.add(new JLabel("Field to change:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; top.add(fieldCombo, gbc);
        gbc.gridx = 0; gbc.gridy = 1; top.add(new JLabel("New value:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; top.add(newValueField, gbc);
        panel.add(top, BorderLayout.NORTH);

        JPanel msgP = new JPanel(new BorderLayout());
        msgP.add(new JLabel("Message (explain reason):"), BorderLayout.NORTH);
        msgP.add(new JScrollPane(msgArea), BorderLayout.CENTER);
        panel.add(msgP, BorderLayout.CENTER);

        int res = JOptionPane.showConfirmDialog(this, panel, "Request Edit", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;

        String field = (String) fieldCombo.getSelectedItem();
        String newVal = newValueField.getText().trim();
        String msg = msgArea.getText().trim();
        if (newVal.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter the new value.", "Missing", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Database.createEditRequest(sid, field, newVal, msg);
            JOptionPane.showMessageDialog(this, "Request submitted. Admin will review it.", "Requested", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to submit request: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Show a dialog listing this student's edit requests (filtered from fetchAllEditRequests) */
    private void showMyRequestsDialog() {
        if (auth == null) {
            JOptionPane.showMessageDialog(this, "Authentication missing.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String sid = studentId;
        if (sid == null || sid.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Student ID not available for your account.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Fetch all requests and filter for this student
        DefaultTableModel all = new DefaultTableModel();
        try {
            Database.fetchAllEditRequests(all);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to load requests: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Build filtered model
        DefaultTableModel model = new DefaultTableModel();
        model.setColumnIdentifiers(new Object[] {"ID","Field","New Value","Message","Status","Created At","Handled By","Handled At","Handled Reason"});
        for (int r = 0; r < all.getRowCount(); r++) {
            Object studentCol = all.getValueAt(r, 1);
            String sidRow = studentCol == null ? "" : studentCol.toString();
            if (!sid.equalsIgnoreCase(sidRow)) continue;
            Object id = all.getValueAt(r, 0);
            Object field = all.getValueAt(r, 2);
            Object newValue = all.getValueAt(r, 3);
            Object message = all.getValueAt(r, 4);
            Object status = all.getValueAt(r, 5);
            Object createdAt = all.getValueAt(r, 6);
            Object handledBy = all.getValueAt(r, 7);
            Object handledAt = all.getValueAt(r, 8);
            Object handledReason = all.getValueAt(r, 9);

            model.addRow(new Object[] { id, field, newValue, message, status, createdAt, handledBy, handledAt, handledReason });
        }

        JTable t = new JTable(model);
        t.setFillsViewportHeight(true);
        t.setAutoCreateRowSorter(true);
        JScrollPane sp = new JScrollPane(t);
        sp.setPreferredSize(new Dimension(900, 300));

        JOptionPane.showMessageDialog(this, sp, "My Edit Requests (" + sid + ")", JOptionPane.PLAIN_MESSAGE);
    }

    // main for standalone testing (optional)
    public static void main(String[] args) {
        try { Database.dbInit(); } catch (Exception e) { e.printStackTrace(); }
        Database.AuthResult ar = new Database.AuthResult(true, "S101", "student", "S101");
        SwingUtilities.invokeLater(() -> {
            StudentView v = new StudentView(ar);
            v.setVisible(true);
        });
    }
}
