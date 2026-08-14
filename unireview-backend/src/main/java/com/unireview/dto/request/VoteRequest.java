package com.unireview.dto.request;

import com.unireview.enums.VoteType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VoteRequest {
    @NotNull
    private VoteType voteType;
}
