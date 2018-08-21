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

import com.yworks.yfiles.geometry.IMutableRectangle;
import com.yworks.yfiles.geometry.InsetsD;
import com.yworks.yfiles.geometry.IOrientedRectangle;
import com.yworks.yfiles.geometry.MutableRectangle;
import com.yworks.yfiles.geometry.OrientedRectangle;
import com.yworks.yfiles.geometry.OrientedRectangleExtensions;
import com.yworks.yfiles.geometry.PointD;
import com.yworks.yfiles.geometry.RectangleExtensions;
import com.yworks.yfiles.geometry.RectD;
import com.yworks.yfiles.geometry.SizeD;
import com.yworks.yfiles.graph.GraphExtensions;
import com.yworks.yfiles.graph.ILabel;
import com.yworks.yfiles.graph.ILookup;
import com.yworks.yfiles.graph.ITagOwner;
import com.yworks.yfiles.graph.labelmodels.FreeLabelModel;
import com.yworks.yfiles.graph.SimpleLabel;
import com.yworks.yfiles.graph.SimpleNode;
import com.yworks.yfiles.graph.styles.ILabelStyle;
import com.yworks.yfiles.graph.styles.ILabelStyleRenderer;
import com.yworks.yfiles.graph.styles.INodeStyle;
import com.yworks.yfiles.graph.styles.UIElementHelpers;
import com.yworks.yfiles.utils.MethodId;
import com.yworks.yfiles.utils.Obfuscation;
import com.yworks.yfiles.view.IBoundsProvider;
import com.yworks.yfiles.view.ICanvasContext;
import com.yworks.yfiles.view.IDisposeVisualCallback;
import com.yworks.yfiles.view.input.IHitTestable;
import com.yworks.yfiles.view.input.IInputModeContext;
import com.yworks.yfiles.view.input.IMarqueeTestable;
import com.yworks.yfiles.view.input.INodeInsetsProvider;
import com.yworks.yfiles.view.IRenderContext;
import com.yworks.yfiles.view.IVisibilityTestable;
import com.yworks.yfiles.view.IVisual;
import com.yworks.yfiles.view.IVisualCreator;
import com.yworks.yfiles.view.RenderContextExtensions;
import com.yworks.yfiles.view.VisualGroup;
import com.yworks.yfiles.view.VoidVisualCreator;

import java.awt.Graphics2D;

class NodeStyleLabelStyleRenderer implements ILabelStyleRenderer, IBoundsProvider, IVisibilityTestable, IMarqueeTestable, IHitTestable, ILookup, IVisualCreator {
  private final SimpleNode dummyNode;

  private final SimpleLabel dummyLabel;

  private final OrientedRectangle rectangle;

  NodeStyleLabelStyleRenderer() {
    SimpleNode newInstance = new SimpleNode();
    {
      newInstance.setLayout(new MutableRectangle());
    }
    this.dummyNode = newInstance;
    rectangle = new OrientedRectangle();
    FreeLabelModel freeLabelModel = new FreeLabelModel();
    dummyLabel = new SimpleLabel(null, "", freeLabelModel.createDynamic(rectangle));
  }

  private IOrientedRectangle layout;

  //region State and Configure

  /**
   * The style that it currently assigned to this renderer instance.
   */
  private NodeStyleLabelStyleAdapter style;

  /**
   * The label that is currently assigned to this renderer instance.
   */
  private ILabel label;

  /**
   * Gets the currently configured style.
   * @return The Style.
   * @see #setStyle(NodeStyleLabelStyleAdapter)
   */
  protected final NodeStyleLabelStyleAdapter getStyle() {
    return style;
  }

  /**
   * Sets the currently configured style.
   * @param value The Style to set.
   * @see #getStyle()
   */
  protected final void setStyle( NodeStyleLabelStyleAdapter value ) {
    style = value;
  }

  /**
   * Gets the currently configured label.
   * @return The Label.
   * @see #setLabel(ILabel)
   */
  protected final ILabel getLabel() {
    return label;
  }

  /**
   * Sets the currently configured label.
   * @param value The Label to set.
   * @see #getLabel()
   */
  protected final void setLabel( ILabel value ) {
    label = value;
  }

  //endregion

  //region IStyleRenderer

