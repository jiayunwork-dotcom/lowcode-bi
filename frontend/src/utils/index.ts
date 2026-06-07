import dayjs from 'dayjs'

export const formatNumber = (value: number, decimals = 2, prefix = '', suffix = ''): string => {
  if (value === null || value === undefined || isNaN(value)) return '-'
  const formatted = value.toLocaleString('zh-CN', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals
  })
  return `${prefix}${formatted}${suffix}`
}

export const formatDate = (value: string | Date, format = 'YYYY-MM-DD HH:mm:ss'): string => {
  if (!value) return '-'
  return dayjs(value).format(format)
}

export const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

export const formatDuration = (ms: number): string => {
  if (ms < 1000) return `${ms}ms`
  if (ms < 60000) return `${(ms / 1000).toFixed(2)}s`
  const minutes = Math.floor(ms / 60000)
  const seconds = ((ms % 60000) / 1000).toFixed(0)
  return `${minutes}m ${seconds}s`
}

export const getColumnTypeIcon = (type: string): string => {
  const typeMap: Record<string, string> = {
    STRING: '📝',
    INTEGER: '🔢',
    DECIMAL: '💯',
    BOOLEAN: '✓',
    DATE: '📅',
    DATETIME: '⏰',
    TEXT: '📄'
  }
  return typeMap[type] || '❓'
}

export const getChartTypeIcon = (type: string): string => {
  const typeMap: Record<string, string> = {
    LINE: '📈',
    BAR: '📊',
    PIE: '🥧',
    SCATTER: '⚬',
    TABLE: '📋',
    KPI: '🎯',
    FUNNEL: '🔻',
    MAP: '🗺️'
  }
  return typeMap[type] || '📊'
}

export const getChartTypeName = (type: string): string => {
  const typeMap: Record<string, string> = {
    LINE: '折线图',
    BAR: '柱状图',
    PIE: '饼图',
    SCATTER: '散点图',
    TABLE: '表格',
    KPI: '指标卡',
    FUNNEL: '漏斗图',
    MAP: '地图'
  }
  return typeMap[type] || type
}

export const getRoleName = (role: string): string => {
  const roleMap: Record<string, string> = {
    ADMIN: '管理员',
    EDITOR: '编辑者',
    VIEWER: '查看者'
  }
  return roleMap[role] || role
}

export const getStatusColor = (status: string): string => {
  const colorMap: Record<string, string> = {
    ACTIVE: 'success',
    INACTIVE: 'default',
    SUCCESS: 'success',
    FAILED: 'error',
    TIMEOUT: 'warning',
    PUBLISHED: 'success',
    DRAFT: 'default'
  }
  return colorMap[status] || 'default'
}

export const getStatusText = (status: string): string => {
  const textMap: Record<string, string> = {
    ACTIVE: '启用',
    INACTIVE: '禁用',
    SUCCESS: '成功',
    FAILED: '失败',
    TIMEOUT: '超时',
    PUBLISHED: '已发布',
    DRAFT: '草稿'
  }
  return textMap[status] || status
}

export const generateId = (): string => {
  return 'id_' + Math.random().toString(36).substr(2, 9) + '_' + Date.now().toString(36)
}

export const debounce = <T extends (...args: any[]) => any>(
  fn: T,
  delay: number
): ((...args: Parameters<T>) => void) => {
  let timer: NodeJS.Timeout | null = null
  return (...args: Parameters<T>) => {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => fn(...args), delay)
  }
}

export const throttle = <T extends (...args: any[]) => any>(
  fn: T,
  delay: number
): ((...args: Parameters<T>) => void) => {
  let lastTime = 0
  return (...args: Parameters<T>) => {
    const now = Date.now()
    if (now - lastTime >= delay) {
      lastTime = now
      fn(...args)
    }
  }
}

