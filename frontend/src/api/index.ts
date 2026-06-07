import { http } from './request'
import request from './request'
import type { AxiosProgressEvent, AxiosResponse } from 'axios'
import type {
  User,
  Tenant,
  DataSource,
  TableMetadata,
  DataModel,
  Dashboard,
  DashboardComponent,
  QueryRequest,
  QueryResult,
  QueryExecutionPlan,
  ScheduleConfig,
  ScheduleExecutionLog,
  EmbedToken,
  RowPermissionRule,
  PreAggregation,
  CsvPreviewResponse,
  CsvUploadResponse,
  ColumnDataType,
  DataLineageResponse,
  FileChunkUploadResponse
} from '@/types'

export const authApi = {
  login: (username: string, password: string, tenantCode: string) =>
    http.post<{ token: string; user: User }>('/auth/login', { username, password, tenantCode }),
  
  logout: () => http.post('/auth/logout'),
  
  getCurrentUser: () => http.get<User>('/auth/me'),
  
  changePassword: (oldPassword: string, newPassword: string) =>
    http.post('/auth/change-password', { oldPassword, newPassword })
}

export const tenantApi = {
  getList: (params?: any) => http.get<Tenant[]>('/tenants', params),
  
  getById: (id: string) => http.get<Tenant>(`/tenants/${id}`),
  
  create: (data: Partial<Tenant>) => http.post<Tenant>('/tenants', data),
  
  update: (id: string, data: Partial<Tenant>) => http.put<Tenant>(`/tenants/${id}`, data),
  
  delete: (id: string) => http.delete(`/tenants/${id}`)
}

export const userApi = {
  getList: (params?: any) => http.get<User[]>('/users', params),
  
  getById: (id: string) => http.get<User>(`/users/${id}`),
  
  create: (data: Partial<User> & { password: string }) => http.post<User>('/users', data),
  
  update: (id: string, data: Partial<User>) => http.put<User>(`/users/${id}`, data),
  
  delete: (id: string) => http.delete(`/users/${id}`),
  
  resetPassword: (id: string, newPassword: string) =>
    http.post(`/users/${id}/reset-password`, { newPassword })
}

export const dataSourceApi = {
  getList: (params?: any) => http.get<DataSource[]>('/data-sources', params),
  
  getById: (id: string) => http.get<DataSource>(`/data-sources/${id}`),
  
  create: (data: Partial<DataSource>) => http.post<DataSource>('/data-sources', data),
  
  update: (id: string, data: Partial<DataSource>) => http.put<DataSource>(`/data-sources/${id}`, data),
  
  delete: (id: string) => http.delete(`/data-sources/${id}`),
  
  testConnection: (data: Partial<DataSource>) =>
    http.post<{ success: boolean; message: string }>('/data-sources/test-connection', data),
  
  getTables: (id: string) => http.get<TableMetadata[]>(`/data-sources/${id}/tables`),
  
  getTablePreview: (id: string, tableName: string, limit = 100) =>
    http.get<QueryResult>(`/data-sources/${id}/tables/${tableName}/preview`, { limit }),
  
  syncMetadata: (id: string) => http.post(`/data-sources/${id}/sync-metadata`),
  
  uploadCsv: (id: string, file: File, onProgress?: (percent: number) => void) =>
    http.upload<CsvUploadResponse>(`/data-sources/${id}/upload-csv`, file, {}, onProgress),
  
  previewCsv: (file: File, limit = 20) =>
    http.upload<CsvPreviewResponse>('/data-sources/preview-csv-enhanced', file, { limit }),
  
  updateColumnTypes: (dataSourceId: string, tableId: string, columnTypes: Record<string, ColumnDataType>) =>
    http.post('/data-sources/update-column-types', { dataSourceId, tableId, columnTypes }),
  
  uploadChunk: (
    fileId: string,
    fileName: string,
    chunkNumber: number,
    totalChunks: number,
    chunkSize: number,
    totalSize: number,
    file: Blob,
    onProgress?: (percent: number) => void
  ): Promise<FileChunkUploadResponse> => {
    const formData = new FormData()
    formData.append('fileId', fileId)
    formData.append('fileName', fileName)
    formData.append('chunkNumber', String(chunkNumber))
    formData.append('totalChunks', String(totalChunks))
    formData.append('chunkSize', String(chunkSize))
    formData.append('totalSize', String(totalSize))
    formData.append('file', file)
    
    return request.post('/data-sources/upload-chunk', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (progressEvent: AxiosProgressEvent) => {
        if (onProgress && progressEvent.total) {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(percent)
        }
      }
    }).then((response: AxiosResponse) => response.data as FileChunkUploadResponse)
  },
  
  checkChunk: (fileId: string, chunkNumber: number) =>
    http.get<{ exists: boolean }>('/data-sources/check-chunk', { fileId, chunkNumber }),
  
  mergeChunks: (id: string, fileId: string, fileName: string, totalChunks: number, totalSize: number) =>
    http.post(`/data-sources/${id}/merge-chunks`, null, {
      params: { fileId, fileName, totalChunks, totalSize }
    }),
  
  configureRefresh: (dataSourceId: string, refreshInterval: string, refreshDirectory: string) =>
    http.post('/data-sources/configure-refresh', { dataSourceId, refreshInterval, refreshDirectory }),
  
  refreshNow: (id: string) =>
    http.post(`/data-sources/${id}/refresh-now`),
  
  getLineage: (id: string) =>
    http.get<DataLineageResponse>(`/data-sources/${id}/lineage`),
  
  getPoolStatus: (id: string) =>
    http.get<{ activeConnections: number; idleConnections: number; totalConnections: number }>(
      `/data-sources/${id}/pool-status`
    )
}

