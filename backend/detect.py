# detect.py

import os

import re

import base64

import asyncio

import threading

from concurrent.futures import ThreadPoolExecutor

import cv2

import numpy as np

import requests

from bs4 import BeautifulSoup

from openai import OpenAI

from ultralytics import YOLO

from dotenv import load_dotenv

load_dotenv()

# ============================================================

# CONFIG — ปรับผ่าน .env ได้หมด ไม่ต้องแก้โค้ด

# ============================================================

MODEL_PATH   = os.getenv("YOLO_MODEL_PATH", "Yolo11s.pt")

CONF_TH      = float(os.getenv("YOLO_CONF", "0.25"))   # ยิ่งต่ำ ยิ่งเจอง่าย แต่ false positive เยอะ

IOU_TH       = float(os.getenv("YOLO_IOU", "0.45"))

IMGSZ        = int(os.getenv("YOLO_IMGSZ", "640"))     # เท่ากับตอนเทรน

IMGSZ_FAR    = int(os.getenv("YOLO_IMGSZ_FAR", "1280"))  # pass 2 สำหรับรูประยะไกลมาก

# โมเดลนี้เทรนด้วย Dataset_grayscale → แปลงภาพเป็น grayscale ก่อนเข้าโมเดล

# ให้ตรงกับ distribution ตอนเทรน (ตั้ง YOLO_GRAYSCALE=0 เพื่อปิด แล้วลองเทียบผล)

USE_GRAYSCALE = os.getenv("YOLO_GRAYSCALE", "1") == "1"

# ---- DESKEW ----------------------------------------------------------------

# โมเดลเทรนด้วย degrees=0.0 → ไม่เคยเห็นฉลากเอียงเลยสักภาพ

# หน้าต่างที่ตรวจเจอได้กว้างแค่ราว +-10 องศา พ้นจากนั้น conf ตกเป็น 0 ทันที

# ไม่ใช่ค่อยๆ ลด จึงต้องหมุนรูปให้ตรงก่อนเข้าโมเดล (CPU ล้วน ไม่ต้องเทรนใหม่)

USE_DESKEW    = os.getenv("DESKEW", "1") == "1"

DESKEW_MAX    = float(os.getenv("DESKEW_MAX_ANGLE", "90"))   # ช่วงค้นหามุม +-90

DESKEW_WORK   = int(os.getenv("DESKEW_WORK", "1000"))        # ย่อเหลือเท่านี้ตอนวิเคราะห์มุม

DESKEW_MARGIN = float(os.getenv("DESKEW_MARGIN", "0.88"))    # ตัดขอบรูปทิ้งก่อนวิเคราะห์

DESKEW_TRIES  = int(os.getenv("DESKEW_TRIES", "3"))          # ลองมุมอันดับ 1-3 ถ้าอันดับแรกไม่เจอ

# ---- ZOOM ------------------------------------------------------------------

# pass 3: ซอยรูปเป็นช่องซ้อนกันแล้วตรวจทีละช่อง = ซูมเข้าไปดู

# ใช้ตอนถ่ายไกลจนกรอบ อย. เล็กกว่า ~14 px ตอนย่อเข้าโมเดล

ZOOM_GRID     = int(os.getenv("ZOOM_GRID", "3"))       # 0/1 = ปิด, 2 = 4 ช่อง, 3 = 9 ช่อง
ZOOM_OVERLAP  = float(os.getenv("ZOOM_OVERLAP", "0.25"))
PAD_RATIO    = float(os.getenv("CROP_PAD", "0.12"))    # เผื่อขอบรอบกล่อง 12%
MIN_CROP_W   = int(os.getenv("MIN_CROP_W", "900"))     # ถ้า crop เล็กกว่านี้ → upscale
MAX_CROP_W   = int(os.getenv("MAX_CROP_W", "1600"))    # กันรูปใหญ่เกินจำเป็น (เปลืองโทเคน)
ENHANCE      = os.getenv("CROP_ENHANCE", "1") == "1"   # CLAHE + unsharp
OCR_MODEL    = os.getenv("OCR_MODEL", "gpt-4o-mini")

client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

# โหลดโมเดลครั้งเดียวตอน import (อย่าโหลดใหม่ทุก request — ช้ามาก)

model = YOLO(MODEL_PATH)

# ultralytics predict ไม่ thread-safe → ล็อกไว้
_model_lock = threading.Lock()

# ใช้ executor ของตัวเอง ไม่ไปแย่ง default pool ของ FastAPI
_executor = ThreadPoolExecutor(max_workers=2)

