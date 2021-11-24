import os
import shutil

#overwrite local settings with cloud
localSettings = os.path.expanduser('~') + "\.runelite\settings.properties"
shutil.copy(localSettings, './')
