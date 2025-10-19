package com.StudentManagementProject;

import java.sql.*;
import javax.swing.table.DefaultTableModel;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

/**
 * Database.java — Handles all database operations for the Student Management System.
 *
 * This class is responsible for connecting to the SQLite database, creating the student table,
 * inserting, updating, deleting, and fetching student records.
 *
 * It also ensures backward compatibility with older database schemas by automatically
 * adding missing columns (age, course, semester).
 */
public class Database {

    // Path to the SQLite database file
    private static final String DB_URL = "jdbc:sqlite:student.db";

    // Date format used to calculate age from DOB
    private static final DateTimeFormatter DOB_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ------------------------- DATABASE CONNECTION -------------------------

    /**
     * Returns a new connection to the SQLite database.
     */
    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * Initializes the database.
     * Creates the students table if it does not exist and adds missing columns if necessary.
     */
    public static void dbInit() throws Exception {
        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {

            // Create the student table if not already present
            String create = "CREATE TABLE IF NOT EXISTS students ("
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
                    + "semester TEXT" + ");";
            st.execute(create);

            // Add missing columns if the table was created in an older version
            ensureColumnExists(conn, "students", "age", "INTEGER");
            ensureColumnExists(conn, "students", "course", "TEXT");
            ensureColumnExists(conn, "students", "semester", "TEXT");

        } catch (SQLException ex) {
            throw new Exception("Database initialization failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Adds a column to a table if it doesn't exist.
     */
    private static void ensureColumnExists(Connection conn, String table, String column, String type) throws SQLException {
        boolean exists = false;
        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery("PRAGMA table_info(" + table + ");")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    exists = true;
                    break;
                }
            }
        }
        if (!exists) {
            try (Statement s2 = conn.createStatement()) {
                s2.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type + ";");
            }
        }
    }

    // ------------------------- FETCH / SEARCH -------------------------

    /**
     * Fetches all student records and fills the JTable model.
     */
    public static void fetchAllData(DefaultTableModel model) throws Exception {
        String sql = "SELECT id, name, father_name, dob, gender, phone, course, semester, email, address, age FROM students ORDER BY name COLLATE NOCASE;";
        model.setRowCount(0);
        model.setColumnIdentifiers(new Object[]{"ID", "Name", "Father", "DOB", "Gender", "Phone", "Course/Sem", "E-mail", "Address", "Age", "Course", "Semester"});

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String courseSem = joinCourseSemester(rs.getString("course"), rs.getString("semester"));
                model.addRow(new Object[]{
                        rs.getString("id"), rs.getString("name"), rs.getString("father_name"), rs.getString("dob"),
                        rs.getString("gender"), rs.getString("phone"), courseSem, rs.getString("email"),
                        rs.getString("address"), rs.getInt("age"), rs.getString("course"), rs.getString("semester")
                });
            }
        } catch (SQLException ex) {
            throw new Exception("Failed to fetch data: " + ex.getMessage(), ex);
        }
    }

    /**
     * Searches student records based on a query (matches ID, name, course, etc.).
     */
    public static void searchStudents(DefaultTableModel model, String q) throws Exception {
        String like = "%" + q + "%";
        String sql = "SELECT * FROM students WHERE id LIKE ? OR name LIKE ? OR father_name LIKE ? OR course LIKE ? OR semester LIKE ? OR phone LIKE ? OR email LIKE ? OR address LIKE ? ORDER BY name COLLATE NOCASE;";
        model.setRowCount(0);
        model.setColumnIdentifiers(new Object[]{"ID", "Name", "Father", "DOB", "Gender", "Phone", "Course/Sem", "E-mail", "Address", "Age", "Course", "Semester"});

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
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

    // ------------------------- INSERT -------------------------

    /**
     * Inserts a student record (old compatible method).
     */
    public static void insertStudent(String id, String name, String fatherName, String dob, String gender, String contact, String section, String email, String address) throws Exception {
        String[] cs = parseSection(section);
        Integer age = computeAgeFromDob(dob);
        insertStudent(id, name, fatherName, dob, age, email, contact, address, cs[0], cs[1], gender);
    }

    /**
     * Inserts a student record (new preferred method).
     */
    public static void insertStudent(String id, String name, String fatherName, String dob, Integer age, String email, String phone, String address, String course, String semester, String gender) throws Exception {
        String sql = "INSERT INTO students (id, name, father_name, dob, gender, age, email, phone, address, course, semester) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
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

    // ------------------------- UPDATE -------------------------

    /**
     * Updates a student record (old compatible method).
     */
    public static void updateStudent(String id, String name, String fatherName, String contact, String dob, String gender, String email, String section, String address) throws Exception {
        String[] cs = parseSection(section);
        Integer age = computeAgeFromDob(dob);
        updateStudent(id, name, fatherName, contact, dob, gender, email, address, cs[0], cs[1], age);
    }

    /**
     * Updates a student record (new preferred method).
     */
    public static void updateStudent(String id, String name, String fatherName, String phone, String dob, String gender, String email, String address, String course, String semester, Integer age) throws Exception {
        String sql = "UPDATE students SET name=?, father_name=?, dob=?, gender=?, age=?, email=?, phone=?, address=?, course=?, semester=? WHERE id=?;";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
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
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new Exception("Update failed: " + ex.getMessage(), ex);
        }
    }

    // ------------------------- DELETE -------------------------

    /**
     * Deletes a student record using the given ID.
     */
    public static void deleteStudent(String id) throws Exception {
        String sql = "DELETE FROM students WHERE id=?;";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new Exception("Delete failed: " + ex.getMessage(), ex);
        }
    }

    // ------------------------- UTILITIES -------------------------

    /**
     * Joins course and semester into one string for display.
     */
    private static String joinCourseSemester(String course, String semester) {
        if (course == null) course = "";
        if (semester == null) semester = "";
        if (course.isEmpty()) return semester;
        if (semester.isEmpty()) return course;
        return course + " - " + semester;
    }

    /**
     * Parses a combined section string (like "CSE - 5") into course and semester.
     */
    private static String[] parseSection(String section) {
        if (section == null) return new String[]{"", ""};
        String s = section.trim();
        if (s.contains(" - ")) return s.split(" - ", 2);
        return new String[]{s, ""};
    }

    /**
     * Computes age based on Date of Birth.
     */
    private static Integer computeAgeFromDob(String dobStr) {
        if (dobStr == null || dobStr.trim().isEmpty()) return null;
        try {
            LocalDate dob = LocalDate.parse(dobStr, DOB_FMT);
            return Period.between(dob, LocalDate.now()).getYears();
        } catch (Exception e) {
            return null;
        }
    }
}
