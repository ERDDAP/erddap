

 package gov.noaa.pmel.util;

/**
* Base class used by LatitudeValue and LongitudeValue for translating between
* a float and deg/min/sec.
*/
  public class GeographicValue {

     protected int degrees;
     protected int minutes;
     protected int seconds;
     protected int sign;
     protected float decimalValue;
     
     // ---------------------------------------------
     //
     public GeographicValue( ) {
        this.decimalValue = 0;
        decimalToDegMinSec();
     }

     // ---------------------------------------------
     //
     public GeographicValue( float decVal ) {
        this.decimalValue = decVal;
        decimalToDegMinSec();
     }

     // ---------------------------------------------
     //
     public void decimalToDegMinSec() {
        sign = 1;
        if (decimalValue < 0) sign = -1;

        float num1 = Math.abs( decimalValue );
        degrees = (new Double( num1)).intValue();
        float f1 = Math.abs( num1 - degrees);
        float f2 = (f1 * 60);

        float num2 = Math.abs( f2 );
        minutes = new Double( Math.floor( (new Double(num2)).doubleValue() )).intValue();
        float f3 = Math.abs( num2 - minutes);
	double dd = f3 * 60;
        seconds = new Long( Math.round( dd )).intValue();
	if (seconds == 60) {
	   seconds = 0;
	   minutes++;
	}
	/*
	System.out.println("======================================================");
	System.out.println(" num1: " + num1 
		+ " \n degrees: " + degrees 
		+ " \n f1: " + f1
		+ " \n f2: " + f2
		+ " \n num2: " + num2
		+ " \n minutes: " + minutes
		+ " \n f3: " + f3
		+ " \n dd: " + dd
		+ " \n seconds: " + seconds);
	System.out.println("");
	*/
     }


     // ---------------------------------------------
     //
     public void degMinSecToDecimal() {
        float minVal = minutes;
        float degVal = degrees;
        float secVal = seconds;
	if (seconds == 0) secVal = 0.1f;
        minVal += (secVal/60f);
        degVal += (minVal/60f);
	degVal = degVal * sign;
 	decimalValue = degVal;
     }

     // ---------------------------------------------
     //
     public void setDegrees( int degrees ) {
        this.degrees = degrees;
     }
     // ---------------------------------------------
     //
     public void setMinutes( int minutes ) {
        this.minutes = minutes;
     }
     // ---------------------------------------------
     //
     public void setSeconds( int seconds ) {
        this.seconds = seconds;
     }
     // ---------------------------------------------
     //
     public void setSign( int sign ) {
        this.sign = sign;
     }
     // ---------------------------------------------
     //
     public void setDecimalValue( float decimalValue ) {
        this.decimalValue = decimalValue;
	decimalToDegMinSec();
     }

     // ---------------------------------------------
     //
     public int getDegrees( ) {
        return degrees;
     }
     // ---------------------------------------------
     //
     public int getMinutes( ) {
        return minutes;
     }
     // ---------------------------------------------
     //
     public int getSeconds( ) {
        return seconds;
     }
     // ---------------------------------------------
     //
     public int getSign( ) {
        return sign;
     }
     // ---------------------------------------------
     //
     public float getDecimalValue( ) {
        return decimalValue;
     }
     
     // ---------------------------------------------
     //
     public String toString( ) {
       StringBuffer str = new StringBuffer();
       str.append( " ----------------------------: " 
		+ " \n degrees: " + degrees 
		+ " \n minutes: " + minutes
		+ " \n seconds: " + seconds
		+ " \n decimalValue: " + decimalValue);
       return str.toString();
     }

     // ---------------------------------------------
     //
     public static void main( String args[] ) {
        GeographicValue l1 = new GeographicValue( -154.7002716064453f);
        System.out.println(" toString: " + l1.toString());
     }
  
  }
