import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Modal, Dimensions } from 'react-native';
import { Ionicons } from '@expo/vector-icons';

const { width: screenWidth } = Dimensions.get('window');

interface DrawingToolbarProps {
  showDrawingCanvas: boolean;
  showDrawingToolbar: boolean;
  currentTool: 'pen' | 'eraser' | 'line' | 'triangle' | 'circle' | 'rectangle' | 'square' | 'highlighter';
  currentColor: string;
  currentThickness: number;
  showColorPalette: boolean;
  showThicknessPalette: boolean;
  showShapePalette: boolean;
  onDrawingToggle: () => void;
  onToolbarToggle: () => void;
  onToolSelect: (tool: 'pen' | 'eraser' | 'line' | 'triangle' | 'circle' | 'rectangle' | 'square' | 'highlighter') => void;
  onColorPaletteToggle: () => void;
  onThicknessPaletteToggle: () => void;
  onShapePaletteToggle: () => void;
  onColorSelect: (color: string) => void;
  onThicknessSelect: (thickness: number) => void;
  onShapeSelect: (shape: 'line' | 'triangle' | 'circle' | 'rectangle' | 'square') => void;
  onUndo: () => void;
  onClear: () => void;
}

export const DrawingToolbar: React.FC<DrawingToolbarProps> = ({
  showDrawingCanvas,
  showDrawingToolbar,
  currentTool,
  currentColor,
  currentThickness,
  showColorPalette,
  showThicknessPalette,
  showShapePalette,
  onDrawingToggle,
  onToolbarToggle,
  onToolSelect,
  onColorPaletteToggle,
  onThicknessPaletteToggle,
  onShapePaletteToggle,
  onColorSelect,
  onThicknessSelect,
  onShapeSelect,
  onUndo,
  onClear,
}) => {
  return (
    <>
      {/* Drawing Tools */}
      {showDrawingCanvas && (
        <View style={styles.drawingContainer}>
          {/* Compact Drawing Toolbar */}
          <View style={[styles.compactDrawingToolbar, !showDrawingToolbar && styles.hiddenToolbar]}>
            {/* Sol Grup - Araçlar */}
            <View style={styles.toolbarGroup}>
              <TouchableOpacity 
                style={[styles.compactTool, currentTool === 'pen' && styles.activeCompactTool]}
                onPress={() => onToolSelect('pen')}
              >
                <Ionicons name="pencil" size={18} color={currentTool === 'pen' ? "#3b82f6" : "#64748b"} />
              </TouchableOpacity>
              
              <TouchableOpacity 
                style={[styles.compactTool, currentTool === 'highlighter' && styles.activeCompactTool]}
                onPress={() => onToolSelect('highlighter')}
              >
                <Ionicons name="brush" size={18} color={currentTool === 'highlighter' ? "#f59e0b" : "#64748b"} />
              </TouchableOpacity>
              
              <TouchableOpacity 
                style={[styles.compactTool, currentTool === 'eraser' && styles.activeCompactTool]}
                onPress={() => onToolSelect('eraser')}
              >
                <Ionicons name="remove-circle" size={18} color={currentTool === 'eraser' ? "#ef4444" : "#64748b"} />
              </TouchableOpacity>
              
              <TouchableOpacity 
                style={[styles.compactTool, ['line', 'triangle', 'circle', 'rectangle', 'square'].includes(currentTool) && styles.activeCompactTool]}
                onPress={onShapePaletteToggle}
              >
                <Ionicons name="shapes" size={18} color={['line', 'triangle', 'circle', 'rectangle', 'square'].includes(currentTool) ? "#3b82f6" : "#64748b"} />
              </TouchableOpacity>
            </View>

            {/* Orta Grup - Renk ve Kalınlık */}
            <View style={styles.toolbarGroup}>
              <TouchableOpacity 
                style={[
                  styles.compactColorButton, 
                  { backgroundColor: currentColor },
                  currentTool === 'highlighter' && styles.highlighterButton
                ]}
                onPress={onColorPaletteToggle}
              />

              <TouchableOpacity 
                style={styles.compactThicknessButton}
                onPress={onThicknessPaletteToggle}
              >
                <View style={[styles.compactThicknessDot, { 
                  width: Math.min(currentThickness * 1.5, 12), 
                  height: Math.min(currentThickness * 1.5, 12) 
                }]} />
              </TouchableOpacity>
            </View>

            {/* Sağ Grup - Aksiyonlar */}
            <View style={styles.toolbarGroup}>
              <TouchableOpacity 
                style={styles.compactActionButton}
                onPress={onUndo}
              >
                <Ionicons name="arrow-undo" size={18} color="#64748b" />
              </TouchableOpacity>
              
              <TouchableOpacity 
                style={styles.compactActionButton}
                onPress={onClear}
              >
                <Ionicons name="trash-outline" size={18} color="#ef4444" />
              </TouchableOpacity>
            </View>
          </View>
        </View>
      )}

      {/* Color Palette Modal */}
      <Modal
        visible={showColorPalette}
        transparent={true}
        animationType="fade"
        onRequestClose={() => onColorPaletteToggle()}
      >
        <TouchableOpacity 
          style={styles.modalOverlay}
          activeOpacity={1}
          onPress={() => onColorPaletteToggle()}
        >
          <View style={styles.colorPalette}>
            <Text style={styles.paletteTitle}>Renk Seç</Text>
            <View style={styles.colorGrid}>
              {['#000000', '#FF0000', '#00FF00', '#0000FF', '#FFFF00', '#FF00FF', '#00FFFF', '#FFA500', '#800080', '#FFC0CB', '#A52A2A', '#808080'].map((color) => (
                <TouchableOpacity
                  key={color}
                  style={[styles.colorOption, { backgroundColor: color }]}
                  onPress={() => onColorSelect(color)}
                />
              ))}
            </View>
          </View>
        </TouchableOpacity>
      </Modal>

      {/* Thickness Palette Modal */}
      <Modal
        visible={showThicknessPalette}
        transparent={true}
        animationType="fade"
        onRequestClose={() => onThicknessPaletteToggle()}
      >
        <TouchableOpacity 
          style={styles.modalOverlay}
          activeOpacity={1}
          onPress={() => onThicknessPaletteToggle()}
        >
          <View style={styles.thicknessPalette}>
            <Text style={styles.paletteTitle}>Kalınlık Seç</Text>
            <View style={styles.thicknessGrid}>
              {[1, 2, 4, 6, 8, 10, 12, 16].map((thickness) => (
                <TouchableOpacity
                  key={thickness}
                  style={styles.thicknessOption}
                  onPress={() => onThicknessSelect(thickness)}
                >
                  <View style={[styles.thicknessLine, { 
                    height: thickness, 
                    backgroundColor: currentColor 
                  }]} />
                </TouchableOpacity>
              ))}
            </View>
          </View>
        </TouchableOpacity>
      </Modal>

      {/* Shape Palette Modal */}
      <Modal
        visible={showShapePalette}
        transparent={true}
        animationType="fade"
        onRequestClose={() => onShapePaletteToggle()}
      >
        <TouchableOpacity 
          style={styles.modalOverlay}
          activeOpacity={1}
          onPress={() => onShapePaletteToggle()}
        >
          <View style={styles.shapePalette}>
            <Text style={styles.paletteTitle}>Şekil Seç</Text>
            <View style={styles.shapeGrid}>
              {[
                { type: 'line', icon: 'remove' },
                { type: 'triangle', icon: 'triangle' },
                { type: 'circle', icon: 'ellipse' },
                { type: 'rectangle', icon: 'square' },
                { type: 'square', icon: 'stop' }
              ].map((shape) => (
                <TouchableOpacity
                  key={shape.type}
                  style={[styles.shapeOption, currentTool === shape.type && styles.activeShapeOption]}
                  onPress={() => onShapeSelect(shape.type as any)}
                >
                  <Ionicons 
                    name={shape.icon as any} 
                    size={24} 
                    color={currentTool === shape.type ? "#3b82f6" : "#64748b"} 
                  />
                </TouchableOpacity>
              ))}
            </View>
          </View>
        </TouchableOpacity>
      </Modal>
    </>
  );
};

