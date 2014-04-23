/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 */

package ir;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collections;

/**
 * A list of postings for a given word.
 */
public class PostingsList implements Serializable, Comparable<PostingsList> {

	/** The postings list as a linked list. */
    private List<PostingsEntry> list = new ArrayList<PostingsEntry>();
    private Map<Integer, Integer> docIDToIndex = new HashMap<Integer, Integer>();

    public PostingsList() {
    }

	/** Number of postings in this list */
	public int size() {
		return list.size();
	}

	/** Returns the ith posting */
	public PostingsEntry get(int i) {
		return list.get(i);
	}

	/** Returns the posting with the docID */
	public PostingsEntry getEntry(int docID) {
		if(docIDToIndex.get(docID) != null)
			return list.get(docIDToIndex.get(docID));
		else
			return null;
	}

	/** Add a new posting to the list **/
	public void add(PostingsEntry entry) {
		if(docIDToIndex.get(entry.docID) == null) {
			list.add(entry);
			docIDToIndex.put(entry.docID, list.size()-1);
		} else {
			getEntry(entry.docID).addNewOffsets(entry.getOffsets());
		}
	}

	public int getDocumentFrequency() {
		return size();
	}

	/** Adds all the elements in the argument to this list **/
	public void addAll(PostingsList list) {
		if (list == null) {
			return;
		}
		for (int i = 0; i < list.size(); i++) {
			add(list.get(i));
			docIDToIndex.put(list.get(i).docID, list.size()-1);
		}
	}

	@Override
	public int compareTo(PostingsList arg) {
		if (arg.size() < this.size()) {
			return 1;
		} else if (arg.size() > this.size()) {
			return -1;
		}
		return 0;
	}

	public boolean contains(PostingsEntry entry) {
		return docIDToIndex.containsKey(entry.docID);
	}

	public void sort() {
		Collections.sort(list);
	}

}
