rem This is the Windows .bat file to start up NetCheck.
rem Modify NetCheck.xml to suit your needs before running this.
rem Then run this from an MS-DOS Prompt (command line) window.
# Use "NetCheck -testMode" to just send emails to the NetCheck administrator.

set thisDir=%~dp0
java -cp %thisDir%/;%thisDir%/classes;%thisDir%/lib/lucene-core.jar;%thisDir%/lib/mail.jar;%thisDir%/lib/netcdfAll-latest.jar;%thisDir%/lib/activation.jar -Xms1000M -Xmx1000M gov.noaa.pfel.coastwatch.netcheck.NetCheck NetCheck.xml %1
