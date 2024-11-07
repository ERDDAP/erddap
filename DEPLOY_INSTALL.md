[How To Do the Initial Setup of ERDDAP™ on Your Server](#initialSetup)
---------------------------------------------------------------------

ERDDAP™ can run on any server that supports Java and Tomcat (and other application servers like Jetty, but we don't support them). ERDDAP™ has been tested on Linux (including on Amazon's AWS), Mac, and Windows computers.

*   **Amazon** -- If you are installing ERDDAP™ on an Amazon Web Services EC2 instance, see this [Amazon Web Services Overview](#amazon) (below) first.
*   **Docker** -- Axiom now offers [ERDDAP™ in a Docker container](https://hub.docker.com/u/axiom/) and IOOS now offers a [Quick Start Guide for ERDDAP™ in a Docker Container](https://ioos.github.io/erddap-gold-standard/index.html).  
    It's the standard ERDDAP™ installation, but Axiom has put it in a docker container.  
    If you already use Docker, you will probably prefer the Docker version.  
    If you don't already use Docker, we generally don't recommend this.  
    If you chose to install ERDDAP™ via Docker, we don't offer any support for the installation process.  
    We haven't worked with Docker yet. If you work with this, please send us your comments.
*   **Linux and Macs** -- ERDDAP™ works great on Linux and Mac computers. See the instructions below.
*   **Windows** -- Windows is fine for testing ERDDAP™ and for personal use (see the instructions below), but we don't recommend using it for public ERDDAPs. Running ERDDAP™ on Windows may have problems: notably, ERDDAP™ may be unable to delete and/or rename files quickly. This is probably due to antivirus software (e.g., from McAfee and Norton) which is checking the files for viruses. If you run into this problem (which can be seen by error messages in the [log.txt](#log) file like "Unable to delete ..."), changing the antivirus software's settings may partially alleviate the problem. Or consider using a Linux or Mac server instead.

**The standard ERDDAP™ installation instructions for Linux, Macs, and Windows computers are:**

0. Make sure any dependencies are installed. On non-Windows machines (Linux and Mac), you need csh.

1.  [For ERDDAP™ v2.19+, set up Java 21.](#java)  
    For security reasons, it is almost always best to use the latest version of Java 21.  
    Please download and install the latest version of  
    [Adoptium's OpenJDK (Temurin) 21 (LTS)](https://adoptium.net/temurin/releases/?version=21). To verify the installation, type "/_javaJreBinDirectory_/java -version", for example  
    /usr/local/jdk-21.0.3+9/jre/bin/java -version
    
    \[For ERDDAP™ versions before v2.19, use Java 8.\]
    
    ERDDAP™ works with Java from other sources, but we recommend Adoptium because it is the main, community-supported, free (as in beer and speech) version of Java 21 that offers Long Term Support (free upgrades for many years past the initial release). For security reasons, please update your ERDDAP's version of Java periodically as new versions of Java 21 become available from Adoptium.
    
    ERDDAP™ has been tested and used extensively with 21, not other versions. For various reasons, we don't test with nor support other versions of Java.  
     
    
2.  [Set up](#tomcat) [Tomcat](https://tomcat.apache.org).  
    Tomcat is the most widely used Java Application Server, which is Java software that stands between the operating system's network services and Java server software like ERDDAP™. It is Free and Open Source Software (FOSS).
    
    You can use another Java Application Server (e.g., Jetty), but we only test with and support Tomcat.  
     
    
    *   Download Tomcat and unpack it on your server or PC.  
        For security reasons, it is almost always best to use the latest version of Tomcat 10 (version 9 and below are not acceptable) which is designed to work with Java 21 or newer. Below, the Tomcat directory will be referred to as _tomcat_.
        
        Warning! If you already have a Tomcat running some other web application (especially THREDDS), we recommend that you install ERDDAP™ in [a second Tomcat](#secondTomcat), because ERDDAP™ needs different Tomcat settings and shouldn't have to contend with other applications for memory.
        
        *   On Linux, [download the "Core" "tar.gz" Tomcat distribution](https://tomcat.apache.org/download-10.cgi) and unpack it. We recommend unpacking it in /usr/local.
        *   On a Mac, Tomcat is probably already installed in /Library/Tomcat, but should update it to the latest version of Tomcat 10.  
            If you download it, [download the "Core" "tar.gz" Tomcat distribution](https://tomcat.apache.org/download-10.cgi) and unpack it in /Library/Tomcat.
        *   On Windows, you can [download the "Core" "zip" Tomcat distribution](https://tomcat.apache.org/download-10.cgi) (which doesn't mess with the Windows registry and which you control from a DOS command line) and unpack it in an appropriate directory. (For development, we use the "Core" "zip" distribution. We make a /programs directory and unpack it there.) Or you can download the "Core" "64-bit Windows zip" distribution, which includes more features. If the distribution is a Windows installer, it will probably put Tomcat in, for example, /Program Files/apache-tomcat-10.0.23 .  
             
    *   [server.xml](#server.xml) - In _tomcat_/conf/server.xml file, there are two changes that you should make to each of the two <Connector> tags (one for '<Connector port="8080" ' and one for '<Connector port="8443" '):
        1.  (Recommended) Increase the connectionTimeout parameter value, perhaps to 300000 (milliseconds) (which is 5 minutes).
        2.  (Recommended) Add a new parameter: relaxedQueryChars="\[\]|" This is optional and slightly less secure, but removes the need for users to percent-encode these characters when they occur in the parameters of a user's request URL.  
             
    *   [context.xml -- Resources Cache](#ResourcesCache) - In _tomcat_/conf/context.xml, right before the </Context> tag, change the Resources tag (or add it if it isn't already there) to set the cacheMaxSize parameter to 80000:  
        <Resources cachingAllowed="true" cacheMaxSize="80000" />  
        This avoids numerous warnings in catalina.out that all start with  
        "WARNING \[main\] org.apache.catalina.webresources.Cache.getResource Unable to add the resource at \[/WEB-INF/classes/..."  
         
    *   [On Linux computers, change the Apache timeout settings](#ApacheTimeout) so that time-consuming user requests don't timeout (with what often appears as a "Proxy" or "Bad Gateway" error). As the root user:
        1.  Modify the Apache httpd.conf file (usually in /etc/httpd/conf/ ):  
            Change the existing <Timeout> setting (or add one at the end of the file) to 3600 (seconds), instead of the default 60 or 120 seconds.  
            Change the existing <ProxyTimeout> setting (or add one at the end of the file) to 3600 (seconds), instead of the default 60 or 120 seconds.
        2.  Restart Apache: /usr/sbin/apachectl -k graceful (but sometimes it is in a different directory).  
             
    *   Security recommendation: See [these instructions](https://tomcat.apache.org/tomcat-10.0-doc/security-howto.html) to increase the security of your Tomcat installation, especially for public servers.  
         
    *   For public ERDDAP™ installations on Linux and Macs, it is best to set up Tomcat (the program) as belonging to user "tomcat" (a separate user with limited permissions and which [has no password](https://unix.stackexchange.com/questions/56765/creating-an-user-without-a-password)). Thus, only the super user can switch to acting as user tomcat. This makes it impossible for hackers to log in to your server as user tomcat. And in any case, you should make it so that the tomcat user has very limited permissions on the server's file system (read+write+execute privileges for the apache-tomcat directory tree and <bigParentDirectory> and read-only privileges for directories with data that ERDDAP™ needs access to).
        *   You can create the tomcat user account (which has no password) by using the command  
            sudo useradd tomcat -s /bin/bash -p '\*'
        *   You can switch to working as user tomcat by using the command  
            sudo su - tomcat  
            (It will ask you for the superuser password for permission to do this.)
        *   You can stop working as user tomcat by using the command  
            exit
        *   Do most of the rest of the Tomcat and ERDDAP™ setup instructions as user "tomcat". Later, run the startup.sh and shutdown.sh scripts as user "tomcat" so that Tomcat has permission to write to its log files.
        *   After unpacking Tomcat, from the parent of the apache-tomcat directory:
            
            *   Change ownership of the apache-tomcat directory tree to the tomcat user.  
                chown -R tomcat apache-tomcat-_10.0.23_  
                (but substitute the actual name of your tomcat directory).
            *   Change the "group" to be tomcat, your username, or the name of a small group that includes tomcat and all the administrators of Tomcat/ERDDAP, e.g.,  
                chgrp -R _yourUserName_ apache-tomcat-_10.0.23_
            *   Change permissions so that tomcat and the group have read, write, execute privileges, e.g,.  
                chmod -R ug+rwx apache-tomcat-_10.0.23_
            *   Remove "other" user's permissions to read, write, or execute:  
                chmod -R o-rwx apache-tomcat-_10.0.23_  
                This is important, because it prevents other users from reading possibly sensitive information in ERDDAP™ setup files.
            
              
             
    *   [Set](#memory) [Tomcat's](#WindowsMemory) [Environment](#MacMemory) [Variables](#LinuxMemory)
        
        On Linux and Macs:  
        Create a file _tomcat_/bin/setenv.sh (or in Red Hat Enterprise Linux \[RHEL\], edit ~tomcat/conf/tomcat10.conf) to set Tomcat's environment variables. This file will be used by _tomcat_/bin/startup.sh and shutdown.sh. The file should contain something like:  
        export JAVA\_HOME=/usr/local/jdk-21.0.3+9  
        export JAVA\_OPTS='-server -Djava.awt.headless=true -Xmx1500M -Xms1500M'  
        export TOMCAT\_HOME=/usr/local/apache-tomcat-_10.0.23_  
        export CATALINA\_HOME=/usr/local/apache-tomcat-_10.0.23_  
        (but substitute the directory names from your computer).  
        (If you previously set JRE\_HOME, you can remove that.)  
        On Macs, you probably don't need to set JAVA\_HOME.
        
        On Windows:  
        Create a file _tomcat_\\bin\\setenv.bat to set Tomcat's environment variables. This file will be used by _tomcat_\\bin\\startup.bat and shutdown.bat. The file should contain something like:  
        SET "JAVA\_HOME=\\_someDirectory_\\jdk-21.0.3+9"  
        SET "JAVA\_OPTS=-server -Xmx1500M -Xms1500M"  
        SET "TOMCAT\_HOME=\\Program Files\\apache-tomcat-_10.0.23_"  
        SET "CATALINA\_HOME=\\Program Files\\apache-tomcat-_10.0.23_"  
        (but substitute the directory names from your computer).  
        If this is just for local testing, remove "-server".  
        (If you previously set JRE\_HOME, you can remove that.)
        
        The -Xmx and -Xms memory settings are important because ERDDAP™ works better with more memory. Always set -Xms to the same value as -Xmx.
        
        *   For 32 bit Operating Systems and 32 bit Java:  
            64 bit Java is much better than 32 bit Java, but 32 bit Java will work as long as the server isn't really busy. The more physical memory in the server the better: 4+ GB is really good, 2 GB is okay, less is not recommended. With 32 bit Java, even with abundant physical memory, Tomcat and Java won't run if you try to set -Xmx much above 1500M (1200M on some computers). If your server has less than 2GB of memory, reduce the -Xmx value (in 'M'egaBytes) to 1/2 of the computer's physical memory.
        *   For 64 bit Operating Systems and 64 bit Java:  
            64 bit Java will only work on a 64 bit operating system.
            
            *   With Java 8, you need to add \-d64 to the Tomcat CATALINA\_OPTS parameter in setenv.bat
            *   With Java 21, you choose 64 bit Java when you download a version of Java marked "64 bit".
            
            With 64 bit Java, Tomcat and Java can use very high -Xmx and -Xms settings. The more physical memory in the server the better. As a simplistic suggestion: we recommend you set -Xmx and -Xms to (in 'M'egaBytes) to 1/2 (or less) of the computer's physical memory. You can see if Tomcat, Java, and ERDDAP™ are indeed running in 64 bit mode by searching for " bit," in ERDDAP's Daily Report email or in the _bigParentDirectory_/logs/[log.txt](#log) file (_bigParentDirectory_ is specified in [setup.xml](#setup.xml)).
        *   [In ERDDAP's](#GC) [log.txt](#log) file, you will see many "GC (Allocation Failure)" messages.  
            This is usually not a problem. It is a frequent message from a normally operating Java saying that it just finished a minor garbage collection because it ran out of room in Eden (the section of the Java heap for very young objects). Usually the message shows you _memoryUseBefore_\->_memoryUseAfter_. If those two numbers are close together, it means that the garbage collection wasn't productive. The message is only a sign of trouble if it is very frequent (every few seconds), not productive, and the numbers are large and not growing, which together indicate that Java needs more memory, is struggling to free up memory, and is unable to free up memory. This may happen during a stressful time, then go away. But if it persists, that is a sign of trouble.
        *   If you see java.lang.OutOfMemoryError's in ERDDAP's [log.txt](#log) file, see [OutOfMemoryError](#OutOfMemoryError) for tips on how to diagnose and resolve the problems.  
             
    *   [On Linux and Macs, change the permissions](#permissions) of all \*.sh files in _tomcat_/bin/ to be executable by the owner, e.g., with  
        chmod +x \*.sh  
         
    *   [Fonts for images:](#fonts) We strongly prefer the free [DejaVu fonts](https://dejavu-fonts.github.io/) to the other Java fonts. Using these fonts is strongly recommended but not required.
        
        If you choose not to use the DejaVu fonts, you need to change the fontFamily setting in setup.xml to <fontFamily>SansSerif</fontFamily>, which is available with all Java distributions. If you set fontFamily to the name of a font that isn't available, ERDDAP™ won't load and will print a list of available fonts in the log.txt file. You must use one of those fonts.
        
        If you choose to use the DejaVu fonts, please make sure the fontFamily setting in setup.xml is <fontFamily>DejaVu Sans</fontFamily>.
        
        To install the DejaVu fonts, please download [DejaVuFonts.zip](https://erddap.github.io/DejaVuFonts.zip) (5,522,795 bytes, MD5=33E1E61FAB06A547851ED308B4FFEF42) and unzip the font files to a temporary directory.
        
        *   On Linux:
            *   For Linux Adoptium Java distributions, see [these instructions](https://blog.adoptopenjdk.net/2021/01/prerequisites-for-font-support-in-adoptopenjdk/).
            *   With other Java distributions: As the Tomcat user, copy the font files into _JAVA\_HOME_/lib/fonts so Java can find the fonts. Remember: if/when you later upgrade to a newer version of Java, you need to reinstall these fonts.
        *   On Macs: for each font file, double click on it and then click Install Font.
        *   On Windows 7 and 10: in Windows Explorer, select all of the font files. Right click. Click on Install.  
             
    *   [Test your Tomcat installation.](#testTomcat)
        *   Linux:
            *   As user "tomcat", run _tomcat_/bin/startup.sh
            *   View your URL + ":8080/" in your browser (e.g., [http://coastwatch.pfeg.noaa.gov:8080/](http://coastwatch.pfeg.noaa.gov:8080/)).
            *   You should see the Tomcat "Congratulations" page.  
                If there is trouble, see the Tomcat log file _tomcat_/logs/catalina.out.
        *   Mac (run tomcat as the system administrator user):
            *   Run _tomcat_/bin/startup.sh
            *   View your URL + ":8080/" in your browser (e.g., [http://coastwatch.pfeg.noaa.gov:8080/](http://coastwatch.pfeg.noaa.gov:8080/)). Note that by default, your Tomcat is only accessible by you. It is not publicly accessible.
            *   You should see the Tomcat "Congratulations" page.  
                If there is trouble, see the Tomcat log file _tomcat_/logs/catalina.out.
        *   Windows localhost:
            
            *   Right click on the Tomcat icon in the system tray, and choose "Start service".
            *   View [http://127.0.0.1:8080/](http://127.0.0.1:8080/) (or perhaps [http://localhost:8080/](http://localhost:8080/)) in your browser. Note that by default, your Tomcat is only accessible by you. It is not publicly accessible.
            *   You should see the Tomcat "Congratulations" page.  
                If there is trouble, see the Tomcat log file _tomcat_/logs/catalina.out.
            
              
             
    *   Troubles with the Tomcat installation?
        *   On Linux and Mac, if you can't reach Tomcat or ERDDAP™ (or perhaps you just can't reach them from a computer outside your firewall), you can test if Tomcat is listening to port 8080, by typing (as root) on a command line of the server:  
            netstat -tuplen | grep 8080  
            That should return one line with something like:  
            tcp 0 0 :::8080 :::\* LISTEN ## ##### ####/java  
            (where '#' is some digit), indicating that a "java" process (presumably Tomcat) is listening on port "8080" for "tcp" traffic. If no lines were returned, if the line returned is significantly different, or if two or more lines were returned, then there may be a problem with the port settings.
        *   See the Tomcat log file _tomcat_/logs/catalina.out. Tomcat problems and some ERDDAP™ startup problems are almost always indicated there. This is common when you are first setting up ERDDAP™.
        *   See the [Tomcat(https://tomcat.apache.org/) website or search the web for help, but please let us know the problems you had and the solutions you found.
        *   Email me at erd dot data at noaa dot gov . I will try to help you.
        *   Or, you can join the [ERDDAP™ Google Group / Mailing List](#ERDDAPMailingList) and post your question there.  
             
3.  [Set up the _tomcat_/content/erddap configuration files.](#erddapContent)  
    On Linux, Mac, and Windows, download [erddapContent.zip](https://github.com/ERDDAP/erddapContent/releases/download/content1.0.0/erddapContent.zip) (version 1.0.0, 20333 bytes, MD5=2B8D2A5AE5ED73E3A42B529C168C60B5, dated 2024-10-14) and unzip it into _tomcat_, creating _tomcat_/content/erddap .
    
    [Other Directory:](#erddapContentDirectory) For Red Hat Enterprise Linux (RHEL) or for other situations where you aren't allowed to modify the Tomcat directory or where you want/need to put the ERDDAP™ content directory in some other location for some other reason (for example, if you use Jetty instead of Tomcat), unzip erddapContent.zip into the desired directory (to which only user=tomcat has access) and set the erddapContentDirectory system property (e.g., erddapContentDirectory=~tomcat/content/erddap) so ERDDAP™ can find this new content directory.
    
    \[Some previous versions are also available:  
    [2.17](https://github.com/ERDDAP/erddap/releases/download/v2.17/erddapContent.zip) (19,792 bytes, MD5=8F892616BAEEF2DF0F4BB036DCB4AD7C, dated 2022-02-16)  
    [2.18](https://github.com/ERDDAP/erddap/releases/download/v2.18/erddapContent.zip) (19,792 bytes, MD5=8F892616BAEEF2DF0F4BB036DCB4AD7C, dated 2022-02-16)  
    [2.21](https://github.com/ERDDAP/erddap/releases/download/v2.21/erddapContent.zip) (19,810 bytes, MD5=1E26F62E7A06191EE6868C40B9A29362, dated 2022-10-09)  
    [2.22](https://github.com/ERDDAP/erddap/releases/download/v2.22/erddapContent.zip) (19,810 bytes, MD5=1E26F62E7A06191EE6868C40B9A29362, dated 2022-12-08)
    [2.23](https://github.com/ERDDAP/erddap/releases/download/v2.23/erddapContent.zip) (19,810 bytes, MD5=1E26F62E7A06191EE6868C40B9A29362, dated 2023-02-27)
    and unzip it into _tomcat_, creating _tomcat_/content/erddap . \]
    
    Then,
    
    *   [Read the comments in _tomcat_/content/erddap/**setup.xml**](#setup.xml) and make the requested changes. setup.xml is the file with all of the settings which specify how your ERDDAP™ behaves.  
        For the initial setup, you MUST at least change these settings: <bigParentDirectory>, <emailEverythingTo>, <baseUrl>, <email.\*>, <admin.\*> (and <baseHttpsUrl> when you set up https).
        
        When you create the bigParentDirectory, from the parent directory of bigParentDirectory:
        
        *   Make user=tomcat the owner of the bigParentDirectory, e.g.,  
            chown -R tomcat _bigParentDirectory_
        *   Change the "group" to be tomcat, your username, or the name of a small group that includes tomcat and all the administrators of Tomcat/ERDDAP, e.g.,  
            chgrp -R _yourUserName_ _bigParentDirectory_
        *   Change permissions so that tomcat and the group have read, write, execute privileges, e.g,.  
            chmod -R ug+rwx _bigParentDirectory_
        *   Remove "other" user's permissions to read, write, or execute:  
            chmod -R o-rwx _bigParentDirectory_ reading possibly sensitive information in ERDDAP™ log files and files with information about private datasets.
        
        [Environment Variables](#setupEnvironmentVariables) - Starting with ERDDAP™ v2.13, ERDDAP™ administrators can override any value in setup.xml by specifying an environment variable named ERDDAP\__valueName_ before running ERDDAP™. For example, use ERDDAP\_baseUrl overrides the <baseUrl> value. This can be handy when deploying ERDDAP™ with a container like Docker, as you can put standard settings in setup.xml and then supply special settings via environment variables. If you supply secret information to ERDDAP™ via this method, be sure to check that the information will remain secret. ERDDAP™ only reads environment variables once per startup, in the first second of startup, so one way to use this is: set the environment variables, start ERDDAP, wait until ERDDAP™ is started, then unset the environment variables.
        
    *   [Read the comments in](#datasets.xml) [**Working with the datasets.xml File**](https://erddap.github.io/setupDatasetsXml.html). Later, after you get ERDDAP™ running for the first time (usually with just the default datasets), you will modify the XML in _tomcat_/content/erddap/**datasets.xml** to specify all of the datasets you want your ERDDAP™ to serve. This is where you will you spend the bulk of your time while setting up ERDDAP™ and later while maintaining your ERDDAP™.  
         
    *   (Unlikely) Now or (slightly more likely) in the future, if you want to modify erddap's CSS file, make a copy of _tomcat_/content/erddap/images/erddapStart2.css called erddap2.css and then make changes to it. Changes to erddap2.css only take effect when ERDDAP™ is restarted and often also require the user to clear the browser's cached files.  
         
    
    ERDDAP™ will not work correctly if the setup.xml or datasets.xml file isn't a well-formed XML file. So, after you edit these files, it is a good idea to verify that the result is well-formed XML by pasting the XML text into an XML checker like [xmlvalidation(https://www.xmlvalidation.com/).  
     
4.  [Install the erddap.war file.](#erddap.war)  
    On Linux, Mac, and Windows, download [erddap.war](https://github.com/ERDDAP/erddap/releases/download/v2.25.1/erddap.war) into _tomcat_/webapps .  
    (version 2.25_1, 592,292,039 bytes, MD5=0F0C14FD22DF80D0E867A4B96ED7F3FF, dated 2024-11-07)
    
    The .war file is big because it contains high resolution coastline, boundary, and elevation data needed to create maps.
    
    \[Some previous versions are also available.  
    [2.17](https://github.com/ERDDAP/erddap/releases/download/v2.17/erddap.war) (551,068,245 bytes, MD5=5FEA912B5D42E50EAB9591F773EA848D, dated 2022-02-16)  
    [2.18](https://github.com/ERDDAP/erddap/releases/download/v2.18/erddap.war) (551,069,844 bytes, MD5=461325E97E7577EC671DD50246CCFB8B, dated 2022-02-23)  
    [2.21](https://github.com/ERDDAP/erddap/releases/download/v2.21/erddap.war) (568,644,411 bytes, MD5=F2CFF805893146E932E498FDDBD519B6, dated 2022-10-09)  
    [2.22](https://github.com/ERDDAP/erddap/releases/download/v2.22/erddap.war) (567,742,765 bytes, MD5=2B33354F633294213AE2AFDDCF4DA6D0, dated 2022-12-08)
    [2.23](https://github.com/ERDDAP/erddap/releases/download/v2.23/erddap.war) (572,124,953 bytes, MD5=D843A043C506725EBD6F8EFDCCA8FD5F, dated 2023-03-03)
    [2.24](https://github.com/ERDDAP/erddap/releases/download/v2.24/erddap.war) (568,748,187 bytes, MD5=970fbee172e28b0b8a07756eecbc898e, dated 2024-06-07)
    \]
    
5.  [Use ProxyPass](#ProxyPass) so users don't have to put the port number, e.g., :8080, in the URL.  
    On Linux computers, if Tomcat is running in Apache, please modify the Apache httpd.conf file (usually in /etc/httpd/conf/ ) to allow HTTP traffic to/from ERDDAP™ without requiring the port number, e.g., :8080, in the URL. As the root user:
    1.  Modify the existing <VirtualHost> tag (if there is one), or add one at the end of the file:
        
        <VirtualHost \*:80>
           ServerName _YourDomain.org_
           ProxyRequests Off
           ProxyPreserveHost On
           ProxyPass /erddap http://localhost:8080/erddap
           ProxyPassReverse /erddap http://localhost:8080/erddap
        </VirtualHost>
        
    2.  Then restart Apache: /usr/sbin/apachectl -k graceful (but sometimes it is in a different directory).  
         
6.  [(UNCOMMON)](#NGINX) If you are using [NGINX(https://www.nginx.com/) (a web server and load balancer):  
    in order to get NGINX and ERDDAP™ working correctly with https, you need to put the following snippet inside the Tomcat server.xml <Host> block:
    
    <Valve className="org.apache.catalina.valves.RemoteIpValve"  
      remoteIpHeader="X-Forwarded-For"  
      protocolHeader="X-Forwarded-Proto"  
      protocolHeaderHttpsValue="https" /> 
    
    And in the nginx config file, you need to set these headers:
    
      proxy\_set\_header Host              $host;
      proxy\_set\_header X-Real-IP         $remote\_addr;
      proxy\_set\_header REMOTE\_ADDR       $remote\_addr;
      proxy\_set\_header HTTP\_CLIENT\_IP    $remote\_addr;
      proxy\_set\_header X-Forwarded-For   $proxy\_add\_x\_forwarded\_for;
      proxy\_set\_header X-Forwarded-Proto $scheme;
    
    (Thanks to Kyle Wilcox.)  
     
7.  [Start Tomcat.](#startTomcat)
    *   (I don't recommend using the Tomcat Web Application Manager. If you don't fully shutdown and startup Tomcat, sooner or later you will have PermGen memory issues.)  
         
    *   (In Linux or Mac OS, if you have created a special user to run Tomcat, e.g., tomcat, remember to do the following steps as that user.)  
         
    *   If Tomcat is already running, shut down Tomcat with (in Linux or Mac OS) _tomcat_/bin/shutdown.sh  
        or (in Windows) _tomcat_\\bin\\shutdown.bat
        
        On Linux, use ps -ef | grep tomcat before and after shutdown.sh to make sure the tomcat process has stopped. The process should be listed before the shutdown and eventually not listed after the shutdown. It may take a minute or two for ERDDAP™ to fully shut down. Be patient. Or if it looks like it won't stop on its own, use:  
        kill -9 _processID_
        
    *   Start Tomcat with (in Linux or Mac OS) _tomcat_/bin/startup.sh  
        or (in Windows) _tomcat_\\bin\\startup.bat  
         
8.  [Is ERDDAP™ running?](#isErddapRunning)  
    Use a browser to try to view http://_www.YourServer.org_/erddap/status.html  
    ERDDAP™ starts up without any datasets loaded. Datasets are loaded in a background thread and so become available one-by-one.
    
    [Trouble? Look in the log files.](#LookInLogFiles)
    
    *   When a request from a user comes in, it goes to Apache (on Linux and Mac OS computers), then Tomcat, then ERDDAP™.
    *   You can see what comes to Apache (and related errors) in the Apache log files.
    *   [You](#tomcatLogs) can see what comes to Tomcat (and related errors) in the Tomcat log files (_tomcat_/logs/catalina.out and other files in that directory).
    *   [You](#log) can see what comes to ERDDAP, diagnostic messages from ERDDAP, and error messages from ERDDAP, in the ERDDAP™ <bigParentDirectory>logs/log.txt file.
    *   Tomcat doesn't start ERDDAP™ until Tomcat gets a request for ERDDAP™. So you can see in the Tomcat log files if it started ERDDAP™ or if there is an error message related to that attempt.
    *   When ERDDAP™ starts up, it renames the old ERDDAP™ log.txt file (logArchivedAt_CurrentTime_.txt) and creates a new log.txt file. So if the log.txt file is old, it is a sign that ERDDAP™ hasn't recently restarted. ERDDAP™ writes log info to a buffer and only writes the buffer to the log file periodically, but you can force ERDDAP™ to write the buffer to the log file by visiting .../erddap/status.html.
    
    [Trouble: Old Version of Java](#OldVersionOfJava)  
    If you are using a version of Java that is too old for ERDDAP, ERDDAP™ won't run and you will see an error message in Tomcat's log file like  
    Exception in thread "main" java.lang.UnsupportedClassVersionError:  
    _some/class/name_: Unsupported major.minor version _someNumber_  
    The solution is to update to the most recent version of Java and make sure that Tomcat is using it.
    
    [Trouble: Slow Startup First Time](#SlowStartup)  
    Tomcat has to do a lot of work the first time an application like ERDDAP™ is started; notably, it has to unpack the erddap.war file (which is like a .zip file). On some servers, the first attempt to view ERDDAP™ stalls (30 seconds?) until this work is finished. On other servers, the first attempt will fail immediately. But if you wait 30 seconds and try again, it will succeed if ERDDAP™ was installed correctly.  
    There is no fix for this. This is simply how Tomcat works. But it only occurs the first time after you install a new version of ERDDAP™.
    
9.  In the future, to shut down (and restart) ERDDAP, see  
    [How to Shut Down and Restart Tomcat and ERDDAP](#shutdown).  
     
10.  Troubles installing Tomcat or ERDDAP?  
    Email me at erd dot data at noaa dot gov . I will help you.  
    Or, you can join the [ERDDAP™ Google Group / Mailing List](#ERDDAPMailingList) and post your question there.  
     
11.  Email [Notification of New Versions of ERDDAP](#updateNotification)  
    If you want to receive an email whenever a new version of ERDDAP™ is available or other important ERDDAP™ announcements, you can join the ERDDAP™ announcements list [here](https://groups.google.com/g/erddap-announce). This list averages roughly one email every three months.  
     
12.  [Customize your ERDDAP™ to highlight your organization (not NOAA ERD).](#customize)
    *   Change the banner that appears at the top of all ERDDAP™ .html pages by editing the <startBodyHtml5> tag in your datasets.xml file. (If there isn't one, copy the default from ERDDAP's  
        \[tomcat\]/webapps/erddap/WEB-INF/classes/gov/noaa/pfel/erddap/util/messages.xml file into datasets.xml and edit it.) For example, you could:
        *   Use a different image (i.e., your organization's logo).
        *   Change the background color.
        *   Change "ERDDAP" to "_YourOrganization_'s ERDDAP"
        *   Change "Easier access to scientific data" to "Easier access to _YourOrganization_'s data".
        *   Change the "Brought to you by" links to be links to your organization and funding sources.
    *   Change the information on the left side of the home page by editing the <theShortDescriptionHtml> tag in your datasets.xml file. (If there isn't one, copy the default from ERDDAP's  
        \[tomcat\]/webapps/erddap/WEB-INF/classes/gov/noaa/pfel/erddap/util/messages.xml file into datasets.xml and edit it.) For example, you could:
        *   Describe what your organization and/or group does.
        *   Describe what kind of data this ERDDAP™ has.
    *   To change the icon that appears on browser tabs, put your organization's favicon.ico in _tomcat_/content/erddap/images/ . See [https://en.wikipedia.org/wiki/Favicon](https://en.wikipedia.org/wiki/Favicon).
