import pygetwindow as gw
import sys

def moveWindow(x, y):
  windows = gw.getWindowsWithTitle("RuneLite")
  for window in windows:
    if("RuneLite" in window.title):
      #window.moveTo(-8, -5)
      window.move(x + (-8), y + (-5))
      window.resizeTo(980, 1055)
      break

if __name__ == "__main__":
  x = int(sys.argv[1])
  y = int(sys.argv[2])
  moveWindow(x, y)
