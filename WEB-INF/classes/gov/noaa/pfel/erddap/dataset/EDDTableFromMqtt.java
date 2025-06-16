package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.StringArray;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientBuilder;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.dataset.metadata.LocalizedAttributes;
import gov.noaa.pfel.erddap.variable.DataVariableInfo;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class EDDTableFromMqtt extends EDDTableFromFiles {

  public static final int DEFAULT_STANDARDIZEWHAT = 0;
  private static final int MQTT_PORT = 8883;
  private static final int MQTT_SECURE_PORT = 1883;

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
      String clientId,
      String username,
      String password,
      boolean useSsl,
      int keepAlive,
      boolean cleanStart,
      long sessionExpiryInterval,
      int connectionTimeout,
      boolean automaticReconnect,
      Consumer<Mqtt5Publish> messageHandler)
      throws Throwable {
    super(
        "EDDTableFromMQTT",
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

    CompletableFuture<Mqtt5AsyncClient> response =
        initialiseMqttAsyncClient(
            serverHost,
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
  }

  public static CompletableFuture<Mqtt5AsyncClient> initialiseMqttAsyncClient(
      String serverHost,
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
    int effectivePort = useSsl ? MQTT_SECURE_PORT : MQTT_PORT;

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
        // Add authentication if provided
        // Only call simpleAuth() if username is provided
        // This will return an Mqtt5ConnectSimpleAuthBuilder
        .simpleAuth() // <--- This starts the simple authentication configuration
        .username(username) // Pass null if username is null
        .password(password != null ? password.getBytes() : null) // Pass null if password is null
        .applySimpleAuth() // <--- This finishes the simple authentication builder and returns to
        // Mqtt5ConnectBuilder.Send
        .send() // Send the CONNECT message
        .orTimeout(connectionTimeout, TimeUnit.SECONDS) // Set connection timeout
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
    throw new UnsupportedOperationException("Unimplemented method 'lowGetSourceDataFromFile'");
  }
}
