#!/bin/bash

source ./setup-env.sh
export DELTIX_HOME=$DELTIX_HOME_52

$DELTIX_HOME_52/bin/qsadmin.sh -home "$ANVIL_HOME/qshome"

exit 0