import os
import shutil

#create Bat
batCommand = "@echo off \n" + \
    "start javaw -jar -ea ./runelite-client/target/{} --debug --developer-mode\n" + \
    'python "2. adjustWin.py"\n' + \
    "exit"

for file in os.listdir(os.getcwd() + "/runelite-client/target"):
    if('shaded' in file):
        batCommand = batCommand.format(file)

        f = open("runRunelite.bat", "w")
        f.write(batCommand)
        f.close()


#overwrite local settings with cloud
currentUser = os.path.expanduser('~')
currentUser += "\.runelite"

shutil.copy('./settings.properties', currentUser)
