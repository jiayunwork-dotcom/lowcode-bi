import React, { useState, useEffect } from 'react'
import { Modal, Card, Tag, Space, Button, Empty, Tooltip } from 'antd'
import { DatabaseOutlined, TableOutlined, DashboardOutlined, ArrowRightOutlined } from '@ant-design/icons'
import type { DataLineageResponse, DataSourceNode, DataModelNode, DashboardNode } from '@/types'
import { dataSourceApi } from '@/api'

interface DataLineageModalProps {
  visible: boolean
  onCancel: () => void
  dataSourceId: string | null
}

const DataLineageModal: React.FC<DataLineageModalProps> = ({
  visible,
  onCancel,
  dataSourceId
}) => {
  const [lineageData, setLineageData] = useState<DataLineageResponse | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (visible && dataSourceId) {
      loadLineageData()
    }
  }, [visible, dataSourceId])

  const loadLineageData = async () => {
    if (!dataSourceId) return

    try {
      setLoading(true)
      const data = await dataSourceApi.getLineage(dataSourceId)
      setLineageData(data)
    } catch (error) {
      console.error('Failed to load lineage data:', error)
    } finally {
      setLoading(false)
    }
  }

  const navigateToDataModel = (modelId: string) => {
    window.location.href = `/datamodels/${modelId}`
  }

  const navigateToDashboard = (dashboardId: string) => {
    window.location.href = `/dashboards/${dashboardId}`
  }

  const renderDataSourceNode = (dataSource: DataSourceNode) => (
    <Card
      size="small"
      style={{
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        border: 'none',
        color: 'white',
        marginBottom: 24
      }}
    >
      <Card.Meta
        avatar={<DatabaseOutlined style={{ fontSize: 24, color: 'white' }} />}
        title={<span style={{ color: 'white' }}>{dataSource.name}</span>}
        description={
          <Tag color="blue" style={{ background: 'rgba(255,255,255,0.2)', color: 'white' }}>
            {dataSource.type}
          </Tag>
        }
      />
    </Card>
  )

  const renderDataModelNode = (model: DataModelNode) => (
    <Card
      size="small"
      hoverable
      onClick={() => navigateToDataModel(model.id)}
      style={{ marginBottom: 12, cursor: 'pointer' }}
    >
      <Card.Meta
        avatar={<TableOutlined style={{ fontSize: 20, color: '#1890ff' }} />}
        title={
          <Space>
            {model.name}
            <Tag color="blue">
              {model.tableCount} 表
            </Tag>
            <Tag color="green">
              {model.measureCount} 度量
            </Tag>
            <Tag color="orange">
              {model.dimensionCount} 维度
            </Tag>
          </Space>
        }
        description={model.description || '暂无描述'}
      />
    </Card>
  )

  const renderDashboardNode = (dashboard: DashboardNode) => (
    <Card
      size="small"
      hoverable
      onClick={() => navigateToDashboard(dashboard.id)}
      style={{ marginBottom: 12, cursor: 'pointer', marginLeft: 24 }}
    >
      <Card.Meta
        avatar={<DashboardOutlined style={{ fontSize: 20, color: '#52c41a' }} />}
        title={
          <Space>
            {dashboard.name}
            {dashboard.isPublished && <Tag color="success">已发布</Tag>}
            <Tag color="purple">
              {dashboard.componentCount} 组件
            </Tag>
          </Space>
        }
        description={dashboard.description || '暂无描述'}
      />
    </Card>
  )

  const modelsByDataSource = lineageData?.dataModels || []
  const dashboardsByModel = new Map<string, DashboardNode[]>()
  
  lineageData?.dashboards?.forEach(dashboard => {
    const modelId = dashboard.dataModelId
    const existing = dashboardsByModel.get(modelId) || []
    dashboardsByModel.set(modelId, [...existing, dashboard])
  })

  return (
    <Modal
      title="数据血缘"
      open={visible}
      onCancel={onCancel}
      width={900}
      footer={[
        <Button key="close" onClick={onCancel}>
        关闭
        </Button>
      ]}
    >
      {lineageData && (
        <div>
          {lineageData.dataSource && renderDataSourceNode(lineageData.dataSource)}
          
          {modelsByDataSource.length === 0 ? (
            <Empty description="暂无关联的数据模型" />
          ) : (
            <div>
              {modelsByDataSource.map(model => (
              <div key={model.id}>
                {renderDataModelNode(model)}
                
                {(dashboardsByModel.get(model.id)?.length ?? 0) > 0 && (
                  <div style={{ marginLeft: 24 }}>
                  <div style={{ display: 'flex', alignItems: 'center', marginBottom: 8 }}>
                    <ArrowRightOutlined style={{ color: '#1890ff', marginRight: 8 }} />
                    <span style={{ color: '#666', fontSize: 12 }}>关联仪表板</span>
                  </div>
                  
                  {dashboardsByModel.get(model.id)?.map(dashboard => (
                    <div key={dashboard.id}>
                      {renderDashboardNode(dashboard)}
                    </div>
                  ))}
                </div>
              )}
              </div>
            ))}
          </div>
        )}
      </div>
    )}
  </Modal>
  )
}

export default DataLineageModal
