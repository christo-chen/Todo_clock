@echo off
setlocal

where powershell >nul 2>nul
if errorlevel 1 (
    echo Missing required tool: powershell. Run this script on Windows with PowerShell installed.
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0package-windows-app.ps1"
exit /b %ERRORLEVEL%
