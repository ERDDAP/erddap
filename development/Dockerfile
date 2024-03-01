# NOTE: This docker file is currently just meant for local development use.
#
# Usage from project root:
# docker build -f development/Dockerfile -t erddap-docker .
# docker run -p 8080:8080 erddap-docker
#
# Erddap should then be available at http://localhost:8080/erddap

# Set up build env.
FROM maven:3.9.6-eclipse-temurin-17 AS build
RUN wget https://downloads.unidata.ucar.edu/netcdf-java/5.5.3/netcdfAll-5.5.3.jar
RUN mvn install:install-file -Dfile=netcdfAll-5.5.3.jar -DgroupId=edu.ucar -DartifactId=netcdfAll -Dversion=5.5.3 -Dpackaging=jar

RUN mkdir /app/
WORKDIR /app/

# Download dependencies + plugins separately from packaging to allow for caching.
COPY pom.xml .
RUN mvn dependency:go-offline

# Temp dir required for a unit test.
RUN mkdir /temp/

# Copy in source files and build the war file.
ADD download ./download
ADD images ./images
ADD src ./src
ADD WEB-INF ./WEB-INF
RUN mvn -f pom.xml package

# Run the built erddap war via a tomcat instance.
FROM tomcat:10.1.19-jdk17-temurin-jammy
RUN mkdir /usr/local/tomcat/content/erddap/ -p
RUN mkdir /usr/local/erddap_data/
COPY --from=build /app/target/*.war /usr/local/tomcat/webapps/erddap.war
COPY --from=build /app/content/erddap/* /usr/local/tomcat/content/erddap/
COPY ./development/config/localSetup.xml /usr/local/tomcat/content/erddap/setup.xml
CMD ["catalina.sh", "run"]
