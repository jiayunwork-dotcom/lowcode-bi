export interface User {
  id: string
  username: string
  email: string
  role: 'ADMIN' | 'EDITOR' | 'VIEWER'
  tenantId: string
  tenantName?: string
  status: 'ACTIVE' | 'INACTIVE'
  avatar?: string
  createdAt: string
  updatedAt: string
}

export interface Tenant {
  id: string
  name: string
  code: string
  status: 'ACTIVE' | 'INACTIVE'
  maxUsers: number
  maxDataSources: number
  maxDashboards: number
  createdAt: string
}

export interface DataSource {
  id: string
  name: string
  type: 'MYSQL' | 'POSTGRESQL' | 'CLICKHOUSE' | 'CSV'
  host: string
  port: number
  database: string
  username: string
  password?: string
  connectionPoolSize: number
  connectionTimeout: number
  queryTimeout: number
  isActive: boolean
  tenantId: string
  createdAt: string
  updatedAt: string
  tables?: TableMetadata[]
  csvFilePath?: string
  csvFileName?: string
  csvFileSize?: number
  csvRefreshInterval?: 'OFF' | 'MANUAL' | 'THIRTY_SECONDS' | 'ONE_MINUTE' | 'FIVE_MINUTES' | 'TEN_MINUTES' | 'THIRTY_MINUTES' | 'ONE_HOUR' | 'ONE_DAY'
  csvRefreshDirectory?: string
  csvLastImportTime?: string
  csvLastRefreshStatus?: 'SUCCESS' | 'FAILED' | 'REFRESHING'
  csvLastRefreshError?: string
  csvRefreshInProgress?: boolean
}

export interface TableMetadata {
  id: string
  name: string
  displayName: string
  description: string
  rowCount: number
  columns: ColumnMetadata[]
  isEnabled: boolean
}

export interface ColumnMetadata {
  id: string
  name: string
  displayName: string
  dataType: 'STRING' | 'INTEGER' | 'DECIMAL' | 'BOOLEAN' | 'DATE' | 'DATETIME' | 'TEXT'
  isNullable: boolean
  isPrimaryKey: boolean
  isForeignKey: boolean
  referencedTable?: string
  referencedColumn?: string
  precision?: number
  scale?: number
}

export interface DataModel {
  id: string
  name: string
  description: string
  dataSourceId: string
  dataSourceName?: string
  tables: ModelTable[]
  relations: ModelRelation[]
  calculatedFields: CalculatedField[]
  measures: Measure[]
  dimensionHierarchies: DimensionHierarchy[]
  isPublished: boolean
  createdAt: string
  updatedAt: string
}

export interface ModelTable {
  id: string
  tableMetadataId: string
  tableName: string
  alias: string
  isEnabled: boolean
  positionX: number
  positionY: number
}

export interface ModelRelation {
  id: string
  sourceTableId: string
  targetTableId: string
  sourceColumn: string
  targetColumn: string
  relationType: 'ONE_TO_ONE' | 'ONE_TO_MANY' | 'MANY_TO_MANY'
  joinType: 'INNER' | 'LEFT' | 'RIGHT' | 'FULL'
}

export interface CalculatedField {
  id: string
  name: string
  expression: string
  returnType: 'STRING' | 'INTEGER' | 'DECIMAL' | 'BOOLEAN' | 'DATE' | 'DATETIME'
  description: string
  isActive: boolean
}

export interface Measure {
  id: string
  name: string
  columnName: string
  aggregationType: 'SUM' | 'AVG' | 'COUNT' | 'COUNTDISTINCT' | 'MAX' | 'MIN'
  filterCondition?: string
  description: string
  format?: string
  isActive: boolean
}

export interface DimensionHierarchy {
  id: string
  name: string
  levels: HierarchyLevel[]
  isActive: boolean
}

export interface HierarchyLevel {
  id: string
  name: string
  columnName: string
  levelOrder: number
}

