import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import adminInventoryService from '../../services/adminInventoryService';
import UpdateStockModal from '../../components/admin/UpdateStockModal';
import { useToast } from '../../context/ToastContext';
import { AlertTriangle, ArrowLeft, Edit, Package } from 'lucide-react';

function AdminLowStockPage() {
  const toast = useToast();

  const [inventories, setInventories] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  // Update stock modal
  const [isUpdateModalOpen, setIsUpdateModalOpen] = useState(false);
  const [selectedInventory, setSelectedInventory] = useState(null);

  useEffect(() => {
    loadLowStock();
  }, []);

  const loadLowStock = async () => {
    setIsLoading(true);
    setError('');
    try {
      const result = await adminInventoryService.getLowStock();
      setInventories(result);
    } catch (err) {
      setError(err.message || 'Could not load low stock list');
      toast.error(err. message);
    } finally {
      setIsLoading(false);
    }
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
      loadLowStock();
    } catch (err) {
      toast.error(err.message || 'Could not update stock');
      throw err;
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-600">Loading low stock list...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Back button */}
      <Link
        to="/admin/inventory"
        className="inline-flex items-center gap-2 text-blue-600 hover:text-blue-700"
      >
        <ArrowLeft className="w-5 h-5" />
        Back to inventory list
      </Link>

      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-800 flex items-center gap-3">
            <AlertTriangle className="w-8 h-8 text-yellow-600" />
            Low Stock
          </h1>
          <p className="text-gray-600 mt-1">
            {inventories.length} SKU need restocking
          </p>
        </div>
      </div>

      {/* Alert Banner */}
      {inventories.length > 0 && (
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 flex items-start gap-3">
          <AlertTriangle className="w-5 h-5 text-yellow-600 flex-shrink-0 mt-0.5" />
          <div className="flex-1">
            <p className="font-semibold text-yellow-800">Low stock warning</p>
            <p className="text-sm text-yellow-700 mt-1">
              There are {inventories.length} SKUs at low stock level (below 10 products). 
              Please restock to avoid running out.
            </p>
          </div>
        </div>
      )}

      {/* Error */}
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg p-4">
          {error}
        </div>
      )}

      {/* Table */}
      {inventories.length === 0 ? (
        <div className="bg-white rounded-xl shadow-sm p-8 text-center">
          <Package className="w-16 h-16 text-green-300 mx-auto mb-4" />
          <p className="text-lg font-semibold text-gray-800 mb-2">
            Great! No SKUs running low
          </p>
          <p className="text-gray-600">
            All SKUs have sufficient stock
          </p>
          <Link
            to="/admin/inventory"
            className="btn-primary inline-flex items-center gap-2 mt-4"
          >
            <ArrowLeft className="w-4 h-4" />
            Back to inventory list
          </Link>
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
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Total Stock
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Reserved
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Available
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Status
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Updated
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {inventories.map((inv) => (
                  <tr 
                    key={inv.skuId} 
                    className={`hover:bg-gray-50 ${
                      inv.status === 'OUT_OF_STOCK' ? 'bg-red-50' :  
                      inv.availableStock <= 5 ? 'bg-yellow-50' : ''
                    }`}
                  >
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-mono text-gray-800">
                      {inv. sku}
                    </td>
                    <td className="px-6 py-4">
                      <Link
                        to={`/admin/books/${inv.bookId}`}
                        className="text-sm font-medium text-blue-600 hover:text-blue-700 line-clamp-2"
                      >
                        {inv. bookTitle}
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
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`text-sm font-bold ${
                        inv.availableStock === 0 ? 'text-red-600' : 
                        inv.availableStock <= 5 ? 'text-yellow-600' :
                        'text-green-600'
                      }`}>
                        {inv.availableStock}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      {getStatusBadge(inv.status)}
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
                        title="Restock"
                      >
                        <Edit className="w-4 h-4" />
                        Restock
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Summary */}
          <div className="bg-gray-50 px-6 py-4 border-t">
            <div className="flex items-center justify-between text-sm">
              <span className="text-gray-600">
                Total: <strong>{inventories.length}</strong> SKUs need restocking
              </span>
              <span className="text-gray-600">
                Out of stock: <strong className="text-red-600">
                  {inventories.filter(inv => inv.status === 'OUT_OF_STOCK').length}
                </strong>
              </span>
            </div>
          </div>
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

export default AdminLowStockPage;