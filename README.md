# TNS GC Emulator

Java emulator of the Czech TNS GC computer from JZD Agrokombinat Slusovice.
It is a standalone Swing desktop application, not a browser applet. It emulates
a Z80-based machine with banked memory, graphics, keyboard, floppy drives, and
an optional hard-disk image interface, allowing original TNS software to run
from disk images.

## Requirements

- A JDK to build the program. The project targets Java 8 source and bytecode;
	newer JDKs can issue warnings about this older target.
- Apache Ant 1.8.0 or later; a current Ant release is recommended.
- A graphical desktop to run Swing. Under WSL, use WSLg or a configured display
	server. A headless terminal alone cannot show the emulator window.
- A bootable TNS system disk image. Creating a blank image does not install an
	operating system.

No third-party Java libraries are declared in the application's compile
classpath. Swing and AWT are included in the JDK. Maven and Gradle are not used.

VS Code can build this project through its terminal without NetBeans installed.
The Java Extension Pack is optional for editing and debugging. Keep
[build.xml](build.xml) and the [nbproject](nbproject) directory together: the
project uses the NetBeans-generated Ant build scripts.

## Build and Run

Run commands from the repository root, not from the distribution directory.
For a native Java/Ant installation with both tools on `PATH`:

```sh
java -version
javac -version
ant -version
ant jar
java -jar dist/TNS.jar
```

`ant jar` performs an incremental build and produces [dist/TNS.jar](dist/TNS.jar).
Use `ant clean jar` for a clean rebuild. The bare `ant` command also invokes
the project's test and Javadoc targets; `ant jar` is the normal packaging command.
The main class is `gui.FTns`.

For the Windows/WSL setup used during development, with Ant installed as a snap:

```powershell
wsl /snap/bin/ant jar
wsl /snap/bin/ant run
```

`ant run` compiles and starts the application from the build output. Alternatively,
run the JAR with a Java runtime installed in the same environment as your disk
image paths. A running emulator does not automatically load newly compiled code;
close and relaunch it after rebuilding.

## Booting a System Disk

1. Open the emulator and click the yellow folder icon, the first toolbar button.
2. Mount a bootable system image in the first floppy slot. The supplied
	 [testy.8sd](testy.8sd) was used during reset/boot verification. Other supplied
	 images are [TNS1.8sd](TNS1.8sd), [TNS2.8sd](TNS2.8sd), and
	 [tnsdemo.8sd](tnsdemo.8sd); their contents may differ.
3. Close the drive dialog and focus the emulator window.
4. At the green boot selector, press `F`, then `0` on the numeric keypad with
	 NumLock enabled. `Ctrl+0` is an alternative for entering the zero using the
	 top-row key with the current keyboard mapping.
5. The display turns yellow and shows the guest operating system's boot messages.
	 Wait until the command prompt appears at the bottom:

```text
00.A>
```

This prompt means the CCP is ready for commands as user 0 on drive A. For
example, type `dir` and press Enter to list files. The yellow system banner or
successful FDC reads alone do not establish that boot has completed.

The green selector, yellow messages, and exact prompt described here apply to
the tested BGD/system-disk setup. Other ROM, video, or disk software combinations
can display different startup screens. The guest OS determines drive letters;
the four emulator floppy slots should not be assumed to have fixed CP/M letters
in every system configuration.

## Toolbar and Execution

Controls appear left to right below the window title:

| Control | Behavior |
| --- | --- |
| Yellow folder | Opens the drive-image dialog. The current tooltip says "Open tape for Load", but this button manages floppy and HDD images. |
| Red power-style circle | Reset: discards the current emulated machine and constructs a fresh one. |
| Blue NMI control | Delivers a non-maskable interrupt to the current CPU. The response depends on guest software; it is not a cold reset. |
| Run/Pause toggle | Stops or resumes emulation without clearing machine state. |
| Settings tools icon | Opens RAM and video settings. |
| Four drive indicators | Show which floppy drive is selected; they are not proof that a read or write succeeded. |

Opening the drive or settings dialog temporarily pauses execution. Closing the
dialog restores the previous running/paused state. Closing the main window
shuts down the machine and releases its disk-image handles.

### Reset Is a Fresh Machine

The red Reset button now follows the same construction path as initial startup
instead of attempting to clear selected registers on an existing machine:

- Stop the old machine's scheduled execution and release its floppy/HDD handles.
- Detach the old keyboard listener and terminate the old machine thread.
- Construct new CPU, RAM, clock, video, keyboard, floppy, and HDD components,
	including fresh buffers, mappings, interrupt state, and framebuffer.
- Reconnect the new machine to the existing screen and drive indicators.
- Start emulation at the boot selector, even if the old machine was paused.

The GUI stays open. Current settings and mounted image paths are retained, but
RAM contents, running programs, and RAM-disk data are discarded. Disk-image
contents are not reverted: changes already written by the guest remain on disk.
Enter `F` and keypad `0` again to boot; Reset does not automatically select a
boot device or restore a running session.

This change resolved the reported case where the first boot worked but subsequent
GUI resets produced disk errors. Repeated boots to the CCP prompt after pressing
Reset have been confirmed by the user. It recreates the emulated hardware within
the same Java process; it does not restart the JVM or the entire GUI.

## Disks and Persistent Configuration

The drive dialog provides four floppy slots and one HDD slot, with controls to
open, eject, or create images. The currently selected floppy slot cannot be
changed through the dialog while in use. After Reset, drives are deselected
until the guest selects them again.

