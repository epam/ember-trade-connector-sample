#!/bin/bash

echo -ne "\033]0;Ember\007"

source ./setup-env.sh

$EMBER_DIST/bin/ember