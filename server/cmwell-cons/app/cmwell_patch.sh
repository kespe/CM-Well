#!/bin/sh
cd $(dirname -- "$0")
source ./cmw.env

scala -nc -cp 'cons-lib/*' -I cmw.conf -e "cmw.genResources(); cmw.restartCassandra ; sys.exit(0)" 2>&1 | bash -c 'tee >( grep -w -v "^#.*#" > out.log )' | grep -w -v "^#.*#"
