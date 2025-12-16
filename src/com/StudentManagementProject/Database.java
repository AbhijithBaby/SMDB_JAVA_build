package com.StudentManagementProject;

import java.sql.*;
import javax.swing.table.DefaultTableModel;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/**
 * Database.java - consolidated and complete for StudentManagementProject
 *
 * - DB init & migration (students, users, edit_requests tables)
 * - User management: createUser, userExists, authenticateUser
 * - Password hashing (SHA-256)
 * - Password operations: changePassword, resetPassword
 * - Student CRUD: insertStudent (old/new), updateStudent (old/new), deleteStudent
 * - Fetch/search: fetchAllData, searchStudents (populate DefaultTableModel)
 * - Edit request workflow: createEditRequest, fetchAllEditRequests, approveEditRequest, rejectEditRequest
 *
 * IMPORTANT: package must be com.StudentManagementProject to match the rest of your project.
 */
public class Database {

    private static final String DB_URL = "jdbc:sqlite:student.db";
    private static final DateTimeFormatter DOB_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // --- Auth result holder
    public static class AuthResult {
        public final boolean ok;
        public final String username;
        public final String role;      // "admin" or "student"
        public final String studentId; // linked student id for role=student (may be null)

        public AuthResult(boolean ok, String username, String role, String studentId) {
            this.ok = ok;
            this.username = username;
            this.role = role;
            this.studentId = studentId;
        }
    }

    // ------------------------- CONNECTION HELPERS -------------------------
    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    // ------------------------- DB INITIALIZATION -------------------------
    /**
     * Initialize DB: create students table (with migration), users table and edit_requests.
     * Auto-creates a default admin user (admin/admin) if no users exist.
     */
    public static void dbInit() throws Exception {
        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {

            // Create students table
            String createStudents = "CREATE TABLE IF NOT EXISTS students ("
                    + "id TEXT PRIMARY KEY, "
                    + "name TEXT, "
                    + "father_name TEXT, "
                    + "dob TEXT, "
                    + "gender TEXT, "
                    + "age INTEGER, "
                    + "email TEXT, "
                    + "phone TEXT, "
                    + "address TEXT, "
                    + "course TEXT, "
                    + "semester TEXT"
                    + ");";
            st.execute(createStudents);

            // Ensure backward-compatible columns exist
            ensureColumnExists(conn, "students", "age", "INTEGER");
            ensureColumnExists(conn, "students", "course", "TEXT");
            ensureColumnExists(conn, "students", "semester", "TEXT");

            // Create users table
            String createUsers = "CREATE TABLE IF NOT EXISTS users ("
                    + "username TEXT PRIMARY KEY, "
                    + "password_hash TEXT NOT NULL, "
                    + "role TEXT NOT NULL, "   // 'admin' or 'student'
                    + "student_id TEXT"        // optional link to students.id
                    + ");";
            st.execute(createUsers);

            // Create edit_requests table
            String createRequests = "CREATE TABLE IF NOT EXISTS edit_requests ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "student_id TEXT NOT NULL, "
                    + "field TEXT NOT NULL, "
                    + "new_value TEXT, "
                    + "message TEXT, "
                    + "status TEXT NOT NULL, "           // OPEN / APPROVED / REJECTED
                    + "created_at TEXT, "
                    + "handled_by TEXT, "
                    + "handled_at TEXT, "
                    + "handled_reason TEXT"
                    + ");";
            st.execute(createRequests);

            // Create default admin if no users exist
            if (!hasAnyUser(conn)) {
                createUser(conn, "admin", "admin", "admin", null); // change password after first run!
            }

        } catch (SQLException ex) {
            throw new Exception("DB init failed: " + ex.getMessage(), ex);
        }
    }

