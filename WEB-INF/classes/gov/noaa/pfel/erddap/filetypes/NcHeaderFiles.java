package gov.noaa.pfel.erddap.filetypes;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.FloatArray;
import com.cohort.array.IntArray;
import com.cohort.array.PAOne;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.ShortArray;
import com.cohort.array.StringArray;
import com.cohort.array.UByteArray;
import com.cohort.array.UIntArray;
import com.cohort.array.UShortArray;
import com.cohort.util.File2;
import com.cohort.util.SimpleException;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.dataset.AxisDataAccessor;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.EDDTable;
import gov.noaa.pfel.erddap.dataset.GridDataAccessor;
import gov.noaa.pfel.erddap.dataset.TableWriter;
import gov.noaa.pfel.erddap.dataset.TableWriterAllWithMetadata;
import gov.noaa.pfel.erddap.util.EDMessages;
import gov.noaa.pfel.erddap.util.EDMessages.Message;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import gov.noaa.pfel.erddap.variable.EDVGridAxis;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.write.NetcdfFileFormat;
import ucar.nc2.write.NetcdfFormatWriter;

@FileTypeClass(
    fileTypeExtension = ".txt",
    fileTypeName = ".ncHeader",
    infoUrl = "https://linux.die.net/man/1/ncdump",
    versionAdded = "1.0.0",
    contentType = "text/plain",
    addContentDispositionHeader = false)
public class NcHeaderFiles extends FileTypeInterface {

  private Attribute.Builder attributeBuilder(String name, PrimitiveArray value) {
    Attribute.Builder attBuild = Attribute.builder(name);
    PAType attributeType = value.elementType();
    attBuild.setDataType(NcHelper.getNc3DataType(attributeType));
    if (value.size() == 1) {
      switch (attributeType) {
        case BOOLEAN:
          attBuild.setNumericValue(value.getInt(0), false);
          break;
        case BYTE:
          attBuild.setNumericValue(value.getInt(0), false);
          break;
        case CHAR:
          attBuild.setStringValue(value.getString(0));
          break;
        case DOUBLE:
          attBuild.setNumericValue(value.getDouble(0), false);
          break;
        case FLOAT:
          attBuild.setNumericValue(value.getFloat(0), false);
          break;
        case INT:
          attBuild.setNumericValue(value.getInt(0), false);
          break;
        case LONG:
          // For nc3, long is a double.
          attBuild.setNumericValue(value.getDouble(0), false);
          break;
        case SHORT:
          attBuild.setNumericValue((short) value.getInt(0), false);
          break;
        case STRING:
          attBuild.setStringValue(value.getString(0));
          break;
        case UBYTE:
          attBuild.setNumericValue(value.getInt(0), true);
          break;
        case UINT:
          attBuild.setNumericValue(value.getInt(0), true);
          break;
        case ULONG:
          // For nc3, ulong is a double.
          attBuild.setNumericValue(value.getDouble(0), true);
          break;
        case USHORT:
          attBuild.setNumericValue((short) value.getInt(0), true);
          break;
        default:
          throw new IllegalArgumentException(
              "NCHeader does not support type: " + attributeType.toString());
      }
    } else {
      List<Object> values = new ArrayList<>();
      switch (attributeType) {
        case BOOLEAN:
          byte[] boolArray = ((ByteArray) value).array;
          for (int i = 0; i < boolArray.length; i++) {
            values.add(boolArray[i]);
          }
          attBuild.setValues(values, false);
          break;
        case BYTE:
          byte[] byteArray = ((ByteArray) value).array;
          for (int i = 0; i < byteArray.length; i++) {
            values.add(byteArray[i]);
          }
          attBuild.setValues(values, false);
          break;
        case CHAR:
          String[] charArray = value.toStringArray();
          for (int i = 0; i < charArray.length; i++) {
            values.add(charArray[i]);
          }
          attBuild.setValues(values, false);
          break;
        case DOUBLE:
          // For nc3, long is a double.
          double[] doubleArray = value.toDoubleArray();
          for (int i = 0; i < doubleArray.length; i++) {
            values.add(doubleArray[i]);
          }
          attBuild.setValues(values, false);
          break;
        case FLOAT:
          float[] floatArray = ((FloatArray) value).array;
          for (int i = 0; i < floatArray.length; i++) {
            values.add(floatArray[i]);
          }
          attBuild.setValues(values, false);
          break;
        case INT:
          int[] intArray = ((IntArray) value).array;
          for (int i = 0; i < intArray.length; i++) {
            values.add(intArray[i]);
          }
          attBuild.setValues(values, false);
          break;
        case LONG:
          // For nc3, long is a double.
          double[] longArray = value.toDoubleArray();
          for (int i = 0; i < longArray.length; i++) {
            values.add(longArray[i]);
          }
          attBuild.setValues(values, false);
          break;
        case SHORT:
          short[] shortArray = ((ShortArray) value).array;
          for (int i = 0; i < shortArray.length; i++) {
            values.add(shortArray[i]);
          }
          attBuild.setValues(values, false);
          break;
        case STRING:
          String[] stringArray = value.toStringArray();
          for (int i = 0; i < stringArray.length; i++) {
            values.add(stringArray[i]);
          }
          attBuild.setValues(values, false);
          break;
        case UBYTE:
          byte[] uByteArray = ((UByteArray) value).array;
          for (int i = 0; i < uByteArray.length; i++) {
            values.add(uByteArray[i]);
          }
          attBuild.setValues(values, true);
          break;
        case UINT:
          int[] uIntArray = ((UIntArray) value).array;
          for (int i = 0; i < uIntArray.length; i++) {
            values.add(uIntArray[i]);
          }
          attBuild.setValues(values, true);
          break;
        case ULONG:
          // For nc3, ulong is a double.
          double[] uLongArray = value.toDoubleArray();
          for (int i = 0; i < uLongArray.length; i++) {
            values.add(uLongArray[i]);
          }
          attBuild.setValues(values, true);
          break;
        case USHORT:
          short[] ushortArray = ((UShortArray) value).array;
          for (int i = 0; i < ushortArray.length; i++) {
            values.add(ushortArray[i]);
          }
          attBuild.setValues(values, true);
          break;
        default:
          throw new IllegalArgumentException(
              "NCHeader does not support type: " + attributeType.toString());
      }
    }

    return attBuild;
  }

