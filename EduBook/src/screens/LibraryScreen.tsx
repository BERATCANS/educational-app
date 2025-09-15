import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  Image,
  FlatList,
  ActivityIndicator,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useQuery } from '@tanstack/react-query';
import { Book } from '../types';
import { apiService } from '../services/api';
import { useAuthStore } from '../store/authStore';

export const LibraryScreen: React.FC = () => {
  const navigation = useNavigation<any>();
  const { user } = useAuthStore();

  // API'den kitapları çek
  const { data: booksData, isLoading, error, refetch } = useQuery({
    queryKey: ['books', user?.id],
    queryFn: () => apiService.getBooksForUser(user?.id || 1),
    enabled: !!user?.id,
  });

  // Her kitap için konu ve test sayılarını çek
  const { data: booksWithStats } = useQuery({
    queryKey: ['booksWithStats', booksData],
    queryFn: async () => {
      if (!booksData) return [];
      
      const booksWithStats = await Promise.all(
        booksData.map(async (book) => {
          try {
            // Konuları çek
            const topics = await apiService.getTopicsForBook(book.id);
            
            // Her konu için test sayısını hesapla
            let totalTests = 0;
            for (const topic of topics) {
              const tests = await apiService.getTestsForTopic(topic.id);
              totalTests += tests.length;
            }
            
            return {
              ...book,
              paid: true,
              topics: topics.map(topic => ({
                ...topic,
                tests: [] // Test detaylarına gerek yok, sadece sayı
              })),
              totalTopics: topics.length,
              totalTests: totalTests
            };
          } catch (error) {
            console.error(`Error fetching stats for book ${book.id}:`, error);
            return {
              ...book,
              paid: true,
              topics: [],
              totalTopics: 0,
              totalTests: 0
            };
          }
        })
      );
      
      return booksWithStats;
    },
    enabled: !!booksData && booksData.length > 0,
  });

  const books = booksWithStats || [];

  const handleBookPress = (book: Book) => {
    navigation.navigate('BookDetail', { book });
  };

  const handleAddBook = () => {
    navigation.navigate('AddBook');
  };

  const renderBookItem = ({ item: book }: { item: Book }) => (
      <TouchableOpacity
        style={styles.bookCard}
      onPress={() => handleBookPress(book)}
      activeOpacity={0.7}
    >
      <View style={styles.bookImageContainer}>
        <Image
          source={{ uri: book.coverImageUrl }}
          style={styles.bookImage}
          resizeMode="cover"
        />
        {book.paid && (
          <View style={styles.paidBadge}>
            <Ionicons name="checkmark-circle" size={16} color="#10b981" />
            <Text style={styles.paidText}>Satın Alındı</Text>
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
          <View style={styles.bookStats}>
          <View style={styles.statItem}>
            <Ionicons name="library" size={14} color="#6b7280" />
            <Text style={styles.statText}>
              {(book as any).totalTopics || 0} Konu
            </Text>
          </View>
          <View style={styles.statItem}>
            <Ionicons name="document-text" size={14} color="#6b7280" />
            <Text style={styles.statText}>
              {(book as any).totalTests || 0} Test
            </Text>
          </View>
        </View>
      </View>
      <View style={styles.bookActions}>
          <Ionicons name="chevron-forward" size={20} color="#9ca3af" />
        </View>
    </TouchableOpacity>
  );

  const renderEmptyState = () => (
    <View style={styles.emptyContainer}>
      <Ionicons name="library-outline" size={64} color="#d1d5db" />
      <Text style={styles.emptyTitle}>Kütüphaneniz Boş</Text>
      <Text style={styles.emptySubtitle}>
        Henüz hiç kitabınız yok. Kitap eklemek için aşağıdaki butona tıklayın.
      </Text>
      <TouchableOpacity style={styles.addBookButton} onPress={handleAddBook}>
        <Ionicons name="add" size={20} color="#ffffff" />
        <Text style={styles.addBookButtonText}>Kitap Ekle</Text>
      </TouchableOpacity>
    </View>
  );

  const renderErrorState = () => (
    <View style={styles.errorContainer}>
      <Ionicons name="alert-circle-outline" size={64} color="#ef4444" />
      <Text style={styles.errorTitle}>Bir Hata Oluştu</Text>
      <Text style={styles.errorSubtitle}>
        Kitaplar yüklenirken bir hata oluştu. Lütfen tekrar deneyin.
      </Text>
      <TouchableOpacity style={styles.retryButton} onPress={() => refetch()}>
        <Ionicons name="refresh" size={20} color="#ffffff" />
        <Text style={styles.retryButtonText}>Tekrar Dene</Text>
      </TouchableOpacity>
    </View>
  );

  if (isLoading || !booksWithStats) {
    return (
      <SafeAreaView style={styles.container}>
        <View style={styles.header}>
          <Text style={styles.headerTitle}>Kütüphanem</Text>
          <TouchableOpacity style={styles.addButton} onPress={handleAddBook}>
            <Ionicons name="add" size={24} color="#ffffff" />
          </TouchableOpacity>
        </View>
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color="#3b82f6" />
          <Text style={styles.loadingText}>Kitaplar yükleniyor...</Text>
        </View>
      </SafeAreaView>
    );
  }

  if (error) {
  return (
      <SafeAreaView style={styles.container}>
        <View style={styles.header}>
          <Text style={styles.headerTitle}>Kütüphanem</Text>
          <TouchableOpacity style={styles.addButton} onPress={handleAddBook}>
            <Ionicons name="add" size={24} color="#ffffff" />
          </TouchableOpacity>
        </View>
        {renderErrorState()}
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Kütüphanem</Text>
        <TouchableOpacity style={styles.addButton} onPress={handleAddBook}>
            <Ionicons name="add" size={24} color="#ffffff" />
          </TouchableOpacity>
        </View>

      {books && books.length > 0 ? (
        <FlatList
          data={books}
          renderItem={renderBookItem}
          keyExtractor={(item) => item.id.toString()}
          contentContainerStyle={styles.booksList}
          showsVerticalScrollIndicator={false}
        />
      ) : (
        renderEmptyState()
      )}
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8fafc',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 16,
    backgroundColor: '#ffffff',
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  headerTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#1f2937',
  },
  addButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#3b82f6',
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingText: {
    marginTop: 16,
    fontSize: 16,
    color: '#6b7280',
  },
  errorContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 40,
  },
  errorTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#1f2937',
    marginTop: 16,
    marginBottom: 8,
  },
  errorSubtitle: {
    fontSize: 16,
    color: '#6b7280',
    textAlign: 'center',
    lineHeight: 24,
    marginBottom: 24,
  },
  retryButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#ef4444',
    paddingHorizontal: 20,
    paddingVertical: 12,
    borderRadius: 8,
  },
  retryButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
    marginLeft: 8,
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 40,
  },
  emptyTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#1f2937',
    marginTop: 16,
    marginBottom: 8,
  },
  emptySubtitle: {
    fontSize: 16,
    color: '#6b7280',
    textAlign: 'center',
    lineHeight: 24,
    marginBottom: 24,
  },
  addBookButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#3b82f6',
    paddingHorizontal: 20,
    paddingVertical: 12,
    borderRadius: 8,
  },
  addBookButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
    marginLeft: 8,
  },
  booksList: {
    padding: 20,
  },
  bookCard: {
    flexDirection: 'row',
    backgroundColor: '#ffffff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 3.84,
    elevation: 5,
  },
  bookImageContainer: {
    position: 'relative',
  },
  bookImage: {
    width: 80,
    height: 100,
    borderRadius: 8,
    backgroundColor: '#f3f4f6',
  },
  paidBadge: {
    position: 'absolute',
    top: -4,
    right: -4,
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#ffffff',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 12,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 1,
    },
    shadowOpacity: 0.2,
    shadowRadius: 1.41,
    elevation: 2,
  },
  paidText: {
    fontSize: 10,
    color: '#10b981',
    fontWeight: '600',
    marginLeft: 2,
  },
  bookInfo: {
    flex: 1,
    marginLeft: 16,
    justifyContent: 'space-between',
  },
  bookTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1f2937',
    lineHeight: 22,
  },
  bookPublisher: {
    fontSize: 14,
    color: '#6b7280',
    marginTop: 4,
  },
  bookStats: {
    flexDirection: 'row',
    marginTop: 8,
  },
  statItem: {
    flexDirection: 'row',
    alignItems: 'center',
    marginRight: 16,
  },
  statText: {
    fontSize: 12,
    color: '#6b7280',
    marginLeft: 4,
  },
  bookActions: {
    justifyContent: 'center',
    alignItems: 'center',
    marginLeft: 8,
  },
});