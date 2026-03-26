import { Star } from 'lucide-react';

function Rating({ rating, reviewCount, showCount = true, size = 'sm' }) {
  const sizes = {
    sm: 'w-4 h-4',
    md: 'w-5 h-5',
    lg: 'w-6 h-6'
  };

  const textSizes = {
    sm: 'text-xs',
    md: 'text-sm',
    lg: 'text-base'
  };

  // Tạo mảng 5 sao
  const stars = Array.from({ length: 5 }, (_, index) => {
    const fillPercentage = Math.min(Math.max(rating - index, 0), 1) * 100;
    return fillPercentage;
  });

  return (
    <div className="flex items-center gap-1">
      <div className="flex">
        {stars. map((fill, index) => (
          <div key={index} className="relative">
            {/* Empty star (background) */}
            <Star className={`${sizes[size]} text-gray-300`} />
            {/* Filled star (overlay) */}
            <div
              className="absolute inset-0 overflow-hidden"
              style={{ width: `${fill}%` }}
            >
              <Star className={`${sizes[size]} text-yellow-400 fill-yellow-400`} />
            </div>
          </div>
        ))}
      </div>

      {showCount && reviewCount !== undefined && (
        <span className={`${textSizes[size]} text-gray-500 ml-1`}>
          ({reviewCount. toLocaleString()})
        </span>
      )}
    </div>
  );
}

export default Rating;