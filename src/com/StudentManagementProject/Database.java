package com.StudentManagementProject ;
import java.sql.*;
import javax.swing.table.DefaultTableModel;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

/**
 * Modified Database.java
 * - New columns: age, course, semester
 * - Migration: adds missing columns (ALTER TABLE ...) if older DB lacks them
 * - Keeps old insert/update signatures for backward compatibility
 * - Adds overloaded insert/update methods with explicit course/semester/age
 *
 * Ensure sqlite-jdbc is added to project libraries.
 */
public class Database {

    private static final String DB_URL = "jdbc:sqlite:student.db";
    private static final DateTimeFormatter DOB_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // --- CONNECTION HELPERS ------------------------------------------------
    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void dbInit() throws Exception {
        // Create table if missing, and attempt to migrate by adding new columns if required.
        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {

            // Create main table if not exists (with new schema)
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
                    + "semester TEXT"
                    + ");";
            st.execute(create);

            // Ensure backward-compatible columns exist: check and add if missing
            ensureColumnExists(conn, "students", "age", "INTEGER");
            ensureColumnExists(conn, "students", "course", "TEXT");
            ensureColumnExists(conn, "students", "semester", "TEXT");

        } catch (SQLException ex) {
            throw new Exception("DB init failed: " + ex.getMessage(), ex);
        }
    }

    private static void ensureColumnExists(Connection conn, String table, String column, String type) throws SQLException {
        // Check PRAGMA table_info and add column if missing
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

    // --- FETCH / SEARCH ---------------------------------------------------
    /**
     * Populates the provided DefaultTableModel with all rows.
     * The columns set here are backward-compatible with older UI:
     * [Id, Name, Father, DOB, Gender, Phone, Course/Sem, E-mail, Address, Age, Course, Semester]
     */
    public static void fetchAllData(DefaultTableModel model) throws Exception {
        String sql = "SELECT id, name, father_name, dob, gender, phone, course, semester, email, address, age FROM students ORDER BY name COLLATE NOCASE;";
        model.setRowCount(0);
        Object[] cols = new Object[] {
                "Id","Name","Father","DOB","Gender","Phone","Course/Sem","E-mail","Address","Age","Course","Semester"
        };
        model.setColumnIdentifiers(cols);

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String id = rs.getString("id");
                String name = rs.getString("name");
                String father = rs.getString("father_name");
                String dob = rs.getString("dob");
                String gender = rs.getString("gender");
                String phone = rs.getString("phone");
                String course = rs.getString("course");
                String semester = rs.getString("semester");
                String email = rs.getString("email");
                String address = rs.getString("address");
                int age = rs.getInt("age");

                String courseSem = joinCourseSemester(course, semester);

                Object[] row = new Object[] {
                        id, name, father, dob, gender, phone, courseSem, email, address, age, course, semester
                };
                model.addRow(row);
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
        Object[] cols = new Object[] {
                "Id","Name","Father","DOB","Gender","Phone","Course/Sem","E-mail","Address","Age","Course","Semester"
        };
        model.setColumnIdentifiers(cols);

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 1; i <= 8; i++) ps.setString(i, like);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String name = rs.getString("name");
                    String father = rs.getString("father_name");
                    String dob = rs.getString("dob");
                    String gender = rs.getString("gender");
                    String phone = rs.getString("phone");
                    String course = rs.getString("course");
                    String semester = rs.getString("semester");
                    String email = rs.getString("email");
                    String address = rs.getString("address");
                    int age = rs.getInt("age");

                    String courseSem = joinCourseSemester(course, semester);

                    Object[] row = new Object[] {
                            id, name, father, dob, gender, phone, courseSem, email, address, age, course, semester
                    };
                    model.addRow(row);
                }
            }
        } catch (SQLException ex) {
            throw new Exception("Search failed: " + ex.getMessage(), ex);
        }
    }

    // --- INSERT (old signature kept) --------------------------------------
    /**
     * Backward-compatible insert:
     * insertStudent(id,name,fatherName,dob,gender,contact,section,email,address)
     * where section previously was "Course - Semester" (or just course).
     *
     * This will parse section into course & semester and compute age from DOB.
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
     * New insert with explicit fields (preferred).
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

    // --- UPDATE (old signature kept) --------------------------------------
    /**
     * Backward-compatible update:
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
     * New update with explicit fields (preferred).
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

    // --- DELETE -----------------------------------------------------------
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

    // --- UTIL -------------------------------------------------------------
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
            // if it's only a semester like "Sem 3", leave course empty
            return new String[] { "", s };
        } else {
            // treat entire section as course
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
            // parsing failed -> return null (age unknown)
            return null;
        }
    }
}
