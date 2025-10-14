package org;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A utility to launch multiple Java applications simultaneously in separate terminal windows.
 * This version is specifically designed for debugging, as it includes commands to keep
 * the terminal windows open after the child process terminates, allowing developers
 * to read error messages.
 */
public class ProcessLauncher {

    public static void main(String[] args) {
        System.out.println("Starting Process Launcher...");

        // Define the fully qualified names of the main classes to run
        String[] classesToRun = {
                "org.KeyboardEntry.KeyboardEntry",
                "org.TextToMorse.TextToMorse",
                "org.display.MorseDisplay"
        };

        for (String className : classesToRun) {
            try {
                launchProcessInNewTerminal(className);
                System.out.println("  -> Launched " + className);
                // A small delay to allow OS to open terminals without fighting for focus
                Thread.sleep(200);
            } catch (IOException | InterruptedException e) {
                System.err.println("Failed to launch " + className);
                e.printStackTrace();
            }
        }

        System.out.println("All processes launched. This launcher will now exit.");
    }

    /**
     * Constructs and executes an OS-specific command to run a Java class
     * in a new terminal window. The commands are configured to keep the
     * terminal open after the process finishes to aid in debugging.
     *
     * @param mainClass The fully qualified name of the class to run.
     * @throws IOException if an I/O error occurs starting the process.
     * @throws InterruptedException if the thread is interrupted.
     */
    private static void launchProcessInNewTerminal(String mainClass) throws IOException, InterruptedException {
        String os = System.getProperty("os.name").toLowerCase();

        // Use the same classpath that this launcher is running with. This is key.
        String classpath = System.getProperty("java.class.path");
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";

        List<String> command = new ArrayList<>();
        String javaCommand = String.format("\"%s\" -cp \"%s\" %s", javaBin, classpath, mainClass);

        if (os.contains("win")) {
            // Windows: Use 'cmd /k' to execute the command AND KEEP the window open.
            command.add("cmd");
            command.add("/c");
            command.add("start");
            command.add("\"" + mainClass.substring(mainClass.lastIndexOf('.') + 1) + "\""); // Set a window title
            command.add("cmd.exe");
            command.add("/k");
            command.add(javaCommand);

        } else if (os.contains("mac")) {
            // macOS: Use osascript to tell the Terminal app to run a shell command
            // that executes java and then waits for the user to press Enter.
            command.add("osascript");
            command.add("-e");
            String script = String.format(
                    "tell application \"Terminal\" to do script \"%s; echo \\\"--- Process Finished. Press Enter to close. ---\\\"; read\"",
                    javaCommand.replace("\"", "\\\"") // Escape quotes for the script
            );
            command.add(script);

        } else if (os.contains("nix") || os.contains("nux")) {
            // Linux: Use a terminal emulator to run a shell that executes java
            // and then waits for the user to press Enter.
            // NOTE: You may need to change 'gnome-terminal' to 'konsole', 'xterm', etc.
            command.add("gnome-terminal");
            command.add("--");
            command.add("bash");
            command.add("-c");
            String script = String.format(
                    "%s; echo; echo '--- Process Finished. Press Enter to close. ---'; read",
                    javaCommand
            );
            command.add(script);

        } else {
            System.err.println("Unsupported OS: " + os + ". Cannot open new terminal.");
            return;
        }

        System.out.println("  -> Executing debug command: " + String.join(" ", command));
        new ProcessBuilder(command).start();
    }
}