export interface Dashboard {
  id: string
  name: string
  description: string
  tabs: DashboardTab[]
  isPublished: boolean
  isTemplate: boolean
  theme: 'LIGHT' | 'DARK'
  autoRefreshInterval: number
  refreshIntervalUnit: 'SECONDS' | 'MINUTES'
  createdAt: string
  updatedAt: string
  createdByName?: string
  lastPublishedAt?: string
}

export interface DashboardTab {
  id: string
  name: string
  orderIndex: number
  components: DashboardComponent[]
}

export interface DashboardComponent {
  id: string
  type: 'LINE' | 'BAR' | 'PIE' | 'SCATTER' | 'TABLE' | 'KPI' | 'FUNNEL' | 'MAP'
  title: string
  dataModelId: string
  x: number
  y: number
  width: number
  height: number
  config: ComponentConfig
  filters: ComponentFilter[]
  linkedComponents: string[]
  _tab?: { components: DashboardComponent[] }
}

export interface ComponentConfig {
  dimensions: string[]
  measures: string[]
  chartSubType?: string
  showLegend?: boolean
  showTooltip?: boolean
  stacked?: boolean
  areaStyle?: boolean
  colorPalette?: string[]
  sortField?: string
  sortOrder?: 'ASC' | 'DESC'
  limit?: number
  tableConfig?: TableConfig
  kpiConfig?: KpiConfig
  mapConfig?: MapConfig
}

export interface TableConfig {
  showRowNumbers?: boolean
  showSummary?: boolean
  conditionalFormats?: ConditionalFormat[]
  pageSize?: number
}

export interface ConditionalFormat {
  field: string
  condition: 'EQ' | 'NE' | 'GT' | 'GTE' | 'LT' | 'LTE' | 'BETWEEN' | 'CONTAINS'
  value1: string | number
  value2?: string | number
  style: {
    backgroundColor?: string
    textColor?: string
    fontWeight?: 'normal' | 'bold'
  }
}

export interface KpiConfig {
  compareField?: string
  compareType?: 'YEAR_OVER_YEAR' | 'QUARTER_OVER_QUARTER' | 'MONTH_OVER_MONTH' | 'DAY_OVER_DAY'
  showTrend?: boolean
  prefix?: string
  suffix?: string
  decimals?: number
}

export interface MapConfig {
  mapType?: 'CHINA_PROVINCE' | 'CHINA_CITY' | 'WORLD'
  valueField?: string
  nameField?: string
}

export interface ComponentFilter {
  id: string
  field: string
  filterType: 'EQUAL' | 'NOT_EQUAL' | 'GREATER_THAN' | 'LESS_THAN' | 'BETWEEN' | 'IN' | 'LIKE' | 'IS_NULL' | 'IS_NOT_NULL'
  value1?: string | number
  value2?: string | number
  values?: (string | number)[]
  isGlobal: boolean
}

export interface DashboardPermission {
  id?: string
  dashboardId?: string
  userId: string
  userName?: string
  permission: 'VIEW' | 'EDIT' | 'OWNER'
  createdAt?: string
}

export interface ScheduleConfig {
  id: string
  name: string
  dashboardId: string
  dashboardName?: string
  cronExpression: string
  timezone: string
  emailSubject: string
  emailBody: string
  recipients: string[]
  isEnabled: boolean
  lastExecutionAt?: string
  lastExecutionStatus?: 'SUCCESS' | 'FAILED' | 'TIMEOUT'
  consecutiveFailures: number
  isPaused: boolean
  createdAt: string
}

export interface ScheduleExecutionLog {
  id: string
  scheduleId: string
  executedAt: string
  status: 'SUCCESS' | 'FAILED' | 'TIMEOUT'
  errorMessage?: string
  screenshotUrl?: string
  durationMs: number
}

export interface QueryRequest {
  sql: string
  dataSourceId?: string
  parameters?: Record<string, any>
  timeout?: number
  useCache?: boolean
  cacheExpirySeconds?: number
}

