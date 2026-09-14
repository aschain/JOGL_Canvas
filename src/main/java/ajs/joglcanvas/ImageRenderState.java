package ajs.joglcanvas;

import com.jogamp.math.Quaternion;
import ij.measure.Calibration;
import ij.process.LUT;
import java.awt.Rectangle;


/**
 * Mutable state that describes how an image is rendered.
 *
 * <p>This is deliberately independent of AWT, and OpenGL objects so
 * rendering state can be moved out of {@link JOGLImageCanvas} incrementally.
 * It currently owns the rotation and gamma correction state; projection,
 * stereo, and volume state can follow without changing its boundary.</p>
 */
final class ImageRenderState {

    private final Quaternion rotation = new Quaternion().setIdentity();
    private float[] gamma;
    private float[] translation = new float[] {0f, 0f, 0f};
    private float supermag = 0f;
    private float[] zNearFar = new float[]{JCP.zNear, JCP.zFar};
    public final PreviousState prev;
    private Rectangle srcRect;
    private double mag;
    private MinMax[] minmaxs;
    private Calibration cal;
    private int c,z,t;
    public IsChanged isChanged=new IsChanged();
    private boolean needRedraw=false;
    private boolean needUpdateViewport=false;


    ImageRenderState() {
        prev = new PreviousState(new Rectangle(0, 0, 0, 0), 0.0, null, null, 0, 0, 0, null, null);
    }
    
    public void check(Rectangle srcRect, double mag, LUT[] luts, Calibration cal, int c, int z, int t) {
        this.srcRect = (Rectangle) srcRect.clone();
        this.mag = mag;
        this.minmaxs = MinMax.getMinMaxs(luts);
        this.cal = (Calibration) cal.clone();
        this.c = c;
        this.z = z;
        this.t = t;
        isChanged.srcRect= (!prev.srcRect.equals(this.srcRect)) || prev.mag!=this.mag;
        isChanged.minmax=false;
        if(prev.minmaxs!=null && prev.minmaxs.length==minmaxs.length){
            for(int i=0;i<minmaxs.length;i++) {
                if(minmaxs[i].min!=prev.minmaxs[i].min || minmaxs[i].max!=prev.minmaxs[i].max)isChanged.minmax=true;
            }
        }
        isChanged.cal=(prev.cal==null ||!prev.cal.equals(this.cal));
        isChanged.c=(this.c!=prev.c);
        isChanged.z=(this.z!=prev.z);
        isChanged.t=(this.t!=prev.t);
        isChanged.czt=(isChanged.c || isChanged.z || isChanged.t);
        isChanged.rotation=(prev.rot==null ||!prev.rot.equals(rotation));
        isChanged.gamma=isChanged.compareGamma(gamma, prev.gamma);
        isChanged.translate=isChanged.compareTranslation(translation, prev.translation);
        isChanged.supermag=(prev.supermag!=supermag);
        isChanged.zNearFar=isChanged.compareZNearFar(zNearFar, prev.zNearFar);
        isChanged.needRedraw=needRedraw;
        isChanged.needUpdateViewport=(needUpdateViewport || isChanged.srcRect || isChanged.zNearFar);
        needRedraw=false;
        needUpdateViewport=false;
        prev.update(this.srcRect, this.mag, this.minmaxs, this.cal, this.c, this.z, this.t, rotation, gamma);
    }

    public boolean shouldUpdateGlobalMatrix() {
        return isChanged.needUpdateViewport;
    }

    public boolean shouldUpdateVertices() {
        return isChanged.needUpdateViewport || isChanged.czt || isChanged.rotation || isChanged.translate || isChanged.supermag;
    }

    public boolean shouldRedraw() {
        return isChanged.minmax || isChanged.rotation || isChanged.srcRect || 
            isChanged.translate || isChanged.supermag || isChanged.zNearFar || 
            isChanged.cal || isChanged.needUpdateViewport || isChanged.needRedraw;
    }

    public void setNeedRedraw() {
        needRedraw=true;
    }
    
    public String toString() {
        return ("isChanged: c:"+isChanged.c+" z:"+isChanged.z+" t:"+isChanged.t+" cal:"+isChanged.cal+" minmax:"+isChanged.minmax+" srcRect:"+isChanged.srcRect+" slice:"+isChanged.slice+" rotation:"+isChanged.rotation);
    }
    /**
     * Returns the mutable rotation used by the renderer.
     *
     * <p>Package visibility keeps direct mutation inside the rendering package.
     * Public callers should use {@link JOGLImageCanvas#setEulerAngles(float[])}.
     * </p>
     */
    Quaternion rotation() {
        return rotation;
    }

    /** Replaces the rotation with the supplied quaternion values. 
     * Returns true if the rotation changed, false if it was the same as before.
    */
    boolean setRotation(Quaternion value) {
        if (value == null) {
            rotation.setIdentity();
        } else {
            rotation.set(value);
        }
        boolean wasChanged = (prev.rot==null || !prev.rot.equals(rotation));
        if(wasChanged) needRedraw=true;
        return wasChanged;
    }

