# Student Management System (Java + SQLite)

A modern, lightweight **Student Management System** built in **Java Swing** using **SQLite** as the local database.  
It provides an easy-to-use interface to manage student records — add, edit, delete, and search — all within a clean and responsive UI.

---

## Features

- Add, update, delete, and search student records  
- Modern and responsive Swing-based UI  
- SQLite integration (no external server needed)  
- Auto-calculates **Age** from **Date of Birth (DOB)**  
- Clean separation of UI and database logic (`StudentManagement.java` + `Database.java`)  
- Works offline — perfect for school or college projects  

---

## Database Structure

**Table:** `students`

| Column Name  | Type    | Description                     |
|---------------|----------|---------------------------------|
| id            | TEXT (PK) | Unique student ID               |
| name          | TEXT     | Student full name               |
| gender        | TEXT     | Gender (Male/Female/Other)      |
| dob           | TEXT     | Date of Birth (YYYY-MM-DD)      |
| age           | INTEGER  | Auto-calculated from DOB        |
| email         | TEXT     | Email address                   |
| phone         | TEXT     | Contact number                  |
| address       | TEXT     | Permanent address               |
| father_name   | TEXT     | Father’s full name              |
| course        | TEXT     | Course enrolled (e.g., B.Tech)  |
| semester      | TEXT     | Current semester (e.g., Sem 3)  |

---

## Technologies Used

| Category | Tools |
|-----------|-------|
| Programming Language | **Java (JDK 8 or higher)** |
| GUI Framework | **Swing** |
| Database | **SQLite** |
| IDE | **NetBeans** (recommended) |
| JDBC Driver | `sqlite-jdbc` |

---

## Setup Instructions

### 1. Clone the Repository
```bash
git clone https://github.com/yourusername/student-management-system-java.git
cd student-management-system-java
```

### 2. Open in NetBeans
- Open **NetBeans**
- Go to **File → Open Project**
- Select this folder  
- Wait for dependencies to load

### 3. Add SQLite JDBC Driver
1. Download the latest **sqlite-jdbc** `.jar` file from [https://github.com/xerial/sqlite-jdbc](https://github.com/xerial/sqlite-jdbc)  
2. In NetBeans → Right-click your project → **Properties → Libraries → Add JAR/Folder**  
3. Add the `.jar` file you downloaded  

### 4. Run the Project
- Right-click the project → **Run** (or press F6)  
- The database file `student.db` will be automatically created in your project folder  

---

## Screenshots (Example Placeholders)

| Main Dashboard | Add Student Form |
|----------------|------------------|
| ![Main UI](docs/screenshots/dashboard.png) | ![Add Form](docs/screenshots/add_student.png) |

---

## How It Works

1. On startup, the app checks for `student.db` and creates it if not found.  
2. `Database.java` handles all SQL operations (Create, Read, Update, Delete).  
3. The `StudentManagement.java` UI calls the database functions and updates the JTable dynamically.  
4. Age is automatically computed from DOB each time a record is added or updated.

---

## Project Structure

```
src/
 └── com/
      └── TechVidvan/
          ├── Database.java          # Handles SQLite operations
          └── StudentManagement.java # GUI + logic
```

---

## Author

**Abhijith Baby**  
B.Tech Computer Science (KTU)  
📫 [your.email@example.com]  
💻 Passionate about Java, Databases, and Modern UI Design  

---

## Future Improvements

- Add login system (Admin / Teacher)  
- Export student data to Excel or PDF  
- Use modern JavaFX UI with themes  
- Add cloud database option (MySQL)  

---

## License

This project is licensed under the **MIT License** — you’re free to use and modify it with attribution.
* **Add New Students:** Quickly input and save new student records (e.g., name, roll number, course, contact info).
* **View All Students:** Retrieve and display a list of all students currently in the database.
* **Search/Retrieve:** Find specific student records using key identifiers like a Roll Number.
* **Update Information:** Modify existing student details (e.g., correcting a phone number).
* **Delete Records:** Permanently remove a student's record from the database.

---

## 📸 Application Screenshot

Here is a view of the application interface:

![Student Management System GUI](Screenshot_2025-10-18_12-57-39.png)

---
## 🛠️ Technology Stack

* **Language:** Java
* **IDE (Inferred):** NetBeans, yep y not 🙃.
* **Database:**  **SQLite**
  
---

## 🚀 Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

* **Java Development Kit (JDK):** Version 8 or newer.
* **IDE:** An Integrated Development Environment like NetBeans or IntelliJ IDEA that supports Java projects.

### Installation

1.  **Clone the Repository:**
    ```bash
    git clone [https://github.com/AbhijithBaby/SMDB_JAVA_build.git](https://github.com/AbhijithBaby/SMDB_JAVA_build.git)
    ```
2.  **Open in IDE:**
    * Open your IDE (e.g., NetBeans).
    * Select **File** -> **Open Project...**
    * Navigate to the cloned directory (`SMDB_JAVA_build`) and open the project.
3.  **Run the Application:**
    * Ensure all necessary database libraries (like the SQLite JDBC driver) are included in the project's build path (if not already handled by the project configuration).
    * Locate the main class (likely inside `src/com/StudentManagementProject`) and run the project.

---

## 💡 Usage

The application is typically run as a standalone desktop application.

1.  Upon startup, a main window or console interface will appear.
2.  The interface will present options such as "Add," "View," "Search," and "Delete."
3.  Follow the on-screen prompts or buttons to perform the desired database operations.

***Note:*** *The database files (`StudentManagementDB.db` or `student.db`) should be present in the project root to ensure the application connects correctly.*

---

## 🤝 Contributing

Contributions are welcome! If you have suggestions for improvement, feel free to:

1.  Fork the repository.
2.  Create your feature branch (`git checkout -b feature/AmazingFeature`).
3.  Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4.  Push to the branch (`git push origin feature/AmazingFeature`).
5.  Open a Pull Request.
6.  i check it or may not 🙂

---

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file
