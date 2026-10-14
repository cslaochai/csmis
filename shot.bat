@echo off
setlocal
set "URL=%~1"
set "OUT=%~2"
set "SIZE=%~3"
if "%OUT%"=="" set "OUT=%~dp0shot.png"
if "%SIZE%"=="" set "SIZE=1440,900"
if "%URL%"=="" set "URL=http://localhost:3100/login"
if exist "%OUT%" del "%OUT%"
set "EDGE=%ProgramFiles(x86)%\Microsoft\Edge\Application\msedge.exe"
if not exist "%EDGE%" set "EDGE=%ProgramFiles%\Microsoft\Edge\Application\msedge.exe"
"%EDGE%" --headless=new --disable-gpu --hide-scrollbars --window-size=%SIZE% --virtual-time-budget=8000 --screenshot="%OUT%" "%URL%"
if not exist "%OUT%" (echo FAILED) else (echo OK %OUT%)
endlocal
