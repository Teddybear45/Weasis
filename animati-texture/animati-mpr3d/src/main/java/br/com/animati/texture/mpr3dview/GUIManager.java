/*
 * @copyright Copyright (c) 2013 Animati Sistemas de Informática Ltda.
 * (http://www.animati.com.br)
 */
package br.com.animati.texture.mpr3dview;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Optional;

import javax.swing.BoundedRangeModel;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.prefs.Preferences;
import org.weasis.core.api.explorer.DataExplorerView;
import org.weasis.core.api.gui.util.ActionState;
import org.weasis.core.api.gui.util.ActionW;
import org.weasis.core.api.gui.util.BasicActionState;
import org.weasis.core.api.gui.util.ComboItemListener;
import org.weasis.core.api.gui.util.RadioMenuItem;
import org.weasis.core.api.gui.util.SliderChangeListener;
import org.weasis.core.api.gui.util.ToggleButtonListener;
import org.weasis.core.api.image.GridBagLayoutModel;
import org.weasis.core.api.image.LutShape;
import org.weasis.core.api.image.util.Unit;
import org.weasis.core.api.media.data.MediaSeries;
import org.weasis.core.api.service.AuditLog;
import org.weasis.core.api.service.BundlePreferences;
import org.weasis.core.api.service.BundleTools;
import org.weasis.core.ui.docking.DockableTool;
import org.weasis.core.ui.docking.UIManager;
import org.weasis.core.ui.editor.SeriesViewerEvent;
import org.weasis.core.ui.editor.SeriesViewerEvent.EVENT;
import org.weasis.core.ui.editor.image.DefaultView2d;
import org.weasis.core.ui.editor.image.ImageViewerEventManager;
import org.weasis.core.ui.editor.image.ImageViewerPlugin;
import org.weasis.core.ui.editor.image.MeasureToolBar;
import org.weasis.core.ui.editor.image.MouseActions;
import org.weasis.core.ui.editor.image.PannerListener;
import org.weasis.core.ui.editor.image.SynchData;
import org.weasis.core.ui.editor.image.SynchData.Mode;
import org.weasis.core.ui.editor.image.SynchEvent;
import org.weasis.core.ui.editor.image.SynchView;
import org.weasis.core.ui.editor.image.ViewCanvas;
import org.weasis.core.ui.editor.image.ViewerPlugin;
import org.weasis.core.ui.editor.image.ViewerToolBar;
import org.weasis.core.ui.editor.image.ZoomToolBar;
import org.weasis.core.ui.model.graphic.Graphic;
import org.weasis.core.ui.model.graphic.GraphicSelectionListener;
import org.weasis.core.ui.model.layer.LayerType;
import org.weasis.core.ui.model.utils.bean.PanPoint;
import org.weasis.dicom.codec.DicomImageElement;
import org.weasis.dicom.codec.SortSeriesStack;
import org.weasis.dicom.codec.display.PresetWindowLevel;
import org.weasis.dicom.explorer.DicomExplorer;
import org.weasis.dicom.explorer.DicomModel;
import org.weasis.dicom.viewer2d.Messages;
import org.weasis.dicom.viewer2d.ResetTools;
import org.weasis.dicom.viewer2d.View2dContainer;

import br.com.animati.texture.codec.StaticHelpers;
import br.com.animati.texture.codec.TextureDicomSeries;
import br.com.animati.texture.mpr3dview.ViewTexture.ViewType;
import br.com.animati.texture.mpr3dview.api.ActionWA;
import br.com.animati.texturedicom.ColorMask;
import br.com.animati.texturedicom.TextureImageCanvas;

/**
 *
 *
 * @author Gabriela Carla Bauerman (gabriela@animati.com.br)
 * @version 2013, 11 Sep.
 */
public class GUIManager extends ImageViewerEventManager<DicomImageElement> {

    public static List<ColorMask> colorMaskList = StaticHelpers.buildColorMaskList();
    public static List<StaticHelpers.TextureKernel> kernelList = StaticHelpers.buildKernelList();

    protected MouseActions mouseActions = new MouseActions(null);

