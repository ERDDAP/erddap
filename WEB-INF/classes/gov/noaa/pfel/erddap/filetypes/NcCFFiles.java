package gov.noaa.pfel.erddap.filetypes;

import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.dataset.EDDTable;
import gov.noaa.pfel.erddap.dataset.TableWriterAllWithMetadata;
import gov.noaa.pfel.erddap.util.EDStatic;

@FileTypeClass(
    fileTypeExtension = ".nc",
    fileTypeName = ".ncCF",
    infoUrl =
        "https://cfconventions.org/Data/cf-conventions/cf-conventions-1.8/cf-conventions.html#discrete-sampling-geometries",
    versionAdded = "1.30.0",
    availableGrid = false)
public class NcCFFiles extends CacheLockFiles {

  public NcCFFiles() {
    super(false);
  }

  protected NcCFFiles(boolean headerOverride) {
    super(headerOverride);
  }

  @Override
  protected void generateTableFile(DapRequestInfo requestInfo, String cacheFullName)
      throws Throwable {
    EDDTable edd = requestInfo.getEDDTable();
    // quick reject?
    if (edd.accessibleViaNcCF().length() > 0)
      throw new SimpleException(
          EDStatic.simpleBilingual(requestInfo.language(), EDStatic.messages.queryErrorAr)
              + edd.accessibleViaNcCF());

    String userDapQuery = requestInfo.userDapQuery();
    // check that query includes required variables
    if (userDapQuery == null) userDapQuery = "";
    String varNames = userDapQuery.substring(0, (userDapQuery + "&").indexOf('&'));
    if (varNames.length() == 0) {
      // ok since all vars are requested and we know dataset has required variables
    } else {
      // queries must include cf_role=timeseries_id var (or profile_id or trajectory_id)
      // queries must include longitude, latitude, time (and altitude or depth if in dataset)
      StringBuilder addVars = new StringBuilder();
      String varList[] = StringArray.arrayFromCSV(SSR.percentDecode(varNames));
      for (String requiredCfRequestVariable : edd.requiredCfRequestVariables()) {
        if (String2.indexOf(varList, requiredCfRequestVariable) < 0)
          addVars.append(requiredCfRequestVariable + ",");
        // throw new SimpleException(EDStatic.simpleBilingual(language,
        // EDStatic.messages.queryErrorAr) +
        //    ".ncCF queries for this dataset must include all of these variables: " +
        // String2.toCSSVString(requiredCfRequestVariables) + ".");
      }
      // add missing vars to beginning of userDapQuery (they're outer vars, so beginning is
      // good)
      userDapQuery = addVars + userDapQuery;

      // query must include at least one non-outer variable
      boolean ok = false;
      for (String s : varList) {
        if (String2.indexOf(edd.outerCfVariables(), s) < 0) {
          ok = true;
          break;
        }
      }
      if (!ok) {
        throw new SimpleException(
            EDStatic.simpleBilingual(requestInfo.language(), EDStatic.messages.queryErrorAr)
                + ".ncCF and .ncCFMA queries for this dataset must at least one variable not on this list: "
                + String2.toCSSVString(edd.outerCfVariables())
                + ".");
      }
    }

    // get the data
    TableWriterAllWithMetadata twawm =
        edd.getTwawmForDapQuery(
            requestInfo.language(),
            requestInfo.loggedInAs(),
            requestInfo.requestUrl(),
            userDapQuery);
    // if (debug) String2.log("\n>>after twawm, globalAttributes=\n" +
    // twawm.globalAttributes());

    // make .ncCF or .ncCFMA file
    boolean nodcMode =
        requestInfo.fileTypeName().equals(".ncCFMA")
            || requestInfo.fileTypeName().equals(".ncCFMAHeader");
    String cdmType =
        edd.combinedGlobalAttributes().getString(requestInfo.language(), "cdm_data_type");
    if (EDDTable.CDM_POINT.equals(cdmType)) {
      edd.saveAsNcCF0(requestInfo.language(), cacheFullName, twawm);
    } else if (EDDTable.CDM_TIMESERIES.equals(cdmType)
        || EDDTable.CDM_PROFILE.equals(cdmType)
        || EDDTable.CDM_TRAJECTORY.equals(cdmType)) {
      edd.saveAsNcCF1(requestInfo.language(), nodcMode, cacheFullName, twawm);
    } else if (EDDTable.CDM_TIMESERIESPROFILE.equals(cdmType)
        || EDDTable.CDM_TRAJECTORYPROFILE.equals(cdmType)) {
      edd.saveAsNcCF2(requestInfo.language(), nodcMode, cacheFullName, twawm);
    } else { // shouldn't happen, since accessibleViaNcCF checks this
      throw new SimpleException("unexpected cdm_data_type=" + cdmType);
    }

    File2.isFile(
        cacheFullName,
        5); // for possible waiting thread, wait till file is visible via operating system
    twawm.close();
  }

  @Override
  protected void generateGridFile(DapRequestInfo requestInfo, String cacheFullName)
      throws Throwable {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelp_ncCFAr[language];
  }
}
