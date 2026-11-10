"""
diagnose_yolo.py
วนเช็คทุกภาพในโฟลเดอร์ test กับโมเดลทั้ง 2 ตัว ที่ conf ต่ำมาก (0.01)
เพื่อดูว่าปัญหา 0 กรอบ เป็นเฉพาะบางภาพ หรือเป็นทุกภาพ/ทั้งโมเดล

วิธีใช้:
    python diagnose_yolo.py
"""

from pathlib import Path
from ultralytics import YOLO

TEST_DIR = "test"
MODELS = {
    "YOLO11s": "test/yolo11s.pt",
    "YOLOv8s": "test/yolov8s.pt",
}
IMG_EXTS = (".jpg", ".jpeg", ".png")

image_files = [
    p for p in Path(TEST_DIR).iterdir()
    if p.suffix.lower() in IMG_EXTS
]

if not image_files:
    print(f"ไม่เจอไฟล์ภาพในโฟลเดอร์ {TEST_DIR}")
else:
    print(f"เจอภาพทั้งหมด {len(image_files)} ไฟล์: {[p.name for p in image_files]}\n")

for model_label, model_path in MODELS.items():
    print(f"===== โมเดล: {model_label} ({model_path}) =====")
    model = YOLO(model_path)

    for img_path in image_files:
        results = model.predict(
            str(img_path), conf=0.01, imgsz=1280, verbose=False
        )
        boxes = results[0].boxes
        n = len(boxes)
        max_conf = float(boxes.conf.max()) if n > 0 else 0.0
        status = "เจอ" if n > 0 else "ไม่เจอ"
        print(f"  {img_path.name:20s} -> {status} ({n} กรอบ, max_conf={max_conf:.3f})")

    print()

print("สรุป: ถ้าทุกแถวขึ้น 'ไม่เจอ' ทั้ง 2 โมเดล แปลว่าไม่ใช่ปัญหาภาพใดภาพหนึ่ง")
print("ให้ลองขอภาพตัวอย่างที่อาจารย์ใช้ตอนเทรน มาทดสอบเทียบดูต่อไป")