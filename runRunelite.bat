@echo off 
start javaw -jar -ea ./runelite-client/target/client-1.8.15-SNAPSHOT-shaded.jar --developer-mode
python "2. adjustWin.py"
exit