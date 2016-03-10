FROM java:openjdk-8-jre

MAINTAINER DevOps <ops@commercetools.de>

COPY service/build/libs/commercetools-payone-integration.jar /build/commercetools-payone-integration.jar

CMD ["java", "-jar", "/build/commercetools-payone-integration.jar"]

EXPOSE 8080
