/* 
 * TableWriterOrderByMean Copyright 2018, NOAA & Irish Marine Institute
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.Math2;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;

/**
 * TableWriterOrderByMean provides a way summarize the response table's rows,
 * and just keep the row where the values hold the mean.
 * For example, you could use orderBy(\"stationID,time,1 day\") to get the mean
 * daily values for each station.
 *
 * <p>This doesn't do anything to missing values and doesn't assume they are
 * stored as NaN or fake missing values.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2018-09-11
 * @author Rob Fuller (rob.fuller@marine.ie) 2018-09-11
 * @author Adam Leadbetter (adam.leadbetter@marine.ie) 2018-09-11
 */
public class TableWriterOrderByMean extends TableWriterAll {


    //set by constructor
    protected final TableWriter otherTableWriter;
    public String orderBy[];
    // maintains count of the number of values in average
    protected final Map<String,int[]> counts = new HashMap<String,int[]>();
    protected final Map<String,Integer> rowmap = new HashMap<String,Integer>();
    // used when calculating degree means at the end, one key for each row.
    protected final StringArray keymap = new StringArray();
    protected final Map<String,DegreesAccumulator> degreesMap = new HashMap<String,DegreesAccumulator>();
    
	private int[] keyCols;
	private BitSet isKeyCol;
	private BitSet cannotMeanCol;
	private BitSet degreesCol;
	private BitSet wasDecimalCol;
	private int timeCol = -1;
	private boolean configured = false;
	private Table meansTable;
	private final Map<String,Rounder> rounders = new HashMap<String,Rounder>();
	private final Pattern alphaPattern = Pattern.compile("\\p{Alpha}");
	

    private static interface Rounder{
       double round(Double time) throws Exception;
    }
    /**
     * The constructor.
     *
     * @param tDir a private cache directory for storing the intermediate files,
     *    usually cacheDirectory(datasetID)
     * @param tFileNameNoExt is the fileName without dir or extension (used as basis for temp files).
     *     A random number will be added to it for safety.
     * @param tOtherTableWriter the tableWriter that will receive the unique rows
     *   found by this tableWriter.
     * @param tOrderByCsv the names of the columns to sort by (most to least important)
     */
    public TableWriterOrderByMean(EDD tEdd, String tNewHistory, String tDir, 
        String tFileNameNoExt, TableWriter tOtherTableWriter, String tOrderByCsv) {

        super(tEdd, tNewHistory, tDir, tFileNameNoExt); 
        otherTableWriter = tOtherTableWriter;
        String[] cols = (tOrderByCsv == null || tOrderByCsv.trim().length() == 0) ? new String[]{} : String2.split(tOrderByCsv, ',');
        // filter out the blanks.
        cols =  Arrays.stream(cols).filter(value -> value.trim().length() > 0).toArray(size -> new String[size]);
        // support the old format where interval was the last field.
        int last = cols.length;
        if(cols.length > 1 && cols[cols.length-1].trim().matches("^\\d")) {
        	last--;
        	cols[cols.length - 2] += "/" + cols[cols.length-1];
        }
    	orderBy = new String[last];
        for(int col=0; col<last; col++) {
        	String[] parts = cols[col].split("/",2);
        	String colName = parts[0].trim();
        	orderBy[col] = colName;
        	if(parts.length == 2) {
        		rounders.put(colName, createRounder(parts[1],cols[col]));
        	}
        }
    }


	private Rounder createRounder(String str,String param) {
		try {
			str = str.replaceAll("\\s", "");
			String[] parts = str.split(":",2);
			Matcher alphaMatcher = alphaPattern.matcher(parts[0]);
			if(alphaMatcher.find()) { // eg: 2days.
				if(parts.length == 2) {
					throw new IllegalArgumentException(Table.QUERY_ERROR + Table.ORDER_BY_MEAN_ERROR + 
		                    " could not parse "+param+", offset not allowed with date intervals");
				}
				return createTimeRounder(str, param);
			} else {
				final double numberOfUnits = Double.parseDouble(parts[0]);
				if(parts.length == 2) {
					final double offset = Double.parseDouble(parts[1]);
					return (d)->(Math.floor((d-offset)  / numberOfUnits));
				}
				return (d)->(Math.floor(d  / numberOfUnits));
			}
		}catch(IllegalArgumentException e) {
			throw e;
		}catch(Throwable t) {
			throw new IllegalArgumentException(Table.QUERY_ERROR + Table.ORDER_BY_MEAN_ERROR + 
                    " could not parse "+param+", format should be variable[/interval[:offset]]");		
		}
	}


