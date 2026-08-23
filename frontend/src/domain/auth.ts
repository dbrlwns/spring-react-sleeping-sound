export interface AuthSession {
  authenticated: boolean;
  username: string | null;
}

export interface AuthCredentials {
  username: string;
  password: string;
}

export type AuthMode = 'login' | 'register';
