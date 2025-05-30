package gov.noaa.pfel.erddap.filetypes;

import com.cohort.array.Attributes;
import com.cohort.array.DoubleArray;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.XML;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.OutputStreamSource;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import gov.noaa.pfel.erddap.variable.EDVGridAxis;
import java.io.IOException;
import java.io.Writer;

@FileTypeClass(
    fileTypeExtension = ".xml",
    fileTypeName = ".ncml",
    infoUrl = "https://docs.unidata.ucar.edu/netcdf-java/current/userguide/ncml_overview.html",
    versionAdded = "1.48.0",
    availableTable = false,
    contentType = "application/xml")
public class NcmlFiles extends FileTypeInterface {

  @Override
  public void writeTableToStream(DapRequestInfo requestInfo) throws Throwable {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeGridToStream(DapRequestInfo requestInfo) throws Throwable {
    saveAsNCML(
        requestInfo.language(),
        requestInfo.loggedInAs(),
        requestInfo.outputStream(),
        requestInfo.getEDDGrid());
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelp_ncmlAr[language];
  }

  /**
   * This writes attributes to an nc3 .ncml file. .ncml are XML files so UTF-8.
   *
   * @param writer
   * @param atts
   * @param indent e.g., " "
   */
  private void writeNcmlAttributes(Writer writer, Attributes atts, String indent)
      throws IOException {

    String[] names = atts.getNames();
    for (String name : names) {
      writer.write(indent + "<attribute name=\"" + XML.encodeAsXML(name) + "\" ");
      // title" value="Daily MUR SST, Interim near-real-time (nrt) product" />
      PrimitiveArray pa = atts.get(name);
      if (pa.elementType() == PAType.LONG) pa = new DoubleArray(pa);
      // even nc3 files write char and String attributes as UTF-8

      // write the attribute
      if (pa instanceof StringArray sa) {
        String s = String2.toSVString(sa.toArray(), "\n", false); // newline separated
        writer.write("value=\"" + XML.encodeAsXML(s) + "\" />\n");
      } else {
        String s = String2.replaceAll(pa.toString(), ",", ""); // comma-space -> space separated
        // NCML types same as Java: String (default), byte, short, int, float, double (and long, but
        // not in nc3)
        writer.write("type=\"" + pa.elementTypeString() + "\" " + "value=\"" + s + "\" />\n");
      }
    }
  }

  /**
   * This writes the dataset structure and attributes in .ncml form (mimicking TDS).
   * https://oceanwatch.pfeg.noaa.gov/thredds/ncml/satellite/MUR/ssta/1day?catalog=http%3A%2F%2Foceanwatch.pfeg.noaa.gov%2Fthredds%2FSatellite%2FaggregsatMUR%2Fssta%2Fcatalog.html&dataset=satellite%2FMUR%2Fssta%2F1day
   * stored locally as c:/data/ncml/MUR.xml <br>
   * Annotated Schema for NcML
   * "https://www.unidata.ucar.edu/software/thredds/current/netcdf-java/ncml/AnnotatedSchema4.html"
   *
   * @param loggedInAs
   * @param outputStreamSource the source of an outputStream (usually already buffered) to receive
   *     the results. At the end of this method the outputStream is flushed, not closed.
   * @throws Throwable if trouble.
   */
  private void saveAsNCML(
      int language, String loggedInAs, OutputStreamSource outputStreamSource, EDDGrid grid)
      throws Throwable {
    if (EDDGrid.reallyVerbose) String2.log("  EDDGrid.saveAsNCML " + grid.datasetID());
    long time = System.currentTimeMillis();

    // get the writer
    try (Writer writer =
        File2.getBufferedWriterUtf8(outputStreamSource.outputStream(File2.UTF_8))) {
      String opendapBaseUrl = EDStatic.baseUrl(loggedInAs) + "/griddap/" + grid.datasetID();
      writer.write( // NCML
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
              + "<netcdf xmlns=\"https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2\" "
              + "location=\""
              + opendapBaseUrl
              + "\">\n");

      // global atts
      // TDS puts different types of atts in different groups. ERDDAP doesn't.
      Attributes atts =
          grid.combinedGlobalAttributes().toAttributes(language); // make a copy to be safe
      // don't append .ncml request to history attribute
      writeNcmlAttributes(writer, atts, "  ");

      // dimensions
      int nAxisVariables = grid.axisVariables().length;
      StringBuilder dvShape = new StringBuilder();
      for (int av = 0; av < nAxisVariables; av++) {
        //  <dimension name="lat" length="16384" />
        EDVGridAxis ega = grid.axisVariables()[av];
        dvShape.append((av == 0 ? "" : " ") + ega.destinationName());
        writer.write(
            "  <dimension name=\""
                + XML.encodeAsXML(ega.destinationName())
                + "\" length=\""
                + ega.sourceValues().size()
                + "\" />\n");
      }

      // axis variables
      for (int av = 0; av < nAxisVariables; av++) {
        //  <variable name="lat" shape="lat" type="double">
        EDVGridAxis ega = grid.axisVariables()[av];
        writer.write(
            "  <variable name=\""
                + XML.encodeAsXML(ega.destinationName())
                + "\" shape=\""
                + XML.encodeAsXML(ega.destinationName())
                + "\" ");
        atts =
            ega.combinedAttributes().toAttributes(language); // make a copy since it may be changed
        String type = ega.destinationDataType();
        if (type.equals("long")) {
          type = "String"; // but trouble since there will be no NcHelper.StringLengthSuffix _strlen
          // dimension
          atts.add("NcHelper", NcHelper.originally_a_LongArray);
        } else if (type.equals("char")) {
          type = "short";
          atts.add("NcHelper", NcHelper.originally_a_CharArray);
        }
        // NCML types same as Java: String (default), byte, short, int, float, double (and long, but
        // not in nc3)
        if (!type.equals("String")) writer.write("type=\"" + type + "\"");
        writer.write(">\n");
        writeNcmlAttributes(writer, atts, "    ");
        writer.write("  </variable>\n");
      }

      // data variables
      for (EDV edv : grid.dataVariables()) {
        writer.write(
            "  <variable name=\""
                + XML.encodeAsXML(edv.destinationName())
                + "\" shape=\""
                + XML.encodeAsXML(dvShape.toString())
                + "\" ");
        String type = edv.destinationDataType();
        if (type.equals("long")) {
          type = "String"; // but trouble since there will be no NcHelper.StringLengthSuffix _strlen
          // dimension
          atts.add("NcHelper", NcHelper.originally_a_LongArray);
        } else if (type.equals("char")) {
          type = "short";
          atts.add("NcHelper", NcHelper.originally_a_CharArray);
        }
        // NCML types same as Java: String (default), byte, short, int, float, double (and long, but
        // not in nc3)
        if (!type.equals("String")) writer.write("type=\"" + type + "\"");
        writer.write(">\n");
        Attributes tAtts = edv.combinedAttributes().toAttributes(language); // use a copy

        PAType paType = edv.destinationDataPAType();
        if (paType == PAType.STRING) tAtts.add(File2.ENCODING, File2.ISO_8859_1);
        // disabled until there is a standard
        //            else if (paType == PAType.CHAR)
        //                tAtts.add(String2.CHARSET, File2.ISO_8859_1);

        writeNcmlAttributes(writer, tAtts, "    ");
        writer.write("  </variable>\n");
      }

      writer.write("</netcdf>\n");
      writer.flush(); // essential
    }

    // diagnostic
    if (EDDGrid.reallyVerbose)
      String2.log(
          "  EDDGrid.saveAsNcML done. TIME=" + (System.currentTimeMillis() - time) + "ms\n");
  }
}
