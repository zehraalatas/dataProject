package com.movierecommender;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * SEKME 2 — KULLANICININ KENDİ PUANLARIYLA ÖNERİ.
 *
 * Bu panel şartnamenin "Get Recommendation according to the movies" arayüzünü uygular.
 * 5 satırlık bir form gösterir: her satırda bir film dropdown'u ve bir puan kutusu.
 *
 * KURALLAR (şartname iv-vi maddeleri):
 *   - Dropdown'lardaki 10 film, movies.csv'den uygulama her açılışında RASTGELE seçilir.
 *   - 5 dropdown'ın tümü aynı 10'lu havuzu paylaşır (aynı oturumda değişmez).
 *   - Kullanıcı 5 FARKLI film seçmek zorunda (tekrarlanan varsa uyarı çıkar).
 *   - Her puan 1-5 arası tamsayı olmalı.
 *
 * AKIŞ: Doğrulanan seçimler bir HashMap<movieId, rating> kullanıcı vektörüne
 * dönüştürülür, sonra RecommendationEngine'e aynı algoritma için verilir.
 *
 * Sekme rengi MOR — Tab 1'deki maviden ayırt edilebilsin.
 */
public class MovieRatingPanel extends JPanel {

    private static final int ROWS = 5;
    private static final int CHOICES_PER_DROPDOWN = 10;

    // Purple/violet palette for this tab
    private static final Color HEADER_FROM = new Color(0x7C3AED);
    private static final Color HEADER_TO   = new Color(0x5B21B6);
    private static final Color ACCENT      = new Color(0x7C3AED);
    private static final Color ACCENT_FG   = Color.WHITE;
    private static final Color CHIP_BG     = new Color(0xEDE9FE);
    private static final Color CHIP_FG     = new Color(0x5B21B6);
    private static final Color HINT        = new Color(0x6B7280);
    private static final Color CARD_BG     = Color.WHITE;
    private static final Color CARD_BORDER = new Color(0xE5E7EB);
    private static final Color PAGE_BG     = new Color(0xF5F3FF);
    private static final Color SUCCESS     = new Color(0x059669);
    private static final Color WARNING     = new Color(0xD97706);

    private final DataLoader data;
    private final RecommendationEngine engine;
    private final List<MovieChoice> sessionChoices;

