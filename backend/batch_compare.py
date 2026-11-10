#batchcompare

import sys
import csv
import time
from pathlib import Path

import cv2
from ultralytics import YOLO

from preprocess_fda import smart_detect

sys.stdout.reconfigure(encoding="utf-8")      # กัน cp1252 พังเวลาเจอ path ภาษาไทย

MODEL_PATH = "Yolo11s.pt"
OUT_DIR = Path(r"D:\FDA_result")
CONF = 0.25

src = Path(sys.argv[1] if len(sys.argv) > 1 else r"D:\FDA\FDA01")
only_back = "--all" not in sys.argv          # default: เฉพาะไฟล์ _2 (ด้านหลัง มีเลข อย.)

files = sorted(p for p in src.rglob("*")
               if p.suffix.lower() in (".jpg", ".jpeg", ".png")
               and (not only_back or p.stem.endswith("_2")))

if not files:
    print(f"ไม่พบไฟล์ใน {src}")
    sys.exit(1)

OUT_DIR.mkdir(parents=True, exist_ok=True)
fail_dir = OUT_DIR / "failed"
fail_dir.mkdir(exist_ok=True)

model = YOLO(MODEL_PATH)
rows = []
old_hit = new_hit = 0
t_start = time.time()

for i, p in enumerate(files, 1):
    img = cv2.imread(str(p))
    if img is None:
        continue

    # --- วิธีเดิม: ยิงเข้าโมเดลตรง ๆ
    r0 = model.predict(img, conf=CONF, imgsz=640, verbose=False)[0]
    c0 = float(r0.boxes.conf.max()) if len(r0.boxes) else 0.0

    # --- วิธีใหม่: ปรับภาพก่อน
    r1 = smart_detect(model, img, conf=CONF)
    c1 = r1["conf"]

    old_hit += c0 >= CONF
    new_hit += r1["found"]
    rows.append([p.name, round(c0, 3), round(c1, 3), r1["angle"], r1["stage"]])

    if not r1["found"]:
        cv2.imwrite(str(fail_dir / p.name), img)

    if i % 25 == 0 or i == len(files):
        print(f"  {i}/{len(files)}  เดิม {old_hit}  ใหม่ {new_hit}")

n = len(rows)
with open(OUT_DIR / "compare.csv", "w", newline="", encoding="utf-8-sig") as f:
    w = csv.writer(f)
    w.writerow(["file", "conf_old", "conf_new", "angle", "stage"])
    w.writerows(rows)

fixed = sum(1 for r in rows if r[1] < CONF <= r[2])
broke = sum(1 for r in rows if r[2] < CONF <= r[1])

print("\n" + "=" * 52)
print(f"ภาพทั้งหมด        {n}")
print(f"วิธีเดิม           {old_hit:4d}  ({old_hit/n*100:5.1f}%)")
print(f"วิธีใหม่           {new_hit:4d}  ({new_hit/n*100:5.1f}%)")
print(f"กู้กลับมาได้       +{fixed}")
print(f"เสียไป             -{broke}")
print(f"เวลา              {time.time()-t_start:.0f}s  "
      f"({(time.time()-t_start)/n*1000:.0f} ms/ภาพ)")
print(f"\nCSV: {OUT_DIR/'compare.csv'}")
print(f"ภาพที่ยังพลาด: {fail_dir}")