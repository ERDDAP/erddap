# Build the ERDDAP war from source
FROM maven:3.9.9-eclipse-temurin-21-jammy AS build

# install zip so certain tests can pass
RUN apt-get update && \
    apt-get install -y --no-install-recommends git zip

WORKDIR /app/

# Copy in source files and build the war file.
COPY development ./development
COPY download ./download
COPY images ./images
COPY src ./src
COPY WEB-INF ./WEB-INF
COPY .mvn ./.mvn
COPY pom.xml .

# if BUILD_FROM_GIT == 1, use code from git clone instead of local source
ARG BUILD_FROM_GIT=0
ARG ERDDAP_GIT_URL=https://github.com/ERDDAP/erddap.git
ARG ERDDAP_GIT_BRANCH=main
ARG ERDDAP_GIT_CACHE_BUST=1
RUN if [ "$BUILD_FROM_GIT" = "1" ] && [ -n "$ERDDAP_GIT_URL" ] && [ -n "$ERDDAP_GIT_BRANCH" ]; then \
      find . -mindepth 1 -delete; \
      git clone ${ERDDAP_GIT_URL} --depth 1 --branch ${ERDDAP_GIT_BRANCH} .; \
    fi

ARG SKIP_TESTS=false
RUN --mount=type=cache,id=m2_repo,target=/root/.m2/repository \
    mvn --batch-mode -DskipTests=${SKIP_TESTS} -Dgcf.skipInstallHooks=true \
    -Ddownload.unpack=true -Ddownload.unpackWhenChanged=false \
    -Dmaven.test.redirectTestOutputToFile=true package \
    && find target -maxdepth 1 -type d -name 'ERDDAP-*' -exec mv {} target/ERDDAP \;

# Run the built erddap war via a tomcat instance
FROM tomcat:11.0.7-jdk21-temurin-jammy

RUN apt-get update && apt-get install -y \
    unzip \
    zip \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Remove default Tomcat web applications
RUN rm -rf ${CATALINA_HOME}/webapps/* ${CATALINA_HOME}/webapps.dist

COPY --from=build /app/content /usr/local/tomcat/content
COPY --from=build /app/target/ERDDAP /usr/local/tomcat/webapps/erddap

# Redirect root path / to /erddap
RUN mkdir "${CATALINA_HOME}/webapps/ROOT" \
  && echo '<% response.sendRedirect("/erddap"); %>' > "${CATALINA_HOME}/webapps/ROOT/index.jsp"

COPY ./docker/tomcat/conf/server.xml ./docker/tomcat/conf/context.xml "${CATALINA_HOME}/conf/"
COPY ./docker/tomcat/bin/setenv.sh "${CATALINA_HOME}/bin/"

# Set placeholder values for setup.xml
ENV ERDDAP_deploymentInfo="docker" \
    ERDDAP_bigParentDirectory="/erddapData" \
    ERDDAP_baseUrl="http://localhost:8080" \
    ERDDAP_baseHttpsUrl="https://localhost:8443" \
    ERDDAP_emailEverythingTo="set-me@domain.com" \
    ERDDAP_emailDailyReportsTo="set-me@domain.com" \
    ERDDAP_emailFromAddress="set-me@domain.com" \
    ERDDAP_emailUserName="" \
    ERDDAP_emailPassword="" \
    ERDDAP_emailProperties="" \
    ERDDAP_emailSmtpHost="" \
    ERDDAP_emailSmtpPort="" \
    ERDDAP_adminInstitution="Set-me Institution" \
    ERDDAP_adminInstitutionUrl="https://set-me.invalid" \
    ERDDAP_adminIndividualName="Firstname Surname" \
    ERDDAP_adminPosition="ERDDAP Administrator" \
    ERDDAP_adminPhone="555-555-5555" \
    ERDDAP_adminAddress="123 Simons Ave." \
    ERDDAP_adminCity="Anywhere" \
    ERDDAP_adminStateOrProvince="MD" \
    ERDDAP_adminPostalCode="12345" \
    ERDDAP_adminCountry="USA" \
    ERDDAP_adminEmail="set-me@domain.com"

ENV ERDDAP_VERSION_SUFFIX="docker"

COPY ./docker/entrypoint.sh /entrypoint.sh

VOLUME /erddapData

ENTRYPOINT ["/entrypoint.sh"]
EXPOSE 8080
CMD ["catalina.sh", "run"]
