@echo off
REM ============================================================
REM  Gemi Radar Simulasyonu - Windows Calistirma Scripti
REM  Kullanim: run.bat
REM ============================================================

echo Gemi Radar Simulasyonu baslatiliyor...

java ^
    -Djava.library.path=lib ^
    -cp "out;lib\*" ^
    com.radar.Main

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo HATA: Uygulama kapandi. Yukaridaki mesajlara bakin.
    pause
)
