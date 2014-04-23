package ir;
import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Created by anton on 4/23/14.
 */
public class QueryExpander {
    private Indexer indexer;
    private HashedIndex index;
    private Map<String, Double> candidateList;
    private final int minimumWordLength = 2;

    public QueryExpander(Indexer indexer) {
        this.indexer = indexer;
        this.index = (HashedIndex) indexer.index;
        candidateList = new HashMap<>();
    }

    public Query expandQuery(PostingsList results, Query query, int docsToEvaluate) {
        System.out.println(results.get(0).docID);
        List< Map<String, Double>> evaluatedDocuments = new ArrayList< Map<String, Double>>();
        List<Integer> docIds = new ArrayList<Integer>();
        //Process all document and rank their terms
        for(int i = 0; i < docsToEvaluate; i++) {
            int docID = results.get(i).docID;
            docIds.add(docID);
            Map<String, Double> processedFile = indexer.processFile(new File(index.docIDs.get(docID+"")), docID);
            evaluatedDocuments.add(processedFile);
        }

        //Build a list of terms that are candidates for synonyms
        buildCandidateList(evaluatedDocuments, query, docIds);

        List<String> newTerms = selectCandidates();

        for(int i = 0; i < newTerms.size(); i++) {
            System.out.println(newTerms.get(i));
            query.terms.add(newTerms.get(i));
            query.weights.add(1.0/newTerms.size());

        }
        return query;
    }

    private void buildCandidateList(List<Map<String, Double>> documentVector, Query query, List<Integer> docIds) {
        for(int i = 0; i < documentVector.size(); i++) {
            for(String term : documentVector.get(i).keySet()) {
                int docID = docIds.get(i);
                for(int j = 0; j < query.terms.size(); j++ ) {
                    if(query.terms.get(j).length() < 4)
                        continue;
                    PostingsEntry termDoc = index.getPostings(term).getEntry(docID);
                    PostingsEntry queryDoc = index.getPostings(query.terms.get(j)).getEntry(docID);
                    if(termDoc == null || queryDoc == null)
                        continue;
                    if(termDoc.getOffsets().size() >= queryDoc.getOffsets().size() && term.length() >= minimumWordLength && !term.equals(query.terms.get(j))) {
                        double idft = Math.log(Index.docIDs.size() / index.getPostings(term).size());

                        if(candidateList.get(term) == null) {
                            candidateList.put(term, idft);
                        } else {
                            candidateList.put(term, candidateList.get(term)+  idft);
                        }
                    }
                }
            }
        }

    }
    public List<String> selectCandidates() {
       List<String> selectedCandidates = new ArrayList<>();
       for(int i = 0; i < 5; i++) {
           String highest = "";
           double score = 0;
           if(candidateList.isEmpty())
               break;
           for(String term : candidateList.keySet()) {
                if(candidateList.get(term) > score) {
                    highest = term;
                    score = candidateList.get(term);
                }
           }
           selectedCandidates.add(highest);
           candidateList.remove(highest);
       }
        return selectedCandidates;
    }


    private class Term {
        public int score;
        public String term;
        public Term(String term, int score) {
            this.score = score;
            this.term = term;
        }
    }

}
