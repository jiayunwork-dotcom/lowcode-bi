import React, { useState, useMemo } from 'react'
import { Table, Tag } from 'antd'
import BaseChart from './BaseChart'
import type { QueryResult, ComponentConfig, ConditionalFormat } from '@/types'
import { formatNumber } from '@/utils'

interface TableChartProps {
  title: string
  data?: QueryResult
  config: ComponentConfig
  loading?: boolean
  error?: string
  height?: number
  onRefresh?: () => void
  onSort?: (field: string, order: string) => void
}

const TableChart: React.FC<TableChartProps> = ({
  title,
  data,
  config,
  loading = false,
  error,
  height = 300,
  onRefresh,
  onSort
}) => {
  const [sortedInfo, setSortedInfo] = useState<any>({})

  const { tableConfig = {} } = config
  const { showRowNumbers = true, showSummary = false, conditionalFormats = [], pageSize = 20 } = tableConfig

  const applyConditionalFormat = (value: any, field: string) => {
    const format = conditionalFormats.find((f: ConditionalFormat) => f.field === field)
    if (!format) return {}

    const numValue = typeof value === 'number' ? value : parseFloat(value)
    let match = false

    switch (format.condition) {
      case 'EQ':
        match = value == format.value1
        break
      case 'NE':
        match = value != format.value1
        break
      case 'GT':
        match = numValue > (format.value1 as number)
        break
      case 'GTE':
        match = numValue >= (format.value1 as number)
        break
      case 'LT':
        match = numValue < (format.value1 as number)
        break
      case 'LTE':
        match = numValue <= (format.value1 as number)
        break
      case 'BETWEEN':
        match = numValue >= (format.value1 as number) && numValue <= (format.value2 as number)
        break
      case 'CONTAINS':
        match = String(value).includes(String(format.value1))
        break
    }

    if (match) {
      return {
        backgroundColor: format.style.backgroundColor,
        color: format.style.textColor,
        fontWeight: format.style.fontWeight
      }
    }
    return {}
  }

  const columns = useMemo(() => {
    if (!data?.columns) return []

    return data.columns.map((col, idx) => ({
      title: col.displayName || col.name,
      dataIndex: idx.toString(),
      key: idx.toString(),
      sorter: true,
      sortOrder: sortedInfo.columnKey === idx.toString() ? sortedInfo.order : null,
      ellipsis: true,
      render: (value: any) => {
        const style = applyConditionalFormat(value, col.name)
        
        if (col.dataType === 'DECIMAL' || col.dataType === 'INTEGER') {
          return <span style={style}>{formatNumber(value)}</span>
        }
        return <span style={style}>{value}</span>
      }
    }))
  }, [data, conditionalFormats, sortedInfo])

  const tableData = useMemo(() => {
    if (!data?.rows) return []
    return data.rows.map((row, idx) => {
      const obj: any = { key: idx }
      row.forEach((val, colIdx) => {
        obj[colIdx.toString()] = val
      })
      return obj
    })
  }, [data])

  const summaryData = useMemo(() => {
    if (!showSummary || !data?.rows || data.rows.length === 0) return null

    const summary: any = { key: 'summary' }
    data.columns?.forEach((col, idx) => {
      if (idx === 0) {
        summary[idx.toString()] = <strong>合计</strong>
      } else if (col.dataType === 'DECIMAL' || col.dataType === 'INTEGER') {
        const sum = data.rows.reduce((acc, row) => acc + (parseFloat(row[idx]) || 0), 0)
        summary[idx.toString()] = <strong>{formatNumber(sum)}</strong>
      } else {
        summary[idx.toString()] = '-'
      }
    })
    return summary
  }, [data, showSummary])

  const handleTableChange = (_pagination: any, _filters: any, sorter: any) => {
    setSortedInfo(sorter)
    if (onSort && sorter.field && sorter.order) {
      const colIndex = parseInt(sorter.field as string)
      const fieldName = data?.columns?.[colIndex]?.name
      if (fieldName) {
        onSort(fieldName, sorter.order === 'ascend' ? 'ASC' : 'DESC')
      }
    }
  }

  return (
    <BaseChart
      title={title}
      loading={loading}
      data={data}
      error={error}
      height={height}
      onRefresh={onRefresh}
    >
      <Table
        columns={columns}
        dataSource={tableData}
        pagination={{
          pageSize,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total) => `共 ${total} 行`
        }}
        onChange={handleTableChange}
        size="small"
        scroll={{ y: height - 100, x: 'max-content' }}
        showHeader={true}
        rowClassName={(record) => record.key === 'summary' ? 'table-summary-row' : ''}
        summary={() => summaryData ? (
          <Table.Summary.Row>
            {Object.keys(summaryData)
              .filter(k => k !== 'key')
              .map((k, idx) => (
                <Table.Summary.Cell key={idx} index={idx}>
                  {summaryData[k]}
                </Table.Summary.Cell>
              ))}
          </Table.Summary.Row>
        ) : null}
      />
    </BaseChart>
  )
}

export default TableChart
