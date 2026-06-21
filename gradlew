#!/bin/sh

# Gradle wrapper script
# This script downloads and runs Gradle if not already present

# Determine the Java command to use
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

# Check if Java is available
if ! command -v "$JAVACMD" >/dev/null 2>&1 ; then
    echo "Error: Java is not installed or JAVA_HOME is not set."
    exit 1
fi

# Get the directory of this script
APP_HOME=$(cd "$(dirname "$0")" && pwd)

# Classpath for Gradle wrapper
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# Check if wrapper jar exists
if [ ! -f "$CLASSPATH" ] ; then
    echo "Error: Gradle wrapper jar not found at $CLASSPATH"
    echo "Please download gradle-wrapper.jar and place it in gradle/wrapper/"
    exit 1
fi

# Run Gradle
exec "$JAVACMD" \
    -Xmx256m \
    -Dorg.gradle.appname=gradlew \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
