import React, { useState, useEffect, useRef } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  Image,
  Alert,
  Dimensions,
  Modal,
  ActivityIndicator, ViewStyle,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { SimpleDrawing } from '../components/SimpleDrawing';
import { AnswerOptions } from '../components/AnswerOptions';
import { DrawingToolbar } from '../components/DrawingToolbar';
import { useNavigation, useRoute } from '@react-navigation/native';
import { Book, Topic, Test, Question, TestSubmissionRequest, TestResult, OptionLetter } from '../types';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { apiService } from '../services/api';
import * as SecureStore from 'expo-secure-store';


const { width: screenWidth } = Dimensions.get('window');

export const TestScreen: React.FC = () => {
  console.log('🚀🚀🚀 TEST SCREEN MOUNTED! 🚀🚀🚀');
  const navigation = useNavigation<any>();
  const route = useRoute<any>();
  const queryClient = useQueryClient();
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
  const [currentTool, setCurrentTool] = useState<'pen' | 'eraser' | 'line' | 'triangle' | 'circle' | 'rectangle' | 'square' | 'highlighter'>('pen');
  const [currentColor, setCurrentColor] = useState('#000000');
  const [currentThickness, setCurrentThickness] = useState(2);
  
  // Ayrı renk ve kalınlık ayarları
  const [penColor, setPenColor] = useState('#000000');
  const [penThickness, setPenThickness] = useState(2);
  const [highlighterColor, setHighlighterColor] = useState('#ffff00');
  const [highlighterThickness, setHighlighterThickness] = useState(8);
  const [showColorPalette, setShowColorPalette] = useState(false);
  const [showThicknessPalette, setShowThicknessPalette] = useState(false);
  const [showShapePalette, setShowShapePalette] = useState(false);
  const [isDrawingShape, setIsDrawingShape] = useState(false);
  const [shapeStartPoint, setShapeStartPoint] = useState<{x: number, y: number} | null>(null);
  const [drawingsByQuestion, setDrawingsByQuestion] = useState<{[questionId: number]: Array<{id: string, path: string, color: string, thickness: number}>}>({});
  const [flaggedQuestions, setFlaggedQuestions] = useState<Set<number>>(new Set());
  const [previousTestResult, setPreviousTestResult] = useState<TestResult | null>(null);
  
  // Hint modal state
  const [showHintModal, setShowHintModal] = useState(false);
  const [currentHintLevel, setCurrentHintLevel] = useState(0); // 0, 1, 2, 3
  const [hintHistory, setHintHistory] = useState<{[questionId: number]: number}>({});


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

  // Önceki test sonucunu çek
  const { data: previousTestResultData, error: testResultError } = useQuery({
    queryKey: ['testResult', test.id],
    queryFn: async () => {
      const token = await SecureStore.getItemAsync('auth-storage');
      if (!token) return null;
      
      try {
        const authData = JSON.parse(token);
        const userId = authData.state?.user?.id;
        if (!userId) return null;
        
        const response = await apiService.getUserTestResult(userId, test.id);
        
        // Backend artık {found: boolean, data: TestResult} formatında döndürüyor
        if (!response || !response.found) {
          console.log('ℹ️ No previous test result found for test:', test.id, '- This is normal for first-time attempts');
          return null;
        }
        
        console.log('✅ Found previous test result for test:', test.id);
        return response.data;
      } catch (error) {
        console.error('❌ Error fetching test result for test:', test.id, error);
        return null;
      }
    },
    enabled: true,
    retry: false, // Hata durumunda tekrar deneme
    staleTime: 30 * 1000, // 30 saniye cache - daha sık yenilensin
  });
  console.log('📊 useQuery result:', { 
    questionsData: questionsData?.length, 
    isLoading, 
    error: error?.message,
    enabled: !!test.id 
  });
  
  console.log('📊 Test result query:', {
    testId: test.id,
    hasResult: !!previousTestResultData,
    isFirstAttempt: previousTestResultData === null,
    error: testResultError?.message
  });

  const questions = questionsData || [];
  const currentQuestion = questions[currentQuestionIndex];
  const totalQuestions = questions.length;

  // Teste ikinci kez girildiğini tespit et
  const isRetakingTest = previousTestResultData !== null;

  // İşaretlenen soruları state'e yükle
  useEffect(() => {
    if (flaggedQuestionsData) {
      const flaggedIds = new Set(flaggedQuestionsData.map(q => q.questionId));
      setFlaggedQuestions(flaggedIds);
      console.log('🏁 Loaded flagged questions:', Array.from(flaggedIds));
    }
  }, [flaggedQuestionsData]);

  // Önceki test sonucunu state'e yükle
  useEffect(() => {
    if (previousTestResultData) {
      setPreviousTestResult(previousTestResultData);
      console.log('📊 Loaded previous test result:', previousTestResultData);
      
      // Önceki cevapları selectedAnswers'a yükle
      if (previousTestResultData.questionResults) {
        const previousAnswers = new Map<number, string>();
        previousTestResultData.questionResults.forEach((qr: any) => {
          // questionId'yi question object'inden al
          const questionId = qr.question?.id || qr.questionId;
          if (questionId) {
            previousAnswers.set(questionId, qr.selectedAnswer);
          }
        });
        setSelectedAnswers(previousAnswers);
        console.log('🔄 Loaded previous answers:', previousAnswers);
      }
    }
  }, [previousTestResultData]);


  // Test tamamlandığında otomatik olarak sonuçları kaydet ve çık
  useEffect(() => {
    if (isTestCompleted) {
      handleFinishTest();
    }
  }, [isTestCompleted]);

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

  // Soru değiştiğinde önceki sorunun çizimlerini kaydet
  const prevQuestionIdRef = useRef<number | null>(null);
  
  useEffect(() => {
    // Eğer önceki soru varsa ve farklıysa, onu kaydet
    if (prevQuestionIdRef.current !== null && 
        prevQuestionIdRef.current !== currentQuestion?.id &&
        drawingsByQuestion[prevQuestionIdRef.current]) {
      console.log('💾 Saving previous question drawings:', prevQuestionIdRef.current);
      saveDrawingsToStorage(prevQuestionIdRef.current, drawingsByQuestion[prevQuestionIdRef.current]);
    }
    
    // Mevcut soru ID'sini güncelle
    prevQuestionIdRef.current = currentQuestion?.id || null;
  }, [currentQuestion?.id]);

  // Soru değiştiğinde hint level'ı sıfırla
  useEffect(() => {
    if (currentQuestion) {
      setCurrentHintLevel(0);
    }
  }, [currentQuestion?.id]);

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

  // Sadece test sonuçlarını kaydet, navigation yapma
  const saveTestResults = async () => {
    const answeredQuestions = selectedAnswers.size;
    const correctAnswers = Array.from(selectedAnswers.entries()).filter(
      ([questionId, answer]) => {
        const question = questions.find(q => q.id === questionId);
        return question && question.correctAnswer === answer;
      }
    ).length;

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
      };

      console.log('📤 Test sonuçları gönderiliyor:', testSubmission);

      // Backend'e test sonuçlarını gönder
      const result = await apiService.submitTest(testSubmission);
      console.log('✅ Test sonuçları başarıyla kaydedildi:', result);
      
      // Test result cache'ini invalidate et - yeni test result'ı çekilsin
      queryClient.invalidateQueries({ queryKey: ['testResult', test.id] });
      console.log('🔄 Test result cache invalidated for test:', test.id);
      
    } catch (error) {
      console.error('❌ Test sonuçları kayıt hatası:', error);
    }
  };

  // Test tamamlandığında çağrılacak fonksiyon
  const handleFinishTest = async () => {
    // Tüm çizimleri kaydet
    for (const [questionId, paths] of Object.entries(drawingsByQuestion)) {
      if (paths && paths.length > 0) {
        await saveDrawingsToStorage(parseInt(questionId), paths);
      }
    }
    
    // Test sonuçlarını kaydet
    await saveTestResults();
    
    // Test tamamlandı, direkt geri dön
    navigation.goBack();
  };

  const goBack = async () => {
    // Tüm çizimleri kaydet
    for (const [questionId, paths] of Object.entries(drawingsByQuestion)) {
      if (paths && paths.length > 0) {
        await saveDrawingsToStorage(parseInt(questionId), paths);
      }
    }
    
    // Eğer test tamamlandıysa uyarı gösterme, direkt çık
    if (isTestCompleted) {
      navigation.goBack();
      return;
    }
    
    if (selectedAnswers.size > 0) {
      Alert.alert(
        'Testten Çık',
        'Testi yarıda bırakmak istediğinizden emin misiniz? Mevcut cevaplarınız kaydedilecek.',
        [
          { text: 'İptal', style: 'cancel' },
          { 
            text: 'Çık', 
            style: 'destructive', 
            onPress: async () => {
              // Mevcut cevapları kaydet (navigation yapma)
              await saveTestResults();
              navigation.goBack();
            }
          },
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

  const handleToolSelect = (tool: 'pen' | 'eraser' | 'line' | 'triangle' | 'circle' | 'rectangle' | 'square' | 'highlighter') => {
    setCurrentTool(tool);
    
    // Tool değiştiğinde kendi renk ve kalınlığına geç
    if (tool === 'pen') {
      setCurrentColor(penColor);
      setCurrentThickness(penThickness);
    } else if (tool === 'highlighter') {
      setCurrentColor(highlighterColor);
      setCurrentThickness(highlighterThickness);
    }
  };

  // Şekil çizme fonksiyonları
  const generateShapePath = (startX: number, startY: number, endX: number, endY: number, shapeType: string): string => {
    const width = Math.abs(endX - startX);
    const height = Math.abs(endY - startY);
    const minX = Math.min(startX, endX);
    const minY = Math.min(startY, endY);

    switch (shapeType) {
      case 'triangle':
        const centerX = minX + width / 2;
        const topY = minY;
        const leftX = minX;
        const rightX = minX + width;
        const bottomY = minY + height;
        return `M${centerX},${topY} L${rightX},${bottomY} L${leftX},${bottomY} Z`;
      
      case 'circle':
        // Merkez: başlangıç noktası (startX, startY)
        // Yarıçap: başlangıç noktasından bitiş noktasına olan mesafe
        const radius = Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));
        const centerX_circle = startX;
        const centerY_circle = startY;
        return `M${centerX_circle - radius},${centerY_circle} A${radius},${radius} 0 1,1 ${centerX_circle + radius},${centerY_circle} A${radius},${radius} 0 1,1 ${centerX_circle - radius},${centerY_circle}`;
      
      case 'rectangle':
        return `M${minX},${minY} L${minX + width},${minY} L${minX + width},${minY + height} L${minX},${minY + height} Z`;
      
      case 'square':
        const size = Math.min(width, height);
        return `M${minX},${minY} L${minX + size},${minY} L${minX + size},${minY + size} L${minX},${minY + size} Z`;
      
      default:
        return '';
    }
  };

  const handleColorSelect = (color: string) => {
    setCurrentColor(color);
    setShowColorPalette(false); // Renk seçildikten sonra paleti kapat
    
    // Tool'a göre ayrı state'leri güncelle
    if (currentTool === 'pen') {
      setPenColor(color);
    } else if (currentTool === 'highlighter') {
      setHighlighterColor(color);
    }
    
    // Line modundaysa line'da kal, değilse pen'e geç
    if (currentTool === 'eraser') {
      setCurrentTool('pen');
    }
  };

  const handleHighlighterColorSelect = (color: string) => {
    setCurrentColor(color);
    setHighlighterColor(color);
    setShowColorPalette(false);
  };

  const handleColorPaletteToggle = () => {
    setShowColorPalette(!showColorPalette);
  };

  // Renk paleti renkleri (daha az renk, daha kompakt)
  const colorPalette = [
    '#000000', '#ffffff', '#ff0000', '#00ff00', '#0000ff', '#ffff00',
    '#ff00ff', '#00ffff', '#ffa500', '#800080', '#808080', '#ffd700'
  ];

  // Fosforlu kalem renkleri
  const highlighterPalette = [
    '#ffff00', '#ffeb3b', '#ffc107', '#ff9800', '#ff5722', '#e91e63', '#9c27b0',
    '#673ab7', '#3f51b5', '#2196f3', '#00bcd4', '#009688', '#4caf50', '#8bc34a',
    '#cddc39', '#ffeb3b'
  ];

  // Kalınlık seçenekleri
  const penThicknessOptions = [1, 2, 3, 4, 5, 6, 8, 10, 12, 16];
  const highlighterThicknessOptions = [4, 6, 8, 10, 12, 14, 16, 18, 20, 24];

  const handleThicknessSelect = (thickness: number) => {
    setCurrentThickness(thickness);
    setShowThicknessPalette(false); // Kalınlık seçildikten sonra paleti kapat
    
    // Tool'a göre ayrı state'leri güncelle
    if (currentTool === 'pen') {
      setPenThickness(thickness);
    } else if (currentTool === 'highlighter') {
      setHighlighterThickness(thickness);
    }
  };

  const handleThicknessPaletteToggle = () => {
    setShowThicknessPalette(!showThicknessPalette);
  };

  const handleShapePaletteToggle = () => {
    setShowShapePalette(!showShapePalette);
  };

  const handleShapeSelect = (shape: 'line' | 'triangle' | 'circle' | 'rectangle' | 'square') => {
    setCurrentTool(shape);
    setShowShapePalette(false);
  };

  // Şekil çizme handler'ları
  const handleShapeStart = (x: number, y: number) => {
    setShapeStartPoint({ x, y });
    setIsDrawingShape(true);
  };

  const handleShapeEnd = (x: number, y: number) => {
    if (!shapeStartPoint || !currentQuestion) return;
    
    const shapePath = generateShapePath(shapeStartPoint.x, shapeStartPoint.y, x, y, currentTool);
    if (shapePath) {
      const newPathObj = {
        id: `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
        path: shapePath,
        color: currentColor,
        thickness: currentThickness,
        tool: currentTool
      };
      
      const newDrawings = {
        ...drawingsByQuestion,
        [currentQuestion.id]: [...(drawingsByQuestion[currentQuestion.id] || []), newPathObj]
      };
      
      setDrawingsByQuestion(newDrawings);
      
      // Anında kaydet
      saveDrawingsToStorage(currentQuestion.id, newDrawings[currentQuestion.id]);
    }
    
    setShapeStartPoint(null);
    setIsDrawingShape(false);
  };

  const handleUndo = () => {
    if (!currentQuestion) return;
    
    const newDrawings = {
      ...drawingsByQuestion,
      [currentQuestion.id]: (drawingsByQuestion[currentQuestion.id] || []).slice(0, -1)
    };
    
    setDrawingsByQuestion(newDrawings);
    
    // Anında kaydet
    saveDrawingsToStorage(currentQuestion.id, newDrawings[currentQuestion.id]);
  };

  const handleClear = () => {
    if (!currentQuestion) return;
    
    const newDrawings = {
      ...drawingsByQuestion,
      [currentQuestion.id]: []
    };
    
    setDrawingsByQuestion(newDrawings);
    
    // Anında kaydet
    saveDrawingsToStorage(currentQuestion.id, []);
  };

  const handlePathsChange = (newPaths: any[]) => {
    if (!currentQuestion) return;
    
    setDrawingsByQuestion(prev => ({
      ...prev,
      [currentQuestion.id]: newPaths
    }));
    
    // Çizim değiştiğinde anında kaydet
    saveDrawingsToStorage(currentQuestion.id, newPaths);
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

  // Hint fonksiyonları
  const handleHintPress = () => {
    if (!currentQuestion) return;
    
    const questionId = currentQuestion.id;
    const currentLevel = hintHistory[questionId] || 0;
    let nextLevel;
    
    if (currentLevel < 3) {
      nextLevel = currentLevel + 1; // 1, 2, 3
    } else {
      nextLevel = 4; // 4. basıştan sonra hep 4
    }
    
    console.log('🔍 handleHintPress - questionId:', questionId, 'currentLevel:', currentLevel, 'nextLevel:', nextLevel);
    
    setHintHistory(prev => ({
      ...prev,
      [questionId]: nextLevel
    }));
    setCurrentHintLevel(nextLevel);
    setShowHintModal(true);
  };

  const handleHintClose = () => {
    setShowHintModal(false);
  };

  const getDisplayedHints = () => {
    if (!currentQuestion || !currentQuestion.hints) return [];
    console.log('🔍 getDisplayedHints - currentHintLevel:', currentHintLevel, 'hints length:', currentQuestion.hints.length);
    
    if (currentHintLevel === 0) return [];
    
    // 4. basıştan sonra tüm hintleri göster
    if (currentHintLevel > 3) {
      console.log('🔍 Showing all hints (4+ basış)');
      return currentQuestion.hints;
    }
    
    // 1., 2., 3. basışta sadece o seviyedeki hint'i göster
    console.log('🔍 Showing hint at index:', currentHintLevel - 1);
    return [currentQuestion.hints[currentHintLevel - 1]];
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
                  size={14} 
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
            onShapeStart={handleShapeStart}
            onShapeEnd={handleShapeEnd}
          />
        </View>
      </>
    );
  };

  // Hint Modal Component
  const HintModal = () => {
    if (!showHintModal || !currentQuestion) return null;
    
    const displayedHints = getDisplayedHints();
    
    return (
      <Modal transparent visible={showHintModal} animationType="fade">
        <TouchableOpacity 
          style={styles.hintOverlay} 
          activeOpacity={1} 
          onPress={handleHintClose}
        >
          <View style={styles.hintModal}>
            <View style={styles.hintHeader}>
              <Text style={styles.hintTitle}>
                İpucu {currentHintLevel > 3 ? '3/3 (Tümü)' : `${currentHintLevel}/3`}
              </Text>
              <TouchableOpacity onPress={handleHintClose}>
                <Ionicons name="close" size={24} color="#666" />
              </TouchableOpacity>
            </View>
            
            <ScrollView 
              style={styles.hintContent}
              contentContainerStyle={styles.hintScrollContent}
              showsVerticalScrollIndicator={true}
              bounces={false}
            >
              {displayedHints.map((hint, index) => (
                <View key={index} style={styles.hintItem}>
                  <Text style={styles.hintText}>{hint}</Text>
                </View>
              ))}
            </ScrollView>
          </View>
        </TouchableOpacity>
      </Modal>
    );
  };


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
        <View style={styles.headerToolsContainer}>
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
              <TouchableOpacity 
                style={[
                  styles.toolButton,
                  (hintHistory[currentQuestion?.id] || 0) > 0 && styles.activeToolButton
                ]} 
                activeOpacity={0.7}
                onPress={handleHintPress}
              >
                <Ionicons name="bulb-outline" size={14} color="#f59e0b" />
                <Text style={styles.toolButtonText}>
                  İpucu {(hintHistory[currentQuestion?.id] || 0) > 0 && `(${Math.min(hintHistory[currentQuestion?.id], 3)}/3)`}
                </Text>
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
            <DrawingToolbar
              showDrawingCanvas={showDrawingCanvas}
              showDrawingToolbar={showDrawingToolbar}
              currentTool={currentTool}
              currentColor={currentColor}
              currentThickness={currentThickness}
              showColorPalette={showColorPalette}
              showThicknessPalette={showThicknessPalette}
              showShapePalette={showShapePalette}
              onDrawingToggle={handleDrawingToggle}
              onToolbarToggle={() => setShowDrawingToolbar(!showDrawingToolbar)}
              onToolSelect={handleToolSelect}
              onColorPaletteToggle={handleColorPaletteToggle}
              onThicknessPaletteToggle={handleThicknessPaletteToggle}
              onShapePaletteToggle={handleShapePaletteToggle}
              onColorSelect={handleColorSelect}
              onThicknessSelect={handleThicknessSelect}
              onShapeSelect={handleShapeSelect}
              onUndo={handleUndo}
              onClear={handleClear}
            />
          </View>
        </View>

        {/* Main Content */}
        <ScrollView 
          style={styles.mainContent}
          contentContainerStyle={styles.scrollContent}
          showsVerticalScrollIndicator={false}
        >
          {renderQuestionContent()}
        </ScrollView>

        {/* Combined Answer Options & Navigation */}
        {!isTestCompleted && currentQuestion && (
          <View style={styles.combinedFooter}>
            {/* Answer Options */}
            <AnswerOptions
              answerNumber={currentQuestion.answerNumber}
              selectedAnswer={selectedAnswers.get(currentQuestion.id) as OptionLetter | null}
              correctAnswer={isRetakingTest ? currentQuestion.correctAnswer : null}
              onAnswerSelect={handleAnswerSelect}
              disabled={isTestCompleted}
            />

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
        
        {/* Hint Modal */}
        <HintModal />
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
  previousResultContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 4,
    backgroundColor: '#fef3c7',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
    alignSelf: 'flex-start',
  },
  previousResultText: {
    fontSize: 11,
    color: '#92400e',
    fontWeight: '600',
    marginLeft: 4,
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
    paddingHorizontal: 4,
  },
  questionNumber: {
    fontSize: Math.max(14, screenWidth * 0.04), // 4% of screen width, min 14px
    fontWeight: '600',
    color: '#374151',
    marginTop: 10,
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
  toolbarToggle: {
    backgroundColor: '#ffffff',
    borderWidth: 1,
    borderColor: '#e2e8f0',
    borderRadius: 12,
    padding: 0,
    minWidth: 28,
    minHeight: 28,
    justifyContent: 'center',
    alignItems: 'center',
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
  // Renk paleti stilleri
  currentColorButton: {
    width: 24,
    height: 24,
    borderRadius: 12,
    borderWidth: 2,
    borderColor: '#e2e8f0',
    marginRight: 8,
  },
  // Hint modal stilleri
  hintOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  hintModal: {
    backgroundColor: 'white',
    borderRadius: 16,
    margin: 20,
    width: '90%',
    height: '50%',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 8,
  },
  hintHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 20,
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  hintTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#1f2937',
  },
  hintContent: {
    flex: 1,
  },
  hintScrollContent: {
    padding: 20,
    paddingBottom: 10,
    justifyContent: 'center',
    flexGrow: 1,
  },
  hintItem: {
    marginBottom: 16,
  },
  hintText: {
    fontSize: 16,
    color: '#374151',
    flex: 1,
    lineHeight: 24,
  },
});
