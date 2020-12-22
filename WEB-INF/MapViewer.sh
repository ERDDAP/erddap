#!/bin/bash
#
# usage: ./MapViewer <in>  
# <in> must be the complete directory + name + extension of a
# .hdf, .nc, or .grd gridded data file, where the x and y values represent
# lon and lat.
# The file can be zipped, but the name of the .zip file must be the
# name of the data file + ".zip".
#
# This script reads the data, plots it on a map, and pops up a JFrame
# to display the image.
# To make this Linux/Unix script executable, use "chmod +x MapViewer".
#
# original 2006-03-14  Bob Simons bob.simons@noaa.gov
# updated 2014-02-13  Bob Simons bob.simons@noaa.gov
#

commandLine=$0
tDir=`echo $commandLine | dirname $0`

java -cp classes:../../../lib/servlet-api.jar:lib/* -Xms1000M -Xmx1000M gov.noaa.pfel.coastwatch.sgt.SgtMap "$@"

# finished 
