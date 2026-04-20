@echo off
REM Postavi debug APK pomoci vestaveneho toolchainu v ./tools a nahraje jej
REM na Google Drive (/ZeddiHub App/<versionName>/app-debug.apk).
REM Vypnuti uploadu: set SKIP_UPLOAD=1 a spust znovu.
setlocal

set PROJECT_DIR=%~dp0
set JAVA_HOME=%PROJECT_DIR%tools\jdk17
set ANDROID_HOME=%PROJECT_DIR%tools\android-sdk
set ANDROID_SDK_ROOT=%ANDROID_HOME%
set GRADLE_USER_HOME=%PROJECT_DIR%tools\gradle-cache
set PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%PATH%

echo ==============================================
echo JAVA_HOME      = %JAVA_HOME%
echo ANDROID_HOME   = %ANDROID_HOME%
echo GRADLE_USER_HOME = %GRADLE_USER_HOME%
echo ==============================================

cd /d "%PROJECT_DIR%"
call gradlew.bat :app:assembleDebug %*
if errorlevel 1 goto :eof

set APK_DIR=%PROJECT_DIR%app\build\outputs\apk\debug
set APK_PATH=
for %%f in ("%APK_DIR%\ZeddiHub-App-*.apk") do set APK_PATH=%%f
if "%APK_PATH%"=="" set APK_PATH=%APK_DIR%\app-debug.apk
if not exist "%APK_PATH%" (
    echo [build] APK nenalezeno v %APK_DIR%
    exit /b 1
)
echo [build] APK: %APK_PATH%

if "%SKIP_UPLOAD%"=="1" (
    echo [build] SKIP_UPLOAD=1, preskakuji upload.
    goto :eof
)

where python >nul 2>nul
if errorlevel 1 (
    echo [build] Python nenalezen - preskakuji upload. Spust: set SKIP_UPLOAD=1 ^& build.bat
    goto :eof
)

echo [build] Nahravam APK na Google Drive...
python "%PROJECT_DIR%scripts\upload_apk.py" --apk "%APK_PATH%"

endlocal
