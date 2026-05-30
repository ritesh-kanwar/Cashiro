import re

file_path = 'parser-core/src/main/kotlin/com/ritesh/parser/core/bank/ZemenBankParser.kt'

with open(file_path, 'r') as f:
    content = f.read()

# Replace the entire conflict block with the 'theirs' part
fixed_content = re.sub(
    r'<<<<<<< ours.*?=======\n(.*?)\n>>>>>>> theirs\n?',
    r'\1\n',
    content,
    flags=re.DOTALL
)

with open(file_path, 'w') as f:
    f.write(fixed_content)

print("Conflicts fixed!")