export const dataModelApi = {
  getList: (params?: any) => http.get<DataModel[]>('/datamodels', params),
  
  getById: (id: string) => http.get<DataModel>(`/datamodels/${id}`),
  
  create: (data: Partial<DataModel>) => http.post<DataModel>('/datamodels', data),
  
  update: (id: string, data: Partial<DataModel>) => http.put<DataModel>(`/datamodels/${id}`, data),
  
  delete: (id: string) => http.delete(`/datamodels/${id}`),
  
  publish: (id: string) => http.post<DataModel>(`/datamodels/${id}/publish`),
  
  unpublish: (id: string) => http.post<DataModel>(`/datamodels/${id}/unpublish`),
  
  validateExpression: (dataModelId: string, expression: string, returnType: string) =>
    http.post<{ valid: boolean; message?: string; sql?: string }>(
      `/datamodels/${dataModelId}/validate-expression`,
      { expression, returnType }
    ),
  
  getFields: (id: string) =>
    http.get<{
      dimensions: { id: string; name: string; displayName: string; type: string }[]
      measures: { id: string; name: string; displayName: string; type: string; aggregation: string }[]
      calculatedFields: { id: string; name: string; displayName: string; type: string }[]
    }>(`/datamodels/${id}/fields`)
}

export const dashboardApi = {
  getList: (params?: any) => http.get<Dashboard[]>('/dashboards', params),
  
  getById: (id: string) => http.get<Dashboard>(`/dashboards/${id}`),
  
  create: (data: Partial<Dashboard>) => http.post<Dashboard>('/dashboards', data),
  
  update: (id: string, data: Partial<Dashboard>) => http.put<Dashboard>(`/dashboards/${id}`, data),
  
  delete: (id: string) => http.delete(`/dashboards/${id}`),
  
  copy: (id: string, name: string) =>
    http.post<Dashboard>(`/dashboards/${id}/copy`, { name }),
  
  publish: (id: string) => http.post<Dashboard>(`/dashboards/${id}/publish`),
  
  unpublish: (id: string) => http.post<Dashboard>(`/dashboards/${id}/unpublish`),
  
  setAsTemplate: (id: string) => http.post<Dashboard>(`/dashboards/${id}/template`),
  
  getTemplates: () => http.get<Dashboard[]>('/dashboards/templates'),
  
  createFromTemplate: (templateId: string, name: string) =>
    http.post<Dashboard>(`/dashboards/template/${templateId}/create`, { name }),
  
  getComponentData: (dashboardId: string, componentId: string, filters?: any) =>
    http.post<QueryResult>(`/dashboards/${dashboardId}/components/${componentId}/data`, { filters }),
  
  exportPdf: (id: string) => http.download(`/dashboards/${id}/export/pdf`, undefined, `dashboard-${id}.pdf`),
  
  exportImage: (id: string) => http.download(`/dashboards/${id}/export/image`, undefined, `dashboard-${id}.png`)
}

export const queryApi = {
  execute: (request: QueryRequest) => http.post<QueryResult>('/query/execute', request),
  
  validate: (sql: string, dataSourceId?: string) =>
    http.post<{ valid: boolean; message?: string; type?: string }>('/query/validate', { sql, dataSourceId }),
  
  explain: (sql: string, dataSourceId?: string) =>
    http.post<QueryExecutionPlan[]>('/query/explain', { sql, dataSourceId }),
  
  getTables: (dataSourceId: string) =>
    http.get<{ name: string; columns: { name: string; type: string }[] }[]>(`/query/tables/${dataSourceId}`),
  
  clearCache: () => http.post('/query/cache/clear'),
  
  getCacheStats: () =>
    http.get<{
      cacheSize: number
      hitCount: number
      missCount: number
      hitRate: number
      evictionCount: number
    }>('/query/cache/stats')
}

