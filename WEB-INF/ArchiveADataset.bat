rem This is the Windows batch file to run ArchiveADataset.
rem See http://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html#Tools

java -cp ./classes;../../../lib/servlet-api.jar;lib/cassandra-driver-core.jar;lib/netty-all.jar;lib/guava.jar;lib/metrics-core.jar;lib/lz4.jar;lib/snappy-java.jar;lib/commons-compress.jar;lib/commons-jexl.jar;lib/itext-1.3.1.jar;lib/lucene-core.jar;lib/mail.jar;lib/netcdfAll-latest.jar;lib/slf4j.jar;lib/postgresql.jdbc.jar -Xms1500M -Xmx1500M  gov.noaa.pfel.erddap.ArchiveADataset %*