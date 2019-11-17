FROM azul/zulu-openjdk:8
MAINTAINER @eaglesakura

ENV _JAVA_OPTIONS="-Dfile.encoding=UTF-8"
# add scripts
ADD swagger-codegen.jar "/usr/local/bin"
ADD entrypoint.sh "/usr/local/bin/entrypoint.sh"
RUN chmod +x "/usr/local/bin/entrypoint.sh"
ENTRYPOINT ["entrypoint.sh"]
