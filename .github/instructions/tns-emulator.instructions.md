---
description: "Use when changing the Java TNS GC emulator: Z80 execution, memory mapping, video, keyboard, timing, peripherals, Swing UI, ROM or disk-image handling."
name: "TNS Emulator Development"
applyTo: ["src/**/*.java", "build.xml", "nbproject/**", "**/*.md"]
---
# TNS Emulator Development

- Treat this as an emulator of the TNS GC computer; preserve compatibility with existing software and machine behavior.
- Before changing CPU, memory, video, timing, keyboard, or peripheral behavior, trace the relevant path through the existing machine components and check available documentation or test images. Do not invent hardware details when evidence is missing; call out the uncertainty.
- Keep hardware responsibilities in the existing `machine`, `z80core`, `gui`, and `utils` boundaries. Preserve cycle counts, memory and I/O mappings, interrupt behavior, and reset semantics unless the requested change specifically requires otherwise.
- Preserve the project's Java 8 source/target level and its NetBeans Ant build unless a version or build-system change is explicitly requested.
- Keep changes focused and consistent with the existing Swing UI and Java style. Avoid broad refactors alongside emulator behavior changes.
- Treat ROMs, disk images, and other binary fixtures as compatibility-sensitive inputs. Do not modify or regenerate them unless explicitly requested.
- Verify changes with the narrowest relevant check available, then run the Ant build when practical. State clearly when hardware behavior could not be verified against documentation or a test image.