package com.StudentManagementProject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Combined dialog file (login.java)
 *
 * Contains:
 *  - public class login { ... }  <-- wrapper (file must match this public class name)
 *  - class LoginDialog extends JDialog { ... }  <-- package-private, used by rest of project
 *  - class ChangePasswordDialog extends JDialog { ... } <-- package-private
 *
 * Usage from other classes remains unchanged:
 *   Database.AuthResult ar = LoginDialog.showLogin(parent);
 *   ChangePasswordDialog.showDialog(parent, username, isAdmin);
 *
 * Save this file as: src/com/StudentManagementProject/login.java
 */
public class login {
    private login() {}

    // convenience wrappers (optional) if you prefer calling login.showLogin(...)
    public static Database.AuthResult showLogin(Frame parent) {
        return LoginDialog.showLogin(parent);
    }

    public static void showChangePassword(Frame parent, String username, boolean isAdmin) {
        ChangePasswordDialog.showDialog(parent, username, isAdmin);
    }
}

/* -------------------------------------------------------------------------
   LoginDialog (package-private top-level class)
   Keeps the role radio buttons (Admin / Student) and returns Database.AuthResult
   ------------------------------------------------------------------------- */
class LoginDialog extends JDialog {

    private JTextField userField;
    private JPasswordField passField;
    private JButton loginBtn, cancelBtn;
    private Database.AuthResult result = null;

    // role selection
    private JRadioButton adminRadio;
    private JRadioButton studentRadio;

