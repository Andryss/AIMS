export type IncidentEventType =
  | 'UNIDENTIFIED_SIGHTING'
  | 'CONTACT_SUSPECT'
  | 'ILLEGAL_UFO_LANDING'
  | 'MEMORY_ANOMALY'
  | 'ALIEN_ARTIFACT'
  | 'ALIEN_CAPTURE'
  | 'ATTACK_ON_PEOPLE'
  | 'PROPERTY_DAMAGE'
  | 'BIOLOGICAL_CONTAMINATION'
  | 'CRITICAL_INFRASTRUCTURE_DISRUPTION';

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

export interface CreateIncidentRequest {
  eventType: IncidentEventType;
  location: string;
  detectedAt: string;
  description: string;
  victimCount?: number | null;
  attachmentFileIds: number[];
  monitoringAlertId?: number;
}

export type MonitoringAlertStatus = 'NEW' | 'INCIDENT_CREATED';

export interface MonitoringAlert {
  id: number;
  externalEventId: string;
  sourceSystem: string;
  status: MonitoringAlertStatus;
  eventType: IncidentEventType;
  location: string;
  detectedAt: string;
  description: string;
  mediaUrls: string[];
  incidentId?: number | null;
  receivedAt: string;
  createdAt: string;
}

export interface MonitoringAlertListResponse {
  items: MonitoringAlert[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateIncidentInitialValues {
  eventType?: IncidentEventType;
  location?: string;
  detectedAt?: string;
  description?: string;
  monitoringAlertId?: number;
  mediaUrls?: string[];
}

export interface IncidentResponse {
  id: number;
  status: IncidentStatus;
  eventType: IncidentEventType;
  location: string;
  detectedAt: string;
  description: string;
  victimCount?: number | null;
  threatLevel?: number | null;
  attachmentFileIds: number[];
  alienId?: number;
  responsibleUserId?: number | null;
  executorUserIds: number[];
  cleanupStatus?: CleanupStatus | null;
  cleanupReportId?: number | null;
  monitoringAlertId?: number | null;
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
  victimCount?: number | null;
  threatLevel?: number | null;
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
  photoFileIds?: number[];
  id: number;
  name: string;
  description: string;
  threatLevel: number;
}

export interface AlienSearchResponse {
  items: Alien[];
}

export interface SaveAlienRequest {
  photoFileIds?: number[];
  name: string;
  description: string;
  threatLevel: number;
}

export interface AlienListResponse {
  items: Alien[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type TenderStatus = 'NEED' | 'COLLECTING_OFFERS' | 'AWAITING_DELIVERY' | 'COMPLETED' | 'CANCELLED';
export type TenderParticipantStatus = 'PARTICIPATING' | 'WINNER';

export interface SaveTenderRequest {
  equipmentType: EquipmentType;
  quantity: number;
  conditions?: string | null;
  incidentId?: number | null;
}

export interface TenderResponse extends SaveTenderRequest {
  id: number;
  status: TenderStatus;
  createdByUserId: number;
  winnerParticipantId?: number | null;
  createdAt: string;
  updatedAt: string;
  completedAt?: string | null;
}

export interface TenderListQuery {
  q?: string;
  status?: TenderStatus;
  equipmentType?: EquipmentType;
  incidentId?: number;
  page: number;
  size: number;
}

export interface TenderListResponse {
  items: TenderResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ChangeTenderStatusRequest {
  status: TenderStatus;
  comment?: string;
}

export interface SaveTenderParticipantRequest {
  name: string;
  proposal: string;
}

export interface TenderParticipantResponse extends SaveTenderParticipantRequest {
  id: number;
  tenderId: number;
  status: TenderParticipantStatus;
  createdByUserId: number;
  createdAt: string;
  updatedAt: string;
}

export interface TenderParticipantListResponse {
  items: TenderParticipantResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface SelectTenderWinnerRequest { participantId: number; }

export interface ReceiveTenderRequest {
  equipment: Array<{ model: string; serialNumber: string }>;
  comment?: string;
}

export interface TenderReceiptResponse {
  tender: TenderResponse;
  equipment: EquipmentResponse[];
}

export interface TenderHistorySnapshot {
  tender: TenderResponse;
  participants: TenderParticipantResponse[];
  transitionComment?: string | null;
  receivedEquipmentIds: number[];
}

export interface TenderHistoryEntry {
  id: number;
  changedAt: string;
  changedByUserId: number;
  snapshot: TenderHistorySnapshot;
}

export interface TenderHistoryListResponse {
  items: TenderHistoryEntry[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type EquipmentType = 'NEURALYZER' | 'NEUTRALIZER';
export type EquipmentStatus = 'AVAILABLE' | 'ISSUED';
export type EquipmentMovementOperation = 'ISSUE' | 'RETURN';
export type EquipmentMovementStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'CANCELLED' | 'REJECTED';

export interface SaveEquipmentRequest {
  type: EquipmentType;
  model: string;
  serialNumber: string;
}

export interface EquipmentResponse extends SaveEquipmentRequest {
  id: number;
  status: EquipmentStatus;
  holderUserId?: number | null;
  currentIssueMovementId?: number | null;
  sourceTenderId?: number | null;
  createdAt: string;
}

export interface EquipmentListQuery {
  q?: string;
  type?: EquipmentType;
  status?: EquipmentStatus;
  holderUserId?: number;
  page: number;
  size: number;
}

export interface EquipmentListResponse {
  items: EquipmentResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface EquipmentHistoryEntry {
  id: number;
  changedAt: string;
  changedByUserId: number;
  snapshot: {
    event: 'REGISTERED' | 'RECEIVED' | 'UPDATED' | 'ISSUED' | 'RETURNED';
    movementId?: number | null;
    equipment: EquipmentResponse;
  };
}

export interface EquipmentHistoryListResponse {
  items: EquipmentHistoryEntry[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface SaveEquipmentMovementRequest {
  operation: EquipmentMovementOperation;
  incidentId: number;
  responsibleUserId: number;
  sourceIssueMovementId?: number | null;
  equipmentIds: number[];
  comment?: string;
}

export interface EquipmentMovementResponse {
  id: number;
  operation: EquipmentMovementOperation;
  status: EquipmentMovementStatus;
  incidentId: number;
  responsibleUserId: number;
  sourceIssueMovementId?: number | null;
  createdByUserId: number;
  comment?: string | null;
  createdAt: string;
  completedAt?: string | null;
  equipment: EquipmentResponse[];
}

export interface EquipmentMovementListQuery {
  incidentId?: number;
  responsibleUserId?: number;
  status?: EquipmentMovementStatus;
  operation?: EquipmentMovementOperation;
  equipmentId?: number;
  page: number;
  size: number;
}

export interface EquipmentMovementListResponse {
  items: EquipmentMovementResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ChangeEquipmentMovementStatusRequest {
  status: EquipmentMovementStatus;
  comment?: string;
}

export interface EquipmentMovementHistoryEntry {
  id: number;
  changedAt: string;
  changedByUserId: number;
  snapshot: {
    movement: EquipmentMovementResponse;
    transitionComment?: string | null;
  };
}

export interface EquipmentMovementHistoryListResponse {
  items: EquipmentMovementHistoryEntry[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type DetentionRequestStatus = 'NEW' | 'ACCEPTED' | 'IN_CUSTODY' | 'RELEASED' | 'REJECTED' | 'CANCELLED';

export interface DetentionRequestResponse {
  id: number;
  incidentId: number;
  subjectName: string;
  subjectDescription: string;
  alienId?: number | null;
  durationDays: number;
  status: DetentionRequestStatus;
  createdByUserId: number;
  reviewedByUserId?: number | null;
  createdAt: string;
  updatedAt: string;
  admittedAt?: string | null;
  plannedReleaseAt?: string | null;
  releasedAt?: string | null;
}

export interface SaveDetentionRequest {
  incidentId: number;
  subjectName: string;
  subjectDescription: string;
  alienId?: number | null;
  durationDays: number;
  comment?: string;
}

export interface ChangeDetentionRequestStatusRequest {
  status: DetentionRequestStatus;
  comment?: string;
}

export interface DetentionRequestListQuery {
  q?: string;
  incidentId?: number;
  status?: DetentionRequestStatus;
  page: number;
  size: number;
}

export interface DetentionRequestListResponse {
  items: DetentionRequestResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface DetentionRequestHistorySnapshot {
  request: DetentionRequestResponse;
  transitionComment?: string | null;
}

export interface DetentionRequestHistoryEntry {
  id: number;
  changedAt: string;
  changedByUserId: number;
  snapshot: DetentionRequestHistorySnapshot;
}

export interface DetentionRequestHistoryListResponse {
  items: DetentionRequestHistoryEntry[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
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


export interface AnalyticsPeriodQuery {
  from: string;
  to: string;
}

export interface IncidentAnalyticsQuery extends AnalyticsPeriodQuery {
  eventType?: IncidentEventType;
  alienId?: number;
  responsibleUserId?: number;
}

export interface EquipmentAnalyticsQuery extends AnalyticsPeriodQuery {
  equipmentType?: EquipmentType;
  responsibleUserId?: number;
  incidentId?: number;
}

export interface DetentionAnalyticsQuery extends AnalyticsPeriodQuery {
  alienId?: number;
  status?: DetentionRequestStatus;
  incidentId?: number;
}

export type AnalyticsBucketUnit = 'HOUR' | 'DAY' | 'MONTH';

export interface AnalyticsMeta {
  from: string;
  to: string;
  generatedAt: string;
  bucketUnit: AnalyticsBucketUnit;
  bucketStep: number;
  recordsWithoutSnapshot: number;
}

export interface DurationPercentiles {
  sampleCount: number;
  excludedSampleCount: number;
  p50Seconds?: number | null;
  p90Seconds?: number | null;
  p95Seconds?: number | null;
  p99Seconds?: number | null;
}

export interface AnalyticsCountGroup {
  key: string;
  count: number;
  name?: string;
}

export interface AnalyticsTrendBucket {
  from: string;
  to: string;
  values: Record<string, number>;
}

export interface StaleIncidentAnalyticsRow {
  incidentId: number;
  location: string;
  status: IncidentStatus;
  responsibleUserId?: number;
  lastChangedAt: string;
}

export interface EquipmentInventoryAnalyticsRow {
  equipmentType: EquipmentType;
  available: number;
  issued: number;
  total: number;
  issuedShare?: number | null;
}

export interface OpenEquipmentRequestAnalyticsRow {
  equipmentType: EquipmentType;
  operation: EquipmentMovementOperation;
  requestCount: number;
  unitCount: number;
}

export interface ExpectedDeliveryAnalyticsRow {
  equipmentType: EquipmentType;
  tenderCount: number;
  unitCount: number;
}

export interface OutstandingEquipmentAnalyticsRow {
  incidentId: number;
  location: string;
  responsibleUserId: number;
  unitsByType: Record<string, number>;
}

export interface UpcomingReleaseAnalyticsRow {
  requestId: number;
  subjectName: string;
  alienId?: number;
  alienName?: string;
  incidentId: number;
  plannedReleaseAt: string;
}

export interface CountAnalyticsResponse {
  meta: AnalyticsMeta;
  count: number;
}

export interface GroupedAnalyticsResponse {
  meta: AnalyticsMeta;
  groups: AnalyticsCountGroup[];
}

export interface TrendAnalyticsResponse {
  meta: AnalyticsMeta;
  buckets: AnalyticsTrendBucket[];
}

export interface DurationAnalyticsResponse {
  meta: AnalyticsMeta;
  percentiles: DurationPercentiles;
}

export interface StaleIncidentAnalyticsResponse {
  meta: AnalyticsMeta;
  totalCount: number;
  items: StaleIncidentAnalyticsRow[];
}

export interface EquipmentInventoryAnalyticsResponse {
  meta: AnalyticsMeta;
  items: EquipmentInventoryAnalyticsRow[];
}

export interface OpenEquipmentRequestsAnalyticsResponse {
  meta: AnalyticsMeta;
  totalCount: number;
  items: OpenEquipmentRequestAnalyticsRow[];
}

export interface ExpectedDeliveriesAnalyticsResponse {
  meta: AnalyticsMeta;
  items: ExpectedDeliveryAnalyticsRow[];
}

export interface OutstandingEquipmentAnalyticsResponse {
  meta: AnalyticsMeta;
  totalCount: number;
  items: OutstandingEquipmentAnalyticsRow[];
}

export interface UpcomingReleasesAnalyticsResponse {
  meta: AnalyticsMeta;
  totalCount: number;
  items: UpcomingReleaseAnalyticsRow[];
}

export interface IncidentAlienClassificationResponse {
  status: 'MATCH' | 'NO_MATCH';
  confidence: number;
  alien: Alien | null;
}

export interface IncidentThreatCalculationResponse {
  calculatedThreatLevel: number;
  contributions: {
    criterion: 'ALIEN_THREAT' | 'DEFAULT_THREAT' | 'EVENT_TYPE' | 'VICTIMS';
    adjustment: number;
  }[];
}
