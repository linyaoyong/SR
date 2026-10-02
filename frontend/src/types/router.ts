import type { Location } from 'react-router-dom';

// 用于在 ProtectedRoute 重定向到 /login 时携带原目标 location，供登录/注册成功后回跳。
export interface LocationState {
  from?: Location;
}
