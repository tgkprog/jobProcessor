#!/bin/bash

# Build and Run Script for Job Processor
# Author: Tushar Kapila

APP_PROPS="src/main/resources/application.properties"
VER_DATE="src/main/resources/verDate.txt"
JAR_FAT="target/jobProc-1.0.0-fat.jar"
JAR_SLIM="target/jobProc-1.0.0.jar"
MAIN_CLASS="com.sel2in.jobProc.JobProcApp"

# Ensure resources directory exists
mkdir -p src/main/resources

# On every run: update verDate.txt with current date, time and time zone
date +"%Y-%m-%d %H:%M:%S %Z" > "$VER_DATE"

usage() {
    echo "Usage: ./build.sh [params]"
    echo "Params:"
    echo "  c    : mvn clean"
    echo "  p    : mvn install"
    echo "  rf   : run fat jar"
    echo "  rs   : run slim jar"
    echo "  ver  : print version and usage, then exit"
    echo "  ver1 : increase patch version (x.y.Z)"
    echo "  ver2 : increase minor version (x.Y.z)"
    echo "  ver3 : increase major version (X.y.z)"
}

get_version() {
    if [ -f "$APP_PROPS" ]; then
        grep "app.version=" "$APP_PROPS" | cut -d'=' -f2
    else
        echo "0.0.0"
    fi
}

update_version() {
    local index=$1
    local current_ver=$(get_version)
    IFS='.' read -r -a parts <<< "$current_ver"
    
    # Fill defaults if missing
    for i in 0 1 2; do
        if [ -z "${parts[$i]}" ]; then parts[$i]=0; fi
    done

    ((parts[$index]++))
    
    # Reset lower parts for semantic versioning
    if [ "$index" -eq 0 ]; then
        parts[1]=0
        parts[2]=0
    elif [ "$index" -eq 1 ]; then
        parts[2]=0
    fi

    local new_ver="${parts[0]}.${parts[1]}.${parts[2]}"
    
    if [ ! -f "$APP_PROPS" ]; then
        echo "app.version=$new_ver" > "$APP_PROPS"
    else
        if grep -q "app.version=" "$APP_PROPS"; then
            sed -i "s/app.version=.*/app.version=$new_ver/" "$APP_PROPS"
        else
            echo "app.version=$new_ver" >> "$APP_PROPS"
        fi
    fi
    echo "Updated version to: $new_ver"
}

if [ $# -eq 0 ]; then
    usage
    exit 0
fi

for arg in "$@"; do
    case $arg in
        c)
            echo "Running mvn clean..."
            mvn clean
            ;;
        p)
            echo "Running mvn install..."
            mvn install
            ;;
        rf)
            echo "Running fat jar..."
            if [ -f "$JAR_FAT" ]; then
                java -jar "$JAR_FAT"
            else
                echo "Error: $JAR_FAT not found. Run 'p' first."
            fi
            ;;
        rs)
            echo "Running slim jar..."
            if [ -f "$JAR_SLIM" ]; then
                java -cp "$JAR_SLIM:target/lib/*" "$MAIN_CLASS"
            else
                echo "Error: $JAR_SLIM not found. Run 'p' first."
            fi
            ;;
        ver)
            echo "Project Version: $(get_version)"
            usage
            exit 0
            ;;
        ver1)
            update_version 2
            ;;
        ver2)
            update_version 1
            ;;
        ver3)
            update_version 0
            ;;
        *)
            echo "Unknown parameter: $arg"
            usage
            ;;
    esac
done
