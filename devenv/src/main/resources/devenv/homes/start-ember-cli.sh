#!/bin/bash

echo -ne "\033]0;Ember CLI\007"

source ./setup-env.sh

$EMBER_DIST/bin/ember-cli
