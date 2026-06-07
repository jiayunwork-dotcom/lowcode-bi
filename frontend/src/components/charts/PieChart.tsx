import React, { useEffect, useRef } from 'react'
import * as echarts from 'echarts'
import BaseChart from './BaseChart'
import type { QueryResult, ComponentConfig } from '@/types'

interface PieChartProps {
  title: string
  data?: QueryResult
  config: ComponentConfig
  loading?: boolean
  error?: string
  height?: number
  onRefresh?: () => void
  onClick?: (params: any) => void
}

const PieChart: React.FC<PieChartProps> = ({
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
    
    const pieData = data.rows.map(row => ({
      name: row[0],
      value: row[1]
    }))

    const isDoughnut = chartSubType === 'doughnut'
    const isRose = chartSubType === 'rose'

    const option: echarts.EChartsOption = {
      tooltip: {
        trigger: 'item',
        show: showTooltip,
        formatter: '{b}: {c} ({d}%)'
      },
      legend: {
        show: showLegend,
        orient: 'vertical',
        left: 'left'
      },
      series: [
        {
          name: config.measures?.[0] || '数据',
          type: 'pie',
          radius: isDoughnut ? ['40%', '70%'] : isRose ? ['20%', '70%'] : '70%',
          center: ['50%', '50%'],
          roseType: isRose ? 'radius' : undefined,
          data: pieData,
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          },
          label: {
            show: true,
            formatter: '{b}: {d}%'
          }
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

export default PieChart
