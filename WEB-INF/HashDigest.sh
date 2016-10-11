#!/bin/bash
# This is the Unix/Linux shell script to run HashDigest.
# See http://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html#HashDigest

java -cp ./classes com.cohort.util.HashDigest "$@"
