/* 
 * TwoTable Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.*;
import com.cohort.util.*;

import gov.noaa.pfel.coastwatch.griddata.DataHelper;
import gov.noaa.pfel.coastwatch.util.SSR;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.List;
import java.util.GregorianCalendar;
import java.util.Vector;

/**
 * Get netcdfAll-......jar from ftp://ftp.unidata.ucar.edu/pub
 * and copy it to <context>/WEB-INF/lib renamed as netcdf-latest.jar.
 * Put it in the classpath for the compiler and for Java.
 */
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
//import ucar.nc2.dods.*;
import ucar.nc2.util.*;
import ucar.ma2.*;

/** The Java DAP classes.  */
import dods.dap.*;

/**
 * THIS IS NOT FINISHED.
 * This class has methods to deal with two related Tables, e.g., a dataTable
 * for meteorological stations and a groupTable with station information,
 * linked by common values in an ID column in each table.
 *
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2006-03-07
 */
public class TwoTable  {

    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;

    public Table dataTable;
    public Table groupTable;
    public int dataIDColumn;
    public int groupIDColumn;

    /** testDir is used for tests. */
    public static String testDir = 
        String2.getClassPath() + //with / separator and / at the end
        "gov/noaa/pfel/coastwatch/pointdata/";


    /**
     * This saves the information in the two tables in an .nc file.
     * The data is written as separate variables, sharing a common 
     * record dimension or a common group dimension.
     * The data values are written as their current data type 
     * (e.g., float or int).
     * This overwrites any existing file of the specified name.
     * This makes an effort not to create a partial file if there is an error.
     * If no exception is thrown, the file was successfully created.
     * 
     * @param fullName The full file name (dir + name + ext (usually .nc))
     * @throws Exception 
     */
    public void saveAsNc(String fullName) throws Exception {
        long time = System.currentTimeMillis();

        //delete any existing file
        File2.delete(fullName);

        //POLICY: because this procedure may be used in more than one thread,
        //do work on unique temp files names using randomInt, then rename to proper file name.
        //If procedure fails half way through, there won't be a half-finished file.
        int randomInt = Math2.random(Integer.MAX_VALUE);

        //open the file (before 'try'); if it fails, no temp file to delete
        NetcdfFileWriter nc = new NetcdfFileWriter(
            NetcdfFileWriter.Version.netcdf3, fullName + randomInt);
        
        try {
            Group rootGroup = nc.addGroup(null, "");
            nc.setFill(false);

            //ensure dataTable has at least one row (.nc requres that)
            int nDataRows = dataTable.nRows();
            int nDataColumns = dataTable.nColumns();
            if (nDataRows == 0) {
                for (int col = 0; col < nDataColumns; col++) {
                    dataTable.getColumn(col).addDouble(Double.NaN);
                }
                nDataRows = 1;
            }

            //ensure groupTable has at least one row (.nc requres that)
            int nGroupRows = groupTable.nRows();
            int nGroupColumns = groupTable.nColumns();
            if (nGroupRows == 0) {
                for (int col = 0; col < nDataColumns; col++) {
                    dataTable.getColumn(col).addDouble(Double.NaN);
                }
                nDataRows = 1;
            }



            //define the dimensions
            Dimension obsDimension  = nc.addDimension(rootGroup, "obs", nRows);
//javadoc says: if there is an unlimited dimension, all variables that use it are in a structure
//Dimension rowDimension  = nc.addDimension(rootGroup, "row", nRows, true, true, false); //isShared, isUnlimited, isUnknown
//String2.log("unlimitied dimension exists: " + (nc.getUnlimitedDimension() != null));

            //add the variables
            for (int col = 0; col < nColumns; col++) {
                PrimitiveArray pa = getColumn(col);
                Class type = pa.elementClass();
                String tColName = getColumnNameWithoutSpaces(col);
                if (type == String.class) {
                    int max = Math.max(1, ((StringArray)pa).maxStringLength()); //nclib wants at least 1
                    Dimension lengthDimension = nc.addDimension(rootGroup, 
                        tcolName + NcHelper.StringLengthSuffix, max);
                    nc.addVariable(rootGroup, tColName, char.class, 
                        Arrays.asList(obsDimension, lengthDimension)); 
                } else {
                    nc.addVariable(rootGroup, tColName, type, Arrays.asList(obsDimension)); 
                }
//nc.addMemberVariable(recordStructure, nc.findVariable(tColName));
            }

//boolean bool = nc.addRecordStructure(); //creates a structure variable called "record"         
//String2.log("addRecordStructure: " + bool);
//Structure recordStructure = (Structure)nc.findVariable("record");

            //write Attributes
            if (globalAttributes != null) {
                for (int i = 0; i < globalAttributes.size(); i += 2) { 
                    //String2.log("Attribute: " + globalAttributes.get(i) + " is " + globalAttributes.get(i+1));
                    nc.addGroupAttribute(rootGroup, (String)globalAttributes.get(i), 
                        DataHelper.getNc1DArray(((PrimitiveArray)globalAttributes.get(i+1)).toObjectArray()));
                }
            }
            if (columnAttributes != null) {
                for (int col = 0; col < nColumns; col++) {
                    ArrayList tAttributes = (ArrayList)columnAttributes.get(col);
                    for (int i = 0; i < tAttributes.size(); i += 2) {
                        nc.addVariableAttribute(getColumnNameWithoutSpaces(col), 
                            (String)tAttributes.get(i), 
                            DataHelper.getNc1DArray(((PrimitiveArray)tAttributes.get(i+1)).toObjectArray()));
                    }
                }
            }

            //leave "define" mode
            nc.create();

            //write the data
            for (int col = 0; col < nColumns; col++)
                nc.write(getColumnNameWithoutSpaces(col), 
                    DataHelper.getNc1DArray(getColumn(col).toObjectArray()));

            //if close throws exception, it is trouble
            nc.close(); //it calls flush() and doesn't like flush called separately
            nc = null;

            //rename the file to the specified name
            File2.rename(fullName + randomInt, fullName);

            //diagnostic
            if (verbose)
                String2.log("Table.saveAsNc success=" + success + 
                    " created in " + (System.currentTimeMillis() - time) + 
                    " ms\n  fileName=" + fullName);
            //String2.log(NcHelper.ncdump(directory + name + ext, "-h"));

        } catch (Exception e) {

            //try to close the file
            try {
                if (nc != null)
                    nc.close(); //it calls flush() and doesn't like flush called separately
            } catch (Exception e2) {
                //don't care
            }

            //delete the partial file
            File2.delete(fullName + randomInt);

            throw e;
        }

    }


    /**
     * A main method -- used to test the methods in this class.
     *
     * @param args is ignored  (use null)
     * @throws Exception if trouble
     */
    public static void main(String args[]) throws Exception {

        TwoTable.verbose = true;
       
        //readWrite tests
//        testNc();

        //done
        String2.log("\n***** TwoTable.main finished successfully");

    }


}
