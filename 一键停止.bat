@echo off
title Heritage Platform - One-Click Stop

set "ROOT=%~dp0"
set "MYSQLADMIN=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqladmin.exe"
rem Memurai is installed at the D:\ root on this machine (NOT Program Files).
if not defined MEMURAI_DIR set "MEMURAI_DIR=D:\"

echo.
echo ============================================================
echo   Heritage Platform - One-Click Stop
echo   Stops: Backend(8080) / Frontend(5173) / MySQL(3307) / Redis(6379)
echo ============================================================
echo.

rem ---------- Stop backend (java on 8080) ----------
echo [1/4] Stopping backend (port 8080) ...
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":8080 " ^| findstr "LISTENING"') do (
    echo       killing PID %%p
    taskkill /f /pid %%p >nul 2>&1
)

rem ---------- Stop frontend (node on 5173) ----------
echo [2/4] Stopping frontend (port 5173) ...
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":5173 " ^| findstr "LISTENING"') do (
    echo       killing PID %%p
    taskkill /f /pid %%p >nul 2>&1
)
rem Vite may spawn extra node processes, clean by window title too
taskkill /f /fi "WINDOWTITLE eq Heritage-Frontend*" >nul 2>&1

rem ---------- Stop MySQL (3307) ----------
echo [3/4] Stopping MySQL (port 3307) ...
netstat -ano | findstr ":3307 " | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 (
    if exist "%MYSQLADMIN%" (
        set "MYSQL_PWD=123456"
        "%MYSQLADMIN%" -h127.0.0.1 -P3307 -uroot shutdown >nul 2>&1
        echo       shutdown command sent
    ) else (
        echo       mysqladmin not found, please close the MySQL window manually
    )
) else (
    echo       MySQL not running, skip
)

rem ---------- Stop Redis / Memurai (6379) ----------
echo [4/4] Stopping Redis (port 6379) ...
netstat -ano | findstr ":6379 " | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 (
    if exist "%MEMURAI_DIR%memurai.exe" (
        pushd "%MEMURAI_DIR%"
        memurai.exe --service-stop >nul 2>&1
        popd
        ping -n 3 127.0.0.1 >nul
        call :port_up 6379
        if !PORT_UP! equ 0 (
            echo       Memurai stopped
        ) else (
            echo       service-stop did not work, killing the listener process ...
            for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":6379 " ^| findstr "LISTENING"') do (
                taskkill /f /pid %%p >nul 2>&1
            )
            echo       done
        )
    ) else (
        echo       memurai.exe not found in "%MEMURAI_DIR%", skip
    )
) else (
    echo       Redis not running, skip
)

echo.
echo All stopped.
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
