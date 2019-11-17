#! /bin/bash -eu

echo "build_version=$1"
./gradlew -Pexecutable -Pbuild_version=$1 clean dockerArtifacts
cd docker
docker build -t eaglesakura/swagger-codegen:$1 .

