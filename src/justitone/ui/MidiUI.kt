package justitone.ui

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import java.io.File
import java.util.ArrayList
import java.util.Collections
import java.util.concurrent.ConcurrentLinkedQueue

import javax.sound.midi.MidiSystem
import javax.sound.midi.MidiUnavailableException
import javax.sound.midi.Sequence
import javax.sound.midi.Sequencer
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JToolBar
import javax.swing.ScrollPaneConstants
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultHighlighter
import javax.swing.text.Highlighter
import javax.swing.text.Highlighter.HighlightPainter

import justitone.audio.*
import justitone.Song
import justitone.TokenPos
import justitone.jidi.JidiEvent
import justitone.jidi.JidiSequence
import justitone.jidi.JidiTrack
import justitone.midi.Midi
import justitone.parser.Reader


class MidiUI(reader: Reader) : JPanel() {

    internal var textArea: JTextArea

    private val highlights = ArrayList<Any>()
    private val playing = DefaultHighlighter.DefaultHighlightPainter(Color.orange)

    init {
        val queue = ConcurrentLinkedQueue<Message>()

        val playback = Playback(queue)

        Thread(playback).start()

        textArea = JTextArea("(def m3 '6) (def p5 '3) 120: [:1 m3 p5]")
        textArea.font = Font("monospaced", Font.PLAIN, 12)

        val scroll = JScrollPane(textArea)
        scroll.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS

        val midiPlayback = JCheckBox("midi")

        val play = JButton("play")
        play.addActionListener {
            if (midiPlayback.isSelected) {
                play.isEnabled = false
                try {
                    val song = reader.parse(textArea.text)
                    val jidiSeq = JidiSequence(song, 768)
                    val midi = Midi()
                    val midiSeq = midi.jidiToMidi(jidiSeq, 2f)

                    val sequencer = MidiSystem.getSequencer()
                    if (sequencer == null) {
                        System.err.println("Sequencer device not supported")
                        return@addActionListener
                    }

                    val highlighter = textArea.highlighter

                    sequencer.sequence = midiSeq

                    val watchMidi = {
                        try {
                            sequencer.open()
                            sequencer.start()

                            while (true) {
                                if (sequencer.isRunning) {
                                    try {
                                        setHighlights(highlighter, sequencer.tickPosition, jidiSeq)

                                        Thread.sleep((1000 / 60).toLong())
                                    } catch (ignore: InterruptedException) {
                                        break
                                    }

                                } else {
                                    break
                                }
                            }

                            highlighter.removeAllHighlights()

                            sequencer.stop()
                            sequencer.close()
                        } catch (e: MidiUnavailableException) {
                            // TODO Auto-generated catch block
                            e.printStackTrace()
                        }
                    }

                    Thread(watchMidi).start()
                } catch (e: Exception) {
                    e.printStackTrace(System.out)
                }

                play.isEnabled = true
            } else {
                queue.add(Message.Stop)
                queue.add(Message.SetSequence(JidiSequence(reader.parse(textArea.text), 768)))
                queue.add(Message.SetTick(0))
                queue.add(Message.Play)
            }
        }
        val playFromCursor = JButton("play from cursor")
        playFromCursor.addActionListener {
            val cursorPos = textArea.caretPosition

            val sequence = JidiSequence(reader.parse(textArea.text), 768)

            val event = sequence.tracks
                    .flatMap { t -> t.events.filter { it is JidiEvent.Token } }
                    .map { it as JidiEvent.Token }
                    .filter { it.start() < cursorPos }
                    .maxBy { it.start() }

            val tick = event?.tick ?: 0

            queue.add(Message.Stop)
            queue.add(Message.SetSequence(JidiSequence(reader.parse(textArea.text), 768)))
            queue.add(Message.SetTick(tick))
            queue.add(Message.Play)
        }

        val stop = JButton("stop")
        stop.addActionListener {
            queue.add(Message.Stop)
        }

        val exportMidi = JButton("export midi")
        exportMidi.addActionListener {
            try {
                val song = reader.parse(textArea.text)

                val seq = JidiSequence(song, 768)

                val midi = Midi()

                val midiSeq = midi.jidiToMidi(seq, 2f)

                val f = File("midifile.mid")
                MidiSystem.write(midiSeq, 1, f)
            } catch (e: Exception) {
                e.printStackTrace(System.out)
            }
        }
        val exportAudio = JButton("export audio")
        exportAudio.addActionListener {
            // todo
        }

        val toolbar = JToolBar()
        toolbar.isFloatable = false
        toolbar.add(play)
        toolbar.add(playFromCursor)
        toolbar.add(stop)
        toolbar.add(midiPlayback)
        toolbar.addSeparator()
        toolbar.add(exportMidi)
        toolbar.add(exportAudio)

        layout = BorderLayout()
        add(BorderLayout.NORTH, toolbar)
        add(BorderLayout.CENTER, scroll)

        val watchAudio = {
            val highlighter = textArea.highlighter

            while (true) {
                if (playback.isRunning) {
                    try {
                        if (playback.isPlaying) setHighlights(highlighter, playback.tick, playback.sequence)

                        Thread.sleep((1000 / 60).toLong())
                    } catch (ignore: InterruptedException) {
                        break
                    }

                } else {
                    break
                }
            }
        }

        Thread(watchAudio).start()
    }

    private fun setHighlights(highlighter: Highlighter, tick: Long, jidiSeq: JidiSequence?) {
        val tokens = ArrayList<TokenPos>()

        for (track in jidiSeq!!.tracks) {
            var last = emptyList<TokenPos>()

            for (event in track.events) {
                if (event.tick <= tick) {

                    if (event is JidiEvent.Token) {
                        last = event.tokens
                    }
                } else
                    break
            }

            tokens.addAll(last)
        }

        highlights.forEach { h -> highlighter.removeHighlight(h) }

        highlights.clear()

        for (token in tokens) {
            try {
                highlights.add(highlighter.addHighlight(token.start, token.stop + 1, playing))
            } catch (e: BadLocationException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }

        }
    }

    companion object {
        private val serialVersionUID = 8128299707315843231L

        @JvmStatic
        fun main(args: Array<String>) {
            val frame = JFrame("justitone")
            frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            frame.setSize(800, 600)
            frame.contentPane = MidiUI(Reader())
            frame.isVisible = true
        }
    }
}
