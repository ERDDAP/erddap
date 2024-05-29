package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;

class DoubleCenterGridsTests {
  /**
   * This tests main() on Bob's Windows computer.
   *
   * @throws Exception
   */
  @org.junit.jupiter.api.Test
  void basicTest() throws Exception {

    // delete old files
    String newDir = DoubleCenterGridsTests.class.getResource("/data/centeredSatellite/").getPath() + "AG";
    RegexFilenameFilter.recursiveDelete(newDir);
    String fullName = newDir + "/ssta/1day/AG2005040_2005040_ssta.nc";

    // copy a dir and subdirs and files
    DoubleCenterGrids.main(
        new String[] { DoubleCenterGridsTests.class.getResource("/data/centeredSatellite/AGsource").getPath(),
            newDir });

    // do tests
    String ncdump = NcHelper.ncdump(fullName, "-h");
    // ensure time is centered correctly
    Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(1.1079504E9), // # from time actual_range
        "2005-02-09T12:00:00Z", "");
    String reference = "netcdf AG2005040_2005040_ssta.nc {\n" +
        "  dimensions:\n" +
        "    time = 1;\n" + // (has coord.var)\n" + //changed when switched to netcdf-java 4.0, 2009-02-23
        "    altitude = 1;\n" + // (has coord.var)\n" +
        "    lat = 91;\n" + // (has coord.var)\n" +
        "    lon = 91;\n" + // (has coord.var)\n" +
        "  variables:\n" +
        "    double time(time=1);\n" +
        "      :actual_range = 1.1079504E9, 1.1079504E9; // double\n" +
        "      :fraction_digits = 0; // int\n" +
        "      :long_name = \"Centered Time\";\n" +
        "      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
        "      :standard_name = \"time\";\n" +
        "      :axis = \"T\";\n" +
        "      :_CoordinateAxisType = \"Time\";\n" +
        "\n" +
        "    double altitude(altitude=1);\n" +
        "      :actual_range = 0.0, 0.0; // double\n" +
        "      :fraction_digits = 0; // int\n" +
        "      :long_name = \"Altitude\";\n" +
        "      :positive = \"up\";\n" +
        "      :standard_name = \"altitude\";\n" +
        "      :units = \"m\";\n" +
        "      :axis = \"Z\";\n" +
        "      :_CoordinateAxisType = \"Height\";\n" +
        "      :_CoordinateZisPositive = \"up\";\n" +
        "\n" +
        "    double lat(lat=91);\n" +
        "      :_CoordinateAxisType = \"Lat\";\n" +
        "      :actual_range = 33.5, 42.5; // double\n" +
        "      :axis = \"Y\";\n" +
        "      :coordsys = \"geographic\";\n" +
        "      :fraction_digits = 1; // int\n" +
        "      :long_name = \"Latitude\";\n" +
        "      :point_spacing = \"even\";\n" +
        "      :standard_name = \"latitude\";\n" +
        "      :units = \"degrees_north\";\n" +
        "\n" +
        "    double lon(lon=91);\n" +
        "      :_CoordinateAxisType = \"Lon\";\n" +
        "      :actual_range = 230.5, 239.5; // double\n" +
        "      :axis = \"X\";\n" +
        "      :coordsys = \"geographic\";\n" +
        "      :fraction_digits = 1; // int\n" +
        "      :long_name = \"Longitude\";\n" +
        "      :point_spacing = \"even\";\n" +
        "      :standard_name = \"longitude\";\n" +
        "      :units = \"degrees_east\";\n" +
        "\n" +
        "    float AGssta(time=1, altitude=1, lat=91, lon=91);\n" +
        "      :_FillValue = -9999999.0f; // float\n" +
        "      :actual_range = 9.7f, 14.5f; // float\n" +
        "      :coordsys = \"geographic\";\n" +
        "      :fraction_digits = 1; // int\n" +
        "      :long_name = \"SST, NOAA POES AVHRR, GAC, 0.1 degrees, Global, Day and Night\";\n" +
        "      :missing_value = -9999999.0f; // float\n" +
        "      :numberOfObservations = 591; // int\n" +
        "      :percentCoverage = 0.07136819224731313; // double\n" +
        "      :standard_name = \"sea_surface_temperature\";\n" +
        "      :units = \"degree_C\";\n" +
        "\n" +
        "  // global attributes:\n" +
        "  :acknowledgement = \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
        "  :cdm_data_type = \"Grid\";\n" +
        "  :cols = 91; // int\n" +
        "  :composite = \"true\";\n" +
        "  :contributor_name = \"NOAA NESDIS OSDPD\";\n" +
        "  :contributor_role = \"Source of level 2 data.\";\n" +
        "  :Conventions = \"COARDS, CF-1.6, ACDD-1.3, CWHDF\";\n" +
        "  :creator_email = \"erd.data@noaa.gov\";\n" +
        "  :creator_name = \"NOAA CoastWatch, West Coast Node\";\n" +
        "  :creator_url = \"https://coastwatch.pfeg.noaa.gov\";\n" +
        "  :cwhdf_version = \"3.4\";\n";

