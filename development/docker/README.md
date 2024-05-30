# ERDDAP&trade; Development with Docker/Tomcat

## DockerFile
For development purposes only, a DockerFile has been created in order to streamline the building and deploying of an ERDDAP&trade; instance locally. This DockerFile uses [Apache Maven](https://maven.apache.org/) to package the application into a WAR file, and then [Apache Tomcat](https://tomcat.apache.org/) to serve the application.

### Building the image:
To build the docker image you can run the following command from the root of the ERDDAP&trade; project:
```bash
docker build -f development/docker/Dockerfile -t erddap-docker .
```
The initial build of ERDDA&trade; may take a fair amount of time, but the DockerFile uses cache mounts in order to speed up subsequent builds of the application by caching dependencies.
It is worth noting that the ERDDAP&trade; unit tests are ran as part of the build stage.

### Running the image:
Once the image has been built, the following command can be used run an ERDDAP&trade; container:
```bash
# The --detach or -d flag can be added to detach this process from your terminal.
docker run -p 8080:8080 erddap-docker
```

ERDDAP&trade; will then be accessible at the URL `http://localhost:8080/erddap`. Due to Tomcat having to deploy the WAR file, you may have to wait a minute for ERDDAP&trade; to be accessible.

### Config
If required, you can edit the file `development/docker/config/localSetup.xml` to customize your local instance of ERDDAP&trade;. Currently this file contains many placeholder values due to the nature of this DockerFile being intended for development use only.

### Datasets
Currently the DockerFile uses the default datsets provided by ERDDAP&trade;. Feel free to extend this DockerFile yourself to allow for the use of custom datasets within the container.
