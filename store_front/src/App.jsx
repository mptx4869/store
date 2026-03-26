import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Layout } from './components/layout';
import { ProtectedRoute, GuestRoute, AuthEventListener } from './components/common';

import AdminRoute from './components/common/AdminRoute';
import AdminLayout from './components/layout/AdminLayout';

import AdminCategoriesPage from './pages/admin/AdminCategoriesPage';
import AdminCategoryDetailPage from './pages/admin/AdminCategoryDetailPage';
import AdminCategoryFormPage from './pages/admin/AdminCategoryFormPage';
import AdminInventoryPage from './pages/admin/AdminInventoryPage';
import AdminLowStockPage from './pages/admin/AdminLowStockPage';
import AdminOrdersPage from './pages/admin/AdminOrdersPage';
import AdminOrderDetailPage from './pages/admin/AdminOrderDetailPage';

import { AuthProvider } from './context/AuthContext';
import { CartProvider } from './context/CartContext';
import { ToastProvider } from './context/ToastContext';

import ToastContainer from './components/common/ToastContainer';

import {
  HomePage,
  BooksPage,
  BookDetailPage,
  CartPage,
  LoginPage,
  RegisterPage,
  OrdersPage,
  OrderDetailPage,
  AdminDashboardPage,
  AdminUsersPage,
  AdminUserDetailPage,
  AdminBooksPage,
  AdminBookDetailPage,
  AdminBookCreatePage,
  AdminBookEditPage,
  AdminReportsPage,
  ChangePasswordPage,
} from './pages';

