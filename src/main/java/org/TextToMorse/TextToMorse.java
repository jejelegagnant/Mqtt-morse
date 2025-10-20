package org.TextToMorse;

import com.epic.morse.service.MorseCode;
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import java.util.UUID;

/**
 * Represents the "transformer" component in the distributed Morse code system.
 * This class acts as an intermediary, subscribing to raw text events from the
 * {@link org.KeyboardEntry.KeyboardEntry} process and transforming them into Morse code.
 * <p>
 * It demonstrates a core principle of process control and event-driven architecture:
 * consuming events from one topic, applying business logic (the conversion), and
 * publishing new, enriched events to another topic. This decouples the data source
 * from the final actuator. It also monitors the status of the input sensor.
 *
 * @author Jérémie Gremaud
 * @version 20.10.2025
 */
public class TextToMorse {
    private static final String server = "tcp://localhost:1883";
    private static final String clientId = "textToMorse";
    private static final String inputTopic = "E/KeyboardEvent";
    private static final String outputTopic = "E/textInMorse";
    private static final String inputStatusTopic = "S/KeyboardEvent";
    private static final String outputStatusTopic = "S/textInMorse";
    private static String lastMessageId = "";

    /**
     * The main entry point for the TextToMorse process.
     * Initializes the MQTT client, sets up subscriptions to the keyboard entry's
     * event and status topics, and connects to the broker. The main thread terminates
     * after setup, but the application continues to run via the background MQTT
     * client thread, which processes incoming messages in the MqttCallback.
     *
     * @throws MqttException if there is an error connecting to the broker.
     */
    static void main() throws MqttException {
        MqttClient client = new MqttClient(server, clientId);
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setCleanStart(true);
        options.setAutomaticReconnect(true);

        byte[] willPayload = "Offline".getBytes();
        int willQos = 1;
        boolean willRetained = true;
        MqttMessage willMessage = new MqttMessage(willPayload);
        willMessage.setQos(willQos);
        willMessage.setRetained(willRetained);

        options.setWill(outputStatusTopic, willMessage);
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
                String payload = new String(message.getPayload());
                if (topic.equals(inputTopic)) {
                    int start = payload.indexOf("msg: ") + 5;
                    int end = payload.indexOf(" id:");
                    String messageText = payload.substring(start, end).trim();
                    String messageId = payload.substring(end).trim();
                    if (!lastMessageId.equals(messageId)) {
                        lastMessageId = messageId;
                        String messageMorse = MorseCode.convertToMorseCode(messageText);
                        String messageWithId = "msg: " + messageMorse + " id: " + UUID.randomUUID();
                        MqttMessage mqttMessage = new MqttMessage(messageWithId.getBytes());
                        client.publish(outputTopic, mqttMessage);
                    }
                }
                if (topic.equals(inputStatusTopic)) {
                    if (payload.equals("Offline")) {
                        System.err.println("WARNING: Input source (KeyboardEntry) is offline. Waiting for it to return...");
                    } else {
                        System.out.println("INFO: Input source (KeyboardEntry) is back online.");
                    }
                }
            }

            @Override
            public void deliveryComplete(IMqttToken token) {
                System.out.println("Delivery complete");
            }

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                System.out.println("Connect complete. Reconnect=" + reconnect + " URI=" + serverURI);
                try {
                    client.subscribe(inputTopic, 1);
                    client.subscribe(inputStatusTopic, 1);
                } catch (MqttException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void authPacketArrived(int reasonCode, MqttProperties properties) {
                // Not used here
            }
        });
        client.connect(options);
        MqttMessage onlineMessage = new MqttMessage("Online".getBytes());
        onlineMessage.setQos(1);
        onlineMessage.setRetained(true);
        client.publish(outputStatusTopic, onlineMessage);
        System.out.println("Online status published" + onlineMessage);
    }
}
