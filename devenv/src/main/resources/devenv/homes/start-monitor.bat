@echo off
title "Ember Monitor"

call setup-env.bat

set EMBER_MONITOR_OPTS=-Dember.home=%EMBER_HOME%
%EMBER_DIST%\bin\ember-monitor.bat