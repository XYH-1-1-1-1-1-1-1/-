@echo off
chcp 65001 >nul
echo ========================================
echo   AI 模拟面试与能力提升软件
echo   锐捷网络 - 教育行业解决方案
echo ========================================
echo.

REM 检查 JAVA_HOME
if "%JAVA_HOME%"=="" (
    echo [错误] 未设置 JAVA_HOME 环境变量
    echo 请确保已安装 JDK 17 或更高版本
    pause
    exit /b 1
)

echo [信息] Java 版本:
java -version
echo.

REM 检查是否设置了 API Key
if "%LLM_API_KEY%"=="" (
    echo [警告] 未设置 LLM_API_KEY 环境变量
    echo 请设置环境变量后重新运行
    echo 或者在 application.yml 中直接配置 api-key
    echo.
)

echo [信息] 正在启动应用...
echo.

REM 使用 IntelliJ IDEA 内置 Maven 编译
if exist "%IDEA_MVN%" (
    call "%IDEA_MVN%" clean package -DskipTests
) else (
    REM 尝试使用系统 Maven
    mvn clean package -DskipTests
    if errorlevel 1 (
        echo.
        echo [错误] Maven 编译失败
        echo 请使用 IntelliJ IDEA 右键项目 -> Maven -> Compile 进行编译
        echo 或者在命令行执行：mvn clean package -DskipTests
        pause
        exit /b 1
    )
)

echo.
echo [信息] 启动 Spring Boot 应用...
java -jar target/interview-simulation-1.0.0.jar

pause