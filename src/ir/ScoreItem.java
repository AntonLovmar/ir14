package ir;

public class ScoreItem implements Comparable<ScoreItem> {

	private int docID;
	private double score;

	public ScoreItem(int docID, double score) {
		this.docID = docID;
		this.score = score;
	}

	public int getDocID() {
		return docID;
	}

	public double getScore() {
		return score;
	}

	@Override
	public int compareTo(ScoreItem other) {
		return Double.compare(other.score, score);
	}

	@Override
	public String toString() {
		return "Id: " + docID + " score: " + score;
	}
}