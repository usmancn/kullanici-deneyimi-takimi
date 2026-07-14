@echo off
REM ============================================================
REM  Gemi Radar Simulasyonu - Windows Calistirma Scripti
REM  Kullanim: run.bat
REM ============================================================

set JAVA_CMD=java

rem Varsayilan Java'nin ARM64 (aarch64) olup olmadigini kontrol et
java -XshowSettings:properties -version 2>&1 | findstr /R "os.arch.*aarch64" >nul
if %ERRORLEVEL% == 0 (
    echo [BILGI] Varsayilan Java ARM64 aarch64 mimarisinde.
    echo JOGL kutuphaneleri yalnizca x64 amd64 destekledigi icin alternatif Java araniyor...
    
    if exist "C:\Program Files\ojdkbuild\java-11-openjdk-11.0.15-1\bin\java.exe" (
        set JAVA_CMD="C:\Program Files\ojdkbuild\java-11-openjdk-11.0.15-1\bin\java.exe"
        echo [OK] ojdkbuild x64 Java bulundu ve kullanilacak.
    ) else (
        echo [UYARI] ojdkbuild x64 Java bulunamadi. Uygulama baslatilamayabilir.
    )
)

echo Gemi Radar Simulasyonu baslatiliyor...

%JAVA_CMD% ^
    -Djava.library.path=lib ^
    -cp "out;lib\*" ^
    com.radar.Main

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo HATA: Uygulama kapandi. Yukaridaki mesajlara bakin.
    pause
)
