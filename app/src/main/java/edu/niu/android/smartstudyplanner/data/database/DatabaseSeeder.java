package edu.niu.android.smartstudyplanner.data.database;

import android.content.Context;
import android.util.Log;

import java.util.Calendar;

import edu.niu.android.smartstudyplanner.data.entities.Course;
import edu.niu.android.smartstudyplanner.data.entities.Flashcard;
import edu.niu.android.smartstudyplanner.data.entities.StudySession;
import edu.niu.android.smartstudyplanner.data.entities.Task;

public class DatabaseSeeder {

    private static final String TAG = "DatabaseSeeder";

    /**
     * Seeds the database if it is empty. No op if courses already exist.
     * Any seeding error is logged and swallowed so it cannot crash startup.
     */
    public static void seedIfNeeded(Context context, StudyPlannerDatabase db) {
        try {
            if (db.courseDao().getCourseCount() > 0) {
                Log.d(TAG, "Database already has data, skipping seed");
                return;
            }
            Log.d(TAG, "Empty database, seeding now");
            seedCourses(db);
            seedTasks(db);
            seedStudySessions(db);
            seedFlashcards(db);
            Log.d(TAG, "Seeding complete");
        } catch (Exception e) {
            Log.e(TAG, "Seeding error", e);
        }
    }

