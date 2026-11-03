package io.vinum.beverage;

import com.google.gson.*;
import io.vinum.barrel.BarrelFluidData;
import io.vinum.barrel.BeverageCatalog;
import io.vinum.config.BarrelConfig;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import io.vinum.item.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.fluids.FluidStack;

/** Packaged presentation data, independent of container items and process rules. */
public final class BeverageAppearance {
  /** Resource IDs are extensible design references, never persisted enum ordinals. */
  public record BottleType(ResourceLocation id) {}
  public record Bottle3DType(ResourceLocation id) {
    public BottleGeometry geometry() { return BottleGeometry.byId(id); }
  }
  public record GlassMaterial(ResourceLocation texture, int tint, float transparency) {
    public GlassMaterial {
      Objects.requireNonNull(texture);
      if (!Float.isFinite(transparency) || transparency < 0 || transparency > 1)
        throw new IllegalArgumentException("Glass transparency must be between zero and one");
    }
    public int color() { return ((int)((1 - transparency) * 255) << 24) | (tint & 0xFFFFFF); }
  }
  public record LabelPanel(float width, float bottom, float top, float u0, float v0, float u1, float v1) {
    public LabelPanel {
      if(!Float.isFinite(width)||width<=0||width>1||!Float.isFinite(bottom)||!Float.isFinite(top)||bottom<0||top<=bottom
          || !(u0>=0 && v0>=0 && u1<=1 && v1<=1 && u1>u0 && v1>v0)) throw new IllegalArgumentException("Invalid label panel");
    }
  }
  public record Label(ResourceLocation artwork, int primaryColor, int secondaryColor, LabelPanel panel) {}
  public enum ClosureType { CORK, CAP, NONE }
  public enum LiquidColorSource { FIXED, BATCH }
  public record Closure(ClosureType type, ResourceLocation texture, int color, double bottom, double top, double radius) {
    public Closure {
      if(!Double.isFinite(bottom)||!Double.isFinite(top)||!Double.isFinite(radius)||bottom<0||top<=bottom||radius<=0)
        throw new IllegalArgumentException("Invalid bottle closure dimensions");
    }
    public Closure(ClosureType type, ResourceLocation texture, int color) { this(type,texture,color,.535,.563,.028); }
  }
  public record Style(
      BottleType bottleType, Bottle3DType bottle3DType, GlassMaterial glassMaterial,
      Label label, Closure closure, int liquidColor, LiquidColorSource liquidColorSource, String name, String flavor) {
    /** Compatibility for callers of the original packaged appearance API. */
    public String bottleBaseType() { return bottleType.id().getPath(); }
    public int bottleLabelPrimaryColor() { return label.primaryColor(); }
    public int bottleLabelSecondaryColor() { return label.secondaryColor(); }
  }
  private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/block/white_concrete.png");
  private static final Style SPIRIT_DEFAULT = createDefault(false);
  private static final Style WINE_DEFAULT = createDefault(true);

  private static Style defaults(boolean wine) { return wine ? WINE_DEFAULT : SPIRIT_DEFAULT; }

  private static Style createDefault(boolean wine) {
    return new Style(new BottleType(ResourceLocation.fromNamespaceAndPath("vinum", wine ? "wine" : "default")),
        new Bottle3DType(ResourceLocation.fromNamespaceAndPath("vinum", wine ? "wine" : "spirit")),
        new GlassMaterial(WHITE, wine ? 0xFF5D8750 : 0xFF7FAAB5, wine ? .75f : .84f),
        new Label(WHITE, 0xFFE2D5B4, 0xFF705C36, new LabelPanel(.5f,.16f,.28f,0,0,1,1)),
        new Closure(wine ? ClosureType.CORK : ClosureType.CAP, WHITE, 0xFFAE8753),
        0xFFFFFFFF, wine ? LiquidColorSource.BATCH : LiquidColorSource.FIXED, "", "");
  }

  private record Stage(String agingStage, double minimumAgeDays, Style style) {}

  private record Definition(Style base, List<Stage> stages) {}

  private static final Map<String, Integer> SHAPES = new LinkedHashMap<>();
  private static final Map<String, Definition> DEFINITIONS = new LinkedHashMap<>();
  private static final Map<ResourceLocation, Style> EMPTY_VESSELS = new LinkedHashMap<>();
  private static final Set<ResourceLocation> LAYERED_ITEMS = new HashSet<>();

