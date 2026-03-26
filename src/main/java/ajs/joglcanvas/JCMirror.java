package ajs.joglcanvas;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Color;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import ij.IJ;
import ij.gui.ImageWindow;
import ij.ImagePlus;

public class JCMirror extends Frame{
    JOGLImageCanvas jic;
    ImagePlus imp;
    AtomicBoolean mirrorPainting=new AtomicBoolean(false);
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public JCMirror(ImagePlus imp, JOGLImageCanvas jic) {
        super("JOGL-DC3D Mirror of "+imp.getTitle());
        this.jic=jic;
        this.imp=imp;
		setLayout(new JCLayout());
		setBackground(Color.WHITE);
		add(jic.icc);
		WindowAdapter wl=new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
                if(JCP.debug) JOGLImageCanvas.log("revertting");
                jic.revert(); 
            }
		};
		addWindowListener(wl);
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				jic.repaintLater();
			}
		});
    }

    class JCLayout implements LayoutManager{
		private static final int TEXT_GAP=11;

		@Override
		public void addLayoutComponent(String name, Component comp) {
		}
		@Override
		public void removeLayoutComponent(Component comp) {
		}
		@Override
		public Dimension preferredLayoutSize(Container parent) {
			Dimension dim=new Dimension(0,0);
			Insets ins=parent.getInsets();
			Dimension cdim=parent.getComponent(0).getSize();
			dim.width+=ins.left+ins.right+cdim.width+ImageWindow.HGAP*2;
			dim.height+=ins.top+ins.bottom+cdim.height+ImageWindow.VGAP*2+TEXT_GAP;
			return dim;
		}
		@Override
		public Dimension minimumLayoutSize(Container parent) {
			return preferredLayoutSize(parent);
		}
		@Override
		public void layoutContainer(Container parent) {
			parent.getComponent(0).setLocation(parent.getInsets().left, parent.getInsets().top);
			Dimension dim=parent.getSize();
			Insets ins=parent.getInsets();
			parent.getComponent(0).setSize(dim.width-ins.left-ins.right-ImageWindow.HGAP*2, dim.height-ins.top-ins.bottom-ImageWindow.VGAP*2-TEXT_GAP);
			parent.getComponent(0).setLocation(parent.getInsets().left+ImageWindow.HGAP, parent.getInsets().top+ImageWindow.VGAP+TEXT_GAP);
			if(JCP.debug)IJ.log("JCLayout move "+parent.getComponent(0));
		}
		
	}

    public void paint(Graphics g) {
    	super.paint(g);
        if(!jic.go3d) {
            g.setColor(Color.WHITE);
            g.fillRect(0,0, getWidth(), getHeight());
            imp.getWindow().drawInfo(g);
        }
    }

    public void draw3DMirrorInfo(){
		if(!isVisible() || jic.glw.isFullscreen() || mirrorPainting.get())return;
		Graphics g=getGraphics();
		if(g==null)return;
		mirrorPainting.set(true);
		java.awt.EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				((java.awt.Graphics2D)g).setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
				g.setColor(Color.WHITE);
				g.fillRect(0,0, getWidth(), getHeight());
				String line="RotX:"+Math.round(jic.dx)+"° RotY:"+Math.round(jic.dy)+"° RotZ:"+Math.round(jic.dz)+"°";
				if(jic.supermag!=0f || jic.getMagnification()!=1.0)line+=" Mag:"+String.format("%.2f", (float)jic.getMagnification()+jic.supermag)+"x";
				if(jic.tx!=0f || jic.ty!=0f || jic.tz!=0f)line+=" Tx:"+String.format("%.1f", jic.tx)+" Ty:"+String.format("%.1f", jic.ty)+" Tz:"+String.format("%.1f", jic.tz);
				g.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12));
				int x=getInsets().left+ImageWindow.HGAP+5, y=getInsets().top+11;
				g.setColor(new Color(0,0,255,255));
				g.drawString(line, x, y);
				mirrorPainting.set(false);
			}
		});
	}
	
	public void setMirrorSizeForCanvas(int width, int height) {
		if(getExtendedState()!=Frame.MAXIMIZED_BOTH) {
			if(jic.icc.getWidth()==width && jic.icc.getHeight()==height)return;
			java.awt.Insets ins=getInsets();
            setSize(width+ins.left+ins.right+ImageWindow.HGAP*2,height+ins.top+ins.bottom+ImageWindow.VGAP*2+11);
		}
	}
}
