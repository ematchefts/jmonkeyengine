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
package com.jme3.gde.terraineditor;

import com.jme3.gde.core.assets.AssetDataObject;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.scene.PreviewRequest;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.scene.SceneListener;
import com.jme3.gde.core.scene.SceneRequest;
import com.jme3.gde.core.sceneexplorer.nodes.JmeNode;
import com.jme3.gde.core.sceneexplorer.nodes.JmeSpatial;
import com.jme3.gde.core.sceneexplorer.nodes.JmeTerrainQuad;
import com.jme3.gde.core.sceneexplorer.nodes.NodeUtility;
import com.jme3.gde.core.sceneviewer.SceneViewerTopComponent;
import com.jme3.gde.core.util.DataObjectSaveNode;
import com.jme3.gde.core.util.ToggleButtonGroup;
import com.jme3.gde.terraineditor.sky.SkyboxWizardAction;
import com.jme3.gde.terraineditor.tools.*;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.terrain.ProgressMonitor;
import com.jme3.terrain.Terrain;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.texture.Texture;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.NotifyDescriptor.Confirmation;
import org.openide.WizardDescriptor;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.nodes.NodeEvent;
import org.openide.nodes.NodeListener;
import org.openide.nodes.NodeMemberEvent;
import org.openide.nodes.NodeReorderEvent;
import org.openide.util.Lookup.Result;
import org.openide.util.*;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(dtd = "-//com.jme3.gde.terraineditor//TerrainEditor//EN",
autostore = false)
public final class TerrainEditorTopComponent extends TopComponent implements SceneListener, LookupListener {

    private static TerrainEditorTopComponent instance;
    /** path to the icon used by the component and its open action */
    static final String ICON_PATH = "com/jme3/gde/terraineditor/TerraMonkey.png";
    private static final String PREFERRED_ID = "TerrainEditorTopComponent";
    private final Result<JmeSpatial> result;

    private TerrainCameraController camController;
    private TerrainToolController toolController;
    private TerrainEditorController editorController;
    private TerrainTextureController textureController;

    private SceneRequest currentRequest;
    private SceneRequest sentRequest;

    private CreateTerrainWizardAction terrainWizard;
    private SkyboxWizardAction skyboxWizard;
    private JmeSpatial selectedSpat;
    //private TerrainNodeListener terrainDeletedNodeListener;

    private HelpCtx ctx = new HelpCtx("sdk.terrain_editor");

    private Map<String, JButton> buttons = new HashMap<String, JButton>();
    private JPanel insideToolSettings;

    //private InstanceContent content;

    public TerrainEditorTopComponent()
    {
        initComponents();
        setName(NbBundle.getMessage(TerrainEditorTopComponent.class, "CTL_TerrainEditorTopComponent"));
        setToolTipText(NbBundle.getMessage(TerrainEditorTopComponent.class, "HINT_TerrainEditorTopComponent"));
        //content = new InstanceContent();

        /*ActionMap actionMap = getActionMap();
        for (Object key : actionMap.allKeys() ) {
            System.out.println("key: "+key+ actionMap.get(key));
            Action value = actionMap.get(key);
        }*/
        //actionMap.put(, terrainWizard);
        Lookup lookup = ExplorerUtils.createLookup(new ExplorerManager(), getActionMap());
        associateLookup(lookup);
        setIcon(ImageUtilities.loadImage(ICON_PATH, true));
        result = Utilities.actionsGlobalContext().lookupResult(JmeSpatial.class);
    }

    public Map<String, JButton> getButtons()
    {
        return this.buttons;
    }

    public TerrainEditorController getTerrainEditorToolController() { return this.editorController; }
    public TerrainToolController getTerrainToolController() { return this.toolController; }
    public TerrainTextureController getTerrainTextureController() { return this.textureController; }

    public JCheckBox getTriplanarCheckBox() { return this.triPlanarCheckBox; }
    public JTextField getShininessField() { return this.shininessField; }

    class EntropyCalcProgressMonitor implements ProgressMonitor {

        private ProgressHandle progressHandle;
        private float progress = 0;
        private float max = 0;
        private final Object lock = new Object();

        public void incrementProgress(float f) {
            progress += f;
            progressHandle.progress((int) progress);
        }

        public void setMonitorMax(float f) {
            max = f;

            if (progressHandle == null)
            {
                progressHandle = ProgressHandleFactory.createHandle("Calculating terrain entropies...");
                progressHandle.start((int) max);
            }
        }

        public float getMonitorMax() {
            return max;
        }

        public void progressComplete() {
            progressHandle.finish();
        }
    }

    private void setHintText(String text) {
        hintTextArea.setText(text);
    }

