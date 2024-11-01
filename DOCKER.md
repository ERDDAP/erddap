# ERDDAP&trade; Docker Image

The Dockerfile included in this project builds the offical ERDDAP&trade; Docker image.
The Dockerfile uses [Apache Maven](https://maven.apache.org/) to package the application into a WAR file,
and serves the application using [Apache Tomcat](https://tomcat.apache.org/).

By default the local ERDDAP source code is used to build the image, but arbitrary git
repositories and branches can alternately be used in the build.

## Building the image

To build the docker image you can run the following command from the root of the ERDDAP&trade; project:

```bash
docker build -t erddap-docker .
```

The initial build of ERDDAP&trade; may take a fair amount of time, but the Dockerfile uses cache mounts
in order to speed up subsequent builds of the application by caching dependencies.
It is worth noting that the ERDDAP&trade; unit tests are ran as part of the build stage, while
integration tests are skipped.

### Building from git

To build an image with source code from a specific git repository and branch instead of the local
source, set build arguments `BUILD_FROM_GIT=1`, `ERDDAP_GIT_URL=<url_to_repo>`,
and `ERDDAP_GIT_BRANCH=<tag_or_branch>`. If `ERDDAP_GIT_BRANCH` is not a tag and is a branch
whose contents can change over time, `ERDDAP_GIT_CACHE_BUST` should also be set to a unique value
to force Docker to not cache a previous build layer and instead fetch and build the source.

Example:

```
docker build --build-arg BUILD_FROM_GIT=1 \
  --build-arg ERDDAP_GIT_URL=https://github.com/someuser/erddap \
  --build-arg ERDDAP_GIT_BRANCH=experimental-feature-3 \
  --build-arg ERDDAP_GIT_CACHE_BUST=$(date +%s) \
  -t erddap-docker:experimental-feature-3 .
```

## Running the image
Once the image has been built, the following command can be used run an ERDDAP&trade; container:

```bash
docker run -p 8080:8080 erddap-docker
```

The `--detach` or `-d` flag can be added to detach this process from your terminal.

ERDDAP&trade; will then be accessible at the URL `http://localhost:8080/erddap`.

## Running with Docker Compose

An example Docker Compose stack is provided in `docker-compose.yml`. This stack will
serve the default ERDDAP&trade; demonstration datasets unless a `datasets.xml` file is
mounted as a volume to `/usr/local/tomcat/content/erddap/datasets.xml`.

To build or rebuild the image:

```
docker compose build
```

To run the stack:

```
docker compose up -d
```

An ERDDAP&trade; instance should then be available at <http://localhost:8080>.

To view and tail Tomcat and ERDDAP&trade; logs:

```
docker compose logs -f
```

To shut down the stack:

```
docker compose down
```

Many options can be customized by setting environment variables (`ERDDAP_PORT` etc).
See the `docker-compose.yml` file for details.

## Config

By default generic setup values are set in the Docker image. You can and should customize those values
using [environment variables](https://github.com/ERDDAP/erddap/blob/main/DEPLOY_INSTALL.md#setupEnvironmentVariables)
and/or a custom `setup.xml` file mounted to `/usr/local/tomcat/content/erddap/setup.xml`

For example, to set the ERDDAP&trade; base URL, set environment variable `ERDDAP_baseUrl=http://yourhost:8080`
on the Docker container.

```
docker run -p 8080:8080 -e ERDDAP_baseUrl=http://yourhost:8080` erddap-docker
```

Similarly, the default ERDDAP&trade; demonstration datasets will be served unless a custom `datasets.xml`
file is mounted as a volume to `/usr/local/tomcat/content/erddap/datasets.xml`.
