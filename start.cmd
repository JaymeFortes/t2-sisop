@echo off
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File "%~dp0run.ps1" -Interativo
exit /b %errorlevel%
