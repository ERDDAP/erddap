/*
 * EDDTableFromAllDatasets Copyright 2013, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.util.Subscriptions;
import gov.noaa.pfel.erddap.variable.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class represents a table of all datasets in this ERDDAP.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2013-12-23
 */
public class EDDTableFromAllDatasets extends EDDTable {

  public static final String DATASET_ID = "allDatasets";

  /** set by the constructor */
  private ConcurrentHashMap<String, EDDGrid> gridDatasetHashMap;

  private ConcurrentHashMap<String, EDDTable> tableDatasetHashMap;

  /**
   * The constructor. This is a built-in class with no options. It is not specified in datasets.xml.
   * LoadDatasets always insures it is in tableDatasetHashMap.
   *
   * @throws Throwable if trouble
   */
  public EDDTableFromAllDatasets(
      ConcurrentHashMap tGridDatasetHashMap, ConcurrentHashMap tTableDatasetHashMap)
      throws Throwable {

    if (verbose) String2.log("\n*** constructing EDDTableFromAllDatasets");
    long constructionStartMillis = System.currentTimeMillis();
    String errorInMethod = "Error in EDDTableFromAllDatasets() constructor:\n";
    int language = 0;

    // save some of the parameters
    gridDatasetHashMap = tGridDatasetHashMap;
    tableDatasetHashMap = tTableDatasetHashMap;

    // set superclass variables
    className = "EDDTableFromAllDatasets";
    datasetID = DATASET_ID;
    setAccessibleTo(null);
    setGraphsAccessibleTo(null);
    onChange = new StringArray();
    fgdcFile = null;
    iso19115File = null;
    sosOfferingPrefix = null;
    defaultDataQuery = null;
    defaultGraphQuery = "maxLongitude,maxLatitude";
    publicSourceUrl = EDStatic.preferredErddapUrl;
    setReloadEveryNMinutes(1000000000); // i.e. never
    localSourceUrl = null;

    // let EDDTable handle all constraints
    sourceCanConstrainNumericData = CONSTRAIN_NO;
    sourceNeedsExpandedFP_EQ = false;
    sourceCanConstrainStringData = CONSTRAIN_NO;
    sourceCanConstrainStringRegex = "";

    // create dataVariables[]
    Table table = makeDatasetTable(language, null);
    sourceGlobalAttributes = table.globalAttributes();
    addGlobalAttributes = new Attributes();
    combinedGlobalAttributes =
        new Attributes(addGlobalAttributes, sourceGlobalAttributes); // order is important
    int ndv = table.nColumns();
    dataVariables = new EDV[ndv];
    for (int dv = 0; dv < ndv; dv++) {
      String colName = table.getColumnName(dv);
      Attributes atts = table.columnAttributes(dv);
      PrimitiveArray pa = table.getColumn(dv);
      if (Calendar2.SECONDS_SINCE_1970.equals(atts.getString("units"))) {
        dataVariables[dv] =
            new EDVTimeStamp(
                datasetID,
                colName,
                colName,
                atts,
                null, // sourceAtts, addAtts
                pa.elementTypeString()); // this constructor gets source / sets destination
        // actual_range
      } else {
        dataVariables[dv] =
            new EDV(
                datasetID,
                colName,
                colName,
                atts,
                null, // sourceAtts, addAtts
                pa.elementTypeString());
        // actual_range of vars in this table always NaN,NaN
        dataVariables[dv].setActualRangeFromDestinationMinMax();
      }
    }

    // during development only:
    // String2.log("\nsuggestKeywords=" + String2.toCSSVString(suggestKeywords(table, table)));

    // Don't gather ERDDAP sos information. This is never a SOS dataset.

    // ensure the setup is valid
    ensureValid();

    // finally
    long cTime = System.currentTimeMillis() - constructionStartMillis;
    if (verbose)
      String2.log(
          (debugMode ? "\n" + toString() : "")
              + "\n*** EDDTableFromAllDatasets constructor finished. TIME="
              + cTime
              + "ms"
              + (cTime >= 600000 ? "  (>10m!)" : cTime >= 10000 ? "  (>10s!)" : "")
              + "\n");
  }

