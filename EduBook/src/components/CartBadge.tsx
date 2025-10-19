import React from 'react';
import { View, Text, StyleSheet } from 'react-native';

interface CartBadgeProps {
  count: number;
}

export const CartBadge: React.FC<CartBadgeProps> = ({ count }) => {
  if (count <= 0) return null;

  return (
    <View style={styles.container}>
      <Text style={styles.text}>
        {count > 99 ? '99+' : count}
      </Text>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    position: 'absolute',
    top: -6,
    right: -6,
    minWidth: 18,
    height: 18,
    borderRadius: 9,
    backgroundColor: '#ef4444',
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 4,
  },
  text: {
    color: '#fff',
    fontSize: 12,
    fontWeight: '600',
  },
});
