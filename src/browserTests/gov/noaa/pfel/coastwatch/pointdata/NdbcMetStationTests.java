package gov.noaa.pfel.coastwatch.pointdata;

import java.nio.file.Path;

import com.cohort.util.Calendar2;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import tags.TagLargeFiles;


class NdbcMetStationTests {
  /**
   * This reads the Historical 46088.nc file and makes sure it has the right info.
   */
  void testHistorical46088Nc(String ndbcHistoricalNcDir) throws Exception {
    // String2.log("\n*** testHistorical46088Nc");

    String results = NcHelper.ncdump(ndbcHistoricalNcDir + "NDBC_46088_met.nc", "-h");
    results = results.replaceFirst("TIME = \\d+;", "TIME = ......;");
    results = results.replaceFirst(":actual_range = 1.0887192E9, [\\d\\.]{7,9}E9",
        ":actual_range = 1.0887192E9, .........E9");
    results = results.replaceFirst(":date_created = \"20..-..-..\";", ":date_created = \"20..-..-..\";");
    results = results.replaceFirst(":date_issued = \"20..-..-..\";", ":date_issued = \"20..-..-..\";");
    results = results.replaceFirst(":time_coverage_end = \"20..-..-..T..:..:..Z\";",
        ":time_coverage_end = \"20..-..-..T..:..:..Z\";");

    String expected = "netcdf NDBC_46088_met.nc {\n" +
        "  dimensions:\n" +
        "    LON = 1;\n" +
        "    LAT = 1;\n" +
        "    DEPTH = 1;\n" +
        "    TIME = ......;\n" + // changes
        "    ID_strlen = 5;\n" +
        "  variables:\n" +
        "    float LON(LON=1);\n" +
        "      :_CoordinateAxisType = \"Lon\";\n" +
        "      :actual_range = -123.167f, -123.167f; // float\n" +
        "      :axis = \"X\";\n" +
        "      :colorBarMaximum = 180.0; // double\n" +
        "      :colorBarMinimum = -180.0; // double\n" +
        "      :comment = \"The longitude of the station.\";\n" +
        "      :ioos_category = \"Location\";\n" +
        "      :long_name = \"Longitude\";\n" +
        "      :standard_name = \"longitude\";\n" +
        "      :units = \"degrees_east\";\n" +
        "\n" +
        "    float LAT(LAT=1);\n" +
        "      :_CoordinateAxisType = \"Lat\";\n" +
        "      :actual_range = 48.333f, 48.333f; // float\n" +
        "      :axis = \"Y\";\n" +
        "      :colorBarMaximum = 90.0; // double\n" +
        "      :colorBarMinimum = -90.0; // double\n" +
        "      :comment = \"The latitude of the station.\";\n" +
        "      :ioos_category = \"Location\";\n" +
        "      :long_name = \"Latitude\";\n" +
        "      :standard_name = \"latitude\";\n" +
        "      :units = \"degrees_north\";\n" +
        "\n" +
        "    float DEPTH(DEPTH=1);\n" +
        "      :_CoordinateAxisType = \"Height\";\n" +
        "      :_CoordinateZisPositive = \"down\";\n" +
        "      :actual_range = 0.0f, 0.0f; // float\n" +
        "      :axis = \"Z\";\n" +
        "      :colorBarMaximum = 0.0; // double\n" +
        "      :colorBarMinimum = 0.0; // double\n" +
        "      :comment = \"The depth of the station, nominally 0 (see station information for details).\";\n" +
        "      :ioos_category = \"Location\";\n" +
        "      :long_name = \"Depth\";\n" +
        "      :positive = \"down\";\n" +
        "      :standard_name = \"depth\";\n" +
        "      :units = \"m\";\n" +
        "\n" +
        "    double TIME(TIME=313441);\n" + // changes here and several places below
        "      :_CoordinateAxisType = \"Time\";\n" +
        "      :actual_range = 1.0887192E9, .........E9; // double\n" + // changes
        "      :axis = \"T\";\n" +
        "      :ioos_category = \"Time\";\n" +
        "      :long_name = \"Time\";\n" +
        "      :standard_name = \"time\";\n" +
        "      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
        "      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
        "\n" +
        "    short WD(TIME=313441, DEPTH=1, LAT=1, LON=1);\n" +
        "      :_FillValue = 32767S; // short\n" +
        "      :actual_range = 0S, 359S; // short\n" +
        "      :colorBarMaximum = 360.0; // double\n" +
        "      :colorBarMinimum = 0.0; // double\n" +
        "      :comment = \"Wind direction (the direction the wind is coming from in degrees clockwise from true N) during the same period used for WSPD.\";\n"
        +
        "      :ioos_category = \"Wind\";\n" +
        "      :long_name = \"Wind Direction\";\n" +
        "      :missing_value = 32767S; // short\n" +
        "      :standard_name = \"wind_from_direction\";\n" +
        "      :units = \"degrees_true\";\n" +
        "\n" +
        "    float WSPD(TIME=313441, DEPTH=1, LAT=1, LON=1);\n" +
        "      :_FillValue = -9999999.0f; // float\n" +
        "      :actual_range = 0.0f, 21.8f; // float\n" +
        "      :colorBarMaximum = 15.0; // double\n" +
        "      :colorBarMinimum = 0.0; // double\n" +
        "      :comment = \"Average wind speed (m/s).\";\n" +
        "      :ioos_category = \"Wind\";\n" +
        "      :long_name = \"Wind Speed\";\n" +
        "      :missing_value = -9999999.0f; // float\n" +
        "      :standard_name = \"wind_speed\";\n" +
        "      :units = \"m s-1\";\n" +
        "\n" +
        "    float GST(TIME=313441, DEPTH=1, LAT=1, LON=1);\n" +
        "      :_FillValue = -9999999.0f; // float\n" +
        "      :actual_range = 0.0f, 28.2f; // float\n" +
        "      :colorBarMaximum = 30.0; // double\n" +
        "      :colorBarMinimum = 0.0; // double\n" +
        "      :comment = \"Peak 5 or 8 second gust speed (m/s).\";\n" +
        "      :ioos_category = \"Wind\";\n" +
        "      :long_name = \"Wind Gust Speed\";\n" +
        "      :missing_value = -9999999.0f; // float\n" +
        "      :standard_name = \"wind_speed_of_gust\";\n" +
        "      :units = \"m s-1\";\n" +
        "\n" +
        "    float WVHT(TIME=313441, DEPTH=1, LAT=1, LON=1);\n" +
        "      :_FillValue = -9999999.0f; // float\n" +
        "      :actual_range = 0.0f, 3.73f; // float\n" +
        "      :colorBarMaximum = 10.0; // double\n" +
        "      :colorBarMinimum = 0.0; // double\n" +
        "      :comment = \"Significant wave height (meters) is calculated as the average of the highest one-third of all of the wave heights during the 20-minute sampling period.\";\n"
        +
        "      :ioos_category = \"Surface Waves\";\n" +
        "      :long_name = \"Wave Height\";\n" +
        "      :missing_value = -9999999.0f; // float\n" +
        "      :standard_name = \"sea_surface_wave_significant_height\";\n" +
        "      :units = \"m\";\n" +
        "\n" +
        "    float DPD(TIME=313441, DEPTH=1, LAT=1, LON=1);\n" +
        "      :_FillValue = -9999999.0f; // float\n" +
        "      :actual_range = 0.0f, 26.67f; // float\n" +
        "      :colorBarMaximum = 20.0; // double\n" +
        "      :colorBarMinimum = 0.0; // double\n" +
        "      :comment = \"Dominant wave period (seconds) is the period with the maximum wave energy.\";\n" +
        "      :ioos_category = \"Surface Waves\";\n" +
        "      :long_name = \"Wave Period, Dominant\";\n" +
        "      :missing_value = -9999999.0f; // float\n" +
        "      :standard_name = \"sea_surface_swell_wave_period\";\n" +
        "      :units = \"s\";\n" +
        "\n" +
        "    float APD(TIME=313441, DEPTH=1, LAT=1, LON=1);\n" +
        "      :_FillValue = -9999999.0f; // float\n" +
        "      :actual_range = 0.0f, 12.02f; // float\n" +
        "      :colorBarMaximum = 20.0; // double\n" +
        "      :colorBarMinimum = 0.0; // double\n" +
        "      :comment = \"Average wave period (seconds) of all waves during the 20-minute period.\";\n" +
        "      :ioos_category = \"Surface Waves\";\n" +
        "      :long_name = \"Wave Period, Average\";\n" +
        "      :missing_value = -9999999.0f; // float\n" +
        "      :standard_name = \"sea_surface_swell_wave_period\";\n" +
        "      :units = \"s\";\n" +
        "\n" +
        "    short MWD(TIME=313441, DEPTH=1, LAT=1, LON=1);\n" +
        "      :_FillValue = 32767S; // short\n" +
        "      :actual_range = 0S, 359S; // short\n" +
        "      :colorBarMaximum = 360.0; // double\n" +
        "      :colorBarMinimum = 0.0; // double\n" +
        "      :comment = \"Mean wave direction corresponding to energy of the dominant period (DOMPD).\";\n" +
        "      :ioos_category = \"Surface Waves\";\n" +
        "      :long_name = \"Wave Direction\";\n" +
        "      :missing_value = 32767S; // short\n" +
        "      :standard_name = \"sea_surface_wave_to_direction\";\n" +
        "      :units = \"degrees_true\";\n" +
        "\n" +
        "    float BAR(TIME=313441, DEPTH=1, LAT=1, LON=1);\n" +
        "      :_FillValue = -9999999.0f; // float\n" +
        "      :actual_range = 969.3f, 1045.9f; // float\n" +
        "      :colorBarMaximum = 1050.0; // double\n" +
        "      :colorBarMinimum = 950.0; // double\n" +
        "      :comment = \"Air pressure (hPa). ('PRES' on some NDBC tables.) For C-MAN sites and Great Lakes buoys, the recorded pressure is reduced to sea level using the method described in NWS Technical Procedures Bulletin 291 (11/14/80).\";\n"
        +
        "      :ioos_category = \"Pressure\";\n" +
        "      :long_name = \"Air Pressure\";\n" +
        "      :missing_value = -9999999.0f; // float\n" +
        "      :standard_name = \"air_pressure_at_sea_level\";\n" +
        "      :units = \"hPa\";\n" +
        "\n" +
        "    float ATMP(TIME=313441, DEPTH=1, LAT=1, LON=1);\n" +
        "      :_FillValue = -9999999.0f; // float\n" +
        "      :actual_range = -5.4f, 24.4f; // float\n" +
        "      :colorBarMaximum = 40.0; // double\n" +
        "      :colorBarMinimum = -10.0; // double\n" +
        "      :comment = \"Air temperature (Celsius). For sensor heights on buoys, see Hull Descriptions. For sensor heights at C-MAN stations, see C-MAN Sensor Locations.\";\n"
        +
        "      :ioos_category = \"Temperature\";\n" +
        "      :long_name = \"Air Temperature\";\n" +
        "      :missing_value = -9999999.0f; // float\n" +
        "      :standard_name = \"air_temperature\";\n" +
        "      :units = \"degree_C\";\n" +
        "\n" +
        "    float WTMP(TIME=313441, DEPTH=1, LAT=1, LON=1);\n" +
        "      :_FillValue = -9999999.0f; // float\n" +
        "      :actual_range = 4.9f, 21.1f; // float\n" +
        "      :colorBarMaximum = 32.0; // double\n" +
        "      :colorBarMinimum = 0.0; // double\n" +
        "      :comment = \"Sea surface temperature (Celsius). For sensor depth, see Hull Description.\";\n" +
        "      :ioos_category = \"Temperature\";\n" +
        "      :long_name = \"SST\";\n" +
        "      :missing_value = -9999999.0f; // float\n" +
        "      :standard_name = \"sea_surface_temperature\";\n" +
        "      :units = \"degree_C\";\n" +
        "\n" +
        "    float DEWP(TIME=313441, DEPTH=1, LAT=1, LON=1);\n" +
        "      :_FillValue = -9999999.0f; // float\n" +
        "      :actual_range = -16.4f, 17.2f; // float\n" +
        "      :colorBarMaximum = 40.0; // double\n" +
        "      :colorBarMinimum = 0.0; // double\n" +
        "      :comment = \"Dewpoint temperature taken at the same height as the air temperature measurement.\";\n" +
        "      :ioos_category = \"Temperature\";\n" +
        "      :long_name = \"Dewpoint Temperature\";\n" +
        "      :missing_value = -9999999.0f; // float\n" +
        "      :standard_name = \"dew_point_temperature\";\n" +
        "      :units = \"degree_C\";\n" +
        "\n" +
        "    float VIS(TIME=313441, DEPTH=1, LAT=1, LON=1);\n" +
        "      :_FillValue = -9999999.0f; // float\n" +
        "      :colorBarMaximum = 100.0; // double\n" +
        "      :colorBarMinimum = 0.0; // double\n" +
        "      :comment = \"Station visibility (km, originally nautical miles in the NDBC .txt files). Note that buoy stations are limited to reports from 0 to 1.6 nmi.\";\n"
        +
        "      :ioos_category = \"Meteorology\";\n" +
        "      :long_name = \"Station Visibility\";\n" +
        "      :missing_value = -9999999.0f; // float\n" +
        "      :standard_name = \"visibility_in_air\";\n" +
        "      :units = \"km\";\n" +
        "\n" +
        "    float PTDY(TIME=313441, DEPTH=1, LAT=1, LON=1);\n" +
        "      :_FillValue = -9999999.0f; // float\n" +
        "      :colorBarMaximum = 3.0; // double\n" +
        "      :colorBarMinimum = -3.0; // double\n" +
        "      :comment = \"Pressure Tendency is the direction (plus or minus) and the amount of pressure change (hPa) for a three hour period ending at the time of observation.\";\n"
        +
        "      :ioos_category = \"Pressure\";\n" +
        "      :long_name = \"Pressure Tendency\";\n" +
        "      :missing_value = -9999999.0f; // float\n" +
        "      :standard_name = \"tendency_of_air_pressure\";\n" +
        "      :units = \"hPa\";\n" +
        "\n" +
        "    float TIDE(TIME=313441, DEPTH=1, LAT=1, LON=1);\n" +
        "      :_FillValue = -9999999.0f; // float\n" +
        "      :colorBarMaximum = 5.0; // double\n" +
        "      :colorBarMinimum = -5.0; // double\n" +
        "      :comment = \"The water level in meters (originally feet in the NDBC .txt files) above or below Mean Lower Low Water (MLLW).\";\n"
        +
        "      :ioos_category = \"Sea Level\";\n" +
        "      :long_name = \"Water Level\";\n" +
        "      :missing_value = -9999999.0f; // float\n" +
        "      :standard_name = \"surface_altitude\";\n" +
        "      :units = \"m\";\n" +
        "\n" +
        "    float WSPU(TIME=313441, DEPTH=1, LAT=1, LON=1);\n" +
        "      :_FillValue = -9999999.0f; // float\n" +
        "      :actual_range = -17.4f, 20.8f; // float\n" +
        "      :colorBarMaximum = 15.0; // double\n" +
        "      :colorBarMinimum = -15.0; // double\n" +
        "      :comment = \"The zonal wind speed (m/s) indicates the u component of where the wind is going, derived from Wind Direction and Wind Speed.\";\n"
        +
        "      :ioos_category = \"Wind\";\n" +
        "      :long_name = \"Wind Speed, Zonal\";\n" +
        "      :missing_value = -9999999.0f; // float\n" +
        "      :standard_name = \"eastward_wind\";\n" +
        "      :units = \"m s-1\";\n" +
        "\n" +
        "    float WSPV(TIME=313441, DEPTH=1, LAT=1, LON=1);\n" +
        "      :_FillValue = -9999999.0f; // float\n" +
        "      :actual_range = -17.5f, 16.0f; // float\n" +
        "      :colorBarMaximum = 15.0; // double\n" +
        "      :colorBarMinimum = -15.0; // double\n" +
        "      :comment = \"The meridional wind speed (m/s) indicates the v component of where the wind is going, derived from Wind Direction and Wind Speed.\";\n"
        +
        "      :ioos_category = \"Wind\";\n" +
        "      :long_name = \"Wind Speed, Meridional\";\n" +
        "      :missing_value = -9999999.0f; // float\n" +
        "      :standard_name = \"northward_wind\";\n" +
        "      :units = \"m s-1\";\n" +
        "\n" +
        "    char ID(ID_strlen=5);\n" +
        "      :_Encoding = \"ISO-8859-1\";\n" +
        "      :cf_role = \"timeseries_id\";\n" +
        "      :comment = \"The station identifier.\";\n" +
        "      :ioos_category = \"Identifier\";\n" +
        "      :long_name = \"Station Identifier\";\n" +
        "\n" +
        "  // global attributes:\n" +
        "  :cdm_data_type = \"TimeSeries\";\n" +
        "  :cdm_timeseries_variables = \"ID, LON, LAT, DEPTH\";\n" +
        "  :contributor_name = \"NOAA NDBC\";\n" +
        "  :contributor_role = \"Source of data.\";\n" +
        "  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
        "  :creator_email = \"erd.data@noaa.gov\";\n" +
        "  :creator_name = \"NOAA NMFS SWFSC ERD\";\n" +
        "  :creator_type = \"institution\";\n" +
        "  :creator_url = \"https://www.pfeg.noaa.gov\";\n" +
        "  :date_created = \"20..-..-..\";\n" + // changes
        "  :date_issued = \"20..-..-..\";\n" + // changes
        "  :Easternmost_Easting = -123.167f; // float\n" +
        "  :geospatial_lat_max = 48.333f; // float\n" +
        "  :geospatial_lat_min = 48.333f; // float\n" +
        "  :geospatial_lat_units = \"degrees_north\";\n" +
        "  :geospatial_lon_max = -123.167f; // float\n" +
        "  :geospatial_lon_min = -123.167f; // float\n" +
        "  :geospatial_lon_units = \"degrees_east\";\n" +
        "  :geospatial_vertical_max = 0.0f; // float\n" +
        "  :geospatial_vertical_min = 0.0f; // float\n" +
        "  :geospatial_vertical_positive = \"down\";\n" +
        "  :geospatial_vertical_units = \"m\";\n" +
        "  :history = \"Around the 25th of each month, erd.data@noaa.gov downloads the latest yearly and monthly historical .txt.gz files from https://www.ndbc.noaa.gov/data/historical/stdmet/ and generates one historical .nc file for each station. erd.data@noaa.gov also downloads all of the 45day near real time .txt files from https://www.ndbc.noaa.gov/data/realtime2/ and generates one near real time .nc file for each station.\n"
        +
        "Every 5 minutes, erd.data@noaa.gov downloads the list of latest data from all stations for the last 2 hours from https://www.ndbc.noaa.gov/data/latest_obs/latest_obs.txt and updates the near real time .nc files.\";\n"
        +
        "  :id = \"NDBC_46088_met\";\n" +
        "  :infoUrl = \"https://www.ndbc.noaa.gov/\";\n" +
        "  :institution = \"NOAA NDBC, NOAA NMFS SWFSC ERD\";\n" +
        "  :keywords = \"Earth Science > Atmosphere > Air Quality > Visibility,\n" +
        "Earth Science > Atmosphere > Altitude > Planetary Boundary Layer Height,\n" +
        "Earth Science > Atmosphere > Atmospheric Pressure > Atmospheric Pressure Measurements,\n" +
        "Earth Science > Atmosphere > Atmospheric Pressure > Pressure Tendency,\n" +
        "Earth Science > Atmosphere > Atmospheric Pressure > Sea Level Pressure,\n" +
        "Earth Science > Atmosphere > Atmospheric Pressure > Static Pressure,\n" +
        "Earth Science > Atmosphere > Atmospheric Temperature > Air Temperature,\n" +
        "Earth Science > Atmosphere > Atmospheric Temperature > Dew Point Temperature,\n" +
        "Earth Science > Atmosphere > Atmospheric Water Vapor > Dew Point Temperature,\n" +
        "Earth Science > Atmosphere > Atmospheric Winds > Surface Winds,\n" +
        "Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature,\n" +
        "Earth Science > Oceans > Ocean Waves > Significant Wave Height,\n" +
        "Earth Science > Oceans > Ocean Waves > Swells,\n" +
        "Earth Science > Oceans > Ocean Waves > Wave Period,\n" +
        "air, air_pressure_at_sea_level, air_temperature, atmosphere, atmospheric, average, boundary, buoy, coastwatch, data, dew point, dew_point_temperature, direction, dominant, eastward, eastward_wind, from, gust, height, identifier, layer, level, measurements, meridional, meteorological, meteorology, name, ndbc, noaa, northward, northward_wind, ocean, oceans, period, planetary, pressure, quality, sea, sea level, sea_surface_swell_wave_period, sea_surface_swell_wave_significant_height, sea_surface_swell_wave_to_direction, sea_surface_temperature, seawater, significant, speed, sst, standard, static, station, surface, surface waves, surface_altitude, swell, swells, temperature, tendency, tendency_of_air_pressure, time, vapor, visibility, visibility_in_air, water, wave, waves, wcn, wind, wind_from_direction, wind_speed, wind_speed_of_gust, winds, zonal\";\n"
        +
        "  :keywords_vocabulary = \"GCMD Science Keywords\";\n" +
        "  :license = \"The data may be used and redistributed for free but is not intended\n" +
        "for legal use, since it may contain inaccuracies. Neither the data\n" +
        "Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
        "of their employees or contractors, makes any warranty, express or\n" +
        "implied, including warranties of merchantability and fitness for a\n" +
        "particular purpose, or assumes any legal liability for the accuracy,\n" +
        "completeness, or usefulness, of this information.\";\n" +
        "  :naming_authority = \"gov.noaa.pfeg.coastwatch\";\n" +
        "  :Northernmost_Northing = 48.333f; // float\n" +
        "  :project = \"NOAA NDBC and NOAA NMFS SWFSC ERD\";\n" +
        "  :publisher_email = \"erd.data@noaa.gov\";\n" +
        "  :publisher_name = \"NOAA NMFS SWFSC ERD\";\n" +
        "  :publisher_type = \"institution\";\n" +
        "  :publisher_url = \"https://www.pfeg.noaa.gov\";\n" +
        "  :quality = \"Automated QC checks with periodic manual QC\";\n" +
        "  :source = \"station observation\";\n" +
        "  :sourceUrl = \"https://www.ndbc.noaa.gov/\";\n" +
        "  :Southernmost_Northing = 48.333f; // float\n" +
        "  :standard_name_vocabulary = \"CF Standard Name Table v70\";\n" +
        "  :subsetVariables = \"ID, LON, LAT, DEPTH\";\n" +
        "  :summary = \"The National Data Buoy Center (NDBC) distributes meteorological data from\n" +
        "moored buoys maintained by NDBC and others. Moored buoys are the weather\n" +
        "sentinels of the sea. They are deployed in the coastal and offshore waters\n" +
        "from the western Atlantic to the Pacific Ocean around Hawaii, and from the\n" +
        "Bering Sea to the South Pacific. NDBC's moored buoys measure and transmit\n" +
        "barometric pressure; wind direction, speed, and gust; air and sea\n" +
        "temperature; and wave energy spectra from which significant wave height,\n" +
        "dominant wave period, and average wave period are derived. Even the\n" +
        "direction of wave propagation is measured on many moored buoys. See\n" +
        "https://www.ndbc.noaa.gov/measdes.shtml for a description of the measurements.\n" +
        "\n" +
        "The source data from NOAA NDBC has different column names, different units,\n" +
        "and different missing values in different files, and other problems (notably,\n" +
        "lots of rows with duplicate or different values for the same time point).\n" +
        "This dataset is a standardized, reformatted, and lightly edited version of\n" +
        "that source data, created by NOAA NMFS SWFSC ERD (email: erd.data at noaa.gov).\n" +
        "Before 2020-01-29, this dataset only had the data that was closest to a given\n" +
        "hour, rounded to the nearest hour. Now, this dataset has all of the data\n" +
        "available from NDBC with the original time values. If there are multiple\n" +
        "source rows for a given buoy for a given time, only the row with the most\n" +
        "non-NaN data values is kept. If there is a gap in the data, a row of missing\n" +
        "values is inserted (which causes a nice gap when the data is graphed). Also,\n" +
        "some impossible data values are removed, but this data is not perfectly clean.\n" +
        "This dataset is now updated every 5 minutes.\n" +
        "\n" +
        "This dataset has both historical data (quality controlled) and near real time\n" +
        "data (less quality controlled).\";\n" +
        "  :testOutOfDate = \"now-25minutes\";\n" + // changes
        "  :time_coverage_end = \"20..-..-..T..:..:..Z\";\n" + // changes
        "  :time_coverage_start = \"2004-07-01T22:00:00Z\";\n" +
        "  :title = \"NDBC Standard Meteorological Buoy Data, 1970-present\";\n" +
        "  :Westernmost_Easting = -123.167f; // float\n" +
        "}\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    Table table = new Table();
    // this converts fake mv e.g., -9999999.0f to standard mv e.g., Float.NaN
    table.read4DNc(ndbcHistoricalNcDir + "NDBC_46088_met.nc", null, -1, NdbcMetStation.ID_NAME, NdbcMetStation.idIndex); // standardizeWhat=-1
    testHistorical46088(table);
  }

