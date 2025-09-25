# Moore machines library - API Reference

## Types
- `typedef struct moore moore_t;`\ represents a single Moore machine:
- `typedef void (*transition_function_t)(uint64_t *next_state, uint64_t const *input, uint64_t const *state, size_t n, size_t s);`\
   Transition function: computes the next state based on the current state and input bits.
  - `next_state` - array to store the next state
  - `input` - input bit array
  - `state` - current state array
  - `n` - number of input bits
  - `s` - number of state bits
- `typedef void (*output_function_t)(uint64_t *output, uint64_t const *state, size_t m, size_t s);`\
  Output function: computes the output bits from the current state.
  - `output` - array to store output bits
  - `state` - current state array
  - `m` - number of output bits
  - `s` - number of state bits

## Creation and deletion
- `moore_t *ma_create_full(size_t n, size_t m, size_t s, transition_function_t t, output_function_t y, uint64_t const *q);`\
  Create a machine with custom transition/output functions and initial state. Returns `NULL` on error.
- `moore_t *ma_create_simple(size_t n, size_t s, transition_function_t t);`\
  Create a machine with # of state bits = # of output bits, zeroed initial state, and a simple identity output function. Returns `NULL` on error.
- `void ma_delete(moore_t *a);`\
  Delete a machine and free all memory, removes all references to the machine. Safe to call with `NULL`.

## Connections
- `int ma_connect(moore_t *a_in, size_t in, moore_t *a_out, size_t out, size_t num);`\
  Connect `num` consecutive inputs of `a_in` to outputs of `a_out`. Returns `0` on success, `-1` on error.
- `int ma_disconnect(moore_t *a_in, size_t in, size_t num);`\
  Disconnect num consecutive inputs of `a_in`. Returns `0` on success, `-1` on error.

## Inputs and state
- `int ma_set_input(moore_t *a, uint64_t const *input);`\
  Set values of unconnected inputs. Returns `0` on success, `-1` on error.
- `int ma_set_state(moore_t *a, uint64_t const *state);`\
  Set the internal state of a machine. Returns `0` on success, `-1` on error.

## Outputs
- `uint64_t const *ma_get_output(moore_t const *a);`\
  Get pointer to the machine’s current output bits. Returns `NULL` if `a == NULL`.

## Execution
- `int ma_step(moore_t *at[], size_t num);`\
  Execute one synchronous step on `num` machines in the array `at[]`. All machines update in parallel based on previous states and inputs. Returns `0` on success, `-1` on error.

## Notes
- Bit sequences are stored in uint64_t arrays, least significant bit first.
- Memory allocation failures are handled safely; no memory leaks occur.
- No artificial limits on automaton size beyond system memory or word size.
- Inputs that are connected to outputs are ignored by ma_set_input.

## Example usage
```
#include "ma.h"
#include <assert.h>

void t_one(uint64_t *next_state, uint64_t const *input,
           uint64_t const *old_state, size_t, size_t) {
  next_state[0] = old_state[0] + input[0];
}

void y_one(uint64_t *output, uint64_t const *state, size_t, size_t) {
  output[0] = state[0] + 1;
}

static int one(void) {
  const uint64_t q1 = 1, x3 = 3, *y;

  moore_t *a = ma_create_full(64, 64, 64, t_one, y_one, &q1);
  assert(a);

  y = ma_get_output(a);
  ASSERT(y != NULL);
  ASSERT(ma_set_input(a, &x3) == 0);
  ASSERT(y[0] == 2);
  ASSERT(ma_step(&a, 1) == 0);
  ASSERT(y[0] == 5);
  ASSERT(ma_step(&a, 1) == 0);
  ASSERT(y[0] == 8);
  ASSERT(ma_set_input(a, &q1) == 0);
  ASSERT(ma_set_state(a, &x3) == 0);
  ASSERT(y[0] == 4);
  ASSERT(ma_step(&a, 1) == 0);
  ASSERT(y[0] == 5);
  ASSERT(ma_step(&a, 1) == 0);
  ASSERT(y[0] == 6);

  ma_delete(a);
  return 0;
}
```
