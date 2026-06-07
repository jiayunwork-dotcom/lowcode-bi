import React, { useState, useEffect } from 'react'
import { Modal, Table, Select, Tag, message, Button, Space } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { CsvPreviewResponse, ColumnDataType } from '@/types'
import { dataSourceApi } from '@/api'

interface CsvPreviewModalProps {
  visible: boolean
  onCancel: () => void
  previewData: CsvPreviewResponse | null
  dataSourceId?: string
  tableId?: string
  onTypesUpdated?: () => void
  onSuccess?: () => void
}

const COLUMN_TYPE_OPTIONS: { value: ColumnDataType; label: string; color: string }[] = [
  { value: 'STRING', label: '字符串', color: 'blue' },
  { value: 'INTEGER', label: '整数', color: 'green' },
  { value: 'LONG', label: '长整数', color: 'cyan' },
  { value: 'DOUBLE', label: '浮点数', color: 'geekblue' },
  { value: 'DECIMAL', label: '小数', color: 'purple' },
  { value: 'BOOLEAN', label: '布尔', color: 'gold' },
  { value: 'DATE', label: '日期', color: 'orange' },
  { value: 'DATETIME', label: '日期时间', color: 'volcano' },
  { value: 'TEXT', label: '长文本', color: 'default' },
]

const getColumnTypeColor = (type: ColumnDataType): string => {
  const option = COLUMN_TYPE_OPTIONS.find(o => o.value === type)
  return option?.color || 'default'
}

const getColumnTypeLabel = (type: ColumnDataType): string => {
  const option = COLUMN_TYPE_OPTIONS.find(o => o.value === type)
  return option?.label || type
}

const CsvPreviewModal: React.FC<CsvPreviewModalProps> = ({
  visible,
  onCancel,
  previewData,
  dataSourceId,
  tableId,
  onTypesUpdated,
  onSuccess
}) => {
  const [columnTypes, setColumnTypes] = useState<ColumnDataType[]>([])
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (previewData) {
      setColumnTypes([...previewData.columnTypes])
    }
  }, [previewData])

  const handleTypeChange = (index: number, newValue: ColumnDataType) => {
    const newTypes = [...columnTypes]
    newTypes[index] = newValue
    setColumnTypes(newTypes)
  }

  const handleSaveTypes = async () => {
    if (!dataSourceId || !tableId) {
      message.error('缺少数据源或表ID')
      return
    }

    try {
      setSaving(true)
      const typeMap: Record<string, ColumnDataType> = {}
      previewData?.headers.forEach((header, index) => {
        typeMap[header] = columnTypes[index]
      })

      await dataSourceApi.updateColumnTypes(dataSourceId, tableId, typeMap)
      message.success('列类型保存成功')
      onTypesUpdated?.()
      onSuccess?.()
      onCancel()
    } catch (error) {
      console.error('Failed to save column types:', error)
    } finally {
      setSaving(false)
    }
  }

  const columns: ColumnsType<Record<string, any>> = previewData?.headers.map((header, index) => ({
    title: (
      <div style={{ minWidth: 150 }}>
        <div style={{ fontWeight: 'bold', marginBottom: 4 }}>{header}</div>
        <Select
          size="small"
          value={columnTypes[index]}
          onChange={(value) => handleTypeChange(index, value)}
          style={{ width: '100%' }}
          options={COLUMN_TYPE_OPTIONS}
        />
      </div>
    ),
    dataIndex: header,
    key: header,
    width: 180,
    render: (text) => (
      <Space>
        <Tag color={getColumnTypeColor(columnTypes[index])}>
          {getColumnTypeLabel(columnTypes[index])}
        </Tag>
        <span>{text}</span>
      </Space>
    )
  })) || []

  return (
    <Modal
      title={
        <Space>
          <span>CSV数据预览</span>
          {previewData && (
            <Tag color="green">
              {previewData.rowCount} 行 / {previewData.columnCount} 列
            </Tag>
          )}
        </Space>
      }
      open={visible}
      width={1200}
      onCancel={onCancel}
      footer={[
        <Button key="cancel" onClick={onCancel}>
          关闭
        </Button>,
        <Button
          key="save"
          type="primary"
          onClick={handleSaveTypes}
          loading={saving}
          disabled={!dataSourceId || !tableId}
        >
          保存列类型
        </Button>
      ]}
    >
      {previewData && (
        <div>
          <div style={{ marginBottom: 16 }}>
            <p style={{ color: '#666', margin: 0 }}>
              文件: <b>{previewData.fileName}</b> ({(previewData.fileSize / 1024 / 1024).toFixed(2)} MB)
            </p>
            <p style={{ color: '#666', margin: '4px 0 0 0' }}>
              编码: <b>{previewData.charset}</b> | 
              显示前 <b>20</b> 行数据
            </p>
          </div>
          <Table
            dataSource={previewData.rows.slice(0, 20)}
            columns={columns}
            rowKey={(_, index) => `row-${index}`}
            scroll={{ x: 'max-content', y: 500 }}
            pagination={false}
            size="small"
          />
        </div>
      )}
    </Modal>
  )
}

export default CsvPreviewModal
