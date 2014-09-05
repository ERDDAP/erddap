/* 
 * DigirObisTDSP Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.*;
import com.cohort.util.*;

import gov.noaa.pfel.coastwatch.griddata.DataHelper;
import gov.noaa.pfel.coastwatch.util.SSR;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.XMLReader;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.List;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
//import javax.xml.xpath.XPath;   //requires java 1.5

import ucar.nc2.NetcdfFile;

/**
 * This class implements a Thredds (version 3.16 or higher) Dataset Source Plugin for 
 * DiGIR (and Darwin and OBIS) data sources.
 * See http://www.unidata.ucar.edu/projects/THREDDS/tech/reference/DatasetSource.html .
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2007-05-07
 */
public class DigirObisTDSP implements thredds.servlet.DatasetSource {

    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;
    public static boolean reallyVerbose = false;

    /**
     * This returns true if this class can/should handle the request.
     * This method is defined by thredds.servlet.DatasetSource.
     *
     * @return true if this class can/should handle the request.
     */
    public boolean isMine(HttpServletRequest request) {
       String path = request.getPathInfo();
       return path.startsWith("/thredds/obis/");
    }

    /**
     * This method processes the request and returns a netcdf file.
     * This method is defined by thredds.servlet.DatasetSource.
     *
     * @return true if this class can/should handle the request.
     */
    public NetcdfFile getNetcdfFile(HttpServletRequest request, 
            HttpServletResponse response) throws IOException {

        try {
            //parse the request
            String path = request.getPathInfo();
            path = path.substring("/thredds/obis/".length());
String junk="";
            String url = junk;
            String resource = junk;
            String query = junk;
String2.log("  url=" + url);
String2.log("  resource=" + resource);
String2.log("  query=" + query);

            //get the data
            Table table = new Table();
            DigirHelper.searchObisOpendapStyle(new String[]{resource}, url,  
                query, table);

            //save the data in a file
            File tFile = File.createTempFile("Obis", ".nc");
            tFile.deleteOnExit();
            String fullName = tFile.getCanonicalPath() + tFile.getName();
String2.log("  fullName=" + fullName);
            table.saveAsFlatNc(fullName, "row");

            //return the file            
            return NetcdfFile.open(fullName);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

    }



}
