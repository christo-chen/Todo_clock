# Todo Flip Clock

A small JavaFX desktop application with a card-style digital clock and a simple to-do list.

## Requirements

- Java 17 or newer
- Maven 3.8 or newer

## Run

```bash
mvn javafx:run
```

## Package for macOS

The packaging scripts use `jpackage`, which is included with a full JDK. On macOS,
`sips` and `iconutil` are used to generate a simple `.icns` application icon.
The script copies the macOS JavaFX classifier jars into a dedicated module path
and passes `--add-modules javafx.controls,javafx.graphics,javafx.base` so the
generated runtime contains JavaFX.

Build the macOS `.app` image:

```bash
chmod +x scripts/package-app.sh scripts/package-dmg.sh
./scripts/package-app.sh
```

The generated app is written to:

```text
dist/app/Todo Flip Clock.app
```

Build the `.dmg` installer:

```bash
./scripts/package-dmg.sh
```

The generated installer is written to:

```text
dist/installer/
```

If Maven needs to download dependencies or plugins, the first packaging run may
take longer.

## Package for Windows

Windows installers should be built on Windows with a full JDK that includes
`jpackage`. The app image does not require WiX. The `.exe` and `.msi` installers
require WiX Toolset on `PATH`. The scripts copy the Windows JavaFX classifier
jars into a dedicated module path and pass
`--add-modules javafx.controls,javafx.graphics,javafx.base` so the generated
runtime contains JavaFX.

Required Windows tools:

- Java 17 or newer JDK, with `java`, `javac`, and `jpackage` on `PATH`
- Maven 3.8 or newer, with `mvn` on `PATH`
- WiX Toolset for `.exe` and `.msi` installers, with `candle.exe`/`light.exe` or
  `wix.exe` on `PATH`

Build the Windows app image from Command Prompt:

```bat
scripts\package-windows-app.bat
```

Or from PowerShell:

```powershell
.\scripts\package-windows-app.ps1
```

The generated app image is written to:

```text
dist/windows/app/
```

Build the Windows `.exe` installer from Command Prompt:

```bat
scripts\package-windows-exe.bat
```

Or from PowerShell:

```powershell
.\scripts\package-windows-exe.ps1
```

Build the Windows `.msi` installer from Command Prompt:

```bat
scripts\package-windows-msi.bat
```

Or from PowerShell:

```powershell
.\scripts\package-windows-msi.ps1
```

Windows installers are written to:

```text
dist/windows/installer/
```

## Project Structure

```text
scripts/
├── package-app.sh
├── package-dmg.sh
├── package-windows-app.bat
├── package-windows-app.ps1
├── package-windows-exe.bat
├── package-windows-exe.ps1
├── package-windows-msi.bat
└── package-windows-msi.ps1
src/main/java/com/example/todoflipclock/
├── MainApp.java
├── controller/
│   └── MainController.java
├── model/
│   └── TodoItem.java
├── service/
│   ├── ClockService.java
│   └── TodoService.java
└── storage/
    └── TodoStorage.java
```

## How It Works

- `MainApp.java` starts the JavaFX application.
- `MainController.java` builds and controls the UI directly in Java code. No FXML is used.
- `ClockService.java` updates the current time once per second.
- `TodoService.java` handles adding, completing, deleting, and saving tasks.
- `TodoStorage.java` saves and loads tasks from `~/.todo-flip-clock/tasks.txt`.

The storage file uses a tiny text format to avoid extra dependencies. Task text is Base64 encoded so special characters and separators are handled safely.
