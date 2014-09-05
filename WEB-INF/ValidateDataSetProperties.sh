#!/bin/bash
#
# usage: ValidateDataSetProperties
#
# ValidateDataSetProperties makes sure that DataSet.properties has all of 
# the required information for each of the data sets listed in 
# DataSet.properties' validDataSets.
# Don't run this on the coastwatch computer.
#
# To make this Linux/Unix script executable, use "chmod +x ValidateDataSetProperties".
#
# original 2006-10-09  Bob Simons bob.simons@noaa.gov
# updated 2014-02-13  Bob Simons bob.simons@noaa.gov
#

commandLine=$0
tDir=`echo $commandLine | dirname $0`
cp1="$tDir/classes:$tDir/lib/netcdfAll-latest.jar:"
cp2="$tDir/lib/activation.jar:$tDir/lib/lucene-core.jar:$tDir/lib/mail.jar"
cp0="$cp1$cp2"

java -cp $cp0 -Xms1000M -Xmx1000M gov.noaa.pfel.coastwatch.ValidateDataSetProperties

# finished 
