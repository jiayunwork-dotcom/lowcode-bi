import React, { useState, useEffect, useCallback, useRef } from 'react'
import {
  Layout,
  Button,
  Space,
  Tabs,
  Tag,
  message,
  Drawer,
  Tooltip,
  Dropdown,
  MenuProps,
  Modal
} from 'antd'
import {
  ArrowLeftOutlined,
  FullscreenOutlined,
  BgColorsOutlined,
  ReloadOutlined,
  FilterOutlined,
  DownloadOutlined,
  ShareAltOutlined,
  SettingOutlined,
  EditOutlined,
  DownOutlined,
  UpOutlined,
  VerticalAlignBottomOutlined
} from '@ant-design/icons'
import { useParams, useNavigate } from 'react-router-dom'
import type {
  Dashboard,
  DashboardComponent,
  QueryResult,
  ComponentFilter,
  DrillDownContext
} from '@/types'
import { dashboardApi, scheduleApi } from '@/api'
import { ChartComponents } from '@/components/charts'
import FilterPanel from './components/FilterPanel'
import './Viewer.css'

const { Header, Content } = Layout
const { TabPane } = Tabs

interface ComponentDataState {
  data?: QueryResult
  loading: boolean
  error?: string
}

interface DashboardViewerProps {
  dashboardId?: string
  embedMode?: boolean
  embedConfig?: any
}

