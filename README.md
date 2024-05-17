# ERDDAP&trade;: Easier Access to Scientific Data

**Welcome to the ERDDAP GitHub repository** 

ERDDAP&trade; is a scientific data server that gives users a simple, consistent way to download subsets of gridded and tabular scientific datasets in common file formats and make graphs and maps.  ERDDAP is a Free and Open Source (Apache and Apache-like) Java Servlet developed by the NOAA NMFS SWFSC Environmental Research Division (ERD).

A live ERDDAP installation can be seen at: https://coastwatch.pfeg.noaa.gov/erddap/index.html.

<div style="width: 100%; clear: both; align: center"><img src="development/images/erddap_sst_graph.jpg" alt="ERDDAP SST data example graph page" width="650" style="margin: 10px; border-width: 1px; border-style: solid; border-color: grey" /></div>

*Example screenshot of ERDDAP's web user interface 'Make-a-Graph' page*


## Developing with ERDDAP

ERDDAP is a Java Servlet-based application and can be run in any compatible Java Servlet Container/Application Server, such as Apache Tomcat.

Local development and testing of ERDDAP code can be done without a production-scale installation.  Two approaches are recommended:

* **Jetty Servlet Container** - see: [ERDDAP Development with Maven and Jetty](./development/jetty/).
* **Docker/Tomcat** (building and running an ERDDAP development Docker image) - see: [ERDDAP Development with Docker/Tomcat](./development/docker/)

For operational ERDDAP deployment, [Apache Tomcat](https://tomcat.apache.org/) is recommended.  See [Deploying ERDDAP Operationally](#deploying-erddap-operationally) for instructions.


### Running JUnit tests

Simply run `mvn test` in a terminal to run the JUnit tests.

Note that by default tests that do an image comparison are enabled. To disable those tests add `ImageComparison` to the `excludedGroups` section of the surefire `configuration`. It is recommended you run the image tests before making changes to ERDDAP so you can generate a baseline set of images that will be later used for comparison.


### Building a war

`mvn package` will create a war file. 

If you'd like to skip the tests while building use `mvn package -DskipTests`. You can use the skipTests flags with other maven commands.


## Contributing Code to ERDDAP

Below are relevant links for getting involved with the ERDDAP community and contributing to ERDDAP:

* Review conversations and ask questions at https://groups.google.com/g/erddap or at https://github.com/erddap/erddap/discussions
* Review and submit issues to https://github.com/erddap/erddap/issues
* To propose feature requests, follow this guidance: https://github.com/erddap/erddap/discussions/93


## Deploying ERDDAP Operationally

Instructions for installing ERDDAP in Apache Tomcat are available at: https://erddap.github.io/setup.html.