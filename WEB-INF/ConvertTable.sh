#!/bin/bash
#
# usage: ./ConvertTable <in> <inType> <out> <outType> 
# <in> must be the complete directory + name + extension
#    or the complete url for an opendap sequence.
# <inType> maybe 0 (ASCII), 1 (.nc), or 2 (opendapSequence).
# <out> must be the complete directory + name + extension.
# <outType> may be 0 (tabbed ASCII), or 1 (.nc).
# <dimensionName> The name for the rows, e.g., "time", "row", or "station".
#
# For more info, use: ./ConvertTable
#
# This script converts a tabular data file from one type to another.
# To make this Linux/Unix script executable, use "chmod +x ConvertTable".
#
# A test which reads data from an opendap sequence and writes it to an .nc file: 
# ./ConvertTable "http://coastwatch.pfeg.noaa.gov/erddap/tabledap/erdGlobecBottle?longitude,latitude,time,sal00,temperature0&amp;time=2002-08-19T08:58:00" 2 result.nc 1 row
#
# original 2006-12-14  Bob Simons bob.simons@noaa.gov
# updated 2014-02-13  Bob Simons bob.simons@noaa.gov
#

commandLine=$0
thisDir=`echo $commandLine | dirname $0`
cp0="classes:lib/netcdfAll-latest.jar:lib/slf4j.jar"

java -cp $cp0 -Xms1000M -Xmx1000M gov.noaa.pfel.coastwatch.pointdata.ConvertTable "$@"

# finished 
