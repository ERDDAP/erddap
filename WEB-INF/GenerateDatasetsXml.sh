#!/bin/bash
# This is the Unix/Linux shell script to run GenerateDatasetsXml.
# See http://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html#Tools

cp1="./classes:../../../lib/servlet-api.jar:lib/activation.jar:lib/axis.jar"
cp2=":lib/cassandra-driver-core.jar:lib/netty-all.jar:lib/guava.jar:lib/metrics-core.jar:lib/lz4.jar:lib/snappy-java.jar"
cp3=":lib/commons-compress.jar:lib/commons-discovery.jar:lib/itext-1.3.1.jar"
cp4=":lib/jaxrpc.jar:lib/joda-time.jar:lib/joid.jar:lib/lucene-core.jar"
cp5=":lib/mail.jar:lib/netcdfAll-latest.jar:lib/postgresql.jdbc.jar"
cp6=":lib/saaj.jar:lib/tsik.jar:lib/wsdl4j.jar"
cp0="$cp1$cp2$cp3$cp4$cp5$cp6"

java -cp $cp0 -Xms1000M -Xmx1000M gov.noaa.pfel.erddap.GenerateDatasetsXml $1 $2 $3 $4 $5 $6 $7 $8 $9
