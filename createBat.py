import os

batCommand = "@echo off \n" + \
    "start javaw -jar ./runelite-client/target/{} \n" + \
    "exit"

for file in os.listdir(os.getcwd() + "/runelite-client/target"):
    if('shaded' in file):
        batCommand = batCommand.format(file)

        f = open("runRunelite.bat", "w")
        f.write(batCommand)
        f.close()
