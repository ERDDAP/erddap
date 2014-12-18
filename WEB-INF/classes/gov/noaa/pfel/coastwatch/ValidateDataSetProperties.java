/* 
 * ValidateDataSetPropterties Copyright 2006, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch;

import com.cohort.util.File2;
import com.cohort.util.ResourceBundle2;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.griddata.FileNameUtility;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;

/**
 * This class is designed to be a stand-alone program to 
 * validate that the DataSet.properties file contains valid information
 * for all datasets listed by validDataSets in DataSet.properties.
 * Don't run this on the coastwatch computer.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2006-10-06
 *
 */
public class ValidateDataSetProperties {

    /**
     * This class is designed to be a stand-alone program to 
     * validate that the DataSet.properties file contains valid information
     * for all datasets listed by validDataSets in DataSet.properties.
     * Don't run this on the coastwatch computer.
     *
     * @param args is ignored
     */
    public static void main(String args[]) throws Exception {

        String2.log("ValidatDataSetProperties (testing DataSet.properties validDataSets");

        //find a browser properties file (e.g., CWBrowser.properties)
        String contextDirectory = SSR.getContextDirectory(); //with / separator and / at the end
        String[] propList = RegexFilenameFilter.list(
            contextDirectory + "WEB-INF/classes/gov/noaa/pfel/coastwatch/", ".+\\.properties");
        int which = -1;
        for (int i = 0; i < propList.length; i++) {
            if (!propList[i].equals("DataSet.properties")) {
                which = i;
                break;
            }
        }
        Test.ensureNotEqual(which, -1, 
            String2.ERROR + ": No non-DataSet.properties properties files found in\n" +
            contextDirectory + "WEB-INF/classes/gov/noaa/pfel/coastwatch/.\n" +
            ".properties files=" + String2.toCSSVString(propList));
      
        FileNameUtility fnu = new FileNameUtility(
            "gov.noaa.pfel.coastwatch." + File2.getNameNoExtension(propList[which]));
        ResourceBundle2 dataSetRB2 = new ResourceBundle2("gov.noaa.pfel.coastwatch.DataSet");
        String infoUrlBaseUrl = dataSetRB2.getString("infoUrlBaseUrl", null);
        String tDataSetList[] = String2.split(dataSetRB2.getString("validDataSets", null), '`');
        int nDataSets = tDataSetList.length;
        //Test.ensureEqual(nDataSets, 228, "nDataSets"); //useful to me, but Dave can't add dataset without recompiling this
        String2.log("  testing " + nDataSets + " data sets");
        boolean excessivelyStrict = true;  
        for (int i = OneOf.N_DUMMY_GRID_DATASETS; i < nDataSets; i++) {  //"2" in order to skip 0=OneOf.NO_DATA, 1=BATHYMETRY
            String seven = tDataSetList[i];
            Test.ensureTrue(seven != null && seven.length() > 0, "  tDataSetList[" + i + "] is ''.");
            fnu.ensureValidDataSetProperties(seven, excessivelyStrict);
            String infoUrl = dataSetRB2.getString(seven + "InfoUrl", null);
            Test.ensureNotNull(infoUrl, seven + "InfoUrl is null.");
            SSR.getUrlResponse(infoUrlBaseUrl + infoUrl);  //on all computers except coastwatch, all are accessible as urls
        }
        String2.log("  ValidatDataSetProperties successfully tested n=" + nDataSets + 
            " last=" + tDataSetList[nDataSets - 1]);
    }
}
