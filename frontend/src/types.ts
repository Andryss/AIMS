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
  | 'PREPARATION_FOR_EXECUTION'
  | 'PREPARED_FOR_EXECUTION'
  | 'EXECUTING'
  | 'EXECUTION_COMPLETED'
  | 'CLARIFICATION_REQUIRED'
  | 'REANALYSIS_REQUIRED';

export type CleanupStatus = 'PREPARATION' | 'EXECUTION' | 'COMPLETED';

export type RoleName = 'OPERATOR' | 'ANALYST' | 'ADMIN' | 'AGENT' | 'CLEANER';

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
  responsibleUserId?: number | null;
  executorUserIds: number[];
  cleanupStatus?: CleanupStatus | null;
  cleanupReportId?: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface ChangeIncidentStatusRequest {
  status: IncidentStatus;
  comment?: string;
}

export interface ChangeCleanupStatusRequest {
  status: CleanupStatus;
}

export interface CreateCleanupReportRequest {
  description: string;
  attachmentFileIds: number[];
}

export interface CleanupReportResponse {
  id: number;
  incidentId: number;
  description: string;
  attachmentFileIds: number[];
  createdByUserId: number;
  createdAt: string;
}

export interface CreateIncidentCommentRequest {
  text: string;
}

export interface IncidentComment {
  id: number;
  incidentId: number;
  authorUserId: number;
  text: string;
  createdAt: string;
}

export interface IncidentCommentListResponse {
  items: IncidentComment[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface IncidentHistorySnapshot {
  status: IncidentStatus;
  eventType: IncidentEventType;
  location: string;
  detectedAt: string;
  description: string;
  attachmentFileIds: number[];
  alienId?: number | null;
  responsibleUserId?: number | null;
  executorUserIds: number[];
  cleanupStatus?: CleanupStatus | null;
  cleanupReportId?: number | null;
}

export interface IncidentHistoryEntry {
  id: number;
  changedAt: string;
  changedByUserId: number;
  snapshot: IncidentHistorySnapshot;
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

export interface SetIncidentResponsibleRequest {
  userId: number | null;
}

export interface SetIncidentExecutorsRequest {
  userIds: number[];
}

export interface IncidentHistoryListResponse {
  items: IncidentHistoryEntry[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
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