  private void buildAttributesFromMap(Variable.Builder<?> varBuilder, Attributes attributes) {
    String[] names = attributes.getNames();
    Arrays.sort(names);
    for (String name : names) {
      Attribute.Builder attBuild = attributeBuilder(name, attributes.get(name));
      varBuilder.addAttribute(attBuild.build());
    }
  }

  private void buildAttributesFromMap(
      Group.Builder rootGroup, Attributes attributes, Map<String, PrimitiveArray> eddAtts) {
    String[] names = attributes.getNames();

    List<String> attributeNameList = new ArrayList<>(eddAtts.keySet());
    attributeNameList.addAll(Arrays.asList(names));
    attributeNameList.sort(
        new Comparator<String>() {
          @Override
          public int compare(String o1, String o2) {
            return o1.compareToIgnoreCase(o2);
          }
        });

    for (String name : attributeNameList) {
      PrimitiveArray value = attributes.get(name);
      if (value != null) {
        Attribute.Builder attBuild = attributeBuilder(name, value);
        rootGroup.addAttribute(attBuild.build());
      } else {
        Attribute.Builder attBuild = attributeBuilder(name, eddAtts.get(name));
        rootGroup.addAttribute(attBuild.build());
      }
    }
  }

  private void buildAttributesFromMap(
      Variable.Builder<?> writerBuilder, Map<String, PrimitiveArray> attributes) {
    List<Map.Entry<String, PrimitiveArray>> attributeList = new ArrayList<>(attributes.entrySet());
    attributeList.sort(
        new Comparator<Map.Entry<String, PrimitiveArray>>() {
          @Override
          public int compare(
              Map.Entry<String, PrimitiveArray> o1, Map.Entry<String, PrimitiveArray> o2) {
            return o1.getKey().compareToIgnoreCase(o2.getKey());
          }
        });
    for (Map.Entry<String, PrimitiveArray> entry : attributeList) {
      Attribute.Builder attBuild = attributeBuilder(entry.getKey(), entry.getValue());
      writerBuilder.addAttribute(attBuild.build());
    }
  }

