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
  message,
  Popconfirm,
  Tag,
  Card,
  Row,
  Col,
  Descriptions,
  Drawer,
  Alert
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  SafetyOutlined,
  ReloadOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined
} from '@ant-design/icons'
import type { RowPermissionRule, Dashboard, DashboardPermission, User } from '@/types'
import { permissionApi, dashboardApi, userApi } from '@/api'
import { formatDate } from '@/utils'

const { Option } = Select

const PermissionManagement: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'row' | 'dashboard'>('row')
  const [rowPermissions, setRowPermissions] = useState<RowPermissionRule[]>([])
  const [dashboardPermissions, setDashboardPermissions] = useState<Record<string, DashboardPermission[]>>({})
  const [dashboards, setDashboards] = useState<Dashboard[]>([])
  const [users, setUsers] = useState<User[]>([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingItem, setEditingItem] = useState<RowPermissionRule | null>(null)
  const [form] = Form.useForm()
  const [validating, setValidating] = useState(false)

  const loadRowPermissions = async () => {
    try {
      const data = await permissionApi.getRowPermissions()
      setRowPermissions(data)
    } catch (error) {
      console.error('Failed to load row permissions:', error)
    }
  }

  const loadDashboardPermissions = async (dashboardId: string) => {
    try {
      const data = await permissionApi.getDashboardPermissions(dashboardId)
      setDashboardPermissions(prev => ({
        ...prev,
        [dashboardId]: data as DashboardPermission[]
      }))
    } catch (error) {
      console.error('Failed to load dashboard permissions:', error)
    }
  }

  const loadData = async () => {
    try {
      setLoading(true)
      const [perms, dashData, userData] = await Promise.all([
        permissionApi.getRowPermissions(),
        dashboardApi.getList(),
        userApi.getList()
      ])
      setRowPermissions(perms)
      setDashboards(dashData)
      setUsers(userData)
    } catch (error) {
      console.error('Failed to load data:', error)
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
      isEnabled: true,
      operator: 'EQ',
      valueType: 'STATIC',
      userRoles: ['VIEWER']
    })
    setModalVisible(true)
  }

  const handleEdit = (record: RowPermissionRule) => {
    setEditingItem(record)
    form.setFieldsValue(record)
    setModalVisible(true)
  }

  const handleDelete = async (id: string) => {
    try {
      await permissionApi.deleteRowPermission(id)
      message.success('删除成功')
      loadRowPermissions()
    } catch (error) {
      console.error('Failed to delete permission:', error)
    }
  }

  const handleValidate = async () => {
    try {
      const values = await form.validateFields()
      setValidating(true)
      const result = await permissionApi.validateRule(values)
      if (result.valid) {
        message.success('规则校验通过')
      } else {
        message.error('规则校验失败: ' + result.message)
      }
    } catch (error) {
      console.error('Validation failed:', error)
    } finally {
      setValidating(false)
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()

      if (editingItem) {
        await permissionApi.updateRowPermission(editingItem.id, values)
        message.success('更新成功')
      } else {
        await permissionApi.createRowPermission(values)
        message.success('创建成功')
      }

      setModalVisible(false)
      loadRowPermissions()
    } catch (error) {
      console.error('Failed to submit permission:', error)
    }
  }

  const handleSetDashboardPermission = async (dashboardId: string, userId: string, permission: string) => {
    try {
      await permissionApi.setDashboardPermission(dashboardId, userId, permission)
      message.success('权限设置成功')
      loadDashboardPermissions(dashboardId)
    } catch (error) {
      console.error('Failed to set dashboard permission:', error)
    }
  }

  const handleRemoveDashboardPermission = async (dashboardId: string, userId: string) => {
    try {
      await permissionApi.removeDashboardPermission(dashboardId, userId)
      message.success('权限移除成功')
      loadDashboardPermissions(dashboardId)
    } catch (error) {
      console.error('Failed to remove permission:', error)
    }
  }

  const operatorLabels: Record<string, string> = {
    EQ: '等于',
    NE: '不等于',
    GT: '大于',
    GTE: '大于等于',
    LT: '小于',
    LTE: '小于等于',
    IN: '包含',
    LIKE: '模糊匹配'
  }

  const valueTypeLabels: Record<string, string> = {
    STATIC: '静态值',
    USER_ATTRIBUTE: '用户属性',
    EXPRESSION: '表达式'
  }

  const roleLabels: Record<string, string> = {
    ADMIN: '管理员',
    EDITOR: '编辑者',
    VIEWER: '查看者'
  }

  const permissionLabels: Record<string, string> = {
    VIEW: '查看',
    EDIT: '编辑',
    OWNER: '所有者'
  }

  const permissionColors: Record<string, string> = {
    VIEW: 'green',
    EDIT: 'blue',
    OWNER: 'purple'
  }

  const rowColumns = [
    {
      title: '规则名称',
      dataIndex: 'name',
      key: 'name',
      render: (text: string, record: RowPermissionRule) => (
        <Space>
          <SafetyOutlined />
          <span>{text}</span>
        </Space>
      )
    },
    {
      title: '数据范围',
      key: 'scope',
      render: (_: any, record: RowPermissionRule) => (
        <span>
          <Tag color="blue">{record.tableName}</Tag>
          .
          <Tag color="cyan">{record.columnName}</Tag>
        </span>
      )
    },
    {
      title: '条件',
      key: 'condition',
      render: (_: any, record: RowPermissionRule) => (
        <Space>
          <Tag>{operatorLabels[record.operator]}</Tag>
          <Tag color="orange">{valueTypeLabels[record.valueType]}</Tag>
          <code>{record.value}</code>
        </Space>
      )
    },
    {
      title: '适用角色',
      dataIndex: 'userRoles',
      key: 'userRoles',
      render: (roles: string[]) => (
        <Space wrap>
          {roles.map(role => (
            <Tag key={role}>{roleLabels[role] || role}</Tag>
          ))}
        </Space>
      )
    },
    {
      title: '状态',
      dataIndex: 'isEnabled',
      key: 'isEnabled',
      width: 100,
      render: (enabled: boolean, record: RowPermissionRule) => (
        <Switch
          checked={enabled}
          onChange={(checked) => permissionApi.updateRowPermission(record.id, { isEnabled: checked })}
          size="small"
        />
      )
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (date: string) => formatDate(date)
    },
    {
      title: '操作',
      key: 'actions',
      width: 150,
      render: (_: any, record: RowPermissionRule) => (
        <Space size="small">
          <Button
            type="text"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          />
          <Popconfirm
            title="确定要删除这个权限规则吗？"
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

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">权限管理</h1>
        <div className="page-actions">
          <Button
            icon={<ReloadOutlined />}
            onClick={loadData}
            loading={loading}
          >
            刷新
          </Button>
          {activeTab === 'row' && (
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={handleCreate}
            >
              新建行级权限
            </Button>
          )}
        </div>
      </div>

      <Alert
        message="安全提示"
        description="行级权限使用参数化查询注入，防止SQL注入。配置错误的规则在保存时会被校验并拒绝。"
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
      />

      <Card
        tabList={[
          { key: 'row' as const, tab: '行级数据权限' },
          { key: 'dashboard' as const, tab: '仪表板权限' }
        ]}
        activeTabKey={activeTab}
        onTabChange={(key) => setActiveTab(key as 'row' | 'dashboard')}
      >
        {activeTab === 'row' ? (
          <Table
            dataSource={rowPermissions}
            columns={rowColumns}
            rowKey="id"
            loading={loading}
            pagination={{
              pageSize: 10,
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total) => `共 ${total} 条`
            }}
          />
        ) : (
          <Row gutter={[16, 16]}>
            {dashboards.map(dashboard => (
              <Col span={12} key={dashboard.id}>
                <Card
                  size="small"
                  title={
                    <Space>
                      <span>{dashboard.name}</span>
                      {dashboard.isPublished && <Tag color="success">已发布</Tag>}
                    </Space>
                  }
                  extra={
                    <Button
                      type="text"
                      size="small"
                      onClick={() => loadDashboardPermissions(dashboard.id)}
                    >
                      <ReloadOutlined />
                    </Button>
                  }
                >
                  <div style={{ marginBottom: 12 }}>
                    <Select
                      placeholder="添加用户权限"
                      style={{ width: '100%' }}
                      onSelect={(userId) => {
                        handleSetDashboardPermission(dashboard.id, userId as string, 'VIEW')
                      }}
                      filterOption={(input, option) =>
                        String(option?.label ?? '').toLowerCase().includes(input.toLowerCase())
                      }
                      options={users.filter(u =>
                        !dashboardPermissions[dashboard.id]?.some(p => p.userId === u.id)
                      ).map(u => ({
                        label: `${u.username} (${u.email})`,
                        value: u.id
                      }))}
                    />
                  </div>

                  {dashboardPermissions[dashboard.id]?.length > 0 ? (
                    <Space direction="vertical" size={8} style={{ width: '100%' }}>
                      {dashboardPermissions[dashboard.id].map(perm => (
                        <div
                          key={perm.userId}
                          style={{
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center',
                            padding: '8px 12px',
                            background: '#fafafa',
                            borderRadius: 4
                          }}
                        >
                          <Space>
                            <span>{perm.userName}</span>
                            <Select
                              value={perm.permission}
                              size="small"
                              style={{ width: 80 }}
                              onChange={(value) => handleSetDashboardPermission(dashboard.id, perm.userId, value)}
                            >
                              {Object.entries(permissionLabels).map(([key, label]) => (
                                <Option key={key} value={key}>
                                  <Tag color={permissionColors[key]}>{label}</Tag>
                                </Option>
                              ))}
                            </Select>
                          </Space>
                          <Popconfirm
                            title="确定要移除这个权限吗？"
                            onConfirm={() => handleRemoveDashboardPermission(dashboard.id, perm.userId)}
                            okText="确定"
                            cancelText="取消"
                          >
                            <Button type="text" danger size="small" icon={<DeleteOutlined />} />
                          </Popconfirm>
                        </div>
                      ))}
                    </Space>
                  ) : (
                    <div style={{ textAlign: 'center', color: '#8c8c8c', padding: '16px 0' }}>
                      暂无权限配置
                    </div>
                  )}
                </Card>
              </Col>
            ))}
          </Row>
        )}
      </Card>

      <Modal
        title={editingItem ? '编辑行级权限' : '新建行级权限'}
        open={modalVisible}
        width={600}
        onCancel={() => setModalVisible(false)}
        footer={[
          <Button key="validate" onClick={handleValidate} loading={validating}>
            校验规则
          </Button>,
          <Button key="cancel" onClick={() => setModalVisible(false)}>取消</Button>,
          <Button key="submit" type="primary" onClick={handleSubmit}>保存</Button>
        ]}
      >
        <Form form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="name"
                label="规则名称"
                rules={[{ required: true, message: '请输入规则名称' }]}
              >
                <Input placeholder="请输入规则名称" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="valueType"
                label="值类型"
                rules={[{ required: true, message: '请选择值类型' }]}
              >
                <Select>
                  {Object.entries(valueTypeLabels).map(([key, label]) => (
                    <Option key={key} value={key}>{label}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="description"
            label="描述"
          >
            <Input.TextArea rows={2} placeholder="请输入规则描述" />
          </Form.Item>

          <Row gutter={16}>
            <Col span={8}>
              <Form.Item
                name="tableName"
                label="表名"
                rules={[{ required: true, message: '请输入表名' }]}
              >
                <Input placeholder="如: orders" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                name="columnName"
                label="字段名"
                rules={[{ required: true, message: '请输入字段名' }]}
              >
                <Input placeholder="如: region" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                name="operator"
                label="操作符"
                rules={[{ required: true, message: '请选择操作符' }]}
              >
                <Select>
                  {Object.entries(operatorLabels).map(([key, label]) => (
                    <Option key={key} value={key}>{label}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="value"
            label="值"
            rules={[{ required: true, message: '请输入值' }]}
            extra="静态值直接填写，用户属性如: ${user.region}，表达式如: YEAR(NOW())"
          >
            <Input placeholder="如: 华东 或 ${user.region}" />
          </Form.Item>

          <Form.Item
            name="userRoles"
            label="适用角色"
            rules={[{ required: true, message: '请选择适用角色' }]}
          >
            <Select mode="multiple" placeholder="选择适用的角色">
              {Object.entries(roleLabels).map(([key, label]) => (
                <Option key={key} value={key}>{label}</Option>
              ))}
            </Select>
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
    </div>
  )
}

export default PermissionManagement
