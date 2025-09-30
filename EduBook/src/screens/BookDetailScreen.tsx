import React, { useState, useEffect, useRef, useMemo, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  Image,
  Dimensions,
  ActivityIndicator,
  Alert,
  Modal,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation, useRoute } from '@react-navigation/native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useQuery } from '@tanstack/react-query';
import { Book, Topic, Test, Question } from '../types';
import { apiService } from '../services/api';
import * as SecureStore from 'expo-secure-store';

// Elle çizilmiş ikonlar
const checkmarkHanddrawn = require('../../assets/checkmark-handdrawn.png');
const crossHanddrawn = require('../../assets/cross-handdrawn.png');

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
  const [currentTestIndex, setCurrentTestIndex] = useState(0);
  
  // Test result'ları için state
  const [testResults, setTestResults] = useState<{[testId: number]: any}>({});
  
  // Optimizasyon için state'ler
  const [hasInitialized, setHasInitialized] = useState(false);
  const [isUpdatingFromTest, setIsUpdatingFromTest] = useState(false);
  
  // Drawer states
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  
  // Current test questions
  const [currentTestQuestions, setCurrentTestQuestions] = useState<Question[]>([]);
  const [loadingQuestions, setLoadingQuestions] = useState(false);
  
  // Memoized values for performance
  const currentTest = useMemo(() => {
    return allTests[currentTestIndex] || null;
  }, [allTests, currentTestIndex]);
  
  const memoizedTestResults = useMemo(() => {
    return testResults;
  }, [testResults]);
  
  // Image dimensions state - her soru için resim boyutları
  const [imageDimensions, setImageDimensions] = useState<{[questionId: number]: {width: number, height: number}}>({});

  // Test result'ları çek - tek seferde tümünü
  useEffect(() => {
    const fetchTestResults = async () => {
      if (allTests.length === 0) return;
      
      try {
        const token = await SecureStore.getItemAsync('auth-storage');
        if (!token) return;
        
        const authData = JSON.parse(token);
        const userId = authData.state?.user?.id;
        if (!userId) return;
        
        // Tek seferde tüm test result'larını çek
        const allResults = await apiService.getUserTestResults(userId);
        
        // Test ID'ye göre map'le
        const results: {[testId: number]: any} = {};
        allResults.forEach((result: any) => {
          if (result.test && result.test.id) {
            results[result.test.id] = result;
          }
        });
        
        setTestResults(results);
        console.log('📊 Loaded test results:', results);
      } catch (error) {
        console.error('Error fetching test results:', error);
      }
    };
    
    fetchTestResults();
  }, [allTests]);

  // Screen focus olduğunda test result'larını yenile
  useEffect(() => {
    const unsubscribe = navigation.addListener('focus', () => {
      console.log('🔄 BookDetailScreen focused, refreshing test results...');
      if (allTests.length > 0) {
        // Test result'larını yenile
        const fetchTestResults = async () => {
          try {
            const token = await SecureStore.getItemAsync('auth-storage');
            if (!token) return;
            
            const authData = JSON.parse(token);
            const userId = authData.state?.user?.id;
            if (!userId) return;
            
            const allResults = await apiService.getUserTestResults(userId);
            const results: {[testId: number]: any} = {};
            allResults.forEach((result: any) => {
              if (result.test && result.test.id) {
                results[result.test.id] = result;
              }
            });
            
            setTestResults(results);
            console.log('🔄 Refreshed test results on focus:', results);
          } catch (error) {
            console.error('Error refreshing test results:', error);
          }
        };
        
        fetchTestResults();
      }
    });

    return unsubscribe;
  }, [navigation, allTests.length]);

  // Test bittiğinde gelen updatedTestResult'ı dinle (artık kullanılmıyor, focus listener ile hallediliyor)
  // Bu useEffect'i kaldırdık çünkü artık navigation.focus listener kullanıyoruz

  // Current test değiştiğinde sorularını yükle
  useEffect(() => {
    if (currentTest) {
      loadQuestionsForTest(currentTest.test.id);
    }
  }, [currentTest]);


  // Debug allTests
  useEffect(() => {
    console.log('📈 allTests changed:', allTests.length, 'total tests available');
  }, [allTests]);

  // Backend'den gelen topic'ler için test'leri çek
  const { data: allTestsData, isLoading: testsLoading } = useQuery({
    queryKey: ['tests', topicsData],
    queryFn: async () => {
      console.log('🔍 useQuery queryFn called with topicsData:', topicsData);
      if (!topicsData) return [];
      
      const allTests: Array<{test: Test, topic: Topic}> = [];
      
      // Her topic için test'leri çek
      for (const topic of topicsData) {
        try {
          console.log('📚 Fetching tests for topic:', topic.id, topic.title);
          const tests = await apiService.getTestsForTopic(topic.id);
          console.log('✅ Tests fetched for topic', topic.id, ':', tests.length, 'tests');
          tests.forEach(test => {
            allTests.push({ test, topic });
          });
        } catch (error) {
          console.error(`Error fetching tests for topic ${topic.id}:`, error);
        }
      }
      
      console.log('🎯 Final allTests array:', allTests.length, 'total tests');
      return allTests;
    },
    enabled: !!topicsData && topicsData.length > 0,
  });

  useEffect(() => {
    console.log('🔄 allTestsData changed:', allTestsData);
    if (allTestsData && !hasInitialized && !isUpdatingFromTest) {
      console.log('✅ Setting allTests with data:', allTestsData.length, 'items');
      setAllTests(allTestsData);
      setCurrentTestIndex(0); // Sadece ilk yüklemede ilk teste başla
      setHasInitialized(true);
    }
  }, [allTestsData, hasInitialized, isUpdatingFromTest]);

  // Navigation functions
  const goToNextTest = () => {
    if (currentTestIndex < allTests.length - 1) {
      setCurrentTestIndex(prev => prev + 1);
    }
  };

  const goToPreviousTest = () => {
    if (currentTestIndex > 0) {
      setCurrentTestIndex(prev => prev - 1);
    }
  };

  const goToTest = (index: number) => {
    if (index >= 0 && index < allTests.length) {
      setCurrentTestIndex(index);
    }
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
  };

  const closeDrawer = () => {
    setIsDrawerOpen(false);
  };


  // Load questions for current test
  const loadQuestionsForTest = useCallback(async (testId: number) => {
    setLoadingQuestions(true);
    
    try {
      console.log('📖 Loading questions for test:', testId);
      const questions = await apiService.getQuestionsForTest(testId, 20);
      setCurrentTestQuestions(questions);
      console.log('✅ Questions loaded for test:', testId, questions.length);
    } catch (error) {
      console.error('❌ Error loading questions for test:', testId, error);
      setCurrentTestQuestions([]);
    } finally {
      setLoadingQuestions(false);
    }
  }, []);

  const scrollToTest = (targetTest: Test, targetTopic: Topic) => {
    const testIndex = allTests.findIndex(
      item => item.test.id === targetTest.id && item.topic.id === targetTopic.id
    );
    
    if (testIndex !== -1) {
      setCurrentTestIndex(testIndex);
    }
    
    closeDrawer();
  };

  const renderQuestion = React.useCallback((question: Question, index: number, test: Test, topic: Topic) => {
    // Target width for image display - use most of screen width
    const targetWidth = screenWidth * 0.9; // 90% of screen width
    
    // Get stored dimensions for this question
    const storedDimensions = imageDimensions[question.id];
    
    // Check if this question was answered correctly in previous test result
    const testResult = memoizedTestResults[test.id];
    let questionStatus: 'correct' | 'incorrect' | 'unanswered' | null = null;
    
    if (testResult && testResult.questionResults) {
      const questionResult = testResult.questionResults.find((qr: any) => 
        qr.question?.id === question.id || qr.questionId === question.id
      );
      
      if (questionResult) {
        questionStatus = questionResult.isCorrect ? 'correct' : 'incorrect';
      } else {
        questionStatus = 'unanswered';
      }
    }
    
    // Calculate height based on aspect ratio if dimensions are available
    const imageStyle = storedDimensions 
      ? {
          width: targetWidth,
          height: (storedDimensions.height / storedDimensions.width) * targetWidth,
          borderRadius: 8,
        }
      : {
          width: targetWidth,
          height: 200, // Fallback height
          borderRadius: 8,
        };
    
    return (
      <View
        key={question.id}
        style={{
          width: '100%',
          alignItems: 'center',
          marginBottom: 16,
          paddingHorizontal: 16,
        }}
      >
        <TouchableOpacity
          style={{
            width: '100%',
            alignItems: 'center',
          }}
          onPress={() => handleQuestionPress(test, topic, question, index)}
          activeOpacity={0.8}
        >
          {question.imageUrl ? (
            <View style={{ width: targetWidth, alignItems: 'center' }}>
              <Image 
                source={{ uri: question.imageUrl }} 
                style={imageStyle}
                resizeMode="contain"
                onError={(error) => {
                  console.log('❌ Image load error for question', question.id, ':', question.imageUrl);
                  console.log('❌ Error details:', error.nativeEvent.error);
                }}
                onLoad={(event) => {
                  const { width, height } = event.nativeEvent.source;
                  console.log('✅ Image loaded for question', question.id, 'original size:', width, 'x', height);
                  
                  // Store original dimensions
                  setImageDimensions(prev => ({
                    ...prev,
                    [question.id]: { width, height }
                  }));
                }}
                onLoadStart={() => {
                  console.log('🔄 Image loading started for question', question.id, ':', question.imageUrl);
                }}
              />
              {/* Question number overlay */}
              <View style={[styles.questionNumberOverlay, { top: 8, right: 8 }]}>
                <Text style={styles.questionNumber}>{index + 1}</Text>
              </View>
              
              {/* Question status indicator - sol üstte */}
              {questionStatus && (
                questionStatus === 'correct' ? (
                  <Image 
                    source={checkmarkHanddrawn} 
                    style={styles.handdrawnCheckmark}
                    resizeMode="contain"
                    tintColor={undefined}
                  />
                ) : questionStatus === 'incorrect' ? (
                  <Image 
                    source={crossHanddrawn} 
                    style={styles.handdrawnCross}
                    resizeMode="contain"
                    tintColor={undefined}
                  />
                ) : (
                  <View style={[
                    styles.questionStatusIndicator,
                    styles.leftTopIndicator,
                    styles.unansweredIndicator
                  ]}>
                    <Ionicons 
                      name="help-circle" 
                      size={18} 
                      color="white" 
                    />
                  </View>
                )
              )}
            </View>
          ) : (
            <View style={[styles.questionImagePlaceholder, { width: targetWidth, height: 200 }]}>
              <Ionicons name="image-outline" size={32} color="#9ca3af" />
              <Text style={{fontSize: 14, color: 'gray', marginTop: 8}}>Soru {index + 1}</Text>
              {/* Question status indicator for placeholder - sol üstte */}
              {questionStatus && (
                questionStatus === 'correct' ? (
                  <Image 
                    source={checkmarkHanddrawn} 
                    style={styles.handdrawnCheckmark}
                    resizeMode="contain"
                    tintColor={undefined}
                  />
                ) : questionStatus === 'incorrect' ? (
                  <Image 
                    source={crossHanddrawn} 
                    style={styles.handdrawnCross}
                    resizeMode="contain"
                    tintColor={undefined}
                  />
                ) : (
                  <View style={[
                    styles.questionStatusIndicator,
                    styles.leftTopIndicator,
                    styles.unansweredIndicator
                  ]}>
                    <Ionicons 
                      name="help-circle" 
                      size={18} 
                      color="white" 
                    />
                  </View>
                )
              )}
          </View>
          )}
        </TouchableOpacity>
      </View>
    );
  }, [imageDimensions, memoizedTestResults]);

  const renderCurrentTest = () => {
    if (allTests.length === 0 || currentTestIndex >= allTests.length) {
      return null;
    }

    const questions = currentTestQuestions;
    const isLoading = loadingQuestions;

    return (
      <View style={styles.testSection}>
        {/* Test Header */}
        <View style={styles.testHeader}>
          <View style={styles.testHeaderInfo}>
            <Text style={styles.testTitle}>{currentTest.topic.title}</Text>
            <View style={styles.testTitleRow}>
              <Text style={styles.testSubtitle}>{currentTest.test.title}</Text>
              {memoizedTestResults[currentTest.test.id] && (
                <View style={styles.headerSuccessRateContainer}>
                  <Ionicons name="trophy" size={14} color="#f59e0b" />
                  <Text style={styles.headerSuccessRateText}>
                    %{Math.round(memoizedTestResults[currentTest.test.id].successRate)}
                  </Text>
                </View>
              )}
            </View>
          </View>
          <View style={styles.testHeaderActions}>
            <Text style={styles.questionCount}>
              {isLoading 
                ? 'Yükleniyor...' 
                : `${questions.length} soru`
              }
            </Text>
            {isLoading && <ActivityIndicator size="small" color="#3b82f6" />}
          </View>
        </View>

        {/* Questions Grid */}
        {!isLoading && questions.length > 0 && (
          <View style={styles.questionsContainer}>
            {questions.map((question, index) => 
              renderQuestion(question, index, currentTest.test, currentTest.topic)
            )}
          </View>
        )}

        {/* Loading state for questions */}
        {isLoading && (
          <View style={styles.loadingQuestionsContainer}>
            <ActivityIndicator size="large" color="#3b82f6" />
            <Text style={styles.loadingQuestionsText}>Sorular yükleniyor...</Text>
          </View>
        )}

        {/* Empty state for questions */}
        {!isLoading && questions.length === 0 && (
          <View style={styles.emptyQuestionsContainer}>
            <Ionicons name="document-outline" size={48} color="#9ca3af" />
            <Text style={styles.emptyQuestionsText}>Bu test için soru bulunamadı</Text>
          </View>
        )}
      </View>
    );
  };

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
              {allTests.length > 0 ? `${currentTestIndex + 1} / ${allTests.length} Test` : 'Test Bulunamadı'}
            </Text>
          </View>
          <TouchableOpacity onPress={openDrawer} style={styles.menuButton}>
            <Ionicons name="menu" size={24} color="#1e293b" />
          </TouchableOpacity>
        </View>

        {/* Current Test */}
        <ScrollView style={styles.testsList} showsVerticalScrollIndicator={false}>
          {renderCurrentTest()}
        </ScrollView>

        {/* Navigation Buttons */}
        {allTests.length > 1 && (
          <View style={styles.navigationContainer}>
            <TouchableOpacity 
              style={[styles.navButton, currentTestIndex === 0 && styles.navButtonDisabled]}
              onPress={goToPreviousTest}
              disabled={currentTestIndex === 0}
            >
              <Ionicons name="chevron-back" size={24} color={currentTestIndex === 0 ? "#9ca3af" : "#1e293b"} />
              <Text style={[styles.navButtonText, currentTestIndex === 0 && styles.navButtonTextDisabled]}>
                Önceki
              </Text>
            </TouchableOpacity>


            <TouchableOpacity 
              style={[styles.navButton, currentTestIndex === allTests.length - 1 && styles.navButtonDisabled]}
              onPress={goToNextTest}
              disabled={currentTestIndex === allTests.length - 1}
            >
              <Text style={[styles.navButtonText, currentTestIndex === allTests.length - 1 && styles.navButtonTextDisabled]}>
                Sonraki
              </Text>
              <Ionicons name="chevron-forward" size={24} color={currentTestIndex === allTests.length - 1 ? "#9ca3af" : "#1e293b"} />
            </TouchableOpacity>
          </View>
        )}

        {/* Drawer Modal */}
        <Modal
          visible={isDrawerOpen}
          transparent={true}
          animationType="slide"
          onRequestClose={closeDrawer}
        >
          <View style={styles.drawerOverlay}>
            <TouchableOpacity 
              style={styles.drawerBackground} 
              activeOpacity={1} 
              onPress={closeDrawer}
            />
            <View style={styles.drawerContainer}>
              <View style={styles.drawerHeader}>
                <Text style={styles.drawerTitle}>Test Listesi</Text>
                <TouchableOpacity onPress={closeDrawer} style={styles.closeButton}>
                  <Ionicons name="close" size={24} color="#1e293b" />
                </TouchableOpacity>
              </View>
              
              <ScrollView style={styles.drawerContent}>
                {topicsLoading ? (
                  <View style={styles.drawerLoadingContainer}>
                    <ActivityIndicator size="small" color="#3b82f6" />
                    <Text style={styles.loadingText}>Testler yükleniyor...</Text>
                  </View>
                ) : topicsData?.map((topic) => {
                  // Bu topic için testleri bul
                  const topicTests = allTests.filter(testItem => testItem.topic.id === topic.id);
                  
                  return (
                    <View key={topic.id} style={styles.topicSection}>
                      <View style={styles.topicHeader}>
                        <Text style={styles.topicTitle}>{topic.title}</Text>
                      </View>
                      {topicTests.map(({ test }, testIndex) => {
                        // Global test index'ini bul
                        const globalTestIndex = allTests.findIndex(
                          item => item.test.id === test.id && item.topic.id === topic.id
                        );
                        
                        return (
                          <TouchableOpacity
                            key={`${topic.id}-${test.id}`}
                            style={[
                              styles.testItem,
                              globalTestIndex === currentTestIndex && styles.testItemActive
                            ]}
                            onPress={() => scrollToTest(test, topic)}
                            activeOpacity={0.7}
                          >
                            <View style={styles.testItemContent}>
                              <Ionicons 
                                name="document-text-outline" 
                                size={16} 
                                color={globalTestIndex === currentTestIndex ? "#3b82f6" : "#6b7280"} 
                              />
                              <Text style={[
                                styles.testItemText,
                                globalTestIndex === currentTestIndex && styles.testItemTextActive
                              ]}>
                                {test.title}
                              </Text>
                            </View>
                            <View style={styles.testItemRight}>
                              <Text style={styles.testQuestionCount}>
                                {test.numberOfQuestions || 0} Soru
                              </Text>
                              {memoizedTestResults[test.id] && (
                                <View style={styles.successRateContainer}>
                                  <Ionicons name="trophy" size={12} color="#f59e0b" />
                                  <Text style={styles.successRateText}>
                                    %{Math.round(memoizedTestResults[test.id].successRate)}
                                  </Text>
                                </View>
                              )}
                            </View>
                          </TouchableOpacity>
                        );
                      })}
                      {topicTests.length === 0 && (
                        <Text style={styles.noTestText}>Bu konu için henüz test yok</Text>
                      )}
                    </View>
                  );
                })}
                
                {!topicsLoading && (!topicsData || topicsData.length === 0) && (
                  <View style={styles.emptyTopicsContainer}>
                    <Text style={styles.emptyTopicsText}>Bu kitap için henüz konu eklenmemiş</Text>
                  </View>
                )}
              </ScrollView>
            </View>
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
  testTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 2,
  },
  headerSuccessRateContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginLeft: 8,
    backgroundColor: '#fef3c7',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 6,
  },
  headerSuccessRateText: {
    fontSize: 11,
    color: '#92400e',
    fontWeight: '600',
    marginLeft: 2,
  },
  questionsContainer: {
    flexDirection: 'column', // Single column for dynamic sizing
    alignItems: 'center', // Center questions
    marginBottom: 16,
    position: 'relative', // Dikey çizgi için
  },
  questionImagePlaceholder: {
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#f1f5f9',
    borderRadius: 12,
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
  // Navigation styles
  navigationContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 12,
    backgroundColor: '#ffffff',
    borderTopWidth: 1,
    borderTopColor: '#e2e8f0',
  },
  navButton: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 8,
    backgroundColor: '#f8fafc',
    borderWidth: 1,
    borderColor: '#e2e8f0',
  },
  navButtonDisabled: {
    backgroundColor: '#f1f5f9',
    borderColor: '#e2e8f0',
  },
  navButtonText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#1e293b',
    marginHorizontal: 4,
  },
  navButtonTextDisabled: {
    color: '#9ca3af',
  },
  testCounter: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    backgroundColor: '#3b82f6',
    borderRadius: 20,
  },
  testCounterText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#ffffff',
  },
  // Loading and empty states for questions
  loadingQuestionsContainer: {
    alignItems: 'center',
    paddingVertical: 40,
    paddingHorizontal: 16,
  },
  loadingQuestionsText: {
    marginTop: 12,
    fontSize: 16,
    color: '#6b7280',
  },
  emptyQuestionsContainer: {
    alignItems: 'center',
    paddingVertical: 40,
    paddingHorizontal: 16,
  },
  emptyQuestionsText: {
    marginTop: 12,
    fontSize: 16,
    color: '#6b7280',
    textAlign: 'center',
  },
  // Drawer item styles
  testItemActive: {
    backgroundColor: '#eff6ff',
    borderColor: '#3b82f6',
    borderWidth: 1,
  },
  testItemRight: {
    alignItems: 'flex-end',
  },
  successRateContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 4,
    backgroundColor: '#fef3c7',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 8,
  },
  successRateText: {
    fontSize: 10,
    color: '#92400e',
    fontWeight: '600',
    marginLeft: 2,
  },
  testItemTopic: {
    fontSize: 12,
    color: '#6b7280',
    marginBottom: 2,
  },
  testItemTextActive: {
    color: '#3b82f6',
    fontWeight: '600',
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
  topicHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 8,
    paddingHorizontal: 4,
  },
  topicTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#374151',
    flex: 1,
  },
  topicTestCount: {
    fontSize: 12,
    fontWeight: '500',
    color: '#6b7280',
    backgroundColor: '#f3f4f6',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
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
  drawerLoadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 20,
  },
  noTestText: {
    fontSize: 14,
    color: '#9ca3af',
    fontStyle: 'italic',
    marginLeft: 16,
    marginTop: 8,
  },
  emptyTopicsContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 40,
  },
  emptyTopicsText: {
    fontSize: 16,
    color: '#6b7280',
    textAlign: 'center',
  },
  testHeaderActions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  questionCount: {
    fontSize: 12,
    color: '#6b7280',
    fontWeight: '500',
  },
  questionNumberOverlay: {
    position: 'absolute',
    top: 4,
    right: 4,
    backgroundColor: 'rgba(0, 0, 0, 0.7)',
    borderRadius: 10,
    width: 20,
    height: 20,
    justifyContent: 'center',
    alignItems: 'center',
  },
  questionNumber: {
    color: '#ffffff',
    fontSize: 10,
    fontWeight: 'bold',
  },
  // Question status indicators
  questionStatusIndicator: {
    position: 'absolute',
    width: 28,
    height: 28,
    borderRadius: 14,
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 3,
    borderColor: '#ffffff',
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.25,
    shadowRadius: 3.84,
    elevation: 5,
  },
  leftTopIndicator: {
    top: '50%',
    left: 8,
    transform: [{ translateY: -14 }], // Yarı yüksekliği kadar yukarı kaydır
  },
  correctIndicator: {
    backgroundColor: '#22c55e', // Daha canlı yeşil
  },
  incorrectIndicator: {
    backgroundColor: '#dc2626', // Daha canlı kırmızı
  },
  unansweredIndicator: {
    backgroundColor: '#6b7280', // Gray
  },
  // Elle çizilmiş tik resmi için özel stil
  handdrawnCheckmark: {
    position: 'absolute',
    top: 20,
    left: -10,
    width: 80,
    height: 80,
  },
  // Elle çizilmiş çarpı resmi için özel stil
  handdrawnCross: {
    position: 'absolute',
    top: 20,
    left: -10,
    width: 80,
    height: 80,
  },
});