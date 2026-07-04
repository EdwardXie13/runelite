@echo off
start javaw -jar -ea ./runelite-client/build/libs/client-1.12.31-SNAPSHOT-shaded.jar --debug --developer-mode
python "2. adjustWin.py"
exit
