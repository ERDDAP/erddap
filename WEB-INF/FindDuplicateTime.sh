#!/bin/bash
# This is run on a Linux computer to find gridded .nc files with duplicate time values.
# An assumption is that there is just one time value per file.
# This standardizes the time values to "seconds since 1970-01-01" for the test.
# Parameters: directory fileNameRegex timeVarName
# This is recursive.
# Written by Bob Simons  original 2021-01-20

java -cp classes:../../../lib/servlet-api.jar:lib/* -Xms1300M -Xmx1300M -verbose:gc gov.noaa.pfel.erddap.dataset.FindDuplicateTime "$@"
