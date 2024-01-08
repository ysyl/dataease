import { AbstractChartView, ChartLibraryType, ChartRenderType } from '../../types'
import { useI18n } from '@/hooks/web/useI18n'

const { t } = useI18n()
/**
 * 富文本视图
 */
export class RichTextChartView extends AbstractChartView {
  properties: EditorProperty[] = ['background-overall-component']
  propertyInner: EditorPropertyInner = {
    'background-overall-component': ['all']
  }
  axis: AxisType[] = ['xAxis', 'yAxis', 'filter']
  axisConfig: AxisConfig = {
    xAxis: {
      name: `${t('chart.dimension')}`,
      type: 'd'
    },
    yAxis: {
      name: `${t('chart.quota')}`,
      type: 'q'
    }
  }
  constructor() {
    super(ChartRenderType.CUSTOM, ChartLibraryType.RICH_TEXT, 'rich-text')
  }
}
