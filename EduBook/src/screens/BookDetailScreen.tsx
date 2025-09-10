import React, { useState, useEffect, useRef } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  Image,
  Dimensions,
  FlatList,
  Animated,
  Modal,
  ActivityIndicator,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation, useRoute } from '@react-navigation/native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useQuery } from '@tanstack/react-query';
import { Book, Topic, Test, Question } from '../types';
import { apiService } from '../services/api';

const { width: screenWidth } = Dimensions.get('window');

export const BookDetailScreen: React.FC = () => {
  const navigation = useNavigation<any>();
  const route = useRoute<any>();
  const { book } = route.params as { book: Book };
  
  // API'den topic'leri çek
  const { data: topicsData, isLoading: topicsLoading, error: topicsError } = useQuery({
    queryKey: ['topics', book.id],
    queryFn: () => apiService.getTopicsForBook(book.id),
    enabled: !!book.id,
  });

  // Tüm testleri flat bir liste halinde topla
  const [allTests, setAllTests] = useState<Array<{test: Test, topic: Topic}>>([]);
  const [displayedTests, setDisplayedTests] = useState<Array<{test: Test, topic: Topic}>>([]);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const testsPerPage = 3; // Her sayfada 3 test
  
  // Drawer states
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const drawerAnimation = useRef(new Animated.Value(-screenWidth * 0.8)).current;
  const flatListRef = useRef<FlatList>(null);
  
  // Dynamic measurements
  const [itemHeights, setItemHeights] = useState<{ [key: string]: number }>({});
  const [bookCoverHeight, setBookCoverHeight] = useState(200);

  // Backend'den gelen topic'ler için test'leri çek
  const { data: allTestsData, isLoading: testsLoading } = useQuery({
    queryKey: ['tests', topicsData],
    queryFn: async () => {
      if (!topicsData) return [];
      
      const allTests: Array<{test: Test, topic: Topic}> = [];
      
      // Her topic için test'leri çek
      for (const topic of topicsData) {
        try {
          const tests = await apiService.getTestsForTopic(topic.id);
          tests.forEach(test => {
            allTests.push({ test, topic });
          });
        } catch (error) {
          console.error(`Error fetching tests for topic ${topic.id}:`, error);
        }
      }
      
      return allTests;
    },
    enabled: !!topicsData && topicsData.length > 0,
  });

  useEffect(() => {
    if (allTestsData) {
      setAllTests(allTestsData);
      // İlk sayfayı yükle
      setDisplayedTests(allTestsData.slice(0, testsPerPage));
    }
  }, [allTestsData]);

  const loadMoreTests = () => {
    if (loading || displayedTests.length >= allTests.length) {
      return;
    }
    
    setLoading(true);
    setTimeout(() => {
      const startIndex = page * testsPerPage;
      const endIndex = startIndex + testsPerPage;
      const newTests = allTests.slice(startIndex, endIndex);
      
      setDisplayedTests(prev => [...prev, ...newTests]);
      setPage(prev => prev + 1);
      setLoading(false);
    }, 500); // Simulate loading delay
  };

  const handleQuestionPress = (test: Test, topic: Topic, question: Question, questionIndex: number) => {
    navigation.navigate('Test', { 
      book, 
      topic, 
      test,
      initialQuestionIndex: questionIndex 
    });
  };


  const goBack = () => {
    navigation.goBack();
  };

  // Drawer functions
  const openDrawer = () => {
    setIsDrawerOpen(true);
    Animated.timing(drawerAnimation, {
      toValue: 0,
      duration: 300,
      useNativeDriver: true,
    }).start();
  };

  const closeDrawer = () => {
    Animated.timing(drawerAnimation, {
      toValue: -screenWidth * 0.8,
      duration: 300,
      useNativeDriver: true,
    }).start(() => {
      setIsDrawerOpen(false);
    });
  };

  // Dynamic height measurement function
  const measureItemHeight = (key: string, height: number) => {
    setItemHeights(prev => ({ ...prev, [key]: height }));
  };

  const calculateDynamicOffset = (targetIndex: number) => {
    let totalOffset = bookCoverHeight;
    
    // Sum up actual measured heights + margins up to target index
    for (let i = 0; i < targetIndex; i++) {
      const itemKey = `${displayedTests[i]?.topic.id}-${displayedTests[i]?.test.id}`;
      const measuredHeight = itemHeights[itemKey];
      if (measuredHeight) {
        // onLayout already includes margins, so just use measured height
        totalOffset += measuredHeight + 8;
      } else {
        // Fallback to estimated height + margin if not measured yet  
        totalOffset += 280 + 8; // +16 for marginBottom (only for fallback)
      }
    }
    
    return totalOffset;
  };

  const navigateToTest = (targetTest: Test, targetTopic: Topic) => {
    // Always load all tests up to target + extra to ensure it's available
    const allTestIndex = allTests.findIndex(
      item => item.test.id === targetTest.id && item.topic.id === targetTopic.id
    );
    
    if (allTestIndex !== -1) {
      // Load all tests up to target + extra
      const extraTests = 5;
      const testsToShow = allTests.slice(0, Math.min(allTestIndex + 1 + extraTests, allTests.length));
      setDisplayedTests(testsToShow);
      setPage(Math.ceil(testsToShow.length / testsPerPage));
      
      // Wait for render and then calculate dynamic offset
      setTimeout(() => {
        const dynamicOffset = calculateDynamicOffset(allTestIndex);
        
        flatListRef.current?.scrollToOffset({ 
          offset: dynamicOffset, 
          animated: true 
        });
      }, 500); // Longer delay to allow measurements
    }
    
    closeDrawer();
  };

  const renderQuestion = React.useCallback((question: Question, index: number, test: Test, topic: Topic) => (
    <TouchableOpacity
      key={question.id}
      style={styles.questionItem}
      onPress={() => handleQuestionPress(test, topic, question, index)}
      activeOpacity={0.8}
    >
      <View style={styles.questionImagePlaceholder}>
        {/* Sadece gri alan, yazı yok */}
      </View>
    </TouchableOpacity>
  ), []);

  const renderTestSection = React.useCallback(({ item }: { item: { test: Test, topic: Topic } }) => {
    const questions = item.test.questions || [];
    
    return (
      <View 
        style={styles.testSection}
        onLayout={(event) => {
          const { height } = event.nativeEvent.layout;
          const itemKey = `${item.topic.id}-${item.test.id}`;
          measureItemHeight(itemKey, height);
        }}
      >
        {/* Test Header */}
        <View style={styles.testHeader}>
          <View style={styles.testHeaderInfo}>
            <Text style={styles.testTitle}>{item.topic.title}</Text>
            <Text style={styles.testSubtitle}>Test {item.test.id}</Text>
          </View>
        </View>

        {/* Questions Grid - TestPreviewScreen formatı */}
        <View style={styles.questionsContainer}>
          {/* Vertical Center Line */}
          <View style={styles.verticalCenterLine} />
          
          {questions.map((question, index) => 
            renderQuestion(question, index, item.test, item.topic)
          )}
        </View>
      </View>
    );
  }, []);

  // Loading state
  if (topicsLoading || testsLoading) {
    return (
      <SafeAreaView style={styles.safeArea}>
        <View style={styles.container}>
          <View style={styles.header}>
            <TouchableOpacity onPress={goBack} style={styles.backButton}>
              <Ionicons name="arrow-back" size={24} color="#1e293b" />
            </TouchableOpacity>
            <View style={styles.headerInfo}>
              <Text style={styles.headerTitle}>{book.title}</Text>
              <Text style={styles.headerSubtitle}>Yükleniyor...</Text>
            </View>
          </View>
          <View style={styles.loadingContainer}>
            <ActivityIndicator size="large" color="#3b82f6" />
            <Text style={styles.loadingText}>Testler yükleniyor...</Text>
          </View>
        </View>
      </SafeAreaView>
    );
  }

  // Error state
  if (topicsError) {
    return (
      <SafeAreaView style={styles.safeArea}>
        <View style={styles.container}>
          <View style={styles.header}>
            <TouchableOpacity onPress={goBack} style={styles.backButton}>
              <Ionicons name="arrow-back" size={24} color="#1e293b" />
            </TouchableOpacity>
            <View style={styles.headerInfo}>
              <Text style={styles.headerTitle}>{book.title}</Text>
              <Text style={styles.headerSubtitle}>Hata</Text>
            </View>
          </View>
          <View style={styles.errorContainer}>
            <Ionicons name="alert-circle-outline" size={64} color="#ef4444" />
            <Text style={styles.errorTitle}>Bir Hata Oluştu</Text>
            <Text style={styles.errorSubtitle}>
              Testler yüklenirken bir hata oluştu. Lütfen tekrar deneyin.
            </Text>
            <TouchableOpacity style={styles.retryButton} onPress={() => navigation.goBack()}>
              <Ionicons name="refresh" size={20} color="#ffffff" />
              <Text style={styles.retryButtonText}>Geri Dön</Text>
            </TouchableOpacity>
          </View>
        </View>
      </SafeAreaView>
    );
  }

  // Empty state
  if (allTests.length === 0) {
    return (
      <SafeAreaView style={styles.safeArea}>
        <View style={styles.container}>
          <View style={styles.header}>
            <TouchableOpacity onPress={goBack} style={styles.backButton}>
              <Ionicons name="arrow-back" size={24} color="#1e293b" />
            </TouchableOpacity>
            <View style={styles.headerInfo}>
              <Text style={styles.headerTitle}>{book.title}</Text>
              <Text style={styles.headerSubtitle}>Test Bulunamadı</Text>
            </View>
          </View>
          <View style={styles.emptyContainer}>
            <Ionicons name="document-outline" size={64} color="#d1d5db" />
            <Text style={styles.emptyTitle}>Test Bulunamadı</Text>
            <Text style={styles.emptySubtitle}>
              Bu kitap için henüz test eklenmemiş.
            </Text>
            <TouchableOpacity style={styles.retryButton} onPress={() => navigation.goBack()}>
              <Ionicons name="arrow-back" size={20} color="#ffffff" />
              <Text style={styles.retryButtonText}>Geri Dön</Text>
            </TouchableOpacity>
          </View>
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.container}>
        {/* Header */}
        <View style={styles.header}>
          <TouchableOpacity onPress={goBack} style={styles.backButton}>
            <Ionicons name="arrow-back" size={24} color="#1e293b" />
          </TouchableOpacity>
          <View style={styles.headerInfo}>
            <Text style={styles.headerTitle}>{book.title}</Text>
            <Text style={styles.headerSubtitle}>
              {allTests.length} Test
            </Text>
          </View>
          <TouchableOpacity onPress={openDrawer} style={styles.menuButton}>
            <Ionicons name="menu" size={24} color="#1e293b" />
          </TouchableOpacity>
        </View>

        {/* Tests List with Book Cover */}
        <FlatList
          ref={flatListRef}
          data={displayedTests}
          renderItem={renderTestSection}
          keyExtractor={(item) => `${item.topic.id}-${item.test.id}`}
          style={styles.testsList}
          showsVerticalScrollIndicator={false}
          onEndReached={loadMoreTests}
          onEndReachedThreshold={0.3}
          removeClippedSubviews={true}
          maxToRenderPerBatch={5}
          windowSize={10}
          initialNumToRender={3}
          ListHeaderComponent={
            <View 
              style={styles.bookCoverSection}
              onLayout={(event) => {
                const { height } = event.nativeEvent.layout;
                setBookCoverHeight(height);
              }}
            >
              <Image source={{ uri: book.coverImageUrl }} style={styles.bookCover} resizeMode="cover" />
            </View>
          }
          ListEmptyComponent={
            <View style={styles.emptyState}>
              <Ionicons name="document-outline" size={48} color="#9ca3af" />
              <Text style={styles.emptyTitle}>Henüz test yok</Text>
              <Text style={styles.emptySubtitle}>Bu kitap için test eklenmemiş</Text>
            </View>
          }
          ListFooterComponent={
            loading ? (
              <View style={styles.loadingFooter}>
                <Text style={styles.loadingText}>Yükleniyor...</Text>
              </View>
            ) : null
          }
        />

        {/* Drawer Modal */}
        <Modal
          visible={isDrawerOpen}
          transparent={true}
          animationType="none"
          onRequestClose={closeDrawer}
        >
          <View style={styles.drawerOverlay}>
            <TouchableOpacity 
              style={styles.drawerBackground} 
              activeOpacity={1} 
              onPress={closeDrawer}
            />
            <Animated.View 
              style={[
                styles.drawerContainer,
                {
                  transform: [{ translateX: drawerAnimation }]
                }
              ]}
            >
              <View style={styles.drawerHeader}>
                <Text style={styles.drawerTitle}>İçindekiler</Text>
                <TouchableOpacity onPress={closeDrawer} style={styles.closeButton}>
                  <Ionicons name="close" size={24} color="#1e293b" />
                </TouchableOpacity>
              </View>
              
              <ScrollView style={styles.drawerContent}>
                {book.topics?.map((topic) => (
                  <View key={topic.id} style={styles.topicSection}>
                    <Text style={styles.topicTitle}>{topic.title}</Text>
                    {topic.tests?.map((test) => (
                      <TouchableOpacity
                        key={`${topic.id}-${test.id}`}
                        style={styles.testItem}
                        onPress={() => navigateToTest(test, topic)}
                        activeOpacity={0.7}
                      >
                        <View style={styles.testItemContent}>
                          <Ionicons name="document-text-outline" size={16} color="#3b82f6" />
                          <Text style={styles.testItemText}>{test.title}</Text>
                        </View>
                        <Text style={styles.testQuestionCount}>
                          {test.questions?.length || 0} soru
                        </Text>
                      </TouchableOpacity>
                    ))}
                  </View>
                ))}
              </ScrollView>
            </Animated.View>
          </View>
        </Modal>
      </View>
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
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
    backgroundColor: '#ffffff',
    borderBottomWidth: 1,
    borderBottomColor: '#e2e8f0',
  },
  backButton: {
    padding: 8,
    marginRight: 8,
  },
  headerInfo: {
    flex: 1,
  },
  headerTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#1e293b',
  },
  headerSubtitle: {
    fontSize: 14,
    color: '#64748b',
    marginTop: 2,
  },
  menuButton: {
    padding: 8,
    marginLeft: 8,
  },
  bookCoverSection: {
    backgroundColor: '#ffffff',
    paddingHorizontal: 16,
    paddingVertical: 20,
    alignItems: 'center',
    borderBottomWidth: 1,
    borderBottomColor: '#e2e8f0',
  },
  bookCover: {
    width: screenWidth * 0.3,
    height: screenWidth * 0.45,
    borderRadius: 8,
  },
  testsList: {
    flex: 1,
  },
  testSection: {
    backgroundColor: '#ffffff',
    marginBottom: 8,
    paddingHorizontal: screenWidth * 0.05, // TestPreviewScreen'deki gibi
  },
  testHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#f1f5f9',
  },
  testHeaderInfo: {
    flex: 1,
  },
  testTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1e293b',
  },
  testSubtitle: {
    fontSize: 14,
    color: '#64748b',
    marginTop: 2,
  },
  questionsContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
    marginBottom: screenWidth * 0.05,
    position: 'relative', // Dikey çizgi için
  },
  verticalCenterLine: {
    position: 'absolute',
    left: '50%',
    transform: [{ translateX: -0.5 }], // Çizgiyi tam ortaya hizala
    top: 0,
    width: 1,
    height: '100%',
    backgroundColor: '#e2e8f0',
    zIndex: 1,
  },
  questionItem: {
    width: (screenWidth * 0.4), // 40% of screen width - 2 tane yan yana sığacak
    marginBottom: screenWidth * 0.03,
    marginHorizontal: screenWidth * 0.01,
  },
  questionImagePlaceholder: {
    height: screenWidth * 0.25, // 25% of screen width
    backgroundColor: '#f1f5f9',
    borderRadius: screenWidth * 0.02,
  },
  testStats: {
    paddingVertical: 12,
    borderTopWidth: 1,
    borderTopColor: '#f1f5f9',
    alignItems: 'center',
  },
  testStatsText: {
    fontSize: 14,
    color: '#64748b',
  },
  emptyState: {
    alignItems: 'center',
    paddingVertical: 40,
    paddingHorizontal: 16,
  },
  loadingFooter: {
    paddingVertical: 20,
    alignItems: 'center',
  },
  // Drawer styles
  drawerOverlay: {
    flex: 1,
    flexDirection: 'row',
  },
  drawerBackground: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
  },
  drawerContainer: {
    width: screenWidth * 0.8,
    backgroundColor: '#ffffff',
    height: '100%',
    shadowColor: '#000',
    shadowOffset: { width: 2, height: 0 },
    shadowOpacity: 0.25,
    shadowRadius: 4,
    elevation: 5,
  },
  drawerHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingVertical: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#e2e8f0',
    backgroundColor: '#f8fafc',
  },
  drawerTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#1e293b',
  },
  closeButton: {
    padding: 4,
  },
  drawerContent: {
    flex: 1,
    paddingHorizontal: 16,
  },
  topicSection: {
    marginVertical: 12,
  },
  topicTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#374151',
    marginBottom: 8,
    paddingHorizontal: 4,
  },
  testItem: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 12,
    paddingVertical: 12,
    backgroundColor: '#f8fafc',
    borderRadius: 8,
    marginBottom: 4,
  },
  testItemContent: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  testItemText: {
    fontSize: 14,
    color: '#374151',
    marginLeft: 8,
    flex: 1,
  },
  testQuestionCount: {
    fontSize: 12,
    color: '#64748b',
    backgroundColor: '#e2e8f0',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
  },
  // Loading, Error, Empty states
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 40,
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
    backgroundColor: '#3b82f6',
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
});