def warmup() -> None:
    """Warmup ด้วยภาพขนาดเล็ก เพื่อลดการใช้ RAM ตอนเริ่มระบบ"""
    warmup_imgsz = min(IMGSZ, 320)
    dummy = np.zeros((warmup_imgsz, warmup_imgsz, 3), dtype=np.uint8)

    try:
        with _model_lock:
            model.predict(
                dummy,
                imgsz=warmup_imgsz,
                verbose=False,
            )
    except RuntimeError as e:
        error_message = str(e).lower()
        if "not enough memory" in error_message or "defaultcpuallocator" in error_message:
            print("[WARN] YOLO warmup skipped: RAM ไม่เพียงพอ")
            return
        raise

# ============================================================

# 1) IMAGE UTILS

# ============================================================

def bytes_to_bgr(img_bytes: bytes) -> np.ndarray:
    """bytes → numpy BGR (OpenCV หมุนตาม EXIF ให้อัตโนมัติ)"""
    arr = np.frombuffer(img_bytes, dtype=np.uint8)
    img = cv2.imdecode(arr, cv2.IMREAD_COLOR)
    if img is None:
        raise ValueError("decode รูปไม่ได้ (ไฟล์เสียหรือไม่ใช่รูป)")
    return img

def bgr_to_jpeg_bytes(img: np.ndarray, quality: int = 95) -> bytes:
    ok, buf = cv2.imencode(".jpg", img, [int(cv2.IMWRITE_JPEG_QUALITY), quality])
    if not ok:
        raise ValueError("encode JPEG ไม่สำเร็จ")
    return buf.tobytes()

def to_model_input(bgr: np.ndarray) -> np.ndarray:
    """แปลงเป็น grayscale 3 channel ให้ match กับตอนเทรน"""
    if not USE_GRAYSCALE:
        return bgr
    gray = cv2.cvtColor(bgr, cv2.COLOR_BGR2GRAY)
    return cv2.cvtColor(gray, cv2.COLOR_GRAY2BGR)


def _deskew_binary(bgr: np.ndarray) -> np.ndarray:

    # ตัดขอบทิ้งก่อน กันขอบรูป / ขอบชั้นวาง / แถบดำ มาหลอกว่าเป็น "บรรทัด"

    h, w = bgr.shape[:2]
    my, mx = int(h * (1 - DESKEW_MARGIN) / 2), int(w * (1 - DESKEW_MARGIN) / 2)
    if my or mx:
        bgr = bgr[my:h - my, mx:w - mx]
    h, w = bgr.shape[:2]
    s = DESKEW_WORK / max(h, w)
    if s < 1:
        bgr = cv2.resize(bgr, (int(w * s), int(h * s)), interpolation=cv2.INTER_AREA)
    g = cv2.cvtColor(bgr, cv2.COLOR_BGR2GRAY)
    g = cv2.createCLAHE(3.0, (8, 8)).apply(g)
    b = cv2.adaptiveThreshold(g, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C,

                              cv2.THRESH_BINARY_INV, 25, 12)

    return cv2.medianBlur(b, 3)
def _line_score(bin_img: np.ndarray, ang: float) -> float:
    """คะแนนความ 'เป็นบรรทัด' ยิ่งตัวอักษรเรียงขนานแกน x คะแนนยิ่งสูง"""
    h, w = bin_img.shape
    M = cv2.getRotationMatrix2D((w / 2, h / 2), ang, 1.0)
    r = cv2.warpAffine(bin_img, M, (w, h), flags=cv2.INTER_NEAREST, borderValue=0)
    d = np.diff(r.sum(axis=1, dtype=np.float64))
    return float(d @ d)

def estimate_skew(bgr: np.ndarray, max_angle: float = DESKEW_MAX,
                  coarse: float = 3.0, fine: float = 0.5) -> float:
    """คืนมุมที่ต้องหมุน (องศา ทวนเข็ม) เพื่อให้ตัวหนังสือบนฉลากตั้งตรง"""
    return skew_candidates(bgr, top_k=1, max_angle=max_angle,
                           coarse=coarse, fine=fine)[0]

