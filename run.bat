@echo off
chcp 65001 >nul
echo ========================================
echo   AI 模拟面试与能力提升软件
echo   锐捷网络 - 教育行业解决方案
echo ========================================
echo.

REM 自动设置 JAVA_HOME - 使用硬编码路径
set JAVA_HOME=C:\Program Files\Java\jdk-21.0.10
set PATH=%JAVA_HOME%\bin;%PATH%

echo [INFO] JAVA_HOME: %JAVA_HOME%
echo [INFO] Java version:
java -version
echo.

REM Check if API Key is set
if "%LLM_API_KEY%"=="" (
    echo [WARNING] LLM_API_KEY environment variable is not set
    echo Please set the environment variable or configure api-key in application.yml
    echo.
)

echo [INFO] Starting application...
echo.

REM Use IntelliJ IDEA built-in Maven or system Maven
if exist "%IDEA_MVN%" (
    call "%IDEA_MVN%" clean package -DskipTests
) else (
    REM Try using system Maven
    call mvn clean package -DskipTests
    if errorlevel 1 (
        echo.
        echo [ERROR] Maven build failed
        echo Please use IntelliJ IDEA to compile: Right-click project -> Maven -> Compile
        echo Or execute: mvn clean package -DskipTests
        pause
        exit /b 1
    )
)

echo.
echo [INFO] Starting Spring Boot application...
java -jar target/interview-simulation-1.0.0.jar

pause