package com.example.courtierprobackend.transactions.presentationlayer;

import com.example.courtierprobackend.transactions.datalayer.enums.BuyerStage;
import com.example.courtierprobackend.transactions.datalayer.enums.SellerStage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/transactions/stages")
public class TransactionStageController {

    @GetMapping
    public Map<String, List<String>> getAllStages() {
        Map<String, List<String>> stages = new HashMap<>();
        
        stages.put("BUYER_STAGES", Arrays.stream(BuyerStage.values())
                .map(Enum::name)
                .toList());
                
        stages.put("SELLER_STAGES", Arrays.stream(SellerStage.values())
                .map(Enum::name)
                .toList());
                
        return stages;
    }
}
