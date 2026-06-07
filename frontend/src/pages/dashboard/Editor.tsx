import React, { useState, useEffect, useCallback, useRef } from 'react'
import {
  Layout,
  Button,
  Space,
  Tabs,
  Form,
  Input,
  Select,
  InputNumber,
  Switch,
  Slider,
  ColorPicker,
  Card,
  List,
  Modal,
  message,
  Divider,
  Tag,
  Tooltip,
  Popconfirm,
  Drawer,
  Collapse,
  Empty
} from 'antd'
import {
  SaveOutlined,
  PlayCircleOutlined,
  EyeOutlined,
  DeleteOutlined,
  PlusOutlined,
  SettingOutlined,
  FullscreenOutlined,
  BgColorsOutlined,
  ReloadOutlined,
  ArrowLeftOutlined,
  FilterOutlined,
  LinkOutlined,
  DownOutlined,
  UpOutlined
} from '@ant-design/icons'
import { useParams, useNavigate } from 'react-router-dom'
import {
  DndContext,
  closestCenter,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  DragEndEvent,
  DragStartEvent,
  DragOverlay
} from '@dnd-kit/core'
import {
  arrayMove,
  SortableContext,
  sortableKeyboardCoordinates,
  rectSortingStrategy
} from '@dnd-kit/sortable'
import type {
  Dashboard,
  DashboardTab,
  DashboardComponent,
  ComponentConfig,
  DataModel,
  QueryResult,
  ComponentFilter
} from '@/types'
import { dashboardApi, dataModelApi } from '@/api'
import { ChartComponents, ChartTypeLabels, ChartTypeIcons, ChartType } from '@/components/charts'
import SortableComponent from './components/SortableComponent'
import ComponentPropertyPanel from './components/ComponentPropertyPanel'
import FilterPanel from './components/FilterPanel'
import './Editor.css'

const { Header, Sider, Content } = Layout
const { Option } = Select
const { TabPane } = Tabs
const { Panel } = Collapse

const GRID_SIZE = 20
const DEFAULT_COMPONENT_WIDTH = 6
const DEFAULT_COMPONENT_HEIGHT = 8

const availableChartTypes: ChartType[] = ['LINE', 'BAR', 'PIE', 'SCATTER', 'TABLE', 'KPI', 'FUNNEL', 'MAP']

