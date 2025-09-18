import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  Alert,
  TouchableOpacity,
  ScrollView,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Button } from '../components/Button';
import { Input } from '../components/Input';
import { apiService } from '../services/api';

export const AddBookScreen: React.FC = () => {
  const [redeemCode, setRedeemCode] = useState('');
  const navigation = useNavigation();

  const submitRedeemCode = async () => {
    if (redeemCode.trim().length === 0) {
      Alert.alert('Hata', 'Lütfen bir kod girin');
      return;
    }

    try {
      await apiService.redeemCode(redeemCode);
      Alert.alert(
        'Başarılı!',
        'Kitap kodunuz doğrulandı ve kütüphanenize eklendi.',
        [
          {
            text: 'Tamam',
            onPress: () => {
              navigation.goBack();
            },
          },
        ]
      );
    } catch (error) {
      Alert.alert(
        'Hata',
        (error as Error).message || 'Geçersiz kod. Lütfen tekrar deneyin.'
      );
    }
  };

  const goBack = () => {
    navigation.goBack();
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.container}>
        {/* Header */}
        <View style={styles.header}>
          <TouchableOpacity onPress={goBack} style={styles.backButton}>
            <Ionicons name="arrow-back" size={24} color="#1e293b" />
          </TouchableOpacity>
          <Text style={styles.headerTitle}>Kitap Ekle</Text>
          <View style={styles.placeholder} />
        </View>

        {/* Content */}
        <ScrollView 
          style={styles.scrollView}
          contentContainerStyle={styles.scrollContent}
          showsVerticalScrollIndicator={false}
          keyboardShouldPersistTaps="handled"
        >
          <View style={styles.content}>
            <View style={styles.iconContainer}>
              <Ionicons name="add-circle" size={80} color="#3b82f6" />
            </View>

            <Text style={styles.title}>Yeni Kitap Ekle</Text>
            <Text style={styles.subtitle}>
              Kitap kodunuzu girerek kütüphanenize yeni kitap ekleyin
            </Text>

            <View style={styles.form}>
              <Input
                label="Kitap Kodu"
                placeholder="Örn: DEMO123"
                value={redeemCode}
                onChangeText={setRedeemCode}
                leftIcon="key"
                autoCapitalize="characters"
                autoCorrect={false}
              />

              <Button
                title="Kodu Doğrula"
                onPress={submitRedeemCode}
                style={styles.submitButton}
              />
            </View>


            <View style={styles.helpSection}>
              <Text style={styles.helpTitle}>Nasıl Kitap Eklersiniz?</Text>
              <View style={styles.helpSteps}>
                <View style={styles.helpStep}>
                  <View style={styles.stepNumber}>
                    <Text style={styles.stepNumberText}>1</Text>
                  </View>
                  <Text style={styles.stepText}>
                    Kitap satın aldığınızda size bir kod verilir
                  </Text>
                </View>
                <View style={styles.helpStep}>
                  <View style={styles.stepNumber}>
                    <Text style={styles.stepNumberText}>2</Text>
                  </View>
                  <Text style={styles.stepText}>
                    Bu kodu yukarıdaki alana girin
                  </Text>
                </View>
                <View style={styles.helpStep}>
                  <View style={styles.stepNumber}>
                    <Text style={styles.stepNumberText}>3</Text>
                  </View>
                  <Text style={styles.stepText}>
                    Kitap otomatik olarak kütüphanenize eklenir
                  </Text>
                </View>
              </View>
            </View>
          </View>
        </ScrollView>
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
    justifyContent: 'space-between',
    backgroundColor: '#ffffff',
    padding: 20,
    borderBottomWidth: 1,
    borderBottomColor: '#e2e8f0',
  },
  backButton: {
    padding: 4,
  },
  headerTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#1e293b',
  },
  placeholder: {
    width: 32,
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    flexGrow: 1,
    padding: 24,
  },
  content: {
    flex: 1,
  },
  iconContainer: {
    alignItems: 'center',
    marginBottom: 24,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#1e293b',
    textAlign: 'center',
    marginBottom: 12,
  },
  subtitle: {
    fontSize: 16,
    color: '#64748b',
    textAlign: 'center',
    marginBottom: 32,
    lineHeight: 24,
  },
  form: {
    marginBottom: 32,
  },
  submitButton: {
    marginTop: 16,
  },
  helpSection: {
    backgroundColor: '#ffffff',
    padding: 20,
    borderRadius: 16,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 4,
  },
  helpTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#1e293b',
    marginBottom: 20,
    textAlign: 'center',
  },
  helpSteps: {
    gap: 16,
  },
  helpStep: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  stepNumber: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: '#3b82f6',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 16,
  },
  stepNumberText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: 'bold',
  },
  stepText: {
    fontSize: 14,
    color: '#374151',
    flex: 1,
    lineHeight: 20,
  },
});
