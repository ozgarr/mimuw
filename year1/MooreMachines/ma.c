/*
  This library provides a memory-safe implementation of Moore machines -
  finite-state machines whose current output values are determined only by
  their current states.

  Author: Oskar Rowicki
*/
#include "ma.h"
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <stdbool.h>
#include <stdint.h>

/**
  -- Macros --
**/

/*
  Sets errno to the proper code for the given error and returns.
  x should be the function's return value when running into an error.
*/
#define VALUE_ERROR(x) errno = EINVAL; return x;
#define MEMORY_ERROR(x) errno = ENOMEM; return x;

/*
  Checks the nth bit in x (indexing from 0 starting from
  the least significant bit), returns the state of the bit.
*/
#define BIT_STATE(x, n) (((x)>>(n)) & (uint64_t)1)

/* Amount of bits held in one uint64 */
#define BLOCK_WIDTH 64

#define NO_BIT 0

/**
  -- Struct definitions --
**/

/*
  Stores information about a connection to a single signal in a
  machine's output/input. bit_number refers to the index of
  the connected bit (indexing from 0 starting from
  the least significant bit). *machine is the address of the connected machine.

  Used to keep track of all connections between machines.
*/
typedef struct bit_connection {
  moore_t *machine;
  size_t bit_number;
} bit_connection_t;

/*
  List of bit_connections. Used to store all connections from a machine's
  output in order to properly disconnect all of them when deleting the machine.

  We need to do this to avoid referencing freed memory in ma_step.
*/
typedef struct connection_list_node {
  bit_connection_t connection;
  struct connection_list_node *next;
} node_t;

/*
  Stores a sequence of signals as bits in uint64s.
  The least significant bit of the first element of the array
  is the first signal.

  Used to represent a machine's output, input and state.
*/
typedef struct signal_sequence {
  uint64_t *array;
  size_t signal_count;
} signal_sequence_t;

/*
  Key struct, represents a moore machine.
  transition and output _functions determine the machine's state and
  output value respectively.

  input_connections holds information about machines connected to this machine's
  input, the array has input.signal_count elements. If the nth element of the
  array has .machine = a_out and .bit_number = m, then that means the nth input
  signal of the machine is connected to the mth output signal of a_out.

  output_connections holds information about machines connected to this
  machine's output, the array has output.signal_count elements, however since
  one output signal can be connected to multiple inputs, the array holds lists
  of connections. The functionality is analogous to input_connections.
  (If the nth element of the array has a node with .machine = a_in and
  .bit_number = m, then that means the nth output signal of the machine
  is connected to the mth input signal of a_in.)
*/
struct moore {
  transition_function_t transition_function;
  output_function_t output_function;

  signal_sequence_t input, output, state;

  bit_connection_t *input_connections;
  node_t **output_connections;
};

/**
  -- Function declarations --
**/

/* Calculates the amount of 64bit blocks needed to store n bits. */
static size_t calculate_size(size_t bit_count);

/*
  - List related -
*/

/*
  Creates a new connection_list node with values of the bit_connection
  given by the parameters.
*/
static node_t *create_new_node(moore_t *machine, size_t bit_number);

static void insert_at_head(node_t **head, node_t *new_node);
static void delete_first_node(node_t **head);

/*
  Deletes the first node in a connection list that matches
  the machine and bit_number given as parameters.
*/
static void delete_matching_node(node_t **head, moore_t *machine,
                                 size_t bit_number);

/*
  - Moore related -
*/

/*
  Allocates a uint64_t array big enough to hold signal_count bits.
  Returns a signal_sequence struct that holds the address of that array
  and the signal_count.
*/
static signal_sequence_t create_sequence(size_t signal_count);

/*
  Frees all the arrays held in *machine (input, output, state .array;
  input_connections, output_connections).
*/
static void moore_free_arrays(moore_t *machine);

/*
  Sets up *machine's struct.
  Sets it's function parameters to the given functions.
  Creates signal_sequences for input, output and state based on
  _length parameters. Allocates the output_connections array
  and (if n > 0) the input_connections array.

  Returns true if all allocations were successful, otherwise returns false.
*/
static bool moore_set_parameters(moore_t *machine, size_t input_length,
                                 size_t output_length, size_t state_length,
                                 transition_function_t transition_function,
                                 output_function_t output_function);