  /**
   * This returns true if this EDDTable knows each variable's actual_range (e.g., EDDTableFromFiles)
   * or false if it doesn't (e.g., EDDTableFromDatabase).
   *
   * @returns true if this EDDTable knows each variable's actual_range (e.g., EDDTableFromFiles) or
   *     false if it doesn't (e.g., EDDTableFromDatabase).
   */
  @Override
  public boolean knowsActualRange() {
    return true;
  } // but irrelevant, because this will never be a child dataset

  /**
   * This overwrites the superclass to give the on-the-fly subsetVariables table.
   *
   * @param language the index of the selected language
   * @param loggedInAs This is used, e.g., for POST data (where the distinct subsetVariables table
   *     is different for each loggedInAs!) and for EDDTableFromAllDatasets.
   */
  @Override
  public Table subsetVariablesDataTable(int language, String loggedInAs) throws Throwable {
    Table table = makeDatasetTable(language, loggedInAs);
    table.reorderColumns(new StringArray(subsetVariables()), true); // discard others
    table.ascendingSortIgnoreCase(subsetVariables());
    table.removeDuplicates();

    // if no rows, add an empty row to avoid trouble
    if (table.nRows() == 0) {
      table.getColumn(0).addNDoubles(1, Double.NaN);
      table.makeColumnsSameSize();
    }

    return table;
  }

  /**
   * This overwrites the superclass to give the on-the-fly distinctSubsetVariables table.
   *
   * <p>time columns are epochSeconds.
   *
   * @param language the index of the selected language
   * @param loggedInAs This is used, e.g., for POST data (where the distinct subsetVariables table
   *     is different for each loggedInAs!) and for EDDTableFromAllDatasets.
   * @param loadVars the specific destinationNames to be loaded (or null for all subsetVariables)
   * @return the table with one column for each subsetVariable. The columns are sorted separately
   *     and have DIFFERENT SIZES! So it isn't a valid table! The table will have full metadata.
   */
  @Override
  public Table distinctSubsetVariablesDataTable(int language, String loggedInAs, String loadVars[])
      throws Throwable {

    // read the combinations table
    // this will throw exception if trouble
    Table table = subsetVariablesDataTable(language, loggedInAs);

    // just the loadVars
    if (loadVars != null) table.reorderColumns(new StringArray(loadVars), true); // discardOthers

    // find the distinct values
    for (int v = 0; v < table.nColumns(); v++) {
      PrimitiveArray pa = table.getColumn(v);
      pa.sortIgnoreCase();
      pa.removeDuplicates(false);
    }

    return table;
  }

