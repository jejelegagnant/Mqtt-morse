# Distributed Morse Converter with a GUI

![Course Badge](https://img.shields.io/badge/UniFR-IN.4028%20Process%20Control-blue)

This project is a distributed system for converting text to Morse code, developed for the "Process Control" course at the University of Fribourg. It demonstrates the principles of event-driven architecture by using MQTT to orchestrate communication between independent processes.

The system captures user input from a command-line interface, converts it to a Morse code sequence, and simulates a flashing light signal in a simple Java Swing GUI.

## System Architecture

The application is decoupled into three distinct processes that communicate exclusively through an MQTT broker. This design ensures that the components are independent and do not share memory, adhering to the principles of a distributed, event-driven system.

The three main components are:

1.  **Sensor (`KeyboardEntry.java`):** A command-line process that acts as a sensor. It captures user-entered text, adds a unique ID to each message, and publishes it as a `KeyboardEvent` to the MQTT broker.
2.  **Transformer (`TextToMorse.java`):** A background process that subscribes to `KeyboardEvent`s. It consumes the text messages, converts them into Morse code sequences, and publishes the result as a new `TextInMorse` event to a different topic.
3.  **Actuator (`MorseDisplay.java`):** A Java Swing application that acts as an actuator. It subscribes to `TextInMorse` events and visually represents the Morse code by changing its background color to simulate a flashing light. It also monitors the status of the other two components and displays an error color if one goes offline.

All communication is mediated by an MQTT broker, which routes events based on their topics.

## Technology Stack

* **Language:** Java 25
* **Messaging:** MQTT v5 (using Eclipse Paho client)
* **Broker:** Mosquitto MQTT Broker (or any other standard MQTT broker)
* **GUI:** Java Swing
* **Build Tool:** Apache Maven
* **Dependencies:**
    * `org.eclipse.paho.mqttv5.client`: For MQTT communication.
    * `morse-code-translator`: For the text-to-Morse conversion logic.

## Prerequisites

Before running the project, you must have the following installed:

1.  **Java JDK 25** (or a later version).
2.  **Apache Maven** to build the project.
3.  An **MQTT Broker** running on `tcp://localhost:1883`. The recommended broker is **Mosquitto**. You can download it from the [official Mosquitto website](https://mosquitto.org/download/).

## How to Use

1.  After launching the system, focus on the terminal window for **"KeyboardEntry"**.
2.  Type any message (e.g., "hello world") and press `Enter`.
3.  Observe the **"TextToMorse"** terminal, where you will see the message being received and converted.
4.  Watch the **"Morse Display"** GUI window, which will begin to flash the Morse code sequence.
5.  To shut down the input client, type `quit` in the **"KeyboardEntry"** terminal and press `Enter`. The other components will detect this and change the display color.

## License

This project is licensed under the MIT License. See the `LICENSE` file for more details.

## Third-Party Licenses

This project utilizes third-party libraries, including the Eclipse Paho MQTT Client and morse-code-translator. The full license information for these dependencies is available in the `NOTICES.md` file.
