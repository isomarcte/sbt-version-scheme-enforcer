.#!/usr/bin/env sh
#
# Tests for git VCS system.

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

export VERSION_SCHEME_OUT_FILE="${TEMP_DIR}/out"

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

# No tags yet

git init -b main

# Github Actions CI test fails if these are not set.
git config user.email 'ci-test@loopback'
git config user.name 'ci-test'
git config commit.gpgSign 'false'

add_commit

check_result 'None'

# Set versionSchemeEnforcerInitialVersion, this should be inferred as
# the fallback fro versionSchemeEnforcerPreviousVersion

# sbt-dynver test

echo 'ThisBuild / versionSchemeEnforcerInitialVersion := Some("1.0.0+0-1234abcd+20140707-1030")' >> initial.sbt

check_result 'Some(1.0.0+0-1234abcd+20140707-1030)'

echo 'ThisBuild / versionSchemeEnforcerInitialVersion := Some("1.0.0.0")' >> initial.sbt

check_result 'Some(1.0.0.0)'

echo 'ThisBuild / versionSchemeEnforcerPreviousVersion := Some("0.0.2.0")' >> initial.sbt

check_result 'Some(0.0.2.0)'

rm initial.sbt

# One tag, no new commits

git tag '0.0.0.1' @

check_result 'Some(0.0.0.1)'

# Two tags, should only find the most recent one

add_commit

git tag '0.0.0.2' @

check_result 'Some(0.0.0.2)'

# Tag filtering test

add_commit

git tag '0.0.0.3-M1'

check_result 'Some(0.0.0.3-M1)'

add_commit

git tag '0.0.0.3-M2'

check_result 'Some(0.0.0.3-M2)'

## Enable tag filtering of milestones

echo 'ThisBuild / versionSchemeEnforcerPreviousVCSTagFilter := _root_.io.isomarcte.sbt.version.scheme.enforcer.plugin.TagFilters.noMilestoneTagFilter' > tag-filter.sbt

check_result 'Some(0.0.0.2)'

rm tag-filter.sbt

# Tag Domain Tests #

git checkout -b branchA

check_result 'Some(0.0.0.3-M2)'

git branch branchB 0.0.0.3-M2

git checkout branchB

add_commit

git tag '0.0.0.4'

check_result 'Some(0.0.0.4)'

git checkout branchA

# Default domain is TagDomain.All, so we should see 0.0.0.4 here.

check_result 'Some(0.0.0.4)'

echo 'ThisBuild / versionSchemeEnforcerTagDomain := _root_.io.isomarcte.sbt.version.scheme.enforcer.plugin.TagDomain.All' > tag-domain.sbt

# Should be the same as default
check_result 'Some(0.0.0.4)'

echo 'ThisBuild / versionSchemeEnforcerTagDomain := _root_.io.isomarcte.sbt.version.scheme.enforcer.plugin.TagDomain.Reachable' > tag-domain.sbt

# Should be 0.0.0.3-M2 because 0.0.0.4 is not reachable from branchA
check_result 'Some(0.0.0.3-M2)'

echo 'ThisBuild / versionSchemeEnforcerTagDomain := _root_.io.isomarcte.sbt.version.scheme.enforcer.plugin.TagDomain.Unreachable' > tag-domain.sbt

git --no-pager tag --sort=-creatordate --no-merged @

# Should be 0.0.0.4 because all other tags are reachable from branchA
check_result 'Some(0.0.0.4)'

echo 'ThisBuild / versionSchemeEnforcerTagDomain := _root_.io.isomarcte.sbt.version.scheme.enforcer.plugin.TagDomain.Contains' > tag-domain.sbt

# Should be 0.0.0.4 because it is the _only_ tag which contains this commit
check_result 'Some(0.0.0.4)'

cd "$ORIGINAL_WD"

rm -rf "${TEMP_DIR}"

exit "$FAILED"
