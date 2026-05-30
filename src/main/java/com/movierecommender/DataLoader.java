package com.movierecommender;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * VERİ YÜKLEYİCİ
 * --------------
 * 3 CSV dosyasını okuyup hafızadaki HashMap'lere yerleştirir:
 *
 *   - main_data.csv    : 600 kullanıcı × 9018 film puanları (1-5, 0 = puansız)
 *   - target_user.csv  : 10 hedef kullanıcı (601-610), aynı sütun düzeni
 *   - movies.csv       : movieId, title, genres (filmlerin isim sözlüğü)
 *
 * NEDEN HashMap?
 *   Matris çok seyrek (sparse). 600 × 9018 ≈ 5.4 milyon hücre olsa da gerçekte
 *   sadece küçük bir kısmı puanlı. Dense 2D dizi kullansak bellek ve döngü
 *   israfı olurdu. Bunun yerine iç içe HashMap kullanıyoruz; sadece > 0 puanları
 *   saklıyoruz. Şartname "HashTable" seçeneğini açıkça izin veriyor.
 *
 *   Yapı: HashMap<userId, HashMap<movieId, rating>>
 *   "Kullanıcı U, M filmini kaç puan vermiş?" sorusu: O(1) lookup.
 */
public class DataLoader {

    // data/ klasörü, JAR'ın bulunduğu çalışma dizininin yanında olmalı.
    public static final String DATA_DIR =
            System.getProperty("user.dir") + File.separator + "data" + File.separator;

    // 600 kullanıcının seyrek puan matrisi
    private HashMap<Integer, HashMap<Integer, Integer>> mainData;

    // 10 hedef kullanıcı (601-610), Tab 1'in dropdown'ında listelenir
    private HashMap<Integer, HashMap<Integer, Integer>> targetUsers;

    // movieId -> film başlığı (ör. 1 -> "Toy Story (1995)")
    private HashMap<Integer, String> movieTitles;

    // main_data.csv başlık satırındaki movieId sütun sırası (uzunluk 9018)
    private int[] movieIdOrder;

    /** Tüm CSV'leri sırayla yükler. */
    public void loadAll() throws IOException {
        this.mainData    = loadRatings(DATA_DIR + "main_data.csv", true);
        this.targetUsers = loadRatings(DATA_DIR + "target_user.csv", false);
        this.movieTitles = loadMovies(DATA_DIR + "movies.csv");
    }

    /**
     * Geniş formatlı puan CSV'sini okur.
     *
     * Format: ilk satır "user_id,1,2,3,...,9018" (movieId sütunları).
     *         sonraki her satır: kullanıcı_id ardından 9018 puan değeri (0-5).
     *
     * Hafıza tasarrufu için sadece 0'dan büyük puanları HashMap'e koyuyoruz.
     *
     * @param captureHeader  true ise başlıktaki movieId sırası movieIdOrder'a kaydedilir
     */
    private HashMap<Integer, HashMap<Integer, Integer>> loadRatings(String path,
                                                                    boolean captureHeader)
            throws IOException {
        HashMap<Integer, HashMap<Integer, Integer>> ratings = new HashMap<>();
        try (BufferedReader br = Files.newBufferedReader(new File(path).toPath(),
                StandardCharsets.UTF_8)) {

            // İlk satır: başlıklar. "user_id, 1, 2, 3, ..., 9018"
            String headerLine = br.readLine();
            if (headerLine == null) {
                throw new IOException("Boş CSV: " + path);
            }
            String[] headers = headerLine.split(",");
            int[] movieIds = new int[headers.length - 1];
            for (int i = 1; i < headers.length; i++) {
                movieIds[i - 1] = Integer.parseInt(headers[i].trim());
            }
            if (captureHeader) {
                this.movieIdOrder = movieIds;
            }

            // Sonraki satırlar: kullanıcı puanları
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) continue;
                String[] tokens = line.split(",");
                int userId = Integer.parseInt(tokens[0].trim());

                // Bu kullanıcının seyrek puan haritası
                HashMap<Integer, Integer> userRatings = new HashMap<>();
                int limit = Math.min(tokens.length - 1, movieIds.length);
                for (int i = 0; i < limit; i++) {
                    String raw = tokens[i + 1].trim();
                    if (raw.isEmpty()) continue;
                    int rating = Integer.parseInt(raw);
                    // Sadece > 0 puanları sakla (0 = puansız demek, bellek israfı)
                    if (rating > 0) {
                        userRatings.put(movieIds[i], rating);
                    }
                }
                ratings.put(userId, userRatings);
            }
        }
        return ratings;
    }

    /**
     * movies.csv dosyasını okuyup (movieId -> title) sözlüğü kurar.
     *
     * DİKKAT: Film başlıkları virgül içerebilir ("Movie, The (1995)" gibi) ve
     * o zaman çift tırnak içine alınır. Bu yüzden basit split(",") yetmez,
     * RFC-4180 tarzı küçük bir parser kullanıyoruz.
     */
    private HashMap<Integer, String> loadMovies(String path) throws IOException {
        HashMap<Integer, String> titles = new HashMap<>();
        try (BufferedReader br = Files.newBufferedReader(new File(path).toPath(),
                StandardCharsets.UTF_8)) {
            br.readLine(); // başlık satırını atla: movieId,title,genres
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) continue;
                List<String> fields = parseCsvLine(line);
                if (fields.size() < 2) continue;
                try {
                    int movieId = Integer.parseInt(fields.get(0).trim());
                    titles.put(movieId, fields.get(1));
                } catch (NumberFormatException ignore) {
                    // Bozuk satır, atla
                }
            }
        }
        return titles;
    }

    /**
     * Çift tırnak destekli minimal CSV satır parser'ı.
     * Örnek: 'Movie, The (1995)' başlığı "Movie, The (1995)" şeklinde gelir;
     *  bizim parser tırnak içindeki virgülü ayraç saymaz.
     */
    private static List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                // Çift tırnak içinde olduğumuzda "" iki çift tırnak, escape edilmiş bir " demek
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes; // tırnak modunu aç/kapat
                }
            } else if (c == ',' && !inQuotes) {
                // Tırnak dışında virgül -> alan ayracı
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }

    // ---- Erişimciler (getter'lar) -----------------------------------------
    public HashMap<Integer, HashMap<Integer, Integer>> getMainData()     { return mainData; }
    public HashMap<Integer, HashMap<Integer, Integer>> getTargetUsers()  { return targetUsers; }
    public HashMap<Integer, String>                    getMovieTitles()  { return movieTitles; }
    public int[]                                       getMovieIdOrder() { return movieIdOrder; }
}
