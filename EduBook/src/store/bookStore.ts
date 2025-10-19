import { create } from 'zustand';
import { BookStoreResponse } from '../types';
import { apiService } from '../services/api';

interface BookState {
  // State
  books: BookStoreResponse[];
  filteredBooks: BookStoreResponse[];
  isLoading: boolean;
  error: string | null;
  searchQuery: string;
  filters: {
    gradeLevel: string | null;
    fieldType: string | null;
    examType: string | null;
    lessonType: string | null;
    priceRange: {
      min: number | null;
      max: number | null;
    };
  };

  // Actions
  fetchBooks: () => Promise<void>;
  setSearchQuery: (query: string) => void;
  setFilter: (key: keyof BookState['filters'], value: any) => void;
  resetFilters: () => void;
  resetError: () => void;
}

export const useBookStore = create<BookState>((set, get) => ({
  // Initial state
  books: [],
  filteredBooks: [],
  isLoading: false,
  error: null,
  searchQuery: '',
  filters: {
    gradeLevel: null,
    fieldType: null,
    examType: null,
    lessonType: null,
    priceRange: {
      min: null,
      max: null,
    },
  },

  // Actions
  fetchBooks: async () => {
    set({ isLoading: true, error: null });
    try {
      const books = await apiService.getBookStore();
      set({ 
        books,
        filteredBooks: books // Başlangıçta tüm kitapları göster
      });
    } catch (error) {
      set({ error: error instanceof Error ? error.message : 'Kitaplar yüklenirken bir hata oluştu' });
    } finally {
      set({ isLoading: false });
    }
  },

  setSearchQuery: (query: string) => {
    set({ searchQuery: query });
    const { books, filters } = get();
    
    // Arama ve filtreleri uygula
    let filtered = [...books];

    // Arama
    if (query) {
      const searchLower = query.toLowerCase();
      filtered = filtered.filter(book => 
        book.title.toLowerCase().includes(searchLower) ||
        book.publisher.toLowerCase().includes(searchLower)
      );
    }

    // Filtreleri uygula
    if (filters.gradeLevel) {
      filtered = filtered.filter(book => book.gradeLevel === filters.gradeLevel);
    }
    if (filters.fieldType) {
      filtered = filtered.filter(book => book.fieldType === filters.fieldType);
    }
    if (filters.examType) {
      filtered = filtered.filter(book => book.examType === filters.examType);
    }
    if (filters.lessonType) {
      filtered = filtered.filter(book => book.lessonType === filters.lessonType);
    }
    if (filters.priceRange.min !== null) {
      filtered = filtered.filter(book => book.price >= filters.priceRange.min!);
    }
    if (filters.priceRange.max !== null) {
      filtered = filtered.filter(book => book.price <= filters.priceRange.max!);
    }

    set({ filteredBooks: filtered });
  },

  setFilter: (key: keyof BookState['filters'], value: any) => {
    set(state => ({
      filters: {
        ...state.filters,
        [key]: value
      }
    }));
    // Filtreleri uygulamak için mevcut aramayı tekrar çalıştır
    get().setSearchQuery(get().searchQuery);
  },

  resetFilters: () => {
    set(state => ({
      filters: {
        gradeLevel: null,
        fieldType: null,
        examType: null,
        lessonType: null,
        priceRange: {
          min: null,
          max: null,
        },
      },
      filteredBooks: state.books // Tüm kitapları göster
    }));
  },

  resetError: () => set({ error: null }),
}));
