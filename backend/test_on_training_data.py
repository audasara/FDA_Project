"""
test_on_training_data.py
ทดสอบ YOLO11s กับภาพชุดที่ใช้เทรนจริง (ที่อาจารย์ให้มาเพิ่ม)
เพื่อเช็คว่าโมเดล detect ภาพในโดเมนที่มันคุ้นเคยได้ดีแค่ไหน
แล้วเทียบกับผลที่ได้จากภาพที่ถ่ายเอง (test/) ก่อนหน้านี้

วิธีใช้:
    วางโฟลเดอร์ภาพที่ใช้เทรน เช่น train_images/ ไว้ในโปรเจกต์
    python test_on_training_data.py --dir train_images
"""

import argparse
from pathlib import Path

import cv2
from ultralytics import YOLO

MODEL_PATH = "test/yolo11s.pt"
IMG_EXTS = (".jpg", ".jpeg", ".png")


def main(image_dir: str, conf: float, imgsz: int, output_dir: str):
    Path(output_dir).mkdir(exist_ok=True)
    model = YOLO(MODEL_PATH)

    image_files = [
        p for p in Path(image_dir).rglob("*") if p.suffix.lower() in IMG_EXTS
    ]
    print(f"เจอภาพในชุดเทรน {len(image_files)} ไฟล์ | conf={conf} imgsz={imgsz}\n")

    # เช็คคร่าวๆ ว่ามีไฟล์ label (.txt) คู่ภาพไหม (บ่งบอกว่าเป็น YOLO labeled data)
    sample = image_files[:5]
    has_labels = any(p.with_suffix(".txt").exists() for p in sample)
    print(f"มีไฟล์ label (.txt) คู่ภาพไหม: {'มี' if has_labels else 'ไม่มี หรือไม่แน่ใจ'}")
    if has_labels:
        print("-> ถ้ามี label จริง แนะนำให้รัน model.val() แยกต่างหากเพื่อได้ mAP อย่างเป็นทางการ\n")
    else:
        print()

    found = 0
    confidences = []

    for img_path in image_files:
        results = model.predict(str(img_path), conf=conf, imgsz=imgsz, verbose=False)
        boxes = results[0].boxes
        img = cv2.imread(str(img_path))
        if img is None:
            print(f"  {img_path.name:25s} -> อ่านภาพไม่ได้ ข้าม")
            continue

        if len(boxes) > 0:
            found += 1
            max_c = float(boxes.conf.max())
            confidences.append(max_c)
            for box in boxes:
                x1, y1, x2, y2 = map(int, box.xyxy[0])
                c = float(box.conf[0])
                cv2.rectangle(img, (x1, y1), (x2, y2), (0, 255, 0), 3)
                cv2.putText(
                    img, f"{c:.2f}", (x1, max(y1 - 10, 0)),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 255, 0), 2,
                )
            status = f"เจอ {len(boxes)} กรอบ (max conf {max_c:.2f})"
        else:
            status = "ไม่เจอ"

        out_path = Path(output_dir) / f"vis_{img_path.stem}.jpg"
        cv2.imwrite(str(out_path), img)
        print(f"  {img_path.name:25s} -> {status}")

    rate = found / len(image_files) * 100 if image_files else 0
    avg_conf = sum(confidences) / len(confidences) if confidences else 0
    print(f"\n===== สรุป =====")
    print(f"เจอ {found}/{len(image_files)} ภาพ ({rate:.1f}%)")
    if confidences:
        print(f"confidence เฉลี่ยตอนเจอ: {avg_conf:.2f}")
    print(f"\nเทียบกับภาพที่ถ่ายเอง (test/) ที่เคยได้ 4/9 (44%)")
    print("ถ้า % ตรงนี้สูงกว่ามาก -> ยืนยันว่าปัญหาคือ 'ภาพที่ถ่ายเองต่างจากที่เทรนมา' ไม่ใช่โมเดลพัง")
    print("ถ้า % ตรงนี้ก็ต่ำใกล้เคียงกัน -> ปัญหาน่าจะอยู่ที่ตัวโมเดลเอง (เทรนไม่พอ/ไม่นิ่ง)")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--dir", required=True, help="โฟลเดอร์ภาพที่ใช้เทรน")
    parser.add_argument("--conf", type=float, default=0.25)
    parser.add_argument("--imgsz", type=int, default=640)
    parser.add_argument("--output", default="vis_train_output")
    args = parser.parse_args()
    main(args.dir, args.conf, args.imgsz, args.output)