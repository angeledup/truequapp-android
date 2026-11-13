@echo off
title Truequapp Localhost Server
echo ============================================================
echo   Iniciando Truequapp Server en Localhost (Puerto 8080)...
echo ============================================================
python "%~dp0server.py" 8080
pause

