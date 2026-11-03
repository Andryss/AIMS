"""Import the demo catalog into PostgreSQL and the existing local file storage."""

import argparse
import json
import os
import shutil
import sys
from pathlib import Path
from uuid import uuid4

import psycopg

from dataset import load_catalog, sha256

ROOT = Path(__file__).resolve().parents[1]


def load_demo(root=ROOT):
    root = Path(root)
    catalog = load_catalog(root / 'catalog.json')
    directory = (root / 'demo').resolve()
    items = json.loads((directory / 'manifest.json').read_text(encoding='utf-8'))['items']
    by_key = {item['classificationKey']: item for item in items}
    if len(by_key) != len(items) or set(by_key) != {item['classificationKey'] for item in catalog}:
        raise ValueError('Demo photos must cover each catalog key exactly once')
    entries = []
    for item in catalog:
        if not item['descriptionRu'].strip() or len(item['descriptionRu']) > 4000 or len(item['nameRu']) > 200:
            raise ValueError('Invalid demo name or description')
        photos = by_key[item['classificationKey']]['photos']
        if not 2 <= len(photos) <= 4 or len({photo['imageSha256'] for photo in photos}) != len(photos):
            raise ValueError('Each species must have 2–4 different demo photos')
        paths = []
        for photo in photos:
            path = (directory / photo['image']).resolve()
            if (not path.is_relative_to(directory) or path.suffix.lower() not in {'.jpg', '.jpeg', '.png'}
                    or not path.is_file() or sha256(path) != photo['imageSha256']):
                raise ValueError('Missing, changed or invalid demo photo: ' + photo['image'])
            paths.append(path)
        entries.append((item, paths))
    return entries


def missing_photos(connection, sources, file_ids):
    names = {row[0] for row in connection.execute(
        'SELECT file_name FROM stored_file WHERE id = ANY(%s)', (file_ids,))}
    return [source for source in sources if source.name not in names]


def store_photo(connection, storage_dir, source, created_files):
    storage_dir.mkdir(parents=True, exist_ok=True)
    storage_id = str(uuid4())
    target = storage_dir / storage_id
    with target.open('xb') as output:
        created_files.append(target)
        with source.open('rb') as content:
            shutil.copyfileobj(content, output)
    content_type = 'image/png' if source.suffix.lower() == '.png' else 'image/jpeg'
    return connection.execute('''
        INSERT INTO stored_file (storage_id, file_name, content_type, file_size)
        VALUES (%s, %s, %s, %s) RETURNING id
    ''', (storage_id, source.name, content_type, target.stat().st_size)).fetchone()[0]


def import_catalog(database_url, storage_dir, *, apply=False, add_photos=False, root=ROOT):
    entries = load_demo(root)
    storage_dir = Path(storage_dir).expanduser().resolve()
    created_files = []
    created = 0
    skipped = 0
    updated = 0
    with psycopg.connect(database_url, autocommit=True) as connection:
        try:
            with connection.transaction():
                if not apply:
                    existing = dict(connection.execute(
                        'SELECT classification_key, photo_file_ids FROM alien WHERE classification_key = ANY(%s)',
                        ([item['classificationKey'] for item, _ in entries],)))
                    updated = sum(bool(missing_photos(connection, sources, existing[item['classificationKey']]))
                                  for item, sources in entries if add_photos and item['classificationKey'] in existing)
                    result = {'wouldCreate': len(entries) - len(existing), 'skipped': len(existing) - updated}
                    if add_photos:
                        result['wouldUpdate'] = updated
                    return result

                for item, sources in entries:
                    row = connection.execute('''
                        INSERT INTO alien (classification_key, name, description, threat_level)
                        VALUES (%s, %s, %s, %s) ON CONFLICT (classification_key) DO NOTHING RETURNING id
                    ''', (item['classificationKey'], item['nameRu'], item['descriptionRu'], item['threatLevel'])).fetchone()
                    is_new = row is not None
                    if not is_new and not add_photos:
                        skipped += 1
                        continue
                    if is_new:
                        alien_id, file_ids = row[0], []
                    else:
                        alien_id, file_ids = connection.execute(
                            'SELECT id, photo_file_ids FROM alien WHERE classification_key=%s FOR UPDATE',
                            (item['classificationKey'],)).fetchone()
                        sources = missing_photos(connection, sources, file_ids)
                        if not sources:
                            skipped += 1
                            continue
                    file_ids = file_ids + [store_photo(connection, storage_dir, source, created_files)
                                           for source in sources]
                    connection.execute('UPDATE alien SET photo_file_ids=%s::jsonb WHERE id=%s',
                                       (json.dumps(file_ids), alien_id))
                    if is_new:
                        created += 1
                    else:
                        updated += 1
        except BaseException:
            if connection.closed:
                print('Database connection lost; copied files kept because commit outcome is unknown.', file=sys.stderr)
            else:
                for path in reversed(created_files):
                    path.unlink(missing_ok=True)
            raise
    result = {'created': created, 'skipped': skipped}
    if add_photos:
        result['updated'] = updated
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--database-url', default=os.environ.get('DATABASE_URL'),
                        help='PostgreSQL connection string; defaults to DATABASE_URL')
    parser.add_argument('--storage-dir', type=Path, required=True,
                        help='The same directory as aims.storage.base-path in the backend')
    parser.add_argument('--apply', action='store_true', help='Write data; without this flag only report missing keys')
    parser.add_argument('--add-photos', action='store_true',
                        help='Also append missing demo photos to existing species, preserving other fields')
    args = parser.parse_args()
    if not args.database_url:
        parser.error('Provide --database-url or DATABASE_URL')
    print(json.dumps(import_catalog(args.database_url, args.storage_dir, apply=args.apply, add_photos=args.add_photos)))


if __name__ == '__main__':
    main()
