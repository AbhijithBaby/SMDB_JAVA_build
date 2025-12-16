package com.StudentManagementProject;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * RequestsDialog — admin UI to review & process edit requests
 *
 * Usage:
 *   RequestsDialog.showForAdmin(parentFrame, adminUsername);
 */
public class RequestsDialog extends JDialog {

    private final JFrame parent;
    private final String adminUsername;
    private DefaultTableModel model;
    private JTable table;

    public RequestsDialog(JFrame parent, String adminUsername) {
        super(parent, "Edit Requests — Admin", true);
        this.parent = parent;
        this.adminUsername = adminUsername == null ? "" : adminUsername;
        initUI();
        loadRequests();
        setSize(940, 520);
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        model = new DefaultTableModel();
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);

        // Top toolbar buttons
        JButton refreshBtn = new JButton("Refresh");
        JButton approveBtn = new JButton("Approve");
        JButton rejectBtn = new JButton("Reject");
        JButton closeBtn = new JButton("Close");

        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        top.add(refreshBtn);
        top.add(approveBtn);
        top.add(rejectBtn);
        top.add(closeBtn);

        setLayout(new BorderLayout(8,8));
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Button actions
        refreshBtn.addActionListener(e -> loadRequests());
        approveBtn.addActionListener(e -> doApprove());
        rejectBtn.addActionListener(e -> doReject());
        closeBtn.addActionListener(e -> dispose());
    }

    private void loadRequests() {
        try {
            Database.fetchAllEditRequests(model);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to load requests: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doApprove() {
        int r = table.getSelectedRow();
        if (r == -1) {
            JOptionPane.showMessageDialog(this, "Select a request first.", "No selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // convert view row to model row (if sorter active)
        int modelRow = table.convertRowIndexToModel(r);
        Object idObj = model.getValueAt(modelRow, 0);
        if (idObj == null) { JOptionPane.showMessageDialog(this, "Invalid request id.", "Error", JOptionPane.ERROR_MESSAGE); return; }
        int id;
        try { id = Integer.parseInt(idObj.toString()); } catch (NumberFormatException nfe) { JOptionPane.showMessageDialog(this, "Invalid request id.", "Error", JOptionPane.ERROR_MESSAGE); return; }

        int conf = JOptionPane.showConfirmDialog(this, "Approve request ID: " + id + " ?\nThis will apply the requested change to the student's record.", "Confirm Approve", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;

        try {
            Database.approveEditRequest(id, adminUsername);
            JOptionPane.showMessageDialog(this, "Request approved and applied.", "Approved", JOptionPane.INFORMATION_MESSAGE);
            loadRequests();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Approve failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doReject() {
        int r = table.getSelectedRow();
        if (r == -1) {
            JOptionPane.showMessageDialog(this, "Select a request first.", "No selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(r);
        Object idObj = model.getValueAt(modelRow, 0);
        if (idObj == null) { JOptionPane.showMessageDialog(this, "Invalid request id.", "Error", JOptionPane.ERROR_MESSAGE); return; }
        int id;
        try { id = Integer.parseInt(idObj.toString()); } catch (NumberFormatException nfe) { JOptionPane.showMessageDialog(this, "Invalid request id.", "Error", JOptionPane.ERROR_MESSAGE); return; }

        String reason = JOptionPane.showInputDialog(this, "Enter rejection reason (optional):", "");
        if (reason == null) return; // cancelled

        int conf = JOptionPane.showConfirmDialog(this, "Reject request ID: " + id + " ?", "Confirm Reject", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;

        try {
            Database.rejectEditRequest(id, adminUsername, reason);
            JOptionPane.showMessageDialog(this, "Request rejected.", "Rejected", JOptionPane.INFORMATION_MESSAGE);
            loadRequests();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Reject failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Convenience static method used by StudentManagement (and reflection).
     */
    public static void showForAdmin(JFrame parent, String adminUsername) {
        RequestsDialog dlg = new RequestsDialog(parent, adminUsername);
        dlg.setVisible(true);
    }
}
