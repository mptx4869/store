import { Image as ImageIcon } from 'lucide-react';

function ImageDisplay({ imageUrl, alt = 'Book image', size = 'small' }) {
  const sizeClasses = {
    small: 'w-16 h-20',      // thumbnail in table
    medium: 'w-32 h-40',     // card
    large: 'w-full h-auto',  // detail page
  };

  if (! imageUrl) {
    return (
      <div
        className={`${sizeClasses[size]} bg-gray-100 rounded-lg flex items-center justify-center`}
      >
        <ImageIcon className="w-6 h-6 text-gray-400" />
      </div>
    );
  }

  return (
    <img
      src={imageUrl}
      alt={alt}
      className={`${sizeClasses[size]} object-cover rounded-lg`}
      onError={(e) => {
        // Fallback if image fails to load
        e. target.style.display = 'none';
        e.target.parentElement.innerHTML = `
          <div class="${sizeClasses[size]} bg-gray-100 rounded-lg flex items-center justify-center">
            <svg class="w-6 h-6 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h. 01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
          </div>
        `;
      }}
    />
  );
}

export default ImageDisplay;