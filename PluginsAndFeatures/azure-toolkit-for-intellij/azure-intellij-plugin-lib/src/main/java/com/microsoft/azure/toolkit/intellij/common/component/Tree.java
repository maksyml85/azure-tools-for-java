/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.component;

import com.google.common.collect.Sets;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.ui.ComponentUtil;
import com.intellij.ui.LoadingNode;
import com.intellij.ui.TreeUIHelper;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.ui.tree.TreeUtil;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.component.NodeView;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.view.IView;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.intellij.ui.AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED;

@Getter
public class Tree extends SimpleTree implements DataProvider {
    protected Node<?> root;

    public Tree() {
        super();
    }

    public Tree(Node<?> root) {
        super();
        this.root = root;
        init(root);
    }

    protected void init(@Nonnull Node<?> root) {
        ComponentUtil.putClientProperty(this, ANIMATION_IN_RENDERER_ALLOWED, true);
        this.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        TreeUtil.installActions(this);
        TreeUIHelper.getInstance().installTreeSpeedSearch(this);
        TreeUIHelper.getInstance().installSmartExpander(this);
        TreeUIHelper.getInstance().installSelectionSaver(this);
        TreeUIHelper.getInstance().installEditSourceOnEnterKeyHandler(this);
        this.setCellRenderer(new NodeRenderer());
        this.setModel(new DefaultTreeModel(new TreeNode<>(root, this)));
        TreeUtils.installExpandListener(this);
        TreeUtils.installSelectionListener(this);
        TreeUtils.installMouseListener(this);
    }

