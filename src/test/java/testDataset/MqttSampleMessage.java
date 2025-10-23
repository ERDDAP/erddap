package testDataset;

import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import gov.noaa.pfel.erddap.dataset.EDDTableFromMqtt;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class MqttSampleMessage {

  public static void main(String[] args) {
    String topic = "erddap/test/topic";
    String message = "{\"lat\": 20.0, \"lon\": -150.0, \"temperature\": 22.5}";

    Mqtt5AsyncClient mqttClient =
        EDDTableFromMqtt.initialiseMqttAsyncClient(
                "localhost", 1883, "erddap-client", "", "", false, 60, false, 10, 10, true)
            .join();
    try {
      mqttClient
          .publishWith()
          .topic(topic)
          .payload(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)))
          .send();

      System.out.println("Message '" + message + "' sent to topic '" + topic + "'");
    } finally {
      // Always disconnect to properly close the connection
      mqttClient.disconnect();
      System.out.println("Disconnected.");
    }
  }
}