  /**
   * This tests the Historical 46088 info in the table.
   */
  void testHistorical46088(Table table) throws Exception {
    // String2.log("\n*** testHistorical46088 nRows=" + table.nRows());

    String fullResults;
    String results, expected;
    int po;

    // Note that VIS converted from nautical miles to km: old*kmPerNMile;
    // and TIDE converted from feet to meters: old*Math2.meterPerFoot
    // String2.log(table.toString(5));

    // !!! CONVERT TIME TO STRING to simplify testing
    table.convertEpochSecondsColumnToIso8601(NdbcMetStation.timeIndex);

    fullResults = table.dataToString();
    expected = "LON,LAT,DEPTH,TIME,ID,WD,WSPD,GST,WVHT,DPD,APD,MWD,BAR,ATMP,WTMP,DEWP,VIS,PTDY,TIDE,WSPU,WSPV\n" +
        "-123.167,48.333,0.0,2004-07-01T22:00:00Z,46088,228,5.7,6.5,0.44,5.0,3.46,270,,,,11.0,,,,4.2,3.8\n" +
        "-123.167,48.333,0.0,2004-07-01T23:00:00Z,46088,242,7.8,10.3,0.3,4.35,2.95,264,,,,11.0,,,,6.9,3.7\n" +
        "-123.167,48.333,0.0,2004-07-02T00:00:00Z,46088,,,,,,,,,,,,,,,,\n" +
        "-123.167,48.333,0.0,2004-07-02T01:00:00Z,46088,254,8.5,9.8,0.64,3.33,3.03,259,1015.8,14.0,11.3,11.0,,,,8.2,2.3\n"
        +
        "-123.167,48.333,0.0,2004-07-02T02:00:00Z,46088,251,8.3,10.3,0.53,3.03,3.06,264,1015.4,14.0,11.2,10.8,,,,7.8,2.7\n"
        +
        "-123.167,48.333,0.0,2004-07-02T03:00:00Z,46088,244,6.7,7.5,0.59,3.13,3.19,260,1015.6,13.6,10.9,10.7,,,,6.0,2.9\n";
    results = fullResults.substring(0, expected.length());
    Test.ensureEqual(results, expected, "results=\n" + results);

    // chunk of data with multiple inserted mv rows
    // 2004-07-30T03:00:00Z and 2004-07-30T05:00:00Z
    expected = "-123.167,48.333,0.0,2004-07-30T00:00:00Z,46088,263,7.3,8.2,0.44,8.33,3.53,247,1013.3,13.9,12.7,13.0,,,,7.2,0.9\n"
        +
        "-123.167,48.333,0.0,2004-07-30T01:00:00Z,46088,249,6.2,8.2,0.44,7.14,3.32,257,1012.9,13.6,13.0,12.9,,,,5.8,2.2\n"
        +
        "-123.167,48.333,0.0,2004-07-30T02:00:00Z,46088,251,9.5,10.8,0.56,2.94,3.17,259,1012.3,13.2,12.8,12.7,,,,9.0,3.1\n"
        +
        "-123.167,48.333,0.0,2004-07-30T03:00:00Z,46088,,,,,,,,,,,,,,,,\n" +
        "-123.167,48.333,0.0,2004-07-30T04:00:00Z,46088,272,10.8,13.0,1.12,4.35,3.66,256,1011.5,15.0,12.8,13.1,,,,10.8,-0.4\n"
        +
        "-123.167,48.333,0.0,2004-07-30T05:00:00Z,46088,,,,,,,,,,,,,,,,\n" + // big gap
        "-123.167,48.333,0.0,2004-08-05T20:00:00Z,46088,231,2.8,4.3,,,,,1014.7,14.1,11.7,11.4,,,,2.2,1.8\n" +
        "-123.167,48.333,0.0,2004-08-05T21:00:00Z,46088,189,2.2,2.8,0.31,3.45,3.33,278,1014.9,13.1,11.9,11.7,,,,0.3,2.2\n";
    po = fullResults.indexOf("2004-07-30T00:00:00Z");
    results = fullResults.substring(po - 20, po - 20 + expected.length());
    Test.ensureEqual(results, expected, "results=\n" + results);

    // This is the winner of a fancyRemoveDuplicateTime row:
    // YYYY MM DD hh mm WD WSPD GST WVHT DPD APD MWD BARO ATMP WTMP DEWP VIS TIDE
    // 2014 02 20 19 50 263 10.2 12.1 1.19 5.88 4.84 245 1019.4 7.1 7.1 2.8 99.0
    // 99.00
    expected =
        // source file:
        // 2014 02 20 19 20 266 9.7 12.2 1.31 5.56 4.81 242 1019.5 6.9 7.0 2.7 99.0
        // 99.00
        // 2014 02 20 19 40 999 99.0 99.0 99.00 99.00 99.00 999 1019.5 999.0 6.1 999.0
        // 99.0 99.00
        // 2014 02 20 19 50 999 99.0 99.0 99.00 99.00 99.00 999 1019.4 999.0 6.3 999.0
        // 99.0 99.00
        // 2014 02 20 19 50 263 10.2 12.1 1.19 5.88 4.84 245 1019.4 7.1 7.1 2.8 99.0
        // 99.00
        // 2014 02 20 20 10 999 99.0 99.0 99.00 99.00 99.00 999 1019.3 999.0 7.0 999.0
        // 99.0 99.00
        // 2014 02 20 20 20 999 99.0 99.0 99.00 99.00 99.00 999 1019.1 999.0 6.8 999.0
        // 99.0 99.00
        // 2014 02 20 20 20 271 11.0 13.2 1.16 5.88 4.97 250 1019.5 7.2 7.1 2.1 99.0
        // 99.00
        // 2014 02 20 20 40 999 99.0 99.0 99.00 99.00 99.00 999 1018.8 999.0 6.7 999.0
        // 99.0 99.00
        // 2014 02 20 20 50 999 99.0 99.0 99.00 99.00 99.00 999 1018.8 999.0 6.6 999.0
        // 99.0 99.00
        // 2014 02 20 20 50 275 10.2 12.8 1.12 5.88 5.01 245 1019.4 7.1 7.1 2.6 99.0
        // 99.00
        // 2014 02 20 21 10 999 99.0 99.0 99.00 99.00 99.00 999 1018.5 999.0 6.7 999.0
        // 99.0 99.00

        "-123.167,48.333,0.0,2014-02-20T19:20:00Z,46088,266,9.7,12.2,1.31,5.56,4.81,242,1019.5,6.9,7.0,2.7,,,,9.7,0.7\n"
            +
            "-123.167,48.333,0.0,2014-02-20T19:40:00Z,46088,,,,,,,,1019.5,,6.1,,,,,,\n" +
            "-123.167,48.333,0.0,2014-02-20T19:50:00Z,46088,263,10.2,12.1,1.19,5.88,4.84,245,1019.4,7.1,7.1,2.8,,,,10.1,1.2\n"
            +
            "-123.167,48.333,0.0,2014-02-20T20:10:00Z,46088,,,,,,,,1019.3,,7.0,,,,,,\n" +
            "-123.167,48.333,0.0,2014-02-20T20:20:00Z,46088,271,11.0,13.2,1.16,5.88,4.97,250,1019.5,7.2,7.1,2.1,,,,11.0,-0.2\n"
            +
            "-123.167,48.333,0.0,2014-02-20T20:40:00Z,46088,,,,,,,,1018.8,,6.7,,,,,,\n" +
            "-123.167,48.333,0.0,2014-02-20T20:50:00Z,46088,275,10.2,12.8,1.12,5.88,5.01,245,1019.4,7.1,7.1,2.6,,,,10.2,-0.9\n"
            +
            "-123.167,48.333,0.0,2014-02-20T21:10:00Z,46088,,,,,,,,1018.5,,6.7,,,,,,\n";
    po = fullResults.indexOf("2014-02-20T19:20");
    results = fullResults.substring(po - 20, po - 20 + expected.length());
    Test.ensureEqual(results, expected, "results=\n" + results);

    // UPDATE_EACH_MONTH
    po = fullResults.indexOf("2021-11-07T23:30"); // change date each month
    results = fullResults.substring(po - 20);
    expected =
        // last rows from e.g., https://www.ndbc.noaa.gov/data/stdmet/Sep/46088.txt
        // 2022 09 30 23 30 268 2.5 2.9 99.00 99.00 99.00 999 1020.0 12.2 11.1 11.7 99.0
        // 99.00
        // 2022 09 30 23 40 271 2.1 2.8 0.09 99.00 3.97 999 1019.9 11.9 10.9 11.5 99.0
        // 99.00
        // 2022 09 30 23 50 271 2.1 2.4 99.00 99.00 99.00 999 1019.9 11.8 11.0 11.4 99.0
        // 99.00

        // copy/paste results verify that they match values in file (above)
        "-123.167,48.333,0.0,2021-11-07T23:30:00Z,46088,236,8.2,13.6,,,,,1014.8,7.7,9.7,2.9,,,,6.8,4.6\n" + //
            "-123.167,48.333,0.0,2021-11-07T23:40:00Z,46088,233,7.2,9.6,0.45,4.17,3.35,108,1014.7,7.6,9.7,2.8,,,,5.8,4.3\n"
            + //
            "-123.167,48.333,0.0,2021-11-07T23:50:00Z,46088,224,6.4,9.2,,,,,1015.1,7.8,9.7,2.9,,,,4.4,4.6\n" + //
            "-123.167,48.333,0.0,2021-11-08T00:00:00Z,46088,228,7.9,13.4,,,,,1015.2,8.0,9.7,3.4,,,,5.9,5.3\n" + //
            "-123.167,48.333,0.0,2021-11-08T00:10:00Z,46088,,,,,,,,1015.5,,9.7,,,,,,\n";
    // "-123.167,48.333,0.0,2021-09-30T23:30:00Z,46088,268,2.5,2.9,,,,,1020.0,12.2,11.1,11.7,,,,2.5,0.1\n"
    // +
    // "-123.167,48.333,0.0,2021-09-30T23:40:00Z,46088,271,2.1,2.8,0.09,,3.97,,1019.9,11.9,10.9,11.5,,,,2.1,0.0\n"
    // +
    // "-123.167,48.333,0.0,2021-09-30T23:50:00Z,46088,271,2.1,2.4,,,,,1019.9,11.8,11.0,11.4,,,,2.1,0.0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    String2.log("testHistorical46088 was successful");
  }

