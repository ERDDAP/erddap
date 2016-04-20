#!/bin/bash
# This is the Unix/Linux shell script to run HashDigest.
# See http://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html#HashDigest

java -cp ./classes -Xms1000M -Xmx1000M gov.noaa.pfel.erddap.DasDds $1 $2 $3 $4 $5 $6 $7 $8 $9
java -cp ./classes com.cohort.util.HashDigest %1 %2 %3 %4 %5 %6 %7 %8 %9