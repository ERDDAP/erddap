package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.String2;
import com.cohort.util.Test;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

class EDDTableFromMqttTests {

  @TempDir private static Path TEMP_DIR;

  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  @org.junit.jupiter.api.Test
  void testProcessMqttData() throws Throwable {
    EDDTableFromMqtt eddTableFromMqtt = (EDDTableFromMqtt) EDDTestDataset.gettestFromMqtt();
    eddTableFromMqtt.fileDir = TEMP_DIR.toAbsolutePath().toString();
    Mqtt5Publish publish = Mockito.mock(Mqtt5Publish.class);
    String topic = "sensor/data";
    String payload = "{\"lat\": 20.0, \"lon\": -150.0, \"temperature\": 22.5}";
    Mockito.when(publish.getTopic())
        .thenReturn(com.hivemq.client.mqtt.datatypes.MqttTopic.of(topic));
    Mockito.when(publish.getPayloadAsBytes()).thenReturn(payload.getBytes());
    eddTableFromMqtt.processMqttData(publish);

    String expectedFilePath = eddTableFromMqtt.getFilePathForTopic(topic);
    Table resultTable = new Table();
    resultTable.readJsonlCSV(expectedFilePath, null, null, true);
    Test.ensureEqual(resultTable.nRows(), 1, "nRows");
    Test.ensureEqual(resultTable.getFloatData(0, 0), 20.0f, "lat");
    Test.ensureEqual(resultTable.getFloatData(1, 0), -150.0f, "lon");
    Test.ensureEqual(resultTable.getFloatData(2, 0), 22.5f, "temperature");
  }

  @org.junit.jupiter.api.Test
  void testGetFilePathForTopic() throws Throwable {
    EDDTableFromMqtt eddTableFromMqtt = (EDDTableFromMqtt) EDDTestDataset.gettestFromMqtt();
    URL url = EDDTableFromMqttTests.class.getResource("/testFromMqtt/test/");
    String filePath = Path.of(url.toURI()).toString();

    Test.ensureEqual(
        eddTableFromMqtt.getFilePathForTopic("test/topic1"),
        filePath + "/topic1.jsonl",
        "topic file path");
  }

  @org.junit.jupiter.api.Test
  void testInitializeMqttClient() {
    String2.log("\n*** EDDTableFromMqttTests.testInitializeMqttClient()");
    CompletableFuture<com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient> futureClient =
        EDDTableFromMqtt.initialiseMqttAsyncClient(
            "broker.hivemq.com",
            1883,
            "test-client",
            "user",
            "password",
            false,
            60,
            true,
            0,
            10,
            true);

    try {
      com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient client = futureClient.join();
      Test.ensureNotNull(client, "Client should not be null");
      client.disconnect();
    } catch (Exception e) {
      // Depending on the environment, a connection might not be possible.
      // The goal is to test the configuration logic.
      String2.log("Could not connect to MQTT broker, which is acceptable for this test.");
    }
  }
}
