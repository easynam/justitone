package justitone.audio;

import justitone.jidi.JidiSequence;

public abstract class Message {
    public static class SetSequence extends Message {
        public JidiSequence sequence;
        public SetSequence(JidiSequence sequence) {
            this.sequence = sequence;
        }
    }
    public static class Play extends Message {}
    public static class Stop extends Message {}
}