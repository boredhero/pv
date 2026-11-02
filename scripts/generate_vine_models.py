#!/usr/bin/env python3
"""Native two-cell vineyard cordons, lobed leaves and individually pickable berry clusters.

No raster art is altered. Ripe berry tints come from wines.json grape_color, matching
its existing grape item. Models share geometry across varieties and use multipart
states so removing one bunch does not change the other or the permanent vine.
"""
import copy
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/vinum'
MODELS = ASSETS / 'models/block/vine'
FACES = ('north', 'south', 'east', 'west', 'up', 'down')
TEXTURES = {'wood': 'minecraft:block/oak_log', 'stem': 'minecraft:block/stripped_oak_log',
            'wire': 'minecraft:block/gray_concrete', 'leaf': 'vinum:block/vine/grape_leaf',
            'twig': 'minecraft:block/white_concrete', 'berry': 'vinum:block/vine/grape_berry',
            'particle': 'minecraft:block/oak_log'}


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + '\n')


def box(lo, hi, texture, tint=None):
    faces = {face: {'texture': '#' + texture} for face in FACES}
    if texture == 'berry':
        for face in faces.values():
            face['uv'] = [0, 0, 16, 16]
    if tint is not None:
        for face in faces.values():
            face['tintindex'] = tint
    return {'from': list(lo), 'to': list(hi), 'faces': faces}


def model(name, elements):
    for element in elements:
        assert all(-16 <= a < b <= 32 for a, b in zip(element['from'], element['to'])), name
    write(MODELS / (name + '.json'), {'parent': 'minecraft:block/block',
          'render_type': 'minecraft:cutout', 'textures': TEXTURES, 'elements': elements})


def bounds(element):
    points = [(x, y, z) for x in (element['from'][0], element['to'][0])
              for y in (element['from'][1], element['to'][1])
              for z in (element['from'][2], element['to'][2])]
    rotation = element.get('rotation')
    if rotation:
        angle = math.radians(rotation['angle'])
        a, b = {'x': (1, 2), 'y': (2, 0), 'z': (0, 1)}[rotation['axis']]
        origin = rotation['origin']
        rotated = []
        for point in points:
            value = list(point)
            first, second = point[a]-origin[a], point[b]-origin[b]
            value[a] = origin[a]+first*math.cos(angle)-second*math.sin(angle)
            value[b] = origin[b]+first*math.sin(angle)+second*math.cos(angle)
            rotated.append(value)
        points = rotated
    return ([min(p[axis] for p in points) for axis in range(3)],
            [max(p[axis] for p in points) for axis in range(3)])


def split(elements, upper):
    output = []
    bottom, top = (16, 32) if upper else (0, 16)
    for element in elements:
        lo, hi = bounds(element)
        if lo[1] >= top or hi[1] <= bottom:
            continue
        piece = copy.deepcopy(element)
        if 'rotation' in piece:
            # Leaf cards are wholly inside their owning cell, so their UV never gets sliced.
            assert lo[1] >= bottom and hi[1] <= top, (bottom, top, element)
            piece['from'][1] -= bottom
            piece['to'][1] -= bottom
            piece['rotation']['origin'][1] -= bottom
        else:
            piece['from'][1] = max(element['from'][1], bottom) - bottom
            piece['to'][1] = min(element['to'][1], top) - bottom
        output.append(piece)
    return output


def transform_point(point, rotation):
    if not rotation:
        return list(point)
    angle = math.radians(rotation['angle'])
    a, b = {'x': (1, 2), 'y': (2, 0), 'z': (0, 1)}[rotation['axis']]
    origin = rotation['origin']
    value = list(point)
    first, second = point[a]-origin[a], point[b]-origin[b]
    value[a] = origin[a]+first*math.cos(angle)-second*math.sin(angle)
    value[b] = origin[b]+first*math.sin(angle)+second*math.cos(angle)
    return value


def shoot(start, end, variant):
    # Short overlapping tapered segments follow a slight curve in 3D. Unlike an axis-aligned
    # vertical stem plus horizontal foot, this cannot leave the visible rectangular ladders.
    middle = [(a+b)/2 for a, b in zip(start, end)]
    middle[0] += .18 if variant % 2 else -.18
    middle[2] += .12
    result = []
    for section, (first, last) in enumerate(((start, middle), (middle, end))):
        count = max(1, math.ceil(math.dist(first, last)/.6))
        for step in range(count):
            a = [x+(y-x)*step/count for x, y in zip(first, last)]
            b = [x+(y-x)*(step+1)/count for x, y in zip(first, last)]
            radius = .085-(section+step/count)*.012
            result.append(box(tuple(min(x,y)-radius for x,y in zip(a,b)),
                              tuple(max(x,y)+radius for x,y in zip(a,b)), 'twig', 3))
    return result