/*
  Goes through *machine's output_connections array, removes connections
  to the *machine's output from all machines.
*/
static void remove_outgoing_connections(moore_t *machine);

/*
  - ma_step related -
*/

/*
  Goes through *machine's input_connections and updates *machine's input.array
  to properly reflect the current state of connected machines' outputs.
*/
static void refresh_input_array(moore_t *machine);

/*
  Allocates a buffer for every machine's state. Runs the machines'
  transition functions to calculate the new states. Copies the states over
  to their "machine->state.array"s

  The buffer allows for structural integrity in case of memory failure.
*/
static int calculate_next_states(moore_t *at[], size_t num);

/**
  -- Function definitions --
**/

size_t calculate_size(size_t bit_count) {
  /* take the ceiling of bit_count divided by BLOCK_WIDTH */
  if (bit_count > SIZE_MAX - BLOCK_WIDTH + 1) {
    return 0;
  }
  size_t size = (bit_count + BLOCK_WIDTH - 1) / BLOCK_WIDTH;
  return size;
}

node_t *create_new_node(moore_t *machine, size_t bit_number) {
  node_t *result = (node_t *)malloc(sizeof(node_t));

  if (!result) {
    MEMORY_ERROR(NULL);
  }

  result->connection.machine = machine;
  result->connection.bit_number = bit_number;
  result->next = NULL;
  return result;
}

void insert_at_head(node_t **head, node_t *new_node) {
  new_node->next = *head;
  *head = new_node;
}

void delete_first_node(node_t **head) {
  if (!*head) {
    return;
  }

  node_t *tmp = *head;
  *head = (*head)->next;
  free(tmp);
}

void delete_matching_node(node_t **head, moore_t *machine, size_t bit_number) {
  if (!*head) {
    return;
  }
  node_t *p = *head;
  node_t *q = (*head)->next;
  bool matching = false;

  /* check if first node matches */
  if (p->connection.machine == machine &&
      p->connection.bit_number == bit_number) {
    delete_first_node(head);
    return;
  }

  /* go through the rest of the list until node matches */
  while (!matching && q) {
    if (q->connection.machine == machine &&
        q->connection.bit_number == bit_number) {
      matching = true;
    } else {
      p = p->next;
      q = q->next;
    }
  }

  if (!q) {
    return;
  }

  /* remove the matching node */
  p->next = q->next;
  free(q);
}

signal_sequence_t create_sequence(size_t signal_count) {
  signal_sequence_t result;

  if (!calculate_size(signal_count)) {
    result = (signal_sequence_t) {
      .signal_count = signal_count,
      .array = NULL
    };
  } else {
    result = (signal_sequence_t) {
      .signal_count = signal_count,
      .array =
        (uint64_t *)calloc(calculate_size(signal_count), sizeof(uint64_t))
    };
  }

  return result;
}

void moore_free_arrays(moore_t *machine) {
  if (machine->input.array) {
    free(machine->input.array);
  }
  if (machine->output.array) {
    free(machine->output.array);
  }
  if (machine->state.array) {
    free(machine->state.array);
  }
  if (machine->input_connections) {
    free(machine->input_connections);
  }
  if (machine->output_connections) {
    free(machine->output_connections);
  }
}

bool moore_set_parameters(moore_t *machine, size_t input_length,
                          size_t output_length, size_t state_length,
                          transition_function_t transition_function,
                          output_function_t output_function) {
  *machine = (moore_t) {
    .transition_function = transition_function,
    .output_function = output_function,
    .input = create_sequence(input_length),
    .output = create_sequence(output_length),
    .state = create_sequence(state_length),
    .output_connections = (node_t **)malloc(output_length * sizeof(node_t *)),
    .input_connections = NULL
  };

  if (input_length > 0) {
    machine->input_connections =
      (bit_connection_t *)malloc(input_length * sizeof(bit_connection_t));
  }

  /* initialize all node pointers as NULL (only if malloc was successful) */
  if (machine->output_connections) {
    for (size_t i = 0; i < output_length; i++) {
      machine->output_connections[i] = NULL;
    }
  }

  /* return true if no allocation failed */
  return (machine->state.array && machine->output.array &&
          machine->output_connections && (input_length == 0 || (
          machine->input.array && machine->input_connections)));
}

