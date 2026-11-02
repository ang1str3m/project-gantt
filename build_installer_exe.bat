@echo off
setlocal EnableExtensions

rem ==============================================
rem Clean Gantrem installer builder
rem 1. Build fresh jar with Maven
rem 2. Compute installer version
rem 3. Build app-image with jpackage
rem 4. Build Windows EXE installer
rem ==============================================

set "APP_NAME=Gantrem"
set "INSTALLER_NAME=Gantrem Installer"
set "MAIN_CLASS=io.angistrem.employeemanagement.Starter"
set "VENDOR=Angistrem"
set "DESCRIPTION=Gantrem Project Management System"
set "COPYRIGHT=Copyright (c) 2026 Angistrem"

set "PROJECT_DIR=C:\Users\angis\Desktop\Project\EmployManagementSystem"
set "DIST_DIR=C:\Users\angis\Desktop\one\dist"
set "OUTPUT_DIR=C:\Users\angis\Desktop\one\output"
set "APP_IMAGE_DIR=%OUTPUT_DIR%\app-image"
set "APP_IMAGE_ROOT=%APP_IMAGE_DIR%\%APP_NAME%"
set "ICON_PATH=%PROJECT_DIR%\src\main\resources\io\angistrem\employeemanagement\img\gantrem.ico"
set "MVN_CMD=mvn"
set "VERSION_FILE=%TEMP%\gantrem_installer_version.txt"
set "VERSION="
set "JAR_NAME="
set "SOURCE_JAR="
set "USE_ICON=0"

echo ==================================================
echo START
echo ==================================================

where %MVN_CMD% >nul 2>nul
if errorlevel 1 (
    echo [!] ERROR: Maven was not found in PATH.
    pause
    exit /b 1
)

where jpackage >nul 2>nul
if errorlevel 1 (
    echo [!] ERROR: jpackage was not found in PATH.
    pause
    exit /b 1
)

echo [1/6] Building fresh project package...
pushd "%PROJECT_DIR%"
call %MVN_CMD% -q clean package -DskipTests
if errorlevel 1 (
    popd
    echo.
    echo [!] ERROR: Maven build failed.
    pause
    exit /b 1
)
popd

echo [2/6] Calculating installer version...
if exist "%VERSION_FILE%" del /f /q "%VERSION_FILE%" >nul 2>nul
powershell -NoProfile -Command ^
  "$pom = [xml](Get-Content '%PROJECT_DIR%\pom.xml');" ^
  "$base = $pom.project.properties.'version.base';" ^
  "if ([string]::IsNullOrWhiteSpace($base)) { $base = '2.3' };" ^
  "$now = Get-Date;" ^
  "$build = (($now.DayOfYear - 1) * 24 * 6) + ($now.Hour * 6) + [math]::Floor($now.Minute / 10);" ^
  "$version = '{0}.{1}' -f $base.Trim(), $build;" ^
  "[System.IO.File]::WriteAllText('%VERSION_FILE%', $version, [System.Text.Encoding]::ASCII)"

if errorlevel 1 (
    echo [!] ERROR: Could not calculate installer version.
    pause
    exit /b 1
)

set /p VERSION=<"%VERSION_FILE%"
if exist "%VERSION_FILE%" del /f /q "%VERSION_FILE%" >nul 2>nul

if not defined VERSION (
    echo [!] ERROR: Empty installer version.
    pause
    exit /b 1
)

set "JAR_NAME="
set "SOURCE_JAR="
for /f "delims=" %%I in ('dir /b /o:-d "%PROJECT_DIR%\target\gantrem-*.jar" 2^>nul ^| findstr /v /i "^original-"') do (
    if not defined JAR_NAME (
        set "JAR_NAME=%%I"
        set "SOURCE_JAR=%PROJECT_DIR%\target\%%I"
    )
)

if not defined JAR_NAME (
    echo [!] ERROR: Built jar was not found in %PROJECT_DIR%\target
    pause
    exit /b 1
)

if exist "%DIST_DIR%" rmdir /s /q "%DIST_DIR%"
if exist "%APP_IMAGE_DIR%" rmdir /s /q "%APP_IMAGE_DIR%"
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

