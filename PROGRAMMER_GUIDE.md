## [Programmer's Guide](https://erddap.github.io/setup.html#programmersGuide)<a id="programmers-guide"></a>

These are things that only a programmer who intends to work with ERDDAP's Java classes needs to know.

### **Getting the Source Code**
   

  - Via Source Code on GitHub
    The source code for recent public versions and in-development versions is also available via [GitHub](https://github.com/ERDDAP). Please read the [Wiki](https://github.com/ERDDAP/erddap/wiki) for that project. If you want to modify the source code (and possibly have the changes incorporated into the standard ERDDAP™ distribution), this is the recommended approach.

### **ERDDAP™ dependencies**
ERDDAP™ uses Maven to load code dependencies as well as some static reference files (WEB-INF/ref). This is done to avoid storing many large files in the repository.
  You can use `mvn compile` and that will fetch the dependencies and ref files. You can also use `mvn package` to generate a war file.
  You can manually download the ref files:

  - [etopo1\_ice\_g\_i2.zip](https://github.com/ERDDAP/ERDDAPRefFiles/releases/download/1.0.0/etopo1_ice_g_i2.zip) and unzip it into /WEB-INF/ref/ .

  - [ref\_files.zip](https://github.com/ERDDAP/ERDDAPRefFiles/releases/download/1.0.0/ref_files.zip) and unzip it into /WEB-INF/ref/ .

  - [erddapContent.zip](https://github.com/ERDDAP/erddapContent/releases/download/content1.0.0/erddapContent.zip) (version 1.0.0, 20333 bytes, MD5=2B8D2A5AE5ED73E3A42B529C168C60B5, dated 2024-10-14) and unzip it into _tomcat_, creating _tomcat_/content/erddap .

NOTE: By default Maven will cache static reference and test data archive downloads and only extract them when a new version is downloaded. To skip downloading entirely, you may set the `skipResourceDownload` and/or `skipTestResourceDownload` properties to Maven (e.g. `mvn -DskipResourceDownload package`). To force extraction, set `-Ddownload.unpack=true` and `-Ddownload.unpackWhenChanged=false`.

- ERDDAP™ and its subcomponents have very liberal, open-source [licenses](https://erddap.github.io/setup.html#license), so you can use and modify the source code for any purpose, for-profit or not-for-profit. Note that ERDDAP™ and many subcomponents have licenses that require that you acknowledge the source of the code that you are using. See [Credits](https://erddap.github.io/setup.html#credits). Whether required or not, it is just good form to acknowledge all of these contributors.
   

- **Use the Code for Other Projects**

  While you are welcome to use parts of the ERDDAP™ code for other projects, be warned that the code can and will change. We don't promise to support other uses of our code. Git and GitHub will be your main solutions for dealing with this -- Git allows you to merge our changes into your changes.\
  **For many situations where you might be tempted to use parts of ERDDAP™ in your project, we think you will find it much easier to install and use ERDDAP™ as is,** and then write other services which use ERDDAP's services. You can set up your own ERDDAP™ installation crudely in an hour or two. You can set up your own ERDDAP™ installation in a polished way in a few days (depending on the number and complexity of your datasets). But hacking out parts of ERDDAP™ for your own project is likely to take weeks (and months to catch subtleties) and you will lose the ability to incorporate changes and bug fixes from subsequent ERDDAP™ releases. We (obviously) think there are many benefits to using ERDDAP™ as is and making your ERDDAP™ installation publicly accessible. However, in some circumstances, you might not want to make your ERDDAP™ installation publicly accessible. Then, your service can access and use your private ERDDAP™ and your clients needn't know about ERDDAP™.\

  #### **Halfway**

  Or, there is another approach which you may find useful which is halfway between delving into ERDDAP's code and using ERDDAP™ as a stand-alone web service: In the EDD class, there is a static method which lets you make an instance of a dataset (based on the specification in datasets.xml):
  `oneFromDatasetXml(String tDatasetID)
  `It returns an instance of an EDDTable or EDDGrid dataset. Given that instance, you can call\
  `makeNewFileForDapQuery(String userDapQuery, String dir, String fileName, String fileTypeName)
  `to tell the instance to make a data file, of a specific fileType, with the results from a user query. Thus, this is a simple way to use ERDDAP's methods to request data and get a file in response, just as a client would use the ERDDAP™ web application. But this approach works within your Java program and bypasses the need for an application server like Tomcat. We use this approach for many of the unit tests of EDDTable and EDDGrid subclasses, so you can see examples of this in the source code for all of those classes.

### **Development Environment**

  - There are configurations for [Jetty ](/development/jetty)and [Docker ](/development/docker)in GitHub, though releases are expected to run in Tomcat.

  - **Optional**: Set up ERDDAP™ in Tomcat\
    Since ERDDAP™ is mainly intended to be a servlet running in Tomcat, we strongly recommend that you follow the standard [installation instructions](/DEPLOY_INSTALL.md) to install Tomcat, and then install ERDDAP™ in Tomcat's webapps directory. Among other things, ERDDAP™ was designed to be installed in Tomcat's directory structure and expects Tomcat to provide some .jar files.

  - ERDDAP™ does not require a specific IDE (Chris mainly uses Visual Studio Code, Bob used EditPlus). We don't use Eclipse, Ant, etc.; nor do we offer ERDDAP-related support for them. The project does use Maven.

  - We use a batch file which deletes all of the .class files in the source tree to ensure that we have a clean compile (with javac).

  - We currently use Adoptium's javac jdk-21.0.3+9 to compile gov.noaa.pfeg.coastwatch.TestAll (it has links to a few classes that wouldn't be compiled otherwise) and run the tests. For security reasons, it is almost always best to use the latest versions of Java 21 and Tomcat 10.

    - When we run javac or java, the current directory is _tomcat_/webapps/erddap/WEB-INF .

    - Our javac and java classpath is
      `classes;../../../lib/servlet-api.jar;lib/*`

    - So your javac command line will be something like\
      `javac -encoding UTF-8 -cp classes;../../../lib/servlet-api.jar;lib/* classes/gov/noaa/pfel/coastwatch/TestAll.java`

    - And your java command line will be something like\
      `java -cp classes;../../../lib/servlet-api.jar;lib/* -Xmx4000M -Xms4000M classes/gov/noaa/pfel/coastwatch/TestAll
      `Optional: you can add `-verbose:gc`, which tells Java to print garbage collection statistics.

    - If TestAll compiles, everything ERDDAP™ needs has been compiled. A few classes are compiled that aren't needed for ERDDAP™. If compiling TestAll succeeds but doesn't compile some class, that class isn't needed. (There are some unfinished/unused classes.)

  - In a few cases, we use 3rd party source code instead of .jar files (notably for DODS) and have modified them slightly to avoid problems compiling with Java 21. We have often made other slight modifications (notably to DODS) for other reasons.

  - Most classes have test methods in their associated src/test file. You can run the JUnit tests with the `mvn test` command. This will download several zip files of data that the tests rely on from the latest release of [ERDDAP/erddapTest](https://github.com/ERDDAP/erddapTest/releases/).\
     
NOTE: Maven caches downloads but will unzip the downloaded archives on each execution, which takes time. To skip downloading
and unzipping test data archives, you may specify the `skipTestResourceDownload` property to Maven (e.g. `mvn -DskipTestResourceDownload package`).

###  **Important Classes**

If you want to look at the source code and try to figure out how ERDDAP™ works, please do.

  - The code has JavaDoc comments, but the JavaDocs haven't been generated. Feel free to generate them.

  - The most important classes (including the ones mentioned below) are within gov/noaa/pfel/erddap.

  - The ERDDAP™ class has the highest level methods. It extends HttpServlet.

  - ERDDAP™ passes requests to instances of subclasses of EDDGrid or EDDTable, which represent individual datasets.

  - EDStatic has most of the static information and settings (e.g., from the setup.xml and messages.xml files) and offers static services (e.g., sending emails).

  - EDDGrid and EDDTable subclasses parse the request, get data from subclass-specific methods, then format the data for the response.

  - EDDGrid subclasses push data into GridDataAccessor (the internal data container for gridded data).

  - EDDTable subclasses push data into TableWriter subclasses, which write data to a specific file type on-the-fly.

  - Other classes (e.g., low level classes) are also important, but it is less likely that you will be working to change them.
     

### **Code Contributions**

- GitHub Issues
  If you would like to contribute but don't have a project, see the list of [GitHub Issues](https://github.com/ERDDAP/erddap/issues), many of which are projects you could take on. If you would like to work on an issue, please assign it to yourself to indicate to others you are working on it. The GitHub issue is the best place to discuss any questions for how to proceed with work on that issue.

- If the change you’d like to make is one of the below common cases, please create a [GitHub Issue ](https://github.com/ERDDAP/erddap/issues)indicating the change you intend to make. Then once the change is complete, make a pull request to request the merge. The common changes include:

  - You want to write another subclass of EDDGrid or EDDTable to handle another data source type. If so, we recommend that you find the closest existing subclass and use that code as a starting point.

  - You want to write another saveAs_FileType_ method. If so, we recommend that you find the closest existing saveAs_FileType_ method in EDDGrid or EDDTable and use that code as a starting point.

Those situations have the advantage that the code you write is self-contained. You won't need to know all the details of ERDDAP's internals. And it will be easy for us to incorporate your code in ERDDAP. Note that if you do submit code, the license will need compatible with the ERDDAP™ [license](https://erddap.github.io/setup.html#license) (e.g., [Apache](https://www.apache.org/licenses/), [BSD](https://www.opensource.org/licenses/bsd-license.php), or [MIT-X](https://www.opensource.org/licenses/mit-license.php)). We'll list your contribution in the [credits](CREDITS.md).

- If you have a feature not covered above that you would like to add to ERDDAP, it is recommended to first create a discussion thread in the [GitHub Discussions](https://github.com/ERDDAP/erddap/discussions/categories/ideas). For significant features/changes the Technical Board will discuss them and decide on whether to approve adding it to ERDDAP™.

### **Judging Your Code Contributions**
If you want to submit code or other changes to be included in ERDDAP, that is great. Your contribution needs to meet certain criteria in order to be accepted. If you follow the guidelines below, you greatly increase the chances of your contribution being accepted.
   

  - The ERDDAP™ project is managed by a  NATD (NOAA Appointed Technical Director) with input from a Technical Board.
    From 2007 (the beginning of ERDDAP) through 2022, that was Bob Simons (also the Founder-Leader). Starting in January 2023, that is Chris John. Basically, the NATD is responsible for ERDDAP, so s/he has the final word on decisions about ERDDAP™ code, notably about the design and whether a given pull request will be accepted or not. It needs to be this way partly for efficiency reasons (it works great for Linus Torvalds and Linux) and partly for security reasons: Someone has to tell the IT security people that s/he takes responsibility for the security and integrity of the code.
     

  - The NATD doesn't guarantee that s/he will accept your code.
    If a project just doesn't work out as well as we had hoped and if it can't be salvaged, the NATD won't include the project in the ERDDAP™ distribution. Please don't feel bad. Sometimes projects don't work out as well as hoped. It happens to all software developers. If you follow the guidelines below, you greatly increase your chances of success.
     

  - It's best if the changes are of general interest and usefulness.
    If the code is specific to your organization, it is probably best to maintain a separate branch of ERDDAP™ for your use. Axiom does this. Fortunately, Git makes this easy to do. The NATD wants to maintain a consistent vision for ERDDAP, not allow it to become a kitchen sink project where everyone adds a custom feature for their project.
     

  - Follow the Java Code Conventions.
    In general, your code should be good quality and should follow the original [Java Code Conventions](https://www.oracle.com/technetwork/java/codeconventions-150003.pdf): put .class files in the proper place in the directory structure, give .class files an appropriate name, include proper JavaDoc comments, include //comments at the start of each paragraph of code, indent with 4 spaces (not tab), avoid lines >80 characters, etc. Conventions change and the source code isn’t always fully up to date. When in doubt, match code to the conventions and not existing code.

- Use descriptive class, method and variable names.
  That makes the code easier for others to read.
   

- Avoid fancy code.
  In the long run, you or other people will have to figure out the code in order to maintain it. So please use simple coding methods that are thus easier for others (including you in the future) to figure out. Obviously, if there is a real advantage to using some fancy Java programming feature, use it, but extensively document what you did, why, and how it works.
   

- Work with the Technical Board before you start.
  If you hope to get your code changes pulled into ERDDAP™, The Technical Board will definitely want to talk about what you're going to do and how you're going to do it before you make any changes to the code. That way, we can avoid you making changes that the NATD, in the end, doesn't accept. When you're doing the work, the NATD and Technical Board is willing to answer questions to help you figure out the existing code and (overall) how to tackle your project.
   

- Work independently (as much as possible) after you start.
  In contrast to the above "Work with the Technical Board", after you get started on the project, the NATD encourages you to work as independently as possible. If the NATD has to tell you almost everything and answer lots of questions (especially ones that you could have answered by reading the documentation or the code), then your efforts aren't a time savings for the NATD and s/he might as well do the work themself. It's the [Mythical Man Month](https://en.wikipedia.org/wiki/The_Mythical_Man-Month) problem. Of course, we should still communicate. It would be great to periodically see your work in progress to make sure the project is on track. But the more you can work independently (after the Technical Board agrees on the task at hand and the general approach), the better.
   

- Avoid bugs.
  If a bug isn't caught before a release, it causes problems for users (at best), returns the wrong information (at worst), is a blot on ERDDAP's reputation, and will persist on out-of-date ERDDAP™ installations for years. Work very hard to avoid bugs. Part of this is writing clean code (so it is easier to see problems). Part of this is writing unit tests. Part of this is a constant attitude of bug avoidance when you write code. Don't make the NATD regret adding your code to ERDDAP™.
   

- Write a unit test or tests.
  For new code, you should write JUnit tests in a test file.
  Please write at least one individual test method that **thoroughly** tests the code you write and add it to the class' JUnit test file so that it is run automatically. Unit (and related) tests are one of the best ways to catch bugs, initially, and in the long run (as other things change in ERDDAP™). As Bob said, "Unit tests are what lets me sleep at night."
   

- Make it easy for the NATD to understand and accept the changes in your pull request.
  Part of that is writing a unit test method(s). Part of that is limiting your changes to one section of code (or one class) if possible. The NATD won't accept any pull request with hundreds of changes throughout the code. The NATD tells the IT security people that s/he takes responsibility for the security and integrity of the code. If there are too many changes or they are too hard to figure out, then it's just too hard to verify the changes are correct and don't introduce bugs or security issues.
   

- Keep it simple.
  A good overall theme for your code is: Keep it simple. Simple code is easy for others (including you in the future) to read and maintain. It's easy for the NATD to understand and thus accept.
   

- Assume long term responsibility for your code.
  In the long run, it is best if you assume ongoing responsibility for maintaining your code and answering questions about it (e.g., in the ERDDAP™ Google Group). As some authors note, code is a liability as well as an asset. If a bug is discovered in the future, it's best if you fix it because no one knows your code better than you (also so that there is an incentive to avoid bugs in the first place). The NATD isn't asking for a firm commitment to provide ongoing maintenance. The NATD is just saying that doing the maintenance will be greatly appreciated.
