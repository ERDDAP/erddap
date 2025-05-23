*GLOBAL*,Conventions,"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.2"
*GLOBAL*,cdm_data_type,TimeSeries
*GLOBAL*,cdm_timeseries_variables,"array, station, wmo_platform_code, longitude, latitude, depth"
*GLOBAL*,creationTimeMillis,"1747835346045"
*GLOBAL*,creator_email,Dai.C.McClurg@noaa.gov
*GLOBAL*,creator_name,GTMBA Project Office/NOAA/PMEL
*GLOBAL*,creator_type,group
*GLOBAL*,creator_url,https://www.pmel.noaa.gov/gtmba/mission
*GLOBAL*,Data_Source,Global Tropical Moored Buoy Array Project Office/NOAA/PMEL
*GLOBAL*,defaultGraphQuery,"longitude,latitude,WU_422,WV_423&orderByMax(""station,time"")&.draw=vectors&.color=0xFF0000&time>=now-7days"
*GLOBAL*,Easternmost_Easting,357.0d
*GLOBAL*,featureType,TimeSeries
*GLOBAL*,File_info,Contact: Dai.C.McClurg@noaa.gov
*GLOBAL*,geospatial_lat_max,21.0d
*GLOBAL*,geospatial_lat_min,-25.0d
*GLOBAL*,geospatial_lat_units,degrees_north
*GLOBAL*,geospatial_lon_max,357.0d
*GLOBAL*,geospatial_lon_min,0.0d
*GLOBAL*,geospatial_lon_units,degrees_east
*GLOBAL*,geospatial_vertical_max,-4.0d
*GLOBAL*,geospatial_vertical_min,-10.0d
*GLOBAL*,geospatial_vertical_positive,down
*GLOBAL*,geospatial_vertical_units,m
*GLOBAL*,history,"This dataset has data from the TAO/TRITON, RAMA, and PIRATA projects.\nThis dataset is a product of the TAO Project Office at NOAA/PMEL.\n2025-05-02 Bob Simons at NOAA/NMFS/SWFSC/ERD (bob.simons@noaa.gov) fully refreshed ERD's copy of this dataset by downloading all of the .cdf files from the PMEL TAO FTP site.  Since then, the dataset has been partially refreshed everyday by downloading and merging the latest version of the last 25 days worth of data."
*GLOBAL*,infoUrl,https://www.pmel.noaa.gov/gtmba/mission
*GLOBAL*,institution,"NOAA PMEL, TAO/TRITON, RAMA, PIRATA"
*GLOBAL*,keywords,"atmosphere, atmospheric, buoys, centered, daily, depth, direction, Earth Science > Atmosphere > Atmospheric Winds > Surface Winds, from, identifier, level, meridional, noaa, pirata, pmel, quality, rama, source, speed, station, surface, tao, time, triton, wind, wind_from_direction, wind_speed, wind_to_direction, winds, x_wind, y_wind, zonal"
*GLOBAL*,keywords_vocabulary,GCMD Science Keywords
*GLOBAL*,license,"Request for Acknowledgement: If you use these data in publications or presentations, please acknowledge the GTMBA Project Office of NOAA/PMEL. Also, we would appreciate receiving a preprint and/or reprint of publications utilizing the data for inclusion in our bibliography. Relevant publications should be sent to: GTMBA Project Office, NOAA/Pacific Marine Environmental Laboratory, 7600 Sand Point Way NE, Seattle, WA 98115\n\nThe data may be used and redistributed for free but is not intended\nfor legal use, since it may contain inaccuracies. Neither the data\nContributor, ERD, NOAA, nor the United States Government, nor any\nof their employees or contractors, makes any warranty, express or\nimplied, including warranties of merchantability and fitness for a\nparticular purpose, or assumes any legal liability for the accuracy,\ncompleteness, or usefulness, of this information."
*GLOBAL*,Northernmost_Northing,21.0d
*GLOBAL*,project,"TAO/TRITON, RAMA, PIRATA"
*GLOBAL*,Request_for_acknowledgement,"If you use these data in publications or presentations, please acknowledge the GTMBA Project Office of NOAA/PMEL. Also, we would appreciate receiving a preprint and/or reprint of publications utilizing the data for inclusion in our bibliography. Relevant publications should be sent to: GTMBA Project Office, NOAA/Pacific Marine Environmental Laboratory, 7600 Sand Point Way NE, Seattle, WA 98115"
*GLOBAL*,sourceErddapVersion,2.26d
*GLOBAL*,sourceUrl,(local files)
*GLOBAL*,Southernmost_Northing,-25.0d
*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v70
*GLOBAL*,subsetVariables,"array, station, wmo_platform_code, longitude, latitude, depth"
*GLOBAL*,summary,"This dataset has daily Wind data from the\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\nhttps://www.pmel.noaa.gov/gtmba/mission ."
*GLOBAL*,testOutOfDate,now-3days
*GLOBAL*,time_coverage_end,2025-05-01T12:00:00Z
*GLOBAL*,time_coverage_start,1977-11-06T12:00:00Z
*GLOBAL*,title,"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Wind"
*GLOBAL*,Westernmost_Easting,0.0d
array,*DATA_TYPE*,String
array,ioos_category,Identifier
array,long_name,Array
station,*DATA_TYPE*,String
station,cf_role,timeseries_id
station,ioos_category,Identifier
station,long_name,Station
wmo_platform_code,*DATA_TYPE*,int
wmo_platform_code,actual_range,13001i,56055i
wmo_platform_code,ioos_category,Identifier
wmo_platform_code,long_name,WMO Platform Code
wmo_platform_code,missing_value,2147483647i
longitude,*DATA_TYPE*,float
longitude,_CoordinateAxisType,Lon
longitude,actual_range,0.0f,357.0f
longitude,axis,X
longitude,epic_code,502i
longitude,ioos_category,Location
longitude,long_name,Nominal Longitude
longitude,missing_value,1.0E35f
longitude,standard_name,longitude
longitude,type,EVEN
longitude,units,degrees_east
latitude,*DATA_TYPE*,float
latitude,_CoordinateAxisType,Lat
latitude,actual_range,-25.0f,21.0f
latitude,axis,Y
latitude,epic_code,500i
latitude,ioos_category,Location
latitude,long_name,Nominal Latitude
latitude,missing_value,1.0E35f
latitude,standard_name,latitude
latitude,type,EVEN
latitude,units,degrees_north
time,*DATA_TYPE*,String
time,_CoordinateAxisType,Time
time,actual_range,1977-11-06T12:00:00Z\n2025-05-01T12:00:00Z
time,axis,T
time,ioos_category,Time
time,long_name,Centered Time
time,point_spacing,even
time,standard_name,time
time,time_origin,01-JAN-1970 00:00:00
time,type,EVEN
time,units,yyyy-MM-dd'T'HH:mm:ssZ
depth,*DATA_TYPE*,float
depth,_CoordinateAxisType,Height
depth,_CoordinateZisPositive,down
depth,actual_range,-10.0f,-4.0f
depth,axis,Z
depth,epic_code,3i
depth,ioos_category,Location
depth,long_name,Depth
depth,missing_value,1.0E35f
depth,positive,down
depth,standard_name,depth
depth,type,EVEN
depth,units,m
WU_422,*DATA_TYPE*,float
WU_422,_FillValue,1.0E35f
WU_422,actual_range,-14.3f,17.7f
WU_422,colorBarMaximum,15.0d
WU_422,colorBarMinimum,-15.0d
WU_422,epic_code,422i
WU_422,generic_name,u
WU_422,ioos_category,Wind
WU_422,long_name,Zonal Wind
WU_422,missing_value,1.0E35f
WU_422,name,WU
WU_422,standard_name,x_wind
WU_422,units,m s-1
WV_423,*DATA_TYPE*,float
WV_423,_FillValue,1.0E35f
WV_423,actual_range,-13.6f,13.7f
WV_423,colorBarMaximum,15.0d
WV_423,colorBarMinimum,-15.0d
WV_423,epic_code,423i
WV_423,generic_name,v
WV_423,ioos_category,Wind
WV_423,long_name,Meridional Wind
WV_423,missing_value,1.0E35f
WV_423,name,WV
WV_423,standard_name,y_wind
WV_423,units,m s-1
WS_401,*DATA_TYPE*,float
WS_401,_FillValue,1.0E35f
WS_401,actual_range,0.0f,17.9f
WS_401,colorBarMaximum,15.0d
WS_401,colorBarMinimum,0.0d
WS_401,epic_code,401i
WS_401,ioos_category,Wind
WS_401,long_name,Wind Speed
WS_401,missing_value,1.0E35f
WS_401,name,WS
WS_401,standard_name,wind_speed
WS_401,units,m s-1
QWS_5401,*DATA_TYPE*,float
QWS_5401,actual_range,0.0f,5.0f
QWS_5401,colorBarContinuous,false
QWS_5401,colorBarMaximum,6.0d
QWS_5401,colorBarMinimum,0.0d
QWS_5401,description,"Quality: 0=missing data, 1=highest, 2=standard, 3=lower, 4=questionable, 5=bad, -9=contact Dai.C.McClurg@noaa.gov.  To get probably valid data only, request QWS_5401>=1 and QWS_5401<=3."
QWS_5401,ioos_category,Quality
QWS_5401,long_name,Wind Speed Quality
QWS_5401,missing_value,1.0E35f
SWS_6401,*DATA_TYPE*,float
SWS_6401,actual_range,0.0f,6.0f
SWS_6401,colorBarContinuous,false
SWS_6401,colorBarMaximum,8.0d
SWS_6401,colorBarMinimum,0.0d
SWS_6401,description,"Source Codes:\n0 = No Sensor, No Data\n1 = Real Time (Telemetered Mode)\n2 = Derived from Real Time\n3 = Temporally Interpolated from Real Time\n4 = Source Code Inactive at Present\n5 = Recovered from Instrument RAM (Delayed Mode)\n6 = Derived from RAM\n7 = Temporally Interpolated from RAM"
SWS_6401,ioos_category,Other
SWS_6401,long_name,Wind Speed Source
SWS_6401,missing_value,1.0E35f
WD_410,*DATA_TYPE*,float
WD_410,_FillValue,1.0E35f
WD_410,actual_range,0.0f,360.0f
WD_410,colorBarMaximum,60.0d
WD_410,colorBarMinimum,0.0d
WD_410,epic_code,410i
WD_410,ioos_category,Wind
WD_410,long_name,Wind Direction
WD_410,missing_value,1.0E35f
WD_410,name,WD
WD_410,standard_name,wind_to_direction
WD_410,units,degrees_true
QWD_5410,*DATA_TYPE*,float
QWD_5410,actual_range,0.0f,5.0f
QWD_5410,colorBarContinuous,false
QWD_5410,colorBarMaximum,6.0d
QWD_5410,colorBarMinimum,0.0d
QWD_5410,description,"Quality: 0=missing data, 1=highest, 2=standard, 3=lower, 4=questionable, 5=bad, -9=contact Dai.C.McClurg@noaa.gov.  To get probably valid data only, request QTAU_5440>=1 and QTAU_5440<=3."
QWD_5410,ioos_category,Quality
QWD_5410,long_name,Wind Direction Quality
QWD_5410,missing_value,1.0E35f
SWD_6410,*DATA_TYPE*,float
SWD_6410,actual_range,0.0f,6.0f
SWD_6410,colorBarContinuous,false
SWD_6410,colorBarMaximum,8.0d
SWD_6410,colorBarMinimum,0.0d
SWD_6410,description,"Source Codes:\n0 = No Sensor, No Data\n1 = Real Time (Telemetered Mode)\n2 = Derived from Real Time\n3 = Temporally Interpolated from Real Time\n4 = Source Code Inactive at Present\n5 = Recovered from Instrument RAM (Delayed Mode)\n6 = Derived from RAM\n7 = Temporally Interpolated from RAM"
SWD_6410,ioos_category,Other
SWD_6410,long_name,Wind Direction Source
SWD_6410,missing_value,1.0E35f

*END_METADATA*
array,station,wmo_platform_code,longitude,latitude,time,depth,WU_422,WV_423,WS_401,QWS_5401,SWS_6401,WD_410,QWD_5410,SWD_6410
*END_DATA*
