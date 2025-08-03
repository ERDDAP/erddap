package gov.noaa.pfel.erddap.filetypes;

import com.cohort.array.Attributes;
import com.cohort.array.NDimensionalIndex;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.pointdata.Table;
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
import java.io.BufferedWriter;
import java.io.Writer;
import java.util.HashSet;

@FileTypeClass(
    fileTypeExtension = ".itx",
    fileTypeName = ".itx",
    infoUrl = "https://www.wavemetrics.net/doc/igorman/II-09%20Data%20Import%20Export.pdf",
    versionAdded = "1.74.0",
    contentType = "application/x-download")
public class ItxFiles extends TableWriterFileType {

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
      saveAsIgor(requestInfo.outputStream(), (TableWriterAllWithMetadata) tableWriter);
    }
  }

  @Override
  public void writeGridToStream(DapRequestInfo requestInfo) throws Throwable {
    saveAsIgor(
        requestInfo.language(),
        requestInfo.requestUrl(),
        requestInfo.userDapQuery(),
        requestInfo.outputStream(),
        requestInfo.dir(),
        requestInfo.getEDDGrid());
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.get(Message.FILE_HELP_ITX_TABLE, language);
  }

  @Override
  public String getGridHelpText(int language) {
    return EDStatic.messages.get(Message.FILE_HELP_ITX_GRID, language);
  }

  /**
   * Save the TableWriterAllWithMetadata data as an Igor Text File .itx file. <br>
   * File reference: in Bob's /programs/igor/ or
   * https://www.wavemetrics.net/doc/igorman/II-09%20Data%20Import%20Export.pdf <br>
   * Command reference: in Bob's /programs/igor/ or
   * https://www.wavemetrics.net/doc/igorman/V-01%20Reference.pdf <br>
   * The file extension should be .itx
   *
   * @param language the index of the selected language
   * @param outputStreamSource If all goes well, the outputstream is closed at the end.
   * @param twawm all the results data, with missingValues stored as destinationMissingValues or
   *     destinationFillValues (they will be converted to NaNs)
   * @throws Throwable
   */
  private void saveAsIgor(OutputStreamSource outputStreamSource, TableWriterAllWithMetadata twawm)
      throws Throwable {

    if (EDDTable.reallyVerbose) String2.log("EDDTable.saveAsIgor");
    long time = System.currentTimeMillis();
    try {

      // make sure there is data but not too much
      long tnRows = twawm.nRows();
      if (tnRows == 0)
        throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (at start of saveAsIgor)");
      if (tnRows >= Integer.MAX_VALUE)
        throw new SimpleException(Math2.memoryTooMuchData + " (at start of saveAsIgor)");
      // the other params are all required by EDD,
      //  so it's a programming error if they are missing

      // open an OutputStream
      try (Writer writer =
          File2.getBufferedWriter(
              outputStreamSource.outputStream(Table.IgorCharset), Table.IgorCharset)) {
        writer.write("IGOR" + Table.IgorEndOfLine);

        // write each col as a wave separately, so data type is preserved
        HashSet<String> colNamesHashset = new HashSet<>();
        int nCols = twawm.nColumns();
        for (int col = 0; col < nCols; col++) {
          Attributes atts = twawm.columnAttributes(col);
          String units = atts.getString("units");
          boolean isTimeStamp =
              units != null && (units.equals(EDV.TIME_UNITS) || units.equals(EDV.TIME_UCUM_UNITS));

          PrimitiveArray pa = twawm.column(col);
          pa.convertToStandardMissingValues(
              atts.getString("_FillValue"), atts.getString("missing_value"));

          Table.writeIgorWave(
              writer,
              Table.makeUniqueIgorColumnName(twawm.columnName(col), colNamesHashset),
              "",
              pa,
              units,
              isTimeStamp,
              "");
        }

        // done!
        writer.flush(); // essential
      }
    } finally {
      twawm.releaseResources();
    }

    if (EDDTable.reallyVerbose)
      String2.log(
          "  EDDTable.saveAsIgor done. TIME=" + (System.currentTimeMillis() - time) + "ms\n");
  }

  /**
   * Save the TableWriterAllWithMetadata data as an Igor Text File .itx file. <br>
   * File reference: in Bob's /programs/igor/ or
   * https://www.wavemetrics.net/doc/igorman/II-09%20Data%20Import%20Export.pdf <br>
   * Command reference: in Bob's /programs/igor/ or
   * https://www.wavemetrics.net/doc/igorman/V-01%20Reference.pdf <br>
   * The file extension should be .itx
   *
   * @param language the index of the selected language
   * @param requestUrl the part of the user's request, after EDStatic.config.baseUrl, before '?'.
   * @param userDapQuery an OPeNDAP DAP-style query string, still percentEncoded (shouldn't be
   *     null). e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160]
   * @param outputStreamSource the source of an outputStream (usually already buffered) to receive
   *     the results. If all goes well, at the end of this method the outputStream is closed.
   * @throws Throwable
   */
  private void saveAsIgor(
      int language,
      String requestUrl,
      String userDapQuery,
      OutputStreamSource oss,
      String dir,
      EDDGrid grid)
      throws Throwable {
    if (EDDGrid.reallyVerbose) String2.log("  EDDGrid.saveAsIgor");
    long time = System.currentTimeMillis();

    // handle axisDapQuery
    if (grid.isAxisDapQuery(userDapQuery)) {
      // get axisDataAccessor first, in case of error when parsing query
      AxisDataAccessor ada = new AxisDataAccessor(language, grid, requestUrl, userDapQuery);
      int nRAV = ada.nRequestedAxisVariables();

      // make a table and saveAsIgor
      Table table = new Table();
      for (int av = 0; av < nRAV; av++) {
        table.addColumn(
            av,
            ada.axisVariables(av).destinationName(),
            ada.axisValues(av),
            ada.axisAttributes(av));
      }
      table.saveAsIgor(
          File2.getBufferedWriter(oss.outputStream(Table.IgorCharset), Table.IgorCharset));

      // diagnostic
      if (EDDGrid.reallyVerbose) String2.log("  EDDGrid.saveAsIgor axis done\n");
      // String2.log(NcHelper.ncdump(fullFileName, "-h"));

      return;
    }

    // ** create gridDataAccessor first,
    // to check for error when parsing query or getting data,
    // and to check n values
    try (GridDataAccessor gda =
        new GridDataAccessor(
            language, grid, requestUrl, userDapQuery, true, true)) { // rowMajor, convertToNaN
      EDV tDataVariables[] = gda.dataVariables();
      int nDV = tDataVariables.length;

      // ensure < Integer.MAX_VALUE items
      // No specific limit in Igor. But this is suggested as very large.
      // And this is limit for PrimitiveArray size.
      NDimensionalIndex totalIndex = gda.totalIndex();
      if (nDV * totalIndex.size() >= Integer.MAX_VALUE)
        throw new SimpleException(
            Math2.memoryTooMuchData
                + " ("
                + (nDV * totalIndex.size())
                + " values is more than "
                + (Integer.MAX_VALUE - 1)
                + ")");
      int nAV = grid.axisVariables().length;
      if (nAV > 4)
        throw new SimpleException(
            EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                + "Igor Text Files can handle 4 dimensions, not "
                + nAV);

      // ** Then get gridDataAllAccessor
      // AllAccessor so I can just request PA with a var's values.
      Math2.ensureDiskAvailable(gda.totalNBytes(), dir, "ItxFiles");
      try (GridDataAllAccessor gdaa = new GridDataAllAccessor(gda);
          BufferedWriter writer =
              File2.getBufferedWriter(oss.outputStream(Table.IgorCharset), Table.IgorCharset)) {
        // write the data
        writer.write("IGOR" + Table.IgorEndOfLine);

        HashSet<String> colNamesHashset = new HashSet<>();
        StringBuilder setScaleForDims = new StringBuilder();
        StringArray avUNames = new StringArray();
        for (int av = 0; av < nAV; av++) {
          Attributes atts = gda.axisAttributes(av);
          String units = atts.getString("units");
          boolean isTimeStamp =
              units != null && (units.equals(EDV.TIME_UNITS) || units.equals(EDV.TIME_UCUM_UNITS));

          PrimitiveArray pa = gda.axisValues(av);
          // pa.convertToStandardMissingValues( //no need, since no missing values
          //    atts.getDouble("_FillValue"),
          //    atts.getDouble("missing_value"));

          String uName =
              Table.makeUniqueIgorColumnName(
                  grid.axisVariables()[av].destinationName(), colNamesHashset);
          Table.writeIgorWave(writer, uName, "", pa, units, isTimeStamp, "");
          avUNames.add(uName);

          // setScaleForDims
          setScaleForDims.append("X SetScale ");
          if (pa.size() == 1 || pa.isEvenlySpaced().length() > 0) {
            // just 1 value or isn't evenlySpaced
            setScaleForDims.append(
                "/I "
                    + "xyzt".charAt(nAV - av - 1)
                    + ", "
                    + pa.getString(0)
                    + ","
                    + pa.getString(pa.size() - 1));

          } else {
            // isEvenlySpaced, num2 is average spacing
            setScaleForDims.append(
                "/P "
                    + "xyzt".charAt(nAV - av - 1)
                    + ", "
                    + pa.getString(0)
                    + ","
                    + ((pa.getDouble(pa.size() - 1) - pa.getDouble(0)) / (pa.size() - 1)));
          }
          String tUnits = isTimeStamp ? "dat" : String2.isSomething(units) ? units : ""; // space?
          setScaleForDims.append(
              ", " + String2.toJson(tUnits) + ", $WAVE_NAME$" + Table.IgorEndOfLine);
        }

        StringBuilder dimInfo = new StringBuilder();
        StringBuilder notes = new StringBuilder(); // the in-common quoted part
        for (int av = nAV - 1; av >= 0; av--) {
          // write each axisVar as a wave separately, so data type is preserved
          // Igor wants the dimension definition to be nRow, nColumn, nLayer, nChunk !
          int n = gda.axisValues(av).size();
          if (av == nAV - 1) dimInfo.append(n);
          else if (av == nAV - 2) dimInfo.insert(0, n + ",");
          else dimInfo.append("," + n);

          // e.g., X Note analysed_sst_mod, "RowsDim:longitude;ColumnsDim:latitude;LayersDim:time2"
          if (notes.length() > 0) notes.append(';');
          notes.append(
              (av == nAV - 1
                      ? "RowsDim"
                      : av == nAV - 2
                          ? "ColumnsDim"
                          : av == nAV - 3
                              ? "LayersDim"
                              : av == nAV - 4 ? "ChunkDim" : "Dim" + (nAV - av - 1))
                  + // shouldn't happen since limited to 4 dims above
                  ":"
                  + avUNames.get(av));
        }

        // write each dataVar as a wave separately, so data type is preserved
        // Igor wants same row-major order as ERDDAP:
        //  "Igor expects the data to be in column/row/layer/chunk order." [t,z,y,x]
        for (int dv = 0; dv < nDV; dv++) {
          Attributes atts = gda.dataAttributes(dv);
          String units = atts.getString("units");
          boolean isTimeStamp =
              units != null && (units.equals(EDV.TIME_UNITS) || units.equals(EDV.TIME_UCUM_UNITS));

          PrimitiveArray pa = gdaa.getPrimitiveArray(dv);
          // converted to NaN by "convertToNaN" above

          String uName =
              Table.makeUniqueIgorColumnName(tDataVariables[dv].destinationName(), colNamesHashset);
          Table.writeIgorWave(
              writer,
              uName,
              "/N=(" + dimInfo + ")",
              pa,
              units,
              isTimeStamp,
              String2.replaceAll(setScaleForDims.toString(), "$WAVE_NAME$", uName)
                  +
                  // e.g., X Note analysed_sst_mod,
                  // "RowsDim:longitude;ColumnsDim:latitude;LayersDim:time2"
                  "X Note "
                  + uName
                  + ", \""
                  + notes
                  + "\""
                  + Table.IgorEndOfLine);
        }

        // done!
        if (writer != null) {
          writer.flush(); // essential
        }
      }
    }

    // diagnostic
    if (EDDGrid.reallyVerbose)
      String2.log(
          "  EDDGrid.saveAsIgor done.  TIME=" + (System.currentTimeMillis() - time) + "ms\n");
    // String2.log(NcHelper.ncdump(directory + name + ext, "-h"));

  }
}
