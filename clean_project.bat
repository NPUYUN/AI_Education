@echo off
chcp 65001 >nul
echo =======================================================
echo          Android 项目深度清理脚本 (安全模式)
echo =======================================================
echo.
echo 开始清理项目构建文件，此操作不会影响您的源代码和 IDE 配置...
echo.

echo [1/3] 正在执行 Gradle 自带清理任务...
if exist "gradlew.bat" (
    call gradlew.bat clean
) else (
    echo 未找到 gradlew.bat，跳过标准清理...
)

echo.
echo [2/3] 正在清理项目根目录缓存 (.gradle, .kotlin)...
if exist ".gradle" (
    echo   - 删除 .gradle 缓存目录...
    rmdir /s /q ".gradle"
)
if exist ".kotlin" (
    echo   - 删除 .kotlin 缓存目录...
    rmdir /s /q ".kotlin"
)

echo.
echo [3/3] 正在深度扫描并强制删除所有模块残留的 build 和 .cxx 目录...
for /d /r . %%d in (build) do (
    if exist "%%d" (
        echo   - 删除残留构建目录: %%d
        rmdir /s /q "%%d"
    )
)
for /d /r . %%d in (.cxx) do (
    if exist "%%d" (
        echo   - 删除残留 C++ 构建目录: %%d
        rmdir /s /q "%%d"
    )
)

echo.
echo =======================================================
echo 清理完成！项目代码和配置文件未受影响。
echo =======================================================
pause
