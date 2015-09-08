#!/bin/bash

ideaVersion="14.1.4"

if [ ! -d ./idea-IC/lib ]; then
    wget http://download.jetbrains.com/idea/ideaIC-${ideaVersion}.tar.gz

    # Unzip IDEA
    tar zxf ideaIC-${ideaVersion}.tar.gz
    rm -rf ideaIC-${ideaVersion}.tar.gz
    ls

    # Move the versioned IDEA folder to a known location
    ideaPath=$(find . -name 'idea-IC*' | head -n 1)
    echo ${ideaPath}
    mv ${ideaPath} ./idea-IC
fi

