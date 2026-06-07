import React, { useState, useEffect, useRef } from 'react'
import {
  Button,
  Tabs,
  Space,
  message,
  Modal,
  Form,
  Input,
  Select,
  InputNumber,
  Switch,
  List,
  Tag,
  Card,
  Empty,
  Tooltip,
  Divider,
  Row,
  Col
} from 'antd'
import {
  PlusOutlined,
  SaveOutlined,
  PlayCircleOutlined,
  TableOutlined,
  ReloadOutlined,
  DeleteOutlined,
  EditOutlined,
  LinkOutlined,
  FunctionOutlined,
  CalculatorOutlined,
  OrderedListOutlined,
  ArrowLeftOutlined
} from '@ant-design/icons'
import { useNavigate, useParams } from 'react-router-dom'
import type { DataModel, ModelTable, ModelRelation, CalculatedField, Measure, DimensionHierarchy } from '@/types'
import { dataModelApi, dataSourceApi } from '@/api'
import { getColumnTypeIcon, deepClone } from '@/utils'

const { Option } = Select
const { TabPane } = Tabs

const DataModelEditor: React.FC = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [model, setModel] = useState<DataModel | null>(null)
  const [tables, setTables] = useState<ModelTable[]>([])
  const [relations, setRelations] = useState<ModelRelation[]>([])
  const [calculatedFields, setCalculatedFields] = useState<CalculatedField[]>([])
  const [measures, setMeasures] = useState<Measure[]>([])
  const [hierarchies, setHierarchies] = useState<DimensionHierarchy[]>([])
  const [availableTables, setAvailableTables] = useState<any[]>([])
  const [loading, setLoading] = useState(false)
  const [activeTab, setActiveTab] = useState('tables')
  const [relationModalVisible, setRelationModalVisible] = useState(false)
  const [calcFieldModalVisible, setCalcFieldModalVisible] = useState(false)
  const [measureModalVisible, setMeasureModalVisible] = useState(false)
  const [hierarchyModalVisible, setHierarchyModalVisible] = useState(false)
  const [editingItem, setEditingItem] = useState<any>(null)
  const [form] = Form.useForm()
  const canvasRef = useRef<HTMLDivElement>(null)
  const [draggedTable, setDraggedTable] = useState<string | null>(null)
  const [selectedTable, setSelectedTable] = useState<string | null>(null)

  const loadData = async () => {
    if (!id) return
    try {
      setLoading(true)
      const modelData = await dataModelApi.getById(id)
      const dsTables = await dataSourceApi.getTables(modelData.dataSourceId)
      setModel(modelData)
      setTables(modelData.tables || [])
      setRelations(modelData.relations || [])
      setCalculatedFields(modelData.calculatedFields || [])
      setMeasures(modelData.measures || [])
      setHierarchies(modelData.dimensionHierarchies || [])
      setAvailableTables(dsTables)
    } catch (error) {
      console.error('Failed to load data model:', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [id])

  const handleSave = async () => {
    if (!id || !model) return
    try {
      await dataModelApi.update(id, {
        ...model,
        tables,
        relations,
        calculatedFields,
        measures,
        dimensionHierarchies: hierarchies
      })
      message.success('保存成功')
    } catch (error) {
      console.error('Failed to save data model:', error)
    }
  }

  const handleAddTable = (tableMetadata: any) => {
    const exists = tables.find(t => t.tableMetadataId === tableMetadata.id)
    if (exists) {
      message.warning('该表已添加')
      return
    }
    
    const newTable: ModelTable = {
      id: 'temp_' + Date.now(),
      tableMetadataId: tableMetadata.id,
      tableName: tableMetadata.name,
      alias: tableMetadata.name,
      isEnabled: true,
      positionX: 50 + tables.length * 220,
      positionY: 50
    }
    setTables([...tables, newTable])
  }

  const handleRemoveTable = (tableId: string) => {
    setTables(tables.filter(t => t.id !== tableId))
    setRelations(relations.filter(r => r.sourceTableId !== tableId && r.targetTableId !== tableId))
  }

  const handleTablePositionChange = (tableId: string, x: number, y: number) => {
    setTables(tables.map(t => 
      t.id === tableId ? { ...t, positionX: x, positionY: y } : t
    ))
  }

  const handleAddRelation = () => {
    setEditingItem(null)
    form.resetFields()
    form.setFieldsValue({
      relationType: 'ONE_TO_MANY',
      joinType: 'LEFT'
    })
    setRelationModalVisible(true)
  }

  const handleEditRelation = (relation: ModelRelation) => {
    setEditingItem(relation)
    form.setFieldsValue(relation)
    setRelationModalVisible(true)
  }

  const handleSaveRelation = async () => {
    try {
      const values = await form.validateFields()
      if (editingItem) {
        setRelations(relations.map(r => r.id === editingItem.id ? { ...r, ...values } : r))
      } else {
        const newRelation: ModelRelation = {
          ...values,
          id: 'temp_' + Date.now()
        }
        setRelations([...relations, newRelation])
      }
      setRelationModalVisible(false)
      message.success('保存成功')
    } catch (error) {
      console.error('Failed to save relation:', error)
    }
  }

  const handleDeleteRelation = (relationId: string) => {
    setRelations(relations.filter(r => r.id !== relationId))
  }

  const handleAddCalcField = () => {
    setEditingItem(null)
    form.resetFields()
    form.setFieldsValue({
      returnType: 'DECIMAL',
      isActive: true
    })
    setCalcFieldModalVisible(true)
  }

  const handleEditCalcField = (field: CalculatedField) => {
    setEditingItem(field)
    form.setFieldsValue(field)
    setCalcFieldModalVisible(true)
  }

  const handleSaveCalcField = async () => {
    try {
      const values = await form.validateFields()
      
      if (model) {
        const validation = await dataModelApi.validateExpression(
          model.id,
          values.expression,
          values.returnType
        )
        
        if (!validation.valid) {
          message.error('表达式验证失败: ' + validation.message)
          return
        }
      }
      
      if (editingItem) {
        setCalculatedFields(calculatedFields.map(f => 
          f.id === editingItem.id ? { ...f, ...values } : f
        ))
      } else {
        const newField: CalculatedField = {
          ...values,
          id: 'temp_' + Date.now()
        }
        setCalculatedFields([...calculatedFields, newField])
      }
      setCalcFieldModalVisible(false)
      message.success('保存成功')
    } catch (error) {
      console.error('Failed to save calculated field:', error)
    }
  }

  const handleDeleteCalcField = (fieldId: string) => {
    setCalculatedFields(calculatedFields.filter(f => f.id !== fieldId))
  }

  const handleAddMeasure = () => {
    setEditingItem(null)
    form.resetFields()
    form.setFieldsValue({
      aggregationType: 'SUM',
      isActive: true
    })
    setMeasureModalVisible(true)
  }

  const handleEditMeasure = (measure: Measure) => {
    setEditingItem(measure)
    form.setFieldsValue(measure)
    setMeasureModalVisible(true)
  }

  const handleSaveMeasure = async () => {
    try {
      const values = await form.validateFields()
      if (editingItem) {
        setMeasures(measures.map(m => m.id === editingItem.id ? { ...m, ...values } : m))
      } else {
        const newMeasure: Measure = {
          ...values,
          id: 'temp_' + Date.now()
        }
        setMeasures([...measures, newMeasure])
      }
      setMeasureModalVisible(false)
      message.success('保存成功')
    } catch (error) {
      console.error('Failed to save measure:', error)
    }
  }

  const handleDeleteMeasure = (measureId: string) => {
    setMeasures(measures.filter(m => m.id !== measureId))
  }

  const handleAddHierarchy = () => {
    setEditingItem(null)
    form.resetFields()
    form.setFieldsValue({
      isActive: true,
      levels: []
    })
    setHierarchyModalVisible(true)
  }

  const handleEditHierarchy = (hierarchy: DimensionHierarchy) => {
    setEditingItem(hierarchy)
    form.setFieldsValue(hierarchy)
    setHierarchyModalVisible(true)
  }

  const handleSaveHierarchy = async () => {
    try {
      const values = await form.validateFields()
      if (editingItem) {
        setHierarchies(hierarchies.map(h => h.id === editingItem.id ? { ...h, ...values } : h))
      } else {
        const newHierarchy: DimensionHierarchy = {
          ...values,
          id: 'temp_' + Date.now()
        }
        setHierarchies([...hierarchies, newHierarchy])
      }
      setHierarchyModalVisible(false)
      message.success('保存成功')
    } catch (error) {
      console.error('Failed to save hierarchy:', error)
    }
  }

  const handleDeleteHierarchy = (hierarchyId: string) => {
    setHierarchies(hierarchies.filter(h => h.id !== hierarchyId))
  }

  const getTableColumns = (tableId: string) => {
    const table = tables.find(t => t.id === tableId)
    if (!table) return []
    const metaTable = availableTables.find(t => t.id === table.tableMetadataId)
    return metaTable?.columns || []
  }

  const renderModelCanvas = () => (
    <div style={{ display: 'flex', height: 'calc(100vh - 200px)' }}>
      <div style={{ width: 280, padding: 16, background: '#fff', borderRight: '1px solid #f0f0f0', overflowY: 'auto' }}>
        <h4 style={{ marginBottom: 12 }}>可用数据表</h4>
        {availableTables.length === 0 ? (
          <Empty description="暂无可用表" />
        ) : (
          <List
            size="small"
            dataSource={availableTables}
            renderItem={(item) => {
              const isAdded = tables.find(t => t.tableMetadataId === item.id)
              return (
                <List.Item
                  style={{
                    opacity: isAdded ? 0.5 : 1,
                    cursor: isAdded ? 'not-allowed' : 'pointer'
                  }}
                  onClick={() => !isAdded && handleAddTable(item)}
                >
                  <Space>
                    <TableOutlined />
                    <span>{item.displayName || item.name}</span>
                    <Tag color="blue">{item.rowCount?.toLocaleString() || 0} 行</Tag>
                  </Space>
                </List.Item>
              )
            }}
          />
        )}
      </div>

      <div className="model-canvas" ref={canvasRef}>
        {tables.map((table) => {
          const metaTable = availableTables.find(t => t.id === table.tableMetadataId)
          return (
            <div
              key={table.id}
              className={`model-table-node ${selectedTable === table.id ? 'selected' : ''}`}
              style={{ left: table.positionX, top: table.positionY }}
              onClick={() => setSelectedTable(table.id)}
              draggable
              onDragStart={() => setDraggedTable(table.id)}
              onDragEnd={(e) => {
                if (canvasRef.current) {
                  const rect = canvasRef.current.getBoundingClientRect()
                  const x = e.clientX - rect.left - 100
                  const y = e.clientY - rect.top - 30
                  handleTablePositionChange(table.id, Math.max(0, x), Math.max(0, y))
                }
                setDraggedTable(null)
              }}
            >
              <div className="model-table-header">
                <Space>
                  <TableOutlined />
                  <span>{table.alias}</span>
                  <Button
                    type="text"
                    size="small"
                    danger
                    icon={<DeleteOutlined />}
                    onClick={(e) => {
                      e.stopPropagation()
                      handleRemoveTable(table.id)
                    }}
                    style={{ padding: 0, marginLeft: 'auto' }}
                  />
                </Space>
              </div>
              <div className="model-table-columns">
                {metaTable?.columns?.slice(0, 10).map((col: any) => (
                  <div key={col.id} className="model-table-column">
                    {col.isPrimaryKey && <span className="primary-key">🔑</span>}
                    {col.isForeignKey && <span className="foreign-key">🔗</span>}
                    <span>{getColumnTypeIcon(col.dataType)}</span>
                    <span>{col.displayName || col.name}</span>
                  </div>
                ))}
                {metaTable?.columns?.length > 10 && (
                  <div style={{ padding: '4px 12px', color: '#8c8c8c', fontSize: 12 }}>
                    还有 {metaTable.columns.length - 10} 列...
                  </div>
                )}
              </div>
            </div>
          )
        })}

        {tables.length === 0 && (
          <div style={{ 
            display: 'flex', 
            alignItems: 'center', 
            justifyContent: 'center', 
            height: '100%',
            color: '#8c8c8c'
          }}>
            从左侧拖拽数据表到画布开始建模
          </div>
        )}
      </div>
    </div>
  )

  const renderRelations = () => (
    <div style={{ padding: 16 }}>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
        <h4 style={{ margin: 0 }}>关联关系 ({relations.length})</h4>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAddRelation}>
          新建关联
        </Button>
      </div>
      
      {relations.length === 0 ? (
        <Empty description="暂无关联关系" />
      ) : (
        <List
          dataSource={relations}
          renderItem={(item) => {
            const sourceTable = tables.find(t => t.id === item.sourceTableId)
            const targetTable = tables.find(t => t.id === item.targetTableId)
            return (
              <List.Item
                actions={[
                  <Button type="text" icon={<EditOutlined />} onClick={() => handleEditRelation(item)} />,
                  <Button type="text" danger icon={<DeleteOutlined />} onClick={() => handleDeleteRelation(item.id)} />
                ]}
              >
                <List.Item.Meta
                  title={
                    <Space>
                      <LinkOutlined />
                      <span>
                        {sourceTable?.alias || item.sourceTableId} → {targetTable?.alias || item.targetTableId}
                      </span>
                    </Space>
                  }
                  description={
                    <Space>
                      <Tag>{item.sourceColumn}</Tag>
                      <span>=</span>
                      <Tag>{item.targetColumn}</Tag>
                      <Tag color="blue">{item.relationType}</Tag>
                      <Tag color="green">{item.joinType}</Tag>
                    </Space>
                  }
                />
              </List.Item>
            )
          }}
        />
      )}
    </div>
  )

  const renderCalculatedFields = () => (
    <div style={{ padding: 16 }}>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
        <h4 style={{ margin: 0 }}>计算字段 ({calculatedFields.length})</h4>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAddCalcField}>
          新建计算字段
        </Button>
      </div>
      
      {calculatedFields.length === 0 ? (
        <Empty description="暂无计算字段" />
      ) : (
        <List
          dataSource={calculatedFields}
          renderItem={(item) => (
            <List.Item
              actions={[
                <Button type="text" icon={<EditOutlined />} onClick={() => handleEditCalcField(item)} />,
                <Button type="text" danger icon={<DeleteOutlined />} onClick={() => handleDeleteCalcField(item.id)} />
              ]}
            >
              <List.Item.Meta
                title={
                  <Space>
                    <FunctionOutlined />
                    <span>{item.name}</span>
                    {item.isActive ? (
                      <Tag color="success">启用</Tag>
                    ) : (
                      <Tag color="default">禁用</Tag>
                    )}
                  </Space>
                }
                description={
                  <div>
                    <div style={{ fontFamily: 'monospace', background: '#f5f5f5', padding: 8, borderRadius: 4, marginBottom: 8 }}>
                      {item.expression}
                    </div>
                    <Space>
                      <Tag color="blue">{item.returnType}</Tag>
                      <span style={{ color: '#8c8c8c' }}>{item.description}</span>
                    </Space>
                  </div>
                }
              />
            </List.Item>
          )}
        />
      )}
    </div>
  )

  const renderMeasures = () => (
    <div style={{ padding: 16 }}>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
        <h4 style={{ margin: 0 }}>度量 ({measures.length})</h4>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAddMeasure}>
          新建度量
        </Button>
      </div>
      
      {measures.length === 0 ? (
        <Empty description="暂无数量" />
      ) : (
        <List
          dataSource={measures}
          renderItem={(item) => (
            <List.Item
              actions={[
                <Button type="text" icon={<EditOutlined />} onClick={() => handleEditMeasure(item)} />,
                <Button type="text" danger icon={<DeleteOutlined />} onClick={() => handleDeleteMeasure(item.id)} />
              ]}
            >
              <List.Item.Meta
                title={
                  <Space>
                    <CalculatorOutlined />
                    <span>{item.name}</span>
                    {item.isActive ? (
                      <Tag color="success">启用</Tag>
                    ) : (
                      <Tag color="default">禁用</Tag>
                    )}
                  </Space>
                }
                description={
                  <Space>
                    <Tag color="blue">{item.aggregationType}</Tag>
                    <Tag>{item.columnName}</Tag>
                    {item.filterCondition && (
                      <Tag color="orange">带过滤</Tag>
                    )}
                    {item.description && (
                      <span style={{ color: '#8c8c8c' }}>{item.description}</span>
                    )}
                  </Space>
                }
              />
            </List.Item>
          )}
        />
      )}
    </div>
  )

  const renderHierarchies = () => (
    <div style={{ padding: 16 }}>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
        <h4 style={{ margin: 0 }}>维度层级 ({hierarchies.length})</h4>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAddHierarchy}>
          新建层级
        </Button>
      </div>
      
      {hierarchies.length === 0 ? (
        <Empty description="暂无维度层级" />
      ) : (
        <List
          dataSource={hierarchies}
          renderItem={(item) => (
            <List.Item
              actions={[
                <Button type="text" icon={<EditOutlined />} onClick={() => handleEditHierarchy(item)} />,
                <Button type="text" danger icon={<DeleteOutlined />} onClick={() => handleDeleteHierarchy(item.id)} />
              ]}
            >
              <List.Item.Meta
                title={
                  <Space>
                    <OrderedListOutlined />
                    <span>{item.name}</span>
                    {item.isActive ? (
                      <Tag color="success">启用</Tag>
                    ) : (
                      <Tag color="default">禁用</Tag>
                    )}
                  </Space>
                }
                description={
                  <Space>
                    {item.levels?.map((level, index) => (
                      <React.Fragment key={level.id}>
                        {index > 0 && <span>→</span>}
                        <Tag color="green">{level.name}</Tag>
                      </React.Fragment>
                    ))}
                  </Space>
                }
              />
            </List.Item>
          )}
        />
      )}
    </div>
  )

  return (
    <div>
      <div className="toolbar">
        <div className="toolbar-left">
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/datamodel')}>
            返回
          </Button>
          <span style={{ fontWeight: 500, fontSize: 16 }}>{model?.name || '数据模型编辑器'}</span>
        </div>
        <div className="toolbar-right">
          <Button icon={<ReloadOutlined />} onClick={loadData}>刷新</Button>
          <Button icon={<SaveOutlined />} onClick={handleSave} type="primary">保存</Button>
          <Button icon={<PlayCircleOutlined />}>预览</Button>
        </div>
      </div>

      <Tabs activeKey={activeTab} onChange={setActiveTab}>
        <TabPane tab={`表模型 (${tables.length})`} key="tables">
          {renderModelCanvas()}
        </TabPane>
        <TabPane tab={`关联关系 (${relations.length})`} key="relations">
          {renderRelations()}
        </TabPane>
        <TabPane tab={`计算字段 (${calculatedFields.length})`} key="calculated">
          {renderCalculatedFields()}
        </TabPane>
        <TabPane tab={`度量 (${measures.length})`} key="measures">
          {renderMeasures()}
        </TabPane>
        <TabPane tab={`维度层级 (${hierarchies.length})`} key="hierarchies">
          {renderHierarchies()}
        </TabPane>
      </Tabs>

      <Modal
        title={editingItem ? '编辑关联关系' : '新建关联关系'}
        open={relationModalVisible}
        width={600}
        onCancel={() => setRelationModalVisible(false)}
        footer={[
          <Button key="cancel" onClick={() => setRelationModalVisible(false)}>取消</Button>,
          <Button key="submit" type="primary" onClick={handleSaveRelation}>保存</Button>
        ]}
      >
        <Form form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="sourceTableId"
                label="源表"
                rules={[{ required: true, message: '请选择源表' }]}
              >
                <Select placeholder="请选择源表">
                  {tables.map(t => (
                    <Option key={t.id} value={t.id}>{t.alias}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="targetTableId"
                label="目标表"
                rules={[{ required: true, message: '请选择目标表' }]}
              >
                <Select placeholder="请选择目标表">
                  {tables.map(t => (
                    <Option key={t.id} value={t.id}>{t.alias}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="sourceColumn"
                label="源列"
                rules={[{ required: true, message: '请选择源列' }]}
              >
                <Select placeholder="请选择源列">
                  {form.getFieldValue('sourceTableId') && getTableColumns(form.getFieldValue('sourceTableId')).map((col: any) => (
                    <Option key={col.id} value={col.name}>{col.displayName || col.name}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="targetColumn"
                label="目标列"
                rules={[{ required: true, message: '请选择目标列' }]}
              >
                <Select placeholder="请选择目标列">
                  {form.getFieldValue('targetTableId') && getTableColumns(form.getFieldValue('targetTableId')).map((col: any) => (
                    <Option key={col.id} value={col.name}>{col.displayName || col.name}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="relationType"
                label="关联类型"
                rules={[{ required: true, message: '请选择关联类型' }]}
              >
                <Select>
                  <Option value="ONE_TO_ONE">一对一</Option>
                  <Option value="ONE_TO_MANY">一对多</Option>
                  <Option value="MANY_TO_MANY">多对多</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="joinType"
                label="连接类型"
                rules={[{ required: true, message: '请选择连接类型' }]}
              >
                <Select>
                  <Option value="INNER">内连接</Option>
                  <Option value="LEFT">左连接</Option>
                  <Option value="RIGHT">右连接</Option>
                  <Option value="FULL">全连接</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      <Modal
        title={editingItem ? '编辑计算字段' : '新建计算字段'}
        open={calcFieldModalVisible}
        width={600}
        onCancel={() => setCalcFieldModalVisible(false)}
        footer={[
          <Button key="cancel" onClick={() => setCalcFieldModalVisible(false)}>取消</Button>,
          <Button key="validate" onClick={async () => {
            const values = form.getFieldsValue()
            if (model) {
              const result = await dataModelApi.validateExpression(model.id, values.expression, values.returnType)
              if (result.valid) {
                message.success('表达式验证通过')
              } else {
                message.error('表达式验证失败: ' + result.message)
              }
            }
          }}>验证表达式</Button>,
          <Button key="submit" type="primary" onClick={handleSaveCalcField}>保存</Button>
        ]}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="字段名称"
            rules={[{ required: true, message: '请输入字段名称' }]}
          >
            <Input placeholder="请输入字段名称" />
          </Form.Item>
          <Form.Item
            name="expression"
            label="表达式"
            rules={[{ required: true, message: '请输入表达式' }]}
            help="支持四则运算、条件判断、日期函数等，类似Excel语法"
          >
            <Input.TextArea rows={4} placeholder="例如: IF(sales > 1000, '高', '低') 或 price * quantity" />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="returnType"
                label="返回类型"
                rules={[{ required: true, message: '请选择返回类型' }]}
              >
                <Select>
                  <Option value="STRING">字符串</Option>
                  <Option value="INTEGER">整数</Option>
                  <Option value="DECIMAL">小数</Option>
                  <Option value="BOOLEAN">布尔值</Option>
                  <Option value="DATE">日期</Option>
                  <Option value="DATETIME">日期时间</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="isActive"
                label="是否启用"
                valuePropName="checked"
              >
                <Switch />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item
            name="description"
            label="描述"
          >
            <Input.TextArea rows={2} placeholder="请输入描述信息" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={editingItem ? '编辑度量' : '新建度量'}
        open={measureModalVisible}
        width={600}
        onCancel={() => setMeasureModalVisible(false)}
        footer={[
          <Button key="cancel" onClick={() => setMeasureModalVisible(false)}>取消</Button>,
          <Button key="submit" type="primary" onClick={handleSaveMeasure}>保存</Button>
        ]}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="度量名称"
            rules={[{ required: true, message: '请输入度量名称' }]}
          >
            <Input placeholder="请输入度量名称" />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="columnName"
                label="字段"
                rules={[{ required: true, message: '请选择字段' }]}
              >
                <Select placeholder="请选择字段">
                  {tables.flatMap(t => getTableColumns(t.id).map((col: any) => (
                    <Option key={`${t.id}_${col.id}`} value={col.name}>
                      {t.alias}.{col.displayName || col.name}
                    </Option>
                  )))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="aggregationType"
                label="聚合方式"
                rules={[{ required: true, message: '请选择聚合方式' }]}
              >
                <Select>
                  <Option value="SUM">求和 (SUM)</Option>
                  <Option value="AVG">平均值 (AVG)</Option>
                  <Option value="COUNT">计数 (COUNT)</Option>
                  <Option value="COUNTDISTINCT">去重计数 (COUNT DISTINCT)</Option>
                  <Option value="MAX">最大值 (MAX)</Option>
                  <Option value="MIN">最小值 (MIN)</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>
          <Form.Item
            name="filterCondition"
            label="过滤条件"
            help="可选，例如: status = 'active'"
          >
            <Input placeholder="请输入过滤条件" />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="format"
                label="显示格式"
              >
                <Input placeholder="例如: #,##0.00" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="isActive"
                label="是否启用"
                valuePropName="checked"
              >
                <Switch />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item
            name="description"
            label="描述"
          >
            <Input.TextArea rows={2} placeholder="请输入描述信息" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={editingItem ? '编辑维度层级' : '新建维度层级'}
        open={hierarchyModalVisible}
        width={600}
        onCancel={() => setHierarchyModalVisible(false)}
        footer={[
          <Button key="cancel" onClick={() => setHierarchyModalVisible(false)}>取消</Button>,
          <Button key="submit" type="primary" onClick={handleSaveHierarchy}>保存</Button>
        ]}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="层级名称"
            rules={[{ required: true, message: '请输入层级名称' }]}
          >
            <Input placeholder="例如: 时间层级、地理层级" />
          </Form.Item>
          <Form.Item
            name="isActive"
            label="是否启用"
            valuePropName="checked"
          >
            <Switch />
          </Form.Item>
          <Divider />
          <div>
            <div style={{ marginBottom: 8, fontWeight: 500 }}>层级定义（从上到下，从粗到细）</div>
            <List
              size="small"
              dataSource={form.getFieldValue('levels') || []}
              renderItem={(item: any, index) => (
                <List.Item
                  actions={[
                    <Button type="text" size="small" icon={<DeleteOutlined />} onClick={() => {
                      const levels = form.getFieldValue('levels') || []
                      form.setFieldsValue({ levels: levels.filter((_: any, i: number) => i !== index) })
                    }} />
                  ]}
                >
                  <Space>
                    <span>层级 {index + 1}:</span>
                    <Input
                      value={item.name}
                      onChange={(e) => {
                        const levels = form.getFieldValue('levels') || []
                        levels[index].name = e.target.value
                        form.setFieldsValue({ levels: [...levels] })
                      }}
                      placeholder="层级名称"
                      style={{ width: 120 }}
                    />
                    <Select
                      value={item.columnName}
                      onChange={(value) => {
                        const levels = form.getFieldValue('levels') || []
                        levels[index].columnName = value
                        form.setFieldsValue({ levels: [...levels] })
                      }}
                      placeholder="选择字段"
                      style={{ width: 150 }}
                    >
                      {tables.flatMap(t => getTableColumns(t.id).map((col: any) => (
                        <Option key={`${t.id}_${col.id}`} value={col.name}>
                          {t.alias}.{col.displayName || col.name}
                        </Option>
                      )))}
                    </Select>
                  </Space>
                </List.Item>
              )}
            />
            <Button
              type="dashed"
              block
              icon={<PlusOutlined />}
              style={{ marginTop: 8 }}
              onClick={() => {
                const levels = form.getFieldValue('levels') || []
                form.setFieldsValue({
                  levels: [...levels, { id: Date.now().toString(), name: '', columnName: '', levelOrder: levels.length }]
                })
              }}
            >
              添加层级
            </Button>
          </div>
        </Form>
      </Modal>
    </div>
  )
}

export default DataModelEditor
