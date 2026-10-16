# Smart Study Planner

An Android app (Java) that helps students stay organized while studying. Courses, tasks, deadlines, a Pomodoro timer, study stats, focus music, flashcards, a GPA calculator, and study tips all live in one app.

All data is stored locally on the device. The app works fully offline and nothing is sent to a server.

**Developer:** Pooja Cheema

---

## Features

### Main tabs
- **Home:** time-based greeting, daily motivational quote, study streak, today's progress ring, quick stats (today, this week, tasks due), live countdowns for exams due within 48 hours, Due Today list, and shortcuts to the study tools.
- **Courses:** add and edit courses with code, name, instructor, schedule, room, semester, and one of 20 preset colors. Includes live search by name, instructor, room, or schedule.
- **Tasks:** assignments, exams, quizzes, and projects with due date, priority, type, and course. Pending, Done, All, and Overdue tabs, each with its own remembered sort and filter settings.
- **Timer:** Pomodoro timer (25 min focus, 5 min break, 15 min long break after every fourth session) with adjustable durations, course selection, and a Recent Sessions list.
- **Stats:** daily study hours bar chart, 28-day activity heatmap, per-course study time for the last 30 days, and a task completion donut chart.

### Study tools
- **Study Music:** 16 ambient focus sounds with volume control. Music keeps playing across screens and with the phone locked, with a mini player at the top of every screen.
- **Flashcards:** per-course decks with a 3D flip, Again / Got It marking, mastery progress, and deck reset.
- **GPA Calculator:** running GPA plus a "What Do I Need?" tool for reaching a target GPA.
- **Study Tips:** 32 tips across 8 topics, with next and random tip.

### Other
- **Deadline reminders:** notification 24 hours before a task is due, scheduled with AlarmManager. Reminders survive app close and device restart.
- **Home screen widget:** pending task count and today's study time, refreshed every 30 minutes.
- **Animated splash screen** with orbiting feature pills.
- **Custom confirmation dialogs** for complete, incomplete, and delete actions.

---

## Tech stack

- **Language:** Java 8
- **Platform:** Android, minSdk 26 (Android 8.0), targetSdk 34
- **Architecture:** single main Activity with fragments, ViewModel + LiveData + Repository
- **Database:** Room (local SQLite)
- **Navigation:** Android Navigation Component with a bottom navigation bar
- **UI:** View Binding, Material Components, CardView, RecyclerView
- **Charts:** MPAndroidChart
- **Background work:** foreground service for music playback, AlarmManager for reminders, boot receiver to restore reminders after restart
- **Widget:** AppWidgetProvider

---

## Project structure

```
edu.niu.android.smartstudyplanner
├── data/
│   ├── dao/            Room DAOs (e.g. TaskDao)
│   ├── database/       StudyPlannerDatabase, DatabaseSeeder
│   ├── entities/       Room entities (e.g. Task)
│   └── repository/     StudyRepository
├── notifications/      NotificationHelper, NotificationReceiver (reminders, boot restore)
├── ui/
│   ├── MainActivity    Hosts navigation, toolbar, and mini player
│   ├── SplashActivity  Animated splash (SplashBackgroundView draws its background)
│   ├── planner/        Home screen, Due Today screen
│   ├── courses/        Course list, add/edit course
│   ├── tasks/          Task list, add/edit task
│   ├── timer/          Timer tab, full-screen study timer
│   ├── stats/          Statistics and charts
│   ├── music/          MusicFragment, MusicService
│   ├── flashcards/
│   ├── gpa/
│   ├── tips/
│   └── countdown/
├── utils/              Date and color helpers
├── viewmodel/          ViewModels
├── StudyPlannerApp     Application class
└── StudyPlannerWidget  Home screen widget
```

---

## Getting started

1. Install the latest stable version of [Android Studio](https://developer.android.com/studio).
2. Clone or download this repository.
   - On Windows, extract the project to a short path such as `C:\dev\SmartStudyPlanner`. Deep folder paths can exceed Windows' 260-character limit and cause "Path too long" errors.
3. Open the project folder in Android Studio and let Gradle sync.
4. Run the app on an emulator or a physical device running Android 8.0 (API 26) or higher.

---

## Permissions

| Permission | Why it's needed |
|---|---|
| `POST_NOTIFICATIONS` | Show deadline reminders and the music playback notification (Android 13+) |
| `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM` | Fire reminders exactly 24 hours before a due date |
| `RECEIVE_BOOT_COMPLETED` | Restore scheduled reminders after the phone restarts |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Keep study music playing in the background |
| `VIBRATE` | Notification vibration |

On first launch, the app asks for notification permission and, on Android 12+, opens the exact alarm settings screen if that permission isn't granted yet.

---

## Documentation

- [App overview (PDF)](docs/Smart-Study-Planner-Overview.pdf)