import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { message } from 'antd'
import { useAuthStore } from '@/store/authStore'

const request: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json'
  }
})

request.interceptors.request.use(
  (config) => {
    const { token } = useAuthStore.getState()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  (response: AxiosResponse) => {
    const { data } = response
    
    if (data && typeof data === 'object' && 'success' in data) {
      if (data.success) {
        return data.data !== undefined ? data.data : data
      } else {
        message.error(data.message || '请求失败')
        return Promise.reject(new Error(data.message || '请求失败'))
      }
    }
    
    return data
  },
  (error) => {
    const { response } = error
    
    if (response) {
      const { status, data } = response
      
      switch (status) {
        case 401:
          message.error('登录已过期，请重新登录')
          useAuthStore.getState().logout()
          window.location.href = '/login'
          break
        case 403:
          message.error('没有权限访问该资源')
          break
        case 404:
          message.error('请求的资源不存在')
          break
        case 500:
          message.error(data?.message || '服务器内部错误')
          break
        case 504:
          message.error('请求超时，请稍后重试')
          break
        default:
          message.error(data?.message || `请求失败 (${status})`)
      }
    } else if (error.code === 'ECONNABORTED') {
      message.error('请求超时，请稍后重试')
    } else if (error.message.includes('Network')) {
      message.error('网络连接失败，请检查网络')
    } else {
      message.error(error.message || '请求失败')
    }
    
    return Promise.reject(error)
  }
)

export interface ApiResponse<T = any> {
  success: boolean
  data: T
  message?: string
  total?: number
}

export const http = {
  get: <T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<T> => {
    return request.get(url, { params, ...config })
  },
  
  post: <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
    return request.post(url, data, config)
  },
  
  put: <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
    return request.put(url, data, config)
  },
  
  delete: <T = any>(url: string, config?: AxiosRequestConfig): Promise<T> => {
    return request.delete(url, config)
  },
  
  upload: <T = any>(url: string, file: File, params?: any, onProgress?: (percent: number) => void): Promise<T> => {
    const formData = new FormData()
    formData.append('file', file)
    
    if (params) {
      Object.keys(params).forEach(key => {
        formData.append(key, params[key])
      })
    }
    
    return request.post(url, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(percent)
        }
      }
    })
  },
  
  download: (url: string, params?: any, filename?: string): Promise<void> => {
    return request.get(url, {
      params,
      responseType: 'blob'
    }).then((response) => {
      const blob = new Blob([response.data])
      const link = document.createElement('a')
      link.href = URL.createObjectURL(blob)
      link.download = filename || 'download'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(link.href)
    })
  }
}

export default request
