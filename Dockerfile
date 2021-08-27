FROM adoptopenjdk/openjdk11:jdk-11.0.9.1_1-alpine as build
RUN apk add --no-cache nodejs && apk add --no-cache npm

WORKDIR /build

ARG PROFILE

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# build all dependencies
RUN ./mvnw dependency:go-offline -B

COPY src src

RUN ./mvnw install -DskipTests
RUN mkdir -p target/dependency && (cd target/dependency; find ../ -maxdepth 1 -name "*.jar" -exec jar -xf {} \;)

FROM adoptopenjdk/openjdk11:jdk-11.0.9.1_1-alpine
VOLUME /tmp

#remove these in production
RUN apk add --no-cache nodejs && apk add --no-cache npm
COPY --from=build /build /build
COPY /frontend /build/frontend

COPY plugins plugins

ARG DEPENDENCY=/build/target/dependency
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app

ENTRYPOINT ["java","-Dspring.profiles.active=${PROFILE}","-cp","app:app/lib/*","de.fraunhofer.iosb.svs.sae.Application"]
EXPOSE 8543
