#!/usr/bin/env python3
"""Fit native 3D item previews into a 12.5-pixel square without changing world models.

Run after block-model generators. Geometry rotations, including Minecraft's optional
rescale, are included in bounds. Only the item model's GUI transform is overridden.
"""
import copy
import itertools
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/vinum/models'
GUI_ROTATION = (30, 225, 0)
PREVIEW_SIZE = 12.5


def inherited(model, seen=None):
    seen = set() if seen is None else seen
    if 'elements' in model:
        return model['elements']
    parent = model.get('parent', '')
    if not parent.startswith('vinum:') or parent in seen:
        return []
    seen.add(parent)
    path = ROOT / (parent.split(':', 1)[1] + '.json')
    return inherited(json.loads(path.read_text()), seen) if path.exists() else []


def rotate(point, axis, angle):
    x, y, z = point
    c, s = math.cos(math.radians(angle)), math.sin(math.radians(angle))
    if axis == 'x':
        return x, c*y-s*z, s*y+c*z
    if axis == 'y':
        return c*x+s*z, y, -s*x+c*z
    return c*x-s*y, s*x+c*y, z


def projected_points(elements):
    points = []
    for element in elements:
        for point in itertools.product(*zip(element['from'], element['to'])):
            rotation = element.get('rotation')
            if rotation:
                origin = rotation['origin']
                local = tuple(value-center for value, center in zip(point, origin))
                local = rotate(local, rotation['axis'], rotation['angle'])
                if rotation.get('rescale'):
                    stretch = 1 / math.cos(math.radians(rotation['angle']))
                    along = 'xyz'.index(rotation['axis'])
                    local = tuple(value if i == along else value*stretch for i, value in enumerate(local))
                point = tuple(value+center for value, center in zip(local, origin))
            point = tuple(value-8 for value in point)
            # ItemTransform uses rotationXYZ: apply Z, then Y, then X to each vertex.
            for axis, angle in reversed(list(zip('xyz', GUI_ROTATION))):
                point = rotate(point, axis, angle)
            points.append(point[:2])
    return points


def carboy_preview():
    """Keep glass walls visible at icon size; original world geometry stays untouched."""
    path = ROOT/'item/carboy.json'
    item = json.loads(path.read_text())
    source = json.loads((ROOT/'block/carboy.json').read_text())
    elements = copy.deepcopy(source['elements'])
    for element in elements:
        if any(face['texture'] in {'#glass', '#airlock'} for face in element['faces'].values()):
            # Cropped automatic UVs missed vanilla glass's border entirely on small pieces.
            for face in element['faces'].values():
                face['uv'] = [0, 0, 16, 16]
    item['elements'] = elements
    item['textures'] = dict(source['textures'], glass='minecraft:block/glass',
                            airlock='minecraft:block/white_stained_glass')
    path.write_text(json.dumps(item, indent=2)+'\n')


def fit(path):
    model = json.loads(path.read_text())
    if not model.get('parent', '').startswith(('vinum:block/', 'vinum:machine/')):
        return False
    elements = inherited(model)
    if not elements:
        return False
    points = projected_points(elements)
    lo = [min(p[i] for p in points) for i in range(2)]
    hi = [max(p[i] for p in points) for i in range(2)]
    # Round down so serialization cannot push the largest dimension beyond its limit.
    scale = math.floor(min(.75, PREVIEW_SIZE/max(hi[i]-lo[i] for i in range(2)))*10000)/10000
    translation = [round(-(lo[i]+hi[i])*scale/2, 4) for i in range(2)]
    assert all(abs(p[i]*scale+translation[i]) <= PREVIEW_SIZE/2+.001 for p in points for i in range(2))
    model.setdefault('display', {})['gui'] = {
        'rotation': list(GUI_ROTATION), 'translation': translation+[0], 'scale': [scale]*3}
    path.write_text(json.dumps(model, indent=2)+'\n')
    return True


if __name__ == '__main__':
    carboy_preview()
    count = sum(fit(path) for path in (ROOT/'item').glob('*.json'))
    print(f'Fitted and checked {count} native item previews within {PREVIEW_SIZE}×{PREVIEW_SIZE} pixels.')
