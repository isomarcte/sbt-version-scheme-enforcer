#!/usr/bin/env sh
#
# Utility script to run the
# outputVersionSchemeEnforcerPreviousVersionTask and then check the
# result. We do this operation for all VCS types in multiple test
# types.

set -ex

EXPECTED="${1:?}"
RESULT=''

sbt outputVersionSchemeEnforcerPreviousVersionTask

RESULT="$(cat "${VERSION_SCHEME_OUT_FILE:?}")"

if [ "${RESULT:?}" != "${EXPECTED:?}" ]
then
    echo "Expected ${EXPECTED:?}, Got ${RESULT:?}" 1>&2
    exit 1
else
    exit 0
fi
