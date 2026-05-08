package ajs.joglcanvas;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.Panel;
import ajs.joglcanvas.JOGLImageCanvas.CutPlanesCube;

@SuppressWarnings("serial")
public class JCCutPlanes extends JCAdjuster {
	
	int[] c=new int[6];
	public boolean constrainToZoom=false;
	private final static char[] cps=new char[] {'X','Y','Z','W','H','D'};
	private NumberScrollPanel[] nsps=new NumberScrollPanel[cps.length];
	final int[] whd=new int[] {imp.getWidth(),imp.getHeight(),imp.getNSlices()};

	public JCCutPlanes(JOGLImageCanvas jic) {
		super("Cut Planes", jic);
		CutPlanesCube cpfc=jic.getCutPlanesCube();
		this.c= new int[] {cpfc.x()+1,cpfc.y()+1,cpfc.z()+1,cpfc.w(),cpfc.h(),cpfc.d()};
		setLayout(new GridBagLayout());
		GridBagConstraints c= new GridBagConstraints();
		c.gridx=0;
		c.gridy=0;
		c.fill=GridBagConstraints.HORIZONTAL;
		c.anchor=GridBagConstraints.CENTER;
		c.insets=new Insets(5,5,5,5);
		add(new Label("Cut Planes"),c);
		for(int i=0;i<cps.length;i++) {
			c.gridy++;
			nsps[i]=new NumberScrollPanel(this.c[i],1,whd[(i>2)?(i-3):i]+1,cps[i],0);
			add(nsps[i], c);
			nsps[i].addAdjustmentListener(this);
			nsps[i].setFocusable(false);
		}
		c.gridy++;
		Panel p=new Panel();
		Button b=new Button("Reset");
		b.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				reset(false);
			}
		});
		p.add(b,java.awt.BorderLayout.WEST);
		Checkbox cb=new Checkbox("Constrain to Zoom");
		cb.setState(constrainToZoom);
		cb.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				constrainToZoom=e.getStateChange()==ItemEvent.SELECTED;
				if(constrainToZoom)constrainToZoom(true);
				else reset(true);
			}
		});
		p.add(cb,java.awt.BorderLayout.CENTER);
		cb=new Checkbox("Apply to Rois");
		cb.setState(cpfc.applyToRoi);
		cb.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				jic.setCutPlanesApplyToRoi(e.getStateChange()==ItemEvent.SELECTED);
			}
		});
		p.add(cb,java.awt.BorderLayout.EAST);
		add(p,c);
		pack();
		setToDefaultLocation();
		setVisible(true);
	}

	public void constrainToZoom(boolean withRepaint) {
		Rectangle r=jic.getSrcRect();
		for(int i=0;i<3;i++) {
			float low=(i==0?r.x:r.y)+1;
			float high=(i==0?r.x+r.width:r.y+r.height);
			if(i==2 || i==5) continue;
			nsps[i].setFloatValue(low);
			nsps[i+3].setFloatValue(high);
		}
		update(withRepaint);
	}

	private void reset(boolean ignoreZ) {
		for(int i=0;i<3;i++) {
			if(ignoreZ && i==2)continue;
			nsps[i].setFloatValue(1);
			nsps[i+3].setFloatValue(whd[i]);
		}
		update();
	}

	private void update() {
		update(true);
	}
	
	private void update(boolean withRepaint) {
		int i=0;
		c[i]=(int)nsps[i++].getFloatValue()-1;
		c[i]=(int)nsps[i++].getFloatValue()-1;
		c[i]=(int)nsps[i++].getFloatValue()-1;
		c[i]=(int)nsps[i++].getFloatValue();
		c[i]=(int)nsps[i++].getFloatValue();
		c[i]=(int)nsps[i].getFloatValue();
		jic.updateCutPlanesCube(c, withRepaint);
	}

	@Override
	public void adjustmentValueChanged(AdjustmentEvent e) {
		if(jic.icc==null || !jic.icc.isVisible())dispose();
		Object source=e.getSource();
		if(source instanceof NumberScrollPanel) {
			NumberScrollPanel nsp=(NumberScrollPanel)source;
			switch(nsp.getLabel()) {
			case 'X':
				if(nsp.getFloatValue()>nsps[3].getFloatValue())nsps[3].setFloatValue(nsp.getFloatValue());
				break;
			case 'Y':
				if(nsp.getFloatValue()>nsps[4].getFloatValue())nsps[4].setFloatValue(nsp.getFloatValue());
				break;
			case 'Z':
				if(nsp.getFloatValue()>nsps[5].getFloatValue())nsps[5].setFloatValue(nsp.getFloatValue());
				break;
			case 'W':
				if(nsp.getFloatValue()<nsps[0].getFloatValue())nsps[0].setFloatValue(nsp.getFloatValue());
				break;
			case 'H':
				if(nsp.getFloatValue()<nsps[1].getFloatValue())nsps[1].setFloatValue(nsp.getFloatValue());
				break;
			case 'D':
				if(nsp.getFloatValue()<nsps[2].getFloatValue())nsps[2].setFloatValue(nsp.getFloatValue());
				break;
			}
		}
		update();

	}
}
