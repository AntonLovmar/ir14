/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2012
 */  
package ir;

import java.util.*;
import java.io.*;

public class PageRank{

    /**  
     *   Maximal number of documents. We're assuming here that we
     *   don't have more docs than we can keep in main memory.
     */
    final static int MAX_NUMBER_OF_DOCS = 2000000;

    /**
     *   Mapping from document names to document numbers.
     */
    Hashtable<String,Integer> docNumber = new Hashtable<String,Integer>();
    Random random = new Random();


    /**
     *   Mapping from document numbers to document names
     */
    String[] docName = new String[MAX_NUMBER_OF_DOCS];

    /**  
     *   A memory-efficient representation of the transition matrix.
     *   The outlinks are represented as a Hashtable, whose keys are 
     *   the numbers of the documents linked from.<p>
     *
     *   The value corresponding to key i is a Hashtable whose keys are 
     *   all the numbers of documents j that i links to.<p>
     *
     *   If there are no outlinks from i, then the value corresponding 
     *   key i is null.
     */
    Map<Integer,Map<Integer,Boolean>> link = new HashMap<Integer, Map<Integer, Boolean>>();

    /**
     *   The number of outlinks from each node.
     */
    int[] out = new int[MAX_NUMBER_OF_DOCS];

    /**
     *   The number of documents with no outlinks.
     */
    int numberOfSinks = 0;

    private double diff;

    /**
     *   The probability that the surfer will be bored, stop
     *   following links, and take a random jump somewhere.
     */
    final static double BORED = 0.15;

    /**
     *   Convergence criterion: Transition probabilities do not 
     *   change more that EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.0001;

    /**
     *   Never do more than this number of iterations regardless
     *   of whether the transistion probabilities converge or not.
     */
    final static int MAX_NUMBER_OF_ITERATIONS = 1000;

    
    /* --------------------------------------------- */


    public PageRank( String filename , String approx) {
	int noOfDocs = readDocs( filename );
        if(approx.equals("apx")) {
            computePageRankApproximation(noOfDocs);
        } else if (approx.equals("mc")) {
            computeMonteCarlo3(noOfDocs);
        } else {
            computePageRank(noOfDocs);
        }
    }


    /* --------------------------------------------- */


    /**
     *   Reads the documents and creates the docs table. When this method 
     *   finishes executing then the @code{out} vector of outlinks is 
     *   initialised for each doc, and the @code{p} matrix is filled with
     *   zeroes (that indicate direct links) and NO_LINK (if there is no
     *   direct link. <p>
     *
     *   @return the number of documents read.
     */
    int readDocs( String filename ) {
	int fileIndex = 0;
	try {
	    System.err.print( "Reading file... " );
	    BufferedReader in = new BufferedReader( new FileReader( filename ));
	    String line;
	    while ((line = in.readLine()) != null && fileIndex<MAX_NUMBER_OF_DOCS ) {
		int index = line.indexOf( ";" );
		String title = line.substring( 0, index );
		Integer fromdoc = docNumber.get( title );
		//  Have we seen this document before?
		if ( fromdoc == null ) {	
		    // This is a previously unseen doc, so add it to the table.
		    fromdoc = fileIndex++;
		    docNumber.put( title, fromdoc );
		    docName[fromdoc] = title;
		}
		// Check all outlinks.
		StringTokenizer tok = new StringTokenizer( line.substring(index+1), "," );
		while ( tok.hasMoreTokens() && fileIndex<MAX_NUMBER_OF_DOCS ) {
		    String otherTitle = tok.nextToken();
		    Integer otherDoc = docNumber.get( otherTitle );
		    if ( otherDoc == null ) {
			// This is a previousy unseen doc, so add it to the table.
			otherDoc = fileIndex++;
			docNumber.put( otherTitle, otherDoc );
			docName[otherDoc] = otherTitle;
		    }
		    // Set the probability to 0 for now, to indicate that there is
		    // a link from fromdoc to otherDoc.
		    if ( link.get(fromdoc) == null ) {
			link.put(fromdoc, new Hashtable<Integer,Boolean>());
		    }
		    if ( link.get(fromdoc).get(otherDoc) == null ) {
			link.get(fromdoc).put( otherDoc, true );
			out[fromdoc]++;
		    }
		}
	    }
	    if ( fileIndex >= MAX_NUMBER_OF_DOCS ) {
		System.err.print( "stopped reading since documents table is full. " );
	    }
	    else {
		System.err.print( "done. " );
	    }
	    // Compute the number of sinks.
	    for ( int i=0; i<fileIndex; i++ ) {
		if ( out[i] == 0 )
		    numberOfSinks++;
	    }
	}
	catch ( FileNotFoundException e ) {
	    System.err.println( "File " + filename + " not found!" );
	}
	catch ( IOException e ) {
	    System.err.println( "Error reading file " + filename );
	}
	System.err.println( "Read " + fileIndex + " number of documents" );
	return fileIndex;
    }


