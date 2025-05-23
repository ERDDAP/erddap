import os

dokka_dir = "dokka/documentation"
dokka_ext = ".md"
dokka_package_list = "dokka/documentation/-e-r-d-d-a-p/package-list"

def find_files(src_filepath, extension):
    filepath_list = []
    #This for loop uses the os.walk() function to walk through the files and directories
    #and records the filepaths of the files to a list
    for root, dirs, files in os.walk(src_filepath):
        #iterate through the files currently obtained by os.walk() and
        #create the filepath string for that file and add it to the filepath_list list
        for file in files:
            #Checks to see if the root is '.' and changes it to the correct current
            #working directory by calling os.getcwd(). Otherwise root_path will just be the root variable value.
            if root == '.':
                root_path = os.getcwd() + "/"
            else:
                root_path = root
            filepath = root_path + "/" + file
            #Appends filepath to filepath_list if filepath does not currently exist in filepath_list
            # Also don't include auto generated documentation (dokka)
            if filepath not in filepath_list and filepath.endswith(extension):
                filepath_list.append(filepath)
    #Return filepath_list        
    return filepath_list


def process_file(file_path):
    with open(file_path, 'r') as file_handle:
        content = file_handle.read()
        
    # Remove the file. It will be re-written after processing, which for constructors
    # will be a slightly different path.
    os.remove(file_path)
    # * Replace all <br> with something else (I used !BR!).
    # * Replace all < with &lt;
    # * Replace all > with &gt;
    # * Replace all { with &#123;
    # * Replace all } with &#125;
    # * Put the br back, but with a tag close eg: <br/>
    output_content = content.replace('<br>','!BR!').replace('<','&lt;').replace('>','&gt;').replace('{','&#123;').replace('}','&#125;').replace('!BR!','<br/>')

    # Because the constructors for each class have the same name as the folder,
    # Docusaurus does not know which one to serve. The best behavior is achieved 
    # by renaming the constructor files and updating the links. I appended -constructor 
    # to the file names. The files are -script-calendar2.md, -script-math.md,
    # -script-math2.md, -script-row.md, and -script-string2.md.
    output_content = output_content.replace('-script-calendar2.md', '-script-calendar2-constructor.md')
    output_content = output_content.replace('-script-math.md', '-script-math-constructor.md')
    output_content = output_content.replace('-script-math2.md', '-script-math2-constructor.md')
    output_content = output_content.replace('-script-row.md', '-script-row-constructor.md')
    output_content = output_content.replace('-script-string2.md', '-script-string2-constructor.md')

    file_path = file_path.replace('-script-calendar2.md', '-script-calendar2-constructor.md')
    file_path = file_path.replace('-script-math.md', '-script-math-constructor.md')
    file_path = file_path.replace('-script-math2.md', '-script-math2-constructor.md')
    file_path = file_path.replace('-script-row.md', '-script-row-constructor.md')
    file_path = file_path.replace('-script-string2.md', '-script-string2-constructor.md')
    with open(file_path, 'w') as output_file_handle:
        output_file_handle.write(output_content)
      

file_list = find_files(dokka_dir, dokka_ext)
for file in file_list:
    process_file(file)

# Also update constructor references in the package-list
process_file(dokka_package_list)
