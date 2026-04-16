import os
from tkinter import Tk, filedialog
from pydub import AudioSegment

def convert_wav_to_ogg(folder_path):
    for root, dirs, files in os.walk(folder_path):
        for file in files:
            if file.lower().endswith(".wav"):
                wav_path = os.path.join(root, file)
                ogg_path = os.path.splitext(wav_path)[0] + ".ogg"

                try:
                    print(f"Converting: {wav_path}")

                    audio = AudioSegment.from_wav(wav_path)
                    audio.export(ogg_path, format="ogg")

                    os.remove(wav_path)
                    print(f"✔ Converted & deleted: {file}")

                except Exception as e:
                    print(f"✖ Failed: {file} | Error: {e}")

def select_folder():
    Tk().withdraw()
    folder = filedialog.askdirectory(title="Select your sounds folder")
    return folder

if __name__ == "__main__":
    folder_path = select_folder()

    if folder_path:
        print(f"Selected folder: {folder_path}")
        convert_wav_to_ogg(folder_path)
        print("Done.")
    else:
        print("No folder selected.")