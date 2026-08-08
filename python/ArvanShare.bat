@echo off
REM ============================================================
REM  ArvanShare desktop client launcher (Windows)
REM  Double-click to start the app.
REM  Prefers a pre-built portable EXE (dist\ArvanShare.exe);
REM  falls back to running from the Python virtual environment.
REM ============================================================
setlocal
cd /d "%~dp0"

set "EXE=%~dp0dist\ArvanShare.exe"
set "PY=%~dp0.venv\Scripts\python.exe"

REM ---- 1. Prefer the standalone EXE (built via build_exe.bat) -----------
if exist "%EXE%" (
    echo Starting ArvanShare (portable exe)...
    start "" "%EXE%"
    exit /b 0
)

REM ---- 2. Fall back to virtualenv ----------------------------------------
if not exist "%PY%" (
    echo [ERROR] Neither dist\ArvanShare.exe nor a Python virtualenv were found.
    echo.
    echo Option A — build a portable EXE (recommended):
    echo   Double-click build_exe.bat
    echo.
    echo Option B — run from source (needs Python 3.10+^):
    echo   python -m venv .venv
    echo   .venv\Scripts\python -m pip install -r requirements.txt
    echo.
    pause
    exit /b 1
)

REM ---- 3. Check boto3 is installed ----------------------------------------
"%PY%" -c "import boto3" >nul 2>&1
if errorlevel 1 (
    echo [ERROR] boto3 not found in virtualenv.
    echo Run:  .venv\Scripts\python -m pip install -r requirements.txt
    echo.
    pause
    exit /b 1
)

REM ---- 4. Launch from source ----------------------------------------------
echo Starting ArvanShare (from source)...
"%PY%" desktop.py
set "EC=%errorlevel%"
if %EC% NEQ 0 (
    echo.
    echo ArvanShare exited with code %EC%.
    pause
)
endlocal & exit /b %EC%
