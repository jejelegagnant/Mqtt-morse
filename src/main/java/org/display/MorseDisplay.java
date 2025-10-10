package org.display;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class MorseDisplay implements Runnable {
    private JPanel panel;
    private JFrame frame;

    // --- MQTT and Morse Timing Constants ---
    private static final String server = "tcp://localhost:1883";
    private static final String clientId = "morseDisplay";
    private static final String inputTopic = "E/textInMorse";
    private static String lastMessageId = "";

    private static final int MORSE_BASE_TIME = 100; // ms
    private static final int DOT_TIME = MORSE_BASE_TIME;
    private static final int DASH_TIME = MORSE_BASE_TIME * 3;
    private static final int INTRA_CHAR_TIME = MORSE_BASE_TIME; // Time between dots/dashes
    private static final int INTER_CHAR_TIME = MORSE_BASE_TIME * 3; // Time between letters

    // --- State Management for the Animation ---
    private String currentMorseMessage;
    private int morseIndex;
    private Timer morseTimer;

    private void defineFrame() {
        frame = new JFrame("Morse Display");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setResizable(false);
        frame.add(panel());
        frame.setVisible(true);
    }

    private JPanel panel() {
        panel = new JPanel();
        panel.setBackground(Color.BLACK);
        return panel;
    }

    // This method is now safe as it's always called from the EDT
    public void changePanelColor(Color newColor) {
        panel.setBackground(newColor);
        panel.repaint();
    }

    @Override
    public void run() {
        defineFrame();
    }

    /**
     * Kicks off the animation for a new Morse code message.
     */
    private void displayMorseMessage(String message) {
        // If an animation is already running, stop it.
        if (morseTimer != null && morseTimer.isRunning()) {
            morseTimer.stop();
        }

        this.currentMorseMessage = message;
        this.morseIndex = 0;
        processNextSignal(); // Start processing the first signal
    }

    /**
     * Processes one signal (dot, dash, or space) from the message.
     * It uses a chain of timers to create non-blocking delays.
     */
    private void processNextSignal() {
        // Stop if we've finished the message
        if (morseIndex >= currentMorseMessage.length()) {
            changePanelColor(Color.BLACK); // Ensure panel is off
            return;
        }

        char signal = currentMorseMessage.charAt(morseIndex);
        morseIndex++; // Move to the next index for the next call

        switch (signal) {
            case '.':
                displaySignal(DOT_TIME);
                break;
            case '-':
                displaySignal(DASH_TIME);
                break;
            case ' ':
                // This is a space between letters, just wait.
                morseTimer = new Timer(INTER_CHAR_TIME, e -> processNextSignal());
                morseTimer.setRepeats(false);
                morseTimer.start();
                break;
        }
    }

    /**
     * Helper method to display a dot or a dash.
     * @param signalDuration The time the panel should be lit (DOT_TIME or DASH_TIME).
     */
    private void displaySignal(int signalDuration) {
        changePanelColor(Color.ORANGE);

        // First timer: Turn the panel OFF after the signal duration.
        // Then, trigger the second timer for the intra-character gap.
        ActionListener turnOffAction = e -> {
            changePanelColor(Color.BLACK);

            // Second timer: After the gap, process the next signal.
            morseTimer = new Timer(INTRA_CHAR_TIME, e2 -> processNextSignal());
            morseTimer.setRepeats(false);
            morseTimer.start();
        };

        morseTimer = new Timer(signalDuration, turnOffAction);
        morseTimer.setRepeats(false);
        morseTimer.start();
    }


    public static void main(String[] args) throws MqttException {
        MorseDisplay morseDisplay = new MorseDisplay();
        SwingUtilities.invokeLater(morseDisplay);

        MqttClient client = new MqttClient(server, clientId);
        client.setCallback(new MqttCallback() {
            @Override
            public void disconnected(MqttDisconnectResponse disconnectResponse) {}

            @Override
            public void mqttErrorOccurred(MqttException exception) {}

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                // This method is now very clean and non-blocking!
                System.out.println("Message arrived. Topic: " + topic + " Message: " + new String(message.getPayload()));
                String payload = new String(message.getPayload());
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
            }

            @Override
            public void deliveryComplete(IMqttToken iMqttToken) {}

            @Override
            public void connectComplete(boolean b, String s) {
                System.out.println("Connected to the server.");
            }

            @Override
            public void authPacketArrived(int i, MqttProperties mqttProperties) {}
        });
        client.connect();
        client.subscribe(inputTopic, 1);
    }
}