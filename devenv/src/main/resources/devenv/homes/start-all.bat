@echo off
title "Launch All Servers"

call setup-env.bat

rem Start TimeBase 5.6
start %ANVIL_HOME%\qshome\services\TimeBase\TimeBase.cmd

timeout 10

rem Start Ember
start %ANVIL_HOME%\start-ember.bat

timeout 10

rem Start Ember Monitor
start %ANVIL_HOME%\start-monitor.bat

timeout 5

rem Open Ember Monitor in browser
start "" http://localhost:8988

rem Start Ember CLI
start %ANVIL_HOME%\start-ember-cli.bat
