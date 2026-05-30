package com.movierecommender;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * RENKLİ DEGRADE BAŞLIK ÇUBUĞU.
 *
 * İki sekmenin de en üstünde kullanılan, özel boyamalı bir JPanel.
 * paintComponent override edilerek köşeleri yuvarlatılmış renk geçişi (gradient)
 * çizilir. Standart Swing bunu yapmaz; biz Graphics2D ile manuel boyuyoruz.
 *
 * Üzerinde:
 *   - Büyük başlık (beyaz, bold)
 *   - Açıklama metni (HTML formatında, biraz açık beyaz)
 *   - Sağ üstte "?" yardım butonu (JLabel + MouseListener kullanıyor,
 *     çünkü Nimbus L&F custom-renkli JButton'u boyamıyor)
 */
public class GradientHeader extends JPanel {

    // Degrade'in başlangıç ve bitiş renkleri (her sekme farklı renk geçiriyor)
    private final Color from;
    private final Color to;

    public GradientHeader(String title, String descriptionHtml,
                          Color from, Color to, ActionListener helpAction) {
        this.from = from;
        this.to = to;
        // Opak değil ki paintComponent'imiz arka planı kendisi çizebilsin
        setOpaque(false);
        setLayout(new BorderLayout(8, 6));
        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));

        JLabel descLabel = new JLabel("<html><div style='width:620px; color:#F8FAFC;'>"
                + descriptionHtml + "</div></html>");
        descLabel.setFont(descLabel.getFont().deriveFont(Font.PLAIN, 13f));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(titleLabel, BorderLayout.WEST);

        if (helpAction != null) {
            // Solid white pill with the gradient's dark color as the "?" text —
            // guaranteed contrast on both blue and purple banners.
            final Color textColor = to.darker();
            final Color baseBg    = Color.WHITE;
            final Color hoverBg   = new Color(0xE0E7FF);

            JLabel help = new JLabel("?", SwingConstants.CENTER);
            help.setOpaque(true);
            help.setBackground(baseBg);
            help.setForeground(textColor);
            help.setFont(new Font("Segoe UI", Font.BOLD, 20));
            help.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(baseBg, 2),
                    BorderFactory.createEmptyBorder(2, 8, 2, 8)));
            help.setPreferredSize(new Dimension(38, 38));
            help.setMinimumSize(new Dimension(38, 38));
            help.setToolTipText("How does this work? Click for details");
            help.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            help.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    helpAction.actionPerformed(null);
                }
                @Override
                public void mouseEntered(MouseEvent e) {
                    help.setBackground(hoverBg);
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    help.setBackground(baseBg);
                }
            });
            titleRow.add(help, BorderLayout.EAST);
        }

        add(titleRow, BorderLayout.NORTH);
        add(descLabel, BorderLayout.CENTER);
        setPreferredSize(new Dimension(getPreferredSize().width, 140));
    }

    /**
     * Panelin arka planını kendimiz boyuyoruz: sol-üstten sağ-alta degrade
     * + 16px köşe yuvarlaklığı. Antialias açık, böylece kenarlar pürüzsüz.
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        // Antialias = kenarlar pürüzsüz, "merdiven efekti" olmaz
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        // Diagonal degrade: (0,0)'da 'from' rengi, (w,h)'de 'to' rengi
        g2.setPaint(new GradientPaint(0, 0, from, w, h, to));
        // Köşeleri 16 piksel yuvarlatılmış dolgu çiz
        g2.fillRoundRect(0, 0, w, h, 16, 16);
        g2.dispose();
        super.paintComponent(g);
    }
}
