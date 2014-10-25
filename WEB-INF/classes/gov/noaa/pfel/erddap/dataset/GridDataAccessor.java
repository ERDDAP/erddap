/* 
 * GridDataAccessor Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.FloatArray;
import com.cohort.array.IntArray;
import com.cohort.array.NDimensionalIndex;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.ShortArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;

import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import gov.noaa.pfel.erddap.variable.EDVGridAxis;

import java.util.Arrays;

/** 
 * This class provides sequential access to the grid data requested by a 
 * grid data query to an EDDGrid.
 * It (usually) gets data in chunks from an EDDGrid subclass by calling
 * getSourceData repeatedly. It then makes the data available in the proper
 * sequence (rowMajor or columnMajor) to the file driver (usually in EDDGrid).
 *
 * <p>Usage: after construction, repeatedly use increment() and then getAxisValueXxx
 * and getDataValueXxx to get the current row's data (as if the data is in 
 * an a1,a2,a3,a4,d1,d2 table).
 * This approach is more cumbersome than just getting the data, but this allows
 * for getting huge amounts of data without using much memory.
 * 
 * <p>The constructor does not get any data from the source. 
 * The first call to increment() causes the first partial response to be obtained 
 * from the source.
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2007-07-06
 */
public class GridDataAccessor { 