const DashboardViewer: React.FC<DashboardViewerProps> = ({ dashboardId, embedMode = false, embedConfig }) => {
  const { id: paramId } = useParams<{ id: string }>()
  const id = dashboardId || paramId
  const navigate = useNavigate()
  
  const [dashboard, setDashboard] = useState<Dashboard | null>(null)
  const [loading, setLoading] = useState(false)
  const [activeTabIndex, setActiveTabIndex] = useState(0)
  const [isFullscreen, setIsFullscreen] = useState(false)
  const [isDarkTheme, setIsDarkTheme] = useState(false)
  const [showFilters, setShowFilters] = useState(false)
  const [globalFilters, setGlobalFilters] = useState<ComponentFilter[]>([])
  const [componentData, setComponentData] = useState<Record<string, ComponentDataState>>({})
  const [drillDownContext, setDrillDownContext] = useState<Record<string, DrillDownContext>>({})
  const [showScheduleModal, setShowScheduleModal] = useState(false)
  const autoRefreshTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  const loadDashboard = useCallback(async () => {
    if (!id) return
    try {
      setLoading(true)
      const data = await dashboardApi.getById(id)
      setDashboard(data)
      setIsDarkTheme(data.theme === 'DARK')
      if (data.tabs && data.tabs.length > 0) {
        setActiveTabIndex(0)
      }
    } catch (error) {
      console.error('Failed to load dashboard:', error)
      message.error('加载仪表板失败')
    } finally {
      setLoading(false)
    }
  }, [id])

  useEffect(() => {
    loadDashboard()
  }, [loadDashboard])

  const loadComponentData = useCallback(async (componentId: string, component: DashboardComponent) => {
    if (!id || !component.dataModelId) return

    try {
      setComponentData(prev => ({
        ...prev,
        [componentId]: { ...prev[componentId], loading: true }
      }))

      const drillFilter = drillDownContext[componentId]
      const filters = [
        ...globalFilters,
        ...(component.filters || []),
        ...(drillFilter ? [{
          id: `drill_${componentId}`,
          field: drillFilter.dimension,
          filterType: 'EQUAL' as const,
          value1: drillFilter.value,
          isGlobal: false
        }] : [])
      ]

      const data = await dashboardApi.getComponentData(id, componentId, { filters })
      
      setComponentData(prev => ({
        ...prev,
        [componentId]: { data, loading: false }
      }))
    } catch (error: any) {
      console.error('Failed to load component data:', error)
      setComponentData(prev => ({
        ...prev,
        [componentId]: { loading: false, error: error.message || '加载失败' }
      }))
    }
  }, [id, globalFilters, drillDownContext])

  const refreshAllComponents = useCallback(() => {
    const currentTab = dashboard?.tabs?.[activeTabIndex]
    if (currentTab?.components) {
      currentTab.components.forEach(comp => {
        loadComponentData(comp.id, comp)
      })
    }
  }, [dashboard, activeTabIndex, loadComponentData])

  useEffect(() => {
    if (dashboard?.tabs?.[activeTabIndex]?.components) {
      refreshAllComponents()
    }
  }, [activeTabIndex, globalFilters, drillDownContext])

  useEffect(() => {
    if (dashboard?.autoRefreshInterval && dashboard.autoRefreshInterval > 0) {
      autoRefreshTimer.current = setInterval(() => {
        refreshAllComponents()
      }, dashboard.autoRefreshInterval * 1000)
    }

    return () => {
      if (autoRefreshTimer.current) {
        clearInterval(autoRefreshTimer.current)
      }
    }
  }, [dashboard?.autoRefreshInterval, refreshAllComponents])

  const handleDrillDown = (componentId: string, dimension: string, value: any, hierarchy?: string) => {
    const currentContext = drillDownContext[componentId]
    if (currentContext && currentContext.hierarchy === hierarchy) {
      setDrillDownContext(prev => ({
        ...prev,
        [componentId]: {
          dimension,
          value,
          hierarchy: hierarchy || dimension,
          currentLevel: currentContext.currentLevel + 1
        }
      }))
    } else {
      setDrillDownContext(prev => ({
        ...prev,
        [componentId]: {
          dimension,
          value,
          hierarchy: hierarchy || dimension,
          currentLevel: 0
        }
      }))
    }
  }

  const handleDrillUp = (componentId: string) => {
    const currentContext = drillDownContext[componentId]
    if (currentContext && currentContext.currentLevel > 0) {
      setDrillDownContext(prev => ({
        ...prev,
        [componentId]: {
          ...currentContext,
          currentLevel: currentContext.currentLevel - 1
        }
      }))
    } else {
      setDrillDownContext(prev => {
        const newContext = { ...prev }
        delete newContext[componentId]
        return newContext
      })
    }
  }

  const handleComponentClick = (component: DashboardComponent, params: any) => {
    if (component.linkedComponents && component.linkedComponents.length > 0) {
      const dimensionValue = params.name || params.value
      const dimension = component.config.dimensions?.[0]
      
      if (dimension && dimensionValue !== undefined) {
        component.linkedComponents.forEach(linkedId => {
          handleDrillDown(linkedId, dimension, dimensionValue)
        })
        message.info(`已联动筛选: ${dimensionValue}`)
      }
    }
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
    setIsDarkTheme(!isDarkTheme)
  }

  const handleExport = async (type: 'pdf' | 'image') => {
    if (!id) return
    try {
      if (type === 'pdf') {
        await dashboardApi.exportPdf(id)
      } else {
        await dashboardApi.exportImage(id)
      }
      message.success('导出成功')
    } catch (error) {
      console.error('Export failed:', error)
      message.error('导出失败')
    }
  }

  const handleSchedule = async () => {
    if (!id) return
    try {
      await scheduleApi.create({
        name: `${dashboard?.name} - 定时推送`,
        dashboardId: id,
        cronExpression: '0 0 9 * * ?',
        timezone: 'Asia/Shanghai',
        emailSubject: dashboard?.name || '仪表板报表',
        emailBody: '请查看附件中的仪表板报表',
        recipients: [],
        isEnabled: true
      })
      message.success('定时任务创建成功')
      setShowScheduleModal(false)
    } catch (error) {
      console.error('Failed to create schedule:', error)
      message.error('创建定时任务失败')
    }
  }

  const exportMenuItems: MenuProps['items'] = [
    {
      key: 'pdf',
      label: '导出PDF',
      icon: <DownloadOutlined />,
      onClick: () => handleExport('pdf')
    },
    {
      key: 'image',
      label: '导出图片',
      icon: <DownloadOutlined />,
      onClick: () => handleExport('image')
    }
  ]

  if (loading && !dashboard) {
    return <div className="viewer-loading">加载中...</div>
  }

  const currentTab = dashboard?.tabs?.[activeTabIndex]

  return (
    <Layout className={`dashboard-viewer ${isDarkTheme ? 'dark-theme' : ''}`}>
      {!isFullscreen && (
        <Header className="viewer-header">
          <div className="viewer-header-left">
            <Button
              type="text"
              icon={<ArrowLeftOutlined />}
              onClick={() => navigate('/dashboard')}
            >
              返回
            </Button>
            <h2 className="viewer-title">{dashboard?.name}</h2>
            {dashboard?.isPublished && <Tag color="success">已发布</Tag>}
            {drillDownContext && Object.keys(drillDownContext).length > 0 && (
              <Tag color="orange">
                已下钻 ({Object.keys(drillDownContext).length})
              </Tag>
            )}
          </div>
          <div className="viewer-header-right">
            <Space>
              <Tooltip title="筛选器">
                <Button
                  icon={<FilterOutlined />}
                  onClick={() => setShowFilters(!showFilters)}
                  type={showFilters ? 'primary' : 'default'}
                />
              </Tooltip>
              <Tooltip title={isDarkTheme ? '浅色模式' : '深色模式'}>
                <Button
                  icon={<BgColorsOutlined />}
                  onClick={toggleTheme}
                />
              </Tooltip>
              <Tooltip title="刷新">
                <Button
                  icon={<ReloadOutlined />}
                  onClick={refreshAllComponents}
                />
              </Tooltip>
              <Dropdown menu={{ items: exportMenuItems }} trigger={['click']}>
                <Tooltip title="导出">
                  <Button icon={<DownloadOutlined />} />
                </Tooltip>
              </Dropdown>
              <Tooltip title="定时推送">
                <Button
                  icon={<SettingOutlined />}
                  onClick={() => setShowScheduleModal(true)}
                />
              </Tooltip>
              <Tooltip title="全屏">
                <Button
                  icon={<FullscreenOutlined />}
                  onClick={toggleFullscreen}
                />
              </Tooltip>
              <Button
                type="primary"
                icon={<EditOutlined />}
                onClick={() => navigate(`/dashboard/${id}/edit`)}
              >
                编辑
              </Button>
            </Space>
          </div>
        </Header>
      )}

      <Layout>
        {showFilters && (
          <div className="viewer-filters">
            <FilterPanel
              filters={globalFilters}
              onChange={setGlobalFilters}
              onClose={() => setShowFilters(false)}
              isGlobal
            />
          </div>
        )}

        <Content className="viewer-content">
          <Tabs
            activeKey={activeTabIndex.toString()}
            onChange={(key) => setActiveTabIndex(parseInt(key))}
            className="viewer-tabs"
            items={dashboard?.tabs?.map((tab, idx) => ({
              key: idx.toString(),
              label: tab.name,
              children: (
                <div className="dashboard-canvas">
                  {tab.components?.map((component) => {
                    const ChartComp = ChartComponents[component.type]
                    const compData = componentData[component.id]
                    const hasDrillDown = !!drillDownContext[component.id]

                    return (
                      <div
                        key={component.id}
                        className="dashboard-component"
                        style={{
                          left: component.x,
                          top: component.y,
                          width: component.width,
                          height: component.height
                        }}
                      >
                        {hasDrillDown && (
                          <div className="drill-down-indicator">
                            <Tooltip title="返回上一级">
                              <Button
                                type="primary"
                                size="small"
                                icon={<UpOutlined />}
                                onClick={() => handleDrillUp(component.id)}
                              >
                                上钻
                              </Button>
                            </Tooltip>
                          </div>
                        )}
                        <ChartComp
                          title={component.title}
                          data={compData?.data}
                          config={component.config}
                          loading={compData?.loading}
                          error={compData?.error}
                          height={component.height - 40}
                          onRefresh={() => loadComponentData(component.id, component)}
                          onClick={(params: any) => {
                            handleComponentClick(component, params)
                            if (component.config.dimensions?.[0]) {
                              handleDrillDown(
                                component.id,
                                component.config.dimensions[0],
                                params.name || params.value
                              )
                            }
                          }}
                        />
                      </div>
                    )
                  })}

                  {(!tab.components || tab.components.length === 0) && (
                    <div className="empty-dashboard">
                      <div className="empty-icon">📊</div>
                      <div className="empty-text">该仪表板暂无组件</div>
                    </div>
                  )}
                </div>
              )
            }))}
          />
        </Content>
      </Layout>

      <Modal
        title="设置定时推送"
        open={showScheduleModal}
        onCancel={() => setShowScheduleModal(false)}
        footer={[
          <Button key="cancel" onClick={() => setShowScheduleModal(false)}>取消</Button>,
          <Button key="submit" type="primary" onClick={handleSchedule}>创建</Button>
        ]}
      >
        <p>将创建一个定时任务，每天早上9点自动推送仪表板截图。</p>
        <p style={{ color: '#8c8c8c', fontSize: 12 }}>
          您可以在"系统设置-定时任务"中修改推送时间和收件人。
        </p>
      </Modal>
    </Layout>
  )
}

export default DashboardViewer
