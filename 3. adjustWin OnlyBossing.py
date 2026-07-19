import pygetwindow as gw

windows = gw.getWindowsWithTitle("RuneLite")
for window in windows:
  if("RuneLite" in window.title):
    window.resizeTo(980, 1555)
    break
