Welcome to the ERDDAP repo. 

ERDDAP is a scientific data server that gives users a simple, consistent way to download subsets of 
gridded and tabular scientific datasets in common file formats and make graphs and maps.
ERDDAP is a Free and Open Source (Apache and Apache-like) Java Servlet from NOAA NMFS SWFSC Environmental Research Division (ERD).
* To see/use an ERDDAP installation: [https://coastwatch.pfeg.noaa.gov/erddap/index.html]
* To read installation instructions: [https://erddap.github.io/setup.html]
* To contribute code: [https://erddap.github.io/setup.html#programmersGuide]

Below you will find relevant links for asking questions and how to contribute.
* Review conversations and ask questions at https://groups.google.com/g/erddap or at https://github.com/erddap/erddap/discussions
* Review and submit issues to https://github.com/erddap/erddap/issues
* To propose feature requests, follow this guidance: https://github.com/erddap/erddap/discussions/93#discussion-4920427

# Running JUnit tests

Simply run `mvn test` in a terminal to run the JUnit tests.

Note that by default tests that do an image comparison are enabled. To disable those tests add `ImageComparison` to the `excludedGroups` section of the surefire `configuration`. It is recommended you run the image tests before making changes to ERDDAP so you can generate a baseline set of images that will be later used for comparison.

# Building a war

`mvn package` will create a war file. 

If you'd like to skip the tests while building use `mvn package -DskipTests`. You can use the skipTests flags with other maven commands.