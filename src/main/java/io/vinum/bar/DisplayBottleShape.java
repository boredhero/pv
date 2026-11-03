package io.vinum.bar;

/** Upright bottle profiles in block units. Fill height integrates the tapered interior volume. */
public enum DisplayBottleShape {
    WINE(16, new double[]{.060,.074,.074,.026,.026}),
    SPIRIT(4, new double[]{.080,.090,.090,.032,.032}),
    TEQUILA(12, new double[]{.050,.058,.080,.031,.031});
    private static final double[] HEIGHTS = {0,.025,.34,.43,.54};
    public static final double LIQUID_TOP = .51;
    private final int sides;
    private final double[] radii;
    DisplayBottleShape(int sides, double[] radii) { this.sides=sides; this.radii=radii; }
    public int sides() { return sides; }
    public int sections() { return HEIGHTS.length-1; }
    public double height(int index) { return HEIGHTS[index]; }
    public double radius(double height) {
        for (int i=0;i<sections();i++) if (height<=HEIGHTS[i+1]) {
            double t=Math.clamp((height-HEIGHTS[i])/(HEIGHTS[i+1]-HEIGHTS[i]),0,1);
            return radii[i]+(radii[i+1]-radii[i])*t;
        }
        return radii[radii.length-1];
    }
    /** The common polygon area coefficient cancels in volume ratios. */
    public double volumeBelow(double height) {
        double volume=0;
        for (int i=0;i<sections();i++) {
            double upper=Math.min(height,HEIGHTS[i+1]);
            if (upper<=HEIGHTS[i]) break;
            double a=radii[i], b=radius(upper);
            volume+=(upper-HEIGHTS[i])*(a*a+a*b+b*b)/3;
        }
        return volume;
    }
    public double fillHeight(double fraction) {
        double target=volumeBelow(LIQUID_TOP)*Math.clamp(fraction,0,1);
        double low=0,high=LIQUID_TOP;
        for(int i=0;i<40;i++){double mid=(low+high)/2;if(volumeBelow(mid)<target)low=mid;else high=mid;}
        return (low+high)/2;
    }
}
