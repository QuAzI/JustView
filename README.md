# JustView

Just view for videos.

Without any unsafe functionality and extremally simple. You can't delete/edit/share files in this app. And no any annoying ads.

Even toddlers can use it safely.

Also it is good hint to download some videos even from YouTube to local cache because now official client regularly freezes on old tablets like Lenovo TAB 2 A10-70F 


## Usage

Choose first video file

Swipe to play next/prev file

If current file finished next file will be played automatically

Media controls show on long click: pause, rewind


## Developed and tested for

Lenovo Phab Plus PB1-770M 32GB LTE [Android 5.1.1 (API Level 22) LOLLIPOP_MR1](https://developer.android.com/studio/releases/platforms#5.1)

Lenovo TAB 2 A10-70F [Android 6.0 (API level 23)](https://developer.android.com/studio/releases/platforms#6.0)

Lenovo Tab M10 HD 2nd Gen TB-X306X - [Android 11 (API level 30)](https://developer.android.com/studio/releases/platforms#11)

Samsung Note10+ - [Android 12 (API level 31)](https://developer.android.com/studio/releases/platforms#12)


## Installation

To enable unsigned apps installation please allow installs from Unknown Sources (Security section)

```
Settings -> Application -> Unknown sources (Allow installation of non-Market applications)
```

### Installation from Desktop

Enable USB debug

```
adb devices -l
adb -s 0123456789ABCDEF install -r ./app/build/intermediates/apk/debug/app-debug.apk
```


### Installation from Android device

Download APK to device and use file manager to install unsigned APK right from filesystem


## Troubleshooting

Standard VideoView used. Codesc and all decoding issues depends on device.

Please check https://developer.android.com/guide/topics/media/media-formats

Preffered extensions: .mp4, .webm, .mkv

Preffered formats:
- h264 = MPEG-4 Part 10 or AVC - Android 3.0+
- h265 = HEVC - Android 5.0+
- vp9 - Android 4.4+ - I had some troubles with it on my Lenovo TAB 2

Looks optimal for me: res:1920,fps codec:h264

Encode to h264 AVC
```
ffmpeg -loop 1 -y -i "$fullfile" -itsoffset 1 -i "$audio" -map 0:v -map 1:a -c:v libx264 -preset veryslow -tune stillimage -vf scale=-2:720 -pix_fmt yuv420p -c:a aac -b:a 128k -filter:a "volume=2.0,atempo=1.2" -t 5 "$destfile"
```
where
- `-vf scale=-2:720` fix images scale error
- `-pix_fmt yuv420p` fix issues with some video files for old Androids


## Known issues

No preview for directories on new Android versions. Only for files. It is not very comfortable for toddlers while they can't read.


# Tools for toddlers

Please check `tools/VideoCardsGenerator` to prepare some video lessons for toddlers
