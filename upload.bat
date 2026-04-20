@echo off
REM Samostatne nahraje posledni debug APK na Google Drive.
setlocal
set PROJECT_DIR=%~dp0
python "%PROJECT_DIR%scripts\upload_apk.py" %*
endlocal
