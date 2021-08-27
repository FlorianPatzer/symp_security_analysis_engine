FROM openjdk:14-alpine

RUN apk add --no-cache wget \
 && wget https://archive.apache.org/dist/jena/binaries/apache-jena-fuseki-3.17.0.tar.gz

RUN tar -xzf /apache-jena-fuseki-3.17.0.tar.gz \
 && mv /apache-jena-fuseki-3.17.0 /fuseki \
 && rm /apache-jena-fuseki-3.17.0.tar.gz

WORKDIR /fuseki

COPY config-tdb2.ttl .

RUN adduser -H -D fuseki fuseki

RUN chown -R fuseki /fuseki \
 && chmod a+x fuseki-server

ENV JAVA_OPTIONS="-Xmx2048m -Xms2048m"

USER fuseki
EXPOSE 3030
ENTRYPOINT ["./fuseki-server", "--tdb2", "--config=config-tdb2.ttl"]
