import os

batCommand = "@echo off \n" + \
    "start javaw -jar ./runelite-client/target/{} \n" + \
    "exit"

for file in os.listdir(os.getcwd() + "/runelite-client/target"):
    if('shaded' in file):
        command = batCommand.format(file)
        #print(command)
        
f = open("runRunelite.bat", "w")
f.write(command)
f.close()
