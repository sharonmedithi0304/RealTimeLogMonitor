@echo off
rem Compiles the project and starts it. Extra arguments are passed to Main,
rem e.g.  run.bat console     or     run.bat token
cd /d "%~dp0"
if not exist out mkdir out
javac -sourcepath src -d out src\Main.java
if errorlevel 1 (
    pause
    exit /b 1
)
java -cp out Main %*
