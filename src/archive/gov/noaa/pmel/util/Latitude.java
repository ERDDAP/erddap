
 package gov.noaa.pmel.util;

/**
* LatitudeValue translates between float and deg/min/sec.
*/
  public class Latitude extends GeographicValue {

     // ---------------------------------------------
     //
     public Latitude( ) {
        super();
     }

     // ---------------------------------------------
     //
     public Latitude( float latitude ) {
        super( latitude );
     }

     // ---------------------------------------------
     //
     public static void main( String args[] ) {
        Latitude l1 = new Latitude( -54.7002716064453f);
        System.out.println(" toString: " + l1.toString());
     }
  
  }
