import React, { useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TextInput,
  ActivityIndicator,
  RefreshControl,
  ScrollView,
  TouchableOpacity,
  Alert
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useBookStore } from '../store/bookStore';
import { useCartStore } from '../store/cartStore';
import { BookStoreItem } from '../components/BookStoreItem';
import { Toast } from '../components/Toast';
import { SafeAreaView } from 'react-native-safe-area-context';

export const BookStoreScreen = () => {
  const {
    filteredBooks,
    isLoading,
    error,
    searchQuery,
    filters,
    fetchBooks,
    setSearchQuery,
    resetFilters
  } = useBookStore();

  const {
    cart,
    addToCart,
    isLoading: isCartLoading
  } = useCartStore();

  const [toast, setToast] = React.useState<{
    visible: boolean;
    message: string;
    type: 'success' | 'warning' | 'error';
  }>({
    visible: false,
    message: '',
    type: 'success'
  });

  useEffect(() => {
    fetchBooks();
  }, []);

  const handleAddToCart = async (bookId: number) => {
    try {
      const result = await addToCart(bookId);
      if (result.success) {
        setToast({
          visible: true,
          message: 'Kitap sepete eklendi',
          type: 'success'
        });
      } else if (result.warning) {
        setToast({
          visible: true,
          message: result.warning,
          type: 'warning'
        });
      } else if (result.error) {
        setToast({
          visible: true,
          message: result.error,
          type: 'error'
        });
      }
    } catch (error) {
      setToast({
        visible: true,
        message: error instanceof Error ? error.message : 'Kitap sepete eklenemedi',
        type: 'error'
      });
    }
  };

  const isInCart = (bookId: number) => {
    return cart?.items.some((item: any) => item.bookId === bookId) ?? false;
  };

  const renderItem = ({ item }: { item: any }) => (
    <BookStoreItem
      book={item}
      onAddToCart={handleAddToCart}
      isInCart={isInCart(item.id)}
    />
  );

  if (error) {
    return (
      <View style={styles.centerContainer}>
        <Text style={styles.errorText}>{error}</Text>
        <TouchableOpacity
          style={styles.retryButton}
          onPress={fetchBooks}
        >
          <Text style={styles.retryButtonText}>Tekrar Dene</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      {/* Toast */}
      <Toast
        visible={toast.visible}
        message={toast.message}
        type={toast.type}
        onHide={() => setToast(prev => ({ ...prev, visible: false }))}
        duration={3000}
      />

      {/* Arama Çubuğu */}
      <View style={styles.searchContainer}>
        <Ionicons name="search" size={20} color="#666" style={styles.searchIcon} />
        <TextInput
          style={styles.searchInput}
          placeholder="Kitap veya yayınevi ara..."
          value={searchQuery}
          onChangeText={setSearchQuery}
          returnKeyType="search"
        />
        {searchQuery ? (
          <TouchableOpacity
            onPress={() => setSearchQuery('')}
            style={styles.clearButton}
          >
            <Ionicons name="close-circle" size={20} color="#666" />
          </TouchableOpacity>
        ) : null}
      </View>

      {/* Filtre Özeti */}
      {(filters.gradeLevel || filters.fieldType || filters.examType || filters.lessonType) && (
        <View style={styles.filterSummary}>
          <ScrollView horizontal showsHorizontalScrollIndicator={false}>
            {filters.gradeLevel && (
              <View style={styles.filterChip}>
                <Text style={styles.filterChipText}>{filters.gradeLevel}</Text>
              </View>
            )}
            {filters.fieldType && (
              <View style={styles.filterChip}>
                <Text style={styles.filterChipText}>{filters.fieldType}</Text>
              </View>
            )}
            {filters.examType && (
              <View style={styles.filterChip}>
                <Text style={styles.filterChipText}>{filters.examType}</Text>
              </View>
            )}
            {filters.lessonType && (
              <View style={styles.filterChip}>
                <Text style={styles.filterChipText}>{filters.lessonType}</Text>
              </View>
            )}
            <TouchableOpacity
              style={styles.resetFiltersButton}
              onPress={resetFilters}
            >
              <Text style={styles.resetFiltersText}>Filtreleri Temizle</Text>
            </TouchableOpacity>
          </ScrollView>
        </View>
      )}

      {/* Kitap Listesi */}
      {isLoading ? (
        <View style={styles.centerContainer}>
          <ActivityIndicator size="large" color="#10b981" />
        </View>
      ) : (
        <FlatList
          data={filteredBooks}
          renderItem={renderItem}
          keyExtractor={item => item.id.toString()}
          numColumns={2}
          contentContainerStyle={styles.listContent}
          columnWrapperStyle={styles.row}
          showsVerticalScrollIndicator={false}
          refreshControl={
            <RefreshControl
              refreshing={isLoading}
              onRefresh={fetchBooks}
              colors={['#10b981']}
              tintColor="#10b981"
            />
          }
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <Ionicons name="book-outline" size={48} color="#666" />
              <Text style={styles.emptyText}>
                {searchQuery
                  ? 'Aramanızla eşleşen kitap bulunamadı'
                  : 'Henüz kitap eklenmemiş'}
              </Text>
            </View>
          }
        />
      )}
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f9fafb',
  },
  centerContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  searchContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fff',
    margin: 16,
    paddingHorizontal: 12,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#e5e7eb',
  },
  searchIcon: {
    marginRight: 8,
  },
  searchInput: {
    flex: 1,
    height: 40,
    fontSize: 16,
    color: '#1f2937',
  },
  clearButton: {
    padding: 4,
  },
  filterSummary: {
    paddingHorizontal: 16,
    marginBottom: 8,
  },
  filterChip: {
    backgroundColor: '#e5e7eb',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 16,
    marginRight: 8,
  },
  filterChipText: {
    fontSize: 14,
    color: '#4b5563',
  },
  resetFiltersButton: {
    paddingHorizontal: 12,
    paddingVertical: 6,
  },
  resetFiltersText: {
    fontSize: 14,
    color: '#ef4444',
  },
  listContent: {
    padding: 8,
  },
  row: {
    justifyContent: 'space-evenly',
  },
  emptyContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    padding: 32,
  },
  emptyText: {
    marginTop: 16,
    fontSize: 16,
    color: '#666',
    textAlign: 'center',
  },
  errorText: {
    fontSize: 16,
    color: '#ef4444',
    textAlign: 'center',
    marginBottom: 16,
  },
  retryButton: {
    backgroundColor: '#10b981',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 8,
  },
  retryButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
});
