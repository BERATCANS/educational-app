import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { StatusBar } from 'expo-status-bar';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { OnboardingScreen } from './src/screens/OnboardingScreen';
import { LoginScreen } from './src/screens/LoginScreen';
import { RegisterScreen } from './src/screens/RegisterScreen';
import { MainTabsScreen } from './src/screens/MainTabsScreen';
import { FlaggedQuestionsScreen } from './src/screens/FlaggedQuestionsScreen';
import { AddBookScreen } from './src/screens/AddBookScreen';
import { BookDetailScreen } from './src/screens/BookDetailScreen';
import { LibraryScreen } from './src/screens/LibraryScreen';
import { TestScreen } from './src/screens/TestScreen';
import { DailyQuestionScreen } from './src/screens/DailyQuestionScreen';
import { useAuthStore } from './src/store/authStore';

const Stack = createNativeStackNavigator();
const queryClient = new QueryClient();

export default function App() {
  const { isAuthenticated, hasCompletedOnboarding } = useAuthStore();

  return (
    <QueryClientProvider client={queryClient}>
      <GestureHandlerRootView style={{ flex: 1 }}>
        <NavigationContainer>
          <StatusBar style="dark" backgroundColor="#ffffff" />
          <Stack.Navigator
            screenOptions={{
              headerShown: false,
            }}
          >
            {!isAuthenticated ? (
              <>
                <Stack.Screen name="Onboarding" component={OnboardingScreen} />
                <Stack.Screen name="Login" component={LoginScreen} />
                <Stack.Screen name="Register" component={RegisterScreen} />
              </>
            ) : (
              <>
                <Stack.Screen name="MainTabs" component={MainTabsScreen} />
                <Stack.Screen name="FlaggedQuestions" component={FlaggedQuestionsScreen} />
                <Stack.Screen name="AddBook" component={AddBookScreen} />
                <Stack.Screen name="BookDetail" component={BookDetailScreen} />
                <Stack.Screen name="Test" component={TestScreen} />
                <Stack.Screen name="DailyQuestion" component={DailyQuestionScreen} />
              </>
            )}
          </Stack.Navigator>
        </NavigationContainer>
      </GestureHandlerRootView>
    </QueryClientProvider>
  );
}
