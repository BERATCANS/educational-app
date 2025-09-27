import React, { useState, useRef, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  SafeAreaView,
  Dimensions,
  Animated,
  FlatList,
  ScrollView,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { LinearGradient } from 'expo-linear-gradient';
import { useAuthStore } from '../store/authStore';

const { width } = Dimensions.get('window');

const examTypes = [
  { id: 1, name: 'LGS', icon: 'school-outline', color: '#10b981', description: 'Liselere Geçiş Sınavı' },
  { id: 2, name: 'YKS', icon: 'library-outline', color: '#3b82f6', description: 'Yükseköğretim Kurumları Sınavı' },
  { id: 3, name: 'KPSS', icon: 'briefcase-outline', color: '#8b5cf6', description: 'Kamu Personeli Seçme Sınavı' },
  { id: 4, name: 'Diğer', icon: 'ellipsis-horizontal-outline', color: '#f59e0b', description: 'Diğer sınavlar' },
];

const lgsGrades = [
  { id: 5, name: '5. Sınıf', color: '#10b981' },
  { id: 6, name: '6. Sınıf', color: '#3b82f6' },
  { id: 7, name: '7. Sınıf', color: '#8b5cf6' },
  { id: 8, name: '8. Sınıf', color: '#f59e0b' },
];

const yksGrades = [
  { id: 9, name: '9. Sınıf', color: '#ef4444' },
  { id: 10, name: '10. Sınıf', color: '#06b6d4' },
  { id: 11, name: '11. Sınıf', color: '#84cc16' },
  { id: 12, name: '12. Sınıf', color: '#f97316' },
  { id: 13, name: 'Mezun', color: '#6366f1' },
];

const schoolTypes = [
  { id: 1, name: 'Fen Lisesi', icon: 'flask-outline', color: '#3b82f6' },
  { id: 2, name: 'Anadolu Lisesi', icon: 'school-outline', color: '#10b981' },
  { id: 3, name: 'Diğer', icon: 'ellipsis-horizontal-outline', color: '#f59e0b' },
];

const examFields = [
  { id: 1, name: 'Sayısal', icon: 'calculator-outline', color: '#3b82f6', description: 'Matematik, Fizik, Kimya, Biyoloji' },
  { id: 2, name: 'Sözel', icon: 'book-outline', color: '#10b981', description: 'Türkçe, Tarih, Coğrafya, Felsefe' },
  { id: 3, name: 'Eşit Ağırlık', icon: 'balance-scale-outline', color: '#8b5cf6', description: 'Matematik, Türkçe, Tarih, Coğrafya' },
  { id: 4, name: 'Dil', icon: 'language-outline', color: '#f59e0b', description: 'İngilizce, Almanca, Fransızca' },
];

const genders = [
  { id: 1, name: 'Kadın', icon: 'woman-outline', color: '#ec4899' },
  { id: 2, name: 'Erkek', icon: 'man-outline', color: '#3b82f6' },
  { id: 3, name: 'Diğer', icon: 'person-outline', color: '#6b7280' },
];

// Şehir listesi (backend'deki City enum'ından)
const cities = [
  'Adana', 'Adıyaman', 'Afyon', 'Ağrı', 'Aksaray', 'Amasya', 'Ankara', 'Antalya',
  'Ardahan', 'Artvin', 'Aydın', 'Balıkesir', 'Bartın', 'Batman', 'Bayburt',
  'Bilecik', 'Bingöl', 'Bitlis', 'Bolu', 'Burdur', 'Bursa', 'Çanakkale',
  'Çankırı', 'Çorum', 'Denizli', 'Diyarbakır', 'Düzce', 'Edirne', 'Elazığ',
  'Erzincan', 'Erzurum', 'Eskişehir', 'Gaziantep', 'Giresun', 'Gümüşhane',
  'Hakkari', 'Hatay', 'Iğdır', 'Isparta', 'İstanbul', 'İzmir', 'Kahramanmaraş',
  'Karabük', 'Karaman', 'Kars', 'Kastamonu', 'Kayseri', 'Kırıkkale',
  'Kırklareli', 'Kırşehir', 'Kilis', 'Kocaeli', 'Konya', 'Kütahya', 'Malatya',
  'Manisa', 'Mardin', 'Mersin', 'Muğla', 'Muş', 'Nevşehir', 'Niğde', 'Ordu',
  'Osmaniye', 'Rize', 'Sakarya', 'Samsun', 'Siirt', 'Sinop', 'Sivas',
  'Şanlıurfa', 'Şırnak', 'Tekirdağ', 'Tokat', 'Trabzon', 'Tunceli', 'Uşak',
  'Van', 'Yalova', 'Yozgat', 'Zonguldak', 'Diğer'
];

export const OnboardingScreen: React.FC = () => {
  const navigation = useNavigation();
  const { setOnboardingCompleted, setOnboardingData } = useAuthStore();
  const [currentStep, setCurrentStep] = useState(0);
  const [selectedExamType, setSelectedExamType] = useState<number | null>(null);
  const [selectedGrade, setSelectedGrade] = useState<number | null>(null);
  const [selectedSchoolType, setSelectedSchoolType] = useState<number | null>(null);
  const [selectedExamField, setSelectedExamField] = useState<number | null>(null);
  const [selectedGender, setSelectedGender] = useState<number | null>(null);
  const [selectedCity, setSelectedCity] = useState<string | null>(null);
  const [showCityPicker, setShowCityPicker] = useState(false);
  
  // Animasyon değerleri
  const fadeAnim = useRef(new Animated.Value(0)).current;
  const slideAnim = useRef(new Animated.Value(50)).current;
  const scaleAnim = useRef(new Animated.Value(0.9)).current;
  const progressAnim = useRef(new Animated.Value(0)).current;

  const getSteps = () => {
    const baseSteps = [
      {
        title: 'Hangi sınava hazırlanıyorsun?',
        subtitle: 'Sınav türünü seç ki sana uygun içerikler gösterelim',
        component: 'examType',
      },
    ];

    if (selectedExamType === 1) { // LGS
      baseSteps.push({
        title: 'Hangi sınıftasın?',
        subtitle: 'LGS için sınıfını seç',
        component: 'grade',
      });
    } else if (selectedExamType === 2) { // YKS
      baseSteps.push({
        title: 'Hangi sınıftasın?',
        subtitle: 'YKS için sınıfını seç',
        component: 'grade',
      });
      baseSteps.push({
        title: 'Hangi okul türündesin?',
        subtitle: 'Okul türünü seç',
        component: 'schoolType',
      });
      baseSteps.push({
        title: 'Hangi alanda hazırlanıyorsun?',
        subtitle: 'YKS alanını seç',
        component: 'examField',
      });
    }

    baseSteps.push({
      title: 'Cinsiyetin nedir?',
      subtitle: 'Kişiselleştirme için cinsiyetini seç',
      component: 'gender',
    });

    baseSteps.push({
      title: 'Hangi şehirdesin?',
      subtitle: 'Şehrini seç ki sana yakın içerikler gösterelim',
      component: 'city',
    });

    baseSteps.push({
      title: 'Hemen başlayalım!',
      subtitle: 'Artık hazırsın. Kayıt ol ve öğrenmeye başla!',
      component: 'finish',
    });

    return baseSteps;
  };

  const steps = getSteps();

  // Animasyon efektleri
  useEffect(() => {
    // Sayfa yüklendiğinde animasyon başlat
    Animated.parallel([
      Animated.timing(fadeAnim, {
        toValue: 1,
        duration: 800,
        useNativeDriver: true,
      }),
      Animated.timing(slideAnim, {
        toValue: 0,
        duration: 800,
        useNativeDriver: true,
      }),
      Animated.timing(scaleAnim, {
        toValue: 1,
        duration: 800,
        useNativeDriver: true,
      }),
      Animated.timing(progressAnim, {
        toValue: ((currentStep + 1) / steps.length) * 100,
        duration: 600,
        useNativeDriver: false,
      }),
    ]).start();
  }, [currentStep]);

  // Adım değiştiğinde animasyon
  const animateStepChange = () => {
    // Önce çıkış animasyonu
    Animated.parallel([
      Animated.timing(fadeAnim, {
        toValue: 0,
        duration: 300,
        useNativeDriver: true,
      }),
      Animated.timing(slideAnim, {
        toValue: -50,
        duration: 300,
        useNativeDriver: true,
      }),
    ]).start(() => {
      // Sonra giriş animasyonu
      Animated.parallel([
        Animated.timing(fadeAnim, {
          toValue: 1,
          duration: 400,
          useNativeDriver: true,
        }),
        Animated.timing(slideAnim, {
          toValue: 0,
          duration: 400,
          useNativeDriver: true,
        }),
        Animated.timing(progressAnim, {
          toValue: ((currentStep + 1) / steps.length) * 100,
          duration: 600,
          useNativeDriver: false,
        }),
      ]).start();
    });
  };

  const handleNext = () => {
    if (currentStep < steps.length - 1) {
      animateStepChange();
      setTimeout(() => {
        setCurrentStep(currentStep + 1);
      }, 300);
    } else {
      // Onboarding tamamlandı, verileri store'a kaydet ve register ekranına git
      const onboardingData = {
        examType: getExamTypeString(selectedExamType),
        gradeLevel: getGradeLevelString(selectedGrade),
        schoolType: selectedSchoolType ? getSchoolTypeString(selectedSchoolType) : undefined,
        fieldType: selectedExamField ? getFieldTypeString(selectedExamField) : undefined,
        gender: getGenderString(selectedGender),
        city: selectedCity || '',
      };
      
      setOnboardingData(onboardingData);
      setOnboardingCompleted(true);
      navigation.navigate('Register' as never);
    }
  };

  // Helper functions to convert IDs to backend strings
  const getExamTypeString = (id: number | null): string => {
    switch (id) {
      case 1: return 'LGS';
      case 2: return 'YKS';
      case 3: return 'KPSS';
      case 4: return 'DIGER';
      default: return 'DIGER';
    }
  };

  const getGradeLevelString = (id: number | null): string => {
    switch (id) {
      case 5: return 'G5';
      case 6: return 'G6';
      case 7: return 'G7';
      case 8: return 'G8';
      case 9: return 'G9';
      case 10: return 'G10';
      case 11: return 'G11';
      case 12: return 'G12';
      case 13: return 'MEZUN';
      default: return 'G9';
    }
  };

  const getSchoolTypeString = (id: number): string => {
    switch (id) {
      case 1: return 'Fen';
      case 2: return 'Anadolu';
      case 3: return 'Diğer';
      default: return 'Diğer';
    }
  };

  const getFieldTypeString = (id: number): string => {
    switch (id) {
      case 1: return 'SAYISAL';
      case 2: return 'SOZEL';
      case 3: return 'ESIT_AGIRLIK';
      case 4: return 'DIL';
      default: return 'SAYISAL';
    }
  };

  const getGenderString = (id: number | null): string => {
    switch (id) {
      case 1: return 'KADIN';
      case 2: return 'ERKEK';
      case 3: return 'DIGER';
      default: return 'DIGER';
    }
  };

  const handleBack = () => {
    if (currentStep > 0) {
      setCurrentStep(currentStep - 1);
    }
  };

  const renderExamTypeSelection = () => (
    <Animated.View 
      style={[
        styles.selectionContainer,
        {
          opacity: fadeAnim,
          transform: [
            { translateY: slideAnim },
            { scale: scaleAnim }
          ]
        }
      ]}
    >
      <ScrollView 
        showsVerticalScrollIndicator={false}
        contentContainerStyle={styles.scrollContent}
      >
        {examTypes.map((exam, index) => (
          <Animated.View
            key={exam.id}
            style={{
              opacity: fadeAnim,
              transform: [
                { 
                  translateY: slideAnim.interpolate({
                    inputRange: [0, 50],
                    outputRange: [0, 50 + (index * 20)],
                    extrapolate: 'clamp',
                  })
                },
                { 
                  scale: scaleAnim.interpolate({
                    inputRange: [0.9, 1],
                    outputRange: [0.9, 1],
                    extrapolate: 'clamp',
                  })
                }
              ]
            }}
          >
            <TouchableOpacity
              style={[
                styles.optionCard,
                selectedExamType === exam.id && styles.selectedCard,
              ]}
              onPress={() => {
                setSelectedExamType(exam.id);
                // Seçim animasyonu
                Animated.sequence([
                  Animated.timing(scaleAnim, {
                    toValue: 1.05,
                    duration: 150,
                    useNativeDriver: true,
                  }),
                  Animated.timing(scaleAnim, {
                    toValue: 1,
                    duration: 150,
                    useNativeDriver: true,
                  }),
                ]).start();
              }}
              activeOpacity={0.8}
            >
              <View style={[styles.optionIcon, { backgroundColor: exam.color }]}>
                <Ionicons name={exam.icon as any} size={24} color="#ffffff" />
              </View>
              <View style={styles.optionTextContainer}>
                <Text style={styles.optionText}>{exam.name}</Text>
                <Text style={styles.optionDescription}>{exam.description}</Text>
              </View>
              {selectedExamType === exam.id && (
                <Animated.View
                  style={{
                    transform: [
                      {
                        scale: selectedExamType === exam.id ? 1 : 0,
                      }
                    ]
                  }}
                >
                  <Ionicons name="checkmark-circle" size={24} color={exam.color} />
                </Animated.View>
              )}
            </TouchableOpacity>
          </Animated.View>
        ))}
      </ScrollView>
    </Animated.View>
  );

  const renderSchoolSelection = () => (
    <Animated.View 
      style={[
        styles.selectionContainer,
        {
          opacity: fadeAnim,
          transform: [
            { translateY: slideAnim },
            { scale: scaleAnim }
          ]
        }
      ]}
    >
      <ScrollView 
        showsVerticalScrollIndicator={false}
        contentContainerStyle={styles.scrollContent}
      >
        {schoolTypes.map((school, index) => (
          <Animated.View
            key={school.id}
            style={{
              opacity: fadeAnim,
              transform: [
                { 
                  translateY: slideAnim.interpolate({
                    inputRange: [0, 50],
                    outputRange: [0, 50 + (index * 20)],
                    extrapolate: 'clamp',
                  })
                },
                { 
                  scale: scaleAnim.interpolate({
                    inputRange: [0.9, 1],
                    outputRange: [0.9, 1],
                    extrapolate: 'clamp',
                  })
                }
              ]
            }}
          >
            <TouchableOpacity
              style={[
                styles.optionCard,
                selectedSchoolType === school.id && styles.selectedCard,
              ]}
              onPress={() => {
                setSelectedSchoolType(school.id);
                // Seçim animasyonu
                Animated.sequence([
                  Animated.timing(scaleAnim, {
                    toValue: 1.05,
                    duration: 150,
                    useNativeDriver: true,
                  }),
                  Animated.timing(scaleAnim, {
                    toValue: 1,
                    duration: 150,
                    useNativeDriver: true,
                  }),
                ]).start();
              }}
              activeOpacity={0.8}
            >
              <View style={[styles.optionIcon, { backgroundColor: school.color }]}>
                <Ionicons name={school.icon as any} size={24} color="#ffffff" />
              </View>
              <Text style={styles.optionText}>{school.name}</Text>
              {selectedSchoolType === school.id && (
                <Animated.View
                  style={{
                    transform: [
                      {
                        scale: selectedSchoolType === school.id ? 1 : 0,
                      }
                    ]
                  }}
                >
                  <Ionicons name="checkmark-circle" size={24} color={school.color} />
                </Animated.View>
              )}
            </TouchableOpacity>
          </Animated.View>
        ))}
      </ScrollView>
    </Animated.View>
  );

  const renderGradeSelection = () => {
    const grades = selectedExamType === 1 ? lgsGrades : yksGrades;
    
    return (
      <Animated.View 
        style={[
          styles.gradeContainer,
          {
            opacity: fadeAnim,
            transform: [
              { translateY: slideAnim },
              { scale: scaleAnim }
            ]
          }
        ]}
      >
        {grades.map((grade, index) => (
        <Animated.View
          key={grade.id}
          style={{
            opacity: fadeAnim,
            transform: [
              { 
                translateY: slideAnim.interpolate({
                  inputRange: [0, 50],
                  outputRange: [0, 30 + (index * 10)],
                  extrapolate: 'clamp',
                })
              },
              { 
                scale: scaleAnim.interpolate({
                  inputRange: [0.9, 1],
                  outputRange: [0.9, 1],
                  extrapolate: 'clamp',
                })
              }
            ]
          }}
        >
          <TouchableOpacity
            style={[
              styles.gradeCard,
              { borderColor: grade.color },
              selectedGrade === grade.id && { backgroundColor: grade.color },
            ]}
            onPress={() => {
              setSelectedGrade(grade.id);
              // Seçim animasyonu
              Animated.sequence([
                Animated.timing(scaleAnim, {
                  toValue: 1.1,
                  duration: 150,
                  useNativeDriver: true,
                }),
                Animated.timing(scaleAnim, {
                  toValue: 1,
                  duration: 150,
                  useNativeDriver: true,
                }),
              ]).start();
            }}
            activeOpacity={0.8}
          >
            <Text
              style={[
                styles.gradeText,
                selectedGrade === grade.id && { color: '#ffffff' },
              ]}
            >
              {grade.name}
            </Text>
          </TouchableOpacity>
        </Animated.View>
      ))}
    </Animated.View>
  );
  };

  const renderSchoolTypeSelection = () => (
    <Animated.View 
      style={[
        styles.selectionContainer,
        {
          opacity: fadeAnim,
          transform: [
            { translateY: slideAnim },
            { scale: scaleAnim }
          ]
        }
      ]}
    >
      <ScrollView 
        showsVerticalScrollIndicator={false}
        contentContainerStyle={styles.scrollContent}
      >
        {schoolTypes.map((school, index) => (
          <Animated.View
            key={school.id}
            style={{
              opacity: fadeAnim,
              transform: [
                { 
                  translateY: slideAnim.interpolate({
                    inputRange: [0, 50],
                    outputRange: [0, 50 + (index * 20)],
                    extrapolate: 'clamp',
                  })
                },
                { 
                  scale: scaleAnim.interpolate({
                    inputRange: [0.9, 1],
                    outputRange: [0.9, 1],
                    extrapolate: 'clamp',
                  })
                }
              ]
            }}
          >
            <TouchableOpacity
              style={[
                styles.optionCard,
                selectedSchoolType === school.id && styles.selectedCard,
              ]}
              onPress={() => {
                setSelectedSchoolType(school.id);
                // Seçim animasyonu
                Animated.sequence([
                  Animated.timing(scaleAnim, {
                    toValue: 1.05,
                    duration: 150,
                    useNativeDriver: true,
                  }),
                  Animated.timing(scaleAnim, {
                    toValue: 1,
                    duration: 150,
                    useNativeDriver: true,
                  }),
                ]).start();
              }}
              activeOpacity={0.8}
            >
              <View style={[styles.optionIcon, { backgroundColor: school.color }]}>
                <Ionicons name={school.icon as any} size={24} color="#ffffff" />
              </View>
              <Text style={styles.optionText}>{school.name}</Text>
              {selectedSchoolType === school.id && (
                <Animated.View
                  style={{
                    transform: [
                      {
                        scale: selectedSchoolType === school.id ? 1 : 0,
                      }
                    ]
                  }}
                >
                  <Ionicons name="checkmark-circle" size={24} color={school.color} />
                </Animated.View>
              )}
            </TouchableOpacity>
          </Animated.View>
        ))}
      </ScrollView>
    </Animated.View>
  );

  const renderExamFieldSelection = () => (
    <Animated.View 
      style={[
        styles.selectionContainer,
        {
          opacity: fadeAnim,
          transform: [
            { translateY: slideAnim },
            { scale: scaleAnim }
          ]
        }
      ]}
    >
      <ScrollView 
        showsVerticalScrollIndicator={false}
        contentContainerStyle={styles.scrollContent}
      >
        {examFields.map((field, index) => (
          <Animated.View
            key={field.id}
            style={{
              opacity: fadeAnim,
              transform: [
                { 
                  translateY: slideAnim.interpolate({
                    inputRange: [0, 50],
                    outputRange: [0, 50 + (index * 20)],
                    extrapolate: 'clamp',
                  })
                },
                { 
                  scale: scaleAnim.interpolate({
                    inputRange: [0.9, 1],
                    outputRange: [0.9, 1],
                    extrapolate: 'clamp',
                  })
                }
              ]
            }}
          >
            <TouchableOpacity
              style={[
                styles.optionCard,
                selectedExamField === field.id && styles.selectedCard,
              ]}
              onPress={() => {
                setSelectedExamField(field.id);
                // Seçim animasyonu
                Animated.sequence([
                  Animated.timing(scaleAnim, {
                    toValue: 1.05,
                    duration: 150,
                    useNativeDriver: true,
                  }),
                  Animated.timing(scaleAnim, {
                    toValue: 1,
                    duration: 150,
                    useNativeDriver: true,
                  }),
                ]).start();
              }}
              activeOpacity={0.8}
            >
              <View style={[styles.optionIcon, { backgroundColor: field.color }]}>
                <Ionicons name={field.icon as any} size={24} color="#ffffff" />
              </View>
              <View style={styles.optionTextContainer}>
                <Text style={styles.optionText}>{field.name}</Text>
                <Text style={styles.optionDescription}>{field.description}</Text>
              </View>
              {selectedExamField === field.id && (
                <Animated.View
                  style={{
                    transform: [
                      {
                        scale: selectedExamField === field.id ? 1 : 0,
                      }
                    ]
                  }}
                >
                  <Ionicons name="checkmark-circle" size={24} color={field.color} />
                </Animated.View>
              )}
            </TouchableOpacity>
          </Animated.View>
        ))}
      </ScrollView>
    </Animated.View>
  );

  const renderGenderSelection = () => (
    <Animated.View 
      style={[
        styles.selectionContainer,
        {
          opacity: fadeAnim,
          transform: [
            { translateY: slideAnim },
            { scale: scaleAnim }
          ]
        }
      ]}
    >
      <ScrollView 
        showsVerticalScrollIndicator={false}
        contentContainerStyle={styles.scrollContent}
      >
        {genders.map((gender, index) => (
          <Animated.View
            key={gender.id}
            style={{
              opacity: fadeAnim,
              transform: [
                { 
                  translateY: slideAnim.interpolate({
                    inputRange: [0, 50],
                    outputRange: [0, 50 + (index * 20)],
                    extrapolate: 'clamp',
                  })
                },
                { 
                  scale: scaleAnim.interpolate({
                    inputRange: [0.9, 1],
                    outputRange: [0.9, 1],
                    extrapolate: 'clamp',
                  })
                }
              ]
            }}
          >
            <TouchableOpacity
              style={[
                styles.optionCard,
                selectedGender === gender.id && styles.selectedCard,
              ]}
              onPress={() => {
                setSelectedGender(gender.id);
                // Seçim animasyonu
                Animated.sequence([
                  Animated.timing(scaleAnim, {
                    toValue: 1.05,
                    duration: 150,
                    useNativeDriver: true,
                  }),
                  Animated.timing(scaleAnim, {
                    toValue: 1,
                    duration: 150,
                    useNativeDriver: true,
                  }),
                ]).start();
              }}
              activeOpacity={0.8}
            >
              <View style={[styles.optionIcon, { backgroundColor: gender.color }]}>
                <Ionicons name={gender.icon as any} size={24} color="#ffffff" />
              </View>
              <Text style={styles.optionText}>{gender.name}</Text>
              {selectedGender === gender.id && (
                <Animated.View
                  style={{
                    transform: [
                      {
                        scale: selectedGender === gender.id ? 1 : 0,
                      }
                    ]
                  }}
                >
                  <Ionicons name="checkmark-circle" size={24} color={gender.color} />
                </Animated.View>
              )}
            </TouchableOpacity>
          </Animated.View>
        ))}
      </ScrollView>
    </Animated.View>
  );

  const renderCitySelection = () => (
    <Animated.View 
      style={[
        styles.selectionContainer,
        {
          opacity: fadeAnim,
          transform: [
            { translateY: slideAnim },
            { scale: scaleAnim }
          ]
        }
      ]}
    >
      <TouchableOpacity
        style={styles.cityPickerButton}
        onPress={() => setShowCityPicker(true)}
      >
        <Text style={[styles.cityPickerText, !selectedCity && styles.placeholderText]}>
          {selectedCity || 'Şehir seçiniz'}
        </Text>
        <Ionicons name="chevron-down" size={20} color="#94a3b8" />
      </TouchableOpacity>
      
      {selectedCity && (
        <Animated.View
          style={[
            styles.selectedCityContainer,
            {
              opacity: fadeAnim,
              transform: [{ translateY: slideAnim }]
            }
          ]}
        >
          <Text style={styles.selectedCityText}>Seçilen: {selectedCity}</Text>
        </Animated.View>
      )}
    </Animated.View>
  );

  const renderFinish = () => (
    <Animated.View 
      style={[
        styles.finishContainer,
        {
          opacity: fadeAnim,
          transform: [
            { translateY: slideAnim },
            { scale: scaleAnim }
          ]
        }
      ]}
    >
      <Animated.View 
        style={[
          styles.finishIcon,
          {
            transform: [
              {
                scale: scaleAnim.interpolate({
                  inputRange: [0.9, 1],
                  outputRange: [0.8, 1.2],
                  extrapolate: 'clamp',
                })
              }
            ]
          }
        ]}
      >
        <Ionicons name="rocket" size={64} color="#3b82f6" />
      </Animated.View>
      <Animated.Text 
        style={[
          styles.finishTitle,
          {
            opacity: fadeAnim,
            transform: [{ translateY: slideAnim }]
          }
        ]}
      >
        Harika!
      </Animated.Text>
      <Animated.Text 
        style={[
          styles.finishSubtitle,
          {
            opacity: fadeAnim,
            transform: [{ translateY: slideAnim }]
          }
        ]}
      >
        Seçimlerin kaydedildi. Şimdi hesabını oluştur ve öğrenmeye başla!
      </Animated.Text>
    </Animated.View>
  );

  const canProceed = () => {
    const currentComponent = steps[currentStep]?.component;
    
    switch (currentComponent) {
      case 'examType':
        return selectedExamType !== null;
      case 'grade':
        return selectedGrade !== null;
      case 'schoolType':
        return selectedSchoolType !== null;
      case 'examField':
        return selectedExamField !== null;
      case 'gender':
        return selectedGender !== null;
      case 'city':
        return selectedCity !== null;
      case 'finish':
        return true;
      default:
        return false;
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <LinearGradient
        colors={['#1e293b', '#334155']}
        style={styles.gradient}
      >
        {/* Header */}
        <View style={styles.header}>
          <TouchableOpacity
            style={styles.backButton}
            onPress={handleBack}
            disabled={currentStep === 0}
          >
            <Ionicons
              name="arrow-back"
              size={24}
              color={currentStep === 0 ? '#64748b' : '#ffffff'}
            />
          </TouchableOpacity>
          
          {/* Progress Bar */}
          <View style={styles.progressContainer}>
            <View style={styles.progressBar}>
              <Animated.View
                style={[
                  styles.progressFill,
                  {
                    width: progressAnim.interpolate({
                      inputRange: [0, 100],
                      outputRange: ['0%', '100%'],
                      extrapolate: 'clamp',
                    }),
                  },
                ]}
              />
            </View>
          </View>
        </View>

        {/* Content */}
        <View style={styles.content}>
          <Text style={styles.title}>{steps[currentStep].title}</Text>
          <Text style={styles.subtitle}>{steps[currentStep].subtitle}</Text>

          {steps[currentStep].component === 'examType' && renderExamTypeSelection()}
          {steps[currentStep].component === 'grade' && renderGradeSelection()}
          {steps[currentStep].component === 'schoolType' && renderSchoolTypeSelection()}
          {steps[currentStep].component === 'examField' && renderExamFieldSelection()}
          {steps[currentStep].component === 'gender' && renderGenderSelection()}
          {steps[currentStep].component === 'city' && renderCitySelection()}
          {steps[currentStep].component === 'finish' && renderFinish()}
        </View>

        {/* Footer */}
        <View style={styles.footer}>
          <TouchableOpacity
            style={[
              styles.nextButton,
              !canProceed() && styles.nextButtonDisabled,
            ]}
            onPress={handleNext}
            disabled={!canProceed()}
          >
            <Text style={styles.nextButtonText}>
              {currentStep === steps.length - 1 ? 'Kayıt Ol' : 'Devam Et'}
            </Text>
            <Ionicons name="arrow-forward" size={20} color="#ffffff" />
          </TouchableOpacity>
          
          {/* Giriş Yap Butonu */}
          <TouchableOpacity
            style={styles.loginButton}
            onPress={() => navigation.navigate('Login' as never)}
          >
            <Text style={styles.loginButtonText}>Zaten hesabın var mı? Giriş Yap</Text>
          </TouchableOpacity>
          
          {/* Debug: Reset onboarding */}
          <TouchableOpacity
            style={styles.resetButton}
            onPress={() => setOnboardingCompleted(false)}
          >
            <Text style={styles.resetButtonText}>Reset Onboarding</Text>
          </TouchableOpacity>
        </View>

        {/* Şehir Seçici Modal */}
        {showCityPicker && (
          <View style={styles.modalOverlay}>
            <View style={styles.modalContent}>
              <View style={styles.modalHeader}>
                <Text style={styles.modalTitle}>Şehir Seçiniz</Text>
                <TouchableOpacity
                  onPress={() => setShowCityPicker(false)}
                  style={styles.closeButton}
                >
                  <Ionicons name="close" size={24} color="#64748b" />
                </TouchableOpacity>
              </View>
              <ScrollView style={styles.cityList}>
                {cities.map((city) => (
                  <TouchableOpacity
                    key={city}
                    style={styles.cityItem}
                    onPress={() => {
                      setSelectedCity(city);
                      setShowCityPicker(false);
                    }}
                  >
                    <Text style={styles.cityText}>{city}</Text>
                  </TouchableOpacity>
                ))}
              </ScrollView>
            </View>
          </View>
        )}
      </LinearGradient>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  gradient: {
    flex: 1,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingTop: 10,
    paddingBottom: 20,
  },
  backButton: {
    marginRight: 16,
  },
  progressContainer: {
    flex: 1,
  },
  progressBar: {
    height: 4,
    backgroundColor: '#475569',
    borderRadius: 2,
  },
  progressFill: {
    height: '100%',
    backgroundColor: '#3b82f6',
    borderRadius: 2,
  },
  content: {
    flex: 1,
    paddingHorizontal: 20,
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#ffffff',
    textAlign: 'center',
    marginBottom: 12,
    lineHeight: 36,
  },
  subtitle: {
    fontSize: 16,
    color: '#94a3b8',
    textAlign: 'center',
    marginBottom: 40,
    lineHeight: 24,
  },
  selectionContainer: {
    flex: 1,
  },
  scrollContent: {
    paddingBottom: 20,
  },
  optionCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#475569',
    padding: 16,
    borderRadius: 12,
    borderWidth: 2,
    borderColor: 'transparent',
  },
  selectedCard: {
    borderColor: '#3b82f6',
    backgroundColor: '#1e40af',
  },
  optionIcon: {
    width: 40,
    height: 40,
    borderRadius: 20,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 16,
  },
  optionTextContainer: {
    flex: 1,
  },
  optionText: {
    fontSize: 16,
    color: '#ffffff',
    fontWeight: '500',
  },
  optionDescription: {
    fontSize: 12,
    color: '#94a3b8',
    marginTop: 2,
  },
  gradeContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 12,
  },
  gradeCard: {
    paddingHorizontal: 20,
    paddingVertical: 12,
    borderRadius: 25,
    borderWidth: 2,
    minWidth: (width - 60) / 2,
  },
  gradeText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#ffffff',
    textAlign: 'center',
  },
  finishContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    flex: 1,
  },
  finishIcon: {
    marginBottom: 24,
  },
  finishTitle: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#ffffff',
    marginBottom: 16,
  },
  finishSubtitle: {
    fontSize: 16,
    color: '#94a3b8',
    textAlign: 'center',
    lineHeight: 24,
    paddingHorizontal: 20,
  },
  footer: {
    paddingHorizontal: 20,
    paddingBottom: 40,
  },
  nextButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#3b82f6',
    paddingVertical: 16,
    borderRadius: 12,
    gap: 8,
  },
  nextButtonDisabled: {
    backgroundColor: '#475569',
  },
  nextButtonText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#ffffff',
  },
  loginButton: {
    marginTop: 16,
    paddingVertical: 12,
    paddingHorizontal: 20,
    backgroundColor: 'transparent',
    borderWidth: 1,
    borderColor: '#64748b',
    borderRadius: 8,
    alignSelf: 'center',
  },
  loginButtonText: {
    fontSize: 14,
    color: '#94a3b8',
    textAlign: 'center',
  },
  resetButton: {
    marginTop: 12,
    paddingVertical: 8,
    paddingHorizontal: 16,
    backgroundColor: '#64748b',
    borderRadius: 8,
    alignSelf: 'center',
  },
  resetButtonText: {
    fontSize: 14,
    color: '#ffffff',
    textAlign: 'center',
  },
  // Yeni alanlar için stiller
  cityPickerButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderWidth: 1,
    borderColor: '#64748b',
    borderRadius: 8,
    backgroundColor: '#475569',
  },
  cityPickerText: {
    fontSize: 16,
    color: '#ffffff',
  },
  placeholderText: {
    color: '#94a3b8',
  },
  selectedCityContainer: {
    marginTop: 12,
    padding: 12,
    backgroundColor: '#1e40af',
    borderRadius: 8,
  },
  selectedCityText: {
    fontSize: 14,
    color: '#ffffff',
    textAlign: 'center',
  },
  // Modal stilleri
  modalOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  modalContent: {
    backgroundColor: '#ffffff',
    borderRadius: 12,
    width: '90%',
    maxHeight: '70%',
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.25,
    shadowRadius: 3.84,
    elevation: 5,
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#1e293b',
  },
  closeButton: {
    padding: 4,
  },
  cityList: {
    maxHeight: 300,
  },
  cityItem: {
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#f3f4f6',
  },
  cityText: {
    fontSize: 16,
    color: '#1e293b',
  },
});
