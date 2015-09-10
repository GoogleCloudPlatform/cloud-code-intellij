#!/bin/bash

# Run the tests
gradle --info test

# Was our build successful?
stat=$?

if [ "${TRAVIS}" != true ]; then
    gradle clean
    rm -rf idea-IC
fi

# Return the build status
exit ${stat}

