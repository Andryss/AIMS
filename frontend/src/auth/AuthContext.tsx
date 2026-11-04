import React, { createContext, useContext } from 'react';
import { AuthMeResponse } from '../types';
import { Access } from './access';

export interface AuthSession { token: string; profile: AuthMeResponse; access: Access; }

export const AuthContext = createContext<AuthSession | null>(null);

export function AuthProvider({ value, children }: { value: AuthSession; children: React.ReactNode }) {
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthSession {
  const session = useContext(AuthContext);
  if (!session) { throw new Error('useAuth requires an authenticated session'); }
  return session;
}
