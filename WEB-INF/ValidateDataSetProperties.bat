rem @echo off
rem
rem usage: ValidateDataSetProperties
rem
rem ValidateDataSetProperties makes sure that DataSet.properties has all of 
rem the required information for each of the data sets listed in 
rem DataSet.properties' validDataSets.
rem Don't run this on the coastwatch computer.
rem
rem 2006-10-09
rem NOAA/Bob Simons  bob.simons@noaa.gov
rem

set thisDir=%~dp0
java -cp %thisDir%/classes;%thisDir%/lib/netcdfAll-latest.jar;%thisDir%/lib/slf4j-jdk14.jar;%thisDir%/lib/activation.jar;%thisDir%/lib/mail.jar -Xms1000M -Xmx1000M gov.noaa.pfel.coastwatch.ValidateDataSetProperties


