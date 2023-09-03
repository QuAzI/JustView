from base64 import encode
from genericpath import isfile
from glob import escape
import os
import pathlib
import subprocess
import sys

# check microphone with
#   ffmpeg -list_devices true -f dshow -i dummy

microphone = 'Внешний микрофон (Realtek(R) Audio)'

workdir = os.getcwd()
for r, d, f in os.walk(workdir):
    for file in f:
        extension = pathlib.Path(file).suffix.lower()
        if extension in [".jpg", ".jpeg", ".png", ".webp"]:
            imageFile = os.path.join(r, file)
            basename = pathlib.Path(file).stem
            voiceFile = os.path.join(r, basename + '.mp3')

            print(f'>> Word: {basename}')
            if os.path.isfile(voiceFile):
                print('    Voice already present')
                continue

            input("    Press Enter to start rec and Q to finish...")
            subprocess.call(" ".join([
                'ffmpeg', 
                '-f', 'dshow',
                '-i', f'audio="{microphone}"',
                '-acodec', 'libmp3lame',
                '-b:a', '256k',
                '-t 5',
                f'"{voiceFile}"'
            ]), shell=True)
