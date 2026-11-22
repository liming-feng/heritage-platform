@echo off
title Heritage - Database only (MySQL 3307)
setlocal enabledelayedexpansion

rem ============================================================
rem  Starts ONLY the project MySQL instance on 127.0.0.1:3307.
rem  Use this when you just want Navicat / Workbench / IDEA to
rem  reach the "heritage" database, without the backend+frontend.
rem  Same logic as step 1 of One-Click-Start.bat.
rem ============================================================

set "ROOT=%~dp0"
set "TOOLS=%ROOT%..\tools\"
set "MYSQLD=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe"
set "MYSQLADMIN=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqladmin.exe"
set "MYSQLINI=%TOOLS%mysql8\my.ini"
set "MYSQLLOG=%TOOLS%mysql8\error.log"
set "MYSQL_USER=root"
set "MYSQL_PWD=123456"

echo.
echo ============================================================
echo   Heritage - start project MySQL only
echo   Instance: 127.0.0.1:3307  db=heritage  user=%MYSQL_USER%
echo ============================================================
echo.

call :port_up 3307
if !PORT_UP! equ 1 (
    echo       MySQL 3307 already running, skip
    goto verify
)

rem Port 3306 is a different instance (XAMPP MariaDB); it has no "heritage" schema.
call :port_up 3306
if !PORT_UP! equ 1 (
    echo       [NOTE] Something is listening on 3306 - that is ANOTHER database
    echo              instance ^(MariaDB/XAMPP^), not this project. Use 3307.
)

rem Method 1: Windows service, if registered once (needs admin).
net start MySQL8Heritage >nul 2>&1
call :port_up 3307

rem Method 2: portable instance driven by tools\mysql8\my.ini
if !PORT_UP! equ 0 (
    if not exist "%MYSQLD%" (
        echo       [ERROR] mysqld.exe not found:
        echo               "%MYSQLD%"
        pause
        exit /b 1
    )
    if not exist "%MYSQLINI%" (
        echo       [ERROR] config file not found:
        echo               "%MYSQLINI%"
        pause
        exit /b 1
    )
    echo       Starting portable MySQL 8 instance on port 3307 ...
    rem Do NOT add --console: it suppresses log-error on Windows, leaving
    rem error.log without any useful content when startup fails.
    start "Heritage-MySQL" /min "%MYSQLD%" --defaults-file="%MYSQLINI%"
    call :wait_port 3307 20
)

:verify
if !PORT_UP! equ 0 (
    echo       [ERROR] MySQL 3307 is NOT listening - the instance failed to start.
    echo               Error log: %MYSQLLOG%
    if exist "%MYSQLLOG%" start "" notepad "%MYSQLLOG%"
    echo               Fallback: double-click tools\start-mysql8.bat and leave
    echo                         its window open.
    pause
    exit /b 1
)

"%MYSQLADMIN%" -h127.0.0.1 -P3307 -u%MYSQL_USER% ping >nul 2>&1
if errorlevel 1 (
    echo       [ERROR] Port 3307 is open but the server refuses connections.
    echo               Error log: %MYSQLLOG%
    if exist "%MYSQLLOG%" start "" notepad "%MYSQLLOG%"
    pause
    exit /b 1
)

echo       MySQL is UP.
echo.
echo   Navicat / Workbench connection:
echo     host 127.0.0.1   port 3307   user root   pass %MYSQL_PWD%   db heritage
echo   Stop it: close the minimized "Heritage-MySQL" window, or run One-Click-STOP.bat
echo.
pause
exit /b 0

rem ================= subroutines =================

:port_up
set "PORT_UP=0"
netstat -ano | findstr ":%~1 " | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 set "PORT_UP=1"
goto :eof

:wait_port
set /a wp_tries=0
:wait_port_loop
call :port_up %~1
if !PORT_UP! equ 1 goto :eof
set /a wp_tries+=1
if !wp_tries! geq %~2 goto :eof
ping -n 3 127.0.0.1 >nul
goto wait_port_loop
