#!/bin/bash

pushd `dirname $0` > /dev/null

VERSION_REGEX='[0-9]+\.[0-9]+\.[0-9]+(rc[0-9]+)?'
NEW_VERSION=$1

if [[ ! "`git branch --show-current`" == "main" ]]; then
  echo "`tput setaf 1`WARNING! You are not on the main branch!`tput op`"
fi

if [[ `git status --porcelain` ]]; then
  echo "You have uncommitted changes. Cannot automatically update version."
  exit 1
fi

if [[ "$NEW_VERSION" == "" ]]; then
  read -p "Enter new version number: " NEW_VERSION
fi

if ! echo $NEW_VERSION | grep -Eq "^$VERSION_REGEX$"; then
  echo "Specified version number $NEW_VERSION does not appear to be valid."
  exit 1
fi

sed -i -E "s/mod_version = $VERSION_REGEX/mod_version = $NEW_VERSION/" ./gradle.properties
git add ./gradle.properties
git commit -am "Version $NEW_VERSION"
git tag $NEW_VERSION

echo "Version updated to $NEW_VERSION. Use 'git push' followed by 'git push --tag' to update the remote."

popd > /dev/null
