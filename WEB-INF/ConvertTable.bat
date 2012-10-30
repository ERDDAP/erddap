@echo off
rem usage: ConvertTable <in> <inType> <out> <outType> 
rem <in> must be the complete directory + name + extension
rem    or the complete url for an opendap sequence.
rem <inType> maybe 0 (ASCII), 1 (.nc), or 2 (opendapSequence).
rem <out> must be the complete directory + name + extension.
rem <outType> may be 0 (tabbed ASCII), or 1 (.nc).
rem <dimensionName> The name for the rows, e.g., "time", "row", or "station".
rem
rem For more info, use: ./ConvertTable
rem
rem This script converts a tabular data file from one type to another.
rem
rem A test which reads data from an opendap sequence and writes it to an .nc file: 
rem ConvertTable "http://coastwatch.pfeg.noaa.gov/erddap/tabledap/erdGlobecBottle?longitude,latitude,time,sal00,temperature0&amp;time=2002-08-19T08:58:00" 2 result.nc 1 row
rem
rem 2006-02-14
rem CoastWatch/Bob Simons  bob.simons@noaa.gov
rem

set thisDir=%~dp0
java -cp %thisDir%/classes;%thisDir%/lib/netcdfAll-latest.jar;%thisDir%/lib/slf4j-jdk14.jar -Xms1000M -Xmx1000M gov.noaa.pfel.coastwatch.pointdata.ConvertTable %1 %2 %3 %4 %5

rem finished 
