*GLOBAL*,Conventions,"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.2"
*GLOBAL*,cdm_data_type,TimeSeries
*GLOBAL*,cdm_timeseries_variables,"station, longitude, latitude"
*GLOBAL*,contributor_name,NOAA NDBC
*GLOBAL*,contributor_role,Source of data.
*GLOBAL*,creationTimeMillis,"1747835216049"
*GLOBAL*,creator_email,erd.data@noaa.gov
*GLOBAL*,creator_name,NOAA NMFS SWFSC ERD
*GLOBAL*,creator_type,institution
*GLOBAL*,creator_url,https://www.pfeg.noaa.gov
*GLOBAL*,date_created,"2025-05-14"
*GLOBAL*,date_issued,"2025-05-14"
*GLOBAL*,Easternmost_Easting,179.001d
*GLOBAL*,featureType,TimeSeries
*GLOBAL*,geospatial_lat_max,71.758d
*GLOBAL*,geospatial_lat_min,-55.0d
*GLOBAL*,geospatial_lat_units,degrees_north
*GLOBAL*,geospatial_lon_max,179.001d
*GLOBAL*,geospatial_lon_min,-177.75d
*GLOBAL*,geospatial_lon_units,degrees_east
*GLOBAL*,geospatial_vertical_positive,down
*GLOBAL*,geospatial_vertical_units,m
*GLOBAL*,history,"Around the 25th of each month, erd.data@noaa.gov downloads the latest yearly and monthly historical .txt.gz files from https://www.ndbc.noaa.gov/data/historical/stdmet/ and generates one historical .nc file for each station. erd.data@noaa.gov also downloads all of the 45day near real time .txt files from https://www.ndbc.noaa.gov/data/realtime2/ and generates one near real time .nc file for each station.\nEvery 5 minutes, erd.data@noaa.gov downloads the list of latest data from all stations for the last 2 hours from https://www.ndbc.noaa.gov/data/latest_obs/latest_obs.txt and updates the near real time .nc files."
*GLOBAL*,id,cwwcNDBCMet
*GLOBAL*,infoUrl,https://www.ndbc.noaa.gov/
*GLOBAL*,institution,"NOAA NDBC, NOAA NMFS SWFSC ERD"
*GLOBAL*,keywords,"air, air_pressure_at_sea_level, air_temperature, atmosphere, atmospheric, average, boundary, buoy, coastwatch, data, dew point, dew_point_temperature, direction, dominant, Earth Science > Atmosphere > Air Quality > Visibility, Earth Science > Atmosphere > Altitude > Planetary Boundary Layer Height, Earth Science > Atmosphere > Atmospheric Pressure > Atmospheric Pressure Measurements, Earth Science > Atmosphere > Atmospheric Pressure > Pressure Tendency, Earth Science > Atmosphere > Atmospheric Pressure > Sea Level Pressure, Earth Science > Atmosphere > Atmospheric Pressure > Static Pressure, Earth Science > Atmosphere > Atmospheric Temperature > Air Temperature, Earth Science > Atmosphere > Atmospheric Temperature > Dew Point Temperature, Earth Science > Atmosphere > Atmospheric Water Vapor > Dew Point Temperature, Earth Science > Atmosphere > Atmospheric Winds > Surface Winds, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, Earth Science > Oceans > Ocean Waves > Significant Wave Height, Earth Science > Oceans > Ocean Waves > Swells, Earth Science > Oceans > Ocean Waves > Wave Period, eastward, eastward_wind, from, gust, height, identifier, layer, level, measurements, meridional, meteorological, meteorology, name, ndbc, noaa, northward, northward_wind, ocean, oceans, period, planetary, pressure, quality, sea, sea level, sea_surface_swell_wave_period, sea_surface_swell_wave_significant_height, sea_surface_swell_wave_to_direction, sea_surface_temperature, seawater, significant, speed, sst, standard, static, station, surface, surface waves, surface_altitude, swell, swells, temperature, tendency, tendency_of_air_pressure, time, vapor, visibility, visibility_in_air, water, wave, waves, wcn, wind, wind_from_direction, wind_speed, wind_speed_of_gust, winds, zonal"
*GLOBAL*,keywords_vocabulary,GCMD Science Keywords
*GLOBAL*,license,"The data may be used and redistributed for free but is not intended\nfor legal use, since it may contain inaccuracies. Neither the data\nContributor, ERD, NOAA, nor the United States Government, nor any\nof their employees or contractors, makes any warranty, express or\nimplied, including warranties of merchantability and fitness for a\nparticular purpose, or assumes any legal liability for the accuracy,\ncompleteness, or usefulness, of this information."
*GLOBAL*,naming_authority,gov.noaa.pfeg.coastwatch
*GLOBAL*,Northernmost_Northing,71.758d
*GLOBAL*,project,NOAA NDBC and NOAA NMFS SWFSC ERD
*GLOBAL*,publisher_email,erd.data@noaa.gov
*GLOBAL*,publisher_name,NOAA NMFS SWFSC ERD
*GLOBAL*,publisher_type,institution
*GLOBAL*,publisher_url,https://www.pfeg.noaa.gov
*GLOBAL*,quality,Automated QC checks with periodic manual QC
*GLOBAL*,source,station observation
*GLOBAL*,sourceErddapVersion,2.26d
*GLOBAL*,sourceUrl,https://www.ndbc.noaa.gov/
*GLOBAL*,Southernmost_Northing,-55.0d
*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v70
*GLOBAL*,subsetVariables,"station, longitude, latitude"
*GLOBAL*,summary,"The National Data Buoy Center (NDBC) distributes meteorological data from\nmoored buoys maintained by NDBC and others. Moored buoys are the weather\nsentinels of the sea. They are deployed in the coastal and offshore waters\nfrom the western Atlantic to the Pacific Ocean around Hawaii, and from the\nBering Sea to the South Pacific. NDBC's moored buoys measure and transmit\nbarometric pressure; wind direction, speed, and gust; air and sea\ntemperature; and wave energy spectra from which significant wave height,\ndominant wave period, and average wave period are derived. Even the\ndirection of wave propagation is measured on many moored buoys. See\nhttps://www.ndbc.noaa.gov/measdes.shtml for a description of the measurements.\n\nThe source data from NOAA NDBC has different column names, different units,\nand different missing values in different files, and other problems\n(notably, lots of rows with duplicate or different values for the same time\npoint). This dataset is a standardized, reformatted, and lightly edited\nversion of that source data, created by NOAA NMFS SWFSC ERD (email:\nerd.data at noaa.gov). Before 2020-01-29, this dataset only had the data\nthat was closest to a given hour, rounded to the nearest hour. Now, this\ndataset has all of the data available from NDBC with the original time\nvalues. If there are multiple source rows for a given buoy for a given\ntime, only the row with the most non-NaN data values is kept. If there is\na gap in the data, a row of missing values is inserted (which causes a nice\ngap when the data is graphed). Also, some impossible data values are\nremoved, but this data is not perfectly clean. This dataset is now updated\nevery 5 minutes.\n\nThis dataset has both historical data (quality controlled, before\n2023-12-01T00:00:00Z) and near real time data (less quality controlled,\nwhich may change at any time, from 2024-01-01T00:00:00Z on)."
*GLOBAL*,testOutOfDate,now-25minutes
*GLOBAL*,time_coverage_end,2025-05-21T13:40:00Z
*GLOBAL*,time_coverage_start,1970-02-26T20:00:00Z
*GLOBAL*,title,"NDBC Standard Meteorological Buoy Data, 1970-present"
*GLOBAL*,Westernmost_Easting,-177.75d
station,*DATA_TYPE*,String
station,cf_role,timeseries_id
station,comment,The station identifier.
station,ioos_category,Identifier
station,long_name,Station Identifier
longitude,*DATA_TYPE*,float
longitude,_CoordinateAxisType,Lon
longitude,actual_range,-177.75f,179.001f
longitude,axis,X
longitude,colorBarMaximum,180.0d
longitude,colorBarMinimum,-180.0d
longitude,comment,The longitude of the station.
longitude,ioos_category,Location
longitude,long_name,Longitude
longitude,standard_name,longitude
longitude,units,degrees_east
latitude,*DATA_TYPE*,float
latitude,_CoordinateAxisType,Lat
latitude,actual_range,-55.0f,71.758f
latitude,axis,Y
latitude,colorBarMaximum,90.0d
latitude,colorBarMinimum,-90.0d
latitude,comment,The latitude of the station.
latitude,ioos_category,Location
latitude,long_name,Latitude
latitude,standard_name,latitude
latitude,units,degrees_north
time,*DATA_TYPE*,String
time,_CoordinateAxisType,Time
time,actual_range,1970-02-26T20:00:00Z\n2025-05-21T13:40:00Z
time,axis,T
time,ioos_category,Time
time,long_name,Time
time,standard_name,time
time,time_origin,01-JAN-1970 00:00:00
time,units,yyyy-MM-dd'T'HH:mm:ssZ
wd,*DATA_TYPE*,short
wd,_FillValue,32767s
wd,actual_range,0s,359s
wd,colorBarMaximum,360.0d
wd,colorBarMinimum,0.0d
wd,comment,Wind direction (the direction the wind is coming from in degrees clockwise from true N) during the same period used for WSPD.
wd,ioos_category,Wind
wd,long_name,Wind Direction
wd,missing_value,32767s
wd,standard_name,wind_from_direction
wd,units,degrees_true
wspd,*DATA_TYPE*,float
wspd,_FillValue,-9999999.0f
wspd,actual_range,0.0f,96.0f
wspd,colorBarMaximum,15.0d
wspd,colorBarMinimum,0.0d
wspd,comment,Average wind speed (m/s).
wspd,ioos_category,Wind
wspd,long_name,Wind Speed
wspd,missing_value,-9999999.0f
wspd,standard_name,wind_speed
wspd,units,m s-1
gst,*DATA_TYPE*,float
gst,_FillValue,-9999999.0f
gst,actual_range,0.0f,75.5f
gst,colorBarMaximum,30.0d
gst,colorBarMinimum,0.0d
gst,comment,Peak 5 or 8 second gust speed (m/s).
gst,ioos_category,Wind
gst,long_name,Wind Gust Speed
gst,missing_value,-9999999.0f
gst,standard_name,wind_speed_of_gust
gst,units,m s-1
wvht,*DATA_TYPE*,float
wvht,_FillValue,-9999999.0f
wvht,actual_range,0.0f,92.39f
wvht,colorBarMaximum,10.0d
wvht,colorBarMinimum,0.0d
wvht,comment,Significant wave height (meters) is calculated as the average of the highest one-third of all of the wave heights during the 20-minute sampling period.
wvht,ioos_category,Surface Waves
wvht,long_name,Wave Height
wvht,missing_value,-9999999.0f
wvht,standard_name,sea_surface_wave_significant_height
wvht,units,m
dpd,*DATA_TYPE*,float
dpd,_FillValue,-9999999.0f
dpd,actual_range,0.0f,64.0f
dpd,colorBarMaximum,20.0d
dpd,colorBarMinimum,0.0d
dpd,comment,Dominant wave period (seconds) is the period with the maximum wave energy.
dpd,ioos_category,Surface Waves
dpd,long_name,"Wave Period, Dominant"
dpd,missing_value,-9999999.0f
dpd,standard_name,sea_surface_swell_wave_period
dpd,units,s
apd,*DATA_TYPE*,float
apd,_FillValue,-9999999.0f
apd,actual_range,0.0f,95.0f
apd,colorBarMaximum,20.0d
apd,colorBarMinimum,0.0d
apd,comment,Average wave period (seconds) of all waves during the 20-minute period.
apd,ioos_category,Surface Waves
apd,long_name,"Wave Period, Average"
apd,missing_value,-9999999.0f
apd,standard_name,sea_surface_swell_wave_period
apd,units,s
mwd,*DATA_TYPE*,short
mwd,_FillValue,32767s
mwd,actual_range,0s,359s
mwd,colorBarMaximum,360.0d
mwd,colorBarMinimum,0.0d
mwd,comment,Mean wave direction corresponding to energy of the dominant period (DOMPD).
mwd,ioos_category,Surface Waves
mwd,long_name,Wave Direction
mwd,missing_value,32767s
mwd,standard_name,sea_surface_wave_to_direction
mwd,units,degrees_true
bar,*DATA_TYPE*,float
bar,_FillValue,-9999999.0f
bar,actual_range,800.7f,1198.8f
bar,colorBarMaximum,1050.0d
bar,colorBarMinimum,950.0d
bar,comment,"Air pressure (hPa). ('PRES' on some NDBC tables.) For C-MAN sites and Great Lakes buoys, the recorded pressure is reduced to sea level using the method described in NWS Technical Procedures Bulletin 291 (11/14/80)."
bar,ioos_category,Pressure
bar,long_name,Air Pressure
bar,missing_value,-9999999.0f
bar,standard_name,air_pressure_at_sea_level
bar,units,hPa
atmp,*DATA_TYPE*,float
atmp,_FillValue,-9999999.0f
atmp,actual_range,-153.4f,50.0f
atmp,colorBarMaximum,40.0d
atmp,colorBarMinimum,-10.0d
atmp,comment,"Air temperature (Celsius). For sensor heights on buoys, see Hull Descriptions. For sensor heights at C-MAN stations, see C-MAN Sensor Locations."
atmp,ioos_category,Temperature
atmp,long_name,Air Temperature
atmp,missing_value,-9999999.0f
atmp,standard_name,air_temperature
atmp,units,degree_C
wtmp,*DATA_TYPE*,float
wtmp,_FillValue,-9999999.0f
wtmp,actual_range,-98.7f,50.0f
wtmp,colorBarMaximum,32.0d
wtmp,colorBarMinimum,0.0d
wtmp,comment,"Sea surface temperature (Celsius). For sensor depth, see Hull Description."
wtmp,ioos_category,Temperature
wtmp,long_name,SST
wtmp,missing_value,-9999999.0f
wtmp,standard_name,sea_surface_temperature
wtmp,units,degree_C
dewp,*DATA_TYPE*,float
dewp,_FillValue,-9999999.0f
dewp,actual_range,-99.9f,48.7f
dewp,colorBarMaximum,40.0d
dewp,colorBarMinimum,0.0d
dewp,comment,Dewpoint temperature taken at the same height as the air temperature measurement.
dewp,ioos_category,Temperature
dewp,long_name,Dewpoint Temperature
dewp,missing_value,-9999999.0f
dewp,standard_name,dew_point_temperature
dewp,units,degree_C
vis,*DATA_TYPE*,float
vis,_FillValue,-9999999.0f
vis,actual_range,0.0f,66.7f
vis,colorBarMaximum,100.0d
vis,colorBarMinimum,0.0d
vis,comment,"Station visibility (km, originally nautical miles in the NDBC .txt files). Note that buoy stations are limited to reports from 0 to 1.6 nmi."
vis,ioos_category,Meteorology
vis,long_name,Station Visibility
vis,missing_value,-9999999.0f
vis,standard_name,visibility_in_air
vis,units,km
ptdy,*DATA_TYPE*,float
ptdy,_FillValue,-9999999.0f
ptdy,actual_range,-14.2f,14.6f
ptdy,colorBarMaximum,3.0d
ptdy,colorBarMinimum,-3.0d
ptdy,comment,Pressure Tendency is the direction (plus or minus) and the amount of pressure change (hPa) for a three hour period ending at the time of observation.
ptdy,ioos_category,Pressure
ptdy,long_name,Pressure Tendency
ptdy,missing_value,-9999999.0f
ptdy,standard_name,tendency_of_air_pressure
ptdy,units,hPa
tide,*DATA_TYPE*,float
tide,_FillValue,-9999999.0f
tide,actual_range,-9.37f,6.15f
tide,colorBarMaximum,5.0d
tide,colorBarMinimum,-5.0d
tide,comment,The water level in meters (originally feet in the NDBC .txt files) above or below Mean Lower Low Water (MLLW).
tide,ioos_category,Sea Level
tide,long_name,Water Level
tide,missing_value,-9999999.0f
tide,standard_name,surface_altitude
tide,units,m
wspu,*DATA_TYPE*,float
wspu,_FillValue,-9999999.0f
wspu,actual_range,-98.7f,97.5f
wspu,colorBarMaximum,15.0d
wspu,colorBarMinimum,-15.0d
wspu,comment,"The zonal wind speed (m/s) indicates the u component of where the wind is going, derived from Wind Direction and Wind Speed."
wspu,ioos_category,Wind
wspu,long_name,"Wind Speed, Zonal"
wspu,missing_value,-9999999.0f
wspu,standard_name,eastward_wind
wspu,units,m s-1
wspv,*DATA_TYPE*,float
wspv,_FillValue,-9999999.0f
wspv,actual_range,-98.7f,97.5f
wspv,colorBarMaximum,15.0d
wspv,colorBarMinimum,-15.0d
wspv,comment,"The meridional wind speed (m/s) indicates the v component of where the wind is going, derived from Wind Direction and Wind Speed."
wspv,ioos_category,Wind
wspv,long_name,"Wind Speed, Meridional"
wspv,missing_value,-9999999.0f
wspv,standard_name,northward_wind
wspv,units,m s-1

*END_METADATA*
station,longitude,latitude,time,wd,wspd,gst,wvht,dpd,apd,mwd,bar,atmp,wtmp,dewp,vis,ptdy,tide,wspu,wspv
*END_DATA*
