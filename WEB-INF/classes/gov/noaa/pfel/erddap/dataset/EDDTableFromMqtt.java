package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.DoubleArray;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.ShortArray;
import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
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
import gov.noaa.pfel.erddap.util.EDMessages;
import gov.noaa.pfel.erddap.variable.DataVariableInfo;
import gov.noaa.pfel.erddap.variable.EDV;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
  protected boolean[] columnIsFixed;
  protected PrimitiveArray[] columnMvFv;

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
    int language = EDMessages.DEFAULT_LANGUAGE;
    int nDV = dataVariables.length;
    columnNames = new String[nDV];
    columnPATypes = new PAType[nDV];
    columnIsFixed = new boolean[nDV];
    columnMvFv = new PrimitiveArray[nDV];

    for (int dvi = 0; dvi < nDV; dvi++) {
      EDV edv = dataVariables[dvi];
      columnNames[dvi] = edv.sourceName();
      columnPATypes[dvi] = edv.sourceDataPAType();
      columnIsFixed[dvi] = edv.sourceName().startsWith("=");

      // from EDDTableFromHttpGet: get mv/fv values for stats calculations
      if (columnPATypes[dvi] == PAType.STRING) {
        StringArray tsa = new StringArray(2, false);
        if (edv.stringMissingValue().length() > 0) tsa.add(edv.stringMissingValue());
        if (edv.stringFillValue().length() > 0) tsa.add(edv.stringFillValue());
        columnMvFv[dvi] = tsa.size() == 0 ? null : tsa;
      } else if (columnPATypes[dvi] == PAType.LONG || columnPATypes[dvi] == PAType.ULONG) {
        StringArray tsa = new StringArray(2, false);
        String ts = edv.combinedAttributes().getString(language, "missing_value");
        if (ts != null) tsa.add(ts);
        ts = edv.combinedAttributes().getString(language, "_FillValue");
        if (ts != null) tsa.add(ts);
        columnMvFv[dvi] = tsa.size() == 0 ? null : tsa;
      } else {
        DoubleArray tda = new DoubleArray(2, false);
        if (!Double.isNaN(edv.sourceMissingValue())) tda.add(edv.sourceMissingValue());
        if (!Double.isNaN(edv.sourceFillValue())) tda.add(edv.sourceFillValue());
        columnMvFv[dvi] = tda.size() == 0 ? null : tda;
      }
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

      // After successful write, update the fileTable with statistics for the new data
      updateFileTableWithStats(table, fullFileName);

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
          java.io.File parentDir = file.getParentFile();
          if (parentDir != null) {
            parentDir.mkdirs();
          }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (Writer writer = File2.getBufferedWriterUtf8(baos)) {
          if (!fileExists) {
            boolean somethingWritten = false;
            writer.write('[');
            for (int col = 0; col < table.nColumns(); col++) {
              if (somethingWritten) writer.write(',');
              writer.write(String2.toJson(table.getColumnName(col)));
              somethingWritten = true;
            }
            writer.write("]\n");
          }

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

        try (BufferedOutputStream fos =
            new BufferedOutputStream(new FileOutputStream(file, true))) {
          fos.write(baos.toByteArray());
        }

      } finally {
        lock.unlock();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(
          "Thread interrupted while waiting for file lock on " + canonicalFileName, e);
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }

  /**
   * Updates the in-memory fileTable with min/max statistics from a new batch of data. This method
   * is ported from EDDTableFromHttpGet.
   *
   * @param newData a Table holding the new rows that were just written
   * @param fullFileName the full path of the file that was written to
   * @throws TimeoutException
   */
  private void updateFileTableWithStats(Table newData, String fullFileName)
      throws TimeoutException {
    if (fileTable == null) {
      return;
    }

    // This entire block is adapted from EDDTableFromHttpGet.insertOrDelete
    int nColumns = newData.nColumns();
    int nRows = newData.nRows();
    if (nRows == 0) {
      return;
    }

    // 1. Calculate statistics for the new data
    String[] columnMinString = new String[nColumns];
    String[] columnMaxString = new String[nColumns];
    long[] columnMinLong = new long[nColumns];
    long[] columnMaxLong = new long[nColumns];
    BigInteger[] columnMinULong = new BigInteger[nColumns];
    BigInteger[] columnMaxULong = new BigInteger[nColumns];
    double[] columnMinDouble = new double[nColumns];
    double[] columnMaxDouble = new double[nColumns];
    boolean[] columnHasNaN = new boolean[nColumns];

    Arrays.fill(columnMinString, "\uFFFF");
    Arrays.fill(columnMaxString, "\u0000");
    Arrays.fill(columnMinLong, Long.MAX_VALUE);
    Arrays.fill(columnMaxLong, Long.MIN_VALUE);
    Arrays.fill(columnMinULong, Math2.ULONG_MAX_VALUE);
    Arrays.fill(columnMaxULong, BigInteger.ZERO);
    Arrays.fill(columnMinDouble, Double.MAX_VALUE);
    Arrays.fill(columnMaxDouble, -Double.MAX_VALUE);

    for (int row = 0; row < nRows; row++) {
      for (int col = 0; col < nColumns; col++) {
        PrimitiveArray pa = newData.getColumn(col);
        String sValue = pa.getString(row);
        if (sValue.length() == 0
            || (columnMvFv[col] != null && columnMvFv[col].indexOf(sValue) >= 0)) {
          columnHasNaN[col] = true;
          continue;
        }

        if (columnPATypes[col] == PAType.STRING) {
          if (sValue.compareTo(columnMinString[col]) < 0) columnMinString[col] = sValue;
          if (sValue.compareTo(columnMaxString[col]) > 0) columnMaxString[col] = sValue;
        } else if (columnPATypes[col] == PAType.LONG) {
          long l = pa.getLong(row);
          if (l < columnMinLong[col]) columnMinLong[col] = l;
          if (l > columnMaxLong[col]) columnMaxLong[col] = l;
        } else if (columnPATypes[col] == PAType.ULONG) {
          BigInteger bi = pa.getULong(row);
          if (bi.compareTo(columnMinULong[col]) < 0) columnMinULong[col] = bi;
          if (bi.compareTo(columnMaxULong[col]) > 0) columnMaxULong[col] = bi;
        } else { // numeric types
          double d = pa.getDouble(row);
          if (d < columnMinDouble[col]) columnMinDouble[col] = d;
          if (d > columnMaxDouble[col]) columnMaxDouble[col] = d;
        }
      }
    }

    // 2. Update the fileTable with the new statistics
    ReentrantLock lock = String2.canonicalLock(fileTable);
    try {
      if (!lock.tryLock(String2.longTimeoutSeconds, TimeUnit.SECONDS)) {
        throw new TimeoutException(
            "Timeout waiting for lock on fileTable in updateFileTableWithStats.");
      }
      try {
        String fileDir = File2.getDirectory(fullFileName);
        String fileName = File2.getNameAndExtension(fullFileName);

        // Find or add directory row
        int dirTableRow = dirTable.getColumn(0).indexOf(fileDir);
        if (dirTableRow < 0) {
          dirTableRow = dirTable.nRows();
          dirTable.getColumn(0).addString(fileDir);
        }

        // Find or add file row
        int fileTableRow = 0;
        ShortArray fileTableDirPA = (ShortArray) fileTable.getColumn(FT_DIR_INDEX_COL);
        StringArray fileTableNamePA = (StringArray) fileTable.getColumn(FT_FILE_LIST_COL);
        int fileTableNRows = fileTable.nRows();
        while (fileTableRow < fileTableNRows
            && (fileTableDirPA.get(fileTableRow) != dirTableRow
                || !fileTableNamePA.get(fileTableRow).equals(fileName))) {
          fileTableRow++;
        }

        if (fileTableRow == fileTableNRows) {
          // It's a new file, add a new row to fileTable
          fileTable.getColumn(FT_DIR_INDEX_COL).addInt(dirTableRow);
          fileTable.getColumn(FT_FILE_LIST_COL).addString(fileName);
          fileTable.getColumn(FT_LAST_MOD_COL).addLong(0);
          fileTable.getColumn(FT_SIZE_COL).addLong(0);
          fileTable.getColumn(FT_SORTED_SPACING_COL).addDouble(Double.NaN); // N/A for MQTT

          for (int col = 0; col < nColumns; col++) {
            int baseFTC = dv0 + col * 3;
            PrimitiveArray minCol = fileTable.getColumn(baseFTC);
            PrimitiveArray maxCol = fileTable.getColumn(baseFTC + 1);

            if (columnIsFixed[col]) {
              String fixedVal = columnNames[col].substring(1);
              minCol.addString(fixedVal);
              maxCol.addString(fixedVal);
            } else if (columnPATypes[col] == PAType.STRING) {
              minCol.addString(columnMinString[col].equals("\uFFFF") ? "" : columnMinString[col]);
              maxCol.addString(columnMaxString[col].equals("\u0000") ? "" : columnMaxString[col]);
            } else if (columnPATypes[col] == PAType.LONG) {
              minCol.addLong(
                  columnMinLong[col] == Long.MAX_VALUE ? Long.MAX_VALUE : columnMinLong[col]);
              maxCol.addLong(
                  columnMaxLong[col] == Long.MIN_VALUE ? Long.MAX_VALUE : columnMaxLong[col]);
            } else if (columnPATypes[col] == PAType.ULONG) {
              minCol.addString(
                  columnMinULong[col].equals(Math2.ULONG_MAX_VALUE)
                      ? ""
                      : columnMinULong[col].toString());
              maxCol.addString(
                  columnMaxULong[col].equals(BigInteger.ZERO)
                      ? ""
                      : columnMaxULong[col].toString());
            } else { // numeric
              minCol.addDouble(
                  columnMinDouble[col] == Double.MAX_VALUE ? Double.NaN : columnMinDouble[col]);
              maxCol.addDouble(
                  columnMaxDouble[col] == -Double.MAX_VALUE ? Double.NaN : columnMaxDouble[col]);
            }
            fileTable.getColumn(baseFTC + 2).addInt(columnHasNaN[col] ? 1 : 0);
          }
        } else {
          // File exists, update the existing row
          for (int col = 0; col < nColumns; col++) {
            if (columnIsFixed[col]) continue;

            int baseFTC = dv0 + col * 3;
            PrimitiveArray minCol = fileTable.getColumn(baseFTC);
            PrimitiveArray maxCol = fileTable.getColumn(baseFTC + 1);

            if (columnPATypes[col] == PAType.STRING) {
              if (!columnMinString[col].equals("\uFFFF")
                  && minCol.getString(fileTableRow).compareTo(columnMinString[col]) > 0)
                minCol.setString(fileTableRow, columnMinString[col]);
              if (!columnMaxString[col].equals("\u0000")
                  && maxCol.getString(fileTableRow).compareTo(columnMaxString[col]) < 0)
                maxCol.setString(fileTableRow, columnMaxString[col]);
            } else if (columnPATypes[col] == PAType.LONG) {
              if (columnMinLong[col] != Long.MAX_VALUE
                  && minCol.getLong(fileTableRow) > columnMinLong[col])
                minCol.setLong(fileTableRow, columnMinLong[col]);
              if (columnMaxLong[col] != Long.MIN_VALUE
                  && maxCol.getLong(fileTableRow) < columnMaxLong[col])
                maxCol.setLong(fileTableRow, columnMaxLong[col]);
            } else if (columnPATypes[col] == PAType.ULONG) {
              // ULong stored as String in fileTable
            } else { // numeric
              if (columnMinDouble[col] != Double.MAX_VALUE
                  && minCol.getDouble(fileTableRow) > columnMinDouble[col])
                minCol.setDouble(fileTableRow, columnMinDouble[col]);
              if (columnMaxDouble[col] != -Double.MAX_VALUE
                  && maxCol.getDouble(fileTableRow) < columnMaxDouble[col])
                maxCol.setDouble(fileTableRow, columnMaxDouble[col]);
            }
            if (columnHasNaN[col]) {
              fileTable.getColumn(baseFTC + 2).setInt(fileTableRow, 1);
            }
          }
        }

        // Update lastMod and size
        java.io.File file = new java.io.File(fullFileName);
        fileTable.getColumn(FT_LAST_MOD_COL).setLong(fileTableRow, file.lastModified());
        fileTable.getColumn(FT_SIZE_COL).setLong(fileTableRow, file.length());

      } finally {
        lock.unlock();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Thread interrupted while waiting for fileTable lock", e);
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
