Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$RootDir = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$AppName = "Todo Flip Clock"
$AppVersion = "1.0.0"
$MainClass = "com.example.todoflipclock.MainApp"
$MainJar = "todo-flip-clock-1.0-SNAPSHOT.jar"
$JavafxModules = "javafx.controls,javafx.graphics,javafx.base"

$InputDir = Join-Path $RootDir "target\jpackage-windows-input"
$WorkDir = Join-Path $RootDir "target\jpackage-windows-work"
$JavafxModuleDir = Join-Path $WorkDir "javafx-modules"
$AppDest = Join-Path $RootDir "dist\windows\app"
$IconFile = Join-Path $WorkDir "TodoFlipClock.ico"

function Require-Windows {
    if ([System.Environment]::OSVersion.Platform -ne [System.PlatformID]::Win32NT) {
        throw "Windows packaging must be run on Windows. This script only generates Windows artifacts on a Windows machine or VM."
    }
}

function Require-Tool {
    param([string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Missing required tool: $Name. Install it and make sure it is available on PATH."
    }
}

function Invoke-Native {
    param(
        [string]$FilePath,
        [string[]]$Arguments
    )

    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed with exit code ${LASTEXITCODE}: $FilePath $($Arguments -join ' ')"
    }
}

function New-WindowsIcon {
    Require-Tool "javac"
    Require-Tool "java"

    New-Item -ItemType Directory -Force -Path $WorkDir | Out-Null
    $GeneratorFile = Join-Path $WorkDir "IconGenerator.java"

    @'
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import javax.imageio.ImageIO;

public class IconGenerator {
    public static void main(String[] args) throws Exception {
        int size = 256;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setPaint(new GradientPaint(0, 0, new Color(30, 36, 54), size, size, new Color(32, 108, 92)));
        g.fillRoundRect(24, 24, 208, 208, 44, 44);

        g.setColor(new Color(248, 250, 252));
        g.fill(new RoundRectangle2D.Double(54, 63, 148, 73, 11, 11));
        g.setColor(new Color(17, 24, 39));
        g.fill(new RoundRectangle2D.Double(62, 71, 132, 57, 7, 7));
        g.setColor(new Color(248, 250, 252, 72));
        g.setStroke(new BasicStroke(2f));
        g.drawLine(69, 99, 187, 99);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 38));
        String text = "12";
        FontMetrics metrics = g.getFontMetrics();
        int x = 128 - metrics.stringWidth(text) / 2;
        int y = 71 + ((57 - metrics.getHeight()) / 2) + metrics.getAscent();
        g.setColor(new Color(248, 250, 252));
        g.drawString(text, x, y);

        g.setColor(new Color(255, 205, 86));
        g.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(75, 166, 101, 192);
        g.drawLine(101, 192, 181, 147);
        g.dispose();

        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(image, "png", png);
        byte[] data = png.toByteArray();

        try (FileOutputStream out = new FileOutputStream(args[0])) {
            writeShort(out, 0);
            writeShort(out, 1);
            writeShort(out, 1);
            out.write(0);
            out.write(0);
            out.write(0);
            out.write(0);
            writeShort(out, 1);
            writeShort(out, 32);
            writeInt(out, data.length);
            writeInt(out, 22);
            out.write(data);
        }
    }

    private static void writeShort(FileOutputStream out, int value) throws Exception {
        out.write(value & 0xff);
        out.write((value >> 8) & 0xff);
    }

    private static void writeInt(FileOutputStream out, int value) throws Exception {
        out.write(value & 0xff);
        out.write((value >> 8) & 0xff);
        out.write((value >> 16) & 0xff);
        out.write((value >> 24) & 0xff);
    }
}
'@ | Set-Content -Path $GeneratorFile -Encoding UTF8

    Invoke-Native "javac" @($GeneratorFile)
    Invoke-Native "java" @("-Djava.awt.headless=true", "-cp", $WorkDir, "IconGenerator", $IconFile)
}

Require-Windows
Require-Tool "java"
Require-Tool "mvn"
Require-Tool "jpackage"

Remove-Item -Recurse -Force $InputDir, $WorkDir, $AppDest -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $InputDir, $AppDest | Out-Null

Invoke-Native "mvn" @(
    "clean",
    "package",
    "dependency:copy-dependencies",
    "-DincludeScope=runtime",
    "-DoutputDirectory=$InputDir"
)

New-Item -ItemType Directory -Force -Path $JavafxModuleDir | Out-Null
Copy-Item -Path (Join-Path $RootDir "target\$MainJar") -Destination $InputDir

# JavaFX must be resolved as modules by the jpackage-created runtime.
# Maven copies both empty generic JavaFX jars and real Windows classifier jars;
# only the classifier jars belong on the module path.
Get-ChildItem -Path $InputDir -Filter "javafx-*-win*.jar" | Copy-Item -Destination $JavafxModuleDir
Get-ChildItem -Path $InputDir -Filter "javafx-*.jar" | Remove-Item -Force
if (-not (Get-ChildItem -Path $JavafxModuleDir -Filter "javafx-*.jar" -ErrorAction SilentlyContinue)) {
    throw "Missing JavaFX Windows runtime jars. Check Maven dependency resolution for OpenJFX win classifier artifacts."
}

New-WindowsIcon

Invoke-Native "jpackage" @(
    "--type", "app-image",
    "--dest", $AppDest,
    "--name", $AppName,
    "--app-version", $AppVersion,
    "--vendor", "Todo Flip Clock",
    "--input", $InputDir,
    "--main-jar", $MainJar,
    "--main-class", $MainClass,
    "--module-path", $JavafxModuleDir,
    "--add-modules", $JavafxModules,
    "--icon", $IconFile,
    "--java-options", "-Dfile.encoding=UTF-8"
)

Write-Host "Created Windows app image in $AppDest"