    private Rounder createTimeRounder(String str, String param) {
    	final double[] numberTimeUnits = Calendar2.parseNumberTimeUnits(str);
        if (numberTimeUnits.length != 2)
            throw new IllegalArgumentException(Table.QUERY_ERROR + Table.ORDER_BY_MEAN_ERROR + 
                "could not parse "+param+", (numberTimeUnits.length must be 2)"); 
        if (!Double.isFinite(numberTimeUnits[0]) || 
            !Double.isFinite(numberTimeUnits[1]))
            throw new IllegalArgumentException(Table.QUERY_ERROR + Table.ORDER_BY_MEAN_ERROR + 
            	"could not parse "+param+", (numberTimeUnits values can't be NaNs)"); 
        if (numberTimeUnits[0] <= 0 || numberTimeUnits[1] <= 0)
            throw new IllegalArgumentException(Table.QUERY_ERROR + Table.ORDER_BY_MEAN_ERROR + 
            	"could not parse "+param+", (numberTimeUnits values must be positive numbers)"); 
        final double simpleInterval = numberTimeUnits[0] * numberTimeUnits[1];
        final int field = 
            numberTimeUnits[1] ==  30 * Calendar2.SECONDS_PER_DAY? Calendar2.MONTH :
            numberTimeUnits[1] == 360 * Calendar2.SECONDS_PER_DAY? Calendar2.YEAR : //but see getYear below
            Integer.MAX_VALUE;
        final int intNumber = Math2.roundToInt(numberTimeUnits[0]); //used for Month and Year
        if (field != Integer.MAX_VALUE &&
            (intNumber < 1 || intNumber != numberTimeUnits[0])) 
            throw new IllegalArgumentException(Table.QUERY_ERROR + Table.ORDER_BY_MEAN_ERROR + 
            	"could not parse "+param+", (The number of months or years must be a positive integer.)"); 
        if (field == Calendar2.MONTH && (intNumber == 5 || intNumber > 6)) 
            throw new IllegalArgumentException(Table.QUERY_ERROR + Table.ORDER_BY_MEAN_ERROR + 
            	"could not parse "+param+", (The number of months must be one of 1,2,3,4, or 6.)");
        
        if(field == Integer.MAX_VALUE) {
            return (d) -> d - d % simpleInterval;
        }else {
        	return (d) -> {
        		GregorianCalendar gc = Calendar2.epochSecondsToGc(d);
        		Calendar2.clearSmallerFields(gc, field);
                while ((field == Calendar2.YEAR? Calendar2.getYear(gc) : gc.get(field)) % intNumber != 0)
                     gc.add(field, -1);
                return Calendar2.gcToEpochSeconds(gc);
        	};
        }
       }


