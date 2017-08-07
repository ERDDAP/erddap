#!/bin/bash
#
# usage: ./GenerateThreddsXml <dataMainDir> <dataSubDir> <incompletMainCatalog> <xmlMainDir>
#
# The program generates two directories (Satellite and Hfradar) in xmlMainDir which
#   which have the dataset catalog.xml files for all two-letter satellite
#   and HF Radar data sets, using info in the
#   gov/noaa/pfel/coastwatch/DataSet.properties file.
# It also generates catalog.xml in xmlMainDir by inserting dataset 
#   tags into the incompleteMainCatalog.
# This uses info in gov/noaa/pfel/coastwatch/DataSet.properties file.
#
# The command line should have four parameters:
#   'dataMainDir': the base data directory, e.g., /u00/ .
#   'dataSubDir': the subdirectory of dataMainDir, 
#      e.g., satellite/, which has subdirectories
#      like AT/ssta/1day with .nc data files.
#      Only the dataSubDir (not the dataMainDir) is used for the 
#      catalog.xml file's ID and urlPath attributes of the 'dataset' tag.
#   'incompleteMainCatalog' is the full name of the 
#      incomplete main catalog.xml file which has "[Insert datasets here.]"
#      where the satellite and HF Radar dataset tags with the inserted.
#   'xmlMainDir': the directory which will be created to hold 
#      the results: e.g., /home/cwatch/bin/xml/ during development,
#      but perhaps someday /opt/tomcat1/content/thredds/ .
#      It will have a new main catalog.xml file and subdirectories
#      with the new dataset catalog.xml files. 
#      Datasets with 'C' as the first letter of the twoName
#      are assumed to be HF Radar datasets and their catalog.xml files are 
#      stored in the Hfradar/aggreghfradar'twoName' subdirectory of xmlMainDir.
#      All other datasets 
#      are assumed to be Satellite datasets and their catalog.xml files are 
#      stored in the Satellite/aggregsat'twoName' subdirectory of xmlMainDir.
#      Only this subdirectory name (not the xmlMainDir) appears in the 
#      resulting main catalog.xml's xlink:href attribute of the catalogRef tag.
#
# To make this Linux/Unix script executable, use 
#    chmod +x GenerateThreddsDataSetHtml
#
# original 2006-10-04  Bob Simons bob.simons@noaa.gov
# updated 2014-02-13  Bob Simons bob.simons@noaa.gov
#

commandLine=$0
thisDir=`echo $commandLine | dirname $0`
java -cp classes:lib/netcdfAll-latest.jar:lib/slf4j.jar:lib/lucene-core.jar:lib/mail.jar -Xms1000M -Xmx1000M gov.noaa.pfel.coastwatch.griddata.GenerateThreddsXml "$@"

# finished 
