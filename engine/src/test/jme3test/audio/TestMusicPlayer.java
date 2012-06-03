/*
 * Copyright (c) 2009-2010 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package jme3test.audio;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.audio.*;
import com.jme3.audio.AudioNode.Status;
import com.jme3.audio.plugins.OGGLoader;
import com.jme3.audio.plugins.WAVLoader;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;
import java.io.*;
import javax.swing.JFileChooser;

public class TestMusicPlayer extends javax.swing.JFrame {

    private AudioRenderer ar;
    private AudioData musicData;
    private AudioNode musicSource;
    private float musicLength = 0;
    private float curTime = 0;
    private Listener listener = new Listener();

    public TestMusicPlayer() {
        initComponents();
        setLocationRelativeTo(null);
        initAudioPlayer();
    }

    private void initAudioPlayer(){
        AppSettings settings = new AppSettings(true);
        settings.setRenderer(null); // disable rendering
        settings.setAudioRenderer("LWJGL");
        ar = JmeSystem.newAudioRenderer(settings);
        ar.initialize();
        ar.setListener(listener);
        AudioContext.setAudioRenderer(ar);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pnlButtons = new javax.swing.JPanel();
        sldVolume = new javax.swing.JSlider();
        btnRewind = new javax.swing.JButton();
        btnStop = new javax.swing.JButton();
        btnPlay = new javax.swing.JButton();
        btnFF = new javax.swing.JButton();
        btnOpen = new javax.swing.JButton();
        pnlBar = new javax.swing.JPanel();
        lblTime = new javax.swing.JLabel();
        sldBar = new javax.swing.JSlider();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        pnlButtons.setLayout(new javax.swing.BoxLayout(pnlButtons, javax.swing.BoxLayout.LINE_AXIS));

        sldVolume.setMajorTickSpacing(20);
        sldVolume.setOrientation(javax.swing.JSlider.VERTICAL);
        sldVolume.setPaintTicks(true);
        sldVolume.setValue(100);
        sldVolume.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sldVolumeStateChanged(evt);
            }
        });
        pnlButtons.add(sldVolume);

        btnRewind.setText("<<");
        pnlButtons.add(btnRewind);

        btnStop.setText("[  ]");
        btnStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStopActionPerformed(evt);
            }
        });
        pnlButtons.add(btnStop);

        btnPlay.setText("II / >");
        btnPlay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPlayActionPerformed(evt);
            }
        });
        pnlButtons.add(btnPlay);

        btnFF.setText(">>");
        btnFF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFFActionPerformed(evt);
            }
        });
        pnlButtons.add(btnFF);

        btnOpen.setText("Open ...");
        btnOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenActionPerformed(evt);
            }
        });
        pnlButtons.add(btnOpen);

        getContentPane().add(pnlButtons, java.awt.BorderLayout.CENTER);

        pnlBar.setLayout(new javax.swing.BoxLayout(pnlBar, javax.swing.BoxLayout.LINE_AXIS));

        lblTime.setText("0:00-0:00");
        lblTime.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 3, 3, 3));
        pnlBar.add(lblTime);

        sldBar.setValue(0);
        sldBar.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sldBarStateChanged(evt);
            }
        });
        pnlBar.add(sldBar);

        getContentPane().add(pnlBar, java.awt.BorderLayout.PAGE_START);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenActionPerformed
        JFileChooser chooser = new JFileChooser();
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setDialogTitle("Select OGG file");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION){
            btnStopActionPerformed(null);
            
            final File selected = chooser.getSelectedFile();
            AssetLoader loader = null;
            if(selected.getName().endsWith(".wav")){
                loader = new WAVLoader();
            }else{
                loader = new OGGLoader();
            }
             
            AudioKey key = new AudioKey(selected.getName(), true, true);
            try{
                musicData = (AudioData) loader.load(new AssetInfo(null, key) {
                    @Override
                    public InputStream openStream() {
                        try{
                            return new FileInputStream(selected);
                        }catch (FileNotFoundException ex){
                            ex.printStackTrace();
                        }
                        return null;
                    }
                });
            }catch (IOException ex){
                ex.printStackTrace();
            }

            musicSource = new AudioNode(musicData, key);
            musicLength = musicData.getDuration();
            updateTime();
        }
    }//GEN-LAST:event_btnOpenActionPerformed

    private void updateTime(){
        int max = (int) (musicLength * 100);
        int pos = (int) (curTime * 100);
        sldBar.setMaximum(max);
        sldBar.setValue(pos);

        int minutesTotal = (int) (musicLength / 60);
        int secondsTotal = (int) (musicLength % 60);
        int minutesNow = (int) (curTime / 60);
        int secondsNow = (int) (curTime % 60);
        String txt = String.format("%01d:%02d-%01d:%02d", minutesNow, secondsNow,
                                                      minutesTotal, secondsTotal);
        lblTime.setText(txt);
    }

    private void btnPlayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPlayActionPerformed
        if (musicSource == null){
            btnOpenActionPerformed(evt);
            return;
        }

        if (musicSource.getStatus() == Status.Playing){
            musicSource.setPitch(1);
            ar.pauseSource(musicSource);
        }else{
            musicSource.setPitch(1);
            musicSource.play();
        }
    }//GEN-LAST:event_btnPlayActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        ar.cleanup();
    }//GEN-LAST:event_formWindowClosing

    private void sldVolumeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sldVolumeStateChanged
       listener.setVolume( (float) sldVolume.getValue() / 100f);
       ar.setListener(listener);
    }//GEN-LAST:event_sldVolumeStateChanged

    private void btnStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStopActionPerformed
        if (musicSource != null){
            musicSource.setPitch(1);
            ar.stopSource(musicSource);
        }
    }//GEN-LAST:event_btnStopActionPerformed

    private void btnFFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFFActionPerformed
        if (musicSource.getStatus() == Status.Playing){
            musicSource.setPitch(2);
        }
    }//GEN-LAST:event_btnFFActionPerformed

    private void sldBarStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sldBarStateChanged
        if (musicSource != null && !sldBar.getValueIsAdjusting()){
            curTime = sldBar.getValue() / 100f;
            if (curTime < 0)
                curTime = 0;
            
            musicSource.setTimeOffset(curTime);
//            if (musicSource.getStatus() == Status.Playing){
//                musicSource.stop();               
//                musicSource.play();
//            }
            updateTime();
        }
    }//GEN-LAST:event_sldBarStateChanged

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new TestMusicPlayer().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnFF;
    private javax.swing.JButton btnOpen;
    private javax.swing.JButton btnPlay;
    private javax.swing.JButton btnRewind;
    private javax.swing.JButton btnStop;
    private javax.swing.JLabel lblTime;
    private javax.swing.JPanel pnlBar;
    private javax.swing.JPanel pnlButtons;
    private javax.swing.JSlider sldBar;
    private javax.swing.JSlider sldVolume;
    // End of variables declaration//GEN-END:variables

}
