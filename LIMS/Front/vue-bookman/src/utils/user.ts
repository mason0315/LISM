import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

/**
 * 从 localStorage 获取当前用户的 user_id
 * @returns 用户ID 或 null（未登录时）
 */
export const getCurrentUserId = (): number | null => {
  const userStr = localStorage.getItem('user');
  if (!userStr) {
    return null;
  }
  const user = JSON.parse(userStr);
  return user?.user_id || null;
};

/**
 * 获取当前登录用户的用户名
 */
export const getCurrentUsername = (): string | null => {
  const userStr = localStorage.getItem('user');
  if (!userStr) return null;
  const user = JSON.parse(userStr);
  return user?.username || null;
};

export function useLogout() {
  const router = useRouter();
  function handleLogout() {
    localStorage.removeItem('user');
    localStorage.removeItem('token');
    localStorage.clear();
    ElMessage.success('已成功退出登录');
    router.push('/');
  }
  return { handleLogout };
}