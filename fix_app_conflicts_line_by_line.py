import os

directory = 'app'

for root, _, files in os.walk(directory):
    for file in files:
        if file.endswith('.kt') or file.endswith('.json'):
            file_path = os.path.join(root, file)
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            new_lines = []
            in_theirs = False
            modified = False
            for line in lines:
                if line.startswith('<<<<<<< ours'):
                    modified = True
                    continue
                if line.startswith('======='):
                    in_theirs = True
                    modified = True
                    continue
                if line.startswith('>>>>>>> theirs'):
                    in_theirs = False
                    modified = True
                    continue
                
                if not in_theirs:
                    new_lines.append(line)
            
            if modified:
                with open(file_path, 'w') as f:
                    f.writelines(new_lines)
                print(f"Fixed {file_path}")
