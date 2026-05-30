import os
import re

directory = 'app'

for root, _, files in os.walk(directory):
    for file in files:
        if file.endswith('.kt') or file.endswith('.json'):
            file_path = os.path.join(root, file)
            with open(file_path, 'r') as f:
                content = f.read()
            
            if '<<<<<<< ours' in content:
                # Replace the entire conflict block with the 'ours' part
                fixed_content = re.sub(
                    r'<<<<<<< ours\n(.*?)\n=======\n.*?\n>>>>>>> theirs\n?',
                    r'\1\n',
                    content,
                    flags=re.DOTALL
                )
                with open(file_path, 'w') as f:
                    f.write(fixed_content)
                print(f"Fixed conflicts in {file_path}")
