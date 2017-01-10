import se.kth.id1020.TinySearchEngineBase;
import se.kth.id1020.util.Attributes;
import se.kth.id1020.util.Document;
import se.kth.id1020.util.Sentence;
import se.kth.id1020.util.Word;

import java.util.*;

import static java.lang.Math.log10;

/**
 * Created by justi on 2016-12-10.
 */


public class TinySearchEngine implements TinySearchEngineBase {
    public Map<String, List<Attributes>> indexedWords = new HashMap<>();
    public Map<Document, Integer> wordCounts = new HashMap<>();

    public String[] operatorList = {"+", "|", "-"};
    public Boolean needsOrdering = false;
    public Boolean needsRelevance = false;
    public String orderBy;
    public String orderDir;


    public void preInserts() {

    }

    public void insert(Sentence sentence, Attributes attributes) {
        for (Word word : sentence.getWords()) {
            //count words in a document
            Document currentDoc = attributes.document;
            if (wordCounts.containsKey(currentDoc)) {
                wordCounts.put(currentDoc, (wordCounts.get(currentDoc) + 1));
            } else {
                wordCounts.put(currentDoc, 1);
            }
            //indexing
            if (indexedWords.containsKey(word.word)) {
                indexedWords.get(word.word).add(attributes);
            } else {
                List<Attributes> attrList = new ArrayList<>();
                attrList.add(attributes);
                indexedWords.put(word.word, attrList);
            }
        }
    }

    public void postInserts() {

    }

    public List<Document> search(String query) {
        List<Document> results = new ArrayList<>();
        String[] input = query.split(" ");

        if (input.length >= 4 && input[input.length - 3].equals("orderby")) { //orderby clause extraction
            needsOrdering = true;
            orderDir = input[input.length - 1];
            orderBy = input[input.length - 2];
            if (orderBy.equals("relevance")) {
                needsRelevance = true;
            }
            String[] tmp = java.util.Arrays.copyOf(input, input.length - 3);
            System.out.println(tmp[0]);
            input = tmp;
        }

        Map<Document, Integer> tmp = parseAndSearch(input);

        //orderby(property, direction, list)
        if (needsOrdering) {
            results = orderBy(orderBy, orderDir, tmp);
        } else {
            results.addAll(tmp.keySet());
        }
        return results;
    }

    public String infix(String s) {
        String[] input = s.split(" ");
        String[] query;
        StringBuilder infix = new StringBuilder();
        if (input.length >= 4 && input[input.length - 3].equals("orderby")) { //orderby clause extraction
            query = java.util.Arrays.copyOf(input, input.length - 3);
            infix.append(" orderby " + input[input.length - 2] + ' ' + input[input.length - 1]);
        } else {
            query = input;
        }
        Stack<String> operands = new Stack<>();
        for (int i = (query.length - 1); i >= 0; i--) {
            if (Arrays.asList(operatorList).contains(query[i])) {
                String a = operands.pop();
                String b = operands.pop();
                StringBuilder tmp = new StringBuilder();
                tmp.append(')');
                tmp.insert(0, b);
                tmp.insert(0, query[i]);
                tmp.insert(0, a);
                tmp.insert(0, '(');
                operands.push(tmp.toString());
            } else {
                operands.push(query[i]);
                //System.out.println(operands.peek());
            }
        }
        return        operands.pop() + infix.toString();
    }

    private Map<Document, Integer> parseAndSearch(String[] queries) {
        Map<Document, Integer> aResults = new HashMap<>();
        Map<Document, Integer> bResults = new HashMap<>();
        Stack<String> operands = new Stack<>();
        Stack<Map<Document, Integer>> sets = new Stack<>();

        for (int i = (queries.length - 1); i >= 0; i--) {
            //perform operation
            if (Arrays.asList(operatorList).contains(queries[i])) {
                String a = operands.pop();
                String b = operands.pop();
                if (!(b.equals("/"))) {
                    bResults = searchString(b);
                } else {
                    bResults = sets.pop();
                }
                if (!(a.equals("/"))) {
                    aResults = searchString(a);
                } else {
                    aResults = sets.pop();
                }
                sets.push(combine(aResults, bResults, queries[i]));
                operands.push("/");
            } else {
                operands.push(queries[i]);
            }
        }
        if (sets.isEmpty()) {
            return searchString(queries[0]);
        }
        return sets.pop();
    }

