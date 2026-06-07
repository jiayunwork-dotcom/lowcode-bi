import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import Login from '@/pages/Login'
import MainLayout from '@/layouts/MainLayout'
import DashboardList from '@/pages/dashboard/List'
import DashboardEditor from '@/pages/dashboard/Editor'
import DashboardViewer from '@/pages/dashboard/Viewer'
import DataSourceList from '@/pages/datasource/List'
import DataModelList from '@/pages/datamodel/List'
import DataModelEditor from '@/pages/datamodel/Editor'
import SqlEditor from '@/pages/sql/Editor'
import EmbedViewer from '@/pages/embed/Viewer'
import UserManagement from '@/pages/system/UserManagement'
import RoleManagement from '@/pages/system/RoleManagement'
import PermissionManagement from '@/pages/system/PermissionManagement'
import ScheduleManagement from '@/pages/schedule/List'
import AlertCenter from '@/pages/alert/AlertCenter'
import AlertRuleManagement from '@/pages/alert/AlertRuleManagement'

function App() {
  const { isAuthenticated, token } = useAuthStore()

  if (!isAuthenticated && !token) {
    return (
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/embed/dashboard/:id" element={<EmbedViewer />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    )
  }

  return (
    <Routes>
      <Route path="/login" element={<Navigate to="/dashboard" replace />} />
      <Route path="/embed/dashboard/:id" element={<EmbedViewer />} />
      <Route path="/" element={<MainLayout />}>
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<DashboardList />} />
        <Route path="dashboard/:id/edit" element={<DashboardEditor />} />
        <Route path="dashboard/:id/view" element={<DashboardViewer />} />
        <Route path="datasource" element={<DataSourceList />} />
        <Route path="datamodel" element={<DataModelList />} />
        <Route path="datamodel/:id/edit" element={<DataModelEditor />} />
        <Route path="sql" element={<SqlEditor />} />
        <Route path="schedule" element={<ScheduleManagement />} />
        <Route path="alert" element={<AlertCenter />} />
        <Route path="alert/rules" element={<AlertRuleManagement />} />
        <Route path="system/users" element={<UserManagement />} />
        <Route path="system/roles" element={<RoleManagement />} />
        <Route path="system/permissions" element={<PermissionManagement />} />
      </Route>
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  )
}

export default App
