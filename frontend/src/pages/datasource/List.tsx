import React, { useState, useEffect } from 'react'
import {
  Table,
  Button,
  Space,
  Modal,
  Form,
  Input,
  Select,
  InputNumber,
  Switch,
  Tag,
  message,
  Popconfirm,
  Upload,
  Progress,
  Card,
  Statistic,
  Row,
  Col,
  Tooltip
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  SyncOutlined,
  PlayCircleOutlined,
  DatabaseOutlined,
  ReloadOutlined,
  UploadOutlined,
  EyeOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  LineChartOutlined,
  SettingOutlined,
  FileTextOutlined
} from '@ant-design/icons'
import type { DataSource, CsvPreviewResponse } from '@/types'
import { dataSourceApi } from '@/api'
import { formatDate, getStatusColor, getStatusText } from '@/utils'
import CsvPreviewModal from './components/CsvPreviewModal'
import CsvRefreshConfigModal from './components/CsvRefreshConfigModal'
import DataLineageModal from './components/DataLineageModal'
import ChunkedUploader from './components/ChunkedUploader'

const { Option } = Select

const DataSourceList: React.FC = () => {
  const [dataSources, setDataSources] = useState<DataSource[]>([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingItem, setEditingItem] = useState<DataSource | null>(null)
  const [form] = Form.useForm()
  const [testingConnection, setTestingConnection] = useState(false)
  const [uploadProgress, setUploadProgress] = useState(0)
  const [uploading, setUploading] = useState(false)
  const [stats, setStats] = useState({
    total: 0,
    active: 0,
    mysql: 0,
    postgresql: 0,
    clickhouse: 0,
    csv: 0
  })

  const [previewModalVisible, setPreviewModalVisible] = useState(false)
  const [previewData, setPreviewData] = useState<CsvPreviewResponse | null>(null)
  const [previewDataSourceId, setPreviewDataSourceId] = useState<string | undefined>()
  const [previewTableId, setPreviewTableId] = useState<string | undefined>()

  const [refreshConfigVisible, setRefreshConfigVisible] = useState(false)
  const [refreshConfigDataSource, setRefreshConfigDataSource] = useState<DataSource | null>(null)

  const [lineageVisible, setLineageVisible] = useState(false)
  const [lineageDataSourceId, setLineageDataSourceId] = useState<string | null>(null)

  const [uploadDataSourceId, setUploadDataSourceId] = useState<string | null>(null)
  const [uploadModalVisible, setUploadModalVisible] = useState(false)

  const loadData = async () => {
    try {
      setLoading(true)
      const data = await dataSourceApi.getList()
      setDataSources(data)
      setStats({
        total: data.length,
        active: data.filter(d => d.isActive).length,
        mysql: data.filter(d => d.type === 'MYSQL').length,
        postgresql: data.filter(d => d.type === 'POSTGRESQL').length,
        clickhouse: data.filter(d => d.type === 'CLICKHOUSE').length,
        csv: data.filter(d => d.type === 'CSV').length
      })
    } catch (error) {
      console.error('Failed to load data sources:', error)
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
      type: 'MYSQL',
      host: 'localhost',
      port: 3306,
      connectionPoolSize: 10,
      connectionTimeout: 30,
      queryTimeout: 60,
      isActive: true
    })
    setModalVisible(true)
  }

  const handleEdit = (record: DataSource) => {
    setEditingItem(record)
    form.setFieldsValue({
      ...record,
      password: ''
    })
    setModalVisible(true)
  }

  const handleDelete = async (id: string) => {
    try {
      await dataSourceApi.delete(id)
      message.success('删除成功')
      loadData()
    } catch (error) {
      console.error('Failed to delete data source:', error)
    }
  }

  const handleTestConnection = async () => {
    try {
      const values = await form.validateFields()
      setTestingConnection(true)
      const result = await dataSourceApi.testConnection(values)
      if (result.success) {
        message.success('连接测试成功')
      } else {
        message.error('连接测试失败: ' + result.message)
      }
    } catch (error) {
      console.error('Connection test failed:', error)
    } finally {
      setTestingConnection(false)
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      
      if (editingItem) {
        await dataSourceApi.update(editingItem.id, values)
        message.success('更新成功')
      } else {
        await dataSourceApi.create(values)
        message.success('创建成功')
      }
      
      setModalVisible(false)
      loadData()
    } catch (error) {
      console.error('Failed to submit data source:', error)
    }
  }

  const handleSyncMetadata = async (id: string) => {
    try {
      await dataSourceApi.syncMetadata(id)
      message.success('元数据同步成功')
    } catch (error) {
      console.error('Failed to sync metadata:', error)
    }
  }

  const handleUploadCsv = async (file: File) => {
    try {
      const preview = await dataSourceApi.previewCsv(file, 20)
      setPreviewData(preview)
      setPreviewModalVisible(true)
      return false
    } catch (error) {
      console.error('Failed to preview CSV:', error)
    }
    return false
  }

  const handleUploadToDataSource = (record: DataSource) => {
    if (record.type !== 'CSV') {
      message.warning('只能上传CSV文件到CSV类型数据源')
      return
    }
    setUploadDataSourceId(record.id)
    setUploadModalVisible(true)
  }

  const handlePreviewCsv = async (file: File) => {
    try {
      const preview = await dataSourceApi.previewCsv(file, 20)
      setPreviewData(preview)
      setPreviewDataSourceId(uploadDataSourceId || undefined)
      setPreviewModalVisible(true)
    } catch (error) {
      console.error('Failed to preview CSV:', error)
    }
  }

  const handleChunkUploadSuccess = (result: any) => {
    if (result.tableId) {
      setPreviewTableId(result.tableId)
      setPreviewDataSourceId(uploadDataSourceId || undefined)
    }
    setUploadModalVisible(false)
    loadData()
  }

  const handleConfigureRefresh = (record: DataSource) => {
    if (record.type !== 'CSV') {
      message.warning('只有CSV类型数据源支持自动刷新')
      return
    }
    setRefreshConfigDataSource(record)
    setRefreshConfigVisible(true)
  }

  const handleViewLineage = (record: DataSource) => {
    setLineageDataSourceId(record.id)
    setLineageVisible(true)
  }

  const getRefreshStatusIcon = (record: DataSource) => {
    if (record.type !== 'CSV' || !record.csvRefreshInterval || record.csvRefreshInterval === 'OFF') {
      return null
    }

    const status = record.csvLastRefreshStatus
    if (record.csvRefreshInProgress || status === 'REFRESHING') {
      return (
        <Tooltip title="刷新中">
          <SyncOutlined spin style={{ color: '#faad14', marginLeft: 8 }} />
        </Tooltip>
      )
    }
    if (status === 'FAILED') {
      return (
        <Tooltip title={`上次刷新失败: ${record.csvLastRefreshError || '未知错误'}`}>
          <CloseCircleOutlined style={{ color: '#ff4d4f', marginLeft: 8 }} />
        </Tooltip>
      )
    }
    return (
      <Tooltip title={`刷新正常 - 上次: ${record.csvLastImportTime || '未知'}`}>
        <CheckCircleOutlined style={{ color: '#52c41a', marginLeft: 8 }} />
      </Tooltip>
    )
  }

  const columns: any = [
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      render: (text: string, record: DataSource) => (
        <Space>
          <DatabaseOutlined style={{ color: getDbTypeColor(record.type) }} />
          <span>{text}</span>
          {getRefreshStatusIcon(record)}
        </Space>
      )
    },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      width: 120,
      render: (type: string) => (
        <Tag color={getDbTypeTagColor(type)}>{getDbTypeName(type)}</Tag>
      )
    },
    {
      title: '连接信息',
      key: 'connection',
      width: 200,
      render: (_: any, record: DataSource) => (
        record.type === 'CSV' ? (
          <span>CSV文件</span>
        ) : (
          <span>{record.host}:{record.port}/{record.database}</span>
        )
      )
    },
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
      width: 120
    },
    {
      title: '连接池大小',
      dataIndex: 'connectionPoolSize',
      key: 'connectionPoolSize',
      width: 100,
      align: 'center'
    },
    {
      title: '状态',
      dataIndex: 'isActive',
      key: 'isActive',
      width: 80,
      render: (active: boolean) => (
        <Tag color={active ? 'success' : 'default'}>
          {active ? '启用' : '禁用'}
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
      width: 320,
      render: (_: any, record: DataSource) => (
        <Space size="small">
          <Tooltip title="测试连接">
            <Button
              type="text"
              size="small"
              icon={<PlayCircleOutlined />}
              onClick={() => testConnection(record)}
            />
          </Tooltip>
          <Tooltip title="同步元数据">
            <Button
              type="text"
              size="small"
              icon={<SyncOutlined />}
              onClick={() => handleSyncMetadata(record.id)}
            />
          </Tooltip>
          <Tooltip title="查看表">
            <Button
              type="text"
              size="small"
              icon={<EyeOutlined />}
              onClick={() => viewTables(record)}
            />
          </Tooltip>
          {record.type === 'CSV' && (
            <Tooltip title="上传CSV">
              <Button
                type="text"
                size="small"
                icon={<FileTextOutlined />}
                onClick={() => handleUploadToDataSource(record)}
              />
            </Tooltip>
          )}
          {record.type === 'CSV' && (
            <Tooltip title="刷新配置">
              <Button
                type="text"
                size="small"
                icon={<SettingOutlined />}
                onClick={() => handleConfigureRefresh(record)}
              />
            </Tooltip>
          )}
          <Tooltip title="数据血缘">
            <Button
              type="text"
              size="small"
              icon={<LineChartOutlined />}
              onClick={() => handleViewLineage(record)}
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
            title="确定要删除这个数据源吗？"
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

  const testConnection = async (record: DataSource) => {
    try {
      setTestingConnection(true)
      const result = await dataSourceApi.testConnection(record)
      if (result.success) {
        message.success('连接测试成功')
      } else {
        message.error('连接测试失败: ' + result.message)
      }
    } catch (error) {
      console.error('Connection test failed:', error)
    } finally {
      setTestingConnection(false)
    }
  }

  const viewTables = async (record: DataSource) => {
    try {
      const tables = await dataSourceApi.getTables(record.id)
      Modal.info({
        title: `${record.name} - 数据表列表`,
        width: 800,
        content: (
          <div>
            <p>共 {tables.length} 张表</p>
            <Table
              size="small"
              dataSource={tables}
              rowKey="id"
              columns={[
                { title: '表名', dataIndex: 'name', key: 'name' },
                { title: '显示名', dataIndex: 'displayName', key: 'displayName' },
                { title: '行数', dataIndex: 'rowCount', key: 'rowCount', width: 100, align: 'right' },
                { title: '列数', dataIndex: 'columns', key: 'columns', width: 80, align: 'right', render: (cols: any[]) => cols?.length || 0 },
                { title: '状态', dataIndex: 'isEnabled', key: 'isEnabled', width: 80, render: (enabled: boolean) => enabled ? <CheckCircleOutlined style={{ color: '#52c41a' }} /> : <CloseCircleOutlined style={{ color: '#ff4d4f' }} /> }
              ]}
              pagination={{ pageSize: 10 }}
            />
          </div>
        )
      })
    } catch (error) {
      console.error('Failed to load tables:', error)
    }
  }

  const getDbTypeColor = (type: string) => {
    const colors: Record<string, string> = {
      MYSQL: '#4479A1',
      POSTGRESQL: '#336791',
      CLICKHOUSE: '#FFCC01',
      CSV: '#28A745'
    }
    return colors[type] || '#666'
  }

  const getDbTypeTagColor = (type: string) => {
    const colors: Record<string, string> = {
      MYSQL: 'blue',
      POSTGRESQL: 'geekblue',
      CLICKHOUSE: 'gold',
      CSV: 'green'
    }
    return colors[type] || 'default'
  }

  const getDbTypeName = (type: string) => {
    const names: Record<string, string> = {
      MYSQL: 'MySQL',
      POSTGRESQL: 'PostgreSQL',
      CLICKHOUSE: 'ClickHouse',
      CSV: 'CSV文件'
    }
    return names[type] || type
  }

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">数据源管理</h1>
        <div className="page-actions">
          <Upload
            accept=".csv"
            showUploadList={false}
            beforeUpload={handleUploadCsv}
            disabled={uploading}
          >
            <Button icon={<UploadOutlined />} loading={uploading}>
              上传CSV
            </Button>
          </Upload>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={handleCreate}
          >
            新建数据源
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

      {uploading && (
        <Card style={{ marginBottom: 16 }}>
          <Progress percent={uploadProgress} status="active" />
        </Card>
      )}

      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={4}>
          <Card>
            <Statistic
              title="总数据源"
              value={stats.total}
              prefix={<DatabaseOutlined />}
            />
          </Card>
        </Col>
        <Col span={4}>
          <Card>
            <Statistic
              title="已启用"
              value={stats.active}
              valueStyle={{ color: '#52c41a' }}
              prefix={<CheckCircleOutlined />}
            />
          </Card>
        </Col>
        <Col span={4}>
          <Card>
            <Statistic
              title="MySQL"
              value={stats.mysql}
              valueStyle={{ color: '#4479A1' }}
            />
          </Card>
        </Col>
        <Col span={4}>
          <Card>
            <Statistic
              title="PostgreSQL"
              value={stats.postgresql}
              valueStyle={{ color: '#336791' }}
            />
          </Card>
        </Col>
        <Col span={4}>
          <Card>
            <Statistic
              title="ClickHouse"
              value={stats.clickhouse}
              valueStyle={{ color: '#FFCC01' }}
            />
          </Card>
        </Col>
        <Col span={4}>
          <Card>
            <Statistic
              title="CSV文件"
              value={stats.csv}
              valueStyle={{ color: '#28A745' }}
            />
          </Card>
        </Col>
      </Row>

      <Card>
        <Table
          dataSource={dataSources}
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
        title={editingItem ? '编辑数据源' : '新建数据源'}
        open={modalVisible}
        width={600}
        onCancel={() => setModalVisible(false)}
        footer={[
          <Button key="test" onClick={handleTestConnection} loading={testingConnection}>
            <PlayCircleOutlined /> 测试连接
          </Button>,
          <Button key="cancel" onClick={() => setModalVisible(false)}>
            取消
          </Button>,
          <Button key="submit" type="primary" onClick={handleSubmit}>
            {editingItem ? '保存' : '创建'}
          </Button>
        ]}
      >
        <Form
          form={form}
          layout="vertical"
        >
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="name"
                label="数据源名称"
                rules={[{ required: true, message: '请输入数据源名称' }]}
              >
                <Input placeholder="请输入数据源名称" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="type"
                label="数据库类型"
                rules={[{ required: true, message: '请选择数据库类型' }]}
              >
                <Select>
                  <Option value="MYSQL">MySQL</Option>
                  <Option value="POSTGRESQL">PostgreSQL</Option>
                  <Option value="CLICKHOUSE">ClickHouse</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={16}>
              <Form.Item
                name="host"
                label="主机地址"
                rules={[{ required: true, message: '请输入主机地址' }]}
              >
                <Input placeholder="localhost" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                name="port"
                label="端口"
                rules={[{ required: true, message: '请输入端口' }]}
              >
                <InputNumber min={1} max={65535} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="database"
                label="数据库名"
                rules={[{ required: true, message: '请输入数据库名' }]}
              >
                <Input placeholder="请输入数据库名" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="username"
                label="用户名"
                rules={[{ required: true, message: '请输入用户名' }]}
              >
                <Input placeholder="请输入用户名" />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="password"
            label="密码"
            rules={editingItem ? [] : [{ required: true, message: '请输入密码' }]}
          >
            <Input.Password placeholder={editingItem ? '不修改请留空' : '请输入密码'} />
          </Form.Item>

          <Row gutter={16}>
            <Col span={8}>
              <Form.Item
                name="connectionPoolSize"
                label="连接池大小"
                rules={[{ required: true, message: '请输入连接池大小' }]}
              >
                <InputNumber min={1} max={50} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                name="connectionTimeout"
                label="连接超时(秒)"
                rules={[{ required: true, message: '请输入连接超时时间' }]}
              >
                <InputNumber min={5} max={300} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                name="queryTimeout"
                label="查询超时(秒)"
                rules={[{ required: true, message: '请输入查询超时时间' }]}
              >
                <InputNumber min={10} max={600} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="isActive"
            label="是否启用"
            valuePropName="checked"
          >
            <Switch />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="上传CSV文件"
        open={uploadModalVisible}
        onCancel={() => setUploadModalVisible(false)}
        width={600}
        footer={null}
      >
        <ChunkedUploader
          dataSourceId={uploadDataSourceId || ''}
          onPreview={handlePreviewCsv}
          onSuccess={handleChunkUploadSuccess}
          onError={() => {}}
        />
      </Modal>

      <CsvPreviewModal
        visible={previewModalVisible}
        onCancel={() => setPreviewModalVisible(false)}
        previewData={previewData}
        dataSourceId={previewDataSourceId}
        tableId={previewTableId}
        onSuccess={() => {
          setPreviewModalVisible(false)
          loadData()
        }}
      />

      <CsvRefreshConfigModal
        visible={refreshConfigVisible}
        onCancel={() => setRefreshConfigVisible(false)}
        dataSource={refreshConfigDataSource}
        onSuccess={() => {
          setRefreshConfigVisible(false)
          loadData()
        }}
      />

      <DataLineageModal
        visible={lineageVisible}
        onCancel={() => setLineageVisible(false)}
        dataSourceId={lineageDataSourceId}
      />
    </div>
  )
}

export default DataSourceList
