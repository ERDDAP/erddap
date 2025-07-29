package gov.noaa.pfel.erddap.filetypes;

import com.cohort.array.Attributes;
import com.cohort.array.CharArray;
import com.cohort.array.NDimensionalIndex;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.erddap.dataset.AxisDataAccessor;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.EDDTable;
import gov.noaa.pfel.erddap.dataset.GridDataAccessor;
import gov.noaa.pfel.erddap.dataset.GridDataAllAccessor;
import gov.noaa.pfel.erddap.dataset.OutputStreamSource;
import gov.noaa.pfel.erddap.dataset.TableWriter;
import gov.noaa.pfel.erddap.dataset.TableWriterAllWithMetadata;
import gov.noaa.pfel.erddap.util.EDMessages.Message;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import gov.noaa.pfel.erddap.variable.EDVGridAxis;
import java.io.BufferedWriter;
import java.io.DataInputStream;

@FileTypeClass(
    fileTypeExtension = ".json",
    fileTypeName = ".ncoJson",
    infoUrl = "https://nco.sourceforge.net/nco.html#json",
    versionAdded = "1.82.0",
    contentType = "application/json",
    addContentDispositionHeader = false)
public class NcoJsonFiles extends TableWriterFileType {

  @Override
  public TableWriter generateTableWriter(DapRequestInfo requestInfo) {
    return new TableWriterAllWithMetadata(
        requestInfo.language(),
        requestInfo.edd(),
        requestInfo.newHistory(),
        requestInfo.dir(),
        requestInfo.fileName());
  }

  @Override
  public void writeTableToFileFormat(DapRequestInfo requestInfo, TableWriter tableWriter)
      throws Throwable {
    if (tableWriter instanceof TableWriterAllWithMetadata) {
      String jsonp = EDStatic.getJsonpFromQuery(requestInfo.language(), requestInfo.userDapQuery());
      saveAsNcoJson(requestInfo.outputStream(), (TableWriterAllWithMetadata) tableWriter, jsonp);
    }
  }

  @Override
  public void writeGridToStream(DapRequestInfo requestInfo) throws Throwable {
    saveAsNcoJson(
        requestInfo.language(),
        requestInfo.requestUrl(),
        requestInfo.userDapQuery(),
        requestInfo.outputStream(),
        requestInfo.getEDDGrid());
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.get(Message.FILE_HELP_NCO_JSON, language);
  }