  /**
   * This reads the Nrt 46088.nc file and makes sure it has the right info.
   */
  void testNrt46089Nc(String ndbcNrtNcDir) throws Exception {
    // String2.log("\n*** testNrt46088Nc");

    String results = NcHelper.ncdump(ndbcNrtNcDir + "NDBC_46089_met.nc", "-h");
    results = results.replaceFirst("TIME = \\d+;", "TIME = ....;");
    results = results.replaceAll("TIME=\\d+", "TIME=....");
    results = results.replaceAll(":actual_range = .* //", ":actual_range = ..., ...; //");
    results = results.replaceFirst(":date_created = \"20..-..-..\";", ":date_created = \"20..-..-..\";");
    results = results.replaceFirst(":date_issued = \"20..-..-..\";", ":date_issued = \"20..-..-..\";");
    String expected = "netcdf NDBC_46089_met.nc {\n" +
        "  dimensions:\n" +
        "    LON = 1;\n" +
        "    LAT = 1;\n" +
        "    DEPTH = 1;\n" +
        "    TIME = ....;\n" + // changes
        "    ID_strlen = 5;\n" +
        "  variables:\n" +
        "    float LON(LON=1);\n" +
        "      :_CoordinateAxisType = \"Lon\";\n" +
        "      :actual_range = ..., ...; // float\n" +
        "      :axis = \"X\";\n" +
        "      :colorBarMaximum = 180.0; // double\n" +
        "      :colorBarMinimum = -180.0; // double\n" +
        "      :comment = \"The longitude of the station.\";\n" +
        "      :ioos_category = \"Location\";\n" +
        "      :long_name = \"Longitude\";\n" +
        "      :standard_name = \"longitude\";\n" +
        "      :units = \"degrees_east\";\n" +
        "\n" +
        "    float LAT(LAT=1);\n" +
        "      :_CoordinateAxisType = \"Lat\";\n" +
        "      :actual_range = ..., ...; // float\n" +
        "      :axis = \"Y\";\n" +
        "      :colorBarMaximum = 90.0; // double\n" +
        "      :colorBarMinimum = -90.0; // double\n" +
        "      :comment = \"The latitude of the station.\";\n" +
        "      :ioos_category = \"Location\";\n" +
        "      :long_name = \"Latitude\";\n" +
        "      :standard_name = \"latitude\";\n" +
        "      :units = \"degrees_north\";\n" +
        "\n" +
        "    float DEPTH(DEPTH=1);\n" +
        "      :_CoordinateAxisType = \"Height\";\n" +
        "      :_CoordinateZisPositive = \"down\";\n" +
        "      :actual_range = ..., ...; // float\n" +
        "      :axis = \"Z\";\n" +
        "      :colorBarMaximum = 0.0; // double\n" +
        "      :colorBarMinimum = 0.0; // double\n" +
        "      :comment = \"The depth of the station, nominally 0 (see station information for details).\";\n" +
        "      :ioos_category = \"Location\";\n" +
        "      :long_name = \"Depth\";\n" +
        "      :positive = \"down\";\n" +
        "      :standard_name = \"depth\";\n" +
        "      :units = \"m\";\n" +
        "\n" +
        "    double TIME(TIME=....);\n" +
        "      :_CoordinateAxisType = \"Time\";\n" +
        "      :actual_range = ..., ...; // double\n" +
        "      :axis = \"T\";\n" +
        "      :ioos_category = \"Time\";\n" +
        "      :long_name = \"Time\";\n" +
        "      :standard_name = \"time\";\n" +
        "      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
        "      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
        "\n" +
        "    short WD(TIME=...., DEPTH=1, LAT=1, LON=1);\n" +
        "      :_FillValue = 32767S; // short\n" +
        "      :actual_range = ..., ...; // short\n" +
        "      :colorBarMaximum = 360.0; // double\n" +
        "      :colorBarMinimum = 0.0; // double\n" +
        "      :comment = \"Wind direction (the direction the wind is coming from in degrees clockwise from true N) during the same period used for WSPD.\";\n"
        +
        "      :ioos_category = \"Wind\";\n" +
        "      :long_name = \"Wind Direction\";\n" +
        "      :missing_value = 32767S; // short\n" +
        "      :standard_name = \"wind_from_direction\";\n" +
        "      :units = \"degrees_true\";\n" +
        "\n" +
        "    float WSPD(TIME=...., DEPTH=1, LAT=1, LON=1);\n" +
        "      :_FillValue = -9999999.0f; // float\n" +
        "      :actual_range = ..., ...; // float\n" +
        "      :colorBarMaximum = 15.0; // double\n" +
        "      :colorBarMinimum = 0.0; // double\n" +
        "      :comment = \"Average wind speed (m/s).\";\n" +
        "      :ioos_category = \"Wind\";\n" +
        "      :long_name = \"Wind Speed\";\n" +
        "      :missing_value = -9999999.0f; // float\n" +
        "      :standard_name = \"wind_speed\";\n" +
        "      :units = \"m s-1\";\n" +
        "\n" +
        "    float GST(TIME=...., DEPTH=1, LAT=1, LON=1);\n" +
        "      :_FillValue = -9999999.0f; // float\n" +
        "      :actual_range = ..., ...; // float\n" +
        "      :colorBarMaximum = 30.0; // double\n" +
        "      :colorBarMinimum = 0.0; // double\n" +
        "      :comment = \"Peak 5 or 8 second gust speed (m/s).\";\n" +
        "      :ioos_category = \"Wind\";\n" +
        "      :long_name = \"Wind Gust Speed\";\n" +
        "      :missing_value = -9999999.0f; // float\n" +
        "      :standard_name = \"wind_speed_of_gust\";\n" +
        "      :units = \"m s-1\";\n" +
        "\n" +
        "    float WVHT(TIME=...., DEPTH=1, LAT=1, LON=1);\n" +
        "      :_FillValue = -9999999.0f; // float\n" +
        "      :actual_range = ..., ...; // float\n" +
        "      :colorBarMaximum = 10.0; // double\n" +
        "      :colorBarMinimum = 0.0; // double\n" +
        "      :comment = \"Significant wave height (meters) is calculated as the average of the highest one-third of all of the wave heights during the 20-minute sampling period.\";\n"
        +
        "      :ioos_category = \"Surface Waves\";\n" +
        "      :long_name = \"Wave Height\";\n" +
        "      :missing_value = -9999999.0f; // float\n" +
        "      :standard_name = \"sea_surface_wave_significant_height\";\n" +
        "      :units = \"m\";\n" +
        "\n" +
        "    float DPD(TIME=...., DEPTH=1, LAT=1, LON=1);\n" +
        "      :_FillValue = -9999999.0f; // float\n" +
        "      :actual_range = ..., ...; // float\n" +
        "      :colorBarMaximum = 20.0; // double\n" +
        "      :colorBarMinimum = 0.0; // double\n" +
        "      :comment = \"Dominant wave period (seconds) is the period with the maximum wave energy.\";\n" +
        "      :ioos_category = \"Surface Waves\";\n" +
        "      :long_name = \"Wave Period, Dominant\";\n" +
        "      :missing_value = -9999999.0f; // float\n" +
        "      :standard_name = \"sea_surface_swell_wave_period\";\n" +
        "      :units = \"s\";\n" +
        "\n" +
        "    float APD(TIME=...., DEPTH=1, LAT=1, LON=1);\n" +
        "      :_FillValue = -9999999.0f; // float\n" +
        "      :actual_range = ..., ...; // float\n" +
        "      :colorBarMaximum = 20.0; // double\n" +
        "      :colorBarMinimum = 0.0; // double\n" +
        "      :comment = \"Average wave period (seconds) of all waves during the 20-minute period.\";\n" +
        "      :ioos_category = \"Surface Waves\";\n" +
        "      :long_name = \"Wave Period, Average\";\n" +
        "      :missing_value = -9999999.0f; // float\n" +
        "      :standard_name = \"sea_surface_swell_wave_period\";\n" +
        "      :units = \"s\";\n" +
        "\n" +
        "    short MWD(TIME=...., DEPTH=1, LAT=1, LON=1);\n" +
        "      :_FillValue = 32767S; // short\n" +
        "      :actual_range = ..., ...; // short\n" +
        "      :colorBarMaximum = 360.0; // double\n" +
        "      :colorBarMinimum = 0.0; // double\n" +
        "      :comment = \"Mean wave direction corresponding to energy of the dominant period (DOMPD).\";\n" +
        "      :ioos_category = \"Surface Waves\";\n" +
        "      :long_name = \"Wave Direction\";\n" +
        "      :missing_value = 32767S; // short\n" +
        "      :standard_name = \"sea_surface_wave_to_direction\";\n" +
        "      :units = \"degrees_true\";\n" +
        "\n" +
        "    float BAR(TIME=...., DEPTH=1, LAT=1, LON=1);\n" +
        "      :_FillValue = -9999999.0f; // float\n" +
        "      :actual_range = ..., ...; // float\n" +
        "      :colorBarMaximum = 1050.0; // double\n" +
        "      :colorBarMinimum = 950.0; // double\n" +
        "      :comment = \"Air pressure (hPa). ('PRES' on some NDBC tables.) For C-MAN sites and Great Lakes buoys, the recorded pressure is reduced to sea level using the method described in NWS Technical Procedures Bulletin 291 (11/14/80).\";\n"
        +
        "      :ioos_category = \"Pressure\";\n" +
        "      :long_name = \"Air Pressure\";\n" +
        "      :missing_value = -9999999.0f; // float\n" +
        "      :standard_name = \"air_pressure_at_sea_level\";\n" +
        "      :units = \"hPa\";\n" +
        "\n" +
        "    float ATMP(TIME=...., DEPTH=1, LAT=1, LON=1);\n" +
        "      :_FillValue = -9999999.0f; // float\n" +
        "      :actual_range = ..., ...; // float\n" + // 2020-06-23 disappeared because no data? //2020-07-21 it's back
        "      :colorBarMaximum = 40.0; // double\n" +
        "      :colorBarMinimum = -10.0; // double\n" +
        "      :comment = \"Air temperature (Celsius). For sensor heights on buoys, see Hull Descriptions. For sensor heights at C-MAN stations, see C-MAN Sensor Locations.\";\n"
        +
        "      :ioos_category = \"Temperature\";\n" +
        "      :long_name = \"Air Temperature\";\n" +
        "      :missing_value = -9999999.0f; // float\n" +
        "      :standard_name = \"air_temperature\";\n" +
        "      :units = \"degree_C\";\n" +
        "\n" +
        "    float WTMP(TIME=...., DEPTH=1, LAT=1, LON=1);\n" +
        "      :_FillValue = -9999999.0f; // float\n" +
        "      :actual_range = ..., ...; // float\n" +
        "      :colorBarMaximum = 32.0; // double\n" +
        "      :colorBarMinimum = 0.0; // double\n" +
        "      :comment = \"Sea surface temperature (Celsius). For sensor depth, see Hull Description.\";\n" +
        "      :ioos_category = \"Temperature\";\n" +
        "      :long_name = \"SST\";\n" +
        "      :missing_value = -9999999.0f; // float\n" +
        "      :standard_name = \"sea_surface_temperature\";\n" +
        "      :units = \"degree_C\";\n" +
        "\n" +
        "    float DEWP(TIME=...., DEPTH=1, LAT=1, LON=1);\n" +
        "      :_FillValue = -9999999.0f; // float\n" +
        "      :actual_range = ..., ...; // float\n" + // 2020-06-23 disappeared because no data? //2020-07-21 it's back
        "      :colorBarMaximum = 40.0; // double\n" +
        "      :colorBarMinimum = 0.0; // double\n" +
        "      :comment = \"Dewpoint temperature taken at the same height as the air temperature measurement.\";\n" +
        "      :ioos_category = \"Temperature\";\n" +
        "      :long_name = \"Dewpoint Temperature\";\n" +
        "      :missing_value = -9999999.0f; // float\n" +
        "      :standard_name = \"dew_point_temperature\";\n" +
        "      :units = \"degree_C\";\n" +
        "\n" +
        "    float VIS(TIME=...., DEPTH=1, LAT=1, LON=1);\n" +
        "      :_FillValue = -9999999.0f; // float\n" +
        "      :colorBarMaximum = 100.0; // double\n" +
        "      :colorBarMinimum = 0.0; // double\n" +
        "      :comment = \"Station visibility (km, originally nautical miles in the NDBC .txt files). Note that buoy stations are limited to reports from 0 to 1.6 nmi.\";\n"
        +
        "      :ioos_category = \"Meteorology\";\n" +
        "      :long_name = \"Station Visibility\";\n" +
        "      :missing_value = -9999999.0f; // float\n" +
        "      :standard_name = \"visibility_in_air\";\n" +
        "      :units = \"km\";\n" +
        "\n" +
        "    float PTDY(TIME=...., DEPTH=1, LAT=1, LON=1);\n" +
        "      :_FillValue = -9999999.0f; // float\n" +
        "      :actual_range = ..., ...; // float\n" +
        "      :colorBarMaximum = 3.0; // double\n" +
        "      :colorBarMinimum = -3.0; // double\n" +
        "      :comment = \"Pressure Tendency is the direction (plus or minus) and the amount of pressure change (hPa) for a three hour period ending at the time of observation.\";\n"
        +
        "      :ioos_category = \"Pressure\";\n" +
        "      :long_name = \"Pressure Tendency\";\n" +
        "      :missing_value = -9999999.0f; // float\n" +
        "      :standard_name = \"tendency_of_air_pressure\";\n" +
        "      :units = \"hPa\";\n" +
        "\n" +
        "    float TIDE(TIME=...., DEPTH=1, LAT=1, LON=1);\n" +
        "      :_FillValue = -9999999.0f; // float\n" +
        "      :colorBarMaximum = 5.0; // double\n" +
        "      :colorBarMinimum = -5.0; // double\n" +
        "      :comment = \"The water level in meters (originally feet in the NDBC .txt files) above or below Mean Lower Low Water (MLLW).\";\n"
        +
        "      :ioos_category = \"Sea Level\";\n" +
        "      :long_name = \"Water Level\";\n" +
        "      :missing_value = -9999999.0f; // float\n" +
        "      :standard_name = \"surface_altitude\";\n" +
        "      :units = \"m\";\n" +
        "\n" +
        "    float WSPU(TIME=...., DEPTH=1, LAT=1, LON=1);\n" +
        "      :_FillValue = -9999999.0f; // float\n" +
        "      :actual_range = ..., ...; // float\n" +
        "      :colorBarMaximum = 15.0; // double\n" +
        "      :colorBarMinimum = -15.0; // double\n" +
        "      :comment = \"The zonal wind speed (m/s) indicates the u component of where the wind is going, derived from Wind Direction and Wind Speed.\";\n"
        +
        "      :ioos_category = \"Wind\";\n" +
        "      :long_name = \"Wind Speed, Zonal\";\n" +
        "      :missing_value = -9999999.0f; // float\n" +
        "      :standard_name = \"eastward_wind\";\n" +
        "      :units = \"m s-1\";\n" +
        "\n" +
        "    float WSPV(TIME=...., DEPTH=1, LAT=1, LON=1);\n" +
        "      :_FillValue = -9999999.0f; // float\n" +
        "      :actual_range = ..., ...; // float\n" +
        "      :colorBarMaximum = 15.0; // double\n" +
        "      :colorBarMinimum = -15.0; // double\n" +
        "      :comment = \"The meridional wind speed (m/s) indicates the v component of where the wind is going, derived from Wind Direction and Wind Speed.\";\n"
        +
        "      :ioos_category = \"Wind\";\n" +
        "      :long_name = \"Wind Speed, Meridional\";\n" +
        "      :missing_value = -9999999.0f; // float\n" +
        "      :standard_name = \"northward_wind\";\n" +
        "      :units = \"m s-1\";\n" +
        "\n" +
        "    char ID(ID_strlen=5);\n" +
        "      :_Encoding = \"ISO-8859-1\";\n" +
        "      :cf_role = \"timeseries_id\";\n" +
        "      :comment = \"The station identifier.\";\n" +
        "      :ioos_category = \"Identifier\";\n" +
        "      :long_name = \"Station Identifier\";\n" +
        "\n" +
        "  // global attributes:\n" +
        "  :cdm_data_type = \"TimeSeries\";\n" +
        "  :cdm_timeseries_variables = \"ID, LON, LAT, DEPTH\";\n" +
        "  :contributor_name = \"NOAA NDBC\";\n" +
        "  :contributor_role = \"Source of data.\";\n" +
        "  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
        "  :creator_email = \"erd.data@noaa.gov\";\n" +
        "  :creator_name = \"NOAA NMFS SWFSC ERD\";\n" +
        "  :creator_type = \"institution\";\n" +
        "  :creator_url = \"https://www.pfeg.noaa.gov\";\n" +
        "  :date_created = \"20..-..-..\";\n" + // changes
        "  :date_issued = \"20..-..-..\";\n" + // changes
        "  :Easternmost_Easting = -125.76f; // float\n" +
        "  :geospatial_lat_max = 45.908f; // float\n" +
        "  :geospatial_lat_min = 45.908f; // float\n" +
        "  :geospatial_lat_units = \"degrees_north\";\n" +
        "  :geospatial_lon_max = -125.76f; // float\n" +
        "  :geospatial_lon_min = -125.76f; // float\n" +
        "  :geospatial_lon_units = \"degrees_east\";\n" +
        "  :geospatial_vertical_max = 0.0f; // float\n" +
        "  :geospatial_vertical_min = 0.0f; // float\n" +
        "  :geospatial_vertical_positive = \"down\";\n" +
        "  :geospatial_vertical_units = \"m\";\n" +
        "  :history = \"Around the 25th of each month, erd.data@noaa.gov downloads the latest yearly and monthly historical .txt.gz files from https://www.ndbc.noaa.gov/data/historical/stdmet/ and generates one historical .nc file for each station. erd.data@noaa.gov also downloads all of the 45day near real time .txt files from https://www.ndbc.noaa.gov/data/realtime2/ and generates one near real time .nc file for each station.\n"
        +
        "Every 5 minutes, erd.data@noaa.gov downloads the list of latest data from all stations for the last 2 hours from https://www.ndbc.noaa.gov/data/latest_obs/latest_obs.txt and updates the near real time .nc files.\";\n"
        +
        "  :id = \"NDBC_46089_met\";\n" +
        "  :infoUrl = \"https://www.ndbc.noaa.gov/\";\n" +
        "  :institution = \"NOAA NDBC, NOAA NMFS SWFSC ERD\";\n" +
        "  :keywords = \"Earth Science > Atmosphere > Air Quality > Visibility,\n" +
        "Earth Science > Atmosphere > Altitude > Planetary Boundary Layer Height,\n" +
        "Earth Science > Atmosphere > Atmospheric Pressure > Atmospheric Pressure Measurements,\n" +
        "Earth Science > Atmosphere > Atmospheric Pressure > Pressure Tendency,\n" +
        "Earth Science > Atmosphere > Atmospheric Pressure > Sea Level Pressure,\n" +
        "Earth Science > Atmosphere > Atmospheric Pressure > Static Pressure,\n" +
        "Earth Science > Atmosphere > Atmospheric Temperature > Air Temperature,\n" +
        "Earth Science > Atmosphere > Atmospheric Temperature > Dew Point Temperature,\n" +
        "Earth Science > Atmosphere > Atmospheric Water Vapor > Dew Point Temperature,\n" +
        "Earth Science > Atmosphere > Atmospheric Winds > Surface Winds,\n" +
        "Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature,\n" +
        "Earth Science > Oceans > Ocean Waves > Significant Wave Height,\n" +
        "Earth Science > Oceans > Ocean Waves > Swells,\n" +
        "Earth Science > Oceans > Ocean Waves > Wave Period,\n" +
        "air, air_pressure_at_sea_level, air_temperature, atmosphere, atmospheric, average, boundary, buoy, coastwatch, data, dew point, dew_point_temperature, direction, dominant, eastward, eastward_wind, from, gust, height, identifier, layer, level, measurements, meridional, meteorological, meteorology, name, ndbc, noaa, northward, northward_wind, ocean, oceans, period, planetary, pressure, quality, sea, sea level, sea_surface_swell_wave_period, sea_surface_swell_wave_significant_height, sea_surface_swell_wave_to_direction, sea_surface_temperature, seawater, significant, speed, sst, standard, static, station, surface, surface waves, surface_altitude, swell, swells, temperature, tendency, tendency_of_air_pressure, time, vapor, visibility, visibility_in_air, water, wave, waves, wcn, wind, wind_from_direction, wind_speed, wind_speed_of_gust, winds, zonal\";\n"
        +
        "  :keywords_vocabulary = \"GCMD Science Keywords\";\n" +
        "  :license = \"The data may be used and redistributed for free but is not intended\n" +
        "for legal use, since it may contain inaccuracies. Neither the data\n" +
        "Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
        "of their employees or contractors, makes any warranty, express or\n" +
        "implied, including warranties of merchantability and fitness for a\n" +
        "particular purpose, or assumes any legal liability for the accuracy,\n" +
        "completeness, or usefulness, of this information.\";\n" +
        "  :naming_authority = \"gov.noaa.pfeg.coastwatch\";\n" +
        "  :Northernmost_Northing = 45.908f; // float\n" +
        "  :project = \"NOAA NDBC and NOAA NMFS SWFSC ERD\";\n" +
        "  :publisher_email = \"erd.data@noaa.gov\";\n" +
        "  :publisher_name = \"NOAA NMFS SWFSC ERD\";\n" +
        "  :publisher_type = \"institution\";\n" +
        "  :publisher_url = \"https://www.pfeg.noaa.gov\";\n" +
        "  :quality = \"Automated QC checks with periodic manual QC\";\n" +
        "  :source = \"station observation\";\n" +
        "  :sourceUrl = \"https://www.ndbc.noaa.gov/\";\n" +
        "  :Southernmost_Northing = 45.908f; // float\n" +
        "  :standard_name_vocabulary = \"CF Standard Name Table v70\";\n" +
        "  :subsetVariables = \"ID, LON, LAT, DEPTH\";\n" +
        "  :summary = \"The National Data Buoy Center (NDBC) distributes meteorological data from\n" +
        "moored buoys maintained by NDBC and others. Moored buoys are the weather\n" +
        "sentinels of the sea. They are deployed in the coastal and offshore waters\n" +
        "from the western Atlantic to the Pacific Ocean around Hawaii, and from the\n" +
        "Bering Sea to the South Pacific. NDBC's moored buoys measure and transmit\n" +
        "barometric pressure; wind direction, speed, and gust; air and sea\n" +
        "temperature; and wave energy spectra from which significant wave height,\n" +
        "dominant wave period, and average wave period are derived. Even the\n" +
        "direction of wave propagation is measured on many moored buoys. See\n" +
        "https://www.ndbc.noaa.gov/measdes.shtml for a description of the measurements.\n" +
        "\n" +
        "The source data from NOAA NDBC has different column names, different units,\n" +
        "and different missing values in different files, and other problems (notably,\n" +
        "lots of rows with duplicate or different values for the same time point).\n" +
        "This dataset is a standardized, reformatted, and lightly edited version of\n" +
        "that source data, created by NOAA NMFS SWFSC ERD (email: erd.data at noaa.gov).\n" +
        "Before 2020-01-29, this dataset only had the data that was closest to a given\n" +
        "hour, rounded to the nearest hour. Now, this dataset has all of the data\n" +
        "available from NDBC with the original time values. If there are multiple\n" +
        "source rows for a given buoy for a given time, only the row with the most\n" +
        "non-NaN data values is kept. If there is a gap in the data, a row of missing\n" +
        "values is inserted (which causes a nice gap when the data is graphed). Also,\n" +
        "some impossible data values are removed, but this data is not perfectly clean.\n" +
        "This dataset is now updated every 5 minutes.\n" +
        "\n" +
        "This dataset has both historical data (quality controlled) and near real time\n" +
        "data (less quality controlled).\";\n" +
        "  :testOutOfDate = \"now-25minutes\";\n" +
        "  :time_coverage_end = \"2022-01-18T20:10:00Z\";\n" + // Don't sanitize. I want to see this.
        "  :time_coverage_start = \"2022-01-01T00:00:00Z\";\n" + // Don't sanitize. I want to see this.
        "  :title = \"NDBC Standard Meteorological Buoy Data, 1970-present\";\n" +
        "  :Westernmost_Easting = -125.76f; // float\n" +
        "}\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    Table table = new Table();
    // this converts fake mv e.g., -9999999.0f to standard mv e.g., Float.NaN
    table.read4DNc(ndbcNrtNcDir + "NDBC_46089_met.nc", null, -1, NdbcMetStation.ID_NAME, NdbcMetStation.idIndex); // standardizeWhat=-1
    this.testNrt46089(table);
  }

