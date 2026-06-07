import React, { useState, useEffect, useCallback } from 'react'
import {
  Row,
  Col,
  Card,
  Statistic,
  List,
  Tag,
  Typography,
  Select,
  Space,
  Button,
  Empty,
  Descriptions,
  Modal,
  message,
  Tooltip,
  Alert
} from 'antd'
import {
  WarningOutlined,
  PlusCircleOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  ExclamationCircleFilled,
  BellOutlined,
  FireOutlined,
  RiseOutlined
} from '@ant-design/icons'
import { alertApi } from '@/api'
import type { AlertEvent, AlertStatistics, AlertSeverity, AlertEventStatus } from '@/types'
import * as echarts from 'echarts'

const { Title, Text, Paragraph } = Typography
const { Option } = Select

const AlertCenter: React.FC = () => {
  const [statistics, setStatistics] = useState<AlertStatistics | null>(null)
  const [events, setEvents] = useState<AlertEvent[]>([])
  const [selectedEvent, setSelectedEvent] = useState<AlertEvent | null>(null)
  const [eventDetail, setEventDetail] = useState<AlertEvent | null>(null)
  const [statusFilter, setStatusFilter] = useState<AlertEventStatus | undefined>()
  const [severityFilter, setSeverityFilter] = useState<AlertSeverity | undefined>()
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(0)
  const [total, setTotal] = useState(0)
  const [hasMore, setHasMore] = useState(true)
  const [trendChartRef, setTrendChartRef] = useState<HTMLDivElement | null>(null)
  const [chartInstance, setChartInstance] = useState<echarts.ECharts | null>(null)
  const [createModalVisible, setCreateModalVisible] = useState(false)

  const loadStatistics = useCallback(async () => {
    try {
      const data = await alertApi.getStatistics()
      setStatistics(data)
    } catch (error) {
      console.error('Failed to load statistics:', error)
    }
  }, [])

  const loadEvents = useCallback(async (reset = false) => {
    setLoading(true)
    try {
      const currentPage = reset ? 0 : page
      const data = await alertApi.getEvents({
        status: statusFilter,
        severity: severityFilter,
        page: currentPage,
        size: 20
      })
      const newEvents = data.content || []
      if (reset) {
        setEvents(newEvents)
        setPage(1)
      } else {
        setEvents(prev => [...prev, ...newEvents])
        setPage(prev => prev + 1)
      }
      setTotal(data.totalElements || 0)
      setHasMore(newEvents.length >= 20 && events.length + newEvents.length < (data.totalElements || 0))
    } catch (error) {
      console.error('Failed to load events:', error)
      message.error('加载告警事件失败')
    } finally {
      setLoading(false)
    }
  }, [statusFilter, severityFilter, page, events.length])

  useEffect(() => {
    loadStatistics()
    loadEvents(true)
  }, [statusFilter, severityFilter])

  useEffect(() => {
    if (trendChartRef && eventDetail?.trendData) {
      let chart = chartInstance
      if (!chart) {
        chart = echarts.init(trendChartRef)
        setChartInstance(chart)
      }

      const triggerTime = eventDetail.triggerHighlightTime ?
        new Date(eventDetail.triggerHighlightTime).getTime() : null

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
            data: eventDetail.trendData.map(p => [
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

      chart.setOption(option)

      const handleResize = () => chart?.resize()
      window.addEventListener('resize', handleResize)
      return () => {
        window.removeEventListener('resize', handleResize)
      }
    }
  }, [trendChartRef, eventDetail, chartInstance])

  const handleEventClick = async (event: AlertEvent) => {
    setSelectedEvent(event)
    try {
      const data = await alertApi.getEvent(event.id)
      setEventDetail(data)
    } catch (error) {
      console.error('Failed to load event detail:', error)
      message.error('加载事件详情失败')
    }
  }

  const handleAcknowledge = async () => {
    if (!selectedEvent) return
    try {
      await alertApi.acknowledgeEvent(selectedEvent.id)
      message.success('告警已确认')
      if (eventDetail && eventDetail.id === selectedEvent.id) {
        setEventDetail({ ...eventDetail, eventStatus: 'ACKNOWLEDGED' as AlertEventStatus })
      }
      setEvents(prev => prev.map(e =>
        e.id === selectedEvent.id ? { ...e, eventStatus: 'ACKNOWLEDGED' as AlertEventStatus } : e
      ))
      loadStatistics()
    } catch (error) {
      console.error('Failed to acknowledge event:', error)
      message.error('确认告警失败')
    }
  }

  const getSeverityColor = (severity: AlertSeverity) => {
    return {
      INFO: 'blue',
      WARNING: 'orange',
      CRITICAL: 'red'
    }[severity] || 'default'
  }

  const getSeverityIcon = (severity: AlertSeverity) => {
    return {
      INFO: <CheckCircleOutlined style={{ color: '#1890ff' }} />,
      WARNING: <WarningOutlined style={{ color: '#faad14' }} />,
      CRITICAL: <ExclamationCircleFilled style={{ color: '#ff4d4f' }} />
    }[severity] || <BellOutlined />
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
      minute: '2-digit'
    })
  }

  const getOperatorText = (operator: string, threshold: number, triggerType: string) => {
    const ops: Record<string, string> = {
      GREATER_THAN: '>',
      LESS_THAN: '<',
      EQUAL: '=',
      NOT_EQUAL: '≠',
      GREATER_THAN_OR_EQUAL: '≥',
      LESS_THAN_OR_EQUAL: '≤',
      INCREASE_PERCENT: '环比上涨 >',
      DECREASE_PERCENT: '环比下跌 >',
      CHANGE_PERCENT: '环比变化绝对值 >'
    }
    if (triggerType === 'RELATIVE_CHANGE' && !['INCREASE_PERCENT', 'DECREASE_PERCENT', 'CHANGE_PERCENT'].includes(operator)) {
      return `${ops[operator] || operator} ${threshold}%`
    }
    return `${ops[operator] || operator} ${threshold}`
  }

  return (
    <div style={{ padding: 0 }}>
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="当前活跃告警"
              value={statistics?.activeAlertCount || 0}
              prefix={<FireOutlined style={{ color: '#ff4d4f' }} />}
              valueStyle={{ color: '#ff4d4f' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="今日新增"
              value={statistics?.todayNewCount || 0}
              prefix={<PlusCircleOutlined style={{ color: '#1890ff' }} />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card title="本周触发最频繁" size="small" bodyStyle={{ padding: '12px' }}>
            {statistics?.topTriggeredRules && statistics.topTriggeredRules.length > 0 ? (
              <List
                size="small"
                dataSource={statistics.topTriggeredRules}
                renderItem={(item, index) => (
                  <List.Item>
                    <Space>
                      <Tag color={index === 0 ? 'red' : index === 1 ? 'orange' : 'gold'}>
                        TOP{index + 1}
                      </Tag>
                      <Text ellipsis style={{ maxWidth: 120 }} title={item.ruleName}>
                        {item.ruleName}
                      </Text>
                      <Text type="secondary">{item.triggerCount}次</Text>
                    </Space>
                  </List.Item>
                )}
              />
            ) : (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无数据" />
            )}
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="平均恢复时长"
              value={statistics?.averageRecoveryMinutes ? statistics.averageRecoveryMinutes.toFixed(1) : '-'}
              suffix={statistics?.averageRecoveryMinutes != null ? '分钟' : ''}
              prefix={<ClockCircleOutlined style={{ color: '#52c41a' }} />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={10}>
          <Card
            title="告警事件"
            extra={
              <Space>
                <Select
                  placeholder="状态筛选"
                  style={{ width: 120 }}
                  allowClear
                  value={statusFilter}
                  onChange={setStatusFilter}
                >
                  <Option value="FIRING">告警中</Option>
                  <Option value="RESOLVED">已恢复</Option>
                  <Option value="ACKNOWLEDGED">已确认</Option>
                </Select>
                <Select
                  placeholder="等级筛选"
                  style={{ width: 120 }}
                  allowClear
                  value={severityFilter}
                  onChange={setSeverityFilter}
                >
                  <Option value="INFO">信息</Option>
                  <Option value="WARNING">警告</Option>
                  <Option value="CRITICAL">严重</Option>
                </Select>
              </Space>
            }
            style={{ height: 'calc(100vh - 300px)', overflow: 'auto' }}
            bodyStyle={{ padding: 0 }}
          >
            {events.length === 0 ? (
              <Empty description="暂无告警事件" style={{ padding: '60px 0' }} />
            ) : (
              <List
                itemLayout="vertical"
                dataSource={events}
                loading={loading}
                renderItem={event => (
                  <List.Item
                    key={event.id}
                    onClick={() => handleEventClick(event)}
                    style={{
                      padding: '16px',
                      cursor: 'pointer',
                      background: selectedEvent?.id === event.id ? '#e6f7ff' : 'transparent',
                      borderBottom: '1px solid #f0f0f0',
                      transition: 'all 0.3s'
                    }}
                    onMouseEnter={(e) => {
                      if (selectedEvent?.id !== event.id) {
                        e.currentTarget.style.background = '#fafafa'
                      }
                    }}
                    onMouseLeave={(e) => {
                      if (selectedEvent?.id !== event.id) {
                        e.currentTarget.style.background = 'transparent'
                      }
                    }}
                  >
                    <List.Item.Meta
                      avatar={getSeverityIcon(event.severity)}
                      title={
                        <Space wrap>
                          <Text strong>{event.alertRuleName}</Text>
                          <Tag color={getSeverityColor(event.severity)}>{event.severity}</Tag>
                          <Tag color={getStatusColor(event.eventStatus)}>
                            {getStatusText(event.eventStatus)}
                          </Tag>
                        </Space>
                      }
                      description={
                        <div>
                          <Paragraph ellipsis={{ rows: 1 }} style={{ marginBottom: 8 }}>
                            度量: {event.measureName} · 当前值: <Text strong>{event.triggerValue}</Text> · 阈值: {event.threshold}
                          </Paragraph>
                          <Text type="secondary" style={{ fontSize: 12 }}>
                            触发时间: {formatTime(event.triggeredAt)}
                          </Text>
                        </div>
                      }
                    />
                  </List.Item>
                )}
                loadMore={
                  hasMore && !loading ? (
                    <div style={{ textAlign: 'center', padding: '12px 0' }}>
                      <Button onClick={() => loadEvents(false)}>加载更多</Button>
                    </div>
                  ) : null
                }
              />
            )}
          </Card>
        </Col>

        <Col xs={24} lg={14}>
          <Card
            title="事件详情"
            extra={
              selectedEvent && selectedEvent.eventStatus === 'FIRING' ? (
                <Button type="primary" onClick={handleAcknowledge}>
                  确认告警
                </Button>
              ) : null
            }
            style={{ height: 'calc(100vh - 300px)', overflow: 'auto' }}
          >
            {eventDetail ? (
              <div>
                <Descriptions column={2} bordered size="small" style={{ marginBottom: 16 }}>
                  <Descriptions.Item label="告警规则">
                    {eventDetail.alertRuleName}
                  </Descriptions.Item>
                  <Descriptions.Item label="度量指标">
                    {eventDetail.measureName}
                  </Descriptions.Item>
                  <Descriptions.Item label="严重等级">
                    <Tag color={getSeverityColor(eventDetail.severity)}>
                      {eventDetail.severity === 'INFO' ? '信息' :
                       eventDetail.severity === 'WARNING' ? '警告' : '严重'}
                    </Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="状态">
                    <Tag color={getStatusColor(eventDetail.eventStatus)}>
                      {getStatusText(eventDetail.eventStatus)}
                    </Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="触发值">
                    <Text strong type="danger">{eventDetail.triggerValue}</Text>
                  </Descriptions.Item>
                  <Descriptions.Item label="阈值">
                    {eventDetail.threshold}
                  </Descriptions.Item>
                  {eventDetail.changePercent != null && (
                    <>
                      <Descriptions.Item label="上一周期值">
                        {eventDetail.previousValue}
                      </Descriptions.Item>
                      <Descriptions.Item label="变化率">
                        <Tag color={eventDetail.changePercent > 0 ? 'red' : 'green'}>
                          {eventDetail.changePercent > 0 ? '+' : ''}{eventDetail.changePercent.toFixed(2)}%
                        </Tag>
                      </Descriptions.Item>
                    </>
                  )}
                  <Descriptions.Item label="触发时间">
                    {formatTime(eventDetail.triggeredAt)}
                  </Descriptions.Item>
                  {eventDetail.resolvedAt && (
                    <Descriptions.Item label="恢复时间">
                      {formatTime(eventDetail.resolvedAt)}
                    </Descriptions.Item>
                  )}
                  {eventDetail.acknowledgedAt && (
                    <>
                      <Descriptions.Item label="确认时间">
                        {formatTime(eventDetail.acknowledgedAt)}
                      </Descriptions.Item>
                      <Descriptions.Item label="确认人">
                        {eventDetail.acknowledgedBy}
                      </Descriptions.Item>
                    </>
                  )}
                </Descriptions>

                <Title level={5} style={{ marginTop: 24, marginBottom: 12 }}>
                  最近24小时趋势
                </Title>
                <div
                  ref={setTrendChartRef}
                  style={{ height: 300, width: '100%' }}
                />

                {eventDetail.isRecovered && eventDetail.recoveryValue != null && (
                  <Alert
                    message="告警已恢复"
                    description={`恢复值: ${eventDetail.recoveryValue}`}
                    type="success"
                    showIcon
                    style={{ marginTop: 16 }}
                  />
                )}
              </div>
            ) : (
              <Empty
                description="请选择一个告警事件查看详情"
                style={{ padding: '60px 0' }}
              />
            )}
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default AlertCenter
