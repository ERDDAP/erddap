#!/bin/bash
# This is the Unix/Linux shell script to run GenerateDatasetsXml.
# See http://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html#Tools

java -cp classes:../../../lib/servlet-api.jar:lib/* -Xms1000M -Xmx1000M gov.noaa.pfel.erddap.GenerateDatasetsXml "$@"
