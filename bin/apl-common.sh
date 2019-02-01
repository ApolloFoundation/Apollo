#!/bin/bash
# (C) 2019 Apollo Foundation 
# Common parts for all Apollo scripts

# base dir for data files, etc
APPLICATION=".apl-blockchain"

SCRIPT=`realpath -s $0`
SCRIPT_DIR=`dirname $SCRIPT`
APL_TOP_DIR=`dirname $SCRIPT_DIR`
ECHO_PREFIX="=== Apollo === "
echo "${ECHO_PREFIX} Apollo wallet installed in directory: ${APL_TOP_DIR}"

APL_LIB_DIR=`realpath -s ${APL_TOP_DIR}/lib`
if [ -x ${APL_LIB_DIR} ]; then
    echo -n
else
    APL_LIB_DIR=${APL_TOP_DIR}/apl-exec/target/lib
fi
echo "${ECHO_PREFIX} Apollo wallet libraries installed in directory: ${APL_LIB_DIR}"

#determine version
vers=`ls ${APL_LIB_DIR}/apl-utils*`
vers=`basename $vers`
vers=${vers##apl-utils-}
APL_VERSION=${vers%%.jar}
echo "${ECHO_PREFIX} Apollo verions is: ${APL_VERSION}"

APL_TOOLS_JAR=${APL_LIB_DIR}/apl-tools-${APL_VERSION}.jar

MAIN_JAR=${APL_TOP_DIR}/Apollo.jar
MAIN_GUI_JAR=${APL_LIB_DIR}/apl-desktop-${APL_VERSION}.jar
if [ -r ${MAIN_JAR} ]; then
    echo -n
else
    MAIN_JAR=${APL_TOP_DIR}/apl-exec/target/Apollo.jar
    MAIN_GUI_JAR=${APL_TOP_DIR}/apl-desktop/target/apl-desktop-${APL_VERSION}.jar
fi
echo "${ECHO_PREFIX} Apollo main jar path: ${MAIN_JAR}"

# Java detection code. At the moment it is enough just to check jre in distribution
# or version of system-wide java installation
if [ -x ${APL_TOP_DIR}/jre/bin/java ]; then
    JAVA_CMD=${APL_TOP_DIR}/jre/bin/java
else
  if [[ -n $(type -p java) ]]
  then
    JAVA_CMD=java
  elif [[ (-n "$JAVA_HOME") && (-x "$JAVA_HOME/bin/java") ]]
  then
    JAVA_CMD="$JAVA_HOME/bin/java"
  fi
    JAVA_CMD=java
fi

jdk_version() {
  local result
  local IFS=$'\n'
  # remove \r for Cygwin
  local lines=$("$JAVA_CMD" -Xms32M -Xmx32M -version 2>&1 | tr '\r' '\n')
  if [[ -z $JAVA_CMD ]]
  then
    result=no_java
  else
    for line in $lines; do
      if [[ (-z $result) && ($line = *"version \""*) ]]
      then
        local ver=$(echo $line | sed -e 's/.*version "\(.*\)"\(.*\)/\1/; 1q')
        # on macOS, sed doesn't support '?'
        if [[ $ver = "1."* ]]
        then
          result=$(echo $ver | sed -e 's/1\.\([0-9]*\)\(.*\)/\1/; 1q')
        else
          result=$(echo $ver | sed -e 's/\([0-9]*\)\(.*\)/\1/; 1q')
        fi
      fi
    done
  fi
  echo "$result"
}

JAVA_VER="$(jdk_version)"

echo -n "${ECHO_PREFIX} Using java at path: ${JAVA_CMD}; Version is: ${JAVA_VER};"

if [ "$JAVA_VER" -ge 11 ]; then 
  echo " Java is OK."
else
    echo
    echo "${ECHO_PREFIX} WARNING! Java 11 or later is required. Application could not run properly!"
    JAVA_CMD="echo 'ERROR!!! No suitable JRE found!'"
fi