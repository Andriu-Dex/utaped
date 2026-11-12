"""Regenerate bundled Spanish WAV clips for UTAPED CAPTCHA audio.

Uses edge-tts with the natural Ecuadorian Spanish voice (es-EC-AndreaNeural),
trimmed of silence and normalized to 22050 Hz 16-bit mono PCM.
"""
import asyncio
from pathlib import Path
import subprocess
import wave
import edge_tts
import numpy as np

# 32 characters in CaptchaService.ALPHABET (excluding ambiguous 0, 1, I, O)
PRONUNCIATIONS = {
    '2': 'dos',
    '3': 'tres',
    '4': 'cuatro',
    '5': 'cinco',
    '6': 'seis',
    '7': 'siete',
    '8': 'ocho',
    '9': 'nueve',
    'A': 'A',
    'B': 'Be',
    'C': 'Ce',
    'D': 'De',
    'E': 'E',
    'F': 'Efe',
    'G': 'Ge',
    'H': 'Hache',
    'J': 'Jota',
    'K': 'Ka',
    'L': 'Ele',
    'M': 'Eme',
    'N': 'Ene',
    'P': 'Pe',
    'Q': 'Cu',
    'R': 'Erre',
    'S': 'Ese',
    'T': 'Te',
    'U': 'U',
    'V': 'Ve',
    'W': 'Doble ve',
    'X': 'Equis',
    'Y': 'Y griega',
    'Z': 'Zeta'
}

VOICE = "es-EC-AndreaNeural"

def trim_and_fade(pcm_data: np.ndarray, sample_rate: int = 22050, thresh: int = 300, pad_ms: int = 40) -> np.ndarray:
    chunk_size = int(sample_rate * 0.01)  # 10ms chunks
    energies = [np.max(np.abs(pcm_data[i:i + chunk_size])) for i in range(0, len(pcm_data), chunk_size)]
    active = [i for i, e in enumerate(energies) if e > thresh]
    if not active:
        return pcm_data

    pad_chunks = int(pad_ms / 10)
    start_chunk = max(0, active[0] - pad_chunks)
    end_chunk = min(len(energies), active[-1] + 1 + pad_chunks)

    start_sample = start_chunk * chunk_size
    end_sample = min(len(pcm_data), end_chunk * chunk_size)
    trimmed = pcm_data[start_sample:end_sample].copy()

    # 15ms smooth cosine fade in and fade out
    fade_len = min(int(sample_rate * 0.015), len(trimmed) // 4)
    if fade_len > 0:
        fade_in = 0.5 * (1 - np.cos(np.linspace(0, np.pi, fade_len)))
        fade_out = 0.5 * (1 + np.cos(np.linspace(0, np.pi, fade_len)))
        trimmed[:fade_len] = (trimmed[:fade_len] * fade_in).astype(np.int16)
        trimmed[-fade_len:] = (trimmed[-fade_len:] * fade_out).astype(np.int16)

    return trimmed

async def generate_clips(destination: Path):
    destination.mkdir(parents=True, exist_ok=True)
    temp_dir = destination / ".temp_build"
    temp_dir.mkdir(parents=True, exist_ok=True)

    try:
        for char, spoken in PRONUNCIATIONS.items():
            mp3_path = temp_dir / f"{char}.mp3"
            wav_path = destination / f"{char}.wav"

            comm = edge_tts.Communicate(spoken, VOICE, rate="+4%")
            await comm.save(str(mp3_path))

            # Decode MP3 to raw 22050 Hz 16-bit mono PCM via ffmpeg
            cmd = ["ffmpeg", "-y", "-i", str(mp3_path), "-ar", "22050", "-ac", "1", "-f", "s16le", "-"]
            raw_bytes = subprocess.check_output(cmd, stderr=subprocess.DEVNULL)
            pcm_samples = np.frombuffer(raw_bytes, dtype=np.int16)

            cleaned_samples = trim_and_fade(pcm_samples, sample_rate=22050)

            with wave.open(str(wav_path), "wb") as output_wave:
                output_wave.setnchannels(1)
                output_wave.setsampwidth(2)
                output_wave.setframerate(22050)
                output_wave.writeframes(cleaned_samples.tobytes())

            print(f"Generated {char}.wav ('{spoken}'): {len(cleaned_samples)/22050:.2f}s ({len(cleaned_samples)} samples)")

    finally:
        # Clean temporary MP3s
        if temp_dir.exists():
            for f in temp_dir.glob("*.mp3"):
                f.unlink(missing_ok=True)
            temp_dir.rmdir()

if __name__ == "__main__":
    dest = Path(__file__).resolve().parents[1] / "backend/src/main/resources/captcha-audio"
    asyncio.run(generate_clips(dest))
    print(f"Successfully generated and validated {len(PRONUNCIATIONS)} audio clips in {dest}")
