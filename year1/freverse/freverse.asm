; -=- Description -=- 
; The program takes one argument (a file) and reverses its contents.
; If the wrong number or type of arguments is passed, or any syscall fails
; during the execution, the program will stop and return 1.
; If successful, returns 0.
; Author: Oskar Rowicki
;
; -=- Algorithm -=-
; The program works as follows:
; 1. Open the file passed as the argument.
; 2. Use lseek with seek_end and offset = 0 to find the size of the file.
; 3. Use mmap to map the file to memory.
; 4. Use two loops to reverse the data in the memory. The loops have a low and
;    a high index and go from both ends towards the middle.
;    a) Main loop: loads 8 bytes each into two registers from [low] and [high], 
;       reverses them using bswap, loads them back into the opposite ends.
;    b) Small loop: deals with the remaining middle bytes (there is 0-7).
;       Loads one byte from [low] into a register, 
;       swaps it around with [high], loads the register into [low].
; 5. Use munmap to unmap the file.
; 6. Close the file and exit.
;
; -=- Miscellaneous -=-
; The program takes advantage of the fact, that rbp = 1 for most of its
; duration. Instead of doing 'mov reg, imm32', it does 'lea reg, [ebp + imm8]'
; or sometimes 'mov reg, ebp'. This saves a few bytes. 
; Despite the code having both 32-bit and 64-bit registers, the comments
; refer to them all by their 64-bit names for consistency.
; Exiting section is in the middle of the code to save bytes on jumps.
; 
; -=- Register usage -=-
; Uses the standard registers for syscalls: 
; rdi, rsi, rdx, r10, r8, r9 for args; rax for call number
; Other than that:
; rbp = exit_code (1 for most of the program, set to 0 if no error occured)
; r12 = file descriptor (fd)
; rbx = file_size
; rax = low
; rsi = high
; rcx = scratch
; rdx = scratch

global _start

SYS_CLOSE equ 3
SYS_EXIT  equ 60

section .text
_start:
      xor   ebp, ebp          ; (xor + inc is smaller than mov 1)
      inc   ebp               ; exit_code = 1 (error by default)
      lea   eax, [ebp + 1]    ; rax = 2
    
      cmp   [rsp], rax        ; argc == 2 ?
      jne   .exit             ; argc != 2 -> exit with error


      ; -=- SYS_OPEN -=-      ; rax = 2 (SYS_OPEN) already
      mov   rdi, [rsp + 16]   ; rdi = argv[1] (file name)
      mov   esi, eax          ; flag = 2 (O_RDWR)
      xor   edx, edx          ; mode = 0

      syscall
      test  rax, rax          ; Check for error
      js    .exit             ; Exit with error
      mov   r12, rax          ; r12 = fd (file descriptor)
    
    
      ; -=- SYS_LSEEK -=-
      mov   rdi, rax          ; Set fd
      xor   esi, esi          ; offset = 0
      lea   edx, [ebp + 1]    ; whence = 2 (SEEK_END)
      lea   eax, [edx + 6]    ; rax = 7 (SYS_LSEEK)
    
      syscall                 ; rax = offset + file_size = file_size
      test  rax, rax          ; Check for error
      js    .close            ; Close file and exit with error
    
      jz    .no_error         ; file_size = 0 -> exit with no error
      mov   rbx, rax          ; rbx = file_size
    
    
      ; -=- SYS_MMAP -=-
      xor   edi, edi          ; addr = NULL
      mov   rsi, rbx          ; length = file_size
      lea   edx, [ebp + 2]    ; prot = 3 (READ | WRITE)
      mov   r10d, ebp         ; flags = 1 (MAP_SHARED)
      mov   r8, r12           ; Set fd
      xor   r9d, r9d          ; offset = 0
      lea   eax, [ebp + 8]    ; rax = 9 (SYS_MMAP)
    
      syscall
      test  rax, rax          ; Check for error
      js    .close            ; Close file and exit with error
    
    
      ; -=- Reversal loops setup -=-
      mov   rdi, rax              ; rdi = addr, rax = low
      lea   rsi, [rax + rbx - 8]  ; rsi = high
      jmp   .main_loop

;------------------------------------------------------
      ; -=- Exiting -=- 
.no_error:
      xor   ebp, ebp          ; Set exit_code to 0

      ; -=- SYS_CLOSE -=-
.close:
      mov   rdi, r12          ; Set fd
      mov   eax, SYS_CLOSE   
      syscall

      test  eax, eax          ; Check for error
      jns   .exit             ; No error -> exit
      xor   ebp, ebp
      inc   ebp               ; Error -> exit_code = 1

      ; -=- SYS_EXIT -=-
.exit:
      mov   eax, SYS_EXIT
      mov   edi, ebp          ; Set exit_code
      syscall
;------------------------------------------------------

      ; -=- Main loop -=-
.main_loop:
      cmp   rax, rsi          ; low ? high
      jge   .main_done        ; low >= high -> done
    
      mov   rcx, [rax]        ; rcx = low
      mov   rdx, [rsi]        ; rdx = high
      bswap rcx               ; Reverse rcx
      bswap rdx               ; Reverse rdx
      mov   [rax], rdx        ; low = reversed high
      mov   [rsi], rcx        ; high = reversed low
    
      add   rax, 8            ; rax = next 8-byte block
      sub   rsi, 8            ; rsi = previous 8-byte block
      jmp   .main_loop


      ; -=- Small loop -=-
.main_done:
      add   rsi, 7            ; [rsi] = byte before last reversed high block
.small_loop:
      cmp   rax, rsi          ; low ? high
      jge   .munmap           ; low >= high -> done
    
      mov   cl, [rax]         ; cl = low
      xchg  [rsi], cl         ; high = low, cl = high
      mov   [rax], cl         ; low = high
    
      inc   rax               ; rax = next byte
      dec   rsi               ; rsi = previous byte
      jmp   .small_loop

      ; -=- SYS_MUNMAP -=-    ; (rdi = addr already)
.munmap:
      mov   rsi, rbx          ; length = file_size
      lea   eax, [ebp + 10]   ; rax = 11 (SYS_MUNMAP)

      syscall
      test  eax, eax          ; Check for error
      js    .close            ; Close file and exit with error
    
      jmp   .no_error         ; No error occured -> change exit_code and exit
