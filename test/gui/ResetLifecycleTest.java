package gui;

import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import machine.Tns;

public class ResetLifecycleTest {
    private static Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("tns-reset-test");
        Path floppy = Files.createFile(directory.resolve("floppy.8sd"));
        Path hardDisk = Files.createFile(directory.resolve("harddisk.img"));
        Path configPath = directory.resolve("tns.config");
        Properties properties = new Properties();
        properties.setProperty("DRIVE1", floppy.toString());
        properties.setProperty("DRIVE2", "");
        properties.setProperty("DRIVE3", "");
        properties.setProperty("DRIVE4", "");
        properties.setProperty("HDD", hardDisk.toString());
        try (java.io.OutputStream output = Files.newOutputStream(configPath)) {
            properties.store(output, "Reset lifecycle test");
        }
        System.setProperty("tns.config.path", configPath.toString());
        List<Tns> machines = new ArrayList<Tns>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                FTns window = null;
                try {
                    window = new FTns();
                    for (int cycle = 0; cycle < 3; cycle++) {
                        Tns previous = (Tns) field(window, "m");
                        machines.add(previous);
                        previous.stopEmulation();
                        previous.getWDC().set3212(0x44);
                        previous.getWDC().setTrk(26);
                        RandomAccessFile oldFloppy = (RandomAccessFile) field(previous.getWDC(), "f");
                        RandomAccessFile oldHardDisk = (RandomAccessFile) field(previous.getWAP(), "f");
                        ((JButton) field(window, "bReset")).doClick();
                        Tns current = (Tns) field(window, "m");
                        check(current != previous, "Reset reused the old machine");
                        check(current.getImage() != previous.getImage(), "Reset reused the framebuffer");
                        for (String component : new String[]{"cpu", "mem", "clk", "wdc", "wap", "key", "grf", "itk"}) {
                            check(field(current, component) != field(previous, component), "Reset reused " + component);
                        }
                        check(window.getKeyListeners().length == 1, "Stale keyboard listeners remain");
                        check(window.getKeyListeners()[0] == current.getKeyboard(), "New keyboard is not connected");
                        check(field(field(window, "scr"), "image") == current.getImage(), "New framebuffer is not connected");
                        check(current.getConfig() == previous.getConfig(), "Settings were lost");
                        check(current.getWDC().getImage(1).equals(floppy.toString()), "Floppy selection was lost");
                        check(current.getWAP().getImage().equals(hardDisk.toString()), "Hard disk selection was lost");
                        check(current.getWDC().getTrk() == 0, "New FDC retained the old track");
                        check(!oldFloppy.getFD().valid(), "Old floppy handle is open");
                        check(!oldHardDisk.getFD().valid(), "Old hard disk handle is open");
                        try {
                            ((Timer) field(previous, "tim")).schedule(new TimerTask() {
                                @Override public void run() { }
                            }, 0);
                            throw new AssertionError("Old timer is still accepting tasks");
                        } catch (IllegalStateException expected) {
                        }
                    }
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                } finally {
                    if (window != null) {
                        try {
                            Tns current = (Tns) field(window, "m");
                            machines.add(current);
                            current.shutdown();
                        } catch (Exception exception) {
                            throw new RuntimeException(exception);
                        } finally {
                            window.dispose();
                        }
                    }
                }
            });
            for (Tns machine : machines) {
                machine.join(2000);
                check(!machine.isAlive(), "Old machine thread did not terminate");
            }
            System.out.println("PASS: three GUI resets create fresh machines and release old resources");
        } finally {
            Files.deleteIfExists(configPath);
            Files.deleteIfExists(floppy);
            Files.deleteIfExists(hardDisk);
            Files.deleteIfExists(directory);
        }
    }
}