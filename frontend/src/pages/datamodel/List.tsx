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
  Row,
  Col,
  Statistic
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  EyeOutlined,
  PlayCircleOutlined,
  TableOutlined,
  ReloadOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ShareAltOutlined
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import type { DataModel } from '@/types'
import { dataModelApi, dataSourceApi } from '@/api'
import { formatDate, getStatusColor } from '@/utils'

const { Option } = Select

const DataModelList: React.FC = () => {
  const [models, setModels] = useState<DataModel[]>([])
  const [dataSources, setDataSources] = useState<{ id: string; name: string }[]>([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [form] = Form.useForm()
  const navigate = useNavigate()

  const loadData = async () => {
    try {
      setLoading(true)
      const [modelData, dsData] = await Promise.all([
        dataModelApi.getList(),
        dataSourceApi.getList()
      ])
      setModels(modelData)
      setDataSources(dsData.map(ds => ({ id: ds.id, name: ds.name })))
    } catch (error) {
      console.error('Failed to load data models:', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [])

  const handleCreate = () => {
    form.resetFields()
    setModalVisible(true)
  }

  const handleEdit = (id: string) => {
    navigate(`/datamodel/${id}/edit`)
  }

  const handleDelete = async (id: string) => {
    try {
      await dataModelApi.delete(id)
      message.success('删除成功')
      loadData()
    } catch (error) {
      console.error('Failed to delete data model:', error)
    }
  }

  const handlePublish = async (id: string, publish: boolean) => {
    try {
      if (publish) {
        await dataModelApi.publish(id)
        message.success('发布成功')
      } else {
        await dataModelApi.unpublish(id)
        message.success('取消发布成功')
      }
      loadData()
    } catch (error) {
      console.error('Failed to publish data model:', error)
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      await dataModelApi.create(values)
      message.success('创建成功')
      setModalVisible(false)
      loadData()
    } catch (error) {
      console.error('Failed to create data model:', error)
    }
  }

  const columns: any = [
    {
      title: '模型名称',
      dataIndex: 'name',
      key: 'name',
      render: (text: string) => (
        <Space>
          <TableOutlined style={{ color: '#1677ff' }} />
          <span>{text}</span>
        </Space>
      )
    },
    {
      title: '数据源',
      dataIndex: 'dataSourceName',
      key: 'dataSourceName',
      width: 150
    },
    {
      title: '表数量',
      dataIndex: 'tables',
      key: 'tables',
      width: 100,
      align: 'center',
      render: (tables: any[]) => tables?.length || 0
    },
    {
      title: '关联关系',
      dataIndex: 'relations',
      key: 'relations',
      width: 100,
      align: 'center',
      render: (relations: any[]) => relations?.length || 0
    },
    {
      title: '计算字段',
      dataIndex: 'calculatedFields',
      key: 'calculatedFields',
      width: 100,
      align: 'center',
      render: (fields: any[]) => fields?.length || 0
    },
    {
      title: '度量',
      dataIndex: 'measures',
      key: 'measures',
      width: 100,
      align: 'center',
      render: (measures: any[]) => measures?.length || 0
    },
    {
      title: '状态',
      dataIndex: 'isPublished',
      key: 'isPublished',
      width: 100,
      render: (published: boolean) => (
        <Tag color={published ? 'success' : 'default'}>
          {published ? '已发布' : '草稿'}
        </Tag>
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
      render: (_: any, record: DataModel) => (
        <Space size="small">
          <Button
            type="text"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record.id)}
          />
          {record.isPublished ? (
            <Button
              type="text"
              size="small"
              icon={<CloseCircleOutlined />}
              onClick={() => handlePublish(record.id, false)}
            />
          ) : (
            <Button
              type="text"
              size="small"
              icon={<CheckCircleOutlined />}
              onClick={() => handlePublish(record.id, true)}
            />
          )}
          <Popconfirm
            title="确定要删除这个数据模型吗？"
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
    total: models.length,
    published: models.filter(m => m.isPublished).length,
    totalTables: models.reduce((sum, m) => sum + (m.tables?.length || 0), 0),
    totalMeasures: models.reduce((sum, m) => sum + (m.measures?.length || 0), 0)
  }

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">数据模型</h1>
        <div className="page-actions">
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={handleCreate}
          >
            新建模型
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
        <Col span={6}>
          <Card>
            <Statistic
              title="总模型数"
              value={stats.total}
              prefix={<TableOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="已发布"
              value={stats.published}
              valueStyle={{ color: '#52c41a' }}
              prefix={<CheckCircleOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="总数据表"
              value={stats.totalTables}
              valueStyle={{ color: '#1677ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="总度量数"
              value={stats.totalMeasures}
              valueStyle={{ color: '#722ed1' }}
            />
          </Card>
        </Col>
      </Row>

      <Card>
        <Table
          dataSource={models}
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
        title="新建数据模型"
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
            label="模型名称"
            rules={[{ required: true, message: '请输入模型名称' }]}
          >
            <Input placeholder="请输入模型名称" />
          </Form.Item>
          <Form.Item
            name="description"
            label="描述"
          >
            <Input.TextArea rows={3} placeholder="请输入描述信息" />
          </Form.Item>
          <Form.Item
            name="dataSourceId"
            label="选择数据源"
            rules={[{ required: true, message: '请选择数据源' }]}
          >
            <Select placeholder="请选择数据源">
              {dataSources.map(ds => (
                <Option key={ds.id} value={ds.id}>{ds.name}</Option>
              ))}
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default DataModelList
