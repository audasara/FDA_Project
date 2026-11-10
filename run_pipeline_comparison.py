"""
รัน pipeline A (ChatGPT OCR อย่างเดียว) และ pipeline B (YOLO11s -> crop -> ChatGPT OCR)
บนภาพทั้งหมดใน IMAGE_DIR แบบอัตโนมัติ พร้อมบันทึกผลลัพธ์และเวลาที่ใช้ลง CSV ทีละแถว

ก่อนรัน:
    pip install ultralytics openai opencv-python --break-system-packages
    ตั้งค่า OPENAI_API_KEY ใน .env หรือ export ก่อนรัน

โครงสร้างไฟล์ที่ต้องมี:
    ground_truth.csv   -> คอลัมน์: image_name, correct_number (กรอกให้ครบก่อนรัน)
    test/                -> โฟลเดอร์ภาพทดสอบ (.jpg, .jpeg, .JPG) + yolo11s.pt
"""

import base64
import csv
import os
import time
from pathlib import Path

import cv2
from dotenv import load_dotenv
from openai import OpenAI
from ultralytics import YOLO

load_dotenv()

# ---------------- CONFIG ----------------
IMAGE_DIR = "train_images"
GROUND_TRUTH_CSV = "ground_truth_train.csv"
OUTPUT_CSV = "results.csv"
YOLO_MODEL_PATH = "test/yolo11s.pt"
OPENAI_MODEL = "gpt-4o-mini"        # ให้ตรงกับที่ detect.py ใช้จริง
TEMP_CROP_PATH = "temp_crop.jpg"
IMG_EXTS = (".jpg", ".jpeg", ".png")

PROMPT = (
    "อ่านเลข อย. จากภาพนี้ ตอบเป็น JSON เท่านั้น ในรูปแบบ "
    '{"อย_number": "เลขที่อ่านได้ หรือ null ถ้าไม่เจอ"} '
    "ห้ามมีข้อความอื่นนอกเหนือจาก JSON"
)

client = OpenAI()
yolo_model = YOLO(YOLO_MODEL_PATH)


def encode_image(image_path: str) -> str:
    with open(image_path, "rb") as f:
        return base64.b64encode(f.read()).decode("utf-8")


def call_chatgpt_ocr(image_path: str) -> str:
    b64 = encode_image(image_path)
    response = client.chat.completions.create(
        model=OPENAI_MODEL,
        messages=[{
            "role": "user",
            "content": [
                {"type": "text", "text": PROMPT},
                {"type": "image_url", "image_url": {"url": f"data:image/jpeg;base64,{b64}"}},
            ],
        }],
        temperature=0,
        response_format={"type": "json_object"},
    )
    return response.choices[0].message.content


def pipeline_a(image_path: str):
    """Baseline: ส่งภาพเต็มเข้า ChatGPT OCR ตรงๆ"""
    start = time.perf_counter()
    result_json = call_chatgpt_ocr(image_path)
    elapsed = time.perf_counter() - start
    return result_json, elapsed


def pipeline_b(image_path: str):
    """YOLO11s หาตำแหน่งก่อน -> crop -> ค่อยส่งเข้า ChatGPT OCR"""
    start = time.perf_counter()

    results = yolo_model.predict(image_path, conf=0.25, verbose=False)
    boxes = results[0].boxes

    if len(boxes) == 0:
        elapsed = time.perf_counter() - start
        # fallback: YOLO หาไม่เจอ ใช้ภาพเต็มแทน (เหมือน detect.py จริง)
        result_json = call_chatgpt_ocr(image_path)
        elapsed = time.perf_counter() - start
        return result_json, elapsed, False

    best_box = boxes[boxes.conf.argmax()]
    x1, y1, x2, y2 = map(int, best_box.xyxy[0])

    img = cv2.imread(image_path)
    cropped = img[y1:y2, x1:x2]
    cv2.imwrite(TEMP_CROP_PATH, cropped)

    result_json = call_chatgpt_ocr(TEMP_CROP_PATH)
    elapsed = time.perf_counter() - start
    os.remove(TEMP_CROP_PATH)
    return result_json, elapsed, True


def load_ground_truth(path: str) -> dict:
    gt = {}
    with open(path, newline="", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            gt[row["image_name"]] = row["correct_number"]
    return gt


def main():
    ground_truth = load_ground_truth(GROUND_TRUTH_CSV)
    image_files = sorted(
        p for p in Path(IMAGE_DIR).iterdir() if p.suffix.lower() in IMG_EXTS
    )

    file_exists = os.path.exists(OUTPUT_CSV)
    with open(OUTPUT_CSV, "a", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        if not file_exists:
            writer.writerow([
                "image_name", "pipeline", "predicted_json",
                "ground_truth", "time_seconds", "yolo_detected",
            ])

        for img_path in image_files:
            name = img_path.name
            gt = ground_truth.get(name, "")

            if not gt:
                print(f"[{name}] ข้าม เพราะยังไม่มี ground truth ใน CSV")
                continue

            # Pipeline A (baseline)
            try:
                result_a, time_a = pipeline_a(str(img_path))
            except Exception as e:
                result_a, time_a = f"ERROR: {e}", -1
            writer.writerow([name, "A_baseline", result_a, gt, round(time_a, 3), ""])
            f.flush()
            print(f"[{name}] A_baseline: {time_a:.2f}s -> {result_a}")

            # Pipeline B (YOLO11s)
            try:
                result_b, time_b, detected = pipeline_b(str(img_path))
            except Exception as e:
                result_b, time_b, detected = f"ERROR: {e}", -1, False
            writer.writerow([name, "B_YOLO11s", result_b, gt, round(time_b, 3), detected])
            f.flush()
            print(f"[{name}] B_YOLO11s: {time_b:.2f}s (yolo_detected={detected}) -> {result_b}")

    print(f"\nเสร็จแล้ว บันทึกผลลงไฟล์: {OUTPUT_CSV}")


if __name__ == "__main__":
    main()
