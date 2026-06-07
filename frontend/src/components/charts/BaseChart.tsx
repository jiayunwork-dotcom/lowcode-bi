import React from 'react'
import { Card, Spin, Empty } from 'antd'
import type { QueryResult } from '@/types'

interface BaseChartProps {
  title: string
  loading?: boolean
  data?: QueryResult
  error?: string
  children: React.ReactNode
  height?: number | string
  onRefresh?: () => void
}

const BaseChart: React.FC<BaseChartProps> = ({
  title,
  loading = false,
  data,
  error,
  children,
  height = 300,
  onRefresh
}) => {
  const hasData = data && data.rows && data.rows.length > 0

  return (
    <Card
      title={title}
      size="small"
      extra={onRefresh ? <a onClick={onRefresh}>刷新</a> : null}
      style={{ height: '100%', display: 'flex', flexDirection: 'column' }}
      bodyStyle={{ flex: 1, padding: 8, minHeight: 0 }}
    >
      <Spin spinning={loading} tip="加载中..." style={{ height: '100%' }}>
        {error ? (
          <div style={{ 
            display: 'flex', 
            alignItems: 'center', 
            justifyContent: 'center', 
            height: '100%',
            color: '#ff4d4f' 
          }}>
            {error}
          </div>
        ) : !hasData ? (
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
            <Empty description="暂无数据" />
          </div>
        ) : (
          <div style={{ height: typeof height === 'number' ? `${height}px` : height }}>
            {children}
          </div>
        )}
      </Spin>
    </Card>
  )
}

export default BaseChart
