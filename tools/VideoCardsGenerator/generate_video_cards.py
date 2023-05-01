#!/usr/bin/env python3
import os
import sys
import subprocess
from gtts import gTTS

def generate_audio(filename):    
    wav = f"{filename}.wav"
    mp3 = f"{filename}.mp3"
    audio = wav
    if os.path.exists(mp3):
        audio = mp3

    print(f"Audio: {audio}")    
    if not os.path.exists(audio):
        print(f"Audio generation: {audio}")
        # espeak-ng
        # subprocess.run(["espeak-ng", "-v", "ru", "-s", "100", "-w", wav, filename], check=True)

        # festival
        # subprocess.run(["text2wave", "-eval", "(voice_msu_ru_nsh_clunits)", "-o", wav], input=filename, text=True, check=True)

        # gTTS
        # subprocess.run(["/usr/local/bin/gtts-cli", filename, "--output", wav, "-s", "-l", "ru", "--nocheck"], check=True)
        tts = gTTS(filename, lang='ru')
        tts.save(audio)

        # RHVoice
        # subprocess.run(["RHVoice-test", "-p", "pavel", "-o", wav, "-r", "80"], input=filename, text=True, check=True)
    return audio

def get_duration(video_file: str, audio_file: str) -> float:
    try:
        ffprobe_output = subprocess.check_output(["ffprobe", "-i", audio_file, "-show_format", "-v", "quiet"])
        duration_raw = [x for x in ffprobe_output.decode('utf-8').split('\r\n') if x.startswith('duration=')]

        duration = float(duration_raw[0].replace('duration=', '')) if len(duration_raw) > 0 else 0
        duration += 3.5
        duration = float(duration // 1)
        print(f"Duration: {duration}")
        return duration
    except:
        print(f"Duration unknown")
    
    return 4

def generate_subtitles(filename):
    subtitles = f"{filename}.ass"
    if not os.path.exists(subtitles) or os.stat(subtitles).st_size == 0:
        # rec_ass_for_card.py
        subprocess.run([sys.executable, "./rec_ass_for_card.py", filename], 
                       stdout=open(subtitles, "w"), 
                       check=True)
    return subtitles

def generate_video(fullfile, duration, audio, subtitles, destfile):
    if os.path.exists(destfile):
        return
    
    filename, ext = os.path.splitext(fullfile.lower())

    args = ["ffmpeg"]
    
    encoder = "libx264"

    if ext.endswith('.avif'):
        encoder = "libaom-av1"
    elif fullfile.lower().endswith('.gif'):
        pass
    else:
        args += ["-loop", "1"]
        
    args += ["-y", "-i", fullfile, "-i", audio,
        "-c:v", encoder, "-preset", "veryslow"]
    
    if not ext.endswith('.avif'):
        args += ["-tune", "stillimage"]

    args += [ "-vf", f'scale=-2:720,subtitles={subtitles}',
        "-pix_fmt", "yuv420p", "-t", str(duration)]

    if audio.endswith(".mp3"):
        args += ["-c:a", "copy"]
    else:
        args += ["-c:a", "aac", "-b:a", "128k", "-filter:a", "volume=2.0,atempo=1.2,adelay=1s"]

    args += [destfile]

    print(" ".join(args))
    subprocess.run(args, check=True)

def convert_file(fullfile: str):
    print(f"Source: {fullfile}")
    filename, _ = os.path.splitext(fullfile)
    audio = generate_audio(filename)
    duration = get_duration(fullfile, audio)
    subtitles = generate_subtitles(filename)    
    destfile = f"./out/{filename}.mp4"
    generate_video(fullfile, duration, audio, subtitles, destfile)


if __name__ == "__main__":
    os.makedirs("./out", exist_ok=True)
    supported_extensions = (
        ".jpg", ".jpeg", ".png", ".webp", ".tif", ".avif", ".gif", ".heif",
        ".mp4", ".mkv", "3gpp"
        )
    for fullfile in os.listdir("."):
        if fullfile.lower().endswith(supported_extensions):
            convert_file(fullfile)

        
