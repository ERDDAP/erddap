# This docker file is currently just meant for local development use.
#
# Usage from project root:
# docker build -f development/Dockerfile -t erddap-docker .
# docker run -p 8080:8080 erddap-docker
#
# ERDDAP should then be available at http://localhost:8080/erddap

# Set up build env.
FROM maven:3.9.6-eclipse-temurin-21 AS build

RUN mkdir /app/
WORKDIR /app/

# Temp dir required for a unit test.
RUN mkdir /temp/

# Copy in source files and build the war file.
ADD development ./development
ADD download ./download
ADD images ./images
ADD src ./src
ADD WEB-INF ./WEB-INF
ADD .mvn ./.mvn
COPY pom.xml .
RUN --mount=type=cache,id=m2_repo,target=/root/.m2/repository mvn package -Dgcf.skipInstallHooks

# Run the built erddap war via a tomcat instance.
FROM tomcat:10.1.19-jdk21-temurin-jammy
RUN mkdir /usr/local/tomcat/content/erddap/ -p
RUN mkdir /usr/local/erddap_data/
COPY --from=build /app/target/*.war /usr/local/tomcat/webapps/erddap.war
COPY --from=build /app/content/erddap/* /usr/local/tomcat/content/erddap/
COPY ./development/docker/config/localSetup.xml /usr/local/tomcat/content/erddap/setup.xml
CMD ["catalina.sh", "run"]
