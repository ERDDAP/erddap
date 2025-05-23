*GLOBAL*,Conventions,"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.2"
*GLOBAL*,cdm_data_type,TimeSeries
*GLOBAL*,cdm_timeseries_variables,"array, station, wmo_platform_code, longitude, latitude, depth"
*GLOBAL*,creationTimeMillis,"1747835255625"
*GLOBAL*,creator_email,Dai.C.McClurg@noaa.gov
*GLOBAL*,creator_name,GTMBA Project Office/NOAA/PMEL
*GLOBAL*,creator_type,group
*GLOBAL*,creator_url,https://www.pmel.noaa.gov/gtmba/mission
*GLOBAL*,Data_Source,Global Tropical Moored Buoy Array Project Office/NOAA/PMEL
*GLOBAL*,defaultGraphQuery,"longitude,latitude,DYN_13&time>=now-7days"
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
*GLOBAL*,keywords,"buoys, centered, daily, depth, dynamic, Earth Science > Oceans > Sea Surface Topography > Sea Surface Height, height, identifier, level, noaa, pirata, pmel, quality, rama, sea, sea level, sea_surface_height, station, tao, time, triton"
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
*GLOBAL*,summary,"This dataset has daily Dynamic Height data (a measure of the elevation of the sea level, calculated by integrating the specific volume anomaly of the sea water between the sea surface and 500 m depth) from the \nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\nhttps://www.pmel.noaa.gov/gtmba/mission ."
*GLOBAL*,testOutOfDate,now-3days
*GLOBAL*,time_coverage_end,2025-05-01T12:00:00Z
*GLOBAL*,time_coverage_start,1980-08-11T12:00:00Z
*GLOBAL*,title,"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1980-present, Dynamic Height"
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
time,actual_range,1980-08-11T12:00:00Z\n2025-05-01T12:00:00Z
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
DYN_13,*DATA_TYPE*,float
DYN_13,_FillValue,1.0E35f
DYN_13,actual_range,61.11259f,187.772f
DYN_13,colorBarMaximum,200.0d
DYN_13,colorBarMinimum,-200.0d
DYN_13,colorBarPalette,BlueWhiteRed
DYN_13,epic_code,13i
DYN_13,generic_name,dynhgt
DYN_13,ioos_category,Sea Level
DYN_13,long_name,Dynamic Height
DYN_13,missing_value,1.0E35f
DYN_13,name,DYN
DYN_13,standard_name,sea_surface_height
DYN_13,units,dyn-cm
QD_5013,*DATA_TYPE*,float
QD_5013,_FillValue,1.0E35f
QD_5013,actual_range,0.0f,2.0f
QD_5013,colorBarContinuous,false
QD_5013,colorBarMaximum,6.0d
QD_5013,colorBarMinimum,0.0d
QD_5013,description,"Quality: 0=missing data, 1=highest, 2=standard, 3=lower, 4=questionable, 5=bad, -9=contact Dai.C.McClurg@noaa.gov.  To get probably valid data only, request QD_5013>=1 and QD_5013<=3."
QD_5013,epic_code,5013i
QD_5013,generic_name,qd
QD_5013,ioos_category,Quality
QD_5013,long_name,Dynamic Height Quality
QD_5013,missing_value,1.0E35f
QD_5013,name,QD

*END_METADATA*
array,station,wmo_platform_code,longitude,latitude,time,depth,DYN_13,QD_5013
*END_DATA*
