@echo off
setlocal

set "MAVEN_VERSION=3.9.9"
set "BASE_DIR=%~dp0"
if "%BASE_DIR:~-1%"=="\" set "BASE_DIR=%BASE_DIR:~0,-1%"
set "MAVEN_HOME=%BASE_DIR%\.mvn\apache-maven-%MAVEN_VERSION%"
set "MAVEN_BIN=%MAVEN_HOME%\bin\mvn.cmd"
set "DOWNLOAD_URL=https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip"
set "ZIP_PATH=%BASE_DIR%\.mvn\apache-maven-%MAVEN_VERSION%-bin.zip"

if not defined JAVA_HOME (
  for /f "usebackq delims=" %%I in (`powershell -NoProfile -Command "$java = Get-Command java -ErrorAction SilentlyContinue; if ($java) { Split-Path -Parent (Split-Path -Parent $java.Source) }"`) do set "JAVA_HOME=%%I"
)

if exist "%MAVEN_BIN%" goto RUN_MAVEN

echo Maven %MAVEN_VERSION% not found. Downloading...
if not exist "%BASE_DIR%\.mvn" mkdir "%BASE_DIR%\.mvn"

powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%DOWNLOAD_URL%' -OutFile '%ZIP_PATH%'"
if errorlevel 1 (
  echo Failed to download Maven from %DOWNLOAD_URL%
  exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%ZIP_PATH%' -DestinationPath '%BASE_DIR%\.mvn' -Force"
if errorlevel 1 (
  echo Failed to extract Maven archive.
  exit /b 1
)

del /q "%ZIP_PATH%" >nul 2>nul

:RUN_MAVEN
"%MAVEN_BIN%" %*
endlocal
