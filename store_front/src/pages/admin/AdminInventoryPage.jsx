import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import adminInventoryService from '../../services/adminInventoryService';
import UpdateStockModal from '../../components/admin/UpdateStockModal';
import { useToast } from '../../context/ToastContext';
import { Package, AlertTriangle, Edit } from 'lucide-react';

function AdminInventoryPage() {
  const navigate = useNavigate();
  const toast = useToast();

  const [inventories, setInventories] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  // Pagination state
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [size] = useState(20);

  // Sort state
  const [sort, setSort] = useState('lastUpdated');
  const [direction, setDirection] = useState('DESC');

  // Low stock count
  const [lowStockCount, setLowStockCount] = useState(0);

  // Update stock modal
  const [isUpdateModalOpen, setIsUpdateModalOpen] = useState(false);
  const [selectedInventory, setSelectedInventory] = useState(null);

  useEffect(() => {
    loadInventory();
    loadLowStockCount();
  }, [page, sort, direction]);

  const loadInventory = async () => {
    setIsLoading(true);
    setError('');
    try {
      const result = await adminInventoryService.getInventory({
        page,
        size,
        sort,
        direction,
      });

      setInventories(result.content);
      setTotalPages(result.totalPages);
      setTotalElements(result. totalElements);
    } catch (err) {
      setError(err.message || 'Could not load inventory list');
      toast.error(err.message);
    } finally {
      setIsLoading(false);
    }
  };

  const loadLowStockCount = async () => {
    try {
      const lowStockItems = await adminInventoryService.getLowStock();
      setLowStockCount(lowStockItems.length);
    } catch (err) {
      console.error('Failed to load low stock count:', err);
    }
  };

  const handleSort = (field) => {
    if (sort === field) {
      setDirection((prev) => (prev === 'ASC' ? 'DESC' : 'ASC'));
    } else {
      setSort(field);
      setDirection('DESC');
    }
    setPage(0);
  };

  const getSortIcon = (field) => {
    if (sort !== field) return '↕️';
    return direction === 'ASC' ? '↑' :  '↓';
  };

  const getStatusBadge = (status) => {
    const styles = {
      IN_STOCK: 'bg-green-100 text-green-800',
      LOW_STOCK: 'bg-yellow-100 text-yellow-800',
      OUT_OF_STOCK: 'bg-red-100 text-red-800',
    };

    const labels = {
      IN_STOCK: 'In Stock',
      LOW_STOCK: 'Low Stock',
      OUT_OF_STOCK: 'Out of Stock',
    };

    return (
      <span className={`inline-block px-2 py-1 text-xs font-medium rounded ${styles[status] || 'bg-gray-100 text-gray-800'}`}>
        {labels[status] || status}
      </span>
    );
  };

  const handleUpdateStock = (inventory) => {
    setSelectedInventory(inventory);
    setIsUpdateModalOpen(true);
  };

  const handleStockSubmit = async (payload) => {
    try {
      await adminInventoryService.updateStock(selectedInventory.skuId, payload);
      toast.success('Stock updated successfully');
      loadInventory();
      loadLowStockCount();
    } catch (err) {
      toast.error(err.message || 'Could not update stock');
      throw err;
    }
  };

  if (isLoading && inventories.length === 0) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-600">Loading inventory list...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header - Reduce icon size and add flex-wrap */}
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div className="flex-1 min-w-0">
          <h1 className="text-3xl font-bold text-gray-800 flex items-center gap-2">
            <Package className="w-7 h-7 flex-shrink-0" />
            <span>Inventory Management</span>
          </h1>
          <p className="text-gray-600 mt-1">
            Total: {totalElements} SKU
          </p>
        </div>

        <Link
          to="/admin/inventory/low-stock"
          className="btn-primary inline-flex items-center gap-2 flex-shrink-0"
        >
          <AlertTriangle className="w-5 h-5" />
          Low Stock ({lowStockCount})
        </Link>
      </div>

      {/* Error */}
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg p-4">
          {error}
        </div>
      )}

      {/* Table */}
      {inventories.length === 0 ? (
        <div className="bg-white rounded-xl shadow-sm p-8 text-center">
          <Package className="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <p className="text-gray-600">No inventory data</p>
        </div>
      ) : (
        <div className="bg-white rounded-xl shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 border-b">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    SKU
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Book
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Format
                  </th>
                  <th
                    className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase cursor-pointer hover:bg-gray-100"
                    onClick={() => handleSort('totalStock')}
                  >
                    Total Stock {getSortIcon('totalStock')}
                  </th>
                  <th
                    className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase cursor-pointer hover:bg-gray-100"
                    onClick={() => handleSort('reservedStock')}
                  >
                    Reserved {getSortIcon('reservedStock')}
                  </th>
                  <th
                    className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase cursor-pointer hover:bg-gray-100"
                    onClick={() => handleSort('availableStock')}
                  >
                    Available {getSortIcon('availableStock')}
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Status
                  </th>
                  <th
                    className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase cursor-pointer hover:bg-gray-100"
                    onClick={() => handleSort('lastUpdated')}
                  >
                    Updated {getSortIcon('lastUpdated')}
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {inventories.map((inv) => (
                  <tr key={inv.skuId} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-mono text-gray-800">
                      {inv. sku}
                    </td>
                    <td className="px-6 py-4">
                      <Link
                        to={`/admin/books/${inv.bookId}`}
                        className="text-sm font-medium text-blue-600 hover:text-blue-700 line-clamp-2"
                      >
                        {inv.bookTitle}
                      </Link>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                      {inv.format}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-semibold text-gray-800">
                      {inv.totalStock}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-orange-600 font-medium">
                      {inv.reservedStock}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-green-600 font-semibold">
                      {inv.availableStock}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      {getStatusBadge(inv. status)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                      {inv.lastUpdated
                        ? new Date(inv.lastUpdated).toLocaleString('vi-VN')
                        : '—'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right">
                      <button
                        onClick={() => handleUpdateStock(inv)}
                        className="text-blue-600 hover:text-blue-700 inline-flex items-center gap-1 text-sm font-medium"
                        title="Update stock"
                      >
                        <Edit className="w-4 h-4" />
                        Update
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="bg-gray-50 px-6 py-4 flex items-center justify-between border-t">
              <div className="text-sm text-gray-700">
                Page {page + 1} / {totalPages}
              </div>

              <div className="flex gap-2">
                <button
                  onClick={() => setPage(0)}
                  disabled={page === 0}
                  className="btn-secondary px-3 py-1 text-sm disabled:opacity-50"
                >
                  First
                </button>
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="btn-secondary px-3 py-1 text-sm disabled:opacity-50"
                >
                  Previous
                </button>
                <button
                  onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1}
                  className="btn-secondary px-3 py-1 text-sm disabled:opacity-50"
                >
                  Next
                </button>
                <button
                  onClick={() => setPage(totalPages - 1)}
                  disabled={page >= totalPages - 1}
                  className="btn-secondary px-3 py-1 text-sm disabled:opacity-50"
                >
                  Last
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Update Stock Modal */}
      <UpdateStockModal
        isOpen={isUpdateModalOpen}
        onClose={() => setIsUpdateModalOpen(false)}
        onSubmit={handleStockSubmit}
        inventory={selectedInventory}
      />
    </div>
  );
}

export default AdminInventoryPage;