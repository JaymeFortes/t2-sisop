@echo off
setlocal
set "script=%~1"
if not defined script set "script=%~dp0run.ps1"
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File "%script%"
exit /b %errorlevel%
