/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.dicom.viewer2d.mpr;

import java.awt.Component;
import java.util.Map;
import javax.swing.Icon;
import org.weasis.core.api.explorer.DataExplorerView;
import org.weasis.core.api.explorer.model.DataExplorerModel;
import org.weasis.core.api.image.GridBagLayoutModel;
import org.weasis.core.api.media.data.MediaElement;
import org.weasis.core.api.media.data.MediaSeries;
import org.weasis.core.api.service.BundleTools;
import org.weasis.core.api.util.ResourceUtil;
import org.weasis.core.api.util.ResourceUtil.OtherIcon;
import org.weasis.core.ui.docking.UIManager;
import org.weasis.core.ui.editor.SeriesViewer;
import org.weasis.core.ui.editor.SeriesViewerFactory;
import org.weasis.core.ui.editor.ViewerPluginBuilder;
import org.weasis.core.util.StringUtil;
import org.weasis.dicom.codec.DicomMediaIO;
import org.weasis.dicom.explorer.DicomExplorer;
import org.weasis.dicom.explorer.DicomModel;
import org.weasis.dicom.viewer2d.Messages;
import org.weasis.dicom.viewer2d.mpr.MprView.SliceOrientation;

@org.osgi.service.component.annotations.Component(service = SeriesViewerFactory.class)
public class MprFactory implements SeriesViewerFactory {

  public static final String NAME = Messages.getString("MPRFactory.title");
  public static final String P_DEFAULT_LAYOUT = "mpr.default.layout";

  @Override
  public Icon getIcon() {
    return ResourceUtil.getIcon(OtherIcon.VIEW_3D);
  }

  @Override
  public String getUIName() {
    return NAME;
  }

  @Override
  public String getDescription() {
    return Messages.getString("MPRFactory.desc");
  }

  public static GridBagLayoutModel getDefaultGridBagLayoutModel() {
    String defLayout = BundleTools.SYSTEM_PREFERENCES.getProperty(MprFactory.P_DEFAULT_LAYOUT);
    if (StringUtil.hasText(defLayout)) {
      return MprContainer.LAYOUT_LIST.stream()
          .filter(g -> defLayout.equals(g.getId()))
          .findFirst()
          .orElse(MprContainer.view1);
    }
    return MprContainer.view1;
  }

  @Override
  public SeriesViewer<?> createSeriesViewer(Map<String, Object> properties) {
    GridBagLayoutModel model = getDefaultGridBagLayoutModel();
    String uid = null;
    if (properties != null) {
      Object obj = properties.get(org.weasis.core.api.image.GridBagLayoutModel.class.getName());
      if (obj instanceof GridBagLayoutModel layoutModel) {
        model = layoutModel;
      }
      // Set UID
      Object val = properties.get(ViewerPluginBuilder.UID);
      if (val instanceof String str) {
        uid = str;
      }
    }

    MprContainer instance = new MprContainer(model, uid);
    if (properties != null) {
      Object obj = properties.get(DataExplorerModel.class.getName());
      if (obj instanceof DicomModel m) {
        // Register the PropertyChangeListener
        m.addPropertyChangeListener(instance);
      }
    }
    int index = 0;
    for (Component val : model.getConstraints().values()) {
      if (val instanceof MprView mprView) {
        SliceOrientation sliceOrientation =
            switch (index) {
              case 1 -> SliceOrientation.CORONAL;
              case 2 -> SliceOrientation.SAGITTAL;
              default -> SliceOrientation.AXIAL;
            };
        mprView.setType(sliceOrientation);
        index++;
      }
    }
    return instance;
  }

  public static void closeSeriesViewer(MprContainer mprContainer) {
    // Unregister the PropertyChangeListener
    DataExplorerView dicomView = UIManager.getExplorerPlugin(DicomExplorer.NAME);
    if (dicomView != null) {
      dicomView.getDataExplorerModel().removePropertyChangeListener(mprContainer);
    }
  }

  @Override
  public boolean canReadMimeType(String mimeType) {
    return DicomMediaIO.SERIES_MIMETYPE.equals(mimeType);
  }

  @Override
  public boolean isViewerCreatedByThisFactory(SeriesViewer<? extends MediaElement> viewer) {
    return viewer instanceof MprContainer;
  }

  @Override
  public int getLevel() {
    return 15;
  }

  @Override
  public boolean canAddSeries() {
    return false;
  }

  @Override
  public boolean canExternalizeSeries() {
    return true;
  }

  @Override
  public boolean canReadSeries(MediaSeries<?> series) {
    return series != null && series.isSuitableFor3d();
  }
}
