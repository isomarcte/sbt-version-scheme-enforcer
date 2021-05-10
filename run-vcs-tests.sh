#!/usr/bin/env sh
#
# Runs tests which for the VCS systems. We have to run these in
# temporary directories outside of the current working directory
# because, at least in the case of git, we don't can have issues
# creating a clean git project when we are already inside a git
# project.

set -ex

sbt +publishLocal

PLUGIN_VERSION="$(grep -o '"[^"].*"' version.sbt | tr -d '"')"
export PLUGIN_VERSION
export TEST_PROJECT_DIR="${PWD:?}/vcs-tests/test-project"
export VERSION_SCHEME="pvp"
export PATH="${PATH:?}:${PWD:?}/vcs-tests/common"

# Git #

./vcs-tests/git/all.sh
