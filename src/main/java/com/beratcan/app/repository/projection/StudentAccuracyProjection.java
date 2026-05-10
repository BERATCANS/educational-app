package com.beratcan.app.repository.projection;

/**
 * Native query projection: kullanıcının DISTINCT question_id (son cevap kazanır)
 * mantığıyla toplam çözülen ve doğru cevap sayısını tek query'de döner.
 */
public interface StudentAccuracyProjection {
    long getTotal();
    long getCorrect();
}
