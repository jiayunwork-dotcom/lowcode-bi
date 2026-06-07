import React, { useState, useEffect, useRef } from 'react'
import {
  Modal,
  Descriptions,
  Tag,
  Typography,
  Button,
  message,
  Alert,
  Card
} from 'antd'
import { alertApi } from '@/api'
import type { AlertEvent, AlertSeverity, AlertEventStatus } from '@/types'
import * as echarts from 'echarts'

const { Title, Text } = Typography

interface AlertEventDetailProps {
  eventId: string
  visible: boolean
  onClose: () => void
}

const AlertEventDetail: React.FC<AlertEventDetailProps> = ({ eventId, visible, onClose }) => {
  const [event, setEvent] = useState<AlertEvent | null>(null)
  const [loading, setLoading] = useState(false)
  const chartRef = useRef<HTMLDivElement>(null)
  const chartInstance = useRef<echarts.ECharts | null>(null)

  useEffect(() => {
    if (visible && eventId) {
      loadEventDetail()
    }
  }, [visible, eventId])

  useEffect(() => {
    if (chartRef.current && event?.trendData && visible) {
      if (!chartInstance.current) {
        chartInstance.current = echarts.init(chartRef.current)
      }

      const triggerTime = event.triggerHighlightTime ?
        new Date(event.triggerHighlightTime).getTime() : null

      const option: echarts.EChartsOption = {
        tooltip: {
          trigger: 'axis',
          formatter: (params: any) => {
            const data = params[0]
            return `${data.axisValue}<br/>值: ${data.value}`
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'time',
          boundaryGap: ['20%', '20%'] as [string, string]
        },
        yAxis: {
          type: 'value',
          scale: true
        },
        series: [
          {
            name: '指标值',
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 6,
            lineStyle: {
              width: 2,
              color: '#1890ff'
            },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(24, 144, 255, 0.3)' },
                { offset: 1, color: 'rgba(24, 144, 255, 0.05)' }
              ])
            },
            data: event.trendData.map(p => [
              new Date(p.time).getTime(),
              p.value
            ]),
            markLine: triggerTime ? {
              silent: true,
              symbol: 'none',
              lineStyle: {
                color: '#ff4d4f',
                width: 2,
                type: 'solid'
              },
              label: {
                formatter: '触发时刻',
                color: '#ff4d4f'
              },
              data: [
                {
                  xAxis: triggerTime
                }
              ]
            } : undefined
          }
        ]
      }

      chartInstance.current.setOption(option)

      const handleResize = () => chartInstance.current?.resize()
      window.addEventListener('resize', handleResize)

      return () => {
        window.removeEventListener('resize', handleResize)
      }
    }
  }, [chartRef.current, event, visible])

  const loadEventDetail = async () => {
    setLoading(true)
    try {
      const data = await alertApi.getEvent(eventId)
      setEvent(data)
    } catch (error) {
      console.error('Failed to load event detail:', error)
      message.error('加载事件详情失败')
    } finally {
      setLoading(false)
    }
  }

  const handleAcknowledge = async () => {
    if (!event) return
    try {
      await alertApi.acknowledgeEvent(event.id)
      message.success('告警已确认')
      setEvent({ ...event, eventStatus: 'ACKNOWLEDGED' as AlertEventStatus })
    } catch (error) {
      console.error('Failed to acknowledge event:', error)
      message.error('确认失败')
    }
  }

  const getSeverityColor = (severity: AlertSeverity) => {
    return {
      INFO: 'blue',
      WARNING: 'orange',
      CRITICAL: 'red'
    }[severity] || 'default'
  }

  const getSeverityText = (severity: AlertSeverity) => {
    return {
      INFO: '信息',
      WARNING: '警告',
      CRITICAL: '严重'
    }[severity] || severity
  }

  const getStatusColor = (status: AlertEventStatus) => {
    return {
      FIRING: 'red',
      RESOLVED: 'green',
      ACKNOWLEDGED: 'blue'
    }[status] || 'default'
  }

  const getStatusText = (status: AlertEventStatus) => {
    return {
      FIRING: '告警中',
      RESOLVED: '已恢复',
      ACKNOWLEDGED: '已确认'
    }[status] || status
  }

  const formatTime = (time: string) => {
    return new Date(time).toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    })
  }

  return (
    <Modal
      title="告警事件详情"
      open={visible}
      onCancel={onClose}
      width={900}
      footer={
        event?.eventStatus === 'FIRING' ? [
          <Button key="ack" type="primary" onClick={handleAcknowledge}>
            确认告警
          </Button>
        ] : null
      }
      destroyOnClose
    >
      {event && (
        <div>
          <Descriptions column={2} bordered size="small" style={{ marginBottom: 16 }}>
            <Descriptions.Item label="告警规则">
              {event.alertRuleName}
            </Descriptions.Item>
            <Descriptions.Item label="度量指标">
              {event.measureName}
            </Descriptions.Item>
            <Descriptions.Item label="严重等级">
              <Tag color={getSeverityColor(event.severity)}>
                {getSeverityText(event.severity)}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="状态">
              <Tag color={getStatusColor(event.eventStatus)}>
                {getStatusText(event.eventStatus)}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="触发值">
              <Text strong type="danger">{event.triggerValue}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="阈值">
              {event.threshold}
            </Descriptions.Item>
            {event.changePercent != null && (
              <>
                <Descriptions.Item label="上一周期值">
                  {event.previousValue}
                </Descriptions.Item>
                <Descriptions.Item label="变化率">
                  <Tag color={event.changePercent > 0 ? 'red' : 'green'}>
                    {event.changePercent > 0 ? '+' : ''}{event.changePercent.toFixed(2)}%
                  </Tag>
                </Descriptions.Item>
              </>
            )}
            <Descriptions.Item label="触发时间">
              {formatTime(event.triggeredAt)}
            </Descriptions.Item>
            {event.resolvedAt && (
              <Descriptions.Item label="恢复时间">
                {formatTime(event.resolvedAt)}
              </Descriptions.Item>
            )}
            {event.acknowledgedAt && (
              <>
                <Descriptions.Item label="确认时间">
                  {formatTime(event.acknowledgedAt)}
                </Descriptions.Item>
                <Descriptions.Item label="确认人">
                  {event.acknowledgedBy}
                </Descriptions.Item>
              </>
            )}
          </Descriptions>

          <Title level={5} style={{ marginTop: 16, marginBottom: 12 }}>
            最近24小时趋势
          </Title>
          <div ref={chartRef} style={{ height: 300, width: '100%' }} />

          {event.isRecovered && event.recoveryValue != null && (
            <Alert
              message="告警已恢复"
              description={`恢复值: ${event.recoveryValue}`}
              type="success"
              showIcon
              style={{ marginTop: 16 }}
            />
          )}
        </div>
      )}
    </Modal>
  )
}

export default AlertEventDetail
