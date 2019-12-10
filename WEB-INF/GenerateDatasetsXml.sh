#!/bin/bash
# This is the Unix/Linux shell script to run GenerateDatasetsXml.
# See http://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html#Tools

cp1="./classes:../../../lib/servlet-api.jar:lib/cassandra-driver-core.jar:lib/netty-all.jar"
cp2=":lib/guava.jar:lib/metrics-core.jar:lib/lz4.jar:lib/snappy-java.jar"
cp3=":lib/commons-compress.jar:lib/commons-jexl.jar:lib/itext-1.3.1.jar:lib/lucene-core.jar"
cp4=":lib/mail.jar:lib/netcdfAll-latest.jar:lib/slf4j.jar:lib/postgresql.jdbc.jar"
cp0="$cp1$cp2$cp3$cp4"

java -cp $cp0 -Xms1000M -Xmx1000M gov.noaa.pfel.erddap.GenerateDatasetsXml "$@"
