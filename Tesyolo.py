"""
test_yolo_detect.py
ทดสอบ YOLO เดี่ยวๆ ก่อนต่อเข้า pipeline เต็ม (detect.py)
ใช้เช็คว่าโมเดล .pt ที่ได้รับมา detect เลข อย. ได้จริงไหม บนภาพของเราเอง

วิธีใช้:
    pip install ultralytics opencv-python --break-system-packages
    python test_yolo_detect.py --image test_images/img001.jpg --model yolo11s.pt
    python test_yolo_detect.py --image test_images/img001.jpg --model yolov8s.pt --output out_v8.jpg
"""

import argparse

import cv2
from ultralytics import YOLO


def detect_and_save(
    image_path: str,
    model_path: str,
    output_path: str = "detected_output.jpg",
    conf_threshold: float = 0.25,
):
    model = YOLO(model_path)
    results = model.predict(image_path, conf=conf_threshold, verbose=False)

    boxes = results[0].boxes
    print(f"image: {image_path}")
    print(f"model: {model_path}")
    print(f"Total found: {len(boxes)} boxes\n")

    img = cv2.imread(image_path)
    if img is None:
        print("can't read image, check the path")
        return boxes

    for i, box in enumerate(boxes):
        x1, y1, x2, y2 = map(int, box.xyxy[0])
        conf = float(box.conf[0])
        cls_id = int(box.cls[0])
        cls_name = model.names[cls_id]

        print(
            f"  box {i + 1}: class={cls_name}, "
            f"confidence={conf:.2f}, index=({x1},{y1})-({x2},{y2})"
        )

        # วาดกรอบ + label ลงบนภาพเพื่อดูผลด้วยตา
        cv2.rectangle(img, (x1, y1), (x2, y2), (0, 255, 0), 2)
        label = f"{cls_name} {conf:.2f}"
        cv2.putText(
            img, label, (x1, max(y1 - 10, 0)),
            cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2,
        )

    cv2.imwrite(output_path, img)
    print(f"\nบันทึกภาพที่มีกรอบแล้วที่: {output_path}")

    if len(boxes) == 0:
        print(
            "ไม่เจอกรอบเลย ลองลด --conf ให้ต่ำลง (เช่น 0.1) "
            "หรือเช็คว่าโมเดลนี้ถูกเทรนมาสำหรับเลข อย. จริงหรือเปล่า"
        )

    return boxes


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="ทดสอบ YOLO detect เลข อย. บนภาพเดียว")
    parser.add_argument("--image", required=True, help="path ของภาพที่จะทดสอบ")
    parser.add_argument("--model", default="yolo11s.pt", help="path ของไฟล์โมเดล .pt")
    parser.add_argument("--output", default="detected_output.jpg", help="path ไฟล์ผลลัพธ์ที่จะบันทึก")
    parser.add_argument("--conf", type=float, default=0.25, help="ค่า confidence threshold ขั้นต่ำ")
    args = parser.parse_args()

    detect_and_save(args.image, args.model, args.output, args.conf)