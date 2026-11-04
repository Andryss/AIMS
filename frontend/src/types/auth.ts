export type RoleName = 'OPERATOR' | 'ANALYST' | 'ADMIN' | 'AGENT' | 'STOREKEEPER' | 'TENDER_MANAGER' | 'MANAGEMENT' | 'PRISON_GUARD' | 'CLEANER';

export interface SignInRequest {
  login: string;
  password: string;
}

export interface SignInResponse {
  accessToken: string;
}

export interface AuthMeResponse {
  userId: number;
  login: string;
  roles: string[];
  permissions: string[];
}

export interface UserSummary {
  id: number;
  login: string;
}

export interface UserSearchResponse {
  items: UserSummary[];
}

export interface BatchUsersRequest {
  ids: number[];
}

export interface BatchUsersResponse {
  items: UserSummary[];
}
