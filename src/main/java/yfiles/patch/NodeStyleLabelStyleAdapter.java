/*
 This demo file is part of yFiles for Java 3.1.

 Copyright (c) 2000-2017 by yWorks GmbH, Vor dem Kreuzberg 28,
 72070 Tuebingen, Germany. All rights reserved.

 yFiles demo files exhibit yFiles for Java functionalities. Any redistribution
 of demo files in source code or binary form, with or without
 modification, is not permitted.

 Owners of a valid software license for a yFiles for Java version that this
 demo is shipped with are allowed to use the demo source code as basis
 for their own yFiles for Java powered applications. Use of such programs is
 governed by the rights and conditions as set out in the yFiles for Java
 license agreement.

 THIS SOFTWARE IS PROVIDED ''AS IS'' AND ANY EXPRESS OR IMPLIED
 WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN
 NO EVENT SHALL yWorks BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */
package yfiles.patch;

import com.yworks.yfiles.geometry.InsetsD;
import com.yworks.yfiles.graph.styles.*;
import com.yworks.yfiles.graphml.DefaultValue;
import com.yworks.yfiles.utils.Obfuscation;

/**
 * A label style decorator that uses a node style instance to render the background and a label style instance to render
 * the foreground of a label.
 */
@Obfuscation(stripAfterObfuscation = false, exclude = true, applyToMembers = false)
public class NodeStyleLabelStyleAdapter implements ILabelStyle, Cloneable {
  private final NodeStyleLabelStyleRenderer renderer;

  private INodeStyle nodeStyle;

  private ILabelStyle labelStyle;

  private boolean autoFlip = true;

  /**
   * Initializes a new instance of the {@link NodeStyleLabelStyleAdapter} class.
   * <p>
   * This constructor uses a {@link ShapeNodeStyle} and a {@link DefaultLabelStyle} for the {@link #getNodeStyle() NodeStyle}
   * and {@link #getLabelStyle() LabelStyle} properties.
   * </p>
   */
  public NodeStyleLabelStyleAdapter() {
    this(new ShapeNodeStyle(), new DefaultLabelStyle());
  }

  /**
   * Creates a label style that uses the provided node style to render the background and the label style to render the
   * foreground of this style.
   * <p>
   * Note that the styles will be stored by reference, thus modifying the style will directly affect the rendering of this
   * instance.
   * </p>
   * @param nodeStyle The style to use for rendering the background of the label.
   * @param labelStyle The style to use for rendering the foreground of the label.
   */
  public NodeStyleLabelStyleAdapter( INodeStyle nodeStyle, ILabelStyle labelStyle ) {
    if (labelStyle == null) {
      throw new NullPointerException("labelStyle");
    }
    if (nodeStyle == null) {
      throw new NullPointerException("nodeStyle");
    }
    this.nodeStyle = nodeStyle;
    this.labelStyle = labelStyle;
    this.renderer = new NodeStyleLabelStyleRenderer();
  }

  public final ILabelStyleRenderer getRenderer() {
    return renderer;
  }

  /**
   * Gets the {@link INodeStyle} that is used for rendering the background of the label.
   * @return The NodeStyle.
   * @see #setNodeStyle(INodeStyle)
   */
  @Obfuscation(exclude = true, stripAfterObfuscation = false)
  public final INodeStyle getNodeStyle() {
    return nodeStyle;
  }

  /**
   * Sets the {@link INodeStyle} that is used for rendering the background of the label.
   * @param value The NodeStyle to set.
   * @see #getNodeStyle()
   */
  @Obfuscation(exclude = true, stripAfterObfuscation = false)
  public final void setNodeStyle( INodeStyle value ) {
    if (value == null) {
      throw new NullPointerException();
    }
    nodeStyle = value;
  }

  /**
   * Gets the {@link ILabelStyle} that is used for rendering the foreground of the label.
   * @return The LabelStyle.
   * @see #setLabelStyle(ILabelStyle)
   */
  @Obfuscation(exclude = true, stripAfterObfuscation = false)
  public final ILabelStyle getLabelStyle() {
    return labelStyle;
  }

  /**
   * Sets the {@link ILabelStyle} that is used for rendering the foreground of the label.
   * @param value The LabelStyle to set.
   * @see #getLabelStyle()
   */
  @Obfuscation(exclude = true, stripAfterObfuscation = false)
  public final void setLabelStyle( ILabelStyle value ) {
    if (value == null) {
      throw new NullPointerException();
    }
    labelStyle = value;
  }

  /**
   * Gets a value indicating whether the label should be flipped 180 degrees automatically, if it would be oriented
   * downwards, otherwise.
   * @return {@code true} if the label should be flipped automatically otherwise, {@code false}. The default is {@code true}.
   * @see #setAutoFlippingEnabled(boolean)
   */
  @DefaultValue(booleanValue = true, valueType = DefaultValue.ValueType.BOOLEAN_TYPE)
  @Obfuscation(exclude = true, stripAfterObfuscation = false)
  public final boolean isAutoFlippingEnabled() {
    return autoFlip;
  }

  /**
   * Sets a value indicating whether the label should be flipped 180 degrees automatically, if it would be oriented
   * downwards, otherwise.
   * @param value {@code true} if the label should be flipped automatically otherwise, {@code false}. The default is {@code true}.
   * @see #isAutoFlippingEnabled()
   */
  @DefaultValue(booleanValue = true, valueType = DefaultValue.ValueType.BOOLEAN_TYPE)
  @Obfuscation(exclude = true, stripAfterObfuscation = false)
  public final void setAutoFlippingEnabled( boolean value ) {
    autoFlip = value;
  }

  private InsetsD labelStyleInsets = new InsetsD();

  /**
   * Gets the insets to apply for the {@link #getLabelStyle() LabelStyle} as margins.
   * @return The label style insets. The default is (0,0,0,0).
   * @see #setLabelStyleInsets(InsetsD)
   */
  @DefaultValue(stringValue = "0", classValue = InsetsD.class)
  @Obfuscation(exclude = true, stripAfterObfuscation = false)
  public final InsetsD getLabelStyleInsets() {
    return this.labelStyleInsets;
  }

  /**
   * Sets the insets to apply for the {@link #getLabelStyle() LabelStyle} as margins.
   * @param value The label style insets. The default is (0,0,0,0).
   * @see #getLabelStyleInsets()
   */
  @DefaultValue(stringValue = "0", classValue = InsetsD.class)
  @Obfuscation(exclude = true, stripAfterObfuscation = false)
  public final void setLabelStyleInsets( InsetsD value ) {
    this.labelStyleInsets = value;
  }

  public NodeStyleLabelStyleAdapter clone() {
    try {
      return (NodeStyleLabelStyleAdapter)super.clone();
    }catch (CloneNotSupportedException exception) {
      throw new RuntimeException("Class doesn't implement java.lang.Cloneable");
    }
  }

  //region Add new code here
  //endregion END: new code
}
