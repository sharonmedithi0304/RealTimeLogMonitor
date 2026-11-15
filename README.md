# Real-Time Log File Monitoring and Analysis System

> UI refresh: dark graphite dashboard with high-contrast event severity, compact metric cards, and a dedicated live analytics panel.

A Java desktop application (Swing) that watches a log file while it is being written,
parses every new line, and shows live statistics: counts per level, the busiest sources,
the most frequent errors and an alert when errors arrive in a burst.

**Course:** Java Programming (22ITC02N), Department of Information Technology, CBIT
**Team:** Medithi Sharon (160125737104), Bugidi Sri Harshitha (160125737075)
**Guide:** Mrs. K. Swathi

---

## Features

- Tails `logs/application.log` in a background thread (only new bytes are read, using NIO `FileChannel`)
- Parses lines like `2026-11-15 17:08:01 [ERROR] AuthService - Login failed for user admin`
  (two interchangeable parsers: regular expression and `StringTokenizer`)
- Invalid lines are skipped, counted and reported (the sample log contains two on purpose)
- Live table with colour-coded rows, level filter and text search (lambda-based filters)
- Live counters, per-source table, top-5 error messages, latest errors and **error burst alert**
- Built-in **log generator** thread that simulates a running application, so the demo is truly real-time
- Export a text report, export CSV (asynchronous I/O), save/load the whole session (serialization),
  view saved reports, archive the log file
- Console mode (Scanner commands) for running without the GUI

## Project structure

```
RealTimeLogMonitor/
├── src/
│   ├── Main.java                         entry point (GUI or console)
│   └── logmonitor/
│       ├── model/      LogEntry, LogLevel, LogParseException
│       ├── parser/     LogParser (interface), AbstractLogParser, RegexLogParser, TokenLogParser
│       ├── monitor/    LogMonitor (thread), LogGenerator (thread), LogBuffer, LogListener
│       ├── analysis/   LogAnalyzer, LogFilter (functional interface)
│       ├── util/       Counter<T>, ListUtils, ReportWriter, SessionStore, AsyncExporter, LogArchiver
│       └── ui/         Dashboard (Swing), ConsoleApp
├── logs/
│   ├── application.log                   log file being monitored (grows while the generator runs)
│   └── sample_application.log            untouched copy of the sample data
├── output/                               reports, CSV files, session.dat, archives are written here
├── docs/screenshots/                     put your result screenshots here
├── run.bat / run.sh                      compile and run in one step
└── README.md
```

## How to run

Requirements: **JDK 21** (JDK 17 or newer also works).

**Windows:** double-click `run.bat`, or in a terminal from the project folder: `run.bat`
**Linux / macOS:** `./run.sh`

Manual commands (from the project folder):

```
javac -sourcepath src -d out src/Main.java
java -cp out Main
```

Optional arguments (any order): `console` (text mode), `token` (use the StringTokenizer parser),
or a path to another log file.

In **VS Code**: open the `RealTimeLogMonitor` folder, open `src/Main.java` and press Run.

### Using the dashboard

1. The window opens and starts monitoring automatically; the existing sample entries appear.
2. Click **Start Log Generator**: new lines are appended every ~0.7 s and show up live.
   Occasionally the generator writes a burst of database errors, and STATUS changes to **ERROR BURST**.
3. Use **Level** and **Search** (press Enter) to filter. **Reset** clears the filters.
4. **Stop Monitoring / Start Monitoring** shows the thread life cycle in the status bar
   (`TIMED_WAITING`, `TERMINATED`, ...). Restarting continues from where it stopped.
5. **Export Report**, **Export CSV**, **Save Session**, **Load Session**, **View Reports**, **Archive Log**
   write to / read from the `output/` folder.

To restore the original sample data, copy `logs/sample_application.log` over `logs/application.log`.
Every line of a log file must end with a newline, otherwise the last line waits to be completed.

### Console commands

`stats` | `level <ALL|DEBUG|INFO|WARN|ERROR>` | `tail <n>` | `gen on` | `gen off` | `export` | `quit`

## Where each syllabus topic is used

