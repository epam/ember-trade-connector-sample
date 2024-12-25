#!/bin/bash

echo -ne "\033]0;Ember Monitor\007"

source ./setup-env.sh

export EMBER_MONITOR_OPTS="-Dember.home=$EMBER_HOME"

$EMBER_DIST/bin/ember-monitor