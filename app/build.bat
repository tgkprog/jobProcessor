@echo off
REM Build and Run Script for Job Processor
REM Author: Tushar Kapila (Adapted for Windows)

set APP_PROPS=src\main\resources\application.properties
set VER_DATE=src\main\resources\verDate.txt
set JAR_FAT=target\jobProc-1.0.0-fat.jar
set JAR_SLIM=target\jobProc-1.0.0.jar
set MAIN_CLASS=com.sel2in.jobProc.JobProcApp

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
    echo Running mvn install...
    mvn install
) else if /I "%arg%"=="rf" (
    echo Running fat jar...
    if exist %JAR_FAT% (
        java -jar %JAR_FAT%
    ) else (
        echo Error: %JAR_FAT% not found. Run "p" first.
    )
) else if /I "%arg%"=="rs" (
    echo Running slim jar...
    if exist %JAR_SLIM% (
        java -cp "%JAR_SLIM%;target\lib\*" %MAIN_CLASS%
    ) else (
        echo Error: %JAR_SLIM% not found. Run "p" first.
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
echo   p    : mvn install
echo   rf   : run fat jar
echo   rs   : run slim jar
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
    echo Project Version: 1.0.0
)
goto :eof
