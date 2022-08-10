#!/bin/sh

# ffmpeg -list_devices true -f dshow -i dummy
ffmpeg -f dshow -i audio="Microphone Array (Технология Intel® Smart Sound)" -acodec libmp3lame -b:a 256k $1.mp3
