import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  Alert,
} from 'react-native';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useNavigation } from '@react-navigation/native';
import { Button } from '../components/Button';
import { Input } from '../components/Input';
import { useAuthStore } from '../store/authStore';
import { apiService } from '../services/api';

const loginSchema = z.object({
  identifier: z.string().min(1, 'Kullanıcı adı, email veya telefon gerekli'),
  password: z.string().min(1, 'Şifre gerekli'),
});

type LoginFormData = z.infer<typeof loginSchema>;

export const LoginScreen: React.FC = () => {
  const [isLoading, setIsLoading] = useState(false);
  const navigation = useNavigation();
  const { setUser, setToken } = useAuthStore();

  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data: LoginFormData) => {
    setIsLoading(true);
    try {
      // Gerçek API çağrısı
      const response = await apiService.login(data);
      
      // Backend'den gelen response'u User objesine çevir
      const user = {
        id: 1, // Backend'den gelecek - şimdilik mock
        firstName: '', // Backend'den gelecek - şimdilik boş
        lastName: '', // Backend'den gelecek - şimdilik boş
        email: data.identifier.includes('@') ? data.identifier : '',
        phoneNumber: data.identifier.match(/^\d+$/) ? data.identifier : '',
        username: response.username,
        password: '',
        role: response.role as 'STUDENT' | 'TEACHER' | 'ADMIN',
        ownedBooks: [],
      };

      // Store'a kaydet
      setUser(user);
      setToken(response.token);
      
      Alert.alert('Başarılı', 'Giriş yapıldı!');
    } catch (error) {
      console.error('Login error:', error);
      Alert.alert(
        'Giriş Hatası',
        error instanceof Error ? error.message : 'Giriş yapılırken bir hata oluştu'
      );
    } finally {
      setIsLoading(false);
    }
  };

  const goToRegister = () => {
    navigation.navigate('Register' as never);
  };


  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      <ScrollView
        contentContainerStyle={styles.scrollContent}
        keyboardShouldPersistTaps="handled"
      >
        <View style={styles.header}>
          <Text style={styles.title}>EduBook</Text>
          <Text style={styles.subtitle}>Eğitim yolculuğuna hoş geldiniz</Text>
        </View>

        <View style={styles.form}>
          <Controller
            control={control}
            name="identifier"
            render={({ field: { onChange, onBlur, value } }) => (
              <Input
                label="Kullanıcı Adı, Email veya Telefon"
                placeholder="Kullanıcı adı, email veya telefon girin"
                value={value}
                onChangeText={onChange}
                onBlur={onBlur}
                error={errors.identifier?.message}
                leftIcon="person"
                autoCapitalize="none"
                autoCorrect={false}
              />
            )}
          />

          <Controller
            control={control}
            name="password"
            render={({ field: { onChange, onBlur, value } }) => (
              <Input
                label="Şifre"
                placeholder="Şifrenizi girin"
                value={value}
                onChangeText={onChange}
                onBlur={onBlur}
                error={errors.password?.message}
                leftIcon="lock-closed"
                secureTextEntry
              />
            )}
          />

          <Button
            title="Giriş Yap"
            onPress={handleSubmit(onSubmit)}
            loading={isLoading}
            style={styles.loginButton}
          />


          <View style={styles.registerSection}>
            <Text style={styles.registerText}>Hesabınız yok mu?</Text>
            <Button
              title="Kayıt Ol"
              onPress={goToRegister}
              variant="outline"
              size="small"
            />
          </View>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8fafc',
  },
  scrollContent: {
    flexGrow: 1,
    justifyContent: 'center',
    padding: 24,
  },
  header: {
    alignItems: 'center',
    marginBottom: 48,
  },
  title: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#1e293b',
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 16,
    color: '#64748b',
    textAlign: 'center',
  },
  form: {
    backgroundColor: '#ffffff',
    borderRadius: 16,
    padding: 24,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 4,
  },
  loginButton: {
    marginTop: 8,
  },
  demoSection: {
    alignItems: 'center',
    marginTop: 20,
    marginBottom: 20,
  },
  demoText: {
    fontSize: 14,
    color: '#64748b',
    marginBottom: 12,
  },
  registerSection: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 24,
    gap: 12,
  },
  registerText: {
    fontSize: 14,
    color: '#64748b',
  },
});
