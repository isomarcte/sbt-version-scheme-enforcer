#!/usr/bin/env sh
#
# Utility script to run the
# outputVersionSchemeEnforcerPreviousVersionTask and then check the
# result. We do this operation for all VCS types in multiple test
# types.

set -e

EXPECTED="${1:?}"
RESULT=''

sbt outputVersionSchemeEnforcerPreviousVersionTask

RESULT="$(cat "${VERSION_SCHEME_OUT_FILE:?}")"

if [ "${RESULT:?}" != "${EXPECTED:?}" ]
then
    echo "Expected ${EXPECTED:?}, Got ${RESULT:?}" 1>&2
    return 1
else
    return 0
fi
