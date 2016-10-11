#!/bin/bash
# This is the Unix/Linux shell script to start up NetCheck.
# To make this Linux/Unix script executable, use "chmod +x NetCheck".
# Modify NetCheck.xml to suit your needs before running this.
# Then run "./NetCheck" from a command prompt.
# Use "./NetCheck -testMode" to just send emails to the NetCheck administrator.

commandLine=$0
tDir=`echo $commandLine | dirname $0`
cp1="classes:lib/lucene-core.jar:lib/mail.jar:"
cp2="lib/netcdfAll-latest.jar:lib/slf4j.jar:lib/activation.jar"
cp0="$cp1$cp2"

java -cp $cp0 -Xms1000M -Xmx1000M gov.noaa.pfel.coastwatch.netcheck.NetCheck NetCheck.xml "$@"
