import React, { useState, useEffect } from 'react'
import {
  Table,
  Button,
  Space,
  Modal,
  Form,
  Input,
  Select,
  Switch,
  InputNumber,
  message,
  Popconfirm,
  Tag,
  Card,
  Row,
  Col,
  Statistic,
  Tooltip,
  Drawer,
  List,
  Descriptions,
  Divider
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  PlayCircleOutlined,
  PauseCircleOutlined,
  ReloadOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  HistoryOutlined,
  SettingOutlined,
  MailOutlined,
  DashboardOutlined
} from '@ant-design/icons'
import type { ScheduleConfig, ScheduleExecutionLog, Dashboard } from '@/types'
import { scheduleApi, dashboardApi } from '@/api'
import { formatDate, getRelativeTime, getStatusColor, getStatusText } from '@/utils'

const { Option } = Select
const { TextArea } = Input

const ScheduleList: React.FC = () => {
  const [schedules, setSchedules] = useState<ScheduleConfig[]>([])
  const [dashboards, setDashboards] = useState<Dashboard[]>([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [logVisible, setLogVisible] = useState(false)
  const [editingItem, setEditingItem] = useState<ScheduleConfig | null>(null)
  const [selectedSchedule, setSelectedSchedule] = useState<ScheduleConfig | null>(null)
  const [executionLogs, setExecutionLogs] = useState<ScheduleExecutionLog[]>([])
  const [logsLoading, setLogsLoading] = useState(false)
  const [form] = Form.useForm()
  const [stats, setStats] = useState({
    total: 0,
    enabled: 0,
    paused: 0,
    failed: 0
  })

  const loadData = async () => {
    try {
      setLoading(true)
      const [scheduleData, dashboardData] = await Promise.all([
        scheduleApi.getList(),
        dashboardApi.getList()
      ])
      setSchedules(scheduleData)
      setDashboards(dashboardData)
      setStats({
        total: scheduleData.length,
        enabled: scheduleData.filter(s => s.isEnabled && !s.isPaused).length,
        paused: scheduleData.filter(s => s.isPaused).length,
        failed: scheduleData.filter(s => s.lastExecutionStatus === 'FAILED').length
      })
    } catch (error) {
      console.error('Failed to load schedules:', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [])

  const handleCreate = () => {
    setEditingItem(null)
    form.resetFields()
    form.setFieldsValue({
      cronExpression: '0 0 9 * * ?',
      timezone: 'Asia/Shanghai',
      isEnabled: true
    })
    setModalVisible(true)
  }

  const handleEdit = (record: ScheduleConfig) => {
    setEditingItem(record)
    form.setFieldsValue({
      ...record,
      recipients: record.recipients?.join(', ')
    })
    setModalVisible(true)
  }

  const handleDelete = async (id: string) => {
    try {
      await scheduleApi.delete(id)
      message.success('删除成功')
      loadData()
    } catch (error) {
      console.error('Failed to delete schedule:', error)
    }
  }

  const handleToggle = async (record: ScheduleConfig, enabled: boolean) => {
    try {
      if (enabled) {
        await scheduleApi.enable(record.id)
      } else {
        await scheduleApi.disable(record.id)
      }
      message.success(enabled ? '已启用' : '已禁用')
      loadData()
    } catch (error) {
      console.error('Failed to toggle schedule:', error)
    }
  }

  const handlePauseResume = async (record: ScheduleConfig) => {
    try {
      if (record.isPaused) {
        await scheduleApi.resume(record.id)
        message.success('已恢复')
      } else {
        await scheduleApi.pause(record.id)
        message.success('已暂停')
      }
      loadData()
    } catch (error) {
      console.error('Failed to pause/resume schedule:', error)
    }
  }

  const handleExecute = async (record: ScheduleConfig) => {
    try {
      await scheduleApi.executeNow(record.id)
      message.success('已触发执行')
    } catch (error) {
      console.error('Failed to execute schedule:', error)
      message.error('执行失败')
    }
  }

  const handleViewLogs = async (record: ScheduleConfig) => {
    try {
      setSelectedSchedule(record)
      setLogsLoading(true)
      const logs = await scheduleApi.getExecutionLogs(record.id)
      setExecutionLogs(logs)
      setLogVisible(true)
    } catch (error) {
      console.error('Failed to load logs:', error)
    } finally {
      setLogsLoading(false)
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      const data = {
        ...values,
        recipients: values.recipients
          ? values.recipients.split(',').map((e: string) => e.trim()).filter(Boolean)
          : []
      }

      if (editingItem) {
        await scheduleApi.update(editingItem.id, data)
        message.success('更新成功')
      } else {
        await scheduleApi.create(data)
        message.success('创建成功')
      }

      setModalVisible(false)
      loadData()
    } catch (error) {
      console.error('Failed to submit schedule:', error)
    }
  }

  const getCronDescription = (cron: string) => {
    const parts = cron.split(' ')
    if (parts.length === 6) {
      const [sec, min, hour, day, month, week] = parts
      if (sec === '0' && min === '0' && hour === '9' && day === '*' && month === '*' && week === '?') {
        return '每天早上9点'
      }
      if (sec === '0' && min === '0' && hour === '0' && day === '*' && month === '*' && week === '?') {
        return '每天凌晨0点'
      }
      if (sec === '0' && min === '0' && hour === '9' && day === '1' && month === '*' && week === '?') {
        return '每月1号早上9点'
      }
      if (sec === '0' && min === '0' && hour === '9' && day === '?' && month === '*' && week === '1') {
        return '每周一早上9点'
      }
    }
    return cron
  }

  const columns = [
    {
      title: '任务名称',
      dataIndex: 'name',
      key: 'name',
      render: (text: string, record: ScheduleConfig) => (
        <Space>
          <ClockCircleOutlined />
          <span>{text}</span>
        </Space>
      )
    },
    {
      title: '仪表板',
      dataIndex: 'dashboardName',
      key: 'dashboardName',
      render: (text: string) => (
        <Space>
          <DashboardOutlined />
          <span>{text}</span>
        </Space>
      )
    },
    {
      title: '执行时间',
      key: 'schedule',
      width: 180,
      render: (_: any, record: ScheduleConfig) => (
        <Tooltip title={record.cronExpression}>
          <Tag color="blue">{getCronDescription(record.cronExpression)}</Tag>
        </Tooltip>
      )
    },
    {
      title: '收件人',
      dataIndex: 'recipients',
      key: 'recipients',
      width: 200,
      render: (recipients: string[]) => (
        <Tooltip title={recipients?.join(', ')}>
          <Space>
            <MailOutlined />
            <span>{recipients?.length || 0} 人</span>
          </Space>
        </Tooltip>
      )
    },
    {
      title: '上次执行',
      key: 'lastExecution',
      width: 150,
      render: (_: any, record: ScheduleConfig) => (
        <Space direction="vertical" size={0}>
          <span style={{ fontSize: 12 }}>
            {record.lastExecutionAt ? getRelativeTime(record.lastExecutionAt) : '未执行'}
          </span>
          {record.lastExecutionStatus && (
            <Tag color={getStatusColor(record.lastExecutionStatus)} style={{ margin: 0 }}>
              {getStatusText(record.lastExecutionStatus)}
            </Tag>
          )}
        </Space>
      )
    },
    {
      title: '连续失败',
      dataIndex: 'consecutiveFailures',
      key: 'consecutiveFailures',
      width: 100,
      align: 'center',
      render: (count: number) => (
        count > 0 ? (
          <Tag color="red">{count} 次</Tag>
        ) : (
          <Tag color="green">正常</Tag>
        )
      )
    },
    {
      title: '状态',
      key: 'status',
      width: 100,
      render: (_: any, record: ScheduleConfig) => (
        <Space>
          <Switch
            checked={record.isEnabled}
            onChange={(checked) => handleToggle(record, checked)}
            size="small"
          />
          {record.isPaused && <Tag color="orange">已暂停</Tag>}
        </Space>
      )
    },
    {
      title: '操作',
      key: 'actions',
      width: 240,
      render: (_: any, record: ScheduleConfig) => (
        <Space size="small">
          <Tooltip title="立即执行">
            <Button
              type="text"
              size="small"
              icon={<PlayCircleOutlined />}
              onClick={() => handleExecute(record)}
            />
          </Tooltip>
          <Tooltip title={record.isPaused ? '恢复' : '暂停'}>
            <Button
              type="text"
              size="small"
              icon={record.isPaused ? <PlayCircleOutlined /> : <PauseCircleOutlined />}
              onClick={() => handlePauseResume(record)}
            />
          </Tooltip>
          <Tooltip title="执行日志">
            <Button
              type="text"
              size="small"
              icon={<HistoryOutlined />}
              onClick={() => handleViewLogs(record)}
            />
          </Tooltip>
          <Tooltip title="编辑">
            <Button
              type="text"
              size="small"
              icon={<EditOutlined />}
              onClick={() => handleEdit(record)}
            />
          </Tooltip>
          <Popconfirm
            title="确定要删除这个定时任务吗？"
            onConfirm={() => handleDelete(record.id)}
            okText="确定"
            cancelText="取消"
          >
            <Button
              type="text"
              size="small"
              danger
              icon={<DeleteOutlined />}
            />
          </Popconfirm>
        </Space>
      )
    }
  ]

  const logColumns = [
    {
      title: '执行时间',
      dataIndex: 'executedAt',
      key: 'executedAt',
      width: 180,
      render: (date: string) => formatDate(date)
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => {
        const colors: Record<string, string> = {
          SUCCESS: 'success',
          FAILED: 'error',
          TIMEOUT: 'warning'
        }
        return <Tag color={colors[status] || 'default'}>{getStatusText(status)}</Tag>
      }
    },
    {
      title: '耗时',
      dataIndex: 'durationMs',
      key: 'durationMs',
      width: 100,
      render: (ms: number) => `${ms}ms`
    },
    {
      title: '错误信息',
      dataIndex: 'errorMessage',
      key: 'errorMessage',
      render: (msg: string) => msg || '-'
    }
  ]

  const cronPresets = [
    { label: '每天早上9点', value: '0 0 9 * * ?' },
    { label: '每天凌晨0点', value: '0 0 0 * * ?' },
    { label: '每周一早上9点', value: '0 0 9 ? * 1' },
    { label: '每月1号早上9点', value: '0 0 9 1 * ?' },
    { label: '每小时', value: '0 0 * * * ?' },
    { label: '每30分钟', value: '0 0/30 * * * ?' }
  ]

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">定时任务管理</h1>
        <div className="page-actions">
          <Button
            icon={<ReloadOutlined />}
            onClick={loadData}
            loading={loading}
          >
            刷新
          </Button>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={handleCreate}
          >
            新建任务
          </Button>
        </div>
      </div>

      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="总任务数"
              value={stats.total}
              prefix={<ClockCircleOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="已启用"
              value={stats.enabled}
              valueStyle={{ color: '#52c41a' }}
              prefix={<CheckCircleOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="已暂停"
              value={stats.paused}
              valueStyle={{ color: '#faad14' }}
              prefix={<PauseCircleOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="执行失败"
              value={stats.failed}
              valueStyle={{ color: '#ff4d4f' }}
              prefix={<CloseCircleOutlined />}
            />
          </Card>
        </Col>
      </Row>

      <Card>
        <Table
          dataSource={schedules}
          columns={columns}
          rowKey="id"
          loading={loading}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`
          }}
        />
      </Card>

      <Modal
        title={editingItem ? '编辑定时任务' : '新建定时任务'}
        open={modalVisible}
        width={600}
        onCancel={() => setModalVisible(false)}
        footer={[
          <Button key="cancel" onClick={() => setModalVisible(false)}>取消</Button>,
          <Button key="submit" type="primary" onClick={handleSubmit}>保存</Button>
        ]}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="任务名称"
            rules={[{ required: true, message: '请输入任务名称' }]}
          >
            <Input placeholder="请输入任务名称" />
          </Form.Item>

          <Form.Item
            name="dashboardId"
            label="选择仪表板"
            rules={[{ required: true, message: '请选择仪表板' }]}
          >
            <Select placeholder="请选择要推送的仪表板">
              {dashboards.map(d => (
                <Option key={d.id} value={d.id}>{d.name}</Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            name="cronExpression"
            label="Cron表达式"
            rules={[{ required: true, message: '请输入Cron表达式' }]}
          >
            <Select
              placeholder="请选择或输入Cron表达式"
              dropdownRender={(menu) => (
                <>
                  {menu}
                  <Divider style={{ margin: '8px 0' }} />
                  <div style={{ padding: '0 12px 8px' }}>
                    <div style={{ fontSize: 12, color: '#8c8c8c', marginBottom: 8 }}>常用表达式:</div>
                    <Space wrap>
                      {cronPresets.map(preset => (
                        <Tag
                          key={preset.value}
                          color="blue"
                          style={{ cursor: 'pointer' }}
                          onClick={() => form.setFieldsValue({ cronExpression: preset.value })}
                        >
                          {preset.label}
                        </Tag>
                      ))}
                    </Space>
                  </div>
                </>
              )}
            >
              {cronPresets.map(preset => (
                <Option key={preset.value} value={preset.value}>{preset.label} ({preset.value})</Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            name="timezone"
            label="时区"
            rules={[{ required: true, message: '请选择时区' }]}
          >
            <Select>
              <Option value="Asia/Shanghai">Asia/Shanghai (UTC+8)</Option>
              <Option value="UTC">UTC (UTC+0)</Option>
              <Option value="America/New_York">America/New_York (UTC-5)</Option>
              <Option value="Europe/London">Europe/London (UTC+0)</Option>
              <Option value="Asia/Tokyo">Asia/Tokyo (UTC+9)</Option>
            </Select>
          </Form.Item>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="emailSubject"
                label="邮件主题"
                rules={[{ required: true, message: '请输入邮件主题' }]}
              >
                <Input placeholder="请输入邮件主题" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="recipients"
                label="收件人"
                rules={[{ required: true, message: '请输入收件人' }]}
                extra="多个邮箱用逗号分隔"
              >
                <Input placeholder="email1@example.com, email2@example.com" />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="emailBody"
            label="邮件正文"
          >
            <TextArea rows={3} placeholder="请输入邮件正文" />
          </Form.Item>

          <Form.Item
            name="isEnabled"
            label="立即启用"
            valuePropName="checked"
          >
            <Switch />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title={`执行日志 - ${selectedSchedule?.name}`}
        placement="right"
        width={720}
        open={logVisible}
        onClose={() => setLogVisible(false)}
        extra={
          <Button onClick={() => handleViewLogs(selectedSchedule!)}>
            <ReloadOutlined /> 刷新
          </Button>
        }
      >
        {selectedSchedule && (
          <Descriptions size="small" column={2} style={{ marginBottom: 16 }}>
            <Descriptions.Item label="Cron表达式">{selectedSchedule.cronExpression}</Descriptions.Item>
            <Descriptions.Item label="时区">{selectedSchedule.timezone}</Descriptions.Item>
            <Descriptions.Item label="收件人">{selectedSchedule.recipients?.join(', ') || '-'}</Descriptions.Item>
            <Descriptions.Item label="连续失败">{selectedSchedule.consecutiveFailures} 次</Descriptions.Item>
          </Descriptions>
        )}

        <Table
          dataSource={executionLogs}
          columns={logColumns}
          rowKey="id"
          loading={logsLoading}
          pagination={{ pageSize: 20 }}
        />
      </Drawer>
    </div>
  )
}

export default ScheduleList
