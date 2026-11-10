#preprocess
import cv2
import numpy as np

# ความสูงขั้นต่ำของกรอบ อย. (พิกเซล) ตอนเข้าโมเดลที่ imgsz=640
# วัดจากการทดสอบจริง: ต่ำกว่า ~14 px โมเดลเริ่มตรวจไม่เจอ
MIN_BOX_H_AT_INPUT = 16


# ══════════════════════════════════════════════════════ 1. DESKEW
def _binarize(img, work=1000, center=0.88):
    # ตัดขอบทิ้งก่อน: กันขอบภาพ / ขอบชั้นวาง / แถบดำ มาหลอกว่าเป็น "บรรทัด"
    h, w = img.shape[:2]
    my, mx = int(h * (1 - center) / 2), int(w * (1 - center) / 2)
    img = img[my:h - my, mx:w - mx] if (my or mx) else img

    h, w = img.shape[:2]
    s = work / max(h, w)
    if s < 1:
        img = cv2.resize(img, (int(w * s), int(h * s)), interpolation=cv2.INTER_AREA)
    g = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    g = cv2.createCLAHE(3.0, (8, 8)).apply(g)
    b = cv2.adaptiveThreshold(g, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
                              cv2.THRESH_BINARY_INV, 25, 12)
    return cv2.medianBlur(b, 3)


def _line_score(bin_img, ang):
    """คะแนนความ 'เป็นบรรทัด' : ยิ่งตัวอักษรเรียงขนานแกน x คะแนนยิ่งสูง"""
    h, w = bin_img.shape
    M = cv2.getRotationMatrix2D((w / 2, h / 2), ang, 1.0)
    r = cv2.warpAffine(bin_img, M, (w, h), flags=cv2.INTER_NEAREST, borderValue=0)
    d = np.diff(r.sum(axis=1, dtype=np.float64))
    return float(d @ d)


def estimate_skew(img, max_angle=60.0, coarse=3.0, fine=0.5):
    """
    คืนมุมที่ต้องหมุน (องศา ทวนเข็ม) เพื่อให้ตัวหนังสือบนฉลากตั้งตรง
    max_angle=60  ครอบคลุมภาพถ่ายมือถือเกือบทั้งหมด (ค่า default)
    max_angle=90  เผื่อกรณีตะแคงขวด/ถือสินค้าหมุน 90 องศา (แทบไม่ช้าลงเลย)
    """
    b = _binarize(img)
    angs = np.arange(-max_angle, max_angle + 1e-9, coarse)
    sc = [_line_score(b, a) for a in angs]
    a0 = float(angs[int(np.argmax(sc))])
    angs2 = np.arange(a0 - coarse, a0 + coarse + 1e-9, fine)
    sc2 = [_line_score(b, a) for a in angs2]
    return float(angs2[int(np.argmax(sc2))])


def rotate_keep(img, angle, border=(255, 255, 255)):
    """หมุนภาพแบบขยายขอบ ไม่ให้มุมภาพโดนตัด  คืน (ภาพ, เมทริกซ์ M)"""
    if abs(angle) < 0.05:
        return img, None
    h, w = img.shape[:2]
    cx, cy = w / 2.0, h / 2.0
    M = cv2.getRotationMatrix2D((cx, cy), angle, 1.0)
    cos, sin = abs(M[0, 0]), abs(M[0, 1])
    nw, nh = int(h * sin + w * cos), int(h * cos + w * sin)
    M[0, 2] += nw / 2.0 - cx
    M[1, 2] += nh / 2.0 - cy
    out = cv2.warpAffine(img, M, (nw, nh), flags=cv2.INTER_CUBIC,
                         borderMode=cv2.BORDER_CONSTANT, borderValue=border)
    return out, M


def auto_deskew(img, max_angle=60.0):
    """ปรับภาพให้ตรงตามแนวตัวหนังสือ  คืน (ภาพที่ตรงแล้ว, มุมที่หมุนไป)"""
    a = estimate_skew(img, max_angle=max_angle)
    out, _ = rotate_keep(img, a)
    return out, a


# ══════════════════════════════════════════════════════ 2. DETECT + ZOOM
def _run(model, img, conf, imgsz, device):
    r = model.predict(img, conf=conf, imgsz=imgsz, verbose=False, device=device)[0]
    if r.boxes is None or len(r.boxes) == 0:
        return 0.0, None
    c = r.boxes.conf.cpu().numpy()
    i = int(c.argmax())
    return float(c[i]), r.boxes.xyxy.cpu().numpy()[i].astype(float)


