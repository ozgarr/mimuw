# Moore Machines Library
This project implements a dynamically loaded C library that simulates **Moore machines** -
deterministic finite-state machines used in synchronous digital systems.

## Features
- Binary Moore automata with:
  - _n_ one-bit inputs
  - _m_ one-bit outputs
  - _s_-bit internal state
- Custom transition and output functions
- Creation and deletion of simple or full machines
- Connecting and disconnecting automata inputs/outputs
- Synchronous, parallel execution of multiple machines
- Safe memory management with failure resistance (tested with Valgrind)

## Build
The library is built under Linux with the provided makefile, `make libma.so`

## Usage
Include the header `#include "ma.h"`, compile and link against the library.

## API Overview
### Creation and deletion
- `ma_create_full` - create a machine with custom transition/output functions and initial state.
- `ma_create_simple` - create a machine with # of state bits equal to # of output bits, identity transition function and zeroed state.
- `ma_delete` - safely delete the machine and all references to it
### Connections
- `ma_connect` - connect some inputs of one machine to outputs of another
- `ma_disconnect` - disconnect some inputs of a machine
### Inputs and state
- `ma_set_input` - set values of unconnected inputs
- `ma_set_state` - set a machine's internal state
### Output
- `ma_get_output` - get pointer to the machine's current output bits
### Execution
- `ma_step` - perform one synchronous step on an array of machines.

> [!NOTE]
> For detailed API documentation, see [API.md](API.md).
