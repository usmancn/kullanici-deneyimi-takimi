@echo off
REM ============================================================
REM  Gemi Radar Simulasyonu - Windows Derleme Scripti
REM  Kullanim: compile.bat
REM ============================================================

echo [1/3] out/ klasoru temizleniyor ve olusturuluyor...
if exist out rmdir /s /q out
mkdir out

echo [2/3] Kaynak dosyalar listeleniyor...
dir /s /b src\*.java > sources.txt

echo [3/3] Derleniyor (Java 11)...
javac -encoding UTF-8 -cp "lib\*" --release 11 -d out @sources.txt

if %ERRORLEVEL% == 0 (
    echo.
    echo ============================================
    echo  DERLEME BASARILI! "run.bat" ile calistirin.
    echo ============================================
) else (
    echo.
    echo ============================================
    echo  DERLEME HATASI! Yukaridaki mesajlara bakin.
    echo ============================================
)

del sources.txt
pause
