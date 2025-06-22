package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientBuilder;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.dataset.metadata.LocalizedAttributes;
import gov.noaa.pfel.erddap.variable.DataVariableInfo;
import gov.noaa.pfel.erddap.variable.EDV;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import org.json.JSONObject;

public class EDDTableFromMqtt extends EDDTableFromFiles {

  public static final int DEFAULT_STANDARDIZEWHAT = 0;
  private static final int MQTT_PORT = 1883;
  private static final int MQTT_SECURE_PORT = 8883;

  protected String[] columnNames;
  protected PAType[] columnPATypes;

  public EDDTableFromMqtt(
      String tClassName,
      String tDatasetID,
      String tAccessibleTo,
      String tGraphsAccessibleTo,
      StringArray tOnChange,
      String tFgdcFile,
      String tIso19115File,
      String tSosOfferingPrefix,
      String tDefaultDataQuery,
      String tDefaultGraphQuery,
      LocalizedAttributes tAddGlobalAttributes,
      List<DataVariableInfo> tDataVariables,
      int tReloadEveryNMinutes,
      int tUpdateEveryNMillis,
      String tFileDir,
      String tFileNameRegex,
      boolean tRecursive,
      String tPathRegex,
      String tMetadataFrom,
      String tCharset,
      String tSkipHeaderToRegex,
      String tSkipLinesRegex,
      int tColumnNamesRow,
      int tFirstDataRow,
      String tColumnSeparator,
      String tPreExtractRegex,
      String tPostExtractRegex,
      String tExtractRegex,
      String tColumnNameForExtract,
      String tSortedColumnSourceName,
      String tSortFilesBySourceNames,
      boolean tSourceNeedsExpandedFP_EQ,
      boolean tFileTableInMemory,
      boolean tAccessibleViaFiles,
      boolean tRemoveMVRows,
      int tStandardizeWhat,
      int tNThreads,
      String tCacheFromUrl,
      int tCacheSizeGB,
      String tCachePartialPathRegex,
      String tAddVariablesWhere,
      String serverHost,
      Integer serverPort,
      String clientId,
      String username,
      String password,
      String[] topics,
      boolean useSsl,
      int keepAlive,
      boolean cleanStart,
      long sessionExpiryInterval,
      int connectionTimeout,
      boolean automaticReconnect,
      Consumer<Mqtt5Publish> messageHandler)
      throws Throwable {
    super(
        "EDDTableFromMqtt",
        tDatasetID,
        tAccessibleTo,
        tGraphsAccessibleTo,
        tOnChange,
        tFgdcFile,
        tIso19115File,
        tSosOfferingPrefix,
        tDefaultDataQuery,
        tDefaultGraphQuery,
        tAddGlobalAttributes,
        tDataVariables,
        tReloadEveryNMinutes,
        tUpdateEveryNMillis,
        tFileDir,
        tFileNameRegex,
        tRecursive,
        tPathRegex,
        tMetadataFrom,
        tCharset,
        tSkipHeaderToRegex,
        tSkipLinesRegex,
        tColumnNamesRow,
        tFirstDataRow,
        tColumnSeparator,
        tPreExtractRegex,
        tPostExtractRegex,
        tExtractRegex,
        tColumnNameForExtract,
        tSortedColumnSourceName,
        tSortFilesBySourceNames,
        tSourceNeedsExpandedFP_EQ,
        tFileTableInMemory,
        tAccessibleViaFiles,
        tRemoveMVRows,
        tStandardizeWhat,
        tNThreads,
        tCacheFromUrl,
        tCacheSizeGB,
        tCachePartialPathRegex,
        tAddVariablesWhere);

    // Initialize column information from dataVariable definitions
    int nDV = dataVariables.length;
    columnNames = new String[nDV];
    columnPATypes = new PAType[nDV];
    for (int dvi = 0; dvi < nDV; dvi++) {
      EDV edv = dataVariables[dvi];
      columnNames[dvi] = edv.sourceName();
      columnPATypes[dvi] = edv.sourceDataPAType();
    }

    CompletableFuture<Mqtt5AsyncClient> response =
        initialiseMqttAsyncClient(
            serverHost,
            serverPort,
            clientId,
            username,
            password,
            useSsl,
            keepAlive,
            cleanStart,
            sessionExpiryInterval,
            connectionTimeout,
            automaticReconnect,
            messageHandler);
    Mqtt5AsyncClient asyncClient = response.join();

    subscribeToDatasetTopics(asyncClient, topics, MqttQos.AT_LEAST_ONCE);
  }

