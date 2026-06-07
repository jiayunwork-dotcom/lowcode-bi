import React, { useState, useRef, useEffect } from 'react'
import { useSortable } from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import { Button, Space, Dropdown, MenuProps } from 'antd'
import {
  DeleteOutlined,
  SettingOutlined,
  MoreOutlined,
  FullscreenOutlined,
  CopyOutlined
} from '@ant-design/icons'
import type { DashboardComponent } from '@/types'

interface SortableComponentProps {
  id: string
  component: DashboardComponent
  isSelected: boolean
  children: React.ReactNode
  onSelect: () => void
  onDelete: () => void
  onResize: (width: number, height: number) => void
}

const SortableComponent: React.FC<SortableComponentProps> = ({
  id,
  component,
  isSelected,
  children,
  onSelect,
  onDelete,
  onResize
}) => {
  const [isResizing, setIsResizing] = useState(false)
  const resizeStartRef = useRef<{ x: number; y: number; width: number; height: number } | null>(null)

  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging
  } = useSortable({ id })

  const style: React.CSSProperties = {
    position: 'absolute',
    left: component.x,
    top: component.y,
    width: component.width,
    height: component.height,
    transform: CSS.Transform.toString(transform),
    transition,
    zIndex: isSelected ? 10 : 1,
    opacity: isDragging ? 0.5 : 1,
    border: isSelected ? '2px solid #1890ff' : '1px solid #d9d9d9',
    borderRadius: 8,
    backgroundColor: '#fff',
    boxShadow: isSelected ? '0 4px 12px rgba(0,0,0,0.15)' : '0 1px 2px rgba(0,0,0,0.05)',
    cursor: isDragging ? 'grabbing' : 'grab',
    overflow: 'hidden'
  }

  const handleResizeStart = (e: React.MouseEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setIsResizing(true)
    resizeStartRef.current = {
      x: e.clientX,
      y: e.clientY,
      width: component.width,
      height: component.height
    }
  }

  useEffect(() => {
    if (!isResizing) return

    const handleMouseMove = (e: MouseEvent) => {
      if (!resizeStartRef.current) return

      const deltaX = e.clientX - resizeStartRef.current.x
      const deltaY = e.clientY - resizeStartRef.current.y

      const newWidth = Math.max(100, resizeStartRef.current.width + deltaX)
      const newHeight = Math.max(80, resizeStartRef.current.height + deltaY)

      onResize(newWidth, newHeight)
    }

    const handleMouseUp = () => {
      setIsResizing(false)
      resizeStartRef.current = null
    }

    document.addEventListener('mousemove', handleMouseMove)
    document.addEventListener('mouseup', handleMouseUp)

    return () => {
      document.removeEventListener('mousemove', handleMouseMove)
      document.removeEventListener('mouseup', handleMouseUp)
    }
  }, [isResizing, onResize])

  const menuItems: MenuProps['items'] = [
    {
      key: 'copy',
      label: '复制',
      icon: <CopyOutlined />
    },
    {
      key: 'fullscreen',
      label: '全屏',
      icon: <FullscreenOutlined />
    },
    {
      type: 'divider'
    },
    {
      key: 'delete',
      label: '删除',
      icon: <DeleteOutlined />,
      danger: true,
      onClick: onDelete
    }
  ]

  return (
    <div
      ref={setNodeRef}
      style={style}
      onClick={(e) => {
        e.stopPropagation()
        onSelect()
      }}
    >
      <div
        style={{
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          height: 28,
          padding: '0 8px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          backgroundColor: isSelected ? '#e6f7ff' : '#fafafa',
          borderBottom: '1px solid #f0f0f0',
          zIndex: 2,
          ...attributes,
          ...listeners
        }}
      >
        <span style={{ fontSize: 12, fontWeight: 500, color: '#595959' }}>
          {component.title}
        </span>
        <Space size={2}>
          <Dropdown menu={{ items: menuItems }} trigger={['click']}>
            <Button
              type="text"
              size="small"
              icon={<MoreOutlined />}
              onClick={(e) => e.stopPropagation()}
              style={{ padding: '0 4px', height: 20 }}
            />
          </Dropdown>
        </Space>
      </div>

      <div
        style={{
          position: 'absolute',
          top: 28,
          left: 0,
          right: 0,
          bottom: 0,
          padding: 4,
          overflow: 'hidden'
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {children}
      </div>

      {isSelected && (
        <>
          <div
            style={{
              position: 'absolute',
              right: -4,
              bottom: -4,
              width: 12,
              height: 12,
              backgroundColor: '#1890ff',
              border: '2px solid #fff',
              borderRadius: '0 0 8px 0',
              cursor: 'se-resize',
              zIndex: 11
            }}
            onMouseDown={handleResizeStart}
          />
          <div
            style={{
              position: 'absolute',
              right: -4,
              top: '50%',
              transform: 'translateY(-50%)',
              width: 8,
              height: 24,
              backgroundColor: '#1890ff',
              border: '2px solid #fff',
              borderRadius: 4,
              cursor: 'e-resize',
              zIndex: 11
            }}
            onMouseDown={handleResizeStart}
          />
          <div
            style={{
              position: 'absolute',
              bottom: -4,
              left: '50%',
              transform: 'translateX(-50%)',
              width: 24,
              height: 8,
              backgroundColor: '#1890ff',
              border: '2px solid #fff',
              borderRadius: 4,
              cursor: 's-resize',
              zIndex: 11
            }}
            onMouseDown={handleResizeStart}
          />
        </>
      )}
    </div>
  )
}

export default SortableComponent
