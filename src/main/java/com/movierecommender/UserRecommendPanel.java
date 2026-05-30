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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

/**
 * SEKME 1 — MEVCUT HEDEF KULLANICIYA GÖRE ÖNERİ.
 *
 * Bu panel şartnamenin "Get Recommendations according to the target user"
 * arayüzünü uygular. Akış:
 *
 *  1) Açılışta target_user.csv'deki 10 kullanıcı (601-610) JComboBox'a gelir.
 *  2) Kullanıcı bir hedef seçer, X ve K değerlerini girer.
 *  3) "Get Recommendations" butonuna basınca SwingWorker arka planda çalışır
 *     (600 kullanıcıyla benzerlik hesaplamasını arayüzü kilitlemeden yapar).
 *  4) Sonuçlar JList'e numaralı film başlıkları olarak yazılır.
 *
 * X * K canlı önizlemesi: kullanıcı sayıları değiştirdikçe chip otomatik
 * günceller ("3 × 5 = 15 recommendations" gibi).
 */
public class UserRecommendPanel extends JPanel {

    // ============================================================
    // BU SEKMENİN MAVİ RENK PALETİ
    // Tab 1 = mavi tema, Tab 2 = mor tema farklılaştırma için.
    // ============================================================
    private static final Color HEADER_FROM = new Color(0x2563EB);
    private static final Color HEADER_TO   = new Color(0x1E40AF);
    private static final Color ACCENT      = new Color(0x2563EB);
    private static final Color ACCENT_FG   = Color.WHITE;
    private static final Color CHIP_BG     = new Color(0xDBEAFE);
    private static final Color CHIP_FG     = new Color(0x1E3A8A);
    private static final Color HINT        = new Color(0x6B7280);
    private static final Color CARD_BG     = Color.WHITE;
    private static final Color CARD_BORDER = new Color(0xE5E7EB);
    private static final Color PAGE_BG     = new Color(0xF1F5F9);
    private static final Color SUCCESS     = new Color(0x059669);
    private static final Color WARNING     = new Color(0xD97706);

    private final DataLoader data;
    private final RecommendationEngine engine;

