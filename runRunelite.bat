@echo off
start javaw -jar -ea ./runelite-client/build/libs/client-1.13.7-SNAPSHOT-shaded.jar --debug --developer-mode
python "2. adjustWin.py"
exit
