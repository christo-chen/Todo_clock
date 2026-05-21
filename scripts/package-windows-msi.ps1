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
$InstallerDest = Join-Path $RootDir "dist\windows\installer"
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

function Require-Wix {
    if ((Get-Command "candle.exe" -ErrorAction SilentlyContinue) -and (Get-Command "light.exe" -ErrorAction SilentlyContinue)) {
        return
    }
    if (Get-Command "wix.exe" -ErrorAction SilentlyContinue) {
        return
    }
    throw "Missing WiX Toolset. Install WiX Toolset and make sure candle.exe/light.exe or wix.exe is available on PATH before building Windows installers."
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

Require-Windows
Require-Tool "java"
Require-Tool "mvn"
Require-Tool "jpackage"
Require-Wix

& (Join-Path $PSScriptRoot "package-windows-app.ps1")
if ($LASTEXITCODE -ne 0) {
    throw "Windows app image packaging failed."
}

New-Item -ItemType Directory -Force -Path $InstallerDest | Out-Null

Invoke-Native "jpackage" @(
    "--type", "msi",
    "--dest", $InstallerDest,
    "--name", $AppName,
    "--app-version", $AppVersion,
    "--vendor", "Todo Flip Clock",
    "--input", $InputDir,
    "--main-jar", $MainJar,
    "--main-class", $MainClass,
    "--module-path", $JavafxModuleDir,
    "--add-modules", $JavafxModules,
    "--icon", $IconFile,
    "--java-options", "-Dfile.encoding=UTF-8",
    "--win-menu",
    "--win-shortcut",
    "--win-dir-chooser"
)

Write-Host "Created Windows .msi installer in $InstallerDest"
