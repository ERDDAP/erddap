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

Once the translations are completed, update messagesOld.xml with the contents of messages.xml. This is how the translation script determines what tags are new or have changed and so need to be translated.