export type IncidentEventType =
  | 'UNIDENTIFIED_SIGHTING'
  | 'CONTACT_SUSPECT'
  | 'ILLEGAL_UFO_LANDING'
  | 'MEMORY_ANOMALY'
  | 'ALIEN_ARTIFACT'
  | 'ALIEN_CAPTURE';

export type IncidentStatus =
  | 'DRAFT'
  | 'READY_FOR_ANALYSIS'
  | 'READY_FOR_EXECUTION'
  | 'CLARIFICATION_REQUIRED';

export interface SignInRequest {
  login: string;
  password: string;
}

export interface SignInResponse {
  accessToken: string;
}

export interface AuthMeResponse {
  login: string;
  roles: string[];
  permissions: string[];
}

export interface CreateIncidentRequest {
  eventType: IncidentEventType;
  location: string;
  detectedAt: string;
  description: string;
  attachmentFileIds: number[];
}

export interface IncidentResponse {
  id: number;
  status: IncidentStatus;
  eventType: IncidentEventType;
  location: string;
  detectedAt: string;
  description: string;
  attachmentFileIds: number[];
  alienId?: number;
  createdAt: string;
  updatedAt: string;
}

export interface ChangeIncidentStatusRequest {
  status: IncidentStatus;
}

export interface IncidentListResponse {
  items: IncidentResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface Alien {
  id: number;
  name: string;
  description: string;
  threatLevel: number;
}

export interface AlienSearchResponse {
  items: Alien[];
}

export interface FileUploadResponse {
  id: number;
  fileName: string;
  contentType: string;
  fileSize: number;
  createdAt: string;
}

export interface NotificationItem {
  id: number;
  message: string;
  relatedEntities: string[];
  read: boolean;
  readAt?: string;
  createdAt: string;
}

export interface NotificationListResponse {
  items: NotificationItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface UnreadCountResponse {
  count: number;
}

export interface ErrorObject {
  code?: number;
  message?: string;
  humanMessage?: string;
}
