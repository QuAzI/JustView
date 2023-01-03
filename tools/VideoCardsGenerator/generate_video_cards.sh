#!/bin/bash

# Generating short videos from images and audiofiles
# Image and his audio must have same names. In other case audio will be generated from image file name

mkdir ./out

shopt -s nullglob
for fullfile in *.{jpg,jpeg,png,webp,avif} ;
do
  echo Image: $fullfile
  
  filename=$(basename -- "$fullfile")
  extension="${filename##*.}"
  filename="${filename%.*}"
  
  destfile="out/$filename.mp4"
  if [ ! -f "$destfile" ]; then
      mp3=$filename.mp3
      audio=$mp3
      if [ ! -f "$audio" ]; then
        audio=$filename.wav
        if [ ! -f "$audio" ]; then
          echo Audio generation: $audio
          
          # sudo apt install espeak-ng
          #espeak-ng -v ru -s 100 -w "$audio" "$filename"
          
          # sudo apt install festival festvox-ru
          #echo $filename | text2wave -eval '(voice_msu_ru_nsh_clunits)' -o "$audio"
          
          # sudo pip3 install gTTS - atempo=1.2 required
          /usr/local/bin/gtts-cli "$filename" --output "$audio" -s -l ru --nocheck
          
          # RHVoice https://github.com/RHVoice/RHVoice
          #echo $filename | RHVoice-test -p pavel -o "$audio" -r 80
        fi
      fi
      echo Audio: $audio
	  
	  subtitles=$filename.ass
	  if [ ! -f "$subtitles" ]; then
		  ./rec_ass_for_card.py "$filename" > "$subtitles"
	  fi

	  duration=`ffprobe -i "$audio" -show_format -v quiet | sed -n 's/duration=//p'`
    #echo duration raw = $duration
	  duration=`echo "($duration + 3.5) / 1" | bc`
	  echo duration = $duration
	  
	  
      if [[ "$audio" == "$mp3" ]]; then
        ffmpeg -loop 1 -y -i "$fullfile" -i "$audio" -c:v libx264 -preset veryslow -tune stillimage -vf scale=-2:720,subtitles="$subtitles" -pix_fmt yuv420p -c:a copy -t $duration "$destfile"
      else
        ffmpeg -loop 1 -y -i "$fullfile" -i "$audio" -c:v libx264 -preset veryslow -tune stillimage -vf scale=-2:720,subtitles="$subtitles" -pix_fmt yuv420p -c:a aac -b:a 128k -filter:a "volume=2.0,atempo=1.2,adelay=1s" -t $duration "$destfile"
        echo ffmpeg -loop 1 -y -i "$fullfile" -i "$audio" -c:v libx264 -preset veryslow -tune stillimage -vf scale=-2:720,subtitles="$subtitles" -pix_fmt yuv420p -c:a aac -b:a 128k -filter:a "volume=2.0,atempo=1.2,adelay=1s" -t $duration "$destfile"
      fi
    fi
done
