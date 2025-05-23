*GLOBAL*,Conventions,"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.2"
*GLOBAL*,cdm_data_type,TimeSeries
*GLOBAL*,cdm_timeseries_variables,"array, station, wmo_platform_code, longitude, latitude"
*GLOBAL*,creationTimeMillis,"1747835245399"
*GLOBAL*,creator_email,Dai.C.McClurg@noaa.gov
*GLOBAL*,creator_name,GTMBA Project Office/NOAA/PMEL
*GLOBAL*,creator_type,group
*GLOBAL*,creator_url,https://www.pmel.noaa.gov/gtmba/mission
*GLOBAL*,Data_Source,Global Tropical Moored Buoy Array Project Office/NOAA/PMEL
*GLOBAL*,defaultGraphQuery,"longitude,latitude,U_320,V_320&time>=now-7days&orderByMax(""station,time"")&.draw=vectors&.color=0xFF0000"
*GLOBAL*,Easternmost_Easting,350.0d
*GLOBAL*,featureType,TimeSeries
*GLOBAL*,File_info,Contact: Dai.C.McClurg@noaa.gov
*GLOBAL*,geospatial_lat_max,21.0d
*GLOBAL*,geospatial_lat_min,-25.0d
*GLOBAL*,geospatial_lat_units,degrees_north
*GLOBAL*,geospatial_lon_max,350.0d
*GLOBAL*,geospatial_lon_min,8.0d
*GLOBAL*,geospatial_lon_units,degrees_east
*GLOBAL*,geospatial_vertical_max,2000.0d
*GLOBAL*,geospatial_vertical_min,3.0d
*GLOBAL*,geospatial_vertical_positive,down
*GLOBAL*,geospatial_vertical_units,m
*GLOBAL*,history,"This dataset has data from the TAO/TRITON, RAMA, and PIRATA projects.\nThis dataset is a product of the TAO Project Office at NOAA/PMEL.\n2025-05-02 Bob Simons at NOAA/NMFS/SWFSC/ERD (bob.simons@noaa.gov) fully refreshed ERD's copy of this dataset by downloading all of the .cdf files from the PMEL TAO FTP site.  Since then, the dataset has been partially refreshed everyday by downloading and merging the latest version of the last 25 days worth of data."
*GLOBAL*,idhis,14i,10i,14i,31i,31i,19i,27i,21i
*GLOBAL*,ihhis,12i,12i,12i,12i,12i,12i,12i,12i
*GLOBAL*,imhis,11i,1i,6i,1i,1i,11i,11i,10i
*GLOBAL*,iminhis,0i,0i,0i,0i,0i,0i,0i,0i
*GLOBAL*,infoUrl,https://www.pmel.noaa.gov/gtmba/mission
*GLOBAL*,institution,"NOAA PMEL, TAO/TRITON, RAMA, PIRATA"
*GLOBAL*,iyhis,2009i,2011i,2012i,2014i,2015i,2016i,2017i,2019i
*GLOBAL*,keywords,"buoys, centered, circulation, code, current, currents, daily, depth, direction, direction_of_sea_water_velocity, Earth Science > Oceans > Ocean Circulation > Ocean Currents, eastward, eastward_sea_water_velocity, identifier, instrument, noaa, northward, northward_sea_water_velocity, ocean, oceans, pirata, pmel, quality, rama, sea, seawater, source, speed, station, tao, time, triton, velocity, water"
*GLOBAL*,keywords_vocabulary,GCMD Science Keywords
*GLOBAL*,license,"Request for Acknowledgement: If you use these data in publications or presentations, please acknowledge the GTMBA Project Office of NOAA/PMEL. Also, we would appreciate receiving a preprint and/or reprint of publications utilizing the data for inclusion in our bibliography. Relevant publications should be sent to: GTMBA Project Office, NOAA/Pacific Marine Environmental Laboratory, 7600 Sand Point Way NE, Seattle, WA 98115\n\nThe data may be used and redistributed for free but is not intended\nfor legal use, since it may contain inaccuracies. Neither the data\nContributor, ERD, NOAA, nor the United States Government, nor any\nof their employees or contractors, makes any warranty, express or\nimplied, including warranties of merchantability and fitness for a\nparticular purpose, or assumes any legal liability for the accuracy,\ncompleteness, or usefulness, of this information."
*GLOBAL*,nhis,8i
*GLOBAL*,Northernmost_Northing,21.0d
*GLOBAL*,project,"TAO/TRITON, RAMA, PIRATA"
*GLOBAL*,Request_for_acknowledgement,"If you use these data in publications or presentations, please acknowledge the GTMBA Project Office of NOAA/PMEL. Also, we would appreciate receiving a preprint and/or reprint of publications utilizing the data for inclusion in our bibliography. Relevant publications should be sent to: GTMBA Project Office, NOAA/Pacific Marine Environmental Laboratory, 7600 Sand Point Way NE, Seattle, WA 98115"
*GLOBAL*,sourceErddapVersion,2.26d
*GLOBAL*,sourceUrl,(local files)
*GLOBAL*,Southernmost_Northing,-25.0d
*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v70
*GLOBAL*,subsetVariables,"array, station, wmo_platform_code, longitude, latitude"
*GLOBAL*,summary,"This dataset has daily Currents data from the\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\nhttps://www.pmel.noaa.gov/gtmba/mission ."
*GLOBAL*,testOutOfDate,now-3days
*GLOBAL*,time_coverage_end,2025-05-01T12:00:00Z
*GLOBAL*,time_coverage_start,1977-11-06T12:00:00Z
*GLOBAL*,title,"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Currents"
*GLOBAL*,Westernmost_Easting,8.0d
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
depth,actual_range,3.0f,2000.0f
depth,axis,Z
depth,epic_code,3i
depth,ioos_category,Location
depth,long_name,Depth
depth,missing_value,1.0E35f
depth,positive,down
depth,standard_name,depth
depth,type,UNEVEN
depth,units,m
U_320,*DATA_TYPE*,float
U_320,_FillValue,1.0E35f
U_320,actual_range,-173.8956f,233.2375f
U_320,colorBarMaximum,150.0d
U_320,colorBarMinimum,-150.0d
U_320,epic_code,320i
U_320,generic_name,u
U_320,ioos_category,Currents
U_320,long_name,Eastward Sea Water Velocity
U_320,missing_value,1.0E35f
U_320,name,U
U_320,standard_name,eastward_sea_water_velocity
U_320,units,cm s-1
V_321,*DATA_TYPE*,float
V_321,_FillValue,1.0E35f
V_321,actual_range,-125.5f,155.0f
V_321,colorBarMaximum,150.0d
V_321,colorBarMinimum,-150.0d
V_321,epic_code,321i
V_321,generic_name,v
V_321,ioos_category,Currents
V_321,long_name,Northward Sea Water Velocity
V_321,missing_value,1.0E35f
V_321,name,V
V_321,standard_name,northward_sea_water_velocity
V_321,units,cm s-1
CS_300,*DATA_TYPE*,float
CS_300,_FillValue,1.0E35f
CS_300,actual_range,0.0f,233.2748f
CS_300,colorBarMaximum,200.0d
CS_300,colorBarMinimum,0.0d
CS_300,epic_code,300i
CS_300,FORTRAN_format,f8.2
CS_300,generic_name,vspd
CS_300,ioos_category,Currents
CS_300,long_name,Sea Water Velocity
CS_300,missing_value,1.0E35f
CS_300,name,CS
CS_300,units,cm s-1
CD_310,*DATA_TYPE*,float
CD_310,_FillValue,1.0E35f
CD_310,actual_range,0.0f,360.0f
CD_310,colorBarMaximum,60.0d
CD_310,colorBarMinimum,0.0d
CD_310,epic_code,310i
CD_310,FORTRAN_format,f8.2
CD_310,generic_name,vdir
CD_310,ioos_category,Currents
CD_310,long_name,Direction of Sea Water Velocity
CD_310,missing_value,1.0E35f
CD_310,name,CD
CD_310,standard_name,direction_of_sea_water_velocity
CD_310,units,degrees_true
QCS_5300,*DATA_TYPE*,float
QCS_5300,actual_range,0.0f,5.0f
QCS_5300,colorBarContinuous,false
QCS_5300,colorBarMaximum,6.0d
QCS_5300,colorBarMinimum,0.0d
QCS_5300,description,"Quality: 0=missing data, 1=highest, 2=standard, 3=lower, 4=questionable, 5=bad, -9=contact Dai.C.McClurg@noaa.gov.  To get probably valid data only, request QCS_5300>=1 and QCS_5300<=3."
QCS_5300,ioos_category,Quality
QCS_5300,long_name,Current Speed Quality
QCS_5300,missing_value,1.0E35f
QCD_5310,*DATA_TYPE*,float
QCD_5310,actual_range,0.0f,5.0f
QCD_5310,colorBarContinuous,false
QCD_5310,colorBarMaximum,6.0d
QCD_5310,colorBarMinimum,0.0d
QCD_5310,description,"Quality: 0=missing data, 1=highest, 2=standard, 3=lower, 4=questionable, 5=bad, -9=contact Dai.C.McClurg@noaa.gov.  To get probably valid data only, request QCD_5310>=1 and QCD_5310<=3."
QCD_5310,ioos_category,Quality
QCD_5310,long_name,Current Direction Quality
QCD_5310,missing_value,1.0E35f
SCS_6300,*DATA_TYPE*,float
SCS_6300,_FillValue,1.0E35f
SCS_6300,actual_range,0.0f,6.0f
SCS_6300,colorBarContinuous,false
SCS_6300,colorBarMaximum,8.0d
SCS_6300,colorBarMinimum,0.0d
SCS_6300,description,"Source Codes:\n0 = No Sensor, No Data\n1 = Real Time (Telemetered Mode)\n2 = Derived from Real Time\n3 = Temporally Interpolated from Real Time\n4 = Source Code Inactive at Present\n5 = Recovered from Instrument RAM (Delayed Mode)\n6 = Derived from RAM\n7 = Temporally Interpolated from RAM"
SCS_6300,epic_code,6300i
SCS_6300,generic_name,scs
SCS_6300,ioos_category,Other
SCS_6300,long_name,Current Speed Source
SCS_6300,missing_value,1.0E35f
SCS_6300,name,SCS
CIC_7300,*DATA_TYPE*,float
CIC_7300,_FillValue,1.0E35f
CIC_7300,actual_range,0.0f,48.0f
CIC_7300,description,Instrument Codes: \n40 = Sontek\n70 = Seacat Temperature\n71 = Microcat Temperature\n99 = Unknown
CIC_7300,epic_code,7300i
CIC_7300,generic_name,cic
CIC_7300,ioos_category,Currents
CIC_7300,long_name,Current Instrument Code
CIC_7300,missing_value,1.0E35f
CIC_7300,name,CIC

*END_METADATA*
array,station,wmo_platform_code,longitude,latitude,time,depth,U_320,V_321,CS_300,CD_310,QCS_5300,QCD_5310,SCS_6300,CIC_7300
*END_DATA*
