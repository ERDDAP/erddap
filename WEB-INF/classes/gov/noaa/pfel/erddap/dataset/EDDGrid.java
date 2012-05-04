/* 
 * EDDGrid Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.IntArray;
import com.cohort.array.NDimensionalIndex;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

import dods.dap.*;

import gov.noaa.pfel.coastwatch.griddata.DataHelper;
import gov.noaa.pfel.coastwatch.griddata.Grid;
import gov.noaa.pfel.coastwatch.griddata.Matlab;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.sgt.CompoundColorMap;
import gov.noaa.pfel.coastwatch.sgt.GraphDataLayer;
import gov.noaa.pfel.coastwatch.sgt.SgtGraph;
import gov.noaa.pfel.coastwatch.sgt.SgtMap;
import gov.noaa.pfel.coastwatch.sgt.SgtUtil;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.util.*;
import gov.noaa.pfel.erddap.variable.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.RenderingHints;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ucar.ma2.Array;
import ucar.nc2.Dimension;
import ucar.nc2.geotiff.GeotiffWriter;
import ucar.nc2.NetcdfFileWriteable;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPointImpl;

/** 
 * This class represents a dataset where the results can be represented as 
 * a grid -- one or more EDV variables
 * sharing the same EDVGridAxis variables (in the same order).
 * If present, the lon, lat, alt, and time axisVariables
 * allow queries to be made in standard units (alt in m above sea level and
 * time as seconds since 1970-01-01T00:00:00Z or as an ISO date/time).
 * 
 * <p>Note that all variables for a given EDDGrid use the same 
 * axis variables. If there are source datasets that
 * serve variables which use different sets of axes, you have
 * to separate them out and create separate EDDGrids (one per 
 * set of axes).
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2007-06-04
 */
public abstract class EDDGrid extends EDD { 

    public final static String dapProtocol = "griddap";

    /** The constructor must set these. */
    protected EDVGridAxis axisVariables[];

    /** These are needed for EDD-required methods of the same name. */
    public final static String[] dataFileTypeNames = {  //
        ".asc", ".csv", ".csvp", ".das", ".dds", ".dods", 
        ".esriAscii", //".grd", ".hdf", 
        ".graph", ".help", ".html", ".htmlTable",
        ".json", 
        ".mat", ".nc", ".ncHeader", 
        ".odvTxt", ".tsv", ".tsvp", ".xhtml"};
    public final static String[] dataFileTypeExtensions = {
        ".asc", ".csv", ".csv", ".das", ".dds", ".dods", 
        ".asc", //".grd", ".hdf", 
        ".html", ".html", ".html", ".html",
        ".json", 
        ".mat", ".nc", ".txt", //.subset currently isn't included
        ".txt", ".tsv", ".tsv", ".xhtml"};
    //These all used to have " (It may take a while. Please be patient.)" at the end.
    public static String[] dataFileTypeDescriptions = {
        "View OPeNDAP-style comma-separated ASCII text.",
        "Download a comma-separated ASCII text table (line 1: names; line 2: units; ISO 8601 times).",
        "Download a .csv file with line 1: name (units). Times are ISO 8601 strings.",
        "View the data's metadata via an OPeNDAP Dataset Attribute Structure (DAS).",
        "View the data's structure via an OPeNDAP Dataset Descriptor Structure (DDS).",
        "OPeNDAP clients use this to download the data in the DODS binary format.",
        "Download an ESRI ASCII file (for lat lon data only; lon must be all below or all above 180).",
        //"Download a GMT-style NetCDF .grd file (for lat lon data only).",
        //"Download a Hierarchal Data Format Version 4 SDS file (for lat lon data only).",
        "View a Make A Graph web page.",
        "View a web page with a description of griddap.",
        "View an OPeNDAP-style HTML Data Access Form.",
        "View a .html web page with the data in a table. Times are ISO 8601 strings.",
        "View a table-like JSON file (missing value = 'null'; times are ISO 8601 strings).",
        "Download a MATLAB binary file.",
        "Download a NetCDF-3 binary file with COARDS/CF/THREDDS metadata.",
        "View the header (the metadata) for the NetCDF-3 file.",
        "Download time,latitude,longitude,otherVariables as an ODV Generic Spreadsheet File (.txt).",
        "Download a tab-separated ASCII text table (line 1: names; line 2: units; ISO 8601 times).",
        "Download a .tsv file with line 1: name (units). Times are ISO 8601 strings.",
        "View an XHTML (XML) file with the data in a table. Times are ISO 8601 strings."
    };
    public static String[] dataFileTypeInfo = {  //"" if not available
        "http://www.opendap.org/user/guide-html/guide_20.html#id4", //OPeNDAP ascii
        //csv: also see http://www.ietf.org/rfc/rfc4180.txt
        "http://en.wikipedia.org/wiki/Comma-separated_values", //csv was "http://www.creativyst.com/Doc/Articles/CSV/CSV01.htm", 
        "http://en.wikipedia.org/wiki/Comma-separated_values", //csv was "http://www.creativyst.com/Doc/Articles/CSV/CSV01.htm", 
        "http://www.opendap.org/user/guide-html/guide_66.html", //das
        "http://www.opendap.org/user/guide-html/guide_65.html", //dds
        "http://www.opendap.org", //dods
        "http://en.wikipedia.org/wiki/Esri_grid", //esriAscii
        //"http://gmt.soest.hawaii.edu/gmt/doc/html/GMT_Docs/node60.html", //grd
        //"http://www.hdfgroup.org/products/hdf4/", //hdf
        "http://coastwatch.pfeg.noaa.gov/erddap/griddap/index.html#GraphicsCommands", //GraphicsCommands
        "http://www.opendap.org/user/guide-html/guide_20.html#id8", //help
        "http://www.opendap.org/user/guide-html/guide_21.html", //html
        "http://www.w3schools.com/html/html_tables.asp", //htmlTable
        "http://www.json.org/", //json
        "http://www.mathworks.com/", //mat
        "http://www.unidata.ucar.edu/software/netcdf/", //nc
        "http://www.unidata.ucar.edu/software/netcdf/docs/ncdump-man-1.html", //ncHeader
        "http://odv.awi.de/en/documentation/", //odv
        "http://www.cs.tut.fi/~jkorpela/TSV.html",  //tsv
        "http://www.cs.tut.fi/~jkorpela/TSV.html",  //tsv
        "http://www.w3schools.com/html/html_tables.asp" //xhtml
        //"http://www.tizag.com/htmlT/tables.php" //xhtml
    };

    public final static String[] imageFileTypeNames = {
        ".geotif", ".kml", 
        ".smallPdf", ".pdf", ".largePdf", ".smallPng", ".png", ".largePng", ".transparentPng"
        };
    public final static String[] imageFileTypeExtensions = {
        ".tif", ".kml", 
        ".pdf", ".pdf", ".pdf", ".png", ".png", ".png", ".png"
        };
    public static String[] imageFileTypeDescriptions = {
        "View a GeoTIFF .tif image file (for lat lon data only; lon must be all below or all above 180).",
        "View a Google Earth .kml file (for lat, lon, [time] results only)",
        "View a small .pdf image file with a graph or map.",
        "View a standard, medium-sized, .pdf image file with a graph or map.",
        "View a large .pdf image file with a graph or map.",
        "View a small .png image file with a graph or map.",
        "View a standard, medium-sized .png image file with a graph or map.",
        "View a large .png image file with a graph or map.",
        "View a .png image file (just the data, without axes, landmask, or legend)."
        };  //.transparentPng: if lon and lat are evenly spaced, .png size will be 1:1; otherwise, 1:1 but morphed a little
    public static String[] imageFileTypeInfo = {
        "http://trac.osgeo.org/geotiff/", //geotiff
        "http://earth.google.com/", //kml
        "http://www.adobe.com/products/acrobat/adobepdf.html", //pdf
        "http://www.adobe.com/products/acrobat/adobepdf.html", //pdf
        "http://www.adobe.com/products/acrobat/adobepdf.html", //pdf
        "http://www.libpng.org/pub/png/", //png
        "http://www.libpng.org/pub/png/", //png
        "http://www.libpng.org/pub/png/", //png
        "http://www.libpng.org/pub/png/" //png
    };

    private static String[] allFileTypeOptions, allFileTypeNames;
    private static int defaultFileTypeOption = 0; //will be reset below

    //wcs
    public final static String wcsServer = "server";
    public final static String wcsVersion = "1.0.0"; //, "1.1.0", "1.1.1", "1.1.2"};
    //wcsResponseFormats parallels wcsRequestFormats
    public final static String wcsRequestFormats100[]  = {"GeoTIFF", "NetCDF3", "PNG"             };
    public final static String wcsResponseFormats100[] = {".geotif", ".nc",     ".transparentPng" };
    //public final static String wcsRequestFormats112[]  = {"image/tiff", "application/x-netcdf", "image/png"};
    //public final static String wcsResponseFormats112[] = {".geotif",    ".nc",                  ".transparentPng"};
    public final static String wcsExceptions = "application/vnd.ogc.se_xml";

    //static constructor
    static {
        int nDFTN = dataFileTypeNames.length;
        int nIFTN = imageFileTypeNames.length;
        Test.ensureEqual(nDFTN, dataFileTypeDescriptions.length,
            "'dataFileTypeNames.length' not equal to 'dataFileTypeDescriptions.length'.");                                     
        Test.ensureEqual(nDFTN, dataFileTypeExtensions.length,
            "'dataFileTypeNames.length' not equal to 'dataFileTypeExtensions.length'.");                                     
        Test.ensureEqual(nDFTN, dataFileTypeInfo.length,
            "'dataFileTypeNames.length' not equal to 'dataFileTypeInfo.length'.");                                     
        Test.ensureEqual(nIFTN, imageFileTypeDescriptions.length,
            "'imageFileTypeNames.length' not equal to 'imageFileTypeDescriptions.length'.");                                     
        Test.ensureEqual(nIFTN, imageFileTypeExtensions.length,
            "'imageFileTypeNames.length' not equal to 'imageFileTypeExtensions.length'.");                                     
        Test.ensureEqual(nIFTN, imageFileTypeInfo.length,
            "'imageFileTypeNames.length' not equal to 'imageFileTypeInfo.length'.");                                     
        defaultFileTypeOption = String2.indexOf(dataFileTypeNames, ".htmlTable");

        //construct allFileTypeOptions
        allFileTypeOptions = new String[nDFTN + nIFTN];
        allFileTypeNames = new String[nDFTN + nIFTN];
        for (int i = 0; i < nDFTN; i++) {
            allFileTypeOptions[i] = dataFileTypeNames[i] + " - " + dataFileTypeDescriptions[i];
            allFileTypeNames[i] = dataFileTypeNames[i];
        }
        for (int i = 0; i < nIFTN; i++) {
            allFileTypeOptions[nDFTN + i] = imageFileTypeNames[i] + " - " + imageFileTypeDescriptions[i];
            allFileTypeNames[nDFTN + i] = imageFileTypeNames[i];
        }
    }

    //ensure org.jdom.Content is compiled -- 
    //GeotiffWriter needs it, but it isn't called directly so
    //it isn't automatically compiled.
    private static org.jdom.Content orgJdomContent;

    //*********** end of static declarations ***************************

    /** The constructor should set these to indicate where the 
     * lon,lat,alt,time variables are in axisVariables 
     * (or leave as -1 if not present).
     */
    protected int lonIndex = -1, latIndex = -1, altIndex = -1, timeIndex = -1;

    /** These are created as needed (in the constructor) from axisVariables. */
    protected String[] axisVariableSourceNames, axisVariableDestinationNames;
    protected String allDimString = null;

    /**
     * This makes the searchString that searchRank searches.
     *
     * @return the searchString that searchRank searches.
     */
    public byte[] searchString() {
        if (searchString != null) 
            return searchString;

        //make a string to search through;   
        StringBuilder sb = startOfSearchString();

        //add axisVariable info
        for (int av = 0; av < axisVariables.length; av++) {
            sb.append(axisVariables[av].destinationName() + "\n");
            if (!axisVariables[av].sourceName().equalsIgnoreCase(axisVariables[av].destinationName()))
                sb.append(axisVariables[av].sourceName() + "\n");
            if (!axisVariables[av].longName().equalsIgnoreCase(axisVariables[av].destinationName()))
                sb.append(axisVariables[av].longName() + "\n");
        }
        for (int av = 0; av < axisVariables.length; av++) sb.append(axisVariables[av].combinedAttributes().toString() + "\n");

        String2.replaceAll(sb, "\"", ""); //no double quotes (esp around attribute values)
        String2.replaceAll(sb, "\n    ", "\n"); //occurs for all attributes
        String tSearchString = sb.toString().toLowerCase();
        searchString = String2.getUTF8Bytes(tSearchString);
        return searchString;
    }

    /** 
     * A string like [time][latitude][longitude] with the axis destination names.
     */
    public String allDimString() {
        if (allDimString == null) {
            StringBuilder sb = new StringBuilder();
            for (int av = 0; av < axisVariables.length; av++) 
                sb.append("[" + axisVariables[av].destinationName() + "]");
            allDimString = sb.toString(); //last thing: atomic assignment
        }
        return allDimString;
    }

    /**
     * This returns the types of data files that this dataset can be returned as.
     * These are short descriptive names that are put in the 
     * request url after the dataset name and before the "?", e.g., ".nc". 
     *
     * @return the types of data files that this dataset can be returned as.
     */
    public String[] dataFileTypeNames() {return dataFileTypeNames; }

    /**
     * This returns the file extensions corresponding to the dataFileTypes.
     * E.g., dataFileTypeName=".htmlTable" returns dataFileTypeExtension=".html".
     *
     * @return the file extensions corresponding to the dataFileTypes.
     */
    public String[] dataFileTypeExtensions() {return dataFileTypeExtensions; }

    /**
     * This returns descriptions (up to 80 characters long, suitable for a tooltip)
     * corresponding to the dataFileTypes. 
     *
     * @return descriptions corresponding to the dataFileTypes.
     */
    public String[] dataFileTypeDescriptions() {return dataFileTypeDescriptions; }

    /**
     * This returns an info URL corresponding to the dataFileTypes. 
     *
     * @return an info URL corresponding to the dataFileTypes (an element is "" if not available).
     */
    public String[] dataFileTypeInfo() {return dataFileTypeInfo; }

    /**
     * This returns the types of image files that this dataset can be returned 
     * as. These are short descriptive names that are put in the 
     * request url after the dataset name and before the "?", e.g., ".largePng". 
     *
     * @return the types of image files that this dataset can be returned as.
     */
    public String[] imageFileTypeNames() {return imageFileTypeNames; }

    /**
     * This returns the file extensions corresponding to the imageFileTypes,
     * e.g., imageFileTypeNames=".largePng" returns imageFileTypeExtensions=".png".
     *
     * @return the file extensions corresponding to the imageFileTypes.
     */
    public String[] imageFileTypeExtensions() {return imageFileTypeExtensions; }

    /**
     * This returns descriptions corresponding to the imageFileTypes 
     * (each is suitable for a tooltip).
     *
     * @return descriptions corresponding to the imageFileTypes.
     */
    public String[] imageFileTypeDescriptions() {return imageFileTypeDescriptions; }

    /**
     * This returns an info URL corresponding to the imageFileTypes. 
     *
     * @return an info URL corresponding to the imageFileTypes.
     */
    public String[] imageFileTypeInfo() {return imageFileTypeInfo; }
    
    /**
     * This returns the "[name] - [description]" for all dataFileTypes and imageFileTypes.
     *
     * @return the "[name] - [description]" for all dataFileTypes and imageFileTypes.
     */
    public String[] allFileTypeOptions() {return allFileTypeOptions; }

    /**
     * This indicates why the dataset isn't accessible via Make A Graph
     * (or "" if it is).
     */
    public String accessibleViaMAG() {
        if (accessibleViaMAG == null) {
            //find the axisVariables (all are always numeric) with >1 value
            boolean hasAG1V = false;
            StringArray sa = new StringArray();
            for (int av = 0; av < axisVariables.length; av++) {
                if (axisVariables[av].sourceValues().size() > 1) {
                    hasAG1V = true;
                    break;
                }
            }
            if (!hasAG1V) {
                accessibleViaMAG = 
                    "Make A Graph isn't available for this dataset because " +
                    "this dataset doesn't have any axis variables with more than one value.";
            } else {

                //find the numeric dataVariables 
                boolean hasNumeric = false;
                for (int dv = 0; dv < dataVariables.length; dv++) {
                    if (dataVariables[dv].destinationDataTypeClass() != String.class) {
                        hasNumeric = true;
                        break;
                    }
                }
                if (hasNumeric)
                    accessibleViaMAG = "";
                else 
                    accessibleViaMAG = 
                        "Make A Graph isn't available for this dataset because " +
                        "this dataset doesn't have any non-String data variables.";
            }
        }
        return accessibleViaMAG;
    }

    /**
     * This indicates why the dataset isn't accessible via .subset
     * (or "" if it is).
     */
    public String accessibleViaSubset() {
        if (accessibleViaSubset == null) 
            accessibleViaSubset = "Grid datasets aren't suitable for .subset.";
        return accessibleViaSubset;
    }

    /** 
     * This indicates why the dataset isn't accessible via SOS
     * (or "" if it is).
     */
    public String accessibleViaSOS() {
        if (accessibleViaSOS == null)
            accessibleViaSOS = "datasetID=" + datasetID + 
                " isn't available via SOS because it's a gridded dataset.";
        return accessibleViaSOS;
    }
     
    /** 
     * This indicates why the dataset isn't accessible via WCS
     * (or "" if it is).
     * There used to be a lon +/-180 restriction, but no more.
     */
    public String accessibleViaWCS() {
        if (accessibleViaWCS == null) {
            String start = "datasetID=" + datasetID + " isn't available via WCS because ";

            //must have lat and lon axes
            if (lonIndex < 0 || latIndex < 0)
                accessibleViaWCS = start + "there is no longitude and/or latitude axis variable.";
            else {
                //must have more than one value for lat and lon axes
                EDVGridAxis lonVar = axisVariables[lonIndex];
                EDVGridAxis latVar = axisVariables[latIndex];
                if (lonVar.destinationMin() == lonVar.destinationMax() || //only 1 value
                    latVar.destinationMin() == latVar.destinationMax())
                    accessibleViaWCS = start + "the longitude and/or latitude variable has only one value.";
                else if (lonVar.destinationMin() >= 360 ||  //unlikely
                         lonVar.destinationMax() <= -180)   //unlikely
                    accessibleViaWCS = start + "there are no longitude values within -180 to 360.";

                //else if (!lonVar.isEvenlySpaced() ||  //not necessary. map is drawn as appropriate.
                //    !latVar.isEvenlySpaced())
                //    accessibleViaWCS = start + "";

                //else {  //NO. other axes are allowed.
                //    //is there an axis (with size > 0) that isn't one of LLAT?
                //    for (int av = 0; av < axisVariables.length; av++) {
                //        if (av == lonIndex || av == latIndex ||
                //            av == altIndex || av == timeIndex) {
                //        } else if (axisVariables[av].sourceValues().size() > 1) {
                //            accessibleViaWCS = start + "";
                //        }
                //    }

                //else for (int dv = 0; dv < dataVariables.length; dv++)
                //    if (dataVariables[dv].hasColorBarMinMax())
                //        accessibleViaWCS = "";

                else accessibleViaWCS = "";
            }
        }
        return accessibleViaWCS;
    }

    /** 
     * This indicates why the dataset is accessible via WMS
     * (or "" if it is).
     * There used to be a lon +/-180 restriction, but no more.
     */
    public String accessibleViaWMS() {
        if (accessibleViaWMS == null) {
            String start = "datasetID=" + datasetID + " isn't available via WMS because ";
            if (lonIndex < 0 || latIndex < 0)
                accessibleViaWMS = start + "there is no longitude and/or latitude variable.";
            else {
                EDVGridAxis lonVar = axisVariables[lonIndex];
                EDVGridAxis latVar = axisVariables[latIndex];
                if (lonVar.destinationMin() == lonVar.destinationMax() || //only 1 value
                    latVar.destinationMin() == latVar.destinationMax())
                    accessibleViaWMS = start + "the longitude and/or latitude variable has only one value.";
                else if (lonVar.destinationMin() >= 360 ||  //unlikely
                         lonVar.destinationMax() <= -180)   //unlikely
                    accessibleViaWMS = start + "there are no longitude values within -180 to 360.";
                //else if (!lonVar.isEvenlySpaced() ||  //not necessary. map is drawn as appropriate.
                //    !latVar.isEvenlySpaced())
                //   accessibleViaWMS = start + "";

                else {
                    accessibleViaWMS = start + "none of variables has colorBar metadata.";
                    for (int dv = 0; dv < dataVariables.length; dv++)
                        if (dataVariables[dv].hasColorBarMinMax())
                            accessibleViaWMS = ""; //set back to OK
                }
            }
        }
        return accessibleViaWMS;
    }

    /** 
     * This indicates why the dataset isn't accessible via .ncCF file type
     * (or "" if it is).
     */
    public String accessibleViaNcCF() {

        if (accessibleViaNcCF == null) {
            return accessibleViaNcCF = 
                "Currently, the .ncCF file type is Point Observation data only. " +
                "For this dataset, use the .nc file type.";
            //String cdmType = combinedAttributes.getString("cdm_data_type");
            //if (cdmType.equals(CDM_GRID))
            //    accessibleViaNcCDM = "";
            //else accessibleViaNcCDM = "Currently, only cdm_data_type=" + CDM_GRID + 
            //    " is supported for .ncDCM.  This dataset cdm_data_type=" + cdmType + ".";
        }
        return accessibleViaNcCF;
    }

    /**
     * This returns the dapProtocol
     *
     * @return the dapProtocol
     */
    public String dapProtocol() {return dapProtocol; }

    /**
     * This returns the dapDescription 
     *
     * @return the dapDescription
     */
    public String dapDescription() {return EDStatic.EDDGridDapDescription; }

    /**
     * This returns the longDapDescription 
     *
     * @return the longDapDescription
     */
    public static String longDapDescription(String tErddapUrl) {
        return String2.replaceAll(EDStatic.EDDGridDapLongDescription, "&erddapUrl;", tErddapUrl); 
    }

    /**
     * This should be used by all subclass constructors to ensure that 
     * all of the items common to all EDDGrids are properly set.
     * This also does some actual work.
     *
     * @throws Throwable if any required item isn't properly set
     */
    public void ensureValid() throws Throwable {
        super.ensureValid();
        String errorInMethod = "datasets.xml/EDDGrid.ensureValid error for " + datasetID + ":\n ";

        for (int v = 0; v < axisVariables.length; v++) {
            Test.ensureTrue(axisVariables[v] != null, 
                errorInMethod + "axisVariable[" + v + "] is null.");
            String tErrorInMethod = errorInMethod + 
                "for axisVariable #" + v + "=" + axisVariables[v].destinationName() + ":\n";
            Test.ensureTrue(axisVariables[v] instanceof EDVGridAxis, 
                tErrorInMethod + "axisVariable[" + v + "] isn't an EDVGridAxis.");
            axisVariables[v].ensureValid(tErrorInMethod);
        }
        Test.ensureTrue(lonIndex < 0 || axisVariables[lonIndex] instanceof EDVLonGridAxis, 
            errorInMethod + "axisVariable[lonIndex=" + lonIndex + "] isn't an EDVLonGridAxis.");
        Test.ensureTrue(latIndex < 0 || axisVariables[latIndex] instanceof EDVLatGridAxis, 
            errorInMethod + "axisVariable[latIndex=" + latIndex + "] isn't an EDVLatGridAxis.");
        Test.ensureTrue(altIndex < 0 || axisVariables[altIndex] instanceof EDVAltGridAxis, 
            errorInMethod + "axisVariable[altIndex=" + altIndex + "] isn't an EDVAltGridAxis.");
        Test.ensureTrue(timeIndex < 0 || axisVariables[timeIndex] instanceof EDVTimeGridAxis, 
            errorInMethod + "axisVariable[timeIndex=" + timeIndex + "] isn't an EDVTimeGridAxis.");

        //add standard metadata to combinedGlobalAttributes
        //(This should always be done, so shouldn't be in an optional method...)
        String avDestNames[] = axisVariableDestinationNames();

        //lon
        combinedGlobalAttributes.remove("geospatial_lon_min");
        combinedGlobalAttributes.remove("geospatial_lon_max");
        combinedGlobalAttributes.remove("geospatial_lon_resolution");
        combinedGlobalAttributes.remove("geospatial_lon_units");
        combinedGlobalAttributes.remove("Westernmost_Easting");
        combinedGlobalAttributes.remove("Easternmost_Easting");
        int av = String2.indexOf(avDestNames, EDV.LON_NAME);
        if (av >= 0) {
            combinedGlobalAttributes.add("geospatial_lon_units", EDV.LON_UNITS);
            EDVGridAxis edvga = axisVariables[av];
            if (edvga.sourceValues().size() > 1 && edvga.isEvenlySpaced()) 
                 combinedGlobalAttributes.add("geospatial_lon_resolution", Math.abs(edvga.averageSpacing()));

            PrimitiveArray pa = edvga.combinedAttributes().get("actual_range");
            if (pa != null) { //it should be; but it can be low,high or high,low, so
                double ttMin = Math.min(pa.getNiceDouble(0), pa.getNiceDouble(1));
                double ttMax = Math.max(pa.getNiceDouble(0), pa.getNiceDouble(1));
                combinedGlobalAttributes.add("geospatial_lon_min", ttMin);
                combinedGlobalAttributes.add("geospatial_lon_max", ttMax);
                combinedGlobalAttributes.add("Westernmost_Easting", ttMin);
                combinedGlobalAttributes.add("Easternmost_Easting", ttMax);
            }
        }

        //lat
        combinedGlobalAttributes.remove("geospatial_lat_min");
        combinedGlobalAttributes.remove("geospatial_lat_max");
        combinedGlobalAttributes.remove("geospatial_lat_resolution");
        combinedGlobalAttributes.remove("geospatial_lat_units");
        combinedGlobalAttributes.remove("Southernmost_Northing");
        combinedGlobalAttributes.remove("Northernmost_Northing");
        av = String2.indexOf(avDestNames, EDV.LAT_NAME); 
        if (av >= 0) {
            combinedGlobalAttributes.add("geospatial_lat_units", EDV.LAT_UNITS);
            EDVGridAxis edvga = axisVariables[av];
            if (edvga.sourceValues().size() > 1 && edvga.isEvenlySpaced()) 
                combinedGlobalAttributes.add("geospatial_lat_resolution", Math.abs(edvga.averageSpacing()));

            PrimitiveArray pa = edvga.combinedAttributes().get("actual_range");
            if (pa != null) { //it should be; but it can be low,high or high,low, so
                double ttMin = Math.min(pa.getNiceDouble(0), pa.getNiceDouble(1));
                double ttMax = Math.max(pa.getNiceDouble(0), pa.getNiceDouble(1));
                combinedGlobalAttributes.add("geospatial_lat_min", ttMin);
                combinedGlobalAttributes.add("geospatial_lat_max", ttMax);
                combinedGlobalAttributes.add("Southernmost_Northing", ttMin);
                combinedGlobalAttributes.add("Northernmost_Northing", ttMax);
            }
        }

        //alt
        combinedGlobalAttributes.remove("geospatial_vertical_min");
        combinedGlobalAttributes.remove("geospatial_vertical_max");
        combinedGlobalAttributes.remove("geospatial_vertical_positive");
        combinedGlobalAttributes.remove("geospatial_vertical_resolution");
        combinedGlobalAttributes.remove("geospatial_vertical_units");
        av = String2.indexOf(avDestNames, EDV.ALT_NAME);
        if (av >= 0) {
            combinedGlobalAttributes.add("geospatial_vertical_positive", "up");
            combinedGlobalAttributes.add("geospatial_vertical_units", EDV.ALT_UNITS);
            EDVGridAxis edvga = axisVariables[av];
            if (edvga.sourceValues().size() > 1 && edvga.isEvenlySpaced()) 
                combinedGlobalAttributes.add("geospatial_vertical_resolution", Math.abs(edvga.averageSpacing()));

            PrimitiveArray pa = edvga.combinedAttributes().get("actual_range");
            if (pa != null) {  //it should be; but it can be low,high or high,low, so
                double ttMin = Math.min(pa.getNiceDouble(0), pa.getNiceDouble(1));
                double ttMax = Math.max(pa.getNiceDouble(0), pa.getNiceDouble(1));
                combinedGlobalAttributes.add("geospatial_vertical_min", ttMin);
                combinedGlobalAttributes.add("geospatial_vertical_max", ttMax);
            }
        }

        //time
        combinedGlobalAttributes.remove("time_coverage_start");
        combinedGlobalAttributes.remove("time_coverage_end");
        av = String2.indexOf(avDestNames, EDV.TIME_NAME);
        if (av >= 0) {
            PrimitiveArray pa = axisVariables[av].combinedAttributes().get("actual_range");
            if (pa != null) { //it should be; but it can be low,high or high,low, so
                double ttMin = Math.min(pa.getDouble(0), pa.getDouble(1));
                double ttMax = Math.max(pa.getDouble(0), pa.getDouble(1));
                if (!Double.isNaN(ttMin))  //it should be
                    combinedGlobalAttributes.set("time_coverage_start",
                        Calendar2.epochSecondsToIsoStringT(ttMin) + "Z");
                //for tables (not grids) will be NaN for 'present'.   Deal with this better???
                if (!Double.isNaN(ttMax))  //it should be 
                    combinedGlobalAttributes.set("time_coverage_end",   
                        Calendar2.epochSecondsToIsoStringT(ttMax) + "Z");
            }
        }

        //last: make searchString
        //This makes creation of searchString thread-safe (always done in constructor's thread).
        searchString();

    }

    /**
     * The string representation of this gridDataSet (for diagnostic purposes).
     *
     * @return the string representation of this gridDataSet.
     */
    public String toString() {  
        //make this JSON format?
        StringBuilder sb = new StringBuilder();
        sb.append("//** EDDGrid " + super.toString());
        for (int v = 0; v < axisVariables.length; v++)
            sb.append(axisVariables[v].toString());            
        sb.append(
            "\\**\n\n");
        return sb.toString();
    }

    
    /** 
     * This returns the axis or data variable which has the specified destination name.
     *
     * @return the specified axis or data variable destinationName
     * @throws Throwable if not found
     */
    public EDV findVariableByDestinationName(String tDestinationName) 
        throws Throwable {
        for (int v = 0; v < axisVariables.length; v++)
            if (axisVariables[v].destinationName().equals(tDestinationName))
                return (EDV)axisVariables[v];
        return (EDV)findDataVariableByDestinationName(tDestinationName);
    }

    /**
     * This returns the index of the lon axisVariable (or -1 if none).
     * @return the index of the lon axisVariable (or -1 if none).
     */
    public int lonIndex() {return lonIndex;}

    /**
     * This returns the index of the lat axisVariable (or -1 if none).
     * @return the index of the lat axisVariable (or -1 if none).
     */
    public int latIndex() {return latIndex;}

    /**
     * This returns the index of the altitude axisVariable (or -1 if none).
     * @return the index of the altitude axisVariable (or -1 if none).
     */
    public int altIndex() {return altIndex;}

    /**
     * This returns the index of the time axisVariable (or -1 if none).
     * @return the index of the time axisVariable (or -1 if none).
     */
    public int timeIndex() {return timeIndex;}

    
    /**
     * This returns the axisVariables.
     * This is the internal data structure, so don't change it.
     *
     * @return the axisVariables.
     */
    public EDVGridAxis[] axisVariables() {return axisVariables; }

    /** 
     * This returns the axis variable which has the specified source name.
     *
     * @return the specified axis variable sourceName
     * @throws Throwable if not found
     */
    public EDVGridAxis findAxisVariableBySourceName(String tSourceName) 
        throws Throwable {

        int which = String2.indexOf(axisVariableSourceNames(), tSourceName);
        if (which < 0) throw new SimpleException(
            "Error: source variable name='" + tSourceName + "' wasn't found.");
        return axisVariables[which];
    }

    /** 
     * This returns the axis variable which has the specified destination name.
     *
     * @return the specified axis variable destinationName
     * @throws Throwable if not found
     */
    public EDVGridAxis findAxisVariableByDestinationName(String tDestinationName) 
        throws Throwable {

        int which = String2.indexOf(axisVariableDestinationNames(), tDestinationName);
        if (which < 0) throw new SimpleException(
            "Error: destination variable name='" + tDestinationName + "' wasn't found.");
        return axisVariables[which];
    }

    /**
     * This returns a list of the axisVariables' source names.
     *
     * @return a list of the axisVariables' source names.
     *    This always returns the same internal array, so don't change it!
     */
    public String[] axisVariableSourceNames() {
        if (axisVariableSourceNames == null) {
            //do it this way to be a little more thread safe
            String tNames[] = new String[axisVariables.length];
            for (int i = 0; i < axisVariables.length; i++)
                tNames[i] = axisVariables[i].sourceName();
            axisVariableSourceNames = tNames;
        }
        return axisVariableSourceNames;
    }

    /**
     * This returns a list of the axisVariables' destination names.
     *
     * @return a list of the axisVariables' destination names.
     *    This always returns the same internal array, so don't change it!
     */
    public String[] axisVariableDestinationNames() {
        if (axisVariableDestinationNames == null) {
            //do it this way to be a little more thread safe
            String tNames[] = new String[axisVariables.length];
            for (int i = 0; i < axisVariables.length; i++)
                tNames[i] = axisVariables[i].destinationName();
            axisVariableDestinationNames = tNames;
        }
        return axisVariableDestinationNames;
    }

    /**
     * This indicates if userDapQuery is a request for one or more axis variables
     * (vs. a request for one or more data variables).
     * 
     * @param userDapQuery the part after the '?', still percentEncoded (may be null).
     */
    public boolean isAxisDapQuery(String userDapQuery) throws Throwable {
        if (userDapQuery == null) return false;

        //remove any &constraints; 
        String ampParts[] = getUserQueryParts(userDapQuery); //always at least 1 part (may be "")
        userDapQuery = ampParts[0];
        int qLength = userDapQuery.length();
        if (qLength == 0) return false;

        //basically, see if first thing is an axis destination name     
        int tPo = userDapQuery.indexOf('[');
        int po = tPo >= 0? tPo : qLength;
        tPo = userDapQuery.indexOf(',');
        if (tPo >= 0) po = Math.min(tPo, po);

        //or request uses gridName.axisName notation?
        String tName = userDapQuery.substring(0, po);
        int period = tName.indexOf('.');
        if (period > 0 && 
            String2.indexOf(dataVariableDestinationNames(), tName.substring(0, period)) >= 0)
            tName = tName.substring(period + 1);

        //is tName an axisName?
        int tAxis = String2.indexOf(axisVariableDestinationNames(), tName);
        return tAxis >= 0;
    }           

    /** 
     * This parses an OPeNDAP DAP-style grid-style query for grid data (not axis) variables, 
     *   e.g., var1,var2 or
     *   var1[start],var2[start] or
     *   var1[start:stop],var2[start:stop] or
     *   var1[start:stride:stop][start:stride:stop][].
     * <ul>
     * <li>An ERDDAP extension of the OPeNDAP standard: If within parentheses, 
     *   start and/or stop are assumed to be specified in destination units (not indices).
     * <li>If only two values are specified for a dimension (e.g., [a:b]),
     *   it is interpreted as [a:1:b].
     * <li>If only one value is specified for a dimension (e.g., [a]),
     *   it is interpreted as [a:1:a].
     * <li>If 0 values are specified for a dimension (e.g., []),
     *     it is interpreted as [0:1:max].
     * <li> Currently, if more than one variable is requested, all variables must
     *     have the same [] constraints.
     * <li> If userDapQuery is "", it is treated as a request for the entire dataset.
     * <li> The query may also have &amp; clauses at the end.
     *   Currently, they must all start with "." (for graphics commands).
     * </ul>
     *
     * @param userDapQuery the part of the user's request after the '?', still percentEncoded (shouldn't be null).
     * @param destinationNames will receive the list of requested destination variable names
     * @param constraints will receive the list of constraints,
     *    stored in axisVariables.length groups of 3 int's: 
     *    start0, stride0, stop0, start1, stride1, stop1, ...
     * @param repair if true, this method tries to do its best repair problems (guess at intent), 
     *     not to throw exceptions 
     * @throws Throwable if invalid query
     *     (0 resultsVariables is a valid query)
     */
    public void parseDataDapQuery(String userDapQuery, StringArray destinationNames,
        IntArray constraints, boolean repair) throws Throwable {

        destinationNames.clear();
        constraints.clear();
        if (reallyVerbose) String2.log("    EDDGrid.parseDataDapQuery: " + userDapQuery);

        //split userDapQuery at '&' and decode
        String ampParts[] = getUserQueryParts(userDapQuery); //always at least 1 part (may be "")

        //ignore any &.cmd constraints
        for (int ap = 1; ap < ampParts.length; ap++) {
            if (!repair && !ampParts[ap].startsWith("."))
                throw new SimpleException("Query error: " +
                    "In a griddap query, '&' must be followed by a .graphicsCommand."); 
        }
        String query = ampParts[0]; //it has been percentDecoded

        //expand query="" into request for everything
        if (query.length() == 0) {
            if (reallyVerbose) String2.log("      query=\"\" is expanded to request entire dataset.");
            query = String2.toSVString(dataVariableDestinationNames(), ",", false);
        }

        //process queries with no [], just csv list of desired dataVariables
        if (query.indexOf('[') < 0) {
            for (int av = 0; av < axisVariables.length; av++) {
                constraints.add(0);
                constraints.add(1);
                constraints.add(axisVariables[av].sourceValues().size() - 1);
            }
            String destNames[] = String2.split(query, ',');
            for (int dv = 0; dv < destNames.length; dv++) {
                //if gridName.gridName notation, remove "gridName."
                //This isn't exactly correct: technically, the response shouldn't include the axis variables.
                String destName = destNames[dv];
                int period = destName.indexOf('.');
                if (period > 0) {
                    String shortName = destName.substring(0, period);
                    if (destName.equals(shortName + "." + shortName) &&
                        String2.indexOf(dataVariableDestinationNames(), shortName) >= 0)
                        destName = shortName;                        
                }

                //ensure destName is valid
                int tdi = String2.indexOf(dataVariableDestinationNames(), destName);
                if (tdi < 0) {
                    if (repair) destName = dataVariableDestinationNames()[0];
                    else {
                        if (String2.indexOf(axisVariableDestinationNames(), destName) >= 0)
                            throw new SimpleException("Query error: " + 
                                "A griddap data variable query can't include an axis variable (" + 
                                destName + ").");
                        findDataVariableByDestinationName(destName); //throws Throwable if trouble                
                    }
                }

                //ensure not duplicate destName
                tdi = destinationNames.indexOf(destName);
                if (tdi >= 0) {
                    if (!repair) 
                        throw new SimpleException("Query error: Variable name='" + destName + 
                            "' occurs twice.");
                } else {
                    destinationNames.add(destName);
                }
            }
            return;
        }


        //get the destinationNames
        int po = 0;
        while (po < query.length()) {
            //after first destinationName+constraints, "," should be next char
            if (po > 0) {
                if (query.charAt(po) != ',') {
                    if (repair) return; //this can only be trouble for second variable
                    else throw new SimpleException("Query error: ',' expected at position=" + po + ".");
                }
                po++;
            }

            //get the destinationName
            //find the '['               ??? Must I require "[" ???
            int leftPo = query.indexOf('[', po);
            if (leftPo < 0) {
                if (repair) return; //this can only be trouble for second variable
                else throw new SimpleException("Query error: '[' not found after position=" + po + ".");
            }
            String destinationName = query.substring(po, leftPo);

            //if gridName.gridName notation, remove "gridName."
            //This isn't exactly correct: technically, the response shouldn't include the axis variables.
            int period = destinationName.indexOf('.');
            if (period > 0) {
                String shortName = destinationName.substring(0, period); 
                if (destinationName.equals(shortName + "." + shortName) &&
                    String2.indexOf(dataVariableDestinationNames(), shortName) >= 0)
                    destinationName = shortName;
            }
            
            //ensure destinationName is valid
            if (reallyVerbose) String2.log("      destinationName=" + destinationName);
            int tdi = String2.indexOf(dataVariableDestinationNames(), destinationName);
            if (tdi < 0) {
                if (repair) destinationName = dataVariableDestinationNames()[0];
                else findDataVariableByDestinationName(destinationName); //throws Throwable if trouble
            }

            //ensure not duplicate destName
            tdi = destinationNames.indexOf(destinationName);
            if (tdi >= 0) {
                if (!repair) 
                    throw new SimpleException("Query error: variable name='" + destinationName + 
                        "' occurs twice.");
            } else {
                destinationNames.add(destinationName);
            }
            po = leftPo;

            //get the axis constraints
            for (int axis = 0; axis < axisVariables.length; axis++) {
                int sssp[] = parseAxisBrackets(query, destinationName, po, axis, repair);
                int startI  = sssp[0];
                int strideI = sssp[1];
                int stopI   = sssp[2];
                po          = sssp[3];

                if (destinationNames.size() == 1) {
                    //store convert sourceStart and sourceStop to indices
                    constraints.add(startI);
                    constraints.add(strideI);
                    constraints.add(stopI);
                    //if (reallyVerbose) String2.log("      axis=" + axis + 
                    //    " constraints: " + startI + " : " + strideI + " : " + stopI);
                } else {
                    //ensure start,stride,stop match first variable
                    if (startI  != constraints.get(axis * 3 + 0) ||
                        strideI != constraints.get(axis * 3 + 1) ||
                        stopI   != constraints.get(axis * 3 + 2)) {
                        if (!repair) throw new SimpleException("Query error: constraint(" +
                            startI + ":" + strideI + ":" + stopI + 
                            ") for variable=" + destinationName + " axis=" + axis + 
                            " is not identical to first variable's constraint(" +
                            constraints.get(axis * 3 + 0) + ":" + 
                            constraints.get(axis * 3 + 1) + ":" + 
                            constraints.get(axis * 3 + 2) + 
                            ").  If you need different subsets, make separate requests.");
                    }
                }
            }
        }        
    }

    /** 
     * This parses an OPeNDAP DAP-style grid-style query for one or more axis (not grid) variables, 
     *  (e.g., var1,var2, perhaps with [start:stride:stop] values).
     * !!!This should only be called if the userDapQuery starts with the name of 
     * an axis variable.
     * <ul>
     * <li>An ERDDAP extension of the OPeNDAP standard: If within parentheses, 
     *   start and/or stop are assumed to be specified in destination units (not indices).
     * <li>If only two values are specified for a dimension (e.g., [a:b]),
     *   it is interpreted as [a:1:b].
     * <li>If only one value is specified for a dimension (e.g., [a]),
     *   it is interpreted as [a:1:a].
     * <li>If 0 values are specified for a dimension (e.g., []),
     *     it is interpreted as [0:1:max].
     * <li> Currently, if more than one variable is requested, all variables must
     *     have the same [] constraints.
     * <li> If userDapQuery is varName, it is treated as a request for the entire variable.
     * </ul>
     *
     * @param userDapQuery the part of the user's request after the '?', still percentEncoded (shouldn't be null).
     * @param destinationNames will receive the list of requested destination axisVariable names
     * @param constraints will receive the list of constraints,
     *    stored as 3 int's (for for each destinationName): start, stride, stop.
     * @param repair if true, this method tries to do its best repair problems (guess at intent), 
     *     not to throw exceptions 
     * @throws Throwable if invalid query (and if !repair)     
     */
    public void parseAxisDapQuery(String userDapQuery, StringArray destinationNames,
        IntArray constraints, boolean repair) throws Throwable {

        destinationNames.clear();
        constraints.clear();
        if (reallyVerbose) String2.log("    EDDGrid.parseAxisDapQuery: " + userDapQuery);

        //split userDapQuery at '&' and decode
        String ampParts[] = getUserQueryParts(userDapQuery);  //always at least 1 part (may be "")

        //ensure not nothing (which is a data request)
        if (ampParts[0].length() == 0) 
            throw new SimpleException("Query error: " + 
                "Grid axis queries must specify at least one axis variable.");

        //ignore any &.cmd constraints
        for (int ap = 1; ap < ampParts.length; ap++)
            if (!repair && !ampParts[ap].startsWith("."))
                throw new SimpleException("Query error: " + 
                    "In a griddap query, '&' must be followed by a .graphicsCommand.");                
        userDapQuery = ampParts[0];

        //get the destinationNames
        int po = 0;
        int qLength = userDapQuery.length();
        while (po < qLength) {
            int commaPo = userDapQuery.indexOf(',', po);
            if (commaPo < 0)
                commaPo = qLength;
            int leftPo = userDapQuery.indexOf('[', po);
            boolean hasBrackets = leftPo >= 0 && leftPo < commaPo;

            //get the destinationName
            String destinationName = userDapQuery.substring(po, 
                hasBrackets? leftPo : commaPo);
            //if gridName.axisName notation, remove "gridName."
            int period = destinationName.indexOf('.');
            if (period > 0) {
                //ensure gridName is valid
                if (!repair && 
                    String2.indexOf(dataVariableDestinationNames(), destinationName.substring(0, period)) < 0)
                    throw new SimpleException("Query error: Unexpected data variable name=\"" + 
                        destinationName.substring(0, period) + "\".");
                destinationName = destinationName.substring(period + 1); 
            }
            
            //ensure destinationName is valid
            if (reallyVerbose) String2.log("      destinationName=" + destinationName);
            int axis = String2.indexOf(axisVariableDestinationNames(), destinationName);
            if (axis < 0) {
                if (repair) destinationName = axisVariableDestinationNames()[0];
                else {
                    if (String2.indexOf(dataVariableDestinationNames(), destinationName) >= 0)
                        throw new SimpleException("Query error: " + 
                            "A griddap axis variable query can't include a data variable (" + 
                            destinationName + ").");
                    findAxisVariableByDestinationName(destinationName); //throws Throwable if trouble
                }
            }

            //ensure not duplicate destName
            int tdi = destinationNames.indexOf(destinationName);
            if (tdi >= 0) {
                if (repair) return;
                else throw new SimpleException("Query error: variable name='" + 
                    destinationName + "' occurs twice.");
            } else {
                destinationNames.add(destinationName);
            }

            if (hasBrackets) {
                //get the axis constraints
                int sssp[] = parseAxisBrackets(userDapQuery, destinationName, leftPo, axis, repair);
                constraints.add(sssp[0]); //start
                constraints.add(sssp[1]); //stride
                constraints.add(sssp[2]); //stop
                po = sssp[3];
                if (po != commaPo && !repair)
                    throw new SimpleException("Query error: Unexpected character#" + (po + 1) + "='" + 
                        userDapQuery.charAt(po) + "'."); 
                //if (reallyVerbose) String2.log("      axis=" + axis + 
                //    " constraints: " + startI + " : " + strideI + " : " + stopI);
            } else {
                constraints.add(0); //start
                constraints.add(1); //stride
                constraints.add(axisVariables[axis].sourceValues().size() - 1); //stop
            }

            po = commaPo + 1;
        }        
    }

    /** 
     * Given a percentDecoded part of a userDapQuery, leftPo, and axis, this parses the contents of a [ ] 
     * in userDapQuery and returns the startI, strideI, stopI, and rightPo+1.
     *
     * @param deQuery a percentDecoded part of a userDapQuery
     * @param destinationName axis or grid variable name (for diagnostic purposes only)
     * @param leftPo the position of the "["
     * @param axis the axis number, 0..
     * @param repair if true, this tries to do its best not to throw an exception (guess at intent)
     * @return int[4], 0=startI, 1=strideI, 2=stopI, and 3=newPo (rightPo+1)
     * @throws Throwable if trouble
     */
    protected int[] parseAxisBrackets(String deQuery, String destinationName, 
        int leftPo, int axis, boolean repair) throws Throwable {

        EDVGridAxis av = axisVariables[axis];
        int nAvSourceValues = av.sourceValues().size();
        int precision = axis == timeIndex? 9 : 5;
        String diagnostic = "for variable=" + destinationName + 
            ", axis#" + axis + "=" + av.destinationName();
        //if (reallyVerbose) String2.log("parseAxisBrackets " + diagnostic + ", leftPo=" + leftPo);
        int defaults[] = {0, 1, nAvSourceValues - 1, deQuery.length()};

        //leftPo must be '['
        int po = leftPo;
        if (po >= deQuery.length() || deQuery.charAt(leftPo) != '[') {
            if (repair) return defaults;
            else throw new SimpleException("Query error: '[' expected at position=" + 
                leftPo + " (" + diagnostic + ").");
        }

        //find the ']'    
        //It shouldn't occur within paren values, so a simple search is fine.
        int rightPo = deQuery.indexOf(']', leftPo + 1);
        if (rightPo < 0) {
            if (repair) return defaults;
            else throw new SimpleException("Query error: ']' not found after position=" + 
                leftPo + " (" + diagnostic + ").");
        }
        defaults[3] = rightPo;
        diagnostic += ", constraint=" +
            deQuery.substring(leftPo, rightPo + 1);
        po = rightPo + 1; //prepare for next axis constraint
    
        //is there anything between [ and ]?
        int startI, strideI, stopI;
        if (leftPo == rightPo - 1) {
            //[] -> 0:1:max
            startI = 0; //indices
            strideI = 1;
            stopI = nAvSourceValues - 1;
        } else {
            //find colon1
            int colon1 = -1;
            if (deQuery.charAt(leftPo + 1) == '(') {
                //seek closing )
                colon1 = deQuery.indexOf(')', leftPo + 2);
                if (colon1 >= rightPo) {
                    if (repair) return defaults;
                    else throw new SimpleException("Query error: " + 
                        "Close ')' not found after position=" + leftPo + 
                        " (" + diagnostic + ").");
                }
                colon1++;
            } else {
                //non-paren value
                colon1 = deQuery.indexOf(':', leftPo + 1);
            }
            if (colon1 < 0 || colon1 >= rightPo)
                //just one value inside []
                colon1 = -1;

            //find colon2
            int colon2 = -1;
            if (colon1 < 0) {
                //there is no colon1, so there is no colon2
            } else if (deQuery.charAt(colon1 + 1) == '(') {
                //seek closing "
                colon2 = deQuery.indexOf(')', colon1 + 2);
                if (colon2 >= rightPo) {
                    if (repair) return defaults;
                    else throw new SimpleException("Query error: " +
                        "close ')' not found after position=" + (colon2 + 2) + 
                        " (" + diagnostic + ").");
                } else {
                    //next char must be ']' or ':'
                    colon2++;
                    if (colon2 == rightPo) {
                        colon2 = -1;
                    } else if (deQuery.charAt(colon2) == ':') {
                        //colon2 is set correctly
                    } else { 
                        if (repair) return defaults;
                        else throw new SimpleException("Query error: " +
                            "':' expected at position=" + colon2 + 
                            " (" + diagnostic + ").");                    
                    }
                }
            } else {
                //non-paren value
                colon2 = deQuery.indexOf(':', colon1 + 1);
                if (colon2 > rightPo)
                    colon2 = -1;
            }
            //String2.log("      " + diagnostic + " colon1=" + colon1 + " colon2=" + colon2);

            //extract the string values
            String startS, stopS;
            if (colon1 < 0) {
                //[start]
                startS = deQuery.substring(leftPo + 1, rightPo);
                strideI = 1;
                stopS = startS; 
            } else if (colon2 < 0) {
                //[start:stop]
                startS = deQuery.substring(leftPo + 1, colon1);
                strideI = 1;
                stopS = deQuery.substring(colon1 + 1, rightPo);
            } else {
                //[start:stride:stop]
                startS = deQuery.substring(leftPo + 1, colon1);
                String strideS = deQuery.substring(colon1 + 1, colon2);
                strideI = String2.parseInt(strideS);
                stopS = deQuery.substring(colon2 + 1, rightPo);
                if (strideI < 1 || strideI == Integer.MAX_VALUE) {
                    if (repair) strideI = 1;
                    else throw new SimpleException("Query error: " + 
                        "Invalid stride=" + strideS + ".");
                }
            }
            startS = startS.trim();
            stopS = stopS.trim();
            //String2.log("      startS=" + startS + " strideI=" + strideI + " stopS=" + stopS);

            double sourceMin = av.sourceValues().getDouble(0);
            double sourceMax = av.sourceValues().getDouble(nAvSourceValues - 1);
            //if (startS.equals("last") || startS.equals("(last)")) {
            //    startI = av.sourceValues().size() - 1;
            //} else 
            if (startS.startsWith("last") || startS.startsWith("(last")) 
                startS = convertLast(av, "Start", startS);

            if (startS.startsWith("(")) {
                //convert paren startS
                startS = startS.substring(1, startS.length() - 1).trim(); //remove begin and end parens
                if (startS.length() == 0 && !repair) 
                    throw new SimpleException("Query error: " +
                        "A Start value inside \"()\" is missing.");
                double startDestD = av.destinationToDouble(startS);

                //since closest() below makes far out values valid, need to test validity
                if (repair && Double.isNaN(startDestD))
                    startDestD = av.destinationMin();

                if (Math2.greaterThanAE(precision, startDestD, av.destinationCoarseMin())) {
                } else {
                    if (repair) startDestD = av.firstDestinationValue();
                    else throw new SimpleException("Query error: " +
                        EDStatic.THERE_IS_NO_DATA +
                        " (in query " + diagnostic + ", the requestedStart=\"" + startDestD + 
                        "\" is less than the axis minimum=" + av.destinationMin() + 
                        " (and even " + av.destinationCoarseMin() + "))");
                }

                if (Math2.lessThanAE(   precision, startDestD, av.destinationCoarseMax())) {
                } else {
                    if (repair) startDestD = av.lastDestinationValue();
                    else throw new SimpleException(EDStatic.THERE_IS_NO_DATA +
                        " (in query " + diagnostic + ", the requestedStart=\"" + startDestD + 
                        "\" is greater than the axis maximum=" + av.destinationMax() + 
                        " (and even " + av.destinationCoarseMax() + "))");
                }

                startI = av.destinationToClosestSourceIndex(startDestD);
            } else {
                //it must be a >= 0 integer index
                if (!startS.matches("[0-9]+")) {
                    if (repair) startS = "0";
                    else throw new SimpleException("Query error: " + 
                        "Invalid requested axis start=\"" + startS + 
                        "\" isn't an integer >= 0 (" + diagnostic + ").");
                }

                startI = String2.parseInt(startS);
                if (startI < 0 || startI > nAvSourceValues - 1) {
                    if (repair) startI = 0;
                    else throw new SimpleException("Query error: " + 
                        "start=" + startS + " must be between 0 and " + 
                        (nAvSourceValues - 1) + " (" + diagnostic + ").");
                }
            }

            //if (startS.equals("last") || stopS.equals("(last)")) {
            //    stopI = av.sourceValues().size() - 1;
            //} else 
            if (stopS.startsWith("last") || stopS.startsWith("(last")) 
                stopS = convertLast(av, "Stop", stopS);

            if (stopS.startsWith("(")) {
                //convert paren stopS
                stopS = stopS.substring(1, stopS.length() - 1).trim(); //remove begin and end parens
                if (stopS.length() == 0 && !repair)
                    throw new SimpleException("Query error: " +
                        "A Stop value inside \"()\" is missing.");
                double stopDestD = av.destinationToDouble(stopS);

                //since closest() below makes far out values valid, need to test validity
                if (repair && Double.isNaN(stopDestD))
                    stopDestD = av.destinationMax();

                if (Math2.greaterThanAE(precision, stopDestD, av.destinationCoarseMin())) {
                } else {
                    if (repair) stopDestD = av.firstDestinationValue();
                    else throw new SimpleException(EDStatic.THERE_IS_NO_DATA +
                        " (in query " + diagnostic + ", the requestedStop=\"" + stopDestD + 
                        "\" is less than " + av.destinationMin() + 
                        " (and even " + av.destinationCoarseMin() + "))");
                }

                if (Math2.lessThanAE(   precision, stopDestD, av.destinationCoarseMax())) {
                } else {
                    if (repair) stopDestD = av.lastDestinationValue();
                    else throw new SimpleException(EDStatic.THERE_IS_NO_DATA +
                        " (in query " + diagnostic + ", the requestedStop=\"" + stopDestD + 
                        "\" is greater than " + av.destinationMax() + 
                        " (and even " + av.destinationCoarseMax() + "))");
                }

                stopI = av.destinationToClosestSourceIndex(stopDestD);
            } else {
                //it must be a >= 0 integer index
                stopS = stopS.trim();
                if (!stopS.matches("[0-9]+")) {
                    if (repair) stopS = "" + (nAvSourceValues - 1);
                    else throw new SimpleException("Query error: " + 
                        "Invalid requested axis stop=\"" + stopS + 
                        "\" in constraint isn't an integer >= 0 (" + 
                        diagnostic + ").");
                }
                stopI = String2.parseInt(stopS);
                if (stopI < 0 || stopI > nAvSourceValues - 1) {
                    if (repair) stopI = nAvSourceValues - 1;
                    else throw new SimpleException("Query error: " + 
                        "stop=" + stopS + 
                        " in constraint must be between 0 and " + 
                        (nAvSourceValues - 1) + " (" + diagnostic + ").");
                }
            }
        }

        //test for no data
        if (startI > stopI) {
            if (repair) {
                int ti = startI; startI = stopI; stopI = ti;
            } else throw new SimpleException("Query error: " +
                "requestStartIndex=" + startI + " is less than requestStopIndex=" + 
                stopI + " in constraint (" + diagnostic + ").");
        }

        //return
        return new int[]{startI, strideI, stopI, po};
    }

    /**
     * This converts an OPeNDAP Start or Stop value of "last[-n]" or "(last-x)"
     * into "index" or "(value)".
     * Without parentheses, n is an index number.
     * With parentheses, x is a number
     * '+' is allowed instead of '-'.
     * Internal spaces are allowed.
     *
     * @param av an EDVGridAxis variable
     * @param name "Start" or "Stop"
     * @param ssValue the start or stop value
     * @return ssValue converted to "index" or a "(value)"
     * @throws Throwable if invalid format or n is too large
     */
    public static String convertLast(EDVGridAxis av, String name, String ssValue) throws Throwable {
        //remove parens
        String ossValue = ssValue;
        boolean hasParens = ssValue.startsWith("(");
        if (hasParens) {
            if (ssValue.endsWith(")"))
                ssValue = ssValue.substring(1, ssValue.length() - 1).trim();
            else
                throw new SimpleException("Query error: " +
                    name + "Value=" + ossValue + 
                    " starts with '(', but doesn't end with ')'.");
        }

        //remove "last"
        if (ssValue.startsWith("last"))
            ssValue = ssValue.substring(4).trim();
        else 
            throw new SimpleException("Query error: " +
                "'last' was expected at beginning of " + 
                name + "Value=" + ossValue + ".");

        //done?
        int lastIndex = av.sourceValues().size() - 1;
        if (ssValue.length() == 0) 
            return hasParens?  
                "(" + av.lastDestinationValue() + ")" : 
                "" + lastIndex;

        // +/-
        int pm = ssValue.startsWith("-")? -1 : 
                 ssValue.startsWith("+")? 1 : 0;
        if (pm == 0)
            throw new IllegalArgumentException ("Unexpected character after 'last' in " + 
                name + "Value=" + ossValue + ".");
        ssValue = ssValue.substring(1).trim();

        //parse the value
        if (hasParens) {
            double td = String2.parseDouble(ssValue);
            if (!Math2.isFinite(td))
                throw new IllegalArgumentException ("The +/-value in " + name + 
                    "Value=" + ossValue + " isn't valid.");               
            return "(" + (av.lastDestinationValue() + pm * td) + ")";
        } else {
            try {
                int ti = Integer.parseInt(ssValue); //be strict
                return "" + (lastIndex + pm * ti); 
            } catch (Throwable t) {
                throw new IllegalArgumentException ("The +/-index value in " + 
                    name + "Value=" + ossValue + " isn't an integer.");
            }
        }
    }

    /** 
     * This builds an OPeNDAP DAP-style grid-style query, 
     *   e.g., var1[start1:stop1][start2:stride2:stop2].
     * This is close to the opposite of parseDapQuery.
     *
     * @param destinationNames
     * @param constraints will receive the list of constraints,
     *    stored in axisVariables.length groups of 3 int's: 
     *    start0, stride0, stop0, start1, stride1, stop1, ...
     * @return the array part of an OPeNDAP DAP-style grid-style query, 
     *   e.g., [start1:stop1][start2:stride2:stop2].
     * @throws Throwable if invalid query
     *     (0 resultsVariables is a valid query)
     */
    public static String buildDapQuery(StringArray destinationNames, IntArray constraints) {
        String arrayQuery = buildDapArrayQuery(constraints);
        String names[] = destinationNames.toArray();        
        for (int i = 0; i < names.length; i++) 
            names[i] += arrayQuery;
        return String2.toSVString(names, ",", false);
    }

    /** 
     * This returns the array part of an OPeNDAP DAP-style grid-style query, 
     *   e.g., [start1:stop1][start2:stride2:stop2].
     * This is close to the opposite of parseDapQuery.
     *
     * @param constraints will receive the list of constraints,
     *    stored in axisVariables.length groups of 3 int's: 
     *    start0, stride0, stop0, start1, stride1, stop1, ...
     * @return the array part of an OPeNDAP DAP-style grid-style query, 
     *   e.g., [start1:stop1][start2:stride2:stop2].
     * @throws Throwable if invalid query
     *     (0 resultsVariables is a valid query)
     */
    public static String buildDapArrayQuery(IntArray constraints) {
        StringBuilder sb = new StringBuilder();
        int po = 0;
        while (po < constraints.size()) {
            int stride = constraints.get(po + 1);
            sb.append("[" + constraints.get(po) + ":" + 
                (stride == 1? "" : stride + ":") + 
                constraints.get(po +  2) + "]");
            po += 3;
        }
        return sb.toString();
    }

   /** 
     * This gets data (not yet standardized) from the data 
     * source for this EDDGrid.     
     * Because this is called by GridDataAccessor, the request won't be the 
     * full user's request, but will be a partial request (for less than
     * EDStatic.partialRequestMaxBytes).
     * 
     * @param tDataVariables
     * @param tConstraints
     * @return a PrimitiveArray[] where the first axisVariables.length elements
     *   are the axisValues and the next tDataVariables.length elements
     *   are the dataValues.
     *   Both the axisValues and dataValues are straight from the source,
     *   not modified.
     * @throws Throwable if trouble
     */
    public abstract PrimitiveArray[] getSourceData(EDV tDataVariables[], IntArray tConstraints) 
        throws Throwable;

    /**
     * This makes a sibling dataset, based on the new sourceUrl.
     *
     * @param tLocalSourceUrl
     * @param ensureAxisValuesAreEqual If Integer.MAX_VALUE, no axis sourceValue tests are performed. 
     *    If 0, this tests if sourceValues for axis-variable #0+ are same.
     *    If 1, this tests if sourceValues for axis-variable #1+ are same.
     *    (This is useful if the, for example, lat and lon values vary slightly and you 
     *    are willing to accept the initial values as the correct values.)
     *    Actually, the tests are always done but this determines whether
     *    the error is just logged or whether it throws an exception.
     * @param shareInfo if true, this ensures that the sibling's 
     *    axis and data variables are basically the same as this datasets,
     *    and then makes the new dataset point to the this instance's data structures
     *    to save memory. (AxisVariable #0 isn't duplicated.)
     * @return EDDGrid
     * @throws Throwable if trouble
     */
    public abstract EDDGrid sibling(String tLocalSourceUrl, int ensureAxisValuesAreEqual,
        boolean shareInfo) throws Throwable;

    /**
     * This tests if the axisVariables and dataVariables of the other dataset are similar 
     *     (same destination data var names, same sourceDataType, same units, 
     *     same missing values).
     *
     * @param other   
     * @param ensureAxisValuesAreEqual If Integer.MAX_VALUE, no axis sourceValue 
     *    tests are performed. 
     *    If 0, this tests if sourceValues for axis-variable #0+ are same.
     *    If 1, this tests if sourceValues for axis-variable #1+ are same.
     *    (This is useful if the, for example, lat and lon values vary slightly and you 
     *    are willing to accept the initial values as the correct values.)
     *    Actually, the tests are always done but this determines whether
     *    the error is just logged or whether it throws an exception.
     * @param strict if !strict, this is less strict
     * @return "" if similar (same axis and data var names,
     *    same units, same sourceDataType, same missing values) 
     *    or a message if not (including if other is null).
     */
    public String similar(EDDGrid other, int ensureAxisValuesAreEqual, boolean strict) {
        try {
            if (other == null) 
                return "EDDGrid.similar: There is no 'other' dataset.  (Perhaps ERDDAP just restarted.)";
            if (reallyVerbose) String2.log("EDDGrid.similar ensureAxisValuesAreEqual=" + ensureAxisValuesAreEqual);
            String results = super.similar(other);
            if (results.length() > 0) 
                return results;

            return similarAxisVariables(other, ensureAxisValuesAreEqual, strict);
        } catch (Throwable t) {
            return MustBe.throwableToShortString(t);
        }
    }

    /**
     * This tests if 'old' is different from this in any way.
     * <br>This test is from the view of a subscriber who wants to know
     *    when a dataset has changed in any way.
     * <br>So some things like onChange and reloadEveryNMinutes are not checked.
     * <br>This only lists the first change found.
     *
     * <p>EDDGrid overwrites this to also check the axis variables.
     *
     * @param old
     * @return "" if same or message if not.
     */
    public String changed(EDD old) {
        if (old == null)
            return super.changed(old); //so message is consistent

        if (!(old instanceof EDDGrid)) 
            return "The new version is an EDDGrid.  The old version isn't!\n";

        EDDGrid oldG = (EDDGrid)old;

        //check most important things first
        int nAv = axisVariables.length;
        StringBuilder diff = new StringBuilder();
        diff.append(test2Changed("The number of axisVariables changed:",
            "" + oldG.axisVariables().length, 
            "" + nAv));
        if (diff.length() > 0) 
            return diff.toString(); //because tests below assume nAv are same

        for (int av = 0; av < nAv; av++) { 
            EDVGridAxis oldAV = oldG.axisVariables[av];
            EDVGridAxis newAV =      axisVariables[av];             
            String newName = newAV.destinationName();

            diff.append(test2Changed("The destinationName for axisVariable #" + av + " changed:",
                oldAV.destinationName(), newName));

            diff.append(test2Changed(
                "The destinationDataType for axisVariable #" + av + "=" + newName + " changed:",
                oldAV.destinationDataType(), 
                newAV.destinationDataType()));

            //most import case: new time value will be displayed as an iso time
            if (newAV.sourceValues().size() != oldAV.sourceValues().size()) 
                diff.append(
                "The number of axisVariable #" + av + "=" + newName + " values changed from " +
                oldAV.sourceValues().size() + " to " + newAV.sourceValues().size() + ".\n");
            int diffIndex = newAV.sourceValues().diffIndex(oldAV.sourceValues());
            if (diffIndex >= 0)
                diff.append(
                "The destinationValues for axisVariable #" + av + "=" + newName + " changed:" +
                "\n  old index #" + diffIndex + "=" + 
                    (diffIndex >= oldAV.sourceValues().size()? "(no value)" : 
                        oldAV.destinationToString(oldAV.destinationValue(diffIndex).getDouble(0))) +
                ",\n  new index #" + diffIndex + "=" + 
                    (diffIndex >= newAV.sourceValues().size()? "(no value)" : 
                        newAV.destinationToString(newAV.destinationValue(diffIndex).getDouble(0))) +
                ".\n");

            diff.append(test1Changed(
                "A combinedAttribute for axisVariable #" + av + "=" + newName + " changed:",
                String2.differentLine(
                    oldAV.combinedAttributes().toString(), 
                    newAV.combinedAttributes().toString())));
        }

        //check least important things last
        diff.append(super.changed(oldG));
        return diff.toString();
    }

    /**
     * This tests if the axisVariables of the other dataset are similar 
     *     (same destination data var names, same sourceDataType, same units, 
     *     same missing values).
     *
     * @param other 
     * @param ensureAxisValuesAreEqual If Integer.MAX_VALUE, no axis sourceValue 
     *        tests are performed. 
     *    If 0, this tests if sourceValues for axis-variable #0+ are same.
     *    If 1, this tests if sourceValues for axis-variable #1+ are same.
     *    (This is useful if the, for example, lat and lon values vary slightly and you 
     *    are willing to accept the initial values as the correct values.)
     *    Actually, the tests are always done but this determines whether
     *    the error is just logged or whether it throws an exception.
     * @param strict if !strict, this is less strict (including allowing different
     *    sourceDataTypes and destinationDataTypes)
     * @return "" if similar (same axis and data var names,
     *    same units, same sourceDataType, same missing values) or a message if not.
     */
    public String similarAxisVariables(EDDGrid other, int ensureAxisValuesAreEqual, 
            boolean strict) {
        if (reallyVerbose) String2.log("EDDGrid.similarAxisVariables ensureAxisValuesAreEqual=" + 
            ensureAxisValuesAreEqual);
        String msg = "EDDGrid.similar: The other dataset has a different ";
        int nAv = axisVariables.length;
        if (nAv != other.axisVariables.length)
            return msg + "number of axisVariables (" + 
                nAv + " != " + other.axisVariables.length + ")";

        for (int av = 0; av < nAv; av++) {
            EDVGridAxis av1 = axisVariables[av];
            EDVGridAxis av2 = other.axisVariables[av];

            //destinationName
            String s1 = av1.destinationName();
            String s2 = av2.destinationName();
            String msg2 = " for axisVariable #" + av + "=" + s1 + " (";
            if (!s1.equals(s2))
                return msg + "destinationName" + msg2 + s1 + " != " + s2 + ")";

            //sourceDataType 
            //if !strict, don't care e.g., if one is float and the other is double
            if (strict) {    
                s1 = av1.sourceDataType();
                s2 = av2.sourceDataType();
                if (!s1.equals(s2))
                    return msg + "sourceDataType" + msg2 +  s1 + " != " + s2 + ")";
            }

            //destinationDataType
            if (strict) {
                s1 = av1.destinationDataType();
                s2 = av2.destinationDataType();
                if (!s1.equals(s2))
                    return msg + "destinationDataType" + msg2 +  s1 + " != " + s2 + ")";
            }

            //units
            s1 = av1.units();
            s2 = av2.units();
            if (!s1.equals(s2))
                return msg + "units" + msg2 +  s1 + " != " + s2 + ")";

            //sourceMissingValue  (irrelevant, since shouldn't be any mv)
            double d1, d2;
            if (strict) {
                d1 = av1.sourceMissingValue();
                d2 = av2.sourceMissingValue();
                if (!Test.equal(d1, d2)) //says NaN==NaN is true
                    return msg + "sourceMissingValue" + msg2 +  d1 + " != " + d2 + ")";
            }

            //sourceFillValue  (irrelevant, since shouldn't be any mv)
            if (strict) {
                d1 = av1.sourceFillValue();
                d2 = av2.sourceFillValue();
                if (!Test.equal(d1, d2)) //says NaN==NaN is true
                    return msg + "sourceFillValue" + msg2 +  d1 + " != " + d2 + ")";
            }

            //test sourceValues  
            String results = av1.sourceValues().almostEqual(av2.sourceValues());
            if (results.length() > 0) {
                results = msg + "sourceValue" + msg2 + results + ")";
                if (av >= ensureAxisValuesAreEqual) 
                    return results; 
                else String2.log("NOTE: " + results);
            }
        }
        //they are similar
        return "";
    }



    /**
     * This responds to an OPeNDAP-style query.
     *
     * @param request may be null. Currently, it is not used.
     *    (It is passed to respondToGraphQuery, but it doesn't use it.)
     * @param response Currently, not used. It may be null.
     * @param loggedInAs  the name of the logged in user (or null if not logged in).
     *   Normally, this is not used to test if this edd is accessibleTo loggedInAs, 
     *   but it unusual cases (EDDTableFromPost?) it could be.
     *   Normally, this is just used to determine which erddapUrl to use (http vs https).
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     * @param userDapQuery the part of the user's request after the '?', still percentEncoded (shouldn't be null).
     * @param outputStreamSource  the source of an outputStream that receives the results,
     *    usually already buffered.
     *     This doesn't call out.close() at the end. The caller MUST!
     * @param dir the directory (on this computer's hard drive) to use for temporary/cache files
     * @param fileName the name for the 'file' (no dir, no extension),
     *    which is used to write the suggested name for the file to the response 
     *    header.
     * @param fileTypeName the fileTypeName for the new file (e.g., .largePng).
     * @throws Throwable if trouble
     */
    public void respondToDapQuery(HttpServletRequest request, 
        HttpServletResponse response,
        String loggedInAs,
        String requestUrl, String userDapQuery, 
        OutputStreamSource outputStreamSource,
        String dir, String fileName, String fileTypeName) throws Throwable {

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);

        //save data to outputStream
        if (fileTypeName.equals(".asc")) {
            saveAsAsc(requestUrl, userDapQuery, outputStreamSource);
            return;
        }

        if (fileTypeName.equals(".csv")) {
            saveAsCsv(requestUrl, userDapQuery, outputStreamSource, '2');
            return;
        }

        if (fileTypeName.equals(".csvp")) {
            saveAsCsv(requestUrl, userDapQuery, outputStreamSource, '(');
            return;
        }

        if (fileTypeName.equals(".das")) {
            saveAsDAS(requestUrl, userDapQuery, outputStreamSource);
            return;
        }

        if (fileTypeName.equals(".dds")) {
            saveAsDDS(requestUrl, userDapQuery, outputStreamSource);
            return;
        }

        if (fileTypeName.equals(".dods")) {
            saveAsDODS(requestUrl, userDapQuery, outputStreamSource);
            return;
        }

        if (fileTypeName.equals(".esriAscii")) {
            saveAsEsriAscii(requestUrl, userDapQuery, outputStreamSource);
            return;
        }

        if (fileTypeName.equals(".graph")) {
            respondToGraphQuery(request, loggedInAs, requestUrl, userDapQuery, outputStreamSource,
                dir, fileName, fileTypeName);
            return;
        }

        if (fileTypeName.equals(".html")) {
            //it is important that this use outputStreamSource so stream is compressed (if possible)
            //OPeNDAP 2.0 section 3.2.3 says US-ASCII (7bit), so might as well go for compatible unicode
            OutputStream out = outputStreamSource.outputStream("UTF-8");
            Writer writer = new OutputStreamWriter(out, "UTF-8"); 
            writer.write(EDStatic.startHeadHtml(tErddapUrl,  
                title() + //", from " + institution() + 
                " - Data Access Form"));
            writer.write("\n" + rssHeadLink(loggedInAs));
            writer.write("\n</head>\n");
            writer.write(EDStatic.startBodyHtml(loggedInAs));
            writer.write("\n");
            writer.write(HtmlWidgets.htmlTooltipScript(EDStatic.imageDirUrl(loggedInAs))); //this is a link to a script
            writer.write(HtmlWidgets.dragDropScript(EDStatic.imageDirUrl(loggedInAs)));    //this is a link to a script
            writer.flush(); //Steve Souder says: the sooner you can send some html to user, the better
            try {
                writer.write(EDStatic.youAreHereWithHelp(loggedInAs, dapProtocol, "Data Access Form", 
                    EDStatic.EDDGridDataAccessFormHtml + "<p>" + EDStatic.EDDGridDownloadDataHtml +
                    "</ol>\n" +
                    "This web page just simplifies the creation of griddap URLs. " +
                    "<br><b>If you want, you can create these URLs by hand or have a computer program do it." +
                    "<br>Then you don't have to use this form to get data. See the 'Bypass this form' link below.</b>"));
                writeHtmlDatasetInfo(loggedInAs, writer, true, false, true, userDapQuery, "");
                writeDapHtmlForm(loggedInAs, userDapQuery, writer);
                writer.write("<hr>\n");
                writer.write("<h2>The Dataset Attribute Structure (.das) for this Dataset</h2>\n" +
                    "<pre>\n");
                writeDAS(File2.forceExtension(requestUrl, ".das"), "", writer, true); //useful so search engines find all relevant words
                writer.write("</pre>\n");
                writer.write("<br>&nbsp;\n");
                writer.write("<hr>\n");
                writeGeneralDapHtmlInstructions(tErddapUrl, writer, false); 
            } catch (Throwable t) {
                writer.write(EDStatic.htmlForException(t));
            }
            if (EDStatic.displayDiagnosticInfo) 
                EDStatic.writeDiagnosticInfoHtml(writer);
            writer.write(EDStatic.endBodyHtml(tErddapUrl));
            writer.write("\n</html>\n");
            writer.flush(); //essential
            return;
        }

        if (fileTypeName.equals(".htmlTable")) {
            saveAsHtmlTable(loggedInAs, requestUrl, userDapQuery, outputStreamSource, 
                fileName, false, "", ""); 
            return;
        }

        if (fileTypeName.equals(".json")) {
            saveAsJson(requestUrl, userDapQuery, outputStreamSource);
            return;
        }

        if (fileTypeName.equals(".mat")) {
            saveAsMatlab(requestUrl, userDapQuery, outputStreamSource);
            return;
        }

        if (fileTypeName.endsWith("Info") && 
            (fileTypeName.equals(".smallPngInfo") ||
             fileTypeName.equals(".pngInfo") ||
             fileTypeName.equals(".largePngInfo") ||
             fileTypeName.equals(".smallPdfInfo") ||
             fileTypeName.equals(".pdfInfo") ||
             fileTypeName.equals(".largePdfInfo"))) {
            //try to readPngInfo (if fromErddap, this saves it to local file)
            //(if info not available, this will fail)
            String imageFileType = fileTypeName.substring(0, fileTypeName.length() - 4);
            Object[] pngInfo = readPngInfo(loggedInAs, userDapQuery, imageFileType);
            if (pngInfo == null) 
                throw new SimpleException(
                    "File not found in cache. Recreate the image and try again.");

            //ok, copy it  (and don't close the outputStream)
            File2.copy(getPngInfoFileName(loggedInAs, userDapQuery, imageFileType),
                outputStreamSource.outputStream("UTF-8"));
            return;
        }

        if (fileTypeName.equals(".odvTxt")) {
            saveAsODV(requestUrl, userDapQuery, outputStreamSource);
            return;
        }

        if (fileTypeName.equals(".tsv")) {
            saveAsTsv(requestUrl, userDapQuery, outputStreamSource, '2');
            return;
        }

        if (fileTypeName.equals(".tsvp")) {
            saveAsTsv(requestUrl, userDapQuery, outputStreamSource, '(');
            return;
        }

        if (fileTypeName.equals(".xhtml")) {
            saveAsHtmlTable(loggedInAs, requestUrl, userDapQuery, 
                outputStreamSource, fileName, true, "", ""); 
            return;
        }

        //*** make a file (then copy it to outputStream)
        //nc files are handled this way because .ncHeader needs to call
        //  NcHelper.dumpString(aRealFile, false). 
        String fileTypeExtension = fileTypeExtension(fileTypeName);
        String fullName = dir + fileName + fileTypeExtension;

        //does the file already exist?
        //if .ncHeader, make sure the .nc file exists (and it is the better file to cache)
        String cacheFullName = fileTypeName.equals(".ncHeader")?  //the only exception there will ever be
            dir + fileName + ".nc" : fullName;
        int random = Math2.random(Integer.MAX_VALUE);

        //thread-safe creation of the file 
        //(If there are almost simultaneous requests for the same one, only one thread will make it.)
        synchronized(String2.canonical(cacheFullName)) {
            if (File2.isFile(cacheFullName)) { //don't 'touch()'; files for latest data will change
                if (verbose) String2.log("  reusing cached " + cacheFullName);

            } else if (fileTypeName.equals(".nc") || fileTypeName.equals(".ncHeader")) {
                //if .ncHeader, make sure the .nc file exists (and it is the better file to cache)
                saveAsNc(requestUrl, userDapQuery, cacheFullName, true, 0); //it saves to temp random file first
                File2.isFile(cacheFullName, 5); //for possible waiting thread, wait till file is visible via operating system

            } else {
                //all other file types
                //create random file; and if error, only partial random file will be created
                FileOutputStream fos = new FileOutputStream(cacheFullName + random); 
                OutputStreamSourceSimple osss = new OutputStreamSourceSimple(fos);
                boolean ok;
                
                if (fileTypeName.equals(".geotif")) {
                    ok = saveAsGeotiff(requestUrl, userDapQuery, osss, dir, fileName);

                } else if (fileTypeName.equals(".kml")) {
                    ok = saveAsKml(loggedInAs, requestUrl, userDapQuery, osss);

                } else if (String2.indexOf(imageFileTypeNames, fileTypeName) >= 0) {
                    //do pdf and png LAST, so kml caught above
                    ok = saveAsImage(loggedInAs, requestUrl, userDapQuery, dir, fileName, osss, fileTypeName);

                } else {
                    fos.close();
                    File2.delete(cacheFullName + random);
                    throw new SimpleException("Error: " +
                        "fileType=" + fileTypeName + " isn't supported by this dataset.");
                }

                fos.close();
                File2.rename(cacheFullName + random, cacheFullName); 
                if (!ok) //make eligible to be removed from cache in 5 minutes
                    File2.touch(cacheFullName, 
                        Math.max(0, EDStatic.cacheMillis - 5 * Calendar2.MILLIS_PER_MINUTE));

                File2.isFile(cacheFullName, 5); //for possible waiting thread, wait till file is visible via operating system
            }
        }

        //then handle .ncHeader
        if (fileTypeName.equals(".ncHeader")) {
            //thread-safe creation of the file 
            //(If there are almost simultaneous requests for the same one, only one thread will make it.)
            synchronized(String2.canonical(fullName)) {
                String error = String2.writeToFile(fullName + random, 
                    NcHelper.dumpString(cacheFullName, false)); //!!!this doesn't do anything to internal " in a String attribute value.
                if (error.length() == 0) {
                    File2.rename(fullName + random, fullName); //make available in an instant
                    File2.isFile(fullName, 5); //for possible waiting thread, wait till file is visible via operating system
                } else {
                    throw new RuntimeException(error);
                }
            }
        }

        //copy file to outputStream
        //(I delayed getting actual outputStream as long as possible.)
        if (!File2.copy(fullName, outputStreamSource.outputStream(
            fileTypeName.equals(".ncHeader")? "UTF-8" : 
            fileTypeName.equals(".kml")? "UTF-8" : 
            ""))) {
            //outputStream contentType already set,
            //so I can't go back to html and display error message
            //note than the message is thrown if user cancels the transmission; so don't email to me
            String2.log("Error while transmitting " + fileName + fileTypeExtension);
        }



    }

    /**
     * This deals with requests for a Make A Graph (MakeAGraph, MAG) web page for this dataset. 
     *
     * @param request may be null. Currently, this isn't used.
     * @param loggedInAs  the name of the logged in user (or null if not logged in).
     *   Normally, this is not used to test if this edd is accessibleTo loggedInAs, 
     *   but it unusual cases (EDDTableFromPost?) it could be.
     *   Normally, this is just used to determine which erddapUrl to use (http vs https).
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     * @param userDapQuery from the user (may be "" or null), still percentEncoded (shouldn't be null).
     *    If the query has missing or invalid parameters, defaults will be used.
     *    If the query has irrelevant parameters, they will be ignored.
     * @param outputStreamSource  the source of an outputStream that receives the results,
     *    usually already buffered.
     * @param dir the directory to use for temporary/cache files [currently, not used]
     * @param fileName the name for the 'file' (no dir, no extension),
     *    which is used to write the suggested name for the file to the response 
     *    header.  [currently, not used]
     * @param fileTypeName must be .graph [currently, not used]
     * @throws Throwable if trouble
     */
    public void respondToGraphQuery(HttpServletRequest request, String loggedInAs,
        String requestUrl, String userDapQuery, 
        OutputStreamSource outputStreamSource,
        String dir, String fileName, String fileTypeName) throws Throwable {

        if (reallyVerbose)
            String2.log("*** respondToGraphQuery");

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        String formName = "f1"; //change JavaScript below if this changes
        OutputStream out = outputStreamSource.outputStream("UTF-8");
        Writer writer = new OutputStreamWriter(out, "UTF-8"); 
        HtmlWidgets widgets = new HtmlWidgets("", true, EDStatic.imageDirUrl(loggedInAs));

        //write the header
        writer.write(EDStatic.startHeadHtml(tErddapUrl,  
            title() + //", from " + XML.encodeAsHTML(institution()) + 
            " - Make A Graph"));
        writer.write("\n" + rssHeadLink(loggedInAs));
        writer.write("\n</head>\n");
        writer.write(EDStatic.startBodyHtml(loggedInAs));
        writer.write("\n");
        writer.write(HtmlWidgets.htmlTooltipScript(EDStatic.imageDirUrl(loggedInAs))); //this is a link to a script
        writer.write(HtmlWidgets.dragDropScript(EDStatic.imageDirUrl(loggedInAs)));    //this is a link to a script
        writer.flush(); //Steve Souder says: the sooner you can send some html to user, the better
        try {
            writer.write(EDStatic.youAreHereWithHelp(loggedInAs, "griddap", 
                "Make a Graph", 
                "<b>To make a graph of data from this grid dataset, repeatedly:</b><ol>" +
                "<li>Change the 'Graph Type' and the variables for the graph's axes." +
                "<li>Change the 'Dimension Ranges' to specify a subset of the data." +
                "<li>Change the 'Graph Settings' as desired." +
                "<li>Press 'Redraw the Graph'." +
                "</ol>" +
                "This Make A Graph web page just simplifies the creation of griddap URLs with graphics commands. " +
                "<br><b>If you want, you can create these URLs by hand or have a computer program do it." +
                "<br>Then you don't have to use this form to get data. See the 'Bypass this form' link below.</b>"));
            writeHtmlDatasetInfo(loggedInAs, writer, true, true, false, userDapQuery, "");

            //make the big table
            writer.write("&nbsp;\n"); //necessary for the blank line before the table (not <p>)
            writer.write(widgets.beginTable(0, 0, ""));  //the big table
            writer.write("<tr><td align=\"left\" valign=\"top\">\n"); 

            //begin the form
            writer.write(widgets.beginForm(formName, "GET", "", ""));
         
            //parse the query so &-separated parts are handy
            String paramName, paramValue, partName, partValue, pParts[];
            String queryParts[] = getUserQueryParts(userDapQuery); //always at least 1 part (may be "")

            //find the axisVariables (all are always numeric) with >1 value
            StringArray sa = new StringArray();
            for (int av = 0; av < axisVariables.length; av++) {
                if (axisVariables[av].sourceValues().size() > 1) 
                    sa.add(axisVariableDestinationNames()[av]);
            }
            if (sa.size() == 0)  //accessibleViaMAG tests this, too
                throw new SimpleException("Query error: " +
                    "Make A Graph requires at least one axis with more than one value.");
            String[] avNames = sa.toArray();

            //find the numeric dataVariables 
            sa = new StringArray();
            for (int dv = 0; dv < dataVariables.length; dv++) {
                if (dataVariables[dv].destinationDataTypeClass() != String.class) 
                    sa.add(dataVariables[dv].destinationName());
            }
            if (sa.size() == 0) //accessibleViaMAG tests this, too
                throw new SimpleException("Query error: " +
                    "Make A Graph requires at least one numeric data variable.");
            String[] dvNames = sa.toArray();
            sa.add(0, "");
            String[] dvNames0 = sa.toArray();
            sa.remove(0);
            sa = null;
            //if you need advNames, you will need to modify the javascript below

            //parse the query to get preferredDV0 and constraints
            StringArray tDestNames = new StringArray();
            IntArray tConstraints = new IntArray();
            parseDataDapQuery(userDapQuery, tDestNames, tConstraints, true);
            String preferredDV0 = tDestNames.size() > 0? tDestNames.get(0) : dvNames[0];
            if (reallyVerbose) String2.log("preferredDV0=" + preferredDV0); 

            String gap = "&nbsp;&nbsp;&nbsp;";

            //*** set the Graph Type
            StringArray drawsSA = new StringArray();        
            //it is important for javascript below that first 3 options are the very similar (L L&M M)
            drawsSA.add("lines");  
            drawsSA.add("linesAndMarkers");   int defaultDraw = 1; 
            drawsSA.add("markers");  
            if (axisVariables.length >= 1 && dataVariables.length >= 2) 
                drawsSA.add("sticks");
            if (lonIndex >= 0 && latIndex >= 0) {//currently on if x=lon and y=lat 
                defaultDraw = drawsSA.size();
                drawsSA.add("surface");
            }
            if (lonIndex >= 0 && latIndex >= 0 && dataVariables.length >= 2) 
                drawsSA.add("vectors");
            String draws[] = drawsSA.toArray();
            boolean preferDefaultVars = true;
            int draw = defaultDraw;
            partValue = String2.stringStartsWith(queryParts, partName = ".draw=");
            if (partValue != null) {
                draw = String2.indexOf(draws, partValue.substring(partName.length()));
                if (draw >= 0) { // valid .draw was specified
                    preferDefaultVars = false;
                    //but check that it is possible
                    boolean trouble = false;
                    if ((draws[draw].equals("surface") || draws[draw].equals("vectors")) &&
                        (lonIndex < 0 || latIndex < 0)) 
                        trouble = true;
                    if ((draws[draw].equals("sticks") || draws[draw].equals("vectors")) && 
                        dvNames.length < 2)
                        trouble = true;
                    if (trouble) {
                        preferDefaultVars = true;
                        draw = String2.indexOf(draws, "linesAndMarkers"); //safest
                    }
                } else {
                    preferDefaultVars = true;
                    draw = defaultDraw;
                }
            }
            boolean drawLines = draws[draw].equals("lines");
            boolean drawLinesAndMarkers = draws[draw].equals("linesAndMarkers");
            boolean drawMarkers = draws[draw].equals("markers");
            boolean drawSticks  = draws[draw].equals("sticks");
            boolean drawSurface = draws[draw].equals("surface");
            boolean drawVectors = draws[draw].equals("vectors");
            if (reallyVerbose) String2.log("draw=" + draws[draw] + " preferDefaultVars=" + preferDefaultVars);
            //if (debugMode) String2.log("respondToGraphQuery 3");

            //find default stick or vector data vars  (adjacent, starting at dv=sameUnits1)
            //heuristic: look for two adjacent dv that have same units
            int sameUnits1 = 0; //default  dv
            String units1 = null;
            String units2 = findDataVariableByDestinationName(dvNames[0]).units(); 
            for (int sameUnits2 = 1; sameUnits2 < dvNames.length; sameUnits2++) {
                units1 = units2;
                units2 = findDataVariableByDestinationName(dvNames[sameUnits2]).units(); 
                if (units1 != null && units2 != null && units1.equals(units2)) {
                    sameUnits1 = sameUnits2 - 1;
                    break;
                }
            }

            //set draw-related things
            int nVars = -1, dvPo = 0;
            String varLabel[], varHelp[], varOptions[][];
            String varName[] = {"", "", "", ""};  //fill with defaults below, then from .vars      
            if (drawLines) {
                nVars = 2;
                varLabel = new String[]{"X Axis:", "Y Axis:"};
                varHelp  = new String[]{"graph's X Axis.", "graph's X Axis."};
                varOptions = new String[][]{avNames, dvNames};
                varName[0] = timeIndex >= 0? EDV.TIME_NAME : avNames[0];
                varName[1] = preferredDV0;
            } else if (drawLinesAndMarkers || drawMarkers) {
                nVars = 3;
                varLabel = new String[]{"X Axis:", "Y Axis:", "Color:"};
                varHelp  = new String[]{"graph's X Axis.", "graph's X Axis.", 
                    "marker's color (via the Color Bar) (or leave blank)."};
                varOptions = new String[][]{avNames, dvNames, dvNames0};
                varName[0] = timeIndex >= 0? EDV.TIME_NAME : avNames[0];
                varName[1] = preferredDV0;
            } else if (drawSticks) {
                nVars = 3;
                varLabel = new String[]{"X Axis:", "Stick X:", "Stick Y:"};
                varHelp  = new String[]{"graph's X Axis.", "stick's x-component.", "stick's y-component."};
                varOptions = new String[][]{avNames, dvNames, dvNames};
                varName[0] = timeIndex >= 0? EDV.TIME_NAME : avNames[0];
                varName[1] = dvNames[sameUnits1];
                varName[2] = dvNames[sameUnits1 + 1];
            } else if (drawSurface) {
                nVars = 3;
                varLabel = new String[]{"X Axis:", "Y Axis:", "Color:"};
                varHelp  = new String[]{"map's X Axis.", "map's Y Axis.", "surface's color (via the Color Bar)."};
                varOptions = new String[][]{new String[]{EDV.LON_NAME}, new String[]{EDV.LAT_NAME}, dvNames};
                varName[0] = EDV.LON_NAME;
                varName[1] = EDV.LAT_NAME;
                varName[2] = preferredDV0;
            } else if (drawVectors) {
                nVars = 4;
                varLabel = new String[]{"X Axis:", "Y Axis:", "Vector X:", "Vector Y:"};
                varHelp  = new String[]{"map's X Axis.", "map's Y Axis.", "vector's x-component.", "vector's y-component."};
                varOptions = new String[][]{new String[]{EDV.LON_NAME}, new String[]{EDV.LAT_NAME}, dvNames, dvNames};
                varName[0] = EDV.LON_NAME;
                varName[1] = EDV.LAT_NAME;
                varName[2] = dvNames[sameUnits1];
                varName[3] = dvNames[sameUnits1 + 1];
            } else throw new SimpleException("Query error: " +
                "'draw' was not set.");
            //if (debugMode) String2.log("respondToGraphQuery 4");

            //avoid lat lon reversed (which sgtMap will reverse)
            if (varName[0].equals("latitude") &&
                varName[1].equals("longitude")) {
                varName[0] = "longitude";
                varName[1] = "latitude";
            }

            //find axisVar index (or -1 if not an axis var), not index in avNames
            int axisVarX = String2.indexOf(axisVariableDestinationNames(), varName[0]); 
            int axisVarY = String2.indexOf(axisVariableDestinationNames(), varName[1]);
            if (reallyVerbose) String2.log("varName[]=" + String2.toCSVString(varName));

            //set dimensions' start and stop
            int nAv = axisVariables.length;
            String avStart[]   = new String[nAv];
            String avStop[]    = new String[nAv];
            int avStartIndex[] = new int[nAv];
            int avStopIndex[]  = new int[nAv];
            StringBuilder constraints = new StringBuilder();
            String sliderFromNames[] = new String[nAv];
            String sliderToNames[] = new String[nAv];
            int sliderNThumbs[] = new int[nAv];
            String sliderUserValuesCsvs[] = new String[nAv];
            int sliderInitFromPositions[] = new int[nAv];
            int sliderInitToPositions[] = new int[nAv];
            boolean showStartAndStopFields[] = new boolean[nAv];
            int sourceSize[] = new int[nAv];
            double 
                latFirst=Double.NaN,  lonFirst=Double.NaN,  timeFirst=Double.NaN,
                latLast=Double.NaN,   lonLast=Double.NaN,   timeLast=Double.NaN,
                latStart=Double.NaN,  lonStart=Double.NaN,  timeStart=Double.NaN,
                latStop=Double.NaN,   lonStop=Double.NaN,   timeStop=Double.NaN,
                latCenter=Double.NaN, lonCenter=Double.NaN, timeCenter=Double.NaN,
                latRange=Double.NaN,  lonRange=Double.NaN,  timeRange=Double.NaN;
            int lonAscending = 0, latAscending = 0, timeAscending = 0;
            for (int av = 0; av < nAv; av++) {
                EDVGridAxis edvga = axisVariables[av];
                EDVTimeGridAxis edvtga = av == timeIndex? (EDVTimeGridAxis)edvga : null;
                double defStart = av == timeIndex?  //note max vs first
                    Math.max(edvga.destinationMax() - 7 * Calendar2.SECONDS_PER_DAY, edvga.destinationMin()) :
                    edvga.firstDestinationValue();
                double defStop = av == timeIndex?
                    edvga.destinationMax():
                    edvga.lastDestinationValue();
                sourceSize[av] = edvga.sourceValues().size();
                int precision = av == timeIndex? 10 : 7;
                showStartAndStopFields[av] = av == axisVarX || av == axisVarY;

                //find start and end
                int ti1 = tConstraints.get(av * 3 + 0);
                int ti2 = tConstraints.get(av * 3 + 2);
                if (showStartAndStopFields[av] && ti1 == ti2) {
                    if (ti2 == 0) {
                        ti1 = 0; ti2 = 1;
                    } else { 
                        ti1 = ti2 - 1;
                    }
                }
                double dStart = userDapQuery.length() == 0? defStart :
                    edvga.destinationValue(ti1).getNiceDouble(0);
                double dStop = userDapQuery.length() == 0? defStop :
                    edvga.destinationValue(ti2).getNiceDouble(0);

                //compare dStart and dStop to ensure valid
                if (edvga.averageSpacing() > 0) { //may be negative,  looser test than isAscending
                    //ascending axis values
                    if (showStartAndStopFields[av]) {
                        if (Math2.greaterThanAE(precision, dStart, dStop)) 
                            dStart = defStart; 
                        if (Math2.greaterThanAE(precision, dStart, dStop)) 
                            dStop = defStop; 
                    } else {
                        if (!Math2.lessThanAE(precision, dStart, dStop)) 
                            dStart = edvga.firstDestinationValue(); //not defStart; stop field is always visible, so change start
                        if (!Math2.lessThanAE(precision, dStart, dStop)) 
                            dStop = edvga.lastDestinationValue(); 
                    }
                } else {
                    //descending axis values
                    if (showStartAndStopFields[av]) {
                        if (Math2.greaterThanAE(precision, dStop, dStart)) //stop start reversed from above 
                            dStart = defStart; 
                        if (Math2.greaterThanAE(precision, dStop, dStart)) 
                            dStop = defStop; 
                    } else {
                        if (!Math2.lessThanAE(precision, dStop, dStart)) 
                            dStart = edvga.firstDestinationValue(); //not defStart; stop field is always visible, so change start
                        if (!Math2.lessThanAE(precision, dStop, dStart)) 
                            dStop = edvga.lastDestinationValue(); 
                    }
                }
                
                //format
                avStart[av]      = edvga.destinationToString(dStart);
                avStop[av]       = edvga.destinationToString(dStop);
                avStartIndex[av] = edvga.destinationToClosestSourceIndex(dStart);
                avStopIndex[av]  = edvga.destinationToClosestSourceIndex(dStop);

                if (av == lonIndex) {
                    lonAscending = edvga.isAscending()? 1 : -1;
                    lonFirst  = edvga.firstDestinationValue();
                    lonLast   = edvga.lastDestinationValue();
                    lonStart  = dStart;
                    lonStop   = dStop;
                    lonCenter = (dStart + dStop) / 2;
                    lonRange  = dStop - dStart;
                }
                if (av == latIndex) {
                    latAscending = edvga.isAscending()? 1 : -1;
                    latFirst  = edvga.firstDestinationValue();
                    latLast   = edvga.lastDestinationValue();
                    latStart  = dStart;
                    latStop   = dStop;
                    latCenter = (dStart + dStop) / 2;
                    latRange  = dStop - dStart;
                }
                if (av == timeIndex) {
                    timeAscending = edvga.isAscending()? 1 : -1;
                    timeFirst  = edvga.firstDestinationValue();
                    timeLast   = edvga.lastDestinationValue();
                    timeStart  = dStart;
                    timeStop   = dStop;
                    timeCenter = (dStart + dStop) / 2;
                    timeRange  = dStop - dStart;
                }
            }
           
            //zoom?
            boolean zoomLatLon = (drawSurface || drawVectors) && 
                varName[0].equals("longitude") &&
                varName[1].equals("latitude");
            boolean zoomTime = varName[0].equals("time") && timeAscending == 1;

            //If user clicked on map, change some of the avXxx[], lonXxx, latXxx values.
            partValue = String2.stringStartsWith(queryParts, partName = ".click=?"); //? indicates user clicked on map
            if (zoomLatLon && partValue != null) {
                try {
                    String xy[] = String2.split(partValue.substring(8), ','); //e.g., 24,116

                    //read pngInfo file (if available, e.g., if graph is x=lon, y=lat and image recently created)
                    int clickPo = userDapQuery.indexOf("&.click=?");
                    Object pngInfo[] = readPngInfo(loggedInAs, userDapQuery.substring(0, clickPo), ".png"); 
                    int graphWESN[] = null;
                    if (pngInfo != null) {
                        graphWESN = (int[])pngInfo[1];
                        if (reallyVerbose) 
                            String2.log("  pngInfo graphWESN=" + String2.toCSVString(graphWESN));
                    }

                    double clickLonLat[] = SgtUtil.xyToLonLat(
                        String2.parseInt(xy[0]), String2.parseInt(xy[1]),
                        graphWESN,    //graph int extent
                        new double[]{ //graph double extent 
                            Math.min(lonStart, lonStop), Math.max(lonStart, lonStop), 
                            Math.min(latStart, latStop), Math.max(latStart, latStop)},
                        new double[]{ //data extent
                            Math.min(lonFirst, lonLast), Math.max(lonFirst, lonLast),
                            Math.min(latFirst, latLast), Math.max(latFirst, latLast)} );

                    if (clickLonLat != null) {
                        EDVGridAxis lonEdvga = axisVariables[lonIndex];
                        EDVGridAxis latEdvga = axisVariables[latIndex];
                        
                        //get current radius, and shrink if clickLonLat is closer to data limits
                        double radius = Math.max(Math.abs(lonStop - lonStart), Math.abs(latStart - latStop)) / 2;
                        radius = Math.min(radius, clickLonLat[0] - lonEdvga.destinationMin());
                        radius = Math.min(radius, lonEdvga.destinationMax() - clickLonLat[0]);
                        radius = Math.min(radius, clickLonLat[1] - latEdvga.destinationMin());
                        radius = Math.min(radius, latEdvga.destinationMax() - clickLonLat[1]);

                        //if not too close to data's limits...  success
                        if (radius >= 0.01) {
                            int index0, index1;

                            //lon
                            index0 = lonEdvga.destinationToClosestSourceIndex(clickLonLat[0] - radius);
                            index1 = lonEdvga.destinationToClosestSourceIndex(clickLonLat[0] + radius);
                            if (!lonEdvga.isAscending()) {
                                int ti = index0; index0 = index1; index1 = ti;
                            }
                            avStartIndex[lonIndex] = index0;
                            avStopIndex[ lonIndex] = index1;
                            lonStart = lonEdvga.destinationValue(index0).getNiceDouble(0);
                            lonStop  = lonEdvga.destinationValue(index1).getNiceDouble(0);
                            avStart[lonIndex] = String2.genEFormat6(lonStart);
                            avStop[ lonIndex] = String2.genEFormat6(lonStop);
                            lonCenter = (lonStart + lonStop) / 2;
                            lonRange = lonStop - lonStart;

                            //lat
                            index0 = latEdvga.destinationToClosestSourceIndex(clickLonLat[1] - radius);
                            index1 = latEdvga.destinationToClosestSourceIndex(clickLonLat[1] + radius);
                            if (!latEdvga.isAscending()) {
                                int ti = index0; index0 = index1; index1 = ti;
                            }
                            avStartIndex[latIndex] = index0;
                            avStopIndex[ latIndex] = index1;
                            latStart = latEdvga.destinationValue(index0).getNiceDouble(0);
                            latStop  = latEdvga.destinationValue(index1).getNiceDouble(0);
                            avStart[latIndex] = String2.genEFormat6(latStart);
                            avStop[ latIndex] = String2.genEFormat6(latStop);
                            latCenter = (latStart + latStop) / 2;
                            latRange = latStop - latStart;
                        }
                    }           
                } catch (Throwable t) {
                    String2.log("Error while trying to read &.click? value.\n" + 
                        MustBe.throwableToString(t));
                }
            }


            int idealTimeN = -1; //1..100
            int idealTimeUnits = -1;
            if (zoomTime) {

                //set idealTimeN (1..100), idealTimeUnits;
                partValue = String2.stringStartsWith(queryParts, ".timeRange=");
                if (partValue != null) {
                    //try to read from url params
                    String parts[] = String2.split(partValue.substring(11), ',');
                    if (parts.length == 2) {
                        idealTimeN = String2.parseInt(parts[0]);
                        idealTimeUnits = String2.indexOf(Calendar2.IDEAL_UNITS_OPTIONS, parts[1]);
                    }
                }
                //if not set, find closest
                if (reallyVerbose)
                    String2.log("  setup zoomTime timeRange=" + timeRange + " idealTimeN=" + idealTimeN + " units=" + idealTimeUnits);
                if (idealTimeN < 1 || idealTimeN > 100 ||
                    idealTimeUnits < 0) {

                    idealTimeUnits = Calendar2.IDEAL_UNITS_OPTIONS.length - 1;
                    while (idealTimeUnits > 0 && 
                        timeRange < Calendar2.IDEAL_UNITS_SECONDS[idealTimeUnits]) {
                        idealTimeUnits--;
                        //String2.log("  selecting timeRange=" + timeRange + " timeUnits=" + idealTimeUnits);
                    }
                    idealTimeN = Math2.minMax(1, 100, 
                        Math2.roundToInt(timeRange / Calendar2.IDEAL_UNITS_SECONDS[idealTimeUnits]));
                }
                if (reallyVerbose)
                    String2.log("  idealTimeN+Units=" + idealTimeN + " " + Calendar2.IDEAL_UNITS_SECONDS[idealTimeUnits]);


                //make idealized timeRange
                timeRange = idealTimeN * Calendar2.IDEAL_UNITS_SECONDS[idealTimeUnits]; //sometimes too low
            }


            //show Graph Type choice
            writer.write(widgets.beginTable(0, 0, "")); //the Graph Type and vars table
            paramName = "draw";
            writer.write(
                "<tr>\n" +
                "  <td nowrap><b>Graph Type:&nbsp;</b>" + 
                "  </td>\n" +
                "  <td nowrap>\n");
            writer.write(widgets.select(paramName, "", 
                1, draws, draw, 
                //change->submit so form always reflects graph type
                "onChange='mySubmit(" +  
                    (draw < 3? "f1.draw.selectedIndex<3" : //if old and new draw are <3, do send var names
                        "false") + //else don't send var names
                    ");'")); 
            writer.write(
                EDStatic.htmlTooltipImage(loggedInAs, 
                    "<b>Graph Type</b>" +
                    "<br>Graph Type = 'lines' draws lines on a graph where X=a dimension" +
                    "<br>and Y=a data variable. " +

                    "<p>Graph Type = 'linesAndMarkers' draws lines and markers on a graph " +
                    "<br>where X=a dimension and Y=a data variable. " +
                    "<br>If a Color variable is specified, the markers are colored." +

                    "<p>Graph Type = 'markers' plots markers on a graph where X=a dimension" +
                    "<br>and Y=a data variable. " +
                    "<br>If a Color variable is specified, the markers are colored." +

                    "<p>Graph Type = 'sticks' is usually used to plot time on the x axis," +
                    "<br>with the sticks being draw from the x component and y component" +
                    "<br>of currents or wind data." +

                    "<p>Graph Type = 'surface' plots a longitude/latitude grid of data as a" +
                    "<br>colored surface on a map." +
                    "<br>Currently, 'surface' requires that X=longitude and Y=latitude." +

                    "<p>Graph Type = 'vectors' plots vectors on a map." +
                    "<br>Currently, this requires that X=longitude and Y=latitude." +
                    "<br>The other variables provide the vector's x component and y component." +
                    "<br>So it is often used for currents or wind data." +

                    "<p><b>Changing the Graph Type or variables automatically submits this form.</b>" +
                    "<br>For the remainder of this form:" +
                    "<br>&nbsp;&nbsp;make changes, then press 'Redraw the Graph' below.") + 
                "  </td>\n" +
                "</tr>\n");


            //pick variables
            partValue = String2.stringStartsWith(queryParts, partName = ".vars=");
            if (partValue == null)
                 pParts = new String[0];
            else pParts = String2.split(partValue.substring(partName.length()), '|');
            for (int v = 0; v < nVars; v++) {
                String tDvNames[] = varOptions[v];
                int vi = -1;
                if (!preferDefaultVars && pParts.length > v) 
                    vi = String2.indexOf(tDvNames, pParts[v]);
                if (vi < 0) //use default
                    vi = String2.indexOf(tDvNames, varName[v]); 
                //avoid duplicate with previous var 
                //(there are never more than 2 axis or 2 data vars in a row)
                if (v >= 1 && varName[v-1].equals(tDvNames[vi]))
                    vi = vi == 0? 1 : 0; 
                varName[v] = tDvNames[vi];
                paramName = "var" + v;
                writer.write("<tr>\n" +
                    "  <td nowrap>" + varLabel[v] + "&nbsp;" +
                    "  </td>\n" +
                    "  <td nowrap>\n");
                writer.write(widgets.select(paramName, "",
                    1, tDvNames, vi, 
                    //change->submit so axisVar's showStartAndStop always reflects graph variables
                    "onChange='mySubmit(true);'")); //true= send var names
                writer.write( 
                    EDStatic.htmlTooltipImage(loggedInAs, 
                        "Select the variable for the " + varHelp[v] + 
                        "<p><b>Changing the Graph Type or variables automatically submits this form.</b>" +
                        "<br>For the remainder of this form:" +
                        "<br>&nbsp;&nbsp;make changes, then press 'Redraw the Graph' below.") +
                    "  </td>\n" +
                    "</tr>\n");
            }

            //end the Graph Type and vars table
            writer.write(widgets.endTable()); 

            //*** write the Dimension Constraints table
            writer.write("&nbsp;\n"); //necessary for the blank line before start of table (not <p>)
            writer.write(widgets.beginTable(0, 0, "width=\"50%\"")); 
            String tRangeStartStopHtml = 
                "<tt>Start:Stop</tt> specify the subset of data that will be plotted on the graph.";
            writer.write(
                "<tr>\n" +
                "  <th nowrap align=\"left\">Dimension Ranges " + 
                    EDStatic.htmlTooltipImage(loggedInAs, tRangeStartStopHtml) + 
                    "</th>\n" +
                "  <th nowrap align=\"center\">" + gap + EDStatic.EDDGridStart + " " + 
                    EDStatic.htmlTooltipImage(loggedInAs, tRangeStartStopHtml + "<br>" + EDStatic.EDDGridStartHtml) + 
                    "</th>\n" +
                "  <th nowrap align=\"center\">" + gap + EDStatic.EDDGridStop + " " + 
                    EDStatic.htmlTooltipImage(loggedInAs, tRangeStartStopHtml + "<br>" + EDStatic.EDDGridStopHtml) + 
                    "</th>\n" +
                "</tr>\n");


            //show the dimension widgets  
            for (int av = 0; av < nAv; av++) {
                EDVGridAxis edvga = axisVariables[av];
                String tFirst = edvga.destinationToString(edvga.firstDestinationValue());
                String tLast  = edvga.destinationToString(edvga.lastDestinationValue());
                String edvgaTooltip = edvga.htmlRangeTooltip();

                String tUnits = av == timeIndex? "UTC" : edvga.units();
                tUnits = tUnits == null? "" : "(" + tUnits + ") ";
                writer.write(
                    "<tr>\n" +
                    "  <td nowrap>" + edvga.destinationName() + " " +
                    tUnits +
                    EDStatic.htmlTooltipImageEDVGA(loggedInAs, edvga) +
                      "</td>\n");

                for (int ss = 0; ss < 2; ss++) { //0=start, 1=stop
                    paramName = (ss == 0? "start" : "stop") + av;
                    int tIndex = ss == 0? avStartIndex[av] : avStopIndex[av];
                    writer.write("<td nowrap>"); //a cell in the dimensions table

                    if (ss == 1 || showStartAndStopFields[av]) {
                        //show start or stop field, in a table with buttons

                        //generate the buttons
                        int fieldSize = 24;
                        StringBuilder buttons = new StringBuilder();

                        //show arrowLL?
                        if (tIndex >= 1 &&
                            (ss == 0 || 
                             (ss == 1 && !showStartAndStopFields[av]))) {
                            buttons.append(
                                "<td nowrap>\n" +
                                HtmlWidgets.htmlTooltipImage( 
                                    EDStatic.imageDirUrl(loggedInAs) + "arrowLL.gif", 
                                    "Click here to use the very first value in the list of values (" + 
                                        tFirst + ")<br>and redraw the graph.", 
                                    "onMouseUp='f1." + paramName + ".value=\"" + tFirst + 
                                        "\"; mySubmit(true);'") +
                                "</td>\n");
                            fieldSize -= 2;                            
                        } 
                        //show -?
                        if (tIndex >= 1 && 
                            (ss == 0 || 
                             (ss == 1 && (!showStartAndStopFields[av] || avStartIndex[av] + 1 < avStopIndex[av])))) {
                            //bug: wrong direction if source alt values are ascending depth values
                            String ts = edvga.destinationToString(
                                edvga.destinationValue(tIndex - 1).getNiceDouble(0)); 
                            buttons.append(
                                "<td nowrap>\n" +
                                HtmlWidgets.htmlTooltipImage( 
                                    EDStatic.imageDirUrl(loggedInAs) + "minus.gif", 
                                    "Click here to use the previous value in the list of values (" + 
                                        ts + ")<br>and redraw the graph.", 
                                    "onMouseUp='f1." + paramName + ".value=\"" + ts + 
                                        "\"; mySubmit(true);'") +
                                "</td>\n");
                            fieldSize -= 1;                            
                        } 
                        //show +?
                        if (tIndex < sourceSize[av] - 1 && 
                            ((ss == 0 && avStartIndex[av] + 1 < avStopIndex[av]) ||
                             ss == 1)) {
                            String ts = edvga.destinationToString(
                                edvga.destinationValue(tIndex + 1).getNiceDouble(0)); 
                            buttons.append(
                                "<td nowrap>\n" +
                                HtmlWidgets.htmlTooltipImage(
                                    EDStatic.imageDirUrl(loggedInAs) + "plus.gif", 
                                    "Click here to use the next value in the list of values (" + 
                                        ts + ")<br>and redraw the graph.", 
                                    "onMouseUp='f1." + paramName + ".value=\"" + ts + 
                                        "\"; mySubmit(true);'") +
                                "</td>\n");
                            fieldSize -= 1;                            
                        } 
                        //show arrowRR?
                        if (tIndex < sourceSize[av] - 1 && ss == 1) {
                            buttons.append(
                                "<td nowrap>\n" +
                                HtmlWidgets.htmlTooltipImage(
                                    EDStatic.imageDirUrl(loggedInAs) + "arrowRR.gif", 
                                    "Click here to use the very last value in the list of values (" + 
                                        tLast + ")<br>and redraw the graph.", 
                                    "onMouseUp='f1." + paramName + ".value=\"" + tLast + 
                                        "\"; mySubmit(true);'") +
                                "</td>\n");
                            fieldSize -= 2;                            
                        } 

                        //show start or stop field
                        writer.write(widgets.beginTable(0, 0, "width=\"10%\"")); //keep it small
                        writer.write("<tr><td nowrap>" + gap);
                        writer.write(widgets.textField(paramName, edvgaTooltip, fieldSize, 255, 
                            ss == 0? avStart[av] : avStop[av], ""));
                        writer.write("</td>\n" + 
                            buttons + 
                            "</tr>\n");
                        writer.write(widgets.endTable()); 
                    } else { 
                        writer.write(gap + "<font class=\"subduedColor\">&nbsp;specify just 1 value &rarr;</font>\n"); 
                        writer.write(widgets.hidden(paramName, "SeeStop"));
                    }
                    writer.write("  </td>\n");
                }

                //add avStart avStop to constraints
                if (showStartAndStopFields[av]) 
                    constraints.append("[(" + avStart[av] + "):(" + avStop[av] + ")]");
                else 
                    constraints.append("[(" + avStop[av] + ")]");

                // *** and a slider for this axis    (Make A Graph)
                sliderFromNames[av] = formName + ".start" + av;
                sliderToNames[  av] = formName + ".stop"  + av;
                sliderUserValuesCsvs[av] = edvga.sliderCsvValues();
                int safeSourceSize1 = Math.max(1, sourceSize[av] - 1);
                if (showStartAndStopFields[av]) {
                    //2 thumbs
                    sliderNThumbs[av] = 2;
                    sliderInitFromPositions[av] = Math2.roundToInt((avStartIndex[av] * (EDV.SLIDER_PIXELS - 1.0)) / safeSourceSize1);
                    sliderInitToPositions[av]   = sourceSize[av] == 1? EDV.SLIDER_PIXELS - 1 :
                                                  Math2.roundToInt((avStopIndex[av]  * (EDV.SLIDER_PIXELS - 1.0)) / safeSourceSize1);
                    writer.write(
                        "<tr align=\"left\">\n" +
                        "  <td nowrap colspan=\"3\" align=\"left\">\n" +
                        widgets.dualSlider(av, EDV.SLIDER_PIXELS - 1, "align=\"left\"") +
                        "  </td>\n" +
                        "</tr>\n");
                } else {
                    //1 thumb
                    sliderNThumbs[av] = 1;
                    sliderFromNames[av] = formName + ".stop"  + av;  //change from default
                    sliderInitFromPositions[av] = Math2.roundToInt((avStopIndex[av] * (EDV.SLIDER_PIXELS - 1.0)) / safeSourceSize1);
                    sliderInitToPositions[av] = EDV.SLIDER_PIXELS - 1;
                    writer.write(
                        "<tr align=\"left\">\n" +
                        "  <td nowrap colspan=\"3\" align=\"left\">\n" +
                        widgets.slider(av, EDV.SLIDER_PIXELS - 1, "align=\"left\"") +
                        "  </td>\n" +
                        "</tr>\n");
                }

            }
            //end of dimensions constraints table
            writer.write(widgets.endTable());


            //*** make graphQuery
            StringBuilder graphQuery = new StringBuilder();
            //add data varNames and constraints
            for (int v = 1; v < nVars; v++) {
                if (String2.indexOf(dvNames, varName[v]) >= 0) {
                    if (graphQuery.length() > 0)
                        graphQuery.append(",");
                    graphQuery.append(varName[v] + constraints);
                }
            }

            //set hidden time range widgets
            if (zoomTime) {
                writer.write(
                    widgets.hidden("timeN", "" + idealTimeN) +
                    widgets.hidden("timeUnits", "" + Calendar2.IDEAL_UNITS_OPTIONS[idealTimeUnits]));
            }


            //add .draw and .vars to graphQuery 
            graphQuery.append(
                "&.draw=" + draws[draw] +
                "&.vars=" + varName[0] + "|" + varName[1] + "|" + varName[2] + 
                    (nVars > 3? "|" + varName[3] : ""));

            //*** Graph Settings
            writer.write("&nbsp;\n"); //necessary for the blank line before start of table (not <p>)
            writer.write(widgets.beginTable(0, 0, "")); 
            writer.write("  <tr><th align=\"left\" colspan=\"2\" nowrap>Graph Settings</th></tr>\n");
            if (drawLinesAndMarkers || drawMarkers) {
                //get Marker settings
                int mType = -1, mSize = -1;
                partValue = String2.stringStartsWith(queryParts, partName = ".marker=");
                if (partValue != null) {
                    pParts = String2.split(partValue.substring(partName.length()), '|');
                    if (pParts.length > 0) mType = String2.parseInt(pParts[0]);
                    if (pParts.length > 1) mSize = String2.parseInt(pParts[1]); //the literal, not the index
                    if (reallyVerbose)
                        String2.log(".marker type=" + mType + " size=" + mSize);
                }

                //markerType
                paramName = "mType";
                if (mType < 0 || mType >= GraphDataLayer.MARKER_TYPES.length)
                    mType = GraphDataLayer.MARKER_TYPE_FILLED_SQUARE;
                //if (!yIsAxisVar && varName[2].length() > 0 && 
                //    GraphDataLayer.MARKER_TYPES[mType].toLowerCase().indexOf("filled") < 0) 
                //    mType = GraphDataLayer.MARKER_TYPE_FILLED_SQUARE; //needs "filled" marker type
                writer.write("  <tr>\n" +
                             "    <td nowrap>Marker Type:&nbsp;</td>\n" +
                             "    <td nowrap>");
                writer.write(widgets.select(paramName, 
                    "Specify the marker type.", 
                    1, GraphDataLayer.MARKER_TYPES, mType, ""));

                //markerSize
                paramName = "mSize";
                String mSizes[] = {"3", "4", "5", "6", "7", "8", "9", "10", "11", 
                    "12", "13", "14", "15", "16", "17", "18"};
                mSize = String2.indexOf(mSizes, "" + mSize); //convert from literal 3.. to index in mSizes[0..]
                if (mSize < 0)
                    mSize = String2.indexOf(mSizes, "" + GraphDataLayer.MARKER_SIZE_SMALL);
                writer.write(gap + "Size: ");
                writer.write(widgets.select(paramName, 
                    "Specify the marker size.", 
                    1, mSizes, mSize, ""));
                writer.write("    </td>\n" +
                             "  </tr>\n");

                //add to graphQuery
                graphQuery.append("&.marker=" + mType + "|" + mSizes[mSize]);
            }

            String colors[] = HtmlWidgets.PALETTE17;
            if (drawLines || drawLinesAndMarkers || drawMarkers || drawSticks || drawVectors) {

                //color
                paramName = "colr"; //not color, to avoid possible conflict
                partValue = String2.stringStartsWith(queryParts, partName = ".color=0x");
                int colori = String2.indexOf(colors, 
                    partValue == null? "" : partValue.substring(partName.length()));
                if (colori < 0)
                    colori = String2.indexOf(colors, "000000");
                writer.write("  <tr>\n" +
                             "    <td nowrap>Color:&nbsp;</td>\n" +
                             "    <td nowrap>");
                writer.write(widgets.color17("", paramName, 
                    "Select a color.", 
                    colori, ""));
                writer.write("    </td>\n" +
                             "  </tr>\n");

                //add to graphQuery
                graphQuery.append("&.color=0x" + HtmlWidgets.PALETTE17[colori]);
            }

            if (drawLinesAndMarkers || drawMarkers || drawSurface) {
                //color bar
                partValue = String2.stringStartsWith(queryParts, partName = ".colorBar=");
                pParts = partValue == null? new String[0] : String2.split(partValue.substring(partName.length()), '|');
                if (reallyVerbose)
                    String2.log(".colorBar=" + String2.toCSVString(pParts));

                //find dataVariable relevant to colorBar
                //(force change in values if this var changes?  but how know, since no state?)
                int tDataVariablePo = String2.indexOf(dataVariableDestinationNames(), 
                    varName[2]); //currently, bothrelevant representation uses varName[2] for "Color"
                EDV tDataVariable = tDataVariablePo >= 0? dataVariables[tDataVariablePo] : null;

                paramName = "p";
                String defaultPalette = ""; 
                //String2.log("defaultPalette=" + defaultPalette + " pParts.length=" + pParts.length);
                int palette = Math.max(0, 
                    String2.indexOf(EDStatic.palettes0, pParts.length > 0? pParts[0] : defaultPalette));
                writer.write("  <tr>\n" +
                             "    <td colspan=\"2\" nowrap>Color Bar: ");
                writer.write(widgets.select(paramName, 
                    "Select a palette for the color bar (or leave blank to get the default).", 
                    1, EDStatic.palettes0, palette, ""));

                String conDis[] = new String[]{"", "Continuous", "Discrete"};
                paramName = "pc";
                int continuous = pParts.length > 1? (pParts[1].equals("D")? 2 : pParts[1].equals("C")? 1 : 0) : 0;
                writer.write(gap + "Continuity: ");
                writer.write(widgets.select(paramName, 
                    "Specify whether the colors should be continuous or discrete<br>(or leave blank for the default).", 
                    1, conDis, continuous, ""));

                paramName = "ps";
                String defaultScale = "";
                int scale = Math.max(0, String2.indexOf(EDV.VALID_SCALES0, pParts.length > 2? pParts[2] : defaultScale));
                writer.write(gap + "Scale: ");
                writer.write(widgets.select(paramName, 
                    "Select a scale for the color bar (or leave blank for the default).", 
                    1, EDV.VALID_SCALES0, scale, ""));
                writer.write(
                    "    </td>\n" +
                    "  </tr>\n");

                paramName = "pMin";
                String defaultMin = "";
                String palMin = pParts.length > 3? pParts[3] : defaultMin;
                writer.write(
                    "  <tr>\n" +
                    "    <td colspan=\"2\" nowrap> " + gap + gap + "Min: ");
                writer.write(widgets.textField(paramName, 
                    "Specify the minimum value for the color bar (or leave blank for the default).", 
                   10, 40, palMin, ""));

                paramName = "pMax";
                String defaultMax = "";
                String palMax = pParts.length > 4? pParts[4] : defaultMax;
                writer.write(gap + "Max: ");
                writer.write(widgets.textField(paramName, 
                    "Specify the maximum value for the color bar (or leave blank for the default).", 
                   10, 40, palMax, ""));

                paramName = "pSec";
                int pSections = Math.max(0, String2.indexOf(EDStatic.paletteSections, pParts.length > 5? pParts[5] : ""));
                writer.write(gap + "N Sections: ");
                writer.write(widgets.select(paramName, 
                    "Specify the number of sections for the color bar" +
                    "<br>and the number of labels beneath the color bar (-1)" +
                    "<br>(or leave blank for the default).", 
                    1, EDStatic.paletteSections, pSections, ""));
                writer.write("    </td>\n" +
                             "  </tr>\n");

                //add to graphQuery 
                graphQuery.append(
                    "&.colorBar=" + EDStatic.palettes0[palette] + "|" +
                    (conDis[continuous].length() == 0? "" : conDis[continuous].charAt(0)) + "|" +
                    EDV.VALID_SCALES0[scale] + "|" +
                    palMin + "|" + palMax + "|" + EDStatic.paletteSections[pSections]);
            }

            if (drawVectors) {

                //Vector Standard 
                paramName = "vec";
                String vec = String2.stringStartsWith(queryParts, partName = ".vec=");
                vec = vec == null? "" : vec.substring(partName.length());
                writer.write("  <tr>\n" +
                             "    <td nowrap>Vector Standard:&nbsp;</td>\n" +
                             "    <td nowrap>");
                writer.write(widgets.textField(paramName, 
                    "Specify the data vector length (in data units) to be " +
                    "<br>scaled to the size of the sample vector in the legend" +
                    "<br>(or leave blank for the default).", 
                   10, 20, vec, ""));
                writer.write("    </td>\n" +
                             "  </tr>\n");

                //add to graphQuery
                if (vec.length() > 0)
                    graphQuery.append("&.vec=" + vec);
            }

            if (drawSurface) {
                //Draw Land
                int tLand = 0;
                String landOptions[] = {"", "under the data", "over the data"};  //order also affects javascript below
                partValue = String2.stringStartsWith(queryParts, partName = ".land=");
                if (partValue != null) {
                    partValue = partValue.substring(6);
                    if      (partValue.equals("under")) tLand = 1;
                    else if (partValue.equals("over"))  tLand = 2;
                }
                writer.write(
                    "<tr>\n" +
                    "  <td colSpan=\"2\" nowrap>Draw the land mask: \n");
                writer.write(widgets.select("land", 
                    "Specify whether the land mask should be drawn under or over the data.\n" +
                    "<br>(Some data has already had a land mask applied, making this irrelevant.)" +
                    "<br>(Or leave this blank to use the default value.)", 
                    1, landOptions, tLand, ""));
                writer.write(
                    "  </td>\n" +
                    "</tr>\n");

                //add to graphQuery
                if (tLand > 0)
                    graphQuery.append("&.land=" + (tLand == 1? "under" : "over"));
            }

            //yRange
            String yRange[] = new String[]{"", ""};
            if (!drawSurface) {
                paramName = "yRange";
                String tyRange = String2.stringStartsWith(queryParts, partName = ".yRange=");
                if (tyRange != null) {
                    String tyRangeAr[] = String2.split(tyRange.substring(partName.length()), '|');
                    for (int i = 0; i < 2; i++) {
                        if (tyRangeAr.length > i) {
                            double td = String2.parseDouble(tyRangeAr[i]);
                            yRange[i] = Double.isNaN(td)? "" : String2.genEFormat10(td);
                        }
                    }
                }
                writer.write(
                    "  <tr>\n" +
                    "    <td nowrap>Y Axis Minimum:&nbsp;</td>\n" +
                    "    <td nowrap width=\"90%\">\n");
                writer.write(widgets.textField("yRangeMin", 
                    EDStatic.yRangeMinHtml + EDStatic.yRangeHtml, 
                   10, 20, yRange[0], ""));
                writer.write("&nbsp;&nbsp;Maximum:\n");
                writer.write(widgets.textField("yRangeMax", 
                    EDStatic.yRangeMaxHtml + EDStatic.yRangeHtml, 
                   10, 20, yRange[1], ""));
                writer.write(
                    "    </td>\n" +
                    "  </tr>\n");

                //add to graphQuery
                if (yRange[0].length() > 0 || yRange[1].length() > 0) 
                    graphQuery.append("&.yRange=" + yRange[0] + "|" + yRange[1]);
            }


            //*** end of form
            writer.write(widgets.endTable()); //end of Graph Settings table

            //make javascript function to generate query
            writer.write(
                "<script type=\"text/javascript\"> \n" +
                "function makeQuery(varsToo) { \n" +
                "  try { \n" +
                "    var d = document; \n" +
                "    var start, tv, c = \"\", q = \"\"; \n"); //c=constraint  q=query
            //gather constraints
            for (int av = 0; av < nAv; av++) 
                writer.write(
                    "    start = d.f1.start" + av + ".value; \n" +
                    "    c += \"[\"; \n" +
                    "    if (start != \"SeeStop\") c += \"(\" + start + \"):\"; \n" + //javascript uses !=, not !equals()
                    "    c += \"(\" + d.f1.stop" + av + ".value + \")]\"; \n");
            //var[constraints],var[constraints]
            for (int v = 1; v < nVars; v++) {
                if (varOptions[v] == dvNames || varOptions[v] == dvNames0) { //simpler because advNames isn't an option
                    writer.write(
                        "    tv = d.f1.var" + v + ".options[d.f1.var" + v + ".selectedIndex].text; \n" +
                        "    if (tv.length > 0) { \n" +
                        "      if (q.length > 0) q += \",\"; \n" +  //javascript uses length, not length()
                        "      q += tv + c; \n" +
                        "    } \n");
                }
            }
            //graph settings  
            writer.write(
                "    q += \"&.draw=\" + d.f1.draw.options[d.f1.draw.selectedIndex].text; \n");
            writer.write(                
                "    if (varsToo) { \n" +
                "      q += \"&.vars=\" + d.f1.var0.options[d.f1.var0.selectedIndex].text + \n" +
                "        \"|\" + d.f1.var1.options[d.f1.var1.selectedIndex].text; \n");
            if (nVars >= 3) writer.write(
                "      q += \"|\" + d.f1.var2.options[d.f1.var2.selectedIndex].text; \n");
            if (nVars >= 4) writer.write(
                "      q += \"|\" + d.f1.var3.options[d.f1.var3.selectedIndex].text; \n");
            writer.write(
                "    } \n");  
            if (drawLinesAndMarkers || drawMarkers) writer.write(
                "    q += \"&.marker=\" + d.f1.mType.selectedIndex + \"|\" + \n" +
                "      d.f1.mSize.options[d.f1.mSize.selectedIndex].text; \n");
            if (drawLines || drawLinesAndMarkers || drawMarkers || drawSticks || drawVectors) writer.write(
                "    q += \"&.color=0x\"; \n" +
                "    for (var rb = 0; rb < " + colors.length + "; rb++) \n" + 
                "      if (d.f1.colr[rb].checked) q += d.f1.colr[rb].value; \n"); //always: one will be checked
            if (drawLinesAndMarkers || drawMarkers || drawSurface) writer.write(
                "    var tpc = d.f1.pc.options[d.f1.pc.selectedIndex].text;\n" +
                "    q += \"&.colorBar=\" + d.f1.p.options[d.f1.p.selectedIndex].text + \"|\" + \n" +
                "      (tpc.length > 0? tpc.charAt(0) : \"\") + \"|\" + \n" +
                "      d.f1.ps.options[d.f1.ps.selectedIndex].text + \"|\" + \n" +
                "      d.f1.pMin.value + \"|\" + d.f1.pMax.value + \"|\" + \n" +
                "      d.f1.pSec.options[d.f1.pSec.selectedIndex].text; \n");
            if (drawVectors) writer.write(
                "    if (d.f1.vec.value.length > 0) q += \"&.vec=\" + d.f1.vec.value; \n");
            if (drawSurface) writer.write(
                "    if (d.f1.land.selectedIndex > 0) " +
                    "q += \"&.land=\" + (d.f1.land.selectedIndex==1? \"under\" : \"over\"); \n");
            if (!drawSurface) writer.write(
                "    var yRMin=d.f1.yRangeMin.value; \n" +
                "    var yRMax=d.f1.yRangeMax.value; \n" +
                "    if (yRMin.length > 0 || yRMax.length > 0)\n" +
                "      q += \"\\x26.yRange=\" + yRMin + \"|\" + yRMax; \n");
            if (zoomTime) writer.write(
                "    q += \"&.timeRange=\" + d.f1.timeN.value + \",\" + d.f1.timeUnits.value; \n");
            writer.write(
                "    return q; \n" +
                "  } catch (e) { \n" +
                "    alert(e); \n" +
                "    return \"\"; \n" +
                "  } \n" +
                "} \n" +
                "function mySubmit(varsToo) { \n" +
                "  var q = makeQuery(varsToo); \n" +
                "  if (q.length > 0) window.location=\"" + //javascript uses length, not length()
                    tErddapUrl + "/griddap/" + datasetID + 
                    ".graph?\" + q;\n" + 
                "} \n" +
                "</script> \n");  

            //submit
            writer.write("&nbsp;\n"); //necessary for the blank line before start of table (not <p>)
            writer.write(widgets.beginTable(0, 0, "")); 
            writer.write(
                "<tr><td nowrap>");
            writer.write(widgets.htmlButton("button", "", "",
                "Redraw the graph based on the settings above.", 
                "<b>Redraw the Graph</b>", 
                "onMouseUp='mySubmit(true);'")); 
            writer.write(
                " " + EDStatic.patientData + "\n" +
                "</td></tr>\n");

            //Download the Data
            writer.write(
                "<tr><td nowrap>&nbsp;<br>Optional:<br>Then set the File Type:\n");
            paramName = "fType";
            writer.write(widgets.select(paramName, EDStatic.EDDSelectFileType, 1,
                allFileTypeNames, defaultFileTypeOption, 
                "onChange='f1.tUrl.value=\"" + tErddapUrl + "/griddap/" + datasetID + 
                    "\" + f1.fType.options[f1.fType.selectedIndex].text + " + 
                    "\"?" + String2.replaceAll(graphQuery.toString(), "&", "&amp;") + "\";'"));
            writer.write(" and\n");
            writer.write(widgets.button("button", "", 
                "Click <tt>'Redraw the Graph'</tt> just before clicking here, so the latest settings are used." +
                "<p>Then click here to download the data or image in the specified File Type." +
                "<br>" + EDStatic.patientData + "",
                "Download the Data or an Image", 
                //"class=\"skinny\" " + //only IE needs it but only IE ignores it
                "onMouseUp='window.location=\"" + 
                    tErddapUrl + "/griddap/" + datasetID + "\" + f1." + 
                    paramName + ".options[f1." + paramName + ".selectedIndex].text + \"?" + 
                    XML.encodeAsHTML(graphQuery.toString()) +
                "\";'")); //or open a new window: window.open(result);\n" +
            writer.write(
                "</td></tr>\n");

            //view the url
            String genViewHtml = String2.replaceAll(EDStatic.justGenerateAndViewGraphUrlHtml, 
                    "&protocolName;", dapProtocol);
            writer.write(
                "<tr><td nowrap>or view the URL: \n");
            writer.write(widgets.textField("tUrl", genViewHtml, 
                72, 1000, 
                tErddapUrl + "/griddap/" + datasetID + 
                    dataFileTypeNames[defaultFileTypeOption] + "?" + graphQuery.toString(), 
                ""));
            writer.write("<br>(<a href=\"" + tErddapUrl + "/griddap/documentation.html\" " +
            "title=\"griddap documentation\">Documentation&nbsp;/&nbsp;Bypass&nbsp;this&nbsp;form</a>\n" +
                EDStatic.htmlTooltipImage(loggedInAs, genViewHtml) + ")\n");
            writer.write("(<a href=\"" + tErddapUrl + "/griddap/documentation.html#fileType\">" +
                "File Type information</a>)\n");
            writer.write(
                "</td></tr>\n" +
                "</table>\n\n");

            //end form
            writer.write(widgets.endForm());
            writer.write(HtmlWidgets.ifJavaScriptDisabled);

            //*** end of left half of big table
            writer.write("</td>\n" +
                "<td>" + gap + "</td>\n" + //gap in center
                "<td nowrap align=\"left\" valign=\"top\">\n"); //begin right half

            //*** zoomLatLon stuff
            if (zoomLatLon) {
                writer.write(
                    "<b>Click</b> on the map to specify a new center point.\n" +
                    EDStatic.htmlTooltipImage(loggedInAs, 
                        "Click on the map to specify a new center point." +
                        "<br>Or, click just outside the map to see a map of the adjacent area." +
                        "<p>You can zoom way in very quickly by alternately:" +
                        "<br> * Clicking on a new center point." +
                        "<br> * Pressing \"In 8x\".") +
                    "<br><b>Zoom:</b>\n");

                double cRadius = Math.max(Math.abs(lonRange), Math.abs(latRange)) / 2; //will get to max eventually
                double zoomOut  = cRadius * 5 / 4;
                double zoomOut2 = cRadius * 2;
                double zoomOut8 = cRadius * 8;
                double zoomIn   = cRadius * 4 / 5;
                double zoomIn2  = cRadius / 2;
                double zoomIn8 =  cRadius / 8;

                //zoom out?
                boolean disableZoomOut = 
                    Math2.almostEqual(9, lonFirst, lonStart) && 
                    Math2.almostEqual(9, latFirst, latStart) &&
                    Math2.almostEqual(9, lonLast,  lonStop)  && 
                    Math2.almostEqual(9, latLast,  latStop);

                writer.write(
                //HtmlWidgets.htmlTooltipImage( 
                //    EDStatic.imageDirUrl(loggedInAs) + "arrowDD.gif", 
                widgets.button("button", "",  
                    "Zoom out to the full extent of the data and redraw the map.",  
                    "Data", 
                    "class=\"skinny\" " + 
                    (disableZoomOut? "disabled" : 
                    "onMouseUp='f1.start" + lonIndex + ".value=\"" + String2.genEFormat6(lonFirst) + "\"; " +
                               "f1.stop"  + lonIndex + ".value=\"" + String2.genEFormat6(lonLast)  + "\"; " +
                               "f1.start" + latIndex + ".value=\"" + String2.genEFormat6(latFirst) + "\"; " +
                               "f1.stop"  + latIndex + ".value=\"" + String2.genEFormat6(latLast)  + "\"; " +
                               "mySubmit(true);'")));

                //if zoom out, keep ranges intact by moving tCenter to safe place
                double tLonCenter, tLatCenter;
                tLonCenter = Math.max( lonCenter, Math.min(lonFirst, lonLast) + zoomOut8);
                tLonCenter = Math.min(tLonCenter, Math.max(lonFirst, lonLast) - zoomOut8);
                tLatCenter = Math.max( latCenter, Math.min(latFirst, latLast) + zoomOut8);
                tLatCenter = Math.min(tLatCenter, Math.max(latFirst, latLast) - zoomOut8);
                writer.write(
                widgets.button("button", "",  
                    "Zoom out 8x and redraw the map.",  
                    "Out 8x",
                    "class=\"skinny\" " + 
                    (disableZoomOut? "disabled" : 
                    "onMouseUp='f1.start" + lonIndex + ".value=\"" + String2.genEFormat6(tLonCenter - lonAscending * zoomOut8) + "\"; " +
                               "f1.stop"  + lonIndex + ".value=\"" + String2.genEFormat6(tLonCenter + lonAscending * zoomOut8) + "\"; " +
                               "f1.start" + latIndex + ".value=\"" + String2.genEFormat6(tLatCenter - latAscending * zoomOut8) + "\"; " +
                               "f1.stop"  + latIndex + ".value=\"" + String2.genEFormat6(tLatCenter + latAscending * zoomOut8) + "\"; " +
                               "mySubmit(true);'")));

                //if zoom out, keep ranges intact by moving tCenter to safe place
                tLonCenter = Math.max( lonCenter, Math.min(lonFirst, lonLast) + zoomOut2);
                tLonCenter = Math.min(tLonCenter, Math.max(lonFirst, lonLast) - zoomOut2);
                tLatCenter = Math.max( latCenter, Math.min(latFirst, latLast) + zoomOut2);
                tLatCenter = Math.min(tLatCenter, Math.max(latFirst, latLast) - zoomOut2);
                writer.write(
                widgets.button("button", "",  
                    "Zoom out 2x and redraw the map.",  
                    "Out 2x",
                    "class=\"skinny\" " + 
                    (disableZoomOut? "disabled" : 
                    "onMouseUp='f1.start" + lonIndex + ".value=\"" + String2.genEFormat6(tLonCenter - lonAscending * zoomOut2) + "\"; " +
                               "f1.stop"  + lonIndex + ".value=\"" + String2.genEFormat6(tLonCenter + lonAscending * zoomOut2) + "\"; " +
                               "f1.start" + latIndex + ".value=\"" + String2.genEFormat6(tLatCenter - latAscending * zoomOut2) + "\"; " +
                               "f1.stop"  + latIndex + ".value=\"" + String2.genEFormat6(tLatCenter + latAscending * zoomOut2) + "\"; " +
                               "mySubmit(true);'")));

                //if zoom out, keep ranges intact by moving tCenter to safe place
                tLonCenter = Math.max( lonCenter, Math.min(lonFirst, lonLast) + zoomOut);
                tLonCenter = Math.min(tLonCenter, Math.max(lonFirst, lonLast) - zoomOut);
                tLatCenter = Math.max( latCenter, Math.min(latFirst, latLast) + zoomOut);
                tLatCenter = Math.min(tLatCenter, Math.max(latFirst, latLast) - zoomOut);
                writer.write(
                widgets.button("button", "",  
                    "Zoom out a little and redraw the map.", 
                    "Out",
                    "class=\"skinny\" " + 
                    (disableZoomOut? "disabled" : 
                    "onMouseUp='f1.start" + lonIndex + ".value=\"" + String2.genEFormat6(tLonCenter - lonAscending * zoomOut) + "\"; " +
                               "f1.stop"  + lonIndex + ".value=\"" + String2.genEFormat6(tLonCenter + lonAscending * zoomOut) + "\"; " +
                               "f1.start" + latIndex + ".value=\"" + String2.genEFormat6(tLatCenter - latAscending * zoomOut) + "\"; " +
                               "f1.stop"  + latIndex + ".value=\"" + String2.genEFormat6(tLatCenter + latAscending * zoomOut) + "\"; " +
                               "mySubmit(true);'")));

                //zoom in     Math.max moves rectangular maps toward square
                writer.write(
                    widgets.button("button", "", 
                        "Zoom in a little and redraw the map.", 
                        "In",
                        "class=\"skinny\" " + 
                        "onMouseUp='f1.start" + lonIndex + ".value=\"" + String2.genEFormat6(lonCenter - lonAscending * zoomIn) + "\"; " +
                                   "f1.stop"  + lonIndex + ".value=\"" + String2.genEFormat6(lonCenter + lonAscending * zoomIn) + "\"; " +
                                   "f1.start" + latIndex + ".value=\"" + String2.genEFormat6(latCenter - latAscending * zoomIn) + "\"; " +
                                   "f1.stop"  + latIndex + ".value=\"" + String2.genEFormat6(latCenter + latAscending * zoomIn) + "\"; " +
                                   "mySubmit(true);'") +

                    widgets.button("button", "", 
                        "Zoom in 2x and redraw the map.",  
                        "In 2x",
                        "class=\"skinny\" " + 
                        "onMouseUp='f1.start" + lonIndex + ".value=\"" + String2.genEFormat6(lonCenter - lonAscending * zoomIn2) + "\"; " +
                                   "f1.stop"  + lonIndex + ".value=\"" + String2.genEFormat6(lonCenter + lonAscending * zoomIn2) + "\"; " +
                                   "f1.start" + latIndex + ".value=\"" + String2.genEFormat6(latCenter - latAscending * zoomIn2) + "\"; " +
                                   "f1.stop"  + latIndex + ".value=\"" + String2.genEFormat6(latCenter + latAscending * zoomIn2) + "\"; " +
                                   "mySubmit(true);'") + 
                    
                    widgets.button("button", "", 
                        "Zoom in 8x and redraw the map.",  
                        "In 8x",
                        "class=\"skinny\" " + 
                        "onMouseUp='f1.start" + lonIndex + ".value=\"" + String2.genEFormat6(lonCenter - lonAscending * zoomIn8) + "\"; " +
                                   "f1.stop"  + lonIndex + ".value=\"" + String2.genEFormat6(lonCenter + lonAscending * zoomIn8) + "\"; " +
                                   "f1.start" + latIndex + ".value=\"" + String2.genEFormat6(latCenter - latAscending * zoomIn8) + "\"; " +
                                   "f1.stop"  + latIndex + ".value=\"" + String2.genEFormat6(latCenter + latAscending * zoomIn8) + "\"; " +
                                   "mySubmit(true);'"));

                //trailing <br>
                writer.write("<br>");
            }

            //*** zoomTime stuff
            if (zoomTime) {
                if (reallyVerbose)
                    String2.log("zoomTime range=" + Calendar2.elapsedTimeString(timeRange * 1000) +
                      " center=" + Calendar2.epochSecondsToIsoStringT(timeCenter) +
                    "\n  first=" + Calendar2.epochSecondsToIsoStringT(timeFirst) +
                      "  start=" + Calendar2.epochSecondsToIsoStringT(timeStart) +
                    "\n   last=" + Calendar2.epochSecondsToIsoStringT(timeLast) +
                      "   stop=" + Calendar2.epochSecondsToIsoStringT(timeStop));

                writer.write(
                    "<b>Time range:</b>\n");

                String timeRangeString = idealTimeN + " " + Calendar2.IDEAL_UNITS_OPTIONS[idealTimeUnits];
                String timesVary = "<br>(The resulting time range will vary a little, based on available data.)";
                String timeRangeTip = 
                    "Select the desired time range." +
                    "<br>Then click on an arrow icon to the right to use the new value." +
                    "<p>Long time ranges (e.g., 1 year) use a lot of computer resources." +
                    "<br>Please don't select a long time range unless you need it.)";
                String timeGap = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\n";


                //n = 1..100
                writer.write(
                widgets.select("timeN", 
                    timeRangeTip, //keep using widgets even though this isn't part f1 form
                    1, Calendar2.IDEAL_N_OPTIONS, idealTimeN - 1, //-1 so index=0 .. 99
                    "onChange='f1.timeN.value=this.options[this.selectedIndex].text; " +
                              "mySubmit(true);'"));

                //timeUnits
                writer.write(
                widgets.select("timeUnits", 
                    timeRangeTip, //keep using widgets even though this isn't part f1 form
                    1, Calendar2.IDEAL_UNITS_OPTIONS, idealTimeUnits, 
                    "onChange='f1.timeUnits.value=this.options[this.selectedIndex].text; " +
                              "mySubmit(true);'"));


                //make idealized current centered time period
                GregorianCalendar idMinGc = Calendar2.roundToIdealGC(timeCenter, idealTimeN, idealTimeUnits);
                //if it rounded to later time period, shift to earlier time period
                if (idMinGc.getTimeInMillis() / 1000 > timeCenter)
                    idMinGc.add(Calendar2.IDEAL_UNITS_FIELD[idealTimeUnits], -idealTimeN);                                 
                GregorianCalendar idMaxGc = Calendar2.newGCalendarZulu(idMinGc.getTimeInMillis());
                    idMaxGc.add(Calendar2.IDEAL_UNITS_FIELD[idealTimeUnits], idealTimeN);                                 

                //time backward
                {
                    //make idealized beginning time
                    GregorianCalendar tidMinGc = Calendar2.roundToIdealGC(timeFirst, idealTimeN, idealTimeUnits);
                    //if it rounded to later time period, shift to earlier time period
                    if (tidMinGc.getTimeInMillis() / 1000 > timeFirst)
                        tidMinGc.add(Calendar2.IDEAL_UNITS_FIELD[idealTimeUnits], -idealTimeN);                                 
                    GregorianCalendar tidMaxGc = Calendar2.newGCalendarZulu(tidMinGc.getTimeInMillis());
                        tidMaxGc.add(Calendar2.IDEAL_UNITS_FIELD[idealTimeUnits], idealTimeN);                                 

                    //always show LL button if idealTime is different from current selection
                    double idRange = (tidMaxGc.getTimeInMillis() - tidMinGc.getTimeInMillis()) / 1000;
                    double ratio = (timeStop - timeStart) / idRange;                    
                    if (timeStart > timeFirst || ratio < 0.99 || ratio > 1.01) {
                        writer.write(
                        "&nbsp;&nbsp;\n" +
                        HtmlWidgets.htmlTooltipImage( 
                            EDStatic.imageDirUrl(loggedInAs) + "arrowLL.gif", 
                            "Go to the dataset's first " + timeRangeString + " and redraw the graph." +
                                timesVary,  
                            "align=\"top\" " +
                            "onMouseUp='f1.start" + timeIndex + ".value=\"" + Calendar2.formatAsISODateTimeT(tidMinGc)  + "Z\"; " +
                                       "f1.stop"  + timeIndex + ".value=\"" + Calendar2.formatAsISODateTimeT(tidMaxGc)  + "Z\"; " +
                                       "mySubmit(true);'"));
                    } else {
                        writer.write(timeGap);
                    }

                    //idealized (rounded) time shift to left 
                    //(show based on more strict circumstances than LL (since relative shift, not absolute))
                    if (timeStart > timeFirst) {
                        idMinGc.add(Calendar2.IDEAL_UNITS_FIELD[idealTimeUnits], -idealTimeN);                                 
                        idMaxGc.add(Calendar2.IDEAL_UNITS_FIELD[idealTimeUnits], -idealTimeN);                                 
                        writer.write(
                        "&nbsp;&nbsp;\n" +
                        HtmlWidgets.htmlTooltipImage( 
                            EDStatic.imageDirUrl(loggedInAs) + "minus.gif", 
                            "Shift time backward " + timeRangeString + " and redraw the graph." +
                                timesVary,  
                            "align=\"top\" " +
                            "onMouseUp='f1.start" + timeIndex + ".value=\"" + Calendar2.formatAsISODateTimeT(idMinGc)  + "Z\"; " +
                                       "f1.stop"  + timeIndex + ".value=\"" + Calendar2.formatAsISODateTimeT(idMaxGc)  + "Z\"; " +
                                       "mySubmit(true);'"));
                        idMinGc.add(Calendar2.IDEAL_UNITS_FIELD[idealTimeUnits], idealTimeN);                                 
                        idMaxGc.add(Calendar2.IDEAL_UNITS_FIELD[idealTimeUnits], idealTimeN);                                 

                    } else {
                        writer.write(timeGap);
                    }
                }

                //time forward
                {
                    //show right button   
                    //(show based on more strict circumstances than RR (since relative shift, not absolute))
                    if (timeStop < timeLast) {  
                        //idealized (rounded) time shift to right
                        idMinGc.add(Calendar2.IDEAL_UNITS_FIELD[idealTimeUnits], idealTimeN);                                 
                        idMaxGc.add(Calendar2.IDEAL_UNITS_FIELD[idealTimeUnits], idealTimeN);                                 
                        writer.write(
                        "&nbsp;&nbsp;\n" +
                        HtmlWidgets.htmlTooltipImage( 
                            EDStatic.imageDirUrl(loggedInAs) + "plus.gif", 
                            "Shift time forward " + timeRangeString + " and redraw the graph." +
                                timesVary,  
                            "align=\"top\" " +
                            "onMouseUp='f1.start" + timeIndex + ".value=\"" + Calendar2.formatAsISODateTimeT(idMinGc)  + "Z\"; " +
                                       "f1.stop"  + timeIndex + ".value=\"" + Calendar2.formatAsISODateTimeT(idMaxGc)  + "Z\"; " +
                                       "mySubmit(true);'"));
                        idMinGc.add(Calendar2.IDEAL_UNITS_FIELD[idealTimeUnits], -idealTimeN);                                 
                        idMaxGc.add(Calendar2.IDEAL_UNITS_FIELD[idealTimeUnits], -idealTimeN);                                 
                    } else {
                        writer.write(timeGap);
                    }

                    //make idealized end time
                    GregorianCalendar tidMaxGc = Calendar2.roundToIdealGC(timeLast, idealTimeN, idealTimeUnits);
                    //if it rounded to earlier time period, shift to later time period
                    if (tidMaxGc.getTimeInMillis() / 1000 < timeLast)
                        tidMaxGc.add(Calendar2.IDEAL_UNITS_FIELD[idealTimeUnits], idealTimeN);                                 
                    GregorianCalendar tidMinGc = Calendar2.newGCalendarZulu(tidMaxGc.getTimeInMillis());
                        tidMinGc.add(Calendar2.IDEAL_UNITS_FIELD[idealTimeUnits], -idealTimeN);                                 

                    //end time
                    //always show RR button if idealTime is different from current selection
                    double idRange = (tidMaxGc.getTimeInMillis() - tidMinGc.getTimeInMillis()) / 1000;
                    double ratio = (timeStop - timeStart) / idRange;                    
                    if (timeStop < timeLast || ratio < 0.99 || ratio > 1.01) {
                        writer.write(
                        "&nbsp;&nbsp;\n" +
                        HtmlWidgets.htmlTooltipImage( 
                            EDStatic.imageDirUrl(loggedInAs) + "arrowRR.gif", 
                            "Go to the dataset's last " + timeRangeString + " and redraw the graph." +
                                timesVary,  
                            "align=\"top\" " +
                            "onMouseUp='f1.start" + timeIndex + ".value=\"" + Calendar2.formatAsISODateTimeT(tidMinGc)  + "Z\"; " +
                                       "f1.stop"  + timeIndex + ".value=\"" + Calendar2.formatAsISODateTimeT(tidMaxGc)  + "Z\"; " +
                                       "mySubmit(true);'"));
                    } else {
                        writer.write(timeGap);
                    }
                }

                //trailing <br>
                writer.write("<br>");

            }

            //show the graph
            String aQuery = XML.encodeAsHTML(graphQuery.toString()); 
            if (verbose) 
                String2.log("graphQuery=" + graphQuery);
            //don't use \n for the following lines
            if (zoomLatLon) writer.write(
                "<a href=\"" + tErddapUrl + "/griddap/" + datasetID+ ".graph?" + aQuery + 
                "&amp;.click=\">"); //if user clicks on image, browser adds "?x,y" to url
            writer.write(
                "<img " + (zoomLatLon? "ismap " : "") + 
                    "width=\"" + EDStatic.imageWidths[1] + 
                    "\" height=\"" + EDStatic.imageHeights[1] + "\" " +
                    "alt=\"" + EDStatic.patientYourGraph + "\" " +
                    "src=\"" +  tErddapUrl + "/griddap/" + datasetID+ ".png?" + aQuery + "\">");
            if (zoomLatLon) 
                writer.write("</a>");

            //*** end of right half of big table
            writer.write("\n</td></tr></table>\n");


            //*** Things you can do with graphs
            writer.write("<br>&nbsp;\n" +
                "<hr>\n" +
                "<h2><a name=\"uses\">Things</a> You Can Do With Your Graphs</h2>\n" +
                "Well, you can do anything you want with your graphs, of course.  But some things you might not have considered are:\n" +
                "<ul>\n" +
                "<li>Web page authors can \n" +
                "  <a href=\"http://coastwatch.pfeg.noaa.gov/erddap/images/embed.html\">embed a graph of the latest data in a web page</a> \n" +
                "  using HTML &lt;img&gt; tags.\n" +
                "<li>Anyone can use <a href=\"" + tErddapUrl + "/slidesorter.html\">Slide Sorter</a> \n" +
                "  to build a personal web page that displays graphs of the latest data, each in its own, draggable slide.\n" +
                "<li>Anyone can use or make\n" +
                "  <a href=\"http://coastwatch.pfeg.noaa.gov/erddap/images/gadgets/GoogleGadgets.html\">Google Gadgets</a>\n" +
                "  to display images with the latest data on their iGoogle home page. \n" +
                "  <br>&nbsp;\n" +
                "</ul>\n" +
                "\n");


            //end of document
            writer.flush(); //Steve Souder says: the sooner you can send some html to user, the better
            writer.write("<hr>\n");
            writer.write("<h2>The Dataset Attribute Structure (.das) for this Dataset</h2>\n" +
                "<pre>\n");
            writeDAS("/griddap/" + datasetID + ".das", "", writer, true); //useful so search engines find all relevant words
            writer.write("</pre>\n");
            writer.write("<br>&nbsp;\n");
            writer.write("<hr>\n");
            writeGeneralDapHtmlInstructions(tErddapUrl, writer, false); 
            if (EDStatic.displayDiagnosticInfo) 
                EDStatic.writeDiagnosticInfoHtml(writer);

            //the javascript for the sliders
            writer.write(widgets.sliderScript(sliderFromNames, sliderToNames, 
                sliderNThumbs, sliderUserValuesCsvs, 
                sliderInitFromPositions, sliderInitToPositions, EDV.SLIDER_PIXELS - 1));
        } catch (Throwable t) {
            writer.write(EDStatic.htmlForException(t));
        }

        writer.write(EDStatic.endBodyHtml(tErddapUrl));
        writer.write("\n</html>\n");

        //essential
        writer.flush(); 
    }


    /**
     * This gets the data for the userDapQuery and writes the grid data to the 
     * outputStream in the DODS ASCII data format, which is not defined in OPeNDAP 2.0,
     * but which is very close to saveAsDODS below.
     * This mimics http://192.168.31.18/thredds/dodsC/satellite/MH/chla/8day.asc?MHchla[1477][0][2080:2:2082][4940] .
     * 
     * @param requestUrl
     * @param userDapQuery an OPeNDAP DAP-style query string, still percentEncoded (shouldn't be null).
     *   e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160]
     * @param outputStreamSource the source of an outputStream (usually already 
     *   buffered) to receive the results.
     *   At the end of this method the outputStream is flushed, not closed.
     * @throws Throwable  if trouble. 
     */
    public void saveAsAsc(String requestUrl, String userDapQuery, OutputStreamSource outputStreamSource) 
        throws Throwable {

        if (reallyVerbose) String2.log("  EDDGrid.saveAsAsc"); 
        long time = System.currentTimeMillis();

        //handle axis request
        if (isAxisDapQuery(userDapQuery)) {
            //get AxisDataAccessor first, in case of error when parsing query
            AxisDataAccessor ada = new AxisDataAccessor(this, requestUrl, userDapQuery);
            int nRAV = ada.nRequestedAxisVariables();

            //write the dds    //OPeNDAP 2.0, 7.2.3
            saveAsDDS(requestUrl, userDapQuery, outputStreamSource);  

            //write the connector  //OPeNDAP 2.0, 7.2.3
            OutputStreamWriter writer = new OutputStreamWriter(
                outputStreamSource.outputStream("ISO-8859-1"),
                "ISO-8859-1"); //OPeNDAP 2.0 section 3.2.3 says US-ASCII (7bit), so might as well go for compatible common 8bit
            writer.write(
                "---------------------------------------------" + OpendapHelper.EOL + 
                "Data:" + OpendapHelper.EOL); //see EOL definition for comments

            //write the data  //OPeNDAP 2.0, 7.3.2.4
            for (int av = 0; av < nRAV; av++) {
                writer.write(ada.axisVariables(av).destinationName() +
                    "[" + ada.axisValues(av).size() + "]" + OpendapHelper.EOL); 
                writer.write(ada.axisValues(av).toString());
                writer.write(OpendapHelper.EOL);
            }

            writer.flush(); //essential

            //diagnostic
            if (reallyVerbose) String2.log("  EDDGrid.saveAsAsc axis done.\n");
            return;
        }

        //get full gridDataAccessor first, in case of error when parsing query
        GridDataAccessor gridDataAccessor = new GridDataAccessor(this, 
            requestUrl, userDapQuery, true, false);  //rowMajor, convertToNaN
        String arrayQuery = buildDapArrayQuery(gridDataAccessor.constraints());
        EDV tDataVariables[] = gridDataAccessor.dataVariables();
        boolean entireDataset = userDapQuery.trim().length() == 0;

        //get partial gridDataAccessor, to test for size error
        GridDataAccessor gda = new GridDataAccessor(this, requestUrl,
            tDataVariables[0].destinationName() + arrayQuery, 
            true, false);   //rowMajor, convertToNaN
        long tSize = gda.totalIndex().size();
        EDStatic.ensureArraySizeOkay(tSize, "OPeNDAP limit");

        //write the dds    //OPeNDAP 2.0, 7.2.3
        saveAsDDS(requestUrl, userDapQuery, outputStreamSource);  

        //write the connector  //OPeNDAP 2.0, 7.2.3
        OutputStreamWriter writer = new OutputStreamWriter(
            outputStreamSource.outputStream("ISO-8859-1"),
            "ISO-8859-1"); //OPeNDAP 2.0 section 3.2.3 says US-ASCII (7bit), so might as well go for compatible common 8bit
        writer.write("---------------------------------------------" + 
            OpendapHelper.EOL); //see EOL definition for comments 

        //write the axis variables
        int nAxisVariables = axisVariables.length;
        if (entireDataset) {
            //send the axis data
            int tShape[] = gridDataAccessor.totalIndex().shape();
            for (int av = 0; av < nAxisVariables; av++) {
                writer.write(axisVariables[av].destinationName() +
                    "[" + tShape[av] + "]" + OpendapHelper.EOL); //see EOL definition for comments
                writer.write(gridDataAccessor.axisValues[av].toString());
                writer.write(OpendapHelper.EOL); //see EOL definition for comments
            }
            writer.write(OpendapHelper.EOL); //see EOL definition for comments
        }

        //write the data  //OPeNDAP 2.0, 7.3.2.4
        //write elements of the array, in dds order
        int nDataVariables = tDataVariables.length;
        for (int dv = 0; dv < nDataVariables; dv++) {
            String dvDestName = tDataVariables[dv].destinationName();
            gda = new GridDataAccessor(this, requestUrl, dvDestName + arrayQuery, 
                true, false);   //rowMajor, convertToNaN
            int shape[] = gda.totalIndex().shape();
            int current[] = gda.totalIndex().getCurrent();

            //identify the array
            writer.write(dvDestName + "." + dvDestName);
            int nAv = axisVariables.length;
            for (int av = 0; av < nAv; av++)
                writer.write("[" + shape[av] + "]");

            //send the array data
            while (gda.increment()) {
                //if last dimension's value is 0, start a new row
                if (current[nAv - 1] == 0) {
                    writer.write(OpendapHelper.EOL); //see EOL definition for comments
                    for (int av = 0; av < nAv - 1; av++)
                        writer.write("[" + current[av] + "]");
                }
                writer.write(", " + gda.getDataValueAsString(0));
            }

            //send the axis data
            for (int av = 0; av < nAxisVariables; av++) {
                writer.write(OpendapHelper.EOL + OpendapHelper.EOL + dvDestName + "." + axisVariables[av].destinationName() +
                    "[" + shape[av] + "]" + OpendapHelper.EOL); //see EOL definition for comments
                writer.write(gda.axisValues[av].toString());
            }
            writer.write(OpendapHelper.EOL); //see EOL definition for comments
        }

        writer.flush(); //essential

        //diagnostic
        if (reallyVerbose)
            String2.log("  EDDGrid.saveAsAsc done. TIME=" + 
                (System.currentTimeMillis() - time) + "\n");
    }

    /**
     * This writes the dataset data attributes (DAS) to the outputStream.
     * It is always the same regardless of the userDapQuery.
     * (That's what THREDDS does -- OPeNDAP 2.0 7.2.1 is vague.
     *  THREDDs doesn't even object if userDapQuery is invalid.)
     * See writeDAS().
     *
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     * @param userDapQuery the part of the user's request
     *    after the '?', still percentEncoded (shouldn't be null).
     * @param outputStreamSource the source of an outputStream (usually already 
     *   buffered) to receive the results.
     *   At the end of this method the outputStream is flushed, not closed.
     * @throws Throwable  if trouble. 
     */
    public void saveAsDAS(String requestUrl, String userDapQuery, 
        OutputStreamSource outputStreamSource) throws Throwable {

        if (reallyVerbose) String2.log("  EDDGrid.saveAsDAS"); 
        long time = System.currentTimeMillis();

        //get the modified outputStream
        Writer writer = new OutputStreamWriter(outputStreamSource.outputStream("ISO-8859-1"),
            "ISO-8859-1"); //OPeNDAP 2.0 section 3.2.3 says US-ASCII (7bit), so might as well go for compatible common 8bit

        //write the DAS
        writeDAS(File2.forceExtension(requestUrl, ".das"), "", writer, false);

        //diagnostic
        if (reallyVerbose)
            String2.log("  EDDGrid.saveAsDAS done. TIME=" + 
                (System.currentTimeMillis() - time) + "\n");
    }

    /**
     * This writes the dataset data attributes (DAS) to the outputStream.
     * It is always the same regardless of the userDapQuery (except for history).
     * (That's what THREDDS does -- OPeNDAP 2.0 7.2.1 is vague.
     *  THREDDs doesn't even object if userDapQuery is invalid.)
     * <p>
     * E.g., <pre>
Attributes {
    altitude {
        Float32 actual_range 0.0, 0.0;
        Int32 fraction_digits 0;
        String long_name "Altitude";
        String standard_name "altitude";
        String units "m";
        String axis "Z";
    }
    NC_GLOBAL {
        ....
    }
}
</pre> 
     *
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     * @param userDapQuery the part of the user's request after the '?', 
     *    still percentEncoded (shouldn't be null).  (Affects history only.)
     * @param writer a Writer.
     *   At the end of this method the Writer is flushed, not closed.
     * @param encodeAsHtml if true, characters like &lt; are converted to their 
     *    character entities.
     * @throws Throwable  if trouble. 
     */
    public void writeDAS(String requestUrl, String userDapQuery, 
        Writer writer, boolean encodeAsHtml) throws Throwable {

        int nAxisVariables = axisVariables.length;
        int nDataVariables = dataVariables.length;
        writer.write("Attributes {" + OpendapHelper.EOL); //see EOL definition for comments
        for (int av = 0; av < nAxisVariables; av++) 
            OpendapHelper.writeToDAS(axisVariables[av].destinationName(),
                axisVariables[av].combinedAttributes(), writer, encodeAsHtml);
        for (int dv = 0; dv < nDataVariables; dv++) 
            OpendapHelper.writeToDAS(dataVariables[dv].destinationName(),
                dataVariables[dv].combinedAttributes(), writer, encodeAsHtml);

        //how do global attributes fit into opendap view of attributes?
        Attributes gAtts = new Attributes(combinedGlobalAttributes); //a copy

        //fix up global attributes  (always to a local COPY of global attributes)
        EDD.addToHistory(gAtts, publicSourceUrl());
        EDD.addToHistory(gAtts, EDStatic.baseUrl + requestUrl + 
            (userDapQuery == null || userDapQuery.length() == 0? "" : "?" + userDapQuery));

        OpendapHelper.writeToDAS(
            "NC_GLOBAL", //.nc files say NC_GLOBAL; ncBrowse and netcdf-java treat NC_GLOBAL as special case
            gAtts, writer, encodeAsHtml);
        writer.write("}" + OpendapHelper.EOL); //see EOL definition for comments
        writer.flush(); //essential

    }

    /**
     * This gets the data for the userDapQuery and writes the grid data 
     * structure (DDS) to the outputStream.
     * E.g. <pre>
  Dataset {
      Float64 lat[lat = 180];
      Float64 lon[lon = 360];
      Float64 time[time = 404];
      Grid {
       ARRAY:
          Int32 sst[time = 404][lat = 180][lon = 360];
       MAPS:
          Float64 time[time = 404];
          Float64 lat[lat = 180];
          Float64 lon[lon = 360];
      } sst;
  } weekly;
 </pre>
     * 
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     * @param userDapQuery an OPeNDAP DAP-style query string, still percentEncoded (shouldn't be null).
     *   e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160]
     * @param outputStreamSource the source of an outputStream (usually already 
     *   buffered) to receive the results.
     *   At the end of this method the outputStream is flushed, not closed.
     * @throws Throwable  if trouble. 
     */
    public void saveAsDDS(String requestUrl, String userDapQuery, 
        OutputStreamSource outputStreamSource) throws Throwable {

        if (reallyVerbose) String2.log("  EDDGrid.saveAsDDS"); 
        long time = System.currentTimeMillis();

        //handle axisDapQuery
        if (isAxisDapQuery(userDapQuery)) {
            //get axisDataAccessor first, in case of error when parsing query
            AxisDataAccessor ada = new AxisDataAccessor(this, requestUrl, userDapQuery);
            int nRAV = ada.nRequestedAxisVariables();

            //then get the modified outputStream
            Writer writer = new OutputStreamWriter(outputStreamSource.outputStream("ISO-8859-1"),
                "ISO-8859-1"); //OPeNDAP 2.0 section 3.2.3 says US-ASCII (7bit), so might as well go for compatible common 8bit

            writer.write("Dataset {" + OpendapHelper.EOL); //see EOL definition for comments
            for (int av = 0; av < nRAV; av++) {
                String destName = ada.axisVariables(av).destinationName();
                PrimitiveArray apa = ada.axisValues(av);
                writer.write("  " + //e.g., Float64 time[time = 404];
                    OpendapHelper.getAtomicType(apa.elementClass()) + " " + 
                    destName + "[" + destName + " = " + apa.size() + "];" + OpendapHelper.EOL); 
            }
            //Thredds recently started using urlEncoding the final name (and other names?). I don't.
            //Update: I think they undid this change.
            writer.write("} " + datasetID + ";" + OpendapHelper.EOL); 
            writer.flush(); //essential

            //diagnostic
            if (reallyVerbose) String2.log("  EDDGrid.saveAsDDS axis done.");
            return;
        }

        //get gridDataAccessor first, in case of error when parsing query
        GridDataAccessor gridDataAccessor = new GridDataAccessor(this, 
            requestUrl, userDapQuery, true, false);   //rowMajor, convertToNaN
        boolean entireDataset = userDapQuery == null || SSR.percentDecode(userDapQuery).trim().length() == 0;

        //then get the modified outputStream
        Writer writer = new OutputStreamWriter(outputStreamSource.outputStream("ISO-8859-1"),
            "ISO-8859-1"); //OPeNDAP 2.0 section 3.2.3 says US-ASCII (7bit), so might as well go for compatible common 8bit

        int nAxisVariables = axisVariables.length;
        int nDataVariables = gridDataAccessor.dataVariables().length;
        writer.write("Dataset {" + OpendapHelper.EOL); //see EOL definition for comments
        String arrayDims[] = new String[nAxisVariables]; //each e.g., [time = 404]
        String dims[] = new String[nAxisVariables];        //each e.g., Float64 time[time = 404];
        StringBuilder allArrayDims = new StringBuilder();
        for (int av = 0; av < nAxisVariables; av++) {
            PrimitiveArray apa = gridDataAccessor.axisValues(av);
            arrayDims[av] = "[" + axisVariables[av].destinationName() + " = " + 
                apa.size() + "]";
            dims[av] = OpendapHelper.getAtomicType(apa.elementClass()) +
                " " + axisVariables[av].destinationName() + arrayDims[av] + ";" + OpendapHelper.EOL;
            allArrayDims.append(arrayDims[av]);
            if (entireDataset) 
                writer.write("  " + dims[av]);
        }
        for (int dv = 0; dv < nDataVariables; dv++) {
            String dvName = gridDataAccessor.dataVariables()[dv].destinationName();
            writer.write("  GRID {" + OpendapHelper.EOL);
            writer.write("    ARRAY:" + OpendapHelper.EOL); 
            writer.write("      " + 
                OpendapHelper.getAtomicType(
                    gridDataAccessor.dataVariables()[dv].destinationDataTypeClass()) +
                " " + dvName + allArrayDims + ";" + OpendapHelper.EOL); 
            writer.write("    MAPS:" + OpendapHelper.EOL); 
            for (int av = 0; av < nAxisVariables; av++) 
                writer.write("      " + dims[av]);
            writer.write("  } " + dvName + ";" + OpendapHelper.EOL); 
        }

        //Thredds recently started using urlEncoding the final name (and other names?).
        //I don't (yet).
        writer.write("} " + datasetID + ";" + OpendapHelper.EOL);
        writer.flush(); //essential

        //diagnostic
        if (reallyVerbose)
            String2.log("  EDDGrid.saveAsDDS done. TIME=" + 
                (System.currentTimeMillis() - time) + "\n");
    }

    /**
     * This gets the data for the userDapQuery and writes the grid data to the 
     * outputStream in the DODS DataDDS format (OPeNDAP 2.0, 7.2.3).
     * 
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     * @param userDapQuery an OPeNDAP DAP-style query string, still percentEncoded (shouldn't be null). 
     *   e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160]
     * @param outputStreamSource the source of an outputStream (usually already 
     *   buffered) to receive the results.
     *   At the end of this method the outputStream is flushed, not closed.
     * @throws Throwable  if trouble. 
     */
    public void saveAsDODS(String requestUrl, String userDapQuery, 
        OutputStreamSource outputStreamSource) throws Throwable {

        if (reallyVerbose) String2.log("  EDDGrid.saveAsDODS"); 
        long time = System.currentTimeMillis();

        //handle axisDapQuery
        if (isAxisDapQuery(userDapQuery)) {
            //get axisDataAccessor first, in case of error when parsing query
            AxisDataAccessor ada = new AxisDataAccessor(this, requestUrl, userDapQuery);
            int nRAV = ada.nRequestedAxisVariables();

            //write the dds    //OPeNDAP 2.0, 7.2.3
            saveAsDDS(requestUrl, userDapQuery, outputStreamSource);  

            //write the connector  //OPeNDAP 2.0, 7.2.3
            //see EOL definition for comments
            OutputStream outputStream = outputStreamSource.outputStream("");
            outputStream.write((OpendapHelper.EOL + "Data:" + OpendapHelper.EOL).getBytes());

            //write the data  //OPeNDAP 2.0, 7.3.2.4
            //write elements of the array, in dds order
            DataOutputStream dos = new DataOutputStream(outputStream);
            for (int av = 0; av < nRAV; av++)
                ada.axisValues(av).externalizeForDODS(dos);
            dos.flush(); //essential

            //diagnostic
            if (reallyVerbose) String2.log("  EDDGrid.saveAsDODS axis done.\n");
            return;
        }

        //get gridDataAccessor first, in case of error when parsing query
        GridDataAccessor gridDataAccessor = new GridDataAccessor(this, 
            requestUrl, userDapQuery, true, false);  //rowMajor, convertToNaN 
        String arrayQuery = buildDapArrayQuery(gridDataAccessor.constraints());
        EDV tDataVariables[] = gridDataAccessor.dataVariables();
        boolean entireDataset = userDapQuery == null || SSR.percentDecode(userDapQuery).trim().length() == 0;

        //get partial gridDataAccessor, in case of size error
        GridDataAccessor gda = new GridDataAccessor(this, requestUrl, 
            tDataVariables[0].destinationName() + arrayQuery, true, false);
        long tSize = gda.totalIndex().size();
        EDStatic.ensureArraySizeOkay(tSize, "OPeNDAP limit");

        //write the dds    //OPeNDAP 2.0, 7.2.3
        saveAsDDS(requestUrl, userDapQuery, outputStreamSource);  

        //write the connector  //OPeNDAP 2.0, 7.2.3
        //see EOL definition for comments
        OutputStream outputStream = outputStreamSource.outputStream("");
        outputStream.write((OpendapHelper.EOL + "Data:" + OpendapHelper.EOL).getBytes()); 

        //make the dataOutputStream
        DataOutputStream dos = new DataOutputStream(outputStream);

        //write the axis variables
        int nAxisVariables = axisVariables.length;
        if (entireDataset) {
            for (int av = 0; av < nAxisVariables; av++) 
                gridDataAccessor.axisValues[av].externalizeForDODS(dos);
        }

        //write the data  //OPeNDAP 2.0, 7.3.2.4
        //write elements of the array, in dds order
        int nDataVariables = tDataVariables.length;
        for (int dv = 0; dv < nDataVariables; dv++) {
            gda = new GridDataAccessor(this, requestUrl, 
                tDataVariables[dv].destinationName() + arrayQuery, 
                true, false);   //rowMajor, convertToNaN
            tSize = gda.totalIndex().size();

            //send the array size (twice)  //OPeNDAP 2.0, 7.3.2.1
            dos.writeInt((int)tSize); //safe since checked above
            dos.writeInt((int)tSize); //safe since checked above

            //send the array data   (Note that DAP doesn't have exact match for some Java data types.)
            Class type = tDataVariables[dv].destinationDataTypeClass();
            if        (type == byte.class  ) {while (gda.increment()) dos.writeByte(gda.getDataValueAsInt(0));
                //pad byte array to 4 byte boundary
                long tn = gda.totalIndex().size();
                while (tn++ % 4 != 0) dos.writeByte(0);
            } else if (type == short.class || //no exact DAP equivalent
                       type == char.class ||  //no exact DAP equivalent
                       type == int.class)    {while (gda.increment()) dos.writeInt(gda.getDataValueAsInt(0));
            } else if (type == float.class ) {while (gda.increment()) dos.writeFloat(gda.getDataValueAsFloat(0));
            } else if (type == long.class ||  //no exact DAP equivalent
                       type == double.class) {while (gda.increment()) dos.writeDouble(gda.getDataValueAsDouble(0));
            } else if (type == String.class) {while (gda.increment()) StringArray.externalizeForDODS(dos, gda.getDataValueAsString(0));
            } else {throw new RuntimeException("Internal error: unsupported source data type=" + 
                PrimitiveArray.elementClassToString(type));
            }
            for (int av = 0; av < nAxisVariables; av++) 
                gridDataAccessor.axisValues[av].externalizeForDODS(dos);

            dos.flush(); 
        }

        dos.flush(); //essential

        //diagnostic
        if (reallyVerbose)
            String2.log("  EDDGrid.saveAsDODS done. TIME=" + 
                (System.currentTimeMillis() - time) + "\n");
    }


    /**
     * This gets the data for the userDapQuery and writes the grid data to the 
     * outputStream in the ESRI ASCII data format.
     * For .esriAsci, dataVariable queries can specify multiple longitude and latitude
     * values, but just one value for other dimensions.
* Currently, the requested lon values can't be below and above 180  
* (below is fine; above is automatically shifted down).
* [future: do more extensive fixup].
     * 
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     * @param userDapQuery an OPeNDAP DAP-style query string, still percentEncoded (shouldn't be null).
     *   e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160]
     * @param outputStreamSource the source of an outputStream (usually already 
     *   buffered) to receive the results.
     *   At the end of this method the outputStream is flushed, not closed.
     * @throws Throwable  if trouble. 
     */
    public void saveAsEsriAscii(String requestUrl, String userDapQuery, 
        OutputStreamSource outputStreamSource) throws Throwable {

        if (reallyVerbose) String2.log("  EDDGrid.saveAsEsriAscii"); 
        long time = System.currentTimeMillis();

        //handle axis request
        if (isAxisDapQuery(userDapQuery)) 
            throw new SimpleException("Error: " +
                "The ESRI .asc format is for latitude longitude data requests only.");

        if (lonIndex < 0 || latIndex < 0) 
            throw new SimpleException("Error: " +
                "The ESRI .asc format is for latitude longitude data requests only.");

        //parse the userDapQuery and get the GridDataAccessor
        //this also tests for error when parsing query
        GridDataAccessor gridDataAccessor = new GridDataAccessor(this, 
            requestUrl, userDapQuery, true, true);  //rowMajor, convertToNaN
        if (gridDataAccessor.dataVariables().length > 1) 
            throw new SimpleException("Error: " +
                "The ESRI .asc format can only handle one data variable.");
        EDV edv = gridDataAccessor.dataVariables()[0];
        Class edvClass = edv.destinationDataTypeClass();
        boolean isIntType = !edvClass.equals(float.class) && !edvClass.equals(double.class) &&
            !edvClass.equals(String.class);
        boolean isFloatType = edvClass.equals(float.class);

        //check that request meets ESRI restrictions
        PrimitiveArray lonPa = null, latPa = null;
        for (int av = 0; av < axisVariables.length; av++) {
            PrimitiveArray avpa = gridDataAccessor.axisValues(av);
            if (av == lonIndex) {
                lonPa = avpa;
            } else if (av == latIndex) {
                latPa = avpa;
            } else {
                if (avpa.size() > 1)
                    throw new SimpleException("Error: " +
                        "For ESRI .asc requests, the " + 
                        axisVariables[av].destinationName() + " dimension's size must be 1."); 
            }
        }

        String error = lonPa.isEvenlySpaced();
        if (error.length() > 0) //size=1 is evenlySpaced
            throw new SimpleException("Error: " +
                "The dataset's longitude values aren't evenly spaced (as required by ESRI's .asc format):\n" + 
                error);
        error = latPa.isEvenlySpaced();
        if (error.length() > 0) //size=1 is evenlySpaced
            throw new SimpleException("Error: " +
                "The dataset's latitude values aren't evenly spaced (as required by ESRI's .asc format):\n" + 
                error);

        int nLon = lonPa.size();
        int nLat = latPa.size();
        double minX = lonPa.getDouble(0);
        double minY = latPa.getDouble(0);
        double maxX = lonPa.getDouble(nLon - 1);
        double maxY = latPa.getDouble(nLat - 1);
        boolean flipX = false;
        boolean flipY = false;
        if (minX > maxX) {flipX = true; double d = minX; minX = maxX; maxX = d; }
        if (minY > maxY) {flipY = true; double d = minY; minY = maxY; maxY = d; }
        double lonSpacing = lonPa.size() <= 1? Double.NaN : (maxX - minX) / (nLon - 1);
        double latSpacing = latPa.size() <= 1? Double.NaN : (maxY - minY) / (nLat - 1);
        if ( Double.isNaN(lonSpacing) && !Double.isNaN(latSpacing)) lonSpacing = latSpacing;
        if (!Double.isNaN(lonSpacing) &&  Double.isNaN(latSpacing)) latSpacing = lonSpacing;
        if ( Double.isNaN(lonSpacing) &&  Double.isNaN(latSpacing)) 
            throw new SimpleException("Error: " +
                "For ESRI .asc requests, the longitude or latitude dimension size must be greater than 1."); 

        //for almostEqual(3, lonSpacing, latSpacing) DON'T GO BELOW 3!!!
        //For example: PHssta has 4096 lon points so spacing is ~.0878
        //But .0878 * 4096 = 359.6   
        //and .0879 * 4096 = 360.0    (just beyond extreme test of 3 digit match)
        //That is unacceptable. So 2 would be abominable.  Even 3 is stretching the limits.
        if (!Math2.almostEqual(3, lonSpacing, latSpacing))
            throw new SimpleException("Error: " +
                "For ESRI .asc requests, the longitude spacing (" + 
                lonSpacing + ") must equal the latitude spacing (" + latSpacing + ").");
        if (minX < 180 && maxX > 180)
            throw new SimpleException("Error: " +
                "For ESRI .asc requests, the longitude values can't be below and above 180.");
        double lonAdjust = lonPa.getDouble(0) >= 180? -360 : 0;
        if (minX + lonAdjust < -180 || maxX + lonAdjust > 180)
            throw new SimpleException("Error: " +
                "For ESRI.asc requests, the adjusted longitude values (" + 
                (minX + lonAdjust) + " to " + (maxX + lonAdjust) + ") must be between -180 and 180.");

        //request is ok and compatible with ESRI .asc!

        //complications:
        //* lonIndex and latIndex can be in any position in axisVariables.
        //* ESRI .asc wants latMajor (that might be rowMajor or columnMajor), 
        //   and TOP row first!
        //The simplest solution is to save all data to temp file,
        //then read values as needed from file and write to writer.

        //make the GridDataRandomAccessor
        GridDataRandomAccessor gdra = new GridDataRandomAccessor(gridDataAccessor);
        int current[] = gridDataAccessor.totalIndex().getCurrent(); //the internal object that changes

        //then get the writer
        //???!!! ISO-8859-1 is a guess. I found no specification.
        OutputStreamWriter writer = new OutputStreamWriter(
            outputStreamSource.outputStream("ISO-8859-1"), "ISO-8859-1"); 

        //ESRI .asc doesn't like NaN
        double dmv = edv.safeDestinationMissingValue();
        String NaNString = Double.isNaN(dmv)? "-9999999" : //good for int and floating data types    
            dmv == Math2.roundToLong(dmv)? "" + Math2.roundToLong(dmv) :
            "" + dmv;

        //write the data
        writer.write("ncols " + nLon + "\n");
        writer.write("nrows " + nLat + "\n");
        //???!!! ERD always uses centered, but others might need was xllcorner yllcorner
        writer.write("xllcenter " + (minX + lonAdjust) + "\n"); 
        writer.write("yllcenter " + minY + "\n"); 
        //ArcGIS forces cellsize to be square; see test above
        writer.write("cellsize " + latSpacing + "\n"); 
        writer.write("nodata_value " + NaNString + "\n");

        //write values from row to row, top to bottom
        Arrays.fill(current, 0);  //manipulate indices in current[]
        for (int tLat = 0; tLat < nLat; tLat++) {
            current[latIndex] = flipY? tLat : nLat - tLat -1;
            for (int tLon = 0; tLon < nLon; tLon++) {
                current[lonIndex] = flipX? nLon - tLon - 1 : tLon;
                double d = gdra.getDataValueAsDouble(current, 0);
                if (Double.isNaN(d)) writer.write(NaNString);
                else if (isIntType)  writer.write("" + Math2.roundToLong(d));
                else if (isFloatType)writer.write("" + (float)d); //it isn't NaN
                else                 writer.write("" + d);
                writer.write(tLon == nLon - 1? '\n' : ' ');
            }
        }
        gdra.closeAndDelete();

        writer.flush(); //essential

        //diagnostic
        if (reallyVerbose)
            String2.log("  EDDGrid.saveAsEsriAscii done. TIME=" + 
                (System.currentTimeMillis() - time) + "\n");
    }


    /**
     * This saves the requested data (must be lat- lon-based data only)  
     * as a grayscale GeoTIFF file.
     * For .geotiff, dataVariable queries can specify multiple longitude and latitude
     * values, but just one value for other dimensions.
     * GeotiffWriter requires that lons are +/-180 (because ESRI only accepts that range).
* Currently, the lons in the request can't be below and above 180  
* (below is fine; above is automatically shifted down).
* [future: do more extensive fixup].
     *
     * <p>javaDoc for the netcdf GeotiffWriter class isn't in standard javaDocs.
     * Try https://www.unidata.ucar.edu/software/netcdf-java/v2.2.20/javadocAll/ucar/nc2/geotiff/GeotiffWriter.html
     * or search Google for GeotiffWriter. 
     *
     * <p>Grayscale GeoTIFFs may not be very colorful, but they have an advantage
     * over color GeoTIFFs: the clear correspondence of the gray level of each pixel 
     * (0 - 255) to the original data allows programs to reconstruct the 
     * original data values, something that is not possible with color GeoTIFFS.
     *
     * @param userDapQuery an OPeNDAP DAP-style query string, still percentEncoded (shouldn't be null). 
     *   e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160]
     * @param outputStreamSource the source of an outputStream (usually already 
     *   buffered) to receive the results.
     *   At the end of this method the outputStream is flushed, not closed.
     * @param directory with a slash at the end in which to cache the file
     * @param fileName The file name with out the extension (e.g., myFile).
     *    The extension ".tif" will be added to create the output file name.
     * @return true of written ok; false if exception occurred (and written on image)
     * @throws Throwable 
     */
    public boolean saveAsGeotiff(String requestUrl, String userDapQuery, OutputStreamSource outputStreamSource,
        String directory, String fileName) throws Throwable {

        if (reallyVerbose) String2.log("Grid.saveAsGeotiff " + fileName);
        long time = System.currentTimeMillis();

        //handle axis request
        if (isAxisDapQuery(userDapQuery)) 
            throw new SimpleException("Error: " +
                "The GeoTIFF format is for latitude longitude data requests only.");

        //lon and lat are required; time is not required
        if (lonIndex < 0 || latIndex < 0) 
            throw new SimpleException("Error: " +
                "The GeoTIFF format is for latitude longitude data requests only.");

        //force to be pm180
        //makeLonPM180(true);

        //parse the userDapQuery and get the GridDataAccessor
        //this also tests for error when parsing query
        GridDataAccessor gridDataAccessor = new GridDataAccessor(this, 
            requestUrl, userDapQuery, true, true);  //rowMajor, convertToNaN
        if (gridDataAccessor.dataVariables().length > 1) 
            throw new SimpleException("Error: " +
                "The GeoTIFF format can only handle one data variable.");
        EDV edv = gridDataAccessor.dataVariables()[0];
        String dataName = edv.destinationName();

        //check that request meets ESRI restrictions

        //The geotiffWriter just throws non-helpful error messages if these requirements aren't met.
        PrimitiveArray lonPa = null, latPa = null;
        double minX = Double.NaN, maxX = Double.NaN, minY = Double.NaN, maxY = Double.NaN,
            lonAdjust = 0, lonSpacing = Double.NaN, latSpacing = Double.NaN;
        for (int av = 0; av < axisVariables.length; av++) {
            PrimitiveArray avpa = gridDataAccessor.axisValues(av);
            if (av == lonIndex) {
                lonPa = avpa;
                String error = lonPa.isEvenlySpaced();
                if (error.length() > 0) //size=1 is evenlySpaced
                    throw new SimpleException("Error: " +
                        "The dataset's longitude values aren't evenly spaced " +
                        "(as required by the GeoTIFF format):\n" + error);
                minX = lonPa.getNiceDouble(0);
                maxX = lonPa.getNiceDouble(lonPa.size() - 1);
                if (lonPa.size() > 1) //first calculate spacing  (may be negative)
                    lonSpacing = (maxX - minX) / (lonPa.size() - 1);
                if (minX > maxX) { //then deal with descending axis values
                    double d = minX; minX = maxX; maxX = d;}
                if (minX < 180 && maxX > 180)
                    throw new SimpleException("Error: " +
                        "For GeoTIFF requests, the dataset's longitude values can't be below and above 180.");
                if (minX >= 180) lonAdjust = -360;
                minX += lonAdjust;
                maxX += lonAdjust;
                if (minX < -180 || maxX > 180)
                    throw new SimpleException("Error: " +
                        "For GeoTIFF requests, the adjusted longitude values (" + 
                        minX + " to " + maxX + ") must be between -180 and 180.");
            } else if (av == latIndex) {
                latPa = avpa;
                String error = latPa.isEvenlySpaced();
                if (error.length() > 0) //size=1 is evenlySpaced
                    throw new SimpleException("Error: " +
                            "The dataset's latitude values aren't evenly spaced (as required by the GeoTIFF format):\n" + error);
                minY = latPa.getNiceDouble(0);
                maxY = latPa.getNiceDouble(latPa.size() - 1);
                if (latPa.size() > 1) //first calculate spacing (may be negative)
                    latSpacing = (maxY - minY) / (latPa.size() - 1);
                if (minY > maxY) { //then deal with descending axis values
                    double d = minY; minY = maxY; maxY = d;}
            } else {
                if (avpa.size() > 1)
                    throw new SimpleException("Error: " +
                        "For GeoTIFF requests, the " + 
                        axisVariables[av].destinationName() + " dimension's size must be 1."); 
            }
        }
        if ( Double.isNaN(lonSpacing) && !Double.isNaN(latSpacing)) lonSpacing = latSpacing;
        if (!Double.isNaN(lonSpacing) &&  Double.isNaN(latSpacing)) latSpacing = lonSpacing;
        if ( Double.isNaN(lonSpacing) &&  Double.isNaN(latSpacing)) 
            throw new SimpleException("Error: " +
                "For GeoTIFF requests, the latitude or longitude dimension size must be greater than 1."); 

        //lon and lat are ascending?    
//future: rearrange the data or get unidata to rearrange the data
        if (lonSpacing < 0 || latSpacing < 0) //see test in EDDGridFromDap.testPmelOscar
            throw new SimpleException("Error: " +
                "For GeoTIFF requests, the latitude and longitude values must be in ascending order."); 

        //commented out, because geotiffWriter allows lonSpacing!=latSpacing
        //if (!Math2.almostEqual(3, lonSpacing, latSpacing))
        //    throw new SimpleException("Error: " +
        //        "For GeoTIFF requests, the longitude spacing (" + 
        //        lonSpacing + ") must equal the latitude spacing (" + latSpacing + ").");

        //request is ok and compatible with geotiffWriter!

        //save the data in a .nc file
        //???I'm pretty sure the axis order can be lon,lat or lat,lon, but not certain.
        //???The GeotiffWriter seems to be detecting lat and lon and reacting accordingly.
        String ncFullName = directory + fileName + "_tiff.nc";  //_tiff is needed because unused axes aren't saved
        if (!File2.isFile(ncFullName))
            saveAsNc(requestUrl, userDapQuery, ncFullName, 
                false, //keepUnusedAxes=false  this is necessary 
                lonAdjust);
        //String2.log(NcHelper.dumpString(ncFullName, false));

        //attempt to create geotif via java netcdf libraries
        LatLonRect latLonRect = new LatLonRect(
            new LatLonPointImpl(minY, minX),
            new LatLonPointImpl(maxY, maxX));
        GeotiffWriter writer = new GeotiffWriter(directory + fileName + ".tif");
        writer.writeGrid(ncFullName, dataName, 0, 0, 
            true, //true=grayscale   color didn't work for me. and see javadocs above.
            latLonRect);
        writer.close();    

        //copy to outputStream
        if (!File2.copy(directory + fileName + ".tif", outputStreamSource.outputStream(""))) {
            //outputStream contentType already set,
            //so I can't go back to html and display error message
            //note than the message is thrown if user cancels the transmission; so don't email to me
            String2.log("Error while transmitting " + fileName + ".tif");
        }

        if (reallyVerbose) String2.log("  Grid.saveAsGeotiff done. TIME=" + 
            (System.currentTimeMillis() - time) + "\n");
        return true;
    }

    /**
     * This writes the data to various types of images.
     * This requires a dataDapQuery (not an axisDapQuery) 
     * where just 1 or 2 of the dimensions be size &gt; 1.
     * One active dimension results in a graph.
     * Two active dimensions results in a map (one active data variable
     *   results in colored graph, two results in vector plot).
     *
     * <p>For transparentPng maps, the longitude and latitude dimensions must be evenly spaced.
     * (Only because the primary client, GoogleEarth assumes they are.)
     * 
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     * @param userDapQuery an OPeNDAP DAP-style query string, still percentEncoded (shouldn't be null). 
     *   e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160]
     * @param dir the directory (on this computer's hard drive) to use for temporary/cache files
     * @param fileName the name for the 'file' (no dir, no extension),
     *    which is used to write the suggested name for the file to the response 
     *    header and is also used to write the [fileTypeName]Info (e.g., .pngInfo) file.
     * @param outputStreamSource
     * @param fileTypeName
     * @return true of written ok; false if exception occurred (and written on image)
     * @throws Throwable  if trouble. 
     */
    public boolean saveAsImage(String loggedInAs, String requestUrl, String userDapQuery, 
        String dir, String fileName, 
        OutputStreamSource outputStreamSource, String fileTypeName) throws Throwable {
        if (reallyVerbose) String2.log("  EDDGrid.saveAsImage query=" + userDapQuery);
        long time = System.currentTimeMillis();

        //determine the image size
        int sizeIndex = 
            fileTypeName.startsWith(".small")? 0 :
            fileTypeName.startsWith(".medium")? 1 :
            fileTypeName.startsWith(".large")? 2 : 1;
        boolean pdf = fileTypeName.toLowerCase().endsWith("pdf");
        boolean png = fileTypeName.toLowerCase().endsWith("png");
        boolean transparentPng = fileTypeName.equals(".transparentPng");
        if (!pdf && !png) 
            throw new SimpleException("Error: " +
                "Unexpected image type=" + fileTypeName);
        int imageWidth, imageHeight;
        if (pdf) {
            imageWidth  = EDStatic.pdfWidths[ sizeIndex]; 
            imageHeight = EDStatic.pdfHeights[sizeIndex];
        } else if (transparentPng) {
            imageWidth  = EDStatic.imageWidths[sizeIndex]; 
            imageHeight = imageWidth;
        } else {
            imageWidth  = EDStatic.imageWidths[sizeIndex]; 
            imageHeight = EDStatic.imageHeights[sizeIndex];
        }
        if (reallyVerbose) String2.log("  sizeIndex=" + sizeIndex + 
            " pdf=" + pdf + " imageWidth=" + imageWidth + " imageHeight=" + imageHeight);
        Object pdfInfo[] = null;
        BufferedImage bufferedImage = null;
        Graphics2D g2 = null;
        Color transparentColor = transparentPng? Color.white : null; //getBufferedImage returns white background
        String drawLegend = LEGEND_BOTTOM;
        int trim = Integer.MAX_VALUE;
        boolean ok = true;

        try {
            //reject axisDapQuery
            if (isAxisDapQuery(userDapQuery)) {
                AxisDataAccessor ada = new AxisDataAccessor(this, requestUrl, userDapQuery);
                throw new SimpleException("Error: " + 
                    "Requests for axis values can't be made into images.");
            }

            //modify the query to get no more data than needed
            StringArray reqDataNames = new StringArray();
            IntArray constraints     = new IntArray();
            parseDataDapQuery(userDapQuery, reqDataNames, constraints, false);

            //for now, just plot first 1 or 2 data variables
            int nDv = reqDataNames.size();
            EDV reqDataVars[] = new EDV[nDv];
            for (int dv = 0; dv < nDv; dv++) 
                reqDataVars[dv] = findDataVariableByDestinationName(reqDataNames.get(dv));

            //extract optional .graphicsSettings from userDapQuery
            //  xRange, yRange, color and colorbar information
            //  title2 -- a prettified constraint string 
            boolean drawLines = false, drawLinesAndMarkers = false, drawMarkers = false, 
                drawSticks = false, drawSurface = false, drawVectors = false; 
            Color color = Color.black;

            //for now, palette values are unset.
            String palette = "";
            String scale = "";
            double paletteMin = Double.NaN;
            double paletteMax = Double.NaN;
            String continuousS = "";
            int nSections = Integer.MAX_VALUE;

            double minX = Double.NaN, maxX = Double.NaN, minY = Double.NaN, maxY = Double.NaN;
            int nVars = 4;
            EDV vars[] = null; //set by .vars or lower
            int axisVarI[] = null, dataVarI[] = null; //set by .vars or lower
            String ampParts[] = getUserQueryParts(userDapQuery); //always at least 1 part (may be "")
            boolean customSize = false;
            int markerType = GraphDataLayer.MARKER_TYPE_FILLED_SQUARE;
            int markerSize = GraphDataLayer.MARKER_SIZE_SMALL;
            double fontScale = 1, vectorStandard = Double.NaN;
            int drawLandAsMask = 0;  //holds the .land setting: 0=default 1=under 2=over
            for (int ap = 0; ap < ampParts.length; ap++) {
                String ampPart = ampParts[ap];

                //.colorBar defaults: palette=""|continuous=C|scale=Linear|min=NaN|max=NaN|nSections=-1
                if (ampPart.startsWith(".colorBar=")) {
                    String pParts[] = String2.split(ampPart.substring(10), '|'); //subparts may be ""; won't be null
                    if (pParts == null) pParts = new String[0];
                    if (pParts.length > 0 && pParts[0].length() > 0) palette = pParts[0];  
                    if (pParts.length > 1 && pParts[1].length() > 0) continuousS = pParts[1].toLowerCase();
                    if (pParts.length > 2 && pParts[2].length() > 0) scale = pParts[2];
                    if (pParts.length > 3 && pParts[3].length() > 0) paletteMin = String2.parseDouble(pParts[3]);
                    if (pParts.length > 4 && pParts[4].length() > 0) paletteMax = String2.parseDouble(pParts[4]);
                    if (pParts.length > 5 && pParts[5].length() > 0) nSections  = String2.parseInt(pParts[5]);
                    if (reallyVerbose)
                        String2.log(".colorBar palette=" + palette + 
                            " continuousS=" + continuousS +
                            " scale=" + scale +                            
                            " min=" + paletteMin + " max=" + paletteMax + 
                            " nSections=" + nSections);                                

                //.color
                } else if (ampPart.startsWith(".color=")) {
                    int iColor = String2.parseInt(ampPart.substring(7));
                    if (iColor < Integer.MAX_VALUE) {
                        color = new Color(iColor);
                        if (reallyVerbose)
                            String2.log(".color=0x" + Integer.toHexString(iColor));
                    }

                //.draw 
                } else if (ampPart.startsWith(".draw=")) {
                    String gt = ampPart.substring(6);
                    //try to set an option to true
                    //ensure others are false in case of multiple .draw
                    drawLines = gt.equals("lines");
                    drawLinesAndMarkers = gt.equals("linesAndMarkers");
                    drawMarkers = gt.equals("markers");
                    drawSticks  = gt.equals("sticks"); 
                    drawSurface = gt.equals("surface");
                    drawVectors = gt.equals("vectors");

                //.font
                } else if (ampPart.startsWith(".font=")) {
                    String pParts[] = String2.split(ampPart.substring(6), '|'); //subparts may be ""; won't be null
                    if (pParts == null) pParts = new String[0];
                    if (pParts.length > 0) fontScale = String2.parseDouble(pParts[0]);
                    fontScale = Double.isNaN(fontScale)? 1 : fontScale < 0.1? 0.1 : fontScale > 10? 10 : fontScale;
                    if (reallyVerbose)
                        String2.log(".font= scale=" + fontScale);

                //.land 
                } else if (ampPart.startsWith(".land=")) {
                    String gt = ampPart.substring(6);
                    if      (gt.equals("under")) drawLandAsMask = 1;
                    else if (gt.equals("over"))  drawLandAsMask = 2;
                    if (reallyVerbose)
                        String2.log(".land= drawLandAsMask=" + drawLandAsMask);

                //.legend 
                } else if (ampPart.startsWith(".legend=")) {
                    drawLegend = ampPart.substring(8);
                    if (!drawLegend.equals(LEGEND_OFF) &&
                        !drawLegend.equals(LEGEND_ONLY))
                        drawLegend = LEGEND_BOTTOM;
                    if (drawLegend.equals(LEGEND_ONLY)) {
                        transparentPng = false; //if it was transparent, it was already png=true, size=1
                        transparentColor = null;
                    }

                //.marker
                } else if (ampPart.startsWith(".marker=")) {
                    String pParts[] = String2.split(ampPart.substring(8), '|'); //subparts may be ""; won't be null
                    if (pParts == null) pParts = new String[0];
                    if (pParts.length > 0) markerType = String2.parseInt(pParts[0]);
                    if (pParts.length > 1) markerSize = String2.parseInt(pParts[1]);
                    if (markerType < 0 || markerType >= GraphDataLayer.MARKER_TYPES.length) 
                        markerType = GraphDataLayer.MARKER_TYPE_FILLED_SQUARE;
                    if (markerSize < 1 || markerSize > 50) markerSize = GraphDataLayer.MARKER_SIZE_SMALL;
                    if (reallyVerbose)
                        String2.log(".marker= type=" + markerType + " size=" + markerSize);

                //.size
                } else if (ampPart.startsWith(".size=")) {
                    customSize = true;
                    String pParts[] = String2.split(ampPart.substring(6), '|'); //subparts may be ""; won't be null
                    if (pParts == null) pParts = new String[0];
                    if (pParts.length > 0) {
                        int w = String2.parseInt(pParts[0]); 
                        if (w > 0 && w < 3000) imageWidth = w;
                    }
                    if (pParts.length > 1) {
                        int h = String2.parseInt(pParts[1]);
                        if (h > 0 && h < 3000) imageHeight = h;
                    }
                    if (reallyVerbose)
                        String2.log(".size=  imageWidth=" + imageWidth + " imageHeight=" + imageHeight);

                //.trim
                } else if (ampPart.startsWith(".trim=")) {
                    trim = String2.parseInt(ampPart.substring(6));
                    if (reallyVerbose)
                        String2.log(".trim " + trim);

                //.vars    request should use this with values or don't use this; no defaults
                } else if (ampPart.startsWith(".vars=")) {
                    vars = new EDV[nVars];
                    axisVarI = new int[nVars]; Arrays.fill(axisVarI, -1);
                    dataVarI = new int[nVars]; Arrays.fill(dataVarI, -1);
                    String pParts[] = String2.split(ampPart.substring(6), '|');
                    for (int p = 0; p < nVars; p++) {
                        if (pParts.length > p && pParts[p].length() > 0) {
                            int ti = String2.indexOf(axisVariableDestinationNames(), pParts[p]);
                            if (ti >= 0) {
                                vars[p] = axisVariables[ti];
                                axisVarI[p] = ti;
                            } else if (reqDataNames.indexOf(pParts[p]) >= 0) {
                                ti = String2.indexOf(dataVariableDestinationNames(), pParts[p]);
                                vars[p] = dataVariables[ti];
                                dataVarI[p] = ti;
                            } else {
                                throw new SimpleException("Query error: " +
                                    ".var #" + p + "=" + pParts[p] + " isn't a valid variable name.");
                            }
                        }
                    }

                //.vec
                } else if (ampPart.startsWith(".vec=")) {
                    vectorStandard = String2.parseDouble(ampPart.substring(5));
                    if (reallyVerbose)
                        String2.log(".vec " + vectorStandard);

                //.xRange   (supported, but currently not created by the Make A Graph form)
                //  prefer set via xVar constratints
                } else if (ampPart.startsWith(".xRange=")) {
                    String pParts[] = String2.split(ampPart.substring(8), '|');
                    if (pParts.length > 0) minX = String2.parseDouble(pParts[0]);
                    if (pParts.length > 1) maxX = String2.parseDouble(pParts[1]);
                    if (reallyVerbose)
                        String2.log(".xRange min=" + minX + " max=" + maxX);

                //.yRange   (supported, as of 2010-10-22 it's on the Make A Graph form)
                //  prefer set via yVar range
                } else if (ampPart.startsWith(".yRange=")) {
                    String pParts[] = String2.split(ampPart.substring(8), '|');
                    if (pParts.length > 0) minY = String2.parseDouble(pParts[0]);
                    if (pParts.length > 1) maxY = String2.parseDouble(pParts[1]);
                    if (reallyVerbose)
                        String2.log(".yRange min=" + minY + " max=" + maxY);

                //just to be clear: ignore any unrecognized .something 
                } else if (ampPart.startsWith(".")) {
                }
            }
            boolean reallySmall = imageWidth < 260; //.smallPng is 240

            //figure out which axes are active (>1 value)
            IntArray activeAxes = new IntArray();
            for (int av = 0; av < axisVariables.length; av++)
                if (constraints.get(av * 3) < constraints.get(av * 3 + 2))
                    activeAxes.add(av);
            int nAAv = activeAxes.size();
            if (nAAv < 1 || nAAv > 2)
                throw new SimpleException("Query error: " +
                    "To draw a graph, either 1 or 2 axes must be active and have a range of values.");

            //figure out / validate graph set up
            //if .draw= was provided...
            int cAxisI = 0, cDataI = 0; //use them up as needed
            if (drawLines) {
                if (vars == null) {
                    vars = new EDV[nVars];
                    for (int v = 0; v < 2; v++) { //get 2 vars
                        if (nAAv > cAxisI)     vars[v] = axisVariables[activeAxes.get(cAxisI++)];
                        else if (nDv > cDataI) vars[v] = reqDataVars[cDataI++]; 
                        else throw new SimpleException("Query error: " +
                            "Too few active axes and/or data variables for .draw=lines.");
                    }
                } else {
                    //vars 0,1 must be valid (any type)
                    if (vars[0] == null) throw new SimpleException("Query error: " +
                        "For .draw=lines, .var #0 is required.");
                    if (vars[1] == null) throw new SimpleException("Query error: " +
                        "For .draw=lines, .var #1 is required.");
                }
                vars[2] = null;
                vars[3] = null;
            } else if (drawLinesAndMarkers || drawMarkers) {
                String what = drawLinesAndMarkers? "linesAndMarkers" : "markers";
                if (vars == null) {
                    vars = new EDV[nVars];
                    for (int v = 0; v < 3; v++) { //get 2 or 3 vars
                        if (nAAv > cAxisI)     vars[v] = axisVariables[activeAxes.get(cAxisI++)];
                        else if (nDv > cDataI) vars[v] = reqDataVars[cDataI++]; 
                        else if (v < 2) throw new SimpleException("Query error: " +
                            "Too few active axes and/or data variables for " +
                            ".draw=" + what + ".");
                    }
                } else {
                    //vars 0,1 must be valid (any type)
                    if (vars[0] == null) throw new SimpleException("Query error: " +
                        "For .draw=" + what + ", .var #0 is required.");
                    if (vars[1] == null) throw new SimpleException("Query error: " +
                        "For .draw=" + what + ", .var #1 is required.");
                }
                vars[3] = null;
            } else if (drawSticks) {
                if (vars == null) {
                    vars = new EDV[nVars];
                    //var0 must be axis
                    if (nAAv > 0) vars[0] = axisVariables[activeAxes.get(cAxisI++)];
                    else throw new SimpleException("Query error: " +
                        ".draw=sticks requires an active axis variable.");
                    //var 1,2 must be data
                    for (int v = 1; v <= 2; v++) { 
                        if (nDv > cDataI) vars[v] = reqDataVars[cDataI++]; 
                        else throw new SimpleException("Query error: " +
                            "Too few data variables to .draw=sticks.");
                    }
                } else {
                    //vars 0 must be axis, 1,2 must be data
                    if (axisVarI[0] < 0) throw new SimpleException("Query error: " +
                        "For .draw=sticks, .var #0 must be an axis variable.");
                    if (dataVarI[1] < 0) throw new SimpleException("Query error: " +
                        "For .draw=sticks, .var #1 must be a data variable.");
                    if (dataVarI[2] < 0) throw new SimpleException("Query error: " +
                        "For .draw=sticks, .var #2 must be a data variable.");
                }
                vars[3] = null;
            } else if (drawSurface) {
                if (vars == null) {
                    vars = new EDV[nVars];
                    //var0,1 must be axis  (currently must be lon,lat)
                    if (activeAxes.indexOf("" + lonIndex) >= 0 &&
                        activeAxes.indexOf("" + latIndex) >= 0) {
                        vars[0] = axisVariables[lonIndex];
                        vars[1] = axisVariables[latIndex];
                    } else throw new SimpleException("Query error: " +
                        ".draw=surface requires active longitude and latitude axes.");
                    //var 2 must be data
                    vars[2] = reqDataVars[cDataI++]; //at least one is valid
                } else {
                    //vars 0 must be axis, 1,2 must be data
                    if (axisVarI[0] != lonIndex || lonIndex < 0) 
                        throw new SimpleException("Query error: " +
                            "For .draw=surface, .var #0 must be longitude.");
                    if (axisVarI[1] != latIndex || latIndex < 0) 
                        throw new SimpleException("Query error: " +
                            "For .draw=surface, .var #1 must be latitude.");
                    if (dataVarI[2] < 0) 
                        throw new SimpleException("Query error: " +
                            "For .draw=surface, .var #2 must be a data variable.");
                }
                vars[3] = null;
            } else if (drawVectors) {
                if (vars == null) {
                    vars = new EDV[nVars];
                    //var0,1 must be axes
                    if (nAAv == 2) {
                        vars[0] = axisVariables[activeAxes.get(0)];
                        vars[1] = axisVariables[activeAxes.get(1)];
                    } else throw new SimpleException("Query error: " +
                        ".draw=vectors requires 2 active axis variables.");
                    //var2,3 must be data
                    if (nDv == 2) {
                        vars[2] = reqDataVars[0];
                        vars[3] = reqDataVars[1];
                    } else throw new SimpleException("Query error: " +
                        ".draw=vectors requires 2 data variables.");
                } else {
                    //vars 0,1 must be axes, 2,3 must be data
                    if (axisVarI[0] < 0) throw new SimpleException("Query error: " +
                        "For .draw=vectors, .var #0 must be an axis variable.");
                    if (axisVarI[1] < 0) throw new SimpleException("Query error: " +
                        "For .draw=vectors, .var #1 must be an axis variable.");
                    if (dataVarI[2] < 0) throw new SimpleException("Query error: " +
                        "For .draw=vectors, .var #2 must be a data variable.");
                    if (dataVarI[3] < 0) throw new SimpleException("Query error: " +
                        "For .draw=vectors, .var #3 must be a data variable.");
                }

            } else if (vars == null) {
                //neither .vars nor .draw were provided
                //detect from OPeNDAP request  (favor linesAndMarkers)
                vars = new EDV[nVars];
                if (nAAv == 0) {
                    throw new SimpleException("Query error: " +
                        "At least 1 axis variable must be active and have a range of values.");
                } else if (nAAv == 1) {
                    drawLinesAndMarkers = true;
                    vars[0] = axisVariables[activeAxes.get(0)];
                    vars[1] = reqDataVars[0];
                    if (nDv > 1) vars[2] = reqDataVars[1];
                } else if (nAAv == 2) {  
                    //currently only if lon lat
                    if (lonIndex >= 0 && latIndex >= 0 &&
                        activeAxes.indexOf(lonIndex) >= 0 &&
                        activeAxes.indexOf(latIndex) >= 0) {
                        vars[0] = axisVariables[lonIndex];
                        vars[1] = axisVariables[latIndex];
                        vars[2] = reqDataVars[0];
                        if (reqDataVars.length >= 2) {
                            //draw vectors
                            drawVectors = true;
                            vars[3] = reqDataVars[1];
                        } else {
                            //draw surface
                            drawSurface = true;
                        }

                    } else throw new SimpleException("Query error: " +
                        "If 2 axes are active, they must be longitude and latitude.");
                } else {
                    throw new SimpleException("Query error: " +
                        "Either 1 or 2 axes must be active and have a range of values.");
                }
            } else {
                //.vars was provided, .draw wasn't
                //look for drawSurface
                if (axisVarI[0] >= 0 &&
                    axisVarI[1] >= 0 &&
                    dataVarI[2] >= 0 &&
                    dataVarI[3] >= 0) {
                    drawVectors = true;

                //look for drawVector(currently must have lon and lat)
                } else if (lonIndex >= 0 && latIndex >= 0 &&
                    activeAxes.indexOf(lonIndex) >= 0 && //lon or lat, in either order
                    activeAxes.indexOf(latIndex) >= 0 &&
                    dataVarI[2] >= 0) {
                    vars[0] = axisVariables[lonIndex]; //force lon 
                    vars[1] = axisVariables[latIndex]; //force lat
                    //vars[2] already set
                    drawSurface = true;
                    vars[3] = null;

                //drawMarker 
                } else {
                    //ensure marker compatible
                    if (axisVarI[0] < 0) 
                        throw new SimpleException("Query error: " +
                            ".var #0 must be an axis variable.");
                    if (axisVarI[1] < 0 && dataVarI[1] < 0) 
                        throw new SimpleException("Query error: " +
                            ".var #1 must be an axis or a data variable.");
                    axisVarI[1] = -1;
                    //var2 may be a dataVar or ""
                    vars[3] = null;
                    drawLinesAndMarkers = true;
                }
            }

            boolean isMap = vars[0] instanceof EDVLonGridAxis &&
                            vars[1] instanceof EDVLatGridAxis;
            boolean xIsTimeAxis = vars[0] instanceof EDVTimeGridAxis;
            boolean yIsTimeAxis = vars[1] instanceof EDVTimeGridAxis;
            int xAxisIndex = String2.indexOf(axisVariableDestinationNames(), vars[0].destinationName());
            int yAxisIndex = String2.indexOf(axisVariableDestinationNames(), vars[1].destinationName());

            //if map or coloredSurface, modify the constraints so as to get only minimal amount of data
            //if 1D graph, no restriction
            int minXIndex = constraints.get(xAxisIndex * 3);
            int maxXIndex = constraints.get(xAxisIndex * 3 + 2);
            EDVGridAxis xAxisVar = axisVariables[xAxisIndex];
            if (Double.isNaN(minX)) minX = xAxisVar.destinationValue(minXIndex).getNiceDouble(0); 
            if (Double.isNaN(maxX)) maxX = xAxisVar.destinationValue(maxXIndex).getNiceDouble(0); 
            if (minX > maxX) {
                double d = minX; minX = maxX; maxX = d;}

            int minYIndex, maxYIndex;
            EDVGridAxis yAxisVar = yAxisIndex >= 0? axisVariables[yAxisIndex] : null;
            double minData = Double.NaN, maxData = Double.NaN;
             
            if (drawSurface || drawVectors) {  
                minYIndex = constraints.get(yAxisIndex * 3);
                maxYIndex = constraints.get(yAxisIndex * 3 + 2);
                if (Double.isNaN(minY)) minY = yAxisVar.destinationValue(minYIndex).getNiceDouble(0); 
                if (Double.isNaN(maxY)) maxY = yAxisVar.destinationValue(maxYIndex).getNiceDouble(0); 
                if (minY > maxY) {
                    double d = minY; minY = maxY; maxY = d;}

                if (transparentPng && drawSurface && !customSize) {

                    //This is the one situation to change imageWidth/Height to ~1 pixel/lon or lat
                    int have = maxXIndex - minXIndex + 1;
                    int stride = constraints.get(xAxisIndex * 3 + 1);
                    imageWidth = DataHelper.strideWillFind(have, stride);
                    //protect against huge .png (and huge amount of data in memory)
                    if (imageWidth > 3601) {
                        stride = DataHelper.findStride(have, 3601);
                        imageWidth = DataHelper.strideWillFind(have, stride);
                        constraints.set(xAxisIndex * 3 + 1, stride);
                        if (reallyVerbose) String2.log("  xStride reduced to stride=" + stride);
                    }
                    have = maxYIndex - minYIndex + 1;
                    stride = constraints.get(yAxisIndex * 3 + 1);
                    imageHeight = DataHelper.strideWillFind(have, stride);
                    if (imageHeight > 1801) {
                        stride = DataHelper.findStride(have, 1801);
                        imageHeight = DataHelper.strideWillFind(have, stride);
                        constraints.set(yAxisIndex * 3 + 1, stride);
                        if (reallyVerbose) String2.log("  yStride reduced to stride=" + stride);
                    }

                } else {
                    //calculate/fix up stride so as to get enough data (but not too much)
                    //find size of map or graph
                    int activeWidth = imageWidth - 50; //decent guess for drawSurface
                    int activeHeight = imageHeight - 75;

                    if (drawVectors) {

                        double maxXY = Math.max(maxX - minX, maxY - minY);
                        double vecInc = SgtMap.suggestVectorIncrement(//e.g. 2 degrees
                            maxXY, Math.max(imageWidth, imageHeight), fontScale);

                        activeWidth  = Math.max(5, Math2.roundToInt((maxX - minX) / vecInc)); //e.g., 20 deg / 2 deg -> 10 
                        activeHeight = Math.max(5, Math2.roundToInt((maxY - minY) / vecInc));

                    } else { //drawSurface;    currently drawSurface is always a map

                        if (transparentPng) {
                            activeWidth = imageWidth;
                            activeHeight = imageHeight;

                        } else {
                            int wh[] = SgtMap.predictGraphSize(fontScale, imageWidth, imageHeight,
                                minX, maxX, minY, maxY);
                            activeWidth = wh[0];
                            activeHeight = wh[1];
                        }
                    } 

                    //calculate/fix up stride so as to get enough data (but not too much)
                    int have = maxXIndex - minXIndex + 1;
                    int stride = DataHelper.findStride(have, activeWidth);
                    constraints.set(xAxisIndex * 3 + 1, stride);
                    if (reallyVerbose) 
                        String2.log("  xStride=" + stride + " activeHeight=" + activeHeight + 
                        " strideWillFind=" + DataHelper.strideWillFind(have, stride));

                    have = maxYIndex - minYIndex + 1;
                    stride = DataHelper.findStride(have, activeHeight);
                    constraints.set(yAxisIndex * 3 + 1, stride);
                    if (reallyVerbose)
                        String2.log("  yStride=" + stride + " activeHeight=" + activeHeight + 
                        " strideWillFind=" + DataHelper.strideWillFind(have, stride));
                }
            } 
         
            //units
            String xUnits = vars[0].units();
            String yUnits = vars[1].units();
            String zUnits = vars[2] == null? null : vars[2].units();
            String tUnits = vars[3] == null? null : vars[3].units();
            xUnits = xUnits == null? "" : " (" + xUnits + ")";
            yUnits = yUnits == null? "" : " (" + yUnits + ")";
            zUnits = zUnits == null? "" : " (" + zUnits + ")";
            tUnits = tUnits == null? "" : " (" + tUnits + ")";

            //get the desctiptive info for the axes with 1 value
            StringBuilder otherInfo = new StringBuilder();
            for (int av = 0; av < axisVariables.length; av++) {
                if (av != xAxisIndex && av != yAxisIndex) {
                    int ttIndex = constraints.get(av * 3);
                    EDVGridAxis axisVar = axisVariables[av];
                    if (otherInfo.length() > 0) 
                        otherInfo.append(", ");
                    double td = axisVar.destinationValue(ttIndex).getNiceDouble(0); 
                    if (av == lonIndex)
                        otherInfo.append(td + " E"); //° didn't work
                    else if (av == latIndex) 
                        otherInfo.append(td + " N"); //° didn't work
                    else if (av == timeIndex)
                        otherInfo.append(Calendar2.epochSecondsToIsoStringT(td) + "Z");
                    else {
                        String avUnits = axisVar.units();
                        avUnits = avUnits == null? "" : " " + avUnits;
                        otherInfo.append(axisVar.longName() + "=" + td + avUnits);
                    }
                }
            }
            if (otherInfo.length() > 0) {
                otherInfo.insert(0, "(");
                otherInfo.append(")");
            }
                
            //prepare to get the data  
            StringArray newReqDataNames = new StringArray();
            int nBytesPerElement = 0;
            for (int v = 0; v < nVars; v++) {
                if (vars[v] != null && !(vars[v] instanceof EDVGridAxis)) {
                    newReqDataNames.add(vars[v].destinationName());
                    nBytesPerElement += drawSurface? 8: //grid always stores data in double[]
                        vars[v].destinationBytesPerElement();
                }
            }
            String newQuery = buildDapQuery(newReqDataNames, constraints);
            if (reallyVerbose) String2.log("  newQuery=" + newQuery);
            GridDataAccessor gda = new GridDataAccessor(this, requestUrl, newQuery, 
                yAxisVar == null, //Table needs row-major order, Grid needs column-major order
                true); //convertToNaN
            long requestNL = gda.totalIndex().size();
            EDStatic.ensureArraySizeOkay(requestNL, "EDDGrid.saveAsImage"); 
            EDStatic.ensureMemoryAvailable(requestNL * nBytesPerElement, "EDDGrid.saveAsImage"); 
            int requestN = (int)requestNL; //safe since checked above
            Grid grid = null;
            Table table = null;
            GraphDataLayer graphDataLayer = null;
            ArrayList graphDataLayers = new ArrayList();
            String cptFullName = null;

            if (drawVectors) {
                //put the data in a Table   0=xAxisVar 1=yAxisVar 2=dataVar1 3=dataVar2
                table = new Table();
                PrimitiveArray xpa = PrimitiveArray.factory(vars[0].destinationDataTypeClass(), requestN, false);
                PrimitiveArray ypa = PrimitiveArray.factory(vars[1].destinationDataTypeClass(), requestN, false);
                PrimitiveArray zpa = PrimitiveArray.factory(vars[2].destinationDataTypeClass(), requestN, false);
                PrimitiveArray tpa = PrimitiveArray.factory(vars[3].destinationDataTypeClass(), requestN, false);
                table.addColumn(vars[0].destinationName(), xpa);
                table.addColumn(vars[1].destinationName(), ypa);
                table.addColumn(vars[2].destinationName(), zpa);
                table.addColumn(vars[3].destinationName(), tpa);
                while (gda.increment()) {
                    xpa.addDouble(gda.getAxisValueAsDouble(xAxisIndex));
                    ypa.addDouble(gda.getAxisValueAsDouble(yAxisIndex));
                    zpa.addDouble(gda.getDataValueAsDouble(0));
                    tpa.addDouble(gda.getDataValueAsDouble(1));
                }
                if (Double.isNaN(vectorStandard)) {
                    double stats1[] = zpa.calculateStats();
                    double stats2[] = tpa.calculateStats();
                    double lh[] = Math2.suggestLowHigh(0, Math.max(  //suggestLowHigh handles NaNs
                        Math.abs(stats1[PrimitiveArray.STATS_MAX]), 
                        Math.abs(stats2[PrimitiveArray.STATS_MAX])));
                    vectorStandard = lh[1];
                }

                String varInfo =  
                    vars[2].longName() + 
                    (zUnits.equals(tUnits)? "" : zUnits) +
                    ", " + 
                    vars[3].longName() + " (" + (float)vectorStandard +
                    (tUnits.length() == 0? "" : " " + vars[3].units()) + 
                    ")";

                //make a graphDataLayer with data  time series line
                graphDataLayer = new GraphDataLayer(
                    -1, //which pointScreen
                    0, 1, 2, 3, 1, //x,y,z1,z2,z3 column numbers
                    GraphDataLayer.DRAW_POINT_VECTORS,
                    xIsTimeAxis, yIsTimeAxis,
                    xAxisVar.longName() + xUnits, 
                    yAxisVar.longName() + yUnits, 
                    varInfo,
                    title(),             
                    otherInfo.toString(), 
                    "Data courtesy of " + institution(), 
                    table, null, null,
                    null, color, 
                    GraphDataLayer.MARKER_TYPE_NONE, 0,
                    vectorStandard,
                    GraphDataLayer.REGRESS_NONE);
                graphDataLayers.add(graphDataLayer);

            } else if (drawSticks) {
                //put the data in a Table   0=xAxisVar 1=uDataVar 2=vDataVar 
                table = new Table();
                PrimitiveArray xpa = PrimitiveArray.factory(vars[0].destinationDataTypeClass(), requestN, false);
                PrimitiveArray ypa = PrimitiveArray.factory(vars[1].destinationDataTypeClass(), requestN, false);
                PrimitiveArray zpa = PrimitiveArray.factory(vars[2].destinationDataTypeClass(), requestN, false);
                table.addColumn(vars[0].destinationName(), xpa);
                table.addColumn(vars[1].destinationName(), ypa);
                table.addColumn(vars[2].destinationName(), zpa);
                while (gda.increment()) {
                    xpa.addDouble(gda.getAxisValueAsDouble(xAxisIndex));
                    ypa.addDouble(gda.getDataValueAsDouble(0));
                    zpa.addDouble(gda.getDataValueAsDouble(1));
                }

                String varInfo =  
                    vars[1].longName() + 
                    (yUnits.equals(zUnits)? "" : yUnits) +
                    ", " + 
                    vars[2].longName() +  
                    (zUnits.length() == 0? "" : zUnits);

                //make a graphDataLayer with data  time series line
                graphDataLayer = new GraphDataLayer(
                    -1, //which pointScreen
                    0, 1, 2, 1, 1, //x,y,z1,z2,z3 column numbers
                    GraphDataLayer.DRAW_STICKS,
                    xIsTimeAxis, yIsTimeAxis,
                    xAxisVar.longName() + xUnits, 
                    varInfo, 
                    title(),
                    otherInfo.toString(), 
                    "",             
                    "Data courtesy of " + institution(), 
                    table, null, null,
                    null, color, 
                    GraphDataLayer.MARKER_TYPE_NONE, 0,
                    1,
                    GraphDataLayer.REGRESS_NONE);
                graphDataLayers.add(graphDataLayer);

            } else if (isMap || drawSurface) {
                //if .colorBar info didn't provide info, try to get defaults from vars[2] colorBarXxx attributes 
                if (vars[2] != null) { //it shouldn't be
                    Attributes colorVarAtts = vars[2].combinedAttributes();
                    if (palette.length() == 0)     palette     = colorVarAtts.getString("colorBarPalette");
                    if (scale.length() == 0)       scale       = colorVarAtts.getString("colorBarScale");
                    if (Double.isNaN(paletteMin))  paletteMin  = colorVarAtts.getDouble("colorBarMinimum");
                    if (Double.isNaN(paletteMax))  paletteMax  = colorVarAtts.getDouble("colorBarMaximum");
                    String ts = colorVarAtts.getString("colorBarContinuous");
                    if (continuousS.length() == 0 && ts != null) continuousS = String2.parseBoolean(ts)? "c" : "d"; //defaults to true
                }

                if (String2.indexOf(EDStatic.palettes, palette) < 0) palette   = "";
                if (String2.indexOf(EDV.VALID_SCALES, scale) < 0)    scale     = "Linear";
                if (nSections < 0 || nSections >= 100)               nSections = -1;
                boolean continuous = continuousS.startsWith("d")? false : true; 

                //put the data in a Grid, data in column-major order
                grid = new Grid();
                grid.data = new double[requestN];
                if (png && drawLegend.equals(LEGEND_ONLY) &&
                    palette.length() > 0 && !Double.isNaN(paletteMin) && !Double.isNaN(paletteMax)) {

                    //legend=Only and palette range is known, so don't need to get the data 
                    if (reallyVerbose) String2.log("***LEGEND ONLY: SO NOT GETTING THE DATA");
                    Arrays.fill(grid.data, Double.NaN); //safe for all situations

                } else {

                    //get the data
                    int po = 0;
                    while (gda.increment()) 
                        grid.data[po++] = gda.getDataValueAsDouble(0);
                }

                if (false) { //reallyVerbose) {
                    DoubleArray da = new DoubleArray(grid.data);
                    double stats[] = da.calculateStats();
                    String2.log("dataNTotal=" + da.size() + 
                        " dataN=" + stats[PrimitiveArray.STATS_N] +
                        " dataMin=" + stats[PrimitiveArray.STATS_MIN] +
                        " dataMax=" + stats[PrimitiveArray.STATS_MAX]);
                }

                //get the lon values
                PrimitiveArray tpa = gda.axisValues(xAxisIndex);
                int tn = tpa.size();
                grid.lon = new double[tn];
                for (int i = 0; i < tn; i++)
                    grid.lon[i] = tpa.getDouble(i);
                grid.lonSpacing = (grid.lon[tn - 1] - grid.lon[0]) / Math.max(1, tn - 1);

                //get the lat values
                tpa = gda.axisValues(yAxisIndex);
                tn = tpa.size();
                grid.lat = new double[tn];
                for (int i = 0; i < tn; i++)
                    grid.lat[i] = tpa.getDouble(i); 
                grid.latSpacing = (grid.lat[tn - 1] - grid.lat[0]) / Math.max(1, tn - 1);

                //cptFullName
                if (Double.isNaN(paletteMin) || Double.isNaN(paletteMax)) {
                    //if not specified, I have the right to change
                    DoubleArray da = new DoubleArray(grid.data);
                    double stats[] = da.calculateStats();
                    minData = stats[PrimitiveArray.STATS_MIN];
                    maxData = stats[PrimitiveArray.STATS_MAX];
                    if (maxData >= minData / -2 && 
                        maxData <= minData * -2) {
                        double td = Math.max(maxData, -minData);
                        minData = -td;
                        maxData = td;
                    }
                    double tRange[] = Math2.suggestLowHigh(minData, maxData);
                    minData = tRange[0];
                    maxData = tRange[1];
                    if (maxData >= minData / -2 && 
                        maxData <= minData * -2) {
                        double td = Math.max(maxData, -minData);
                        minData = -td;
                        maxData = td;
                    }
                    if (Double.isNaN(paletteMin)) paletteMin = minData;
                    if (Double.isNaN(paletteMax)) paletteMax = maxData;
                }
                if (paletteMin > paletteMax) {
                    double d = paletteMin; paletteMin = paletteMax; paletteMax = d;
                }
                if (paletteMin == paletteMax) {
                    double tRange[] = Math2.suggestLowHigh(paletteMin, paletteMax);
                    paletteMin = tRange[0];
                    paletteMax = tRange[1];
                }
                if (palette.length() == 0)
                    palette = Math2.almostEqual(3, -paletteMin, paletteMax)? "BlueWhiteRed" : "Rainbow";
                if (scale.length() == 0)       
                    scale = "Linear";
                cptFullName = CompoundColorMap.makeCPT(EDStatic.fullPaletteDirectory, 
                    palette, scale, paletteMin, paletteMax, nSections, continuous, 
                    EDStatic.fullCptCacheDirectory);

                //make a graphDataLayer with coloredSurface setup
                graphDataLayer = new GraphDataLayer(
                    -1, //which pointScreen
                    0, 1, 1, 1, 1, //x,y,z1,z2,z3 column numbers    irrelevant
                    GraphDataLayer.DRAW_COLORED_SURFACE, //AND_CONTOUR_LINE?
                    xIsTimeAxis, yIsTimeAxis,
                    (reallySmall? xAxisVar.destinationName() : xAxisVar.longName()) + xUnits, //x,yAxisTitle  for now, always std units 
                    (reallySmall? yAxisVar.destinationName() : yAxisVar.longName()) + yUnits, 
                    (reallySmall? vars[2].destinationName()  : vars[2].longName()) + zUnits, //boldTitle
                    title(),             
                    "",
                    "Data courtesy of " + institution(), 
                    null, grid, null,
                    new CompoundColorMap(cptFullName), color, //color is irrelevant 
                    -1, -1, //marker type, size
                    0, //vectorStandard
                    GraphDataLayer.REGRESS_NONE);
                graphDataLayers.add(graphDataLayer);

            } else {  //make graph with lines, linesAndMarkers, or markers
                //put the data in a Table   x,y,(z)
                table = new Table();
                PrimitiveArray xpa = PrimitiveArray.factory(vars[0].destinationDataTypeClass(), requestN, false);
                PrimitiveArray ypa = PrimitiveArray.factory(vars[1].destinationDataTypeClass(), requestN, false);
                PrimitiveArray zpa = vars[2] == null? null :
                                     PrimitiveArray.factory(vars[2].destinationDataTypeClass(), requestN, false);
                table.addColumn(vars[0].destinationName(), xpa);
                table.addColumn(vars[1].destinationName(), ypa);

                if (vars[2] != null) {
                    table.addColumn(vars[2].destinationName(), zpa);

                    //if .colorBar info didn't provide info, try to get defaults from vars[2] colorBarXxx attributes 
                    Attributes colorVarAtts = vars[2].combinedAttributes();
                    if (palette.length() == 0)     palette     = colorVarAtts.getString("colorBarPalette");
                    if (scale.length() == 0)       scale       = colorVarAtts.getString("colorBarScale");
                    if (Double.isNaN(paletteMin))  paletteMin  = colorVarAtts.getDouble("colorBarMinimum");
                    if (Double.isNaN(paletteMax))  paletteMax  = colorVarAtts.getDouble("colorBarMaximum");
                    String ts = colorVarAtts.getString("colorBarContinuous");
                    if (continuousS.length() == 0 && ts != null) continuousS = String2.parseBoolean(ts)? "c" : "d"; //defaults to true

                    if (String2.indexOf(EDStatic.palettes, palette) < 0) palette   = "";
                    if (String2.indexOf(EDV.VALID_SCALES, scale) < 0)    scale     = "Linear";
                    if (nSections < 0 || nSections >= 100)               nSections = -1;
                }

                if (png && drawLegend.equals(LEGEND_ONLY) && 
                    (vars[2] == null ||
                     (palette.length() > 0 && !Double.isNaN(paletteMin) && !Double.isNaN(paletteMax)))) {

                    //legend=Only and (no color var or palette range is known), so don't need to get the data 
                    if (reallyVerbose) String2.log("***LEGEND ONLY: SO NOT GETTING THE DATA");
                    xpa.addDouble(Double.NaN);
                    ypa.addDouble(Double.NaN);
                    if (vars[2] != null) 
                        zpa.addDouble(Double.NaN);

                } else {
                    //need to get the data
                    while (gda.increment()) {
                        xpa.addDouble(gda.getAxisValueAsDouble(xAxisIndex));
                        ypa.addDouble(yAxisIndex >= 0? gda.getAxisValueAsDouble(yAxisIndex) : 
                            gda.getDataValueAsDouble(0));
                        if (vars[2] != null) 
                            zpa.addDouble(gda.getDataValueAsDouble(yAxisIndex >= 0? 0 : 1)); //yAxisIndex>=0 is true if y is index
                    }
                }


                //make the colorbar
                CompoundColorMap colorMap = null;
                if (vars[2] != null) {
                    boolean continuous = continuousS.startsWith("d")? false : true; 

                    if (palette.length() == 0 || Double.isNaN(paletteMin) || Double.isNaN(paletteMax)) {
                        //set missing items based on z data
                        double zStats[] = table.getColumn(2).calculateStats();
                        if (zStats[PrimitiveArray.STATS_N] > 0) {
                            double minMax[];
                            if (vars[2] instanceof EDVTimeStamp) {
                                //???I think this is too crude. Smarter code elsewhere? Or handled by compoundColorMap?
                                double r20 = 
                                    (zStats[PrimitiveArray.STATS_MAX] -
                                     zStats[PrimitiveArray.STATS_MIN]) / 20;
                                minMax = new double[]{
                                    zStats[PrimitiveArray.STATS_MIN] - r20,
                                    zStats[PrimitiveArray.STATS_MAX] + r20};
                            } else {
                                minMax = Math2.suggestLowHigh(
                                    zStats[PrimitiveArray.STATS_MIN],
                                    zStats[PrimitiveArray.STATS_MAX]);
                            }

                            if (palette.length() == 0) {
                                if (minMax[1] >= minMax[0] / -2 && 
                                    minMax[1] <= minMax[0] * -2) {
                                    double td = Math.max(minMax[1], -minMax[0]);
                                    minMax[0] = -td;
                                    minMax[1] = td;
                                    palette = "BlueWhiteRed";
                                //} else if (minMax[0] >= 0 && minMax[0] < minMax[1] / 5) {
                                //    palette = "WhiteRedBlack";
                                } else {
                                    palette = "Rainbow";
                                }
                            }
                            if (Double.isNaN(paletteMin)) 
                                paletteMin = minMax[0];
                            if (Double.isNaN(paletteMax)) 
                                paletteMax = minMax[1];
                        }                                             
                    }
                    if (palette.length() == 0 || Double.isNaN(paletteMin) || Double.isNaN(paletteMax)) {
                        //don't create a colorMap
                        String2.log("Warning in EDDTable.saveAsImage: NaNs not allowed (zVar has no numeric data):" +
                            " palette=" + palette +
                            " paletteMin=" + paletteMin +
                            " paletteMax=" + paletteMax);
                    } else {
                        if (reallyVerbose)
                            String2.log("create colorBar palette=" + palette +
                                " continuous=" + continuous +
                                " scale=" + scale +
                                " min=" + paletteMin + " max=" + paletteMax +
                                " nSections=" + nSections);                                
                        if (vars[2] instanceof EDVTimeStamp)
                            colorMap = new CompoundColorMap(
                            EDStatic.fullPaletteDirectory, palette, false, //false= data is seconds
                            paletteMin, paletteMax, nSections, 
                            continuous, EDStatic.fullCptCacheDirectory);
                        else colorMap = new CompoundColorMap(
                            EDStatic.fullPaletteDirectory, palette, scale, 
                            paletteMin, paletteMax, nSections, 
                            continuous, EDStatic.fullCptCacheDirectory);
                    }
                }

                //make a graphDataLayer with data  time series line
                graphDataLayer = new GraphDataLayer(
                    -1, //which pointScreen
                    0, 1, vars[2] == null? 1 : 2, 1, 1, //x,y,z1,z2,z3 column numbers
                    drawLines? GraphDataLayer.DRAW_LINES :
                        drawMarkers? GraphDataLayer.DRAW_MARKERS :
                        GraphDataLayer.DRAW_MARKERS_AND_LINES,
                    xIsTimeAxis, yIsTimeAxis,
                    (reallySmall? xAxisVar.destinationName() : xAxisVar.longName()) + xUnits, //x,yAxisTitle  for now, always std units 
                    (reallySmall? vars[1].destinationName()  :  vars[1].longName()) + yUnits, 
                    vars[2] == null? title() : 
                        (reallySmall? vars[2].destinationName() : vars[2].longName()) + zUnits,             
                    vars[2] == null? ""      : title(),
                    otherInfo.toString(), 
                    "Data courtesy of " + institution(), 
                    table, null, null,
                    colorMap, color, 
                    markerType, markerSize,
                    0, //vectorStandard
                    GraphDataLayer.REGRESS_NONE);
                graphDataLayers.add(graphDataLayer);
            } 

            //setup graphics2D
            String logoImageFile;
            //transparentPng will revise this below
            if (pdf) {
                fontScale *= 1.4 * fontScale; //SgtMap.PDF_FONTSCALE=1.5 is too big
                logoImageFile = EDStatic.highResLogoImageFile;
                pdfInfo = SgtUtil.createPdf(SgtUtil.PDF_PORTRAIT, 
                    imageWidth, imageHeight, outputStreamSource.outputStream("UTF-8"));
                g2 = (Graphics2D)pdfInfo[0];
            } else {
                fontScale *= imageWidth < 500? 1: 1.25;
                logoImageFile = sizeIndex <= 1? EDStatic.lowResLogoImageFile : EDStatic.highResLogoImageFile;
                bufferedImage = SgtUtil.getBufferedImage(imageWidth, imageHeight);
                g2 = (Graphics2D)bufferedImage.getGraphics();
            }

            if (transparentPng) {
                //fill with unusual color --> later convert to transparent
                //Not a great approach to the problem.
                transparentColor = new Color(0,3,1); //not common, not in grayscale, not white
                g2.setColor(transparentColor);
                g2.fillRect(0, 0, imageWidth, imageHeight);
            }

            if (drawSurface) {
                if (transparentPng) {
                    //draw the map
                    SgtMap.makeCleanMap(minX, maxX, minY, maxY,
                        false,
                        grid, 1, 1, 0, //double gridScaleFactor, gridAltScaleFactor, gridAltOffset,
                        cptFullName, 
                        false, false, SgtMap.NO_LAKES_AND_RIVERS, false, false,
                        g2, imageWidth, imageHeight,
                        0, 0, imageWidth, imageHeight); 
                } else {
                    if (drawLandAsMask == 0) 
                        drawLandAsMask = vars[2].drawLandMask(defaultDrawLandMask())? 2 : 1;
                    ArrayList mmal = SgtMap.makeMap(false, 
                        SgtUtil.LEGEND_BELOW,
                        EDStatic.legendTitle1, EDStatic.legendTitle2,
                        EDStatic.imageDir, logoImageFile,
                        minX, maxX, minY, maxY, 
                        drawLandAsMask == 2, 
                        true, //plotGridData 
                        grid, 1, 1, 0, //double gridScaleFactor, gridAltScaleFactor, gridAltOffset,
                        cptFullName,
                        vars[2].longName() + zUnits,
                        title(),
                        otherInfo.toString(),
                        "Data courtesy of " + institution(),
                        palette.equals("Ocean") || palette.equals("Topography") ? 
                            SgtMap.FILL_LAKES_AND_RIVERS : SgtMap.STROKE_LAKES_AND_RIVERS, 
                        false, null, 1, 1, 1, "", null, "", "", "", "", "", //plot contour 
                        new ArrayList(),
                        g2, 0, 0, imageWidth, imageHeight,
                        0, //no boundaryResAdjust,
                        fontScale);

                    writePngInfo(loggedInAs, userDapQuery, fileTypeName, mmal);
                }

            } else if (drawVectors || drawLines || drawLinesAndMarkers || drawMarkers || drawSticks) {
                if (drawLandAsMask == 0) {
                    EDV edv = vars[2] == null? vars[1] : vars[2];
                    drawLandAsMask = edv.drawLandMask(defaultDrawLandMask())? 2 : 1;
                }

                ArrayList mmal = isMap?
                    SgtMap.makeMap(transparentPng, 
                        SgtUtil.LEGEND_BELOW,
                        EDStatic.legendTitle1, EDStatic.legendTitle2,
                        EDStatic.imageDir, logoImageFile,
                        minX, maxX, minY, maxY, 
                        drawLandAsMask == 2,
                        false, null, 1, 1, 0, "", "", "", "", "", //plotGridData                
                        SgtMap.FILL_LAKES_AND_RIVERS, 
                        false, null, 1, 1, 1, "", null, "", "", "", "", "", //plot contour 
                        graphDataLayers,
                        g2, 0, 0, imageWidth, imageHeight,
                        0, //no boundaryResAdjust,
                        fontScale) :

                    EDStatic.sgtGraph.makeGraph(transparentPng,
                        graphDataLayer.xAxisTitle, 
                        png && drawLegend.equals(LEGEND_ONLY)? "." : graphDataLayer.yAxisTitle, //avoid running into legend
                        SgtUtil.LEGEND_BELOW, EDStatic.legendTitle1, EDStatic.legendTitle2,
                        EDStatic.imageDir, logoImageFile,
                        minX, maxX,
                        minY, maxY,
                        xIsTimeAxis, yIsTimeAxis, 
                        graphDataLayers,
                        g2, 0, 0, imageWidth, imageHeight,  1, //graph imageWidth/imageHeight
                        fontScale); 

                writePngInfo(loggedInAs, userDapQuery, fileTypeName, mmal);
            }

            //.legend
            if (png && !transparentPng) {
                if (drawLegend.equals(LEGEND_OFF))
                    bufferedImage = SgtUtil.removeLegend(bufferedImage);
                else if (drawLegend.equals(LEGEND_ONLY))
                    bufferedImage = SgtUtil.extractLegend(bufferedImage);

                //do after removeLegend
                bufferedImage = SgtUtil.trimBottom(bufferedImage, trim);
            }

        } catch (Throwable t) {
            ok = false;
            try {
                String msg = MustBe.getShortErrorMessage(t);
                String2.log(MustBe.throwableToString(t)); //log full message with stack trace

                if (png && drawLegend.equals(LEGEND_ONLY)) {
                    //return a transparent 1x1 pixel image
                    bufferedImage = SgtUtil.getBufferedImage(1, 1); //has white background
                    transparentColor = Color.white;

                } else {
                    //write exception info on image
                    double tFontScale = pdf? 1.25 : 1;
                    int tHeight = Math2.roundToInt(tFontScale * 12);

                    
                    if (pdf) {
                        if (pdfInfo == null)
                            pdfInfo = SgtUtil.createPdf(SgtUtil.PDF_PORTRAIT, 
                                imageWidth, imageHeight, outputStreamSource.outputStream("UTF-8"));
                        if (g2 == null)
                            g2 = (Graphics2D)pdfInfo[0];
                    } else { //png
                        //make a new image (I don't think pdf can work this way -- sent as created)
                        bufferedImage = SgtUtil.getBufferedImage(imageWidth, imageHeight);
                        g2 = (Graphics2D)bufferedImage.getGraphics();
                    }
                    if (transparentPng) {
                        //don't write the message  
                        //The "right" thing to do is different in different situations.
                        //But e.g., No Data, should just be a transparent image.
                        transparentColor = Color.white;
                    } else {
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                            RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setClip(0, 0, imageWidth, imageHeight); //unset in case set by sgtGraph
                        msg = String2.noLongLines(msg, (imageWidth * 10 / 6) / tHeight, "    ");
                        String lines[] = msg.split("\\n"); //not String2.split which trims
                        g2.setColor(Color.black);
                        g2.setFont(new Font(EDStatic.fontFamily, Font.PLAIN, tHeight));
                        int ty = tHeight * 2;
                        for (int i = 0; i < lines.length; i++) {
                            g2.drawString(lines[i], tHeight, ty);
                            ty += tHeight + 2;
                        }
                    }
                }
            } catch (Throwable t2) {
                String2.log("ERROR2 while creating image:\n" + MustBe.throwableToString(t2));
                if (pdf) {
                    if (pdfInfo == null) throw t;
                } else {
                    if (bufferedImage == null) throw t;
                }
                //else fall through to close/save image below
            }
        }

        //save image
        if (pdf) {
            SgtUtil.closePdf(pdfInfo);
        } else {
            SgtUtil.saveAsTransparentPng(bufferedImage, transparentColor, 
                outputStreamSource.outputStream("")); 
        }

        outputStreamSource.outputStream("").flush(); //safety

        if (reallyVerbose) String2.log("  EDDGrid.saveAsImage done. TIME=" + 
            (System.currentTimeMillis() - time) + "\n");
        return ok;
    }

    /**
     * This writes the axis or grid data to the outputStream in JSON 
     * (http://www.json.org/) format.
     * If no exception is thrown, the data was successfully written.
     * 
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     * @param userDapQuery an OPeNDAP DAP-style query string, still percentEncoded (shouldn't be null). 
     *   e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160].
     *   This method extracts the jsonp text to be prepended to the results (or null if none).
     *     See http://niryariv.wordpress.com/2009/05/05/jsonp-quickly/
     *     and http://bob.pythonmac.org/archives/2005/12/05/remote-json-jsonp/
     *     and http://www.insideria.com/2009/03/what-in-the-heck-is-jsonp-and.html .
     * @param outputStreamSource the source of an outputStream (usually already 
     *   buffered) to receive the results.
     *   At the end of this method the outputStream is flushed, not closed.
     * @throws Throwable  if trouble. 
     */
    public void saveAsJson(String requestUrl, String userDapQuery, 
        OutputStreamSource outputStreamSource) throws Throwable {

//currently, this writes a table. 
//Perhaps better to write nDimensional array?
        if (reallyVerbose) String2.log("  EDDGrid.saveAsJson"); 
        long time = System.currentTimeMillis();

        //did query include &.jsonp= ?
        String parts[] = getUserQueryParts(userDapQuery);
        String jsonp = String2.stringStartsWith(parts, ".jsonp="); //may be null
        if (jsonp != null) 
            jsonp = SSR.percentDecode(jsonp.substring(7));

        //get dataAccessor first, in case of error when parsing query
        boolean isAxisDapQuery = isAxisDapQuery(userDapQuery);
        AxisDataAccessor ada = null;
        GridDataAccessor gda = null;
        if (isAxisDapQuery) 
             ada = new AxisDataAccessor(this, requestUrl, userDapQuery);
        else gda = new GridDataAccessor(this, requestUrl, userDapQuery, 
            true, false);   //rowMajor, convertToNaN (would be true, but TableWriterJson will do it)

        //write the data to the tableWriter
        TableWriter tw = new TableWriterJson(outputStreamSource, jsonp, true); //writeUnits 
        if (isAxisDapQuery) 
             saveAsTableWriter(ada, tw);
        else saveAsTableWriter(gda, tw);

        //diagnostic
        if (reallyVerbose)
            String2.log("  EDDGrid.saveAsJson done. TIME=" + 
                (System.currentTimeMillis() - time) + "\n");

    }

    /**
     * This writes grid data (not axis data) to the outputStream in Google Earth's 
     * .kml format (http://earth.google.com/).
     * If no exception is thrown, the data was successfully written.
     * For .kml, dataVariable queries can specify multiple longitude, latitude,
     * and time values, but just one value for other dimensions.
     * 
     * @param loggedInAs  the name of the logged in user (or null if not logged in).
     *   Normally, this is not used to test if this edd is accessibleTo loggedInAs, 
     *   but it unusual cases (EDDTableFromPost?) it could be.
     *   Normally, this is just used to determine which erddapUrl to use (http vs https).
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     * @param userDapQuery an OPeNDAP DAP-style query string, still percentEncoded (shouldn't be null). 
     *   e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160].
     * @param outputStreamSource the source of an outputStream (usually already 
     *   buffered) to receive the results.
     *   At the end of this method the outputStream is flushed, not closed.
     * @return true of written ok; false if exception occurred (and written on image)
     * @throws Throwable  if trouble. 
     */
    public boolean saveAsKml(
        String loggedInAs, String requestUrl, String userDapQuery, 
        OutputStreamSource outputStreamSource) throws Throwable {

        if (reallyVerbose) 
            String2.log("  EDDGrid.saveAsKml"); 
        long time = System.currentTimeMillis();
        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);

        //check that request meets .kml restrictions.
        //.transparentPng does some of these tests, but better to catch problems
        //  here than in GoogleEarth.

        //.kml not available for axis request
        //lon and lat are required; time is not required
        if (isAxisDapQuery(userDapQuery) || lonIndex < 0 || latIndex < 0) 
            throw new SimpleException("Error: " +
                "The .kml format is for latitude longitude data requests only.");

        //parse the userDapQuery
        //this also tests for error when parsing query
        StringArray tDestinationNames = new StringArray();
        IntArray tConstraints = new IntArray();
        parseDataDapQuery(userDapQuery, tDestinationNames, tConstraints, false);
        if (tDestinationNames.size() != 1) 
            throw new SimpleException("Query error: " +
                "The .kml format can only handle one data variable.");

        //find any &constraints (simplistic approach, but sufficient for here and hard to replace with getUserQueryParts)
        int ampPo = -1;
        if (userDapQuery != null) {
            ampPo = userDapQuery.indexOf('&');
            if (ampPo == -1) 
                ampPo = userDapQuery.indexOf("%26");  //shouldn't be.  but allow overly zealous percent encoding.
        }
        String percentEncodedAmpQuery = ampPo >= 0?   //so constraints can be used in reference urls in kml
            XML.encodeAsXML(userDapQuery.substring(ampPo)) : "";

        EDVTimeGridAxis timeEdv = null;
        PrimitiveArray timePa = null;
        double timeStartd = Double.NaN, timeStopd = Double.NaN;
        int nTimes = 0;
        for (int av = 0; av < axisVariables.length; av++) {
            if (av == lonIndex) {

            } else if (av == latIndex) {

            } else if (av == timeIndex) {
                timeEdv = (EDVTimeGridAxis)axisVariables[timeIndex];
                timePa = timeEdv.sourceValues().subset(tConstraints.get(av*3 + 0), 
                    tConstraints.get(av*3 + 1), tConstraints.get(av*3 + 2));
                timePa = timeEdv.toDestination(timePa);
                nTimes = timePa.size();
                timeStartd = timePa.getNiceDouble(0);
                timeStopd  = timePa.getNiceDouble(nTimes - 1);
                if (nTimes > 500) //arbitrary: prevents requests that would take too long to respond to
                    throw new SimpleException("Error: " +
                        "For .kml requests, the time dimension's size must be less than 500.");

            } else {
                if (tConstraints.get(av*3 + 0) != tConstraints.get(av*3 + 2))
                    throw new SimpleException("Error: " +
                        "For .kml requests, the " + 
                        axisVariables[av].destinationName() + " dimension's size must be 1."); 
            }
        }

        //lat lon info
        //lon and lat axis values don't have to be evenly spaced.
        //.transparentPng uses Sgt.makeCleanMap which projects data (even, e.g., Mercator)
        //so resulting .png will use a geographic projection.

        //although the Google docs say lon must be +-180, lon > 180 is sortof ok!
        EDVLonGridAxis lonEdv = (EDVLonGridAxis)axisVariables[lonIndex];
        EDVLatGridAxis latEdv = (EDVLatGridAxis)axisVariables[latIndex];

        int    totalNLon = lonEdv.sourceValues().size();
        int    lonStarti = tConstraints.get(lonIndex*3 + 0);
        int    lonStopi  = tConstraints.get(lonIndex*3 + 2);
        double lonStartd = lonEdv.destinationValue(lonStarti).getNiceDouble(0);
        double lonStopd  = lonEdv.destinationValue(lonStopi).getNiceDouble(0);
        if (lonStopd  <= -180 || lonStartd >= 360)
            throw new SimpleException("Error: " +
                "For .kml requests, there must be some longitude values must be between -180 and 360.");
        if (lonStartd < -180) {
            lonStarti = lonEdv.destinationToClosestSourceIndex(-180);
            lonStartd = lonEdv.destinationValue(lonStarti).getNiceDouble(0);
        }
        if (lonStopd > Math.min(lonStartd + 360, 360)) {
            lonStopi = lonEdv.destinationToClosestSourceIndex(Math.min(lonStartd + 360, 360));
            lonStopd = lonEdv.destinationValue(lonStopi).getNiceDouble(0);
        }
        int    lonMidi   = (lonStarti + lonStopi) / 2;
        double lonMidd   = lonEdv.destinationValue(lonMidi).getNiceDouble(0);
        double lonAverageSpacing = Math.abs(lonEdv.averageSpacing());

        int    totalNLat = latEdv.sourceValues().size();
        int    latStarti = tConstraints.get(latIndex*3 + 0);
        int    latStopi  = tConstraints.get(latIndex*3 + 2);
        double latStartd = latEdv.destinationValue(latStarti).getNiceDouble(0);
        double latStopd  = latEdv.destinationValue(latStopi).getNiceDouble(0);
        if (latStartd < -90 || latStopd > 90)
            throw new SimpleException("Error: " +
                "For .kml requests, the latitude values must be between -90 and 90.");
        int    latMidi   = (latStarti + latStopi) / 2;
        double latMidd   = latEdv.destinationValue(latMidi).getNiceDouble(0);
        double latAverageSpacing = Math.abs(latEdv.averageSpacing());

        if (lonStarti == lonStopi || latStarti == latStopi) 
            throw new SimpleException("Error: " +
                "For .kml requests, the lon and lat dimension sizes must be greater than 1."); 
        //request is ok and compatible with .kml request!

        String datasetUrl = tErddapUrl + "/" + dapProtocol + "/" + datasetID;
        String timeString = "";
        if (nTimes >= 1) timeString += Calendar2.epochSecondsToIsoStringT(Math.min(timeStartd, timeStopd)) + "Z";
        if (nTimes >= 2) 
            throw new SimpleException("Error: " +
                "For .kml requests, the time dimension size must be 1."); 
            //timeString += " through " +
            //Calendar2.epochSecondsToIsoStringT(Math.max(timeStartd, timeStopd)) + "Z";
        String brTimeString = timeString.length() == 0? "" : "Time: " + timeString + "<br />\n"; 

        //calculate doMax and get drawOrder
        int drawOrder = 1;
        int doMax = 1;  //max value of drawOrder for this dataset
        double tnLon = totalNLon;
        double tnLat = totalNLat;
        int txPo = Math2.roundToInt(lonStarti / tnLon); //at this level, the txPo'th x tile 
        int tyPo = Math2.roundToInt(latStarti / tnLat);
        while (Math.min(tnLon, tnLat) > 512) { //256 led to lots of artifacts and gaps at seams
            //This determines size of all tiles.
            //512 leads to smallest tile edge being >256.
            //256 here relates to minLodPixels 256 below (although Google example used 128 below)
            //and Google example uses tile sizes of 256x256.

            //go to next level
            tnLon /= 2;
            tnLat /= 2;
            doMax++;

            //if user requested lat lon range < this level, drawOrder is at least this level
            //!!!THIS IS TRICKY if user starts at some wierd subset (not full image).
            if (reallyVerbose) 
                String2.log("doMax=" + doMax + 
                "; cLon=" + (lonStopi - lonStarti + 1) + " <= 1.5*tnLon=" + (1.5*tnLon) + 
                "; cLat=" + (latStopi - latStarti + 1) + " <= 1.5*tnLat=" + (1.5*tnLat)); 
            if (lonStopi - lonStarti + 1 <= 1.5 * tnLon &&  //1.5 ~rounds to nearest drawOrder
                latStopi - latStarti + 1 <= 1.5 * tnLat) { 
                drawOrder++;
                txPo = Math2.roundToInt(lonStarti / tnLon); //at this level, this is the txPo'th x tile 
                tyPo = Math2.roundToInt(latStarti / tnLat);
                if (reallyVerbose)
                    String2.log("    drawOrder=" + drawOrder +  
                        " txPo=" + lonStarti + "/" + tnLon + "+" + txPo + 
                        " tyPo=" + latStarti + "/" + tnLat + "+" + tyPo);
            }
        }

        //calculate lonLatStride: 1 for doMax, 2 for doMax-1
        int lonLatStride = 1;
        for (int i = drawOrder; i < doMax; i++)
            lonLatStride *= 2;
        if (reallyVerbose) 
            String2.log("    final drawOrder=" + drawOrder +  
            " txPo=" + txPo + " tyPo=" + tyPo +
            " doMax=" + doMax + " lonLatStride=" + lonLatStride);

        //Based on http://code.google.com/apis/kml/documentation/kml_21tutorial.html#superoverlays
        //Was based on quirky example (but lots of useful info):
        //http://161.55.17.243/cgi-bin/pydap.cgi/AG/ssta/3day/AG2006001_2006003_ssta.nc.kml?LAYERS=AGssta
        //kml docs: http://earth.google.com/kml/kml_tags.html
        //CDATA is necessary for url's with queries
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
            outputStreamSource.outputStream("UTF-8"), "UTF-8"));
        writer.write(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
            "<Document>\n" +
            //human-friendly, but descriptive, <name>
            //name is used as link title -- leads to <description> 
            "  <name>");
        if (drawOrder == 1) writer.write(
            XML.encodeAsXML(title()) + "</name>\n" +
            //<description appears in help balloon
            //<br /> is what kml/description documentation recommends
            "  <description><![CDATA[" + 
            brTimeString +
            "Data courtesy of: " + XML.encodeAsXML(institution()) + "<br />\n" +
            //link to download data
            "<a href=\"" + 
                datasetUrl + ".html?" + 
                   SSR.minimalPercentEncode(tDestinationNames.get(0)) + //XML.encodeAsXML doesn't work 
                "\">Download data from this dataset.</a><br />\n" +
            "    ]]></description>\n");
        else writer.write(drawOrder + "_" + txPo + "_" + tyPo + "</name>\n");

        //GoogleEarth says it just takes lon +/-180, but it does ok (not perfect) with 180.. 360.
        //If minLon>=180, it is easy to adjust the lon value references in the kml,
        //  but leave the userDapQuery for the .transparentPng unchanged.
        //lonAdjust is ESSENTIAL for proper work with lon > 180.
        //GoogleEarth doesn't select correct drawOrder region if lon > 180.
        double lonAdjust = Math.min(lonStartd, lonStopd) >= 180? -360 : 0;
        String llBox = 
            "      <west>"  + (Math.min(lonStartd, lonStopd) + lonAdjust) + "</west>\n" +
            "      <east>"  + (Math.max(lonStartd, lonStopd) + lonAdjust) + "</east>\n" +
            "      <south>" +  Math.min(latStartd, latStopd) + "</south>\n" +
            "      <north>" +  Math.max(latStartd, latStopd) + "</north>\n";


        //is nTimes <= 1?
        StringBuilder tQuery; 
        if (nTimes <= 1) {
            //the Region
            writer.write(
                //min Level Of Detail: minimum size (initially while zooming in) at which this region is made visible
                //see http://code.google.com/apis/kml/documentation/kmlreference.html#lod
                "  <Region>\n" +
                "    <Lod><minLodPixels>" + (drawOrder == 1? 2 : 256) + "</minLodPixels>" +
                         //"<maxLodPixels>" + (drawOrder == 1? -1 : 1024) + "</maxLodPixels>" + //doesn't work as expected
                   "</Lod>\n" +
                "    <LatLonAltBox>\n" +
                    llBox +
                "    </LatLonAltBox>\n" +
                "  </Region>\n");

            if (drawOrder < doMax) {
                //NetworkLinks to subregions (quadrant)
                tQuery = new StringBuilder(tDestinationNames.get(0)); //limited chars, no need to URLEncode
                for (int nl = 0; nl < 4; nl++) {
                    double tLonStartd   = nl < 2? lonStartd : lonMidd;
                    double tLonStopd    = nl < 2? lonMidd   : lonStopd;
                    int ttxPo = txPo*2 + (nl < 2? 0 : 1);
                    double tLatStartd   = Math2.odd(nl)? latMidd : latStartd;
                    double tLatStopd    = Math2.odd(nl)? latStopd : latMidd;
                    int ttyPo = tyPo*2 + (Math2.odd(nl)? 1 : 0);                    
                    double tLonAdjust = Math.min(tLonStartd, tLonStopd) >= 180? -360 : 0; //see comments for lonAdjust above

                    tQuery = new StringBuilder(tDestinationNames.get(0)); //limited chars, no need to URLEncode
                    for (int av = 0; av < axisVariables.length; av++) {
                        if (av == lonIndex)
                            tQuery.append("[(" + tLonStartd + "):(" + tLonStopd + ")]");
                        else if (av == latIndex)
                            tQuery.append("[(" + tLatStartd + "):(" + tLatStopd + ")]");
                        else if (av == timeIndex)
                            tQuery.append("[(" + timeString + ")]");
                        else tQuery.append("[" + tConstraints.get(av*3 + 0) + "]");
                    }                

                    writer.write(
                    "  <NetworkLink>\n" +
                    "    <name>" + drawOrder + "_" + txPo + "_" + tyPo + "_" + nl + "</name>\n" +
                    "    <Region>\n" +
                    "      <Lod><minLodPixels>256</minLodPixels>" +
                           //"<maxLodPixels>1024</maxLodPixels>" + //doesn't work as expected.
                          "</Lod>\n" +
                    "      <LatLonAltBox>\n" +
                    "        <west>"  + (Math.min(tLonStartd, tLonStopd) + tLonAdjust) + "</west>\n" +
                    "        <east>"  + (Math.max(tLonStartd, tLonStopd) + tLonAdjust) + "</east>\n" +
                    "        <south>" +  Math.min(tLatStartd, tLatStopd) + "</south>\n" +
                    "        <north>" +  Math.max(tLatStartd, tLatStopd) + "</north>\n" +
                    "      </LatLonAltBox>\n" +
                    "    </Region>\n" +
                    "    <Link>\n" +
                    "      <href>" + datasetUrl + ".kml?" + 
                              SSR.minimalPercentEncode(tQuery.toString()) + //XML.encodeAsXML doesn't work 
                              percentEncodedAmpQuery + 
                          "</href>\n" +
                    "      <viewRefreshMode>onRegion</viewRefreshMode>\n" +
                    "    </Link>\n" +
                    "  </NetworkLink>\n");
                }
            }

            //the GroundOverlay which shows the current image
            tQuery = new StringBuilder(tDestinationNames.get(0)); //limited chars, no need to URLEncode
            for (int av = 0; av < axisVariables.length; av++) {
                if (av == lonIndex)
                    tQuery.append("[(" + lonStartd + "):" + lonLatStride + ":(" + lonStopd + ")]");
                else if (av == latIndex)
                    tQuery.append("[(" + latStartd + "):" + lonLatStride + ":(" + latStopd + ")]");
                else if (av == timeIndex)
                    tQuery.append("[(" + timeString + ")]");
                else tQuery.append("[" + tConstraints.get(av*3 + 0) + "]");
            }                
            writer.write(
                "  <GroundOverlay>\n" +
                //"    <name>" + XML.encodeAsXML(title()) + 
                //    (timeString.length() > 0? ", " + timeString : "") +
                //    "</name>\n" +
                "    <drawOrder>" + drawOrder + "</drawOrder>\n" +
                "    <Icon>\n" +
                "      <href>" + datasetUrl + ".transparentPng?" + 
                    SSR.minimalPercentEncode(tQuery.toString()) + //XML.encodeAsXML doesn't work 
                    percentEncodedAmpQuery + 
                     "</href>\n" +
                "    </Icon>\n" +
                "    <LatLonBox>\n" +
                        llBox +
                "    </LatLonBox>\n" +
                //"    <visibility>1</visibility>\n" +
                "  </GroundOverlay>\n");
        } /*else { 
            //nTimes >= 2, so make a timeline in Google Earth
            //Problem: I don't know what time range each image represents.
            //  Because I don't know what the timePeriod is for the dataset (e.g., 8day).
            //  And I don't know if the images overlap (e.g., 8day composites, every day)
            //  And if the stride>1, it is further unknown.
            //Solution (crummy): assume an image represents -1/2 time to previous image until 1/2 time till next image

            //get all the .dotConstraints
            String parts[] = getUserQueryParts(userDapQuery); //always at least 1 part (may be "")
            StringBuilder dotConstraintsSB = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].startsWith(".")) {
                    if (dotConstraintsSB.size() > 0)
                        dotConstraintsSB.append("&");
                    dotConstraintsSB.append(parts[i]);
                }
            }        
            String dotConstraints = dotConstraintsSB.toString();

            IntArray tConstraints = (IntArray)gridDataAccessor.constraints().clone();
            int startTimeIndex = tConstraints.get(timeIndex * 3);
            int timeStride     = tConstraints.get(timeIndex * 3 + 1);
            int stopTimeIndex  = tConstraints.get(timeIndex * 3 + 2); 
            double preTime = Double.NaN;
            double nextTime = allTimeDestPa.getDouble(startTimeIndex);
            double currentTime = nextTime - (allTimeDestPa.getDouble(startTimeIndex + timeStride) - nextTime);
            for (int tIndex = startTimeIndex; tIndex <= stopTimeIndex; tIndex += timeStride) {
                preTime = currentTime;
                currentTime = nextTime;
                nextTime = tIndex + timeStride > stopTimeIndex? 
                    currentTime + (currentTime - preTime) :
                    allTimeDestPa.getDouble(tIndex + timeStride);
                //String2.log("  tIndex=" + tIndex + " preT=" + preTime + " curT=" + currentTime + " nextT=" + nextTime);
                //just change the time constraints; leave all others unchanged
                tConstraints.set(timeIndex * 3, tIndex);
                tConstraints.set(timeIndex * 3 + 1, 1);
                tConstraints.set(timeIndex * 3 + 2, tIndex); 
                String tDapQuery = buildDapQuery(tDestinationNames, tConstraints) + dotConstraints;
                writer.write(
                    //the kml link to the data 
                    "  <GroundOverlay>\n" +
                    "    <name>" + Calendar2.epochSecondsToIsoStringT(currentTime) + "Z" + "</name>\n" +
                    "    <Icon>\n" +
                    "      <href>" + 
                        datasetUrl + ".transparentPng?" + I changed this: was minimalPercentEncode()... tDapQuery + //XML.encodeAsXML isn't ok
                        "</href>\n" +
                    "    </Icon>\n" +
                    "    <LatLonBox>\n" +
                    "      <west>" + west + "</west>\n" +
                    "      <east>" + east + "</east>\n" +
                    "      <south>" + south + "</south>\n" +
                    "      <north>" + north + "</north>\n" +
                    "    </LatLonBox>\n" +
                    "    <TimeSpan>\n" +
                    "      <begin>" + Calendar2.epochSecondsToIsoStringT((preTime + currentTime)  / 2.0) + "Z</begin>\n" +
                    "      <end>"   + Calendar2.epochSecondsToIsoStringT((currentTime + nextTime) / 2.0) + "Z</end>\n" +
                    "    </TimeSpan>\n" +
                    "    <visibility>1</visibility>\n" +
                    "  </GroundOverlay>\n");
            }
        }*/
        if (drawOrder == 1) 
            writer.write(getKmlIconScreenOverlay());
        writer.write(
            "</Document>\n" +
            "</kml>\n");
        writer.flush(); //essential

        //diagnostic
        if (reallyVerbose)
            String2.log("  EDDGrid.saveAsKml done. TIME=" + 
                (System.currentTimeMillis() - time) + "\n");
        return true;
    }

    /*
    public void saveAsKml(String requestUrl, String userDapQuery, 
        OutputStreamSource outputStreamSource) throws Throwable {

        if (reallyVerbose) String2.log("  EDDGrid.saveAsKml"); 
        long time = System.currentTimeMillis();

        //handle axis request
        if (isAxisDapQuery(userDapQuery)) 
            throw new SimpleException("Error: " +
                "The .kml format is for latitude longitude data requests only.");

        //lon and lat are required; time is not required
        if (lonIndex < 0 || latIndex < 0) 
            throw new SimpleException("Error: " +
                "The .kml format is for latitude longitude data requests only.");

        //parse the userDapQuery and get the GridDataAccessor
        //this also tests for error when parsing query
        GridDataAccessor gridDataAccessor = new GridDataAccessor(this, 
            requestUrl, userDapQuery, true, true);  //rowMajor, convertToNaN
        if (gridDataAccessor.dataVariables().length != 1) 
            throw new SimpleException("Error: " +
                "The .kml format can only handle one data variable.");
        StringArray tDestinationNames = new StringArray();
        tDestinationNames.add(gridDataAccessor.dataVariables()[0].destinationName());

        //check that request meets .kml restrictions.
        //.transparentPng does some of these tests, but better to catch problems
        //here than in GoogleEarth.
        int nTimes = 0;
        double firstTime = Double.NaN, lastTime = Double.NaN, timeSpacing = Double.NaN;
        PrimitiveArray lonPa = null, latPa = null, timePa = null, allTimeDestPa = null;
        EDVTimeGridAxis timeEdv = null;
        double lonAdjust = 0;
        for (int av = 0; av < axisVariables.length; av++) {
            PrimitiveArray avpa = gridDataAccessor.axisValues(av);
            if (av == lonIndex) {
                lonPa = avpa;

                //lon and lat axis values don't have to be evenly spaced.
                //.transparentPng uses Sgt.makeCleanMap which projects data (even, e.g., Mercator)
                //so resulting .png will use a geographic projection.

                //although the Google docs say lon must be +-180, lon > 180 is ok!
                //if (lonPa.getDouble(0) < 180 && lonPa.getDouble(lonPa.size() - 1) > 180)
                //    throw new SimpleException("Error: " +
                //    "For .kml requests, the longitude values can't be below and above 180.");

                //But if minLon>=180, it is easy to adjust the lon value references in the kml,
                //but leave the userDapQuery for the .transparentPng unchanged.
                if (lonPa.getDouble(0) >= 180)
                    lonAdjust = -360;
            } else if (av == latIndex) {
                latPa = avpa;
            } else if (av == timeIndex) {
                timeEdv = (EDVTimeGridAxis)axisVariables[timeIndex];
                allTimeDestPa = timeEdv.destinationValues();
                timePa = avpa;
                nTimes = timePa.size();
                if (nTimes > 500) //arbitrary: prevents requests that would take too long to respond to
                    throw new SimpleException("Error: " +
                        "For .kml requests, the time dimension's size must be less than 500.");
                firstTime = timePa.getDouble(0);
                lastTime = timePa.getDouble(nTimes - 1);
                if (nTimes > 1) 
                    timeSpacing = (lastTime - firstTime) / (nTimes - 1);
            } else {
                if (avpa.size() > 1)
                    throw new SimpleException("Error: " +
                        "For .kml requests, the " + 
                        axisVariables[av].destinationName() + " dimension's size must be 1."); 
            }
        }
        if (lonPa == null || latPa == null || lonPa.size() < 2 || latPa.size() < 2) 
            throw new SimpleException("Error: " +
                "For .kml requests, the lon and lat dimension sizes must be greater than 1."); 
        //request is ok and compatible with .kml request!

        //based on quirky example (but lots of useful info):
        //http://161.55.17.243/cgi-bin/pydap.cgi/AG/ssta/3day/AG2006001_2006003_ssta.nc.kml?LAYERS=AGssta
        //kml docs: http://earth.google.com/kml/kml_tags.html
        //CDATA is necessary for url's with queries
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
            outputStreamSource.outputStream("UTF-8"), "UTF-8"));
        double dWest = lonPa.getNiceDouble(0) + lonAdjust;
        double dEast = lonPa.getNiceDouble(lonPa.size() - 1) + lonAdjust;
        if (dWest > dEast) {  //it happens if axis is in descending order
            double td = dWest; dWest = dEast; dEast = td;}
        String west  = String2.genEFormat10(dWest);
        String east  = String2.genEFormat10(dEast);

        double dSouth = latPa.getNiceDouble(0);
        double dNorth = latPa.getNiceDouble(latPa.size() - 1);
        if (dSouth > dNorth) { //it happens if axis is in descending order
            double td = dSouth; dSouth = dNorth; dNorth = td; }        
        String south  = String2.genEFormat10(dSouth);
        String north  = String2.genEFormat10(dNorth);
        String datasetUrl = tErddapUrl + "/" + dapProtocol + "/" + datasetID;
        String timeString = nTimes == 0? "" :
            nTimes == 1? Calendar2.epochSecondsToIsoStringT(firstTime) :
              Calendar2.epochSecondsToIsoStringT(firstTime) + " through " + 
              Calendar2.epochSecondsToIsoStringT(lastTime);
        String brTimeString = timeString.length() == 0? "" : "Time: " + timeString + "<br />\n"; 
        writer.write(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
            "<Document>\n" +
            //human-friendly, but descriptive, <name>
            //name is used as link title -- leads to <description> 
            "  <name>" + XML.encodeAsXML(title()) + "</name>\n" +
            //<description appears in help balloon
            //<br /> is what kml/description documentation recommends
            "  <description><![CDATA[" + 
            brTimeString +
            "Data courtesy of: " + XML.encodeAsXML(institution()) + "<br />\n" +
            //link to download data
            "<a href=\"" + datasetUrl + ".html?" + SSR.minimalPercentEncode(userDapQuery) + //XML.encodeAsXML isn't ok
                "\">Download data from this dataset.</a><br />\n" +
            "    ]]></description>\n");

        //is nTimes <= 1?
        if (nTimes <= 1) {
            //no timeline in Google Earth
            writer.write(
                //the kml link to the data 
                "  <GroundOverlay>\n" +
                "    <name>" + title() + 
                    (timeString.length() > 0? ", " + timeString : "") +
                    "</name>\n" +
                "    <Icon>\n" +
                "      <href>" + 
                    datasetUrl + ".transparentPng?" + SSR.minimalPercentEncode(userDapQuery) + //XML.encodeAsXML isn't ok
                    "</href>\n" +
                "    </Icon>\n" +
                "    <LatLonBox>\n" +
                "      <west>" + west + "</west>\n" +
                "      <east>" + east + "</east>\n" +
                "      <south>" + south + "</south>\n" +
                "      <north>" + north + "</north>\n" +
                "    </LatLonBox>\n" +
                "    <visibility>1</visibility>\n" +
                "  </GroundOverlay>\n");
        } else { 
            //nTimes >= 2, so make a timeline in Google Earth
            //Problem: I don't know what time range each image represents.
            //  Because I don't know what the timePeriod is for the dataset (e.g., 8day).
            //  And I don't know if the images overlap (e.g., 8day composites, every day)
            //  And if the stride>1, it is further unknown.
            //Solution (crummy): assume an image represents -1/2 time to previous image until 1/2 time till next image

            //get all the .dotConstraints
            String parts[] = getUserQueryParts(userDapQuery); //always at least 1 part (may be "")
            StringBuilder dotConstraintsSB = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].startsWith(".")) {
                    if (dotConstraintsSB.size() > 0)
                        dotConstraintsSB.append("&");
                    dotConstraintsSB.append(parts[i]);
                }
            }        
            String dotConstraints = dotConstraintsSB.toString();

            IntArray tConstraints = (IntArray)gridDataAccessor.constraints().clone();
            int startTimeIndex = tConstraints.get(timeIndex * 3);
            int timeStride     = tConstraints.get(timeIndex * 3 + 1);
            int stopTimeIndex  = tConstraints.get(timeIndex * 3 + 2); 
            double preTime = Double.NaN;
            double nextTime = allTimeDestPa.getDouble(startTimeIndex);
            double currentTime = nextTime - (allTimeDestPa.getDouble(startTimeIndex + timeStride) - nextTime);
            for (int tIndex = startTimeIndex; tIndex <= stopTimeIndex; tIndex += timeStride) {
                preTime = currentTime;
                currentTime = nextTime;
                nextTime = tIndex + timeStride > stopTimeIndex? 
                    currentTime + (currentTime - preTime) :
                    allTimeDestPa.getDouble(tIndex + timeStride);
                //String2.log("  tIndex=" + tIndex + " preT=" + preTime + " curT=" + currentTime + " nextT=" + nextTime);
                //just change the time constraints; leave all others unchanged
                tConstraints.set(timeIndex * 3, tIndex);
                tConstraints.set(timeIndex * 3 + 1, 1);
                tConstraints.set(timeIndex * 3 + 2, tIndex); 
                String tDapQuery = buildDapQuery(tDestinationNames, tConstraints) + dotConstraints;
                writer.write(
                    //the kml link to the data 
                    "  <GroundOverlay>\n" +
                    "    <name>" + Calendar2.epochSecondsToIsoStringT(currentTime) + "Z" + "</name>\n" +
                    "    <Icon>\n" +
                    "      <href>" + 
                        datasetUrl + ".transparentPng?" + SSR.minimalPercentEncode(tDapQuery) + //XML.encodeAsXML isn't ok
                        "</href>\n" +
                    "    </Icon>\n" +
                    "    <LatLonBox>\n" +
                    "      <west>" + west + "</west>\n" +
                    "      <east>" + east + "</east>\n" +
                    "      <south>" + south + "</south>\n" +
                    "      <north>" + north + "</north>\n" +
                    "    </LatLonBox>\n" +
                    "    <TimeSpan>\n" +
                    "      <begin>" + Calendar2.epochSecondsToIsoStringT((preTime + currentTime)  / 2.0) + "Z</begin>\n" +
                    "      <end>"   + Calendar2.epochSecondsToIsoStringT((currentTime + nextTime) / 2.0) + "Z</end>\n" +
                    "    </TimeSpan>\n" +
                    "    <visibility>1</visibility>\n" +
                    "  </GroundOverlay>\n");
            }
        }
        writer.write(
            getKmlIconScreenOverlay() +
            "</Document>\n" +
            "</kml>\n");
        writer.flush(); //essential

        //diagnostic
        if (reallyVerbose)
            String2.log("  EDDGrid.saveAsKml done. TIME=" + 
                (System.currentTimeMillis() - time) + "\n");
    }
    */
    /**
     * This writes the grid from this dataset to the outputStream in 
     * Matlab .mat format.
     * This writes the lon values as they are currently in this grid
     *    (e.g., +-180 or 0..360).
     * If no exception is thrown, the data was successfully written.
     * 
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     * @param userDapQuery an OPeNDAP DAP-style query string, still percent-encoded (shouldn't be null),
     *   e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160]
     * @param outputStreamSource the source of an outputStream (usually already 
     *   buffered) to receive the results.
     *   At the end of this method the outputStream is flushed, not closed.
     * @throws Throwable 
     */
    public void saveAsMatlab(String requestUrl, String userDapQuery, 
        OutputStreamSource outputStreamSource) throws Throwable {

        if (reallyVerbose) String2.log("  EDDGrid.saveAsMatlab");
        long time = System.currentTimeMillis();

        //handle axisDapQuery
        if (isAxisDapQuery(userDapQuery)) {
            //this doesn't write attributes because .mat files don't store attributes
            //get axisDataAccessor first, in case of error when parsing query
            AxisDataAccessor ada = new AxisDataAccessor(this, requestUrl, userDapQuery);

            //make the table
            Table table = new Table();
            int nRAV = ada.nRequestedAxisVariables();
            for (int av = 0; av < nRAV; av++)
                table.addColumn( 
                    ada.axisVariables(av).destinationName(),
                    ada.axisValues(av));
            //don't call table.makeColumnsSameSize();  leave them different lengths

            //then get the modified outputStream
            DataOutputStream dos = new DataOutputStream(outputStreamSource.outputStream(""));
            table.saveAsMatlab(dos, datasetID);
            dos.flush(); //essential

            if (reallyVerbose) String2.log("  EDDGrid.saveAsMatlab axis done.\n");
            return;
        }

        //get gridDataAccessor first, in case of error when parsing query
        GridDataAccessor mainGda = new GridDataAccessor(this, requestUrl, userDapQuery, 
            false, //Matlab is one of the few drivers that needs column-major order
            true); //convertToNaN

        //Make sure no String data and that gridsize isn't > Integer.MAX_VALUE bytes (Matlab's limit)
        EDV tDataVariables[] = mainGda.dataVariables();
        int nAv = axisVariables.length;
        int ntDv = tDataVariables.length;
        byte structureNameInfo[] = Matlab.nameInfo(datasetID); //structure name
        //int largest = 1; //find the largest data item nBytesPerElement
        long cumSize = //see 1-32
            16 + //for array flags
            16 + //my structure is always 2 dimensions 
            structureNameInfo.length +
            8 + //field name length (for all fields)
            8 + (nAv + ntDv) * 32; //field names

        PrimitiveArray avPa[] = new PrimitiveArray[nAv];
        NDimensionalIndex avNDIndex[] = new NDimensionalIndex[nAv];
        for (int av = 0; av < nAv; av++) {
            avPa[av] = mainGda.axisValues[av];
            avNDIndex[av] = Matlab.make2DNDIndex(avPa[av].size());
            cumSize += 8 + Matlab.sizeOfNDimensionalArray( //throws exception if too big for Matlab
                "", //names are done separately
                avPa[av].elementClass(), avNDIndex[av]);
        }

        GridDataAccessor tGda[] = new GridDataAccessor[ntDv];
        NDimensionalIndex dvNDIndex[] = new NDimensionalIndex[ntDv];
        String arrayQuery = buildDapArrayQuery(mainGda.constraints());
        for (int dv = 0; dv < ntDv; dv++) {
            if (tDataVariables[dv].destinationDataTypeClass() == String.class) 
                //can't do String data because you need random access to all values
                //that could be a memory nightmare
                //so just don't allow it
                throw new SimpleException("Error: ERDDAP doesn't support String data in Matlab grid data files.");
            //largest = Math.max(largest, 
            //    tDataVariables[dv].destinationBytesPerElement());

            //make a GridDataAccessor for this dataVariable
            String tUserDapQuery = tDataVariables[dv].destinationName() + arrayQuery;
            tGda[dv] = new GridDataAccessor(this, requestUrl, tUserDapQuery, 
                false, //Matlab is one of the few drivers that needs column-major order
                true); //convertToNaN
            dvNDIndex[dv] = tGda[dv].totalIndex();
            if (dvNDIndex[dv].nDimensions() == 1)
                dvNDIndex[dv] = Matlab.make2DNDIndex(dvNDIndex[dv].shape()[0]);
            
            cumSize += 8 + Matlab.sizeOfNDimensionalArray( //throws exception if too big for Matlab
                "", //names are done separately
                tDataVariables[dv].destinationDataTypeClass(), dvNDIndex[dv]);
        }
        if (cumSize >= Integer.MAX_VALUE - 1000)
            throw new SimpleException(EDStatic.thereIsTooMuchData + 
                " (" + (cumSize / Math2.BytesPerMB) + " MB is more than Matlab's 2GB limit.)"); 
                //"Error: " +
                //"The requested data (" + 
                //(cumSize / Math2.BytesPerMB) + 
                //" MB) is greater than Matlab's limit (" +
                //(Integer.MAX_VALUE / Math2.BytesPerMB) + " MB)."); //safe

        //then get the modified outputStream
        DataOutputStream stream = new DataOutputStream(outputStreamSource.outputStream(""));

        //write the header
        Matlab.writeMatlabHeader(stream);

        //write the miMatrix dataType and nBytes
        stream.writeInt(Matlab.miMATRIX);        //dataType
        stream.writeInt((int)cumSize); //safe since checked above

        //write array flags 
        stream.writeInt(Matlab.miUINT32); //dataType
        stream.writeInt(8);  //fixed nBytes of data
        stream.writeInt(Matlab.mxSTRUCT_CLASS); //array flags  
        stream.writeInt(0); //reserved; ends on 8 byte boundary

        //write structure's dimension array  
        stream.writeInt(Matlab.miINT32); //dataType
        stream.writeInt(2 * 4);  //nBytes
        //matlab docs have 2,1, octave has 1,1. 
        //Think of structure as one row of a table, where elements are entire arrays:  e.g., sst.lon sst.lat sst.sst.
        //Having multidimensions (e.g., 2 here) lets you have additional rows, e.g., sst(2).lon sst(2).lat sst(2).sst.
        //So 1,1 makes sense.
        stream.writeInt(1);  
        stream.writeInt(1);
         
        //write structure name 
        stream.write(structureNameInfo, 0, structureNameInfo.length);

        //write length for all field names (always 32)  (short form)
        stream.writeShort(4);                //nBytes
        stream.writeShort(Matlab.miINT32);   //dataType
        stream.writeInt(32);                 //32 bytes per field name

        //write the structure's field names (each 32 bytes)
        stream.writeInt(Matlab.miINT8);      //dataType
        stream.writeInt((nAv + ntDv) * 32);  //32 bytes per field name
        String nulls = String2.makeString('\u0000', 32);
        for (int av = 0; av < nAv; av++)
            stream.write(String2.toByteArray(
                String2.noLongerThan(axisVariables[av].destinationName(), 31) + nulls), 0, 32);
        for (int dv = 0; dv < ntDv; dv++) 
            stream.write(String2.toByteArray(
                String2.noLongerThan(tDataVariables[dv].destinationName(), 31) + nulls), 0, 32);

        //write the axis miMatrix
        for (int av = 0; av < nAv; av++)
            Matlab.writeNDimensionalArray(stream, "", //name is written above
                avPa[av], avNDIndex[av]);

        //make the data miMatrix
        for (int dv = 0; dv < ntDv; dv++) 
            writeNDimensionalMatlabArray(stream, "",  //name is written above 
                tGda[dv], dvNDIndex[dv]); 

        //this doesn't write attributes because .mat files don't store attributes
        stream.flush(); //essential

        if (reallyVerbose) String2.log("  EDDGrid.saveAsMatlab done. TIME=" + 
            (System.currentTimeMillis() - time) + "\n");
    }

    /**
     * This writes gda's dataVariable[0] as a "matrix" to the Matlab stream.
     *
     * @param stream the stream for the Matlab file
     * @param name usually the destinationName. But inside a Structure, use "".
     * @param gda provides access to just *one* of the data variables. gda must be column-major.
     * @param ndIndex the 2+ dimension NDimensionalIndex
     * @throws Throwable if trouble
     */
    public void writeNDimensionalMatlabArray(DataOutputStream stream, String name, 
        GridDataAccessor gda, NDimensionalIndex ndIndex) throws Throwable {

        if (gda.rowMajor())
            throw new SimpleException("Internal error in " +
                "EDDGrid.writeNDimensionalMatlabArray: the GridDataAccessor must be column-major.");

        //do the first part
        EDV edv = gda.dataVariables()[0];
        Class elementClass = edv.destinationDataTypeClass();
        if (elementClass == String.class) 
            //can't do String data because you need random access to all values
            //that could be a memory nightmare
            //so just don't allow it
            throw new SimpleException("Error: " +
                "Matlab files can't have String data.");
        int nDataBytes = Matlab.writeNDimensionalArray1(stream, name, 
            elementClass, ndIndex);

        //do the second part here 
        //note:  calling increment() on column-major gda returns data in column-major order
        if      (elementClass == double.class) while (gda.increment()) stream.writeDouble(gda.getDataValueAsDouble(0)); 
        else if (elementClass == float.class)  while (gda.increment()) stream.writeFloat( gda.getDataValueAsFloat(0));
        else if (elementClass == long.class)   while (gda.increment()) stream.writeDouble(gda.getDataValueAsDouble(0)); 
        else if (elementClass == int.class)    while (gda.increment()) stream.writeInt(   gda.getDataValueAsInt(0));
        else if (elementClass == short.class)  while (gda.increment()) stream.writeShort( gda.getDataValueAsInt(0));
        else if (elementClass == byte.class)   while (gda.increment()) stream.writeByte(  gda.getDataValueAsInt(0));
        else if (elementClass == char.class)   while (gda.increment()) stream.writeChar(  gda.getDataValueAsInt(0));
        //else if (elementClass == String.class) ...

        //pad data to 8 byte boundary
        int i = nDataBytes % 8;
        while ((i++ % 8) != 0)
            stream.write(0); //0 padded to 8 byte boundary
    }

    /**
     * Save the grid data in a netCDF .nc file.
     * This overwrites any existing file of the specified name.
     * This makes an effort not to create a partial file if there is an error.
     * If no exception is thrown, the file was successfully created.
     * 
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     * @param userDapQuery an OPeNDAP DAP-style query string, still percentEncoded (shouldn't be null). 
     *   e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160]
     * @param fullFileName the name for the file (including directory and extension)
     * @param keepUnusedAxes if true, axes with size=1 will be stored in the file.
     *    If false, axes with size=1 will not be stored in the file (geotiff needs this).
     * @param lonAdjust the value to be added to all lon values (e.g., 0 or -360).
     * @throws Throwable 
     */
    public void saveAsNc(String requestUrl, String userDapQuery, 
        String fullFileName, boolean keepUnusedAxes,
        double lonAdjust) throws Throwable {
        if (reallyVerbose) String2.log("  EDDGrid.saveAsNc"); 
        long time = System.currentTimeMillis();

        //delete any existing file
        File2.delete(fullFileName);

        //POLICY: because this procedure may be used in more than one thread,
        //do work on unique temp files names using randomInt, then rename to proper file name.
        //If procedure fails half way through, there won't be a half-finished file.
        int randomInt = Math2.random(Integer.MAX_VALUE);

        //handle axisDapQuery
        if (isAxisDapQuery(userDapQuery)) {
            //get axisDataAccessor first, in case of error when parsing query
            AxisDataAccessor ada = new AxisDataAccessor(this, requestUrl, userDapQuery);
            int nRAV = ada.nRequestedAxisVariables();

            //write the data
            //items determined by looking at a .nc file; items written in that order 
            NetcdfFileWriteable nc = NetcdfFileWriteable.createNew(fullFileName + randomInt,
                false); //false says: create a new file and don't fill with missing_values
            try {

                //define the dimensions
                Array axisArrays[] = new Array[nRAV];
                for (int av = 0; av < nRAV; av++) {
                    String destName = ada.axisVariables(av).destinationName();
                    PrimitiveArray pa = ada.axisValues(av);
                    Dimension dimension = nc.addDimension(destName, pa.size());
                    axisArrays[av] = Array.factory(
                        pa.elementClass(),
                        new int[]{pa.size()},
                        pa.toObjectArray());
                    nc.addVariable(destName, 
                        NcHelper.getDataType(pa.elementClass()), 
                        new Dimension[]{dimension}); 

                    //write axis attributes
                    NcHelper.setAttributes(nc, destName, ada.axisAttributes(av));
                }

                //write global attributes
                NcHelper.setAttributes(nc, "NC_GLOBAL", ada.globalAttributes());

                //leave "define" mode
                nc.create();

                //write the axis values
                for (int av = 0; av < nRAV; av++) 
                    nc.write(ada.axisVariables(av).destinationName(), axisArrays[av]);

                //if close throws Throwable, it is trouble
                nc.close(); //it calls flush() and doesn't like flush called separately

                //rename the file to the specified name
                File2.rename(fullFileName + randomInt, fullFileName);

                //diagnostic
                if (reallyVerbose) String2.log("  EDDGrid.saveAsNc axis done\n");
                //String2.log(NcHelper.dumpString(directory + name + ext, false));

            } catch (Throwable t) {
                //try to close the file
                try {
                    nc.close(); //it calls flush() and doesn't like flush called separately
                } catch (Throwable t2) {
                    //don't care
                }

                //delete the partial file
                File2.delete(fullFileName);

                throw t;
            }
            return;
        }

        //get gridDataAccessor first, in case of error when parsing query
        //(This makes an unneccessary call to ensureMemoryAvailable with a 
        //  possibly different nBytes than will actually be used below (via dvGda). 
        //  But it isn't an unreasonable request.
        //  nBytes will still be less than partialRequestMaxBytes.)
        GridDataAccessor mainGda = new GridDataAccessor(this, requestUrl, userDapQuery, 
            true, false);  //rowMajor, convertToNaN         
        EDV tDataVariables[] = mainGda.dataVariables();

        //ensure file size < 2GB  
        //???is there a way to allow >2GB netcdf 3 files?
        //   And even if so, what about OS limit ERDDAP is running on? and client OS?
        //Or, view this as protection against accidental requests for too much data (e.g., whole dataset).
        if (mainGda.totalNBytes() > 2100000000) //leave some space for axis vars, etc.
            throw new SimpleException(EDStatic.thereIsTooMuchData + 
                " (" + ((mainGda.totalNBytes() + 100000) / Math2.BytesPerMB) + 
                " MB is more than the .nc file limit of 2 GB.)");

        //write the data
        //items determined by looking at a .nc file; items written in that order 
        NetcdfFileWriteable nc = NetcdfFileWriteable.createNew(fullFileName + randomInt,
            false); //false says: create a new file and don't fill with missing_values
        try {

            //find active axes
            IntArray activeAxes = new IntArray();
            for (int av = 0; av < axisVariables.length; av++) {
                if (keepUnusedAxes || mainGda.axisValues(av).size() > 1)
                    activeAxes.add(av);
            }

            //define the dimensions
            int nActiveAxes = activeAxes.size();
            Dimension dimensions[] = new Dimension[nActiveAxes];
            Array axisArrays[] = new Array[nActiveAxes];
            for (int a = 0; a < nActiveAxes; a++) {
                int av = activeAxes.get(a);
                String avName = axisVariables[av].destinationName();
                PrimitiveArray pa = mainGda.axisValues(av);
                //if (reallyVerbose) String2.log(" create dim=" + avName + " size=" + pa.size());
                dimensions[a] = nc.addDimension(avName, pa.size());
                if (av == lonIndex)
                    pa.scaleAddOffset(1, lonAdjust);
                axisArrays[a] = Array.factory(
                    mainGda.axisValues(av).elementClass(),
                    new int[]{pa.size()},
                    pa.toObjectArray());
                //if (reallyVerbose) String2.log(" create var=" + avName);
                nc.addVariable(avName, 
                    NcHelper.getDataType(pa.elementClass()), 
                    new Dimension[]{dimensions[a]});
            }            

            //define the data variables
            Array dataArrays[] = new Array[tDataVariables.length]; 
            for (int dv = 0; dv < tDataVariables.length; dv++) {
                //if (reallyVerbose) String2.log(" create var=" + tDataVariables[dv].destinationName());
                nc.addVariable(tDataVariables[dv].destinationName(),
                    NcHelper.getDataType(tDataVariables[dv].destinationDataTypeClass()), 
                    dimensions);
            }

            //write global attributes
            NcHelper.setAttributes(nc, "NC_GLOBAL", mainGda.globalAttributes);

            //write axis attributes
            for (int a = 0; a < nActiveAxes; a++) {
                int av = activeAxes.get(a);
                NcHelper.setAttributes(nc, axisVariables[av].destinationName(), 
                    mainGda.axisAttributes[av]);
            }

            //write data attributes
            for (int dv = 0; dv < tDataVariables.length; dv++) {
                NcHelper.setAttributes(nc, tDataVariables[dv].destinationName(), 
                    mainGda.dataAttributes[dv]);
            }

            //leave "define" mode
            nc.create();

            //write the axis variables
            for (int a = 0; a < nActiveAxes; a++) {
                int av = activeAxes.get(a);
                nc.write(axisVariables[av].destinationName(), axisArrays[a]);
            }

            //get the constraints string
            String constraintsString = mainGda.constraintsString();

            //write the data variables
            for (int dv = 0; dv < tDataVariables.length; dv++) {
                long dvTime = System.currentTimeMillis();

                //Read/write chunks
                //(I tried write data values one-by-one, but it is too slow. 
                //Writing takes 10X longer than reading! 9/10 of time is in nc.write(...).)                

                //make a GridDataAccessor for this dv
                EDV edv = tDataVariables[dv];
                String destName = edv.destinationName();
                Class edvClass = edv.destinationDataTypeClass();
                GridDataAccessor dvGda = new GridDataAccessor(this, requestUrl, 
                    edv.destinationName() + constraintsString, 
                    true, false);  //rowMajor, convertToNaN         

                int partialIndexShape[] = dvGda.partialIndex().shape();
                int totalIndexCurrent[] = dvGda.totalIndex().getCurrent();
                long rwTime = System.currentTimeMillis();
                while (dvGda.incrementChunk()) {
                    //make shape with just activeAxes
                    int ncShape[] = new int[nActiveAxes];
                    int ncOffset[] = new int[nActiveAxes];
                    for (int a = 0; a < nActiveAxes; a++) { 
                        int aaa = activeAxes.get(a);
                        ncShape[ a] = partialIndexShape[aaa];
                        ncOffset[a] = totalIndexCurrent[aaa];
                    }
                    if (reallyVerbose)String2.log(
                        "        ncShape=[" + String2.toCSVString(ncShape) + "]\n" +
                        "        ncOffset=[" + String2.toCSVString(ncOffset) + "]");

                    Array array = Array.factory(edvClass, ncShape, 
                        dvGda.getPartialDataValues(0).toObjectArray());
                    nc.write(destName, ncOffset, array);
                    if (reallyVerbose) {
                        String2.log(
                            "        rwTime=" + (System.currentTimeMillis() - rwTime));
                        rwTime = System.currentTimeMillis();
                    }
                }                   

                if (reallyVerbose) String2.log("dv=" + dv + " done. time=" + 
                    (System.currentTimeMillis() - dvTime));
            }

            //if close throws Throwable, it is trouble
            nc.close(); //it calls flush() and doesn't like flush called separately

            //rename the file to the specified name
            File2.rename(fullFileName + randomInt, fullFileName);

            //diagnostic
            if (reallyVerbose) String2.log("  EDDGrid.saveAsNc done.  TIME=" + 
                (System.currentTimeMillis() - time) + "\n");
            //String2.log(NcHelper.dumpString(directory + name + ext, false));

        } catch (Throwable t) {
            //try to close the file
            try {
                nc.close(); //it calls flush() and doesn't like flush called separately
            } catch (Throwable t2) {
                //don't care
            }

            //delete the partial file
            File2.delete(fullFileName);

            throw t;
        }

    }
 
    /**
     * This writes the grid data to the outputStream in comma-separated-value 
     * ASCII format.
     * If no exception is thrown, the data was successfully written.
     * 
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     * @param userDapQuery an OPeNDAP DAP-style query string, still percentEncoded (shouldn't be null). 
     *   e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160]
     * @param outputStreamSource the source of an outputStream (usually already 
     *   buffered) to receive the results.
     *   At the end of this method the outputStream is flushed, not closed.
     * @param writeUnits  '0'=no, 
     *    '('=on the first line as "variableName (units)" (if present),
     *    2=on the second line.
     * @throws Throwable  if trouble. 
     */
    public void saveAsCsv(String requestUrl, String userDapQuery, 
        OutputStreamSource outputStreamSource, char writeUnits) throws Throwable {

        saveAsSeparatedAscii(requestUrl, userDapQuery, outputStreamSource, ", ", true,  //true=quoted
            writeUnits);
    }

    /**
     * This writes the grid data to the outputStream in tab-separated-value 
     * ASCII format.
     * If no exception is thrown, the data was successfully written.
     * 
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     * @param userDapQuery an OPeNDAP DAP-style query string, still percentEncoded (shouldn't be null). 
     *   e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160]
     * @param outputStreamSource the source of an outputStream (usually already 
     *   buffered) to receive the results.
     *   At the end of this method the outputStream is flushed, not closed.
     * @param writeUnits  '0'=no, 
     *    '('=on the first line as "variableName (units)" (if present),
     *    2=on the second line.
     * @throws Throwable  if trouble. 
     */
    public void saveAsTsv(String requestUrl, String userDapQuery, 
        OutputStreamSource outputStreamSource, char writeUnits) throws Throwable {

        saveAsSeparatedAscii(requestUrl, userDapQuery, outputStreamSource, "\t", false, //false = !quoted
            writeUnits);
    }

    /**
     * This writes the axis or grid data to the outputStream in a separated-value 
     * ASCII format.
     * If no exception is thrown, the data was successfully written.
     * 
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     * @param userDapQuery an OPeNDAP DAP-style query string, still percentEncoded (shouldn't be null). 
     *   e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160]
     * @param outputStreamSource the source of an outputStream (usually already 
     *   buffered) to receive the results.
     *   At the end of this method the outputStream is flushed, not closed.
     * @param separator
     * @param quoted if true, String values are enclosed in double quotes
     *   and internal double quotes are converted to 2 double quotes.
     * @param writeUnits  '0'=no, 
     *    '('=on the first line as "variableName (units)" (if present),
     *    2=on the second line.
     * @throws Throwable  if trouble. 
     */
    public void saveAsSeparatedAscii(String requestUrl, String userDapQuery, 
        OutputStreamSource outputStreamSource,
        String separator, boolean quoted, char writeUnits) throws Throwable {

        if (reallyVerbose) String2.log("  EDDGrid.saveAsSeparatedAscii separator=\"" + 
            String2.annotatedString(separator) + "\""); 
        long time = System.currentTimeMillis();

        //get dataAccessor first, in case of error when parsing query
        boolean isAxisDapQuery = isAxisDapQuery(userDapQuery);
        AxisDataAccessor ada = null;
        GridDataAccessor gda = null;
        if (isAxisDapQuery) 
             ada = new AxisDataAccessor(this, requestUrl, userDapQuery);
        else gda = new GridDataAccessor(this, requestUrl, userDapQuery, 
            true, false);   //rowMajor, convertToNaN (would be true, but TableWriterSeparatedValue will do it)

        //write the data to the tableWriter
        TableWriter tw = new TableWriterSeparatedValue(outputStreamSource, 
            separator, quoted, writeUnits, "NaN");
        if (isAxisDapQuery) 
             saveAsTableWriter(ada, tw);
        else saveAsTableWriter(gda, tw);

        //diagnostic
        if (reallyVerbose)
            String2.log("  EDDGrid.saveAsSeparatedAscii done. TIME=" + 
                (System.currentTimeMillis() - time) + "\n");

    }

    /**
     * This writes grid data (not just axis data) to the outputStream in an
     * ODV Generic Spreadsheet Format .txt file.
     * If no exception is thrown, the data was successfully written.
     * 
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     * @param userDapQuery an OPeNDAP DAP-style query string, still percentEncoded (shouldn't be null). 
     *   e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160]
     * @param outputStreamSource the source of an outputStream (usually already 
     *   buffered) to receive the results.
     *   At the end of this method the outputStream is flushed, not closed.
     * @throws Throwable  if trouble. 
     */
    public void saveAsODV(String requestUrl, String userDapQuery, 
        OutputStreamSource outputStreamSource) throws Throwable {
        //FUTURE: it might be nice if this prevented a user from getting 
        //a very high resolution subset (wasted in ODV) by reducing the 
        //resolution automatically.

        if (reallyVerbose) String2.log("  EDDGrid.saveAsODV"); 
        long time = System.currentTimeMillis();

        //do quick error checking
        if (isAxisDapQuery(userDapQuery))
            throw new SimpleException("You can't save just axis data in on ODV .txt file. " +
                "Please select a subset of a data variable.");
        if (lonIndex < 0 || latIndex < 0 || timeIndex < 0) 
            throw new SimpleException("Data for an ODV .txt file MUST include longitude, " +
                "latitude, and time axes.");
        //lon can be +-180 or 0-360. See EDDTable.saveAsODV

        //get dataAccessor first, in case of error when parsing query
        GridDataAccessor gda = new GridDataAccessor(this, requestUrl, userDapQuery, 
            true, false);   //rowMajor, convertToNaN (EDDTable.saveAsODV handles convertToNaN)

        //write the data to the tableWriterAllWithMetadata
        TableWriterAllWithMetadata twawm = new TableWriterAllWithMetadata(
            EDStatic.fullCacheDirectory + datasetID + "/",
            "ODV"); //A random number will be added to it for safety.
        saveAsTableWriter(gda, twawm);

        //write the ODV .txt file
        EDDTable.saveAsODV(outputStreamSource, twawm, datasetID, publicSourceUrl(), 
            infoUrl());

        //diagnostic
        if (reallyVerbose)
            String2.log("  EDDGrid.saveAsODV done. TIME=" + 
                (System.currentTimeMillis() - time) + "\n");

    }

    /**
     * This gets the data for the userDapQuery and writes the data 
     * to the outputStream as an html or xhtml table.
     * See TableWriterHtml for details.
     * 
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     * @param userDapQuery an OPeNDAP DAP-style query string, still percentEncoded (shouldn't be null). 
     *   for a axis data, e.g., time[40:45], or 
     *   for a grid data, e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160]
     * @param outputStreamSource the source of an outputStream (usually already 
     *   buffered) to receive the results.
     *   At the end of this method the outputStream is flushed, not closed.
     * @param fileName (no extension) used for the document title
     * @param xhtmlMode if true, the table is stored as an XHTML table.
     *   If false, it is stored as an HTML table.
     * @param preTableHtml is html or xhtml text to be inserted at the start of the 
     *   body of the document, before the table tag
     *   (or "" if none).
     * @param postTableHtml is html or xhtml text to be inserted at the end of the 
     *   body of the document, after the table tag
     *   (or "" if none).
     * @throws Throwable  if trouble. 
     */
    public void saveAsHtmlTable(String loggedInAs, String requestUrl, String userDapQuery, 
        OutputStreamSource outputStreamSource, 
        String fileName, boolean xhtmlMode, String preTableHtml, String postTableHtml) 
        throws Throwable {

        if (reallyVerbose) String2.log("  EDDGrid.saveAsHtmlTable"); 
        long time = System.currentTimeMillis();

        //get dataAccessor first, in case of error when parsing query
        boolean isAxisDapQuery = isAxisDapQuery(userDapQuery);
        AxisDataAccessor ada = null;
        GridDataAccessor gda = null;
        if (isAxisDapQuery) 
             ada = new AxisDataAccessor(this, requestUrl, userDapQuery);
        else gda = new GridDataAccessor(this, requestUrl, userDapQuery, 
            true, false);   //rowMajor, convertToNaN  (would be true, but TableWriterHtmlTable will do it)

        //write the data to the tableWriter
        TableWriter tw = new TableWriterHtmlTable(loggedInAs, outputStreamSource,
            true, fileName, xhtmlMode, preTableHtml, postTableHtml, 
            true, true); //tencodeAsHTML, tWriteUnits 
        if (isAxisDapQuery) 
             saveAsTableWriter(ada, tw);
        else saveAsTableWriter(gda, tw);

        //diagnostic
        if (reallyVerbose)
            String2.log("  EDDGrid.saveAsHtmlTable done. TIME=" + 
                (System.currentTimeMillis() - time) + "\n");
    }


    /**
     * This gets the axis data for the userDapQuery and writes the data to 
     * a tableWriter.
     * This writes the lon values as they are in the source
     *    (e.g., +-180 or 0..360). 
     * If no exception is thrown, the data was successfully written.
     * 
     * @param ada The source of data to be written to the tableWriter.
     * @param tw  This calls tw.finish() at the end.
     * @throws Throwable  if trouble. 
     */
    public void saveAsTableWriter(AxisDataAccessor ada, TableWriter tw) 
        throws Throwable {

        //make the table
        //note that TableWriter expects time values as doubles, and (sometimes) displays them as ISO 8601 strings
        Table table = new Table();
        int nRAV = ada.nRequestedAxisVariables();
        for (int av = 0; av < nRAV; av++) {
            table.addColumn(ada.axisVariables(av).destinationName(), ada.axisValues(av));
            String tUnits = ada.axisVariables(av).units(); //ok if null
            if (tUnits != null) 
                table.columnAttributes(av).set("units", tUnits);
        }
        table.makeColumnsSameSize();

        //write the table
        tw.writeAllAndFinish(table);
    }

    /**
     * This gets the data for the userDapQuery and writes the data to 
     * a TableWriter.
     * This writes the lon values as they are in the source
     *    (e.g., +-180 or 0..360). 
     *    //note that TableWriter expects time values as doubles, and displays them as ISO 8601 strings
     * If no exception is thrown, the data was successfully written.
     * 
     * @param gridDataAccessor The source of data to be written to the tableWriter.
     *    Missing values should be as they are in the source.
     *    Some tableWriter's convert them to other values (e.g., NaN).
     * @param tw  This calls tw.finish() at the end.
     * @throws Throwable  if trouble. 
     */
    public void saveAsTableWriter(GridDataAccessor gridDataAccessor, 
        TableWriter tw) throws Throwable {

        //create the table (with one dummy row of data)
        Table table = new Table();
        int nAv = axisVariables.length;
        EDV queryDataVariables[] = gridDataAccessor.dataVariables();
        int nDv = queryDataVariables.length;
        PrimitiveArray avPa[] = new PrimitiveArray[nAv];
        boolean isDoubleAv[] = new boolean[nAv];
        boolean isFloatAv[]  = new boolean[nAv];
        PrimitiveArray dvPa[] = new PrimitiveArray[nDv];
        boolean isStringDv[] = new boolean[nDv];
        boolean isDoubleDv[] = new boolean[nDv];
        boolean isFloatDv[]  = new boolean[nDv];
        int nBufferRows = 1000;
        for (int av = 0; av < nAv; av++) {
            EDV edv = axisVariables[av];
            Class tClass = edv.destinationDataTypeClass();
            isDoubleAv[av] = tClass == double.class || tClass == long.class;
            isFloatAv[av]  = tClass == float.class;
            avPa[av] = PrimitiveArray.factory(tClass, nBufferRows, false);
            //???need to remove file-specific metadata (e.g., actual_range) from Attributes clone?
            table.addColumn(av, edv.destinationName(), avPa[av], 
                (Attributes)edv.combinedAttributes().clone());
        }
        for (int dv = 0; dv < nDv; dv++) {
            EDV edv = queryDataVariables[dv];
            Class tClass = edv.destinationDataTypeClass();
            isStringDv[dv] = tClass == String.class;
            isDoubleDv[dv] = tClass == double.class || tClass == long.class;
            isFloatDv[dv]  = tClass == float.class;
            dvPa[dv] = PrimitiveArray.factory(tClass, nBufferRows, false);
            //???need to remove file-specific metadata (e.g., actual_range) from Attributes clone?
            table.addColumn(nAv + dv, edv.destinationName(), dvPa[dv], 
                (Attributes)edv.combinedAttributes().clone());
        }

        //write the data
        int tRows = 0;
        while (gridDataAccessor.increment()) {
            //put the data in row one of the table
            for (int av = 0; av < nAv; av++) {
                if      (isDoubleAv[av]) avPa[av].addDouble(gridDataAccessor.getAxisValueAsDouble(av));
                else if (isFloatAv[av])  avPa[av].addFloat( gridDataAccessor.getAxisValueAsFloat(av));
                else                     avPa[av].addInt(   gridDataAccessor.getAxisValueAsInt(av));
            }

            for (int dv = 0; dv < nDv; dv++) {
                if      (isStringDv[dv]) dvPa[dv].addString(gridDataAccessor.getDataValueAsString(dv));
                else if (isDoubleDv[dv]) dvPa[dv].addDouble(gridDataAccessor.getDataValueAsDouble(dv));
                else if (isFloatDv[dv])  dvPa[dv].addFloat( gridDataAccessor.getDataValueAsFloat(dv));
                else                     dvPa[dv].addInt(   gridDataAccessor.getDataValueAsInt(dv));
            }
            tRows++;

            //write the table 
            if (tRows >= nBufferRows) {
                tw.writeSome(table);
                table.removeAllRows();
                tRows = 0;
            }
        }
        if (tRows > 0) 
            tw.writeSome(table);
        tw.finish();

    }

    /**
     * This writes an HTML form requesting info from this dataset (like the OPeNDAP Data Access forms).
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in).
     *   Normally, this is not used to test if this edd is accessibleTo loggedInAs, 
     *   but it unusual cases (EDDTableFromPost?) it could be.
     *   Normally, this is just used to determine which erddapUrl to use (http vs https).
     * @param userDapQuery the part after the '?', still percentEncoded (shouldn't be null).
     * @param writer
     * @throws Throwable if trouble
     */
    public void writeDapHtmlForm(String loggedInAs,
        String userDapQuery, Writer writer) throws Throwable {

        //parse userDapQuery 
        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        if (userDapQuery == null)
            userDapQuery = "";
        userDapQuery = userDapQuery.trim();
        StringArray destinationNames = new StringArray();
        IntArray constraints = new IntArray();
        boolean isAxisDapQuery = false; //only true if userDapQuery.length() > 0 and is axisDapQuery
        if (userDapQuery.length() > 0) {
            try {
                isAxisDapQuery = isAxisDapQuery(userDapQuery);
                if (isAxisDapQuery) 
                     parseAxisDapQuery(userDapQuery, destinationNames, constraints, true); 
                else parseDataDapQuery(userDapQuery, destinationNames, constraints, true); 
            } catch (Throwable t) {
                String2.log(MustBe.throwableToString(t));
                userDapQuery = ""; //as if no userDapQuery
            }
        }


        //beginning of form   ("form1" is used in javascript below")
        HtmlWidgets widgets = new HtmlWidgets("", true, EDStatic.imageDirUrl(loggedInAs));
        String formName = "form1";
        EDVTimeGridAxis timeVar = timeIndex >= 0? (EDVTimeGridAxis)axisVariables[timeIndex] : null;
        String liClickSubmit = "\n" +
            "  <li> " + EDStatic.EDDClickOnSubmitHtml + "\n" +
            "  </ol>\n";
        writer.write("&nbsp;\n"); //necessary for the blank line before the form (not <p>)
        writer.write(widgets.beginForm(formName, "GET", "", ""));

        //begin table  ("width=60%" doesn't keep table tight; I wish it did)
        writer.write(widgets.beginTable(0, 0, "")); 

        //write the table's column names   
        String dimHelp = EDStatic.EDDGridDimensionHtml + "\n<br>";
        String sss = dimHelp +
            EDStatic.EDDGridSSSHtml + "\n<br>";
        String startTooltip  = sss + EDStatic.EDDGridStartHtml;
        String stopTooltip   = sss + EDStatic.EDDGridStopHtml;
        String strideTooltip = sss + EDStatic.EDDGridStrideHtml;
        String downloadTooltip = EDStatic.EDDGridDownloadTooltipHtml;
        String gap = "&nbsp;&nbsp;&nbsp;";
        writer.write(
            "<tr>\n" +
            "  <th nowrap align=\"left\">" + EDStatic.EDDGridDimension + " " + 
                EDStatic.htmlTooltipImage(loggedInAs, dimHelp + 
                    EDStatic.EDDGridVarHasDimHtml) + " </th>\n" +
            "  <th nowrap align=\"left\">" + EDStatic.EDDGridStart  + " " + EDStatic.htmlTooltipImage(loggedInAs, startTooltip)  + " </th>\n" +
            "  <th nowrap align=\"left\">" + EDStatic.EDDGridStride + " " + EDStatic.htmlTooltipImage(loggedInAs, strideTooltip) + " </th>\n" +
            "  <th nowrap align=\"left\">" + EDStatic.EDDGridStop   + " " + EDStatic.htmlTooltipImage(loggedInAs, stopTooltip)   + " </th>\n" +
            //"  <th nowrap align=\"left\">&nbsp;" + EDStatic.EDDGridFirst + " " + 
            //    EDStatic.htmlTooltipImage(loggedInAs, EDStatic.EDDGridDimensionFirstHtml) + "</th>\n" +
            "  <th nowrap align=\"left\">&nbsp;" + EDStatic.EDDGridNValues + " " + 
                EDStatic.htmlTooltipImage(loggedInAs, EDStatic.EDDGridNValuesHtml) + "</th>\n" +
            "  <th nowrap align=\"left\">" + gap + EDStatic.EDDGridSpacing + " " + 
                EDStatic.htmlTooltipImage(loggedInAs, EDStatic.EDDGridSpacingHtml) + "</th>\n" +
            //"  <th nowrap align=\"left\">" + gap + EDStatic.EDDGridLast + " " +
            //    EDStatic.htmlTooltipImage(loggedInAs, EDStatic.EDDGridDimensionLastHtml) + "</th>\n" +
            "</tr>\n");

        //a row for each axisVariable
        int nAv = axisVariables.length;
        String sliderFromNames[] = new String[nAv];
        String sliderToNames[] = new String[nAv];
        int sliderNThumbs[] = new int[nAv];
        String sliderUserValuesCsvs[] = new String[nAv];
        int sliderInitFromPositions[] = new int[nAv];
        int sliderInitToPositions[] = new int[nAv];
        for (int av = 0; av < nAv; av++) {
            EDVGridAxis edvga = axisVariables[av];
            int sourceSize = edvga.sourceValues().size();
            writer.write("<tr>\n");
            
            //get the extra info   
            String extra = edvga.units();
            if (av == timeIndex)
                extra = "UTC"; //no longer true: "seconds since 1970-01-01..."
            if (extra == null) 
                extra = "";
            if (showLongName(edvga.destinationName(), edvga.longName()))
                extra = edvga.longName() + (extra.length() == 0? "" : ", " + extra);
            if (extra.length() > 0) 
                extra = " (" + extra + ")";

            //variables: checkbox destName (longName, extra) 
            writer.write("  <td nowrap>\n");
            writer.write(widgets.checkbox("avar" + av, downloadTooltip,  
                userDapQuery.length() > 0 && isAxisDapQuery? 
                    destinationNames.indexOf(edvga.destinationName()) >= 0 : 
                    true, 
                edvga.destinationName(), edvga.destinationName(), ""));

            writer.write(extra + " ");
            writer.write(EDStatic.htmlTooltipImageEDVGA(loggedInAs, edvga));           
            writer.write("&nbsp;</td>\n");

            //set default start, stride, stop                       
            int tStarti = av == timeIndex? sourceSize - 1 : 0;
            int tStopi  = sourceSize - 1;
            double tdv = av == timeIndex? edvga.destinationMax() : //yes, time max, to limit time
                                          edvga.firstDestinationValue();
            String tStart = edvga.destinationToString(tdv);
            String tStride = "1";
            tdv = av == timeIndex? edvga.destinationMax() : 
                                   edvga.lastDestinationValue();
            String tStop = edvga.destinationToString(tdv);
 
            //if possible, override defaults via userDapQuery
            if (userDapQuery.length() > 0) {
                int tAv = isAxisDapQuery?
                    destinationNames.indexOf(edvga.destinationName()) :
                    av;
                if (tAv >= 0) {
                    tStarti = constraints.get(3*tAv + 0);
                    tStopi  = constraints.get(3*tAv + 2);
                    tStart  = edvga.destinationToString(edvga.destinationValue(tStarti).getNiceDouble(0));
                    tStride = "" + constraints.get(3*tAv + 1);
                    tStop   = edvga.destinationToString(edvga.destinationValue(tStopi).getNiceDouble(0));
                }
            }

            //start
            String edvgaTooltip = edvga.htmlRangeTooltip();
            writer.write("  <td nowrap>");
            //new style: textfield
            writer.write(widgets.textField("start" + av, edvgaTooltip, 19, 30,  tStart, ""));
            //old style: select; very slow in IE 7
            //PrimitiveArray destValues = edvga.destinationStringValues();
            ////StringArray destValues = new StringArray(tdestValues);
            ////int maxLength = destValues.maxStringLength();
            ////String tStyle = ""; //"style=\"width:" + (maxLength*0.6) + "em\"";
            //writer.write(widgets.select("start" + av, startTooltip, 
            //    HtmlWidgets.BUTTONS_0n + HtmlWidgets.BUTTONS_1000,
            //    destValues, 
            //    av == timeIndex? destValues.size() - 1 : 0, tStyle));

            writer.write("  </td>\n");

            //stride
            writer.write("  <td nowrap>"); // + gap);
            writer.write(widgets.textField("stride" + av, edvgaTooltip, 7, 10, tStride, ""));
            writer.write("  " + //gap + 
                "</td>\n");

            //stop
            writer.write("  <td nowrap>");
            //new style: textfield
            writer.write(widgets.textField("stop" + av, edvgaTooltip, 19, 30, tStop, ""));
            //old style: select; very slow in IE 7
            //writer.write(widgets.select("stop" + av, stopTooltip, 
            //    HtmlWidgets.BUTTONS_0n + HtmlWidgets.BUTTONS_1000,
            //    destValues, 
            //    av == latIndex || av == lonIndex || av == timeIndex? destValues.size() - 1 : 0, 
            //    tStyle));
            writer.write("  </td>\n");

            //first
            //writer.write("  <td nowrap>&nbsp;" + 
            //    edvga.destinationToString(edvga.firstDestinationValue()) + "</td>\n");

            //n Values
            writer.write("  <td nowrap>" + gap + sourceSize + "</td>\n");

            //spacing
            writer.write("  <td nowrap>" + gap + edvga.spacingDescription() + "</td>\n");

            //last
            //writer.write("  <td nowrap>" + gap +
            //    edvga.destinationToString(edvga.lastDestinationValue()) +
            //    "</td>\n");

            //end of row
            writer.write("</tr>\n");

            // *** and a slider for this axis  (Data Access Form)
            sliderFromNames[av] = formName + ".start" + av;
            sliderToNames[  av] = formName + ".stop"  + av;
            int safeSourceSize1 = Math.max(1, sourceSize - 1);
            //if (sourceSize == 1) {
            //    sliderNThumbs[av] = 0;
            //    sliderUserValuesCsvs[av] = "";
            //    sliderInitFromPositions[av] = 0;
            //    sliderInitToPositions[av] = 0;
            //} else {
                sliderNThumbs[av] = 2;
                sliderUserValuesCsvs[av] = edvga.sliderCsvValues();
                sliderInitFromPositions[av] = Math2.roundToInt((tStarti * (EDV.SLIDER_PIXELS - 1.0)) / safeSourceSize1);
                sliderInitToPositions[  av] = sourceSize == 1? EDV.SLIDER_PIXELS - 1 :
                                              Math2.roundToInt((tStopi  * (EDV.SLIDER_PIXELS - 1.0)) / safeSourceSize1);
                writer.write(
                    "<tr align=\"left\">\n" +
                    "  <td nowrap colspan=\"6\" align=\"left\">\n" +
                    widgets.spacer(10, 1, "align=\"left\"") +
                    widgets.dualSlider(av, EDV.SLIDER_PIXELS - 1, "align=\"left\"") +
                    "  </td>\n" +
                    "</tr>\n");
            //}
        }

        writer.write(
            "<tr>\n" +
            "  <td colspan=\"4\" align=\"left\">&nbsp;<br>" + EDStatic.EDDGridGridVariableHtml + "</td>\n" +
            "</tr>\n");

        //a row for each dataVariable
        for (int dv = 0; dv < dataVariables.length; dv++) {
            EDV edv = dataVariables[dv];
            writer.write("<tr>\n");
            
            //get the extra info   
            String extra = edv.units();
            if (extra == null) 
                extra = "";
            if (showLongName(edv.destinationName(), edv.longName()))
                extra = edv.longName() + (extra.length() == 0? "" : ", " + extra);
            if (extra.length() > 0) 
                extra = " (" + extra + ")";

            //variables: checkbox destName (longName, units)
            writer.write("  <td nowrap colspan=\"4\">\n");
            writer.write(widgets.checkbox("dvar" + dv, downloadTooltip,  
                userDapQuery.length() > 0 && isAxisDapQuery? false :
                    userDapQuery.length() > 0? destinationNames.indexOf(edv.destinationName()) >= 0 : 
                    true,  
                edv.destinationName(), edv.destinationName(), ""));

            writer.write(extra + " ");
            writer.write(EDStatic.htmlTooltipImageEDVG(loggedInAs, edv, allDimString()));           
            writer.write("</td>\n");

            //end of row
            writer.write("</tr>\n");
        }

        //end of table
        writer.write(widgets.endTable());

        //fileType
        writer.write("<p><b>" + EDStatic.EDDFileType + "</b>\n");
        writer.write(widgets.select("fileType", EDStatic.EDDSelectFileType, 1,
            allFileTypeOptions, defaultFileTypeOption, ""));
        writer.write(" <a href=\"" + tErddapUrl + "/griddap/documentation.html#fileType\">more&nbsp;info</a>\n");

        //generate the javaScript
        String javaScript = 
            "var result = \"\";\n" +
            "try {\n" +
            "  var ft = form1.fileType.options[form1.fileType.selectedIndex].text;\n" +
            "  var start = \"" + tErddapUrl + "/griddap/" + datasetID + 
              "\" + ft.substring(0, ft.indexOf(\" - \")) + \"?\";\n" +
            "  var sss = new Array(); var cum = \"\"; var done = false;\n" +

            //gather startStrideStop and cumulative sss
            "  for (var av = 0; av < " + nAv + "; av++) {\n" +
            "    sss[av] = \"[(\" + " + 
                   "eval(\"form1.start\"  + av + \".value\") + \"):\" + " +                   
                   //"eval(\"form1.start\"  + av + \".options[form1.start\"  + av + \".selectedIndex].text\") + \"):\" + " +
                   "eval(\"form1.stride\" + av + \".value\") + \":(\" + " +
                   "eval(\"form1.stop\"   + av + \".value\") + \")]\";\n" +                   
                   //"eval(\"form1.stop\"   + av + \".options[form1.stop\"   + av + \".selectedIndex].text\") + \")]\";\n" +
            "    cum += sss[av];\n" +
            "  }\n" +

            //see if any dataVars were selected
            "  for (var dv = 0; dv < " + dataVariables.length + "; dv++) {\n" +
            "    if (eval(\"form1.dvar\" + dv + \".checked\")) {\n" +
            "      if (result.length > 0) result += \",\";\n" +
            "      result += (eval(\"form1.dvar\" + dv + \".value\")) + cum; }\n" +
            "  }\n" +

            //else find selected axes
            "  if (result.length > 0) {\n" +
            "    result = start + result;\n" +
            "  } else {\n" +
            "    result = start;\n" +
            "    for (var av = 0; av < " + nAv + "; av++) {\n" +
            "      if (eval(\"form1.avar\" + av + \".checked\")) { \n" +
            "        if (result.length > start.length) result += \",\";\n" +
            "        result += (eval(\"form1.avar\" + av + \".value\")) + sss[av];\n" +
            "      }\n" +
            "    }\n" +
            "    if (result.length == start.length) { result = \"\";\n" +
            "      throw new Error(\"Please select (check) one of the variables.\"); }\n" +
            "  }\n" +
            "} catch (e) {alert(e);}\n" +  //result is in 'result'  (or "" if error or nothing selected)
            "form1.tUrl.value = result;\n";

        //just generate URL
        writer.write("<br>"); 
        String genViewHtml = String2.replaceAll(EDStatic.justGenerateAndViewHtml, "&protocolName;", dapProtocol);
        writer.write(widgets.button("button", "getUrl", genViewHtml, 
            EDStatic.justGenerateAndView, 
            //"class=\"skinny\" " + //only IE needs it but only IE ignores it
            "onclick='" + javaScript + "'"));
        writer.write(widgets.textField("tUrl", 
            EDStatic.justGenerateAndViewUrl,
            70, 1000, "", ""));
        writer.write("<a href=\"" + tErddapUrl + "/griddap/documentation.html\" " +
            "title=\"griddap documentation\">Documentation&nbsp;/&nbsp;Bypass&nbsp;this&nbsp;form</a>\n" +
            EDStatic.htmlTooltipImage(loggedInAs, genViewHtml));

        //submit
        writer.write("<br>"); 
        writer.write(
            widgets.htmlButton("button", "submit1", 
                "", EDStatic.submitTooltip, "<b>" + EDStatic.submit + "</b>", 
                "onclick='" + javaScript +
                "if (result.length > 0) window.location=result;\n" + //or open a new window: window.open(result);\n" +
                "'") +
            " " + EDStatic.patientData + "\n");

        //end of form
        writer.write(widgets.endForm());
        writer.write(HtmlWidgets.ifJavaScriptDisabled);
        writer.write("<br>&nbsp;\n");

        //the javascript for the sliders
        writer.write(widgets.sliderScript(sliderFromNames, sliderToNames, 
            sliderNThumbs, sliderUserValuesCsvs, 
            sliderInitFromPositions, sliderInitToPositions, EDV.SLIDER_PIXELS - 1));

        writer.flush(); //be nice   

    }

    /**
     * This writes HTML info on forming OPeNDAP DAP-style requests for this type of dataset.
     *
     * @param tErddapUrl  from EDStatic.erddapUrl(loggedInAs)  (erddapUrl, or erddapHttpsUrl if user is logged in)
     * @param writer to which will be written HTML info on forming OPeNDAP DAP-style 
     *  requests for this type of dataset.
     * @param complete if false, this just writes a paragraph and shows a link
     *    to [protocol]/documentation.html
     * @throws Throwable if trouble
     */
    public static void writeGeneralDapHtmlInstructions(String tErddapUrl, 
        Writer writer, boolean complete) throws Throwable {

        String dapBase = tErddapUrl + "/" + dapProtocol + "/";
        String datasetBase = dapBase + EDStatic.EDDGridIdExample;
        String ddsExample           = datasetBase + ".dds";
        String dds1VarExample       = datasetBase + ".dds?" + EDStatic.EDDGridNoHyperExample;
        //all of the fullXxx examples are pre-encoded
        String fullDimensionExample = XML.encodeAsHTML(datasetBase + ".htmlTable?" + EDStatic.EDDGridDimensionExample);
        String fullIndexExample     = XML.encodeAsHTML(datasetBase + ".htmlTable?" + EDStatic.EDDGridDataIndexExample);
        String fullValueExample     = XML.encodeAsHTML(datasetBase + ".htmlTable?" + EDStatic.EDDGridDataValueExample);
        String fullTimeExample      = XML.encodeAsHTML(datasetBase + ".htmlTable?" + EDStatic.EDDGridDataTimeExample);
        String fullTimeCsvExample   = XML.encodeAsHTML(datasetBase + ".csv?"       + EDStatic.EDDGridMapExample);
        String fullTimeNcExample    = XML.encodeAsHTML(datasetBase + ".nc?"        + EDStatic.EDDGridMapExample);
        String fullGraphExample     = XML.encodeAsHTML(datasetBase + ".png?"       + EDStatic.EDDGridGraphExample);
        String fullGraphMAGExample  = XML.encodeAsHTML(datasetBase + ".graph?"     + EDStatic.EDDGridGraphExample);
        String fullGraphDataExample = XML.encodeAsHTML(datasetBase + ".htmlTable?" + EDStatic.EDDGridGraphExample);
        String fullMapExample       = XML.encodeAsHTML(datasetBase + ".png?"       + EDStatic.EDDGridMapExample);
        String fullMapMAGExample    = XML.encodeAsHTML(datasetBase + ".graph?"     + EDStatic.EDDGridMapExample);
        String fullMapDataExample   = XML.encodeAsHTML(datasetBase + ".htmlTable?" + EDStatic.EDDGridMapExample);
        String fullMatExample       = XML.encodeAsHTML(datasetBase + ".mat?"       + EDStatic.EDDGridMapExample);

        writer.write(
            "<h2><a name=\"instructions\">Using</a> griddap to Request Data and Graphs from Gridded Datasets</h2>\n" +
            longDapDescription(tErddapUrl) +
            "<p><b>griddap request URLs must be in the form</b>\n" +
            "<br>&nbsp;&nbsp;&nbsp;<tt>" + dapBase +  
                "<i><a href=\"" + dapBase + "#datasetID\">datasetID</a></i>." + 
                "<i><a href=\"" + dapBase + "#fileType\">fileType</a></i>{?" + 
                "<i><a href=\"" + dapBase + "#query\">query</a></i>}</tt>\n" +
            "<br>For example,\n" +
            "<br>&nbsp;&nbsp;&nbsp;<a href=\"" + fullTimeExample + "\"><tt>" + 
                                                 fullTimeExample + "</tt></a>\n" +
            "<br>Thus, the query is often a variable name (e.g., <tt>" + EDStatic.EDDGridNoHyperExample + "</tt>),\n" +
            "<br>followed by a set of [] for each of the variable's dimensions\n" +
            "<br>(for example, <tt>" + EDStatic.EDDGridDimNamesExample + "</tt>),\n" +
            "<br>with your desired <tt>(<i>start</i>):<i>stride</i>:(<i>stop</i>)</tt> values within each []. \n" +
            "\n");

        if (!complete) {
            writer.write(
            "<p>For details, see the <a href=\"" + dapBase + 
                "documentation.html\">" + dapProtocol + " Documentation</a>.\n");
            return;
        }
         
        //details
        writer.write(
            "<p><b>Details:</b><ul>\n" +
            "<li>Requests must not have any internal spaces.\n"+
            "<li>Requests are case sensitive.\n" +
            "<li>{} is notation to denote an optional part of the request.\n" + 
            "  <br>&nbsp;\n" +

            //datasetID
            "<li><a name=\"datasetID\"><b>datasetID</b></a> identifies the name that ERDDAP\n" +
            "  assigned to the dataset (for example, <tt>" + EDStatic.EDDGridIdExample + "</tt>). \n" +
            "  <br>You can see a list of " +
              "<a href=\"" + tErddapUrl + "/" + dapProtocol + 
                  "/index.html\">datasetID options available via griddap</a>.\n" +
            "  <br>&nbsp;\n" +

            //fileType
            "<li><a name=\"fileType\"><b>fileType</b></a> specifies the type of grid data file that you want " +
            "  to download (for example, <tt>.htmlTable</tt>).\n" +
            "  <br>The actual extension of the resulting file may be slightly different than the fileType (for example,\n" +
            "  <br><tt>.smallPdf</tt> returns a small .pdf file). \n" +
            "  <br>The fileType options for downloading gridded data are:\n" +
            "  <br>&nbsp;\n" +
            "  <table class=\"erd\" cellspacing=\"0\">\n" + 
            "    <tr><th>Data<br>fileTypes</th><th>Description</th><th>Info</th><th>Example</th></tr>\n");
        for (int i = 0; i < dataFileTypeNames.length; i++) 
            writer.write(
                "    <tr>\n" +
                "      <td>" + dataFileTypeNames[i] + "</td>\n" +
                "      <td>" + dataFileTypeDescriptions[i] + "</td>\n" +
                "      <td>" + 
                      (dataFileTypeInfo[i].equals("")? 
                          "&nbsp;" : "<a href=\"" +  XML.encodeAsHTML(dataFileTypeInfo[i]) + "\">info</a>") + 
                      "</td>\n" +
                "      <td><a href=\"" +  datasetBase + dataFileTypeNames[i] + "?" + 
                    XML.encodeAsHTML(EDStatic.EDDGridDataTimeExample) + "\">example</a></td>\n" +
                "    </tr>\n");
        writer.write(
            "   </table>\n" +
            "   <br>For example, here is a request URL to download data formatted as an HTML table:\n" +
            "   <br><a href=\"" + fullTimeExample + "\"><tt>" + 
                                  fullTimeExample + "</tt></a>\n" +
            "\n" +

            //ArcGIS
            "<p><b><a href=\"http://www.esri.com/software/arcgis/index.html\">ArcGIS</a>\n" +
            "     <a href=\"http://en.wikipedia.org/wiki/Esri_grid\">.esriAsc</a></b>\n" +
            "     - <a name=\"ArcGIS\">ArcGIS</a> is a family of Geographical Information Systems (GIS) products from ESRI:\n" +
            "   <br>ArcView, ArcEditor, and ArcInfo.  To get data from ERDDAP into your ArcGIS program:\n" +
            "   <br>in ERDDAP, save some data in an .esriAscii file.  The file's extension will be .asc.\n" +
            "   <br>Then, import the data file into your ArcGIS program:\n" +
            "   <ul>\n" +
            "   <li>For ArcInfo v8 or higher:\n" +
            "     <ol>\n" +
            "     <li>Open ArcInfo.\n" +
            "     <li>Use ArcToolbox.\n" +
            "     <li>Use the <tt>Import to Raster : ASCII to Grid</tt> command \n" +
            "     </ol>\n" +
            "   <li>For ArcView's or ArcEditor's ArcMap:\n" +
            "     <ol>\n" +
            "     <li>Open ArcMap.\n" +
            "     <li>Choose a new empty map or load an existing map.\n" +
            "     <li>Click on the ArcToolbox icon.\n" +
            "       <ol>\n" +
            "       <li>Open <tt>Conversion Tools : To Raster : ASCII to Raster</tt>\n" +
            "         <ol>\n" +
            "         <li><tt>Input Raster File</tt> - Browse to select the .esriAscii .asc file.\n" +
            "         <li><tt>Output Raster File</tt> - Keep the default (or change it).\n" +
            "         <li><tt>Output Data Type</tt> - change to FLOAT.\n" +
            "         <li>Click on <tt>OK</tt>.\n" +
            "         </ol>\n" +
            "       <li>Open <tt>Data Management Tools : Projections and Transformations : Define Projection</tt>\n" +
            "         <ol>\n" +
            "         <li>Input Dataset or Feature Class - Browse to select the Raster (GRID) that was just\n" +
            "           <br>created (on disk).\n" +
            "         <li>Coordinate System - click the icon to the right of the text box.\n" +
            "           <br>That will open the Spatial Reference Properties window.\n" +
            "           <ol>\n" +
            "           <li>Click the <tt>Select</tt> button.\n" +
            "           <li>Choose the <tt>Geographic Coordinate Systems</tt> folder. (Most data in ERDDAP uses a\n" +
            "             <br>'geographic' projection, otherwise known as unprojected data or lat/lon data).\n" + 
            "           <li>Select on the <tt>Spheroid</tt>-based folder.\n" +
            "           <li>Select the <tt>Clarke 1866.prj</tt> file and click <tt>Add</tt>.\n" +
            "           <li>Click <tt>OK</tt> in the Spatial Reference Properties Window.\n" +
            "           <li>Click <tt>OK</tt> in the Define Projection Window.\n" +
            "           </ol>\n" +
            "         </ol>\n" +
            "       </ol>\n" +
            "     </ol>\n" +
            "   <li>For older ArcView (v3) - You need the Spatial Analyst extension, which is sold separately.\n" +
            "     <ol>\n" +
            "     <li>Open ArcView\n" +
            "     <li>Open Spatial Analyst\n" +
            "     <li>Use <tt>Import Data Source : ASCII Raster</tt> \n" +
            "     </ol>\n" +
            "  </ul>\n" +
            "\n" +
            "  <p>Shapefiles - Sorry, we currently do not distribute grid data as shapefiles.\n" +
            "  <p>The ESRI .asc format was designed by ESRI to transfer grid data between computers.\n" +
            "\n" +
            "  <p>Or, if you have a recent version of ArcGIS, you can download a .nc file and open it in ArcGIS.\n" +
            "\n" +
            //IDL
            "  <p><b><a href=\"http://www.ittvis.com/language/en-us/productsservices/idl.aspx/\">IDL</a></b> - \n" +
            "    <a name=\"IDL\">IDL</a> is a commercial scientific data visualization program.\n" +
            "  <br>To get data from ERDDAP into IDL, first use ERDDAP to select a subset of data and download a .nc file.\n" +
            "  <br>Then, use these\n" +
            "    <a href=\"http://www.atmos.umd.edu/~gcm/usefuldocs/hdf_netcdf/IDL_hdf-netcdf.html\">instructions</a>\n" +
            "    to import the data from the .nc file into IDL.\n" +
            "\n" +
            //json
            "  <p><b><a href=\"http://www.json.org/\">JSON .json</a></b>\n" +
            "    <a name=\"json\">files</a> are widely used to transfer data to JavaScript scripts running on web pages.\n" +
            "  <br>Griddap will format the data in a flat, table-like structure in the .json file.\n" +
            "\n" +
            //jsonp
            "  <p><b><a href=\"http://niryariv.wordpress.com/2009/05/05/jsonp-quickly/\">JSONP</a>\n" +
            "    <a href=\"http://www.json.org/\">.json</a></b>\n" +
            "    - <a name=\"jsonp\">Requests</a> for .json files may now include an optional jsonp request\n" +
            "  <br>by adding <tt>&amp;.jsonp=<i>functionName</i></tt> to the end of the query.\n" +
            "  <br>Basically, this just tells ERDDAP to add <tt><i>functionName</i>(</tt> to the beginning of the\n" +
            "  <br>response and \")\" to the end of the response.\n" +
            "  <br>If originally there was no query, leave off the \"&amp;\" in your query.\n" +
            "\n" + 
            //matlab
            "  <p><b><a href=\"http://www.mathworks.com/products/matlab/\">MATLAB</a>\n" +
            "    <a href=\"http://www.serc.iisc.ernet.in/ComputingFacilities/software/matfile_format.pdf\">.mat</a></b>\n" +
            "    <a name=\"matlab\">users</a> can use griddap's .mat file type to download data from within MATLAB.\n" +
            "  <br>Here is a one line example:\n" +
                 "<pre>load(urlwrite('" + fullMatExample + "', 'test.mat'));</pre>\n" +
            "    The data will be in a MATLAB structure. The structure's name will be the datasetID\n" +
            "  <br>(for example, <tt>" + EDStatic.EDDGridIdExample + "</tt>). \n" +
            "  <br>The structure's internal variables will have the same names as in ERDDAP,\n" +
            "  <br>(for example, use <tt>fieldnames(" + EDStatic.EDDGridIdExample + ")</tt>). \n" +
            "  <br>If you download a 2D matrix of data (as in the example above), you can plot it with\n" +
            "  <br>(for example):\n" +
                 "<pre>" + EDStatic.EDDGridMatlabPlotExample + "</pre>\n" +
            "  The numbers at the end of the first line specify the range for the color mapping. \n" +
            "  <br>The 'set' command flips the map to make it upright.\n" +
            "  <p>There are also Matlab <a href=\"http://coastwatch.pfeg.noaa.gov/xtracto/\">Xtractomatic</a> scripts for ERDDAP,\n" +
            "    which are particularly useful for getting environmental\n" +
            "  <br>data related to points along an animal's track (e.g.,\n" +
            "    <a href=\"http://gtopp.org/\">GTOPP</a> data).\n" +
            "\n" +
            //nc
            "  <p><b><a href=\"http://www.unidata.ucar.edu/software/netcdf/\">NetCDF</a>\n" +
            "    <a href=\"http://www.unidata.ucar.edu/software/netcdf/docs/netcdf/File-Format-Specification.html\">.nc</a></b>\n" +
            "    - <a name=\"nc\">Requests</a> for .nc files return the data in a standard, version 3, 32-bit, .nc file.\n" +
            "\n" +
            //ncHeader
            "  <p><b>.ncHeader</b>\n" +
            "    - <a name=\"ncHeader\">Requests</a> for .ncHeader files will return the header information (text) that would be generated\n" +
            "  <br>if you used\n" +
            "    <a href=\"http://www.unidata.ucar.edu/software/netcdf/docs/ncdump-man-1.html\">ncdump -h <i>fileName</i></a>\n" +
            "    on the corresponding .nc file.\n" +
            "\n" +
            //odv
            "  <p><b><a href=\"http://odv.awi.de/\">Ocean Data View</a> .odvTxt</b>\n" +
            "    - <a name=\"ODV\">ODV</a> users can download data in a\n" +
            "    <a href=\"http://odv.awi.de/en/documentation/\">ODV Generic Spreadsheet Format .txt file</a>\n" +
            "  <br>by requesting griddap's .odvTxt fileType.\n" +
            "  <br>The dataset MUST include longitude, latitude, and time dimensions.\n" +
            "  <br>Any longitude values (0 to 360, or -180 to 180) are fine.\n" +
            "  <br>After saving the resulting file (with the extension .txt) in your computer:\n" +
            "  <ol>\n" +
            "  <li>Open ODV.\n" +
            "  <li>Use <tt>File : Open</tt>.\n" +
            "    <ul>\n" +
            "    <li>Change <tt>Files of type</tt> to <tt>Data Files (*.txt *.csv *.jos *.o4x)</tt>.\n" + 
            "    <li>Browse to select the .txt file you created in ERDDAP and click on <tt>Open</tt>.\n" +
            "      <br>The data locations should now be visible on a map in ODV.\n" +
            "    </ul>\n" +
            "  <li>If you downloaded a longitude, latitude slice of a dataset, use:\n" +
            "    <ul>\n" +
            "    <li>Press F12 or <tt>View : Layout Templates : 1 SURFACE Window</tt> to view the default isosurface variable.\n" + 
            "    <li>If you want to view a different isosurface variable,\n" +
            "      <ul>\n" +
            "      <li>To define a new isosurface variable, use <tt>View : Isosurface Variables</tt>.\n" +
            "      <li>Right click on the map and choose <tt>Properties : Data : Z-axis</tt> to pick\n" +
            "        <br>the new isosurface variable.\n" +
            "      </ul>\n" +
            //"  <li>To zoom in on the data, use <tt>View : Window Properties : Maps : Domain : Full Domain : OK</tt>.\n" +
            "    </ul>\n" +
            "  <li>See ODV's <tt>Help</tt> menu for more help using ODV.\n" +
            "  </ol>\n" +
            "\n" +
            //R
            "  <p><b><a href=\"http://www.r-project.org/\">R Statistical Package</a></b> -\n" +
            "    <a name=\"R\">R</a> is an open source statistical package for many operating systems.\n" +
            "  <br>In R, you can download a NetCDF version 3 .nc file from ERDDAP. For example:\n" +
            "<pre>  download.file(url=\"" + fullTimeNcExample + "\", destfile=\"/home/bsimons/test.nc\")</pre>\n" +
            "  Then import data from that .nc file into R with the RNetCDF, ncdf, or ncdf4 packages available from\n" +
            "  <a href=\"http://cran.r-project.org/\">CRAN</a>.\n" +
            "  <br>Or, if you want the data in tabular form, download and import the data in a .csv file. For example,\n" +
            "<pre>  download.file(url=\"" + fullTimeCsvExample + "\", destfile=\"/home/bsimons/test.csv\")\n" +
                 "  test&lt;-read.csv(file=\"/home/bsimons/test.csv\")</pre>\n" +
            "  There are also R <a href=\"http://coastwatch.pfeg.noaa.gov/xtracto/\">Xtractomatic</a> scripts for ERDDAP,\n" +
            "    which are particularly useful for getting environmental\n" +
            "  <br>data related to points along an animal's track (e.g.,\n" +
            "    <a href=\"http://gtopp.org/\">GTOPP</a> data).\n" +
            "\n");
            
        //imageFile Types, graphs and maps
        writer.write(
            "  <p><a name=\"imageFileTypes\"><b>Making an Image File with a Graph or Map of Gridded Data</b></a>\n" +
            "  <br>If a griddap request URL specifies a subset of data which is suitable for making\n" +
            "  <br>a graph or a map, and the fileType is an image fileType, griddap will return an image\n" +
            "  <br>with a graph or map. \n" +
            "  <br>griddap request URLs can include optional <a href=\"#GraphicsCommands\">graphics commands</a> which let you\n" +
            "  <br>customize the graph or map.\n" +
            "  <br>As with other griddap request URLs, you can create these URLs by hand or have a\n" +
            "  <br>computer program do it.  Or, you can use the Make A Graph web pages, which simplify\n" +
            "  <br>creating these URLs (see the \"graph\" links in the table of\n" +
               "<a href=\"" + dapBase + "index.html\">griddap datasets</a>). \n" +
            "\n" +
            "   <p>The fileType options for downloading images of graphs and maps of grid data are:\n" +
            "  <table class=\"erd\" cellspacing=\"0\">\n" + 
            "    <tr><th>Image<br>fileTypes</th><th>Description</th><th>Info</th><th>Example</th></tr>\n");
        for (int i = 0; i < imageFileTypeNames.length; i++) 
            writer.write(
                "    <tr>\n" +
                "      <td>" + imageFileTypeNames[i] + "</td>\n" +
                "      <td>" + imageFileTypeDescriptions[i] + "</td>\n" +
                "      <td>" + 
                      (imageFileTypeInfo[i] == null || imageFileTypeInfo[i].equals("")? 
                          "&nbsp;" : "<a href=\"" +  imageFileTypeInfo[i] + "\">info</a>") + 
                      "</td>\n" +   //must be mapExample below because kml doesn't work with graphExample
                "      <td><a href=\"" +  datasetBase + imageFileTypeNames[i] + "?" + 
                    XML.encodeAsHTML(EDStatic.EDDGridMapExample) + "\">example</a></td>\n" +
                "    </tr>\n");
        writer.write(
            "  </table>\n" +
            "\n" +
        //size
            "  <p>Image Size - \".small\" and \".large\" were ERDDAP's original system for making\n" +
            "  <br>different-sized images. Now, for .png and .transparentPng images (not other\n" +
            "  <br>image file types), you can also use the\n" +
            "    <a href=\"#GraphicsCommands\">&amp;.size=<i>width</i>|<i>height</i></a>\n" +
            "    parameter to request\n" +
            "  <br>an image of any size.\n" +
            "\n" +
        //transparentPng
            "  <p>.transparentPng - The .transparentPng file type will make a graph or map without\n" +
            "  <br>the graph axes, landmask, or legend, and with a transparent (not opaque white)\n" +
            "  <br>background.  This option can be used for any type of graph or map.\n" +
            "  <br>The default image size is 360x360 pixels.\n" +
            "  <br>Or, you can use the <a href=\"#GraphicsCommands\">&amp;.size=<i>width</i>|<i>height</i></a> parameter\n" +
            "    request an image of any size.\n" +
            "  <br>The one exception is when draw=surface. Then, ERDDAP makes an image where each\n" +
            "  <br>data point becomes one pixel. So in most cases, you should use a stride value\n" +
            "  <br>(see below) for the x and y axis variables, or use the\n" +
            "    <a href=\"#GraphicsCommands\">&amp;.size=<i>width</i>|<i>height</i></a> parameter\n" +
            "  <br>to restrict the image size, so that the image isn't *huge*.\n" +
            "\n");

        //file type incompatibilities
        writer.write(
            "  <p><b>Incompatibilities</b>\n" +
            "  <br>Some results file types have restrictions. For example, Google Earth .kml is only\n" +
            "  <br>appropriate for results with longitude and latitude values. If a given request is\n" +
            "  <br>incompatible with the requested file type, griddap throws an error.\n" + 
            "\n" +

        //curl
            "<p><a name=\"curl\"><b>Command Line Downloads with curl</b></a>\n" +
            "<br>If you want to download a series of files from ERDDAP, you don't have to request each file's\n" +
            "<br>ERDDAP URL in your browser, sitting and waiting for each file to download. \n" +
            "<br>If you are comfortable writing computer programs (e.g., with C, Java, Python, Matlab, r)\n" +
            "<br>you can write a program with a loop that imports all of the desired data files.\n" +
            "<br>Or, if are comfortable with command line programs (just running a program, or using bash or tcsh\n" +
            "<br>scripts in Linux or Mac OS X, or batch files in Windows), you can use curl to save results files\n" +
            "<br>from ERDDAP into files on your hard drive, without using a browser or writing a computer program.\n" +
            "<br>ERDDAP+curl is amazingly powerful and allows you to use ERDDAP in many new ways.\n" +
            "<br>On Linux or Mac OS X, curl is probably already installed as /usr/bin/curl.\n" +
            "<br>On Windows, or if your computer doesn't have curl already, you need to \n" +
            "  <a href=\"http://curl.haxx.se/download.html\">download curl</a>\n" +
            "<br>and install it.  To get to a command line in Windows, use \"Start : Run\" and type in \"cmd\".\n" +
            "<br>(\"Win32 - Generic, Win32 2000/XP, #.##.#, binary, SSL\" worked for me on Windows XP.)\n" +
            "<br>Instructions for using curl are on the \n" +
                "<a href=\"http://curl.haxx.se/download.html\">curl man page</a> and in this\n" +
                "<a href=\"http://curl.haxx.se/docs/httpscripting.html\">curl tutorial</a>.\n" +
            "<br>But here is a quick tutorial related to using curl with ERDDAP:\n" +
            "<ul>\n" +
            "<li>To download and save one file, use \n" +
            "  <br><tt>curl -g \"<i>erddapUrl</i>\" -o <i>fileDir/fileName.ext</i></tt>\n" +
            "  <br>where <tt>-g</tt> disables curl's globbing feature,\n" +
            "  <br>&nbsp;&nbsp;<tt><i>erddapUrl</i></tt> is any ERDDAP URL that requests a data or image file, and\n" +
            "  <br>&nbsp;&nbsp;<tt>-o <i>fileDir/fileName.ext</i></tt> specifies the name for the file that will be created.\n" +
            "  <br>For example,\n" +
            "<pre>curl -g \"http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdBAssta5day.png?sst[%282010-09-01T12:00:00Z%29][][][]&amp;.draw=surface&amp;.vars=longitude|latitude|sst&amp;.colorBar=|||||\" -o BAssta5day20100901.png</pre>\n" +
            "  The erddapUrl must be <a href=\"http://en.wikipedia.org/wiki/Percent-encoding\">percent encoded</a>.\n" +
            "  <br>If you get the URL from your browser's address textfield, this may be already done.\n" +
            "  <br>If not, in practice, this can be very minimal percent encoding: all you usually\n" +
            "  <br>have to do is convert % into %25, &amp; into %26, \" into %22, + into %2B,\n" +
            "  <br>space into %20 (or +), &lt; into %3C, &gt; into %3E, ~ into %7E, and convert\n" +
            "  <br>all characters above #126 to their %HH form (where HH is the 2-digit hex value).\n" +
            "  <br>Unicode characters above #255 must be UTF-8 encoded and then each byte\n" +
            "  <br> must be converted to %HH form (ask a programmer for help).\n" +
            "  <br>&nbsp;\n" +
            "<li>To download and save many files in one step, use curl with the globbing feature enabled:\n" +
            "  <br><tt>curl \"<i>erddapUrl</i>\" -o <i>fileDir/fileName#1.ext</i></tt>\n" +
            "  <br>Since the globbing feature treats the characters [, ], {, and } as special, you must also\n" +
            "  <br><a href=\"http://en.wikipedia.org/wiki/Percent-encoding\">percent encode</a> \n" +
              "them in the erddapURL as &#037;5B, &#037;5D, &#037;7B, &#037;7D, respectively.\n" +
            "  <br>Then, in the erddapUrl, replace a zero-padded number (for example <tt>01</tt>) with a range\n" +
            "  <br>of values (for example, <tt>[01-05]</tt> ),\n" +
            "  <br>or replace a substring (for example <tt>5day</tt>) with a list of values (for example,\n" +
            "  <br><tt>{5day,8day,mday}</tt> ).\n" +
            "  <br>The <tt>#1</tt> within the output fileName causes the current value of the range or list\n" +
            "  <br>to be put into the output fileName.\n" +
            "  <br>For example, \n" +
            "<pre>curl \"http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdBAssta5day.png?sst&#037;5B%282010-09-[01-05]T12:00:00Z%29&#037;5D&#037;5B&#037;5D&#037;5B&#037;5D&#037;5B&#037;5D&amp;.draw=surface&amp;.vars=longitude|latitude|sst&amp;.colorBar=|||||\" -o BAssta5day201009#1.png</pre>\n" +
            "</ul>\n" +
            "<br>&nbsp;\n");

        //query
        writer.write(
            "<li><a name=\"query\"><b>query</b></a> is the part of the request after the \"?\". \n" +
            "  <br>It specifies the subset of data that you want to receive.\n" +
            "  <br>In griddap, it is an optional\n" +
            "    <a href=\"http://www.opendap.org\">OPeNDAP</a>\n " +
            "    <a href=\"http://www.opendap.org/pdf/ESE-RFC-004v1.1.pdf\">DAP</a>-style\n" +
            "    <a href=\"http://www.opendap.org/user/guide-html/guide_61.html#id5\">hyperslab query</a>\n" +
            "  which can request:\n" +
            "   <ul>\n" +
            "   <li>One or more dimension (axis) variables, for example\n " +
            "       <br><a href=\"" + fullDimensionExample + "\"><tt>" + 
                                      fullDimensionExample + "</tt></a> .\n" +
            "   <li>One or more data variables, for example\n" +
            "       <br><a href=\"" + fullIndexExample + "\"><tt>" + 
                                      fullIndexExample + "</tt></a> .\n" +
            "       <br>To request more than one data variable, separate the desired data variable names by commas.\n" +
            "       <br>If you do request more than one data variable, the requested subset for each variable\n" +
            "       <br>must be identical (see below).\n" +
            "       <br>(In griddap, all data variables within a grid dataset share the same dimensions.)\n" +
            "   <li>The entire dataset. Omitting the entire query is the same as requesting all of the data\n" +
            "     <br>for all of the variables. Because of the large size of most datasets, this is usually\n" +
            "     <br>only appropriate for fileTypes that return information about the dataset (for example,\n" +
            "     <br>.das, .dds, and .html), but don't return all of the actual data values. For example,\n" +
            "     <br><a href=\"" + XML.encodeAsHTML(ddsExample) + "\"><tt>" + 
                                    XML.encodeAsHTML(ddsExample) + "</tt></a>\n" +
            "     <br>griddap is designed to handle requests of any size but trying to download all of the\n" +
            "     <br>data with one request will usually fail (for example, downloads that last days usually\n" +
            "     <br>fail at some point).  If you need all of the data, consider breaking your big request\n" +
            "     <br>into several smaller requests. If you just need a sample of the data, use the largest\n" +
            "     <br>acceptable stride values (see below) to minimize the download time.\n" +
            "   </ul>\n" +
            "   \n" +
            "   <p><a name=\"StartStrideStop\">Using</a> <tt>[start:stride:stop]</tt>\n" +
            "     <br>When requesting dimension (axis) variables or data variables, the query may\n" +
            "     <br>specify a subset of a given dimension by identifying the <tt>[start{{:stride}:stop}]</tt>\n" +
            "     <br>indices for that dimension.\n" +
            "   <ul>\n" +
            "   <li><tt>start</tt> is the index of the first desired value. Indices are 0-based.\n" +
            "     <br>(0 is the first index. 1 is the second index. ...) \n" +
            "   <li><tt>stride</tt> indicates how many intervening values to get: 1=get every value,\n" +
            "     <br>2=get every other value, 3=get every third value, ...\n" +
            "     <br>Stride values are in index units (not the units of the dimension).\n" +
            "   <li><tt>stop</tt> is the index of the last desired value. \n" +             
            "   <li>Specifying only two values for a dimension (i.e., <tt>[start:stop]</tt>) is interpreted\n" +
            "     <br>as <tt>[start:1:stop]</tt>.\n" +
            "   <li>Specifying only one value for a dimension (i.e., <tt>[start]</tt>) is interpreted\n" +
            "     <br>as <tt>[start:1:start]</tt>.\n" +
            "   <li>Specifying no values for a dimension (i.e., []) is interpreted as <tt>[0:1:max]</tt>.\n" +
            "   <li>Omitting all of the <tt>[start:stride:stop]</tt> values (that is, requesting the\n" +
            "     <br>variable without the subset constraint) is equivalent to requesting the entire variable.\n" +
            "     <br>For dimension variables (for example, longitude, latitude, and time) and for fileTypes\n" +
            "     <br>that don't download actual data (notably, .das, .dds, .html, and all of the graph and\n" +
            "     <br>map fileTypes) this is fine. For example,\n" +
            "     <br><a href=\"" + XML.encodeAsHTML(dds1VarExample) + "\"><tt>" + 
                                    XML.encodeAsHTML(dds1VarExample) + "</tt></a>\n" +
            "     <br>For data variables, the resulting data may be very large.\n" +
            "     <br>griddap is designed to handle requests of any size. But if you try to download all of\n" +
            "     <br>the data with one request, the request will often fail for other reasons (for example,\n" +
            "     <br>downloads that last for days usually fail at some point). If you need all of the data,\n" +
            "     <br>consider breaking your big request into several smaller requests.\n" +
            "     <br>If you just need a sample of the data, use the largest acceptable stride values\n" +
            "     <br>to minimize the download time.\n" +
            "   <li><a name=\"last\"><tt>last</tt></a> - ERDDAP extends the OPeNDAP standard by interpreting \n" +
            "     <br>a <tt>start</tt> or <tt>stop</tt> value of <tt>last</tt> as the last available index value.\n" + 
            "     <br>You can also use the notation <tt>last-<i>n</i></tt> (e.g., <tt>last-10</tt>)\n" +
            "     <br>to specify the last index minus some number of indices.\n" + 
            "     <br>You can use '+' in place of '-'. The number of indices can be negative.\n" +
            "   <li><a name=\"parentheses\">griddap</a> extends the standard OPeNDAP subset syntax by allowing the start\n" +
            "     <br>and/or stop values to be actual dimension values (for example, longitude values\n" +
            "     <br>in degrees_east) within parentheses, instead of array indices.  \n" +
            "     <br>This example with " + EDStatic.EDDGridDimNamesExample + " dimension values \n" +
            "     <br><a href=\"" + fullValueExample + "\"><tt>" + 
                                    fullValueExample + "</tt></a>\n" +
            "     <br>is (at least at the time of writing this) equivalent to this example with dimension indices\n" +
            "     <br><a href=\"" + fullIndexExample + "\"><tt>" + 
                                    fullIndexExample + "</tt></a>\n" +
            "     <br>The value in parentheses must be within the range of values for the dimension. \n" +
            "     <br>If the value in parentheses doesn't exactly equal one of the dimension values, the\n" +
            "     <br>closest dimension value will be used.\n" +
            "   <li><a name=\"strideParentheses\">griddap</a> does not allow parentheses around stride values.\n" +
            "     <br>The reasoning is: With the start and stop values, it is easy to convert the value in\n" +
            "     <br>parentheses into the appropriate index value by finding the nearest dimension value.\n" +
            "     <br>This works if the dimension values are evenly spaced or not.\n" +
            "     <br>If the dimension values were always evenly spaced, it would be easy to use a similar\n" +
            "     <br>technique to convert a stride value in parentheses into a stride index value.\n" +
            "     <br>But dimension values often aren't evenly spaced. So for now, ERDDAP doesn't support the\n" +
            "     <br>parentheses notation for stride values.\n" +
            "   <li>griddap always stores date/time values as numbers (in seconds since 1970-01-01T00:00:00Z).\n" +
            "     <br>Here is an example of a query which includes date/time numbers:\n" +
            "     <br><a href=\"" + fullValueExample + "\"><tt>" + 
                                    fullValueExample + "</tt></a>\n" +
            "     <br>Some fileTypes (notably, .csv, .tsv, .htmlTable, .odvTxt, and .xhtml) display date/time values as\n" +
            "     <br><a href=\"http://www.iso.org/iso/date_and_time_format\">ISO 8601:2004 \"extended\" date/time strings</a>\n" +
            "       (e.g., 2002-08-03T12:30:00Z).\n" +
            "     <br>ERDDAP has a utility to\n" +
            "       <a href=\"" + tErddapUrl + "/convert/time.html\">Convert\n" +
            "       a Numeric Time to/from a String Time</a>.\n" +
            "     <br>See also:\n" +
            "       <a href=\"" + tErddapUrl + "/convert/time.html#erddap\">How\n" +
            "       ERDDAP Deals with Time</a>.\n" +
            "   <li>For the time dimension, griddap extends the OPeNDAP standard by allowing you to specify an\n" +
            "     <br>ISO 8601 date/time values in parentheses, which griddap then converts to the internal\n" +
            "     <br>number (in seconds since 1970-01-01T00:00:00Z) and then to the appropriate array index.\n" +
            "     <br>The ISO date/time value should be in the form: <i>YYYY-MM-DD</i>T<i>hh:mm:ssZ</i>, where Z is 'Z'\n" +
            "     <br>or a &plusmn;hh:mm offset from UTC.\n" +
            "     <br>If you omit Z (or the &plusmn;hh:mm offset), :ssZ, :mm:ssZ, or Thh:mm:ssZ from the ISO date/time\n" +
            "     <br>that you specify, the missing fields are assumed to be 0.\n" +
            "     <br>The example below is equivalent (at least at the time of writing this) to the examples above:\n" +
            "     <br><a href=\"" + fullTimeExample + "\"><tt>" + 
                                    fullTimeExample + "</tt></a>\n" +
            "   <li><a name=\"lastInParentheses\"><tt>(last)</tt></a> - ERDDAP interprets \n" +
            "       a <tt>start</tt> or <tt>stop</tt> value of <tt>(last)</tt> as the last\n" + 
            "     <br>available index.\n" + 
            "     <br>You can also use the notation <tt>(last-<i>d</i>)</tt> (e.g., <tt>last-10.5</tt>) to specify\n" +
            "     <br>the last index's value minus some number, which is then converted to the nearest\n" +
            "     <br>index. You can use '+' in place of '-'. The number can be negative.\n" +
            "     <br>For a time axis, the number is interpreted as some number of seconds.\n" +

            //Graphics Commands
            "   <li><a name=\"GraphicsCommands\"><b>Graphics Commands</b></a> - <a name=\"MakeAGraph\">griddap</a> extends the OPeNDAP standard by allowing graphics commands\n" +
            "     <br>in the query. \n" +
            "     <br>The Make A Graph web pages simplify the creation of URLs with these graphics commands\n" + 
            "     <br>(see the \"graph\" links in the table of <a href=\"" + 
                dapBase + "index.html\">griddap datasets</a>). \n" +
            "     <br>So we recommend using the Make A Graph web pages to generate URLs, and then, when\n" + 
            "     <br>needed, using the information here to modify the URLs for special purposes.\n" +
            "     <br>These commands are optional.\n" + 
            "     <br>If present, they must occur after the data request part of the query. \n" +
            "     <br>These commands are used by griddap if you request an <a href=\"#imageFileTypes\">image fileType</a> (PNG or PDF) and are\n" + 
            "     <br>ignored if you request a data file (e.g., .asc). \n" +
            "     <br>If relevant commands are not included in your request, griddap uses the defaults and tries\n" + 
            "     <br>its best to generate a reasonable graph or map.\n" +
            "     <br>All of the commands are in the form <tt>&amp;.<i>commandName</i>=<i>value</i></tt> . \n" +
            "     <br>If the value has sub-values, they are separated by the '|' character. \n" +
            "     <br>The commands are:\n" +
            "     <ul>\n" +
            "     <li><tt>&amp;.colorBar=<i>palette</i>|<i>continuous</i>|<i>scale</i>|<i>min</i>|<i>max</i>|<i>nSections</i></tt> \n" +
            "       <br>This specifies the settings for a color bar.  The sub-values are:\n" +
            "        <ul>\n" +
            //the standard palettes are listed in 'palettes' in Bob's messages.xml, 
            //        EDDTable.java, EDDGrid.java, and setupDatasetsXml.html
            "        <li><i>palette</i> - All ERDDAP installations support a standard set of palettes:\n" + 
            "          <br>BlackBlueWhite, BlackRedWhite, BlackWhite, BlueWhiteRed, LightRainbow,\n" + 
            "          <br>Ocean, Rainbow, RedWhiteBlue, ReverseRainbow, Topography, WhiteBlack,\n" + 
            "          <br>WhiteBlueBlack, WhiteRedBlack.\n" +
            "          <br>Some ERDDAP installations support additional options. See a Make A Graph\n" + 
            "          <br>web page for a complete list.\n" +
            "          <br>The default varies based on min and max: if -1*min ~= max, the default\n" + 
            "          <br>is BlueWhiteRed; otherwise, the default is Rainbow.\n" +
            "        <li><i>continuous</i> - must be either no value (the default), 'C' (for Continuous),\n" + 
            "          <br>or 'D' (for Discrete). The default is different for different datasets.\n" +
            "        <li><i>scale</i> - must be either no value (the default), <tt>Linear</tt>, or <tt>Log</tt>.\n" +
            "          <br>The default is different for different datasets.\n" +
            "        <li><i>min</i> - The minimum value for the color bar. \n" +
            "          <br>The default is different for different datasets.\n" +
            "        <li><i>max</i> - The maximum value for the color bar. \n" +
            "          <br>The default is different for different datasets.\n" +
            "        <li><i>nSections</i> - The preferred number of sections (for Log color bars,\n" + 
            "          <br>this is a minimum value). The default is different for different datasets.\n" +
            "        </ul>\n" +
            "        If you don't specify one of the sub-values, the default for the sub-value will be used.\n" +
            "      <li><tt>&amp;.color=<i>value</i></tt>\n" +
            "          <br>This specifies the color for data lines, markers, vectors, etc. The value must\n" + 
            "          <br>be specified as an 0xRRGGBB value (e.g., 0xFF0000 is red, 0x00FF00 is green).\n" + 
            "          <br>The default is 0x000000 (black).\n" +
            "      <li><tt>&amp;.draw=<i>value</i></tt> \n" +
            "          <br>This specifies how the data will be drawn, as <tt>lines</tt>, <tt>linesAndMarkers</tt>,\n" + 
            "          <br><tt>markers</tt>, <tt>sticks</tt>, <tt>surface</tt>, or <tt>vectors</tt>. \n" +
            "      <li><tt>&amp;.font=<i>scaleFactor</i></tt>\n" +
            "          <br>This specifies a scale factor for the font (e.g., 1.5 would make the font\n" + 
            "          <br>1.5 times as big as normal).\n" +
            "      <li><tt>&amp;.land=<i>value</i></tt>\n" +
            "        <br>This specifies whether the landmask should be drawn <tt>under</tt> or <tt>over</tt> the data.\n" +
            "        <br>The default is different for different datasets (under the ERDDAP administrator's\n" + 
            "        <br>control via a drawLandMask setting in datasets.xml, or via the fallback <tt>drawLandMask</tt>\n" +
            "        <br>setting in setup.xml).\n" +
            "        <br>Terrestrial researchers usually prefer <tt>under</tt>.\n" + 
            "        <br>Oceanographers often prefer <tt>over</tt>. \n" +
            "      <li><tt>&amp;.legend=<i>value</i></tt>\n" +
            "        <br>This specifies whether the legend on PNG images (not PDF's) should be at the\n" + 
            "        <br><tt>Bottom</tt> (default), <tt>Off</tt>, or <tt>Only</tt> (which returns only the legend).\n" +
            "      <li><tt>&amp;.marker=<i>markerType</i>|<i>markerSize</i></tt>\n" +
            "        <br>markerType is an integer: 0=None, 1=Plus, 2=X, 3=Dot, 4=Square,\n" + 
            "        <br>5=Filled Square (default), 6=Circle, 7=Filled Circle, 8=Up Triangle,\n" + 
            "        <br>9=Filled Up Triangle.\n" +
            "        <br>markerSize is an integer from 3 to 50 (default=5)\n" +
            "      <li><tt>&amp;.size=<i>width</i>|<i>height</i></tt>\n" +
            "        <br>For PNG images (not PDF's), this specifies the desired size of the image, in pixels.\n" +
            "        <br>This allows you to specify sizes other than the predefined sizes of .smallPng, .png,\n" +
            "        <br>and .largePng, or the variable size of .transparentPng.\n" +
            "      <li><tt>&amp;.trim=<i>trimPixels</i></tt>\n" +
            "        <br>For PNG images (not PDF's), this tells ERDDAP to make the image shorter by removing\n" +
            "        <br>all whitespace at the bottom, except for <i>trimPixels</i>.\n" +
            "      <li><tt>&amp;.vars=<i>'|'-separated list</i></tt>\n" +
            "        <br>This is a '|'-separated list of variables names. Defaults are hard to predict.\n" +
            "        <br>The meaning associated with each position varies with the <tt>&amp;.draw</tt> value:\n" +
            "        <ul>\n" +
            "        <li>for lines: xAxis|yAxis\n" +
            "        <li>for linesAndMarkers: xAxis|yAxis|Color\n" +
            "        <li>for markers: xAxis|yAxis|Color\n" +
            "        <li>for sticks: xAxis|uComponent|vComponent\n" +
            "        <li>for surface: xAxis|yAxis|Color\n" +
            "        <li>for vectors: xAxis|yAxis|uComponent|vComponent\n" +
            "        </ul>\n" +
            "        If xAxis=longitude and yAxis=latitude, you get a map; otherwise, you get a graph.\n " +
            "      <li><tt>&amp;.vec=<i>value</i></tt>\n" +
            "        <br>This specifies the data vector length (in data units) to be scaled to the\n" + 
            "        <br>size of the sample vector in the legend. The default varies based on the data.\n" +
            "      <li><tt>&amp;.xRange=<i>min</i>|<i>max</i></tt>\n" +
            "        <br>This specifies the min|max for the X axis. The default varies based on the data.\n" +
            "      <li><tt>&amp;.yRange=<i>min</i>|<i>max</i></tt>\n" +
            "        <br>This specifies the min|max for the Y axis. The default varies based on the data.\n" +
            "      </ul>\n" +
            "    <br>A sample graph URL is \n" +
            "    <br><a href=\"" + fullGraphExample + "\"><tt>" + 
                                   fullGraphExample + "</tt></a>\n" +
            "\n" +
            "    <p>Or, if you change the fileType in the URL from .png to .graph, you can see a Make A Graph\n" + 
            "    <br>web page with that request loaded:\n" +
            "    <br><a href=\"" + fullGraphMAGExample + "\"><tt>" + 
                                   fullGraphMAGExample + "</tt></a>\n" +
            "    <br>That makes it easy for humans to modify an image request to make a similar graph or map.\n" + 
            "\n" +
            "    <p>Or, if you change the fileType in the URL from .png to a data fileType (e.g., .htmlTable), you can download the data that was graphed:\n" +
            "    <br><a href=\"" + fullGraphDataExample + "\"><tt>" + 
                                   fullGraphDataExample + "</tt></a>\n" +
            "\n" +
            "    <p>A sample map URL is \n" +
            "    <br><a href=\"" + fullMapExample + "\"><tt>" + 
                                   fullMapExample + "</tt></a>\n" +
            "\n" +
            "    <p>Or, if you change the fileType in the URL from .png to .graph, you can see a Make A Graph\n" + 
            "    <br>web page with that request loaded:\n" +
            "    <br><a href=\"" + fullMapMAGExample + "\"><tt>" + 
                                   fullMapMAGExample + "</tt></a>\n" +
            "\n" +
            "    <p>Or, if you change the fileType in the URL from .png to a data fileType (e.g., .htmlTable),\n" + 
            "    <br>you can download the data that was mapped:\n" +
            "    <br><a href=\"" + fullMapDataExample + "\"><tt>" + 
                                   fullMapDataExample + "</tt></a>\n" +
            "  </ul>\n" +
            "</ul>\n" +

            //other info
            "<a name=\"otherInformation\"><b>Other Information</b></a>\n" +
            "<ul>\n" +
            "<li><a name=\"dataModel\"><b>Data Model</b></a> - Each griddap dataset can be represented as:\n" + 
            "  <ul>\n" +
            "  <li>An ordered list of one or more 1-dimensional axis variables.\n" +
            "      <br>Each axis variable has data of one specific type.\n" +
            "      <br>If the data source has a dimension with a size but no values, ERDDAP uses the values\n" +
            "      <br>0, 1, 2, ...\n" +
            "      <br>The supported types are int8, uint16, int16, int32, int64, float32, and float64.\n" +
            "      <br>Missing values are not allowed.\n" +
            "      <br>The values MUST be sorted in either ascending (recommended) or descending order.\n" +
            "      <br>Unsorted values are not allowed because <tt>[(start):(stop)]</tt> requests must\n" +
            "      <br>translate into a contiguous range of indices.\n" +
            "      <br>Tied values are not allowed because requests for a single <tt>[(value)]</tt> must\n" +
            "      <br>translate unambiguously to one index.\n" +
            "      <br>Each axis variable has a name composed of a letter (A-Z, a-z) and then 0 or more\n" +
            "      <br>characters (A-Z, a-z, 0-9, _).\n" +
            "      <br>Each axis variable has metadata which is a set of Key=Value pairs.\n" +
            "  <li>A set of one or more n-dimensional data variables.\n" +
            "      <br>All data variables use all of the axis variables, in order, as their dimensions.\n" +
            "      <br>Each data variable has data of one specific type.\n" +
            "      <br>The supported types are (int8, uint16, int16, int32, int64, float32, float64, and\n" +
            "      <br>String of any length).\n" +
            "      <br>Missing values are allowed.\n" +
            "      <br>Each data variable has a name composed of a letter (A-Z, a-z) and then 0\n" +
            "      <br>or more characters (A-Z, a-z, 0-9, _).\n" +
            "      <br>Each data variable has metadata which is a set of Key=Value pairs.\n" +
            "  <li>The dataset has Global metadata which is a set of Key=Value pairs.\n" +
            "  <li>Note about metadata: each variable's metadata and the global metadata is a set of 0\n" +
            "      <br>or more <tt><i>Key</i>=<i>Value</i></tt> pairs.\n" +
            "      <br>Each Key is a String consisting of a letter (A-Z, a-z) and then 0 or more other\n" +
            "      <br>characters (A-Z, a-z, 0-9, '_').\n" +
            "      <br>Each Value is either one or more numbers (of one Java type), or one or more Strings\n" +
            "      <br>of any length (using \\n as the separator).\n" +
            "      <br>&nbsp;\n" +
            "  </ul>\n" +
            "<li><a name=\"specialVariables\"><b>Special Variables</b></a>\n" +
            "  <ul>\n" +
            "  <li>In griddap, a longitude axis variable (if present) always has the name \"" + EDV.LON_NAME + "\"\n" + 
            "    <br>and the units \"" + EDV.LON_UNITS + "\".\n" +
            "  <li>In griddap, a latitude axis variable (if present) always has the name, \"" + EDV.LAT_NAME + "\"\n" + 
            "    <br>and the units \"" + EDV.LAT_UNITS + "\".\n" +
            "  <li>In griddap, an altitude axis variable (if present) always has the name \"" + EDV.ALT_NAME + "\"\n" + 
            "    <br>and the units \"" + EDV.ALT_UNITS + "\" above sea level.\n" +
            "    <br>Locations below sea level have negative altitude values.\n" +
            "  <li>In griddap, a time axis variable (if present) always has the name \"" + EDV.TIME_NAME + "\"\n" +
            "    <br>and the units \"" + EDV.TIME_UNITS + "\".\n" +
            "    <br>If you request data and specify a start and/or stop value for the time axis,\n" +
            "    <br>you can specify the time as a number (in seconds since 1970-01-01T00:00:00Z)\n" +
            "    <br>or as a String value (e.g., \"2002-12-25T07:00:00Z\" in the GMT/Zulu time zone).\n" +
            "  <li>Because the longitude, latitude, altitude, and time axis variables are specifically\n" +
            "    <br>recognized, ERDDAP is aware of the spatiotemporal features of each dataset.\n" +
            "    <br>This is useful when making images with maps or time-series, and when saving data\n" +
            "    <br>in geo-referenced file types (e.g., .esriAscii and .kml).\n" +
            "    <br>&nbsp;\n" +
            "  </ul>\n" +
            "<li><a name=\"incompatibilities\"><b>Incompatibilities</b></a>\n" +
            "  <ul>\n" +
            "  <li>File Types - Some results file types have restrictions.\n" +
            "    <br>For example, .kml is only appropriate for results with a range of longitude and\n" +
            "    <br>latitude values.\n" +
            "    <br>If a given request is incompatible with the requested file type, griddap throws an error.\n" +
            "    <br>&nbsp;\n" +
            "  </ul>\n" +
            "<li>" + OutputStreamFromHttpResponse.acceptEncodingHtml +
            "</ul>\n" +
            "<br>&nbsp;\n");
    }                   

    /**
     * Get colorBarMinimum and Maximum for all grid variables in erddap.
     * Currently, this is just set up for Bob's use.
     */
    public static void suggestGraphMinMax() throws Throwable {
        String tDir = "c:/temp/griddap/";
        String tName = "datasets.tsv";

        while (true) {
            String dsName = String2.getStringFromSystemIn("Grid datasetID? "); 
            if (dsName.length() == 0) 
                dsName = "erdBAssta5day"; //hycomPacS";

            Table info = new Table();
            SSR.downloadFile("http://coastwatch.pfeg.noaa.gov/erddap/info/" + dsName + "/index.tsv",
                tDir + tName, true);

            String response[] = String2.readFromFile(tDir + tName);
            Test.ensureTrue(response[0].length() == 0, response[0]);
            String2.log("Dataset info (500 chars):\n" + response[1].substring(0, Math.min(response[1].length(), 500)));

            info.readASCII(tDir + tName);
            //String2.log(info.toString());

            //generate request for data for range of lat and lon  and middle one of other axes
            StringBuilder subset = new StringBuilder();
            StringArray dataVars = new StringArray();
            int nDim = 0;
            for (int row = 0; row < info.nRows(); row++) {
                String type = info.getStringData(0, row);
                String varName = info.getStringData(1, row);
                if (type.equals("variable")) {
                    dataVars.add(varName);
                    continue;
                }

                if (!type.equals("dimension"))
                    continue;

                //deal with dimensions
                nDim++;
                String s4[] = String2.split(info.getStringData(4, row), ',');
                int nValues = String2.parseInt(String2.split(s4[0], '=')[1]);
                String2.log(varName + " " + nValues);
                if (varName.equals("longitude")) 
                    subset.append("[0:" + (nValues / 36) + ":" + (nValues - 1) + "]");
                else if (varName.equals("latitude")) 
                    subset.append("[0:" + (nValues / 18) + ":" + (nValues - 1) + "]");
                else 
                    subset.append("[" + (nValues / 2) + "]");
            }
            String2.log("subset=" + subset.toString() +
                "\nnDim=" + nDim + " vars=" + dataVars.toString());

            //get suggested range for each dataVariable
            Table data = new Table();
            int ndv = dataVars.size();
            for (int v = 0; v < ndv; v++) {
                try {
                    String varName = dataVars.get(v);
                    SSR.downloadFile("http://coastwatch.pfeg.noaa.gov/erddap/griddap/" + dsName + ".tsv?" +
                        varName + subset.toString(), tDir + tName, true);

                    response = String2.readFromFile(tDir + tName);
                    Test.ensureTrue(response[0].length() == 0, response[0]);
                    if (response[1].startsWith("<!DOCTYPE HTML")) {
                        int start = response[1].indexOf("The error:");
                        int stop = response[1].length();
                        if (start >= 0) {
                            start = response[1].indexOf("Your request URL:");
                            stop = response[1].indexOf("</tr>", start);
                            stop = response[1].indexOf("</tr>", stop);
                            stop = response[1].indexOf("</tr>", stop);
                        }
                        if (start < 0) {
                            start = 0;
                            stop = response[1].length();
                        }
                        String2.log("Response for varName=" + varName + ":\n" + 
                            String2.replaceAll(response[1].substring(start, stop),"<br>", "\n<br>"));
                    }

                    data.readASCII(tDir + tName);
                    PrimitiveArray pa = data.getColumn(data.nColumns() - 1);
                    double stats[] = pa.calculateStats();
                    double tMin = stats[PrimitiveArray.STATS_MIN];
                    double tMax = stats[PrimitiveArray.STATS_MAX];
                    double range = tMax - tMin; 
                    double loHi[] = Math2.suggestLowHigh(tMin + range/10, tMax - range/10); //interior range
                    String2.log(
                        "varName=" + varName + " min=" + tMin + " max=" + tMax + "\n" +
                        "                <att name=\"colorBarMinimum\" type=\"double\">" + loHi[0] + "</att>\n" +
                        "                <att name=\"colorBarMaximum\" type=\"double\">" + loHi[1] + "</att>\n");

                } catch (Throwable t) {
                    String2.log("\n" + MustBe.throwableToString(t));
                }
            }

        }
    }


    /**
     * This responds by writing WCS info for this dataset.
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in).
     *      Caller should have already checked that user has access to this dataset.
     */
    public void wcsInfo(String loggedInAs, Writer writer) throws Throwable {

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        String wcsUrl = tErddapUrl + "/wcs/" + datasetID + "/" + EDDGrid.wcsServer;
        String getCapabilities = wcsUrl + "?service=WCS&amp;request=GetCapabilities";
        String wcsExample = wcsUrl + "?NotYetFinished"; //EDStatic.wcsSampleStation;
        writer.write(
            EDStatic.youAreHere(loggedInAs, "Web Coverage Service (WCS)") +
            "\n" +
            "<h2>Overview</h2>\n" +
            "In addition to making data available via \n" +
            "<a href=\"" + tErddapUrl + "/griddap/index.html\">gridddap</a> and \n" +
            "<a href=\"" + tErddapUrl + "/tabledap/index.html\">tabledap</a>,\n" + 
            "ERDDAP makes some datasets available via ERDDAP's Web Coverage Service (WCS) web service.\n" +
            "\n" +
            "<p>See the\n" +
            "<a href=\"" + tErddapUrl + "/wcs/index.html\">list of datasets available via WCS</a>\n" +
            "at this ERDDAP installation.\n" +
            "\n" +
            "<p>" + EDStatic.wcsLongDescriptionHtml + "\n" +
            "\n" +
            "<p>WCS clients send HTTP POST or GET requests (specially formed URLs) to the WCS service and get XML responses.\n" +
            "Some WCS client programs are:\n" +
            "<ul>\n" +
            "<li><a href=\"http://pypi.python.org/pypi/OWSLib/\">OWSLib</a> (free) - a Python command line library\n" +
            "<li><a href=\"http://zeus.pin.unifi.it/cgi-bin/twiki/view/GIgo/WebHome\">GI-go</a> (free)\n" +
            "<li><a href=\"http://www.cadcorp.com/\">CADCorp</a> (commercial) - has a \"no cost\" product called\n" +
            "    <a href=\"http://www.cadcorp.com/products_geographical_information_systems/map_browser.htm\">Map Browser</a>\n" +
            "<li><a href=\"http://www.ittvis.com/ProductServices/IDL.aspx\">IDL</a> (commercial)\n" +
            "<li><a href=\"http://www.gvsig.gva.es/index.php?id=gvsig&amp;L=2\">gvSIG</a> (free)\n" +
            "</ul>\n" +
            "<h2>Sample WCS Requests</h2>\n" +
            "<ul>\n" +
            "<li>You can request the list of capabilities via \n" +
            "  <a href=\"" + getCapabilities + "\">" + getCapabilities + "</a>.\n" +
            "<li>A sample data request is \n" +
            "  <a href=\"" + wcsExample + "\">" + wcsExample + "</a>.\n" +
            "</ul>\n");
    }


    /**
     * This returns the WCS capabilities xml for this dataset (see Erddap.doWcs).
     * This should only be called if accessibleViaWCS is true and
     *    loggedInAs has access to this dataset 
     *    (so redirected to login, instead of getting an error here).
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in).
     * @param version Currently, only "1.0.0" is supported.
     * @param writer In the end, the writer is flushed, not closed.
     */
    public void wcsGetCapabilities(String loggedInAs, String version, Writer writer) 
        throws Throwable {

        if (!isAccessibleTo(EDStatic.getRoles(loggedInAs))) 
            throw new SimpleException("loggedInAs=" + loggedInAs + 
                " isn't authorized to access datasetID=" + datasetID + ".");

        if (accessibleViaWCS().length() > 0)
            throw new SimpleException(accessibleViaWCS());

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        String wcsUrl = tErddapUrl + "/wcs/" + datasetID + "/" + wcsServer;
        String titleXml = XML.encodeAsXML(title());
        String keywordsSA[] = keywords();
        EDVGridAxis lonEdv  = axisVariables[lonIndex];
        EDVGridAxis latEdv  = axisVariables[latIndex];
        EDVGridAxis altEdv  = altIndex  < 0? null : axisVariables[altIndex];
        EDVGridAxis timeEdv = timeIndex < 0? null : axisVariables[timeIndex];
        String lonLatLowerCorner = lonEdv.destinationMinString() + " " +
                                   latEdv.destinationMinString();
        String lonLatUpperCorner = lonEdv.destinationMaxString() + " " +
                                   latEdv.destinationMaxString();

       
        //****  WCS 1.0.0 
        //http://download.deegree.org/deegree2.2/docs/htmldocu_wcs/deegree_wcs_documentation_en.html
        //see THREDDS from July 2009:
        //http://thredds1.pfeg.noaa.gov/thredds/wcs/satellite/MH/chla/8day?request=GetCapabilities&version=1.0.0&service=WCS
        if (version == null || version.equals("1.0.0")) {
            if (version == null)
                version = "1.0.0";
            writer.write(
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<WCS_Capabilities version=\"" + version + "\"\n" +
"  xmlns=\"http://www.opengis.net/wcs\"\n" +
"  xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n" +
"  xmlns:gml=\"http://www.opengis.net/gml\"\n" +
"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
"  xsi:schemaLocation=\"http://www.opengis.net/wcs http://schemas.opengis.net/wcs/1.0.0/wcsCapabilities.xsd\"\n" +
//" updateSequence=\"" + Calendar2.getCurrentISODateTimeStringZulu() + "Z\" +
"  >\n" +
"  <Service>\n" +
"    <description>" + XML.encodeAsXML(summary()) + "</description>\n" +
"    <name>" + datasetID + "</name>\n" +
"    <label>" + titleXml + "</label>\n" +
"    <keywords>\n");
            for (int i = 0; i < keywordsSA.length; i++)
                writer.write(
"      <keyword>" + XML.encodeAsXML(keywordsSA[i]) + "</keyword>\n");
            writer.write(
"    </keywords>\n" +
"    <responsibleParty>\n" +
"      <individualName>" + XML.encodeAsXML(EDStatic.adminIndividualName) + "</individualName>\n" +
"      <organisationName>" + XML.encodeAsXML(EDStatic.adminInstitution) + "</organisationName>\n" +
"      <positionName>" + XML.encodeAsXML(EDStatic.adminPosition) + "</positionName>\n" +
"      <contactInfo>\n" +
"        <phone>\n" +
"          <voice>" + XML.encodeAsXML(EDStatic.adminPhone) + "</voice>\n" +
"        </phone>\n" +
"        <address>\n" +
"          <deliveryPoint>" + XML.encodeAsXML(EDStatic.adminAddress) + "</deliveryPoint>\n" +
"          <city>" + XML.encodeAsXML(EDStatic.adminCity) + "</city>\n" +
"          <administrativeArea>" + XML.encodeAsXML(EDStatic.adminStateOrProvince) + "</administrativeArea>\n" +
"          <postalCode>" + XML.encodeAsXML(EDStatic.adminPostalCode) + "</postalCode>\n" +
"          <country>" + XML.encodeAsXML(EDStatic.adminCountry) + "</country>\n" +
"          <electronicMailAddress>" + XML.encodeAsXML(EDStatic.adminEmail) + "</electronicMailAddress>\n" +
"        </address>\n" +
"        <onlineResource xlink:href=\"" + tErddapUrl + "/wcs/documentation.html\" xlink:type=\"simple\"/>\n" +
"      </contactInfo>\n" +
"    </responsibleParty>\n" +
"    <fees>" + XML.encodeAsXML(fees()) + "</fees>\n" +
"    <accessConstraints>" + XML.encodeAsXML(accessConstraints()) + "</accessConstraints>\n" +
"  </Service>\n" +
"  <Capability>\n" +
"    <Request>\n" +
"      <GetCapabilities>\n" +
"        <DCPType>\n" +
"          <HTTP>\n" +
"            <Get>\n" +
"              <OnlineResource xlink:href=\"" + wcsUrl + "?\" xlink:type=\"simple\" />\n" +
"            </Get>\n" +
"          </HTTP>\n" +
"        </DCPType>\n" +
"      </GetCapabilities>\n" +
"      <DescribeCoverage>\n" +
"        <DCPType>\n" +
"          <HTTP>\n" +
"            <Get>\n" +
"              <OnlineResource xlink:href=\"" + wcsUrl + "?\" xlink:type=\"simple\" />\n" +
"            </Get>\n" +
"          </HTTP>\n" +
"        </DCPType>\n" +
"      </DescribeCoverage>\n" +
"      <GetCoverage>\n" +
"        <DCPType>\n" +
"          <HTTP>\n" +
"            <Get>\n" +
"              <OnlineResource xlink:href=\"" + wcsUrl + "?\" xlink:type=\"simple\" />\n" +
"            </Get>\n" +
"          </HTTP>\n" +
"        </DCPType>\n" +
"      </GetCoverage>\n" +
"    </Request>\n" +
"    <Exception>\n" +
"      <Format>application/vnd.ogc.se_xml</Format>\n" +
"    </Exception>\n" +
"  </Capability>\n" +
"  <ContentMetadata>\n");

            //gather info common to all variables
            StringBuilder varInfo = new StringBuilder(
"      <lonLatEnvelope srsName=\"urn:ogc:def:crs:OGC:1.3:CRS84\">\n" +
"        <gml:pos>" + lonLatLowerCorner + "</gml:pos>\n" +
"        <gml:pos>" + lonLatUpperCorner + "</gml:pos>\n");
            if (timeEdv != null)
                varInfo.append(
"        <gml:timePosition>" + timeEdv.destinationMinString() + "</gml:timePosition>\n" +
"        <gml:timePosition>" + timeEdv.destinationMaxString() + "</gml:timePosition>\n");
            varInfo.append(
"      </lonLatEnvelope>\n");
            String varInfoString = varInfo.toString();

            //write for each dataVariable...
            for (int dv = 0; dv < dataVariables.length; dv++) {
                writer.write(
"    <CoverageOfferingBrief>\n" +
"      <name>" + XML.encodeAsXML(dataVariables[dv].destinationName()) + "</name>\n" +
"      <label>" + XML.encodeAsXML(dataVariables[dv].longName()) + "</label>\n" +
       varInfoString);              

            writer.write(
"    </CoverageOfferingBrief>\n");
            }

         writer.write(
"  </ContentMetadata>\n" +
"</WCS_Capabilities>\n");

            
        //**** getCapabilities 1.1.2
        //based on WCS 1.1.2 spec, Annex C
        //also https://wiki.ucar.edu/display/NNEWD/MIT+Lincoln+Laboratory+Web+Coverage+Service
        /*} else if (version == null || version.equals("1.1.0") || version.equals("1.1.1") || 
                     version.equals("1.1.2")) {
            if (version == null)
                version = "1.1.2";
            writer.write(
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<Capabilities version=\"" + version + "\"\n" +
"  xmlns=\"http://www.opengis.net/wcs/1.1\"\n" +
"  xmlns:ows=\"http://www.opengis.net/ows/1.1\"\n" +
"  xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n" +
"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
"  xsi:schemaLocation=\"http://www.opengis.net/wcs/1.1 ../wcsGetCapabilities.xsd http://www.opengis.net/ows/1.1 ../../../ows/1.1.0/owsAll.xsd\"\n" +
//"  updateSequence=\"" + Calendar2.getCurrentISODateTimeStringZulu() + "Z\" +
"  >\n" +
"  <ows:ServiceIdentification>\n" +
"    <ows:Title>" + titleXml + "</ows:Title>\n" +
"    <ows:Abstract>" + XML.encodeAsXML(summary()) + "</ows:Abstract>\n" +
"    <ows:Keywords>\n");
            for (int i = 0; i < keywordsSA.length; i++)
                writer.write(
"      <ows:keyword>" + XML.encodeAsXML(keywordsSA[i]) + "</ows:keyword>\n");
            writer.write(
"    </ows:Keywords>\n" +
"    <ows:ServiceType>WCS</ows:ServiceType>\n");
            for (int v = 0; v < wcsVersions.length; v++)
                writer.write(
"    <ows:ServiceTypeVersion>" + wcsVersions[v] + "</ows:ServiceTypeVersion>\n");
            writer.write(
"    <ows:Fees>" + XML.encodeAsXML(fees()) + "</ows:Fees>\n" +
"    <ows:AccessConstraints>" + XML.encodeAsXML(accessConstraints()) + "</ows:AccessConstraints>\n" +
"  </ows:ServiceIdentification>\n" +
"  <ows:ServiceProvider>\n" +
"    <ows:ProviderName>" + XML.encodeAsXML(EDStatic.adminInstitution) + "</ows:ProviderName>\n" +
"    <ows:ProviderSite xlink:href=\"" + XML.encodeAsXML(EDStatic.erddapUrl) + "\">\n" +
"    <ows:ServiceContact>\n" +
"      <ows:IndividualName>" + XML.encodeAsXML(EDStatic.adminIndividualName) + "</ows:IndividualName>\n" +
"      <ows:PositionName>" + XML.encodeAsXML(EDStatic.adminPosition) + "</ows:PositionName>\n" +
"      <ows:ContactInfo>\n" +
"        <ows:Phone>\n" +
"          <ows:Voice>" + XML.encodeAsXML(EDStatic.adminPhone) + "</ows:Voice>\n" +
"        </ows:Phone>\n" +
"        <ows:Address>\n" +
"          <ows:DeliveryPoint>" + XML.encodeAsXML(EDStatic.adminAddress) + "</ows:DeliveryPoint>\n" +
"          <ows:City>" + XML.encodeAsXML(EDStatic.adminCity) + "</ows:City>\n" +
"          <ows:AdministrativeArea>" + XML.encodeAsXML(EDStatic.adminStateOrProvince) + "</ows:AdministrativeArea>\n" +
"          <ows:PostalCode>" + XML.encodeAsXML(EDStatic.adminPostalCode) + "</ows:PostalCode>\n" +
"          <ows:Country>" + XML.encodeAsXML(EDStatic.adminCountry) + "</ows:Country>\n" +
"          <ows:ElectronicMailAddress>" + XML.encodeAsXML(EDStatic.adminEmail) + "</ows:ElectronicMailAddress>\n" +
"        </ows:Address>\n" +
"      </ows:ContactInfo>\n" +
"      <ows:Role>ERDDAP/WCS Administrator</ows:Role>\n" +
"    </ows:ServiceContact>\n" +
"  </ows:ServiceProvider>\n" +
"  <ows:OperationsMetadata>\n" +
"    <ows:Operation name=\"GetCapabilities\">\n" +
"      <ows:DCP>\n" +
"        <ows:HTTP>\n" +
"          <ows:Get xlink:href=\"" + wcsUrl + "?\"/>\n" +
//"          <ows:Post xlink:href=\"" + wcsUrl + "?\"/>\n" +
"        </ows:HTTP>\n" +
"      </ows:DCP>\n" +
"    </ows:Operation>\n" +
"    <ows:Operation name=\"GetCoverage\">\n" +
"      <ows:DCP>\n" +
"        <ows:HTTP>\n" +
"          <ows:Get xlink:href=\"" + wcsUrl + "?\"/>\n" +
//"          <ows:Post xlink:href=\"" + wcsUrl + "?\"/>\n" +
"        </ows:HTTP>\n" +
"      </ows:DCP>\n" +
"      <ows:Parameter name=\"Format\">\n" +
"        <ows:AllowedValues>\n");
            for (int f = 0; f < wcsRequestFormats112.length; f++) 
                writer.write(
"          <ows:Value>" + wcsRequestFormats112[f] + "</ows:Value>\n");
            writer.write(
"        </ows:AllowedValues>\n" +
"      </ows:Parameter>\n" +
"    </ows:Operation>\n" +
//???not yet supported?
"    <ows:Operation name=\"DescribeCoverage\">\n" +
"      <ows:DCP>\n" +
"        <ows:HTTP>\n" +
"          <ows:Get xlink:href=\"" + wcsUrl + "?\"/>\n" +
"        </ows:HTTP>\n" +
"      </ows:DCP>\n" +
"      <ows:Parameter name=\"Format\">\n" +
"        <ows:AllowedValues>\n" +
"          <ows:Value>text/xml</ows:Value>\n" +
"        </ows:AllowedValues>\n" +
"      </ows:Parameter>\n" +
"    </ows:Operation>\n" +
"  </ows:OperationsMetadata>\n" +
"  <Contents>\n");

            //for each dataVariable
            for (int dv = 0; dv < dataVariables.length; dv++) {
                String longNameXml = XML.encodeAsXML(dataVariables[dv].longName());
                writer.write(
"    <CoverageSummary>\n" +
"      <ows:Title>" + longNameXml + "</ows:Title>\n" +
"      <ows:Abstract>" + longNameXml + "</ows:Abstract>\n" +
"      <ows:Metadata type=\"other\" xlink:href=\"" + tErddapUrl + "/info/" + datasetID + "/index.html\" />\n" +
"      <ows:WGS84BoundingBox>\n" +  //yes: lon before lat
"        <ows:LowerCorner>" + lonLatLowerCorner + "</ows:LowerCorner>\n" +
"        <ows:UpperCorner>" + lonLatUpperCorner + "</ows:UpperCorner>\n" +
"      </ows:WGS84BoundingBox>\n" +
"      <Identifier>" + XML.encodeAsXML(dataVariables[dv].destinationName()) + "</Identifier>\n" +
"      <Domain>\n" +
"        <SpatialDomain>\n" +
"          <ows:BoundingBox crs=\"urn:ogc:def:crs:OGC:6.3:WGS84\">\n" +
"            <ows:LowerCorner>" + lonLatLowerCorner + "</ows:LowerCorner>\n" +
"            <ows:UpperCorner>" + lonLatUpperCorner + "</ows:UpperCorner>\n" +
"          </ows:BoundingBox>\n" +
"        </SpatialDomain>\n");

                if (timeEdv != null) {
                    writer.write(
"        <TemporalDomain>\n");
                    if (timeEdv.destinationMinString().equals(timeEdv.destinationMaxString())) {
                        writer.write(
"          <TimePosition>" + timeEdv.destinationMinString() + "</TimePosition>\n");
                    } else {
                        writer.write(
"          <TimePeriod>\n" +
"            <BeginTime>" + timeEdv.destinationMinString() + "</BeginTime>\n" +
"            <EndTime>"   + timeEdv.destinationMaxString() + "</EndTime>\n" +
"          </TimePeriod>\n");
                    }
                    writer.write(
"        </TemporalDomain>\n");
                }

                writer.write(
"      </Domain>\n");

//???NEEDS WORK   NOT YET 1.1.2-style   
//identify default value?
                boolean rsPrinted = false;
                for (int av = 0; av < axisVariables.length; av++) {
                    if (av == lonIndex || av == latIndex || av == timeIndex) //catch altIndex too?
                        continue;
                    EDVGridAxis edvga = axisVariables[av];
                    if (!rsPrinted) {
                        writer.write(
"      <Range>\n");
                        rsPrinted = true;
                    }
                    writer.write(
"        <range>\n" +
"          <name>" + XML.encodeAsXML(edvga.destinationName()) + "</name>\n" +
"          <label>" + XML.encodeAsXML(edvga.longName()) + "</label>\n");
                    if (edvga.sourceValues().size() == 1) {
                        writer.write(
"          <singleValue>" + edvga.destinationMinString() + "</singleValue>\n");
                    } else {
                        writer.write(
"          <interval>\n" +
"            <min>" + edvga.destinationMinString() + "</min>\n" +
"            <max>" + edvga.destinationMaxString() + "</max>\n" +
(edvga.isEvenlySpaced()?
"            <res>" + Math.abs(edvga.averageSpacing()) + "</res>\n" : "") +
"          </interval>\n");
                    }
                    writer.write(
"        </range>\n");
                }
                if (rsPrinted) 
                    writer.write(
"      </Range>\n");

                writer.write(
"    </CoverageSummary>\n");
            }

            //CRS and formats
            writer.write(   //or urn:ogc:def:crs:EPSG:4326 ???
"    <SupportedCRS>urn:ogc:def:crs,crs:EPSG:6.3:4326</SupportedCRS>\n");
            for (int f = 0; f < wcsRequestFormats112.length; f++) 
                writer.write(
"    <SupportedFormat>" + wcsRequestFormats112[f] + "</SupportedFormat>\n");
            writer.write(
"  </Contents>\n" +
"</Capabilities>\n");
*/
        } else {
            throw new SimpleException("Query error: version=" + version + " must be \"" + wcsVersion + "\".");
            //one of \"" + String2.toCSVString(wcsVersions) + "\"."); 

        }        

        //essential
        writer.flush();
    }

    /**
     * This returns the WCS DescribeCoverage xml for this dataset (see Erddap.doWcs).
     * This should only be called if accessibleViaWCS is true and
     *    loggedInAs has access to this dataset 
     *    (so redirected to login, instead of getting an error here).
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in).
     * @param version Currently, only "1.0.0" is supported and null defaults to 1.0.0.
     * @param coveragesCSV a comma-separated list of the name of the desired variables (1 or more).
     * @param writer In the end, the writer is flushed, not closed.
     */
    public void wcsDescribeCoverage(String loggedInAs, String version, String coveragesCSV,
        Writer writer) throws Throwable {

        if (!isAccessibleTo(EDStatic.getRoles(loggedInAs))) 
            throw new SimpleException("loggedInAs=" + loggedInAs + 
                " isn't authorized to access datasetID=" + datasetID + ".");

        if (accessibleViaWCS().length() > 0)
            throw new SimpleException(accessibleViaWCS());

        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        String wcsUrl = tErddapUrl + "/wcs/" + datasetID + "/" + wcsServer;
        String titleXml = XML.encodeAsXML(title());
        String keywordsSA[] = keywords();
        EDVGridAxis lonEdv  = axisVariables[lonIndex];
        EDVGridAxis latEdv  = axisVariables[latIndex];
        EDVGridAxis altEdv  = altIndex  < 0? null : axisVariables[altIndex];
        EDVGridAxis timeEdv = timeIndex < 0? null : axisVariables[timeIndex];
        String lonLatLowerCorner = lonEdv.destinationMinString() + " " +
                                   latEdv.destinationMinString();
        String lonLatUpperCorner = lonEdv.destinationMaxString() + " " +
                                   latEdv.destinationMaxString();
        String coverages[] = String2.split(coveragesCSV, ',');
        for (int cov = 0; cov < coverages.length; cov++) {
            if (String2.indexOf(dataVariableDestinationNames(), coverages[cov]) < 0)
                throw new SimpleException("Query error: coverage=" + coverages[cov] + 
                    " isn't a valid coverage name.");
        }
       
        //****  WCS 1.0.0 
        //http://download.deegree.org/deegree2.2/docs/htmldocu_wcs/deegree_wcs_documentation_en.html
        //see THREDDS from July 2009:
        //http://thredds1.pfeg.noaa.gov/thredds/wcs/satellite/MH/chla/8day?request=DescribeCoverage&version=1.0.0&service=WCS&coverage=MHchla
        if (version == null || version.equals("1.0.0")) {
            if (version == null)
                version = "1.0.0";
            writer.write(
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<CoverageDescription version=\"1.0.0\"\n" +
"  xmlns=\"http://www.opengis.net/wcs\"\n" +
"  xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n" +
"  xmlns:gml=\"http://www.opengis.net/gml\"\n" +
"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
"  xsi:schemaLocation=\"http://www.opengis.net/wcs http://schemas.opengis.net/wcs/1.0.0/describeCoverage.xsd\"\n" +
"  >\n");

            //for each requested 
            for (int cov = 0; cov < coverages.length; cov++) {
                EDV edv = findDataVariableByDestinationName(coverages[cov]); 
                writer.write(
"  <CoverageOffering>\n" +
"    <name>" + coverages[cov] + "</name>\n" +
"    <label>" + XML.encodeAsXML(edv.longName()) + "</label>\n" +
"    <lonLatEnvelope srsName=\"urn:ogc:def:crs:OGC:1.3:CRS84\">\n" +
"      <gml:pos>" + lonLatLowerCorner + "</gml:pos>\n" +
"      <gml:pos>" + lonLatUpperCorner + "</gml:pos>\n" +
"    </lonLatEnvelope>\n" +
"    <domainSet>\n" +
"      <spatialDomain>\n" +
"        <gml:Envelope srsName=\"urn:ogc:def:crs:OGC:1.3:CRS84\">\n" +
"          <gml:pos>" + lonLatLowerCorner + "</gml:pos>\n" +
"          <gml:pos>" + lonLatUpperCorner + "</gml:pos>\n" +
"        </gml:Envelope>\n");

//thredds does it, but optional, and I encourage native coord requests (not index values)
//              writer.write(
//"        <gml:RectifiedGrid dimension=\"" + (altEdv == null? 2 : 3) + "\">\n" +
//"          <gml:limits>\n" +
//"            <gml:GridEnvelope>\n" +
//"              <gml:low>0 0" + (altEdv == null? "" : " 0") + "</gml:low>\n" +
//"              <gml:high>" + (lonEdv.sourceValues().size()-1) + " " +
//                             (latEdv.sourceValues().size()-1) + 
//                    (altEdv == null? "" : " " + (altEdv.sourceValues().size()-1)) + 
//               "</gml:high>\n" +
//"            </gml:GridEnvelope>\n" +
//"          </gml:limits>\n" +
//"          <gml:axisName>x</gml:axisName>\n" +
//"          <gml:axisName>y</gml:axisName>\n" +
//(altEdv == null? "" : "          <gml:axisName>z</gml:axisName>\n") +
//"          <gml:origin>\n" +
//"            <gml:pos>" + lonEdv.destinationMinString() + " " +
//                          latEdv.destinationMinString() + 
//                    (altEdv == null? "" : " " + altEdv.destinationMinString()) + 
//               "</gml:pos>\n" +
//"          </gml:origin>\n" +
////???
//"          <gml:offsetVector>0.04166667052552379 0.0 0.0</gml:offsetVector>\n" +  
//"          <gml:offsetVector>0.0 0.0416666635795323 0.0</gml:offsetVector>\n" +
//"          <gml:offsetVector>0.0 0.0 0.0</gml:offsetVector>\n" +
//"        </gml:RectifiedGrid>\n");

                writer.write(
"      </spatialDomain>\n");

                //time
                if (timeIndex > 0) {
                    writer.write(
"      <temporalDomain>\n");
                    PrimitiveArray destValues = timeEdv.destinationStringValues();
                    int nDestValues = destValues.size();
                    for (int i = 0; i < nDestValues; i++) {
                        writer.write(
"        <gml:timePosition>" + destValues.getString(i) + "Z</gml:timePosition>\n");
                    }
                    writer.write(
"      </temporalDomain>\n");
                }

                writer.write(
"    </domainSet>\n");

                //if there are axes other than lon, lat, time, make a rangeSet
                boolean rsPrinted = false;
                for (int av = 0; av < axisVariables.length; av++) {
                    if (av == lonIndex || av == latIndex || av == timeIndex)
                        continue;

                    if (!rsPrinted) {
                        rsPrinted = true;
                        writer.write(
"    <rangeSet>\n" +
"      <RangeSet>\n" +
"        <name>RangeSetName</name>\n" +
"        <label>RangeSetLabel</label>\n");
                    }

                    EDVGridAxis edvga = axisVariables[av];
                    writer.write(
"        <axisDescription>\n" +
"          <AxisDescription>\n" +
"            <name>" + XML.encodeAsXML(edvga.destinationName()) + "</name>\n" +
"            <label>" + XML.encodeAsXML(edvga.longName()) + "</label>\n" +
"            <values>\n");
                    if (edvga.sourceValues().size() == 1) {
                        writer.write(
"              <singleValue>" + edvga.destinationMinString() + "</singleValue>\n");
                    } else {
                        writer.write(
"              <interval>" + 
"                <min>" + edvga.destinationMinString() + "</min>\n" +
"                <max>" + edvga.destinationMaxString() + "</max>\n");
                        if (edvga.isEvenlySpaced()) {
                            writer.write(
"                <res>" + Math.abs(edvga.averageSpacing()) + "</res>\n");
                        }
                        writer.write(
"              </interval>");
                    }
                    writer.write(
                    //default is last value
"              <default>" + edvga.destinationMaxString() + "</default>\n" +
"            </values>\n" +
"          </AxisDescription>\n" +
"        </axisDescription>\n");
//"        <nullValues>\n" +  //axes never have null values
//"          <singleValue>NaN</singleValue>\n" +
//"        </nullValues>\n" +
            } //end of av loop

            if (rsPrinted)
                writer.write(
"      </RangeSet>\n" +
"    </rangeSet>\n");

            writer.write(
"    <supportedCRSs>\n" +
"      <requestCRSs>urn:ogc:def:crs:EPSG:4326</requestCRSs>\n" +
"      <responseCRSs>urn:ogc:def:crs:EPSG:4326</responseCRSs>\n" +
"      <nativeCRSs>urn:ogc:def:crs:EPSG:4326</nativeCRSs>\n" +
"    </supportedCRSs>\n" +
"    <supportedFormats>\n");
            for (int f = 0; f < wcsRequestFormats100.length; f++) 
                writer.write(
"      <formats>" + wcsRequestFormats100[f] + "</formats>\n");
            writer.write(
"    </supportedFormats>\n" +
"    <supportedInterpolations>\n" +
"      <interpolationMethod>none</interpolationMethod>\n" +
"    </supportedInterpolations>\n" +
"  </CoverageOffering>\n");
            } //end of cov loop

            writer.write(
"</CoverageDescription>\n");

        } else {
            throw new SimpleException("Query error: version=" + version + " must be \"" + wcsVersion + "\".");
            //one of \"" + String2.toCSVString(wcsVersions) + "\"."); 
        }

        //essential
        writer.flush();
    }

    /**
     * This responds to WCS query.
     * This should only be called if accessibleViaWCS is true and
     *    loggedInAs has access to this dataset
     *    (so redirected to login, instead of getting an error here);
     *    this doesn't check.
     *
     * @param loggedInAs  or null if not logged in
     * @param wcsQuery a getCoverage query
     * @param outputStreamSource  if all goes well, this method calls out.close() at the end.
     * @throws Exception if trouble (e.g., invalid query parameter)
     */
    public void wcsGetCoverage(String loggedInAs, String wcsQuery, 
        OutputStreamSource outputStreamSource) throws Throwable {

        if (reallyVerbose) String2.log("\nrespondToWcsQuery q=" + wcsQuery);
        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        String requestUrl = "/wcs/" + datasetID + "/" + wcsServer;

        //??? should this check?  getDataForDapQuery doesn't
        //if (!isAccessibleTo(EDStatic.getRoles(loggedInAs))) 
        //    throw new SimpleException("loggedInAs=" + loggedInAs + 
        //        " isn't authorized to access datasetID=" + datasetID + ".");

        if (accessibleViaWCS().length() > 0)
            throw new SimpleException(accessibleViaWCS);

        //parse the wcsQuery
        String dapQuery[] = wcsQueryToDapQuery(EDD.userQueryHashMap(wcsQuery, true));

        //get the data
        respondToDapQuery(null, null, loggedInAs, 
            requestUrl, dapQuery[0], 
            outputStreamSource,
            EDStatic.fullCacheDirectory + datasetID + "/", 
            suggestFileName(loggedInAs, dapQuery[0], dapQuery[1]),
            dapQuery[1]);

        //close the outputStream  
        OutputStream out = outputStreamSource.outputStream("");
        if (out instanceof ZipOutputStream) ((ZipOutputStream)out).closeEntry();
        out.close(); 
    }

    /**
     * This converts a WCS query into an OPeNDAP query.
     * See WCS 1.0.0 spec (finished here), Table 9, pg 32 "The GetCoverageRequest expressed as Key-Value Pairs"
     * and WCS 1.1.2 spec (NOT FINISHED HERE), Table 28, pg 49
     *
     * @param wcsQueryMap   from EDD.userQueryHashMap(sosUserQuery, true); //true=names toLowerCase
     * @return [0]=dapQuery, [1]=format (e.g., .nc)
     * @throws Exception if trouble (e.g., invalid query parameter)
     */
    public String[] wcsQueryToDapQuery(HashMap<String, String> wcsQueryMap) throws Throwable {

        //parse the query and build the dapQuery

        //1.0.0 style:
        //request=GetCoverage&version=1.0.0&service=WCS
        //&format=NetCDF3&coverage=ta
        //&time=2005-05-10T00:00:00Z&vertical=100.0&bbox=-134,11,-47,57
    
        //service
        String service = wcsQueryMap.get("service"); //test name.toLowerCase()
        if (service == null || !service.equals("WCS"))
            throw new SimpleException("Query error: service=" + service + " should have been \"WCS\"."); 

        //version
        String version = wcsQueryMap.get("version"); //test name.toLowerCase()
        if (version == null || !wcsVersion.equals(version)) //String2.indexOf(wcsVersions, version) < 0)
            throw new SimpleException("Query error: version=" + version + " should have been \"" + wcsVersion + "\".");
            //one of \"" + String2.toCSVString(wcsVersions) + "\"."); 
        boolean version100 = version.equals("1.0.0");

        //request
        String request = wcsQueryMap.get("request"); //test name.toLowerCase()
        if (request == null || !request.equals("GetCoverage"))
            throw new SimpleException("Query error: request=" + request + " should have been \"GetCoverage\"."); 

        //format
        String requestFormat = wcsQueryMap.get("format"); //test name.toLowerCase()
        String tRequestFormats[]  = wcsRequestFormats100;  //version100? wcsRequestFormats100  : wcsRequestFormats112;
        String tResponseFormats[] = wcsResponseFormats100; //version100? wcsResponseFormats100 : wcsResponseFormats112;
        int fi = String2.caseInsensitiveIndexOf(tRequestFormats, requestFormat);
        if (fi < 0)
            throw new SimpleException("Query error: format=" + requestFormat + " isn't supported."); 
        String responseFormat = tResponseFormats[fi];
        
        //interpolation (1.0.0)
        if (wcsQueryMap.get("interpolation") != null)  //test name.toLowerCase()
            throw new SimpleException("Query error: 'interpolation' isn't supported."); 
        
        //GridXxx (for regridding in 1.1.2)
        if (wcsQueryMap.get("gridbasecrs") != null || //test name.toLowerCase()
            wcsQueryMap.get("gridtype")    != null || //test name.toLowerCase()
            wcsQueryMap.get("gridcs")      != null || //test name.toLowerCase()
            wcsQueryMap.get("gridorigin")  != null || //test name.toLowerCase()
            wcsQueryMap.get("gridoffsets") != null)   //test name.toLowerCase()
            throw new SimpleException("Query error: regridding via 'GridXxx' parameters isn't supported."); 

        //exceptions    optional
        String exceptions = wcsQueryMap.get("exceptions");
        if (exceptions != null && !exceptions.equals(wcsExceptions))
            throw new SimpleException("Query error: exceptions=" + exceptions + " must be " +
                wcsExceptions + "."); 
        
        //store (1.1.2)
        //if (wcsQueryMap.get("store") != null)  //test name.toLowerCase()
        //    throw new SimpleException("Query error: 'store' isn't supported."); 

        //1.0.0 coverage or 1.1.2 identifier
        String cName = version100? "coverage": "identifier"; //test name.toLowerCase()
        String coverage = wcsQueryMap.get(cName); 
        if (String2.indexOf(dataVariableDestinationNames(), coverage) < 0)
            throw new SimpleException("Query error: " + cName + "=" + coverage + " isn't supported."); 

        //1.0.0 bbox or 1.1.2 BoundingBox
        //wcs requires it, but here it is optional (default to max lat lon range)
        String bboxName = version100? "bbox": "boundingbox"; //test name.toLowerCase()
        String bbox = wcsQueryMap.get(bboxName); 
        EDVGridAxis lonEdv  = axisVariables[lonIndex];
        EDVGridAxis latEdv  = axisVariables[latIndex];
        EDVGridAxis altEdv  = altIndex  < 0? null : axisVariables[altIndex];
        EDVGridAxis timeEdv = timeIndex < 0? null : axisVariables[timeIndex];
        String minLon = lonEdv.destinationMinString();
        String maxLon = lonEdv.destinationMaxString();
        String minLat = latEdv.destinationMinString();
        String maxLat = latEdv.destinationMaxString();
        String minAlt = altIndex < 0? null : 
            altEdv.destinationMaxString();  //yes, default is just last
        String maxAlt = minAlt;
        if (bbox != null) {
            String bboxSA[] = String2.split(bbox, ',');
            if (bboxSA.length < 4) 
                throw new SimpleException(
                    "Query error: " + bboxName + " must have at least 4 comma-separated values.");
            minLon = bboxSA[0];  //note goofy ordering of options
            maxLon = bboxSA[2];            
            minLat = bboxSA[1];
            maxLat = bboxSA[3];
            if (version100 && altIndex >= 0 && bboxSA.length >= 6) {
                minAlt = bboxSA[4];  
                maxAlt = bboxSA[5];
            }
            //??? if (!version100 && bboxSA.length > 4) ...
        }
        double minLonD = String2.parseDouble(minLon);
        double maxLonD = String2.parseDouble(maxLon);
        if (Double.isNaN(minLonD) || Double.isNaN(maxLonD) || minLonD > maxLonD)
            throw new SimpleException(
                "Query error: " + bboxName + " minLongitude=" + minLonD + 
                " must be <= maxLongitude=" + maxLonD + ".");
        double minLatD = String2.parseDouble(minLat);
        double maxLatD = String2.parseDouble(maxLat);
        if (Double.isNaN(minLatD) || Double.isNaN(maxLatD) || minLatD > maxLatD)
            throw new SimpleException(
                "Query error: " + bboxName + " minLatitude=" + minLatD + 
                " must be <= maxLatitude=" + maxLatD + ".");
        double minAltD = String2.parseDouble(minAlt);
        double maxAltD = String2.parseDouble(maxAlt);
        if (altIndex >= 0 && (Double.isNaN(minAltD) || Double.isNaN(maxAltD) || minAltD > maxAltD))
            throw new SimpleException(
                "Query error: " + bboxName + " minAltitude=" + minAltD + 
                " must be <= maxAltitude=" + maxAltD + ".");

        //1.0.0 width/height/depth, resx/y/z
        int lonStride = 1;
        int latStride = 1;
        int altStride = 1;
        if (version100) {

            //lonStride
            String n   = wcsQueryMap.get("width"); //test name.toLowerCase()
            String res = wcsQueryMap.get("resx");  //test name.toLowerCase()
            int start = lonEdv.destinationToClosestSourceIndex(minLonD);
            int stop  = lonEdv.destinationToClosestSourceIndex(maxLonD);
            if (start > stop) { //because !isAscending
                int ti = start; start = stop; stop = ti;}
            if (n != null) {
                int ni = String2.parseInt(n);
                if (ni == Integer.MAX_VALUE || ni <= 0) 
                    throw new SimpleException("Query error: width=" + n + " must be > 0.");
                lonStride = DataHelper.findStride(stop - start + 1, ni); 
            } else if (res != null) {
                double resD = String2.parseDouble(res);
                if (Double.isNaN(resD) || resD <= 0)
                    throw new SimpleException("Query error: resx=" + res + " must be > 0.");
                lonStride = Math2.minMax(1, stop - start, Math2.roundToInt((maxLonD - minLonD) / resD));
            }

            //latStride
            n   = wcsQueryMap.get("height"); //test name.toLowerCase()
            res = wcsQueryMap.get("resy");  //test name.toLowerCase()
            start = latEdv.destinationToClosestSourceIndex(minLatD);
            stop  = latEdv.destinationToClosestSourceIndex(maxLatD);
            if (start > stop) { //because !isAscending
                int ti = start; start = stop; stop = ti;}
            if (n != null) {
                int ni = String2.parseInt(n);
                if (ni == Integer.MAX_VALUE || ni <= 0) 
                    throw new SimpleException("Query error: height=" + n + " must be > 0.");
                latStride = DataHelper.findStride(stop - start + 1, ni); 
                //String2.log("start=" + start + " stop=" + stop + " ni=" + ni + " latStride=" + latStride);
            } else if (res != null) {
                double resD = String2.parseDouble(res);
                if (Double.isNaN(resD) || resD <= 0)
                    throw new SimpleException("Query error: resy=" + res + " must be > 0.");
                latStride = Math2.minMax(1, stop - start, Math2.roundToInt((maxLatD - minLatD) / resD));
            }

            //altStride
            if (altIndex >= 0) {
                n   = wcsQueryMap.get("depth"); //test name.toLowerCase()
                res = wcsQueryMap.get("resz");  //test name.toLowerCase()
                start = altEdv.destinationToClosestSourceIndex(minAltD);
                stop  = altEdv.destinationToClosestSourceIndex(maxAltD);
                if (start > stop) { //because !isAscending
                    int ti = start; start = stop; stop = ti;}
                if (n != null) {
                    int ni = String2.parseInt(n);
                    if (ni == Integer.MAX_VALUE || ni <= 0) 
                        throw new SimpleException("Query error: depth=" + n + " must be > 0.");
                    altStride = DataHelper.findStride(stop - start + 1, ni); 
                } else if (res != null) {
                    double resD = String2.parseDouble(res);
                    if (Double.isNaN(resD) || resD <= 0)
                        throw new SimpleException("Query error: resz=" + res + " must be > 0.");
                    altStride = Math2.minMax(1, stop - start, Math2.roundToInt((maxAltD - minAltD) / resD));
                }
            }             
        }

        //build the dapQuery
        //slightly different from standard: if no time or bbox or other axes specified
        //  defaults are used (max lat and lon range, and "last" of all other axes)
        StringBuilder dapQuery = new StringBuilder(coverage);
        for (int av = 0; av < axisVariables.length; av++) {
            //lon
            if (av == lonIndex) {
                if (lonEdv.isAscending())
                     dapQuery.append("[(" + minLon + "):" + lonStride + ":(" + maxLon + ")]");
                else dapQuery.append("[(" + maxLon + "):" + lonStride + ":(" + minLon + ")]");

            //lat
            } else if (av == latIndex) {
                if (latEdv.isAscending())
                     dapQuery.append("[(" + minLat + "):" + latStride + ":(" + maxLat + ")]");
                else dapQuery.append("[(" + maxLat + "):" + latStride + ":(" + minLat + ")]");

            //alt
            } else if (av == altIndex) {
                if (altEdv.isAscending())
                     dapQuery.append("[(" + minAlt + "):" + altStride + ":(" + maxAlt + ")]");
                else dapQuery.append("[(" + maxAlt + "):" + altStride + ":(" + minAlt + ")]");

            //time
            } else if (av == timeIndex) {
                String paramName = version100? "time": "timesequence";
                String time = wcsQueryMap.get(paramName); //test name.toLowerCase()
                String minTime = null, maxTime = null;
                if (time == null) {
                    dapQuery.append("[(last)]");  //default
                } else {
                    if (time.indexOf(',') >= 0)
                        throw new SimpleException("Query error: comma-separated lists of " + 
                            paramName + "s are not supported.");
                    String timeSA[] = String2.split(time, '/');
                    //'now', see 1.0.0 section 9.2.2.8
                    for (int ti = 0; ti < timeSA.length; ti++) {
                        if (timeSA[ti].toLowerCase().equals("now")) 
                            timeSA[ti] = "last";
                    }
                    if (timeSA.length == 0 || timeSA[0].length() == 0) {
                        throw new SimpleException("Query error: invalid " + paramName + "=\"\".");
                    } else if (timeSA.length == 1) {
                        dapQuery.append("[(" + timeSA[0] + ")]");
                    } else if (timeSA.length == 2) {
                        if (timeEdv.isAscending())
                             dapQuery.append("[(" + timeSA[0] + "):(" + timeSA[1] + ")]");
                        else dapQuery.append("[(" + timeSA[1] + "):(" + timeSA[0] + ")]");
                    } else {
                        throw new SimpleException("Query error: " + paramName + 
                            " resolution values are not supported.");
                    }
                }

            //all other axes
            } else {
                EDVGridAxis edv = axisVariables[av]; 
                String dName = edv.destinationName();
                String paramName = version100? dName : "rangesubset"; //test name.toLowerCase()
//???support for rangesubset below needs help
                String val = wcsQueryMap.get(paramName); 
                if (val == null) {
                    dapQuery.append("[(last)]");  //default
                } else {
                    if (val.indexOf(',') >= 0)
                        throw new SimpleException("Query error: comma-separated lists of " + 
                            dName + "'s are not supported.");
                    String valSA[] = String2.split(val, '/');
                    if (valSA.length == 0 || valSA[0].length() == 0) {
                        throw new SimpleException("Query error: invalid " + paramName + "=\"\".");
                    } else if (valSA.length == 1) {
                        dapQuery.append("[(" + valSA[0] + ")]");
                    } else if (valSA.length == 2) {
                        if (edv.isAscending())
                             dapQuery.append("[(" + valSA[0] + "):(" + valSA[1] + ")]");
                        else dapQuery.append("[(" + valSA[1] + "):(" + valSA[2] + ")]");
                    } else if (valSA.length == 3) {
                        double minD = String2.parseDouble(valSA[0]);
                        double maxD = String2.parseDouble(valSA[1]);
                        double resD = String2.parseDouble(valSA[2]);
                        if (Double.isNaN(minD) || Double.isNaN(maxD) || minD > maxD)
                            throw new SimpleException(
                                "Query error: " + dName + " min=" + valSA[0] + 
                                " must be <= max=" + valSA[1] + ".");
                        int start = edv.destinationToClosestSourceIndex(minD);
                        int stop  = edv.destinationToClosestSourceIndex(maxD);
                        if (start < stop) { //because !isAscending
                            int ti = start; start = stop; stop = ti; }                        
                        if (Double.isNaN(resD) || resD <= 0)
                            throw new SimpleException(
                                "Query error: " + dName + " res=" + valSA[2] + " must be > 0.");
                        int stride = Math2.minMax(1, stop - start, 
                            Math2.roundToInt((maxD - minD) / resD));
                        if (edv.isAscending())
                             dapQuery.append("[(" + valSA[0] + "):" + stride + ":(" + valSA[1] + ")]");
                        else dapQuery.append("[(" + valSA[1] + "):" + stride + ":(" + valSA[0] + ")]");
                    } else {
                        throw new SimpleException("Query error: number=" + valSA.length +
                            " of values for " + dName + " must be <= 3.");
                    }
                }
            }
        }
       

        if (reallyVerbose) String2.log("wcsQueryToDapQuery=" + dapQuery.toString() + 
            "\n  version=" + version + " format=" + responseFormat);
        return new String[] {dapQuery.toString(), responseFormat};
    }

    /**
     * This writes the /wcs/[datasetID]/index.html page to the writer,
     * starting with youAreHere.
     * <br>Currently, this just works as a WCS 1.0.0 server.
     * <br>The caller should have already checked loggedInAs and accessibleViaWCS().
     *
     * @param loggedInAs  the name of the logged in user (or null if not logged in).
     *    This doesn't check if eddGrid is accessible to loggedInAs.
     *    The caller should do that.
     * @param writer   afterwards, the writer is flushed, not closed
     * @throws Throwable if trouble (there shouldn't be)
     */
    public void wcsDatasetHtml(String loggedInAs, Writer writer) throws Throwable {
      
        String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        
        String wcsUrl = tErddapUrl + "/wcs/" + datasetID + "/" + wcsServer;
        String destName0 = dataVariables[0].destinationName();
        EDVGridAxis lonEdv = axisVariables[lonIndex];
        EDVGridAxis latEdv = axisVariables[latIndex];
        String getCap = wcsUrl + "?service=WCS&version=1.0.0&request=GetCapabilities";
        String desCov = wcsUrl + "?service=WCS&version=1.0.0&request=DescribeCoverage&coverage=" + destName0;       
        StringBuilder getCovSB = new StringBuilder(wcsUrl);
        getCovSB.append("?service=WCS&version=1.0.0&request=GetCoverage&coverage=" + destName0 +
            "&bbox=" + 
            lonEdv.destinationMinString() + "," +
            latEdv.destinationMinString() + "," +
            lonEdv.destinationMaxString() + "," +
            latEdv.destinationMaxString());
        if (altIndex >= 0)
            getCovSB.append(
                "," + axisVariables[altIndex].destinationMaxString() + //max to max
                "," + axisVariables[altIndex].destinationMaxString());
        if (timeIndex >= 0)
            getCovSB.append("&time=" + axisVariables[timeIndex].destinationMaxString());
        for (int av = 0; av < axisVariables.length; av++) {
            if (av == lonIndex || av == latIndex || av == altIndex || av == timeIndex)
                continue;
            EDVGridAxis edvga = axisVariables[av];
            getCovSB.append("&" + edvga.destinationName() + "=" + 
                axisVariables[av].destinationMaxString());
        }
        //make height=200 and width proportional
        int width = Math.min(2000, Math2.roundToInt(
            ((lonEdv.destinationMax() - lonEdv.destinationMin()) * 200) /
             (latEdv.destinationMax() - latEdv.destinationMin()) ));
        getCovSB.append("&height=200&width=" + width + "&format=PNG");
        String getCov = getCovSB.toString();


        //*** html head
        //writer.write(EDStatic.startHeadHtml(tErddapUrl, title() + " - WCS"));
        //writer.write("\n" + rssHeadLink(loggedInAs));
        //writer.write("</head>\n");
        //writer.write(EDStatic.startBodyHtml(loggedInAs) + "\n");
        //writer.write(HtmlWidgets.htmlTooltipScript(EDStatic.imageDirUrl(loggedInAs)));
        //writer.flush(); //Steve Souder says: the sooner you can send some html to user, the better

        //*** html body content
        writer.write(EDStatic.youAreHere(loggedInAs, "wcs", datasetID)); //wcs must be lowercase for link to work
        writeHtmlDatasetInfo(loggedInAs, writer, true, true, true, "", "");

        String makeAGraphRef = "<a href=\"" + tErddapUrl + "/griddap/" + datasetID + ".graph\">Make A Graph</a>";
        String datasetListRef = 
            "<br>See the\n" +
            "  <a href=\"" + tErddapUrl + "/wcs/index.html\">list \n" +
            "    of datasets available via WCS</a> at this ERDDAP installation.\n";
        String makeAGraphListRef =
            "  <br>See the\n" +
            "    <a href=\"" + tErddapUrl + "/info/index.html\">list \n" +
            "      of datasets with Make A Graph</a> at this ERDDAP installation.\n";

        //What is WCS?   (for tDatasetID) 
        //!!!see the almost identical documentation above
        writer.write(
            "<h2><a name=\"description\">What</a> is WCS?</h2>\n" +
            EDStatic.wcsLongDescriptionHtml + "\n" +
            datasetListRef +
            "\n" +
            "<h2>Sample WCS Requests for this Dataset</h2>\n" +
            "WCS requests are specially formed URLs with queries. You can create these URLs yourself.\n" +
            "<br>Or, if you use WCS client software, the software will create the URLs and process the results for you.\n" + 
            "<br>There are three types of WCS requests:\n" +
            "<ul>\n" +
            "<li><b>GetCapabilities</b> - A GetCapabilities request returns an XML document which provides\n" +
            "  <br>background information about the service and specific information about all of the data\n" +
            "  <br>available from this service. For this dataset, use\n" + 
            "  <br><a href=\"" + getCap + "\">\n" + 
                getCap + "</a>\n" +
            "  <br>&nbsp;\n" +
            "<li><b>DescribeCoverage</b> - A DescribeCoverage request returns an XML document which provides\n" +
            "  <br>more detailed information about a specific coverage. For example,\n" + 
            "  <br><a href=\"" + desCov + "\"><tt>" + desCov + "</tt></a>\n" +
            "  <br>&nbsp;\n" +
            "<li><b>GetCoverage</b> - A GetCoverage request specifies the subset of data that you want:\n" +
            "    <ul>\n" +
            "    <li>The coverage name (e.g., sst).\n" + 
            "    <li>The bounding box (<i>minLon,minLat,maxLon,maxLat</i>).\n" +
            "    <li>The time range.\n" +
            "    <li>The width and height, or x resolution and y resolution.\n" +
            "    <li>The file format (e.g., " + String2.toCSVString(wcsRequestFormats100) + ").\n" +
            "    </ul>\n" +
            "  <br>The WCS service responds with a file with the requested data. A PNG example is\n" +
            "  <br><a href=\"" + getCov + "\">\n" + 
                getCov + "</a>\n" +
            "  <br>&nbsp;\n" +
            "</ul>\n");

        //client software
        //writer.write(
        //    "<h2><a name=\"clientSoftware\">Client Software</a></h2>" +
        //    "WCS can be used directly by humans using a browser.\n" +
        //    "<br>Some of the information you need to write such software is below.\n" +
        //    "<br>For additional information, please see the\n" +
        //    "  <a href=\"http://www.opengeospatial.org/standards/wcs\">WCS standard documentation</a>.\n" +
        //    "\n");

        wcsRequestDocumentation(tErddapUrl, writer, getCap, desCov, getCov);


            /*
            http://www.esri.com/software/arcgis/\">ArcGIS</a>,\n" +
            "    <a href=\"http://mapserver.refractions.net/phpwms/phpwms-cvs/\">Refractions PHP WMS Client</a>, and\n" +
            "    <a href=\"http://udig.refractions.net//\">uDig</a>. \n" +
            "  <br>To make a client work, you would install the software on your computer.\n" +
            "  <br>Then, you would enter the URL of the WMS service into the client.\n" +
            "  <br>For example, in ArcGIS (not yet fully working because it doesn't handle time!), use\n" +
            "  <br>\"Arc Catalog : Add Service : Arc Catalog Servers Folder : GIS Servers : Add WMS Server\".\n" +
            "  <br>In ERDDAP, this dataset has its own WMS service, which is located at\n" +
            "  <br>&nbsp; &nbsp; <b>" + tErddapUrl + "/wms/" + tDatasetID + "/" + WMS_SERVER + "?</b>\n" +  
            "  <br>(Some WMS client programs don't want the <b>?</b> at the end of that URL.)\n" +
            datasetListRef +
        writer.write(
            "  <p><b>In practice,</b> we haven't found any WMS clients that properly handle dimensions\n" +
            "  <br>other than longitude and latitude (e.g., time), a feature which is specified by the WMS\n" +
            "  <br>specification and which is utilized by most datasets in ERDDAP's WMS servers.\n" +
            "  <br>You may find that using\n" +
            makeAGraphRef + "\n" +
            "    and selecting the .kml file type (an OGC standard)\n" +
            "  <br>to load images into <a href=\"http://earth.google.com/\">Google Earth</a> provides\n" +            
            "     a good (non-WMS) map client.\n" +
            makeAGraphListRef +
            "  <br>&nbsp;\n" +
            "<li> <b>Web page authors can embed a WMS client in a web page.</b>\n" +
            "  <br>For the map above, ERDDAP is using \n" +
            "    <a href=\"http://openlayers.org\">OpenLayers</a>, \n" +  
            "    which is a very versatile WMS client.\n" +
            "  <br>OpenLayers doesn't automatically deal with dimensions\n" +
            "    other than longitude and latitude (e.g., time),\n" +            
            "  <br>so you will have to write JavaScript (or other scripting code) to do that.\n" +
            "  <br>(Adventurous JavaScript programmers can look at the Souce Code for this web page.)\n" + 
            "  <br>&nbsp;\n" +
            "<li> <b>A person with a browser or a computer program can generate special GetMap URLs\n" +
            "  and view/use the resulting image file.</b>\n" +
            "  <br><b>Opaque example:</b> <a href=\"" + tWmsOpaqueExample + "\">" + 
                                                        tWmsOpaqueExample + "</a>\n" +
            "  <br><b>Transparent example:</b> <a href=\"" + tWmsTransparentExample + "\">" + 
                                                             tWmsTransparentExample + "</a>\n" +
            datasetListRef +
            "  <br><b>For more information, see ERDDAP's \n" +
            "    <a href=\"" +tErddapUrl + "/wms/documentation.html\">WMS Documentation</a> .</b>\n" +
            "  <p><b>In practice, it is probably easier and more versatile to use this dataset's\n" +
            "    " + makeAGraphRef + " form</b>\n" +
            "  <br>than to use WMS for this purpose.\n" +
            makeAGraphListRef +
            "</ol>\n" +
            "\n");
            */
        
        //if (EDStatic.displayDiagnosticInfo) 
        //    EDStatic.writeDiagnosticInfoHtml(writer);
        //writer.write(EDStatic.endBodyHtml(tErddapUrl));
        //writer.write("\n</html>\n");
        writer.flush(); 
    }

    /**
     * This writes the html with detailed info about WCS queries. 
     *
     * @param tErddapUrl
     * @param writer
     * @param getCapabilities a sample URL.
     * @param describeCoverage a sample URL.
     * @param getCoverage a sample URL.
     */
    public void wcsRequestDocumentation(String tErddapUrl, Writer writer, String getCapabilities,
        String describeCoverage, String getCoverage) throws Throwable {
        //GetCapabilities
        writer.write(
            "<h2><a name=\"request\">WCS</a> Requests - Detailed Description</h2>\n" +
            "WCS requests are specially formed URLs with queries.\n" +
            "You can create these URLs yourself.\n" +
            "<br>Or, if you use WCS client software, the software will create the URLs and process the results for you.\n" + 
            "<br>There are three types of WCS requests: GetCapabilities, DescribeCoverage, GetCoverage.\n" +
            "<br>For detailed information, please see the\n" +
            "  <a href=\"http://www.opengeospatial.org/standards/wcs\">WCS standard documentation</a>.\n" +
            "\n" +

            "<p><b>GetCapabilities</b> - A GetCapabilities request returns an XML document which provides\n" +
            "  <br>background information about the service and basic information about all of the data\n" +
            "  <br>available from this service.  For this dataset, use\n" + 
            "  <br><a href=\"" + getCapabilities + "\">\n" + 
                getCapabilities + "</a>\n" +
            "  <p>The parameters for a GetCapabilities request are:\n" +
            "<table class=\"erd commonBGColor\" cellspacing=\"4\">\n" +
            "  <tr>\n" +
            "    <th nowrap><i>name=value</i><sup>*</sup></th>\n" +
            "    <th>Description</th>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>service=WCS</td>\n" +
            "    <td>Required.</td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>version=" + wcsVersion + "</td>\n" +
            "    <td>The only valid value is " + wcsVersion + " . This parameter is optional.\n" +
            "    </td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>request=GetCapabilities</td>\n" +
            "    <td>Required.</td>\n" +
            "  </tr>\n" +
            "  </table>\n" +
            "  <sup>*</sup> Parameter names are case-insensitive.\n" +
            "  <br>Parameter values are case sensitive and must be\n" +
            "    <a href=\"http://en.wikipedia.org/wiki/Percent-encoding\">percent encoded</a>,\n" +
            "    which your browser normally handles for you.\n" +
            "  <br>The parameters may be in any order in the URL.\n" +
            "  <br>&nbsp;\n" +
            "\n");

        //DescribeCoverage
        //"?service=WCS&version=1.0.0&request=DescribeCoverage&coverage=" + destName0;       
        writer.write(
            "<p><b>DescribeSensor</b> - A DescribeSensor request returns an XML document which provides\n" +
            "  <br>more detailed information about a specific coverage. For example,\n" + 
            "  <br><a href=\"" + describeCoverage + "\">\n" + 
                describeCoverage + "</a>\n" +
            "\n" +
            "  <p>The parameters for a DescribeCoverage request are:\n" +
            "<table class=\"erd commonBGColor\" cellspacing=\"4\">\n" +
            "  <tr>\n" +
            "    <th nowrap><i>name=value</i><sup>*</sup></th>\n" +
            "    <th>Description</th>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>service=WCS</td>\n" +
            "    <td>Required.</td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>version=" + wcsVersion + "</td>\n" +
            "    <td>The only valid value is " + wcsVersion + " .  Required.\n" +
            "    </td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>request=DescribeCoverage</td>\n" +
            "    <td>Required.</td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>coverage=<i>coverage</i></td>\n" +
            "    <td>A comma-separated list of one or more coverage names \n" +
            "      from the list in the GetCapabilities response.\n" +
            "      <br>Required.</td>\n" +
            "  </tr>\n" +
            "  </table>\n" +
            "  <sup>*</sup> Parameter names are case-insensitive.\n" +
            "  <br>Parameter values are case sensitive and must be\n" +
            "    <a href=\"http://en.wikipedia.org/wiki/Percent-encoding\">percent encoded</a>,\n" +
            "    which your browser normally handles for you.\n" +
            "  <br>The parameters may be in any order in the URL.\n" +
            "  <br>&nbsp;\n" +
            "\n");

        //GetCoverage
        writer.write(
            "<p><b>GetCoverage</b> - A GetCoverage request specifies the subset of data that you want.\n" +
            "  The WCS service responds with a file with the requested data. A PNG example is" +
            "  <br><a href=\"" + getCoverage + "\">\n" + 
                 getCoverage + "</a>\n" +
            "  <p>The parameters for a GetCoverage request are:\n" +
            "<table class=\"erd commonBGColor\" cellspacing=\"4\">\n" +
            "  <tr>\n" +
            "    <th nowrap><i>name=value</i><sup>*</sup></th>\n" +
            "    <th>Description</th>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>service=WCS</td>\n" +
            "    <td>Required.</td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>version=" + wcsVersion + "</td>\n" +
            "    <td>Required.</td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>request=GetCoverage</td>\n" +
            "    <td>Required.</td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>coverage=<i>coverage</i></td>\n" +
            "    <td>The name of a &lt;coverage&gt; from the list in GetCapabilities. Required.</td>\n" +
            "  </tr>\n" +
            //change this behavior???
            "  <tr>\n" +
            "    <td nowrap>crs=<i>crs</i></td>\n" +
            "    <td>This parameter is required by the WCS standard.  ERDDAP ignores this.</td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>response_crs=<i>response_crs</i></td>\n" +
            "    <td>This parameter is optional in the WCS standard.  ERDDAP ignores this.</td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>bbox=<i>minLon,minLat,maxLon,maxLat</i></td>\n" +
            "    <td>BBOX allows you to specify a longitude, latitude bounding box constraint.\n" +
            "      <br>The WCS standard requires at least one BBOX or TIME.\n" +
            "      <br>In ERDDAP, this parameter is optional and the default is always the full longitude, latitude range.</td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>time=<i>time</i>\n" +
            "    <br>time=<i>beginTime/endTime</i></td>\n" +
            "    <td>The time values must be in\n" +
            "      <a href=\"http://www.iso.org/iso/date_and_time_format\">ISO 8601:2004 \"extended\" format</a>,\n" +
            "      for example, <span style=\"white-space: nowrap;\">\"1985-01-02T00:00:00Z\").</span>\n" +
            "      <br>In ERDDAP, any time value specified rounds to the nearest available time.\n" +
            "      <br>Or, in the WCS standard and ERDDAP, you can use \"now\" to get the last available time.\n" +
            "      <br>The WCS standard requires at least one BBOX or TIME.\n" +
            "      <br>In ERDDAP, this parameter is optional and the default is always the last time available.\n" +
            "      <br>The WCS standard allows <i>time=beginTime,endTime,timeRes</i>.  ERDDAP doesn't allow this.\n" +
            "      <br>The WCS standard allows <i>time=time1,time2,...</i>  ERDDAP doesn't allow this.</td>\n" +
            "      <br>ERDDAP has a utility to\n" +
            "        <a href=\"" + tErddapUrl + "/convert/time.html\">Convert\n" +
            "        a Numeric Time to/from a String Time</a>.\n" +
            "      <br>See also:\n" +
            "        <a href=\"" + tErddapUrl + "/convert/time.html#erddap\">How\n" +
            "        ERDDAP Deals with Time</a>.\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap><i>parameter=value</i>\n" +
            "    <br><i>parameter=minValue/maxValue</i>\n" +
            "    <br><i>parameter=minValue/maxValue/resolution</i></td>\n" +
            "    <td>This allows you to specify values for each axis (the 'parameter') other than longitude, latitude, or time.\n" +
            "      <br>'resolution' is an optional average spacing between values (specified in axis units).\n" +
            "      <br>If 'resolution' is omitted, ERDDAP returns every value in the minValue/maxValue range.\n" +
            "      <br>The WCS standard requires this if there is no default value.\n" +
            "      <br>In ERDDAP, this parameter is optional because the default is always the last value available.\n" +
            "      <br>The WCS standard allows <i>parameter=value1,value2,...</i>  ERDDAP doesn't allow this.</td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>width=<i>width</i>\n" +
            "      <br>height=<i>height</i></td>\n" +
            "    <td>Requests a grid of the specified width (longitude values) or height (latitude values).\n" +
            "      <br>The WCS standard requires these or resx and resy.\n" + 
            "      <br>In ERDDAP, these are optional because the default is the full width and height of the grid.\n" +
            //true???
            "      <br>The WCS standard presumably returns a grid of exactly the requested size.\n" + 
            "      <br>In ERDDAP, the grid returned may be slightly larger than requested, because the longitude\n" +
            "      <br>and latitude values will be evenly spaced (in index space) and ERDDAP doesn't interpolate.</td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>resx=<i>resx</i>\n" +
            "      <br>resy=<i>resy</i></td>\n" +
            "    <td>Requests a grid where the x (longitude) values and y (latitude) values have the specified spacing.\n" +
            "      <br>The WCS standard requires these or width and height.\n" + 
            "      <br>In ERDDAP, these are optional because the default is full resolution.\n" +
            //true???
            "      <br>The WCS standard presumably returns a grid of exactly the requested size.\n" + 
            "      <br>In ERDDAP, the grid returned may be slightly larger than requested, because the longitude\n" +
            "      <br>and latitude values will be evenly spaced (in index space) and ERDDAP doesn't interpolate.</td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>interpolation=<i>interpolation</i></td>\n" +
            "    <td>This parameter is optional in the WCS standard.  ERDDAP does not allow or support this.</td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>format=<i>format</i></td>\n" +
            "    <td>In ERDDAP, this can be any one of several response formats:\n" +
            "      <br>");
        for (int f = 0; f < wcsRequestFormats100.length; f++) {
            if (f > 0) writer.write(", ");
            writer.write("\"" + SSR.minimalPercentEncode(wcsRequestFormats100[f]) + "\"");
        }
        writer.write(
            ".\n" +
            "      <br>\"GeoTIFF\" and \"PNG\" only work if the request is for more than one longitude\n" +
            "      <br>and latitude value, and if the request is for just one value for all other axes.\n" +
            "      <br>Required.</td>\n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "    <td nowrap>exceptions=" + wcsExceptions + "</td>\n" +
            "    <td>There is only one valid value.  This parameter is optional.</td>\n" +
            "  </tr>\n" +
            "  </table>\n" +
            "  <sup>*</sup> Parameter names are case-insensitive.\n" +
            "  <br>Parameter values are case sensitive and must be\n" +
            "    <a href=\"http://en.wikipedia.org/wiki/Percent-encoding\">percent encoded</a>,\n" +
            "    which your browser normally handles for you.\n" +
            "  <br>The parameters may be in any order in the URL.\n" +
            "  <br>&nbsp;\n" +
            "\n");

    }

    /**
     * Test the WCS server using erdBAssta5day.
     */
    public static void testWcsBAssta() throws Throwable {
        String2.log("\n*** EDDGridFromNcFiles.testWcsBAssta()");
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetXml("erdBAssta5day"); 
        String wcsQuery, fileName, results, expected;
        java.io.StringWriter writer;
        ByteArrayOutputStream baos;
        OutputStreamSourceSimple osss;
        String loggedInAs = null;

        //1.0.0 capabilities 
        //try to validate with http://www.validome.org/xml/validate/ (just an error in a schema)
        String2.log("\n+++ GetCapabilities 1.0.0");
        writer = new java.io.StringWriter();
        eddGrid.wcsGetCapabilities(loggedInAs, "1.0.0", writer);
        results = writer.toString();
        String2.log(results);        

        //1.0.0 DescribeCoverage
        //try to validate with http://www.validome.org/xml/validate/
        String2.log("\n+++ DescribeCoverage 1.0.0");
        writer = new java.io.StringWriter();
        eddGrid.wcsDescribeCoverage(loggedInAs, "1.0.0", "sst", writer);
        results = writer.toString();
        String2.log(results);        

        //test wcsQueryToDapQuery()
        String wcsQuery1 = 
            "service=WCS&version=1.0.0&request=GetCoverage" +
            "&coverage=sst" +
            "&format=NetCDF3";
        String wcsQuery2 = 
            "service=WCS&version=1.0.0&request=GetCoverage" +
            "&coverage=sst" +
            "&format=netcdf3" +
            "&time=2008-08-01T00:00:00Z" +
            "&bbox=220,20,250,50&width=10&height=10";
        String wcsQuery3 = 
            "service=WCS&version=1.0.0&request=GetCoverage" +
            "&coverage=sst" +
            "&format=png" +
            "&time=2008-08-01T00:00:00Z" +
            "&bbox=220,20,250,50";
        HashMap<String, String> wcsQueryMap1 = EDD.userQueryHashMap(wcsQuery1, true);
        HashMap<String, String> wcsQueryMap2 = EDD.userQueryHashMap(wcsQuery2, true);
        HashMap<String, String> wcsQueryMap3 = EDD.userQueryHashMap(wcsQuery3, true);

        String dapQuery1[] = eddGrid.wcsQueryToDapQuery(wcsQueryMap1);
        String2.log("\nwcsQuery1=" + wcsQuery1 + "\n\ndapQuery1=" + dapQuery1[0]);
        Test.ensureEqual(dapQuery1[0], 
            "sst[(last)][(0.0):1:(0.0)][(-75.0):1:(75.0)][(0.0):1:(360.0)]", "");

        String dapQuery2[] = eddGrid.wcsQueryToDapQuery(wcsQueryMap2);
        String2.log("\nwcsQuery2=" + wcsQuery2 + "\n\ndapQuery2=" + dapQuery2[0]);
        Test.ensureEqual(dapQuery2[0], 
            "sst[(2008-08-01T00:00:00Z)][(0.0):1:(0.0)][(20):33:(50)][(220):33:(250)]", 
            "");
        Test.ensureEqual(dapQuery2[1], ".nc", "");

        String dapQuery3[] = eddGrid.wcsQueryToDapQuery(wcsQueryMap3);
        String2.log("\nwcsQuery3=" + wcsQuery3 + "\n\ndapQuery3=" + dapQuery3[0]);
        Test.ensureEqual(dapQuery3[0], 
            "sst[(2008-08-01T00:00:00Z)][(0.0):1:(0.0)][(20):1:(50)][(220):1:(250)]", 
            "");
        Test.ensureEqual(dapQuery3[1], ".transparentPng", "");

        //???write tests of invalid queries?

        //*** check netcdf response        
        String2.log("\n+++ GetCoverage\n" + wcsQuery1);
        fileName = EDStatic.fullTestCacheDirectory + "testWcsBA_2.nc";
        eddGrid.wcsGetCoverage(loggedInAs, wcsQuery2, 
            new OutputStreamSourceSimple(new FileOutputStream(fileName)));
        results = NcHelper.dumpString(fileName, true);
        String2.log(results);        
        //expected = "zztop";
        //Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //*** check png response        
        String2.log("\n+++ GetCoverage\n" + wcsQuery1);
        fileName = EDStatic.fullTestCacheDirectory + "testWcsBA_3.png";
        eddGrid.wcsGetCoverage(loggedInAs, wcsQuery3, 
            new OutputStreamSourceSimple(new FileOutputStream(fileName)));
        SSR.displayInBrowser("file://" + fileName);

/*
        //*** observations   for all stations and with BBOX  (but just same 1 station)
        //featureOfInterest=BBOX:<min_lon>,<min_lat>,<max_lon>,<max_lat>
        String2.log("\n+++ GetObservations with BBOX (1 station)");
        writer = new java.io.StringWriter();
        String2.log("query: " + wcsQuery2);
        baos = new ByteArrayOutputStream();
        osss = new OutputStreamSourceSimple(baos);
        eddGrid.respondToWcsQuery(wcsQuery2, null, osss);
        results = baos.toString("UTF-8");
        String2.log(results);        
        
        //*** observations   for all stations and with BBOX  (multiple stations)
        //featureOfInterest=BBOX:<min_lon>,<min_lat>,<max_lon>,<max_lat>
        String2.log("\n+++ GetObservations with BBOX (multiple stations)");
        writer = new java.io.StringWriter();
        String2.log("query: " + wcsQuery3);
        baos = new ByteArrayOutputStream();
        osss = new OutputStreamSourceSimple(baos);
        eddGrid.respondToWcsQuery(wcsQuery3, null, osss);
        results = baos.toString("UTF-8");
        String2.log(results);        
  */      
    }

    /** 
     * This writes the dataset's ISO 19115-2 XML to the writer.
     * The template is initially based on THREDDS ncIso output from
     * http://oceanwatch.pfeg.noaa.gov/thredds/iso/satellite/MH/chla/8day
     * (stored on Bob's computer as F:/programs/iso19115/threddsNcIsoMHchla8day.xml).
     * 
     * @param writer a UTF-8 writer
     */
    public static void writeIso19115(Writer writer) throws Throwable {
        writer.write(
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<gmi:MI_Metadata xsi:schemaLocation=\"http://www.isotc211.org/2005/gmi http://www.ngdc.noaa.gov/metadata/published/xsd/schema.xsd\" \n" + 
"xmlns:gts=\"http://www.isotc211.org/2005/gts\" \n" + 
"xmlns:gco=\"http://www.isotc211.org/2005/gco\" \n" + 
"xmlns:gmd=\"http://www.isotc211.org/2005/gmd\" \n" + 
"xmlns:srv=\"http://www.isotc211.org/2005/srv\" \n" + 
"xmlns:gmx=\"http://www.isotc211.org/2005/gmx\" \n" + 
"xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" \n" + 
"xmlns:xlink=\"http://www.w3.org/1999/xlink\" \n" + 
"xmlns:gss=\"http://www.isotc211.org/2005/gss\" \n" + 
"xmlns:nc=\"http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2\" \n" + 
"xmlns:gsr=\"http://www.isotc211.org/2005/gsr\" \n" + 
"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" + 
"xmlns:gml=\"http://www.opengis.net/gml\" \n" + 
"xmlns:gmd2=\"http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2\" \n" + 
"xmlns:gmi=\"http://www.isotc211.org/2005/gmi\">\n" +
"  <gmd:fileIdentifier>\n" +
//???
"    <gco:CharacterString>gov.noaa.pfel.coastwatch:LMHchlaS8day_20110513000000</gco:CharacterString>\n" +
"  </gmd:fileIdentifier>\n" +
"  <gmd:language>\n" +
"    <gmd:LanguageCode codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:LanguageCode\" codeListValue=\"eng\">eng</gmd:LanguageCode>\n" +
"  </gmd:language>\n" +
"  <gmd:characterSet>\n" +
"    <gmd:MD_CharacterSetCode codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_CharacterSetCode\" codeListValue=\"UTF8\">UTF8</gmd:MD_CharacterSetCode>\n" +
"  </gmd:characterSet>\n" +
"  <gmd:hierarchyLevel>\n" +
"    <gmd:MD_ScopeCode codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_ScopeCode\" codeListValue=\"dataset\">dataset</gmd:MD_ScopeCode>\n" +
"  </gmd:hierarchyLevel>\n" +
"  <gmd:hierarchyLevel>\n" +
"    <gmd:MD_ScopeCode codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_ScopeCode\" codeListValue=\"service\">service</gmd:MD_ScopeCode>\n" +
"  </gmd:hierarchyLevel>\n" +
"  <gmd:contact gco:nilReason=\"unknown\"/>\n" +
"  <gmd:dateStamp gco:nilReason=\"unknown\"/>\n" +
"  <gmd:metadataStandardName>\n" +
"    <gco:CharacterString>ISO 19115-2 Geographic Information - Metadata Part 2 Extensions for imagery and gridded data</gco:CharacterString>\n" +
"  </gmd:metadataStandardName>\n" +
"  <gmd:metadataStandardVersion>\n" +
"    <gco:CharacterString>ISO 19115-2:2009(E)</gco:CharacterString>\n" +
"  </gmd:metadataStandardVersion>\n" +
"  <gmd:spatialRepresentationInfo>\n" +
"    <gmd:MD_GridSpatialRepresentation>\n" +
//??? 4 dimensions, but 8 listed: 4 generic, 4 actual
"      <gmd:numberOfDimensions>\n" +
"        <gco:Integer>4</gco:Integer>\n" +
"      </gmd:numberOfDimensions>\n" +
//column
"      <gmd:axisDimensionProperties>\n" +
"        <gmd:MD_Dimension>\n" +
"          <gmd:dimensionName>\n" +
"            <gmd:MD_DimensionNameTypeCode codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_DimensionNameTypeCode\" codeListValue=\"column\">column</gmd:MD_DimensionNameTypeCode>\n" +
"          </gmd:dimensionName>\n" +
"          <gmd:dimensionSize gco:nilReason=\"unknown\"/>\n" +
"          <gmd:resolution>\n" +
"            <gco:Measure uom=\"degrees_east\">0.04167148975575877</gco:Measure>\n" +
"          </gmd:resolution>\n" +
"        </gmd:MD_Dimension>\n" +
"      </gmd:axisDimensionProperties>\n" +
//row 
"      <gmd:axisDimensionProperties>\n" +
"        <gmd:MD_Dimension>\n" +
"          <gmd:dimensionName>\n" +
"            <gmd:MD_DimensionNameTypeCode codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_DimensionNameTypeCode\" codeListValue=\"row\">row</gmd:MD_DimensionNameTypeCode>\n" +
"          </gmd:dimensionName>\n" +
"          <gmd:dimensionSize gco:nilReason=\"unknown\"/>\n" +
"          <gmd:resolution>\n" +
"            <gco:Measure uom=\"degrees_north\">0.041676313961565174</gco:Measure>\n" +
"          </gmd:resolution>\n" +
"        </gmd:MD_Dimension>\n" +
"      </gmd:axisDimensionProperties>\n" +
//vertical
"      <gmd:axisDimensionProperties>\n" +
"        <gmd:MD_Dimension>\n" +
"          <gmd:dimensionName>\n" +
"            <gmd:MD_DimensionNameTypeCode codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_DimensionNameTypeCode\" codeListValue=\"vertical\">vertical</gmd:MD_DimensionNameTypeCode>\n" +
"          </gmd:dimensionName>\n" +
"          <gmd:dimensionSize gco:nilReason=\"unknown\"/>\n" +
"          <gmd:resolution>\n" +
"            <gco:Measure uom=\"m\">0.0</gco:Measure>\n" +
"          </gmd:resolution>\n" +
"        </gmd:MD_Dimension>\n" +
"      </gmd:axisDimensionProperties>\n" +
//temporal
"      <gmd:axisDimensionProperties>\n" +
"        <gmd:MD_Dimension>\n" +
"          <gmd:dimensionName>\n" +
"            <gmd:MD_DimensionNameTypeCode codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_DimensionNameTypeCode\" codeListValue=\"temporal\">temporal</gmd:MD_DimensionNameTypeCode>\n" +
"          </gmd:dimensionName>\n" +
"          <gmd:dimensionSize gco:nilReason=\"unknown\"/>\n" +
"          <gmd:resolution>\n" +
"            <gco:Measure uom=\"unknown\">701352.0 seconds</gco:Measure>\n" +
"          </gmd:resolution>\n" +
"        </gmd:MD_Dimension>\n" +
"      </gmd:axisDimensionProperties>\n" +
//unknown: altitude
"      <gmd:axisDimensionProperties>\n" +
"        <gmd:MD_Dimension id=\"altitude\">\n" +
"          <gmd:dimensionName>\n" +
"            <gmd:MD_DimensionNameTypeCode codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_DimensionNameTypeCode\" codeListValue=\"unknown\">unknown</gmd:MD_DimensionNameTypeCode>\n" +
"          </gmd:dimensionName>\n" +
"          <gmd:dimensionSize>\n" +
"            <gco:Integer>1</gco:Integer>\n" +
"          </gmd:dimensionSize>\n" +
"          <gmd:resolution gco:nilReason=\"missing\"/>\n" +
"        </gmd:MD_Dimension>\n" +
"      </gmd:axisDimensionProperties>\n" +
//unknown: lat
"      <gmd:axisDimensionProperties>\n" +
"        <gmd:MD_Dimension id=\"lat\">\n" +
"          <gmd:dimensionName>\n" +
"            <gmd:MD_DimensionNameTypeCode codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_DimensionNameTypeCode\" codeListValue=\"unknown\">unknown</gmd:MD_DimensionNameTypeCode>\n" +
"          </gmd:dimensionName>\n" +
"          <gmd:dimensionSize>\n" +
"            <gco:Integer>4320</gco:Integer>\n" +
"          </gmd:dimensionSize>\n" +
"          <gmd:resolution gco:nilReason=\"missing\"/>\n" +
"        </gmd:MD_Dimension>\n" +
"      </gmd:axisDimensionProperties>\n" +
//unknown: lon
"      <gmd:axisDimensionProperties>\n" +
"        <gmd:MD_Dimension id=\"lon\">\n" +
"          <gmd:dimensionName>\n" +
"            <gmd:MD_DimensionNameTypeCode codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_DimensionNameTypeCode\" codeListValue=\"unknown\">unknown</gmd:MD_DimensionNameTypeCode>\n" +
"          </gmd:dimensionName>\n" +
"          <gmd:dimensionSize>\n" +
"            <gco:Integer>8640</gco:Integer>\n" +
"          </gmd:dimensionSize>\n" +
"          <gmd:resolution gco:nilReason=\"missing\"/>\n" +
"        </gmd:MD_Dimension>\n" +
"      </gmd:axisDimensionProperties>\n" +
//unknown: time
"      <gmd:axisDimensionProperties>\n" +
"        <gmd:MD_Dimension id=\"time\">\n" +
"          <gmd:dimensionName>\n" +
"            <gmd:MD_DimensionNameTypeCode codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_DimensionNameTypeCode\" codeListValue=\"unknown\">unknown</gmd:MD_DimensionNameTypeCode>\n" +
"          </gmd:dimensionName>\n" +
"          <gmd:dimensionSize>\n" +
"            <gco:Integer>400</gco:Integer>\n" +
"          </gmd:dimensionSize>\n" +
"          <gmd:resolution gco:nilReason=\"missing\"/>\n" +
"        </gmd:MD_Dimension>\n" +
"      </gmd:axisDimensionProperties>\n" +
"      <gmd:cellGeometry>\n" +
"        <gmd:MD_CellGeometryCode codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_CellGeometryCode\" codeListValue=\"area\">area</gmd:MD_CellGeometryCode>\n" +
"      </gmd:cellGeometry>\n" +
"      <gmd:transformationParameterAvailability gco:nilReason=\"unknown\"/>\n" +
"    </gmd:MD_GridSpatialRepresentation>\n" +
"  </gmd:spatialRepresentationInfo>\n" +
"  <gmd:identificationInfo>\n" +
"    <gmd:MD_DataIdentification id=\"DataIdentification\">\n" +
"      <gmd:citation>\n" +
"        <gmd:CI_Citation>\n" +
"          <gmd:title>\n" +
"            <gco:CharacterString>Chlorophyll-a, Aqua MODIS, NPP, 0.05 degrees, Global, Science Quality</gco:CharacterString>\n" +
"          </gmd:title>\n" +
"          <gmd:date>\n" +
"            <gmd:CI_Date>\n" +
"              <gmd:date>\n" +
"                <gco:Date>2011-06-03Z</gco:Date>\n" +
"              </gmd:date>\n" +
"              <gmd:dateType>\n" +
"                <gmd:CI_DateTypeCode codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_DateTypeCode\" codeListValue=\"creation\">creation</gmd:CI_DateTypeCode>\n" +
"              </gmd:dateType>\n" +
"            </gmd:CI_Date>\n" +
"          </gmd:date>\n" +
"          <gmd:date>\n" +
"            <gmd:CI_Date>\n" +
"              <gmd:date>\n" +
"                <gco:Date>2011-06-03Z</gco:Date>\n" +
"              </gmd:date>\n" +
"              <gmd:dateType>\n" +
"                <gmd:CI_DateTypeCode codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_DateTypeCode\" codeListValue=\"issued\">issued</gmd:CI_DateTypeCode>\n" +
"              </gmd:dateType>\n" +
"            </gmd:CI_Date>\n" +
"          </gmd:date>\n" +
"          <gmd:identifier>\n" +
"            <gmd:MD_Identifier>\n" +
"              <gmd:authority>\n" +
"                <gmd:CI_Citation>\n" +
"                  <gmd:title>\n" +
"                    <gco:CharacterString>gov.noaa.pfel.coastwatch</gco:CharacterString>\n" +
"                  </gmd:title>\n" +
"                  <gmd:date gco:nilReason=\"inapplicable\"/>\n" +
"                </gmd:CI_Citation>\n" +
"              </gmd:authority>\n" +
"              <gmd:code>\n" +
"                <gco:CharacterString>LMHchlaS8day_20110513000000</gco:CharacterString>\n" +
"              </gmd:code>\n" +
"            </gmd:MD_Identifier>\n" +
"          </gmd:identifier>\n" +
"          <gmd:citedResponsibleParty>\n" +
"            <gmd:CI_ResponsibleParty>\n" +
"              <gmd:individualName>\n" +
"                <gco:CharacterString>NOAA CoastWatch, West Coast Node</gco:CharacterString>\n" +
"              </gmd:individualName>\n" +
"              <gmd:organisationName>\n" +
"                <gco:CharacterString>NOAA CoastWatch, West Coast Node</gco:CharacterString>\n" +
"              </gmd:organisationName>\n" +
"              <gmd:contactInfo>\n" +
"                <gmd:CI_Contact>\n" +
"                  <gmd:address>\n" +
"                    <gmd:CI_Address>\n" +
"                      <gmd:electronicMailAddress>\n" +
"                        <gco:CharacterString>dave.foley@noaa.gov</gco:CharacterString>\n" +
"                      </gmd:electronicMailAddress>\n" +
"                    </gmd:CI_Address>\n" +
"                  </gmd:address>\n" +
"                  <gmd:onlineResource>\n" +
"                    <gmd:CI_OnlineResource>\n" +
"                      <gmd:linkage>\n" +
"                        <gmd:URL>http://coastwatch.pfel.noaa.gov</gmd:URL>\n" +
"                      </gmd:linkage>\n" +
"                      <gmd:protocol>\n" +
"                        <gco:CharacterString>http</gco:CharacterString>\n" +
"                      </gmd:protocol>\n" +
"                      <gmd:applicationProfile>\n" +
"                        <gco:CharacterString>web browser</gco:CharacterString>\n" +
"                      </gmd:applicationProfile>\n" +
"                      <gmd:name>\n" +
"                        <gco:CharacterString/>\n" +
"                      </gmd:name>\n" +
"                      <gmd:description>\n" +
"                        <gco:CharacterString/>\n" +
"                      </gmd:description>\n" +
"                      <gmd:function>\n" +
"                        <gmd:CI_OnLineFunctionCode codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_OnLineFunctionCode\" codeListValue=\"information\">information</gmd:CI_OnLineFunctionCode>\n" +
"                      </gmd:function>\n" +
"                    </gmd:CI_OnlineResource>\n" +
"                  </gmd:onlineResource>\n" +
"                </gmd:CI_Contact>\n" +
"              </gmd:contactInfo>\n" +
"              <gmd:role>\n" +
"                <gmd:CI_RoleCode codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_RoleCode\" codeListValue=\"originator\">originator</gmd:CI_RoleCode>\n" +
"              </gmd:role>\n" +
"            </gmd:CI_ResponsibleParty>\n" +
"          </gmd:citedResponsibleParty>\n" +
"          <gmd:citedResponsibleParty>\n" +
"            <gmd:CI_ResponsibleParty>\n" +
"              <gmd:individualName>\n" +
"                <gco:CharacterString>NASA GSFC (OBPG)</gco:CharacterString>\n" +
"              </gmd:individualName>\n" +
"              <gmd:organisationName gco:nilReason=\"missing\"/>\n" +
"              <gmd:contactInfo gco:nilReason=\"missing\"/>\n" +
"              <gmd:role>\n" +
"                <gmd:CI_RoleCode codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_RoleCode\" codeListValue=\"Source of level 2 data.\">Source of level 2 data.</gmd:CI_RoleCode>\n" +
"              </gmd:role>\n" +
"            </gmd:CI_ResponsibleParty>\n" +
"          </gmd:citedResponsibleParty>\n" +
"        </gmd:CI_Citation>\n" +
"      </gmd:citation>\n" +
"      <gmd:abstract>\n" +
"        <gco:CharacterString>NOAA CoastWatch distributes chlorophyll-a concentration data from NASA's Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.   This is Science Quality data.</gco:CharacterString>\n" +
"      </gmd:abstract>\n" +
"      <gmd:credit gco:nilReason=\"missing\"/>\n" +
"      <gmd:descriptiveKeywords>\n" +
"        <gmd:MD_Keywords>\n" +
"          <gmd:keyword>\n" +
"            <gco:CharacterString>EARTH SCIENCE &gt; Oceans &gt; Ocean Chemistry &gt; Chlorophyll</gco:CharacterString>\n" +
"          </gmd:keyword>\n" +
"          <gmd:type>\n" +
"            <gmd:MD_KeywordTypeCode codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_KeywordTypeCode\" codeListValue=\"theme\">theme</gmd:MD_KeywordTypeCode>\n" +
"          </gmd:type>\n" +
"          <gmd:thesaurusName>\n" +
"            <gmd:CI_Citation>\n" +
"              <gmd:title>\n" +
"                <gco:CharacterString>GCMD Science Keywords</gco:CharacterString>\n" +
"              </gmd:title>\n" +
"              <gmd:date gco:nilReason=\"unknown\"/>\n" +
"            </gmd:CI_Citation>\n" +
"          </gmd:thesaurusName>\n" +
"        </gmd:MD_Keywords>\n" +
"      </gmd:descriptiveKeywords>\n" +
"      <gmd:descriptiveKeywords>\n" +
"        <gmd:MD_Keywords>\n" +
"          <gmd:keyword>\n" +
"            <gco:CharacterString>CoastWatch (http://coastwatch.noaa.gov/)</gco:CharacterString>\n" +
"          </gmd:keyword>\n" +
"          <gmd:type>\n" +
"            <gmd:MD_KeywordTypeCode codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_KeywordTypeCode\" codeListValue=\"project\">project</gmd:MD_KeywordTypeCode>\n" +
"          </gmd:type>\n" +
"          <gmd:thesaurusName gco:nilReason=\"unknown\"/>\n" +
"        </gmd:MD_Keywords>\n" +
"      </gmd:descriptiveKeywords>\n" +
"      <gmd:descriptiveKeywords>\n" +
"        <gmd:MD_Keywords>\n" +
"          <gmd:keyword>\n" +
"            <gco:CharacterString>concentration_of_chlorophyll_in_sea_water</gco:CharacterString>\n" +
"          </gmd:keyword>\n" +
"          <gmd:keyword>\n" +
"            <gco:CharacterString>altitude</gco:CharacterString>\n" +
"          </gmd:keyword>\n" +
"          <gmd:keyword>\n" +
"            <gco:CharacterString>latitude</gco:CharacterString>\n" +
"          </gmd:keyword>\n" +
"          <gmd:keyword>\n" +
"            <gco:CharacterString>longitude</gco:CharacterString>\n" +
"          </gmd:keyword>\n" +
"          <gmd:keyword>\n" +
"            <gco:CharacterString>time</gco:CharacterString>\n" +
"          </gmd:keyword>\n" +
"          <gmd:type>\n" +
"            <gmd:MD_KeywordTypeCode codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_KeywordTypeCode\" codeListValue=\"theme\">theme</gmd:MD_KeywordTypeCode>\n" +
"          </gmd:type>\n" +
"          <gmd:thesaurusName>\n" +
"            <gmd:CI_Citation>\n" +
"              <gmd:title>\n" +
"                <gco:CharacterString>CF-1.0</gco:CharacterString>\n" +
"              </gmd:title>\n" +
"              <gmd:date gco:nilReason=\"unknown\"/>\n" +
"            </gmd:CI_Citation>\n" +
"          </gmd:thesaurusName>\n" +
"        </gmd:MD_Keywords>\n" +
"      </gmd:descriptiveKeywords>\n" +
"      <gmd:resourceConstraints>\n" +
"        <gmd:MD_LegalConstraints>\n" +
"          <gmd:useLimitation>\n" +
"            <gco:CharacterString>The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.</gco:CharacterString>\n" +
"          </gmd:useLimitation>\n" +
"        </gmd:MD_LegalConstraints>\n" +
"      </gmd:resourceConstraints>\n" +
"      <gmd:aggregationInfo>\n" +
"        <gmd:MD_AggregateInformation>\n" +
"          <gmd:aggregateDataSetName>\n" +
"            <gmd:CI_Citation>\n" +
"              <gmd:title>\n" +
"                <gco:CharacterString>CoastWatch (http://coastwatch.noaa.gov/)</gco:CharacterString>\n" +
"              </gmd:title>\n" +
"              <gmd:date gco:nilReason=\"inapplicable\"/>\n" +
"            </gmd:CI_Citation>\n" +
"          </gmd:aggregateDataSetName>\n" +
"          <gmd:associationType>\n" +
"            <gmd:DS_AssociationTypeCode codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:DS_AssociationTypeCode\" codeListValue=\"largerWorkCitation\">largerWorkCitation</gmd:DS_AssociationTypeCode>\n" +
"          </gmd:associationType>\n" +
"          <gmd:initiativeType>\n" +
"            <gmd:DS_InitiativeTypeCode codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:DS_InitiativeTypeCode\" codeListValue=\"project\">project</gmd:DS_InitiativeTypeCode>\n" +
"          </gmd:initiativeType>\n" +
"        </gmd:MD_AggregateInformation>\n" +
"      </gmd:aggregationInfo>\n" +
"      <gmd:aggregationInfo>\n" +
"        <gmd:MD_AggregateInformation>\n" +
"          <gmd:aggregateDataSetIdentifier>\n" +
"            <gmd:MD_Identifier>\n" +
"              <gmd:authority>\n" +
"                <gmd:CI_Citation>\n" +
"                  <gmd:title>\n" +
"                    <gco:CharacterString>Unidata Common Data Model</gco:CharacterString>\n" +
"                  </gmd:title>\n" +
"                  <gmd:date gco:nilReason=\"inapplicable\"/>\n" +
"                </gmd:CI_Citation>\n" +
"              </gmd:authority>\n" +
"              <gmd:code>\n" +
"                <gco:CharacterString>Grid</gco:CharacterString>\n" +
"              </gmd:code>\n" +
"            </gmd:MD_Identifier>\n" +
"          </gmd:aggregateDataSetIdentifier>\n" +
"          <gmd:associationType>\n" +
"            <gmd:DS_AssociationTypeCode codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:DS_AssociationTypeCode\" codeListValue=\"largerWorkCitation\">largerWorkCitation</gmd:DS_AssociationTypeCode>\n" +
"          </gmd:associationType>\n" +
"          <gmd:initiativeType>\n" +
"            <gmd:DS_InitiativeTypeCode codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:DS_InitiativeTypeCode\" codeListValue=\"project\">project</gmd:DS_InitiativeTypeCode>\n" +
"          </gmd:initiativeType>\n" +
"        </gmd:MD_AggregateInformation>\n" +
"      </gmd:aggregationInfo>\n" +
"      <gmd:language>\n" +
"        <gco:CharacterString>eng</gco:CharacterString>\n" +
"      </gmd:language>\n" +
"      <gmd:topicCategory>\n" +
"        <gmd:MD_TopicCategoryCode>climatologyMeteorologyAtmosphere</gmd:MD_TopicCategoryCode>\n" +
"      </gmd:topicCategory>\n" +
"      <gmd:extent>\n" +
"        <gmd:EX_Extent id=\"boundingExtent\">\n" +
"          <gmd:geographicElement>\n" +
"            <gmd:EX_GeographicBoundingBox id=\"boundingGeographicBoundingBox\">\n" +
"              <gmd:extentTypeCode>\n" +
"                <gco:Boolean>1</gco:Boolean>\n" +
"              </gmd:extentTypeCode>\n" +
"              <gmd:westBoundLongitude>\n" +
"                <gco:Decimal>0.0</gco:Decimal>\n" +
"              </gmd:westBoundLongitude>\n" +
"              <gmd:eastBoundLongitude>\n" +
"                <gco:Decimal>360.0</gco:Decimal>\n" +
"              </gmd:eastBoundLongitude>\n" +
"              <gmd:southBoundLatitude>\n" +
"                <gco:Decimal>-90.0</gco:Decimal>\n" +
"              </gmd:southBoundLatitude>\n" +
"              <gmd:northBoundLatitude>\n" +
"                <gco:Decimal>90.0</gco:Decimal>\n" +
"              </gmd:northBoundLatitude>\n" +
"            </gmd:EX_GeographicBoundingBox>\n" +
"          </gmd:geographicElement>\n" +
"          <gmd:temporalElement>\n" +
"            <gmd:EX_TemporalExtent id=\"boundingTemporalExtent\">\n" +
"              <gmd:extent>\n" +
"                <gml:TimePeriod gml:id=\"timePeriod_id\">\n" +
"                  <gml:beginPosition>2011-05-09T00:00:00Z</gml:beginPosition>\n" +
"                  <gml:endPosition>2011-05-17T00:00:00Z</gml:endPosition>\n" +
"                </gml:TimePeriod>\n" +
"              </gmd:extent>\n" +
"            </gmd:EX_TemporalExtent>\n" +
"          </gmd:temporalElement>\n" +
"          <gmd:verticalElement>\n" +
"            <gmd:EX_VerticalExtent>\n" +
"              <gmd:minimumValue>\n" +
"                <gco:Real>0.0</gco:Real>\n" +
"              </gmd:minimumValue>\n" +
"              <gmd:maximumValue>\n" +
"                <gco:Real>0.0</gco:Real>\n" +
"              </gmd:maximumValue>\n" +
"              <gmd:verticalCRS gco:nilReason=\"missing\"/>\n" +
"            </gmd:EX_VerticalExtent>\n" +
"          </gmd:verticalElement>\n" +
"        </gmd:EX_Extent>\n" +
"      </gmd:extent>\n" +
"    </gmd:MD_DataIdentification>\n" +
"  </gmd:identificationInfo>\n" +
"  <gmd:identificationInfo>\n" +
"    <srv:SV_ServiceIdentification id=\"OPeNDAP\">\n" +
"      <gmd:citation>\n" +
"        <gmd:CI_Citation>\n" +
"          <gmd:title>\n" +
"            <gco:CharacterString>Chlorophyll-a, Aqua MODIS, NPP, 0.05 degrees, Global, Science Quality THREDDS OPeNDAP</gco:CharacterString>\n" +
"          </gmd:title>\n" +
"          <gmd:date>\n" +
"            <gmd:CI_Date>\n" +
"              <gmd:date>\n" +
"                <gco:Date>2011-06-03Z</gco:Date>\n" +
"              </gmd:date>\n" +
"              <gmd:dateType>\n" +
"                <gmd:CI_DateTypeCode codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_DateTypeCode\" codeListValue=\"creation\">creation</gmd:CI_DateTypeCode>\n" +
"              </gmd:dateType>\n" +
"            </gmd:CI_Date>\n" +
"          </gmd:date>\n" +
"          <gmd:date>\n" +
"            <gmd:CI_Date>\n" +
"              <gmd:date>\n" +
"                <gco:Date>2011-06-03Z</gco:Date>\n" +
"              </gmd:date>\n" +
"              <gmd:dateType>\n" +
"                <gmd:CI_DateTypeCode codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_DateTypeCode\" codeListValue=\"issued\">issued</gmd:CI_DateTypeCode>\n" +
"              </gmd:dateType>\n" +
"            </gmd:CI_Date>\n" +
"          </gmd:date>\n" +
"          <gmd:citedResponsibleParty>\n" +
"            <gmd:CI_ResponsibleParty>\n" +
"              <gmd:individualName>\n" +
"                <gco:CharacterString>NOAA CoastWatch, West Coast Node</gco:CharacterString>\n" +
"              </gmd:individualName>\n" +
"              <gmd:organisationName>\n" +
"                <gco:CharacterString>NOAA CoastWatch, West Coast Node</gco:CharacterString>\n" +
"              </gmd:organisationName>\n" +
"              <gmd:contactInfo>\n" +
"                <gmd:CI_Contact>\n" +
"                  <gmd:address>\n" +
"                    <gmd:CI_Address>\n" +
"                      <gmd:electronicMailAddress>\n" +
"                        <gco:CharacterString>dave.foley@noaa.gov</gco:CharacterString>\n" +
"                      </gmd:electronicMailAddress>\n" +
"                    </gmd:CI_Address>\n" +
"                  </gmd:address>\n" +
"                  <gmd:onlineResource>\n" +
"                    <gmd:CI_OnlineResource>\n" +
"                      <gmd:linkage>\n" +
"                        <gmd:URL>http://coastwatch.pfel.noaa.gov</gmd:URL>\n" +
"                      </gmd:linkage>\n" +
"                      <gmd:protocol>\n" +
"                        <gco:CharacterString>http</gco:CharacterString>\n" +
"                      </gmd:protocol>\n" +
"                      <gmd:applicationProfile>\n" +
"                        <gco:CharacterString>web browser</gco:CharacterString>\n" +
"                      </gmd:applicationProfile>\n" +
"                      <gmd:name>\n" +
"                        <gco:CharacterString/>\n" +
"                      </gmd:name>\n" +
"                      <gmd:description>\n" +
"                        <gco:CharacterString/>\n" +
"                      </gmd:description>\n" +
"                      <gmd:function>\n" +
"                        <gmd:CI_OnLineFunctionCode codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_OnLineFunctionCode\" codeListValue=\"information\">information</gmd:CI_OnLineFunctionCode>\n" +
"                      </gmd:function>\n" +
"                    </gmd:CI_OnlineResource>\n" +
"                  </gmd:onlineResource>\n" +
"                </gmd:CI_Contact>\n" +
"              </gmd:contactInfo>\n" +
"              <gmd:role>\n" +
"                <gmd:CI_RoleCode codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_RoleCode\" codeListValue=\"originator\">originator</gmd:CI_RoleCode>\n" +
"              </gmd:role>\n" +
"            </gmd:CI_ResponsibleParty>\n" +
"          </gmd:citedResponsibleParty>\n" +
"          <gmd:citedResponsibleParty>\n" +
"            <gmd:CI_ResponsibleParty>\n" +
"              <gmd:individualName>\n" +
"                <gco:CharacterString>NASA GSFC (OBPG)</gco:CharacterString>\n" +
"              </gmd:individualName>\n" +
"              <gmd:organisationName gco:nilReason=\"missing\"/>\n" +
"              <gmd:contactInfo gco:nilReason=\"missing\"/>\n" +
"              <gmd:role>\n" +
"                <gmd:CI_RoleCode codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_RoleCode\" codeListValue=\"Source of level 2 data.\">Source of level 2 data.</gmd:CI_RoleCode>\n" +
"              </gmd:role>\n" +
"            </gmd:CI_ResponsibleParty>\n" +
"          </gmd:citedResponsibleParty>\n" +
"        </gmd:CI_Citation>\n" +
"      </gmd:citation>\n" +
"      <gmd:abstract>\n" +
"        <gco:CharacterString>NOAA CoastWatch distributes chlorophyll-a concentration data from NASA's Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.   This is Science Quality data.</gco:CharacterString>\n" +
"      </gmd:abstract>\n" +
"      <srv:serviceType>\n" +
"        <gco:LocalName>THREDDS OPeNDAP</gco:LocalName>\n" +
"      </srv:serviceType>\n" +
"      <srv:extent>\n" +
"        <gmd:EX_Extent>\n" +
"          <gmd:geographicElement>\n" +
"            <gmd:EX_GeographicBoundingBox>\n" +
"              <gmd:extentTypeCode>\n" +
"                <gco:Boolean>1</gco:Boolean>\n" +
"              </gmd:extentTypeCode>\n" +
"              <gmd:westBoundLongitude>\n" +
"                <gco:Decimal>0.0</gco:Decimal>\n" +
"              </gmd:westBoundLongitude>\n" +
"              <gmd:eastBoundLongitude>\n" +
"                <gco:Decimal>360.0</gco:Decimal>\n" +
"              </gmd:eastBoundLongitude>\n" +
"              <gmd:southBoundLatitude>\n" +
"                <gco:Decimal>-90.0</gco:Decimal>\n" +
"              </gmd:southBoundLatitude>\n" +
"              <gmd:northBoundLatitude>\n" +
"                <gco:Decimal>90.0</gco:Decimal>\n" +
"              </gmd:northBoundLatitude>\n" +
"            </gmd:EX_GeographicBoundingBox>\n" +
"          </gmd:geographicElement>\n" +
"          <gmd:temporalElement>\n" +
"            <gmd:EX_TemporalExtent>\n" +
"              <gmd:extent>\n" +
"                <gml:TimePeriod gml:id=\"_timePeriod_id\">\n" +
"                  <gml:beginPosition>2011-05-09T00:00:00Z</gml:beginPosition>\n" +
"                  <gml:endPosition>2011-05-17T00:00:00Z</gml:endPosition>\n" +
"                </gml:TimePeriod>\n" +
"              </gmd:extent>\n" +
"            </gmd:EX_TemporalExtent>\n" +
"          </gmd:temporalElement>\n" +
"          <gmd:verticalElement>\n" +
"            <gmd:EX_VerticalExtent>\n" +
"              <gmd:minimumValue>\n" +
"                <gco:Real>0.0</gco:Real>\n" +
"              </gmd:minimumValue>\n" +
"              <gmd:maximumValue>\n" +
"                <gco:Real>0.0</gco:Real>\n" +
"              </gmd:maximumValue>\n" +
"              <gmd:verticalCRS gco:nilReason=\"missing\"/>\n" +
"            </gmd:EX_VerticalExtent>\n" +
"          </gmd:verticalElement>\n" +
"        </gmd:EX_Extent>\n" +
"      </srv:extent>\n" +
"      <srv:couplingType>\n" +
"        <srv:SV_CouplingType codeList=\"http://www.tc211.org/ISO19139/resources/codeList.xml#SV_CouplingType\" codeListValue=\"tight\">tight</srv:SV_CouplingType>\n" +
"      </srv:couplingType>\n" +
"      <srv:containsOperations>\n" +
"        <srv:SV_OperationMetadata>\n" +
"          <srv:operationName>\n" +
"            <gco:CharacterString>OPeNDAPDatasetQueryAndAccess</gco:CharacterString>\n" +
"          </srv:operationName>\n" +
"          <srv:DCP gco:nilReason=\"unknown\"/>\n" +
"          <srv:connectPoint>\n" +
"            <gmd:CI_OnlineResource>\n" +
"              <gmd:linkage>\n" +
"                <gmd:URL>http://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/8day</gmd:URL>\n" +
"              </gmd:linkage>\n" +
"              <gmd:name>\n" +
"                <gco:CharacterString>OPeNDAP</gco:CharacterString>\n" +
"              </gmd:name>\n" +
"              <gmd:description>\n" +
"                <gco:CharacterString>THREDDS OPeNDAP</gco:CharacterString>\n" +
"              </gmd:description>\n" +
"              <gmd:function>\n" +
"                <gmd:CI_OnLineFunctionCode codeList=\"http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_OnLineFunctionCode\" codeListValue=\"download\">download</gmd:CI_OnLineFunctionCode>\n" +
"              </gmd:function>\n" +
"            </gmd:CI_OnlineResource>\n" +
"          </srv:connectPoint>\n" +
"        </srv:SV_OperationMetadata>\n" +
"      </srv:containsOperations>\n" +
"      <srv:operatesOn xlink:href=\"#DataIdentification\"/>\n" +
"    </srv:SV_ServiceIdentification>\n" +
"  </gmd:identificationInfo>\n" +
"  <gmd:contentInfo>\n" +
"    <gmi:MI_CoverageDescription>\n" +
"      <gmd:attributeDescription gco:nilReason=\"unknown\"/>\n" +
"      <gmd:contentType gco:nilReason=\"unknown\"/>\n" +
"      <gmd:dimension>\n" +
"        <gmd:MD_Band>\n" +
"          <gmd:sequenceIdentifier>\n" +
"            <gco:MemberName>\n" +
"              <gco:aName>\n" +
"                <gco:CharacterString>MHchla</gco:CharacterString>\n" +
"              </gco:aName>\n" +
"              <gco:attributeType>\n" +
"                <gco:TypeName>\n" +
"                  <gco:aName>\n" +
"                    <gco:CharacterString>float</gco:CharacterString>\n" +
"                  </gco:aName>\n" +
"                </gco:TypeName>\n" +
"              </gco:attributeType>\n" +
"            </gco:MemberName>\n" +
"          </gmd:sequenceIdentifier>\n" +
"          <gmd:descriptor>\n" +
"            <gco:CharacterString>Chlorophyll-a, Aqua MODIS, NPP, 0.05 degrees, Global, Science Quality</gco:CharacterString>\n" +
"          </gmd:descriptor>\n" +
"          <gmd:units xlink:href=\"http://someUnitsDictionary.xml#mg m-3\"/>\n" +
"        </gmd:MD_Band>\n" +
"      </gmd:dimension>\n" +
"      <gmd:dimension>\n" +
"        <gmd:MD_Band>\n" +
"          <gmd:sequenceIdentifier>\n" +
"            <gco:MemberName>\n" +
"              <gco:aName>\n" +
"                <gco:CharacterString>altitude</gco:CharacterString>\n" +
"              </gco:aName>\n" +
"              <gco:attributeType>\n" +
"                <gco:TypeName>\n" +
"                  <gco:aName>\n" +
"                    <gco:CharacterString>double</gco:CharacterString>\n" +
"                  </gco:aName>\n" +
"                </gco:TypeName>\n" +
"              </gco:attributeType>\n" +
"            </gco:MemberName>\n" +
"          </gmd:sequenceIdentifier>\n" +
"          <gmd:descriptor>\n" +
"            <gco:CharacterString>Altitude</gco:CharacterString>\n" +
"          </gmd:descriptor>\n" +
"          <gmd:units xlink:href=\"http://someUnitsDictionary.xml#m\"/>\n" +
"        </gmd:MD_Band>\n" +
"      </gmd:dimension>\n" +
"      <gmd:dimension>\n" +
"        <gmd:MD_Band>\n" +
"          <gmd:sequenceIdentifier>\n" +
"            <gco:MemberName>\n" +
"              <gco:aName>\n" +
"                <gco:CharacterString>lat</gco:CharacterString>\n" +
"              </gco:aName>\n" +
"              <gco:attributeType>\n" +
"                <gco:TypeName>\n" +
"                  <gco:aName>\n" +
"                    <gco:CharacterString>double</gco:CharacterString>\n" +
"                  </gco:aName>\n" +
"                </gco:TypeName>\n" +
"              </gco:attributeType>\n" +
"            </gco:MemberName>\n" +
"          </gmd:sequenceIdentifier>\n" +
"          <gmd:descriptor>\n" +
"            <gco:CharacterString>Latitude</gco:CharacterString>\n" +
"          </gmd:descriptor>\n" +
"          <gmd:units xlink:href=\"http://someUnitsDictionary.xml#degrees_north\"/>\n" +
"        </gmd:MD_Band>\n" +
"      </gmd:dimension>\n" +
"      <gmd:dimension>\n" +
"        <gmd:MD_Band>\n" +
"          <gmd:sequenceIdentifier>\n" +
"            <gco:MemberName>\n" +
"              <gco:aName>\n" +
"                <gco:CharacterString>lon</gco:CharacterString>\n" +
"              </gco:aName>\n" +
"              <gco:attributeType>\n" +
"                <gco:TypeName>\n" +
"                  <gco:aName>\n" +
"                    <gco:CharacterString>double</gco:CharacterString>\n" +
"                  </gco:aName>\n" +
"                </gco:TypeName>\n" +
"              </gco:attributeType>\n" +
"            </gco:MemberName>\n" +
"          </gmd:sequenceIdentifier>\n" +
"          <gmd:descriptor>\n" +
"            <gco:CharacterString>Longitude</gco:CharacterString>\n" +
"          </gmd:descriptor>\n" +
"          <gmd:units xlink:href=\"http://someUnitsDictionary.xml#degrees_east\"/>\n" +
"        </gmd:MD_Band>\n" +
"      </gmd:dimension>\n" +
"      <gmd:dimension>\n" +
"        <gmd:MD_Band>\n" +
"          <gmd:sequenceIdentifier>\n" +
"            <gco:MemberName>\n" +
"              <gco:aName>\n" +
"                <gco:CharacterString>time</gco:CharacterString>\n" +
"              </gco:aName>\n" +
"              <gco:attributeType>\n" +
"                <gco:TypeName>\n" +
"                  <gco:aName>\n" +
"                    <gco:CharacterString>double</gco:CharacterString>\n" +
"                  </gco:aName>\n" +
"                </gco:TypeName>\n" +
"              </gco:attributeType>\n" +
"            </gco:MemberName>\n" +
"          </gmd:sequenceIdentifier>\n" +
"          <gmd:descriptor>\n" +
"            <gco:CharacterString>Centered Time</gco:CharacterString>\n" +
"          </gmd:descriptor>\n" +
"          <gmd:units xlink:href=\"http://someUnitsDictionary.xml#seconds since 1970-01-01T00:00:00Z\"/>\n" +
"        </gmd:MD_Band>\n" +
"      </gmd:dimension>\n" +
"    </gmi:MI_CoverageDescription>\n" +
"  </gmd:contentInfo>\n" +
"  <gmd:distributionInfo>\n" +
"    <gmd:MD_Distribution>\n" +
"      <gmd:distributor>\n" +
"        <gmd:MD_Distributor>\n" +
"          <gmd:distributorContact gco:nilReason=\"missing\"/>\n" +
"          <gmd:distributorFormat>\n" +
"            <gmd:MD_Format>\n" +
"              <gmd:name>\n" +
"                <gco:CharacterString>OPeNDAP</gco:CharacterString>\n" +
"              </gmd:name>\n" +
"              <gmd:version gco:nilReason=\"unknown\"/>\n" +
"            </gmd:MD_Format>\n" +
"          </gmd:distributorFormat>\n" +
"          <gmd:distributorTransferOptions>\n" +
"            <gmd:MD_DigitalTransferOptions>\n" +
"              <gmd:onLine>\n" +
"                <gmd:CI_OnlineResource>\n" +
"                  <gmd:linkage>\n" +
"                    <gmd:URL>http://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/8day.html</gmd:URL>\n" +
"                  </gmd:linkage>\n" +
"                  <gmd:name>\n" +
"                    <gco:CharacterString>File Information</gco:CharacterString>\n" +
"                  </gmd:name>\n" +
"                  <gmd:description>\n" +
"                    <gco:CharacterString>This URL provides a complete description of the data file. Change the extension to .html for an OPeNDAP query interface</gco:CharacterString>\n" +
"                  </gmd:description>\n" +
"                  <gmd:function>\n" +
"                    <gmd:CI_OnLineFunctionCode codeList=\"http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_OnLineFunctionCode\" codeListValue=\"download\">download</gmd:CI_OnLineFunctionCode>\n" +
"                  </gmd:function>\n" +
"                </gmd:CI_OnlineResource>\n" +
"              </gmd:onLine>\n" +
"            </gmd:MD_DigitalTransferOptions>\n" +
"          </gmd:distributorTransferOptions>\n" +
"        </gmd:MD_Distributor>\n" +
"      </gmd:distributor>\n" +
"    </gmd:MD_Distribution>\n" +
"  </gmd:distributionInfo>\n" +
"  <gmd:dataQualityInfo>\n" +
"    <gmd:DQ_DataQuality>\n" +
"      <gmd:scope>\n" +
"        <gmd:DQ_Scope>\n" +
"          <gmd:level>\n" +
"            <gmd:MD_ScopeCode codeList=\"http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_ScopeCode\" codeListValue=\"dataset\">dataset</gmd:MD_ScopeCode>\n" +
"          </gmd:level>\n" +
"        </gmd:DQ_Scope>\n" +
"      </gmd:scope>\n" +
"      <gmd:lineage>\n" +
"        <gmd:LI_Lineage>\n" +
"          <gmd:statement>\n" +
"            <gco:CharacterString>NASA GSFC (OBPG)\n" +
"2011-06-03T10:20:23Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD</gco:CharacterString>\n" +
"          </gmd:statement>\n" +
"        </gmd:LI_Lineage>\n" +
"      </gmd:lineage>\n" +
"    </gmd:DQ_DataQuality>\n" +
"  </gmd:dataQualityInfo>\n" +
"  <gmd:metadataMaintenance>\n" +
"    <gmd:MD_MaintenanceInformation>\n" +
"      <gmd:maintenanceAndUpdateFrequency gco:nilReason=\"unknown\"/>\n" +
"      <gmd:maintenanceNote>\n" +
"        <gco:CharacterString>This record was translated from NcML using UnidataDD2MI.xsl Version 2.0.5</gco:CharacterString>\n" +
"      </gmd:maintenanceNote>\n" +
"    </gmd:MD_MaintenanceInformation>\n" +
"  </gmd:metadataMaintenance>\n" +
"</gmi:MI_Metadata>\n");

    }


}