  /**
   * Save the TableWriterAllWithMetadata data as an NCO .json lvl=2 pedantic file.
   * https://nco.sourceforge.net/nco.html#json
   *
   * <p>Issues (that I have raised with Charlie Zender):
   *
   * <ul>
   *   <li>How should NaN data values be represented? The sample file
   *       http://dust.ess.uci.edu/tmp/in.json.fmt2 has comments about nan for vars nan_arr and
   *       nan_scl. Basically, it says they are hard to work with so "comment them out", which is
   *       not helpful for my purposes. Further test: If I go to https://jsonlint.com/ and enter [1,
   *       2.0, 1e30], it says it is valid. If I enter [1, 2.0, NaN, 1e30], it says NaN is not
   *       valid. If I enter [1, 2.0, null, 1e30], it says it is valid. See also
   *       https://stackoverflow.com/questions/15228651/how-to-parse-json-string-containing-nan-in-node-js
   *       So my code (PrimitiveArray.toJsonCsvString()) represents them as null. Charlie Zender now
   *       agrees and will change NCO behavior: use null.
   *   <li>char vs String (collapse rightmost dimension) variables? I see that for data variables,
   *       NCO json mimics what is in nc the file and represents the rightmost dimension's chunks as
   *       strings (see below). But that approach doesn't handle the netcdf-4's clear distinction
   *       between char and String variables. I see there are examples with data "type"="string" in
   *       http://dust.ess.uci.edu/tmp/in_grp.json.fmt2 ("string_arr"), but no examples of "string"
   *       attributes. For now, I'll write string vars as if in nc3 file: char arrays with extra
   *       dimension for strlen. See writeStringsAsStrings below. char attributes are always written
   *       as chars.
   *   <li>The example of a representation of a NUL char in http://dust.ess.uci.edu/tmp/in.json.fmt2
   *       seems wrong. How is this different than the character 0 (zero)? I think it should be
   *       json-encoded as "\u0000" (If this were a string (which it isn't here), NUL is a
   *       terminator so it might be represented as ""). "char_var_nul": { "type": "char",
   *       "attributes": { "long_name": { "type": "char", "data": "Character variable containing one
   *       NUL"} }, "data": "0" }, My code writes the json encoding e.g., "\u0000". Charlie Zender
   *       is thinking about this.
   * </ul>
   *
   * <p>See test of this in EDDTableFromNccsvFiles.testChar().
   *
   * @param language the index of the selected language
   * @param outputStreamSource
   * @param twawm all the results data, with missingValues stored as destinationMissingValues or
   *     destinationFillValues (they are converted to NaNs)
   * @param jsonp the not-percent-encoded jsonp functionName to be prepended to the results (or null
   *     if none). See https://niryariv.wordpress.com/2009/05/05/jsonp-quickly/ and
   *     https://bob.pythonmac.org/archives/2005/12/05/remote-json-jsonp/ and
   *     https://www.raymondcamden.com/2014/03/12/Reprint-What-in-the-heck-is-JSONP-and-why-would-you-use-it/
   *     . A SimpleException will be thrown if tJsonp is not null but isn't
   *     String2.isVariableNameSafe.
   * @throws Throwable
   */
  private void saveAsNcoJson(
      OutputStreamSource outputStreamSource, TableWriterAllWithMetadata twawm, String jsonp)
      throws Throwable {
    if (EDDTable.reallyVerbose) String2.log("EDDTable.saveAsNcoJson");
    long time = System.currentTimeMillis();

    // for now, write strings as if in nc3 file: char arrays with extra dimension for strlen
    boolean writeStringsAsStrings = false; // if false, they are written as chars
    String stringOpenBracket = writeStringsAsStrings ? "" : "[";
    String stringCloseBracket = writeStringsAsStrings ? "" : "]";

    // make sure there is data
    long nRows = twawm.nRows();
    if (nRows == 0)
      throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (at start of saveAsNcoJson)");
    int nCols = twawm.nColumns();

    // create a writer
    try (BufferedWriter writer =
        File2.getBufferedWriterUtf8(outputStreamSource.outputStream(File2.UTF_8))) {
      if (jsonp != null) writer.write(jsonp + "(");

      // write start
      writer.write("{\n");

      // write the global attributes
      writer.write(twawm.globalAttributes().toNcoJsonString("  "));

      // write row dimension
      // {
      //  "dimensions": {
      //    "row": 10,
      //    "bnd": 2
      //  }
      writer.write("  \"dimensions\": {\n" + "    \"row\": " + nRows);
      String stringDim[] = new String[nCols];
      if (!writeStringsAsStrings) {
        for (int col = 0; col < nCols; col++) {
          boolean isString = twawm.columnType(col) == PAType.STRING;
          if (isString) {
            stringDim[col] = String2.toJson(twawm.columnName(col) + NcHelper.StringLengthSuffix);
            writer.write(
                ",\n"
                    + // end of previous line
                    "    "
                    + stringDim[col]
                    + ": "
                    + Math.max(1, twawm.columnMaxStringLength(col)));
          }
        }
      }
      writer.write(
          "\n" + // end of previous line
              "  },\n"); // end of dimensions

      // write the variables
      writer.write("  \"variables\": {\n");

      StringBuilder ssb = new StringBuilder(); // reused for string variables
      for (int col = 0; col < nCols; col++) {
        //    "att_var": {
        //      "shape": ["time"],
        //      "type": "float",
        //      "attributes": { ... },
        //      "data": [10.0, 10.10, 10.20, 10.30, 10.40101, 10.50, 10.60, 10.70, 10.80, 10.990]
        //    }
        Attributes atts = twawm.columnAttributes(col);
        String tType = PAType.toCohortString(twawm.columnType(col));
        boolean isString = tType.equals("String");
        boolean isChar = tType.equals("char");
        int bufferSize =
            (int) Math.min(isChar ? 8192 : 10, nRows); // this is also nPerLine for all except char
        if (isString) tType = writeStringsAsStrings ? "string" : "char";
        else if (tType.equals("long")) tType = "int64"; // see
        // https://www.unidata.ucar.edu/software/netcdf/docs/netcdf_utilities_guide.html#cdl_data_types and NCO JSON examples
        else if (tType.equals("ulong")) tType = "uint64";
        writer.write(
            "    "
                + String2.toJson(twawm.columnName(col))
                + ": {\n"
                + "      \"shape\": [\"row\""
                + (isString && !writeStringsAsStrings ? ", " + stringDim[col] : "")
                + "],\n"
                + "      \"type\": \""
                + tType
                + "\",\n");
        writer.write(atts.toNcoJsonString("      "));
        writer.write("      \"data\": [");
        try (DataInputStream dis = twawm.dataInputStream(col)) {
          // create the bufferPA
          PrimitiveArray pa = null;
          long nRowsRead = 0;
          while (nRowsRead < nRows) {
            int nToRead = (int) Math.min(bufferSize, nRows - nRowsRead);
            if (pa == null) {
              pa = twawm.columnEmptyPA(col);
              pa.ensureCapacity(nToRead);
            }
            pa.clear();
            pa.setMaxIsMV(twawm.columnMaxIsMV(col)); // reset after clear()
            pa.readDis(dis, nToRead);
            if (isChar) {
              // write it as one string with chars concatenated
              // see "md5_abc" in in http://dust.ess.uci.edu/tmp/in.json.fmt2
              //  "shape": ["lev"],          //dim lev size=3
              //  ...
              //  "data": ["abc"]
              // here: write it in buffersize chunks
              if (nRowsRead == 0) writer.write("\""); // start the string
              String s = String2.toJson(new String(((CharArray) pa).toArray()));
              writer.write(s.substring(1, s.length() - 1)); // remove start and end "
            } else if (isString) {
              // Arrays of Strings are written oddly: (example from
              // http://dust.ess.uci.edu/tmp/in.json.fmt2)
              //    "date_rec": {
              //      "shape": ["time", "char_dmn_lng26"],
              //      "type": "char",
              //      "attributes": ...,
              //      "data": [["2010-11-01T00:00:00.000000"], ["2010-11-01T01:00:00.000000"],
              // ["2010-11-01T02:00:00.000000"], ["2010-11-01T03:00:00.000000"],
              // ["2010-11-01T04:00:00.000000"], ["2010-11-01T05:00:00.000000"],
              // ["2010-11-01T06:00:00.000000"], ["2010-11-01T07:00:00.000000"],
              // ["2010-11-01T08:00:00.000000"], ["2010-11-01T09:00:00.000000"]]
              //    },
              if (nRowsRead > 0) writer.write(",\n    "); // separate and write on next line
              ssb.setLength(0);
              for (int row = 0; row < nToRead; row++)
                ssb.append(
                    (row == 0 ? "" : ", ")
                        + stringOpenBracket
                        + String2.toJson(pa.getString(row))
                        + stringCloseBracket);
              writer.write(ssb.toString());
            } else {
              if (nRowsRead > 0) writer.write(",\n    "); // separate and write on next line
              writer.write(pa.toJsonCsvString());
            }
            nRowsRead += nToRead;
          }
        }
        if (isChar) writer.write('\"'); // terminate the string
        writer.write(
            "]\n"
                + // end of data
                "    }"
                + (col < nCols - 1 ? ",\n" : "\n")); // end of variable
      }
      writer.write(
          "  }\n" + // end of variables object
              "}\n"); // end of main object
      if (jsonp != null) writer.write(")");
      writer.flush(); // essential
    } finally {
      twawm.releaseResources();
    }

    if (EDDTable.reallyVerbose)
      String2.log(
          "  EDDTable.saveAsNcoJson done. TIME=" + (System.currentTimeMillis() - time) + "ms\n");
  }

