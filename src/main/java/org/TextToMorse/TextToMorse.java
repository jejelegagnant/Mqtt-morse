package org.TextToMorse;

import com.epic.morse.service.MorseCode;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

public class TextToMorse {
    private static final String server = "tcp://localhost:1883";
    private static final String clientId = "textToMorse";
    private static final String inputTopic = "E/KeyboardEvent";
    private static final String outputTopic = "E/textInMorse";
    private static String lastMessageId = "";
    private static MqttClient mqttClient;
    private static IMqttToken mqttToken;
    static void main() throws MqttException {
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
                String payload = new String(message.getPayload());
                int start = payload.indexOf("msg: ") + 5;
                int end = payload.indexOf(" id:");
                String messageText = payload.substring(start, end).trim();
                String messageId = payload.substring(end).trim();
                if (!lastMessageId.equals(messageId)) {
                    lastMessageId = messageId;
                    String messageMorse = MorseCode.convertToMorseCode(messageText);
                    MqttMessage mqttMessage = new MqttMessage(messageMorse.getBytes());
                    client.publish(outputTopic,mqttMessage);

                }

                //todo: convert to morse before republishing
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
