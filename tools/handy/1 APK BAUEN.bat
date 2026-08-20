@echo off
setlocal enabledelayedexpansion
title Compass Zero - APK bauen
cd /d "%~dp0..\.."

echo.
echo ===========================================================
echo   COMPASS ZERO - die App neu bauen
echo ===========================================================
echo.
echo   Das dauert beim ersten Mal ein paar Minuten, danach
echo   meist unter einer Minute. Fenster offen lassen.
echo.

call :finde_jdk
if not defined JAVA_HOME (
  echo   FEHLT: Ein JDK 21. Gesucht wurde unter
  echo          C:\Program Files\Java\jdk-21*
  echo   Ohne das geht es nicht. Herunterladen: adoptium.net
  goto :ende
)
echo   JDK      %JAVA_HOME%

call :finde_sdk
if not defined ANDROID_HOME (
  echo   FEHLT: Das Android-SDK. Gesucht wurde unter
  echo          %USERPROFILE%\Android\Sdk und %LOCALAPPDATA%\Android\Sdk
  echo   Ohne das laesst sich keine APK bauen.
  goto :ende
)
echo   SDK      %ANDROID_HOME%
echo.

set PACKSIGN=tools\packsign\build\install\packsign\bin\packsign.bat
if not exist "%PACKSIGN%" (
  echo   [1/5] Packwerkzeug bauen ...
  call ".\gradlew.bat" :tools:packsign:installDist -q
  if errorlevel 1 goto :fehler
) else (
  echo   [1/5] Packwerkzeug ist da.
)

if not exist "work\devkey\entwicklung.secret" (
  echo.
  echo   FEHLT: Der Unterschriftsschluessel work\devkey\entwicklung.secret
  echo   Ohne ihn kann das Inhaltspaket nicht unterschrieben werden.
  goto :ende
)

echo   [2/5] Inhalt einpacken ...
if exist "work\build\europe-de.zip" del "work\build\europe-de.zip"
if exist "work\build\europe-de.czp" del "work\build\europe-de.czp"
if not exist "work\build" mkdir "work\build"
call "%PACKSIGN%" pack --in content\europe-de\paket --out work\build\europe-de.zip
if errorlevel 1 goto :fehler

echo   [3/5] Unterschreiben und pruefen ...
call "%PACKSIGN%" sign --key work\devkey\entwicklung.secret --in work\build\europe-de.zip --out work\build\europe-de.czp
if errorlevel 1 goto :fehler
call "%PACKSIGN%" verify --keys work\devkey\trust.txt --in work\build\europe-de.czp
if errorlevel 1 goto :fehler

echo   [4/5] App bauen ...
call ".\gradlew.bat" :androidApp:assembleDebug -q
if errorlevel 1 goto :fehler

echo   [5/5] Ablegen und Pruefsumme bilden ...
copy /y "androidApp\build\outputs\apk\debug\androidApp-debug.apk" "work\fuers-handy\compass-zero.apk" >nul
if errorlevel 1 goto :fehler
call :finde_python
if defined PY "%PY%" "tools\handy\stand_schreiben.py"

echo.
echo ===========================================================
echo   FERTIG.
echo.
for %%A in ("work\fuers-handy\compass-zero.apk") do echo   Datei:  %%~fA
for %%A in ("work\fuers-handy\compass-zero.apk") do set /a MB=%%~zA/1048576
echo   Groesse: !MB! MB
echo.
echo   Weiter mit "3 AUFS HANDY.bat" oder die Datei von Hand
echo   auf das Telefon kopieren und dort antippen.
echo ===========================================================
goto :ende

:finde_jdk
for /d %%D in ("C:\Program Files\Java\jdk-21*") do set "JAVA_HOME=%%D"
if not defined JAVA_HOME for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk-21*") do set "JAVA_HOME=%%D"
exit /b

:finde_sdk
if exist "%USERPROFILE%\Android\Sdk\platform-tools" set "ANDROID_HOME=%USERPROFILE%\Android\Sdk"
if not defined ANDROID_HOME if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools" set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
exit /b

:finde_python
set "PY="
for %%P in ("%LOCALAPPDATA%\Programs\Python\Python312\python.exe" "%LOCALAPPDATA%\Programs\Python\Python311\python.exe") do if exist %%P set "PY=%%~P"
if not defined PY where python >nul 2>nul && set "PY=python"
exit /b

:fehler
echo.
echo ===========================================================
echo   ABGEBROCHEN. Der Schritt oben ist fehlgeschlagen.
echo   Die alte APK in work\fuers-handy\ ist unveraendert und
echo   weiter benutzbar.
echo ===========================================================

:ende
echo.
pause
