#!/usr/bin/env bash

cd "$(dirname "$0")"/..
sbt -Denv=prod debian:build