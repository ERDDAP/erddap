package gov.noaa.pfel.erddap.filetypes;

import com.cohort.array.Attributes;
import com.cohort.array.DoubleArray;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.OutputStreamSource;
import gov.noaa.pfel.erddap.util.EDMessages.Message;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import gov.noaa.pfel.erddap.variable.EDVGridAxis;
import gov.noaa.pfel.erddap.variable.EDVTimeStamp;
import gov.noaa.pfel.erddap.variable.EDVTimeStampGridAxis;
import java.io.Writer;

@FileTypeClass(
    fileTypeExtension = ".csv",
    fileTypeName = ".nccsvMetadata",
    infoUrl = "https://erddap.github.io/docs/user/nccsv-1.20",
    versionAdded = "1.76.0",
    contentType = "text/csv")
public class NccsvMetadataFiles extends FileTypeInterface {

  @Override
  public void writeTableToStream(DapRequestInfo requestInfo) throws Throwable {
    Table table = new Table();
    EDD edd = requestInfo.edd();
    table
        .globalAttributes()
        .add(edd.combinedGlobalAttributes().toAttributes(requestInfo.language()));
    for (int dvi = 0; dvi < edd.dataVariables().length; dvi++) {
      EDV dv = edd.dataVariables()[dvi];
      Attributes catts = dv.combinedAttributes().toAttributes(requestInfo.language());
      PAType tPAType = dv.destinationDataPAType();
      if (dv instanceof EDVTimeStamp) {
        // convert to String times
        tPAType = PAType.STRING;
        // make changes to a copy (generated in the toAttributes call above)
        String timePre = catts.getString(EDV.TIME_PRECISION);
        catts.set("units", Calendar2.timePrecisionToTimeFormat(timePre));

        PrimitiveArray pa = catts.get("actual_range");
        if (pa instanceof DoubleArray && pa.size() == 2) {
          StringArray sa = new StringArray();
          for (int i = 0; i < 2; i++)
            sa.add(Calendar2.epochSecondsToLimitedIsoStringT(timePre, pa.getDouble(i), ""));
          catts.set("actual_range", sa);
        }
      }
      table.addColumn(dvi, dv.destinationName(), PrimitiveArray.factory(tPAType, 1, false), catts);
    }
    Writer writer =
        File2.getBufferedWriterUtf8(requestInfo.outputStream().outputStream(File2.UTF_8));
    table.saveAsNccsv(false, true, 0, 0, writer); // catchScalars, writeMetadata, writeDataRows
  }

  @Override
  public void writeGridToStream(DapRequestInfo requestInfo) throws Throwable {
    saveAsNccsv(requestInfo.language(), requestInfo.outputStream(), requestInfo.getEDDGrid());
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.get(Message.FILE_HELP_NCCSV_METADATA, language);
  }

  /**
   * This writes the requested axis or grid data to the outputStream in NCCSV
   * (https://erddap.github.io/docs/user/nccsv-1.20) format. If no exception is thrown, the data was
   * successfully written.
   *
   * @param outputStreamSource the source of an outputStream (usually already buffered) to receive
   *     the results. At the end of this method the outputStream is flushed, not closed.
   * @throws Throwable if trouble.
   */
  private void saveAsNccsv(int language, OutputStreamSource outputStreamSource, EDDGrid grid)
      throws Throwable {

    if (EDDGrid.reallyVerbose) String2.log("  EDDGrid.saveAsNccsv");

    try (Writer writer =
        File2.getBufferedWriter88591(outputStreamSource.outputStream(File2.ISO_8859_1))) {
      Table table = new Table();
      table.globalAttributes().add(grid.combinedGlobalAttributes().toAttributes(language));
      for (int avi = 0; avi < grid.axisVariables().length; avi++) {
        EDVGridAxis av = grid.axisVariables()[avi];
        Attributes catts = av.combinedAttributes().toAttributes(language);
        PAType tPAType = av.destinationDataPAType();
        if (av instanceof EDVTimeStampGridAxis) {
          // convert to String times
          tPAType = PAType.STRING;
          // make changes to a copy (generated in the toAttributes call above)
          String timePre = catts.getString(EDV.TIME_PRECISION);
          catts.set("units", Calendar2.timePrecisionToTimeFormat(timePre));

          PrimitiveArray pa = catts.get("actual_range");
          if (pa instanceof DoubleArray && pa.size() == 2) {
            StringArray sa = new StringArray();
            for (int i = 0; i < 2; i++)
              sa.add(Calendar2.epochSecondsToLimitedIsoStringT(timePre, pa.getDouble(i), ""));
            catts.set("actual_range", sa);
          }
        }
        table.addColumn(
            avi, av.destinationName(), PrimitiveArray.factory(tPAType, 1, false), catts);
      }
      for (EDV dv : grid.dataVariables()) {
        Attributes catts = dv.combinedAttributes().toAttributes(language);
        PAType tPAType = dv.destinationDataPAType();
        if (dv instanceof EDVTimeStamp) {
          // make changes to a copy (generated in the toAttributes call above)
          catts.set(
              "units", Calendar2.timePrecisionToTimeFormat(catts.getString(EDV.TIME_PRECISION)));
          tPAType = PAType.STRING;
        }
        table.addColumn(
            table.nColumns(),
            dv.destinationName(),
            PrimitiveArray.factory(tPAType, 1, false),
            catts);
      }
      table.saveAsNccsv(
          false, true, 0, 0,
          writer); // catchScalars, writeMetadata, writeDataRows; writer is flushed not closed
    }
  }
}
