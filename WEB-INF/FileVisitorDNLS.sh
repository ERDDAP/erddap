#!/bin/bash
# This is the Unix/Linux shell script to run FileVisitorDNLS.

/usr/local/jre1.8.0_73/bin/java -cp classes:../../../lib/servlet-api.jar:lib/* -Xms1000M -Xmx1000M gov.noaa.pfel.coastwatch.util.FileVisitorDNLS "$@"
