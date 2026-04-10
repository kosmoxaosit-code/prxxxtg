@echo off
setlocal
set SCRIPT_DIR=%~dp0
powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%build-apk.ps1" %*
if errorlevel 1 (
  echo Build failed.
  exit /b 1
)
echo Build completed successfully.
exit /b 0