    private GUIManager() {
        // Initialize actions with a null value. These are used by mouse or keyevent actions.
        setAction(new BasicActionState(ActionW.WINLEVEL));
        setAction(new BasicActionState(ActionW.CONTEXTMENU));
        setAction(new BasicActionState(ActionW.NO_ACTION));
        // setAction(new BasicActionState(ActionW.DRAW));
        setAction(new BasicActionState(ActionW.MEASURE));

        setAction(newScrollSeries());
        setAction(newVolumeQuality());
        setAction(newWindowAction());
        setAction(newLevelAction());
        setAction(newRotateAction());
        setAction(newZoomAction());
        setAction(newMipDepth());

        setAction(newFlipAction());
        setAction(newInverseLutAction());
        setAction(newInverseStackAction());
        setAction(newDrawOnlyOnceAction());
        setAction(newSmoothing());
        setAction(newVolumeSlicing());
        setAction(newVolumeLighting());

        setAction(newPresetAction());
        // setAction(newLutShapeAction());
        setAction(newLutAction());
        setAction(newFilterAction());
        setAction(newSortStackAction());
        setAction(newLayoutAction(
            View2dContainer.LAYOUT_LIST.toArray(new GridBagLayoutModel[View2dContainer.LAYOUT_LIST.size()])));
        setAction(newSynchAction(View2dContainer.SYNCH_LIST.toArray(new SynchView[View2dContainer.SYNCH_LIST.size()])));
        getAction(ActionW.SYNCH, ComboItemListener.class)
            .ifPresent(a -> a.setSelectedItemWithoutTriggerAction(SynchView.DEFAULT_STACK));
        setAction(newMeasurementAction(
            MeasureToolBar.measureGraphicList.toArray(new Graphic[MeasureToolBar.measureGraphicList.size()])));
        setAction(newSpatialUnit(Unit.values()));
        setAction(newMipOption());

        setAction(buildPanAction());
        setAction(newCrosshairAction());
        setAction(new BasicActionState(ActionW.RESET));
        setAction(new BasicActionState(ActionW.SHOW_HEADER));

        final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        Preferences prefs = BundlePreferences.getDefaultPreferences(context);
        zoomSetting.applyPreferences(prefs);
        mouseActions.applyPreferences(prefs);

        initializeParameters();
    }

    private void initializeParameters() {
        enableActions(false);
    }

