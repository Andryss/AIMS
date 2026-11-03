import json
import os
import shutil
from pathlib import Path
from uuid import uuid4

import psycopg
from psycopg import sql
from psycopg.conninfo import make_conninfo
import pytest

from demo.import_catalog import import_catalog, load_demo

ROOT = Path(__file__).resolve().parents[1]


@pytest.fixture
def demo_bundle(tmp_path):
    root = tmp_path / 'bundle'
    root.mkdir()
    shutil.copy2(ROOT / 'catalog.json', root / 'catalog.json')
    shutil.copytree(ROOT / 'demo', root / 'demo', ignore=shutil.ignore_patterns('__pycache__'))
    return root


def test_loads_all_species_and_curated_originals():
    entries = load_demo(ROOT)
    assert len(entries) == 50
    assert entries[0][0]['classificationKey'] == 'starfield_angler'
    assert all(len(photos) == 3 for _, photos in entries)
    assert all(path.is_file() for _, photos in entries for path in photos)


@pytest.mark.parametrize('variant', ['changed-photo', 'missing-photo', 'duplicate-key', 'missing-key', 'outside-path'])
def test_rejects_invalid_bundle_before_database_or_storage_changes(demo_bundle, variant):
    path = demo_bundle / 'demo/manifest.json'
    manifest = json.loads(path.read_text())
    photo = demo_bundle / 'demo' / manifest['items'][0]['photos'][0]['image']
    if variant == 'changed-photo':
        photo.write_bytes(b'changed')
    elif variant == 'missing-photo':
        photo.unlink()
    elif variant == 'duplicate-key':
        manifest['items'][1]['classificationKey'] = manifest['items'][0]['classificationKey']
    elif variant == 'missing-key':
        manifest['items'].pop()
    else:
        manifest['items'][0]['photos'][0]['image'] = '../catalog.json'
    path.write_text(json.dumps(manifest))
    with pytest.raises(ValueError):
        load_demo(demo_bundle)


@pytest.fixture
def demo_database():
    url = os.environ.get('AIMS_DEMO_TEST_DATABASE_URL')
    if not url:
        pytest.skip('Set AIMS_DEMO_TEST_DATABASE_URL to run PostgreSQL import tests')
    schema = 'aims_demo_test_' + uuid4().hex
    with psycopg.connect(url, autocommit=True) as connection:
        connection.execute(sql.SQL('CREATE SCHEMA {}').format(sql.Identifier(schema)))
        try:
            connection.execute(sql.SQL('SET search_path TO {}').format(sql.Identifier(schema)))
            connection.execute('CREATE TABLE app_user (id bigserial primary key)')
            connection.execute('CREATE TABLE incident (id bigserial primary key)')
            tables = ROOT.parents[1] / 'backend/src/main/resources/liquibase/table'
            connection.execute((tables / 'stored_file.sql').read_text())
            connection.execute((tables / 'alien.sql').read_text())
            yield make_conninfo(url, options='-csearch_path=' + schema)
        finally:
            connection.execute(sql.SQL('DROP SCHEMA {} CASCADE').format(sql.Identifier(schema)))


def test_dry_run_creates_neither_rows_nor_storage(demo_database, tmp_path):
    storage = tmp_path / 'files'
    assert import_catalog(demo_database, storage, apply=False) == {'wouldCreate': 50, 'skipped': 0}
    with psycopg.connect(demo_database) as connection:
        assert connection.execute('SELECT count(*) FROM alien').fetchone()[0] == 0
    assert not storage.exists()


def test_import_is_repeatable_and_preserves_user_edits(demo_database, tmp_path):
    storage = tmp_path / 'files'
    assert import_catalog(demo_database, storage, apply=True) == {'created': 50, 'skipped': 0}
    original_files = {p.name: p.read_bytes() for p in storage.iterdir()}
    with psycopg.connect(demo_database) as connection:
        assert connection.execute('SELECT count(*) FROM stored_file').fetchone()[0] == 150
        connection.execute("UPDATE alien SET name='Уточнённое имя', description='Наблюдение',"
                           " threat_level=2, photo_file_ids='[]' WHERE classification_key='starfield_ashta'")
        metadata = connection.execute('SELECT storage_id,file_size FROM stored_file').fetchall()
        assert all(len(original_files[key]) == size for key, size in metadata)
    assert import_catalog(demo_database, storage, apply=True) == {'created': 0, 'skipped': 50}
    assert {p.name: p.read_bytes() for p in storage.iterdir()} == original_files
    with psycopg.connect(demo_database) as connection:
        assert connection.execute('SELECT count(*) FROM alien').fetchone()[0] == 50
        assert connection.execute('SELECT count(*) FROM stored_file').fetchone()[0] == 150
        assert connection.execute("SELECT name,description,threat_level,photo_file_ids FROM alien"
                                  " WHERE classification_key='starfield_ashta'").fetchone() == (
                                      'Уточнённое имя', 'Наблюдение', 2, [])