const DashboardEditor: React.FC = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  
  const [dashboard, setDashboard] = useState<Dashboard | null>(null)
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [activeTabIndex, setActiveTabIndex] = useState(0)
  const [selectedComponentId, setSelectedComponentId] = useState<string | null>(null)
  const [activePanel, setActivePanel] = useState<'property' | 'filter'>('property')
  const [dataModels, setDataModels] = useState<DataModel[]>([])
  const [componentData, setComponentData] = useState<Record<string, QueryResult>>({})
  const [componentLoading, setComponentLoading] = useState<Record<string, boolean>>({})
  const [draggedType, setDraggedType] = useState<ChartType | null>(null)
  const [showGlobalFilters, setShowGlobalFilters] = useState(false)
  const [globalFilters, setGlobalFilters] = useState<ComponentFilter[]>([])
  const [unsavedChanges, setUnsavedChanges] = useState(false)

  const sensors = useSensors(
    useSensor(PointerSensor),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates
    })
  )

  const loadDashboard = useCallback(async () => {
    if (!id) return
    try {
      setLoading(true)
      const data = await dashboardApi.getById(id)
      setDashboard(data)
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

  const loadDataModels = useCallback(async () => {
    try {
      const data = await dataModelApi.getList()
      setDataModels(data.filter(d => d.isPublished))
    } catch (error) {
      console.error('Failed to load data models:', error)
    }
  }, [])

  useEffect(() => {
    loadDashboard()
    loadDataModels()
  }, [loadDashboard, loadDataModels])

  const loadComponentData = useCallback(async (componentId: string, component: DashboardComponent) => {
    if (!id || !component.dataModelId) return

    try {
      setComponentLoading(prev => ({ ...prev, [componentId]: true }))
      const data = await dashboardApi.getComponentData(id, componentId, {
        filters: [...globalFilters, ...(component.filters || [])]
      })
      setComponentData(prev => ({ ...prev, [componentId]: data }))
    } catch (error) {
      console.error('Failed to load component data:', error)
    } finally {
      setComponentLoading(prev => ({ ...prev, [componentId]: false }))
    }
  }, [id, globalFilters])

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
  }, [activeTabIndex, globalFilters])

  const handleDragStart = (event: DragStartEvent) => {
    const { active } = event
    if (availableChartTypes.includes(active.id as ChartType)) {
      setDraggedType(active.id as ChartType)
    }
  }

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, delta } = event
    setDraggedType(null)

    if (!dashboard || !dashboard.tabs) return

    if (availableChartTypes.includes(active.id as ChartType)) {
      const newComponent: DashboardComponent = {
        id: `comp_${Date.now()}`,
        type: active.id as ChartType,
        title: ChartTypeLabels[active.id as ChartType],
        dataModelId: dataModels[0]?.id || '',
        x: Math.round(delta.x / GRID_SIZE) * GRID_SIZE,
        y: Math.round(delta.y / GRID_SIZE) * GRID_SIZE,
        width: DEFAULT_COMPONENT_WIDTH * GRID_SIZE,
        height: DEFAULT_COMPONENT_HEIGHT * GRID_SIZE,
        config: {
          dimensions: [],
          measures: [],
          showLegend: true,
          showTooltip: true
        },
        filters: [],
        linkedComponents: []
      }

      const updatedTabs = [...dashboard.tabs]
      if (!updatedTabs[activeTabIndex].components) {
        updatedTabs[activeTabIndex].components = []
      }
      updatedTabs[activeTabIndex].components = [...updatedTabs[activeTabIndex].components, newComponent]

      setDashboard({ ...dashboard, tabs: updatedTabs })
      setSelectedComponentId(newComponent.id)
      setUnsavedChanges(true)
      loadComponentData(newComponent.id, newComponent)
    } else {
      const components = dashboard.tabs[activeTabIndex].components
      const oldIndex = components.findIndex(c => c.id === active.id)
      if (oldIndex > -1) {
        const updatedComponents = [...components]
        const comp = updatedComponents[oldIndex]
        updatedComponents[oldIndex] = {
          ...comp,
          x: Math.max(0, comp.x + Math.round(delta.x / GRID_SIZE) * GRID_SIZE),
          y: Math.max(0, comp.y + Math.round(delta.y / GRID_SIZE) * GRID_SIZE)
        }

        const updatedTabs = [...dashboard.tabs]
        updatedTabs[activeTabIndex] = {
          ...updatedTabs[activeTabIndex],
          components: updatedComponents
        }
        setDashboard({ ...dashboard, tabs: updatedTabs })
        setUnsavedChanges(true)
      }
    }
  }

  const handleSave = async () => {
    if (!dashboard || !id) return

    try {
      setSaving(true)
      await dashboardApi.update(id, dashboard)
      message.success('保存成功')
      setUnsavedChanges(false)
    } catch (error) {
      console.error('Failed to save dashboard:', error)
      message.error('保存失败')
    } finally {
      setSaving(false)
    }
  }

  const handlePreview = () => {
    if (id) {
      window.open(`/dashboard/${id}/view`, '_blank')
    }
  }

  const handlePublish = async () => {
    if (!id) return
    try {
      await dashboardApi.publish(id)
      message.success('发布成功')
      loadDashboard()
    } catch (error) {
      console.error('Failed to publish dashboard:', error)
      message.error('发布失败')
    }
  }

  const handleDeleteComponent = (componentId: string) => {
    if (!dashboard || !dashboard.tabs) return

    const updatedTabs = [...dashboard.tabs]
    updatedTabs[activeTabIndex] = {
      ...updatedTabs[activeTabIndex],
      components: updatedTabs[activeTabIndex].components.filter(c => c.id !== componentId)
    }

    setDashboard({ ...dashboard, tabs: updatedTabs })
    setSelectedComponentId(null)
    setUnsavedChanges(true)
  }

  const handleUpdateComponent = (componentId: string, updates: Partial<DashboardComponent>) => {
    if (!dashboard || !dashboard.tabs) return

    const updatedTabs = [...dashboard.tabs]
    const components = [...updatedTabs[activeTabIndex].components]
    const idx = components.findIndex(c => c.id === componentId)
    if (idx > -1) {
      components[idx] = { ...components[idx], ...updates }
      updatedTabs[activeTabIndex] = {
        ...updatedTabs[activeTabIndex],
        components
      }
      setDashboard({ ...dashboard, tabs: updatedTabs })
      setUnsavedChanges(true)

      if (updates.config || updates.dataModelId) {
        loadComponentData(componentId, components[idx])
      }
    }
  }

  const handleUpdateConfig = (componentId: string, config: Partial<ComponentConfig>) => {
    if (!dashboard || !dashboard.tabs) return

    const updatedTabs = [...dashboard.tabs]
    const components = [...updatedTabs[activeTabIndex].components]
    const idx = components.findIndex(c => c.id === componentId)
    if (idx > -1) {
      components[idx] = {
        ...components[idx],
        config: { ...components[idx].config, ...config }
      }
      updatedTabs[activeTabIndex] = {
        ...updatedTabs[activeTabIndex],
        components
      }
      setDashboard({ ...dashboard, tabs: updatedTabs })
      setUnsavedChanges(true)
      loadComponentData(componentId, components[idx])
    }
  }

  const handleAddTab = () => {
    if (!dashboard) return

    const newTab: DashboardTab = {
      id: `tab_${Date.now()}`,
      name: `标签${(dashboard.tabs?.length || 0) + 1}`,
      orderIndex: dashboard.tabs?.length || 0,
      components: []
    }

    setDashboard({
      ...dashboard,
      tabs: [...(dashboard.tabs || []), newTab]
    })
    setActiveTabIndex(dashboard.tabs?.length || 0)
    setUnsavedChanges(true)
  }

  const handleDeleteTab = (tabIndex: number) => {
    if (!dashboard || !dashboard.tabs || dashboard.tabs.length <= 1) {
      message.warning('至少保留一个标签页')
      return
    }

    const updatedTabs = dashboard.tabs.filter((_, idx) => idx !== tabIndex)
    setDashboard({ ...dashboard, tabs: updatedTabs })
    if (activeTabIndex >= tabIndex && activeTabIndex > 0) {
      setActiveTabIndex(activeTabIndex - 1)
    }
    setUnsavedChanges(true)
  }

  const handleUpdateTab = (tabIndex: number, name: string) => {
    if (!dashboard || !dashboard.tabs) return

    const updatedTabs = [...dashboard.tabs]
    updatedTabs[tabIndex] = { ...updatedTabs[tabIndex], name }
    setDashboard({ ...dashboard, tabs: updatedTabs })
    setUnsavedChanges(true)
  }

  const handleResizeComponent = (componentId: string, width: number, height: number) => {
    handleUpdateComponent(componentId, {
      width: Math.round(width / GRID_SIZE) * GRID_SIZE,
      height: Math.round(height / GRID_SIZE) * GRID_SIZE
    })
  }

  const selectedComponent = dashboard?.tabs?.[activeTabIndex]?.components?.find(
    c => c.id === selectedComponentId
  )

  const renderCanvasGrid = () => {
    const cols = 24
    const rows = 40
    const gridItems = []

    for (let i = 0; i < rows; i++) {
      for (let j = 0; j < cols; j++) {
        gridItems.push(
          <div
            key={`${i}-${j}`}
            className="grid-cell"
            style={{
              left: j * GRID_SIZE,
              top: i * GRID_SIZE,
              width: GRID_SIZE,
              height: GRID_SIZE
            }}
          />
        )
      }
    }

    return gridItems
  }

  if (loading && !dashboard) {
    return <div className="loading-container">加载中...</div>
  }

  const currentTab = dashboard?.tabs?.[activeTabIndex]

  return (
    <Layout className="dashboard-editor">
      <Header className="editor-header">
        <div className="header-left">
          <Button
            type="text"
            icon={<ArrowLeftOutlined />}
            onClick={() => navigate('/dashboard')}
          >
            返回
          </Button>
          <Input
            className="dashboard-title-input"
            value={dashboard?.name}
            onChange={(e) => {
              setDashboard(prev => prev ? { ...prev, name: e.target.value } : null)
              setUnsavedChanges(true)
            }}
            placeholder="仪表板名称"
          />
          {dashboard?.isPublished && <Tag color="success">已发布</Tag>}
          {unsavedChanges && <Tag color="orange">未保存</Tag>}
        </div>
        <div className="header-right">
          <Space>
            <Button
              icon={<BgColorsOutlined />}
              onClick={() => {
                const newTheme = dashboard?.theme === 'DARK' ? 'LIGHT' : 'DARK'
                setDashboard(prev => prev ? { ...prev, theme: newTheme } : null)
                setUnsavedChanges(true)
              }}
            >
              {dashboard?.theme === 'DARK' ? '浅色' : '深色'}
            </Button>
            <Select
              value={dashboard?.autoRefreshInterval || 0}
              style={{ width: 120 }}
              onChange={(value) => {
                setDashboard(prev => prev ? { ...prev, autoRefreshInterval: value } : null)
                setUnsavedChanges(true)
              }}
            >
              <Option value={0}>关闭刷新</Option>
              <Option value={30}>30秒</Option>
              <Option value={60}>1分钟</Option>
              <Option value={300}>5分钟</Option>
            </Select>
            <Button icon={<FilterOutlined />} onClick={() => setShowGlobalFilters(!showGlobalFilters)}>
              全局筛选
            </Button>
            <Button icon={<ReloadOutlined />} onClick={refreshAllComponents}>
              刷新全部
            </Button>
            <Button icon={<EyeOutlined />} onClick={handlePreview}>
              预览
            </Button>
            <Button icon={<SaveOutlined />} onClick={handleSave} loading={saving}>
              保存
            </Button>
            <Button type="primary" icon={<PlayCircleOutlined />} onClick={handlePublish}>
              发布
            </Button>
          </Space>
        </div>
      </Header>

      <Layout>
        <Sider width={220} className="component-palette">
          <div className="palette-title">组件库</div>
          <List
            grid={{ gutter: 8, column: 2 }}
            dataSource={availableChartTypes}
            renderItem={(type) => (
              <List.Item>
                <div
                  className="chart-type-item"
                  draggable
                  onDragStart={(e) => {
                    e.dataTransfer.setData('chartType', type)
                    setDraggedType(type)
                  }}
                  onDragEnd={() => setDraggedType(null)}
                >
                  <div className="chart-icon">{ChartTypeIcons[type]}</div>
                  <div className="chart-label">{ChartTypeLabels[type]}</div>
                </div>
              </List.Item>
            )}
          />
          
          <Divider />
          
          <div className="palette-title">数据模型</div>
          <Select
            style={{ width: '100%', marginBottom: 12 }}
            placeholder="选择默认数据模型"
            value={selectedComponent?.dataModelId}
            onChange={(value) => {
              if (selectedComponentId) {
                handleUpdateComponent(selectedComponentId, { dataModelId: value })
              }
            }}
          >
            {dataModels.map(model => (
              <Option key={model.id} value={model.id}>{model.name}</Option>
            ))}
          </Select>

          {selectedComponent && (
            <>
              <Divider />
              <div className="palette-title">字段</div>
              <Collapse defaultActiveKey={['dimensions', 'measures']} ghost>
                <Panel header="维度" key="dimensions">
                  {(() => {
                    const model = dataModels.find(m => m.id === selectedComponent.dataModelId)
                    const dims = model?.tables
                      ?.flatMap(t => model.tables.flatMap(t => t.tableName)) || []
                    return dims.length > 0 ? dims.map((dim, idx) => (
                      <Tag key={idx} style={{ margin: 2 }}>{dim}</Tag>
                    )) : <Empty description="暂无维度" image={null} style={{ padding: 0 }} />
                  })()}
                </Panel>
                <Panel header="度量" key="measures">
                  {(() => {
                    const model = dataModels.find(m => m.id === selectedComponent.dataModelId)
                    return model?.measures?.map(measure => (
                      <Tag key={measure.id} color="blue" style={{ margin: 2 }}>{measure.name}</Tag>
                    )) || <Empty description="暂无数值" image={null} style={{ padding: 0 }} />
                  })()}
                </Panel>
              </Collapse>
            </>
          )}
        </Sider>

        <Content className="editor-content">
          {showGlobalFilters && (
            <FilterPanel
              filters={globalFilters}
              onChange={setGlobalFilters}
              onClose={() => setShowGlobalFilters(false)}
            />
          )}

          <Tabs
            activeKey={activeTabIndex.toString()}
            onChange={(key) => setActiveTabIndex(parseInt(key))}
            type="editable-card"
            onEdit={(targetKey, action) => {
              if (action === 'add') {
                handleAddTab()
              } else if (action === 'remove') {
                handleDeleteTab(parseInt(targetKey as string))
              }
            }}
            onTabEdit={(targetKey, action) => {
              if (action === 'remove') {
                handleDeleteTab(parseInt(targetKey as string))
              }
            }}
            renderTabBar={(props, DefaultTabBar) => (
              <DefaultTabBar {...props}>
                {node => node}
              </DefaultTabBar>
            )}
          >
            {dashboard?.tabs?.map((tab, idx) => (
              <TabPane
                key={idx.toString()}
                tab={
                  <Input
                    value={tab.name}
                    onClick={(e) => e.stopPropagation()}
                    onChange={(e) => handleUpdateTab(idx, e.target.value)}
                    bordered={false}
                    style={{ width: 100 }}
                  />
                }
                closeIcon={dashboard.tabs.length > 1 ? <DeleteOutlined /> : null}
              >
                <div
                  className="canvas-container"
                  onDragOver={(e) => e.preventDefault()}
                  onDrop={(e) => {
                    e.preventDefault()
                    const chartType = e.dataTransfer.getData('chartType') as ChartType
                    if (chartType && availableChartTypes.includes(chartType)) {
                      const rect = e.currentTarget.getBoundingClientRect()
                      const x = Math.floor((e.clientX - rect.left) / GRID_SIZE) * GRID_SIZE
                      const y = Math.floor((e.clientY - rect.top) / GRID_SIZE) * GRID_SIZE

                      const newComponent: DashboardComponent = {
                        id: `comp_${Date.now()}`,
                        type: chartType,
                        title: ChartTypeLabels[chartType],
                        dataModelId: dataModels[0]?.id || '',
                        x: x,
                        y: y,
                        width: DEFAULT_COMPONENT_WIDTH * GRID_SIZE,
                        height: DEFAULT_COMPONENT_HEIGHT * GRID_SIZE,
                        config: {
                          dimensions: [],
                          measures: [],
                          showLegend: true,
                          showTooltip: true
                        },
                        filters: [],
                        linkedComponents: []
                      }

                      const updatedTabs = [...dashboard.tabs]
                      if (!updatedTabs[idx].components) {
                        updatedTabs[idx].components = []
                      }
                      updatedTabs[idx].components = [...updatedTabs[idx].components, newComponent]
                      setDashboard({ ...dashboard, tabs: updatedTabs })
                      setSelectedComponentId(newComponent.id)
                      setUnsavedChanges(true)
                      loadComponentData(newComponent.id, newComponent)
                    }
                  }}
                >
                  <div className="grid-background">
                    {renderCanvasGrid()}
                  </div>

                  <DndContext
                    sensors={sensors}
                    collisionDetection={closestCenter}
                    onDragStart={handleDragStart}
                    onDragEnd={handleDragEnd}
                  >
                    <SortableContext
                      items={currentTab?.components?.map(c => c.id) || []}
                      strategy={rectSortingStrategy}
                    >
                      {currentTab?.components?.map((component) => {
                        const ChartComp = ChartComponents[component.type]
                        return (
                          <SortableComponent
                            key={component.id}
                            id={component.id}
                            component={component}
                            isSelected={selectedComponentId === component.id}
                            onSelect={() => setSelectedComponentId(component.id)}
                            onDelete={() => handleDeleteComponent(component.id)}
                            onResize={(w, h) => handleResizeComponent(component.id, w, h)}
                          >
                            <ChartComp
                              title={component.title}
                              data={componentData[component.id]}
                              config={component.config}
                              loading={componentLoading[component.id]}
                              height={component.height - 40}
                              onRefresh={() => loadComponentData(component.id, component)}
                            />
                          </SortableComponent>
                        )
                      })}
                    </SortableContext>

                    <DragOverlay>
                      {draggedType ? (
                        <div className="drag-overlay">
                          {ChartTypeIcons[draggedType]} {ChartTypeLabels[draggedType]}
                        </div>
                      ) : null}
                    </DragOverlay>
                  </DndContext>

                  {(!currentTab?.components || currentTab.components.length === 0) && (
                    <div className="empty-canvas">
                      <div className="empty-icon">📊</div>
                      <div className="empty-text">从左侧拖拽组件到画布开始设计</div>
                    </div>
                  )}
                </div>
              </TabPane>
            ))}
          </Tabs>
        </Content>

        <Sider width={320} className="property-panel" theme="light">
          <div className="panel-tabs">
            <Button
              type={activePanel === 'property' ? 'primary' : 'default'}
              onClick={() => setActivePanel('property')}
              icon={<SettingOutlined />}
              block
            >
              属性
            </Button>
            <Button
              type={activePanel === 'filter' ? 'primary' : 'default'}
              onClick={() => setActivePanel('filter')}
              icon={<FilterOutlined />}
              block
            >
              筛选
            </Button>
          </div>

          <div className="panel-content">
            {selectedComponent ? (
              activePanel === 'property' ? (
                <ComponentPropertyPanel
                  component={selectedComponent}
                  dataModels={dataModels}
                  onUpdateConfig={(config) => handleUpdateConfig(selectedComponent.id, config)}
                  onUpdateComponent={(updates) => handleUpdateComponent(selectedComponent.id, updates)}
                />
              ) : (
                <FilterPanel
                  filters={selectedComponent.filters || []}
                  onChange={(filters) => handleUpdateComponent(selectedComponent.id, { filters })}
                  dataModelId={selectedComponent.dataModelId}
                />
              )
            ) : (
              <div className="no-selection">
                <div className="no-selection-icon">👆</div>
                <div className="no-selection-text">选择一个组件以编辑属性</div>
              </div>
            )}
          </div>
        </Sider>
      </Layout>
    </Layout>
  )
}

export default DashboardEditor
