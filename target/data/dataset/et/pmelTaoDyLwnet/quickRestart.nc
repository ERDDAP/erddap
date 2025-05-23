*GLOBAL*,Conventions,"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.2"
*GLOBAL*,cdm_data_type,TimeSeries
*GLOBAL*,cdm_timeseries_variables,"array, station, wmo_platform_code, longitude, latitude, depth"
*GLOBAL*,creationTimeMillis,"1747835280357"
*GLOBAL*,creator_email,Dai.C.McClurg@noaa.gov
*GLOBAL*,creator_name,GTMBA Project Office/NOAA/PMEL
*GLOBAL*,creator_type,group
*GLOBAL*,creator_url,https://www.pmel.noaa.gov/gtmba/mission
*GLOBAL*,Data_Source,Global Tropical Moored Buoy Array Project Office/NOAA/PMEL
*GLOBAL*,defaultGraphQuery,"longitude,latitude,LWN_1136&time>=now-7days"
*GLOBAL*,Easternmost_Easting,350.0d
*GLOBAL*,featureType,TimeSeries
*GLOBAL*,File_info,Contact: Dai.C.McClurg@noaa.gov
*GLOBAL*,geospatial_lat_max,20.0d
*GLOBAL*,geospatial_lat_min,-25.0d
*GLOBAL*,geospatial_lat_units,degrees_north
*GLOBAL*,geospatial_lon_max,350.0d
*GLOBAL*,geospatial_lon_min,8.0d
*GLOBAL*,geospatial_lon_units,degrees_east
*GLOBAL*,geospatial_vertical_max,-3.5d
*GLOBAL*,geospatial_vertical_min,-4.0d
*GLOBAL*,geospatial_vertical_positive,down
*GLOBAL*,geospatial_vertical_units,m
*GLOBAL*,history,"This dataset has data from the TAO/TRITON, RAMA, and PIRATA projects.\nThis dataset is a product of the TAO Project Office at NOAA/PMEL.\n2025-05-02 Bob Simons at NOAA/NMFS/SWFSC/ERD (bob.simons@noaa.gov) fully refreshed ERD's copy of this dataset by downloading all of the .cdf files from the PMEL TAO FTP site.  Since then, the dataset has been partially refreshed everyday by downloading and merging the latest version of the last 25 days worth of data."
*GLOBAL*,infoUrl,https://www.pmel.noaa.gov/gtmba/mission
*GLOBAL*,institution,"NOAA PMEL, TAO/TRITON, RAMA, PIRATA"
*GLOBAL*,keywords,"atmosphere, atmospheric, buoys, centered, daily, depth, Earth Science > Atmosphere > Atmospheric Radiation > Longwave Radiation, identifier, longwave, net, noaa, pirata, pmel, quality, radiation, rama, source, station, surface_net_downward_longwave_flux, tao, time, triton"
*GLOBAL*,keywords_vocabulary,GCMD Science Keywords
*GLOBAL*,license,"Request for Acknowledgement: If you use these data in publications or presentations, please acknowledge the GTMBA Project Office of NOAA/PMEL. Also, we would appreciate receiving a preprint and/or reprint of publications utilizing the data for inclusion in our bibliography. Relevant publications should be sent to: GTMBA Project Office, NOAA/Pacific Marine Environmental Laboratory, 7600 Sand Point Way NE, Seattle, WA 98115\n\nThe data may be used and redistributed for free but is not intended\nfor legal use, since it may contain inaccuracies. Neither the data\nContributor, ERD, NOAA, nor the United States Government, nor any\nof their employees or contractors, makes any warranty, express or\nimplied, including warranties of merchantability and fitness for a\nparticular purpose, or assumes any legal liability for the accuracy,\ncompleteness, or usefulness, of this information."
*GLOBAL*,Northernmost_Northing,20.0d
*GLOBAL*,project,"TAO/TRITON, RAMA, PIRATA"
*GLOBAL*,Request_for_acknowledgement,"If you use these data in publications or presentations, please acknowledge the GTMBA Project Office of NOAA/PMEL. Also, we would appreciate receiving a preprint and/or reprint of publications utilizing the data for inclusion in our bibliography. Relevant publications should be sent to: GTMBA Project Office, NOAA/Pacific Marine Environmental Laboratory, 7600 Sand Point Way NE, Seattle, WA 98115"
*GLOBAL*,sourceErddapVersion,2.26d
*GLOBAL*,sourceUrl,(local files)
*GLOBAL*,Southernmost_Northing,-25.0d
*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v70
*GLOBAL*,subsetVariables,"array, station, wmo_platform_code, longitude, latitude, depth"
*GLOBAL*,summary,"This dataset has daily Net Longwave Radiation data from the\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\nhttps://www.pmel.noaa.gov/gtmba/mission ."
*GLOBAL*,testOutOfDate,now-3days
*GLOBAL*,time_coverage_end,2025-05-01T12:00:00Z
*GLOBAL*,time_coverage_start,2000-04-22T12:00:00Z
*GLOBAL*,title,"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 2000-present, Net Longwave Radiation"
*GLOBAL*,Westernmost_Easting,8.0d
array,*DATA_TYPE*,String
array,ioos_category,Identifier
array,long_name,Array
station,*DATA_TYPE*,String
station,cf_role,timeseries_id
station,ioos_category,Identifier
station,long_name,Station
wmo_platform_code,*DATA_TYPE*,int
wmo_platform_code,actual_range,13008i,56055i
wmo_platform_code,ioos_category,Identifier
wmo_platform_code,long_name,WMO Platform Code
wmo_platform_code,missing_value,2147483647i
longitude,*DATA_TYPE*,float
longitude,_CoordinateAxisType,Lon
longitude,actual_range,8.0f,350.0f
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
latitude,actual_range,-25.0f,20.0f
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
time,actual_range,2000-04-22T12:00:00Z\n2025-05-01T12:00:00Z
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
depth,actual_range,-4.0f,-3.5f
depth,axis,Z
depth,epic_code,3i
depth,ioos_category,Location
depth,long_name,Depth
depth,missing_value,1.0E35f
depth,positive,down
depth,standard_name,depth
depth,type,EVEN
depth,units,m
LWN_1136,*DATA_TYPE*,float
LWN_1136,_FillValue,1.0E35f
LWN_1136,actual_range,-120.6373f,114.6859f
LWN_1136,colorBarMaximum,130.0d
LWN_1136,colorBarMinimum,-30.0d
LWN_1136,epic_code,1136i
LWN_1136,generic_name,qln
LWN_1136,ioos_category,Other
LWN_1136,long_name,Net Longwave Radiation
LWN_1136,missing_value,1.0E35f
LWN_1136,name,LWN
LWN_1136,standard_name,surface_net_downward_longwave_flux
LWN_1136,units,W m-2
QLW_5136,*DATA_TYPE*,float
QLW_5136,_FillValue,1.0E35f
QLW_5136,actual_range,0.0f,5.0f
QLW_5136,colorBarContinuous,false
QLW_5136,colorBarMaximum,6.0d
QLW_5136,colorBarMinimum,0.0d
QLW_5136,description,"Quality: 0=missing data, 1=highest, 2=standard, 3=lower, 4=questionable, 5=bad, -9=contact Dai.C.McClurg@noaa.gov.  To get probably valid data only, request QLW_5136>=1 and QLW_5136<=3."
QLW_5136,epic_code,5136i
QLW_5136,generic_name,qlw
QLW_5136,ioos_category,Quality
QLW_5136,long_name,Longwave Radiation Quality
QLW_5136,missing_value,1.0E35f
QLW_5136,name,QLW
SLW_6136,*DATA_TYPE*,float
SLW_6136,_FillValue,1.0E35f
SLW_6136,actual_range,0.0f,5.0f
SLW_6136,colorBarContinuous,false
SLW_6136,colorBarMaximum,8.0d
SLW_6136,colorBarMinimum,0.0d
SLW_6136,description,"Source Codes:\n0 = No Sensor, No Data\n1 = Real Time (Telemetered Mode)\n2 = Derived from Real Time\n3 = Temporally Interpolated from Real Time\n4 = Source Code Inactive at Present\n5 = Recovered from Instrument RAM (Delayed Mode)\n6 = Derived from RAM\n7 = Temporally Interpolated from RAM"
SLW_6136,epic_code,6136i
SLW_6136,generic_name,slw
SLW_6136,ioos_category,Other
SLW_6136,long_name,Longwave Radiation Source
SLW_6136,missing_value,1.0E35f
SLW_6136,name,SLW

*END_METADATA*
array,station,wmo_platform_code,longitude,latitude,time,depth,LWN_1136,QLW_5136,SLW_6136
*END_DATA*
