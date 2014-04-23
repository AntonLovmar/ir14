/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 */

package ir;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {

	public int docID;
	public double score;
	private List<Integer> offsets;
	private int termFrequency;

	/**
	 * Create a new PostingsEntry with the specified docID
	 * 
	 * @param docID
	 */
	public PostingsEntry(int docID, int offset) {
		this.docID = docID;
		offsets = new ArrayList<Integer>();
		offsets.add(offset);
		termFrequency = 1;
	}

	public PostingsEntry(int docID, List<Integer> offsets) {
		this.docID = docID;
		this.offsets = offsets;
		termFrequency = offsets.size();
	}

	/**
	 * Adds a new offset to the offset list of the entry
	 * 
	 * @param offset
	 *            new offset
	 */
	public void addNewOffsets(List<Integer> offsets) {
		this.offsets.addAll(offsets);
		termFrequency += offsets.size();
	}

	public void addOffset(int offset) {
		termFrequency++;
		offsets.add(offset);
	}

	public int getTermFrequency() {
		return termFrequency;
	}

	/**
	 * Returns the list of offsets
	 * 
	 * @return offsets
	 */
	public List<Integer> getOffsets() {
		return offsets;
	}

	/**
	 * PostingsEntries are compared by their score (only relevant in ranked
	 * retrieval).
	 * 
	 * The comparison is defined so that entries will be put in descending
	 * order.
	 */
	public int compareTo(PostingsEntry other) {
		return Double.compare(other.score, score);
	}

	@Override
	public boolean equals(Object arg) {
		if (!(arg instanceof PostingsEntry))
			return false;
		PostingsEntry other = (PostingsEntry) arg;
		if (other.docID == this.docID) {
			return true;
		}
		return false;
	}

}