export const scheduleApi = {
  getList: (params?: any) => http.get<ScheduleConfig[]>('/schedules', params),
  
  getById: (id: string) => http.get<ScheduleConfig>(`/schedules/${id}`),
  
  create: (data: Partial<ScheduleConfig>) => http.post<ScheduleConfig>('/schedules', data),
  
  update: (id: string, data: Partial<ScheduleConfig>) => http.put<ScheduleConfig>(`/schedules/${id}`, data),
  
  delete: (id: string) => http.delete(`/schedules/${id}`),
  
  enable: (id: string) => http.post<ScheduleConfig>(`/schedules/${id}/enable`),
  
  disable: (id: string) => http.post<ScheduleConfig>(`/schedules/${id}/disable`),
  
  executeNow: (id: string) => http.post<ScheduleExecutionLog>(`/schedules/${id}/execute`),
  
  getExecutionLogs: (id: string, limit = 50) =>
    http.get<ScheduleExecutionLog[]>(`/schedules/${id}/logs`, { limit }),
  
  pause: (id: string) => http.post<ScheduleConfig>(`/schedules/${id}/pause`),
  
  resume: (id: string) => http.post<ScheduleConfig>(`/schedules/${id}/resume`)
}

export const permissionApi = {
  getRowPermissions: () => http.get<RowPermissionRule[]>('/permissions/row-rules'),
  
  createRowPermission: (data: Partial<RowPermissionRule>) =>
    http.post<RowPermissionRule>('/permissions/row-rules', data),
  
  updateRowPermission: (id: string, data: Partial<RowPermissionRule>) =>
    http.put<RowPermissionRule>(`/permissions/row-rules/${id}`, data),
  
  deleteRowPermission: (id: string) => http.delete(`/permissions/row-rules/${id}`),
  
  getDashboardPermissions: (dashboardId: string) =>
    http.get<{ userId: string; userName: string; permission: string }[]>(
      `/permissions/dashboard/${dashboardId}`
    ),
  
  setDashboardPermission: (dashboardId: string, userId: string, permission: string) =>
    http.post(`/permissions/dashboard/${dashboardId}`, { userId, permission }),
  
  removeDashboardPermission: (dashboardId: string, userId: string) =>
    http.delete(`/permissions/dashboard/${dashboardId}/${userId}`),
  
  validateRule: (data: Partial<RowPermissionRule>) =>
    http.post<{ valid: boolean; message?: string }>('/permissions/row-rules/validate', data)
}

export const embedApi = {
  createToken: (data: {
    dashboardId: string
    name: string
    description?: string
    validitySeconds?: number
    hideTitle?: boolean
    hideToolbar?: boolean
    hideFilters?: boolean
    hideTabs?: boolean
    enableFullscreen?: boolean
    enableExport?: boolean
    enableDrilldown?: boolean
    enableFilterInteraction?: boolean
    defaultFilters?: Record<string, any>
    defaultTabId?: string
    rowPermissionRules?: any[]
    allowedDomains?: string[]
    maxUses?: number
    theme?: string
    locale?: string
    iframeWidth?: string
    iframeHeight?: string
    borderStyle?: string
    customCss?: string
  }) => http.post<EmbedToken>('/embed/token', data),
  
  getToken: (id: string) => http.get<EmbedToken>(`/embed/token/${id}`),
  
  getTokensByDashboard: (dashboardId: string) =>
    http.get<EmbedToken[]>(`/embed/dashboard/${dashboardId}`),
  
  getTokens: () => http.get<EmbedToken[]>('/embed/token'),
  
  revokeToken: (id: string) => http.post(`/embed/token/${id}/revoke`),
  
  deleteToken: (id: string) => http.delete(`/embed/token/${id}`),
  
  getEmbedCode: (id: string) => http.get<{ code: string }>(`/embed/token/${id}/code`),
  
  getEmbedUrl: (id: string) => http.get<{ url: string }>(`/embed/token/${id}/url`),
  
  getEmbedConfig: (id: string) =>
    http.get<Record<string, any>>(`/embed/token/${id}/config`),
  
  getTokenStats: (id: string) =>
    http.get<Record<string, any>>(`/embed/token/${id}/stats`),
  
  validateToken: (token: string) =>
    http.post<{ valid: boolean; config?: Record<string, any>; message?: string }>(
      '/embed/token/validate',
      { token }
    )
}

export const preAggregationApi = {
  getList: (params?: any) => http.get<PreAggregation[]>('/pre-aggregations', params),
  
  getById: (id: string) => http.get<PreAggregation>(`/pre-aggregations/${id}`),
  
  create: (data: Partial<PreAggregation>) => http.post<PreAggregation>('/pre-aggregations', data),
  
  update: (id: string, data: Partial<PreAggregation>) =>
    http.put<PreAggregation>(`/pre-aggregations/${id}`, data),
  
  delete: (id: string) => http.delete(`/pre-aggregations/${id}`),
  
  execute: (id: string) => http.post(`/pre-aggregations/${id}/execute`),
  
  enable: (id: string) => http.post<PreAggregation>(`/pre-aggregations/${id}/enable`),
  
  disable: (id: string) => http.post<PreAggregation>(`/pre-aggregations/${id}/disable`)
}

export default {
  auth: authApi,
  tenant: tenantApi,
  user: userApi,
  dataSource: dataSourceApi,
  dataModel: dataModelApi,
  dashboard: dashboardApi,
  query: queryApi,
  schedule: scheduleApi,
  permission: permissionApi,
  embed: embedApi,
  preAggregation: preAggregationApi
}
