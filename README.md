# Fn Flow Stages

Fn Flow Stages provides a way of connecting Fn functions in a declarative way, based on the [ASL specification](https://states-language.net). This defines a state machine, where a JSON document is passed between labelled states that perform actions depending on the state type and the contents of the document. Fn  Flow Stages is implemented using Fn Flow and the Fn Java FDK.

There are currently two ways of using Fn Flow Stages:

* The first method uses a machine definition that is packaged alongside the Fn function. The function is called with a JSON document that is passed to the initial state handler, and the machine then transitions between states until it reaches a terminal state (an end, success, or fail state).
* The second method allows the machine definition to be provided when calling the function. The function is called with an document that defines the machine, and returns a URL. Making an HTTP POST request to this URL with a JSON document starts the machine at the initial state, and progresses in the same way as in the first method described above.

## Instructions 

### Prerequisites (from the [Fn Flow user guide](https://github.com/fnproject/fdk-java/blob/master/docs/FnFlowsUserGuide.md#pre-requisites))

Before you get started, you will need to have the following things:

* Fn CLI
* Fn Java FDK
* Fn completer
* Docker-ce 17.06+ installed locally
* A Docker Hub account

#### Install the Fn CLI tool

To install the Fn CLI tool, just run the following:
```bash
$ curl -LSs https://raw.githubusercontent.com/fnproject/cli/master/install | sh
```

This will download a shell script and execute it. If the script asks for a password, that is because it invokes sudo.

#### Log in to DockerHub

You will also need to be logged in to your Docker Hub account in order to deploy functions.
```bash
$ docker login
```
#### Start a local Fn server and completer server

In a terminal, start the functions server:
```bash
$ fn start
```
Similarly, start the Fn Flow completer server and point it at the functions server API URL:

```bash
$ DOCKER_LOCALHOST=$(docker inspect --type container -f '{{.NetworkSettings.Gateway}}' functions)

$ docker run --rm  \
       -p 8081:8081 \
       -d \
       -e API_URL="http://$DOCKER_LOCALHOST:8080/r" \
       -e no_proxy=$DOCKER_LOCALHOST \
       --name completer \
       fnproject/completer:latest
```

### Running Fn Flow Stages with the packaged machine definition

Clone this repo
```bash
git clone https://github.com/fnproject/flow-stages.git
cd flow-stages
```
Build the function
```bash
$ fn build
```
Add a route to the function
```bash
$ fn routes create flow /stages
```
Configure the function with the API URL of the Flow completer server
```bash
$ fn apps config set flow COMPLETER_BASE_URL $DOCKER_LOCALHOST
```
The example machine definition provided with Fn Flow Stages (at `./src/main/resources/machine.json`) is a simple counting machine, that calls an incrementing function (`example/increment`) with the value of the `value` field in the document repeatedly, until it is equal to 3, then terminates successfully.

The incrementing function needs to be built (and a route created to it) before running the machine
```bash
$ cd increment
$ fn build
$ fn routes create example /increment
$ cd -
```
Finally, the function with a JSON document:
```bash
$ echo '{"value":1}' | fn call flow /stages
```
The result of the call above should be  `{"value": 3}`.
