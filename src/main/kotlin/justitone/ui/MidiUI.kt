package justitone.ui

import justitone.TokenPos
import justitone.audio.Message
import justitone.audio.Playback
import justitone.jidi.JidiEvent
import justitone.jidi.JidiSequence
import justitone.jidi.JidiTrack
import justitone.midi.Midi
import justitone.parser.Reader
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import javax.sound.midi.MidiSystem
import javax.sound.midi.MidiUnavailableException
import javax.swing.*
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultHighlighter
import javax.swing.text.Highlighter
import kotlin.collections.HashSet


class MidiUI(reader: Reader) : JPanel() {

    internal var textArea: JTextArea

    private val highlights = ArrayList<Any>()
    private val playing = DefaultHighlighter.DefaultHighlightPainter(Color.orange)

    init {
        val queue = ConcurrentLinkedQueue<Message>()

        val playback = Playback(queue)

        Thread(playback).start()

        textArea = JTextArea("120: /4[[:1 '3 :1, '5 '6 '5] ['6, '3][:1 :2 :3] /8[:1 :2 :1 :2 :1 :2 :1 :2]['6, '3][:1 :2 :3]]")
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
                    .filter {
                        it.tokens.any {
                            token -> token.start <= cursorPos && token.stop >= cursorPos
                        }
                    }
                    .minBy { it.tick }

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

    private fun setHighlights(highlighter: Highlighter, tick: Long, jidiSeq: JidiSequence) {
        val tokens = ArrayList<TokenPos>()

        for (track in jidiSeq.tracks) {
            var last = emptySet<TokenPos>()

//            println("a track")

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
//        println("done")

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
