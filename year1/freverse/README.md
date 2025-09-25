# freverse
A simple x86 assembly program that allows the user to reverse the contents of a file.

## Usage
Run `./freverse file` to reverse `file`'s contents.

## Build
You can compile and link the program under Linux with:
```
nasm -f elf64 -w+all -w+error -w-unknown-warning -w-reloc-rel -o freverse.o freverse.asm
ld --fatal-warnings -o freverse freverse.o
```
## Features
The program has no artificial file size limit and works fast while maintaining a small footprint in terms of section sizes.

## Example
A file containing `This is great!\n` will turn into `\n!taerg si sihT`.
