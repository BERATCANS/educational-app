import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  Image,
  FlatList,
  Animated,
  Alert,
  ActivityIndicator,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation, useFocusEffect } from '@react-navigation/native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Book, Topic, Test } from '../types';
import { apiService as api } from '../services/api';


export const DashboardScreen: React.FC = () => {
  const navigation = useNavigation<any>();
  const [stats, setStats] = useState({
    flaggedCount: 0,
    bookCount: 0,
    testCount: 0,
  });
  const [userBooks, setUserBooks] = useState<Book[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchStats = async () => {
    try {
      const [flaggedQuestions, books] = await Promise.all([
        api.getFlaggedQuestions(),
        api.getBooksForUser(1), // User ID'yi auth store'dan alabilirsiniz
      ]);
      
      setUserBooks(books);
      setStats({
        flaggedCount: flaggedQuestions.length,
        bookCount: books.length,
        testCount: 0, // Test sayısını ayrı bir endpoint'den alabilirsiniz
      });
      setError(null);
    } catch (err) {
      setError('İstatistikleri yüklerken bir hata oluştu.');
    } finally {
      setLoading(false);
    }
  };

  useFocusEffect(
    React.useCallback(() => {
      fetchStats();
    }, [])
  );

  const handleFlaggedQuestions = () => {
    navigation.navigate('FlaggedQuestions' as never);
  };

  const handleAddBook = () => {
    navigation.navigate('AddBook');
  };

  const handleLibrary = () => {
    // Kütüphane sekmesine git
    navigation.navigate('Library' as never);
  };

  const handleBookPress = (book: Book) => {
    navigation.navigate('BookDetail', { book });
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <ScrollView style={styles.container} showsVerticalScrollIndicator={false}>
        {/* Header */}
        <View style={styles.header}>
          <Text style={styles.welcomeText}>Hoş Geldiniz!</Text>
          <Text style={styles.subtitleText}>Bugün ne öğrenmek istiyorsunuz?</Text>
        </View>

        {/* Top Section - İşaretlenen Sorular ve Kitap Ekle */}
        <View style={styles.topSection}>
          {/* Sol: İşaretlenen Sorular */}
          <TouchableOpacity
            style={styles.topCard}
            onPress={handleFlaggedQuestions}
            activeOpacity={0.8}
          >
            <View style={styles.cardHeader}>
              <Ionicons name="help-circle" size={24} color="#ef4444" />
              <Text style={styles.cardTitle}>İşaretlenen</Text>
            </View>
            <Text style={styles.cardValue}>{stats.flaggedCount}</Text>
            <Text style={styles.cardSubtitle}>Soru</Text>
            <View style={styles.cardFooter}>
              <Text style={styles.cardActionText}>Görüntüle →</Text>
            </View>
          </TouchableOpacity>

          {/* Sağ: Kitap Ekle */}
          <TouchableOpacity
            style={styles.topCard}
            onPress={handleAddBook}
            activeOpacity={0.8}
          >
            <View style={styles.cardHeader}>
              <Ionicons name="add-circle" size={24} color="#10b981" />
              <Text style={styles.cardTitle}>Kitap Ekle</Text>
            </View>
            <Text style={styles.cardValue}>+</Text>
            <Text style={styles.cardSubtitle}>Yeni Kitap</Text>
            <View style={styles.cardFooter}>
              <Text style={styles.cardActionText}>Kod Gir →</Text>
            </View>
          </TouchableOpacity>
        </View>

        {/* Kütüphane Section */}
        <View style={styles.librarySection}>
          <View style={styles.sectionHeader}>
            <Text style={styles.sectionTitle}>Kütüphanem</Text>
            <TouchableOpacity onPress={handleLibrary}>
              <Text style={styles.seeAllText}>Tümünü Gör →</Text>
            </TouchableOpacity>
          </View>
          
          {userBooks.length > 0 ? (
            <FlatList
              data={userBooks}
              horizontal
              showsHorizontalScrollIndicator={false}
              keyExtractor={(item) => item.id.toString()}
              contentContainerStyle={styles.booksList}
              renderItem={({ item: book }) => (
                <TouchableOpacity
                  style={styles.bookCard}
                  onPress={() => handleBookPress(book)}
                  activeOpacity={0.8}
                >
                  <Image
                    source={{ uri: book.coverImageUrl }}
                    style={styles.bookCover}
                    resizeMode="cover"
                  />
                </TouchableOpacity>
              )}
            />
          ) : (
            <TouchableOpacity
              style={styles.libraryCard}
              onPress={handleAddBook}
              activeOpacity={0.8}
            >
              <View style={styles.libraryCardContent}>
                <Ionicons name="add-circle" size={32} color="#3b82f6" />
                <View style={styles.libraryCardText}>
                  <Text style={styles.libraryCardTitle}>İlk Kitabınızı Ekleyin</Text>
                  <Text style={styles.libraryCardSubtitle}>
                    Kitap kodunu girerek kütüphanenizi oluşturmaya başlayın
                  </Text>
                </View>
                <Ionicons name="chevron-forward" size={24} color="#9ca3af" />
              </View>
            </TouchableOpacity>
          )}
        </View>

        {/* Quick Stats */}
        <View style={styles.statsSection}>
          <Text style={styles.sectionTitle}>Hızlı İstatistikler</Text>
          <View style={styles.statsGrid}>
            <View style={styles.statItem}>
              <Ionicons name="book" size={20} color="#3b82f6" />
              <Text style={styles.statValue}>{stats.bookCount}</Text>
              <Text style={styles.statLabel}>Toplam Kitap</Text>
            </View>
            <View style={styles.statItem}>
              <Ionicons name="checkmark-circle" size={20} color="#10b981" />
              <Text style={styles.statValue}>2</Text>
              <Text style={styles.statLabel}>Tamamlanan Test</Text>
            </View>
            <View style={styles.statItem}>
              <Ionicons name="time" size={20} color="#f59e0b" />
              <Text style={styles.statValue}>12s</Text>
              <Text style={styles.statLabel}>Çalışma Süresi</Text>
            </View>
          </View>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#f8fafc',
  },
  container: {
    flex: 1,
    backgroundColor: '#f8fafc',
  },
  header: {
    backgroundColor: '#ffffff',
    padding: 24,
    borderBottomWidth: 1,
    borderBottomColor: '#e2e8f0',
  },
  welcomeText: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#1e293b',
    marginBottom: 8,
  },
  subtitleText: {
    fontSize: 16,
    color: '#64748b',
  },
  topSection: {
    flexDirection: 'row',
    padding: 16,
    gap: 16,
  },
  topCard: {
    flex: 1,
    backgroundColor: '#ffffff',
    borderRadius: 16,
    padding: 20,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 4,
  },
  cardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 12,
  },
  cardTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#374151',
    marginLeft: 8,
  },
  cardValue: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#1e293b',
    marginBottom: 4,
  },
  cardSubtitle: {
    fontSize: 14,
    color: '#64748b',
    marginBottom: 16,
  },
  cardFooter: {
    alignItems: 'flex-end',
  },
  cardActionText: {
    fontSize: 12,
    color: '#3b82f6',
    fontWeight: '500',
  },
  librarySection: {
    backgroundColor: '#ffffff',
    margin: 16,
    borderRadius: 16,
    padding: 20,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 4,
  },
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 20,
  },
  sectionTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#1e293b',
  },
  seeAllText: {
    fontSize: 14,
    color: '#3b82f6',
    fontWeight: '500',
  },
  libraryCard: {
    backgroundColor: '#f8fafc',
    borderRadius: 12,
    padding: 20,
    borderWidth: 2,
    borderColor: '#e2e8f0',
    borderStyle: 'dashed',
  },
  libraryCardContent: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  libraryCardText: {
    flex: 1,
    marginLeft: 16,
  },
  libraryCardTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#374151',
    marginBottom: 4,
  },
  libraryCardSubtitle: {
    fontSize: 14,
    color: '#64748b',
    lineHeight: 20,
  },
  statsSection: {
    backgroundColor: '#ffffff',
    margin: 16,
    borderRadius: 16,
    padding: 20,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 4,
  },
  statsGrid: {
    flexDirection: 'row',
    justifyContent: 'space-around',
  },
  statItem: {
    alignItems: 'center',
  },
  statValue: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#1e293b',
    marginTop: 8,
    marginBottom: 4,
  },
  statLabel: {
    fontSize: 12,
    color: '#64748b',
    textAlign: 'center',
  },
  booksList: {
    paddingHorizontal: 4,
  },
  bookCard: {
    marginRight: 12,
    borderRadius: 12,
    overflow: 'hidden',
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  bookCover: {
    width: 100,
    height: 140,
    backgroundColor: '#f3f4f6',
  },
});
