/*
 * NetViewer
 */

import java.util.Vector;      import java.io.DataInputStream;
import java.io.EOFException;  import java.io.BufferedInputStream;
import java.io.FileReader;    import java.io.FileNotFoundException;
import java.io.IOException;   import java.io.BufferedReader;
import java.io.File;					import java.net.URLConnection;
import java.net.URL;          import java.net.MalformedURLException;
import java.awt.Frame;   			import java.awt.BorderLayout;
import java.awt.FlowLayout;   import java.awt.event.WindowAdapter;
import java.awt.GridLayout;   import java.awt.event.WindowEvent;
import java.awt.Dimension;    import java.awt.event.KeyListener;
import java.awt.Component;    import java.awt.event.KeyEvent;
import java.awt.Color;        import java.awt.event.ItemListener;
import java.awt.Insets;				import java.awt.event.ItemEvent;
import javax.swing.JApplet;   import java.awt.event.ActionListener;
import javax.swing.JToolBar;  import java.awt.event.ActionEvent;
import javax.swing.JPanel;    import java.awt.event.ComponentListener;
import javax.swing.JButton;   import java.awt.event.ComponentEvent;
import javax.swing.JLabel;    import javax.swing.event.ChangeListener;
import javax.swing.Box;       import javax.swing.event.ChangeEvent;
import javax.swing.Timer;     import javax.swing.JTabbedPane;
import javax.swing.JSlider;   import javax.swing.JEditorPane;
import javax.swing.JTextArea; import javax.swing.JOptionPane;
import javax.swing.ImageIcon; import javax.swing.JScrollPane;
import javax.swing.JComboBox; import javax.swing.JTextField;
import javax.swing.JSplitPane;import javax.swing.JMenuBar;
import javax.swing.JCheckBox; import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;   import javax.swing.JMenuItem;
import javax.swing.JMenu;


public class NetViewer extends JApplet implements ActionListener {

	// Network Control
	private static NetworkManager networkManager; // controls network creation and algorithm execution

	// Timer
	private static Timer timer;
	private static long timeStampBegin, executionTime;
	private static boolean paused, aborted, doneInSlider;

	// GUI
	private static NetworkPanel networkPanel; // where drawing occurs
	private static JToolBar toolBar;
	private static JMenuBar menuBar;
	private static JLabel statusLabel, runningTimeLabel, messagesWithNLabel, messagesNoNLabel, clockTicksLabel;
	private static JButton playPauseButton, newNetworkButton, stopButton, clearButton;
	private static ImageIcon playImage, pauseImage, stopImage;
	private static JSlider speedSlider; // to adjust the speed of the links/messages
	private static JCheckBox fifo, synchronous, instantWakeUp, autoBarb, autoBtree;
	private static JComboBox topologyMenu; // Menu of topologies (Ring, Grid, etc.)
	private static JComboBox casesMenu, algorithmMenuRing, algorithmMenuCG, algorithmMenuCR, algorithmMenuGrid, algorithmMenuTorus, algorithmMenuTree, algorithmMenuArb;
	private static ActionListener playAction, pauseResumeAction; // static because accessed from static timer function
	public static JTextField chordsField, maxChildrenField; // public so the tree panel and network manager can access them directly
	public static JLabel algorithmLabel, topologyLabel; // public so the arbitrary panel can access them directly
	public static JPanel drawingPanel;
	public static boolean coolBackground = false, beepSync = false;
	public static MyTextArea out;

	private long totalPausedTime; // declared here because accessed within actionPerformed()
	private boolean thisTimeFIFO; // declared here because accessed within inner class and cannot be final
	protected static ImageIcon netViewerIcon; // goes in the upper left corner of the window
	private static JTextField numNodesField; // so we can request focus from main()
	private static int javaVersion;
  private JSplitPane innerSplitPane;
  private long pauseBegin;


	public void init() {

		/*-------------------- ** MODIFY ** --------------------

		  Adding an algorithm: When you write a new algorithm,
		  add it to the appropriate list below. This ensures it
		  will appear in a drop down menu in the NetViewer. Be
		  sure to use exactly the same name as the class you
		  create, apart from the fact that you may use spaces here.

		  ex: Class name: AlternatingSteps
		  		Your entry here: Alternating Steps

		-------------------------------------------------------*/

		String[] ringAlgorithms = {"Franklin Stages", "Alternating Steps", "All The Way", "Far As Can"};
		String[] chordalRingAlgorithms = {"Wake Up"};
		String[] completeGraphAlgorithms = {"Wake Up", "Election"};
		String[] treeAlgorithms = {"Wake Up"};
		String[] gridAlgorithms = {"Smallest Corner"};
		String[] torusAlgorithms = {"Wake Up"};
		String[] arbitraryAlgorithms = {"Wake Up", "Shout"};

		//-------------------------------------------------------

		javaVersion = Integer.parseInt(System.getProperty("java.version").substring(2,3));
		networkManager = new NetworkManager();
		networkPanel = new NetworkPanel(networkManager);
		JPanel contentPane = new JPanel();
		contentPane.setLayout(new BorderLayout());
		JPanel resultsPanel = new JPanel();
		JScrollPane resultsScrollPane = new JScrollPane(resultsPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		final JTextArea pseudocodeField = new JTextArea();
		pseudocodeField.setEditable(false);
		out = new MyTextArea();
		out.setEditable(false);
		paused = false;
		aborted = false;
		doneInSlider = false;
		drawingPanel = new JPanel();
		drawingPanel.setLayout(new BorderLayout());
		final JScrollPane infoScrollPane = new JScrollPane(pseudocodeField, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		final JScrollPane logScrollPane = new JScrollPane(out, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		JPanel logPanel = new JPanel(new BorderLayout());
		logPanel.add(logScrollPane, BorderLayout.CENTER);
		// Tabbed pane on the right of the split pane
		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Results", resultsScrollPane);
		tabs.addTab("Pseudocode", infoScrollPane);
		tabs.addTab("Log", logPanel);
		innerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, drawingPanel, tabs); // left right
		innerSplitPane.setDividerLocation(475);
		innerSplitPane.setOneTouchExpandable(true);

		// Timer
		timer = new Timer(80, this);
		timer.setInitialDelay(0);
		timer.setCoalesce(true);

		final JPanel speedPanel = new JPanel(); // holds the speed slider

		// Create play/pause/stop button images
		// Create NetViewer icon for the corner of the main window
		// Create URL for the about box contents
		URL theURL = null;
		try { // read images over web
		  String codeBase = getCodeBase().toString();
			theURL = new URL(codeBase+"playIcon.GIF");
			playImage = new ImageIcon(theURL);
			theURL = new URL(codeBase+"pauseIcon.GIF");
			pauseImage = new ImageIcon(theURL);
			theURL = new URL(codeBase+"stopIcon.GIF");
			stopImage = new ImageIcon(theURL);
			theURL = new URL(codeBase+"NetViewerIcon.GIF");
			netViewerIcon = new ImageIcon(theURL);
			theURL = new URL(codeBase+"AboutBox.html");
		} catch (MalformedURLException ex) {
			out.println("Malformed URL");
		}
		catch (NullPointerException e) { // read from local file system
			playImage = new ImageIcon("playIcon.GIF");
			pauseImage = new ImageIcon("pauseIcon.GIF");
			stopImage = new ImageIcon("stopIcon.GIF");
			netViewerIcon = new ImageIcon("NetViewerIcon.gif");
			try {
				File f = new File("AboutBox.html");
				theURL = f.toURL();
			} catch (MalformedURLException ex) {
				out.println("Malformed URL: "+ex.getMessage());
			}
		}


		/*-------- GUI COMPONENTS ON THE MENU BAR --------*/

		// Menu Bar
		menuBar = new JMenuBar();
		JMenu file = new JMenu("File");
		JMenu options = new JMenu("Options");
		JMenu help = new JMenu("Help");

		file.setMnemonic('F');
		options.setMnemonic('O');
		help.setMnemonic('H');

		// Exit menu item
		JMenuItem exit = new JMenuItem("Exit");
		exit.setMnemonic('E');
		exit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		file.add(exit);

		// Cool background menu item
		JCheckBoxMenuItem coolBg = new JCheckBoxMenuItem("Cool Background");
		coolBg.setMnemonic('C');
		coolBg.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				coolBackground = !coolBackground;
				Color newColor = (coolBackground) ? Color.white : null;
				speedSlider.setBackground(newColor);
				speedPanel.setBackground(newColor);
				networkPanel.repaint();
			}
		});
		options.add(coolBg);

