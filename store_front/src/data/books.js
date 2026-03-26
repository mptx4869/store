export const books = [
  {
    id: 1,
    title: "How to Win Friends and Influence People",
    author: "Dale Carnegie",
    price: 86000,
    originalPrice: 108000,
    image: "https://picsum.photos/seed/book1/300/400",
    rating: 4.8,
    reviewCount: 2345,
    category: "Life Skills",
    isNew: false,
    isBestSeller: true,
    discount: 20,
    description: "The most famous, best-selling and influential book of all time.",
    stock: 150
  },
  {
    id: 2,
    title: "The Alchemist",
    author: "Paulo Coelho",
    price: 69000,
    originalPrice: 79000,
    image: "https://picsum.photos/seed/book2/300/400",
    rating: 4.7,
    reviewCount: 1876,
    category: "Novel",
    isNew: false,
    isBestSeller: true,
    discount: 13,
    description: "A novel first published in Brazil in 1988, has been translated into 67 languages.",
    stock: 200
  },
  {
    id: 3,
    title: "Atomic Habits",
    author: "James Clear",
    price: 150000,
    originalPrice: 199000,
    image: "https://picsum.photos/seed/book3/300/400",
    rating: 4.9,
    reviewCount: 3210,
    category: "Life Skills",
    isNew: true,
    isBestSeller: true,
    discount: 25,
    description: "An easy and proven way to build good habits and break bad ones.",
    stock: 85
  },
  {
    id: 4,
    title: "I See Yellow Flowers on the Green Grass",
    author: "Nguyen Nhat Anh",
    price: 125000,
    originalPrice: 150000,
    image: "https://picsum.photos/seed/book4/300/400",
    rating: 4.6,
    reviewCount: 987,
    category: "Vietnamese Literature",
    isNew: false,
    isBestSeller: false,
    discount: 17,
    description: "A book about a poor childhood but full of beautiful memories.",
    stock: 120
  },
  {
    id: 5,
    title: "Clean Code",
    author: "Robert C.  Martin",
    price: 450000,
    originalPrice: 550000,
    image: "https://picsum.photos/seed/book5/300/400",
    rating: 4.8,
    reviewCount: 1543,
    category: "Information Technology",
    isNew: true,
    isBestSeller: false,
    discount: 18,
    description: "A guide to writing clean, readable, and maintainable code for programmers.",
    stock: 45
  },
  {
    id: 6,
    title: "Sapiens: A Brief History of Humankind",
    author: "Yuval Noah Harari",
    price: 189000,
    originalPrice: 230000,
    image: "https://picsum.photos/seed/book6/300/400",
    rating: 4.7,
    reviewCount: 2100,
    category: "History",
    isNew: false,
    isBestSeller: true,
    discount: 18,
    description: "A book about human history from the Stone Age to the 21st century.",
    stock: 95
  },
  {
    id: 7,
    title: "How Much Is Your Youth Worth",
    author: "Rosie Nguyen",
    price: 70000,
    originalPrice: 90000,
    image: "https://picsum.photos/seed/book7/300/400",
    rating: 4.3,
    reviewCount: 654,
    category: "Life Skills",
    isNew: true,
    isBestSeller: false,
    discount: 22,
    description: "Insights and perspectives on youth, living and working meaningfully.",
    stock: 180
  },
  {
    id: 8,
    title: "Kane and Abel",
    author: "Jeffrey Archer",
    price: 155000,
    originalPrice: 185000,
    image: "https://picsum.photos/seed/book8/300/400",
    rating: 4.5,
    reviewCount: 876,
    category: "Novel",
    isNew: false,
    isBestSeller: false,
    discount: 16,
    description: "A story about two men born on the same day but with completely different fates.",
    stock: 65
  },
  {
    id: 9,
    title: "Thinking, Fast and Slow",
    author: "Daniel Kahneman",
    price: 250000,
    originalPrice: 320000,
    image: "https://picsum.photos/seed/book9/300/400",
    rating: 4.6,
    reviewCount: 1432,
    category: "Psychology",
    isNew: true,
    isBestSeller: true,
    discount: 22,
    description: "Explore the two thinking systems that govern how we think and make decisions.",
    stock: 55
  },
  {
    id: 10,
    title: "Doraemon - Volume 1",
    author: "Fujiko F. Fujio",
    price: 20000,
    originalPrice: 25000,
    image: "https://picsum.photos/seed/book10/300/400",
    rating: 4.9,
    reviewCount: 5678,
    category: "Comic",
    isNew: false,
    isBestSeller: true,
    discount: 20,
    description: "The famous comic series about the robot cat from the future.",
    stock: 500
  },
  {
    id: 11,
    title: "The Psychology of Money",
    author: "Morgan Housel",
    price: 168000,
    originalPrice: 210000,
    image: "https://picsum.photos/seed/book11/300/400",
    rating: 4.8,
    reviewCount: 2890,
    category: "Finance",
    isNew: true,
    isBestSeller: true,
    discount: 20,
    description: "Lessons about money, greed and happiness.",
    stock: 110
  },
  {
    id: 12,
    title: "The Rainbow Troops",
    author: "Andrea Hirata",
    price: 95000,
    originalPrice: 115000,
    image: "https://picsum.photos/seed/book12/300/400",
    rating: 4.4,
    reviewCount: 543,
    category: "Novel",
    isNew: false,
    isBestSeller: false,
    discount: 17,
    description: "A touching story about poor children and their dream of going to school.",
    stock: 75
  }
];

// Get new books
export const getNewBooks = () => {
  return books.filter(book => book. isNew);
};

// Get best sellers
export const getBestSellers = () => {
  return books.filter(book => book. isBestSeller);
};

// Get books by category
export const getBooksByCategory = (category) => {
  return books.filter(book => book.category === category);
};

// Get book by ID
export const getBookById = (id) => {
  return books.find(book => book.id === parseInt(id));
};

// Search books
export const searchBooks = (query) => {
  const lowerQuery = query. toLowerCase();
  return books.filter(book =>
    book.title.toLowerCase().includes(lowerQuery) ||
    book.author.toLowerCase().includes(lowerQuery) ||
    book.category.toLowerCase().includes(lowerQuery)
  );
};

// Get category list
export const getCategories = () => {
  const categories = [... new Set(books. map(book => book.category))];
  return categories;
};