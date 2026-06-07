import React, { useState, useEffect } from 'react'
import {
  Form,
  Select,
  Input,
  DatePicker,
  InputNumber,
  Button,
  Space,
  Row,
  Col,
  Card,
  Tag,
  Divider,
  Empty,
  Tooltip
} from 'antd'
import { PlusOutlined, DeleteOutlined, CloseOutlined } from '@ant-design/icons'
import type { ComponentFilter } from '@/types'
import { dataModelApi } from '@/api'

const { Option } = Select
const { RangePicker } = DatePicker

interface FilterPanelProps {
  filters: ComponentFilter[]
  onChange: (filters: ComponentFilter[]) => void
  onClose?: () => void
  dataModelId?: string
  isGlobal?: boolean
}

const FilterPanel: React.FC<FilterPanelProps> = ({
  filters,
  onChange,
  onClose,
  dataModelId,
  isGlobal = false
}) => {
  const [availableFields, setAvailableFields] = useState<{
    name: string
    displayName: string
    type: string
  }[]>([])

  useEffect(() => {
    if (dataModelId) {
      loadModelFields(dataModelId)
    }
  }, [dataModelId])

  const loadModelFields = async (modelId: string) => {
    try {
      const fields = await dataModelApi.getFields(modelId)
      const allFields = [
        ...fields.dimensions.map(d => ({ name: d.name, displayName: d.displayName, type: d.type })),
        ...fields.measures.map(m => ({ name: m.name, displayName: m.displayName, type: m.type })),
        ...fields.calculatedFields.map(c => ({ name: c.name, displayName: c.displayName, type: c.type }))
      ]
      setAvailableFields(allFields)
    } catch (error) {
      console.error('Failed to load model fields:', error)
    }
  }

  const addFilter = () => {
    const newFilter: ComponentFilter = {
      id: `filter_${Date.now()}`,
      field: availableFields[0]?.name || '',
      filterType: 'EQUAL',
      isGlobal
    }
    onChange([...filters, newFilter])
  }

  const updateFilter = (index: number, updates: Partial<ComponentFilter>) => {
    const updatedFilters = [...filters]
    updatedFilters[index] = { ...updatedFilters[index], ...updates }
    onChange(updatedFilters)
  }

  const removeFilter = (index: number) => {
    const updatedFilters = filters.filter((_, i) => i !== index)
    onChange(updatedFilters)
  }

  const getFilterTypeOptions = () => [
    { value: 'EQUAL', label: '等于', needValue: true },
    { value: 'NOT_EQUAL', label: '不等于', needValue: true },
    { value: 'GREATER_THAN', label: '大于', needValue: true },
    { value: 'LESS_THAN', label: '小于', needValue: true },
    { value: 'GREATER_EQUAL', label: '大于等于', needValue: true },
    { value: 'LESS_EQUAL', label: '小于等于', needValue: true },
    { value: 'BETWEEN', label: '介于', needValue: true, needValue2: true },
    { value: 'IN', label: '包含', needValue: true, isMulti: true },
    { value: 'NOT_IN', label: '不包含', needValue: true, isMulti: true },
    { value: 'LIKE', label: '模糊匹配', needValue: true },
    { value: 'IS_NULL', label: '为空', needValue: false },
    { value: 'IS_NOT_NULL', label: '不为空', needValue: false }
  ]

  const renderFilterValue = (filter: ComponentFilter, index: number) => {
    const field = availableFields.find(f => f.name === filter.field)
    const filterType = getFilterTypeOptions().find(opt => opt.value === filter.filterType)

    if (!filterType?.needValue) return null

    const isDateType = field?.type?.toUpperCase().includes('DATE') || field?.type?.toUpperCase().includes('TIME')
    const isNumericType = field?.type?.toUpperCase().includes('INT') || field?.type?.toUpperCase().includes('DECIMAL') || field?.type?.toUpperCase().includes('NUMBER')

    if (filterType.needValue && filterType.needValue2) {
      if (isDateType) {
        return (
          <RangePicker
            style={{ width: '100%' }}
            value={filter.value1 && filter.value2 ? [filter.value1 as any, filter.value2 as any] : undefined}
            onChange={(dates) => {
              updateFilter(index, {
                value1: dates?.[0]?.toISOString(),
                value2: dates?.[1]?.toISOString()
              })
            }}
          />
        )
      }
      return (
        <Space>
          <InputNumber
            placeholder="最小值"
            value={filter.value1 as number}
            onChange={(value) => updateFilter(index, { value1: value as number })}
          />
          <span>~</span>
          <InputNumber
            placeholder="最大值"
            value={filter.value2 as number}
            onChange={(value) => updateFilter(index, { value2: value as number })}
          />
        </Space>
      )
    }

    if (filterType.isMulti) {
      return (
        <Select
          mode="multiple"
          placeholder="请选择值"
          style={{ width: '100%' }}
          value={filter.values}
          onChange={(values) => updateFilter(index, { values })}
        />
      )
    }

    if (isDateType) {
      return (
        <DatePicker
          style={{ width: '100%' }}
          value={filter.value1 ? (filter.value1 as any) : undefined}
          onChange={(date) => updateFilter(index, { value1: date?.toISOString() })}
        />
      )
    }

    if (isNumericType) {
      return (
        <InputNumber
          style={{ width: '100%' }}
          placeholder="请输入值"
          value={filter.value1 as number}
          onChange={(value) => updateFilter(index, { value1: value as number })}
        />
      )
    }

    return (
      <Input
        placeholder="请输入值"
        value={filter.value1 as string}
        onChange={(e) => updateFilter(index, { value1: e.target.value })}
      />
    )
  }

  return (
    <Card
      size="small"
      className="filter-panel"
      title={
        <Space>
          <Tag color={isGlobal ? 'red' : 'blue'}>{isGlobal ? '全局筛选' : '组件筛选'}</Tag>
          <span>共 {filters.length} 个筛选条件</span>
        </Space>
      }
      extra={onClose && (
        <Button
          type="text"
          icon={<CloseOutlined />}
          onClick={onClose}
        />
      )}
    >
      {filters.length === 0 ? (
        <Empty
          description="暂无筛选条件"
          image={null}
          style={{ padding: '16px 0' }}
        />
      ) : (
        <div className="filter-list">
          {filters.map((filter, index) => (
            <div key={filter.id} className="filter-item">
              <Row gutter={8} align="middle">
                <Col span={5}>
                  <Select
                    size="small"
                    value={filter.field}
                    onChange={(value) => updateFilter(index, { field: value })}
                    style={{ width: '100%' }}
                  >
                    {availableFields.map(field => (
                      <Option key={field.name} value={field.name}>{field.displayName}</Option>
                    ))}
                  </Select>
                </Col>
                <Col span={4}>
                  <Select
                    size="small"
                    value={filter.filterType}
                    onChange={(value) => updateFilter(index, { filterType: value })}
                    style={{ width: '100%' }}
                  >
                    {getFilterTypeOptions().map(opt => (
                      <Option key={opt.value} value={opt.value}>{opt.label}</Option>
                    ))}
                  </Select>
                </Col>
                <Col flex="auto">
                  {renderFilterValue(filter, index)}
                </Col>
                <Col span={2}>
                  <Tooltip title="删除">
                    <Button
                      type="text"
                      danger
                      size="small"
                      icon={<DeleteOutlined />}
                      onClick={() => removeFilter(index)}
                    />
                  </Tooltip>
                </Col>
              </Row>
            </div>
          ))}
        </div>
      )}

      <Divider style={{ margin: '12px 0' }} />

      <Button
        type="dashed"
        icon={<PlusOutlined />}
        onClick={addFilter}
        block
        disabled={availableFields.length === 0}
      >
        添加筛选条件
      </Button>
    </Card>
  )
}

export default FilterPanel