  /**
   * Configures the {@code style} and {@code label} parameters, calls {@link #configure()} and returns {@code this}.
   * @param label The label to retrieve the {@link IVisualCreator} for. The value will be stored in the {@link #getLabel() Label}
   * property.
   * @param style The style to associate with the label. The value will be stored in the {@link #getStyle() Style} property.
   * @return {@code this}
   * @see #createVisual(IRenderContext)
   * @see #updateVisual(IRenderContext, IVisual)
   */
  @Obfuscation(stripAfterObfuscation = false, exclude = true)
  public IVisualCreator getVisualCreator( ILabel label, ILabelStyle style ) {
    NodeStyleLabelStyleAdapter theStyle = (style instanceof NodeStyleLabelStyleAdapter) ? (NodeStyleLabelStyleAdapter)style : null;
    if (theStyle != null) {
      setStyle(theStyle);
      setLabel(label);
      configure();
      return this;
    } else {
      return VoidVisualCreator.INSTANCE;
    }
  }

  /**
   * Configures the {@code style} and {@code label} parameters, calls {@link #configure()} and returns {@code this}.
   * @param label The label to retrieve the bounds provider for. The value will be stored in the {@link #getLabel() Label} property.
   * @param style The style to associate with the label. The value will be stored in the {@link #getStyle() Style} property.
   * @return {@code this}
   * @see #getBounds(ICanvasContext)
   */
  @Obfuscation(stripAfterObfuscation = false, exclude = true)
  public IBoundsProvider getBoundsProvider( ILabel label, ILabelStyle style ) {
    NodeStyleLabelStyleAdapter theStyle = (style instanceof NodeStyleLabelStyleAdapter) ? (NodeStyleLabelStyleAdapter)style : null;
    if (theStyle != null) {
      setStyle(theStyle);
      setLabel(label);
      configure();
      return this;
    } else {
      return IBoundsProvider.EMPTY;
    }
  }

  /**
   * Configures the {@code style} and {@code label} parameters, calls {@link #configure()} and returns {@code this}.
   * @param label The label to query hit test with. The value will be stored in the {@link #getLabel() Label} property.
   * @param style The style to associate with the label. The value will be stored in the {@link #getStyle() Style} property.
   * @return {@code this}
   * @see #isHit(IInputModeContext, PointD)
   */
  @Obfuscation(stripAfterObfuscation = false, exclude = true)
  public IHitTestable getHitTestable( ILabel label, ILabelStyle style ) {
    NodeStyleLabelStyleAdapter theStyle = (style instanceof NodeStyleLabelStyleAdapter) ? (NodeStyleLabelStyleAdapter)style : null;
    if (theStyle != null) {
      setStyle(theStyle);
      setLabel(label);
      configure();
      return this;
    } else {
      return IHitTestable.NEVER;
    }
  }

  /**
   * Configures the {@code style} and {@code label} parameters, calls {@link #configure()} and returns {@code this}.
   * @param label The label to query marquee intersection tests. The value will be stored in the {@link #getLabel() Label} property.
   * @param style The style to associate with the label. The value will be stored in the {@link #getStyle() Style} property.
   * @return {@code this}
   * @see #isInBox(IInputModeContext, RectD)
   */
  @Obfuscation(stripAfterObfuscation = false, exclude = true)
  public IMarqueeTestable getMarqueeTestable( ILabel label, ILabelStyle style ) {
    NodeStyleLabelStyleAdapter theStyle = (style instanceof NodeStyleLabelStyleAdapter) ? (NodeStyleLabelStyleAdapter)style : null;
    if (theStyle != null) {
      setStyle(theStyle);
      setLabel(label);
      configure();
      return this;
    } else {
      return IMarqueeTestable.NEVER;
    }
  }

  /**
   * Configures the {@code style} and {@code label} parameters, does <b>not</b> call {@link #configure()} and returns
   * {@code this}.
   * <p>
   * Unlike most of the other methods this implementation does <b>not</b> call {@link #configure()}. If the subclass
   * implementation depends on this instance to be configured, it needs to call {@code Configure} in
   * {@link #isVisible(ICanvasContext, RectD)}.
   * </p>
   * @param label The label to query visibility tests. The value will be stored in the {@link #getLabel() Label} property.
   * @param style The style to associate with the label. The value will be stored in the {@link #getStyle() Style} property.
   * @return {@code this}
   * @see #isVisible(ICanvasContext, RectD)
   */
  @Obfuscation(stripAfterObfuscation = false, exclude = true)
  public IVisibilityTestable getVisibilityTestable( ILabel label, ILabelStyle style ) {
    NodeStyleLabelStyleAdapter theStyle = (style instanceof NodeStyleLabelStyleAdapter) ? (NodeStyleLabelStyleAdapter)style : null;
    if (theStyle != null) {
      setStyle(theStyle);
      setLabel(label);
      return this;
    } else {
      return IVisibilityTestable.NEVER;
    }
  }