    /**
     * Set this to true (by calling verbose=true in your program, 
     * not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false; 

    /**
     * Set this to true (by calling reallyVerbose=true in your program, 
     * not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean reallyVerbose = false; 

    //things passed into the constructor
    protected EDDGrid eddGrid;
    protected String userDapQuery;
    protected boolean rowMajor;
    protected boolean convertToNaN;
    
    //things the constructor generates
    protected int nAxisVariables;
    protected EDV dataVariables[]; //[dv in the query]
    protected IntArray constraints;
    protected int getAllOfNAxes;
    protected NDimensionalIndex totalIndex, driverIndex, partialIndex;
    protected boolean avInDriver[];
    protected Attributes globalAttributes;
    protected EDVGridAxis axisVariables[];
    protected Attributes axisAttributes[];
    protected Attributes dataAttributes[]; //[dv in the query]
    protected PrimitiveArray axisValues[]; //destinationValues for total request
    protected PrimitiveArray partialDataValues[]; //[dv in the query]
    protected long totalNBytes;

    /**
     * This is the constructor.
     * This constructor sets everything up, but doesn't get any grid data.
     *
     * @param tEDDGrid   the data source
     * @param tRequestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     *     Here, it is just used for history metadata.
     * @param tUserDapQuery  the original user DAP-style query after the '?', still percentEncoded, may be null.
     * @param tRowMajor  
     *    Set this to true if you want to get the data in row major order. 
     *    Set this to false if you want to get the data in column major order. 
     * @param tConvertToNaN  set this to True if you want the GridDataAccessor
     *    to convert stand-in missing values (e.g., -9999999.0, as identified
     *    by the missing_value or _FillValue metadata) to NaNs.
     * @throws Throwable if trouble
     */
    public GridDataAccessor(EDDGrid tEDDGrid, String tRequestUrl, String tUserDapQuery, 
        boolean tRowMajor, boolean tConvertToNaN) throws Throwable {

        eddGrid = tEDDGrid;
        userDapQuery = tUserDapQuery;
        rowMajor = tRowMajor;
        convertToNaN = tConvertToNaN;
        if (reallyVerbose) String2.log(
            "\n    GridDataAccessor constructor" + 
            "\n      EDDGrid=" + eddGrid.datasetID() +
            "\n      userDapQuery=" + userDapQuery +
            "\n      rowMajor=" + rowMajor +
            "\n      convertToNaN=" + convertToNaN);

        //parse the query
        StringArray destinationNames = new StringArray();
        constraints = new IntArray();
        eddGrid.parseDataDapQuery(userDapQuery, destinationNames, constraints, false);
        dataVariables = new EDV[destinationNames.size()];
        partialDataValues = new PrimitiveArray[destinationNames.size()];
        for (int dv = 0; dv < destinationNames.size(); dv++) {
            dataVariables[dv] = eddGrid.findDataVariableByDestinationName(destinationNames.get(dv));
        }
        if (reallyVerbose) String2.log(
            "      dataVariables=" + destinationNames +
            "\n      constraints=" + constraints);

        //make globalAttributes
        globalAttributes = new Attributes(eddGrid.combinedGlobalAttributes()); //make a copy

        //fix up global attributes  (always to a local COPY of global attributes)
        EDD.addToHistory(globalAttributes, eddGrid.publicSourceUrl());
        EDD.addToHistory(globalAttributes, 
            EDStatic.baseUrl + tRequestUrl + 
            (tUserDapQuery == null || tUserDapQuery.length() == 0? "" : "?" + tUserDapQuery));

        //make axisValues and axisAttributes
        nAxisVariables = eddGrid.axisVariables.length;
        axisValues = new PrimitiveArray[nAxisVariables];
        axisAttributes = new Attributes[nAxisVariables];
        axisVariables = eddGrid.axisVariables();
        int constraintsI = 0;
        int totalShape[] = new int[nAxisVariables];
        for (int av = 0; av < nAxisVariables; av++) {            
            //make axisValues
            axisValues[av] = axisVariables[av].sourceValues().subset(constraints.get(constraintsI),
                constraints.get(constraintsI + 1), constraints.get(constraintsI + 2));
            constraintsI += 3;

            //make totalShape
            totalShape[av] = axisValues[av].size();

            //make axisAttributes
            axisAttributes[av] = new Attributes(axisVariables[av].combinedAttributes()); //make a copy

            //convert source values to destination values  
            //(e.g., convert datatype and apply scale_factor/scaleFactor and add_offset/addOffset)
            axisValues[av] = axisVariables[av].toDestination(axisValues[av]);

            //setActualRangeAndBoundingBox  (see comments in method javadocs above)
            //if no data, don't specify range
            //actual_range is type-specific
            double dMin = axisValues[av].getDouble(0);
            double dMax = axisValues[av].getDouble(axisValues[av].size() - 1);
            if (dMin > dMax) {
                double d = dMin; dMin = dMax; dMax = d;
            }
            PrimitiveArray minMax = PrimitiveArray.factory(axisValues[av].elementClass(), 2, false);
            minMax.addDouble(dMin);
            minMax.addDouble(dMax);

            if (Double.isNaN(dMin)) 
                 axisAttributes[av].remove("actual_range");
            else axisAttributes[av].set("actual_range", minMax);

            //remove/set acdd-style and google-style bounding box
            float fMin = Math2.doubleToFloatNaN(dMin);
            float fMax = Math2.doubleToFloatNaN(dMax);
            int   iMin = Math2.roundToInt(dMin);
            int   iMax = Math2.roundToInt(dMax);
            if (av == eddGrid.lonIndex) {
                if (minMax instanceof FloatArray) {
                    globalAttributes.set("geospatial_lon_min",  fMin);
                    globalAttributes.set("geospatial_lon_max",  fMax);
                    globalAttributes.set("Westernmost_Easting", fMin);
                    globalAttributes.set("Easternmost_Easting", fMax);
                } else {
                    globalAttributes.set("geospatial_lon_min",  dMin);
                    globalAttributes.set("geospatial_lon_max",  dMax);
                    globalAttributes.set("Westernmost_Easting", dMin);
                    globalAttributes.set("Easternmost_Easting", dMax);
                }
            } else if (av == eddGrid.latIndex) {
                if (minMax instanceof FloatArray) {
                    globalAttributes.set("geospatial_lat_min", fMin);
                    globalAttributes.set("geospatial_lat_max", fMax);
                    globalAttributes.set("Southernmost_Northing", fMin);
                    globalAttributes.set("Northernmost_Northing", fMax);
                } else {
                    globalAttributes.set("geospatial_lat_min", dMin);
                    globalAttributes.set("geospatial_lat_max", dMax);
                    globalAttributes.set("Southernmost_Northing", dMin);
                    globalAttributes.set("Northernmost_Northing", dMax);
                } 
            } else if (av == eddGrid.altIndex) {
                globalAttributes.set("geospatial_vertical_positive", "up");
                globalAttributes.set("geospatial_vertical_units", EDV.ALT_UNITS);
                if (minMax instanceof FloatArray) {
                    globalAttributes.set("geospatial_vertical_min", fMin); //unidata-related
                    globalAttributes.set("geospatial_vertical_max", fMax);
                } else if (minMax instanceof IntArray ||
                           minMax instanceof ShortArray ||
                           minMax instanceof ByteArray) {
                    globalAttributes.set("geospatial_vertical_min", iMin); //unidata-related
                    globalAttributes.set("geospatial_vertical_max", iMax);
                } else {
                    globalAttributes.set("geospatial_vertical_min", dMin); //unidata-related
                    globalAttributes.set("geospatial_vertical_max", dMax);
                }
            } else if (av == eddGrid.depthIndex) {
                globalAttributes.set("geospatial_vertical_positive", "down");
                globalAttributes.set("geospatial_vertical_units", EDV.DEPTH_UNITS);
                if (minMax instanceof FloatArray) {
                    globalAttributes.set("geospatial_vertical_min", fMin); //unidata-related
                    globalAttributes.set("geospatial_vertical_max", fMax);
                } else if (minMax instanceof IntArray ||
                           minMax instanceof ShortArray ||
                           minMax instanceof ByteArray) {
                    globalAttributes.set("geospatial_vertical_min", iMin); //unidata-related
                    globalAttributes.set("geospatial_vertical_max", iMax);
                } else {
                    globalAttributes.set("geospatial_vertical_min", dMin); //unidata-related
                    globalAttributes.set("geospatial_vertical_max", dMax);
                }
            } else if (av == eddGrid.timeIndex) {
                String tp = axisAttributes[av].getString(EDV.TIME_PRECISION);
                //"" unsets the attribute if dMin or dMax isNaN
                globalAttributes.set("time_coverage_start", 
                    Calendar2.epochSecondsToLimitedIsoStringT(tp, dMin, ""));
                //for tables (not grids) will be NaN for 'present'.   Deal with this better???
                globalAttributes.set("time_coverage_end", 
                    Calendar2.epochSecondsToLimitedIsoStringT(tp, dMax, ""));
            }
        }

        //make totalIndex
        totalIndex = new NDimensionalIndex(totalShape);
        if (reallyVerbose) String2.log("      totalShape=" + String2.toCSSVString(totalShape));

        //make the dataAttributes
        dataAttributes = new Attributes[dataVariables.length];
        int nDataBytesPerRow = 0;
        for (int dv = 0; dv < dataVariables.length; dv++) {  //dv in the query

            //add dataAttributes
            dataAttributes[dv] = new Attributes(dataVariables[dv].combinedAttributes()); //make a copy
//dataAttributes NEEDS actual_range, and ... , but not available, so remove...
            dataAttributes[dv].remove("actual_range");
            //String2.log("      dataAttributes[" + dv + "]=\n" + dataAttributes[dv]);
            nDataBytesPerRow += dataVariables[dv].destinationBytesPerElement();        

        }
        if (reallyVerbose) String2.log("      nDataBytesPerRow=" + nDataBytesPerRow);

        //decide how many axes will be obtained completely by each partial request
        getAllOfNAxes = 0; //each partialRequest gets all the data for these axes
        // and driverIndex (which drives partial requests)
        int driverShape[] = new int[nAxisVariables]; 
        System.arraycopy(totalShape, 0, driverShape, 0, nAxisVariables);
        // and partialIndex (which holds data from one partial request)
        int partialShape[] = new int[nAxisVariables]; 
        Arrays.fill(partialShape, 1);
        avInDriver = new boolean[nAxisVariables]; 
        Arrays.fill(avInDriver, true);
        long nBytesPerPartialRequest = nDataBytesPerRow; //long to safely avoid overflow
        if (rowMajor) {
            //work from right
            int av = axisAttributes.length - 1;
            boolean keepGoing = true;
            while (av >= 0 &&
                (keepGoing || 
                    nBytesPerPartialRequest * totalShape[av] < EDStatic.partialRequestMaxBytes)) {
                driverShape[av] = 1;
                partialShape[av] = totalShape[av];
                avInDriver[av] = false;
                nBytesPerPartialRequest *= totalShape[av];
                if (totalShape[av] > 1) keepGoing = false; //include at least 1 totalShape[av] which is > 1
                getAllOfNAxes++;
                av--;
            }              
        } else {
            //work from right
            int av = 0;
            boolean keepGoing = true;
            while (av < axisAttributes.length &&
                (keepGoing || 
                    nBytesPerPartialRequest * totalShape[av] < EDStatic.partialRequestMaxBytes)) {
                driverShape[av] = 1;
                partialShape[av] = totalShape[av];
                avInDriver[av] = false;
                nBytesPerPartialRequest *= totalShape[av];
                if (totalShape[av] > 1) keepGoing = false; //include at least 1 totalShape[av] which is > 1
                getAllOfNAxes++;
                av++;
            }
        }
        EDStatic.ensureMemoryAvailable(nBytesPerPartialRequest, "GridDataAccessor");
        driverIndex = new NDimensionalIndex(driverShape);
        partialIndex = new NDimensionalIndex(partialShape);
        EDStatic.ensureArraySizeOkay(driverIndex.size(), "GridDataAccessor");  //ensure not >Integer.MAX_VALUE chunks (will never finish!)
        EDStatic.ensureArraySizeOkay(partialIndex.size(), "GridDataAccessor"); //ensure each chunk size() is ok
        totalNBytes = driverIndex.size() * nBytesPerPartialRequest; //driverIndex.size() is a long
        if (reallyVerbose) String2.log("      getAllOfNAxes=" + getAllOfNAxes + 
            //driverShape e.g., [15][1][1][1],  note getAllOfNAxes 1's on right if row-major
            "\n      driverShape=" + String2.toCSSVString(driverShape) +  
            //partialShape e.g., [1][1][43][45],  note 1's on left if row-major
            "\n      partialShape=" + String2.toCSSVString(partialShape) +  
            "\n      nBytesPerPartialRequest=" + nBytesPerPartialRequest +
            "\n      totalNBytes=" + totalNBytes);
    }

