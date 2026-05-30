package com.movierecommender;

import java.util.ArrayList;

/**
 * ÖZEL MAX-HEAP (BİNARY MAX-HEAP)
 * --------------------------------
 * Proje şartnamesi diyor ki: "Insert and search operations will be done using heap.
 * Please don't use arrays instead". Yani plain Java dizisi (int[] gibi) kullanmak
 * yasak. Biz de bunun yerine ArrayList<UserSimilarity> kullanıyoruz; bu sayede
 * dinamik boyutlu, "gerçek bir veri yapısı" elde ediyoruz.
 *
 * MAX-HEAP NEDİR?
 *   Binary heap, tam (complete) ikili ağaç olarak organize edilmiş bir dizidir.
 *   Bizim sürümümüzde her ebeveynin skoru, çocuklarının skorundan >= olmak zorunda.
 *   Yani kökte (index 0) her zaman en büyük (en çok benzeyen) kullanıcı bulunur.
 *
 *   Bir öğenin index'i i ise:
 *     - ebeveyni       : (i - 1) / 2
 *     - sol çocuğu     : 2 * i + 1
 *     - sağ çocuğu     : 2 * i + 2
 *
 * KARMASIK­LIK:
 *   - insert      : O(log n)  (heapifyUp en fazla ağaç yüksekliği kadar adım)
 *   - extractMax  : O(log n)  (heapifyDown aynı şekilde)
 *   - size        : O(1)
 */
public class MaxHeap {

    // Heap'in arkaplandaki dinamik dizisi. ArrayList olduğu için "plain array"
    // yasağına takılmıyoruz (şartnamenin istediği gibi).
    private final ArrayList<UserSimilarity> heap;

    /** Boş heap oluşturur. */
    public MaxHeap() {
        this.heap = new ArrayList<>();
    }

    /** Bilinen büyüklük varsa baştan kapasite ayırmak için yardımcı constructor. */
    public MaxHeap(int initialCapacity) {
        this.heap = new ArrayList<>(initialCapacity);
    }

    /**
     * Yeni bir öğe ekler. İşlem:
     *  1) Önce listenin sonuna ekle (ağacın en altına yapraklarına).
     *  2) heapifyUp ile yukarı sızarak heap özelliğini geri kazandır.
     */
    public void insert(UserSimilarity us) {
        heap.add(us);
        heapifyUp(heap.size() - 1);
    }

    /**
     * En büyük öğeyi (kökü) çıkarır ve döndürür. İşlem:
     *  1) Kökü kaydet.
     *  2) Listenin son öğesini köke koy, sondan sil.
     *  3) heapifyDown ile aşağı sızarak heap özelliğini geri kazandır.
     */
    public UserSimilarity extractMax() {
        if (heap.isEmpty()) {
            throw new IllegalStateException("Boş heap'ten extractMax çağrıldı");
        }
        UserSimilarity max = heap.get(0);
        int last = heap.size() - 1;
        UserSimilarity tail = heap.remove(last);
        if (!heap.isEmpty()) {
            heap.set(0, tail);
            heapifyDown(0);
        }
        return max;
    }

    public int size() {
        return heap.size();
    }

    public boolean isEmpty() {
        return heap.isEmpty();
    }

    /**
     * YUKARI SIZDIRMA (SIFT-UP).
     * Yeni eklenen eleman ebeveyninden büyükse swap yap, bir üst seviyeye çık.
     * Bu döngü ya kök'e ulaşana ya da ebeveyn >= çocuk olana kadar sürer.
     */
    private void heapifyUp(int index) {
        while (index > 0) {
            int parent = (index - 1) / 2;
            // compareTo > 0  =>  index'tekinin skoru ebeveynden büyük => takas et
            if (heap.get(index).compareTo(heap.get(parent)) > 0) {
                swap(index, parent);
                index = parent;
            } else {
                break;
            }
        }
    }

    /**
     * AŞAĞI SIZDIRMA (SIFT-DOWN).
     * Kökten başlayarak, çocuklardan büyüğü ile yer değiştir; ta ki her iki
     * çocuktan da büyük (ya da çocuksuz) hale gelene kadar.
     */
    private void heapifyDown(int index) {
        int n = heap.size();
        while (true) {
            int left = 2 * index + 1;
            int right = 2 * index + 2;
            int largest = index;

            // Sol çocuk daha büyük mü?
            if (left < n && heap.get(left).compareTo(heap.get(largest)) > 0) {
                largest = left;
            }
            // Sağ çocuk daha da mı büyük?
            if (right < n && heap.get(right).compareTo(heap.get(largest)) > 0) {
                largest = right;
            }
            // En büyük zaten kendimizsek heap özelliği sağlanmış demektir, çık.
            if (largest == index) {
                break;
            }
            swap(index, largest);
            index = largest;
        }
    }

    /** İki index'teki öğeyi takaslar (heap işlemlerinin tek yardımcısı). */
    private void swap(int i, int j) {
        UserSimilarity tmp = heap.get(i);
        heap.set(i, heap.get(j));
        heap.set(j, tmp);
    }
}
