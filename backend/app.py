# app.py
from contextlib import asynccontextmanager

from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import Response
import uvicorn

from detect import detect_and_scrape, debug_crop, warmup

from auth import router as auth_router

@asynccontextmanager
async def lifespan(app: FastAPI):
    warmup()
    yield


app = FastAPI(title="FDA Image Checker API", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth_router)
def _read_image(file: UploadFile, img_bytes: bytes) -> None:
    if not file.content_type or not file.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="Uploaded file is not an image")
    if not img_bytes:
        raise HTTPException(status_code=400, detail="Empty image file")


@app.get("/health")
async def health():
    return {"status": "ok"}


@app.post("/check-oy")
async def check_oy(file: UploadFile = File(...)):
    """
    อัปโหลดรูป → หมุนรูปให้ตรง → YOLO หากรอบ อย. → crop → OCR → ดึงข้อมูลจากเว็บ อย.

    การหมุนรูป / ซูม ทำอยู่ใน detect.py ทั้งหมด (ฟังก์ชัน deskew_and_detect)
    อย่ามาทำซ้ำที่ชั้นนี้ ไม่งั้นรูปจะโดนหมุนสองรอบ มุมเพี้ยนและช้าเป็นเท่าตัว
    """
    img_bytes = await file.read()
    _read_image(file, img_bytes)

    try:
        return await detect_and_scrape(img_bytes)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Processing error: {e}")


@app.post(
    "/debug/crop",
    response_class=Response,
 
    responses={
        200: {"content": {"image/jpeg": {}}, "description": "รูป crop ที่ส่งให้ OCR"},
        404: {"description": "ไม่พบกรอบ อย."},
    },
)
async def debug_crop_endpoint(file: UploadFile = File(...)):
    """
    ดีบัก: คืนรูปที่ YOLO crop ออกมาจริงๆ (ก่อนส่งเข้า OCR)
    เปิดดูใน browser / Postman ได้เลย ว่ากรอบตรงมั้ย ชัดพอมั้ย
    ข้อมูล box/conf/angle/zoomed อยู่ใน response header: X-Detection
    """
    img_bytes = await file.read()
    _read_image(file, img_bytes)

    try:
        crop_bytes, info = await debug_crop(img_bytes)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Processing error: {e}")

    if crop_bytes is None:
        raise HTTPException(status_code=404, detail="YOLO ไม่พบกรอบ อย. ในรูปนี้")

    return Response(
        content=crop_bytes,          
        media_type="image/jpeg",
        headers={
            "X-Detection": str(info).encode("ascii", "backslashreplace").decode(),
            "Cache-Control": "no-store",
        },
    )


if __name__ == "__main__":
    uvicorn.run("app:app", host="0.0.0.0", port=8000, reload=True)