    /** Replaces the rotation with the supplied Euler angles.
     * Returns true if the rotation changed, false if it was the same as before.
     */
    boolean setRotationEuler(float[] euler) {
        if(euler==null || euler.length!=3)return false;
        rotation.setFromEuler(euler[0], euler[1], euler[2]);
        boolean wasChanged = (prev.rot==null || !prev.rot.equals(rotation));
        if(wasChanged) needRedraw=true;
        return wasChanged;
    }

    boolean setGamma(float[] value) {
        gamma = value == null ? null : value.clone();
        return isChanged.compareGamma(gamma, prev.gamma);
    }

    /** Returns a snapshot so callers cannot mutate renderer-owned state. */
    float[] getGamma() {
        return gamma == null ? null : gamma.clone();
    }

    /** Internal accessor for per-frame rendering. Do not retain or mutate it. */
    float[] gammaForRendering() {
        return gamma;
    }

    float[] getTranslation() {
        return translation.clone();
    }

    boolean setTranslation(float[] value) {
        translation = value == null ? new float[] {0f, 0f, 0f} : value.clone();
        boolean wasChanged = isChanged.compareTranslation(translation, prev.translation);
        if(wasChanged) needRedraw=true;
        return wasChanged;
    }

    float getSupermag() {
        return supermag;
    }

    boolean adjustSupermag(float delta) {
        return setSupermag(supermag + delta);
    }

    boolean setSupermag(float value) {
        supermag = value;
        boolean wasChanged = (prev.supermag != supermag);
        if(wasChanged) needRedraw=true;
        return wasChanged;
    }

    float[] getZNearFar() {
        return zNearFar.clone();
    }

    boolean setZNearFar(float[] value) {
        zNearFar = (value == null ? new float[]{JCP.zNear, JCP.zFar} : value.clone());
        return isChanged.compareZNearFar(zNearFar, prev.zNearFar);
    }
    
    static class MinMax{
        public double min=0,max=0;
        public MinMax(double min, double max) {this.min=min;this.max=max;}
        public static MinMax[] getMinMaxs(LUT[] luts) {
            if(luts==null || luts.length==0)return null;
            MinMax[] minmaxs=new MinMax[luts.length];
            double min=0,max=0;
            for(int i=0;i<luts.length;i++) {
                if(luts!=null && i<luts.length) {min=luts[i].min;max=luts[i].max;}
                minmaxs[i]=new MinMax(min,max);
            }
            return minmaxs;
        }
    }
    
    static class IsChanged{
        public boolean c,z,t,czt,cal,minmax,srcRect, slice, rotation, gamma, needRedraw;
        public boolean translate, supermag, zNearFar, needUpdateViewport;

        public void reset() {
            slice=srcRect=minmax=cal=czt=c=z=t=rotation=gamma=needRedraw=false;
        }

        private boolean compareFloatArray(float[] fa1, float[] fa2) {
            if(fa1!=fa2) {
                if(fa1==null || fa2==null || fa1.length!=fa2.length)return true;
                for(int i=0;i<fa2.length;i++) {
                    if(fa1[i]!=fa2[i])return true;
                }
            }
            return false;
        }

        public boolean compareGamma(float[] gamma, float[] prevGamma) {
            return compareFloatArray(gamma, prevGamma);
        }

        public boolean compareTranslation(float[] translation, float[] prevTranslation) {
            return compareFloatArray(translation, prevTranslation);
        }

        public boolean compareZNearFar(float[] zNearFar, float[] prevZNearFar) {
            return compareFloatArray(zNearFar, prevZNearFar);
        }
    }

    static class PreviousState {
        public Rectangle srcRect;
        public double mag;
        public MinMax[] minmaxs;
        public Calibration cal;
        public int c,z,t;
        public Quaternion rot;
        public float[] gamma;
        public float[] translation;
        public float supermag;
        public float[] zNearFar;

        PreviousState(Rectangle srcRect, double mag, MinMax[] minmaxs, Calibration cal, int c, int z, int t, Quaternion rot, float[] gamma) {
            update(srcRect, mag, minmaxs, cal, c, z, t, rot, gamma);
        }

        public void update(Rectangle srcRect, double mag, MinMax[] minmaxs, Calibration cal, int c, int z, int t, Quaternion rot, float[] gamma) {
            this.srcRect = (Rectangle) srcRect.clone();
            this.mag = mag;
            this.minmaxs = (minmaxs == null ? null : minmaxs.clone());
            this.cal = (cal==null ? null :(Calibration) cal.clone());
            this.c = c;
            this.z = z;
            this.t = t;
            if(rot==null)this.rot=null;
            else if(this.rot==null)this.rot=new Quaternion(rot);
            else this.rot.set(rot);
            this.gamma = (gamma == null ? null : gamma.clone());
        }
    }
}
