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

const loginSchema = z.object({
  username: z.string().min(1, 'Kullanıcı adı gerekli'),
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
      // Mock login - herhangi bir kullanıcı adı/şifre ile giriş yapılabilir
      const mockUser = {
        id: 1,
        firstName: 'Ahmet',
        lastName: 'Yılmaz',
        email: 'ahmet@example.com',
        phoneNumber: '+905551234567',
        username: data.username,
        password: 'hashed-password',
        role: 'STUDENT' as const,
        ownedBooks: [],
      };
      const mockToken = 'mock-jwt-token-' + Date.now();
      setToken(mockToken);
      setUser(mockUser);
      // Navigation otomatik olarak App.tsx'teki isAuthenticated değişimi ile tetiklenecek
    } catch (error) {
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

  // Demo giriş butonu
  const handleDemoLogin = () => {
    const mockUser = {
      id: 1,
      firstName: 'Demo',
      lastName: 'Kullanıcı',
      email: 'demo@edubook.com',
      phoneNumber: '+905551234567',
      username: 'demo_user',
      password: 'hashed-password',
      role: 'STUDENT' as const,
      ownedBooks: [],
    };
    const mockToken = 'demo-token-' + Date.now();
    setToken(mockToken);
    setUser(mockUser);
    // Navigation otomatik olarak App.tsx'teki isAuthenticated değişimi ile tetiklenecek
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
            name="username"
            render={({ field: { onChange, onBlur, value } }) => (
              <Input
                label="Kullanıcı Adı"
                placeholder="Kullanıcı adınızı girin"
                value={value}
                onChangeText={onChange}
                onBlur={onBlur}
                error={errors.username?.message}
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

          <View style={styles.demoSection}>
            <Text style={styles.demoText}>veya</Text>
            <Button
              title="Demo ile Giriş Yap"
              onPress={handleDemoLogin}
              variant="secondary"
              size="small"
            />
          </View>

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
