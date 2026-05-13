@REM Maven Wrapper - Bootstrap script
@REM Downloads and runs Maven
@echo off
setlocal

set "WRAPPER_JAR_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
set "MAVEN_DIST_URL=https://dlcdn.apache.org/maven/maven-3/3.9.12/binaries/apache-maven-3.9.12-bin.zip"
set "WRAPPER_JAR=%~dp0.mvn\wrapper\maven-wrapper.jar"
set "WRAPPER_PROPERTIES=%~dp0.mvn\wrapper\maven-wrapper.properties"
set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.12\maven"

REM Create directories if they don't exist
if not exist "%~dp0.mvn\wrapper" mkdir "%~dp0.mvn\wrapper"
if not exist "%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.12" mkdir "%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.12"

REM Check if Maven is already downloaded
if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo Downloading Apache Maven 3.9.12...
    powershell -Command "& { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%MAVEN_DIST_URL%' -OutFile '%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.12\maven.zip' }"
    echo Extracting Maven...
    powershell -Command "& { Expand-Archive -Path '%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.12\maven.zip' -DestinationPath '%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.12' -Force }"
    ren "%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.12\apache-maven-3.9.12" maven
    del "%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.12\maven.zip"
)

REM Run Maven with all arguments
"%MAVEN_HOME%\bin\mvn.cmd" %*