		JCheckBoxMenuItem beepSyncItem = new JCheckBoxMenuItem("Beep on clock ticks (synchronous)");
		beepSyncItem.setMnemonic('B');
		beepSyncItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				beepSync = !beepSync;
			}
		});
		options.add(beepSyncItem);

		// About box
		final JPanel aboutContentPane = new JPanel();
		aboutContentPane.setLayout(new BorderLayout());
		final JButton ok = new JButton("OK");
		final JDialog aboutBox = new JDialog((Frame)null, "About the NetViewer", true);

		JEditorPane jep = null;
		try {
			jep = new JEditorPane(theURL);
		} catch (IOException ex) {
			out.println("IO Error: "+ex.getMessage());
		}

		JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		southPanel.setBackground(Color.white);
		southPanel.add(ok);

		aboutContentPane.add(jep, BorderLayout.CENTER);
		aboutContentPane.add(southPanel, BorderLayout.SOUTH);
		aboutBox.setContentPane(aboutContentPane);
		aboutBox.setLocation(200,200);
		aboutBox.setSize(400,300);

		JMenuItem aboutItem = new JMenuItem("About the NetViewer");
		aboutItem.setMnemonic('A');
		aboutItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				aboutBox.show();
				ok.requestFocus(); // not working
			}
		});
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				aboutBox.hide();
			}
		});
		help.add(aboutItem);

		menuBar.add(file);
		menuBar.add(options);
		menuBar.add(help);


		/*-------- GUI COMPONENTS ON THE TOOLBAR --------*/

		// Tool Bar
		toolBar = new JToolBar();
		toolBar.setFloatable(false);
		toolBar.setMargin(new Insets(0,10,0,10));

		// Create the combo box for selecting a network type
		String[] topologyStrings = {"Ring", "Grid", "Tree", "Arbitrary", "Complete Graph", "Chordal Ring", "Torus"};
		topologyMenu = new JComboBox(topologyStrings);
		topologyMenu.setMinimumSize(new Dimension(120,26));
		topologyMenu.setMaximumSize(new Dimension(120,26));
		toolBar.add(topologyMenu);

		// Create panel with ring options
		algorithmMenuRing = new JComboBox(ringAlgorithms);
		JLabel numNodesLabel = new JLabel("n: ");
		numNodesField = new JTextField(3); // text field for inputting # nodes
		numNodesField.setPreferredSize(new Dimension(30,26));
		Box numNodesBox = Box.createHorizontalBox();
		numNodesBox.add(numNodesLabel);
		numNodesBox.add(numNodesField);
		final JPanel ringOptions = new JPanel(new GridLayout(2,1,5,5));
		ringOptions.add(algorithmMenuRing);
		ringOptions.add(numNodesBox);
		ringOptions.setMaximumSize(new Dimension(135,60));
		ringOptions.setMinimumSize(new Dimension(135,60));
		JPanel spacer1 = new JPanel();
		spacer1.setMinimumSize(new Dimension(10,1));
		toolBar.add(spacer1);
		toolBar.add(ringOptions);

		// Create panel with chordal ring options
		algorithmMenuCR = new JComboBox(chordalRingAlgorithms);
		final JTextField numNodesFieldCR = new JTextField(3); // text field for inputting # nodes
		JLabel numNodesLabelCR = new JLabel("n: ");
		JLabel chordsLabel = new JLabel("  chords: ");
		chordsField = new JTextField(5); // text field for inputting chord length
		Box numNodesAndChordsBoxCR = Box.createHorizontalBox();
		numNodesAndChordsBoxCR.add(numNodesLabelCR);
		numNodesAndChordsBoxCR.add(numNodesFieldCR);
		numNodesAndChordsBoxCR.add(chordsLabel);
		numNodesAndChordsBoxCR.add(chordsField);
		final JPanel chordalRingOptions = new JPanel(new GridLayout(2,1,5,5));
		chordalRingOptions.add(algorithmMenuCR);
		chordalRingOptions.add(numNodesAndChordsBoxCR);
		chordalRingOptions.setMaximumSize(new Dimension(135,60));
		chordalRingOptions.setMinimumSize(new Dimension(135,60));

		// Create panel with complete graph options
		algorithmMenuCG = new JComboBox(completeGraphAlgorithms);
		JLabel numNodesLabelCG = new JLabel("n: ");
		final JTextField numNodesFieldCG = new JTextField(3); // text field for inputting # nodes
		Box numNodesBoxCG = Box.createHorizontalBox();
		numNodesBoxCG.add(numNodesLabelCG);
		numNodesBoxCG.add(numNodesFieldCG);
		final JPanel completeGraphOptions = new JPanel(new GridLayout(2,1,5,5));
		completeGraphOptions.add(algorithmMenuCG);
		completeGraphOptions.add(numNodesBoxCG);
		completeGraphOptions.setMaximumSize(new Dimension(135,60));
		completeGraphOptions.setMinimumSize(new Dimension(135,60));

		// Create panel with tree options
		algorithmMenuTree = new JComboBox(treeAlgorithms);
		final JPanel treeOptions = new JPanel(new GridLayout(2,1,5,5));
		treeOptions.add(algorithmMenuTree);
		treeOptions.setMaximumSize(new Dimension(135,60));
		treeOptions.setMinimumSize(new Dimension(135,60));
		JLabel maxChildrenLabel = new JLabel("Max children: ");
		maxChildrenField = new JTextField(3); // text field for inputting max # children per node
		maxChildrenField.setText(String.valueOf(TreeNode.DEFAULT_MAX_CHILDREN));
		maxChildrenField.setMinimumSize(new Dimension(30,26));
		Box maxChildrenBox = Box.createHorizontalBox();
		maxChildrenBox.add(maxChildrenLabel);
		maxChildrenBox.add(maxChildrenField);
		treeOptions.add(maxChildrenBox);

		// Create panel with grid options
		algorithmMenuGrid = new JComboBox(gridAlgorithms);
		JLabel rowsLabel = new JLabel("rows: ");
		JLabel colsLabel = new JLabel("  cols: ");
		final JTextField rowsField = new JTextField(3); // # rows
		final JTextField colsField = new JTextField(3); // # cols

		Box colRowBox = Box.createHorizontalBox();
		colRowBox.add(rowsLabel);
		colRowBox.add(rowsField);
		colRowBox.add(colsLabel);
		colRowBox.add(colsField);
		final JPanel gridOptions = new JPanel(new GridLayout(2,1,5,5));
		gridOptions.add(algorithmMenuGrid);
		gridOptions.add(colRowBox);
		gridOptions.setMaximumSize(new Dimension(135,60));
		gridOptions.setMinimumSize(new Dimension(135,60));

		// Create panel with torus options
		algorithmMenuTorus = new JComboBox(torusAlgorithms);
		JLabel rowsLabelTorus = new JLabel("rows: ");
		JLabel colsLabelTorus = new JLabel("  cols: ");
		final JTextField rowsFieldTorus = new JTextField(3); // # rows
		final JTextField colsFieldTorus = new JTextField(3); // # cols
		Box colRowBoxTorus = Box.createHorizontalBox();
		colRowBoxTorus.add(rowsLabelTorus);
		colRowBoxTorus.add(rowsFieldTorus);
		colRowBoxTorus.add(colsLabelTorus);
		colRowBoxTorus.add(colsFieldTorus);
		final JPanel torusOptions = new JPanel(new GridLayout(2,1,5,5));
		torusOptions.add(algorithmMenuTorus);
		torusOptions.add(colRowBoxTorus);
		torusOptions.setMaximumSize(new Dimension(135,60));
		torusOptions.setMinimumSize(new Dimension(135,60));

		// Create panel with arbitrary network options
		algorithmMenuArb = new JComboBox(arbitraryAlgorithms);
		algorithmMenuArb.setMinimumSize(new Dimension(60,26));
		algorithmMenuArb.setMaximumSize(new Dimension(120,26));
		final JPanel arbitraryOptions = new JPanel(new GridLayout(1,1,5,5));
		Box b3 = Box.createVerticalBox();
		b3.add(Box.createGlue());
		b3.add(algorithmMenuArb);
		b3.add(Box.createGlue());
		//arbitraryOptions.add(algorithmMenuArb);
		arbitraryOptions.add(b3);
		arbitraryOptions.setMaximumSize(new Dimension(135,60));
		arbitraryOptions.setMinimumSize(new Dimension(135,60));


		/*-------- GUI COMPONENTS ON THE RESULTS TAB --------*/

		JLabel statusTitle      = new JLabel(" Status: ");
		JLabel algorithmTitle   = new JLabel(" Algorithm: ");
		JLabel topologyTitle    = new JLabel(" Network Topology: ");
		JLabel messagesNoNTitle = new JLabel(" Messages (without notification): ");
		JLabel runningTimeTitle = new JLabel(" Running Time: ");
		final JLabel clockTicksTitle  = new JLabel(" Clock Ticks: "); // final because it gets disabled from within an inner class
		JLabel messagesWithNTitle = new JLabel(" Messages (with notification): ");

		statusLabel      = new JLabel("Idle");
		algorithmLabel   = new JLabel();
		topologyLabel    = new JLabel();
		runningTimeLabel = new JLabel("0");
		messagesWithNLabel = new JLabel("0");
		messagesNoNLabel = new JLabel("0");
		clockTicksLabel = new JLabel("0");

		Box statusBox      = Box.createHorizontalBox();
		Box algorithmBox   = Box.createHorizontalBox();
		Box topologyBox    = Box.createHorizontalBox();
		Box runningTimeBox = Box.createHorizontalBox();
		Box messagesNoNBox = Box.createHorizontalBox();
		Box clockTicksBox  = Box.createHorizontalBox();
		Box messagesWithNBox = Box.createHorizontalBox();

		statusBox.add(statusTitle);
		statusBox.add(statusLabel);
		statusBox.add(Box.createGlue()); // glue expands so labels stay flush to the left
		topologyBox.add(topologyTitle);
		topologyBox.add(topologyLabel);
		topologyBox.add(Box.createGlue()); // glue expands so labels stay flush to the left
		algorithmBox.add(algorithmTitle);
		algorithmBox.add(algorithmLabel);
		algorithmBox.add(Box.createGlue()); // glue expands so labels stay flush to the left
		messagesWithNBox.add(messagesWithNTitle);
		messagesWithNBox.add(messagesWithNLabel);
		messagesWithNBox.add(Box.createGlue()); // glue expands so labels stay flush to the left
		messagesNoNBox.add(messagesNoNTitle);
		messagesNoNBox.add(messagesNoNLabel);
		messagesNoNBox.add(Box.createGlue()); // glue expands so labels stay flush to the left
		runningTimeBox.add(runningTimeTitle);
		runningTimeBox.add(runningTimeLabel);
		runningTimeBox.add(Box.createGlue()); // glue expands so labels stay flush to the left
		clockTicksBox.add(clockTicksTitle);
		clockTicksBox.add(clockTicksLabel);
		clockTicksBox.add(Box.createGlue()); // glue expands so labels stay flush to the left
		clockTicksTitle.setEnabled(false); // because NetViewer starts out non-synchronous
		clockTicksLabel.setEnabled(false); // because NetViewer starts out non-synchronous

		Box resultsBox = Box.createVerticalBox();
		resultsBox.add(statusBox);
		resultsBox.add(Box.createVerticalStrut(5));
		resultsBox.add(topologyBox);
		resultsBox.add(Box.createVerticalStrut(5));
		resultsBox.add(algorithmBox);
		resultsBox.add(Box.createVerticalStrut(5));
		resultsBox.add(messagesWithNBox);
		resultsBox.add(Box.createVerticalStrut(5));
		resultsBox.add(messagesNoNBox);
		resultsBox.add(Box.createVerticalStrut(5));
		resultsBox.add(runningTimeBox);
		resultsBox.add(Box.createVerticalStrut(5));
		resultsBox.add(clockTicksBox);
		resultsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
		resultsPanel.add(resultsBox);


		/*-------- GUI COMPONENTS ON THE LOG TAB --------*/

		final JButton clearLogButton = new JButton("Clear");
		clearLogButton.setPreferredSize(new Dimension(70,26));
		clearLogButton.setRequestFocusEnabled(false);
	  clearLogButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
				out.setText("");
			}
    });
		final JButton copyLogButton = new JButton("Copy");
		copyLogButton.setPreferredSize(new Dimension(70,26));
		copyLogButton.setRequestFocusEnabled(false);
	  copyLogButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
				out.selectAll();
				out.copy();
			}
    });
		final JPanel clearCopyPanel = new JPanel();
		clearCopyPanel.add(clearLogButton);
		clearCopyPanel.add(copyLogButton);
		// Resize the clear/copy buttons when the panel gets too small. Avoids them being positioned out of sight.
		clearCopyPanel.addComponentListener(new ComponentListener() {
			public void componentResized(ComponentEvent e) {
				int width = clearCopyPanel.getWidth();
				if (width > 0 && width < 155)
				{
					int newLength = (width-15)/2;
					clearLogButton.setPreferredSize(new Dimension(newLength, 26));
					copyLogButton.setPreferredSize(new Dimension(newLength, 26));
				}
				else
				{
					clearLogButton.setPreferredSize(new Dimension(70, 26));
					copyLogButton.setPreferredSize(new Dimension(70, 26));
				}
				clearCopyPanel.revalidate();
			}
			public void componentMoved(ComponentEvent e) {}
			public void componentShown(ComponentEvent e) {}
			public void componentHidden(ComponentEvent e) {}
		});
		logPanel.add(clearCopyPanel, BorderLayout.NORTH);


		/*-------- ACTION LISTENERS --------*/

		// Action for menu that changes between average/best/worst case
		ItemListener changeCase = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				JComboBox casesMenu = (JComboBox)e.getSource();
				if (e.getStateChange() == ItemEvent.DESELECTED)
					return; // do nothing
				String menuItem = ((String)casesMenu.getSelectedItem()).toLowerCase();
				String whichCase = menuItem.substring(0,menuItem.indexOf(" "));
				casesMenu.setActionCommand(whichCase);
				if (networkPanel.getDrawingArea().isBlank())
					return; // stop here; do not change case because there are no nodes
				networkManager.changeCase(whichCase); // arrange node ids to best case configuration
				if (networkPanel.getDrawingArea().isDirty()) {
					networkManager.resetNodesAndLinks();
					clearResults();
					networkPanel.getDrawingArea().setIsDirty(false);
				}
				networkPanel.repaint();
			} // itemStateChanged
		};
		// Cases drop down menu for average, best, worst case
		String[] caseNames = {"Average Case", "Best Case", "Worst Case"};
		casesMenu = new JComboBox(caseNames);
		casesMenu.setActionCommand("average"); // default value
		casesMenu.addItemListener(changeCase);

		// Action for check box that changes between instant and staggered wakeup
		ItemListener instantWakeUpAction = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (networkPanel.getDrawingArea().isBlank())
					return; // stop here; no nodes to work with
				if (e.getStateChange() == ItemEvent.SELECTED)
					networkManager.instantWakeUp();
				else { // deselected
					networkManager.setWakeUpOrder(); // randomize for synchronous networks
					networkManager.staggerWakeUp(); // randomize for non-synchronous networks
				}
			} // itemStateChanged
		};

		// Action for check box that synchronizes the network
		ItemListener synchroAction = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Link.setNewUnitOfTime();
				// update GUI
				if (e.getStateChange() == ItemEvent.SELECTED) {
					fifo.setSelected(true);
					fifo.setEnabled(false);
					clockTicksTitle.setEnabled(true);
					clockTicksLabel.setEnabled(true);
				}
				else { // deselected
					fifo.setEnabled(true);
					clockTicksTitle.setEnabled(false);
					clockTicksLabel.setEnabled(false);
				}
				if (networkPanel.getDrawingArea().isBlank())
					return; // stop here; no nodes to work with
				// modify network
				if (e.getStateChange() == ItemEvent.SELECTED) {
					networkManager.setAllLinksSameSpeed();
					if (!isInstantWakeUp())
						networkManager.setWakeUpOrder();
				}
				else { // deselected
					networkManager.setRandomSpeeds();
				}
