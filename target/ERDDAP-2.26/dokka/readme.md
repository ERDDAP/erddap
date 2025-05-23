# Generate the documentation
```
mvn dokka:dokka
```
# Prepare the documentation for serving

Run dokka/prepareDocs.py to prepare the dokka docs for serving in docusaurus. This does the below changes.

When it's finished, move the processed files to the documentation project.

## Escape characters needed for Docusaurus

Within the documentation directory:
* Replace all <br> with something else (I used !BR!).
* Replace all < with &lt;
* Replace all > with &gt;
* Replace all { with &#123;
* Replace all } with &#125;
* Put the br back, but with a tag close eg: <br/>

## Fix the main pages for each class

Because the constructors for each class have the same name as the folder, Docusaurus does not know which one to serve. The best behavior is achieved by renaming the constructor files and updating the links. I appended -constructor to the file names. The files are -script-calendar2.md, -script-math.md, -script-math2.md, -script-row.md, and -script-string2.md.

You should update the references in:
* package-list
* self reference in the file
* constructor reference in the index.md for the folder

# Move the fixed documentation to the documentation project

If there have been a lot of changes you may want to clear the folder and then re-add. If you do this it is recommended to recreate the Docusarus _category_.json files afterwards. If there's only additional methods or changes to the existing strings, you can likely replace existing files with the new ones.