export const deepClone = <T>(obj: T): T => {
  if (obj === null || typeof obj !== 'object') return obj
  if (obj instanceof Date) return new Date(obj.getTime()) as any
  if (obj instanceof Array) return obj.map(item => deepClone(item)) as any
  const cloned = {} as T
  for (const key in obj) {
    if (Object.prototype.hasOwnProperty.call(obj, key)) {
      cloned[key] = deepClone(obj[key])
    }
  }
  return cloned
}

export const getTrendIcon = (value: number, isPositiveGood = true): { icon: string; color: string } => {
  if (value > 0) {
    return {
      icon: isPositiveGood ? '↑' : '↓',
      color: isPositiveGood ? '#52c41a' : '#ff4d4f'
    }
  } else if (value < 0) {
    return {
      icon: isPositiveGood ? '↓' : '↑',
      color: isPositiveGood ? '#ff4d4f' : '#52c41a'
    }
  }
  return { icon: '→', color: '#8c8c8c' }
}

export const formatTrend = (value: number, decimals = 2): string => {
  const prefix = value > 0 ? '+' : ''
  return `${prefix}${value.toFixed(decimals)}%`
}

export const parseCronExpression = (cron: string): string => {
  const parts = cron.trim().split(/\s+/)
  if (parts.length !== 6) return cron

  const [second, minute, hour, dayOfMonth, month, dayOfWeek] = parts

  let description = ''

  if (second === '*' && minute === '*' && hour === '*' && dayOfMonth === '*' && month === '*' && dayOfWeek === '*') {
    description = '每秒执行'
  } else if (second === '0' && minute !== '*' && hour === '*' && dayOfMonth === '*' && month === '*' && dayOfWeek === '*') {
    description = `每小时第 ${minute} 分钟执行`
  } else if (second === '0' && minute === '0' && hour !== '*' && dayOfMonth === '*' && month === '*' && dayOfWeek === '*') {
    description = `每天 ${hour} 点执行`
  } else if (second === '0' && minute === '0' && hour === '0' && dayOfMonth !== '*' && month === '*' && dayOfWeek === '*') {
    description = `每月 ${dayOfMonth} 日执行`
  } else if (second === '0' && minute === '0' && hour === '0' && dayOfMonth === '*' && month === '*' && dayOfWeek !== '*') {
    const weekDays = ['日', '一', '二', '三', '四', '五', '六']
    description = `每周${weekDays[parseInt(dayOfWeek)]}执行`
  } else {
    description = `${month}月 ${dayOfMonth}日 ${hour}:${minute}:${second} 执行`
  }

  return description
}

export const validateSqlIdentifier = (name: string): boolean => {
  return /^[a-zA-Z_][a-zA-Z0-9_]*$/.test(name)
}

