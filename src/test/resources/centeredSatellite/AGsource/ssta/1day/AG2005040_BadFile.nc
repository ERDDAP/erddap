c: 
cd \programs\tomcat\webapps\cwexperimental
rem Delete files in temp directories
del /q CWBrowser\*.*
del /q WEB-INF\temp\*.*
del /q WEB-INF\classes\gov\noaa\pfel\coastwatch\log.*
cd \content\bat
c:\programs\jdk1506\bin\jar cvf c:\backup\BackupContent%1.jar \content \programs\tomcat\webapps\coastwatch  \programs\tomcat\webapps\cwexperimental \programs\editplus\tool.ini
