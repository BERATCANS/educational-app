import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { Ionicons } from '@expo/vector-icons';
import { View } from 'react-native';
import { DashboardScreen } from './DashboardScreen';
import { LibraryScreen } from './LibraryScreen';
import { ProfileScreen } from './ProfileScreen';
import { BookStoreScreen } from './BookStoreScreen';
import { CartScreen } from './CartScreen';
import { CartBadge } from '../components/CartBadge';
import { useCartStore } from '../store/cartStore';

const Tab = createBottomTabNavigator();

export const MainTabsScreen: React.FC = () => {
  const itemCount = useCartStore(state => state.itemCount);
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        tabBarIcon: ({ focused, color, size }) => {
          let iconName: keyof typeof Ionicons.glyphMap;

          if (route.name === 'Dashboard') {
            iconName = focused ? 'home' : 'home-outline';
          } else if (route.name === 'Library') {
            iconName = focused ? 'library' : 'library-outline';
          } else if (route.name === 'BookStore') {
            iconName = focused ? 'book' : 'book-outline';
          } else if (route.name === 'Cart') {
            iconName = focused ? 'cart' : 'cart-outline';
          } else if (route.name === 'Profile') {
            iconName = focused ? 'person' : 'person-outline';
          } else {
            iconName = 'help-outline';
          }

          return <Ionicons name={iconName} size={size} color={color} />;
        },
        tabBarActiveTintColor: '#3b82f6',
        tabBarInactiveTintColor: '#9ca3af',
        tabBarStyle: {
          backgroundColor: '#ffffff',
          borderTopWidth: 1,
          borderTopColor: '#e2e8f0',
          paddingBottom: 8,
          paddingTop: 8,
          height: 60,
        },
        headerShown: false,
      })}
    >
      <Tab.Screen 
        name="Dashboard" 
        component={DashboardScreen}
        options={{
          title: 'Ana Sayfa',
          tabBarLabel: 'Ana Sayfa',
        }}
      />
      <Tab.Screen 
        name="Library" 
        component={LibraryScreen}
        options={{
          title: 'Kütüphane',
          tabBarLabel: 'Kütüphane',
        }}
      />
      <Tab.Screen 
        name="BookStore" 
        component={BookStoreScreen}
        options={{
          title: 'Kitaplar',
          tabBarLabel: 'Kitaplar',
        }}
      />
      <Tab.Screen 
        name="Cart" 
        component={CartScreen}
        options={{
          title: 'Sepetim',
          tabBarLabel: 'Sepetim',
          tabBarIcon: ({ focused, color, size }) => (
            <View>
              <Ionicons 
                name={focused ? 'cart' : 'cart-outline'} 
                size={size} 
                color={color} 
              />
              <CartBadge count={itemCount} />
            </View>
          ),
        }}
      />
      <Tab.Screen 
        name="Profile" 
        component={ProfileScreen}
        options={{
          title: 'Profil',
          tabBarLabel: 'Profil',
        }}
      />
    </Tab.Navigator>
  );
};
