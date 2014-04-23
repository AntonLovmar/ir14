/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 *   Additions: Hedvig Kjellström, 2012-14
 */

package ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.io.*;

/**
 * Implements an inverted index as a Hashtable from words to PostingsLists.
 */
public class HashedIndex implements Index {

	/** The index as a hashtable. */
	private HashMap<String, PostingsList> index = new HashMap<>();
	private int numDocs = 0;
	private HashMap<Integer, Integer> occurences = new HashMap<>();

	/**
	 * Inserts this token in the index.
	 */
	public void insert(String token, int docID, int offset) {
		numDocs = docID;
		if(occurences.get(docID) == null) {
			occurences.put(docID, 1);
		} else {
			occurences.put(docID, occurences.get(docID)+1);
		}
		if (!index.containsKey(token)) {
			PostingsList tokenList = new PostingsList();
			tokenList.add(new PostingsEntry(docID, offset));
			index.put(token, tokenList);
		} else {
			PostingsList tokenList = getPostings(token);
			tokenList.add(new PostingsEntry(docID, offset));
		}
	}

	/**
	 * Returns all the words in the index.
	 */
	public Iterator<String> getDictionary() {
		Iterator<String> dictionary = index.keySet().iterator();
		return dictionary;
	}

	/**
	 * Returns the postings for a specific term, or null if the term is not in
	 * the index.
	 */
	public PostingsList getPostings(String token) {
		return index.get(token);
	}

	/**
	 * Searches the index for postings matching the query.
	 */
	public PostingsList search(Query query, int queryType, int rankingType, int structureType) {
		
		List<PostingsList> parameters = new ArrayList<PostingsList>();
		for (String term : query.terms) {
				parameters.add(index.get(term));
		}

		if (queryType == Index.INTERSECTION_QUERY) {
			return intersect(parameters);
		} else if (queryType == Index.PHRASE_QUERY) {
			return positionalIntersect(parameters);
		} else if (queryType == Index.RANKED_QUERY) {
			return cosineSimilarity(query);
		}
		return null;
		
	}
	
	public PostingsList cosineSimilarity(Query query) {
		double[] scores = new double[numDocs+1];

		for(int j = 0; j < query.terms.size(); j++) {
            PostingsList postings = index.get(query.terms.get(j));
            double wtq = query.weights.get(j);
			for(int i = 0; i < postings.size(); i++) {
				PostingsEntry doc = postings.get(i);
                if(doc == null)
                    continue;
				double idf = Math.log(numDocs/postings.getDocumentFrequency());
				double tfidf = doc.getTermFrequency()*idf;
				scores[doc.docID] += tfidf*wtq;
			}
		}
		for(int i = 0; i < numDocs; i++) {
			scores[i] = scores[i]/occurences.get(i);
		}
        List<PostingsList> terms = new ArrayList<PostingsList>();
        for (String term : query.terms) {
            terms.add(index.get(term));
        }
		PostingsList result = union(terms);
		for(int i = 0; i  < result.size(); i++) {
			PostingsEntry entry = result.get(i);
			entry.score = scores[entry.docID];
		}
		result.sort();
		return result;
	}

	public PostingsList union(List<PostingsList> terms) {
		PostingsList result = new PostingsList();
		result.addAll(terms.get(0));
		terms.remove(0);
		while(!terms.isEmpty()) {
			PostingsList current = terms.get(0);
			for(int i = 0; i < current.size(); i++) {
				if(!result.contains(current.get(i))) 
					result.add(current.get(i));
			}
			terms.remove(0);
		} 
		return result;
	}

	public PostingsList positionalIntersect(PostingsList list1, PostingsList list2, int k) {
		PostingsList answer = new PostingsList();
		List<Integer> matches;
		int p1 = 0;
		int p2 = 0;
		while ((p1 != list1.size()) && (p2 != list2.size())) {
			if (list1.get(p1).docID == list2.get(p2).docID) {
				List<Integer> plist1 = list1.get(p1).getOffsets();
				List<Integer> plist2 = list2.get(p2).getOffsets();

				for(Integer p1Offset : plist1) {
					matches = new LinkedList<Integer>();
					for(Integer p2Offset : plist2) {
						if ((p2Offset - p1Offset) <= k && (p2Offset - p1Offset) > 0) {
							matches.add(p2Offset);
						} if (p2Offset > p1Offset) {
							break;
						}
					}
					
					for (Integer ps : matches) {
						List<Integer> offsets = new LinkedList<>();
						//offsets.add(p1Offset);
						offsets.add(ps);
						answer.add(new PostingsEntry(list1.get(p1).docID, offsets));
					}
				}
				p1++;
				p2++;
			} else if (list1.get(p1).docID < list2.get(p2).docID) {
				p1++;
			} else {
				p2++;
			}
		}
		return answer;
	}

	public PostingsList positionalIntersect(List<PostingsList> terms) {
		if (terms == null) {
			return null;
		}
		PostingsList result = terms.get(0);
		terms.remove(0);
		while (!terms.isEmpty() && result != null) {
			result = positionalIntersect(result, terms.get(0), 1);
			terms.remove(0);
		}
		return result;
	}

	public PostingsList intersect(List<PostingsList> terms) {
		if (terms == null) {
			return null;
		}
		terms = sortByFrequency(terms);
		PostingsList result = terms.get(0);
		terms.remove(0);
		while (!terms.isEmpty() && result != null) {
			result = intersect(result, terms.get(0));
			terms.remove(0);
		}
		return result;
	}

	public List<PostingsList> sortByFrequency(List<PostingsList> terms) {
		Collections.sort(terms);
		return terms;
	}

	public PostingsList intersect(PostingsList list1, PostingsList list2) {
		PostingsList answer = new PostingsList();
		int p1 = 0;
		int p2 = 0;
		while ((p1 != list1.size()) && (p2 != list2.size())) {
			if (list1.get(p1).docID == list2.get(p2).docID) {
				answer.add(list1.get(p1));
				p1++;
				p2++;
			} else if (list1.get(p1).docID < list2.get(p2).docID) {
				p1++;
			} else {
				p2++;
			}
		}
		return answer;
	}

	public boolean hasSavedIndex() {
		return false;
	}
	/**
	 * No need for cleanup in a HashedIndex.
	 */
	public void cleanup() {
	}

	public void saveIndex() {
	}
}