  /**
   * This makes a sorted table of the datasets' info.
   *
   * <p>time columns are epochSeconds.
   *
   * @param language the index of the selected language
   * @param loggedInAs the name of the logged in user (or null if not logged in). This is used to
   *     ensure that the user sees only datasets they have a right to know exist.
   * @return table a table with plain text information about the datasets
   */
  public Table makeDatasetTable(int language, String loggedInAs) {

    StringArray datasetIDs = new StringArray(gridDatasetHashMap.keys());
    datasetIDs.append(new StringArray(tableDatasetHashMap.keys()));

    String tErddapUrl = EDStatic.erddapUrl(loggedInAs, language);
    String roles[] = EDStatic.getRoles(loggedInAs);
    boolean isLoggedIn = loggedInAs != null && !loggedInAs.equals(EDStatic.loggedInAsHttps);
    double nowES = System.currentTimeMillis() / 1000.0;

    // create the table and the global attributes
    Table table = new Table();
    table
        .globalAttributes()
        .add("cdm_data_type", CDM_OTHER)
        .add("Conventions", "COARDS, CF-1.6, ACDD-1.3")
        .add("creator_name", EDStatic.adminIndividualName)
        .add("creator_email", EDStatic.adminEmail)
        .add("creator_url", tErddapUrl)
        .add("infoUrl", tErddapUrl)
        .add("institution", EDStatic.adminInstitution)
        .add("keywords", EDStatic.admKeywords)
        .add("license", EDStatic.standardLicense)
        .add("sourceUrl", publicSourceUrl)
        .add("subsetVariables", EDStatic.admSubsetVariables)
        .add("summary", EDStatic.admSummaryAr[language])
        // "* " is distinctive and almost ensures it will be sorted first (or close)
        .add("title", "* " + EDStatic.admTitleAr[language] + " *");

    // order here is not important
    StringArray idCol = new StringArray();
    StringArray accessCol = new StringArray();
    StringArray institutionCol = new StringArray();
    StringArray dataStructureCol = new StringArray();
    StringArray cdmCol = new StringArray();
    StringArray classCol = new StringArray();
    StringArray titleCol = new StringArray();
    DoubleArray minLongitude = new DoubleArray();
    DoubleArray maxLongitude = new DoubleArray();
    DoubleArray longitudeSpacing = new DoubleArray();
    DoubleArray minLatitude = new DoubleArray();
    DoubleArray maxLatitude = new DoubleArray();
    DoubleArray latitudeSpacing = new DoubleArray();
    DoubleArray minAltitude = new DoubleArray();
    DoubleArray maxAltitude = new DoubleArray();
    DoubleArray minTime = new DoubleArray();
    DoubleArray maxTime = new DoubleArray();
    DoubleArray timeSpacing = new DoubleArray();
    StringArray gdCol = new StringArray(); // griddap
    StringArray subCol = new StringArray();
    StringArray tdCol = new StringArray(); // tabledap
    StringArray magCol = new StringArray();
    StringArray sosCol = new StringArray();
    StringArray wcsCol = new StringArray();
    StringArray wmsCol = new StringArray();
    StringArray filesCol = new StringArray();
    StringArray fgdcCol = new StringArray();
    StringArray iso19115Col = new StringArray();
    StringArray metadataCol = new StringArray();
    StringArray sourceCol = new StringArray();
    StringArray infoUrlCol = new StringArray();
    StringArray rssCol = new StringArray();
    StringArray emailCol = new StringArray();
    StringArray summaryCol = new StringArray();
    StringArray testOutOfDateCol = new StringArray();
    FloatArray outOfDateCol = new FloatArray();

    // Create the table -- column order in final table is determined here.
    // !!! DON'T TRANSLATE COLUMN NAMES, SO CONSISTENT FOR ALL ERDDAPs
    // !!! ALL COLUMNS ALWAYS AVAILABLE, SO CONSISTENT FOR ALL ERDDAPS

    int col;
    col = table.addColumn("datasetID", idCol); // Dataset ID in /info
    int idColNumber = col;
    table
        .columnAttributes(col)
        .add(
            "fileAccessBaseUrl",
            tErddapUrl + "/info/") // can't be griddap|tabledap because not same for all datasets
        .add("fileAccessSuffix", "/index.html")
        .add("ioos_category", "Other")
        .add("long_name", EDStatic.advl_datasetID);
    col = table.addColumn("accessible", accessCol);
    table
        .columnAttributes(col)
        .add("comment", EDStatic.advc_accessibleAr[language])
        .add("ioos_category", "Other")
        .add("long_name", EDStatic.advl_accessibleAr[language]);
    col = table.addColumn("institution", institutionCol); // Institution
    table
        .columnAttributes(col)
        .add("ioos_category", "Other")
        .add("long_name", EDStatic.advl_institutionAr[language]);
    col = table.addColumn("dataStructure", dataStructureCol);
    table
        .columnAttributes(col)
        .add("comment", EDStatic.advc_dataStructureAr[language])
        .add("ioos_category", "Other")
        .add("long_name", EDStatic.advl_dataStructureAr[language])
        .add("references", EDStatic.advr_dataStructure);
    col = table.addColumn("cdm_data_type", cdmCol);
    table
        .columnAttributes(col)
        .add("ioos_category", "Other")
        .add("long_name", EDStatic.advl_cdm_data_typeAr[language])
        .add("references", EDStatic.advr_cdm_data_type);
    col = table.addColumn("class", classCol);
    table
        .columnAttributes(col)
        .add("ioos_category", "Other")
        .add("long_name", EDStatic.advl_classAr[language])
        .add("references", EDStatic.advr_class);
    col = table.addColumn("title", titleCol); // Title
    int titleColNumber = col;
    table
        .columnAttributes(col)
        .add("ioos_category", "Other")
        .add("long_name", EDStatic.advl_titleAr[language]);

    col = table.addColumn("minLongitude", minLongitude);
    table
        .columnAttributes(col)
        .add("ioos_category", "Location")
        .add("long_name", EDStatic.advl_minLongitudeAr[language])
        .add("units", EDV.LON_UNITS);
    col = table.addColumn("maxLongitude", maxLongitude);
    table
        .columnAttributes(col)
        .add("ioos_category", "Location")
        .add("long_name", EDStatic.advl_maxLongitudeAr[language])
        .add("units", EDV.LON_UNITS);
    col = table.addColumn("longitudeSpacing", longitudeSpacing);
    table
        .columnAttributes(col)
        .add("ioos_category", "Location")
        .add("long_name", EDStatic.advl_longitudeSpacingAr[language])
        .add("units", EDV.LON_UNITS);
    col = table.addColumn("minLatitude", minLatitude);
    table
        .columnAttributes(col)
        .add("ioos_category", "Location")
        .add("long_name", EDStatic.advl_minLatitudeAr[language])
        .add("units", EDV.LAT_UNITS);
    col = table.addColumn("maxLatitude", maxLatitude);
    table
        .columnAttributes(col)
        .add("ioos_category", "Location")
        .add("long_name", EDStatic.advl_maxLatitudeAr[language])
        .add("units", EDV.LAT_UNITS);
    col = table.addColumn("latitudeSpacing", latitudeSpacing);
    table
        .columnAttributes(col)
        .add("ioos_category", "Location")
        .add("long_name", EDStatic.advl_latitudeSpacingAr[language])
        .add("units", EDV.LAT_UNITS);
    col = table.addColumn("minAltitude", minAltitude);
    table
        .columnAttributes(col)
        .add("ioos_category", "Location")
        .add("long_name", EDStatic.advl_minAltitudeAr[language])
        .add("positive", "up")
        .add("units", "m");
    col = table.addColumn("maxAltitude", maxAltitude);
    table
        .columnAttributes(col)
        .add("ioos_category", "Location")
        .add("long_name", EDStatic.advl_maxAltitudeAr[language])
        .add("positive", "up")
        .add("units", "m");
    col = table.addColumn("minTime", minTime);
    table
        .columnAttributes(col)
        .add("ioos_category", "Time")
        .add("long_name", EDStatic.advl_minTimeAr[language])
        .add("units", Calendar2.SECONDS_SINCE_1970);
    col = table.addColumn("maxTime", maxTime);
    table
        .columnAttributes(col)
        .add("comment", EDStatic.advc_maxTimeAr[language])
        .add("ioos_category", "Time")
        .add("long_name", EDStatic.advl_maxTimeAr[language])
        .add("units", Calendar2.SECONDS_SINCE_1970);
    col = table.addColumn("timeSpacing", timeSpacing);
    table
        .columnAttributes(col)
        .add("ioos_category", "Time")
        .add("long_name", EDStatic.advl_timeSpacingAr[language])
        .add("units", "seconds");
    // other columns
    col = table.addColumn("griddap", gdCol); // just protocol name
    table
        .columnAttributes(col)
        .add("comment", EDStatic.advc_griddapAr[language])
        .add("ioos_category", "Other")
        .add("long_name", EDStatic.advl_griddapAr[language]);
    col = table.addColumn("subset", subCol);
    table
        .columnAttributes(col)
        .add("ioos_category", "Other")
        .add("long_name", EDStatic.advl_subsetAr[language]);
    col = table.addColumn("tabledap", tdCol);
    table
        .columnAttributes(col)
        .add("comment", EDStatic.advc_tabledapAr[language])
        .add("ioos_category", "Other")
        .add("long_name", EDStatic.advl_tabledapAr[language]);
    col = table.addColumn("MakeAGraph", magCol); // Make A Graph
    table
        .columnAttributes(col)
        .add("ioos_category", "Other")
        .add("long_name", EDStatic.advl_MakeAGraphAr[language]);
    col = table.addColumn("sos", sosCol);
    table
        .columnAttributes(col)
        .add("comment", EDStatic.advc_sosAr[language])
        .add("ioos_category", "Other")
        .add("long_name", EDStatic.advl_sosAr[language]);
    col = table.addColumn("wcs", wcsCol);
    table
        .columnAttributes(col)
        .add("ioos_category", "Other")
        .add("long_name", EDStatic.advl_wcsAr[language]);
    col = table.addColumn("wms", wmsCol);
    table
        .columnAttributes(col)
        .add("ioos_category", "Other")
        .add("long_name", EDStatic.advl_wmsAr[language]);
    col = table.addColumn("files", filesCol);
    table
        .columnAttributes(col)
        .add("comment", EDStatic.advc_filesAr[language])
        .add("ioos_category", "Other")
        .add("long_name", EDStatic.advl_filesAr[language]);
    col = table.addColumn("fgdc", fgdcCol);
    table
        .columnAttributes(col)
        .add("comment", EDStatic.advc_fgdcAr[language])
        .add("ioos_category", "Other")
        .add("long_name", EDStatic.advl_fgdcAr[language]);
    col = table.addColumn("iso19115", iso19115Col);
    table
        .columnAttributes(col)
        .add("comment", EDStatic.advc_iso19115Ar[language])
        .add("ioos_category", "Other")
        .add("long_name", EDStatic.advl_iso19115Ar[language]);
    col = table.addColumn("metadata", metadataCol); // Info
    table
        .columnAttributes(col)
        .add("comment", EDStatic.advc_metadataAr[language])
        .add("ioos_category", "Other")
        .add("long_name", EDStatic.advl_metadataAr[language]);
    col = table.addColumn("sourceUrl", sourceCol);
    table
        .columnAttributes(col)
        .add("ioos_category", "Other")
        .add("long_name", EDStatic.advl_sourceUrlAr[language]);
    col = table.addColumn("infoUrl", infoUrlCol); // Background Info
    table
        .columnAttributes(col)
        .add("ioos_category", "Other")
        .add("long_name", EDStatic.advl_infoUrlAr[language]);
    col = table.addColumn("rss", rssCol);
    table
        .columnAttributes(col)
        .add("ioos_category", "Other")
        .add("long_name", EDStatic.advl_rssAr[language]);
    col = table.addColumn("email", emailCol);
    table
        .columnAttributes(col)
        .add("comment", EDStatic.advc_emailAr[language])
        .add("ioos_category", "Other")
        .add("long_name", EDStatic.advl_emailAr[language]);
    col = table.addColumn("testOutOfDate", testOutOfDateCol);
    table
        .columnAttributes(col)
        .add("comment", EDStatic.advc_testOutOfDateAr[language])
        .add("ioos_category", "Other")
        .add("long_name", EDStatic.advl_testOutOfDateAr[language]);
    col = table.addColumn("outOfDate", outOfDateCol);
    table
        .columnAttributes(col)
        .add("comment", EDStatic.advc_outOfDateAr[language])
        .add("ioos_category", "Other")
        .add("long_name", EDStatic.advl_outOfDateAr[language]);
    col = table.addColumn("summary", summaryCol);
    table
        .columnAttributes(col)
        .add("ioos_category", "Other")
        .add("long_name", EDStatic.advl_summaryAr[language]);

    // add each dataset's information
    // only title, summary, institution, id are always accessible if !listPrivateDatasets
    for (int i = 0; i < datasetIDs.size(); i++) {
      String tId = datasetIDs.get(i);
      EDDGrid eddGrid = gridDatasetHashMap.get(tId);
      EDDTable eddTable = null;
      EDD edd = eddGrid;
      boolean isGrid = true;
      if (edd == null) {
        eddTable = tableDatasetHashMap.get(tId);
        edd = eddTable;
        isGrid = false;
      } else {
        eddGrid = (EDDGrid) edd;
      }
      if (edd == null) // perhaps just deleted
      continue;
      boolean isAccessible = edd.isAccessibleTo(roles);
      boolean graphsAccessible = isAccessible || edd.graphsAccessibleToPublic();
      if (!EDStatic.listPrivateDatasets && !isAccessible && !graphsAccessible) continue;

      // add this dataset's value to each column   (order is not important)
      idCol.add(tId);
      accessCol.add(
          edd.getAccessibleTo() == null
              ? "public"
              : isAccessible ? "yes" : graphsAccessible ? "graphs" : isLoggedIn ? "no" : "log in");
      institutionCol.add(edd.institution());
      dataStructureCol.add(isGrid ? "grid" : "table");
      cdmCol.add(edd.cdmDataType());
      classCol.add(edd.className());
      titleCol.add(edd.title());

      // lon
      EDV tedv;
      tedv =
          isGrid && eddGrid.lonIndex() >= 0
              ? eddGrid.axisVariables[eddGrid.lonIndex()]
              : !isGrid && eddTable.lonIndex() >= 0
                  ? eddTable.dataVariables[eddTable.lonIndex()]
                  : null;
      minLongitude.add(
          !graphsAccessible || tedv == null ? Double.NaN : tedv.destinationMinDouble());
      maxLongitude.add(
          !graphsAccessible || tedv == null ? Double.NaN : tedv.destinationMaxDouble());
      longitudeSpacing.add(
          graphsAccessible && isGrid && tedv != null
              ? ((EDVGridAxis) tedv).averageSpacing()
              : Double.NaN);

      // lat
      tedv =
          isGrid && eddGrid.latIndex() >= 0
              ? eddGrid.axisVariables[eddGrid.latIndex()]
              : !isGrid && eddTable.latIndex() >= 0
                  ? eddTable.dataVariables[eddTable.latIndex()]
                  : null;
      minLatitude.add(!graphsAccessible || tedv == null ? Double.NaN : tedv.destinationMinDouble());
      maxLatitude.add(!graphsAccessible || tedv == null ? Double.NaN : tedv.destinationMaxDouble());
      latitudeSpacing.add(
          graphsAccessible && isGrid && tedv != null
              ? ((EDVGridAxis) tedv).averageSpacing()
              : Double.NaN);

      // alt or depth
      tedv =
          isGrid && eddGrid.altIndex() >= 0
              ? eddGrid.axisVariables[eddGrid.altIndex()]
              : !isGrid && eddTable.altIndex() >= 0
                  ? eddTable.dataVariables[eddTable.altIndex()]
                  : null;
      if (tedv == null) {
        // depth?
        tedv =
            isGrid && eddGrid.depthIndex() >= 0
                ? eddGrid.axisVariables[eddGrid.depthIndex()]
                : !isGrid && eddTable.depthIndex() >= 0
                    ? eddTable.dataVariables[eddTable.depthIndex()]
                    : null;
        minAltitude.add(
            !graphsAccessible || tedv == null ? Double.NaN : -tedv.destinationMinDouble());
        maxAltitude.add(
            !graphsAccessible || tedv == null ? Double.NaN : -tedv.destinationMaxDouble());
      } else {
        // alt
        minAltitude.add(
            !graphsAccessible || tedv == null ? Double.NaN : tedv.destinationMinDouble());
        maxAltitude.add(
            !graphsAccessible || tedv == null ? Double.NaN : tedv.destinationMaxDouble());
      }

      // time
      tedv =
          isGrid && eddGrid.timeIndex() >= 0
              ? eddGrid.axisVariables[eddGrid.timeIndex()]
              : !isGrid && eddTable.timeIndex() >= 0
                  ? eddTable.dataVariables[eddTable.timeIndex()]
                  : null;
      minTime.add(!graphsAccessible || tedv == null ? Double.NaN : tedv.destinationMinDouble());
      double tMaxTime =
          !graphsAccessible || tedv == null ? Double.NaN : tedv.destinationMaxDouble();
      maxTime.add(tMaxTime);
      timeSpacing.add(
          graphsAccessible && isGrid && tedv != null
              ? ((EDVGridAxis) tedv).averageSpacing()
              : Double.NaN);

      // outOfDate
      double ood = Double.NaN;
      String oods = edd.combinedGlobalAttributes().getString("testOutOfDate");
      testOutOfDateCol.add(String2.isSomething(oods) ? oods : "");
      if (!Double.isNaN(tMaxTime) && String2.isSomething(oods)) {
        double nmes = Calendar2.safeNowStringToEpochSeconds(oods, Double.NaN);
        if (!Double.isNaN(nmes)) {
          if (nmes < nowES) // now-   For near-real-time    //! specifically not <=
            //          howLate      / scaleFactor
            ood = (nowES - tMaxTime) / Math.abs(nowES - nmes);
          else // now+    for a forecast, assume updates roughly every day
            //         e.g., 8day forecast: now+6days
            //         howLate      / scaleFactor
            ood =
                (nmes + 2 * Calendar2.SECONDS_PER_DAY - tMaxTime) / (2 * Calendar2.SECONDS_PER_DAY);
        }
      }
      outOfDateCol.add(Math2.doubleToFloatNaN(ood)); // all errors -> NaN

      // other
      String daps =
          tErddapUrl + "/" + edd.dapProtocol() + "/" + tId; // without an extension, so easy to add
      gdCol.add(isAccessible && edd instanceof EDDGrid ? daps : "");
      subCol.add(isAccessible && edd.accessibleViaSubset().length() == 0 ? daps + ".subset" : "");
      tdCol.add(isAccessible && edd instanceof EDDTable ? daps : "");
      magCol.add(
          graphsAccessible && edd.accessibleViaMAG().length() == 0
              ? // graphs
              daps + ".graph"
              : "");
      sosCol.add(
          isAccessible && edd.accessibleViaSOS().length() == 0
              ? tErddapUrl + "/sos/" + tId + "/" + EDDTable.sosServer
              : "");
      wcsCol.add(
          isAccessible && edd.accessibleViaWCS().length() == 0
              ? tErddapUrl + "/wcs/" + tId + "/" + EDDGrid.wcsServer
              : "");
      wmsCol.add(
          graphsAccessible && edd.accessibleViaWMS().length() == 0
              ? // graphs
              tErddapUrl + "/wms/" + tId + "/" + EDD.WMS_SERVER
              : "");
      filesCol.add(
          isAccessible && edd.accessibleViaFiles ? tErddapUrl + "/files/" + tId + "/" : "");
      fgdcCol.add(
          graphsAccessible && edd.accessibleViaFGDC().length() == 0
              ? tErddapUrl
                  + "/"
                  + EDStatic.fgdcXmlDirectory
                  + edd.datasetID()
                  + EDD.fgdcSuffix
                  + ".xml"
              : "");
      iso19115Col.add(
          graphsAccessible && edd.accessibleViaISO19115().length() == 0
              ? tErddapUrl
                  + "/"
                  + EDStatic.iso19115XmlDirectory
                  + edd.datasetID()
                  + EDD.iso19115Suffix
                  + ".xml"
              : "");
      metadataCol.add(graphsAccessible ? tErddapUrl + "/info/" + edd.datasetID() + "/index" : "");
      sourceCol.add(graphsAccessible ? edd.publicSourceUrl() : "");
      infoUrlCol.add(graphsAccessible ? edd.infoUrl() : "");
      rssCol.add(
          graphsAccessible
              ? EDStatic.erddapUrl + "/rss/" + edd.datasetID() + ".rss"
              : ""); // never https url
      emailCol.add(
          graphsAccessible && EDStatic.subscriptionSystemActive
              ? tErddapUrl
                  + "/"
                  + Subscriptions.ADD_HTML
                  + "?datasetID="
                  + edd.datasetID()
                  + "&showErrors=false&email="
              : "");
      summaryCol.add(edd.summary());
    }

    // for testing: table.ensureValid();

    table.sortIgnoreCase(new int[] {titleColNumber, idColNumber}, new boolean[] {true, true});
    return table;
  }

  /**
   * This gets the data (chunk by chunk) from this EDDTable for the OPeNDAP DAP-style query and
   * writes it to the TableWriter. See the EDDTable method documentation.
   *
   * @param language the index of the selected language
   * @param loggedInAs the user's login name if logged in (or null if not logged in).
   * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
   * @param userDapQuery the part of the user's request after the '?', still percentEncoded, may be
   *     null.
   * @param tableWriter
   * @throws Throwable if trouble (notably, WaitThenTryAgainException)
   */
  @Override
  public void getDataForDapQuery(
      int language,
      String loggedInAs,
      String requestUrl,
      String userDapQuery,
      TableWriter tableWriter)
      throws Throwable {

    Table table = makeDatasetTable(language, loggedInAs);
    standardizeResultsTable(language, requestUrl, userDapQuery, table);
    tableWriter.writeAllAndFinish(table);
  }
}
