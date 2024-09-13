rem Bob Simons uses this to create erddap.war.
rem The specific files in erddap.war do change occasionally.
rem The directories are specific to Bob's computer. Other people will need to change them.
rem The starting dir is important.
rem For everyone but Bob, the cwexperimental directory will instead be called 'erddap'.
c: 
cd \programs\_tomcat\webapps\cwexperimental
rem Name of war file will be name of webapp directory.
rem Delete files in temp directories
del /q public\*.*
del /q WEB-INF\temp\*.*
del /q WEB-INF\classes\gov\noaa\pfel\coastwatch\log.*
del /q WEB-INF\NetCheck.xml.log
copy WEB-INF\erddap.web.xml WEB-INF\web.xml
C:\programs\jdk-17.0.3+7\bin\jar.exe -cvf /backup/erddap.war download/ images/orcid_24x24.png images/data.gif images/envelope.gif images/external.png images/favicon.ico images/fileIcons images/nlogo.gif images/noaa_simple.gif images/noaab.png images/noaa20.gif images/openid.png images/QuestionMark.png images/resize.gif images/x.gif images/arrow2R.gif images/arrow2L.gif images/arrow2U.gif images/arrow2D.gif images/arrowR.gif images/arrowRR.gif images/arrowL.gif images/arrowLL.gif images/arrowU.gif images/arrowUU.gif images/arrowD.gif images/arrowDD.gif images/erddap.css images/erddap2.css images/leaflet.css images/leaflet.js images/loading.png images/plus.gif images/plusplus.gif images/plus10.gif images/plus100.gif images/plus1000.gif images/minus.gif images/minusminus.gif images/minus10.gif images/minus100.gif images/minus1000.gif images/rss.gif images/sliderBg.gif images/sliderCenter.gif images/sliderLeft.gif images/sliderRight.gif images/spacer.gif images/world540.png images/world0360.png images/worldPM180.png images/world540Big.png images/world0360Big.png images/worldPM180Big.png images/wz_dragdrop.js images/TranslatedByGoogle.png images/wz_tooltip.js images/youtube.png public/ WEB-INF/ArchiveADataset.sh WEB-INF/ArchiveADataset.bat WEB-INF/DasDds.sh WEB-INF/DasDds.bat WEB-INF/ConvertTable.sh WEB-INF/ConvertTable.bat WEB-INF/GenerateDatasetsXml.sh WEB-INF/GenerateDatasetsXml.bat WEB-INF/HashDigest.sh WEB-INF/HashDigest.bat WEB-INF/web.xml WEB-INF/classes/ WEB-INF/cptfiles/ WEB-INF/lib/ WEB-INF/ref/ WEB-INF/temp/ src/main/resources/

cd \content\bat
rem To deploy, put the .war file in tomcat\webapps on the remote computer.
rem When Tomcat is restarted, the files are deployed.

