module stack_structural_normal(
    inout wire[3:0] IO_DATA, 
    input wire RESET, 
    input wire CLK, 
    input wire[1:0] COMMAND,
    input wire[2:0] INDEX
    ); 

    wire y0, y1, y2, y3, g0, noty1;
    wire[3:0] i0, i1, i2, i3, i4, new_i, i3_minus1, j0, j1;
    wire[2:0] ind;

    demux_2x4 dmx0(COMMAND, 1'b1, y0, y1, y2, y3);
    expand e0(INDEX, i0);
    in_out e1(i0, y3, i1);
    stack_header header(y1, y2, CLK, RESET, i2);
    subtract_4bit sub0(i2, i1, i3);
    or(g0, y2, y3);
    dec_mod5 minus1(i3, i3_minus1);
    in_out e2(i3_minus1, g0, j0);
    in_out e3(i3, y1, j1);
    or4bit unite(j0, j1, new_i);
    demux_4x16_mod5 dmx1(new_i, i4);
    limit l(i4, ind);
    stack_mem ram(g0, CLK, y1, RESET, ind, IO_DATA);
endmodule


module rs_trigger (
    input wire r, s, clk,
    output wire q, qi
    );

    wire w0, w1, w2, w3;
    and(w0, r, clk);
    and(w1, s, clk);
    nor(q, w0, qi);
    nor(qi, w1, q);
endmodule


module d_trigger(
    input wire d, clk,
    output wire q
    );

    wire notd, qi;
    not(notd, d);
    rs_trigger r0(notd, d, clk, q, qi);
endmodule


module d_trigger_bk(
    input wire d, clk,
    output wire q
    );

    wire notd, notclk, w0, w1, qi;
    not(notd, d);
    not(notclk, clk);
    rs_trigger r0(d, notd, clk, w0, w1);
    rs_trigger r1(w0, w1, notclk, q, qi);
endmodule


module RAM_4bit(
    input wire[3:0] d_in,
    input wire clk,
    output wire[3:0] d_out
    );

    d_trigger d0(d_in[0], clk, d_out[0]);
    d_trigger d1(d_in[1], clk, d_out[1]);
    d_trigger d2(d_in[2], clk, d_out[2]);
    d_trigger d3(d_in[3], clk, d_out[3]);
endmodule


module RAM_4bit_bk(
    input wire[3:0] d_in,
    input wire clk,
    output wire[3:0] d_out
    );

    d_trigger_bk d0(d_in[0], clk, d_out[0]);
    d_trigger_bk d1(d_in[1], clk, d_out[1]);
    d_trigger_bk d2(d_in[2], clk, d_out[2]);
    d_trigger_bk d3(d_in[3], clk, d_out[3]);
endmodule


module summ_1bit(
    input wire a, b, c_in,
    output wire s, c_out
    );

    wire w0, w1, w2, w3;
    and(w0, c_in, b);
    and(w1, c_in, a);
    and(w2, b, a);
    or3 c0(w0, w1, w2, c_out);
    xor3 x0(a, b, c_in, s);
endmodule


module summ_4bit(
    input wire[3:0] a, b,
    output wire[3:0] s
    );

    wire c0, c1, c2, c3;
    summ_1bit s0(a[0], b[0], 1'b0, s[0], c0);
    summ_1bit s1(a[1], b[1], c0, s[1], c1);
    summ_1bit s2(a[2], b[2], c1, s[2], c2);
    summ_1bit s3(a[3], b[3], c2, s[3], c3); 
endmodule


module subtract_4bit(
    input wire[3:0] a, b,
    output wire[3:0] diff
    );

    wire[3:0] notb, inv, a5;
    not4bit n0(b, notb);
    summ_4bit sum0(a, 4'b0101, a5);
    summ_4bit sum1(a5, notb, inv);
    summ_4bit sum2(inv, 4'b0001, diff);
endmodule


module demux_2x4(
    input wire[1:0] data,
    input wire d,
    output wire y0, y1, y2, y3
    );

    wire b0, b1;
    not(b0, data[0]);
    not(b1, data[1]);
    and3 a0(d, b0, b1, y0);
    and3 a1(d, data[0], b1, y1);
    and3 a2(d, b0, data[1], y2);
    and3 a3(d, data[0], data[1], y3);
endmodule


module demux_3x8(
    input wire[2:0] data,
    input wire d,
    output wire y0, y1, y2, y3, y4, y5, y6, y7
    );

    wire w0, w1, w2;
    not(w0, data[2]);
    and(w1, data[2], d);
    and(w2, w0, d);
    demux_2x4 dem0(data[1:0], w2, y0, y1, y2, y3);
    demux_2x4 dem1(data[1:0], w1, y4, y5, y6, y7);
endmodule


module demux_4x16_mod5(
    input wire[3:0] data_in,
    output wire[3:0] data_out
    );

    wire w0, b0, b1, b2, b3, b4, b5, b6, b7, b8, b9, b10, b11, b12, b13, b14, b15;
    wire[3:0] d0, d1, d2, d3, d4, d5, d6, d7, d8, d9, d10, d11, d12, d13, d14, d15;
    wire[3:0] r0, r1, r2, r3, r4, r5, r6, r7;
    wire[3:0] p0, p1, p2, p3, q0, q1;

    not(w0, data_in[3]);
    demux_3x8 dem0(data_in[2:0], w0, b0, b1, b2, b3, b4, b5, b6, b7);
    demux_3x8 dem1(data_in[2:0], data_in[3], b8, b9, b10, b11, b12, b13, b14, b15);

    in_out t0(4'b0000, b0, d0);
    in_out t1(4'b0001, b1, d1);
    in_out t2(4'b0010, b2, d2);
    in_out t3(4'b0011, b3, d3);
    in_out t4(4'b0100, b4, d4);
    in_out t5(4'b0000, b5, d5);
    in_out t6(4'b0001, b6, d6);
    in_out t7(4'b0010, b7, d7);
    in_out t8(4'b0011, b8, d8);
    in_out t9(4'b0100, b9, d9);
    in_out t10(4'b0000, b10, d10);
    in_out t11(4'b0001, b11, d11);
    in_out t12(4'b0010, b12, d12);
    in_out t13(4'b0011, b13, d13);
    in_out t14(4'b0100, b14, d14);
    in_out t15(4'b0000, b15, d15);

    or4bit z0(d0, d1, r0);
    or4bit z1(d2, d3, r1);
    or4bit z2(d4, d5, r2);
    or4bit z3(d6, d7, r3);
    or4bit z4(d8, d9, r4);
    or4bit z5(d10, d11, r5);
    or4bit z6(d12, d13, r6);
    or4bit z7(d14, d15, r7);

    or4bit z8(r0, r1, p0);
    or4bit z9(r2, r3, p1);
    or4bit z10(r4, r5, p2);
    or4bit z11(r6, r7, p3);

    or4bit z12(p0, p1, q0);
    or4bit z13(p2, p3, q1);
    or4bit z14(q0, q1, data_out);
endmodule


module in_out(
    input wire[3:0] data_in,
    input wire d,
    output wire[3:0] data_out
    );

    and(data_out[0], data_in[0], d);
    and(data_out[1], data_in[1], d);
    and(data_out[2], data_in[2], d);
    and(data_out[3], data_in[3], d);
endmodule


module inc_mod5(
    input wire[3:0] data_in,
    output wire[3:0] data_out
    );

    wire w0, nw0;
    wire[3:0] d1, d2, d3, d4;
    and4 check(data_in[0], data_in[1], data_in[2], data_in[3], w0);
    not(nw0, w0);
    in_out io0(data_in, nw0, d1);
    in_out io1(4'b0000, w0, d2);
    or4bit k0(d1, d2, d3);
    summ_4bit plus(d3, 4'b0001, d4);
    demux_4x16_mod5 mod5(d4, data_out);
endmodule


module dec_mod5(
    input wire[3:0] data_in,
    output wire[3:0] data_out
    );

    wire w0, nw0;
    wire[3:0] d1, d2, d3, d4;
    nand4 check(data_in[0], data_in[1], data_in[2], data_in[3], w0);
    not(nw0, w0);
    in_out io0(data_in, nw0, d1);
    in_out io1(4'b0101, w0, d2);
    or4bit k0(d1, d2, d3);
    subtract_4bit minus(d3, 4'b0001, d4);
    demux_4x16_mod5 mod5(d4, data_out);
endmodule


module stack_header(
    input wire push, pop, clk, reset,
    output wire[3:0] index
    );

    wire w0, w1, w2, notreset;
    wire[3:0] data, b0, b1, b2, b3, t;
    or(w0, push, pop);
    and(w1, w0, clk);
    or(w2, w1, reset);
    inc_mod5 plus1(index, b0);
    in_out ifinc(b0, push, b1);
    dec_mod5 minus1(index, b2);
    in_out ifdec(b2, pop, b3);
    or4bit k0(b1, b3, t);
    not(notreset, reset);
    in_out change(t, notreset, data);
    RAM_4bit_bk ram(data, w2, index);
endmodule


module stack_mem(
    input wire get, clk, set, reset,
    input wire[2:0] index,
    inout wire[3:0] io
    );

    wire notreset, w0, w1, w2, w3, w4, w5, w6, w7, w8, w9, vent;
    wire y0, y1, y2, y3, y4, y5, y6, y7, u0, u1, u2, u3, u4, u5, u6, u7;
    wire[3:0] data, d0, d1, d2, d3, d4, x0, x1, x2, x3, x4;
    wire[3:0] i_data, o_data;

    not(notreset, reset);
    in_out ch(io, notreset, data);
    demux_3x8 setindex(index, set, y0, y1, y2, y3, y4, y5, y6, y7);
    and(w0, y0, clk);
    and(w1, y1, clk);
    and(w2, y2, clk);
    and(w3, y3, clk);
    and(w4, y4, clk);
    or(w5, w0, reset);
    or(w6, w1, reset);
    or(w7, w2, reset);
    or(w8, w3, reset);
    or(w9, w4, reset);
    RAM_4bit cell0(data, w5, d0);
    RAM_4bit cell1(data, w6, d1);
    RAM_4bit cell2(data, w7, d2);
    RAM_4bit cell3(data, w8, d3);
    RAM_4bit cell4(data, w9, d4);
    and(vent, get, clk);
    demux_3x8 getindex(index, get, u0, u1, u2, u3, u4, u5, u6, u7);
    in_out change0(d0, u0, x0);
    in_out change1(d1, u1, x1);
    in_out change2(d2, u2, x2);
    in_out change3(d3, u3, x3);
    in_out change4(d4, u4, x4);
    or5 unite(x0, x1, x2, x3, x4, o_data);
    ventil_4bit ventil1(o_data, vent, io);
endmodule


module ventil_1bit(
    input wire d,
    inout wire i,
    output wire o
    );

    wire nd;
    not(nd, d);
    cmos v(o, i, d, nd);
endmodule


module ventil_4bit(
    inout wire[3:0] data_in,
    input wire d,
    output wire[3:0] data_out
    );

    ventil_1bit v0(d, data_in[0], data_out[0]);
    ventil_1bit v1(d, data_in[1], data_out[1]);
    ventil_1bit v2(d, data_in[2], data_out[2]);
    ventil_1bit v3(d, data_in[3], data_out[3]);
endmodule



module or3(
    input wire i0, i1, i2,
    output wire o
    );

    wire w0;
    or(w0, i0, i1);
    or(o, w0, i2);
endmodule

module or5(
    input wire[3:0] i0, i1, i2, i3, i4,
    output wire[3:0] o
    );


    wire[3:0] w0, w1, w2;
    or4bit f0(i0, i1, w0);
    or4bit f1(i2, i3, w1);
    or4bit f2(w0, w1, w2);
    or4bit f3(w2, i4, o);
endmodule

module xor3(
    input wire a, b, c,
    output wire o
    );

    wire w0, w1, w2, w3, w4;
    xor(w0, a, b);
    xnor(w1, a, b);
    not(w2, c);
    and(w3, w0, w2);
    and(w4, w1, c);
    or(o, w3, w4);
endmodule

module and3(
    input wire i0, i1, i2,
    output wire o
    );

    wire w0;
    and(w0, i0, i1);
    and(o, w0, i2);
endmodule

module and4(
    input wire i0, i1, i2, i3,
    output wire o
    );

    wire w0, w1;
    and(w0, i0, i1);
    and(w1, i2, i3);
    and(o, w0, w1);
endmodule

module nand4(
    input wire i0, i1, i2, i3,
    output wire o
    );

    wire n0, n1, n2, n3, w0, w1;
    not(n0, i0);
    not(n1, i1);
    not(n2, i2);
    not(n3, i3);
    and(w0, n0, n1);
    and(w1, n2, n3);
    and(o, w0, w1);
endmodule

module not4bit(
    input wire[3:0] i,
    output wire[3:0] o
    );

    not(o[0], i[0]);
    not(o[1], i[1]);
    not(o[2], i[2]);
    not(o[3], i[3]);
endmodule

module or4bit(
    input wire[3:0] a, b,
    output wire[3:0] o
    );

    or(o[0], a[0], b[0]);
    or(o[1], a[1], b[1]);
    or(o[2], a[2], b[2]);
    or(o[3], a[3], b[3]);
endmodule

module expand(
    input wire[2:0] a,
    output wire[3:0] b
    );

    and(b[0], a[0], 1'b1);
    and(b[1], a[1], 1'b1);
    and(b[2], a[2], 1'b1);
    and(b[3], 1'b0, 1'b1);
endmodule

module limit(
    input wire[3:0] a,
    output wire[2:0] b
    );

    and(b[0], a[0], 1'b1);
    and(b[1], a[1], 1'b1);
    and(b[2], a[2], 1'b1);
endmodule
