import { Link, useLocation } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { 
  LayoutDashboard, 
  Users, 
  BookOpen, 
  FolderTree,
  Package,
  ShoppingCart,
  Settings,
  LogOut 
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import adminInventoryService from '../../services/adminInventoryService';

function AdminLayout({ children }) {
  const location = useLocation();
  const { user, logout } = useAuth();
  const [lowStockCount, setLowStockCount] = useState(0);

  useEffect(() => {
    loadLowStockCount();
    
    const interval = setInterval(loadLowStockCount, 5 * 60 * 1000);
    return () => clearInterval(interval);
  }, []);

  const loadLowStockCount = async () => {
    try {
      const lowStockItems = await adminInventoryService.getLowStock();
      setLowStockCount(lowStockItems.length);
    } catch (err) {
      console.error('Failed to load low stock count:', err);
    }
  };

  const navItems = [
    { path: '/admin', label: 'Dashboard', icon:  <LayoutDashboard className="w-5 h-5" /> },
    { path: '/admin/reports', label: 'Reports', icon: <BookOpen className="w-5 h-5" /> },
    { path: '/admin/users', label: 'Users', icon: <Users className="w-5 h-5" /> },
    { path: '/admin/books', label: 'Books', icon: <BookOpen className="w-5 h-5" /> },
    { path: '/admin/categories', label: 'Categories', icon: <FolderTree className="w-5 h-5" /> },
    { path: '/admin/orders', label: 'Orders', icon: <ShoppingCart className="w-5 h-5" /> },
    { path: '/admin/inventory', label: 'Inventory', icon: <Package className="w-5 h-5" />, badge: lowStockCount },
    { path: '/admin/settings', label: 'Settings', icon: <Settings className="w-5 h-5" /> },
  ];

  const isActive = (path) => {
    if (path === '/admin') {
      return location.pathname === '/admin';
    }
    return location.pathname.startsWith(path);
  };

  return (
    <div className="min-h-screen bg-gray-100">
      {/* Sidebar */}
      <aside className="fixed left-0 top-0 bottom-0 w-64 bg-white shadow-lg overflow-y-auto z-30">
        {/* Header */}
        <div className="p-6 border-b bg-white">
          <Link to="/admin" className="block">
            <h1 className="text-2xl font-bold text-gray-800">Admin Panel</h1>
          </Link>
          <p className="text-sm text-gray-600 mt-1">{user?. username}</p>
        </div>

        {/* Navigation */}
        <nav className="p-4">
          {navItems.map((item) => (
            <Link
              key={item.path}
              to={item.path}
              className={`
                flex items-center justify-between gap-3 px-4 py-3 rounded-lg mb-2 transition-colors
                ${
                  isActive(item.path)
                    ? 'bg-blue-50 text-blue-600 font-medium'
                    : 'text-gray-700 hover:bg-gray-50'
                }
              `}
            >
              <div className="flex items-center gap-3">
                {item.icon}
                {item.label}
              </div>
              {item.badge !== undefined && item.badge > 0 && (
                <span className="inline-flex items-center justify-center px-2 py-1 text-xs font-bold leading-none text-white bg-red-600 rounded-full min-w-[20px]">
                  {item.badge}
                </span>
              )}
            </Link>
          ))}
        </nav>

        {/* Logout */}
        <div className="absolute bottom-0 left-0 right-0 p-4 border-t bg-white">
          <button
            onClick={logout}
            className="w-full flex items-center gap-3 px-4 py-3 rounded-lg text-red-600 hover:bg-red-50 transition-colors"
          >
            <LogOut className="w-5 h-5" />
            Logout
          </button>
        </div>
      </aside>

      {/* Main content */}
      <main className="ml-64 min-h-screen relative z-40">
        <div className="p-8">
          {children}
        </div>
      </main>
    </div>
  );
}

export default AdminLayout;