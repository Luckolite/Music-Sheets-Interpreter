# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
"""Standard MIDI preview writer; no synthesizer, sound bank or external dependency."""
import math
import struct
from pathlib import Path


def variable_length(value):
    if not 0 <= value <= 0x0FFFFFFF:
        raise ValueError("MIDI delta time exceeds four bytes")
    out = [value & 127]
    value >>= 7
    while value:
        out.insert(0, (value & 127) | 128)
        value >>= 7
    return bytes(out)


def write_midi(document, path, bpm=120):
    if not math.isfinite(bpm) or not 15 <= bpm <= 400:
        raise ValueError("Initial BPM must be 15..400 quarter notes per minute")
    ppq = 480
    tempo = round(60_000_000 / bpm)
    events = [(0, 0, b"\xff\x51\x03" + tempo.to_bytes(3, "big"))]
    offset = 0.0
    tones = []
    previous = {}
    for page in document["pages"]:
        starts = [0.0]
        for beats in page["measureBeats"]:
            starts.append(starts[-1] + beats)
        for change in page["score"]["tempoChanges"]:
            bar = change["measureIndex"]
            beat = offset + starts[bar] + page["measureBeats"][bar] * change["positionInMeasure"]
            micros = round(60_000_000 / change["bpm"])
            events.append((round(beat * ppq), 0, b"\xff\x51\x03" + micros.to_bytes(3, "big")))
        for note in sorted(page["events"], key=lambda n: n["startBeat"]):
            pitch = note["midi"]
            if not 0 <= pitch <= 127:
                continue
            start = max(0, round((offset + note["startBeat"]) * ppq))
            end = max(start + 1, round((offset + note["startBeat"] + note["durationBeats"]) * ppq))
            lane = (note["staffCount"], note["staffIndex"], pitch)
            previous_tone = previous.get(lane)
            if note["tiedFromPrevious"] and previous_tone is not None and abs(previous_tone[1] - start) <= ppq // 8:
                previous_tone[1] = max(previous_tone[1], end)
            else:
                tone = [start, end, pitch]
                tones.append(tone)
                previous[lane] = tone
        offset += page["totalBeats"]
    active = {}
    for start, end, pitch in sorted(tones):
        # Simultaneous same-pitch voices need separate channels: one note-off must not
        # cut off another held voice. Other pitches can safely share those channels.
        channels = active.setdefault(pitch, {})
        channel = next((c for c in range(16) if c != 9 and channels.get(c, -1) <= start), None)
        if channel is None:
            raise ValueError("MIDI cannot represent more than 15 simultaneous voices of one pitch")
        channels[channel] = end
        events.extend([(start, 2, bytes([0x90 | channel, pitch, 80])),
                       (end, 1, bytes([0x80 | channel, pitch, 0]))])
    track = bytearray()
    last = 0
    for tick, _, message in sorted(events, key=lambda x: (x[0], x[1])):
        track.extend(variable_length(tick - last))
        track.extend(message)
        last = tick
    track.extend(b"\x00\xff\x2f\x00")
    Path(path).write_bytes(b"MThd" + struct.pack(">IHHH", 6, 0, 1, ppq) + b"MTrk" + struct.pack(">I", len(track)) + track)