    @SuppressWarnings("unchecked")
    private final JComboBox<MovieChoice>[] movieCombos = (JComboBox<MovieChoice>[]) new JComboBox[ROWS];
    private final JTextField[] ratingFields = new JTextField[ROWS];
    private final JTextField xField = new JTextField("3", 4);
    private final JTextField kField = new JTextField("5", 4);
    private final JLabel previewChip = new JLabel(" 3 × 5 = 15 recommendations ");
    private final JButton runButton = primaryButton("Get Recommendations");
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.LEFT);
    private final DefaultListModel<String> resultModel = new DefaultListModel<>();

    public MovieRatingPanel(DataLoader data, RecommendationEngine engine) {
        this.data = data;
        this.engine = engine;
        this.sessionChoices = pickRandomMovies(data.getMovieTitles(), CHOICES_PER_DROPDOWN);

        setLayout(new BorderLayout(0, 14));
        setBackground(PAGE_BG);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        GradientHeader header = new GradientHeader(
                "Build Your Own Profile",
                "Pick <b>5 different movies</b> and rate each from <b>1 to 5</b>. The app "
                        + "compares your taste to 600 users, picks the <b>X</b> most similar "
                        + "people, and shows you their top <b>K</b> favorites.",
                HEADER_FROM, HEADER_TO, e -> showHelp());

        add(header, BorderLayout.NORTH);

        JScrollPane outerScroll = new JScrollPane(buildCenter());
        outerScroll.setBorder(BorderFactory.createEmptyBorder());
        outerScroll.getVerticalScrollBar().setUnitIncrement(16);
        outerScroll.getViewport().setBackground(PAGE_BG);
        add(outerScroll, BorderLayout.CENTER);

        runButton.addActionListener(e -> onRun());
        attachLivePreview();
        updatePreview();
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        center.add(buildForm(), BorderLayout.NORTH);
        center.add(buildResultsCard(), BorderLayout.CENTER);
        return center;
    }

    private JPanel buildForm() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER),
                BorderFactory.createEmptyBorder(18, 20, 18, 20)));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;

        MovieChoice[] choices = sessionChoices.toArray(new MovieChoice[0]);

        // Section: rate 5 movies
        g.gridy = 0; g.gridx = 0; g.gridwidth = 4;
        JLabel section = new JLabel("Step 1 — Rate 5 movies (1 = bad, 5 = great)");
        section.setForeground(CHIP_FG);
        section.setFont(section.getFont().deriveFont(Font.BOLD, 13f));
        card.add(section, g);
        g.gridwidth = 1;

        for (int i = 0; i < ROWS; i++) {
            JComboBox<MovieChoice> combo = new JComboBox<>(choices);
            if (i < choices.length) combo.setSelectedIndex(i);
            combo.setPreferredSize(new Dimension(340, 30));
            combo.setMinimumSize(new Dimension(340, 30));
            combo.setMaximumSize(new Dimension(340, 30));
            combo.setToolTipText("Pick a movie (all 5 rows must be different titles)");
            movieCombos[i] = combo;

            JTextField rating = new JTextField(3);
            rating.setPreferredSize(new Dimension(55, 30));
            rating.setMinimumSize(new Dimension(55, 30));
            rating.setMaximumSize(new Dimension(55, 30));
            rating.setToolTipText("Your rating: 1 (bad) – 5 (excellent)");
            ratingFields[i] = rating;

            g.gridy = i + 1; g.weightx = 0;
            g.gridx = 0; card.add(numberBadge(i + 1), g);
            g.gridx = 1; card.add(combo, g);
            g.gridx = 2; card.add(boldLabel("Rating:"), g);
            g.gridx = 3; card.add(rating, g);
        }

        // Section: X & K
        g.gridy = ROWS + 1; g.gridx = 0; g.gridwidth = 4;
        g.insets = new Insets(16, 6, 4, 6);
        JLabel sec2 = new JLabel("Step 2 — How many recommendations?");
        sec2.setForeground(CHIP_FG);
        sec2.setFont(sec2.getFont().deriveFont(Font.BOLD, 13f));
        card.add(sec2, g);
        g.gridwidth = 1;
        g.insets = new Insets(6, 6, 6, 6);

        // X / K row — fixed-size fields so typing more digits doesn't reflow.
        g.gridy = ROWS + 2;
        g.gridx = 0; card.add(boldLabel("X — similar users:"), g);
        g.gridx = 1; xField.setPreferredSize(new Dimension(70, 30));
        xField.setMinimumSize(new Dimension(70, 30));
        xField.setMaximumSize(new Dimension(70, 30));
        xField.setToolTipText("How many of the most similar users to look at. Recommended: 3.");
        card.add(xField, g);
        g.gridx = 2; card.add(boldLabel("K — movies each:"), g);
        g.gridx = 3; kField.setPreferredSize(new Dimension(70, 30));
        kField.setMinimumSize(new Dimension(70, 30));
        kField.setMaximumSize(new Dimension(70, 30));
        kField.setToolTipText("How many of their favorites to take. Recommended: 5.");
        card.add(kField, g);

        // Live preview
        g.gridy = ROWS + 3; g.gridx = 0; g.gridwidth = 4;
        g.insets = new Insets(8, 6, 6, 6);
        card.add(buildPreviewRow(), g);
        g.gridwidth = 1;
        g.insets = new Insets(6, 6, 6, 6);

        // Run button
        g.gridy = ROWS + 4; g.gridx = 0; g.gridwidth = 4;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(10, 6, 4, 6);
        card.add(runButton, g);
        g.gridwidth = 1;
        g.fill = GridBagConstraints.NONE;

        // Status
        g.gridy = ROWS + 5; g.gridx = 0; g.gridwidth = 4;
        g.insets = new Insets(0, 6, 0, 6);
        statusLabel.setForeground(HINT);
        card.add(statusLabel, g);
        g.gridwidth = 1;

        // Right-side spacer column absorbs slack so content columns stay fixed.
        GridBagConstraints spacer = new GridBagConstraints();
        spacer.gridx = 4; spacer.gridy = 0;
        spacer.weightx = 1.0;
        spacer.fill = GridBagConstraints.HORIZONTAL;
        card.add(Box.createHorizontalGlue(), spacer);

        return card;
    }

    private JPanel buildPreviewRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        previewChip.setOpaque(true);
        previewChip.setBackground(CHIP_BG);
        previewChip.setForeground(CHIP_FG);
        previewChip.setFont(previewChip.getFont().deriveFont(Font.BOLD, 13f));
        previewChip.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        // Lock the chip to a fixed width so very large numbers don't reflow the form.
        Dimension chipSize = new Dimension(320, 32);
        previewChip.setPreferredSize(chipSize);
        previewChip.setMinimumSize(chipSize);
        previewChip.setMaximumSize(chipSize);

        JLabel example = new JLabel("  Pick 3 people similar to you → grab each one's "
                + "5 favorite movies → you get 15 recommendations.");
        example.setForeground(HINT);
        example.setFont(example.getFont().deriveFont(Font.ITALIC, 12f));

        row.add(previewChip, BorderLayout.WEST);
        row.add(example, BorderLayout.CENTER);
        return row;
    }

    private JScrollPane buildResultsCard() {
        JList<String> list = new JList<>(resultModel);
        list.setFont(list.getFont().deriveFont(14f));
        list.setFixedCellHeight(26);
        list.setVisibleRowCount(18);
        list.setBackground(CARD_BG);

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(CARD_BORDER), "  Recommended Movies  "));
        scroll.getViewport().setBackground(CARD_BG);
        return scroll;
    }

    private void attachLivePreview() {
        DocumentListener dl = new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { updatePreview(); }
            public void removeUpdate(DocumentEvent e)  { updatePreview(); }
            public void changedUpdate(DocumentEvent e) { updatePreview(); }
        };
        xField.getDocument().addDocumentListener(dl);
        kField.getDocument().addDocumentListener(dl);
    }

    private void updatePreview() {
        try {
            int x = Integer.parseInt(xField.getText().trim());
            int k = Integer.parseInt(kField.getText().trim());
            if (x > 0 && k > 0) {
                previewChip.setText(" " + x + " × " + k + " = " + (x * k) + " recommendations ");
                previewChip.setBackground(CHIP_BG);
                previewChip.setForeground(CHIP_FG);
                return;
            }
        } catch (NumberFormatException ignored) { /* fall through */ }
        previewChip.setText("  Enter positive integers  ");
        previewChip.setBackground(new Color(0xFEE2E2));
        previewChip.setForeground(new Color(0x991B1B));
    }

    /**
     * "Get Recommendations" butonuna basılınca:
     *  1) X, K text-field'larını doğrula.
     *  2) 5 satırın hepsinin film ve geçerli puan içerdiğini doğrula.
     *  3) Aynı filmin iki kez seçilmediğinden emin ol (şartname kuralı).
     *  4) Seçimleri HashMap<movieId, rating> kullanıcı vektörüne dönüştür.
     *  5) SwingWorker ile arka planda hesapla; sonuçları JList'e yaz.
     */
    private void onRun() {
        // -- (1) X ve K doğrulama --
        final int X, K;
        try {
            X = Integer.parseInt(xField.getText().trim());
            K = Integer.parseInt(kField.getText().trim());
        } catch (NumberFormatException ex) {
            showError("X and K must be positive integers.");
            return;
        }
        if (X <= 0 || K <= 0) {
            showError("X and K must be positive integers.");
            return;
        }
        if (X > data.getMainData().size()) {
            showError("X cannot exceed " + data.getMainData().size()
                    + " (total users in main_data).");
            return;
        }

        // -- (2-4) 5 satırı oku, doğrula, hedef vektörü kur --
        HashMap<Integer, Integer> targetVector = new HashMap<>();
        Set<Integer> chosenIds = new HashSet<>(); // Aynı filmi 2 kez tespit için
        for (int i = 0; i < ROWS; i++) {
            MovieChoice mc = (MovieChoice) movieCombos[i].getSelectedItem();
            if (mc == null) {
                showError("Row " + (i + 1) + ": pick a movie.");
                return;
            }
            if (!chosenIds.add(mc.movieId)) {
                showError("All 5 movies must be different (row " + (i + 1)
                        + " repeats an earlier choice).");
                return;
            }
            String raw = ratingFields[i].getText().trim();
            int rating;
            try {
                rating = Integer.parseInt(raw);
            } catch (NumberFormatException ex) {
                showError("Row " + (i + 1) + ": rating must be an integer between 1 and 5.");
                return;
            }
            if (rating < 1 || rating > 5) {
                showError("Row " + (i + 1) + ": rating must be between 1 and 5.");
                return;
            }
            targetVector.put(mc.movieId, rating);
        }

        runButton.setEnabled(false);
        statusLabel.setForeground(HINT);
        statusLabel.setText("Computing similarities...");
        resultModel.clear();

        final int requested = X * K;
        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() {
                return engine.getRecommendations(targetVector, X, K);
            }

            @Override
            protected void done() {
                try {
                    List<String> recs = get();
                    if (recs == null) recs = Collections.emptyList();
                    int n = 1;
                    for (String title : recs) {
                        resultModel.addElement("  " + (n++) + ".   " + title);
                    }
                    if (recs.size() < requested) {
                        int dupes = requested - recs.size();
                        statusLabel.setForeground(WARNING);
                        statusLabel.setText(requested + " requested, " + recs.size()
                                + " unique after removing " + dupes
                                + " duplicate(s). Similar users often share favorites — "
                                + "try a higher K or X for more variety.");
                    } else {
                        statusLabel.setForeground(SUCCESS);
                        statusLabel.setText("Done — " + recs.size() + " movies recommended.");
                    }
                } catch (Exception ex) {
                    statusLabel.setText(" ");
                    showError("Recommendation failed: " + ex.getMessage());
                } finally {
                    runButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private void showHelp() {
        String html = "<html><div style='width:480px; font-family:Segoe UI; font-size:12px;'>"
                + "<h2 style='margin:0 0 8px 0; color:#5B21B6;'>How it works</h2>"
                + "<p>Instead of picking an existing user, you build your own taste profile "
                + "by rating 5 movies. Then:</p>"
                + "<p style='background:#EDE9FE; padding:8px; border-radius:6px;'>"
                + "<i>\"Find the 600-database users who rated those same 5 movies most like you, "
                + "and copy what they love.\"</i></p>"
                + "<h3 style='margin:10px 0 4px 0; color:#5B21B6;'>Steps</h3>"
                + "<ol>"
                + "<li><b>Cosine similarity</b> compares your 5 ratings to every user.</li>"
                + "<li>A custom <b>MaxHeap</b> picks the top <b>X</b> most similar users.</li>"
                + "<li>For each, the top <b>K</b> movies you haven't rated are pulled.</li>"
                + "<li>X × K candidates are merged and de-duplicated.</li>"
                + "</ol>"
                + "<h3 style='margin:10px 0 4px 0; color:#5B21B6;'>Why do I get fewer than X × K?</h3>"
                + "<p>Users with similar taste tend to love the same movies. After "
                + "the X × K candidates are merged and duplicates removed, the final "
                + "list is shorter. Increase <b>K</b> or <b>X</b> to get more variety.</p>"
                + "<h3 style='margin:10px 0 4px 0; color:#5B21B6;'>Notes</h3>"
                + "<ul>"
                + "<li>The 10 movies in each dropdown are <b>randomized at every launch</b> "
                + "— close and re-open the app to get a different pool.</li>"
                + "<li>All 5 selected movies must be different.</li>"
                + "<li>Ratings must be integers from 1 to 5.</li>"
                + "</ul>"
                + "</div></html>";
        JOptionPane.showMessageDialog(this, html, "Help", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Movies.csv'deki tüm filmlerden rastgele {@code count} tanesini seçer.
     * Şartname dropdown başına 10 rastgele film istiyor; bu metot uygulama
     * her açılışta YENİ bir 10'luk havuz üretir.
     *
     *  1) Tüm filmleri ArrayList'e koy.
     *  2) Collections.shuffle ile karıştır.
     *  3) İlk {@code count} tanesini al, alfabetik sıraya koy (UX için).
     */
    private static List<MovieChoice> pickRandomMovies(HashMap<Integer, String> movies, int count) {
        // 1) Tüm filmleri (id, başlık) çiftleri olarak listeye al
        List<MovieChoice> all = new ArrayList<>(movies.size());
        for (Map.Entry<Integer, String> e : movies.entrySet()) {
            all.add(new MovieChoice(e.getKey(), e.getValue()));
        }
        // 2) Tüm listeyi karıştır (Fisher-Yates shuffle, O(n))
        Collections.shuffle(all, new Random());
        // 3) Sadece ilk N tanesini al, kullanıcıya alfabetik göster
        List<MovieChoice> picked = all.subList(0, Math.min(count, all.size()));
        picked.sort(Comparator.comparing(mc -> mc.title.toLowerCase()));
        return new ArrayList<>(picked);
    }

    private static JLabel boldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }

    private static JLabel numberBadge(int n) {
        JLabel l = new JLabel(String.valueOf(n));
        l.setOpaque(true);
        l.setBackground(ACCENT);
        l.setForeground(Color.WHITE);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 12f));
        l.setPreferredSize(new Dimension(26, 26));
        return l;
    }

    private static JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(ACCENT);
        b.setForeground(ACCENT_FG);
        b.setFocusPainted(false);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 14f));
        b.setBorder(BorderFactory.createEmptyBorder(11, 18, 11, 18));
        b.setOpaque(true);
        return b;
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Input Error", JOptionPane.ERROR_MESSAGE);
    }

    private static final class MovieChoice {
        final int movieId;
        final String title;
        MovieChoice(int movieId, String title) {
            this.movieId = movieId;
            this.title = title;
        }
        @Override public String toString() { return title; }
    }
}
