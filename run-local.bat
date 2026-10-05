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
echo [A] Launch All Microservices in Correct Order (Separate Windows)
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
    echo =========================================================================
    echo Launching Distributed Microservices Architecture in Sequential Order:
    echo 1. Config Server (:8888)
    echo 2. Eureka Service Registry (:8761)
    echo 3. Product Service (:8081)
    echo 4. Order Service (:8082)
    echo 5. Inventory Service (:8083)
    echo 6. Payment Service (:8084)
    echo 7. Notification Service (:8085)
    echo 8. API Gateway (:8080)
    echo 9. Frontend Control Center (:3000)
    echo =========================================================================
    
    echo [1/9] Starting Config Server on port 8888...
    start "Config Server :8888" cmd /k "set JAVA_HOME=C:\Users\abhit\jdks\jdk-17.0.20.1+1 && cd config-server && C:\Users\abhit\maven\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run"
    timeout /t 8 /nobreak >nul

    echo [2/9] Starting Eureka Service Registry on port 8761...
    start "Eureka Registry :8761" cmd /k "set JAVA_HOME=C:\Users\abhit\jdks\jdk-17.0.20.1+1 && cd service-registry && C:\Users\abhit\maven\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run"
    timeout /t 8 /nobreak >nul

    echo [3/9] Starting Product Service on port 8081...
    start "Product Service :8081" cmd /k "set JAVA_HOME=C:\Users\abhit\jdks\jdk-17.0.20.1+1 && cd product-service && C:\Users\abhit\maven\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run"
    timeout /t 6 /nobreak >nul

    echo [4/9] Starting Order Service on port 8082...
    start "Order Service :8082" cmd /k "set JAVA_HOME=C:\Users\abhit\jdks\jdk-17.0.20.1+1 && cd order-service && C:\Users\abhit\maven\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run"
    timeout /t 6 /nobreak >nul

    echo [5/9] Starting Inventory Service on port 8083...
    start "Inventory Service :8083" cmd /k "set JAVA_HOME=C:\Users\abhit\jdks\jdk-17.0.20.1+1 && cd inventory-service && C:\Users\abhit\maven\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run"
    timeout /t 6 /nobreak >nul

    echo [6/9] Starting Payment Service on port 8084...
    start "Payment Service :8084" cmd /k "set JAVA_HOME=C:\Users\abhit\jdks\jdk-17.0.20.1+1 && cd payment-service && C:\Users\abhit\maven\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run"
    timeout /t 6 /nobreak >nul

    echo [7/9] Starting Notification Service on port 8085...
    start "Notification Service :8085" cmd /k "set JAVA_HOME=C:\Users\abhit\jdks\jdk-17.0.20.1+1 && cd notification-service && C:\Users\abhit\maven\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run"
    timeout /t 6 /nobreak >nul

    echo [8/9] Starting API Gateway on port 8080...
    start "API Gateway :8080" cmd /k "set JAVA_HOME=C:\Users\abhit\jdks\jdk-17.0.20.1+1 && cd api-gateway && C:\Users\abhit\maven\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run"
    timeout /t 8 /nobreak >nul

    echo [9/9] Starting Frontend Control Center on port 3000...
    start "Frontend :3000" cmd /k "cd frontend && node server.js"

    echo.
    echo All microservices launched! Access Frontend at: http://localhost:3000
    goto end
)

:end
