@echo off
chcp 936 >nul
echo =======================================================
echo          Android Project Deep Clean Script
echo =======================================================
echo.
echo Starting to clean project build files...
echo This will NOT affect your source code or IDE config.
echo.

echo [1/4] Stopping Gradle and Kotlin Daemons...
call gradlew.bat --stop
taskkill /F /IM java.exe >nul 2>&1
echo.

echo [2/4] Running gradle clean task...
if exist "gradlew.bat" (
    call gradlew.bat clean
) else (
    echo gradlew.bat not found, skipping standard clean...
)

echo.
echo [3/4] Cleaning root cache directories (.gradle, .kotlin)...
if exist ".gradle" (
    echo   - Deleting .gradle directory...
    rmdir /s /q ".gradle"
)
if exist ".kotlin" (
    echo   - Deleting .kotlin directory...
    rmdir /s /q ".kotlin"
)

echo.
echo [4/4] Scanning and deleting remaining build and .cxx directories...
for /d /r . %%d in (build) do (
    if exist "%%d" (
        echo   - Deleting: %%d
        rmdir /s /q "%%d"
    )
)
for /d /r . %%d in (.cxx) do (
    if exist "%%d" (
        echo   - Deleting: %%d
        rmdir /s /q "%%d"
    )
)

echo.
echo =======================================================
echo Clean completed!
echo =======================================================
pause