import React, { useState, useEffect } from 'react'
import {
  Card,
  Row,
  Col,
  Button,
  Space,
  Modal,
  Form,
  Input,
  Select,
  message,
  Popconfirm,
  Tag,
  Dropdown,
  MenuProps
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  EyeOutlined,
  CopyOutlined,
  ShareAltOutlined,
  FileTextOutlined,
  DownloadOutlined,
  MoreOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  AppstoreOutlined,
  DashboardOutlined
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import type { Dashboard } from '@/types'
import { dashboardApi } from '@/api'
import { formatDate, getRelativeTime, getStatusText, getStatusColor } from '@/utils'

const { Option } = Select
const { Meta } = Card

const DashboardList: React.FC = () => {
  const [dashboards, setDashboards] = useState<Dashboard[]>([])
  const [templates, setTemplates] = useState<Dashboard[]>([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [templateModalVisible, setTemplateModalVisible] = useState(false)
  const [form] = Form.useForm()
  const [selectedTemplate, setSelectedTemplate] = useState<string | null>(null)
  const navigate = useNavigate()

  const loadData = async () => {
    try {
      setLoading(true)
      const [data, templateData] = await Promise.all([
        dashboardApi.getList(),
        dashboardApi.getTemplates()
      ])
      setDashboards(data)
      setTemplates(templateData)
    } catch (error) {
      console.error('Failed to load dashboards:', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [])

  const handleCreate = () => {
    form.resetFields()
    form.setFieldsValue({
      theme: 'LIGHT',
      autoRefreshInterval: 0,
      refreshIntervalUnit: 'MINUTES'
    })
    setModalVisible(true)
  }

  const handleCreateFromTemplate = () => {
    form.resetFields()
    setSelectedTemplate(null)
    setTemplateModalVisible(true)
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      const dashboard = await dashboardApi.create(values)
      message.success('创建成功')
      setModalVisible(false)
      navigate(`/dashboard/${dashboard.id}/edit`)
    } catch (error) {
      console.error('Failed to create dashboard:', error)
    }
  }

  const handleSubmitFromTemplate = async () => {
    try {
      const values = await form.validateFields()
      if (!selectedTemplate) {
        message.error('请选择模板')
        return
      }
      const dashboard = await dashboardApi.createFromTemplate(selectedTemplate, values.name)
      message.success('从模板创建成功')
      setTemplateModalVisible(false)
      navigate(`/dashboard/${dashboard.id}/edit`)
    } catch (error) {
      console.error('Failed to create from template:', error)
    }
  }

  const handleEdit = (id: string) => {
    navigate(`/dashboard/${id}/edit`)
  }

  const handleView = (id: string) => {
    navigate(`/dashboard/${id}/view`)
  }

  const handleCopy = async (id: string) => {
    try {
      const dashboard = dashboards.find(d => d.id === id)
      const newName = `${dashboard?.name || '仪表板'} - 副本`
      const result = await dashboardApi.copy(id, newName)
      message.success('复制成功')
      navigate(`/dashboard/${result.id}/edit`)
    } catch (error) {
      console.error('Failed to copy dashboard:', error)
    }
  }

  const handleDelete = async (id: string) => {
    try {
      await dashboardApi.delete(id)
      message.success('删除成功')
      loadData()
    } catch (error) {
      console.error('Failed to delete dashboard:', error)
    }
  }

  const handlePublish = async (id: string) => {
    try {
      await dashboardApi.publish(id)
      message.success('发布成功')
      loadData()
    } catch (error) {
      console.error('Failed to publish dashboard:', error)
    }
  }

  const handleSetTemplate = async (id: string) => {
    try {
      await dashboardApi.setAsTemplate(id)
      message.success('已设为模板')
      loadData()
    } catch (error) {
      console.error('Failed to set as template:', error)
    }
  }

  const getMoreMenu = (record: Dashboard): MenuProps => ({
    items: [
      {
        key: 'publish',
        label: record.isPublished ? '取消发布' : '发布',
        icon: <CheckCircleOutlined />,
        onClick: () => handlePublish(record.id)
      },
      {
        key: 'template',
        label: '设为模板',
        icon: <AppstoreOutlined />,
        onClick: () => handleSetTemplate(record.id)
      },
      {
        key: 'export-pdf',
        label: '导出PDF',
        icon: <FileTextOutlined />,
        onClick: () => dashboardApi.exportPdf(record.id)
      },
      {
        key: 'export-image',
        label: '导出图片',
        icon: <DownloadOutlined />,
        onClick: () => dashboardApi.exportImage(record.id)
      },
      {
        key: 'share',
        label: '分享嵌入',
        icon: <ShareAltOutlined />,
        onClick: () => message.info('功能开发中')
      }
    ]
  })

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">仪表板管理</h1>
        <div className="page-actions">
          <Button icon={<AppstoreOutlined />} onClick={handleCreateFromTemplate}>
            从模板创建
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
            新建仪表板
          </Button>
        </div>
      </div>

      <Row gutter={[16, 16]}>
        {dashboards.map((dashboard) => (
          <Col xs={24} sm={12} md={8} lg={6} key={dashboard.id}>
            <Card
              className="dashboard-card"
              hoverable
              cover={
                <div className="dashboard-thumbnail">
                  <DashboardOutlined style={{ fontSize: 48 }} />
                </div>
              }
              actions={[
                <EyeOutlined key="view" onClick={() => handleView(dashboard.id)} />,
                <EditOutlined key="edit" onClick={() => handleEdit(dashboard.id)} />,
                <CopyOutlined key="copy" onClick={() => handleCopy(dashboard.id)} />,
                <Dropdown menu={getMoreMenu(dashboard)} trigger={['click']}>
                  <MoreOutlined key="more" />
                </Dropdown>
              ]}
            >
              <Meta
                title={
                  <Space>
                    <span style={{ fontWeight: 500 }}>{dashboard.name}</span>
                    {dashboard.isPublished && (
                      <Tag color="success" style={{ margin: 0 }}>已发布</Tag>
                    )}
                    {dashboard.isTemplate && (
                      <Tag color="blue" style={{ margin: 0 }}>模板</Tag>
                    )}
                  </Space>
                }
                description={
                  <div style={{ marginTop: 8 }}>
                    <div style={{ fontSize: 12, color: '#8c8c8c', marginBottom: 4 }}>
                      {dashboard.description || '暂无描述'}
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <Space size="small" style={{ fontSize: 12, color: '#8c8c8c' }}>
                        <ClockCircleOutlined />
                        <span>{getRelativeTime(dashboard.updatedAt)}</span>
                      </Space>
                      <Space size="small">
                        <Tag color={getStatusColor(dashboard.theme as string)}>
                          {dashboard.theme === 'DARK' ? '深色' : '浅色'}
                        </Tag>
                        <Tag color="default">{dashboard.tabs?.length || 0} 个标签</Tag>
                      </Space>
                    </div>
                  </div>
                }
              />
            </Card>
          </Col>
        ))}
      </Row>

      {dashboards.length === 0 && !loading && (
        <div className="empty-state">
          <div className="icon"><DashboardOutlined /></div>
          <div className="title">暂无仪表板</div>
          <div className="description">
            点击"新建仪表板"开始创建您的第一个数据分析仪表板
          </div>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={handleCreate}
            style={{ marginTop: 16 }}
          >
            新建仪表板
          </Button>
        </div>
      )}

      <Modal
        title="新建仪表板"
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        footer={[
          <Button key="cancel" onClick={() => setModalVisible(false)}>取消</Button>,
          <Button key="submit" type="primary" onClick={handleSubmit}>创建</Button>
        ]}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="仪表板名称"
            rules={[{ required: true, message: '请输入仪表板名称' }]}
          >
            <Input placeholder="请输入仪表板名称" />
          </Form.Item>
          <Form.Item
            name="description"
            label="描述"
          >
            <Input.TextArea rows={3} placeholder="请输入描述信息" />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="theme"
                label="主题"
                rules={[{ required: true, message: '请选择主题' }]}
              >
                <Select>
                  <Option value="LIGHT">浅色主题</Option>
                  <Option value="DARK">深色主题</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="autoRefreshInterval"
                label="自动刷新"
                rules={[{ required: true, message: '请选择刷新间隔' }]}
              >
                <Select>
                  <Option value={0}>关闭</Option>
                  <Option value={30}>30秒</Option>
                  <Option value={60}>1分钟</Option>
                  <Option value={300}>5分钟</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      <Modal
        title="从模板创建"
        open={templateModalVisible}
        onCancel={() => setTemplateModalVisible(false)}
        footer={[
          <Button key="cancel" onClick={() => setTemplateModalVisible(false)}>取消</Button>,
          <Button key="submit" type="primary" onClick={handleSubmitFromTemplate}>创建</Button>
        ]}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="template"
            label="选择模板"
            rules={[{ required: true, message: '请选择模板' }]}
          >
            <Select
              placeholder="请选择模板"
              onChange={(value) => setSelectedTemplate(value)}
            >
              {templates.map((template) => (
                <Option key={template.id} value={template.id}>
                  {template.name}
                </Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item
            name="name"
            label="仪表板名称"
            rules={[{ required: true, message: '请输入仪表板名称' }]}
          >
            <Input placeholder="请输入仪表板名称" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default DashboardList