def skew_candidates(bgr: np.ndarray, top_k: int = DESKEW_TRIES,
                    max_angle: float = DESKEW_MAX, coarse: float = 3.0,
                    fine: float = 0.5, min_sep: float = 15.0) -> list[float]:


    b = _deskew_binary(bgr)
    angs = np.arange(-max_angle, max_angle + 1e-9, coarse)
    sc = np.array([_line_score(b, a) for a in angs])
    # ถ้ายอดสูงสุดไม่โดดกว่าค่ากลางชัดเจน แปลว่ารูปไม่มีแนวตัวอักษรที่ชัด

    # (รูปเบลอ รูปเปล่า) กรณีนี้อย่าหมุน เดามั่วแย่กว่าไม่ทำอะไร

    med = float(np.median(sc))
    if med <= 0 or sc.max() / med < 1.15:
        return [0.0]
    # เก็บยอดที่ห่างกันอย่างน้อย min_sep องศา จะได้ไม่ได้มุมใกล้ ๆ เดิมซ้ำ

    picked: list[float] = []

    for i in np.argsort(sc)[::-1]:
        a = float(angs[i])
        if all(abs(a - p) >= min_sep for p in picked):
            picked.append(a)
        if len(picked) >= max(1, top_k):
            break

    out = []

    for a0 in picked:

        a2 = np.arange(a0 - coarse, a0 + coarse + 1e-9, fine)

        s2 = [_line_score(b, a) for a in a2]

        out.append(round(float(a2[int(np.argmax(s2))]), 1))

    return out

def rotate_expand(bgr: np.ndarray, angle: float) -> np.ndarray:

    """หมุนแบบขยายขอบ ไม่ให้มุมรูปโดนตัด"""

    if abs(angle) < 0.05:

        return bgr

    h, w = bgr.shape[:2]

    cx, cy = w / 2.0, h / 2.0

    M = cv2.getRotationMatrix2D((cx, cy), angle, 1.0)

    cos, sin = abs(M[0, 0]), abs(M[0, 1])

    nw, nh = int(h * sin + w * cos), int(h * cos + w * sin)

    M[0, 2] += nw / 2.0 - cx

    M[1, 2] += nh / 2.0 - cy

    # LINEAR พอ ไม่ต้อง CUBIC — เร็วกว่าเท่าตัวบนรูป 12MP (363 vs 723 ms)

    # และ enhance_crop() มี unsharp ตามหลังอยู่แล้ว

    return cv2.warpAffine(bgr, M, (nw, nh), flags=cv2.INTER_LINEAR,

                          borderMode=cv2.BORDER_REPLICATE)

def auto_deskew(bgr: np.ndarray) -> tuple[np.ndarray, float]:

    """คืน (รูปที่หมุนตรงแล้ว, มุมที่หมุนไป)"""

    if not USE_DESKEW:

        return bgr, 0.0

    a = estimate_skew(bgr)

    return rotate_expand(bgr, a), round(a, 1)

# ============================================================

# 2) YOLO DETECTION — หา "กรอบ อย."

# ============================================================

def _predict(bgr: np.ndarray, imgsz: int) -> list[dict]:

    """คืน list ของ {box, conf, area} เรียงตาม conf มาก→น้อย"""

    src = to_model_input(bgr)

    with _model_lock:

        result = model.predict(

            src,

            imgsz=imgsz,

            conf=CONF_TH,

            iou=IOU_TH,

            max_det=10,

            verbose=False,

        )[0]

    boxes = result.boxes

    if boxes is None or len(boxes) == 0:

        return []

    xyxy  = boxes.xyxy.cpu().numpy()

    confs = boxes.conf.cpu().numpy()

    out = []

    for (x1, y1, x2, y2), c in zip(xyxy, confs):

        out.append({

            "box": [float(x1), float(y1), float(x2), float(y2)],

            "conf": float(c),

            "area": float((x2 - x1) * (y2 - y1)),

        })

    out.sort(key=lambda d: d["conf"], reverse=True)

    return out

def pick_best(cands: list[dict]) -> dict | None:

    """

    เลือกกล่องที่ดีที่สุด

    - ปกติเอา conf สูงสุด

    - แต่ถ้ามีหลายกล่อง conf ใกล้กัน (ห่างไม่เกิน 0.10) ให้เลือกกล่อง "ใหญ่สุด"

      เพราะกล่องใหญ่ = ตัวอักษรชัดกว่า → OCR ง่ายกว่า

    """

    if not cands:

        return None

    top = cands[0]["conf"]

    near = [c for c in cands if c["conf"] >= top - 0.10]

    return max(near, key=lambda d: d["area"])