  /**
   * This tests the Nrt 46088 info in the table.
   */
  void testNrt46089(Table table) throws Exception {
    // String2.log("\n*** testNrt46088 nRows=" + table.nRows());
    table.convertEpochSecondsColumnToIso8601(NdbcMetStation.timeIndex);
    String results, expected;
    String fullResults;
    int po;

    // Note that VIS converted from nautical miles to km: old*kmPerNMile;
    // and TIDE converted from feet to meters: old*Math2.meterPerFoot

    fullResults = table.dataToString();

    // UPDATE_EACH_MONTH
    // 1) copy first post-historical rows from 45day file 46088.txt
    // https://www.ndbc.noaa.gov/data/realtime2/46088.txt //45 day
    // Copied rows are in opposite order to expected.

    // 2022 10 01 00 20 280 1.0 2.0 0.1 MM 3.5 MM 1019.7 12.0 10.9 11.1 MM MM MM
    // 2022 10 01 00 10 260 1.0 2.0 0.1 MM 3.5 MM 1019.8 12.0 10.6 11.5 MM MM MM
    // 2022 10 01 00 00 270 2.0 2.0 MM MM MM MM 1019.8 11.7 10.6 11.4 MM MM MM

    // 2) Run the test to get the actual expected content, paste it below, and check
    // that data matches
    // 3) Rerun the test
    expected =
        // Older start time because no monthly files for 46088
        "LON,LAT,DEPTH,TIME,ID,WD,WSPD,GST,WVHT,DPD,APD,MWD,BAR,ATMP,WTMP,DEWP,VIS,PTDY,TIDE,WSPU,WSPV\n" +
            "-125.76,45.908,0.0,2022-01-01T00:00:00Z,46089,10,5.0,7.0,,,,,1019.2,5.3,,-2.8,,,,-0.9,-4.9\n" + //
            "-125.76,45.908,0.0,2022-01-01T00:10:00Z,46089,10,5.0,7.0,,,,,1019.3,5.3,9.1,-2.4,,,,-0.9,-4.9\n" + //
            "-125.76,45.908,0.0,2022-01-01T00:20:00Z,46089,20,5.0,7.0,,,,,1019.3,5.3,9.1,-2.1,,,,-1.7,-4.7\n";
    // "-123.167,48.333,0.0,2022-10-01T00:00:00Z,46088,270,2.0,2.0,,,,,1019.8,11.7,10.6,11.4,,,,2.0,0.0\n"
    // +
    // "-123.167,48.333,0.0,2022-10-01T00:10:00Z,46088,260,1.0,2.0,0.1,,3.5,,1019.8,12.0,10.6,11.5,,,,1.0,0.2\n"
    // +
    // "-123.167,48.333,0.0,2022-10-01T00:20:00Z,46088,280,1.0,2.0,0.1,,3.5,,1019.7,12.0,10.9,11.1,,,,1.0,-0.2\n";
    results = fullResults.substring(0, expected.length());
    Test.ensureEqual(results, expected, "results=\n" + results);

    // 4) copy most recent times in 45day file 46088.txt
    // 2022 10 14 20 40 MM 0.0 1.0 MM MM MM MM 1017.5 12.3 10.7 10.7 MM MM MM
    // 2022 10 14 20 30 MM 0.0 1.0 MM MM MM MM 1017.7 12.2 10.6 10.8 MM MM MM
    // 2022 10 14 20 20 MM 0.0 1.0 0.1 MM 4.6 MM 1017.8 11.8 10.7 10.8 MM MM MM

    expected =
        // 5) Put correct 3rd-to-the-last date/time on first row
        // 6) Run the test to get the actual expected content and paste it below
        // 7) Rerun the test
        // 8) The values here may change when addLast is run (updated info)
        "-125.76,45.908,0.0,2022-01-18T20:00:00Z,46089,230,4.0,5.0,,,,,1021.1,7.8,,7.8,,,,3.1,2.6\n" + //
            "-125.76,45.908,0.0,2022-01-18T20:10:00Z,46089,220,4.0,4.0,,,,,1021.2,7.8,9.8,7.8,,,,2.6,3.1\n" + //
            "-125.76,45.908,0.0,2022-01-18T20:40:00Z,46089,220,3.0,5.0,,,8.8,248,1020.8,8.0,9.8,8.0,,,,1.9,2.3\n";
    // "-123.167,48.333,0.0,2022-10-14T20:20:00Z,46088,,0.0,1.0,0.1,,4.6,,1017.8,11.8,10.7,10.8,,,,,\n"
    // +
    // "-123.167,48.333,0.0,2022-10-14T20:30:00Z,46088,,0.0,1.0,,,,,1017.7,12.2,10.6,10.8,,,,,\n"
    // +
    // "-123.167,48.333,0.0,2022-10-14T20:40:00Z,46088,,0.0,1.0,,,,,1017.5,12.3,10.7,10.7,,,,,\n";
    po = fullResults.indexOf(expected.substring(0, 40));
    if (po < 0)
      String2.log(fullResults.substring(fullResults.length() - 400) +
          "Not found: " + expected.substring(0, 40));
    results = fullResults.substring(po, Math.min(fullResults.length(), po + expected.length()));
    Test.ensureEqual(results, expected, "results=\n" + results);

    String2.log("testNrt46088 was successful");
  }

