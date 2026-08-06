# ERDDAP translations

Translate `src/main/resources/gov/noaa/pfel/erddap/util/messages.xml` into
various languages in `src/main/resources/gov/noaa/pfel/erddap/util/translatedMessages`.

## Usage

Using pip:

```
# Install Dependencies
pip install argostranslate lxml
# Run the translations
python translation/translate.py
```

Using uv:

```
uv run --with argostranslate --with lxml ./translation/translate.py
```
