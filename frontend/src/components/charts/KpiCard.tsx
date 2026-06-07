import React, { useMemo } from 'react'
import { Card, Statistic, Row, Col, Tag, Tooltip } from 'antd'
import { ArrowUpOutlined, ArrowDownOutlined, MinusOutlined } from '@ant-design/icons'
import BaseChart from './BaseChart'
import type { QueryResult, ComponentConfig } from '@/types'
import { formatNumber } from '@/utils'

interface KpiCardProps {
  title: string
  data?: QueryResult
  config: ComponentConfig
  loading?: boolean
  error?: string
  height?: number
  onRefresh?: () => void
}

const KpiCard: React.FC<KpiCardProps> = ({
  title,
  data,
  config,
  loading = false,
  error,
  height = 200,
  onRefresh
}) => {
  const { kpiConfig = {} } = config
  const { compareType, showTrend = true, prefix = '', suffix = '', decimals = 2 } = kpiConfig

  const kpiData = useMemo(() => {
    if (!data?.rows || data.rows.length === 0) return null

    const currentValue = parseFloat(data.rows[0][0]) || 0
    let compareValue: number | null = null
    let changePercent: number | null = null
    let changeValue: number | null = null

    if (data.rows[0].length > 1 && data.rows[0][1] !== null && data.rows[0][1] !== undefined) {
      compareValue = parseFloat(data.rows[0][1]) || 0
      if (compareValue !== 0) {
        changePercent = ((currentValue - compareValue) / Math.abs(compareValue)) * 100
        changeValue = currentValue - compareValue
      }
    }

    return {
      currentValue,
      compareValue,
      changePercent,
      changeValue
    }
  }, [data])

  const getCompareLabel = () => {
    const labels: Record<string, string> = {
      YEAR_OVER_YEAR: '同比',
      QUARTER_OVER_QUARTER: '环比季度',
      MONTH_OVER_MONTH: '环比月',
      DAY_OVER_DAY: '环比日'
    }
    return labels[compareType as string] || '对比'
  }

  const getTrendIcon = () => {
    if (!kpiData || kpiData.changePercent === null) return <MinusOutlined style={{ color: '#8c8c8c' }} />
    if (kpiData.changePercent > 0) return <ArrowUpOutlined style={{ color: '#52c41a' }} />
    if (kpiData.changePercent < 0) return <ArrowDownOutlined style={{ color: '#ff4d4f' }} />
    return <MinusOutlined style={{ color: '#8c8c8c' }} />
  }

  const getTrendColor = () => {
    if (!kpiData || kpiData.changePercent === null) return '#8c8c8c'
    if (kpiData.changePercent > 0) return '#52c41a'
    if (kpiData.changePercent < 0) return '#ff4d4f'
    return '#8c8c8c'
  }

  const formatValue = (value: number) => {
    return `${prefix}${formatNumber(value, decimals)}${suffix}`
  }

  return (
    <BaseChart
      title={title}
      loading={loading}
      data={data}
      error={error}
      height={height}
      onRefresh={onRefresh}
    >
      <div style={{ height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
        {kpiData && (
          <>
            <Statistic
              title={config.measures?.[0] || '指标值'}
              value={kpiData.currentValue}
              formatter={(value) => formatValue(value as number)}
              style={{ marginBottom: 16 }}
            />
            {showTrend && kpiData.compareValue !== null && (
              <Row gutter={16} align="middle">
                <Col>
                  <Tooltip title={`${getCompareLabel()}值: ${formatValue(kpiData.compareValue)}`}>
                    <Tag color="default">{getCompareLabel()}</Tag>
                  </Tooltip>
                </Col>
                <Col flex="auto">
                  <span style={{ color: getTrendColor(), fontSize: 16, fontWeight: 500 }}>
                    {getTrendIcon()}
                    {' '}
                    {kpiData.changePercent !== null ? `${Math.abs(kpiData.changePercent).toFixed(2)}%` : '-'}
                    {kpiData.changeValue !== null && (
                      <span style={{ marginLeft: 8, fontSize: 14 }}>
                        ({kpiData.changeValue >= 0 ? '+' : ''}{formatValue(kpiData.changeValue)})
                      </span>
                    )}
                  </span>
                </Col>
              </Row>
            )}
          </>
        )}
      </div>
    </BaseChart>
  )
}

export default KpiCard
