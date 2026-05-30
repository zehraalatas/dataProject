package com.movierecommender;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * ANA PENCERE.
 *
 * Şartname iki ayrı arayüz istiyor:
 *   - Tab 1: Mevcut hedef kullanıcıya göre öneri (target_user.csv'den seçim)
 *   - Tab 2: Kullanıcının kendi 5 puanını girerek öneri
 *
 * Bunları tek bir pencerede JTabbedPane ile gösteriyoruz. Pencere açılışta
 * tam ekrana büyüyor; minimum boyut 900×650 (daraltılırsa hâlâ kullanılır).
 */
public class MainFrame extends JFrame {

    public MainFrame(DataLoader data, RecommendationEngine engine) {
        // Pencere başlığı
        super("Movie Recommendation System");

        // X'e basınca uygulamadan tamamen çık (pencereyi gizleme)
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Minimum boyut: daha küçük olamaz. Başlangıç boyutu: 1100×800.
        setMinimumSize(new Dimension(900, 650));
        setSize(1100, 800);
        setLocationRelativeTo(null); // ekranın ortasında aç

        // Tam ekran aç ki form + sonuçlar bir bakışta görünsün
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // İki sekmeli panel — şartnamedeki "two different user interfaces"
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(tabs.getFont().deriveFont(java.awt.Font.BOLD, 13f));
        tabs.addTab("  Recommend by User  ",    new UserRecommendPanel(data, engine));
        tabs.addTab("  Recommend by Ratings  ", new MovieRatingPanel(data, engine));

        // Sekmeler pencerenin tüm iç alanını kaplasın
        setLayout(new BorderLayout());
        add(tabs, BorderLayout.CENTER);
    }
}