The floppy chooser accepts `.img`, `.8sd`, and `cpm`-matching filenames. These
are treated as raw sector images, not automatically detected filesystem formats.
The default per-drive geometry is 77 tracks, 26 sectors per track, 128 bytes per
sector, and one side: 256,256 bytes per floppy image. New floppy images are
filled with `0xE5`. The HDD chooser uses `.hdd` images; newly created HDD images
contain 2,610 tracks of 64 sectors of 128 bytes, also filled with `0xE5`.

**Use working copies of important images.** Controllers open images read/write;
guest commands can modify the original files. The emulator does not provide an
image snapshot, rollback, or a write-protect switch in the drive dialog.

Settings and media selections are saved in a Java properties file named
`tns.config`. It stores `DRIVE1` through `DRIVE4`, `HDD`, `RAMSIZE`, `VIDEOBW`,
and `VIDEOITK`. Empty drive values mean no image is mounted.

- Direct JAR launches use a config beside the JAR by default.
- Ant launches use the repository-root config through `tns.config.path`.
- Before `clean`, the build copies a newer distribution config to the root;
	after JAR creation, it copies a newer root config back to the distribution.
	The root copy survives deletion of build output and is ignored by Git.
- These are timestamp-based copies, not a live settings synchronization service.
	Avoid running multiple instances that save different configurations concurrently.

Choose an explicit config file to share settings between launch methods:

```sh
java -Dtns.config.path=/absolute/path/to/tns.config -jar dist/TNS.jar
```

Keep the config location writable. Saved image paths must be valid in the
environment running Java: a WSL path such as `/mnt/c/...` is not a native Windows
Java path. Switching between WSL Java and Windows Java may require selecting the
images again or using separate config files.

## Settings, Video, and Keyboard

The settings dialog offers 64, 256, 512, 768, or 1024 KB RAM, BGD or ITK video,
and a black-and-white video option. Defaults are 256 KB, BGD, and color.
RAM/video-adapter changes request an in-place hardware reset; this settings
path is distinct from the red toolbar button's full machine recreation. Use
the red Reset button afterwards when a complete cold start is needed.

The Swing screen displays a 640-by-256 image with doubled vertical scaling to
640-by-512. Execution is scheduled at 20 ms frame intervals, with a budget of
40,000 emulated CPU T-states per frame. Host performance and console tracing
can affect responsiveness; this is not a guarantee of wall-clock timing accuracy.

Give the emulator window keyboard focus before typing. Special TNS keys are:

| TNS Key | Host Key |
| --- | --- |
| START | Home |
| STOP | End |
| ANO | Page Up |
| NE | Page Down |
| Vezmi / Enter | Enter |

The keyboard uses explicit key-code tables rather than a generic text-input
field. Shift, Ctrl, Caps Lock, Scroll Lock, and numeric-keypad location affect
translation. For boot selection, use NumLock keypad `0` or the `Ctrl+0`
alternative above; an unmodified top-row `0` is not equivalent in every layout.

## Diagnostics and Verification

FDC tracing is opt-in. To trace a JAR launch:

```sh
java -Dtns.fdc.trace=true -jar dist/TNS.jar
```

For Windows PowerShell with WSL, preserving the root config path:

```powershell
wsl /snap/bin/ant "-Drun.jvmargs=-Dtns.config.path=/mnt/c/Ikon/tns/emu/TNS/tns.config -Dtns.fdc.trace=true" run
```

Adjust the absolute path for your checkout. Quote the whole `-D` argument in
PowerShell. FDC trace track/sector values are hexadecimal: `track=1A` means
track 26. Logs include seek/read commands, drive-selection changes, status
transitions, host-file read offsets/counts, and completed non-buffered sector
transfers. They help diagnose controller activity but do not prove CCP readiness.
Unhandled-port output is separately enabled with `-Dtns.port.trace=true` and
can be very verbose; leave it disabled for normal use.

The bundled ROM files are loaded from an external `roms` directory relative to
the working directory when available, with classpath resources as fallback.
The current loader checks against 8 KB pages even though the bundled ROM files
are smaller, so startup can print ROM-size warnings. Such warnings have occurred
during successful boots, but missing resources or a stalled machine still need
investigation. Do not pad or replace ROM images just to suppress the messages.

The dependency-free GUI lifecycle regression test uses temporary media/config
files and exercises the actual Reset button three times:

```powershell
wsl /snap/bin/ant "-Drun.class=gui.ResetLifecycleTest" "-Djavac.includes=gui/ResetLifecycleTest.java" run-test-with-main
```

Expected result:

```text
PASS: three GUI resets create fresh machines and release old resources
BUILD SUCCESSFUL
```

It checks fresh machine components, screen/keyboard rebinding, retained media
selections, closed image handles, cancelled timers, and terminated old threads.
It requires a graphical environment and does not boot a real guest OS.

For an end-to-end check, boot to `00.A>`, run `dir`, click the red Reset button,
enter `F` plus keypad `0` at the green selector, and verify that `00.A>` and
`dir` work again. Repeat to check consecutive resets.

## Source Layout

| Path | Responsibility |
| --- | --- |
| [src/gui](src/gui) | Swing window, screen, settings, and drive dialogs. `.form` files hold NetBeans form metadata. |
| [src/machine](src/machine) | Machine lifecycle, memory, timing, video, keyboard, floppy/HDD controllers, and configuration. |
| [src/z80core](src/z80core) | Z80 CPU implementation and machine I/O interfaces. |
| [src/utils](src/utils) | Drive geometry and ring buffer. |
| [src/roms](src/roms) | Bundled boot and character ROMs. |
| [test/gui/ResetLifecycleTest.java](test/gui/ResetLifecycleTest.java) | GUI reset/resource-lifecycle regression check. |

Customize Ant in [build.xml](build.xml), not the generated
[nbproject/build-impl.xml](nbproject/build-impl.xml). Preserve NetBeans-generated
Swing layout sections and matching form metadata when changing the UI.
