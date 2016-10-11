rem This is the Windows batch file to run HashDigest.
rem See http://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html#HashDigest

java -cp ./classes com.cohort.util.HashDigest %*
