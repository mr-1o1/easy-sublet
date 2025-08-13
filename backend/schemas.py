from pydantic import BaseModel, EmailStr


class UserCreate(BaseModel):
	email: EmailStr
	password: str
	name: str | None = None


class UserOut(BaseModel):
	id: int
	email: EmailStr
	name: str | None

	class Config:
		from_attributes = True


class TokenResponse(BaseModel):
	access_token: str
	token_type: str = "bearer"