    /* --------------------------------------------- */


    /*
     *   Computes the pagerank of each document.
     */
    void computePageRank( int numberOfDocs ) {
        double[][] probabilityMatrix = new double[numberOfDocs][numberOfDocs];
        createProbabilityMatrix(probabilityMatrix);
        double[] init = new double[numberOfDocs];
        for(int i = 0; i < numberOfDocs; i++) {
            init[i] = (1.0/numberOfDocs);
        }

        double[] res = multiplyMatrix(init, probabilityMatrix);
        diff=Double.MAX_VALUE;
        for(int i = 1; i < MAX_NUMBER_OF_ITERATIONS && diff > EPSILON; i++) {
            res = multiplyMatrix(res, probabilityMatrix);
        }
        printScores(res);

    }

    void computePageRankApproximation(int numberOfDocs) {
        double[] scores = new double[numberOfDocs];
        double[] init = new double[numberOfDocs];
        for(int i = 0; i < numberOfDocs; i++) {
            init[i] = (1.0/numberOfDocs);
        }
        double c = (1-BORED);
        diff=Double.MAX_VALUE;

        for(int k = 1; k < MAX_NUMBER_OF_ITERATIONS && diff > EPSILON; k++) {
            diff = 0;
            for(int i = 0; i < numberOfDocs; i++) {
                if(link.get(i) != null) {
                    for(Integer j : link.get(i).keySet()) {
                        scores[j] += init[i]*c/out[i];
                    }
                }
                scores[i] += (1-c)/numberOfDocs;
                scores[i] += numberOfSinks/numberOfDocs/numberOfDocs;
            }
            for(int j = 0; j < numberOfDocs; j++) {
                diff += Math.abs(scores[j]-init[j]);
            }
            init = scores;
            scores = new double[numberOfDocs];
        }
        printScores(init);
    }

    private void printScores(double[] scores) {
        List<ScoreItem> scoresList = new ArrayList<>();
        for(int i = 0; i < scores.length; i++) {
            scoresList.add(new ScoreItem(i, scores[i]));
        }
        Collections.sort(scoresList);
        for(int i = 0; i < 50; i++) {
            System.out.println(i + ". " + docName[scoresList.get(i).getDocID()] + " " + scoresList.get(i).getScore());
        }
    }

    private void createProbabilityMatrix(double[][] probabilityMatrix) {
        int numberOfDocs = probabilityMatrix.length;
        for(int i = 0; i < numberOfDocs; i++) {
            if(link.get(i) == null) {
                for(int j = 0; j < numberOfDocs; j++) {
                    probabilityMatrix[i][j] = (BORED/numberOfDocs)+(1-BORED)*(1.0/numberOfDocs);
                }
            } else {
                for(int j = 0; j < numberOfDocs; j++) {
                    if(link.get(i).get(j) != null && link.get(i).get(j)) {
                        probabilityMatrix[i][j] = (BORED/numberOfDocs)+(1-BORED)*(1.0/link.get(i).size());
                    } else {
                        probabilityMatrix[i][j] = (BORED/numberOfDocs);
                    }
                }
            }
        }
    }