	/**
     * This adds the current contents of table (a chunk of data) to the OutputStream.
     * This calls ensureCompatible each time it is called.
     * If this is the first time this is called, this does first time things
     *   (e.g., call OutputStreamSource.outputStream() and write file header).
     * The number of columns, the column names, and the types of columns 
     *   must be the same each time this is called.
     *
     * @param table with destinationValues
     * @throws Throwable if trouble
     */
    public void writeSome(Table table) throws Throwable {
    	int nRows = table.nRows();
        if (nRows == 0) 
            return;
        if(!configured) {
            configured = configure(table);// throws Exception
        }
        StringBuilder sbKey = new StringBuilder();
        int nCols = table.nColumns();
        ROW:
        for(int row = 0; row < nRows; row++) {
        	sbKey.setLength(0);
        	for(int i=0; i<this.keyCols.length; i++) {
        		int col = this.keyCols[i];
        		PrimitiveArray column = table.getColumn(col);
        		String columnName = table.getColumnName(col);
        		if(column.isFloatingPointType() || column.isIntegerType()) {
        			double value = column.getDouble(row);
        			if(this.rounders.containsKey(columnName)) {
        				if(value == Double.NaN) {
        					// No value, cannot group by this...
        					continue ROW;
        				}
        				value = this.rounders.get(columnName).round(value);
        			}
        			sbKey.append(value);
        		}else {
        			sbKey.append(column.getString(row));
        		}
        		sbKey.append(":");
        	}
        	String key = sbKey.toString();
        	int[] counts = this.counts.get(key);
        	if(counts == null) {
        		counts = new int[nCols];
        		int idx = this.counts.size();
        		this.counts.put(key, counts);
        		for(int col=0;col<nCols;col++) {
            		PrimitiveArray column = table.getColumn(col);
            		String value = column.getRawString(row);
            		this.meansTable.getColumn(col).addString(value);
        		}
        		this.rowmap.put(key, idx);
        		this.keymap.add(key);
        	}
        	int idx = this.rowmap.get(key);
        	for(int col=0;col<nCols;col++) {
        		PrimitiveArray column = table.getColumn(col);
        		if(this.cannotMeanCol.get(col)) {
        			// Keep the value only if all rows are the same.
        			String value = column.getRawString(row);
        			String prev = this.meansTable.getColumn(col).getRawString(idx);
        			if(!("".equals(prev)||prev.equals(value))) {
        				this.meansTable.setStringData(col, idx, "");
        			}
        			continue;
        		}
        		if(!(column.isFloatingPointType() || column.isIntegerType())){
        			this.meansTable.setStringData(col, idx, column.getRawString(row));
        			continue;
        		}
        		double value = table.getDoubleData(col, row);
        		if(value == Double.NaN) {
        			continue;
        		}
        		if(this.degreesCol.get(col)) {
        			// is the value constant?
        			if(counts[col] == 0) {
        				this.meansTable.setDoubleData(col, idx, value);
        				counts[col]++;
        				continue;
        			}
        			double oldValue = this.meansTable.getDoubleData(col, idx);
        			if(counts[col] > 0 && value == oldValue) {
        				counts[col]++;
        				continue;
        			}
        			if(counts[col]>0) {
        				for(int k = 0; k< counts[col]; k++) {
                			this.accumulateDegrees( key + ":" + col,oldValue);
        				}
        				counts[col] = -1;
        			}
        			this.accumulateDegrees( key + ":" + col,value);
        			continue;
        		}
        		counts[col] += 1;
        		if(counts[col] == 1) {
        			this.meansTable.setDoubleData(col, idx, value);
        			continue;
        		}
        		double mean = this.meansTable.getDoubleData(col, idx);
        		mean = mean + (value-mean)/counts[col];
        		this.meansTable.setDoubleData(col, idx, mean);
        	}
        }
    }
    

	private boolean isTimeColumn(Table table, int col) {
    	String units = table.columnAttributes(col).getString("units");
        return "time".equals(table.getColumnName(col)) || EDV.TIME_UNITS.equals(units);
    }
    
    private boolean isDegreeUnitsColumn(Table table, int col) {
    	String units = table.columnAttributes(col).getString("units");
        return units != null && EDStatic.angularDegreeUnitsSet.contains(units);
    }
    
    /*
     * Find the key columns and possibly the time column.
     */
	private boolean configure(Table table) throws SimpleException{
		int nKeyCols = orderBy.length;
		int ncols = table.nColumns();
		ArrayList <Integer> keyCols= new ArrayList<Integer>();
		this.isKeyCol = new BitSet(ncols);
		this.cannotMeanCol = new BitSet(ncols);
		this.degreesCol = new BitSet(ncols);
		this.wasDecimalCol = new BitSet(ncols);
		for (int k = 0; k < nKeyCols; k++) {
		    int col = table.findColumnNumber(orderBy[k]);
		    if (col < 0)
		        throw new SimpleException(Table.QUERY_ERROR + Table.ORDER_BY_MEAN_ERROR + 
		            " (unknown orderBy column=" + orderBy[k] + ")");
		    keyCols.add(col);
		    isKeyCol.set(col);
		}
		this.rounders.keySet().forEach((columnName)-> {
			PrimitiveArray column = table.getColumn(columnName);
			if(!(column.isIntegerType() || column.isFloatingPointType())) {
		        throw new SimpleException(Table.QUERY_ERROR + Table.ORDER_BY_MEAN_ERROR + 
			            " (cannot group numerically from column =" + columnName + ")");
				
			}
		});
		this.keyCols = keyCols.stream().mapToInt(i -> i).toArray();
		String colName[] = new String[ncols];
		String dataType[] = new String[ncols];
		for(int col = 0; col<ncols; col++) {
			colName[col] = table.getColumnName(col);
			PrimitiveArray column = table.getColumn(col);
			if(this.isKeyCol.get(col)) {
				dataType[col] = column.elementClassString();
			}else{
			    if(isDegreeUnitsColumn(table,col)) {
			    	degreesCol.set(col);
			    }
			    if(column.isFloatingPointType()) {
			    	this.wasDecimalCol.set(col);
			    }
				if(column.isIntegerType() || column.isFloatingPointType()) {
					dataType[col] = "double";
				}else if(col!=timeCol) {
					//include this in the output only if a single value
					dataType[col] = column.elementClass() == char.class ? 
						"char": column.elementClass() == byte.class ? "byte" : "String";
					cannotMeanCol.set(col);
				}
			}
		}
		this.meansTable = Table.makeEmptyTable(colName, dataType);
		return true;
	}

	
    /*
     * Sometimes it makes sense to change the column type back to integer,
     * For example year and month. Only do this if all the values are integers.
     */
    private void useIntegersWhereSensible() {
		int ncols = this.meansTable.nColumns();
		for(int col = 0; col < ncols; col++) {
			PrimitiveArray column = this.meansTable.getColumn(col);
			if(this.wasDecimalCol.get(col) || !column.isFloatingPointType()) {
				continue;
			}
			if(areAllValuesIntegers(column)) {
				int nrows = column.size();
				IntArray intArray = new IntArray();
				for(int row=0; row<nrows; row++) {
					intArray.add(column.getInt(row));
				}
				String name = this.meansTable.getColumnName(col);
				this.meansTable.removeColumn(col);
				this.meansTable.addColumn(col, name, intArray);
			}
		}

		
	}