    LoginDialog(Frame parent) {
        super(parent, "Login", true);
        initUI();
        pack();
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        JPanel p = new JPanel(new BorderLayout(10,10));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));

        adminRadio = new JRadioButton("Admin");
        studentRadio = new JRadioButton("Student");
        adminRadio.setSelected(true);

        ButtonGroup g = new ButtonGroup();
        g.add(adminRadio);
        g.add(studentRadio);

        top.add(new JLabel("Login as:"));
        top.add(adminRadio);
        top.add(studentRadio);

        p.add(top, BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridLayout(2,2,8,8));
        fields.add(new JLabel("Username:"));
        userField = new JTextField(18);
        fields.add(userField);
        fields.add(new JLabel("Password:"));
        passField = new JPasswordField(18);
        fields.add(passField);

        p.add(fields, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        loginBtn = new JButton("Login");
        cancelBtn = new JButton("Cancel");
        buttons.add(loginBtn);
        buttons.add(cancelBtn);
        p.add(buttons, BorderLayout.SOUTH);

        setContentPane(p);

        // actions
        loginBtn.addActionListener(e -> doLogin());
        cancelBtn.addActionListener(e -> { result = null; setVisible(false); });

        // Enter key triggers login
        getRootPane().setDefaultButton(loginBtn);

        // Escape = cancel
        addKeyBindings();

        // small UX: focus username
        SwingUtilities.invokeLater(() -> userField.requestFocusInWindow());
    }

    private void addKeyBindings() {
        KeyStroke esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().registerKeyboardAction(e -> { result = null; setVisible(false); },
                esc, JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void doLogin() {
        String u = userField.getText().trim();
        String p = new String(passField.getPassword());
        String wantedRole = adminRadio.isSelected() ? "admin" : "student";

        if (u.isEmpty() || p.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter username and password.", "Missing", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Database.AuthResult ar = Database.authenticateUser(u, p);
            if (ar == null || !ar.ok) {
                JOptionPane.showMessageDialog(this, "Invalid username or password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (ar.role == null) {
                JOptionPane.showMessageDialog(this, "Authenticated but no role assigned. Contact admin.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!ar.role.equalsIgnoreCase(wantedRole)) {
                JOptionPane.showMessageDialog(this,
                        "This account is registered as '" + ar.role + "'.\nPlease select the correct role or contact admin.",
                        "Role Mismatch", JOptionPane.ERROR_MESSAGE);
                return;
            }

            this.result = ar;
            setVisible(false);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Login error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Shows the dialog and returns AuthResult (ok==true) or null if cancelled/failed.
     */
    public static Database.AuthResult showLogin(Frame parent) {
        final Database.AuthResult[] out = new Database.AuthResult[1];
        // show on EDT and block until closed (modal)
        try {
            if (SwingUtilities.isEventDispatchThread()) {
                LoginDialog dlg = new LoginDialog(parent);
                dlg.setVisible(true);
                out[0] = dlg.result;
            } else {
                SwingUtilities.invokeAndWait(() -> {
                    LoginDialog dlg = new LoginDialog(parent);
                    dlg.setVisible(true); // modal; blocks here until user closes dialog
                    out[0] = dlg.result;
                });
            }
        } catch (Exception e) {
            // fallback: try a simpler show
            LoginDialog dlg = new LoginDialog(parent);
            dlg.setVisible(true);
            out[0] = dlg.result;
        }
        return out[0];
    }
}

/* -------------------------------------------------------------------------
   ChangePasswordDialog (package-private top-level class)
   Robust version that ensures visibility / focus and supports admin resets.
   ------------------------------------------------------------------------- */
class ChangePasswordDialog extends JDialog {

    private final JTextField targetField;        // target username (editable for admin)
    private final JPasswordField oldField;       // old/current password (for own change)
    private final JPasswordField adminAuthField; // admin password for resetting others (visible when needed)
    private final JPasswordField newField;
    private final JPasswordField confirmField;
    private final JButton changeBtn;
    private final JButton cancelBtn;

    private final boolean isAdmin;
    private final String currentUsername;

    ChangePasswordDialog(Frame parent, String currentUsername, boolean isAdmin) {
        super(parent, "Change Password", true);
        this.isAdmin = isAdmin;
        this.currentUsername = currentUsername == null ? "" : currentUsername;
        targetField = new JTextField(24);
        oldField = new JPasswordField(24);
        adminAuthField = new JPasswordField(24);
        newField = new JPasswordField(24);
        confirmField = new JPasswordField(24);
        changeBtn = new JButton("Change");
        cancelBtn = new JButton("Cancel");
        initUI(parent);
    }

    private void initUI(Frame parent) {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        JPanel fields = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;

        // Username / target
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0;
        fields.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1.0;
        targetField.setText(currentUsername);
        if (!isAdmin) targetField.setEditable(false);
        targetField.setToolTipText("Username of account to change (students cannot edit this)");
        fields.add(targetField, gbc);

        // Current password (for own change)
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0;
        fields.add(new JLabel("Current password:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1.0;
        oldField.setToolTipText("Enter current password (required when changing own password)");
        fields.add(oldField, gbc);

        // Admin auth (only used when admin resets another user)
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0;
        fields.add(new JLabel("Admin password (for resetting):"), gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1.0;
        adminAuthField.setToolTipText("Admin must enter their password when resetting someone else's password");
        fields.add(adminAuthField, gbc);

        // New password
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0;
        fields.add(new JLabel("New password:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1.0;
        newField.setToolTipText("Enter the new password (min 4 chars recommended)");
        fields.add(newField, gbc);

        // Confirm
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0;
        fields.add(new JLabel("Confirm new password:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1.0;
        confirmField.setToolTipText("Re-enter the new password");
        fields.add(confirmField, gbc);

        p.add(fields, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.add(changeBtn);
        btns.add(cancelBtn);
        p.add(btns, BorderLayout.SOUTH);

        setContentPane(p);
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Default button and keyboard shortcuts
        getRootPane().setDefaultButton(changeBtn);
        KeyStroke esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().registerKeyboardAction(e -> dispose(), esc, JComponent.WHEN_IN_FOCUSED_WINDOW);

        // Action listeners
        changeBtn.addActionListener(e -> doChange());
        cancelBtn.addActionListener(e -> dispose());

        // Request focus on the username field after show
        SwingUtilities.invokeLater(() -> {
            if (targetField.isEditable()) targetField.requestFocusInWindow();
            else oldField.requestFocusInWindow();
        });

        if (!isAdmin) {
            adminAuthField.setEnabled(false);
            adminAuthField.setText("");
        }
    }

    private void doChange() {
        final String target = targetField.getText().trim();
        final String old = new String(oldField.getPassword());
        final String adminAuth = new String(adminAuthField.getPassword());
        final String np = new String(newField.getPassword());
        final String cp = new String(confirmField.getPassword());

        if (target.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter the username.", "Missing", JOptionPane.WARNING_MESSAGE);
            targetField.requestFocusInWindow();
            return;
        }
        if (np.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter the new password.", "Missing", JOptionPane.WARNING_MESSAGE);
            newField.requestFocusInWindow();
            return;
        }
        if (!np.equals(cp)) {
            JOptionPane.showMessageDialog(this, "New password and confirmation do not match.", "Mismatch", JOptionPane.WARNING_MESSAGE);
            newField.requestFocusInWindow();
            return;
        }
        if (np.length() < 1) {
            JOptionPane.showMessageDialog(this, "New password is too short.", "Weak password", JOptionPane.WARNING_MESSAGE);
            newField.requestFocusInWindow();
            return;
        }

        try {
            if (!isAdmin) {
                if (old.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Enter your current password.", "Missing", JOptionPane.WARNING_MESSAGE);
                    oldField.requestFocusInWindow();
                    return;
                }
                Database.changePassword(target, old, np);
                JOptionPane.showMessageDialog(this, "Password changed successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                dispose();
                return;
            } else {
                if (target.equalsIgnoreCase(currentUsername)) {
                    if (old.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Enter your current password.", "Missing", JOptionPane.WARNING_MESSAGE);
                        oldField.requestFocusInWindow();
                        return;
                    }
                    Database.changePassword(target, old, np);
                    JOptionPane.showMessageDialog(this, "Your password changed successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                    return;
                } else {
                    if (adminAuth.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Enter your admin password to authorize the reset.", "Missing", JOptionPane.WARNING_MESSAGE);
                        adminAuthField.requestFocusInWindow();
                        return;
                    }
                    Database.resetPassword(currentUsername, adminAuth, target, np);
                    JOptionPane.showMessageDialog(this, "Password for user '" + target + "' has been reset.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                    return;
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Password change failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Show the dialog in a safe way (on EDT) and ensure it appears on top.
     * Note: this method will schedule display on EDT and return quickly.
     */
    public static void showDialog(Frame parent, String currentUsername, boolean isAdmin) {
        SwingUtilities.invokeLater(() -> {
            System.out.println("[DEBUG] Opening ChangePasswordDialog for user=" + currentUsername + " isAdmin=" + isAdmin);
            ChangePasswordDialog dlg = new ChangePasswordDialog(parent, currentUsername, isAdmin);
            dlg.setAlwaysOnTop(true);
            dlg.setVisible(true); // modal; blocks until closed
            dlg.setAlwaysOnTop(false);
            System.out.println("[DEBUG] ChangePasswordDialog closed");
        });
    }
}
