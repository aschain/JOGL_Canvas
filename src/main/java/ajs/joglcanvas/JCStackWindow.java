package ajs.joglcanvas;

import java.awt.Button;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import javax.swing.JFrame;
import ij.ImagePlus;
import ij.gui.ScrollbarWithLabel;
import ij.gui.StackWindow;

public class JCStackWindow extends StackWindow {

	private final JOGLImageCanvas jic;
	private Button menuButton=null;
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public JCStackWindow(ImagePlus imp, JOGLImageCanvas jic) {
		super(imp,jic);
		this.jic=jic;
		if(!jic.isMirror) {
			Container c=this;
			if(c instanceof JFrame)c=((JFrame)c).getContentPane();
	        Component[] wincs=c.getComponents();
	        for(int i=0;i<wincs.length;i++) {
	        	if(wincs[i] instanceof JOGLImageCanvas) {c.remove(i); c.add(jic.icc, i);}
	        }
			repaint();
			if(JCP.mouseWheelFix)jic.addMouseWheelListener(this);
		}
		setTitle(imp.getTitle());
		addAdjustmentListening();
	}
	
	@Override
	public void setTitle(String title) {
		if(jic!=null && !jic.isMirror)super.setTitle(title+" (JOGL Canvas)");
		else super.setTitle(title);
	}
	
	@Override
	public boolean close() {
		if(imp.getWindow()==this) ((JOGLImageCanvas) imp.getCanvas()).dispose();
		return super.close();
	}

	@Override
	public void drawInfo(java.awt.Graphics g) {
		super.drawInfo(g);
		if(jic.isMirror && !jic.go3d) {
			drawInfoOnMirror();
		}
	}

	public void drawInfoOnMirror(){
		java.awt.Graphics jg = jic.mirror.getGraphics();
		jg.setColor(java.awt.Color.WHITE);
		jg.fillRect(0, 0, jic.mirror.getWidth(), jic.mirror.getHeight());
		super.drawInfo(jg);
	}
	
	private void addAdjustmentListening() {
		if(imp==null || imp.getWindow()==null) {JOGLImageCanvas.log("JOGLCanvas was created but no ImagePlus Window was found"); return;}
		if(imp.getStackSize()==1)return; //Don't need if no stack
		Component[] comps=getComponents();
		for(int i=0;i<comps.length;i++) {
			if(comps[i] instanceof ScrollbarWithLabel) {
				ScrollbarWithLabel scr=(ScrollbarWithLabel)comps[i];
				scr.addAdjustmentListener(new AdjustmentListener() {
					@Override
					public void adjustmentValueChanged(AdjustmentEvent e) {
						boolean isadj=e.getValueIsAdjusting();
						if(JCP.debug && isadj!=jic.scbrAdjusting.get())JOGLImageCanvas.log("scbradj: "+isadj);
						jic.scbrAdjusting.set(isadj);
					}
				});
				if(JCP.debug)JOGLImageCanvas.log("Added adj list: "+scr);
			}
		}
	}
	
	/**
	 * Adds an update button the the window.
	 * The 3D image takes some time to load into memory,
	 * So it is not updated on every draw.  In case the user
	 * changes the image (cut, paste, draw, fill, process),
	 * The user can then press the update button (or type
	 * u) to update the 3d image.
	 * @param show
	 */
	public void showMenuButton(boolean show) {
		boolean nowin=(imp==null || imp.getWindow()==null || !(imp.getWindow() instanceof StackWindow));
		if(menuButton!=null && (!show || nowin)) {
			Container parent=menuButton.getParent();
			if(parent!=null) {parent.remove(menuButton);}
			menuButton=null;
		}
		if(nowin)return;
		StackWindow stwin=(StackWindow) imp.getWindow();
		if(show && menuButton!=null) {
			if(menuButton.getParent()!=null && menuButton.getParent().getParent()==stwin && menuButton.isEnabled())return;
		}
		ScrollbarWithLabel scr=null;
		Component[] comps=stwin.getComponents();
		for(int i=0;i<comps.length;i++) {
			if(comps[i] instanceof ij.gui.ScrollbarWithLabel) {
				scr=(ScrollbarWithLabel)comps[i];
			}
		}
		if(scr!=null) {
			//Remove any orphaned updateButtons, like with a crashed JOGLImageCanvas
			comps=scr.getComponents();
			for(int i=0;i<comps.length;i++) {
				if(comps[i] instanceof Button) {
					String label=((Button)comps[i]).getLabel();
					if(label.equals("GL")) {
						scr.remove(comps[i]);
					}
				}
			}
			
			if(show) {
				menuButton= new Button("GL");
				menuButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						if (jic.dcpopup!=null) {
							menuButton.add(jic.dcpopup);
							jic.dcpopup.show(menuButton,10,10);
						}
					}
				});
				menuButton.setFocusable(false);
				scr.add(menuButton,java.awt.BorderLayout.EAST);
			}
			stwin.pack();
		}
	}
		
}
