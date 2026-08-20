@echo off
setlocal enabledelayedexpansion
title Compass Zero - Karte fuer ein Land bauen
cd /d "%~dp0..\.."

echo.
echo ===========================================================
echo   COMPASS ZERO - Karte fuer ein Land bauen
echo ===========================================================
echo.

call :finde_python
if not defined PY (
  echo   FEHLT: Python. Ohne das geht es nicht.
  goto :ende
)
call :finde_jdk
call :finde_sdk

if "%~1"=="" (
  echo   Welches Land? Zum Beispiel:  Luxemburg   Slowenien   Kroatien
  echo   Alle Namen sehen:  Liste eingeben
  echo.
  set /p LAND=  Land: 
) else (
  set "LAND=%~1"
)

if /i "!LAND!"=="Liste" (
  echo.
  "%PY%" tools\karte\laender.py
  goto :ende
)
if "!LAND!"=="" goto :ende

echo.
echo   [1/2] Erst rechnen: wie gross wird das, wie lange dauert es?
echo.
"%PY%" tools\karte\land_bauen.py "!LAND!" --nur-rechnen
if errorlevel 1 (
  echo.
  echo   Das Land wurde nicht gefunden oder hat keinen eigenen Auszug.
  echo   Mit "Liste" siehst du alle Namen.
  goto :ende
)

echo.
echo   Wenn dir Groesse und Dauer passen: weiter mit Enter.
echo   Abbrechen: Fenster zumachen.
pause >nul

echo.
echo   [2/2] Bauen. Das laeuft lange -- der Rechner darf dabei
echo         benutzt werden, nur nicht ausschalten.
echo         Ein Abbruch ist nicht schlimm: beim naechsten Start
echo         wird da weitergemacht, wo es aufgehoert hat.
echo.
"%PY%" tools\karte\land_bauen.py "!LAND!" --aus "work\fuers-handy\!LAND!.czk"
if errorlevel 1 goto :fehler

call "%PY%" "tools\handy\stand_schreiben.py"
echo.
echo ===========================================================
echo   FERTIG. Die Datei liegt in work\fuers-handy\ und kann
echo   aufs Telefon. Wie, steht in ANLEITUNG.txt.
echo ===========================================================
goto :ende

:finde_python
set "PY="
for %%P in ("%LOCALAPPDATA%\Programs\Python\Python312\python.exe" "%LOCALAPPDATA%\Programs\Python\Python311\python.exe") do if exist %%P set "PY=%%~P"
if not defined PY where python >nul 2>nul && set "PY=python"
exit /b

:finde_jdk
for /d %%D in ("C:\Program Files\Java\jdk-21*") do set "JAVA_HOME=%%D"
exit /b

:finde_sdk
if exist "%USERPROFILE%\Android\Sdk\platform-tools" set "ANDROID_HOME=%USERPROFILE%\Android\Sdk"
exit /b

:fehler
echo.
echo   ABGEBROCHEN. Beim naechsten Start wird da weitergemacht,
echo   wo es aufgehoert hat -- fertige Zwischenschritte bleiben.

:ende
echo.
pause
