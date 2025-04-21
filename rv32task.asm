addi s0, zero, 16
add s2, s0, s0
slli s2, s2, 4
sw a7, 2, s0
mul a4, s0, s2
beq t1, s0, 0
ori s2, s2, 1
xori s4, s2, 4327623
fence rw, r
sh t0, 17, s8
addi s3, s0, 10
blt s2, s3, 80
addi s2, s2, -250
lb s0, 2, s3
lui s10, 2
addi s3, s3, 12
jal zero, -20