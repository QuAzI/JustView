#!/bin/bash

# Generating short videos from images and audiofiles
# Image and his audio must have same names. In other case audio will be generated from image file name

shopt -s nullglob
for fullfile in *.{jpg,jpeg,png,webp} ;
do
  echo Image: $fullfile
  
  filename=$(basename -- "$fullfile")
  extension="${filename##*.}"
  filename="${filename%.*}"
  
  mp3=$filename.mp3
  audio=$mp3
  if [ ! -f "$audio" ]; then
    audio=$filename.wav
    if [ ! -f "$audio" ]; then
      echo Audio generation: $audio
      # espeak-ng -v ru -s 100 -w "$audio" "$filename"
      echo $filename | text2wave -eval '(voice_msu_ru_nsh_clunits)' -o "$audio"
    fi
  fi
  echo Audio: $audio
  
  if [[ "$audio" == "$mp3" ]]; then
    # ffmpeg -y -i image.png -i audio.mp3 -c:a copy result.mp4
    ffmpeg -loop 1 -y -i "$fullfile" -itsoffset 1 -i "$audio" -map 0:v -map 1:a -c:v libx264 -preset veryslow -tune stillimage -c:a copy -t 5 out/"$filename.mp4"
  else
    ffmpeg -loop 1 -y -i "$fullfile" -itsoffset 1 -i "$audio" -map 0:v -map 1:a -c:v libx264 -preset veryslow -tune stillimage -c:a aac -b:a 128k -filter:a "atempo=0.8" -t 5 "out/$filename.mp4"
  fi
done
