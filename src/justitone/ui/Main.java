package justitone.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.io.File;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import justitone.Song;
import justitone.jidi.JidiSequence;
import justitone.midi.Midi;
import justitone.parser.Reader;

public class Main extends JPanel {
    private static final long serialVersionUID = 8128299707315843231L;

    JTextArea textArea;

    public Main(Reader reader) {
        textArea = new JTextArea("120: [* :1 ]");
        textArea.setFont(new Font("monospaced", Font.PLAIN, 12));

        JButton play = new JButton("Play");
        play.addActionListener(a -> {
            try {
                Song song = reader.parse(textArea.getText());
                
                JidiSequence seq = new JidiSequence(song, 768);
                
                Midi midi = new Midi();
                
                Sequence midiSeq = midi.jidiToMidi(seq, 2f);

                Sequencer sequencer = MidiSystem.getSequencer();
                if (sequencer == null) {
                    System.err.println("Sequencer device not supported");
                    return;
                }

                sequencer.setSequence(midiSeq);
                sequencer.open();
                sequencer.start();
                while(true) {
                    if(sequencer.isRunning()) {
                        try {
                            Thread.sleep(100);
                        } catch(InterruptedException ignore) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
                sequencer.stop();
                sequencer.close();
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        });

        JButton save = new JButton("save");
        save.addActionListener(a -> {
            try {
                Song song = reader.parse(textArea.getText());
                
                JidiSequence seq = new JidiSequence(song, 768);
                
                Midi midi = new Midi();
                
                Sequence midiSeq = midi.jidiToMidi(seq, 2f);
                
                File f = new File("midifile.mid");
                MidiSystem.write(midiSeq, 1, f);
            } catch (Exception e) {
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
        frame.setSize(800, 600);
        frame.setContentPane(new Main(new Reader()));
        frame.setVisible(true);
    }
}