  /**
   * Save the grid data in an NCO JSON .ncoJson file. See https://nco.sourceforge.net/nco.html#json
   * See issues in JavaDocs for EDDTable.saveAsNcoJson().
   *
   * <p>This gives up a few things (e.g., actual_range) in order to make this a streaming response
   * (not write file then transfer file).
   *
   * @param language the index of the selected language
   * @param ncVersion either NetcdfFileFormat.NETCDF3 or NETCDF4.
   * @param requestUrl the part of the user's request, after EDStatic.config.baseUrl, before '?'.
   * @param userDapQuery an OPeNDAP DAP-style query string, still percentEncoded (shouldn't be
   *     null). e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160]
   * @throws Throwable
   */
  private void saveAsNcoJson(
      int language,
      String requestUrl,
      String userDapQuery,
      OutputStreamSource outputStreamSource,
      EDDGrid grid)
      throws Throwable {
    if (EDDGrid.reallyVerbose) String2.log("  EDDGrid.saveAsNc");

    // for now, write strings as if in nc3 file: char arrays with extra dimension for strlen
    boolean writeStringsAsStrings = false; // if false, they are written as chars
    String stringOpenBracket = writeStringsAsStrings ? "" : "[";
    String stringCloseBracket = writeStringsAsStrings ? "" : "]";

    // did query include &.jsonp= ?
    String jsonp = EDStatic.getJsonpFromQuery(language, userDapQuery);
    // handle axisDapQuery
    if (grid.isAxisDapQuery(userDapQuery)) {
      // get axisDataAccessor first, in case of error when parsing query
      AxisDataAccessor ada = new AxisDataAccessor(language, grid, requestUrl, userDapQuery);
      int nRAV = ada.nRequestedAxisVariables();

      // create a writer
      try (BufferedWriter writer =
          File2.getBufferedWriterUtf8(outputStreamSource.outputStream(File2.UTF_8))) {
        if (jsonp != null) writer.write(jsonp + "(");

        // write start
        writer.write("{\n");

        // write the global attributes
        writer.write(ada.globalAttributes().toNcoJsonString("  "));

        // write dimensions
        // {
        //  "dimensions": {
        //    "row": 10,
        //    "bnd": 2
        //  }
        writer.write("  \"dimensions\": {\n");
        for (int avi = 0; avi < nRAV; avi++) {
          writer.write(
              (avi == 0 ? "" : ",\n")
                  + // end of previous line
                  "    "
                  + String2.toJson(ada.axisVariables(avi).destinationName())
                  + ": "
                  + ada.axisValues(avi).size());
        }
        writer.write(
            "\n" + // end of previous line
                "  },\n");

        // write the variables
        writer.write("  \"variables\": {\n");

        for (int avi = 0; avi < nRAV; avi++) {
          //    "att_var": {
          //      "shape": ["time"],
          //      "type": "float",
          //      "attributes": { ... },
          //      "data": [10.0, 10.10, 10.20, 10.30, 10.40101, 10.50, 10.60, 10.70, 10.80, 10.990]
          //    }
          EDVGridAxis av = ada.axisVariables(avi);
          String tType = av.destinationDataType();
          tType =
              switch (tType) {
                case "String" -> // shouldn't be any
                    writeStringsAsStrings ? "string" : "char";
                case "long" -> "int64"; // see

                  // https://www.unidata.ucar.edu/software/netcdf/docs/netcdf_utilities_guide.html#cdl_data_types and NCO JSON examples
                case "ulong" -> "uint64";
                default -> tType;
              };
          writer.write(
              "    "
                  + String2.toJson(av.destinationName())
                  + ": {\n"
                  + "      \"shape\": ["
                  + String2.toJson(av.destinationName())
                  + "],\n"
                  + "      \"type\": \""
                  + tType
                  + "\",\n");
          writer.write(ada.axisAttributes(avi).toNcoJsonString("      "));
          writer.write("      \"data\": [");
          writer.write(ada.axisValues(avi).toJsonCsvString());
          writer.write(
              "]\n"
                  + // end of data
                  "    }"
                  + (avi < nRAV - 1 ? ",\n" : "\n")); // end of variable
        }
        writer.write(
            "  }\n" + // end of variables object
                "}\n"); // end of main object
        if (jsonp != null) writer.write(")");
        writer.flush(); // essential
      }
      return;
    }

    BufferedWriter writer = null;
    // Need to use GridDataAllAccessor because need to know max String lengths.
    // Get this early so error thrown before writer created.
    try (GridDataAccessor gda =
            new GridDataAccessor(
                language, grid, requestUrl, userDapQuery, true, false); // tRowMajor, tConvertToNaN
        GridDataAllAccessor gdaa = new GridDataAllAccessor(gda)) {

      int nAV = grid.axisVariables().length;
      int nRDV = gda.dataVariables().length;

      // create a writer
      writer = File2.getBufferedWriterUtf8(outputStreamSource.outputStream(File2.UTF_8));
      if (jsonp != null) writer.write(jsonp + "(");

      // write start
      writer.write("{\n");

      // write the global attributes
      writer.write(gda.globalAttributes().toNcoJsonString("  "));

      // write dimensions
      // {
      //  "dimensions": {
      //    "row": 10,
      //    "bnd": 2
      //  }
      writer.write("  \"dimensions\": {\n");
      for (int avi = 0; avi < nAV; avi++) {
        writer.write(
            (avi == 0 ? "" : ",\n")
                + // end of previous line
                "    "
                + String2.toJson(grid.axisVariables()[avi].destinationName())
                + ": "
                + gda.axisValues(avi).size());
      }
      // need to create dimensions for string lengths
      if (!writeStringsAsStrings) {
        for (int dvi = 0; dvi < nRDV; dvi++) {
          EDV dv = gda.dataVariables()[dvi];
          if (dv.destinationDataPAType() == PAType.STRING) {
            int max = 1;
            long n = gda.totalIndex().size();
            try (DataInputStream dis = gdaa.getDataInputStream(dvi)) {
              for (int i = 0; i < n; i++) max = Math.max(max, dis.readUTF().length());
            }
            writer.write(
                ",\n"
                    + // end of previous line
                    "    "
                    + String2.toJson(dv.destinationName() + NcHelper.StringLengthSuffix)
                    + ": "
                    + max);
          }
        }
      }
      writer.write(
          "\n" + // end of previous line
              "  },\n");

      // write the variables
      writer.write("  \"variables\": {\n");

      // axisVariables
      for (int avi = 0; avi < nAV; avi++) {
        //    "att_var": {
        //      "shape": ["time"],
        //      "type": "float",
        //      "attributes": { ... },
        //      "data": [10.0, 10.10, 10.20, 10.30, 10.40101, 10.50, 10.60, 10.70, 10.80, 10.990]
        //    }
        EDVGridAxis av = grid.axisVariables()[avi];
        String tType = av.destinationDataType(); // never char or string
        if (tType.equals("long")) {
          tType = "int64"; // see
          // https://www.unidata.ucar.edu/software/netcdf/docs/netcdf_utilities_guide.html#cdl_data_types and NCO JSON examples
        } else if (tType.equals("ulong")) {
          tType = "uint64";
        }
        writer.write(
            "    "
                + String2.toJson(av.destinationName())
                + ": {\n"
                + "      \"shape\": ["
                + String2.toJson(av.destinationName())
                + "],\n"
                + "      \"type\": \""
                + tType
                + "\",\n");
        writer.write(gda.axisAttributes(avi).toNcoJsonString("      "));

        writer.write("      \"data\": [");
        writer.write(gda.axisValues(avi).toJsonCsvString());
        writer.write(
            "]\n"
                + // end of data
                "    },\n"); // end of variable
      }

      // dataVariables
      String tdvShape = new StringArray(grid.axisVariableDestinationNames()).toJsonCsvString();
      NDimensionalIndex tIndex =
          (NDimensionalIndex) gda.totalIndex().clone(); // incremented before get data
      long nRows = tIndex.size();
      for (int dvi = 0; dvi < nRDV; dvi++) {
        //    "att_var": {
        //      "shape": ["time"],
        //      "type": "float",
        //      "attributes": { ... },
        //      "data": [10.0, 10.10, 10.20, 10.30, 10.40101, 10.50, 10.60, 10.70, 10.80, 10.990]
        //    }
        EDV dv = gda.dataVariables()[dvi];
        Attributes atts = gda.dataAttributes(dvi);
        String tType = dv.destinationDataType();
        boolean isChar = tType.equals("char");
        boolean isString = tType.equals("String");
        String slShape = "";
        if (isString) {
          if (writeStringsAsStrings) {
            tType = "string";
          } else {
            slShape += ", " + String2.toJson(dv.destinationName() + NcHelper.StringLengthSuffix);
            tType = "char";
          }
        } else if (tType.equals("long")) {
          tType = "int64"; // see
          // https://www.unidata.ucar.edu/software/netcdf/docs/netcdf_utilities_guide.html#cdl_data_types and NCO JSON examples
        } else if (tType.equals("ulong")) {
          tType = "uint64";
        }
        writer.write(
            "    "
                + String2.toJson(dv.destinationName())
                + ": {\n"
                + "      \"shape\": ["
                + tdvShape
                + slShape
                + "],\n"
                + "      \"type\": \""
                + tType
                + "\",\n");
        writer.write(atts.toNcoJsonString("      "));

        writer.write("      \"data\":\n");
        for (int avi = 0; avi < nAV; avi++) writer.write("[ ");
        try (DataInputStream dis = gdaa.getDataInputStream(dvi)) {

          // create the bufferPA
          PrimitiveArray pa =
              PrimitiveArray.factory(
                  dv.destinationDataPAType(), 1, false); // safe since checked above
          CharArray ca = isChar ? (CharArray) pa : null;
          if (isChar) writer.write("\""); // start the string
          tIndex.reset();
          tIndex.increment(); // so we're at 1st datum
          for (long nRowsRead = 0; nRowsRead < nRows; nRowsRead++) {

            // String2.log(">> preCurrent=" + String2.toCSSVString(preCurrent));
            pa.clear();
            pa.readDis(dis, 1);

            // write one data value
            if (isChar) {
              // write it as one string with chars concatenated
              // see "md5_abc" in in http://dust.ess.uci.edu/tmp/in.json.fmt2
              //  "shape": ["lev"],          //dim lev size=3
              //  ...
              //  "data": ["abc"]
              writer.write(String2.charToJsonString(ca.get(0), 127, true)); // encodeNewline
            } else if (isString) {
              // Arrays of Strings are written oddly: (example from
              // http://dust.ess.uci.edu/tmp/in.json.fmt2)
              //    "date_rec": {
              //      "shape": ["time", "char_dmn_lng26"],
              //      "type": "char",
              //      "attributes": ...,
              //      "data": [["2010-11-01T00:00:00.000000"], ["2010-11-01T01:00:00.000000"],
              // ["2010-11-01T02:00:00.000000"], ["2010-11-01T03:00:00.000000"],
              // ["2010-11-01T04:00:00.000000"], ["2010-11-01T05:00:00.000000"],
              // ["2010-11-01T06:00:00.000000"], ["2010-11-01T07:00:00.000000"],
              // ["2010-11-01T08:00:00.000000"], ["2010-11-01T09:00:00.000000"]]
              //    },
              writer.write(
                  stringOpenBracket + String2.toJson(pa.getString(0)) + stringCloseBracket);
            } else {
              writer.write(pa.toJsonCsvString());
            }

            // write commas and brackets
            // This was difficult for me: It took me a while to figure out:
            // Given n (tIndex.nDimensionschanged()),
            //  the proper separators between data items are
            //  n-1 close brackets, 1 comma, n-1 open brackets.
            if (tIndex.increment()) {
              int nDimChanged = tIndex.nDimensionsChanged();
              if (nDimChanged == 1) {
                // easier to deal with this specially
                if (!isChar) writer.write(", ");
              } else {
                if (isChar) writer.write('\"'); // close the quote
                for (int dim = 0; dim < nDimChanged - 1; dim++) writer.write(" ]");
                writer.write(",\n");
                for (int dim = 0; dim < nDimChanged - 1; dim++) writer.write("[ ");
                if (isChar) writer.write('\"'); // open the quote
              }
            } // else it is the end of the data. Handle brackets specially below...
          } // end of data
        }
        if (isChar) writer.write("\""); // start the string
        for (int avi = 0; avi < nAV; avi++) writer.write(" ]");
        writer.write("\n" + "    }" + (dvi < nRDV - 1 ? "," : "") + "\n"); // end of variable
      }
      writer.write(
          "  }\n" + // end of variables object
              "}\n"); // end of main object
      if (jsonp != null) writer.write(")");
      writer.flush(); // essential
    } finally {
      if (writer != null) {
        try {
          writer.close();
        } catch (Exception e) {
          String2.log("Error closing writer, log and continue: " + e.getMessage());
        }
      }
    }
  }
}