    private static boolean hasAnyUser(Connection conn) throws SQLException {
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) AS c FROM users;")) {
            if (rs.next()) return rs.getInt("c") > 0;
            return false;
        }
    }

    /**
     * Adds a column to a table if it doesn't exist (used for migrations).
     */
    private static void ensureColumnExists(Connection conn, String table, String column, String type) throws SQLException {
        boolean exists = false;
        String qi = "PRAGMA table_info(" + table + ");";
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(qi)) {
            while (rs.next()) {
                String colName = rs.getString("name");
                if (colName != null && colName.equalsIgnoreCase(column)) {
                    exists = true;
                    break;
                }
            }
        }
        if (!exists) {
            String alter = "ALTER TABLE " + table + " ADD COLUMN " + column + " " + type + ";";
            try (Statement s2 = conn.createStatement()) {
                s2.execute(alter);
            }
        }
    }

    // ------------------------- USER MANAGEMENT -------------------------

    /**
     * Internal helper to create a user using an existing Connection.
     */
    private static void createUser(Connection conn, String username, String passwordPlain, String role, String studentId) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, role, student_id) VALUES (?, ?, ?, ?);";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hashPassword(passwordPlain));
            ps.setString(3, role);
            if (studentId == null) ps.setNull(4, Types.VARCHAR); else ps.setString(4, studentId);
            ps.executeUpdate();
        }
    }

    /**
     * Public method to create a user (admin or student).
     */
    public static void createUser(String username, String passwordPlain, String role, String studentId) throws Exception {
        try (Connection conn = getConnection()) {
            createUser(conn, username, passwordPlain, role, studentId);
        } catch (SQLException ex) {
            throw new Exception("Failed to create user: " + ex.getMessage(), ex);
        }
    }

    /**
     * Public helper: returns true if a user with username exists.
     */
    public static boolean userExists(String username) throws Exception {
        if (username == null) return false;
        String sql = "SELECT COUNT(*) AS c FROM users WHERE username = ?;";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("c") > 0;
                return false;
            }
        } catch (SQLException ex) {
            throw new Exception("userExists check failed: " + ex.getMessage(), ex);
        }
    }

    // ------------------------- AUTHENTICATION -------------------------

    /**
     * Authenticate a user; returns AuthResult with role and optional studentId.
     */
    public static AuthResult authenticateUser(String username, String passwordPlain) throws Exception {
        String sql = "SELECT username, password_hash, role, student_id FROM users WHERE username = ?;";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return new AuthResult(false, username, null, null);
                String storedHash = rs.getString("password_hash");
                String role = rs.getString("role");
                String studentId = rs.getString("student_id");
                String providedHash = hashPassword(passwordPlain);
                if (storedHash != null && storedHash.equals(providedHash)) {
                    return new AuthResult(true, username, role, studentId);
                } else {
                    return new AuthResult(false, username, null, null);
                }
            }
        } catch (SQLException ex) {
            throw new Exception("Authentication failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Simple SHA-256 hashing (hex) for passwords.
     * NOTE: For production use salted hashing (BCrypt/Argon2). This is OK for an academic desktop app.
     */
    private static String hashPassword(String plain) {
        if (plain == null) plain = "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(plain.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) {
            // fallback (should not happen)
            return Integer.toHexString(plain.hashCode());
        }
    }

    /**
     * Change own password (verify old password).
     */
    public static void changePassword(String username, String oldPassword, String newPassword) throws Exception {
        if (username == null || username.trim().isEmpty()) throw new Exception("Username required.");
        if (oldPassword == null || newPassword == null) throw new Exception("Passwords cannot be null.");
        // verify old password
        AuthResult ar = authenticateUser(username, oldPassword);
        if (ar == null || !ar.ok) {
            throw new Exception("Current password is incorrect.");
        }
        // update
        String sql = "UPDATE users SET password_hash = ? WHERE username = ?;";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hashPassword(newPassword));
            ps.setString(2, username);
            int updated = ps.executeUpdate();
            if (updated == 0) throw new Exception("Failed to update password (user not found).");
        } catch (SQLException ex) {
            throw new Exception("Failed to change password: " + ex.getMessage(), ex);
        }
    }

    /**
     * Admin resets another user's password.
     * Verifies adminUsername/adminPassword are valid and role == 'admin'.
     */
    public static void resetPassword(String adminUsername, String adminPassword, String targetUsername, String newPassword) throws Exception {
        if (adminUsername == null || adminPassword == null) throw new Exception("Admin credentials required.");
        if (targetUsername == null || targetUsername.trim().isEmpty()) throw new Exception("Target username required.");
        // authenticate admin
        AuthResult ar = authenticateUser(adminUsername, adminPassword);
        if (ar == null || !ar.ok || ar.role == null || !ar.role.equalsIgnoreCase("admin")) {
            throw new Exception("Admin authentication failed or not authorized.");
        }
        // update target
        String sql = "UPDATE users SET password_hash = ? WHERE username = ?;";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hashPassword(newPassword));
            ps.setString(2, targetUsername);
            int updated = ps.executeUpdate();
            if (updated == 0) throw new Exception("Target user not found: " + targetUsername);
        } catch (SQLException ex) {
            throw new Exception("Failed to reset password: " + ex.getMessage(), ex);
        }
    }

    // ------------------------- FETCH / SEARCH -------------------------
    /**
     * Populates the provided DefaultTableModel with all rows.
     * Columns: ID, Name, Father, DOB, Gender, Phone, Course/Sem, E-mail, Address, Age, Course, Semester
     */
    public static void fetchAllData(DefaultTableModel model) throws Exception {
        String sql = "SELECT id, name, father_name, dob, gender, phone, course, semester, email, address, age FROM students ORDER BY name COLLATE NOCASE;";
        model.setRowCount(0);
        model.setColumnIdentifiers(new Object[]{"ID", "Name", "Father", "DOB", "Gender", "Phone", "Course/Sem", "E-mail", "Address", "Age", "Course", "Semester"});

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String courseSem = joinCourseSemester(rs.getString("course"), rs.getString("semester"));
                model.addRow(new Object[]{
                        rs.getString("id"), rs.getString("name"), rs.getString("father_name"), rs.getString("dob"),
                        rs.getString("gender"), rs.getString("phone"), courseSem, rs.getString("email"),
                        rs.getString("address"), rs.getInt("age"), rs.getString("course"), rs.getString("semester")
                });
            }
        } catch (SQLException ex) {
            throw new Exception("Failed fetchAllData: " + ex.getMessage(), ex);
        }
    }

    /**
     * Search by id, name, father_name, course, semester, phone, email, address (LIKE %q%).
     * Populates same columns as fetchAllData.
     */
    public static void searchStudents(DefaultTableModel model, String q) throws Exception {
        String like = "%" + q + "%";
        String sql = "SELECT id, name, father_name, dob, gender, phone, course, semester, email, address, age FROM students "
                + "WHERE id LIKE ? OR name LIKE ? OR father_name LIKE ? OR course LIKE ? OR semester LIKE ? OR phone LIKE ? OR email LIKE ? OR address LIKE ? "
                + "ORDER BY name COLLATE NOCASE;";
        model.setRowCount(0);
        model.setColumnIdentifiers(new Object[]{"ID", "Name", "Father", "DOB", "Gender", "Phone", "Course/Sem", "E-mail", "Address", "Age", "Course", "Semester"});

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 1; i <= 8; i++) ps.setString(i, like);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String courseSem = joinCourseSemester(rs.getString("course"), rs.getString("semester"));
                    model.addRow(new Object[]{
                            rs.getString("id"), rs.getString("name"), rs.getString("father_name"), rs.getString("dob"),
                            rs.getString("gender"), rs.getString("phone"), courseSem, rs.getString("email"),
                            rs.getString("address"), rs.getInt("age"), rs.getString("course"), rs.getString("semester")
                    });
                }
            }
        } catch (SQLException ex) {
            throw new Exception("Search failed: " + ex.getMessage(), ex);
        }
    }

    // ------------------------- INSERT / UPDATE / DELETE -------------------------
    /**
     * Backward-compatible insert signature:
     * insertStudent(id,name,fatherName,dob,gender,contact,section,email,address)
     * where section was previously "Course - Semester"
     */
    public static void insertStudent(String id, String name, String fatherName,
                                     String dob, String gender, String contact,
                                     String section, String email, String address) throws Exception {
        String[] cs = parseSection(section);
        String course = cs[0];
        String semester = cs[1];
        Integer age = computeAgeFromDob(dob);

        insertStudent(id, name, fatherName, dob, age, email, contact, address, course, semester, gender);
    }

    /**
     * New/explicit insert (preferred).
     */
    public static void insertStudent(String id, String name, String fatherName,
                                     String dob, Integer age, String email, String phone,
                                     String address, String course, String semester, String gender) throws Exception {
        String sql = "INSERT INTO students (id, name, father_name, dob, gender, age, email, phone, address, course, semester) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            ps.setString(2, name);
            ps.setString(3, fatherName);
            ps.setString(4, dob);
            ps.setString(5, gender);
            if (age == null) ps.setNull(6, Types.INTEGER); else ps.setInt(6, age);
            ps.setString(7, email);
            ps.setString(8, phone);
            ps.setString(9, address);
            ps.setString(10, course);
            ps.setString(11, semester);

            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new Exception("Insert failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Backward-compatible update signature:
     * updateStudent(String id,String name,String fatherName,String contact,
     *               String dob,String gender,String email, String section,String address)
     */
    public static void updateStudent(String id, String name, String fatherName,
                                     String contact, String dob, String gender,
                                     String email, String section, String address) throws Exception {
        String[] cs = parseSection(section);
        String course = cs[0];
        String semester = cs[1];
        Integer age = computeAgeFromDob(dob);

        updateStudent(id, name, fatherName, contact, dob, gender, email, address, course, semester, age);
    }

    /**
     * New explicit update (preferred).
     */
    public static void updateStudent(String id, String name, String fatherName,
                                     String phone, String dob, String gender,
                                     String email, String address, String course,
                                     String semester, Integer age) throws Exception {
        String sql = "UPDATE students SET name = ?, father_name = ?, dob = ?, gender = ?, age = ?, email = ?, phone = ?, address = ?, course = ?, semester = ? WHERE id = ?;";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, fatherName);
            ps.setString(3, dob);
            ps.setString(4, gender);
            if (age == null) ps.setNull(5, Types.INTEGER); else ps.setInt(5, age);
            ps.setString(6, email);
            ps.setString(7, phone);
            ps.setString(8, address);
            ps.setString(9, course);
            ps.setString(10, semester);
            ps.setString(11, id);

            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new Exception("No student with id '" + id + "' found to update.");
            }
        } catch (SQLException ex) {
            throw new Exception("Update failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Delete by id.
     */
    public static void deleteStudent(String id) throws Exception {
        String sql = "DELETE FROM students WHERE id = ?;";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            int deleted = ps.executeUpdate();
            if (deleted == 0) {
                throw new Exception("No student with id '" + id + "' found to delete.");
            }
        } catch (SQLException ex) {
            throw new Exception("Delete failed: " + ex.getMessage(), ex);
        }
    }

    // ------------------------- EDIT REQUESTS WORKFLOW -------------------------

    /**
     * Create a new edit request (called by students)
     */
    public static void createEditRequest(String studentId, String field, String newValue, String message) throws Exception {
        if (studentId == null || studentId.trim().isEmpty()) throw new Exception("Student ID required.");
        if (field == null || field.trim().isEmpty()) throw new Exception("Field required.");
        // normalize field to allowed column
        String column = normalizeField(field);
        if (column == null) throw new Exception("Field not allowed: " + field);

        String sql = "INSERT INTO edit_requests (student_id, field, new_value, message, status, created_at) VALUES (?, ?, ?, ?, 'OPEN', datetime('now'));";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, column);
            ps.setString(3, newValue);
            ps.setString(4, message);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new Exception("Failed to create edit request: " + ex.getMessage(), ex);
        }
    }

    /**
     * Fetch all edit requests into a DefaultTableModel (for admin UI)
     */
    public static void fetchAllEditRequests(DefaultTableModel model) throws Exception {
        String sql = "SELECT id, student_id, field, new_value, message, status, created_at, handled_by, handled_at, handled_reason FROM edit_requests ORDER BY created_at DESC;";
        model.setRowCount(0);
        model.setColumnIdentifiers(new Object[] {"ID","Student ID","Field","New Value","Message","Status","Created At","Handled By","Handled At","Handled Reason"});
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                model.addRow(new Object[] {
                    rs.getInt("id"),
                    rs.getString("student_id"),
                    rs.getString("field"),
                    rs.getString("new_value"),
                    rs.getString("message"),
                    rs.getString("status"),
                    rs.getString("created_at"),
                    rs.getString("handled_by"),
                    rs.getString("handled_at"),
                    rs.getString("handled_reason")
                });
            }
        } catch (SQLException ex) {
            throw new Exception("Failed to fetch requests: " + ex.getMessage(), ex);
        }
    }

    /**
     * Approve a request: apply change to students table and mark request APPROVED
     */
    public static void approveEditRequest(int requestId, String adminUsername) throws Exception {
        if (adminUsername == null || adminUsername.trim().isEmpty()) throw new Exception("Admin required.");
        String select = "SELECT student_id, field, new_value FROM edit_requests WHERE id = ? AND status = 'OPEN';";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(select)) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new Exception("Request not found or not open.");
                String studentId = rs.getString("student_id");
                String field = rs.getString("field");
                String newValue = rs.getString("new_value");

                // validate field is allowed
                if (normalizeField(field) == null) throw new Exception("Invalid field in request.");

                // Apply update to students table
                if ("age".equalsIgnoreCase(field)) {
                    String upd = "UPDATE students SET age = ? WHERE id = ?;";
                    try (PreparedStatement u = conn.prepareStatement(upd)) {
                        try {
                            int age = Integer.parseInt(newValue);
                            u.setInt(1, age);
                        } catch (NumberFormatException nfe) {
                            throw new Exception("Invalid age value: " + newValue);
                        }
                        u.setString(2, studentId);
                        u.executeUpdate();
                    }
                } else if ("dob".equalsIgnoreCase(field)) {
                    String upd = "UPDATE students SET dob = ?, age = ? WHERE id = ?;";
                    try (PreparedStatement u = conn.prepareStatement(upd)) {
                        u.setString(1, newValue);
                        Integer age = computeAgeFromDob(newValue);
                        if (age == null) u.setNull(2, Types.INTEGER); else u.setInt(2, age);
                        u.setString(3, studentId);
                        u.executeUpdate();
                    }
                } else {
                    String upd = "UPDATE students SET " + field + " = ? WHERE id = ?;";
                    try (PreparedStatement u = conn.prepareStatement(upd)) {
                        u.setString(1, newValue);
                        u.setString(2, studentId);
                        u.executeUpdate();
                    }
                }

                // mark request approved
                String mark = "UPDATE edit_requests SET status = 'APPROVED', handled_by = ?, handled_at = datetime('now') WHERE id = ?;";
                try (PreparedStatement m = conn.prepareStatement(mark)) {
                    m.setString(1, adminUsername);
                    m.setInt(2, requestId);
                    m.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new Exception("Failed to approve request: " + ex.getMessage(), ex);
        }
    }

    /**
     * Reject a request: mark REJECTED with reason
     */
    public static void rejectEditRequest(int requestId, String adminUsername, String reason) throws Exception {
        if (adminUsername == null || adminUsername.trim().isEmpty()) throw new Exception("Admin required.");
        String sql = "UPDATE edit_requests SET status = 'REJECTED', handled_by = ?, handled_at = datetime('now'), handled_reason = ? WHERE id = ? AND status = 'OPEN';";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, adminUsername);
            ps.setString(2, reason);
            ps.setInt(3, requestId);
            int updated = ps.executeUpdate();
            if (updated == 0) throw new Exception("Request not found or not open.");
        } catch (SQLException ex) {
            throw new Exception("Failed to reject request: " + ex.getMessage(), ex);
        }
    }

    // Helper: allowed fields mapping (returns actual column name or null)
    private static String normalizeField(String field) {
        if (field == null) return null;
        String f = field.trim().toLowerCase();
        switch (f) {
            case "name": return "name";
            case "father_name": case "father": return "father_name";
            case "gender": return "gender";
            case "dob": return "dob";
            case "age": return "age";
            case "email": return "email";
            case "phone": return "phone";
            case "address": return "address";
            case "course": return "course";
            case "semester": return "semester";
            default: return null;
        }
    }

    // ------------------------- UTILITIES -------------------------
    private static String joinCourseSemester(String course, String semester) {
        if (course == null) course = "";
        if (semester == null) semester = "";
        course = course.trim();
        semester = semester.trim();
        if (course.isEmpty() && semester.isEmpty()) return "";
        if (course.isEmpty()) return semester;
        if (semester.isEmpty()) return course;
        return course + " - " + semester;
    }

    private static String[] parseSection(String section) {
        if (section == null) return new String[] {"", ""};
        String s = section.trim();
        if (s.contains(" - ")) {
            String[] parts = s.split(" - ", 2);
            return new String[] { parts[0].trim(), parts[1].trim() };
        } else if (s.toLowerCase().startsWith("sem") || s.toLowerCase().startsWith("semester")) {
            return new String[] { "", s };
        } else {
            return new String[] { s, "" };
        }
    }

    private static Integer computeAgeFromDob(String dobStr) {
        if (dobStr == null) return null;
        String s = dobStr.trim();
        if (s.isEmpty()) return null;
        try {
            LocalDate dob = LocalDate.parse(s, DOB_FMT);
            LocalDate now = LocalDate.now();
            if (dob.isAfter(now)) return null;
            return Period.between(dob, now).getYears();
        } catch (Exception ex) {
            return null;
        }
    }
}
