@echo off
REM ============================================================
REM  ArvanShare — build portable Windows EXE (PyInstaller)
REM  Run this once from the python/ directory.
REM  Output:  python/dist/ArvanShare.exe  (~50-80 MB, standalone)
REM ============================================================
setlocal
cd /d "%~dp0"

set "PY=%~dp0.venv\Scripts\python.exe"

REM ---- 1. Check virtualenv -----------------------------------------------
if not exist "%PY%" (
    echo [ERROR] Virtualenv not found.
    echo Run:  python -m venv .venv  ^&^&  .venv\Scripts\python -m pip install -r requirements.txt
    pause & exit /b 1
)

REM ---- 2. Check PyInstaller ----------------------------------------------
"%PY%" -c "import PyInstaller" >nul 2>&1
if errorlevel 1 (
    echo Installing PyInstaller...
    "%PY%" -m pip install "pyinstaller>=6.0,<7" --quiet
)

REM ---- 3. Build ----------------------------------------------------------
echo.
echo Building ArvanShare.exe (this takes ~60 seconds on first run)...
"%PY%" -m PyInstaller ^
    --onefile ^
    --noconsole ^
    --name ArvanShare ^
    --add-data "config.example.ini;." ^
    desktop.py

if errorlevel 1 (
    echo.
    echo [ERROR] Build failed. See output above.
    pause & exit /b 1
)

echo.
echo ============================================================
echo  Build successful!
echo  Output: %~dp0dist\ArvanShare.exe
echo.
echo  On first run the EXE will ask for your ArvanCloud details.
echo  Settings are stored in:
echo    %%LOCALAPPDATA%%\ArvanShare\config.ini  (if it can write there)
echo    or next to ArvanShare.exe
echo ============================================================
pause
endlocal
