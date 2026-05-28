@echo off
setlocal

set "PROJECT_ROOT=%~dp0.."
set "MAVEN_HOME=%PROJECT_ROOT%\.tools\apache-maven-3.9.9"

if not defined JAVA_HOME (
  set "JAVA_HOME=C:\Users\Lenovo\.jdks\openjdk-23.0.2"
)

set "MAVEN_REPO=%PROJECT_ROOT%\.tools\m2"
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"

call "%MAVEN_HOME%\bin\mvn.cmd" -Dmaven.repo.local="%MAVEN_REPO%" %*
