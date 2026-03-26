import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import adminOrderService from '../../services/adminOrderService';
import { formatCurrency } from '../../utils/format';

function AdminDashboardPage() {
  const [analyticsData, setAnalyticsData] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [groupBy, setGroupBy] = useState('DAY');

  useEffect(() => {
    async function fetchAnalytics() {
      setIsLoading(true);
      setError('');
      try {
        const endDate = new Date().toISOString();
        let startDate = new Date();
        
        if (groupBy === 'DAY') startDate.setDate(startDate.getDate() - 30);
        else if (groupBy === 'WEEK') startDate.setDate(startDate.getDate() - 90);
        else if (groupBy === 'MONTH') startDate.setMonth(startDate.getMonth() - 12);
        
        const data = await adminOrderService.getRevenueAnalytics(startDate.toISOString(), endDate, groupBy);
        setAnalyticsData(data);
      } catch (err) {
        setError('Failed to load analytics data.');
      } finally {
        setIsLoading(false);
      }
    }
    fetchAnalytics();
  }, [groupBy]);

  const totalRevenue = analyticsData.reduce((sum, item) => sum + item.totalRevenue, 0);
  const totalOrders = analyticsData.reduce((sum, item) => sum + item.orderCount, 0);

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Admin Dashboard</h1>
          <p className="text-sm text-gray-500 mt-1">
            System administration area & Revenue Analytics.
          </p>
        </div>
        <select 
          value={groupBy} 
          onChange={(e) => setGroupBy(e.target.value)}
          className="border rounded-lg px-4 py-2 bg-white shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="DAY">Last 30 Days (Daily)</option>
          <option value="WEEK">Last 3 Months (Weekly)</option>
          <option value="MONTH">Last 12 Months (Monthly)</option>
        </select>
      </div>

      {/* Summary Cards */}
      <div className="grid sm:grid-cols-2 gap-4">
        <div className="bg-white border rounded-xl p-6 shadow-sm">
          <p className="text-sm text-gray-500 font-medium">Total Revenue</p>
          <p className="text-3xl font-bold text-gray-800 mt-2">{formatCurrency(totalRevenue)}</p>
        </div>
        <div className="bg-white border rounded-xl p-6 shadow-sm">
          <p className="text-sm text-gray-500 font-medium">Total Orders</p>
          <p className="text-3xl font-bold text-gray-800 mt-2">{totalOrders}</p>
        </div>
      </div>

      {/* Analytics Chart */}
      <div className="bg-white border rounded-xl p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-gray-800 mb-6">Revenue Chart</h2>
        {isLoading ? (
          <div className="h-80 flex items-center justify-center text-gray-500">Loading chart data...</div>
        ) : error ? (
          <div className="h-80 flex items-center justify-center text-red-500">{error}</div>
        ) : analyticsData.length === 0 ? (
          <div className="h-80 flex items-center justify-center text-gray-500">No data available for this period.</div>
        ) : (
          <div className="h-80 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={analyticsData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="period" />
                <YAxis yAxisId="left" orientation="left" stroke="#3b82f6" />
                <YAxis yAxisId="right" orientation="right" stroke="#10b981" />
                <Tooltip 
                  formatter={(value, name) => [
                    name === 'Revenue' ? formatCurrency(value) : value, 
                    name
                  ]}
                />
                <Legend />
                <Bar yAxisId="left" dataKey="totalRevenue" name="Revenue" fill="#3b82f6" radius={[4, 4, 0, 0]} />
                <Bar yAxisId="right" dataKey="orderCount" name="Orders" fill="#10b981" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}
      </div>

      {/* Quick Links */}
      <h2 className="text-lg font-semibold text-gray-800 mt-8">Quick Links</h2>
      <div className="grid sm:grid-cols-2 gap-4">
        <Link to="/admin/users" className="block border rounded-xl p-4 hover:bg-gray-50 transition-colors">
          <p className="font-semibold text-gray-800">User Management</p>
          <p className="text-sm text-gray-500 mt-1">View user list, change roles, lock/unlock accounts.</p>
        </Link>
        <Link to="/admin/books" className="block border rounded-xl p-4 hover:bg-gray-50 transition-colors">
          <p className="font-semibold text-gray-800">Book Management</p>
          <p className="text-sm text-gray-500 mt-1">CRUD books, manage SKUs and inventory.</p>
        </Link>
      </div>
    </div>
  );
}

export default AdminDashboardPage;