def leaf(x, y, z, size, variant, axis='z', angle=0):
    # UV rotation supplies the full circle of leaf directions; native rotation tilts the plane.
    turn = (90, 180, 270, 0, 180, 90, 270, 180)[variant % 8]
    face = {'texture': '#leaf', 'uv': [0, 0, 16, 16], 'tintindex': variant % 3, 'rotation': turn}
    back = dict(face, uv=[16, 0, 0, 16], rotation=(360-turn) % 360)
    result = {'from': [x-size/2, y-size/2, z],
              'to': [x+size/2, y+size/2, z+.025],
              'faces': {'north': face, 'south': back}, 'shade': False}
    if angle:
        result['rotation'] = {'origin': [x, y, z], 'axis': axis, 'angle': angle, 'rescale': False}
    # The visible petiole tip is pixel (15,29) in the installed32px sprite. Match its centre,
    # including both the UV rotation and the element's rotation, before attaching a shoot.
    tip = transform_point([x+size*(.5-15.5/32), y+size*(.5-29.5/32), z+.0125],
                          {'origin': [x,y,z], 'axis': 'z', 'angle': turn})
    tip = transform_point(tip, result.get('rotation'))
    return result, tip


def canopy(age, wild):
    if age < 2:
        return [(7, 4, 6, 3, 'z', -22.5)] + ([] if age == 0 else [(8.5, 11, 7.5, 4, 'y', 22.5)])
    # Leaf stems sit on shoots running out from the permanent cordon. Three depth layers
    # form a continuous crown in a row while leaving the fruiting zone in front readable.
    positions = [
        (1.9, 28.4, 6.5, 5.3, 'z', -22.5), (6.0, 28.6, 7.1, 5.0, 'x', -22.5),
        (10.1, 28.2, 6.6, 5.4, 'z', 22.5), (14.4, 28.0, 7.3, 5.6, 'y', -22.5),
        (3.6, 27.2, 9.0, 5.5, 'y', 45), (8.0, 27.6, 9.5, 5.4, 'x', 45),
        (12.5, 26.5, 9.2, 5.6, 'y', -45), (1.0, 25.6, 8.2, 4.9, 'z', 22.5),
        (5.0, 26.6, 5.2, 5.2, 'y', -22.5), (10.9, 26.7, 5.7, 5.3, 'x', 22.5),
        (15.3, 25.6, 7.9, 4.8, 'z', -22.5), (7.8, 24.5, 6.0, 5.0, 'z', -22.5),
        (1.0, 23.0, 8.8, 4.6, 'y', 45), (14.6, 22.8, 9.6, 4.9, 'y', -22.5),
        (6.6, 21.8, 8.5, 4.4, 'x', 22.5), (9.6, 23.8, 10.2, 4.7, 'z', 22.5),
        (4.2, 24.5, 10.8, 4.8, 'x', -45), (11.5, 24.6, 11.2, 4.8, 'y', 22.5),
        # Broad overlapping outer shoots give the crown volume on both sides of the wire.
        (-.8, 27.0, 4.2, 6.0, 'y', -45), (4.0, 28.0, 3.2, 5.9, 'x', 45),
        (9.0, 27.7, 3.6, 6.1, 'z', -22.5), (16.2, 27.3, 4.0, 5.8, 'y', 45),
        (-.6, 24.2, 11.8, 5.9, 'y', 22.5), (3.9, 26.6, 13.3, 6.2, 'x', -45),
        (8.5, 27.5, 13.8, 6.0, 'z', 22.5), (13.1, 26.8, 13.1, 6.1, 'x', 45),
        (16.7, 23.8, 11.5, 5.5, 'y', -45), (6.0, 22.0, 12.4, 5.5, 'z', -22.5),
        (10.5, 21.3, 12.2, 5.3, 'y', 45), (8.0, 24.2, 2.6, 5.2, 'x', -45)]
    if age == 2:
        positions = positions[:10]
    if wild:
        # A compact wild bush has the same detailed foliage within its single occupied cell.
        return [(8+(x-8)*.78, 6+(y-16)*.52, 7+(z-7)*.75, size*.58, axis, angle)
                for x, y, z, size, axis, angle in positions]
    return positions


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
        for index, (x, y, z, size, axis, angle) in enumerate(canopy(age, wild)):
            card, tip = leaf(x, y, z, size, index, axis, angle)
            elements.append(card)
            anchor = ([max(.7, min(15.2, tip[0]+(1.1 if index%2 else -1.1))), height-.55, 6.7]
                      if age >= 2 else [stem_x, min(height-.2, tip[1]), 6.7])
            elements += shoot(anchor, tip, index)
        if not wild and age >= 2:
            # Side-facing leaves project away from the trunk on short attached petioles.
            for index, (x,y,z,size,axis,angle) in (
                    (24, (3.8,7.5,3.4,4.8,'x',45)),
                    (26, (10.3,12,4.5,4.7,'y',45))):
                card, tip = leaf(x,y,z,size,index,axis,angle)
                elements.append(card)
                elements += shoot([stem_x, tip[1]+.3, 6.7],tip,index)
        # A distinct flowering stage: branching panicles of cream-green florets
        # occupy the future fruit positions before small green berries develop.
        if age == 3:
            top = 12.4 if wild else 25.4
            for cluster, x in enumerate((3.6, 12.2)):
                neck = [x, top, 4.6]
                tip = [x+.25, top-3.7, 4.3]
                elements += shoot([x-.5, height-.55, 6.7], neck, cluster)
                elements += shoot(neck, tip, cluster+1)
                for tier in range(4):
                    width = 1.35-tier*.25
                    for side in (-1, 1):
                        start = [x+.06*tier, top-.45-tier*.78, 4.55]
                        end = [x+side*width, start[1]-.4, 4.0+(tier%2)*.45]
                        elements += shoot(start, end, tier)
                        # Three little flowers form each branchlet, separated enough to read
                        # as an airy inflorescence rather than a solid immature bunch.
                        for petal in range(3):
                            cx = end[0]+(petal-1)*.25
                            cy = end[1]+(.2 if petal == 1 else -.12)
                            cz = end[2]+(.15 if petal == 1 else -.12)
                            elements.append(box((cx-.18, cy-.18, cz-.18),
                                                (cx+.18, cy+.18, cz+.18), 'twig', 4))
        for upper in (False, True):
            model(f'{prefix}_{age}_' + ('upper' if upper else 'lower'), split(elements, upper))
        if not wild:
            model(f'branch_{age}_upper', split(elements[1:], True))
    for stage in range(4, 8):
        for side, x in [('left', 3.6), ('right', 12.2)]:
            # Berries grow from small green fruit into substantial conical bunches.
            size = .62 if stage == 4 else .94 if stage == 5 else 1.0
            top = 12.4 if wild else 25.4
            elements = [box((x-.16, top-2, 4.9), (x+.16, top+1.5, 5.25), 'twig', 3),
                        box((x-.16, top+1.2, 4.9), (x+.16, top+1.6, 6.8), 'twig', 3)]
            berries = []
            for row, (number, ring, y) in enumerate(((5, 1.05, -.8), (6, 1.3, -2.1),
                                                    (5, 1.0, -3.4), (3, .6, -4.65), (1, 0, -5.75))):
                for berry in range(number):
                    angle = berry*math.tau/number + row*.71
                    berries.append((math.cos(angle)*ring, y + math.sin(angle)*.13,
                                    math.sin(angle)*ring))
            for index, (dx, dy, dz) in enumerate(berries):
                radius = (.68 if index < 16 else .61) * size
                cx, cy, cz = x+dx*size, top+dy*size, 4.6+dz*size
                # A broad middle and narrower crown bevel the berries in all viewing directions.
                for width, height, depth in ((radius, radius*.72, radius),
                                             (radius*.75, radius, radius*.75)):
                    elements.append(box((cx-width, cy-height, cz-depth),
                                        (cx+width, cy+height, cz+depth), 'berry', 10+index%5))
            # Model coordinates are local to the occupied upper/lower cell.
            model(f'{prefix}_bunch_{stage}_{side}', split(elements, not wild))

model('branch_lower', [])

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
                if wild:
                    parts.append({'when': condition, 'apply': apply(f'{prefix}_{age}_{half}', rotation)})
                else:
                    parts.append({'when': dict(condition, branch='false'), 'apply': apply(f'{prefix}_{age}_{half}', rotation)})
                    branch_model = f'branch_{age}_upper' if half == 'upper' else 'branch_lower'
                    parts.append({'when': dict(condition, branch='true'), 'apply': apply(branch_model, rotation)})
                if age >= 4 and half == ('lower' if wild else 'upper'):
                    for side, masks in [('left', '1|3'), ('right', '2|3')]:
                        parts.append({'when': dict(condition, bunches=masks),
                                      'apply': apply(f'{prefix}_bunch_{age}_{side}', rotation)})
# Exhaustively verify multipart states: harvested clusters cannot remain visible.
for facing in rotations:
    for wild in (False, True):
        for half in (('lower',) if wild else ('lower', 'upper')):
            for age in range(8):
                for branch in (('false',) if wild else ('false', 'true')):
                  for mask in range(4):
                    state = {'facing': facing, 'half': half, 'age': str(age),
                             'trellised': str(not wild).lower(), 'bunches': str(mask), 'branch': branch}
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