    /**
     * Builds a millisecond timestamp from year, month (1 based), day, hour, minute.
     */
    private static long date(int year, int month, int day, int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, day, hour, minute, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * Inserts the Spring 2026 demo courses.
     */
    private static void seedCourses(StudyPlannerDatabase db) {
        long now = System.currentTimeMillis();

        db.courseDao().insert(new Course(
                "CSCI 524 - Adv Android Mobile Device Prog",
                "Prof. Smith",
                "#0369A1",
                "MW 9:30-10:45 AM",
                "Room 101",
                "Spring 2026",
                now));

        db.courseDao().insert(new Course(
                "CSCI 638 - Visual Analytics",
                "Prof. Johnson",
                "#6D28D9",
                "MW 11:00-12:15 PM",
                "Room 102",
                "Spring 2026",
                now));

        db.courseDao().insert(new Course(
                "CSCI 502 - Programming Principles in Java",
                "Prof. Johnson",
                "#065F46",
                "MW 2:00-3:15 PM",
                "Room 103",
                "Spring 2026",
                now));

        db.courseDao().insert(new Course(
                "CSCI 602 - Design & Analysis of Algorithms",
                "Prof. Williams",
                "#B45309",
                "TTh 9:30-10:45 AM",
                "Room 103",
                "Spring 2026",
                now));

        db.courseDao().insert(new Course(
                "CSCI 627 - Data Visualization",
                "Prof. Brown",
                "#0F766E",
                "MW 12:30-1:45 PM",
                "Room 103",
                "Spring 2026",
                now));

        db.courseDao().insert(new Course(
                "CSCI 671 - Software Testing & Quality Assurance",
                "Prof. Davis",
                "#9D174D",
                "MW 12:30-1:45 PM",
                "Room 104",
                "Spring 2026",
                now));

        db.courseDao().insert(new Course(
                "CSCI 636 - Pattern Recognition & Data Mining 1",
                "Prof. Miller",
                "#3730A3",
                "TTh 2:00-3:15 PM",
                "Room 201",
                "Spring 2026",
                now));

        db.courseDao().insert(new Course(
                "CSCI 513 - Software Development & Engineering",
                "Prof. Davis",
                "#1E3A5F",
                "MW 9:30-10:45 AM",
                "Room 202",
                "Spring 2026",
                now));
    }

    /**
     * Inserts a mix of completed tasks, pending assignments, and final exams
     * across the seeded courses so the Tasks tab has content on first launch.
     */
    private static void seedTasks(StudyPlannerDatabase db) {

        // Completed tasks

        Task t1 = new Task(1L,
                "Where's My Car App",
                "Android app to save and retrieve parking location using GPS and Google Maps API",
                Task.TYPE_PROJECT, Task.PRIORITY_HIGH,
                date(2026, 4, 25, 23, 59),
                date(2026, 4, 20, 10, 0));
        t1.setCompleted(true);
        t1.setCompletedAt(date(2026, 4, 28, 14, 30));
        db.taskDao().insert(t1);

        Task t2 = new Task(1L,
                "Room Database Implementation",
                "Implement Room DB with DAO interfaces, entity classes, and database migrations",
                Task.TYPE_ASSIGNMENT, Task.PRIORITY_HIGH,
                date(2026, 4, 26, 23, 59),
                date(2026, 4, 21, 9, 0));
        t2.setCompleted(true);
        t2.setCompletedAt(date(2026, 4, 27, 16, 45));
        db.taskDao().insert(t2);

        Task t3 = new Task(3L,
                "Java Collections Framework Quiz",
                "Online quiz covering List, Set, Map, and Queue implementations and complexity",
                Task.TYPE_QUIZ, Task.PRIORITY_MEDIUM,
                date(2026, 4, 24, 23, 59),
                date(2026, 4, 20, 8, 0));
        t3.setCompleted(true);
        t3.setCompletedAt(date(2026, 4, 26, 11, 0));
        db.taskDao().insert(t3);

        Task t4 = new Task(4L,
                "Sorting Algorithms Analysis",
                "Written comparative analysis of QuickSort, MergeSort, and HeapSort performance",
                Task.TYPE_ASSIGNMENT, Task.PRIORITY_MEDIUM,
                date(2026, 4, 23, 23, 59),
                date(2026, 4, 18, 10, 0));
        t4.setCompleted(true);
        t4.setCompletedAt(date(2026, 4, 25, 15, 30));
        db.taskDao().insert(t4);

        Task t5 = new Task(6L,
                "Test Case Design Exercise",
                "Design black-box and white-box test cases for the provided program specification",
                Task.TYPE_ASSIGNMENT, Task.PRIORITY_LOW,
                date(2026, 4, 22, 23, 59),
                date(2026, 4, 18, 9, 0));
        t5.setCompleted(true);
        t5.setCompletedAt(date(2026, 4, 24, 13, 0));
        db.taskDao().insert(t5);

        Task t6 = new Task(8L,
                "SDE Requirements Document",
                "Software requirements specification document for the semester project system",
                Task.TYPE_ASSIGNMENT, Task.PRIORITY_MEDIUM,
                date(2026, 4, 21, 23, 59),
                date(2026, 4, 16, 10, 0));
        t6.setCompleted(true);
        t6.setCompletedAt(date(2026, 4, 23, 17, 0));
        db.taskDao().insert(t6);

        Task t7 = new Task(7L,
                "K-Means Clustering Lab",
                "Implement and evaluate K-Means clustering algorithm on the provided dataset",
                Task.TYPE_ASSIGNMENT, Task.PRIORITY_MEDIUM,
                date(2026, 4, 20, 23, 59),
                date(2026, 4, 15, 10, 0));
        t7.setCompleted(true);
        t7.setCompletedAt(date(2026, 4, 22, 14, 0));
        db.taskDao().insert(t7);

        // Pending assignments

        db.taskDao().insert(new Task(1L,
                "Smart Study Planner - Final Debug",
                "Fix all remaining bugs, test all features end-to-end before submission",
                Task.TYPE_ASSIGNMENT, Task.PRIORITY_HIGH,
                date(2026, 4, 30, 23, 59),
                date(2026, 4, 29, 10, 0)));

        db.taskDao().insert(new Task(4L,
                "Algorithm Homework - Graph Problems",
                "Complete problems 5.3, 5.7, 5.9 on shortest path and minimum spanning tree",
                Task.TYPE_ASSIGNMENT, Task.PRIORITY_HIGH,
                date(2026, 4, 30, 23, 59),
                date(2026, 4, 28, 9, 0)));

        db.taskDao().insert(new Task(2L,
                "Visual Analytics Reading Response",
                "Written response to assigned reading on perception and visual encoding channels",
                Task.TYPE_ASSIGNMENT, Task.PRIORITY_MEDIUM,
                date(2026, 4, 30, 23, 59),
                date(2026, 4, 28, 11, 0)));

        db.taskDao().insert(new Task(7L,
                "Pattern Recognition Problem Set",
                "Problem set on Bayesian classifiers and discriminant analysis - problems 3, 4, 6",
                Task.TYPE_ASSIGNMENT, Task.PRIORITY_MEDIUM,
                date(2026, 4, 30, 23, 59),
                date(2026, 4, 29, 9, 0)));

        db.taskDao().insert(new Task(3L,
                "Java Streams and Lambdas Assignment",
                "Implement 8 programming exercises using Java 8 streams, lambdas, and method references",
                Task.TYPE_ASSIGNMENT, Task.PRIORITY_HIGH,
                date(2026, 5, 1, 23, 59),
                date(2026, 4, 27, 10, 0)));

        db.taskDao().insert(new Task(5L,
                "Data Visualization Critique",
                "Written critique of 3 real-world visualizations - evaluate design choices using course principles",
                Task.TYPE_ASSIGNMENT, Task.PRIORITY_MEDIUM,
                date(2026, 5, 1, 23, 59),
                date(2026, 4, 28, 10, 0)));

        db.taskDao().insert(new Task(6L,
                "Software Testing Quiz",
                "Online quiz on equivalence partitioning, boundary value analysis, and control flow testing",
                Task.TYPE_QUIZ, Task.PRIORITY_HIGH,
                date(2026, 5, 1, 23, 59),
                date(2026, 4, 29, 9, 0)));

        db.taskDao().insert(new Task(8L,
                "SDE Code Review Submission",
                "Submit peer code review comments and quality assessment for assigned team project",
                Task.TYPE_ASSIGNMENT, Task.PRIORITY_MEDIUM,
                date(2026, 5, 1, 23, 59),
                date(2026, 4, 29, 10, 0)));

        db.taskDao().insert(new Task(1L,
                "Final Project Submission",
                "Complete Smart Study Planner Android app - MVVM, Room DB, Navigation, all features implemented",
                Task.TYPE_PROJECT, Task.PRIORITY_HIGH,
                date(2026, 5, 2, 23, 59),
                date(2026, 4, 28, 10, 0)));

        db.taskDao().insert(new Task(4L,
                "Algorithm Analysis Report",
                "Written analysis of time and space complexity for all assigned algorithm problems",
                Task.TYPE_ASSIGNMENT, Task.PRIORITY_HIGH,
                date(2026, 5, 2, 23, 59),
                date(2026, 4, 27, 9, 0)));

        db.taskDao().insert(new Task(6L,
                "Unit Test Suite Implementation",
                "Complete JUnit 5 test suite covering all major components of the semester project",
                Task.TYPE_ASSIGNMENT, Task.PRIORITY_HIGH,
                date(2026, 5, 2, 23, 59),
                date(2026, 4, 27, 10, 0)));

        db.taskDao().insert(new Task(7L,
                "Pattern Mining Lab Report",
                "Lab report on association rule mining using the Apriori algorithm on retail dataset",
                Task.TYPE_ASSIGNMENT, Task.PRIORITY_HIGH,
                date(2026, 5, 2, 23, 59),
                date(2026, 4, 27, 11, 0)));

        db.taskDao().insert(new Task(8L,
                "SDE Sprint Review",
                "Final sprint review presentation and live demo of semester project deliverable",
                Task.TYPE_PROJECT, Task.PRIORITY_HIGH,
                date(2026, 5, 2, 23, 59),
                date(2026, 4, 26, 10, 0)));

        db.taskDao().insert(new Task(2L,
                "Visual Analytics Dashboard",
                "Interactive multi-view dashboard with brushing and linking using D3.js",
                Task.TYPE_PROJECT, Task.PRIORITY_HIGH,
                date(2026, 5, 3, 23, 59),
                date(2026, 4, 25, 10, 0)));

        db.taskDao().insert(new Task(6L,
                "Regression Testing Report",
                "Document regression test strategy, test suite, and results for semester project",
                Task.TYPE_ASSIGNMENT, Task.PRIORITY_MEDIUM,
                date(2026, 5, 4, 23, 59),
                date(2026, 4, 26, 10, 0)));

        db.taskDao().insert(new Task(7L,
                "Decision Tree Implementation",
                "Implement ID3 decision tree classifier and evaluate accuracy on classification dataset",
                Task.TYPE_ASSIGNMENT, Task.PRIORITY_MEDIUM,
                date(2026, 5, 4, 23, 59),
                date(2026, 4, 26, 11, 0)));

        db.taskDao().insert(new Task(3L,
                "Java Design Patterns Quiz",
                "Online quiz covering Singleton, Factory, Observer, and Decorator design patterns",
                Task.TYPE_QUIZ, Task.PRIORITY_MEDIUM,
                date(2026, 5, 5, 23, 59),
                date(2026, 4, 28, 9, 0)));

        db.taskDao().insert(new Task(5L,
                "D3.js Visualization Lab",
                "Create interactive bar chart and scatter plot with tooltips and transitions using D3.js v7",
                Task.TYPE_ASSIGNMENT, Task.PRIORITY_MEDIUM,
                date(2026, 5, 5, 23, 59),
                date(2026, 4, 28, 10, 0)));

        db.taskDao().insert(new Task(5L,
                "Information Visualization Project",
                "Final project - interactive visualization of chosen public dataset with narrative",
                Task.TYPE_PROJECT, Task.PRIORITY_HIGH,
                date(2026, 5, 6, 23, 59),
                date(2026, 4, 24, 10, 0)));

        db.taskDao().insert(new Task(4L,
                "Dynamic Programming Assignment",
                "Implement solutions to LCS, 0/1 Knapsack, Matrix Chain Multiplication, and Coin Change",
                Task.TYPE_ASSIGNMENT, Task.PRIORITY_LOW,
                date(2026, 5, 9, 23, 59),
                date(2026, 4, 28, 10, 0)));

        db.taskDao().insert(new Task(6L,
                "Software Quality Metrics Report",
                "Final project - comprehensive metrics analysis and quality assessment report",
                Task.TYPE_PROJECT, Task.PRIORITY_HIGH,
                date(2026, 5, 9, 23, 59),
                date(2026, 4, 24, 10, 0)));

        db.taskDao().insert(new Task(8L,
                "SDE Project Documentation",
                "Complete technical documentation package for semester project final deliverable",
                Task.TYPE_PROJECT, Task.PRIORITY_HIGH,
                date(2026, 5, 9, 23, 59),
                date(2026, 4, 24, 11, 0)));

        // Final exams, one per course

        db.taskDao().insert(new Task(1L,
                "Adv Android Mobile Device Programming Final Exam",
                "Final comprehensive exam",
                Task.TYPE_EXAM, Task.PRIORITY_HIGH,
                date(2026, 5, 3, 12, 30),
                date(2026, 4, 25, 9, 0)));

        db.taskDao().insert(new Task(2L,
                "Visual Analytics Final Exam",
                "Final exam covering visual perception, interaction techniques, evaluation methods, " +
                        "and visual analytics system design",
                Task.TYPE_EXAM, Task.PRIORITY_HIGH,
                date(2026, 5, 4, 8, 0),
                date(2026, 4, 25, 9, 0)));

        db.taskDao().insert(new Task(3L,
                "Programming Principles in Java Final Exam",
                "Final exam covering OOP concepts, inheritance, polymorphism, generics, " +
                        "Java Collections Framework, streams, lambdas, and design patterns",
                Task.TYPE_EXAM, Task.PRIORITY_HIGH,
                date(2026, 5, 5, 14, 0),
                date(2026, 4, 25, 9, 0)));

        db.taskDao().insert(new Task(4L,
                "Design & Analysis of Algorithms Final Exam",
                "Final exam covering greedy algorithms, dynamic programming, graph algorithms, " +
                        "NP-completeness, approximation algorithms, and computational complexity",
                Task.TYPE_EXAM, Task.PRIORITY_HIGH,
                date(2026, 5, 6, 11, 0),
                date(2026, 4, 25, 9, 0)));

        db.taskDao().insert(new Task(5L,
                "Data Visualization Final Exam",
                "Final exam covering D3.js, visualization design principles, color theory, " +
                        "interaction techniques, and data storytelling",
                Task.TYPE_EXAM, Task.PRIORITY_HIGH,
                date(2026, 5, 3, 10, 0),
                date(2026, 4, 25, 9, 0)));

        db.taskDao().insert(new Task(6L,
                "Software Testing & Quality Assurance Final Exam",
                "Final exam covering unit testing, JUnit 5, black-box and white-box testing, " +
                        "code coverage, regression testing, and software quality metrics",
                Task.TYPE_EXAM, Task.PRIORITY_HIGH,
                date(2026, 5, 4, 11, 0),
                date(2026, 4, 25, 9, 0)));

        db.taskDao().insert(new Task(7L,
                "Pattern Recognition & Data Mining Final Exam",
                "Final exam covering Bayesian classifiers, decision trees, SVMs, " +
                        "clustering algorithms, dimensionality reduction, and association rule mining",
                Task.TYPE_EXAM, Task.PRIORITY_HIGH,
                date(2026, 5, 5, 9, 30),
                date(2026, 4, 25, 9, 0)));

        db.taskDao().insert(new Task(8L,
                "Software Development & Engineering Final Exam",
                "Final exam covering SDLC models, UML diagrams, design patterns, Agile methodologies, " +
                        "software architecture, and project management principles",
                Task.TYPE_EXAM, Task.PRIORITY_HIGH,
                date(2026, 5, 7, 14, 0),
                date(2026, 4, 25, 9, 0)));
    }

    /**
     * Inserts recent study sessions so the Stats charts have content on first run.
     */
    private static void seedStudySessions(StudyPlannerDatabase db) {

        db.studySessionDao().insert(new StudySession(
                1L, "Final project polish and testing",
                date(2026, 4, 30, 9, 0), date(2026, 4, 30, 10, 30),
                5400L, StudySession.SESSION_TYPE_POMODORO, 2, true));

        db.studySessionDao().insert(new StudySession(
                4L, "Algorithm analysis report writing",
                date(2026, 4, 30, 14, 0), date(2026, 4, 30, 15, 0),
                3600L, StudySession.SESSION_TYPE_FREE, 0, true));

        db.studySessionDao().insert(new StudySession(
                1L, "UI redesign and splash screen animations",
                date(2026, 4, 29, 19, 0), date(2026, 4, 29, 21, 0),
                7200L, StudySession.SESSION_TYPE_POMODORO, 4, true));

        db.studySessionDao().insert(new StudySession(
                2L, "Visual analytics dashboard linked views implementation",
                date(2026, 4, 29, 15, 0), date(2026, 4, 29, 16, 15),
                4500L, StudySession.SESSION_TYPE_FREE, 0, true));

        db.studySessionDao().insert(new StudySession(
                3L, "Design patterns review - Factory and Observer patterns",
                date(2026, 4, 28, 18, 0), date(2026, 4, 28, 18, 45),
                2700L, StudySession.SESSION_TYPE_FREE, 0, true));

        db.studySessionDao().insert(new StudySession(
                6L, "JUnit test suite for semester project components",
                date(2026, 4, 28, 8, 0), date(2026, 4, 28, 9, 30),
                5400L, StudySession.SESSION_TYPE_POMODORO, 2, true));

        db.studySessionDao().insert(new StudySession(
                5L, "D3.js scatter plot and bar chart implementation",
                date(2026, 4, 27, 16, 0), date(2026, 4, 27, 17, 0),
                3600L, StudySession.SESSION_TYPE_FREE, 0, true));

        db.studySessionDao().insert(new StudySession(
                4L, "Graph algorithms - Dijkstra and Bellman-Ford exam review",
                date(2026, 4, 27, 10, 0), date(2026, 4, 27, 12, 0),
                7200L, StudySession.SESSION_TYPE_POMODORO, 4, true));
    }

    /**
     * Inserts starter flashcard decks for each seeded course.
     */
    private static void seedFlashcards(StudyPlannerDatabase db) {
        long now = System.currentTimeMillis();

        db.flashcardDao().insert(new Flashcard(1L, "What is MVVM architecture?",
                "Model-View-ViewModel. Separates UI (View) from business logic (ViewModel) and data (Model). ViewModel survives configuration changes via LiveData observers.", now));
        db.flashcardDao().insert(new Flashcard(1L, "What is Room Database?",
                "Android's ORM library built on SQLite. Uses @Entity, @Dao, and @Database annotations to generate type-safe, compile-time-verified database access code.", now));
        db.flashcardDao().insert(new Flashcard(1L, "What is LiveData?",
                "An observable data holder class that is lifecycle-aware. Only updates active observers, preventing memory leaks and null pointer crashes from destroyed UI.", now));
        db.flashcardDao().insert(new Flashcard(1L, "What is a NavController?",
                "The central component of Jetpack Navigation. Manages app navigation, fragment back stack, and transitions via a navigation graph defined in XML.", now));
        db.flashcardDao().insert(new Flashcard(1L, "What is ViewBinding?",
                "A Jetpack feature generating a binding class for each XML layout. Provides direct null-safe references to all views with IDs without using findViewById.", now));
        db.flashcardDao().insert(new Flashcard(1L, "What is a RecyclerView?",
                "An efficient scrollable list/grid component using ViewHolder pattern and a LayoutManager. Recycles off-screen item views to improve performance and memory use.", now));
        db.flashcardDao().insert(new Flashcard(1L, "What is WorkManager?",
                "Jetpack library for guaranteed background work persisting across app restarts and device reboots. Handles constraints like network availability and battery level.", now));
        db.flashcardDao().insert(new Flashcard(1L, "What is the difference between ViewModel and onSaveInstanceState?",
                "ViewModel survives configuration changes (rotation) but not process death. onSaveInstanceState survives process death but is limited to small serializable data.", now));

        db.flashcardDao().insert(new Flashcard(4L, "What is Big-O notation?",
                "A mathematical notation describing the upper bound of an algorithm's time or space complexity as input size n grows toward infinity, ignoring constant factors.", now));
        db.flashcardDao().insert(new Flashcard(4L, "What is a greedy algorithm?",
                "An algorithm making the locally optimal choice at each step. Correct for Huffman coding and Dijkstra's shortest path but not all optimization problems.", now));
        db.flashcardDao().insert(new Flashcard(4L, "What is dynamic programming?",
                "Solving complex problems by breaking them into overlapping subproblems and storing results to avoid recomputation. Uses memoization (top-down) or tabulation (bottom-up).", now));
        db.flashcardDao().insert(new Flashcard(4L, "What is the time complexity of Merge Sort?",
                "O(n log n) in all cases - best, average, and worst. Uses divide-and-conquer, recursively splitting the array in half and merging sorted halves back together.", now));
        db.flashcardDao().insert(new Flashcard(4L, "What is an NP-complete problem?",
                "A problem in NP that is at least as hard as every other NP problem. No known polynomial-time solution exists. Classic example: the Travelling Salesman Problem.", now));
        db.flashcardDao().insert(new Flashcard(4L, "What is Dijkstra's algorithm?",
                "A greedy algorithm for finding shortest paths from a source to all vertices in a weighted graph with non-negative edge weights. O((V+E) log V) with a min-heap.", now));

        db.flashcardDao().insert(new Flashcard(3L, "What is polymorphism in Java?",
                "The ability of an object to take many forms. A parent class reference can hold a child class object, enabling runtime method overriding through dynamic dispatch.", now));
        db.flashcardDao().insert(new Flashcard(3L, "What is the difference between ArrayList and LinkedList?",
                "ArrayList uses a dynamic array - O(1) random access, O(n) insert/delete. LinkedList uses doubly linked nodes - O(1) insert/delete at ends, O(n) random access.", now));
        db.flashcardDao().insert(new Flashcard(3L, "What is a Lambda expression in Java?",
                "An anonymous function introduced in Java 8. Enables functional programming with syntax: (parameters) -> expression. Used with functional interfaces like Runnable and Comparator.", now));
        db.flashcardDao().insert(new Flashcard(3L, "What is the difference between abstract class and interface?",
                "Abstract class can have state, constructors, and partial implementation. Interface (Java 8+) can have default and static methods but no instance state or constructors.", now));
        db.flashcardDao().insert(new Flashcard(3L, "What is the Java Collections Framework?",
                "A unified architecture of interfaces (List, Set, Map, Queue) and implementations (ArrayList, HashMap, TreeSet) for storing and manipulating groups of objects.", now));

        db.flashcardDao().insert(new Flashcard(2L, "What is Visual Analytics?",
                "The science of analytical reasoning supported by interactive visual interfaces. Combines automated data analysis with human-in-the-loop interaction for complex insight discovery.", now));
        db.flashcardDao().insert(new Flashcard(2L, "What is Shneiderman's mantra?",
                "Overview first, zoom and filter, then details on demand. A foundational design principle for interactive information visualization interface design.", now));
        db.flashcardDao().insert(new Flashcard(2L, "What is a treemap?",
                "A space-filling visualization displaying hierarchical data as nested rectangles. Size and color encode quantitative attributes for comparison within hierarchy levels.", now));
        db.flashcardDao().insert(new Flashcard(2L, "What is the difference between data visualization and visual analytics?",
                "Data visualization presents data visually for communication. Visual analytics integrates automated analysis algorithms with interactive visualization to support decision-making.", now));

        db.flashcardDao().insert(new Flashcard(5L, "What is D3.js?",
                "Data-Driven Documents - a JavaScript library for creating dynamic, interactive data visualizations in browsers using SVG, HTML, and CSS driven by bound data arrays.", now));
        db.flashcardDao().insert(new Flashcard(5L, "What is the Gestalt principle of proximity?",
                "Objects placed close together are perceived as a group. Used in chart design to visually cluster related data elements without explicit borders or labels.", now));
        db.flashcardDao().insert(new Flashcard(5L, "What is a choropleth map?",
                "A thematic map where geographic areas are shaded proportionally to a statistical variable such as population density, income, or election results per region.", now));
        db.flashcardDao().insert(new Flashcard(5L, "What is preattentive processing?",
                "Visual processing occurring automatically before conscious attention - within 200ms. Color, size, orientation, and shape are preattentive attributes used in visualization design.", now));

        db.flashcardDao().insert(new Flashcard(6L, "What is unit testing?",
                "Testing individual components or functions in isolation to verify correctness. JUnit is the standard Java framework. Tests must be fast, isolated, repeatable, and self-validating.", now));
        db.flashcardDao().insert(new Flashcard(6L, "What is the difference between black-box and white-box testing?",
                "Black-box tests external behavior without code knowledge. White-box tests internal logic, code paths, and structure. Both strategies are complementary and necessary.", now));
        db.flashcardDao().insert(new Flashcard(6L, "What is code coverage?",
                "A metric measuring percentage of source code executed during testing. Types include statement, branch, condition, and path coverage. Higher coverage reduces undetected defects.", now));
        db.flashcardDao().insert(new Flashcard(6L, "What is regression testing?",
                "Re-running a full test suite after code changes to ensure new modifications do not break previously working functionality. Critical component of CI/CD pipelines.", now));

        db.flashcardDao().insert(new Flashcard(7L, "What is supervised learning?",
                "A machine learning paradigm where a model trains on labeled input-output pairs to learn a mapping function, then predicts outputs for new unseen inputs.", now));
        db.flashcardDao().insert(new Flashcard(7L, "What is the K-Means algorithm?",
                "An unsupervised clustering algorithm partitioning n observations into k clusters by iteratively assigning points to nearest centroid and updating centroids until convergence.", now));
        db.flashcardDao().insert(new Flashcard(7L, "What is overfitting?",
                "When a model learns training data too well including noise, causing poor generalization to new data. Addressed with regularization, dropout, cross-validation, or more training data.", now));
        db.flashcardDao().insert(new Flashcard(7L, "What is a confusion matrix?",
                "A table showing TP, TN, FP, FN counts for a classification model. Used to compute precision, recall, F1-score, and accuracy to evaluate classifier performance.", now));

        db.flashcardDao().insert(new Flashcard(8L, "What is Agile methodology?",
                "An iterative software development approach emphasizing collaboration, working software, customer feedback, and responding to change over rigid upfront planning.", now));
        db.flashcardDao().insert(new Flashcard(8L, "What is the difference between Scrum and Kanban?",
                "Scrum uses fixed-length sprints with defined roles (PO, SM, Dev). Kanban is a continuous flow system with no iterations, using a visual board to limit work-in-progress.", now));
        db.flashcardDao().insert(new Flashcard(8L, "What is a design pattern?",
                "A reusable solution template for a commonly occurring software design problem. Categories: Creational (Factory, Singleton), Structural (Adapter), Behavioral (Observer, Strategy).", now));
        db.flashcardDao().insert(new Flashcard(8L, "What is CI/CD?",
                "Continuous Integration automatically builds and tests code on every commit. Continuous Delivery automates deployment of tested builds to staging or production environments.", now));
    }
}