"""
batch_visualize.py
รัน YOLO11s กับทุกภาพในโฟลเดอร์ test ทีเดียว วาดกรอบ + บันทึกไว้ดูทีเดียว
ไม่ต้องรันทีละภาพแบบ Tesyolo.py

วิธีใช้:
    python batch_visualize.py
    python batch_visualize.py --conf 0.15   (เปลี่ยน threshold ได้)
"""

import argparse
from pathlib import Path

import cv2
from ultralytics import YOLO

TEST_DIR = "test"
MODEL_PATH = "test/yolo11s.pt"
OUTPUT_DIR = "vis_output"
IMG_EXTS = (".jpg", ".jpeg", ".png")


def main(conf_threshold: float, imgsz: int):
    Path(OUTPUT_DIR).mkdir(exist_ok=True)

    model = YOLO(MODEL_PATH)
    image_files = [
        p for p in Path(TEST_DIR).iterdir() if p.suffix.lower() in IMG_EXTS
    ]

    print(f"เจอภาพทั้งหมด {len(image_files)} ไฟล์ | conf={conf_threshold} | imgsz={imgsz}\n")

    found_count = 0
    not_found_count = 0

    for img_path in image_files:
        results = model.predict(
            str(img_path), conf=conf_threshold, imgsz=imgsz, verbose=False
        )
        boxes = results[0].boxes
        img = cv2.imread(str(img_path))

        if len(boxes) == 0:
            not_found_count += 1
            status = "ไม่เจอ"
        else:
            found_count += 1
            status = f"เจอ {len(boxes)} กรอบ"
            for box in boxes:
                x1, y1, x2, y2 = map(int, box.xyxy[0])
                conf = float(box.conf[0])
                cv2.rectangle(img, (x1, y1), (x2, y2), (0, 255, 0), 3)
                cv2.putText(
                    img, f"{conf:.2f}", (x1, max(y1 - 10, 0)),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 255, 0), 2,
                )

        out_path = Path(OUTPUT_DIR) / f"vis_{img_path.stem}_sz{imgsz}.jpg"
        cv2.imwrite(str(out_path), img)
        print(f"  {img_path.name:20s} -> {status}  (บันทึกที่ {out_path})")

    print(f"\nสรุป: เจอ {found_count} ภาพ / ไม่เจอ {not_found_count} ภาพ จากทั้งหมด {len(image_files)} ภาพ")
    print(f"เปิดโฟลเดอร์ '{OUTPUT_DIR}' เพื่อดูภาพที่วาดกรอบแล้วทั้งหมดได้เลย")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--conf", type=float, default=0.25, help="confidence threshold")
    parser.add_argument("--imgsz", type=int, default=640, help="ขนาดภาพก่อนเข้าโมเดล (640 หรือ 1280)")
    args = parser.parse_args()
    main(args.conf, args.imgsz)