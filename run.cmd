@echo off
rem Builds and starts the game with a JDK 25, whatever JAVA_HOME happens to point at.
rem Without this the build fails with "release version 25 not supported" and you end up
rem running whatever classes were compiled last time.
setlocal

set "JDK="
for /d %%D in ("C:\Program Files\Java\jdk-25*") do set "JDK=%%D"
for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk-25*") do set "JDK=%%D"

if not defined JDK (
  echo No JDK 25 found. Install one or set JAVA_HOME yourself, then rerun.
  exit /b 1
)

set "JAVA_HOME=%JDK%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d "%~dp0"

echo Using %JAVA_HOME%
call mvn -q clean install -DskipTests || exit /b 1
call mvn -q -pl client/desktop exec:exec
