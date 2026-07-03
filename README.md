# Unified Productivity

An Android app that unifies **Notes**, **Reminders**, and **Calendar** into one
cohesive workspace — preserving each module's distinct feel while adding
cross-module linking (a deadline can block time on the calendar; a meeting can spin
up a linked note) and Google Drive sync.

> Status: **Phase 1 foundation** — a buildable app skeleton with all three modules
> functional end-to-end on a local Room database, the unified Home dashboard, and
> the cross-module linking + sync scaffolding in place. See
> [Implementation status](#implementation-status) for exactly what is and isn't wired up.

---

## Tech stack

| Concern | Choice |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 (theme-aware, dark mode) |
| Architecture | MVVM (repositories → ViewModels → Compose screens) |
| Persistence | Room (SQLite) |
| Async | Coroutines + `Flow` |
| Navigation | Navigation-Compose (bottom nav) |
| Background sync | WorkManager |
| Build | Gradle (Kotlin DSL) + version catalog |
| Min / Target SDK | 29 (Android 10) / 35 (Android 15) |

## Building

You need the **Android SDK** (platform 35, build-tools) and JDK 17.

```bash
# Point Gradle at your SDK (or set ANDROID_HOME / ANDROID_SDK_ROOT)
echo "sdk.dir=/path/to/Android/sdk" > local.properties

# Build a debug APK
./gradlew assembleDebug

# Run the JVM unit tests (conflict resolution + date math)
./gradlew test
```

The APK lands in `app/build/outputs/apk/debug/`.

> The Gradle wrapper is pinned to Gradle 8.9. If your network blocks
> `services.gradle.org`, run with a locally installed Gradle of a compatible
> version instead (`gradle assembleDebug`).

## Project structure

```
app/src/main/java/com/unifiedproductivity/app/
├── UnifiedProductivityApp.kt     # Application: DI container, first-run seeding, sync scheduling
├── MainActivity.kt               # Single activity hosting Compose
├── di/AppContainer.kt            # Manual dependency graph (DB → repositories → link service)
├── data/
│   ├── entity/                   # Room entities: Note, Folder, Reminder, Subtask,
│   │                             #   ReminderList, CalendarEntity, Event
│   ├── dao/                      # Room DAOs (Flow-based queries incl. smart lists)
│   ├── repository/               # NotesRepository, RemindersRepository, CalendarRepository
│   ├── model/Enums.kt            # Priority, NoteType, RecurrenceFrequency, SmartList, ...
│   ├── Converters.kt             # Room type converters (List<String>)
│   └── AppDatabase.kt            # RoomDatabase definition
├── integration/LinkService.kt    # Cross-module glue (reminder↔calendar, event→note)
├── notifications/                # NotificationHelper, ReminderScheduler (AlarmManager),
│                                 #   ReminderAlarmReceiver, BootReceiver
├── sync/
│   ├── ConflictResolver.kt       # Last-write-wins merge (unit-tested)
│   ├── BackupManager.kt          # JSON snapshot export / merge / restore
│   ├── DriveClient.kt            # Drive v3 REST calls (appDataFolder)
│   ├── DriveSyncManager.kt       # Google Sign-In + push/pull orchestration
│   └── SyncWorker.kt             # WorkManager periodic sync (runs when signed in)
├── util/DateTimeUtils.kt         # Date/recurrence helpers (unit-tested)
└── ui/
    ├── AppRoot.kt                # Bottom nav + NavHost
    ├── theme/                    # Color, Type, Theme (per-module accents, dark mode)
    ├── home/                     # Unified dashboard (today's events + due/overdue tasks)
    ├── notes/                    # Notes list + rich-text editor
    ├── reminders/                # Reminders list, smart-list chips, add dialog
    ├── calendar/                 # Month grid + day agenda, add-event dialog
    └── settings/                 # Google Drive sign-in, sync / backup / restore
```

## Features implemented

**Notes**
- Create / edit / delete (soft delete), pin to top
- Folders + folder filter chips
- Full-text search across title & content
- Tags, and specialized note *types* (meeting, research, journal, financial) in the model
- Rich-text WYSIWYG editor (bold/italic/underline/headings/lists), stored as Markdown

**Reminders**
- Create / complete / flag / delete, with priority (High/Medium/Low)
- Smart lists with live counts: Today, Scheduled, All, Flagged, Overdue, Completed
- User-created lists
- Due dates, recurring reminders (roll forward on completion), subtasks & task
  dependencies (`blockedBy`) in the model
- "Block this time on Calendar" when adding a due reminder → creates a linked Focus-Time event

**Calendar**
- Month grid with event dots, tap-to-select day, day agenda list
- Create all-day or timed events, with location
- Multiple color-coded calendars (visibility-aware queries)
- "Attach note" when creating an event → creates a linked meeting note

**Home dashboard**
- Merges today's events, tasks due today, and overdue tasks into one view
- Quick actions to jump into each module

**Cross-module integration** (`LinkService`)
- Reminder → Calendar focus-time block (color-coded by priority; archived when the reminder completes)
- Event → pre-filled, bidirectionally linked meeting note; the note button on any
  event opens its meeting note (created on first tap)
- Link indicators on rows (reminder shows a calendar icon when time is blocked;
  event shows when it's tied to a reminder); Home rows jump to their module

**Editing & interaction (Apple-style)**
- Tap any reminder or event to edit it in place; real date & time pickers
- Swipe left to delete notes and reminders
- Locations on both reminders and events; event locations open in the maps app
- Notes can be filed into folders from the editor
- Manual **System / Light / Dark** theme switch in Settings (dynamic color disabled
  so the per-module accent palette stays consistent)

**Notifications**
- Local due-date notifications via `AlarmManager` + a broadcast receiver
- Notification channel + runtime `POST_NOTIFICATIONS` request; exact-alarm fallback on Android 12+
- Alarms re-armed after reboot; auto-cancelled when a reminder is completed or deleted

**Rich-text notes**
- WYSIWYG editor (bold / italic / underline / headings / bulleted & numbered lists) via compose-rich-editor
- Content persisted as Markdown, so it stays portable and syncs cleanly

**Google Drive sync** (Settings tab)
- Google Sign-In with the `drive.appdata` scope (private app folder)
- Sync now / Back up now / Restore, plus a periodic WorkManager sync when signed in
- JSON snapshots (`notes.json`, `reminders.json`, `calendar.json`) merged last-write-wins

## Implementation status

| Area | State |
| --- | --- |
| Local data layer (Room) for all 3 modules | ✅ Complete |
| Notes / Reminders / Calendar UI | ✅ Functional |
| Rich-text note editor (Markdown-backed) | ✅ Functional |
| Home unified dashboard | ✅ Functional |
| Cross-module linking | ✅ Functional |
| Due-reminder notifications (alarm + boot re-arm) | ✅ Functional |
| Conflict resolution (last-write-wins) | ✅ Implemented + unit-tested |
| Google Drive sync (sign-in, push/pull, backup, restore) | ✅ Implemented — needs a Cloud OAuth client (below) |
| OCR, note version history, geofencing | ⛔ Future phases (modeled where relevant) |

### Enabling Google Drive sync

The sync code is complete; to use it you register the app in a Google Cloud
project (no secret is committed — Google Sign-In authenticates via the signing
certificate):

1. Create a project at [console.cloud.google.com](https://console.cloud.google.com) and **enable the Google Drive API**.
2. Configure the OAuth consent screen (add your Google account as a test user).
3. Create an **OAuth client ID → Android**, using the package name
   `com.unifiedproductivity.app` and your APK's signing SHA-1
   (`keytool -list -v -keystore <keystore>` — for the CI debug APK, the debug keystore's SHA-1).
4. Install the app, open **Settings → Sign in with Google**, and approve Drive access.

Data lives in Drive's private *appDataFolder* (invisible to the rest of your
Drive). Soft deletes (`deletedAt`) propagate across devices during sync.

## Design decisions (answers to the spec's open questions)

- **Calendar view:** Month view first (most useful for planning); Day/Week are next.
- **Notes formatting:** Rich-text WYSIWYG, persisted as Markdown.
- **Recurring reminders:** Supported (roll forward to next occurrence on completion).
- **Time tracking:** Modeled (`estimatedMinutes`/`actualMinutes`); UI deferred.
- **Conflict resolution:** Last-write-wins, as specified.
- **Geofencing:** Deferred; `Reminder` keeps the fields for later.

## Tests

```bash
./gradlew test
```

- `ConflictResolverTest` — last-write-wins across missing/tied/newer cases + list merge
- `DateTimeUtilsTest` — day bounds, recurrence stepping, month geometry, overdue detection