	private boolean areAllValuesIntegers(PrimitiveArray column) {
		int nrows = column.size();
		for(int row=0; row<nrows; row++) {
			if(column.getDouble(row) != column.getInt(row)) {
				return false;
			}
		}
		return true;
	}


	private void accumulateDegrees(String key, double value) {
    	DegreesAccumulator accum = degreesMap.get(key);
    	if(accum == null) {
    		accum = new DegreesAccumulator();
    		degreesMap.put(key, accum);
    	}
    	accum.add(value);
	}


    
    private void calculateDegreeMeans() {
    	if(meansTable == null || degreesCol.isEmpty()) {
    		return;
    	}
    	int ncols = meansTable.nColumns();
    	for(int col=0; col<ncols;col++) {
    		if(this.degreesCol.get(col)) {
    	    	int nRows = meansTable.nRows();
    	        for(int row = 0; row < nRows; row++) {
        			String key = keymap.get(row)+":"+col;
        			if (this.degreesMap.containsKey(key)) {
        				meansTable.setDoubleData(col, row, degreesMap.get(key).getMean());
        			}
    	        }
    		}
    	}		
	}

    private class DegreesAccumulator{
    	double x = 0.0;
    	double y = 0.0;
    	int count = 0;
    	void add(double angleDegrees) {
    		double angleR = Math.toRadians(angleDegrees);
    		 x += Math.cos(angleR);
    		 y += Math.sin(angleR);
    		 count += 1;
    	}
    	public double getMean() {
            double avgR = Math.atan2(y / count, x / count);
            return Math.toDegrees(avgR);    		
    	}
    }
    
    /**
     * This finishes orderByMean and writes results to otherTableWriter
     * If ignoreFinish=true, nothing will be done.
     *
     * @throws Throwable if trouble (e.g., EDStatic.THERE_IS_NO_DATA if there is no data)
     */
    public void finish() throws Throwable {
        if (ignoreFinish) 
            return;

        if(this.keyCols != null) {
        	calculateDegreeMeans();
        	useIntegersWhereSensible();
            if(this.keyCols.length > 0) {
                boolean yes[] = new boolean[this.keyCols.length];
                for(int i=0;i<yes.length;i++) yes[i] = true;
                meansTable.sort(this.keyCols, yes);
            }
            super.writeSome(meansTable);
        }
        super.finish();

        Table cumulativeTable = cumulativeTable();
        releaseResources();
        otherTableWriter.writeAllAndFinish(cumulativeTable);

        //clean up
        meansTable = null;
        this.degreesMap.clear();
        this.counts.clear();
        this.rowmap.clear();
    }


	/**
     * If caller has the entire table, use this instead of repeated writeSome() + finish().
     * This overwrites the superclass method.
     *
     * @throws Throwable if trouble (e.g., EDStatic.THERE_IS_NO_DATA if there is no data)
     */
    public void writeAllAndFinish(Table tCumulativeTable) throws Throwable {
    	this.writeSome(tCumulativeTable);
    	this.finish();
    }

}