    @Override
    public @Nullable Object getData(@Nonnull String dataId) {
        if (StringUtils.equals(dataId, Action.SOURCE)) {
            final DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) this.getLastSelectedPathComponent();
            if (Objects.nonNull(selectedNode)) {
                return selectedNode.getUserObject();
            }
        }
        return null;
    }

    @EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
    public static class TreeNode<T> extends DefaultMutableTreeNode implements NodeView.Refresher {
        @Nonnull
        @EqualsAndHashCode.Include
        protected final Node<T> inner;
        protected final JTree tree;
        Boolean loaded = null; //null:not loading/loaded, false: loading: true: loaded

        public TreeNode(@Nonnull Node<T> n, JTree tree) {
            super(n.data(), n.hasChildren());
            this.inner = n;
            this.tree = tree;
            if (this.getAllowsChildren()) {
                this.add(new LoadingNode());
            }
            if (!this.inner.lazy()) {
                this.loadChildren();
            }
            final NodeView view = this.inner.view();
            view.setRefresher(this);
        }

        @Override
        @EqualsAndHashCode.Include
        // NOTE: equivalent nodes in same tree will cause rendering problems.
        public javax.swing.tree.TreeNode getParent() {
            return super.getParent();
        }

        public T getData() {
            return this.inner.data();
        }

        public String getLabel() {
            return this.inner.view().getLabel();
        }

        public List<IView.Label> getInlineActionViews() {
            return this.inner.inlineActionList().stream().map(action -> action.getView(this.inner.data()))
                    .filter(IView.Label::isEnabled)
                    .collect(Collectors.toList());
        }

        @Override
        public void refreshView() {
            synchronized (this.tree) {
                final DefaultTreeModel model = (DefaultTreeModel) this.tree.getModel();
                if (Objects.nonNull(model) && (Objects.nonNull(this.getParent()) || Objects.equals(model.getRoot(), this))) {
                    model.nodeChanged(this);
                }
            }
        }

        private void refreshChildrenView() {
            synchronized (this.tree) {
                final DefaultTreeModel model = (DefaultTreeModel) this.tree.getModel();
                if (Objects.nonNull(model) && (Objects.nonNull(this.getParent()) || Objects.equals(model.getRoot(), this))) {
                    model.nodeStructureChanged(this);
                }
            }
        }

        @Override
        @AzureOperation(name = "user/common.load_children.node", params = "this.getLabel()")
        public synchronized void refreshChildren(boolean... incremental) {
            if (this.getAllowsChildren() && BooleanUtils.isNotFalse(this.loaded)) {
                final DefaultTreeModel model = (DefaultTreeModel) this.tree.getModel();
                if (incremental.length > 0 && incremental[0] && Objects.nonNull(model)) {
                    this.removeLoadMoreNode();
                    this.refreshChildrenView();
                    model.insertNodeInto(new LoadingNode(), this, this.getChildCount());
                } else {
                    this.removeAllChildren();
                    this.add(new LoadingNode());
                    this.refreshChildrenView();
                }
                this.loaded = null;
                this.loadChildren(incremental);
            }
        }

        protected synchronized void loadChildren(boolean... incremental) {
            if (loaded != null) {
                return; // return if loading/loaded
            }
            this.loaded = false;
            final AzureTaskManager tm = AzureTaskManager.getInstance();
            tm.runOnPooledThread(() -> {
                final List<Node<?>> children = this.inner.getChildren();
                if (incremental.length > 0 && incremental[0]) {
                    tm.runLater(() -> updateChildren(children));
                } else {
                    tm.runLater(() -> setChildren(children));
                }
            });
        }

        private synchronized void setChildren(List<Node<?>> children) {
            this.removeAllChildren();
            children.stream().map(c -> new TreeNode<>(c, this.tree)).forEach(this::add);
            this.addLoadMoreNode();
            this.loaded = true;
            this.refreshChildrenView();
        }

        private synchronized void updateChildren(List<Node<?>> children) {
            final Map<Object, DefaultMutableTreeNode> oldChildren = IntStream.range(0, this.getChildCount() - 1).mapToObj(this::getChildAt)
                .filter(n -> n instanceof DefaultMutableTreeNode).map(n -> ((DefaultMutableTreeNode) n))
                .collect(Collectors.toMap(DefaultMutableTreeNode::getUserObject, n -> n));

            final Set<Object> newChildrenData = children.stream().map(Node::data).collect(Collectors.toSet());
            final Set<Object> oldChildrenData = oldChildren.keySet();
            Sets.difference(oldChildrenData, newChildrenData).forEach(o -> oldChildren.get(o).removeFromParent());

            TreePath toSelect = null;
            if (this.inner.newItemOrder() == Node.Order.LIST_ORDER) {
                for (int i = 0; i < children.size(); i++) {
                    final Node<?> node = children.get(i);
                    if (!oldChildrenData.contains(node.data())) {
                        final TreeNode<?> treeNode = new TreeNode<>(node, this.tree);
                        this.insert(treeNode, i);
                        toSelect = new TreePath(treeNode.getPath());
                    } else { // discarded nodes should be disposed manually to unregister listeners.
                        node.dispose();
                    }
                }
            } else {
                final List<Node<?>> newChildren = children.stream()
                    .filter(c -> !oldChildrenData.contains(c.data())).toList();
                newChildren.forEach(node -> this.insert(new TreeNode<>(node, this.tree), getChildCount()));
            }

            this.removeLoadingNode();
            this.addLoadMoreNode();
            this.refreshChildrenView();
//            Optional.ofNullable(toSelect)
//                .filter(s -> ((DefaultMutableTreeNode) s.getPathComponent(1)).getUserObject() instanceof AzureResources) //  is node in app-centric view.
//                .ifPresent(p -> TreeUtil.selectPath(this.tree, p, false));
            this.loaded = true;
        }

        public synchronized void clearChildren() {
            synchronized (this.tree) {
                this.removeAllChildren();
                this.loaded = null;
                if (this.getAllowsChildren()) {
                    this.add(new LoadingNode());
                    this.tree.collapsePath(new TreePath(this.getPath()));
                }
                this.refreshChildrenView();
            }
        }

        @Override
        public void setParent(MutableTreeNode newParent) {
            super.setParent(newParent);
            if (this.getParent() == null) {
                this.inner.dispose();
            }
        }

        private void removeLoadingNode() {
            this.children().asIterator().forEachRemaining(c -> {
                if (c instanceof LoadingNode) {
                    ((LoadingNode) c).removeFromParent();
                }
            });
        }

        private void addLoadMoreNode() {
            if (this.inner.hasMoreChildren()) {
                this.add(new LoadMoreNode());
            }
        }

        private void removeLoadMoreNode() {
            this.children().asIterator().forEachRemaining(c -> {
                if (c instanceof LoadMoreNode) {
                    ((LoadMoreNode) c).removeFromParent();
                }
            });
        }
    }

    public static class NodeRenderer extends com.intellij.ide.util.treeView.NodeRenderer {

        @Override
        public void customizeCellRenderer(@Nonnull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            if (value instanceof TreeNode) {
                TreeUtils.renderMyTreeNode(tree, (TreeNode<?>) value, selected, this);
            } else {
                super.customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus);
            }
        }
    }

    public static class LoadMoreNode extends DefaultMutableTreeNode {
        public static final String LABEL = "load more...";

        public LoadMoreNode() {
            super(LABEL);
        }

        public void load() {
            Optional.ofNullable(this.getParent()).map(p -> (TreeNode<?>) p).map(p -> p.inner).ifPresent(Node::loadMoreChildren);
        }
    }
}
