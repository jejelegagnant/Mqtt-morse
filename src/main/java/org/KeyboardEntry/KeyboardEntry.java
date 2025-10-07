package org.KeyboardEntry;

import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;

import java.util.Scanner;
import java.util.UUID;

public class KeyboardEntry {
private static final String server = "tcp://localhost:1883";
private static final String clientId = "KeyboardEntry";
private static final String eventTopic = "E/KeyboardEvent";
    static void main() throws MqttException {
        MqttClient client = new MqttClient(server,clientId);
        client.connect();
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter Message: ");
        String input = scanner.nextLine();
        while (!input.equals("quit")) {
            System.out.println(input);
            String message = "msg: " + input + " id: " + UUID.randomUUID();
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            client.publish(eventTopic, mqttMessage);
            input = scanner.nextLine();
        }
    }
}
