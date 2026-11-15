# ⚡ Real-Time Log File Monitoring & Analysis System

<p align="center">
  <strong>Monitor. Analyze. Detect. Understand.</strong>
  <br/>
  A Java-based desktop application for real-time log monitoring, intelligent log analysis, and interactive visualization.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Language-Java-orange?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Interface-Java%20Swing-blue?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Monitoring-Real--Time-success?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Status-Active%20Development-brightgreen?style=for-the-badge" />
</p>

---

## ◈ Overview

Logs tell the story of what happens inside a system. But when those logs grow continuously, finding meaningful information becomes difficult.

**Real-Time Log File Monitoring & Analysis System** is a desktop application designed to make that process easier.

It continuously monitors log files, detects new entries, categorizes messages by severity, and presents useful insights through an interactive dashboard.

Instead of manually reading thousands of lines, users can monitor activity, filter events, inspect errors, and understand log patterns from one interface.

## ✦ Key Features

| Feature | Description |
|---|---|
| **Live Log Monitoring** | Detects and reads new log entries as files are updated. |
| **Log Parsing** | Extracts and processes log information using parsing strategies. |
| **Severity Detection** | Identifies DEBUG, INFO, WARN, and ERROR messages. |
| **Interactive Dashboard** | Displays log activity and analytics in a graphical interface. |
| **Log Filtering** | Filters entries to focus on relevant messages and severity levels. |
| **Source Filtering** | Helps narrow down logs based on their source. |
| **Live Analytics** | Presents useful summaries of incoming log activity. |
| **Export Functionality** | Exports log data in text and CSV formats. |
| **Session Management** | Supports saving and loading monitoring sessions. |
| **Report History** | Keeps track of generated reports. |
| **Log Archiving** | Provides an archive mechanism for managing log data. |

## ◈ How It Works

```text
       LOG FILE
           │
           ▼
   FILE MONITORING
      (Java NIO)
           │
           ▼
      LOG PARSER
           │
           ▼
    LOG CLASSIFICATION
   DEBUG / INFO / WARN / ERROR
           │
           ▼
      LOG BUFFER
           │
           ▼
    ANALYSIS ENGINE
           │
           ▼
    INTERACTIVE DASHBOARD
           │
     ┌─────┼──────────┐
     ▼     ▼          ▼
  FILTER  ANALYTICS  EXPORT
                     │
                     ▼
                TEXT / CSV
```

## 🛠️ Technology Stack

- **Java** — Core application development.
- **Java Swing** — Desktop graphical user interface.
- **Java NIO** — File monitoring and change detection.
- **Regular Expressions** — Log pattern recognition and parsing.
- **Multithreading** — Background monitoring and log generation.
- **Collections & Data Structures** — Managing and processing log entries.

## 🏗️ Project Architecture

The application follows a modular package structure:

```text
src/
└── logmonitor/
    ├── model/
    ├── parser/
    ├── monitor/
    ├── analysis/
    ├── util/
    └── ui/
```

| Package | Responsibility |
|---|---|
| `model` | Represents log data and related application models. |
| `parser` | Handles log parsing and parsing strategies. |
| `monitor` | Manages file monitoring and log generation. |
| `analysis` | Processes log data and generates analytical insights. |
| `util` | Provides supporting utility functionality. |
| `ui` | Contains the desktop interface and dashboard. |

## 🚀 Getting Started

### Prerequisites

- Java Development Kit (JDK)
- Git
- An IDE such as IntelliJ IDEA, Eclipse, or Visual Studio Code

### 1. Clone the repository

```bash
git clone https://github.com/sharonmedithi0304/RealTimeLogMonitor.git
```

### 2. Navigate to the project

```bash
cd RealTimeLogMonitor
```

### 3. Open the project

Open the project in your preferred Java IDE and run the application's main entry point.

> **Note:** The exact launch class and build instructions should match the current source tree. Check the project entry point before running.

## 📸 Application Preview

Add actual screenshots of the application here.

| Dashboard | Live Monitoring |
|---|---|
| *Add dashboard screenshot* | *Add live monitoring screenshot* |

| Analytics | Log Filtering |
|---|---|
| *Add analytics screenshot* | *Add filtering screenshot* |

## 🎯 Project Objectives

- Automate the process of monitoring continuously changing log files.
- Reduce the effort required to inspect large volumes of log data.
- Organize log entries according to their severity.
- Provide a centralized interface for monitoring and analysis.
- Make log information easier to filter, review, and export.

## 🔮 Future Enhancements

Potential directions for extending the system:

- Advanced anomaly detection.
- Configurable log alert rules.
- Search with advanced query filters.
- Real-time graphical trend analysis.
- Multi-file monitoring.
- Custom dashboard configurations.
- Additional report formats.

## 👩‍💻 Contributors

**Developed as an academic software project.**

- **Medithi Sharon**
- **Bugidi Sri Harshitha**

## 📄 License

This project is available under the MIT License.

---

<p align="center">
  <strong>Built with Java. Designed for visibility. Made for real-time monitoring.</strong>
  <br/>
  ⭐ If you find this project interesting, consider giving the repository a star!
</p>