    public double[] multiplyMatrix(double[] initial, double[][] probabilityMatrix) {
        double[] result = new double[initial.length];
        diff = 0;
        for(int i = 0; i < initial.length; i++) {
            double sum = 0;
            for(int j = 0; j < initial.length; j++) {
                sum += initial[j]*probabilityMatrix[j][i];
            }
            diff += Math.abs(initial[i]-sum);
            result[i] = sum;
        }
        return result;
    }
    /* --------------------------------------------- */

    private void computeMonteCarlo(int numberOfDocs) {
        double[] numEnds = new double[numberOfDocs];
        int numberOfWalks = numberOfDocs*numberOfDocs;
        int numberOfSteps = 10;
        for(int i = 0; i < numberOfWalks; i++) {
            int current = random.nextInt(numberOfDocs);

            for(int j = 0; j < numberOfSteps; j++) {
                if(Math.random() < BORED || link.get(current) == null) {
                    current = random.nextInt(numberOfDocs); //do a jump
                } else {
                    current = getRandomLink(link.get(current).keySet()); //go to a random page
                }
            }
            numEnds[current] = numEnds[current] + 1;
        }
        double[] scores = new double[numberOfDocs];
        for(int i = 0; i < numberOfDocs; i++) {
            scores[i] = numEnds[i]/numberOfWalks;
        }
        printScores(scores);
    }

    private void computeMonteCarlo2(int numberOfDocs) {
        double[] numEnds = new double[numberOfDocs];
        int m = 1000;
        int n = numberOfDocs;
        int numberOfSteps = 10;
        for(int k = 0; k < m; k++) {
            for(int i = 0; i < n; i++) {
                int current = i;

                for(int j = 0; j < numberOfSteps; j++) {
                    if(Math.random() < BORED || link.get(current) == null) {
                        current = random.nextInt(numberOfDocs); //do a jump
                    } else {
                        current = getRandomLink(link.get(current).keySet()); //go to a random page
                    }
                }
                numEnds[current] = numEnds[current] + 1;
            }
        }
        double[] scores = new double[numberOfDocs];
        for(int i = 0; i < numberOfDocs; i++) {
            scores[i] = numEnds[i]/(m*n);
        }
        printScores(scores);
    }

    private void computeMonteCarlo3(int numberOfDocs) {
        double[] numVisits = new double[numberOfDocs];
        int m = 1000;
        int n = numberOfDocs;
        int numberOfSteps = 10;
        for(int k = 0; k < m; k++) {
            for(int i = 0; i < n; i++) {
                int current = i;
                if(link.get(i) == null)
                    continue;
                for(int j = 0; j < numberOfSteps; j++) {
                    numVisits[current]++;
                    if(Math.random() < BORED || link.get(current) == null) {
                        current = random.nextInt(numberOfDocs); //do a jump
                    } else {
                        current = getRandomLink(link.get(current).keySet()); //go to a random page
                    }
                }
            }
        }
        double[] scores = new double[numberOfDocs];
        for(int i = 0; i < numberOfDocs; i++) {
            scores[i] = numVisits[i]/(m*n*numberOfSteps);
        }
        printScores(scores);
    }

    private int getRandomLink(Set<Integer> links) {
        int index = random.nextInt(links.size());
        int current = 0;
        for(Integer link : links) {
            if(current == index)
                return link;
            else
               current++;
        }
        return 0;
    }

    public static void main( String[] args ) {
	if ( args.length < 1) {
	    System.err.println( "Please give the name of the link file and approximation parameter" );
	} else if( args.length != 2) {
        new PageRank( args[0], "no");
    }
	else {
	    new PageRank( args[0], args[1]);
	}
    }
}