void remove_outgoing_connections(moore_t *machine) {
  /* iterate over array of connection lists */
  for (size_t i = 0; i < machine->output.signal_count; i++) {
    node_t *curr = machine->output_connections[i];

    /* go through a list */
    while (curr) {
      moore_t *a_in = curr->connection.machine;
      size_t bit_number = curr->connection.bit_number;

      /* remove the input connection from the connected machine */
      a_in->input_connections[bit_number].machine = NULL;
      a_in->input_connections[bit_number].bit_number = NO_BIT;

      /* connection removed so remove it from the list */
      node_t *tmp = curr;
      curr = curr->next;
      free(tmp);
    }
    machine->output_connections[i] = NULL;
  }
}

void refresh_input_array(moore_t *machine) {
  /* iterate over array of input connections */
  for (size_t in = 0; in < machine->input.signal_count; in++) {
    moore_t *a_out = machine->input_connections[in].machine;
    size_t out = machine->input_connections[in].bit_number;

    /* if a_out not NULL then this bit is connected */
    if (a_out) {
      /* calculate which block the bits are in */
      size_t out_array_index = out / BLOCK_WIDTH;
      size_t in_array_index = in / BLOCK_WIDTH;
      /* calculate what position in their blocks 'in' and 'out' have */
      size_t out_index = out % BLOCK_WIDTH;
      size_t in_index = in % BLOCK_WIDTH;

      /* determine state of the output signal */
      bool bit_state = BIT_STATE(a_out->output.array[out_array_index],
                                 out_index);
      /* create a mask to change only the specified bit */
      uint64_t mask = (uint64_t)1 << in_index;

      /* change the specified bit in input to match the connected output bit */
      if (bit_state) {
        machine->input.array[in_array_index] |= mask;
      } else {
        machine->input.array[in_array_index] &= ~mask;
      }
    }
  }
}

int calculate_next_states(moore_t *at[], size_t num) {
  /* allocate an array for state buffers */
  uint64_t **states_buffer = (uint64_t **)malloc(num * sizeof(uint64_t *));
  if (!states_buffer) {
    MEMORY_ERROR(-1);
  }

  /* allocate buffers for every machine's state */
  for (size_t i = 0; i < num; i++) {
    states_buffer[i] =
      (uint64_t *)calloc(calculate_size(at[i]->state.signal_count),
        sizeof(uint64_t));

    if (!states_buffer[i]) {
      for (size_t j = 0; j < i; j++) {
        free(states_buffer[j]);
      }

      free(states_buffer);
      MEMORY_ERROR(-1);
    }
  }

  /* run the transition functions */
  for (size_t i = 0; i < num; i++) {
    at[i]->transition_function(states_buffer[i], at[i]->input.array,
      at[i]->state.array, at[i]->input.signal_count, at[i]->state.signal_count);
  }

  /* copy the states over and free the buffers */
  for (size_t i = 0; i < num; i++) {
    memcpy(at[i]->state.array, states_buffer[i],
      calculate_size(at[i]->state.signal_count) * sizeof(uint64_t));
    free(states_buffer[i]);
  }

  /* free the array */
  free(states_buffer);
  return 0;
}

/**
  -- Library functions --
**/

moore_t *ma_create_full(size_t n, size_t m, size_t s, transition_function_t t,
                        output_function_t y, uint64_t const *q) {
  if (!m || !s || !t || !y || !q) {
    VALUE_ERROR(NULL);
  }

  moore_t *a = (moore_t *)malloc(sizeof(moore_t));
  if (!a) {
    MEMORY_ERROR(NULL);
  }

  if (!moore_set_parameters(a, n, m, s, t, y)) {
    moore_free_arrays(a);
    free(a);
    MEMORY_ERROR(NULL);
  }

  /* initialize input_connections to show no connections made yet */
  for (size_t i = 0; i < a->input.signal_count; i++) {
    a->input_connections[i].machine = NULL;
    a->input_connections[i].bit_number = NO_BIT;
  }

  memcpy(a->state.array, q, calculate_size(s) * sizeof(uint64_t));

  a->output_function(a->output.array, a->state.array,
    a->output.signal_count, a->state.signal_count);

  return a;
}

/* output_function that simply copies the state into the output */
static void identity(uint64_t *output, uint64_t const *state,
                     size_t m, size_t) {
  memcpy(output, state, calculate_size(m) * sizeof(uint64_t));
}