    /**
     * This returns the totalIndex.
     *
     * @return totalIndex so you can call .size(), .shape(), .current(), ....
     *    Don't call totalIndex.increment() or make other changes to its state.
     */
    public  NDimensionalIndex totalIndex() {
        return totalIndex;
    }

    /**
     * This returns the driverIndex.
     *
     * @return driverIndex so you can call .size(), .shape(), .current(), ....
     *    Don't call driverIndex.increment() or make other changes to its state.
     */
    public  NDimensionalIndex driverIndex() {
        return driverIndex;
    }

    /**
     * This returns the partialIndex.
     *
     * @return partialIndex so you can call .size(), .shape(), .current(), ....
     *    Don't call partialIndex.increment() or make other changes to its state.
     */
    public  NDimensionalIndex partialIndex() {
        return partialIndex;
    }

    /**
     * This returns the total number of data bytes.
     *
     * @return the total number of data bytes
     */
    public long totalNBytes() {
        return totalNBytes;
    }

    /** 
     * This returns the EDDGrid that is the source of this data.
     *
     * @return the EDDGrid that is the source of this data.
     */
    public EDDGrid eddGrid() {return eddGrid; }

    /** 
     * This returns the userDapQuery used to make this.
     *
     * @return the userDapQuery used to make this, still percentEncoded, may be null.
     */
    public String userDapQuery() {return userDapQuery; }

