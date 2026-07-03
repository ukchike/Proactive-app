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
├── sync/
│   ├── ConflictResolver.kt       # Last-write-wins merge (unit-tested)
│   └── SyncWorker.kt             # WorkManager periodic sync (Drive upload stubbed — see below)
├── util/DateTimeUtils.kt         # Date/recurrence helpers (unit-tested)
└── ui/
    ├── AppRoot.kt                # Bottom nav + NavHost
    ├── theme/                    # Color, Type, Theme (per-module accents, dark mode)
    ├── home/                     # Unified dashboard (today's events + due/overdue tasks)
    ├── notes/                    # Notes list + editor
    ├── reminders/                # Reminders list, smart-list chips, add dialog
    └── calendar/                 # Month grid + day agenda, add-event dialog
```

## Features implemented

**Notes**
- Create / edit / delete (soft delete), pin to top
- Folders + folder filter chips
- Full-text search across title & content
- Tags, and specialized note *types* (meeting, research, journal, financial) in the model
- Markdown/plain-text content (rich-text WYSIWYG is a later phase)

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
- Event → pre-filled, bidirectionally linked meeting note

## Implementation status

| Area | State |
| --- | --- |
| Local data layer (Room) for all 3 modules | ✅ Complete |
| Notes / Reminders / Calendar UI | ✅ Functional (MVP scope) |
| Home unified dashboard | ✅ Functional |
| Cross-module linking | ✅ Functional |
| Conflict resolution (last-write-wins) | ✅ Implemented + unit-tested |
| Date/recurrence utilities | ✅ Implemented + unit-tested |
| WorkManager sync job | ⚙️ Scheduled; Drive transport stubbed |
| **Google Drive OAuth + upload/download** | ⛔ Not wired — needs OAuth client credentials |
| Rich-text editor, OCR, version history, geofencing | ⛔ Future phases (modeled where relevant) |

### Wiring up Google Drive sync

The merge machinery is done; the remaining step is the network transport. In
`SyncWorker.doWork()`:

1. Add a Drive OAuth client ID (`drive.appdata` + `userinfo.email` scopes).
2. Pull `sync/{notes,reminders,calendar}.json` from the Drive *appDataFolder*.
3. `ConflictResolver.merge(local, remote)` per entity type.
4. Push the merged state back up; keep daily backups under `backup/`.

Soft deletes (`deletedAt`) already flow through every entity so deletions
propagate across devices.

## Design decisions (answers to the spec's open questions)

- **Calendar view:** Month view first (most useful for planning); Day/Week are next.
- **Notes formatting:** Plain-text/Markdown for the MVP; rich-text WYSIWYG deferred.
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
