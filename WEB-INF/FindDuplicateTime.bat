rem This is run on a Windows computer to find gridded .nc files with duplicate time values.
rem An assumption is that there is just one time value per file.
rem This standardizes the time values to "seconds since 1970-01-01" for the test.
rem Parameters: directory fileNameRegex timeVarName
rem This is recursive.
rem Written by Bob Simons  original 2021-01-20

C:\programs\jdk8u265-b01\bin\java.exe -cp classes;../../../lib/servlet-api.jar;lib/* -Xms1300M -Xmx1300M -verbose:gc gov.noaa.pfel.erddap.dataset.FindDuplicateTime %*
