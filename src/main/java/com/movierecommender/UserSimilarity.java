package com.movierecommender;

/**
 * MaxHeap'in tutacağı tek bir öğe: (kullanıcı kimliği, benzerlik skoru) ikilisi.
 *
 * Comparable arayüzünü implement ediyoruz çünkü heap, hangi öğenin daha "büyük"
 * olduğunu compareTo ile anlayacak. Burada "büyük" = "benzerlik skoru daha yüksek"
 * demek. Yani heap'in kökünde her zaman hedefe en çok benzeyen kullanıcı bulunur.
 */
public class UserSimilarity implements Comparable<UserSimilarity> {

    // Kullanıcı ID'si (main_data.csv'deki user_id sütunu)
    private final int userId;

    // 0.0 ile 1.0 arası kosinüs benzerlik skoru
    private final double similarityScore;

    // Constructor: ID ve skoru kilitliyoruz (immutable nesne)
    public UserSimilarity(int userId, double similarityScore) {
        this.userId = userId;
        this.similarityScore = similarityScore;
    }

    public int getUserId() {
        return userId;
    }

    public double getSimilarityScore() {
        return similarityScore;
    }

    /**
     * İki UserSimilarity'yi sadece skorlarına göre karşılaştırır.
     * compareTo > 0  => bu nesne daha büyük (daha benzer)  => heap'te yukarı çıkar.
     *
     * Double.compare kullanıyoruz çünkü NaN ve -0.0 gibi köşe durumlarını
     * doğru şekilde ele alır (basit a - b çıkarması yapsaydık hatalı olabilirdi).
     */
    @Override
    public int compareTo(UserSimilarity other) {
        return Double.compare(this.similarityScore, other.similarityScore);
    }

    @Override
    public String toString() {
        return "User " + userId + " (sim=" + similarityScore + ")";
    }
}
