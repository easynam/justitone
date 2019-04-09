package justitone.ui;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import justitone.Playback;
import justitone.parser.Reader;

public class Main extends JPanel {
	private static final long serialVersionUID = 8128299707315843231L;

	JTextArea textArea;
	JButton play;
	
	public Main(Reader reader) {
	       textArea = new JTextArea("instrument: [>0 >20 >36 >60 >90 - _ >20] >0");
	       textArea.setFont(new Font("monospaced", Font.PLAIN, 12));
	       
	       play = new JButton("Play");
	       play.addActionListener(a -> {
	    	   try {
	    		   Playback.play(reader.parse(textArea.getText()));
	    	   }
	    	   catch (Exception e) {
	    		   e.printStackTrace(System.out);
	    	   }
	       });
	       
	       JButton save = new JButton("save");
	       save.addActionListener(a -> { 
	    	   try {
	    		   Playback.save(reader.parse(textArea.getText()));
	    	   }
	    	   catch (Exception e) {
	    		   e.printStackTrace(System.out);
		       }
	       });
	       
	       setLayout(new BorderLayout());
	       add(BorderLayout.SOUTH, play);
	       add(BorderLayout.NORTH, save);
	       add(BorderLayout.CENTER, textArea);
	}
    
    public static void main(String args[]) {
	       JFrame frame = new JFrame("justitone");
	       frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	       frame.setSize(800,600);
	       frame.setContentPane(new Main(new Reader()));
	       frame.setVisible(true);
    }
}
