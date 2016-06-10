package beatboxapp;

import static com.sun.org.apache.xerces.internal.util.PropertyState.is;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.security.AccessController;
import java.util.*;
import javax.sound.midi.*;
import javax.swing.*;
import sun.awt.OSInfo;

public class BeatBoxApp {

    JPanel mainPanel;
    ArrayList<JCheckBox> checkboxList;
    Sequencer sequencer;
    Sequence sequence;
    Track track;
    JFrame theFrame;

    String[] instrumentNames = {"Bass Drum", "Closed Hi-Hat", "Open Hi-Hat",
        "Acoustic Snare", "Crash Cymbal", "Hand Clap", "High Tom", "Hi Bongo",
        "Maracas", "Whistle", "Low Conga", "Cowbell", "Vibraslap", "Low-mid Tom",
        "High Agogo", "Open Hi Conga"};
    
    int[] instruments = {35,42,46,38,49,39,50,60,70,72,64,56,58,47,67,63};

    public static void main(String[] args)
    {
        new BeatBoxApp().buildGUI();
    }

    public void buildGUI()
    {
        try{
            OSInfo.OSType osType = AccessController.doPrivileged(OSInfo.getOSTypeAction());
            if (osType == OSInfo.OSType.LINUX)
            {
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.GTK.GTKLookAndFeel");
            }
            else
            {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }   
        }catch(Exception ex){
            ex.printStackTrace();
        }
        
        theFrame = new JFrame("Cyber BeatBox");
        theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel background = new JPanel(new BorderLayout());
        background.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        checkboxList = new ArrayList<JCheckBox>();
        Box buttonBox = new Box(BoxLayout.Y_AXIS);
        
        JButton start = new JButton("Start");
        start.addActionListener(new MyStartListener());
        buttonBox.add(start);
        
        JButton stop = new JButton("Stop");
        stop.addActionListener(new MyStopListener());
        buttonBox.add(stop);
        
        JButton clear = new JButton("Clear");
        clear.addActionListener(new MyClearListener());
        buttonBox.add(clear);
        
        JButton upTempo = new JButton("Tempo Up");
        upTempo.addActionListener(new MyUpTempoListener());
        buttonBox.add(upTempo);
        
        JButton downTempo = new JButton("Tempo Down");
        downTempo.addActionListener(new MyDownTempoListener());
        buttonBox.add(downTempo);
        
        JButton serialize = new JButton("Save");
        serialize.addActionListener(new MySaveListener());
        buttonBox.add(serialize);
        
        JButton restore = new JButton("Restore");
        restore.addActionListener(new MyRestoreListener());
        buttonBox.add(restore);
        
        Box nameBox = new Box(BoxLayout.Y_AXIS);
        for (int i = 0; i < 16; i++){
            nameBox.add(new Label(instrumentNames[i]));
        }
        
        background.add(BorderLayout.EAST, buttonBox);
        background.add(BorderLayout.WEST, nameBox);
        
        theFrame.getContentPane().add(background);
        
        GridLayout grid = new GridLayout(16,16);
        grid.setVgap(1);
        grid.setHgap(2);
        mainPanel = new JPanel(grid);
        background.add(BorderLayout.CENTER, mainPanel);
        
        MyCheckBoxListener listener = new MyCheckBoxListener();
        for (int i = 0; i < 256; i++)
        {
            JCheckBox c = new JCheckBox();
            c.addActionListener(listener);
            c.setSelected(false);
            checkboxList.add(c);
            mainPanel.add(c);
        }
        
        setUpMidi();
        
        theFrame.setBounds(100,100,300,300);
        theFrame.setResizable(false);
        theFrame.pack();
        theFrame.setVisible(true);
    }
    
    public void setUpMidi()
    {
        try 
        {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequence = new Sequence(Sequence.PPQ,4);
            track = sequence.createTrack();
            sequencer.setTempoInBPM(120);
        }catch(Exception e){e.printStackTrace();}
    }
    
