*GLOBAL*,Conventions,"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.2"
*GLOBAL*,cdm_data_type,TimeSeries
*GLOBAL*,cdm_timeseries_variables,"array, station, wmo_platform_code, longitude, latitude"
*GLOBAL*,creationTimeMillis,"1747835229233"
*GLOBAL*,creator_email,Dai.C.McClurg@noaa.gov
*GLOBAL*,creator_name,GTMBA Project Office/NOAA/PMEL
*GLOBAL*,creator_type,group
*GLOBAL*,creator_url,https://www.pmel.noaa.gov/gtmba/mission
*GLOBAL*,Data_Source,Global Tropical Moored Buoy Array Project Office/NOAA/PMEL
*GLOBAL*,Easternmost_Easting,337.0d
*GLOBAL*,featureType,TimeSeries
*GLOBAL*,File_info,Contact: Dai.C.McClurg@noaa.gov
*GLOBAL*,geospatial_lat_max,8.0d
*GLOBAL*,geospatial_lat_min,-8.5d
*GLOBAL*,geospatial_lat_units,degrees_north
*GLOBAL*,geospatial_lon_max,337.0d
*GLOBAL*,geospatial_lon_min,67.0d
*GLOBAL*,geospatial_lon_units,degrees_east
*GLOBAL*,geospatial_vertical_max,475.0d
*GLOBAL*,geospatial_vertical_min,5.0d
*GLOBAL*,geospatial_vertical_positive,down
*GLOBAL*,geospatial_vertical_units,m
*GLOBAL*,history,"This dataset has data from the TAO/TRITON, RAMA, and PIRATA projects.\nThis dataset is a product of the TAO Project Office at NOAA/PMEL.\n2025-05-02 Bob Simons at NOAA/NMFS/SWFSC/ERD (bob.simons@noaa.gov) fully refreshed ERD's copy of this dataset by downloading all of the .cdf files from the PMEL TAO FTP site.  Since then, the dataset has been partially refreshed everyday by downloading and merging the latest version of the last 25 days worth of data."
*GLOBAL*,infoUrl,https://www.pmel.noaa.gov/gtmba/mission
*GLOBAL*,institution,"NOAA PMEL, TAO/TRITON, RAMA, PIRATA"
*GLOBAL*,keywords,"adcp, buoys, centered, circulation, currents, daily, depth, Earth Science > Oceans > Ocean Circulation > Ocean Currents, eastward, eastward_sea_water_velocity, identifier, noaa, northward, northward_sea_water_velocity, ocean, oceans, pirata, pmel, quality, rama, sea, seawater, station, tao, time, triton, velocity, water"
*GLOBAL*,keywords_vocabulary,GCMD Science Keywords
*GLOBAL*,license,"Request for Acknowledgement: If you use these data in publications or presentations, please acknowledge the GTMBA Project Office of NOAA/PMEL. Also, we would appreciate receiving a preprint and/or reprint of publications utilizing the data for inclusion in our bibliography. Relevant publications should be sent to: GTMBA Project Office, NOAA/Pacific Marine Environmental Laboratory, 7600 Sand Point Way NE, Seattle, WA 98115\n\nThe data may be used and redistributed for free but is not intended\nfor legal use, since it may contain inaccuracies. Neither the data\nContributor, ERD, NOAA, nor the United States Government, nor any\nof their employees or contractors, makes any warranty, express or\nimplied, including warranties of merchantability and fitness for a\nparticular purpose, or assumes any legal liability for the accuracy,\ncompleteness, or usefulness, of this information."
*GLOBAL*,Northernmost_Northing,8.0d
*GLOBAL*,project,"TAO/TRITON, RAMA, PIRATA"
*GLOBAL*,Request_for_acknowledgement,"If you use these data in publications or presentations, please acknowledge the GTMBA Project Office of NOAA/PMEL. Also, we would appreciate receiving a preprint and/or reprint of publications utilizing the data for inclusion in our bibliography. Relevant publications should be sent to: GTMBA Project Office, NOAA/Pacific Marine Environmental Laboratory, 7600 Sand Point Way NE, Seattle, WA 98115"
*GLOBAL*,sourceErddapVersion,2.26d
*GLOBAL*,sourceUrl,(local files)
*GLOBAL*,Southernmost_Northing,-8.5d
*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v70
*GLOBAL*,subsetVariables,"array, station, wmo_platform_code, longitude, latitude"
*GLOBAL*,summary,"This dataset has daily Acoustic Doppler Current Profiler (ADCP) water currents data from the\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  ADCP data are available only after mooring recoveries, which are scheduled on an annual basis.  For more information, see\nhttps://www.pmel.noaa.gov/gtmba/mission ."
*GLOBAL*,time_coverage_end,2023-09-23T12:00:00Z
*GLOBAL*,time_coverage_start,1988-05-16T12:00:00Z
*GLOBAL*,title,"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1988-2020, ADCP"
*GLOBAL*,Westernmost_Easting,67.0d
array,*DATA_TYPE*,String
array,ioos_category,Identifier
array,long_name,Array
station,*DATA_TYPE*,String
station,cf_role,timeseries_id
station,ioos_category,Identifier
station,long_name,Station
wmo_platform_code,*DATA_TYPE*,int
wmo_platform_code,actual_range,14040i,52321i
wmo_platform_code,ioos_category,Identifier
wmo_platform_code,long_name,WMO Platform Code
wmo_platform_code,missing_value,2147483647i
longitude,*DATA_TYPE*,float
longitude,_CoordinateAxisType,Lon
longitude,actual_range,67.0f,337.0f
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
latitude,actual_range,-8.5f,8.0f
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
time,actual_range,1988-05-16T12:00:00Z\n2023-09-23T12:00:00Z
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
depth,actual_range,5.0f,475.0f
depth,axis,Z
depth,epic_code,3i
depth,ioos_category,Location
depth,long_name,Depth
depth,missing_value,1.0E35f
depth,positive,down
depth,standard_name,depth
depth,type,EVEN
depth,units,m
u_1205,*DATA_TYPE*,float
u_1205,_FillValue,1.0E35f
u_1205,actual_range,-189.9937f,244.2879f
u_1205,colorBarMaximum,150.0d
u_1205,colorBarMinimum,-150.0d
u_1205,epic_code,1205i
u_1205,generic_name,u
u_1205,ioos_category,Currents
u_1205,long_name,Eastward Sea Water Velocity
u_1205,missing_value,1.0E35f
u_1205,name,u
u_1205,standard_name,eastward_sea_water_velocity
u_1205,units,cm/s
QU_5205,*DATA_TYPE*,float
QU_5205,_FillValue,1.0E35f
QU_5205,actual_range,0.0f,2.0f
QU_5205,colorBarContinuous,false
QU_5205,colorBarMaximum,6.0d
QU_5205,colorBarMinimum,0.0d
QU_5205,description,"Quality: 0=missing data, 1=highest, 2=standard, 3=lower, 4=questionable, 5=bad, -9=contact Dai.C.McClurg@noaa.gov.  To get probably valid data only, request QU_5205>=1 and QU_5205<=3."
QU_5205,epic_code,5205i
QU_5205,generic_name,qu
QU_5205,ioos_category,Quality
QU_5205,long_name,Eastward Sea Water Velocity Quality
QU_5205,missing_value,1.0E35f
QU_5205,name,QU
v_1206,*DATA_TYPE*,float
v_1206,_FillValue,1.0E35f
v_1206,actual_range,-127.6f,134.6f
v_1206,colorBarMaximum,150.0d
v_1206,colorBarMinimum,-150.0d
v_1206,epic_code,1206i
v_1206,generic_name,v
v_1206,ioos_category,Currents
v_1206,long_name,Northward Sea Water Velocity
v_1206,missing_value,1.0E35f
v_1206,name,v
v_1206,standard_name,northward_sea_water_velocity
v_1206,units,cm/s
QV_5206,*DATA_TYPE*,float
QV_5206,_FillValue,1.0E35f
QV_5206,actual_range,0.0f,2.0f
QV_5206,colorBarContinuous,false
QV_5206,colorBarMaximum,6.0d
QV_5206,colorBarMinimum,0.0d
QV_5206,description,"Quality: 0=missing data, 1=highest, 2=standard, 3=lower, 4=questionable, 5=bad, -9=contact Dai.C.McClurg@noaa.gov.  To get probably valid data only, request QV_5206>=1 and QV_5206<=3."
QV_5206,epic_code,5206i
QV_5206,generic_name,qv
QV_5206,ioos_category,Quality
QV_5206,long_name,Northward Sea Water Velocity Quality
QV_5206,missing_value,1.0E35f
QV_5206,name,QV

*END_METADATA*
array,station,wmo_platform_code,longitude,latitude,time,depth,u_1205,QU_5205,v_1206,QV_5206
*END_DATA*
