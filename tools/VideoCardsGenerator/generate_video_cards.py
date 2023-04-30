#!/usr/bin/env python3
import os
import sys
import subprocess
from gtts import gTTS

def generate_audio(filename):
    mp3 = f"{filename}.mp3"
    audio = mp3
    if not os.path.exists(audio):
        wav = f"{filename}.wav"
        if not os.path.exists(wav):
            print(f"Audio generation: {wav}")
            # espeak-ng
            # subprocess.run(["espeak-ng", "-v", "ru", "-s", "100", "-w", wav, filename], check=True)

            # festival
            # subprocess.run(["text2wave", "-eval", "(voice_msu_ru_nsh_clunits)", "-o", wav], input=filename, text=True, check=True)

            # gTTS
            # subprocess.run(["/usr/local/bin/gtts-cli", filename, "--output", wav, "-s", "-l", "ru", "--nocheck"], check=True)
            tts = gTTS(filename)
            tts.save(wav)

            # RHVoice
            # subprocess.run(["RHVoice-test", "-p", "pavel", "-o", wav, "-r", "80"], input=filename, text=True, check=True)
        audio = wav
    return audio

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

    args = [
        "ffmpeg", "-y", "-i", fullfile, "-i", audio,
        "-c:v", "libx264", "-preset", "veryslow", "-tune", "stillimage",
        "-vf", f"scale=-2:720,subtitles={subtitles}",
        "-pix_fmt", "yuv420p", "-t", str(duration)
    ]

    if audio.endswith(".mp3"):
        args += ["-c:a", "copy"]
    else:
        args += ["-c:a", "aac", "-b:a", "128k", "-filter:a", "volume=2.0,atempo=1.2,adelay=1s"]

    args += [destfile]

    print(" ".join(args))
    subprocess.run(args, check=True)

if __name__ == "__main__":
    os.makedirs("./out", exist_ok=True)
    image_extensions = (".jpg", ".jpeg", ".png", ".webp", ".avif")
    for fullfile in os.listdir("."):
        if not fullfile.endswith(image_extensions):
            continue
        print(f"Image: {fullfile}")

        filename, _ = os.path.splitext(fullfile)
        audio = generate_audio(filename)
        print(f"Audio: {audio}")

        subtitles = generate_subtitles(filename)

        ffprobe_output = subprocess.check_output(["ffprobe", "-i", audio, "-show_format", "-v", "quiet"])
        duration_raw = [x for x in ffprobe_output.decode('utf-8').split('\r\n') if x.startswith('duration=')]

        duration = float(duration_raw[0].replace('duration=', '')) if len(duration_raw) > 0 else 0
        duration += 3.5
        duration = float(duration // 1)
        print(f"Duration: {duration}")

        destfile = f"./out/{filename}.mp4"
        generate_video(fullfile, duration, audio, subtitles, destfile)
