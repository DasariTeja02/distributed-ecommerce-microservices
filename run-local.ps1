# Distributed E-Commerce Microservices - PowerShell Local Launcher
$env:JAVA_HOME = "C:\Users\abhit\jdks\jdk-17.0.20.1+1"
$mvn = "C:\Users\abhit\maven\apache-maven-3.9.9\bin\mvn.cmd"
$workspace = Split-Path -Parent $MyInvocation.MyCommand.Definition

Write-Host "=========================================================================" -ForegroundColor Cyan
Write-Host "   ZAALIMA DEVELOPERS - DISTRIBUTED E-COMMERCE MICROSERVICES LAUNCHER" -ForegroundColor Cyan
Write-Host "=========================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "[1] Start Frontend Control Center (http://localhost:3000)" -ForegroundColor Green
Write-Host "[2] Start Eureka Service Registry (http://localhost:8761)" -ForegroundColor Yellow
Write-Host "[3] Start Config Server (http://localhost:8888)" -ForegroundColor Yellow
Write-Host "[4] Start API Gateway (http://localhost:8080)" -ForegroundColor Yellow
Write-Host "[5] Start Product Service (http://localhost:8081)" -ForegroundColor Yellow
Write-Host "[6] Start Order Service (http://localhost:8082)" -ForegroundColor Yellow
Write-Host "[7] Start Inventory Service (http://localhost:8083)" -ForegroundColor Yellow
Write-Host "[8] Start Payment Service (http://localhost:8084)" -ForegroundColor Yellow
Write-Host "[9] Start Notification Service (http://localhost:8085)" -ForegroundColor Yellow
Write-Host "[A] Launch Core Services in Separate Windows (Eureka + Config + Gateway + Frontend)" -ForegroundColor Magenta
Write-Host ""

$choice = Read-Host "Enter your choice [1-9 or A]"

switch ($choice.ToUpper()) {
    "1" {
        Set-Location "$workspace\frontend"
        node server.js
    }
    "2" {
        Set-Location "$workspace\service-registry"
        & $mvn spring-boot:run
    }
    "3" {
        Set-Location "$workspace\config-server"
        & $mvn spring-boot:run
    }
    "4" {
        Set-Location "$workspace\api-gateway"
        & $mvn spring-boot:run
    }
    "5" {
        Set-Location "$workspace\product-service"
        & $mvn spring-boot:run
    }
    "6" {
        Set-Location "$workspace\order-service"
        & $mvn spring-boot:run
    }
    "7" {
        Set-Location "$workspace\inventory-service"
        & $mvn spring-boot:run
    }
    "8" {
        Set-Location "$workspace\payment-service"
        & $mvn spring-boot:run
    }
    "9" {
        Set-Location "$workspace\notification-service"
        & $mvn spring-boot:run
    }
    "A" {
        Write-Host "Spawning separate terminal windows for Core Services..." -ForegroundColor Green
        Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$workspace\frontend'; node server.js"
        Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$env:JAVA_HOME = '$env:JAVA_HOME'; Set-Location '$workspace\service-registry'; & '$mvn' spring-boot:run"
        Start-Sleep -Seconds 10
        Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$env:JAVA_HOME = '$env:JAVA_HOME'; Set-Location '$workspace\config-server'; & '$mvn' spring-boot:run"
        Start-Sleep -Seconds 10
        Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$env:JAVA_HOME = '$env:JAVA_HOME'; Set-Location '$workspace\api-gateway'; & '$mvn' spring-boot:run"
    }
    Default {
        Write-Host "Invalid option selected." -ForegroundColor Red
    }
}
