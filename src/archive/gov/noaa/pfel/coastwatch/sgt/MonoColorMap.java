/* 
 * MonoColorMap Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
//This class is loosely based on gov.noaa.pmel.util.ColorMap.
package gov.noaa.pfel.coastwatch.sgt;

import com.cohort.util.String2;

import gov.noaa.pmel.sgt.ColorMap;
import gov.noaa.pmel.util.Range2D;
import java.awt.Color;

/**
 * This class just returns one color for any value.
 * This is useful for the landMask, which has just 0's and NaN's.
 */
public class MonoColorMap extends ColorMap {

    Color color;
    Range2D range = new Range2D(-Double.MAX_VALUE, Double.MAX_VALUE, 1); //not Double.MIN_VALUE which ~= 0
    public Color NaNColor = Color.GRAY;

    /**
     * The constructor.
     * 
     * @param theColor
     */
    public MonoColorMap(Color theColor) {
        color = theColor;
    }

    /**
     * This returns a shallow copy of this colormap.
     */
    public ColorMap copy() {
        return new MonoColorMap(color);
    }
    
    /**
     * This crudely implements equals.
     * returns false
     */
    public boolean equals(ColorMap colorMap) {
        return false;
    }
    
    /**
     * This returns the range of values covered by this colorMap.  
     * @return the range
     */
    public Range2D getRange() {
        return range;
    }
    
    /**
     * This always returns the stored color.
     * 
     * @param inVal the incoming value
     * @return NaN returns NaNColor; everything else returns theColor.
     */
    public Color getColor(double inVal) {
        return Double.isNaN(inVal)? NaNColor : color;
    }
    

}