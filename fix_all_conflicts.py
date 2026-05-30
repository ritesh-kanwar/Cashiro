import os
import re

directory = 'parser-core/src/main/kotlin/com/ritesh/parser/core'

for root, _, files in os.walk(directory):
    for file in files:
        if file.endswith('.kt'):
            file_path = os.path.join(root, file)
            with open(file_path, 'r') as f:
                content = f.read()
            
            if '<<<<<<< ours' in content:
                # Replace the entire conflict block with the 'theirs' part
                fixed_content = re.sub(
                    r'<<<<<<< ours.*?=======\n(.*?)\n>>>>>>> theirs\n?',
                    r'\1\n',
                    content,
                    flags=re.DOTALL
                )
                with open(file_path, 'w') as f:
                    f.write(fixed_content)
                print(f"Fixed conflicts in {file_path}")
