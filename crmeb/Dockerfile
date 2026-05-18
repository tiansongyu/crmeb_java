FROM maven:3.8.8-eclipse-temurin-8 AS build

ARG MODULE=crmeb-admin
ARG JAR_NAME=Crmeb-admin.jar

WORKDIR /workspace
COPY pom.xml ./
COPY crmeb-common/pom.xml crmeb-common/pom.xml
COPY crmeb-service/pom.xml crmeb-service/pom.xml
COPY crmeb-admin/pom.xml crmeb-admin/pom.xml
COPY crmeb-front/pom.xml crmeb-front/pom.xml
COPY crmeb-common/src crmeb-common/src
COPY crmeb-service/src crmeb-service/src
COPY crmeb-admin/src crmeb-admin/src
COPY crmeb-front/src crmeb-front/src

RUN mvn -pl "${MODULE}" -am -DskipTests package

FROM eclipse-temurin:8-jre

ARG MODULE=crmeb-admin
ARG JAR_NAME=Crmeb-admin.jar

WORKDIR /app
ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=prod
COPY --from=build /workspace/${MODULE}/target/${JAR_NAME} /app/app.jar
EXPOSE 20400 20410
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