    /** 
     * This returns true if this is a rowMajor acceessor.
     *
     * @return true if this is a rowMajor acceessor.
     */
    public boolean rowMajor() {return rowMajor; }

    /** 
     * This returns the constraints derived from the userDapQuery. 
     * This is the internal data structure, so don't change it.
     *
     * @return the constraints derived from the userDapQuery. 
     */
    public IntArray constraints() {return constraints; }

    /** 
     * This returns the constraints derived from the userDapQuery as a String. 
     *
     * @return the constraints string derived from the userDapQuery. 
     */
    public String constraintsString() {
        return EDDGrid.buildDapArrayQuery(constraints);
    }

    /** 
     * This returns the dataVariables included in the query. 
     *
     * @return the dataVariables included in the query. 
     */
    public EDV[] dataVariables() {return dataVariables; }

    /** 
     * This returns the global attributes (source + add). 
     * @return the global attributes (source + add). 
     */
    public Attributes globalAttributes() {return globalAttributes; }

    /** 
     * This returns the Attributes (source + add) for an axisVariable. 
     * There is one for each axisVariable in the EDDGrid dataset. 
     *
     * @param av the axis variable index
     * @return the Attributes (source + add) for an axisVariable. 
     */
    public Attributes axisAttributes(int av) {return axisAttributes[av]; }

