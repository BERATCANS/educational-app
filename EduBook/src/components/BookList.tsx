import React, { useState, useMemo } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  Image,
  RefreshControl,
  Alert,
} from 'react-native';
import { Book } from '../types';
import { useBooks } from '../hooks/useBooks';
import { bookService } from '../services/bookService';

interface BookListProps {
  onBookPress?: (book: Book) => void;
  showAddButton?: boolean;
  onAddBook?: () => void;
}

export const BookList: React.FC<BookListProps> = ({
  onBookPress,
  showAddButton = false,
  onAddBook,
}) => {
  const { books, loading, error, refreshBooks, deleteBook } = useBooks();
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState<'all' | 'paid' | 'free'>('all');
  const [sortBy, setSortBy] = useState<'title' | 'publisher' | 'paid'>('title');

  // Filter and sort books
  const filteredBooks = useMemo(() => {
    let filtered = books;

    // Apply search filter
    if (searchTerm.trim()) {
      filtered = bookService.searchBooks(filtered, searchTerm);
    }

    // Apply type filter
    if (filterType !== 'all') {
      filtered = bookService.filterBooksByType(filtered, filterType === 'paid');
    }

    // Apply sorting
    filtered = bookService.sortBooks(filtered, sortBy);

    return filtered;
  }, [books, searchTerm, filterType, sortBy]);

  const handleDeleteBook = (book: Book) => {
    Alert.alert(
      'Delete Book',
      `Are you sure you want to delete "${book.title}"?`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: async () => {
            const result = await deleteBook(book.id);
            if (result.success) {
              Alert.alert('Success', result.message);
            } else {
              Alert.alert('Error', result.message);
            }
          },
        },
      ]
    );
  };

  const renderBookItem = ({ item: book }: { item: Book }) => (
    <TouchableOpacity
      style={styles.bookItem}
      onPress={() => onBookPress?.(book)}
      activeOpacity={0.7}
    >
      <View style={styles.bookContent}>
        <View style={styles.bookImageContainer}>
          {book.coverImageUrl ? (
            <Image source={{ uri: book.coverImageUrl }} style={styles.bookImage} />
          ) : (
            <View style={styles.placeholderImage}>
              <Text style={styles.placeholderText}>📚</Text>
            </View>
          )}
        </View>

        <View style={styles.bookInfo}>
          <Text style={styles.bookTitle} numberOfLines={2}>
            {book.title}
          </Text>
          <Text style={styles.bookPublisher} numberOfLines={1}>
            {book.publisher}
          </Text>
          
          <View style={styles.bookMeta}>
            <View style={[styles.badge, book.paid ? styles.paidBadge : styles.freeBadge]}>
              <Text style={[styles.badgeText, book.paid ? styles.paidText : styles.freeText]}>
                {book.paid ? 'Paid' : 'Free'}
              </Text>
            </View>
            
            {book.topics && book.topics.length > 0 && (
              <Text style={styles.topicCount}>
                {book.topics.length} topic{book.topics.length !== 1 ? 's' : ''}
              </Text>
            )}
          </View>
        </View>

        <View style={styles.bookActions}>
          <TouchableOpacity
            style={styles.actionButton}
            onPress={() => handleDeleteBook(book)}
          >
            <Text style={styles.actionButtonText}>🗑️</Text>
          </TouchableOpacity>
        </View>
      </View>
    </TouchableOpacity>
  );

  const renderEmptyState = () => (
    <View style={styles.emptyState}>
      <Text style={styles.emptyStateIcon}>📚</Text>
      <Text style={styles.emptyStateTitle}>No Books Found</Text>
      <Text style={styles.emptyStateText}>
        {searchTerm || filterType !== 'all'
          ? 'No books match your current filters'
          : 'You don\'t have any books yet. Add some using book codes!'}
      </Text>
    </View>
  );

  const renderHeader = () => (
    <View style={styles.header}>
      <Text style={styles.headerTitle}>Your Books ({filteredBooks.length})</Text>
      
      {/* Filter buttons */}
      <View style={styles.filterContainer}>
        {(['all', 'paid', 'free'] as const).map((type) => (
          <TouchableOpacity
            key={type}
            style={[
              styles.filterButton,
              filterType === type && styles.filterButtonActive,
            ]}
            onPress={() => setFilterType(type)}
          >
            <Text
              style={[
                styles.filterButtonText,
                filterType === type && styles.filterButtonTextActive,
              ]}
            >
              {type.charAt(0).toUpperCase() + type.slice(1)}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      {/* Sort buttons */}
      <View style={styles.sortContainer}>
        {(['title', 'publisher', 'paid'] as const).map((sort) => (
          <TouchableOpacity
            key={sort}
            style={[
              styles.sortButton,
              sortBy === sort && styles.sortButtonActive,
            ]}
            onPress={() => setSortBy(sort)}
          >
            <Text
              style={[
                styles.sortButtonText,
                sortBy === sort && styles.sortButtonTextActive,
              ]}
            >
              {sort.charAt(0).toUpperCase() + sort.slice(1)}
            </Text>
          </TouchableOpacity>
        ))}
      </View>
    </View>
  );

  if (error) {
    return (
      <View style={styles.errorState}>
        <Text style={styles.errorIcon}>⚠️</Text>
        <Text style={styles.errorTitle}>Error Loading Books</Text>
        <Text style={styles.errorText}>{error}</Text>
        <TouchableOpacity style={styles.retryButton} onPress={refreshBooks}>
          <Text style={styles.retryButtonText}>Try Again</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <FlatList
        data={filteredBooks}
        renderItem={renderBookItem}
        keyExtractor={(item) => item.id.toString()}
        ListHeaderComponent={renderHeader}
        ListEmptyComponent={renderEmptyState}
        refreshControl={
          <RefreshControl refreshing={loading} onRefresh={refreshBooks} />
        }
        contentContainerStyle={styles.listContainer}
        showsVerticalScrollIndicator={false}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8f9fa',
  },
  listContainer: {
    paddingBottom: 20,
  },
  header: {
    padding: 16,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#e9ecef',
  },
  headerTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#2c3e50',
    marginBottom: 12,
  },
  filterContainer: {
    flexDirection: 'row',
    marginBottom: 12,
    gap: 8,
  },
  filterButton: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    backgroundColor: '#ecf0f1',
  },
  filterButtonActive: {
    backgroundColor: '#3498db',
  },
  filterButtonText: {
    fontSize: 14,
    color: '#7f8c8d',
    fontWeight: '500',
  },
  filterButtonTextActive: {
    color: '#fff',
  },
  sortContainer: {
    flexDirection: 'row',
    gap: 8,
  },
  sortButton: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 16,
    backgroundColor: '#f8f9fa',
    borderWidth: 1,
    borderColor: '#dee2e6',
  },
  sortButtonActive: {
    backgroundColor: '#e9ecef',
    borderColor: '#adb5bd',
  },
  sortButtonText: {
    fontSize: 12,
    color: '#6c757d',
  },
  sortButtonTextActive: {
    color: '#495057',
    fontWeight: '500',
  },
  bookItem: {
    backgroundColor: '#fff',
    marginHorizontal: 16,
    marginVertical: 4,
    borderRadius: 12,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 2,
  },
  bookContent: {
    flexDirection: 'row',
    padding: 16,
    alignItems: 'center',
  },
  bookImageContainer: {
    marginRight: 12,
  },
  bookImage: {
    width: 60,
    height: 80,
    borderRadius: 8,
  },
  placeholderImage: {
    width: 60,
    height: 80,
    borderRadius: 8,
    backgroundColor: '#ecf0f1',
    justifyContent: 'center',
    alignItems: 'center',
  },
  placeholderText: {
    fontSize: 24,
  },
  bookInfo: {
    flex: 1,
  },
  bookTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#2c3e50',
    marginBottom: 4,
  },
  bookPublisher: {
    fontSize: 14,
    color: '#7f8c8d',
    marginBottom: 8,
  },
  bookMeta: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  badge: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
  },
  paidBadge: {
    backgroundColor: '#e8f5e8',
  },
  freeBadge: {
    backgroundColor: '#e3f2fd',
  },
  badgeText: {
    fontSize: 12,
    fontWeight: '500',
  },
  paidText: {
    color: '#2e7d32',
  },
  freeText: {
    color: '#1976d2',
  },
  topicCount: {
    fontSize: 12,
    color: '#6c757d',
  },
  bookActions: {
    marginLeft: 8,
  },
  actionButton: {
    padding: 8,
  },
  actionButtonText: {
    fontSize: 16,
  },
  emptyState: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 40,
  },
  emptyStateIcon: {
    fontSize: 48,
    marginBottom: 16,
  },
  emptyStateTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#2c3e50',
    marginBottom: 8,
  },
  emptyStateText: {
    fontSize: 14,
    color: '#7f8c8d',
    textAlign: 'center',
    lineHeight: 20,
  },
  errorState: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 40,
  },
  errorIcon: {
    fontSize: 48,
    marginBottom: 16,
  },
  errorTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#e74c3c',
    marginBottom: 8,
  },
  errorText: {
    fontSize: 14,
    color: '#7f8c8d',
    textAlign: 'center',
    marginBottom: 20,
  },
  retryButton: {
    backgroundColor: '#3498db',
    paddingHorizontal: 20,
    paddingVertical: 10,
    borderRadius: 8,
  },
  retryButtonText: {
    color: '#fff',
    fontWeight: '500',
  },
});
