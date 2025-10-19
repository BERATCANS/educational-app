import React, { useState } from 'react';
import { View, StyleSheet, TouchableOpacity, Modal, Text, ActivityIndicator } from 'react-native';
import { WebView } from 'react-native-webview';
import { Ionicons } from '@expo/vector-icons';
import { Base64 } from 'js-base64';

interface VideoPlayerProps {
  videoUrl: string;
  visible: boolean;
  onClose: () => void;
  isFullScreen?: boolean;
  onFullScreenToggle?: () => void;
  style?: any;
}

export const VideoPlayer: React.FC<VideoPlayerProps> = ({
  videoUrl,
  visible,
  onClose,
  isFullScreen = false,
  onFullScreenToggle,
  style
}) => {
  const [isLoading, setIsLoading] = useState(true);
  const [hasError, setHasError] = useState(false);

  // YouTube URL'ini embed formatına çevir
  const getEmbedUrl = (url: string) => {
    let videoId = '';
    
    if (url.includes('youtube.com/watch')) {
      videoId = url.split('v=')[1]?.split('&')[0] || '';
    } else if (url.includes('youtu.be/')) {
      videoId = url.split('youtu.be/')[1]?.split('?')[0] || '';
    }
    
    if (videoId) {
      return `https://www.youtube-nocookie.com/embed/${videoId}?autoplay=1&rel=0&modestbranding=1&showinfo=0&controls=1&fs=1&playsinline=1&iv_load_policy=3&cc_load_policy=0&color=white&theme=dark`;
    }
    
    return url;
  };

  const handleVideoLoad = () => {
    setIsLoading(false);
    setHasError(false);
  };

  const handleVideoError = (error: any) => {
    console.log('Video error:', error);
    setIsLoading(false);
    setHasError(true);
  };

  if (!visible) return null;

  const renderVideo = () => (
    <View style={[styles.container, !isFullScreen && style]}>
      {isLoading && (
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color="#ff0000" />
          <Text style={styles.loadingText}>Video yükleniyor...</Text>
        </View>
      )}

      {hasError ? (
        <View style={styles.errorContainer}>
          <Ionicons name="alert-circle" size={48} color="#ff6b6b" />
          <Text style={styles.errorTitle}>Video Yüklenemedi</Text>
          <Text style={styles.errorText}>
            Video oynatılırken bir hata oluştu. Lütfen tekrar deneyin.
          </Text>
          <TouchableOpacity style={styles.retryButton} onPress={() => {
            setHasError(false);
            setIsLoading(true);
          }}>
            <Text style={styles.retryButtonText}>Tekrar Dene</Text>
          </TouchableOpacity>
        </View>
      ) : (
        <WebView
          source={{ uri: getEmbedUrl(videoUrl) }}
          style={styles.video}
          onLoad={handleVideoLoad}
          onError={handleVideoError}
          allowsFullscreenVideo={true}
          mediaPlaybackRequiresUserAction={false}
          javaScriptEnabled={true}
          domStorageEnabled={true}
          startInLoadingState={true}
          scrollEnabled={false}
          bounces={false}
          showsHorizontalScrollIndicator={false}
          showsVerticalScrollIndicator={false}
          androidLayerType="hardware"
          renderLoading={() => (
            <View style={styles.loadingContainer}>
              <ActivityIndicator size="large" color="#ff0000" />
              <Text style={styles.loadingText}>Video yükleniyor...</Text>
            </View>
          )}
        />
      )}
      
      <View style={styles.controls}>
        {onFullScreenToggle && (
          <TouchableOpacity style={styles.fullScreenButton} onPress={onFullScreenToggle}>
            <Ionicons name={isFullScreen ? "contract" : "expand"} size={24} color="#fff" />
          </TouchableOpacity>
        )}
        <TouchableOpacity style={styles.closeButton} onPress={onClose}>
          <Ionicons name="close" size={24} color="#fff" />
        </TouchableOpacity>
      </View>
    </View>
  );

  if (isFullScreen) {
    return (
      <Modal 
        visible={visible} 
        animationType="slide" 
        presentationStyle="fullScreen"
        onRequestClose={onClose}
      >
        {renderVideo()}
      </Modal>
    );
  }

  return renderVideo();
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000',
    position: 'relative',
  },
  video: {
    flex: 1,
  },
  loadingContainer: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#000',
    zIndex: 1,
  },
  loadingText: {
    color: '#fff',
    fontSize: 16,
    marginTop: 16,
  },
  errorContainer: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#000',
    padding: 20,
    zIndex: 1,
  },
  errorTitle: {
    color: '#fff',
    fontSize: 20,
    fontWeight: 'bold',
    marginTop: 16,
    marginBottom: 8,
  },
  errorText: {
    color: '#ccc',
    fontSize: 16,
    textAlign: 'center',
    lineHeight: 24,
    marginBottom: 24,
  },
  retryButton: {
    backgroundColor: '#ff0000',
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderRadius: 8,
  },
  retryButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  controls: {
    position: 'absolute',
    top: 16,
    right: 16,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    zIndex: 2,
  },
  closeButton: {
    backgroundColor: 'rgba(0,0,0,0.7)',
    borderRadius: 20,
    padding: 10,
  },
  fullScreenButton: {
    backgroundColor: 'rgba(0,0,0,0.7)',
    borderRadius: 20,
    padding: 10,
  },
});
