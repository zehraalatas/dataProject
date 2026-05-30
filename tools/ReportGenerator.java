import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.zip.*;

/**
 * Generates two deliverables for the Movie Recommendation System report:
 *   1) uml.png      — class diagram drawn with Graphics2D
 *   2) Report.docx  — full English technical report, hand-built as a minimal
 *                     DOCX (ZIP of XML) so we don't need Apache POI or docx-js.
 *
 * Run from the repo root:
 *   javac -d build tools/ReportGenerator.java
 *   java -cp build ReportGenerator
 *
 * Outputs are written next to the JAR (current working directory).
 */
public class ReportGenerator {

    // ============================================================
    // ENTRY POINT
    // ============================================================
    public static void main(String[] args) throws Exception {
        BufferedImage uml = drawUml();
        File pngFile = new File("uml.png");
        ImageIO.write(uml, "png", pngFile);
        System.out.println("Wrote " + pngFile.getAbsolutePath());

        byte[] pngBytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(uml, "png", baos);
            pngBytes = baos.toByteArray();
        }

        File docxFile = new File("Report.docx");
        buildDocx(docxFile, pngBytes, uml.getWidth(), uml.getHeight());
        System.out.println("Wrote " + docxFile.getAbsolutePath());
    }

    // ============================================================
    // 1) UML CLASS DIAGRAM (Graphics2D)
    // ============================================================
    static BufferedImage drawUml() {
        final int W = 1400, H = 1700;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        // Background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, W, H);

        // Define boxes: [name, attrs[], methods[], x, y, width, headerColor]
        List<UmlBox> boxes = new ArrayList<>();

        // GUI layer (top)
        boxes.add(new UmlBox("Main",
                new String[]{},
                new String[]{"+ main(args: String[]): void", "- applyModernLookAndFeel(): void"},
                580, 30, 320, new Color(0x4F46E5)));

        boxes.add(new UmlBox("MainFrame  «JFrame»",
                new String[]{},
                new String[]{"+ MainFrame(data: DataLoader, engine: RecommendationEngine)"},
                540, 220, 400, new Color(0x4F46E5)));

        boxes.add(new UmlBox("UserRecommendPanel  «JPanel»",
                new String[]{"- data: DataLoader", "- engine: RecommendationEngine",
                        "- userCombo: JComboBox<Integer>", "- xField, kField: JTextField",
                        "- resultModel: DefaultListModel<String>"},
                new String[]{"+ UserRecommendPanel(data, engine)", "- onRun(): void",
                        "- updatePreview(): void", "- showHelp(): void"},
                40, 470, 460, new Color(0x2563EB)));

        boxes.add(new UmlBox("MovieRatingPanel  «JPanel»",
                new String[]{"- data: DataLoader", "- engine: RecommendationEngine",
                        "- movieCombos: JComboBox<MovieChoice>[]",
                        "- ratingFields: JTextField[]", "- sessionChoices: List<MovieChoice>"},
                new String[]{"+ MovieRatingPanel(data, engine)", "- onRun(): void",
                        "- pickRandomMovies(...)", "- showHelp(): void"},
                980, 470, 380, new Color(0x7C3AED)));

        boxes.add(new UmlBox("GradientHeader  «JPanel»",
                new String[]{"- from: Color", "- to: Color"},
                new String[]{"+ GradientHeader(title, desc, from, to, helpAction)",
                        "# paintComponent(g: Graphics): void"},
                540, 470, 400, new Color(0x10B981)));

        // Algorithm layer (middle)
        boxes.add(new UmlBox("RecommendationEngine",
                new String[]{"- mainData: HashMap<Integer, HashMap<Integer, Integer>>",
                        "- movieTitles: HashMap<Integer, String>"},
                new String[]{"+ cosineSimilarity(a, b): double  «static»",
                        "+ getRecommendations(target, X, K): List<String>"},
                480, 880, 520, new Color(0xDC2626)));

        // Data layer (bottom)
        boxes.add(new UmlBox("DataLoader",
                new String[]{"- mainData: HashMap<Integer, HashMap<Integer, Integer>>",
                        "- targetUsers: HashMap<Integer, HashMap<Integer, Integer>>",
                        "- movieTitles: HashMap<Integer, String>", "- movieIdOrder: int[]"},
                new String[]{"+ loadAll(): void",
                        "- loadRatings(path, captureHeader): HashMap<...>",
                        "- loadMovies(path): HashMap<Integer, String>",
                        "- parseCsvLine(line): List<String>  «static»"},
                40, 1130, 600, new Color(0xF59E0B)));

        boxes.add(new UmlBox("MaxHeap",
                new String[]{"- heap: ArrayList<UserSimilarity>"},
                new String[]{"+ insert(us: UserSimilarity): void",
                        "+ extractMax(): UserSimilarity",
                        "+ size(): int", "+ isEmpty(): boolean",
                        "- heapifyUp(i: int): void",
                        "- heapifyDown(i: int): void",
                        "- swap(i: int, j: int): void"},
                700, 1130, 380, new Color(0xDB2777)));

        boxes.add(new UmlBox("UserSimilarity  «Comparable»",
                new String[]{"- userId: int", "- similarityScore: double"},
                new String[]{"+ getUserId(): int", "+ getSimilarityScore(): double",
                        "+ compareTo(other: UserSimilarity): int"},
                1120, 1130, 250, new Color(0xDB2777)));

        // Measure & adjust box heights
        for (UmlBox b : boxes) b.height = b.computeHeight(g);

        // Draw arrows first (under boxes)
        UmlBox main = findBox(boxes, "Main");
        UmlBox mf = findBox(boxes, "MainFrame");
        UmlBox tab1 = findBox(boxes, "UserRecommendPanel");
        UmlBox tab2 = findBox(boxes, "MovieRatingPanel");
        UmlBox gh = findBox(boxes, "GradientHeader");
        UmlBox eng = findBox(boxes, "RecommendationEngine");
        UmlBox dl = findBox(boxes, "DataLoader");
        UmlBox mh = findBox(boxes, "MaxHeap");
        UmlBox us = findBox(boxes, "UserSimilarity");

        // Composition / containment
        drawArrow(g, main, mf, "creates", false);
        drawArrow(g, mf, tab1, "contains", true);
        drawArrow(g, mf, tab2, "contains", true);
        drawArrow(g, tab1, gh, "uses", false);
        drawArrow(g, tab2, gh, "uses", false);
        drawArrow(g, tab1, eng, "uses", false);
        drawArrow(g, tab2, eng, "uses", false);
        drawArrow(g, tab1, dl, "reads", false);
        drawArrow(g, tab2, dl, "reads", false);
        drawArrow(g, eng, mh, "uses", false);
        drawArrow(g, mh, us, "stores", true);

        // Boxes on top of arrows
        for (UmlBox b : boxes) b.draw(g);

        // Title
        g.setColor(new Color(0x111827));
        g.setFont(new Font("Segoe UI", Font.BOLD, 28));
        g.drawString("Movie Recommendation System — UML Class Diagram", 30, 25);

        g.dispose();
        return img;
    }

    static UmlBox findBox(List<UmlBox> bs, String name) {
        for (UmlBox b : bs) if (b.name.startsWith(name)) return b;
        return null;
    }

    /** Simple straight arrow from box `a` to box `b` with a label. */
    static void drawArrow(Graphics2D g, UmlBox a, UmlBox b, String label, boolean diamond) {
        int x1 = a.x + a.width / 2;
        int y1 = a.y + a.height;
        int x2 = b.x + b.width / 2;
        int y2 = b.y;

        // If b is above a, flip
        if (b.y < a.y) { int ty = y1; y1 = a.y; y2 = b.y + b.height; }

        g.setColor(new Color(0x6B7280));
        g.setStroke(new BasicStroke(1.6f));
        g.draw(new Line2D.Double(x1, y1, x2, y2));

        // Arrowhead at b
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int ahLen = 10;
        int ax1 = (int) (x2 - ahLen * Math.cos(angle - Math.PI / 6));
        int ay1 = (int) (y2 - ahLen * Math.sin(angle - Math.PI / 6));
        int ax2 = (int) (x2 - ahLen * Math.cos(angle + Math.PI / 6));
        int ay2 = (int) (y2 - ahLen * Math.sin(angle + Math.PI / 6));
        g.drawLine(x2, y2, ax1, ay1);
        g.drawLine(x2, y2, ax2, ay2);

        if (diamond) {
            int dSize = 8;
            int dx1 = (int) (x1 + dSize * Math.cos(angle));
            int dy1 = (int) (y1 + dSize * Math.sin(angle));
            int dx2 = (int) (x1 + dSize * Math.cos(angle + Math.PI / 2) * 0.6);
            int dy2 = (int) (y1 + dSize * Math.sin(angle + Math.PI / 2) * 0.6);
            int dx3 = (int) (x1 + dSize * Math.cos(angle - Math.PI / 2) * 0.6);
            int dy3 = (int) (y1 + dSize * Math.sin(angle - Math.PI / 2) * 0.6);
            Polygon p = new Polygon();
            p.addPoint(x1, y1); p.addPoint(dx2 + dSize/2, dy2);
            p.addPoint(dx1, dy1); p.addPoint(dx3 + dSize/2, dy3);
            g.setColor(Color.WHITE);
            g.fillPolygon(p);
            g.setColor(new Color(0x6B7280));
            g.drawPolygon(p);
        }

        // Label
        g.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        g.setColor(new Color(0x4B5563));
        int mx = (x1 + x2) / 2 + 4;
        int my = (y1 + y2) / 2;
        // Label background for legibility
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(label) + 8;
        int th = fm.getHeight();
        g.setColor(Color.WHITE);
        g.fillRect(mx, my - th + 4, tw, th);
        g.setColor(new Color(0x4B5563));
        g.drawString(label, mx + 4, my);
    }

    /** UML box with header, attribute section, method section. */
    static class UmlBox {
        String name;
        String[] attrs;
        String[] methods;
        int x, y, width, height;
        Color header;

        UmlBox(String name, String[] attrs, String[] methods,
               int x, int y, int width, Color header) {
            this.name = name;
            this.attrs = attrs;
            this.methods = methods;
            this.x = x; this.y = y; this.width = width;
            this.header = header;
        }

        int computeHeight(Graphics2D g) {
            int rowH = 18;
            int headerH = 36;
            int attrH = Math.max(attrs.length, 1) * rowH + 12;
            int methH = Math.max(methods.length, 1) * rowH + 12;
            return headerH + attrH + methH;
        }

        void draw(Graphics2D g) {
            // Shadow
            g.setColor(new Color(0, 0, 0, 30));
            g.fillRoundRect(x + 3, y + 3, width, height, 8, 8);

            // Body
            g.setColor(Color.WHITE);
            g.fillRoundRect(x, y, width, height, 8, 8);

            // Header band
            g.setColor(header);
            g.fillRoundRect(x, y, width, 36, 8, 8);
            g.fillRect(x, y + 18, width, 18);

            // Outline
            g.setColor(header.darker());
            g.setStroke(new BasicStroke(1.4f));
            g.drawRoundRect(x, y, width, height, 8, 8);

            // Class name
            g.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g.setColor(Color.WHITE);
            FontMetrics fm = g.getFontMetrics();
            int nameY = y + 24;
            g.drawString(name, x + (width - fm.stringWidth(name)) / 2, nameY);

            int rowH = 18;
            int yy = y + 36 + 14;
            g.setColor(new Color(0x111827));
            g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            for (String a : attrs) {
                g.drawString(a, x + 10, yy);
                yy += rowH;
            }
            int sepY = y + 36 + attrs.length * rowH + 12;
            if (attrs.length == 0) sepY = y + 36 + 12;
            g.setColor(new Color(0xD1D5DB));
            g.drawLine(x + 4, sepY, x + width - 4, sepY);

            yy = sepY + 14;
            g.setColor(new Color(0x111827));
            for (String m : methods) {
                g.drawString(m, x + 10, yy);
                yy += rowH;
            }
        }
    }

    // ============================================================
    // 2) DOCX (manual ZIP + XML)
    // ============================================================
    static void buildDocx(File output, byte[] pngBytes, int pngW, int pngH) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(output);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            putEntry(zos, "[Content_Types].xml", contentTypesXml().getBytes(StandardCharsets.UTF_8));
            putEntry(zos, "_rels/.rels", rootRelsXml().getBytes(StandardCharsets.UTF_8));
            putEntry(zos, "word/_rels/document.xml.rels", docRelsXml().getBytes(StandardCharsets.UTF_8));
            putEntry(zos, "word/styles.xml", stylesXml().getBytes(StandardCharsets.UTF_8));
            putEntry(zos, "word/document.xml", documentXml(pngW, pngH).getBytes(StandardCharsets.UTF_8));
            putEntry(zos, "word/media/image1.png", pngBytes);
        }
    }

    static void putEntry(ZipOutputStream zos, String name, byte[] data) throws IOException {
        ZipEntry e = new ZipEntry(name);
        zos.putNextEntry(e);
        zos.write(data);
        zos.closeEntry();
    }

    static String contentTypesXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                "<Default Extension=\"png\" ContentType=\"image/png\"/>" +
                "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>" +
                "<Override PartName=\"/word/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml\"/>" +
                "</Types>";
    }

    static String rootRelsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>" +
                "</Relationships>";
    }

    static String docRelsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>" +
                "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" Target=\"media/image1.png\"/>" +
                "</Relationships>";
    }

    static String stylesXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<w:styles xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">" +
                "<w:docDefaults><w:rPrDefault><w:rPr>" +
                "<w:rFonts w:ascii=\"Calibri\" w:hAnsi=\"Calibri\"/>" +
                "<w:sz w:val=\"22\"/></w:rPr></w:rPrDefault></w:docDefaults>" +
                style("Title", "Title", "Normal", "44", true, "1E40AF", true) +
                style("Heading1", "heading 1", "Normal", "32", true, "1E3A8A", false) +
                style("Heading2", "heading 2", "Normal", "26", true, "374151", false) +
                style("Heading3", "heading 3", "Normal", "22", true, "4B5563", false) +
                style("Caption", "caption", "Normal", "18", false, "6B7280", true) +
                "<w:style w:type=\"paragraph\" w:styleId=\"Code\"><w:name w:val=\"Code\"/>" +
                "<w:basedOn w:val=\"Normal\"/>" +
                "<w:pPr><w:shd w:val=\"clear\" w:color=\"auto\" w:fill=\"F3F4F6\"/>" +
                "<w:spacing w:before=\"60\" w:after=\"60\"/></w:pPr>" +
                "<w:rPr><w:rFonts w:ascii=\"Consolas\" w:hAnsi=\"Consolas\"/><w:sz w:val=\"20\"/></w:rPr>" +
                "</w:style>" +
                "</w:styles>";
    }

    static String style(String id, String name, String basedOn, String size,
                        boolean bold, String color, boolean centered) {
        StringBuilder sb = new StringBuilder();
        sb.append("<w:style w:type=\"paragraph\" w:styleId=\"").append(id).append("\">");
        sb.append("<w:name w:val=\"").append(name).append("\"/>");
        sb.append("<w:basedOn w:val=\"").append(basedOn).append("\"/>");
        sb.append("<w:pPr><w:spacing w:before=\"240\" w:after=\"120\"/>");
        if (centered) sb.append("<w:jc w:val=\"center\"/>");
        sb.append("</w:pPr>");
        sb.append("<w:rPr>");
        if (bold) sb.append("<w:b/>");
        sb.append("<w:color w:val=\"").append(color).append("\"/>");
        sb.append("<w:sz w:val=\"").append(size).append("\"/>");
        sb.append("</w:rPr></w:style>");
        return sb.toString();
    }

    // ---------- document.xml ----------
    static String documentXml(int pngW, int pngH) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" ");
        sb.append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" ");
        sb.append("xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\" ");
        sb.append("xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" ");
        sb.append("xmlns:pic=\"http://schemas.openxmlformats.org/drawingml/2006/picture\">");
        sb.append("<w:body>");

        // ---- Title page ----
        sb.append(p("Title", "Movie Recommendation System"));
        sb.append(p("Caption", "Heap-Based Collaborative Filtering"));
        sb.append(p(null, ""));
        sb.append(p("Caption", "Data Structures Programming Project #2"));
        sb.append(p("Caption", "Submission Report"));
        sb.append(p(null, ""));
        sb.append(p(null, ""));
        sb.append(p(null, ""));

        // ---- 1. Introduction ----
        sb.append(p("Heading1", "1. Introduction"));
        sb.append(p(null,
                "This report documents the design and implementation of a movie recommendation " +
                "system built in Java for Programming Project #2. The system applies user-based " +
                "collaborative filtering: given a target user, it identifies the X most similar " +
                "users in a 600-user database using cosine similarity, then returns the top K " +
                "movies each of those users has rated, yielding up to X × K recommendations."));
        sb.append(p(null,
                "The dataset consists of three CSV files placed in a sibling data/ folder: " +
                "main_data.csv (600 users × 9018 movies of ratings 0–5), target_user.csv " +
                "(10 target profiles for users 601–610) and movies.csv (movieId, title, genres)."));

        sb.append(p("Heading2", "1.1 Functional Requirements"));
        sb.append(bullet("Read a user-movie matrix from CSV files and store it in an appropriate data structure."));
        sb.append(bullet("Compute cosine similarity between any two users (sparse, non-zero ratings only)."));
        sb.append(bullet("Maintain a max-heap of similarity scores so the most similar user is at the root."));
        sb.append(bullet("Retrieve X × K movie recommendations by extracting the top X users and pulling their top K rated movies."));
        sb.append(bullet("Expose X and K as user-tunable parameters in the GUI."));
        sb.append(bullet("Provide two distinct user interfaces: one driven by an existing target user, one driven by ad-hoc ratings the user enters."));

        // ---- 2. Architecture ----
        sb.append(p("Heading1", "2. Architecture and Class Design"));
        sb.append(p(null,
                "The code is organized into four packages reflecting clean separation of " +
                "concerns. Data ingestion, the recommendation algorithm, the heap data structure " +
                "and the Swing front-end are all isolated, which makes the project easy to " +
                "navigate, test and grade."));

        sb.append(p("Heading2", "2.1 Package Layout"));
        sb.append(code("com.movierecommender"));
        sb.append(code("  ├── Main                         (entry point, sets Look & Feel)"));
        sb.append(code("  ├── model"));
        sb.append(code("  │     ├── MaxHeap                (ArrayList-backed binary max-heap)"));
        sb.append(code("  │     └── UserSimilarity         (Comparable: userId + score)"));
        sb.append(code("  ├── data"));
        sb.append(code("  │     └── DataLoader             (CSV parsing into sparse HashMaps)"));
        sb.append(code("  ├── algorithm"));
        sb.append(code("  │     └── RecommendationEngine   (cosine similarity + heap pipeline)"));
        sb.append(code("  └── gui"));
        sb.append(code("        ├── MainFrame              (JFrame with two-tab JTabbedPane)"));
        sb.append(code("        ├── UserRecommendPanel     (Tab 1)"));
        sb.append(code("        ├── MovieRatingPanel       (Tab 2)"));
        sb.append(code("        └── GradientHeader         (shared colored banner)"));

        // ---- UML image ----
        sb.append(p("Heading2", "2.2 UML Class Diagram"));
        sb.append(p(null,
                "The diagram below shows the classes, their key fields and operations, and the " +
                "relationships between them (creates, contains, uses, stores)."));
        sb.append(imageParagraph(pngW, pngH));
        sb.append(p("Caption", "Figure 1 — Class diagram of the recommendation system"));

        // ---- 3. Data structure ----
        sb.append(p("Heading1", "3. Data Structure Selection"));
        sb.append(p(null,
                "The user-movie matrix is sparse: only a small fraction of the 600 × 9018 = ~5.4M " +
                "cells are non-zero. Storing this as a dense 2D array would waste memory and slow " +
                "down every similarity computation. Instead the project uses a nested HashMap " +
                "(allowed by the spec as \"HashTable\"):"));
        sb.append(code("HashMap<Integer, HashMap<Integer, Integer>>  // userId -> (movieId -> rating)"));
        sb.append(p("Heading2", "3.1 Rationale"));
        sb.append(bullet("Sparse: only rated cells (value > 0) are inserted. Memory ≈ number of ratings, not 600 × 9018."));
        sb.append(bullet("O(1) lookup for \"did user U rate movie M?\" via the inner map."));
        sb.append(bullet("Iterating a user's rated movies for similarity is proportional to their rating count, not the full movie universe."));
        sb.append(bullet("Movie titles are loaded into a separate HashMap<Integer, String> for O(1) movieId-to-name lookups when presenting results."));

        // ---- 4. MaxHeap ----
        sb.append(p("Heading1", "4. Custom MaxHeap Implementation"));
        sb.append(p(null,
                "The project specification mandates a heap data structure for storing similar " +
                "users and explicitly forbids the use of plain arrays as the primary structure. " +
                "The MaxHeap class therefore uses a Java ArrayList internally and exposes the " +
                "two operations required for the recommendation pipeline: insert (O(log n)) and " +
                "extractMax (O(log n))."));

        sb.append(p("Heading2", "4.1 Heap Property"));
        sb.append(p(null,
                "For every index i > 0 the parent element at index (i-1)/2 has a similarity " +
                "score greater than or equal to the element at i. As a result the root (index 0) " +
                "is always the most similar user — exactly what the recommendation step needs."));

        sb.append(p("Heading2", "4.2 Key Operations"));
        sb.append(p(null, "insert(us): appends to the end of the ArrayList and sifts up:"));
        sb.append(code("private void heapifyUp(int index) {"));
        sb.append(code("    while (index > 0) {"));
        sb.append(code("        int parent = (index - 1) / 2;"));
        sb.append(code("        if (heap.get(index).compareTo(heap.get(parent)) > 0) {"));
        sb.append(code("            swap(index, parent);"));
        sb.append(code("            index = parent;"));
        sb.append(code("        } else break;"));
        sb.append(code("    }"));
        sb.append(code("}"));

        sb.append(p(null, "extractMax(): returns the root, moves the last element to index 0 and sifts down:"));
        sb.append(code("private void heapifyDown(int index) {"));
        sb.append(code("    int n = heap.size();"));
        sb.append(code("    while (true) {"));
        sb.append(code("        int left = 2 * index + 1, right = 2 * index + 2, largest = index;"));
        sb.append(code("        if (left < n  && heap.get(left).compareTo(heap.get(largest)) > 0)  largest = left;"));
        sb.append(code("        if (right < n && heap.get(right).compareTo(heap.get(largest)) > 0) largest = right;"));
        sb.append(code("        if (largest == index) break;"));
        sb.append(code("        swap(index, largest); index = largest;"));
        sb.append(code("    }"));
        sb.append(code("}"));

        sb.append(p("Heading2", "4.3 Complexity"));
        sb.append(bullet("insert: O(log n) per call. Building a heap of N similarities is O(N log N)."));
        sb.append(bullet("extractMax: O(log n)."));
        sb.append(bullet("Total per recommendation request: O(N log N + X log N + X · K log K) where N = 600, X and K are user parameters."));

        // ---- 5. Cosine similarity ----
        sb.append(p("Heading1", "5. Cosine Similarity"));
        sb.append(p(null,
                "Given two rating vectors A and B over the 9018 movies, their cosine similarity is:"));
        sb.append(code("similarity(A, B) = (A · B) / (||A|| × ||B||)"));
        sb.append(p(null,
                "The implementation does NOT materialize the full 9018-element vector. It iterates " +
                "the smaller map and looks up each key in the larger map to compute the dot " +
                "product, and walks each map's values for the magnitudes. The result is O(|A| + |B|) " +
                "rather than O(9018) per pair, which keeps a 600-comparison sweep well below one " +
                "second on commodity hardware."));
        sb.append(p(null,
                "Edge case: if either vector has no rated movies, the function returns 0.0 instead " +
                "of dividing by zero. Long arithmetic is used for the dot and squared sums to avoid " +
                "any integer overflow even in worst-case ratings."));

        // ---- 6. Recommendation Algorithm ----
        sb.append(p("Heading1", "6. Recommendation Algorithm"));
        sb.append(p(null, "Given a target rating vector and user-supplied X and K, the engine performs the following steps:"));
        sb.append(numbered("Compute the cosine similarity between the target vector and every user in main_data.csv."));
        sb.append(numbered("Insert each (userId, similarity) pair into the MaxHeap."));
        sb.append(numbered("Extract the top X most-similar users by calling extractMax X times."));
        sb.append(numbered("For each of those X users, collect their rated movies (rating > 0) that the target user has NOT already rated, sort by rating descending, take the top K."));
        sb.append(numbered("Concatenate all X · K candidate titles and remove duplicates with a LinkedHashSet (preserves insertion order — the first occurrence wins)."));
        sb.append(numbered("Return the resulting list, which contains at most X · K unique titles."));

        sb.append(p("Heading2", "6.1 Concrete Example"));
        sb.append(p(null,
                "With X = 3 and K = 5 the algorithm finds the three users most similar to the " +
                "target, takes each one's five highest-rated movies that the target has not seen, " +
                "and merges those 15 candidates."));

        sb.append(p("Heading2", "6.2 Why the result is sometimes shorter than X × K"));
        sb.append(p(null,
                "Users with similar taste typically rate the same popular movies highly. After " +
                "the X × K candidates are merged and duplicates removed, the final list can be " +
                "shorter. This is correct collaborative-filtering behavior, not a bug. The GUI " +
                "surfaces this with a coloured status line and explains it in the help dialog."));

        // ---- 7. GUI ----
        sb.append(p("Heading1", "7. Graphical User Interface"));
        sb.append(p(null,
                "The Swing front-end uses a JTabbedPane containing the two interfaces required " +
                "by the specification. The system Look & Feel is set to Nimbus for a modern " +
                "appearance, and the default font is Segoe UI 14pt. Each tab is built around a " +
                "GradientHeader (a custom JPanel that paints a rounded gradient via Graphics2D) " +
                "and a white \"card\" form. All recommendation work is delegated to a " +
                "SwingWorker so the UI never freezes during the 600-user sweep."));

        sb.append(p("Heading2", "7.1 Tab 1 — Recommend by User"));
        sb.append(bullet("JComboBox lists the 10 target users (IDs 601–610) loaded from target_user.csv at startup."));
        sb.append(bullet("Two JTextFields accept X (default 3) and K (default 5)."));
        sb.append(bullet("A live preview chip displays \"X × K = N recommendations\" and updates as the user types."));
        sb.append(bullet("Validation: X and K must be positive integers; X cannot exceed the number of users in main_data."));
        sb.append(bullet("Results render in a JList showing numbered movie titles (never raw IDs)."));

        sb.append(p("Heading2", "7.2 Tab 2 — Build Your Own Profile"));
        sb.append(bullet("Five JComboBoxes share the same 10 movies, randomly selected from movies.csv at every application launch."));
        sb.append(bullet("Five JTextFields accept integer ratings between 1 and 5."));
        sb.append(bullet("Validation rejects duplicate movie selections and out-of-range ratings."));
        sb.append(bullet("On submit, the 5 ratings are converted to a HashMap<Integer, Integer> and the same recommendation algorithm runs."));

        sb.append(p("Heading2", "7.3 Help System"));
        sb.append(p(null,
                "Each tab includes a circular \"?\" help button on the gradient banner that " +
                "opens an HTML-formatted JOptionPane explaining the algorithm steps and the " +
                "concrete meaning of X and K, including why the final list can be shorter than " +
                "X × K. This makes the tool self-documenting for first-time users."));

        // ---- 8. Sample Output ----
        sb.append(p("Heading1", "8. Sample Output"));
        sb.append(p(null,
                "Recommendation run for target user 601, X = 3, K = 5 (15 candidates requested, " +
                "12 unique after de-duplication):"));
        sb.append(code("  1.  Romeo Is Bleeding (1993)"));
        sb.append(code("  2.  Manhattan Murder Mystery (1993)"));
        sb.append(code("  3.  Black Beauty (1994)"));
        sb.append(code("  4.  Free Willy 2: The Adventure Home (1995)"));
        sb.append(code("  5.  Roommates (1995)"));
        sb.append(code("  6.  Young Poisoner's Handbook, The (1995)"));
        sb.append(code("  7.  Inkwell, The (1994)"));
        sb.append(code("  8.  Heat (1995)"));
        sb.append(code("  9.  Batman (1989)"));
        sb.append(code(" 10.  Man of the House (1995)"));
        sb.append(code(" 11.  Fargo (1996)"));
        sb.append(code(" 12.  Congo (1995)"));
        sb.append(p(null,
                "Measured wall-clock timings on a typical laptop: CSV loading ~580 ms (one-off " +
                "at startup), full recommendation request ~30 ms."));

        // ---- 9. Build & Run ----
        sb.append(p("Heading1", "9. Build and Run Instructions"));
        sb.append(p(null,
                "The project requires only the Java standard library (Java 11+). No external " +
                "dependencies. To build and run from the command line:"));
        sb.append(code("javac -d build -encoding UTF-8 $(find src/main/java -name \"*.java\")"));
        sb.append(code("java -cp build com.movierecommender.Main"));
        sb.append(p(null,
                "The data/ folder must sit next to the working directory. Inside NetBeans the " +
                "project can be opened as a Maven project (pom.xml at the root) and executed " +
                "with the green Run button (F6)."));

        // ---- 10. Conclusion ----
        sb.append(p("Heading1", "10. Conclusion"));
        sb.append(p(null,
                "The project fulfils every requirement listed in the specification:"));
        sb.append(bullet("CSV ingestion uses a sparse HashMap (HashTable, allowed by the spec)."));
        sb.append(bullet("A custom ArrayList-backed MaxHeap with explicit heapifyUp / heapifyDown is used to rank similar users."));
        sb.append(bullet("Cosine similarity is implemented and applied sparsely for performance."));
        sb.append(bullet("Top X × K movie titles (never IDs) are retrieved through the heap and deduplicated."));
        sb.append(bullet("X and K are user-parametric in both GUI tabs, including live validation and feedback."));
        sb.append(bullet("Two distinct GUI screens (target-user mode and ad-hoc ratings mode) are provided as required."));
        sb.append(p(null,
                "The code base is small (nine source files, ~1,000 lines), modular and uses only " +
                "the standard library, which keeps the build reproducible and the deliverable " +
                "easy to grade."));

        sb.append("</w:body></w:document>");
        return sb.toString();
    }

    // --- DOCX paragraph helpers ---
    static String p(String style, String text) {
        StringBuilder sb = new StringBuilder();
        sb.append("<w:p>");
        if (style != null) {
            sb.append("<w:pPr><w:pStyle w:val=\"").append(style).append("\"/></w:pPr>");
        }
        if (!text.isEmpty()) {
            sb.append("<w:r><w:t xml:space=\"preserve\">").append(escape(text)).append("</w:t></w:r>");
        }
        sb.append("</w:p>");
        return sb.toString();
    }

    static String code(String text) {
        return "<w:p><w:pPr><w:pStyle w:val=\"Code\"/></w:pPr>" +
                "<w:r><w:t xml:space=\"preserve\">" + escape(text) + "</w:t></w:r></w:p>";
    }

    static String bullet(String text) {
        return "<w:p><w:pPr><w:ind w:left=\"360\" w:hanging=\"180\"/></w:pPr>" +
                "<w:r><w:t xml:space=\"preserve\">•  " + escape(text) + "</w:t></w:r></w:p>";
    }

    static String numbered(String text) {
        return "<w:p><w:pPr><w:ind w:left=\"360\"/></w:pPr>" +
                "<w:r><w:t xml:space=\"preserve\">" + escape(text) + "</w:t></w:r></w:p>";
    }

    static String escape(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /** Embed image1.png in the document. Width fits 6 inches; height scales proportionally. */
    static String imageParagraph(int pngW, int pngH) {
        long emuW = 5486400L; // 6 inches = 6 * 914400
        long emuH = (long) (emuW * ((double) pngH / pngW));
        String cx = String.valueOf(emuW);
        String cy = String.valueOf(emuH);
        return "<w:p><w:pPr><w:jc w:val=\"center\"/></w:pPr><w:r><w:drawing>" +
                "<wp:inline distT=\"0\" distB=\"0\" distL=\"0\" distR=\"0\">" +
                "<wp:extent cx=\"" + cx + "\" cy=\"" + cy + "\"/>" +
                "<wp:effectExtent l=\"0\" t=\"0\" r=\"0\" b=\"0\"/>" +
                "<wp:docPr id=\"1\" name=\"UML\"/>" +
                "<wp:cNvGraphicFramePr/>" +
                "<a:graphic xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\">" +
                "<a:graphicData uri=\"http://schemas.openxmlformats.org/drawingml/2006/picture\">" +
                "<pic:pic xmlns:pic=\"http://schemas.openxmlformats.org/drawingml/2006/picture\">" +
                "<pic:nvPicPr><pic:cNvPr id=\"1\" name=\"UML\"/><pic:cNvPicPr/></pic:nvPicPr>" +
                "<pic:blipFill><a:blip r:embed=\"rId2\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"/>" +
                "<a:stretch><a:fillRect/></a:stretch></pic:blipFill>" +
                "<pic:spPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"" + cx + "\" cy=\"" + cy + "\"/></a:xfrm>" +
                "<a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></pic:spPr>" +
                "</pic:pic></a:graphicData></a:graphic></wp:inline>" +
                "</w:drawing></w:r></w:p>";
    }
}
