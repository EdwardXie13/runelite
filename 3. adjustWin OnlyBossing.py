import pygetwindow as gw

windows = gw.getWindowsWithTitle("RuneLite")
for window in windows:
  if("RuneLite" in window.title):
    window.moveTo(-8, -5)
    window.resizeTo(1300, 1055)
    break
