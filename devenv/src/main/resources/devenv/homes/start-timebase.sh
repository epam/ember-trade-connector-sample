#!/bin/bash

echo -ne "\033]0;TimeBase\007"

source ./setup-env.sh

export QS_HOME=@devenvQsHome@

$QS_HOME/services/TimeBase/TimeBase