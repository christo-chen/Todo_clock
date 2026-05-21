#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_NAME="Todo Flip Clock"
APP_VERSION="1.0"
PACKAGE_ID="com.example.todoflipclock"
MAIN_CLASS="com.example.todoflipclock.MainApp"
MAIN_JAR="todo-flip-clock-1.0-SNAPSHOT.jar"
JAVAFX_MODULES="javafx.controls,javafx.graphics,javafx.base"

INPUT_DIR="$ROOT_DIR/target/jpackage-input"
WORK_DIR="$ROOT_DIR/target/jpackage-work"
JAVAFX_MODULE_DIR="$WORK_DIR/javafx-modules"
INSTALLER_DEST="$ROOT_DIR/dist/installer"
ICON_FILE="$WORK_DIR/TodoFlipClock.icns"

require_tool() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "Missing required tool: $1" >&2
        exit 1
    fi
}

require_tool jpackage

"$ROOT_DIR/scripts/package-app.sh"

rm -rf "$INSTALLER_DEST"
mkdir -p "$INSTALLER_DEST"

jpackage \
    --type dmg \
    --dest "$INSTALLER_DEST" \
    --name "$APP_NAME" \
    --app-version "$APP_VERSION" \
    --vendor "Todo Flip Clock" \
    --mac-package-identifier "$PACKAGE_ID" \
    --input "$INPUT_DIR" \
    --main-jar "$MAIN_JAR" \
    --main-class "$MAIN_CLASS" \
    --module-path "$JAVAFX_MODULE_DIR" \
    --add-modules "$JAVAFX_MODULES" \
    --icon "$ICON_FILE" \
    --java-options "-Dfile.encoding=UTF-8"

echo "Created DMG in $INSTALLER_DEST"