def test_database_failure_rolls_back_rows_and_removes_written_files(demo_database, tmp_path):
    with psycopg.connect(demo_database) as connection:
        connection.execute("""
            CREATE FUNCTION reject_demo() RETURNS trigger LANGUAGE plpgsql AS $$
            BEGIN
                IF NEW.classification_key = 'starfield_anklyosaurus' THEN
                    RAISE EXCEPTION 'Rejected demo record';
                END IF;
                RETURN NEW;
            END $$;
            CREATE TRIGGER reject_demo BEFORE INSERT ON alien FOR EACH ROW EXECUTE FUNCTION reject_demo();
        """)
    storage = tmp_path / 'files'
    with pytest.raises(psycopg.errors.RaiseException, match='Rejected demo record'):
        import_catalog(demo_database, storage, apply=True)
    assert list(storage.iterdir()) == []
    with psycopg.connect(demo_database) as connection:
        assert connection.execute('SELECT count(*) FROM alien').fetchone()[0] == 0
        assert connection.execute('SELECT count(*) FROM stored_file').fetchone()[0] == 0


def test_storage_failure_rolls_back_database(demo_database, tmp_path):
    storage = tmp_path / 'occupied'
    storage.write_text('existing content')
    with pytest.raises(FileExistsError):
        import_catalog(demo_database, storage, apply=True)
    assert storage.read_text() == 'existing content'
    with psycopg.connect(demo_database) as connection:
        assert connection.execute('SELECT count(*) FROM alien').fetchone()[0] == 0
        assert connection.execute('SELECT count(*) FROM stored_file').fetchone()[0] == 0


def test_adds_missing_demo_photos_to_existing_species_without_duplicates(demo_database, tmp_path):
    storage = tmp_path / 'files'
    import_catalog(demo_database, storage, apply=True)
    with psycopg.connect(demo_database) as connection:
        connection.execute("UPDATE alien SET photo_file_ids=jsonb_build_array(photo_file_ids->0)")
        originals = connection.execute('SELECT id,photo_file_ids FROM alien ORDER BY id').fetchall()
        removed = connection.execute("DELETE FROM stored_file WHERE id NOT IN "
                                     "(SELECT (photo_file_ids->>0)::bigint FROM alien) RETURNING storage_id").fetchall()
        for (storage_id,) in removed:
            (storage / storage_id).unlink()
        connection.execute("UPDATE alien SET name='Уточнённое имя', description='Наблюдение', threat_level=2"
                           " WHERE classification_key='starfield_ashta'")
    assert import_catalog(demo_database, storage, add_photos=True) == {
        'wouldCreate': 0, 'skipped': 0, 'wouldUpdate': 50}
    assert len(list(storage.iterdir())) == 50
    assert import_catalog(demo_database, storage, apply=True, add_photos=True) == {
        'created': 0, 'skipped': 0, 'updated': 50}
    files = {p.name: p.read_bytes() for p in storage.iterdir()}
    assert len(files) == 150
    with psycopg.connect(demo_database) as connection:
        updated = connection.execute('SELECT id,photo_file_ids FROM alien ORDER BY id').fetchall()
        assert all(new_id == old_id and len(ids) == 3 and ids[0] == first[0]
                   for (new_id, ids), (old_id, first) in zip(updated, originals))
        assert connection.execute("SELECT name,description,threat_level FROM alien"
                                  " WHERE classification_key='starfield_ashta'").fetchone() == (
                                      'Уточнённое имя', 'Наблюдение', 2)
        assert {row[0] for row in connection.execute('SELECT content_type FROM stored_file')} == {
            'image/jpeg', 'image/png'}
    assert import_catalog(demo_database, storage, apply=True, add_photos=True) == {
        'created': 0, 'skipped': 50, 'updated': 0}
    assert {p.name: p.read_bytes() for p in storage.iterdir()} == files
