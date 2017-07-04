#!/usr/bin/env bash

cd "$(dirname "$0")"/..
sbt -Denv=dev debian:build