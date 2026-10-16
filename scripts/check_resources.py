#!/usr/bin/env python3
"""Check Vinum resource links and 1.21.1 recipe structure without launching Minecraft.

Optional vanilla and NeoForge jars enable checking references outside this mod.
Minecraft's own resource reload and in-game tests remain the schema authority.
"""

import argparse
import json
from pathlib import Path
import sys
import zipfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--minecraft-jar", type=Path)
    parser.add_argument("--neoforge-jar", type=Path)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1] / "src/main/resources"
    external = set()
    checked_namespaces = {"vinum"}
    for path, namespaces in ((args.minecraft_jar, {"minecraft"}),
                             (args.neoforge_jar, {"neoforge", "c"})):
        if path:
            with zipfile.ZipFile(path) as archive:
                external.update(archive.namelist())
            checked_namespaces.update(namespaces)
    errors = []
    documents = {}
    for path in root.rglob("*.json"):
        try:
            documents[path.relative_to(root).as_posix()] = json.loads(path.read_text())
        except (ValueError, UnicodeError) as error:
            errors.append(f"{path.relative_to(root)}: {error}")

    def check_ref(source, identifier, folder, suffix=".json"):
        namespace, name = identifier.split(":", 1) if ":" in identifier else ("minecraft", identifier)
        if namespace not in checked_namespaces:
            return
        candidate = folder.format(namespace=namespace) + "/" + name + suffix
        if not (root / candidate).is_file() and candidate not in external:
            errors.append(f"{source}: missing {candidate}")

    def walk(value):
        if isinstance(value, dict):
            yield value
            for child in value.values():
                yield from walk(child)
        elif isinstance(value, list):
            for child in value:
                yield from walk(child)

    recipes = 0
    translations = documents.get("assets/vinum/lang/en_us.json", {})
    for source, doc in documents.items():
        parts = Path(source).parts
        if (len(parts) > 2 and parts[0] == "data" and parts[2] in {"recipes", "loot_tables"}) or "/tags/items/" in source:
            errors.append(f"{source}: legacy plural data directory")
        if "/models/" in source:
            if "parent" in doc and not doc["parent"].startswith("builtin/"):
                check_ref(source, doc["parent"], "assets/{namespace}/models")
            for texture in doc.get("textures", {}).values():
                if not texture.startswith("#"):
                    check_ref(source, texture, "assets/{namespace}/textures", ".png")
            for element in doc.get("elements", []):
                if any(value < -16 or value > 32 for key in ("from", "to") for value in element[key]):
                    errors.append(f"{source}: model element exceeds Minecraft bounds")
                rotation = element.get("rotation")
                if rotation and (rotation.get("axis") not in {"x", "y", "z"}
                                 or rotation.get("angle") not in {-45, -22.5, 0, 22.5, 45}):
                    errors.append(f"{source}: unsupported native model rotation")
        if "/blockstates/" in source:
            name = Path(source).stem
            if source.startswith("assets/vinum/"):
                if f"block.vinum.{name}" not in translations:
                    errors.append(f"{source}: missing block translation")
                check_ref(source, "vinum:blocks/" + name, "data/{namespace}/loot_table")
            for node in walk(doc):
                if "model" in node:
                    check_ref(source, node["model"], "assets/{namespace}/models")
        if source.startswith("assets/vinum/models/item/"):
            name = Path(source).stem
            kind = "block" if f"assets/vinum/blockstates/{name}.json" in documents else "item"
            if f"{kind}.vinum.{name}" not in translations:
                errors.append(f"{source}: missing item display name")
        if "/recipe/" in source:
            recipes += 1
            recipe_path = source.split("/recipe/", 1)[1]
            advancement_path = "data/vinum/advancement/recipes/" + recipe_path
            advancement = documents.get(advancement_path)
            recipe_id = "vinum:" + recipe_path.removesuffix(".json")
            if not advancement or recipe_id not in advancement.get("rewards", {}).get("recipes", []):
                errors.append(f"{source}: missing recipe-book unlock advancement")
            else:
                ingredients = {("#" + node["tag"]) if "tag" in node else node["item"]
                               for node in walk(doc) if "tag" in node or "item" in node}
                unlocks = [criterion for criterion in advancement.get("criteria", {}).values()
                           if criterion.get("trigger") == "minecraft:inventory_changed"]
                if not any(predicate.get("items") in ingredients
                           for criterion in unlocks
                           for predicate in criterion.get("conditions", {}).get("items", [])):
                    errors.append(f"{advancement_path}: unlock must match a recipe ingredient")
            result = doc.get("result")
            if not isinstance(result, dict) or "id" not in result:
                errors.append(f"{source}: result must be a 1.21.1 item stack with id")
            elif result["id"].startswith("vinum:"):
                check_ref(source, result["id"].replace("vinum:", "vinum:item/"), "assets/{namespace}/models")
            for node in walk(doc):
                if "tag" in node:
                    check_ref(source, node["tag"], "data/{namespace}/tags/item")
                if "item" in node and node["item"].startswith("vinum:"):
                    check_ref(source, node["item"].replace("vinum:", "vinum:item/"), "assets/{namespace}/models")
            if doc.get("type") == "minecraft:crafting_shaped":
                pattern = doc["pattern"]
                symbols = set("".join(pattern)) - {" "}
                if symbols != set(doc["key"]) or len({len(row) for row in pattern}) != 1:
                    errors.append(f"{source}: malformed crafting pattern/key")
        if "/loot_table/" in source:
            for node in walk(doc):
                if node.get("type") == "minecraft:item" and node["name"].startswith("vinum:"):
                    check_ref(source, node["name"].replace("vinum:", "vinum:item/"), "assets/{namespace}/models")
        if "/advancement/" in source:
            if "parent" in doc:
                check_ref(source, doc["parent"], "data/{namespace}/advancement")
            for recipe_id in doc.get("rewards", {}).get("recipes", []):
                check_ref(source, recipe_id, "data/{namespace}/recipe")
            criteria = doc.get("criteria", {})
            for group in doc.get("requirements", []):
                if not group or any(criterion not in criteria for criterion in group):
                    errors.append(f"{source}: invalid advancement requirements")
            for criterion in criteria.values():
                if criterion.get("trigger") == "minecraft:inventory_changed":
                    for predicate in criterion.get("conditions", {}).get("items", []):
                        item = predicate.get("items")
                        if isinstance(item, str) and item.startswith("#"):
                            check_ref(source, item[1:], "data/{namespace}/tags/item")
        if "/worldgen/placed_feature/" in source and isinstance(doc.get("feature"), str):
            check_ref(source, doc["feature"], "data/{namespace}/worldgen/configured_feature")
        if "/neoforge/biome_modifier/" in source:
            check_ref(source, doc["features"], "data/{namespace}/worldgen/placed_feature")
            if doc["biomes"].startswith("#"):
                check_ref(source, doc["biomes"][1:], "data/{namespace}/tags/worldgen/biome")
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print(f"Validated {len(documents)} JSON resources and {recipes} recipes; namespaces: {', '.join(sorted(checked_namespaces))}.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
