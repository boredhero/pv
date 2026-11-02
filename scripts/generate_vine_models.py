#!/usr/bin/env python3
"""Native two-cell vineyard cordons, lobed leaves and individually pickable berry clusters.

No raster art is altered. Ripe berry tints come from wines.json grape_color, matching
its existing grape item. Models share geometry across varieties and use multipart
states so removing one bunch does not change the other or the permanent vine.
"""
import copy
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/vinum'
MODELS = ASSETS / 'models/block/vine'
FACES = ('north', 'south', 'east', 'west', 'up', 'down')
TEXTURES = {'wood': 'minecraft:block/oak_log', 'stem': 'minecraft:block/stripped_oak_log',
            'wire': 'minecraft:block/gray_concrete', 'leaf': 'minecraft:block/white_concrete',
            'berry': 'minecraft:block/white_concrete', 'particle': 'minecraft:block/oak_log'}


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + '\n')


def box(lo, hi, texture, tint=None):
    faces = {face: {'texture': '#' + texture} for face in FACES}
    if tint is not None:
        for face in faces.values():
            face['tintindex'] = tint
    return {'from': list(lo), 'to': list(hi), 'faces': faces}


def model(name, elements):
    for element in elements:
        assert all(-16 <= a < b <= 32 for a, b in zip(element['from'], element['to'])), name
    write(MODELS / (name + '.json'), {'parent': 'minecraft:block/block',
          'textures': TEXTURES, 'elements': elements})


def split(elements, upper):
    output = []
    bottom, top = (16, 32) if upper else (0, 16)
    for element in elements:
        lo, hi = element['from'], element['to']
        if lo[1] >= top or hi[1] <= bottom:
            continue
        piece = copy.deepcopy(element)
        piece['from'][1] = max(lo[1], bottom) - bottom
        piece['to'][1] = min(hi[1], top) - bottom
        output.append(piece)
    return output


def leaf(x, y, z, size, variant):
    # Five flat, stepped lobes read as grape leaves, rather than opaque leaf blocks.
    result = []
    for dx, dy, width, height in [(0, 0, .72, .9), (-.42, -.15, .42, .55),
                                  (.42, -.15, .42, .55), (-.3, -.49, .34, .38),
                                  (.3, -.49, .34, .38)]:
        result.append(box((x+(dx-width/2)*size, y+(dy-height/2)*size, z),
                          (x+(dx+width/2)*size, y+(dy+height/2)*size, z+.28), 'leaf', variant % 3))
    result.append(box((x-.11, y-size*.65, z-.025), (x+.11, y+size*.4, z+.31), 'leaf', 3))
    return result


support = [box((7, 0, 7), (9, 32, 9), 'wood'),
           box((0, 27, 7), (16, 29, 9), 'wood')]
# Lower thin training wires run continuously when trellises are placed in a row.
support += [box((0, y, 7.15), (16, y+.3, 7.5), 'wire') for y in (15.7, 22)]
for upper in (False, True):
    model('trellis_' + ('upper' if upper else 'lower'), split(support, upper))
model('trellis_item', support)
write(ASSETS / 'models/item/grape_trellis.json', {
    'parent': 'vinum:block/vine/trellis_item',
    'display': {'gui': {'rotation': [30, 225, 0], 'translation': [0, -3, 0], 'scale': [.34, .34, .34]}}})

