module stack_behaviour_normal(
    inout wire[3:0] IO_DATA, 
    input wire RESET, 
    input wire CLK, 
    input wire[1:0] COMMAND,
    input wire[2:0] INDEX
    ); 

    reg [3:0] stack [4:0];
    reg [2:0] header;
    reg [3:0] output_data = 4'bzzzz;

    always @(!CLK) begin
        output_data = 4'bzzzz;
    end

    always @(*) begin
        if (RESET) begin
            header = 3'b000;
            stack[0] = 4'b0000;
            stack[1] = 4'b0000;
            stack[2] = 4'b0000;
            stack[3] = 4'b0000;
            stack[4] = 4'b0000;
        end
        else if (CLK) begin
            case (COMMAND)
                2'b00: begin
                    // nop
                    output_data = 4'bzzzz;
                end
                2'b01: begin
                    // push
                    stack[header] = IO_DATA;
                    header = (header + 3'b001) % 5;
                    output_data = 4'bzzzz;
                end
                2'b10: begin
                    // pop
                    if (header == 4) begin
                        header = 3;
                    end
                    else begin
                        header = (header + 4) % 5;
                    end
                    output_data = stack[header];
                end
                2'b11: begin
                    // get
                    if (header == 4) begin
                        if (INDEX % 5 == 4) begin
                            output_data = stack[4];
                        end
                        else begin
                            output_data = stack[(3 - (INDEX % 5)) % 5];
                        end
                    end
                    else begin
                        output_data = stack[(header + 4 - (INDEX % 5)) % 5];
                    end
                end
            endcase
        end
    end

    assign IO_DATA = output_data;

endmodule
