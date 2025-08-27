import React, { useState } from 'react';
import { 
  View, 
  Text, 
  TextInput, 
  TouchableOpacity, 
  ViewStyle, 
  TextStyle,
  TextInputProps 
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';

interface InputProps extends Omit<TextInputProps, 'style'> {
  label?: string;
  error?: string;
  leftIcon?: keyof typeof Ionicons.glyphMap;
  rightIcon?: keyof typeof Ionicons.glyphMap;
  onRightIconPress?: () => void;
  containerStyle?: ViewStyle;
  inputStyle?: ViewStyle;
  labelStyle?: TextStyle;
  errorStyle?: TextStyle;
}

export const Input: React.FC<InputProps> = ({
  label,
  error,
  leftIcon,
  rightIcon,
  onRightIconPress,
  containerStyle,
  inputStyle,
  labelStyle,
  errorStyle,
  secureTextEntry,
  ...props
}) => {
  const [isPasswordVisible, setIsPasswordVisible] = useState(false);
  const [isFocused, setIsFocused] = useState(false);

  const togglePasswordVisibility = () => {
    setIsPasswordVisible(!isPasswordVisible);
  };

  const getInputStyle = (): ViewStyle => {
    const baseStyle: ViewStyle = {
      flex: 1,
      fontSize: 16,
      color: '#1f2937',
      paddingVertical: 12,
      paddingHorizontal: 16,
    };

    if (leftIcon) {
      baseStyle.paddingLeft = 48;
    }

    if (rightIcon || secureTextEntry) {
      baseStyle.paddingRight = 48;
    }

    return {
      ...baseStyle,
      ...inputStyle,
    };
  };

  const getContainerStyle = (): ViewStyle => {
    const baseStyle: ViewStyle = {
      borderWidth: 1,
      borderRadius: 8,
      backgroundColor: '#ffffff',
      flexDirection: 'row',
      alignItems: 'center',
    };

    if (isFocused) {
      baseStyle.borderColor = '#3b82f6';
    } else if (error) {
      baseStyle.borderColor = '#ef4444';
    } else {
      baseStyle.borderColor = '#d1d5db';
    }

    return {
      ...baseStyle,
      ...containerStyle,
    };
  };

  return (
    <View style={{ marginBottom: 16 }}>
      {label && (
        <Text style={[
          {
            fontSize: 14,
            fontWeight: '500',
            color: '#374151',
            marginBottom: 8,
          },
          labelStyle
        ]}>
          {label}
        </Text>
      )}
      
      <View style={getContainerStyle()}>
        {leftIcon && (
          <View style={{
            position: 'absolute',
            left: 16,
            zIndex: 1,
          }}>
            <Ionicons name={leftIcon} size={20} color="#6b7280" />
          </View>
        )}

        <TextInput
          style={getInputStyle()}
          secureTextEntry={secureTextEntry && !isPasswordVisible}
          onFocus={() => setIsFocused(true)}
          onBlur={() => setIsFocused(false)}
          placeholderTextColor="#9ca3af"
          testID="input"
          {...props}
        />

        {secureTextEntry && (
          <TouchableOpacity
            style={{
              position: 'absolute',
              right: 16,
              zIndex: 1,
            }}
            onPress={togglePasswordVisibility}
            testID="password-toggle"
          >
            <Ionicons
              name={isPasswordVisible ? 'eye-off' : 'eye'}
              size={20}
              color="#6b7280"
            />
          </TouchableOpacity>
        )}

        {rightIcon && !secureTextEntry && (
          <TouchableOpacity
            style={{
              position: 'absolute',
              right: 16,
              zIndex: 1,
            }}
            onPress={onRightIconPress}
            testID="right-icon"
          >
            <Ionicons name={rightIcon} size={20} color="#6b7280" />
          </TouchableOpacity>
        )}
      </View>

      {error && (
        <Text style={[
          {
            fontSize: 12,
            color: '#ef4444',
            marginTop: 4,
            marginLeft: 4,
          },
          errorStyle
        ]}>
          {error}
        </Text>
      )}
    </View>
  );
};