moore_t *ma_create_simple(size_t n, size_t m, transition_function_t t) {
  if (!m || !t) {
    VALUE_ERROR(NULL);
  }
  uint64_t *q = (uint64_t *)calloc(calculate_size(m), sizeof(uint64_t));
  if (!q) {
    MEMORY_ERROR(NULL);
  }
  moore_t *result = ma_create_full(n, m, m, t, identity, q);
  if (!result) {
    free(q);
    MEMORY_ERROR(NULL);
  }

  free(q);
  return result;
}

void ma_delete(moore_t *a) {
  if (!a) {
    return;
  }

  /* disconnect the entire input */
  if (a->input.signal_count > 0) {
    ma_disconnect(a, 0, a->input.signal_count);
  }

  /* remove references to this machine */
  remove_outgoing_connections(a);

  /* free the entire struct */
  moore_free_arrays(a);
  free(a);
}

int ma_connect(moore_t *a_in, size_t in, moore_t *a_out,
               size_t out, size_t num) {
  if (!a_in || !a_out || !num || in > SIZE_MAX - num ||
      out > SIZE_MAX - num || in + num > a_in->input.signal_count ||
      out + num > a_out->output.signal_count) {
    VALUE_ERROR(-1);
  }

  for (size_t i = 0; i < num; i++) {
    node_t *new_connection = create_new_node(a_in, in + i);
    if (!new_connection) {
      MEMORY_ERROR(-1);
    }

    /* remove old connection */
    ma_disconnect(a_in, in + i, 1);

    /* record connection in a_out */
    insert_at_head(&a_out->output_connections[out + i], new_connection);

    /* record connection in a_in */
    a_in->input_connections[in + i].machine = a_out;
    a_in->input_connections[in + i].bit_number = out + i;
  }

  return 0;
}

int ma_disconnect(moore_t *a_in, size_t in, size_t num) {
  if (!a_in || !num  || in > SIZE_MAX - num ||
      in + num > a_in->input.signal_count) {
    VALUE_ERROR(-1);
  }

  /* iterate over connections to input */
  for (size_t i = in; i < in + num; i++) {
    /* check if some machine is actually connected */
    if (a_in->input_connections[i].machine) {

      /* remove the connection from the connected machine's output_connections*/
      size_t a_out_bit_nr = a_in->input_connections[i].bit_number;
      delete_matching_node(&a_in->input_connections[i].
        machine->output_connections[a_out_bit_nr], a_in, i);

      /* remove the connection from a_in's input_connections */
      a_in->input_connections[i].machine = NULL;
      a_in->input_connections[i].bit_number = 0;
    }
  }

  return 0;
}

int ma_set_input(moore_t *a, uint64_t const *input) {
  if (!a || !input || !a->input.signal_count) {
    VALUE_ERROR(-1);
  }

  memcpy(a->input.array, input,
    calculate_size(a->input.signal_count) * sizeof(uint64_t));
  return 0;
}

int ma_set_state(moore_t *a, uint64_t const *state) {
  if (!a || !state) {
    VALUE_ERROR(-1);
  }

  memcpy(a->state.array, state,
    calculate_size(a->state.signal_count) * sizeof(uint64_t));

  a->output_function(a->output.array, a->state.array,
    a->output.signal_count, a->state.signal_count);
  return 0;
}

uint64_t const *ma_get_output(moore_t const *a) {
  if (!a) {
    VALUE_ERROR(NULL);
  }

  return a->output.array;
}

int ma_step(moore_t *at[], size_t num) {
  if (!at || !num) {
    VALUE_ERROR(-1);
  }
  for (size_t i = 0; i < num; i++) {
    if (!at[i]) {
      VALUE_ERROR(-1);
    }
  }

  /* update all the inputs */
  for (size_t i = 0; i < num; i++) {
    refresh_input_array(at[i]);
  }

  /* calculate next state for every machine */
  if (calculate_next_states(at, num) == -1) {
    MEMORY_ERROR(-1);
  }

  /* calculate new output for every machine */
  for (size_t i = 0; i < num; i++) {
    at[i]->output_function(at[i]->output.array, at[i]->state.array,
      at[i]->output.signal_count, at[i]->state.signal_count);
  }

  return 0;
}