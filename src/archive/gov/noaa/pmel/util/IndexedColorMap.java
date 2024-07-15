package gov.noaa.pmel.util;

import java.util.*;
import java.awt.*;


public class IndexedColorMap {
	String mParamName;
	double mMinVal;
	double mMaxVal;
	double[] mVals = null;
	Color[] mColors = null;
	int mNumColors;
	ColorMap mColorMap;
	
	public IndexedColorMap(ColorMap inCM, int numColors) {
		mNumColors = numColors;
		mColorMap = inCM;
		mVals = new double[mNumColors];
		mColors = new Color[mNumColors];
		
		// initialize the indexed colormap
		mMinVal = mColorMap.getMinValue();
		mMaxVal = mColorMap.getMaxValue();
		mParamName = new String(mColorMap.getParamName());
		
		double delta = (mMaxVal - mMinVal)/(double)mNumColors;
		
		for (int i=0; i<mNumColors; i++) {
			mVals[i] = mMinVal + (double)i * delta;
		}
		
		mVals[mNumColors-1] = mMaxVal;
		
		for (int i=0; i<mNumColors; i++) {
			mColors[i] = mColorMap.getColor(mVals[i]);
		}
	}
	
	public IndexedColorMap(IndexedColorMap inMap) {
		// copy constructor
	}
	
	// public methods
	public Color getColor(double inVal) {
		return new Color(1.0f, 1.0f, 1.0f);
	}
	
	public Color getColor(int index) {
		if (index < 0)
			return mColors[0];
		else if (index >= mNumColors)
			return mColors[mNumColors-1];
		else
			return mColors[index];
	}
	
	public Color[] getColors() {
		return mColors;
	}
	
	public String getParamName() {
		return mParamName;
	}
}