  /**
   * Configures the {@code style} and {@code label} parameters, does <b>not</b> call {@link #configure()} and returns
   * {@code this}.
   * <p>
   * As this method may be called often it will not automatically call {@link #configure()}, instead subclasses should ensure
   * that in the {@link #lookup(Class)} method call they should call {@link #configure()} only if needed, i.e. if they decide
   * to return {@code this} or an instance that depends on a correctly configured {@code this}.
   * </p>
   * @param label The label to query the context for. The value will be stored in the {@link #getLabel() Label} property.
   * @param style The style to associate with the label. The value will be stored in the {@link #getStyle() Style} property.
   * @return {@code this}
   * @see #lookup(Class)
   */
  @Obfuscation(stripAfterObfuscation = false, exclude = true)
  public ILookup getContext( ILabel label, ILabelStyle style ) {
    NodeStyleLabelStyleAdapter theStyle = (style instanceof NodeStyleLabelStyleAdapter) ? (NodeStyleLabelStyleAdapter)style : null;
    if (theStyle != null) {
      setStyle(theStyle);
      setLabel(label);
      return this;
    } else {
      return ILookup.EMPTY;
    }
  }

  //endregion

  //region Configured interface implementations

  /**
   * Implements the {@link ILookup} interface.
   * <p>
   * This method will be used by default if {@link #getContext(ILabel, ILabelStyle)} has been queried for a lookup
   * implementation. Note that it cannot be assumed that {@link #configure()} has already been invoked. However, normally {@link #getLabel() Label}
   * and {@link #getStyle() Style} will be correctly configured if invoked directly after {@code GetContext}. Subclass
   * implementations should make sure to configure this instance before they return {@code this} as a result of a successful
   * lookup. This implementation will check if {@code type.IsInstanceOfType(this)} and will call {@link #configure()} on
   * success and return {@code this}.
   * </p>
   * @param type The type to query for.
   * @return An implementation or {@code null}.
   */
  public <TLookup> TLookup lookup( Class<TLookup> type ) {
    if (type.isInstance(this)) {
      configure();
      return (TLookup)this;
    } else {
      return null;
    }
  }

  //endregion

  //region Configured interfaces: ILabelStyleRenderer specific

  @Obfuscation(stripAfterObfuscation = false, exclude = true)
  public boolean isHit( IInputModeContext context, PointD location ) {
    return OrientedRectangleExtensions.contains(layout, location, context.getHitTestRadius());
  }

  @Obfuscation(stripAfterObfuscation = false, exclude = true)
  public boolean isInBox( IInputModeContext context, RectD rectangle ) {
    return rectangle.intersects(layout, context.getHitTestRadius());
  }

  @Obfuscation(stripAfterObfuscation = false, exclude = true)
  public RectD getBounds( ICanvasContext context ) {
    return OrientedRectangleExtensions.getBounds(layout);
  }

  /**
   * Uses the {@link ILabel#getLayout() layout} to determine whether the clip intersects.
   */
  @Obfuscation(stripAfterObfuscation = false, exclude = true)
  public boolean isVisible( ICanvasContext context, RectD rectangle ) {
    return rectangle.intersects(GraphExtensions.getLayout(label), 2);
  }

  //endregion

  //region ILabelStyleRenderer

  public final SizeD getPreferredSize( ILabel label, ILabelStyle style ) {
    setLabel(label);
    setStyle((NodeStyleLabelStyleAdapter)style);
    configure();
    return getPreferredSize();
  }

  //endregion

  protected SizeD getPreferredSize() {
    SizeD size = style.getLabelStyle().getRenderer().getPreferredSize(label, style.getLabelStyle());
    ILookup lookup = style.getNodeStyle().getRenderer().getContext(dummyNode, style.getNodeStyle());
    INodeInsetsProvider provider = lookup.lookup(INodeInsetsProvider.class);
    if (provider != null) {
      InsetsD insets = provider.getInsets(dummyNode);
      insets = insets.createUnion(getLabelStyleInsets());
      return new SizeD(size.width + insets.getHorizontalInsets(), size.height + insets.getVerticalInsets());
    } else {
      InsetsD insets = getLabelStyleInsets();
      return new SizeD(size.width + insets.getHorizontalInsets(), size.height + insets.getVerticalInsets());
    }
  }

  protected void configure() {
    layout = GraphExtensions.getLayout(label);
    dummyNode.setStyle(style.getNodeStyle());
    RectangleExtensions.reshape(((IMutableRectangle)dummyNode.getLayout()), 0, 0, layout.getWidth(), layout.getHeight());

    configureDummyLabel(dummyLabel);
    updateDummyLabelLayout();
  }

