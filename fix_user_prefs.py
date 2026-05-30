import re

file_path = 'app/src/main/java/com/ritesh/cashiro/data/preferences/UserPreferencesRepository.kt'

with open(file_path, 'r') as f:
    content = f.read()

# Replace the entire conflict block with the 'ours' part
fixed_content = re.sub(
    r'<<<<<<< ours\n(.*?)\n=======\n.*?\n>>>>>>> theirs\n?',
    r'\1\n',
    content,
    flags=re.DOTALL
)

with open(file_path, 'w') as f:
    f.write(fixed_content)

print("Conflicts fixed!")
