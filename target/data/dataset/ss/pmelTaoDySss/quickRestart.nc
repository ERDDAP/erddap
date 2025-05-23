*GLOBAL*,Conventions,"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.2"
*GLOBAL*,cdm_data_type,TimeSeries
*GLOBAL*,cdm_timeseries_variables,"array, station, wmo_platform_code, longitude, latitude, depth"
*GLOBAL*,creationTimeMillis,"1747835325347"
*GLOBAL*,creator_email,Dai.C.McClurg@noaa.gov
*GLOBAL*,creator_name,GTMBA Project Office/NOAA/PMEL
*GLOBAL*,creator_type,group
*GLOBAL*,creator_url,https://www.pmel.noaa.gov/gtmba/mission
*GLOBAL*,Data_Source,Global Tropical Moored Buoy Array Project Office/NOAA/PMEL
*GLOBAL*,defaultGraphQuery,"longitude,latitude,S_41&time>=now-7days"
*GLOBAL*,Easternmost_Easting,357.0d
*GLOBAL*,featureType,TimeSeries
*GLOBAL*,File_info,Contact: Dai.C.McClurg@noaa.gov
*GLOBAL*,geospatial_lat_max,21.0d
*GLOBAL*,geospatial_lat_min,-25.0d
*GLOBAL*,geospatial_lat_units,degrees_north
*GLOBAL*,geospatial_lon_max,357.0d
*GLOBAL*,geospatial_lon_min,0.0d
*GLOBAL*,geospatial_lon_units,degrees_east
*GLOBAL*,geospatial_vertical_max,3.0d
*GLOBAL*,geospatial_vertical_min,1.0d
*GLOBAL*,geospatial_vertical_positive,down
*GLOBAL*,geospatial_vertical_units,m
*GLOBAL*,history,"This dataset has data from the TAO/TRITON, RAMA, and PIRATA projects.\nThis dataset is a product of the TAO Project Office at NOAA/PMEL.\n2025-05-02 Bob Simons at NOAA/NMFS/SWFSC/ERD (bob.simons@noaa.gov) fully refreshed ERD's copy of this dataset by downloading all of the .cdf files from the PMEL TAO FTP site.  Since then, the dataset has been partially refreshed everyday by downloading and merging the latest version of the last 25 days worth of data."
*GLOBAL*,infoUrl,https://www.pmel.noaa.gov/gtmba/mission
*GLOBAL*,institution,"NOAA PMEL, TAO/TRITON, RAMA, PIRATA"
*GLOBAL*,keywords,"buoys, centered, code, daily, density, depth, Earth Science > Oceans > Salinity/Density > Salinity, identifier, instrument, noaa, oceans, pirata, pmel, quality, rama, salinity, sea, sea_water_practical_salinity, seawater, source, station, surface, tao, time, triton, water"
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
*GLOBAL*,summary,"This dataset has daily Sea Surface Salinity data from the\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\nhttps://www.pmel.noaa.gov/gtmba/mission ."
*GLOBAL*,testOutOfDate,now-3days
*GLOBAL*,time_coverage_end,2025-05-01T12:00:00Z
*GLOBAL*,time_coverage_start,1992-02-08T12:00:00Z
*GLOBAL*,title,"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1992-present, Sea Surface Salinity"
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
time,actual_range,1992-02-08T12:00:00Z\n2025-05-01T12:00:00Z
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
depth,actual_range,1.0f,3.0f
depth,axis,Z
depth,epic_code,3i
depth,ioos_category,Location
depth,long_name,Depth
depth,missing_value,1.0E35f
depth,positive,down
depth,standard_name,depth
depth,type,UNEVEN
depth,units,m
S_41,*DATA_TYPE*,float
S_41,_FillValue,1.0E35f
S_41,actual_range,25.88f,38.43806f
S_41,colorBarMaximum,37.0d
S_41,colorBarMinimum,32.0d
S_41,epic_code,41i
S_41,generic_name,sal
S_41,ioos_category,Salinity
S_41,long_name,Sea Water Practical Salinity
S_41,missing_value,1.0E35f
S_41,name,S
S_41,standard_name,sea_water_practical_salinity
S_41,units,PSU
QS_5041,*DATA_TYPE*,float
QS_5041,_FillValue,1.0E35f
QS_5041,actual_range,-9.0f,5.0f
QS_5041,colorBarContinuous,false
QS_5041,colorBarMaximum,6.0d
QS_5041,colorBarMinimum,0.0d
QS_5041,description,"Quality: 0=missing data, 1=highest, 2=standard, 3=lower, 4=questionable, 5=bad, -9=contact Dai.C.McClurg@noaa.gov.  To get probably valid data only, request QS_5041>=1 and QS_5041<=3."
QS_5041,epic_code,5041i
QS_5041,generic_name,qs
QS_5041,ioos_category,Quality
QS_5041,long_name,Salinity Quality
QS_5041,missing_value,1.0E35f
QS_5041,name,QS
SS_6041,*DATA_TYPE*,float
SS_6041,_FillValue,1.0E35f
SS_6041,actual_range,0.0f,7.0f
SS_6041,colorBarContinuous,false
SS_6041,colorBarMaximum,8.0d
SS_6041,colorBarMinimum,0.0d
SS_6041,description,"Source Codes:\n0 = No Sensor, No Data\n1 = Real Time (Telemetered Mode)\n2 = Derived from Real Time\n3 = Temporally Interpolated from Real Time\n4 = Source Code Inactive at Present\n5 = Recovered from Instrument RAM (Delayed Mode)\n6 = Derived from RAM\n7 = Temporally Interpolated from RAM"
SS_6041,epic_code,6041i
SS_6041,generic_name,ss
SS_6041,ioos_category,Other
SS_6041,long_name,Salinity Source
SS_6041,missing_value,1.0E35f
SS_6041,name,SS
SIC_7041,*DATA_TYPE*,float
SIC_7041,_FillValue,1.0E35f
SIC_7041,actual_range,0.0f,99.0f
SIC_7041,description,Instrument Codes:\n 0 = No Sensor\n 4 = Conductivity (FSI)\n14 = NextGen Conductivity\n24 = NextGen Conductivity (Firmware version 5.03+)\n70 = Seacat Conductivity\n71 = Microcat Conductivity   \n99 = Unknown
SIC_7041,epic_code,7041i
SIC_7041,generic_name,sic
SIC_7041,ioos_category,Other
SIC_7041,long_name,Salinity Instrument Code
SIC_7041,missing_value,1.0E35f
SIC_7041,name,SIC

*END_METADATA*
array,station,wmo_platform_code,longitude,latitude,time,depth,S_41,QS_5041,SS_6041,SIC_7041
*END_DATA*
