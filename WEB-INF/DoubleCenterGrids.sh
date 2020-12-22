#!/bin/bash
#
# usage: ./DoubleCenterGrids <oldEndTimeDir> <newCenteredTimeDir> [-fast]
# For example: DoubleCenterGrids /u00/satellite/avhrr_hrpt /u00/satellite/centeredavhrr_hrpt
# Or, run with no parameters to see documentation.
#
# To make this Linux/Unix script executable, use 
#    chmod +x CenterGrids
#
# original 2007-01-23   Bob Simons bob.simons@noaa.gov
# updated 2014-02-13  Bob Simons bob.simons@noaa.gov
#

java -cp classes:../../../lib/servlet-api.jar:lib/* -Xms1000M -Xmx1000M gov.noaa.pfel.coastwatch.griddata.DoubleCenterGrids "$@"

# finished 
