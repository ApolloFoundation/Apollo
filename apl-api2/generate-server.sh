#!/bin/bash
BASE_DIR=generator
GENERATOR_VERSION=3.0.19
GENERATOR_REMOTE_JAR=https://repo1.maven.org/maven2/io/swagger/codegen/v3/swagger-codegen-cli/${GENERATOR_VERSION}/swagger-codegen-cli-${GENERATOR_VERSION}.jar
GENERATOR_JAR=$BASE_DIR/swagger-codegen-cli-${GENERATOR_VERSION}.jar
CONFIG_FILE=$BASE_DIR/java-openapi-resteasy.json
OUTPUT_DIR=./
if [ ! -f $GENERATOR_JAR ]; then
    echo "Generator not found. Try downloading remote file $GENERATOR_REMOTE_JAR"
    curl --silent --output $GENERATOR_JAR $GENERATOR_REMOTE_JAR
    if [ ! -f $GENERATOR_JAR ]; then
        echo "Generator not found."
        exit 1
    fi
fi
export JAVA_OPTS="${JAVA_OPTS} -Xmx1024M -Dlogback.configurationFile=logback.xml"

echo "Removing files and folders under $OUTPUT_DIR/src"
rm -rf $OUTPUT_DIR/src/gen
rm -rf $OUTPUT_DIR/target

INPUT_SPEC=(src/main/resources/yaml/apollo-api-v2.yaml src/main/resources/yaml/apollo-auth-api.yaml)
for spec in ${INPUT_SPEC[*]}; do
    echo "Use specification: $spec"
    GENERATOR_GENERATOR_ARGS=" generate -DhideGenerationTimestamp=true --config $CONFIG_FILE --lang jaxrs-resteasy --output $OUTPUT_DIR --input-spec $spec"
    java $JAVA_OPTS -jar $GENERATOR_JAR $GENERATOR_GENERATOR_ARGS
done

find $OUTPUT_DIR -maxdepth 1 -type f -name "*.gradle" -delete
rm -rf $OUTPUT_DIR/src/main/webapp
rm $OUTPUT_DIR/.swagger-codegen-ignore
