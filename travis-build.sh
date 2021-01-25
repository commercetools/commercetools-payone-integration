#! /bin/bash

set -e

export REPO="commercetools/commercetools-payone-integration"
export DOCKER_TAG=`if [ "$TRAVIS_BRANCH" == "master" -a "$TRAVIS_PULL_REQUEST" = "false" ]; then echo "latest"; else echo "wip-${TRAVIS_BRANCH//\//-}" ; fi`

echo "Building service jar file including all dependencies."
./gradlew stage

echo "Building Docker image using tag '${REPO}:${COMMIT}'."
docker build -t "${REPO}:${COMMIT}" .

docker login -u="${DOCKER_USERNAME}" -p="${DOCKER_PASSWORD}"

echo "Adding additional tag '${REPO}:${DOCKER_TAG}' to already built Docker image '${REPO}:${COMMIT}'."
docker tag $REPO:$COMMIT $REPO:$DOCKER_TAG
echo "Adding additional tag '${REPO}:travis-${TRAVIS_BUILD_NUMBER}' to already built Docker image '${REPO}:${COMMIT}'."
docker tag $REPO:$COMMIT $REPO:travis-$TRAVIS_BUILD_NUMBER
if [ "$TRAVIS_TAG" ]; then
  echo "Adding additional tag '${REPO}:${TRAVIS_TAG}' to already built Docker image '${REPO}:${COMMIT}'."
  docker tag $REPO:$COMMIT $REPO:${TRAVIS_TAG};
fi
echo "Pushing Docker images to repository '${REPO}' (all local tags are pushed)."
docker push $REPO
docker logout
