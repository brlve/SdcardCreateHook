#!/bin/sh
DIR="$(cd "$(dirname "$0")" && pwd)"
export ANDROID_HOME=/home/workdir/android-sdk
exec java -Xmx1024m -classpath "$DIR/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
