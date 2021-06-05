#!/usr/bin/env sh

set -e

# Variables #

TEMP_DIR=''
FAILED=0

if [ -z "$PLUGIN_VERSION" ]
then
    echo 'PLUGIN_VERSION is not set. This is required for the VCS tests' 1>&2
    exit 1
fi

if [ -z "$TEST_PROJECT_DIR" ]
then
    echo 'TEST_PROJECT_DIR is not set. This is required for the VCS tests' 1>&2
    exit 1
fi

if [ -z "$VERSION_SCHEME" ]
then
    echo 'VERSION_SCHEME is not set. This is required for the VCS tests' 1>&2
    exit 1
fi

ORIGINAL_WD="${PWD:?}"

# Use minimal arguments for portability. Technically mktemp(1) is not
# in POSIX, but most platforms, Linux, *BSD, Solaris have it
# defined. That said, they do all have the same feature set.
TEMP_DIR="$(mktemp -d)"
TEST_PROJECT="${TEMP_DIR}/test-project"

# Setup #

cp -vr "${TEST_PROJECT_DIR:?}" "${TEST_PROJECT:?}"

cd "$TEST_PROJECT"

# No tags yet

git init -b main

# Github Actions CI test fails if these are not set.
git config user.email 'ci-test@loopback'
git config user.name 'ci-test'
git config commit.gpgSign 'false'

echo 'ThisBuild / versionSchemeEnforcerInitialVersion := Some("0.0.0.1")' >> version.sbt
echo 'ThisBuild / version := "0.0.0.1-SNAPSHOT"' >> version.sbt

# Tests #

## If the versionSchemeEnforcerInitialVersion value is set and it is
## the same as the current version (minus -SNAPSHOT), then
## versionSchemeEnforcerCheck should be a no-op

sbt versionSchemeEnforcerCheck

cd "$ORIGINAL_WD"

rm -rf "${TEMP_DIR}"

exit "$FAILED"
