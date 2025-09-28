import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  Image,
  Alert,
  Dimensions,
  ActivityIndicator, ViewStyle,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { SimpleDrawing } from '../components/SimpleDrawing';
import { useNavigation, useRoute } from '@react-navigation/native';
import { Book, Topic, Test, Question, TestSubmissionRequest } from '../types';
import { useQuery } from '@tanstack/react-query';
import { apiService } from '../services/api';
import * as SecureStore from 'expo-secure-store';


const { width: screenWidth } = Dimensions.get('window');

export const TestScreen: React.FC = () => {
  console.log('🚀🚀🚀 TEST SCREEN MOUNTED! 🚀🚀🚀');
  const navigation = useNavigation<any>();
  const route = useRoute<any>();
  const { book, topic, test, initialQuestionIndex = 0, targetQuestionId } = route.params as { 
    book: Book; 
    topic: Topic; 
    test: Test; 
    initialQuestionIndex?: number;
    targetQuestionId?: number;
  };
  console.log('📋 TestScreen params:', { 
    book: book?.title, 
    topic: topic?.title, 
    test: test?.title, 
    initialQuestionIndex,
    targetQuestionId 
  });
  
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(initialQuestionIndex);
  const [selectedAnswers, setSelectedAnswers] = useState<Map<number, string>>(new Map());
  const [isTestCompleted, setIsTestCompleted] = useState(false);
  const [testStartTime] = useState(Date.now()); // Test başlangıç zamanı
  const [showDrawingCanvas, setShowDrawingCanvas] = useState(false);
  const [showDrawingToolbar, setShowDrawingToolbar] = useState(true);
  const [currentTool, setCurrentTool] = useState<'pen' | 'eraser' | 'line'>('pen');
  const [isTestAbandoned, setIsTestAbandoned] = useState(false); // Test terk edildi mi?
  const [currentColor, setCurrentColor] = useState('#000000');
  const [currentThickness, setCurrentThickness] = useState(2);
  const [drawingsByQuestion, setDrawingsByQuestion] = useState<{[questionId: number]: Array<{id: string, path: string, color: string, thickness: number}>}>({});
  const [flaggedQuestions, setFlaggedQuestions] = useState<Set<number>>(new Set());

  // Abandoned test gönderme fonksiyonu
  const submitAbandonedTest = async () => {
    if (isTestCompleted || isTestAbandoned || selectedAnswers.size === 0) {
      return; // Test zaten tamamlanmış veya terk edilmiş veya hiç cevap yok
    }

    try {
      // Gerçek kullanıcı ID'sini al
      const token = await SecureStore.getItemAsync('auth-storage');
      
      if (!token) {
        console.log('⚠️ No auth token found for abandoned test submission');
        return;
      }
      
      let userId: number;
      try {
        const authData = JSON.parse(token);
        if (!authData.state?.user?.id) {
          console.log('⚠️ No user ID found for abandoned test submission');
          return;
        }
        userId = authData.state.user.id;
      } catch (error) {
        console.log('⚠️ Error parsing auth data for abandoned test submission:', error);
        return;
      }

      // Test submission verilerini hazırla
      const userAnswers: Record<number, 'A' | 'B' | 'C' | 'D' | 'E'> = {};
      selectedAnswers.forEach((answer, questionId) => {
        userAnswers[questionId] = answer as 'A' | 'B' | 'C' | 'D' | 'E';
      });

      const testSubmission: TestSubmissionRequest = {
        userId,
        testId: test.id,
        userAnswers,
        totalTimeSeconds: Math.floor((Date.now() - testStartTime) / 1000),
        isAbandoned: true, // Abandoned olarak işaretle
      };

      console.log('🚪 Submitting abandoned test:', testSubmission);
      await apiService.submitTest(testSubmission);
      console.log('✅ Abandoned test submitted successfully');
    } catch (error) {
      console.error('❌ Error submitting abandoned test:', error);
    }
  };

  // API'den soruları çek - gerçek test ID'sini kullan
  const actualTestId = test.id;
  console.log('🔍 Setting up useQuery for test.id:', test.id, '(using actualTestId:', actualTestId, ')');
  const { data: questionsData, isLoading, error } = useQuery({
    queryKey: ['questions', actualTestId],
    queryFn: () => {
      console.log('⚡ React Query queryFn called for actualTestId:', actualTestId);
      return apiService.getQuestionsForTest(actualTestId);
    },
    enabled: !!actualTestId,
  });

  // İşaretlenen soruları çek
  const { data: flaggedQuestionsData } = useQuery({
    queryKey: ['flaggedQuestions'],
    queryFn: () => apiService.getFlaggedQuestions(),
    enabled: true,
  });
  console.log('📊 useQuery result:', { 
    questionsData: questionsData?.length, 
    isLoading, 
    error: error?.message,
    enabled: !!test.id 
  });

  const questions = questionsData || [];
  const currentQuestion = questions[currentQuestionIndex];
  const totalQuestions = questions.length;

  // İşaretlenen soruları state'e yükle
  useEffect(() => {
    if (flaggedQuestionsData) {
      const flaggedIds = new Set(flaggedQuestionsData.map(q => q.questionId));
      setFlaggedQuestions(flaggedIds);
      console.log('🏁 Loaded flagged questions:', Array.from(flaggedIds));
    }
  }, [flaggedQuestionsData]);

  // Component unmount olduğunda abandoned test gönder
  useEffect(() => {
    return () => {
      // Component unmount olduğunda çalışır
      if (!isTestCompleted && !isTestAbandoned && selectedAnswers.size > 0) {
        console.log('🚪 TestScreen unmounting - submitting abandoned test');
        submitAbandonedTest();
      }
    };
  }, [isTestCompleted, isTestAbandoned, selectedAnswers.size]);

  // targetQuestionId geldiğinde doğru soruya git
  useEffect(() => {
    if (targetQuestionId && questions.length > 0) {
      console.log('🎯 Looking for targetQuestionId:', targetQuestionId, 'in', questions.length, 'questions');
      
      const targetIndex = questions.findIndex(q => q.id === targetQuestionId);
      console.log('🔍 Found targetQuestionId at index:', targetIndex);
      
      if (targetIndex !== -1) {
        console.log('✅ Navigating to target question at index:', targetIndex);
        setCurrentQuestionIndex(targetIndex);
      } else {
        console.warn('⚠️ targetQuestionId not found in questions, staying at index 0');
      }
    }
  }, [targetQuestionId, questions]);

  // Debug logging
  useEffect(() => {
    console.log('TestScreen Debug:', {
      originalTestId: test.id,
      actualTestId: actualTestId,
      isLoading,
      error: error?.message,
      questionsCount: questions.length,
      currentQuestionIndex,
      currentQuestion: currentQuestion ? { id: currentQuestion.id, imageUrl: currentQuestion.imageUrl } : null
    });
  }, [test.id, actualTestId, isLoading, error, questions.length, currentQuestionIndex, currentQuestion]);
  
  // Current question'ın çizimlerini al
  const currentQuestionPaths = drawingsByQuestion[currentQuestion?.id] || [];

  // Lazy loading + Memory cleanup: Sadece aktif sorunun çizimlerini yükle
  useEffect(() => {
    const loadCurrentQuestionDrawing = async () => {
      if (!currentQuestion) return;
      
      // Memory cleanup: Diğer soruların çizimlerini RAM'den temizle (sadece aktif soru kalsın)
      const currentQuestionDrawings = drawingsByQuestion[currentQuestion.id];
      setDrawingsByQuestion(currentQuestionDrawings ? { [currentQuestion.id]: currentQuestionDrawings } : {});
      
      // Eğer bu sorunun çizimleri yoksa storage'dan yükle
      if (!currentQuestionDrawings) {
        const paths = await loadDrawingsFromStorage(currentQuestion.id);
        if (paths.length > 0) {
          setDrawingsByQuestion({ [currentQuestion.id]: paths });
        }
      }
    };
    
    loadCurrentQuestionDrawing();
  }, [currentQuestion?.id]);

  // Debounced save: Çizim değişikliklerini 500ms delay ile kaydet
  useEffect(() => {
    if (!currentQuestion || !drawingsByQuestion[currentQuestion.id]) return;
    
    const timeoutId = setTimeout(() => {
      saveDrawingsToStorage(currentQuestion.id, drawingsByQuestion[currentQuestion.id]);
    }, 500);
    
    return () => clearTimeout(timeoutId);
  }, [drawingsByQuestion, currentQuestion?.id]);

  const handleAnswerSelect = (answer: string) => {
    if (!currentQuestion) return;
    
    const newSelectedAnswers = new Map(selectedAnswers);
    newSelectedAnswers.set(currentQuestion.id, answer);
    setSelectedAnswers(newSelectedAnswers);
  };

  const handleNextQuestion = () => {
    if (currentQuestionIndex < totalQuestions - 1) {
      setCurrentQuestionIndex(currentQuestionIndex + 1);
    } else {
      // Test tamamlandı
      setIsTestCompleted(true);
    }
  };

  const handlePreviousQuestion = () => {
    if (currentQuestionIndex > 0) {
      setCurrentQuestionIndex(currentQuestionIndex - 1);
    }
  };

  const handleFinishTest = async () => {
    const answeredQuestions = selectedAnswers.size;
    const correctAnswers = Array.from(selectedAnswers.entries()).filter(
      ([questionId, answer]) => {
        const question = questions.find(q => q.id === questionId);
        return question && question.correctAnswer === answer;
      }
    ).length;

    // Test tamamlandı olarak işaretle
    setIsTestCompleted(true);

    // Test sonuçlarını backend'e gönder
    try {
      const token = await SecureStore.getItemAsync('auth-storage');
      let userId = 1; // Default fallback
      
      if (token) {
        try {
          const authData = JSON.parse(token);
          if (authData.state?.user?.id) {
            userId = authData.state.user.id;
          }
        } catch (error) {
          console.error('Error parsing auth data:', error);
        }
      }

      // Test submission verilerini hazırla
      const userAnswers: Record<number, 'A' | 'B' | 'C' | 'D' | 'E'> = {};
      selectedAnswers.forEach((answer, questionId) => {
        userAnswers[questionId] = answer as 'A' | 'B' | 'C' | 'D' | 'E';
      });

      const testSubmission: TestSubmissionRequest = {
        userId,
        testId: test.id,
        userAnswers,
        totalTimeSeconds: Math.floor((Date.now() - testStartTime) / 1000),
        isAbandoned: false, // Normal test tamamlama
      };

      console.log('📤 Test sonuçları gönderiliyor:', testSubmission);

      // Backend'e test sonuçlarını gönder
      const result = await apiService.submitTest(testSubmission);
      console.log('✅ Test sonuçları başarıyla kaydedildi:', result);

    } catch (error) {
      console.error('❌ Test sonuçları kayıt hatası:', error);
      // Hata olsa bile kullanıcıya sonuçları göster
    }

    Alert.alert(
      'Test Tamamlandı!',
      `Sonuçlarınız:\n\n` +
      `Toplam Soru: ${totalQuestions}\n` +
      `Cevaplanan: ${answeredQuestions}\n` +
      `Doğru: ${correctAnswers}\n` +
      `Yanlış: ${answeredQuestions - correctAnswers}\n` +
      `Başarı Oranı: ${Math.round((correctAnswers / totalQuestions) * 100)}%`,
      [
        {
          text: 'Testi Kapat',
          onPress: async () => {
            // Çizimler otomatik kaydediliyor, ekstra kaydetmeye gerek yok
            navigation.goBack();
          },
        },
        {
          text: 'Tekrar Çöz',
          onPress: () => {
            setCurrentQuestionIndex(0);
            setSelectedAnswers(new Map());
            setIsTestCompleted(false);
          },
        },
      ]
    );
  };

  const goBack = async () => {
    // Çizimler otomatik kaydediliyor
    
    if (selectedAnswers.size > 0) {
      Alert.alert(
        'Testten Çık',
        'Testi yarıda bırakmak istediğinizden emin misiniz? İlerlemeniz kaydedilmeyecek.',
        [
          { text: 'İptal', style: 'cancel' },
          { text: 'Çık', style: 'destructive', onPress: () => navigation.goBack() },
        ]
      );
    } else {
      navigation.goBack();
    }
  };

  const handleDrawingToggle = () => {
    if (!showDrawingCanvas) {
      // Çizim modu açılırken toolbar'ı gizli başlat
      setShowDrawingToolbar(false);
    }
    setShowDrawingCanvas(!showDrawingCanvas);
  };

  const handleToolSelect = (tool: 'pen' | 'eraser' | 'line') => {
    setCurrentTool(tool);
  };

  const handleColorSelect = (color: string) => {
    setCurrentColor(color);
    // Line modundaysa line'da kal, değilse pen'e geç
    if (currentTool === 'eraser') {
      setCurrentTool('pen');
    }
  };

  const handleThicknessSelect = (thickness: number) => {
    setCurrentThickness(thickness);
  };

  const handleUndo = () => {
    if (!currentQuestion) return;
    
    setDrawingsByQuestion(prev => ({
      ...prev,
      [currentQuestion.id]: (prev[currentQuestion.id] || []).slice(0, -1)
    }));
  };

  const handleClear = () => {
    if (!currentQuestion) return;
    
    setDrawingsByQuestion(prev => ({
      ...prev,
      [currentQuestion.id]: []
    }));
  };

  const handlePathsChange = (newPaths: any[]) => {
    if (!currentQuestion) return;
    
    setDrawingsByQuestion(prev => ({
      ...prev,
      [currentQuestion.id]: newPaths
    }));
  };

  // Storage fonksiyonları
  const getStorageKey = (questionId: number) => `drawings_book${book.id}_test${test.id}_q${questionId}`;

  const saveDrawingsToStorage = async (questionId: number, paths: Array<{id: string, path: string, color: string, thickness: number}>) => {
    try {
      const key = getStorageKey(questionId);
      await SecureStore.setItemAsync(key, JSON.stringify(paths));
    } catch (error) {
      console.error('Çizim kaydetme hatası:', error);
    }
  };

  const loadDrawingsFromStorage = async (questionId: number) => {
    try {
      const key = getStorageKey(questionId);
      const saved = await SecureStore.getItemAsync(key);
      if (saved) {
        const parsed = JSON.parse(saved);
        return parsed;
      }
      return [];
    } catch (error) {
      console.error('Çizim yükleme hatası:', error);
      return [];
    }
  };

  // Flagleme fonksiyonları
  const handleFlagQuestion = async (questionId: number) => {
    try {
      // Backend'den userId'yi al
      const token = await SecureStore.getItemAsync('auth-storage');
      let userId = 1; // Default fallback
      
      if (token) {
        try {
          const authData = JSON.parse(token);
          if (authData.state?.user?.id) {
            userId = authData.state.user.id;
          }
        } catch (error) {
          console.error('Error parsing auth data:', error);
        }
      }

      const isFlagged = flaggedQuestions.has(questionId);
      
      if (isFlagged) {
        // Flag'i kaldır
        await apiService.unflagQuestion(userId, questionId);
        setFlaggedQuestions(prev => {
          const newSet = new Set(prev);
          newSet.delete(questionId);
          return newSet;
        });
        Alert.alert('Başarılı', 'Soru işareti kaldırıldı');
      } else {
        // Flag'i ekle
        await apiService.flagQuestion(userId, questionId);
        setFlaggedQuestions(prev => new Set(prev).add(questionId));
        Alert.alert('Başarılı', 'Soru işaretlendi');
      }
    } catch (error) {
      console.error('Flagleme hatası:', error);
      Alert.alert('Hata', 'İşlem sırasında bir hata oluştu');
    }
  };





  const renderQuestionContent = () => {
    if (!currentQuestion) return null;

    return (
      <>
        {/* Compact Question Header */}
        <View style={styles.questionHeader}>
          <View style={styles.questionHeaderTop}>
            <Text style={styles.questionNumber}>
              {currentQuestionIndex + 1} / {totalQuestions}
            </Text>
            {showDrawingCanvas && (
              <TouchableOpacity 
                style={styles.toolbarToggle}
                onPress={() => setShowDrawingToolbar(!showDrawingToolbar)}
              >
                <Ionicons 
                  name={showDrawingToolbar ? "chevron-up" : "chevron-down"} 
                  size={20} 
                  color="#64748b" 
                />
              </TouchableOpacity>
            )}
          </View>
          <View style={styles.questionProgress}>
            <View
                style={[
                  styles.progressBar,
                  { width: `${((currentQuestionIndex + 1) / totalQuestions) * 100}%` } as ViewStyle,
                ]}
            />
          </View>
        </View>

        {/* Full Screen Question Image */}
        <View style={styles.questionImageContainer}>
          <Image 
            source={{ uri: currentQuestion.imageUrl }} 
            style={styles.questionImage}
            resizeMode="contain"
          />
          
          {/* Drawing Component Overlay */}
          <SimpleDrawing
            isActive={showDrawingCanvas}
            currentColor={currentColor}
            currentThickness={currentThickness}
            currentTool={currentTool}
            paths={currentQuestionPaths}
            onPathsChange={handlePathsChange}
          />
        </View>
      </>
    );
  };

  const renderTestCompleted = () => (
    <View style={styles.completedContainer}>
      <Ionicons name="checkmark-circle" size={80} color="#10b981" />
      <Text style={styles.completedTitle}>Test Tamamlandı!</Text>
      <Text style={styles.completedSubtitle}>
        Tüm soruları cevapladınız. Sonuçlarınızı görmek için "Testi Bitir" butonuna tıklayın.
      </Text>
      
      <View style={styles.completedStats}>
        <View style={styles.statItem}>
          <Text style={styles.statValue}>{totalQuestions}</Text>
          <Text style={styles.statLabel}>Toplam Soru</Text>
        </View>
        <View style={styles.statItem}>
          <Text style={styles.statValue}>{selectedAnswers.size}</Text>
          <Text style={styles.statLabel}>Cevaplanan</Text>
        </View>
      </View>

      <TouchableOpacity
        style={styles.finishButton}
        onPress={handleFinishTest}
        activeOpacity={0.7}
      >
        <Text style={styles.finishButtonText}>Testi Bitir</Text>
      </TouchableOpacity>
    </View>
  );

  // Loading state
  if (isLoading) {
    return (
      <SafeAreaView style={styles.safeArea}>
        <View style={styles.container}>
          <View style={styles.header}>
            <TouchableOpacity onPress={goBack} style={styles.backButton}>
              <Ionicons name="arrow-back" size={24} color="#1e293b" />
            </TouchableOpacity>
            <View style={styles.headerInfo}>
              <Text style={styles.headerTitle}>{test.title}</Text>
              <Text style={styles.headerSubtitle}>
                {topic.title} • {book.title}
              </Text>
            </View>
          </View>
          <View style={styles.loadingContainer}>
            <ActivityIndicator size="large" color="#3b82f6" />
            <Text style={styles.loadingText}>Sorular yükleniyor...</Text>
          </View>
        </View>
      </SafeAreaView>
    );
  }

  // Loading state
  if (isLoading) {
    return (
      <SafeAreaView style={styles.safeArea}>
        <View style={styles.container}>
          <View style={styles.header}>
            <TouchableOpacity onPress={goBack} style={styles.backButton}>
              <Ionicons name="arrow-back" size={24} color="#1e293b" />
            </TouchableOpacity>
            <View style={styles.headerInfo}>
              <Text style={styles.headerTitle}>{test.title}</Text>
              <Text style={styles.headerSubtitle}>
                {topic.title} • {book.title}
              </Text>
            </View>
          </View>
          <View style={styles.loadingContainer}>
            <ActivityIndicator size="large" color="#3b82f6" />
            <Text style={styles.loadingText}>Sorular yükleniyor...</Text>
          </View>
        </View>
      </SafeAreaView>
    );
  }

  // Empty questions state
  if (!isLoading && questions.length === 0) {
    return (
      <SafeAreaView style={styles.safeArea}>
        <View style={styles.container}>
          <View style={styles.header}>
            <TouchableOpacity onPress={goBack} style={styles.backButton}>
              <Ionicons name="arrow-back" size={24} color="#1e293b" />
            </TouchableOpacity>
            <View style={styles.headerInfo}>
              <Text style={styles.headerTitle}>{test.title}</Text>
              <Text style={styles.headerSubtitle}>
                {topic.title} • {book.title}
              </Text>
            </View>
          </View>
          <View style={styles.emptyContainer}>
            <Ionicons name="document-outline" size={64} color="#9ca3af" />
            <Text style={styles.emptyTitle}>Soru Bulunamadı</Text>
            <Text style={styles.emptySubtitle}>
              Bu testte henüz soru eklenmemiş.
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

  // Error state
  if (error) {
    return (
      <SafeAreaView style={styles.safeArea}>
        <View style={styles.container}>
          <View style={styles.header}>
            <TouchableOpacity onPress={goBack} style={styles.backButton}>
              <Ionicons name="arrow-back" size={24} color="#1e293b" />
            </TouchableOpacity>
            <View style={styles.headerInfo}>
              <Text style={styles.headerTitle}>{test.title}</Text>
              <Text style={styles.headerSubtitle}>
                {topic.title} • {book.title}
              </Text>
            </View>
          </View>
          <View style={styles.errorContainer}>
            <Ionicons name="alert-circle-outline" size={64} color="#ef4444" />
            <Text style={styles.errorTitle}>Bir Hata Oluştu</Text>
            <Text style={styles.errorSubtitle}>
              Sorular yüklenirken bir hata oluştu. Lütfen tekrar deneyin.
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

  // Ensure currentQuestion exists before rendering
  if (!currentQuestion) {
    console.log('⚠️ No current question available, showing loading...');
    return (
      <SafeAreaView style={styles.safeArea}>
        <View style={styles.container}>
          <View style={styles.header}>
            <TouchableOpacity onPress={goBack} style={styles.backButton}>
              <Ionicons name="arrow-back" size={24} color="#1e293b" />
            </TouchableOpacity>
            <View style={styles.headerInfo}>
              <Text style={styles.headerTitle}>{test.title}</Text>
              <Text style={styles.headerSubtitle}>
                {topic.title} • {book.title}
              </Text>
            </View>
          </View>
          <View style={styles.loadingContainer}>
            <ActivityIndicator size="large" color="#3b82f6" />
            <Text style={styles.loadingText}>Sorular hazırlanıyor...</Text>
          </View>
        </View>
      </SafeAreaView>
    );
  }

  console.log('🎯 RENDERING QUESTION SCREEN - currentQuestion:', currentQuestion ? currentQuestion.id : 'null');

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.container}>
        {/* Combined Header & Tools */}
        <View style={[styles.headerToolsContainer, showDrawingCanvas && styles.expandedHeaderTools]}>
          {/* Header Section */}
          <View style={styles.header}>
            <TouchableOpacity onPress={goBack} style={styles.backButton}>
              <Ionicons name="arrow-back" size={24} color="#1e293b" />
            </TouchableOpacity>
            <View style={styles.headerInfo}>
              <Text style={styles.headerTitle}>{test.title}</Text>
              <Text style={styles.headerSubtitle}>
                {topic.title} • {book.title}
              </Text>
            </View>
            <TouchableOpacity 
              style={[styles.flagButton, flaggedQuestions.has(currentQuestion?.id) && styles.activeFlagButton]} 
              activeOpacity={0.7}
              onPress={() => currentQuestion && handleFlagQuestion(currentQuestion.id)}
            >
              <Ionicons 
                name={flaggedQuestions.has(currentQuestion?.id) ? "flag" : "flag-outline"} 
                size={20} 
                color={flaggedQuestions.has(currentQuestion?.id) ? "#ef4444" : "#64748b"} 
              />
            </TouchableOpacity>
          </View>

          {/* Tool Buttons Section */}
          <View style={styles.toolButtons}>
            <View style={styles.mainToolButtons}>
              <TouchableOpacity style={styles.toolButton} activeOpacity={0.7}>
                <Ionicons name="bulb-outline" size={14} color="#f59e0b" />
                <Text style={styles.toolButtonText}>İpucu</Text>
              </TouchableOpacity>
              
              <TouchableOpacity style={styles.toolButton} activeOpacity={0.7}>
                <Ionicons name="document-text-outline" size={14} color="#8b5cf6" />
                <Text style={styles.toolButtonText}>Çözüm</Text>
              </TouchableOpacity>
              
              <TouchableOpacity style={styles.toolButton} activeOpacity={0.7}>
                <Ionicons name="play-circle-outline" size={14} color="#3b82f6" />
                <Text style={styles.toolButtonText}>Video</Text>
              </TouchableOpacity>
              
              <TouchableOpacity 
                style={[styles.toolButton, showDrawingCanvas && styles.activeToolButton]} 
                activeOpacity={0.7}
                onPress={handleDrawingToggle}
              >
                <Ionicons name="pencil-outline" size={14} color={showDrawingCanvas ? "#3b82f6" : "#10b981"} />
                <Text style={[styles.toolButtonText, showDrawingCanvas && styles.activeToolButtonText]}>
                  Çizim
                </Text>
              </TouchableOpacity>
            </View>

            {/* Drawing Tools */}
            {showDrawingCanvas && (
              <View style={styles.drawingContainer}>
                {/* Drawing Toolbar */}
                <View style={[styles.drawingToolbar, !showDrawingToolbar && styles.hiddenToolbar]}>
                  {/* Tool Selection */}
                  <View style={styles.drawingSection}>
                    <Text style={styles.sectionTitle}>Araç</Text>
                    <View style={styles.toolRow}>
                      <TouchableOpacity 
                        style={[styles.drawingTool, currentTool === 'pen' && styles.activeTool]}
                        onPress={() => handleToolSelect('pen')}
                      >
                        <Ionicons name="pencil" size={18} color={currentTool === 'pen' ? "#3b82f6" : "#64748b"} />
                        <Text style={[styles.drawingToolText, currentTool === 'pen' && styles.activeToolText]}>Kalem</Text>
                      </TouchableOpacity>
                      <TouchableOpacity 
                        style={[styles.drawingTool, currentTool === 'line' && styles.activeTool]}
                        onPress={() => handleToolSelect('line')}
                      >
                        <Ionicons name="remove" size={18} color={currentTool === 'line' ? "#10b981" : "#64748b"} />
                        <Text style={[styles.drawingToolText, currentTool === 'line' && styles.activeToolText]}>Çizgi</Text>
                      </TouchableOpacity>
                      <TouchableOpacity 
                        style={[styles.drawingTool, currentTool === 'eraser' && styles.activeTool]}
                        onPress={() => handleToolSelect('eraser')}
                      >
                        <Ionicons name="trash" size={18} color={currentTool === 'eraser' ? "#ef4444" : "#64748b"} />
                        <Text style={[styles.drawingToolText, currentTool === 'eraser' && styles.activeToolText]}>Silgi</Text>
                      </TouchableOpacity>
                    </View>
                  </View>

                  {/* Colors & Thickness */}
                  <View style={styles.colorThicknessSection}>
                    <View style={styles.colorSection}>
                      <Text style={styles.sectionTitle}>Renk</Text>
                      <View style={styles.colorRow}>
                        <TouchableOpacity 
                          style={[
                            styles.colorButton, 
                            { backgroundColor: '#000000' },
                            currentColor === '#000000' && styles.activeColorButton
                          ]}
                          onPress={() => handleColorSelect('#000000')}
                        />
                        <TouchableOpacity 
                          style={[
                            styles.colorButton, 
                            { backgroundColor: '#ff0000' },
                            currentColor === '#ff0000' && styles.activeColorButton
                          ]}
                          onPress={() => handleColorSelect('#ff0000')}
                        />
                        <TouchableOpacity 
                          style={[
                            styles.colorButton, 
                            { backgroundColor: '#0000ff' },
                            currentColor === '#0000ff' && styles.activeColorButton
                          ]}
                          onPress={() => handleColorSelect('#0000ff')}
                        />
                        <TouchableOpacity 
                          style={[
                            styles.colorButton, 
                            { backgroundColor: '#00ff00' },
                            currentColor === '#00ff00' && styles.activeColorButton
                          ]}
                          onPress={() => handleColorSelect('#00ff00')}
                        />
                      </View>
                    </View>

                    <View style={styles.thicknessSection}>
                      <Text style={styles.sectionTitle}>Kalınlık</Text>
                      <View style={styles.thicknessRow}>
                        <TouchableOpacity 
                          style={[styles.thicknessButton, currentThickness === 2 && styles.activeThickness]}
                          onPress={() => handleThicknessSelect(2)}
                        >
                          <View style={[styles.thicknessDot, { width: 2, height: 2 }]} />
                        </TouchableOpacity>
                        <TouchableOpacity 
                          style={[styles.thicknessButton, currentThickness === 4 && styles.activeThickness]}
                          onPress={() => handleThicknessSelect(4)}
                        >
                          <View style={[styles.thicknessDot, { width: 4, height: 4 }]} />
                        </TouchableOpacity>
                        <TouchableOpacity 
                          style={[styles.thicknessButton, currentThickness === 6 && styles.activeThickness]}
                          onPress={() => handleThicknessSelect(6)}
                        >
                          <View style={[styles.thicknessDot, { width: 6, height: 6 }]} />
                        </TouchableOpacity>
                      </View>
                    </View>
                  </View>

                  {/* Actions */}
                  <View style={styles.drawingSection}>
                    <Text style={styles.sectionTitle}>İşlem</Text>
                    <View style={styles.actionRow}>
                      <TouchableOpacity style={styles.actionButton} onPress={handleUndo}>
                        <Ionicons name="arrow-undo" size={16} color="#64748b" />
                        <Text style={styles.actionButtonText}>Geri</Text>
                      </TouchableOpacity>
                      <TouchableOpacity style={styles.actionButton} onPress={handleClear}>
                        <Ionicons name="trash-outline" size={16} color="#ef4444" />
                        <Text style={styles.actionButtonText}>Temizle</Text>
                      </TouchableOpacity>
                    </View>
                  </View>
                </View>
              </View>
            )}
          </View>
        </View>

        {/* Main Content */}
        <ScrollView 
          style={styles.mainContent}
          contentContainerStyle={styles.scrollContent}
          showsVerticalScrollIndicator={false}
        >
          {isTestCompleted ? renderTestCompleted() : renderQuestionContent()}
        </ScrollView>

        {/* Combined Answer Options & Navigation */}
        {!isTestCompleted && currentQuestion && (
          <View style={styles.combinedFooter}>
            {/* Answer Options */}
            <View style={styles.answerOptions}>
              {['A', 'B', 'C', 'D', 'E'].slice(0, currentQuestion.answerNumber).map((letter) => (
                <TouchableOpacity
                  key={letter}
                  style={[
                    styles.answerOption,
                    selectedAnswers.get(currentQuestion.id) === letter && styles.selectedAnswerOption
                  ]}
                  onPress={() => handleAnswerSelect(letter)}
                  activeOpacity={0.7}
                >
                  <Text style={[
                    styles.answerLetterText,
                    selectedAnswers.get(currentQuestion.id) === letter && styles.selectedAnswerLetterText
                  ]}>
                    {letter}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>

            {/* Navigation Buttons */}
            <View style={styles.navigationRow}>
              {currentQuestionIndex > 0 && (
                <TouchableOpacity
                  style={styles.navButton}
                  onPress={handlePreviousQuestion}
                  activeOpacity={0.7}
                >
                  <Ionicons name="chevron-back" size={20} color="#ffffff" />
                  <Text style={styles.navButtonText}>Önceki</Text>
                </TouchableOpacity>
              )}

              <TouchableOpacity
                style={[
                  styles.navButton,
                  currentQuestionIndex === 0 && styles.singleButton
                ]}
                onPress={handleNextQuestion}
                activeOpacity={0.7}
              >
                <Text style={styles.navButtonText}>
                  {currentQuestionIndex === totalQuestions - 1 ? 'Bitir' : 'Sonraki'}
                </Text>
                <Ionicons name="chevron-forward" size={20} color="#ffffff" />
              </TouchableOpacity>
            </View>
          </View>
        )}
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
  headerToolsContainer: {
    backgroundColor: '#ffffff',
    borderBottomWidth: 1,
    borderBottomColor: '#e2e8f0',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  expandedHeaderTools: {
    paddingBottom: 8,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 12,
  },
  backButton: {
    padding: 8,
    marginRight: 12,
  },
  headerInfo: {
    flex: 1,
  },
  headerTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1e293b',
    marginBottom: 2,
  },
  headerSubtitle: {
    fontSize: 12,
    color: '#64748b',
  },
  placeholder: {
    width: 40,
  },
  flagButton: {
    padding: 8,
    borderRadius: 20,
    backgroundColor: '#f8fafc',
    borderWidth: 1,
    borderColor: '#e2e8f0',
  },
  activeFlagButton: {
    backgroundColor: '#fef2f2',
    borderColor: '#ef4444',
  },
  mainContent: {
    flex: 1,
  },
  scrollContent: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    paddingBottom: 160, // Combined footer height + extra space
  },
  questionHeader: {
    marginBottom: 8,
  },
  questionHeaderTop: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  questionNumber: {
    fontSize: Math.max(14, screenWidth * 0.04), // 4% of screen width, min 14px
    fontWeight: '600',
    color: '#374151',
    marginBottom: 0,
  },
  questionProgress: {
    height: Math.max(3, screenWidth * 0.01), // 1% of screen width, min 3px
    backgroundColor: '#e2e8f0',
    borderRadius: Math.max(1, screenWidth * 0.005), // 0.5% of screen width, min 1px
    overflow: 'hidden',
  },
  progressBar: {
    height: '100%',
    backgroundColor: '#3b82f6',
    borderRadius: Math.max(1, screenWidth * 0.005), // 0.5% of screen width, min 1px
  },
  questionImageContainer: {
    position: 'relative',
    width: '100%',
    minHeight: screenWidth * 1.2, // Minimum height for large questions
    alignItems: 'center',
    marginBottom: 12,
    backgroundColor: '#f8fafc',
    borderRadius: 8,
  },
  questionImage: {
    width: '100%',
    minHeight: screenWidth * 1.2,
    borderRadius: 8,
    backgroundColor: '#f1f5f9',
  },
  combinedFooter: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    backgroundColor: '#ffffff',
    paddingVertical: 16,
    paddingHorizontal: 20,
    borderTopWidth: 1,
    borderTopColor: '#e5e7eb',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: -2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 10,
    zIndex: 5,
  },
  navigationRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 16,
  },
  answerOptions: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    alignItems: 'center',
    width: '100%',
  },
  answerOption: {
    width: screenWidth * 0.12, // 12% of screen width
    height: screenWidth * 0.12,
    borderRadius: screenWidth * 0.06, // half of width/height
    backgroundColor: '#f8fafc',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: Math.max(1, screenWidth * 0.005), // 0.5% of screen width, min 1px
    borderColor: '#e2e8f0',
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: Math.max(1, screenWidth * 0.002), // 0.2% of screen width, min 1px
    },
    shadowOpacity: 0.1,
    shadowRadius: Math.max(1, screenWidth * 0.005), // 0.5% of screen width, min 1px
    elevation: 1,
  },
  selectedAnswerOption: {
    backgroundColor: '#3b82f6',
    borderColor: '#3b82f6',
    shadowColor: '#3b82f6',
    shadowOpacity: 0.2,
  },
  answerLetterText: {
    fontSize: Math.max(16, screenWidth * 0.04), // 4% of screen width, min 16px
    fontWeight: 'bold',
    color: '#374151',
  },
  selectedAnswerLetterText: {
    color: '#ffffff',
  },
  completedContainer: {
    alignItems: 'center',
    padding: 40,
  },
  completedTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#1e293b',
    marginTop: 16,
    marginBottom: 8,
  },
  completedSubtitle: {
    fontSize: 16,
    color: '#64748b',
    textAlign: 'center',
    marginBottom: 32,
    lineHeight: 24,
  },
  completedStats: {
    flexDirection: 'row',
    gap: 40,
    marginBottom: 32,
  },
  statItem: {
    alignItems: 'center',
  },
  statValue: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#3b82f6',
    marginBottom: 4,
  },
  statLabel: {
    fontSize: 14,
    color: '#64748b',
  },
  finishButton: {
    backgroundColor: '#10b981',
    paddingHorizontal: 32,
    paddingVertical: 16,
    borderRadius: 12,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 4,
  },
  finishButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
  navButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#3b82f6',
    paddingHorizontal: 20,
    paddingVertical: 12,
    borderRadius: 8,
    minWidth: 100,
    justifyContent: 'center',
  },
  singleButton: {
    flex: 1,
    justifyContent: 'center',
  },
  navButtonText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '600',
    marginHorizontal: 8,
  },
  toolButtons: {
    flexDirection: 'column',
    backgroundColor: '#ffffff',
  },
  toolButton: {
    alignItems: 'center',
    flex: 1,
    paddingVertical: 4,
  },
  toolButtonText: {
    fontSize: 12,
    color: '#64748b',
    marginTop: 4,
  },
  activeToolButton: {
    backgroundColor: '#e0f2fe', // Light blue background for active tool
  },
  activeToolButtonText: {
    color: '#3b82f6', // Blue text for active tool
  },
  mainToolButtons: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    paddingHorizontal: 12,
    paddingVertical: 4,
    backgroundColor: '#ffffff',
    minHeight: 32,
  },
  drawingToolbar: {
    backgroundColor: '#f8fafc',
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderTopWidth: 1,
    borderTopColor: '#e2e8f0',
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    maxWidth: screenWidth - 20,
    position: 'relative',
    overflow: 'hidden',
    height: 'auto',
  },
  hiddenToolbar: {
    height: 0,
    paddingVertical: 0,
    borderTopWidth: 0,
    opacity: 0,
  },
  drawingContainer: {
    backgroundColor: '#ffffff',
  },
  toolbarToggle: {
    backgroundColor: '#ffffff',
    borderWidth: 1,
    borderColor: '#e2e8f0',
    borderRadius: 20,
    padding: 4,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  drawingSection: {
    alignItems: 'center',
    marginHorizontal: 3,
    flex: 1,
  },
  colorThicknessSection: {
    alignItems: 'center',
    marginHorizontal: 3,
    flex: 1,
  },
  colorSection: {
    alignItems: 'center',
    marginBottom: 8,
  },
  thicknessSection: {
    alignItems: 'center',
  },
  sectionTitle: {
    fontSize: 10,
    color: '#64748b',
    fontWeight: '600',
    marginBottom: 4,
  },
  toolRow: {
    flexDirection: 'row',
    gap: 4,
  },
  drawingTool: {
    alignItems: 'center',
    padding: 4,
    borderRadius: 6,
    backgroundColor: '#ffffff',
    borderWidth: 1,
    borderColor: '#e2e8f0',
    minWidth: 32,
    minHeight: 32,
  },
  drawingToolText: {
    fontSize: 8,
    color: '#64748b',
    marginTop: 2,
  },
  colorRow: {
    flexDirection: 'row',
    gap: 3,
  },
  colorButton: {
    width: 16,
    height: 16,
    borderRadius: 8,
    borderWidth: 2,
    borderColor: '#e2e8f0',
  },
  thicknessRow: {
    flexDirection: 'row',
    gap: 4,
  },
  thicknessButton: {
    width: 24,
    height: 24,
    borderRadius: 12,
    backgroundColor: '#ffffff',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#e2e8f0',
  },
  thicknessDot: {
    backgroundColor: '#374151',
    borderRadius: 10,
  },
  actionRow: {
    flexDirection: 'row',
    gap: 4,
  },
  actionButton: {
    alignItems: 'center',
    padding: 4,
    borderRadius: 6,
    backgroundColor: '#ffffff',
    borderWidth: 1,
    borderColor: '#e2e8f0',
    minWidth: 32,
    minHeight: 32,
  },
  actionButtonText: {
    fontSize: 8,
    color: '#64748b',
    marginTop: 2,
  },
  activeTool: {
    backgroundColor: '#e0f2fe',
    borderColor: '#3b82f6',
    borderWidth: 2,
  },
  activeToolText: {
    color: '#3b82f6',
    fontWeight: '600',
  },
  activeColorButton: {
    borderColor: '#3b82f6',
    borderWidth: 3,
  },
  activeThickness: {
    backgroundColor: '#e0f2fe',
    borderColor: '#3b82f6',
    borderWidth: 2,
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
