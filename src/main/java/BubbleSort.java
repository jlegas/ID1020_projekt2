import java.util.List;

/**
 * Created by justi on 2016-12-11.
 */
public class BubbleSort {

    public static void sort(List<TinySearchEngine.OrderableSearchResult> stuff, int ord) {
        if (!(stuff.size() > 0)) {
            throw new IllegalArgumentException("size of the list must be > 0");
        }

        int r = stuff.size() - 2;
        boolean swapped = true;

        //order asc
        if (ord == 1) {
            while (r >= 0 && swapped == true) {
                swapped = false;
                for (int i = 0; i <= r; i++) {
                    if (stuff.get(i).ordering > stuff.get(i + 1).ordering) {
                        swapped = true;
                        TinySearchEngine.OrderableSearchResult tmp = stuff.get(i);
                        stuff.set(i, stuff.get(i + 1));
                        stuff.set(i + 1, tmp);
                    }
                }
                r--;
            }
        }
        //order desc
        else if (ord == 2) {
            r++;
            while (r >= 1 && swapped == true) {
                swapped = false;
                for (int i = stuff.size() - 1; i >= 1; i--) {
                    //System.out.println(stuff.get(i).order + " <? " + stuff.get(i - 1).order);
                    if (stuff.get(i).ordering > stuff.get(i - 1).ordering) {
                        swapped = true;
                        TinySearchEngine.OrderableSearchResult tmp = stuff.get(i);
                        stuff.set(i, stuff.get(i - 1));
                        stuff.set(i - 1, tmp);
                    }
                }
                r--;
            }
        }
    }
}
