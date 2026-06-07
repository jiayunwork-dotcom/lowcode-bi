import React, { useState, useRef } from 'react'
import { Progress, Button, Alert, Space, Tag, message } from 'antd'
import { UploadOutlined, ReloadOutlined } from '@ant-design/icons'
import { dataSourceApi } from '@/api'
import type { FileChunkUploadResponse } from '@/types'

interface ChunkedUploaderProps {
  dataSourceId: string
  onPreview?: (file: File) => void
  onSuccess: (result: any) => void
  onError?: (error: Error) => void
}

const CHUNK_SIZE = 2 * 1024 * 1024
const LARGE_FILE_THRESHOLD = 10 * 1024 * 1024
const MAX_RETRIES = 3

const ChunkedUploader: React.FC<ChunkedUploaderProps> = ({
  dataSourceId,
  onPreview,
  onSuccess,
  onError
}) => {
  const [uploading, setUploading] = useState(false)
  const [progress, setProgress] = useState(0)
  const [currentChunk, setCurrentChunk] = useState(0)
  const [totalChunks, setTotalChunks] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [failedChunks, setFailedChunks] = useState<number[]>([])
  const fileInputRef = useRef<HTMLInputElement>(null)

  const generateFileId = (file: File): string => {
    return `${file.name}-${file.size}-${Date.now()}`.replace(/[^a-zA-Z0-9-_]/g, '_')
  }

  const uploadChunk = async (
    fileId: string,
    file: File,
    chunkNumber: number,
    total: number,
    retryCount = 0
  ): Promise<FileChunkUploadResponse> => {
    const start = (chunkNumber - 1) * CHUNK_SIZE
    const end = Math.min(start + CHUNK_SIZE, file.size)
    const blob = file.slice(start, end)

    try {
      const result = await dataSourceApi.uploadChunk(
        fileId,
        file.name,
        chunkNumber,
        total,
        CHUNK_SIZE,
        file.size,
        blob
      )
      return result
    } catch (error) {
      if (retryCount < MAX_RETRIES) {
        console.log(`分片 ${chunkNumber} 上传失败，重试第 ${retryCount + 1} 次`)
        return uploadChunk(fileId, file, chunkNumber, total, retryCount + 1)
      }
      throw error
    }
  }

  const handleFileSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return

    if (!file.name.toLowerCase().endsWith('.csv')) {
      message.error('只支持CSV文件')
      return
    }

    if (file.size <= LARGE_FILE_THRESHOLD && onPreview) {
      onPreview(file)
      if (fileInputRef.current) {
        fileInputRef.current.value = ''
      }
      return
    }

    setUploading(true)
    setProgress(0)
    setError(null)
    setFailedChunks([])

    try {
      if (file.size <= LARGE_FILE_THRESHOLD) {
        await dataSourceApi.uploadCsv(dataSourceId, file, (percent) => {
          setProgress(percent)
        })
        message.success('CSV上传成功')
        onSuccess({})
        return
      }

      const fileId = generateFileId(file)
      const total = Math.ceil(file.size / CHUNK_SIZE)
      setTotalChunks(total)

      const failed: number[] = []

      for (let i = 1; i <= total; i++) {
        setCurrentChunk(i)
        const chunkProgress = Math.round(((i - 1) / total) * 100)
        setProgress(chunkProgress)

        try {
          const result = await uploadChunk(fileId, file, i, total)
          if (!result.success) {
            failed.push(i)
            setFailedChunks([...failed])
          }
        } catch (error) {
          failed.push(i)
          setFailedChunks([...failed])
          console.error(`分片 ${i} 上传失败`, error)
        }
      }

      if (failed.length > 0) {
        setError(`有 ${failed.length} 个分片上传失败，请点击重试按钮重新上传失败的分片`)
        setProgress(Math.round(((total - failed.length) / total) * 100))
        return
      }

      setProgress(95)
      message.info('所有分片上传完成，正在合并文件...')

      const mergeResult = await dataSourceApi.mergeChunks(
        dataSourceId,
        fileId,
        file.name,
        total,
        file.size
      )

      setProgress(100)
      message.success('CSV上传成功')
      onSuccess(mergeResult)

    } catch (error) {
      console.error('上传失败:', error)
      setError('上传失败: ' + (error as Error).message)
      onError?.(error as Error)
    } finally {
      if (fileInputRef.current) {
        fileInputRef.current.value = ''
      }
    }
  }

  const handleRetryFailed = async () => {
    if (!fileInputRef.current?.files?.[0] || failedChunks.length === 0) return

    const file = fileInputRef.current.files[0]
    const fileId = generateFileId(file)
    const total = Math.ceil(file.size / CHUNK_SIZE)

    setError(null)
    const remainingFailed = [...failedChunks]

    for (const chunkNum of failedChunks) {
      setCurrentChunk(chunkNum)
      try {
        const result = await uploadChunk(fileId, file, chunkNum, total)
        if (result.success) {
          const index = remainingFailed.indexOf(chunkNum)
          if (index > -1) {
            remainingFailed.splice(index, 1)
            setFailedChunks([...remainingFailed])
          }
          setProgress(Math.round(((total - remainingFailed.length) / total) * 100))
        }
      } catch (error) {
        console.error(`分片 ${chunkNum} 重试失败`, error)
      }
    }

    if (remainingFailed.length === 0) {
      try {
        setProgress(95)
        const mergeResult = await dataSourceApi.mergeChunks(
          dataSourceId,
          fileId,
          file.name,
          total,
          file.size
        )
        setProgress(100)
        message.success('CSV上传成功')
        onSuccess(mergeResult)
        setFailedChunks([])
        setUploading(false)
      } catch (error) {
        setError('文件合并失败: ' + (error as Error).message)
      }
    } else {
      setError(`仍有 ${remainingFailed.length} 个分片上传失败`)
    }
  }

  return (
    <div>
      <input
        type="file"
        accept=".csv"
        ref={fileInputRef}
        onChange={handleFileSelect}
        style={{ display: 'none' }}
        disabled={uploading && failedChunks.length === 0}
      />

      <Button
        icon={<UploadOutlined />}
        onClick={() => fileInputRef.current?.click()}
        loading={uploading && failedChunks.length === 0}
        disabled={uploading && failedChunks.length === 0}
      >
        上传CSV
      </Button>

      {uploading && (
        <div style={{ marginTop: 16 }}>
          <Progress
            percent={progress}
            status={error ? 'exception' : 'active'}
            strokeColor={{
              '0%': '#108ee9',
              '100%': '#87d068',
            }}
          />
          
          {totalChunks > 0 && (
            <div style={{ marginTop: 8, fontSize: 12, color: '#666' }}>
              <Space>
                <Tag color="blue">分片 {currentChunk} / {totalChunks}</Tag>
                <Tag color="green">分片大小: 2MB</Tag>
              </Space>
            </div>
          )}

          {failedChunks.length > 0 && (
            <div style={{ marginTop: 8 }}>
              <Alert
                message="部分分片上传失败"
                description={`失败的分片: ${failedChunks.join(', ')}`}
                type="warning"
                showIcon
                action={
                  <Button
                    size="small"
                    type="primary"
                    icon={<ReloadOutlined />}
                    onClick={handleRetryFailed}
                  >
                    重试失败分片
                  </Button>
                }
              />
            </div>
          )}

          {error && !failedChunks.length && (
            <Alert
              message="上传失败"
              description={error}
              type="error"
              showIcon
              style={{ marginTop: 8 }}
            />
          )}
        </div>
      )}
    </div>
  )
}

export default ChunkedUploader
