import React, { useState, useEffect, useCallback } from 'react'
import {
  Table,
  Button,
  Modal,
  Form,
  Input,
  Select,
  InputNumber,
  Switch,
  Space,
  Tag,
  message,
  Popconfirm,
  Card,
  Row,
  Col,
  Tabs,
  List,
  Empty,
  Tooltip,
  Divider,
  Typography,
  Descriptions
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  PlayCircleOutlined,
  PauseCircleOutlined,
  BellOutlined,
  SettingOutlined,
  ExclamationCircleOutlined,
  InfoCircleOutlined,
  EyeOutlined,
  LinkOutlined,
  FireOutlined,
  RiseOutlined,
  ArrowUpOutlined
} from '@ant-design/icons'
import { alertApi, dataModelApi, userApi } from '@/api'
import type {
  AlertRule,
  AlertEvent,
  AlertSeverity,
  AlertTriggerType,
  AlertOperator,
  AlertCheckInterval,
  NotificationChannel,
  DataModel,
  Measure,
  User,
  EscalationRecipient
} from '@/types'
import AlertEventDetail from './components/AlertEventDetail'

const { Title, Text, Paragraph } = Typography
const { Option } = Select
const { TextArea } = Input
const { TabPane } = Tabs

const AlertRuleManagement: React.FC = () => {
  const [rules, setRules] = useState<AlertRule[]>([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingRule, setEditingRule] = useState<AlertRule | null>(null)
  const [form] = Form.useForm()
  const [dataModels, setDataModels] = useState<DataModel[]>([])
  const [measures, setMeasures] = useState<{ id: string; name: string; displayName: string }[]>([])
  const [selectedRule, setSelectedRule] = useState<AlertRule | null>(null)
  const [detailModalVisible, setDetailModalVisible] = useState(false)
  const [channels, setChannels] = useState<NotificationChannel[]>([])
  const [activeTab, setActiveTab] = useState('all')
  const [ruleEvents, setRuleEvents] = useState<AlertEvent[]>([])
  const [eventsLoading, setEventsLoading] = useState(false)
  const [showEventDetail, setShowEventDetail] = useState<AlertEvent | null>(null)
  const [subscribedRules, setSubscribedRules] = useState<AlertRule[]>([])
  const [subscribedTotal, setSubscribedTotal] = useState(0)
  const [users, setUsers] = useState<User[]>([])
  const [escalationRecipients, setEscalationRecipients] = useState<string[]>([])

  const loadRules = useCallback(async () => {
    setLoading(true)
    try {
      const data = await alertApi.getRules({ page: 0, size: 100 })
      setRules(data.content || [])
    } catch (error) {
      console.error('Failed to load rules:', error)
      message.error('加载告警规则失败')
    } finally {
      setLoading(false)
    }
  }, [])

  const loadSubscribedRules = useCallback(async () => {
    setLoading(true)
    try {
      const data = await alertApi.getSubscribedRules({ page: 0, size: 100 })
      setSubscribedRules(data.content || [])
      setSubscribedTotal(data.totalElements || 0)
    } catch (error) {
      console.error('Failed to load subscribed rules:', error)
      message.error('加载订阅规则失败')
    } finally {
      setLoading(false)
    }
  }, [])

  const loadDataModels = useCallback(async () => {
    try {
      const data = await dataModelApi.getList()
      setDataModels(data || [])
    } catch (error) {
      console.error('Failed to load data models:', error)
    }
  }, [])

  const loadUsers = useCallback(async () => {
    try {
      const data = await userApi.getList()
      setUsers(data || [])
    } catch (error) {
      console.error('Failed to load users:', error)
    }
  }, [])

  const loadMeasures = useCallback(async (dataModelId: string) => {
    try {
      const data = await dataModelApi.getFields(dataModelId)
      setMeasures(data.measures || [])
    } catch (error) {
      console.error('Failed to load measures:', error)
      setMeasures([])
    }
  }, [])

  useEffect(() => {
    loadRules()
    loadDataModels()
    loadUsers()
  }, [loadRules, loadDataModels, loadUsers])

  useEffect(() => {
    if (activeTab === 'subscribed') {
      loadSubscribedRules()
    }
  }, [activeTab, loadSubscribedRules])

  const handleDataModelChange = (value: string) => {
    loadMeasures(value)
  }

  const handleCreate = () => {
    setEditingRule(null)
    setChannels([
      { type: 'IN_APP', config: {}, enabled: true }
    ])
    setEscalationRecipients([])
    form.resetFields()
    form.setFieldsValue({
      triggerType: 'VALUE',
      operator: 'GREATER_THAN',
      checkInterval: 'EVERY_HOUR',
      silencePeriod: 300,
      severity: 'WARNING',
      isEnabled: true,
      escalationEnabled: false,
      escalationThreshold: 3
    })
    setModalVisible(true)
  }

  const handleEdit = (rule: AlertRule) => {
    setEditingRule(rule)
    setChannels(rule.notificationChannels || [])
    setEscalationRecipients(rule.escalationRecipients?.map(r => r.userId) || [])
    handleDataModelChange(rule.dataModelId)
    form.setFieldsValue({
      name: rule.name,
      description: rule.description,
      dataModelId: rule.dataModelId,
      measureId: rule.measureId,
      triggerType: rule.triggerType,
      operator: rule.operator,
      threshold: rule.threshold,
      checkInterval: rule.checkInterval,
      silencePeriod: rule.silencePeriod,
      severity: rule.severity,
      isEnabled: rule.isEnabled,
      escalationEnabled: rule.escalationEnabled || false,
      escalationThreshold: rule.escalationThreshold || 3
    })
    setModalVisible(true)
  }

  const handleDelete = async (id: string) => {
    try {
      await alertApi.deleteRule(id)
      message.success('删除成功')
      loadRules()
    } catch (error) {
      console.error('Failed to delete rule:', error)
      message.error('删除失败')
    }
  }

  const handleToggleEnabled = async (rule: AlertRule, enabled: boolean) => {
    try {
      if (enabled) {
        await alertApi.enableRule(rule.id)
        message.success('已启用')
      } else {
        await alertApi.disableRule(rule.id)
        message.success('已禁用')
      }
      loadRules()
    } catch (error) {
      console.error('Failed to toggle rule:', error)
      message.error('操作失败')
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()

      const requestData = {
        ...values,
        notificationChannels: channels,
        escalationRecipientUserIds: values.escalationEnabled ? escalationRecipients : []
      }

      if (editingRule) {
        await alertApi.updateRule(editingRule.id, requestData)
        message.success('更新成功')
      } else {
        await alertApi.createRule(requestData)
        message.success('创建成功')
      }

      setModalVisible(false)
      loadRules()
    } catch (error) {
      console.error('Failed to save rule:', error)
      if ((error as any).message !== 'Validate Failed') {
        message.error('保存失败')
      }
    }
  }

  const handleSubscribe = async (rule: AlertRule, subscribed: boolean) => {
    try {
      await alertApi.subscribe(rule.id, subscribed)
      message.success(subscribed ? '订阅成功' : '取消订阅成功')
      if (activeTab === 'subscribed') {
        loadSubscribedRules()
      } else {
        loadRules()
      }
    } catch (error) {
      console.error('Failed to subscribe:', error)
      message.error('操作失败')
    }
  }

  const handleViewDetail = async (rule: AlertRule) => {
    setSelectedRule(rule)
    setEventsLoading(true)
    try {
      const data = await alertApi.getEventsByRule(rule.id, { page: 0, size: 50 })
      setRuleEvents(data.content || [])
    } catch (error) {
      console.error('Failed to load rule events:', error)
      setRuleEvents([])
    } finally {
      setEventsLoading(false)
    }
    setDetailModalVisible(true)
  }

  const addChannel = (type: 'EMAIL' | 'WEBHOOK') => {
    const exists = channels.some(c => c.type === type)
    if (exists) {
      message.warning('该类型的通知渠道已存在')
      return
    }
    const newChannel: NotificationChannel = {
      type,
      config: type === 'WEBHOOK' ? { url: '', headers: {} } : {},
      enabled: true
    }
    setChannels([...channels, newChannel])
  }

  const removeChannel = (type: string) => {
    if (type === 'IN_APP') {
      message.warning('站内信渠道不可删除')
      return
    }
    setChannels(channels.filter(c => c.type !== type))
  }

  const updateChannelConfig = (type: string, config: Record<string, any>) => {
    setChannels(channels.map(c =>
      c.type === type ? { ...c, config } : c
    ))
  }

  const toggleChannelEnabled = (type: string, enabled: boolean) => {
    setChannels(channels.map(c =>
      c.type === type ? { ...c, enabled } : c
    ))
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

  const getIntervalText = (interval: AlertCheckInterval) => {
    return {
      EVERY_5_MINUTES: '每5分钟',
      EVERY_15_MINUTES: '每15分钟',
      EVERY_30_MINUTES: '每30分钟',
      EVERY_HOUR: '每小时',
      EVERY_6_HOURS: '每6小时',
      EVERY_12_HOURS: '每12小时',
      EVERY_DAY: '每天'
    }[interval] || interval
  }

  const getOperatorText = (operator: AlertOperator, threshold: number, triggerType: AlertTriggerType) => {
    const ops: Record<string, string> = {
      GREATER_THAN: '>',
      LESS_THAN: '<',
      EQUAL: '=',
      NOT_EQUAL: '≠',
      GREATER_THAN_OR_EQUAL: '≥',
      LESS_THAN_OR_EQUAL: '≤',
      INCREASE_PERCENT: '环比上涨 >',
      DECREASE_PERCENT: '环比下跌 >',
      CHANGE_PERCENT: '环比变化 >'
    }
    if (triggerType === 'RELATIVE_CHANGE' || ['INCREASE_PERCENT', 'DECREASE_PERCENT', 'CHANGE_PERCENT'].includes(operator)) {
      return `${ops[operator] || operator} ${threshold}%`
    }
    return `${ops[operator] || operator} ${threshold}`
  }

  const columns = [
    {
      title: '规则名称',
      dataIndex: 'name',
      key: 'name',
      render: (text: string, record: AlertRule) => (
        <Space>
          <BellOutlined style={{ color: '#1890ff' }} />
          <Text strong>{text}</Text>
          {record.status === 'INVALID' && (
            <Tooltip title="关联的数据模型已被删除">
              <Tag color="default">已失效</Tag>
            </Tooltip>
          )}
        </Space>
      )
    },
    {
      title: '度量指标',
      dataIndex: 'measureName',
      key: 'measureName',
      render: (text: string, record: AlertRule) => (
        <div>
          <Text>{text}</Text>
          <div style={{ fontSize: 12, color: '#999' }}>
            {record.dataModelName}
          </div>
        </div>
      )
    },
    {
      title: '触发条件',
      key: 'condition',
      width: 200,
      render: (_: any, record: AlertRule) => (
        <Tag color="blue">
          {getOperatorText(record.operator, record.threshold, record.triggerType)}
        </Tag>
      )
    },
    {
      title: '检测周期',
      dataIndex: 'checkInterval',
      key: 'checkInterval',
      width: 120,
      render: (interval: AlertCheckInterval) => getIntervalText(interval)
    },
    {
      title: '严重等级',
      dataIndex: 'severity',
      key: 'severity',
      width: 100,
      render: (severity: AlertSeverity, record: AlertRule) => (
        <Space direction="vertical" size={2}>
          <Tag color={getSeverityColor(severity)}>
            {getSeverityText(severity)}
          </Tag>
          {record.currentSeverity && record.currentSeverity !== severity && (
            <Tag color="red" style={{ fontSize: 11 }}>
              当前: {getSeverityText(record.currentSeverity)}
            </Tag>
          )}
        </Space>
      )
    },
    {
      title: '连续触发',
      dataIndex: 'consecutiveTriggerCount',
      key: 'consecutiveTriggerCount',
      width: 90,
      render: (count: number, record: AlertRule) => (
        <Tooltip title={`已连续触发 ${count || 0} 次`}>
          <Space>
            <FireOutlined style={{ color: count > 0 ? '#ff4d4f' : '#999' }} />
            <Text strong={count > 0} style={{ color: count > 0 ? '#ff4d4f' : undefined }}>
              {count || 0}
            </Text>
          </Space>
        </Tooltip>
      )
    },
    {
      title: '已升级',
      dataIndex: 'escalationLevel',
      key: 'escalationLevel',
      width: 80,
      render: (level: number, record: AlertRule) => (
        <Tooltip title={record.escalationEnabled ? `已升级 ${level || 0} 次` : '未启用升级策略'}>
          <Space>
            <RiseOutlined style={{ color: (level || 0) > 0 ? '#fa8c16' : '#999' }} />
            <Text strong={(level || 0) > 0} style={{ color: (level || 0) > 0 ? '#fa8c16' : undefined }}>
              {level || 0}
            </Text>
          </Space>
        </Tooltip>
      )
    },
    {
      title: '状态',
      dataIndex: 'isEnabled',
      key: 'isEnabled',
      width: 100,
      render: (enabled: boolean, record: AlertRule) => (
        <Switch
          checked={enabled}
          disabled={record.status === 'INVALID'}
          onChange={(checked) => handleToggleEnabled(record, checked)}
          checkedChildren="启用"
          unCheckedChildren="禁用"
        />
      )
    },
    {
      title: '创建人',
      dataIndex: 'createdBy',
      key: 'createdBy',
      width: 100
    },
    {
      title: '操作',
      key: 'actions',
      width: 240,
      fixed: 'right' as const,
      render: (_: any, record: AlertRule) => (
        <Space size="small">
          <Tooltip title="查看详情">
            <Button
              type="text"
              size="small"
              icon={<EyeOutlined />}
              onClick={() => handleViewDetail(record)}
            />
          </Tooltip>
          <Tooltip title="编辑">
            <Button
              type="text"
              size="small"
              icon={<EditOutlined />}
              disabled={record.status === 'INVALID'}
              onClick={() => handleEdit(record)}
            />
          </Tooltip>
          <Tooltip title={record.subscriberCount || 0 > 0 ? '已订阅' : '订阅'}>
            <Button
              type="text"
              size="small"
              icon={<BellOutlined />}
              onClick={() => handleSubscribe(record, !(record.subscriberCount || 0 > 0))}
            >
              {record.subscriberCount || 0 > 0 ? '已订阅' : '订阅'}
            </Button>
          </Tooltip>
          <Popconfirm
            title="确定要删除这条规则吗？"
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

  const displayRules = activeTab === 'subscribed' ? subscribedRules : rules
  const totalCount = activeTab === 'subscribed' ? subscribedTotal : rules.length

  return (
    <div>
      <Card
        title="告警规则管理"
        extra={
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={handleCreate}
          >
            创建规则
          </Button>
        }
      >
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          style={{ marginBottom: 16 }}
        >
          <TabPane tab={`全部规则 (${rules.length})`} key="all" />
          <TabPane tab={`我订阅的 (${subscribedTotal})`} key="subscribed" />
        </Tabs>

        <Table
          columns={columns}
          dataSource={displayRules}
          rowKey="id"
          loading={loading}
          pagination={{
            pageSize: 20,
            showSizeChanger: true,
            showTotal: (total) => `共 ${totalCount} 条规则`
          }}
          scroll={{ x: 1000 }}
        />
      </Card>

      <Modal
        title={editingRule ? '编辑告警规则' : '创建告警规则'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        width={800}
        okText="保存"
        cancelText="取消"
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={{
            triggerType: 'VALUE',
            operator: 'GREATER_THAN',
            checkInterval: 'EVERY_HOUR',
            silencePeriod: 300,
            severity: 'WARNING',
            isEnabled: true
          }}
        >
          <Row gutter={16}>
            <Col span={24}>
              <Form.Item
                name="name"
                label="规则名称"
                rules={[{ required: true, message: '请输入规则名称' }]}
              >
                <Input placeholder="请输入规则名称" maxLength={100} />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="description" label="描述">
                <TextArea placeholder="请输入规则描述" rows={2} maxLength={500} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="dataModelId"
                label="数据模型"
                rules={[{ required: true, message: '请选择数据模型' }]}
              >
                <Select
                  placeholder="请选择数据模型"
                  disabled={!!editingRule}
                  onChange={handleDataModelChange}
                  showSearch
                  optionFilterProp="children"
                >
                  {dataModels.map(dm => (
                    <Option key={dm.id} value={dm.id}>
                      {dm.name}
                    </Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="measureId"
                label="度量指标"
                rules={[{ required: true, message: '请选择度量指标' }]}
              >
                <Select
                  placeholder="请选择度量指标"
                  disabled={!!editingRule}
                  showSearch
                  optionFilterProp="children"
                >
                  {measures.map(m => (
                    <Option key={m.id} value={m.id}>
                      {m.displayName || m.name}
                    </Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={24}>
              <Divider orientation="left">触发条件</Divider>
            </Col>
            <Col span={12}>
              <Form.Item
                name="triggerType"
                label="触发类型"
                rules={[{ required: true }]}
              >
                <Select>
                  <Option value="VALUE">绝对值阈值</Option>
                  <Option value="RELATIVE_CHANGE">环比变化</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="operator"
                label="比较方式"
                rules={[{ required: true }]}
              >
                <Select>
                  <Form.Item noStyle shouldUpdate>
                    {({ getFieldValue }) => {
                      const triggerType = getFieldValue('triggerType')
                      if (triggerType === 'RELATIVE_CHANGE') {
                        return (
                          <>
                            <Option value="INCREASE_PERCENT">环比上涨超过</Option>
                            <Option value="DECREASE_PERCENT">环比下跌超过</Option>
                            <Option value="CHANGE_PERCENT">环比变化绝对值超过</Option>
                          </>
                        )
                      }
                      return (
                        <>
                          <Option value="GREATER_THAN">大于 {'>'}</Option>
                          <Option value="GREATER_THAN_OR_EQUAL">大于等于 (≥)</Option>
                          <Option value="LESS_THAN">小于 {'<'}</Option>
                          <Option value="LESS_THAN_OR_EQUAL">小于等于 (≤)</Option>
                          <Option value="EQUAL">等于 (=)</Option>
                          <Option value="NOT_EQUAL">不等于 (≠)</Option>
                        </>
                      )
                    }}
                  </Form.Item>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="threshold"
                label="阈值"
                rules={[{ required: true, message: '请输入阈值' }]}
              >
                <InputNumber
                  style={{ width: '100%' }}
                  placeholder="请输入阈值"
                  step="0.01"
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="checkInterval"
                label="检测周期"
                rules={[{ required: true }]}
              >
                <Select>
                  <Option value="EVERY_5_MINUTES">每5分钟</Option>
                  <Option value="EVERY_15_MINUTES">每15分钟</Option>
                  <Option value="EVERY_30_MINUTES">每30分钟</Option>
                  <Option value="EVERY_HOUR">每小时</Option>
                  <Option value="EVERY_6_HOURS">每6小时</Option>
                  <Option value="EVERY_12_HOURS">每12小时</Option>
                  <Option value="EVERY_DAY">每天</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="silencePeriod"
                label="沉默期 (秒)"
                tooltip="触发后多久内不重复告警"
                rules={[{ required: true }]}
              >
                <Select>
                  <Option value={60}>1分钟</Option>
                  <Option value={300}>5分钟</Option>
                  <Option value={600}>10分钟</Option>
                  <Option value={1800}>30分钟</Option>
                  <Option value={3600}>1小时</Option>
                  <Option value={7200}>2小时</Option>
                  <Option value={86400}>1天</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="severity"
                label="严重等级"
                rules={[{ required: true }]}
              >
                <Select>
                  <Option value="INFO">
                    <Tag color="blue">信息</Tag>
                  </Option>
                  <Option value="WARNING">
                    <Tag color="orange">警告</Tag>
                  </Option>
                  <Option value="CRITICAL">
                    <Tag color="red">严重</Tag>
                  </Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="isEnabled"
                label="启用规则"
                valuePropName="checked"
              >
                <Switch />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Divider orientation="left">
                <Space>
                  <ArrowUpOutlined />
                  告警升级策略
                </Space>
              </Divider>

              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                    name="escalationEnabled"
                    label="启用自动升级"
                    valuePropName="checked"
                    tooltip="当告警连续触发且无人确认时，自动提升严重等级"
                  >
                    <Switch />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item
                    name="escalationThreshold"
                    label="连续触发次数阈值"
                    tooltip="连续触发多少次后自动升级"
                    rules={[{ required: false }]}
                  >
                    <Select>
                      <Option value={2}>2 次</Option>
                      <Option value={3}>3 次</Option>
                      <Option value={5}>5 次</Option>
                      <Option value={10}>10 次</Option>
                    </Select>
                  </Form.Item>
                </Col>
                <Col span={24}>
                  <Form.Item noStyle shouldUpdate>
                    {({ getFieldValue }) => {
                      const escalationEnabled = getFieldValue('escalationEnabled')
                      if (!escalationEnabled) return null
                      return (
                        <Card size="small" title="升级接收人">
                          <Paragraph type="secondary" style={{ marginBottom: 12 }}>
                            告警升级时将向以下用户发送站内信通知（可选择不在原订阅列表中的用户）
                          </Paragraph>
                          <Select
                            mode="multiple"
                            placeholder="请选择升级接收人"
                            value={escalationRecipients}
                            onChange={setEscalationRecipients}
                            style={{ width: '100%' }}
                            showSearch
                            optionFilterProp="children"
                          >
                            {users.map(user => (
                              <Option key={user.id} value={user.id}>
                                {user.username} ({user.email})
                              </Option>
                            ))}
                          </Select>
                        </Card>
                      )
                    }}
                  </Form.Item>
                </Col>
              </Row>
            </Col>
            <Col span={24}>
              <Divider orientation="left">
                <Space>
                  <SettingOutlined />
                  通知渠道
                </Space>
              </Divider>

              <Space direction="vertical" style={{ width: '100%' }} size="middle">
                {channels.map(channel => (
                  <Card
                    key={channel.type}
                    size="small"
                    title={
                      <Space>
                        {channel.type === 'IN_APP' && <BellOutlined />}
                        {channel.type === 'EMAIL' && <InfoCircleOutlined />}
                        {channel.type === 'WEBHOOK' && <LinkOutlined />}
                        {channel.type === 'IN_APP' ? '站内信' :
                         channel.type === 'EMAIL' ? '邮件' : 'Webhook'}
                        <Switch
                          size="small"
                          checked={channel.enabled !== false}
                          onChange={(checked) => toggleChannelEnabled(channel.type, checked)}
                        />
                      </Space>
                    }
                    extra={
                      channel.type !== 'IN_APP' ? (
                        <Button
                          type="text"
                          danger
                          size="small"
                          icon={<DeleteOutlined />}
                          onClick={() => removeChannel(channel.type)}
                        />
                      ) : null
                    }
                  >
                    {channel.type === 'EMAIL' && (
                      <Paragraph type="secondary" style={{ margin: 0 }}>
                        将向所有订阅用户的注册邮箱发送告警通知（仅记录到待发送队列）
                      </Paragraph>
                    )}
                    {channel.type === 'WEBHOOK' && (
                      <Space direction="vertical" style={{ width: '100%' }}>
                        <Form.Item
                          label="Webhook URL"
                          style={{ marginBottom: 8 }}
                        >
                          <Input
                            placeholder="https://example.com/webhook"
                            value={channel.config?.url || ''}
                            onChange={(e) => updateChannelConfig(channel.type, {
                              ...channel.config,
                              url: e.target.value
                            })}
                          />
                        </Form.Item>
                        <Form.Item
                          label="请求头 (JSON格式)"
                          style={{ marginBottom: 0 }}
                        >
                          <TextArea
                            placeholder='{"Authorization": "Bearer xxx"}'
                            value={JSON.stringify(channel.config?.headers || {}, null, 2)}
                            onChange={(e) => {
                              try {
                                const headers = JSON.parse(e.target.value)
                                updateChannelConfig(channel.type, {
                                  ...channel.config,
                                  headers
                                })
                              } catch (err) {
                                // 允许用户输入不完整的JSON
                              }
                            }}
                            rows={3}
                            style={{ fontFamily: 'monospace' }}
                          />
                        </Form.Item>
                      </Space>
                    )}
                  </Card>
                ))}

                <Space>
                  <Button
                    icon={<PlusOutlined />}
                    onClick={() => addChannel('EMAIL')}
                    disabled={channels.some(c => c.type === 'EMAIL')}
                  >
                    添加邮件通知
                  </Button>
                  <Button
                    icon={<PlusOutlined />}
                    onClick={() => addChannel('WEBHOOK')}
                    disabled={channels.some(c => c.type === 'WEBHOOK')}
                  >
                    添加Webhook
                  </Button>
                </Space>
              </Space>
            </Col>
          </Row>
        </Form>
      </Modal>

      <Modal
        title={`规则详情 - ${selectedRule?.name}`}
        open={detailModalVisible}
        onCancel={() => setDetailModalVisible(false)}
        footer={null}
        width={1000}
      >
        {selectedRule && (
          <div>
            <Descriptions column={2} bordered size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="规则名称">
                {selectedRule.name}
              </Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={selectedRule.isEnabled ? 'green' : 'default'}>
                  {selectedRule.isEnabled ? '已启用' : '已禁用'}
                </Tag>
                {selectedRule.status === 'INVALID' && (
                  <Tag color="default">已失效</Tag>
                )}
              </Descriptions.Item>
              <Descriptions.Item label="数据模型">
                {selectedRule.dataModelName}
              </Descriptions.Item>
              <Descriptions.Item label="度量指标">
                {selectedRule.measureName}
              </Descriptions.Item>
              <Descriptions.Item label="触发条件">
                {getOperatorText(selectedRule.operator, selectedRule.threshold, selectedRule.triggerType)}
              </Descriptions.Item>
              <Descriptions.Item label="检测周期">
                {getIntervalText(selectedRule.checkInterval)}
              </Descriptions.Item>
              <Descriptions.Item label="严重等级">
                <Tag color={getSeverityColor(selectedRule.severity)}>
                  {getSeverityText(selectedRule.severity)}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="创建人">
                {selectedRule.createdBy}
              </Descriptions.Item>
              <Descriptions.Item label="创建时间">
                {new Date(selectedRule.createdAt).toLocaleString('zh-CN')}
              </Descriptions.Item>
              <Descriptions.Item label="上次触发">
                {selectedRule.lastTriggeredAt ?
                  new Date(selectedRule.lastTriggeredAt).toLocaleString('zh-CN') :
                  '从未触发'}
              </Descriptions.Item>
              <Descriptions.Item label="连续触发次数">
                <Tag color={selectedRule.consecutiveTriggerCount ? 'red' : 'default'}>
                  {selectedRule.consecutiveTriggerCount || 0} 次
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="已升级次数">
                <Tag color={selectedRule.escalationLevel ? 'orange' : 'default'}>
                  {selectedRule.escalationLevel || 0} 次
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="当前严重等级">
                <Tag color={getSeverityColor(selectedRule.currentSeverity || selectedRule.severity)}>
                  {getSeverityText(selectedRule.currentSeverity || selectedRule.severity)}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="升级策略">
                {selectedRule.escalationEnabled ? (
                  <Space>
                    <Tag color="green">已启用</Tag>
                    <Text type="secondary">
                      连续 {selectedRule.escalationThreshold} 次触发后升级
                    </Text>
                  </Space>
                ) : (
                  <Tag color="default">未启用</Tag>
                )}
              </Descriptions.Item>
              {selectedRule.escalationEnabled && selectedRule.escalationRecipients && (
                <Descriptions.Item label="升级接收人" span={2}>
                  <Space wrap>
                    {selectedRule.escalationRecipients.map(r => (
                      <Tag key={r.userId} color="blue">
                        {r.username} ({r.email})
                      </Tag>
                    ))}
                  </Space>
                </Descriptions.Item>
              )}
            </Descriptions>

            <Divider orientation="left">告警事件历史</Divider>
            {ruleEvents.length === 0 ? (
              <Empty description="暂无告警事件" />
            ) : (
              <List
                dataSource={ruleEvents}
                loading={eventsLoading}
                renderItem={event => (
                  <List.Item
                    actions={[
                      <Button
                        type="link"
                        size="small"
                        onClick={() => setShowEventDetail(event)}
                      >
                        查看详情
                      </Button>
                    ]}
                  >
                    <List.Item.Meta
                      title={
                        <Space>
                          <Text strong>{event.alertRuleName}</Text>
                          <Tag color={getSeverityColor(event.severity)}>
                            {getSeverityText(event.severity)}
                          </Tag>
                          <Tag color={
                            event.eventStatus === 'FIRING' ? 'red' :
                            event.eventStatus === 'RESOLVED' ? 'green' : 'blue'
                          }>
                            {event.eventStatus === 'FIRING' ? '告警中' :
                             event.eventStatus === 'RESOLVED' ? '已恢复' : '已确认'}
                          </Tag>
                        </Space>
                      }
                      description={
                        <div>
                          <Text>触发值: {event.triggerValue}</Text>
                          <Divider type="vertical" />
                          <Text>阈值: {event.threshold}</Text>
                          {event.changePercent != null && (
                            <>
                              <Divider type="vertical" />
                              <Text>变化率: {event.changePercent.toFixed(2)}%</Text>
                            </>
                          )}
                          <Divider type="vertical" />
                          <Text type="secondary">
                            {new Date(event.triggeredAt).toLocaleString('zh-CN')}
                          </Text>
                        </div>
                      }
                    />
                  </List.Item>
                )}
              />
            )}
          </div>
        )}
      </Modal>

      {showEventDetail && (
        <AlertEventDetail
          eventId={showEventDetail.id}
          visible={!!showEventDetail}
          onClose={() => setShowEventDetail(null)}
        />
      )}
    </div>
  )
}

export default AlertRuleManagement
