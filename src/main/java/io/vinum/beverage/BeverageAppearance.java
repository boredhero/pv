package io.vinum.beverage;

import com.google.gson.*;
import io.vinum.barrel.BarrelFluidData;
import io.vinum.barrel.BeverageCatalog;
import io.vinum.config.BarrelConfig;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.fluids.FluidStack;

/** Packaged presentation data, independent of container items and process rules. */
public final class BeverageAppearance {
  public record Style(
      String bottleBaseType,
      int liquidColor,
      int bottleLabelPrimaryColor,
      int bottleLabelSecondaryColor,
      String name) {}

  private record Stage(String agingStage, double minimumAgeDays, Style style) {}

  private record Definition(Style base, List<Stage> stages) {}

  private static final Map<String, Integer> SHAPES = new LinkedHashMap<>();
  private static final Map<String, Definition> DEFINITIONS = new LinkedHashMap<>();

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
            });
    for (var entry : read("beverage_appearances.json").getAsJsonArray()) {
      var json = entry.getAsJsonObject();
      Style base = style(json, new Style("default", 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, ""));
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

  private static Style style(JsonObject data, Style fallback) {
    String shape = string(data, "bottleBaseType", fallback.bottleBaseType());
    if (!SHAPES.containsKey(shape))
      throw new IllegalArgumentException("Undefined bottle shape: " + shape);
    return new Style(
        shape,
        color(data, "liquidColor", fallback.liquidColor()),
        color(data, "bottleLabelPrimaryColor", fallback.bottleLabelPrimaryColor()),
        color(data, "bottleLabelSecondaryColor", fallback.bottleLabelSecondaryColor()),
        string(data, "name", fallback.name()));
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

  public static int model(FluidStack fluid) {
    return find(fluid).map(s -> SHAPES.get(s.bottleBaseType())).orElse(0);
  }

  /** Generated models encode palette channel and neutral shading in each tint index. */
  public static int tint(FluidStack fluid, int tint) {
    if (tint == 0) return 0xFFFFFFFF;
    int channel = tint >>> 8, shade = tint & 255;
    var style = find(fluid);
    int color =
        switch (channel) {
          case 1 -> style.map(Style::bottleLabelPrimaryColor).orElse(0xFFFFFFFF);
          case 2 -> style.map(Style::bottleLabelSecondaryColor).orElse(0xFFFFFFFF);
          default -> WineCatalog.color(fluid);
        };
    return 0xFF000000
        | (((color >> 16 & 255) * shade / 255) << 16)
        | (((color >> 8 & 255) * shade / 255) << 8)
        | ((color & 255) * shade / 255);
  }
}