const styles = StyleSheet.create({
  drawingContainer: {
    backgroundColor: '#ffffff',
  },
  compactDrawingToolbar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 8,
    paddingVertical: 6,
    backgroundColor: '#f8fafc',
    borderTopWidth: 1,
    borderTopColor: '#e2e8f0',
    flexWrap: 'nowrap',
  },
  hiddenToolbar: {
    height: 0,
    paddingVertical: 0,
    borderTopWidth: 0,
    opacity: 0,
  },
  toolbarGroup: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  compactTool: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: '#ffffff',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 2,
    borderColor: '#e2e8f0',
    marginHorizontal: 2,
    marginVertical: 2,
  },
  activeCompactTool: {
    borderColor: '#3b82f6',
    backgroundColor: '#e0f2fe',
  },
  compactColorButton: {
    width: 28,
    height: 28,
    borderRadius: 14,
    borderWidth: 2,
    borderColor: '#e2e8f0',
    marginHorizontal: 2,
    marginVertical: 2,
  },
  compactThicknessButton: {
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: '#ffffff',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 2,
    borderColor: '#e2e8f0',
    marginHorizontal: 2,
    marginVertical: 2,
  },
  compactThicknessDot: {
    backgroundColor: '#374151',
    borderRadius: 10,
  },
  compactActionButton: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: '#ffffff',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 2,
    borderColor: '#e2e8f0',
    marginHorizontal: 2,
    marginVertical: 2,
  },
  highlighterButton: {
    borderWidth: 2,
    borderColor: '#f59e0b',
    borderRadius: 4,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  colorPalette: {
    backgroundColor: '#ffffff',
    borderRadius: 12,
    padding: 20,
    margin: 20,
    minWidth: 280,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 8,
  },
  thicknessPalette: {
    backgroundColor: '#ffffff',
    borderRadius: 12,
    padding: 20,
    margin: 20,
    minWidth: 280,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 8,
  },
  shapePalette: {
    backgroundColor: '#ffffff',
    borderRadius: 12,
    padding: 20,
    margin: 20,
    minWidth: 280,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 8,
  },
  paletteTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#1e293b',
    marginBottom: 16,
    textAlign: 'center',
  },
  colorGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'center',
    gap: 12,
  },
  colorOption: {
    width: 40,
    height: 40,
    borderRadius: 20,
    borderWidth: 2,
    borderColor: '#e2e8f0',
    margin: 4,
  },
  thicknessGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'center',
    gap: 12,
  },
  thicknessOption: {
    width: 50,
    height: 50,
    borderRadius: 25,
    backgroundColor: '#f8fafc',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 2,
    borderColor: '#e2e8f0',
    margin: 4,
  },
  thicknessLine: {
    width: 30,
    borderRadius: 2,
  },
  shapeGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'center',
    gap: 12,
  },
  shapeOption: {
    width: 50,
    height: 50,
    borderRadius: 25,
    backgroundColor: '#f8fafc',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 2,
    borderColor: '#e2e8f0',
    margin: 4,
  },
  activeShapeOption: {
    borderColor: '#3b82f6',
    backgroundColor: '#e0f2fe',
  },
});