    // " :date_created = \"2007-01-23Z\";\n" +
    // " :date_issued = \"2007-01-23Z\";\n" +
    String reference2 = "  :Easternmost_Easting = 239.5; // double\n" +
        "  :et_affine = 0.0, 0.1, 0.1, 0.0, 230.5, 33.5; // double\n" +
        "  :gctp_datum = 12; // int\n" +
        "  :gctp_parm = 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0; // double\n" +
        "  :gctp_sys = 0; // int\n" +
        "  :gctp_zone = 0; // int\n" +
        "  :geospatial_lat_max = 42.5; // double\n" +
        "  :geospatial_lat_min = 33.5; // double\n" +
        "  :geospatial_lat_resolution = 0.1; // double\n" +
        "  :geospatial_lat_units = \"degrees_north\";\n" +
        "  :geospatial_lon_max = 239.5; // double\n" +
        "  :geospatial_lon_min = 230.5; // double\n" +
        "  :geospatial_lon_resolution = 0.1; // double\n" +
        "  :geospatial_lon_units = \"degrees_east\";\n" +
        "  :geospatial_vertical_max = 0.0; // double\n" +
        "  :geospatial_vertical_min = 0.0; // double\n" +
        "  :geospatial_vertical_positive = \"up\";\n" +
        "  :geospatial_vertical_units = \"m\";\n" +
        "  :history = \"NOAA NESDIS OSDPD\n" +
        "20"; // start of a date

    // "2007-01-23T19:30:17Z NOAA CoastWatch, West Coast Node\";\n" +
    String reference3 = "  :id = \"LAGsstaS1day\";\n" +
        "  :institution = \"NOAA CoastWatch, West Coast Node\";\n" +
        "  :keywords = \"EARTH SCIENCE > Oceans > Ocean Temperature > Sea Surface Temperature\";\n" +
        "  :keywords_vocabulary = \"GCMD Science Keywords\";\n" +
        "  :license = \"The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.\";\n"
        +
        "  :naming_authority = \"gov.noaa.pfeg.coastwatch\";\n" +
        "  :Northernmost_Northing = 42.5; // double\n" +
        "  :origin = \"NOAA NESDIS OSDPD\";\n" +
        "  :pass_date = 12823; // int\n" +
        "  :polygon_latitude = 33.5, 42.5, 42.5, 33.5, 33.5; // double\n" +
        "  :polygon_longitude = 230.5, 230.5, 239.5, 239.5, 230.5; // double\n" +
        "  :processing_level = \"3 (projected)\";\n" +
        "  :project = \"CoastWatch (https://coastwatch.noaa.gov/)\";\n" +
        "  :projection = \"geographic\";\n" +
        "  :projection_type = \"mapped\";\n" +
        "  :references = \"NOAA POES satellites information: https://www.oso.noaa.gov/poes/index.htm . Processing link: https://www.ospo.noaa.gov/PSB/PSB.html . Processing reference: Walton C. C., W. G. Pichel, J. F. Sapper, D. A. May. The development and operational application of nonlinear algorithms for the measurement of sea surface temperatures with the NOAA polar-orbiting environmental satellites. J.G.R., 103: (C12) 27999-28012, 1998. Cloudmask reference: Stowe, L. L., P. A. Davis, and E. P. McClain.  Scientific basis and initial evaluation of the CLAVR-1 global clear/cloud classification algorithm for the advanced very high resolution radiometer.  J. Atmos. Oceanic Technol., 16, 656-681. 1999. Calibration and Validation: Li, X., W. Pichel, E. Maturi, P. Clemente-Colon, and J. Sapper. Deriving the operational nonlinear multi-channel sea surface temperature algorithm coefficients for NOAA-15 AVHRR/3. International Journal of Remote Sensing, Volume 22, No. 4, 699 - 704, March 2001a. Calibration and Validation: Li, X, W. Pichel, P. Clemente-Colon, V. Krasnopolsky, and J. Sapper. Validation of coastal sea and lake surface temperature measurements derived from NOAA/AVHRR Data. International Journal of Remote Sensing, Vol. 22, No. 7, 1285-1303, 2001b.\";\n"
        +
        "  :rows = 91; // int\n" +
        "  :satellite = \"POES\";\n" +
        "  :sensor = \"AVHRR GAC\";\n" +
        "  :source = \"satellite observation: POES, AVHRR GAC\";\n" +
        "  :Southernmost_Northing = 33.5; // double\n" +
        "  :standard_name_vocabulary = \"CF Standard Name Table v70\";\n" +
        "  :start_time = 0.0; // double\n" +
        "  :summary = \"NOAA CoastWatch provides sea surface temperature (SST) products derived from NOAA's Polar Operational Environmental Satellites (POES).  This data provides global area coverage at 0.1 degrees resolution.  Measurements are gathered by the Advanced Very High Resolution Radiometer (AVHRR) instrument, a multiband radiance sensor carried aboard the NOAA POES satellites.\";\n"
        +
        "  :time_coverage_end = \"2005-02-10T00:00:00Z\";\n" +
        "  :time_coverage_start = \"2005-02-09T00:00:00Z\";\n" +
        "  :title = \"SST, NOAA POES AVHRR, GAC, 0.1 degrees, Global, Day and Night\";\n" +
        "  :Westernmost_Easting = 230.5; // double\n" +
        "}\n";
    // String2.log(ncdump);
    Test.ensureEqual(ncdump.substring(0, reference.length()), reference,
        "ncdump=" + ncdump + "\nreference=" + reference);
    int po = ncdump.indexOf("  :Easternmost_Easting");
    Test.ensureEqual(ncdump.substring(po, po + reference2.length()), reference2, "ncdump=" + ncdump);
    po = ncdump.indexOf("  :id = \"LAGsstaS1day");
    Test.ensureEqual(ncdump.substring(po), reference3, "ncdump=" + ncdump);

    // just ensure the other file exists
    Test.ensureTrue(File2.isFile(
        DoubleCenterGridsTests.class.getResource("/data/centeredSatellite/AG/ssta/1day/AG2005041_2005041_ssta.nc")
            .getPath()),
        "");

    // delete the log file

    // File2.delete("c:/programs/_tomcat/webapps/cwexperimental/WEB-INF/DoubleCenterGrids.log");
  }

}
