package justitone.jidi;

import java.util.ArrayList;
import java.util.List;

public class JidiTrack {
    public List<JidiEvent> events;
    public int id;
    
    public JidiTrack(int id) {
        this.events = new ArrayList<>();
        this.id = id;
    }
    
    public void add(JidiEvent e) {
        System.out.println("track "+id+":"+e);
        
        events.add(e);
    }
}