for wild in (False, True):
    prefix = 'wild' if wild else 'trained'
    for age in range(8):
        height = (4, 8, 13, 14, 14, 14, 14, 14)[age] if wild else (6, 15, 26, 27, 27, 27, 27, 27)[age]
        stem_x = 8 if wild else 6.3
        elements = [box((stem_x-.55, 0, 6.4), (stem_x+.55, height, 7.5), 'stem')]
        if age >= 2:
            elements.append(box((.6, height-1.1, 6.2), (15.4, height, 7.2), 'stem'))
        positions = [(7, min(height, 6), 6, 2.5)]
        if age >= 1:
            positions += [(9, height-1, 7.7, 3.3)]
        if age >= 2:
            positions += [(2.4, height+.1, 6.0, 4), (12.8, height+.7, 7.4, 4.4),
                          (6, height+1.1, 8.5, 4.1), (10.5, height-2.5, 8.8, 3.6)]
        for index, (x, y, z, size) in enumerate(positions):
            elements += leaf(x, y, z, size, index)
        # Bloom is tiny pale flowers along two hanging inflorescences, not fruit yet.
        if age == 3:
            for x in (3.6, 12.2):
                elements.append(box((x-.12, height-4.5, 5), (x+.12, height-.5, 5.25), 'leaf', 3))
                elements.append(box((x-.12, height-.8, 5), (x+.12, height-.5, 6.8), 'leaf', 3))
                for index in range(5):
                    dx = (-.55 if index % 2 else .35)
                    y = height-1.5-index*.55
                    elements.append(box((x+dx, y, 4.8), (x+dx+.48, y+.48, 5.4), 'leaf', 4))
        for upper in (False, True):
            model(f'{prefix}_{age}_' + ('upper' if upper else 'lower'), split(elements, upper))
    for stage in range(4, 8):
        for side, x in [('left', 3.6), ('right', 12.2)]:
            # Berries grow from small green fruit into substantial conical bunches.
            size = .62 if stage == 4 else .94 if stage == 5 else 1.0
            top = 12.4 if wild else 25.4
            elements = [box((x-.16, top-2, 4.9), (x+.16, top+1.5, 5.25), 'leaf', 3),
                        box((x-.16, top+1.2, 4.9), (x+.16, top+1.6, 6.8), 'leaf', 3)]
            berries = [(-.9, -.7, -.5), (.9, -.9, -.45), (0, -.2, .7),
                       (-1.15, -2.1, .1), (.9, -2.25, .7), (.1, -2, -1),
                       (-.8, -3.45, -.6), (.85, -3.5, -.45), (0, -3.65, .8),
                       (-.35, -4.8, -.25), (.4, -4.75, .4), (.05, -5.9, -.1)]
            for index, (dx, dy, dz) in enumerate(berries):
                radius = (.84 if index < 9 else .72) * size
                cx, cy, cz = x+dx*size, top+dy*size, 4.6+dz*size
                # Two intersecting cuboids soften cube corners without overly smooth spheres.
                for width, depth in ((radius*.72, radius), (radius, radius*.72)):
                    elements.append(box((cx-width, cy-radius*.85, cz-depth),
                                        (cx+width, cy+radius*.85, cz+depth), 'berry', 10+index%5))
            # Model coordinates are local to the occupied upper/lower cell.
            model(f'{prefix}_bunch_{stage}_{side}', split(elements, not wild))

rotations = {'north': 0, 'east': 90, 'south': 180, 'west': 270}

def apply(name, rotation):
    return {'model': 'vinum:block/vine/' + name, 'y': rotation}

write(ASSETS / 'blockstates/grape_trellis.json', {'variants': {
    f'facing={facing},half={half}': apply('trellis_' + half, rotation)
    for facing, rotation in rotations.items() for half in ('lower', 'upper')}})
parts = []
for facing, rotation in rotations.items():
    for half in ('lower', 'upper'):
        parts.append({'when': {'facing': facing, 'half': half, 'trellised': 'true'},
                      'apply': apply('trellis_' + half, rotation)})
        for age in range(8):
            for wild in (False, True):
                if wild and half == 'upper':
                    continue
                prefix = 'wild' if wild else 'trained'
                condition = {'facing': facing, 'half': half, 'age': str(age), 'trellised': str(not wild).lower()}
                parts.append({'when': condition, 'apply': apply(f'{prefix}_{age}_{half}', rotation)})
                if age >= 4 and half == ('lower' if wild else 'upper'):
                    for side, masks in [('left', '1|3'), ('right', '2|3')]:
                        parts.append({'when': dict(condition, bunches=masks),
                                      'apply': apply(f'{prefix}_bunch_{age}_{side}', rotation)})
# Exhaustively verify multipart states: harvested clusters cannot remain visible.
for facing in rotations:
    for wild in (False, True):
        for half in (('lower',) if wild else ('lower', 'upper')):
            for age in range(8):
                for mask in range(4):
                    state = {'facing': facing, 'half': half, 'age': str(age),
                             'trellised': str(not wild).lower(), 'bunches': str(mask)}
                    selected = [part['apply']['model'] for part in parts
                                if all(state[key] in value.split('|') for key, value in part['when'].items())]
                    bunches = [name for name in selected if '_bunch_' in name]
                    expected = mask.bit_count() if age >= 4 and half == ('lower' if wild else 'upper') else 0
                    assert len(bunches) == expected, (state, selected)
                    assert len(selected) == (1 if wild else 2) + expected, (state, selected)
for variety in json.loads((ROOT / 'src/main/resources/vinum/wines.json').read_text()):
    name = 'crop_grape_' + variety['id']
    write(ASSETS / 'blockstates' / (name + '.json'), {'multipart': parts})
    for old in (ASSETS / 'models/block').glob(name + '_stage*.json'):
        old.unlink()
print('Generated shared vineyard geometry, eight growth stages and separate berry clusters.')
