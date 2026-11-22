import { Navigate, Route, Routes } from 'react-router-dom'
import Navbar from './components/Navbar'
import Guard from './components/Guard'
import Landing from './pages/Landing'
import Auth from './pages/Auth'
import Marketplace from './pages/Marketplace'
import RetailerOrders from './pages/RetailerOrders'
import FarmerDashboard from './pages/FarmerDashboard'
import IncomingOrders from './pages/IncomingOrders'
import AdminDashboard from './pages/AdminDashboard'

// Same URLs as the original FarmLink app.
export default function App() {
  return (
    <>
      <Navbar />
      <Routes>
        <Route path="/" element={<Landing />} />
        <Route path="/auth" element={<Auth />} />
        <Route path="/marketplace" element={<Marketplace />} />
        <Route path="/retailer/orders" element={<Guard role="RETAILER"><RetailerOrders /></Guard>} />
        <Route path="/farmer/dashboard" element={<Guard role="FARMER"><FarmerDashboard /></Guard>} />
        <Route path="/farmer/incoming-orders" element={<Guard role="FARMER"><IncomingOrders /></Guard>} />
        <Route path="/admin" element={<Guard role="ADMIN"><AdminDashboard /></Guard>} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </>
  )
}
