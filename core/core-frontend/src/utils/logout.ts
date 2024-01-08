import { useUserStoreWithOut } from '@/store/modules/user'
import router from '@/router'
import { usePermissionStoreWithOut } from '@/store/modules/permission'
import { interactiveStoreWithOut } from '@/store/modules/interactive'
import { useCache } from '@/hooks/web/useCache'
const { wsCache } = useCache()
const permissionStore = usePermissionStoreWithOut()
const userStore = useUserStoreWithOut()
const interactiveStore = interactiveStoreWithOut()

export const logoutHandler = (justClean?: boolean) => {
  userStore.clear()
  userStore.$reset()
  permissionStore.clear()
  permissionStore.$reset()
  interactiveStore.clear()
  interactiveStore.$reset()
  removeCache()
  let queryRedirectPath = '/workbranch/index'
  // 如果redirect参数中有值
  if (router.currentRoute.value.fullPath) {
    queryRedirectPath = router.currentRoute.value.fullPath as string
  }
  if (wsCache.get('out_auth_platform') === 'cas') {
    const uri = window.location.href
    window.location.href = '/casbi/cas/logout?service=' + uri
    return
  }
  if (wsCache.get('out_auth_platform') === 'oidc') {
    window.location.href = '/oidcbi/oidc/logout'
    return
  }
  router.push(justClean ? queryRedirectPath : `/login?redirect=${queryRedirectPath}`)
}

const removeCache = () => {
  const keys = Object.keys(wsCache['storage'])
  keys.forEach(key => {
    if (key.startsWith('de-plugin-')) {
      wsCache.delete(key)
    }
  })
}