    /** 
     * This returns the Attributes (source + add) for a dataVariable. 
     * There is one for each results dataVariable. 
     *
     * @param dv the data variable index in the query
     * @return the Attributes (source + add) for a dataVariable. 
     */
    public Attributes dataAttributes(int dv) {return dataAttributes[dv]; }

    /** 
     * This returns the axis values (for the entire response) for an axisVariable. 
     * There is one for each axisVariable in the EDDGrid dataset. 
     *
     * @param av the axis variable index
     * @return the axis values (for the entire response) for an axisVariable. 
     */
    public PrimitiveArray axisValues(int av) {return axisValues[av]; }

    /**
     * This increments to the next axis and data values in the 
     * row-major or column-major sequence.
     * After calling this, call getAxisValueAsXxx or getDataValueAsXxx to get the
     * current values.
     *
     * <p>This increments the totalIndex and partialIndex (and indirectly sometimes the driverIndex).
     *
     * @return true if successful (false if done)
     * @throws Throwable if trouble (e.g., error while getting data from source)
     */
    public boolean increment() throws Throwable {

        //increment totalIndex
        boolean tb = rowMajor? totalIndex.increment() : totalIndex.incrementCM();
        if (!tb)
            return false;

        //increment the partial index
        tb = rowMajor? partialIndex.increment() : partialIndex.incrementCM();
        if (!tb) {
            partialIndex.reset();
            tb = rowMajor? partialIndex.increment() : partialIndex.incrementCM();
        }

        //need to get more partial data from source? 
        if (partialIndex.getIndex() == 0) 
            getChunk();

        return true;
    }

    /**
     * This is an alternative to increment() that advances to the next chunk of data.
     * After using this, use getPartialDataValues.
     *
     * <p>This increments the totalIndex (and indirectly the driverIndex).
     *
     * @return true if new data is available; or false if no more.
     * @throws Throwable if trouble (e.g., error while getting source data)
     */
    public boolean incrementChunk() throws Throwable {
//Use of this with column major is untested!!!

        //increment totalIndex
        if (totalIndex.getIndex() == -1) {
            //first time
            boolean tb = rowMajor? totalIndex.increment() : totalIndex.incrementCM();
            if (!tb)
                return false;
            tb = rowMajor? partialIndex.increment() : partialIndex.incrementCM(); //should succeed
        } else {
            //subsequent times
            //increment totalIndex by partialIndex.size, for row major or column major
            if (totalIndex.getIndex() + partialIndex.size() >= totalIndex.size())
                return false;
            totalIndex.setIndex(totalIndex.getIndex() + partialIndex.size());
        }

        getChunk(); //works for rowMajor or columnMajor

        return true;
    }




