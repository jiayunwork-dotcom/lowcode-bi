import React, { useState, useEffect, useRef } from 'react'
import {
  Button,
  Space,
  Select,
  Table,
  Card,
  Tabs,
  message,
  Modal,
  Form,
  Input,
  Tag,
  Row,
  Col,
  Statistic,
  Empty,
  Spin,
  Tooltip,
  InputNumber
} from 'antd'
import {
  PlayCircleOutlined,
  SaveOutlined,
  ReloadOutlined,
  DatabaseOutlined,
  ClockCircleOutlined,
  InfoCircleOutlined,
  FileTextOutlined,
  DownloadOutlined,
  HistoryOutlined,
  SlidersOutlined,
  ClearOutlined
} from '@ant-design/icons'
import Editor from '@monaco-editor/react'
import type { QueryResult, QueryColumn, QueryExecutionPlan } from '@/types'
import { queryApi, dataSourceApi } from '@/api'
import { formatDuration, formatNumber, getColumnTypeIcon } from '@/utils'

const { Option } = Select
const { TabPane } = Tabs

interface QueryHistory {
  id: string
  sql: string
  dataSourceId: string
  dataSourceName: string
  executedAt: string
  durationMs: number
  success: boolean
  rowCount?: number
}

const SqlEditor: React.FC = () => {
  const [dataSources, setDataSources] = useState<{ id: string; name: string; type: string }[]>([])
  const [selectedDataSource, setSelectedDataSource] = useState<string>('')
  const [sql, setSql] = useState<string>('SELECT * FROM ')
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState<QueryResult | null>(null)
  const [executionPlan, setExecutionPlan] = useState<QueryExecutionPlan[]>([])
  const [queryHistory, setQueryHistory] = useState<QueryHistory[]>([])
  const [historyModalVisible, setHistoryModalVisible] = useState(false)
  const [paramsModalVisible, setParamsModalVisible] = useState(false)
  const [parameters, setParameters] = useState<{ name: string; value: string }[]>([])
  const [timeout, setTimeout] = useState<number>(60)
  const [useCache, setUseCache] = useState<boolean>(true)
  const [activeTab, setActiveTab] = useState<string>('result')
  const [validationResult, setValidationResult] = useState<{ valid: boolean; message?: string; type?: string } | null>(null)
  const editorRef = useRef<any>(null)
  const [showPlan, setShowPlan] = useState(false)

  const loadDataSources = async () => {
    try {
      const data = await dataSourceApi.getList()
      const activeSources = data.filter(ds => ds.isActive)
      setDataSources(activeSources.map(ds => ({ id: ds.id, name: ds.name, type: ds.type })))
      if (activeSources.length > 0 && !selectedDataSource) {
        setSelectedDataSource(activeSources[0].id)
      }
    } catch (error) {
      console.error('Failed to load data sources:', error)
    }
  }

  useEffect(() => {
    loadDataSources()
  }, [])

  const handleEditorDidMount = (editor: any) => {
    editorRef.current = editor
    editor.focus()
  }

  const extractParameters = (sqlText: string) => {
    const regex = /\{\{(\w+)\}\}/g
    const matches = sqlText.match(regex) || []
    const paramNames = [...new Set(matches.map(m => m.replace(/\{\{|\}\}/g, '')))]
    
    const newParams = paramNames.map(name => {
      const existing = parameters.find(p => p.name === name)
      return existing || { name, value: '' }
    })
    
    setParameters(newParams)
    
    if (newParams.length > 0) {
      setParamsModalVisible(true)
    }
    
    return newParams
  }

  const handleExecute = async () => {
    if (!selectedDataSource) {
      message.error('请先选择数据源')
      return
    }

    if (!sql.trim()) {
      message.error('请输入SQL语句')
      return
    }

    const params = extractParameters(sql)
    
    if (params.length > 0) {
      const hasEmpty = params.some(p => !p.value)
      if (hasEmpty) {
        setParamsModalVisible(true)
        return
      }
    }

    const startTime = Date.now()
    try {
      setLoading(true)
      setResult(null)
      setExecutionPlan([])
      setValidationResult(null)

      const paramMap: Record<string, any> = {}
      params.forEach(p => {
        paramMap[p.name] = p.value
      })
      const resultData = await queryApi.execute({
        sql,
        dataSourceId: selectedDataSource,
        parameters: paramMap,
        timeout,
        useCache
      })

      const duration = Date.now() - startTime
      setResult(resultData)

      const dsName = dataSources.find(ds => ds.id === selectedDataSource)?.name || ''
      const historyItem: QueryHistory = {
        id: Date.now().toString(),
        sql,
        dataSourceId: selectedDataSource,
        dataSourceName: dsName,
        executedAt: new Date().toISOString(),
        durationMs: duration,
        success: true,
        rowCount: resultData.totalRows
      }
      setQueryHistory([historyItem, ...queryHistory.slice(0, 49)])

      if (resultData.isCached) {
        message.success(`查询成功 (来自缓存，${duration}ms)`)
      } else {
        message.success(`查询成功 (${duration}ms，${resultData.totalRows} 行)`)
      }

      setActiveTab('result')
    } catch (error: any) {
      console.error('Query execution failed:', error)
      
      const dsName = dataSources.find(ds => ds.id === selectedDataSource)?.name || ''
      const historyItem: QueryHistory = {
        id: Date.now().toString(),
        sql,
        dataSourceId: selectedDataSource,
        dataSourceName: dsName,
        executedAt: new Date().toISOString(),
        durationMs: Date.now() - startTime,
        success: false
      }
      setQueryHistory([historyItem, ...queryHistory.slice(0, 49)])
    } finally {
      setLoading(false)
    }
  }

  const handleValidate = async () => {
    try {
      const result = await queryApi.validate(sql, selectedDataSource)
      setValidationResult(result)
      if (result.valid) {
        message.success('SQL验证通过，类型: ' + (result.type || 'SELECT'))
      } else {
        message.error('SQL验证失败: ' + result.message)
      }
    } catch (error) {
      console.error('SQL validation failed:', error)
    }
  }

  const handleExplain = async () => {
    try {
      setLoading(true)
      const plan = await queryApi.explain(sql, selectedDataSource)
      setExecutionPlan(plan)
      setShowPlan(true)
      setActiveTab('plan')
    } catch (error) {
      console.error('Failed to get execution plan:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleFormat = () => {
    if (editorRef.current) {
      editorRef.current.getAction('editor.action.formatDocument').run()
    }
  }

  const handleClear = () => {
    setSql('')
    setResult(null)
    setExecutionPlan([])
    setValidationResult(null)
    if (editorRef.current) {
      editorRef.current.focus()
    }
  }

  const handleLoadFromHistory = (item: QueryHistory) => {
    setSql(item.sql)
    setSelectedDataSource(item.dataSourceId)
    setHistoryModalVisible(false)
  }

  const handleExportResult = () => {
    if (!result) return

    let csvContent = result.columns.map(c => c.displayName || c.name).join(',') + '\n'
    result.rows.forEach(row => {
      csvContent += row.join(',') + '\n'
    })

    const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `query_result_${Date.now()}.csv`
    link.click()
    URL.revokeObjectURL(link.href)
  }

  const renderTableData = () => {
    if (!result) return null

    const columns = result.columns.map((col: QueryColumn, index: number) => ({
      title: (
        <Space>
          <span>{getColumnTypeIcon(col.dataType)}</span>
          <span>{col.displayName || col.name}</span>
          <Tag color="blue" style={{ fontSize: 10 }}>{col.dataType}</Tag>
        </Space>
      ),
      dataIndex: index.toString(),
      key: index.toString(),
      render: (value: any) => {
        if (value === null || value === undefined) {
          return <span style={{ color: '#bfbfbf' }}>NULL</span>
        }
        if (typeof value === 'number') {
          return formatNumber(value, 2)
        }
        return String(value)
      }
    }))

    const dataSource = result.rows.map((row: any[], index: number) => {
      const obj: Record<string, any> = { key: index }
      row.forEach((value, i) => {
        obj[i.toString()] = value
      })
      return obj
    })

    return (
      <Table
        dataSource={dataSource}
        columns={columns}
        size="small"
        scroll={{ x: 'max-content', y: 400 }}
        pagination={{
          pageSize: 100,
          showSizeChanger: true,
          pageSizeOptions: ['50', '100', '500', '1000'],
          showTotal: (total) => `共 ${total} 行`
        }}
      />
    )
  }

  const renderExecutionPlan = (plan: QueryExecutionPlan[], level = 0) => {
    return plan.map((step, index) => (
      <div key={`${step.step}-${index}`} style={{ marginLeft: level * 20, marginBottom: 8 }}>
        <Card size="small" style={{ background: '#fafafa' }}>
          <Space>
            <Tag color="blue">Step {step.step}</Tag>
            <strong>{step.operation}</strong>
            {step.tableName && <Tag>{step.tableName}</Tag>}
            {step.filterCondition && (
              <span style={{ color: '#8c8c8c' }}>WHERE {step.filterCondition}</span>
            )}
            <span style={{ marginLeft: 'auto', color: '#52c41a' }}>
              估算行数: {step.estimatedRows?.toLocaleString()}
            </span>
            <span style={{ color: '#faad14' }}>
              Cost: {step.cost}
            </span>
          </Space>
        </Card>
        {step.children && step.children.length > 0 && renderExecutionPlan(step.children, level + 1)}
      </div>
    ))
  }

  return (
    <div className="sql-editor-container">
      <div className="sql-editor-toolbar">
        <div className="toolbar-left">
          <Space>
            <Select
              style={{ width: 200 }}
              placeholder="选择数据源"
              value={selectedDataSource}
              onChange={setSelectedDataSource}
              prefix={<DatabaseOutlined />}
            >
              {dataSources.map(ds => (
                <Option key={ds.id} value={ds.id}>
                  <Space>
                    <DatabaseOutlined style={{ color: ds.type === 'MYSQL' ? '#4479A1' : ds.type === 'POSTGRESQL' ? '#336791' : '#FFCC01' }} />
                    <span>{ds.name}</span>
                    <Tag color="default" style={{ fontSize: 10 }}>{ds.type}</Tag>
                  </Space>
                </Option>
              ))}
            </Select>

            <Tooltip title="参数设置">
              <Button
                icon={<SlidersOutlined />}
                onClick={() => setParamsModalVisible(true)}
              >
                参数
              </Button>
            </Tooltip>

            <InputNumber
              min={10}
              max={600}
              value={timeout}
              onChange={(value) => setTimeout(value as number)}
              addonBefore="超时"
              addonAfter="秒"
              style={{ width: 150 }}
            />

            <span style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
              <input
                type="checkbox"
                checked={useCache}
                onChange={(e) => setUseCache(e.target.checked)}
              />
              <span style={{ fontSize: 12 }}>使用缓存</span>
            </span>
          </Space>
        </div>

        <div className="toolbar-right">
          <Space>
            <Button icon={<HistoryOutlined />} onClick={() => setHistoryModalVisible(true)}>
              历史
            </Button>
            <Button icon={<InfoCircleOutlined />} onClick={handleValidate}>
              验证
            </Button>
            <Button icon={<FileTextOutlined />} onClick={handleExplain}>
              执行计划
            </Button>
            <Button icon={<ClearOutlined />} onClick={handleClear}>
              清空
            </Button>
            <Button
              type="primary"
              icon={<PlayCircleOutlined />}
              onClick={handleExecute}
              loading={loading}
            >
              执行 (Ctrl+Enter)
            </Button>
          </Space>
        </div>
      </div>

      {validationResult && (
        <div style={{
          padding: '8px 16px',
          background: validationResult.valid ? '#f6ffed' : '#fff2f0',
          borderBottom: `1px solid ${validationResult.valid ? '#b7eb8f' : '#ffccc7'}`,
          color: validationResult.valid ? '#52c41a' : '#ff4d4f'
        }}>
          {validationResult.valid ? '✓' : '✗'} {validationResult.message || 'SQL验证通过'}
          {validationResult.type && <Tag style={{ marginLeft: 8 }}>{validationResult.type}</Tag>}
        </div>
      )}

      <div className="sql-editor-content">
        <div className="sql-editor-input">
          <Editor
            height="100%"
            defaultLanguage="sql"
            theme="vs-dark"
            value={sql}
            onChange={(value) => setSql(value || '')}
            onMount={handleEditorDidMount}
            options={{
              minimap: { enabled: false },
              fontSize: 14,
              lineNumbers: 'on',
              automaticLayout: true,
              tabSize: 2,
              wordWrap: 'on',
              suggestOnTriggerCharacters: true,
              quickSuggestions: true,
              acceptSuggestionOnEnter: 'smart'
            }}
          />
        </div>

        <Tabs activeKey={activeTab} onChange={setActiveTab} style={{ flex: 1 }}>
          <TabPane tab={`查询结果${result ? ` (${result.totalRows}行)` : ''}`} key="result">
            <div className="sql-editor-results">
              {loading ? (
                <div className="loading-spinner">
                  <Spin size="large" tip="正在执行查询..." />
                </div>
              ) : result ? (
                <div>
                  <div style={{ padding: '12px 16px', background: '#fafafa', borderBottom: '1px solid #f0f0f0', display: 'flex', justifyContent: 'space-between' }}>
                    <Space>
                      <Tag color={result.isCached ? 'green' : 'blue'}>
                        {result.isCached ? '来自缓存' : '实时查询'}
                      </Tag>
                      <Tag>
                        <ClockCircleOutlined /> {formatDuration(result.executionTimeMs)}
                      </Tag>
                      <Tag>
                        <DatabaseOutlined /> {result.totalRows.toLocaleString()} 行
                      </Tag>
                      {result.cacheKey && (
                        <Tag color="purple">
                          Cache Key: {result.cacheKey.slice(0, 16)}...
                        </Tag>
                      )}
                    </Space>
                    <Button
                      type="text"
                      icon={<DownloadOutlined />}
                      onClick={handleExportResult}
                    >
                      导出CSV
                    </Button>
                  </div>
                  {renderTableData()}
                </div>
              ) : (
                <div className="empty-state">
                  <div className="icon"><FileTextOutlined /></div>
                  <div className="title">暂无查询结果</div>
                  <div className="description">输入SQL语句并点击"执行"按钮开始查询</div>
                </div>
              )}
            </div>
          </TabPane>
          <TabPane tab="执行计划" key="plan">
            <div style={{ padding: 16, maxHeight: '100%', overflow: 'auto' }}>
              {executionPlan.length > 0 ? (
                renderExecutionPlan(executionPlan)
              ) : (
                <Empty description="暂无执行计划，请先点击'执行计划'按钮" />
              )}
            </div>
          </TabPane>
        </Tabs>
      </div>

      <Modal
        title="查询历史"
        open={historyModalVisible}
        width={800}
        onCancel={() => setHistoryModalVisible(false)}
        footer={null}
      >
        {queryHistory.length === 0 ? (
          <Empty description="暂无查询历史" />
        ) : (
          <Table
            dataSource={queryHistory}
            rowKey="id"
            size="small"
            columns={[
              {
                title: '状态',
                dataIndex: 'success',
                key: 'success',
                width: 60,
                render: (success: boolean) => (
                  <span style={{ color: success ? '#52c41a' : '#ff4d4f' }}>
                    {success ? '✓' : '✗'}
                  </span>
                )
              },
              {
                title: 'SQL',
                dataIndex: 'sql',
                key: 'sql',
                ellipsis: true,
                render: (sql: string) => (
                  <code style={{ fontSize: 12 }}>{sql.slice(0, 100)}...</code>
                )
              },
              {
                title: '数据源',
                dataIndex: 'dataSourceName',
                key: 'dataSourceName',
                width: 120
              },
              {
                title: '耗时',
                dataIndex: 'durationMs',
                key: 'durationMs',
                width: 80,
                render: (ms: number) => formatDuration(ms)
              },
              {
                title: '行数',
                dataIndex: 'rowCount',
                key: 'rowCount',
                width: 80,
                render: (count?: number) => count?.toLocaleString() || '-'
              },
              {
                title: '执行时间',
                dataIndex: 'executedAt',
                key: 'executedAt',
                width: 160,
                render: (time: string) => new Date(time).toLocaleString()
              },
              {
                title: '操作',
                key: 'action',
                width: 80,
                render: (_: any, record: QueryHistory) => (
                  <Button type="link" size="small" onClick={() => handleLoadFromHistory(record)}>
                    加载
                  </Button>
                )
              }
            ]}
            pagination={{ pageSize: 10 }}
          />
        )}
      </Modal>

      <Modal
        title="查询参数"
        open={paramsModalVisible}
        width={500}
        onCancel={() => setParamsModalVisible(false)}
        footer={[
          <Button key="cancel" onClick={() => setParamsModalVisible(false)}>取消</Button>,
          <Button key="submit" type="primary" onClick={() => {
            setParamsModalVisible(false)
            handleExecute()
          }}>
            继续执行
          </Button>
        ]}
      >
        <p style={{ marginBottom: 16, color: '#8c8c8c' }}>
          查询中检测到 <Tag color="blue">{parameters.length}</Tag> 个参数，请为其赋值：
        </p>
        <Form layout="vertical">
          {parameters.map((param, index) => (
            <Form.Item key={param.name} label={`{{${param.name}}}`}>
              <Input
                value={param.value}
                placeholder="请输入参数值"
                onChange={(e) => {
                  const newParams = [...parameters]
                  newParams[index].value = e.target.value
                  setParameters(newParams)
                }}
              />
            </Form.Item>
          ))}
        </Form>
      </Modal>
    </div>
  )
}

export default SqlEditor
