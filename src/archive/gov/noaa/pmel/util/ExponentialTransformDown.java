/*
 * $Id: ExponentialTransformDown.java,v 1.1 2002/12/04 01:27:38 oz Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */
 
package gov.noaa.pmel.util;

import java.beans.PropertyChangeListener;
import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.sgt.Transform;
 
/**
 * <code>ExponentialTransformUp</code> defines a exponential transformations between 
 * user and physical coordinates defined between two end points.
 *
 * @see AxisTransform
 *
 * @author Donald Denbo
 * @version $Revision: 1.1 $, $Date: 2002/12/04 01:27:38 $
 */
public class ExponentialTransformDown implements Transform {
	Range2D mPhysRange = null;
	Range2D mUserRange = null;
	//double a = 0.46487;
	//double b = 1.7563;
	//double c = -2.8931;
	//double d = 2.6001;
	
	double a = 0.82605;
	double b = -2.3006;
	double c = 2.4729;

	public ExponentialTransformDown(double p1, double p2, double u1, double u2) {
		setRangeP(p1, p2);
		setRangeU(u1, u2);
	}

	public ExponentialTransformDown(Range2D prange, Range2D urange) {
		setRangeP(prange);
		setRangeU(urange);
	}

	public ExponentialTransformDown() {
	}
	
	/**
	* Set physical coordinate range.
	*
	* @param p1 minimum value, physical coordinates
	* @param p2 maximum value, physical coordinates
	* @see LinearTransform
	**/
	public void setRangeP(double p1,double p2) {
		mPhysRange = new Range2D(p1, p2);
	}

	/**
	* Set physical coordinate range.
	*
	* @param prange physcial coordinate range
	* @see Range2D
	* @see LinearTransform
	**/
	public void setRangeP(Range2D prange) {
		mPhysRange = null;
		mPhysRange = new Range2D();
		mPhysRange.add(prange);
	}

	/**
	* Get the physical coordinate range.
	*
	* @return physcial coordinate range
	* @see Range2D
	**/
	public Range2D getRangeP() {
		return mPhysRange;
	}

	/**
	* Set the user coordinate range for double values.
	*
	* @param u1 minimum value, user coordinates
	* @param u2 maximum value, user coordinates
	* @see LinearTransform
	**/
	public void setRangeU(double u1,double u2) {
		mUserRange = new Range2D(u1, u2);
	}

	/**
	* Set the user coordinate range for double values.
	*
	* @param urange user coordinate range
	* @see Range2D
	* @see LinearTransform
	**/
	public void setRangeU(Range2D urange) {
		mUserRange = null;
		mUserRange = new Range2D();
		mUserRange.add(urange);
	}

	/**
	* Get the user coordinate range for double values.
	*
	* @return user range
	* @see Range2D
	**/
	public Range2D getRangeU() {
		return mUserRange;
	}

	/**
	* Transform from user to physical coordinates.
	*
	* @param u user value
	* @return physical value
	*/
	public double getTransP(double u) {
		double retVal = 0.0;
		// first have to find u in the normalized range of the y axis
		double y = (u - mUserRange.start)/(mUserRange.end - mUserRange.start);
		
		// now iterate to find solution
		double x = 0.5;
		double inc = 0.0001;
		double eps = 0.001;
		int cnt = 0;
		while (true) {
			//double tstVal = d * x + c * (x * x) + b * (x * x * x) + a * (x * x * x * x);
			double tstVal = c * x + b * (x * x) + a * (x * x * x);
			if (Math.abs(tstVal - y) < eps) {
				retVal = x;
				break;
			}
			else {
				if (tstVal > y)
					x -= inc;
				else
					x += inc;
			}
			cnt++;
		}
		//System.out.println("iter count = " + cnt);
		
		// scale the x value back to the unnormalized range
		retVal = mPhysRange.start + x * (mPhysRange.end - mPhysRange.start);
		return retVal;
	}

	/**
	* Transform from physical to user coordinates.
	*
	* @param p physical value
	* @return user value
	*/
	public double getTransU(double p) {
		double retVal = 0.0;
		double x = (p - mPhysRange.start)/(mPhysRange.end - mPhysRange.start);
		//double y = d * x + c * (x * x) + b * (x * x * x) + a * (x * x * x * x);
		double y = c * x + b * (x * x) + a * (x * x * x);
		retVal = mUserRange.start + y * (mUserRange.end - mUserRange.start);
		return retVal;
	}

	/**
	* Add listener for changes to transform properties.
	*/
	public void addPropertyChangeListener(PropertyChangeListener listener) {

	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {

	}

    public void releaseResources() throws Exception { //Kyle and Bob added
      mPhysRange = null;
	  mUserRange = null;
    }
}