  @Override
  public void writeTableToStream(DapRequestInfo requestInfo) throws Throwable {
    if (EDStatic.config.ncHeaderMakeFile) {
      FileTypeInterface ncFiles = new NcFiles(true);
      ncFiles.writeTableToStream(requestInfo);
      return;
    }

    String cdlOutput = "";
    NetcdfFormatWriter.Builder writerBuilder =
        NetcdfFormatWriter.builder()
            .setFormat(NetcdfFileFormat.NETCDF3) // Set format
            .setLocation(requestInfo.dir() + requestInfo.fileName() + ".nc");

    Group.Builder rootGroup = Group.builder();

    EDDTable eddTable = requestInfo.getEDDTable();
    if (eddTable == null) {
      throw new SimpleException(
          EDStatic.bilingual(
              requestInfo.language(),
              EDStatic.messages.get(Message.QUERY_ERROR, 0)
                  + EDStatic.messages.get(Message.ERROR_INTERNAL, 0),
              EDStatic.messages.get(Message.QUERY_ERROR, requestInfo.language())
                  + EDStatic.messages.get(Message.ERROR_INTERNAL, requestInfo.language())));
    }
    try (TableWriterAllWithMetadata tableWriter =
        new TableWriterAllWithMetadata(
            requestInfo.language(),
            eddTable,
            requestInfo.newHistory(),
            requestInfo.dir(),
            requestInfo.fileName() + "_temp")) {
      if (tableWriter != null) {
        TableWriter tTableWriter =
            eddTable.encloseTableWriter(
                requestInfo.language(),
                true, // alwaysDoAll
                requestInfo.dir(),
                requestInfo.fileName(),
                tableWriter,
                requestInfo.requestUrl(),
                requestInfo.userDapQuery());
        if (eddTable.handleViaFixedOrSubsetVariables(
            requestInfo.language(),
            requestInfo.loggedInAs(),
            requestInfo.requestUrl(),
            requestInfo.userDapQuery(),
            tTableWriter)) {
        } else {
          if (tTableWriter != tableWriter)
            tTableWriter =
                eddTable.encloseTableWriter(
                    requestInfo.language(),
                    false, // alwaysDoAll
                    requestInfo.dir(),
                    requestInfo.fileName(),
                    tableWriter,
                    requestInfo.requestUrl(),
                    requestInfo.userDapQuery());
          eddTable.getDataForDapQuery(
              requestInfo.language(),
              requestInfo.loggedInAs(),
              requestInfo.requestUrl(),
              requestInfo.userDapQuery(),
              tTableWriter);
        }
        Table table = tableWriter.cumulativeTable();
        tTableWriter.close();
        tableWriter.close();

        buildAttributesFromMap(
            rootGroup,
            table.globalAttributes(),
            requestInfo.edd().combinedGlobalAttributes().toMap(EDMessages.DEFAULT_LANGUAGE));

        Dimension obsDim = Dimension.builder("row", table.nRows()).build();
        rootGroup.addDimension(obsDim);

        for (int i = 0; i < table.nColumns(); i++) {

          Variable.Builder<?> varBuilder;
          PAType columnType = table.getColumn(i).elementType();

          Attributes varAtts = table.columnAttributes(i);

          // Special handling for NetCDF-3 strings (CHAR arrays)
          if (columnType == PAType.STRING) {
            Dimension strDim =
                Dimension.builder(
                        table.getColumnName(i) + "_strlen",
                        ((StringArray) table.getColumn(i)).maxStringLength())
                    .build();

            // Add this dimension to the rootGroup builder
            rootGroup.addDimension(strDim);

            varBuilder =
                Variable.builder()
                    .setName(table.getColumnName(i))
                    .setDataType(NcHelper.getNc3DataType(PAType.CHAR))
                    .setDimensions(List.of(obsDim, strDim));

            varAtts.add("_Encoding", PAOne.fromString("ISO-8859-1").pa());

          } else {
            // Standard numeric case
            varBuilder =
                Variable.builder()
                    .setName(table.getColumnName(i))
                    .setDataType(NcHelper.getNc3DataType(columnType))
                    .setDimensions(List.of(obsDim));
          }

          // Add all attributes to the *variable* builder
          buildAttributesFromMap(varBuilder, varAtts);

          // Add the fully-built variable to the rootGroup builder
          rootGroup.addVariable(varBuilder);
        }
        writerBuilder.setRootGroup(rootGroup);

        // Build the writer and extract the header
        // writerBuilder.build() creates the in-memory object.
        try (NetcdfFormatWriter writer = writerBuilder.build()) {

          // Get the NetcdfFile object from the writer
          // and call toString() to get the CDL header.
          cdlOutput = writer.getOutputFile().toString();

          try (Writer outputWriter =
              File2.getBufferedWriter88591(
                  requestInfo.outputStream().outputStream(File2.ISO_8859_1))) {
            outputWriter.append(cdlOutput);
            outputWriter.flush();
          }
        }
      }
    }
  }

