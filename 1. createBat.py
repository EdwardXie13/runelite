import os

jar_dir = os.path.join(os.getcwd(), "runelite-client", "build", "libs")

jar_file = None
for file in os.listdir(jar_dir):
    if "shaded" in file and file.endswith(".jar"):
        jar_file = file
        break

if jar_file is None:
    raise FileNotFoundError("No shaded jar found in build/libs")

bat_command = (
    "@echo off\n"
    f"start javaw -jar -ea ./runelite-client/build/libs/{jar_file} --debug --developer-mode\n"
    'python "2. adjustWin.py"\n'
    "exit\n"
)

with open("runRunelite.bat", "w") as f:
    f.write(bat_command)

print(f"Created runRunelite.bat using: {jar_file}")
