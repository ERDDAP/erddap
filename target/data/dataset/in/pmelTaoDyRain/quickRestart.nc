*GLOBAL*,Conventions,"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.2"
*GLOBAL*,cdm_data_type,TimeSeries
*GLOBAL*,cdm_timeseries_variables,"array, station, wmo_platform_code, longitude, latitude, depth"
*GLOBAL*,creationTimeMillis,"1747835305616"
*GLOBAL*,creator_email,Dai.C.McClurg@noaa.gov
*GLOBAL*,creator_name,GTMBA Project Office/NOAA/PMEL
*GLOBAL*,creator_type,group
*GLOBAL*,creator_url,https://www.pmel.noaa.gov/gtmba/mission
*GLOBAL*,Data_Source,Global Tropical Moored Buoy Array Project Office/NOAA/PMEL
*GLOBAL*,defaultGraphQuery,"longitude,latitude,RN_485&time>=now-7days"
*GLOBAL*,Easternmost_Easting,357.0d
*GLOBAL*,featureType,TimeSeries
*GLOBAL*,File_info,Contact: Dai.C.McClurg@noaa.gov
*GLOBAL*,geospatial_lat_max,21.0d
*GLOBAL*,geospatial_lat_min,-25.0d
*GLOBAL*,geospatial_lat_units,degrees_north
*GLOBAL*,geospatial_lon_max,357.0d
*GLOBAL*,geospatial_lon_min,0.0d
*GLOBAL*,geospatial_lon_units,degrees_east
*GLOBAL*,geospatial_vertical_max,-2.8d
*GLOBAL*,geospatial_vertical_min,-4.0d
*GLOBAL*,geospatial_vertical_positive,down
*GLOBAL*,geospatial_vertical_units,m
*GLOBAL*,history,"This dataset has data from the TAO/TRITON, RAMA, and PIRATA projects.\nThis dataset is a product of the TAO Project Office at NOAA/PMEL.\n2025-05-02 Bob Simons at NOAA/NMFS/SWFSC/ERD (bob.simons@noaa.gov) fully refreshed ERD's copy of this dataset by downloading all of the .cdf files from the PMEL TAO FTP site.  Since then, the dataset has been partially refreshed everyday by downloading and merging the latest version of the last 25 days worth of data."
*GLOBAL*,infoUrl,https://www.pmel.noaa.gov/gtmba/mission
*GLOBAL*,institution,"NOAA PMEL, TAO/TRITON, RAMA, PIRATA"
*GLOBAL*,keywords,"amount, atmosphere, buoys, centered, daily, depth, deviation, Earth Science > Atmosphere > Precipitation > Precipitation Amount, identifier, meteorology, noaa, percent, pirata, pmel, precipitation, precipitation_amount, quality, raining, rama, source, standard, station, statistics, tao, time, triton"
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
*GLOBAL*,summary,"This dataset has daily Precipitation data from the\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\nhttps://www.pmel.noaa.gov/gtmba/mission ."
*GLOBAL*,testOutOfDate,now-3days
*GLOBAL*,time_coverage_end,2025-05-01T12:00:00Z
*GLOBAL*,time_coverage_start,1997-04-05T12:00:00Z
*GLOBAL*,title,"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1997-present, Precipitation"
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
time,actual_range,1997-04-05T12:00:00Z\n2025-05-01T12:00:00Z
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
depth,actual_range,-4.0f,-2.8f
depth,axis,Z
depth,epic_code,3i
depth,ioos_category,Location
depth,long_name,Depth
depth,missing_value,1.0E35f
depth,positive,down
depth,standard_name,depth
depth,type,EVEN
depth,units,m
RN_485,*DATA_TYPE*,float
RN_485,_FillValue,1.0E35f
RN_485,actual_range,-0.88f,38.11f
RN_485,colorBarMaximum,70.0d
RN_485,colorBarMinimum,0.0d
RN_485,epic_code,485i
RN_485,FORTRAN_format,rain
RN_485,ioos_category,Meteorology
RN_485,long_name,Precipitation
RN_485,missing_value,1.0E35f
RN_485,name,RN
RN_485,standard_name,precipitation_amount
RN_485,units,MM/HR
QRN_5485,*DATA_TYPE*,float
QRN_5485,_FillValue,1.0E35f
QRN_5485,actual_range,0.0f,5.0f
QRN_5485,colorBarContinuous,false
QRN_5485,colorBarMaximum,6.0d
QRN_5485,colorBarMinimum,0.0d
QRN_5485,description,"Quality: 0=missing data, 1=highest, 2=standard, 3=lower, 4=questionable, 5=bad, -9=contact Dai.C.McClurg@noaa.gov.  To get probably valid data only, request QRN_5485>=1 and QRN_5485<=3."
QRN_5485,epic_code,5485i
QRN_5485,generic_name,qrn
QRN_5485,ioos_category,Quality
QRN_5485,long_name,Precipitation Quality
QRN_5485,missing_value,1.0E35f
QRN_5485,name,QRN
SRN_6485,*DATA_TYPE*,float
SRN_6485,_FillValue,1.0E35f
SRN_6485,actual_range,0.0f,7.0f
SRN_6485,colorBarContinuous,false
SRN_6485,colorBarMaximum,8.0d
SRN_6485,colorBarMinimum,0.0d
SRN_6485,description,"Source Codes:\n0 = No Sensor, No Data\n1 = Real Time (Telemetered Mode)\n2 = Derived from Real Time\n3 = Temporally Interpolated from Real Time\n4 = Source Code Inactive at Present\n5 = Recovered from Instrument RAM (Delayed Mode)\n6 = Derived from RAM\n7 = Temporally Interpolated from RAM"
SRN_6485,epic_code,6485i
SRN_6485,generic_name,srn
SRN_6485,ioos_category,Other
SRN_6485,long_name,Precipitation Source
SRN_6485,missing_value,1.0E35f
SRN_6485,name,SRN
RNS_486,*DATA_TYPE*,float
RNS_486,_FillValue,1.0E35f
RNS_486,actual_range,0.0f,270.42f
RNS_486,colorBarMaximum,200.0d
RNS_486,colorBarMinimum,0.0d
RNS_486,epic_code,486i
RNS_486,generic_name,stdev
RNS_486,ioos_category,Statistics
RNS_486,long_name,Precipitation Standard Deviation
RNS_486,missing_value,1.0E35f
RNS_486,name,RNS
RNS_486,units,MM/HR
RNP_487,*DATA_TYPE*,float
RNP_487,_FillValue,1.0E35f
RNP_487,actual_range,0.0f,91.7f
RNP_487,colorBarMaximum,100.0d
RNP_487,colorBarMinimum,0.0d
RNP_487,epic_code,487i
RNP_487,ioos_category,Statistics
RNP_487,long_name,Percent Time Raining
RNP_487,missing_value,1.0E35f
RNP_487,name,RNP
RNP_487,units,percent

*END_METADATA*
array,station,wmo_platform_code,longitude,latitude,time,depth,RN_485,QRN_5485,SRN_6485,RNS_486,RNP_487
*END_DATA*
