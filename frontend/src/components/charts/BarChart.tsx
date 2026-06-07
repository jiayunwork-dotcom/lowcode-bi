import React, { useEffect, useRef } from 'react'
import * as echarts from 'echarts'
import BaseChart from './BaseChart'
import type { QueryResult, ComponentConfig } from '@/types'

interface BarChartProps {
  title: string
  data?: QueryResult
  config: ComponentConfig
  loading?: boolean
  error?: string
  height?: number
  onRefresh?: () => void
  onClick?: (params: any) => void
}

const BarChart: React.FC<BarChartProps> = ({
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

    const { dimensions = [], measures = [], chartSubType, showLegend = true, showTooltip = true, stacked = false } = config
    
    const xAxisData = data.rows.map(row => row[0])
    const isHorizontal = chartSubType === 'horizontal'

    const series = measures.map((measure, idx) => {
      const seriesData = data.rows.map(row => row[idx + 1])
      return {
        name: measure,
        type: 'bar',
        data: seriesData,
        stack: stacked ? 'total' : undefined,
        emphasis: {
          focus: 'series'
        },
        label: {
          show: true,
          position: stacked ? 'inside' : 'top'
        }
      }
    })

    const option: echarts.EChartsOption = {
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow'
        },
        show: showTooltip
      },
      legend: {
        show: showLegend,
        data: measures
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: isHorizontal ? {
        type: 'value'
      } : {
        type: 'category',
        data: xAxisData,
        axisLabel: {
          rotate: xAxisData.length > 10 ? 30 : 0
        }
      },
      yAxis: isHorizontal ? {
        type: 'category',
        data: xAxisData
      } : {
        type: 'value'
      },
      series: series as any
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

export default BarChart
