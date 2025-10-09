package org.display;

import com.epic.morse.service.MorseCode;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import javax.swing.*;
import java.awt.*;
import java.util.UUID;

public class MorseDisplay implements Runnable {
    private JPanel panel;
    private JFrame frame;

    private static final String server = "tcp://localhost:1883";
    private static final String clientId = "morseDisplay";
    private static final String inputTopic = "E/textInMorse";
    private static String lastMessageId = "";

    private void defineFrame(){
        frame = new JFrame("Morse Display");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800,600);
        frame.setResizable(false);
        frame.add(panel());
        frame.setVisible(true);
    }

    private JPanel panel(){
        panel = new JPanel();
        panel.setBackground(Color.BLACK);
        return panel;
    }

    public void changePanelColor(Color newColor) {
        if (panel != null) {
            SwingUtilities.invokeLater(() -> {
                panel.setBackground(newColor);
                panel.repaint();
            });
        }
    }

    @Override
    public void run() {
        defineFrame();
        // Change color after frame is visible
        changePanelColor(Color.ORANGE);
    }


    public static void main(String[] args) throws MqttException {
        SwingUtilities.invokeLater(new MorseDisplay());
        MqttClient client = new MqttClient(server,clientId);
        client.setCallback(new MqttCallback() {
            @Override
            public void disconnected(MqttDisconnectResponse disconnectResponse) {
                System.out.println("Disconnected: " + disconnectResponse.getReasonString());
            }

            @Override
            public void mqttErrorOccurred(MqttException exception) {
                System.out.println("MQTT error: " + exception.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                System.out.println("Message arrived. Topic: " + topic +
                        " Message: " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttToken token) {
                System.out.println("Delivery complete");
            }

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                System.out.println("Connect complete. Reconnect=" + reconnect + " URI=" + serverURI);
            }

            @Override
            public void authPacketArrived(int reasonCode, MqttProperties properties) {
                // Not used here
            }
        });
        client.connect();
        client.subscribe(inputTopic,1);
    }
}
