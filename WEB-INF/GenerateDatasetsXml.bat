rem This is the Windows batch file to run GenerateDatasetsXml.
rem See http://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html#Tools

java -cp ./classes;../../../lib/servlet-api.jar;lib/activation.jar;lib/axis.jar;lib/commons-compress.jar;lib/commons-discovery.jar;lib/itext-1.3.1.jar;lib/jaxrpc.jar;lib/joda-time.jar;lib/joid.jar;lib/lucene-core.jar;lib/mail.jar;lib/netcdfAll-latest.jar;lib/postgresql.jdbc.jar;lib/saaj.jar;lib/slf4j-jdk14.jar;lib/tsik.jar;lib/wsdl4j.jar  -Xms1000M -Xmx1000M  gov.noaa.pfel.erddap.GenerateDatasetsXml %1 %2 %3 %4 %5 %6 %7 %8 %9