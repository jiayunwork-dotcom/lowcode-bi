import React, { useState, useEffect } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { Spin, message } from 'antd'
import { embedApi } from '@/api'
import DashboardViewer from '../dashboard/Viewer'

const EmbedViewer: React.FC = () => {
  const { id } = useParams<{ id: string }>()
  const [searchParams] = useSearchParams()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [embedConfig, setEmbedConfig] = useState<any>(null)
  const token = searchParams.get('token')

  useEffect(() => {
    const validateToken = async () => {
      if (!token) {
        setError('缺少访问令牌')
        setLoading(false)
        return
      }

      try {
        setLoading(true)
        const result = await embedApi.validateToken(token)
        
        if (result.valid) {
          setEmbedConfig(result.config)
        } else {
          setError(result.message || '令牌无效或已过期')
        }
      } catch (error: any) {
        setError(error.message || '验证失败')
      } finally {
        setLoading(false)
      }
    }

    validateToken()
  }, [token])

  if (loading) {
    return (
      <div className="embed-loading">
        <Spin size="large" tip="加载中..." />
      </div>
    )
  }

  if (error) {
    return (
      <div className="embed-error">
        <div style={{ fontSize: 48, marginBottom: 16 }}>⚠️</div>
        <div style={{ fontSize: 18, fontWeight: 500 }}>{error}</div>
        <div style={{ marginTop: 8, fontSize: 14, opacity: 0.8 }}>
          请联系管理员获取有效的访问链接
        </div>
      </div>
    )
  }

  return (
    <div className="embed-container">
      <DashboardViewer
        dashboardId={id}
        embedMode={true}
        embedConfig={embedConfig}
      />
    </div>
  )
}

export default EmbedViewer
