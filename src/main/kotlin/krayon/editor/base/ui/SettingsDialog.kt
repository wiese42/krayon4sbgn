/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.ui

import krayon.editor.base.command.SetLookAndFeel
import mdlaf.resources.MaterialBorders
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import javax.swing.*
import javax.swing.border.Border
import javax.swing.tree.*

@Suppress("LeakingThis")
open class SettingsDialog(frame:JFrame?) : JDialog(frame, true) {

    protected val root = DefaultMutableTreeNode()
    private val settingsPanel = JPanel()
    private val tree:JTree

    class SettingsContext(val dialog:SettingsDialog)

    init {
        contentPane = JPanel(BorderLayout()).apply {
            border = emptyBorder(5)
        }
        initializeTree()
        tree = JTree(root)
        tree.isRootVisible = false
        tree.isEditable = false
        tree.cellRenderer = object:DefaultTreeCellRenderer() {
            val initialBackground = background
            override fun getTreeCellRendererComponent(tree: JTree, value: Any, isSelected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean): Component {
                val displayName = ((value as DefaultMutableTreeNode).userObject as? SettingItem)?.name ?: ""
                val comp = super.getTreeCellRendererComponent(tree, displayName, isSelected, expanded, leaf, row, hasFocus) as DefaultTreeCellRenderer
                comp.background = if (isSelected) comp.backgroundSelectionColor else initialBackground
                comp.leafIcon = null
                return comp
            }
        }

        tree.border = createBorder(10)

        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        tree.addTreeSelectionListener {
            val item = (tree.selectionPath.lastPathComponent as DefaultMutableTreeNode).userObject as SettingItem
            settingsPanel.components.forEach(settingsPanel::remove)
            settingsPanel.add(item.getSettingsComponent(SettingsContext(this)))
            settingsPanel.revalidate()
            settingsPanel.repaint()
        }
        val treeContainer = JScrollPane(tree).apply {
            preferredSize = Dimension(200,600)
        }

        contentPane.add(treeContainer, BorderLayout.WEST)
        settingsPanel.preferredSize = Dimension(600,600)

        settingsPanel.border = createBorder(10)

        contentPane.add(settingsPanel, BorderLayout.CENTER)

        val buttonPanel = JPanel()
        buttonPanel.border = emptyBorder(10)

        buttonPanel.add(JButton(UiFactory.createAction {
            commitAllChanges(root)
            isVisible = false
        }.apply { name = "Ok" }))

        buttonPanel.add(JButton(UiFactory.createAction {
            resetAllChanges(root)
            isVisible = false
        }.apply { name = "Cancel" }))

        buttonPanel.add(JButton(UiFactory.createAction {
            commitAllChanges(root)
        }.apply { name = "Apply" }))

        contentPane.add(buttonPanel, BorderLayout.SOUTH)

        if(tree.selectionModel.isSelectionEmpty && root.childCount > 0) {
            tree.selectionModel.selectionPath = TreePath(root.children().toList().first())
        }

        pack()

    }

    private fun createBorder(size:Int): Border {
        return if(SetLookAndFeel.activeLaF == SetLookAndFeel.LnF.MATERIAL) MaterialBorders.LIGHT_SHADOW_BORDER else emptyBorder(size)
    }

    private fun commitAllChanges(root:TreeNode) {
        for (child in root.children()) {
            val node = (child as DefaultMutableTreeNode)
            (node.userObject as? SettingAgent)?.commitSettings(SettingsContext(this))
            commitAllChanges(node)
        }
    }

    private fun resetAllChanges(root:TreeNode) {
        for (child in root.children()) {
            val node = (child as DefaultMutableTreeNode)
            (node.userObject as? SettingAgent)?.resetSettings(SettingsContext(this))
            resetAllChanges(node)
        }
    }

    protected open fun initializeTree() {
    }

}

abstract class SettingItem(val name:String) {
    abstract fun getSettingsComponent(context:SettingsDialog.SettingsContext):JComponent
}

abstract class SettingAgent(name:String) : SettingItem(name) {
    abstract fun commitSettings(context:SettingsDialog.SettingsContext)
    abstract fun resetSettings(context:SettingsDialog.SettingsContext)
}

//class SettingCategory(name:String) : SettingItem(name) {
//    override fun getSettingsComponent(context:SettingsDialog.SettingsContext): JComponent {
//        return JPanel(BorderLayout()).apply {
//            add(JLabel("Category ${this@SettingCategory.name}"),BorderLayout.NORTH)
//        }
//    }
//}
