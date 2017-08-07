#!/bin/bash
#
# usage: ./GenerateOtterThreddsXml
#
# GenerateOtterThreddsXml is a version of GenerateThreddsXml specifically
# for use on the otter computer at ERD.
# It specifies all the paramaters, so you don't need to supply any.
# See GenerateThreddsXml for more general information.
#
# The four parameters which are hard-coded below are:
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
#      the results.
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
#    chmod +x GenerateOtterThreddsXml
#
# original 2007-05-25  Bob Simons bob.simons@noaa.gov
# updated 2014-02-13  Bob Simons bob.simons@noaa.gov
#

commandLine=$0
thisDir=`echo $commandLine | dirname $0`
dataMainDir=/u00/ 
dataSubDir=satellite/ 
incomplete=/opt/tomcat1/content/thredds/GeneratedXml/incompleteMainCatalog.xml 
xmlMainDir=/opt/tomcat1/content/thredds/GeneratedXml

java -cp classes:lib/netcdfAll-latest.jar:lib/slf4j.jar:lib/lucene-core.jar:lib/mail.jar -Xms1000M -Xmx1000M gov.noaa.pfel.coastwatch.griddata.GenerateThreddsXml $dataMainDir $dataSubDir $incomplete $xmlMainDir


# finished 
