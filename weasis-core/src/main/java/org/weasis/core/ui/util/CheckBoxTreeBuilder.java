/*
 * Copyright (c) 2022 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.ui.util;

import it.cnr.imaa.essi.lablib.gui.checkboxtree.DefaultCheckboxTreeCellRenderer;
import java.util.Enumeration;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

public class CheckBoxTreeBuilder {

  private CheckBoxTreeBuilder() {}

  public static DefaultCheckboxTreeCellRenderer buildNoIconCheckboxTreeCellRenderer() {
    DefaultCheckboxTreeCellRenderer renderer = new DefaultCheckboxTreeCellRenderer();
    renderer.setOpenIcon(null);
    renderer.setClosedIcon(null);
    renderer.setLeafIcon(null);
    return renderer;
  }

  public static void expandTree(JTree tree, DefaultMutableTreeNode start) {
    Enumeration<?> children = start.children();
    while (children.hasMoreElements()) {
      Object child = children.nextElement();
      if (child instanceof DefaultMutableTreeNode dtm && !dtm.isLeaf()) {
        TreePath tp = new TreePath(dtm.getPath());
        tree.expandPath(tp);
        expandTree(tree, dtm);
      }
    }
  }
}
