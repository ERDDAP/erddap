rem @echo off
rem usage: GridSaveAs <in> <out>  
rem <in> and <out> must be the complete directory + name + extension.
rem For whole directories, don't supply the name part of the <in> and <out>.
rem <in> and <out> may also have a .zip or .gz extension.
rem
rem For more info, use: ./GridSaveAs
rem
rem This script converts a grid data file (or files) from one type to another.
rem
rem A test which can be run from the GridSaveAs.bat file directory is
rem   GridSaveAs QN2005193_2005193_ux10_westus.grd  QN2005193_2005193_ux10_westus.nc 
rem
rem 2005-12-02
rem CoastWatch/Bob Simons  bob.simons@noaa.gov
rem

set thisDir=%~dp0
java -cp %thisDir%/classes;%thisDir%/lib/netcdfAll-latest.jar;lib/slf4j.jar;%thisDir%/lib/activation.jar;%thisDir%/lib/lucene-core.jar;%thisDir%/lib/mail.jar -Xms1000M -Xmx1000M gov.noaa.pfel.coastwatch.griddata.GridSaveAs %*
