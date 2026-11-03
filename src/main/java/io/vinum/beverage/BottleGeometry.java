package io.vinum.beverage;

/** Upright bottle profiles in block units. Fill height integrates the tapered interior volume. */
public final class BottleGeometry {
    private static final java.util.Map<net.minecraft.resources.ResourceLocation, BottleGeometry> PROFILES = load();
    public static final double LIQUID_TOP = .51;
    private final int sides;
    private final double[] radii;
    private final double[] heights;
    private BottleGeometry(int sides, double[] heights, double[] radii) {
        if (sides < 3 || sides > 64 || heights.length < 2 || heights.length != radii.length || heights[0] != 0)
            throw new IllegalArgumentException("Invalid bottle profile dimensions");
        for (int i=0;i<heights.length;i++) {
            if (!Double.isFinite(heights[i]) || !Double.isFinite(radii[i]) || radii[i] <= 0
                || (i > 0 && heights[i] <= heights[i-1])) throw new IllegalArgumentException("Invalid bottle profile section");
        }
        if (heights[heights.length-1] < LIQUID_TOP) throw new IllegalArgumentException("Bottle must contain its fill height");
        this.sides=sides; this.heights=heights.clone(); this.radii=radii.clone();
    }
    private static java.util.Map<net.minecraft.resources.ResourceLocation, BottleGeometry> load() {
        var result = new java.util.LinkedHashMap<net.minecraft.resources.ResourceLocation, BottleGeometry>();
        try (var stream=BottleGeometry.class.getResourceAsStream("/vinum/bottle_3d_types.json")) {
            if(stream==null)throw new IllegalStateException("Missing bottle geometry catalog");
            var data=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            for(var entry:data.entrySet()) {
                var json=entry.getValue().getAsJsonObject(); var sections=json.getAsJsonArray("sections");
                double[] heights=new double[sections.size()], radii=new double[sections.size()];
                for(int i=0;i<sections.size();i++){var section=sections.get(i).getAsJsonArray();heights[i]=section.get(0).getAsDouble();radii[i]=section.get(1).getAsDouble();}
                result.put(net.minecraft.resources.ResourceLocation.parse(entry.getKey()),new BottleGeometry(json.get("sides").getAsInt(),heights,radii));
            }
            return java.util.Collections.unmodifiableMap(result);
        } catch(java.io.IOException e){throw new ExceptionInInitializerError(e);}
    }
    public static BottleGeometry byId(net.minecraft.resources.ResourceLocation id) {
        var profile=PROFILES.get(id);
        if(profile==null)throw new IllegalArgumentException("Unknown bottle geometry: "+id);
        return profile;
    }
    public static java.util.Collection<BottleGeometry> values(){return PROFILES.values();}
    public int sides() { return sides; }
    public int sections() { return heights.length-1; }
    public double height(int index) { return heights[index]; }
    public double radius(double height) {
        for (int i=0;i<sections();i++) if (height<=heights[i+1]) {
            double t=Math.clamp((height-heights[i])/(heights[i+1]-heights[i]),0,1);
            return radii[i]+(radii[i+1]-radii[i])*t;
        }
        return radii[radii.length-1];
    }
    /** The common polygon area coefficient cancels in volume ratios. */
    public double volumeBelow(double height) {
        double volume=0;
        for (int i=0;i<sections();i++) {
            double upper=Math.min(height,heights[i+1]);
            if (upper<=heights[i]) break;
            double a=radii[i], b=radius(upper);
            volume+=(upper-heights[i])*(a*a+a*b+b*b)/3;
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
