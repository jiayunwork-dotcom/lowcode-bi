import React, { useState, useEffect } from 'react'
import {
  Form,
  Input,
  Select,
  InputNumber,
  Switch,
  Collapse,
  Space,
  Divider,
  ColorPicker,
  Tag,
  Row,
  Col,
  Button,
  Transfer,
  message
} from 'antd'
import type { Color } from 'antd/es/color-picker'
import type { DashboardComponent, ComponentConfig, DataModel } from '@/types'
import { dataModelApi } from '@/api'

const { Option } = Select
const { Panel } = Collapse

interface ComponentPropertyPanelProps {
  component: DashboardComponent
  dataModels: DataModel[]
  onUpdateConfig: (config: Partial<ComponentConfig>) => void
  onUpdateComponent: (updates: Partial<DashboardComponent>) => void
}

const ComponentPropertyPanel: React.FC<ComponentPropertyPanelProps> = ({
  component,
  dataModels,
  onUpdateConfig,
  onUpdateComponent
}) => {
  const [form] = Form.useForm()
  const [availableFields, setAvailableFields] = useState<{
    dimensions: { id: string; name: string; displayName: string; type: string }[]
    measures: { id: string; name: string; displayName: string; type: string; aggregation: string }[]
    calculatedFields: { id: string; name: string; displayName: string; type: string }[]
  }>({ dimensions: [], measures: [], calculatedFields: [] })

  useEffect(() => {
    form.setFieldsValue({
      title: component.title,
      dataModelId: component.dataModelId,
      ...component.config
    })
  }, [component, form])

  useEffect(() => {
    if (component.dataModelId) {
      loadModelFields(component.dataModelId)
    }
  }, [component.dataModelId])

  const loadModelFields = async (modelId: string) => {
    try {
      const fields = await dataModelApi.getFields(modelId)
      setAvailableFields(fields)
    } catch (error) {
      console.error('Failed to load model fields:', error)
    }
  }

  const handleValuesChange = (changedValues: any) => {
    if (changedValues.title !== undefined) {
      onUpdateComponent({ title: changedValues.title })
    }
    if (changedValues.dataModelId !== undefined) {
      onUpdateComponent({ dataModelId: changedValues.dataModelId })
    }
    if (changedValues.colorPalette) {
      const colors = changedValues.colorPalette.map((c: Color) => c.toHexString())
      onUpdateConfig({ colorPalette: colors })
    } else {
      const configChanges = { ...changedValues }
      delete configChanges.title
      delete configChanges.dataModelId
      delete configChanges.colorPalette
      if (Object.keys(configChanges).length > 0) {
        onUpdateConfig(configChanges)
      }
    }
  }

  const getChartSubTypeOptions = () => {
    const options: Record<string, { label: string; value: string }[]> = {
      LINE: [
        { label: '标准折线', value: 'standard' },
        { label: '平滑曲线', value: 'smooth' },
        { label: '面积图', value: 'area' }
      ],
      BAR: [
        { label: '垂直柱状', value: 'vertical' },
        { label: '水平柱状', value: 'horizontal' },
        { label: '瀑布图', value: 'waterfall' }
      ],
      PIE: [
        { label: '标准饼图', value: 'standard' },
        { label: '环形图', value: 'doughnut' },
        { label: '玫瑰图', value: 'rose' }
      ],
      SCATTER: [
        { label: '散点图', value: 'scatter' },
        { label: '气泡图', value: 'bubble' }
      ],
      FUNNEL: [
        { label: '标准漏斗', value: 'standard' },
        { label: '反向漏斗', value: 'reverse' }
      ]
    }
    return options[component.type] || []
  }

  const allFields = [
    ...availableFields.dimensions.map(d => ({ ...d, category: 'dimension' })),
    ...availableFields.measures.map(m => ({ ...m, category: 'measure' })),
    ...availableFields.calculatedFields.map(c => ({ ...c, category: 'calculated' }))
  ]

  return (
    <div className="property-panel-content">
      <Form
        form={form}
        layout="vertical"
        onValuesChange={handleValuesChange}
        size="small"
      >
        <Collapse defaultActiveKey={['basic', 'data', 'style']} ghost>
          <Panel header="基本属性" key="basic">
            <Form.Item name="title" label="组件标题">
              <Input placeholder="请输入标题" />
            </Form.Item>

            <Form.Item name="dataModelId" label="数据模型">
              <Select placeholder="请选择数据模型">
                {dataModels.map(model => (
                  <Option key={model.id} value={model.id}>{model.name}</Option>
                ))}
              </Select>
            </Form.Item>
          </Panel>

          <Panel header="数据绑定" key="data">
            <Form.Item label="维度字段">
              <Select
                mode="multiple"
                placeholder="选择维度字段"
                value={component.config.dimensions}
                onChange={(value) => onUpdateConfig({ dimensions: value })}
                style={{ width: '100%' }}
                optionFilterProp="label"
              >
                {availableFields.dimensions.map(dim => (
                  <Option key={dim.id} value={dim.id} label={dim.displayName}>
                    <Tag color="blue" style={{ marginRight: 8 }}>维度</Tag>
                    {dim.displayName}
                  </Option>
                ))}
                {availableFields.calculatedFields.map(cf => (
                  <Option key={cf.id} value={cf.id} label={cf.displayName}>
                    <Tag color="purple" style={{ marginRight: 8 }}>计算</Tag>
                    {cf.displayName}
                  </Option>
                ))}
              </Select>
            </Form.Item>

            <Form.Item label="度量字段">
              <Select
                mode="multiple"
                placeholder="选择度量字段"
                value={component.config.measures}
                onChange={(value) => onUpdateConfig({ measures: value })}
                style={{ width: '100%' }}
                optionFilterProp="label"
              >
                {availableFields.measures.map(measure => (
                  <Option key={measure.id} value={measure.id} label={measure.displayName}>
                    <Tag color="green" style={{ marginRight: 8 }}>度量</Tag>
                    {measure.displayName}
                    <span style={{ color: '#8c8c8c', marginLeft: 8 }}>{measure.aggregation}</span>
                  </Option>
                ))}
                {availableFields.calculatedFields.map(cf => (
                  <Option key={cf.id} value={cf.id} label={cf.displayName}>
                    <Tag color="purple" style={{ marginRight: 8 }}>计算</Tag>
                    {cf.displayName}
                  </Option>
                ))}
              </Select>
            </Form.Item>

            {component.type !== 'KPI' && component.type !== 'TABLE' && (
              <Form.Item name="chartSubType" label="图表子类型">
                <Select placeholder="请选择子类型">
                  {getChartSubTypeOptions().map(opt => (
                    <Option key={opt.value} value={opt.value}>{opt.label}</Option>
                  ))}
                </Select>
              </Form.Item>
            )}

            <Divider />

            <Row gutter={8}>
              <Col span={12}>
                <Form.Item name="sortField" label="排序字段">
                  <Select placeholder="选择字段" allowClear>
                    {allFields.map(field => (
                      <Option key={field.id} value={field.id}>{field.displayName}</Option>
                    ))}
                  </Select>
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item name="sortOrder" label="排序方式">
                  <Select placeholder="排序方式" allowClear>
                    <Option value="ASC">升序</Option>
                    <Option value="DESC">降序</Option>
                  </Select>
                </Form.Item>
              </Col>
            </Row>

            <Form.Item name="limit" label="数据条数">
              <InputNumber placeholder="0为不限制" min={0} style={{ width: '100%' }} />
            </Form.Item>
          </Panel>

          <Panel header="样式配置" key="style">
            <Row gutter={8}>
              <Col span={12}>
                <Form.Item name="showLegend" label="显示图例" valuePropName="checked">
                  <Switch />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item name="showTooltip" label="显示提示" valuePropName="checked">
                  <Switch />
                </Form.Item>
              </Col>
            </Row>

            {(component.type === 'LINE' || component.type === 'BAR') && (
              <Form.Item name="stacked" label="堆叠显示" valuePropName="checked">
                <Switch />
              </Form.Item>
            )}

            {component.type === 'LINE' && (
              <Form.Item name="areaStyle" label="面积样式" valuePropName="checked">
                <Switch />
              </Form.Item>
            )}

            <Form.Item name="colorPalette" label="配色方案">
              <ColorPicker
                mode={'multiple' as any}
                defaultValue={['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de'] as any}
                format="hex"
              />
            </Form.Item>

            {component.type === 'TABLE' && (
              <>
                <Divider />
                <div style={{ fontWeight: 500, marginBottom: 8 }}>表格配置</div>
                <Row gutter={8}>
                  <Col span={12}>
                    <Form.Item name={['tableConfig', 'showRowNumbers']} label="显示行号" valuePropName="checked">
                      <Switch />
                    </Form.Item>
                  </Col>
                  <Col span={12}>
                    <Form.Item name={['tableConfig', 'showSummary']} label="显示合计" valuePropName="checked">
                      <Switch />
                    </Form.Item>
                  </Col>
                </Row>
                <Form.Item name={['tableConfig', 'pageSize']} label="每页行数">
                  <InputNumber min={10} max={1000} style={{ width: '100%' }} />
                </Form.Item>
              </>
            )}

            {component.type === 'KPI' && (
              <>
                <Divider />
                <div style={{ fontWeight: 500, marginBottom: 8 }}>KPI配置</div>
                <Form.Item name={['kpiConfig', 'compareType']} label="对比方式">
                  <Select placeholder="选择对比方式" allowClear>
                    <Option value="YEAR_OVER_YEAR">同比</Option>
                    <Option value="QUARTER_OVER_QUARTER">环比季度</Option>
                    <Option value="MONTH_OVER_MONTH">环比月</Option>
                    <Option value="DAY_OVER_DAY">环比日</Option>
                  </Select>
                </Form.Item>
                <Row gutter={8}>
                  <Col span={12}>
                    <Form.Item name={['kpiConfig', 'prefix']} label="前缀">
                      <Input placeholder="如: ¥" />
                    </Form.Item>
                  </Col>
                  <Col span={12}>
                    <Form.Item name={['kpiConfig', 'suffix']} label="后缀">
                      <Input placeholder="如: %" />
                    </Form.Item>
                  </Col>
                </Row>
                <Form.Item name={['kpiConfig', 'decimals']} label="小数位数">
                  <InputNumber min={0} max={10} style={{ width: '100%' }} />
                </Form.Item>
                <Form.Item name={['kpiConfig', 'showTrend']} label="显示趋势" valuePropName="checked">
                  <Switch />
                </Form.Item>
              </>
            )}

            {component.type === 'MAP' && (
              <>
                <Divider />
                <div style={{ fontWeight: 500, marginBottom: 8 }}>地图配置</div>
                <Form.Item name={['mapConfig', 'mapType']} label="地图类型">
                  <Select>
                    <Option value="CHINA_PROVINCE">中国省级地图</Option>
                    <Option value="CHINA_CITY">中国城市地图</Option>
                    <Option value="WORLD">世界地图</Option>
                  </Select>
                </Form.Item>
              </>
            )}
          </Panel>

          <Panel header="联动配置" key="linkage">
            <Form.Item label="联动组件">
              <Select
                mode="multiple"
                placeholder="选择要联动的组件"
                value={component.linkedComponents}
                onChange={(value) => onUpdateComponent({ linkedComponents: value })}
                style={{ width: '100%' }}
              >
                {(() => {
                  const tab = component._tab || { components: [] }
                  return tab.components?.filter((c: DashboardComponent) => c.id !== component.id).map((c: DashboardComponent) => (
                    <Option key={c.id} value={c.id}>{c.title}</Option>
                  )) || []
                })()}
              </Select>
            </Form.Item>
            <div style={{ fontSize: 12, color: '#8c8c8c' }}>
              点击当前组件时，会将选中的维度值作为筛选条件应用到联动组件
            </div>
          </Panel>
        </Collapse>
      </Form>
    </div>
  )
}

export default ComponentPropertyPanel
