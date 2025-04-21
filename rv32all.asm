addi s0, zero, 2
add s1, s0, s0
slli s1, s1, 2
srli s1, s1, 1
srai s2, s1, 1
sub s3, s1, s0
sll s4, s0, s0
slt s5, s3, s0
sltu s6, s2, s1
xor s5, s6, s0
srl s7, s6, s1
sra s8, s5, s7
or s9, s7, s8
and s10, s8, s9
mul s11, s9, s10
mulh t0, s10, s11
mulhsu t1, s6, s2
mulhu t1, s6, s3
div t2, s1, s0
divu t3, s2, s0
rem t3, s7, s9
remu t4, s6, s8
andi t3, t3, 31
ori t5, t3, 17
xori s7, t5, 5
slti t6, s7, 11
sltiu t5, s9, 12
fence i, ro
fence.tso
pause
ecall
ebreak
lb t0, 1, s0
lh t1, 7777, s2
lw t2, 1092299, s4
lbu t3, -1, s1
lhu t4, 0, s1
beq s0, zero, 19
bne s1, s0, 24
bge t2, t3, 0x14
blt s1, s0, 20
bltu s2, s3, 48
bgeu s5, s4, 100
jal t0, 4
auipc t1, 3
lui t2, 9
sb t0, 12, t1
sh t2, 16, t3
sw t4, 9992, t5
jalr s1, ra, 4