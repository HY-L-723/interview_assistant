@echo off
setlocal

set "DB_HOST=%DB_HOST%"
set "DB_PORT=%DB_PORT%"
set "DB_USERNAME=%DB_USERNAME%"
set "DB_PASSWORD=%DB_PASSWORD%"

if "%DB_HOST%"=="" set "DB_HOST=localhost"
if "%DB_PORT%"=="" set "DB_PORT=3306"
if "%DB_USERNAME%"=="" set "DB_USERNAME=root"
if "%DB_PASSWORD%"=="" set "DB_PASSWORD=root"

mysql -h %DB_HOST% -P %DB_PORT% -u %DB_USERNAME% -p%DB_PASSWORD% < "%~dp0schema.sql"