def _predict_tiled(bgr: np.ndarray, grid: int = ZOOM_GRID,

                   overlap: float = ZOOM_OVERLAP) -> dict | None:

    """

    pass 3 — ซอยรูปเป็นช่องซ้อนกันแล้วตรวจทีละช่อง

    วัดจริงแล้วกรอบ อย. ตอนย่อเข้าโมเดลที่ 640 เหลือสูงแค่ 13-33 px

    ต่ำกว่า ~14 px โมเดลตรวจไม่เจอทันที และการเพิ่ม imgsz ไม่ช่วย ต้องซอยเอา

    แต่ละช่องหามุมเอียงใหม่ของตัวเอง เพราะตอนถ่ายไกล ฉลากกินพื้นที่น้อย

    มุมที่หาจากรูปเต็มจะโดนพื้นหลังกลบ ช่องเล็กลง ฉลากมีน้ำหนักมากขึ้น

    หมายเหตุ: กล่องที่คืนกลับเป็นพิกัดของ "ช่องที่หมุนแล้ว" ไม่ใช่ของรูปเต็ม

    จึงคืนช่องนั้นมาด้วย ให้ผู้เรียกใช้ crop จากช่องแทน

    """

    if grid < 2:

        return None

    H, W = bgr.shape[:2]

    th = int(H / (grid - (grid - 1) * overlap))

    tw = int(W / (grid - (grid - 1) * overlap))

    sy, sx = int(th * (1 - overlap)), int(tw * (1 - overlap))

    best = None

    for iy in range(grid):

        for ix in range(grid):

            y0 = min(iy * sy, max(0, H - th))

            x0 = min(ix * sx, max(0, W - tw))

            tile = bgr[y0:y0 + th, x0:x0 + tw]

            for ang in (skew_candidates(tile, top_k=1) if USE_DESKEW else [0.0]):

                rot = rotate_expand(tile, ang)

                cand = pick_best(_predict(rot, IMGSZ))

                if cand and (best is None or cand["conf"] > best["conf"]):

                    cand["imgsz_used"] = IMGSZ

                    cand["zoomed"] = True

                    cand["_image"] = rot          # crop ต้องใช้รูปช่องนี้

                    cand["_angle"] = round(ang, 1)

                    best = cand

    return best

def detect_label(bgr: np.ndarray) -> dict | None:

    """

    2 pass บนรูปที่หมุนตรงแล้ว (ไม่ทำ deskew เอง ใช้คู่กับ deskew_and_detect)

      pass 1 — imgsz 640 (เท่าตอนเทรน) เร็ว ใช้ได้กับรูปทั่วไป

      pass 2 — imgsz 1280 เฉพาะตอน pass 1 ไม่เจอ

    """

    for size in (IMGSZ, IMGSZ_FAR):

        best = pick_best(_predict(bgr, size))

        if best:

            best["imgsz_used"] = size

            best["zoomed"] = False

            return best

    return None

def deskew_and_detect(bgr: np.ndarray) -> tuple[np.ndarray, dict | None, float]:

    """

    หมุนรูปให้ตรง แล้วหากรอบ อย.

    คืน (รูปที่หมุนแล้ว, det หรือ None, มุมที่ใช้)

    ลำดับ:

      1. ลองมุมอันดับ 1 → pass 640 → pass 1280

      2. ไม่เจอ → ลองมุมอันดับ 2, 3 (พื้นหลังรกอาจดันยอดอันดับ 1 ให้ผิดได้)

      3. ยังไม่เจอ → ซอยรูปซูมเข้าไปดู บนมุมอันดับ 1

    กรณีปกติหยุดตั้งแต่ข้อ 1 เรียก YOLO แค่ครั้งเดียว

    """

    angles = skew_candidates(bgr) if USE_DESKEW else [0.0]

    for ang in angles:

        rot = rotate_expand(bgr, ang)

        det = detect_label(rot)

        if det:

            return rot, det, ang

    det = _predict_tiled(bgr)

    if det:

        # ช่องที่เจอมีรูป + มุมของตัวเอง ต้อง crop จากรูปช่องนั้น

        return det.pop("_image"), det, det.pop("_angle")

    return rotate_expand(bgr, angles[0]), None, angles[0]

# ============================================================

# 3) CROP + ENHANCE — เตรียมรูปให้ OCR อ่านง่าย

# ============================================================