del /f /q "%OUTPUT_DIR%\%APP_NAME%-*.exe" >nul 2>nul
del /f /q "%OUTPUT_DIR%\%INSTALLER_NAME%-*.exe" >nul 2>nul

mkdir "%DIST_DIR%"
mkdir "%APP_IMAGE_DIR%"

copy /y "%SOURCE_JAR%" "%DIST_DIR%\%JAR_NAME%" >nul

echo [3/6] Version detected: %VERSION%
echo [4/6] Building app-image...

if exist "%ICON_PATH%" (
    set "USE_ICON=1"
) else (
    echo [!] WARNING: ICO file not found. Default icon will be used.
)

if "%USE_ICON%"=="1" (
    jpackage ^
      --type app-image ^
      --dest "%APP_IMAGE_DIR%" ^
      --name "%APP_NAME%" ^
      --input "%DIST_DIR%" ^
      --main-jar "%JAR_NAME%" ^
      --main-class %MAIN_CLASS% ^
      --icon "%ICON_PATH%" ^
      --app-version %VERSION% ^
      --vendor "%VENDOR%" ^
      --description "%DESCRIPTION%" ^
      --copyright "%COPYRIGHT%"
) else (
    jpackage ^
      --type app-image ^
      --dest "%APP_IMAGE_DIR%" ^
      --name "%APP_NAME%" ^
      --input "%DIST_DIR%" ^
      --main-jar "%JAR_NAME%" ^
      --main-class %MAIN_CLASS% ^
      --app-version %VERSION% ^
      --vendor "%VENDOR%" ^
      --description "%DESCRIPTION%" ^
      --copyright "%COPYRIGHT%"
)

if errorlevel 1 (
    echo.
    echo [!] ERROR: App-image build failed.
    pause
    exit /b 1
)

if not exist "%APP_IMAGE_ROOT%" (
    echo [!] ERROR: App-image folder not found: %APP_IMAGE_ROOT%
    pause
    exit /b 1
)

echo [5/6] Building EXE installer...
if "%USE_ICON%"=="1" (
    jpackage ^
      --type exe ^
      --dest "%OUTPUT_DIR%" ^
      --name "%APP_NAME%" ^
      --app-image "%APP_IMAGE_ROOT%" ^
      --icon "%ICON_PATH%" ^
      --app-version %VERSION% ^
      --vendor "%VENDOR%" ^
      --description "%DESCRIPTION%" ^
      --copyright "%COPYRIGHT%" ^
      --win-shortcut ^
      --win-menu ^
      --win-dir-chooser ^
      --win-upgrade-uuid "550e8400-e29b-41d4-a716-446655440000"
) else (
    jpackage ^
      --type exe ^
      --dest "%OUTPUT_DIR%" ^
      --name "%APP_NAME%" ^
      --app-image "%APP_IMAGE_ROOT%" ^
      --app-version %VERSION% ^
      --vendor "%VENDOR%" ^
      --description "%DESCRIPTION%" ^
      --copyright "%COPYRIGHT%" ^
      --win-shortcut ^
      --win-menu ^
      --win-dir-chooser ^
      --win-upgrade-uuid "550e8400-e29b-41d4-a716-446655440000"
)

if errorlevel 1 (
    echo.
    echo [!] ERROR: EXE installer build failed.
    pause
    exit /b 1
)

echo [6/6] Finalizing output...
if exist "%OUTPUT_DIR%\%APP_NAME%-%VERSION%.exe" (
    ren "%OUTPUT_DIR%\%APP_NAME%-%VERSION%.exe" "%INSTALLER_NAME%-%VERSION%.exe"
)

echo --------------------------------------------------
echo   FINISHED
echo   Version:    %VERSION%
echo   Source JAR: %SOURCE_JAR%
echo   Installer:  %OUTPUT_DIR%\%INSTALLER_NAME%-%VERSION%.exe
echo   App image:  %APP_IMAGE_ROOT%
echo --------------------------------------------------
pause
