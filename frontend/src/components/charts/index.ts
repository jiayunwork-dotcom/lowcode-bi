import LineChart from './LineChart'
import BarChart from './BarChart'
import PieChart from './PieChart'
import ScatterChart from './ScatterChart'
import TableChart from './TableChart'
import KpiCard from './KpiCard'
import FunnelChart from './FunnelChart'
import MapChart from './MapChart'
import BaseChart from './BaseChart'

export type ChartType = 'LINE' | 'BAR' | 'PIE' | 'SCATTER' | 'TABLE' | 'KPI' | 'FUNNEL' | 'MAP'

export const ChartComponents: Record<ChartType, any> = {
  LINE: LineChart,
  BAR: BarChart,
  PIE: PieChart,
  SCATTER: ScatterChart,
  TABLE: TableChart,
  KPI: KpiCard,
  FUNNEL: FunnelChart,
  MAP: MapChart
}

export const ChartTypeLabels: Record<ChartType, string> = {
  LINE: '折线图',
  BAR: '柱状图',
  PIE: '饼图',
  SCATTER: '散点图',
  TABLE: '表格',
  KPI: '指标卡',
  FUNNEL: '漏斗图',
  MAP: '地图'
}

export const ChartTypeIcons: Record<ChartType, string> = {
  LINE: '📈',
  BAR: '📊',
  PIE: '🥧',
  SCATTER: '💠',
  TABLE: '📋',
  KPI: '🎯',
  FUNNEL: '🔻',
  MAP: '🗺️'
}

export {
  LineChart,
  BarChart,
  PieChart,
  ScatterChart,
  TableChart,
  KpiCard,
  FunnelChart,
  MapChart,
  BaseChart
}
