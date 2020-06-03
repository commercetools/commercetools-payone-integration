FROM openjdk:8-jre-alpine

LABEL maintainer="DevOps <ops@commercetools.de>"

COPY service/build/libs/commercetools-payone-integration.jar /build/commercetools-payone-integration.jar

CMD exec java $JAVA_OPTS -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -jar /build/commercetools-payone-integration.jar

EXPOSE 8080