    /**
     * This is used by increment and incrementChunk to get a chunk of data.
     * "GridDataAccessor.getChunk" in stack trace is elsewhere used as indication
     * of something seriously wrong with the data source.
     *
     * <p>This increments the driverIndex.
     *
     * @throws Throwable if driverIndex fails to increment (e.g., no more data)
     *    or unable to get the data.
     */
    protected void getChunk() throws Throwable {
        
        //increment driverIndex
        boolean tb = rowMajor? driverIndex.increment() : driverIndex.incrementCM();
        if (!tb)
            throw new RuntimeException("GridDataAccessor.increment: driverIndex failed to increment" +
                "at mainIndex.current=" + String2.toCSSVString(totalIndex.getCurrent()));
        if (reallyVerbose) String2.log("      GridDataAccessor.increment getting partial data; driverIndex=[" + 
            String2.toCSSVString(driverIndex.getCurrent()) + "]");

        //generate the partial constraint
        IntArray partialConstraints = new IntArray(constraints);
        int driverCurrent[] = driverIndex.getCurrent();
        int pcPo = 0;
        double avInDriverExpectedValues[] = new double[nAxisVariables]; //source value
        for (int av = 0; av < nAxisVariables; av++) {
            if (avInDriver[av]) {
                //get 1 value: driverCurrent indicates 'which' in 0,1,2... form
                //so need calculate source 'which' based on total constraint
                int which = constraints.get(pcPo + 0) +
                    driverCurrent[av] * constraints.get(pcPo + 1);
                partialConstraints.set(pcPo + 0, which);
                partialConstraints.set(pcPo + 1, 1);
                partialConstraints.set(pcPo + 2, which);
                avInDriverExpectedValues[av] = axisVariables[av].sourceValues().getDouble(which);
            } //no change if !avInDriver[av]
            pcPo += 3;
        }

        //get the data
        PrimitiveArray partialResults[] = null;
        try {
            if (reallyVerbose) 
                String2.log("      calling getSourceData partialConstraints=" + partialConstraints);
            long time = System.currentTimeMillis();
            partialResults = eddGrid.getSourceData(dataVariables, partialConstraints);
            if (reallyVerbose) 
                String2.log("      getSourceData done. nDV=" + dataVariables.length +
                    " nElements/dv=" + partialResults[partialResults.length - 1].size() +
                    " time=" + (System.currentTimeMillis() - time));
            //for (int i = 0; i < partialResults.length; i++)
            //    String2.log("!pa[" + i + "]=" + partialResults[i]);

        } catch (WaitThenTryAgainException twwae) {
            throw twwae;

        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}

            //if too much data, rethrow t
            String tToString = t.toString();
            if (tToString.indexOf(Math2.memoryTooMuchData) >= 0)
                throw t;

            //rewrap it as WTTAE
            throw new WaitThenTryAgainException(EDStatic.waitThenTryAgain + 
                "\n(" + EDStatic.errorFromDataSource + tToString + ")", 
                t); 
        }

        //check that axisValues are as expected
        for (int av = 0; av < nAxisVariables; av++) {
            PrimitiveArray pa = partialResults[av];
            if (avInDriver[av]) {
                if (pa.size() != 1 ||
                    !Math2.almostEqual(9, pa.getDouble(0), avInDriverExpectedValues[av])) { //source values
                    throw new WaitThenTryAgainException(EDStatic.waitThenTryAgain +
                        "\n(Details: GridDataAccessor.increment: partialResults[" + av +
                        "]=\"" + pa + "\" was expected to be " + 
                        avInDriverExpectedValues[av] + ".)");
                }
            } else {
                //convert source values to destination values
                pa = axisVariables[av].toDestination(pa);
                String tError = axisValues[av].almostEqual(pa); //destination values
                if (tError.length() > 0) 
                    throw new WaitThenTryAgainException(EDStatic.waitThenTryAgain +
                        "\n(Details: GridDataAccessor.increment: partialResults[" + 
                        av + "] was not as expected.\n" + 
                        tError + ")");
            }
        }
            
