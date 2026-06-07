import React, { useEffect, useRef, useState } from 'react'
import * as echarts from 'echarts'
import BaseChart from './BaseChart'
import type { QueryResult, ComponentConfig } from '@/types'

interface MapChartProps {
  title: string
  data?: QueryResult
  config: ComponentConfig
  loading?: boolean
  error?: string
  height?: number
  onRefresh?: () => void
  onClick?: (params: any) => void
}

const MapChart: React.FC<MapChartProps> = ({
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
  const [mapLoaded, setMapLoaded] = useState(false)

  const { mapConfig = {} } = config
  const { mapType = 'CHINA_PROVINCE' } = mapConfig

  useEffect(() => {
    const loadMapData = async () => {
      try {
        const chinaGeoJson = {
          type: 'FeatureCollection',
          features: [
            { type: 'Feature', properties: { name: '北京', adcode: 110000 }, geometry: { type: 'Polygon', coordinates: [[[115.5, 39.5], [117.5, 39.5], [117.5, 41], [115.5, 41], [115.5, 39.5]]] } },
            { type: 'Feature', properties: { name: '天津', adcode: 120000 }, geometry: { type: 'Polygon', coordinates: [[[116.7, 38.7], [118, 38.7], [118, 40.2], [116.7, 40.2], [116.7, 38.7]]] } },
            { type: 'Feature', properties: { name: '河北', adcode: 130000 }, geometry: { type: 'Polygon', coordinates: [[[113.5, 36], [119.5, 36], [119.5, 42.5], [113.5, 42.5], [113.5, 36]]] } },
            { type: 'Feature', properties: { name: '山西', adcode: 140000 }, geometry: { type: 'Polygon', coordinates: [[[110.3, 34.5], [114.5, 34.5], [114.5, 40.5], [110.3, 40.5], [110.3, 34.5]]] } },
            { type: 'Feature', properties: { name: '内蒙古', adcode: 150000 }, geometry: { type: 'Polygon', coordinates: [[[97, 37.5], [126, 37.5], [126, 53.3], [97, 53.3], [97, 37.5]]] } },
            { type: 'Feature', properties: { name: '辽宁', adcode: 210000 }, geometry: { type: 'Polygon', coordinates: [[[119, 38.5], [125.5, 38.5], [125.5, 43.5], [119, 43.5], [119, 38.5]]] } },
            { type: 'Feature', properties: { name: '吉林', adcode: 220000 }, geometry: { type: 'Polygon', coordinates: [[[121.5, 40.8], [131.3, 40.8], [131.3, 46.3], [121.5, 46.3], [121.5, 40.8]]] } },
            { type: 'Feature', properties: { name: '黑龙江', adcode: 230000 }, geometry: { type: 'Polygon', coordinates: [[[121.5, 43.5], [135, 43.5], [135, 53.5], [121.5, 53.5], [121.5, 43.5]]] } },
            { type: 'Feature', properties: { name: '上海', adcode: 310000 }, geometry: { type: 'Polygon', coordinates: [[[121, 30.7], [122, 30.7], [122, 31.8], [121, 31.8], [121, 30.7]]] } },
            { type: 'Feature', properties: { name: '江苏', adcode: 320000 }, geometry: { type: 'Polygon', coordinates: [[[116.4, 30.8], [121.9, 30.8], [121.9, 35.1], [116.4, 35.1], [116.4, 30.8]]] } },
            { type: 'Feature', properties: { name: '浙江', adcode: 330000 }, geometry: { type: 'Polygon', coordinates: [[[118, 27], [122.5, 27], [122.5, 31.2], [118, 31.2], [118, 27]]] } },
            { type: 'Feature', properties: { name: '安徽', adcode: 340000 }, geometry: { type: 'Polygon', coordinates: [[[114.9, 29.4], [119.6, 29.4], [119.6, 34.6], [114.9, 34.6], [114.9, 29.4]]] } },
            { type: 'Feature', properties: { name: '福建', adcode: 350000 }, geometry: { type: 'Polygon', coordinates: [[[116, 23.5], [120.5, 23.5], [120.5, 28.3], [116, 28.3], [116, 23.5]]] } },
            { type: 'Feature', properties: { name: '江西', adcode: 360000 }, geometry: { type: 'Polygon', coordinates: [[[113.5, 24.5], [118.5, 24.5], [118.5, 30], [113.5, 30], [113.5, 24.5]]] } },
            { type: 'Feature', properties: { name: '山东', adcode: 370000 }, geometry: { type: 'Polygon', coordinates: [[[114.8, 34.2], [122.7, 34.2], [122.7, 38.4], [114.8, 38.4], [114.8, 34.2]]] } },
            { type: 'Feature', properties: { name: '河南', adcode: 410000 }, geometry: { type: 'Polygon', coordinates: [[[110.4, 31.4], [116.6, 31.4], [116.6, 36.4], [110.4, 36.4], [110.4, 31.4]]] } },
            { type: 'Feature', properties: { name: '湖北', adcode: 420000 }, geometry: { type: 'Polygon', coordinates: [[[108.4, 29], [116.1, 29], [116.1, 33.3], [108.4, 33.3], [108.4, 29]]] } },
            { type: 'Feature', properties: { name: '湖南', adcode: 430000 }, geometry: { type: 'Polygon', coordinates: [[[108.8, 24.6], [114.3, 24.6], [114.3, 30.1], [108.8, 30.1], [108.8, 24.6]]] } },
            { type: 'Feature', properties: { name: '广东', adcode: 440000 }, geometry: { type: 'Polygon', coordinates: [[[109.7, 20.2], [117.2, 20.2], [117.2, 25.5], [109.7, 25.5], [109.7, 20.2]]] } },
            { type: 'Feature', properties: { name: '广西', adcode: 450000 }, geometry: { type: 'Polygon', coordinates: [[[104.3, 20.5], [112.1, 20.5], [112.1, 26.4], [104.3, 26.4], [104.3, 20.5]]] } },
            { type: 'Feature', properties: { name: '海南', adcode: 460000 }, geometry: { type: 'Polygon', coordinates: [[[108.6, 18], [111.1, 18], [111.1, 20.2], [108.6, 20.2], [108.6, 18]]] } },
            { type: 'Feature', properties: { name: '重庆', adcode: 500000 }, geometry: { type: 'Polygon', coordinates: [[[105.3, 28.2], [110.2, 28.2], [110.2, 32.2], [105.3, 32.2], [105.3, 28.2]]] } },
            { type: 'Feature', properties: { name: '四川', adcode: 510000 }, geometry: { type: 'Polygon', coordinates: [[[97.4, 26], [108.5, 26], [108.5, 34.3], [97.4, 34.3], [97.4, 26]]] } },
            { type: 'Feature', properties: { name: '贵州', adcode: 520000 }, geometry: { type: 'Polygon', coordinates: [[[103.6, 24.6], [109.6, 24.6], [109.6, 29.2], [103.6, 29.2], [103.6, 24.6]]] } },
            { type: 'Feature', properties: { name: '云南', adcode: 530000 }, geometry: { type: 'Polygon', coordinates: [[[97.5, 21.1], [106.2, 21.1], [106.2, 29.2], [97.5, 29.2], [97.5, 21.1]]] } },
            { type: 'Feature', properties: { name: '西藏', adcode: 540000 }, geometry: { type: 'Polygon', coordinates: [[[78.4, 26.9], [99.1, 26.9], [99.1, 36.5], [78.4, 36.5], [78.4, 26.9]]] } },
            { type: 'Feature', properties: { name: '陕西', adcode: 610000 }, geometry: { type: 'Polygon', coordinates: [[[105.3, 31.7], [111.2, 31.7], [111.2, 39.6], [105.3, 39.6], [105.3, 31.7]]] } },
            { type: 'Feature', properties: { name: '甘肃', adcode: 620000 }, geometry: { type: 'Polygon', coordinates: [[[92.3, 32.6], [108.7, 32.6], [108.7, 42.8], [92.3, 42.8], [92.3, 32.6]]] } },
            { type: 'Feature', properties: { name: '青海', adcode: 630000 }, geometry: { type: 'Polygon', coordinates: [[[89.4, 31.6], [103.1, 31.6], [103.1, 39.2], [89.4, 39.2], [89.4, 31.6]]] } },
            { type: 'Feature', properties: { name: '宁夏', adcode: 640000 }, geometry: { type: 'Polygon', coordinates: [[[104.3, 35.2], [107.7, 35.2], [107.7, 39.4], [104.3, 39.4], [104.3, 35.2]]] } },
            { type: 'Feature', properties: { name: '新疆', adcode: 650000 }, geometry: { type: 'Polygon', coordinates: [[[73.5, 34.3], [96.4, 34.3], [96.4, 49.2], [73.5, 49.2], [73.5, 34.3]]] } },
            { type: 'Feature', properties: { name: '台湾', adcode: 710000 }, geometry: { type: 'Polygon', coordinates: [[[120, 21.9], [122, 21.9], [122, 25.3], [120, 25.3], [120, 21.9]]] } },
            { type: 'Feature', properties: { name: '香港', adcode: 810000 }, geometry: { type: 'Polygon', coordinates: [[[113.8, 22.1], [114.4, 22.1], [114.4, 22.6], [113.8, 22.6], [113.8, 22.1]]] } },
            { type: 'Feature', properties: { name: '澳门', adcode: 820000 }, geometry: { type: 'Polygon', coordinates: [[[113.5, 22.1], [113.6, 22.1], [113.6, 22.25], [113.5, 22.25], [113.5, 22.1]]] } }
          ]
        }
        echarts.registerMap('china', chinaGeoJson as any)
        setMapLoaded(true)
      } catch (error) {
        console.error('Failed to load map data:', error)
        setMapLoaded(true)
      }
    }
    loadMapData()
  }, [])

  useEffect(() => {
    if (!chartInstance.current || !mapLoaded) return
    
    if (chartRef.current && !chartInstance.current) {
      chartInstance.current = echarts.init(chartRef.current)
      
      const handleResize = () => {
        chartInstance.current?.resize()
      }
      window.addEventListener('resize', handleResize)
    }

    if (!data || data.rows.length === 0) return

    const { showLegend = true, showTooltip = true } = config
    
    const mapData = data.rows.map(row => ({
      name: row[0],
      value: row[1]
    }))

    const values = mapData.map(d => d.value as number)
    const minValue = Math.min(...values)
    const maxValue = Math.max(...values)

    const option: echarts.EChartsOption = {
      tooltip: {
        trigger: 'item',
        show: showTooltip,
        formatter: '{b}: {c}'
      },
      legend: {
        show: showLegend,
        orient: 'vertical',
        left: 'left',
        data: [config.measures?.[0] || '数据']
      },
      visualMap: {
        min: minValue,
        max: maxValue,
        left: 'left',
        top: 'bottom',
        text: ['高', '低'],
        calculable: true,
        inRange: {
          color: ['#e0f3f8', '#abd9e9', '#74add1', '#4575b4', '#313695']
        }
      },
      series: [
        {
          name: config.measures?.[0] || '数据',
          type: 'map',
          map: 'china',
          roam: true,
          label: {
            show: true,
            fontSize: 10
          },
          emphasis: {
            label: {
              color: '#fff'
            },
            itemStyle: {
              areaColor: '#f59e0b'
            }
          },
          data: mapData
        }
      ]
    }

    chartInstance.current.setOption(option, true)

    if (onClick) {
      chartInstance.current.off('click')
      chartInstance.current.on('click', onClick)
    }
  }, [data, config, mapLoaded, onClick])

  useEffect(() => {
    return () => {
      chartInstance.current?.dispose()
    }
  }, [])

  return (
    <BaseChart
      title={title}
      loading={loading || !mapLoaded}
      data={data}
      error={error}
      height={height}
      onRefresh={onRefresh}
    >
      <div ref={chartRef} style={{ width: '100%', height: '100%' }} />
    </BaseChart>
  )
}

export default MapChart
