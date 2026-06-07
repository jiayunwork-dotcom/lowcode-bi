import React, { useEffect, useRef } from 'react'
import * as echarts from 'echarts'
import BaseChart from './BaseChart'
import type { QueryResult, ComponentConfig } from '@/types'

interface FunnelChartProps {
  title: string
  data?: QueryResult
  config: ComponentConfig
  loading?: boolean
  error?: string
  height?: number
  onRefresh?: () => void
  onClick?: (params: any) => void
}

const FunnelChart: React.FC<FunnelChartProps> = ({
  title,
  data,
  config,
  loading = false,
  error,
  height = 300,
  onRefresh,
  onClick
}) => {
  const chartRef = useRef<HTMLDivElement>(null)
  const chartInstance = useRef<echarts.ECharts | null>(null)

  useEffect(() => {
    if (chartRef.current) {
      chartInstance.current = echarts.init(chartRef.current)
      
      const handleResize = () => {
        chartInstance.current?.resize()
      }
      window.addEventListener('resize', handleResize)

      return () => {
        window.removeEventListener('resize', handleResize)
        chartInstance.current?.dispose()
      }
    }
  }, [])

  useEffect(() => {
    if (!chartInstance.current || !data || data.rows.length === 0) return

    const { chartSubType, showLegend = true, showTooltip = true } = config
    
    const funnelData = data.rows.map(row => ({
      name: row[0],
      value: row[1]
    }))

    const option: echarts.EChartsOption = {
      tooltip: {
        trigger: 'item',
        show: showTooltip,
        formatter: '{b}: {c}'
      },
      legend: {
        show: showLegend,
        data: funnelData.map(d => d.name)
      },
      series: [
        {
          name: config.measures?.[0] || '数据',
          type: 'funnel',
          left: '10%',
          top: 60,
          bottom: 60,
          width: '80%',
          min: 0,
          max: Math.max(...funnelData.map(d => d.value as number)),
          minSize: '0%',
          maxSize: '100%',
          sort: chartSubType === 'reverse' ? 'ascending' : 'descending',
          gap: 2,
          label: {
            show: true,
            position: 'inside',
            formatter: '{b}: {c}'
          },
          labelLine: {
            length: 10,
            lineStyle: {
              width: 1,
              type: 'solid'
            }
          },
          itemStyle: {
            borderColor: '#fff',
            borderWidth: 1
          },
          emphasis: {
            label: {
              fontSize: 16
            }
          },
          data: funnelData
        }
      ]
    }

    chartInstance.current.setOption(option, true)

    if (onClick) {
      chartInstance.current.off('click')
      chartInstance.current.on('click', onClick)
    }
  }, [data, config, onClick])

  return (
    <BaseChart
      title={title}
      loading={loading}
      data={data}
      error={error}
      height={height}
      onRefresh={onRefresh}
    >
      <div ref={chartRef} style={{ width: '100%', height: '100%' }} />
    </BaseChart>
  )
}

export default FunnelChart
