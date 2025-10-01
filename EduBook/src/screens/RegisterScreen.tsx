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
import {useForm, Controller, Resolver} from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useNavigation } from '@react-navigation/native';
import { Button } from '../components/Button';
import { Input } from '../components/Input';
import { useAuthStore } from '../store/authStore';
import { apiService } from '../services/api';

// TypeScript enum oluştur
enum Role {
  STUDENT = 'STUDENT',
  TEACHER = 'TEACHER',
  ADMIN = 'ADMIN',
}

// Zod şeması - Sadece temel bilgiler
const registerSchema = z
    .object({
      firstName: z.string().min(2, 'Ad en az 2 karakter olmalı'),
      lastName: z.string().min(2, 'Soyad en az 2 karakter olmalı'),
      email: z.string().email('Geçerli bir email adresi girin'),
      phoneNumber: z.string().min(10, 'Geçerli bir telefon numarası girin'),
      username: z.string().min(3, 'Kullanıcı adı en az 3 karakter olmalı'),
      password: z.string().min(6, 'Şifre en az 6 karakter olmalı'),
      confirmPassword: z.string(),
    })
    .refine((data) => data.password === data.confirmPassword, {
      message: 'Şifreler eşleşmiyor',
      path: ['confirmPassword'],
    });

type RegisterFormData = z.infer<typeof registerSchema>;

export const RegisterScreen: React.FC = () => {
  const [isLoading, setIsLoading] = useState(false);
  const navigation = useNavigation();
  const { setUser, setToken, onboardingData } = useAuthStore();

  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema) as Resolver<RegisterFormData>
  });
  const onSubmit = async (data: RegisterFormData) => {
    setIsLoading(true);
    try {
      // Onboarding'den gelen verileri kullan
      const registerData = {
        firstName: data.firstName,
        lastName: data.lastName,
        email: data.email,
        phoneNumber: data.phoneNumber,
        username: data.username,
        password: data.password,
        role: 'STUDENT', // Varsayılan olarak STUDENT
        gradeLevel: onboardingData?.gradeLevel || 'G9',
        examType: onboardingData?.examType || 'DIGER',
        fieldType: onboardingData?.fieldType,
        gender: onboardingData?.gender || 'DIGER',
        city: onboardingData?.city || 'Diğer',
        schoolType: onboardingData?.schoolType,
      };

      const user = await apiService.register(registerData);

      const loginData = {
        identifier: data.username,
        password: data.password,
      };

      const authResponse = await apiService.login(loginData);

      const userObj = {
        id: user.id,
        firstName: user.firstName,
        lastName: user.lastName,
        email: user.email,
        phoneNumber: user.phoneNumber,
        username: user.username,
        password: '',
        role: user.role as Role,
        ownedBooks: user.ownedBooks || [],
      };

      setUser(userObj);
      setToken(authResponse.token);

      Alert.alert('Başarılı', 'Hesabınız oluşturuldu ve giriş yapıldı!');
    } catch (error) {
      console.error('Register error:', error);
      
      let errorMessage = 'Kayıt olurken bir hata oluştu';
      
      if (error instanceof Error) {
        errorMessage = error.message;
      }
      
      Alert.alert(
        '⚠️ Kayıt Oluşturulamadı',
        errorMessage,
        [
          {
            text: 'Tamam',
            style: 'default'
          }
        ]
      );
    } finally {
      setIsLoading(false);
    }
  };

  const goToLogin = () => {
    navigation.goBack();
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
            <Text style={styles.title}>Hesap Oluştur</Text>
            <Text style={styles.subtitle}>KitApp'e katılmak için bilgilerinizi girin</Text>
          </View>

          <View style={styles.form}>
            <View style={styles.nameRow}>
              <Controller
                  control={control}
                  name="firstName"
                  render={({ field: { onChange, onBlur, value } }) => (
                      <View style={styles.nameInput}>
                        <Input
                            label="Ad"
                            placeholder="Adınız"
                            value={value}
                            onChangeText={onChange}
                            onBlur={onBlur}
                            error={errors.firstName?.message}
                            leftIcon="person"
                        />
                      </View>
                  )}
              />

              <Controller
                  control={control}
                  name="lastName"
                  render={({ field: { onChange, onBlur, value } }) => (
                      <View style={styles.nameInput}>
                        <Input
                            label="Soyad"
                            placeholder="Soyadınız"
                            value={value}
                            onChangeText={onChange}
                            onBlur={onBlur}
                            error={errors.lastName?.message}
                            leftIcon="person"
                        />
                      </View>
                  )}
              />
            </View>

            <Controller
                control={control}
                name="email"
                render={({ field: { onChange, onBlur, value } }) => (
                    <Input
                        label="Email"
                        placeholder="email@example.com"
                        value={value}
                        onChangeText={onChange}
                        onBlur={onBlur}
                        error={errors.email?.message}
                        leftIcon="mail"
                        keyboardType="email-address"
                        autoCapitalize="none"
                        autoCorrect={false}
                    />
                )}
            />

            <Controller
                control={control}
                name="phoneNumber"
                render={({ field: { onChange, onBlur, value } }) => (
                    <Input
                        label="Telefon Numarası"
                        placeholder="+90 5XX XXX XX XX"
                        value={value}
                        onChangeText={onChange}
                        onBlur={onBlur}
                        error={errors.phoneNumber?.message}
                        leftIcon="call"
                        keyboardType="phone-pad"
                    />
                )}
            />

            <Controller
                control={control}
                name="username"
                render={({ field: { onChange, onBlur, value } }) => (
                    <Input
                        label="Kullanıcı Adı"
                        placeholder="kullanici_adi"
                        value={value}
                        onChangeText={onChange}
                        onBlur={onBlur}
                        error={errors.username?.message}
                        leftIcon="at"
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

            <Controller
                control={control}
                name="confirmPassword"
                render={({ field: { onChange, onBlur, value } }) => (
                    <Input
                        label="Şifre Tekrar"
                        placeholder="Şifrenizi tekrar girin"
                        value={value}
                        onChangeText={onChange}
                        onBlur={onBlur}
                        error={errors.confirmPassword?.message}
                        leftIcon="lock-closed"
                        secureTextEntry
                    />
                )}
            />


            <Button
                title="Hesap Oluştur"
                onPress={handleSubmit(onSubmit)}
                loading={isLoading}
                style={styles.registerButton}
            />

            <View style={styles.loginSection}>
              <Text style={styles.loginText}>Zaten hesabınız var mı?</Text>
              <Button
                  title="Giriş Yap"
                  onPress={goToLogin}
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
    padding: 24,
  },
  header: {
    alignItems: 'center',
    marginBottom: 32,
    marginTop: 20,
  },
  title: {
    fontSize: 28,
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
  nameRow: {
    flexDirection: 'row',
    gap: 12,
  },
  nameInput: {
    flex: 1,
  },
  registerButton: {
    marginTop: 8,
  },
  loginSection: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 24,
    gap: 12,
  },
  loginText: {
    fontSize: 14,
    color: '#64748b',
  },
});
