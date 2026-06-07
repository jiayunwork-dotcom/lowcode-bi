import React, { useState, useEffect } from 'react'
import {
  Table,
  Button,
  Space,
  Modal,
  Form,
  Input,
  Select,
  message,
  Popconfirm,
  Tag,
  Card,
  Switch,
  Descriptions,
  Drawer,
  Checkbox,
  Row,
  Col
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  KeyOutlined,
  UserOutlined,
  SettingOutlined,
  EyeOutlined
} from '@ant-design/icons'
import type { User } from '@/types'
import { userApi, permissionApi } from '@/api'
import { formatDate, getStatusColor, getStatusText } from '@/utils'

const { Option } = Select

const RoleManagement: React.FC = () => {
  const [users, setUsers] = useState<User[]>([])
  const [loading, setLoading] = useState(false)
  const [detailVisible, setDetailVisible] = useState(false)
  const [selectedUser, setSelectedUser] = useState<User | null>(null)
  const [permissions, setPermissions] = useState<any[]>([])

  const roleLabels: Record<string, string> = {
    ADMIN: '管理员',
    EDITOR: '编辑者',
    VIEWER: '查看者'
  }

  const roleColors: Record<string, string> = {
    ADMIN: 'red',
    EDITOR: 'blue',
    VIEWER: 'green'
  }

  const loadData = async () => {
    try {
      setLoading(true)
      const data = await userApi.getList()
      setUsers(data)
    } catch (error) {
      console.error('Failed to load users:', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [])

  const handleViewDetail = async (user: User) => {
    setSelectedUser(user)
    setDetailVisible(true)
    try {
      const perms = await permissionApi.getRowPermissions()
      setPermissions(perms.filter((p: any) => p.userRoles.includes(user.role)))
    } catch (error) {
      console.error('Failed to load permissions:', error)
    }
  }

  const handleUpdateRole = async (userId: string, role: 'ADMIN' | 'EDITOR' | 'VIEWER') => {
    try {
      await userApi.update(userId, { role })
      message.success('角色更新成功')
      loadData()
    } catch (error) {
      console.error('Failed to update role:', error)
      message.error('角色更新失败')
    }
  }

  const handleUpdateStatus = async (userId: string, status: 'ACTIVE' | 'INACTIVE') => {
    try {
      await userApi.update(userId, { status })
      message.success('状态更新成功')
      loadData()
    } catch (error) {
      console.error('Failed to update status:', error)
      message.error('状态更新失败')
    }
  }

  const handleDelete = async (userId: string) => {
    try {
      await userApi.delete(userId)
      message.success('删除成功')
      loadData()
    } catch (error) {
      console.error('Failed to delete user:', error)
      message.error('删除失败')
    }
  }

  const handleResetPassword = async (userId: string) => {
    try {
      const newPassword = Math.random().toString(36).slice(-8)
      await userApi.resetPassword(userId, newPassword)
      Modal.success({
        title: '密码重置成功',
        content: `新密码: ${newPassword}`,
      })
    } catch (error) {
      console.error('Failed to reset password:', error)
      message.error('密码重置失败')
    }
  }

  const columns = [
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
      render: (text: string, record: User) => (
        <Space>
          <UserOutlined />
          <span>{text}</span>
        </Space>
      )
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email'
    },
    {
      title: '角色',
      dataIndex: 'role',
      key: 'role',
      width: 120,
      render: (role: string, record: User) => (
        <Select
          value={role}
          size="small"
          style={{ width: 100 }}
          onChange={(value) => handleUpdateRole(record.id, value as 'ADMIN' | 'EDITOR' | 'VIEWER')}
        >
          <Option value="ADMIN">
            <Tag color={roleColors.ADMIN}>{roleLabels.ADMIN}</Tag>
          </Option>
          <Option value="EDITOR">
            <Tag color={roleColors.EDITOR}>{roleLabels.EDITOR}</Tag>
          </Option>
          <Option value="VIEWER">
            <Tag color={roleColors.VIEWER}>{roleLabels.VIEWER}</Tag>
          </Option>
        </Select>
      )
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string, record: User) => (
        <Space>
          <Switch
            checked={status === 'ACTIVE'}
            onChange={(checked) => handleUpdateStatus(record.id, checked ? 'ACTIVE' : 'INACTIVE')}
            size="small"
          />
          <Tag color={getStatusColor(status)}>{getStatusText(status)}</Tag>
        </Space>
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
      width: 240,
      render: (_: any, record: User) => (
        <Space size="small">
          <Button
            type="text"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => handleViewDetail(record)}
          >
            详情
          </Button>
          <Button
            type="text"
            size="small"
            icon={<KeyOutlined />}
            onClick={() => handleResetPassword(record.id)}
          >
            重置密码
          </Button>
          <Popconfirm
            title="确定要删除这个用户吗？"
            onConfirm={() => handleDelete(record.id)}
            okText="确定"
            cancelText="取消"
          >
            <Button
              type="text"
              size="small"
              danger
              icon={<DeleteOutlined />}
            >
              删除
            </Button>
          </Popconfirm>
        </Space>
      )
    }
  ]

  const rolePermissions: Record<string, string[]> = {
    ADMIN: ['全部权限'],
    EDITOR: ['查看仪表板', '编辑仪表板', '创建仪表板', '删除仪表板', '数据查询'],
    VIEWER: ['查看仪表板']
  }

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">角色与权限管理</h1>
      </div>

      <Row gutter={16} style={{ marginBottom: 16 }}>
        {Object.entries(roleLabels).map(([key, label]) => (
          <Col span={8} key={key}>
            <Card>
              <Card.Meta
                avatar={<Tag color={roleColors[key]} style={{ fontSize: 16, padding: '4px 12px' }}>{label}</Tag>}
                title={
                  <Space>
                    <SettingOutlined />
                    {label}
                  </Space>
                }
                description={
                  <div style={{ marginTop: 8 }}>
                    {rolePermissions[key].map((perm, idx) => (
                      <Tag key={idx} style={{ margin: 2 }}>{perm}</Tag>
                    ))}
                  </div>
                }
              />
            </Card>
          </Col>
        ))}
      </Row>

      <Card title="用户角色管理">
        <Table
          dataSource={users}
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

      <Drawer
        title={`用户详情 - ${selectedUser?.username}`}
        placement="right"
        width={600}
        open={detailVisible}
        onClose={() => setDetailVisible(false)}
      >
        {selectedUser && (
          <>
            <Descriptions column={1} bordered size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="用户名">{selectedUser.username}</Descriptions.Item>
              <Descriptions.Item label="邮箱">{selectedUser.email}</Descriptions.Item>
              <Descriptions.Item label="角色">
                <Tag color={roleColors[selectedUser.role]}>{roleLabels[selectedUser.role]}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={getStatusColor(selectedUser.status)}>{getStatusText(selectedUser.status)}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="创建时间">{formatDate(selectedUser.createdAt)}</Descriptions.Item>
              <Descriptions.Item label="更新时间">{formatDate(selectedUser.updatedAt)}</Descriptions.Item>
            </Descriptions>

            <Card title="拥有的权限" size="small">
              {rolePermissions[selectedUser.role]?.map((perm, idx) => (
                <Checkbox key={idx} checked disabled style={{ display: 'block', marginBottom: 8 }}>
                  {perm}
                </Checkbox>
              ))}
            </Card>

            {permissions.length > 0 && (
              <Card title="行级数据权限规则" size="small" style={{ marginTop: 16 }}>
                {permissions.map((perm, idx) => (
                  <div key={idx} style={{ padding: '8px 0', borderBottom: idx < permissions.length - 1 ? '1px solid #f0f0f0' : 'none' }}>
                    <Space direction="vertical" size={4}>
                      <Space>
                        <Tag color="blue">{perm.name}</Tag>
                        <span style={{ color: '#8c8c8c' }}>{perm.description}</span>
                      </Space>
                      <div style={{ fontSize: 12, color: '#595959' }}>
                        {perm.tableName}.{perm.columnName} {perm.operator} {perm.value}
                      </div>
                    </Space>
                  </div>
                ))}
              </Card>
            )}
          </>
        )}
      </Drawer>
    </div>
  )
}

export default RoleManagement
