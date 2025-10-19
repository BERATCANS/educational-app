import React, { useEffect, useState, useCallback } from 'react';
import {
  View,
  Text,
  Image,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  ActivityIndicator,
  Dimensions,
  Modal,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiService from '../services/api';
import { DailyQuestion, OptionLetter, DailyQuestionAnswerRequest } from '../types';
import { useAuthStore } from '../store/authStore';
import * as Localization from 'expo-localization';
import { MaterialCommunityIcons, Ionicons } from '@expo/vector-icons';
import AnswerStatistics from '../components/AnswerStatistics';
import { SimpleDrawing } from '../components/SimpleDrawing';
import { DailyQuestionTimer } from '../components/DailyQuestionTimer';
import { AnswerOptions } from '../components/AnswerOptions';
import { DrawingToolbar } from '../components/DrawingToolbar';
import { VideoPlayer } from '../components/VideoPlayer';

const { width: screenWidth } = Dimensions.get('window');

type RootStackParamList = {
  DailyQuestion: undefined;
};

type DailyQuestionScreenNavigationProp = NativeStackNavigationProp<RootStackParamList, 'DailyQuestion'>;

export const DailyQuestionScreen: React.FC = () => {
  const navigation = useNavigation<DailyQuestionScreenNavigationProp>();
  const queryClient = useQueryClient();
  const userId = useAuthStore(state => state.user?.id) || 1;
  const [selectedAnswer, setSelectedAnswer] = useState<OptionLetter | null>(null);
  const [showDrawingCanvas, setShowDrawingCanvas] = useState(false);
  const [showVideo, setShowVideo] = useState(false);
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

  // Günün sorusunu getir
  const { data: question, isLoading, error } = useQuery({
    queryKey: ['dailyQuestion'],
    queryFn: () => apiService.getCurrentDailyQuestion(userId),
    refetchInterval: 60000, // Her dakika güncelle
  });

  // Debug: Question response'unu logla
  useEffect(() => {
    console.log('🔍 Question state:', {
      question: question,
      isLoading: isLoading,
      error: error,
      hasQuestion: !!question,
      hasImageUrl: question?.imageUrl,
      imageUrlValue: question?.imageUrl
    });
  }, [question, isLoading, error]);

  // Cevap gönderme mutasyonu
  const submitAnswer = useMutation<DailyQuestion, Error, OptionLetter, unknown>({
    mutationFn: (answer: OptionLetter) => {
      const request: DailyQuestionAnswerRequest = {
        dailyQuestionId: question!.id,
        selectedAnswer: answer
      };
      return apiService.submitDailyQuestionAnswer(userId, request);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dailyQuestion'] });
    },
  });

  // Timer callback - gerekirse parent component'e bilgi verebilir
  const handleTimeUpdate = (timeRemaining: string) => {
    // Gerekirse burada ek işlemler yapılabilir
    console.log('Timer updated:', timeRemaining);
  };

  if (isLoading) {
    return (
      <SafeAreaView style={styles.safeArea}>
          <View style={styles.mainContent}>
            <ActivityIndicator size="large" color="#0000ff" />
        </View>
      </SafeAreaView>
    );
  }

  if (error) {
    const errorMessage = error && typeof error === 'object' && 'message' in error
      ? String(error.message)
      : 'Bilinmeyen bir hata oluştu';

    return (
      <SafeAreaView style={styles.safeArea}>
          <View style={styles.mainContent}>
            <Text style={styles.errorText}>{errorMessage}</Text>
          <TouchableOpacity 
            style={styles.retryButton}
            onPress={() => queryClient.invalidateQueries({ queryKey: ['dailyQuestion'] })}
          >
            <Text style={styles.retryButtonText}>Tekrar Dene</Text>
          </TouchableOpacity>
        </View>
      </SafeAreaView>
    );
  }

  if (!question || !question.id) {
    return (
      <SafeAreaView style={styles.safeArea}>
          <View style={styles.mainContent}>
            <Text style={styles.noQuestionText}>
            {question?.errorMessage || 'Bugün için soru bulunmuyor.'}
          </Text>
        </View>
      </SafeAreaView>
    );
  }

  const handleAnswerSelect = (answer: OptionLetter) => {
    if (question.hasUserAnswered) return;
    setSelectedAnswer(answer);
  };

  const handleSubmit = () => {
    if (!selectedAnswer || question.hasUserAnswered) return;
    submitAnswer.mutate(selectedAnswer);
  };

  // Çizim fonksiyonları
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

  const handleShapeSelect = (shape: 'line' | 'triangle' | 'circle' | 'rectangle' | 'square') => {
    setCurrentTool(shape);
    setShowShapePalette(false);
  };

  const handleShapeStart = (x: number, y: number) => {
    setShapeStartPoint({ x, y });
    setIsDrawingShape(true);
  };

  const handleShapeEnd = (x: number, y: number) => {
    if (!shapeStartPoint || !question) return;
    
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
        [question.id]: [...(drawingsByQuestion[question.id] || []), newPathObj]
      };
      
      setDrawingsByQuestion(newDrawings);
    }
    
    setShapeStartPoint(null);
    setIsDrawingShape(false);
  };

  const handleUndo = () => {
    if (!question) return;
    
    const newDrawings = {
      ...drawingsByQuestion,
      [question.id]: (drawingsByQuestion[question.id] || []).slice(0, -1)
    };
    
    setDrawingsByQuestion(newDrawings);
  };

  const handleClear = () => {
    if (!question) return;
    
    const newDrawings = {
      ...drawingsByQuestion,
      [question.id]: []
    };
    
    setDrawingsByQuestion(newDrawings);
  };

  const handlePathsChange = (newPaths: any[]) => {
    if (!question) return;
    
    setDrawingsByQuestion(prev => ({
      ...prev,
      [question.id]: newPaths
    }));
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

  return (
    <SafeAreaView style={styles.safeArea}>
      {/* Combined Header & Tools */}
      <View style={styles.headerToolsContainer}>
        {/* Header Section */}
        <View style={styles.header}>
          <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backButton}>
            <Ionicons name="arrow-back" size={24} color="#1e293b" />
          </TouchableOpacity>
          <View style={styles.headerInfo}>
            <Text style={styles.headerTitle}>Günün Sorusu</Text>
            <Text style={styles.date}>
              {new Date(question.displayDate + 'T00:00:00').toLocaleDateString(Localization.getLocales()[0].languageTag, {
                day: 'numeric',
                month: 'long',
                weekday: 'long'
              })}
            </Text>
          </View>

          {/* Timer Section */}
          <DailyQuestionTimer 
            question={question} 
            onTimeUpdate={handleTimeUpdate}
          />
        </View>

        {/* Tool Buttons Section */}
        <View style={styles.toolButtons}>
          <View style={styles.mainToolButtons}>
            {/* Video Butonu - Sadece süre bittikten sonra göster */}
            {question.videoUrl && new Date() > new Date(question.displayDate + 'T' + question.endTime) && (
              <TouchableOpacity 
                style={styles.toolButton} 
                activeOpacity={0.7}
                onPress={() => setShowVideo(true)}
              >
                <Ionicons name="play-circle-outline" size={14} color="#3b82f6" />
                <Text style={styles.toolButtonText}>Video</Text>
              </TouchableOpacity>
            )}

            {/* Çizim Butonu */}
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
            onColorPaletteToggle={() => setShowColorPalette(true)}
            onThicknessPaletteToggle={() => setShowThicknessPalette(true)}
            onShapePaletteToggle={() => setShowShapePalette(true)}
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
        style={styles.scrollContent}
        contentContainerStyle={styles.scrollContentContainer}
      >
        {/* Question Header with Toolbar Toggle */}
        {showDrawingCanvas && (
          <View style={styles.questionHeader}>
            <View style={styles.questionHeaderTop}>
              <View style={styles.placeholder} />
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
            </View>
          </View>
        )}

        {/* Soru Resmi - Sadece soru aktifken görünür */}
        {question && question.imageUrl && 
         new Date() >= new Date(question.displayDate + 'T' + question.startTime) && (
          <View style={styles.questionImageContainer}>
            <Image
              source={{ uri: question.imageUrl }}
              style={styles.questionImage}
              resizeMode="contain"
            />
            
            {/* Drawing Component Overlay */}
            <SimpleDrawing
              isActive={showDrawingCanvas}
              currentColor={currentColor}
              currentThickness={currentThickness}
              currentTool={currentTool}
              paths={drawingsByQuestion[question.id] || []}
              onPathsChange={handlePathsChange}
              onShapeStart={handleShapeStart}
              onShapeEnd={handleShapeEnd}
            />
          </View>
        )}

        {/* Soru başlamadan önce - Gün değişiminden sonra */}
        {new Date() < new Date(question.displayDate + 'T' + question.startTime) && (
          <View style={styles.waitingContainer}>
            <Text style={styles.waitingText}>
              Soru henüz başlamadı. Başlangıç saatinde tekrar gelin!
            </Text>
          </View>
        )}

        {/* Soru bitti - Gün sonuna kadar istatistikler */}
        {new Date() > new Date(question.displayDate + 'T' + question.endTime) && 
         new Date() < new Date(question.displayDate + 'T23:59:59') && (
          <View style={styles.resultContainer}>
            {/* Kullanıcının cevabı */}
            {question.hasUserAnswered && (
              <>
                <Text style={styles.resultTitle}>
                  {question.userAnswer?.selectedAnswer === question.afterEndTime?.correctAnswer
                    ? '✅ Doğru Cevap!'
                    : '❌ Yanlış Cevap'}
                </Text>
                <Text style={styles.resultText}>
                  {`Senin cevabın: ${question.userAnswer?.selectedAnswer}`}
                </Text>
                <Text style={styles.resultText}>
                  {`Doğru cevap: ${question.afterEndTime?.correctAnswer || 'bekleniyor...'}`}
                </Text>
              </>
            )}

            {/* Açıklama */}
            <Text style={styles.explanationTitle}>Açıklama</Text>
            <Text style={styles.explanationText}>{question.afterEndTime?.explanation}</Text>

            {/* İstatistikler */}
            <Text style={styles.statsTitle}>İstatistikler</Text>
            {(() => {
              console.log('📊 İstatistikler:', {
                distribution: question.afterEndTime?.statistics.distribution,
                totalAnswers: question.afterEndTime?.statistics.totalAnswers,
                correctAnswer: question.afterEndTime?.correctAnswer,
                userAnswer: question.userAnswer?.selectedAnswer,
                answerNumber: question.answerNumber
              });
              return null;
            })()}
            <AnswerStatistics
              distribution={question.afterEndTime?.statistics.distribution || { A: 0, B: 0, C: 0, D: 0, E: 0 }}
              totalAnswers={question.afterEndTime?.statistics.totalAnswers || 0}
              correctAnswer={question.afterEndTime?.correctAnswer || 'A'}
              userAnswer={question.userAnswer?.selectedAnswer}
              answerNumber={question.answerNumber}
            />

          </View>
        )}
      </ScrollView>

      {/* Video Player */}
      <VideoPlayer 
        videoUrl={question.videoUrl || ''}
        visible={showVideo}
        onClose={() => setShowVideo(false)}
      />

      {/* Soru aktif */}
        {new Date() >= new Date(question.displayDate + 'T' + question.startTime) && 
         new Date() <= new Date(question.displayDate + 'T' + question.endTime) && (
          <>
            {/* Combined Answer Options & Navigation */}
            {!question.hasUserAnswered && (
              <View style={styles.combinedFooter}>
                {/* Answer Options */}
                <AnswerOptions
                  answerNumber={question.answerNumber}
                  selectedAnswer={selectedAnswer}
                  correctAnswer={question.afterEndTime?.correctAnswer}
                  onAnswerSelect={handleAnswerSelect}
                  disabled={question.hasUserAnswered}
                />

                {/* Submit Button */}
                <TouchableOpacity
                  style={[
                    styles.navButton,
                    (!selectedAnswer || submitAnswer.status === 'pending') && styles.submitButtonDisabled
                  ]}
                  onPress={handleSubmit}
                  disabled={!selectedAnswer || submitAnswer.status === 'pending'}
                  activeOpacity={0.7}
                >
                  {submitAnswer.status === 'pending' ? (
                    <ActivityIndicator color="#fff" />
                  ) : (
                    <>
                      <Text style={styles.navButtonText}>Cevapla</Text>
                      <Ionicons name="chevron-forward" size={20} color="#ffffff" />
                    </>
                  )}
                </TouchableOpacity>
              </View>
            )}
          </>
        )}


    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  waitingContainer: {
    position: 'absolute',
    top: '50%',
    left: 16,
    right: 16,
    transform: [{ translateY: -50 }],
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'rgba(255, 255, 255, 0.9)',
    padding: 16,
    borderRadius: 8,
  },
  waitingText: {
    fontSize: 16,
    textAlign: 'center',
    color: '#666',
  },
  resultTitle: {
    fontSize: 24,
    fontWeight: '700',
    textAlign: 'center',
    marginBottom: 16,
  },
  resultText: {
    fontSize: 16,
    textAlign: 'center',
    marginBottom: 8,
    color: '#666',
  },
  statsTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginTop: 24,
    marginBottom: 16,
  },
  safeArea: {
    flex: 1,
    backgroundColor: '#f8fafc',
  },
  mainContent: {
    flex: 1,
    backgroundColor: '#fff',
  },
  scrollContent: {
    flex: 1,
  },
  scrollContentContainer: {
    paddingBottom: 100,
    flexGrow: 1,
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
    fontSize: Math.max(14, screenWidth * 0.04),
    fontWeight: '600',
    color: '#374151',
    marginTop: 10,
    marginBottom: 0,
  },
  questionProgress: {
    height: Math.max(3, screenWidth * 0.01),
    backgroundColor: '#e2e8f0',
    borderRadius: Math.max(1, screenWidth * 0.005),
    overflow: 'hidden',
  },
  progressBar: {
    height: '100%',
    backgroundColor: '#3b82f6',
    borderRadius: Math.max(1, screenWidth * 0.005),
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
    backgroundColor: '#e0f2fe',
  },
  activeToolButtonText: {
    color: '#3b82f6',
  },
  mainToolButtons: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    paddingHorizontal: 12,
    paddingVertical: 4,
    backgroundColor: '#ffffff',
    minHeight: 32,
  },
  date: {
    fontSize: 12,
    color: '#64748b',
  },
  questionImageContainer: {
    position: 'relative',
    width: '100%',
    minHeight: 300,
    alignItems: 'center',
    marginBottom: 12,
    backgroundColor: '#ffffff',
    borderRadius: 8,
  },
  questionImage: {
    width: '100%',
    minHeight: screenWidth * 1.2,
    borderRadius: 8,
    backgroundColor: '#ffffff',
  },
  answersContainer: {
    padding: 16,
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
  navButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#3b82f6',
    paddingHorizontal: 20,
    paddingVertical: 12,
    borderRadius: 8,
    justifyContent: 'center',
  },
  navButtonText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '600',
    marginHorizontal: 8,
  },
  submitButtonDisabled: {
    backgroundColor: '#bdbdbd',
  },
  resultContainer: {
    padding: 16,
  },
  explanationTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginBottom: 8,
  },
  explanationText: {
    fontSize: 16,
    lineHeight: 24,
    marginBottom: 16,
  },
  statsText: {
    fontSize: 16,
    marginBottom: 8,
  },
  distributionContainer: {
    marginTop: 8,
  },
  distributionItem: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 4,
  },
  distributionOption: {
    width: 30,
    fontWeight: '600',
  },
  distributionCount: {
    marginHorizontal: 8,
  },
  distributionPercentage: {
    color: '#666',
  },
  videoButton: {
    backgroundColor: '#ff0000', // YouTube kırmızısı
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 16,
    borderRadius: 8,
    margin: 16,
    shadowColor: '#ff0000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.3,
    shadowRadius: 4,
    elevation: 5,
  },
  videoButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
    marginLeft: 8,
  },
  errorText: {
    color: '#f44336',
    fontSize: 16,
    textAlign: 'center',
    margin: 16,
  },
  noQuestionText: {
    fontSize: 16,
    textAlign: 'center',
    margin: 16,
  },
  retryButton: {
    backgroundColor: '#2196f3',
    padding: 12,
    borderRadius: 8,
    marginTop: 16,
  },
  retryButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
    textAlign: 'center',
  },
  placeholder: {
    flex: 1,
  },
});
