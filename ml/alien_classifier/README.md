# Классификатор инопланетян

MobileNetV3 Small для 50 видов [Starfield Fauna](https://github.com/tesselwait/Starfield_Fauna). Обучается классифицирующий слой, остальная сеть заморожена.

## Файлы

| Файл | Назначение |
| --- | --- |
| `prepare_dataset.py` | Команда распаковки, проверки изображений, поиска повторов и создания манифеста. |
| `train.py` | Команда проверки готовности, обучения, оценки и сохранения модели. |
| `export.py` | Экспорт выбранного checkpoint в ONNX, проверка численной точности и эталоны для Java. |
| `archive_dataset.py` | Импорт полного ZIP или его частей с проверкой полноты и путей. |
| `dataset.py` | Чтение каталога и списка источников, проверка ключей и контрольных сумм. |
| `model.py` | MobileNet, заморозка слоёв, преобразование изображений в тензоры. |
| `evaluate.py` | Top-1/top-3, macro-F1, матрица ошибок и выбор порога по validation. |
| `training_control.py` | Параметры запуска, early stopping, лимит времени и завершение процессов. |
| `catalog.json` | 50 видов: ключ класса, исходная метка, названия, описание и оценка угрозы. |
| `sources.json` | Источники датасета и описаний, фотографии для сверки, шкала и основания оценок угрозы. |
| `source_files.json` | Полный список исходных изображений: пути и Drive ID для проверки комплектности архива. |
| `requirements.txt` | Зависимости подготовки данных. |
| `requirements-train.txt` | Дополнительно PyTorch и Torchvision для обучения. |
| `requirements-export.txt` | Дополнительно ONNX, ONNX Script и CPU-runtime для экспорта и проверки. |
| `requirements-test.txt` | Дополнительно pytest для проверок. |
| `tests/` | Проверки импорта, целостности данных, модели, метрик и ограничений ресурсов. |

Вручную запускаются `prepare_dataset.py`, `train.py` и `export.py`; остальные Python-файлы — внутренние модули.

```text
data/archives/         # исходные ZIP
data/raw/              # распакованные изображения
data/prepared/         # manifest.csv, labels.json, report.json
data/raw-archives.json # хеши ZIP и сведения о распаковке
data/cache/            # кэш признаков замороженной сети
data/torch/            # исходные предобученные веса
artifacts/<run>/       # checkpoint, настройки и результаты оценки
```

`data/` и `artifacts/` исключены из Git. Русские названия и описания — авторская адаптация сведений вики, справочников и фотографий для демонстрационной базы знаний. Угроза оценивается по шкале AIMS от 1 до 10; это не игровая характеристика. Один визуальный класс может включать мирные и хищные популяции. Предположения и недостаток наблюдений отмечены в описаниях, ссылки и основания оценок — в `sources.json`.

## Установка

Python 3.11+. Из корня репозитория:

```sh
cd ml/alien_classifier
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

Следующие команды выполняются из `ml/alien_classifier`.

## Датасет

Скачать [всю папку 240p](https://drive.google.com/drive/folders/1xQqYR1VHP3TSzQ7bXP3OKtWqcF34q8yz) через меню её названия → «Скачать». Все полученные ZIP положить в `data/archives/`.

```sh
python prepare_dataset.py --archives data/archives/*.zip  # первый импорт; data/raw ещё не существует
python prepare_dataset.py                               # повторная проверка распакованных данных
python train.py                                         # проверка готовности без обучения
```

Точные повторы исключаются из манифеста с приоритетом test → validation → train; исходные файлы сохраняются. Близкие совпадения перечисляются в `data/prepared/report.json`. Разбиение кадров видео не гарантирует независимость сцен.

## Обучение и результаты

```sh
pip install -r requirements-train.txt
python train.py --run --epochs 100 --patience 10 --output artifacts/run-001
python train.py --help
```

Нужно новое имя директории для каждого запуска. По умолчанию: 10 эпох, early stopping после 3 эпох без улучшения validation loss, batch 64, до 2 загрузчиков и CPU-потоков, лимит 30 минут. Через `--epochs` можно задать до 100 эпох, через `--patience` — от 1 до 20 эпох ожидания улучшения. Устройство — CUDA/MPS; CPU выбирается явно через `--device cpu`.

```text
artifacts/<run>/best.pt                 # модель с лучшим validation loss
artifacts/<run>/last.pt                 # последний checkpoint и прогресс
artifacts/<run>/labels.json             # порядок классов и их ключи
artifacts/<run>/preprocessing.json      # параметры обработки изображений
artifacts/<run>/run.json                # настройки, версии, время и результат запуска
artifacts/<run>/history.csv             # loss по эпохам
artifacts/<run>/validation_metrics.json # показатели validation
artifacts/<run>/metrics.json            # показатели полного очищенного test
artifacts/<run>/confusion_matrix.csv    # матрица ошибок
artifacts/<run>/mistakes.json            # ошибочно классифицированные изображения
```

## Экспорт ONNX

```sh
pip install -r requirements-export.txt
python export.py --run-dir artifacts/run-001 \
    --images data/raw/test/angler/400-angler.png
```

Экспортируется `best.pt` завершённого запуска. Результат — новая директория `artifacts/<run>/onnx/`; существующую можно сохранить, выбрав другой путь через `--output`. Обучение при экспорте не выполняется.

```text
onnx/model.onnx           # RGB float32 NCHW [batch,3,224,224] → 50 logits
onnx/labels.json          # индекс выхода → стабильный ключ вида
onnx/preprocessing.json   # размер, обрезка, нормализация, EXIF и правила округления
onnx/model-manifest.json  # версии, хеши, порог из validation и результаты проверки
onnx/references/          # изображения, входные тензоры и ожидаемые logits для Java
```

Перед публикацией комплекта проверяются совпадение с PyTorch и batch 1/3. Эталоны включают портретную геометрию, grayscale, RGBA и EXIF-поворот; файлы `.f32` содержат float32 little-endian без заголовка, формы указаны в `references/cases.json`. Допуски сравнения logits записаны там же и в манифесте. CPU-замеры измеряют только выполнение модели, без чтения и обработки изображения.

## Тесты

```sh
pip install -r requirements-test.txt
python -m pytest tests -q
```
