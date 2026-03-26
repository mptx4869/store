import { Book, Facebook, Mail, MapPin, Phone } from 'lucide-react';

function Footer() {
  return (
    <footer className="bg-gray-800 text-gray-300">
      {/* Main footer */}
      <div className="container mx-auto px-4 py-12">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
          {/* About */}
          <div>
            <div className="flex items-center gap-2 mb-4">
              <Book className="w-8 h-8 text-blue-400" />
              <span className="text-2xl font-bold text-white">
                Book<span className="text-blue-400">Store</span>
              </span>
            </div>
            <p className="text-gray-400 mb-4">
              Vietnam's leading online book sales platform with thousands of 
              diverse book titles.
            </p>
            <div className="flex gap-4">
              <a 
                href="#" 
                className="w-10 h-10 bg-gray-700 rounded-full flex items-center 
                         justify-center hover:bg-blue-600 transition-colors"
              >
                <Facebook className="w-5 h-5" />
              </a>
              <a 
                href="#" 
                className="w-10 h-10 bg-gray-700 rounded-full flex items-center 
                         justify-center hover:bg-blue-600 transition-colors"
              >
                <Mail className="w-5 h-5" />
              </a>
            </div>
          </div>

          {/* Quick links */}
          <div>
            <h3 className="text-white font-semibold text-lg mb-4">Quick Links</h3>
            <ul className="space-y-2">
              <li>
                <a href="/" className="hover:text-blue-400 transition-colors">
                  Home
                </a>
              </li>
              <li>
                <a href="/books" className="hover:text-blue-400 transition-colors">
                  New Books
                </a>
              </li>
              <li>
                <a href="/bestsellers" className="hover:text-blue-400 transition-colors">
                  Bestsellers
                </a>
              </li>
              <li>
                <a href="/categories" className="hover:text-blue-400 transition-colors">
                  Categories
                </a>
              </li>
              <li>
                <a href="/promotions" className="hover:text-blue-400 transition-colors">
                  Promotions
                </a>
              </li>
            </ul>
          </div>

          {/* Support */}
          <div>
            <h3 className="text-white font-semibold text-lg mb-4">Support</h3>
            <ul className="space-y-2">
              <li>
                <a href="/shipping" className="hover:text-blue-400 transition-colors">
                  Shipping Policy
                </a>
              </li>
              <li>
                <a href="/return" className="hover:text-blue-400 transition-colors">
                  Return Policy
                </a>
              </li>
              <li>
                <a href="/payment" className="hover:text-blue-400 transition-colors">
                  Payment Methods
                </a>
              </li>
              <li>
                <a href="/faq" className="hover:text-blue-400 transition-colors">
                  FAQ
                </a>
              </li>
              <li>
                <a href="/contact" className="hover:text-blue-400 transition-colors">
                  Contact
                </a>
              </li>
            </ul>
          </div>

          {/* Contact */}
          <div>
            <h3 className="text-white font-semibold text-lg mb-4">Contact</h3>
            <ul className="space-y-3">
              <li className="flex items-start gap-3">
                <MapPin className="w-5 h-5 text-blue-400 flex-shrink-0 mt-0. 5" />
                <span>123 ABC Street, District 1, Ho Chi Minh City</span>
              </li>
              <li className="flex items-center gap-3">
                <Phone className="w-5 h-5 text-blue-400 flex-shrink-0" />
                <span>1234 5678 90</span>
              </li>
              <li className="flex items-center gap-3">
                <Mail className="w-5 h-5 text-blue-400 flex-shrink-0" />
                <span>ITITIU21205@student.hcmiu.edu.vn</span>
              </li>
            </ul>
          </div>
        </div>
      </div>

      {/* Bottom footer */}
      <div className="border-t border-gray-700">
        <div className="container mx-auto px-4 py-6">
          <div className="flex flex-col md:flex-row justify-between items-center gap-4">
            <p className="text-gray-400 text-sm text-center md:text-left">
              © 2025 BookStore. All rights reserved.
            </p>
            <div className="flex gap-6 text-sm">
              <a href="/privacy" className="text-gray-400 hover:text-blue-400 
                                           transition-colors">
                Privacy Policy
              </a>
              <a href="/terms" className="text-gray-400 hover:text-blue-400 
                                         transition-colors">
                Terms of Service
              </a>
            </div>
          </div>
        </div>
      </div>
    </footer>
  );
}

export default Footer;