  static {
    read("bottle_shapes.json")
        .getAsJsonObject()
        .entrySet()
        .forEach(
            e -> {
              int value = e.getValue().getAsJsonObject().get("modelValue").getAsInt();
              if (value < 0 || SHAPES.containsValue(value))
                throw new IllegalArgumentException(
                    "Bottle model values must be unique nonnegative integers");
              SHAPES.put(e.getKey(), value);
              var definition=e.getValue().getAsJsonObject();
              if(definition.has("items")) for(var item:definition.getAsJsonArray("items"))
                LAYERED_ITEMS.add(ResourceLocation.fromNamespaceAndPath("vinum",item.getAsString()));
            });
    for (var entry : read("beverage_appearances.json").getAsJsonArray()) {
      var json = entry.getAsJsonObject();
      Style base = style(json, defaults(false));
      List<Stage> stages = new ArrayList<>();
      if (json.has("stages"))
        for (var stage : json.getAsJsonArray("stages")) {
          var data = stage.getAsJsonObject();
          double days = data.has("minimumAgeDays") ? data.get("minimumAgeDays").getAsDouble() : -1;
          if (!Double.isFinite(days) || days < -1)
            throw new IllegalArgumentException("Invalid appearance age threshold");
          stages.add(new Stage(string(data, "agingStage", ""), days, style(data, base)));
        }
      Definition definition = new Definition(base, List.copyOf(stages));
      if(json.has("items")) for(var item:json.getAsJsonArray("items"))
        if(EMPTY_VESSELS.putIfAbsent(ResourceLocation.parse(item.getAsString()),base)!=null)
          throw new IllegalArgumentException("Duplicate empty bottle appearance: "+item);
      for (var fluid : json.getAsJsonArray("fluids"))
        if (DEFINITIONS.putIfAbsent(fluid.getAsString(), definition) != null)
          throw new IllegalArgumentException("Duplicate beverage appearance: " + fluid);
    }
  }

  private BeverageAppearance() {}