/*				if (networkPanel.getDrawingArea().isDirty()) {
					networkManager.resetNodes();
					clearResults();
					networkPanel.getDrawingArea().setIsDirty(false);
					networkPanel.repaint();
				}
*/
			} // itemStateChanged
		};

		instantWakeUp = new JCheckBox("Instant Wake Up");
		instantWakeUp.addItemListener(instantWakeUpAction);
		Box casesBox = Box.createHorizontalBox();
		casesBox.add(casesMenu);
		casesBox.add(Box.createGlue());
		final JPanel options1 = new JPanel(new GridLayout(2,1,3,3));
		//options1.setMaximumSize(new Dimension(135,60));
		options1.add(casesBox);
		options1.add(instantWakeUp);

		// FIFO and synchronous checkboxes
		final JPanel options2 = new JPanel(new GridLayout(2,1,3,3));
		fifo = new JCheckBox("FIFO", true);
		synchronous = new JCheckBox("Synchronous");
		synchronous.addItemListener(synchroAction);
		options2.add(fifo);
		options2.add(synchronous);
		thisTimeFIFO = true;

		/*-------- KEY STROKE VALIDATION --------*/

		// Basic key stoke validation for text fields. Only accept numbers, restrict # nodes to 2 digits (99)
		final KeyListener validateKey = new KeyListener() {
			public void keyPressed(KeyEvent e) {
				if (e.isControlDown()) // prevent copy/paste (it could allow incorrect entries into the text field with a single key stroke)
					e.setKeyCode(KeyEvent.CHAR_UNDEFINED); // void the key pressed
			}
			public void keyReleased(KeyEvent e) {}
			public void keyTyped(KeyEvent e) {
				char key = e.getKeyChar();
				if (key == KeyEvent.VK_BACK_SPACE)
					return; // backspace is allowed. No further processing needed.
				if (((JTextField)e.getSource()).getText().length() >= 2) { // prevent huge #s of nodes - just freezes the program
					if (((JTextField)e.getSource()).getSelectedText() == null) { // there is no selected text
						e.setKeyChar(KeyEvent.CHAR_UNDEFINED); // void the key typed
						return;
					}
				}
				if (key == '1' || key == '2' || key == '3' || key == '4' || key == '5' || key == '6' || key == '7' || key == '8' || key == '9' || key == '0') { // numeric keys are ok
					// ok
				}
				else { // not ok --> non-numeric key pressed
					e.setKeyChar(KeyEvent.CHAR_UNDEFINED);
				}
			} // key typed
		};

		numNodesField  .addKeyListener(validateKey);
    numNodesFieldCR.addKeyListener(validateKey);
    numNodesFieldCG.addKeyListener(validateKey);
		rowsField      .addKeyListener(validateKey);
		colsField      .addKeyListener(validateKey);
		rowsFieldTorus .addKeyListener(validateKey);
		colsFieldTorus .addKeyListener(validateKey);

		// validation - only accept numbers
		chordsField.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				if (e.isControlDown()) // prevent copy/paste (it could allow incorrect entries into the text field with a single key stroke)
					e.setKeyCode(KeyEvent.CHAR_UNDEFINED); // void the key pressed
			}
			public void keyReleased(KeyEvent e) { }
			public void keyTyped(KeyEvent e) {
				char key = e.getKeyChar();
				if (key == KeyEvent.VK_BACK_SPACE)
					return; // backspace is allowed. No further processing needed.
				if (key == '1' || key == '2' || key == '3' || key == '4' || key == '5' || key == '6' || key == '7' || key == '8' || key == '9' || key == '0' || key == ',') {
					// ok
				}
				else { // not ok
					e.setKeyChar(KeyEvent.CHAR_UNDEFINED);
				}
			} // key pressed
		});

		// validation - only accept numbers, limit # children to 15
		maxChildrenField.addKeyListener(new KeyListener() {
			String originalText;
			int originalCaretPosition;
			public void keyPressed(KeyEvent e) {
				if (e.isControlDown()) // prevent copy/paste (it could allow incorrect entries into the text field with a single key stroke)
					e.setKeyCode(KeyEvent.CHAR_UNDEFINED); // void the key pressed
			}
			public void keyTyped(KeyEvent e) {
				int key = e.getKeyChar();
				if (key == '1' || key == '2' || key == '3' || key == '4' || key == '5' || key == '6' || key == '7' || key == '8' || key == '9' || key == '0' || key == KeyEvent.VK_BACK_SPACE) { // numeric keys and backspace are ok
					originalText = maxChildrenField.getText(); // save in case the user enters a number that is too large and we need to revert to the original number
					originalCaretPosition = maxChildrenField.getCaretPosition();
				}
				else { // not ok --> non-numeric key pressed
					e.setKeyChar(KeyEvent.CHAR_UNDEFINED);
				}
			} // key typed
			public void keyReleased(KeyEvent e) {
				if (maxChildrenField.getText().length() > 0 && Integer.parseInt(maxChildrenField.getText()) > 15)
				{
					JOptionPane.showMessageDialog(playPauseButton,"The number of children must be 15 or less.", "Input Error", JOptionPane.WARNING_MESSAGE);
					maxChildrenField.setText(originalText);
					maxChildrenField.setCaretPosition(originalCaretPosition);
				}
			}
		});

		final ActionListener loadPseudocode = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox)e.getSource();
				String algorithmName = (String)cb.getSelectedItem();
				String pseudoCode = getAlgPseudoCode(networkManager.noSpaces(algorithmName)+".txt");
				pseudocodeField.setText(pseudoCode);
				try // place cursor in text field
				{
					JTextField textField = null;
					Box box = (Box)cb.getParent().getComponent(1); // the *2nd* component --> the box with the text field
					Component[] components = box.getComponents();
					for (int i=0; i < components.length; i++) {
						if (components[i] instanceof JTextField) {
							textField = (JTextField)components[i];
							break;
						}
					}
					if (textField != null)
						textField.requestFocus();
				}
				catch (ClassCastException ex) {
					// do nothing. Some topologies (ex. Arbitrary) have no text fields
				}
				pseudocodeField.setCaretPosition(0); // scroll to top of text area (pseudocode)
			}
		};

		ItemListener changeAlgorithm = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.DESELECTED)
					return;
				JComboBox algMenu = (JComboBox)e.getSource();
				loadPseudocode.actionPerformed(new ActionEvent(algMenu, 0, null)); // load pseudocode
				if (networkPanel.getDrawingArea().isBlank())
					return;
				// Change algorithm
				String algorithmName = (String)algMenu.getSelectedItem();
				networkManager.changeAlgorithm(algorithmName); // change any existing nodes to this type
				if (casesMenu.isEnabled()) {
					String whichCase = casesMenu.getActionCommand();
					if (!whichCase.equals("average")) // best or worst case
						networkManager.changeCase(whichCase); // change node ids to reflect case
				}
				networkPanel.repaint();
				topologyLabel.setText(networkManager.getNetworkType());
				algorithmLabel.setText(algorithmName);
			}
		};

		algorithmMenuGrid .addItemListener(changeAlgorithm);
		algorithmMenuTorus.addItemListener(changeAlgorithm);
		algorithmMenuCR   .addItemListener(changeAlgorithm);
		algorithmMenuCG   .addItemListener(changeAlgorithm);
		
		algorithmMenuArb  .addItemListener(changeAlgorithm);
		algorithmMenuArb  .addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				JComboBox cb = (JComboBox)e.getSource();
				String algorithmName = (String)cb.getSelectedItem();
				if (algorithmName.equals("Shout"))
					fifo.setSelected(true); // Shout must be FIFO
			}
		});
		
		algorithmMenuTree .addItemListener(changeAlgorithm);
		
		algorithmMenuRing .addItemListener(changeAlgorithm);
		algorithmMenuRing .addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				JComboBox cb = (JComboBox)e.getSource();
				String algorithmName = (String)cb.getSelectedItem();
				if (algorithmName.equals("Franklin Stages"))
					fifo.setSelected(true); // Franklin Stages must be FIFO
			}
		});

		JPanel spacer2 = new JPanel();
		spacer2.setMinimumSize(new Dimension(10,1));
    toolBar.add(spacer2);
    toolBar.add(options1);
		JPanel spacer3 = new JPanel();
		spacer3.setMinimumSize(new Dimension(10,1));
    toolBar.add(spacer3);
    toolBar.add(options2);
		speedSlider = new JSlider(1,180,80);
		speedSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (((JSlider)e.getSource()).getValueIsAdjusting()) return; // only reset speed if slider has finished adjusting (user has released the mouse)
			 	if (networkPanel.getDrawingArea().isBlank()) return; // make sure a network has been created
				if (isSynchronous()) {
					Link.setNewUnitOfTime();
					networkManager.setAllLinksSameSpeed();
				}
				else { // not synchronous
					networkManager.setRandomSpeeds();
				}
				if (networkManager.isRunning()) { // then recalculate speed of existing messages
					for (int i = 0; i < networkManager.getNumLinks(); i++)
						adjustMessages(networkManager.getLink(i));
					if (isSynchronous()) { // adjust timer's delay between tasks
						networkManager.syncTimer.stop(); // pause momentarily
						long now;
						if (paused)
							now = pauseBegin;
						else
							now = System.currentTimeMillis();
						long oldDelay = networkManager.syncTimer.getDelay();
						long newDelay = Link.getUnitOfTime();
						long timeDone = now - networkManager.lastTick;
						double percentDone = (double)timeDone/oldDelay;
						if (percentDone > 1) {
							networkManager.syncTimer.setInitialDelay(0);
							networkManager.lastTick = now - newDelay; // a full unit of time ago
						}
						else {
							networkManager.syncTimer.setInitialDelay((int)((1-percentDone)*newDelay));
							networkManager.lastTick = now - (newDelay - networkManager.syncTimer.getInitialDelay());//now - (long)percentDone*newDelay;
						}
						networkManager.syncTimer.setDelay((int)newDelay);
						if (!paused)
							networkManager.syncTimer.restart(); // with initial delay to complete previous time unit
						else
							doneInSlider = true; // when resumed, don't recalculate initial delay of timer and last tick
					}
				} // timer running
			} // stateChanged
		});
		JLabel speedLabel = new JLabel("Speed: ");
		Box speedSliderBox = Box.createHorizontalBox();
		speedSliderBox.add(speedLabel);
		speedSliderBox.add(speedSlider);
		speedPanel.add(speedSliderBox);

		// Options for automatic tree creation
		autoBtree = new JCheckBox("Auto");
		autoBtree.setSelected(true);
		final JLabel nLabelTree = new JLabel(" n: ");
		final JTextField nFieldTree = new JTextField(2);
		nFieldTree.addKeyListener(validateKey);
		autoBtree.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					nFieldTree.setEnabled(true);
					nLabelTree.setEnabled(true);
					newNetworkButton.setEnabled(true);
					nFieldTree.requestFocus();
				}
				else { // deselected
					nFieldTree.setEnabled(false);
					nLabelTree.setEnabled(false);
					newNetworkButton.requestFocus();
				}
			} // itemStateChanged
		});
		final Box autoBoxTree = Box.createHorizontalBox();
		autoBoxTree.add(autoBtree);
		autoBoxTree.add(nLabelTree);
		autoBoxTree.add(nFieldTree);

		// Options for automatic arbitrary network creation
		autoBarb = new JCheckBox("Auto");
		autoBarb.setSelected(true);
		final JLabel nLabelArb = new JLabel(" n: ");
		final JTextField nFieldArb = new JTextField(2);
		nFieldArb.addKeyListener(validateKey);
		autoBarb.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					nFieldArb.setEnabled(true);
					nLabelArb.setEnabled(true);
					newNetworkButton.setEnabled(true);
					nFieldArb.requestFocus();
				}
				else { // deselected
					nFieldArb.setEnabled(false);
					nLabelArb.setEnabled(false);
					newNetworkButton.setEnabled(false);
				}
			} // itemStateChanged
		});
		final Box autoBoxArb = Box.createHorizontalBox();
		autoBoxArb.add(autoBarb);
		autoBoxArb.add(nLabelArb);
		autoBoxArb.add(nFieldArb);

		newNetworkButton = new JButton("New Network");
		final JPanel newNetworkPanel = new JPanel();
		newNetworkPanel.add(newNetworkButton);
		topologyMenu.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.DESELECTED) return;
				JComboBox cb = (JComboBox)e.getSource();
				String topologyName = (String)cb.getSelectedItem();
				toolBar.remove(toolBar.getComponentAtIndex(2));
				JComboBox selector = null;
				if (topologyName.equals("Ring")) {
					toolBar.add(ringOptions, 2);
					selector = algorithmMenuRing;
					casesMenu.setEnabled(true);
					newNetworkButton.setEnabled(true);
					newNetworkPanel.remove(autoBoxTree);
					newNetworkPanel.remove(autoBoxArb);
				}
				if (topologyName.equals("Complete Graph")) {
					toolBar.add(completeGraphOptions, 2);
					selector = algorithmMenuCG;
					casesMenu.setEnabled(false);
					newNetworkButton.setEnabled(true);
					newNetworkPanel.remove(autoBoxTree);
					newNetworkPanel.remove(autoBoxArb);
				}
				if (topologyName.equals("Chordal Ring")) {
					toolBar.add(chordalRingOptions, 2);
					selector = algorithmMenuCR;
					casesMenu.setEnabled(false);
					newNetworkButton.setEnabled(true);
					newNetworkPanel.remove(autoBoxTree);
					newNetworkPanel.remove(autoBoxArb);
				}
				else if (topologyName.equals("Grid")) {
					toolBar.add(gridOptions, 2);
					selector = algorithmMenuGrid;
					casesMenu.setEnabled(false);
					newNetworkButton.setEnabled(true);
					newNetworkPanel.remove(autoBoxTree);
					newNetworkPanel.remove(autoBoxArb);
				}
				else if (topologyName.equals("Torus")) {
					toolBar.add(torusOptions, 2);
					selector = algorithmMenuTorus;
					casesMenu.setEnabled(false);
					newNetworkButton.setEnabled(true);
					newNetworkPanel.remove(autoBoxTree);
					newNetworkPanel.remove(autoBoxArb);
				}
				else if (topologyName.equals("Tree")) {
					toolBar.add(treeOptions, 2);
					selector = algorithmMenuTree;
					casesMenu.setEnabled(false);
					newNetworkButton.setEnabled(true);
					newNetworkPanel.remove(autoBoxArb);
					newNetworkPanel.add(autoBoxTree);
				}
				else if (topologyName.equals("Arbitrary")) {
					toolBar.add(arbitraryOptions, 2);
					selector = algorithmMenuArb;
					casesMenu.setEnabled(false);
					if (!autoBarb.isSelected())
						newNetworkButton.setEnabled(false);
					newNetworkPanel.remove(autoBoxTree);
					newNetworkPanel.add(autoBoxArb);
				}
				/*---------------------------------------
				Validate this container and all of its subcomponents.
				The validate method is used to make the container lay
				out its subcomponents. It should be invoked when the
				container's subcomponents are modified (added to or removed,
				or layout-related information changed) after the container
				has been displayed.
				---------------------------------------*/
				networkPanel.switchTo(topologyName);
				loadPseudocode.actionPerformed(new ActionEvent(selector, 0, null)); // load pseudocode
				toolBar.revalidate(); // options panel has changed
				newNetworkPanel.revalidate(); // in case the auto creation components were added
				toolBar.repaint();
			}
		});
		// Initial configuration
		networkPanel.switchTo("Ring");
		loadPseudocode.actionPerformed(new ActionEvent(algorithmMenuRing, 0, null)); // load pseudocode
		numNodesField.requestFocus();
		toolBar.revalidate();

		newNetworkButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (topologyMenu.getSelectedIndex()==0) { // Ring
					if (numNodesField.getText().equals("")) { // popup window with error message
						JOptionPane.showMessageDialog(playPauseButton,"Please enter a value for the number of nodes.", "Input Error", JOptionPane.WARNING_MESSAGE);
						numNodesField.requestFocus();
					}
					else if (Integer.parseInt(numNodesField.getText()) < 2) { // popup window with error message
						JOptionPane.showMessageDialog(playPauseButton,"The number of nodes must be larger than 1.", "Input Error", JOptionPane.WARNING_MESSAGE);
						numNodesField.requestFocus();
					}
					else { // proceed
						int numNodes = Integer.parseInt(numNodesField.getText());
						String algorithm = (String)algorithmMenuRing.getSelectedItem();
						String whichCase = casesMenu.getActionCommand();
						networkManager.createRingNetwork(numNodes, algorithm, whichCase);
						networkPanel.getDrawingArea().setIsDirty(false);
						clearResults();
						topologyLabel.setText(networkManager.getNetworkType());
						algorithmLabel.setText(networkManager.getAlgorithm());
						networkPanel.repaint();
					}
				}
				else if (topologyMenu.getSelectedIndex()==1) { // Grid
					if (rowsField.getText().equals("")) { // popup window with error message
						JOptionPane.showMessageDialog(playPauseButton,"Please enter a value for the number of rows.", "Input Error", JOptionPane.WARNING_MESSAGE);
						rowsField.requestFocus();
					}
					else if (colsField.getText().equals("")) { // popup window with error message
						JOptionPane.showMessageDialog(playPauseButton,"Please enter a value for the number of columns.", "Input Error", JOptionPane.WARNING_MESSAGE);
						colsField.requestFocus();
					}
					else if (Integer.parseInt(rowsField.getText()) < 2) { // popup window with error message
						JOptionPane.showMessageDialog(playPauseButton,"The number of rows must be larger than 1.", "Input Error", JOptionPane.WARNING_MESSAGE);
						rowsField.requestFocus();
					}
					else if (Integer.parseInt(colsField.getText()) < 2) { // popup window with error message
						JOptionPane.showMessageDialog(playPauseButton,"The number of columns must be larger than 1.", "Input Error", JOptionPane.WARNING_MESSAGE);
						colsField.requestFocus();
					}
					else { // proceed
						int rows = Integer.parseInt(rowsField.getText());
						int cols = Integer.parseInt(colsField.getText());
						String algorithm = (String)algorithmMenuGrid.getSelectedItem();
						String whichCase = casesMenu.getActionCommand();
						networkManager.createGridNetwork(rows, cols, algorithm, whichCase);
						networkPanel.getDrawingArea().setIsDirty(false);
						clearResults();
						topologyLabel.setText(networkManager.getNetworkType());
						algorithmLabel.setText(networkManager.getAlgorithm());
						networkPanel.repaint();
					}
				} // grid
				else if (topologyMenu.getSelectedIndex()==4) { // Complete graph
					if (numNodesFieldCG.getText().equals("")) { // popup window with error message
						JOptionPane.showMessageDialog(playPauseButton,"Please enter a value for the number of nodes.", "Input Error", JOptionPane.WARNING_MESSAGE);
						numNodesField.requestFocus();
					}
					else if (Integer.parseInt(numNodesFieldCG.getText()) < 2) { // popup window with error message
						JOptionPane.showMessageDialog(playPauseButton,"The number of nodes must be larger than 1.", "Input Error", JOptionPane.WARNING_MESSAGE);
						numNodesField.requestFocus();
					}
					else { // proceed
						int numNodes = Integer.parseInt(numNodesFieldCG.getText());
						String algorithm = (String)algorithmMenuCG.getSelectedItem();
						String whichCase = casesMenu.getActionCommand();
						networkManager.createCompleteGraphNetwork(numNodes, algorithm, whichCase);
						networkPanel.getDrawingArea().setIsDirty(false);
						clearResults();
						topologyLabel.setText(networkManager.getNetworkType());
						algorithmLabel.setText(networkManager.getAlgorithm());
						networkPanel.repaint();
					}
				} // complete graph
				else if (topologyMenu.getSelectedIndex()==5) { // chordal ring
					if (numNodesFieldCR.getText().equals("")) { // popup window with error message
						JOptionPane.showMessageDialog(playPauseButton,"Please enter a value for the number of nodes.", "Input Error", JOptionPane.WARNING_MESSAGE);
						numNodesField.requestFocus();
					}
					else if (Integer.parseInt(numNodesFieldCR.getText()) < 2) { // popup window with error message
						JOptionPane.showMessageDialog(playPauseButton,"The number of nodes must be larger than 1.", "Input Error", JOptionPane.WARNING_MESSAGE);
						numNodesField.requestFocus();
					}
					else { // proceed
						int numNodes = Integer.parseInt(numNodesFieldCR.getText());
						String algorithm = (String)algorithmMenuCG.getSelectedItem();
						String whichCase = casesMenu.getActionCommand();
						String chords = chordsField.getText();
						networkManager.createChordalRingNetwork(numNodes, algorithm, whichCase, chords);
						networkPanel.getDrawingArea().setIsDirty(false);
						clearResults();
						topologyLabel.setText(networkManager.getNetworkType());
						algorithmLabel.setText(networkManager.getAlgorithm());
						networkPanel.repaint();
					}
				} // chordal ring
				else if (topologyMenu.getSelectedIndex()==6) { // torus
					if (rowsFieldTorus.getText().equals("")) { // popup window with error message
						JOptionPane.showMessageDialog(playPauseButton,"Please enter a value for the number of rows.", "Input Error", JOptionPane.WARNING_MESSAGE);
						rowsFieldTorus.requestFocus();
					}
					else if (colsFieldTorus.getText().equals("")) { // popup window with error message
						JOptionPane.showMessageDialog(playPauseButton,"Please enter a value for the number of columns.", "Input Error", JOptionPane.WARNING_MESSAGE);
						colsFieldTorus.requestFocus();
					}
					else if (Integer.parseInt(rowsFieldTorus.getText()) < 2) { // popup window with error message
						JOptionPane.showMessageDialog(playPauseButton,"The number of rows must be larger than 1.", "Input Error", JOptionPane.WARNING_MESSAGE);
						rowsFieldTorus.requestFocus();
					}
					else if (Integer.parseInt(colsFieldTorus.getText()) < 2) { // popup window with error message
						JOptionPane.showMessageDialog(playPauseButton,"The number of columns must be larger than 1.", "Input Error", JOptionPane.WARNING_MESSAGE);
						colsFieldTorus.requestFocus();
					}
					else { // proceed
						int rows = Integer.parseInt(rowsFieldTorus.getText());
						int cols = Integer.parseInt(colsFieldTorus.getText());
						String algorithm = (String)algorithmMenuTorus.getSelectedItem();
						String whichCase = casesMenu.getActionCommand();
						networkManager.createTorusNetwork(rows, cols, algorithm, whichCase);
						networkPanel.getDrawingArea().setIsDirty(false);
						clearResults();
						topologyLabel.setText(networkManager.getNetworkType());
						algorithmLabel.setText(networkManager.getAlgorithm());
						networkPanel.repaint();
					}
				} // torus
				else if (topologyMenu.getSelectedIndex()==2) { // tree
					if (!autoBtree.isSelected()) // manual tree creation
					{ // just draw tree root
						networkPanel.getDrawingArea().setIsDirty(false);
						networkPanel.getDrawingArea().setIsBlank(true);
						networkManager.initializeNetwork();
						clearResults();
						topologyLabel.setText(networkManager.getNetworkType());
						algorithmLabel.setText(networkManager.getAlgorithm());
						((TreePanel)networkPanel.getDrawingArea()).drawRoot(); // repaints itself
					}
					else // automatic tree creation
					{ // generate whole tree with set # nodes
						if (!nFieldTree.getText().equals("") && Integer.parseInt(nFieldTree.getText()) == 0) { // popup window with error message
							JOptionPane.showMessageDialog(playPauseButton,"The number of nodes must be larger than 0.", "Input Error", JOptionPane.WARNING_MESSAGE);
							nFieldTree.requestFocus();
							return;
						}
						networkManager.initializeNetwork();
						int n;
						if (nFieldTree.getText().equals(""))
							n = (int)Math.round((14)*Math.random())+2; // set n between 2 and 15
						else
							n = Integer.parseInt(nFieldTree.getText());
						networkPanel.getDrawingArea().setIsDirty(false);
						networkPanel.getDrawingArea().setIsBlank(true);
						networkManager.createTree(n);
						clearResults();
						topologyLabel.setText(networkManager.getNetworkType());
						algorithmLabel.setText(networkManager.getAlgorithm());
						networkPanel.repaint();
					}
				} // tree
				else if (topologyMenu.getSelectedIndex()==3) { // arbitrary network (automatic creation)
					if (!nFieldArb.getText().equals("") && Integer.parseInt(nFieldArb.getText()) == 0) { // popup window with error message
						JOptionPane.showMessageDialog(playPauseButton,"The number of nodes must be larger than 0.", "Input Error", JOptionPane.WARNING_MESSAGE);
						nFieldArb.requestFocus();
						return;
					}
					networkManager.initializeNetwork();
					int n;
					if (nFieldArb.getText().equals(""))
						n = (int)Math.round((14)*Math.random())+2; // set n between 2 and 15
					else
						n = Integer.parseInt(nFieldArb.getText());
					networkPanel.getDrawingArea().setIsDirty(false);
					networkPanel.getDrawingArea().setIsBlank(true);
					networkManager.createArbitraryNetwork(n);
					clearResults();
					topologyLabel.setText(networkManager.getNetworkType());
					algorithmLabel.setText(networkManager.getAlgorithm());
					networkPanel.repaint();
				} // arb
			} // actionPerformed
		}); // new network

		// Create play/pause button
		playPauseButton = new JButton(playImage);
		playPauseButton.setRequestFocusEnabled(false);
		playPauseButton.setPreferredSize(new Dimension(60,26));
		pauseResumeAction = new ActionListener() {
			long syncTimeLeft;
			public void actionPerformed(ActionEvent e) {
				if (!paused) { // pause action
					out.println("Algorithm Paused");
					statusLabel.setText("Paused");
					paused = true;
					timer.stop();
					if (isSynchronous()) networkManager.syncTimer.stop();
					pauseBegin = System.currentTimeMillis();
					playPauseButton.setIcon(playImage);
				}
				else {
					out.println("Algorithm Resumed");
					statusLabel.setText("Running");
					paused = false;
					timer.restart();
					long now = System.currentTimeMillis();
					totalPausedTime += now - pauseBegin;
					playPauseButton.setIcon(pauseImage);
					if (isSynchronous()) {
						if (!doneInSlider) {
							long timeLeft = networkManager.syncTimer.getDelay() - (pauseBegin - networkManager.lastTick);
							if (timeLeft < 0) {
								networkManager.syncTimer.setInitialDelay(0);
								networkManager.lastTick = now - networkManager.syncTimer.getDelay();
							}
							else {
								networkManager.syncTimer.setInitialDelay((int)timeLeft);
								networkManager.lastTick = now - (networkManager.syncTimer.getDelay() - timeLeft);
							}
						}
						else {
							doneInSlider = false;
						}
						networkManager.syncTimer.restart();
					}
				}
			}
		};
		playAction = new ActionListener() {
    	public void actionPerformed(ActionEvent e) {
			 	if (networkPanel.getDrawingArea().isBlank())
			 		JOptionPane.showMessageDialog(playPauseButton, "Please create a network.", "Input Error", JOptionPane.WARNING_MESSAGE);
				else { // a network exists
					if (networkPanel.getDrawingArea().isDirty()) {
						networkManager.resetNodesAndLinks();
						clearResults();
						networkPanel.getDrawingArea().setIsDirty(false);
					}
					thisTimeFIFO = isFIFO();
					executionTime = 0; // initialize
					totalPausedTime = 0;
					if (aborted)
						aborted = false;
					timeStampBegin = System.currentTimeMillis();
					networkManager.startAlgorithm();
					playPauseButton.setIcon(pauseImage);
					playPauseButton.removeActionListener(this);
					playPauseButton.addActionListener(pauseResumeAction);
				} // else (drawing area is not blank)
			} // action performed
		};
    playPauseButton.addActionListener(playAction);

		// Create stop button
		stopButton = new JButton(stopImage);
		stopButton.setPreferredSize(new Dimension(60,26));
		stopButton.setRequestFocusEnabled(false);
	  stopButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
				if (!timer.isRunning() && !paused) return; // no algortihm running; cannot stop
				out.println("Algorithm Aborted");
				aborted = true;
				if (paused) // then resume briefly so threads can terminate properly
					playPauseButton.doClick();
				try {
					networkManager.waitForThreadsToFinish(); // will happen quickly due abort flag
				} catch (InterruptedException ex) {}
				stopAnimation();
				networkManager.resetNodesAndLinks(); // for re-running
				clearResults();
				networkPanel.repaint();
			}
    });

    // Create clear button
		clearButton = new JButton("Clear");
	  clearButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
				if (networkManager.getNumNodes() == 0) return; // already clear
				if (!networkManager.getNetworkType().equals(getNetworkType())) return; // don't clear a network of another type that is not currenly displayed on the screen
				networkManager.clear();
				networkPanel.getDrawingArea().setIsBlank(true);
				networkPanel.getDrawingArea().setIsDirty(false);
				clearResults();
				topologyLabel.setText("");
				algorithmLabel.setText("");
				drawingPanel.repaint();
			}
    });
		final JPanel clearPanel = new JPanel();
		clearPanel.add(clearButton);
    final JPanel playOptionsPanel = new JPanel(); // the panel across the top of the network panel
		final JPanel playStopButtonsPanel = new JPanel();
		playStopButtonsPanel.add(playPauseButton);
		playStopButtonsPanel.add(stopButton);
		// Resize the play/stop buttons when the panel gets too small. Avoids them being positioned out of sight.
		playStopButtonsPanel.addComponentListener(new ComponentListener() {
			public void componentResized(ComponentEvent e) {
				int width = playStopButtonsPanel.getWidth();
				if (width > 0 && width < 135)
				{
					int newLength = (width-15)/2;
					stopButton.setPreferredSize(new Dimension(newLength, 26));
					playPauseButton.setPreferredSize(new Dimension(newLength, 26));
				}
				else
				{
					stopButton.setPreferredSize(new Dimension(60, 26));
					playPauseButton.setPreferredSize(new Dimension(60, 26));
				}
				playStopButtonsPanel.revalidate();
			}
			public void componentMoved(ComponentEvent e) {}
			public void componentShown(ComponentEvent e) {}
			public void componentHidden(ComponentEvent e) {}
		});

		// Create play options panel that goes above the drawing panel
		playOptionsPanel.setLayout(new BorderLayout());
		playOptionsPanel.add(newNetworkPanel, BorderLayout.WEST);
		playOptionsPanel.add(playStopButtonsPanel, BorderLayout.CENTER);
		playOptionsPanel.add(clearPanel, BorderLayout.EAST);

		// Layout drawing panel
		drawingPanel.add(playOptionsPanel, BorderLayout.NORTH);
		drawingPanel.add(networkPanel, BorderLayout.CENTER);
		drawingPanel.add(speedPanel, BorderLayout.SOUTH);

		// Layout menu bars
		JPanel menus = new JPanel();
		menus.setLayout(new BorderLayout());
		menus.add(menuBar, BorderLayout.NORTH);
		menus.add(toolBar, BorderLayout.CENTER);

		// Add top level elements to the content pane
		contentPane.add(menus, BorderLayout.NORTH);
		contentPane.add(innerSplitPane, BorderLayout.CENTER);
		setContentPane(contentPane);
		repaint();
	}

	private String getAlgPseudoCode(String algFileName) {
		try {
			URL theURL = null;
			try {
				theURL = new URL(getCodeBase().toString()+algFileName);
			} catch (MalformedURLException ex) {
				return "Bad URL";
			}
			StringBuffer buf = new StringBuffer();
			DataInputStream data = null;
			try {
				URLConnection conn = theURL.openConnection();
				conn.connect();
				BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
				data = new DataInputStream(bis);
		    String nextChar;
		    byte[] b = new byte[1]; // The text files are ANSI encoding, which means one byte = one character
		    while (true) {
					b[0] = data.readByte(); // Read the next character (1 byte in ANSI)
					nextChar = new String(b); // convert to String
					buf.append(nextChar);
				}
			}
			catch (EOFException exce) { // proper way reading should finish -> hits eof
					try { data.close(); } catch (IOException excep) {}
					return buf.toString();
			}
			catch (IOException exc) {
					return "IO Error:"+exc.getMessage();
			}
		}
		catch (NullPointerException e) { // read from local file system
			String fileContents = "";
			String line;
				try{
					BufferedReader in = new BufferedReader( new FileReader(algFileName));
					while( ( line = in.readLine()) != null )
						fileContents += line + "\n";
					in.close();
				}
				catch (FileNotFoundException x) {
					fileContents = algFileName + " not found";
					out.println(algFileName + " not found");
				}
				catch (IOException x) {
					out.println("IO Exception");
				}
				return fileContents;
		}
	}

	public void start() {
		// starting applet...
	}

	public void stop() {
			// stopping applet...
	}

	public void destroy() {
			// preparing applet for unloading...
	}

	static class SimpleWindowListener extends WindowAdapter {
		public void windowClosing(WindowEvent e) {
			System.exit(0);
		}
	}

  /* Start animation. Can be invoked from any thread.
   */
  public static synchronized void startAnimation() {
    if (!timer.isRunning()) {
      timer.start();
	  	setMenusEnabled(false);
      statusLabel.setText("Running");
		}
  }

  /* Stop animation. Can be invoked from any thread.
   */
  public static synchronized void stopAnimation() {
    if (timer.isRunning()) {
	  	networkManager.setIsRunning(false);
	    timer.stop();
	    networkManager.syncTimer.stop();
	    statusLabel.setText("Idle");
	  	setMenusEnabled(true);
   		playPauseButton.setIcon(playImage);
   		playPauseButton.removeActionListener(pauseResumeAction);
   		playPauseButton.addActionListener(playAction);
    }
  }

	/* Set new speed and time left for any messages on the given link.
	 * Used because link's speed has changed.
	 */
	private void adjustMessages(Link link) {
		Vector msgVector;
		double ratio, timeLeft, speed;
		long newTimeLeft;
		Message msg;
		if (isSynchronous())
			msgVector = link.getSyncMessages();
		else
			msgVector = link.getActiveMessages();
		for (int j = 0; j < msgVector.size(); j++) {
		  msg = (Message)msgVector.get(j);
		  timeLeft = msg.getTimeLeft(); // original time left
		  speed = msg.getSpeed(); // original speed
		  ratio = timeLeft/speed; // percent finished
		  if (isFIFO())
		  {
				if (isSynchronous())
					speed = Link.getUnitOfTime(); // this is the speed of all messages in a synchronous system
				else
					speed = link.getSpeed();
		  }
		  else
		    speed = link.getNewSpeed(); // generate a new random speed that is proportional to the speed slider
		  newTimeLeft = (long)(ratio*speed);
		  msg.setNewInfo(newTimeLeft, (long)speed);
		}
	}

  // Makes the animation work by repainting the screen. Called every time the timer ticks.
  public void actionPerformed(ActionEvent e) {
		networkPanel.repaint();
    executionTime = System.currentTimeMillis() - timeStampBegin - totalPausedTime;
    runningTimeLabel.setText(String.valueOf(executionTime/1000)+" seconds");
    messagesWithNLabel.setText(String.valueOf(Message.getTotalMessagesWithN()));
    messagesNoNLabel.setText(String.valueOf(Message.getTotalMessagesNoN()));
  }

	public static boolean isAborted() {
		return aborted;
	}

	public static boolean isPaused() {
		return paused;
	}

	public static JToolBar getToolBar() {
		return toolBar;
	}

	public static Component getPopupLocation() {
		return playPauseButton;
	}

	/* Must be static because it is used with the timer */
	public static NetworkPanel getNetworkPanel() {
		return networkPanel;
	}

	public static JPanel getDrawingPanel() {
		return drawingPanel;
	}

	public static Timer getTimer() {
		return timer;
	}

	public static int getSpeed() {
		return speedSlider.getValue();
	}

	public static boolean isFIFO() {
		return fifo.isSelected();
	}

	public static boolean isInstantWakeUp() {
		return instantWakeUp.isSelected();
	}

	public static void setInstantWakeUp(boolean tf) {
		instantWakeUp.setSelected(tf);
	}

	public static boolean isSynchronous() {
		return synchronous.isSelected();
	}

	public static void setSynchronous(boolean tf) {
		synchronous.setSelected(tf);
	}

	public static long getExecutionTime() {
    return executionTime;
  }

  public static String getNetworkType() {
		return (String)topologyMenu.getSelectedItem();
	}

  public static String getAlgorithm() {
		if (getNetworkType().equals("Arbitrary"))
			return (String)algorithmMenuArb.getSelectedItem();
		else
			return (String)((JComboBox)((JPanel)toolBar.getComponentAtIndex(2)).getComponent(0)).getSelectedItem();
	}

  /* Disable or enable a large group of menus.
   * Used to prevent modifications while running an algorithm.
   */
	private static void setMenusEnabled(boolean tf) {
		String netType = networkManager.getNetworkType();
		topologyMenu.setEnabled(tf);
		if (netType.equals("Arbitrary")) {
			if (tf && autoBarb.isSelected())
				newNetworkButton.setEnabled(tf);
			else if (!tf)
				newNetworkButton.setEnabled(tf);
		}
		else // not Arbitrary
			newNetworkButton.setEnabled(tf);
		if (!tf || (tf && !synchronous.isSelected()))
			fifo.setEnabled(tf);
		synchronous.setEnabled(tf);
		instantWakeUp.setEnabled(tf);
		clearButton.setEnabled(tf);
		if (netType.equals("Ring"))
			casesMenu.setEnabled(tf);
		if (netType.equals("Arbitrary"))
			autoBarb.setEnabled(tf);
		if (netType.equals("Tree"))
			autoBtree.setEnabled(tf);
		((JPanel)toolBar.getComponentAtIndex(2)).getComponent(0).setEnabled(tf); // algorithm selector
	}

	public void clearResults() {
    statusLabel.setText("Idle");
    runningTimeLabel.setText("0");
    messagesWithNLabel.setText("0");
    messagesNoNLabel.setText("0");
		clockTicksLabel.setText("0");
	}

	public static void updateClockTick(int tick) {
		clockTicksLabel.setText(String.valueOf(tick));
	}

  public static void main(String[] args){
  	NetViewer applet = new NetViewer();
  	applet.init();
  	applet.start();
  	MinSizeFrame aFrame = new MinSizeFrame("NetViewer");
  	aFrame.addWindowListener(new SimpleWindowListener());
  	aFrame.add(applet, BorderLayout.CENTER);
  	aFrame.setVisible(true);
  	// maximize window
  	if (javaVersion < 4)
  		aFrame.setSize(java.awt.Toolkit.getDefaultToolkit().getScreenSize().width, java.awt.Toolkit.getDefaultToolkit().getScreenSize().height-30);
		else
		 	aFrame.setExtendedState(Frame.MAXIMIZED_BOTH);
  	aFrame.setIconImage(netViewerIcon.getImage());
  	aFrame.setVisible(true);
		applet.numNodesField.requestFocus(); // place cursor in num nodes box for the ring
		applet.innerSplitPane.setDividerLocation(0.65);
  }
}
