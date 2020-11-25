rem This is the Windows batch file to run GenerateDatasetsXml.
rem See http://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html#Tools

rem You'll need to change java's path to make this work:
C:\programs\jdk8u265-b01\bin\java.exe -cp ./classes;../../../lib/servlet-api.jar;lib/cassandra-driver-core.jar;lib/netty-all.jar;lib/guava.jar;lib/metrics-core.jar;lib/lz4.jar;lib/snappy-java.jar;lib/commons-compress.jar;lib/commons-jexl.jar;lib/itext-1.3.1.jar;lib/lucene-core.jar;lib/mail.jar;lib/netcdfAll-latest.jar;lib/slf4j.jar;lib/postgresql.jdbc.jar;lib/commons-jexl.jar;lib/aws-java-sdk.jar;lib/jackson-annotations.jar;lib/jackson-core.jar;lib/jackson-databind.jar -Xms1000M -Xmx1000M  gov.noaa.pfel.erddap.GenerateDatasetsXml %*