def crop_with_padding(bgr: np.ndarray, box: list[float], pad: float = PAD_RATIO) -> np.ndarray:

    h, w = bgr.shape[:2]

    x1, y1, x2, y2 = box

    bw, bh = x2 - x1, y2 - y1

    x1 = int(max(0, x1 - bw * pad))

    y1 = int(max(0, y1 - bh * pad))

    x2 = int(min(w, x2 + bw * pad))

    y2 = int(min(h, y2 + bh * pad))

    return bgr[y1:y2, x1:x2].copy()

def enhance_crop(crop: np.ndarray) -> np.ndarray:

    """

    รูประยะไกล → crop ออกมาจะเล็กและเบลอ ต้อง:

      1. upscale ให้กว้างอย่างน้อย MIN_CROP_W (INTER_CUBIC)

      2. CLAHE เพิ่ม contrast ตัวเลขกับพื้นหลัง

      3. unsharp mask ดึงขอบตัวอักษรให้คม

    """

    h, w = crop.shape[:2]

    if w == 0 or h == 0:

        return crop

    if w < MIN_CROP_W:

        scale = MIN_CROP_W / w

        crop = cv2.resize(crop, (int(w * scale), int(h * scale)), interpolation=cv2.INTER_CUBIC)

    elif w > MAX_CROP_W:

        scale = MAX_CROP_W / w

        crop = cv2.resize(crop, (int(w * scale), int(h * scale)), interpolation=cv2.INTER_AREA)

    if not ENHANCE:

        return crop

    lab = cv2.cvtColor(crop, cv2.COLOR_BGR2LAB)

    l, a, b = cv2.split(lab)

    l = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8)).apply(l)

    crop = cv2.cvtColor(cv2.merge([l, a, b]), cv2.COLOR_LAB2BGR)

    blur = cv2.GaussianBlur(crop, (0, 0), 3)

    crop = cv2.addWeighted(crop, 1.5, blur, -0.5, 0)

    return crop

def prepare_crop(bgr: np.ndarray, det: dict) -> bytes:

    crop = crop_with_padding(bgr, det["box"])

    crop = enhance_crop(crop)

    return bgr_to_jpeg_bytes(crop)

# ============================================================

# 4) OCR อ่านเลข อย.

# ============================================================

OCR_PROMPT = (

    "ภาพนี้คือกรอบเครื่องหมาย อย. บนฉลากอาหาร\n"

    "อ่านเลขสารบบอาหาร 13 หลักในกรอบ\n"

    "ตอบเฉพาะตัวเลขรูปแบบ XX-X-XXXXX-X-XXXX เท่านั้น ห้ามมีข้อความอื่น\n"

    "ถ้าอ่านไม่ออกหรือไม่พบ ตอบว่า ไม่พบ"

)

def ocr_oy_number(img_bytes: bytes) -> dict:

    b64 = base64.b64encode(img_bytes).decode()

    resp = client.chat.completions.create(

        model=OCR_MODEL,

        messages=[{

            "role": "user",

            "content": [

                {"type": "text", "text": OCR_PROMPT},

                {

                    "type": "image_url",

                    "image_url": {

                        "url": f"data:image/jpeg;base64,{b64}",

                        "detail": "high",   # สำคัญ! ตัวเลขเล็ก ต้องใช้ high

                    },

                },

            ],

        }],

        max_tokens=50,

        temperature=0,

    )

    text = (resp.choices[0].message.content or "").strip()

    digits = "".join(re.findall(r"\d", text))

    if len(digits) != 13:

        return {"success": False, "message": "ไม่พบเลข อย. ในภาพ", "oy_number": None}

    oy = (

        f"{digits[0:2]}-{digits[2]}-{digits[3:8]}-{digits[8]}-{digits[9:13]}"

    )

    return {"success": True, "message": "พบเลข อย.", "oy_number": oy}

# เก็บชื่อเดิมไว้ เผื่อมีโค้ดอื่น import อยู่

process_image = ocr_oy_number

# ============================================================

# 5) เว็บ อย. (ของเดิม ไม่เปลี่ยน)

# ============================================================

FDA_URL = (

    "https://porta.fda.moph.go.th/FDA_SEARCH_ALL/PRODUCT/"

    "FRM_PRODUCT_FOOD.aspx?fdpdtno={pid}"

)

def fetch_product_page(product_id: str) -> str | None:

    resp = requests.get(

        FDA_URL.format(pid=product_id),

        headers={"User-Agent": "Mozilla/5.0"},

        timeout=15,

    )

    return resp.text if resp.status_code == 200 else None

