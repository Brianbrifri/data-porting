import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;


public class MainForm implements TextAreaUpdater{

	Parser parser;

	private JFrame frmDataPorting;
	private JTextField dataPathTextBox;
	private JTextField sqlUserTextBox;
	private JLabel sqlCredLabel;
	private JLabel lblUser;
	private JLabel lblPass;
	private JTextField sqlPassTextBox;
	private JTextArea outputTextArea;
	private final Action action = new SwingAction();
	private JButton btnConvert;
	private final Action action_1 = new SwingAction_1();

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(() -> {
            try {
                MainForm window = new MainForm();
                window.frmDataPorting.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
	}

	/**
	 * Create the application.
	 */
	private MainForm() throws IOException {
		initialize();
		outputTextArea.append("Welcome! Please enter your credentials\nand choose a file or directory to convert\n");
		outputTextArea.append("If you have previously ran this program from this directory\n");
        outputTextArea.append("please be sure to save your error.sql file to another name or dir\n");
        outputTextArea.append("as it will be overwritten if you convert again without doing so\n");
	}

    @Override
    public void updateLog(String string) {
	    outputTextArea.append(string);
	    outputTextArea.setCaretPosition(outputTextArea.getDocument().getLength());
	    outputTextArea.update(outputTextArea.getGraphics());
    }

    /**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmDataPorting = new JFrame();
		frmDataPorting.setTitle("Data Porting");
		frmDataPorting.setBounds(100, 100, 320, 384);
		frmDataPorting.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmDataPorting.getContentPane().setLayout(null);
		frmDataPorting.setResizable(false);

		dataPathTextBox = new JTextField();
		dataPathTextBox.setBounds(55, 98, 195, 20);
		frmDataPorting.getContentPane().add(dataPathTextBox);
		dataPathTextBox.setColumns(10);
		
		JLabel dataPathLabel = new JLabel("Path:");
		dataPathLabel.setFont(new Font("Tahoma", Font.BOLD, 11));
		dataPathLabel.setBounds(10, 101, 35, 14);
		frmDataPorting.getContentPane().add(dataPathLabel);
		
		JPanel sqlPanel = new JPanel();
		sqlPanel.setBorder(new LineBorder(new Color(0, 0, 0)));
		sqlPanel.setBounds(10, 11, 285, 76);
		frmDataPorting.getContentPane().add(sqlPanel);
		sqlPanel.setLayout(null);
				
		sqlCredLabel = new JLabel("SQL Credentials");
		sqlCredLabel.setBounds(75, 11, 124, 21);
		sqlPanel.add(sqlCredLabel);
		sqlCredLabel.setFont(new Font("Dialog", Font.BOLD, 16));
		
		lblUser = new JLabel("User:");
		lblUser.setBounds(10, 46, 41, 14);
		sqlPanel.add(lblUser);
		lblUser.setFont(new Font("Tahoma", Font.BOLD, 11));
		
		sqlUserTextBox = new JTextField();
		sqlUserTextBox.setBounds(48, 43, 86, 20);
		sqlPanel.add(sqlUserTextBox);
		sqlUserTextBox.setColumns(10);
		
		lblPass = new JLabel("Pass:");
		lblPass.setBounds(144, 46, 41, 14);
		sqlPanel.add(lblPass);
		lblPass.setFont(new Font("Tahoma", Font.BOLD, 11));

		sqlPassTextBox = new JPasswordField();
		sqlPassTextBox.setBounds(182, 43, 86, 20);
		sqlPanel.add(sqlPassTextBox);
		sqlPassTextBox.setColumns(10);
		
		JScrollPane outputScrollPane = new JScrollPane();
		outputScrollPane.setBounds(10, 129, 284, 113);
		outputScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		outputScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		frmDataPorting.getContentPane().add(outputScrollPane);
		
		outputTextArea = new JTextArea(5, 20);
		outputScrollPane.setViewportView(outputTextArea);
		outputTextArea.setText("");
		outputTextArea.setEditable(false);

        try {
            parser = new Parser(outputTextArea);
        } catch (IOException e) {
            e.printStackTrace();
        }

        parser.setListener(this);

        JButton btnBrowse = new JButton("...");
		btnBrowse.setAction(action);
		btnBrowse.setBounds(260, 97, 35, 23);
		frmDataPorting.getContentPane().add(btnBrowse);
		
		btnConvert = new JButton("Convert");

		btnConvert.setAction(action_1);
		btnConvert.setBounds(108, 253, 89, 23);
		frmDataPorting.getContentPane().add(btnConvert);
	}
	private class SwingAction extends AbstractAction {
		JFileChooser chooser = new JFileChooser();
		
		SwingAction() {
			putValue(NAME, "SwingAction");
			putValue(SHORT_DESCRIPTION, "Browse Files");
		}
		public void actionPerformed(ActionEvent e) {
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			if (chooser.showOpenDialog(frmDataPorting) == JFileChooser.APPROVE_OPTION) {
				dataPathTextBox.setText(chooser.getSelectedFile().getAbsolutePath());
			}			
		}
	}
	private class SwingAction_1 extends AbstractAction {
		SwingAction_1() {
			putValue(NAME, "Convert");
			putValue(SHORT_DESCRIPTION, "Start converting your files to the DB");
		}
		public void actionPerformed(ActionEvent e) {
			// Do some converting!
			System.out.println(dataPathTextBox.getText());
			try {
				StringBuilder usr = new StringBuilder(sqlUserTextBox.getText());
				StringBuilder pswd = new StringBuilder(sqlPassTextBox.getText());

				//**Hard code in correct credentials here**
				String correctUsr = "umsl_cs_team";
				String correctPswd = "!kmcR0ck5";
				if(usr.toString().equals(correctUsr) && pswd.toString().equals(correctPswd)) {
					outputTextArea.append("Credentials correct\n");
					parser.Parse(dataPathTextBox.getText(), usr, pswd);
				}
				else {
					outputTextArea.append("Please enter correct credentials\n");
					outputTextArea.append("for the database: lacuna.dhcp.umsl.edu\n");
				}
				usr.setLength(0);
				pswd.setLength(0);
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
}
