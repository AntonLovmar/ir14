/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version: Johan Boye, 2012
 *   Additions: Hedvig Kjellström, 2012-14
 */

package ir;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.LinkedList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;

/**
 * A graphical interface to the information retrieval system.
 */
public class SearchGUI extends JFrame {

	/** The indexer creating the search index. */
	Indexer indexer;

	/**
	 * The query posed by the user, used in search() and
	 * relevanceFeedbackSearch()
	 */
	private Query query;

	/** The returned documents, used in search() and relevanceFeedbackSearch() */
	private PostingsList results;

	/** Directories that should be indexed. */
	LinkedList<String> dirNames = new LinkedList<String>();

	/** The query type (either intersection, phrase, or ranked). */
	int queryType = Index.INTERSECTION_QUERY;

	/** The index type (either entirely in memory or partly on disk). */
	int indexType = Index.DISK_INDEX;

	/** The ranking type (either tf-idf, pagerank, or combination). */
	int rankingType = Index.TF_IDF;

	/** The word structure type (either unigram, bigram, or subphrase). */
	int structureType = Index.UNIGRAM;

	/** Lock to prevent simultaneous access to the index. */
	Object indexLock = new Object();

	/** Directory from which the code is compiled and run. */
	public static final String homeDir = "~/workspace/ir14";

	/*
	 * The nice logotype Generated at
	 * http://neswork.com/logo-generator/google-font
	 */
	static final String LOGOPIC = homeDir + "/pics/IRfourteen.jpg";
	static final String BLANKPIC = homeDir + "/pics/blank.jpg";

	/*
	 * Common GUI resources
	 */
	public JTextField queryWindow = new JTextField("", 28);
	public JTextArea resultWindow = new JTextArea("", 23, 28);
	private JScrollPane resultPane = new JScrollPane(resultWindow);
	private Font queryFont = new Font("Arial", Font.BOLD, 24);
	private Font resultFont = new Font("Arial", Font.BOLD, 16);
	JMenuBar menuBar = new JMenuBar();
	JMenu fileMenu = new JMenu("File");
	JMenu optionsMenu = new JMenu("Search options");
	JMenu rankingMenu = new JMenu("Ranking score");
	JMenu structureMenu = new JMenu("Text structure");
	JMenuItem saveItem = new JMenuItem("Save index and exit");
	JMenuItem quitItem = new JMenuItem("Quit");
	JRadioButtonMenuItem intersectionItem = new JRadioButtonMenuItem("Intersection query");
	JRadioButtonMenuItem phraseItem = new JRadioButtonMenuItem("Phrase query");
	JRadioButtonMenuItem rankedItem = new JRadioButtonMenuItem("Ranked retrieval");
	JRadioButtonMenuItem tfidfItem = new JRadioButtonMenuItem("tf-idf");
	JRadioButtonMenuItem unigramItem = new JRadioButtonMenuItem("Unigram");
	ButtonGroup queries = new ButtonGroup();
	ButtonGroup ranking = new ButtonGroup();
	ButtonGroup structure = new ButtonGroup();

	/* ----------------------------------------------- */

