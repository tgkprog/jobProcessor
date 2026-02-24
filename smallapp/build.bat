@echo off
REM Build and Run Script for SmallApp
REM Author: Tushar Kapila (Adapted for SmallApp)

set APP_PROPS=src\main\resources\application.properties
set VER_DATE=src\main\resources\verDate.txt
set JAR_FAT=target\smallapp-0.0.1-SNAPSHOT.jar
set MAIN_CLASS=com.example.smallapp.SmallAppApplication

if "%~1"=="" goto usage

:loop
if "%~1"=="" goto :eof
set arg=%~1

if /I "%arg%"=="c" (
    call :prepare_resources
    echo Running mvn clean...
    mvn clean
) else if /I "%arg%"=="p" (
    call :prepare_resources
    echo Running mvn package...
    mvn package -DskipTests
) else if /I "%arg%"=="r" (
    echo Running jar...
    if exist %JAR_FAT% (
        java -jar %JAR_FAT%
    ) else (
        echo Error: %JAR_FAT% not found. Run "p" first.
    )
) else if /I "%arg%"=="ver" (
    call :usage
    if exist %VER_DATE% (
        for /f "delims=" %%i in (%VER_DATE%) do echo Last Build: %%i
    ) else (
        echo Last Build: No build record found.
    )
    call :print_version
    goto :eof
) else if /I "%arg%"=="ver1" (
    echo Version update not fully implemented in BAT yet
) else (
    echo Unknown parameter: %arg%
    call :usage
)

shift
goto loop

:usage
echo Usage: build.bat [params]
echo Params:
echo   c    : mvn clean
echo   p    : mvn package
echo   r    : run jar
echo   ver  : print usage and version and exit
goto :eof

:prepare_resources
if not exist src\main\resources mkdir src\main\resources
echo %DATE% %TIME% > %VER_DATE%
echo Updated %VER_DATE%
goto :eof

:print_version
if exist %APP_PROPS% (
    for /f "tokens=2 delims==" %%i in ('findstr "app.version=" %APP_PROPS%') do echo Project Version: %%i
) else (
    echo Project Version: 0.0.1
)
goto :eof
