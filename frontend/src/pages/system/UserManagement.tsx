import React, { useState, useEffect } from 'react'
import {
  Table,
  Button,
  Space,
  Modal,
  Form,
  Input,
  Select,
  Tag,
  message,
  Popconfirm,
  Card,
  Row,
  Col,
  Statistic,
  Switch,
  Avatar
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ReloadOutlined,
  UserOutlined,
  KeyOutlined,
  SafetyCertificateOutlined
} from '@ant-design/icons'
import type { User } from '@/types'
import { userApi } from '@/api'
import { formatDate, getRoleName, getStatusColor } from '@/utils'

const { Option } = Select

const UserManagement: React.FC = () => {
  const [users, setUsers] = useState<User[]>([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [passwordModalVisible, setPasswordModalVisible] = useState(false)
  const [editingItem, setEditingItem] = useState<User | null>(null)
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null)
  const [form] = Form.useForm()
  const [passwordForm] = Form.useForm()

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

  const handleCreate = () => {
    setEditingItem(null)
    form.resetFields()
    form.setFieldsValue({
      role: 'VIEWER',
      status: 'ACTIVE'
    })
    setModalVisible(true)
  }

  const handleEdit = (record: User) => {
    setEditingItem(record)
    form.setFieldsValue({
      ...record,
      password: ''
    })
    setModalVisible(true)
  }

  const handleDelete = async (id: string) => {
    try {
      await userApi.delete(id)
      message.success('删除成功')
      loadData()
    } catch (error) {
      console.error('Failed to delete user:', error)
    }
  }

  const handleResetPassword = (userId: string) => {
    setSelectedUserId(userId)
    passwordForm.resetFields()
    setPasswordModalVisible(true)
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      
      if (editingItem) {
        const { password, ...updateData } = values
        await userApi.update(editingItem.id, updateData)
        message.success('更新成功')
      } else {
        await userApi.create(values)
        message.success('创建成功')
      }
      
      setModalVisible(false)
      loadData()
    } catch (error) {
      console.error('Failed to submit user:', error)
    }
  }

  const handlePasswordSubmit = async () => {
    try {
      const values = await passwordForm.validateFields()
      if (selectedUserId) {
        await userApi.resetPassword(selectedUserId, values.newPassword)
        message.success('密码重置成功')
        setPasswordModalVisible(false)
      }
    } catch (error) {
      console.error('Failed to reset password:', error)
    }
  }

  const handleToggleStatus = async (id: string, active: boolean) => {
    try {
      await userApi.update(id, { status: active ? 'ACTIVE' : 'INACTIVE' })
      message.success(active ? '用户已启用' : '用户已禁用')
      loadData()
    } catch (error) {
      console.error('Failed to toggle user status:', error)
    }
  }

  const columns = [
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
      render: (text: string) => (
        <Space>
          <Avatar size="small" icon={<UserOutlined />} />
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
      render: (role: string) => (
        <Tag color={role === 'ADMIN' ? 'red' : role === 'EDITOR' ? 'blue' : 'green'}>
          {getRoleName(role)}
        </Tag>
      )
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string, record: User) => (
        <Switch
          checked={status === 'ACTIVE'}
          onChange={(checked) => handleToggleStatus(record.id, checked)}
          checkedChildren="启用"
          unCheckedChildren="禁用"
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
      width: 200,
      render: (_: any, record: User) => (
        <Space size="small">
          <Button
            type="text"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          />
          <Button
            type="text"
            size="small"
            icon={<KeyOutlined />}
            onClick={() => handleResetPassword(record.id)}
          />
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
            />
          </Popconfirm>
        </Space>
      )
    }
  ]

  const stats = {
    total: users.length,
    admin: users.filter(u => u.role === 'ADMIN').length,
    editor: users.filter(u => u.role === 'EDITOR').length,
    viewer: users.filter(u => u.role === 'VIEWER').length,
    active: users.filter(u => u.status === 'ACTIVE').length
  }

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">用户管理</h1>
        <div className="page-actions">
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={handleCreate}
          >
            新建用户
          </Button>
          <Button
            icon={<ReloadOutlined />}
            onClick={loadData}
            loading={loading}
          >
            刷新
          </Button>
        </div>
      </div>

      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={4}>
          <Card>
            <Statistic
              title="总用户数"
              value={stats.total}
              prefix={<UserOutlined />}
            />
          </Card>
        </Col>
        <Col span={4}>
          <Card>
            <Statistic
              title="管理员"
              value={stats.admin}
              valueStyle={{ color: '#ff4d4f' }}
              prefix={<SafetyCertificateOutlined />}
            />
          </Card>
        </Col>
        <Col span={4}>
          <Card>
            <Statistic
              title="编辑者"
              value={stats.editor}
              valueStyle={{ color: '#1677ff' }}
            />
          </Card>
        </Col>
        <Col span={4}>
          <Card>
            <Statistic
              title="查看者"
              value={stats.viewer}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={4}>
          <Card>
            <Statistic
              title="已启用"
              value={stats.active}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={4}>
          <Card>
            <Statistic
              title="已禁用"
              value={stats.total - stats.active}
              valueStyle={{ color: '#8c8c8c' }}
            />
          </Card>
        </Col>
      </Row>

      <Card>
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

      <Modal
        title={editingItem ? '编辑用户' : '新建用户'}
        open={modalVisible}
        width={500}
        onCancel={() => setModalVisible(false)}
        footer={[
          <Button key="cancel" onClick={() => setModalVisible(false)}>取消</Button>,
          <Button key="submit" type="primary" onClick={handleSubmit}>
            {editingItem ? '保存' : '创建'}
          </Button>
        ]}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="username"
            label="用户名"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input placeholder="请输入用户名" disabled={!!editingItem} />
          </Form.Item>
          <Form.Item
            name="email"
            label="邮箱"
            rules={[
              { required: true, message: '请输入邮箱' },
              { type: 'email', message: '请输入有效的邮箱地址' }
            ]}
          >
            <Input placeholder="请输入邮箱" />
          </Form.Item>
          {!editingItem && (
            <Form.Item
              name="password"
              label="密码"
              rules={[
                { required: true, message: '请输入密码' },
                { min: 6, message: '密码至少6位' }
              ]}
            >
              <Input.Password placeholder="请输入密码" />
            </Form.Item>
          )}
          <Form.Item
            name="role"
            label="角色"
            rules={[{ required: true, message: '请选择角色' }]}
          >
            <Select>
              <Option value="ADMIN">管理员</Option>
              <Option value="EDITOR">编辑者</Option>
              <Option value="VIEWER">查看者</Option>
            </Select>
          </Form.Item>
          <Form.Item
            name="status"
            label="状态"
            rules={[{ required: true, message: '请选择状态' }]}
          >
            <Select>
              <Option value="ACTIVE">启用</Option>
              <Option value="INACTIVE">禁用</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="重置密码"
        open={passwordModalVisible}
        width={400}
        onCancel={() => setPasswordModalVisible(false)}
        footer={[
          <Button key="cancel" onClick={() => setPasswordModalVisible(false)}>取消</Button>,
          <Button key="submit" type="primary" onClick={handlePasswordSubmit}>确认重置</Button>
        ]}
      >
        <Form form={passwordForm} layout="vertical">
          <Form.Item
            name="newPassword"
            label="新密码"
            rules={[
              { required: true, message: '请输入新密码' },
              { min: 6, message: '密码至少6位' }
            ]}
          >
            <Input.Password placeholder="请输入新密码" />
          </Form.Item>
          <Form.Item
            name="confirmPassword"
            label="确认密码"
            dependencies={['newPassword']}
            rules={[
              { required: true, message: '请再次输入密码' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('newPassword') === value) {
                    return Promise.resolve()
                  }
                  return Promise.reject(new Error('两次输入的密码不一致'))
                }
              })
            ]}
          >
            <Input.Password placeholder="请再次输入密码" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default UserManagement
