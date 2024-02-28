# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
RUN wget https://downloads.unidata.ucar.edu/netcdf-java/5.5.3/netcdfAll-5.5.3.jar
RUN mvn install:install-file -Dfile=netcdfAll-5.5.3.jar -DgroupId=edu.ucar -DartifactId=netcdfAll -Dversion=5.5.3 -Dpackaging=jar
COPY . /app/

# Temp dir required for a test.
RUN mkdir /temp/

WORKDIR /app/
RUN mvn -f pom.xml clean package

# Run stage
FROM tomcat:10.1.19-jdk17-temurin-jammy
RUN mkdir /usr/local/tomcat/content/erddap/ -p
RUN mkdir /usr/local/erddap_data/
COPY --from=build /app/target/*.war /usr/local/tomcat/webapps/erddap.war
COPY --from=build /app/content/erddap/* /usr/local/tomcat/content/erddap/
COPY ./config/localSetup.xml /usr/local/tomcat/content/erddap/setup.xml
CMD ["catalina.sh", "run"]