export const escapeSql = (value: string): string => {
  return value.replace(/'/g, "''")
}

export const safeJsonParse = <T>(str: string, defaultValue: T): T => {
  try {
    return JSON.parse(str)
  } catch {
    return defaultValue
  }
}

export const safeJsonStringify = (obj: any, space?: number): string => {
  try {
    return JSON.stringify(obj, null, space)
  } catch {
    return ''
  }
}

export const getColorPalette = (count: number): string[] => {
  const defaultPalette = [
    '#1677ff',
    '#52c41a',
    '#faad14',
    '#f5222d',
    '#722ed1',
    '#13c2c2',
    '#eb2f96',
    '#fa8c16',
    '#a0d911',
    '#2f54eb'
  ]
  
  if (count <= defaultPalette.length) {
    return defaultPalette.slice(0, count)
  }
  
  const result = [...defaultPalette]
  for (let i = defaultPalette.length; i < count; i++) {
    const hue = (i * 360) / count
    result.push(`hsl(${hue}, 70%, 50%)`)
  }
  return result
}

export const downloadFile = (content: string, filename: string, mimeType = 'text/plain'): void => {
  const blob = new Blob([content], { type: mimeType })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

export const copyToClipboard = async (text: string): Promise<boolean> => {
  try {
    await navigator.clipboard.writeText(text)
    return true
  } catch {
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    const success = document.execCommand('copy')
    document.body.removeChild(textarea)
    return success
  }
}

export const getRelativeTime = (date: string | Date): string => {
  const now = dayjs()
  const target = dayjs(date)
  const diffDays = now.diff(target, 'day')
  
  if (diffDays === 0) {
    const diffHours = now.diff(target, 'hour')
    if (diffHours === 0) {
      const diffMinutes = now.diff(target, 'minute')
      if (diffMinutes === 0) {
        return '刚刚'
      }
      return `${diffMinutes}分钟前`
    }
    return `${diffHours}小时前`
  } else if (diffDays === 1) {
    return '昨天'
  } else if (diffDays < 7) {
    return `${diffDays}天前`
  } else if (diffDays < 30) {
    return `${Math.floor(diffDays / 7)}周前`
  } else if (diffDays < 365) {
    return `${Math.floor(diffDays / 30)}个月前`
  } else {
    return `${Math.floor(diffDays / 365)}年前`
  }
}

export const paginateArray = <T>(array: T[], page: number, pageSize: number): {
  data: T[]
  total: number
  page: number
  pageSize: number
  totalPages: number
} => {
  const total = array.length
  const totalPages = Math.ceil(total / pageSize)
  const startIndex = (page - 1) * pageSize
  const endIndex = startIndex + pageSize
  const data = array.slice(startIndex, endIndex)
  
  return {
    data,
    total,
    page,
    pageSize,
    totalPages
  }
}

export const sortArray = <T>(array: T[], field: keyof T, order: 'asc' | 'desc' = 'asc'): T[] => {
  return [...array].sort((a, b) => {
    const aVal = a[field]
    const bVal = b[field]
    
    if (aVal === null || aVal === undefined) return order === 'asc' ? -1 : 1
    if (bVal === null || bVal === undefined) return order === 'asc' ? 1 : -1
    
    if (typeof aVal === 'string' && typeof bVal === 'string') {
      return order === 'asc' ? aVal.localeCompare(bVal) : bVal.localeCompare(aVal)
    }
    
    if (aVal < bVal) return order === 'asc' ? -1 : 1
    if (aVal > bVal) return order === 'asc' ? 1 : -1
    return 0
  })
}

export const filterArray = <T>(array: T[], filters: Partial<T>): T[] => {
  return array.filter(item => {
    return Object.entries(filters).every(([key, value]) => {
      if (value === undefined || value === null || value === '') return true
      const itemValue = item[key as keyof T]
      if (typeof value === 'string' && typeof itemValue === 'string') {
        return itemValue.toLowerCase().includes(value.toLowerCase())
      }
      return itemValue === value
    })
  })
}

export const groupArray = <T, K extends keyof T>(array: T[], key: K): Record<string, T[]> => {
  return array.reduce((groups, item) => {
    const groupKey = String(item[key])
    if (!groups[groupKey]) {
      groups[groupKey] = []
    }
    groups[groupKey].push(item)
    return groups
  }, {} as Record<string, T[]>)
}

export const uniqueArray = <T>(array: T[], key?: keyof T): T[] => {
  if (!key) {
    return [...new Set(array)]
  }
  const seen = new Set()
  return array.filter(item => {
    const value = item[key]
    if (seen.has(value)) return false
    seen.add(value)
    return true
  })
}

export const sumArray = <T>(array: T[], field?: keyof T): number => {
  if (!field) {
    return array.reduce((sum, item) => sum + (Number(item) || 0), 0)
  }
  return array.reduce((sum, item) => sum + (Number(item[field]) || 0), 0)
}

export const avgArray = <T>(array: T[], field?: keyof T): number => {
  if (array.length === 0) return 0
  return sumArray(array, field) / array.length
}
