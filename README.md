# JustView

Just view for videos.

Without any unsafe functionality and extremally simple. You can't delete/edit/share files in this app. And no any annoying ads.

Even toddlers can use it safely.


## Usage

Choose first video file

Swipe to play next/prev file

If current file finished next file will be played automatically

Media controls show on long click: pause, rewind

## Developed and tested for

Lenovo TAB 2 A10-70F

[Android 6.0 (API level 23)](https://developer.android.com/studio/releases/platforms#6.0)


## Installation

### Installation from Desktop

Enable USB debug

```
adb devices -l
adb -s 0123456789ABCDEF install JustView-v1.0.0.apk 
```


### Installation from Android device

Enable unsigned apps installation

```
Settings -> Application -> Unknown sources (Allow installation of non-Market applications)
```

then copy APK to device and install


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
ffmpeg -i "%1" -c:v libx264 -preset veryslow -c:a aac "%1.x264.mp4"
```
