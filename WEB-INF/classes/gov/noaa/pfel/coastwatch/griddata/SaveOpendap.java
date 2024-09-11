/*
 * SaveOpendap Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.array.*;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.util.*;
import ucar.nc2.write.NetcdfFormatWriter;

/** The Java DAP classes. */
// import dods.dap.*;

/**
 * This class has some static methods to read opendap data sources and save the data in different
 * types of files.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2006-08-15
 */
public class SaveOpendap {

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  /**
   * Save tries to save all the variables (attributes and data) from an opendap source to an nc
   * file. Currently, it doesn't work with structures.
   *
   * @param url the opendap url
   * @param fullName The full file name (dir + name + ext (usually .nc))
   * @throws Exception
   */
  public static void asNc(String url, String fullName) throws Exception {
    long time = System.currentTimeMillis();
    if (verbose) String2.log("SaveOpendap.asNc" + "\n  url=" + url + "\n  fullName=" + fullName);

    // delete any existing file
    File2.delete(fullName);

    // POLICY: because this procedure may be used in more than one thread,
    // do work on unique temp files names using randomInt, then rename to proper file name.
    // If procedure fails half way through, there won't be a half-finished file.
    int randomInt = Math2.random(Integer.MAX_VALUE);

    // open the file (before 'try'); if it fails, no temp file to delete
    // DODSNetcdfFile in = new DODSNetcdfFile(url); //bug? showed no global attributes //but fixed
    // in 2.2.16
    // NetcdfFile in = NetcdfFiles.open(url);  //this fails: The server does not support byte
    // ranges.
    NetcdfDataset in = NetcdfDatasets.openDataset(url); // 2021: 's' is new API
    try {

      NetcdfFormatWriter ncWriter = null;

      try {
        NetcdfFormatWriter.Builder out = NetcdfFormatWriter.createNewNetcdf3(fullName + randomInt);
        Group.Builder outRootGroup = out.getRootGroup();
        out.setFill(false);

        // get the list of dimensions
        Group inRootGroup = in.getRootGroup();
        List globalDimList = inRootGroup.getDimensions();

        // add the dimensions to 'out'
        int nGlobalDim = globalDimList.size();
        if (verbose) String2.log("  nGlobalDim=" + nGlobalDim);
        for (int i = 0; i < nGlobalDim; i++) {
          Dimension dim = (Dimension) globalDimList.get(i);
          String dimName = dim.getName();
          Dimension tDim =
              NcHelper.addDimension(
                  outRootGroup,
                  dimName,
                  dim.getLength(),
                  true,
                  dim.isUnlimited(),
                  false); // isShared, ..., isVariableLength
          if (verbose) String2.log("    dimName" + i + "=" + dimName);
        }

        // get the list of opendap variables
        List inVarList = in.getVariables();

        // add the variables to .nc
        int nInVars = inVarList.size();
        if (verbose) String2.log("  nInVars=" + nInVars);
        Variable.Builder newVars[] = new Variable.Builder[nInVars];
        for (int v = 0; v < nInVars; v++) {
          // get the variable
          Variable inVar = (Variable) inVarList.get(v);
          String varName = inVar.getFullName();
          if (verbose) String2.log("    varName" + v + "=" + varName);

          // get nDimensions
          List tDimList = inVar.getDimensions();
          int nTDim = tDimList.size();

          // create dimension[]
          ArrayList<Dimension> dimList = new ArrayList();
          for (int d = 0; d < nTDim; d++) dimList.add((Dimension) tDimList.get(d));

          // add the variable to 'out'
          newVars[v] = NcHelper.addVariable(outRootGroup, varName, inVar.getDataType(), dimList);

          // write Attributes   (after adding variables)
          AttributeContainer attList = inVar.attributes();
          Iterator it = attList.iterator();
          while (it.hasNext()) newVars[v].addAttribute((ucar.nc2.Attribute) it.next());
        }

        // global attributes
        List globalAttList = in.getGlobalAttributes();
        int nGlobalAtt = globalAttList.size();
        if (verbose) String2.log("  nGlobalAtt=" + nGlobalAtt);
        for (int a = 0; a < nGlobalAtt; a++) {
          Attribute att = (Attribute) globalAttList.get(a);
          if (verbose) String2.log("  globalAtt name=" + att.getName());
          outRootGroup.addAttribute(att);
        }

        // leave "define" mode
        if (verbose) String2.log("  leaving 'define' mode");
        ncWriter = out.build();

        // write the data
        for (int v = 0; v < nInVars; v++) {
          Variable inVar = (Variable) inVarList.get(v);
          if (verbose) String2.log("  writing data inVar=" + inVar.getFullName());

          // write the data
          ncWriter.write(newVars[v].getFullName(), inVar.read());
        }

        // I care about this exception
        ncWriter.close();
        ncWriter = null;

      } catch (Exception e) {
        String2.log(NcHelper.ERROR_WHILE_CREATING_NC_FILE + MustBe.throwableToString(e));
        if (ncWriter != null) {
          try {
            ncWriter.abort();
          } catch (Exception e9) {
          }
          File2.delete(fullName + randomInt);
          ncWriter = null;
        }
        throw e;
      }

      // I care about this exception
      in.close();
      in = null;

      // rename the file to the specified name
      File2.rename(fullName + randomInt, fullName);

      // success!

    } catch (Exception e) {
      try {
        if (in != null) in.close(); // explicitly close it
      } catch (Exception e2) {
        // don't care
      }

      File2.delete(fullName + randomInt);

      throw e;
    }

    // diagnostic
    if (verbose)
      String2.log(
          "SaveOpendap.asNc done. created in "
              + Calendar2.elapsedTimeString(System.currentTimeMillis() - time)
              + "\n  fileName="
              + fullName);
    // String2.log(NcHelper.ncdump(directory + name + ext, "-h"));

  }

  /** This downloads the MBARI Mx station data. */
  public static void downloadMbariData() throws Exception {
    // one time transfer of MBARI data
    verbose = true;
    String dir = "c:/data/mbari/";
    String2.log("\n*** One time transfer of MBARI data to " + dir);

    for (int i = 0; i < 3; i++) // roughly ~4, ~57, ~37 minutes (crude remembrance)
    asNc(
          "http://dods.mbari.org/cgi-bin/nph-nc/data/OASISdata/netcdf/hourlyM" + i + ".nc",
          dir + "M" + i + ".nc");
  }

  /**
   * NOT YET FINISHED. A method to get data from an opendap source and save it in a file.
   *
   * @param args
   * @throws Exception if trouble
   */
  public static void main(String args[]) throws Exception {

    // done
    String2.log("\n***** SaveOpendap.main finished successfully");
    Math2.incgc("SaveOpendap.main (between tests)", 2000); // in a test
  }
}
