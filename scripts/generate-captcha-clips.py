"""Regenerate bundled Spanish WAV clips with the local espeak-ng executable."""
from pathlib import Path
import subprocess
import wave

NAMES = dict(zip("ABCDEFGHJKLMNPQRSTUVWXYZ23456789", [
    "a", "be", "ce", "de", "e", "efe", "ge", "hache", "jota", "ka", "ele",
    "eme", "ene", "pe", "cu", "erre", "ese", "te", "u", "uve", "doble uve",
    "equis", "i griega", "zeta", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve",
]))

if __name__ == "__main__":
    destination = Path(__file__).resolve().parents[1] / "backend/src/main/resources/captcha-audio"
    destination.mkdir(parents=True, exist_ok=True)
    for character, name in NAMES.items():
        path = destination / f"{character}.wav"
        subprocess.run(["espeak-ng", "-v", "es", "-s", "145", "-w", str(path),
                        ("número " if character.isdigit() else "letra ") + name], check=True)
        with wave.open(str(path), "rb") as source:
            assert (source.getnchannels(), source.getsampwidth(), source.getframerate()) == (1, 2, 22050)
    print(f"Validated {len(NAMES)} Spanish PCM clips")