  /**
   * This reads the Historical RCPT2.nc file and makes sure it has the right info.
   */
  static void testHistoricalRCPT2Nc(String ndbcHistoricalNcDir) throws Exception {
    String2.log("\n*** testHistoricalRCPT2Nc");

    Table table = new Table();
    // this converts fake mv e.g., -9999999.0f to standard mv e.g., Float.NaN
    table.read4DNc(ndbcHistoricalNcDir + "NDBC_RCPT2_met.nc", null, -1, NdbcMetStation.ID_NAME, NdbcMetStation.idIndex); // standardizeWhat=-1

    String fullResults;
    String results, expected;
    int po;

    // Note that VIS converted from nautical miles to km: old*kmPerNMile;
    // and TIDE converted from feet to meters: old*Math2.meterPerFoot
    // String2.log(table.toString(5));

    // !!! CONVERT TIME TO STRING to simplify testing
    table.convertEpochSecondsColumnToIso8601(NdbcMetStation.timeIndex);

    fullResults = table.dataToString();
    // first rows from first yearly monthly file x: e.g.,
    // /u00/data/points/ndbcMet2HistoricalTxt/rcpt2h2008.txt
    // #YY MM DD hh mm WDIR WSPD GST WVHT DPD APD MWD PRES ATMP WTMP DEWP VIS TIDE
    // #yr mo dy hr mn degT m/s m/s m sec sec deg hPa degC degC degC nmi ft
    // 2008 09 11 13 36 86 4.1 5.4 99.00 99.00 99.00 999 1010.8 28.6 30.0 999.0 99.0
    // 99.00
    // 2008 09 11 13 42 88 3.6 5.4 99.00 99.00 99.00 999 1010.8 28.6 30.0 999.0 99.0
    // 99.00
    // 2008 09 11 13 48 94 3.5 5.4 99.00 99.00 99.00 999 1010.8 28.7 30.0 999.0 99.0
    // 99.00
    expected = "LON,LAT,DEPTH,TIME,ID,WD,WSPD,GST,WVHT,DPD,APD,MWD,BAR,ATMP,WTMP,DEWP,VIS,PTDY,TIDE,WSPU,WSPV\n" +
        "-97.047,28.022,0.0,2008-09-11T13:36:00Z,RCPT2,86,4.1,5.4,,,,,1010.8,28.6,30.0,,,,,-4.1,-0.3\n" +
        "-97.047,28.022,0.0,2008-09-11T13:42:00Z,RCPT2,88,3.6,5.4,,,,,1010.8,28.6,30.0,,,,,-3.6,-0.1\n" +
        "-97.047,28.022,0.0,2008-09-11T13:48:00Z,RCPT2,94,3.5,5.4,,,,,1010.8,28.7,30.0,,,,,-3.5,0.2\n";
    results = fullResults.substring(0, expected.length());
    Test.ensureEqual(results, expected, "results=\n" + results);

    // UPDATE_EACH_MONTH
    // 1) copy last rows from latest monthly file x:
    // https://www.ndbc.noaa.gov/data/stdmet/Sep/rcpt2.txt !but change 3-letter
    // month each month
    // or e.g., /u00/data/points/ndbcMet2HistoricalTxt/rcpt2 x 2020.txt
    // 2022 09 30 23 42 92 3.5 6.0 99.00 99.00 99.00 999 1011.4 28.0 29.1 999.0 99.0
    // 99.00
    // 2022 09 30 23 48 89 2.3 5.9 99.00 99.00 99.00 999 1011.4 27.7 29.2 999.0 99.0
    // 99.00
    // 2022 09 30 23 54 102 5.3 6.3 99.00 99.00 99.00 999 1011.4 27.3 29.1 999.0
    // 99.0 99.00

    // 2) change date each month to first time from above
    po = fullResults.indexOf("2021-12-31T23:42");
    if (po < 0)
      String2.log("end of fullResults:\n" + fullResults.substring(fullResults.length() - 280));
    results = fullResults.substring(po - 19);
    expected =
        // 3) run the test. Copy/paste results. verify that they match values in file
        // (above)
        "-97.047,28.022,0.0,2021-12-31T23:42:00Z,RCPT2,153,8.5,10.3,,,,,1005.4,23.3,24.0,,,,,-3.9,7.6\n" + //
            "-97.047,28.022,0.0,2021-12-31T23:48:00Z,RCPT2,153,7.8,9.7,,,,,1005.4,23.3,24.0,,,,,-3.5,6.9\n" + //
            "-97.047,28.022,0.0,2021-12-31T23:54:00Z,RCPT2,153,8.5,9.6,,,,,1005.4,23.3,24.0,,,,,-3.9,7.6\n";
    // "-97.047,28.022,0.0,2022-09-30T23:42:00Z,RCPT2,92,3.5,6.0,,,,,1011.4,28.0,29.1,,,,,-3.5,0.1\n"
    // +
    // "-97.047,28.022,0.0,2022-09-30T23:48:00Z,RCPT2,89,2.3,5.9,,,,,1011.4,27.7,29.2,,,,,-2.3,0.0\n"
    // +
    // "-97.047,28.022,0.0,2022-09-30T23:54:00Z,RCPT2,102,5.3,6.3,,,,,1011.4,27.3,29.1,,,,,-5.2,1.1\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    String2.log("testHistoricalRCPT2 was successful");
  }

