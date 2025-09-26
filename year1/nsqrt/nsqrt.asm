global nsqrt
; == Algorithm ==
; The program calculates the square root digit-by-digit,
; the method is based on the binomial theorem.
;
; We don't compute T explicitly, but instead shift Q on the fly
; when comparing and subtracting.
;
; To account for + 4^{n - j} we add 2^{n - j - 1} to Q before shifting.
; Multiplying Q by 2^{n - j + 1}, we get
; 2^{n - j - 1} * 2^{n - j + 1} = 4^{n - j}, so it checks out.
; After comparing and possibly subtracting R and T we reset the changed bit.
; For j = n, we would have to add 1/2, so instead we skip this step and
; deal with the edge case in 'compare'.
;
; To compute T on the fly, we find {word_offset} and {bit_offset},
; get Q[index + word_offset] and Q[index + word_offset - 1] and
; shift the former to the left by {bit_offset} with bits shifting in
; from the right from the latter.
; {word_offset} = how many full 64-bit words are we shifting by
; {bit_offset} = shift count modulo 64
;
; When comparing and subtracting we assume that at any point R cannot be
; more than 2^64 times larger than T. Thus we don't check R words past
; R[Q_word_count + word_offset + 1].
;
; == Registers usage ==
; rdi = scratch
; rsi = *R
; rdx = word_count
; rbp = main loop index (n - j)
; rcx = bit_offset / scratch
; rbx = word_offset
; r8 = (n - j - 1) / 64
; r9 = scratch
; r10 = scratch
; r11 = *Q
; rax = scratch / CF
section .text
nsqrt:
      push rbx              ; Preserve callee-saved registers
      push rbp

      mov  ebp, edx         ; ebp = n - j (j = 0 before loop entry)
      shr  edx, 6           ; edx = n / 64 {word_count}

      ; == Zero out Q ==
      mov  r11, rdi         ; r11 = *Q

      mov  ecx, edx         ; ecx = word_count
      xor  eax, eax         ; eax = 0
      rep  stosq            ; (mov [rdi], rax; rdi += 8) rcx times

main_loop:
      dec  ebp              ; (n - j)-- <-> j++
      jz   compare          ; j = n -> dont add 2^{n-j-1}

      ; == Q += 2^{n-j-1} ==
      lea  edi, [rbp - 1]   ; edi = n - j - 1
      mov  r8d, edi
      shr  r8d, 6           ; r8d = (n - j - 1) / 64
      and  edi, 63          ; edi = (n - j - 1) % 64
      bts  [r11 + r8*8], rdi

; Creates T words on the fly from Q and compares them with R.
; Starts from index = word_count and goes until index < 0.
; Edge case for j = 1: index = word_count - 1 to avoid going out of bounds.
; We are doing that, because for j = 1, index + word_offset = 2 * word_count, so it
; would be out of bounds of R.
; We change rsi to contain the offset address, at the end of the loop
; we recover the original address of *R.
; r10 = higher, rax = lower
; r10 = Q[index], rax = Q[index - 1], either of these is 0 if their index
; is out of bounds.
compare:
      lea  ebx, [rbp + 1]   ; ebx = n - j + 1
      mov  ecx, ebx         ; ecx = n - j + 1
      shr  ebx, 6           ; ebx = (n - j + 1) / 64 {word_offset}

      xor  r10, r10         ; higher = 0
      mov  edi, edx         ; edi = word_count {index}

      cmp  edx, ebx         ; word_count == word_offset -> j = 1 (edge case)
      lea  rsi, [rsi + rbx*8] ; rsi = *R + word_offset * 8 (!!!)

      jne  .mov_lower       ; no edge case -> start with higher = 0

.loop:
      xor  eax, eax         ; lower = 0

      dec  edi              ; index--
      js   .loop_end        ; index < 0 -> exit loop

      mov  r10, [r11 + rdi*8] ; mov higher
      jz   .shift           ; index = 0 -> lower = 0, shift

.mov_lower:
      mov  rax, [r11 + rdi*8 - 8] ; mov lower

.shift:
      shld r10, rax, cl     ; Adjust for bit_offset

      cmp  [rsi + rdi*8], r10

      ja   subtract         ; Above -> R > T
      jb   smaller          ; Below -> R < T
      jmp  .loop            ; Even -> undetermined, loop

.loop_end:
      test ebp, ebp         ; for j = n, we didn't add the 1, so if R = T,
      jz   smaller          ; then R < T + 1, so jump to smaller

; Creates T words on the fly and subtracts them from R.
; Starts from index = (n - j - 1) / 64 and goes until index >= word_count.
; We don't start from 0, because we know everything in Q below that is 0s.
; Uses rax as CF in case a borrow occured. (lahf and sahf)
; In case of the edge case we skip subtraction to avoid a memory leak.
; r8 is set to (n - j - 1) / 64 thanks to the beginning and ending 
; of the main loop.
; r8 = index,
; rdi = higher, r9 = lower
subtract:
      xor  r9, r9            ; zero lower
      lahf                   ; load cf

.loop:
      mov  rdi, [r11 + r8*8] ; mov higher
      mov  r10, rdi          ; save higher for moving to lower

.sbb:
      shld rdi, r9, cl       ; Adjust for bit_offset

      sahf                   ; extract CF from rax
      sbb  [rsi + r8*8], rdi
      lahf                   ; load CF into rax

      mov  r9, r10           ; lower = higher
      inc  r8d               ; index++

      cmp  r8d, edx          ; index ? word_count
      jl   .loop             
      jg   .exit              

      xor  edi, edi          ; zero higher
      cmp  ebx, edx          ; edge case, skip subtraction
      jne  .sbb

.exit:
      ; == Q += 2^{n-j} ==
      mov  edi, ebp          ; edi = n - j
      shr  edi, 6            ; edi = (n - j) / 64
      dec  ecx               ; ecx = n - j
      and  ecx, 63           ; ecx = (n - j) % 64
      bts  [r11 + rdi*8], rcx

smaller:

      ; == Q -= 2^{n - j - 1} ==
      mov  edi, ebp          ; edi = n - j
      dec  edi               ; edi = n - j - 1
      js   return            ; SF -> ebp = 0 -> dont subtract, just return
      
      mov  r8d, edi          ; r8 = n - j - 1
      shr  r8d, 6            ; r8 = (n - j - 1) / 64
      and  edi, 63           ; edi = (n - j - 1) % 63
      btr  [r11 + r8*8], rdi

      neg  rbx               ; word_offset = -word_offset
      lea  rsi, [rsi + rbx*8]; rsi = *R (we subtract the added offset)

      jmp  main_loop

return:
      pop  rbp               ; recover saved registers
      pop  rbx
      ret
