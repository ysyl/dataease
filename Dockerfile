FROM registry.cn-qingdao.aliyuncs.com/dataease/alpine-openjdk17-jre

ARG IMAGE_TAG

RUN mkdir -p /opt/apps/config /opt/dataease2.0/drivers/ /opt/dataease2.0/cache/ /opt/dataease2.0/data/map /opt/dataease2.0/data/static-resource/

ADD drivers/* /opt/dataease2.0/drivers/
ADD mapFiles/ /opt/dataease2.0/data/map/
ADD staticResource/ /opt/dataease2.0/data/static-resource/

WORKDIR /opt/apps

ADD core/core-backend/target/CoreApplication.jar /opt/apps/app.jar
ADD de-xpack/xpack-permissions/target/xpack-permissions-$IMAGE_TAG.jar /opt/apps/xpack-permission.jar
ADD de-xpack/xpack-base/target/xpack-base-$IMAGE_TAG.jar /opt/apps/xpack-base.jar

ENV JAVA_APP_JAR=/opt/apps/app.jar

HEALTHCHECK --interval=15s --timeout=5s --retries=20 --start-period=30s CMD curl -f 127.0.0.1:8100


CMD java -Xmx1024m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/opt/dataease2.0/logs/dump.hprof -Dloader.path=/opt/apps/xpack-permission.jar,/opt/apps/xpack-base.jar -jar /opt/apps/app.jar
