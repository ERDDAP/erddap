rem This is the Windows batch file to run ArchiveADataset.
rem See http://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html#Tools

rem You'll need to change java's path to make this work:
C:\programs\jdk8u292-b10\bin\java.exe -cp classes;../../../lib/servlet-api.jar;lib/* -Xms1500M -Xmx1500M  gov.noaa.pfel.erddap.ArchiveADataset %*