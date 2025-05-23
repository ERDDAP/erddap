*GLOBAL*,Conventions,"COARDS, WOCE, GTSPP, CF-1.10, ACDD-1.3, NCCSV-1.2"
*GLOBAL*,acknowledgment,These data were acquired from the US NOAA National Oceanographic Data Center (NODC) on 2025-05-09 from https://www.nodc.noaa.gov/GTSPP/.
*GLOBAL*,cdm_altitude_proxy,depth
*GLOBAL*,cdm_data_type,TrajectoryProfile
*GLOBAL*,cdm_profile_variables,"station_id, longitude, latitude, time"
*GLOBAL*,cdm_trajectory_variables,"trajectory, org, type, platform, cruise"
*GLOBAL*,creationTimeMillis,"1747835220822"
*GLOBAL*,creator_email,nodc.gtspp@noaa.gov
*GLOBAL*,creator_name,NOAA NESDIS NODC (IN295)
*GLOBAL*,creator_url,https://www.nodc.noaa.gov/GTSPP/
*GLOBAL*,crs,EPSG:4326
*GLOBAL*,defaultGraphQuery,"longitude,latitude,station_id&time%3E=max(time)-7days&time%3C=max(time)&.draw=markers&.marker=10|5"
*GLOBAL*,Easternmost_Easting,179.999d
*GLOBAL*,featureType,TrajectoryProfile
*GLOBAL*,file_source,The GTSPP Continuously Managed Data Base
*GLOBAL*,geospatial_lat_max,90.0d
*GLOBAL*,geospatial_lat_min,-78.579d
*GLOBAL*,geospatial_lat_units,degrees_north
*GLOBAL*,geospatial_lon_max,179.999d
*GLOBAL*,geospatial_lon_min,-180.0d
*GLOBAL*,geospatial_lon_units,degrees_east
*GLOBAL*,geospatial_vertical_max,9910.0d
*GLOBAL*,geospatial_vertical_min,-0.4d
*GLOBAL*,geospatial_vertical_positive,down
*GLOBAL*,geospatial_vertical_units,m
*GLOBAL*,gtspp_ConventionVersion,GTSPP4.0
*GLOBAL*,gtspp_handbook_version,GTSPP Data User's Manual 1.0
*GLOBAL*,gtspp_program,writeGTSPPnc40.f90
*GLOBAL*,gtspp_programVersion,"1.8"
*GLOBAL*,history,"2025-02-01  csun writeGTSPPnc40.f90 Version 1.8\n.tgz files from ftp.nodc.noaa.gov /pub/data.nodc/gtspp/bestcopy/netcdf (https://www.nodc.noaa.gov/GTSPP/)\n2025-04-01 Most recent ingest, clean, and reformat at ERD (erd.data at noaa.gov)."
*GLOBAL*,id,erdGtsppBest
*GLOBAL*,infoUrl,https://www.nodc.noaa.gov/GTSPP/
*GLOBAL*,institution,NOAA NODC
*GLOBAL*,keywords,"cruise, data, density, depth, Earth Science > Oceans > Ocean Temperature > Water Temperature, Earth Science > Oceans > Salinity/Density > Salinity, global, gtspp, identifier, noaa, nodc, observation, ocean, oceans, organization, profile, program, salinity, sea, sea_water_practical_salinity, sea_water_temperature, seawater, station, temperature, temperature-salinity, time, type, water"
*GLOBAL*,keywords_vocabulary,"NODC Data Types, CF Standard Names, GCMD Science Keywords"
*GLOBAL*,LEXICON,NODC_GTSPP
*GLOBAL*,license,"These data are openly available to the public.  Please acknowledge the use of these data with:\nThese data were acquired from the US NOAA National Oceanographic Data Center (NODC) on 2025-05-09 from https://www.nodc.noaa.gov/GTSPP/.\n\nThe data may be used and redistributed for free but is not intended\nfor legal use, since it may contain inaccuracies. Neither the data\nContributor, ERD, NOAA, nor the United States Government, nor any\nof their employees or contractors, makes any warranty, express or\nimplied, including warranties of merchantability and fitness for a\nparticular purpose, or assumes any legal liability for the accuracy,\ncompleteness, or usefulness, of this information."
*GLOBAL*,naming_authority,gov.noaa.nodc
*GLOBAL*,Northernmost_Northing,90.0d
*GLOBAL*,project,Joint IODE/JCOMM Global Temperature-Salinity Profile Programme
*GLOBAL*,references,https://www.nodc.noaa.gov/GTSPP/
*GLOBAL*,sourceErddapVersion,2.26d
*GLOBAL*,sourceUrl,(local files)
*GLOBAL*,Southernmost_Northing,-78.579d
*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v70
*GLOBAL*,subsetVariables,"trajectory, org, type, platform, cruise"
*GLOBAL*,summary,"The Global Temperature-Salinity Profile Programme (GTSPP) develops and maintains a global ocean temperature and salinity resource with data that are both up-to-date and of the highest quality. It is a joint World Meteorological Organization (WMO) and Intergovernmental Oceanographic Commission (IOC) program.  It includes data from XBTs, CTDs, moored and drifting buoys, and PALACE floats. For information about organizations contributing data to GTSPP, see http://gosic.org/goos/GTSPP-data-flow.htm .  The U.S. National Oceanographic Data Center (NODC) maintains the GTSPP Continuously Managed Data Base and releases new 'best-copy' data once per month.\n\nWARNING: This dataset has a *lot* of data.  If you request too much data, your request will fail.\n* If you don't specify a longitude and latitude bounding box, don't request more than a month's data at a time.\n* If you do specify a longitude and latitude bounding box, you can request data for a proportionally longer time period.\nRequesting data for a specific station_id may be slow, but it works.\n\n*** This ERDDAP dataset has data for the entire world for all available times (currently, up to and including the April 2025 data) but is a subset of the original NODC 'best-copy' data.  It only includes data where the quality flags indicate the data is 1=CORRECT, 2=PROBABLY GOOD, or 5=MODIFIED. It does not include some of the metadata, any of the history data, or any of the quality flag data of the original dataset. You can always get the complete, up-to-date dataset (and additional, near-real-time data) from the source: https://www.nodc.noaa.gov/GTSPP/ .  Specific differences are:\n* Profiles with a position_quality_flag or a time_quality_flag other than 1|2|5 were removed.\n* Rows with a depth (z) value less than -0.4 or greater than 10000 or a z_variable_quality_flag other than 1|2|5 were removed.\n* Temperature values less than -4 or greater than 40 or with a temperature_quality_flag other than 1|2|5 were set to NaN.\n* Salinity values less than 0 or greater than 41 or with a salinity_quality_flag other than 1|2|5 were set to NaN.\n* Time values were converted from ""days since 1900-01-01 00:00:00"" to ""seconds since 1970-01-01T00:00:00"".\n\nSee the Quality Flag definitions on page 5 and ""Table 2.1: Global Impossible Parameter Values"" on page 61 of\nhttps://www.nodc.noaa.gov/GTSPP/document/qcmans/GTSPP_RT_QC_Manual_20090916.pdf .\nThe Quality Flag definitions are also at\nhttps://www.nodc.noaa.gov/GTSPP/document/qcmans/qcflags.htm ."
*GLOBAL*,testOutOfDate,now-45days
*GLOBAL*,time_coverage_end,2025-04-25T13:01:00Z
*GLOBAL*,time_coverage_start,1985-02-15T00:00:00Z
*GLOBAL*,title,"Global Temperature and Salinity Profile Programme (GTSPP) Data, 1985-present"
*GLOBAL*,Westernmost_Easting,-180.0d
trajectory,*DATA_TYPE*,String
trajectory,cf_role,trajectory_id
trajectory,comment,Constructed from org_type_platform_cruise
trajectory,ioos_category,Identifier
trajectory,long_name,Trajectory ID
org,*DATA_TYPE*,String
org,comment,"From the first 2 characters of stream_ident:\nCode  Meaning\nAD  Australian Oceanographic Data Centre\nAF  Argentina Fisheries (Fisheries Research and Development National Institute (INIDEP), Mar del Plata, Argentina\nAO  Atlantic Oceanographic and Meteorological Lab\nAP  Asia-Pacific (International Pacific Research Center/ Asia-Pacific Data-Research Center)\nBI  BIO Bedford institute of Oceanography\nCF  Canadian Navy\nCS  CSIRO in Australia\nDA  Dalhousie University\nFN  FNOC in Monterey, California\nFR  Orstom, Brest\nFW  Fresh Water Institute (Winnipeg)\nGE  BSH, Germany\nIC  ICES\nII  IIP\nIK  Institut fur Meereskunde, Kiel\nIM  IML\nIO  IOS in Pat Bay, BC\nJA  Japanese Meteorologocal Agency\nJF  Japan Fisheries Agency\nME  EDS\nMO  Moncton\nMU  Memorial University\nNA  NAFC\nNO  NODC (Washington)\nNW  US National Weather Service\nOD  Old Dominion Univ, USA\nRU  Russian Federation\nSA  St Andrews\nSI  Scripps Institute of Oceanography\nSO  Southampton Oceanographic Centre, UK\nTC  TOGA Subsurface Data Centre (France)\nTI  Tiberon lab US\nUB  University of BC\nUQ  University of Quebec at Rimouski\nVL  Far Eastern Regional Hydromet. Res. Inst. of V\nWH  Woods Hole\n\nfrom https://www.nodc.noaa.gov/GTSPP/document/codetbls/gtsppcode.html#ref006"
org,ioos_category,Identifier
org,long_name,Organization
type,*DATA_TYPE*,String
type,comment,"From the 3rd and 4th characters of stream_ident:\nCode  Meaning\nAR  Animal mounted recorder\nBA  BATHY message\nBF  Undulating Oceanographic Recorder (e.g. Batfish CTD)\nBO  Bottle\nBT  general BT data\nCD  CTD down trace\nCT  CTD data, up or down\nCU  CTD up trace\nDB  Drifting buoy\nDD  Delayed mode drifting buoy data\nDM  Delayed mode version from originator\nDT  Digital BT\nIC  Ice core\nID  Interpolated drifting buoy data\nIN  Ship intake samples\nMB  MBT\nMC  CTD and bottle data are mixed for the station\nMI  Data from a mixed set of instruments\nML  Minilog\nOF  Real-time oxygen and fluorescence\nPF  Profiling float\nRM  Radio message\nRQ  Radio message with scientific QC\nSC  Sediment core\nSG  Thermosalinograph data\nST  STD data\nSV  Sound velocity probe\nTE  TESAC message\nTG  Thermograph data\nTK  TRACKOB message\nTO  Towed CTD\nTR  Thermistor chain\nXB  XBT\nXC  Expendable CTD\n\nfrom https://www.nodc.noaa.gov/GTSPP/document/codetbls/gtsppcode.html#ref082"
type,ioos_category,Identifier
type,long_name,Data Type
platform,*DATA_TYPE*,String
platform,comment,See the list of platform codes (sorted in various ways) at https://www.nodc.noaa.gov/GTSPP/document/codetbls/calllist.html
platform,ioos_category,Identifier
platform,long_name,GTSPP Platform Code
platform,references,https://www.nodc.noaa.gov/gtspp/document/codetbls/callist.html
cruise,*DATA_TYPE*,String
cruise,comment,"Radio callsign + year for real time data, or NODC reference number for delayed mode data.  See\nhttps://www.nodc.noaa.gov/GTSPP/document/codetbls/calllist.html .\n'X' indicates a missing value.\nTwo or more adjacent spaces in the original cruise names have been compacted to 1 space."
cruise,ioos_category,Identifier
cruise,long_name,Cruise_ID
station_id,*DATA_TYPE*,int
station_id,_FillValue,2147483647i
station_id,actual_range,1i,58671918i
station_id,cf_role,profile_id
station_id,comment,Identification number of the station (profile) in the GTSPP Continuously Managed Database
station_id,ioos_category,Identifier
station_id,long_name,Station ID Number
station_id,missing_value,2147483647i
longitude,*DATA_TYPE*,float
longitude,_CoordinateAxisType,Lon
longitude,_FillValue,NaNf
longitude,actual_range,-180.0f,179.999f
longitude,axis,X
longitude,C_format,%9.4f
longitude,colorBarMaximum,180.0d
longitude,colorBarMinimum,-180.0d
longitude,epic_code,502i
longitude,FORTRAN_format,F9.4
longitude,ioos_category,Location
longitude,long_name,Longitude
longitude,missing_value,NaNf
longitude,standard_name,longitude
longitude,units,degrees_east
longitude,valid_max,180.0f
longitude,valid_min,-180.0f
latitude,*DATA_TYPE*,float
latitude,_CoordinateAxisType,Lat
latitude,_FillValue,NaNf
latitude,actual_range,-78.579f,90.0f
latitude,axis,Y
latitude,C_format,%8.4f
latitude,colorBarMaximum,90.0d
latitude,colorBarMinimum,-90.0d
latitude,epic_code,500i
latitude,FORTRAN_format,F8.4
latitude,ioos_category,Location
latitude,long_name,Latitude
latitude,missing_value,NaNf
latitude,standard_name,latitude
latitude,units,degrees_north
latitude,valid_max,90.0f
latitude,valid_min,-90.0f
time,*DATA_TYPE*,String
time,_CoordinateAxisType,Time
time,actual_range,1985-02-15T00:00:00Z\n2025-04-25T13:01:00Z
time,axis,T
time,ioos_category,Time
time,long_name,Time
time,standard_name,time
time,time_origin,01-JAN-1970 00:00:00
time,units,yyyy-MM-dd'T'HH:mm:ssZ
depth,*DATA_TYPE*,float
depth,_CoordinateAxisType,Height
depth,_CoordinateZisPositive,down
depth,_FillValue,NaNf
depth,actual_range,-0.4f,9910.0f
depth,axis,Z
depth,C_format,%6.2f
depth,colorBarMaximum,5000.0d
depth,colorBarMinimum,0.0d
depth,epic_code,3i
depth,FORTRAN_format,F6.2
depth,ioos_category,Location
depth,long_name,Depth of the Observations
depth,missing_value,NaNf
depth,positive,down
depth,standard_name,depth
depth,units,m
temperature,*DATA_TYPE*,float
temperature,_FillValue,NaNf
temperature,actual_range,-3.91f,40.0f
temperature,C_format,%9.4f
temperature,cell_methods,time: point longitude: point latitude: point depth: point
temperature,colorBarMaximum,32.0d
temperature,colorBarMinimum,0.0d
temperature,coordinates,time latitude longitude depth
temperature,epic_code,28i
temperature,FORTRAN_format,F9.4
temperature,ioos_category,Temperature
temperature,long_name,Sea Water Temperature
temperature,missing_value,NaNf
temperature,standard_name,sea_water_temperature
temperature,units,degree_C
salinity,*DATA_TYPE*,float
salinity,_FillValue,NaNf
salinity,actual_range,0.0f,41.0f
salinity,C_format,%9.4f
salinity,cell_methods,time: point longitude: point latitude: point depth: point
salinity,colorBarMaximum,37.0d
salinity,colorBarMinimum,32.0d
salinity,coordinates,time latitude longitude depth
salinity,epic_code,41i
salinity,FORTRAN_format,F9.4
salinity,ioos_category,Salinity
salinity,long_name,Practical Salinity
salinity,missing_value,NaNf
salinity,salinity_scale,PSU
salinity,standard_name,sea_water_practical_salinity
salinity,units,PSU

*END_METADATA*
trajectory,org,type,platform,cruise,station_id,longitude,latitude,time,depth,temperature,salinity
*END_DATA*
