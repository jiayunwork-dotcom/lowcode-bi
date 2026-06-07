import React, { useState } from 'react'
import { Form, Input, Button, Card, message, Tabs } from 'antd'
import { UserOutlined, LockOutlined, ApartmentOutlined, LoginOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import { authApi } from '@/api'

const Login: React.FC = () => {
  const [loading, setLoading] = useState(false)
  const [form] = Form.useForm()
  const navigate = useNavigate()
  const { login } = useAuthStore()

  const handleLogin = async (values: {
    username: string
    password: string
    tenantCode: string
  }) => {
    try {
      setLoading(true)
      const response = await authApi.login(values.username, values.password, values.tenantCode)
      login(response.token)
      message.success('登录成功')
      navigate('/dashboard')
    } catch (error) {
      console.error('Login failed:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleQuickLogin = async (role: string) => {
    const accounts: Record<string, { username: string; password: string; tenantCode: string }> = {
      ADMIN: { username: 'admin', password: 'admin123', tenantCode: 'default' },
      EDITOR: { username: 'editor', password: 'editor123', tenantCode: 'default' },
      VIEWER: { username: 'viewer', password: 'viewer123', tenantCode: 'default' }
    }
    
    const account = accounts[role]
    if (account) {
      form.setFieldsValue(account)
      handleLogin(account)
    }
  }

  return (
    <div className="auth-container">
      <Card className="auth-card" bordered={false}>
        <div className="auth-logo">
          <div className="icon">📊</div>
          <div className="title">低代码BI平台</div>
          <div className="subtitle">企业级数据分析与可视化平台</div>
        </div>

        <Form
          form={form}
          name="login"
          className="auth-form"
          initialValues={{ remember: true, tenantCode: 'default' }}
          onFinish={handleLogin}
          size="large"
        >
          <Form.Item
            name="tenantCode"
            rules={[{ required: true, message: '请输入租户编码' }]}
          >
            <Input
              prefix={<ApartmentOutlined />}
              placeholder="租户编码"
              autoComplete="off"
            />
          </Form.Item>

          <Form.Item
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input
              prefix={<UserOutlined />}
              placeholder="用户名"
              autoComplete="username"
            />
          </Form.Item>

          <Form.Item
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="密码"
              autoComplete="current-password"
            />
          </Form.Item>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              icon={<LoginOutlined />}
              block
              size="large"
            >
              登 录
            </Button>
          </Form.Item>
        </Form>

        <Tabs
          centered
          items={[
            {
              key: 'quick',
              label: '快速体验',
              children: (
                <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                  <Button size="small" onClick={() => handleQuickLogin('ADMIN')}>
                    管理员
                  </Button>
                  <Button size="small" onClick={() => handleQuickLogin('EDITOR')}>
                    编辑者
                  </Button>
                  <Button size="small" onClick={() => handleQuickLogin('VIEWER')}>
                    查看者
                  </Button>
                </div>
              )
            }
          ]}
        />

        <div className="auth-footer">
          © 2024 低代码BI平台 · 支持多租户 · 拖拽式设计 · 自助分析
        </div>
      </Card>
    </div>
  )
}

export default Login
