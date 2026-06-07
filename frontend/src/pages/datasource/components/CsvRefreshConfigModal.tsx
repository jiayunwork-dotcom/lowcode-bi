import React, { useState, useEffect } from 'react'
import { Modal, Form, Select, Input, Button, message, Space, Tag } from 'antd'
import type { DataSource } from '@/types'
import { dataSourceApi } from '@/api'

interface CsvRefreshConfigModalProps {
  visible: boolean
  onCancel: () => void
  dataSource: DataSource | null
  onConfigSaved?: () => void
  onSuccess?: () => void
}

const REFRESH_INTERVAL_OPTIONS = [
  { value: 'MANUAL', label: '手动刷新', description: '仅在用户手动触发时刷新' },
  { value: 'ONE_HOUR', label: '每小时', description: '每小时自动扫描并刷新' },
  { value: 'ONE_DAY', label: '每天', description: '每天自动扫描并刷新' },
  { value: 'OFF', label: '关闭', description: '禁用自动刷新' },
]

const CsvRefreshConfigModal: React.FC<CsvRefreshConfigModalProps> = ({
  visible,
  onCancel,
  dataSource,
  onConfigSaved,
  onSuccess
}) => {
  const [form] = Form.useForm()
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (visible && dataSource) {
      form.setFieldsValue({
        refreshInterval: dataSource.csvRefreshInterval || 'MANUAL',
        refreshDirectory: dataSource.csvRefreshDirectory || ''
      })
    }
  }, [visible, dataSource, form])

  const handleSubmit = async () => {
    if (!dataSource) return

    try {
      const values = await form.validateFields()
      setSaving(true)

      await dataSourceApi.configureRefresh(
        dataSource.id,
        values.refreshInterval,
        values.refreshDirectory
      )

      message.success('刷新配置保存成功')
      onConfigSaved?.()
      onSuccess?.()
      onCancel()
    } catch (error) {
      console.error('Failed to save refresh config:', error)
    } finally {
      setSaving(false)
    }
  }

  const handleRefreshNow = async () => {
    if (!dataSource) return

    try {
      await dataSourceApi.refreshNow(dataSource.id)
      message.success('刷新任务已启动，请稍后查看刷新状态')
    } catch (error) {
      console.error('Failed to trigger refresh:', error)
    }
  }

  return (
    <Modal
      title="CSV自动刷新配置"
      open={visible}
      onCancel={onCancel}
      footer={[
        <Button key="cancel" onClick={onCancel}>
          取消
        </Button>,
        <Space key="actions">
          {dataSource?.csvRefreshInterval && dataSource.csvRefreshInterval !== 'OFF' && (
            <Button onClick={handleRefreshNow}>
              立即刷新
            </Button>
          )}
          <Button key="save" type="primary" onClick={handleSubmit} loading={saving}>
            保存配置
          </Button>
        </Space>
      ]}
    >
      {dataSource && (
        <div>
          <div style={{ marginBottom: 16, padding: 12, background: '#f5f5f5', borderRadius: 4 }}>
            <p style={{ margin: 0 }}>数据源: <b>{dataSource.name}</b></p>
            <p style={{ margin: '4px 0 0 0', color: '#666' }}>
              CSV文件: {dataSource.csvFileName || '未上传'}
            </p>
            {dataSource.csvLastRefreshStatus && (
              <p style={{ margin: '4px 0 0 0' }}>
                上次刷新: 
                <Tag color={
                  dataSource.csvLastRefreshStatus === 'SUCCESS' ? 'green' :
                  dataSource.csvLastRefreshStatus === 'REFRESHING' ? 'orange' : 'red'
                }>
                  {dataSource.csvLastRefreshStatus === 'SUCCESS' ? '成功' :
                   dataSource.csvLastRefreshStatus === 'REFRESHING' ? '刷新中' : '失败'}
                </Tag>
                {dataSource.csvLastImportTime && (
                  <span style={{ marginLeft: 8, color: '#666' }}>
                    {dataSource.csvLastImportTime}
                  </span>
                )}
              </p>
            )}
            {dataSource.csvLastRefreshError && (
              <p style={{ margin: '4px 0 0 0', color: '#ff4d4f' }}>
                错误: {dataSource.csvLastRefreshError}
              </p>
            )}
          </div>

          <Form form={form} layout="vertical">
            <Form.Item
              name="refreshInterval"
              label="刷新频率"
              rules={[{ required: true, message: '请选择刷新频率' }]}
            >
              <Select
                options={REFRESH_INTERVAL_OPTIONS.map(opt => ({
                  value: opt.value,
                  label: (
                    <div>
                      <div>{opt.label}</div>
                      <div style={{ fontSize: 12, color: '#999' }}>{opt.description}</div>
                    </div>
                  )
                }))}
              />
            </Form.Item>

            <Form.Item
              name="refreshDirectory"
              label="本地目录路径"
              rules={[{ required: true, message: '请输入本地目录路径' }]}
              extra="系统将扫描该目录下与上传文件名相同的CSV文件，如果文件修改时间较新则自动刷新"
            >
              <Input placeholder="/path/to/csv/directory" />
            </Form.Item>
          </Form>
        </div>
      )}
    </Modal>
  )
}

export default CsvRefreshConfigModal
