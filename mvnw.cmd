@echo off
setlocal
if not defined JAVA_HOME (
    if exist "%~dp0.tools\jdk-17.0.12+7" (
        set "JAVA_HOME=%~dp0.tools\jdk-17.0.12+7"
    )
)
if exist "%~dp0.tools\jdk-17.0.12+7\bin" (
    set "PATH=%~dp0.tools\jdk-17.0.12+7\bin;%PATH%"
)
if exist "%~dp0.tools\apache-maven-3.9.6\bin" (
    set "PATH=%~dp0.tools\apache-maven-3.9.6\bin;%PATH%"
)

if exist "%~dp0.tools\apache-maven-3.9.6\bin\mvn.cmd" (
    call "%~dp0.tools\apache-maven-3.9.6\bin\mvn.cmd" %*
) else (
    call mvn %*
)

