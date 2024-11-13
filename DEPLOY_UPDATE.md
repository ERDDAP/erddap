[How To Do an Update of an Existing ERDDAP™ on Your Server](#update)
-------------------------------------------------------------------

1.  Make the changes listed in [Changes](https://erddap.github.io/changes.html) in the section entitled "Things ERDDAP™ Administrators Need to Know and Do" for all of the ERDDAP™ versions since the version you were using.  
     
2.  If you are upgrading from ERDDAP™ version 2.18 or below, you need to switch to Java 21 (or newer) and the related Tomcat 10. See the regular ERDDAP™ installation instructions for [Java](#java) and [Tomcat](#tomcat). You'll also have to copy your _tomcat_/content/erddap directory from your old Tomcat installation to your new Tomcat installation.  
     
3.  Download [erddap.war](https://github.com/ERDDAP/erddap/releases/download/v2.25.1/erddap.war) into _tomcat_/webapps .  
    (version 2.25_1, 592,292,039 bytes, MD5=0F0C14FD22DF80D0E867A4B96ED7F3FF, dated 2024-11-07)
     
4.  [messages.xml](#messages.xml)
    *   Common: If you are upgrading from ERDDAP™ version 1.46 (or above) and you just use the standard messages, the new standard messages.xml will be installed automatically (amongst the .class files via erddap.war).  
         
    *   Rare: If you are upgrading from ERDDAP™ version 1.44 (or below),  
        you MUST delete the old messages.xml file:  
        _tomcat_/content/erddap/messages.xml .  
        The new standard messages.xml will be installed automatically (amongst the .class files via erddap.war).  
         
    *   Rare: If you always make changes to the standard messages.xml file (in place),  
        you need to make those changes to the new messages.xml file (which is  
        WEB-INF/classes/gov/noaa/pfel/erddap/util/messages.xml after erddap.war is decompressed by Tomcat).  
         
    *   Rare: If you maintain a custom messages.xml file in _tomcat_/content/erddap/,  
        you need to figure out (via diff) what changes have been made to the default messages.xml (which are in the new erddap.war as  
        WEB-INF/classes/gov/noaa/pfel/erddap/util/messages.xml) and modify your custom messages.xml file accordingly.  
         
5.  Install the new ERDDAP™ in Tomcat:  
    \* Don't use Tomcat Manager. Sooner or later there will be PermGen memory issues. It is better to actually shutdown and startup Tomcat.  
    \* Replace references to _tomcat_ below with the actual Tomcat directory on your computer.  
     
    *   For Linux and Macs:
        1.  Shutdown Tomcat: From a command line, use: _tomcat_/bin/shutdown.sh  
            And use ps -ef | grep tomcat to see if/when the process has been stopped. (It may take a minute or two.)
        2.  Remove the decompressed ERDDAP™ installation: In _tomcat_/webapps, use  
            rm -rf erddap
        3.  Delete the old erddap.war file: In _tomcat_/webapps, use rm erddap.war
        4.  Copy the new erddap.war file from the temporary directory to _tomcat_/webapps
        5.  Restart Tomcat and ERDDAP: use _tomcat_/bin/startup.sh
        6.  View ERDDAP™ in your browser to check that the restart succeeded.  
            (Often, you have to try a few times and wait a minute before you see ERDDAP™.)  
             
    *   For Windows:
        1.  Shutdown Tomcat: From a command line, use: _tomcat_\\bin\\shutdown.bat
        2.  Remove the decompressed ERDDAP™ installation: In _tomcat_/webapps, use  
            del /S/Q erddap
        3.  Delete the old erddap.war file: In _tomcat_\\webapps, use del erddap.war
        4.  Copy the new erddap.war file from the temporary directory to _tomcat_\\webapps
        5.  Restart Tomcat and ERDDAP: use _tomcat_\\bin\\startup.bat
        6.  View ERDDAP™ in your browser to check that the restart succeeded.  
            (Often, you have to try a few times and wait a minute before you see ERDDAP™.)

Troubles updating ERDDAP?  
Email me at erd dot data at noaa dot gov . I will help you.  
Or, you can join the [ERDDAP™ Google Group / Mailing List](#ERDDAPMailingList) and post your question there.