	/*
	 * Create the GUI.
	 */
	private void createGUI(String[] args) {

		Index index;
		if(args[0].equals("-disk")) {
			index = new DiskIndex();
		} else {
			index = new HashedIndex();
		}
		indexer = new Indexer(index);

		// GUI definition
		setSize(600, 650);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		getContentPane().add(p, BorderLayout.CENTER);
		// Top menu
		menuBar.add(fileMenu);
		menuBar.add(optionsMenu);
		menuBar.add(rankingMenu);
		menuBar.add(structureMenu);
		fileMenu.add(saveItem);
		fileMenu.add(quitItem);
		optionsMenu.add(intersectionItem);
		optionsMenu.add(phraseItem);
		optionsMenu.add(rankedItem);
		rankingMenu.add(tfidfItem);
		structureMenu.add(unigramItem);
		queries.add(intersectionItem);
		queries.add(phraseItem);
		queries.add(rankedItem);
		ranking.add(tfidfItem);
		structure.add(unigramItem);
		intersectionItem.setSelected(true);
		tfidfItem.setSelected(true);
		unigramItem.setSelected(true);
		p.add(menuBar);
		// Logo
		JPanel p1 = new JPanel();
		p1.setLayout(new BoxLayout(p1, BoxLayout.X_AXIS));
		p1.add(new JLabel(new ImageIcon(LOGOPIC)));
		p.add(p1);
		JPanel p3 = new JPanel();
		// Search box
		p3.setLayout(new BoxLayout(p3, BoxLayout.X_AXIS));
		p3.add(new JLabel(new ImageIcon(BLANKPIC)));
		p3.add(queryWindow);
		queryWindow.setFont(queryFont);
		p3.add(new JLabel(new ImageIcon(BLANKPIC)));
		p.add(p3);
		// Display area for search results
		p.add(resultPane);
		resultWindow.setFont(resultFont);
		// Show the interface
		setVisible(true);

		Action search = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				// Normalize the search string and turn it into a Query
				String queryString = SimpleTokenizer.normalize(queryWindow.getText());
				query = new Query(queryString);
				// Search and print results. Access to the index is synchronized
				// since
				// we don't want to search at the same time we're indexing new
				// files
				// (this might corrupt the index).
				synchronized (indexLock) {
					results = indexer.index.search(query, queryType, rankingType, structureType);
				}
				StringBuffer buf = new StringBuffer();
				if (results != null) {
					buf.append("\nFound " + results.size() + " matching document(s)\n\n");
					for (int i = 0; i < results.size(); i++) {
						buf.append(" " + i + ". ");
						String filename = indexer.index.docIDs.get("" + results.get(i).docID);
						if (filename == null) {
							buf.append("" + results.get(i).docID);
						} else {
							buf.append(filename);
						}
						if (queryType == Index.RANKED_QUERY) {
							buf.append("   " + String.format("%.5f", results.get(i).score));
						}
						buf.append("\n");
					}
				} else {
					buf.append("\nFound 0 matching document(s)\n\n");
				}
				resultWindow.setText(buf.toString());
				resultWindow.setCaretPosition(0);
			}
		};
		queryWindow.registerKeyboardAction(search, "", KeyStroke.getKeyStroke("ENTER"), JComponent.WHEN_FOCUSED);


		Action saveAndQuit = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				resultWindow.setText("\n  Saving index...");
				indexer.index.saveIndex();
				System.exit(0);
			}
		};
		saveItem.addActionListener(saveAndQuit);

		Action quit = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				indexer.index.cleanup();
				System.exit(0);
			}
		};
		quitItem.addActionListener(quit);

		Action setIntersectionQuery = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				queryType = Index.INTERSECTION_QUERY;
			}
		};
		intersectionItem.addActionListener(setIntersectionQuery);

		Action setPhraseQuery = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				queryType = Index.PHRASE_QUERY;
			}
		};
		phraseItem.addActionListener(setPhraseQuery);

		Action setRankedQuery = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				queryType = Index.RANKED_QUERY;
			}
		};
		rankedItem.addActionListener(setRankedQuery);

		Action setTfidfRanking = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				rankingType = Index.TF_IDF;
			}
		};
		tfidfItem.addActionListener(setTfidfRanking);

		Action setUnigramStructure = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				structureType = Index.UNIGRAM;
			}
		};
		unigramItem.addActionListener(setUnigramStructure);
	}

	/* ----------------------------------------------- */

	/**
	 * Calls the indexer to index the chosen directory structure. Access to the
	 * index is synchronized since we don't want to search at the same time
	 * we're indexing new files (this might corrupt the index).
	 */
	private void index() {
		if(!indexer.index.hasSavedIndex()) {
			synchronized (indexLock) {
				resultWindow.setText("\n  Indexing, please wait...");
				for (int i = 0; i < dirNames.size(); i++) {
					File dokDir = new File(dirNames.get(i));
					indexer.processFiles(dokDir);
				}
				resultWindow.setText("\n  Done!");
			}
		} else {
			resultWindow.setText("\n  Loaded saved index!");
		}
	};

	/* ----------------------------------------------- */

	/**
	 * Decodes the command line arguments.
	 */
	private void decodeArgs(String[] args) {
		int i = 0, j = 0;
		while (i < args.length) {
			if ("-d".equals(args[i])) {
				i++;
				if (i < args.length) {
					dirNames.add(args[i++]);
				}
			} else {
				System.err.println("Unknown option: " + args[i]);
				break;
			}
		}
	}

	/* ----------------------------------------------- */

	public static void main(String[] args) {
		SearchGUI s = new SearchGUI();
		s.createGUI(args);
		s.decodeArgs(args);
		s.index();
	}

}
