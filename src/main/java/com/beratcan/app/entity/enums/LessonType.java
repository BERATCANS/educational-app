package com.beratcan.app.entity.enums;

public enum LessonType {
    // ✅ Temel Dersler (Mevcut)
    MATHEMATICS, // Matematik
    PHYSICS, // Fizik
    CHEMISTRY, // Kimya
    BIOLOGY, // Biyoloji
    SCIENCE, // Fen Bilimleri (genel - ilkokul/ortaokul için)

    // ✅ Sosyal Bilimler (Mevcut)
    HISTORY, // Tarih
    GEOGRAPHY, // Coğrafya

    // ✅ Yeni Eklenenler
    PHILOSOPHY, // Felsefe
    RELIGION, // Din Kültürü ve Ahlak Bilgisi
    LITERATURE, // Türkçe / Edebiyat
    CIVICS, // Güncel Bilgiler ve Vatandaşlık
    TURKISH_HISTORY, // İnkılap Tarihi ve Atatürkçülük
    FOREIGN_LANGUAGE, // Yabancı Dil (İngilizce, Almanca, vs.)

    // ✅ Genel Kategoriler (Mevcut)
    LANGUAGE, // Dil (genel - istersen FOREIGN_LANGUAGE ile birleştirilebilir)
    ART, // Görsel Sanatlar / Resim
    MUSIC, // Müzik

    // ✅ Fallback
    OTHER; // Diğer
}