    private void setHintText(TerrainTool tool) {
        if (tool != null) {
            hintTextArea.setText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, tool.getToolHintTextKey()));
        } else {
            hintTextArea.setText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.toolHint.default"));
        }

        // show/hide the extra tool variables scroll pane
        if (insideToolSettings != null) {
            insideToolSettings.setVisible(false);
            toolSettingsPanel.remove(insideToolSettings);
            insideToolSettings = null;
        }

        if (tool != null) {
            if (tool.getClass() == RoughTerrainTool.class) {
                if (roughTerrainButton.isSelected()) {
                    toolSettingsPanel.add(fractalBrushPanel);
                    insideToolSettings = fractalBrushPanel;
                }
            } else if (tool.getClass() == LevelTerrainTool.class) {
                if (levelTerrainButton.isSelected()) {
                    toolSettingsPanel.add(levelBrushPanel);
                    insideToolSettings = levelBrushPanel;
                }
            } else if (tool.getClass() == SlopeTerrainTool.class) {
                if (slopeTerrainButton.isSelected()) {
                    toolSettingsPanel.add(slopeBrushPanel);
                    insideToolSettings = slopeBrushPanel;
                }
            }
        }

        if (insideToolSettings != null)
            insideToolSettings.setVisible(true);

        toolSettingsPanel.validate();
        validate();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        terrainModButtonGroup = new ToggleButtonGroup();
        textureFileChooser = new javax.swing.JFileChooser();
        levelBrushPanel = new javax.swing.JPanel();
        levelPrecisionCheckbox = new javax.swing.JCheckBox();
        jLabel6 = new javax.swing.JLabel();
        levelAbsoluteCheckbox = new javax.swing.JCheckBox();
        levelAbsoluteHeightField = new javax.swing.JFormattedTextField(NumberFormat.getInstance());
        fractalBrushPanel = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        lacunarityField = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        octavesField = new javax.swing.JTextField();
        scaleLabel = new javax.swing.JLabel();
        scaleField = new javax.swing.JTextField();
        slopeBrushPanel = new javax.swing.JPanel();
        slopePrecisionCheckbox = new javax.swing.JCheckBox();
        slopeLockCheckbox = new javax.swing.JCheckBox();
        jToolBar1 = new javax.swing.JToolBar();
        createTerrainButton = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        raiseTerrainButton = new javax.swing.JToggleButton();
        smoothTerrainButton = new javax.swing.JToggleButton();
        roughTerrainButton = new javax.swing.JToggleButton();
        levelTerrainButton = new javax.swing.JToggleButton();
        slopeTerrainButton = new javax.swing.JToggleButton();
        jSeparator2 = new javax.swing.JToolBar.Separator();
        addTextureButton = new javax.swing.JButton();
        removeTextureButton = new javax.swing.JButton();
        paintButton = new javax.swing.JToggleButton();
        jSeparator3 = new javax.swing.JToolBar.Separator();
        radiusLabel = new javax.swing.JLabel();
        radiusSlider = new javax.swing.JSlider();
        heightLabel = new javax.swing.JLabel();
        heightSlider = new javax.swing.JSlider();
        toolSettingsPanel = new javax.swing.JPanel();
        paintingPanel = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        textureTable = new javax.swing.JTable();
        remainingTexTitleLabel = new javax.swing.JLabel();
        remainingTexturesLabel = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        shininessField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        triPlanarCheckBox = new javax.swing.JCheckBox();
        hintPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        hintTextArea = new javax.swing.JTextArea();

        textureFileChooser.setApproveButtonText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.textureFileChooser.approveButtonText_1")); // NOI18N
        textureFileChooser.setCurrentDirectory(new java.io.File("C:\\Assets\\Textures"));
        textureFileChooser.setDialogTitle(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.textureFileChooser.dialogTitle_1")); // NOI18N
        textureFileChooser.setFileFilter(new ImageFilter());

        levelBrushPanel.setBackground(new java.awt.Color(204, 204, 204));
        levelBrushPanel.setOpaque(false);
        levelBrushPanel.setLayout(new java.awt.GridLayout(3, 2));

        org.openide.awt.Mnemonics.setLocalizedText(levelPrecisionCheckbox, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.PrecisionCheckbox.text")); // NOI18N
        levelPrecisionCheckbox.setToolTipText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.PrecisionCheckbox.tooltip")); // NOI18N
        levelPrecisionCheckbox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                levelPrecisionCheckboxActionPerformed(evt);
            }
        });
        levelBrushPanel.add(levelPrecisionCheckbox);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel6, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.jLabel6.text")); // NOI18N
        levelBrushPanel.add(jLabel6);

        org.openide.awt.Mnemonics.setLocalizedText(levelAbsoluteCheckbox, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.AbsoluteCheckbox.text")); // NOI18N
        levelAbsoluteCheckbox.setToolTipText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.AbsoluteCheckbox.tooltip")); // NOI18N
        levelAbsoluteCheckbox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                levelAbsoluteCheckboxActionPerformed(evt);
            }
        });
        levelBrushPanel.add(levelAbsoluteCheckbox);

        levelAbsoluteHeightField.setText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.levelAbsoluteHeightField.text")); // NOI18N
        levelAbsoluteHeightField.addKeyListener(new java.awt.event.KeyAdapter()
        {
            public void keyTyped(java.awt.event.KeyEvent evt)
            {
                levelAbsoluteHeightFieldKeyTyped(evt);
            }
        });
        levelBrushPanel.add(levelAbsoluteHeightField);

        fractalBrushPanel.setBackground(new java.awt.Color(204, 204, 204));
        fractalBrushPanel.setOpaque(false);
        fractalBrushPanel.setLayout(new java.awt.GridLayout(3, 2));

        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.jLabel3.text")); // NOI18N
        jLabel3.setToolTipText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.jLabel3.toolTipText")); // NOI18N
        fractalBrushPanel.add(jLabel3);

        lacunarityField.setText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.lacunarityField.text")); // NOI18N
        lacunarityField.setInputVerifier(new NumberInputVerifier());
        lacunarityField.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                lacunarityFieldActionPerformed(evt);
            }
        });
        lacunarityField.addKeyListener(new java.awt.event.KeyAdapter()
        {
            public void keyTyped(java.awt.event.KeyEvent evt)
            {
                lacunarityFieldKeyTyped(evt);
            }
        });
        fractalBrushPanel.add(lacunarityField);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel4, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.jLabel4.text")); // NOI18N
        jLabel4.setToolTipText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.jLabel4.toolTipText")); // NOI18N
        fractalBrushPanel.add(jLabel4);

        octavesField.setText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.octavesField.text")); // NOI18N
        octavesField.setInputVerifier(new NumberInputVerifier());
        octavesField.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                octavesFieldActionPerformed(evt);
            }
        });
        octavesField.addKeyListener(new java.awt.event.KeyAdapter()
        {
            public void keyTyped(java.awt.event.KeyEvent evt)
            {
                octavesFieldKeyTyped(evt);
            }
        });
        fractalBrushPanel.add(octavesField);

        org.openide.awt.Mnemonics.setLocalizedText(scaleLabel, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.scaleLabel.text")); // NOI18N
        scaleLabel.setToolTipText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.scaleLabel.toolTipText")); // NOI18N
        fractalBrushPanel.add(scaleLabel);

        scaleField.setText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.scaleField.text")); // NOI18N
        scaleField.setInputVerifier(new NumberInputVerifier());
        scaleField.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                scaleFieldActionPerformed(evt);
            }
        });
        scaleField.addKeyListener(new java.awt.event.KeyAdapter()
        {
            public void keyTyped(java.awt.event.KeyEvent evt)
            {
                scaleFieldKeyTyped(evt);
            }
        });
        fractalBrushPanel.add(scaleField);

        slopeBrushPanel.setBackground(new java.awt.Color(204, 204, 204));
        slopeBrushPanel.setOpaque(false);
        slopeBrushPanel.setLayout(new java.awt.GridLayout(3, 2));

        org.openide.awt.Mnemonics.setLocalizedText(slopePrecisionCheckbox, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.PrecisionCheckbox.text")); // NOI18N
        slopePrecisionCheckbox.setToolTipText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.PrecisionCheckbox.tooltip")); // NOI18N
        slopePrecisionCheckbox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                slopePrecisionCheckboxActionPerformed(evt);
            }
        });
        slopeBrushPanel.add(slopePrecisionCheckbox);

        org.openide.awt.Mnemonics.setLocalizedText(slopeLockCheckbox, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.slopeLockCheckbox.text")); // NOI18N
        slopeLockCheckbox.setToolTipText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.slopeLockCheckbox.tooltip")); // NOI18N
        slopeLockCheckbox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                slopeLockCheckboxActionPerformed(evt);
            }
        });
        slopeBrushPanel.add(slopeLockCheckbox);

        setBackground(java.awt.Color.gray);

        jToolBar1.setBackground(new java.awt.Color(204, 204, 204));
        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);
        jToolBar1.setOpaque(false);

        createTerrainButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/jme3/gde/terraineditor/icon_terrain-new.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(createTerrainButton, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.createTerrainButton.text")); // NOI18N
        createTerrainButton.setToolTipText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.createTerrainButton.toolTipText")); // NOI18N
        createTerrainButton.setOpaque(false);
        createTerrainButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                createTerrainButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(createTerrainButton);
        jToolBar1.add(jSeparator1);

        terrainModButtonGroup.add(raiseTerrainButton);
        raiseTerrainButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/jme3/gde/terraineditor/icon_terrain-up.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(raiseTerrainButton, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.raiseTerrainButton.text")); // NOI18N
        raiseTerrainButton.setToolTipText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.raiseTerrainButton.toolTipText")); // NOI18N
        raiseTerrainButton.setActionCommand(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.raiseTerrainButton.actionCommand")); // NOI18N
        raiseTerrainButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                raiseTerrainButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(raiseTerrainButton);

        terrainModButtonGroup.add(smoothTerrainButton);
        smoothTerrainButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/jme3/gde/terraineditor/icon_terrain-smooth.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(smoothTerrainButton, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.smoothTerrainButton.text")); // NOI18N
        smoothTerrainButton.setToolTipText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.smoothTerrainButton.toolTipText")); // NOI18N
        smoothTerrainButton.setActionCommand(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.smoothTerrainButton.actionCommand")); // NOI18N
        smoothTerrainButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                smoothTerrainButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(smoothTerrainButton);

        terrainModButtonGroup.add(roughTerrainButton);
        roughTerrainButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/jme3/gde/terraineditor/icon_terrain-rough.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(roughTerrainButton, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.roughTerrainButton.text")); // NOI18N
        roughTerrainButton.setToolTipText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.roughTerrainButton.toolTipText")); // NOI18N
        roughTerrainButton.setActionCommand(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.roughTerrainButton.actionCommand")); // NOI18N
        roughTerrainButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                roughTerrainButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(roughTerrainButton);

        terrainModButtonGroup.add(levelTerrainButton);
        levelTerrainButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/jme3/gde/terraineditor/icon_terrain-level.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(levelTerrainButton, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.levelTerrainButton.text")); // NOI18N
        levelTerrainButton.setToolTipText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.levelTerrainButton.toolTipText")); // NOI18N
        levelTerrainButton.setFocusable(false);
        levelTerrainButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        levelTerrainButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        levelTerrainButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                levelTerrainButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(levelTerrainButton);

        terrainModButtonGroup.add(slopeTerrainButton);
        slopeTerrainButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/jme3/gde/terraineditor/icon_terrain-slope.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(slopeTerrainButton, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.slopeTerrainButton.text")); // NOI18N
        slopeTerrainButton.setToolTipText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.slopeTerrainButton.toolTipText")); // NOI18N
        slopeTerrainButton.setFocusable(false);
        slopeTerrainButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        slopeTerrainButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        slopeTerrainButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                slopeTerrainButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(slopeTerrainButton);
        jToolBar1.add(jSeparator2);

        addTextureButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/jme3/gde/terraineditor/icon_terrain-add-texture.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(addTextureButton, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.addTextureButton.text")); // NOI18N
        addTextureButton.setToolTipText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.addTextureButton.toolTipText")); // NOI18N
        addTextureButton.setBorderPainted(false);
        addTextureButton.setOpaque(false);
        addTextureButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                addTextureButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(addTextureButton);

        removeTextureButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/jme3/gde/terraineditor/icon_terrain-remove-texture.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(removeTextureButton, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.removeTextureButton.text")); // NOI18N
        removeTextureButton.setToolTipText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.removeTextureButton.toolTipText")); // NOI18N
        removeTextureButton.setBorderPainted(false);
        removeTextureButton.setOpaque(false);
        removeTextureButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                removeTextureButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(removeTextureButton);

        terrainModButtonGroup.add(paintButton);
        paintButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/jme3/gde/terraineditor/icon_terrain-paint-circle.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(paintButton, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.paintButton.text")); // NOI18N
        paintButton.setToolTipText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.paintButton.toolTipText")); // NOI18N
        paintButton.setFocusable(false);
        paintButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        paintButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        paintButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                paintButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(paintButton);
        jToolBar1.add(jSeparator3);

        org.openide.awt.Mnemonics.setLocalizedText(radiusLabel, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.radiusLabel.text")); // NOI18N
        jToolBar1.add(radiusLabel);

        radiusSlider.setMajorTickSpacing(10);
        radiusSlider.setMinorTickSpacing(5);
        radiusSlider.setPaintTicks(true);
        radiusSlider.setToolTipText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.radiusSlider.toolTipText")); // NOI18N
        radiusSlider.setValue(5);
        radiusSlider.setOpaque(false);
        radiusSlider.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                radiusSliderStateChanged(evt);
            }
        });
        jToolBar1.add(radiusSlider);

        org.openide.awt.Mnemonics.setLocalizedText(heightLabel, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.heightLabel.text")); // NOI18N
        jToolBar1.add(heightLabel);

        heightSlider.setMajorTickSpacing(20);
        heightSlider.setMaximum(200);
        heightSlider.setPaintTicks(true);
        heightSlider.setOpaque(false);
        heightSlider.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                heightSliderStateChanged(evt);
            }
        });
        jToolBar1.add(heightSlider);

        toolSettingsPanel.setBackground(new java.awt.Color(204, 204, 204));
        toolSettingsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.toolSettingsPanel.border.title"))); // NOI18N
        toolSettingsPanel.setOpaque(false);
        toolSettingsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));

        paintingPanel.setBackground(new java.awt.Color(204, 204, 204));
        paintingPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.paintingPanel.border.title"))); // NOI18N
        paintingPanel.setOpaque(false);

        textureTable.setModel(new TextureTableModel(this));
        textureTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        textureTable.setCellEditor(new TextureCellRendererEditor(this));
        textureTable.setColumnSelectionAllowed(true);
        textureTable.setSelectionModel(new TableSelectionModel());
        textureTable.getTableHeader().setReorderingAllowed(false);
        jScrollPane2.setViewportView(textureTable);
        textureTable.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        org.openide.awt.Mnemonics.setLocalizedText(remainingTexTitleLabel, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.remainingTexTitleLabel.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(remainingTexturesLabel, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.remainingTexturesLabel.text")); // NOI18N

        javax.swing.GroupLayout paintingPanelLayout = new javax.swing.GroupLayout(paintingPanel);
        paintingPanel.setLayout(paintingPanelLayout);
        paintingPanelLayout.setHorizontalGroup(
            paintingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(paintingPanelLayout.createSequentialGroup()
                .addComponent(remainingTexTitleLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(remainingTexturesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 281, Short.MAX_VALUE)
        );
        paintingPanelLayout.setVerticalGroup(
            paintingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, paintingPanelLayout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(paintingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(remainingTexTitleLabel)
                    .addComponent(remainingTexturesLabel)))
        );

        jPanel2.setBackground(new java.awt.Color(204, 204, 204));
        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.jPanel2.border.title"))); // NOI18N
        jPanel2.setOpaque(false);

        shininessField.setText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.shininessField.text")); // NOI18N
        shininessField.setToolTipText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.shininessField.toolTipText")); // NOI18N
        shininessField.setInputVerifier(new ShininessVerifier());
        shininessField.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                shininessFieldActionPerformed(evt);
            }
        });
        shininessField.addKeyListener(new java.awt.event.KeyAdapter()
        {
            public void keyTyped(java.awt.event.KeyEvent evt)
            {
                shininessFieldKeyTyped(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.jLabel1.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(triPlanarCheckBox, org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.triPlanarCheckBox.text")); // NOI18N
        triPlanarCheckBox.setToolTipText(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.triPlanarCheckBox.toolTipText")); // NOI18N
        triPlanarCheckBox.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                triPlanarCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(shininessField, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 66, Short.MAX_VALUE))
            .addComponent(triPlanarCheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, 111, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(triPlanarCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(shininessField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addContainerGap())
        );

        hintPanel.setBackground(new java.awt.Color(204, 204, 204));
        hintPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(TerrainEditorTopComponent.class, "TerrainEditorTopComponent.hintPanel.border.title"))); // NOI18N
        hintPanel.setOpaque(false);

        hintTextArea.setEditable(false);
        hintTextArea.setColumns(20);
        hintTextArea.setLineWrap(true);
        hintTextArea.setRows(2);
        hintTextArea.setTabSize(4);
        hintTextArea.setWrapStyleWord(true);
        hintTextArea.setFocusable(false);
        hintTextArea.setRequestFocusEnabled(false);
        jScrollPane1.setViewportView(hintTextArea);

        javax.swing.GroupLayout hintPanelLayout = new javax.swing.GroupLayout(hintPanel);
        hintPanel.setLayout(hintPanelLayout);
        hintPanelLayout.setHorizontalGroup(
            hintPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 361, Short.MAX_VALUE)
        );
        hintPanelLayout.setVerticalGroup(
            hintPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 121, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(toolSettingsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 208, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(paintingPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(hintPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, 1015, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(hintPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(paintingPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(toolSettingsPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 144, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void createTerrainButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createTerrainButtonActionPerformed
        addSpatial("Terrain");
    }//GEN-LAST:event_createTerrainButtonActionPerformed

    private void raiseTerrainButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_raiseTerrainButtonActionPerformed

        //toolController.setShowGrid(true);

        if (raiseTerrainButton.isSelected()) {
            RaiseTerrainTool tool = new RaiseTerrainTool(editorController);
            toolController.setTerrainEditButtonState(tool);
            setHintText(tool);
        } else {
            toolController.setTerrainEditButtonState(null);
            setHintText((TerrainTool) null);
        }
    }//GEN-LAST:event_raiseTerrainButtonActionPerformed

    private void addTextureButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addTextureButtonActionPerformed
        if (editorController == null || editorController.getTerrain(null) == null) {
            return;
        }

        int index = textureController.getTableModel().getRowCount(); // get the last row
        addNewTextureLayer(index);
        //  editorController.enableTextureButtons();
    }//GEN-LAST:event_addTextureButtonActionPerformed

    protected void enableAddTextureButton(boolean enabled) {
        addTextureButton.setEnabled(enabled);
    }

    protected void enableRemoveTextureButton(boolean enabled) {
        removeTextureButton.setEnabled(enabled);
    }

    protected void updateTextureCountLabel(int count) {
        remainingTexturesLabel.setText("" + count);
    }

    protected void setAddNormalTextureEnabled(boolean enabled) {
        textureController.availableNormalTextures = enabled;
    }

    private void removeTextureButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeTextureButtonActionPerformed
        if (editorController == null || editorController.getTerrain(null) == null) {
            return;
        }
        if (textureController.getTableModel().getRowCount() == 0) {
            return;
        }
        int index = textureController.getTableModel().getRowCount() - 1; // get the last row
        removeTextureLayer(index);
        buttons.remove(index + "-" + 1);
        buttons.remove(index + "-" + 2);
        textureController.enableTextureButtons();
    }//GEN-LAST:event_removeTextureButtonActionPerformed

    private void paintButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_paintButtonActionPerformed
        if (paintButton.isSelected()) {
            PaintTerrainTool tool = new PaintTerrainTool(editorController);
            toolController.setTerrainEditButtonState(tool);
            setHintText(tool);
        } else {
            toolController.setTerrainEditButtonState(null);
            setHintText((TerrainTool) null);
        }
    }//GEN-LAST:event_paintButtonActionPerformed

    private void triPlanarCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_triPlanarCheckBoxActionPerformed
        editorController.setTriPlanarEnabled(triPlanarCheckBox.isSelected());
        ((TextureTableModel) textureTable.getModel()).updateScales();
        if (triPlanarCheckBox.isSelected()) {
            setHintText("Make sure your scale is a power of 2, (1/2^n), when in tri-planar mode");
        }
    }//GEN-LAST:event_triPlanarCheckBoxActionPerformed

    private void levelTerrainButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_levelTerrainButtonActionPerformed
        if (levelTerrainButton.isSelected()) {
            LevelTerrainTool tool = new LevelTerrainTool(editorController);
            toolController.setTerrainEditButtonState(tool);
            updateLevelToolParams();
            setHintText(tool);
        } else {
            toolController.setTerrainEditButtonState(null);
            setHintText((TerrainTool) null);
        }
    }//GEN-LAST:event_levelTerrainButtonActionPerformed

    private void radiusSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_radiusSliderStateChanged
        if (toolController != null) {
            toolController.setHeightToolRadius((float) radiusSlider.getValue() / (float) radiusSlider.getMaximum());
        }
    }//GEN-LAST:event_radiusSliderStateChanged

    private void heightSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_heightSliderStateChanged
        if (toolController != null) {
            toolController.setHeightToolHeight((float) heightSlider.getValue() / (float) heightSlider.getMaximum());
        }
    }//GEN-LAST:event_heightSliderStateChanged

    private void smoothTerrainButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_smoothTerrainButtonActionPerformed
        if (smoothTerrainButton.isSelected()) {
            SmoothTerrainTool tool = new SmoothTerrainTool(editorController);
            toolController.setTerrainEditButtonState(tool);
            setHintText(tool);
        } else {
            toolController.setTerrainEditButtonState(null);
            setHintText((TerrainTool) null);
        }
    }//GEN-LAST:event_smoothTerrainButtonActionPerformed

    private void shininessFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_shininessFieldActionPerformed
        try {
            Float f = new Float(shininessField.getText());
            editorController.setShininess(Math.max(0, f));
        } catch (Exception e) {
            Logger.getLogger(TerrainEditorTopComponent.class.getName()).log(Level.WARNING,
                    "Error accessing shininess field in terrain material.", e);
        }

    }//GEN-LAST:event_shininessFieldActionPerformed

    private void shininessFieldKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_shininessFieldKeyTyped
        if (KeyEvent.VK_ENTER == evt.getKeyCode() ||
            KeyEvent.VK_TAB == evt.getKeyCode() ){
            shininessFieldActionPerformed(null);
        }
    }//GEN-LAST:event_shininessFieldKeyTyped

    private void octavesFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_octavesFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_octavesFieldActionPerformed

    private void roughTerrainButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_roughTerrainButtonActionPerformed
        if (roughTerrainButton.isSelected()) {
            RoughTerrainTool tool = new RoughTerrainTool(editorController);
            toolController.setTerrainEditButtonState(tool);
            updateRoughenFractalToolParams();
            setHintText(tool);
        } else {
            toolController.setTerrainEditButtonState(null);
            setHintText((TerrainTool) null);
        }
    }//GEN-LAST:event_roughTerrainButtonActionPerformed

    private void lacunarityFieldKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_lacunarityFieldKeyTyped
        updateRoughenFractalToolParams();
    }//GEN-LAST:event_lacunarityFieldKeyTyped

    private void octavesFieldKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_octavesFieldKeyTyped
        updateRoughenFractalToolParams();
    }//GEN-LAST:event_octavesFieldKeyTyped

    private void scaleFieldKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_scaleFieldKeyTyped
        updateRoughenFractalToolParams();
    }//GEN-LAST:event_scaleFieldKeyTyped

    private void lacunarityFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lacunarityFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_lacunarityFieldActionPerformed

    private void scaleFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scaleFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_scaleFieldActionPerformed

    private void slopeTerrainButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_slopeTerrainButtonActionPerformed
        if (slopeTerrainButton.isSelected()) {
            SlopeTerrainTool tool = new SlopeTerrainTool();
            toolController.setTerrainEditButtonState(tool);
            updateSlopeToolParams();
            setHintText(tool);
        } else {
            toolController.setTerrainEditButtonState(null);
            setHintText((TerrainTool) null);
        }
    }//GEN-LAST:event_slopeTerrainButtonActionPerformed

    private void levelPrecisionCheckboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_levelPrecisionCheckboxActionPerformed
        updateLevelToolParams();
    }//GEN-LAST:event_levelPrecisionCheckboxActionPerformed

    private void levelAbsoluteHeightFieldKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_levelAbsoluteHeightFieldKeyTyped
        updateLevelToolParams();
    }//GEN-LAST:event_levelAbsoluteHeightFieldKeyTyped

    private void slopePrecisionCheckboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_slopePrecisionCheckboxActionPerformed
        updateSlopeToolParams();
    }//GEN-LAST:event_slopePrecisionCheckboxActionPerformed

    private void slopeLockCheckboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_slopeLockCheckboxActionPerformed
        updateSlopeToolParams();
    }//GEN-LAST:event_slopeLockCheckboxActionPerformed

    private void levelAbsoluteCheckboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_levelAbsoluteCheckboxActionPerformed
        updateLevelToolParams();
        levelAbsoluteHeightField.setEnabled(levelAbsoluteCheckbox.isEnabled());
    }//GEN-LAST:event_levelAbsoluteCheckboxActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addTextureButton;
    private javax.swing.JButton createTerrainButton;
    private javax.swing.JPanel fractalBrushPanel;
    private javax.swing.JLabel heightLabel;
    private javax.swing.JSlider heightSlider;
    private javax.swing.JPanel hintPanel;
    private javax.swing.JTextArea hintTextArea;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator2;
    private javax.swing.JToolBar.Separator jSeparator3;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JTextField lacunarityField;
    private javax.swing.JCheckBox levelAbsoluteCheckbox;
    private javax.swing.JFormattedTextField levelAbsoluteHeightField;
    private javax.swing.JPanel levelBrushPanel;
    private javax.swing.JCheckBox levelPrecisionCheckbox;
    private javax.swing.JToggleButton levelTerrainButton;
    private javax.swing.JTextField octavesField;
    private javax.swing.JToggleButton paintButton;
    private javax.swing.JPanel paintingPanel;
    private javax.swing.JLabel radiusLabel;
    private javax.swing.JSlider radiusSlider;
    private javax.swing.JToggleButton raiseTerrainButton;
    private javax.swing.JLabel remainingTexTitleLabel;
    private javax.swing.JLabel remainingTexturesLabel;
    private javax.swing.JButton removeTextureButton;
    private javax.swing.JToggleButton roughTerrainButton;
    private javax.swing.JTextField scaleField;
    private javax.swing.JLabel scaleLabel;
    private javax.swing.JTextField shininessField;
    private javax.swing.JPanel slopeBrushPanel;
    private javax.swing.JCheckBox slopeLockCheckbox;
    private javax.swing.JCheckBox slopePrecisionCheckbox;
    private javax.swing.JToggleButton slopeTerrainButton;
    private javax.swing.JToggleButton smoothTerrainButton;
    private javax.swing.ButtonGroup terrainModButtonGroup;
    private javax.swing.JFileChooser textureFileChooser;
    private javax.swing.JTable textureTable;
    private javax.swing.JPanel toolSettingsPanel;
    private javax.swing.JCheckBox triPlanarCheckBox;
    // End of variables declaration//GEN-END:variables

    /**
     * Validate text fields for Float number values
     */
    protected class NumberInputVerifier extends InputVerifier {
        @Override
        public boolean verify(JComponent input) {
            try {
                javax.swing.JTextField textField = (javax.swing.JTextField)input;
                String a=textField.getText();
                Float.parseFloat(a);
            }
            catch (NumberFormatException e) {
                Toolkit.getDefaultToolkit().beep();
                return false;
            }
            return true;
        }
    }

    private class ShininessVerifier extends InputVerifier {
        @Override
        public boolean verify(JComponent input) {
            if (input instanceof javax.swing.JTextField) {
                String text = ((javax.swing.JTextField)input).getText();
                try {
                    Float f = new Float(text);
                    if (f > 0)
                        return true;
                } catch (Exception e) {
                    Toolkit.getDefaultToolkit().beep();
                    return false;
                }
            }
            return false;
        }

    }

     private void updateSlopeToolParams() {
         SlopeExtraToolParams params = new SlopeExtraToolParams();
         params.precision = slopePrecisionCheckbox.isSelected();
         params.lock = slopeLockCheckbox.isSelected();
         toolController.setExtraToolParams(params);
    }

    private void updateLevelToolParams() {
        try {
            LevelExtraToolParams params = new LevelExtraToolParams();
            params.absolute = levelAbsoluteCheckbox.isSelected();
            params.precision = levelPrecisionCheckbox.isSelected();
            params.height = new Float(levelAbsoluteHeightField.getText());
            toolController.setExtraToolParams(params);
        } catch (NumberFormatException e) {}
    }

    private void updateRoughenFractalToolParams() {
        try {
            RoughExtraToolParams params = new RoughExtraToolParams();
            //params.amplitude = new Float(amplitudeField.getText());
            //params.frequency = new Float(frequencyField.getText());
            params.lacunarity = new Float(lacunarityField.getText());
            params.octaves = new Float(octavesField.getText());
            //params.roughness = new Float(roughnessField.getText());
            params.scale = new Float(scaleField.getText());
            toolController.setExtraToolParams(params);

        } catch (NumberFormatException e) {}
    }

    protected void getExtraToolParams() {
        if (toolController.getCurrentTerrainTool() != null) {
            if (toolController.getCurrentTerrainTool().getClass() == RoughTerrainTool.class)
                updateRoughenFractalToolParams();
            else if (toolController.getCurrentTerrainTool().getClass() == LevelTerrainTool.class)
                updateLevelToolParams();
            else if (toolController.getCurrentTerrainTool().getClass() == SlopeTerrainTool.class)
                updateSlopeToolParams();
        }
    }

    /**
     * Gets default instance. Do not use directly: reserved for *.settings files only,
     * i.e. deserialization routines; otherwise you could get a non-deserialized instance.
     * To obtain the singleton instance, use {@link #findInstance}.
     */
    public static synchronized TerrainEditorTopComponent getDefault() {
        if (instance == null) {
            instance = new TerrainEditorTopComponent();
        }
        return instance;
    }

    public void addSpatial(final String name) {
        if (selectedSpat == null) {

            Confirmation msg = new NotifyDescriptor.Confirmation(
                    "You must select a Node to add the " + name + " to in the Scene Explorer window",
                    NotifyDescriptor.OK_CANCEL_OPTION,
                    NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(msg);
            return;
        }

        final Spatial node = selectedSpat.getLookup().lookup(Spatial.class);
        if (node != null) {
            if ("Terrain".equals(name)) {
                if (terrainWizard == null) {
                    terrainWizard = new CreateTerrainWizardAction(this);
                }
                terrainWizard.performAction();
            } else if ("Skybox".equals(name)) {
                if (skyboxWizard == null) {
                    skyboxWizard = new SkyboxWizardAction(this);
                }
                skyboxWizard.performAction();
            } else if ("Ocean".equals(name)) {
            }
        }

    }

    protected void generateTerrain(final WizardDescriptor wizardDescriptor) {
        final Spatial node = selectedSpat.getLookup().lookup(Spatial.class);

        int totalSize = (Integer) wizardDescriptor.getProperty("totalSize");
        int patchSize = (Integer) wizardDescriptor.getProperty("patchSize");
        int alphaTextureSize = (Integer) wizardDescriptor.getProperty("alphaTextureSize");

        float[] heightmapData = null;
        AbstractHeightMap heightmap = (AbstractHeightMap) wizardDescriptor.getProperty("abstractHeightMap");
        if (heightmap != null) {
            heightmap.load(); // can take a while
            heightmapData = heightmap.getHeightMap();
        }

        // eg. Scenes/newScene1.j3o
        String[] split1 = currentRequest.getWindowTitle().split("/");
        String[] split2 = split1[split1.length - 1].split("\\.");

        Terrain terrain = null;
        try {
            terrain = editorController.createTerrain((Node) node,
                    totalSize,
                    patchSize,
                    alphaTextureSize,
                    heightmapData,
                    split2[0],
                    selectedSpat);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        addSaveNode(selectedSpat);

        editorController.setNeedsSave(true);
        textureController.enableTextureButtons();

        textureController.reinitTextureTable(); // update the UI
        refreshSelected();

        //createTerrainButton.setEnabled(false); // only let the user add one terrain
    }

    public void generateSkybox(WizardDescriptor wiz) {

        Spatial sky = null;
        final Spatial node = selectedSpat.getLookup().lookup(Spatial.class);

        if ((Boolean) wiz.getProperty("multipleTextures")) {
            Texture south = (Texture) wiz.getProperty("textureSouth");
            Texture north = (Texture) wiz.getProperty("textureNorth");
            Texture east = (Texture) wiz.getProperty("textureEast");
            Texture west = (Texture) wiz.getProperty("textureWest");
            Texture top = (Texture) wiz.getProperty("textureTop");
            Texture bottom = (Texture) wiz.getProperty("textureBottom");
            Vector3f normalScale = (Vector3f) wiz.getProperty("normalScale");
            sky = editorController.createSky((Node) node, west, east, north, south, top, bottom, normalScale);
        } else {
            Texture textureSingle = (Texture) wiz.getProperty("textureSingle");
            Vector3f normalScale = (Vector3f) wiz.getProperty("normalScale");
            boolean useSpheremap = (Boolean) wiz.getProperty("useSpheremap");
            sky = editorController.createSky((Node) node, textureSingle, useSpheremap, normalScale);
        }

        editorController.setNeedsSave(true);
        refreshSelected();
    }

    private void refreshSelected() {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                if (selectedSpat != null) {
                    selectedSpat.refresh(false);
                }
            }
        });

    }

    public JTable getTextureTable()
    {
        return this.textureTable;
    }

    // set texture table via node selection
    private void setTextureTable(JmeSpatial terrain)
    {
        textureController.clearTextureTable();
        JmeTerrainQuad jt = (JmeTerrainQuad)terrain;

        TerrainQuad tq = terrain.getLookup().lookup(TerrainQuad.class);

        for (int i = 0; i < 3; i++)
        {
            // List<MatParam> params = (List<MatParam>) terrainMaterial.getParams();
            return;
        }

    }


    /**
     * listener for node selection changes
     */
    public void resultChanged(LookupEvent ev) {
        if (currentRequest == null || !currentRequest.isDisplayed()) {
            return;
        }
        Collection<JmeSpatial> items = (Collection<JmeSpatial>) result.allInstances();
        for (JmeSpatial spatial : items) {
            selectSpatial(spatial);
            return;
        }
    }

    private void selectSpatial(JmeSpatial spatial) {
        selectedSpat = spatial;
        editorController.setSelectedSpat(spatial);
        /*if (selectedSpat instanceof JmeTerrainQuad) { //TODO shouldn't be terrainQuad, should be a generic JmeTerrain
        selectedSpat.removeNodeListener(terrainDeletedNodeListener); // remove it if it exists, no way to check if it is there already
        selectedSpat.addNodeListener(terrainDeletedNodeListener); // add it back
        }*/

        // debug
        if (spatial instanceof JmeTerrainQuad)
        {
            System.out.println("Selected Terrain: " + spatial.getName());

            found = false;
            displaySelectedTerrainNodeMaterial(editorController.rootNode, spatial.getName());
        }
    }

    private boolean found = false;
    private void displaySelectedTerrainNodeMaterial(Node parent, String name)
    {
        if (found) return;

        for (Spatial child : parent.getChildren())
        {
            if (child instanceof TerrainQuad)
            {
                if (child.getName().equals(name))
                {
                    editorController.terrainNode = (Node)child;
                    textureController.reinitTextureTable();
                    found = true;
                    return;
                }
            }
            else
            {
                Node node = (Node)child;
                if (!node.getChildren().isEmpty())
                {
                    displaySelectedTerrainNodeMaterial(node, name);
                }
            }
        }
    }

    /**
     * When the terrain is deleted, enable the 'add terrain' button again
     * and reinitialize the texture table
     */
    private class TerrainNodeListener implements NodeListener {

        public void childrenAdded(NodeMemberEvent nme) {
        }

        public void childrenRemoved(NodeMemberEvent nme) {
        }

        public void childrenReordered(NodeReorderEvent nre) {
        }

        public void nodeDestroyed(NodeEvent ne) {
            createTerrainButton.setEnabled(true);
            textureController.reinitTextureTable();
        }

        public void propertyChange(PropertyChangeEvent evt) {
        }
    }

    /*
     *
     *******************************************************************/
    /**
     * Obtain the TerrainEditorTopComponent instance. Never call {@link #getDefault} directly!
     */
    public static synchronized TerrainEditorTopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) {
            Logger.getLogger(TerrainEditorTopComponent.class.getName()).warning(
                    "Cannot find " + PREFERRED_ID + " component. It will not be located properly in the window system.");
            return getDefault();
        }
        if (win instanceof TerrainEditorTopComponent) {
            return (TerrainEditorTopComponent) win;
        }
        Logger.getLogger(TerrainEditorTopComponent.class.getName()).warning(
                "There seem to be multiple components with the '" + PREFERRED_ID
                + "' ID. That is a potential source of errors and unexpected behavior.");
        return getDefault();
    }

    @Override
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_ALWAYS;
    }

    @Override
    public void componentOpened() {
        super.componentOpened();
        if (currentRequest == null) {
            close();
        }
    }

    @Override
    public void componentClosed() {
        super.componentClosed();
        if (currentRequest != null) {
            SceneApplication.getApplication().closeScene(currentRequest);
        }

    }

    @Override
    protected void componentActivated() {
        SceneViewerTopComponent.findInstance().requestVisible();
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    Object readProperties(java.util.Properties p) {
        if (instance == null) {
            instance = this;
        }
        instance.readPropertiesImpl(p);
        return instance;
    }

    private void readPropertiesImpl(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }

    @Override
    protected String preferredID() {
        return PREFERRED_ID;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return ctx;
    }

    public void openScene(Spatial spat, AssetDataObject file, ProjectAssetManager manager) {
        cleanupControllers();
        SceneApplication.getApplication().addSceneListener(this);
        result.addLookupListener(this);
        //TODO: handle request change
        Node node;
        if (spat instanceof Node) {
            node = (Node) spat;
        } else {
            node = new Node();
            node.attachChild(spat);
        }
        JmeNode jmeNode = NodeUtility.createNode(node, file, false);
        SceneRequest request = new SceneRequest(this, jmeNode, manager);
        request.setDataObject(file);
        request.setHelpCtx(ctx);

        addSaveNode(jmeNode);

        //SceneUndoRedoManager m = Lookup.getDefault().lookup(SceneUndoRedoManager.class);//TODO remove this line

        Logger.getLogger(TerrainEditorTopComponent.class.getName()).log(Level.FINER, "Terrain openScene: {0}", file.getName());

        if (editorController != null) {
            editorController.cleanup();
        }

        //this.associateLookup( new AbstractLookup(content) ); // for saving alpha images

        editorController = new TerrainEditorController(jmeNode, file, this);


        this.sentRequest = request;
        request.setWindowTitle("TerrainEditor - " + manager.getRelativeAssetPath(file.getPrimaryFile().getPath()));
        request.setToolNode(new Node("TerrainEditorToolNode"));
        SceneApplication.getApplication().openScene(request);

        //terrainDeletedNodeListener = new TerrainNodeListener();
        //editorController.enableTextureButtons();

    }

    // runs on AWT thread now
    public void sceneOpened(SceneRequest request) {

        if (request.equals(sentRequest) && editorController != null) {
            currentRequest = request;
            //Logger.getLogger(TerrainEditorTopComponent.class.getName()).finer("Terrain sceneRequested " + request.getWindowTitle());

            setSceneInfo(currentRequest.getJmeNode(), true);

            //editorController.doGetAlphaSaveDataObject(this);

            // if the opened scene has terrain, add it to a save node

            /* Terrain terrain = (Terrain) editorController.getTerrain(null);
            if (terrain != null) {
                // add the terrain root save node

                ((Node) terrain).setMaterial(terrain.getMaterial(null));
                // it appears when loading the actual applied material on the terrain
                // does not reflect the material that we get from the terrain.

                refreshSelected();
            } */


            if (camController != null)
            {
                camController.disable();
            }

            if (toolController != null)
            {
                toolController.cleanup();
            }

            if (textureController != null)
            {
                textureController.cleanup();
            }

            toolController = new TerrainToolController(currentRequest.getToolNode(), currentRequest.getManager().getManager(), request.getJmeNode());
            camController = new TerrainCameraController(SceneApplication.getApplication().getCamera());
            camController.setMaster(this);
            camController.enable();

            camController.setToolController(toolController);
            camController.setEditorController(editorController);
            toolController.setEditorController(editorController);
            toolController.setCameraController(camController);
            toolController.setTopComponent(this);

            toolController.setHeightToolRadius((float) radiusSlider.getValue() / (float) radiusSlider.getMaximum());
            toolController.setHeightToolHeight((float) heightSlider.getValue() / (float) heightSlider.getMaximum());
            //toolController.setToolMesh(meshForm.isSelected()); // future for adding brush shape

            textureController = new TerrainTextureController(this);

            editorController.setTerrainLodCamera();

            textureController.reinitTextureTable(); // update the UI
            fractalBrushPanel.setVisible(false);

            Node rootNode = editorController.getRootNode();
            recurseTerrainRefresh(rootNode);
        }
    }

    private void recurseTerrainRefresh(Node parent)
    {
        for (Spatial child : parent.getChildren())
        {
            if (child instanceof Terrain)
            {
                Terrain terrain = (Terrain)child;
                ((Node) terrain).setMaterial(terrain.getMaterial(null));
            }
            else
            {
                Node node = (Node)child;

                if (!node.getChildren().isEmpty())
                    recurseTerrainRefresh(node);
            }
        }
    }

    protected synchronized void addDataObject(final DataObjectSaveNode dataObject) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                addSaveNode(dataObject);
            }
        });
    }

    protected void addSaveNode(org.openide.nodes.Node node) {
        setActivatedNodes(new org.openide.nodes.Node[]{node});
    }

    private void setSceneInfo(final JmeNode jmeNode, final boolean active) {
        final TerrainEditorTopComponent inst = this;
        if (jmeNode != null) {
        } else {
        }

        if (!active) {
            result.removeLookupListener(inst);
            close();
        } else {
            open();
            requestActive();
        }
    }

    public void sceneClosed(SceneRequest request) {
        if (request.equals(currentRequest)) {
            setActivatedNodes(new org.openide.nodes.Node[]{});
            SceneApplication.getApplication().removeSceneListener(this);
            currentRequest = null;
            setSceneInfo(null, false);
            cleanupControllers();
        }
    }

    public void previewCreated(PreviewRequest request) {
    }

    private void cleanupControllers() {
        if (camController != null) {
            camController.disable();
            camController = null;
        }
        if (toolController != null) {
            toolController.cleanup();
            toolController = null;
        }
        if (editorController != null) {
            editorController.cleanup();
            editorController = null;
        }
        setActivatedNodes(new org.openide.nodes.Node[]{});
    }

    /**
     * Adds another texture layer to the material, sets a default texture for it.
     * Assumes that the new index is in the range of the amount of available textures
     * the material can support.
     * Assumes this is the last index, or else messy stuff in the material will happen.
     * @param the index it is being placed at.
     */
    private void addNewTextureLayer(int newIndex) {
        textureController.getTableModel().addNewTexture(newIndex);
    }

    /**
     * Removes the selected index.
     * If the index is -1, it just returns.
     * Assumes this is the last index, or else messy stuff in the material will happen.
     * @param selectedIndex
     */
    private void removeTextureLayer(int selectedIndex) {
        if (selectedIndex < 0) {
            return; // abort
        }
        textureController.getTableModel().removeTexture(selectedIndex);
        textureController.removeTextureLayer(selectedIndex);
    }

    /**
     * signals to the tool controller when a different row is selected
     */
    public class TableSelectionModel extends DefaultListSelectionModel {

        public TableSelectionModel() {
            super.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            addListSelectionListener(new ListSelectionListener() {

                public void valueChanged(ListSelectionEvent e)
                {
                    if (toolController != null)
                    {
                        toolController.setSelectedTextureIndex(textureTable.getSelectedRow());

                        /* Node currentTerrain = editorController.getTerrain(null);
                        if (currentTerrain != null)
                        {
                            String terrainName = currentTerrain.getName();
                            int selectedRow = getTextureTable().getSelectedRow();


                            getTerrainTextureController().setSelectedTextureReminder(terrainName, selectedRow);
                        }*/
                    }
                }
            });
        }
    }

    /**
     * A file filter to only show images
     */
    public class ImageFilter extends FileFilter {

        Utils utils = new Utils();
        //Accept all directories and all gif, jpg, tiff, or png files.

        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }

            String extension = utils.getExtension(f);
            if (extension != null) {
                if (extension.equals(utils.tiff)
                        || extension.equals(utils.tif)
                        || extension.equals(utils.gif)
                        || extension.equals(utils.jpeg)
                        || extension.equals(utils.jpg)
                        || extension.equals(utils.png)
                        || extension.equals(utils.dds)) {
                    return true;
                } else {
                    return false;
                }
            }

            return false;
        }

        //The description of this filter
        public String getDescription() {
            return "Just Images";
        }
    }

    /**
     * restricts the file chooser to a specified directory tree, such as the assets folder
     */
    class DirectoryRestrictedFileSystemView extends FileSystemView {

        private final File[] rootDirectories;

        DirectoryRestrictedFileSystemView(File rootDirectory) {
            this.rootDirectories = new File[]{rootDirectory};
        }

        DirectoryRestrictedFileSystemView(File[] rootDirectories) {
            this.rootDirectories = rootDirectories;
        }

        @Override
        public Boolean isTraversable(File f) {
            if (f.getAbsolutePath().indexOf(rootDirectories[0].getAbsolutePath()) >= 0) {
                return Boolean.valueOf(f.isDirectory());
            } else {
                return false;
            }
        }

        @Override
        public File getDefaultDirectory() {
            return rootDirectories[0];
        }

        @Override
        public File getHomeDirectory() {
            return rootDirectories[0];
        }

        @Override
        public File[] getRoots() {
            return rootDirectories;
        }

        @Override
        public File createNewFolder(File containingDir) throws IOException {
            throw new UnsupportedOperationException("Unable to create directory");
        }
        /*
        @Override
        public File[] getRoots()
        {
        return rootDirectories;
        }

        @Override
        public boolean isRoot(File file)
        {
        for (File root : rootDirectories) {
        if (root.equals(file)) {
        return true;
        }
        }
        return false;
        }*/
    }

    public class Utils {

        public final String jpeg = "jpeg";
        public final String jpg = "jpg";
        public final String gif = "gif";
        public final String tiff = "tiff";
        public final String tif = "tif";
        public final String png = "png";
        public final String dds = "dds";

        /*
         * Get the extension of a file.
         */
        public String getExtension(File f) {
            String ext = null;
            String s = f.getName();
            int i = s.lastIndexOf('.');

            if (i > 0 && i < s.length() - 1) {
                ext = s.substring(i + 1).toLowerCase();
            }
            return ext;
        }

        /** Returns an ImageIcon, or null if the path was invalid. */
        protected ImageIcon createImageIcon(String path) {
            java.net.URL imgURL = Utils.class.getResource(path);
            if (imgURL != null) {
                return new ImageIcon(imgURL);
            } else {
                //System.err.println("Couldn't find file: " + path);
                return null;
            }
        }
    }
}