    public void buildTrackAndStart()
    {
        int[] trackList = null;
        
        sequence.deleteTrack(track);
        track = sequence.createTrack();
        
        for (int i = 0; i < 16; i++)
        {
            trackList = new int[16];
            int key = instruments[i];
            
            for (int j = 0; j < 16; j++)
            {
                JCheckBox jc = checkboxList.get(j + (16*i));
                if (jc.isSelected())
                {
                    trackList[j] = key;
                }else
                {
                    trackList[j] = 0;
                }
            }
            makeTracks(trackList);
            track.add(makeEvent(176,1,127,0,16));
        }
        
        track.add(makeEvent(192,9,1,0,15));
        try
        {
            sequencer.setSequence(sequence);
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
            sequencer.start();
            sequencer.setTempoInBPM(120);
        }catch(Exception ex){ex.printStackTrace();}
    }
    
     public class MyStartListener implements ActionListener 
     {
        public void actionPerformed(ActionEvent e)
        {
            buildTrackAndStart();
        }
     }
     
     public class MyStopListener implements ActionListener 
     {
        public void actionPerformed(ActionEvent e)
        {
            sequencer.stop();
        }
     }
     
     public class MyUpTempoListener implements ActionListener 
     {
        public void actionPerformed(ActionEvent e)
        {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float)(tempoFactor * 1.03));
        }
     }
     
     public class MyDownTempoListener implements ActionListener 
     {
        public void actionPerformed(ActionEvent e)
        {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float)(tempoFactor * 0.97));
        }
     }
     
     public class MyClearListener implements ActionListener 
     {
        public void actionPerformed(ActionEvent e)
        {
            sequencer.stop();
            if (!checkboxList.isEmpty())
            {
                for (JCheckBox cb : checkboxList)
                {
                    cb.setSelected(false);
                }
            }
        }
     }
     
     public class MyCheckBoxListener implements ActionListener 
     {
        public void actionPerformed(ActionEvent e)
        {
            if (sequencer.isRunning())
            {
                buildTrackAndStart();
            }
        }
     } 
     
     public class MySaveListener implements ActionListener 
     {
        public void actionPerformed(ActionEvent e)
        {
            
            boolean[] checkboxState = new boolean[256];
            
            for (int i = 0; i < 256; i++)
            {
                checkboxState[i] = checkboxList.get(i).isSelected();
            }
            
            try 
            {
                FileOutputStream fileStream = new FileOutputStream(new File("BeatBox.ser"));
                try (ObjectOutputStream os = new ObjectOutputStream(fileStream))
                {  
                    os.writeObject(checkboxState);
                }
            }catch(Exception ex)
            {
                ex.printStackTrace();
            }
        }
     }
     
     public class MyRestoreListener implements ActionListener 
     {
        public void actionPerformed(ActionEvent e)
        {
            boolean[] checkboxState = null;
            
            try 
            {
                FileInputStream fileIn = new FileInputStream(new File("BeatBox.ser"));
                ObjectInputStream is = new ObjectInputStream(fileIn);
                checkboxState = (boolean[]) is.readObject();
                is.close();
            }catch(Exception ex)
            {
                ex.printStackTrace();
            }
            
            if (checkboxState.length != 0)
            {
                for (int i = 0; i < 256; i++)
                {
                    checkboxList.get(i).setSelected(checkboxState[i]);
                }
            }
            sequencer.stop();
        }
     }
     
     public void makeTracks(int[] list) 
     {
         for (int i = 0; i < 16; i++)
         {
             int key = list[i];
             if (key != 0)
             {
                 track.add(makeEvent(144,9,key,100,i));
                 track.add(makeEvent(128,9,key,100,i+1));
             }
         }
     }
     
     public static MidiEvent makeEvent(int comd, int chan, int one, int two, int tick)
     {
        MidiEvent event = null;
        try 
        {
            ShortMessage a = new ShortMessage();
            a.setMessage(comd, chan, one, two);
            event = new MidiEvent(a, tick);
        }catch(Exception ex) {ex.printStackTrace();}
        return event;
    }
    
}