  private void configureDummyLabel( SimpleLabel dummyLabel ) {
    dummyLabel.setStyle(style.getLabelStyle());

    ITagOwner tagOwner = label.lookup(ITagOwner.class);
    if (tagOwner != null) {
      dummyLabel.setTag(tagOwner.getTag());
    } else {
      dummyLabel.setTag(null);
    }
    dummyLabel.setText(label.getText());
  }

  private void updateDummyLabelLayout() {
    ILookup lookup = style.getNodeStyle().getRenderer().getContext(dummyNode, style.getNodeStyle());
    INodeInsetsProvider provider = lookup.lookup(INodeInsetsProvider.class);
    InsetsD innerInsets = getLabelStyleInsets();
    if (provider != null) {
      InsetsD insets = provider.getInsets(dummyNode);
      insets = insets.createUnion(innerInsets);
      rectangle.setWidth(layout.getWidth() - insets.getHorizontalInsets());
      rectangle.setHeight(layout.getHeight() - insets.getVerticalInsets());
      rectangle.setAnchorX(insets.left);
      rectangle.setAnchorY(layout.getHeight() - insets.bottom);
    } else {
      rectangle.setWidth(layout.getWidth() - innerInsets.getHorizontalInsets());
      rectangle.setHeight(layout.getHeight() - innerInsets.getVerticalInsets());
      rectangle.setAnchorX(innerInsets.left);
      rectangle.setAnchorY(layout.getHeight() - innerInsets.bottom);
    }
  }

  protected InsetsD getLabelStyleInsets() {
    return style.getLabelStyleInsets();
  }

  public IVisual createVisual( IRenderContext context ) {
    double h = layout.getHeight();
    double w = layout.getWidth();
    if (w < 0 || h < 0) {
      return null;
    }
    INodeStyle nodeStyle = style.getNodeStyle();
    ILabelStyle labelStyle = style.getLabelStyle();

    CachingVisualGroup container = new CachingVisualGroup();
    SimpleNode newInstance = new SimpleNode();
    {
      newInstance.setLayout(new MutableRectangle());
      newInstance.setStyle(style.getNodeStyle());
    }
    SimpleNode dummyNode = newInstance;
    RectangleExtensions.reshape(((IMutableRectangle)dummyNode.getLayout()), 0, 0, layout.getWidth(), layout.getHeight());
    IVisualCreator creator1 = nodeStyle.getRenderer().getVisualCreator(dummyNode, nodeStyle);
    IVisual background = creator1.createVisual(context);
    if (background != null) {
      container.add(background);
    } else {
      container.add(VoidVisual.Instance);
    }
    OrientedRectangle rect = new OrientedRectangle();
    rect.setAnchorX(rectangle.getAnchorX());
    rect.setAnchorY(rectangle.getAnchorY());
    rect.setWidth(rectangle.getWidth());
    rect.setHeight(rectangle.getHeight());
    SimpleLabel dummyLabel = new SimpleLabel(null, "", ((FreeLabelModel)this.dummyLabel.getLayoutParameter().getModel()).createDynamic(rect));
    configureDummyLabel(dummyLabel);
    IVisualCreator creator2 = labelStyle.getRenderer().getVisualCreator(dummyLabel, labelStyle);
    IVisual foreground = creator2.createVisual(context);
    if (foreground != null) {
      container.add(foreground);
    } else {
      container.add(VoidVisual.Instance);
    }

    container.setTransform(UIElementHelpers.createTransformForLayout(layout, isAutoFlip()));
    container.setRenderDataCache(new DummyElements(dummyNode, dummyLabel, rect, nodeStyle, labelStyle));
    RenderContextExtensions.registerForChildrenIfNecessary(context, container, NodeStyleLabelStyleRenderer.createIDisposeVisualCallback(container, disposeChildren_MethodId));
    return container;
  }

  static IDisposeVisualCallback createIDisposeVisualCallback( final VisualGroup thisInstance, final MethodId methodId ) {
    return new IDisposeVisualCallback(){
      public IVisual dispose( IRenderContext context, IVisual removedVisual, boolean dispose ) {
        if (methodId == NodeStyleLabelStyleRenderer.disposeChildren_MethodId) {
          return thisInstance.disposeChildren(context, removedVisual, dispose);
        }
        throw new IllegalArgumentException();
      }
    };
  }

  static final MethodId disposeChildren_MethodId = MethodId.create("com.yworks.yfiles.view.VisualGroup#DisposeChildren(com.yworks.yfiles.view.IRenderContext,com.yworks.yfiles.view.IVisual,boolean)");

  private static final class VoidVisual implements IVisual {
    public static VoidVisual Instance = new VoidVisual();

    private VoidVisual() {
    }

