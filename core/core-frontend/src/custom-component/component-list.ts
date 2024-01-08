// 公共样式
import { deepCopy } from '@/utils/utils'
import { guid } from '@/views/visualized/data/dataset/form/util'
import { getViewConfig } from '@/views/chart/components/editor/util/chart'

export const commonStyle = {
  rotate: 0,
  opacity: 1
}

export const defaultStyleValue = {
  ...commonStyle,
  color: '',
  fontSize: 16,
  activeFontSize: 18,
  headHorizontalPosition: 'left',
  headFontColor: '#000000',
  headFontActiveColor: '#000000'
}

export const COMMON_COMPONENT_BACKGROUND_BASE = {
  backgroundColorSelect: true,
  backgroundImageEnable: false,
  backgroundType: 'innerImage',
  innerImage: 'board/board_1.svg',
  outerImage: null,
  innerPadding: 12,
  borderRadius: 0
}

export const COMMON_COMPONENT_BACKGROUND_LIGHT = {
  ...COMMON_COMPONENT_BACKGROUND_BASE,
  backgroundColor: 'rgba(255,255,255,1)',
  innerImageColor: 'rgba(16, 148, 229,1)'
}

export const COMMON_COMPONENT_BACKGROUND_DARK = {
  ...COMMON_COMPONENT_BACKGROUND_BASE,
  backgroundColor: 'rgba(19,28,66,1)',
  innerImageColor: '#1094E5'
}

export const COMMON_COMPONENT_BACKGROUND_SCREEN_DARK = {
  ...COMMON_COMPONENT_BACKGROUND_BASE,
  backgroundColorSelect: false,
  backgroundColor: '#131E42',
  innerImageColor: '#1094E5'
}

export const COMMON_COMPONENT_BACKGROUND_MAP = {
  light: COMMON_COMPONENT_BACKGROUND_LIGHT,
  dark: COMMON_COMPONENT_BACKGROUND_DARK
}

export const commonAttr = {
  animations: [],
  canvasId: 'canvas-main',
  events: {},
  groupStyle: {}, // 当一个组件成为 Group 的子组件时使用
  isLock: false, // 是否锁定组件
  isShow: true, // 是否显示组件
  collapseName: ['position', 'background', 'style', 'picture'], // 编辑组件时记录当前使用的是哪个折叠面板，再次回来时恢复上次打开的折叠面板，优化用户体验
  linkage: {
    duration: 0, // 过渡持续时间
    data: [
      // 组件联动
      {
        id: '', // 联动的组件 id
        label: '', // 联动的组件名称
        event: '', // 监听事件
        style: [{ key: '', value: '' }] // 监听的事件触发时，需要改变的属性
      }
    ]
  }
}

// 编辑器左侧组件列表
const list = [
  {
    component: 'VQuery',
    name: '查询',
    label: '查询',
    propValue: '',
    icon: 'other_text',
    innerType: 'VQuery',
    x: 1,
    y: 1,
    sizeX: 36,
    sizeY: 2,
    request: {
      method: 'GET',
      data: [],
      url: '',
      series: false, // 是否定时发送请求
      time: 1000, // 定时更新时间
      paramType: '', // string object array
      requestCount: 0 // 请求次数限制，0 为无限
    },
    matrixStyle: {}
  },
  {
    component: 'UserView',
    name: '图表',
    label: '图表',
    propValue: { textValue: '' },
    icon: 'bar',
    innerType: 'bar',
    editing: false,
    canvasActive: false,
    x: 1,
    y: 1,
    sizeX: 18,
    sizeY: 7,
    style: {
      width: 600,
      height: 300
    },
    matrixStyle: {}
  },
  {
    component: 'Picture',
    name: '图片',
    label: '图片',
    icon: 'dv-picture-real',
    innerType: 'Picture',
    editing: false,
    canvasActive: false,
    x: 1,
    y: 1,
    sizeX: 18,
    sizeY: 7,
    propValue: {
      url: '',
      flip: {
        horizontal: false,
        vertical: false
      }
    },
    style: {
      width: 300,
      height: 200
    },
    matrixStyle: {}
  },
  {
    component: 'CanvasIcon',
    name: '图标',
    label: '图标',
    propValue: '',
    icon: 'other_material_icon',
    innerType: '',
    editing: false,
    canvasActive: false,
    x: 1,
    y: 1,
    sizeX: 5,
    sizeY: 5,
    style: {
      width: 40,
      height: 40,
      color: ''
    }
  },
  {
    component: 'CanvasBoard',
    name: '边框',
    label: '边框',
    propValue: '',
    icon: 'other_material_board',
    innerType: '',
    editing: false,
    canvasActive: false,
    x: 1,
    y: 1,
    sizeX: 15,
    sizeY: 15,
    style: {
      width: 600,
      height: 300,
      color: ''
    }
  },
  {
    component: 'DeTabs',
    name: '选项卡',
    label: '选项卡',
    propValue: [
      {
        name: 'tab',
        title: '新建Tab',
        componentData: [],
        closable: true
      }
    ],
    icon: 'dv-tab',
    innerType: '',
    editing: false,
    canvasActive: false,
    x: 1,
    y: 1,
    sizeX: 18,
    sizeY: 7,
    style: {
      width: 600,
      height: 300,
      fontSize: 16,
      activeFontSize: 18,
      headHorizontalPosition: 'left',
      headFontColor: '#000000',
      headFontActiveColor: '#000000'
    }
  }
]

for (let i = 0, len = list.length; i < len; i++) {
  const item = list[i]
  item.style = { ...commonStyle, ...item.style }
  item['commonBackground'] = deepCopy(COMMON_COMPONENT_BACKGROUND_BASE)
  item['state'] = 'prepare'
  list[i] = { ...commonAttr, ...item }
}

export function findNewComponentFromList(componentName, innerType, curOriginThemes) {
  let newComponent
  list.forEach(comp => {
    if (comp.component === componentName) {
      newComponent = deepCopy(comp)
      newComponent['commonBackground'] = deepCopy(
        COMMON_COMPONENT_BACKGROUND_MAP[curOriginThemes.value]
      )
      newComponent.innerType = innerType
      if (comp.component === 'DeTabs') {
        newComponent.propValue[0].name = guid()
      }
    }
  })

  if (componentName === 'UserView') {
    const viewConfig = getViewConfig(innerType)
    newComponent.name = viewConfig?.title
    newComponent.label = viewConfig?.title
    newComponent.render = viewConfig?.render
  }
  return newComponent
}

export function findBaseDeFaultAttr(componentName) {
  let result = {}
  list.forEach(comp => {
    if (comp.component === componentName) {
      const stylePropertyInner = []
      Object.keys(comp.style).forEach(styleKey => {
        stylePropertyInner.push(styleKey)
      })
      result = {
        properties: ['common-style', 'background-overall-component'],
        propertyInner: {
          'common-style': stylePropertyInner,
          'background-overall-component': ['all']
        },
        value: comp.name,
        componentType: componentName
      }
    }
  })
  return result
}

export default list
