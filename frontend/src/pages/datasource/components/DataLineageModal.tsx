import React, { useState, useEffect } from 'react'
import { Modal, Card, Tag, Space, Button, Empty, Tooltip, Row, Col } from 'antd'
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
    <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 24 }}>
      <Card
        size="small"
        style={{
          background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
          border: 'none',
          color: 'white',
          minWidth: 280,
          boxShadow: '0 4px 12px rgba(102, 126, 234, 0.4)'
        }}
      >
        <Card.Meta
          avatar={<DatabaseOutlined style={{ fontSize: 28, color: 'white' }} />}
          title={<span style={{ color: 'white', fontSize: 16, fontWeight: 'bold' }}>{dataSource.name}</span>}
          description={
            <Tag color="blue" style={{ background: 'rgba(255,255,255,0.3)', color: 'white', border: 'none' }}>
              {dataSource.type}
            </Tag>
          }
        />
      </Card>
    </div>
  )

  const renderDataModelNode = (model: DataModelNode) => (
    <Card
      size="small"
      hoverable
      onClick={() => navigateToDataModel(model.id)}
      style={{ 
        marginBottom: 12, 
        cursor: 'pointer',
        borderLeft: '4px solid #1890ff',
        transition: 'all 0.3s'
      }}
    >
      <Card.Meta
        avatar={<TableOutlined style={{ fontSize: 20, color: '#1890ff' }} />}
        title={
          <Space>
            <span style={{ fontWeight: '500' }}>{model.name}</span>
          </Space>
        }
        description={
          <Space size={4} wrap>
            <Tag color="blue" style={{ margin: 0 }}>{model.tableCount} 表</Tag>
            <Tag color="green" style={{ margin: 0 }}>{model.measureCount} 度量</Tag>
            <Tag color="orange" style={{ margin: 0 }}>{model.dimensionCount} 维度</Tag>
          </Space>
        }
      />
    </Card>
  )

  const renderDashboardNode = (dashboard: DashboardNode) => (
    <Card
      size="small"
      hoverable
      onClick={() => navigateToDashboard(dashboard.id)}
      style={{ 
        marginBottom: 8, 
        cursor: 'pointer',
        marginLeft: 20,
        borderLeft: '4px solid #52c41a',
        background: '#f6ffed'
      }}
    >
      <Card.Meta
        avatar={<DashboardOutlined style={{ fontSize: 18, color: '#52c41a' }} />}
        title={
          <Space>
            <span style={{ fontSize: 13 }}>{dashboard.name}</span>
            {dashboard.isPublished && <Tag color="success" style={{ margin: 0 }}>已发布</Tag>}
          </Space>
        }
        description={
          <Tag color="purple" style={{ margin: 0 }}>{dashboard.componentCount} 组件</Tag>
        }
      />
    </Card>
  )

  const renderConnector = () => (
    <div style={{ display: 'flex', justifyContent: 'center', margin: '8px 0' }}>
      <div style={{ 
        width: 2, 
        height: 20, 
        background: 'linear-gradient(to bottom, #667eea, #1890ff)',
        position: 'relative'
      }}>
        <div style={{
          position: 'absolute',
          bottom: -4,
          left: -5,
          width: 0,
          height: 0,
          borderLeft: '6px solid transparent',
          borderRight: '6px solid transparent',
          borderTop: '8px solid #1890ff'
        }} />
      </div>
    </div>
  )

  const renderHorizontalConnector = () => (
    <div style={{ 
      display: 'flex', 
      alignItems: 'center', 
      justifyContent: 'center',
      padding: '0 8px'
    }}>
      <div style={{ 
        height: 2, 
        flex: 1, 
        background: 'linear-gradient(to right, #1890ff, #52c41a)',
        position: 'relative',
        minWidth: 20
      }}>
        <div style={{
          position: 'absolute',
          right: -4,
          top: -5,
          width: 0,
          height: 0,
          borderTop: '6px solid transparent',
          borderBottom: '6px solid transparent',
          borderLeft: '8px solid #52c41a'
        }} />
      </div>
    </div>
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
      title={
        <Space>
          <span style={{ fontSize: 16, fontWeight: 'bold' }}>数据血缘关系图</span>
          <Tag color="purple">
            {modelsByDataSource.length} 个数据模型 · {lineageData?.dashboards?.length || 0} 个仪表板
          </Tag>
        </Space>
      }
      open={visible}
      onCancel={onCancel}
      width={1000}
      footer={[
        <Button key="close" onClick={onCancel}>
        关闭
        </Button>
      ]}
    >
      <div style={{ 
        padding: '20px 0', 
        background: 'linear-gradient(180deg, #f0f5ff 0%, #f6ffed 100%)',
        borderRadius: 8,
        minHeight: 400
      }}>
        {lineageData && (
          <div style={{ position: 'relative' }}>
            <div style={{ textAlign: 'center', marginBottom: 8 }}>
              <Tag color="purple" style={{ fontSize: 12, padding: '4px 12px' }}>
                数据源层
              </Tag>
            </div>

            {lineageData.dataSource && renderDataSourceNode(lineageData.dataSource)}
            
            {modelsByDataSource.length === 0 ? (
              <>
                {renderConnector()}
                <div style={{ textAlign: 'center', marginTop: 24 }}>
                  <Empty description="暂无关联的数据模型和仪表板" />
                </div>
              </>
            ) : (
              <>
                {renderConnector()}
                
                <div style={{ textAlign: 'center', marginBottom: 16 }}>
                  <Tag color="blue" style={{ fontSize: 12, padding: '4px 12px' }}>
                    数据模型层
                  </Tag>
                </div>

                <Row gutter={[16, 16]} style={{ padding: '0 40px' }}>
                  {modelsByDataSource.map(model => (
                    <Col xs={24} sm={12} md={12} lg={8} key={model.id}>
                      {renderDataModelNode(model)}
                      
                      {(dashboardsByModel.get(model.id)?.length ?? 0) > 0 && (
                        <>
                          {renderHorizontalConnector()}
                          <div style={{ textAlign: 'center', margin: '4px 0 8px 20px' }}>
                            <Tag color="green" style={{ fontSize: 11 }}>仪表板层</Tag>
                          </div>
                          {dashboardsByModel.get(model.id)?.map(dashboard => (
                            <div key={dashboard.id}>
                              {renderDashboardNode(dashboard)}
                            </div>
                          ))}
                        </>
                      )}
                    </Col>
                  ))}
                </Row>
              </>
            )}
          </div>
        )}

        {lineageData && (
          <div style={{ 
            marginTop: 32, 
            paddingTop: 16, 
            borderTop: '1px dashed #d9d9d9',
            textAlign: 'center'
          }}>
            <Space size={24}>
              <Space>
                <div style={{ 
                  width: 16, 
                  height: 16, 
                  background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                  borderRadius: 2
                }} />
                <span style={{ fontSize: 12, color: '#666' }}>数据源</span>
              </Space>
              <Space>
                <div style={{ 
                  width: 16, 
                  height: 16, 
                  background: 'white',
                  border: '2px solid #1890ff',
                  borderRadius: 2
                }} />
                <span style={{ fontSize: 12, color: '#666' }}>数据模型</span>
              </Space>
              <Space>
                <div style={{ 
                  width: 16, 
                  height: 16, 
                  background: '#f6ffed',
                  border: '2px solid #52c41a',
                  borderRadius: 2
                }} />
                <span style={{ fontSize: 12, color: '#666' }}>仪表板</span>
              </Space>
              <Space>
                <ArrowRightOutlined style={{ color: '#1890ff' }} />
                <span style={{ fontSize: 12, color: '#666' }}>引用关系</span>
              </Space>
            </Space>
          </div>
        )}
      </div>
    </Modal>
  )
}

export default DataLineageModal