function App() {
  return (
    <BrowserRouter>
      <ToastProvider>
        <AuthProvider>
          <CartProvider>
            <ToastContainer />
            <AuthEventListener />

            <Routes>
              {/* Public Routes */}
              <Route
                path="/"
                element={
                  <Layout>
                    <HomePage />
                  </Layout>
                }
              />
              <Route
                path="/books"
                element={
                  <Layout>
                    <BooksPage />
                  </Layout>
                }
              />
              <Route
                path="/books/:id"
                element={
                  <Layout>
                    <BookDetailPage />
                  </Layout>
                }
              />
              <Route
                path="/bestsellers"
                element={
                  <Layout>
                    <BooksPage />
                  </Layout>
                }
              />

              {/* Protected Routes */}
               <Route
                 path="/settings/change-password"
                 element={
                   <ProtectedRoute>
                     <Layout>
                       <ChangePasswordPage />
                     </Layout>
                   </ProtectedRoute>
                 }
               />
              <Route
                path="/cart"
                element={
                  <ProtectedRoute>
                    <Layout>
                      <CartPage />
                    </Layout>
                  </ProtectedRoute>
                }
              />
              <Route
                path="/orders"
                element={
                  <ProtectedRoute>
                    <Layout>
                      <OrdersPage />
                    </Layout>
                  </ProtectedRoute>
                }
              />
              <Route
                path="/orders/:orderId"
                element={
                  <ProtectedRoute>
                    <Layout>
                      <OrderDetailPage />
                    </Layout>
                  </ProtectedRoute>
                }
              />

              {/* Admin Routes */}
              <Route
                path="/admin"
                element={
                  <AdminRoute>
                    <Layout>
                      <AdminLayout>
                        <AdminDashboardPage />
                      </AdminLayout>
                    </Layout>
                  </AdminRoute>
                }
              />

              <Route
                path="/admin/users"
                element={
                  <AdminRoute>
                    <Layout>
                      <AdminLayout>
                        <AdminUsersPage />
                      </AdminLayout>
                    </Layout>
                  </AdminRoute>
                }
              />
              <Route
                path="/admin/users/:userId"
                element={
                  <AdminRoute>
                    <Layout>
                      <AdminLayout>
                        <AdminUserDetailPage />
                      </AdminLayout>
                    </Layout>
                  </AdminRoute>
                }
              />

              <Route
                path="/admin/books"
                element={
                  <AdminRoute>
                    <Layout>
                      <AdminLayout>
                        <AdminBooksPage />
                      </AdminLayout>
                    </Layout>
                  </AdminRoute>
                }
              />
              <Route
                path="/admin/books/new"
                element={
                  <AdminRoute>
                    <Layout>
                      <AdminLayout>
                        <AdminBookCreatePage />
                      </AdminLayout>
                    </Layout>
                  </AdminRoute>
                }
              />
              <Route
                path="/admin/books/:bookId"
                element={
                  <AdminRoute>
                    <Layout>
                      <AdminLayout>
                        <AdminBookDetailPage />
                      </AdminLayout>
                    </Layout>
                  </AdminRoute>
                }
              />
              <Route
                path="/admin/books/:bookId/edit"
                element={
                  <AdminRoute>
                    <Layout>
                      <AdminLayout>
                        <AdminBookEditPage />
                      </AdminLayout>
                    </Layout>
                  </AdminRoute>
                }
              />
               {/* Admin Routes - Dashboard */}
              <Route
                path="/admin"
                element={
                  <AdminRoute>
                    <AdminLayout>
                      <AdminDashboardPage />
                    </AdminLayout>
                  </AdminRoute>
                }
              />
              <Route
                path="/admin/reports"
                element={
                  <AdminRoute>
                    <AdminLayout>
                      <AdminReportsPage />
                    </AdminLayout>
                  </AdminRoute>
                }
              />

              {/* Admin Routes - Users */}
              <Route
                path="/admin/users"
                element={
                  <AdminRoute>
                    <AdminLayout>
                      <AdminUsersPage />
                    </AdminLayout>
                  </AdminRoute>
                }
              />

              {/* Admin Routes - Books */}
              <Route
                path="/admin/books"
                element={
                  <AdminRoute>
                    <AdminLayout>
                      <AdminBooksPage />
                    </AdminLayout>
                  </AdminRoute>
                }
              />
              <Route
                path="/admin/books/new"
                element={
                  <AdminRoute>
                    <AdminLayout>
                      <AdminBookCreatePage />
                    </AdminLayout>
                  </AdminRoute>
                }
              />
              <Route
                path="/admin/books/:bookId/edit"
                element={
                  <AdminRoute>
                    <AdminLayout>
                      <AdminBookEditPage />
                    </AdminLayout>
                  </AdminRoute>
                }
              />
              <Route
                path="/admin/books/:bookId"
                element={
                  <AdminRoute>
                    <AdminLayout>
                      <AdminBookDetailPage />
                    </AdminLayout>
                  </AdminRoute>
                }
              />

              {/* Admin Routes - Categories (THỨ TỰ QUAN TRỌNG) */}
              <Route
                path="/admin/categories"
                element={
                  <AdminRoute>
                    <AdminLayout>
                      <AdminCategoriesPage />
                    </AdminLayout>
                  </AdminRoute>
                }
              />
              <Route
                path="/admin/categories/new"
                element={
                  <AdminRoute>
                    <AdminLayout>
                      <AdminCategoryFormPage />
                    </AdminLayout>
                  </AdminRoute>
                }
              />
              <Route
                path="/admin/categories/:categoryId/edit"
                element={
                  <AdminRoute>
                    <AdminLayout>
                      <AdminCategoryFormPage />
                    </AdminLayout>
                  </AdminRoute>
                }
              />
              <Route
                path="/admin/categories/:categoryId"
                element={
                  <AdminRoute>
                    <AdminLayout>
                      <AdminCategoryDetailPage />
                    </AdminLayout>
                  </AdminRoute>
                }
              />
              {/* Admin Routes - Inventory */}
              <Route
                path="/admin/inventory"
                element={
                  <AdminRoute>
                    <AdminLayout>
                      <AdminInventoryPage />
                    </AdminLayout>
                  </AdminRoute>
                }
              />
              <Route
                path="/admin/inventory/low-stock"
                element={
                  <AdminRoute>
                    <AdminLayout>
                      <AdminLowStockPage />
                    </AdminLayout>
                  </AdminRoute>
                }
              />
               {/* Admin Routes - Orders */}
              <Route
                path="/admin/orders"
                element={
                  <AdminRoute>
                    <AdminLayout>
                      <AdminOrdersPage />
                    </AdminLayout>
                  </AdminRoute>
                }
              />
              <Route
                path="/admin/orders/:orderId"
                element={
                  <AdminRoute>
                    <AdminLayout>
                      <AdminOrderDetailPage />
                    </AdminLayout>
                  </AdminRoute>
                }
              />
              {/* Guest Routes */}
              <Route
                path="/login"
                element={
                  <GuestRoute>
                    <Layout>
                      <LoginPage />
                    </Layout>
                  </GuestRoute>
                }
              />
              <Route
                path="/register"
                element={
                  <GuestRoute>
                    <Layout>
                      <RegisterPage />
                    </Layout>
                  </GuestRoute>
                }
              />

              {/* 404 */}
              <Route
                path="*"
                element={
                  <Layout>
                    <div className="container mx-auto px-4 py-16 text-center">
                      <div className="text-6xl mb-4">404</div>
                      <h1 className="text-2xl font-bold text-gray-800 mb-4">
                        Page Not Found
                      </h1>
                      <a href="/" className="btn-primary inline-block">
                        Back to Home
                      </a>
                    </div>
                  </Layout>
                }
              />
            </Routes>
          </CartProvider>
        </AuthProvider>
      </ToastProvider>
    </BrowserRouter>
  );
}

export default App;