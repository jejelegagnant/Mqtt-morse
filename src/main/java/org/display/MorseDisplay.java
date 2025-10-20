package org.display;

import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import javax.swing.*;
import java.awt.*;

/**
 * Represents the "actuator" component of the distributed system.
 * This class subscribes to Morse code events and visually displays them by
 * simulating a flashing light in a Java Swing GUI. It is the final step in the
 * process control pipeline, where the processed data is used to effect a change.
 * <p>
 * This class addresses the challenge of integrating a network client (MQTT) with
 * a GUI framework. It correctly handles threading by performing GUI updates on the
 * Event Dispatch Thread (EDT) using {@code SwingUtilities.invokeLater}, and manages
 * the animation timing in a separate background thread to avoid freezing the UI.
 * It also monitors the status of upstream components and provides visual feedback
 * if one goes offline.
 *
 * @author Jérémie Gremaud
 * @version 20.10.2025
 */
public class MorseDisplay implements Runnable {
    private JPanel panel;
    private JFrame frame;

    // --- MQTT and Morse Timing Constants ---
    private static final String server = "tcp://localhost:1883";
    private static final String clientId = "morseDisplay";
    private static final String inputTopic = "E/textInMorse";
    private static final String converterStatusTopic = "S/textInMorse";
    private static final String keyboardStatusTopic = "S/KeyboardEvent";
    private static String lastMessageId = "";

    private static final int MORSE_BASE_TIME = 500; // ms
    private static final int DOT_TIME = MORSE_BASE_TIME;
    private static final int DASH_TIME = MORSE_BASE_TIME * 3;
    private static final int INTRA_CHAR_TIME = MORSE_BASE_TIME; // Time between dots/dashes
    private static final int INTER_CHAR_TIME = MORSE_BASE_TIME * 3; // Time between letters

    // --- State Management for the Animation ---
    private String currentMorseMessage;
    private int morseIndex;
    private Timer morseTimer;

    // --- Separate thread to ensure accurate timing
    private Thread morseAnimatorThread;

    /**
     * Initializes the main JFrame for the display.
     */
    private void defineFrame() {
        frame = new JFrame("Morse Display");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setResizable(false);
        frame.add(panel());
        frame.setVisible(true);
    }

    /**
     * Initializes the JPanel that will act as the flashing light.
     *
     * @return The configured JPanel.
     */
    private JPanel panel() {
        panel = new JPanel();
        panel.setBackground(Color.BLACK);
        return panel;
    }

    /**
     * Safely changes the background color of the panel. This method must be
     * called on the Event Dispatch Thread.
     *
     * @param newColor The new color for the panel.
     */
    public void changePanelColor(Color newColor) {
        panel.setBackground(newColor);
        panel.repaint();
    }

    /**
     * The run method for the Runnable interface. Called by SwingUtilities.invokeLater
     * to set up the GUI on the Event Dispatch Thread.
     */
    @Override
    public void run() {
        defineFrame();
    }

