# Script para iniciar el servidor de sincronización y Room SQLite en Localhost
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  Iniciando Truequapp Server en Localhost (Puerto 8080)..." -ForegroundColor Yellow
Write-Host "============================================================" -ForegroundColor Cyan
$PSScriptRoot = Split-Path -Parent -Path $MyInvocation.MyCommand.Definition
python "$PSScriptRoot\server.py" 8080