  /**
   * This reads the Nrt RCPT2.nc file and makes sure it has the right info.
   */
  void testNrtRCPT2Nc(String ndbcNrtNcDir) throws Exception {
    // String2.log("\n*** testNrtRCPT2Nc");

    Table table = new Table();
    // this converts fake mv e.g., -9999999.0f to standard mv e.g., Float.NaN
    table.read4DNc(ndbcNrtNcDir + "NDBC_RCPT2_met.nc", null, -1, NdbcMetStation.ID_NAME, NdbcMetStation.idIndex); // standardizeWhat=-1
    table.convertEpochSecondsColumnToIso8601(NdbcMetStation.timeIndex);
    String results, expected;
    String fullResults;
    int po;

    // Note that VIS converted from nautical miles to km: old*kmPerNMile;
    // and TIDE converted from feet to meters: old*Math2.meterPerFoot

    fullResults = table.dataToString();

    // UPDATE_EACH_MONTH
    // 1) copy first post-historical rows from 45day file RCPT2.txt
    // /u00/data/points/ndbcMet2_45DayTxt/RCPT2.txt
    // or https://www.ndbc.noaa.gov/data/realtime2/RCPT2.txt //45 day
    // Copied rows are in opposite order to expected.

    // 2022 10 01 00 12 100 4.6 6.2 MM MM MM MM 1011.4 26.4 29.2 MM MM MM MM
    // 2022 10 01 00 06 110 4.1 6.7 MM MM MM MM 1011.4 26.6 29.2 MM MM MM MM
    // 2022 10 01 00 00 90 4.6 6.7 MM MM MM MM 1011.4 27.2 29.1 MM MM MM MM

    // 2) Run the test to get the actual expected content and paste it below
    // 3) Rerun the test
    expected = "LON,LAT,DEPTH,TIME,ID,WD,WSPD,GST,WVHT,DPD,APD,MWD,BAR,ATMP,WTMP,DEWP,VIS,PTDY,TIDE,WSPU,WSPV\n" +
        "-97.047,28.022,0.0,2022-01-01T00:00:00Z,RCPT2,150,8.8,9.8,,,,,1005.3,23.3,24.0,,,-0.9,,-4.4,7.6\n" + //
        "-97.047,28.022,0.0,2022-01-01T00:06:00Z,RCPT2,160,8.2,10.3,,,,,1005.3,23.3,24.1,,,,,-2.8,7.7\n" + //
        "-97.047,28.022,0.0,2022-01-01T00:12:00Z,RCPT2,160,8.2,9.8,,,,,1005.2,23.3,24.1,,,,,-2.8,7.7\n";
    // "-97.047,28.022,0.0,2022-10-01T00:00:00Z,RCPT2,90,4.6,6.7,,,,,1011.4,27.2,29.1,,,,,-4.6,0.0\n"
    // +
    // "-97.047,28.022,0.0,2022-10-01T00:06:00Z,RCPT2,110,4.1,6.7,,,,,1011.4,26.6,29.2,,,,,-3.9,1.4\n"
    // +
    // "-97.047,28.022,0.0,2022-10-01T00:12:00Z,RCPT2,100,4.6,6.2,,,,,1011.4,26.4,29.2,,,,,-4.5,0.8\n";
    results = fullResults.substring(0, expected.length());
    Test.ensureEqual(results, expected, "results=\n" + results);

    // 4) copy most recent times from that 45day file RCPT2.txt
    // 2022 10 14 20 18 110 3.6 4.6 MM MM MM MM MM MM 29.7 MM MM MM MM
    // 2022 10 14 20 12 110 3.6 5.1 MM MM MM MM MM MM 29.7 MM MM MM MM
    // 2022 10 14 20 06 110 4.1 5.7 MM MM MM MM MM MM 29.7 MM MM MM MM

    // 5) Put correct 3rd-from-last date/time on first row
    // 6) Run the test to get the actual expected content and paste it below
    // 7) Rerun the test
    expected = "-97.047,28.022,0.0,2022-01-18T19:54:00Z,RCPT2,170,5.7,6.7,,,,,1015.0,18.1,16.9,,,,,-1.0,5.6\n" + //
        "-97.047,28.022,0.0,2022-01-18T20:00:00Z,RCPT2,170,5.1,5.7,,,,,1014.9,18.1,17.0,,,-3.6,,-0.9,5.0\n" + //
        "-97.047,28.022,0.0,2022-01-18T20:30:00Z,RCPT2,160,5.1,6.2,,,,,1014.5,18.5,17.1,,,,,-1.7,4.8\n";
    // "-97.047,28.022,0.0,2022-10-14T20:06:00Z,RCPT2,110,4.1,5.7,,,,,,,29.7,,,,,-3.9,1.4\n"
    // +
    // "-97.047,28.022,0.0,2022-10-14T20:12:00Z,RCPT2,110,3.6,5.1,,,,,,,29.7,,,,,-3.4,1.2\n"
    // +
    // "-97.047,28.022,0.0,2022-10-14T20:18:00Z,RCPT2,110,3.6,4.6,,,,,,,29.7,,,,,-3.4,1.2\n";
    po = fullResults.indexOf(expected.substring(0, 39));
    if (po < 0)
      String2.log(fullResults.substring(fullResults.length() - 400) +
          "Not found: " + expected.substring(0, 39));
    results = fullResults.substring(po, Math.min(fullResults.length(), po + expected.length()));
    Test.ensureEqual(results, expected, "results=\n" + results);

    String2.log("testNrtRCPT2 was successful");
  }