  private void writeGridAxis(
      EDVGridAxis axis,
      PrimitiveArray pa,
      List<Dimension> builtDimensions,
      Group.Builder rootGroup,
      NetcdfFormatWriter.Builder writerBuilder) {
    String dimName = axis.destinationName();
    Dimension newDim = Dimension.builder(dimName, pa.size()).build();
    writerBuilder.addDimension(newDim);
    builtDimensions.add(newDim); // Save for later
    rootGroup.addDimension(newDim);

    String[] minMax = pa.getNMinMax();
    Map<String, PrimitiveArray> varAtts =
        axis.combinedAttributes().toMap(EDMessages.DEFAULT_LANGUAGE);
    PrimitiveArray minMaxPA = PrimitiveArray.factory(pa.elementType(), 0, minMax[1]);
    minMaxPA.addString(minMax[1]);
    minMaxPA.addString(minMax[2]);
    varAtts.put("actual_range", minMaxPA);

    Variable.Builder<?> varBuilder =
        Variable.builder()
            .setName(axis.destinationName())
            .setDataType(NcHelper.getNc3DataType(axis.destinationDataPAType()))
            .setDimensions(List.of(newDim));
    buildAttributesFromMap(varBuilder, varAtts);
    rootGroup.addVariable(varBuilder);
  }

  private void writeGridDataVarialbe(
      EDV variable,
      List<Dimension> builtDimensions,
      Group.Builder rootGroup,
      NetcdfFormatWriter.Builder writerBuilder) {
    // Create a builder for *this variable*
    Variable.Builder<?> varBuilder;
    Map<String, PrimitiveArray> varAtts =
        variable.combinedAttributes().toMap(EDMessages.DEFAULT_LANGUAGE);

    // Special handling for NetCDF-3 strings (CHAR arrays)
    if (variable.destinationDataPAType() == PAType.CHAR
        || variable.destinationDataPAType() == PAType.STRING) {
      Dimension strDim =
          Dimension.builder(
                  variable.destinationName() + "_strlen",
                  10 // Does this matter? Using 10 as a placeholder.
                  )
              .build();

      // Add this dimension to the *file* builder
      writerBuilder.addDimension(strDim);

      varBuilder =
          Variable.builder()
              .setName(variable.destinationName())
              .setDataType(NcHelper.getNc3DataType(PAType.CHAR))
              .setDimensions(builtDimensions);

      varAtts.put("_Encoding", PAOne.fromString("ISO-8859-1").pa());

    } else {
      // Standard numeric case
      varBuilder =
          Variable.builder()
              .setName(variable.destinationName())
              .setDataType(NcHelper.getNc3DataType(variable.destinationDataPAType()))
              .setDimensions(builtDimensions);
    }

    // Add all attributes to the *variable* builder
    buildAttributesFromMap(varBuilder, varAtts);

    // Add the fully-built variable to the *file* builder
    rootGroup.addVariable(varBuilder);
  }