    private final JComboBox<Integer> userCombo;
    private final JTextField xField = new JTextField("3", 4);
    private final JTextField kField = new JTextField("5", 4);
    private final JLabel previewChip = new JLabel(" 3 × 5 = 15 recommendations ");
    private final JButton runButton = primaryButton("Get Recommendations");
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.LEFT);
    private final DefaultListModel<String> resultModel = new DefaultListModel<>();

    public UserRecommendPanel(DataLoader data, RecommendationEngine engine) {
        // Veri ve algoritma motorunu dışarıdan alıyoruz (dependency injection).
        this.data = data;
        this.engine = engine;

        // target_user.csv'deki 10 ID'yi TreeSet ile sıraya koyup combo'ya koy.
        // (601, 602, ..., 610 sırayla görünür.)
        TreeSet<Integer> ids = new TreeSet<>(data.getTargetUsers().keySet());
        userCombo = new JComboBox<>(ids.toArray(new Integer[0]));

        setLayout(new BorderLayout(0, 14));
        setBackground(PAGE_BG);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        GradientHeader header = new GradientHeader(
                "Find Movies for an Existing User",
                "Pick one of the 10 sample users (IDs 601–610). The app finds the "
                        + "<b>X</b> users with the most similar taste, then takes each of their "
                        + "top <b>K</b> favorite movies. Total suggestions = <b>X × K</b>.",
                HEADER_FROM, HEADER_TO, e -> showHelp());

        add(header, BorderLayout.NORTH);

        // Outer scroll wraps the form + results so the whole page scrolls when
        // the window is too short to show both blocks at once.
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

        // Columns 0..3 hold the actual content (label, input, hint).
        // Column 4 is a flexible spacer that absorbs all extra horizontal space
        // so the content columns stay anchored to the left and never reflow
        // when the user types into X/K.
        final int SPACER_COL = 4;

        // Row 0 — Target user
        g.gridy = 0;
        g.gridx = 0; g.weightx = 0; card.add(stepLabel("1", "Target user"), g);
        g.gridx = 1; userCombo.setPreferredSize(new Dimension(120, 30));
        userCombo.setToolTipText("One of the 10 sample users (IDs 601–610). "
                + "Recommendations will be computed for this user.");
        card.add(userCombo, g);
        g.gridx = 2; card.add(hintLabel("Which sample user are we recommending for?"), g);

        // Row 1 — X
        g.gridy = 1;
        g.gridx = 0; card.add(stepLabel("2", "X — similar users"), g);
        g.gridx = 1; xField.setPreferredSize(new Dimension(70, 30));
        xField.setMinimumSize(new Dimension(70, 30));
        xField.setMaximumSize(new Dimension(70, 30));
        xField.setToolTipText("How many of the most similar users to look at. Recommended: 3–5.");
        card.add(xField, g);
        g.gridx = 2; card.add(hintLabel("How many similar people should we ask?"), g);

        // Row 2 — K
        g.gridy = 2;
        g.gridx = 0; card.add(stepLabel("3", "K — movies each"), g);
        g.gridx = 1; kField.setPreferredSize(new Dimension(70, 30));
        kField.setMinimumSize(new Dimension(70, 30));
        kField.setMaximumSize(new Dimension(70, 30));
        kField.setToolTipText("How many top-rated movies to pull from each of those people. "
                + "Recommended: 5.");
        card.add(kField, g);
        g.gridx = 2; card.add(hintLabel("How many of their favorites should we grab from each person?"), g);

        // Row 3 — Live preview chip + example
        g.gridy = 3; g.gridx = 0; g.gridwidth = 4;
        g.insets = new Insets(10, 6, 6, 6);
        card.add(buildPreviewRow(), g);
        g.gridwidth = 1;
        g.insets = new Insets(6, 6, 6, 6);

        // Row 4 — Run button (spans content columns, but spacer still absorbs slack)
        g.gridy = 4; g.gridx = 0; g.gridwidth = 4;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(10, 6, 4, 6);
        card.add(runButton, g);
        g.gridwidth = 1;
        g.fill = GridBagConstraints.NONE;

        // Row 5 — Status
        g.gridy = 5; g.gridx = 0; g.gridwidth = 4;
        g.insets = new Insets(0, 6, 0, 6);
        statusLabel.setForeground(HINT);
        card.add(statusLabel, g);
        g.gridwidth = 1;

        // Right-side spacer: absorbs all remaining horizontal space so the
        // content columns above don't recompute when input widths change.
        GridBagConstraints spacer = new GridBagConstraints();
        spacer.gridx = SPACER_COL; spacer.gridy = 0;
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

        JLabel example = new JLabel("  Example: with X=3, K=5 you'd see the 5 favorite "
                + "movies of each of the 3 most similar users.");
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
        list.setVisibleRowCount(18); // makes the JScrollPane request ~470px tall
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

    /**
     * Canlı önizleme chip'ini günceller.
     * X ve K text-field'larında her değişiklikte (DocumentListener) çağrılır.
     * Geçerli sayılarsa: "3 × 5 = 15 recommendations" yazar (mavi tema).
     * Geçersizse: kırmızı uyarı moduna geçer.
     */
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
        } catch (NumberFormatException ignored) { /* sayıya çevrilemezse aşağı düş */ }
        // Geçersiz giriş: kırmızı uyarı
        previewChip.setText("  Enter positive integers  ");
        previewChip.setBackground(new Color(0xFEE2E2));
        previewChip.setForeground(new Color(0x991B1B));
    }

    /**
     * "Get Recommendations" butonuna basılınca çalışır.
     *  1) Girişleri (X, K, hedef kullanıcı) doğrular.
     *  2) Geçerliyse butonu kilitleyip SwingWorker ile arka planda hesaplar.
     *  3) Sonuçları JList'e basar, durum etiketini günceller.
     *
     * SwingWorker neden gerekli? engine.getRecommendations çağrısı 30-100 ms sürer.
     * Bu, EDT (UI thread) üzerinde yapılırsa arayüz bu süre boyunca KİLİTLENİR
     * (donar). SwingWorker hesaplamayı başka thread'e taşır, done() metodu EDT'ye
     * geri döner.
     */
    private void onRun() {
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

        Integer selected = (Integer) userCombo.getSelectedItem();
        if (selected == null) {
            showError("Please pick a target user.");
            return;
        }
        HashMap<Integer, Integer> targetVector = data.getTargetUsers().get(selected);
        if (targetVector == null) {
            showError("Target user " + selected + " not found.");
            return;
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
                + "<h2 style='margin:0 0 8px 0; color:#1E40AF;'>How it works</h2>"
                + "<p>This is <b>user-based collaborative filtering</b>. The idea:</p>"
                + "<p style='background:#DBEAFE; padding:8px; border-radius:6px;'>"
                + "<i>\"People who rate movies the way you do probably love movies you haven't "
                + "seen yet. Let's find those people and copy their favorites.\"</i></p>"
                + "<h3 style='margin:10px 0 4px 0; color:#1E40AF;'>Steps</h3>"
                + "<ol>"
                + "<li><b>Similarity:</b> The target user's ratings are compared with all 600 "
                + "users in the database using <i>cosine similarity</i> (closer to 1 = more alike).</li>"
                + "<li><b>Top X:</b> The X most-similar users are picked using a custom "
                + "<b>MaxHeap</b> data structure.</li>"
                + "<li><b>Top K:</b> From each of those X users, the K highest-rated movies "
                + "(that the target hasn't seen yet) are pulled.</li>"
                + "<li><b>Combine:</b> X × K candidates are merged, duplicates removed, "
                + "and you get the final list.</li>"
                + "</ol>"
                + "<h3 style='margin:10px 0 4px 0; color:#1E40AF;'>Concrete example</h3>"
                + "<p>X = 3 and K = 5 means:<br>"
                + "&nbsp;&nbsp;→ Find the 3 people whose taste is closest to yours.<br>"
                + "&nbsp;&nbsp;→ Look at each one's 5 favorite movies.<br>"
                + "&nbsp;&nbsp;→ You get up to 3 × 5 = <b>15</b> movie suggestions.</p>"
                + "<h3 style='margin:10px 0 4px 0; color:#1E40AF;'>Why do I sometimes get fewer than X × K?</h3>"
                + "<p>Similar users often share favorite movies — if 3 of your top "
                + "matches all love <i>The Matrix</i>, it counts once, not three times. "
                + "After merging the X × K candidate lists and removing duplicates, the "
                + "final list can be shorter. Increase <b>K</b> for more variety from each "
                + "person, or increase <b>X</b> to bring in more diverse users.</p>"
                + "</div></html>";
        JOptionPane.showMessageDialog(this, html, "Help", JOptionPane.INFORMATION_MESSAGE);
    }

    // ---- helpers --------------------------------------------------------

    private static JPanel stepLabel(String number, String text) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);

        JLabel num = new JLabel(number);
        num.setOpaque(true);
        num.setBackground(ACCENT);
        num.setForeground(Color.WHITE);
        num.setHorizontalAlignment(SwingConstants.CENTER);
        num.setFont(num.getFont().deriveFont(Font.BOLD, 12f));
        num.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        num.setPreferredSize(new Dimension(24, 24));

        JLabel txt = new JLabel(text);
        txt.setFont(txt.getFont().deriveFont(Font.BOLD, 13f));

        p.add(num, BorderLayout.WEST);
        p.add(txt, BorderLayout.CENTER);
        return p;
    }

    private static JLabel hintLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(HINT);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 12f));
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
}