def _tiles(H, W, grid, overlap):
    th = int(H / (grid - (grid - 1) * overlap))
    tw = int(W / (grid - (grid - 1) * overlap))
    sy, sx = int(th * (1 - overlap)), int(tw * (1 - overlap))
    for iy in range(grid):
        for ix in range(grid):
            y0 = min(iy * sy, max(0, H - th))
            x0 = min(ix * sx, max(0, W - tw))
            yield x0, y0, min(tw, W - x0), min(th, H - y0)


def smart_detect(model, img_bgr, conf=0.25, imgsz=640, device="cpu",
                 accept_conf=0.55, max_angle=60.0, zoom_grid=3,
                 ocr_target_h=120, max_side=2000):
    """
    ปรับภาพ -> ตรวจจับ -> ถ้าฉลากเล็กก็ซอยภาพซูมเข้าไปหา -> คืน crop ที่พร้อมเข้า OCR

    คืน dict
      found      : bool
      conf       : ความมั่นใจ
      angle      : ภาพต้นฉบับเอียงกี่องศา (ที่แก้ไปแล้ว)
      stage      : "full" = เจอจากภาพเต็ม, "zoom" = ต้องซูมถึงเจอ
      crop       : ภาพกรอบ อย. ตั้งตรง + ขยายจนสูงพอ  <-- ส่งเข้า EasyOCR
      deskewed   : ภาพเต็มที่หมุนตรงแล้ว (ไว้ debug / วาดกรอบ)
      box        : [x1,y1,x2,y2] บน deskewed
    """
    out = {"found": False, "conf": 0.0, "angle": 0.0,
           "stage": None, "crop": None, "deskewed": None, "box": None}

    # ---- ขั้นที่ 0 : ย่อภาพถ้าใหญ่เกิน  (รูปมือถือ 12 MP หมุนทีใช้เวลาเป็นวินาที)
    h, w = img_bgr.shape[:2]
    if max(h, w) > max_side:
        s = max_side / max(h, w)
        img_bgr = cv2.resize(img_bgr, (int(w * s), int(h * s)),
                             interpolation=cv2.INTER_AREA)

    # ---- ขั้นที่ 1 : ปรับภาพให้ตรง
    dsk, ang = auto_deskew(img_bgr, max_angle=max_angle)
    out["deskewed"], out["angle"] = dsk, round(ang, 1)

    # ---- ขั้นที่ 2 : ตรวจจากภาพเต็ม
    c, box = _run(model, dsk, conf, imgsz, device)
    stage = "full"

    # ---- ขั้นที่ 3 : ยังไม่เจอ / ไม่มั่นใจ -> ซอยภาพซูมเข้าไปหา
    if c < accept_conf:
        H, W = dsk.shape[:2]
        for x0, y0, tw, th in _tiles(H, W, zoom_grid, 0.25):
            tc, tb = _run(model, dsk[y0:y0+th, x0:x0+tw], conf, imgsz, device)
            if tc > c:
                c, stage = tc, "zoom"
                box = tb + np.array([x0, y0, x0, y0])

    if box is None or c < conf:
        return out

    # ---- ขั้นที่ 4 : crop + ขยายให้ตัวเลขใหญ่พอสำหรับ OCR
    H, W = dsk.shape[:2]
    bw, bh = box[2] - box[0], box[3] - box[1]
    padx, pady = bw * 0.06, bh * 0.30
    x1 = int(max(0, box[0] - padx)); y1 = int(max(0, box[1] - pady))
    x2 = int(min(W, box[2] + padx)); y2 = int(min(H, box[3] + pady))
    crop = dsk[y1:y2, x1:x2]

    if crop.size and crop.shape[0] < ocr_target_h:
        s = ocr_target_h / crop.shape[0]
        crop = cv2.resize(crop, (int(crop.shape[1] * s), ocr_target_h),
                          interpolation=cv2.INTER_CUBIC)

    out.update(found=True, conf=round(c, 4), stage=stage,
               crop=crop, box=[x1, y1, x2, y2])
    return out


if __name__ == "__main__":
    import sys, time
    from ultralytics import YOLO

    model = YOLO(sys.argv[1] if len(sys.argv) > 1 else "Yolo11s.pt")
    for path in sys.argv[2:]:
        img = cv2.imread(path)
        t = time.time()
        r = smart_detect(model, img)
        print(f"{path.split('/')[-1]:26s} found={r['found']} conf={r['conf']:.3f} "
              f"angle={r['angle']:+.1f}° stage={r['stage']} "
              f"crop={None if r['crop'] is None else r['crop'].shape[:2]} "
              f"({(time.time()-t)*1000:.0f} ms)")