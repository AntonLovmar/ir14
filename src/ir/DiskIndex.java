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
public class DiskIndex implements Index {

	/** The index as a hashtable. */
	private HashMap<String, String> index = new HashMap<>();

	public DiskIndex() {
		File dir = new File("postingslists");
		dir.mkdir();
		if(hasSavedIndex()) {
			retrieveDocIDs();
		}
	} 
	/**
	 * Inserts this token in the index.
	 */
	public void insert(String token, int docID, int offset) {
		if (!index.containsKey(token)) {
			PostingsList tokenList = new PostingsList();
			tokenList.add(new PostingsEntry(docID, offset));
			savePostingsList(tokenList, token);
			tokenList = null;
		} else {
			PostingsList tokenList = getPostings(token);
			tokenList.add(new PostingsEntry(docID, offset));
			savePostingsList(tokenList, token);
		}
	}

	public boolean hasSavedIndex() {
		boolean saved = false;
		try {
			BufferedReader br = new BufferedReader(new FileReader("info.index"));
        	StringBuilder sb = new StringBuilder();
       		String line = br.readLine();

        while (line != null) {
            sb.append(line);
            line = br.readLine();
        }
        String everything = sb.toString();
        System.out.println(everything.split(" ")[1]);
        if(everything.split(" ")[1].equals("1"))
        	saved = true;
	    br.close();
	    } catch(IOException e) {
	    } finally {
	    	System.out.println(saved);
	    	return saved;
	    }
	}

	public void savePostingsList(PostingsList list, String token) {
		String filename = "postingslists/" + token + ".ser";
		try (OutputStream file = new FileOutputStream(filename	);
				OutputStream buffer = new BufferedOutputStream(file);
				ObjectOutput output = new ObjectOutputStream(buffer);) {
			output.writeObject(list);
			index.put(token, filename);
			output.close();
			buffer.close();
			file.close();
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
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
		PostingsList list = null;
		String filename = "postingslists/" + token + ".ser";
		try (InputStream file = new FileInputStream(filename);
				InputStream buffer = new BufferedInputStream(file);
				ObjectInput input = new ObjectInputStream(buffer);) {

			list = (PostingsList) input.readObject();
			input.close();
			buffer.close();
			file.close();
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		} catch (ClassNotFoundException e) {	
			System.out.println(e.getMessage());
		}
		return list;
	}

	/**
	 * Searches the index for postings matching the query.
	 */
	public PostingsList search(Query query, int queryType, int rankingType, int structureType) {
		List<PostingsList> parameters = new ArrayList<PostingsList>();
		PostingsList answer = null;
		long before = System.currentTimeMillis();
		if (queryType == Index.INTERSECTION_QUERY) {
			for (String term : query.terms) {
				parameters.add(getPostings(term));
			}
			answer = intersect(parameters);
		} else if (queryType == Index.PHRASE_QUERY) {
			for (String term : query.terms) {
				parameters.add(getPostings(term));
			}
			answer = positionalIntersect(parameters);
		}
		System.out.println("Query runtime: " + (System.currentTimeMillis()-before));
		return answer;
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

	public void cleanup() {
		Writer writer = null;
		try {
		    writer = new BufferedWriter(new OutputStreamWriter(
		          new FileOutputStream("info.index"), "utf-8"));
		    writer.write("savedIndex: 0");
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		} finally {
		   try {writer.close();} catch (Exception ex) {}
		}
		File dir = new File("postingslists");
		for(File f : dir.listFiles()) {
			f.delete();
		}
	}

	public void saveIndex() {

		Writer writer = null;
		try {
		    writer = new BufferedWriter(new OutputStreamWriter(
		          new FileOutputStream("info.index"), "utf-8"));
		    writer.write("savedIndex: 1");
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		} finally {
		   try {writer.close();} catch (Exception ex) {}
		}
		saveDocIDs();
	}

	public void saveDocIDs() {
		String filename = "docIDs.ser";
		try (OutputStream file = new FileOutputStream(filename);
				OutputStream buffer = new BufferedOutputStream(file);
				ObjectOutput output = new ObjectOutputStream(buffer);) {
			output.writeObject(this.docIDs);
			output.close();
			buffer.close();
			file.close();
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		}
	}

	public void retrieveDocIDs() {
		String filename = "docIDs.ser";
		try (InputStream file = new FileInputStream(filename);
				InputStream buffer = new BufferedInputStream(file);
				ObjectInput input = new ObjectInputStream(buffer);) {
			HashMap<String, String> tmpDocIDs =(HashMap<String, String>)input.readObject();
			docIDs.clear();
			docIDs.putAll(tmpDocIDs);
			input.close();
			buffer.close();
			file.close();
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		}catch (ClassNotFoundException e) {	
			System.out.println(e.getMessage());
		}
	}
}
