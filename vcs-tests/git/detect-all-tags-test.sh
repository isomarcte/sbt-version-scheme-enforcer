.#!/usr/bin/env sh

set -ex

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

# Register Cleanup trap

if [ -z "$NO_CLEANUP" ]
then
    trap 'rm -rf ${TEMP_DIR}' EXIT
else
    echo "NO_CLEANUP is set, leaving ${TEMP_DIR} on exit."
fi

export VERSION_SCHEME_OUT_FILE="${TEMP_DIR}/out"
export VERSION_SCHEME_VCS_TAGS_FILE="${TEMP_DIR:?}/vcs-tags"

# Functions #

# Wraps check-result.sh to allow us to continue testing even after the
# first failure.
check_result() {
    if check-result.sh "${1:?}"
    then
        return 0
    else
        FAILED=1
    fi
}

# Adds an new commit to our test project. It doesn't matter if any of
# the Scala files are in VCS, just that we have a new commit.
add_commit() {
    touch foo
    echo 'foo ' >> foo
    git add foo
    git commit -m 'foo'
}

# Tests #

cp -r "${TEST_PROJECT_DIR:?}" "${TEST_PROJECT:?}"

cd "$TEST_PROJECT"

# No repo yet

sbt 'show versionSchemeEnforcerVCSTags'
sbt 'show versionSchemeEnforcerProjectInfo'

echo 'repo create'

git init

sbt 'show versionSchemeEnforcerVCSTags'
sbt 'show versionSchemeEnforcerProjectInfo'

echo 'add commit'

add_commit

sbt 'show versionSchemeEnforcerVCSTags'
sbt 'show versionSchemeEnforcerProjectInfo'

echo 'add tag 0'

git tag '0.0.1' @

sbt 'show versionSchemeEnforcerVCSTags'
sbt 'show versionSchemeEnforcerProjectInfo'

echo 'add tag 1'

git tag '0.1.1' @

sbt 'show versionSchemeEnforcerVCSTags'
sbt 'show versionSchemeEnforcerProjectInfo'
