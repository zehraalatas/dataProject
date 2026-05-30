package com.movierecommender;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.Font;

/**
 * UYGULAMANIN GİRİŞ NOKTASI (main metodu burada).
 *
 * Yaptıkları:
 *  1) Nimbus look-and-feel + Segoe UI fontunu uygular (modern görünüm için).
 *  2) DataLoader ile 3 CSV dosyasını okur.
 *  3) RecommendationEngine'i kurar.
 *  4) MainFrame penceresini EDT (Event Dispatch Thread) üzerinde gösterir.
 *
 * CSV dosyaları okunamazsa hata diyaloğu gösterip programdan çıkar.
 */
public final class Main {

    // Yardımcı sınıf olduğu için instance üretilemez.
    private Main() {}

    public static void main(String[] args) {
        // Önce GUI temasını ayarla (Nimbus). Pencere oluşturulmadan ÖNCE
        // çağırmak şart, yoksa eski Metal teması kullanılırdı.
        applyModernLookAndFeel();

        // Swing kuralı: bütün UI işleri Event Dispatch Thread (EDT) üzerinde olmalı.
        SwingUtilities.invokeLater(() -> {
            try {
                // 1) Veriyi yükle (yaklaşık 600 ms — CSV parse + HashMap kurma)
                DataLoader loader = new DataLoader();
                loader.loadAll();

                // 2) Algoritma motorunu oluştur (ana veri + film başlıkları)
                RecommendationEngine engine = new RecommendationEngine(
                        loader.getMainData(), loader.getMovieTitles());

                // 3) Ana pencereyi göster
                new MainFrame(loader, engine).setVisible(true);
            } catch (Exception ex) {
                // Beklenmedik bir hata olursa kullanıcıya net mesaj göster.
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Failed to start: " + ex.getMessage()
                                + "\n\nMake sure the 'data' folder with main_data.csv, "
                                + "target_user.csv and movies.csv is next to the app.",
                        "Startup Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }

    /**
     * Nimbus L&F + Segoe UI fontunu tüm Swing bileşenlerine uygular.
     * Nimbus, Java'nın varsayılan modern görünümüdür; Metal'den çok daha temiz durur.
     */
    private static void applyModernLookAndFeel() {
        try {
            // Nimbus'u kurulu look-and-feel listesinden bul ve seç.
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // Hata olursa sessizce varsayılan L&F ile devam et.
        }

        // Tüm yazıların aynı (modern) font ile gelmesi için UIManager'a yaz.
        FontUIResource base = new FontUIResource(new Font("Segoe UI", Font.PLAIN, 14));
        String[] keys = {
                "Label.font", "Button.font", "TextField.font", "ComboBox.font",
                "List.font", "TitledBorder.font", "TabbedPane.font",
                "OptionPane.messageFont", "OptionPane.buttonFont", "TextArea.font"
        };
        for (String k : keys) UIManager.put(k, base);
    }
}
