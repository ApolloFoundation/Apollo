#!/bin/bash
echo "*********************************************************************"
echo "* Start height monitor to compare heights on some peers to monitor  *"
echo "* possible forks                                                    *"
echo "* This shell file will start height monitor, which every 30 sec     *"
echo "* check heights on peers, which ips you can specify in separate file*"
echo "* and log max block diff between peers for future analysis          *"
echo "* Can be run without parameter, but you can also pass cmd params    *"
echo "* Cmd parameters (order is important) (optional)                    *"
echo "* a) path to peers file, where peers ips specified, one ip per line *"
echo "* b) list of time periods in hours to log max block diff for period *"
echo "*    periods separated by whitespace *"
echo "* c) delay of height monitor checks, by default is 30 sec           *"
echo "* TODO fix app launch                                               *"
echo "*********************************************************************"
SCRIPT=`realpath -s $0`
DIR=`dirname $SCRIPT`
 . ${DIR}/../bin/apl-common.sh

${JAVA_CMD}  -jar ${APL_TOOLS_JAR} heightmon
exit $?

