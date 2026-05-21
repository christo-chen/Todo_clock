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
DIST_DIR="$ROOT_DIR/dist"
APP_DEST="$DIST_DIR/app"
ICON_FILE="$WORK_DIR/TodoFlipClock.icns"

require_tool() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "Missing required tool: $1" >&2
        exit 1
    fi
}

generate_icon() {
    require_tool javac
    require_tool java
    require_tool sips
    require_tool iconutil

    local icon_src="$WORK_DIR/icon-1024.png"
    local iconset="$WORK_DIR/TodoFlipClock.iconset"

    mkdir -p "$WORK_DIR"
    cat > "$WORK_DIR/IconGenerator.java" <<'JAVA'
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class IconGenerator {
    public static void main(String[] args) throws Exception {
        int size = 1024;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setPaint(new GradientPaint(0, 0, new Color(30, 36, 54), size, size, new Color(32, 108, 92)));
        g.fillRoundRect(96, 96, 832, 832, 180, 180);

        g.setColor(new Color(248, 250, 252));
        g.fill(new RoundRectangle2D.Double(216, 252, 592, 292, 42, 42));
        g.setColor(new Color(17, 24, 39));
        g.fill(new RoundRectangle2D.Double(246, 282, 532, 232, 28, 28));
        g.setColor(new Color(248, 250, 252, 72));
        g.setStroke(new BasicStroke(8f));
        g.drawLine(276, 398, 748, 398);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 152));
        String text = "12";
        FontMetrics metrics = g.getFontMetrics();
        int x = 512 - metrics.stringWidth(text) / 2;
        int y = 282 + ((232 - metrics.getHeight()) / 2) + metrics.getAscent();
        g.setColor(new Color(248, 250, 252));
        g.drawString(text, x, y);

        g.setColor(new Color(255, 205, 86));
        g.setStroke(new BasicStroke(34f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(300, 664, 402, 766);
        g.drawLine(402, 766, 724, 588);

        g.dispose();
        ImageIO.write(image, "png", new File(args[0]));
    }
}
JAVA

    javac "$WORK_DIR/IconGenerator.java"
    java -Djava.awt.headless=true -cp "$WORK_DIR" IconGenerator "$icon_src"

    rm -rf "$iconset"
    mkdir -p "$iconset"
    sips -z 16 16 "$icon_src" --out "$iconset/icon_16x16.png" >/dev/null
    sips -z 32 32 "$icon_src" --out "$iconset/icon_16x16@2x.png" >/dev/null
    sips -z 32 32 "$icon_src" --out "$iconset/icon_32x32.png" >/dev/null
    sips -z 64 64 "$icon_src" --out "$iconset/icon_32x32@2x.png" >/dev/null
    sips -z 128 128 "$icon_src" --out "$iconset/icon_128x128.png" >/dev/null
    sips -z 256 256 "$icon_src" --out "$iconset/icon_128x128@2x.png" >/dev/null
    sips -z 256 256 "$icon_src" --out "$iconset/icon_256x256.png" >/dev/null
    sips -z 512 512 "$icon_src" --out "$iconset/icon_256x256@2x.png" >/dev/null
    sips -z 512 512 "$icon_src" --out "$iconset/icon_512x512.png" >/dev/null
    cp "$icon_src" "$iconset/icon_512x512@2x.png"
    iconutil -c icns "$iconset" -o "$ICON_FILE"
}

require_tool mvn
require_tool jpackage

rm -rf "$INPUT_DIR" "$WORK_DIR" "$APP_DEST"
mkdir -p "$INPUT_DIR" "$APP_DEST"

mvn -q clean package dependency:copy-dependencies \
    -DincludeScope=runtime \
    -DoutputDirectory="$INPUT_DIR"
mkdir -p "$JAVAFX_MODULE_DIR"
cp "$ROOT_DIR/target/$MAIN_JAR" "$INPUT_DIR/"

# JavaFX must be resolved as modules by the jpackage-created runtime.
# Maven copies both empty generic JavaFX jars and real macOS classifier jars;
# only the classifier jars belong on the module path.
find "$INPUT_DIR" -name "javafx-*-mac*.jar" -exec cp {} "$JAVAFX_MODULE_DIR/" \;
find "$INPUT_DIR" -name "javafx-*.jar" -delete
if ! find "$JAVAFX_MODULE_DIR" -name "javafx-*.jar" -print -quit | grep -q .; then
    echo "Missing JavaFX macOS runtime jars. Check Maven dependency resolution for OpenJFX mac classifier artifacts." >&2
    exit 1
fi

generate_icon

jpackage \
    --type app-image \
    --dest "$APP_DEST" \
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

echo "Created $APP_DEST/$APP_NAME.app"
