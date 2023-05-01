#!/usr/bin/env python3

import sys
from string import Template
import pyphen

template = Template("""[Script Info]
Title: ${filename}
ScriptType: v4.00+
WrapStyle: 0
ScaledBorderAndShadow: yes
YCbCr Matrix: PC.601
PlayResX: 720
PlayResY: 720

[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
Style: Default,DejaVu Sans Mono,96,&H00FFFFFF,&H000000FF,&H00000000,&H00000000,-1,0,0,0,100,100,0,0,1,1,0,2,10,10,10,1

[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
Dialogue: 0,0:00:01.00,0:00:05.00,Default,,0,0,0,,${subtitle}
""")


def split_words_into_syllables(sentence):
    dic = pyphen.Pyphen(lang='ru')  # выбираем русский язык
    words = sentence.split()  # разбиваем предложение на слова
    result = []
    for word in words:
        syllables = dic.inserted(word, hyphen='{\\fscx25}\u202f{\\r}')  # получаем список слогов для каждого слова
        result.append(syllables)
    return ' '.join(result)


def colorify(text: str) -> str:
    RED = '{\c&H0000FF&}'
    BLUE = '{\c&HFF0000&}'
    GRAY = '{\c&H7F7F7F&}'
    subs = ""
    mode = ""
    for ch in text:
        if ch in ' ':
            pass
        elif ch.capitalize() in 'АЕЁИОУЫЭЮЯ':
            if mode != RED:
                mode = RED
                subs += mode
            
        elif ch.capitalize() in 'ЙЦКНГШЩЗХФВПРЛДЖЧСМТБ':
            if mode != BLUE:
                mode = BLUE
                subs += mode
        else:
            if mode != GRAY:
                mode = GRAY
                subs += mode

        subs += ch

    return subs

if __name__ == "__main__":
    text = sys.argv[1]
    d = {
        'filename': text,
        'subtitle': colorify(split_words_into_syllables(text))
    }
    print(template.safe_substitute(d))