    //region Add new code here
    public void paint(IRenderContext context, Graphics2D g) {}
    //endregion END: new code
  }

  private static class DummyElements {
    public SimpleNode dummyNode;

    public SimpleLabel dummyLabel;

    public OrientedRectangle rect;

    public INodeStyle nodeStyle;

    public ILabelStyle labelStyle;

    //region Add new code here

    public DummyElements(SimpleNode dummyNode, SimpleLabel dummyLabel, OrientedRectangle rect, INodeStyle nodeStyle, ILabelStyle labelStyle) {
      this.dummyNode = dummyNode;
      this.dummyLabel = dummyLabel;
      this.rect = rect;
      this.nodeStyle = nodeStyle;
      this.labelStyle = labelStyle;
    }

    //endregion END: new code
  }

  public IVisual updateVisual( IRenderContext context, IVisual oldVisual ) {
    double h = layout.getHeight();
    double w = layout.getWidth();
    if (w < 0 || h < 0) {
      if (oldVisual != null) {
        RenderContextExtensions.childVisualRemoved(context, oldVisual);
      }
      return null;
    }

    CachingVisualGroup container = (oldVisual instanceof CachingVisualGroup) ? (CachingVisualGroup)oldVisual : null;
    DummyElements dummyElements;
    if (container != null && container.getChildren().size() == 2 && (dummyElements = container.getRenderDataCache()) != null) {
      SimpleNode dummyNode = dummyElements.dummyNode;
      dummyNode.setStyle(getStyle().getNodeStyle());
      RectangleExtensions.reshape(((IMutableRectangle)dummyNode.getLayout()), 0, 0, w, h);
      INodeStyle nodeStyle = style.getNodeStyle();
      ILabelStyle labelStyle = style.getLabelStyle();

      IVisualCreator creator1 = nodeStyle.getRenderer().getVisualCreator(dummyNode, nodeStyle);
      IVisual background = (IVisual)container.getChildren().get(0);
      if (background instanceof VoidVisual || nodeStyle != dummyElements.nodeStyle) {
        dummyElements.nodeStyle = nodeStyle;
        background = creator1.createVisual(context);
      } else {
        background = creator1.updateVisual(context, background);
      }
      if (background == null) {
        background = VoidVisual.Instance;
      }
      if (background != container.getChildren().get(0)) {
        IVisual visual = container.getChildren().get(0);
        container.getChildren().set(0, background);
        RenderContextExtensions.childVisualRemoved(context, visual);
      }

      SimpleLabel dummyLabel = dummyElements.dummyLabel;
      OrientedRectangle rect = dummyElements.rect;
      rect.setAnchorX(rectangle.getAnchorX());
      rect.setAnchorY(rectangle.getAnchorY());
      rect.setWidth(rectangle.getWidth());
      rect.setHeight(rectangle.getHeight());
      configureDummyLabel(dummyLabel);
      IVisualCreator creator2 = labelStyle.getRenderer().getVisualCreator(dummyLabel, labelStyle);
      IVisual foreground = (IVisual)container.getChildren().get(1);
      if (background instanceof VoidVisual || labelStyle != dummyElements.labelStyle) {
        dummyElements.labelStyle = labelStyle;
        foreground = creator2.createVisual(context);
      } else {
        foreground = creator2.updateVisual(context, foreground);
      }
      if (foreground == null) {
        foreground = VoidVisual.Instance;
      }
      if (foreground != container.getChildren().get(1)) {
        IVisual visual = container.getChildren().get(1);
        container.getChildren().set(1, foreground);
        RenderContextExtensions.childVisualRemoved(context, visual);
      }
      container.setTransform(UIElementHelpers.createTransformForLayout(layout, isAutoFlip()));
      RenderContextExtensions.registerForChildrenIfNecessary(context, container, NodeStyleLabelStyleRenderer.createIDisposeVisualCallback(container, disposeChildren_MethodId));
      return container;
    }
    RenderContextExtensions.childVisualRemoved(context, oldVisual);
    return createVisual(context);
  }

  /**
   * Delegates to {@link NodeStyleLabelStyleAdapter#isAutoFlippingEnabled() AutoFlippingEnabled}.
   */
  protected boolean isAutoFlip() {
    return style.isAutoFlippingEnabled();
  }

  //region Add new code here

  private static final class CachingVisualGroup extends VisualGroup {

    private DummyElements renderDataCache;

    public DummyElements getRenderDataCache() {
      return renderDataCache;
    }

    public void setRenderDataCache(
        DummyElements renderDataCache) {
      this.renderDataCache = renderDataCache;
    }
  }

  //endregion END: new code
}