  private static JsonElement read(String name) {
    try (var stream = BeverageAppearance.class.getResourceAsStream("/vinum/" + name)) {
      if (stream == null) throw new IllegalStateException("Missing " + name);
      return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
    } catch (java.io.IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private static String string(JsonObject data, String name, String fallback) {
    return data.has(name) ? data.get(name).getAsString() : fallback;
  }

  private static int color(JsonObject data, String name, int fallback) {
    if (!data.has(name)) return fallback;
    String hex = data.get(name).getAsString();
    if (!hex.matches("[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Invalid RGB " + name);
    return 0xFF000000 | Integer.parseInt(hex, 16);
  }

  private static LabelPanel panel(JsonObject label, LabelPanel fallback) {
    if(!label.has("panel")) return fallback;
    var p=label.getAsJsonObject("panel");
    var uv=p.has("uv")?p.getAsJsonArray("uv"):null;
    return new LabelPanel(p.has("width")?p.get("width").getAsFloat():fallback.width(),
        p.has("bottom")?p.get("bottom").getAsFloat():fallback.bottom(),p.has("top")?p.get("top").getAsFloat():fallback.top(),
        uv==null?fallback.u0():uv.get(0).getAsFloat(),uv==null?fallback.v0():uv.get(1).getAsFloat(),
        uv==null?fallback.u1():uv.get(2).getAsFloat(),uv==null?fallback.v1():uv.get(3).getAsFloat());
  }

  private static Style style(JsonObject data, Style fallback) {
    String shape = string(data, "bottleType", string(data, "bottleBaseType", fallback.bottleType().id().toString()));
    ResourceLocation inventory = ResourceLocation.parse(shape.contains(":") ? shape : "vinum:" + shape);
    if (!inventory.getNamespace().equals("vinum") || !SHAPES.containsKey(inventory.getPath()))
      throw new IllegalArgumentException("Undefined inventory bottle: " + inventory);
    var geometry = new Bottle3DType(ResourceLocation.parse(string(data, "bottle3DType", fallback.bottle3DType().id().toString())));
    geometry.geometry(); // Validate references while loading, not during rendering.
    JsonObject glass = data.has("glassMaterial") ? data.getAsJsonObject("glassMaterial") : new JsonObject();
    JsonObject label = data.has("label") ? data.getAsJsonObject("label") : new JsonObject();
    JsonObject closure = data.has("closure") ? data.getAsJsonObject("closure") : new JsonObject();
    return new Style(new BottleType(inventory), geometry,
        new GlassMaterial(ResourceLocation.parse(string(glass, "texture", fallback.glassMaterial().texture().toString())),
            color(glass, "tint", fallback.glassMaterial().tint()),
            glass.has("transparency") ? glass.get("transparency").getAsFloat() : fallback.glassMaterial().transparency()),
        new Label(ResourceLocation.parse(string(label, "artwork", fallback.label().artwork().toString())),
            color(label, "primaryColor", color(data, "bottleLabelPrimaryColor", fallback.label().primaryColor())),
            color(label, "secondaryColor", color(data, "bottleLabelSecondaryColor", fallback.label().secondaryColor())), panel(label, fallback.label().panel())),
        new Closure(ClosureType.valueOf(string(closure, "type", fallback.closure().type().name()).toUpperCase(Locale.ROOT)),
            ResourceLocation.parse(string(closure, "texture", fallback.closure().texture().toString())),
            color(closure, "color", fallback.closure().color()),
            closure.has("bottom")?closure.get("bottom").getAsDouble():fallback.closure().bottom(),
            closure.has("top")?closure.get("top").getAsDouble():fallback.closure().top(),
            closure.has("radius")?closure.get("radius").getAsDouble():fallback.closure().radius()),
        color(data, "liquidColor", fallback.liquidColor()),
        LiquidColorSource.valueOf(string(data,"liquidColorSource",fallback.liquidColorSource().name()).toUpperCase(Locale.ROOT)),
        string(data, "name", fallback.name()),
        string(data, "flavor", fallback.flavor()));
  }

  /** Single resolution boundary. Future per-stack design components belong here, not in renderers. */
  public static Style resolve(FluidStack fluid, ItemStack vessel) {
    return find(fluid).orElseGet(() -> EMPTY_VESSELS.getOrDefault(BuiltInRegistries.ITEM.getKey(vessel.getItem()),
        defaults(WineCatalog.isWine(fluid) || vessel.is(ModItems.WINE_BOTTLE_EMPTY.get()))));
  }


  public static Optional<Style> find(FluidStack fluid) {
    if (fluid.isEmpty()) return Optional.empty();
    Definition definition =
        DEFINITIONS.get(BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString());
    if (definition == null) return Optional.empty();
    long age = BarrelFluidData.getAge(fluid);
    String aging =
        BeverageCatalog.lookupFluid(fluid.getFluid()).map(d -> d.aging().stage(age)).orElse("");
    Style result = definition.base();
    for (Stage stage : definition.stages())
      if ((!stage.agingStage().isEmpty() && stage.agingStage().equals(aging))
          || (stage.minimumAgeDays() >= 0
              && age / (double) BarrelConfig.ticksPerDay() >= stage.minimumAgeDays()))
        result = stage.style();
    return Optional.of(result);
  }

  public static int model(FluidStack fluid) { return model(fluid, ItemStack.EMPTY); }
  public static boolean hasLayeredModel(ItemStack stack) { return LAYERED_ITEMS.contains(BuiltInRegistries.ITEM.getKey(stack.getItem())); }

  public static int model(FluidStack fluid, ItemStack vessel) {
    return SHAPES.get(resolve(fluid, vessel).bottleType().id().getPath());
  }

  /** Generated models encode palette channel and neutral shading in each tint index. */
  public static int tint(FluidStack fluid, int tint) { return tint(fluid, ItemStack.EMPTY, tint); }

  public static int tint(FluidStack fluid, ItemStack vessel, int tint) {
    if (tint == 0) return 0xFFFFFFFF;
    int channel = tint >>> 8, shade = tint & 255;
    var style = resolve(fluid, vessel);
    int color =
        switch (channel) {
          case 1 -> style.label().primaryColor();
          case 2 -> style.label().secondaryColor();
          case 3 -> style.glassMaterial().tint();
          default -> WineCatalog.color(fluid);
        };
    return 0xFF000000
        | (((color >> 16 & 255) * shade / 255) << 16)
        | (((color >> 8 & 255) * shade / 255) << 8)
        | ((color & 255) * shade / 255);
  }
}
