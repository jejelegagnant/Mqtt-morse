package org.KeyboardEntry;

import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import java.util.Scanner;
import java.util.UUID;

public class KeyboardEntry {
private static final String server = "tcp://localhost:1883";
private static final String clientId = "KeyboardEntry";
private static final String eventTopic = "E/KeyboardEvent";
private static final String statusTopic = "S/KeyboardEvent";
    static void main() throws MqttException {
        MqttClient client = new MqttClient(server,clientId);
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setCleanStart(true);
        options.setAutomaticReconnect(true);

        byte[] willPayload = "Offline".getBytes();
        int willQos = 1;
        boolean willRetained = true;
        MqttMessage willMessage = new MqttMessage(willPayload);
        willMessage.setQos(willQos);
        willMessage.setRetained(willRetained);

        options.setWill(statusTopic, willMessage);

        client.setCallback(new MqttCallback() {

            @Override
            public void disconnected(MqttDisconnectResponse mqttDisconnectResponse) {
                System.out.println(mqttDisconnectResponse);
            }

            @Override
            public void mqttErrorOccurred(MqttException e) {
                System.out.println(e.getMessage());
            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                System.err.println("A message has arrived, however no message where expected: " + s);
                throw new RuntimeException("A message has arrived, however no message where expected: " + s);
            }

            @Override
            public void deliveryComplete(IMqttToken iMqttToken) {
                System.out.println("Delivery complete");
            }

            @Override
            public void connectComplete(boolean b, String s) {
                System.out.println("Connection complete" + b);
            }

            @Override
            public void authPacketArrived(int i, MqttProperties mqttProperties) {
                System.out.println("Auth packet arrived" + i);
            }
        });
        client.connect(options);
        MqttMessage onlineMessage = new MqttMessage("Online".getBytes());
        onlineMessage.setQos(1);
        onlineMessage.setRetained(true);
        client.publish(statusTopic, onlineMessage);
        System.out.println("Online status published : " + onlineMessage);
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter Message: ");
        String input = scanner.nextLine();
        while (!input.equals("quit")) {
            System.out.println(input);
            String message = "msg: " + input + " id: " + UUID.randomUUID();
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(1);
            mqttMessage.setRetained(false);
            client.publish(eventTopic, mqttMessage);
            System.out.println("Enter Message: ");
            input = scanner.nextLine();
        }
        scanner.close();
        client.publish(statusTopic, willMessage);
        client.disconnect();
        client.close();
        System.out.println("Quit command received, client is stopped");
    }
}
