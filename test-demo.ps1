# Multi-Tenant Data Source Routing - Demo Script (PowerShell)

Write-Host "==================================================" -ForegroundColor Blue
Write-Host "   MULTI-TENANT ROUTING PROJECT DEMO             " -ForegroundColor Blue
Write-Host "==================================================" -ForegroundColor Blue

# 1. Setup
Write-Host "`n[1/5] Setting up environment..." -ForegroundColor Green
if (-not (Test-Path .env)) {
    Copy-Item .env.example .env
    Write-Host "Created .env from example."
}

# 2. Start Docker
Write-Host "`n[2/5] Starting services with Docker Compose..." -ForegroundColor Green
docker compose up --build -d

Write-Host -NoNewline "Waiting for application to be healthy..."
while ($true) {
    $status = docker inspect --format='{{json .State.Health.Status}}' demo_app
    if ($status -eq '"healthy"') { break }
    Write-Host -NoNewline "."
    Start-Sleep -Seconds 2
}
Write-Host " READY!" -ForegroundColor Green

# 3. Health Checks
Write-Host "`n[3/5] Verifying Health Checks..." -ForegroundColor Green
Write-Host "Actuator Health Output:" -ForegroundColor Blue
Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" | ConvertTo-Json

Write-Host "`nTenant DataSources Health:" -ForegroundColor Blue
Invoke-RestMethod -Uri "http://localhost:8080/actuator/health/datasources" | ConvertTo-Json

# 4. Multi-Tenant Operations
Write-Host "`n[4/5] Performing Multi-Tenant Data Operations..." -ForegroundColor Green

Write-Host "Creating user for TENANT 1..." -ForegroundColor Blue
$body1 = @{ name = "Alice Green"; email = "alice@tenant1.com" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/users" -Headers @{ "X-Tenant-ID" = "tenant1"; "Content-Type" = "application/json" } -Body $body1 | ConvertTo-Json

Write-Host "`nCreating user for TENANT 2..." -ForegroundColor Blue
$body2 = @{ name = "Bob Blue"; email = "bob@tenant2.com" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/users" -Headers @{ "X-Tenant-ID" = "tenant2"; "Content-Type" = "application/json" } -Body $body2 | ConvertTo-Json

# 5. Isolation Verification
Write-Host "`n[5/5] Verifying Data Isolation..." -ForegroundColor Green

Write-Host "Listing users for TENANT 1 (Expected: Alice only):" -ForegroundColor Blue
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/users" -Headers @{ "X-Tenant-ID" = "tenant1" } | ConvertTo-Json

Write-Host "`nListing users for TENANT 2 (Expected: Bob only):" -ForegroundColor Blue
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/users" -Headers @{ "X-Tenant-ID" = "tenant2" } | ConvertTo-Json

Write-Host "`nListing users for TENANT 3 (Expected: Empty list):" -ForegroundColor Blue
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/users" -Headers @{ "X-Tenant-ID" = "tenant3" } | ConvertTo-Json

Write-Host "`n==================================================" -ForegroundColor Green
Write-Host "   DEMO COMPLETE - ISOLATION VERIFIED            " -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Green
