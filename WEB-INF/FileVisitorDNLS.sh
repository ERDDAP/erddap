#!/bin/bash
# This is the Unix/Linux shell script to run FileVisitorDNLS.

cp1="./classes:../../../lib/servlet-api.jar:lib/activation.jar:lib/axis.jar"
cp2=":lib/cassandra-driver-core.jar:lib/netty-all.jar:lib/guava.jar:lib/metrics-core.jar:lib/lz4.jar:lib/snappy-java.jar"
cp3=":lib/commons-compress.jar:lib/commons-discovery.jar:lib/itext-1.3.1.jar"
cp4=":lib/jaxrpc.jar:lib/joda-time.jar:lib/lucene-core.jar"
cp5=":lib/mail.jar:lib/netcdfAll-latest.jar:lib/slf4j.jar:lib/postgresql.jdbc.jar"
cp6=":lib/saaj.jar:lib/wsdl4j.jar"
cp7=":lib/aws-java-sdk.jar:lib/commons-codec.jar:lib/commons-logging.jar"
cp8=":lib/fluent-hc.jar:lib/httpclient.jar:lib/httpclient-cache.jar:lib/httpcore.jar"
cp9=":lib/httpmime.jar:lib/jna.jar:lib/jna-platform.jar:lib/jackson-annotations.jar"
cp10=":lib/jackson-core.jar:lib/jackson-databind.jar"
cp0="$cp1$cp2$cp3$cp4$cp5$cp6$cp7$cp8$cp9$cp10"

/usr/local/jre1.8.0_73/bin/java -cp $cp0 -Xms1000M -Xmx1000M gov.noaa.pfel.coastwatch.util.FileVisitorDNLS "$@"
