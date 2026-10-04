@echo off
TITLE Distributed E-Commerce Microservices Launcher
SET JAVA_HOME=C:\Users\abhit\jdks\jdk-17.0.20.1+1
SET MVN_CMD=C:\Users\abhit\maven\apache-maven-3.9.9\bin\mvn.cmd

echo =========================================================================
echo    ZAALIMA DEVELOPERS - DISTRIBUTED E-COMMERCE MICROSERVICES LAUNCHER
echo =========================================================================
echo.
echo Select an option:
echo [1] Start Frontend Control Center (http://localhost:3000)
echo [2] Start Service Registry - Eureka (http://localhost:8761)
echo [3] Start Config Server (http://localhost:8888)
echo [4] Start API Gateway (http://localhost:8080)
echo [5] Start Product Service (http://localhost:8081)
echo [6] Start Order Service (http://localhost:8082)
echo [7] Start Inventory Service (http://localhost:8083)
echo [8] Start Payment Service (http://localhost:8084)
echo [9] Start Notification Service (http://localhost:8085)
echo [A] Launch Core Services in Separate Windows (Eureka + Config + Gateway + Frontend)
echo.
set /p opt="Enter choice [1-9 or A]: "

if /I "%opt%"=="1" (
    echo Starting Frontend on http://localhost:3000 ...
    cd frontend
    node server.js
    pause
    goto end
)
if /I "%opt%"=="2" (
    echo Starting Eureka Service Registry on port 8761 ...
    cd service-registry
    call "%MVN_CMD%" spring-boot:run
    pause
    goto end
)
if /I "%opt%"=="3" (
    echo Starting Config Server on port 8888 ...
    cd config-server
    call "%MVN_CMD%" spring-boot:run
    pause
    goto end
)
if /I "%opt%"=="4" (
    echo Starting API Gateway on port 8080 ...
    cd api-gateway
    call "%MVN_CMD%" spring-boot:run
    pause
    goto end
)
if /I "%opt%"=="5" (
    echo Starting Product Service on port 8081 ...
    cd product-service
    call "%MVN_CMD%" spring-boot:run
    pause
    goto end
)
if /I "%opt%"=="6" (
    echo Starting Order Service on port 8082 ...
    cd order-service
    call "%MVN_CMD%" spring-boot:run
    pause
    goto end
)
if /I "%opt%"=="7" (
    echo Starting Inventory Service on port 8083 ...
    cd inventory-service
    call "%MVN_CMD%" spring-boot:run
    pause
    goto end
)
if /I "%opt%"=="8" (
    echo Starting Payment Service on port 8084 ...
    cd payment-service
    call "%MVN_CMD%" spring-boot:run
    pause
    goto end
)
if /I "%opt%"=="9" (
    echo Starting Notification Service on port 8085 ...
    cd notification-service
    call "%MVN_CMD%" spring-boot:run
    pause
    goto end
)
if /I "%opt%"=="A" (
    echo Launching Core Services in separate windows...
    start "Frontend :3000" cmd /k "cd frontend && node server.js"
    start "Eureka :8761" cmd /k "set JAVA_HOME=C:\Users\abhit\jdks\jdk-17.0.20.1+1 && cd service-registry && C:\Users\abhit\maven\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run"
    timeout /t 10
    start "Config Server :8888" cmd /k "set JAVA_HOME=C:\Users\abhit\jdks\jdk-17.0.20.1+1 && cd config-server && C:\Users\abhit\maven\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run"
    timeout /t 10
    start "API Gateway :8080" cmd /k "set JAVA_HOME=C:\Users\abhit\jdks\jdk-17.0.20.1+1 && cd api-gateway && C:\Users\abhit\maven\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run"
    goto end
)

:end
