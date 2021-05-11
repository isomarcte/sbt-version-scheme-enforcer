#!/usr/bin/env bash

set -e

declare PUBLISH='NO'

if git update-index --refresh && git diff-index --quiet @ --
then
    # Publish locally first for the scripted tests.
    sbt '+publishLocal'
    sbt ';clean;scalafixAll --check;scalafmtSbtCheck;scalafmtCheckAll;test;scripted;doc;test:doc;'
    ./run-vcs-tests.sh
    read -r -p 'Continue with publish? Type (YES): ' PUBLISH
    if [ "${PUBLISH:?}" = 'YES' ]
    then
        sbt '+publishSigned'
    else
        echo "${PUBLISH} is not YES. Aborting." 1>&2
    fi
else
    echo 'Uncommited local changes. Aborting' 1>&2
    exit 1
fi
