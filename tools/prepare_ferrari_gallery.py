"""Generate delivery WebPs from supplied PNGs; never write or rename originals.
Run: python tools/prepare_ferrari_gallery.py (requires Pillow; not a runtime dependency).
"""
import hashlib
import json
from pathlib import Path
from PIL import Image, ImageOps

ROOT = Path(__file__).resolve().parents[1]
STATIC = ROOT / "src/main/resources/static"
SOURCE = STATIC / "assets/cars/ferrari-sf90-stradale/ferrari_sf90_20_views"
manifest = json.loads((ROOT / "src/main/resources/galleries/ferrari-sf90-stradale.json").read_text(encoding="utf-8"))
files = sorted(SOURCE.glob("*.png"))
assert {p.name for p in files} == {i["originalFilename"] for i in manifest["images"]}
before = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in files}
pixels = {}
total_full = total_thumb = 0
for item in manifest["images"]:
    source = SOURCE / item["originalFilename"]
    with Image.open(source) as original:
        photo = ImageOps.exif_transpose(original).convert("RGB")
        assert photo.size == (item["width"], item["height"]), source
        pixels[source.name] = hashlib.sha256(photo.tobytes()).hexdigest()
        destination = STATIC / item["imageUrl"].lstrip("/")
        destination.parent.mkdir(parents=True, exist_ok=True)
        photo.save(destination, "WEBP", quality=88, method=6)
        total_full += destination.stat().st_size
        thumb = destination.parent / "thumbnails" / destination.name
        thumb.parent.mkdir(parents=True, exist_ok=True)
        photo.thumbnail((360, 240), Image.Resampling.LANCZOS)
        photo.save(thumb, "WEBP", quality=78, method=6)
        total_thumb += thumb.stat().st_size
assert before == {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in files}
print(json.dumps({"images": len(files), "sourceBytes": sum(p.stat().st_size for p in files),
                  "galleryBytes": total_full, "thumbnailBytes": total_thumb,
                  "uniqueFileHashes": len(set(before.values())), "uniquePixelHashes": len(set(pixels.values())),
                  "originalsUnchanged": True}, indent=2))
