#!/usr/bin/env python3
"""Flat GUI views over preserved native models; placed/held/NONE views remain 3D."""
import json
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/vinum'

def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + '\n')

def plane(x0, y0, x1, y1, z, texture, tint=None):
    face = {'texture': '#' + texture, 'uv': [0, 0, 16, 16]}
    if tint is not None:
        face['tintindex'] = tint
    return {'from': [x0, y0, z], 'to': [x1, y1, z], 'shade': False,
            'faces': {'south': face, 'north': dict(face, uv=[16, 0, 0, 16])}}

def sprite(name, tier=0, airlock=False):
    model = {'parent': 'minecraft:block/block', 'gui_light': 'front', 'ambientocclusion': False,
             'textures': {'sprite': f'vinum:item/inventory/{name}', 'particle': f'vinum:item/inventory/{name}'},
             'elements': []}
    if tier:
        model['textures']['liquid'] = 'minecraft:block/white_concrete'
        # The glass sprite overlays this bounded, tintable fill plane. No glass is tinted.
        model['elements'].append(plane(4.6, 2.6, 11.2, 2.6 + 5.5 * tier / 4, 7.49, 'liquid', 0))
    model['elements'].append(plane(0, 0, 16, 16, 7.5, 'sprite'))
    if airlock:
        model['textures']['airlock'] = 'vinum:item/inventory/airlock'
        model['elements'].append(plane(10.5, 9.5, 16, 16, 7.51, 'airlock'))
    return model

def wrap(path, gui):
    path = ROOT / 'models' / (path + '.json')
    current = json.loads(path.read_text())
    if current.get('loader') == 'neoforge:separate_transforms':
        current['perspectives']['gui'] = gui
    else:
        overrides = current.pop('overrides', None)
        current = {'loader': 'neoforge:separate_transforms', 'base': current,
                   'perspectives': {'gui': gui}}
        if overrides:
            current['overrides'] = overrides
    write(path, current)

def carboys():
    wrap('item/airlock', sprite('airlock'))
    wrap('item/carboy', sprite('carboy'))
    for installed in (False, True):
        for tier in range(5):
            wrap(f'container/carboy_{tier}' + ('_airlock' if installed else ''), sprite('carboy', tier, installed))

def trellis():
    wrap('item/grape_trellis', sprite('grape_trellis'))

if __name__ == '__main__':
    carboys()
    trellis()
    print('Generated flat inventory views for carboys, airlock and trellis; native views preserved.')