  public static CompletableFuture<Mqtt5AsyncClient> initialiseMqttAsyncClient(
      String serverHost,
      Integer serverPort,
      String clientId,
      String username,
      String password,
      boolean useSsl,
      int keepAlive,
      boolean cleanStart,
      long sessionExpiryInterval,
      int connectionTimeout,
      boolean automaticReconnect,
      Consumer<Mqtt5Publish> messageHandler) {

    // Generate client ID if not provided
    String effectiveClientId =
        (clientId != null && !clientId.isEmpty())
            ? clientId
            : "erddap-mqtt-" + UUID.randomUUID().toString();

    // Determine default port based on SSL usage
    int effectivePort = (serverPort != null) ? serverPort : (useSsl ? MQTT_SECURE_PORT : MQTT_PORT);

    // Build the MQTT client base configuration
    Mqtt5ClientBuilder clientBuilder =
        MqttClient.builder()
            .useMqttVersion5()
            .identifier(effectiveClientId)
            .serverHost(serverHost)
            .serverPort(effectivePort);

    // Configure SSL if enabled
    if (useSsl) {
      clientBuilder.sslWithDefaultConfig();
    }

    // Configure automatic reconnect - crucial for ERDDAP continuous operation
    if (automaticReconnect) {
      clientBuilder.automaticReconnectWithDefaultConfig();
    }

    // Build the async client instance
    Mqtt5AsyncClient client = clientBuilder.buildAsync();

    // Connect asynchronously using the fluent API
    return client
        .connectWith()
        .keepAlive(keepAlive)
        .cleanStart(cleanStart)
        .sessionExpiryInterval(sessionExpiryInterval)
        .simpleAuth()
        .username(username)
        .password(password != null ? password.getBytes() : null)
        .applySimpleAuth()
        .send()
        .orTimeout(connectionTimeout, TimeUnit.SECONDS)
        .thenApply(
            connAck -> {
              System.out.println("ERDDAP MQTT Client connected successfully!");
              System.out.println("Connection ACK Reason Code: " + connAck.getReasonCode());

              if (messageHandler != null) {
                client.publishes(MqttGlobalPublishFilter.ALL, messageHandler);
              }

              return client;
            })
        .exceptionally(
            throwable -> {
              System.err.println("Failed to connect ERDDAP MQTT client: " + throwable.getMessage());
              throw new RuntimeException("MQTT connection failed", throwable);
            });
  }

  public void subscribeToDatasetTopics(Mqtt5AsyncClient client, String[] topics, MqttQos qosLevel) {

    for (String topic : topics) {
      if (topic == null || topic.trim().isEmpty()) {
        continue;
      }
      client
          .subscribeWith()
          .topicFilter(topic)
          .qos(qosLevel)
          .callback(
              publish -> {
                try {
                  processMqttData(publish);
                } catch (Throwable t) {
                  String2.log(
                      String2.ERROR
                          + " processing MQTT message for datasetID="
                          + datasetID
                          + " from topic="
                          + publish.getTopic()
                          + "\n"
                          + MustBe.throwableToString(t));
                }
              })
          .send()
          .whenComplete(
              (suback, throwable) -> {
                if (throwable != null) {
                  System.err.println(
                      "Failed to subscribe to topic " + topic + ": " + throwable.getMessage());
                } else {
                  System.out.println("Successfully subscribed to topic: " + topic);
                  System.out.println("Subscription result: " + suback.getReasonCodes().get(0));
                }
              });
    }
  }

  /**
   * Processes a received MQTT message, parses its JSON payload, and appends it to a .jsonl file.
   *
   * @param publish the received MQTT message.
   */
  public void processMqttData(Mqtt5Publish publish) {
    String topic = publish.getTopic().toString();
    byte[] payload = publish.getPayloadAsBytes();
    if (payload == null || payload.length == 0) {
      return; // No data to process
    }
    String message = new String(payload, StandardCharsets.UTF_8);

    try {
      // Determine the target file path from the MQTT topic
      String fullFileName = getFilePathForTopic(topic);

      // Parse the JSON payload from the message
      JSONObject json = new JSONObject(message);

      // Create a new Table to hold the single row of data
      Table table = new Table();
      for (int i = 0; i < columnNames.length; i++) {
        String colName = columnNames[i];
        PAType paType = columnPATypes[i];
        PrimitiveArray pa = PrimitiveArray.factory(paType, 1, false);

        if (json.has(colName)) {
          pa.addString(json.get(colName).toString());
        } else {
          pa.addString(""); // Add missing value
        }
        table.addColumn(colName, pa);
      }

      // Append the data to the corresponding .jsonl file
      appendTableToJsonlFile(table, fullFileName);

    } catch (Exception e) {
      throw new RuntimeException("Error processing MQTT message from topic=" + topic, e);
    }
  }

