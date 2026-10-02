import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import AppLayout from './components/AppLayout';
import ProtectedRoute from './components/ProtectedRoute';
import BlacklistPage from './pages/BlacklistPage';
import ConversationPage from './pages/ConversationPage';
import DisputeCreatePage from './pages/DisputeCreatePage';
import FavoritesPage from './pages/FavoritesPage';
import ItemDetailPage from './pages/ItemDetailPage';
import ItemEditPage from './pages/ItemEditPage';
import ItemsPage from './pages/ItemsPage';
import LoginPage from './pages/LoginPage';
import MessagesPage from './pages/MessagesPage';
import MyItemsPage from './pages/MyItemsPage';
import OrderDetailPage from './pages/OrderDetailPage';
import OrderSnapshotPage from './pages/OrderSnapshotPage';
import OrdersPage from './pages/OrdersPage';
import ProfilePage from './pages/ProfilePage';
import RegisterPage from './pages/RegisterPage';
import UserDetailPage from './pages/UserDetailPage';
import WalletPage from './pages/WalletPage';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route element={<AppLayout />}>
          <Route path="/" element={<Navigate to="/items" replace />} />
          <Route path="/items" element={<ItemsPage />} />
          <Route path="/items/:id" element={<ItemDetailPage />} />
          <Route path="/users/:id" element={<UserDetailPage />} />
          <Route element={<ProtectedRoute />}>
            <Route path="/my-items" element={<MyItemsPage />} />
            <Route path="/items/create" element={<ItemEditPage mode="create" />} />
            <Route path="/items/:id/edit" element={<ItemEditPage mode="edit" />} />
            <Route path="/messages" element={<MessagesPage />} />
            <Route path="/messages/:conversationId" element={<ConversationPage />} />
            <Route path="/orders" element={<OrdersPage />} />
            <Route path="/orders/:id" element={<OrderDetailPage />} />
            <Route path="/orders/:id/snapshot" element={<OrderSnapshotPage />} />
            <Route path="/favorites" element={<FavoritesPage />} />
            <Route path="/profile" element={<ProfilePage />} />
            <Route path="/wallet" element={<WalletPage />} />
            <Route path="/blacklist" element={<BlacklistPage />} />
            <Route path="/orders/:id/dispute" element={<DisputeCreatePage />} />
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/items" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
