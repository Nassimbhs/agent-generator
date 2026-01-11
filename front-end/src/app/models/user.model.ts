export interface User {
  id: number;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  isActive: boolean;
  roles: string[];
  createdAt: string;
  updatedAt: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
}

export interface AuthResponse {
  token: string;
  type: string;
  user: User;
}


