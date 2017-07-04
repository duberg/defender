#!/usr/bin/env bash

cd "$(dirname "$0")"/..
sbt -Denv=docker defender-integration/docker:publishLocal