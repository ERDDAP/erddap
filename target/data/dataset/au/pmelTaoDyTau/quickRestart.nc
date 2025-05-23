*GLOBAL*,Conventions,"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.2"
*GLOBAL*,cdm_data_type,TimeSeries
*GLOBAL*,cdm_timeseries_variables,"array, station, wmo_platform_code, longitude, latitude, depth"
*GLOBAL*,creationTimeMillis,"1747835342049"
*GLOBAL*,creator_email,Dai.C.McClurg@noaa.gov
*GLOBAL*,creator_name,GTMBA Project Office/NOAA/PMEL
*GLOBAL*,creator_type,group
*GLOBAL*,creator_url,https://www.pmel.noaa.gov/gtmba/mission
*GLOBAL*,Data_Source,Global Tropical Moored Buoy Array Project Office/NOAA/PMEL
*GLOBAL*,defaultGraphQuery,"longitude,latitude,TX_442,TY_443&orderByMax(""station,time"")&.draw=vectors&.color=0xFF0000&time>=now-7days"
*GLOBAL*,Easternmost_Easting,357.0d
*GLOBAL*,featureType,TimeSeries
*GLOBAL*,File_info,Contact: Dai.C.McClurg@noaa.gov
*GLOBAL*,geospatial_lat_max,21.0d
*GLOBAL*,geospatial_lat_min,-25.0d
*GLOBAL*,geospatial_lat_units,degrees_north
*GLOBAL*,geospatial_lon_max,357.0d
*GLOBAL*,geospatial_lon_min,0.0d
*GLOBAL*,geospatial_lon_units,degrees_east
*GLOBAL*,geospatial_vertical_max,0.0d
*GLOBAL*,geospatial_vertical_min,0.0d
*GLOBAL*,geospatial_vertical_positive,down
*GLOBAL*,geospatial_vertical_units,m
*GLOBAL*,history,"This dataset has data from the TAO/TRITON, RAMA, and PIRATA projects.\nThis dataset is a product of the TAO Project Office at NOAA/PMEL.\n2025-05-02 Bob Simons at NOAA/NMFS/SWFSC/ERD (bob.simons@noaa.gov) fully refreshed ERD's copy of this dataset by downloading all of the .cdf files from the PMEL TAO FTP site.  Since then, the dataset has been partially refreshed everyday by downloading and merging the latest version of the last 25 days worth of data."
*GLOBAL*,infoUrl,https://www.pmel.noaa.gov/gtmba/mission
*GLOBAL*,institution,"NOAA PMEL, TAO/TRITON, RAMA, PIRATA"
*GLOBAL*,keywords,"atmosphere, atmospheric, buoys, centered, daily, depth, direction, downward, Earth Science > Atmosphere > Atmospheric Winds > Surface Winds, Earth Science > Atmosphere > Atmospheric Winds > Wind Stress, eastward, from, identifier, meridional, noaa, northward, pirata, pmel, quality, rama, station, stress, surface, surface_downward_eastward_stress, surface_downward_northward_stress, tao, time, triton, wind, wind_from_direction, winds, zonal"
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
*GLOBAL*,summary,"This dataset has daily Wind Stress data from the\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\nhttps://www.pmel.noaa.gov/gtmba/mission ."
*GLOBAL*,testOutOfDate,now-3days
*GLOBAL*,time_coverage_end,2025-05-01T12:00:00Z
*GLOBAL*,time_coverage_start,1989-11-29T12:00:00Z
*GLOBAL*,title,"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1989-present, Wind Stress"
*GLOBAL*,Westernmost_Easting,0.0d
*GLOBAL*,wind_stress_type,Stresses computed from absolute wind speeds
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
time,actual_range,1989-11-29T12:00:00Z\n2025-05-01T12:00:00Z
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
depth,actual_range,0.0f,0.0f
depth,axis,Z
depth,epic_code,3i
depth,ioos_category,Location
depth,long_name,Depth
depth,missing_value,1.0E35f
depth,positive,down
depth,standard_name,depth
depth,type,EVEN
depth,units,m
TX_442,*DATA_TYPE*,float
TX_442,_FillValue,1.0E35f
TX_442,actual_range,-0.4558819f,0.4622149f
TX_442,colorBarMaximum,0.5d
TX_442,colorBarMinimum,-0.5d
TX_442,epic_code,442i
TX_442,generic_name,tx
TX_442,ioos_category,Wind
TX_442,long_name,Zonal Wind Stress
TX_442,missing_value,1.0E35f
TX_442,name,TX
TX_442,units,N/m2
TY_443,*DATA_TYPE*,float
TY_443,_FillValue,1.0E35f
TY_443,actual_range,-0.4769209f,0.520821f
TY_443,colorBarMaximum,0.5d
TY_443,colorBarMinimum,-0.5d
TY_443,epic_code,443i
TY_443,generic_name,ty
TY_443,ioos_category,Wind
TY_443,long_name,Meridional Wind Stress
TY_443,missing_value,1.0E35f
TY_443,name,TY
TY_443,units,N/m2
TAU_440,*DATA_TYPE*,float
TAU_440,_FillValue,1.0E35f
TAU_440,actual_range,0.0f,0.571551f
TAU_440,colorBarMaximum,0.5d
TAU_440,colorBarMinimum,0.0d
TAU_440,epic_code,440i
TAU_440,generic_name,tau
TAU_440,ioos_category,Wind
TAU_440,long_name,Wind Stress
TAU_440,missing_value,1.0E35f
TAU_440,name,TAU
TAU_440,units,N/m2
TD_445,*DATA_TYPE*,float
TD_445,_FillValue,1.0E35f
TD_445,actual_range,0.0f,359.9986f
TD_445,colorBarMaximum,60.0d
TD_445,colorBarMinimum,0.0d
TD_445,epic_code,445i
TD_445,generic_name,td
TD_445,ioos_category,Wind
TD_445,long_name,Wind Stress Direction
TD_445,missing_value,1.0E35f
TD_445,name,TD
TD_445,units,degrees_true
QTAU_5440,*DATA_TYPE*,float
QTAU_5440,_FillValue,1.0E35f
QTAU_5440,actual_range,0.0f,2.0f
QTAU_5440,colorBarContinuous,false
QTAU_5440,colorBarMaximum,6.0d
QTAU_5440,colorBarMinimum,0.0d
QTAU_5440,description,"Quality: 0=missing data, 1=highest, 2=standard, 3=lower, 4=questionable, 5=bad, -9=contact Dai.C.McClurg@noaa.gov.  To get probably valid data only, request QTAU_5440>=1 and QTAU_5440<=3."
QTAU_5440,epic_code,5440i
QTAU_5440,generic_name,qtau
QTAU_5440,ioos_category,Quality
QTAU_5440,long_name,Wind Stress Quality
QTAU_5440,missing_value,1.0E35f
QTAU_5440,name,QTAU

*END_METADATA*
array,station,wmo_platform_code,longitude,latitude,time,depth,TX_442,TY_443,TAU_440,TD_445,QTAU_5440
*END_DATA*
