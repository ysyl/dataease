import { FunnelOptions, Funnel as G2Funnel } from '@antv/g2plot/esm/plots/funnel'
import { G2PlotChartView, G2PlotDrawOptions } from '../../types/impl/g2plot'
import { flow } from '@/views/chart/components/js/util'
import { getPadding } from '../../common/common_antv'
import { useI18n } from '@/hooks/web/useI18n'

const { t } = useI18n()

/**
 * 漏斗图
 */
export class Funnel extends G2PlotChartView<FunnelOptions, G2Funnel> {
  properties: EditorProperty[] = [
    'background-overall-component',
    'basic-style-selector',
    'label-selector',
    'tooltip-selector',
    'title-selector',
    'legend-selector',
    'jump-set',
    'linkage'
  ]
  propertyInner: EditorPropertyInner = {
    'background-overall-component': ['all'],
    'basic-style-selector': ['colors', 'alpha'],
    'label-selector': ['fontSize', 'color', 'hPosition', 'labelFormatter'],
    'tooltip-selector': ['color', 'fontSize', 'backgroundColor', 'seriesTooltipFormatter'],
    'title-selector': [
      'show',
      'title',
      'fontSize',
      'color',
      'hPosition',
      'isItalic',
      'isBolder',
      'remarkShow',
      'fontFamily',
      'letterSpace',
      'fontShadow'
    ],
    'legend-selector': ['icon', 'orient', 'color', 'fontSize', 'hPosition', 'vPosition']
  }
  axis: AxisType[] = ['xAxis', 'yAxis', 'filter', 'drill', 'extLabel', 'extTooltip']
  axisConfig: AxisConfig = {
    xAxis: {
      name: `${t('chart.drag_block_funnel_split')} / ${t('chart.dimension')}`,
      type: 'd'
    },
    yAxis: {
      name: `${t('chart.drag_block_funnel_width')} / ${t('chart.quota')}`,
      type: 'q',
      limit: 1
    }
  }

  public drawChart(drawOptions: G2PlotDrawOptions<G2Funnel>): G2Funnel {
    const { chart, container, action } = drawOptions
    if (!chart.data?.data) {
      return
    }
    const data = chart.data.data
    const baseOptions: FunnelOptions = {
      data,
      xField: 'field',
      yField: 'value',
      appendPadding: getPadding(chart),
      conversionTag: false,
      interactions: [
        {
          type: 'legend-active',
          cfg: {
            start: [{ trigger: 'legend-item:mouseenter', action: ['element-active:reset'] }],
            end: [{ trigger: 'legend-item:mouseleave', action: ['element-active:reset'] }]
          }
        },
        {
          type: 'legend-filter',
          cfg: {
            start: [
              {
                trigger: 'legend-item:click',
                action: [
                  'list-unchecked:toggle',
                  'data-filter:filter',
                  'element-active:reset',
                  'element-highlight:reset'
                ]
              }
            ]
          }
        },
        {
          type: 'tooltip',
          cfg: {
            start: [{ trigger: 'interval:mousemove', action: 'tooltip:show' }],
            end: [{ trigger: 'interval:mouseleave', action: 'tooltip:hide' }]
          }
        }
      ]
    }
    const options = this.setupOptions(chart, baseOptions)
    const newChart = new G2Funnel(container, options)
    newChart.on('interval:click', action)
    return newChart
  }

  protected configLabel(chart: Chart, options: FunnelOptions): FunnelOptions {
    const tmpOptions = super.configLabel(chart, options)
    if (!tmpOptions.label) {
      return tmpOptions
    }
    const position = tmpOptions.label.position
    if (position === 'right') {
      tmpOptions.label.offsetX = -40
    }
    return tmpOptions
  }

  protected setupOptions(chart: Chart, options: FunnelOptions): FunnelOptions {
    return flow(
      this.configTheme,
      this.configLabel,
      this.configMultiSeriesTooltip,
      this.configLegend
    )(chart, options)
  }
  setupDefaultOptions(chart: ChartObj): ChartObj {
    const { customAttr } = chart
    const { label } = customAttr
    if (!['left', 'middle', 'right'].includes(label.position)) {
      label.position = 'middle'
    }
    return chart
  }

  constructor() {
    super('funnel', [])
  }
}
