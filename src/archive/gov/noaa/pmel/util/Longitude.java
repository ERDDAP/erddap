
 package gov.noaa.pmel.util;

/**
* LongitudeValue translates between float and deg/min/sec.
*/
  public class Longitude extends GeographicValue {

     
     // ---------------------------------------------
     //
     public Longitude( ) {
	super();
     }

     // ---------------------------------------------
     //
     public Longitude( float longitude ) {
        super( longitude );
     }

     // ---------------------------------------------
     //
     public static void main( String args[] ) {
        Longitude l1 = new Longitude( -154.7002716064453f);
        System.out.println(" toString: " + l1.toString());
     }
  
  }
