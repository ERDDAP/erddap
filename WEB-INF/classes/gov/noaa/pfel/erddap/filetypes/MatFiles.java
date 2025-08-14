package gov.noaa.pfel.erddap.filetypes;

import com.cohort.array.NDimensionalIndex;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.griddata.Matlab;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.dataset.AxisDataAccessor;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.EDDTable;
import gov.noaa.pfel.erddap.dataset.GridDataAccessor;
import gov.noaa.pfel.erddap.dataset.OutputStreamSource;
import gov.noaa.pfel.erddap.dataset.TableWriter;
import gov.noaa.pfel.erddap.dataset.TableWriterAllWithMetadata;
import gov.noaa.pfel.erddap.util.EDMessages.Message;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import java.io.DataOutputStream;
import java.text.MessageFormat;

@FileTypeClass(
    fileTypeExtension = ".mat",
    fileTypeName = ".mat",
    infoUrl = "https://www.mathworks.com/",
    versionAdded = "1.0.0",
    contentType = "application/x-download")
public class MatFiles extends TableWriterFileType {

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
    if (tableWriter instanceof TableWriterAllWithMetadata twalwm) {
      saveAsMatlab(requestInfo.outputStream(), twalwm, requestInfo.edd().datasetID());
    }
  }

  @Override
  public void writeGridToStream(DapRequestInfo requestInfo) throws Throwable {
    saveAsMatlab(
        requestInfo.language(),
        requestInfo.requestUrl(),
        requestInfo.userDapQuery(),
        requestInfo.outputStream(),
        requestInfo.getEDDGrid());
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.get(Message.FILE_HELP_MAT, language);
  }

  /**
   * Save the TableWriterAllWithMetadata data as a Matlab .mat file. This doesn't write attributes
   * because .mat files don't store attributes. This maintains the data types (Strings become
   * char[][]).
   *
   * @param outputStreamSource
   * @param twawm all the results data, with missingValues stored as destinationMissingValues or
   *     destinationFillValues (they are converted to NaNs)
   * @param structureName the name to use for the variable which holds all of the data, usually the
   *     dataset's internal name (datasetID). If structureName isn't a valid Matlab variable name,
   *     it will be made so via String2.modifyToBeVariableNameSafe().
   * @throws Throwable
   */
  private void saveAsMatlab(
      OutputStreamSource outputStreamSource, TableWriterAllWithMetadata twawm, String structureName)
      throws Throwable {
    if (EDDTable.reallyVerbose) String2.log("EDDTable.saveAsMatlab");
    long time = System.currentTimeMillis();
    structureName = String2.modifyToBeVariableNameSafe(structureName);

    // make sure there is data
    long tnRows = twawm.nRows();
    if (tnRows == 0)
      throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (at start of saveAsMatlab)");
    if (tnRows >= Integer.MAX_VALUE)
      throw new SimpleException(
          Math2.memoryTooMuchData
              + "  "
              + MessageFormat.format(
                  EDStatic.messages.get(Message.ERROR_MORE_THAN_2GB, 0),
                  ".mat",
                  tnRows + " " + EDStatic.messages.get(Message.ROWS, 0)));
    int nCols = twawm.nColumns();
    int nRows = (int) tnRows; // safe since checked above

    // calculate cumulative size of the structure
    byte structureNameInfo[] = Matlab.nameInfo(structureName);
    NDimensionalIndex ndIndex[] = new NDimensionalIndex[nCols];
    long cumSize = // see 1-32
        16
            + // for array flags
            16
            + // my structure is always 2 dimensions
            structureNameInfo.length
            + 8
            + // field name length (for all fields)
            8
            + nCols * 32L; // field names
    for (int col = 0; col < nCols; col++) {
      PAType type = twawm.columnType(col);
      if (type == PAType.STRING)
        ndIndex[col] = Matlab.make2DNDIndex(nRows, twawm.columnMaxStringLength(col));
      else ndIndex[col] = Matlab.make2DNDIndex(nRows);
      // add size of each cell
      cumSize +=
          8
              + // type and size
              Matlab.sizeOfNDimensionalArray( // throws exception if too big for Matlab
                  "", // without column names (they're stored separately)
                  type,
                  ndIndex[col]);
    }

    if (cumSize >= Integer.MAX_VALUE - 1000)
      throw new SimpleException(
          Math2.memoryTooMuchData
              + "  "
              + MessageFormat.format(
                  EDStatic.messages.get(Message.ERROR_MORE_THAN_2GB, 0),
                  ".mat",
                  (cumSize / Math2.BytesPerMB) + " MB"));

    // open a dataOutputStream
    DataOutputStream stream = new DataOutputStream(outputStreamSource.outputStream(""));

    // *** write Matlab Structure  see 1-32
    // *** THIS CODE MIMICS Table.saveAsMatlab. If make changes here, make them there, too.
    //    The code in EDDGrid.saveAsMatlab is similar, too.
    // write the header
    Matlab.writeMatlabHeader(stream);

    // write the miMatrix dataType and nBytes
    stream.writeInt(Matlab.miMATRIX); // dataType
    stream.writeInt((int) cumSize); // safe since checked above

    // write array flags
    stream.writeInt(Matlab.miUINT32); // dataType
    stream.writeInt(8); // fixed nBytes of data
    stream.writeInt(Matlab.mxSTRUCT_CLASS); // array flags
    stream.writeInt(0); // reserved; ends on 8 byte boundary

    // write structure's dimension array
    stream.writeInt(Matlab.miINT32); // dataType
    stream.writeInt(2 * 4); // nBytes
    // matlab docs have 2,1, octave has 1,1.
    // Think of structure as one row of a table, where elements are entire arrays:  e.g., sst.lon
    // sst.lat sst.sst.
    // Having multidimensions (e.g., 2 here) lets you have additional rows, e.g., sst(2).lon
    // sst(2).lat sst(2).sst.
    // So 1,1 makes sense.
    stream.writeInt(1);
    stream.writeInt(1);

    // write structure name
    stream.write(structureNameInfo, 0, structureNameInfo.length);

    // write length for all field names (always 32)  (short form)
    stream.writeShort(4); // nBytes
    stream.writeShort(Matlab.miINT32); // dataType
    stream.writeInt(32); // 32 bytes per field name

    // write the field names (each 32 bytes)
    stream.writeInt(Matlab.miINT8); // dataType
    stream.writeInt(nCols * 32); // 32 bytes per field name
    String nulls = String2.makeString('\u0000', 32);
    for (int col = 0; col < nCols; col++)
      stream.write(
          String2.toByteArray(String2.noLongerThan(twawm.columnName(col), 31) + nulls),
          0,
          32); // EEEK! Better not be longer.

    // write the structure's elements (one for each col)
    // This is pretty good at conserving memory (just one column in memory at a time).
    // It would be hard to make more conservative because Strings have
    // to be written out: all first chars, all second chars, all third chars...
    for (int col = 0; col < nCols; col++) {
      PrimitiveArray pa = twawm.column(col);
      // convert missing values to NaNs  (StringArray and CharArray are unchanged)
      pa.convertToStandardMissingValues(
          twawm.columnAttributes(col).getString("_FillValue"),
          twawm.columnAttributes(col).getString("missing_value"));
      Matlab.writeNDimensionalArray(
          stream,
          "", // without column names (they're stored separately)
          pa,
          ndIndex[col]);
    }

    // this doesn't write attributes because .mat files don't store attributes

    stream.flush(); // essential

    if (EDDTable.reallyVerbose)
      String2.log(
          "  EDDTable.saveAsMatlab done. TIME=" + (System.currentTimeMillis() - time) + "ms\n");
  }

  /**
   * This writes the grid from this dataset to the outputStream in Matlab .mat format. This writes
   * the lon values as they are currently in this grid (e.g., +-180 or 0..360). If no exception is
   * thrown, the data was successfully written.
   *
   * @param language the index of the selected language
   * @param requestUrl the part of the user's request, after EDStatic.config.baseUrl, before '?'.
   * @param userDapQuery an OPeNDAP DAP-style query string, still percent-encoded (shouldn't be
   *     null), e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160]
   * @param outputStreamSource the source of an outputStream (usually already buffered) to receive
   *     the results. At the end of this method the outputStream is flushed, not closed.
   * @throws Throwable
   */
  private void saveAsMatlab(
      int language,
      String requestUrl,
      String userDapQuery,
      OutputStreamSource outputStreamSource,
      EDDGrid grid)
      throws Throwable {

    if (EDDGrid.reallyVerbose) String2.log("  EDDGrid.saveAsMatlab");
    long time = System.currentTimeMillis();

    // handle axisDapQuery
    if (grid.isAxisDapQuery(userDapQuery)) {
      // this doesn't write attributes because .mat files don't store attributes
      // get axisDataAccessor first, in case of error when parsing query
      AxisDataAccessor ada = new AxisDataAccessor(language, grid, requestUrl, userDapQuery);

      // make the table
      Table table = new Table();
      int nRAV = ada.nRequestedAxisVariables();
      for (int av = 0; av < nRAV; av++)
        table.addColumn(ada.axisVariables(av).destinationName(), ada.axisValues(av));
      // don't call table.makeColumnsSameSize();  leave them different lengths

      // then get the modified outputStream
      DataOutputStream dos = new DataOutputStream(outputStreamSource.outputStream(""));
      table.saveAsMatlab(dos, grid.datasetID()); // it makes structure and varNames Matlab-safe
      dos.flush(); // essential

      if (EDDGrid.reallyVerbose) String2.log("  EDDGrid.saveAsMatlab axis done.\n");
      return;
    }

    // get gridDataAccessor first, in case of error when parsing query
    try (GridDataAccessor mainGda =
        new GridDataAccessor(
            language,
            grid,
            requestUrl,
            userDapQuery,
            false, // Matlab is one of the few drivers that needs column-major order
            true)) { // convertToNaN
      String structureName = String2.encodeMatlabNameSafe(grid.datasetID());

      // Make sure no String data and that gridsize isn't > Integer.MAX_VALUE bytes (Matlab's limit)
      EDV tDataVariables[] = mainGda.dataVariables();
      int nAv = grid.axisVariables().length;
      int ntDv = tDataVariables.length;
      byte structureNameInfo[] = Matlab.nameInfo(structureName);
      // int largest = 1; //find the largest data item nBytesPerElement
      long cumSize = // see 1-32
          16
              + // for array flags
              16
              + // my structure is always 2 dimensions
              structureNameInfo.length
              + 8
              + // field name length (for all fields)
              8
              + (nAv + ntDv) * 32L; // field names

      PrimitiveArray avPa[] = new PrimitiveArray[nAv];
      NDimensionalIndex avNDIndex[] = new NDimensionalIndex[nAv];
      for (int av = 0; av < nAv; av++) {
        avPa[av] = mainGda.axisValues(av);
        avNDIndex[av] = Matlab.make2DNDIndex(avPa[av].size());
        cumSize +=
            8
                + Matlab.sizeOfNDimensionalArray( // throws exception if too big for Matlab
                    "", // names are done separately
                    avPa[av].elementType(),
                    avNDIndex[av]);
      }

      GridDataAccessor tGda[] = new GridDataAccessor[ntDv];
      NDimensionalIndex dvNDIndex[] = new NDimensionalIndex[ntDv];
      String arrayQuery = EDDGrid.buildDapArrayQuery(mainGda.constraints());
      for (int dv = 0; dv < ntDv; dv++) {
        if (tDataVariables[dv].destinationDataPAType() == PAType.STRING)
          // can't do String data because you need random access to all values
          // that could be a memory nightmare
          // so just don't allow it
          throw new SimpleException(
              EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                  + "ERDDAP doesn't support String data in Matlab grid data files.");
        // largest = Math.max(largest,
        //    tDataVariables[dv].destinationBytesPerElement());

        // make a GridDataAccessor for this dataVariable
        String tUserDapQuery = tDataVariables[dv].destinationName() + arrayQuery;
        tGda[dv] =
            new GridDataAccessor(
                language,
                grid,
                requestUrl,
                tUserDapQuery,
                false, // Matlab is one of the few drivers that needs column-major order
                true); // convertToNaN
        dvNDIndex[dv] = tGda[dv].totalIndex();
        if (dvNDIndex[dv].nDimensions() == 1)
          dvNDIndex[dv] = Matlab.make2DNDIndex(dvNDIndex[dv].shape()[0]);

        cumSize +=
            8
                + Matlab.sizeOfNDimensionalArray( // throws exception if too big for Matlab
                    "", // names are done separately
                    tDataVariables[dv].destinationDataPAType(),
                    dvNDIndex[dv]);
      }
      if (cumSize >= Integer.MAX_VALUE - 1000)
        throw new SimpleException(
            Math2.memoryTooMuchData
                + "  "
                + MessageFormat.format(
                    EDStatic.messages.get(Message.ERROR_MORE_THAN_2GB, 0),
                    ".mat",
                    (cumSize / Math2.BytesPerMB) + " MB"));
      // "Error: " +
      // "The requested data (" +
      // (cumSize / Math2.BytesPerMB) +
      // " MB) is greater than Matlab's limit (" +
      // (Integer.MAX_VALUE / Math2.BytesPerMB) + " MB)."); //safe

      // then get the modified outputStream
      DataOutputStream stream = new DataOutputStream(outputStreamSource.outputStream(""));

      // write the header
      Matlab.writeMatlabHeader(stream);

      // write the miMatrix dataType and nBytes
      stream.writeInt(Matlab.miMATRIX); // dataType
      stream.writeInt((int) cumSize); // safe since checked above

      // write array flags
      stream.writeInt(Matlab.miUINT32); // dataType
      stream.writeInt(8); // fixed nBytes of data
      stream.writeInt(Matlab.mxSTRUCT_CLASS); // array flags
      stream.writeInt(0); // reserved; ends on 8 byte boundary

      // write structure's dimension array
      stream.writeInt(Matlab.miINT32); // dataType
      stream.writeInt(2 * 4); // nBytes
      // matlab docs have 2,1, octave has 1,1.
      // Think of structure as one row of a table, where elements are entire arrays:  e.g., sst.lon
      // sst.lat sst.sst.
      // Having multidimensions (e.g., 2 here) lets you have additional rows, e.g., sst(2).lon
      // sst(2).lat sst(2).sst.
      // So 1,1 makes sense.
      stream.writeInt(1);
      stream.writeInt(1);

      // write structure name
      stream.write(structureNameInfo, 0, structureNameInfo.length);

      // write length for all field names (always 32)  (short form)
      stream.writeShort(4); // nBytes
      stream.writeShort(Matlab.miINT32); // dataType
      stream.writeInt(32); // 32 bytes per field name

      // write the structure's field names (each 32 bytes)
      stream.writeInt(Matlab.miINT8); // dataType
      stream.writeInt((nAv + ntDv) * 32); // 32 bytes per field name
      String nulls = String2.makeString('\u0000', 32);
      for (int av = 0; av < nAv; av++)
        stream.write(
            String2.toByteArray(
                String2.noLongerThan(
                        String2.encodeMatlabNameSafe(grid.axisVariables()[av].destinationName()),
                        31)
                    + nulls),
            0,
            32); // EEEK! Better not be longer.
      for (EDV tDataVariable : tDataVariables)
        stream.write(
            String2.toByteArray(
                String2.noLongerThan(
                        String2.encodeMatlabNameSafe(tDataVariable.destinationName()), 31)
                    + nulls),
            0,
            32); // EEEK! Better not be longer.

      // write the axis miMatrix
      for (int av = 0; av < nAv; av++)
        Matlab.writeNDimensionalArray(
            stream,
            "", // name is written above
            avPa[av],
            avNDIndex[av]);

      // make the data miMatrix
      for (int dv = 0; dv < ntDv; dv++) {
        grid.writeNDimensionalMatlabArray(
            language,
            stream,
            "", // name is written above
            tGda[dv],
            dvNDIndex[dv]);
        tGda[dv].close();
      }

      // this doesn't write attributes because .mat files don't store attributes
      stream.flush(); // essential
    }
    if (EDDGrid.reallyVerbose)
      String2.log(
          "  EDDGrid.saveAsMatlab done. TIME=" + (System.currentTimeMillis() - time) + "ms\n");
  }
}
