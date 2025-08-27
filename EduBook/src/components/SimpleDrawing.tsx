import React, { useState } from 'react';
import { View, StyleSheet } from 'react-native';
import { PanGestureHandler, State } from 'react-native-gesture-handler';
import Svg, { Path, Circle } from 'react-native-svg';

interface SimpleDrawingProps {
  isActive: boolean;
  currentColor: string;
  currentThickness: number;
  currentTool: 'pen' | 'eraser' | 'line';
  onPathsChange?: (paths: any[]) => void;
  paths?: any[];
}

export const SimpleDrawing: React.FC<SimpleDrawingProps> = ({
  isActive,
  currentColor,
  currentThickness,
  currentTool,
  onPathsChange,
  paths = []
}) => {
  const [currentPath, setCurrentPath] = useState<string>('');
  const [isDrawing, setIsDrawing] = useState(false);
  const [lineStartPoint, setLineStartPoint] = useState<{x: number, y: number} | null>(null);

  // Bir noktanın path üzerinde olup olmadığını kontrol et
  const isPointOnPath = (x: number, y: number, pathData: string, thickness: number) => {
    const points = pathData.match(/[ML]\s*([0-9.]+),([0-9.]+)/g);
    if (!points) return false;

    const tolerance = thickness + 10; // Silme alanı biraz daha geniş

    // Önce noktaları parse et
    const parsedPoints = points.map(point => {
      const coords = point.replace(/[ML]\s*/, '').split(',');
      return {
        x: parseFloat(coords[0]),
        y: parseFloat(coords[1])
      };
    });

    // Her nokta için mesafeyi kontrol et
    for (const point of parsedPoints) {
      const distance = Math.sqrt((x - point.x) ** 2 + (y - point.y) ** 2);
      if (distance <= tolerance) {
        return true;
      }
    }

    // Çizgi segmentleri için - her iki nokta arasındaki çizgiyi kontrol et
    for (let i = 1; i < parsedPoints.length; i++) {
      const p1 = parsedPoints[i - 1];
      const p2 = parsedPoints[i];
      
      // Çizgi segmentine olan en kısa mesafeyi hesapla
      const distanceToLine = distanceFromPointToLineSegment(x, y, p1.x, p1.y, p2.x, p2.y);
      if (distanceToLine <= tolerance) {
        return true;
      }
    }

    return false;
  };

  // Bir noktanın çizgi segmentine olan en kısa mesafesini hesapla
  const distanceFromPointToLineSegment = (px: number, py: number, x1: number, y1: number, x2: number, y2: number) => {
    const A = px - x1;
    const B = py - y1;
    const C = x2 - x1;
    const D = y2 - y1;

    const dot = A * C + B * D;
    const lenSq = C * C + D * D;
    
    if (lenSq === 0) {
      // Nokta ile nokta arasındaki mesafe
      return Math.sqrt(A * A + B * B);
    }

    let param = dot / lenSq;

    let xx, yy;

    if (param < 0) {
      xx = x1;
      yy = y1;
    } else if (param > 1) {
      xx = x2;
      yy = y2;
    } else {
      xx = x1 + param * C;
      yy = y1 + param * D;
    }

    const dx = px - xx;
    const dy = py - yy;
    return Math.sqrt(dx * dx + dy * dy);
  };

  const handleGesture = (event: any) => {
    if (!isActive) return;

    const { x, y, state } = event.nativeEvent;
    


    if (currentTool === 'line') {
      // Çizgi modu
      if (state === State.BEGAN) {
        if (!lineStartPoint) {
          // İlk tıklama - başlangıç noktası
          setLineStartPoint({ x, y });
        } else {
          // İkinci tıklama - çizgiyi tamamla
          const linePath = `M${lineStartPoint.x},${lineStartPoint.y} L${x},${y}`;
          const newPathObj = {
            id: Date.now().toString(),
            path: linePath,
            color: currentColor,
            thickness: currentThickness
          };
          const newPaths = [...paths, newPathObj];
          onPathsChange?.(newPaths);
          setLineStartPoint(null);
        }
      }
    } else if (currentTool === 'eraser') {
      // Silgi modu - dokunulan çizgileri sil
      if (state === State.BEGAN || state === State.ACTIVE) {
        const pathsToDelete: string[] = [];
        
        // Hangi path'lerin silineceğini bul
        paths.forEach((pathObj) => {
          if (isPointOnPath(x, y, pathObj.path, pathObj.thickness)) {
            pathsToDelete.push(pathObj.id);
          }
        });

        if (pathsToDelete.length > 0) {
          // Bulunan path'leri sil
          const newPaths = paths.filter(pathObj => !pathsToDelete.includes(pathObj.id));
          onPathsChange?.(newPaths);
        }
      }
    } else {
      // Normal pen modu
      if (state === State.BEGAN) {
        setIsDrawing(true);
        const newPath = `M${x},${y}`;
        setCurrentPath(newPath);
      } else if (state === State.ACTIVE && isDrawing) {
        const newPath = currentPath + ` L${x},${y}`;
        setCurrentPath(newPath);
      } else if (state === State.END && isDrawing) {
        setIsDrawing(false);
        if (currentPath) {
          const newPathObj = {
            id: Date.now().toString(),
            path: currentPath,
            color: currentColor,
            thickness: currentThickness
          };
          const newPaths = [...paths, newPathObj];
          onPathsChange?.(newPaths);
          setCurrentPath('');
        }
      }
    }
  };

  if (!isActive) return null;

  return (
    <PanGestureHandler 
      onGestureEvent={handleGesture}
      onHandlerStateChange={handleGesture}
      shouldCancelWhenOutside={false}
    >
      <View style={styles.container}>
        <Svg style={styles.svg}>
          {/* Render all completed paths */}
          {paths.map((pathObj) => (
            <Path
              key={pathObj.id}
              d={pathObj.path}
              stroke={pathObj.color}
              strokeWidth={pathObj.thickness}
              fill="none"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          ))}
          
          {/* Render current drawing path */}
          {currentPath && (
            <Path
              d={currentPath}
              stroke={currentTool === 'eraser' ? '#ffffff' : currentColor}
              strokeWidth={currentTool === 'eraser' ? 20 : currentThickness}
              fill="none"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          )}
          
          {/* Render line start point indicator */}
          {currentTool === 'line' && lineStartPoint && (
            <Circle
              cx={lineStartPoint.x}
              cy={lineStartPoint.y}
              r="4"
              fill={currentColor}
              stroke="#ffffff"
              strokeWidth="2"
            />
          )}
        </Svg>
      </View>
    </PanGestureHandler>
  );
};

const styles = StyleSheet.create({
  container: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    zIndex: 1000,
  },
  svg: {
    width: '100%',
    height: '100%',
  },
});
