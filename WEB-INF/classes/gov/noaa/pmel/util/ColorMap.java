package gov.noaa.pmel.util;

import java.util.*;
import java.awt.*;
import gov.noaa.pmel.sgt.Transform;

public class ColorMap {
	protected String mParamName;
	Vector mPieceRanges = new Vector();		// stores the physical ranges (Range2D) for each of the pieces of the colormap
	Vector mRedTransforms = new Vector();	// stores the transforms from physical to user coords for the red component
	Vector mGreenTransforms = new Vector();	// stores the transforms from physical to user coords for the green component
	Vector mBlueTransforms = new Vector();	// stores the transforms from physical to user coords for blue component
	
	public ColorMap() {
		// zero argument ctor
	}
	
	public ColorMap(ColorMap inMap) {
		// copy constructor
	
	}
	
	// public methods
	public Color getColor(double inVal) {
		// find the appropriate transform by testing what Range2D it's in
		int foundPiece = -99;
		int numPieces = mPieceRanges.size();
		
		// first test whether it's out of range
		double sVal =((Range2D)mPieceRanges.elementAt(0)).start;
		double eVal =((Range2D)mPieceRanges.elementAt(numPieces - 1)).end;
		if (inVal <= sVal) {
			float red = (float)(((Transform)mRedTransforms.elementAt(0)).getTransU(sVal));
			float green = (float)(((Transform)mGreenTransforms.elementAt(0)).getTransU(sVal));
			float blue = (float)(((Transform)mBlueTransforms.elementAt(0)).getTransU(sVal));
			return new Color(red, green, blue);
		}
		else if (inVal >= eVal) {
			float red = (float)(((Transform)mRedTransforms.elementAt(numPieces - 1)).getTransU(eVal));
			float green = (float)(((Transform)mGreenTransforms.elementAt(numPieces - 1)).getTransU(eVal));
			float blue = (float)(((Transform)mBlueTransforms.elementAt(numPieces - 1)).getTransU(eVal));
			System.out.println(red + " " + green + " " + blue);
			return new Color(red, green, blue);
		}
		
		// value is in range
		for (int i=0; i<mPieceRanges.size(); i++) {
			Range2D range = (Range2D)mPieceRanges.elementAt(i);
			sVal = range.start;
			eVal = range.end;
			if (inVal >= sVal && inVal <= eVal) {
				foundPiece = i;
				break;
			}
		}
		float red = (float)(((Transform)mRedTransforms.elementAt(foundPiece)).getTransU(eVal));
		float green = (float)(((Transform)mGreenTransforms.elementAt(foundPiece)).getTransU(eVal));
		float blue = (float)(((Transform)mBlueTransforms.elementAt(foundPiece)).getTransU(eVal));
		return new Color(red, green, blue);
	}
	
	public void addTransform(Transform inTransRed, Transform inTransGreen, Transform inTransBlue) {
		mRedTransforms.addElement(inTransRed);
		mGreenTransforms.addElement(inTransGreen);
		mBlueTransforms.addElement(inTransBlue);
		mPieceRanges.addElement(inTransRed.getRangeP());
	}
	
	public IndexedColorMap getIndexedColorMap(int numColors) {
		return new IndexedColorMap(this, numColors);
	}
	
	public String getParamName() {
		return mParamName;
	}
	
	public double getMaxValue() {
		return ((Range2D)mPieceRanges.elementAt(mPieceRanges.size()-1)).end;
	}
	
	public double getMinValue() {
		return ((Range2D)mPieceRanges.elementAt(0)).start;
	
	}
}