    /**
     * Displays a Morse code message by simulating a flashing light in a background thread.
     * This method ensures accurate timing using {@code Thread.sleep()} without blocking the GUI.
     * If a new message arrives while a previous one is being displayed, the old
     * animation thread is interrupted and a new one is started.
     *
     * @param message The Morse code string to be displayed (e.g., "... --- ...").
     */
    private void displayMorseMessage(String message) {
        // If an animation is already running from a previous message, interrupt it.
        if (morseAnimatorThread != null && morseAnimatorThread.isAlive()) {
            morseAnimatorThread.interrupt();
        }

        // Create and start a new thread for the new message animation.
        morseAnimatorThread = new Thread(() -> {
            try {
                // Loop through each character of the morse string
                for (char signal : message.toCharArray()) {
                    // Check if the thread has been interrupted (e.g., by a new message arriving)
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException();
                    }

                    switch (signal) {
                        case '.':
                            // Turn light ON
                            SwingUtilities.invokeLater(() -> changePanelColor(Color.ORANGE));
                            Thread.sleep(DOT_TIME);

                            // Turn light OFF
                            SwingUtilities.invokeLater(() -> changePanelColor(Color.BLACK));
                            Thread.sleep(INTRA_CHAR_TIME); // Gap between signals in a letter
                            break;

                        case '-':
                            // Turn light ON
                            SwingUtilities.invokeLater(() -> changePanelColor(Color.ORANGE));
                            Thread.sleep(DASH_TIME);

                            // Turn light OFF
                            SwingUtilities.invokeLater(() -> changePanelColor(Color.BLACK));
                            Thread.sleep(INTRA_CHAR_TIME); // Gap between signals in a letter
                            break;

                        case ' ':
                            // A space between letters requires an INTER_CHAR_TIME pause.
                            // We already paused for INTRA_CHAR_TIME after the last signal,
                            // so we only need to wait for the remaining time.
                            Thread.sleep(INTER_CHAR_TIME - INTRA_CHAR_TIME);
                            break;
                    }
                }
            } catch (InterruptedException e) {
                // This happens when a new message arrives and interrupts the current animation.
                // We turn the panel black to clean up the state.
                System.out.println("Morse animation interrupted.");
                SwingUtilities.invokeLater(() -> changePanelColor(Color.BLACK));
                // Restore the interrupted status
                Thread.currentThread().interrupt();
            }
        });
        morseAnimatorThread.start();
    }

    /**
     * The main entry point for the MorseDisplay process.
     * It initializes the GUI on the Event Dispatch Thread and sets up the MQTT client
     * to receive and process Morse code and status messages.
     *
     * @throws MqttException if there is an error connecting to the broker.
     */
    static void main() throws MqttException {
        MorseDisplay morseDisplay = new MorseDisplay();
        SwingUtilities.invokeLater(morseDisplay);

        MqttClient client = new MqttClient(server, clientId);
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setCleanStart(true);
        options.setAutomaticReconnect(true);

        client.setCallback(new MqttCallback() {
            @Override
            public void disconnected(MqttDisconnectResponse disconnectResponse) {
            }

            @Override
            public void mqttErrorOccurred(MqttException exception) {
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                // This method is now very clean and non-blocking!
                System.out.println("Message arrived. Topic: " + topic + " Message: " + new String(message.getPayload()));
                String payload = new String(message.getPayload());
                if (topic.equals(inputTopic)) {
                    int start = payload.indexOf("msg: ") + 5;
                    int end = payload.indexOf(" id:");
                    String messageMorse = payload.substring(start, end).trim();
                    String messageId = payload.substring(end).trim();

                    if (!lastMessageId.equals(messageId)) {
                        lastMessageId = messageId;
                        System.out.println("Displaying Morse: " + messageMorse);

                        // Safely start the animation on the EDT
                        SwingUtilities.invokeLater(() -> morseDisplay.displayMorseMessage(messageMorse));
                    }
                } else if (topic.equals(converterStatusTopic)) {
                    if (payload.equals("Offline")) {
                        morseDisplay.morseAnimatorThread.interrupt();
                        SwingUtilities.invokeLater(() -> morseDisplay.changePanelColor(Color.RED));
                    }
                    if (payload.equals("Online")) {
                        SwingUtilities.invokeLater(() -> morseDisplay.changePanelColor(Color.BLACK));
                    }
                } else if (topic.equals(keyboardStatusTopic)) {
                    if (payload.equals("Offline")) {
                        morseDisplay.morseAnimatorThread.interrupt();
                        SwingUtilities.invokeLater(() -> morseDisplay.changePanelColor(Color.MAGENTA));
                    }
                    if (payload.equals("Online")) {
                        SwingUtilities.invokeLater(() -> morseDisplay.changePanelColor(Color.BLACK));
                    }
                }
            }

            @Override
            public void deliveryComplete(IMqttToken iMqttToken) {
            }

            @Override
            public void connectComplete(boolean b, String s) {
                System.out.println("Connected to the server.");
                try {
                    client.subscribe(inputTopic, 1);
                    client.subscribe(keyboardStatusTopic, 1);
                    client.subscribe(converterStatusTopic, 1);
                } catch (MqttException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void authPacketArrived(int i, MqttProperties mqttProperties) {
            }
        });
        client.connect(options);
    }
}