  @Override
  public void writeGridToStream(DapRequestInfo requestInfo) throws Throwable {
    if (EDStatic.config.ncHeaderMakeFile) {
      FileTypeInterface ncFiles = new NcFiles(true);
      ncFiles.writeGridToStream(requestInfo);
      return;
    }

    String cdlOutput = "";
    NetcdfFormatWriter.Builder writerBuilder =
        NetcdfFormatWriter.builder()
            .setFormat(NetcdfFileFormat.NETCDF3)
            .setLocation(requestInfo.dir() + requestInfo.fileName() + ".nc");

    Group.Builder rootGroup = Group.builder();

    // This list is crucial: it holds the dimensions
    List<Dimension> builtDimensions = new ArrayList<>();

    EDDGrid eddGrid = requestInfo.getEDDGrid();

    long rows = 1;
    // handle axisDapQuery
    if (eddGrid.isAxisDapQuery(requestInfo.userDapQuery())) {
      AxisDataAccessor ada =
          new AxisDataAccessor(
              requestInfo.language(),
              eddGrid,
              requestInfo.requestUrl(),
              requestInfo.userDapQuery());
      buildAttributesFromMap(
          rootGroup,
          ada.globalAttributes(),
          requestInfo.edd().combinedGlobalAttributes().toMap(EDMessages.DEFAULT_LANGUAGE));
      int nRAV = ada.nRequestedAxisVariables();
      for (int av = 0; av < nRAV; av++) {
        int dimSize = ada.axisValues(av).size();
        rows = rows * dimSize;
        EDVGridAxis axis = ada.axisVariables(av);
        writeGridAxis(axis, ada.axisValues(av), builtDimensions, rootGroup, writerBuilder);
      }

      for (EDV variable : requestInfo.edd().dataVariables()) {
        if (ada.destinationNames().indexOf(variable.destinationName()) != -1) {
          writeGridDataVarialbe(variable, builtDimensions, rootGroup, writerBuilder);
        }
      }
    } else {
      // handle gridDapQuery
      try (GridDataAccessor mainGda =
          new GridDataAccessor(
              requestInfo.language(),
              eddGrid,
              requestInfo.requestUrl(),
              requestInfo.userDapQuery(),
              false,
              true)) {
        buildAttributesFromMap(
            rootGroup,
            mainGda.globalAttributes(),
            requestInfo.edd().combinedGlobalAttributes().toMap(EDMessages.DEFAULT_LANGUAGE));
        int nRAV = eddGrid.axisVariables().length;
        for (int av = 0; av < nRAV; av++) {
          int dimSize = mainGda.axisValues(av).size();
          rows = rows * dimSize;
          EDVGridAxis axis = eddGrid.axisVariables()[av];
          writeGridAxis(axis, mainGda.axisValues(av), builtDimensions, rootGroup, writerBuilder);
        }

        for (EDV variable : mainGda.dataVariables()) {
          writeGridDataVarialbe(variable, builtDimensions, rootGroup, writerBuilder);
        }
      }
    }
    writerBuilder.setRootGroup(rootGroup);

    try (NetcdfFormatWriter writer = writerBuilder.build()) {
      cdlOutput = writer.getOutputFile().toString();
    }

    try (Writer outputWriter =
        File2.getBufferedWriter88591(requestInfo.outputStream().outputStream(File2.ISO_8859_1))) {
      outputWriter.append(cdlOutput);
      outputWriter.flush();
    }
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.get(Message.FILE_HELP_NC3_HEADER, language);
  }

  @Override
  public String getGridHelpText(int language) {
    return EDStatic.messages.get(Message.FILE_HELP_NC3_HEADER, language);
  }
}
