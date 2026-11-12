@echo off
echo ====================================================
echo  Building Expense Tracker with Java 17 and TeaVM
echo ====================================================
call "%~dp0mvnw.cmd" clean package
echo.
echo Build complete! Static site generated in /dist directory.

