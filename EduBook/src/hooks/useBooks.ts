import { useState, useEffect, useCallback } from 'react';
import { Book } from '../types';
import { bookService } from '../services/bookService';
import { useAuthStore } from '../store/authStore';

interface UseBooksReturn {
  books: Book[];
  loading: boolean;
  error: string | null;
  refreshBooks: () => Promise<void>;
  useBookCode: (code: string) => Promise<{ success: boolean; message: string }>;
  addBook: (bookData: {
    title: string;
    publisher: string;
    coverImageUrl?: string;
    paid: boolean;
    topics?: string[];
  }) => Promise<{ success: boolean; message: string; book?: Book }>;
  updateBook: (bookId: number, bookData: {
    title: string;
    publisher: string;
    coverImageUrl?: string;
    paid: boolean;
    topics?: string[];
  }) => Promise<{ success: boolean; message: string; book?: Book }>;
  deleteBook: (bookId: number) => Promise<{ success: boolean; message: string }>;
  searchBooks: (searchTerm: string) => Book[];
  filterBooksByType: (paid: boolean) => Book[];
  sortBooks: (sortBy: 'title' | 'publisher' | 'paid') => Book[];
}

export const useBooks = (): UseBooksReturn => {
  const [books, setBooks] = useState<Book[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { user } = useAuthStore();

  // Fetch books for the current user
  const fetchBooks = useCallback(async () => {
    if (!user?.id) {
      setBooks([]);
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const userBooks = await bookService.getBooksForUser(user.id);
      setBooks(userBooks);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to fetch books';
      setError(errorMessage);
      console.error('Error fetching books:', err);
    } finally {
      setLoading(false);
    }
  }, [user?.id]);

  // Refresh books
  const refreshBooks = useCallback(async () => {
    await fetchBooks();
  }, [fetchBooks]);

  // Use book code
  const useBookCode = useCallback(async (code: string): Promise<{ success: boolean; message: string }> => {
    if (!user?.id) {
      return { success: false, message: 'User not authenticated' };
    }

    // Validate code format
    const validation = bookService.validateBookCode(code);
    if (!validation.isValid) {
      return { success: false, message: validation.message || 'Invalid book code format' };
    }

    setLoading(true);
    setError(null);

    try {
      const result = await bookService.useBookCode(user.id, code);
      // Refresh books after successful code usage
      await fetchBooks();
      return { success: true, message: result };
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to use book code';
      setError(errorMessage);
      return { success: false, message: errorMessage };
    } finally {
      setLoading(false);
    }
  }, [user?.id, fetchBooks]);

  // Add book
  const addBook = useCallback(async (bookData: {
    title: string;
    publisher: string;
    coverImageUrl?: string;
    paid: boolean;
    topics?: string[];
  }): Promise<{ success: boolean; message: string; book?: Book }> => {
    // Validate book data
    const validation = bookService.validateBookData(bookData);
    if (!validation.isValid) {
      return { 
        success: false, 
        message: `Validation failed: ${validation.errors.join(', ')}` 
      };
    }

    setLoading(true);
    setError(null);

    try {
      const newBook = await bookService.addBook(bookData);
      // Refresh books after successful addition
      await fetchBooks();
      return { success: true, message: 'Book added successfully', book: newBook };
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to add book';
      setError(errorMessage);
      return { success: false, message: errorMessage };
    } finally {
      setLoading(false);
    }
  }, [fetchBooks]);

  // Update book
  const updateBook = useCallback(async (bookId: number, bookData: {
    title: string;
    publisher: string;
    coverImageUrl?: string;
    paid: boolean;
    topics?: string[];
  }): Promise<{ success: boolean; message: string; book?: Book }> => {
    // Validate book data
    const validation = bookService.validateBookData(bookData);
    if (!validation.isValid) {
      return { 
        success: false, 
        message: `Validation failed: ${validation.errors.join(', ')}` 
      };
    }

    setLoading(true);
    setError(null);

    try {
      const updatedBook = await bookService.updateBook(bookId, bookData);
      // Refresh books after successful update
      await fetchBooks();
      return { success: true, message: 'Book updated successfully', book: updatedBook };
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to update book';
      setError(errorMessage);
      return { success: false, message: errorMessage };
    } finally {
      setLoading(false);
    }
  }, [fetchBooks]);

  // Delete book
  const deleteBook = useCallback(async (bookId: number): Promise<{ success: boolean; message: string }> => {
    setLoading(true);
    setError(null);

    try {
      await bookService.deleteBook(bookId);
      // Refresh books after successful deletion
      await fetchBooks();
      return { success: true, message: 'Book deleted successfully' };
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to delete book';
      setError(errorMessage);
      return { success: false, message: errorMessage };
    } finally {
      setLoading(false);
    }
  }, [fetchBooks]);

  // Search books
  const searchBooks = useCallback((searchTerm: string): Book[] => {
    return bookService.searchBooks(books, searchTerm);
  }, [books]);

  // Filter books by type
  const filterBooksByType = useCallback((paid: boolean): Book[] => {
    return bookService.filterBooksByType(books, paid);
  }, [books]);

  // Sort books
  const sortBooks = useCallback((sortBy: 'title' | 'publisher' | 'paid'): Book[] => {
    return bookService.sortBooks(books, sortBy);
  }, [books]);

  // Fetch books on mount and when user changes
  useEffect(() => {
    fetchBooks();
  }, [fetchBooks]);

  return {
    books,
    loading,
    error,
    refreshBooks,
    useBookCode,
    addBook,
    updateBook,
    deleteBook,
    searchBooks,
    filterBooksByType,
    sortBooks,
  };
};
