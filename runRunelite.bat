@echo off 
start javaw -jar -ea ./runelite-client/target/client-1.10.29-SNAPSHOT-shaded.jar --debug --developer-mode
python "2. adjustWin.py"
exit