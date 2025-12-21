#!/bin/bash
for patch in ../filtered_patches/*.patch; do
    echo "==================================="
    echo "Applying $(basename "$patch")"
    if git am -3 "$patch"; then
        echo "Successfully applied via git am"
    else
        echo "Conflict detected! Falling back to apply --reject"
        git am --abort
        
        # Extract metadata
        AUTHOR=$(grep -m 1 "^From: " "$patch" | sed 's/^From: //')
        DATE=$(grep -m 1 "^Date: " "$patch" | sed 's/^Date: //')
        # Extract multi-line subject / commit message (up to the first diff)
        # Using awk to get the commit message body from the patch
        # The body is between the first blank line and the line '---'
        MSG=$(awk '/^$/{if(!started){started=1; next}} /^---$/{if(started){exit}} started{print}' "$patch")
        SUBJECT=$(grep -m 1 "^Subject: " "$patch" | sed 's/^Subject: \[PATCH.*\] //')
        
        FULL_MSG="$SUBJECT"$'\n\n'"$MSG"
        
        # Apply what we can
        git apply -3 "$patch" || git apply --reject "$patch" || true
        
        # Add all successfully modified files
        git add .
        
        # Clean up any rejected hunks
        find . -name "*.rej" -delete
        
        # Commit with the original author and date
        git commit -m "$FULL_MSG" --author="$AUTHOR" --date="$DATE" --allow-empty
    fi
done