| Unit | Topic | Where in the project |
|------|-------|----------------------|
| I | Classes, fields, methods, objects, constructors | `LogEntry`, `LogAnalyzer`, `LogMonitor` |
| I | Constructor overloading, access modifiers | `LogEntry` (2 constructors), `LogMonitor`, `LogGenerator`, `Dashboard`; private fields with public getters |
| I | `static` and `final` | `LogEntry.FORMAT`, `final class RegexLogParser`, `final` method `AbstractLogParser.parse`, `ListUtils` |
| I | Arrays, Strings, `StringTokenizer`, `Scanner` | message arrays in `LogGenerator`; `TokenLogParser`; `ConsoleApp` |
| II | Inheritance, `super`, overriding | `AbstractLogParser` -> `RegexLogParser` / `TokenLogParser`; `super(...)`, `super.toString()` |
| II | Preventing inheritance, `Object` class | `final` class and method; `toString`, `equals`, `hashCode` in `LogEntry` |
| II | Dynamic method dispatch, abstract classes | `AbstractLogParser parser` used by `Dashboard`, `ConsoleApp`, `LogMonitor` |
| II | Interfaces, interface vs abstract class | `LogParser`, `LogListener`, `LogFilter` (contracts) vs `AbstractLogParser` (shared state and code) |
| II | Inner classes | `Dashboard.DashboardListener`, `Dashboard.LevelCellRenderer`, `ConsoleApp.ConsoleListener`; anonymous classes for the table model, `WindowAdapter`, `CompletionHandler` |
| II | Packages, CLASSPATH, import | `logmonitor.model/parser/monitor/analysis/util/ui`; `java -cp out Main` |
| III | Exceptions: try, catch, throw, throws, finally | `LogParseException` (custom), `AbstractLogParser.parse`, `SessionStore`, all I/O classes |
| III | Threads, life cycle, creation, priority | `LogMonitor extends Thread`, `LogGenerator implements Runnable`, `setPriority`, state shown in status bar |
| III | Thread synchronization | `synchronized` methods in `LogAnalyzer` and `AbstractLogParser`; `wait()/notifyAll()` in `LogBuffer` (producer-consumer) |
| III | Generics | `Counter<T extends Comparable<T>>`, generic method `ListUtils.lastN` |
| IV | List, Set, Map and their classes | `ArrayList`, `LinkedList`, `HashSet`, `TreeSet`, `HashMap`, `TreeMap` in `LogAnalyzer` and `Counter` |
| IV | `Iterator`, `ListIterator` | `LogAnalyzer.hasErrorBurst`, `latestErrors`; `ListUtils.lastN` |
| IV | `Comparable`, `Comparator` | `LogEntry.compareTo`, `Counter.topN`, `LogAnalyzer.sortedBySeverity` |
| IV | Lambdas, functional interfaces, Java 8 features | `LogFilter` (+ default/static methods), button listeners, streams and method references in `LogAnalyzer` |
| V | `InputStream`, `OutputStream` | `LogArchiver` (byte copy) |
| V | `Reader`, `Writer`, `FileReader`, `FileWriter`, `BufferedReader`, `BufferedWriter` | `ReportWriter`, `LogGenerator` |
| V | Serialization and deserialization | `SessionStore` with `Serializable` `LogEntry` (input filter + validation on load) |
| V | NIO: `Path`, `Files` | `Main`, `ReportWriter`, `SessionStore` |
| V | NIO: Channels and Buffers | `LogMonitor` (`FileChannel` + `ByteBuffer`) |
| V | Asynchronous I/O | `AsyncExporter` (`AsynchronousFileChannel` + `CompletionHandler`) |
| V | NIO vs IO | monitoring and CSV export use NIO; reports and the generator use classic IO |
| V | Selectors | Not used: selectors work with socket channels, and a log file is not a selectable channel. Covered in the report as theory. |

## Uploading to GitHub

```
git init
git add .
git commit -m "Real-Time Log File Monitoring and Analysis System"
git branch -M main
git remote add origin https://github.com/<your-username>/<repository-name>.git
git push -u origin main
```

Create the empty repository on github.com first, then paste its URL in the `git remote` line.
