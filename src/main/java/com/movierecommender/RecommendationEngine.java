package com.movierecommender;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * ÖNERİ MOTORU (RECOMMENDATION ENGINE)
 * ------------------------------------
 * İşbirlikçi filtreleme (collaborative filtering) algoritmasının kalbi.
 *
 * GENEL FİKİR:
 *   "Sana benzer zevkleri olan insanlar, senin görmediğin filmleri seviyordur."
 *   Yani hedef kullanıcının puanlarına en yakın puan veren X kişiyi bulup,
 *   her birinin en sevdiği K filmi alıp birleştiriyoruz.
 *
 * AKIŞ:
 *   1) Kosinüs benzerliğiyle hedef ile 600 kullanıcının her birini karşılaştır.
 *   2) Tüm sonuçları MaxHeap'e at (en yüksek skor kökte birikir).
 *   3) Heap'ten X kez extractMax yap → en benzer X kullanıcı, sırasıyla.
 *   4) Her biri için, hedefin daha izlemediği en yüksek K filmi al.
 *   5) Sonuçları birleştir, tekrarları çıkar, başlıkları döndür.
 *
 * NEDEN SPARSE (SEYREK) HESAPLAMA?
 *   Vektörler 9018 elemanlı ama çoğu 0. Tüm 9018'i gezsek 600 * 9018 ≈ 5.4M
 *   işlem yapardık. Bunun yerine sadece puanlı (HashMap'te kayıtlı) öğeleri
 *   gezerek O(|A| + |B|) sürede tek karşılaştırma yapıyoruz.
 */
public class RecommendationEngine {

    // Sistemdeki 600 kullanıcının puan haritası (seyrek)
    private final HashMap<Integer, HashMap<Integer, Integer>> mainData;

    // movieId -> film başlığı sözlüğü (sonuçları yazarken kullanılacak)
    private final HashMap<Integer, String> movieTitles;

    public RecommendationEngine(HashMap<Integer, HashMap<Integer, Integer>> mainData,
                                HashMap<Integer, String> movieTitles) {
        this.mainData = mainData;
        this.movieTitles = movieTitles;
    }

    /**
     * KOSİNÜS BENZERLİĞİ
     * ------------------
     * Formül: similarity(A, B) = (A · B) / (||A|| * ||B||)
     *   A · B    : iç çarpım (her i için A[i] * B[i] toplamı)
     *   ||A||    : A vektörünün uzunluğu = sqrt(sum(A[i]^2))
     *
     * Sonuç [0, 1] aralığında: 1 = mükemmel benzer, 0 = ortak puanlı film yok.
     *
     * Performans için küçük HashMap'i geziyoruz (daha az anahtar = daha az lookup),
     * her anahtarı büyük HashMap'te O(1) arayıp iç çarpıma katıyoruz. Magnitudes'lar
     * her bir tarafın TÜM puanları üzerinden hesaplanır (tek geziş yeterli).
     *
     * Köşe durum: taraflardan birinin hiç puanı yoksa benzerlik 0 dönülür.
     * Long aritmetik kullanıyoruz çünkü puanlar küçük (≤5) ama sayıları çoksa
     * int taşması yaşanabilir.
     */
    public static double cosineSimilarity(HashMap<Integer, Integer> a,
                                          HashMap<Integer, Integer> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        // Daha küçük olanı gezmek toplam iterasyon sayısını azaltır.
        HashMap<Integer, Integer> small = a.size() <= b.size() ? a : b;
        HashMap<Integer, Integer> large = small == a ? b : a;

        // 1) İç çarpım: ortak movieId'lerin puanlarını çarpıp topla.
        long dot = 0L;
        for (Map.Entry<Integer, Integer> e : small.entrySet()) {
            Integer other = large.get(e.getKey());
            if (other != null) {
                dot += (long) e.getValue() * other;
            }
        }
        if (dot == 0L) return 0.0; // Hiç ortak puanlı film yoksa erken çık.

        // 2) Magnitudes: her vektörün uzunluğunun karelerinin toplamı.
        long sumA = 0L;
        for (int v : a.values()) sumA += (long) v * v;
        long sumB = 0L;
        for (int v : b.values()) sumB += (long) v * v;

        // 3) Bölme. Pay 0 değilse payda da 0 olmaz ama yine de güvenli kontrol.
        double denom = Math.sqrt((double) sumA) * Math.sqrt((double) sumB);
        return denom == 0.0 ? 0.0 : dot / denom;
    }

    /**
     * X * K FİLM ÖNERİSİ ÜRETİR.
     *
     * @param targetVector  hedef kullanıcının puan haritası (movieId -> rating)
     * @param X             kaç benzer kullanıcı alalım (heap'ten extractMax sayısı)
     * @param K             her benzer kullanıcıdan kaç film alalım
     * @return              en fazla X*K başlık (tekrarlar elendiğinden daha az olabilir)
     */
    public List<String> getRecommendations(HashMap<Integer, Integer> targetVector,
                                           int X, int K) {

        // ============================================================
        // ADIM 1 & 2: Tüm kullanıcılarla benzerlik hesapla ve heap'e at
        // ============================================================
        MaxHeap heap = new MaxHeap(mainData.size());
        for (Map.Entry<Integer, HashMap<Integer, Integer>> e : mainData.entrySet()) {
            double sim = cosineSimilarity(targetVector, e.getValue());
            heap.insert(new UserSimilarity(e.getKey(), sim));
        }

        // ============================================================
        // ADIM 3-5: En benzer X kullanıcıyı çek, her birinden top-K film al
        // ============================================================
        int topUsers = Math.min(X, heap.size());

        // LinkedHashSet, eklenme sırasını koruyup tekrarları otomatik eler.
        // Şartname: "Combine all results → X*K recommendations. Remove duplicates
        // if any (keep first occurrence)." — tam olarak bu davranış.
        LinkedHashSet<String> results = new LinkedHashSet<>();

        for (int i = 0; i < topUsers; i++) {
            // En benzer kullanıcıyı al (heap'in kökü).
            UserSimilarity top = heap.extractMax();
            HashMap<Integer, Integer> theirRatings = mainData.get(top.getUserId());
            if (theirRatings == null || theirRatings.isEmpty()) continue;

            // Bu kullanıcının: > 0 puanlı VE hedefin izlemediği filmleri topla.
            List<Map.Entry<Integer, Integer>> candidates = new ArrayList<>();
            for (Map.Entry<Integer, Integer> r : theirRatings.entrySet()) {
                if (r.getValue() <= 0) continue;
                // Hedef zaten izlemişse atla (yine önerme!)
                if (targetVector.containsKey(r.getKey())) continue;
                candidates.add(r);
            }
            // Puanı yüksekten düşüğe sırala (en sevdikleri başta olsun).
            candidates.sort(Comparator.comparingInt((Map.Entry<Integer, Integer> m) -> m.getValue())
                    .reversed());

            // İlk K tanesini sonuçlara ekle (başlık olarak).
            int taken = 0;
            for (Map.Entry<Integer, Integer> c : candidates) {
                if (taken >= K) break;
                String title = movieTitles.get(c.getKey());
                if (title == null) continue; // movies.csv'de başlığı olmayan ID'leri atla
                results.add(title);          // LinkedHashSet kendi dedup'ını halleder
                taken++;
            }
        }
        return new ArrayList<>(results);
    }
}
