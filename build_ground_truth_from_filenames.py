"""
build_ground_truth_from_filenames.py
สแกนโฟลเดอร์ภาพ แล้วดึงเลข อย. ที่ถูกต้องจากชื่อไฟล์โดยตรง
ใช้ได้เฉพาะไฟล์ที่ตั้งชื่อตรงกับรูปแบบเลข อย. (เช่น 13-1-02954-5-0218.jpg)
ภาพที่ชื่อไฟล์ไม่ตรงรูปแบบ (เช่น 1.jpg, 1_2.JPG) จะถูกข้าม ต้องกรอกเองทีหลัง

วิธีใช้:
    python build_ground_truth_from_filenames.py --dir train_images --output ground_truth_train.csv
"""

import argparse
import csv
import re
from pathlib import Path

IMG_EXTS = (".jpg", ".jpeg", ".png")
# รูปแบบเลข อย.: XX-X-XXXXX-X-XXXX (2-1-5-1-4 หลัก)
OY_PATTERN = re.compile(r"^\d{2}-\d-\d{5}-\d-\d{4}$")


def main(image_dir: str, output_csv: str):
    image_files = [
        p for p in Path(image_dir).iterdir() if p.suffix.lower() in IMG_EXTS
    ]

    matched = []
    unmatched = []

    for p in image_files:
        stem = p.stem  # ชื่อไฟล์ไม่รวมนามสกุล
        if OY_PATTERN.match(stem):
            matched.append((p.name, stem))
        else:
            unmatched.append(p.name)

    with open(output_csv, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(["image_name", "correct_number"])
        for name, number in matched:
            writer.writerow([name, number])

    print(f"เจอภาพทั้งหมด {len(image_files)} ไฟล์")
    print(f"ดึง ground truth จากชื่อไฟล์ได้อัตโนมัติ: {len(matched)} ภาพ -> บันทึกที่ {output_csv}")
    print(f"ภาพที่ชื่อไฟล์ไม่ตรงรูปแบบ (ยังไม่มี ground truth): {len(unmatched)} ภาพ")
    if unmatched:
        print("ตัวอย่างภาพที่ต้องกรอกเอง (ถ้าต้องการเพิ่ม):", unmatched[:10])


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--dir", required=True, help="โฟลเดอร์ภาพ (เช่น train_images)")
    parser.add_argument("--output", default="ground_truth_train.csv")
    args = parser.parse_args()
    main(args.dir, args.output)