        //process the results
        for (int dv = 0; dv < dataVariables.length; dv++) { //dv in the query
            //convert source values to destination values and store
            //String2.log("!source  dv=" + dataVariables[dv].destinationName() + " " + partialResults[nAxisVariables + dv]);
            partialDataValues[dv] = dataVariables[dv].toDestination(partialResults[nAxisVariables + dv]);
            //String2.log("!dest    dv=" + dataVariables[dv].destinationName() + " " + partialDataValues[dv]);

            //convert missing_value to NaN
            if (convertToNaN) {
                double mv = dataVariables[dv].destinationMissingValue();
                double fv = dataVariables[dv].destinationFillValue();
                if (!Double.isNaN(mv))
                    partialDataValues[dv].switchFromTo("" + mv, ""); //for e.g., byte mv=127, ByteArray will detect 127=127 and do nothing
                if (!Double.isNaN(fv) && fv != mv)   //if mv is NaN, fv!=mv will be true
                    partialDataValues[dv].switchFromTo("" + fv, "");
            }
        }
    }

    /** 
     * Call this after incrementChunk() to get a chunk of data in a PrimitiveArray.
     *
     * @param dv a dataVariable number in the query
     * @return the PrimitiveArray with the chunk of data for this dv
     */
     public PrimitiveArray getPartialDataValues(int dv) {
         return partialDataValues[dv]; 
     }


    /**
     * Call this after increment() to get a current axis destination value (as an int).
     *
     * @param av an axisVariable number
     * @return the axis destination value
     */
    public int getAxisValueAsInt(int av) {
        return axisValues[av].getInt(totalIndex.getCurrent()[av]);
    }

    /**
     * Call this after increment() to get a current axis destination value (as a long).
     *
     * @param av an axisVariable number
     * @return the axis destination value
     */
    public long getAxisValueAsLong(int av) {
        return axisValues[av].getLong(totalIndex.getCurrent()[av]);
    }

    /**
     * Call this after increment() to get a current axis destination value (as a float).
     *
     * @param av an axisVariable number
     * @return the axis destination value
     */
    public float getAxisValueAsFloat(int av) {
        return axisValues[av].getFloat(totalIndex.getCurrent()[av]);
    }

    /**
     * Call this after increment() to get a current axis destination value (as a double).
     *
     * @param av an axisVariable number
     * @return the axis destination value
     */
    public double getAxisValueAsDouble(int av) {
        return axisValues[av].getDouble(totalIndex.getCurrent()[av]);
    }

    /**
     * Call this after increment() to get a current axis destination value (as a String).
     *
     * @param av an axisVariable number
     * @return the axis destination value
     */
    public String getAxisValueAsString(int av) {
        return axisValues[av].getString(totalIndex.getCurrent()[av]);
    }

    /**
     * Call this after increment() to get the current data value (as an int) 
     * from the specified dataVariable.
     *
     * <p>If partialDataValues[dv] is a ByteArray (or ShortArray or CharArray), this
     * will return standard missing value as Integer.MAX_VALUE (not Byte.MAX_VALUE).
     *
     * @param dv a dataVariable number in the query 
     * @return the data value
     */
    public int getDataValueAsInt(int dv) {
        return partialDataValues[dv].getInt((int)partialIndex.getIndex()); //safe since partialIndex size checked when constructed
    }

    /**
     * Call this after increment() to get the current data value (as a long) 
     * from the specified dataVariable.
     *
     * @param dv a dataVariable number in the query 
     * @return the data value
     */
    public long getDataValueAsLong(int dv) {
        return partialDataValues[dv].getLong((int)partialIndex.getIndex()); //safe since partialIndex size checked when constructed
    }

    /**
     * Call this after increment() to get the current data value (as a float) 
     * from the specified dataVariable.
     *
     * @param dv a dataVariable number in the query
     * @return the data value
     */
    public float getDataValueAsFloat(int dv) {
        return partialDataValues[dv].getFloat((int)partialIndex.getIndex()); //safe since partialIndex size checked when constructed
    }

    /**
     * Call this after increment() to get the current data value (as a double) 
     * from the specified dataVariable.
     *
     * @param dv a dataVariable number in the query 
     * @return the data value
     */
    public double getDataValueAsDouble(int dv) {
        return partialDataValues[dv].getDouble((int)partialIndex.getIndex()); //safe since partialIndex size checked when constructed
    }

    /**
     * Call this after increment() to get the current data value (as a String) 
     * from the specified dataVariable.
     *
     * @param dv a dataVariable number in the query
     * @return the data value
     */
    public String getDataValueAsString(int dv) {
        return partialDataValues[dv].getString((int)partialIndex.getIndex()); //safe since partialIndex size checked when constructed
    }



}
