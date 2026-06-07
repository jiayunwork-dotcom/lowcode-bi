import React, { useEffect, useRef } from 'react'
import * as echarts from 'echarts'
import BaseChart from './BaseChart'
import type { QueryResult, ComponentConfig } from '@/types'

interface LineChartProps {
  title: string
  data?: QueryResult
  config: ComponentConfig
  loading?: boolean
  error?: string
  height?: number
  onRefresh?: () => void
  onClick?: (params: any) => void
}

const LineChart: React.FC<LineChartProps> = ({
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

    const { dimensions = [], measures = [], chartSubType, showLegend = true, showTooltip = true, stacked = false, areaStyle = false } = config
    
    const xAxisData = data.rows.map(row => row[0])
    const series = measures.map((measure, idx) => {
      const seriesData = data.rows.map(row => row[idx + 1])
      return {
        name: measure,
        type: 'line',
        data: seriesData,
        smooth: chartSubType === 'smooth',
        stack: stacked ? 'total' : undefined,
        areaStyle: areaStyle ? {} : undefined,
        emphasis: {
          focus: 'series'
        }
      }
    })

    const option: echarts.EChartsOption = {
      tooltip: {
        trigger: 'axis',
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
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: xAxisData,
        axisLabel: {
          rotate: xAxisData.length > 10 ? 30 : 0
        }
      },
      yAxis: {
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

export default LineChart
