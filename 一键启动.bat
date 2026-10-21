@echo off
title Heritage Platform - One-Click Start
setlocal enabledelayedexpansion

rem ============================================================
rem  Heritage Platform one-click launcher
rem  Starts: MySQL(3307) - check Redis(6379) - Backend(8080) - Frontend(5173)
rem  To stop everything, run One-Click-STOP.bat in the same folder.
rem  For the Chinese user guide, see doc file 00 in this folder.
rem ============================================================

set "ROOT=%~dp0"
set "TOOLS=%ROOT%..\tools\"
set "JAVAI=%TOOLS%jdk17\bin\java.exe"
set "MVN=%TOOLS%apache-maven-3.9.9\bin\mvn.cmd"
set "SETTINGS=%TOOLS%maven-settings.xml"
set "MYSQLD=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe"
set "JAR=%ROOT%heritage-server\target\heritage-server-1.0.0.jar"
rem Memurai is installed at the D:\ root on this machine (NOT Program Files).
rem Override with the MEMURAI_DIR environment variable if it moves.
if not defined MEMURAI_DIR set "MEMURAI_DIR=D:\"

echo.
echo ============================================================
echo   Heritage Platform - One-Click Start
echo   Project dir: %ROOT%
echo ============================================================
echo.

rem ---------- 1. MySQL (3307) ----------
echo [1/4] Checking MySQL (port 3307) ...
call :port_up 3307
if !PORT_UP! equ 1 (
    echo       MySQL already running, skip
    goto step2
)
if not exist "%MYSQLD%" (
    echo       [ERROR] mysqld not found: "%MYSQLD%"
    echo       Or start it manually with: tools\start-mysql8.bat
    pause
    exit /b 1
)
echo       Starting portable MySQL instance ...
start "Heritage-MySQL" /min "%MYSQLD%" --defaults-file="%TOOLS%mysql8\my.ini" --console
call :wait_port 3307 15
if !PORT_UP! equ 0 (
    echo       [ERROR] MySQL did not start within 30s, check MySQL window
    pause
    exit /b 1
)
echo       MySQL started

rem ---------- 2. Redis (6379, Memurai service) ----------
:step2
echo [2/4] Checking Redis (port 6379) ...
call :port_up 6379
if !PORT_UP! equ 1 (
    echo       Redis already running, skip
    goto step3
)
rem Method 1: Windows service (works if Memurai was registered with --service-install)
net start Memurai >nul 2>&1
ping -n 3 127.0.0.1 >nul
call :port_up 6379

rem Method 2: start via the memurai.exe binary found in MEMURAI_DIR
if !PORT_UP! equ 0 (
    if exist "%MEMURAI_DIR%memurai.exe" (
        echo       Service not available, starting via "%MEMURAI_DIR%memurai.exe" ...
        pushd "%MEMURAI_DIR%"
        memurai.exe --service-start >nul 2>&1
        popd
        ping -n 4 127.0.0.1 >nul
        call :port_up 6379
    ) else (
        echo       [WARN] memurai.exe not found in "%MEMURAI_DIR%"
        echo              Set MEMURAI_DIR to the folder containing memurai.exe
    )
)

if !PORT_UP! equ 1 (
    echo       Redis started
) else (
    echo       [WARN] Redis NOT started. Memurai may need admin rights.
    echo              Start it manually:  cd /d "%MEMURAI_DIR%" ^&^& memurai.exe --service-start
    echo              Note: Memurai (dev build) can auto-shutdown; see doc 00, section 7.
)

rem ---------- 3. Backend (8080) ----------
:step3
echo [3/4] Checking backend heritage-server (port 8080) ...
call :port_up 8080
if !PORT_UP! equ 1 (
    echo       Port 8080 already in use ^(backend may be running^), skip
    goto step4
)
if not exist "%JAR%" (
    echo       jar not found, building now ^(1-2 min first time^) ...
    set "JAVA_HOME=%TOOLS%jdk17"
    call "%MVN%" -s "%SETTINGS%" -f "%ROOT%heritage-server\pom.xml" package -DskipTests -q
    if !errorlevel! neq 0 (
        echo       [ERROR] Maven build failed
        pause
        exit /b 1
    )
)
if not exist "%ROOT%logs" mkdir "%ROOT%logs"
echo       Starting backend ...
start "Heritage-Backend" /min cmd /c ""%JAVAI%" -Dfile.encoding=UTF-8 -jar "%JAR%" > "%ROOT%logs\backend-console.log" 2>&1"
call :wait_port 8080 20
if !PORT_UP! equ 0 (
    echo       [ERROR] Backend did not start within 60s
    echo       Log file: logs\backend-console.log
    pause
    exit /b 1
)
echo       Backend started  (API docs: http://localhost:8080/api/doc.html)

rem ---------- 4. Frontend (5173) ----------
:step4
echo [4/4] Checking frontend heritage-web (port 5173) ...
call :port_up 5173
if !PORT_UP! equ 1 (
    echo       Frontend already running, skip
    goto finish
)
if not exist "%ROOT%heritage-web\node_modules" (
    echo       First run: installing frontend dependencies ...
    pushd "%ROOT%heritage-web"
    call npm install
    if !errorlevel! neq 0 (
        echo       [ERROR] npm install failed, check Node.js installation
        popd
        pause
        exit /b 1
    )
    popd
)
echo       Starting frontend Vite ...
start "Heritage-Frontend" /d "%ROOT%heritage-web" cmd /c npm run dev
call :wait_port 5173 15
if !PORT_UP! equ 1 (
    echo       Frontend started
) else (
    echo       [WARN] Frontend not listening on 5173 within 30s, check its window
)

rem ---------- Open browser ----------
:finish
ping -n 2 127.0.0.1 >nul
start http://127.0.0.1:5173

echo.
echo ============================================================
echo   ALL READY!
echo    Frontend:  http://127.0.0.1:5173
echo    API docs:  http://localhost:8080/api/doc.html
echo    Accounts:  admin / zhangjian / limu / wangtao / user01 / user02
echo    Password:  123456
echo   To stop:    run One-Click-STOP.bat in this folder
echo ============================================================
echo.
pause
exit /b 0

rem ================= subroutines =================

rem Check if a port is listening: result in PORT_UP (1=yes 0=no)
:port_up
set "PORT_UP=0"
netstat -ano | findstr ":%~1 " | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 set "PORT_UP=1"
goto :eof

rem Wait for a port to listen: %1=port %2=max polls (about 2s each)
:wait_port
set /a wp_tries=0
:wait_port_loop
call :port_up %~1
if !PORT_UP! equ 1 goto :eof
set /a wp_tries+=1
if !wp_tries! geq %~2 goto :eof
ping -n 3 127.0.0.1 >nul
goto wait_port_loop