export interface QueryResult {
  columns: QueryColumn[]
  rows: any[][]
  totalRows: number
  executionTimeMs: number
  isCached: boolean
  cacheKey?: string
}

export interface QueryColumn {
  name: string
  displayName: string
  dataType: string
}

export interface QueryExecutionPlan {
  step: number
  operation: string
  tableName?: string
  filterCondition?: string
  estimatedRows: number
  cost: number
  children?: QueryExecutionPlan[]
}

export interface EmbedToken {
  id: string
  name: string
  description: string
  dashboardId: string
  dashboardName?: string
  token: string
  tokenType: string
  expiresAt: string
  validitySeconds: number
  isActive: boolean
  hideTitle: boolean
  hideToolbar: boolean
  hideFilters: boolean
  hideTabs: boolean
  enableFullscreen: boolean
  enableExport: boolean
  enableDrilldown: boolean
  enableFilterInteraction: boolean
  defaultTabId?: string
  maxUses?: number
  currentUses: number
  theme: string
  locale: string
  iframeWidth: string
  iframeHeight: string
  allowedDomains?: string
  defaultFilters?: string
  rowPermissionRules?: string
  lastUsedAt?: string
  lastUsedIp?: string
  createdAt: string
}

export interface RowPermissionRule {
  id: string
  name: string
  description: string
  tableName: string
  columnName: string
  operator: 'EQ' | 'NE' | 'GT' | 'GTE' | 'LT' | 'LTE' | 'IN' | 'LIKE'
  value: string
  valueType: 'STATIC' | 'USER_ATTRIBUTE' | 'EXPRESSION'
  isEnabled: boolean
  userRoles: string[]
}

export interface PreAggregation {
  id: string
  name: string
  description: string
  dataModelId: string
  dimensions: string[]
  measures: string[]
  filterCondition?: string
  scheduleCron: string
  targetTableName: string
  lastRunAt?: string
  lastRunStatus?: 'SUCCESS' | 'FAILED'
  isEnabled: boolean
  createdAt: string
}

export interface DrillDownContext {
  dimension: string
  value: any
  hierarchy: string
  currentLevel: number
}

export interface FilterValue {
  field: string
  filterType: string
  value: any
}

export interface ChartDataPoint {
  name: string
  value: number
  [key: string]: any
}

export interface ChartSeries {
  name: string
  type: string
  data: ChartDataPoint[]
}

export interface CsvPreviewResponse {
  headers: string[]
  columnTypes: ColumnDataType[]
  rows: Record<string, any>[]
  charset: string
  rowCount: number
  columnCount: number
  fileName: string
  fileSize: number
  tableId?: string
}

export type ColumnDataType = 'STRING' | 'INTEGER' | 'LONG' | 'DOUBLE' | 'DECIMAL' | 'BOOLEAN' | 'DATE' | 'DATETIME' | 'TIMESTAMP' | 'TEXT' | 'BINARY' | 'JSON'

export interface DataLineageResponse {
  dataSource: DataSourceNode
  dataModels: DataModelNode[]
  dashboards: DashboardNode[]
}

export interface DataSourceNode {
  id: string
  name: string
  type: string
}

export interface DataModelNode {
  id: string
  name: string
  description: string
  dataSourceId: string
  tableCount: number
  measureCount: number
  dimensionCount: number
}

export interface DashboardNode {
  id: string
  name: string
  description: string
  dataModelId: string
  componentCount: number
  isPublished: boolean
}

export interface CsvUploadResponse {
  success: boolean
  tableId?: string
  dataSource?: DataSource
  message?: string
}

export interface FileChunkUploadResponse {
  fileId: string
  chunkNumber: number
  success: boolean
  completed: boolean
  message: string
  uploadedFilePath?: string
}

