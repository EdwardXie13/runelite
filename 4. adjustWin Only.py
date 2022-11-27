import pygetwindow as gw

windows = gw.getWindowsWithTitle("RuneLite")
for window in windows:
  if(window.title == "RuneLite"):
    window.moveTo(-8, -5)
    window.resizeTo(980, 1055)
    break
