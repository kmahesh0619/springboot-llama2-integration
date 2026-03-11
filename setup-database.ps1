#!/usr/bin/env powershell
# PostgreSQL Database Setup Script

$psqlPath = "C:\Program Files\PostgreSQL\18\bin\psql.exe"
$dbUser = "postgres"
$dbHost = "localhost"
$dbName = "factory_incidents"
$schemaFile = "d:\SpringBoot Project\AI Based Project\springboot-llama2-integration\database-schema.sql"

Write-Host "═══════════════════════════════════════════════════════════════"
Write-Host "PostgreSQL Database Setup"
Write-Host "═══════════════════════════════════════════════════════════════"
Write-Host ""

# Set password environment variable
if (-not $env:PGPASSWORD) {
    $env:PGPASSWORD = Read-Host -Prompt "Enter PostgreSQL password for '$dbUser'" -AsSecureString
}

try {
    # Test connection
    Write-Host "Testing PostgreSQL connection..." -ForegroundColor Cyan
    $result = & $psqlPath -U $dbUser -h $dbHost -w -c "SELECT version();" 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ PostgreSQL connection successful" -ForegroundColor Green
    } else {
        Write-Host "✗ Failed to connect to PostgreSQL" -ForegroundColor Red
        Write-Host "Error: $result"
        exit 1
    }

    # Create database
    Write-Host ""
    Write-Host "Creating database '$dbName'..." -ForegroundColor Cyan
    & $psqlPath -U $dbUser -h $dbHost -w -c "CREATE DATABASE $dbName;" 2>&1 | Tee-Object -Variable createDbOutput
    
    if ($LASTEXITCODE -eq 0 -or $createDbOutput -match "already exists") {
        Write-Host "✓ Database created or already exists" -ForegroundColor Green
    } else {
        Write-Host "✗ Failed to create database" -ForegroundColor Red
        Write-Host $createDbOutput
        exit 1
    }

    # Apply schema
    Write-Host ""
    Write-Host "Applying schema to database..." -ForegroundColor Cyan
    & $psqlPath -U $dbUser -h $dbHost -w -d $dbName -f $schemaFile 2>&1 | Tee-Object -Variable schemaOutput
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Schema applied successfully" -ForegroundColor Green
    } else {
        Write-Host "⚠ Schema application completed with warnings/errors:" -ForegroundColor Yellow
        Write-Host $schemaOutput
    }

    Write-Host ""
    Write-Host "═══════════════════════════════════════════════════════════════"
    Write-Host "Database setup complete!" -ForegroundColor Green
    Write-Host "═══════════════════════════════════════════════════════════════"
    Write-Host ""
    Write-Host "Connection details:"
    Write-Host "  Host: $dbHost"
    Write-Host "  Database: $dbName"
    Write-Host "  User: $dbUser"
    Write-Host "  Port: 5432"
    Write-Host ""

} catch {
    Write-Host "✗ Error: $_" -ForegroundColor Red
    exit 1
} finally {
    # Clear password from environment
    $env:PGPASSWORD = ""
}
