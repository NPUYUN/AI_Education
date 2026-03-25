@echo off
chcp 936 >nul
echo =======================================================
echo          Android Project Deep Clean Script
echo =======================================================
echo.
echo Starting to clean project build files...
echo This will NOT affect your source code or IDE config.
echo.

echo [1/5] Stopping Gradle and Kotlin Daemons...
call gradlew.bat --stop
echo   - Killing any remaining java.exe processes to release file locks...
taskkill /F /IM java.exe /T >nul 2>&1
echo   - Killing any remaining Kotlin compiler processes...
taskkill /F /IM kotlin-daemon.exe /T >nul 2>&1
echo.

echo [2/5] Running gradle clean task...
if exist "gradlew.bat" (
    call gradlew.bat clean
) else (
    echo gradlew.bat not found, skipping standard clean...
)

echo.
echo [3/5] Cleaning root cache directories (.gradle, .kotlin)...
if exist ".gradle" (
    echo   - Deleting .gradle directory...
    rmdir /s /q ".gradle"
)
if exist ".kotlin" (
    echo   - Deleting .kotlin directory...
    rmdir /s /q ".kotlin"
)

echo.
echo [4/5] Cleaning IDE cache directories (.idea)...
if exist ".idea\workspace.xml" (
    echo   - Deleting workspace.xml...
    del /f /q ".idea\workspace.xml"
)
if exist ".idea\caches" (
    echo   - Deleting caches...
    rmdir /s /q ".idea\caches"
)
if exist ".idea\libraries" (
    echo   - Deleting libraries...
    rmdir /s /q ".idea\libraries"
)
if exist ".idea\modules.xml" (
    echo   - Deleting modules.xml...
    del /f /q ".idea\modules.xml"
)
if exist ".idea\navEditor.xml" (
    echo   - Deleting navEditor.xml...
    del /f /q ".idea\navEditor.xml"
)
if exist ".idea\assetWizardSettings.xml" (
    echo   - Deleting assetWizardSettings.xml...
    del /f /q ".idea\assetWizardSettings.xml"
)

echo.
echo [5/5] Scanning and deleting remaining build and .cxx directories...
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
for /d /r . %%d in (captures) do (
    if exist "%%d" (
        echo   - Deleting: %%d
        rmdir /s /q "%%d"
    )
)
for /d /r . %%d in (apk) do (
    if exist "%%d" (
        echo   - Deleting: %%d
        rmdir /s /q "%%d"
    )
)
for /d /r . %%d in (logs) do (
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