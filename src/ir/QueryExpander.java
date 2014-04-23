package ir;
import java.io.File;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Created by anton on 4/23/14.
 */
public class QueryExpander {
    private Indexer indexer;
    private Index index;

    public QueryExpander(Indexer indexer) {
        this.indexer = indexer;
        this.index = indexer.index;
    }

    public Query expandQuery(PostingsList results, Query query, int docsToEvaluate) {
        System.out.println(results.get(0).docID);
        List< Map<String, Double>> evaluatedDocuments = new ArrayList< Map<String, Double>>();

        //Process all document and rank their terms
        for(int i = 0; i < docsToEvaluate; i++) {
            int docID = results.get(i).docID;
            Map<String, Double> processedFile = indexer.processFile(new File(index.docIDs.get(docID+"")), docID);
            evaluatedDocuments.add(processedFile);
        }
        List<Term> newTerms = new ArrayList<Term>();

        //Get the term with the highest tf-idf for each document
        for(int i = 0; i < evaluatedDocuments.size(); i++) {
            Term newTerm = getHighestScoringTerm(evaluatedDocuments.get(i));
            newTerms.add(newTerm);
        }

        for(int i = 0; i < newTerms.size(); i++) {
            query.terms.add(newTerms.get(i).term);
            query.weights.add(1.0);

        }
        return query;
    }

    private Term getHighestScoringTerm(Map<String, Double> documentVector) {
        Term highest = null;
        for(String key : documentVector.keySet()) {
            if(highest == null) {
                highest = new Term(key, documentVector.get(key));
                continue;
            }
            if(highest.score < documentVector.get(key)) {
                highest = new Term(key, documentVector.get(key));
            }
        }
        return highest;
    }


    private class Term {
        public double score;
        public String term;
        public Term(String term, double score) {
            this.score = score;
            this.term = term;
        }
    }

}
