@echo off
setlocal enabledelayedexpansion
title Compass Zero - aufs Telefon bringen
cd /d "%~dp0..\.."
set "HANDY=work\fuers-handy"

echo.
echo ===========================================================
echo   COMPASS ZERO - aufs Telefon bringen
echo ===========================================================
echo.

call :finde_sdk
if not defined ANDROID_HOME (
  echo   Kein Android-SDK gefunden. Das ist NICHT schlimm --
  echo   es geht auch ohne, siehe ANLEITUNG.txt: Dateien einfach
  echo   per Kabel in den Ordner "Download" des Telefons kopieren.
  goto :ende
)
set "ADB=%ANDROID_HOME%\platform-tools\adb.exe"

echo   Angeschlossene Geraete:
echo.
"%ADB%" devices
echo.
echo   Steht dort nichts oder "unauthorized":
echo     - Kabel pruefen, Telefon entsperren
echo     - Am Telefon: Entwickleroptionen und USB-Debugging an
echo     - Die Nachfrage am Telefon bestaetigen
echo.

set "ZIEL="
for /f "skip=1 tokens=1,2" %%A in ('"%ADB%" devices') do (
  if "%%B"=="device" (
    if defined ZIEL (
      set "MEHRERE=ja"
    ) else (
      set "ZIEL=%%A"
    )
  )
)
if not defined ZIEL (
  echo   Kein Geraet bereit. Abgebrochen.
  goto :ende
)
if defined MEHRERE (
  echo   Es haengen MEHRERE Geraete dran. Welches soll es sein?
  set /p ZIEL=  Kennung aus der Liste oben: 
)
echo   Ziel: !ZIEL!
echo.
echo   ACHTUNG: Auf DIESES Geraet wird gleich geschrieben.
echo   Ist das das richtige? Dann Enter. Sonst Fenster zumachen.
pause >nul
echo.

if not exist "%HANDY%\compass-zero.apk" (
  echo   Keine APK da. Erst "1 APK BAUEN.bat" laufen lassen.
  goto :ende
)

echo   [1/2] App aufspielen ...
"%ADB%" -s !ZIEL! install -r "%HANDY%\compass-zero.apk"
if errorlevel 1 (
  echo.
  echo   Das Aufspielen ist fehlgeschlagen. Haeufigster Grund: Auf dem
  echo   Telefon liegt eine Fassung mit anderer Unterschrift. Dann die
  echo   alte App am Telefon deinstallieren und noch einmal starten.
  echo   Gemerkte Eintraege gehen dabei verloren.
  goto :ende
)

echo.
echo   [2/2] Kartendateien in den Ordner Download kopieren ...
set "GAB_KARTE="
for %%F in ("%HANDY%\*.czk" "%HANDY%\*.czb" "%HANDY%\*.czh") do (
  if exist "%%F" (
    set "GAB_KARTE=ja"
    echo       %%~nxF  ^(%%~zF Bytes^) -- das dauert
    "%ADB%" -s !ZIEL! push "%%F" /sdcard/Download/
  )
)
if not defined GAB_KARTE echo       Keine Kartendateien da -- die App bringt ihre eigene mit.

echo.
echo ===========================================================
echo   FERTIG.
echo.
echo   Karten muessen in der App noch EINGELESEN werden:
echo     App oeffnen  ^>  Einstellungen  ^>  "Karte einlesen"
echo     ^>  KARTENDATEI AUSWAEHLEN  ^>  Download  ^>  die Datei
echo.
echo   Danach einmal auf einen anderen Reiter und zurueck auf
echo   "Karte" -- sonst steht noch die alte da.
echo ===========================================================
goto :ende

:finde_sdk
if exist "%USERPROFILE%\Android\Sdk\platform-tools\adb.exe" set "ANDROID_HOME=%USERPROFILE%\Android\Sdk"
if not defined ANDROID_HOME if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
exit /b

:ende
echo.
pause
