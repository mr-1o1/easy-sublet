from datetime import timedelta

from fastapi import Depends, FastAPI, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.orm import Session

from . import schemas
from .auth import (
	ACCESS_TOKEN_EXPIRE_MINUTES,
	create_access_token,
	get_current_user,
	get_db,
	get_password_hash,
	verify_password,
)
from .database import Base, engine
from .models import User


app = FastAPI(title="Easy Sublet API")

# For local development and emulator testing. Adjust origins for web if needed.
app.add_middleware(
	CORSMiddleware,
	allow_origins=["*"],
	allow_credentials=True,
	allow_methods=["*"],
	allow_headers=["*"],
)


@app.get("/health")
def health_check() -> dict:
	return {"status": "ok"}


# Create tables (simple for MVP; prefer Alembic later)
Base.metadata.create_all(bind=engine)


@app.post("/auth/signup", response_model=schemas.UserOut)
def signup(user_in: schemas.UserCreate, db: Session = Depends(get_db)):
	existing = db.query(User).filter(User.email == user_in.email).first()
	if existing:
		raise HTTPException(status_code=400, detail="Email already registered")
	user = User(
		email=user_in.email,
		name=user_in.name,
		hashed_password=get_password_hash(user_in.password),
	)
	db.add(user)
	db.commit()
	db.refresh(user)
	return user


@app.post("/auth/login", response_model=schemas.TokenResponse)
def login(
	form_data: OAuth2PasswordRequestForm = Depends(),
	db: Session = Depends(get_db),
):
	user = db.query(User).filter(User.email == form_data.username).first()
	if not user or not verify_password(form_data.password, user.hashed_password):
		raise HTTPException(
			status_code=status.HTTP_401_UNAUTHORIZED,
			detail="Incorrect email or password",
			headers={"WWW-Authenticate": "Bearer"},
		)
	access_token_expires = timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
	access_token = create_access_token(
		data={"sub": user.email}, expires_delta=access_token_expires
	)
	return {"access_token": access_token, "token_type": "bearer"}


@app.get("/auth/me", response_model=schemas.UserOut)
def read_users_me(current_user: User = Depends(get_current_user)):
	return current_user


