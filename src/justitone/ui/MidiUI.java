package justitone.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

import justitone.Song;
import justitone.TokenPos;
import justitone.jidi.JidiEvent;
import justitone.jidi.JidiSequence;
import justitone.jidi.JidiTrack;
import justitone.midi.Midi;
import justitone.parser.Reader;

public class MidiUI extends JPanel {
    private static final long serialVersionUID = 8128299707315843231L;

    JTextArea textArea;

    public MidiUI(Reader reader) {
        textArea = new JTextArea("120: 3/2[* [:1 :2 :3] ]");
        textArea.setFont(new Font("monospaced", Font.PLAIN, 12));

        JButton play = new JButton("Play");
        play.addActionListener(a -> {
            play.setEnabled(false);
            try {
                Song song = reader.parse(textArea.getText());
                
                JidiSequence jidiSeq = new JidiSequence(song, 768);
                
                Midi midi = new Midi();
                
                Sequence midiSeq = midi.jidiToMidi(jidiSeq, 2f);

                Sequencer sequencer = MidiSystem.getSequencer();
                if (sequencer == null) {
                    System.err.println("Sequencer device not supported");
                    return;
                }
                
                Highlighter highlighter = textArea.getHighlighter();

                sequencer.setSequence(midiSeq);
                
                Runnable watchMidi = () -> {
                    try {
                        sequencer.open();
                        sequencer.start();
                        
                        while(true) {
                            if(sequencer.isRunning()) {
                                try {
                                    setHighlights(highlighter, sequencer, jidiSeq);
                                    
                                    Thread.sleep(1000/60);
                                } catch(InterruptedException ignore) {
                                    break;
                                }
                            } else {
                                break;
                            }
                        }
                        
                        highlighter.removeAllHighlights();
                        
                        sequencer.stop();
                        sequencer.close();
                    } catch (MidiUnavailableException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                };
                
                new Thread(watchMidi).start();
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }

            play.setEnabled(true);
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
    
    public void setHighlights(Highlighter highlighter, Sequencer sequencer, JidiSequence jidiSeq) {
        long tick = sequencer.getTickPosition();
        
        List<TokenPos> tokens = new ArrayList<>();
        
        for (JidiTrack track : jidiSeq.tracks) {
            TokenPos last = null;
            
            for (JidiEvent event : track.events) {
                if (event.tick <= tick) {
                    
                    if (event instanceof JidiEvent.NoteOff) {
                        last = null;
                    }
                    else {
                        last = event.tokenPos;
                    }
                }
                else break;
            }
            
            if (last != null)
              tokens.add(last);
        }
        
        highlighter.removeAllHighlights();
        
        for (TokenPos token : tokens) {
            try {
                highlighter.addHighlight(token.start, token.stop+1, DefaultHighlighter.DefaultPainter);
            } catch (BadLocationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static void main(String args[]) {
        JFrame frame = new JFrame("justitone");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setContentPane(new MidiUI(new Reader()));
        frame.setVisible(true);
    }
}