  /**
   * This reads the 46088.nc file and makes sure it was updated correctly
   * by addLastNDays.
   */
  void test46089AddLastNDaysNc(String ndbcNrtNcDir) throws Exception {
    // String2.log("\n*** test46088AddLastNDaysNc");
    Table table = new Table();
    // this converts fake mv e.g., -9999999.0f to standard mv e.g., Float.NaN
    table.read4DNc(ndbcNrtNcDir + "NDBC_46089_met.nc", null, -1, NdbcMetStation.ID_NAME, NdbcMetStation.idIndex); // standardizeWhat=-1

    test46089AddLastNDays(table);
  }

  /**
   * This reads the 46088.nc file and makes sure it was updated correctly
   * by addLastNDays.
   */
  void test46089AddLastNDays(Table table) throws Exception {

    table.convertEpochSecondsColumnToIso8601(NdbcMetStation.timeIndex);
    String fullResults = table.dataToString(Integer.MAX_VALUE);
    String results, expected;
    int po;

    // Note that VIS converted from nautical miles to km: old*kmPerNMile;
    // and TIDE converted from feet to meters: old*Math2.meterPerFoot

    // !!!***SPECIAL UPDATE EACH MONTH -- after separateFiles made
    // 1) Copy first 3 NRT (beginning of month) rows of
    // https://www.ndbc.noaa.gov/data/realtime2/46088.txt here
    // BUT: 2021 copy earliest data in file because currently no monthly 46088
    // files.
    // 2022 10 01 00 20 280 1.0 2.0 0.1 MM 3.5 MM 1019.7 12.0 10.9 11.1 MM MM MM
    // 2022 10 01 00 10 260 1.0 2.0 0.1 MM 3.5 MM 1019.8 12.0 10.6 11.5 MM MM MM
    // 2022 10 01 00 00 270 2.0 2.0 MM MM MM MM 1019.8 11.7 10.6 11.4 MM MM MM

    // 3) Run the test to get the actual expected content and paste it below
    // 4) Verify that the numbers below are match the numbers above.
    // 5) Rerun the test
    expected = "LON,LAT,DEPTH,TIME,ID,WD,WSPD,GST,WVHT,DPD,APD,MWD,BAR,ATMP,WTMP,DEWP,VIS,PTDY,TIDE,WSPU,WSPV\n" +
        "-125.76,45.908,0.0,2022-01-01T00:00:00Z,46089,10,5.0,7.0,,,,,1019.2,5.3,,-2.8,,,,-0.9,-4.9\n" + //
        "-125.76,45.908,0.0,2022-01-01T00:10:00Z,46089,10,5.0,7.0,,,,,1019.3,5.3,9.1,-2.4,,,,-0.9,-4.9\n" + //
        "-125.76,45.908,0.0,2022-01-01T00:20:00Z,46089,20,5.0,7.0,,,,,1019.3,5.3,9.1,-2.1,,,,-1.7,-4.7\n";
    results = fullResults.substring(0, expected.length());
    Test.ensureEqual(results, expected, "fullResults=\n" + results);

    // !!!***SPECIAL UPDATE EACH MONTH -- after separateFiles made
    // a time point in the 5 day file AFTER the last 45 day time

    // 1) copy first 3 rows (last 3 times) of
    // https://www.ndbc.noaa.gov/data/realtime2/46088.txt here
    // #YY MM DD hh mm WDIR WSPD GST WVHT DPD APD MWD PRES ATMP WTMP DEWP VIS PTDY
    // TIDE
    // #yr mo dy hr mn degT m/s m/s m sec sec degT hPa degC degC degC mi hPa ft
    // 2022 10 14 21 10 MM 0.0 1.0 MM MM MM MM 1017.0 13.8 10.7 10.3 MM MM MM
    // 2022 10 14 21 00 MM 0.0 1.0 MM MM MM MM 1017.2 12.9 10.7 10.4 MM MM MM
    // 2022 10 14 20 50 180 1.0 1.0 0.1 MM 3.9 MM 1017.3 12.3 10.7 10.5 MM MM MM

    expected =
        // 2) Put 3rd to last date/time on first row below
        // 3) Run the test to get the actual expected content and paste it below
        // 4) Verify that the numbers below are match the numbers above.
        // 5) Rerun the test
        "-125.76,45.908,0.0,2022-01-18T19:50:00Z,46089,230,3.0,5.0,2.1,11.0,8.6,248,1021.0,7.8,9.8,7.8,,,,2.3,1.9\n" + //
            "-125.76,45.908,0.0,2022-01-18T20:00:00Z,46089,230,4.0,5.0,,,,,1021.1,7.8,,7.8,,,,3.1,2.6\n" + //
            "-125.76,45.908,0.0,2022-01-18T20:10:00Z,46089,220,4.0,4.0,,,,,1021.2,7.8,9.8,7.8,,,,2.6,3.1\n" + //
            "-125.76,45.908,0.0,2022-01-18T20:40:00Z,46089,220,3.0,5.0,,,8.8,248,1020.8,8.0,9.8,8.0,,,,1.9,2.3\n";
    // "-123.167,48.333,0.0,2022-10-14T20:20:00Z,46088,,0.0,1.0,0.1,,4.6,,1017.8,11.8,10.7,10.8,,,,,\n"
    // +
    // "-123.167,48.333,0.0,2022-10-14T20:30:00Z,46088,,0.0,1.0,,,,,1017.7,12.2,10.6,10.8,,,,,\n"
    // +
    // "-123.167,48.333,0.0,2022-10-14T20:40:00Z,46088,,0.0,1.0,,,,,1017.5,12.3,10.7,10.7,,,,,\n"
    // +
    // "-123.167,48.333,0.0,2022-10-14T21:10:00Z,46088,,0.0,1.0,,,,,1017.0,13.8,10.7,10.3,,,,,\n";
    po = fullResults.indexOf(expected.substring(0, 40));
    if (po < 0)
      Test.error("end of results:\n" + fullResults.substring(fullResults.length() - 400) +
          "...\nstart of expected string not found.");
    results = fullResults.substring(po, Math.min(fullResults.length(), po + expected.length()));
    Test.ensureEqual(results, expected, "fullResults(po)=\n" + fullResults.substring(po));

    String2.log("test46088AddLastNDays was successful");
  }

  /**
   * This reads the RCPT2.nc file and makes sure it was updated correctly
   * by addLastNDays.
   */
  void testRCPT2AddLastNDaysNc(String ndbcNrtNcDir) throws Exception {
    String2.log("\n*** testRCPT2AddLastNDaysNc");
    Table table = new Table();
    // this converts fake mv e.g., -9999999.0f to standard mv e.g., Float.NaN
    table.read4DNc(ndbcNrtNcDir + "NDBC_RCPT2_met.nc", null, -1, NdbcMetStation.ID_NAME, NdbcMetStation.idIndex); // standardizeWhat=-1

    testRCPT2AddLastNDays(table);
  }

  /**
   * This reads the RCPT2.nc file and makes sure it was updated correctly
   * by addLastNDays.
   */
  void testRCPT2AddLastNDays(Table table) throws Exception {

    table.convertEpochSecondsColumnToIso8601(NdbcMetStation.timeIndex);
    String fullResults = table.dataToString(Integer.MAX_VALUE);
    String results, expected;
    int po;

    // Note that VIS converted from nautical miles to km: old*kmPerNMile;
    // and TIDE converted from feet to meters: old*Math2.meterPerFoot

    // !!!***SPECIAL UPDATE_EACH_MONTH -- after separateFiles made
    // 1) Copy first 3 rows (start of month) of
    // https://www.ndbc.noaa.gov/data/realtime2/RCPT2.txt here
    // #YY MM DD hh mm WDIR WSPD GST WVHT DPD APD MWD PRES ATMP WTMP DEWP VIS PTDY
    // TIDE
    // #yr mo dy hr mn degT m/s m/s m sec sec degT hPa degC degC degC mi hPa ft
    // 2022 10 01 00 12 100 4.6 6.2 MM MM MM MM 1011.4 26.4 29.2 MM MM MM MM
    // 2022 10 01 00 06 110 4.1 6.7 MM MM MM MM 1011.4 26.6 29.2 MM MM MM MM
    // 2022 10 01 00 00 90 4.6 6.7 MM MM MM MM 1011.4 27.2 29.1 MM MM MM MM

    // 3) Run the test to get the actual expected content and paste it below
    // 4) Verify that the numbers below are match the numbers above.
    // 5) Rerun the test
    expected = "LON,LAT,DEPTH,TIME,ID,WD,WSPD,GST,WVHT,DPD,APD,MWD,BAR,ATMP,WTMP,DEWP,VIS,PTDY,TIDE,WSPU,WSPV\n" +
        "-97.047,28.022,0.0,2022-01-01T00:00:00Z,RCPT2,150,8.8,9.8,,,,,1005.3,23.3,24.0,,,-0.9,,-4.4,7.6\n" + //
        "-97.047,28.022,0.0,2022-01-01T00:06:00Z,RCPT2,160,8.2,10.3,,,,,1005.3,23.3,24.1,,,,,-2.8,7.7\n" + //
        "-97.047,28.022,0.0,2022-01-01T00:12:00Z,RCPT2,160,8.2,9.8,,,,,1005.2,23.3,24.1,,,,,-2.8,7.7\n";
    // "-97.047,28.022,0.0,2022-10-01T00:00:00Z,RCPT2,90,4.6,6.7,,,,,1011.4,27.2,29.1,,,,,-4.6,0.0\n"
    // +
    // "-97.047,28.022,0.0,2022-10-01T00:06:00Z,RCPT2,110,4.1,6.7,,,,,1011.4,26.6,29.2,,,,,-3.9,1.4\n"
    // +
    // "-97.047,28.022,0.0,2022-10-01T00:12:00Z,RCPT2,100,4.6,6.2,,,,,1011.4,26.4,29.2,,,,,-4.5,0.8\n";
    results = fullResults.substring(0, expected.length());
    Test.ensureEqual(results, expected, "fullResults=\n" + fullResults);

    // !!!***SPECIAL UPDATE_EACH_MONTH -- after separateFiles made
    // a time point in the 5 day file AFTER the last 45 day time
    // #YY MM DD hh mm WDIR WSPD GST WVHT DPD APD MWD PRES ATMP WTMP DEWP VIS PTDY
    // TIDE
    // #yr mo dy hr mn degT m/s m/s m sec sec degT hPa degC degC degC mi hPa ft
    // 1) put the most recent time's data from
    // https://www.ndbc.noaa.gov/data/realtime2/RCPT2.txt here
    // 2022 10 14 20 48 110 4.1 6.2 MM MM MM MM MM MM 29.8 MM MM MM MM
    // 2022 10 14 20 42 120 4.1 5.7 MM MM MM MM MM MM 29.7 MM MM MM MM
    // 2022 10 14 20 36 120 3.6 5.1 MM MM MM MM MM MM 29.6 MM MM MM MM

    expected =
        // 2) Put correct last date/time on first row
        // 3) Run the test to get the actual expected content and paste it below
        // 4) Verify that the numbers below are match the numbers above.
        // 5) Rerun the test
        // TROUBLE: different source files seem to have different time points, so
        // results here are often different/incomplete!
        "-97.047,28.022,0.0,2022-01-18T19:48:00Z,RCPT2,170,6.2,6.7,,,,,1015.1,18.4,16.9,,,,,-1.1,6.1\n" + //
            "-97.047,28.022,0.0,2022-01-18T19:54:00Z,RCPT2,170,5.7,6.7,,,,,1015.0,18.1,16.9,,,,,-1.0,5.6\n" + //
            "-97.047,28.022,0.0,2022-01-18T20:00:00Z,RCPT2,170,5.1,5.7,,,,,1014.9,18.1,17.0,,,-3.6,,-0.9,5.0\n" + //
            "-97.047,28.022,0.0,2022-01-18T20:30:00Z,RCPT2,160,5.1,6.2,,,,,1014.5,18.5,17.1,,,,,-1.7,4.8\n";
    // "-97.047,28.022,0.0,2022-10-14T20:24:00Z,RCPT2,120,3.6,5.1,,,,,1011.8,,29.6,,,,,-3.1,1.8\n"
    // +
    // "-97.047,28.022,0.0,2022-10-14T20:30:00Z,RCPT2,120,4.1,5.7,,,,,1011.8,,29.6,,,,,-3.6,2.0\n"
    // +
    // "-97.047,28.022,0.0,2022-10-14T20:48:00Z,RCPT2,110,4.1,6.2,,,,,,,29.8,,,,,-3.9,1.4\n";
    po = Math.max(0, fullResults.indexOf(expected.substring(0, 40)));
    results = fullResults.substring(po, Math.min(fullResults.length(), po + expected.length()));
    Test.ensureEqual(results, expected, "fullResults=\n" + fullResults);
    String2.log("testRCPT2AddLastNDays was successful");
  }

