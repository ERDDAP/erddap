[Credits](#credits)
-------------------

ERDDAP is a product of the [NOAA](https://www.noaa.gov "National Oceanic and Atmospheric Administration") [NMFS](https://www.fisheries.noaa.gov "National Marine Fisheries Service") [SWFSC](https://swfsc.noaa.gov "Southwest Fisheries Science Center") [ERD](https://www.fisheries.noaa.gov/about/environmental-research-division-southwest-fisheries-science-center "Environmental Research Division").

Bob Simons is the original main author of ERDDAP (the designer and software developer who wrote the ERDDAP-specific code). The starting point was Roy Mendelssohn's (Bob's boss) suggestion that Bob turn his ConvertTable program (a small utility which converts tabular data from one format to another and which was largely code from Bob's pre-NOAA work that Bob re-licensed to be open source) into a web service.

It was and is Roy Mendelssohn's ideas about distributed data systems, his initial suggestion to Bob, and his ongoing support (including hardware, network, and other software support, and by freeing up Bob's time so he could spend more time on the ERDDAP code) that has made this project possible and enabled its growth.

The ERDDAP-specific code is licensed as copyrighted open source, with [NOAA](https://www.noaa.gov) holding the copyright. See the [ERDDAP license](#license).  
ERDDAP uses copyrighted open source, Apache, LGPL, MIT/X, Mozilla, and public domain libraries and data.  
ERDDAP does not require any GPL code or commercial programs.

The bulk of the funding for work on ERDDAP has come from NOAA, in that it paid Bob Simons' salary. For the first year of ERDDAP, when he was a government contractor, funding came from the [NOAA CoastWatch](https://coastwatch.noaa.gov/) program, the [NOAA IOOS](https://ioos.noaa.gov/) program, and the now defunct Pacific Ocean Shelf Tracking (POST) program.

Much credit goes to the many ERDDAP administrators and users who have made suggestions and comments which have led to many improvements in ERDDAP. Many are mentioned by name in the [List of Changes](https://erddap.github.io/changes.html). Thank you all (named and unnamed) very much. Thus, ERDDAP is a great example of [User-Driven Innovation](https://en.wikipedia.org/wiki/User_innovation), where product innovation often comes from consumers (ERDDAP users), not just the producers (ERDDAP developers).

Here is the list of software and datasets that are in the ERDDAP distribution. We are very grateful for all of these. Thank you very much.  
\[Starting in 2021, it has become almost impossible to properly list all of the sources of code for ERDDAP because a few of the libraries we use (notably netcdf-java and especially AWS) in turn use many, many other libraries. All of the libraries that ERDDAP code calls directly are included below, as are many of the libraries that the other libraries call in turn. If you see that we have omitted a project below, please let us know so we can add the project below and give credit where credit is due.\]

*   Overview  
    ERDDAP is a [Java Servlet](https://www.oracle.com/technetwork/java/javaee/servlet/index.html) program. At ERD, it runs inside of a [Tomcat](https://tomcat.apache.org/) application server (license: [Apache](https://www.apache.org/licenses/)), with an [Apache](https://httpd.apache.org/) web server (license: [Apache](https://www.apache.org/licenses/)), running on a computer using the [Red Hat Linux](https://www.redhat.com/) operating system (license: [GPL](https://www.gnu.org/licenses/gpl-3.0.html)).  
     
*   Datasets  
    The data sets are from various sources. See the metadata (in particular the "sourceUrl", "infoUrl", "institution", and "license") for each dataset. Many datasets have a restriction on their use that requires you to cite/credit the data provider whenever you use the data. It is always good form to cite/credit the data provider. See [How to Cite a Dataset in a Paper](https://coastwatch.pfeg.noaa.gov/erddap/information.html#citeDataset).  
     
*   CoHort Software  
    [The com/cohort classes](#licenseCoHortSoftware) are from CoHort Software ([https://www.cohortsoftware.com](https://www.cohortsoftware.com)) which makes these classes available with an MIT/X-like license (see classes/com/cohort/util/LICENSE.txt).  
     
*   CoastWatch Browser  
    ERDDAP uses code from the CoastWatch Browser project (now decomissioned) from the [NOAA CoastWatch](https://coastwatch.noaa.gov) [West Coast Regional Node](https://coastwatch.pfeg.noaa.gov/) (license: copyrighted open source). That project was initiated and managed by Dave Foley, a former Coordinator of the NOAA CoastWatch West Coast Regional Node. All of the CoastWatch Browser code was written by Bob Simons.  
     
*   OPeNDAP  
    Data from [OPeNDAP](https://www.opendap.org) servers are read with [Java DAP 1.1.7](https://www.opendap.org/deprecated-software/java-dap) (license: LGPL).  
     
*   NetCDF-java  
    NetCDF files (.nc), GMT-style NetCDF files (.grd), GRIB, and BUFR are read and written with code in the [NetCDF Java Library](https://www.unidata.ucar.edu/software/netcdf-java/) (license: [BSD-3](https://github.com/Unidata/netcdf-java/blob/develop/LICENSE)) from [Unidata](https://www.unidata.ucar.edu/).
    
    Software Included in the NetCDF Java .jar:
    
    *   slf4j  
        The NetCDF Java Library and Cassandra need [slf4j from the Simple Logging Facade for Java](https://www.slf4j.org/) project. Currently, ERDDAP uses the slf4j-simple-xxx.jar renamed as slf4j.jar to meet this need. (license: [MIT/X](https://www.slf4j.org/license.html)).  
         
    *   JDOM  
        The NetCDF Java .jar includes XML processing code from [JDOM](http://www.jdom.org/) (license: [Apache](http://www.jdom.org/docs/faq.html#a0030)), which is included in the netcdfAll.jar.  
         
    *   Joda  
        The NetCDF Java .jar includes [Joda](https://www.joda.org/joda-time/) for calendar calculations (which are probably not used by ERDDAP). (license: [Apache 2.0](https://www.joda.org/joda-time/licenses.html)).  
         
    *   Apache  
        The NetCDF Java .jar includes .jar files from several [Apache projects](https://www.apache.org/):  
        [commons-codec](https://commons.apache.org/proper/commons-codec/),  
        [commons-discovery](https://commons.apache.org/discovery/),  
        [commons-httpclient](https://hc.apache.org/httpcomponents-client-ga/),  
        [commons-logging](https://commons.apache.org/proper/commons-logging/)  
        [HttpComponents](https://hc.apache.org),  
        (For all: license: [Apache](https://www.apache.org/licenses/LICENSE-2.0))  
        These are included in the netcdfAll.jar.  
         
    *   Other  
        The NetCDF Java .jar also includes code from: com.google.code.findbugs, com.google.errorprone, com.google.guava, com.google.j2objc, com.google.protobuf, edu.ucar, org.codehaus.mojo, com.beust.jcommander, com.google.common, com.google.re2j, and com.google.thirdparty. (Google uses Apache and BSD-like licenses.)  
         
*   SGT  
    The graphs and maps are created on-the-fly with a modified version of NOAA's SGT (was at https://www.pmel.noaa.gov/epic/java/sgt/, now discontinued) version 3 (a Java-based Scientific Graphics Toolkit written by Donald Denbo at [NOAA PMEL](https://www.pmel.noaa.gov/)) (license: copyrighted open source (was at https://www.pmel.noaa.gov/epic/java/license.html)).  
     
*   Walter Zorn  
    Big, HTML tooltips on ERDDAP's HTML pages are created with Walter Zorn's wz\_tooltip.js (license: LGPL).  
    Sliders and the drag and drop feature of the Slide Sorter are created with Walter Zorn's wz\_dragdrop.js (license: LGPL).  
     
*   iText  
    The .pdf files are created with [iText](https://itextpdf.com/) (version 1.3.1, which used the  Mozilla license), a free Java-PDF library by Bruno Lowagie and Paulo Soares.  
     
*   GSHHS  
    The shoreline and lake data are from [GSHHS](https://www.ngdc.noaa.gov/mgg/shorelines/gshhs.html) -- A Global Self-consistent, Hierarchical, High-resolution Shoreline Database (license: [GPL](https://www.soest.hawaii.edu/pwessel/gshhs/README.TXT)) and created by Paul Wessel and Walter Smith.
    
    WE MAKE NO CLAIM ABOUT THE CORRECTNESS OF THE SHORELINE DATA THAT COMES WITH ERDDAP -- DO NOT USE IT FOR NAVIGATIONAL PURPOSES.  
     
    
*   GMT pscoast  
    The political boundary and river data are from the [pscoast](https://www.soest.hawaii.edu/gmt/gmt/html/man/pscoast.html) program in [GMT](https://www.soest.hawaii.edu/gmt/), which uses data from the [CIA World Data Bank II](https://www.evl.uic.edu/pape/data/WDB/) (license: public domain).
    
    WE MAKE NO CLAIM ABOUT THE CORRECTNESS OF THE POLITICAL BOUNDARY DATA THAT COMES WITH ERDDAP.
    
*   ETOPO  
    The bathymetry/topography data used in the background of some maps is the [ETOPO1 Global 1-Minute Gridded Elevation Data Set](https://www.ngdc.noaa.gov/mgg/global/global.html) (Ice Surface, grid registered, binary, 2 byte int: etopo1\_ice\_g\_i2.zip) (license: [public domain](https://www.ngdc.noaa.gov/ngdcinfo/privacy.html#copyright)), which is distributed for free by [NOAA NGDC](https://www.ngdc.noaa.gov).
    
    WE MAKE NO CLAIM ABOUT THE CORRECTNESS OF THE BATHYMETRY/TOPOGRAPHY DATA THAT COMES WITH ERDDAP. DO NOT USE IT FOR NAVIGATIONAL PURPOSES.
    
*   JavaMail  
    Emails are sent using code in mail.jar from Oracle's [JavaMail API](https://javaee.github.io/javamail/) (license: [COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.1](https://javaee.github.io/javamail/LICENSE)).  
     
*   JSON  
    ERDDAP uses [json.org's Java-based JSON library](https://www.json.org/index.html) to parse [JSON](https://www.json.org/) data (license: [copyrighted open source](https://www.json.org/license.html)).  
     
*   PostgrSQL  
    ERDDAP includes the [PostGres JDBC](https://mvnrepository.com/artifact/org.postgresql/postgresql) driver (license: [BSD](https://www.postgresql.org/about/licence/)). The driver is Copyright (c) 1997-2010, PostgreSQL Global Development Group. All rights reserved.  
     
*   Lucene  
    ERDDAP use code from Apache [Lucene](https://lucene.apache.org/). (license: [Apache](https://www.apache.org/licenses/LICENSE-2.0)) for the "lucene" search engine option (but not for the default "original" search engine).  
     
*   commons-compress  
    ERDDAP use code from Apache [commons-compress](https://commons.apache.org/compress/). (license: [Apache](https://www.apache.org/licenses/LICENSE-2.0)).  
     
*   JEXL  
    ERDDAP support for evaluating expressions and scripts in <sourceNames>'s relies on the [Apache project's](https://www.apache.org/): [Java Expression Language (JEXL)](https://commons.apache.org/proper/commons-jexl/) (license: [Apache](https://www.apache.org/licenses/LICENSE-2.0)).  
     
*   Cassandra  
    ERDDAP includes Apache [Cassandra's](https://cassandra.apache.org/) [cassandra-driver-core.jar](https://mvnrepository.com/artifact/com.datastax.cassandra/cassandra-driver-core) (license: [Apache 2.0](https://github.com/datastax/java-driver/blob/2.1/LICENSE)).  
    Cassandra's cassandra-driver-core.jar requires (and so ERDDAP includes):
    *   [guava.jar](https://github.com/google/guava) (license: [Apache 2.0](https://github.com/google/guava/blob/master/LICENSE)).
    *   [lz4.jar](https://repo1.maven.org/maven2/net/jpountz/lz4/lz4/) (license: [Apache 2.0](https://github.com/jpountz/lz4-java/blob/master/LICENSE.txt)).
    *   [metrics-core.jar](https://mvnrepository.com/artifact/com.codahale.metrics/metrics-core/3.0.2) (license: [MIT](https://github.com/codahale/metrics/blob/master/LICENSE)).
    *   [netty-all.jar](https://netty.io/downloads.html) (license: [Apache 2.0](https://netty.io/downloads.html)).
    *   [snappy-java.jar](https://xerial.org/snappy-java/) (license: [Apache 2.0](https://github.com/xerial/snappy-java/blob/develop/LICENSE)).  
         
*   KT\_ palettes  
    The color palettes which have the prefix "KT\_" are a [collection of .cpt palettes by Kristen Thyng](http://soliton.vm.bytemark.co.uk/pub/cpt-city/cmocean/index.html) (license: [MIT/X](http://soliton.vm.bytemark.co.uk/pub/cpt-city/cmocean/copying.html)), but slightly reformatted by Jennifer Sevadjian of NOAA so that they conform to ERDDAP's .cpt requirements.  
     
*   Leaflet  
    ERDDAP uses the JavaScript library [Leaflet](https://leafletjs.com/) (license: [BSD 2](https://github.com/Leaflet/Leaflet/blob/main/LICENSE)) as the WMS client on WMS web pages in ERDDAP. It is excellent software (well designed, easy to use, fast, and free) from Vladimir Agafonkin.  
     
*   AWS  
    For working with Amazon AWS (including S3), ERDDAP uses v2 of the [AWS SDK for Java](https://aws.amazon.com/sdk-for-java/) (license: [Apache](https://www.apache.org/licenses/)).
    
    AWS requires Maven to pull in the dependencies. They include the following .jar files (where xxx is the version number, which changes over time, and the license type is in parentheses): annotations-xxx.jar (Apache), apache-client-xxx.jar (Apache), ams-xxx.jar (BSD), asm-xxx.jar (BSD), asm-analysis-xxx.jar (BSD), asm-commons-xxx.jar (BSD), asm-tree-xxx.jar (BSD), asm-util-xxx.jar (BSD), auth-xxx.jar (?), aws-core-xxx.jar (Apache), aws-query-protocol-xxx.jar (Apache), aws-xml-protocol-xxx.jar (Apache), checker-qual-xxx.jar (MIT), error\_prone\_annotations-xxx.jar (Apache), eventstream-xxx.jar (Apache), failureaccess-xxx.jar (Apache), httpcore-xxx.jar (Apache), j2objc-annotations-xxx.jar (Apache), jackson-annotations-xxx.jar (Apache), jackson-core-xxx.jar (Apache), jackson-databind-xxx.jar (Apache), jaxen-xxx.jar (BSD), jffi-xxx.jar (Apache), jffi-xxx.native.jar (Apache), jnr-constants-xxx.jar (Apache), jnr-ffi-xxx.jar (Apache), jnr-posix-xxx.jar (Apache), jnr-x86asm-xxx.jar (Apache), json-xxx.jar (Copyrighted open source), jsr305-xxx.jar (Apache), listenablefuture-xxx.jar (Apache), about a dozen netty .jar's (Apache), profiles-xxx.jar (Apache), protocol-core-xxx.jar (Apache), reactive-streams-xxx.jar (CCO 1.0), regions-xxx.jar (Apache), s3-xxx.jar (Apache), sdk-core-xxx.jar (Apache), utils-xxx.jar (?). To see the actual licenses, search for the .jar name in the [Maven Repository](https://mvnrepository.com/) and then rummage around in the project's files to find the license. 

Contributions to ERDDAP code
*   [MergeIR](#submittedCode)  
    [EDDGridFromMergeIRFiles.java](https://erddap.github.io/setupDatasetsXml.html#EDDGridFromMergeIRFiles) was written and contributed by Jonathan Lafite and Philippe Makowski of R.Tech Engineering (license: copyrighted open source). Thank you, Jonathan and Philippe!  
     
*   TableWriterDataTable  
    [.dataTable (TableWriterDataTable.java)](https://coastwatch.pfeg.noaa.gov/erddap/tabledap/documentation.html#fileType) was written and contributed by Roland Schweitzer of NOAA (license: copyrighted open source). Thank you, Roland!  
     
*   json-ld  
    The initial version of the [Semantic Markup of Datasets with json-ld (JSON Linked Data)](#jsonld) feature (and thus all of the hard work in designing the content) was written and contributed (license: copyrighted open source) by Adam Leadbetter and Rob Fuller of the Marine Institute in Ireland. Thank you, Adam and Rob!  
     
*   orderBy  
    The code for the [orderByMean filter](https://coastwatch.pfeg.noaa.gov/erddap/tabledap/documentation.html#orderByMean) in tabledap and the extensive changes to the code to support the [_variableName/divisor:offset_ notation](https://coastwatch.pfeg.noaa.gov/erddap/tabledap/documentation.html#orderByDivisorOptions) for all orderBy filters was written and contributed (license: copyrighted open source) by Rob Fuller and Adam Leadbetter of the Marine Institute in Ireland. Thank you, Rob and Adam!  
     
*   Borderless Marker Types  
    The code for three new marker types (Borderless Filled Square, Borderless Filled Circle, Borderless Filled Up Triangle) was contributed by Marco Alba of ETT / EMODnet Physics. Thank you, Marco Alba!  
     
*   Translations of messages.xml  
    The initial version of the code in TranslateMessages.java which uses Google's translation service to translate messages.xml into various languages was written by Qi Zeng, who was working as a Google Summer of Code intern. Thank you, Qi!  
     
*   orderBySum  
    The code for the [orderBySum filter](https://coastwatch.pfeg.noaa.gov/erddap/tabledap/documentation.html#orderBySum) in tabledap (based on Rob Fuller and Adam Leadbetter's orderByMean) and the Check All and Uncheck All buttons on the EDDGrid Data Access Form were written and contributed (license: copyrighted open source) by Marco Alba of ETT Solutions and EMODnet. Thank you, Marco!  
     
*   Out-of-range .transparentPng Requests  
    ERDDAP now accepts requests for .transparentPng's when the latitude and/or longitude values are partly or fully out-of-range. (This was ERDDAP GitHub Issues #19, posted by Rob Fuller -- thanks for posting that, Rob.) The code to fix this was written by Chris John. Thank you, Chris!  
     
*   Display base64 image data in .htmlTable responses  
    The code for displaying base64 image data in .htmlTable responses was contributed by Marco Alba of ETT / EMODnet Physics. Thank you, Marco Alba!  
     
*   nThreads Improvement  
    The nThreads system for EDDTableFromFiles was significantly improved. These changes lead to a huge speed improvement (e.g., 2X speedup when nThreads is set to 2 or more) for the most challenging requests (when a large number of files must be processed to gather the results). These changes will also lead to a general speedup throughout ERDDAP. The code for these changes was contributed by Chris John. Thank you, Chris!  
     

We are also very grateful for all of the software and websites that we use when developing ERDDAP, including  
[Chrome](https://www.google.com/chrome/browser/desktop/),  
[curl](https://curl.haxx.se/),  
[DuckDuckGo](https://duckduckgo.com/?q=),  
[EditPlus](https://www.editplus.com/),  
[FileZilla](https://filezilla-project.org/).  
[GitHub](https://github.com/),  
[Google Search](https://www.google.com/webhp),  
[PuTTY](https://www.chiark.greenend.org.uk/~sgtatham/putty/download.html),  
[stack overflow](https://stackoverflow.com/),  
[todoist](https://todoist.com/?lang=en),  
[Wikipedia](https://www.wikipedia.org/),  
the Internet, the World Wide Web, and all the other, great, helpful websites.  
Thank you very much.