export type AlertTriggerType = 'VALUE' | 'RELATIVE_CHANGE'
export type AlertOperator =
  | 'GREATER_THAN'
  | 'LESS_THAN'
  | 'EQUAL'
  | 'NOT_EQUAL'
  | 'GREATER_THAN_OR_EQUAL'
  | 'LESS_THAN_OR_EQUAL'
  | 'INCREASE_PERCENT'
  | 'DECREASE_PERCENT'
  | 'CHANGE_PERCENT'
export type AlertCheckInterval =
  | 'EVERY_5_MINUTES'
  | 'EVERY_15_MINUTES'
  | 'EVERY_30_MINUTES'
  | 'EVERY_HOUR'
  | 'EVERY_6_HOURS'
  | 'EVERY_12_HOURS'
  | 'EVERY_DAY'
export type AlertSeverity = 'INFO' | 'WARNING' | 'CRITICAL'
export type AlertRuleStatus = 'ACTIVE' | 'INVALID' | 'DISABLED'
export type AlertEventStatus = 'FIRING' | 'RESOLVED' | 'ACKNOWLEDGED'
export type NotificationChannelType = 'IN_APP' | 'EMAIL' | 'WEBHOOK'
export type NotificationStatus = 'PENDING' | 'SENT' | 'FAILED' | 'RETRYING'
export type SystemMessageType = 'ALERT' | 'NOTIFICATION' | 'SYSTEM'

export interface AlertRule {
  id: string
  name: string
  description: string
  dataModelId: string
  dataModelName?: string
  measureId: string
  measureName: string
  triggerType: AlertTriggerType
  operator: AlertOperator
  threshold: number
  checkInterval: AlertCheckInterval
  silencePeriod: number
  severity: AlertSeverity
  isEnabled: boolean
  status: AlertRuleStatus
  lastTriggeredAt?: string
  lastCheckedAt?: string
  createdBy: string
  createdAt: string
  notificationChannels?: NotificationChannel[]
  eventCount?: number
  subscriberCount?: number
  escalationEnabled?: boolean
  escalationThreshold?: number
  consecutiveTriggerCount?: number
  escalationLevel?: number
  currentSeverity?: AlertSeverity
  escalationRecipients?: EscalationRecipient[]
}

export interface EscalationRecipient {
  userId: string
  username: string
  email: string
}

export interface SeverityDistribution {
  infoCount: number
  warningCount: number
  criticalCount: number
  infoPercent: number
  warningPercent: number
  criticalPercent: number
}

export interface AlertEventTimelineItem {
  eventId: string
  triggeredAt: string
  resolvedAt?: string
  severity: AlertSeverity
  isRecovered: boolean
  triggerValue: string
}

export interface AlertEventGroup {
  ruleId: string
  ruleName: string
  dataModelName: string
  dataModelId: string
  activeEventCount: number
  totalEventCount: number
  lastTriggeredAt?: string
  averageRecoveryMinutes?: number
  severityDistribution: SeverityDistribution
  timeline?: AlertEventTimelineItem[]
}

export interface NotificationChannel {
  id?: string
  type: NotificationChannelType
  config: Record<string, any>
  enabled?: boolean
}

export interface AlertEvent {
  id: string
  alertRuleId: string
  alertRuleName: string
  measureName: string
  triggerValue: number
  threshold: number
  previousValue?: number
  changePercent?: number
  severity: AlertSeverity
  eventStatus: AlertEventStatus
  triggeredAt: string
  resolvedAt?: string
  acknowledgedAt?: string
  acknowledgedBy?: string
  isRecovered: boolean
  recoveryValue?: number
  trendData?: { time: string; value: number }[]
  triggerHighlightTime?: string
}

export interface AlertStatistics {
  activeAlertCount: number
  todayNewCount: number
  topTriggeredRules: {
    ruleId: string
    ruleName: string
    triggerCount: number
  }[]
  averageRecoveryMinutes?: number
}

export interface SystemMessage {
  id: string
  messageType: SystemMessageType
  title: string
  content?: string
  relatedType?: string
  relatedId?: string
  isRead: boolean
  readAt?: string
  createdAt: string
  unreadCount?: number
}