  /**
   * Determines the full local file path for data from a given MQTT topic. The directory structure
   * mirrors the topic structure.
   *
   * @param topic The MQTT topic (e.g., "sensors/temp/room1").
   * @return The full path for the corresponding .jsonl file (e.g.,
   *     "/data/erddap/sensors/temp/room1.jsonl").
   */
  private String getFilePathForTopic(String topic) {
    if (topic.contains("..")) {
      throw new IllegalArgumentException("Invalid topic name (contains '..'): " + topic);
    }
    String relativePath = topic.replace('/', java.io.File.separatorChar) + ".jsonl";
    return fileDir + relativePath;
  }

  /**
   * Appends the data from a Table object to a specified JSONL file. This method is thread-safe and
   * handles file creation, including writing a header row for new files.
   *
   * @param table The Table containing the data rows to append.
   * @param fullFileName The absolute path of the target .jsonl file.
   * @throws IOException if a file I/O error occurs.
   * @throws TimeoutException if the file lock cannot be acquired.
   */
  private void appendTableToJsonlFile(Table table, String fullFileName)
      throws IOException, TimeoutException {
    String canonicalFileName = String2.canonical(fullFileName);
    ReentrantLock lock = String2.canonicalLock(canonicalFileName);
    try {
      if (!lock.tryLock(String2.longTimeoutSeconds, TimeUnit.SECONDS)) {
        throw new TimeoutException("Timeout waiting for lock on file: " + canonicalFileName);
      }
      try {
        java.io.File file = new java.io.File(canonicalFileName);
        boolean fileExists = file.exists();

        if (!fileExists) {
          // Ensure parent directory exists
          java.io.File parentDir = file.getParentFile();
          if (parentDir != null) {
            parentDir.mkdirs();
          }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (Writer writer = File2.getBufferedWriterUtf8(baos)) {
          // Manually construct the JSON array string for the header and data rows
          if (!fileExists) {
            // For a new file, write the column names as the first line (header)
            boolean somethingWritten = false;
            writer.write('[');
            for (int col = 0; col < table.nColumns(); col++) {
              if (somethingWritten) writer.write(',');
              writer.write(String2.toJson(table.getColumnName(col)));
              somethingWritten = true;
            }
            writer.write("]\n");
          }

          // Write each row of the table as a JSON array
          int nRows = table.nRows();
          int nCols = table.nColumns();
          for (int row = 0; row < nRows; row++) {
            boolean somethingWritten = false;
            writer.write('[');
            for (int col = 0; col < nCols; col++) {
              if (somethingWritten) writer.write(',');
              writer.write(table.getColumn(col).getJsonString(row));
              somethingWritten = true;
            }
            writer.write("]\n");
          }
        }

        // Append the contents of the ByteArrayOutputStream to the file
        try (BufferedOutputStream fos =
            new BufferedOutputStream(new FileOutputStream(file, true))) {
          fos.write(baos.toByteArray());
        }

      } finally {
        lock.unlock();
      }
    } catch (InterruptedException e) {
      // Preserve the interrupted status
      Thread.currentThread().interrupt();
      throw new RuntimeException(
          "Thread interrupted while waiting for file lock on " + canonicalFileName, e);
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }

  @Override
  public int defaultStandardizeWhat() {
    return DEFAULT_STANDARDIZEWHAT;
  }

  @Override
  public Table lowGetSourceDataFromFile(
      String tFileDir,
      String tFileName,
      StringArray sourceDataNames,
      String[] sourceDataTypes,
      double sortedSpacing,
      double minSorted,
      double maxSorted,
      StringArray sourceConVars,
      StringArray sourceConOps,
      StringArray sourceConValues,
      boolean getMetadata,
      boolean mustGetData)
      throws Throwable {
    // This method needs to be implemented based on how data retrieval will work.
    // For now, it delegates to the superclass's implementation which is suitable
    // for reading from the generated .jsonl files.
    // return super.lowGetSourceDataFromFile(
    //     tFileDir,
    //     tFileName,
    //     sourceDataNames,
    //     sourceDataTypes,
    //     sortedSpacing,
    //     minSorted,
    //     maxSorted,
    //     sourceConVars,
    //     sourceConOps,
    //     sourceConValues,
    //     getMetadata,
    //     mustGetData);
    return null;
  }
}
