import React, { useState } from 'react'
import { Layout, Menu, Avatar, Dropdown, Button, theme, Badge, Switch } from 'antd'
import {
  DashboardOutlined,
  DatabaseOutlined,
  TableOutlined,
  CodeOutlined,
  ClockCircleOutlined,
  SettingOutlined,
  TeamOutlined,
  SafetyCertificateOutlined,
  UserOutlined,
  LogoutOutlined,
  KeyOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  FullscreenOutlined,
  FullscreenExitOutlined,
  BulbOutlined,
  BulbFilled,
  BellOutlined,
  WarningOutlined
} from '@ant-design/icons'
import { useNavigate, useLocation, Outlet } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'

const { Header, Sider, Content } = Layout

interface MainLayoutProps {
  children?: React.ReactNode
}

const MainLayout: React.FC<MainLayoutProps> = () => {
  const [collapsed, setCollapsed] = useState(false)
  const [isDark, setIsDark] = useState(false)
  const [isFullscreen, setIsFullscreen] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  const { user, logout } = useAuthStore()
  const {
    token: { colorBgContainer, borderRadiusLG }
  } = theme.useToken()

  const menuItems = [
    {
      key: '/dashboard',
      icon: <DashboardOutlined />,
      label: '仪表板'
    },
    {
      key: '/datasource',
      icon: <DatabaseOutlined />,
      label: '数据源',
      roles: ['ADMIN', 'EDITOR']
    },
    {
      key: '/datamodel',
      icon: <TableOutlined />,
      label: '数据模型',
      roles: ['ADMIN', 'EDITOR']
    },
    {
      key: '/sql',
      icon: <CodeOutlined />,
      label: 'SQL编辑器',
      roles: ['ADMIN', 'EDITOR']
    },
    {
      key: '/schedule',
      icon: <ClockCircleOutlined />,
      label: '定时任务',
      roles: ['ADMIN', 'EDITOR']
    },
    {
      key: '/alert',
      icon: <BellOutlined />,
      label: '告警中心',
      roles: ['ADMIN', 'EDITOR', 'VIEWER']
    },
    {
      key: '/alert/rules',
      icon: <WarningOutlined />,
      label: '告警规则',
      roles: ['ADMIN', 'EDITOR']
    },
    {
      key: 'system',
      icon: <SettingOutlined />,
      label: '系统管理',
      roles: ['ADMIN'],
      children: [
        {
          key: '/system/users',
          icon: <TeamOutlined />,
          label: '用户管理'
        },
        {
          key: '/system/roles',
          icon: <SafetyCertificateOutlined />,
          label: '角色权限'
        },
        {
          key: '/system/permissions',
          icon: <KeyOutlined />,
          label: '数据权限'
        }
      ]
    }
  ]

  const filterMenuItems = (items: any[], userRole: string | null): any[] => {
    return items
      .filter(item => {
        if (!item.roles) return true
        return item.roles.includes(userRole)
      })
      .map(item => {
        if (item.children) {
          return {
            ...item,
            children: filterMenuItems(item.children, userRole)
          }
        }
        return item
      })
  }

  const filteredMenuItems = filterMenuItems(menuItems, user?.role || null)

  const handleMenuClick = ({ key }: { key: string }) => {
    navigate(key)
  }

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const toggleFullscreen = () => {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen()
      setIsFullscreen(true)
    } else {
      document.exitFullscreen()
      setIsFullscreen(false)
    }
  }

  const toggleTheme = () => {
    setIsDark(!isDark)
    document.body.classList.toggle('dark-theme', !isDark)
  }

  const userMenuItems = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人信息'
    },
    {
      key: 'password',
      icon: <KeyOutlined />,
      label: '修改密码'
    },
    {
      type: 'divider' as const
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: handleLogout
    }
  ]

  const selectedKeys = [location.pathname]
  const openKeys = location.pathname.startsWith('/system') ? ['system'] : []

  return (
    <Layout style={{ minHeight: '100vh' }} className={isDark ? 'dark-theme' : ''}>
      <Sider
        trigger={null}
        collapsible
        collapsed={collapsed}
        theme={isDark ? 'dark' : 'light'}
        style={{
          background: isDark ? '#1f1f1f' : colorBgContainer,
          borderRight: '1px solid ' + (isDark ? '#333' : '#f0f0f0')
        }}
      >
        <div
          style={{
            height: 64,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            borderBottom: '1px solid ' + (isDark ? '#333' : '#f0f0f0'),
            fontWeight: 'bold',
            fontSize: collapsed ? 12 : 18,
            color: isDark ? '#fff' : '#1677ff'
          }}
        >
          {collapsed ? 'BI' : '低代码BI平台'}
        </div>
        <Menu
          mode="inline"
          theme={isDark ? 'dark' : 'light'}
          selectedKeys={selectedKeys}
          defaultOpenKeys={openKeys}
          items={filteredMenuItems}
          onClick={handleMenuClick}
          style={{ borderRight: 'none' }}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            padding: '0 16px',
            background: isDark ? '#1f1f1f' : colorBgContainer,
            borderBottom: '1px solid ' + (isDark ? '#333' : '#f0f0f0'),
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between'
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            <Button
              type="text"
              icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
              onClick={() => setCollapsed(!collapsed)}
              style={{ fontSize: '16px', width: 64, height: 64 }}
            />
            <span style={{ fontSize: 16, fontWeight: 500, color: isDark ? '#fff' : '#262626' }}>
              {menuItems.find(item => item.key === location.pathname)?.label ||
                menuItems.flatMap(item => item.children || []).find(item => item.key === location.pathname)?.label}
            </span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            <Badge count={3} size="small">
              <Button type="text" icon={<BellOutlined />} />
            </Badge>
            <Button
              type="text"
              icon={isDark ? <BulbFilled /> : <BulbOutlined />}
              onClick={toggleTheme}
            />
            <Button
              type="text"
              icon={isFullscreen ? <FullscreenExitOutlined /> : <FullscreenOutlined />}
              onClick={toggleFullscreen}
            />
            <Switch
              checked={isDark}
              onChange={toggleTheme}
              checkedChildren="🌙"
              unCheckedChildren="☀️"
            />
            <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
                <Avatar size="small" icon={<UserOutlined />} src={user?.avatar} />
                <span style={{ color: isDark ? '#fff' : '#262626' }}>
                  {user?.username || '用户'}
                </span>
              </div>
            </Dropdown>
          </div>
        </Header>
        <Content
          style={{
            margin: '16px',
            padding: 16,
            minHeight: 280,
            background: isDark ? '#141414' : '#f0f2f5',
            borderRadius: borderRadiusLG
          }}
        >
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}

export default MainLayout