  @org.junit.jupiter.api.Test
  @TagLargeFiles
  void testAddLastMode() throws Exception {
    String observationDir = Path.of(NdbcMetStationTests.class.getResource("/veryLarge/").toURI()).toString();
    // String ndbcHistoricalNcDir = observationDir + "ndbcMet2/historical/";
    String ndbcNrtNcDir = observationDir + "ndbcMet2/nrt/";
    String logDir = observationDir + "ndbcMet2Logs/";
    // String ndbcStationHtmlDir = observationDir + "ndbcMet2StationHtml/";
    // String ndbcHistoricalTxtDir = observationDir + "ndbcMet2HistoricalTxt/";
    // String ndbc45DayTxtDir = observationDir + "ndbcMet2_45DayTxt/";
    // double firstNrtSeconds =
    // Calendar2.isoStringToEpochSeconds(NdbcMetStation.firstNearRealTimeData);

    // verbose = true;
    // Table.verbose = true;
    // Table.reallyVerbose = true;
    // oneTime();

    // open a log file
    String dateTime = Calendar2.formatAsCompactDateTime(Calendar2.newGCalendarLocal());
    // logs written to file with HHmm.
    // So if this is run every 5 minutes, this makes 24*12 files/day, overwritten
    // ever day.
    String2.setupLog(true, false, logDir + "log" + dateTime.substring(8, 12) + ".txt", // HHmm
        false, String2.logFileDefaultMaxSize); // append
    String2.log("*** Starting NdbcMetStation " +
        Calendar2.getCurrentISODateTimeStringLocalTZ() + "\n" +
        "logFile=" + String2.logFileName() + "\n" +
        String2.standardHelpAboutMessage());
    long time = System.currentTimeMillis();

    // addLast mode
    NdbcMetStation.addLatestObsData(ndbcNrtNcDir, false); // 2020-02-05 was addLastNDaysInfo(ndbcNrtNcDir, 5, false);
                                                          // //5 or 45, testMode=false
    String2.log("\n*** NdbcMetStation.main addLast finished successfully in " +
        Calendar2.elapsedTimeString(System.currentTimeMillis() - time));
    String2.returnLoggingToSystemOut();
  }

  /**
   * This is used for maintenance and testing of this class.
   */
  @org.junit.jupiter.api.Test
  @TagLargeFiles
  void testHistorical() throws Exception {

    String observationDir = Path.of(NdbcMetStationTests.class.getResource("/veryLarge/").toURI()).toString();
    String ndbcHistoricalNcDir = observationDir + "/ndbcMet2/historical/";
    String ndbcNrtNcDir = observationDir + "/ndbcMet2/nrt/";
    String logDir = observationDir + "/ndbcMet2Logs/";
    // String ndbcStationHtmlDir = observationDir + "ndbcMet2StationHtml/";
    // String ndbcHistoricalTxtDir = observationDir + "ndbcMet2HistoricalTxt/";
    // String ndbc45DayTxtDir = observationDir + "ndbcMet2_45DayTxt/";
    // double firstNrtSeconds =
    // Calendar2.isoStringToEpochSeconds(NdbcMetStation.firstNearRealTimeData);

    // verbose = true;
    // Table.verbose = true;
    // Table.reallyVerbose = true;
    // oneTime();

    // open a log file
    String dateTime = Calendar2.formatAsCompactDateTime(Calendar2.newGCalendarLocal());
    // logs written to file with HHmm.
    // So if this is run every 5 minutes, this makes 24*12 files/day, overwritten
    // ever day.
    String2.setupLog(true, false, logDir + "log" + dateTime.substring(8, 12) + ".txt", // HHmm
        false, String2.logFileDefaultMaxSize); // append
    String2.log("*** Starting NdbcMetStation " +
        Calendar2.getCurrentISODateTimeStringLocalTZ() + "\n" +
        "logFile=" + String2.logFileName() + "\n" +
        String2.standardHelpAboutMessage());
    long time = System.currentTimeMillis();

    // MONTHLY UPDATE PROCEDURE (done on/after 25th of month).
    // 1) *** CHANGE firstNearRealTimeData STRING AT TOP OF FILE!

    // 2) *** get new historical files
    // historical yearly files are from:
    // https://www.ndbc.noaa.gov/data/historical/stdmet/
    // monthly dirs: https://www.ndbc.noaa.gov/data/stdmet/
    // (Once a year ~Feb 20, the new yearly files appear
    // and monthly files disappear (except Jan, which are now from the new year).
    // copy last year's monthly files with DOS window:
    // cd /u00/data/points
    // md ndbcMet2HistoricalTxt2021 (last year)
    // copy ndbcMet2HistoricalTxt\*2021.txt ndbcMet2HistoricalTxt2021
    // del ndbcMet2HistoricalTxt\*2021.txt
    // change HISTORICAL_FILES_CURRENT_YEAR at top of file to the current year,
    // then follow normal update procedure.)
    // 2011-02-28 I re-downloaded ALL of the files (since previous years had been
    // modified).
    // I also re-downloaded ALL of the station html files (by renaming previous
    // ndbcMetStationHtml dir to Retired and making a new dir).
    // historical monthly files are from: https://www.ndbc.noaa.gov/data/stdmet/
    // <month3Letter>/ e.g., Jan
    // !!!!**** Windows GUI My Computer doesn't show all the files in the directory!
    // Use DOS window "dir" or Linux ls instead of the GUI.
    // downloadNewHistoricalTxtFiles(ndbcHistoricalTxtDir); // ~10 minutes (2020:
    // yearly faster now thanks to Akamai(?) and .gz'd files)
    // String2.pressEnterToContinue();
    // if (true) return;

    // 3) *** Make the historical nc files
    // !!!!**** EACH MONTH, SOME TESTS NEED UPDATING: SEE "UPDATE_EACH_MONTH"
    // no station info for a station? search for "no station info" above
    // or lat lon available? search for "get the lat and lon" above
    // .txt file is gibberish? usually it is .gz but not labeled such:
    // so in /u00/data/points/ndbcMetHistoricalTxt change the extention to .gz,
    // use git bash to gunzip it, then reprocess that station.
    boolean testMode = true; // I usually run 'true' then 'false'
    String ignoreStationsBefore = " "; // use " " to process all stations or lowercase characters to start in middle
    // makeStationNcFiles(true, firstNrtSeconds, //historicalMode?
    // ndbcStationHtmlDir, ndbcHistoricalTxtDir, ndbc45DayTxtDir,
    // ndbcHistoricalNcDir, ndbcNrtNcDir, ignoreStationsBefore,
    // testMode); //4 hrs with new high res data, was M4700 ~2 hrs, was ~3 hrs
    testHistorical46088Nc(ndbcHistoricalNcDir); // !!!!**** EACH MONTH, THIS TEST NEED UPDATING
    testHistoricalRCPT2Nc(ndbcHistoricalNcDir); // !!!!**** EACH MONTH, THIS TEST NEED UPDATING

    // 4) *** get latest 45 day files
    // DON'T download45DayTextFiles after 45 days after last historicalTxt date.
    // download45DayTxtFiles(ndbc45DayTxtDir); // ~10 minutes (2020: faster now
    // thanks to Akamai(?) and/or .gz )

    // 5) *** Make the nrt nc files
    testMode = true; // I usually run 'true' then 'false'
    ignoreStationsBefore = " "; // use " " to process all stations or lowercase characters to start in middle
    // makeStationNcFiles(false, firstNrtSeconds, //historicalMode?
    // ndbcStationHtmlDir, ndbcHistoricalTxtDir, ndbc45DayTxtDir,
    // ndbcHistoricalNcDir, ndbcNrtNcDir, ignoreStationsBefore,
    // testMode); //4 minutes
    testNrt46089Nc(ndbcNrtNcDir); // !!!!**** EACH MONTH, THIS TEST NEED UPDATING
    testNrtRCPT2Nc(ndbcNrtNcDir); // !!!!**** EACH MONTH, THIS TEST NEED UPDATING

    // 6) *** addLastNDaysInfo
    // Doing this isn't (strictly-speaking) needed for the monthly reprocessing of
    // the ndbc data
    // because the updating script running on coastwatch will do it.
    // This does last hour's data from 1 source file (from 5 or 45 day file if
    // needed)
    testMode = true; // do true first, then false
    // addLatestObsData(ndbcNrtNcDir, testMode); //3 minutes on my PC (if done
    // recently)
    // was addLastNDaysInfo(ndbcNrtNcDir, 5, testMode); //5 or 45
    test46089AddLastNDaysNc(ndbcNrtNcDir); // !!!!**** EACH MONTH, THIS TEST NEED UPDATING
    testRCPT2AddLastNDaysNc(ndbcNrtNcDir); // !!!!**** EACH MONTH, THIS TEST NEED UPDATING

    /*
     * 7) *** On LAPTOP:
     * use git bash:
     * rename ndbcMet2 to ndbcMet2t, then
     * cd /c/u00/data/points/
     * tar zcvf ndbcMet2t.tgz ndbcMet2t
     * ftp ndbcMet2t.tgz to coastwatch's /u00/data/points
     * On coastwatch SERVER:
     * cd /u00/data/points
     * tar zxvf ndbcMet2t.tgz
     * then rename ndbcMet2 ndbcMet2R20150224 ndbcMet2
     * then rename ndbcMet2t ndbcMet2 ndbcMet2t
     * rm ndbcMet2t.tgz
     */

    // 8) On PC, rename ndbcMet2t.tgz to ndbcMet2tYYYYMMDD.tgz

    // 9) *** In datasetsFEDCW.xml and datasets2.xml,
    // change the 2 'historic' dates in summary attribute for datasetID=cwwcNDBCMet
    // to reflect new transition date.
    // * On laptop, use updateDatasetsXml.py (EditPlus Python tools #2)
    // * Copy datasetsFEDCW.xml to coastwatch rename to put in place
    // * Set cwwcNDBCMet flag.

    // 10) *** test cwwcNDBCMet sometimes:
    // * Run TestAll: String2.log(EDD.testDasDds("cwwcNDBCMet"));
    // to see if trouble.

    String2.log("\n*** NdbcMetStation.main finished successfully in " +
        Calendar2.elapsedTimeString(System.currentTimeMillis() - time));
    String2.returnLoggingToSystemOut();

  }
}
