import pygetwindow as gw
import time

hasLauncherAppeared = False
hasLauncherDisappeared = False

while True:
    #wait until shows up
    if(len(gw.getWindowsWithTitle("RuneLite Launcher")) == 0 and hasLauncherAppeared == False):
        print("sleep")
        time.sleep(.5)
    elif(len(gw.getWindowsWithTitle("RuneLite Launcher")) > 0):
        hasLauncherAppeared = True
        print("appeared")
        time.sleep(.5)
    elif(hasLauncherDisappeared == True and len(gw.getWindowsWithTitle("RuneLite")) == 1):
        print("disappeared sleep")
        time.sleep(2)
        window = gw.getWindowsWithTitle("RuneLite")[0]
        window.moveTo(-10, -5)
        window.resizeTo(980, 1055)
        break
    elif(len(gw.getWindowsWithTitle("RuneLite Launcher")) == 0 and hasLauncherAppeared == True):
        hasLauncherDisappeared = True
        print("disappeared")
        time.sleep(.5)
    