    protected PannerListener buildPanAction() {
        return new PannerListener(ActionW.PAN, null) {

            @Override
            public void pointChanged(Point2D point) {
                firePropertyChange(ActionW.SYNCH.cmd(), null,
                    new SynchEvent(getSelectedViewPane(), getActionW().cmd(), point));
            }

            @Override
            public void mousePressed(MouseEvent e) {
                int buttonMask = getButtonMaskEx();
                if ((e.getModifiersEx() & buttonMask) != 0) {
                    pickPoint = e.getPoint();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int buttonMask = getButtonMaskEx();
                if (!e.isConsumed() && (e.getModifiersEx() & buttonMask) != 0) {
                    ViewCanvas panner = getViewCanvas(e);
                    if (panner != null) {
                        if (pickPoint != null && panner.getViewModel() != null) {
                            Point pt = e.getPoint();
                            setPoint(new PanPoint(PanPoint.State.DRAGGING, pt.x - pickPoint.x, pt.y - pickPoint.y));
                            pickPoint = pt;
                            panner.addPointerType(ViewCanvas.CENTER_POINTER);
                        }
                    }
                }
            }

        };
    }

    private SliderChangeListener newScrollSeries() {
        return new SliderChangeListener(ActionW.SCROLL_SERIES, 1, 100, 1, true, 0.1) {
            @Override
            public void stateChanged(BoundedRangeModel model) {
                firePropertyChange(ActionW.SYNCH.cmd(), null,
                    new SynchEvent(getSelectedViewPane(), getActionW().cmd(), model.getValue()));
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                setValue(getValue() + e.getWheelRotation());
            }
        };
    }

    private SliderChangeListener newVolumeQuality() {
        return new SliderChangeListener(ActionWA.VOLUM_QUALITY, 75, 2000, 300, true) {
            @Override
            public void stateChanged(BoundedRangeModel model) {
                firePropertyChange(ActionW.SYNCH.cmd(), null,
                    new SynchEvent(getSelectedViewPane(), getActionW().cmd(), model.getValue()));
            }
        };
    }

    private ComboItemListener newFilterAction() {
        return new ComboItemListener(ActionW.FILTER, kernelList.toArray()) {

            @Override
            public void itemStateChanged(Object object) {
                if (object instanceof StaticHelpers.TextureKernel) {
                    firePropertyChange(ActionW.SYNCH.cmd(), null,
                        new SynchEvent(getSelectedViewPane(), getActionW().cmd(), object));
                }
            }
        };
    }

    @Override
    protected SliderChangeListener newWindowAction() {
        return new SliderChangeListener(ActionW.WINDOW, WINDOW_SMALLEST, WINDOW_LARGEST, WINDOW_DEFAULT, true, 1.25) {

            @Override
            public void stateChanged(BoundedRangeModel model) {
                updatePreset(getActionW().cmd(), model.getValue());
            }
        };
    }

    @Override
    protected SliderChangeListener newLevelAction() {
        return new SliderChangeListener(ActionW.LEVEL, LEVEL_SMALLEST, LEVEL_LARGEST, LEVEL_DEFAULT, true, 1.25) {

            @Override
            public void stateChanged(BoundedRangeModel model) {
                updatePreset(getActionW().cmd(), model.getValue());
            }
        };
    }

    protected void updatePreset(String cmd, Object object) {
        String command = cmd;
        final PresetWindowLevel preset;
        Optional<ComboItemListener> presetAction = getAction(ActionW.PRESET, ComboItemListener.class);
        if (ActionW.PRESET.cmd().equals(command) && object instanceof PresetWindowLevel) {
            preset = (PresetWindowLevel) object;
            getAction(ActionW.WINDOW, SliderChangeListener.class)
                .ifPresent(a -> a.setValueWithoutTriggerAction(preset.getWindow().intValue()));
            getAction(ActionW.LEVEL, SliderChangeListener.class)
                .ifPresent(a -> a.setValueWithoutTriggerAction(preset.getLevel().intValue()));
            // getAction(ActionW.LUT_SHAPE, ComboItemListener.class)
            // .ifPresent(a -> a.setSelectedItemWithoutTriggerAction(preset.getLutShape()));
        } else {
            preset = (PresetWindowLevel) (object instanceof PresetWindowLevel ? object : null);
            presetAction.ifPresent(a -> a.setSelectedItemWithoutTriggerAction(preset));
        }

        SynchEvent evt = new SynchEvent(getSelectedViewPane(), command, object);
        evt.put(ActionW.PRESET.cmd(), preset);
        firePropertyChange(ActionW.SYNCH.cmd(), null, evt);
    };

    private ComboItemListener newPresetAction() {
        return new ComboItemListener(ActionW.PRESET, null) {

            @Override
            public void itemStateChanged(Object object) {
                updatePreset(getActionW().cmd(), object);
            }
        };
    }

    private ComboItemListener newLutShapeAction() {
        return new ComboItemListener(ActionW.LUT_SHAPE, LutShape.DEFAULT_FACTORY_FUNCTIONS.toArray()) {

            @Override
            public void itemStateChanged(Object object) {
                updatePreset(action.cmd(), object);
            }
        };
    }

    private ComboItemListener newLutAction() {

        return new ComboItemListener(ActionW.LUT, colorMaskList.toArray()) {

            @Override
            public void itemStateChanged(Object object) {
                firePropertyChange(ActionW.SYNCH.cmd(), null,
                    new SynchEvent(getSelectedViewPane(), action.cmd(), object));
            }
        };
    }

    private ComboItemListener newSortStackAction() {
        return new ComboItemListener(ActionW.SORTSTACK, SortSeriesStack.getValues()) {

            @Override
            public void itemStateChanged(Object object) {
                ImageViewerPlugin<DicomImageElement> container = GUIManager.getInstance().getSelectedView2dContainer();
                if (container != null) {
                    container.addSeries(GUIManager.getInstance().getSelectedSeries());
                }
            }
        };
    }

    @Override
    protected ToggleButtonListener newInverseStackAction() {
        return new ToggleButtonListener(ActionW.INVERSESTACK, false) {

            @Override
            public void actionPerformed(boolean selected) {
                ImageViewerPlugin<DicomImageElement> container = GUIManager.getInstance().getSelectedView2dContainer();
                if (container != null) {
                    container.addSeries(GUIManager.getInstance().getSelectedSeries());
                }
            }
        };
    }

    private ComboItemListener newMipOption() {

        return new ComboItemListener(ActionWA.MIP_OPTION,
            new TextureImageCanvas.MipOption[] { TextureImageCanvas.MipOption.None,
                TextureImageCanvas.MipOption.Minimum, TextureImageCanvas.MipOption.Average,
                TextureImageCanvas.MipOption.Maximum }) {

            @Override
            public void itemStateChanged(Object object) {
                getAction(ActionWA.MIP_DEPTH, SliderChangeListener.class)
                    .ifPresent(a -> a.enableAction(!(TextureImageCanvas.MipOption.None.equals(object))));
                firePropertyChange(ActionW.SYNCH.cmd(), null,
                    new SynchEvent(getSelectedViewPane(), action.cmd(), object));
            }
        };
    }

    private SliderChangeListener newMipDepth() {
        return new SliderChangeListener(ActionWA.MIP_DEPTH, 1, 100, 5, true) {

            @Override
            public void stateChanged(BoundedRangeModel model) {
                firePropertyChange(ActionW.SYNCH.cmd(), null,
                    new SynchEvent(getSelectedViewPane(), getActionW().cmd(), model.getValue()));
            }
        };
    }

    private ToggleButtonListener newVolumeLighting() {
        return new ToggleButtonListener(ActionWA.VOLUM_LIGHT, true) {
            @Override
            public void actionPerformed(boolean selected) {
                firePropertyChange(ActionW.SYNCH.cmd(), null,
                    new SynchEvent(getSelectedViewPane(), action.cmd(), selected));
            }
        };
    }

    private ToggleButtonListener newVolumeSlicing() {
        return new ToggleButtonListener(ActionWA.VOLUM_CENTER_SLICING, false) {
            @Override
            public void actionPerformed(boolean selected) {
                firePropertyChange(ActionW.SYNCH.cmd(), null,
                    new SynchEvent(getSelectedViewPane(), action.cmd(), selected));
            }
        };
    }

    private ToggleButtonListener newSmoothing() {
        return new ToggleButtonListener(ActionWA.SMOOTHING, true) {
            @Override
            public void actionPerformed(boolean selected) {
                firePropertyChange(ActionW.SYNCH.cmd(), null,
                    new SynchEvent(getSelectedViewPane(), action.cmd(), selected));
            }
        };
    }

    public static GUIManager getInstance() {
        return GUIManagerHolder.INSTANCE;
    }

    public List<ViewerPlugin<?>> getAllViewerPlugins() {
        return UIManager.VIEWER_PLUGINS;
    }

    public MediaSeries<DicomImageElement> getSelectedSeries() {
        ViewCanvas<DicomImageElement> pane = getSelectedViewPane();
        if (pane != null) {
            return pane.getSeries();
        }
        return null;
    }

    private static class GUIManagerHolder {
        private static final GUIManager INSTANCE = new GUIManager();
    }

    @Override
    public MouseActions getMouseActions() {
        return mouseActions;
    }

    public void setMouseActions(final MouseActions mActions) {
        mouseActions = mActions;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyEvent = e.getKeyCode();
        int modifiers = e.getModifiers();

        if (keyEvent == KeyEvent.VK_ESCAPE) {
            resetDisplay();
        } else {
            Optional<ComboItemListener> measure = getAction(ActionW.DRAW_MEASURE, ComboItemListener.class);
            if (measure.isPresent() && measure.get().isActionEnabled()) {
                ComboItemListener measureAction = measure.get();
                for (Object obj : measureAction.getAllItem()) {
                    if (obj instanceof Graphic) {
                        Graphic g = (Graphic) obj;
                        if (g.getKeyCode() == keyEvent && g.getModifier() == modifiers) {
                            ImageViewerPlugin<DicomImageElement> view = getSelectedView2dContainer();
                            if (view != null) {
                                final ViewerToolBar toolBar = view.getViewerToolBar();
                                if (toolBar != null) {
                                    String cmd = ActionW.MEASURE.cmd();
                                    if (!toolBar.isCommandActive(cmd)) {
                                        mouseActions.setAction(MouseActions.LEFT, cmd);
                                        if (view != null) {
                                            view.setMouseActions(mouseActions);
                                        }
                                        toolBar.changeButtonState(MouseActions.LEFT, cmd);
                                    }
                                }
                            }
                            measureAction.setSelectedItem(obj);
                            return;
                        }
                    }
                }
            }

            Optional<ComboItemListener> presetAction = getAction(ActionW.PRESET, ComboItemListener.class);
            if (modifiers == 0 && presetAction.isPresent() && presetAction.get().isActionEnabled()) {
                ComboItemListener presetComboListener = presetAction.get();
                DefaultComboBoxModel model = presetComboListener.getModel();
                for (int i = 0; i < model.getSize(); i++) {
                    PresetWindowLevel val = (PresetWindowLevel) model.getElementAt(i);
                    if (val.getKeyCode() == keyEvent) {
                        presetComboListener.setSelectedItem(val);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    public DicomModel getActiveDicomModel() {
        DataExplorerView explorerplugin = UIManager.getExplorerplugin(DicomExplorer.NAME);
        return (DicomModel) explorerplugin.getDataExplorerModel();
    }

    @Override
    public void setSelectedView2dContainer(ImageViewerPlugin<DicomImageElement> selectedView2dContainer) {
        if (this.selectedView2dContainer != null) {
            this.selectedView2dContainer.setMouseActions(null);
            this.selectedView2dContainer.setDrawActions(null);
        }
        ImageViewerPlugin<DicomImageElement> oldContainer = this.selectedView2dContainer;
        this.selectedView2dContainer = selectedView2dContainer;

        if (selectedView2dContainer != null) {
            Optional<ComboItemListener> synchAction = getAction(ActionW.SYNCH, ComboItemListener.class);
            Optional<ComboItemListener> layoutAction = getAction(ActionW.LAYOUT, ComboItemListener.class);
            if (oldContainer == null || !oldContainer.getClass().equals(selectedView2dContainer.getClass())) {
                synchAction.ifPresent(
                    a -> a.setDataListWithoutTriggerAction(selectedView2dContainer.getSynchList().toArray()));
                layoutAction.ifPresent(
                    a -> a.setDataListWithoutTriggerAction(selectedView2dContainer.getLayoutList().toArray()));

            }
            if (oldContainer != null) {
                ViewCanvas<DicomImageElement> pane = oldContainer.getSelectedImagePane();
                if (pane != null) {
                    pane.setFocused(false);
                }
            }
            synchAction.ifPresent(a -> a.setSelectedItemWithoutTriggerAction(selectedView2dContainer.getSynchView()));
            layoutAction.ifPresent(
                a -> a.setSelectedItemWithoutTriggerAction(selectedView2dContainer.getOriginalLayoutModel()));

            updateComponentsListener(selectedView2dContainer.getSelectedImagePane());
            selectedView2dContainer.setMouseActions(mouseActions);
            Graphic graphic = null;
            ActionState action = getAction(ActionW.DRAW_MEASURE);
            if (action instanceof ComboItemListener) {
                graphic = (Graphic) ((ComboItemListener) action).getSelectedItem();
            }
            selectedView2dContainer.setDrawActions(graphic);
            ViewCanvas<DicomImageElement> pane = selectedView2dContainer.getSelectedImagePane();
            if (pane != null) {
                pane.setFocused(true);
                fireSeriesViewerListeners(
                    new SeriesViewerEvent(selectedView2dContainer, pane.getSeries(), null, EVENT.SELECT_VIEW));
            }
        }
    }

    @Override
    public boolean updateComponentsListener(ViewCanvas<DicomImageElement> view2d) {
        if (view2d == null) {
            return false;
        }

        if (selectedView2dContainer == null || view2d != selectedView2dContainer.getSelectedImagePane()) {
            return false;
        }

        clearAllPropertyChangeListeners();
        Optional<SliderChangeListener> cineAction = getAction(ActionW.SCROLL_SERIES, SliderChangeListener.class);

        if (view2d instanceof ViewTexture == false || view2d.getSeries() == null) {
            enableActions(false);
            return false;
        }

        if (!enabledAction) {
            enableActions(true);
        }

        updateWindowLevelComponentsListener((ViewTexture) view2d);

        getAction(ActionW.LUT, ComboItemListener.class)
            .ifPresent(a -> a.setSelectedItemWithoutTriggerAction(view2d.getActionValue(ActionW.LUT.cmd())));
        getAction(ActionW.INVERT_LUT, ToggleButtonListener.class).ifPresent(
            a -> a.setSelectedWithoutTriggerAction((Boolean) view2d.getActionValue(ActionW.INVERT_LUT.cmd())));
        getAction(ActionW.FILTER, ComboItemListener.class)
            .ifPresent(a -> a.setSelectedItemWithoutTriggerAction(view2d.getActionValue(ActionW.FILTER.cmd())));
        getAction(ActionW.ROTATION, SliderChangeListener.class)
            .ifPresent(a -> a.setValueWithoutTriggerAction((Integer) view2d.getActionValue(ActionW.ROTATION.cmd())));
        getAction(ActionW.FLIP, ToggleButtonListener.class)
            .ifPresent(a -> a.setSelectedWithoutTriggerAction((Boolean) view2d.getActionValue(ActionW.FLIP.cmd())));

        getAction(ActionWA.SMOOTHING, ToggleButtonListener.class).ifPresent(
            a -> a.setSelectedWithoutTriggerAction((Boolean) view2d.getActionValue(ActionWA.SMOOTHING.cmd())));

        getAction(ActionW.ZOOM, SliderChangeListener.class).ifPresent(a -> a.setValueWithoutTriggerAction(
            viewScaleToSliderValue(Math.abs((Double) view2d.getActionValue(ActionW.ZOOM.cmd())))));
        getAction(ActionW.SPATIAL_UNIT, ComboItemListener.class)
            .ifPresent(a -> a.setSelectedItemWithoutTriggerAction(view2d.getActionValue(ActionW.SPATIAL_UNIT.cmd())));

        getAction(ActionWA.MIP_OPTION, ComboItemListener.class)
            .ifPresent(a -> a.setSelectedItemWithoutTriggerAction(view2d.getActionValue(ActionWA.MIP_OPTION.cmd())));

        cineAction.ifPresent(a -> a.setMinMaxValueWithoutTriggerAction(1,
            ((TextureImageCanvas) view2d).getTotalSlices(), view2d.getFrameIndex()));

        getAction(ActionWA.MIP_DEPTH, SliderChangeListener.class).ifPresent(
            a -> a.setMinMaxValueWithoutTriggerAction(1, cineAction.isPresent() ? cineAction.get().getMax() : 1,
                (Integer) view2d.getActionValue(ActionWA.MIP_DEPTH.cmd())));

        boolean volume = ViewType.VOLUME3D.equals(((ViewTexture) view2d).getViewType());
        Optional<ToggleButtonListener> volumeLighting = getAction(ActionWA.VOLUM_LIGHT, ToggleButtonListener.class);
        Optional<ToggleButtonListener> volumeSlicing =
            getAction(ActionWA.VOLUM_CENTER_SLICING, ToggleButtonListener.class);
        Optional<SliderChangeListener> volumeQuality = getAction(ActionWA.VOLUM_QUALITY, SliderChangeListener.class);
        if (volume) {
            volumeLighting.ifPresent(
                a -> a.setSelectedWithoutTriggerAction((Boolean) view2d.getActionValue(ActionWA.VOLUM_LIGHT.cmd())));
            volumeSlicing.ifPresent(a -> a
                .setSelectedWithoutTriggerAction((Boolean) view2d.getActionValue(ActionWA.VOLUM_CENTER_SLICING.cmd())));
            volumeQuality.ifPresent(
                a -> a.setValueWithoutTriggerAction((Integer) view2d.getActionValue(ActionWA.VOLUM_QUALITY.cmd())));
        }
        volumeLighting.ifPresent(a -> a.enableAction(volume));
        volumeSlicing.ifPresent(a -> a.enableAction(volume));
        volumeQuality.ifPresent(a -> a.enableAction(volume));

        getAction(ActionW.SORTSTACK, ComboItemListener.class)
            .ifPresent(a -> a.setSelectedItemWithoutTriggerAction(view2d.getActionValue(ActionW.SORTSTACK.cmd())));
        getAction(ActionW.INVERSESTACK, ToggleButtonListener.class).ifPresent(
            a -> a.setSelectedWithoutTriggerAction((Boolean) view2d.getActionValue(ActionW.INVERSESTACK.cmd())));

        // register all actions for the selected view and for the other views register according to synchview.
        ComboItemListener synchAtction = getAction(ActionW.SYNCH, ComboItemListener.class).orElse(null);
        updateAllListeners(selectedView2dContainer,
            synchAtction == null ? SynchView.NONE : (SynchView) synchAtction.getSelectedItem());

        List<DockableTool> tools = selectedView2dContainer.getToolPanel();
        synchronized (tools) {
            for (DockableTool p : tools) {
                if (p instanceof GraphicSelectionListener) {
                    view2d.getGraphicManager().addGraphicSelectionListener((GraphicSelectionListener) p);
                }
            }
        }
        return true;
    }

    private void updateWindowLevelComponentsListener(ViewTexture view2d) {

        TextureDicomSeries series = view2d.getSeriesObject();
        if (series != null) {
            int fullDynamicWidth = series.windowingMaxInValue - series.windowingMinInValue;

            getAction(ActionW.WINDOW, SliderChangeListener.class)
                .ifPresent(a -> a.setMinMaxValueWithoutTriggerAction(1, fullDynamicWidth, view2d.windowingWindow));
            getAction(ActionW.LEVEL, SliderChangeListener.class)
                .ifPresent(a -> a.setMinMaxValueWithoutTriggerAction(series.windowingMinInValue,
                    series.windowingMaxInValue, view2d.windowingLevel));

            Optional<ComboItemListener> presetAction = getAction(ActionW.PRESET, ComboItemListener.class);
            List<PresetWindowLevel> presetList = series.getPresetList(true, false);
            if (presetList != null) {
                presetAction.ifPresent(a -> a.setDataListWithoutTriggerAction(presetList.toArray()));
            }
            presetAction
                .ifPresent(a -> a.setSelectedItemWithoutTriggerAction(view2d.getActionValue(ActionW.PRESET.cmd())));
        }
    }

    @Override
    public void resetDisplay() {
        reset(ResetTools.All);
    }

    public void reset(ResetTools action) {
        AuditLog.LOGGER.info("reset action:{}", action.name()); //$NON-NLS-1$
        if (ResetTools.All.equals(action)) {
            firePropertyChange(ActionW.SYNCH.cmd(), null,
                new SynchEvent(getSelectedViewPane(), ActionW.RESET.cmd(), true));
        } else if (ResetTools.Zoom.equals(action)) {
            // Pass the value 0.0 (convention: default value according the zoom type) directly to the property change,
            // otherwise the value is adjusted by the BoundedRangeModel
            firePropertyChange(ActionW.SYNCH.cmd(), null,
                new SynchEvent(getSelectedViewPane(), ActionW.ZOOM.cmd(), 0.0));

        } else if (ResetTools.Rotation.equals(action)) {
            getAction(ActionW.ROTATION, SliderChangeListener.class).ifPresent(a -> a.setValue(0));
        } else if (ResetTools.WindowLevel.equals(action)) {
            getAction(ActionW.PRESET, ComboItemListener.class).ifPresent(a -> a.setSelectedItem(a.getFirstItem()));
        } else if (ResetTools.Pan.equals(action)) {
            if (selectedView2dContainer != null) {
                ViewCanvas viewPane = selectedView2dContainer.getSelectedImagePane();
                if (viewPane != null) {
                    viewPane.resetPan();
                    viewPane.getJComponent().repaint();
                }
            }
        }
    }

    @Override
    public void updateAllListeners(ImageViewerPlugin<DicomImageElement> viewerPlugin, SynchView synchView) {
        clearAllPropertyChangeListeners();

        if (viewerPlugin != null) {
            ViewCanvas<DicomImageElement> viewPane = viewerPlugin.getSelectedImagePane();
            // if (viewPane == null || viewPane.getSeries() == null) {
            if (viewPane == null) {
                return;
            }
            SynchData synch = synchView.getSynchData();
            MediaSeries<DicomImageElement> series = viewPane.getSeries();
            if (series != null) {
                SynchData oldSynch = (SynchData) viewPane.getActionValue(ActionW.SYNCH_LINK.cmd());
                if (oldSynch == null || !oldSynch.getMode().equals(synch.getMode())) {
                    oldSynch = synch;
                }
                viewPane.setActionsInView(ActionW.SYNCH_LINK.cmd(), null);
                addPropertyChangeListener(ActionW.SYNCH.cmd(), viewPane);

                final List<ViewCanvas<DicomImageElement>> panes = viewerPlugin.getImagePanels();
                panes.remove(viewPane);
                viewPane.setActionsInView(ActionW.SYNCH_CROSSLINE.cmd(), false);

                if (SynchView.NONE.equals(synchView) || (viewPane instanceof ViewTexture
                    && ((ViewTexture) viewPane).getViewType() == ViewType.VOLUME3D)) {
                    for (int i = 0; i < panes.size(); i++) {
                        ViewCanvas<DicomImageElement> pane = panes.get(i);
                        pane.getGraphicManager().deleteByLayerType(LayerType.CROSSLINES);

                        oldSynch = (SynchData) pane.getActionValue(ActionW.SYNCH_LINK.cmd());
                        if (oldSynch == null || !oldSynch.getMode().equals(synch.getMode())) {
                            oldSynch = synch;
                        }
                        pane.setActionsInView(ActionW.SYNCH_LINK.cmd(), oldSynch);
                        // pane.updateSynchState();
                    }
                } else {
                    // TODO if Pan is activated than rotation is required
                    if (Mode.Stack.equals(synch.getMode())) {
                        for (int i = 0; i < panes.size(); i++) {
                            ViewCanvas<DicomImageElement> pane = panes.get(i);
                            pane.getGraphicManager().deleteByLayerType(LayerType.CROSSLINES);

                            MediaSeries<DicomImageElement> s = pane.getSeries();
                            if (s != null) {
                                oldSynch = (SynchData) pane.getActionValue(ActionW.SYNCH_LINK.cmd());
                                if (oldSynch == null || !oldSynch.getMode().equals(synch.getMode())) {
                                    oldSynch = synch.copy();
                                }
                                if (pane instanceof ViewTexture
                                    && ((ViewTexture) pane).getViewType() != ViewType.VOLUME3D) {
                                    addPropertyChangeListener(ActionW.SYNCH.cmd(), pane);
                                }

                                pane.setActionsInView(ActionW.SYNCH_LINK.cmd(), oldSynch);
                                // pane.updateSynchState();
                            }
                        }
                    }
                }
            }

            // viewPane.updateSynchState();
        }
    }

    public JMenu getResetMenu(String prop) {
        JMenu menu = null;
        if (BundleTools.SYSTEM_PREFERENCES.getBooleanProperty(prop, true)) {
            ButtonGroup group = new ButtonGroup();
            menu = new JMenu("Reset");
            menu.setIcon(new ImageIcon(DefaultView2d.class.getResource("/icon/16x16/reset.png"))); //$NON-NLS-1$
            menu.setEnabled(getSelectedSeries() != null);

            if (menu.isEnabled()) {
                for (final ResetTools action : ResetTools.values()) {
                    final JMenuItem item = new JMenuItem(action.toString());
                    item.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            reset(action);
                        }
                    });
                    menu.add(item);
                    group.add(item);
                }
            }
        }
        return menu;
    }

    public JMenu getPresetMenu(String prop) {
        JMenu menu = null;
        if (BundleTools.SYSTEM_PREFERENCES.getBooleanProperty(prop, true)) {
            Optional<ComboItemListener> presetAction = getAction(ActionW.PRESET, ComboItemListener.class);
            if (presetAction.isPresent()) {
                menu = presetAction.get().createUnregisteredRadioMenu(Messages.getString("View2dContainer.presets"));//$NON-NLS-1$
                menu.setIcon(ActionW.WINLEVEL.getSmallIcon());
                for (Component mitem : menu.getMenuComponents()) {
                    RadioMenuItem ritem = (RadioMenuItem) mitem;
                    PresetWindowLevel preset = (PresetWindowLevel) ritem.getUserObject();
                    if (preset.getKeyCode() > 0) {
                        ritem.setAccelerator(KeyStroke.getKeyStroke(preset.getKeyCode(), 0));
                    }
                }
            }
        }
        return menu;
    }

    public JMenu getLutShapeMenu(String prop) {
        JMenu menu = null;
        if (BundleTools.SYSTEM_PREFERENCES.getBooleanProperty(prop, true)) {
            // menu = lutShapeAction.createUnregisteredRadioMenu(ActionW.LUT_SHAPE.getTitle());
        }
        return menu;
    }

    public JMenu getZoomMenu(String prop) {
        JMenu menu = null;
        if (BundleTools.SYSTEM_PREFERENCES.getBooleanProperty(prop, true)) {
            Optional<SliderChangeListener> zoomAction = getAction(ActionW.ZOOM, SliderChangeListener.class);
            if (zoomAction.isPresent()) {
                menu = new JMenu(ActionW.ZOOM.getTitle());
                menu.setIcon(ActionW.ZOOM.getSmallIcon());
                menu.setEnabled(zoomAction.get().isActionEnabled());

                if (zoomAction.get().isActionEnabled()) {
                    for (JMenuItem jMenuItem : ZoomToolBar.getZoomListMenuItems(this)) {
                        menu.add(jMenuItem);
                    }
                }
            }
        }
        return menu;
    }

    public JMenu getOrientationMenu(String prop) {
        JMenu menu = null;
        if (BundleTools.SYSTEM_PREFERENCES.getBooleanProperty(prop, true)) {
            Optional<SliderChangeListener> rotateAction = getAction(ActionW.ROTATION, SliderChangeListener.class);
            if (rotateAction.isPresent()) {
                menu = new JMenu(Messages.getString("View2dContainer.orientation")); //$NON-NLS-1$
                menu.setIcon(ActionW.ROTATION.getSmallIcon());
                menu.setEnabled(rotateAction.get().isActionEnabled());

                if (rotateAction.get().isActionEnabled()) {
                    JMenuItem menuItem = new JMenuItem(Messages.getString("ResetTools.reset")); //$NON-NLS-1$
                    menuItem.addActionListener(e -> rotateAction.get().setValue(0));
                    menu.add(menuItem);
                    menuItem = new JMenuItem(Messages.getString("View2dContainer.-90")); //$NON-NLS-1$
                    menuItem.addActionListener(
                        e -> rotateAction.get().setValue((rotateAction.get().getValue() - 90 + 360) % 360));
                    menu.add(menuItem);
                    menuItem = new JMenuItem(Messages.getString("View2dContainer.+90")); //$NON-NLS-1$
                    menuItem.addActionListener(
                        e -> rotateAction.get().setValue((rotateAction.get().getValue() + 90) % 360));
                    menu.add(menuItem);
                    menuItem = new JMenuItem(Messages.getString("View2dContainer.+180")); //$NON-NLS-1$
                    menuItem.addActionListener(
                        e -> rotateAction.get().setValue((rotateAction.get().getValue() + 180) % 360));
                    menu.add(menuItem);

                    Optional<ToggleButtonListener> flipAction = getAction(ActionW.FLIP, ToggleButtonListener.class);
                    if (flipAction.isPresent()) {
                        menu.add(new JSeparator());
                        menu.add(flipAction.get()
                            .createUnregiteredJCheckBoxMenuItem(Messages.getString("View2dContainer.flip_h"))); //$NON-NLS-1$
                    }
                }
            }
        }
        return menu;
    }

    public JMenu getSortStackMenu(String prop) {
        JMenu menu = null;
        if (BundleTools.SYSTEM_PREFERENCES.getBooleanProperty(prop, true)) {
            Optional<ComboItemListener> sortStackAction = getAction(ActionW.SORTSTACK, ComboItemListener.class);
            if (sortStackAction.isPresent()) {
                menu =
                    sortStackAction.get().createUnregisteredRadioMenu(Messages.getString("View2dContainer.sort_stack")); //$NON-NLS-1$
                Optional<ToggleButtonListener> inverseStackAction =
                    getAction(ActionW.INVERSESTACK, ToggleButtonListener.class);
                if (inverseStackAction.isPresent()) {
                    menu.add(new JSeparator());
                    menu.add(inverseStackAction.get()
                        .createUnregiteredJCheckBoxMenuItem(Messages.getString("View2dContainer.inv_stack"))); //$NON-NLS-1$
                }
            }
        }
        return menu;
    }

    public JMenu getLutMenu(String prop) {
        JMenu menu = null;
        if (BundleTools.SYSTEM_PREFERENCES.getBooleanProperty(prop, true)) {
            Optional<ComboItemListener> lutAction = getAction(ActionW.LUT, ComboItemListener.class);
            if (lutAction.isPresent()) {
                menu = lutAction.get().createUnregisteredRadioMenu(Messages.getString("ImageTool.lut"));//$NON-NLS-1$
            }
        }
        return menu;
    }

    public JCheckBoxMenuItem getLutInverseMenu(String prop) {
        JCheckBoxMenuItem menu = null;
        if (BundleTools.SYSTEM_PREFERENCES.getBooleanProperty(prop, true)) {
            Optional<ToggleButtonListener> inverseLutAction = getAction(ActionW.INVERT_LUT, ToggleButtonListener.class);
            if (inverseLutAction.isPresent()) {
                menu = inverseLutAction.get().createUnregiteredJCheckBoxMenuItem(ActionW.INVERT_LUT.getTitle());
            }
        }
        return menu;
    }

    public JMenu getFilterMenu(String prop) {
        JMenu menu = null;
        if (BundleTools.SYSTEM_PREFERENCES.getBooleanProperty(prop, true)) {
            Optional<ComboItemListener> filterAction = getAction(ActionW.FILTER, ComboItemListener.class);
            if (filterAction.isPresent()) {
                menu = filterAction.get().createUnregisteredRadioMenu(Messages.getString("ImageTool.filter")); //$NON-NLS-1$
            }
        }
        return menu;
    }

}