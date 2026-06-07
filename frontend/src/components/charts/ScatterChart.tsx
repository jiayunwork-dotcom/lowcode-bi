import React, { useEffect, useRef } from 'react'
import * as echarts from 'echarts'
import BaseChart from './BaseChart'
import type { QueryResult, ComponentConfig } from '@/types'

interface ScatterChartProps {
  title: string
  data?: QueryResult
  config: ComponentConfig
  loading?: boolean
  error?: string
  height?: number
  onRefresh?: () => void
  onClick?: (params: any) => void
}

const ScatterChart: React.FC<ScatterChartProps> = ({
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
    
    const isBubble = chartSubType === 'bubble'
    const seriesData: any[] = []

    if (isBubble) {
      data.rows.forEach(row => {
        seriesData.push({
          name: row[0],
          value: [row[1], row[2], row[3] || 10]
        })
      })
    } else {
      data.rows.forEach(row => {
        seriesData.push({
          name: row[0],
          value: [row[1], row[2]]
        })
      })
    }

    const option: echarts.EChartsOption = {
      tooltip: {
        trigger: 'item',
        show: showTooltip,
        formatter: (params: any) => {
          const data = params.data
          return `${data.name}<br/>X: ${data.value[0]}<br/>Y: ${data.value[1]}${isBubble ? `<br/>大小: ${data.value[2]}` : ''}`
        }
      },
      legend: {
        show: showLegend,
        data: [config.measures?.[0] || '数据']
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'value',
        name: config.dimensions?.[0] || 'X轴'
      },
      yAxis: {
        type: 'value',
        name: config.dimensions?.[1] || 'Y轴'
      },
      series: [
        {
          name: config.measures?.[0] || '数据',
          type: 'scatter',
          data: seriesData,
          symbolSize: isBubble ? (data: any) => Math.sqrt(data[2]) * 5 : 10,
          emphasis: {
            focus: 'series'
          },
          itemStyle: {
            shadowBlur: 10,
            shadowColor: 'rgba(120, 36, 50, 0.5)',
            shadowOffsetY: 5,
            color: new echarts.graphic.RadialGradient(0.4, 0.3, 1, [
              { offset: 0, color: 'rgb(129, 140, 248)' },
              { offset: 1, color: 'rgb(99, 102, 241)' }
            ])
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

export default ScatterChart