def extract_product_info(html: str, product_id: str) -> dict:

    soup = BeautifulSoup(html, "html.parser")

    def get_span(span_id: str) -> str:

        el = soup.find("span", id=span_id)

        return el.get_text(strip=True) if el else ""

    return {

        "product_id": product_id,

        "registration_number": get_span("ContentPlaceHolder1_lbl_fdpdtno"),

        "url": FDA_URL.format(pid=product_id),

        "type": get_span("ContentPlaceHolder1_lbl_typecd"),

        "name_th": get_span("ContentPlaceHolder1_lbl_thai"),

        "name_en": get_span("ContentPlaceHolder1_lbl_eng"),

        "product_status": get_span("ContentPlaceHolder1_lbl_lcnstatus0"),

        "location_name": get_span("ContentPlaceHolder1_lbl_lctname"),

        "address_name": get_span("ContentPlaceHolder1_lbl_lctaddr"),

    }

# ============================================================

# 6) MAIN PIPELINE

# ============================================================

def process_image_and_fetch_oy_detail(img_bytes: bytes) -> dict:

    bgr = bytes_to_bgr(img_bytes)

    # ---- STEP 0+1: หมุนรูปให้ตรง แล้ว YOLO หากรอบ ----

    # ต้องหมุนก่อนเพราะโมเดลไม่ทนภาพเอียง

    # รูปที่ตรงแล้วยังช่วยให้ OCR อ่านง่ายขึ้นด้วย ได้สองต่อ

    bgr, det, angle = deskew_and_detect(bgr)

    detection_info = {

        "found": det is not None,

        "conf": round(det["conf"], 3) if det else None,

        "box": [round(v) for v in det["box"]] if det else None,

        "imgsz_used": det.get("imgsz_used") if det else None,

        "angle": angle,                                          # รูปเอียงกี่องศา (หมุนแก้แล้ว)

        "zoomed": det.get("zoomed", False) if det else False,    # ต้องซอยรูปถึงเจอมั้ย

    }

    # ---- STEP 2: OCR ----

    if det:

        oy_result = ocr_oy_number(prepare_crop(bgr, det))

        ocr_source = "crop"

        # crop อ่านไม่ออก → ลองทั้งรูปอีกที (กันกรณี box เพี้ยน)

        if not oy_result["success"]:

            oy_result = ocr_oy_number(bgr_to_jpeg_bytes(bgr))

            ocr_source = "crop_then_full"

    else:

        oy_result = ocr_oy_number(bgr_to_jpeg_bytes(bgr))

        ocr_source = "full_image"

    meta = {"detection": detection_info, "ocr_source": ocr_source}

    if not oy_result["success"]:

        return {**oy_result, **meta}

    # ---- STEP 3: ยิงเว็บ อย. ----

    oy_number = oy_result["oy_number"]

    product_id = oy_number.replace("-", "")

    html = fetch_product_page(product_id)

    if not html:

        return {

            "success": False,

            "message": "ไม่สามารถดึงหน้าเว็บ อย. ได้",

            "oy_number": oy_number,

            **meta,

        }

    detail = extract_product_info(html, product_id)

    if not detail["registration_number"]:

        return {

            "success": False,

            "message": "ไม่พบข้อมูลเลข อย. ในระบบ",

            "oy_number": oy_number,

            **meta,

        }

    return {

        "success": True,

        "message": "ดึงข้อมูลเลข อย. สำเร็จ",

        "oy_number": oy_number,

        "detail": detail,

        **meta,

    }

# ============================================================

# 7) ฟังก์ชันที่ FastAPI import

# ============================================================

async def detect_and_scrape(img_bytes: bytes) -> dict:

    loop = asyncio.get_running_loop()

    return await loop.run_in_executor(

        _executor, process_image_and_fetch_oy_detail, img_bytes

    )

async def debug_crop(img_bytes: bytes) -> tuple[bytes | None, dict]:

    """ใช้กับ endpoint /debug/crop — ดูว่า YOLO crop อะไรส่งให้ OCR"""

    def _work():

        bgr = bytes_to_bgr(img_bytes)

        bgr, det, angle = deskew_and_detect(bgr)

        if not det:

            return None, {"found": False, "angle": angle}

        return prepare_crop(bgr, det), {

            "found": True,

            "conf": round(det["conf"], 3),

            "box": [round(v) for v in det["box"]],

            "imgsz_used": det["imgsz_used"],

            "angle": angle,

            "zoomed": det.get("zoomed", False),

        }

    loop = asyncio.get_running_loop()

    return await loop.run_in_executor(_executor, _work)