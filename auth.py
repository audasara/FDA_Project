#auth
"""
ระบบสมาชิกสำหรับแอปตรวจเลข อย.
เก็บผู้ใช้ลง SQLite (ไฟล์ users.db วางข้างไฟล์นี้) และออก JWT ให้แอปถือไว้

ติดตั้งเพิ่มแค่ตัวเดียว:
    pip install pyjwt

ต่อเข้ากับ app.py ด้วยสองบรรทัด:
    from auth import router as auth_router
    app.include_router(auth_router)

ไม่ได้ใส่ tags ไว้ ทุก endpoint เลยไปรวมอยู่กองเดียวกันในหน้า /docs

รหัสผ่านไม่ได้เก็บเป็นตัวหนังสือ แต่ผ่าน PBKDF2-HMAC-SHA256 390,000 รอบ
พร้อม salt เฉพาะของแต่ละคน ต่อให้ไฟล์ฐานข้อมูลหลุดก็ย้อนกลับเป็นรหัสเดิมไม่ได้
"""

import base64
import hashlib
import os
import pathlib
import re
import secrets
import sqlite3
from datetime import datetime, timedelta, timezone

import jwt
from fastapi import APIRouter
from pydantic import BaseModel

# ===================== ตั้งค่า =====================

# ตอนใช้งานจริงให้ตั้ง environment variable ชื่อ FDA_SECRET_KEY
# ห้ามปล่อยค่า default นี้ขึ้นเซิร์ฟเวอร์จริง ใครรู้คีย์นี้ปลอม token ได้ทันที
SECRET_KEY = os.getenv("FDA_SECRET_KEY", "dev-only-change-me-before-deploy")
ALGORITHM = "HS256"
TOKEN_DAYS = 30

DB_PATH = pathlib.Path(__file__).with_name("users.db")
PBKDF2_ROUNDS = 390_000

EMAIL_RE = re.compile(r"^[^@\s]+@[^@\s]+\.[^@\s]+$")

router = APIRouter(prefix="/auth")


# ===================== ฐานข้อมูล =====================

def get_conn() -> sqlite3.Connection:
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


def init_db() -> None:
    with get_conn() as conn:
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS users (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                email         TEXT    NOT NULL UNIQUE,
                name          TEXT    NOT NULL DEFAULT '',
                password_hash TEXT    NOT NULL,
                salt          TEXT    NOT NULL,
                created_at    TEXT    NOT NULL
            )
            """
        )
        conn.execute("CREATE INDEX IF NOT EXISTS idx_users_email ON users(email)")


init_db()


# ===================== รหัสผ่าน =====================

def hash_password(password: str, salt: bytes) -> str:
    dk = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), salt, PBKDF2_ROUNDS)
    return base64.b64encode(dk).decode("ascii")


def verify_password(password: str, salt_b64: str, hash_b64: str) -> bool:
    salt = base64.b64decode(salt_b64)
    calculated = hash_password(password, salt)
    # เทียบแบบเวลาคงที่ กันการเดารหัสจากเวลาที่ใช้ตอบ
    return secrets.compare_digest(calculated, hash_b64)


# ===================== โทเคน =====================

def make_token(user_id: int, email: str) -> str:
    now = datetime.now(timezone.utc)
    payload = {
        "sub": str(user_id),
        "email": email,
        "iat": now,
        "exp": now + timedelta(days=TOKEN_DAYS),
    }
    return jwt.encode(payload, SECRET_KEY, algorithm=ALGORITHM)


# ===================== รูปแบบข้อมูล =====================

class RegisterBody(BaseModel):
    email: str
    password: str
    name: str | None = None


class LoginBody(BaseModel):
    email: str
    password: str


class UserOut(BaseModel):
    email: str
    name: str


class AuthOut(BaseModel):
    success: bool
    message: str
    token: str | None = None
    user: UserOut | None = None


# ===================== เส้นทาง =====================

@router.post("/register", response_model=AuthOut)
def register(body: RegisterBody):
    """สมัครสมาชิก บันทึกลงตาราง users สมัครเสร็จได้โทเคนเลย ไม่ต้องล็อกอินซ้ำ"""
    email = body.email.strip().lower()
    name = (body.name or "").strip() or email.split("@")[0]

    if not EMAIL_RE.match(email):
        return AuthOut(success=False, message="รูปแบบอีเมลไม่ถูกต้อง")
    if len(body.password) < 6:
        return AuthOut(success=False, message="รหัสผ่านต้องมีอย่างน้อย 6 ตัวอักษร")

    salt = os.urandom(16)
    now = datetime.now(timezone.utc).isoformat()

    try:
        with get_conn() as conn:
            cur = conn.execute(
                "INSERT INTO users (email, name, password_hash, salt, created_at) "
                "VALUES (?, ?, ?, ?, ?)",
                (
                    email,
                    name,
                    hash_password(body.password, salt),
                    base64.b64encode(salt).decode("ascii"),
                    now,
                ),
            )
            user_id = cur.lastrowid
    except sqlite3.IntegrityError:
        return AuthOut(success=False, message="อีเมลนี้ถูกใช้สมัครไปแล้ว")

    return AuthOut(
        success=True,
        message="สมัครสมาชิกสำเร็จ",
        token=make_token(user_id, email),
        user=UserOut(email=email, name=name),
    )


@router.post("/login", response_model=AuthOut)
def login(body: LoginBody):
    """เข้าสู่ระบบ เทียบรหัสผ่านกับที่เก็บไว้

    อีเมลผิดกับรหัสผิดตอบข้อความเดียวกัน กันการไล่เดาว่าอีเมลไหนมีอยู่ในระบบ
    """
    email = body.email.strip().lower()

    with get_conn() as conn:
        row = conn.execute(
            "SELECT id, email, name, password_hash, salt FROM users WHERE email = ?",
            (email,),
        ).fetchone()

    if row is None or not verify_password(body.password, row["salt"], row["password_hash"]):
        return AuthOut(success=False, message="อีเมลหรือรหัสผ่านไม่ถูกต้อง")

    return AuthOut(
        success=True,
        message="เข้าสู่ระบบสำเร็จ",
        token=make_token(row["id"], row["email"]),
        user=UserOut(email=row["email"], name=row["name"]),
    )