package justitone.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

import justitone.audio.Playback;
import justitone.audio.Message;
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
        ConcurrentLinkedQueue<Message> queue = new ConcurrentLinkedQueue<>();
        
        Playback playback = new Playback(queue);
        
        new Thread(playback).start();

        textArea = new JTextArea("120: 3/2[* [:1 :2 :3] ]");
        textArea.setFont(new Font("monospaced", Font.PLAIN, 12));

        JCheckBox midiPlayback = new JCheckBox("midi");

        JButton play = new JButton("play");
        play.addActionListener(a -> {
            if (midiPlayback.isSelected()) {
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
                                        setHighlights(highlighter, sequencer.getTickPosition(), jidiSeq);
                                        
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
            } else {
                queue.add(new Message.Stop());
                queue.add(new Message.SetSequence(new JidiSequence(reader.parse(textArea.getText()), 768)));
                queue.add(new Message.SetTick(0));
                queue.add(new Message.Play());
            }
        });
        JButton playFromCursor = new JButton("play from cursor");
        playFromCursor.addActionListener(a -> {
            int cursorPos = textArea.getCaretPosition();
         
            JidiSequence sequence = new JidiSequence(reader.parse(textArea.getText()), 768);
            
            JidiEvent.Token event = 
                    sequence.tracks.stream()
                                   .flatMap(t -> t.events.stream() .filter(e -> e instanceof JidiEvent.Token))
                                   .map(e -> (JidiEvent.Token) e)
                                   .filter(e -> e.start() < cursorPos)
                                   .max((e1, e2) -> (int) (e1.start() - e2.start()))
                                   .orElse(null);
            
            long tick = 0;
            
            if (event != null) tick = event.tick;
            
            queue.add(new Message.Stop());
            queue.add(new Message.SetSequence(new JidiSequence(reader.parse(textArea.getText()), 768)));
            queue.add(new Message.SetTick(tick));
            queue.add(new Message.Play());
        });
        
        JButton stop = new JButton("stop");
        stop.addActionListener(a -> {
            try {
                queue.add(new Message.Stop());
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        });

        JButton exportMidi = new JButton("export midi");
        exportMidi.addActionListener(a -> {
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
        JButton exportAudio = new JButton("export audio");
        exportAudio.addActionListener(a -> {
            // todo
        });

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.add(play);
        toolbar.add(playFromCursor);
        toolbar.add(stop);
        toolbar.add(midiPlayback);
        toolbar.addSeparator();
        toolbar.add(exportMidi);
        toolbar.add(exportAudio);

        setLayout(new BorderLayout());
        add(BorderLayout.NORTH, toolbar);
        add(BorderLayout.CENTER, textArea);
        
        Runnable watchAudio = () -> {
            Highlighter highlighter = textArea.getHighlighter();
            
            while(true) {
                if(playback.isRunning()) {
                    try {
                        if (playback.isPlaying()) setHighlights(highlighter, playback.getTick(), playback.getSequence());
                        
                        Thread.sleep(1000/60);
                    } catch(InterruptedException ignore) {
                        break;
                    }
                } else {
                    break;
                }
            }
        };
        
        new Thread(watchAudio).start();
    }
    
    
    
    public void setHighlights(Highlighter highlighter, long tick, JidiSequence jidiSeq) {
        List<TokenPos> tokens = new ArrayList<>();
        
        for (JidiTrack track : jidiSeq.tracks) {
            List<TokenPos> last = Collections.emptyList();
            
            for (JidiEvent event : track.events) {
                if (event.tick <= tick) {
                    
                    if (event instanceof JidiEvent.Token) {
                        last = ((JidiEvent.Token) event).tokens;
                    }
                }
                else break;
            }
            
            tokens.addAll(last);
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
