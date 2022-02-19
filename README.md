# JustView

Just view for videos.

Without any unsafe functionality and extremally simple. You can't delete/edit/share files in this app. And no any annoying ads.

Even toddlers can use it safely.


## Usage

Choose first video file

Swipe to play next/prev file

If current file finished next file will be played automatically


## Developed and tested for

Lenovo TAB 2 A10-70F

[Android 6.0 (API level 23)](https://developer.android.com/studio/releases/platforms#6.0)


## Installation

### from Desktop

Enable USB debug

```
adb devices -l
adb -s 0123456789ABCDEF install JustView-v1.0.0.apk 
```


### from Android device

Enable unsigned apps installation

```
Settings -> Application -> Unknown sources (Allow installation of non-Market applications)
```

then copy APK to device and install


## Troubleshooting

Standard VideoView used. Codesc and all decoding issues depends on device.
Please check https://developer.android.com/guide/topics/media/media-formats
