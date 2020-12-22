#!/bin/bash
# This is the Unix/Linux shell script to run ArchiveADataset.
# See http://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html#Tools

java -cp classes:../../../lib/servlet-api.jar:lib/* -Xms1500M -Xmx1500M gov.noaa.pfel.erddap.ArchiveADataset "$@"
