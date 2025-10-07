import React, { useState } from 'react';
import { View, StyleSheet } from 'react-native';
import { PanGestureHandler, State } from 'react-native-gesture-handler';
import Svg, { Path, Circle } from 'react-native-svg';

interface SimpleDrawingProps {
  isActive: boolean;
  currentColor: string;
  currentThickness: number;
  currentTool: 'pen' | 'eraser' | 'line' | 'triangle' | 'circle' | 'rectangle' | 'square' | 'highlighter';
  onPathsChange?: (paths: any[]) => void;
  paths?: any[];
  onShapeStart?: (x: number, y: number) => void;
  onShapeEnd?: (x: number, y: number) => void;
}

export const SimpleDrawing: React.FC<SimpleDrawingProps> = ({
  isActive,
  currentColor,
  currentThickness,
  currentTool,
  onPathsChange,
  paths = [],
  onShapeStart,
  onShapeEnd
}) => {
  const [currentPath, setCurrentPath] = useState<string>('');
  const [isDrawing, setIsDrawing] = useState(false);
  const [lineStartPoint, setLineStartPoint] = useState<{x: number, y: number} | null>(null);

  // Şekil çizme fonksiyonları
  const generateShapePath = (startX: number, startY: number, endX: number, endY: number, shapeType: string): string => {
    const width = Math.abs(endX - startX);
    const height = Math.abs(endY - startY);
    const minX = Math.min(startX, endX);
    const minY = Math.min(startY, endY);

    switch (shapeType) {
      case 'triangle':
        const centerX = minX + width / 2;
        const topY = minY;
        const leftX = minX;
        const rightX = minX + width;
        const bottomY = minY + height;
        return `M${centerX},${topY} L${rightX},${bottomY} L${leftX},${bottomY} Z`;
      
      case 'circle':
        // Merkez: başlangıç noktası (startX, startY)
        // Yarıçap: başlangıç noktasından bitiş noktasına olan mesafe
        const radius = Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));
        const centerX_circle = startX;
        const centerY_circle = startY;
        return `M${centerX_circle - radius},${centerY_circle} A${radius},${radius} 0 1,1 ${centerX_circle + radius},${centerY_circle} A${radius},${radius} 0 1,1 ${centerX_circle - radius},${centerY_circle}`;
      
      case 'rectangle':
        return `M${minX},${minY} L${minX + width},${minY} L${minX + width},${minY + height} L${minX},${minY + height} Z`;
      
      case 'square':
        const size = Math.min(width, height);
        return `M${minX},${minY} L${minX + size},${minY} L${minX + size},${minY + size} L${minX},${minY + size} Z`;
      
      default:
        return '';
    }
  };

  // Bir noktanın path üzerinde olup olmadığını kontrol et
  const isPointOnPath = (x: number, y: number, pathData: string, thickness: number) => {
    const tolerance = thickness + 10; // Silme alanı biraz daha geniş

    // Çember kontrolü - A komutları içeriyorsa çember
    if (pathData.includes('A')) {
      // Çember path'ini parse et: M${centerX - radius},${centerY} A${radius},${radius} 0 1,1 ${centerX + radius},${centerY} A${radius},${radius} 0 1,1 ${centerX - radius},${centerY}
      const match = pathData.match(/M([0-9.-]+),([0-9.-]+)\s+A([0-9.-]+),([0-9.-]+)/);
      if (match) {
        const centerX = parseFloat(match[1]) + parseFloat(match[3]); // centerX - radius + radius = centerX
        const centerY = parseFloat(match[2]);
        const radius = parseFloat(match[3]);
        
        // Nokta çemberin üzerinde mi kontrol et
        const distance = Math.sqrt((x - centerX) ** 2 + (y - centerY) ** 2);
        return distance <= radius + tolerance && distance >= radius - tolerance;
      }
    }

    const points = pathData.match(/[ML]\s*([0-9.]+),([0-9.]+)/g);
    if (!points) return false;

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
    
    // Şekil çizme araçları - sürükleme ile çizim
    if (['triangle', 'circle', 'rectangle', 'square'].includes(currentTool)) {
      if (state === State.BEGAN) {
        // Başlangıç noktası
        setLineStartPoint({ x, y });
        setCurrentPath('');
        setIsDrawing(true);
        onShapeStart?.(x, y);
      } else if (state === State.ACTIVE && lineStartPoint) {
        // Sürükleme sırasında - geçici şekil göster
        const shapePath = generateShapePath(lineStartPoint.x, lineStartPoint.y, x, y, currentTool);
        setCurrentPath(shapePath);
      } else if (state === State.END && lineStartPoint) {
        // Sürükleme bitti - şekli kaydet
        const shapePath = generateShapePath(lineStartPoint.x, lineStartPoint.y, x, y, currentTool);
        if (shapePath) {
          const newPathObj = {
            id: `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
            path: shapePath,
            color: currentColor,
            thickness: currentThickness
          };
          const newPaths = [...paths, newPathObj];
          onPathsChange?.(newPaths);
        }
        setLineStartPoint(null);
        setCurrentPath('');
        setIsDrawing(false);
        onShapeEnd?.(x, y);
      }
      return;
    }

    if (currentTool === 'line') {
      // Çizgi modu - sürükleme ile çizim
      if (state === State.BEGAN) {
        // Başlangıç noktası
        setLineStartPoint({ x, y });
        setCurrentPath('');
        setIsDrawing(true);
      } else if (state === State.ACTIVE && lineStartPoint) {
        // Sürükleme sırasında - geçici çizgi göster
        setCurrentPath(`M${lineStartPoint.x},${lineStartPoint.y} L${x},${y}`);
      } else if (state === State.END && lineStartPoint) {
        // Sürükleme bitti - çizgiyi kaydet
        const linePath = `M${lineStartPoint.x},${lineStartPoint.y} L${x},${y}`;
        const newPathObj = {
          id: `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
          path: linePath,
          color: currentColor,
          thickness: currentThickness
        };
        const newPaths = [...paths, newPathObj];
        onPathsChange?.(newPaths);
        setLineStartPoint(null);
        setCurrentPath('');
        setIsDrawing(false);
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
            id: `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
            path: currentPath,
            color: currentColor,
            thickness: currentThickness,
            tool: currentTool // Tool bilgisini de ekle
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
            opacity={pathObj.tool === 'highlighter' ? 0.4 : 1}
          />
        ))}
          
          {/* Render current drawing path */}
          {currentPath && currentTool !== 'line' && (
            <Path
              d={currentPath}
              stroke={currentTool === 'eraser' ? '#ffffff' : currentColor}
              strokeWidth={currentTool === 'eraser' ? 20 : currentThickness}
              fill="none"
              strokeLinecap="round"
              strokeLinejoin="round"
              opacity={currentTool === 'highlighter' ? 0.4 : 1}
            />
          )}
          
          {/* Render line preview */}
          {currentPath && currentTool === 'line' && isDrawing && (
            <Path
              d={currentPath}
              stroke={currentColor}
              strokeWidth={currentThickness}
              fill="none"
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeDasharray="5,5"
              opacity={0.7}
            />
          )}
          
          {/* Render shape preview */}
          {currentPath && ['triangle', 'circle', 'rectangle', 'square'].includes(currentTool) && isDrawing && (
            <Path
              d={currentPath}
              stroke={currentColor}
              strokeWidth={currentThickness}
              fill="none"
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeDasharray={currentTool === 'highlighter' ? undefined : "5,5"}
              opacity={currentTool === 'highlighter' ? 0.3 : 0.7}
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
