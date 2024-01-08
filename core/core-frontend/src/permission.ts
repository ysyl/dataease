import router from './router'
import { useUserStoreWithOut } from '@/store/modules/user'
import { useAppStoreWithOut } from '@/store/modules/app'
import type { RouteRecordRaw } from 'vue-router'
import { useNProgress } from '@/hooks/web/useNProgress'
import { usePermissionStoreWithOut, pathValid, getFirstAuthMenu } from '@/store/modules/permission'
import { usePageLoading } from '@/hooks/web/usePageLoading'
import { getRoleRouters } from '@/api/common'
import { useCache } from '@/hooks/web/useCache'
import { interactiveStoreWithOut } from '@/store/modules/interactive'
const { wsCache } = useCache()
const permissionStore = usePermissionStoreWithOut()
const interactiveStore = interactiveStoreWithOut()
const userStore = useUserStoreWithOut()
const appStore = useAppStoreWithOut()

const { start, done } = useNProgress()

const { loadStart, loadDone } = usePageLoading()

const whiteList = ['/login', '/de-link'] // 不重定向白名单

router.beforeEach(async (to, from, next) => {
  start()
  loadStart()
  let isDesktop = wsCache.get('app.desktop')
  if (isDesktop === null) {
    await appStore.setAppModel()
    isDesktop = appStore.getDesktop
  }
  if (wsCache.get('user.token') || isDesktop) {
    if (!userStore.getUid) {
      await userStore.setUser()
    }
    if (to.path === '/login') {
      next({ path: '/workbranch/index' })
    } else {
      permissionStore.setCurrentPath(to.path)
      if (permissionStore.getIsAddRouters) {
        let str = ''
        if (((from.query.redirect as string) || '?').split('?')[0] === to.path) {
          str = ((from.query.redirect as string) || '?').split('?')[1]
        }
        if (str) {
          to.fullPath += '?' + str
          to.query = str.split('&').reduce((pre, itx) => {
            const [key, val] = itx.split('=')
            pre[key] = val
            return pre
          }, {})
        }
        if (!pathValid(to.path) && to.path !== '/404' && !to.path.startsWith('/de-link')) {
          const firstPath = getFirstAuthMenu()
          next({ path: firstPath || '/404' })
          return
        }
        next()
        return
      }

      let roleRouters = (await getRoleRouters()) || []
      if (isDesktop) {
        roleRouters = roleRouters.filter(item => item.name !== 'system')
      }
      const routers: any[] = roleRouters as AppCustomRouteRecordRaw[]
      routers.forEach(item => (item['top'] = true))
      await permissionStore.generateRoutes(routers as AppCustomRouteRecordRaw[])

      permissionStore.getAddRouters.forEach(route => {
        router.addRoute(route as unknown as RouteRecordRaw) // 动态添加可访问路由表
      })

      const redirectPath = from.query.redirect || to.path
      const redirect = decodeURIComponent(redirectPath as string)
      const nextData = to.path === redirect ? { ...to, replace: true } : { path: redirect }

      permissionStore.setIsAddRouters(true)
      await interactiveStore.initInteractive(true)

      if (!pathValid(to.path) && to.path !== '/404' && !to.path.startsWith('/de-link')) {
        const firstPath = getFirstAuthMenu()
        next({ path: firstPath || '/404' })
        return
      }
      next(nextData)
    }
  } else {
    if (whiteList.indexOf(to.path) !== -1 || to.path.startsWith('/de-link/')) {
      permissionStore.setCurrentPath(to.path)
      next()
    } else {
      next(`/login?redirect=${to.path}`) // 否则全部重定向到登录页
    }
  }
})

router.afterEach(() => {
  done()
  loadDone()
})