    private Map<Document, Integer> combine(Map<Document, Integer> a, Map<Document, Integer> b, String operator) {
        Map<Document, Integer> tmp = new HashMap<>();

        //intersection
        if (operator.equals("+")) {
            for (Map.Entry<Document, Integer> entry : a.entrySet()) {
                Document key = entry.getKey();
                if (b.containsKey(key)) {
                    tmp.put(key, a.get(key) + b.get(key));
                }
            }
            return tmp;
        }

        //union
        else if (operator.equals("|")) {
            for (Map.Entry<Document, Integer> entry : a.entrySet()) {
                tmp.put(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<Document, Integer> entry : b.entrySet()) {
                Document key = entry.getKey();
                if (tmp.containsKey(key)) {
                    tmp.put(key, tmp.get(key) + b.get(key));
                } else {
                    tmp.put(key, b.get(key));
                }
            }
            return tmp;
        }

        //difference
        else if (operator.equals("-")) {
            for (Map.Entry<Document, Integer> entry : a.entrySet()) {
                Document key = entry.getKey();
                if (!(b.containsKey(key))) {
                    tmp.put(key, a.get(key));
                }
            }
            return tmp;
        }
        return tmp;
    }

    private void getTf(Map<Document, Integer> occurences, Map<Document, Integer> tf) {
        for (Map.Entry<Document, Integer> entry : occurences.entrySet()) {
            Document key = entry.getKey();
            Integer val = entry.getValue();
            val = val / (wordCounts.get(key));
            tf.put(key, val);
        }
    }

    private void getRelevance(Map<Document, Integer> tf, Map<Document, Integer> relevance, double idf) {
        for (Map.Entry<Document, Integer> entry : tf.entrySet()) {
            Document key = entry.getKey();
            Integer val = entry.getValue();
            val = val * (int) idf;
            relevance.put(key, val);
        }
    }

    private List<Document> orderBy(String by, String direction, Map<Document, Integer> toOrder) {
        List<OrderableSearchResult> resultToOrder = new ArrayList<>();
        List<Document> finalResults = new ArrayList<>();

        //order by popularity
        if (by.equals("popularity")) {
            //build orderable set
            for (Map.Entry<Document, Integer> entry : toOrder.entrySet()) {
                OrderableSearchResult check = new OrderableSearchResult(entry.getKey().popularity, entry.getKey());
                resultToOrder.add(check);
            }
        }

        //order by relevance
        else if (by.equals("relevance")) {
            //build orderable set
            for (Map.Entry<Document, Integer> entry : toOrder.entrySet()) {
                OrderableSearchResult check = new OrderableSearchResult(entry.getValue(), entry.getKey());
                resultToOrder.add(check);
            }
        }

        //order the orderable result set
        if (direction.equals("asc")) {
            BubbleSort.sort(resultToOrder, 1);
        } else if (direction.equals("desc")) {
            BubbleSort.sort(resultToOrder, 2);
        }

        //construct final deliverable result list containing all the documents
        for (OrderableSearchResult orderedResult : resultToOrder) {
            if (!finalResults.contains(orderedResult.document)) {
                finalResults.add(orderedResult.document);
            }
        }

        return finalResults;
    }

    private Map<Document, Integer> searchString(String query) {
        Map<Document, Integer> occurences = new HashMap<>();
        if (indexedWords.containsKey(query)) {
            //return all doc list
            for (Attributes attr : indexedWords.get(query)) {
                Document key = attr.document;
                if (occurences.containsKey(key)) {
                    occurences.put(key, (occurences.get(key) + 1));
                } else {
                    occurences.put(key, 1);
                }
            }
        }
        Map<Document, Integer> tf = new HashMap<>();
        getTf(occurences, tf);
        Map<Document, Integer> relevance = new HashMap<>();
        double idf = log10(500 / occurences.size());
        getRelevance(tf, relevance, idf);
        return relevance;
    }

    public class OrderableSearchResult {
        public int ordering;
        public Document document;

        public OrderableSearchResult(int ordering, Document document) {
            this.ordering = ordering;
            this.document = document;
        }
    }

}
