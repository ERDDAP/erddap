# ERDDAP™ Development with Maven and Jetty

Follow these steps to build and run ERDDAP™ for local development using Maven and the [Jetty 12](https://www.eclipse.org/jetty/) Servlet Container. This workflow streamlines the [iterative development process for ERDDAP](#iterative-erddap-development-with-maven-and-jetty) (re-compiling Java source code and redeploying static assets).

Upon completion, ERDDAP™ will be available at **http://localhost:8080/erddap**.

<br />


## Deploying ERDDAP™ with Maven and Jetty

First, install Maven following [these directions](https://maven.apache.org/install.html).  

Once complete, you should have the `mvn` command available on your command prompt.  From the root directory of your ERDDAP™ clone, run `mvn validate` to ensure everything is working: 

```
mvn validate
```


### Build ERDDAP&trade;

These steps will download some additional files not included in the ERDDAP™ GitHub repository, create local directories for ERDDAP's "content" (`./content`) and "bigParentDirectory" (`./data`) directories, compile the ERDDAP™ code, and finally create an 'exploded' Java WAR directory to use to run ERDDAP™.  There is also a pre-configured version of ERDDAP's `setup.xml` file that will be copied to the `./content` directory to allow ERDDAP™ to start.

```
mvn compile
mvn war:exploded
```


### Run ERDDAP™ in Jetty

ERDDAP™ can be run in Jetty with the following command:

```
MAVEN_OPTS="-Xms4096m -Xmx4096m" mvn jetty:run -DerddapContentDirectory=development/jetty/config
```

**Access ERDDAP™ at: http://localhost:8080/erddap**.  

**Note:** the `-Xms` and `-Xmx` flags assign memory to the Java process running Jetty, and must be specified using the `MAVEN_OPTS` parameter.  4096M (or 4G) is sufficient for running ERDDAP™ in development mode, however you can tune these values according to your preferences (to limit memory to 2G, use instead: `MAVEN_OPTS="-Xms2048m -Xmx2048m" mvn jetty:run -DerddapContentDirectory=development/jetty/config`).

<br />


## Iterative ERDDAP™ development with Maven and Jetty

Maven and Jetty offers advantages over Tomcat for iterative development, shortening the dev/test cycle and improving developer productivity.  

The following table is a short list of Maven commands describing the workflow to update source files and redeploy ERDDAP™ in Jetty.  Typically, you would run all three in succession to view your changes.

|**Command**|**Description**|
|-----------|---------------|
| `mvn compile` | Compiles ERDDAP™ Java classes to `./target/classes`. <br /><br />This can be skipped if you haven't modified any Java source files.  |
| `mvn war:exploded` | Creates an 'exploded' ERDDAP™ WAR file in `./target/${project.build.finalName}` (currently `./target/ERDDAP-2.24-SNAPSHOT`) to run ERDDAP™ in Jetty, including Java classes that were compiled to `./target/classes` via `mvn compile`. <br /><br />This can be run without the `mvn compile` step if only static (non-Java) files have been modified. | 
| `CR (carriage return)` | Type a CR (carriage return) in the console running the Jetty process (`mvn jetty:run`) to reload the ERDDAP™ web application (most likely only necessary for changes to compiled Java classes). | 

**Note:** both the `mvn compile` and `mvn war:exploded` commands must be run before launching ERDDAP™ via `mvn jetty:run`, or after making code changes and reloading ERDDAP™ via `CR (carriage return)` in the Jetty console, due to some ERDDAP™ filesystem lookups in utility classes like [File2.java](https://github.com/ERDDAP/erddap/blob/33e6e531484b0cc6ed461f0640b1f0ad3b83e45b/WEB-INF/classes/com/cohort/util/File2.java#L216).  These require ERDDAP™ to be run in Jetty in 'exploded WAR' format so that the `WEB-INF` directory is in the path.  

Removing these lookups from ERDDAP™ would allow leveraging Jetty features such as auto-redploying ERDDAP™ on source code changes using the `scan` parameter, as described in the [mvn jetty:run documentation](https://eclipse.dev/jetty/documentation/jetty-12/programming-guide/index.html#jetty-run-goal), and enable running ERDDAP™ from source with only a single command:

```
MAVEN_OPTS="-Xms4096m -Xmx4096m" mvn jetty:run -DerddapContentDirectory=development/jetty/config
```

<br />


### Maven Command Reference

Some other useful commands for developing ERDDAP™ with Maven (not needed for development workflow):

|**Command**|**Description**|
|-----------|---------------|
| `mvn clean` |  Deletes the `${project.build.directory}` (i.e. `./target`) directory. |
| `mvn resources:resources` |  Copies ERDDAP™ static files from `./WEB-INF/classes` to `./target/classes`. |
| `mvn package` |  Builds `./target/${project.build.finalName}.war` (currently `/target/ERDDAP-2.24-SNAPSHOT.war`) distributable ERDDAP™ WAR (suitable for deployment to Tomcat if desired). |