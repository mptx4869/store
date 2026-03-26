import { useState, useEffect } from 'react';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend,
} from 'chart.js';
import { Bar } from 'react-chartjs-2';
import adminOrderService from '../../services/adminOrderService';
import { formatCurrency } from '../../utils/format';

ChartJS.register(
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend
);

function AdminReportsPage() {
  const [analyticsData, setAnalyticsData] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [groupBy, setGroupBy] = useState('MONTH');

  useEffect(() => {
    async function fetchAnalytics() {
      setIsLoading(true);
      setError('');
      try {
        const getLocalISOString = (date) => {
          const tzOffset = date.getTimezoneOffset() * 60000;
          return new Date(date.getTime() - tzOffset).toISOString().slice(0, -1);
        };

        const endDate = new Date();
        let startDate = new Date();

        if (groupBy === 'DAY') startDate.setDate(startDate.getDate() - 30);
        else if (groupBy === 'WEEK') startDate.setDate(startDate.getDate() - 90);
        else if (groupBy === 'MONTH') startDate.setMonth(startDate.getMonth() - 12);

        const data = await adminOrderService.getRevenueAnalytics(getLocalISOString(startDate), getLocalISOString(endDate), groupBy);
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

  const chartData = {
    labels: analyticsData.map(item => item.period),
    datasets: [
      {
        label: 'Revenue',
        data: analyticsData.map(item => item.totalRevenue),
        backgroundColor: 'rgba(59, 130, 246, 0.8)',
        borderColor: 'rgba(37, 99, 235, 1)',
        borderWidth: 1,
        yAxisID: 'y',
        borderRadius: 4,
      },
      {
        label: 'Orders',
        data: analyticsData.map(item => item.orderCount),
        backgroundColor: 'rgba(16, 185, 129, 0.8)',
        borderColor: 'rgba(5, 150, 105, 1)',
        borderWidth: 1,
        yAxisID: 'y1',
        borderRadius: 4,
      }
    ],
  };

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: {
      mode: 'index',
      intersect: false,
    },
    plugins: {
      tooltip: {
        callbacks: {
          label: function(context) {
            let label = context.dataset.label || '';
            if (label) {
              label += ': ';
            }
            if (context.parsed.y !== null) {
              if (context.datasetIndex === 0) {
                label += formatCurrency(context.parsed.y);
              } else {
                label += context.parsed.y;
              }
            }
            return label;
          }
        }
      }
    },
    scales: {
      y: {
        type: 'linear',
        display: true,
        position: 'left',
        title: { display: true, text: 'Revenue' }
      },
      y1: {
        type: 'linear',
        display: true,
        position: 'right',
        grid: { drawOnChartArea: false },
        title: { display: true, text: 'Orders' },
        ticks: { stepSize: 1 }
      },
    },
  };

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Revenue Reports</h1>
          <p className="text-sm text-gray-500 mt-1">
            View income and order volume over time.
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
        <div className="bg-white border rounded-xl p-6 shadow-sm flex flex-col justify-center items-center">
          <p className="text-sm text-gray-500 font-medium">Total Revenue ({groupBy})</p>
          <p className="text-4xl font-bold text-blue-600 mt-2">{formatCurrency(totalRevenue)}</p>
        </div>
        <div className="bg-white border rounded-xl p-6 shadow-sm flex flex-col justify-center items-center">
          <p className="text-sm text-gray-500 font-medium">Total Orders ({groupBy})</p>
          <p className="text-4xl font-bold text-green-600 mt-2">{totalOrders}</p>
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
          <div className="h-80 w-full pb-4">
            <Bar data={chartData} options={chartOptions} />
          </div>
        )}
      </div>
    </div>
  );
}

export default AdminReportsPage;
